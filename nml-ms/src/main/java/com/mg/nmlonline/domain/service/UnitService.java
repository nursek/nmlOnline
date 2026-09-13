package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.MovementOrderDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitEquipment;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import com.mg.nmlonline.mapper.MovementMapper;
import com.mg.nmlonline.mapper.UnitMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des unités par un joueur authentifié : équipement et ordres à pied.
 *
 * <p>Ownership : {@code playerId} jamais lu depuis le corps — re-dérivé du {@code userId}
 * du JWT ; tout écart lève {@link SecurityException} → 403.
 */
@Service
@Transactional
public class UnitService {

    private final BoardService boardService;
    private final PlayerService playerService;
    private final PlayerActionService playerActionService;
    private final MovementService movementService;
    private final TurnService turnService;
    private final UnitRepository unitRepository;
    private final UnitMapper unitMapper;
    private final MovementMapper movementMapper;
    private final EntityManager em;

    public UnitService(BoardService boardService,
                        PlayerService playerService,
                        PlayerActionService playerActionService,
                        MovementService movementService,
                        TurnService turnService,
                        UnitRepository unitRepository,
                        UnitMapper unitMapper,
                        MovementMapper movementMapper,
                        EntityManager em) {
        this.boardService = boardService;
        this.playerService = playerService;
        this.playerActionService = playerActionService;
        this.movementService = movementService;
        this.turnService = turnService;
        this.unitRepository = unitRepository;
        this.unitMapper = unitMapper;
        this.movementMapper = movementMapper;
        this.em = em;
    }

    public Unit assignEquipment(Long unitId, Long userId, String equipmentName) {
        Player player = requirePlayerByUserIdForUpdate(userId);
        Board board = requireBoard();
        Unit unit = requireUnit(board, unitId);
        requireOwnedBy(unit, player);

        EquipmentStack stack = player.getEquipments().stream()
                .filter(s -> s.getEquipment() != null && s.getEquipment().getName().equals(equipmentName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Équipement \"" + equipmentName + "\" introuvable dans l'inventaire du joueur."));

        if (stack.getAvailable() <= 0) {
            throw new IllegalArgumentException(
                    "Aucun exemplaire disponible de \"" + equipmentName + "\" dans l'inventaire.");
        }

        if (!unit.canEquip(stack.getEquipment())) {
            throw new IllegalArgumentException(
                    "L'unité #" + unit.getId() + " ne peut pas équiper \"" + equipmentName
                            + "\" (classe incompatible ou catégorie pleine).");
        }

        unit.addEquipment(stack.getEquipment());
        player.decrementEquipmentAvailability(stack.getEquipment());
        playerService.save(player);
        boardService.save(board);
        playerActionService.recordEquipUnit(player.getId(), unit.getId(), equipmentName);
        return unit;
    }

    public Unit removeEquipment(Long unitId, Long userId, String equipmentName) {
        Player player = requirePlayerByUserIdForUpdate(userId);
        Board board = requireBoard();
        Unit unit = requireUnit(board, unitId);
        requireOwnedBy(unit, player);

        // Retrouver l'Equipment du catalogue par nom : s'il n'est plus en stock, on ne peut l'identifier.
        EquipmentStack stack = player.getEquipments().stream()
                .filter(s -> s.getEquipment() != null && s.getEquipment().getName().equals(equipmentName))
                .findFirst()
                .orElse(null);

        if (stack == null) {
            throw new IllegalArgumentException(
                    "Aucun équipement \"" + equipmentName + "\" dans l'inventaire du joueur.");
        }

        // Sans orphanRemoval : em.remove d'une occurrence (les doublons doivent survivre au clic).
        UnitEquipment row = unit.removeOneEquipment(equipmentName);
        if (row == null) {
            throw new IllegalArgumentException(
                    "L'unité #" + unit.getId() + " ne porte pas l'équipement \"" + equipmentName + "\".");
        }
        em.remove(row);

        player.incrementEquipmentAvailability(stack.getEquipment());
        playerService.save(player);
        boardService.save(board);
        playerActionService.recordUnequipUnit(player.getId(), unit.getId(), equipmentName);
        return unit;
    }

    public MovementOrder placeFootOrder(Long userId, List<Long> entityIds, List<Integer> route) {
        // Verrou joueur : sérialise avec setCrew (occupant vs ordre à pied) sur le même ordre P→V.
        Player player = requirePlayerByUserIdForUpdate(userId);
        Board board = requireBoard();
        int turn = turnService.getCurrentTurn();
        return movementService.placeFootOrder(player.getId(), turn, entityIds, route, board);
    }

    public List<MovementOrder> getPlayerPendingOrders(Long userId) {
        Player player = requirePlayerByUserId(userId);
        int turn = turnService.getCurrentTurn();
        return movementService.getPlayerOrders(player.getId(), turn);
    }

    public void cancelOrder(Long userId, Long orderId) {
        Player player = requirePlayerByUserId(userId);
        movementService.cancelOrderOrThrow(player.getId(), orderId);
    }

    // === Mapping dans la transaction (classesSet/unitEquipments et route/entityIds sont LAZY) ===

    public UnitDto assignEquipmentDto(Long unitId, Long userId, String equipmentName) {
        return unitMapper.toDto(assignEquipment(unitId, userId, equipmentName));
    }

    public UnitDto removeEquipmentDto(Long unitId, Long userId, String equipmentName) {
        return unitMapper.toDto(removeEquipment(unitId, userId, equipmentName));
    }

    public MovementOrderDto placeFootOrderDto(Long userId, List<Long> entityIds, List<Integer> route) {
        return movementMapper.toDto(placeFootOrder(userId, entityIds, route));
    }

    public List<MovementOrderDto> getPlayerPendingOrdersDto(Long userId) {
        return getPlayerPendingOrders(userId).stream().map(movementMapper::toDto).toList();
    }

    private Player requirePlayerByUserId(Long userId) {
        Player player = playerService.findByUserId(userId);
        if (player == null) {
            throw new EntityNotFoundException("Joueur introuvable pour l'utilisateur " + userId);
        }
        return player;
    }

    private Player requirePlayerByUserIdForUpdate(Long userId) {
        Player player = playerService.findByUserIdForUpdate(userId);
        if (player == null) {
            throw new EntityNotFoundException("Joueur introuvable pour l'utilisateur " + userId);
        }
        return player;
    }

    private Board requireBoard() {
        return boardService.getAllBoards().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun plateau disponible."));
    }

    private Unit requireUnit(Board board, Long unitId) {
        if (unitId == null) {
            throw new IllegalArgumentException("L'ID de l'unité est requis.");
        }
        // Lookup direct par ID : évite l'ancien scan (1 SELECT lazy par secteur) jusqu'à trouver l'unité.
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("Unité introuvable avec l'ID " + unitId));
        if (unit.getSector() == null || unit.getSector().getBoard() == null
                || !unit.getSector().getBoard().getId().equals(board.getId())) {
            throw new EntityNotFoundException("Unité introuvable avec l'ID " + unitId);
        }
        return unit;
    }

    private void requireOwnedBy(Unit unit, Player player) {
        if (player.getId() == null || !player.getId().equals(unit.getPlayerId())) {
            throw new SecurityException("Cette unité n'appartient pas au joueur authentifié.");
        }
    }
}