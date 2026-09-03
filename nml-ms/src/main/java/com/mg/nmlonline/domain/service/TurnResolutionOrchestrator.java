package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.TurnFinalizeResultDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.DestinationConflict;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
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
    private final TurnService turnService;
    private final GameCharacterService characterService;

    private volatile Session session;

    public TurnResolutionOrchestrator(TurnLock turnLock,
                                      BoardRepository boardRepository,
                                      PlayerRepository playerRepository,
                                      MovementService movementService,
                                      CombatService combatService,
                                      TurnService turnService,
                                      GameCharacterService characterService) {
        this.turnLock = turnLock;
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.movementService = movementService;
        this.combatService = combatService;
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
        List<DestinationConflict> stepConflicts = movementService.resolveStep(board, nextStep, s.ctx);
        s.pendingConflicts.clear();
        for (DestinationConflict c : stepConflicts) {
            s.pendingConflicts.add(new PendingConflict(++s.conflictIdSeq, c));
        }
        return toStateDto(s);
    }

    /** Résout une bataille du hop courant via {@link CombatService#simulateSectorBattle}. */
    public ResolvedBattleDto resolveBattle(int conflictId) {
        Session s = requireSession();
        PendingConflict pc = s.pendingConflicts.stream()
                .filter(p -> p.id == conflictId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conflit " + conflictId + " introuvable ou déjà résolu"));
        Player attacker = playerRepository.findById(pc.attackerPlayerId)
                .orElseThrow(() -> new IllegalStateException("Joueur attaquant " + pc.attackerPlayerId + " introuvable"));
        Player defender = playerRepository.findById(pc.defenderPlayerId)
                .orElseThrow(() -> new IllegalStateException("Joueur défenseur " + pc.defenderPlayerId + " introuvable"));
        Board board = loadBoard();
        CombatService.SectorBattleResult r =
                combatService.simulateSectorBattle(attacker, defender, board, pc.sectorNumber);

        ResolvedBattle rb = new ResolvedBattle(pc.sectorNumber, pc.attackerPlayerId, pc.defenderPlayerId, r);
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
            turnService.invalidateTurnCache();

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
                .map(pc -> toPendingDto(pc, names))
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

    private PendingConflictDto toPendingDto(PendingConflict pc, Function<Long, String> names) {
        PendingConflictDto dto = new PendingConflictDto();
        dto.setConflictId(pc.id);
        dto.setSectorNumber(pc.sectorNumber);
        dto.setAttackerPlayerId(pc.attackerPlayerId);
        dto.setAttackerName(names.apply(pc.attackerPlayerId));
        dto.setDefenderPlayerId(pc.defenderPlayerId);
        dto.setDefenderName(names.apply(pc.defenderPlayerId));
        return dto;
    }

    private Function<Long, String> resolveNamesForSession(Session s) {
        List<Long> ids = new ArrayList<>();
        s.pendingConflicts.forEach(pc -> { ids.add(pc.attackerPlayerId); ids.add(pc.defenderPlayerId); });
        s.resolvedConflicts.forEach(rb -> { ids.add(rb.attackerPlayerId); ids.add(rb.defenderPlayerId); });
        if (ids.isEmpty()) {
            return id -> null;
        }
        var players = playerRepository.findAllById(ids);
        return players.stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a))::get;
    }

    private Function<Long, String> resolveNames(ResolvedBattle rb) {
        List<Long> ids = List.of(rb.attackerPlayerId, rb.defenderPlayerId);
        var players = playerRepository.findAllById(ids);
        return players.stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a))::get;
    }

    private ResolvedBattleDto toBattleDto(ResolvedBattle rb, Function<Long, String> names) {
        ResolvedBattleDto dto = new ResolvedBattleDto();
        dto.setSectorNumber(rb.sectorNumber);
        dto.setAttackerPlayerId(rb.attackerPlayerId);
        dto.setAttackerName(names.apply(rb.attackerPlayerId));
        dto.setDefenderPlayerId(rb.defenderPlayerId);
        dto.setDefenderName(names.apply(rb.defenderPlayerId));
        dto.setSuccess(rb.success);
        dto.setMessage(rb.message);
        dto.setWinnerId(rb.winnerId);
        dto.setWinnerName(rb.winnerId != null ? names.apply(rb.winnerId) : null);
        dto.setAttackerCasualties(rb.attackerCasualties);
        dto.setDefenderCasualties(rb.defenderCasualties);
        dto.setAttackerInjured(rb.attackerInjured);
        dto.setDefenderInjured(rb.defenderInjured);
        dto.setCapturedBuildings(rb.capturedBuildings);
        dto.setAttackerCharacterLost(rb.attackerCharacterLost);
        dto.setDefenderCharacterLost(rb.defenderCharacterLost);
        dto.setDefenderHeadquartersCaptured(rb.defenderHeadquartersCaptured);
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
        final Long attackerPlayerId;
        final Long defenderPlayerId;

        PendingConflict(int id, DestinationConflict c) {
            this.id = id;
            this.sectorNumber = c.sectorNumber();
            this.attackerPlayerId = c.attackerPlayerId();
            this.defenderPlayerId = c.defenderPlayerId();
        }
    }

    private static final class ResolvedBattle {
        final int sectorNumber;
        final Long attackerPlayerId;
        final Long defenderPlayerId;
        final boolean success;
        final String message;
        final Long winnerId;
        final int attackerCasualties;
        final int defenderCasualties;
        final int attackerInjured;
        final int defenderInjured;
        final int capturedBuildings;
        final boolean attackerCharacterLost;
        final boolean defenderCharacterLost;
        final boolean defenderHeadquartersCaptured;

        ResolvedBattle(int sectorNumber, Long attackerPlayerId, Long defenderPlayerId,
                       CombatService.SectorBattleResult r) {
            this.sectorNumber = sectorNumber;
            this.attackerPlayerId = attackerPlayerId;
            this.defenderPlayerId = defenderPlayerId;
            this.success = r.success();
            this.message = r.message();
            this.winnerId = r.winner() != null ? r.winner().getId() : null;
            this.attackerCasualties = r.attackerCasualties().size();
            this.defenderCasualties = r.defenderCasualties().size();
            this.attackerInjured = r.attackerInjured().size();
            this.defenderInjured = r.defenderInjured().size();
            this.capturedBuildings = r.capturedBuildings();
            this.attackerCharacterLost = r.attackerCharacterLost();
            this.defenderCharacterLost = r.defenderCharacterLost();
            this.defenderHeadquartersCaptured = r.defenderHeadquartersCaptured();
        }
    }
}