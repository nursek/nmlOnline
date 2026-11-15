package com.mg.nmlonline.api.dto;

public record PlayerStatusDto(
        boolean eliminated,
        String eliminationCause,
        Integer eliminatedTurn,
        Long eliminatedByPlayerId,
        boolean participant,
        boolean gameFinished,
        Integer finalTurn,
        Long winnerPlayerId,
        String winnerName,
        boolean winnerPosthumous,
        boolean winnerAdjusted,
        boolean winner) {
}
