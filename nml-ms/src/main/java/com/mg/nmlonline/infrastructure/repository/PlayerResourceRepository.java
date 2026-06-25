package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.resource.PlayerResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour gérer les ressources possédées par les joueurs
 */
@Repository
public interface PlayerResourceRepository extends JpaRepository<PlayerResource, Long> {
}
