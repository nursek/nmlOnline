package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Les endpoints admin qui construisent un DTO depuis des entitǸs doivent tenir sans
 * open-in-view (dǸsactivǸ en test comme en prod) : le mapping doit se faire dans la
 * transaction du service, pas dans le controller apr��s commit.
 */
@EmbeddedPostgresTest
@AutoConfigureMockMvc
class AdminControllerTransactionBoundaryTest {

    private static final long TOKEN_TTL_MS = 3_600_000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getAllPlayers() throws Exception {
        mockMvc.perform(get("/api/admin/players").header("Authorization", bearer(TestDataInitializer.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllExchanges() throws Exception {
        mockMvc.perform(get("/api/admin/exchanges").header("Authorization", bearer(TestDataInitializer.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllExchangesRejectsUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/admin/exchanges").param("status", "INCONNU")
                        .header("Authorization", bearer(TestDataInitializer.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllianceHistory() throws Exception {
        mockMvc.perform(get("/api/admin/alliances")
                        .header("Authorization", bearer(TestDataInitializer.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void importPlayer() throws Exception {
        mockMvc.perform(multipart("/api/admin/players/import")
                        .file(jsonPart("file", "players/lurio.json"))
                        .header("Authorization", bearer(TestDataInitializer.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void importBoard() throws Exception {
        importMainBoard();
    }

    @Test
    void adminSeesFullSectorDetailOnBoards() throws Exception {
        importMainBoard();

        mockMvc.perform(get("/api/boards").header("Authorization", bearer(TestDataInitializer.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sectors['1'].income").value(4019.0))
                .andExpect(jsonPath("$[0].sectors['1'].army").isArray());
    }

    @Test
    void playerSeesRestrictedSectorDetailOnBoards() throws Exception {
        importMainBoard();

        mockMvc.perform(get("/api/boards").header("Authorization", bearer(TestDataInitializer.USER_1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sectors['1'].neighbors").exists())
                .andExpect(jsonPath("$[0].sectors['1'].income").doesNotExist())
                .andExpect(jsonPath("$[0].sectors['1'].army").doesNotExist());
    }

    private void importMainBoard() throws Exception {
        mockMvc.perform(multipart("/api/admin/boards/import")
                        .file(jsonPart("file", "boards/board.json"))
                        .header("Authorization", bearer(TestDataInitializer.ADMIN)))
                .andExpect(status().isOk());
    }

    private String bearer(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalStateException("Utilisateur de test introuvable : " + username);
        }
        return "Bearer " + jwtService.generateToken(user, TOKEN_TTL_MS);
    }

    private static MockMultipartFile jsonPart(String param, String classpathLocation) throws Exception {
        return new MockMultipartFile(param, classpathLocation, "application/json",
                new ClassPathResource(classpathLocation).getInputStream().readAllBytes());
    }
}
