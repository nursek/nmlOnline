package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BattleForcesBuilder {

    private BattleForcesBuilder() {
    }

    public static List<BattleReportDto.PlayerForceDto> build(List<Player> camp, List<CombatEntity> fighters,
                                                             String side) {
        Set<CombatEntity> embarked = new HashSet<>();
        fighters.stream()
                .filter(Vehicle.class::isInstance)
                .map(Vehicle.class::cast)
                .forEach(vehicle -> embarked.addAll(vehicle.getAllOccupants()));

        List<BattleReportDto.PlayerForceDto> forces = new ArrayList<>();
        for (Player player : camp) {
            List<CombatEntity> owned = fighters.stream()
                    .filter(entity -> player.getId().equals(entity.getPlayerId()))
                    .filter(entity -> !embarked.contains(entity))
                    .toList();
            BattleReportDto.PlayerForceDto force = new BattleReportDto.PlayerForceDto();
            force.setPlayerId(player.getId());
            force.setPlayerName(player.getName());
            force.setSide(side);
            force.setCharacters(owned.stream()
                    .filter(GameCharacter.class::isInstance)
                    .map(BattleForcesBuilder::unitForce)
                    .toList());
            force.setVehicles(owned.stream()
                    .filter(Vehicle.class::isInstance)
                    .map(Vehicle.class::cast)
                    .map(BattleForcesBuilder::vehicleForce)
                    .toList());
            force.setInfantry(owned.stream()
                    .filter(Unit.class::isInstance)
                    .map(BattleForcesBuilder::unitForce)
                    .toList());
            force.setBuildings(owned.stream()
                    .filter(Building.class::isInstance)
                    .map(Building.class::cast)
                    .map(BattleForcesBuilder::buildingForce)
                    .toList());
            forces.add(force);
        }
        return forces;
    }

    private static BattleReportDto.UnitForceDto unitForce(CombatEntity entity) {
        BattleReportDto.UnitForceDto force = new BattleReportDto.UnitForceDto();
        force.setDisplayName(entity.getDisplayName());
        force.setInjured(entity.isInjured());
        if (entity instanceof Unit unit) {
            force.setNumber(unit.getNumber());
            force.setType(unit.getType() != null ? unit.getType().name() : null);
            force.setExperience(unit.getExperience());
        }
        return force;
    }

    private static BattleReportDto.VehicleForceDto vehicleForce(Vehicle vehicle) {
        BattleReportDto.VehicleForceDto force = new BattleReportDto.VehicleForceDto();
        force.setDisplayName(vehicle.getDisplayName());
        force.setAerial(vehicle.isAerial());
        force.setCrew(vehicle.getAllOccupants().stream()
                .map(BattleForcesBuilder::unitForce)
                .toList());
        return force;
    }

    private static BattleReportDto.BuildingForceDto buildingForce(Building building) {
        BattleReportDto.BuildingForceDto force = new BattleReportDto.BuildingForceDto();
        force.setDisplayName(building.getDisplayName());
        force.setCategory(building.getBuildingType() != null ? building.getBuildingType().name() : null);
        return force;
    }
}
