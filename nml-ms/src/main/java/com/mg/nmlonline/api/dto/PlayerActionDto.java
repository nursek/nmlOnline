package com.mg.nmlonline.api.dto;

import com.mg.nmlonline.domain.model.action.PlayerActionStatus;
import com.mg.nmlonline.domain.model.action.PlayerActionType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlayerActionDto {
    private Long id;
    private int turn;
    private PlayerActionType type;
    private PlayerActionStatus status;
    private String label;
    private Double money;
    private Integer quantity;
    private String equipmentName;
    private String resourceName;
    private Long unitId;
    private Long vehicleId;
    private Long buildingId;
    private Integer fromSectorNumber;
    private Integer toSectorNumber;
}
