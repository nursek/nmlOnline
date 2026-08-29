package com.mg.nmlonline.domain.model.vehicle;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VehicleType {

    TOURELLE("Véhicule à tourelle", 25, 40, 1300, 2, 1, 0, false, false),

    VTT_LEGER("VTT léger", 0, 50, 4000, 2, 10, 0, false, false),

    VTT_BLINDE("VTT blindé", 100, 150, 6500, 2, 10, 0, true, false),

    TANK("Tank de combat", 125, 250, 7500, 1, 0, 50, false, false),

    HELICOPTERE("Hélicoptère de combat", 250, 125, 9000, 2, 5, 0, true, true),

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