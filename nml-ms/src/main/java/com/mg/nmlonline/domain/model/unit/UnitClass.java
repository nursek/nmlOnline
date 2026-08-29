package com.mg.nmlonline.domain.model.unit;

import lombok.Getter;

@Getter
public enum UnitClass {

    /** Unité légère — peut se déplacer de 2 secteurs par tour. */
    LEGER("L") {
        @Override
        public int getMaxMovementHops() {
            return 2;
        }
    },

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

    SNIPER("S") {};

    private final String code;

    UnitClass(String code) {
        this.code = code;
    }

    public double getDamageReduction(String damageType) {
        return 0;
    }

    public double getCriticalChance() {
        return 0.0;
    }

    public double getCriticalMultiplier() {
        return 1.0;
    }

    /** Nombre maximum de secteurs parcourus par tour. Par défaut : 1. */
    public int getMaxMovementHops() {
        return 1;
    }

}