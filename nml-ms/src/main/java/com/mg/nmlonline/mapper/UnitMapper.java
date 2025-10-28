package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.EquipmentDto;
import com.mg.nmlonline.api.dto.UnitClassDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.api.dto.UnitTypeDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.infrastructure.entity.SectorEntity;
import com.mg.nmlonline.infrastructure.entity.UnitEntity;
import com.mg.nmlonline.infrastructure.entity.UnitEquipmentEntity;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
public class UnitMapper {

    private final EquipmentMapper equipmentMapper;
    private final EquipmentRepository equipmentRepository;

    public UnitMapper(EquipmentMapper equipmentMapper, EquipmentRepository equipmentRepository) {
        this.equipmentMapper = equipmentMapper;
        this.equipmentRepository = equipmentRepository;
    }

    /**
     * Convertit une entité UnitEntity en objet Unit du domaine
     */
    public Unit toDomain(UnitEntity entity) {
        if (entity == null) return null;

        Unit unit = new Unit();
        unit.setId(entity.getId() != null ? entity.getId().intValue() : 0);
        unit.setName(entity.getName());
        unit.setNumber(entity.getNumber());
        unit.setExperience(entity.getExperience());
        unit.setType(entity.getType());
        unit.setClasses(new ArrayList<>(entity.getClasses() != null ? entity.getClasses() : new HashSet<>()));
        unit.setInjured(entity.isInjured());

        // Stats
        unit.setAttack(entity.getAttack());
        unit.setDefense(entity.getDefense());
        unit.setPdf(entity.getPdf());
        unit.setPdc(entity.getPdc());
        unit.setArmor(entity.getArmor());
        unit.setEvasion(entity.getEvasion());

        // Conversion des équipements
        if (entity.getEquipments() != null) {
            List<Equipment> equipments = entity.getEquipments().stream()
                    .map(UnitEquipmentEntity::getEquipment)
                    .map(equipmentMapper::toDomain)
                    .toList();
            unit.setEquipments(equipments);
        }

        return unit;
    }

    /**
     * Convertit un DTO UnitDto en objet Unit du domaine
     */
    public Unit toDomain(UnitDto dto) {
        if (dto == null) return null;

        Unit unit = new Unit();
        unit.setId(dto.getId() != null ? dto.getId() : 0);
        unit.setName(dto.getName());
        unit.setNumber(dto.getNumber() != null ? dto.getNumber() : 0);
        unit.setExperience(dto.getExperience() != null ? dto.getExperience() : 0.0);

        // Conversion du type
        if (dto.getType() != null && dto.getType().getName() != null) {
            try {
                unit.setType(UnitType.valueOf(dto.getType().getName()));
            } catch (IllegalArgumentException e) {
                unit.setType(UnitType.LARBIN);
            }
        }

        // Conversion des classes
        if (dto.getClasses() != null) {
            List<UnitClass> classes = dto.getClasses().stream()
                    .map(this::fromUnitClassDto)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            unit.setClasses(classes);
        }

        unit.setInjured(dto.getIsInjured());

        // Stats
        unit.setAttack(dto.getAttack() != null ? dto.getAttack() : 0.0);
        unit.setDefense(dto.getDefense() != null ? dto.getDefense() : 0.0);
        unit.setPdf(dto.getPdf() != null ? dto.getPdf() : 0.0);
        unit.setPdc(dto.getPdc() != null ? dto.getPdc() : 0.0);
        unit.setArmor(dto.getArmor() != null ? dto.getArmor() : 0.0);
        unit.setEvasion(dto.getEvasion() != null ? dto.getEvasion() : 0.0);

        // Équipements
        if (dto.getEquipments() != null) {
            List<Equipment> equipments = dto.getEquipments().stream()
                    .map(equipmentMapper::toDomain)
                    .toList();
            unit.setEquipments(equipments);
        }

        return unit;
    }

    /**
     * Convertit un objet Unit du domaine en entité UnitEntity
     */
    public UnitEntity toEntity(Unit unit, SectorEntity sector) {
        if (unit == null) return null;

        UnitEntity entity = new UnitEntity();
        entity.setSector(sector);
        entity.setName(unit.getName());
        entity.setNumber(unit.getNumber());
        entity.setExperience(unit.getExperience());
        entity.setType(unit.getType());
        entity.setClasses(new HashSet<>(unit.getClasses() != null ? unit.getClasses() : new ArrayList<>()));
        entity.setInjured(unit.isInjured());

        // Stats
        entity.setAttack(unit.getAttack());
        entity.setDefense(unit.getDefense());
        entity.setPdf(unit.getPdf());
        entity.setPdc(unit.getPdc());
        entity.setArmor(unit.getArmor());
        entity.setEvasion(unit.getEvasion());

        // Conversion des équipements - rechercher les existants
        if (unit.getEquipments() != null) {
            List<UnitEquipmentEntity> unitEquipments = unit.getEquipments().stream()
                    .map(equipment -> {
                        UnitEquipmentEntity ue = new UnitEquipmentEntity();
                        ue.setUnit(entity);

                        // Rechercher l'équipement existant par nom
                        var equipmentEntity = equipmentRepository.findByName(equipment.getName())
                                .orElseGet(() -> equipmentMapper.toEntity(equipment));

                        ue.setEquipment(equipmentEntity);
                        return ue;
                    })
                    .toList();
            entity.setEquipments(unitEquipments);
        }

        return entity;
    }

    /**
     * Convertit un objet Unit du domaine en DTO
     */
    public UnitDto toDto(Unit unit) {
        if (unit == null) return null;

        UnitDto dto = new UnitDto();
        dto.setId(unit.getId());
        dto.setName(unit.getName());
        dto.setNumber(unit.getNumber());
        dto.setExperience(unit.getExperience());

        // Conversion du type en DTO
        dto.setType(toUnitTypeDto(unit.getType()));

        // Conversion des classes en DTOs
        if (unit.getClasses() != null) {
            dto.setClasses(unit.getClasses().stream()
                    .map(this::toUnitClassDto)
                    .toList());
        }

        dto.setIsInjured(unit.isInjured());

        // Stats
        dto.setAttack(unit.getAttack());
        dto.setDefense(unit.getDefense());
        dto.setPdf(unit.getPdf());
        dto.setPdc(unit.getPdc());
        dto.setArmor(unit.getArmor());
        dto.setEvasion(unit.getEvasion());

        // Équipements
        if (unit.getEquipments() != null) {
            List<EquipmentDto> equipmentDtos = unit.getEquipments().stream()
                    .map(equipmentMapper::toDto)
                    .toList();
            dto.setEquipments(equipmentDtos);
        }

        return dto;
    }

    // === MÉTHODES AUXILIAIRES ===

    private UnitTypeDto toUnitTypeDto(UnitType type) {
        if (type == null) return null;

        UnitTypeDto dto = new UnitTypeDto();
        dto.setName(type.name());
        dto.setLevel(type.getLevel());
        dto.setMinExp(type.getMinExp());
        dto.setMaxExp(type.getMaxExp());
        dto.setBaseAttack(type.getBaseAttack());
        dto.setBaseDefense(type.getBaseDefense());
        dto.setMaxFirearms(type.getMaxFirearms());
        dto.setMaxMeleeWeapons(type.getMaxMeleeWeapons());
        dto.setMaxDefensiveEquipment(type.getMaxDefensiveEquipment());
        return dto;
    }

    private UnitClassDto toUnitClassDto(UnitClass unitClass) {
        if (unitClass == null) return null;

        UnitClassDto dto = new UnitClassDto();
        dto.setName(unitClass.name());
        dto.setCode(unitClass.getCode());
        dto.setCriticalChance(unitClass.getCriticalChance());
        dto.setCriticalMultiplier(unitClass.getCriticalMultiplier());
        dto.setDamageReductionPdf(unitClass.getDamageReduction("PDF"));
        dto.setDamageReductionPdc(unitClass.getDamageReduction("PDC"));
        return dto;
    }

    private UnitClass fromUnitClassDto(UnitClassDto dto) {
        if (dto == null || dto.getName() == null) return null;
        try {
            return UnitClass.valueOf(dto.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}