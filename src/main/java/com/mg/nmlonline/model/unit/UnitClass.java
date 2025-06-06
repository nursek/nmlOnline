package com.mg.nmlonline.model.unit;

import lombok.Getter;

// Énumération pour les classes de spécialisation
@Getter
public enum UnitClass {
    LEGER("L") {
        @Override
        public boolean canDoubleMove() { return true; }

        @Override
        public double getDefenseReductionOnFirstSector() { return 0.5; }
    },

    TIREUR("T") {
        @Override
        public double getCriticalChance() { return 0.10; }

        @Override
        public double getCriticalMultiplier() { return 1.5; }
    },

    MASTODONTE("M") {
        @Override
        public double getDamageReduction() { return 0.75; }
    },

    PILOTE_DESTRUCTEUR("P") {
        @Override
        public boolean canControlVehicles() { return true; }

        @Override
        public boolean targetsVehiclesFirst() { return true; }
    },

    SNIPER("S") {
        @Override
        public boolean canSnipe() { return true; }

        @Override
        public double getEvasionChance(UnitType unitType) {
            return switch (unitType) {
                case LARBIN -> 0.20;
                case VOYOU -> 0.35;
                case MALFRAT -> 0.50;
                case BRUTE -> 0.65;
            };
        }
    };

    private final String code;

    UnitClass(String code) { 
        this.code = code;
    }

    // Méthodes par défaut
    public double getDamageReduction() { return 1.0; }
    public double getCriticalChance() { return 0.0; }
    public double getCriticalMultiplier() { return 1.0; }
    public boolean canDoubleMove() { return false; }
    public double getDefenseReductionOnFirstSector() { return 1.0; }
    public boolean canControlVehicles() { return false; }
    public boolean targetsVehiclesFirst() { return false; }
    public boolean canSnipe() { return false; }
    public double getEvasionChance(UnitType unitType) { return 0.0; }
}