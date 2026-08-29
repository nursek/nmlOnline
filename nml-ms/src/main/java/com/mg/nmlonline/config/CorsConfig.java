package com.mg.nmlonline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") String extraOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = new ArrayList<>(Arrays.asList(
            "http://localhost:5174",
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:4200",
            "https://nml.lurio.fr"
        ));
        if (!extraOrigins.isBlank()) {
            origins.addAll(Arrays.asList(extraOrigins.split(",")));
        }
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));

        configuration.setAllowCredentials(true);

        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        ));

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}

