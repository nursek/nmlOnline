package com.mg.nmlonline.domain.model.movement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ordre de déplacement d'un bâtiment donné par un joueur pour un tour.
 *
 * <p>Les bâtiments obéissent à des règles strictement différentes des unités :</p>
 * <ul>
 *   <li>Le secteur de départ <strong>et</strong> de destination doivent appartenir au joueur.</li>
 *   <li>Les deux secteurs doivent être adjacents (1 hop uniquement).</li>
 *   <li>Un cooldown empêche de déplacer le même bâtiment deux tours de suite.</li>
 * </ul>
 *
 * <p>Ces ordres sont résolus séparément des ordres de mouvement d'unités
 * ({@link MovementOrder}) car ils ne génèrent pas de conflits inter-joueurs.</p>
 */
@Entity
@Table(name = "BUILDING_ORDERS")
@Getter
@Setter
@NoArgsConstructor
public class BuildingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Joueur propriétaire du bâtiment. */
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    /** Tour auquel l'ordre a été donné. */
    @Column(nullable = false)
    private int turn;

    /** ID du bâtiment à déplacer. */
    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    /** Secteur de départ (doit appartenir au joueur). */
    @Column(name = "from_sector", nullable = false)
    private int fromSectorNumber;

    /** Secteur de destination (doit appartenir au joueur, adjacent au départ). */
    @Column(name = "to_sector", nullable = false)
    private int toSectorNumber;

    /** Statut de l'ordre. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementStatus status = MovementStatus.PENDING;

    /** Message d'erreur si l'ordre est bloqué. */
    @Column(name = "status_message", length = 500)
    private String statusMessage;

    // === CONSTRUCTEUR UTILITAIRE ===

    public static BuildingOrder create(Long playerId, int turn, Long buildingId, int from, int to) {
        BuildingOrder order = new BuildingOrder();
        order.setPlayerId(playerId);
        order.setTurn(turn);
        order.setBuildingId(buildingId);
        order.setFromSectorNumber(from);
        order.setToSectorNumber(to);
        return order;
    }

    // === MÉTHODES UTILITAIRES ===

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
        return "BuildingOrder{id=" + id
                + ", player=" + playerId
                + ", turn=" + turn
                + ", building=" + buildingId
                + ", " + fromSectorNumber + "→" + toSectorNumber
                + ", status=" + status + "}";
    }
}
