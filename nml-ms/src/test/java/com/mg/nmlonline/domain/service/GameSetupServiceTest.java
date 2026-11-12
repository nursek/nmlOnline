package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.AdminTemplatePlayerRequestDto;
import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@Transactional
@DisplayName("GameSetupService — tour 0 de bout en bout")
class GameSetupServiceTest {

    @Autowired
    private BoardService boardService;
    @Autowired
    private GameSetupService gameSetupService;
    @Autowired
    private PreparationService preparationService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private GameCharacterService gameCharacterService;
    @Autowired
    private UnitRepository unitRepository;
    @Autowired
    private UserService userService;

    @Test
    @DisplayName("Template, placement, lien de compte puis démarrage passent le tour à 1")
    void templatePlacementLinkAndStart() {
        String boardName = "Préparation " + UUID.randomUUID();
        Board board = boardService.saveBoard(board(boardName), boardName);
        String playerName = "Prep" + UUID.randomUUID().toString().substring(0, 8);

        PlayerDto dto = gameSetupService.createTemplatePlayer(board.getId(),
                new AdminTemplatePlayerRequestDto(playerName, "ORKS",
                        List.of(1), "Héros " + playerName));

        Player player = playerService.findById(dto.getId()).orElseThrow();
        assertNull(player.getUserId());
        assertEquals(GameSetupService.STARTING_MONEY, player.getStats().getMoney());
        assertEquals(100, unitRepository.findByPlayerIdAndSectorIsNull(player.getId()).size());
        assertTrue(unitRepository.findByPlayerIdAndSectorIsNull(player.getId()).stream()
                .allMatch(unit -> unit.getClasses().isEmpty()));
        assertEquals(3, player.getBuildings().size());
        assertTrue(player.getBuildings().stream().allMatch(building -> building.getSector() == null));
        GameCharacter character = gameCharacterService.getCharacter(player.getId()).orElseThrow();
        assertNull(character.getSector());

        User account = createAccount();
        adminService.linkAccount(player.getId(), account.getUsername());
        assertEquals(account.getId(), playerService.findById(player.getId()).orElseThrow().getUserId());

        preparationService.placeCharacter(account.getId(), board.getId(), 1);
        assertNotNull(gameCharacterService.getCharacter(player.getId()).orElseThrow().getSector());

        Building building = player.getBuildings().getFirst();
        preparationService.placeBuilding(account.getId(), board.getId(), building.getId(), 1);
        assertTrue(playerService.findById(player.getId()).orElseThrow().getBuildings().stream()
                .anyMatch(placed -> placed.getSector() != null));

        preparationService.distributeUnits(account.getId(), board.getId(), 1, 10, List.of("LEGER"));
        List<Unit> units = unitRepository.findByPlayerId(player.getId());
        assertEquals(10, units.stream().filter(unit -> unit.getSector() != null).count());
        assertEquals(90, units.stream().filter(unit -> unit.getSector() == null).count());

        gameSetupService.startGame(board.getId());

        Board started = boardService.getBoardById(board.getId()).orElseThrow();
        assertEquals(1, started.getCurrentTurn());
        assertNotNull(started.getStartedAt());
        assertTrue(unitRepository.findByPlayerId(player.getId()).stream()
                .allMatch(unit -> !unit.getClasses().isEmpty()));
        assertThrows(IllegalStateException.class,
                () -> adminService.linkAccount(player.getId(), account.getUsername()));
        assertThrows(IllegalStateException.class,
                () -> preparationService.distributeUnits(account.getId(), board.getId(), 1, 1,
                        List.of("LEGER")));
    }

    @Test
    @DisplayName("Les 100 unités peuvent être placées et classées pendant le tour 0")
    void allStartingUnitsCanBePlacedDuringPreparation() {
        String boardName = "Préparation " + UUID.randomUUID();
        Board board = boardService.saveBoard(board(boardName), boardName);
        PlayerDto dto = gameSetupService.createTemplatePlayer(board.getId(),
                new AdminTemplatePlayerRequestDto("ToutesUnites" + UUID.randomUUID(), "ORKS",
                        List.of(1), null));
        User account = createAccount();
        adminService.linkAccount(dto.getId(), account.getUsername());

        preparationService.distributeUnits(account.getId(), board.getId(), 1, 100, List.of("LEGER"));
        assertEquals(0, unitRepository.findByPlayerIdAndSectorIsNull(dto.getId()).size());

        gameSetupService.startGame(board.getId());
        assertEquals(0, unitRepository.findByPlayerIdAndSectorIsNull(dto.getId()).size());
    }

    @Test
    @DisplayName("Le démarrage refuse un joueur sans compte relié")
    void startRefusesUnlinkedPlayer() {
        String boardName = "Préparation " + UUID.randomUUID();
        Board board = boardService.saveBoard(board(boardName), boardName);
        gameSetupService.createTemplatePlayer(board.getId(),
                new AdminTemplatePlayerRequestDto("SansCompte" + UUID.randomUUID(), "ORKS",
                        List.of(1), null));

        assertThrows(IllegalStateException.class, () -> gameSetupService.startGame(board.getId()));
    }

    @Test
    @DisplayName("Le démarrage refuse un joueur sans secteur de départ")
    void startRefusesPlayerWithoutSector() {
        String boardName = "Préparation " + UUID.randomUUID();
        Board board = boardService.saveBoard(board(boardName), boardName);
        PlayerDto dto = gameSetupService.createTemplatePlayer(board.getId(),
                new AdminTemplatePlayerRequestDto("SansSecteur" + UUID.randomUUID(), "ORKS",
                        List.of(1), null));
        gameSetupService.assignStartingSectors(board.getId(), dto.getId(), List.of());
        User account = createAccount();
        adminService.linkAccount(dto.getId(), account.getUsername());

        assertThrows(IllegalStateException.class, () -> gameSetupService.startGame(board.getId()));
    }

    private User createAccount() {
        User user = new User();
        user.setUsername("prep-" + UUID.randomUUID());
        user.setPassword(userService.encodePassword("password"));
        user.setRole("USER");
        userService.save(user);
        return user;
    }

    private Board board(String name) {
        Board board = new Board();
        board.setName(name);
        for (int number = 1; number <= 3; number++) {
            board.addSector(new Sector(number, "Secteur " + number));
        }
        return board;
    }
}
