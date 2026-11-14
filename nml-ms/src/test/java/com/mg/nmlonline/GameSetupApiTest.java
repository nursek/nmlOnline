package com.mg.nmlonline;

import com.jayway.jsonpath.JsonPath;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EmbeddedPostgresTest
@AutoConfigureMockMvc
@DisplayName("API de préparation — template, verrou de phase et démarrage")
class GameSetupApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BoardService boardService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    @Test
    void templateThenStartLocksPreparation() throws Exception {
        String boardName = "API préparation " + UUID.randomUUID();
        Board board = new Board();
        board.setName(boardName);
        for (int number = 1; number <= 3; number++) {
            board.addSector(new Sector(number, "Secteur " + number));
        }
        board = boardService.saveBoard(board, boardName);

        User admin = userRepository.findByUsername(TestDataInitializer.ADMIN);
        String adminToken = jwtService.generateToken(admin, 3_600_000L);

        String body = "{\"name\":\"ApiPlayer\",\"race\":\"ORKS\",\"sectorNumbers\":[1]}";
        MvcResult created = mockMvc.perform(post("/api/admin/players/template")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ApiPlayer"))
                .andReturn();
        Number playerId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/admin/players/" + playerId + "/account")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + TestDataInitializer.USER_1 + "\"}"))
                .andExpect(status().isNoContent());
        User playerUser = userRepository.findByUsername(TestDataInitializer.USER_1);
        String playerToken = jwtService.generateToken(playerUser, 3_600_000L);

        mockMvc.perform(post("/api/players/equipment/buy")
                        .header("Authorization", "Bearer " + playerToken)
                        .header("X-Board-Id", board.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/preparation/units")
                        .header("Authorization", "Bearer " + playerToken)
                        .header("X-Board-Id", board.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectorNumber\":1,\"quantity\":1,\"classes\":[\"LEGER\"]}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/admin/board/start")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/admin/players/" + playerId + "/account")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un MJ n'est pas un joueur ; l'admin plateforme n'est pas MJ")
    void mjAndPlayerAreSeparate() throws Exception {
        String boardName = "API rôles " + UUID.randomUUID();
        Board board = new Board();
        board.setName(boardName);
        for (int number = 1; number <= 3; number++) {
            board.addSector(new Sector(number, "Secteur " + number));
        }
        board = boardService.saveBoard(board, boardName);

        User admin = userRepository.findByUsername(TestDataInitializer.ADMIN);
        User playerUser = userRepository.findByUsername(TestDataInitializer.USER_1);
        User mjUser = userRepository.findByUsername(TestDataInitializer.USER_2);
        String adminToken = jwtService.generateToken(admin, 3_600_000L);

        MvcResult created = mockMvc.perform(post("/api/admin/players/template")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"JoueurRôle\",\"race\":\"ORKS\",\"sectorNumbers\":[1]}"))
                .andExpect(status().isOk())
                .andReturn();
        Number playerId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/admin/players/" + playerId + "/account")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + playerUser.getUsername() + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/admin/instances/" + board.getId() + "/admins/" + playerUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString()))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/admin/instances/" + board.getId() + "/admins/" + mjUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString()))
                .andExpect(status().isNoContent());

        MvcResult created2 = mockMvc.perform(post("/api/admin/players/template")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"JoueurRôle2\",\"race\":\"ORKS\",\"sectorNumbers\":[2]}"))
                .andExpect(status().isOk())
                .andReturn();
        Number playerId2 = JsonPath.read(created2.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/admin/players/" + playerId2 + "/account")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + mjUser.getUsername() + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/admin/instances/" + board.getId() + "/admins/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", board.getId().toString()))
                .andExpect(status().isConflict());
    }
}
