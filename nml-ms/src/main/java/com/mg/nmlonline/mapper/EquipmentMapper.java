package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.EquipmentDto;
import com.mg.nmlonline.api.dto.UnitClassDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.VehicleBonusTarget;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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
                category,
                toDisplayNameDtos(domain.getDisplayNames())
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
        if (dto.getVehicleBonus() < 0) {
            throw new IllegalArgumentException(
                    "Bonus anti-véhicule négatif interdit : " + dto.getVehicleBonus());
        }
        equipment.setVehicleBonus(dto.getVehicleBonus());
        if (dto.getVehicleBonusTarget() != null && !dto.getVehicleBonusTarget().isBlank()) {
            try {
                equipment.setVehicleBonusTarget(VehicleBonusTarget.valueOf(dto.getVehicleBonusTarget()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Cible anti-véhicule invalide : " + dto.getVehicleBonusTarget());
            }
        }
        equipment.setDisplayNames(toDisplayNames(dto.getDisplayNames()));
        return equipment;
    }

    private Map<String, String> toDisplayNameDtos(Map<PlayerRace, String> displayNames) {
        if (displayNames == null || displayNames.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> dtos = new HashMap<>();
        displayNames.forEach((race, label) -> dtos.put(race.name(), label));
        return dtos;
    }

    private Map<PlayerRace, String> toDisplayNames(Map<String, String> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return new HashMap<>();
        }
        Map<PlayerRace, String> displayNames = new HashMap<>();
        dtos.forEach((race, label) -> {
            if (race == null) {
                throw new IllegalArgumentException("Race inconnue pour la traduction : null");
            }
            try {
                displayNames.put(PlayerRace.valueOf(race.trim().toUpperCase(Locale.ROOT)), label);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Race inconnue pour la traduction : " + race);
            }
        });
        return displayNames;
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
