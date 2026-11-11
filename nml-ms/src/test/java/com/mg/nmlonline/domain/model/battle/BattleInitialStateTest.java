package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Battle — état initial")
class BattleInitialStateTest {

    @Test
    @DisplayName("Sépare les armées par joueur, imbrique l'équipage et n'expose pas d'identifiant interne")
    void separatesPlayersAndNestsCrew() {
        Battle battle = new Battle();
        battle.setRandomSource(() -> 100);
        battle.setParticipantNames(Map.of(1L, "Lurio", 2L, "Nursek", 3L, "Mortarion"));

        Player leader = player(1L, "Lurio");
        Player defender = player(2L, "Nursek");

        Unit leaderUnit = unit(1L, 1, 4.0);
        Unit allyUnit = unit(3L, 3, 6.0);
        Unit crew = unit(3L, 2, 2.0);
        Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, 1L);
        vehicle.embark(crew);
        GameCharacter character = new GameCharacter("Arene", 30, 0, 0, 30, 0, 0);
        character.setPlayerId(2L);

        List<CombatEntity> attackers = new ArrayList<>(List.of(leaderUnit, allyUnit, crew, vehicle));
        List<CombatEntity> defenders = new ArrayList<>(List.of(unit(2L, 4, 0.0), character));

        battle.classicCombatConfiguration(leader, defender, attackers, defenders);

        List<String> messages = battle.getLog().stream().map(BattleLogEntry::message).toList();
        assertTrue(messages.contains("Attaquant — Lurio (2 entité(s)) :"),
                "Les entités de Lurio (unité + véhicule) restent sous Lurio");
        assertTrue(messages.contains("Attaquant — Mortarion (1 entité(s)) :"),
                "L'allié a son propre bloc ; l'unité embarquée n'est pas comptée deux fois");
        assertTrue(messages.contains("Défenseur — Nursek (2 entité(s)) :"));
        assertTrue(messages.contains("  Personnages :"));
        assertTrue(messages.contains("  Véhicules :"));
        assertTrue(messages.contains("  Infanterie :"));
        assertTrue(messages.stream().anyMatch(message -> message.startsWith("      Équipage :")
                        && message.contains(crew.getDisplayName())),
                "L'équipage est rattaché à son véhicule");
        assertFalse(messages.stream().anyMatch(message -> message.contains("Unique id")),
                "Aucun identifiant technique ne doit apparaître dans le rapport");
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
