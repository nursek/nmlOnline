package com.mg.nmlonline;

import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.EMBEDDED;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boote le profil {@code prod} sur un PostgreSQL 14 embarqué : couvre la configuration prod que le
 * profil {@code test} ne voit pas (beans {@code @Profile("prod")}, variables d'environnement).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = EMBEDDED)
class ProdBootParityTest {

    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BoardRepository boardRepository;

    @DynamicPropertySource
    static void prodConfiguration(DynamicPropertyRegistry registry) {
        registry.add("app.admin.password", () -> ADMIN_PASSWORD);
        // Peuple unités/bâtiments/personnages : sans ça le test d'endpoint ne prouve rien.
        registry.add("app.import-demo-data", () -> "true");
    }

    @Test
    void prodAdminBootstrapLoginWorks() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void registrationIsClosedInProd() throws Exception {
        mockMvc.perform(get("/api/auth/registration-mode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.inviteRequired").value(true));

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"produser\",\"password\":\"secret123\"}"))
                .andExpect(status().isForbidden());
    }

    /** Le DTO joueur parcourt des relations LAZY : 500 si le mapping sort de la transaction (OSIV). */
    @Test
    void adminApiListsPlayersOnMigratedSchema() throws Exception {
        User admin = userRepository.findByUsername("admin");
        String token = DemoFixtures.tokenFor(admin, jwtService);

        Long boardId = DemoFixtures.mainBoardId(boardRepository);
        mockMvc.perform(get("/api/admin/players")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", String.valueOf(boardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)));
    }
}
