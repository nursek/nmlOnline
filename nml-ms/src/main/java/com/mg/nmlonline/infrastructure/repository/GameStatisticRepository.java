package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.statistics.GameStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameStatisticRepository extends JpaRepository<GameStatistic, String> {

    @Modifying
    @Query("UPDATE GameStatistic s SET s.total = s.total + 1 WHERE s.statKey = :statKey")
    int increment(@Param("statKey") String statKey);
}
