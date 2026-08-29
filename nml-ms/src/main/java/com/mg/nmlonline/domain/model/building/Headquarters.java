package com.mg.nmlonline.domain.model.building;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("HEADQUARTERS")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Headquarters extends Building {

    public static final double DEFAULT_WEALTH_STORAGE_PERCENTAGE = 0.25;
    public static final int DEFAULT_MOVE_COOLDOWN = 5;

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

    public double getStoredWealth(double totalWealth) {
        return totalWealth * DEFAULT_WEALTH_STORAGE_PERCENTAGE;
    }

    public void destroy() {
        setDestroyed(true);
        this.isOperational = false;
        recalculateBaseStats();
    }

    public void reconstructSameLocation() {
        setDestroyed(false);
        this.isOperational = true;
        this.attack = BuildingType.HEADQUARTERS.getBaseAttack();
        this.defense = BuildingType.HEADQUARTERS.getBaseDefense();
    }

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

