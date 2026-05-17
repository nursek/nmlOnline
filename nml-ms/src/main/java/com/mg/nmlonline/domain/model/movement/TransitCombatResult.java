package com.mg.nmlonline.domain.model.movement;

/**
 * Résultat d'un combat de transit (véhicule traversant un secteur ennemi).
 */
public record TransitCombatResult(int sectorNumber, Long vehicleId, boolean vehicleFired) {}
