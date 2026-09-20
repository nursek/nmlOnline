package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.alliance.PendingCapture;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PendingCaptureRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Attribution en attente : une seule ligne par secteur et par tour")
class PendingCaptureServiceTest {

    @Mock
    PendingCaptureRepository repository;

    @Mock
    BoardRepository boardRepository;

    @Mock
    PlayerRepository playerRepository;

    @InjectMocks
    PendingCaptureService service;

    private Board boardWithSector() {
        Board board = new Board();
        board.setId(7L);
        board.addSector(new Sector(1, "Objectif"));
        return board;
    }

    @Test
    @DisplayName("Ligne déjà en attente pour le secteur/tour : aucune nouvelle ligne")
    void queueNeutralSkipsDuplicate() {
        Board board = boardWithSector();
        when(repository.existsByBoardIdAndSectorNumberAndTurnAndResolvedFalse(7L, 1, 3)).thenReturn(true);

        service.queueNeutral(board, 1, 3, List.of(1L, 2L));

        verify(repository, never()).save(any(PendingCapture.class));
    }

    @Test
    @DisplayName("Première conquête : le secteur devient neutre et la ligne est créée")
    void queueNeutralCreatesPendingCapture() {
        Board board = boardWithSector();
        when(repository.existsByBoardIdAndSectorNumberAndTurnAndResolvedFalse(7L, 1, 3)).thenReturn(false);

        service.queueNeutral(board, 1, 3, List.of(1L, 2L));

        assertNull(board.getSector(1).getOwnerId());
        verify(repository).save(any(PendingCapture.class));
    }
}
