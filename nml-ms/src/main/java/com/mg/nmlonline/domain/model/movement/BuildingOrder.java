package com.mg.nmlonline.domain.model.movement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ordre de déplacement d'un bâtiment — règles distinctes de MovementOrder : départ et arrivée au joueur, 1 hop, cooldown. */
@Entity
@Table(name = "BUILDING_ORDERS")
@Getter
@Setter
@NoArgsConstructor
public class BuildingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "building_order_seq")
    @SequenceGenerator(name = "building_order_seq", sequenceName = "building_orders_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private int turn;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "from_sector", nullable = false)
    private int fromSectorNumber;

    @Column(name = "to_sector", nullable = false)
    private int toSectorNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementStatus status = MovementStatus.PENDING;

    @Column(name = "status_message", length = 500)
    private String statusMessage;

    public static BuildingOrder create(Long playerId, int turn, Long buildingId, int from, int to) {
        BuildingOrder order = new BuildingOrder();
        order.setPlayerId(playerId);
        order.setTurn(turn);
        order.setBuildingId(buildingId);
        order.setFromSectorNumber(from);
        order.setToSectorNumber(to);
        return order;
    }

    public boolean isNotPending() {
        return status != MovementStatus.PENDING;
    }

    public void resolve() {
        this.status = MovementStatus.RESOLVED;
    }

    public void block(String reason) {
        this.status = MovementStatus.BLOCKED;
        this.statusMessage = reason;
    }

    public void cancel() {
        this.status = MovementStatus.CANCELLED;
    }

    @Override
    public String toString() {
        return "BuildingOrder{id=" + id
                + ", player=" + playerId
                + ", turn=" + turn
                + ", building=" + buildingId
                + ", " + fromSectorNumber + "→" + toSectorNumber
                + ", status=" + status + "}";
    }
}
