package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.Resource;
import com.mg.nmlonline.infrastructure.repository.ResourceTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service pour gérer les ressources des joueurs
 * Gère les conversions, ventes avec multiplicateurs, et récupération des prix depuis Resource
 */
@Service
public class ResourceService {

    private final ResourceTypeRepository resourceTypeRepository;

    @Autowired
    public ResourceService(ResourceTypeRepository resourceTypeRepository) {
        this.resourceTypeRepository = resourceTypeRepository;
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

    /**
     * Récupère le prix de base d'une ressource depuis la table RESOURCE_TYPE
     */
    public double getBaseValue(String resourceName) {
        return resourceTypeRepository.findByName(resourceName)
                .map(Resource::getBaseValue)
                .orElseThrow(() -> new IllegalArgumentException("Ressource inconnue: " + resourceName));
    }

    /**
     * Calcule le multiplicateur de vente basé sur la quantité
     * Over 9 units, can't reach higher multiplier
     */
    private double getMultiplier(int quantity) {
        if (quantity <= 0) return 0.0;
        if (quantity >= SALE_MULTIPLIERS.length) {
            return SALE_MULTIPLIERS[SALE_MULTIPLIERS.length - 1];
        }
        return SALE_MULTIPLIERS[quantity - 1];
    }

    /**
     * Calcule le prix de vente total avec multiplicateur
     * Prix = baseValue * multiplicateur (quantité)
     */
    public double calculateSaleValue(String resourceName, int quantity) {
        double baseValue = getBaseValue(resourceName);
        double multiplier = getMultiplier(quantity);
        return baseValue * multiplier;
    }

    /**
     * Vend une quantité de ressources avec multiplicateur et ajoute l'argent au joueur
     *
     * @param player Le joueur
     * @param resourceName Nom de la ressource à vendre
     * @param quantity Quantité à vendre
     * @return Le montant gagné, ou 0 si la vente a échoué
     */
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

    /**
     * Transfère des ressources d'un joueur à un autre
     *
     * @param fromPlayer Joueur source
     * @param toPlayer Joueur destinataire
     * @param resourceName Nom de la ressource
     * @param quantity Quantité à transférer
     * @return true si le transfert a réussi
     */
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

    /**
     * Récupère les ressources d'un secteur conquis ou lors du tour
     *
     * @param player Le joueur qui récupère
     * @param resourceName Nom de la ressource du secteur
     * @param quantity Quantité trouvée/générée dans le secteur
     */
    public void collectSectorResource(Player player, String resourceName, int quantity) {
        if (player != null && resourceName != null && quantity > 0) {
            player.addResource(resourceName, quantity);
        }
    }

    /**
     * Calcule la valeur totale d'une ressource possédée par un joueur
     * (sans multiplicateur, juste baseValue * quantity)
     */
    public double calculateResourceValue(String resourceName, int quantity) {
        double baseValue = getBaseValue(resourceName);
        return baseValue * quantity;
    }

}
