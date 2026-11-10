package com.mg.nmlonline;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.board.BoardAdmin;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.infrastructure.repository.BoardAdminRepository;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EmbeddedPostgresTest
@AutoConfigureMockMvc
@DisplayName("Multi-parties — isolation des données et rôle MJ par partie")
class InstanceAccessTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private BoardAdminRepository boardAdminRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    private Board secondBoard;
    private User gameMaster;

    @AfterEach
    void cleanup() {
        if (secondBoard != null && secondBoard.getId() != null && boardRepository.existsById(secondBoard.getId())) {
            boardRepository.deleteById(secondBoard.getId());
        }
        if (gameMaster != null && gameMaster.getId() != null) {
            userRepository.deleteById(gameMaster.getId());
        }
    }

    @Test
    @DisplayName("un compte joue sur deux parties, est MJ sur une seule, et rien ne fuit")
    void playerOnTwoInstancesIsAdminOnOne() throws Exception {
        Long firstBoardId = boardRepository.findAll().getFirst().getId();
        secondBoard = createBoard("Partie isolée");
        Long secondBoardId = secondBoard.getId();

        gameMaster = createUser("mj_isole");
        Long userId = gameMaster.getId();
        Player onFirst = createPlayer("JoueurPartie1", userId, firstBoardId);
        Player onSecond = createPlayer("JoueurPartie2", userId, secondBoardId);
        boardAdminRepository.save(new BoardAdmin(secondBoardId, userId));
        String token = jwtService.generateToken(gameMaster, 3_600_000L);

        mockMvc.perform(get("/api/instances").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].role".formatted(secondBoardId)).value("MJ"))
                .andExpect(jsonPath("$[?(@.id == %d)].role".formatted(firstBoardId)).value("PLAYER"))
                .andExpect(jsonPath("$[?(@.id == %d)].playerName".formatted(secondBoardId))
                        .value("JoueurPartie2"));

        mockMvc.perform(get("/api/players/me")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", String.valueOf(firstBoardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(onFirst.getName()));

        mockMvc.perform(get("/api/players/me")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", String.valueOf(secondBoardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(onSecond.getName()));

        assertNotEquals(onFirst.getId(), onSecond.getId());

        mockMvc.perform(get("/api/admin/players")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", String.valueOf(secondBoardId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/players")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", String.valueOf(firstBoardId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un admin plateforme délègue un MJ à une seule partie")
    void platformAdminDelegatesGameMaster() throws Exception {
        Long boardId = boardRepository.findAll().getFirst().getId();
        gameMaster = createUser("mj_delegue");
        String adminToken = jwtService.generateToken(
                userRepository.findByUsername(TestDataInitializer.ADMIN), 3_600_000L);

        mockMvc.perform(put("/api/admin/instances/{boardId}/admins/{userId}", boardId, gameMaster.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", String.valueOf(boardId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/players")
                        .header("Authorization", "Bearer " + jwtService.generateToken(gameMaster, 3_600_000L))
                        .header("X-Board-Id", String.valueOf(boardId)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/instances/{boardId}/admins/{userId}", boardId, gameMaster.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", String.valueOf(boardId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/players")
                        .header("Authorization", "Bearer " + jwtService.generateToken(gameMaster, 3_600_000L))
                        .header("X-Board-Id", String.valueOf(boardId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("une partie ne soigne ni ne liste les joueurs de l autre")
    void boardsDoNotLeakPlayers() throws Exception {
        Long firstBoardId = boardRepository.findAll().getFirst().getId();
        secondBoard = createBoard("Partie étanche");
        Player onlyOnSecond = createPlayer("JoueurEtanche", null, secondBoard.getId());
        String token = jwtService.generateToken(
                userRepository.findByUsername(TestDataInitializer.USER_1), 3_600_000L);

        mockMvc.perform(get("/api/players")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", String.valueOf(secondBoard.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name == 'JoueurEtanche')]").exists());

        mockMvc.perform(get("/api/players")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", String.valueOf(firstBoardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.name == 'JoueurEtanche')]").doesNotExist());

        playerRepository.deleteById(onlyOnSecond.getId());
    }

    @Test
    @DisplayName("sans en-tête : 400 avec plusieurs parties, 400 si invalide, 404 si inconnue")
    void headerValidationFailsClosed() throws Exception {
        secondBoard = createBoard("Partie en-tête");
        String token = jwtService.generateToken(
                userRepository.findByUsername(TestDataInitializer.USER_1), 3_600_000L);

        mockMvc.perform(get("/api/turn/current").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/turn/current")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", "abc"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/turn/current")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", "999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("un MJ ne peut pas éditer un joueur d une autre partie")
    void gameMasterCannotEditPlayerFromAnotherBoard() throws Exception {
        Long firstBoardId = boardRepository.findAll().getFirst().getId();
        secondBoard = createBoard("Partie MJ");
        gameMaster = createUser("mj_scope");
        Player outsider = createPlayer("JoueurAutrePartie", null, firstBoardId);
        boardAdminRepository.save(new BoardAdmin(secondBoard.getId(), gameMaster.getId()));
        String token = jwtService.generateToken(gameMaster, 3_600_000L);

        mockMvc.perform(put("/api/admin/players/{id}/money", outsider.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", String.valueOf(secondBoard.getId()))
                        .contentType("application/json")
                        .content("{\"amount\":999999}"))
                .andExpect(status().isForbidden());

        playerRepository.deleteById(outsider.getId());
    }

    @Test
    @DisplayName("un MJ ne peut pas s.octroyer des droits plateforme")
    void gameMasterCannotUsePlatformEndpoints() throws Exception {
        secondBoard = createBoard("Partie escalade");
        gameMaster = createUser("mj_escalade");
        boardAdminRepository.save(new BoardAdmin(secondBoard.getId(), gameMaster.getId()));
        String token = jwtService.generateToken(gameMaster, 3_600_000L);

        mockMvc.perform(put("/api/admin/instances/{boardId}/admins/{userId}",
                        secondBoard.getId(), gameMaster.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("X-Board-Id", String.valueOf(secondBoard.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("supprimer une partie supprime ses participations, jamais les comptes")
    void deletingBoardRemovesOnlyItsParticipations() throws Exception {
        secondBoard = createBoard("Partie à supprimer");
        Player doomed = createPlayer("JoueurSupprime", null, secondBoard.getId());
        String adminToken = jwtService.generateToken(
                userRepository.findByUsername(TestDataInitializer.ADMIN), 3_600_000L);

        mockMvc.perform(delete("/api/boards/{id}", secondBoard.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Board-Id", String.valueOf(secondBoard.getId())))
                .andExpect(status().isNoContent());

        assertFalse(boardRepository.existsById(secondBoard.getId()));
        assertFalse(playerRepository.existsById(doomed.getId()));
        secondBoard = null;
    }

    private Board createBoard(String name) {
        Board board = new Board();
        board.setName(name);
        Sector sector = new Sector();
        sector.setBoard(board);
        sector.setNumber(1);
        sector.setName("Secteur 1");
        board.getSectorsList().add(sector);
        return boardRepository.save(board);
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("{noop}irrelevant");
        user.setRole("USER");
        return userRepository.save(user);
    }

    private Player createPlayer(String name, Long userId, Long boardId) {
        Player player = new Player(name);
        player.setUserId(userId);
        player.setBoardId(boardId);
        return playerRepository.save(player);
    }
}
