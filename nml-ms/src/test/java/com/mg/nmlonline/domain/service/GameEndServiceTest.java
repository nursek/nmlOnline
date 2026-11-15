package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.press.EliminationEvent;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.EliminationEventRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GameEndService — fin de partie et champion")
class GameEndServiceTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final EliminationEventRepository eliminationEventRepository =
            mock(EliminationEventRepository.class);
    private final AdminAuditService adminAuditService = mock(AdminAuditService.class);
    private final GameEndService service = new GameEndService(boardRepository, playerRepository,
            eliminationEventRepository, adminAuditService);

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
    void oneSurvivorWins() {
        Board board = new Board();
        board.setCurrentTurn(5);
        Player winner = new Player("nursek");
        winner.setId(1L);
        Player eliminated = eliminated("lurio");

        GameEndService.GameOutcome state = service.finishIfOver(board, List.of(winner, eliminated),
                List.of());

        assertTrue(state.finished());
        assertEquals(1L, state.winnerPlayerId());
        assertFalse(state.posthumous());
        assertNotNull(board.getFinishedAt());
        assertEquals(1L, board.getWinnerPlayerId());
        assertFalse(board.isWinnerAdjusted());
    }

    @Test
    void noSurvivorGivesTheLastDeathAsPosthumousWinner() {
        Board board = new Board();
        board.setCurrentTurn(5);
        Player first = eliminated("nursek");
        first.setId(1L);
        Player victim = eliminated("lurio");
        victim.setId(2L);
        EliminationEvent event = new EliminationEvent();
        event.setVictimPlayerId(2L);
        event.setVictimName(victim.getName());
        event.setDeathIndex(1);
        when(playerRepository.findById(2L)).thenReturn(Optional.of(victim));

        GameEndService.GameOutcome state = service.finishIfOver(board,
                List.of(first, victim), List.of(event));

        assertTrue(state.finished());
        assertEquals(2L, state.winnerPlayerId());
        assertTrue(state.posthumous());
    }

    @Test
    void twoSurvivorsDoNotEndTheGame() {
        Board board = new Board();
        Player first = new Player("nursek");
        Player second = new Player("lurio");

        GameEndService.GameOutcome state = service.finishIfOver(board, List.of(first, second),
                List.of());

        assertFalse(state.finished());
        assertNull(board.getFinishedAt());
    }

    @Test
    void adjustWinnerDesignatesAnotherEliminatedPlayer() {
        Board board = new Board();
        board.setCurrentTurn(5);
        board.setFinishedAt(Instant.now());
        Player natural = eliminated("lurio");
        natural.setId(2L);
        Player designated = eliminated("trazyn");
        designated.setId(3L);
        board.setWinnerPlayerId(2L);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(playerRepository.findByBoardId(1L)).thenReturn(List.of(natural, designated));
        when(playerRepository.findById(3L)).thenReturn(Optional.of(designated));
        when(eliminationEventRepository.findFirstByBoardIdOrderByDeathIndexDesc(1L))
                .thenReturn(Optional.of(event(natural)));

        GameEndService.GameOutcome state = service.adjustWinner(1L, 3L);

        assertEquals(3L, board.getWinnerPlayerId());
        assertTrue(board.isWinnerAdjusted());
        assertEquals(3L, state.winnerPlayerId());
        verify(boardRepository).save(board);
        verify(adminAuditService).record(eq(1L), eq(3L), any(), any());
    }

    @Test
    void adjustWinnerRefusedWhenSomeoneSurvives() {
        Board board = new Board();
        board.setFinishedAt(Instant.now());
        Player survivor = new Player("nursek");
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(playerRepository.findByBoardId(1L)).thenReturn(List.of(survivor));

        assertThrows(IllegalStateException.class,
                () -> service.adjustWinner(1L, survivor.getId()));
    }

    private static Player eliminated(String name) {
        Player player = new Player(name);
        player.setBoardId(1L);
        player.eliminate(EliminationCause.MAIN_CHARACTER_KILLED, 5, 1L);
        return player;
    }

    private static EliminationEvent event(Player victim) {
        EliminationEvent event = new EliminationEvent();
        event.setVictimPlayerId(victim.getId());
        event.setVictimName(victim.getName());
        return event;
    }
}
