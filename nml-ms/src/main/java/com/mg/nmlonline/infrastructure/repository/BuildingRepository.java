package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour les bâtiments (QG, Cache d'armes, Banque).
 *
 * Toutes les requêtes utilisent les méthodes dérivées Spring Data JPA avec les constantes
 * enum {@link BuildingType} pour éviter les chaînes littérales fragiles en JPQL.
 * La logique métier (ex: état opérationnel du QG) est filtrée côté service en Java.
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    /**
     * Trouve un bâtiment par type pour un joueur.
     * Ex: findByPlayerIdAndBuildingType(playerId, BuildingType.HEADQUARTERS)
     */
    Optional<Building> findByPlayerIdAndBuildingType(Long playerId, BuildingType buildingType);

    /**
     * Trouve tous les bâtiments capturés par un joueur.
     */
    List<Building> findByCapturedByPlayerId(Long capturedByPlayerId);

    /**
     * Trouve tous les bâtiments non détruits d'un joueur.
     */
    List<Building> findByPlayerIdAndIsDestroyedFalse(Long playerId);

    /**
     * Trouve tous les bâtiments d'un type donné pour un joueur, non détruits.
     * Ex: findByPlayerIdAndBuildingTypeAndIsDestroyedFalse(playerId, BuildingType.WEAPON_CACHE)
     */
    List<Building> findByPlayerIdAndBuildingTypeAndIsDestroyedFalse(Long playerId, BuildingType buildingType);
}

