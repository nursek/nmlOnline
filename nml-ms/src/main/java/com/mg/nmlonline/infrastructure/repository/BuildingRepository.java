package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les bâtiments (QG, Cache d'armes, Banque).
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    /**
     * Trouve tous les bâtiments d'un joueur.
     */
    List<Building> findByPlayerId(Long playerId);

    /**
     * Trouve un bâtiment par type pour un joueur.
     */
    Optional<Building> findByPlayerIdAndBuildingType(Long playerId, BuildingType buildingType);

    /**
     * Trouve tous les bâtiments d'un type donné.
     */
    List<Building> findByBuildingType(BuildingType buildingType);

    /**
     * Trouve tous les bâtiments capturés par un joueur.
     */
    List<Building> findByCapturedByPlayerId(Long capturedByPlayerId);

    /**
     * Trouve tous les bâtiments non détruits d'un joueur.
     */
    List<Building> findByPlayerIdAndIsDestroyedFalse(Long playerId);

    /**
     * Vérifie si un joueur a un QG opérationnel.
     */
    @Query("SELECT COUNT(b) > 0 FROM Building b WHERE b.playerId = :playerId " +
           "AND b.buildingType = 'HEADQUARTERS' AND b.isDestroyed = false " +
           "AND TYPE(b) = Headquarters AND CAST(b AS Headquarters).isOperational = true")
    boolean hasOperationalHeadquarters(@Param("playerId") Long playerId);

    /**
     * Trouve le QG d'un joueur.
     */
    @Query("SELECT b FROM Building b WHERE b.playerId = :playerId AND b.buildingType = 'HEADQUARTERS'")
    Optional<Building> findHeadquartersByPlayerId(@Param("playerId") Long playerId);

    /**
     * Trouve la banque d'un joueur.
     */
    @Query("SELECT b FROM Building b WHERE b.playerId = :playerId AND b.buildingType = 'BANK'")
    Optional<Building> findBankByPlayerId(@Param("playerId") Long playerId);

    /**
     * Trouve toutes les caches d'armes d'un joueur.
     */
    @Query("SELECT b FROM Building b WHERE b.playerId = :playerId AND b.buildingType = 'WEAPON_CACHE'")
    List<Building> findWeaponCachesByPlayerId(@Param("playerId") Long playerId);
}

