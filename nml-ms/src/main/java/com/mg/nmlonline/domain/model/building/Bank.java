package com.mg.nmlonline.domain.model.building;

import com.mg.nmlonline.domain.model.resource.PlayerResource;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Banque - Bâtiment économique de stockage de l'argent et des ressources.
 *
 * Caractéristiques :
 * - Stocke 75% de la fortune personnelle et toutes les ressources
 * - Déplaçable une seule fois, à partir du tour 5
 * - Le déplacement empêche le déplacement des autres bâtiments ce tour
 *
 * Si capturée :
 * - L'adversaire s'empare de tout le contenu
 * - Vampirisation progressive des revenus : 15% → 25% → 35% → ... → 75%
 * - Argent et ressources stockés temporairement dans le QG
 */
@Entity
@DiscriminatorValue("BANK")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Bank extends Building {

    // Constantes
    public static final double WEALTH_STORAGE_PERCENTAGE = 0.75; // 75%
    public static final int MIN_TURN_FOR_MOVE = 5;
    public static final double INITIAL_VAMPIRIZE_RATE = 0.15; // 15%
    public static final double VAMPIRIZE_INCREMENT = 0.10; // +10% par tour
    public static final double MAX_VAMPIRIZE_RATE = 0.75; // 75% max

    /**
     * Indique si la banque a déjà été déplacée (déplacement unique).
     */
    @Column(name = "has_moved")
    private boolean hasMoved = false;

    /**
     * Ressources stockées dans la banque.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bank_id")
    private List<PlayerResource> storedResources = new ArrayList<>();

    /**
     * Montant d'argent stocké dans la banque.
     */
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
        return -1; // Déplacement unique
    }

    @Override
    public void recordMove(int currentTurn) {
        super.recordMove(currentTurn);
        this.hasMoved = true;
    }

    /**
     * Calcule la part de la fortune stockée dans la banque.
     *
     * @param totalWealth Fortune totale du joueur
     * @return Montant stocké dans la banque
     */
    public double calculateStoredWealth(double totalWealth) {
        return totalWealth * WEALTH_STORAGE_PERCENTAGE;
    }

    /**
     * Met à jour le montant stocké dans la banque.
     */
    public void updateStoredMoney(double totalWealth) {
        this.storedMoney = calculateStoredWealth(totalWealth);
    }

    /**
     * Calcule le taux de vampirisation actuel basé sur le nombre de tours depuis la capture.
     *
     * @param currentTurn Tour actuel
     * @return Taux de vampirisation (entre 0.15 et 0.75)
     */
    public double getVampirizeRate(int currentTurn) {
        if (!isCaptured() || getCapturedTurn() == null) {
            return 0;
        }

        int turnsSinceCapture = currentTurn - getCapturedTurn();
        double rate = INITIAL_VAMPIRIZE_RATE + (turnsSinceCapture * VAMPIRIZE_INCREMENT);
        return Math.min(rate, MAX_VAMPIRIZE_RATE);
    }

    /**
     * Calcule le montant vampirisé des revenus du joueur.
     *
     * @param playerIncome Revenus du joueur ce tour
     * @param currentTurn Tour actuel
     * @return Montant vampirisé
     */
    public double calculateVampirizedAmount(double playerIncome, int currentTurn) {
        return playerIncome * getVampirizeRate(currentTurn);
    }

    /**
     * Transfère tout le contenu de la banque lors d'une capture.
     *
     * @return Le montant d'argent transféré
     */
    public double transferMoney() {
        double transferred = storedMoney;
        storedMoney = 0;
        return transferred;
    }

    /**
     * Transfère toutes les ressources lors d'une capture.
     *
     * @return La liste des ressources transférées
     */
    public List<PlayerResource> transferResources() {
        List<PlayerResource> transferred = new ArrayList<>(storedResources);
        storedResources.clear();
        return transferred;
    }

    @Override
    public void onCapture(Long capturingPlayerId, int currentTurn) {
        super.onCapture(capturingPlayerId, currentTurn);
        // Le transfert effectif sera géré par le service
    }

    /**
     * Décrit le statut de vampirisation pour l'affichage.
     */
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

        if (!hasMoved) {
            sb.append(" [Déplacement disponible]");
        }

        if (isCaptured() && getCapturedTurn() != null) {
            sb.append(" [Capturée - Vampirisation active]");
        }

        return sb.toString();
    }
}

