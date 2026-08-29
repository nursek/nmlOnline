package com.mg.nmlonline.mapper;

import com.mg.nmlonline.api.dto.AdminMovementOrderDto;
import com.mg.nmlonline.api.dto.DestinationConflictDto;
import com.mg.nmlonline.api.dto.MovementOrderDto;
import com.mg.nmlonline.api.dto.MovementResolutionResultDto;
import com.mg.nmlonline.api.dto.TransitCombatResultDto;
import com.mg.nmlonline.domain.model.movement.DestinationConflict;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.TransitCombatResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class MovementMapper {

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

    /** playerName résolu en lot par l'appelant (anti N+1). */
    public AdminMovementOrderDto toAdminDto(MovementOrder order, String playerName) {
        if (order == null) return null;

        AdminMovementOrderDto dto = new AdminMovementOrderDto();
        dto.setId(order.getId());
        dto.setTurn(order.getTurn());
        dto.setPlayerId(order.getPlayerId());
        dto.setPlayerName(playerName);
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        dto.setFromSectorNumber(order.getFromSectorNumber());
        dto.setToSectorNumber(order.getToSectorNumber());
        dto.setRoute(order.getRoute() != null ? new ArrayList<>(order.getRoute()) : null);
        dto.setEntityIds(order.getEntityIds() != null ? new ArrayList<>(order.getEntityIds()) : null);
        dto.setVehicleId(order.getVehicleId());
        dto.setStatusMessage(order.getStatusMessage());
        return dto;
    }

    /** namesById résout playerId → nom (lookup en lot par l'appelant, anti N+1). */
    public MovementResolutionResultDto toResolutionDto(MovementResolutionResult result,
                                                       int turn,
                                                       Function<Long, String> namesById) {
        MovementResolutionResultDto dto = new MovementResolutionResultDto();
        dto.setTurn(turn);
        dto.setResolved(toAdminDtoList(result.getResolved(), namesById));
        dto.setBlocked(toAdminDtoList(result.getBlocked(), namesById));
        dto.setConflicts(toConflictDtoList(result.getConflicts(), namesById));
        dto.setTransitCombats(toTransitDtoList(result.getTransitCombats()));
        dto.setHasConflicts(result.hasConflicts());
        dto.setHasTransitCombats(result.hasTransitCombats());
        return dto;
    }

    private List<AdminMovementOrderDto> toAdminDtoList(List<MovementOrder> orders,
                                                       Function<Long, String> namesById) {
        List<AdminMovementOrderDto> list = new ArrayList<>(orders.size());
        for (MovementOrder order : orders) {
            list.add(toAdminDto(order, namesById.apply(order.getPlayerId())));
        }
        return list;
    }

    private List<DestinationConflictDto> toConflictDtoList(List<DestinationConflict> conflicts,
                                                           Function<Long, String> namesById) {
        List<DestinationConflictDto> list = new ArrayList<>(conflicts.size());
        for (DestinationConflict c : conflicts) {
            DestinationConflictDto dto = new DestinationConflictDto();
            dto.setSectorNumber(c.sectorNumber());
            dto.setAttackerPlayerId(c.attackerPlayerId());
            dto.setAttackerName(namesById.apply(c.attackerPlayerId()));
            dto.setDefenderPlayerId(c.defenderPlayerId());
            dto.setDefenderName(namesById.apply(c.defenderPlayerId()));
            list.add(dto);
        }
        return list;
    }

    private List<TransitCombatResultDto> toTransitDtoList(List<TransitCombatResult> combats) {
        List<TransitCombatResultDto> list = new ArrayList<>(combats.size());
        for (TransitCombatResult t : combats) {
            TransitCombatResultDto dto = new TransitCombatResultDto();
            dto.setSectorNumber(t.sectorNumber());
            dto.setVehicleId(t.vehicleId());
            dto.setVehicleFired(t.vehicleFired());
            list.add(dto);
        }
        return list;
    }
}