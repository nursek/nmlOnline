package com.mg.nmlonline.domain.model.building;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.EntityCategory;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Classe abstraite représentant un bâtiment du jeu.
 *
 * Les bâtiments sont des entités combattantes avec des règles spéciales :
 * - Stats fixes selon le type
 * - Règles de déplacement spécifiques
 * - Effets spéciaux lors de la capture
 */
@Entity
@DiscriminatorValue("BUILDING")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class Building extends CombatEntity {

    /**
     * Type de bâtiment
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "building_type")
    private BuildingType buildingType;

    /**
     * Tour du dernier déplacement du bâtiment
     */
    @Column(name = "last_moved_turn")
    private Integer lastMovedTurn;

    /**
     * ID du joueur qui a capturé ce bâtiment (null si propriétaire original)
     */
    @Column(name = "captured_by_player_id")
    private Long capturedByPlayerId;

    /**
     * Tour de la capture (pour les effets progressifs comme la Banque)
     */
    @Column(name = "captured_turn")
    private Integer capturedTurn;

    /**
     * Référence bidirectionnelle au joueur propriétaire du bâtiment.
     * Important : JPA gère cette relation via la colonne player_id dans la table BUILDINGS.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false, insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore  // Éviter les boucles infinies lors de la sérialisation JSON
    private Player player;

    /**
     * Constructeur de base pour les bâtiments.
     */
    protected Building(BuildingType buildingType, double baseAttack, double baseDefense) {
        this.buildingType = buildingType;
        this.attack = baseAttack;
        this.defense = baseDefense;
        this.pdf = 0;
        this.pdc = 0;
        this.armor = 0;
        this.evasion = 0;
    }

    @Override
    public EntityCategory getEntityCategory() {
        return EntityCategory.BUILDING;
    }

    @Override
    public void recalculateBaseStats() {
        // Les bâtiments ont des stats fixes.
        // Sauf en cas de destruction où tout passe à 0.
        if (isDestroyed()) {
            this.attack = 0;
            this.defense = 0;
        }
    }

    public abstract boolean canMove(int currentTurn);

    public abstract int getMoveCooldown();

    public void onCapture(Long capturingPlayerId, int currentTurn) {
        this.capturedByPlayerId = capturingPlayerId;
        this.capturedTurn = currentTurn;
    }

    public boolean isCaptured() {
        return capturedByPlayerId != null;
    }

    public void reclaim() {
        this.capturedByPlayerId = null;
        this.capturedTurn = null;
    }

    /**
     * Enregistre un déplacement du bâtiment.
     */
    public void recordMove(int currentTurn) {
        this.lastMovedTurn = currentTurn;
    }

    @Override
    public String getDisplayName() {
        return buildingType != null ? buildingType.getDisplayName() : "Bâtiment";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isDestroyed()) {
            sb.append("[DÉTRUIT] ");
        }

        sb.append(getDisplayName()).append(" (");
        buildStatsString(sb);
        sb.append(")");

        if (isCaptured()) {
            sb.append(" [Capturé]");
        }

        return sb.toString();
    }
}

