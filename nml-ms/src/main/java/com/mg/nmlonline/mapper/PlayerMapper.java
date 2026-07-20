package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.*;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.player.PlayerStats;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.service.ResourceService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper simplifié pour Player - conversion uniquement entre Domain et DTO
 * Les entités Entity ont été fusionnées avec les classes du domaine
 */
@Component
public class PlayerMapper {

    private final EquipmentMapper equipmentMapper;
    private final ResourceService resourceService;
    private final GameCharacterMapper gameCharacterMapper;
    private final BuildingMapper buildingMapper;
    private final SectorMapper sectorMapper;

    public PlayerMapper(EquipmentMapper equipmentMapper, ResourceService resourceService,
                        GameCharacterMapper gameCharacterMapper, BuildingMapper buildingMapper,
                        SectorMapper sectorMapper) {
        this.equipmentMapper = equipmentMapper;
        this.resourceService = resourceService;
        this.gameCharacterMapper = gameCharacterMapper;
        this.buildingMapper = buildingMapper;
        this.sectorMapper = sectorMapper;
    }

    /**
     * Convertit un objet Player du domaine en DTO PlayerDto
     */
    public PlayerDto toDto(Player player) {
        if (player == null) return null;

        PlayerDto dto = new PlayerDto();
        dto.setId(player.getId());
        dto.setName(player.getName());

        // Conversion des stats
        if (player.getStats() != null) {
            PlayerStatsDto statsDto = getPlayerStatsDto(player);
            dto.setStats(statsDto);
        }

        // Conversion des équipements
        if (player.getEquipments() != null) {
            List<EquipmentStackDto> equipmentDtos = player.getEquipments().stream()
                    .map(this::equipmentStackToDto)
                    .toList();
            dto.setEquipments(equipmentDtos);
        }

        // Conversion des ressources
        if (player.getResources() != null) {
            List<PlayerResourceDto> resourceDtos = player.getResources().stream()
                    .map(this::playerResourceToDto)
                    .toList();
            dto.setResources(resourceDtos);
        }

        // Conversion du personnage principal
        if (player.getCharacter() != null) {
            dto.setCharacter(gameCharacterMapper.toDto(player.getCharacter()));
        }

        // Conversion des bâtiments
        if (player.getBuildings() != null) {
            List<BuildingDto> buildingDtos = player.getBuildings().stream()
                    .map(buildingMapper::toDto)
                    .toList();
            dto.setBuildings(buildingDtos);
        }

        return dto;
    }

    /**
     * Convertit un Player en DTO enrichi avec les secteurs complets depuis la board fournie.
     * Note : utilise la première board disponible (le jeu ne supporte qu'une board active).
     */
    public PlayerDto toDtoWithSectors(Player player, Board board) {
        if (player == null) {
            return null;
        }

        PlayerDto dto = toDto(player);

        // Enrichir avec les secteurs du joueur
        List<SectorDto> playerSectors = new ArrayList<>();
        if (board != null && player.getId() != null) {
            for (Sector sector : board.getAllSectors()) {
                if (player.getId().equals(sector.getOwnerId())) {
                    playerSectors.add(sectorMapper.toDto(sector));
                }
            }
        }
        dto.setSectors(playerSectors);

        return dto;
    }

    private static PlayerStatsDto getPlayerStatsDto(Player player) {
        PlayerStatsDto statsDto = new PlayerStatsDto();
        PlayerStats stats = player.getStats();
        statsDto.setMoney(stats.getMoney());
        statsDto.setTotalIncome(stats.getTotalIncome());
        statsDto.setTotalVehiclesValue(stats.getTotalVehiclesValue());
        statsDto.setTotalEquipmentValue(stats.getTotalEquipmentValue());
        statsDto.setTotalOffensivePower(stats.getTotalOffensivePower());
        statsDto.setTotalDefensivePower(stats.getTotalDefensivePower());
        statsDto.setGlobalPower(stats.getGlobalPower());
        statsDto.setTotalEconomyPower(stats.getTotalEconomyPower());
        statsDto.setTotalAtk(stats.getTotalAtk());
        statsDto.setTotalPdf(stats.getTotalPdf());
        statsDto.setTotalPdc(stats.getTotalPdc());
        statsDto.setTotalDef(stats.getTotalDef());
        statsDto.setTotalArmor(stats.getTotalArmor());
        return statsDto;
    }

    // === Méthodes utilitaires pour EquipmentStack ===

    private EquipmentStackDto equipmentStackToDto(EquipmentStack stack) {
        if (stack == null || stack.getEquipment() == null) return null;

        EquipmentStackDto dto = new EquipmentStackDto();
        dto.setEquipment(equipmentMapper.toDto(stack.getEquipment()));
        dto.setQuantity(stack.getQuantity());
        dto.setAvailable(stack.getAvailable());
        return dto;
    }

    // === Méthodes utilitaires pour PlayerResource ===

    private PlayerResourceDto playerResourceToDto(PlayerResource resource) {
        if (resource == null) return null;

        PlayerResourceDto dto = new PlayerResourceDto();
        dto.setId(resource.getId());
        dto.setName(resource.getResourceName());
        dto.setQuantity(resource.getQuantity());

        // Enrichir avec le prix de base depuis Resource
        try {
            double baseValue = resourceService.getBaseValue(resource.getResourceName());
            dto.setBaseValue(baseValue);
        } catch (IllegalArgumentException e) {
            // Ressource inconnue, laisser baseValue à null
            dto.setBaseValue(null);
        }

        return dto;
    }
}
