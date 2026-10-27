package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void getAllPlayers() throws Exception {
        mockMvc.perform(get("/api/admin/players").header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void getPlayerForEdit() throws Exception {
        Long playerId = playerRepository.findAll().getFirst().getId();
        mockMvc.perform(get("/api/admin/players/{id}", playerId).header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void getCatalog() throws Exception {
        mockMvc.perform(get("/api/admin/catalog").header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void getUnknownPlayerReturns404() throws Exception {
        mockMvc.perform(get("/api/admin/players/{id}", 999_999L).header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectNegativeMoney() throws Exception {
        Long playerId = playerRepository.findAll().getFirst().getId();
        mockMvc.perform(put("/api/admin/players/{id}/money", playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-1}")
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectResourceQuantityAboveCap() throws Exception {
        Long playerId = playerRepository.findAll().getFirst().getId();
        mockMvc.perform(post("/api/admin/players/{id}/resources", playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Caisse d'armes\",\"quantity\":1001}")
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addUnitsRejectsTooManyClasses() throws Exception {
        Long playerId = playerRepository.findByName("lurio").orElseThrow().getId();
        mockMvc.perform(post("/api/admin/players/{id}/units", playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"LARBIN\",\"classes\":[\"LEGER\",\"TIREUR\",\"SNIPER\"],"
                                + "\"quantity\":1,\"sectorNumber\":41}")
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addUnitsRejectsForeignSector() throws Exception {
        Long playerId = playerRepository.findByName("lurio").orElseThrow().getId();
        mockMvc.perform(post("/api/admin/players/{id}/units", playerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"LARBIN\",\"classes\":[\"LEGER\"],"
                                + "\"quantity\":1,\"sectorNumber\":1}")
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUnknownUnitReturns404() throws Exception {
        Long playerId = playerRepository.findByName("lurio").orElseThrow().getId();
        mockMvc.perform(delete("/api/admin/players/{id}/units/{unitId}", playerId, 999_999L)
                        .header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllExchanges() throws Exception {
        mockMvc.perform(get("/api/admin/exchanges").header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void getAllExchangesRejectsUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/admin/exchanges").param("status", "INCONNU")
                        .header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importPlayer() throws Exception {
        mockMvc.perform(multipart("/api/admin/players/import")
                        .file(jsonPart("file", "players/lurio.json"))
                        .header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void importBoard() throws Exception {
        mockMvc.perform(multipart("/api/admin/boards/import")
                        .file(jsonPart("file", "boards/board.json"))
                        .header("Authorization", bearer()))
                .andExpect(status().isOk());
    }

    private String bearer() {
        User admin = userRepository.findByUsername(TestDataInitializer.ADMIN);
        if (admin == null) {
            throw new IllegalStateException("Utilisateur admin de test introuvable");
        }
        return "Bearer " + jwtService.generateToken(admin, TOKEN_TTL_MS);
    }

    private static MockMultipartFile jsonPart(String param, String classpathLocation) throws Exception {
        return new MockMultipartFile(param, classpathLocation, "application/json",
                new ClassPathResource(classpathLocation).getInputStream().readAllBytes());
    }
}
