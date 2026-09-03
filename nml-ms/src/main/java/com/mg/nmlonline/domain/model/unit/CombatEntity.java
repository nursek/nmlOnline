package com.mg.nmlonline.domain.model.unit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mg.nmlonline.domain.model.sector.Sector;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entité combattante (unité/personnage/bâtiment) — héritage JPA SINGLE_TABLE avec discriminateur. */
@Entity
@Table(name = "COMBAT_ENTITIES")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entity_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
public abstract class CombatEntity {

    protected static final double INJURED_STAT_MULTIPLIER = 0.5;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "combat_entity_seq")
    @SequenceGenerator(name = "combat_entity_seq", sequenceName = "combat_entities_id_seq", allocationSize = 50)
    protected Long id;

    @Column(name = "player_id")
    protected Long playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "board_id", referencedColumnName = "board_id"),
            @JoinColumn(name = "sector_number", referencedColumnName = "number")
    })
    @JsonIgnore
    protected Sector sector;

    @Column(nullable = false)
    protected double attack;

    @Column(nullable = false)
    protected double defense;

    @Column(nullable = false)
    protected double pdf = 0;

    @Column(nullable = false)
    protected double pdc = 0;

    @Column(nullable = false)
    protected double armor = 0;

    @Column(nullable = false)
    protected double evasion = 0;

    @Column(name = "is_destroyed", nullable = false)
    protected boolean isDestroyed = false;

    @Column(name = "is_injured", nullable = true)
    protected boolean isInjured = false;

    public abstract EntityCategory getEntityCategory();

    public abstract void recalculateBaseStats();

    public abstract String getDisplayName();

    public abstract double getBaseDefense();

    public double getDamageReduction(String damageType) {
        return 0.0;
    }

    public double getTotalAttack() {
        return attack + pdf + pdc;
    }

    public double getTotalDefense() {
        return defense + armor;
    }


    public boolean canFight() {
        return !isDestroyed;
    }

    protected String formatStat(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.2f", value).replaceAll("0+$", "").replaceAll(",$", "");
        }
    }

    protected void buildStatsString(StringBuilder sb) {
        sb.append(formatStat(attack)).append(" Atk");
        if (pdf > 0) sb.append(" + ").append(formatStat(pdf)).append(" Pdf");
        if (pdc > 0) sb.append(" + ").append(formatStat(pdc)).append(" Pdc");
        sb.append(" / ").append(formatStat(defense)).append(" Def");
        if (armor > 0) sb.append(" + ").append(formatStat(armor)).append(" Arm");
        if (evasion > 0) sb.append(". Esquive : ").append((int) Math.ceil(evasion)).append(" %");
    }
}

