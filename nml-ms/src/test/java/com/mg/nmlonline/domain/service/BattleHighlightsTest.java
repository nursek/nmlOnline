package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BattleHighlightDto;
import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.domain.model.battle.BattleLogEntry;
import com.mg.nmlonline.domain.model.unit.UnitType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BattleHighlights — points clés")
class BattleHighlightsTest {

    @Test
    @DisplayName("Détecte vainqueur, personnage perdu, QG, véhicule, promotion, captures et pertes lourdes")
    void buildsPublicAndAdminHighlights() {
        BattleReportDto.PlayerForceDto attacker = force(1L, "Lurio", "ATTAQUANT",
                List.of(unit("VOYOU n°1")), List.of(vehicle("Rhino n°1")));
        BattleReportDto.PlayerForceDto defender = force(2L, "Nursek", "DEFENSEUR",
                List.of(unit("Arene")), List.of());

        List<BattleHighlightDto> highlights = BattleHighlights.build(
                List.of(attacker, defender),
                List.of(new CombatService.CasualtyInfo(2L, "Arene", "CHARACTER", null, null, null),
                        new CombatService.CasualtyInfo(1L, "Rhino n°1", "VEHICLE", null, null, null)),
                List.of(new CombatService.ExperienceGain(1L, 10L, 1, UnitType.LARBIN, 1.0, 2.0,
                        UnitType.VOYOU, 3.0)),
                "Lurio", true, 1,
                List.of(new BattleLogEntry("PDF", BattleLogEntry.CRIT, "critique")));

        List<String> kinds = highlights.stream().map(BattleHighlightDto::getKind).toList();
        assertTrue(kinds.containsAll(List.of("WINNER", "CHARACTER_LOST", "HEADQUARTERS_CAPTURED",
                "VEHICLE_DESTROYED", "CRIT", "PROMOTION", "BUILDING_CAPTURED", "HEAVY_LOSSES")));
        assertEquals(BattleHighlights.ADMIN, highlights.stream()
                .filter(highlight -> "CRIT".equals(highlight.getKind()))
                .findFirst().orElseThrow().getVisibility());
        assertTrue(highlights.stream()
                .filter(highlight -> !"CRIT".equals(highlight.getKind()))
                .allMatch(highlight -> BattleHighlights.PUBLIC.equals(highlight.getVisibility())));
        assertTrue(highlights.stream().anyMatch(highlight ->
                "Personnage perdu : Nursek".equals(highlight.getMessage())));
        assertTrue(highlights.stream().anyMatch(highlight ->
                "Véhicule détruit : Rhino n°1 (Lurio)".equals(highlight.getMessage())));
        assertFalse(highlights.stream().anyMatch(highlight ->
                "Personnage perdu : Lurio".equals(highlight.getMessage())));
    }

    @Test
    @DisplayName("Aucun point clé de mécanique n'est public")
    void criticalsStayAdmin() {
        List<BattleHighlightDto> highlights = BattleHighlights.build(
                List.of(force(1L, "Lurio", "ATTAQUANT", List.of(), List.of())),
                List.of(), List.of(), "Lurio", false, 0,
                List.of(new BattleLogEntry("PDF", BattleLogEntry.CRIT, "critique")));

        assertTrue(highlights.stream()
                .filter(highlight -> "CRIT".equals(highlight.getKind()))
                .allMatch(highlight -> BattleHighlights.ADMIN.equals(highlight.getVisibility())));
    }

    private static BattleReportDto.PlayerForceDto force(Long playerId, String name, String side,
                                                        List<BattleReportDto.UnitForceDto> characters,
                                                        List<BattleReportDto.VehicleForceDto> vehicles) {
        BattleReportDto.PlayerForceDto force = new BattleReportDto.PlayerForceDto();
        force.setPlayerId(playerId);
        force.setPlayerName(name);
        force.setSide(side);
        force.setCharacters(characters);
        force.setVehicles(vehicles);
        force.setInfantry(List.of());
        force.setBuildings(List.of());
        return force;
    }

    private static BattleReportDto.UnitForceDto unit(String displayName) {
        BattleReportDto.UnitForceDto unit = new BattleReportDto.UnitForceDto();
        unit.setDisplayName(displayName);
        return unit;
    }

    private static BattleReportDto.VehicleForceDto vehicle(String displayName) {
        BattleReportDto.VehicleForceDto vehicle = new BattleReportDto.VehicleForceDto();
        vehicle.setDisplayName(displayName);
        return vehicle;
    }
}
