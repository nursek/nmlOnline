package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import com.mg.nmlonline.mapper.BoardMapper;
import com.mg.nmlonline.mapper.PlayerMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService — gestion du compte User (création/maj/suppression)")
class AdminServiceTest {

    @Mock
    PlayerImportService playerImportService;
    @Mock
    PlayerService playerService;
    @Mock
    BoardService boardService;
    @Mock
    UserService userService;
    @Mock
    UserRepository userRepository;
    @Mock
    ResourceRepository resourceRepository;
    @Mock
    EntityManager entityManager;
    @Mock
    PlayerMapper playerMapper;
    @Mock
    BoardMapper boardMapper;

    @InjectMocks
    AdminService adminService;

    private static final String JSON = "{\"name\":\"lurio\",\"money\":0}";
    private static final String PLAYER_NAME = "lurio";
    private static final String PASSWORD = "s3cr3t";

    private Player newPlayer() {
        return new Player(PLAYER_NAME);
    }

    @Test
    @DisplayName("password fourni + user inexistant → crée un nouveau User (role USER), encode, save et lie")
    void shouldCreateNewUserWhenImportingWithPassword() throws Exception {
        PlayerImportService.PlayerDTO dto = new PlayerImportService.PlayerDTO();
        dto.name = PLAYER_NAME;
        Player player = newPlayer();
        Player persisted = newPlayer();
        persisted.setId(42L);

        when(playerImportService.parse(JSON)).thenReturn(dto);
        when(playerImportService.importPlayer(dto)).thenReturn(player);
        when(playerService.findByName(PLAYER_NAME)).thenReturn(null);
        when(playerService.save(any(Player.class))).thenReturn(persisted);
        when(boardService.getAllBoards()).thenReturn(java.util.List.of());
        when(userRepository.findByUsername(PLAYER_NAME)).thenReturn(null);
        when(userService.encodePassword(PASSWORD)).thenReturn("hashed:" + PASSWORD);
        User savedUser = new User();
        savedUser.setId(7L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Player result = adminService.importPlayer(JSON, PASSWORD);

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();
        assertEquals(PLAYER_NAME, created.getUsername());
        assertEquals("USER", created.getRole());
        assertEquals("hashed:" + PASSWORD, created.getPassword());
        assertEquals(7L, result.getUserId());
        verify(userService).encodePassword(PASSWORD);
    }

    @Test
    @DisplayName("password fourni + user existant → met à jour le mot de passe sans changer le role")
    void shouldUpdatePasswordWhenUserAlreadyExists() throws Exception {
        PlayerImportService.PlayerDTO dto = new PlayerImportService.PlayerDTO();
        dto.name = PLAYER_NAME;
        Player persisted = newPlayer();
        persisted.setId(42L);

        User existing = new User();
        existing.setId(7L);
        existing.setUsername(PLAYER_NAME);
        existing.setRole("USER");
        existing.setPassword("old-hash");

        when(playerImportService.parse(JSON)).thenReturn(dto);
        when(playerImportService.importPlayer(dto)).thenReturn(newPlayer());
        when(playerService.findByName(PLAYER_NAME)).thenReturn(null);
        when(playerService.save(any(Player.class))).thenReturn(persisted);
        when(boardService.getAllBoards()).thenReturn(java.util.List.of());
        when(userRepository.findByUsername(PLAYER_NAME)).thenReturn(existing);
        when(userService.encodePassword(PASSWORD)).thenReturn("new-hash");
        when(userRepository.save(existing)).thenReturn(existing);

        adminService.importPlayer(JSON, PASSWORD);

        verify(userRepository).save(existing);
        assertEquals("USER", existing.getRole());
        assertEquals("new-hash", existing.getPassword());
        assertEquals(7L, persisted.getUserId());
    }

    @Test
    @DisplayName("password blank → aucun compte User créé ni mis à jour")
    void shouldNotTouchUsersWhenPasswordBlank() throws Exception {
        PlayerImportService.PlayerDTO dto = new PlayerImportService.PlayerDTO();
        dto.name = PLAYER_NAME;
        Player persisted = newPlayer();
        persisted.setId(42L);

        when(playerImportService.parse(JSON)).thenReturn(dto);
        when(playerImportService.importPlayer(dto)).thenReturn(newPlayer());
        when(playerService.findByName(PLAYER_NAME)).thenReturn(null);
        when(playerService.save(any(Player.class))).thenReturn(persisted);
        when(boardService.getAllBoards()).thenReturn(java.util.List.of());

        Player result = adminService.importPlayer(JSON, "   ");

        assertNull(result.getUserId(), "Aucun userId ne doit être posé sans mot de passe");
        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).save(any(User.class));
        verify(userService, never()).encodePassword(any());
    }

    @Test
    @DisplayName("deletePlayer + user non-admin → supprime aussi le compte User")
    void shouldDeleteNonAdminUserOnPlayerDeletion() {
        Long playerId = 42L;
        Player player = newPlayer();
        player.setId(playerId);

        User user = new User();
        user.setId(7L);
        user.setUsername(PLAYER_NAME);
        user.setRole("USER");

        when(playerService.findById(playerId)).thenReturn(java.util.Optional.of(player));
        when(playerService.delete(playerId)).thenReturn(true);
        when(userRepository.findByUsername(PLAYER_NAME)).thenReturn(user);

        adminService.deletePlayer(playerId);

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deletePlayer + user ADMIN → ne supprime PAS le compte admin")
    void shouldNotDeleteAdminUserOnPlayerDeletion() {
        Long playerId = 42L;
        Player player = newPlayer();
        player.setId(playerId);

        User admin = new User();
        admin.setId(1L);
        admin.setUsername(PLAYER_NAME);
        admin.setRole("ADMIN");

        when(playerService.findById(playerId)).thenReturn(java.util.Optional.of(player));
        when(playerService.delete(playerId)).thenReturn(true);
        when(userRepository.findByUsername(PLAYER_NAME)).thenReturn(admin);

        adminService.deletePlayer(playerId);

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deletePlayer + aucun user associé → ne lève pas, ne supprime rien")
    void shouldTolerateMissingUserOnPlayerDeletion() {
        Long playerId = 42L;
        Player player = newPlayer();
        player.setId(playerId);

        when(playerService.findById(playerId)).thenReturn(java.util.Optional.of(player));
        when(playerService.delete(playerId)).thenReturn(true);
        when(userRepository.findByUsername(PLAYER_NAME)).thenReturn(null);

        adminService.deletePlayer(playerId);

        verify(userRepository, never()).delete(any());
    }
}