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

import java.util.ArrayList;
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
    private final UnitRepository unitRepository;
    private final UnitMapper unitMapper;
    private final MovementMapper movementMapper;
    private final EntityManager em;

    public UnitService(BoardService boardService,
                        PlayerService playerService,
                        MovementService movementService,
                        TurnService turnService,
                        UnitRepository unitRepository,
                        UnitMapper unitMapper,
                        MovementMapper movementMapper,
                        EntityManager em) {
        this.boardService = boardService;
        this.playerService = playerService;
        this.movementService = movementService;
        this.turnService = turnService;
        this.unitRepository = unitRepository;
        this.unitMapper = unitMapper;
        this.movementMapper = movementMapper;
        this.em = em;
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

        // Unit.unitEquipments n'a plus orphanRemoval (Phase 2: voir Unit.java) — son
        // retrait de la collection en mémoire n'émet plus de DELETE SQL automatique.
        // On détruit donc explicitement les rows persistées via em.remove(ue) avant
        // le retrait collection (sinon les rows orphelines subsisteraient en base).
        List<UnitEquipment> persistedUEs = new ArrayList<>();
        for (UnitEquipment ue : unit.getUnitEquipments()) {
            if (ue.getEquipment() != null && ue.getEquipment().getName().equals(equipmentName)) {
                persistedUEs.add(ue);
            }
        }
        if (persistedUEs.isEmpty()) {
            throw new IllegalArgumentException(
                    "L'unité #" + unit.getId() + " ne porte pas l'équipement \"" + equipmentName + "\".");
        }
        persistedUEs.forEach(em::remove);

        // Mutations in-memory (collection transient `equipments` + bag `unitEquipments`)
        // et recalcul des stats de l'unité — recalculateBaseStats reposé car les bonus
        // équipement ne s'appliquent plus.
        boolean removed = unit.removeEquipment(stack.getEquipment());
        if (!removed) {
            // Cohérence : persistedUEs était non-vide, donc unit.removeEquipment devait
            // trouver l'entry. Sinon, incohérence transient vs persistant — on lève tôt.
            throw new IllegalStateException(
                    "Incohérence : UnitEquipment persistés trouvés pour \"" + equipmentName
                            + "\" mais unit.removeEquipment transient n'a rien retiré.");
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
        // Lookup direct par ID : l'ancienne version initialisait l'armée de chaque
        // secteur du plateau (1 SELECT lazy par secteur) jusqu'à trouver l'unité.
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