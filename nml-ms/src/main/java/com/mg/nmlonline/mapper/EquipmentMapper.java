package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.EquipmentDto;
import com.mg.nmlonline.api.dto.UnitClassDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.entity.EquipmentEntity;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EquipmentMapper {

    /**
     * Convertit une entité EquipmentEntity en objet Equipment du domaine
     */
    public Equipment toDomain(EquipmentEntity entity) {
        if (entity == null) return null;

        // Conversion de la classe compatible unique en Set
        Set<UnitClass> compatibleClasses = entity.getCompatibleClass() != null
                ? Set.of(entity.getCompatibleClass())
                : new HashSet<>();

        EquipmentCategory category = entity.getCategory();

        return new Equipment(
                entity.getName(),
                entity.getCost(),
                entity.getPdfBonus(),
                entity.getPdcBonus(),
                entity.getArmBonus(),
                entity.getEvasionBonus(),
                compatibleClasses,
                category
        );
    }

    /**
     * Convertit un objet Equipment du domaine en entité EquipmentEntity
     */
    public EquipmentEntity toEntity(Equipment domain) {
        if (domain == null) return null;

        EquipmentEntity entity = new EquipmentEntity();
        entity.setName(domain.getName());
        entity.setCost(domain.getCost());
        entity.setPdfBonus(domain.getPdfBonus());
        entity.setPdcBonus(domain.getPdcBonus());
        entity.setArmBonus(domain.getArmBonus());
        entity.setEvasionBonus(domain.getEvasionBonus());

        // Conversion du Set en classe unique (prend la première)
        if (domain.getCompatibleClasses() != null && !domain.getCompatibleClasses().isEmpty()) {
            entity.setCompatibleClass(domain.getCompatibleClasses().iterator().next());
        }

        entity.setCategory(domain.getCategory());

        return entity;
    }

    /**
     * Convertit un objet Equipment du domaine en DTO
     */
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
                compatibleClassDtos,
                category
        );
    }

    /**
     * Convertit un DTO en objet Equipment du domaine
     */
    public Equipment toDomain(EquipmentDto dto) {
        if (dto == null) return null;

        Set<UnitClass> compatibleClasses = toUnitClass(dto.getCompatibleClass());
        EquipmentCategory category = null;

        if (dto.getCategory() != null) {
            try {
                category = EquipmentCategory.valueOf(dto.getCategory());
            } catch (IllegalArgumentException e) {
                // Catégorie invalide, on laisse null
            }
        }

        return new Equipment(
                dto.getName(),
                (int) dto.getCost(),
                dto.getPdfBonus(),
                dto.getPdcBonus(),
                dto.getArmBonus(),
                dto.getEvasionBonus(),
                compatibleClasses,
                category
        );
    }

    // === MÉTHODES AUXILIAIRES ===

    /**
     * Convertit Set<UnitClass> en Set<UnitClassDto>
     */
    private Set<UnitClassDto> toUnitClassDto(Set<UnitClass> classes) {
        if (classes == null) return new HashSet<>();

        return classes.stream()
                .map(uc -> {
                    UnitClassDto dto = new UnitClassDto();
                    dto.setName(uc.name());
                    dto.setCode(uc.getCode());
                    dto.setCriticalChance(uc.getCriticalChance());
                    dto.setCriticalMultiplier(uc.getCriticalMultiplier());
                    dto.setDamageReductionPdf(uc.getDamageReduction("PDF"));
                    dto.setDamageReductionPdc(uc.getDamageReduction("PDC"));
                    return dto;
                })
                .collect(Collectors.toSet());
    }

    /**
     * Convertit Set<UnitClassDto> en Set<UnitClass>
     */
    private Set<UnitClass> toUnitClass(Set<UnitClassDto> dtos) {
        if (dtos == null) return new HashSet<>();

        return dtos.stream()
                .filter(dto -> dto.getCode() != null)
                .map(dto -> {
                    for (UnitClass uc : UnitClass.values()) {
                        if (uc.getCode().equals(dto.getCode())) {
                            return uc;
                        }
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
