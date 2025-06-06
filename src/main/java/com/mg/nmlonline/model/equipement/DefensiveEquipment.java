package com.mg.nmlonline.model.equipement;

import com.mg.nmlonline.model.unit.UnitClass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class DefensiveEquipment extends Equipment {
    private final double armBonus;
    private final double evasionBonus;
    private final Set<UnitClass> compatibleClasses;

    public DefensiveEquipment(String name, int cost, double armBonus, double evasionBonus, 
                             Set<UnitClass> compatibleClasses) {
        super(name, cost);
        this.armBonus = armBonus;
        this.evasionBonus = evasionBonus;
        this.compatibleClasses = compatibleClasses;
    }

    @Override
    public double getPdfBonus() { return 0; }
    @Override
    public double getPdcBonus() { return 0; }
}