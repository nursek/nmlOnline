package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.building.*;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des bâtiments (QG, Cache d'armes, Banque).
 */
@Service
@Transactional
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final PlayerRepository playerRepository;

    public BuildingService(BuildingRepository buildingRepository, PlayerRepository playerRepository) {
        this.buildingRepository = buildingRepository;
        this.playerRepository = playerRepository;
    }

    // === CRÉATION DES BÂTIMENTS INITIAUX ===

    /**
     * Récupère un bâtiment par son ID.
     */
    public Optional<Building> findById(Long buildingId) {
        return buildingRepository.findById(buildingId);
    }

    /**
     * Crée les bâtiments de départ pour un nouveau joueur.
     * Maintient la cohérence bidirectionnelle de la relation Player <-> Building.
     */
    public void createInitialBuildings(Player player) {
        // Création du QG
        Headquarters headquarters = new Headquarters(player.getId());
        headquarters.setPlayer(player);
        player.getBuildings().add(headquarters);
        buildingRepository.save(headquarters);

        // Création de la Cache d'armes
        WeaponCache weaponCache = new WeaponCache(player.getId());
        weaponCache.setPlayer(player);
        player.getBuildings().add(weaponCache);
        buildingRepository.save(weaponCache);

        // Création de la Banque
        Bank bank = new Bank(player.getId());
        bank.setPlayer(player);
        player.getBuildings().add(bank);
        buildingRepository.save(bank);
    }

    // === GESTION DU QG ===

    /**
     * Récupère le QG d'un joueur.
     */
    public Optional<Headquarters> getHeadquarters(Long playerId) {
        return buildingRepository.findByPlayerIdAndBuildingType(playerId, BuildingType.HEADQUARTERS)
                .filter(Headquarters.class::isInstance)
                .map(b -> (Headquarters) b);
    }

    /**
     * Vérifie si un joueur a un QG opérationnel.
     * La logique métier est filtrée en Java pour éviter les requêtes JPQL fragiles.
     */
    public boolean hasOperationalHeadquarters(Long playerId) {
        return getHeadquarters(playerId)
                .map(Headquarters::isOperational)
                .orElse(false);
    }

    /**
     * Reconstruit le QG sur place.
     */
    public boolean reconstructHeadquartersSameLocation(Long playerId) {
        Optional<Headquarters> hqOpt = getHeadquarters(playerId);
        if (hqOpt.isEmpty()) return false;

        Headquarters hq = hqOpt.get();
        Player player = playerRepository.findById(playerId).orElse(null);
        if (player == null) return false;

        if (hq.reconstructSameLocation(player.getStats().getMoney())) {
            player.getStats().setMoney(player.getStats().getMoney() - Headquarters.RECONSTRUCTION_SAME_LOCATION_COST);
            playerRepository.save(player);
            buildingRepository.save(hq);
            return true;
        }
        return false;
    }

    /**
     * Capture du QG - entraîne la défaite du joueur.
     */
    public void captureHeadquarters(Long victimPlayerId, Long capturingPlayerId, int currentTurn) {
        Optional<Headquarters> hqOpt = getHeadquarters(victimPlayerId);
        if (hqOpt.isEmpty()) return;

        Headquarters hq = hqOpt.get();
        hq.onCapture(capturingPlayerId, currentTurn);
        buildingRepository.save(hq);

        // Transférer tous les autres bâtiments au conquérant
        List<Building> victimBuildings = buildingRepository.findByPlayerIdAndIsDestroyedFalse(victimPlayerId);
        if (!victimBuildings.isEmpty()) {
            for (Building building : victimBuildings) {
                building.onCapture(capturingPlayerId, currentTurn);
            }
            buildingRepository.saveAll(victimBuildings);
        }
        // TODO implémenter une meilleure logique de transfert des ressources et équipements lors de la capture du QG. Si le joueur récupère le quartier, il récupère l'ensemble des territoires du joueur.
    }

    // === GESTION DE LA CACHE D'ARMES ===

    /**
     * Récupère les caches d'armes d'un joueur.
     */
    public List<WeaponCache> getWeaponCaches(Long playerId) {
        return buildingRepository.findByPlayerIdAndBuildingTypeAndIsDestroyedFalse(playerId, BuildingType.WEAPON_CACHE).stream()
                .filter(WeaponCache.class::isInstance)
                .map(b -> (WeaponCache) b)
                .toList();
    }

    /**
     * Capture d'une cache d'armes - transfert des équipements.
     */
    public List<EquipmentStack> captureWeaponCache(Long cacheId, Long capturingPlayerId, int currentTurn) {
        Building building = buildingRepository.findById(cacheId).orElse(null);
        if (!(building instanceof WeaponCache cache)) {
            return List.of();
        }

        cache.onCapture(capturingPlayerId, currentTurn);
        List<EquipmentStack> transferred = cache.transferAllEquipments();
        buildingRepository.save(cache);

        return transferred;
    }

    // === GESTION DE LA BANQUE ===

    /**
     * Récupère la banque d'un joueur.
     */
    public Optional<Bank> getBank(Long playerId) {
        return buildingRepository.findByPlayerIdAndBuildingType(playerId, BuildingType.BANK)
                .filter(Bank.class::isInstance)
                .map(b -> (Bank) b);
    }

    /**
     * Capture d'une banque - transfert d'argent et activation de la vampirisation.
     * @throws IllegalArgumentException si la banque ou le joueur capturant n'existe pas
     */
    public CaptureResult captureBank(Long bankId, Long capturingPlayerId, int currentTurn) {
        Building building = buildingRepository.findById(bankId).orElse(null);
        if (!(building instanceof Bank bank)) {
            return new CaptureResult(0, List.of());
        }

        // Vérifier que le joueur capturant existe AVANT de capturer la banque
        Player capturingPlayer = playerRepository.findById(capturingPlayerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Le joueur capturant avec l'ID " + capturingPlayerId + " n'existe pas"));

        bank.onCapture(capturingPlayerId, currentTurn);
        double transferredMoney = bank.transferMoney();
        List<PlayerResource> transferredResources = bank.transferResources();
        buildingRepository.save(bank);

        // Créditer le joueur avec l'argent transféré
        capturingPlayer.incrementMoney(transferredMoney);

        // Ajouter les ressources transférées à l'inventaire du joueur
        if (transferredResources != null && !transferredResources.isEmpty()) {
            List<PlayerResource> playerResources = capturingPlayer.getResources();
            if (playerResources == null) {
                playerResources = new ArrayList<>();
                capturingPlayer.setResources(playerResources);
            }

            for (PlayerResource resource : transferredResources) {
                resource.setPlayer(capturingPlayer);
            }
            playerResources.addAll(transferredResources);
        }

        playerRepository.save(capturingPlayer);
        return new CaptureResult(transferredMoney, transferredResources != null ? transferredResources : List.of());
    }

    /**
     * Calcule le montant vampirisé des revenus d'un joueur dont la banque est capturée.
     */
    public double calculateVampirizedIncome(Long playerId, double income, int currentTurn) {
        Optional<Bank> bankOpt = getBank(playerId);
        if (bankOpt.isEmpty() || !bankOpt.get().isCaptured()) {
            return 0;
        }

        return bankOpt.get().calculateVampirizedAmount(income, currentTurn);
    }

    // === DÉPLACEMENT DES BÂTIMENTS ===

    /**
     * Déplace un bâtiment vers un nouveau secteur.
     */
    public boolean moveBuilding(Long buildingId, int newSectorNumber, int currentTurn) {
        Building building = buildingRepository.findById(buildingId).orElse(null);
        if (building == null || !building.canMove(currentTurn)) {
            return false;
        }

        // La logique de changement de secteur sera implémentée ici
        building.recordMove(currentTurn);
        buildingRepository.save(building);
        return true;
    }

    // === CLASSES UTILITAIRES ===

    /**     * Retourne le tour courant du jeu.
     */
    public int getCurrentTurn(Long playerId) {
        return 1; // TODO implémenter la logique pour récupérer le tour courant du jeu à partir du contexte ou d'un service de gestion de partie
    }

    public record CaptureResult(double money, List<PlayerResource> resources) {}
}

