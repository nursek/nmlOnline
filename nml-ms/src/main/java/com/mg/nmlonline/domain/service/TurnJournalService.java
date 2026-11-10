package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.TurnJournalDto;
import com.mg.nmlonline.domain.model.turn.TurnJournal;
import com.mg.nmlonline.infrastructure.repository.TurnJournalRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class TurnJournalService {

    private final TurnJournalRepository repository;
    private final ObjectMapper objectMapper;

    public TurnJournalService(TurnJournalRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void save(Long boardId, TurnJournalDto journal) {
        TurnJournal entity = new TurnJournal();
        entity.setBoardId(boardId);
        entity.setTurn(journal.getTurn());
        entity.setPayload(write(journal));
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public TurnJournalDto findByTurn(Long boardId, int turn) {
        return repository.findFirstByBoardIdAndTurnOrderByIdDesc(boardId, turn)
                .map(this::read)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucun journal archivé pour le tour " + turn));
    }

    private String write(TurnJournalDto journal) {
        try {
            return objectMapper.writeValueAsString(journal);
        } catch (JacksonException e) {
            throw new IllegalStateException("Sérialisation du journal du tour impossible", e);
        }
    }

    private TurnJournalDto read(TurnJournal journal) {
        try {
            return objectMapper.readValue(journal.getPayload(), TurnJournalDto.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Journal du tour " + journal.getId() + " illisible", e);
        }
    }
}
