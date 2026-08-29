package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByPlayerId(Long playerId);

    List<Vehicle> findByPlayerIdAndVehicleType(Long playerId, VehicleType vehicleType);

    List<Vehicle> findByPlayerIdAndIsDestroyedFalse(Long playerId);
}
