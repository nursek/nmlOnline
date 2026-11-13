package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GameEndService — état de fin de partie")
class GameEndServiceTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final GameEndService service = new GameEndService(boardRepository, playerRepository);

    @Test
    void runningGameHasNoWinner() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(new Board()));

        GameEndService.GameOutcome state = service.getState(1L);

        assertFalse(state.finished());
        assertNull(state.finalTurn());
        assertNull(state.winnerPlayerId());
        assertFalse(state.posthumous());
    }

    @Test
    void finishedGameExposesWinnerAndAdjustment() {
        Board board = new Board();
        board.setCurrentTurn(7);
        board.setFinishedAt(Instant.now());
        board.setWinnerPlayerId(7L);
        board.setWinnerAdjusted(true);
        Player winner = new Player("nursek");
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(playerRepository.findById(7L)).thenReturn(Optional.of(winner));

        GameEndService.GameOutcome state = service.getState(1L);

        assertTrue(state.finished());
        assertEquals(7, state.finalTurn());
        assertEquals(7L, state.winnerPlayerId());
        assertEquals("nursek", state.winnerName());
        assertFalse(state.posthumous());
        assertTrue(state.winnerAdjusted());
    }

    @Test
    void eliminatedWinnerIsPosthumous() {
        Board board = new Board();
        board.setCurrentTurn(5);
        board.setFinishedAt(Instant.now());
        board.setWinnerPlayerId(3L);
        Player winner = new Player("lurio");
        winner.eliminate(EliminationCause.HEADQUARTERS_CAPTURED, 5, 2L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(playerRepository.findById(3L)).thenReturn(Optional.of(winner));

        assertTrue(service.getState(1L).posthumous());
    }
}
