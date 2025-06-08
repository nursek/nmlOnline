package com.mg.nmlonline.model.equipement;

import lombok.Getter;

@Getter
public enum EquipmentType {
    // ARMES À FEU
        // LÉGER
        PISTOLET_9MM("Pistolet 9mm"),
        PISTOLET_MITRAILLEUR("Pistolet-mitrailleur"),
        HK_MP7("HK-MP7"),
        // TIREUR
        MITRAILLEUSE("Mitrailleuse"),
        FUSIL_ASSAUT("Fusil d'assaut"),
        MINI_MACHINE_GUN("Mini machine gun"),
        MINI_MACHINE_GUN_CM("Mini machine gun [CM]"),
        MINI_MACHINE_GUN_MP("Mini machine gun [MP]"),
        MINI_MACHINE_GUN_CM_MP("Mini machine gun [CM] [MP]"),
        // MASTODONTE
        FUSIL_POMPE("Fusil à pompe"),
        WINCHESTER("Winchester"),
        TROMBLON("Tromblon"),
        // ELEMENTAIRE
        PISTOLET_CHAUFFANT("Pistolet chauffant"),
        FUSIL_IMPULSION("Fusil à impulsion"),
        CANON_GLACE("Canon à glace"),
        LANCE_FLAMMES("Lance-flammes"),
        //PILOTE_DESTRUCTEUR
        BOMBES_COLLANTES("Bombes collantes"),
        LANCE_ROQUETTES("Lance-roquettes"),
        LANCE_GRENADES("Lance-grenades"),
        // SNIPER
        SNIPER_LEGER("Fusil de sniper léger"),
        SNIPER_LOURD("Fusil de sniper lourd"),
        SNIPER_COMBAT("Fusil de sniper de combat"),
    // CORPS-À-CORPS
        // LÉGERx
        POING_AMERICAIN("Poing américain"),
        MATRAQUE_TELESCOPIC("Matraque télescopique"),
        // TIREUR
        BATTE_METAL("Batte de métal"),
        MACHETTE("Machette"),
        // MASTODONTE
        HACHE_BUCHERON("Hache de bûcheron"),
        TRONCONNEUSE("Tronçonneuse"),
        // ELEMENTAIRE
        MATRAQUE_ELECTRIQUE("Matraque électrique"),
        GANTELET_ELECTRIQUE("Gantelets électrique"),
        // PILOTE_DESTRUCTEUR
        PANACHURROS("Panachurros"),
        PANACHOUQUETTE("Panachouquette"),
        PANACHOUCROUTE("Panachoucroute"),// PILOTE_DESTRUCTEUR
        // SNIPER
        COUTEAU_CUISINE("Couteau de cuisine"),
        COUTEAU_COMBAT("Couteau de combat"),
    // DEFENSIFS
        // LÉGER
        TENUE_ULTRA_LEGERE("Tenue ultra légère"),
        GRENADE_ASSOURDISSANTE("Grenade assourdissante"),
        GRENADE_LACRYMOGENE("Grenade lacrymogène"),
        FIBRE_CHAUFFANTE("Tenue légère en fibre chauffante"),
        // TIREUR
        GILET_PARE_BALLES_LEGER("Gilet pare-balles léger"),
        GILET_PARE_BALLES_MOYEN("Gilet pare-balles moyen"),
        EQUIPEMENT_MILITAIRE_COMPLET("Équipement militaire complet"),
        // MASTODONTE
        PROTECTION_DORSALE("Protection dorsale"),
        BOUCLIER_ANTI_EMEUTES("Bouclier anti-émeutes"),
        BOUCLIER_BALISTIQUE("Bouclier balistique"),
        // PILOTE_DESTRUCTEUR
        PROTEGE_DENT("Protège-dents"),
        CASQUE_MILITAIRE("Casque militaire"),
        GILET_KEVLAR("Gilet Kevlar"),
        // ELEMENTAIRE
        ARMURE_CONDUCTRICE("Armure conductrice"),
        AREMURE_THERMORESISTANTE("Armure thermorésistante"),
        ARMURE_ISOLANTE("Armure isolante"),
        ARMURE_THERMIQUE("Armure thermique"),
        // SNIPER
        TREILLIS_CAMOUFLAGE("Treillis de camouflage urbain"),
        GILET_CAMOUFLAGE("Gilet de camouflage optique");


    private final String displayName;

    EquipmentType(String displayName) {
        this.displayName = displayName;
    }
    
}