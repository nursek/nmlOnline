package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.press.EliminationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EliminationEventRepository extends JpaRepository<EliminationEvent, Long> {

    List<EliminationEvent> findByBoardIdOrderByDeathIndexAsc(Long boardId);

    long countByBoardId(Long boardId);

    Optional<EliminationEvent> findFirstByBoardIdOrderByDeathIndexDesc(Long boardId);

    void deleteByBoardId(Long boardId);
}
