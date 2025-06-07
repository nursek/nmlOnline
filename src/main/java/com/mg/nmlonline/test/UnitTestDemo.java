
package com.mg.nmlonline.test;

import com.mg.nmlonline.model.unit.Unit;
import com.mg.nmlonline.model.unit.UnitClass;
import com.mg.nmlonline.model.equipement.EquipmentFactory;

/**
 * Classe de test pour démontrer la création et l'affichage des unités
 */
public class UnitTestDemo {

    public static void main(String[] args) {
        System.out.println("=== DÉMO CRÉATION ET AFFICHAGE DES UNITÉS ===\n");
        
        // Test 1: Création d'unités de base
        testBasicUnitCreation();
        
        // Test 2: Évolution des unités
        testUnitEvolution();
        
        // Test 3: Équipement des unités
        testUnitEquipment();
        
        // Test 4: Unités avec double classe
        testDualClassUnits();
        
        // Test 5: Reproduction des exemples donnés
        testExampleUnits();
    }

    /**
     * Test 1: Création d'unités de base
     */
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

    /**
     * Test 2: Évolution des unités
     */
    private static void testUnitEvolution() {
        System.out.println("🔹 TEST 2: Évolution des unités");
        System.out.println("================================");
        
        Unit unit = new Unit(10, "Unité Évolutive", UnitClass.TIREUR);
        System.out.println("Départ: " + unit);
        
        // Évolution en Voyou
        unit.gainExperience(2);
        System.out.println("Voyou:  " + unit);
        
        // Évolution en Malfrat
        unit.gainExperience(3);
        System.out.println("Malfrat:" + unit);
        
        // Évolution en Brute
        unit.gainExperience(3);
        System.out.println("Brute:  " + unit);
        System.out.println();
    }

    /**
     * Test 3: Équipement des unités
     */
    private static void testUnitEquipment() {
        System.out.println("🔹 TEST 3: Équipement des unités");
        System.out.println("=================================");
        
        // Larbin Léger équipé
        Unit larbinLeger = new Unit(20, "Larbin Équipé", UnitClass.LEGER);
        larbinLeger.equip(EquipmentFactory.createPistoletMitrailleur());
        larbinLeger.equip(EquipmentFactory.createMatraqueTelescopic());
        larbinLeger.equip(EquipmentFactory.createTenueUltraLegere());
        System.out.println(larbinLeger);
        
        // Brute Tireur équipée
        Unit bruteTireur = new Unit(21, "Brute Équipée", UnitClass.TIREUR);
        bruteTireur.gainExperience(8);
        bruteTireur.equip(EquipmentFactory.createMiniMachineGun());
        bruteTireur.equip(EquipmentFactory.createMachette());
        bruteTireur.equip(EquipmentFactory.createBatteMetal());
        bruteTireur.equip(EquipmentFactory.createGiletPareBalesMoyen());
        bruteTireur.equip(EquipmentFactory.createEquipementMilitaireComplet());
        System.out.println(bruteTireur);
        
        // Mastodonte avec Tromblon
        Unit mastodonte = new Unit(22, "Mastodonte Destructeur", UnitClass.MASTODONTE);
        mastodonte.gainExperience(8);
        mastodonte.equip(EquipmentFactory.createTromblon());
        mastodonte.equip(EquipmentFactory.createTronconneuse());
        mastodonte.equip(EquipmentFactory.createHacheBucheron());
        mastodonte.equip(EquipmentFactory.createBouclierBalistique());
        mastodonte.equip(EquipmentFactory.createBouclierAntiEmeutes());
        System.out.println(mastodonte);
        System.out.println();
    }

    /**
     * Test 4: Unités avec double classe
     */
    private static void testDualClassUnits() {
        System.out.println("🔹 TEST 4: Unités avec double classe");
        System.out.println("====================================");
        
        // Malfrat qui peut avoir une seconde classe
        Unit malfrat = new Unit(30, "Malfrat Polyvalent", UnitClass.TIREUR);
        malfrat.gainExperience(5); // Devient Malfrat
        
        System.out.println("Avant seconde classe: " + malfrat);
        System.out.println("Peut ajouter seconde classe: " + malfrat.canAddSecondClass());
        
        malfrat.addSecondClass(UnitClass.MASTODONTE);
        System.out.println("Après seconde classe: " + malfrat);
        
        // Équipement avec les deux classes
        malfrat.equip(EquipmentFactory.createMiniMachineGun()); // Compatible Tireur
        malfrat.equip(EquipmentFactory.createTronconneuse()); // Compatible Mastodonte
        malfrat.equip(EquipmentFactory.createGiletPareBalesMoyen()); // Compatible Tireur
        malfrat.equip(EquipmentFactory.createBouclierAntiEmeutes()); // Compatible Mastodonte
        System.out.println("Équipé: " + malfrat);
        System.out.println();
    }

    /**
     * Test 5: Reproduction des exemples donnés
     */
    private static void testExampleUnits() {
        System.out.println("🔹 TEST 5: Reproduction des exemples donnés");
        System.out.println("============================================");
        
        // (T) (M) Brute n°3 (9 Exp) : Mini machine gun. Tronçonneuse. Tronçonneuse. Tronçonneuse. 
        // Bouclier anti-émeutes. Équipement militaire complet. Bouclier balistique. 
        // 100 Atk + 400 Pdf + 300 Pdc / 100 Def + 730 Arm.
        Unit brute3 = new Unit(3, "Brute", UnitClass.TIREUR);
        brute3.gainExperience(9);
        brute3.addSecondClass(UnitClass.MASTODONTE);
        brute3.equip(EquipmentFactory.createMiniMachineGun());
        brute3.equip(EquipmentFactory.createTronconneuse());
        brute3.equip(EquipmentFactory.createTronconneuse());
        brute3.equip(EquipmentFactory.createTronconneuse());
        brute3.equip(EquipmentFactory.createBouclierAntiEmeutes());
        brute3.equip(EquipmentFactory.createEquipementMilitaireComplet());
        brute3.equip(EquipmentFactory.createBouclierBalistique());
        System.out.println("Exemple 1: " + brute3);
        
        // (L) Larbin n°6 (0 Exp) : Pistolet-mitrailleur. Tenue ultralégère. 
        // 10 Atk + 15 Pdf / 10 Def + 7,5 Arm. Esquive : 10 %.
        Unit larbin6 = new Unit(6, "Larbin", UnitClass.LEGER);
        larbin6.equip(EquipmentFactory.createPistoletMitrailleur());
        larbin6.equip(EquipmentFactory.createTenueUltraLegere());
        System.out.println("Exemple 2: " + larbin6);
        
        // (T) Malfrat n°3 (6 Exp) : Mini machine gun. Machette. Machette. 
        // Gilet pare-balles moyen. Équipement militaire complet. 
        // 50 Atk + 200 Pdf + 75 Pdc / 50 Def + 150 Arm.
        Unit malfrat3 = new Unit(3, "Malfrat", UnitClass.TIREUR);
        malfrat3.gainExperience(6);
        malfrat3.equip(EquipmentFactory.createMiniMachineGun());
        malfrat3.equip(EquipmentFactory.createMachette());
        malfrat3.equip(EquipmentFactory.createMachette());
        malfrat3.equip(EquipmentFactory.createGiletPareBalesMoyen());
        malfrat3.equip(EquipmentFactory.createEquipementMilitaireComplet());
        System.out.println("Exemple 3: " + malfrat3);
        
        System.out.println();
        System.out.println("✅ Tests terminés avec succès !");
    }

}