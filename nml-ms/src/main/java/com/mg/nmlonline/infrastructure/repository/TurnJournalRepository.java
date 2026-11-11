package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.turn.TurnJournal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TurnJournalRepository extends JpaRepository<TurnJournal, Long> {

    Optional<TurnJournal> findFirstByTurnOrderByIdDesc(int turn);
}
