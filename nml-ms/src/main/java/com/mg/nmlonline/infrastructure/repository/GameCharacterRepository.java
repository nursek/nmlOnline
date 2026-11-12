package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.unit.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {

    Optional<GameCharacter> findByPlayerId(Long playerId);

    @Query("select c from GameCharacter c, Player p where p.id = c.playerId "
            + "and p.boardId = :boardId and c.name = :name")
    Optional<GameCharacter> findByBoardIdAndName(@Param("boardId") Long boardId, @Param("name") String name);

    boolean existsByPlayerId(Long playerId);

    @Query("select count(c) > 0 from GameCharacter c, Player p where p.id = c.playerId "
            + "and p.boardId = :boardId and c.name = :name")
    boolean existsByBoardIdAndName(@Param("boardId") Long boardId, @Param("name") String name);

    // Personnage sans secteur = non déployé : il n'appartient à aucune partie.
    @Query("select c from GameCharacter c where c.sector.board.id = :boardId")
    List<GameCharacter> findByBoardId(@Param("boardId") Long boardId);
}

