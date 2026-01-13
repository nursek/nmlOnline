package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.EquipmentDto;
import com.mg.nmlonline.api.dto.UnitClassDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.api.dto.UnitTypeDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper simplifié pour Unit - conversion uniquement entre Domain et DTO
 */
@Component
public class UnitMapper {

    private final EquipmentMapper equipmentMapper;

    public UnitMapper(EquipmentMapper equipmentMapper) {
        this.equipmentMapper = equipmentMapper;
    }

    /**
     * Convertit un DTO UnitDto en objet Unit du domaine
     */
    public Unit toDomain(UnitDto dto) {
        if (dto == null) return null;

        Unit unit = new Unit();
        unit.setId(dto.getId() != null ? dto.getId() : 0);
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

        unit.setInjured(dto.getIsInjured() != null && dto.getIsInjured());

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
     * Convertit un objet Unit du domaine en DTO UnitDto
     */
    public UnitDto toDto(Unit unit) {
        if (unit == null) return null;

        UnitDto dto = new UnitDto();
        dto.setId(unit.getId());
        dto.setNumber(unit.getNumber());
        dto.setExperience(unit.getExperience());

        // Conversion du type
        if (unit.getType() != null) {
            UnitTypeDto typeDto = new UnitTypeDto();
            typeDto.setName(unit.getType().name());
            typeDto.setLevel(unit.getType().getLevel());
            typeDto.setBaseAttack(unit.getType().getBaseAttack());
            typeDto.setBaseDefense(unit.getType().getBaseDefense());
            dto.setType(typeDto);
        }

        // Conversion des classes
        if (unit.getClasses() != null) {
            List<UnitClassDto> classDtos = unit.getClasses().stream()
                    .map(this::toUnitClassDto)
                    .toList();
            dto.setClasses(classDtos);
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

    // === Méthodes utilitaires ===

    private UnitClass fromUnitClassDto(UnitClassDto dto) {
        if (dto == null || dto.getName() == null) return null;
        try {
            return UnitClass.valueOf(dto.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private UnitClassDto toUnitClassDto(UnitClass unitClass) {
        if (unitClass == null) return null;
        UnitClassDto dto = new UnitClassDto();
        dto.setName(unitClass.name());
        dto.setCode(unitClass.getCode());
        return dto;
    }
}