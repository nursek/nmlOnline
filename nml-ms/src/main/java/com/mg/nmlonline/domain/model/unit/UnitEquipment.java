package com.mg.nmlonline.domain.model.unit;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Table de liaison entre Unit et Equipment
 * Permet de gérer les équipements portés par une unité
 */
@Entity
@Table(name = "UNIT_EQUIPMENTS")
@Data
@NoArgsConstructor
public class UnitEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    public UnitEquipment(Unit unit, Equipment equipment) {
        this.unit = unit;
        this.equipment = equipment;
    }
}

