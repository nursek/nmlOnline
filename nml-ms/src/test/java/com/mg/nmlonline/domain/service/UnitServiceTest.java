package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
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
    private EquipmentService equipmentService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("assignEquipment équipe l'unité depuis l'inventaire et décompte la dispo")
    void shouldAssignEquipmentFromInventory() {
        Player player = playerService.findByUserId(1L);
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
        player = playerService.findByUserId(1L);

        long dispoBefore = player.getEquipments().stream()
                .filter(s -> s.getEquipment().getName().equals(eq.getName()))
                .mapToInt(s -> s.getAvailable()).findFirst().orElse(-1);
        assertEquals(2, dispoBefore);

        Unit updated = unitService.assignEquipment(unit.getId(), 1L, eq.getName());

        assertTrue(updated.getEquipments().stream().anyMatch(e -> e.getName().equals(eq.getName())),
                "L'unité doit porter l'équipement après assignation");

        player = playerService.findByUserId(1L);
        long dispoAfter = player.getEquipments().stream()
                .filter(s -> s.getEquipment().getName().equals(eq.getName()))
                .mapToInt(s -> s.getAvailable()).findFirst().orElse(-1);
        assertEquals(1, dispoAfter, "La dispo de l'inventaire doit décrémenter de 1");

        unitService.removeEquipment(unit.getId(), 1L, eq.getName());
        player = playerService.findByUserId(1L);
        long dispoFinal = player.getEquipments().stream()
                .filter(s -> s.getEquipment().getName().equals(eq.getName()))
                .mapToInt(s -> s.getAvailable()).findFirst().orElse(-1);
        assertEquals(2, dispoFinal, "removeEquipment doit rendre l'exemplaire à l'inventaire");
    }

    @Test
    @DisplayName("assignEquipment sur une unité d'un autre joueur → SecurityException")
    void shouldRefuseForeignUnit() {
        Player owner = playerService.findByUserId(1L);
        Player other = playerRepository.findAll().stream()
                .filter(p -> !owner.getId().equals(p.getId()) && p.getUserId() != null && !owner.getUserId().equals(p.getUserId()))
                .findFirst()
                .orElseThrow();
        Board board = boardService.getAllBoards().stream().findFirst().orElseThrow();
        Sector sector = findNeutralSector(board, 4);
        Unit unit = newUnit(other.getId(), UnitType.LARBIN, Set.of(UnitClass.ELEMENTAIRE));
        sector.addUnit(unit);
        entityManager.persist(unit);
        entityManager.flush();

        assertThrows(SecurityException.class,
                () -> unitService.assignEquipment(unit.getId(), owner.getUserId(), anyCompatibleEquipment().getName()),
                "Équiper l'unité d'un autre joueur doit lever SecurityException");
    }

    @Test
    @DisplayName("assignEquipment d'un équipement incompatible → IllegalArgumentException")
    void shouldRefuseIncompatibleEquipment() {
        Player player = playerService.findByUserId(1L);
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
                () -> unitService.assignEquipment(unit.getId(), 1L, incompatible.getName()),
                "Un équipement incompatible doit être refusé");
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
        Unit u = new Unit();
        u.setPlayerId(playerId);
        u.setType(type);
        u.setClasses(List.copyOf(classes));
        u.setExperience(5.0);
        u.setNumber(1);
        return u;
    }

    private Equipment anyCompatibleEquipment() {
        return equipmentService.findAll(org.springframework.data.domain.Pageable.ofSize(50)).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Catalogue d'équipement vide"));
    }
}