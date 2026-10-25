package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.PendingEliminationDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.SectorCaptureDto;
import com.mg.nmlonline.api.dto.TurnFinalizeResultDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.SectorCapture;
import com.mg.nmlonline.domain.model.movement.SectorConflict;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
    private final VehicleMaintenanceService vehicleMaintenanceService;
    private final ReserveUnitPlacer reserveUnitPlacer;
    private final UnitRecoveryService unitRecoveryService;
    private final PlayerEliminationService playerEliminationService;

    private static final long SESSION_TIMEOUT_MS = TimeUnit.HOURS.toMillis(2);

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
                                      VehicleMaintenanceService vehicleMaintenanceService,
                                      ReserveUnitPlacer reserveUnitPlacer,
                                      UnitRecoveryService unitRecoveryService,
                                      PlayerEliminationService playerEliminationService) {
        this.turnLock = turnLock;
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.movementService = movementService;
        this.combatService = combatService;
        this.battleReportService = battleReportService;
        this.turnService = turnService;
        this.characterService = characterService;
        this.harvestAutoCollector = harvestAutoCollector;
        this.vehicleMaintenanceService = vehicleMaintenanceService;
        this.reserveUnitPlacer = reserveUnitPlacer;
        this.unitRecoveryService = unitRecoveryService;
        this.playerEliminationService = playerEliminationService;
    }

    /** Acquiert le verrou et prépare la résolution (validation, positions initiales) ; aucun hop effectué. */
    public synchronized TurnResolutionStateDto startSession() {
        // Session abandonnée (onglet fermé) : on la libère au bout de 2 h pour ne pas figer le jeu.
        if (session != null && System.currentTimeMillis() - session.startedAt > SESSION_TIMEOUT_MS) {
            turnLock.release();
            session = null;
        }
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
    public synchronized TurnResolutionStateDto advanceHop() {
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
    public synchronized ResolvedBattleDto resolveBattle(int conflictId) {
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
        clearHeadquartersEliminationPending(s, locked);
        registerPendingEliminations(s, rb, locked);
        return toBattleDto(rb, resolveNames(rb));
    }

    /** La capture du QG remplace une élimination par mort de personnage : l'attribution MJ devient inutile. */
    private void clearHeadquartersEliminationPending(Session s, Map<Long, Player> locked) {
        for (Player player : locked.values()) {
            if (player.getEliminationCause() == EliminationCause.HEADQUARTERS_CAPTURED) {
                s.pendingEliminations.remove(player.getId());
                s.eliminationKillers.remove(player.getId());
            }
        }
    }

    /** Une mort de personnage principal est attribuée par le MJ parmi les participants du combat. */
    private void registerPendingEliminations(Session s, ResolvedBattle rb, Map<Long, Player> locked) {
        for (var elimination : rb.eliminatedPlayers()) {
            if (!"MAIN_CHARACTER_KILLED".equals(elimination.cause())) {
                continue;
            }
            Player victim = locked.get(elimination.playerId());
            if (victim == null || victim.getEliminatedTurn() == null
                    || victim.getEliminatedTurn() != s.turnEnding) {
                continue;
            }
            if (s.pendingEliminations.containsKey(victim.getId())
                    || s.eliminationKillers.containsKey(victim.getId())) {
                continue;
            }
            List<Long> participants = rb.participantPlayerIds().stream()
                    .filter(playerId -> !playerId.equals(victim.getId()))
                    .distinct()
                    .toList();
            if (participants.isEmpty()) {
                s.eliminationKillers.put(victim.getId(), null);
            } else {
                s.pendingEliminations.put(victim.getId(),
                        new PendingElimination(victim.getId(), participants));
            }
        }
    }

    public synchronized TurnResolutionStateDto resolveElimination(Long victimPlayerId, Long killerPlayerId) {
        Session s = requireSession();
        PendingElimination pending = s.pendingEliminations.get(victimPlayerId);
        if (pending == null) {
            throw new IllegalArgumentException(
                    "Aucune élimination en attente pour le joueur " + victimPlayerId);
        }
        if (killerPlayerId != null && !pending.participantIds().contains(killerPlayerId)) {
            throw new IllegalArgumentException(
                    "Le tueur " + killerPlayerId + " n'a pas participé au combat");
        }
        // Choix commité tout de suite : un abandon ou un redémarrage ne doit pas le perdre.
        Player victim = playerRepository.findByIdForUpdate(victimPlayerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur " + victimPlayerId + " introuvable"));
        victim.setEliminatedByPlayerId(killerPlayerId);
        playerRepository.save(victim);
        s.eliminationKillers.put(victimPlayerId, killerPlayerId);
        s.pendingEliminations.remove(victimPlayerId);
        return toStateDto(s);
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
    public synchronized TurnFinalizeResultDto finalizeTurn() {
        Session s = requireSession();
        if (!s.pendingConflicts.isEmpty()) {
            throw new IllegalStateException("Résolvez toutes les batailles avant de finaliser");
        }
        if (!s.pendingEliminations.isEmpty()) {
            throw new IllegalStateException("Attribuez les éliminations avant de finaliser");
        }
        if (s.ctx.getCurrentStep() < s.ctx.getMaxSteps()) {
            throw new IllegalStateException("Effectuez tous les hops avant de finaliser");
        }
        Board board = loadBoard();
        movementService.refreshActiveOrders(s.ctx);
        try {
            if (board.getCurrentTurn() != s.turnEnding) {
                throw new IllegalStateException("Session périmée : le tour " + s.turnEnding
                        + " a déjà été finalisé");
            }
            // Propriété d'avant combats/déplacements : un secteur capturé pendant la résolution ne paie pas deux fois.
            harvestAutoCollector.collectRemainingMoney(board, s.turnEnding, s.sectorOwners);

            MovementResolutionResult result = movementService.finalizeResolution(board, s.ctx);
            List<Player> eliminated = playerRepository.findByEliminatedTurnOrderByNameAsc(s.turnEnding);
            for (Player victim : eliminated) {
                // Décision en session d'abord : le commit du choix peut suivre une finalisation concurrente.
                Long killerId = s.eliminationKillers.containsKey(victim.getId())
                        ? s.eliminationKillers.get(victim.getId())
                        : victim.getEliminatedByPlayerId();
                playerEliminationService.apply(board, victim, killerId, s.turnEnding);
            }
            characterService.regenerateAllCharacters();
            vehicleMaintenanceService.repairAllVehicles();
            unitRecoveryService.recoverWoundedUnits(s.turnEnding);
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
            dto.setEliminatedPlayers(eliminated.stream().map(Player::getName).toList());
            String eliminatedSuffix = eliminated.isEmpty() ? ""
                    : " " + String.join(", ", eliminated.stream().map(Player::getName).toList())
                            + " éliminé(s).";
            dto.setMessage(captures.isEmpty()
                    ? "Tour " + newTurn + " démarré." + eliminatedSuffix
                    : "Tour " + newTurn + " démarré — " + captures.size() + " secteur(s) capturé(s)."
                            + eliminatedSuffix);
            return dto;
        } finally {
            turnLock.release();
            this.session = null;
        }
    }

    /** Abandon soft : libère le verrou sans rollback des entités déplacées ni des combats résolus. */
    public synchronized void abort() {
        Session s = this.session;
        if (s == null) {
            return;
        }
        turnLock.release();
        this.session = null;
    }

    @Transactional(readOnly = true)
    public synchronized TurnResolutionStateDto getState() {
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
        dto.setPendingEliminations(s.pendingEliminations.values().stream()
                .map(pending -> toPendingEliminationDto(pending, names))
                .collect(Collectors.toList()));
        dto.setTransitCombatsCount(s.ctx.getTransitCombats().size());
        boolean pendingEmpty = s.pendingConflicts.isEmpty();
        dto.setCanAdvance(s.ctx.getCurrentStep() < s.ctx.getMaxSteps() && pendingEmpty);
        boolean allHopsDone = s.ctx.getCurrentStep() >= s.ctx.getMaxSteps();
        boolean eliminationsResolved = s.pendingEliminations.isEmpty();
        dto.setCanFinalize(allHopsDone && pendingEmpty && eliminationsResolved);
        dto.setAllDone(allHopsDone && pendingEmpty && eliminationsResolved);
        return dto;
    }

    private PendingEliminationDto toPendingEliminationDto(PendingElimination pending,
                                                          Function<Long, String> names) {
        PendingEliminationDto dto = new PendingEliminationDto();
        dto.setVictimPlayerId(pending.victimPlayerId());
        dto.setVictimPlayerName(names.apply(pending.victimPlayerId()));
        dto.setCause("MAIN_CHARACTER_KILLED");
        dto.setParticipants(pending.participantIds().stream()
                .map(playerId -> {
                    PendingEliminationDto.ParticipantDto participant =
                            new PendingEliminationDto.ParticipantDto();
                    participant.setPlayerId(playerId);
                    participant.setPlayerName(names.apply(playerId));
                    return participant;
                })
                .toList());
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
        s.pendingEliminations.values().forEach(pending -> {
            ids.add(pending.victimPlayerId());
            ids.addAll(pending.participantIds());
        });
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
        dto.setEliminatedPlayers(rb.eliminatedPlayers().stream()
                .map(elimination -> {
                    ResolvedBattleDto.EliminatedPlayerDto eliminated =
                            new ResolvedBattleDto.EliminatedPlayerDto();
                    eliminated.setPlayerId(elimination.playerId());
                    eliminated.setPlayerName(elimination.playerName());
                    eliminated.setCause(elimination.cause());
                    return eliminated;
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
        final long startedAt = System.currentTimeMillis();
        final List<PendingConflict> pendingConflicts = new ArrayList<>();
        final List<ResolvedBattle> resolvedConflicts = new ArrayList<>();
        final Map<Long, PendingElimination> pendingEliminations = new LinkedHashMap<>();
        final Map<Long, Long> eliminationKillers = new HashMap<>();
        int conflictIdSeq = 0;

        Session(MovementService.ResolutionContext ctx, int turnEnding, Map<Integer, Long> sectorOwners) {
            this.ctx = ctx;
            this.turnEnding = turnEnding;
            this.sectorOwners = sectorOwners;
        }
    }

    private record PendingElimination(Long victimPlayerId, List<Long> participantIds) {
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
                                  List<EliminatedPlayer> eliminatedPlayers,
                                  List<CombatService.StandoffBattleResult.PlayerOutcome> outcomes,
                                  List<BattleLogEntry> battleLog) {

        private record EliminatedPlayer(Long playerId, String playerName, String cause) {
        }

        private static List<EliminatedPlayer> eliminations(List<Player> players) {
            return players.stream()
                    .filter(Player::isEliminated)
                    .map(player -> new EliminatedPlayer(player.getId(), player.getName(),
                            player.getEliminationCause().name()))
                    .toList();
        }

        static ResolvedBattle duel(int sectorNumber, List<Player> attackerCamp, List<Player> defenderCamp,
                                   CombatService.SectorBattleResult r) {
            List<Long> attackerIds = attackerCamp.stream().map(Player::getId).toList();
            List<Long> defenderIds = defenderCamp.stream().map(Player::getId).toList();
            List<Long> winning = r.winner() == null ? List.of()
                    : attackerIds.contains(r.winner().getId()) ? attackerIds : defenderIds;
            List<Long> participants = new ArrayList<>(attackerIds);
            participants.addAll(defenderIds);
            List<Player> combatants = new ArrayList<>(attackerCamp);
            combatants.addAll(defenderCamp);
            return new ResolvedBattle(sectorNumber, false, participants, attackerIds, defenderIds, winning,
                    attackerIds.getFirst(), defenderIds.getFirst(), r.success(), r.message(),
                    r.winner() != null ? r.winner().getId() : null,
                    r.attackerCasualties().size(), r.defenderCasualties().size(),
                    r.attackerInjured().size(), r.defenderInjured().size(),
                    r.capturedBuildings(), r.attackerCharacterLost(), r.defenderCharacterLost(),
                    r.defenderHeadquartersCaptured(), eliminations(combatants), null, r.battleLog());
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
            List<Player> combatants = camps.stream().flatMap(List::stream).toList();
            return new ResolvedBattle(sectorNumber, true, participants, List.of(), List.of(), winning,
                    null, null, r.success(), r.message(),
                    r.winner() != null ? r.winner().getId() : null,
                    0, 0, 0, 0, r.capturedBuildings(), false, false, false,
                    eliminations(combatants), r.outcomes(), r.battleLog());
        }
    }
}