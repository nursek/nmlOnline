package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les véhicules.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * Tous les véhicules d'un joueur.
     */
    List<Vehicle> findByPlayerId(Long playerId);

    /**
     * Tous les véhicules d'un joueur par type.
     */
    List<Vehicle> findByPlayerIdAndVehicleType(Long playerId, VehicleType vehicleType);

    /**
     * Tous les véhicules non détruits d'un joueur.
     */
    List<Vehicle> findByPlayerIdAndIsDestroyedFalse(Long playerId);
}
