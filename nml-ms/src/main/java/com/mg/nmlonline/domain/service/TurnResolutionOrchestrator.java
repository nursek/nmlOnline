package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BattleHighlightDto;
import com.mg.nmlonline.api.dto.BattleReportDetailDto;
import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.api.dto.PendingConflictDto;
import com.mg.nmlonline.api.dto.PendingEliminationDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.SectorCaptureDto;
import com.mg.nmlonline.api.dto.TransitCombatResultDto;
import com.mg.nmlonline.api.dto.TurnFinalizeResultDto;
import com.mg.nmlonline.api.dto.TurnJournalDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.SectorCapture;
import com.mg.nmlonline.domain.model.movement.SectorConflict;
import com.mg.nmlonline.domain.model.movement.TransitCombatResult;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.press.EliminationEvent;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final SniperService sniperService;
    private final TurnJournalService turnJournalService;
    private final EliminationHistoryService eliminationHistoryService;
    private final GameEndService gameEndService;
    private final FinalDuelService finalDuelService;

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
                                      PlayerEliminationService playerEliminationService,
                                      SniperService sniperService,
                                      TurnJournalService turnJournalService,
                                      EliminationHistoryService eliminationHistoryService,
                                      GameEndService gameEndService,
                                      FinalDuelService finalDuelService) {
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
        this.sniperService = sniperService;
        this.turnJournalService = turnJournalService;
        this.eliminationHistoryService = eliminationHistoryService;
        this.gameEndService = gameEndService;
        this.finalDuelService = finalDuelService;
    }

    /** Acquiert le verrou et prépare la résolution (validation, positions initiales) ; aucun hop effectué. */
    public synchronized TurnResolutionStateDto startSession(Long boardId) {
        // Session abandonnée (onglet fermé) : on la libère au bout de 2 h pour ne pas figer le jeu.
        if (session != null && System.currentTimeMillis() - session.startedAt > SESSION_TIMEOUT_MS) {
            turnLock.release(session.boardId);
            session = null;
        }
        if (session != null && !session.boardId.equals(boardId)) {
            throw new IllegalStateException("Une résolution est déjà en cours sur une autre partie");
        }
        if (!turnLock.tryAcquire(boardId)) {
            throw new IllegalStateException("Une résolution de fin de tour est déjà en cours");
        }
        try {
            Board board = boardOf(boardId);
            if (board.isFinished()) {
                throw new IllegalStateException("Partie terminée : aucune résolution de tour");
            }
            int turnEnding = board.getCurrentTurn();
            reserveUnitPlacer.placeAllAtHeadquarters();
            MovementService.ResolutionContext ctx = movementService.prepareResolution(turnEnding, board);
            Session s = new Session(boardId, ctx, turnEnding, HarvestAutoCollector.ownersBySector(board));
            // Conflits de rupture/trahison détectés dès la préparation (ex-alliés co-localisés).
            for (SectorConflict conflict : ctx.getConflicts()) {
                s.pendingConflicts.add(new PendingConflict(++s.conflictIdSeq, conflict));
            }
            this.session = s;
            return toStateDto(s);
        } catch (RuntimeException e) {
            turnLock.release(boardId);
            throw e;
        }
    }

    /** Avance d'un hop et expose les conflits à l'admin. Refusé si des batailles du hop courant sont en attente. */
    public synchronized TurnResolutionStateDto advanceHop(Long boardId) {
        Session s = requireSession(boardId);
        if (!s.pendingConflicts.isEmpty()) {
            throw new IllegalStateException("Résolvez les batailles du hop courant avant de passer au suivant");
        }
        if (s.ctx.getCurrentStep() >= s.ctx.getMaxSteps()) {
            throw new IllegalStateException("Tous les hops sont déjà effectués — finalisez le tour");
        }
        int nextStep = s.ctx.getCurrentStep() + 1;
        Board board = boardOf(s);
        movementService.refreshActiveOrders(s.ctx);
        List<SectorConflict> stepConflicts = movementService.resolveStep(board, nextStep, s.ctx);
        s.pendingConflicts.clear();
        for (SectorConflict c : stepConflicts) {
            s.pendingConflicts.add(new PendingConflict(++s.conflictIdSeq, c));
        }
        return toStateDto(s);
    }

    /** Résout le conflit : duel classique à 2 camps (1 ou 2 alliés), impasse à 3+ (un seul appel pour tout le cercle). */
    public synchronized ResolvedBattleDto resolveBattle(Long boardId, int conflictId) {
        Session s = requireSession(boardId);
        PendingConflict pc = s.pendingConflicts.stream()
                .filter(p -> p.id == conflictId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conflit " + conflictId + " introuvable ou déjà résolu"));
        Board board = boardOf(s);
        Set<Integer> battleSectors = new HashSet<>();
        s.resolvedConflicts.forEach(resolved -> battleSectors.add(resolved.sectorNumber()));
        s.pendingConflicts.forEach(pending -> battleSectors.add(pending.sectorNumber));
        Set<Long> transitEntityIds = inTransitEntityIds(s.ctx);

        ResolvedBattle rb;
        Map<Long, Player> locked;
        if (pc.standoff) {
            SniperService.StandoffSupport support = sniperService.collectStandoffSupport(
                    s.boardId, s.turnEnding, pc.sectorNumber, pc.camps, battleSectors);
            List<List<Long>> campIds = campsWithIndependents(pc.camps, support.independents());
            locked = lockPlayers(campIds);
            List<List<Player>> camps = toPlayerCamps(campIds, locked);
            Map<Integer, List<Unit>> byCamp = new HashMap<>(support.byCamp());
            Set<Integer> rangedOnly = attachIndependents(pc.camps.size(), support.independents(), byCamp);
            CombatService.StandoffBattleResult r = combatService.simulateSectorStandoff(
                    camps, board, pc.sectorNumber, byCamp, rangedOnly, transitEntityIds);
            battleReportService.saveStandoffReport(s.boardId, s.turnEnding, pc.sectorNumber,
                    camps.stream().flatMap(List::stream).toList(), r);
            if (r.success()) {
                sniperService.markResolved(support.resolvedOrders());
            }
            rb = ResolvedBattle.standoff(pc.id, pc.sectorNumber, camps, r);
        } else {
            SniperService.DuelSupport support = sniperService.collectDuelSupport(
                    s.boardId, s.turnEnding, pc.sectorNumber, pc.camps.get(0), pc.camps.get(1), battleSectors);
            if (support.independents().isEmpty()) {
                locked = lockPlayers(pc.camps);
                List<Player> attackerCamp = pc.camps.get(0).stream().map(locked::get).toList();
                List<Player> defenderCamp = pc.camps.get(1).stream().map(locked::get).toList();
                CombatService.SectorBattleResult r =
                        combatService.simulateSectorBattle(attackerCamp, defenderCamp, board, pc.sectorNumber,
                                support.attackers(), support.defenders(), transitEntityIds);
                battleReportService.saveDuelReport(s.boardId, s.turnEnding, pc.sectorNumber, attackerCamp,
                        defenderCamp, r);
                if (r.success()) {
                    sniperService.markResolved(support.resolvedOrders());
                }
                rb = ResolvedBattle.duel(pc.id, pc.sectorNumber, attackerCamp, defenderCamp, r);
            } else {
                List<List<Long>> campIds = campsWithIndependents(pc.camps, support.independents());
                locked = lockPlayers(campIds);
                List<List<Player>> camps = toPlayerCamps(campIds, locked);
                Map<Integer, List<Unit>> byCamp = new HashMap<>();
                byCamp.put(0, support.attackers());
                byCamp.put(1, support.defenders());
                Set<Integer> rangedOnly = attachIndependents(pc.camps.size(), support.independents(), byCamp);
                CombatService.StandoffBattleResult r = combatService.simulateSectorStandoff(
                        camps, board, pc.sectorNumber, byCamp, rangedOnly, transitEntityIds);
                battleReportService.saveStandoffReport(s.boardId, s.turnEnding, pc.sectorNumber,
                        camps.stream().flatMap(List::stream).toList(), r);
                if (r.success()) {
                    sniperService.markResolved(support.resolvedOrders());
                }
                rb = ResolvedBattle.standoff(pc.id, pc.sectorNumber, camps, r);
            }
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

    private static List<List<Long>> campsWithIndependents(List<List<Long>> camps,
                                                          List<SniperService.IndependentSupport> independents) {
        List<List<Long>> all = new ArrayList<>(camps);
        independents.forEach(support -> all.add(List.of(support.playerId())));
        return all;
    }

    private static List<List<Player>> toPlayerCamps(List<List<Long>> campIds, Map<Long, Player> locked) {
        return campIds.stream()
                .map(camp -> camp.stream().map(locked::get).toList())
                .toList();
    }

    private static Set<Integer> attachIndependents(int baseCampCount,
                                                   List<SniperService.IndependentSupport> independents,
                                                   Map<Integer, List<Unit>> byCamp) {
        Set<Integer> rangedOnly = new HashSet<>();
        for (int i = 0; i < independents.size(); i++) {
            int campIndex = baseCampCount + i;
            rangedOnly.add(campIndex);
            byCamp.put(campIndex, independents.get(i).units());
        }
        return rangedOnly;
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

    public synchronized TurnResolutionStateDto resolveElimination(Long boardId, Long victimPlayerId, Long killerPlayerId) {
        Session s = requireSession(boardId);
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

    private Set<Long> inTransitEntityIds(MovementService.ResolutionContext ctx) {
        int step = ctx.getCurrentStep();
        if (step < 1) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        for (MovementOrder order : ctx.getActiveOrders()) {
            if (!order.isFootMovement() || ctx.getStoppedIds().contains(order.getId())) {
                continue;
            }
            if (step >= order.getHopCount()) {
                continue;
            }
            Integer position = ctx.getCurrentPosition().get(order.getId());
            if (!Integer.valueOf(order.getRoute().get(step)).equals(position)) {
                continue;
            }
            ids.addAll(order.getEntityIds());
        }
        return ids;
    }

    /** Finalise le tour (ordres RESOLVED + incrémentation), libère le verrou. Nécessite tous hops + batailles résolus. */
    public synchronized TurnFinalizeResultDto finalizeTurn(Long boardId) {
        Session s = requireSession(boardId);
        if (!s.pendingConflicts.isEmpty()) {
            throw new IllegalStateException("Résolvez toutes les batailles avant de finaliser");
        }
        if (!s.pendingEliminations.isEmpty()) {
            throw new IllegalStateException("Attribuez les éliminations avant de finaliser");
        }
        if (s.ctx.getCurrentStep() < s.ctx.getMaxSteps()) {
            throw new IllegalStateException("Effectuez tous les hops avant de finaliser");
        }
        Board board = boardOf(s);
        movementService.refreshActiveOrders(s.ctx);
        try {
            if (board.getCurrentTurn() != s.turnEnding) {
                throw new IllegalStateException("Session périmée : le tour " + s.turnEnding
                        + " a déjà été finalisé");
            }
            // Propriété d'avant combats/déplacements : un secteur capturé pendant la résolution ne paie pas deux fois.
            harvestAutoCollector.collectRemainingMoney(board, s.turnEnding, s.sectorOwners);

            MovementResolutionResult result = movementService.finalizeResolution(board, s.ctx);
            Set<Integer> battleSectors = new HashSet<>();
            s.resolvedConflicts.forEach(resolved -> battleSectors.add(resolved.sectorNumber()));
            List<BattleReportDetailDto> rangedCombats =
                    sniperService.resolveRemainingFire(s.turnEnding, board, battleSectors);
            List<Player> players = playerRepository.findByBoardId(s.boardId);
            List<Player> eliminated = players.stream()
                    .filter(player -> Objects.equals(player.getEliminatedTurn(), s.turnEnding))
                    .toList();
            List<Player> deathOrder = deathOrder(s, eliminated);
            List<EliminationHistoryService.EliminationSnapshot> snapshots =
                    eliminationSnapshots(deathOrder, s);
            List<EliminationEvent> events =
                    eliminationHistoryService.save(s.boardId, s.turnEnding, snapshots);
            GameEndService.GameOutcome outcome = gameEndService.finishIfOver(board, players, events);
            if (!outcome.finished()) {
                finalDuelService.breakLastAllianceIfNeeded(board, players, s.turnEnding);
            }
            for (Player victim : deathOrder) {
                // Décision en session d'abord : le commit du choix peut suivre une finalisation concurrente.
                Long killerId = s.eliminationKillers.containsKey(victim.getId())
                        ? s.eliminationKillers.get(victim.getId())
                        : victim.getEliminatedByPlayerId();
                playerEliminationService.apply(board, victim, killerId, s.turnEnding);
            }
            characterService.regenerateAllCharacters(s.boardId);
            vehicleMaintenanceService.repairAllVehicles(s.boardId);
            unitRecoveryService.recoverAtTurnEnd(s.turnEnding, s.boardId);
            if (!outcome.finished()) {
                board.setCurrentTurn(s.turnEnding + 1);
            }
            boardRepository.save(board);
            turnService.publishTurn(s.boardId, board.getCurrentTurn());

            int newTurn = board.getCurrentTurn();
            List<SectorCapture> captures = result.getCaptures();
            TurnFinalizeResultDto dto = new TurnFinalizeResultDto();
            dto.setNewTurn(newTurn);
            dto.setTurnEnding(s.turnEnding);
            dto.setResolvedOrders(result.getResolved().size());
            dto.setBlockedOrders(result.getBlocked().size());
            dto.setConflictsResolved(s.resolvedConflicts.size());
            dto.setTransitCombatsCount(result.getTransitCombats().size());
            dto.setTransitCombats(toTransitDtoList(result.getTransitCombats()));
            dto.setCapturedSectors(toCaptureDtoList(captures));
            dto.setEliminatedPlayers(deathOrder.stream().map(Player::getName).toList());
            dto.setGameFinished(outcome.finished());
            dto.setWinnerPlayerId(outcome.winnerPlayerId());
            dto.setWinnerName(outcome.winnerName());
            dto.setWinnerPosthumous(outcome.posthumous());
            dto.setWinnerAdjusted(outcome.winnerAdjusted());
            String eliminatedSuffix = deathOrder.isEmpty() ? ""
                    : " " + String.join(", ", deathOrder.stream().map(Player::getName).toList())
                            + " éliminé(s).";
            if (outcome.finished()) {
                dto.setMessage("Partie terminée au tour " + s.turnEnding + " : "
                        + outcome.winnerName() + " remporte la partie." + eliminatedSuffix);
            } else {
                dto.setMessage(captures.isEmpty()
                        ? "Tour " + newTurn + " démarré." + eliminatedSuffix
                        : "Tour " + newTurn + " démarré — " + captures.size() + " secteur(s) capturé(s)."
                                + eliminatedSuffix);
            }
            turnJournalService.save(s.boardId, buildTurnJournal(s, dto, rangedCombats));
            return dto;
        } finally {
            turnLock.release(s.boardId);
            this.session = null;
        }
    }

    private List<Player> deathOrder(Session s, List<Player> eliminated) {
        Map<Long, Player> byId = new HashMap<>();
        eliminated.forEach(player -> byId.put(player.getId(), player));
        List<Player> ordered = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        s.resolvedConflicts.stream()
                .sorted(Comparator.comparingInt(ResolvedBattle::conflictId))
                .forEach(battle -> battle.eliminatedPlayers().forEach(elimination -> {
                    Player player = byId.get(elimination.playerId());
                    if (player != null && seen.add(player.getId())) {
                        ordered.add(player);
                    }
                }));
        eliminated.stream()
                .filter(player -> !seen.contains(player.getId()))
                .sorted(Comparator.comparing(Player::getId))
                .forEach(ordered::add);
        return ordered;
    }

    private List<EliminationHistoryService.EliminationSnapshot> eliminationSnapshots(
            List<Player> deathOrder, Session s) {
        List<EliminationHistoryService.EliminationSnapshot> snapshots = new ArrayList<>();
        for (Player victim : deathOrder) {
            Long killerId = s.eliminationKillers.containsKey(victim.getId())
                    ? s.eliminationKillers.get(victim.getId())
                    : victim.getEliminatedByPlayerId();
            EliminationCause cause = victim.getEliminationCause();
            ResolvedBattle context = eliminationContext(s, victim.getId(), cause);
            String killerName = killerId != null
                    ? playerRepository.findById(killerId).map(Player::getName).orElse(null)
                    : null;
            String characterName = victim.getCharacter() != null
                    ? victim.getCharacter().getName()
                    : null;
            snapshots.add(new EliminationHistoryService.EliminationSnapshot(victim.getId(),
                    victim.getName(), victim.getRace(), characterName, cause, killerId, killerName,
                    assists(context, victim.getId(), killerId),
                    context != null ? context.sectorNumber() : null));
        }
        return snapshots;
    }

    private ResolvedBattle eliminationContext(Session s, Long victimId, EliminationCause cause) {
        ResolvedBattle context = null;
        for (ResolvedBattle battle : s.resolvedConflicts.stream()
                .sorted(Comparator.comparingInt(ResolvedBattle::conflictId)).toList()) {
            boolean match = battle.eliminatedPlayers().stream()
                    .anyMatch(elimination -> elimination.playerId().equals(victimId)
                            && elimination.cause().equals(cause.name()));
            if (match) {
                context = battle;
            }
        }
        return context;
    }

    private static List<Long> assists(ResolvedBattle battle, Long victimId, Long killerId) {
        if (battle == null || killerId == null) {
            return List.of();
        }
        List<Long> killerCamp = battle.camps().stream()
                .filter(camp -> camp.contains(killerId))
                .findFirst()
                .orElse(null);
        if (killerCamp == null) {
            return List.of();
        }
        return killerCamp.stream()
                .filter(playerId -> !playerId.equals(killerId) && !playerId.equals(victimId))
                .distinct()
                .sorted()
                .toList();
    }

    private TurnJournalDto buildTurnJournal(Session s, TurnFinalizeResultDto finalize,
                                            List<BattleReportDetailDto> rangedCombats) {
        Function<Long, String> names = resolveNamesForSession(s);
        List<ResolvedBattleDto> battles = s.resolvedConflicts.stream()
                .map(rb -> toBattleDto(rb, names))
                .toList();
        TurnJournalDto.SummaryDto summary = new TurnJournalDto.SummaryDto();
        summary.setBattles(battles.size());
        summary.setRangedCombats(rangedCombats.size());
        summary.setResolvedOrders(finalize.getResolvedOrders());
        summary.setBlockedOrders(finalize.getBlockedOrders());
        summary.setCaptures(finalize.getCapturedSectors().size());
        summary.setEliminatedPlayers(finalize.getEliminatedPlayers().size());
        summary.setTransitCombats(finalize.getTransitCombatsCount());

        TurnJournalDto journal = new TurnJournalDto();
        journal.setTurn(s.turnEnding);
        journal.setExportedAt(Instant.now().toString());
        journal.setFinalized(true);
        journal.setSummary(summary);
        journal.setBattles(battles);
        journal.setRangedCombats(rangedCombats);
        journal.setCaptures(finalize.getCapturedSectors());
        journal.setTransitCombats(finalize.getTransitCombats());
        journal.setEliminatedPlayers(finalize.getEliminatedPlayers());
        journal.setGameFinished(finalize.isGameFinished());
        journal.setWinnerPlayerId(finalize.getWinnerPlayerId());
        journal.setWinnerName(finalize.getWinnerName());
        journal.setWinnerPosthumous(finalize.isWinnerPosthumous());
        journal.setWinnerAdjusted(finalize.isWinnerAdjusted());
        journal.setGazetteDraftTurn(finalize.getGazetteDraftTurn());
        return journal;
    }

    /** Abandon soft : libère le verrou sans rollback des entités déplacées ni des combats résolus. */
    public synchronized void abort(Long boardId) {
        Session s = this.session;
        if (s == null || !s.boardId.equals(boardId)) {
            return;
        }
        turnLock.release(s.boardId);
        this.session = null;
    }

    @Transactional(readOnly = true)
    public synchronized TurnResolutionStateDto getState(Long boardId) {
        Session s = this.session;
        if (s == null || !s.boardId.equals(boardId)) {
            TurnResolutionStateDto dto = new TurnResolutionStateDto();
            dto.setActive(false);
            return dto;
        }
        return toStateDto(s);
    }

    private Session requireSession(Long boardId) {
        Session s = this.session;
        if (s == null || !s.boardId.equals(boardId)) {
            throw new IllegalStateException("Aucune session de résolution active sur cette partie");
        }
        return s;
    }

    private Board boardOf(Session s) {
        return boardOf(s.boardId);
    }

    private Board boardOf(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalStateException("Plateau " + boardId + " introuvable"));
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

    private List<TransitCombatResultDto> toTransitDtoList(List<TransitCombatResult> combats) {
        return combats.stream()
                .map(combat -> {
                    TransitCombatResultDto dto = new TransitCombatResultDto();
                    dto.setSectorNumber(combat.sectorNumber());
                    dto.setVehicleId(combat.vehicleId());
                    dto.setVehicleName(combat.vehicleName());
                    dto.setVehicleType(combat.vehicleType());
                    dto.setOwnerPlayerId(combat.ownerPlayerId());
                    dto.setVehicleFired(combat.vehicleFired());
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
        dto.setForces(rb.forces());
        dto.setHighlights(rb.highlights());
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
        final Long boardId;
        final MovementService.ResolutionContext ctx;
        final int turnEnding;
        final Map<Integer, Long> sectorOwners;
        final long startedAt = System.currentTimeMillis();
        final List<PendingConflict> pendingConflicts = new ArrayList<>();
        final List<ResolvedBattle> resolvedConflicts = new ArrayList<>();
        final Map<Long, PendingElimination> pendingEliminations = new LinkedHashMap<>();
        final Map<Long, Long> eliminationKillers = new HashMap<>();
        int conflictIdSeq = 0;

        Session(Long boardId, MovementService.ResolutionContext ctx, int turnEnding,
                Map<Integer, Long> sectorOwners) {
            this.boardId = boardId;
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

    private record ResolvedBattle(int conflictId, int sectorNumber, boolean standoff,
                                  List<Long> participantPlayerIds, List<List<Long>> camps,
                                  List<Long> attackerCampPlayerIds, List<Long> defenderCampPlayerIds,
                                  List<Long> winningCampPlayerIds,
                                  Long attackerPlayerId, Long defenderPlayerId, boolean success, String message,
                                  Long winnerId, int attackerCasualties, int defenderCasualties,
                                  int attackerInjured, int defenderInjured, int capturedBuildings,
                                  boolean attackerCharacterLost, boolean defenderCharacterLost,
                                  boolean defenderHeadquartersCaptured,
                                  List<EliminatedPlayer> eliminatedPlayers,
                                  List<CombatService.StandoffBattleResult.PlayerOutcome> outcomes,
                                  List<BattleLogEntry> battleLog,
                                  List<BattleReportDto.PlayerForceDto> forces,
                                  List<BattleHighlightDto> highlights) {

        private record EliminatedPlayer(Long playerId, String playerName, String cause) {
        }

        private static List<EliminatedPlayer> eliminations(List<Player> players) {
            return players.stream()
                    .filter(Player::isEliminated)
                    .map(player -> new EliminatedPlayer(player.getId(), player.getName(),
                            player.getEliminationCause().name()))
                    .toList();
        }

        static ResolvedBattle duel(int conflictId, int sectorNumber, List<Player> attackerCamp,
                                   List<Player> defenderCamp, CombatService.SectorBattleResult r) {
            List<Long> attackerIds = attackerCamp.stream().map(Player::getId).toList();
            List<Long> defenderIds = defenderCamp.stream().map(Player::getId).toList();
            List<Long> winning = r.winner() == null ? List.of()
                    : attackerIds.contains(r.winner().getId()) ? attackerIds : defenderIds;
            List<Long> participants = new ArrayList<>(attackerIds);
            participants.addAll(defenderIds);
            List<Player> combatants = new ArrayList<>(attackerCamp);
            combatants.addAll(defenderCamp);
            return new ResolvedBattle(conflictId, sectorNumber, false, participants,
                    List.of(attackerIds, defenderIds), attackerIds, defenderIds, winning,
                    attackerIds.getFirst(), defenderIds.getFirst(), r.success(), r.message(),
                    r.winner() != null ? r.winner().getId() : null,
                    r.attackerCasualties().size(), r.defenderCasualties().size(),
                    r.attackerInjured().size(), r.defenderInjured().size(),
                    r.capturedBuildings(), r.attackerCharacterLost(), r.defenderCharacterLost(),
                    r.defenderHeadquartersCaptured(), eliminations(combatants), null, r.battleLog(),
                    r.forces(), r.highlights());
        }

        static ResolvedBattle standoff(int conflictId, int sectorNumber, List<List<Player>> camps,
                                       CombatService.StandoffBattleResult r) {
            List<Long> participants = camps.stream().flatMap(List::stream).map(Player::getId).toList();
            List<List<Long>> campIds = camps.stream()
                    .map(camp -> camp.stream().map(Player::getId).toList())
                    .toList();
            List<Long> winning = r.winner() == null ? List.of()
                    : camps.stream()
                            .filter(camp -> camp.stream()
                                    .anyMatch(player -> player.getId().equals(r.winner().getId())))
                            .findFirst()
                            .map(camp -> camp.stream().map(Player::getId).toList())
                            .orElse(List.of(r.winner().getId()));
            List<Player> combatants = camps.stream().flatMap(List::stream).toList();
            return new ResolvedBattle(conflictId, sectorNumber, true, participants, campIds,
                    List.of(), List.of(), winning,
                    null, null, r.success(), r.message(),
                    r.winner() != null ? r.winner().getId() : null,
                    0, 0, 0, 0, r.capturedBuildings(), false, false, false,
                    eliminations(combatants), r.outcomes(), r.battleLog(), r.forces(), r.highlights());
        }
    }
}