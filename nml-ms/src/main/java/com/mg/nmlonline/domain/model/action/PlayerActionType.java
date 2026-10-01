package com.mg.nmlonline.domain.model.action;

public enum PlayerActionType {
    BUY_EQUIPMENT,
    SELL_RESOURCE,
    EQUIP_UNIT,
    UNEQUIP_UNIT,
    BUY_VEHICLE,
    PLACE_VEHICLE,
    BUY_UNIT,
    PLACE_UNIT,
    MOVE_BUILDING,
    SET_VEHICLE_CREW,
    HARVEST_MONEY,
    HARVEST_RESOURCE;

    public boolean isHarvest() {
        return this == HARVEST_MONEY || this == HARVEST_RESOURCE;
    }
}
