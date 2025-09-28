package com.mg.nmlonline.model.unit;

import lombok.Getter;

// Énumération pour les classes de spécialisation
@Getter
public enum UnitClass {
    LEGER("L") {},

    ELEMENTAIRE("E") {},

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

    PILOTE_DESTRUCTEUR("P") {},

    SNIPER("S") {},

    BLESSE("X") {
        @Override
        public double getStatMultiplier() { return 0.5; } // Stats/2
    };

    private final String code;

    /**
     * Retourne la classe d'unité correspondant au code fourni.
     *
     * @param code Le code de la classe d'unité (L, T, M, P, S).
     * @return La classe d'unité correspondante.
     * @throws IllegalArgumentException Si le code ne correspond à aucune classe connue.
     */
    public static UnitClass fromCode(String code) {
        for (UnitClass uc : values()) {
            if (uc.getCode().equalsIgnoreCase(code.trim())) {
                return uc;
            }
        }
        throw new IllegalArgumentException("Classe inconnue : " + code);
    }

    UnitClass(String code) { 
        this.code = code;
    }

    // Méthodes par défaut
    public double getDamageReduction() { return 1.0; }
    public double getCriticalChance() { return 0.0; }
    public double getCriticalMultiplier() { return 1.0; }
    public double getStatMultiplier() { return 1.0; }
}