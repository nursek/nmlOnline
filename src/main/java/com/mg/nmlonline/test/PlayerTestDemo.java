package com.mg.nmlonline.test;

import com.mg.nmlonline.model.player.Player;
import com.mg.nmlonline.model.unit.Unit;
import com.mg.nmlonline.model.unit.UnitClass;
import com.mg.nmlonline.model.equipement.EquipmentFactory;

/**
 * Test de la classe Player avec tri des unités
 */
public class PlayerTestDemo {

    public static void main(String[] args) {
        System.out.println("=== DÉMO CLASSE PLAYER ===\n");
        //testComplexArmy();
        testFileArmy();
    }

    private static void testComplexArmy() {
        System.out.println("🔹 TEST: Armée complexe (reproduction exemple)");
        System.out.println("===============================================");
        
        Player player = new Player("Général Suprême");
        
        // Création de plusieurs unités pour tester le tri complet
        
        // Brute avec défense maximale
        Unit brute1 = new Unit(2, "Brute", UnitClass.TIREUR);
        brute1.gainExperience(9);
        brute1.addSecondClass(UnitClass.PILOTE_DESTRUCTEUR);
        brute1.equipFirearm(EquipmentFactory.createMiniMachineGun()); // Lance-roquettes simulé
        brute1.equipDefensive(EquipmentFactory.createGiletPareBalesMoyen()); // Casque militaire simulé
        brute1.equipDefensive(EquipmentFactory.createEquipementMilitaireComplet());
        
        // Brute avec défense légèrement inférieure
        Unit brute2 = new Unit(3, "Brute", UnitClass.TIREUR);
        brute2.gainExperience(9);
        brute2.addSecondClass(UnitClass.MASTODONTE);
        brute2.equipFirearm(EquipmentFactory.createMiniMachineGun());
        brute2.equipDefensive(EquipmentFactory.createBouclierAntiEmeutes());
        brute2.equipDefensive(EquipmentFactory.createEquipementMilitaireComplet());
        brute2.equipDefensive(EquipmentFactory.createBouclierBalistique());
        brute2.equipDefensive(EquipmentFactory.createBouclierBalistique());
        brute2.equipDefensive(EquipmentFactory.createBouclierBalistique());
        
        // Brute avec moins d'expérience
        Unit brute3 = new Unit(4, "Brute", UnitClass.LEGER);
        brute3.gainExperience(8.5);
        brute3.addSecondClass(UnitClass.MASTODONTE);

        brute3.equipFirearm(EquipmentFactory.createTromblon());
        brute3.equipDefensive(EquipmentFactory.createTenueUltraLegere());
        brute3.equipDefensive(EquipmentFactory.createBouclierAntiEmeutes());
        brute3.equipDefensive(EquipmentFactory.createBouclierBalistique());
        
        // Malfrats
        Unit malfrat1 = new Unit(1, "Malfrat", UnitClass.MASTODONTE);
        malfrat1.gainExperience(7);
        malfrat1.addSecondClass(UnitClass.TIREUR);
        malfrat1.equipFirearm(EquipmentFactory.createTromblon());
        malfrat1.equipDefensive(EquipmentFactory.createGiletPareBalesMoyen());
        malfrat1.equipDefensive(EquipmentFactory.createBouclierAntiEmeutes());
        malfrat1.equipDefensive(EquipmentFactory.createBouclierBalistique());
        
        Unit malfrat2 = new Unit(2, "Malfrat", UnitClass.TIREUR);
        malfrat2.addSecondClass(UnitClass.MASTODONTE);
        malfrat2.gainExperience(6);
        malfrat2.equipFirearm(EquipmentFactory.createMiniMachineGun());
        malfrat2.equipDefensive(EquipmentFactory.createGiletPareBalesMoyen());
        malfrat2.equipDefensive(EquipmentFactory.createBouclierAntiEmeutes());
        malfrat2.equipDefensive(EquipmentFactory.createBouclierBalistique());
        
        // Voyous
        Unit voyou1 = new Unit(2, "Voyou", UnitClass.TIREUR);
        voyou1.gainExperience(2);
        voyou1.equipFirearm(EquipmentFactory.createMitrailleuse());
        voyou1.equipDefensive(EquipmentFactory.createGiletPareBalesLeger());
        voyou1.equipDefensive(EquipmentFactory.createGiletPareBalesMoyen());
        
        Unit voyou2 = new Unit(16, "Voyou", UnitClass.LEGER);
        voyou2.gainExperience(2);
        voyou2.equipFirearm(EquipmentFactory.createHKMP7());
        voyou2.equipDefensive(EquipmentFactory.createTenueUltraLegere());

        Unit larbin = new Unit(5, "Larbin", UnitClass.LEGER);
        larbin.gainExperience(1);
        larbin.equipFirearm(EquipmentFactory.createHKMP7());
        larbin.equipDefensive(EquipmentFactory.createTenueUltraLegere());
        
        // Ajout dans l'ordre désordonné pour tester le tri
        player.addUnit(voyou1);
        player.addUnit(malfrat2);
        player.addUnit(brute3);
        player.addUnit(voyou2);
        player.addUnit(brute1);
        player.addUnit(malfrat1);
        player.addUnit(brute2);
        player.addUnit(larbin);
        
        player.displayArmy();
        System.out.println("\n✅ Tests Player terminés !");
    }

    private static void testFileArmy(){
        System.out.println("🔹 TEST: FILE ARMY (reproduction exemple)");
        System.out.println("===============================================");

        Player player = new Player("Ratio Antoine");
        try {
            player.fromFile("src/main/resources/army_example.txt");
            player.displayArmy();
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du fichier d'armée : " + e.getMessage());
        }
    }
}