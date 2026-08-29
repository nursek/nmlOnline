package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    Optional<Building> findByPlayerIdAndBuildingType(Long playerId, BuildingType buildingType);

    List<Building> findByCapturedByPlayerId(Long capturedByPlayerId);

    List<Building> findByPlayerIdAndIsDestroyedFalse(Long playerId);

    List<Building> findByPlayerIdAndBuildingTypeAndIsDestroyedFalse(Long playerId, BuildingType buildingType);
}

