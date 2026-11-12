package com.mg.nmlonline.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaForwardControllerTest {

    private static final Pattern ANGULAR_ROUTE = Pattern.compile("path:\\s*'([^']+)'");

    // Une route oubliée dans le forward casse le rechargement en prod (404 "No static resource").
    @Test
    void chaqueRouteAngularEstRedirigeeVersIndex() throws Exception {
        Set<String> forwarded = Arrays.stream(SpaForwardController.class.getMethod("forward")
                        .getAnnotation(GetMapping.class).value())
                .map(pattern -> pattern.replaceFirst("^/", "").replaceFirst("/\\*\\*$", ""))
                .collect(Collectors.toSet());
        List<String> uncovered = ANGULAR_ROUTE.matcher(
                        Files.readString(Path.of("..", "nml-ui", "src", "app", "app.routes.ts")))
                .results()
                .map(match -> match.group(1))
                .filter(route -> !route.isEmpty() && !route.equals("**"))
                .filter(route -> forwarded.stream()
                        .noneMatch(path -> route.equals(path) || route.startsWith(path + "/")))
                .toList();
        assertTrue(uncovered.isEmpty(), "Routes Angular sans forward SPA : " + uncovered);
    }
}
