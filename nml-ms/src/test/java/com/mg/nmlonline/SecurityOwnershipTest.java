package com.mg.nmlonline;

import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.service.JwtService;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Authentification et propriété sur les endpoints critiques (profil test). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityOwnershipTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private static final long TOKEN_TTL_MS = 3_600_000;

    private String tokenFor(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalStateException("Utilisateur de test introuvable : " + username);
        }
        return jwtService.generateToken(user, TOKEN_TTL_MS);
    }

    private Long playerIdForUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalStateException("Utilisateur de test introuvable : " + username);
        }
        Player player = playerRepository.findByUserId(user.getId()).orElse(null);
        if (player == null) {
            throw new IllegalStateException("Joueur de test introuvable pour l'utilisateur : " + username);
        }
        return player.getId();
    }

    @Test
    void unauthenticated_getPlayers_returns401() throws Exception {
        mockMvc.perform(get("/api/players"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_getAdminPlayers_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/players"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_getMyVehicles_returns401() throws Exception {
        mockMvc.perform(get("/api/vehicles/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_getCurrentTurn_returns401() throws Exception {
        mockMvc.perform(get("/api/turn/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_buyEquipment_returns401() throws Exception {
        mockMvc.perform(post("/api/players/equipment/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUser_cannotAccessAdminPlayers_returns403() throws Exception {
        String token = tokenFor(TestDataInitializer.USER_1);

        mockMvc.perform(get("/api/admin/players")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUser_cannotAccessOtherPlayerCharacter_returns403() throws Exception {
        String token = tokenFor(TestDataInitializer.USER_1);
        Long otherPlayerId = playerIdForUser(TestDataInitializer.USER_2);

        mockMvc.perform(get("/api/characters/player/" + otherPlayerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUser_cannotAccessOtherPlayerHeadquarters_returns403() throws Exception {
        String token = tokenFor(TestDataInitializer.USER_1);
        Long otherPlayerId = playerIdForUser(TestDataInitializer.USER_2);

        mockMvc.perform(get("/api/buildings/headquarters/" + otherPlayerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
