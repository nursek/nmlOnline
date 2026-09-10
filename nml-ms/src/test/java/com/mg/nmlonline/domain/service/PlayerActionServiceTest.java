package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.BuyEquipmentItemDto;
import com.mg.nmlonline.api.dto.BuyVehicleRequestDto;
import com.mg.nmlonline.api.dto.PlayerActionDto;
import com.mg.nmlonline.config.TestDataInitializer;
import com.mg.nmlonline.domain.model.action.PlayerActionType;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.domain.model.user.User;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.UserRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@Transactional
@DisplayName("PlayerActionService — journal & annulation en cascade")
class PlayerActionServiceTest {

    @Autowired
    private PlayerActionService playerActionService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private UnitService unitService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Achat + équipement : annuler l'achat annule d'abord l'équipement (rembourse et déséquipe)")
    void undoBuyEquipmentCascadesOverEquip() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        double moneyBefore = fund(player);

        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector sector = findNeutralSector(board);
        Unit unit = newUnit(player.getId(), UnitType.MALFRAT, Set.of(UnitClass.TIREUR));
        sector.addUnit(unit);
        entityManager.persist(unit);
        entityManager.flush();

        Equipment eq = firearmFor(UnitClass.TIREUR);
        BuyEquipmentItemDto item = new BuyEquipmentItemDto();
        item.setName(eq.getName());
        item.setQuantity(2);
        playerService.buyEquipments(player.getId(), List.of(item));
        unitService.assignEquipment(unit.getId(), player.getUserId(), eq.getName());

        List<PlayerActionDto> actions = playerActionService.getCurrentTurnActions(player.getUserId());
        assertEquals(2, actions.size());
        assertEquals(PlayerActionType.BUY_EQUIPMENT, actions.get(0).getType());
        assertEquals(PlayerActionType.EQUIP_UNIT, actions.get(1).getType());

        playerActionService.undoFrom(player.getUserId(), actions.get(0).getId());

        assertTrue(playerActionService.getCurrentTurnActions(player.getUserId()).isEmpty(),
                "L'annulation en cascade doit vider le journal du tour");
        Player reloaded = playerRepository.findById(player.getId()).orElseThrow();
        assertEquals(moneyBefore, reloaded.getStats().getMoney(), 0.001,
                "L'argent de l'achat et l'équipement doivent être remboursés");
        assertTrue(reloaded.getEquipments().stream()
                        .noneMatch(s -> s.getEquipment().getName().equals(eq.getName())),
                "Le stock acheté doit être supprimé après annulation");
    }

    @Test
    @DisplayName("Achat + placement véhicule : l'annulation en cascade retire le véhicule et rembourse")
    void undoVehicleBuyCascadesOverPlacement() {
        Player player = playerOfTestUser(TestDataInitializer.USER_1);
        double moneyBefore = fund(player);

        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector owned = findNeutralSector(board);
        owned.setOwnerId(player.getId());
        entityManager.flush();

        BuyVehicleRequestDto item = new BuyVehicleRequestDto();
        item.setVehicleType("TANK");
        item.setQuantity(1);
        List<com.mg.nmlonline.domain.model.vehicle.Vehicle> created =
                vehicleService.buyVehiclesBatch(player.getUserId(), List.of(item));
        Long vehicleId = created.getFirst().getId();
        vehicleService.placeVehicle(vehicleId, board.getId(), owned.getNumber(), player.getUserId());

        List<PlayerActionDto> actions = playerActionService.getCurrentTurnActions(player.getUserId());
        assertEquals(2, actions.size());
        assertEquals(PlayerActionType.BUY_VEHICLE, actions.get(0).getType());
        assertEquals(PlayerActionType.PLACE_VEHICLE, actions.get(1).getType());

        playerActionService.undoFrom(player.getUserId(), actions.get(0).getId());

        assertTrue(vehicleRepository.findById(vehicleId).isEmpty(),
                "Le véhicule acheté doit être supprimé");
        Player reloaded = playerRepository.findById(player.getId()).orElseThrow();
        assertEquals(moneyBefore, reloaded.getStats().getMoney(), 0.001,
                "Le coût du véhicule doit être remboursé");
    }

    private double fund(Player player) {
        player.getStats().setMoney(100000.0);
        playerRepository.save(player);
        return 100000.0;
    }

    private Player playerOfTestUser(String username) {
        User user = userRepository.findByUsername(username);
        assertNotNull(user, "L'utilisateur de test " + username + " doit exister");
        Player player = playerService.findByUserId(user.getId());
        assertNotNull(player, "Le joueur lié à " + username + " doit exister");
        return player;
    }

    private Sector findNeutralSector(Board board) {
        return board.getAllSectors().stream()
                .filter(s -> s.getArmy() == null || s.getArmy().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun secteur neutre disponible"));
    }

    private Unit newUnit(Long playerId, UnitType type, Set<UnitClass> classes) {
        Unit u = new Unit();
        u.setPlayerId(playerId);
        u.setType(type);
        u.setClasses(List.copyOf(classes));
        u.setExperience(5.0);
        u.setNumber(1);
        return u;
    }

    private Equipment firearmFor(UnitClass unitClass) {
        return equipmentService.findAll(org.springframework.data.domain.Pageable.ofSize(100)).stream()
                .filter(e -> e.getCategory() == EquipmentCategory.FIREARM)
                .filter(e -> e.getCompatibleClasses().contains(unitClass))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Catalogue : aucun FIREARM compatible " + unitClass));
    }
}
