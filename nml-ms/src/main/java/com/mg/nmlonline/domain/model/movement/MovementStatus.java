package com.mg.nmlonline.domain.model.movement;

/**
 * Statut d'un ordre de déplacement.
 */
public enum MovementStatus {
    /** Ordre en attente de résolution (fin de tour). */
    PENDING,

    /** Ordre résolu avec succès — les entités ont été déplacées. */
    RESOLVED,

    /** Ordre bloqué — route invalide ou conditions non remplies. */
    BLOCKED,

    /** Ordre annulé par le joueur avant la résolution. */
    CANCELLED
}
