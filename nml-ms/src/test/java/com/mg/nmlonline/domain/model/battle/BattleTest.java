package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Tests déterministes : évasion 0 ou 100, Random seedé injecté par sécurité. */
@DisplayName("Battle — Moteur de combat")
class BattleTest {

    private Battle battle;

    @BeforeEach
    void setUp() {
        battle = new Battle();
        battle.setRandom(new Random(42));
    }

    private Unit larbin() {
        return new Unit(0, UnitClass.TIREUR);
    }

    private Unit brute() {
        return new Unit(8, UnitClass.TIREUR);
    }

    private Equipment defensive(double armBonus, double evasionBonus) {
        return new Equipment("Protection", 100, 0, 0, armBonus, evasionBonus,
                Set.of(UnitClass.TIREUR), EquipmentCategory.DEFENSIVE);
    }

    @Nested
    @DisplayName("Phase classique (classicPhaseConfiguration)")
    class PhaseTests {

        @Test
        @DisplayName("Destruction si (armure + défense) <= points effectifs")
        void shouldDestroyUnitWhenPointsExceedDefensePlusArmor() {
            List<Unit> defenders = new ArrayList<>(List.of(larbin()));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 50, "ATK");

            assertEquals(1, result.casualties().size());
            assertTrue(result.survivors().isEmpty());
            assertEquals(40.0, result.remainingPoints());
        }

        @Test
        @DisplayName("Destructions en chaîne tant que les points suffisent")
        void shouldChainKillsWhilePointsLast() {
            List<Unit> defenders = new ArrayList<>(List.of(larbin(), larbin()));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 25, "ATK");

            assertEquals(2, result.casualties().size());
            assertEquals(5.0, result.remainingPoints());
        }

        @Test
        @DisplayName("Cible = dernière unité de la liste (la plus faible après tri)")
        void shouldTargetLastUnitFirst() {
            Unit strong = brute();
            Unit weak = larbin();
            List<Unit> defenders = new ArrayList<>(List.of(strong, weak));

            // 10 points = exactement le coût de destruction du LARBIN
            PhaseResult result = battle.classicPhaseConfiguration(defenders, 10, "ATK");

            assertEquals(1, result.casualties().size());
            assertSame(weak, result.casualties().getFirst());
            assertSame(strong, result.survivors().getFirst());
            assertEquals(100.0, strong.getDefense());
            assertEquals(0.0, result.remainingPoints());
        }

        @Test
        @DisplayName("Points excédentaires : la cible suivante absorbe le reliquat")
        void shouldChainRemainingPointsToNextTarget() {
            Unit strong = brute();
            Unit weak = larbin();
            List<Unit> defenders = new ArrayList<>(List.of(strong, weak));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 50, "ATK");

            assertEquals(1, result.casualties().size());
            assertSame(weak, result.casualties().getFirst());
            assertEquals(60.0, strong.getDefense());
            assertEquals(0.0, result.remainingPoints());
        }

        @Test
        @DisplayName("L'armure absorbe avant la défense")
        void shouldDamageArmorBeforeDefense() {
            Unit unit = new Unit(5, UnitClass.TIREUR);
            unit.addEquipment(defensive(40, 0));
            List<Unit> defenders = new ArrayList<>(List.of(unit));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 15, "ATK");

            assertTrue(result.casualties().isEmpty());
            assertEquals(5.0, unit.getArmor());
            assertEquals(50.0, unit.getDefense());
            assertEquals(0.0, result.remainingPoints());
        }

        @Test
        @DisplayName("Armure percée : le reliquat tape la défense")
        void shouldPierceArmorThenDamageDefense() {
            Unit unit = new Unit(5, UnitClass.TIREUR);
            unit.addEquipment(defensive(40, 0));
            List<Unit> defenders = new ArrayList<>(List.of(unit));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 30, "ATK");

            assertTrue(result.casualties().isEmpty());
            assertEquals(0.0, unit.getArmor());
            assertEquals(40.0, unit.getDefense());
            assertEquals(0.0, result.remainingPoints());
        }

        @Test
        @DisplayName("Esquive : coût plein (défense + armure) consommé malgré l'esquive")
        void shouldConsumeFullDefenseCostOnEvasion() {
            Unit unit = larbin();
            unit.addEquipment(defensive(0, 100));
            List<Unit> defenders = new ArrayList<>(List.of(unit));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 20, "ATK");

            assertTrue(result.casualties().isEmpty());
            assertEquals(1, result.survivors().size());
            assertEquals(0.0, result.remainingPoints());
            assertEquals(10.0, unit.getDefense());
        }

        @Test
        @DisplayName("Résistance MASTODONTE : 25% vs PDF, coût de destruction majoré")
        void shouldApplyMastodonteResistanceAgainstPdf() {
            Unit unit = new Unit(5, UnitClass.MASTODONTE);
            List<Unit> defenders = new ArrayList<>(List.of(unit));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 100, "PDF");

            assertEquals(1, result.casualties().size());
            // coût = (def + armor) / (1 - 0.25) = 50 / 0.75 ≈ 66.67
            assertEquals(33.333, result.remainingPoints(), 0.001);
        }

        @Test
        @DisplayName("Pas de résistance MASTODONTE vs ATK")
        void shouldNotApplyMastodonteResistanceAgainstAtk() {
            Unit unit = new Unit(5, UnitClass.MASTODONTE);
            List<Unit> defenders = new ArrayList<>(List.of(unit));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 100, "ATK");

            assertEquals(1, result.casualties().size());
            assertEquals(50.0, result.remainingPoints());
        }
    }

    @Nested
    @DisplayName("Combat complet (classicCombatConfiguration)")
    class FullCombatTests {

        private Player attacker;
        private Player defender;

        @BeforeEach
        void setUpPlayers() {
            attacker = new Player("Attaquant");
            attacker.setId(1L);
            defender = new Player("Défenseur");
            defender.setId(2L);
        }

        @Test
        @DisplayName("BRUTE vs 2 LARBIN : défenseurs éliminés, attaquant blessé (stats ÷ 2)")
        void shouldWipeDefendersAndInjureAttacker() {
            Unit bruteUnit = brute();
            List<Unit> attackerUnits = new ArrayList<>(List.of(bruteUnit));
            List<Unit> defenderUnits = new ArrayList<>(List.of(larbin(), larbin()));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(defenderUnits.isEmpty());
            assertEquals(1, attackerUnits.size());
            // défense passée de 100 à 80 (20 atk adverses) → blessé → recalcul × 0.5
            assertTrue(bruteUnit.isInjured());
            assertEquals(50.0, bruteUnit.getAttack());
            assertEquals(50.0, bruteUnit.getDefense());
        }

        @Test
        @DisplayName("LARBIN vs VOYOU : attaquant éliminé, survivant blessé")
        void shouldInjureSurvivorWithReducedDefense() {
            List<Unit> attackerUnits = new ArrayList<>(List.of(larbin()));
            Unit voyou = new Unit(2, UnitClass.TIREUR);
            List<Unit> defenderUnits = new ArrayList<>(List.of(voyou));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(attackerUnits.isEmpty());
            assertEquals(1, defenderUnits.size());
            assertTrue(voyou.isInjured());
            assertEquals(10.0, voyou.getAttack());
            assertEquals(10.0, voyou.getDefense());
        }

        @Test
        @DisplayName("Défense intacte après le combat : pas de blessure")
        void shouldNotInjureUnitWithIntactDefense() {
            Unit attackerUnit = new Unit(5, UnitClass.TIREUR);
            attackerUnit.addEquipment(defensive(120, 0));
            Unit defenderUnit = new Unit(5, UnitClass.TIREUR);
            defenderUnit.addEquipment(defensive(120, 0));
            List<Unit> attackerUnits = new ArrayList<>(List.of(attackerUnit));
            List<Unit> defenderUnits = new ArrayList<>(List.of(defenderUnit));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertEquals(1, attackerUnits.size());
            assertEquals(1, defenderUnits.size());
            assertFalse(attackerUnit.isInjured());
            assertFalse(defenderUnit.isInjured());
            assertEquals(10.0, attackerUnit.getArmor());
            assertEquals(10.0, defenderUnit.getArmor());
            assertEquals(50.0, attackerUnit.getDefense());
        }

        @Test
        @DisplayName("Phase PDF létale : combat terminé sans phase ATK")
        void shouldEndCombatAfterLethalPdfPhase() {
            Unit shooter = brute();
            Equipment gun = new Equipment("Fusil", 100, 50, 0, 0, 0,
                    Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
            shooter.addEquipment(gun);
            List<Unit> attackerUnits = new ArrayList<>(List.of(shooter));
            List<Unit> defenderUnits = new ArrayList<>(List.of(larbin(), larbin()));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(defenderUnits.isEmpty());
            assertEquals(100.0, shooter.getDefense());
            assertFalse(shooter.isInjured());
        }

        @Test
        @DisplayName("Le vainqueur n'est jamais assigné")
        void shouldPinNullWinner() {
            // Battle.winner n'est jamais assigné.
            List<Unit> attackerUnits = new ArrayList<>(List.of(brute()));
            List<Unit> defenderUnits = new ArrayList<>(List.of(larbin()));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertNull(battle.getWinner());
        }
    }
}
