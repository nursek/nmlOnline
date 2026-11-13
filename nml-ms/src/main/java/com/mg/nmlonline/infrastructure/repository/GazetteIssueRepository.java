package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.press.GazetteIssue;
import com.mg.nmlonline.domain.model.press.GazetteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GazetteIssueRepository extends JpaRepository<GazetteIssue, Long> {

    Optional<GazetteIssue> findByBoardIdAndTurn(Long boardId, int turn);

    List<GazetteIssue> findByBoardIdOrderByTurnDesc(Long boardId);

    List<GazetteIssue> findByBoardIdAndStatusOrderByTurnDesc(Long boardId, GazetteStatus status);

    Optional<GazetteIssue> findFirstByBoardIdAndStatusOrderByTurnDesc(Long boardId,
                                                                     GazetteStatus status);

    @Query("select coalesce(max(g.issueNumber), 0) from GazetteIssue g where g.boardId = :boardId")
    int findMaxIssueNumberByBoardId(@Param("boardId") Long boardId);

    long countByBoardId(Long boardId);

    void deleteByBoardId(Long boardId);
}
