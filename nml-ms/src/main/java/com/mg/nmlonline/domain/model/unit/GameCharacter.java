package com.mg.nmlonline.domain.model.unit;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("CHARACTER")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class GameCharacter extends CombatEntity {

    @Column(name = "character_name", length = 100, unique = true)
    private String name;

    @Column(name = "base_attack")
    private double baseAttack;

    @Column(name = "base_defense")
    private double baseDefense;

    @Column(name = "base_pdf")
    private double basePdf;

    @Column(name = "base_pdc")
    private double basePdc;

    @Column(name = "base_armor")
    private double baseArmor;

    @Column(name = "base_evasion")
    private double baseEvasion;

    public static final double DEFENSE_REGEN_PER_TURN = 50;


    public GameCharacter(String name, double baseAttack, double basePdf, double basePdc,
                         double baseDefense, double baseArmor, double baseEvasion) {
        this.name = name;
        this.baseAttack = baseAttack;
        this.basePdf = basePdf;
        this.basePdc = basePdc;
        this.baseDefense = baseDefense;
        this.baseArmor = baseArmor;
        this.baseEvasion = baseEvasion;

        this.attack = baseAttack;
        this.pdf = basePdf;
        this.pdc = basePdc;
        this.defense = baseDefense;
        this.armor = baseArmor;
        this.evasion = baseEvasion;
    }

    @Override
    public EntityCategory getEntityCategory() {
        return EntityCategory.CHARACTER;
    }

    @Override
    public void recalculateBaseStats() {
        // Jamais blessé : restauration complète des stats de base.
        this.attack = baseAttack;
        this.pdf = basePdf;
        this.pdc = basePdc;
        this.defense = baseDefense;
        this.armor = baseArmor;
        this.evasion = baseEvasion;
    }

    /** Après bataille : restore l'offense/soak, la défense endommagée n'est PAS soignée (régén fin de tour). */
    public void regenerateAfterBattle() {
        this.attack = baseAttack;
        this.pdf = basePdf;
        this.pdc = basePdc;
        this.armor = baseArmor;
        this.evasion = baseEvasion;
    }

    /** +points de défense, plafonné à baseDefense. */
    public void regenerateDefense(double points) {
        this.defense = Math.min(defense + points, baseDefense);
    }

    @Override
    public double getBaseDefense() {
        return baseDefense;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (");
        buildStatsString(sb);
        sb.append(")");

        return sb.toString();
    }
}

