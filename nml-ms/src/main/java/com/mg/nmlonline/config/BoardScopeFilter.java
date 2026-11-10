package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.infrastructure.repository.BoardAdminRepository;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Plusieurs parties imposent X-Board-Id : aucun repli silencieux sur la première. */
@Component
public class BoardScopeFilter extends OncePerRequestFilter {

    public static final String BOARD_ID_ATTRIBUTE = "boardId";
    static final String INSTANCE_ADMIN_ROLE = "ROLE_INSTANCE_ADMIN";

    private static final Pattern ADMIN_PLAYER_PATH =
            Pattern.compile("^/api/admin/players/(\\d+)(/.*)?$");

    private final BoardRepository boardRepository;
    private final BoardAdminRepository boardAdminRepository;
    private final PlayerRepository playerRepository;

    public BoardScopeFilter(BoardRepository boardRepository, BoardAdminRepository boardAdminRepository,
                           PlayerRepository playerRepository) {
        this.boardRepository = boardRepository;
        this.boardAdminRepository = boardAdminRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String raw = request.getHeader("X-Board-Id");
        Long boardId;
        if (raw == null || raw.isBlank()) {
            List<Long> ids = boardRepository.findAll().stream().map(Board::getId).toList();
            if (ids.isEmpty()) {
                reject(response, HttpStatus.BAD_REQUEST,
                        "Aucune partie en base : créez ou importez une partie d abord.");
                return;
            }
            if (ids.size() > 1) {
                reject(response, HttpStatus.BAD_REQUEST,
                        "En-tête X-Board-Id requis : plusieurs parties existent.");
                return;
            }
            boardId = ids.getFirst();
        } else {
            try {
                boardId = Long.valueOf(raw.trim());
            } catch (NumberFormatException e) {
                reject(response, HttpStatus.BAD_REQUEST, "X-Board-Id invalide : " + raw);
                return;
            }
            if (!boardRepository.existsById(boardId)) {
                reject(response, HttpStatus.NOT_FOUND, "Partie inconnue : " + raw);
                return;
            }
        }
        request.setAttribute(BOARD_ID_ATTRIBUTE, boardId);
        grantGameMasterAuthority(request, boardId);
        if (!enforceGameMasterPlayerScope(request, boardId)) {
            reject(response, HttpStatus.FORBIDDEN,
                    "Ce joueur n'appartient pas à la partie sélectionnée.");
            return;
        }
        filterChain.doFilter(request, response);
    }

    // Autorité valable pour cette requête seulement : la révocation d'un MJ prend effet immédiatement.
    private void grantGameMasterAuthority(HttpServletRequest request, Long boardId) {
        Object userId = request.getAttribute("userId");
        if (!(userId instanceof Long id) || !boardAdminRepository.existsByBoardIdAndUserId(boardId, id)) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return;
        }
        List<GrantedAuthority> authorities = new ArrayList<>(authentication.getAuthorities());
        GrantedAuthority instanceAdmin = new SimpleGrantedAuthority(INSTANCE_ADMIN_ROLE);
        if (!authorities.contains(instanceAdmin)) {
            authorities.add(instanceAdmin);
            SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            authentication.getPrincipal(), authentication.getCredentials(), authorities));
        }
    }

    /** Un MJ ne peut lire ou éditer que les joueurs de sa partie, même en changeant l'id dans l'URL. */
    private boolean enforceGameMasterPlayerScope(HttpServletRequest request, Long boardId) {
        if (hasAuthority("ROLE_ADMIN") || !hasAuthority(INSTANCE_ADMIN_ROLE)) {
            return true;
        }
        Matcher matcher = ADMIN_PLAYER_PATH.matcher(request.getRequestURI());
        if (!matcher.matches()) {
            return true;
        }
        Long playerId = Long.valueOf(matcher.group(1));
        return playerRepository.findById(playerId)
                .map(player -> boardId.equals(player.getBoardId()))
                .orElse(false);
    }

    // isUserInRole dépend de SecurityContextHolderAwareRequestFilter, qui tourne après nous.
    private boolean hasAuthority(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Créer une partie ne peut pas dépendre d'une partie déjà choisie.
        if (path.equals("/api/boards") && "POST".equals(request.getMethod())) {
            return true;
        }
        return !path.startsWith("/api/")
                || path.equals("/api/instances")
                || path.equals("/api/admin/boards/import")
                || path.equals("/api/admin/boards/assets")
                || path.equals("/api/login")
                || path.equals("/api/register")
                || path.startsWith("/api/auth/");
    }

    private void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status.value() + ",\"title\":\"" + status.getReasonPhrase()
                + "\",\"detail\":\"" + detail + "\"}");
    }
}
