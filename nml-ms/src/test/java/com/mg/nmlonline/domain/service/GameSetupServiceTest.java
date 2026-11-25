package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.AdminPreparationStatusDto;
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

    private record Draft(Board board, PlayerDto dto, Player player) {
    }

    private Draft createTemplate(List<Integer> sectors, String prefix, String characterName) {
        String boardName = "Préparation " + UUID.randomUUID();
        Board board = boardService.saveBoard(emptyBoard(boardName), boardName);
        PlayerDto dto = gameSetupService.createTemplatePlayer(board.getId(),
                new AdminTemplatePlayerRequestDto(prefix + UUID.randomUUID(), "ORKS",
                        sectors, characterName));
        Player player = playerService.findById(dto.getId()).orElseThrow();
        return new Draft(board, dto, player);
    }

    private User linkAccount(PlayerDto dto) {
        User account = createAccount();
        adminService.linkAccount(dto.getId(), account.getUsername());
        return account;
    }

    @Test
    @DisplayName("Template, placement, lien de compte puis démarrage passent le tour à 1")
    void templatePlacementLinkAndStart() {
        String characterName = "Héros";
        Draft draft = createTemplate(List.of(1), "Prep", characterName);
        Player player = draft.player();

        assertNull(player.getUserId());
        assertEquals(GameSetupService.STARTING_MONEY, player.getStats().getMoney());
        assertEquals(100, unitRepository.findByPlayerIdAndSectorIsNull(player.getId()).size());
        assertTrue(unitRepository.findByPlayerIdAndSectorIsNull(player.getId()).stream()
                .allMatch(unit -> unit.getClasses().isEmpty()));
        assertEquals(3, player.getBuildings().size());
        assertTrue(player.getBuildings().stream().allMatch(building -> building.getSector() == null));
        assertNull(gameCharacterService.getCharacter(player.getId()).orElseThrow().getSector());

        User account = linkAccount(draft.dto());
        assertEquals(account.getId(), playerService.findById(player.getId()).orElseThrow().getUserId());

        preparationService.placeCharacter(account.getId(), draft.board().getId(), 1);
        assertNotNull(gameCharacterService.getCharacter(player.getId()).orElseThrow().getSector());

        Building building = player.getBuildings().getFirst();
        preparationService.placeBuilding(account.getId(), draft.board().getId(), building.getId(), 1);
        assertTrue(playerService.findById(player.getId()).orElseThrow().getBuildings().stream()
                .anyMatch(placed -> placed.getSector() != null));

        preparationService.distributeUnits(account.getId(), draft.board().getId(), 1, 10, List.of("LEGER"));
        List<Unit> units = unitRepository.findByPlayerId(player.getId());
        assertEquals(10, units.stream().filter(unit -> unit.getSector() != null).count());
        assertEquals(90, units.stream().filter(unit -> unit.getSector() == null).count());

        gameSetupService.startGame(draft.board().getId());

        Board started = boardService.getBoardById(draft.board().getId()).orElseThrow();
        assertEquals(1, started.getCurrentTurn());
        assertNotNull(started.getStartedAt());
        assertTrue(unitRepository.findByPlayerId(player.getId()).stream()
                .allMatch(unit -> !unit.getClasses().isEmpty()));
        assertThrows(IllegalStateException.class,
                () -> adminService.linkAccount(player.getId(), account.getUsername()));
        assertThrows(IllegalStateException.class,
                () -> preparationService.distributeUnits(account.getId(), draft.board().getId(), 1, 1,
                        List.of("LEGER")));
    }

    @Test
    @DisplayName("Les 100 unités peuvent être placées et classées pendant le tour 0")
    void allStartingUnitsCanBePlacedDuringPreparation() {
        Draft draft = createTemplate(List.of(1), "ToutesUnites", null);
        User account = linkAccount(draft.dto());

        preparationService.distributeUnits(account.getId(), draft.board().getId(), 1, 100, List.of("LEGER"));
        assertEquals(0, unitRepository.findByPlayerIdAndSectorIsNull(draft.dto().getId()).size());

        gameSetupService.startGame(draft.board().getId());
        assertEquals(0, unitRepository.findByPlayerIdAndSectorIsNull(draft.dto().getId()).size());
    }

    @Test
    @DisplayName("Les unités se répartissent en lots de classes différentes sur plusieurs secteurs")
    void startingUnitsCanBeSplitAcrossSectorsAndClasses() {
        Draft draft = createTemplate(List.of(1, 2, 3), "Lots", null);
        User account = linkAccount(draft.dto());

        preparationService.distributeUnits(account.getId(), draft.board().getId(), 1, 20, List.of("SNIPER"));
        preparationService.distributeUnits(account.getId(), draft.board().getId(), 2, 50, List.of("MASTODONTE"));
        preparationService.distributeUnits(account.getId(), draft.board().getId(), 3, 30, List.of("TIREUR"));

        List<Unit> units = unitRepository.findByPlayerId(draft.dto().getId());
        assertEquals(20, countBySectorAndClass(units, 1, UnitClass.SNIPER));
        assertEquals(50, countBySectorAndClass(units, 2, UnitClass.MASTODONTE));
        assertEquals(30, countBySectorAndClass(units, 3, UnitClass.TIREUR));
        assertEquals(0, unitRepository.findByPlayerIdAndSectorIsNull(draft.dto().getId()).size());
    }

    @Test
    @DisplayName("Les placements de préparation peuvent être retirés puis réinitialisés")
    void preparationPlacementsCanBeUndoneAndReset() {
        Draft draft = createTemplate(List.of(1, 2, 3), "Annule", null);
        User account = linkAccount(draft.dto());
        Long boardId = draft.board().getId();
        Player player = draft.player();

        preparationService.placeCharacter(account.getId(), boardId, 1);
        Building building = player.getBuildings().getFirst();
        preparationService.placeBuilding(account.getId(), boardId, building.getId(), 1);
        preparationService.distributeUnits(account.getId(), boardId, 1, 20, List.of("SNIPER"));
        preparationService.distributeUnits(account.getId(), boardId, 1, 10, List.of("TIREUR"));
        preparationService.distributeUnits(account.getId(), boardId, 2, 50, List.of("MASTODONTE"));
        preparationService.distributeUnits(account.getId(), boardId, 3, 20, List.of("TIREUR"));

        preparationService.setReady(account.getId(), boardId, true);
        assertTrue(playerService.findById(draft.dto().getId()).orElseThrow().isPreparationReady());

        preparationService.unplaceCharacter(account.getId(), boardId);
        assertNull(gameCharacterService.getCharacter(draft.dto().getId()).orElseThrow().getSector());
        assertFalse(playerService.findById(draft.dto().getId()).orElseThrow().isPreparationReady());

        preparationService.unplaceBuilding(account.getId(), boardId, building.getId());
        assertNull(building.getSector());

        preparationService.unplaceUnits(account.getId(), boardId, 1, 10, List.of("TIREUR"));
        List<Unit> units = unitRepository.findByPlayerId(draft.dto().getId());
        assertEquals(20, countBySectorAndClass(units, 1, UnitClass.SNIPER));
        assertEquals(0, countBySectorAndClass(units, 1, UnitClass.TIREUR));
        assertEquals(10, unitRepository.findByPlayerIdAndSectorIsNull(draft.dto().getId()).stream()
                .filter(unit -> unit.getClasses().isEmpty())
                .count());

        preparationService.resetPlacement(account.getId(), boardId);
        assertNull(gameCharacterService.getCharacter(draft.dto().getId()).orElseThrow().getSector());
        assertTrue(playerService.findById(draft.dto().getId()).orElseThrow().getBuildings().stream()
                .allMatch(placedBuilding -> placedBuilding.getSector() == null));
        assertTrue(unitRepository.findByPlayerId(draft.dto().getId()).stream()
                .allMatch(unit -> unit.getSector() == null && unit.getClasses().isEmpty()));
    }

    @Test
    @DisplayName("L'état de préparation expose les comptes, secteurs, placements et unités")
    void preparationStatusExposesReadiness() {
        Draft draft = createTemplate(List.of(1, 2), "Statut", null);
        User account = linkAccount(draft.dto());
        Long boardId = draft.board().getId();
        Player player = draft.player();

        preparationService.placeCharacter(account.getId(), boardId, 1);
        Building building = player.getBuildings().getFirst();
        preparationService.placeBuilding(account.getId(), boardId, building.getId(), 1);
        preparationService.distributeUnits(account.getId(), boardId, 1, 20, List.of("SNIPER"));
        preparationService.setReady(account.getId(), boardId, true);

        List<AdminPreparationStatusDto> statuses = gameSetupService.preparationStatus(boardId);
        assertEquals(1, statuses.size());
        AdminPreparationStatusDto status = statuses.getFirst();
        assertTrue(status.accountLinked());
        assertEquals(2, status.sectorCount());
        assertTrue(status.characterPlaced());
        assertEquals(1, status.buildingsPlaced());
        assertEquals(3, status.buildingsTotal());
        assertEquals(100, status.unitsTotal());
        assertEquals(80, status.unitsUnclassified());
        assertTrue(status.ready());
    }

    @Test
    @DisplayName("Le démarrage refuse un joueur sans compte relié")
    void startRefusesUnlinkedPlayer() {
        Draft draft = createTemplate(List.of(1), "SansCompte", null);

        assertThrows(IllegalStateException.class,
                () -> gameSetupService.startGame(draft.board().getId()));
    }

    @Test
    @DisplayName("Le démarrage refuse un joueur sans secteur de départ")
    void startRefusesPlayerWithoutSector() {
        Draft draft = createTemplate(List.of(1), "SansSecteur", null);
        gameSetupService.assignStartingSectors(draft.board().getId(), draft.dto().getId(), List.of());
        linkAccount(draft.dto());

        assertThrows(IllegalStateException.class,
                () -> gameSetupService.startGame(draft.board().getId()));
    }

    private static long countBySectorAndClass(List<Unit> units, int sectorNumber,
                                              UnitClass unitClass) {
        return units.stream()
                .filter(unit -> unit.getSector() != null
                        && unit.getSector().getNumber() == sectorNumber)
                .filter(unit -> unit.getClasses().contains(unitClass))
                .count();
    }

    private User createAccount() {
        User user = new User();
        user.setUsername("prep-" + UUID.randomUUID());
        user.setPassword(userService.encodePassword("password"));
        user.setRole("USER");
        userService.save(user);
        return user;
    }

    private Board emptyBoard(String name) {
        Board board = new Board();
        board.setName(name);
        for (int number = 1; number <= 3; number++) {
            board.addSector(new Sector(number, "Secteur " + number));
        }
        return board;
    }
}
