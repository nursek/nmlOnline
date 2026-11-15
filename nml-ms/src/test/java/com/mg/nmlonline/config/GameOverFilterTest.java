package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameOverFilterTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final GameOverFilter filter = new GameOverFilter(boardRepository);

    private record Result(MockHttpServletResponse response, MockFilterChain chain) {
    }

    private Result perform(String method, String uri, Long boardId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setAttribute("userId", 1L);
        if (boardId != null) {
            request.setAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE, boardId);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return new Result(response, chain);
    }

    private void assertBlocked(String method, String uri) throws Exception {
        Result result = perform(method, uri, 1L);
        assertEquals(409, result.response().getStatus());
        assertNull(result.chain().getRequest(), "La requête ne doit pas atteindre la chaîne");
    }

    private void assertPassed(String method, String uri, Long boardId) throws Exception {
        Result result = perform(method, uri, boardId);
        assertEquals(200, result.response().getStatus());
        assertNotNull(result.chain().getRequest(), "La requête doit atteindre la chaîne");
    }

    private Board finishedBoard() {
        Board board = new Board();
        board.setFinishedAt(Instant.now());
        return board;
    }

    @Test
    void blocksMutationsAfterTheEnd() throws Exception {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(finishedBoard()));

        assertBlocked("POST", "/api/units/orders");
        assertBlocked("PUT", "/api/vehicles/1/crew");
        assertBlocked("PATCH", "/api/players/me");
        assertBlocked("DELETE", "/api/exchanges/offers/5");
    }

    @Test
    void letsReadsAdminsAndRunningGamesThrough() throws Exception {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(finishedBoard()));
        when(boardRepository.findById(2L)).thenReturn(Optional.of(new Board()));

        assertPassed("GET", "/api/players/me", 1L);
        assertPassed("POST", "/api/admin/turn/next", 1L);
        assertPassed("POST", "/api/units/orders", 2L);
    }

    @Test
    void letsRequestsWithoutBoardThrough() throws Exception {
        assertPassed("POST", "/api/boards", null);
    }
}
