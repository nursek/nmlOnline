package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
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

    private GameCharacter character(double attack, double pdf, double defense) {
        GameCharacter character = new GameCharacter("HerosTest", attack, pdf, 0, defense, 0, 0);
        character.setPlayerId(1L);
        return character;
    }

    @Nested
    @DisplayName("Phase classique (classicPhaseConfiguration)")
    class PhaseTests {

        @Test
        @DisplayName("Destruction si (armure + défense) <= points effectifs")
        void shouldDestroyUnitWhenPointsExceedDefensePlusArmor() {
            List<CombatEntity> defenders = new ArrayList<>(List.of(larbin()));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 50, "ATK");

            assertEquals(1, result.casualties().size());
            assertTrue(result.survivors().isEmpty());
            assertEquals(40.0, result.remainingPoints());
        }

        @Test
        @DisplayName("Destructions en chaîne tant que les points suffisent")
        void shouldChainKillsWhilePointsLast() {
            List<CombatEntity> defenders = new ArrayList<>(List.of(larbin(), larbin()));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 25, "ATK");

            assertEquals(2, result.casualties().size());
            assertEquals(5.0, result.remainingPoints());
        }

        @Test
        @DisplayName("Cible = dernière entité de la liste (la plus faible après tri)")
        void shouldTargetLastUnitFirst() {
            Unit strong = brute();
            Unit weak = larbin();
            List<CombatEntity> defenders = new ArrayList<>(List.of(strong, weak));

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
            List<CombatEntity> defenders = new ArrayList<>(List.of(strong, weak));

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
            List<CombatEntity> defenders = new ArrayList<>(List.of(unit));

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
            List<CombatEntity> defenders = new ArrayList<>(List.of(unit));

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
            List<CombatEntity> defenders = new ArrayList<>(List.of(unit));

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
            List<CombatEntity> defenders = new ArrayList<>(List.of(unit));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 100, "PDF");

            assertEquals(1, result.casualties().size());
            // coût = (def + armor) / (1 - 0.25) = 50 / 0.75 ≈ 66.67
            assertEquals(33.333, result.remainingPoints(), 0.001);
        }

        @Test
        @DisplayName("Pas de résistance MASTODONTE vs ATK")
        void shouldNotApplyMastodonteResistanceAgainstAtk() {
            Unit unit = new Unit(5, UnitClass.MASTODONTE);
            List<CombatEntity> defenders = new ArrayList<>(List.of(unit));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 100, "ATK");

            assertEquals(1, result.casualties().size());
            assertEquals(50.0, result.remainingPoints());
        }

        @Test
        @DisplayName("QG destructible en bataille : il tombe comme les autres bâtiments")
        void headquartersIsDestructibleInBattle() {
            Headquarters hq = new Headquarters(2L);
            List<CombatEntity> defenders = new ArrayList<>(List.of(hq));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 500, "ATK");

            assertEquals(1, result.casualties().size(), "Le QG n'est plus indestructible");
            assertTrue(result.survivors().isEmpty());
            assertEquals(300.0, result.remainingPoints());
        }

        @Test
        @DisplayName("Ordre de ciblage bâtiments : la Banque tombe avant le Cache")
        void bankFallsBeforeWeaponCache() {
            Headquarters hq = new Headquarters(2L);
            WeaponCache cache = new WeaponCache(2L);
            Bank bank = new Bank(2L);
            // Ordre des participants côté service : [QG, Cache, Banque] → getLast() frappe la Banque d'abord.
            List<CombatEntity> defenders = new ArrayList<>(List.of(hq, cache, bank));

            PhaseResult result = battle.classicPhaseConfiguration(defenders, 150, "ATK");

            assertEquals(2, result.casualties().size());
            assertSame(bank, result.casualties().get(0));
            assertSame(cache, result.casualties().get(1));
            assertTrue(result.survivors().contains(hq));
            assertEquals(200.0, hq.getDefense(), "Le QG intact n'est pas touché");
            assertEquals(0.0, result.remainingPoints());
        }

        @Test
        @DisplayName("Ordre de ciblage : le personnage ne tombe qu'en tout dernier (après le QG)")
        void characterIsTargetedLast() {
            GameCharacter hero = character(30, 0, 30);
            Headquarters hq = new Headquarters(2L);
            WeaponCache cache = new WeaponCache(2L);
            Bank bank = new Bank(2L);
            Unit larbin = larbin();
            // Ordre des participants côté service : [personnage, QG, Cache, Banque, unités].
            List<CombatEntity> defenders = new ArrayList<>(List.of(hero, hq, cache, bank, larbin));

            // 420 = LARBIN (10) + Banque (50) + Cache (100) + QG (200) + personnage (30)
            PhaseResult result = battle.classicPhaseConfiguration(defenders, 420, "ATK");

            assertEquals(5, result.casualties().size());
            assertSame(larbin, result.casualties().get(0));
            assertSame(bank, result.casualties().get(1));
            assertSame(cache, result.casualties().get(2));
            assertSame(hq, result.casualties().get(3));
            assertSame(hero, result.casualties().get(4), "Le personnage tombe après le QG, en tout dernier");
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
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(bruteUnit));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(larbin(), larbin()));

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
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(larbin()));
            Unit voyou = new Unit(2, UnitClass.TIREUR);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(voyou));

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
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(attackerUnit));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(defenderUnit));

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
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(shooter));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(larbin(), larbin()));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(defenderUnits.isEmpty());
            assertEquals(100.0, shooter.getDefense());
            assertFalse(shooter.isInjured());
        }

        @Test
        @DisplayName("Vainqueur assigné : défenseur anéanti, attaquant survivant")
        void shouldAssignWinnerWhenDefenderFightersAreWiped() {
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(brute()));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(larbin()));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertEquals(attacker, battle.getWinner());
        }

        @Test
        @DisplayName("Attaquant anéanti : le défenseur garde le secteur même sans survivant")
        void shouldAssignDefenderWhenAttackerFightersAreWiped() {
            // 10 atk vs 10 def mutuel : les deux LARBINs se détruisent → deux camps anéantis.
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(larbin()));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(larbin()));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(attackerUnits.isEmpty());
            assertTrue(defenderUnits.isEmpty());
            assertEquals(defender, battle.getWinner(), "L'attaquant repoussé ⇒ le secteur tient");
        }

        @Test
        @DisplayName("Deux camps survivants : aucun vainqueur")
        void shouldLeaveWinnerNullWhenBothSidesSurvive() {
            // 100 atk vs (armure 120 + def 100) mutuel : chacun perd 100 d'armure, personne ne meurt.
            Unit attackerUnit = brute();
            attackerUnit.addEquipment(defensive(120, 0));
            Unit defenderUnit = brute();
            defenderUnit.addEquipment(defensive(120, 0));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(attackerUnit));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(defenderUnit));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertEquals(1, attackerUnits.size());
            assertEquals(1, defenderUnits.size());
            assertNull(battle.getWinner());
        }

        @Test
        @DisplayName("Défense QG seul : le QG tombe en bataille, l'attaquant l'emporte")
        void buildingsOnlyDefenseHeadquartersFalls() {
            Headquarters hq = new Headquarters(2L);
            hq.setPlayerId(2L);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(brute(), brute(), brute()));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(hq));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            // ATK (pool unités) : 3×100 ≥ 200 def → le QG est détruit, sortie avant la phase QG.
            assertTrue(defenderUnits.isEmpty(), "Le QG destructible tombe sous 300 atk");
            assertEquals(attacker, battle.getWinner(), "Le QG n'est pas un combattant : l'attaquant l'emporte");
        }

        @Test
        @DisplayName("Phase bâtiments secondaires (entre PDF et PDC) : le PDC adverse n'intervient jamais")
        void secondaryBuildingsStrikeBeforePdcPhase() {
            WeaponCache cache = new WeaponCache(1L);
            cache.setPlayerId(1L);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(cache));
            Unit shooter = new Unit(5, UnitClass.TIREUR);
            Equipment gun = new Equipment("Lance-pierre", 100, 0, 100, 0, 0,
                    Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
            shooter.addEquipment(gun); // pdc = 50 × 100% = 50
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(shooter));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(defenderUnits.isEmpty(), "Le MALFRAT (50 def) tombe sous les 100 atk du Cache");
            // Si le PDC passait avant la riposte, le Cache serait entamé (def 50) : il est intact.
            assertEquals(100.0, cache.getDefense(), "Le PDC du MALFRAT n'a jamais frappé : sortie avant la phase PDC");
            assertEquals(defender, battle.getWinner(), "Un bâtiment n'est pas un combattant : secteur repoussé");
        }

        @Test
        @DisplayName("Phase QG après ATK : le QG frappe avec son attack après les unités")
        void headquartersStrikesAfterAtkPhase() {
            Headquarters hq = new Headquarters(1L);
            hq.setPlayerId(1L);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(hq));
            Unit defenderUnit = brute();
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(defenderUnit));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            // ATK : le BRUTE (100 atk, pool unités) entame le QG (def 200 → 100) ; le pool attaquant = 0.
            assertEquals(100.0, hq.getDefense(), "Le BRUTE a frappé en phase ATK");
            assertTrue(defenderUnits.isEmpty(), "Le BRUTE (100 def) tombe sous les 100 atk du QG en phase dédiée");
            assertEquals(100.0, hq.getAttack(), "Le reassign ATK (unités seules) n'a pas zéro l'attaque du QG");
            assertEquals(defender, battle.getWinner(), "Le QG n'est pas un combattant : secteur repoussé");
        }

        @Test
        @DisplayName("Phase personnage en dernier : attack seul, jamais dans le pool ATK, jamais blessé")
        void characterStrikesLastWithAttackOnly() {
            GameCharacter hero = character(100, 0, 200);
            Unit defenderUnit = brute();
            defenderUnit.addEquipment(defensive(120, 0)); // armure 120, def 100
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(hero));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(defenderUnit));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            // ATK : pool unités attaquant = 0 (no-op) ; le BRUTE (100 atk) entame le personnage (def 200 → 100).
            assertEquals(100.0, hero.getDefense(), "Le BRUTE a frappé le personnage en phase ATK");
            // Phase personnage : 100 atk vs armure 120 → armure 20, défense intacte.
            assertEquals(20.0, defenderUnit.getArmor());
            assertEquals(100.0, defenderUnit.getDefense(),
                    "Le personnage a frappé UNE fois (phase dédiée) : s'il comptait aussi dans le pool ATK, def serait 20");
            assertEquals(100.0, hero.getAttack(), "Le reassign ATK (unités seules) n'a pas zéro l'attaque du personnage");
            assertFalse(hero.isInjured(), "Un personnage n'est jamais blessé");
            assertNull(battle.getWinner(), "Deux combattants survivent : aucun vainqueur");
        }

        @Test
        @DisplayName("PDF/PDC des personnages : comptés dans les phases partagées")
        void characterPdfPdcCountInSharedPhases() {
            GameCharacter hero = character(100, 50, 100);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(hero));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(larbin(), larbin()));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            // Pool PDF attaquant = 50 (pdf du personnage) : les 2 LARBINs tombent avant toute autre phase.
            assertTrue(defenderUnits.isEmpty(), "Le pdf du personnage a frappé en phase PDF");
            assertEquals(attacker, battle.getWinner());
        }
    }
}
