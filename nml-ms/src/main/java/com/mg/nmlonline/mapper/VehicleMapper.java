package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.VehicleDto;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import org.springframework.stereotype.Component;

/**
 * Mapper pour les véhicules.
 */
@Component
public class VehicleMapper {

    public VehicleDto toDto(Vehicle vehicle) {
        if (vehicle == null) return null;

        VehicleDto dto = new VehicleDto();
        dto.setId(vehicle.getId());
        dto.setPlayerId(vehicle.getPlayerId());
        dto.setVehicleType(vehicle.getVehicleType() != null ? vehicle.getVehicleType().name() : null);
        dto.setDisplayName(vehicle.getDisplayName());
        dto.setPdf(vehicle.getPdf());
        dto.setDefense(vehicle.getDefense());
        dto.setIsDestroyed(vehicle.isDestroyed());
        dto.setSpeed(vehicle.getSpeed());
        dto.setCapacity(vehicle.getCapacity());
        dto.setPassengerCount(vehicle.getPassengerCount());
        dto.setHasPilot(vehicle.hasPilot());
        dto.setSectorNumber(vehicle.getSector() != null ? vehicle.getSector().getNumber() : null);

        return dto;
    }
}
