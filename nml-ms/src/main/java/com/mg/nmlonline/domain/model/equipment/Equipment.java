package com.mg.nmlonline.domain.model.equipment;

import com.mg.nmlonline.domain.model.unit.UnitClass;
import lombok.Data;
import java.util.Set;

// Classe abstraite pour les équipements
@Data
public class Equipment {
    protected final String name;
    protected final int cost;
    private final double pdfBonus;
    private final double pdcBonus;
    private final double armBonus;
    private final double evasionBonus;
    private final Set<UnitClass> compatibleClasses;
    private final EquipmentCategory category;

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