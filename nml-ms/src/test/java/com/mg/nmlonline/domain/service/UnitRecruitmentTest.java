package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.BuyUnitRequestDto;
import com.mg.nmlonline.api.dto.PlayerActionDto;
import com.mg.nmlonline.api.dto.UnitCatalogEntryDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import com.mg.nmlonline.domain.model.action.PlayerActionType;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@Transactional
@DisplayName("Recrutement — achat, quota, placement et réserve")
class UnitRecruitmentTest {

    @Autowired
    private UnitService unitService;

    @Autowired
    private PlayerActionService playerActionService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private TurnService turnService;

    @Autowired
    private ReserveUnitPlacer reserveUnitPlacer;

    @Autowired
    private MovementAdminService movementAdminService;

    @Autowired
    private TurnLock turnLock;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearTurnCache() {
        turnService.invalidateTurnCache();
    }

    @Test
    @DisplayName("Achat de 2 LARBIN au tour 2 : classe choisie, débit, actions et réserve")
    void buyLarbinsAtTurnTwo() {
        Player player = fundedPlayer(10000.0);
        Board board = board();
        setTurn(board, 2);

        List<UnitDto> created = unitService.buyUnitsDto(player.getUserId(),
                List.of(item("LARBIN", "LEGER", 2)));

        assertEquals(2, created.size());
        UnitDto first = created.getFirst();
        assertEquals("LARBIN", first.getType().getName());
        assertEquals(0.0, first.getExperience(), 0.001, "LARBIN acheté = expérience 0");
        assertEquals(1, first.getClasses().size());
        assertEquals("LEGER", first.getClasses().getFirst().getName());
        assertEquals(9200.0, reload(player).getStats().getMoney(), 0.001);

        assertEquals(2, unitService.getReserveUnitsDto(player.getUserId()).size());
        assertEquals(2, playerActionService.getCurrentTurnActions(player.getUserId()).stream()
                .filter(a -> a.getType() == PlayerActionType.BUY_UNIT)
                .count());
    }

    @Test
    @DisplayName("VOYOU refusé avant le tour 5, disponible ensuite")
    void voyouAvailability() {
        Player player = fundedPlayer(10000.0);
        Board board = board();
        setTurn(board, 2);

        assertThrows(IllegalStateException.class, () -> unitService.buyUnits(player.getUserId(),
                List.of(item("VOYOU", "LEGER", 1))));

        setTurn(board, 5);
        List<UnitDto> created = unitService.buyUnitsDto(player.getUserId(),
                List.of(item("VOYOU", "TIREUR", 1)));
        assertEquals("VOYOU", created.getFirst().getType().getName());
        assertEquals(2.0, created.getFirst().getExperience(), 0.001);
    }

    @Test
    @DisplayName("Quota par tour : 21 LARBIN refusés au tour 2, 30 acceptés au tour 5")
    void perTurnQuota() {
        Player player = fundedPlayer(100000.0);
        Board board = board();
        setTurn(board, 2);

        assertThrows(IllegalStateException.class, () -> unitService.buyUnits(player.getUserId(),
                List.of(item("LARBIN", "ELEMENTAIRE", 21))));

        setTurn(board, 5);
        assertEquals(30, unitService.buyUnitsDto(player.getUserId(),
                List.of(item("LARBIN", "ELEMENTAIRE", 30))).size());
        assertThrows(IllegalStateException.class, () -> unitService.buyUnits(player.getUserId(),
                List.of(item("LARBIN", "ELEMENTAIRE", 1))));
    }

    @Test
    @DisplayName("Fonds insuffisants → InsufficientFundsException")
    void insufficientFunds() {
        Player player = fundedPlayer(100.0);
        Board board = board();
        setTurn(board, 2);

        assertThrows(InsufficientFundsException.class, () -> unitService.buyUnits(player.getUserId(),
                List.of(item("LARBIN", "LEGER", 1))));
    }

    @Test
    @DisplayName("Placement sur secteur possédé, puis annulation du placement et de l'achat")
    void placeThenUndo() {
        Player player = fundedPlayer(10000.0);
        Board board = board();
        Sector sector = ownedSector(player, board);
        setTurn(board, 2);

        UnitDto bought = unitService.buyUnitsDto(player.getUserId(),
                List.of(item("LARBIN", "LEGER", 1))).getFirst();
        long unitId = bought.getId().longValue();

        unitService.placeUnitDto(player.getUserId(), unitId, board.getId(), sector.getNumber());

        assertTrue(unitService.getReserveUnitsDto(player.getUserId()).isEmpty());
        assertTrue(sector.getArmy().stream().anyMatch(u -> unitId == u.getId()));

        PlayerActionDto placement = playerActionService.getCurrentTurnActions(player.getUserId()).stream()
                .filter(a -> a.getType() == PlayerActionType.PLACE_UNIT)
                .findFirst().orElseThrow();
        playerActionService.undoFrom(player.getUserId(), placement.getId());

        assertFalse(unitService.getReserveUnitsDto(player.getUserId()).isEmpty(),
                "L'unité retourne en réserve après annulation du placement");

        PlayerActionDto purchase = playerActionService.getCurrentTurnActions(player.getUserId()).stream()
                .filter(a -> a.getType() == PlayerActionType.BUY_UNIT)
                .findFirst().orElseThrow();
        playerActionService.undoFrom(player.getUserId(), purchase.getId());

        assertTrue(unitService.getReserveUnitsDto(player.getUserId()).isEmpty(),
                "L'unité achetée est supprimée par l'annulation");
        assertEquals(10000.0, reload(player).getStats().getMoney(), 0.001,
                "Le coût est remboursé");
    }

    @Test
    @DisplayName("Auto-placement : une unité en réserve rejoint le secteur du QG")
    void autoPlaceAtHeadquarters() {
        Player player = fundedPlayer(10000.0);
        Board board = board();
        Sector hqSector = ownedSector(player, board);
        Headquarters hq = buildingService.getHeadquarters(player.getId()).orElseThrow();
        hq.setSector(hqSector);
        entityManager.flush();
        setTurn(board, 2);

        UnitDto bought = unitService.buyUnitsDto(player.getUserId(),
                List.of(item("LARBIN", "LEGER", 1))).getFirst();

        assertEquals(1, reserveUnitPlacer.placeAllAtHeadquarters());
        assertTrue(unitService.getReserveUnitsDto(player.getUserId()).isEmpty());
        Unit placed = entityManager.find(Unit.class, bought.getId().longValue());
        assertEquals(hqSector.getNumber(), placed.getSector().getNumber());
    }

    @Test
    @DisplayName("QG capturé : les recrues restent en réserve")
    void capturedHeadquartersKeepsReserve() {
        Player player = fundedPlayer(10000.0);
        Board board = board();
        Sector hqSector = ownedSector(player, board);
        Headquarters hq = buildingService.getHeadquarters(player.getId()).orElseThrow();
        hq.setSector(hqSector);
        hq.onCapture(99L, 2);
        entityManager.flush();
        setTurn(board, 2);

        unitService.buyUnitsDto(player.getUserId(), List.of(item("LARBIN", "LEGER", 1)));

        assertEquals(0, reserveUnitPlacer.placeAllAtHeadquarters());
        assertFalse(unitService.getReserveUnitsDto(player.getUserId()).isEmpty());
    }

    @Test
    @DisplayName("resolveMovements admin place les recrues au QG avant la résolution")
    void adminResolvePlacesReserveUnits() {
        Player player = fundedPlayer(10000.0);
        Board board = board();
        Sector hqSector = ownedSector(player, board);
        Headquarters hq = buildingService.getHeadquarters(player.getId()).orElseThrow();
        hq.setSector(hqSector);
        entityManager.flush();
        setTurn(board, 2);

        UnitDto bought = unitService.buyUnitsDto(player.getUserId(),
                List.of(item("LARBIN", "LEGER", 1))).getFirst();

        movementAdminService.resolveMovements(2);

        Unit placed = entityManager.find(Unit.class, bought.getId().longValue());
        assertEquals(hqSector.getNumber(), placed.getSector().getNumber());
        assertTrue(unitService.getReserveUnitsDto(player.getUserId()).isEmpty());
    }

    @Test
    @DisplayName("Résolution en cours : achat et placement refusés")
    void recruitmentClosedDuringResolution() {
        Player player = fundedPlayer(10000.0);
        Board board = board();
        Sector sector = ownedSector(player, board);
        setTurn(board, 2);
        UnitDto bought = unitService.buyUnitsDto(player.getUserId(),
                List.of(item("LARBIN", "LEGER", 1))).getFirst();

        turnLock.tryAcquire();
        try {
            assertThrows(IllegalStateException.class, () -> unitService.buyUnits(player.getUserId(),
                    List.of(item("LARBIN", "LEGER", 1))));
            assertThrows(IllegalStateException.class, () -> unitService.placeUnit(player.getUserId(),
                    bought.getId().longValue(), board.getId(), sector.getNumber()));
        } finally {
            turnLock.release();
        }

        assertFalse(unitService.getReserveUnitsDto(player.getUserId()).isEmpty());
    }

    @Test
    @DisplayName("Annuler un achat libère le quota du tour (catalogue inclus)")
    void undoBuyFreesQuota() {
        Player player = fundedPlayer(100000.0);
        Board board = board();
        setTurn(board, 2);

        unitService.buyUnitsDto(player.getUserId(), List.of(item("LARBIN", "LEGER", 20)));
        assertThrows(IllegalStateException.class, () -> unitService.buyUnits(player.getUserId(),
                List.of(item("LARBIN", "LEGER", 1))));
        assertEquals(20, catalogEntry(player, "LARBIN").getPurchasedThisTurn());

        PlayerActionDto lastPurchase = playerActionService.getCurrentTurnActions(player.getUserId()).stream()
                .filter(a -> a.getType() == PlayerActionType.BUY_UNIT)
                .toList().getLast();
        playerActionService.undoFrom(player.getUserId(), lastPurchase.getId());

        assertEquals(19, catalogEntry(player, "LARBIN").getPurchasedThisTurn());
        assertEquals(1, unitService.buyUnitsDto(player.getUserId(),
                List.of(item("LARBIN", "LEGER", 1))).size());
        assertEquals(20, catalogEntry(player, "LARBIN").getPurchasedThisTurn());
    }

    @Test
    @DisplayName("Disponibilités : unités et véhicules suivent la table de tours")
    void availabilityTables() {
        assertEquals(2, UnitType.LARBIN.getAvailableFromTurn());
        assertEquals(20, UnitType.LARBIN.maxPerTurnAt(2));
        assertEquals(30, UnitType.LARBIN.maxPerTurnAt(5));
        assertEquals(40, UnitType.LARBIN.maxPerTurnAt(10));
        assertEquals(5, UnitType.VOYOU.maxPerTurnAt(5));
        assertEquals(10, UnitType.VOYOU.maxPerTurnAt(10));
        assertEquals(5, UnitType.MALFRAT.maxPerTurnAt(10));
        assertFalse(UnitType.BRUTE.isPurchasable());

        assertFalse(VehicleType.TANK.isAvailableAt(7));
        assertTrue(VehicleType.TANK.isAvailableAt(8));
        assertFalse(VehicleType.VTT_LEGER.isAvailableAt(4));
        assertTrue(VehicleType.VTT_LEGER.isAvailableAt(5));
        assertFalse(VehicleType.AVION_TRANSPORT.isAvailableAt(11));
        assertTrue(VehicleType.AVION_TRANSPORT.isAvailableAt(12));
    }

    private BuyUnitRequestDto item(String type, String unitClass, int quantity) {
        BuyUnitRequestDto dto = new BuyUnitRequestDto();
        dto.setUnitType(type);
        dto.setUnitClass(unitClass);
        dto.setQuantity(quantity);
        return dto;
    }

    private UnitCatalogEntryDto catalogEntry(Player player, String typeName) {
        return unitService.getCatalog(player.getUserId()).getEntries().stream()
                .filter(entry -> entry.getName().equals(typeName))
                .findFirst().orElseThrow();
    }

    private Player fundedPlayer(double money) {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        player.getStats().setMoney(money);
        playerRepository.save(player);
        return player;
    }

    private Player reload(Player player) {
        return playerRepository.findById(player.getId()).orElseThrow();
    }

    private Board board() {
        return boardService.getAllBoards().stream().findFirst().orElseThrow();
    }

    private Sector ownedSector(Player player, Board board) {
        Sector sector = board.getAllSectors().stream()
                .filter(s -> s.getArmy() == null || s.getArmy().isEmpty())
                .findFirst().orElseThrow();
        sector.setOwnerId(player.getId());
        entityManager.flush();
        return sector;
    }

    private void setTurn(Board board, int turn) {
        board.setCurrentTurn(turn);
        entityManager.flush();
        turnService.publishTurn(turn);
    }

    private Player playerOfTestUser(String username) {
        User user = userRepository.findByUsername(username);
        assertNotNull(user, "L'utilisateur de test " + username + " doit exister");
        Player player = playerService.findByUserId(user.getId());
        assertNotNull(player, "Le joueur lié à " + username + " doit exister");
        return player;
    }
}
