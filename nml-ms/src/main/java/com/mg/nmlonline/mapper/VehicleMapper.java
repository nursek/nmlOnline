package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.VehicleDto;
import com.mg.nmlonline.api.dto.VehicleTypeDto;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
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
        if (vehicle.getSector() != null) {
            dto.setSectorNumber(vehicle.getSector().getNumber());
            dto.setBoardId(vehicle.getSector().getBoard() != null ? vehicle.getSector().getBoard().getId() : null);
        }

        return dto;
    }

    public VehicleTypeDto vehicleTypeToDto(VehicleType vt) {
        if (vt == null) return null;

        VehicleTypeDto dto = new VehicleTypeDto();
        dto.setName(vt.name());
        dto.setDisplayName(vt.getDisplayName());
        dto.setCost(vt.getCost());
        dto.setBasePdf(vt.getBasePdf());
        dto.setBaseDefense(vt.getBaseDefense());
        dto.setSpeed(vt.getSpeed());
        dto.setCapacity(vt.getCapacity());
        dto.setResistance(vt.getResistance());
        dto.setFiresInTransit(vt.isFiresInTransit());
        dto.setAerial(vt.isAerial());

        return dto;
    }
}
