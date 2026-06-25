package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.SellResourceBatchItemDto;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.resource.Resource;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerResourceRepository;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResourceService {

    private static final double[] SALE_MULTIPLIERS = {1.0, 3.0, 6.0, 9.0, 13.0, 19.5, 24.5, 33.0, 45.0};

    private final ResourceRepository resourceRepository;
    private final PlayerResourceRepository playerResourceRepository;
    private final PlayerRepository playerRepository;

    public ResourceService(ResourceRepository resourceRepository,
                           PlayerResourceRepository playerResourceRepository,
                           PlayerRepository playerRepository) {
        this.resourceRepository = resourceRepository;
        this.playerResourceRepository = playerResourceRepository;
        this.playerRepository = playerRepository;
    }

    public double getBaseValue(String resourceName) {
        return resourceRepository.findByName(resourceName)
                .map(Resource::getBaseValue)
                .orElseThrow(() -> new IllegalArgumentException("Ressource inconnue: " + resourceName));
    }

    /**
     * Calcule le multiplicateur de vente basé sur la quantité.
     * Au-delà de la dernière entrée configurée, le multiplicateur max est utilisé.
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
        // TODO: Ajouter un nouvel attribut au secteur, indiquant que la ressource a été collectée.
    }

    /**
     * Vend une ressource d'un joueur et ajoute l'argent correspondant
     * @param resourceId L'ID de la ressource à vendre (PlayerResource)
     * @param quantity La quantité à vendre
     * @return Les informations de la vente (nom, quantité, montant)
     * @throws IllegalArgumentException si la quantité est insuffisante
     * @throws RuntimeException si la ressource n'est pas trouvée
     */
    @Transactional
    public SaleResult sellResource(Long resourceId, int quantity, Long userId) {
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Joueur introuvable : " + userId));

        // Récupérer la ressource du joueur
        PlayerResource playerResource = playerResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        // Vérifier que la ressource appartient au joueur authentifié
        Player owner = playerResource.getPlayer();
        if (owner == null || !player.getId().equals(owner.getId())) {
            throw new SecurityException("Access denied: resource does not belong to authenticated user");
        }

        // Vérifier que le joueur a suffisamment de ressources
        if (playerResource.getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient quantity");
        }

        String resourceName = playerResource.getResourceName();

        // Calculer le prix de vente avec multiplicateur
        double sellPrice = calculateSaleValue(resourceName, quantity);

        // Ajouter l'argent au joueur (owner already validated above)
        owner.incrementMoney(sellPrice);


        // Retirer la quantité de ressources
        playerResource.removeQuantity(quantity);

        // Si la quantité atteint 0, supprimer l'entrée
        if (playerResource.getQuantity() == 0) {
            playerResourceRepository.delete(playerResource);
        } else {
            playerResourceRepository.save(playerResource);
        }

        // Sauvegarder le joueur
        playerRepository.save(owner);

        // Retourner les informations de la vente
        return new SaleResult(resourceName, quantity, sellPrice);
    }

    /**
     * Vend un lot de ressources de manière atomique pour le joueur authentifié.
     *
     * @param userId l'id de l'utilisateur authentifié
     * @param items  liste des lignes de vente (PlayerResource.id + quantité)
     * @return la liste des résultats de chaque ligne de vente
     */
    @Transactional
    public List<SaleResult> sellResourcesBatch(Long userId, List<SellResourceBatchItemDto> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Le panier de vente est vide");
        }

        List<SaleResult> results = new ArrayList<>();
        for (SellResourceBatchItemDto item : items) {
            results.add(sellResource(item.getPlayerResourceId(), item.getQuantity(), userId));
        }

        return results;
    }

    /**
     * Classe interne représentant le résultat d'une vente
     */
    public record SaleResult(
            String resourceName,
            int quantitySold,
            double saleValue) {
    }

}
