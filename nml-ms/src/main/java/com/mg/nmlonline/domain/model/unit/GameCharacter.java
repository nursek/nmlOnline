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

    /**
     * Stats de base du personnage (fixes, définies à la création)
     */
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


    /**
     * Constructeur complet avec toutes les stats offensives et défensives.
     */
    public GameCharacter(String name, double baseAttack, double basePdf, double basePdc,
                         double baseDefense, double baseArmor, double baseEvasion) {
        this.name = name;
        this.baseAttack = baseAttack;
        this.basePdf = basePdf;
        this.basePdc = basePdc;
        this.baseDefense = baseDefense;
        this.baseArmor = baseArmor;
        this.baseEvasion = baseEvasion;

        // Initialize inherited combat stats
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
        // No-op for GameCharacter: base stats are fixed and defined at creation.
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

