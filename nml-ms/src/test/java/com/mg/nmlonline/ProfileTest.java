package com.mg.nmlonline;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EmbeddedPostgresTest
@AutoConfigureMockMvc
@DisplayName("Profil utilisateur")
class ProfileTest {

    private static final String AVATAR_PNG =
            "data:image/png;base64,"
                    + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

    @Autowired
    private MockMvc mockMvc;

    private record Session(String authorization, Cookie refreshCookie) {}

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
    }

    private Session login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        return new Session("Bearer " + token, result.getResponse().getCookie("refresh_token"));
    }

    @Test
    @DisplayName("pseudo, photo et mot de passe changent sans X-Board-Id et révoquent le refresh")
    void profileUpdates() throws Exception {
        String password = "secret123";
        String nouveauMotDePasse = "secret456";
        register("profileuser", password);
        Session session = login("profileuser", password);
        String authorization = session.authorization();

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
                        .content("{\"username\":\"profileuser2\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult relogin = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"profileuser2\",\"password\":\"" + nouveauMotDePasse + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(AVATAR_PNG))
                .andReturn();
        String token = JsonPath.read(relogin.getResponse().getContentAsString(), "$.token");
        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + nouveauMotDePasse + "\",\"newPassword\":\"court\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/profile/avatar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatarUrl\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"profileuser2\",\"password\":\"" + nouveauMotDePasse + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()));
    }
}
