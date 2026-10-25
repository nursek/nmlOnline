package com.mg.nmlonline.api.dto;

public record PlayerStatusDto(
        boolean eliminated,
        String eliminationCause,
        Integer eliminatedTurn,
        Long eliminatedByPlayerId) {
}
