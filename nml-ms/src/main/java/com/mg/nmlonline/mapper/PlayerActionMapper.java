package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.PlayerActionDto;
import com.mg.nmlonline.domain.model.action.PlayerAction;
import org.springframework.stereotype.Component;

@Component
public class PlayerActionMapper {

    public PlayerActionDto toDto(PlayerAction action) {
        if (action == null) return null;

        PlayerActionDto dto = new PlayerActionDto();
        dto.setId(action.getId());
        dto.setTurn(action.getTurn());
        dto.setType(action.getType());
        dto.setStatus(action.getStatus());
        dto.setMoney(action.getMoney());
        dto.setQuantity(action.getQuantity());
        dto.setEquipmentName(action.getEquipmentName());
        dto.setResourceName(action.getResourceName());
        dto.setUnitId(action.getUnitId());
        dto.setVehicleId(action.getVehicleId());
        dto.setBuildingId(action.getBuildingId());
        dto.setFromSectorNumber(action.getFromSectorNumber());
        dto.setToSectorNumber(action.getToSectorNumber());
        dto.setLabel(label(action));
        return dto;
    }

    private String label(PlayerAction action) {
        return switch (action.getType()) {
            case BUY_EQUIPMENT -> "Achat de " + action.getQuantity() + " × " + action.getEquipmentName();
            case SELL_RESOURCE -> "Vente de " + action.getQuantity() + " × " + action.getResourceName();
            case EQUIP_UNIT -> "Équipement « " + action.getEquipmentName() + " » sur l'unité #" + action.getUnitId();
            case UNEQUIP_UNIT -> "Retrait de « " + action.getEquipmentName() + " » de l'unité #" + action.getUnitId();
            case BUY_VEHICLE -> "Achat d'un véhicule";
            case PLACE_VEHICLE -> "Placement d'un véhicule sur le secteur " + action.getToSectorNumber();
            case MOVE_BUILDING -> "Déplacement vers le secteur " + action.getToSectorNumber();
            case SET_VEHICLE_CREW -> "Équipage du véhicule #" + action.getVehicleId() + " modifié";
        };
    }
}
