package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.*;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.player.PlayerBonuses;
import com.mg.nmlonline.domain.model.player.PlayerStats;
import com.mg.nmlonline.infrastructure.entity.*;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
public class PlayerMapper {

    private final EquipmentMapper equipmentMapper;
    private final SectorMapper sectorMapper;
    private final EquipmentRepository equipmentRepository;

    public PlayerMapper(EquipmentMapper equipmentMapper, SectorMapper sectorMapper, EquipmentRepository equipmentRepository) {
        this.equipmentMapper = equipmentMapper;
        this.sectorMapper = sectorMapper;
        this.equipmentRepository = equipmentRepository;
    }

    /**
     * Convertit une entité PlayerEntity en objet Player du domaine
     */
    public Player toDomain(PlayerEntity entity) {
        if (entity == null) return new Player();

        Player player = new Player(entity.getName());

        // Conversion des stats
        if (entity.getStats() != null) {
            PlayerStats stats = new PlayerStats();
            PlayerStatsEmbeddable statsEmb = entity.getStats();
            stats.setMoney(statsEmb.getMoney());
            stats.setTotalIncome(statsEmb.getTotalIncome());
            stats.setTotalVehiclesValue(statsEmb.getTotalVehiclesValue());
            stats.setTotalEquipmentValue(statsEmb.getTotalEquipmentValue());
            stats.setTotalOffensivePower(statsEmb.getTotalOffensivePower());
            stats.setTotalDefensivePower(statsEmb.getTotalDefensivePower());
            stats.setGlobalPower(statsEmb.getGlobalPower());
            stats.setTotalEconomyPower(statsEmb.getTotalEconomyPower());
            stats.setTotalAtk(statsEmb.getTotalAtk());
            stats.setTotalPdf(statsEmb.getTotalPdf());
            stats.setTotalPdc(statsEmb.getTotalPdc());
            stats.setTotalDef(statsEmb.getTotalDef());
            stats.setTotalArmor(statsEmb.getTotalArmor());
            player.setStats(stats);
        }

        // Conversion des bonus (si nécessaire)
        if (entity.getBonuses() != null) {
            PlayerBonuses bonuses = new PlayerBonuses();
            PlayerBonusesEmbeddable bonusesEmb = entity.getBonuses();
            bonuses.setAttackBonusPercent(bonusesEmb.getAttackBonusPercent());
            bonuses.setDefenseBonusPercent(bonusesEmb.getDefenseBonusPercent());
            bonuses.setPdfBonusPercent(bonusesEmb.getPdfBonusPercent());
            bonuses.setPdcBonusPercent(bonusesEmb.getPdcBonusPercent());
            bonuses.setArmorBonusPercent(bonusesEmb.getArmorBonusPercent());
            bonuses.setEvasionBonusPercent(bonusesEmb.getEvasionBonusPercent());
            // Note: Player n'a pas de champ bonuses dans le domaine actuel
            // Vous pourrez l'ajouter si nécessaire
        }

        // Conversion des équipements (EquipmentStack)
        if (entity.getEquipments() != null) {
            List<EquipmentStack> equipmentStacks = entity.getEquipments().stream()
                    .map(this::equipmentStackToDomain)
                    .toList();
            player.setEquipments(equipmentStacks);
        }

        // Conversion des IDs de secteurs
        if (entity.getOwnedSectorIds() != null) {
            player.setOwnedSectorIds(new HashSet<>(entity.getOwnedSectorIds()));
        }

        // Définir l'ID du joueur
        player.setId(entity.getId());

        return player;
    }

    /**
     * Convertit un DTO PlayerDto en objet Player du domaine
     */
    public Player toDomain(PlayerDto dto) {
        if (dto == null) return new Player();

        Player player = new Player(dto.getName());

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

        // Définir l'ID du joueur
        player.setId(dto.getId());

        return player;
    }

    /**
     * Convertit un objet Player du domaine en entité PlayerEntity
     */
    public PlayerEntity toEntity(Player player) {
        if (player == null) return new PlayerEntity();

        PlayerEntity entity = new PlayerEntity();
        entity.setName(player.getName());

        // Conversion des stats
        if (player.getStats() != null) {
            PlayerStatsEmbeddable statsEmb = new PlayerStatsEmbeddable();
            PlayerStats stats = player.getStats();
            statsEmb.setMoney(stats.getMoney());
            statsEmb.setTotalIncome(stats.getTotalIncome());
            statsEmb.setTotalVehiclesValue(stats.getTotalVehiclesValue());
            statsEmb.setTotalEquipmentValue(stats.getTotalEquipmentValue());
            statsEmb.setTotalOffensivePower(stats.getTotalOffensivePower());
            statsEmb.setTotalDefensivePower(stats.getTotalDefensivePower());
            statsEmb.setGlobalPower(stats.getGlobalPower());
            statsEmb.setTotalEconomyPower(stats.getTotalEconomyPower());
            statsEmb.setTotalAtk(stats.getTotalAtk());
            statsEmb.setTotalPdf(stats.getTotalPdf());
            statsEmb.setTotalPdc(stats.getTotalPdc());
            statsEmb.setTotalDef(stats.getTotalDef());
            statsEmb.setTotalArmor(stats.getTotalArmor());
            entity.setStats(statsEmb);
        }

        // Conversion des équipements
        if (player.getEquipments() != null) {
            List<EquipmentStackEntity> equipmentStackEntities = player.getEquipments().stream()
                    .map(stack -> equipmentStackToEntity(stack, entity))
                    .toList();
            entity.setEquipments(equipmentStackEntities);
        }

        // Conversion des IDs de secteurs
        if (player.getOwnedSectorIds() != null) {
            entity.setOwnedSectorIds(new HashSet<>(player.getOwnedSectorIds()));
        }

        // Définir l'ID de l'entité
        if (player.getId() != null) {
            entity.setId(player.getId());
        }

        return entity;
    }

    /**
     * Convertit un objet Player du domaine en DTO
     */
    public PlayerDto toDto(Player player) {
        if (player == null) return new PlayerDto();

        PlayerDto dto = new PlayerDto();
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
            List<EquipmentStackDto> equipmentStackDtos = player.getEquipments().stream()
                    .map(this::equipmentStackToDto)
                    .toList();
            dto.setEquipments(equipmentStackDtos);
        }

        // Conversion des IDs de secteurs
        if (player.getOwnedSectorIds() != null) {
            dto.setOwnedSectorIds(new HashSet<>(player.getOwnedSectorIds()));
        }

        // Définir l'ID du DTO
        dto.setId(player.getId());

        return dto;
    }

    // === MÉTHODES AUXILIAIRES POUR EQUIPMENTSTACK ===

    private EquipmentStack equipmentStackToDomain(EquipmentStackEntity entity) {
        if (entity == null) return null;
        Equipment equipment = equipmentMapper.toDomain(entity.getEquipment());
        EquipmentStack stack = new EquipmentStack(equipment);
        stack.setQuantity(entity.getQuantity());
        stack.setAvailable(entity.getAvailable());
        return stack;
    }

    private EquipmentStack equipmentStackFromDto(EquipmentStackDto dto) {
        if (dto == null) return null;
        Equipment equipment = equipmentMapper.toDomain(dto.getEquipment());
        EquipmentStack stack = new EquipmentStack(equipment);
        stack.setQuantity(dto.getQuantity());
        stack.setAvailable(dto.getAvailable());
        return stack;
    }

    public EquipmentStackEntity equipmentStackToEntity(EquipmentStack stack, PlayerEntity player) {
        if (stack == null) return null;
        EquipmentStackEntity entity = new EquipmentStackEntity();
        entity.setPlayer(player);

        // Rechercher l'équipement existant par nom
        // Il DOIT exister car data.sql l'a inséré
        EquipmentEntity equipmentEntity = equipmentRepository.findByName(stack.getEquipment().getName())
                .orElseThrow(() -> new IllegalStateException(
                    "Equipment not found: " + stack.getEquipment().getName() +
                    ". Make sure data.sql has been executed."
                ));

        entity.setEquipment(equipmentEntity);
        entity.setQuantity(stack.getQuantity());
        entity.setAvailable(stack.getAvailable());
        return entity;
    }

    private EquipmentStackDto equipmentStackToDto(EquipmentStack stack) {
        if (stack == null) return null;
        EquipmentStackDto dto = new EquipmentStackDto();
        dto.setEquipment(equipmentMapper.toDto(stack.getEquipment()));
        dto.setQuantity(stack.getQuantity());
        dto.setAvailable(stack.getAvailable());
        return dto;
    }
}
