package com.mg.nmlonline.domain.model.building;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.CombatEntity;
import com.mg.nmlonline.domain.model.unit.EntityCategory;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("BUILDING")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class Building extends CombatEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "building_type")
    private BuildingType buildingType;

    @Column(name = "last_moved_turn")
    private Integer lastMovedTurn;

    @Column(name = "captured_by_player_id")
    private Long capturedByPlayerId;

    @Column(name = "captured_turn")
    private Integer capturedTurn;

    // Côté inverse de Player.buildings : insertable=false/updatable=false, la FK player_id est gérée par le propriétaire.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false, insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Player player;

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
        // PV régénérés depuis le type (annule le reassign-zéro post-bataille) ; 0 si détruit —
        // pas de reconstruction pour les bâtiments secondaires.
        if (isDestroyed() || buildingType == null) {
            this.attack = 0;
            this.defense = 0;
        } else {
            this.attack = buildingType.getBaseAttack();
            this.defense = buildingType.getBaseDefense();
        }
    }

    @Override
    public double getBaseDefense() {
        return buildingType != null ? buildingType.getBaseDefense() : 0;
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

