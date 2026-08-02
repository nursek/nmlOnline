package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.MovementOrderDto;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Mapper pour {@link MovementOrder} ⇔ {@link MovementOrderDto}.
 * Symétrique avec les autres mappers du package (EquipmentMapper, UnitMapper…)
 * afin que la conversion DTO ne vive pas dans le contrôleur.
 */
@Component
public class MovementMapper {

    /** Convertit un {@link MovementOrder} du domaine en DTO de sortie API. */
    public MovementOrderDto toDto(MovementOrder order) {
        if (order == null) return null;

        MovementOrderDto dto = new MovementOrderDto();
        dto.setId(order.getId());
        dto.setTurn(order.getTurn());
        dto.setPlayerId(order.getPlayerId());
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        dto.setFromSectorNumber(order.getFromSectorNumber());
        dto.setToSectorNumber(order.getToSectorNumber());
        dto.setRoute(order.getRoute() != null ? new ArrayList<>(order.getRoute()) : null);
        dto.setEntityIds(order.getEntityIds() != null ? new ArrayList<>(order.getEntityIds()) : null);
        dto.setVehicleId(order.getVehicleId());
        dto.setStatusMessage(order.getStatusMessage());
        return dto;
    }
}