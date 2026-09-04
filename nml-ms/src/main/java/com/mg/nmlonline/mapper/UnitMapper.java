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

@Component
public class UnitMapper {

    private final EquipmentMapper equipmentMapper;

    public UnitMapper(EquipmentMapper equipmentMapper) {
        this.equipmentMapper = equipmentMapper;
    }

    public Unit toDomain(UnitDto dto) {
        if (dto == null) return null;

        Unit unit = new Unit();
        unit.setId(dto.getId() != null ? dto.getId().longValue() : null);
        unit.setNumber(dto.getNumber() != null ? dto.getNumber() : 0);
        unit.setExperience(dto.getExperience() != null ? dto.getExperience() : 0.0);

        if (dto.getType() != null && dto.getType().getName() != null) {
            try {
                unit.setType(UnitType.valueOf(dto.getType().getName()));
            } catch (IllegalArgumentException e) {
                unit.setType(UnitType.LARBIN);
            }
        }

        if (dto.getClasses() != null) {
            List<UnitClass> classes = dto.getClasses().stream()
                    .map(this::fromUnitClassDto)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            unit.setClasses(classes);
        }

        unit.setInjured(dto.getIsInjured() != null && dto.getIsInjured());

        unit.setAttack(dto.getAttack() != null ? dto.getAttack() : 0.0);
        unit.setDefense(dto.getDefense() != null ? dto.getDefense() : 0.0);
        unit.setPdf(dto.getPdf() != null ? dto.getPdf() : 0.0);
        unit.setPdc(dto.getPdc() != null ? dto.getPdc() : 0.0);
        unit.setArmor(dto.getArmor() != null ? dto.getArmor() : 0.0);
        unit.setEvasion(dto.getEvasion() != null ? dto.getEvasion() : 0.0);

        if (dto.getEquipments() != null) {
            List<Equipment> equipments = dto.getEquipments().stream()
                    .map(equipmentMapper::toDomain)
                    .toList();
            unit.setEquipments(equipments);
        }

        return unit;
    }

    public UnitDto toDto(Unit unit) {
        if (unit == null) return null;

        UnitDto dto = new UnitDto();
        Long unitId = unit.getId();
        if (unitId != null) {
            if (unitId > Integer.MAX_VALUE || unitId < Integer.MIN_VALUE) {
                throw new IllegalArgumentException("Unit id out of Integer range: " + unitId);
            }
            dto.setId(unitId.intValue());
        }
        dto.setPlayerId(unit.getPlayerId());
        dto.setNumber(unit.getNumber());
        dto.setExperience(unit.getExperience());

        if (unit.getType() != null) {
            UnitTypeDto typeDto = new UnitTypeDto();
            typeDto.setName(unit.getType().name());
            typeDto.setLevel(unit.getType().getLevel());
            typeDto.setMinExp(unit.getType().getMinExp());
            typeDto.setMaxExp(unit.getType().getMaxExp());
            typeDto.setBaseAttack(unit.getType().getBaseAttack());
            typeDto.setBaseDefense(unit.getType().getBaseDefense());
            typeDto.setMaxFirearms(unit.getType().getMaxFirearms());
            typeDto.setMaxMeleeWeapons(unit.getType().getMaxMeleeWeapons());
            typeDto.setMaxDefensiveEquipment(unit.getType().getMaxDefensiveEquipment());
            dto.setType(typeDto);
        }

        if (unit.getClasses() != null) {
            List<UnitClassDto> classDtos = unit.getClasses().stream()
                    .map(this::toUnitClassDto)
                    .toList();
            dto.setClasses(classDtos);
        }

        dto.setIsInjured(unit.isInjured());

        dto.setAttack(unit.getAttack());
        dto.setDefense(unit.getDefense());
        dto.setPdf(unit.getPdf());
        dto.setPdc(unit.getPdc());
        dto.setArmor(unit.getArmor());
        dto.setEvasion(unit.getEvasion());

        if (unit.getEquipments() != null) {
            List<EquipmentDto> equipmentDtos = unit.getEquipments().stream()
                    .map(equipmentMapper::toDto)
                    .toList();
            dto.setEquipments(equipmentDtos);
        }

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

    private UnitClassDto toUnitClassDto(UnitClass unitClass) {
        if (unitClass == null) return null;
        UnitClassDto dto = new UnitClassDto();
        dto.setName(unitClass.name());
        dto.setCode(unitClass.getCode());
        dto.setCriticalChance(unitClass.getCriticalChance());
        dto.setCriticalMultiplier(unitClass.getCriticalMultiplier());
        dto.setDamageReductionPdf(unitClass.getDamageReduction("PDF"));
        dto.setDamageReductionPdc(unitClass.getDamageReduction("PDC"));
        dto.setMaxMovementHops(unitClass.getMaxMovementHops());
        return dto;
    }
}