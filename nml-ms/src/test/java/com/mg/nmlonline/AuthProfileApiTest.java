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

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EmbeddedPostgresTest
@AutoConfigureMockMvc
@DisplayName("Authentification et profil — API")
class AuthProfileApiTest {

    private static final String AVATAR_PNG =
            "data:image/png;base64,"
                    + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

    @Autowired
    private MockMvc mockMvc;

    private record Session(String token, Cookie refreshCookie) {}

    private String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andExpect(status().isOk());
    }

    private Session login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        return new Session(token, result.getResponse().getCookie("refresh_token"));
    }

    @Test
    @DisplayName("register → login → refresh → logout → refresh invalide")
    void registerLoginRefreshLogout() throws Exception {
        String username = "authflowuser";
        String password = "secret123";

        register(username, password);
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andExpect(status().isConflict());

        Session session = login(username, password);
        assertNotNull(session.token());
        assertNotNull(session.refreshCookie(), "Le login doit poser le cookie refresh_token");
        assertTrue(session.refreshCookie().getSecure(),
                "Le cookie refresh doit porter Secure hors exception dev");

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
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
                            .content(credentials(username, "mauvais")))
                    .andExpect(status().isUnauthorized());
        }

        MvcResult fifth = mockMvc.perform(post("/api/login")
                        .with(request -> {
                            request.setRemoteAddr(ip);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "mauvais")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andReturn();

        int retryAfter = Integer.parseInt(fifth.getResponse().getHeader("Retry-After"));
        assertTrue(retryAfter > 0 && retryAfter <= 60,
                "Retry-After doit donner le décompte en secondes");

        mockMvc.perform(post("/api/login")
                        .with(request -> {
                            request.setRemoteAddr(ip);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "secret123")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Un access token ne vaut pas comme refresh, un refresh pas comme Bearer")
    void tokenTypesAreNotInterchangeable() throws Exception {
        Session session = login(TestDataInitializer.USER_1, TestDataInitializer.PASSWORD);
        assertNotNull(session.refreshCookie());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", session.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

        mockMvc.perform(get("/api/turn/current")
                        .header("Authorization", "Bearer " + session.refreshCookie().getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Réutilisation d'un token rotaté : la session active est révoquée")
    void rotatedTokenReuseRevokesActiveSession() throws Exception {
        String username = "authreuseuser";
        String password = "secret123";
        register(username, password);
        Session session = login(username, password);
        assertNotNull(session.refreshCookie());

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andReturn();
        Cookie active = refreshed.getResponse().getCookie("refresh_token");
        assertNotNull(active);

        // Sortir de la grace period de 3 s pour que l'ancien token soit traité comme une réutilisation.
        Thread.sleep(3_200);

        mockMvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

        mockMvc.perform(post("/api/auth/refresh").cookie(active))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @DisplayName("pseudo, photo et mot de passe changent sans X-Board-Id et révoquent le refresh")
    void profileUpdates() throws Exception {
        String password = "secret123";
        String nouveauMotDePasse = "secret456";
        register("profileuser", password);
        Session session = login("profileuser", password);
        String authorization = "Bearer " + session.token();

        mockMvc.perform(put("/api/profile/username")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"profileuser2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("profileuser2"));

        mockMvc.perform(put("/api/profile/avatar")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarUrl\":\"data:image/svg+xml;base64,PHN2Zy8+\"}"))
                .andExpect(status().isBadRequest());

        // Le type MIME annoncé est image/png mais le contenu n'est pas une image : les magic bytes refusent.
        mockMvc.perform(put("/api/profile/avatar")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarUrl\":\"data:image/png;base64,PGh0bWw+PGJvZHk+\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/profile/avatar")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarUrl\":\"" + AVATAR_PNG + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(AVATAR_PNG));

        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"mauvais\",\"newPassword\":\"" + nouveauMotDePasse + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + password + "\",\"newPassword\":\"" + nouveauMotDePasse + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh").cookie(session.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("profileuser2", password)))
                .andExpect(status().isUnauthorized());

        Session relogin = login("profileuser2", nouveauMotDePasse);
        assertNotNull(relogin.token());
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("profileuser2", nouveauMotDePasse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(AVATAR_PNG));

        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", "Bearer " + relogin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + nouveauMotDePasse + "\",\"newPassword\":\"court\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/profile/avatar")
                        .header("Authorization", "Bearer " + relogin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarUrl\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("profileuser2", nouveauMotDePasse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()));
    }
}
