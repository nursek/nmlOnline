package com.mg.nmlonline.domain.model.unit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mg.nmlonline.domain.model.sector.Sector;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Classe abstraite représentant une entité combattante (unité, personnage ou bâtiment).
 * Utilise l'héritage JPA SINGLE_TABLE avec discriminateur.
 */
@Entity
@Table(name = "COMBAT_ENTITIES")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entity_type", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor
public abstract class CombatEntity {

    // ===== CONSTANTES =====
    protected static final double INJURED_STAT_MULTIPLIER = 0.5;

    // ===== IDENTIFIANT UNIQUE =====
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    // ID du joueur propriétaire
    @Column(name = "player_id")
    protected Long playerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "board_id", referencedColumnName = "board_id"),
            @JoinColumn(name = "sector_number", referencedColumnName = "number")
    })
    @JsonIgnore
    protected Sector sector;

    // ===== STATISTIQUES DE BASE =====
    @Column(nullable = false)
    protected double attack;

    @Column(nullable = false)
    protected double defense;

    // ===== STATISTIQUES CALCULÉES =====
    @Column(nullable = false)
    protected double pdf = 0;

    @Column(nullable = false)
    protected double pdc = 0;

    @Column(nullable = false)
    protected double armor = 0;

    @Column(nullable = false)
    protected double evasion = 0;

    // ===== ÉTAT =====
    @Column(name = "is_destroyed", nullable = false)
    protected boolean isDestroyed = false;

    // ===== MÉTHODES ABSTRAITES =====

    /**
     * Retourne le type d'entité pour l'affichage et la logique métier.
     */
    public abstract EntityCategory getEntityCategory();

    /**
     * Recalcule les statistiques de base de l'entité.
     */
    public abstract void recalculateBaseStats();

    /**
     * Retourne le nom d'affichage de l'entité.
     */
    public abstract String getDisplayName();

    // ===== MÉTHODES COMMUNES =====

    public double getTotalAttack() {
        return attack + pdf + pdc;
    }

    public double getTotalDefense() {
        return defense + armor;
    }


    /**
     * Vérifie si cette entité peut participer au combat.
     */
    public boolean canFight() {
        return !isDestroyed;
    }

    /**
     * Formate une statistique pour l'affichage.
     */
    protected String formatStat(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.2f", value).replaceAll("0+$", "").replaceAll(",$", "");
        }
    }

    /**
     * Construit la chaîne de statistiques pour l'affichage.
     */
    protected void buildStatsString(StringBuilder sb) {
        sb.append(formatStat(attack)).append(" Atk");
        if (pdf > 0) sb.append(" + ").append(formatStat(pdf)).append(" Pdf");
        if (pdc > 0) sb.append(" + ").append(formatStat(pdc)).append(" Pdc");
        sb.append(" / ").append(formatStat(defense)).append(" Def");
        if (armor > 0) sb.append(" + ").append(formatStat(armor)).append(" Arm");
        if (evasion > 0) sb.append(". Esquive : ").append((int) Math.ceil(evasion)).append(" %");
    }
}

