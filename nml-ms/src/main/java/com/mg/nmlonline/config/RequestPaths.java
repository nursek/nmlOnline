package com.mg.nmlonline.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

final class RequestPaths {

    private RequestPaths() {
    }

    // getRequestURI est brut alors que Spring Security et MVC routent sur le chemin décodé : « /api/%61dmin » contournerait un startsWith.
    static String decoded(HttpServletRequest request) {
        return UriUtils.decode(request.getRequestURI(), StandardCharsets.UTF_8);
    }
}
