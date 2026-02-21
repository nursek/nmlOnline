package com.mg.nmlonline.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Activer CORS avec la configuration personnalisée
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));

        // Configuration des autorisations
        http.authorizeHttpRequests(auth -> auth
                // Endpoints publics (authentification)
                .requestMatchers(
                        "/api/login",
                        "/api/register",
                        "/api/auth/refresh",
                        "/api/auth/logout"
                ).permitAll()
                // Console H2 (dev uniquement)
                .requestMatchers("/h2-console/**").permitAll()
                // Fichiers statiques Angular
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/*.js",
                        "/*.css",
                        "/*.ico",
                        "/*.png",
                        "/*.svg",
                        "/*.woff",
                        "/*.woff2",
                        "/assets/**"
                ).permitAll()
                // Endpoints admin (nécessitent le rôle ADMIN)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Tous les autres endpoints API nécessitent une authentification
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
        );

        // Retourner 401 (au lieu de 403) quand l'utilisateur n'est pas authentifié
        // Permet à l'intercepteur Angular de déclencher le refresh du token
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expiré ou absent")
                )
        );

        // Ajouter le filtre JWT avant UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Désactiver CSRF pour une API REST stateless (JWT)
        http.csrf(AbstractHttpConfigurer::disable);

        // Autoriser l'affichage dans un iframe de la même origine (console H2)
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        // Mode sans état : pour JWT (pas de session côté serveur)
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

}
