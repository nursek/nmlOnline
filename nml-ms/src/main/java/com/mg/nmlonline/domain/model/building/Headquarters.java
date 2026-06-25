package com.mg.nmlonline.domain.model.building;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Quartier Général - Le centre névralgique de l'empire du joueur.
 *
 * Caractéristiques :
 * - Stats : 100 Atk / 200 Def
 * - Stocke 25% de la fortune personnelle
 * - Déplaçable tous les 5 tours
 * - Perte = défaite immédiate
 * - Si capturé, l'adversaire récupère tous les Quartiers
 * - Si détruit sans être pris, l'armée est immobilisée
 *
 * Reconstruction :
 * - Sur place : 75 000 $
 * - Dans un autre Quartier : 150 000 $
 */
@Entity
@DiscriminatorValue("HEADQUARTERS")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Headquarters extends Building {

    public static final double DEFAULT_WEALTH_STORAGE_PERCENTAGE = 0.25;
    public static final int DEFAULT_MOVE_COOLDOWN = 5;

    /**
     * Indique si le QG est actuellement opérationnel.
     * Un QG détruit mais non capturé immobilise l'armée.
     */
    @Column(name = "is_operational")
    private boolean isOperational = true;

    public Headquarters(Long playerId) {
        super(BuildingType.HEADQUARTERS,
              BuildingType.HEADQUARTERS.getBaseAttack(),
              BuildingType.HEADQUARTERS.getBaseDefense());
        setPlayerId(playerId);
    }

    @Override
    public boolean canMove(int currentTurn) {
        return canMove(currentTurn, DEFAULT_MOVE_COOLDOWN);
    }

    public boolean canMove(int currentTurn, int moveCooldown) {
        if (!isOperational || isDestroyed()) {
            return false;
        }
        if (getLastMovedTurn() == null) {
            return true;
        }
        return currentTurn - getLastMovedTurn() >= moveCooldown;
    }

    @Override
    public int getMoveCooldown() {
        return DEFAULT_MOVE_COOLDOWN;
    }

    /**
     * Calcule la part de la fortune stockée dans le QG.
     *
     * @param totalWealth Fortune totale du joueur
     * @return Montant stocké dans le QG
     */
    public double getStoredWealth(double totalWealth) {
        return totalWealth * DEFAULT_WEALTH_STORAGE_PERCENTAGE;
    }

    /**
     * Détruit le QG sans le capturer (l'armée est immobilisée).
     */
    public void destroy() {
        setDestroyed(true);
        this.isOperational = false;
        recalculateBaseStats();
    }

    /**
     * Reconstruit le QG sur place.
     * La vérification du coût est faite par le service.
     */
    public void reconstructSameLocation() {
        setDestroyed(false);
        this.isOperational = true;
        this.attack = BuildingType.HEADQUARTERS.getBaseAttack();
        this.defense = BuildingType.HEADQUARTERS.getBaseDefense();
    }

    /**
     * Vérifie si l'armée du joueur est immobilisée (QG détruit non reconstruit).
     */
    public boolean isArmyImmobilized() {
        return isDestroyed() && !isCaptured();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isDestroyed()) {
            sb.append("[DÉTRUIT] ");
        } else if (!isOperational) {
            sb.append("[HORS SERVICE] ");
        }

        sb.append("Quartier Général (");
        buildStatsString(sb);
        sb.append(")");

        if (isCaptured()) {
            sb.append(" [Capturé - Défaite]");
        }

        return sb.toString();
    }
}

