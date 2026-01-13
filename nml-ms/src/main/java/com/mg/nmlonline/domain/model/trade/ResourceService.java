package com.mg.nmlonline.domain.model.trade;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les ressources des joueurs.
 * Permet de vendre des ressources et calculer les profits.
 */
@Slf4j
public class ResourceService {

    /**
     * Calcule le prix de vente pour un lot de ressources du même type.
     *
     * @param type Type de ressource
     * @param quantity Quantité à vendre
     * @return Prix de vente total en dollars
     */
    public int calculateSalePrice(ResourceType type, int quantity) {
        if (type == null || quantity <= 0) {
            return 0;
        }

        int totalPrice = type.calculateSalePrice(quantity);

        log.info("Vente de {} unités de {} : {} $ (coefficient x{})",
                 quantity, type.getName(), totalPrice, ResourceType.getMultiplier(quantity));

        return totalPrice;
    }

    /**
     * Optimise une vente en déterminant les meilleures combinaisons de quantités.
     * Par exemple, vendre 10 unités en 9+1 peut être plus rentable que 10 unités directement.
     *
     * @param totalQuantity Quantité totale à vendre
     * @return Liste des quantités optimales à vendre séparément
     */
    public List<Integer> optimizeSale(int totalQuantity) {
        List<Integer> batches = new ArrayList<>();

        // Stratégie simple : vendre par lots de 9 (max coefficient) puis le reste
        while (totalQuantity > 0) {
            if (totalQuantity >= 9) {
                batches.add(9);
                totalQuantity -= 9;
            } else {
                batches.add(totalQuantity);
                totalQuantity = 0;
            }
        }

        log.info("Optimisation de vente : lots de {}", batches);
        return batches;
    }
}