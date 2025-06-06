package com.mg.nmlonline.model.equipement;

import lombok.Getter;

@Getter
public enum EquipmentType {
    // ARMES À FEU
    PISTOLET_9MM("Pistolet 9mm"),
    PISTOLET_MITRAILLEUR("Pistolet-mitrailleur"),
    HK_MP7("HK-MP7"),
    MITRAILLEUSE("Mitrailleuse"),
    FUSIL_ASSAUT("Fusil d'assaut"),
    MINI_MACHINE_GUN("Mini machine gun"),
    FUSIL_POMPE("Fusil à pompe"),
    WINCHESTER("Winchester"),
    TROMBLON("Tromblon"),
    // CORPS-À-CORPS
    POING_AMERICAIN("Poing américain"),
    MATRAQUE_TELESCOPIC("Matraque télescopique"),
    BATTE_METAL("Batte de métal"),
    MACHETTE("Machette"),
    HACHE_BUCHERON("Hache de bûcheron"),
    TRONCONNEUSE("Tronçonneuse"),
    // DEFENSIFS
    TENUE_ULTRA_LEGERE("Tenue ultra légère"),
    GRENADE_LACRYMOGENE("Grenade lacrymogène"),
    GILET_PARE_BALLES_LEGER("Gilet pare-balles léger"),
    GILET_PARE_BALLES_MOYEN("Gilet pare-balles moyen"),
    EQUIPEMENT_MILITAIRE_COMPLET("Équipement militaire complet"),
    PROTECTION_DORSALE("Protection dorsale"),
    BOUCLIER_ANTI_EMEUTES("Bouclier anti-émeutes"),
    BOUCLIER_BALISTIQUE("Bouclier balistique");

    private final String displayName;

    EquipmentType(String displayName) {
        this.displayName = displayName;
    }

    public static EquipmentType fromDisplayName(String name) {
        for (EquipmentType type : values()) {
            if (type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Aucun EquipmentType pour le nom: " + name);
    }
}