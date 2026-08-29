package com.mg.nmlonline.domain.model.movement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Ordre de déplacement d'un tour — à pied (groupe d'unités/personnages) ou en véhicule (porte ses passagers). Les bâtiments utilisent BuildingOrder (règles distinctes). */
@Entity
@Table(name = "MOVEMENT_ORDERS")
@Getter
@Setter
@NoArgsConstructor
public class MovementOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "movement_order_seq")
    @SequenceGenerator(name = "movement_order_seq", sequenceName = "movement_orders_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private int turn;

    @Column(name = "from_sector", nullable = false)
    private int fromSectorNumber;

    @Column(name = "to_sector", nullable = false)
    private int toSectorNumber;

    /** Route complète [départ, …, arrivée] : à pied 1 hop ([from,to]) ou 2 hops léger ([from,mid,to]) ; véhicule limité par sa vitesse. */
    @ElementCollection
    @CollectionTable(name = "MOVEMENT_ORDER_ROUTE",
            joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "sector_number")
    @OrderColumn(name = "route_index")
    private List<Integer> route = new ArrayList<>();

    /** IDs des entités déplacées à pied (unités + personnages) ; vide si l'ordre concerne un véhicule. */
    @ElementCollection
    @CollectionTable(name = "MOVEMENT_ORDER_ENTITIES",
            joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "entity_id")
    private List<Long> entityIds = new ArrayList<>();

    /** ID du véhicule déplacé (null si à pied) — les passagers sont portés par le véhicule, non listés dans entityIds. */
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementStatus status = MovementStatus.PENDING;

    @Column(name = "status_message", length = 500)
    private String statusMessage;

    public static MovementOrder createFootOrder(Long playerId, int turn, List<Long> entityIds, List<Integer> route) {
        if (route == null || route.size() < 2) {
            throw new IllegalArgumentException("La route doit contenir au moins 2 secteurs (départ et arrivée).");
        }
        MovementOrder order = new MovementOrder();
        order.setPlayerId(playerId);
        order.setTurn(turn);
        order.setFromSectorNumber(route.getFirst());
        order.setToSectorNumber(route.getLast());
        order.setRoute(new ArrayList<>(route));
        order.setEntityIds(new ArrayList<>(entityIds));
        order.setStatus(MovementStatus.PENDING);
        return order;
    }

    public static MovementOrder createVehicleOrder(Long playerId, int turn, Long vehicleId, List<Integer> route) {
        if (route == null || route.size() < 2) {
            throw new IllegalArgumentException("La route doit contenir au moins 2 secteurs (départ et arrivée).");
        }
        MovementOrder order = new MovementOrder();
        order.setPlayerId(playerId);
        order.setTurn(turn);
        order.setVehicleId(vehicleId);
        order.setFromSectorNumber(route.getFirst());
        order.setToSectorNumber(route.getLast());
        order.setRoute(new ArrayList<>(route));
        order.setStatus(MovementStatus.PENDING);
        return order;
    }

    public boolean isVehicleMovement() {
        return vehicleId != null;
    }

    public boolean isFootMovement() {
        return vehicleId == null;
    }

    public int getHopCount() {
        return route.size() - 1;
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
        StringBuilder sb = new StringBuilder("MovementOrder{");
        sb.append("id=").append(id);
        sb.append(", player=").append(playerId);
        sb.append(", turn=").append(turn);
        sb.append(", route=").append(route);
        if (isVehicleMovement()) {
            sb.append(", vehicle=").append(vehicleId);
        } else {
            sb.append(", entities=").append(entityIds.size()).append(" entité(s)");
        }
        sb.append(", status=").append(status);
        sb.append("}");
        return sb.toString();
    }
}

