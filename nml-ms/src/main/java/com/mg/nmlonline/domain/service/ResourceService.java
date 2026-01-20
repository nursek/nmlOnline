package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.Resource;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * Multiplicateurs de vente basés sur la quantité vendue
     * 1 unité = x1, 2 unités = x3, 3 unités = x6, etc.
     */
    private static final double[] SALE_MULTIPLIERS = {
        1.0,    // 1 unité = x1
        3.0,    // 2 unités = x3
        6.0,    // 3 unités = x6
        9.0,    // 4 unités = x9
        13.0,   // 5 unités = x13
        19.5,   // 6 unités = x19.5
        24.5,   // 7 unités = x24.5
        33.0,   // 8 unités = x33
        45.0    // 9 unités = x45
    };

    public double getBaseValue(String resourceName) {
        return resourceRepository.findByName(resourceName)
                .map(Resource::getBaseValue)
                .orElseThrow(() -> new IllegalArgumentException("Ressource inconnue: " + resourceName));
    }

    /**
     * Calcule le multiplicateur de vente basé sur la quantité
     * Au-delà de 9 unités, impossible d'atteindre un multiplicateur plus élevé
     */
    private double getMultiplier(int quantity) {
        if (quantity <= 0) return 0.0;
        if (quantity >= SALE_MULTIPLIERS.length) {
            return SALE_MULTIPLIERS[SALE_MULTIPLIERS.length - 1];
        }
        return SALE_MULTIPLIERS[quantity - 1];
    }

    public double calculateSaleValue(String resourceName, int quantity) {
        return getBaseValue(resourceName) * getMultiplier(quantity);
    }

    public double sellResourceWithMultiplier(Player player, String resourceName, int quantity) {
        if (player == null || resourceName == null || quantity <= 0) {
            return 0.0;
        }

        // Vérifier que le joueur a suffisamment de ressources
        if (!player.hasResource(resourceName, quantity)) {
            return 0.0;
        }

        double saleValue = calculateSaleValue(resourceName, quantity);

        // Retirer la ressource et ajouter l'argent
        boolean removed = player.removeResource(resourceName, quantity);
        if (removed) {
            player.getStats().setMoney(player.getStats().getMoney() + saleValue);
            return saleValue;
        }

        return 0.0;
    }

    public boolean transferResource(Player fromPlayer, Player toPlayer,
                                     String resourceName, int quantity) {
        if (fromPlayer == null || toPlayer == null || resourceName == null || quantity <= 0) {
            return false;
        }

        // Vérifier que le joueur source a suffisamment de ressources
        if (!fromPlayer.hasResource(resourceName, quantity)) {
            return false;
        }

        // Effectuer le transfert
        boolean removed = fromPlayer.removeResource(resourceName, quantity);
        if (removed) {
            toPlayer.addResource(resourceName, quantity);
            return true;
        }

        return false;
    }


    public void collectSectorResource(Player player, String resourceName, int quantity) {
        if (player != null && resourceName != null && quantity > 0) {
            player.addResource(resourceName, quantity);
        }
        // Ajouter un nouvel attribut au secteur, indiquant que la ressource a été collectée.
    }

    public double calculateResourceValue(String resourceName, int quantity) {
        return quantity * getBaseValue(resourceName); // Valeur totale sans multiplicateur
    }

}
