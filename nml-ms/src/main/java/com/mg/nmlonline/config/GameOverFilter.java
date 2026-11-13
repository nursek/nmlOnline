package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GameOverFilter extends OncePerRequestFilter {

    private final BoardRepository boardRepository;

    public GameOverFilter(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Object boardId = request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE);
        if (!(boardId instanceof Long id)) {
            filterChain.doFilter(request, response);
            return;
        }
        Board board = boardRepository.findById(id).orElse(null);
        if (board != null && board.isFinished()) {
            response.setStatus(HttpStatus.CONFLICT.value());
            response.setContentType("application/problem+json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":409,\"title\":\"Conflict\","
                    + "\"detail\":\"Partie terminée — les actions sont désactivées.\"}");
            return;
        }
        filterChain.doFilter(request, response);
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
                || (path.equals("/api/boards") && "POST".equals(method))) {
            return true;
        }
        return request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE) == null;
    }
}
