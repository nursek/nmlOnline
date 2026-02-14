package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.resource.PlayerResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour gérer les ressources possédées par les joueurs
 */
@Repository
public interface PlayerResourceRepository extends JpaRepository<PlayerResource, Long> {
    Optional<PlayerResource> findByIdAndPlayerName(Long id, String playerName);
}
