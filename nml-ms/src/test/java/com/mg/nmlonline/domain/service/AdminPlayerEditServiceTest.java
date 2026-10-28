package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.AdminCatalogDto;
import com.mg.nmlonline.api.dto.AdminPlayerEditDto;
import com.mg.nmlonline.api.dto.BuildingDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.api.dto.VehicleDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitEquipment;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("Le catalogue et la vue d'édition ne dépendent pas de la session HTTP (mapping en transaction)")
    void dtoMappingSurvivesWithoutOpenSessionInView() {
        AdminPlayerEditDto dto = service.getEdit(player.getId());
        assertNotNull(dto.getPlayer().getStats());
        assertNotNull(dto.getPlayer().getBuildings());
        assertNotNull(dto.getUnits().getFirst().getType().getName());
        assertNotNull(dto.getVehicles());
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
