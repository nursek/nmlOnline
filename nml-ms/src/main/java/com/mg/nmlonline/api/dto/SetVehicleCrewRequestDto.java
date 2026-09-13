package com.mg.nmlonline.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class SetVehicleCrewRequestDto {
    private Long pilotId;
    private List<Long> passengerIds = List.of();
}
