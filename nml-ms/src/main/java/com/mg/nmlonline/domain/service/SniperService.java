package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.sniper.SniperOrder;
import com.mg.nmlonline.domain.model.sniper.SniperOrderStatus;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.infrastructure.repository.SniperOrderRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class SniperService {

    private final SniperOrderRepository sniperOrderRepository;
    private final UnitRepository unitRepository;
    private final AllianceGraph allianceGraph;

    public SniperService(SniperOrderRepository sniperOrderRepository,
                         UnitRepository unitRepository,
                         AllianceGraph allianceGraph) {
        this.sniperOrderRepository = sniperOrderRepository;
        this.unitRepository = unitRepository;
        this.allianceGraph = allianceGraph;
    }

    public record DuelSupport(List<Unit> attackers, List<Unit> defenders, List<SniperOrder> resolvedOrders) {
    }

    public record StandoffSupport(Map<Integer, List<Unit>> byCamp, List<SniperOrder> resolvedOrders) {
    }

    public DuelSupport collectDuelSupport(int turn, int targetSector, List<Long> attackerIds,
                                          List<Long> defenderIds, Set<Integer> battleSectors) {
        List<Unit> attackers = new ArrayList<>();
        List<Unit> defenders = new ArrayList<>();
        List<SniperOrder> resolved = new ArrayList<>();
        for (SniperOrder order : pendingForSector(turn, targetSector)) {
            Unit unit = eligibleUnit(order, battleSectors);
            if (unit == null) {
                continue;
            }
            if (inCamp(order.getPlayerId(), attackerIds)) {
                attackers.add(unit);
                resolved.add(order);
            } else if (inCamp(order.getPlayerId(), defenderIds)) {
                defenders.add(unit);
                resolved.add(order);
            }
        }
        return new DuelSupport(attackers, defenders, resolved);
    }

    public StandoffSupport collectStandoffSupport(int turn, int targetSector, List<List<Long>> camps,
                                                  Set<Integer> battleSectors) {
        Map<Integer, List<Unit>> byCamp = new HashMap<>();
        List<SniperOrder> resolved = new ArrayList<>();
        for (SniperOrder order : pendingForSector(turn, targetSector)) {
            Unit unit = eligibleUnit(order, battleSectors);
            if (unit == null) {
                continue;
            }
            int camp = campIndex(order.getPlayerId(), camps);
            if (camp < 0) {
                continue;
            }
            byCamp.computeIfAbsent(camp, key -> new ArrayList<>()).add(unit);
            resolved.add(order);
        }
        return new StandoffSupport(byCamp, resolved);
    }

    public void markResolved(Collection<SniperOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        orders.forEach(SniperOrder::resolve);
        sniperOrderRepository.saveAll(orders);
    }

    private List<SniperOrder> pendingForSector(int turn, int targetSector) {
        return sniperOrderRepository.findByTurnAndStatus(turn, SniperOrderStatus.PENDING).stream()
                .filter(order -> order.getTargetSectorNumber() == targetSector)
                .toList();
    }

    private Unit eligibleUnit(SniperOrder order, Set<Integer> battleSectors) {
        Unit unit = unitRepository.findById(order.getUnitId()).orElse(null);
        if (unit == null || !unit.isSniper()) {
            cancel(order, "Unité sniper introuvable.");
            return null;
        }
        if (unit.getSector() == null || unit.getSector().getNumber() != order.getFromSectorNumber()) {
            cancel(order, "L'unité a quitté son secteur d'origine.");
            return null;
        }
        if (battleSectors.contains(order.getFromSectorNumber())) {
            cancel(order, "Secteur d'origine attaqué : le sniper se défend sur place.");
            return null;
        }
        return unit;
    }

    private void cancel(SniperOrder order, String reason) {
        order.setStatusMessage(reason);
        order.cancel();
        sniperOrderRepository.save(order);
    }

    private boolean inCamp(Long playerId, List<Long> camp) {
        if (camp.contains(playerId)) {
            return true;
        }
        return camp.stream().anyMatch(campPlayerId -> allianceGraph.areAllied(campPlayerId, playerId));
    }

    private int campIndex(Long playerId, List<List<Long>> camps) {
        for (int i = 0; i < camps.size(); i++) {
            if (inCamp(playerId, camps.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
