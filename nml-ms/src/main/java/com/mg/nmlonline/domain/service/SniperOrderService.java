package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.SniperOrderDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sniper.SniperOrder;
import com.mg.nmlonline.domain.model.sniper.SniperOrderAction;
import com.mg.nmlonline.domain.model.sniper.SniperOrderStatus;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.SniperOrderRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SniperOrderService {

    private final PlayerService playerService;
    private final BoardService boardService;
    private final UnitRepository unitRepository;
    private final SniperOrderRepository sniperOrderRepository;
    private final MovementOrderRepository movementOrderRepository;
    private final VehicleRepository vehicleRepository;
    private final TurnService turnService;
    private final TurnLock turnLock;
    private final AllianceGraph allianceGraph;

    public SniperOrderService(PlayerService playerService,
                              BoardService boardService,
                              UnitRepository unitRepository,
                              SniperOrderRepository sniperOrderRepository,
                              MovementOrderRepository movementOrderRepository,
                              VehicleRepository vehicleRepository,
                              TurnService turnService,
                              TurnLock turnLock,
                              AllianceGraph allianceGraph) {
        this.playerService = playerService;
        this.boardService = boardService;
        this.unitRepository = unitRepository;
        this.sniperOrderRepository = sniperOrderRepository;
        this.movementOrderRepository = movementOrderRepository;
        this.vehicleRepository = vehicleRepository;
        this.turnService = turnService;
        this.turnLock = turnLock;
        this.allianceGraph = allianceGraph;
    }

    public SniperOrderDto place(Long userId, Long unitId, int targetSectorNumber) {
        if (turnLock.isLocked()) {
            throw new IllegalStateException(
                    "La résolution du tour est en cours — réessayez après la fin du tour");
        }
        Player player = requirePlayerForUpdate(userId);
        Board board = requireBoard();
        Unit unit = requireOwnedSniper(board, unitId, player);
        Sector from = unit.getSector();
        if (targetSectorNumber == from.getNumber()) {
            throw new IllegalArgumentException(
                    "Le secteur cible doit être différent du secteur de l'unité.");
        }
        Sector target = board.getSector(targetSectorNumber);
        if (target == null) {
            throw new EntityNotFoundException("Secteur cible introuvable : " + targetSectorNumber);
        }
        if (!from.isNeighbor(targetSectorNumber)) {
            throw new IllegalArgumentException(
                    "Le secteur " + targetSectorNumber + " n'est pas adjacent au secteur " + from.getNumber() + ".");
        }
        int turn = turnService.getCurrentTurn();
        if (!movementOrderRepository.findPendingEntityIds(turn, List.of(unitId)).isEmpty()) {
            throw new IllegalArgumentException("Cette unité a déjà un ordre de déplacement en attente.");
        }
        if (sniperOrderRepository.existsByUnitIdAndTurnAndStatus(
                unitId, turn, SniperOrderStatus.PENDING)) {
            throw new IllegalArgumentException(
                    "Cette unité a déjà un ordre sniper en attente pour ce tour.");
        }
        SniperOrder order = sniperOrderRepository.save(SniperOrder.create(
                player.getId(), turn, unitId, from.getNumber(), targetSectorNumber));
        return toDto(order, player, target);
    }

    @Transactional(readOnly = true)
    public List<SniperOrderDto> currentPending(Long userId) {
        Player player = requirePlayer(userId);
        Board board = requireBoard();
        int turn = turnService.getCurrentTurn();
        return sniperOrderRepository
                .findByPlayerIdAndTurnAndStatus(player.getId(), turn, SniperOrderStatus.PENDING)
                .stream()
                .map(order -> toDto(order, player, board.getSector(order.getTargetSectorNumber())))
                .toList();
    }

    public void cancel(Long userId, Long orderId) {
        Player player = requirePlayer(userId);
        SniperOrder order = sniperOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ordre sniper #" + orderId + " introuvable."));
        if (!order.getPlayerId().equals(player.getId())) {
            throw new SecurityException(
                    "L'ordre sniper #" + orderId + " n'appartient pas au joueur " + player.getId() + ".");
        }
        if (!order.isPending()) {
            throw new IllegalStateException(
                    "L'ordre sniper #" + orderId + " n'est plus PENDING (statut=" + order.getStatus() + ").");
        }
        order.cancel();
        sniperOrderRepository.save(order);
    }

    private SniperOrderDto toDto(SniperOrder order, Player player, Sector targetSector) {
        SniperOrderDto dto = new SniperOrderDto();
        dto.setId(order.getId());
        dto.setTurn(order.getTurn());
        dto.setPlayerId(order.getPlayerId());
        dto.setUnitId(order.getUnitId());
        dto.setFromSectorNumber(order.getFromSectorNumber());
        dto.setTargetSectorNumber(order.getTargetSectorNumber());
        dto.setStatus(order.getStatus().name());
        dto.setStatusMessage(order.getStatusMessage());
        dto.setAction(actionFor(player, targetSector).name());
        return dto;
    }

    private SniperOrderAction actionFor(Player player, Sector targetSector) {
        if (targetSector == null || targetSector.getOwnerId() == null) {
            return SniperOrderAction.TIR_PRECISION;
        }
        if (targetSector.getOwnerId().equals(player.getId())
                || allianceGraph.areAllied(player.getId(), targetSector.getOwnerId())) {
            return SniperOrderAction.SURVEILLANCE;
        }
        return SniperOrderAction.TIR_PRECISION;
    }

    private Player requirePlayer(Long userId) {
        Player player = playerService.findByUserId(userId);
        if (player == null) {
            throw new EntityNotFoundException("Joueur introuvable pour l'utilisateur " + userId);
        }
        return player;
    }

    private Player requirePlayerForUpdate(Long userId) {
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

    private Unit requireOwnedSniper(Board board, Long unitId, Player player) {
        if (unitId == null) {
            throw new IllegalArgumentException("L'ID de l'unité est requis.");
        }
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new EntityNotFoundException("Unité introuvable avec l'ID " + unitId));
        if (unit.getSector() == null || unit.getSector().getBoard() == null
                || !unit.getSector().getBoard().getId().equals(board.getId())) {
            throw new EntityNotFoundException("Unité introuvable avec l'ID " + unitId);
        }
        if (!player.getId().equals(unit.getPlayerId())) {
            throw new SecurityException("Cette unité n'appartient pas au joueur authentifié.");
        }
        if (!unit.isSniper()) {
            throw new IllegalArgumentException("L'unité #" + unitId + " n'est pas de classe SNIPER.");
        }
        if (vehicleRepository.existsByPilot_Id(unitId)
                || vehicleRepository.existsByPassengers_Id(unitId)) {
            throw new IllegalArgumentException(
                    "Un sniper à bord d'un véhicule ne peut pas recevoir d'ordre sniper.");
        }
        return unit;
    }
}
