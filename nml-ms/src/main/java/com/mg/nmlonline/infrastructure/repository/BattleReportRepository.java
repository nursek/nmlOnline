package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.battle.BattleReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BattleReportRepository extends JpaRepository<BattleReport, Long> {

    @Query("SELECT DISTINCT r FROM BattleReport r JOIN r.participantIds p WHERE p = :playerId ORDER BY r.id DESC")
    List<BattleReport> findByParticipantId(@Param("playerId") Long playerId);
}
