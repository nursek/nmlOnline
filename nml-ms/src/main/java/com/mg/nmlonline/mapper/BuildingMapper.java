package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.BuildingDto;
import com.mg.nmlonline.api.dto.EquipmentStackDto;
import com.mg.nmlonline.api.dto.PlayerResourceDto;
import com.mg.nmlonline.domain.model.building.*;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.service.TurnService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper pour Building et ses sous-classes - conversion entre Domain et DTO.
 */
@Component
public class BuildingMapper {

    private final EquipmentMapper equipmentMapper;
    private final TurnService turnService;

    public BuildingMapper(EquipmentMapper equipmentMapper, TurnService turnService) {
        this.equipmentMapper = equipmentMapper;
        this.turnService = turnService;
    }

    /**
     * Convertit un Building du domaine en DTO.
     */
    public BuildingDto toDto(Building building) {
        if (building == null) return null;

        BuildingDto dto = new BuildingDto();
        dto.setId(building.getId());
        dto.setPlayerId(building.getPlayerId());
        dto.setBuildingType(building.getBuildingType().name());
        dto.setDisplayName(building.getDisplayName());

        // Stats de combat
        dto.setAttack(building.getAttack());
        dto.setDefense(building.getDefense());

        // État général
        dto.setIsDestroyed(building.isDestroyed());
        dto.setIsCaptured(building.isCaptured());
        dto.setCapturedByPlayerId(building.getCapturedByPlayerId());
        dto.setCapturedTurn(building.getCapturedTurn());

        // Déplacement
        dto.setLastMovedTurn(building.getLastMovedTurn());
        dto.setMoveCooldown(building.getMoveCooldown());

        // Localisation
        if (building.getSector() != null) {
            dto.setSectorNumber(building.getSector().getNumber());
        }

        // Conversion spécifique selon le type
        if (building instanceof Headquarters hq) {
            mapHeadquartersToDto(hq, dto);
        } else if (building instanceof WeaponCache cache) {
            mapWeaponCacheToDto(cache, dto);
        } else if (building instanceof Bank bank) {
            mapBankToDto(bank, dto);
        }

        return dto;
    }

    private void mapHeadquartersToDto(Headquarters hq, BuildingDto dto) {
        dto.setIsOperational(hq.isOperational());
        dto.setCanMove(hq.canMove(getCurrentTurn()));
    }

    private void mapWeaponCacheToDto(WeaponCache cache, BuildingDto dto) {
        dto.setMaxCapacity(cache.getMaxCapacity());
        dto.setCurrentCapacity(cache.getTotalStoredCount());
        dto.setAvailableCapacity(cache.getAvailableCapacity());
        dto.setFillPercentage(cache.getFillPercentage());
        dto.setCanMove(cache.canMove(getCurrentTurn()));

        // Convertir les équipements stockés
        if (cache.getStoredEquipments() != null) {
            List<EquipmentStackDto> equipmentDtos = cache.getStoredEquipments().stream()
                    .map(this::toEquipmentStackDto)
                    .collect(Collectors.toList());
            dto.setStoredEquipments(equipmentDtos);
        }
    }

    private void mapBankToDto(Bank bank, BuildingDto dto) {
        dto.setHasMoved(bank.isHasMoved());
        dto.setStoredMoney(bank.getStoredMoney());
        dto.setCanMove(bank.canMove(getCurrentTurn()));
        dto.setCurrentVampirizeRate(bank.getVampirizeRate(getCurrentTurn()));

        // Convertir les ressources stockées
        if (bank.getStoredResources() != null) {
            List<PlayerResourceDto> resourceDtos = bank.getStoredResources().stream()
                    .map(this::toPlayerResourceDto)
                    .collect(Collectors.toList());
            dto.setStoredResources(resourceDtos);
        }
    }

    // === Helpers ===

    private EquipmentStackDto toEquipmentStackDto(EquipmentStack stack) {
        if (stack == null) return null;
        EquipmentStackDto dto = new EquipmentStackDto();
        dto.setEquipment(equipmentMapper.toDto(stack.getEquipment()));
        dto.setQuantity(stack.getQuantity());
        dto.setAvailable(stack.getAvailable());
        return dto;
    }

    private PlayerResourceDto toPlayerResourceDto(PlayerResource resource) {
        if (resource == null) return null;
        PlayerResourceDto dto = new PlayerResourceDto();
        // resourceName (colonne non null) plutôt que resource.getResource().getName() :
        // évite un SELECT lazy par ressource et une NPE si la relation est absente.
        dto.setName(resource.getResourceName());
        dto.setQuantity(resource.getQuantity());
        return dto;
    }

    /**
     * Tour courant — source unique de vérité via {@link TurnService}.
     */
    private int getCurrentTurn() {
        return turnService.getCurrentTurn();
    }
}

