package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            List<CombatEntity> allOwned = fighters.stream()
                    .filter(entity -> player.getId().equals(entity.getPlayerId()))
                    .toList();
            List<CombatEntity> owned = allOwned.stream()
                    .filter(entity -> !embarked.contains(entity))
                    .toList();
            BattleReportDto.PlayerForceDto force = new BattleReportDto.PlayerForceDto();
            force.setPlayerId(player.getId());
            force.setPlayerName(player.getName());
            force.setSide(side);
            force.setCharacters(owned.stream()
                    .filter(GameCharacter.class::isInstance)
                    .map(character -> unitForce(character, player))
                    .toList());
            force.setVehicles(owned.stream()
                    .filter(Vehicle.class::isInstance)
                    .map(Vehicle.class::cast)
                    .map(vehicle -> vehicleForce(vehicle, player))
                    .toList());
            force.setInfantry(owned.stream()
                    .filter(Unit.class::isInstance)
                    .map(unit -> unitForce(unit, player))
                    .toList());
            force.setBuildings(owned.stream()
                    .filter(Building.class::isInstance)
                    .map(Building.class::cast)
                    .map(BattleForcesBuilder::buildingForce)
                    .toList());
            force.setTotals(totalOf(allOwned));
            forces.add(force);
        }
        return forces;
    }

    private static BattleReportDto.UnitForceDto unitForce(CombatEntity entity, Player player) {
        BattleReportDto.UnitForceDto force = new BattleReportDto.UnitForceDto();
        force.setDisplayName(entity.getDisplayName());
        force.setInjured(entity.isInjured());
        force.setStats(statsOf(entity));
        if (entity instanceof Unit unit) {
            force.setNumber(unit.getNumber());
            force.setType(unit.getType() != null ? unit.getType().name() : null);
            force.setExperience(unit.getExperience());
            force.setClassCode(classCode(unit));
            force.setEquipmentNames(equipmentNames(unit, player));
        }
        return force;
    }

    private static BattleReportDto.VehicleForceDto vehicleForce(Vehicle vehicle, Player player) {
        BattleReportDto.VehicleForceDto force = new BattleReportDto.VehicleForceDto();
        force.setDisplayName(vehicle.getDisplayName(player.getRace()));
        force.setAerial(vehicle.isAerial());
        force.setCrew(vehicle.getAllOccupants().stream()
                .map(occupant -> unitForce(occupant, player))
                .toList());
        force.setStats(statsOf(vehicle));
        return force;
    }

    private static BattleReportDto.BuildingForceDto buildingForce(Building building) {
        BattleReportDto.BuildingForceDto force = new BattleReportDto.BuildingForceDto();
        force.setDisplayName(building.getDisplayName());
        force.setCategory(building.getBuildingType() != null ? building.getBuildingType().name() : null);
        force.setStats(statsOf(building));
        return force;
    }

    private static String classCode(Unit unit) {
        return unit.getClasses().stream()
                .map(UnitClass::getCode)
                .sorted()
                .collect(Collectors.joining());
    }

    private static List<String> equipmentNames(Unit unit, Player player) {
        return unit.getEquipments().stream()
                .map(equipment -> equipmentName(equipment, player.getRace()))
                .toList();
    }

    private static String equipmentName(Equipment equipment, PlayerRace race) {
        String localized = race != null ? equipment.getDisplayNames().get(race) : null;
        return localized != null ? localized : equipment.getName();
    }

    private static BattleReportDto.StatsDto statsOf(CombatEntity entity) {
        BattleReportDto.StatsDto stats = new BattleReportDto.StatsDto();
        stats.setAttack(entity.getAttack());
        stats.setPdf(entity.getPdf());
        stats.setPdc(entity.getPdc());
        stats.setDefense(entity.getDefense());
        stats.setArmor(entity.getArmor());
        stats.setEvasion(entity.getEvasion());
        return stats;
    }

    private static BattleReportDto.StatsDto totalOf(List<CombatEntity> entities) {
        BattleReportDto.StatsDto totals = new BattleReportDto.StatsDto();
        for (CombatEntity entity : entities) {
            totals.setAttack(totals.getAttack() + entity.getAttack());
            totals.setPdf(totals.getPdf() + entity.getPdf());
            totals.setPdc(totals.getPdc() + entity.getPdc());
            totals.setDefense(totals.getDefense() + entity.getDefense());
            totals.setArmor(totals.getArmor() + entity.getArmor());
            totals.setEvasion(totals.getEvasion() + entity.getEvasion());
        }
        return totals;
    }
}
