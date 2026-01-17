package com.mg.nmlonline.domain.model.equipment;

import com.mg.nmlonline.domain.model.unit.UnitClass;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Classe représentant un équipement - Entité JPA
 */
@Entity
@Table(name = "EQUIPMENT")
@Data
@NoArgsConstructor
public class Equipment {

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

    @ElementCollection(targetClass = UnitClass.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "EQUIPMENT_COMPATIBLE_CLASSES", joinColumns = @JoinColumn(name = "equipment_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_class")
    private Set<UnitClass> compatibleClasses = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentCategory category;

    // Constructeur complet pour la création programmatique
    public Equipment(String name, int cost, double pdfBonus, double pdcBonus,
                     double armBonus, double evasionBonus, Set<UnitClass> compatibleClasses,
                     EquipmentCategory category) {
        this.name = name;
        this.cost = cost;
        this.pdfBonus = pdfBonus;
        this.pdcBonus = pdcBonus;
        this.armBonus = armBonus;
        this.evasionBonus = evasionBonus;
        this.compatibleClasses = compatibleClasses != null ? new HashSet<>(compatibleClasses) : new HashSet<>();
        this.category = category;
    }


    @Override
    public String toString() {
        StringBuilder stats = new StringBuilder();
        if (pdfBonus != 0) stats.append(formatPercent(pdfBonus)).append("Pdf").append(" ; ");
        if (pdcBonus != 0) stats.append(formatPercent(pdcBonus)).append("Pdc").append(" ; ");
        if (armBonus != 0) stats.append(formatPercent(armBonus)).append("Arm").append(" ; ");
        if (evasionBonus != 0) stats.append(formatPercent(evasionBonus)).append("Esquive").append(" ; ");
        if (!stats.isEmpty()) {
            stats.setLength(stats.length() - 3);
            return name + " (" + stats + ").";
        }
        return name;
    }

    private String formatPercent(double value) {
        String sign = value > 0 ? "+" : "-";
        double absValue = Math.abs(value);
        if (absValue == (int) absValue) {
            return String.format("%s%d%% ", sign, (int) absValue);
        } else {
            return String.format("%s%.1f%% ", sign, absValue);
        }
    }
}