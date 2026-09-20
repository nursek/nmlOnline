package com.mg.nmlonline.api.dto;

import com.mg.nmlonline.domain.model.action.PlayerActionType;

import java.util.List;

public record HarvestRequestDto(PlayerActionType choice, List<Integer> sectorNumbers) {
}
