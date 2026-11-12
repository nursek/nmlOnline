package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.statistics.GameStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameStatisticRepository extends JpaRepository<GameStatistic, GameStatistic.StatId> {

    List<GameStatistic> findByBoardId(Long boardId);

    @Modifying
    @Query("UPDATE GameStatistic s SET s.total = s.total + 1 WHERE s.boardId = :boardId AND s.statKey = :statKey")
    int increment(@Param("boardId") Long boardId, @Param("statKey") String statKey);
}
