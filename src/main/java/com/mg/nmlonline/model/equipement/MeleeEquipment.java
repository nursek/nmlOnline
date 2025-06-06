package com.mg.nmlonline.model.equipement;

import com.mg.nmlonline.model.unit.UnitClass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class MeleeEquipment extends Equipment {
    private final double pdcBonus;
    private final Set<UnitClass> compatibleClasses;

    public MeleeEquipment(String name, int cost, double pdcBonus, Set<UnitClass> compatibleClasses) {
        super(name, cost);
        this.pdcBonus = pdcBonus;
        this.compatibleClasses = compatibleClasses;
    }

    @Override
    public double getPdfBonus() { return 0; }
    @Override
    public double getArmBonus() { return 0; }
    @Override
    public double getEvasionBonus() { return 0; }
}