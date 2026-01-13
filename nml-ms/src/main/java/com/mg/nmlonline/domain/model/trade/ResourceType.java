package com.mg.nmlonline.domain.model.trade;

import lombok.Getter;

/**
 * Énumération des types de ressources avec leurs valeurs de vente.
 */
@Getter
public enum ResourceType {
    CIGARE("Cigare", 600),
    ALCOOL("Alcool", 800),
    ANTIQUITES("Antiquités", 1000),
    IVOIRE("Ivoire", 1100),
    URANIUM("Uranium", 1400),
    OR("Or", 1700),
    JOYAUX("Joyaux", 2000);

    private final String name;
    private final int baseValue; // Valeur de vente

    private static final double[] SALE_MULTIPLIERS = {
        1.0,    // 1 unité  = x1
        3.0,    // 2 unités = x3
        6.0,    // 3 unités = x6
        9.0,    // 4 unités = x9
        13.0,   // 5 unités = x13
        19.5,   // 6 unités = x19.5
        24.5,   // 7 unités = x24.5
        33.0,   // 8 unités = x33
        45.0    // 9 unités = x45
    };

    ResourceType(String name, int baseValue) {
        this.name = name;
        this.baseValue = baseValue;
    }

    /**
     * Calcule le prix de vente total pour un nombre donné d'unités.
     *
     * @param quantity Nombre d'unités à vendre (1-9)
     * @return Prix total de vente
     */
    public int calculateSalePrice(int quantity) {
        if (quantity <= 0) return 0;
        if (quantity > SALE_MULTIPLIERS.length) {
            quantity = SALE_MULTIPLIERS.length; // Cap à 9 unités maximum
        }

        double multiplier = SALE_MULTIPLIERS[quantity - 1];
        return (int) (baseValue * multiplier);
    }

    /**
     * Retourne le coefficient multiplicateur pour une quantité donnée.
     *
     * @param quantity Nombre d'unités (1-9)
     * @return Le coefficient multiplicateur
     */
    public static double getMultiplier(int quantity) {
        if (quantity <= 0) return 0;
        if (quantity > SALE_MULTIPLIERS.length) {
            return SALE_MULTIPLIERS[SALE_MULTIPLIERS.length - 1];
        }
        return SALE_MULTIPLIERS[quantity - 1];
    }

    @Override
    public String toString() {
        return String.format("%s (%d$ l'unité)", name, baseValue);
    }
}

