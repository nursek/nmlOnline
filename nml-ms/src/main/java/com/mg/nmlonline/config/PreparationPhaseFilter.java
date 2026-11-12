package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tour 0 : seules la préparation, les échanges/alliances et les actions admin sont ouvertes.
 * Les autres mutations (achat, ordre, récolte, déplacement) seraient jouées hors tour.
 */
@Component
public class PreparationPhaseFilter extends OncePerRequestFilter {

    private final BoardRepository boardRepository;

    public PreparationPhaseFilter(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Long boardId = (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE);
        if (boardId == null) {
            filterChain.doFilter(request, response);
            return;
        }
        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null || !board.isPreparation()) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":409,\"title\":\"Conflict\","
                + "\"detail\":\"Partie en préparation : action indisponible avant le tour 1\"}");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (!path.startsWith("/api/")
                || "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return true;
        }
        if (path.startsWith("/api/auth/")
                || path.equals("/api/login")
                || path.equals("/api/register")
                || path.equals("/api/instances")
                || path.startsWith("/api/admin/")
                || path.startsWith("/api/preparation/")
                || path.startsWith("/api/alliances/")
                || path.startsWith("/api/bank/offers")
                || path.equals("/api/boards")
                || path.startsWith("/api/boards/")) {
            return true;
        }
        return request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE) == null;
    }
}
