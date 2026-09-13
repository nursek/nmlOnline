package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.TurnFinalizeResultDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.SectorConflict;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestration pas-à-pas de la fin de tour, pilotée par l'admin hop par hop :
 * {@code startSession} → {@code advanceHop} × N → {@code resolveBattle} par conflit
 * → {@code finalizeTurn}.
 *
 * <p>Session en mémoire, single-JVM (perdue au redémarrage). {@link TurnLock} est
 * acquis au {@code startSession} et tenu jusqu'à {@code finalizeTurn}/{@code abort},
 * bloquant {@link TurnService#advanceTurn()} en parallèle.
 */
@Service
@Transactional
public class TurnResolutionOrchestrator {

    private final TurnLock turnLock;
    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;
    private final MovementService movementService;
    private final CombatService combatService;
    private final BattleReportService battleReportService;
    private final TurnService turnService;
    private final GameCharacterService characterService;

    private volatile Session session;

    @Autowired
    public TurnResolutionOrchestrator(TurnLock turnLock,
                                      BoardRepository boardRepository,
                                      PlayerRepository playerRepository,
                                      MovementService movementService,
                                      CombatService combatService,
                                      BattleReportService battleReportService,
                                      TurnService turnService,
                                      GameCharacterService characterService) {
        this.turnLock = turnLock;
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.movementService = movementService;
        this.combatService = combatService;
        this.battleReportService = battleReportService;
        this.turnService = turnService;
        this.characterService = characterService;
    }

    /** Acquiert le verrou et prépare la résolution (validation, positions initiales) ; aucun hop effectué. */
    public TurnResolutionStateDto startSession() {
        if (!turnLock.tryAcquire()) {
            throw new IllegalStateException("Une résolution de fin de tour est déjà en cours");
        }
        try {
            Board board = loadBoard();
            int turnEnding = board.getCurrentTurn();
            MovementService.ResolutionContext ctx = movementService.prepareResolution(turnEnding, board);
            this.session = new Session(ctx, turnEnding);
            return toStateDto(session);
        } catch (RuntimeException e) {
            turnLock.release();
            throw e;
        }
    }

    /** Avance d'un hop et expose les conflits à l'admin. Refusé si des batailles du hop courant sont en attente. */
    public TurnResolutionStateDto advanceHop() {
        Session s = requireSession();
        if (!s.pendingConflicts.isEmpty()) {
            throw new IllegalStateException("Résolvez les batailles du hop courant avant de passer au suivant");
        }
        if (s.ctx.getCurrentStep() >= s.ctx.getMaxSteps()) {
            throw new IllegalStateException("Tous les hops sont déjà effectués — finalisez le tour");
        }
        int nextStep = s.ctx.getCurrentStep() + 1;
        Board board = loadBoard();
        movementService.refreshActiveOrders(s.ctx);
        List<SectorConflict> stepConflicts = movementService.resolveStep(board, nextStep, s.ctx);
        s.pendingConflicts.clear();
        for (SectorConflict c : stepConflicts) {
            s.pendingConflicts.add(new PendingConflict(++s.conflictIdSeq, c));
        }
        return toStateDto(s);
    }

    /** Résout le conflit : duel classique à 2 camps, impasse mexicaine à 3+ (un seul appel pour tout le cercle). */
    public ResolvedBattleDto resolveBattle(int conflictId) {
        Session s = requireSession();
        PendingConflict pc = s.pendingConflicts.stream()
                .filter(p -> p.id == conflictId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conflit " + conflictId + " introuvable ou déjà résolu"));
        Board board = loadBoard();

        ResolvedBattle rb;
        if (pc.standoff) {
            // Verrou pessimiste : les stats de combat de tous les camps sont recalculées/flushées pendant la bataille.
            List<Player> participants = new ArrayList<>();
            for (Long playerId : pc.participantPlayerIds) {
                participants.add(playerRepository.findByIdForUpdate(playerId)
                        .orElseThrow(() -> new IllegalStateException("Joueur " + playerId + " introuvable")));
            }
            CombatService.StandoffBattleResult r =
                    combatService.simulateSectorStandoff(participants, board, pc.sectorNumber);
            battleReportService.saveStandoffReport(s.turnEnding, pc.sectorNumber, participants, r);
            rb = ResolvedBattle.standoff(pc.sectorNumber, r);
        } else {
            Player attacker = playerRepository.findByIdForUpdate(pc.participantPlayerIds.get(0))
                    .orElseThrow(() -> new IllegalStateException(
                            "Joueur attaquant " + pc.participantPlayerIds.get(0) + " introuvable"));
            Player defender = playerRepository.findByIdForUpdate(pc.participantPlayerIds.get(1))
                    .orElseThrow(() -> new IllegalStateException(
                            "Joueur défenseur " + pc.participantPlayerIds.get(1) + " introuvable"));
            CombatService.SectorBattleResult r =
                    combatService.simulateSectorBattle(attacker, defender, board, pc.sectorNumber);
            battleReportService.saveDuelReport(s.turnEnding, pc.sectorNumber, attacker, defender, r);
            rb = ResolvedBattle.duel(pc.sectorNumber, attacker.getId(), defender.getId(), r);
        }

        s.resolvedConflicts.add(rb);
        s.pendingConflicts.remove(pc);
        return toBattleDto(rb, resolveNames(rb));
    }

    /** Finalise le tour (ordres RESOLVED + incrémentation), libère le verrou. Nécessite tous hops + batailles résolus. */
    public TurnFinalizeResultDto finalizeTurn() {
        Session s = requireSession();
        if (!s.pendingConflicts.isEmpty()) {
            throw new IllegalStateException("Résolvez toutes les batailles avant de finaliser");
        }
        if (s.ctx.getCurrentStep() < s.ctx.getMaxSteps()) {
            throw new IllegalStateException("Effectuez tous les hops avant de finaliser");
        }
        Board board = loadBoard();
        movementService.refreshActiveOrders(s.ctx);
        try {
            MovementResolutionResult result = movementService.finalizeResolution(board, s.ctx);
            characterService.regenerateAllCharacters();
            board.setCurrentTurn(s.turnEnding + 1);
            boardRepository.save(board);
            turnService.publishTurn(s.turnEnding + 1);

            int newTurn = board.getCurrentTurn();
            TurnFinalizeResultDto dto = new TurnFinalizeResultDto();
            dto.setNewTurn(newTurn);
            dto.setTurnEnding(s.turnEnding);
            dto.setResolvedOrders(result.getResolved().size());
            dto.setBlockedOrders(result.getBlocked().size());
            dto.setConflictsResolved(s.resolvedConflicts.size());
            dto.setTransitCombats(result.getTransitCombats().size());
            dto.setMessage("Tour " + newTurn + " démarré.");
            return dto;
        } finally {
            turnLock.release();
            this.session = null;
        }
    }

    /** Abandon soft : libère le verrou sans rollback des entités déplacées ni des combats résolus. */
    public void abort() {
        Session s = this.session;
        if (s == null) {
            return;
        }
        turnLock.release();
        this.session = null;
    }

    @Transactional(readOnly = true)
    public TurnResolutionStateDto getState() {
        Session s = this.session;
        if (s == null) {
            TurnResolutionStateDto dto = new TurnResolutionStateDto();
            dto.setActive(false);
            return dto;
        }
        return toStateDto(s);
    }

    private Session requireSession() {
        Session s = this.session;
        if (s == null) {
            throw new IllegalStateException("Aucune session de résolution pas-à-pas active — démarrez-la d'abord");
        }
        return s;
    }

    private Board loadBoard() {
        return boardRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun plateau trouvé"));
    }

    private TurnResolutionStateDto toStateDto(Session s) {
        Function<Long, String> names = resolveNamesForSession(s);
        TurnResolutionStateDto dto = new TurnResolutionStateDto();
        dto.setActive(true);
        dto.setTurnEnding(s.turnEnding);
        dto.setCurrentStep(s.ctx.getCurrentStep());
        dto.setMaxSteps(s.ctx.getMaxSteps());
        dto.setPendingConflicts(s.pendingConflicts.stream()
                .map(pc -> toPendingDto(pc, names, s.ctx))
                .collect(Collectors.toList()));
        dto.setResolvedConflicts(s.resolvedConflicts.stream()
                .map(rb -> toBattleDto(rb, names))
                .collect(Collectors.toList()));
        dto.setTransitCombatsCount(s.ctx.getTransitCombats().size());
        boolean pendingEmpty = s.pendingConflicts.isEmpty();
        dto.setCanAdvance(s.ctx.getCurrentStep() < s.ctx.getMaxSteps() && pendingEmpty);
        boolean allHopsDone = s.ctx.getCurrentStep() >= s.ctx.getMaxSteps();
        dto.setCanFinalize(allHopsDone && pendingEmpty);
        dto.setAllDone(allHopsDone && pendingEmpty);
        return dto;
    }

    private PendingConflictDto toPendingDto(PendingConflict pc, Function<Long, String> names,
                                            MovementService.ResolutionContext ctx) {
        PendingConflictDto dto = new PendingConflictDto();
        dto.setConflictId(pc.id);
        dto.setSectorNumber(pc.sectorNumber);
        dto.setStandoff(pc.standoff);
        dto.setParticipants(pc.participantPlayerIds.stream()
                .map(playerId -> {
                    PendingConflictDto.ParticipantDto participant = new PendingConflictDto.ParticipantDto();
                    participant.setPlayerId(playerId);
                    participant.setPlayerName(names.apply(playerId));
                    participant.setSubmittedAt(ctx.getFirstOrderAt().get(playerId));
                    return participant;
                })
                .toList());
        if (!pc.standoff && pc.participantPlayerIds.size() == 2) {
            dto.setAttackerPlayerId(pc.participantPlayerIds.get(0));
            dto.setAttackerName(names.apply(pc.participantPlayerIds.get(0)));
            dto.setDefenderPlayerId(pc.participantPlayerIds.get(1));
            dto.setDefenderName(names.apply(pc.participantPlayerIds.get(1)));
        }
        return dto;
    }

    private Function<Long, String> resolveNamesForSession(Session s) {
        List<Long> ids = new ArrayList<>();
        s.pendingConflicts.forEach(pc -> ids.addAll(pc.participantPlayerIds));
        s.resolvedConflicts.forEach(rb -> ids.addAll(rb.participantPlayerIds()));
        return resolveNamesForIds(ids);
    }

    private Function<Long, String> resolveNames(ResolvedBattle rb) {
        return resolveNamesForIds(rb.participantPlayerIds());
    }

    private Function<Long, String> resolveNamesForIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return id -> null;
        }
        var players = playerRepository.findAllById(ids);
        return players.stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a))::get;
    }

    private ResolvedBattleDto toBattleDto(ResolvedBattle rb, Function<Long, String> names) {
        ResolvedBattleDto dto = new ResolvedBattleDto();
        dto.setSectorNumber(rb.sectorNumber());
        dto.setStandoff(rb.standoff());
        dto.setSuccess(rb.success());
        dto.setMessage(rb.message());
        dto.setWinnerId(rb.winnerId());
        dto.setWinnerName(rb.winnerId() != null ? names.apply(rb.winnerId()) : null);
        dto.setCapturedBuildings(rb.capturedBuildings());
        dto.setBattleLog(rb.battleLog().stream()
                .map(entry -> {
                    ResolvedBattleDto.BattleLogEntryDto logEntry = new ResolvedBattleDto.BattleLogEntryDto();
                    logEntry.setPhase(entry.phase());
                    logEntry.setOutcome(entry.outcome());
                    logEntry.setMessage(entry.message());
                    return logEntry;
                })
                .toList());
        if (rb.standoff()) {
            dto.setParticipants(rb.outcomes().stream()
                    .map(outcome -> {
                        ResolvedBattleDto.StandoffParticipantDto participant =
                                new ResolvedBattleDto.StandoffParticipantDto();
                        participant.setPlayerId(outcome.playerId());
                        participant.setPlayerName(names.apply(outcome.playerId()));
                        participant.setCasualties(outcome.casualties());
                        participant.setInjured(outcome.injured());
                        participant.setCharacterLost(outcome.characterLost());
                        participant.setEliminated(outcome.eliminated());
                        return participant;
                    })
                    .toList());
            return dto;
        }
        dto.setAttackerPlayerId(rb.attackerPlayerId());
        dto.setAttackerName(rb.attackerPlayerId() != null ? names.apply(rb.attackerPlayerId()) : null);
        dto.setDefenderPlayerId(rb.defenderPlayerId());
        dto.setDefenderName(rb.defenderPlayerId() != null ? names.apply(rb.defenderPlayerId()) : null);
        dto.setAttackerCasualties(rb.attackerCasualties());
        dto.setDefenderCasualties(rb.defenderCasualties());
        dto.setAttackerInjured(rb.attackerInjured());
        dto.setDefenderInjured(rb.defenderInjured());
        dto.setAttackerCharacterLost(rb.attackerCharacterLost());
        dto.setDefenderCharacterLost(rb.defenderCharacterLost());
        dto.setDefenderHeadquartersCaptured(rb.defenderHeadquartersCaptured());
        return dto;
    }

    private static final class Session {
        final MovementService.ResolutionContext ctx;
        final int turnEnding;
        final List<PendingConflict> pendingConflicts = new ArrayList<>();
        final List<ResolvedBattle> resolvedConflicts = new ArrayList<>();
        int conflictIdSeq = 0;

        Session(MovementService.ResolutionContext ctx, int turnEnding) {
            this.ctx = ctx;
            this.turnEnding = turnEnding;
        }
    }

    private static final class PendingConflict {
        final int id;
        final int sectorNumber;
        final List<Long> participantPlayerIds;
        final boolean standoff;

        PendingConflict(int id, SectorConflict c) {
            this.id = id;
            this.sectorNumber = c.sectorNumber();
            this.participantPlayerIds = c.participantPlayerIds();
            this.standoff = c.isStandoff();
        }
    }

    private record ResolvedBattle(int sectorNumber, boolean standoff, List<Long> participantPlayerIds,
                                  Long attackerPlayerId, Long defenderPlayerId, boolean success, String message,
                                  Long winnerId, int attackerCasualties, int defenderCasualties,
                                  int attackerInjured, int defenderInjured, int capturedBuildings,
                                  boolean attackerCharacterLost, boolean defenderCharacterLost,
                                  boolean defenderHeadquartersCaptured,
                                  List<CombatService.StandoffBattleResult.PlayerOutcome> outcomes,
                                  List<BattleLogEntry> battleLog) {

        static ResolvedBattle duel(int sectorNumber, Long attackerPlayerId, Long defenderPlayerId,
                                   CombatService.SectorBattleResult r) {
            return new ResolvedBattle(sectorNumber, false, List.of(attackerPlayerId, defenderPlayerId),
                    attackerPlayerId, defenderPlayerId, r.success(), r.message(),
                    r.winner() != null ? r.winner().getId() : null,
                    r.attackerCasualties().size(), r.defenderCasualties().size(),
                    r.attackerInjured().size(), r.defenderInjured().size(),
                    r.capturedBuildings(), r.attackerCharacterLost(), r.defenderCharacterLost(),
                    r.defenderHeadquartersCaptured(), null, r.battleLog());
        }

        static ResolvedBattle standoff(int sectorNumber, CombatService.StandoffBattleResult r) {
            return new ResolvedBattle(sectorNumber, true,
                    r.outcomes().stream().map(CombatService.StandoffBattleResult.PlayerOutcome::playerId).toList(),
                    null, null, r.success(), r.message(),
                    r.winner() != null ? r.winner().getId() : null,
                    0, 0, 0, 0, r.capturedBuildings(), false, false, false, r.outcomes(), r.battleLog());
        }
    }
}