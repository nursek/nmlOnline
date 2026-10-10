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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    @DisplayName("resolve attribue le secteur avec la couleur d'un autre secteur du joueur")
    void resolveAssignsSectorWithPlayerColor() {
        Board board = boardWithSector();
        Sector owned = new Sector(2, "Autre secteur");
        owned.setOwnerAndColor(42L, "#123456");
        board.addSector(owned);
        PendingCapture pending = PendingCapture.create(7L, 1, 3, List.of(42L));
        pending.setId(11L);
        when(repository.findById(11L)).thenReturn(Optional.of(pending));
        when(boardRepository.findById(7L)).thenReturn(Optional.of(board));

        service.resolve(11L, 42L);

        assertEquals(42L, board.getSector(1).getOwnerId());
        assertEquals("#123456", board.getSector(1).getColor(), "La couleur vient d'un autre secteur du joueur");
        assertTrue(pending.isResolved());
        verify(boardRepository).save(board);
        verify(repository).save(pending);
    }

    @Test
    @DisplayName("resolve d'une attribution déjà résolue → IllegalStateException")
    void resolveAlreadyResolvedThrows() {
        PendingCapture pending = PendingCapture.create(7L, 1, 3, List.of(42L));
        pending.setResolved(true);
        when(repository.findById(11L)).thenReturn(Optional.of(pending));

        assertThrows(IllegalStateException.class, () -> service.resolve(11L, 42L));
    }

    @Test
    @DisplayName("dismiss marque l'attribution résolue sans toucher au secteur")
    void dismissMarksResolved() {
        PendingCapture pending = PendingCapture.create(7L, 1, 3, List.of(42L));
        when(repository.findById(11L)).thenReturn(Optional.of(pending));

        service.dismiss(11L);

        assertTrue(pending.isResolved());
        verify(repository).save(pending);
    }
}
