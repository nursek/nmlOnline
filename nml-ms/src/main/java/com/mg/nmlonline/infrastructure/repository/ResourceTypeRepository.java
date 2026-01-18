package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.resource.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour gérer les types de ressources disponibles dans le jeu
 */
@Repository
public interface ResourceTypeRepository extends JpaRepository<ResourceType, Long> {
    Optional<ResourceType> findByName(String name);
}
