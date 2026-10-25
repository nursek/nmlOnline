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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EliminatedPlayerFilterTest {

    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final EliminatedPlayerFilter filter = new EliminatedPlayerFilter(playerRepository);

    private record Result(MockHttpServletResponse response, MockFilterChain chain) {
    }

    private Result perform(String method, String uri, Long userId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (userId != null) {
            request.setAttribute("userId", userId);
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

    private void assertPassed(String method, String uri, Long userId) throws Exception {
        Result result = perform(method, uri, userId);
        assertEquals(200, result.response().getStatus());
        assertNotNull(result.chain().getRequest(), "La requête doit atteindre la chaîne");
    }

    private Player eliminatedPlayer() {
        Player player = new Player("Elimine");
        player.eliminate(EliminationCause.MAIN_CHARACTER_KILLED, 3);
        return player;
    }

    @Test
    void blocksMutationsOfEliminatedPlayers() throws Exception {
        when(playerRepository.findByUserId(1L)).thenReturn(Optional.of(eliminatedPlayer()));

        assertBlocked("POST", "/api/units/orders");
        assertBlocked("PUT", "/api/vehicles/1/crew");
        assertBlocked("PATCH", "/api/players/me");
        assertBlocked("DELETE", "/api/exchanges/offers/5");
    }

    @Test
    void letsEliminatedPlayersReadAndLetsActivePlayersAct() throws Exception {
        when(playerRepository.findByUserId(1L)).thenReturn(Optional.of(eliminatedPlayer()));
        when(playerRepository.findByUserId(2L)).thenReturn(Optional.of(new Player("Actif")));

        assertPassed("GET", "/api/players/me", 1L);
        assertPassed("POST", "/api/units/orders", 2L);
        assertPassed("POST", "/api/units/orders", null);
    }

    @Test
    void ignoresAdminEndpoints() throws Exception {
        when(playerRepository.findByUserId(1L)).thenReturn(Optional.of(eliminatedPlayer()));

        assertPassed("POST", "/api/admin/turn/next", 1L);
    }
}
