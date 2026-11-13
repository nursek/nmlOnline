package com.mg.nmlonline;

import com.jayway.jsonpath.JsonPath;
import com.mg.nmlonline.config.TestDataInitializer;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EmbeddedPostgresTest
@AutoConfigureMockMvc
@DisplayName("Flux d'authentification")
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    private String registerBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private String loginBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    @DisplayName("register → login → refresh → logout → refresh invalide")
    void registerLoginRefreshLogout() throws Exception {
        String username = "authflowuser";
        String password = "secret123";

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, password)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, password)))
                .andExpect(status().isConflict());

        MvcResult login = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");
        assertNotNull(refreshCookie, "Le login doit poser le cookie refresh_token");
        assertTrue(refreshCookie.getSecure(), "Le cookie refresh doit porter Secure hors exception dev");

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andReturn();

        Cookie rotated = refreshed.getResponse().getCookie("refresh_token");
        assertNotNull(rotated, "Le refresh doit faire tourner le cookie");

        mockMvc.perform(post("/api/auth/logout").cookie(rotated))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh").cookie(rotated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @DisplayName("Login bloqué : 429 avec Retry-After et durée restante")
    void blockedLoginReturnsRetryAfter() throws Exception {
        String username = "authblockeduser";
        String ip = "203.0.113.7";

        for (int attempt = 1; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/login")
                            .with(request -> {
                                request.setRemoteAddr(ip);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(username, "mauvais")))
                    .andExpect(status().isUnauthorized());
        }

        MvcResult fifth = mockMvc.perform(post("/api/login")
                        .with(request -> {
                            request.setRemoteAddr(ip);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, "mauvais")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andReturn();

        int retryAfter = Integer.parseInt(fifth.getResponse().getHeader("Retry-After"));
        assertTrue(retryAfter > 0 && retryAfter <= 60, "Retry-After doit donner le décompte en secondes");

        mockMvc.perform(post("/api/login")
                        .with(request -> {
                            request.setRemoteAddr(ip);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, "secret123")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Un access token ne vaut pas comme refresh, un refresh pas comme Bearer")
    void tokenTypesAreNotInterchangeable() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(TestDataInitializer.USER_1, TestDataInitializer.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.token");
        Cookie refreshCookie = login.getResponse().getCookie("refresh_token");
        assertNotNull(refreshCookie);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

        mockMvc.perform(get("/api/turn/current")
                        .header("Authorization", "Bearer " + refreshCookie.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Réutilisation d'un token rotaté : la session active est révoquée")
    void rotatedTokenReuseRevokesActiveSession() throws Exception {
        String username = "authreuseuser";
        String password = "secret123";

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(username, password)))
                .andExpect(status().isOk());

        MvcResult login = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie stolen = login.getResponse().getCookie("refresh_token");
        assertNotNull(stolen);

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(stolen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andReturn();
        Cookie active = refreshed.getResponse().getCookie("refresh_token");
        assertNotNull(active);

        // Sortir de la grace period de 3 s pour que l'ancien token soit traité comme une réutilisation.
        Thread.sleep(3_200);

        mockMvc.perform(post("/api/auth/refresh").cookie(stolen))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

        mockMvc.perform(post("/api/auth/refresh").cookie(active))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
