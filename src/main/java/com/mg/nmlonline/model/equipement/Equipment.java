package com.mg.nmlonline.model.equipement;

import com.mg.nmlonline.model.unit.UnitClass;
import lombok.Data;
import java.util.Set;

// Classe abstraite pour les équipements
@Data
public abstract class Equipment {
    protected final String name;
    protected final int cost;

    protected Equipment(String name, int cost) {
        this.name = name;
        this.cost = cost;
    }

    public abstract double getPdfBonus();
    public abstract double getPdcBonus();
    public abstract double getArmBonus();
    public abstract double getEvasionBonus();
    public abstract Set<UnitClass> getCompatibleClasses();
}