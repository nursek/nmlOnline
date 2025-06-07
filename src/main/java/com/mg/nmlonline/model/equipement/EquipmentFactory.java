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
            case PISTOLET_CHAUFFANT -> null;
            case FUSIL_IMPULSION -> null;
            case CANON_GLACE -> null;
            case LANCE_FLAMMES -> null;
            case BOMBES_COLLANTES -> null;
            case LANCE_ROQUETTES -> null;
            case LANCE_GRENADES -> null;
            case SNIPER_LEGER -> createSniperLeger();
            case SNIPER_LOURD -> createSniperLourd();
            case SNIPER_COMBAT -> createSniperCombat();
            // CORPS-À-CORPS
            case POING_AMERICAIN -> createPoingAmericain();
            case MATRAQUE_TELESCOPIC -> createMatraqueTelescopic();
            case BATTE_METAL -> createBatteMetal();
            case MACHETTE -> createMachette();
            case HACHE_BUCHERON -> createHacheBucheron();
            case TRONCONNEUSE -> createTronconneuse();
            case MATRAQUE_ELECTRIQUE -> createMatraqueElectrique();
            case GANTELET_ELECTRIQUE -> createGanteletElectrique();
            case PANACHURROS -> null;
            case PANACHOUQUETTE -> null;
            case PANACHOUCROUTE -> null;
            // DEFENSIFS
            case COUTEAU_CUISINE -> createCouteauCuisine();
            case COUTEAU_COMBAT -> createCouteauCombat();
            case TENUE_ULTRA_LEGERE -> createTenueUltraLegere();
            case GRENADE_ASSOURDISSANTE -> null;
            case GRENADE_LACRYMOGENE -> createGrenadeLacrymogene();
            case GILET_PARE_BALLES_LEGER -> createGiletPareBalesLeger();
            case GILET_PARE_BALLES_MOYEN -> createGiletPareBalesMoyen();
            case EQUIPEMENT_MILITAIRE_COMPLET -> createEquipementMilitaireComplet();
            case PROTECTION_DORSALE -> createProtectionDorsale();
            case BOUCLIER_ANTI_EMEUTES -> createBouclierAntiEmeutes();
            case BOUCLIER_BALISTIQUE -> createBouclierBalistique();
            case PROTEGE_DENT -> null;
            case CASQUE_MILITAIRE -> null;
            case GILET_KEVLAR -> null;
            case ARMURE_CONDUCTRICE -> null;
            case AREMURE_THERMORESISTANTE -> null;
            case ARMURE_ISOLANTE -> null;
            case ARMURE_THERMIQUE -> null;
            case TREILLIS_CAMOUFLAGE -> null;
            case GILET_CAMOUFLAGE -> null;
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

    // === ARMES À FEU CLASSE ÉLÉMENTAIRE ===
    //- (F) Pistolet chauffant (Arme à feu) : +100 % Pdf. 900 $.
    //
    //
    //- (E) Fusil à impulsion électromagnétique (Arme à feu) : +250 % Pdf. 2250 $.
    //- (G) Canon à glace (Arme à feu) : +300 % Pdf. 2500 $.
    //- (F) Lance-flammes (Arme à feu) : +400 % Pdf. 3000 $.
    public static FirearmEquipment createPistoletChauffant() {
        return new FirearmEquipment("Pistolet chauffant", 900, 100, 0, 0, Set.of(UnitClass.ELEMENTAIRE));
    }
    public static FirearmEquipment createFusilImpulsion() {
        return new FirearmEquipment("Fusil à impulsion électromagnétique", 2250, 250, 0, 0, Set.of(UnitClass.ELEMENTAIRE));
    }
    public static FirearmEquipment createCanonGlace() {
        return new FirearmEquipment("Canon à glace", 2500, 300, 0, 0, Set.of(UnitClass.ELEMENTAIRE));
    }

    public static FirearmEquipment createLanceFlammes() {
        return new FirearmEquipment("Lance-flammes", 3000, 400, 0, 0, Set.of(UnitClass.ELEMENTAIRE));
    }

    // === ARMES À FEU CLASSE PILOTE DESTRUCTEUR ===
    //- Bombes collantes (Arme à feu) : +80 % Pdf. Dégâts doublés contre les véhicules terrestres et blindés. 3 400 $.
    //- Lance-roquettes (Arme à feu) : +400 % Pdf ; +200 % Arm. Dégâts doublés contre les véhicules aériens. 5 500 $.
    //- Lance-grenades (Arme à feu) : +500 % Pdf ; +100 % Arm. Dégâts doublés contre les véhicules terrestres et blindés. 5 800 $.
    public static FirearmEquipment createBombesCollantes() {
        return new FirearmEquipment("Bombes collantes", 3400, 80, 0, 0, Set.of(UnitClass.PILOTE_DESTRUCTEUR));
    }

    public static FirearmEquipment createLanceRockets() {
        return new FirearmEquipment("Lance-roquettes", 5500, 400, 200, 0, Set.of(UnitClass.PILOTE_DESTRUCTEUR));
    }

    public static FirearmEquipment createLanceGrenades() {
        return new FirearmEquipment("Lance-grenades", 5800, 500, 100, 0, Set.of(UnitClass.PILOTE_DESTRUCTEUR));
    }

    // === ARMES À FEU CLASSE SNIPER ===
    public static FirearmEquipment createSniperLeger() {
        return new FirearmEquipment("Fusil de sniper léger", 1000, 150, 0, 30, Set.of(UnitClass.SNIPER));
    }

    public static FirearmEquipment createSniperLourd() {
        return new FirearmEquipment("Fusil de sniper lourd", 1800, 300, 0, 50, Set.of(UnitClass.SNIPER));
    }
    public static FirearmEquipment createSniperCombat() {
        return new FirearmEquipment("Fusil de sniper de combat", 2300, 400, 0, 60, Set.of(UnitClass.SNIPER));
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

    private static Equipment createMatraqueElectrique() {
        return new MeleeEquipment("Matraque électrique", 450, 50, Set.of(UnitClass.ELEMENTAIRE));
    }

    private static Equipment createGanteletElectrique() {
        return new MeleeEquipment("Gantelets électrique", 1000, 85, Set.of(UnitClass.ELEMENTAIRE));
    }

    private static Equipment createCouteauCuisine() {
        return new MeleeEquipment("Couteau de cuisine", 200, 40, Set.of(UnitClass.SNIPER));
    }

    private static Equipment createCouteauCombat() {
        return new MeleeEquipment("Couteau de combat", 300, 60, Set.of(UnitClass.SNIPER));
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