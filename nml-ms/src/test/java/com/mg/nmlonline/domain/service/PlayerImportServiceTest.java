package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.EquipmentRepository;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import com.mg.nmlonline.domain.service.PlayerImportService.CharacterDTO;
import com.mg.nmlonline.domain.service.PlayerImportService.PlayerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests unitaires de {@link PlayerImportService#importCharacter} : création du
 * GameCharacter, liaison au Player, rattachement au secteur du Board.
 * Isolation maximale (Mockito) — aucun contexte Spring requis.
 */
@DisplayName("PlayerImportService — importCharacter")
class PlayerImportServiceTest {

    private PlayerImportService service;

    @BeforeEach
    void setUp() {
        // importCharacter n'utilise ni les équipements ni les ressources ni les
        // stats : mocks inertes suffisent.
        service = new PlayerImportService(
                mock(PlayerStatsService.class),
                mock(EquipmentRepository.class),
                mock(ResourceRepository.class)
        );
    }

    @Test
    @DisplayName("dto.character null → retourne null sans toucher au joueur")
    void shouldReturnNullWhenCharacterNull() {
        Player player = newPlayerWithId(1L);
        Board board = new Board();

        GameCharacter result = service.importCharacter(new PlayerDTO(), player, board);

        assertNull(result);
        assertNull(player.getCharacter());
    }

    @Test
    @DisplayName("character.name null → retourne null")
    void shouldReturnNullWhenNameNull() {
        Player player = newPlayerWithId(1L);
        Board board = new Board();
        PlayerDTO dto = new PlayerDTO();
        dto.character = new CharacterDTO();
        dto.character.name = null;

        GameCharacter result = service.importCharacter(dto, player, board);

        assertNull(result);
        assertNull(player.getCharacter());
    }

    @Test
    @DisplayName("Cas nominal : crée le personnage, lie au joueur avec son playerId")
    void shouldCreateCharacterLinkedToPlayer() {
        Player player = newPlayerWithId(7L);
        Board board = new Board();
        PlayerDTO dto = characterDto("Boss", 0, 10, 20, 30, 40, 50, 60);

        GameCharacter result = service.importCharacter(dto, player, board);

        assertNotNull(result);
        assertEquals("Boss", result.getName());
        assertEquals(10.0, result.getAttack());
        assertEquals(20.0, result.getPdf());
        assertEquals(30.0, result.getPdc());
        assertEquals(40.0, result.getDefense());
        assertEquals(50.0, result.getArmor());
        assertEquals(60.0, result.getEvasion());
        assertEquals(7L, result.getPlayerId());
        assertSame(result, player.getCharacter());
    }

    @Test
    @DisplayName("sectorNumber valide → rattaché au secteur du Board")
    void shouldAttachCharacterToExistingSector() {
        Player player = newPlayerWithId(7L);
        Board board = new Board();
        Sector sector = new Sector(12);
        board.addSector(sector);
        PlayerDTO dto = characterDto("Boss", 12, 0, 0, 0, 0, 0, 0);

        GameCharacter result = service.importCharacter(dto, player, board);

        assertNotNull(result);
        assertSame(sector, result.getSector());
    }

    @Test
    @DisplayName("sectorNumber inexistant → sector null, ne plante pas (warning loggé)")
    void shouldLeaveSectorNullWhenMissing() {
        Player player = newPlayerWithId(7L);
        Board board = new Board();
        // Aucun secteur 999 dans le board.
        PlayerDTO dto = characterDto("Boss", 999, 0, 0, 0, 0, 0, 0);

        GameCharacter result = service.importCharacter(dto, player, board);

        assertNotNull(result);
        assertNull(result.getSector());
    }

    // === Helpers ===

    private Player newPlayerWithId(Long id) {
        Player player = new Player("Joueur");
        player.setId(id);
        return player;
    }

    private PlayerDTO characterDto(String name, int sectorNumber,
                                   double atk, double pdf, double pdc,
                                   double def, double armor, double evasion) {
        PlayerDTO dto = new PlayerDTO();
        dto.character = new CharacterDTO();
        dto.character.name = name;
        dto.character.sectorNumber = sectorNumber;
        dto.character.baseAttack = atk;
        dto.character.basePdf = pdf;
        dto.character.basePdc = pdc;
        dto.character.baseDefense = def;
        dto.character.baseArmor = armor;
        dto.character.baseEvasion = evasion;
        return dto;
    }
}