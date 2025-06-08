package com.mg.nmlonline.test;

import com.mg.nmlonline.model.unit.Unit;
import com.mg.nmlonline.model.unit.UnitClass;
import com.mg.nmlonline.model.equipement.EquipmentFactory;
import com.mg.nmlonline.model.equipement.EquipmentType;

public class UnitTestDemo {

    public static void main(String[] args) {
        System.out.println("=== DÉMO CRÉATION ET AFFICHAGE DES UNITÉS ===\n");

        testBasicUnitCreation();
        testUnitEvolution();
        testUnitEquipment();
        testDualClassUnits();
        testExampleUnits();
    }

    private static void testBasicUnitCreation() {
        System.out.println("🔹 TEST 1: Création d'unités de base");
        System.out.println("=====================================");

        Unit larbinLeger = new Unit(1, "Larbin Léger", UnitClass.LEGER);
        Unit larbinTireur = new Unit(2, "Larbin Tireur", UnitClass.TIREUR);
        Unit larbinMastodonte = new Unit(3, "Larbin Mastodonte", UnitClass.MASTODONTE);
        Unit larbinSniper = new Unit(4, "Larbin Sniper", UnitClass.SNIPER);
        Unit larbinPilote = new Unit(5, "Larbin Pilote", UnitClass.PILOTE_DESTRUCTEUR);

        System.out.println(larbinLeger);
        System.out.println(larbinTireur);
        System.out.println(larbinMastodonte);
        System.out.println(larbinSniper);
        System.out.println(larbinPilote);
        System.out.println();
    }

    private static void testUnitEvolution() {
        System.out.println("🔹 TEST 2: Évolution des unités");
        System.out.println("================================");

        Unit unit = new Unit(10, "Unité Évolutive", UnitClass.TIREUR);
        System.out.println("Départ: " + unit);

        unit.gainExperience(2);
        System.out.println("Voyou:  " + unit);

        unit.gainExperience(3);
        System.out.println("Malfrat:" + unit);

        unit.gainExperience(3);
        System.out.println("Brute:  " + unit);
        System.out.println();
    }

    private static void testUnitEquipment() {
        System.out.println("🔹 TEST 3: Équipement des unités");
        System.out.println("=================================");

        Unit larbinLeger = new Unit(20, "Larbin Équipé", UnitClass.LEGER);
        larbinLeger.equip(EquipmentFactory.createFromEnum(EquipmentType.PISTOLET_MITRAILLEUR));
        larbinLeger.equip(EquipmentFactory.createFromEnum(EquipmentType.MATRAQUE_TELESCOPIC));
        larbinLeger.equip(EquipmentFactory.createFromEnum(EquipmentType.TENUE_ULTRA_LEGERE));
        System.out.println(larbinLeger);

        Unit bruteTireur = new Unit(21, "Brute Équipée", UnitClass.TIREUR);
        bruteTireur.gainExperience(8);
        bruteTireur.equip(EquipmentFactory.createFromEnum(EquipmentType.MINI_MACHINE_GUN));
        bruteTireur.equip(EquipmentFactory.createFromEnum(EquipmentType.MACHETTE));
        bruteTireur.equip(EquipmentFactory.createFromEnum(EquipmentType.BATTE_METAL));
        bruteTireur.equip(EquipmentFactory.createFromEnum(EquipmentType.GILET_PARE_BALLES_MOYEN));
        bruteTireur.equip(EquipmentFactory.createFromEnum(EquipmentType.EQUIPEMENT_MILITAIRE_COMPLET));
        System.out.println(bruteTireur);

        Unit mastodonte = new Unit(22, "Mastodonte Destructeur", UnitClass.MASTODONTE);
        mastodonte.gainExperience(8);
        mastodonte.equip(EquipmentFactory.createFromEnum(EquipmentType.TROMBLON));
        mastodonte.equip(EquipmentFactory.createFromEnum(EquipmentType.TRONCONNEUSE));
        mastodonte.equip(EquipmentFactory.createFromEnum(EquipmentType.HACHE_BUCHERON));
        mastodonte.equip(EquipmentFactory.createFromEnum(EquipmentType.BOUCLIER_BALISTIQUE));
        mastodonte.equip(EquipmentFactory.createFromEnum(EquipmentType.BOUCLIER_ANTI_EMEUTES));
        System.out.println(mastodonte);
        System.out.println();
    }

    private static void testDualClassUnits() {
        System.out.println("🔹 TEST 4: Unités avec double classe");
        System.out.println("====================================");

        Unit malfrat = new Unit(30, "Malfrat Polyvalent", UnitClass.TIREUR);
        malfrat.gainExperience(5);

        System.out.println("Avant seconde classe: " + malfrat);
        System.out.println("Peut ajouter seconde classe: " + malfrat.canAddSecondClass());

        malfrat.addSecondClass(UnitClass.MASTODONTE);
        System.out.println("Après seconde classe: " + malfrat);

        malfrat.equip(EquipmentFactory.createFromEnum(EquipmentType.MINI_MACHINE_GUN));
        malfrat.equip(EquipmentFactory.createFromEnum(EquipmentType.TRONCONNEUSE));
        malfrat.equip(EquipmentFactory.createFromEnum(EquipmentType.GILET_PARE_BALLES_MOYEN));
        malfrat.equip(EquipmentFactory.createFromEnum(EquipmentType.BOUCLIER_ANTI_EMEUTES));
        System.out.println("Équipé: " + malfrat);
        System.out.println();
    }

    private static void testExampleUnits() {
        System.out.println("🔹 TEST 5: Reproduction des exemples donnés");
        System.out.println("============================================");

        Unit brute3 = new Unit(0, "Brute", UnitClass.TIREUR);
        brute3.gainExperience(9);
        brute3.addSecondClass(UnitClass.MASTODONTE);
        brute3.equip(EquipmentFactory.createFromEnum(EquipmentType.MINI_MACHINE_GUN));
        brute3.equip(EquipmentFactory.createFromEnum(EquipmentType.TRONCONNEUSE));
        brute3.equip(EquipmentFactory.createFromEnum(EquipmentType.TRONCONNEUSE));
        brute3.equip(EquipmentFactory.createFromEnum(EquipmentType.TRONCONNEUSE));
        brute3.equip(EquipmentFactory.createFromEnum(EquipmentType.BOUCLIER_ANTI_EMEUTES));
        brute3.equip(EquipmentFactory.createFromEnum(EquipmentType.EQUIPEMENT_MILITAIRE_COMPLET));
        brute3.equip(EquipmentFactory.createFromEnum(EquipmentType.BOUCLIER_BALISTIQUE));
        System.out.println("Exemple 1: " + brute3);

        Unit larbin6 = new Unit(0, "Larbin", UnitClass.LEGER);
        larbin6.equip(EquipmentFactory.createFromEnum(EquipmentType.PISTOLET_MITRAILLEUR));
        larbin6.equip(EquipmentFactory.createFromEnum(EquipmentType.TENUE_ULTRA_LEGERE));
        System.out.println("Exemple 2: " + larbin6);

        Unit malfrat3 = new Unit(0, "Malfrat", UnitClass.TIREUR);
        malfrat3.gainExperience(6);
        malfrat3.equip(EquipmentFactory.createFromEnum(EquipmentType.MINI_MACHINE_GUN));
        malfrat3.equip(EquipmentFactory.createFromEnum(EquipmentType.MACHETTE));
        malfrat3.equip(EquipmentFactory.createFromEnum(EquipmentType.MACHETTE));
        malfrat3.equip(EquipmentFactory.createFromEnum(EquipmentType.GILET_PARE_BALLES_MOYEN));
        malfrat3.equip(EquipmentFactory.createFromEnum(EquipmentType.EQUIPEMENT_MILITAIRE_COMPLET));
        System.out.println("Exemple 3: " + malfrat3);

        System.out.println();
        System.out.println("✅ Tests terminés avec succès !");
    }
}