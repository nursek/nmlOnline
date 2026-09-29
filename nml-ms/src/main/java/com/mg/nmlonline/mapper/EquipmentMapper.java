package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.EquipmentDto;
import com.mg.nmlonline.api.dto.UnitClassDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.VehicleBonusTarget;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EquipmentMapper {

    public EquipmentDto toDto(Equipment domain) {
        if (domain == null) return null;

        Set<UnitClassDto> compatibleClassDtos = toUnitClassDto(domain.getCompatibleClasses());
        String category = Optional.ofNullable(domain.getCategory())
                .map(Enum::name)
                .orElse(null);

        return new EquipmentDto(
                domain.getName(),
                domain.getCost(),
                domain.getPdfBonus(),
                domain.getPdcBonus(),
                domain.getArmBonus(),
                domain.getEvasionBonus(),
                domain.getVehicleBonus(),
                domain.getVehicleBonusTarget() != null ? domain.getVehicleBonusTarget().name() : null,
                compatibleClassDtos,
                category
        );
    }

    public Equipment toDomain(EquipmentDto dto) {
        if (dto == null) return null;

        Set<UnitClass> compatibleClasses = toUnitClass(dto.getCompatibleClass());
        EquipmentCategory category = null;

        if (dto.getCategory() != null) {
            try {
                category = EquipmentCategory.valueOf(dto.getCategory());
            } catch (IllegalArgumentException e) {
                category = EquipmentCategory.FIREARM;
            }
        }

        Equipment equipment = new Equipment(
                dto.getName(),
                (int) dto.getCost(),
                dto.getPdfBonus(),
                dto.getPdcBonus(),
                dto.getArmBonus(),
                dto.getEvasionBonus(),
                compatibleClasses,
                category
        );
        equipment.setVehicleBonus(dto.getVehicleBonus());
        if (dto.getVehicleBonusTarget() != null && !dto.getVehicleBonusTarget().isBlank()) {
            try {
                equipment.setVehicleBonusTarget(VehicleBonusTarget.valueOf(dto.getVehicleBonusTarget()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Cible anti-véhicule invalide : " + dto.getVehicleBonusTarget());
            }
        }
        return equipment;
    }

    private Set<UnitClassDto> toUnitClassDto(Set<UnitClass> classes) {
        if (classes == null || classes.isEmpty()) {
            return new HashSet<>();
        }
        return classes.stream()
                .map(this::toUnitClassDto)
                .collect(Collectors.toSet());
    }

    private UnitClassDto toUnitClassDto(UnitClass unitClass) {
        if (unitClass == null) return null;
        UnitClassDto dto = new UnitClassDto();
        dto.setName(unitClass.name());
        dto.setCode(unitClass.getCode());
        return dto;
    }

    private Set<UnitClass> toUnitClass(Set<UnitClassDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new HashSet<>();
        }
        return dtos.stream()
                .map(this::toUnitClass)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private UnitClass toUnitClass(UnitClassDto dto) {
        if (dto == null || dto.getName() == null) return null;
        try {
            return UnitClass.valueOf(dto.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
