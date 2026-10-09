package com.mg.nmlonline.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MutationRateLimitFilterTest {

    private final MutationRateLimitFilter filter = new MutationRateLimitFilter();

    private MockHttpServletResponse perform(String method, Long userId) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/players/actions/harvest");
        if (userId != null) {
            request.setAttribute("userId", userId);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void blocksMutationsBeyondLimitPerUser() throws Exception {
        for (int i = 0; i < 120; i++) {
            assertEquals(200, perform("POST", 1L).getStatus());
        }

        assertEquals(429, perform("POST", 1L).getStatus());
        assertEquals(200, perform("POST", 2L).getStatus());
    }

    @Test
    void ignoresReadsAndAnonymousRequests() throws Exception {
        for (int i = 0; i < 200; i++) {
            assertEquals(200, perform("GET", 1L).getStatus());
            assertEquals(200, perform("POST", null).getStatus());
        }
    }
}
