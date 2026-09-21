package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.SectorCaptureDto;
import com.mg.nmlonline.api.dto.TurnFinalizeResultDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.SectorCapture;
import com.mg.nmlonline.domain.model.movement.SectorConflict;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final HarvestAutoCollector harvestAutoCollector;
    private final ReserveUnitPlacer reserveUnitPlacer;

    private volatile Session session;

    @Autowired
    public TurnResolutionOrchestrator(TurnLock turnLock,
                                      BoardRepository boardRepository,
                                      PlayerRepository playerRepository,
                                      MovementService movementService,
                                      CombatService combatService,
                                      BattleReportService battleReportService,
                                      TurnService turnService,
                                      GameCharacterService characterService,
                                      HarvestAutoCollector harvestAutoCollector,
                                      ReserveUnitPlacer reserveUnitPlacer) {
        this.turnLock = turnLock;
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.movementService = movementService;
        this.combatService = combatService;
        this.battleReportService = battleReportService;
        this.turnService = turnService;
        this.characterService = characterService;
        this.harvestAutoCollector = harvestAutoCollector;
        this.reserveUnitPlacer = reserveUnitPlacer;
    }

    /** Acquiert le verrou et prépare la résolution (validation, positions initiales) ; aucun hop effectué. */
    public TurnResolutionStateDto startSession() {
        if (!turnLock.tryAcquire()) {
            throw new IllegalStateException("Une résolution de fin de tour est déjà en cours");
        }
        try {
            Board board = loadBoard();
            int turnEnding = board.getCurrentTurn();
            reserveUnitPlacer.placeAllAtHeadquarters();
            MovementService.ResolutionContext ctx = movementService.prepareResolution(turnEnding, board);
            Session s = new Session(ctx, turnEnding, HarvestAutoCollector.ownersBySector(board));
            // Conflits de rupture/trahison détectés dès la préparation (ex-alliés co-localisés).
            for (SectorConflict conflict : ctx.getConflicts()) {
                s.pendingConflicts.add(new PendingConflict(++s.conflictIdSeq, conflict));
            }
            this.session = s;
            return toStateDto(s);
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

    /** Résout le conflit : duel classique à 2 camps (1 ou 2 alliés), impasse à 3+ (un seul appel pour tout le cercle). */
    public ResolvedBattleDto resolveBattle(int conflictId) {
        Session s = requireSession();
        PendingConflict pc = s.pendingConflicts.stream()
                .filter(p -> p.id == conflictId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conflit " + conflictId + " introuvable ou déjà résolu"));
        Board board = loadBoard();
        Map<Long, Player> locked = lockPlayers(pc.camps);

        ResolvedBattle rb;
        if (pc.standoff) {
            List<List<Player>> camps = pc.camps.stream()
                    .map(camp -> camp.stream().map(locked::get).toList())
                    .toList();
            CombatService.StandoffBattleResult r =
                    combatService.simulateSectorStandoff(camps, board, pc.sectorNumber);
            battleReportService.saveStandoffReport(s.turnEnding, pc.sectorNumber,
                    camps.stream().flatMap(List::stream).toList(), r);
            rb = ResolvedBattle.standoff(pc.sectorNumber, camps, r);
        } else {
            List<Player> attackerCamp = pc.camps.get(0).stream().map(locked::get).toList();
            List<Player> defenderCamp = pc.camps.get(1).stream().map(locked::get).toList();
            CombatService.SectorBattleResult r =
                    combatService.simulateSectorBattle(attackerCamp, defenderCamp, board, pc.sectorNumber);
            battleReportService.saveDuelReport(s.turnEnding, pc.sectorNumber, attackerCamp, defenderCamp, r);
            rb = ResolvedBattle.duel(pc.sectorNumber, attackerCamp, defenderCamp, r);
        }

        if (!rb.winningCampPlayerIds().isEmpty()) {
            movementService.captureAfterBattle(board, s.ctx, pc.sectorNumber, rb.winningCampPlayerIds());
        }

        s.resolvedConflicts.add(rb);
        s.pendingConflicts.remove(pc);
        return toBattleDto(rb, resolveNames(rb));
    }

    /** Lock pessimiste ordonné par id sur tous les combattants : évite le deadlock entre deux résolutions. */
    private Map<Long, Player> lockPlayers(List<List<Long>> camps) {
        List<Long> ids = camps.stream().flatMap(List::stream).distinct().sorted().toList();
        Map<Long, Player> players = new HashMap<>();
        for (Long id : ids) {
            players.put(id, playerRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalStateException("Joueur " + id + " introuvable")));
        }
        return players;
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
            // Propriété d'avant combats/déplacements : un secteur capturé pendant la résolution ne paie pas deux fois.
            harvestAutoCollector.collectRemainingMoney(board, s.turnEnding, s.sectorOwners);

            MovementResolutionResult result = movementService.finalizeResolution(board, s.ctx);
            characterService.regenerateAllCharacters();
            board.setCurrentTurn(s.turnEnding + 1);
            boardRepository.save(board);
            turnService.publishTurn(s.turnEnding + 1);

            int newTurn = board.getCurrentTurn();
            List<SectorCapture> captures = result.getCaptures();
            TurnFinalizeResultDto dto = new TurnFinalizeResultDto();
            dto.setNewTurn(newTurn);
            dto.setTurnEnding(s.turnEnding);
            dto.setResolvedOrders(result.getResolved().size());
            dto.setBlockedOrders(result.getBlocked().size());
            dto.setConflictsResolved(s.resolvedConflicts.size());
            dto.setTransitCombats(result.getTransitCombats().size());
            dto.setCapturedSectors(toCaptureDtoList(captures));
            dto.setMessage(captures.isEmpty()
                    ? "Tour " + newTurn + " démarré."
                    : "Tour " + newTurn + " démarré — " + captures.size() + " secteur(s) capturé(s).");
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
        List<Long> participants = pc.participantPlayerIds();
        dto.setParticipants(participants.stream()
                .map(playerId -> {
                    PendingConflictDto.ParticipantDto participant = new PendingConflictDto.ParticipantDto();
                    participant.setPlayerId(playerId);
                    participant.setPlayerName(names.apply(playerId));
                    participant.setSubmittedAt(ctx.getFirstOrderAt().get(playerId));
                    return participant;
                })
                .toList());
        if (!pc.standoff && pc.camps.size() == 2) {
            dto.setAttackerPlayerId(pc.camps.get(0).getFirst());
            dto.setAttackerName(campNames(pc.camps.get(0), names));
            dto.setDefenderPlayerId(pc.camps.get(1).getFirst());
            dto.setDefenderName(campNames(pc.camps.get(1), names));
        }
        return dto;
    }

    private static String campNames(List<Long> camp, Function<Long, String> names) {
        if (camp == null || camp.isEmpty()) {
            return null;
        }
        return camp.stream().map(names).collect(Collectors.joining(" + "));
    }

    private Function<Long, String> resolveNamesForSession(Session s) {
        List<Long> ids = new ArrayList<>();
        s.pendingConflicts.forEach(pc -> ids.addAll(pc.participantPlayerIds()));
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

    private List<SectorCaptureDto> toCaptureDtoList(List<SectorCapture> captures) {
        Function<Long, String> names = resolveNamesForIds(
                captures.stream().map(SectorCapture::playerId).distinct().toList());
        return captures.stream()
                .map(capture -> {
                    SectorCaptureDto dto = new SectorCaptureDto();
                    dto.setSectorNumber(capture.sectorNumber());
                    dto.setPlayerId(capture.playerId());
                    dto.setPlayerName(names.apply(capture.playerId()));
                    dto.setOnTheFly(capture.onTheFly());
                    return dto;
                })
                .toList();
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
        dto.setAttackerName(campNames(rb.attackerCampPlayerIds(), names));
        dto.setDefenderPlayerId(rb.defenderPlayerId());
        dto.setDefenderName(campNames(rb.defenderCampPlayerIds(), names));
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
        final Map<Integer, Long> sectorOwners;
        final List<PendingConflict> pendingConflicts = new ArrayList<>();
        final List<ResolvedBattle> resolvedConflicts = new ArrayList<>();
        int conflictIdSeq = 0;

        Session(MovementService.ResolutionContext ctx, int turnEnding, Map<Integer, Long> sectorOwners) {
            this.ctx = ctx;
            this.turnEnding = turnEnding;
            this.sectorOwners = sectorOwners;
        }
    }

    private static final class PendingConflict {
        final int id;
        final int sectorNumber;
        final List<List<Long>> camps;
        final boolean standoff;

        PendingConflict(int id, SectorConflict c) {
            this.id = id;
            this.sectorNumber = c.sectorNumber();
            this.camps = c.camps();
            this.standoff = c.isStandoff();
        }

        List<Long> participantPlayerIds() {
            return camps.stream().flatMap(List::stream).toList();
        }
    }

    private record ResolvedBattle(int sectorNumber, boolean standoff, List<Long> participantPlayerIds,
                                  List<Long> attackerCampPlayerIds, List<Long> defenderCampPlayerIds,
                                  List<Long> winningCampPlayerIds,
                                  Long attackerPlayerId, Long defenderPlayerId, boolean success, String message,
                                  Long winnerId, int attackerCasualties, int defenderCasualties,
                                  int attackerInjured, int defenderInjured, int capturedBuildings,
                                  boolean attackerCharacterLost, boolean defenderCharacterLost,
                                  boolean defenderHeadquartersCaptured,
                                  List<CombatService.StandoffBattleResult.PlayerOutcome> outcomes,
                                  List<BattleLogEntry> battleLog) {

        static ResolvedBattle duel(int sectorNumber, List<Player> attackerCamp, List<Player> defenderCamp,
                                   CombatService.SectorBattleResult r) {
            List<Long> attackerIds = attackerCamp.stream().map(Player::getId).toList();
            List<Long> defenderIds = defenderCamp.stream().map(Player::getId).toList();
            List<Long> winning = r.winner() == null ? List.of()
                    : attackerIds.contains(r.winner().getId()) ? attackerIds : defenderIds;
            List<Long> participants = new ArrayList<>(attackerIds);
            participants.addAll(defenderIds);
            return new ResolvedBattle(sectorNumber, false, participants, attackerIds, defenderIds, winning,
                    attackerIds.getFirst(), defenderIds.getFirst(), r.success(), r.message(),
                    r.winner() != null ? r.winner().getId() : null,
                    r.attackerCasualties().size(), r.defenderCasualties().size(),
                    r.attackerInjured().size(), r.defenderInjured().size(),
                    r.capturedBuildings(), r.attackerCharacterLost(), r.defenderCharacterLost(),
                    r.defenderHeadquartersCaptured(), null, r.battleLog());
        }

        static ResolvedBattle standoff(int sectorNumber, List<List<Player>> camps,
                                       CombatService.StandoffBattleResult r) {
            List<Long> participants = camps.stream().flatMap(List::stream).map(Player::getId).toList();
            List<Long> winning = r.winner() == null ? List.of()
                    : camps.stream()
                            .filter(camp -> camp.stream()
                                    .anyMatch(player -> player.getId().equals(r.winner().getId())))
                            .findFirst()
                            .map(camp -> camp.stream().map(Player::getId).toList())
                            .orElse(List.of(r.winner().getId()));
            return new ResolvedBattle(sectorNumber, true, participants, List.of(), List.of(), winning,
                    null, null, r.success(), r.message(),
                    r.winner() != null ? r.winner().getId() : null,
                    0, 0, 0, 0, r.capturedBuildings(), false, false, false, r.outcomes(), r.battleLog());
        }
    }
}