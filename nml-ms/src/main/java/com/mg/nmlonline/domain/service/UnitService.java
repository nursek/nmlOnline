package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BuyUnitRequestDto;
import com.mg.nmlonline.api.dto.MovementOrderDto;
import com.mg.nmlonline.api.dto.UnitCatalogDto;
import com.mg.nmlonline.api.dto.UnitCatalogEntryDto;
import com.mg.nmlonline.api.dto.UnitDto;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import com.mg.nmlonline.domain.model.action.PlayerAction;
import com.mg.nmlonline.domain.model.action.PlayerActionStatus;
import com.mg.nmlonline.domain.model.action.PlayerActionType;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.unit.UnitEquipment;
import com.mg.nmlonline.domain.model.unit.UnitType;
import com.mg.nmlonline.infrastructure.repository.PlayerActionRepository;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import com.mg.nmlonline.mapper.MovementMapper;
import com.mg.nmlonline.mapper.UnitMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestion des unités par un joueur authentifié : recrutement, équipement et ordres à pied.
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
    private final TurnLock turnLock;
    private final UnitRepository unitRepository;
    private final PlayerActionRepository actionRepository;
    private final SectorRepository sectorRepository;
    private final UnitMapper unitMapper;
    private final MovementMapper movementMapper;
    private final EntityManager em;

    public UnitService(BoardService boardService,
                        PlayerService playerService,
                        PlayerActionService playerActionService,
                        MovementService movementService,
                        TurnService turnService,
                        TurnLock turnLock,
                        UnitRepository unitRepository,
                        PlayerActionRepository actionRepository,
                        SectorRepository sectorRepository,
                        UnitMapper unitMapper,
                        MovementMapper movementMapper,
                        EntityManager em) {
        this.boardService = boardService;
        this.playerService = playerService;
        this.playerActionService = playerActionService;
        this.movementService = movementService;
        this.turnService = turnService;
        this.turnLock = turnLock;
        this.unitRepository = unitRepository;
        this.actionRepository = actionRepository;
        this.sectorRepository = sectorRepository;
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

    @Transactional(readOnly = true)
    public UnitCatalogDto getCatalog(Long userId) {
        Player player = requirePlayerByUserId(userId);
        int turn = turnService.getCurrentTurn();
        Map<String, Integer> purchased = purchasedByType(player.getId(), turn);

        UnitCatalogDto catalog = new UnitCatalogDto();
        catalog.setClasses(Arrays.stream(UnitClass.values()).map(unitMapper::toUnitClassDto).toList());
        List<UnitCatalogEntryDto> entries = new ArrayList<>();
        for (UnitType type : UnitType.values()) {
            if (!type.isPurchasable()) {
                continue;
            }
            UnitCatalogEntryDto entry = new UnitCatalogEntryDto();
            entry.setName(type.name());
            entry.setCost(type.getCost());
            entry.setBaseAttack(type.getBaseAttack());
            entry.setBaseDefense(type.getBaseDefense());
            entry.setAvailableFromTurn(type.getAvailableFromTurn());
            entry.setMaxPerTurn(type.maxPerTurnAt(turn));
            entry.setPurchasedThisTurn(purchased.getOrDefault(type.name(), 0));
            entry.setAvailableNow(type.isAvailableAt(turn));
            entries.add(entry);
        }
        catalog.setEntries(entries);
        return catalog;
    }

    public List<Unit> buyUnits(Long userId, List<BuyUnitRequestDto> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Le panier d'unités est vide");
        }
        requireTurnOpen();
        Player player = requirePlayerByUserIdForUpdate(userId);
        int turn = turnService.getCurrentTurn();

        record OrderLine(UnitType type, UnitClass unitClass, int quantity) {
        }
        List<OrderLine> lines = new ArrayList<>();
        Map<UnitType, Long> requested = new EnumMap<>(UnitType.class);
        long totalCost = 0;
        for (BuyUnitRequestDto item : items) {
            UnitType type = parseUnitType(item);
            UnitClass unitClass = parseUnitClass(item.getUnitClass());
            if (!type.isPurchasable()) {
                throw new IllegalArgumentException("Type d'unité non achetable : " + type.name());
            }
            if (!type.isAvailableAt(turn)) {
                throw new IllegalStateException("« " + type.name() + " » est disponible à l'achat à partir du tour "
                        + type.getAvailableFromTurn());
            }
            if (item.getQuantity() < 1) {
                throw new IllegalArgumentException("La quantité doit être au moins 1");
            }
            // Aucune expansion avant validation : une quantité arbitraire ne doit pas allouer de listes.
            lines.add(new OrderLine(type, unitClass, item.getQuantity()));
            requested.merge(type, (long) item.getQuantity(), Long::sum);
            totalCost += (long) type.getCost() * item.getQuantity();
        }

        Map<String, Integer> purchased = purchasedByType(player.getId(), turn);
        for (Map.Entry<UnitType, Long> entry : requested.entrySet()) {
            int max = entry.getKey().maxPerTurnAt(turn);
            long already = purchased.getOrDefault(entry.getKey().name(), 0);
            if (already + entry.getValue() > max) {
                throw new IllegalStateException("Limite d'achat atteinte pour " + entry.getKey().name()
                        + " : " + max + " par tour (déjà " + already + ")");
            }
        }
        if (player.getStats().getMoney() < totalCost) {
            throw new InsufficientFundsException(
                    "Fonds insuffisants pour acheter ces unités (coût total : " + totalCost + " ₡)");
        }

        List<Unit> created = new ArrayList<>();
        for (OrderLine line : lines) {
            for (int i = 0; i < line.quantity(); i++) {
                double startingShare = player.startingShareOf(line.type().getCost());
                if (!player.spendMoney(line.type().getCost())) {
                    throw new InsufficientFundsException("Fonds insuffisants pour acheter l'unité "
                            + line.type().name());
                }
                Unit unit = new Unit(line.type().getMinExp(), line.unitClass());
                unit.setPlayerId(player.getId());
                Unit saved = unitRepository.save(unit);
                created.add(saved);
                playerActionService.recordBuyUnit(player.getId(), saved.getId(), line.type().name(),
                        line.type().getCost(), startingShare);
            }
        }
        playerService.save(player);
        return created;
    }

    public Unit placeUnit(Long userId, Long unitId, Long boardId, int sectorNumber) {
        requireTurnOpen();
        // Verrou joueur d'abord : sérialise avec l'undo (joueur puis unité) et évite le double placement.
        Player player = requirePlayerByUserIdForUpdate(userId);
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("Unité introuvable avec l'ID " + unitId));
        if (!player.getId().equals(unit.getPlayerId())) {
            throw new SecurityException("Cette unité ne vous appartient pas");
        }
        if (unit.getSector() != null) {
            throw new IllegalStateException("L'unité est déjà déployée.");
        }
        if (unit.isDestroyed()) {
            throw new IllegalStateException("L'unité est détruite.");
        }
        Sector sector = sectorRepository.findByBoard_IdAndNumber(boardId, sectorNumber)
                .orElseThrow(() -> new EntityNotFoundException("Secteur introuvable"));
        if (!player.getId().equals(sector.getOwnerId())) {
            throw new SecurityException("Vous ne possédez pas ce secteur");
        }
        sector.addUnit(unit);
        unitRepository.save(unit);
        playerActionService.recordPlaceUnit(player.getId(), unit.getId(), boardId, sectorNumber);
        return unit;
    }

    @Transactional(readOnly = true)
    public List<UnitDto> getReserveUnitsDto(Long userId) {
        Player player = requirePlayerByUserId(userId);
        return unitRepository.findByPlayerIdAndSectorIsNull(player.getId()).stream()
                .map(unitMapper::toDto)
                .toList();
    }

    private Map<String, Integer> purchasedByType(Long playerId, int turn) {
        Map<String, Integer> counts = new HashMap<>();
        for (PlayerAction action : actionRepository.findByPlayerIdAndTurnAndTypeAndStatus(
                playerId, turn, PlayerActionType.BUY_UNIT, PlayerActionStatus.ACTIVE)) {
            if (action.getUnitType() != null) {
                counts.merge(action.getUnitType(), 1, Integer::sum);
            }
        }
        return counts;
    }

    /** Même fermeture que la récolte : un achat pendant la résolution échapperait au placement au QG. */
    private void requireTurnOpen() {
        if (turnLock.isLocked()) {
            throw new IllegalStateException(
                    "La résolution du tour est en cours — réessayez après la fin du tour");
        }
    }

    private UnitType parseUnitType(BuyUnitRequestDto item) {
        if (item == null || item.getUnitType() == null || item.getUnitType().isBlank()) {
            throw new IllegalArgumentException("Le type d'unité est requis");
        }
        try {
            return UnitType.valueOf(item.getUnitType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type d'unité invalide : " + item.getUnitType());
        }
    }

    private UnitClass parseUnitClass(String unitClass) {
        if (unitClass == null || unitClass.isBlank()) {
            throw new IllegalArgumentException("La classe est requise");
        }
        try {
            return UnitClass.valueOf(unitClass);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Classe invalide : " + unitClass);
        }
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

    public List<UnitDto> buyUnitsDto(Long userId, List<BuyUnitRequestDto> items) {
        return buyUnits(userId, items).stream().map(unitMapper::toDto).toList();
    }

    public UnitDto placeUnitDto(Long userId, Long unitId, Long boardId, int sectorNumber) {
        return unitMapper.toDto(placeUnit(userId, unitId, boardId, sectorNumber));
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