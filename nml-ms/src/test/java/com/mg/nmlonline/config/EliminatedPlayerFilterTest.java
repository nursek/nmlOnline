package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EliminatedPlayerFilterTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final EliminatedPlayerFilter filter = new EliminatedPlayerFilter(playerRepository);

    private MockHttpServletResponse perform(String method, String uri, Long userId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (userId != null) {
            request.setAttribute("userId", userId);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private Player eliminatedPlayer() {
        Player player = new Player("Elimine");
        player.eliminate(EliminationCause.MAIN_CHARACTER_KILLED, 3);
        return player;
    }

    @Test
    void blocksMutationsOfEliminatedPlayers() throws Exception {
        when(playerRepository.findByUserId(1L)).thenReturn(Optional.of(eliminatedPlayer()));

        assertEquals(409, perform("POST", "/api/units/orders", 1L).getStatus());
        assertEquals(409, perform("DELETE", "/api/exchanges/offers/5", 1L).getStatus());
    }

    @Test
    void letsEliminatedPlayersReadAndLetsActivePlayersAct() throws Exception {
        when(playerRepository.findByUserId(1L)).thenReturn(Optional.of(eliminatedPlayer()));
        Player active = new Player("Actif");
        when(playerRepository.findByUserId(2L)).thenReturn(Optional.of(active));

        assertEquals(200, perform("GET", "/api/players/me", 1L).getStatus());
        assertEquals(200, perform("POST", "/api/units/orders", 2L).getStatus());
        assertEquals(200, perform("POST", "/api/units/orders", null).getStatus());
    }

    @Test
    void ignoresAdminEndpoints() throws Exception {
        when(playerRepository.findByUserId(1L)).thenReturn(Optional.of(eliminatedPlayer()));

        assertEquals(200, perform("POST", "/api/admin/turn/next", 1L).getStatus());
    }
}
