package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.unit.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour les personnages principaux.
 */
@Repository
public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {

    /**
     * Trouve le personnage d'un joueur.
     */
    Optional<GameCharacter> findByPlayerId(Long playerId);

    /**
     * Trouve un personnage par son nom.
     */
    Optional<GameCharacter> findByName(String name);

    /**
     * Vérifie si un joueur possède déjà un personnage.
     */
    boolean existsByPlayerId(Long playerId);

    /**
     * Vérifie si un nom de personnage existe déjà.
     */
    boolean existsByName(String name);
}

