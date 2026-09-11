package com.mg.nmlonline.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Sandbox des assets board servis same-origin : un SVG uploadé ne doit jamais exécuter de script. */
@Component
public class BoardAssetHeadersFilter extends OncePerRequestFilter {

    private static final String CSP = "sandbox; script-src 'none'; object-src 'none'; base-uri 'none'";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", CSP);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !decodedPath(request).startsWith("/boards/");
    }

    /** Le handler statique mappe le chemin décodé : on décode aussi, sinon /%62oards/** échappe au sandbox. */
    private static String decodedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (int i = 0; i < 2; i++) {
            try {
                String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
                if (decoded.equals(path)) break;
                path = decoded;
            } catch (IllegalArgumentException e) {
                break;
            }
        }
        return path;
    }
}
