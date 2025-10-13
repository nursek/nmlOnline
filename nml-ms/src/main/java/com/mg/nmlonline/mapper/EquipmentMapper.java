package com.mg.nmlonline.mapper;

import com.mg.nmlonline.dto.EquipmentDto;
import com.mg.nmlonline.entity.equipment.Equipment;
import com.mg.nmlonline.entity.equipment.EquipmentEntity;
import com.mg.nmlonline.entity.unit.UnitClass;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class EquipmentMapper {

    public Equipment entityToDomain(EquipmentEntity e) {
        if (e == null) return null;
        Set<UnitClass> compatible = (e.getCompatibleClass() == null) ? Set.of() : Set.of(e.getCompatibleClass());
        // Equipment constructor (final fields) : (String name, int cost, double pdfBonus, double pdcBonus, double armBonus, double evasionBonus, Set<UnitClass> compatibleClasses, EquipmentCategory category)
        // on laisse category à null si non disponible/convertible simplement
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

    public EquipmentEntity domainToEntity(Equipment domain) {
        if (domain == null) return null;
        EquipmentEntity e = new EquipmentEntity();
        e.setName(domain.getName());
        e.setCost(domain.getCost());
        // les champs pdf/pdc/arm/evasion sont des int en entité -> arrondir depuis le domaine si nécessaire
        e.setPdfBonus((int) Math.round(domain.getPdfBonus()));
        e.setPdcBonus((int) Math.round(domain.getPdcBonus()));
        e.setArmBonus((int) Math.round(domain.getArmBonus()));
        e.setEvasionBonus((int) Math.round(domain.getEvasionBonus()));
        // prendo le premier compatibleClass s'il existe
        e.setCompatibleClass(domain.getCompatibleClasses() == null || domain.getCompatibleClasses().isEmpty()
                ? null
                : domain.getCompatibleClasses().iterator().next());
        // category en base est String ; si le domaine a une enum, on conserve son name()
        e.setCategory(domain.getCategory() == null ? null : domain.getCategory().name());
        return e;
    }

    public EquipmentDto domainToDto(Equipment d) {
        if (d == null) return null;
        Long id = null;
        UnitClass comp = (d.getCompatibleClasses() == null || d.getCompatibleClasses().isEmpty())
                ? null
                : d.getCompatibleClasses().iterator().next();
        String category = Optional.ofNullable(d.getCategory()).map(Enum::name).orElse(null);
        return new EquipmentDto(
                id,
                d.getName(),
                d.getCost(),
                (int) Math.round(d.getPdfBonus()),
                (int) Math.round(d.getPdcBonus()),
                (int) Math.round(d.getArmBonus()),
                (int) Math.round(d.getEvasionBonus()),
                comp,
                category
        );
    }

    public EquipmentDto entityToDto(EquipmentEntity e) {
        if (e == null) return null;
        return new EquipmentDto(
                e.getId(),
                e.getName(),
                e.getCost(),
                e.getPdfBonus(),
                e.getPdcBonus(),
                e.getArmBonus(),
                e.getEvasionBonus(),
                e.getCompatibleClass(),
                e.getCategory()
        );
    }

    public EquipmentEntity dtoToEntity(EquipmentDto dto) {
        if (dto == null) return null;
        EquipmentEntity e = new EquipmentEntity();
        e.setId(dto.getId());
        e.setName(dto.getName());
        e.setCost(dto.getCost());
        e.setPdfBonus(dto.getPdfBonus());
        e.setPdcBonus(dto.getPdcBonus());
        e.setArmBonus(dto.getArmBonus());
        e.setEvasionBonus(dto.getEvasionBonus());
        e.setCompatibleClass(dto.getCompatibleClass());
        e.setCategory(dto.getCategory());
        return e;
    }
}
