package com.mg.nmlonline.domain.model.movement;

public record TransitCombatResult(int sectorNumber, Long vehicleId, boolean vehicleFired,
                                  String vehicleName, String vehicleType, Long ownerPlayerId) {
}
