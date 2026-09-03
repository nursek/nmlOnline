package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BuildingDto;
import com.mg.nmlonline.domain.model.building.*;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.mapper.BuildingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BuildingService {

    private static final double HQ_RECONSTRUCTION_SAME_LOCATION_COST = 75000;

    private final BuildingRepository buildingRepository;
    private final PlayerRepository playerRepository;
    private final BoardService boardService;
    private final TurnService turnService;
    private final BuildingMapper buildingMapper;

    public BuildingService(BuildingRepository buildingRepository,
                           PlayerRepository playerRepository,
                           BoardService boardService,
                           TurnService turnService,
                           BuildingMapper buildingMapper) {
        this.buildingRepository = buildingRepository;
        this.playerRepository = playerRepository;
        this.boardService = boardService;
        this.turnService = turnService;
        this.buildingMapper = buildingMapper;
    }

    public Optional<Building> findById(Long buildingId) {
        return buildingRepository.findById(buildingId);
    }

    /** Crée QG + Cache + Banque ; maintient la cohérence bidirectionnelle Player <-> Building. */
    public void createInitialBuildings(Player player) {
        Headquarters headquarters = new Headquarters(player.getId());
        headquarters.setPlayer(player);
        player.getBuildings().add(headquarters);
        buildingRepository.save(headquarters);
        WeaponCache weaponCache = new WeaponCache(player.getId());
        weaponCache.setPlayer(player);
        player.getBuildings().add(weaponCache);
        buildingRepository.save(weaponCache);
        Bank bank = new Bank(player.getId());
        bank.setPlayer(player);
        player.getBuildings().add(bank);
        buildingRepository.save(bank);
    }

    public Optional<Headquarters> getHeadquarters(Long playerId) {
        return buildingRepository.findByPlayerIdAndBuildingType(playerId, BuildingType.HEADQUARTERS)
                .filter(Headquarters.class::isInstance)
                .map(b -> (Headquarters) b);
    }

    /** Filtre en Java (et non JPQL) pour éviter des requêtes fragiles. */
    public boolean hasOperationalHeadquarters(Long playerId) {
        return getHeadquarters(playerId)
                .map(Headquarters::isOperational)
                .orElse(false);
    }

    public boolean reconstructHeadquartersSameLocation(Long playerId) {
        Optional<Headquarters> hqOpt = getHeadquarters(playerId);
        if (hqOpt.isEmpty()) return false;

        Headquarters hq = hqOpt.get();
        Player player = playerRepository.findById(playerId).orElse(null);
        if (player == null) return false;

        double cost = HQ_RECONSTRUCTION_SAME_LOCATION_COST;
        if (player.getStats().getMoney() < cost) {
            return false;
        }

        hq.reconstructSameLocation();
        player.getStats().setMoney(player.getStats().getMoney() - cost);
        playerRepository.save(player);
        buildingRepository.save(hq);
        return true;
    }

    /** Capture du QG - entraîne la défaite du joueur. */
    public void captureHeadquarters(Long victimPlayerId, Long capturingPlayerId, int currentTurn) {
        Optional<Headquarters> hqOpt = getHeadquarters(victimPlayerId);
        if (hqOpt.isEmpty()) return;

        Headquarters hq = hqOpt.get();
        hq.onCapture(capturingPlayerId, currentTurn);
        buildingRepository.save(hq);

        List<Building> victimBuildings = buildingRepository.findByPlayerIdAndIsDestroyedFalse(victimPlayerId);
        if (!victimBuildings.isEmpty()) {
            for (Building building : victimBuildings) {
                building.onCapture(capturingPlayerId, currentTurn);
            }
            buildingRepository.saveAll(victimBuildings);
        }
        // TODO implémenter une meilleure logique de transfert des ressources et équipements lors de la capture du QG. Si le joueur récupère le quartier, il récupère l'ensemble des territoires du joueur.
    }

    public List<WeaponCache> getWeaponCaches(Long playerId) {
        return buildingRepository.findByPlayerIdAndBuildingTypeAndIsDestroyedFalse(playerId, BuildingType.WEAPON_CACHE).stream()
                .filter(WeaponCache.class::isInstance)
                .map(b -> (WeaponCache) b)
                .toList();
    }

    /** Capture d'un cache d'armes : les équipements stockés rejoignent l'inventaire du capturant. */
    public void captureWeaponCache(Long cacheId, Long capturingPlayerId, int currentTurn) {
        Building building = buildingRepository.findById(cacheId).orElse(null);
        if (!(building instanceof WeaponCache cache)) {
            return;
        }

        // Valider le joueur capturant AVANT de muter le cache.
        Player capturingPlayer = playerRepository.findById(capturingPlayerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Le joueur capturant avec l'ID " + capturingPlayerId + " n'existe pas"));

        cache.onCapture(capturingPlayerId, currentTurn);
        // orphanRemoval : un stack sorti du Cache est DELETE au flush, d'où la recréation chez le capturant.
        for (EquipmentStack stack : cache.transferAllEquipments()) {
            capturingPlayer.addEquipmentToStack(stack.getEquipment(), stack.getQuantity());
        }
        buildingRepository.save(cache);
        playerRepository.save(capturingPlayer);
    }

    public Optional<Bank> getBank(Long playerId) {
        return buildingRepository.findByPlayerIdAndBuildingType(playerId, BuildingType.BANK)
                .filter(Bank.class::isInstance)
                .map(b -> (Bank) b);
    }

    /** Capture d'une banque : transfert argent + ressources, active la vampirisation des revenus. */
    public CaptureResult captureBank(Long bankId, Long capturingPlayerId, int currentTurn) {
        Building building = buildingRepository.findById(bankId).orElse(null);
        if (!(building instanceof Bank bank)) {
            return new CaptureResult(0, List.of());
        }

        // Valider le joueur capturant AVANT de muter la banque.
        Player capturingPlayer = playerRepository.findById(capturingPlayerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Le joueur capturant avec l'ID " + capturingPlayerId + " n'existe pas"));

        bank.onCapture(capturingPlayerId, currentTurn);
        double transferredMoney = bank.transferMoney();
        List<PlayerResource> transferredResources = bank.transferResources();
        buildingRepository.save(bank);

        capturingPlayer.incrementMoney(transferredMoney);

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

    public double calculateVampirizedIncome(Long playerId, double income, int currentTurn) {
        Optional<Bank> bankOpt = getBank(playerId);
        if (bankOpt.isEmpty() || !bankOpt.get().isCaptured()) {
            return 0;
        }

        return bankOpt.get().calculateVampirizedAmount(income, currentTurn);
    }

    public boolean moveBuilding(Long buildingId, Long boardId, int newSectorNumber, int currentTurn) {
        Building building = buildingRepository.findById(buildingId).orElse(null);
        if (building == null) {
            throw new IllegalArgumentException("Bâtiment introuvable : " + buildingId);
        }
        if (!building.canMove(currentTurn)) {
            throw new IllegalStateException("Le bâtiment ne peut pas se déplacer ce tour-ci");
        }

        Sector targetSector = boardService.getSectorFromBoard(boardId, newSectorNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Secteur " + newSectorNumber + " introuvable sur la board " + boardId));

        if (!Long.valueOf(building.getPlayerId()).equals(targetSector.getOwnerId())) {
            throw new IllegalStateException("Le secteur cible n'appartient pas au propriétaire du bâtiment");
        }

        building.setSector(targetSector);
        building.recordMove(currentTurn);
        buildingRepository.save(building);
        return true;
    }

    // Mapping dans la transaction (relations LAZY : sector, storedEquipments, storedResources).

    public Optional<BuildingDto> getHeadquartersDto(Long playerId) {
        return getHeadquarters(playerId).map(buildingMapper::toDto);
    }

    public Optional<BuildingDto> getBankDto(Long playerId) {
        return getBank(playerId).map(buildingMapper::toDto);
    }

    public List<BuildingDto> getWeaponCachesDto(Long playerId) {
        return getWeaponCaches(playerId).stream().map(buildingMapper::toDto).toList();
    }

    /** playerId ignoré (tour global) — conservé pour compatibilité. Source unique : TurnService. */
    public int getCurrentTurn(Long playerId) {
        return turnService.getCurrentTurn();
    }

    public record CaptureResult(double money, List<PlayerResource> resources) {}
}

