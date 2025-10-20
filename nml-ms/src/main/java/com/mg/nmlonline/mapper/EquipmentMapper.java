package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.EquipmentDto;
import com.mg.nmlonline.api.dto.UnitClassDto;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.infrastructure.entity.EquipmentEntity;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class EquipmentMapper {

    public Equipment toDomain(EquipmentEntity e) {
        if (e == null) return null;
        Set<UnitClass> compatible = (e.getCompatibleClass() == null) ? Set.of() : Set.of(e.getCompatibleClass());
        return new Equipment(
                e.getName(),
                e.getCost(),
                e.getPdfBonus(),
                e.getPdcBonus(),
                e.getArmBonus(),
                e.getEvasionBonus(),
                compatible,
                null
        );
    }

    public EquipmentEntity toEntity(Equipment domain) {
        if (domain == null) return null;
        EquipmentEntity e = new EquipmentEntity();
        e.setName(domain.getName());
        e.setCost(domain.getCost());
        e.setPdfBonus((int) Math.round(domain.getPdfBonus()));
        e.setPdcBonus((int) Math.round(domain.getPdcBonus()));
        e.setArmBonus((int) Math.round(domain.getArmBonus()));
        e.setEvasionBonus((int) Math.round(domain.getEvasionBonus()));
        e.setCompatibleClass(domain.getCompatibleClasses() == null || domain.getCompatibleClasses().isEmpty()
                ? null
                : domain.getCompatibleClasses().iterator().next());
        e.setCategory(domain.getCategory() == null ? null : domain.getCategory().name());
        return e;
    }

    // Ajout du mapping UnitClass <-> UnitClassDto
    private UnitClassDto toUnitClassDto(UnitClass uc) {
        if (uc == null) return null;
        UnitClassDto dto = new UnitClassDto();
        dto.setName(uc.name());
        dto.setCode(uc.getCode());
        dto.setCriticalChance(uc.getCriticalChance());
        dto.setCriticalMultiplier(uc.getCriticalMultiplier());
        dto.setDamageReductionPdf(uc.getDamageReduction("PDF"));
        dto.setDamageReductionPdc(uc.getDamageReduction("PDC"));

        return dto;
    }

    private UnitClass toUnitClass(UnitClassDto dto) {
        if (dto == null || dto.getCode() == null) return null;
        for (UnitClass uc : UnitClass.values()) {
            if (uc.getCode().equals(dto.getCode())) {
                return uc;
            }
        }
        return null;
    }

    public EquipmentDto toDto(Equipment d) {
        if (d == null) return null;
        UnitClass comp = (d.getCompatibleClasses() == null || d.getCompatibleClasses().isEmpty())
                ? null
                : d.getCompatibleClasses().iterator().next();
        String category = Optional.ofNullable(d.getCategory()).map(Enum::name).orElse(null);
        return new EquipmentDto(
                d.getName(),
                d.getCost(),
                d.getPdfBonus(),
                d.getPdcBonus(),
                d.getArmBonus(),
                d.getEvasionBonus(),
                toUnitClassDto(comp),
                category
        );
    }

    public Equipment toDomain(EquipmentDto dto) {
        if (dto == null) return null;
        UnitClass comp = toUnitClass(dto.getCompatibleClass());
        Set<UnitClass> compatible = (comp == null) ? Set.of() : Set.of(comp);
        EquipmentCategory category = null;
        if (dto.getCategory() != null) {
            try {
                category = EquipmentCategory.valueOf(dto.getCategory());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new Equipment(
                dto.getName(),
                (int) Math.round(dto.getCost()),
                dto.getPdfBonus(),
                dto.getPdcBonus(),
                dto.getArmBonus(),
                dto.getEvasionBonus(),
                compatible,
                category
        );
    }
}
