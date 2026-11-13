package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sniper.SniperOrder;
import com.mg.nmlonline.domain.model.sniper.SniperOrderStatus;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.api.dto.TurnJournalDto;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.GameCharacterRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.SniperOrderRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@DisplayName("Fin de tour — soins et verrous restent dans leur plateau")
class TurnBoardIsolationTest {

    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private UnitRepository unitRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private GameCharacterRepository characterRepository;
    @Autowired
    private UnitRecoveryService unitRecoveryService;
    @Autowired
    private VehicleMaintenanceService vehicleMaintenanceService;
    @Autowired
    private GameCharacterService characterService;
    @Autowired
    private TurnLock turnLock;
    @Autowired
    private SniperService sniperService;
    @Autowired
    private TurnJournalService turnJournalService;
    @Autowired
    private SniperOrderRepository sniperOrderRepository;
    @Autowired
    private PlayerRepository playerRepository;

    private Board firstBoard;
    private Board secondBoard;

    @AfterEach
    void deleteBoards() {
        for (Board board : new Board[]{firstBoard, secondBoard}) {
            if (board != null && board.getId() != null && boardRepository.existsById(board.getId())) {
                boardRepository.deleteById(board.getId());
            }
        }
    }

    @Test
    @DisplayName("un tour soigne son plateau et laisse l'autre intact")
    void repairsStayInsideTheirBoard() {
        firstBoard = createBoard("Iso Plateau A");
        secondBoard = createBoard("Iso Plateau B");
        Sector firstSector = firstBoard.getSectorsList().getFirst();
        Sector secondSector = secondBoard.getSectorsList().getFirst();
        int turnEnding = 12;

        Unit unit = new Unit();
        unit.setNumber(1);
        unit.setType(UnitType.LARBIN);
        unit.setSector(secondSector);
        unit.setInjured(true);
        unit.setInjuredAtTurn(turnEnding - 2);
        unitRepository.save(unit);

        Vehicle vehicle = new Vehicle(VehicleType.TANK, null);
        vehicle.setSector(secondSector);
        vehicle.setDefense(10);
        vehicleRepository.save(vehicle);

        GameCharacter character = new GameCharacter("IsoHero-" + UUID.randomUUID(), 1, 1, 1, 500, 1, 1);
        character.setSector(secondSector);
        character.setDefense(100);
        characterRepository.save(character);

        double vehicleDefenseBefore = vehicle.getDefense();

        unitRecoveryService.recoverAtTurnEnd(turnEnding, firstBoard.getId());
        vehicleMaintenanceService.repairAllVehicles(firstBoard.getId());
        characterService.regenerateAllCharacters(firstBoard.getId());

        assertTrue(unitRepository.findById(unit.getId()).orElseThrow().isInjured(),
                "l'unité blessée d'un autre plateau ne doit pas guérir");
        assertEquals(vehicleDefenseBefore, vehicleRepository.findById(vehicle.getId()).orElseThrow().getDefense(),
                "le véhicule endommagé d'un autre plateau ne doit pas être réparé");
        assertEquals(100, characterRepository.findById(character.getId()).orElseThrow().getDefense(),
                "le personnage entamé d'un autre plateau ne doit pas régénérer");

        unitRecoveryService.recoverAtTurnEnd(turnEnding, secondBoard.getId());
        vehicleMaintenanceService.repairAllVehicles(secondBoard.getId());
        characterService.regenerateAllCharacters(secondBoard.getId());

        assertFalse(unitRepository.findById(unit.getId()).orElseThrow().isInjured(),
                "l'unité du plateau résolu doit guérir");
        assertTrue(vehicleRepository.findById(vehicle.getId()).orElseThrow().getDefense() > vehicleDefenseBefore,
                "le véhicule du plateau résolu doit être réparé");
        assertTrue(characterRepository.findById(character.getId()).orElseThrow().getDefense() > 100,
                "le personnage du plateau résolu doit régénérer");
    }

    @Test
    @DisplayName("deux plateaux peuvent tenir leur verrou de tour en même temps")
    void locksAreScopedPerBoard() {
        assertTrue(turnLock.tryAcquire(101L));
        try {
            assertTrue(turnLock.tryAcquire(102L), "un tour en cours ne doit pas bloquer une autre partie");
            turnLock.release(102L);
            assertFalse(turnLock.tryAcquire(101L), "le même plateau reste verrouillé");
        } finally {
            turnLock.release(101L);
        }
        assertFalse(turnLock.isLocked(101L));
        assertFalse(turnLock.isLocked(102L));
    }

    @Test
    @DisplayName("les journaux de tour restent dans leur partie")
    void turnJournalsStayInsideTheirBoard() {
        firstBoard = createBoard("Iso Journal A");
        secondBoard = createBoard("Iso Journal B");
        turnJournalService.save(firstBoard.getId(), journal(firstBoard, "journal du plateau A"));
        turnJournalService.save(secondBoard.getId(), journal(secondBoard, "journal du plateau B"));

        assertEquals("journal du plateau A", turnJournalService
                .findByTurn(firstBoard.getId(), firstBoard.getCurrentTurn()).getExportedAt());
        assertEquals("journal du plateau B", turnJournalService
                .findByTurn(secondBoard.getId(), secondBoard.getCurrentTurn()).getExportedAt());
        assertThrows(EntityNotFoundException.class,
                () -> turnJournalService.findByTurn(firstBoard.getId(), firstBoard.getCurrentTurn() + 10));
    }

    private TurnJournalDto journal(Board board, String marker) {
        TurnJournalDto journal = new TurnJournalDto();
        journal.setTurn(board.getCurrentTurn());
        journal.setFinalized(true);
        journal.setExportedAt(marker);
        return journal;
    }

    @Test
    @DisplayName("les ordres sniper d'une partie ne sont pas résolus par la fin de tour d'une autre")
    void sniperOrdersStayInsideTheirBoard() {
        firstBoard = createBoard("Iso Sniper A");
        secondBoard = createBoard("Iso Sniper B");
        SniperOrder firstOrder = pendingSniperOrder(firstBoard, "SniperA");
        SniperOrder secondOrder = pendingSniperOrder(secondBoard, "SniperB");

        sniperService.resolveRemainingFire(firstBoard.getCurrentTurn(), firstBoard, Set.of());

        assertEquals(SniperOrderStatus.RESOLVED,
                sniperOrderRepository.findById(firstOrder.getId()).orElseThrow().getStatus(),
                "l'ordre du plateau résolu est consommé");
        assertEquals(SniperOrderStatus.PENDING,
                sniperOrderRepository.findById(secondOrder.getId()).orElseThrow().getStatus(),
                "l'ordre de l'autre partie reste en attente");
    }

    private SniperOrder pendingSniperOrder(Board board, String sniperName) {
        Sector home = board.getSectorsList().getFirst();
        board.addSector(new Sector(2, "Iso cible"));
        boardRepository.save(board);

        Player player = new Player(sniperName + UUID.randomUUID());
        player.setBoardId(board.getId());
        playerRepository.save(player);

        Unit unit = new Unit(0.0, UnitClass.SNIPER);
        unit.setNumber(1);
        unit.setPlayerId(player.getId());
        unit.setSector(home);
        unitRepository.save(unit);

        return sniperOrderRepository.save(SniperOrder.create(board.getId(), player.getId(),
                board.getCurrentTurn(), unit.getId(), home.getNumber(), 2));
    }

    private Board createBoard(String name) {
        Board board = new Board();
        board.setName(name);
        Sector sector = new Sector();
        sector.setBoard(board);
        sector.setNumber(1);
        sector.setName("Iso secteur 1");
        board.getSectorsList().add(sector);
        return boardRepository.save(board);
    }
}
