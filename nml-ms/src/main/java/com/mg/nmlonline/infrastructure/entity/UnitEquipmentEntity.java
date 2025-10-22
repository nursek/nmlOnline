package com.mg.nmlonline.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Table de liaison entre Unit et Equipment
 * Permet de gérer les équipements portés par une unité
 * Ces équipements occupent une quantité disponible dans l'inventaire du joueur (EquipmentStack)
 */
@Entity
@Table(name = "UNIT_EQUIPMENTS")
@Data
@NoArgsConstructor
public class UnitEquipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private UnitEntity unit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "equipment_id", nullable = false)
    private EquipmentEntity equipment;

    // Optionnel : on pourrait ajouter des infos sur l'équipement équipé
    // Par exemple si l'équipement est endommagé, etc.
}
