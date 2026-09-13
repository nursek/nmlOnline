package com.mg.nmlonline.domain.model.action;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Payload plat : chaque type ne remplit que ce dont son annulation a besoin.
@Entity
@Table(name = "PLAYER_ACTIONS")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlayerAction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_action_seq")
    @SequenceGenerator(name = "player_action_seq", sequenceName = "player_actions_id_seq", allocationSize = 50)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private int turn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerActionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerActionStatus status = PlayerActionStatus.ACTIVE;

    @Column(name = "equipment_name")
    private String equipmentName;

    @Column(name = "resource_name")
    private String resourceName;

    private Integer quantity;

    private Double money;

    @Column(name = "unit_id")
    private Long unitId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "building_id")
    private Long buildingId;

    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "from_sector_number")
    private Integer fromSectorNumber;

    @Column(name = "to_sector_number")
    private Integer toSectorNumber;

    @Column(name = "prev_last_moved_turn")
    private Integer prevLastMovedTurn;

    @Column(name = "prev_has_moved")
    private Boolean prevHasMoved;

    @Column(name = "prev_pilot_id")
    private Long prevPilotId;

    // CSV d'IDs de passagers : le payload plat ne peut pas porter une collection.
    @Column(name = "prev_passenger_ids", length = 2000)
    private String prevPassengerIds;

    public static PlayerAction buyEquipment(Long playerId, int turn, String equipmentName, int quantity, double money) {
        PlayerAction action = base(playerId, turn, PlayerActionType.BUY_EQUIPMENT);
        action.setEquipmentName(equipmentName);
        action.setQuantity(quantity);
        action.setMoney(money);
        return action;
    }

    public static PlayerAction sellResource(Long playerId, int turn, String resourceName, int quantity, double money) {
        PlayerAction action = base(playerId, turn, PlayerActionType.SELL_RESOURCE);
        action.setResourceName(resourceName);
        action.setQuantity(quantity);
        action.setMoney(money);
        return action;
    }

    public static PlayerAction equipUnit(Long playerId, int turn, Long unitId, String equipmentName) {
        PlayerAction action = base(playerId, turn, PlayerActionType.EQUIP_UNIT);
        action.setUnitId(unitId);
        action.setEquipmentName(equipmentName);
        return action;
    }

    public static PlayerAction unequipUnit(Long playerId, int turn, Long unitId, String equipmentName) {
        PlayerAction action = base(playerId, turn, PlayerActionType.UNEQUIP_UNIT);
        action.setUnitId(unitId);
        action.setEquipmentName(equipmentName);
        return action;
    }

    public static PlayerAction buyVehicle(Long playerId, int turn, Long vehicleId, double money) {
        PlayerAction action = base(playerId, turn, PlayerActionType.BUY_VEHICLE);
        action.setVehicleId(vehicleId);
        action.setMoney(money);
        return action;
    }

    public static PlayerAction placeVehicle(Long playerId, int turn, Long vehicleId, Long boardId, int sectorNumber) {
        PlayerAction action = base(playerId, turn, PlayerActionType.PLACE_VEHICLE);
        action.setVehicleId(vehicleId);
        action.setBoardId(boardId);
        action.setToSectorNumber(sectorNumber);
        return action;
    }

    public static PlayerAction moveBuilding(Long playerId, int turn, Long buildingId, Long boardId,
                                            int fromSectorNumber, int toSectorNumber,
                                            Integer prevLastMovedTurn, Boolean prevHasMoved) {
        PlayerAction action = base(playerId, turn, PlayerActionType.MOVE_BUILDING);
        action.setBuildingId(buildingId);
        action.setBoardId(boardId);
        action.setFromSectorNumber(fromSectorNumber);
        action.setToSectorNumber(toSectorNumber);
        action.setPrevLastMovedTurn(prevLastMovedTurn);
        action.setPrevHasMoved(prevHasMoved);
        return action;
    }

    public static PlayerAction setVehicleCrew(Long playerId, int turn, Long vehicleId,
                                               Long prevPilotId, String prevPassengerIds) {
        PlayerAction action = base(playerId, turn, PlayerActionType.SET_VEHICLE_CREW);
        action.setVehicleId(vehicleId);
        action.setPrevPilotId(prevPilotId);
        action.setPrevPassengerIds(prevPassengerIds);
        return action;
    }

    private static PlayerAction base(Long playerId, int turn, PlayerActionType type) {
        PlayerAction action = new PlayerAction();
        action.setPlayerId(playerId);
        action.setTurn(turn);
        action.setType(type);
        action.setStatus(PlayerActionStatus.ACTIVE);
        return action;
    }

    public void markUndone() {
        this.status = PlayerActionStatus.UNDONE;
    }
}
