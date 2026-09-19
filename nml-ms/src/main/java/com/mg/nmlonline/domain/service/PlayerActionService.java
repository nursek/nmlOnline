package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.PlayerActionDto;
import com.mg.nmlonline.domain.exception.PlayerActionUndoException;
import com.mg.nmlonline.domain.model.action.PlayerAction;
import com.mg.nmlonline.domain.model.action.PlayerActionStatus;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitEquipment;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerActionRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import com.mg.nmlonline.mapper.PlayerActionMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// LIFO par suffixe : pré-requis d'annulation (ex. déséquiper) toujours satisfaits.
@Service
@Transactional
public class PlayerActionService {

    private final PlayerActionRepository actionRepository;
    private final PlayerRepository playerRepository;
    private final UnitRepository unitRepository;
    private final VehicleRepository vehicleRepository;
    private final BuildingRepository buildingRepository;
    private final SectorRepository sectorRepository;
    private final MovementOrderRepository movementOrderRepository;
    private final VehicleCrewService vehicleCrewService;
    private final TurnService turnService;
    private final PlayerActionMapper actionMapper;
    private final EntityManager em;

    public PlayerActionService(PlayerActionRepository actionRepository,
                               PlayerRepository playerRepository,
                               UnitRepository unitRepository,
                               VehicleRepository vehicleRepository,
                               BuildingRepository buildingRepository,
                               SectorRepository sectorRepository,
                               MovementOrderRepository movementOrderRepository,
                               VehicleCrewService vehicleCrewService,
                               TurnService turnService,
                               PlayerActionMapper actionMapper,
                               EntityManager em) {
        this.actionRepository = actionRepository;
        this.playerRepository = playerRepository;
        this.unitRepository = unitRepository;
        this.vehicleRepository = vehicleRepository;
        this.buildingRepository = buildingRepository;
        this.sectorRepository = sectorRepository;
        this.movementOrderRepository = movementOrderRepository;
        this.vehicleCrewService = vehicleCrewService;
        this.turnService = turnService;
        this.actionMapper = actionMapper;
        this.em = em;
    }

    public void recordBuyEquipment(Long playerId, String equipmentName, int quantity, double cost) {
        save(PlayerAction.buyEquipment(playerId, turnService.getCurrentTurn(), equipmentName, quantity, cost));
    }

    public void recordSellResource(Long playerId, String resourceName, int quantity, double value) {
        save(PlayerAction.sellResource(playerId, turnService.getCurrentTurn(), resourceName, quantity, value));
    }

    public void recordEquipUnit(Long playerId, Long unitId, String equipmentName) {
        save(PlayerAction.equipUnit(playerId, turnService.getCurrentTurn(), unitId, equipmentName));
    }

    public void recordUnequipUnit(Long playerId, Long unitId, String equipmentName) {
        save(PlayerAction.unequipUnit(playerId, turnService.getCurrentTurn(), unitId, equipmentName));
    }

    public void recordBuyVehicle(Long playerId, Long vehicleId, double cost) {
        save(PlayerAction.buyVehicle(playerId, turnService.getCurrentTurn(), vehicleId, cost));
    }

    public void recordPlaceVehicle(Long playerId, Long vehicleId, Long boardId, int sectorNumber) {
        save(PlayerAction.placeVehicle(playerId, turnService.getCurrentTurn(), vehicleId, boardId, sectorNumber));
    }

    public void recordMoveBuilding(Long playerId, Long buildingId, Long boardId, int fromSectorNumber,
                                   int toSectorNumber, Integer prevLastMovedTurn, Boolean prevHasMoved) {
        save(PlayerAction.moveBuilding(playerId, turnService.getCurrentTurn(), buildingId, boardId,
                fromSectorNumber, toSectorNumber, prevLastMovedTurn, prevHasMoved));
    }

    public void recordSetVehicleCrew(Long playerId, Long vehicleId, Long prevPilotId, String prevPassengerIds) {
        save(PlayerAction.setVehicleCrew(playerId, turnService.getCurrentTurn(), vehicleId,
                prevPilotId, prevPassengerIds));
    }

    @Transactional(readOnly = true)
    public List<PlayerActionDto> getCurrentTurnActions(Long userId) {
        Player player = requirePlayerByUserId(userId);
        return mapActive(player.getId(), turnService.getCurrentTurn());
    }

    public void deleteForPlayer(Long playerId) {
        actionRepository.deleteByPlayerId(playerId);
    }

    public List<PlayerActionDto> undoFrom(Long userId, Long actionId) {
        // Verrou d'abord : un FOR UPDATE ne rafraîchit pas une entité déjà managée.
        Player locked = lockPlayerByUserId(userId);
        int turn = turnService.getCurrentTurn();

        PlayerAction target = actionRepository.findByIdAndPlayerId(actionId, locked.getId())
                .orElseThrow(() -> new EntityNotFoundException("Action introuvable : " + actionId));
        if (target.getTurn() != turn) {
            throw new PlayerActionUndoException("Cette action appartient à un tour terminé.");
        }

        List<PlayerAction> toUndo = new ArrayList<>(actionRepository
                .findByPlayerIdAndTurnAndStatusAndIdGreaterThanEqualOrderByIdAsc(
                        locked.getId(), turn, PlayerActionStatus.ACTIVE, actionId));
        toUndo.sort((a, b) -> b.getId().compareTo(a.getId()));

        for (PlayerAction action : toUndo) {
            applyUndo(action, locked);
            action.markUndone();
            actionRepository.save(action);
        }

        playerRepository.save(locked);
        return mapActive(locked.getId(), turn);
    }

    public List<PlayerActionDto> undoAll(Long userId) {
        Player locked = lockPlayerByUserId(userId);
        int turn = turnService.getCurrentTurn();
        List<PlayerAction> active = actionRepository
                .findByPlayerIdAndTurnAndStatusOrderByIdAsc(locked.getId(), turn, PlayerActionStatus.ACTIVE);
        if (active.isEmpty()) {
            return List.of();
        }
        return undoFrom(userId, active.getFirst().getId());
    }

    private void applyUndo(PlayerAction action, Player player) {
        switch (action.getType()) {
            case BUY_EQUIPMENT -> undoBuyEquipment(action, player);
            case SELL_RESOURCE -> undoSellResource(action, player);
            case EQUIP_UNIT -> undoEquipUnit(action, player);
            case UNEQUIP_UNIT -> undoUnequipUnit(action, player);
            case BUY_VEHICLE -> undoBuyVehicle(action, player);
            case PLACE_VEHICLE -> undoPlaceVehicle(action);
            case MOVE_BUILDING -> undoMoveBuilding(action);
            case SET_VEHICLE_CREW -> undoSetVehicleCrew(action);
        }
    }

    private void undoBuyEquipment(PlayerAction action, Player player) {
        EquipmentStack stack = findStack(player, action.getEquipmentName());
        int quantity = action.getQuantity() != null ? action.getQuantity() : 1;
        if (stack == null || stack.getAvailable() < quantity) {
            throw new PlayerActionUndoException(
                    "« " + action.getEquipmentName() + " » est équipé sur une unité : déséquipez-le d'abord.");
        }
        for (int i = 0; i < quantity; i++) {
            player.removeEquipmentFromStack(stack.getEquipment());
        }
        player.refundMoney(action.getMoney() != null ? action.getMoney() : 0);
        player.setTotalEquipmentValue();
        player.calculateTotalEconomyPower();
    }

    private void undoSellResource(PlayerAction action, Player player) {
        double value = action.getMoney() != null ? action.getMoney() : 0;
        if (player.getStats().getMoney() < value) {
            throw new PlayerActionUndoException(
                    "Fonds insuffisants pour annuler la vente de « " + action.getResourceName() + " ».");
        }
        player.addResource(action.getResourceName(), action.getQuantity() != null ? action.getQuantity() : 0);
        player.decrementMoney(value);
    }

    private void undoEquipUnit(PlayerAction action, Player player) {
        Unit unit = requireUnit(action.getUnitId(), player);
        EquipmentStack stack = findStack(player, action.getEquipmentName());
        if (stack == null) {
            throw new PlayerActionUndoException("Équipement « " + action.getEquipmentName() + " » introuvable.");
        }
        // Sans orphanRemoval : em.remove d'une occurrence (cf. UnitService.removeEquipment).
        UnitEquipment row = unit.removeOneEquipment(action.getEquipmentName());
        if (row == null) {
            throw new PlayerActionUndoException("L'unité ne porte pas « " + action.getEquipmentName() + " ».");
        }
        em.remove(row);
        player.incrementEquipmentAvailability(stack.getEquipment());
        unitRepository.save(unit);
    }

    private void undoUnequipUnit(PlayerAction action, Player player) {
        Unit unit = requireUnit(action.getUnitId(), player);
        EquipmentStack stack = findStack(player, action.getEquipmentName());
        if (stack == null) {
            throw new PlayerActionUndoException("Équipement « " + action.getEquipmentName() + " » introuvable.");
        }
        Equipment equipment = stack.getEquipment();
        if (!unit.addEquipment(equipment)) {
            throw new PlayerActionUndoException(
                    "L'unité ne peut plus équiper « " + action.getEquipmentName() + " ».");
        }
        player.decrementEquipmentAvailability(equipment);
        unitRepository.save(unit);
    }

    private void undoBuyVehicle(PlayerAction action, Player player) {
        Vehicle vehicle = vehicleRepository.findById(action.getVehicleId())
                .orElseThrow(() -> new PlayerActionUndoException("Véhicule introuvable."));
        if (vehicle.getSector() != null) {
            throw new PlayerActionUndoException("Le véhicule est déployé : retirez-le avant d'annuler l'achat.");
        }
        if (vehicle.hasPilot() || vehicle.getPassengerCount() > 0) {
            throw new PlayerActionUndoException("Le véhicule transporte une unité : videz-le d'abord.");
        }
        double cost = action.getMoney() != null ? action.getMoney() : 0;
        em.remove(vehicle);
        player.refundMoney(cost);
        player.getStats().setTotalVehiclesValue(player.getStats().getTotalVehiclesValue() - cost);
        player.calculateTotalEconomyPower();
    }

    private void undoPlaceVehicle(PlayerAction action) {
        Vehicle vehicle = vehicleRepository.findById(action.getVehicleId())
                .orElseThrow(() -> new PlayerActionUndoException("Véhicule introuvable."));
        vehicleCrewService.applyCrew(vehicle, null, List.of());
        vehicle.setSector(null);
        vehicleRepository.save(vehicle);
    }

    private void undoMoveBuilding(PlayerAction action) {
        Building building = buildingRepository.findById(action.getBuildingId())
                .orElseThrow(() -> new PlayerActionUndoException("Bâtiment introuvable."));
        if (action.getBoardId() == null || action.getFromSectorNumber() == null) {
            throw new PlayerActionUndoException("Secteur d'origine inconnu.");
        }
        Sector from = sectorRepository.findByBoard_IdAndNumber(action.getBoardId(), action.getFromSectorNumber())
                .orElseThrow(() -> new PlayerActionUndoException("Secteur d'origine introuvable."));
        building.setSector(from);
        building.setLastMovedTurn(action.getPrevLastMovedTurn());
        if (building instanceof Bank bank) {
            bank.setHasMoved(Boolean.TRUE.equals(action.getPrevHasMoved()));
        }
        buildingRepository.save(building);
    }

    private void undoSetVehicleCrew(PlayerAction action) {
        Vehicle vehicle = vehicleRepository.findById(action.getVehicleId())
                .orElseThrow(() -> new PlayerActionUndoException("Véhicule introuvable."));
        int turn = turnService.getCurrentTurn();
        if (!movementOrderRepository
                .findByVehicleIdAndTurnAndStatus(vehicle.getId(), turn, MovementStatus.PENDING).isEmpty()) {
            throw new PlayerActionUndoException(
                    "Le véhicule a un ordre de mouvement en attente : annulez-le d'abord.");
        }
        List<Long> restoredIds = new ArrayList<>(parseIds(action.getPrevPassengerIds()));
        if (action.getPrevPilotId() != null) {
            restoredIds.add(action.getPrevPilotId());
        }
        if (!restoredIds.isEmpty()) {
            List<Long> engaged = movementOrderRepository.findPendingEntityIds(turn, restoredIds);
            if (!engaged.isEmpty()) {
                throw new PlayerActionUndoException(
                        "Des occupants ont un ordre à pied en attente : annulez-le d'abord.");
            }
        }
        vehicleCrewService.applyCrew(vehicle, action.getPrevPilotId(), parseIds(action.getPrevPassengerIds()));
        vehicleRepository.save(vehicle);
    }

    private List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).map(Long::valueOf).toList();
    }

    private Unit requireUnit(Long unitId, Player player) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new PlayerActionUndoException("Unité introuvable : " + unitId));
        if (!player.getId().equals(unit.getPlayerId())) {
            throw new SecurityException("Cette unité n'appartient pas au joueur authentifié.");
        }
        return unit;
    }

    private EquipmentStack findStack(Player player, String equipmentName) {
        return player.getEquipments().stream()
                .filter(s -> s.getEquipment() != null && s.getEquipment().getName().equals(equipmentName))
                .findFirst()
                .orElse(null);
    }

    private Player requirePlayerByUserId(Long userId) {
        Player player = playerRepository.findByUserId(userId).orElse(null);
        if (player == null) {
            throw new EntityNotFoundException("Joueur introuvable pour l'utilisateur " + userId);
        }
        return player;
    }

    private Player lockPlayerByUserId(Long userId) {
        return playerRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new EntityNotFoundException("Joueur introuvable pour l'utilisateur " + userId));
    }

    private void save(PlayerAction action) {
        actionRepository.save(action);
    }

    private List<PlayerActionDto> mapActive(Long playerId, int turn) {
        return actionRepository
                .findByPlayerIdAndTurnAndStatusOrderByIdAsc(playerId, turn, PlayerActionStatus.ACTIVE)
                .stream()
                .map(actionMapper::toDto)
                .toList();
    }
}
