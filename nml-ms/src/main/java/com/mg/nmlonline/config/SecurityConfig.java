package com.mg.nmlonline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Activer CORS avec la configuration personnalisée
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));

        // Autoriser les endpoints publics (login/register/refresh/logout + console H2)
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**",
                        "/h2-console/**"
                ).permitAll()
                .anyRequest().authenticated()
        );

        // Désactiver CSRF pour une API REST stateless (JWT)
        http.csrf(AbstractHttpConfigurer::disable);

        // Ne pas appliquer CSRF sur la console H2 (sécurisé pour dev) - redondant mais inoffensif
        // http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));

        // Autoriser l'affichage dans un iframe de la même origine (résout X-Frame-Options: DENY)
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        // Mode sans état : pour JWT.
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

}
