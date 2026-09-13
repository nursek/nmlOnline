package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByPlayerId(Long playerId);

    List<Vehicle> findByPlayerIdAndVehicleType(Long playerId, VehicleType vehicleType);

    List<Vehicle> findByPlayerIdAndIsDestroyedFalse(Long playerId);

    // Pessimistic-write : sérialise pilote et ordres d'un même véhicule (check-then-act).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);

    Optional<Vehicle> findByPilot_Id(Long pilotId);

    Optional<Vehicle> findByPassengers_Id(Long passengerId);

    boolean existsByPilot_Id(Long pilotId);

    boolean existsByPassengers_Id(Long passengerId);
}
