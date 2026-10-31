package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.AdminCatalogDto;
import com.mg.nmlonline.api.dto.AdminCharacterRequestDto;
import com.mg.nmlonline.api.dto.AdminPlayerEditDto;
import com.mg.nmlonline.api.dto.AdminUnitUpdateRequestDto;
import com.mg.nmlonline.api.dto.BuildingDto;
import com.mg.nmlonline.api.dto.SetVehicleCrewRequestDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.api.dto.VehicleDto;
import com.mg.nmlonline.domain.exception.PlayerActionUndoException;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.alliance.PendingCapture;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.action.PlayerAction;
import com.mg.nmlonline.domain.model.action.PlayerActionStatus;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitEquipment;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PendingCaptureRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerActionRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@EmbeddedPostgresTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("AdminPlayerEditService — édition complète d'un joueur, invariants d'intégrité")
class AdminPlayerEditServiceTest {

    @Autowired
    private AdminPlayerEditService service;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private BoardService boardService;

    @Autowired
    private MovementService movementService;

    @Autowired
    private MovementOrderRepository movementOrderRepository;

    @Autowired
    private PlayerActionRepository playerActionRepository;

    @Autowired
    private PlayerActionService playerActionService;

    @Autowired
    private PendingCaptureRepository pendingCaptureRepository;

    @Autowired
    private TurnLock turnLock;

    @Autowired
    private EntityManager entityManager;

    private Player player;
    private Board board;

    @BeforeEach
    void setUp() {
        player = playerRepository.findByName("lurio")
                .orElseThrow(() -> new IllegalStateException("Fixture lurio absente"));
        board = boardService.getAllBoards().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Board de test absent"));
    }

    @Test
    @DisplayName("Vue d'édition : inventaire + toutes les unités (secteur renseigné) + véhicules")
    void getEditReturnsPlayerInventoryAndUnits() {
        AdminPlayerEditDto dto = service.getEdit(player.getId());

        assertNotNull(dto.getPlayer());
        assertEquals("lurio", dto.getPlayer().getName());
        assertFalse(dto.getPlayer().getSectors().isEmpty());
        assertFalse(dto.getUnits().isEmpty());
        assertTrue(dto.getUnits().stream().allMatch(unit -> unit.getSectorNumber() != null));
    }

    @Test
    @DisplayName("Catalogue admin : équipements, ressources, types et classes")
    void getCatalogExposesEveryCatalog() {
        AdminCatalogDto catalog = service.getCatalog();

        assertFalse(catalog.getEquipments().isEmpty());
        assertFalse(catalog.getResources().isEmpty());
        assertFalse(catalog.getUnitTypes().isEmpty());
        assertTrue(catalog.getUnitTypes().stream().noneMatch(type -> "PERSONNAGE".equals(type.getName())));
        assertEquals(UnitClass.values().length, catalog.getUnitClasses().size());
        assertEquals(VehicleType.values().length, catalog.getVehicleTypes().size());
    }

    @Test
    @DisplayName("Argent, ressources et équipements : ajout puis retrait, valeur d'économie recalculée")
    void inventoryMutationsAreApplied() {
        service.setMoney(player.getId(), 1234.5);
        service.addResource(player.getId(), "Caisse d'armes", 3);
        service.removeResource(player.getId(), "Caisse d'armes", 2);
        service.addEquipment(player.getId(), "Tesla Carbine", 2);

        assertEquals(1234.5, player.getStats().getMoney());
        assertEquals(11, player.getResourceQuantity("Caisse d'armes"));
        assertTrue(player.getEquipments().stream()
                .anyMatch(stack -> "Tesla Carbine".equals(stack.getEquipment().getName())));
    }

    @Test
    @DisplayName("Équipement équipé : refus sans includeEquipped, retrait des unités avec")
    void removeEquippedEquipmentNeedsConfirmation() {
        EquipmentStack tesla = stack("Tesla Carbine");
        int quantity = tesla.getQuantity();
        assertEquals(0, tesla.getAvailable());

        assertThrows(IllegalArgumentException.class,
                () -> service.removeEquipment(player.getId(), "Tesla Carbine", quantity, false));

        service.removeEquipment(player.getId(), "Tesla Carbine", quantity, true);
        entityManager.flush();

        assertTrue(player.getEquipments().stream()
                .noneMatch(s -> "Tesla Carbine".equals(s.getEquipment().getName())));
        assertTrue(unitRepository.findByPlayerId(player.getId()).stream()
                .flatMap(unit -> unit.getEquipments().stream())
                .noneMatch(equipment -> "Tesla Carbine".equals(equipment.getName())));
    }

    @Test
    @DisplayName("Équipement : le retrait mixte consomme le stock avant les unités")
    void removePartiallyEquippedEquipmentConsumesStockFirst() {
        EquipmentStack weave = stack("Sempiternal Weave");
        assertEquals(2, weave.getAvailable());
        int carriersBefore = carriersOf("Sempiternal Weave").size();

        service.removeEquipment(player.getId(), "Sempiternal Weave", 3, true);
        entityManager.flush();

        EquipmentStack updated = stack("Sempiternal Weave");
        assertEquals(3, updated.getQuantity());
        assertEquals(0, updated.getAvailable());
        assertEquals(carriersBefore - 1, carriersOf("Sempiternal Weave").size());
    }

    @Test
    @DisplayName("Équipement : un retrait couvert par le stock ne touche aucune unité")
    void removeStockOnlyLeavesUnitsUntouched() {
        int carriersBefore = carriersOf("Sempiternal Weave").size();

        service.removeEquipment(player.getId(), "Sempiternal Weave", 1, false);
        entityManager.flush();

        EquipmentStack updated = stack("Sempiternal Weave");
        assertEquals(5, updated.getQuantity());
        assertEquals(1, updated.getAvailable());
        assertEquals(carriersBefore, carriersOf("Sempiternal Weave").size());
    }

    @Test
    @DisplayName("Équipement : les copies perdues avec une unité détruite sont écrites en perte")
    void removeEquipmentWritesOffPhantomCopies() {
        List<Unit> carriers = carriersOf("Sempiternal Weave");
        assertTrue(carriers.size() >= 2);
        for (int i = 0; i < 2; i++) {
            UnitEquipment row = carriers.get(i).removeOneEquipment("Sempiternal Weave");
            assertNotNull(row);
            entityManager.remove(row);
        }
        entityManager.flush();

        service.removeEquipment(player.getId(), "Sempiternal Weave", 6, true);
        entityManager.flush();

        assertTrue(player.getEquipments().stream()
                .noneMatch(s -> "Sempiternal Weave".equals(s.getEquipment().getName())));
    }

    @Test
    @DisplayName("Vue d'édition : une unité en réserve est renvoyée avec sectorNumber null")
    void getEditIncludesReserveUnits() {
        Unit reserve = new Unit(0, UnitClass.LEGER);
        reserve.setPlayerId(player.getId());
        unitRepository.save(reserve);
        entityManager.flush();

        AdminPlayerEditDto dto = service.getEdit(player.getId());
        UnitDto reserveDto = dto.getUnits().stream()
                .filter(unit -> unit.getId() != null && reserve.getId().equals(unit.getId().longValue()))
                .findFirst().orElseThrow();

        assertNull(reserveDto.getSectorNumber());
    }

    @Test
    @DisplayName("Unité : ajout puis suppression ; le pilote est détaché du véhicule et l'équipement revient au stock")
    void addAndDeleteUnitDetachesPilot() {
        List<Long> beforeIds = unitRepository.findByPlayerId(player.getId()).stream()
                .map(Unit::getId).toList();
        service.addUnits(player.getId(), "LARBIN", List.of("LEGER"), 2, 41);
        entityManager.flush();
        List<Unit> created = unitRepository.findByPlayerId(player.getId()).stream()
                .filter(unit -> !beforeIds.contains(unit.getId()))
                .toList();
        assertEquals(2, created.size());
        assertTrue(created.stream().allMatch(unit -> unit.getType() == UnitType.LARBIN));
        assertTrue(created.stream().allMatch(unit -> unit.getClasses().contains(UnitClass.LEGER)));
        assertTrue(created.stream().allMatch(unit -> unit.getSector() != null
                && unit.getSector().getNumber() == 41));

        Unit pilot = unitRepository.findByPlayerId(player.getId()).stream()
                .filter(unit -> unit.getClasses().contains(UnitClass.PILOTE_DESTRUCTEUR))
                .findFirst().orElseThrow();
        int availableBefore = stack("Gauss Destructor").getAvailable();

        Sector sector = board.getSector(41);
        Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, player.getId());
        vehicle.setSector(sector);
        sector.getVehicles().add(vehicle);
        assertTrue(vehicle.assignPilot(pilot));
        vehicleRepository.save(vehicle);
        entityManager.flush();

        service.deleteUnit(player.getId(), pilot.getId());
        entityManager.flush();

        assertTrue(unitRepository.findById(pilot.getId()).isEmpty());
        assertNull(vehicleRepository.findById(vehicle.getId()).orElseThrow().getPilot());
        assertEquals(availableBefore + 1, stack("Gauss Destructor").getAvailable());
    }

    @Test
    @DisplayName("Véhicule : ajout (valeur d'économie, secteur) puis suppression (valeur retirée)")
    void addAndDeleteVehicleAdjustsEconomyValue() {
        double before = player.getStats().getTotalVehiclesValue();
        service.addVehicles(player.getId(), "VTT_LEGER", 2, 41);
        entityManager.flush();
        assertEquals(before + 2.0 * VehicleType.VTT_LEGER.getCost(),
                player.getStats().getTotalVehiclesValue(), 0.001);
        List<Vehicle> added = vehicleRepository.findByPlayerId(player.getId());
        assertEquals(2, added.size());
        assertTrue(added.stream().allMatch(vehicle -> vehicle.getSector() != null
                && vehicle.getSector().getNumber() == 41));

        List<VehicleDto> created = service.getEdit(player.getId()).getVehicles().stream()
                .filter(vehicle -> "VTT_LEGER".equals(vehicle.getVehicleType()))
                .toList();
        assertEquals(2, created.size());
        for (VehicleDto vehicle : created) {
            service.deleteVehicle(player.getId(), vehicle.getId());
        }
        entityManager.flush();

        assertTrue(service.getEdit(player.getId()).getVehicles().isEmpty());
        assertEquals(before, player.getStats().getTotalVehiclesValue(), 0.001);
    }

    @Test
    @DisplayName("Suppression d'une unité avec ordre PENDING : ordre annulé, pas de capture du secteur intermédiaire")
    void deleteUnitCancelsPendingFootOrder() {
        Unit unit = unitRepository.findByPlayerId(player.getId()).stream()
                .filter(candidate -> candidate.getSector() != null
                        && candidate.getSector().getNumber() == 13)
                .filter(candidate -> candidate.getClasses().contains(UnitClass.LEGER))
                .findFirst().orElseThrow();
        Sector start = board.getSector(13);
        Sector middle = start.getNeighbors().stream()
                .map(board::getSector)
                .filter(candidate -> candidate != null && candidate.isNeutral()
                        && candidate.getCombatEntities().isEmpty())
                .findFirst().orElseThrow();
        Sector end = middle.getNeighbors().stream()
                .map(board::getSector)
                .filter(candidate -> candidate != null && candidate.getNumber() != start.getNumber())
                .findFirst().orElseThrow();

        int turn = board.getCurrentTurn();
        MovementOrder order = movementService.placeFootOrder(player.getId(), turn,
                List.of(unit.getId()),
                List.of(start.getNumber(), middle.getNumber(), end.getNumber()), board);

        service.deleteUnit(player.getId(), unit.getId());
        entityManager.flush();
        assertEquals(MovementStatus.CANCELLED, order.getStatus());

        movementService.resolveAllMovements(turn, board);
        entityManager.flush();
        assertNotEquals(player.getId(), board.getSector(middle.getNumber()).getOwnerId());
    }

    @Test
    @DisplayName("Suppression d'un véhicule avec ordre PENDING : l'ordre est annulé")
    void deleteVehicleCancelsPendingVehicleOrder() {
        service.addVehicles(player.getId(), "VTT_LEGER", 1, 41);
        entityManager.flush();
        Vehicle vehicle = vehicleRepository.findByPlayerId(player.getId()).getFirst();
        MovementOrder order = MovementOrder.createVehicleOrder(player.getId(),
                board.getCurrentTurn(), vehicle.getId(), List.of(41, 13));
        movementOrderRepository.save(order);
        entityManager.flush();

        service.deleteVehicle(player.getId(), vehicle.getId());
        entityManager.flush();

        assertEquals(MovementStatus.CANCELLED, order.getStatus());
        assertTrue(vehicleRepository.findById(vehicle.getId()).isEmpty());
    }

    @Test
    @DisplayName("Ajout d'unités en réserve : aucune unité n'est rattachée à un secteur")
    void addUnitsToReserveKeepsUnitWithoutSector() {
        service.addUnits(player.getId(), "LARBIN", List.of("LEGER"), 2, null);
        entityManager.flush();

        List<Unit> reserve = unitRepository.findByPlayerId(player.getId()).stream()
                .filter(unit -> unit.getSector() == null)
                .toList();
        assertEquals(2, reserve.size());
        assertTrue(reserve.stream().allMatch(unit -> unit.getType() == UnitType.LARBIN));
    }

    @Test
    @DisplayName("Ajout de véhicules en réserve : valeur d'économie mise à jour, aucun secteur")
    void addVehiclesToReserveKeepsVehicleWithoutSector() {
        double before = player.getStats().getTotalVehiclesValue();
        service.addVehicles(player.getId(), "VTT_LEGER", 1, null);
        entityManager.flush();

        Vehicle vehicle = vehicleRepository.findByPlayerId(player.getId()).getFirst();
        assertNull(vehicle.getSector());
        assertEquals(before + VehicleType.VTT_LEGER.getCost(),
                player.getStats().getTotalVehiclesValue(), 0.001);
    }

    @Test
    @DisplayName("Unités : classes vides ou plus de deux refusées, deux classes acceptées")
    void addUnitsValidatesClasses() {
        int before = unitRepository.findByPlayerId(player.getId()).size();
        assertThrows(IllegalArgumentException.class,
                () -> service.addUnits(player.getId(), "LARBIN", List.of(), 1, 41));
        assertThrows(IllegalArgumentException.class,
                () -> service.addUnits(player.getId(), "LARBIN",
                        List.of("LEGER", "TIREUR", "SNIPER"), 1, 41));
        assertEquals(before, unitRepository.findByPlayerId(player.getId()).size());

        List<Long> beforeIds = unitRepository.findByPlayerId(player.getId()).stream()
                .map(Unit::getId).toList();
        service.addUnits(player.getId(), "MALFRAT", List.of("TIREUR", "MASTODONTE"), 1, 41);
        entityManager.flush();

        Unit created = unitRepository.findByPlayerId(player.getId()).stream()
                .filter(unit -> !beforeIds.contains(unit.getId()))
                .findFirst().orElseThrow();
        assertEquals(UnitType.MALFRAT, created.getType());
        assertEquals(2, created.getClasses().size());
    }

    @Test
    @DisplayName("Ajout d'unité dans un secteur non possédé : refusé")
    void addUnitsRejectsSectorNotOwned() {
        int before = unitRepository.findByPlayerId(player.getId()).size();
        assertThrows(IllegalArgumentException.class,
                () -> service.addUnits(player.getId(), "LARBIN", List.of("LEGER"), 1, 1));
        assertEquals(before, unitRepository.findByPlayerId(player.getId()).size());
    }

    @Test
    @DisplayName("Véhicule : suppression avec équipage, les unités restent dans le secteur")
    void deleteVehicleDisembarksCrew() {
        List<Long> beforeIds = unitRepository.findByPlayerId(player.getId()).stream()
                .map(Unit::getId).toList();
        service.addUnits(player.getId(), "LARBIN", List.of("PILOTE_DESTRUCTEUR"), 1, 41);
        service.addUnits(player.getId(), "LARBIN", List.of("LEGER"), 1, 41);
        entityManager.flush();
        List<Unit> created = unitRepository.findByPlayerId(player.getId()).stream()
                .filter(unit -> !beforeIds.contains(unit.getId()))
                .toList();
        assertEquals(2, created.size());
        Unit pilot = created.stream()
                .filter(unit -> unit.getClasses().contains(UnitClass.PILOTE_DESTRUCTEUR))
                .findFirst().orElseThrow();
        Unit passenger = created.stream()
                .filter(unit -> !unit.getClasses().contains(UnitClass.PILOTE_DESTRUCTEUR))
                .findFirst().orElseThrow();

        service.addVehicles(player.getId(), "VTT_LEGER", 1, 41);
        entityManager.flush();
        Vehicle vehicle = vehicleRepository.findByPlayerId(player.getId()).getFirst();
        assertTrue(vehicle.assignPilot(pilot));
        assertTrue(vehicle.embark(passenger));
        vehicleRepository.save(vehicle);
        entityManager.flush();

        service.deleteVehicle(player.getId(), vehicle.getId());
        entityManager.flush();

        Sector sector = board.getSector(41);
        assertTrue(vehicleRepository.findById(vehicle.getId()).isEmpty());
        assertTrue(sector.getVehicles().isEmpty());
        assertTrue(sector.getArmy().stream().anyMatch(unit -> unit.getId().equals(pilot.getId())));
        assertTrue(sector.getArmy().stream().anyMatch(unit -> unit.getId().equals(passenger.getId())));
    }

    @Test
    @DisplayName("Véhicule détruit : la suppression ne décote pas une seconde fois la valeur")
    void deleteDestroyedVehicleDoesNotDecrementValue() {
        service.addVehicles(player.getId(), "VTT_LEGER", 1, 41);
        entityManager.flush();
        Vehicle vehicle = vehicleRepository.findByPlayerId(player.getId()).getFirst();
        vehicle.setDestroyed(true);
        vehicleRepository.save(vehicle);
        entityManager.flush();
        double before = player.getStats().getTotalVehiclesValue();

        service.deleteVehicle(player.getId(), vehicle.getId());
        entityManager.flush();

        assertTrue(vehicleRepository.findById(vehicle.getId()).isEmpty());
        assertEquals(before, player.getStats().getTotalVehiclesValue(), 0.001);
    }

    @Test
    @DisplayName("Cache d'armes : la suppression efface les équipements stockés")
    void deleteWeaponCacheDropsStoredEquipments() {
        service.addBuilding(player.getId(), "WEAPON_CACHE", 41);
        entityManager.flush();
        WeaponCache cache = player.getBuildings().stream()
                .filter(WeaponCache.class::isInstance)
                .map(WeaponCache.class::cast)
                .filter(candidate -> candidate.getSector() != null
                        && candidate.getSector().getNumber() == 41)
                .findFirst().orElseThrow();
        EquipmentStack stored = new EquipmentStack(stack("Gauss Destructor").getEquipment());
        stored.setPlayer(player);
        cache.getStoredEquipments().add(stored);
        entityManager.flush();
        Long storedId = stored.getId();

        service.deleteBuilding(player.getId(), cache.getId());
        entityManager.flush();

        assertNull(entityManager.find(EquipmentStack.class, storedId));
    }

    @Test
    @DisplayName("Bâtiments : QG en double refusé, cache ajoutée puis supprimée, suppression du QG autorisée")
    void buildingsRespectUniquenessAndCanBeDeleted() {
        assertThrows(IllegalStateException.class,
                () -> service.addBuilding(player.getId(), "HEADQUARTERS", 41));
        assertThrows(IllegalStateException.class,
                () -> service.addBuilding(player.getId(), "BANK", 41));

        service.addBuilding(player.getId(), "WEAPON_CACHE", 41);
        BuildingDto cache = service.getEdit(player.getId()).getPlayer().getBuildings().stream()
                .filter(building -> "WEAPON_CACHE".equals(building.getBuildingType()))
                .filter(building -> building.getSectorNumber() != null && building.getSectorNumber() == 41)
                .findFirst().orElseThrow();
        service.deleteBuilding(player.getId(), cache.getId());
        entityManager.flush();
        assertTrue(buildingRepository.findById(cache.getId()).isEmpty());

        BuildingDto headquarters = service.getEdit(player.getId()).getPlayer().getBuildings().stream()
                .filter(building -> "HEADQUARTERS".equals(building.getBuildingType()))
                .findFirst().orElseThrow();
        service.deleteBuilding(player.getId(), headquarters.getId());
        entityManager.flush();
        assertTrue(buildingRepository.findById(headquarters.getId()).isEmpty());
    }

    @Test
    @DisplayName("Argent : la dotation restante est plafonnée au nouveau solde")
    void setMoneyCapsStartingMoneyRemaining() {
        player.getStats().setStartingMoneyTotal(2000);
        player.getStats().setStartingMoneyRemaining(2000);
        playerRepository.save(player);
        entityManager.flush();

        service.setMoney(player.getId(), 1500);
        entityManager.flush();
        assertEquals(1500, player.getStats().getStartingMoneyRemaining(), 0.001);
        assertEquals(0, player.getTransferableMoney(), 0.001);

        service.setMoney(player.getId(), 2500);
        entityManager.flush();
        assertEquals(1500, player.getStats().getStartingMoneyRemaining(), 0.001);
        assertEquals(1000, player.getTransferableMoney(), 0.001);
    }

    @Test
    @DisplayName("Suppression d'une unité : les actions ACTIVE liées sont clôturées, les autres restent")
    void deleteUnitDiscardsActiveActions() {
        Unit unit = unitRepository.findByPlayerId(player.getId()).stream()
                .filter(candidate -> candidate.getSector() != null)
                .findFirst().orElseThrow();
        int turn = board.getCurrentTurn();
        PlayerAction unitAction = PlayerAction.buyUnit(
                player.getId(), turn, unit.getId(), "LARBIN", 100, 0);
        PlayerAction otherAction = PlayerAction.sellResource(
                player.getId(), turn, "Caisse d'armes", 1, 10);
        playerActionRepository.saveAll(List.of(unitAction, otherAction));
        entityManager.flush();

        service.deleteUnit(player.getId(), unit.getId());
        entityManager.flush();

        assertEquals(PlayerActionStatus.UNDONE,
                playerActionRepository.findById(unitAction.getId()).orElseThrow().getStatus());
        assertEquals(PlayerActionStatus.ACTIVE,
                playerActionRepository.findById(otherAction.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Retrait d'équipement : les actions ACTIVE liées à l'équipement sont clôturées")
    void removeEquipmentDiscardsEquipmentActions() {
        Unit unit = unitRepository.findByPlayerId(player.getId()).stream()
                .filter(candidate -> candidate.getSector() != null)
                .findFirst().orElseThrow();
        PlayerAction action = PlayerAction.equipUnit(
                player.getId(), board.getCurrentTurn(), unit.getId(), "Tesla Carbine");
        playerActionRepository.save(action);
        entityManager.flush();

        service.removeEquipment(player.getId(), "Tesla Carbine", 1, true);
        entityManager.flush();

        assertEquals(PlayerActionStatus.UNDONE,
                playerActionRepository.findById(action.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Suppression d'une entité d'un autre joueur : refusée et entité intacte")
    void deleteRejectsEntitiesOfAnotherPlayer() {
        Unit foreignUnit = unitRepository.findAll().stream()
                .filter(candidate -> candidate.getPlayerId() != null
                        && !player.getId().equals(candidate.getPlayerId()))
                .findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteUnit(player.getId(), foreignUnit.getId()));
        assertTrue(unitRepository.findById(foreignUnit.getId()).isPresent());

        Vehicle foreignVehicle = vehicleRepository.findAll().stream()
                .filter(candidate -> candidate.getPlayerId() != null
                        && !player.getId().equals(candidate.getPlayerId()))
                .findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteVehicle(player.getId(), foreignVehicle.getId()));
        assertTrue(vehicleRepository.findById(foreignVehicle.getId()).isPresent());

        Building foreignBuilding = buildingRepository.findAll().stream()
                .filter(candidate -> candidate.getPlayerId() != null
                        && !player.getId().equals(candidate.getPlayerId()))
                .findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> service.deleteBuilding(player.getId(), foreignBuilding.getId()));
        assertTrue(buildingRepository.findById(foreignBuilding.getId()).isPresent());
    }

    @Test
    @DisplayName("Suppression d'une unité sur deux : l'ordre reste PENDING pour la survivante")
    void deleteOneUnitKeepsOrderForSurvivor() {
        List<Long> beforeIds = unitRepository.findByPlayerId(player.getId()).stream()
                .map(Unit::getId).toList();
        service.addUnits(player.getId(), "LARBIN", List.of("LEGER"), 2, 41);
        entityManager.flush();
        List<Unit> created = unitRepository.findByPlayerId(player.getId()).stream()
                .filter(unit -> !beforeIds.contains(unit.getId()))
                .sorted(Comparator.comparing(Unit::getId))
                .toList();
        assertEquals(2, created.size());
        MovementOrder order = movementService.placeFootOrder(player.getId(),
                board.getCurrentTurn(),
                created.stream().map(Unit::getId).toList(),
                List.of(41, 13), board);

        service.deleteUnit(player.getId(), created.getFirst().getId());
        entityManager.flush();

        MovementOrder reloaded = movementOrderRepository.findById(order.getId()).orElseThrow();
        assertEquals(MovementStatus.PENDING, reloaded.getStatus());
        assertEquals(List.of(created.get(1).getId()), reloaded.getEntityIds());
    }

    @Test
    @DisplayName("Édition admin refusée pendant la résolution du tour")
    void mutationsRejectedWhileTurnLocked() {
        assertTrue(turnLock.tryAcquire());
        try {
            assertThrows(IllegalStateException.class, () -> service.addUnits(
                    player.getId(), "LARBIN", List.of("LEGER"), 1, 41));
        } finally {
            turnLock.release();
        }
    }

    @Test
    @DisplayName("Argent et équipement : valeur d'inventaire et puissance économique recalculées")
    void moneyAndEquipmentUpdatesRecalculateEconomy() {
        player.getStats().setTotalIncome(10);
        player.getStats().setTotalVehiclesValue(20);
        player.setTotalEquipmentValue();
        service.setMoney(player.getId(), 100);
        entityManager.flush();
        double equipmentValue = player.getStats().getTotalEquipmentValue();
        assertEquals(130 + equipmentValue, player.getStats().getTotalEconomyPower(), 0.001);

        double cost = stack("Tesla Carbine").getEquipment().getCost();
        service.addEquipment(player.getId(), "Tesla Carbine", 2);
        entityManager.flush();
        assertEquals(equipmentValue + 2 * cost,
                player.getStats().getTotalEquipmentValue(), 0.001);
        assertEquals(130 + equipmentValue + 2 * cost,
                player.getStats().getTotalEconomyPower(), 0.001);
    }

    @Test
    @DisplayName("Unité : l'expérience admin fait évoluer le type")
    void updateUnitEvolvesTypeFromExperience() {
        Unit unit = addUnit("LARBIN", UnitClass.LEGER, 41);
        AdminUnitUpdateRequestDto request = new AdminUnitUpdateRequestDto();
        request.setExperience((double) UnitType.MALFRAT.getMinExp());

        service.updateUnit(player.getId(), unit.getId(), request);
        entityManager.flush();

        assertEquals(UnitType.MALFRAT, unit.getType());
        assertEquals(UnitType.MALFRAT.getMinExp(), unit.getExperience(), 0.001);
    }

    @Test
    @DisplayName("Unité : règles de classes appliquées à l'édition")
    void updateUnitAppliesClassRules() {
        Unit larbin = addUnit("LARBIN", UnitClass.LEGER, 41);
        AdminUnitUpdateRequestDto twoClasses = new AdminUnitUpdateRequestDto();
        twoClasses.setClasses(List.of("LEGER", "TIREUR"));
        assertThrows(IllegalArgumentException.class,
                () -> service.updateUnit(player.getId(), larbin.getId(), twoClasses));
        assertEquals(1, larbin.getClasses().size());

        Unit malfrat = addUnit("MALFRAT", UnitClass.TIREUR, 41);
        twoClasses.setClasses(List.of("TIREUR", "MASTODONTE"));
        service.updateUnit(player.getId(), malfrat.getId(), twoClasses);
        entityManager.flush();
        assertEquals(2, malfrat.getClasses().size());
    }

    @Test
    @DisplayName("Unité : un changement de classes déséquipe les pièces incompatibles")
    void updateUnitUnequipsIncompatibleEquipment() {
        Equipment equipment = player.getEquipments().stream()
                .map(EquipmentStack::getEquipment)
                .filter(candidate -> !candidate.getCompatibleClasses().isEmpty())
                .filter(candidate -> candidate.getCompatibleClasses().size()
                        < UnitClass.values().length)
                .findFirst().orElseThrow();
        UnitClass compatible = equipment.getCompatibleClasses().iterator().next();
        UnitClass incompatible = Arrays.stream(UnitClass.values())
                .filter(candidate -> !equipment.getCompatibleClasses().contains(candidate))
                .findFirst().orElseThrow();
        Unit unit = addUnit("MALFRAT", compatible, 41);
        service.addEquipment(player.getId(), equipment.getName(), 1);
        service.equipUnit(player.getId(), unit.getId(), equipment.getName());
        entityManager.flush();
        int availableBefore = stack(equipment.getName()).getAvailable();

        AdminUnitUpdateRequestDto request = new AdminUnitUpdateRequestDto();
        request.setClasses(List.of(incompatible.name()));
        service.updateUnit(player.getId(), unit.getId(), request);
        entityManager.flush();

        assertTrue(unit.getEquipments().isEmpty());
        assertEquals(availableBefore + 1, stack(equipment.getName()).getAvailable());
    }

    @Test
    @DisplayName("Unité : déplacement secteur ↔ réserve, secteur étranger refusé")
    void moveUnitBetweenSectorsAndReserve() {
        Unit unit = addUnit("LARBIN", UnitClass.LEGER, 41);

        service.moveUnit(player.getId(), unit.getId(), 13);
        entityManager.flush();
        assertEquals(13, unit.getSector().getNumber());

        service.moveUnit(player.getId(), unit.getId(), null);
        entityManager.flush();
        assertNull(unit.getSector());
        assertEquals(0, unit.getNumber());

        service.moveUnit(player.getId(), unit.getId(), 41);
        entityManager.flush();
        assertEquals(41, unit.getSector().getNumber());

        assertThrows(IllegalArgumentException.class,
                () -> service.moveUnit(player.getId(), unit.getId(), 1));
    }

    @Test
    @DisplayName("Unité : blessure et soin admin recalculent les stats")
    void updateUnitTogglesInjury() {
        Unit unit = addUnit("LARBIN", UnitClass.LEGER, 41);
        double attack = unit.getAttack();
        AdminUnitUpdateRequestDto request = new AdminUnitUpdateRequestDto();

        request.setInjured(true);
        service.updateUnit(player.getId(), unit.getId(), request);
        entityManager.flush();
        assertTrue(unit.isInjured());
        assertEquals(attack / 2, unit.getAttack(), 0.001);

        request.setInjured(false);
        service.updateUnit(player.getId(), unit.getId(), request);
        entityManager.flush();
        assertFalse(unit.isInjured());
        assertEquals(attack, unit.getAttack(), 0.001);
    }

    @Test
    @DisplayName("Joueur : renommage autorisé, nom en doublon refusé")
    void updatePlayerNameRejectsDuplicate() {
        service.updatePlayerName(player.getId(), "lurio-admin");
        entityManager.flush();
        assertEquals("lurio-admin", player.getName());

        String otherName = playerRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(player.getId()))
                .map(Player::getName)
                .findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> service.updatePlayerName(player.getId(), otherName));
        assertEquals("lurio-admin", player.getName());
    }

    @Test
    @DisplayName("Personnage : mise à jour des stats de base et soin complet")
    void updateCharacterRecalculatesStats() {
        GameCharacter character = player.getCharacter();
        assertNotNull(character);
        character.setDefense(Math.max(0, character.getBaseDefense() - 10));
        entityManager.flush();

        AdminCharacterRequestDto request = characterRequest(
                character.getName(), 120, 80, 40, 30, 20, 10);
        service.updateCharacter(player.getId(), request);
        entityManager.flush();

        assertEquals(120, character.getAttack(), 0.001);
        assertEquals(80, character.getDefense(), 0.001);
        assertEquals(80, character.getBaseDefense(), 0.001);
        assertEquals(10, character.getEvasion(), 0.001);
    }

    @Test
    @DisplayName("Personnage : nom en doublon refusé")
    void updateCharacterRejectsDuplicateName() {
        GameCharacter character = player.getCharacter();
        assertNotNull(character);
        String otherName = playerRepository.findAll().stream()
                .filter(candidate -> !candidate.getId().equals(player.getId()))
                .map(Player::getCharacter)
                .filter(Objects::nonNull)
                .map(GameCharacter::getName)
                .findFirst().orElse(null);
        assumeTrue(otherName != null);
        AdminCharacterRequestDto request = characterRequest(otherName, 1, 1, 1, 1, 1, 1);
        assertThrows(IllegalArgumentException.class,
                () -> service.updateCharacter(player.getId(), request));
    }

    @Test
    @DisplayName("Personnage : suppression puis recréation, détachement du secteur")
    void deleteAndCreateCharacterDetachesFromSector() {
        GameCharacter character = player.getCharacter();
        assertNotNull(character);
        Sector sector = character.getSector();
        service.deleteCharacter(player.getId());
        entityManager.flush();

        assertNull(player.getCharacter());
        assertNull(entityManager.find(GameCharacter.class, character.getId()));
        if (sector != null) {
            assertFalse(sector.getCharacters().contains(character));
        }

        AdminCharacterRequestDto request = characterRequest(
                "Lurio-Recreated", 10, 20, 30, 40, 50, 60);
        service.createCharacter(player.getId(), request);
        entityManager.flush();

        assertNotNull(player.getCharacter());
        assertEquals("Lurio-Recreated", player.getCharacter().getName());
    }

    @Test
    @DisplayName("Secteur : réassignation à un joueur puis neutralisation")
    void reassignSectorMovesOwnerAndStats() {
        Player target = playerRepository.findByName("cegorach").orElseThrow();
        service.reassignSector(41, target.getId());
        entityManager.flush();

        Sector sector = board.getSector(41);
        assertEquals(target.getId(), sector.getOwnerId());
        assertNotNull(sector.getColor());
        assertTrue(board.getSectorsByOwner(target.getId()).contains(sector));
        assertFalse(board.getSectorsByOwner(player.getId()).contains(sector));

        service.reassignSector(41, null);
        entityManager.flush();
        assertNull(sector.getOwnerId());
        assertEquals("#ffffff", sector.getColor());
    }

    @Test
    @DisplayName("Secteur : réassignation refusée si une capture est en attente")
    void reassignSectorRejectsPendingCapture() {
        PendingCapture pending = PendingCapture.create(
                board.getId(), 41, board.getCurrentTurn(), List.of(player.getId()));
        pendingCaptureRepository.save(pending);
        entityManager.flush();
        Player target = playerRepository.findByName("cegorach").orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> service.reassignSector(41, target.getId()));
    }

    @Test
    @DisplayName("Secteur : réassignation refusée vers un joueur éliminé")
    void reassignSectorRejectsEliminatedTarget() {
        Player target = playerRepository.findByName("cegorach").orElseThrow();
        target.setEliminated(true);
        playerRepository.save(target);
        entityManager.flush();

        assertThrows(IllegalArgumentException.class,
                () -> service.reassignSector(41, target.getId()));
    }

    @Test
    @DisplayName("Unité : l'ajout refuse deux classes à un LARBIN")
    void addUnitsRejectsTwoClassesForLarbin() {
        int before = unitRepository.findByPlayerId(player.getId()).size();
        assertThrows(IllegalArgumentException.class,
                () -> service.addUnits(player.getId(), "LARBIN",
                        List.of("LEGER", "TIREUR"), 1, 41));
        assertEquals(before, unitRepository.findByPlayerId(player.getId()).size());
    }

    @Test
    @DisplayName("Personnage : la mise à jour recalcule le secteur et les stats du joueur")
    void updateCharacterRecalculatesSectorAndPlayerStats() {
        GameCharacter character = player.getCharacter();
        assertNotNull(character);
        Sector sector = character.getSector();
        if (sector == null) {
            service.updateCharacter(player.getId(), characterRequest(
                    character.getName(), 120, 80, 40, 30, 20, 10));
            entityManager.flush();
            character = player.getCharacter();
            sector = character.getSector();
        }
        assertNotNull(sector);
        double playerAtkBefore = player.getStats().getTotalAtk();
        double sectorAtkBefore = sector.getStats().getTotalAtk();

        service.updateCharacter(player.getId(), characterRequest(
                character.getName(), 500, 80, 40, 30, 20, 10));
        entityManager.flush();

        assertEquals(500, character.getAttack(), 0.001);
        assertTrue(player.getStats().getTotalAtk() > playerAtkBefore);
        assertTrue(sector.getStats().getTotalAtk() > sectorAtkBefore);
    }

    @Test
    @DisplayName("Personnage : suppression d'un ordre PENDING et recréation dans un secteur")
    void deleteCharacterCleansOrderAndCreatesInSector() {
        GameCharacter character = player.getCharacter();
        assertNotNull(character);
        assertNotNull(character.getSector());
        int sectorNumber = character.getSector().getNumber();
        int targetNumber = character.getSector().getNeighbors().getFirst();
        MovementOrder order = movementService.placeFootOrder(player.getId(),
                board.getCurrentTurn(), List.of(character.getId()),
                List.of(sectorNumber, targetNumber), board);

        service.deleteCharacter(player.getId());
        entityManager.flush();

        assertEquals(MovementStatus.CANCELLED, order.getStatus());
        assertNull(player.getCharacter());

        service.createCharacter(player.getId(), characterRequest(
                "Lurio-Recreated", 10, 20, 30, 40, 50, 60));
        entityManager.flush();

        GameCharacter recreated = player.getCharacter();
        assertNotNull(recreated);
        assertNotNull(recreated.getSector());
        assertEquals(10, recreated.getAttack(), 0.001);
        assertEquals(20, recreated.getDefense(), 0.001);
        assertEquals(30, recreated.getPdf(), 0.001);
        assertEquals(40, recreated.getPdc(), 0.001);
        assertEquals(50, recreated.getArmor(), 0.001);
        assertEquals(60, recreated.getEvasion(), 0.001);
    }

    @Test
    @DisplayName("Secteur : une capture en attente d'un tour antérieur bloque la réassignation")
    void reassignSectorRejectsStalePendingCapture() {
        PendingCapture pending = PendingCapture.create(
                board.getId(), 41, board.getCurrentTurn() - 1, List.of(player.getId()));
        pendingCaptureRepository.save(pending);
        entityManager.flush();
        Player target = playerRepository.findByName("cegorach").orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> service.reassignSector(41, target.getId()));
    }

    @Test
    @DisplayName("Le catalogue et la vue d'édition ne dépendent pas de la session HTTP (mapping en transaction)")
    void dtoMappingSurvivesWithoutOpenSessionInView() {
        AdminPlayerEditDto dto = service.getEdit(player.getId());
        assertNotNull(dto.getPlayer().getStats());
        assertNotNull(dto.getPlayer().getBuildings());
        assertNotNull(dto.getUnits().getFirst().getType().getName());
        assertNotNull(dto.getVehicles());
    }

    @Test
    @DisplayName("Banque : ressources ajoutées puis retirées, l'inventaire du joueur reste intact")
    void bankResourcesAreEditedWithoutTouchingInventory() {
        int inventoryBefore = player.getResourceQuantity("Caisse d'armes");
        service.addBankResource(player.getId(), "Caisse d'armes", 3);
        entityManager.flush();

        Bank bank = player.getBuildings().stream()
                .filter(Bank.class::isInstance)
                .map(Bank.class::cast)
                .findFirst().orElseThrow();
        assertEquals(3, storedBankQuantity(bank, "Caisse d'armes"));
        assertEquals(inventoryBefore, player.getResourceQuantity("Caisse d'armes"));

        service.removeBankResource(player.getId(), "Caisse d'armes", 2);
        entityManager.flush();
        assertEquals(1, storedBankQuantity(bank, "Caisse d'armes"));
        assertThrows(IllegalArgumentException.class,
                () -> service.removeBankResource(player.getId(), "Caisse d'armes", 2));

        service.removeBankResource(player.getId(), "Caisse d'armes", 1);
        entityManager.flush();
        assertTrue(bank.getStoredResources().isEmpty());

        entityManager.clear();
        Player reloaded = playerRepository.findById(player.getId()).orElseThrow();
        assertEquals(inventoryBefore, reloaded.getResourceQuantity("Caisse d'armes"));
    }

    @Test
    @DisplayName("Cache d'armes : ajout/retrait bornés par la capacité, inventaire inchangé")
    void cacheEquipmentIsEditedWithinCapacity() {
        service.addBuilding(player.getId(), "WEAPON_CACHE", 41);
        entityManager.flush();
        WeaponCache cache = player.getBuildings().stream()
                .filter(WeaponCache.class::isInstance)
                .map(WeaponCache.class::cast)
                .filter(candidate -> candidate.getSector() != null
                        && candidate.getSector().getNumber() == 41)
                .findFirst().orElseThrow();
        int inventoryBefore = equipmentQuantity(player, "Gauss Destructor");

        service.addCacheEquipment(player.getId(), cache.getId(), "Gauss Destructor", 2);
        entityManager.flush();
        assertEquals(2, cachedQuantity(cache, "Gauss Destructor"));

        assertThrows(IllegalArgumentException.class, () -> service.addCacheEquipment(
                player.getId(), cache.getId(), "Gauss Destructor", 299));

        service.removeCacheEquipment(player.getId(), cache.getId(), "Gauss Destructor", 1);
        entityManager.flush();
        assertEquals(1, cachedQuantity(cache, "Gauss Destructor"));
        service.removeCacheEquipment(player.getId(), cache.getId(), "Gauss Destructor", 1);
        entityManager.flush();
        assertTrue(cache.getStoredEquipments().isEmpty());

        entityManager.clear();
        Player reloaded = playerRepository.findById(player.getId()).orElseThrow();
        assertEquals(inventoryBefore, equipmentQuantity(reloaded, "Gauss Destructor"));
    }

    @Test
    @DisplayName("Contenu banque/cache : hors inventaire même après rechargement")
    void storedBankAndCacheContentStaysOutOfInventory() {
        int inventoryResource = player.getResourceQuantity("Caisse d'armes");
        int inventoryEquipment = equipmentQuantity(player, "Gauss Destructor");

        service.addBankResource(player.getId(), "Caisse d'armes", 3);
        service.addBuilding(player.getId(), "WEAPON_CACHE", 41);
        entityManager.flush();
        WeaponCache cache = player.getBuildings().stream()
                .filter(WeaponCache.class::isInstance)
                .map(WeaponCache.class::cast)
                .filter(candidate -> candidate.getSector() != null
                        && candidate.getSector().getNumber() == 41)
                .findFirst().orElseThrow();
        service.addCacheEquipment(player.getId(), cache.getId(), "Gauss Destructor", 2);
        entityManager.flush();

        entityManager.clear();
        Player reloaded = playerRepository.findById(player.getId()).orElseThrow();
        assertEquals(inventoryResource, reloaded.getResourceQuantity("Caisse d'armes"));
        assertEquals(inventoryEquipment, equipmentQuantity(reloaded, "Gauss Destructor"));

        Bank bank = reloaded.getBuildings().stream()
                .filter(Bank.class::isInstance)
                .map(Bank.class::cast)
                .findFirst().orElseThrow();
        assertEquals(3, storedBankQuantity(bank, "Caisse d'armes"));
        WeaponCache reloadedCache = reloaded.getBuildings().stream()
                .filter(WeaponCache.class::isInstance)
                .map(WeaponCache.class::cast)
                .filter(candidate -> candidate.getSector() != null
                        && candidate.getSector().getNumber() == 41)
                .findFirst().orElseThrow();
        assertEquals(2, cachedQuantity(reloadedCache, "Gauss Destructor"));
    }

    @Test
    @DisplayName("Cache d'armes : cible refusée si ce n'est pas une cache ou pas celle du joueur")
    void cacheEquipmentRejectsWrongBuilding() {
        Building bank = buildingRepository.findAll().stream()
                .filter(candidate -> player.getId().equals(candidate.getPlayerId()))
                .filter(Bank.class::isInstance)
                .findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> service.addCacheEquipment(
                player.getId(), bank.getId(), "Gauss Destructor", 1));

        WeaponCache foreign = buildingRepository.findAll().stream()
                .filter(WeaponCache.class::isInstance)
                .map(WeaponCache.class::cast)
                .filter(candidate -> candidate.getPlayerId() != null
                        && !player.getId().equals(candidate.getPlayerId()))
                .findFirst().orElse(null);
        assumeTrue(foreign != null);
        assertThrows(IllegalArgumentException.class, () -> service.addCacheEquipment(
                player.getId(), foreign.getId(), "Gauss Destructor", 1));
    }

    @Test
    @DisplayName("Équipage : règles du jeu appliquées (pilote P, même secteur, propriétaire, capacité)")
    void setVehicleCrewEnforcesGameRules() {
        Unit pilot = addUnit("LARBIN", UnitClass.PILOTE_DESTRUCTEUR, 41);
        Unit passenger = addUnit("LARBIN", UnitClass.LEGER, 41);
        Unit outsider = addUnit("LARBIN", UnitClass.LEGER, 13);
        service.addVehicles(player.getId(), "VTT_LEGER", 1, 41);
        entityManager.flush();
        Vehicle vehicle = vehicleRepository.findByPlayerId(player.getId()).getFirst();

        service.setVehicleCrew(player.getId(), vehicle.getId(),
                crewRequest(pilot.getId(), List.of(passenger.getId())));
        entityManager.flush();
        assertEquals(pilot.getId(), vehicle.getPilot().getId());
        assertEquals(1, vehicle.getPassengerCount());

        assertThrows(IllegalArgumentException.class, () -> service.setVehicleCrew(
                player.getId(), vehicle.getId(), crewRequest(passenger.getId(), List.of())));
        assertThrows(IllegalArgumentException.class, () -> service.setVehicleCrew(
                player.getId(), vehicle.getId(), crewRequest(null, List.of(outsider.getId()))));
        assertThrows(IllegalArgumentException.class, () -> service.setVehicleCrew(
                player.getId(), vehicle.getId(), crewRequest(null,
                        List.of(pilot.getId(), passenger.getId(), outsider.getId()))));

        Unit foreign = unitRepository.findAll().stream()
                .filter(unit -> unit.getPlayerId() != null
                        && !player.getId().equals(unit.getPlayerId()))
                .findFirst().orElse(null);
        assumeTrue(foreign != null);
        assertThrows(SecurityException.class, () -> service.setVehicleCrew(
                player.getId(), vehicle.getId(), crewRequest(null, List.of(foreign.getId()))));
    }

    @Test
    @DisplayName("Équipage : véhicule en réserve refusé, seule l'action d'équipage est clôturée")
    void setVehicleCrewRequiresDeployedVehicleAndClosesActions() {
        Unit pilot = addUnit("LARBIN", UnitClass.PILOTE_DESTRUCTEUR, 41);
        service.addVehicles(player.getId(), "VTT_LEGER", 1, 41);
        service.addVehicles(player.getId(), "VTT_LEGER", 1, null);
        entityManager.flush();
        Vehicle vehicle = vehicleRepository.findByPlayerId(player.getId()).stream()
                .filter(candidate -> candidate.getSector() != null)
                .findFirst().orElseThrow();
        Vehicle reserve = vehicleRepository.findByPlayerId(player.getId()).stream()
                .filter(candidate -> candidate.getSector() == null)
                .findFirst().orElseThrow();

        assertThrows(IllegalStateException.class, () -> service.setVehicleCrew(
                player.getId(), reserve.getId(), crewRequest(pilot.getId(), List.of())));

        PlayerAction action = PlayerAction.setVehicleCrew(
                player.getId(), board.getCurrentTurn(), vehicle.getId(), null, "");
        playerActionRepository.save(action);
        PlayerAction placement = PlayerAction.placeVehicle(player.getId(), board.getCurrentTurn(),
                vehicle.getId(), board.getId(), vehicle.getSector().getNumber());
        playerActionRepository.save(placement);
        entityManager.flush();

        service.setVehicleCrew(player.getId(), vehicle.getId(), crewRequest(pilot.getId(), List.of()));
        entityManager.flush();

        assertEquals(PlayerActionStatus.UNDONE,
                playerActionRepository.findById(action.getId()).orElseThrow().getStatus());
        assertEquals(PlayerActionStatus.ACTIVE,
                playerActionRepository.findById(placement.getId()).orElseThrow().getStatus());
        assertEquals(pilot.getId(), vehicle.getPilot().getId());
    }

    @Test
    @DisplayName("Équipage : le joueur ne peut pas annuler le placement d'une unité embarquée")
    void undoPlacementIsRefusedForEmbarkedUnit() {
        Unit pilot = addUnit("LARBIN", UnitClass.PILOTE_DESTRUCTEUR, 41);
        service.addVehicles(player.getId(), "VTT_LEGER", 1, 41);
        entityManager.flush();
        Vehicle vehicle = vehicleRepository.findByPlayerId(player.getId()).getFirst();

        PlayerAction placement = PlayerAction.placeUnit(
                player.getId(), board.getCurrentTurn(), pilot.getId(), board.getId(), 41);
        playerActionRepository.save(placement);
        entityManager.flush();

        service.setVehicleCrew(player.getId(), vehicle.getId(), crewRequest(pilot.getId(), List.of()));
        entityManager.flush();

        assertThrows(PlayerActionUndoException.class,
                () -> playerActionService.undoFrom(player.getUserId(), placement.getId()));
        assertEquals(pilot.getId(), vehicle.getPilot().getId());
        assertNotNull(pilot.getSector());
    }

    private SetVehicleCrewRequestDto crewRequest(Long pilotId, List<Long> passengerIds) {
        SetVehicleCrewRequestDto request = new SetVehicleCrewRequestDto();
        request.setPilotId(pilotId);
        request.setPassengerIds(passengerIds);
        return request;
    }

    private int storedBankQuantity(Bank bank, String name) {
        return bank.getStoredResources().stream()
                .filter(resource -> name.equals(resource.getResourceName()))
                .map(PlayerResource::getQuantity)
                .findFirst().orElse(0);
    }

    private int cachedQuantity(WeaponCache cache, String name) {
        return cache.getStoredEquipments().stream()
                .filter(stack -> name.equals(stack.getEquipment().getName()))
                .mapToInt(EquipmentStack::getQuantity)
                .findFirst().orElse(0);
    }

    private int equipmentQuantity(Player owner, String name) {
        return owner.getEquipments().stream()
                .filter(stack -> name.equals(stack.getEquipment().getName()))
                .mapToInt(EquipmentStack::getQuantity)
                .sum();
    }

    private AdminCharacterRequestDto characterRequest(String name, double attack, double defense,
                                                       double pdf, double pdc, double armor,
                                                       double evasion) {
        AdminCharacterRequestDto request = new AdminCharacterRequestDto();
        request.setName(name);
        request.setBaseAttack(attack);
        request.setBaseDefense(defense);
        request.setBasePdf(pdf);
        request.setBasePdc(pdc);
        request.setBaseArmor(armor);
        request.setBaseEvasion(evasion);
        return request;
    }

    private Unit addUnit(String type, UnitClass unitClass, Integer sectorNumber) {
        List<Long> beforeIds = unitRepository.findByPlayerId(player.getId()).stream()
                .map(Unit::getId).toList();
        service.addUnits(player.getId(), type, List.of(unitClass.name()), 1, sectorNumber);
        entityManager.flush();
        return unitRepository.findByPlayerId(player.getId()).stream()
                .filter(unit -> !beforeIds.contains(unit.getId()))
                .findFirst().orElseThrow();
    }

    private List<Unit> carriersOf(String equipmentName) {
        return unitRepository.findByPlayerId(player.getId()).stream()
                .filter(unit -> unit.getEquipments().stream()
                        .anyMatch(equipment -> equipmentName.equals(equipment.getName())))
                .toList();
    }

    private EquipmentStack stack(String equipmentName) {
        return player.getEquipments().stream()
                .filter(stack -> equipmentName.equals(stack.getEquipment().getName()))
                .findFirst().orElseThrow();
    }
}
