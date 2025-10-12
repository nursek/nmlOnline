// java
package com.mg.nmlonline.entity.equipment;

import com.mg.nmlonline.entity.unit.UnitClass;
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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int cost;

    @Column(name = "pdf_bonus", nullable = false)
    private int pdfBonus;

    @Column(name = "pdc_bonus", nullable = false)
    private int pdcBonus;

    @Column(name = "arm_bonus", nullable = false)
    private int armBonus;

    @Column(name = "evasion_bonus", nullable = false)
    private int evasionBonus;

    @Column(name = "compatible_class", length = 255)
    @Enumerated(EnumType.STRING)
    private UnitClass compatibleClass;

    @Column(length = 100)
    private String category;
}
