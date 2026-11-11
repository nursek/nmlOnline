package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("BattleForcesBuilder — instantané des forces")
class BattleForcesBuilderTest {

    @Test
    @DisplayName("Sépare les alliés, imbrique l'équipage et classe les bâtiments à part")
    void splitsAlliesAndNestsCrew() {
        Player lurio = player(1L, "Lurio");
        Player mortarion = player(3L, "Mortarion");
        Unit lurioUnit = unit(1L, 1, 4.0);
        Unit allyUnit = unit(3L, 3, 6.0);
        Unit crew = unit(3L, 2, 2.0);
        Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, 1L);
        vehicle.embark(crew);

        List<BattleReportDto.PlayerForceDto> attackers = BattleForcesBuilder.build(
                List.of(lurio, mortarion),
                List.of(lurioUnit, allyUnit, crew, vehicle),
                "ATTAQUANT");

        assertEquals(2, attackers.size());
        BattleReportDto.PlayerForceDto lurioForce = attackers.getFirst();
        assertEquals("ATTAQUANT", lurioForce.getSide());
        assertEquals(1, lurioForce.getInfantry().size());
        assertEquals(1, lurioForce.getVehicles().size());
        assertEquals(1, lurioForce.getVehicles().getFirst().getCrew().size(),
                "L'équipage est rattaché au véhicule");
        BattleReportDto.PlayerForceDto allyForce = attackers.get(1);
        assertEquals(1, allyForce.getInfantry().size(), "L'unité embarquée n'est pas comptée deux fois");
        assertEquals(0, allyForce.getVehicles().size());

        GameCharacter character = new GameCharacter("Arene", 30, 0, 0, 30, 0, 0);
        character.setPlayerId(2L);
        Headquarters headquarters = new Headquarters(1L);
        headquarters.setPlayerId(2L);

        List<BattleReportDto.PlayerForceDto> defenders = BattleForcesBuilder.build(
                List.of(player(2L, "Nursek")),
                List.of(character, headquarters),
                "DEFENSEUR");

        BattleReportDto.PlayerForceDto defenderForce = defenders.getFirst();
        assertEquals(1, defenderForce.getCharacters().size());
        assertEquals(1, defenderForce.getBuildings().size());
        assertEquals(BuildingType.HEADQUARTERS.name(), defenderForce.getBuildings().getFirst().getCategory());
    }

    private static Player player(Long id, String name) {
        Player player = new Player(name);
        player.setId(id);
        return player;
    }

    private static Unit unit(Long playerId, int number, double experience) {
        Unit unit = new Unit(experience, UnitClass.TIREUR);
        unit.setPlayerId(playerId);
        unit.setNumber(number);
        return unit;
    }
}
