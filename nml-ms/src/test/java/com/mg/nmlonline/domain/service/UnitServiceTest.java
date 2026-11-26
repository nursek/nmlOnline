package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.DemoFixtures;
import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.AutoEquipResultDto;
import com.mg.nmlonline.api.dto.BuyUnitRequestDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@Transactional
@DisplayName("UnitService — équipement & déplacement")
class UnitServiceTest {

    @Autowired
    private UnitService unitService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private BuildingService buildingService;

    @Autowired
    private TurnService turnService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("assignEquipment équipe l'unité depuis l'inventaire et décompte la dispo")
    void shouldAssignEquipmentFromInventory() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        assertNotNull(player, "Le joueur TestPlayer1 doit exister avec un userId");
        Long playerId = player.getId();

        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector sector = findNeutralSector(board, 3);
        Unit unit = newUnit(playerId, UnitType.MALFRAT, Set.of(UnitClass.TIREUR));
        sector.addUnit(unit);
        entityManager.persist(unit);
        entityManager.flush();
        assertNotNull(unit.getId(), "L'unité doit être persistée");

        Equipment eq = equipmentService.findAll(org.springframework.data.domain.Pageable.ofSize(50)).stream()
                .filter(e -> e.getCategory() == EquipmentCategory.FIREARM)
                .filter(e -> e.getCompatibleClasses().contains(UnitClass.TIREUR))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Catalogue : aucun FIREARM compatible TIREUR"));
        player.addEquipmentToStack(eq, 2);
        playerService.save(player);
        player = playerOfTestUser(TestDataInitializer.USER_1);

        long dispoBefore = availableOf(eq.getName());
        assertEquals(2, dispoBefore);

        Unit updated = unitService.assignEquipment(unit.getId(), player.getUserId(), eq.getName(), player.getBoardId());

        assertTrue(updated.getEquipments().stream().anyMatch(e -> e.getName().equals(eq.getName())),
                "L'unité doit porter l'équipement après assignation");

        long dispoAfter = availableOf(eq.getName());
        assertEquals(1, dispoAfter, "La dispo du matériel doit décrémenter de 1");

        unitService.removeEquipment(unit.getId(), player.getUserId(), eq.getName(), player.getBoardId());
        long dispoFinal = availableOf(eq.getName());
        assertEquals(2, dispoFinal, "removeEquipment doit rendre l'exemplaire au matériel");
    }

    @Test
    @DisplayName("assignEquipment sur une unité d'un autre joueur → SecurityException")
    void shouldRefuseForeignUnit() {
        Player owner = playerOfTestUser(TestDataInitializer.USER_1);
        Player other = playerOfTestUser(TestDataInitializer.USER_2);
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector sector = findNeutralSector(board, 4);
        Unit unit = newUnit(other.getId(), UnitType.LARBIN, Set.of(UnitClass.LEGER));
        sector.addUnit(unit);
        entityManager.persist(unit);
        entityManager.flush();

        assertThrows(SecurityException.class,
                () -> unitService.assignEquipment(unit.getId(), owner.getUserId(), anyCompatibleEquipment().getName(),
                        owner.getBoardId()),
                "Équiper l unité d un autre joueur doit lever SecurityException");
    }

    @Test
    @DisplayName("assignEquipment d'un équipement incompatible → IllegalArgumentException")
    void shouldRefuseIncompatibleEquipment() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector sector = findNeutralSector(board, 5);
        Unit unit = newUnit(player.getId(), UnitType.LARBIN, Set.of(UnitClass.SNIPER));
        sector.addUnit(unit);
        entityManager.persist(unit);
        entityManager.flush();

        Equipment eq = anyCompatibleEquipment();
        Equipment incompatible = equipmentService.findAll(org.springframework.data.domain.Pageable.ofSize(50)).stream()
                .filter(e -> !e.getCompatibleClasses().contains(UnitClass.SNIPER))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Catalogue : aucun équipement incompatible SNIPER"));
        player.addEquipmentToStack(incompatible, 1);
        playerService.save(player);

        assertThrows(IllegalArgumentException.class,
                () -> unitService.assignEquipment(unit.getId(), player.getUserId(), incompatible.getName(),
                        player.getBoardId()),
                "Un équipement incompatible doit être refusé");
    }

    @Test
    @DisplayName("Un ordre à pied ne peut pas déplacer un bâtiment")
    void shouldRefuseBuildingInFootOrder() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();

        Sector from = board.getAllSectors().stream()
                .filter(s -> !s.getNeighbors().isEmpty())
                .findFirst().orElseThrow();
        Sector to = board.getSector(from.getNeighbors().getFirst());
        assertNotNull(to, "Le secteur voisin doit exister");

        Headquarters hq = buildingService.getHeadquarters(player.getId()).orElseThrow();
        hq.setSector(from);
        entityManager.flush();

        assertThrows(IllegalArgumentException.class,
                () -> unitService.placeFootOrder(player.getUserId(), List.of(hq.getId()),
                        List.of(from.getNumber(), to.getNumber()), player.getBoardId()),
                "Un bâtiment ne peut pas être déplacé par un ordre à pied");
    }

    @Test
    @DisplayName("removeEquipment ne retire qu'une copie d'un même équipement")
    void shouldRemoveOnlyOneDuplicateCopy() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector sector = findNeutralSector(board, 6);
        Unit unit = newUnit(player.getId(), UnitType.MALFRAT, Set.of(UnitClass.TIREUR));
        sector.addUnit(unit);
        entityManager.persist(unit);
        entityManager.flush();

        Equipment melee = equipmentService.findAll(org.springframework.data.domain.Pageable.ofSize(100)).stream()
                .filter(e -> e.getCategory() == EquipmentCategory.MELEE)
                .filter(e -> e.getCompatibleClasses().contains(UnitClass.TIREUR))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Catalogue : aucun MELEE compatible TIREUR"));
        player.addEquipmentToStack(melee, 2);
        playerService.save(player);

        unitService.assignEquipment(unit.getId(), player.getUserId(), melee.getName(), player.getBoardId());
        unitService.assignEquipment(unit.getId(), player.getUserId(), melee.getName(), player.getBoardId());

        unitService.removeEquipment(unit.getId(), player.getUserId(), melee.getName(), player.getBoardId());

        assertEquals(1, unit.getEquipments().size(), "Une seule copie doit rester équipée");
        assertEquals(1, availableOf(melee.getName()), "Une seule copie doit revenir au stock");

        unitService.removeEquipment(unit.getId(), player.getUserId(), melee.getName(), player.getBoardId());

        assertEquals(0, unit.getEquipments().size());
        assertEquals(2, availableOf(melee.getName()));
    }

    @Test
    @DisplayName("autoEquipRemaining équipe d'abord les unités les plus expérimentées")
    void shouldAutoEquipBestStockByExperience() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector sector = findNeutralSector(board, 8);
        Unit novice = newUnit(player.getId(), UnitType.LARBIN, Set.of(UnitClass.LEGER), 0);
        Unit veteran = newUnit(player.getId(), UnitType.VOYOU, Set.of(UnitClass.LEGER), 2);
        sector.addUnit(novice);
        sector.addUnit(veteran);
        Unit reserve = newUnit(player.getId(), UnitType.MALFRAT, Set.of(UnitClass.LEGER), 8);
        Unit destroyed = newUnit(player.getId(), UnitType.MALFRAT, Set.of(UnitClass.LEGER), 9);
        destroyed.setDestroyed(true);
        sector.addUnit(destroyed);
        entityManager.persist(novice);
        entityManager.persist(veteran);
        entityManager.persist(reserve);
        entityManager.persist(destroyed);
        entityManager.flush();

        List<Equipment> firearms = equipmentByCategory(EquipmentCategory.FIREARM, UnitClass.LEGER);
        Equipment best = firearms.get(0);
        Equipment runnerUp = firearms.get(1);
        player.addEquipmentToStack(best, 1);
        player.addEquipmentToStack(runnerUp, 1);
        Equipment melee = equipmentByCategory(EquipmentCategory.MELEE, UnitClass.LEGER).getFirst();
        player.addEquipmentToStack(melee, 1);
        playerService.save(player);

        AutoEquipResultDto result = unitService.autoEquipRemaining(
                player.getUserId(), List.of(EquipmentCategory.FIREARM.name()), null, player.getBoardId());

        assertEquals(2, result.getEquippedCount());
        assertEquals(2, result.getUnitCount());
        assertTrue(hasEquipment(veteran, best.getName()), "L'unité la plus expérimentée doit recevoir le meilleur stock");
        assertTrue(hasEquipment(novice, runnerUp.getName()));
        assertTrue(reserve.getEquipments().isEmpty(), "Une unité en réserve ne doit pas être équipée");
        assertTrue(destroyed.getEquipments().isEmpty(), "Une unité détruite ne doit pas être équipée");
        assertEquals(0, availableOf(best.getName()));
        assertEquals(1, availableOf(melee.getName()), "Une catégorie non demandée doit rester en stock");
    }

    @Test
    @DisplayName("autoEquipRemaining remplit plusieurs emplacements de la même catégorie")
    void shouldAutoEquipSeveralSlotsOfSameCategory() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector sector = findNeutralSector(board, 10);
        Unit veteran = newUnit(player.getId(), UnitType.MALFRAT, Set.of(UnitClass.TIREUR), 5);
        Unit recruit = newUnit(player.getId(), UnitType.VOYOU, Set.of(UnitClass.TIREUR), 2);
        sector.addUnit(veteran);
        sector.addUnit(recruit);
        entityManager.persist(veteran);
        entityManager.persist(recruit);
        entityManager.flush();

        Equipment bestMelee = equipmentByCategory(EquipmentCategory.MELEE, UnitClass.TIREUR).getFirst();
        player.addEquipmentToStack(bestMelee, 3);
        playerService.save(player);

        AutoEquipResultDto result = unitService.autoEquipRemaining(
                player.getUserId(), List.of(EquipmentCategory.MELEE.name()), null, player.getBoardId());

        assertEquals(3, result.getEquippedCount());
        assertEquals(2, result.getUnitCount());
        assertEquals(2L, countEquipment(veteran, bestMelee.getName()));
        assertEquals(1L, countEquipment(recruit, bestMelee.getName()));
        assertEquals(0, availableOf(bestMelee.getName()));
    }

    @Test
    @DisplayName("autoEquipRemaining ne distribue que les équipements sélectionnés")
    void shouldAutoEquipOnlySelectedNames() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector sector = findNeutralSector(board, 9);
        Unit unit = newUnit(player.getId(), UnitType.LARBIN, Set.of(UnitClass.LEGER), 0);
        sector.addUnit(unit);
        entityManager.persist(unit);
        entityManager.flush();

        List<Equipment> firearms = equipmentByCategory(EquipmentCategory.FIREARM, UnitClass.LEGER);
        Equipment best = firearms.get(0);
        Equipment runnerUp = firearms.get(1);
        player.addEquipmentToStack(best, 1);
        player.addEquipmentToStack(runnerUp, 1);
        playerService.save(player);

        AutoEquipResultDto result = unitService.autoEquipRemaining(
                player.getUserId(), List.of(EquipmentCategory.FIREARM.name()),
                List.of(runnerUp.getName()), player.getBoardId());

        assertEquals(1, result.getEquippedCount());
        assertTrue(hasEquipment(unit, runnerUp.getName()));
        assertEquals(1, availableOf(best.getName()), "L'arme non sélectionnée doit rester en stock");
    }

    @Test
    @DisplayName("placeUnit d'une unité d'un autre joueur → SecurityException")
    void shouldRefusePlacingForeignUnit() {
        Player owner = playerOfTestUser(TestDataInitializer.USER_1);
        Player other = playerOfTestUser(TestDataInitializer.USER_2);
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector sector = findNeutralSector(board, 7);
        Unit unit = newUnit(other.getId(), UnitType.LARBIN, Set.of(UnitClass.LEGER));
        entityManager.persist(unit);
        entityManager.flush();

        assertThrows(SecurityException.class,
                () -> unitService.placeUnit(owner.getUserId(), unit.getId(), board.getId(), sector.getNumber()),
                "Placer l'unité d'un autre joueur doit lever SecurityException");
    }

    @Test
    @DisplayName("buyUnits : entrée invalide → IllegalArgumentException")
    void shouldRejectInvalidBuyUnitsInput() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);

        assertThrows(IllegalArgumentException.class,
                () -> unitService.buyUnits(player.getUserId(), List.of(), player.getBoardId()));

        BuyUnitRequestDto blankType = new BuyUnitRequestDto();
        blankType.setUnitType(" ");
        blankType.setUnitClass(UnitClass.LEGER.name());
        assertThrows(IllegalArgumentException.class,
                () -> unitService.buyUnits(player.getUserId(), List.of(blankType), player.getBoardId()));

        // LARBIN n'est achetable qu'à partir du tour 2 : on force un tour éligible puis on restaure.
        int originalTurn = turnService.getCurrentTurn(player.getBoardId());
        try {
            turnService.setCurrentTurn(player.getBoardId(), Math.max(2, originalTurn));

            BuyUnitRequestDto zeroQuantity = new BuyUnitRequestDto();
            zeroQuantity.setUnitType(UnitType.LARBIN.name());
            zeroQuantity.setUnitClass(UnitClass.LEGER.name());
            zeroQuantity.setQuantity(0);
            assertThrows(IllegalArgumentException.class,
                    () -> unitService.buyUnits(player.getUserId(), List.of(zeroQuantity), player.getBoardId()));
        } finally {
            turnService.setCurrentTurn(player.getBoardId(), originalTurn);
            turnService.invalidateTurnCache();
        }
    }

    /** Résout le joueur par nom d'utilisateur : les ids générés dépendent de la séquence, pas d'une constante. */
    private Player playerOfTestUser(String username) {
        return DemoFixtures.playerOfTestUser(userRepository, playerService, boardService, username);
    }

    private static Sector findNeutralSector(Board board, int preferred) {
        Sector s = board.getSector(preferred);
        if (s != null && (s.getArmy() == null || s.getArmy().isEmpty())) return s;
        return board.getAllSectors().stream()
                .filter(x -> x.getArmy() == null || x.getArmy().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun secteur neutre disponible pour le setup"));
    }

    private static Unit newUnit(Long playerId, UnitType type, Set<UnitClass> classes) {
        return newUnit(playerId, type, classes, 5.0);
    }

    private static Unit newUnit(Long playerId, UnitType type, Set<UnitClass> classes, double experience) {
        Unit u = new Unit();
        u.setPlayerId(playerId);
        u.setType(type);
        u.setClasses(List.copyOf(classes));
        u.setExperience(experience);
        u.setNumber(1);
        return u;
    }

    private List<Equipment> equipmentByCategory(EquipmentCategory category, UnitClass unitClass) {
        return equipmentService.findAll(org.springframework.data.domain.Pageable.ofSize(100)).stream()
                .filter(e -> e.getCategory() == category)
                .filter(e -> e.getCompatibleClasses().contains(unitClass))
                .sorted(Comparator.comparingInt(Equipment::getCost).reversed())
                .toList();
    }

    private static boolean hasEquipment(Unit unit, String equipmentName) {
        return unit.getEquipments().stream()
                .anyMatch(e -> e.getName().equals(equipmentName));
    }

    private static long countEquipment(Unit unit, String equipmentName) {
        return unit.getEquipments().stream()
                .filter(e -> e.getName().equals(equipmentName))
                .count();
    }

    private Equipment anyCompatibleEquipment() {
        return equipmentService.findAll(org.springframework.data.domain.Pageable.ofSize(50)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Catalogue d'équipement vide"));
    }

    private int availableOf(String equipmentName) {
        return playerOfTestUser(TestDataInitializer.USER_1)
                .getAvailableEquipmentQuantity(equipmentName);
    }
}
