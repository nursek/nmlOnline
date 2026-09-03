package com.mg.nmlonline.domain.model.building;

import com.mg.nmlonline.domain.model.resource.PlayerResource;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("BANK")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Bank extends Building {

    public static final double WEALTH_STORAGE_PERCENTAGE = 0.75;
    public static final int MIN_TURN_FOR_MOVE = 5;
    public static final double INITIAL_VAMPIRIZE_RATE = 0.15;
    public static final double VAMPIRIZE_INCREMENT = 0.10;
    public static final double MAX_VAMPIRIZE_RATE = 0.75;

    @Column(name = "has_moved")
    private boolean hasMoved = false;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bank_id")
    private List<PlayerResource> storedResources = new ArrayList<>();

    @Column(name = "stored_money")
    private double storedMoney = 0;

    public Bank(Long playerId) {
        super(BuildingType.BANK,
              BuildingType.BANK.getBaseAttack(),
              BuildingType.BANK.getBaseDefense());
        setPlayerId(playerId);
    }

    @Override
    public boolean canMove(int currentTurn) {
        if (isDestroyed() || hasMoved) {
            return false;
        }
        return currentTurn >= MIN_TURN_FOR_MOVE;
    }

    @Override
    public int getMoveCooldown() {
        return -1; // déplacement unique
    }

    @Override
    public void recordMove(int currentTurn) {
        super.recordMove(currentTurn);
        this.hasMoved = true;
    }

    public double calculateStoredWealth(double totalWealth) {
        return totalWealth * WEALTH_STORAGE_PERCENTAGE;
    }

    public void updateStoredMoney(double totalWealth) {
        this.storedMoney = calculateStoredWealth(totalWealth);
    }

    public double getVampirizeRate(int currentTurn) {
        if (!isCaptured() || getCapturedTurn() == null) {
            return 0;
        }

        int turnsSinceCapture = currentTurn - getCapturedTurn();
        double rate = INITIAL_VAMPIRIZE_RATE + (turnsSinceCapture * VAMPIRIZE_INCREMENT);
        return Math.min(rate, MAX_VAMPIRIZE_RATE);
    }

    public double calculateVampirizedAmount(double playerIncome, int currentTurn) {
        return playerIncome * getVampirizeRate(currentTurn);
    }

    public double transferMoney() {
        double transferred = storedMoney;
        storedMoney = 0;
        return transferred;
    }

    public List<PlayerResource> transferResources() {
        List<PlayerResource> transferred = new ArrayList<>(storedResources);
        storedResources.clear();
        return transferred;
    }

    public String getVampirizeStatus(int currentTurn) {
        if (!isCaptured()) {
            return "Aucune vampirisation";
        }
        double rate = getVampirizeRate(currentTurn);
        return String.format("Vampirisation : %.0f%% des revenus", rate * 100);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isDestroyed()) {
            sb.append("[DÉTRUIT] ");
        }

        sb.append("Banque (");
        buildStatsString(sb);
        sb.append(")");

        if (isCaptured() && getCapturedTurn() != null) {
            sb.append(" [Capturée - Vampirisation active]");
        }

        return sb.toString();
    }
}

