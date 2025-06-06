package com.mg.nmlonline.model.equipement;

import com.mg.nmlonline.model.unit.UnitClass;
import java.util.Set;

// Factory pour créer les équipements prédéfinis
public class EquipmentFactory {

    public static Equipment createEquipmentByType(EquipmentType type) {
        return switch (type) {
            // ARMES À FEU
            case PISTOLET_9MM -> createPistolet9mm();
            case PISTOLET_MITRAILLEUR -> createPistoletMitrailleur();
            case HK_MP7 -> createHKMP7();
            case MITRAILLEUSE -> createMitrailleuse();
            case FUSIL_ASSAUT -> createFusilAssaut();
            case MINI_MACHINE_GUN -> createMiniMachineGun();
            case FUSIL_POMPE -> createFusilPompe();
            case WINCHESTER -> createWinchester();
            case TROMBLON -> createTromblon();
            // CORPS-À-CORPS
            case POING_AMERICAIN -> createPoingAmericain();
            case MATRAQUE_TELESCOPIC -> createMatraqueTelescopic();
            case BATTE_METAL -> createBatteMetal();
            case MACHETTE -> createMachette();
            case HACHE_BUCHERON -> createHacheBucheron();
            case TRONCONNEUSE -> createTronconneuse();
            // DEFENSIFS
            case TENUE_ULTRA_LEGERE -> createTenueUltraLegere();
            case GRENADE_LACRYMOGENE -> createGrenadeLacrymogene();
            case GILET_PARE_BALLES_LEGER -> createGiletPareBalesLeger();
            case GILET_PARE_BALLES_MOYEN -> createGiletPareBalesMoyen();
            case EQUIPEMENT_MILITAIRE_COMPLET -> createEquipementMilitaireComplet();
            case PROTECTION_DORSALE -> createProtectionDorsale();
            case BOUCLIER_ANTI_EMEUTES -> createBouclierAntiEmeutes();
            case BOUCLIER_BALISTIQUE -> createBouclierBalistique();
        };
    }

    // === ARMES À FEU CLASSE LÉGER ===
    public static FirearmEquipment createPistolet9mm() {
        return new FirearmEquipment("Pistolet 9mm", 400, 80, 0, 0, Set.of(UnitClass.LEGER));
    }
    
    public static FirearmEquipment createPistoletMitrailleur() {
        return new FirearmEquipment("Pistolet-mitrailleur", 850, 150, 0, 25, Set.of(UnitClass.LEGER));
    }
    
    public static FirearmEquipment createHKMP7() {
        return new FirearmEquipment("HK-MP7", 1600, 300, 0, 25, Set.of(UnitClass.LEGER));
    }
    
    // === ARMES À FEU CLASSE TIREUR ===
    public static FirearmEquipment createMitrailleuse() {
        return new FirearmEquipment("Mitrailleuse", 1500, 200, 0, 50, Set.of(UnitClass.TIREUR));
    }
    
    public static FirearmEquipment createFusilAssaut() {
        return new FirearmEquipment("Fusil d'assaut", 2300, 350, 0, 60, Set.of(UnitClass.TIREUR));
    }
    
    public static FirearmEquipment createMiniMachineGun() {
        return new FirearmEquipment("Mini machine gun", 2600, 400, 0, 80, Set.of(UnitClass.TIREUR));
    }
    
    // === ARMES À FEU CLASSE MASTODONTE ===
    public static FirearmEquipment createFusilPompe() {
        return new FirearmEquipment("Fusil à pompe", 1000, 0, 200, 0, Set.of(UnitClass.MASTODONTE));
    }
    
    public static FirearmEquipment createWinchester() {
        return new FirearmEquipment("Winchester", 1500, 0, 300, 0, Set.of(UnitClass.MASTODONTE));
    }
    
    public static FirearmEquipment createTromblon() {
        return new FirearmEquipment("Tromblon", 2000, 0, 400, 0, Set.of(UnitClass.MASTODONTE));
    }
    
    // === ARMES DE CORPS-À-CORPS ===
    public static MeleeEquipment createPoingAmericain() {
        return new MeleeEquipment("Poing américain", 100, 20, Set.of(UnitClass.LEGER));
    }
    
    public static MeleeEquipment createMatraqueTelescopic() {
        return new MeleeEquipment("Matraque télescopique", 200, 40, Set.of(UnitClass.LEGER));
    }
    
    public static MeleeEquipment createBatteMetal() {
        return new MeleeEquipment("Batte de métal", 250, 50, Set.of(UnitClass.TIREUR));
    }
    
    public static MeleeEquipment createMachette() {
        return new MeleeEquipment("Machette", 375, 75, Set.of(UnitClass.TIREUR));
    }
    
    public static MeleeEquipment createHacheBucheron() {
        return new MeleeEquipment("Hache de bûcheron", 450, 90, Set.of(UnitClass.MASTODONTE));
    }
    
    public static MeleeEquipment createTronconneuse() {
        return new MeleeEquipment("Tronçonneuse", 500, 100, Set.of(UnitClass.MASTODONTE));
    }
    
    // === ÉQUIPEMENTS DÉFENSIFS ===
    public static DefensiveEquipment createTenueUltraLegere() {
        return new DefensiveEquipment("Tenue ultra légère", 750, 50, 10, Set.of(UnitClass.LEGER));
    }
    
    public static DefensiveEquipment createGrenadeLacrymogene() {
        return new DefensiveEquipment("Grenade lacrymogène", 2500, 0, 25, Set.of(UnitClass.LEGER));
    }
    
    public static DefensiveEquipment createGiletPareBalesLeger() {
        return new DefensiveEquipment("Gilet pare-balles léger", 500, 50, 0, Set.of(UnitClass.TIREUR));
    }
    
    public static DefensiveEquipment createGiletPareBalesMoyen() {
        return new DefensiveEquipment("Gilet pare-balles moyen", 1000, 100, 0, Set.of(UnitClass.TIREUR));
    }
    
    public static DefensiveEquipment createEquipementMilitaireComplet() {
        return new DefensiveEquipment("Équipement militaire complet", 2000, 200, 0, Set.of(UnitClass.TIREUR));
    }
    
    public static DefensiveEquipment createProtectionDorsale() {
        return new DefensiveEquipment("Protection dorsale", 250, 30, 0, Set.of(UnitClass.MASTODONTE));
    }
    
    public static DefensiveEquipment createBouclierAntiEmeutes() {
        return new DefensiveEquipment("Bouclier anti-émeutes", 1500, 150, 0, Set.of(UnitClass.MASTODONTE));
    }
    
    public static DefensiveEquipment createBouclierBalistique() {
        return new DefensiveEquipment("Bouclier balistique", 3000, 300, 0, Set.of(UnitClass.MASTODONTE));
    }
}