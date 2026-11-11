package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Battle — salves sniper de soutien")
class BattleRangedTest {

    private Player player(Long id, String name) {
        Player player = new Player(name);
        player.setId(id);
        return player;
    }

    private Unit unit(Long playerId) {
        Unit unit = new Unit(0.0, UnitClass.LEGER);
        unit.setPlayerId(playerId);
        unit.setAttack(0);
        unit.setPdf(0);
        unit.setPdc(0);
        unit.setDefense(1);
        return unit;
    }

    private Unit sniper(Long playerId, double pdf) {
        Unit sniper = new Unit(0.0, UnitClass.SNIPER);
        sniper.setPlayerId(playerId);
        sniper.setAttack(0);
        sniper.setPdf(pdf);
        sniper.setPdc(0);
        return sniper;
    }

    private Bank bank(Long playerId, double defense) {
        Bank bank = new Bank(playerId);
        bank.setAttack(0);
        bank.setPdf(0);
        bank.setPdc(0);
        bank.setArmor(0);
        bank.setDefense(defense);
        return bank;
    }

    private Battle battle() {
        Battle battle = new Battle();
        battle.setRandom(new Random(7));
        return battle;
    }

    private Unit sniperLarbin(Long playerId, double pdf) {
        Unit unit = new Unit(0.0, UnitClass.SNIPER);
        unit.setPlayerId(playerId);
        Equipment rifle = new Equipment("Fusil de test", 0, 0, 0, 0, 0,
                Set.of(UnitClass.SNIPER), EquipmentCategory.FIREARM);
        rifle.setAimMalusReduction(20);
        unit.addEquipment(rifle);
        unit.setPdf(pdf);
        return unit;
    }

    @Test
    @DisplayName("Soutien attaquant : PdF ajoutée en phase PdF, cible détruite, sniper hors combat")
    void attackerSupportStrikesDuringPdf() {
        Player attacker = player(1L, "Attaquant");
        Player defender = player(2L, "Defenseur");
        Unit attackerUnit = unit(1L);
        Unit sniper = sniper(1L, 500);
        List<CombatEntity> defenders = new ArrayList<>(List.of(bank(2L, 100)));
        Battle battle = battle();
        battle.setRangedAttackers(List.of(sniper));

        battle.classicCombatConfiguration(attacker, defender,
                new ArrayList<>(List.of(attackerUnit)), defenders);

        assertTrue(defenders.isEmpty());
        assertSame(attacker, battle.getWinner());
        assertTrue(battle.getLog().stream().anyMatch(entry -> entry.message().contains("Tir à distance")));
        assertFalse(battle.getParticipation().containsKey(sniper));
    }

    @Test
    @DisplayName("Soutien défenseur : PdF ajoutée en phase PdF, attaquant détruit")
    void defenderSupportStrikesDuringPdf() {
        Player attacker = player(1L, "Attaquant");
        Player defender = player(2L, "Defenseur");
        List<CombatEntity> attackers = new ArrayList<>(List.of(bank(1L, 100)));
        List<CombatEntity> defenders = new ArrayList<>(List.of(unit(2L)));
        Battle battle = battle();
        battle.setRangedDefenders(List.of(sniper(2L, 500)));

        battle.classicCombatConfiguration(attacker, defender, attackers, defenders);

        assertTrue(attackers.isEmpty());
        assertSame(defender, battle.getWinner());
        assertTrue(battle.getLog().stream().anyMatch(entry -> entry.message().contains("Tir à distance")));
    }

    @Test
    @DisplayName("Impasse : le soutien du camp frappe le camp cible")
    void standoffSupportStrikesTargetCamp() {
        Player first = player(1L, "Camp 1");
        Player second = player(2L, "Camp 2");
        Player third = player(3L, "Camp 3");
        List<CombatEntity> firstCamp = new ArrayList<>(List.of(unit(1L)));
        List<CombatEntity> secondCamp = new ArrayList<>(List.of(bank(2L, 100)));
        List<CombatEntity> thirdCamp = new ArrayList<>(List.of(unit(3L)));
        Battle battle = battle();
        battle.setRangedByCamp(Map.of(0, List.of(sniper(1L, 500))));

        battle.classicStandoffConfiguration(List.of(first, second, third),
                List.of(firstCamp, secondCamp, thirdCamp), new int[]{1, 2, 0}, new double[]{0, 0, 0});

        assertTrue(secondCamp.isEmpty());
        assertTrue(battle.getLog().stream().anyMatch(entry -> entry.message().contains("Tir à distance")));
    }

    @Test
    @DisplayName("Tir isolé : le sniper ciblé contre-tire contre les tireurs")
    void standaloneFireTriggersCounterFire() {
        Unit shooter = sniperLarbin(1L, 500);
        Unit targetSniper = sniperLarbin(2L, 500);
        List<CombatEntity> targets = new ArrayList<>(List.of(targetSniper));
        Battle battle = battle();

        battle.rangedOnlyConfiguration(List.of(shooter), targets, "Secteur 2");

        assertTrue(battle.getTargetedSnipers().contains(targetSniper));
        assertTrue(targets.isEmpty());
        assertTrue(battle.getRangedDestroyed().contains(targetSniper));

        battle.counterFireConfiguration(List.of(targetSniper), List.of(shooter), "Secteur 1");

        assertTrue(battle.getRangedDestroyed().contains(shooter));
        assertTrue(battle.getLog().stream()
                .anyMatch(entry -> entry.message().contains("Contre-tir à distance")));
    }

    @Test
    @DisplayName("Un survivant endommagé par un tir isolé est blessé")
    void rangedSurvivorIsInjured() {
        Unit shooter = sniperLarbin(1L, 5);
        Unit target = new Unit(0.0, UnitClass.LEGER);
        target.setPlayerId(2L);
        target.setDefense(10);
        target.setPdf(0);
        target.setPdc(0);
        Battle battle = battle();

        battle.rangedOnlyConfiguration(List.of(shooter), new ArrayList<>(List.of(target)), "Secteur 2");

        assertFalse(battle.getRangedDestroyed().contains(target));
        assertTrue(target.isInjured());
    }

    @Test
    @DisplayName("Un véhicule détruit débarque son équipage, puis la salve continue sur lui")
    void rangedSalvoContinuesOnDisembarkedCrew() {
        Unit shooter = sniperLarbin(1L, 500);
        Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, 2L);
        Unit pilot = new Unit(0.0, UnitClass.PILOTE_DESTRUCTEUR);
        pilot.setPlayerId(2L);
        Unit passenger = new Unit(0.0, UnitClass.TIREUR);
        passenger.setPlayerId(2L);
        vehicle.assignPilot(pilot);
        vehicle.embark(passenger);
        List<CombatEntity> targets = new ArrayList<>(List.of(vehicle, pilot, passenger));
        Battle battle = battle();

        battle.rangedOnlyConfiguration(List.of(shooter), targets, "Secteur 2");

        assertTrue(battle.getRangedDestroyed().contains(vehicle));
        assertTrue(battle.getRangedDestroyed().contains(pilot));
        assertTrue(battle.getRangedDestroyed().contains(passenger));
    }

    @Test
    @DisplayName("Impasse : un camp sniper tiers n'est pas touché par les attaques au sol")
    void rangedOnlyCampIsNotHitByGround() {
        Player first = player(1L, "Camp 1");
        Player second = player(2L, "Camp 2");
        Player third = player(3L, "Camp 3");
        Unit firstUnit = sniper(1L, 500);
        firstUnit.setArmor(1000);
        Unit secondUnit = sniper(2L, 0);
        secondUnit.setArmor(1000);
        Unit thirdSniper = sniperLarbin(3L, 500);
        List<CombatEntity> firstCamp = new ArrayList<>(List.of(firstUnit));
        List<CombatEntity> secondCamp = new ArrayList<>(List.of(secondUnit));
        List<CombatEntity> thirdCamp = new ArrayList<>(List.of(thirdSniper));
        Battle battle = battle();
        battle.setRangedOnlyCamps(Set.of(2));
        battle.setRangedByCamp(Map.of(2, List.of(thirdSniper)));

        battle.classicStandoffConfiguration(List.of(first, second, third),
                List.of(firstCamp, secondCamp, thirdCamp), new int[]{1, 2, 0}, new double[]{0, 0, 0});

        assertFalse(thirdCamp.isEmpty(), "Le camp sniper n'est pas une cible au sol");
        assertFalse(battle.getRangedDestroyed().contains(thirdSniper));
        assertNull(battle.getWinner(), "Un camp sniper ne peut pas gagner l'impasse");
    }

    @Test
    @DisplayName("Impasse : un camp sans sniper reporte son PDF sur le camp suivant en phase PDC")
    void standoffWithoutSniperCarriesPdfToPdc() {
        Player first = player(1L, "A");
        Player second = player(2L, "B");
        Player third = player(3L, "C");
        Unit firstUnit = unit(1L);
        firstUnit.setDefense(1000);
        Unit secondUnit = unit(2L);
        secondUnit.setPdf(500);
        secondUnit.setPdc(500);
        secondUnit.setDefense(1000);
        Unit thirdSniper = sniper(3L, 100);
        thirdSniper.setDefense(1000);
        List<CombatEntity> firstCamp = new ArrayList<>(List.of(firstUnit));
        List<CombatEntity> secondCamp = new ArrayList<>(List.of(secondUnit));
        List<CombatEntity> thirdCamp = new ArrayList<>(List.of(thirdSniper));
        Battle battle = battle();
        battle.setRangedOnlyCamps(Set.of(2));
        battle.setRangedByCamp(Map.of(2, List.of(thirdSniper)));

        battle.classicStandoffConfiguration(List.of(first, second, third),
                List.of(firstCamp, secondCamp, thirdCamp), new int[]{1, 2, 0}, new double[]{0, 0, 0});

        assertTrue(battle.getLog().stream()
                        .anyMatch(entry -> entry.phase().contains("PDC")
                                && entry.message().contains("touche A")),
                "B n'a pas de sniper : son PDF non utilisé retombe sur A en phase PDC");
    }

    @Test
    @DisplayName("Impasse : un sniper d'un autre camp peut détruire le camp sniper tiers")
    void sniperSupportHitsRangedOnlyCamp() {
        Player first = player(1L, "Camp 1");
        Player second = player(2L, "Camp 2");
        Player third = player(3L, "Camp 3");
        Unit firstUnit = sniper(1L, 0);
        firstUnit.setArmor(1000);
        Unit secondUnit = sniper(2L, 0);
        secondUnit.setArmor(1000);
        Unit secondSniper = sniperLarbin(2L, 500);
        Unit thirdSniper = sniperLarbin(3L, 500);
        List<CombatEntity> firstCamp = new ArrayList<>(List.of(firstUnit));
        List<CombatEntity> secondCamp = new ArrayList<>(List.of(secondUnit));
        List<CombatEntity> thirdCamp = new ArrayList<>(List.of(thirdSniper));
        Battle battle = battle();
        battle.setRangedOnlyCamps(Set.of(2));
        battle.setRangedByCamp(Map.of(1, List.of(secondSniper), 2, List.of(thirdSniper)));

        battle.classicStandoffConfiguration(List.of(first, second, third),
                List.of(firstCamp, secondCamp, thirdCamp), new int[]{1, 2, 0}, new double[]{0, 0, 0});

        assertTrue(thirdCamp.isEmpty(), "Le sniper du camp 2 détruit le sniper tiers");
        assertTrue(battle.getRangedDestroyed().contains(thirdSniper));
        assertNull(battle.getWinner());
    }
}
