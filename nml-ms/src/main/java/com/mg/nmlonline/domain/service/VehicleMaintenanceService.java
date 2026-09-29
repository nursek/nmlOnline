package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fin de tour : +50 défense plafonnée sur les véhicules endommagés, comme les personnages. */
@Service
public class VehicleMaintenanceService {

    private final VehicleRepository vehicleRepository;

    public VehicleMaintenanceService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public void repairAllVehicles() {
        vehicleRepository.findAll().stream()
                .filter(vehicle -> !vehicle.isDestroyed() && vehicle.getDefense() < vehicle.getBaseDefense())
                .forEach(vehicle -> vehicle.regenerateDefense(Vehicle.DEFENSE_REGEN_PER_TURN));
    }
}
