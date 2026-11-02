package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.sector.SectorNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectorNoteRepository extends JpaRepository<SectorNote, SectorNote.NoteId> {
    List<SectorNote> findByPlayerIdAndBoardId(Long playerId, Long boardId);

    void deleteByPlayerIdAndBoardIdAndSectorNumber(Long playerId, Long boardId, int sectorNumber);
}
