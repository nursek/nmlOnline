package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.unit.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByPlayerId(Long playerId);

    List<Unit> findByPlayerIdAndSectorIsNull(Long playerId);

    // Projection : ne charge pas les entités avant les verrous joueurs (sinon snapshot périmé).
    @Query("select distinct u.playerId from Unit u where u.sector is null and u.playerId is not null")
    List<Long> findPlayerIdsWithReserveUnits();

    // Une unité sans secteur n'appartient à aucune partie : elle est replacée au QG avant la résolution.
    @Query("select u from Unit u where u.sector.board.id = :boardId")
    List<Unit> findByBoardId(@Param("boardId") Long boardId);
}
