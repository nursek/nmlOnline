package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/** Après élimination, toutes les actions de jeu sont refusées : un seul garde couvre tous les endpoints mutatifs. */
@Component
public class EliminatedPlayerFilter extends OncePerRequestFilter {

    private final PlayerRepository playerRepository;

    public EliminatedPlayerFilter(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Long id && isMutation(request.getMethod())
                && request.getRequestURI().startsWith("/api/")
                && !request.getRequestURI().startsWith("/api/admin/")) {
            Optional<Player> player = playerRepository.findByUserId(id);
            if (player.isPresent() && player.get().isEliminated()) {
                response.setStatus(HttpStatus.CONFLICT.value());
                response.setContentType("application/problem+json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\":409,\"title\":\"Conflict\","
                        + "\"detail\":\"Joueur éliminé — les actions sont désactivées.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isMutation(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }
}
