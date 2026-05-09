package com.mg.nmlonline.domain.model.movement;

/**
 * Conflit à la destination (entités de joueurs différents dans le même secteur).
 */
public record DestinationConflict(int sectorNumber, Long attackerPlayerId, Long defenderPlayerId) {}
