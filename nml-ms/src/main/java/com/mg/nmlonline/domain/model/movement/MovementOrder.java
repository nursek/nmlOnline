package com.mg.nmlonline.domain.model.movement;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordre de déplacement donné par un joueur pour un tour.
 *
 * <p>Couvre deux cas d'usage :</p>
 * <ul>
 *   <li><strong>À pied</strong> : groupe d'unités et/ou personnages (même route).
 *       Une unité standard parcourt 1 secteur/tour ; une unité légère (L) peut en parcourir 2.
 *       La route contient [départ, …, arrivée] avec autant d'étapes que nécessaire.</li>
 *   <li><strong>Véhicule</strong> : le véhicule (+ passagers embarqués) suit une route multi-hop
 *       limitée par sa vitesse ({@link com.mg.nmlonline.domain.model.vehicle.VehicleType#getSpeed()}).</li>
 * </ul>
 *
 * <p>Les bâtiments ont leur propre type d'ordre ({@link BuildingOrder}) car ils obéissent
 * à des règles différentes (territoires alliés uniquement, cooldown, etc.).</p>
 *
 * <p>Les ordres sont collectés pendant le tour et résolus simultanément en fin de tour.</p>
 */
@Entity
@Table(name = "MOVEMENT_ORDERS")
@Data
@NoArgsConstructor
public class MovementOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Joueur qui a donné l'ordre. */
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    /** Tour auquel l'ordre a été donné. */
    @Column(nullable = false)
    private int turn;

    /** Secteur de départ (= route.get(0)). */
    @Column(name = "from_sector", nullable = false)
    private int fromSectorNumber;

    /** Secteur de destination finale (= route.getLast()). */
    @Column(name = "to_sector", nullable = false)
    private int toSectorNumber;

    /**
     * Route complète : liste ordonnée de numéros de secteurs traversés, départ et arrivée inclus.
     * <ul>
     *   <li>À pied standard : {@code [from, to]} (1 hop).</li>
     *   <li>À pied léger : {@code [from, intermédiaire, to]} (2 hops max).</li>
     *   <li>Véhicule : autant de secteurs que sa vitesse le permet.</li>
     * </ul>
     * Toujours au moins 2 éléments.
     */
    @ElementCollection
    @CollectionTable(name = "MOVEMENT_ORDER_ROUTE",
            joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "sector_number")
    @OrderColumn(name = "route_index")
    private List<Integer> route = new ArrayList<>();

    /**
     * IDs des entités combattantes déplacées à pied (unités + personnages).
     * Vide si l'ordre concerne un véhicule.
     */
    @ElementCollection
    @CollectionTable(name = "MOVEMENT_ORDER_ENTITIES",
            joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "entity_id")
    private List<Long> entityIds = new ArrayList<>();

    /**
     * ID du véhicule déplacé (null si déplacement à pied).
     * Le véhicule porte ses passagers — pas besoin de les lister dans entityIds.
     */
    @Column(name = "vehicle_id")
    private Long vehicleId;

    /** Statut de l'ordre. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementStatus status = MovementStatus.PENDING;

    /** Message d'erreur ou d'info si l'ordre est bloqué/annulé. */
    @Column(name = "status_message", length = 500)
    private String statusMessage;

    // === CONSTRUCTEURS UTILITAIRES ===

    /**
     * Crée un ordre de déplacement à pied pour un groupe d'entités.
     *
     * @param playerId  ID du joueur
     * @param turn      Tour courant
     * @param entityIds IDs des entités déplacées (toutes doivent supporter la longueur de la route)
     * @param route     Route complète — {@code [from, to]} pour 1 hop, {@code [from, mid, to]} pour 2 hops (LEGER)
     */
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

    /**
     * Crée un ordre de déplacement en véhicule.
     * La route inclut le secteur de départ et d'arrivée.
     */
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

    // === MÉTHODES UTILITAIRES ===

    /** Vérifie si c'est un mouvement en véhicule. */
    public boolean isVehicleMovement() {
        return vehicleId != null;
    }

    /** Vérifie si c'est un mouvement à pied (groupe d'unités/personnages). */
    public boolean isFootMovement() {
        return vehicleId == null;
    }

    /** Nombre de secteurs traversés (hors départ). */
    public int getHopCount() {
        return route.size() - 1;
    }

    /** Vérifie si l'ordre est encore en attente de résolution. */
    public boolean isNotPending() {
        return status != MovementStatus.PENDING;
    }

    /** Marque l'ordre comme résolu. */
    public void resolve() {
        this.status = MovementStatus.RESOLVED;
    }

    /** Marque l'ordre comme bloqué avec un message explicatif. */
    public void block(String reason) {
        this.status = MovementStatus.BLOCKED;
        this.statusMessage = reason;
    }

    /** Annule l'ordre. */
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

