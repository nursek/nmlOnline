package com.mg.nmlonline.model.equipement;

import com.mg.nmlonline.model.unit.UnitClass;
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
}