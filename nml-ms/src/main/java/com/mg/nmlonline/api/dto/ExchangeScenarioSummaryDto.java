package com.mg.nmlonline.api.dto;

public record ExchangeScenarioSummaryDto(
        int turn,
        Long pendingOfferId,
        Long acceptedOfferId,
        String message) {
}
