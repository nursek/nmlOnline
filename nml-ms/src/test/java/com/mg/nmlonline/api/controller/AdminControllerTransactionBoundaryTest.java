package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
        mockMvc.perform(get("/api/admin/players").header("Authorization", bearer()))
                .andExpect(status().isOk());
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
