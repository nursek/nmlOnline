package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtre JWT qui valide le token sur chaque requête protégée.
 * Extrait le username du token et peuple le SecurityContext.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si pas de header Authorization ou pas de Bearer token, continuer sans authentifier
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            // Valider le token et extraire les claims
            JwtService.JwtClaims claims = jwtService.validateAndExtractClaims(jwt);

            if (claims != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Créer l'authentification avec le username et l'ID de l'utilisateur
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        claims.username(),
                        null,
                        Collections.emptyList()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Stocker l'ID utilisateur dans les détails pour usage ultérieur
                request.setAttribute("userId", claims.userId());

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token invalide ou expiré - ne pas authentifier, laisser passer
            // La sécurité refusera l'accès aux endpoints protégés
            logger.debug("JWT validation failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Ne pas filtrer les endpoints publics
        return path.equals("/api/login") ||
               path.equals("/api/register") ||
               path.equals("/api/auth/refresh") ||
               path.equals("/api/auth/logout") ||
               path.startsWith("/h2-console");
    }
}

