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

    @Enumerated(EnumType.STRING)
    @Column(name = "compatible_class")
    private UnitClass compatibleClass;

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
        // On prend la première classe compatible (compatibilité avec l'ancien code)
        this.compatibleClass = compatibleClasses != null && !compatibleClasses.isEmpty()
                ? compatibleClasses.iterator().next() : null;
        this.category = category;
    }

    /**
     * Retourne les classes compatibles sous forme de Set (compatibilité)
     */
    public Set<UnitClass> getCompatibleClasses() {
        if (compatibleClass == null) {
            return new HashSet<>();
        }
        return Set.of(compatibleClass);
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