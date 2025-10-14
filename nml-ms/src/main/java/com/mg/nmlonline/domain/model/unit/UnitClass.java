package com.mg.nmlonline.domain.model.unit;

import lombok.Getter;

// Énumération pour les classes de spécialisation
@Getter
public enum UnitClass {
    LEGER("L") {},

    ELEMENTAIRE("E") {},

    TIREUR("T") {
        @Override
        public double getCriticalChance() {
            return 0.10;
        }

        @Override
        public double getCriticalMultiplier() {
            return 1.5;
        }
    },

    MASTODONTE("M") {
        @Override
        public double getDamageReduction(String damageType) {
            return switch (damageType) {
                case "PDF", "PDC" -> 0.25;
                default -> 0.0;
            };
        }
    },

    PILOTE_DESTRUCTEUR("P") {},

    SNIPER("S") {},

    BLESSE("X") {
        @Override
        public double getStatMultiplier() {
            return 0.5;
        } // Stats/2
    };

    private final String code;

    UnitClass(String code) {
        this.code = code;
    }

    // Méthodes par défaut
    public double getDamageReduction(String damageType) {
        return 0;
    }

    public double getCriticalChance() {
        return 0.0;
    }

    public double getCriticalMultiplier() {
        return 1.0;
    }

    public double getStatMultiplier() {
        return 1.0;
    }
}