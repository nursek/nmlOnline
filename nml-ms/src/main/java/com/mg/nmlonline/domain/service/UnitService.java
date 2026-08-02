package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de gestion des unités par un joueur authentifié :
 * <ul>
 *   <li>Équipement / retrait d'équipement (depuis l'inventaire du joueur) ;</li>
 *   <li>Ordres de déplacement à pied (délégation à {@link MovementService}).</li>
 * </ul>
 *
 * <p>Règle d'ownership (AGENTS.md) : le {@code playerId} n'est jamais lu depuis
 * le corps de la requête — il est re-dérivé depuis le {@code userId} porté par
 * le JWT. Tout écart lève une {@link SecurityException} → 403.
 */
@Service
@Transactional
public class UnitService {

    private final BoardService boardService;
    private final PlayerService playerService;
    private final MovementService movementService;
    private final TurnService turnService;

    public UnitService(BoardService boardService,
                        PlayerService playerService,
                        MovementService movementService,
                        TurnService turnService) {
        this.boardService = boardService;
        this.playerService = playerService;
        this.movementService = movementService;
        this.turnService = turnService;
    }

    // ============================================================
    // === ÉQUIPEMENT D'UNITÉ =====================================
    // ============================================================

    /** ÉQUIPE une unité depuis l'inventaire du joueur authentifié. */
    public Unit assignEquipment(Long unitId, Long userId, String equipmentName) {
        Player player = requirePlayerByUserId(userId);
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
        return unit;
    }

    /** RETIRE un équipement de l'unité et le rend à l'inventaire du joueur. */
    public Unit removeEquipment(Long unitId, Long userId, String equipmentName) {
        Player player = requirePlayerByUserId(userId);
        Board board = requireBoard();
        Unit unit = requireUnit(board, unitId);
        requireOwnedBy(unit, player);

        // On doit retrouver l'Equipment du catalogue portant ce nom : s'il n'est
        // plus en stock (quantité 0 ou stack absent), on ne peut pas l'identifier.
        EquipmentStack stack = player.getEquipments().stream()
                .filter(s -> s.getEquipment() != null && s.getEquipment().getName().equals(equipmentName))
                .findFirst()
                .orElse(null);

        // Si le stack manque totalement, on ne peut pas récupérer l'instance Equipment
        // (le catalogue n'est pas chargé ici). On refuse explicitement.
        if (stack == null) {
            throw new IllegalArgumentException(
                    "Aucun équipement \"" + equipmentName + "\" dans l'inventaire du joueur.");
        }

        boolean removed = unit.removeEquipment(stack.getEquipment());
        if (!removed) {
            throw new IllegalArgumentException(
                    "L'unité #" + unit.getId() + " ne porte pas l'équipement \"" + equipmentName + "\".");
        }
        player.incrementEquipmentAvailability(stack.getEquipment());
        playerService.save(player);
        boardService.save(board);
        return unit;
    }

    // ============================================================
    // === ORDRES DE DÉPLACEMENT À PIED ===========================
    // ============================================================

    public MovementOrder placeFootOrder(Long userId, List<Long> entityIds, List<Integer> route) {
        Player player = requirePlayerByUserId(userId);
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

    // ============================================================
    // === HELPERS =================================================
    // ============================================================

    private Player requirePlayerByUserId(Long userId) {
        Player player = playerService.findByUserId(userId);
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
        for (Sector sector : board.getAllSectors()) {
            if (sector.getArmy() == null) continue;
            for (Unit unit : sector.getArmy()) {
                if (unitId.equals(unit.getId())) {
                    return unit;
                }
            }
        }
        throw new EntityNotFoundException("Unité introuvable avec l'ID " + unitId);
    }

    private void requireOwnedBy(Unit unit, Player player) {
        if (player.getId() == null || !player.getId().equals(unit.getPlayerId())) {
            throw new SecurityException("Cette unité n'appartient pas au joueur authentifié.");
        }
    }
}