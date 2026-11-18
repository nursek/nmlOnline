package com.mg.nmlonline;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Test
    @DisplayName("pseudo, photo et mot de passe changent sans X-Board-Id et sans perdre la session")
    void profileUpdates() throws Exception {
        String password = "secret123";
        String nouveauMotDePasse = "secret456";
        register("profileuser", password);
        String authorization = login("profileuser", password);

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
    }
}
