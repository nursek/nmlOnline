package com.mg.nmlonline.domain.model.vehicle;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Énumération des types de véhicules avec leurs caractéristiques.
 *
 * Chaque véhicule nécessite un pilote (classe PILOTE_DESTRUCTEUR).
 * Les passagers embarqués n'interviennent pas en combat de transit.
 */
@Getter
@AllArgsConstructor
public enum VehicleType {

    /**
     * Véhicule surmonté d'une tourelle.
     * Nécessite un pilote + 1 unité à la tourelle.
     * 25 Pdf / 40 Def. 1 300 $. 2 quartiers/tour.
     */
    TOURELLE("Véhicule à tourelle", 25, 40, 1300, 2, 1, 0, false, false),

    /**
     * Véhicule de transport de troupes léger.
     * 10 places + pilote. 50 Def. 4 000 $. 2 quartiers/tour.
     */
    VTT_LEGER("VTT léger", 0, 50, 4000, 2, 10, 0, false, false),

    /**
     * Véhicule de transport de troupes blindé.
     * 10 places + pilote. 100 Pdf / 150 Def. 6 500 $. 2 quartiers/tour.
     * Fait feu en transit sur les unités rencontrées.
     */
    VTT_BLINDE("VTT blindé", 100, 150, 6500, 2, 10, 0, true, false),

    /**
     * Tank de combat.
     * Pilote uniquement. 125 Pdf / 250 Def. 7 500 $. 1 quartier/tour.
     * Résistance de 50 %.
     */
    TANK("Tank de combat", 125, 250, 7500, 1, 0, 50, false, false),

    /**
     * Hélicoptère de combat.
     * Pilote + 5 passagers. 250 Pdf / 125 Def. 9 000 $. 2 quartiers/tour.
     * Chance de toucher variable selon l'attaquant.
     */
    HELICOPTERE("Hélicoptère de combat", 250, 125, 9000, 2, 5, 0, true, true),

    /**
     * Avion de transport.
     * Pilote + 50 passagers. 0 Pdf / 1 000 Def. 15 000 $. 4 quartiers/tour.
     * Immunité aux unités au sol. Survie au crash variable.
     * N'intervient pas en combat au sol.
     */
    AVION_TRANSPORT("Avion de transport", 0, 1000, 15000, 4, 50, 0, false, true);

    private final String displayName;
    private final double basePdf;
    private final double baseDefense;
    private final int cost;
    private final int speed;           // Nombre max de secteurs par tour
    private final int capacity;        // Nombre de passagers (hors pilote)
    private final int resistance;      // % de résistance aux dégâts (ex: Tank = 50)
    private final boolean firesInTransit; // Tire sur les ennemis lors du transit
    private final boolean isAerial;    // Véhicule aérien (hélicoptère, avion)
}