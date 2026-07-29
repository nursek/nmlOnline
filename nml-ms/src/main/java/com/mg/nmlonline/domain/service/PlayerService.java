package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BuyEquipmentItemDto;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final SectorService sectorService;
    private final EquipmentService equipmentService;

    public PlayerService(PlayerRepository playerRepository,
                          SectorService sectorService,
                          EquipmentService equipmentService) {
        this.playerRepository = playerRepository;
        this.sectorService = sectorService;
        this.equipmentService = equipmentService;
    }

    // --- Lecture ---
    public Page<Player> findAll(Pageable pageable) {
        return playerRepository.findAllByOrderByNameAsc(pageable);
    }

    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id);
    }

    /**
     * Find a player by name.
     * @param name the player name to search for
     * @return the Player if found, null otherwise
     */
    public Player findByName(String name) {
        return playerRepository.findByName(name).orElse(null);
    }

    public Player findByUserId(Long userId) {
        return playerRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public Player save(Player player) {
        return playerRepository.save(player);
    }

    /**
     * Achète une liste d'équipements de manière atomique pour le joueur.
     * Valide d'abord tous les items (existence, quantité positive, fonds suffisants),
     * puis applique les modifications. En cas d'erreur, la transaction est rollbacke.
     *
     * @param playerId  l'identifiant du joueur
     * @param items     la liste des équipements à acheter
     * @return le joueur mis à jour
     * @throws IllegalArgumentException si un item est invalide ou les fonds sont insuffisants
     */
    @Transactional
    public Player buyEquipments(Long playerId, List<BuyEquipmentItemDto> items) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player ID is required");
        }
        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No items to buy");
        }

        // --- Phase 1 : validation et calcul du coût total ---
        record ResolvedItem(Equipment equipment, int quantity) {}
        List<ResolvedItem> resolvedItems = new ArrayList<>();
        double totalCost = 0;

        for (BuyEquipmentItemDto item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Cart contains a null item");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0 for: " + item.getName());
            }
            Equipment equipment = equipmentService.findByName(item.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found: " + item.getName()));
            totalCost += (double) equipment.getCost() * item.getQuantity();
            resolvedItems.add(new ResolvedItem(equipment, item.getQuantity()));
        }

        if (player.getStats().getMoney() < totalCost) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        // --- Phase 2 : application atomique ---
        for (ResolvedItem resolved : resolvedItems) {
            boolean success = player.buyEquipment(resolved.equipment(), resolved.quantity());
            if (!success) {
                throw new IllegalStateException("Failed to apply purchase for: " + resolved.equipment().getName());
            }
        }

        return playerRepository.save(player);
    }

    @Transactional
    public boolean delete(Long id) {
        if (!playerRepository.existsById(id)) return false;
        // D'abord nettoyer les secteurs (réinitialiser ownership, supprimer armées)
        sectorService.removePlayerFromSectors(id);
        // Ensuite supprimer le joueur (cascade vers EquipmentStack, PlayerResource)
        playerRepository.deleteById(id);
        return true;
    }
}
