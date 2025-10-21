// java
package com.mg.nmlonline.infrastructure.entity;

import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "EQUIPMENT")
@Data
@NoArgsConstructor
public class EquipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int cost;

    @Column(name = "pdf_bonus", nullable = false)
    private double pdfBonus;

    @Column(name = "pdc_bonus", nullable = false)
    private double pdcBonus;

    @Column(name = "arm_bonus", nullable = false)
    private double armBonus;

    @Column(name = "evasion_bonus", nullable = false)
    private double evasionBonus;

    @Enumerated(EnumType.STRING)
    @Column(name = "compatible_class")
    private UnitClass compatibleClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentCategory category;
}
