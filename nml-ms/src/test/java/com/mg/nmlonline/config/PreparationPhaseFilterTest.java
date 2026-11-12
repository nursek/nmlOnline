package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreparationPhaseFilterTest {

    private final BoardRepository boardRepository = mock(BoardRepository.class);
    private final PreparationPhaseFilter filter = new PreparationPhaseFilter(boardRepository);

    private MockHttpServletResponse perform(String method, String path, Long boardId) throws Exception {
        if (boardId != null) {
            Board board = new Board();
            board.setId(boardId);
            board.setCurrentTurn(0);
            when(boardRepository.findById(boardId)).thenReturn(Optional.of(board));
        }
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setAttribute("userId", 1L);
        if (boardId != null) {
            request.setAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE, boardId);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void blocksGameMutationsDuringPreparation() throws Exception {
        assertEquals(409, perform("POST", "/api/players/equipment/buy", 1L).getStatus());
        assertEquals(409, perform("POST", "/api/units/buy-batch", 1L).getStatus());
        assertEquals(409, perform("PUT", "/api/buildings/3/move", 1L).getStatus());
    }

    @Test
    void allowsPreparationSocialAndAdminRoutes() throws Exception {
        assertEquals(200, perform("POST", "/api/preparation/units", 1L).getStatus());
        assertEquals(200, perform("POST", "/api/alliances/proposals", 1L).getStatus());
        assertEquals(200, perform("POST", "/api/bank/offers", 1L).getStatus());
        assertEquals(200, perform("POST", "/api/admin/players/template", 1L).getStatus());
        assertEquals(200, perform("GET", "/api/players/me", 1L).getStatus());
    }

    @Test
    void ignoresRequestsWithoutBoard() throws Exception {
        assertEquals(200, perform("POST", "/api/players/equipment/buy", null).getStatus());
    }
}
