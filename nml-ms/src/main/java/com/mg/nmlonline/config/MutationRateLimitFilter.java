package com.mg.nmlonline.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MutationRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_MUTATIONS_PER_MINUTE = 120;
    private static final long WINDOW_MS = 60_000;
    private static final int MAX_ENTRIES = 10_000;

    private final Map<Long, long[]> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long id && isMutation(request.getMethod())
                && request.getRequestURI().startsWith("/api/")) {
            long now = System.currentTimeMillis();
            long[] window = windows.compute(id, (k, w) ->
                    w == null || now - w[0] >= WINDOW_MS ? new long[]{now, 1} : new long[]{w[0], w[1] + 1});
            if (window[1] > MAX_MUTATIONS_PER_MINUTE) {
                response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Trop de requêtes");
                return;
            }
            if (windows.size() > MAX_ENTRIES) {
                windows.entrySet().removeIf(e -> now - e.getValue()[0] >= WINDOW_MS);
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isMutation(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }
}
