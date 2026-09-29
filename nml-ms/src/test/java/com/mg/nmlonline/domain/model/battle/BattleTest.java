package com.mg.nmlonline.domain.model.battle;

import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.VehicleBonusTarget;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Tests déterministes : évasion 0 ou 100, aucune cible aérienne sauf mention, Random seedé injecté par sécurité. */
@DisplayName("Battle — Moteur de combat individuel")
class BattleTest {

    private Battle battle;
    private Player attacker;
    private Player defender;

    @BeforeEach
    void setUp() {
        battle = new Battle();
        battle.setRandom(new Random(42));
        attacker = new Player("Attaquant");
        attacker.setId(1L);
        defender = new Player("Defenseur");
        defender.setId(2L);
    }

    private Unit larbin() {
        return new Unit(0, UnitClass.TIREUR);
    }

    private Unit brute() {
        return new Unit(8, UnitClass.TIREUR);
    }

    private Unit destructor(double experience) {
        return new Unit(experience, UnitClass.PILOTE_DESTRUCTEUR);
    }

    private Equipment defensive(double armBonus, double evasionBonus) {
        return new Equipment("Protection", 100, 0, 0, armBonus, evasionBonus,
                Set.of(UnitClass.TIREUR, UnitClass.PILOTE_DESTRUCTEUR), EquipmentCategory.DEFENSIVE);
    }

    private Equipment firearm(double pdfBonus) {
        return new Equipment("Fusil", 100, pdfBonus, 0, 0, 0,
                Set.of(UnitClass.TIREUR, UnitClass.PILOTE_DESTRUCTEUR), EquipmentCategory.FIREARM);
    }

    private Equipment melee(double pdcBonus) {
        return new Equipment("Lame", 100, 0, pdcBonus, 0, 0,
                Set.of(UnitClass.TIREUR, UnitClass.PILOTE_DESTRUCTEUR), EquipmentCategory.MELEE);
    }

    private Equipment antiGround(double pdfBonus) {
        Equipment equipment = new Equipment("Gauss Cannon", 100, pdfBonus, 0, 0, 0,
                Set.of(UnitClass.PILOTE_DESTRUCTEUR), EquipmentCategory.FIREARM);
        equipment.setVehicleBonus(100);
        equipment.setVehicleBonusTarget(VehicleBonusTarget.GROUND);
        return equipment;
    }

    private GameCharacter character(double attack, double pdf, double defense) {
        GameCharacter character = new GameCharacter("HerosTest", attack, pdf, 0, defense, 0, 0);
        character.setPlayerId(1L);
        return character;
    }

    private boolean logContains(String fragment) {
        return battle.getLog().stream().anyMatch(entry -> entry.message().contains(fragment));
    }

    @Nested
    @DisplayName("Frappes individuelles")
    class StrikeTests {

        @Test
        @DisplayName("Une unité frappe avec ses propres points et enchaîne le surplus sur la cible suivante")
        void unitStrikesWithOwnPointsAndChainsSurplus() {
            Unit brute = brute();
            brute.setNumber(1);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(brute));
            Unit weak1 = larbin();
            weak1.setNumber(1);
            Unit weak2 = larbin();
            weak2.setNumber(2);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(weak1, weak2));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(defenderUnits.isEmpty(), "100 ATK détruit deux LARBINs (10 def chacun)");
            assertTrue(logContains("surplus enchaîné"));
            assertEquals(attacker, battle.getWinner());
        }

        @Test
        @DisplayName("Un seul tireur ne peut pas entamer une cible trop défendue : la frappe est consommée")
        void singleStrikeCannotPoolWithAllies() {
            Unit larbin1 = larbin();
            larbin1.setNumber(1);
            Unit larbin2 = larbin();
            larbin2.setNumber(2);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(larbin1, larbin2));
            Unit target = brute();
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(target));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            // Les deux LARBINs frappent avant riposte : 20 dégâts, puis le BRUTE survit blessé (défense ÷ 2).
            assertTrue(attackerUnits.isEmpty(), "Le BRUTE riposte et détruit les deux LARBINs");
            assertTrue(target.isInjured());
            assertEquals(50.0, target.getDefense());
        }

        @Test
        @DisplayName("Esquive 100 % : l'attaque est perdue, le journal donne le jet et le coût")
        void dodgeCancelsStrikeAndLogsRoll() {
            Unit striker = brute();
            striker.setNumber(1);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(striker));
            Unit dodger = larbin();
            dodger.addEquipment(defensive(0, 100));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(dodger));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertEquals(10.0, dodger.getDefense(), "Aucun dégât : l'esquive annule la frappe");
            assertTrue(logContains("esquive (jet"));
            assertTrue(logContains("attaque perdue (100 ATK)"));
            assertTrue(battle.getParticipation().get(striker).fired());
            assertTrue(battle.getParticipation().get(dodger).dodged());
            assertFalse(battle.getParticipation().get(dodger).damaged());
        }

        @Test
        @DisplayName("La cible la plus faible encaisse d'abord, le surplus entame la suivante")
        void surplusChainsOnWeakestInCategory() {
            Unit striker = brute();
            striker.setNumber(1);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(striker));
            Unit weak = larbin();
            weak.setNumber(1);
            Unit strong = brute();
            strong.setNumber(1);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(strong, weak));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(logContains("surplus enchaîné"));
            String chain = battle.getLog().stream()
                    .map(BattleLogEntry::message)
                    .filter(message -> message.contains("surplus enchaîné") && message.contains("LARBIN n°1"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(chain.indexOf("détruit Defenseur · LARBIN n°1")
                            < chain.indexOf("touche Defenseur · BRUTE n°1"),
                    "Le LARBIN tombe avant que le BRUTE soit entamé : " + chain);
            assertTrue(strong.isInjured(), "Le BRUTE entamé (90 dégâts) survit blessé");
            assertEquals(50.0, strong.getDefense());
        }

        @Test
        @DisplayName("Armure percée : les dégâts comptés valent les points effectifs, pas armure + effectif")
        void piercedArmorCountsEffectiveDamageOnly() {
            Unit striker = new Unit(5, UnitClass.TIREUR);
            striker.addEquipment(defensive(100, 0));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(striker));
            Unit target = new Unit(5, UnitClass.TIREUR);
            target.addEquipment(defensive(80, 0));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(target));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            Battle.Participation dealt = battle.getParticipation().get(striker);
            Battle.Participation taken = battle.getParticipation().get(target);
            assertEquals(50.0, dealt.damageDealt(), "50 ATK : 40 d'armure puis 10 de défense");
            assertEquals(50.0, taken.damageTaken(), "L'armure ne doit pas être comptée deux fois");
        }
    }

    @Nested
    @DisplayName("Ciblage et véhicules")
    class TargetingTests {

        @Test
        @DisplayName("Pilote destructeur : priorité aux véhicules même avec des unités exposées")
        void destroyerPilotTargetsVehiclesFirst() {
            Unit shooter = destructor(5);
            shooter.addEquipment(firearm(400));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(shooter));
            Unit decoy = larbin();
            decoy.setNumber(1);
            Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, 2L);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(decoy, vehicle));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(logContains("détruit Defenseur · VTT léger"), "Le premier segment cible le véhicule");
            assertTrue(logContains("surplus enchaîné"), "Le surplus bascule ensuite sur l'infanterie");
            assertTrue(defenderUnits.isEmpty());
        }

        @Test
        @DisplayName("Sans pilote destructeur, une seule catégorie disponible : véhicules seuls ou unités seules")
        void categoryFallsBackWhenOnlyOneAvailable() {
            Unit shooter = larbin();
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(shooter));
            Vehicle vehicle = new Vehicle(VehicleType.VTT_BLINDE, 2L);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(vehicle));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertEquals(140.0, vehicle.getDefense(), "10 ATK du LARBIN sur la défense du VTT");
            assertEquals(10.0, shooter.getDefense(), "Véhicule sans pilote : il ne riposte pas");
            assertNull(battle.getWinner());
        }

        @Test
        @DisplayName("Véhicule sans pilote : cible passive, il ne tire pas en PdF")
        void unPilotedVehicleDoesNotFire() {
            Unit attackerUnit = larbin();
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(attackerUnit));

            Vehicle vehicle = new Vehicle(VehicleType.HELICOPTERE, 2L);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(vehicle));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertEquals(125.0, vehicle.getDefense());
            assertEquals(10.0, attackerUnit.getDefense(), "Aucun tir de l'hélicoptère sans pilote");
        }

        @Test
        @DisplayName("Résistance du Tank : les dégâts PdF sont divisés par deux")
        void tankHalvesPdfDamage() {
            Unit shooter = larbin();
            shooter.addEquipment(firearm(1000));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(shooter));
            Vehicle tank = new Vehicle(VehicleType.TANK, 2L);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(tank));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(logContains("résistance 50 % (100 → 50)"));
            assertTrue(logContains("défense 250 → 200"));
        }

        @Test
        @DisplayName("Arme anti-véhicule : dégâts doublés contre la catégorie visée, journalisés")
        void antiVehicleWeaponDoublesDamage() {
            Unit shooter = destructor(5);
            shooter.addEquipment(antiGround(80));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(shooter));
            Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, 2L);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(vehicle));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(logContains("bonus anti-véhicule +100 % (40 → 80)"));
            assertTrue(logContains("détruit Defenseur · VTT léger"));
            assertTrue(defenderUnits.isEmpty());
        }

        @Test
        @DisplayName("Bonus anti-véhicule : le surplus enchaîné reste en points de base, le bonus n'est pas doublé")
        void vehicleBonusIsNotReappliedToChainedSurplus() {
            Unit shooter = destructor(5);
            shooter.addEquipment(antiGround(80));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(shooter));
            Vehicle first = new Vehicle(VehicleType.VTT_LEGER, 2L);
            Vehicle second = new Vehicle(VehicleType.VTT_LEGER, 2L);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(first, second));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(logContains("détruit Defenseur · VTT léger (PDF), bonus anti-véhicule +100 % (40 → 80)"));
            assertTrue(logContains("bonus anti-véhicule +100 % (15 → 30)"),
                    "Le surplus (40 − 25) reste en points de base : 15 ×2, pas 30 ×2");
        }

        @Test
        @DisplayName("Véhicule aérien : ciblable uniquement en PdF, jamais en PdC ni ATK")
        void aerialTargetsRequirePdf() {
            Unit attackerUnit = new Unit(5, UnitClass.TIREUR);
            attackerUnit.addEquipment(melee(200));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(attackerUnit));
            Vehicle helicopter = new Vehicle(VehicleType.HELICOPTERE, 2L);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(helicopter));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertEquals(125.0, helicopter.getDefense(), "PdC et ATK ne peuvent pas toucher l'aérien");
            assertEquals(2, battle.getLog().stream()
                    .filter(entry -> entry.message().contains("n'a aucune cible"))
                    .count());
        }
    }

    @Nested
    @DisplayName("Équipage")
    class CrewTests {

        @Test
        @DisplayName("Équipage d'un véhicule armé : protégé et il tire sa propre PdF")
        void crewIsProtectedAndFiresFromArmedVehicle() {
            Unit brutePilot = destructor(8);
            brutePilot.addEquipment(defensive(200, 0));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(brutePilot));

            Vehicle vehicle = new Vehicle(VehicleType.VTT_BLINDE, 2L);
            Unit pilot = destructor(0);
            pilot.setNumber(1);
            pilot.addEquipment(firearm(200));
            vehicle.assignPilot(pilot);
            Unit passenger = larbin();
            passenger.setNumber(2);
            vehicle.embark(passenger);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(vehicle, pilot, passenger));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(logContains("(à bord de VTT blindé)"), "Le pilote tire depuis le véhicule");
            assertEquals(80.0, brutePilot.getArmor(), "100 Pdf du VTT + 20 Pdf du pilote");
            assertEquals(50.0, vehicle.getDefense(), "Seul le véhicule encaisse la phase ATK");
            assertEquals(10.0, pilot.getDefense(), "Pilote intouchable tant que le véhicule vit");
            assertEquals(10.0, passenger.getDefense());
            assertTrue(battle.getParticipation().get(pilot).fired());
        }

        @Test
        @DisplayName("Véhicule détruit : l'équipage débarque et redevient ciblable")
        void destroyedVehicleDisembarksCrew() {
            Unit shooter = destructor(5);
            shooter.addEquipment(antiGround(80));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(shooter));

            Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, 2L);
            Unit pilot = destructor(8);
            vehicle.assignPilot(pilot);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(vehicle, pilot));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertNull(vehicle.getPilot(), "Le pilote est détaché de l'épave");
            assertTrue(vehicle.getAllOccupants().isEmpty());
            assertTrue(logContains("équipage débarqué : BRUTE"));
            assertTrue(pilot.isInjured(), "Le surplus de la frappe (30) entame l'équipage : 70 → blessé");
            assertEquals(50.0, pilot.getDefense());
            assertSame(pilot, defenderUnits.getFirst());
        }

        @Test
        @DisplayName("Véhicule détruit pendant la phase : l'équipage riposte quand même (simultané)")
        void destroyedVehicleCrewStillStrikesInSamePhase() {
            Unit striker = destructor(8);
            striker.addEquipment(defensive(1000, 0));
            striker.addEquipment(antiGround(1000));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(striker));

            Vehicle vehicle = new Vehicle(VehicleType.VTT_BLINDE, 2L);
            Unit pilot = destructor(8);
            pilot.addEquipment(defensive(2000, 0));
            pilot.addEquipment(firearm(200));
            vehicle.assignPilot(pilot);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(vehicle, pilot));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertNull(vehicle.getPilot(), "Le véhicule a bien été détruit et son équipage débarqué");
            assertTrue(logContains("équipage débarqué : BRUTE"));
            assertTrue(battle.getParticipation().get(pilot).fired(),
                    "Les frappeurs sont figés au début de la phase : le pilote tire malgré la destruction");
        }
    }

    @Nested
    @DisplayName("Chance de toucher l'aérien")
    class AerialHitChanceTests {

        @Test
        @DisplayName("Barème par type d'attaquant et arme anti-aérienne")
        void aerialHitChancesByAttacker() {
            assertEquals(0.10, larbin().getAerialHitChance());
            assertEquals(0.25, new Unit(2, UnitClass.TIREUR).getAerialHitChance());
            assertEquals(0.40, new Unit(5, UnitClass.TIREUR).getAerialHitChance());
            assertEquals(0.65, brute().getAerialHitChance());
            assertEquals(0.65, character(30, 0, 30).getAerialHitChance());
            assertEquals(0.50, new Vehicle(VehicleType.TANK, 1L).getAerialHitChance());
            assertEquals(0.75, new Vehicle(VehicleType.HELICOPTERE, 1L).getAerialHitChance());

            Unit antiAir = brute();
            Equipment heavy = new Equipment("Heavy Gauss Cannon", 100, 0, 0, 0, 0,
                    Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
            heavy.setVehicleBonus(100);
            heavy.setVehicleBonusTarget(VehicleBonusTarget.AERIAL);
            antiAir.addEquipment(heavy);
            assertEquals(0.75, antiAir.getAerialHitChance());

            Unit withDefensiveAA = brute();
            Equipment shield = new Equipment("Bouclier AA", 100, 0, 0, 0, 0,
                    Set.of(UnitClass.TIREUR), EquipmentCategory.DEFENSIVE);
            shield.setVehicleBonus(100);
            shield.setVehicleBonusTarget(VehicleBonusTarget.AERIAL);
            withDefensiveAA.addEquipment(shield);
            assertEquals(0.65, withDefensiveAA.getAerialHitChance(),
                    "Un équipement défensif n'est pas une arme anti-aérienne");
        }
    }

    @Nested
    @DisplayName("Ordre des phases")
    class PhaseOrderTests {

        @Test
        @DisplayName("Riposte des bâtiments secondaires avant la phase PdC")
        void secondaryBuildingsStrikeBeforePdcPhase() {
            WeaponCache cache = new WeaponCache(1L);
            cache.setPlayerId(1L);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(cache));
            Unit shooter = new Unit(5, UnitClass.TIREUR);
            shooter.addEquipment(melee(200));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(shooter));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(defenderUnits.isEmpty(), "100 ATK du Cache détruisent le MALFRAT (50 def)");
            assertEquals(100.0, cache.getDefense(), "Le PdC du MALFRAT n'a jamais frappé");
            assertEquals(defender, battle.getWinner(), "Un bâtiment n'est pas un combattant : secteur repoussé");
        }

        @Test
        @DisplayName("Le QG frappe en phase QG, après la phase ATK des unités")
        void headquartersStrikesAfterAtkPhase() {
            Headquarters hq = new Headquarters(1L);
            hq.setPlayerId(1L);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(hq));
            Unit defenderUnit = brute();
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(defenderUnit));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertEquals(100.0, hq.getDefense(), "Le BRUTE (100 atk) a frappé le QG en phase ATK");
            assertTrue(defenderUnits.isEmpty(), "Le BRUTE (100 def) tombe sous les 100 atk du QG");
            assertEquals(defender, battle.getWinner());
        }

        @Test
        @DisplayName("Le personnage frappe en dernier, avec son attack seul")
        void characterStrikesLastWithAttackOnly() {
            GameCharacter hero = character(100, 0, 200);
            Unit defenderUnit = brute();
            defenderUnit.addEquipment(defensive(120, 0));
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(hero));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(defenderUnit));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertEquals(100.0, hero.getDefense(), "Le BRUTE a frappé le personnage en phase ATK");
            assertEquals(20.0, defenderUnit.getArmor(), "Frappe unique du personnage : 100 vs armure 120");
            assertEquals(100.0, defenderUnit.getDefense());
            assertNull(battle.getWinner());
        }

        @Test
        @DisplayName("La PdF des personnages frappe dès la phase PdF")
        void characterPdfCountsInPdfPhase() {
            GameCharacter hero = character(100, 50, 100);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(hero));
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(larbin(), larbin()));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            assertTrue(defenderUnits.isEmpty(), "50 Pdf détruisent deux LARBINs (10 def) et enchaînent");
            assertEquals(attacker, battle.getWinner());
        }

        @Test
        @DisplayName("Journal : état initial, phases, destructions, esquive et vainqueur")
        void battleLogRecordsPhasesAndOutcomes() {
            Unit brute = brute();
            brute.setNumber(1);
            List<CombatEntity> attackerUnits = new ArrayList<>(List.of(brute));
            Unit weak = larbin();
            weak.setNumber(1);
            List<CombatEntity> defenderUnits = new ArrayList<>(List.of(weak));

            battle.classicCombatConfiguration(attacker, defender, attackerUnits, defenderUnits);

            List<BattleLogEntry> log = battle.getLog();
            assertFalse(log.isEmpty());
            assertEquals("État initial", log.getFirst().phase());
            assertTrue(log.stream().anyMatch(e -> "État initial".equals(e.phase()) && e.message().contains("100 Atk")));
            assertTrue(log.stream().anyMatch(e -> e.message().startsWith("=== Phase")));
            assertTrue(log.stream().anyMatch(e -> BattleLogEntry.DESTROYED.equals(e.outcome())
                    && e.message().contains("Attaquant · BRUTE n°1 détruit Defenseur · LARBIN n°1")));
            assertTrue(log.stream().anyMatch(e -> BattleLogEntry.WINNER.equals(e.outcome())));
        }
    }
}
