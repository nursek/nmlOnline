package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.unit.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {

    Optional<GameCharacter> findByPlayerId(Long playerId);

    Optional<GameCharacter> findByName(String name);

    boolean existsByPlayerId(Long playerId);

    boolean existsByName(String name);
}

