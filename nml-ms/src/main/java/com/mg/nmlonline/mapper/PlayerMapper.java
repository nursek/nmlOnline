package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.*;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.player.PlayerStats;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * Mapper simplifié pour Player - conversion uniquement entre Domain et DTO
 * Les entités Entity ont été fusionnées avec les classes du domaine
 */
@Component
public class PlayerMapper {

    private final EquipmentMapper equipmentMapper;

    public PlayerMapper(EquipmentMapper equipmentMapper) {
        this.equipmentMapper = equipmentMapper;
    }

    /**
     * Convertit un DTO PlayerDto en objet Player du domaine
     */
    public Player toDomain(PlayerDto dto) {
        if (dto == null) return new Player();

        Player player = new Player(dto.getName());
        player.setId(dto.getId());

        // Conversion des stats
        if (dto.getStats() != null) {
            PlayerStats stats = new PlayerStats();
            PlayerStatsDto statsDto = dto.getStats();
            stats.setMoney(statsDto.getMoney());
            stats.setTotalIncome(statsDto.getTotalIncome());
            stats.setTotalVehiclesValue(statsDto.getTotalVehiclesValue());
            stats.setTotalEquipmentValue(statsDto.getTotalEquipmentValue());
            stats.setTotalOffensivePower(statsDto.getTotalOffensivePower());
            stats.setTotalDefensivePower(statsDto.getTotalDefensivePower());
            stats.setGlobalPower(statsDto.getGlobalPower());
            stats.setTotalEconomyPower(statsDto.getTotalEconomyPower());
            stats.setTotalAtk(statsDto.getTotalAtk());
            stats.setTotalPdf(statsDto.getTotalPdf());
            stats.setTotalPdc(statsDto.getTotalPdc());
            stats.setTotalDef(statsDto.getTotalDef());
            stats.setTotalArmor(statsDto.getTotalArmor());
            player.setStats(stats);
        }

        // Conversion des équipements
        if (dto.getEquipments() != null) {
            List<EquipmentStack> equipmentStacks = dto.getEquipments().stream()
                    .map(this::equipmentStackFromDto)
                    .toList();
            player.setEquipments(equipmentStacks);
        }

        // Conversion des IDs de secteurs
        if (dto.getOwnedSectorIds() != null) {
            player.setOwnedSectorIds(new HashSet<>(dto.getOwnedSectorIds()));
        }

        return player;
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
            dto.setStats(statsDto);
        }

        // Conversion des équipements
        if (player.getEquipments() != null) {
            List<EquipmentStackDto> equipmentDtos = player.getEquipments().stream()
                    .map(this::equipmentStackToDto)
                    .toList();
            dto.setEquipments(equipmentDtos);
        }

        // Conversion des IDs de secteurs
        if (player.getOwnedSectorIds() != null) {
            dto.setOwnedSectorIds(new HashSet<>(player.getOwnedSectorIds()));
        }

        return dto;
    }

    // === Méthodes utilitaires pour EquipmentStack ===

    private EquipmentStack equipmentStackFromDto(EquipmentStackDto dto) {
        if (dto == null || dto.getEquipment() == null) return null;

        Equipment equipment = equipmentMapper.toDomain(dto.getEquipment());
        EquipmentStack stack = new EquipmentStack(equipment);
        stack.setQuantity(dto.getQuantity());
        stack.setAvailable(dto.getAvailable());
        return stack;
    }

    private EquipmentStackDto equipmentStackToDto(EquipmentStack stack) {
        if (stack == null || stack.getEquipment() == null) return null;

        EquipmentStackDto dto = new EquipmentStackDto();
        dto.setEquipment(equipmentMapper.toDto(stack.getEquipment()));
        dto.setQuantity(stack.getQuantity());
        dto.setAvailable(stack.getAvailable());
        return dto;
    }
}
