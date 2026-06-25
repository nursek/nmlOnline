package com.mg.nmlonline.domain.model.unit;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de régression sur les règles des unités :
 * seuils d'expérience, stats de base, blessure, formules d'équipement,
 * limites par catégorie, seconde classe, réductions de dégâts.
 */
@DisplayName("Unit")
class UnitTest {

    private Equipment firearm(double pdfBonus) {
        return new Equipment("Arme à feu", 100, pdfBonus, 0, 0, 0,
                Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
    }

    private Equipment defensive(double armBonus, double evasionBonus) {
        return new Equipment("Protection", 100, 0, 0, armBonus, evasionBonus,
                Set.of(UnitClass.TIREUR), EquipmentCategory.DEFENSIVE);
    }

    @Nested
    @DisplayName("Seuils d'expérience et types")
    class ExperienceThresholdTests {

        @Test
        @DisplayName("0-1 exp → LARBIN, 2-4 → VOYOU, 5-7 → MALFRAT, 8+ → BRUTE")
        void shouldPinExperienceThresholds() {
            assertEquals(UnitType.LARBIN, new Unit(0, UnitClass.TIREUR).getType());
            assertEquals(UnitType.LARBIN, new Unit(1, UnitClass.TIREUR).getType());
            assertEquals(UnitType.VOYOU, new Unit(2, UnitClass.TIREUR).getType());
            assertEquals(UnitType.VOYOU, new Unit(4, UnitClass.TIREUR).getType());
            assertEquals(UnitType.MALFRAT, new Unit(5, UnitClass.TIREUR).getType());
            assertEquals(UnitType.MALFRAT, new Unit(7, UnitClass.TIREUR).getType());
            assertEquals(UnitType.BRUTE, new Unit(8, UnitClass.TIREUR).getType());
            assertEquals(UnitType.BRUTE, new Unit(100, UnitClass.TIREUR).getType());
        }

        @Test
        @DisplayName("Stats de base par type : 10/10, 20/20, 50/50, 100/100")
        void shouldPinBaseStatsPerType() {
            assertEquals(10.0, new Unit(0, UnitClass.TIREUR).getAttack());
            assertEquals(10.0, new Unit(0, UnitClass.TIREUR).getDefense());
            assertEquals(20.0, new Unit(2, UnitClass.TIREUR).getAttack());
            assertEquals(50.0, new Unit(5, UnitClass.TIREUR).getDefense());
            assertEquals(100.0, new Unit(8, UnitClass.TIREUR).getAttack());
        }

        @Test
        @DisplayName("gainExperience fait évoluer le type au franchissement de seuil")
        void shouldEvolveTypeWhenCrossingThreshold() {
            Unit unit = new Unit(1, UnitClass.TIREUR);
            unit.gainExperience(1); // total 2 → VOYOU

            assertEquals(UnitType.VOYOU, unit.getType());
            assertEquals(20.0, unit.getAttack());
            assertEquals(20.0, unit.getDefense());

            unit.gainExperience(3); // total 5 → MALFRAT
            assertEquals(UnitType.MALFRAT, unit.getType());
            assertEquals(50.0, unit.getAttack());
        }

        @Test
        @DisplayName("gainExperience sans franchissement ne change pas le type")
        void shouldNotEvolveBelowThreshold() {
            Unit unit = new Unit(0, UnitClass.TIREUR);
            unit.gainExperience(1.5); // total 1.5 → toujours LARBIN

            assertEquals(UnitType.LARBIN, unit.getType());
            assertEquals(10.0, unit.getAttack());
        }

        @Test
        @DisplayName("L'évolution recalcule les stats d'équipement sur la nouvelle base")
        void shouldRecalculateEquipmentStatsOnEvolution() {
            Unit unit = new Unit(7, UnitClass.TIREUR); // MALFRAT atk 50
            unit.addEquipment(firearm(50)); // pdf = 50 × 50% = 25
            assertEquals(25.0, unit.getPdf());

            unit.gainExperience(1); // → BRUTE atk 100, pdf = 100 × 50% = 50

            assertEquals(UnitType.BRUTE, unit.getType());
            assertEquals(50.0, unit.getPdf());
        }
    }

    @Nested
    @DisplayName("Blessure")
    class InjuryTests {

        @Test
        @DisplayName("Blessure : attaque et défense divisées par 2")
        void shouldHalveAttackAndDefenseWhenInjured() {
            Unit unit = new Unit(5, UnitClass.TIREUR); // 50/50
            unit.setInjured(true);
            unit.recalculateBaseStats();

            assertEquals(25.0, unit.getAttack());
            assertEquals(25.0, unit.getDefense());
        }

        @Test
        @DisplayName("Blessure : évasion brute inchangée, pdf recalculé sur l'attaque réduite")
        void shouldPinInjuryEffectOnCalculatedStats() {
            Unit unit = new Unit(5, UnitClass.TIREUR);
            unit.addEquipment(firearm(50));       // pdf = 25
            unit.addEquipment(defensive(40, 30)); // armor = 20, evasion = 30

            unit.setInjured(true);
            unit.recalculateBaseStats();

            // comportement actuel piné : pdf/armor dérivent des stats réduites,
            // l'évasion (somme brute) n'est pas affectée par la blessure
            assertEquals(25.0, unit.getAttack());
            assertEquals(25.0, unit.getDefense());
            assertEquals(12.5, unit.getPdf());
            assertEquals(10.0, unit.getArmor());
            assertEquals(30.0, unit.getEvasion());
        }
    }

    @Nested
    @DisplayName("Formules d'équipement")
    class EquipmentFormulaTests {

        @Test
        @DisplayName("pdf = somme(attaque × pdfBonus/100) des équipements compatibles")
        void shouldComputePdfFromCompatibleEquipment() {
            Unit unit = new Unit(5, UnitClass.TIREUR); // atk 50
            unit.addEquipment(firearm(50)); // +25
            unit.addEquipment(firearm(20)); // +10 — MALFRAT max 1 firearm, refusé

            assertEquals(25.0, unit.getPdf());
        }

        @Test
        @DisplayName("Équipement incompatible ne contribue pas aux stats")
        void shouldIgnoreIncompatibleEquipmentInStats() {
            Unit unit = new Unit(5, UnitClass.SNIPER); // pas TIREUR
            boolean added = unit.addEquipment(firearm(50));

            assertFalse(added);
            assertEquals(0.0, unit.getPdf());
        }

        @Test
        @DisplayName("armor = défense × armBonus/100, evasion = somme brute des bonus")
        void shouldComputeArmorAndEvasion() {
            Unit unit = new Unit(5, UnitClass.TIREUR); // def 50
            unit.addEquipment(defensive(40, 15)); // armor 20, evasion 15

            assertEquals(20.0, unit.getArmor());
            assertEquals(15.0, unit.getEvasion());
        }
    }

    @Nested
    @DisplayName("Limites d'équipement par catégorie")
    class EquipmentLimitTests {

        @Test
        @DisplayName("LARBIN : 1 arme à feu, 1 mêlée, 1 défensif max")
        void shouldEnforceLarbinLimits() {
            Unit unit = new Unit(0, UnitClass.TIREUR);

            assertTrue(unit.canEquip(firearm(10)));
            unit.addEquipment(firearm(10));
            assertFalse(unit.canEquip(firearm(20)));
        }

        @Test
        @DisplayName("VOYOU : 2 équipements défensifs max")
        void shouldEnforceVoyouDefensiveLimit() {
            Unit unit = new Unit(2, UnitClass.TIREUR);

            assertTrue(unit.addEquipment(defensive(10, 0)));
            assertTrue(unit.addEquipment(defensive(20, 0)));
            assertFalse(unit.addEquipment(defensive(30, 0)));
            assertEquals(2, unit.getEquipments().size());
        }

        @Test
        @DisplayName("BRUTE : 1 arme à feu, 3 mêlées, 4 défensifs max")
        void shouldPinBruteLimits() {
            assertEquals(1, UnitType.BRUTE.getMaxFirearms());
            assertEquals(3, UnitType.BRUTE.getMaxMeleeWeapons());
            assertEquals(4, UnitType.BRUTE.getMaxDefensiveEquipment());
        }

        @Test
        @DisplayName("removeEquipment retire par nom et recalcule")
        void shouldRemoveEquipmentAndRecalculate() {
            Unit unit = new Unit(5, UnitClass.TIREUR);
            Equipment gun = firearm(50);
            unit.addEquipment(gun);
            assertEquals(25.0, unit.getPdf());

            assertTrue(unit.removeEquipment(gun));

            assertEquals(0.0, unit.getPdf());
            assertTrue(unit.getEquipments().isEmpty());
        }
    }

    @Nested
    @DisplayName("Seconde classe")
    class SecondClassTests {

        @Test
        @DisplayName("LARBIN et VOYOU ne peuvent jamais prendre de seconde classe")
        void shouldNeverAllowSecondClassForLowTypes() {
            // comportement actuel piné : la classe primaire compte déjà,
            // donc la condition "< 1 classe" est impossible pour LARBIN/VOYOU
            assertFalse(new Unit(0, UnitClass.TIREUR).canAddSecondClass());
            assertFalse(new Unit(3, UnitClass.TIREUR).canAddSecondClass());
        }

        @Test
        @DisplayName("MALFRAT (exp >= 5) avec une seule classe peut évoluer")
        void shouldAllowSecondClassForMalfrat() {
            Unit unit = new Unit(5, UnitClass.TIREUR);

            assertTrue(unit.canAddSecondClass());
            unit.addSecondClass(UnitClass.LEGER);

            assertEquals(2, unit.getClassesSet().size());
            assertTrue(unit.getClassesSet().contains(UnitClass.LEGER));
        }

        @Test
        @DisplayName("Deux classes maximum")
        void shouldRefuseThirdClass() {
            Unit unit = new Unit(8, UnitClass.TIREUR);
            unit.addSecondClass(UnitClass.LEGER);

            assertFalse(unit.canAddSecondClass());
            unit.addSecondClass(UnitClass.SNIPER);

            assertEquals(2, unit.getClassesSet().size());
        }

        @Test
        @DisplayName("Classe dupliquée refusée silencieusement")
        void shouldRefuseDuplicateClass() {
            Unit unit = new Unit(5, UnitClass.TIREUR);
            unit.addSecondClass(UnitClass.TIREUR);

            assertEquals(1, unit.getClassesSet().size());
        }
    }

    @Nested
    @DisplayName("Déplacement et réduction de dégâts")
    class MovementAndReductionTests {

        @Test
        @DisplayName("LEGER = 2 secteurs/tour, toute autre classe = 1")
        void shouldPinMovementHops() {
            assertEquals(2, new Unit(0, UnitClass.LEGER).getMaxMovementHops());
            assertEquals(1, new Unit(0, UnitClass.TIREUR).getMaxMovementHops());
            assertEquals(1, new Unit(0, UnitClass.MASTODONTE).getMaxMovementHops());
        }

        @Test
        @DisplayName("Hops = max des classes (LEGER + TIREUR = 2)")
        void shouldTakeMaxHopsAcrossClasses() {
            Unit unit = new Unit(5, UnitClass.TIREUR);
            unit.addSecondClass(UnitClass.LEGER);

            assertEquals(2, unit.getMaxMovementHops());
        }

        @Test
        @DisplayName("MASTODONTE : 25% de réduction vs PDF et PDC, 0 vs ATK")
        void shouldPinMastodonteReduction() {
            Unit unit = new Unit(5, UnitClass.MASTODONTE);

            assertEquals(0.25, unit.getDamageReduction("PDF"));
            assertEquals(0.25, unit.getDamageReduction("PDC"));
            assertEquals(0.0, unit.getDamageReduction("ATK"));
        }

        @Test
        @DisplayName("Réduction = max parmi les classes de l'unité")
        void shouldTakeMaxReductionAcrossClasses() {
            Unit unit = new Unit(5, UnitClass.TIREUR); // 0 partout
            unit.addSecondClass(UnitClass.MASTODONTE); // 0.25 vs PDF

            assertEquals(0.25, unit.getDamageReduction("PDF"));
            assertEquals(0.0, unit.getDamageReduction("ATK"));
        }

        @Test
        @DisplayName("Critique TIREUR défini mais jamais utilisé en combat")
        void shouldPinUnusedTireurCrit() {
            // comportement actuel piné : règle morte, aucun appel dans Battle
            assertEquals(0.10, UnitClass.TIREUR.getCriticalChance());
            assertEquals(1.5, UnitClass.TIREUR.getCriticalMultiplier());
        }
    }
}
