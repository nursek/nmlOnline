package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BattleReportDetailDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.battle.Battle;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.battle.RandomSource;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sniper.SniperOrder;
import com.mg.nmlonline.domain.model.sniper.SniperOrderStatus;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.SniperOrderRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SniperService {

    private final SniperOrderRepository sniperOrderRepository;
    private final UnitRepository unitRepository;
    private final AllianceGraph allianceGraph;
    private final PlayerRepository playerRepository;
    private final CombatService combatService;
    private final BattleReportService battleReportService;
    private final RandomSource randomSource;

    public SniperService(SniperOrderRepository sniperOrderRepository,
                         UnitRepository unitRepository,
                         AllianceGraph allianceGraph,
                         PlayerRepository playerRepository,
                         CombatService combatService,
                         BattleReportService battleReportService,
                         RandomSource randomSource) {
        this.sniperOrderRepository = sniperOrderRepository;
        this.unitRepository = unitRepository;
        this.allianceGraph = allianceGraph;
        this.playerRepository = playerRepository;
        this.combatService = combatService;
        this.battleReportService = battleReportService;
        this.randomSource = randomSource;
    }

    public record IndependentSupport(Long playerId, List<Unit> units, List<SniperOrder> orders) {
    }

    public record DuelSupport(List<Unit> attackers, List<Unit> defenders,
                              List<IndependentSupport> independents, List<SniperOrder> resolvedOrders) {
    }

    public record StandoffSupport(Map<Integer, List<Unit>> byCamp, List<IndependentSupport> independents,
                                  List<SniperOrder> resolvedOrders) {
    }

    public DuelSupport collectDuelSupport(Long boardId, int turn, int targetSector, List<Long> attackerIds,
                                          List<Long> defenderIds, Set<Integer> battleSectors) {
        List<Unit> attackers = new ArrayList<>();
        List<Unit> defenders = new ArrayList<>();
        Map<Long, List<Unit>> independentUnits = new LinkedHashMap<>();
        Map<Long, List<SniperOrder>> independentOrders = new LinkedHashMap<>();
        List<SniperOrder> resolved = new ArrayList<>();
        for (SniperOrder order : pendingForSector(boardId, turn, targetSector)) {
            Unit unit = eligibleUnit(order, battleSectors);
            if (unit == null) {
                continue;
            }
            if (inCamp(boardId, order.getPlayerId(), attackerIds)) {
                attackers.add(unit);
                resolved.add(order);
            } else if (inCamp(boardId, order.getPlayerId(), defenderIds)) {
                defenders.add(unit);
                resolved.add(order);
            } else {
                independentUnits.computeIfAbsent(order.getPlayerId(), key -> new ArrayList<>()).add(unit);
                independentOrders.computeIfAbsent(order.getPlayerId(), key -> new ArrayList<>()).add(order);
                resolved.add(order);
            }
        }
        return new DuelSupport(attackers, defenders, independents(independentUnits, independentOrders), resolved);
    }

    public StandoffSupport collectStandoffSupport(Long boardId, int turn, int targetSector,
                                                  List<List<Long>> camps, Set<Integer> battleSectors) {
        Map<Integer, List<Unit>> byCamp = new HashMap<>();
        Map<Long, List<Unit>> independentUnits = new LinkedHashMap<>();
        Map<Long, List<SniperOrder>> independentOrders = new LinkedHashMap<>();
        List<SniperOrder> resolved = new ArrayList<>();
        for (SniperOrder order : pendingForSector(boardId, turn, targetSector)) {
            Unit unit = eligibleUnit(order, battleSectors);
            if (unit == null) {
                continue;
            }
            int camp = campIndex(boardId, order.getPlayerId(), camps);
            if (camp < 0) {
                independentUnits.computeIfAbsent(order.getPlayerId(), key -> new ArrayList<>()).add(unit);
                independentOrders.computeIfAbsent(order.getPlayerId(), key -> new ArrayList<>()).add(order);
                resolved.add(order);
                continue;
            }
            byCamp.computeIfAbsent(camp, key -> new ArrayList<>()).add(unit);
            resolved.add(order);
        }
        return new StandoffSupport(byCamp, independents(independentUnits, independentOrders), resolved);
    }

    private List<IndependentSupport> independents(Map<Long, List<Unit>> unitsByPlayer,
                                                  Map<Long, List<SniperOrder>> ordersByPlayer) {
        List<IndependentSupport> independents = new ArrayList<>();
        unitsByPlayer.forEach((playerId, units) ->
                independents.add(new IndependentSupport(playerId, units, ordersByPlayer.get(playerId))));
        return independents;
    }

    public void markResolved(Collection<SniperOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        // Les unités détruites emportent leurs ordres (FK CASCADE) : flusher avant de filtrer les orphelins.
        sniperOrderRepository.flush();
        List<SniperOrder> alive = orders.stream()
                .filter(order -> unitRepository.findById(order.getUnitId()).isPresent())
                .toList();
        alive.forEach(SniperOrder::resolve);
        sniperOrderRepository.saveAll(alive);
    }

    /** Traite les ordres restants sans combat au sol : tir isolé, avec contre-tir des snipers inactifs ciblés. */
    public List<BattleReportDetailDto> resolveRemainingFire(int turn, Board board, Set<Integer> battleSectors) {
        List<SniperOrder> pending =
                sniperOrderRepository.findByBoardIdAndTurnAndStatus(board.getId(), turn, SniperOrderStatus.PENDING);
        if (pending.isEmpty()) {
            return List.of();
        }
        Set<Long> exhaustedUnitIds = pending.stream()
                .map(SniperOrder::getUnitId)
                .collect(Collectors.toSet());
        // Un ordre RESOLVED a déjà tiré ce tour : il ne peut pas contre-tirer.
        sniperOrderRepository.findByBoardIdAndTurnAndStatus(board.getId(), turn, SniperOrderStatus.RESOLVED).stream()
                .map(SniperOrder::getUnitId)
                .forEach(exhaustedUnitIds::add);
        Set<Long> destroyedUnitIds = new HashSet<>();
        Set<Long> awardedUnitIds = new HashSet<>();
        List<OrderContext> validOrders = new ArrayList<>();
        List<InvalidOrder> invalidOrders = new ArrayList<>();
        Map<Integer, List<OrderContext>> byTarget = new LinkedHashMap<>();
        for (SniperOrder order : pending) {
            Unit unit = unitRepository.findById(order.getUnitId()).orElse(null);
            String reason = invalidReason(unit, order, battleSectors);
            if (reason != null) {
                invalidOrders.add(new InvalidOrder(order, reason));
                continue;
            }
            if (battleSectors.contains(order.getTargetSectorNumber())) {
                invalidOrders.add(new InvalidOrder(order, "Combat non résolu sur le secteur cible."));
                continue;
            }
            if (board.getSector(order.getTargetSectorNumber()) == null) {
                invalidOrders.add(new InvalidOrder(order, "Secteur cible introuvable."));
                continue;
            }
            OrderContext context = new OrderContext(order, unit);
            validOrders.add(context);
            byTarget.computeIfAbsent(order.getTargetSectorNumber(), key -> new ArrayList<>()).add(context);
        }
        List<BattleReportDetailDto> reports = new ArrayList<>();
        byTarget.forEach((targetSector, orders) -> {
            BattleReportDetailDto report = resolveStandaloneSector(turn, board, targetSector, orders,
                    exhaustedUnitIds, destroyedUnitIds, awardedUnitIds);
            if (report != null) {
                reports.add(report);
            }
        });
        invalidOrders.stream()
                .filter(invalid -> !destroyedUnitIds.contains(invalid.order().getUnitId()))
                .forEach(invalid -> cancel(invalid.order(), invalid.reason()));
        markResolvedGroup(validOrders, destroyedUnitIds);
        return reports;
    }

    private BattleReportDetailDto resolveStandaloneSector(int turn, Board board, int targetSectorNumber,
                                                          List<OrderContext> orders, Set<Long> exhaustedUnitIds,
                                                          Set<Long> destroyedUnitIds, Set<Long> awardedUnitIds) {
        Sector sector = board.getSector(targetSectorNumber);
        if (sector == null) {
            return null;
        }
        Map<Long, List<OrderContext>> byPlayer = new LinkedHashMap<>();
        for (OrderContext context : orders) {
            byPlayer.computeIfAbsent(context.order().getPlayerId(), key -> new ArrayList<>()).add(context);
        }
        Set<Long> counteredIds = new HashSet<>();
        List<CombatService.CasualtyInfo> casualties = new ArrayList<>();
        List<CombatService.ExperienceGain> experienceGains = new ArrayList<>();
        Set<Long> participantIds = new LinkedHashSet<>();
        Map<Long, Boolean> characterLost = new HashMap<>();
        List<String> summaries = new ArrayList<>();
        addParticipant(participantIds, sector.getOwnerId());
        sector.getCombatEntities().forEach(entity -> addParticipant(participantIds, entity.getPlayerId()));

        List<Map.Entry<Long, List<OrderContext>>> firingGroups = new ArrayList<>();
        Set<Long> firingSides = new HashSet<>();
        List<Unit> shooters = new ArrayList<>();
        for (Map.Entry<Long, List<OrderContext>> group : byPlayer.entrySet()) {
            List<Unit> groupShooters = group.getValue().stream().map(OrderContext::unit).toList();
            groupShooters.forEach(shooter -> addParticipant(participantIds, shooter.getPlayerId()));
            Set<Long> shooterSide = new HashSet<>(allianceGraph.alliedPlayerIds(board.getId(), group.getKey()));
            shooterSide.add(group.getKey());
            Long ownerId = sector.getOwnerId();
            if (ownerId != null && shooterSide.contains(ownerId)) {
                continue;
            }
            firingGroups.add(group);
            firingSides.addAll(shooterSide);
            shooters.addAll(groupShooters);
        }
        if (firingGroups.isEmpty()) {
            return null;
        }
        // Les salves de tous les joueurs du même secteur arrivent ensemble : une seule résolution.
        List<CombatEntity> targets = sector.getCombatEntities().stream()
                .filter(entity -> !entity.isDestroyed())
                .filter(entity -> entity.getPlayerId() == null
                        || !firingSides.contains(entity.getPlayerId()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (targets.isEmpty()) {
            return null;
        }
        Set<Long> beforeIds = targets.stream().map(CombatEntity::getId).collect(Collectors.toSet());
        Battle battle = new Battle();
        battle.setRandomSource(randomSource);
        battle.rangedOnlyConfiguration(shooters, targets, sector.getName());
        Set<Long> survivorIds = targets.stream().map(CombatEntity::getId).collect(Collectors.toSet());

        List<Unit> counterSnipers = battle.getTargetedSnipers().stream()
                .filter(Unit::isSniper)
                .filter(sniper -> sniper.getSector() != null
                        && sniper.getSector().getNumber() == targetSectorNumber)
                .filter(sniper -> sniper.getId() != null && !counteredIds.contains(sniper.getId()))
                .filter(sniper -> !exhaustedUnitIds.contains(sniper.getId()))
                .toList();
        if (!counterSnipers.isEmpty()) {
            counterSnipers.forEach(sniper -> counteredIds.add(sniper.getId()));
            battle.counterFireConfiguration(counterSnipers, shooters, sector.getName());
        }

        Map<Long, Player> players = loadPlayers(participantIds);
        List<CombatEntity> targetCasualties = combatService.removeCasualties(
                sector, beforeIds, survivorIds, players, characterLost, turn);
        Set<Long> shooterPlayerIds = firingGroups.stream().map(Map.Entry::getKey).collect(Collectors.toSet());
        // Salves combinées : avec plusieurs tireurs, le tireur létal n'est pas identifiable, le MJ arbitre.
        if (shooterPlayerIds.size() == 1) {
            attributeSniperEliminations(targetCasualties, players, shooterPlayerIds.iterator().next());
        }
        collectDestroyed(targetCasualties, destroyedUnitIds);
        casualties.addAll(toCasualties(targetCasualties));
        targets.stream()
                .filter(Unit.class::isInstance)
                .map(Unit.class::cast)
                .forEach(unit -> unit.recordCombatParticipation(turn));
        shooters.stream()
                .filter(shooter -> !battle.getRangedDestroyed().contains(shooter))
                .forEach(shooter -> shooter.recordCombatParticipation(turn));
        for (CombatEntity survivor : targets) {
            combatService.regenerateAfterBattle(survivor);
        }
        sector.recalculateMilitaryPower();
        applyCounterCasualties(turn, board, shooters, battle.getRangedDestroyed(), players,
                characterLost, casualties, destroyedUnitIds);
        experienceGains.addAll(awardSniperExperience(battle, awardedUnitIds));
        summaries.addAll(battle.getLog().stream()
                .map(BattleLogEntry::message)
                .filter(message -> message.contains("PdF"))
                .toList());

        if (summaries.isEmpty() && casualties.isEmpty()) {
            return null;
        }
        List<Player> participants = participantIds.isEmpty()
                ? List.of()
                : playerRepository.findAllById(participantIds);
        return battleReportService.saveRangedReport(board.getId(), turn, targetSectorNumber, participants,
                casualties, experienceGains, String.join(" ; ", summaries), characterLost);
    }

    private void applyCounterCasualties(int turn, Board board, List<Unit> shooters,
                                        Set<CombatEntity> destroyed, Map<Long, Player> players,
                                        Map<Long, Boolean> characterLost,
                                        List<CombatService.CasualtyInfo> casualties,
                                        Set<Long> destroyedUnitIds) {
        Map<Integer, List<Unit>> bySector = shooters.stream()
                .filter(shooter -> shooter.getSector() != null && shooter.getId() != null)
                .collect(Collectors.groupingBy(shooter -> shooter.getSector().getNumber(),
                        LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Integer, List<Unit>> entry : bySector.entrySet()) {
            Sector sector = board.getSector(entry.getKey());
            if (sector == null) {
                continue;
            }
            Set<Long> beforeIds = entry.getValue().stream().map(Unit::getId).collect(Collectors.toSet());
            Set<Long> survivorIds = entry.getValue().stream()
                    .filter(shooter -> !destroyed.contains(shooter))
                    .map(Unit::getId)
                    .collect(Collectors.toSet());
            List<CombatEntity> counterCasualties = combatService.removeCasualties(
                    sector, beforeIds, survivorIds, players, characterLost, turn);
            collectDestroyed(counterCasualties, destroyedUnitIds);
            casualties.addAll(toCasualties(counterCasualties));
            sector.recalculateMilitaryPower();
        }
    }

    private Map<Long, Player> loadPlayers(Set<Long> participantIds) {
        if (participantIds.isEmpty()) {
            return Map.of();
        }
        return playerRepository.findAllById(participantIds).stream()
                .collect(Collectors.toMap(Player::getId, player -> player));
    }

    private List<CombatService.CasualtyInfo> toCasualties(List<CombatEntity> entities) {
        return entities.stream().map(combatService::toCasualtyInfo).toList();
    }

    private static void collectDestroyed(List<CombatEntity> entities, Set<Long> destroyedUnitIds) {
        entities.stream()
                .map(CombatEntity::getId)
                .filter(Objects::nonNull)
                .forEach(destroyedUnitIds::add);
    }

    private List<CombatService.ExperienceGain> awardSniperExperience(Battle battle, Set<Long> awardedUnitIds) {
        Map<Long, Unit> candidates = new LinkedHashMap<>();
        battle.getTargetedSnipers().stream()
                .filter(unit -> unit.getId() != null)
                .forEach(unit -> candidates.putIfAbsent(unit.getId(), unit));
        List<CombatService.ExperienceGain> gains = new ArrayList<>();
        for (Unit unit : candidates.values()) {
            if (awardedUnitIds.contains(unit.getId()) || battle.getRangedDestroyed().contains(unit)) {
                continue;
            }
            awardedUnitIds.add(unit.getId());
            gains.add(gain(unit, 1));
        }
        return gains;
    }

    private CombatService.ExperienceGain gain(Unit unit, double amount) {
        UnitType typeBefore = unit.getType();
        double experienceBefore = unit.getExperience();
        unit.gainExperience(amount);
        return new CombatService.ExperienceGain(unit.getPlayerId(), unit.getId(), unit.getNumber(),
                typeBefore, experienceBefore, amount, unit.getType(), unit.getExperience());
    }

    private static void attributeSniperEliminations(List<CombatEntity> casualties,
                                                    Map<Long, Player> players, Long shooterPlayerId) {
        for (CombatEntity casualty : casualties) {
            if (!(casualty instanceof GameCharacter character)) {
                continue;
            }
            Player victim = players.get(character.getPlayerId());
            if (victim != null && victim.isEliminated()
                    && victim.getEliminationCause() == EliminationCause.MAIN_CHARACTER_KILLED) {
                victim.setEliminatedByPlayerId(shooterPlayerId);
            }
        }
    }

    private void markResolvedGroup(List<OrderContext> group, Set<Long> destroyedUnitIds) {
        markResolved(group.stream()
                .map(OrderContext::order)
                .filter(order -> !destroyedUnitIds.contains(order.getUnitId()))
                .toList());
    }

    private static void addParticipant(Set<Long> ids, Long playerId) {
        if (playerId != null) {
            ids.add(playerId);
        }
    }

    private record OrderContext(SniperOrder order, Unit unit) {
    }

    private record InvalidOrder(SniperOrder order, String reason) {
    }

    private List<SniperOrder> pendingForSector(Long boardId, int turn, int targetSector) {
        return sniperOrderRepository.findByBoardIdAndTurnAndStatus(boardId, turn, SniperOrderStatus.PENDING).stream()
                .filter(order -> order.getTargetSectorNumber() == targetSector)
                .toList();
    }

    private Unit eligibleUnit(SniperOrder order, Set<Integer> battleSectors) {
        Unit unit = unitRepository.findById(order.getUnitId()).orElse(null);
        String reason = invalidReason(unit, order, battleSectors);
        if (reason != null) {
            cancel(order, reason);
            return null;
        }
        return unit;
    }

    private String invalidReason(Unit unit, SniperOrder order, Set<Integer> battleSectors) {
        if (unit == null || !unit.isSniper()) {
            return "Unité sniper introuvable.";
        }
        if (unit.getSector() == null || unit.getSector().getNumber() != order.getFromSectorNumber()) {
            return "L'unité a quitté son secteur d'origine.";
        }
        if (battleSectors.contains(order.getFromSectorNumber())) {
            return "Secteur d'origine attaqué : le sniper se défend sur place.";
        }
        return null;
    }

    private void cancel(SniperOrder order, String reason) {
        order.setStatusMessage(reason);
        order.cancel();
        sniperOrderRepository.save(order);
    }

    private boolean inCamp(Long boardId, Long playerId, List<Long> camp) {
        if (camp.contains(playerId)) {
            return true;
        }
        return camp.stream().anyMatch(campPlayerId -> allianceGraph.areAllied(boardId, campPlayerId, playerId));
    }

    private int campIndex(Long boardId, Long playerId, List<List<Long>> camps) {
        for (int i = 0; i < camps.size(); i++) {
            if (inCamp(boardId, playerId, camps.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
