package com.mg.nmlonline.model.equipement;

import com.mg.nmlonline.model.unit.UnitClass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class FirearmEquipment extends Equipment {
    private final double pdfBonus;
    private final double pdcBonus;
    private final double armBonus;
    private final Set<UnitClass> compatibleClasses;

    public FirearmEquipment(String name, int cost, double pdfBonus, double pdcBonus, 
                           double armBonus, Set<UnitClass> compatibleClasses) {
        super(name, cost);
        this.pdfBonus = pdfBonus;
        this.pdcBonus = pdcBonus;
        this.armBonus = armBonus;
        this.compatibleClasses = compatibleClasses;
    }

    @Override
    public double getEvasionBonus() { return 0; }
}