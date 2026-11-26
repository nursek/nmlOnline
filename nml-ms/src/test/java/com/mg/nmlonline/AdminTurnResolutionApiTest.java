package com.mg.nmlonline;

import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EmbeddedPostgresTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("API admin — session de résolution pas-à-pas")
class AdminTurnResolutionApiTest extends DemoDatabaseReset {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private JwtService jwtService;

    private String adminBearer() {
        return DemoFixtures.bearer(userRepository, jwtService, TestDataInitializer.ADMIN);
    }

    private String boardId() {
        return String.valueOf(DemoFixtures.mainBoardId(boardRepository));
    }

    @Test
    void sessionLifecycleExposesConflictsAndRefusesDoubleStart() throws Exception {
        String bearer = adminBearer();
        mockMvc.perform(post("/api/admin/turn/resolve/start")
                        .header("Authorization", bearer)
                        .header("X-Board-Id", boardId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        try {
            mockMvc.perform(post("/api/admin/turn/resolve/start")
                            .header("Authorization", bearer)
                            .header("X-Board-Id", boardId()))
                    .andExpect(status().isConflict());

            mockMvc.perform(get("/api/admin/turn/resolve/state")
                            .header("Authorization", bearer)
                            .header("X-Board-Id", boardId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true));

            mockMvc.perform(post("/api/admin/turn/resolve/resolve-battle")
                            .param("conflictId", "999999")
                            .header("Authorization", bearer)
                            .header("X-Board-Id", boardId()))
                    .andExpect(status().isNotFound());
        } finally {
            mockMvc.perform(delete("/api/admin/turn/resolve")
                            .header("Authorization", bearer)
                            .header("X-Board-Id", boardId()))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/admin/turn/resolve/state")
                        .header("Authorization", bearer)
                        .header("X-Board-Id", boardId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
