package com.mg.nmlonline.api.dto;

import com.mg.nmlonline.domain.model.bank.ExchangeOfferStatus;

import java.time.Instant;
import java.util.List;

public record ExchangeOfferDto(
        Long id,
        Long senderPlayerId,
        String senderName,
        Long receiverPlayerId,
        String receiverName,
        double money,
        List<ExchangeOfferItemDto> resources,
        ExchangeOfferStatus status,
        int createdTurn,
        int expiresTurn,
        Integer resolvedTurn,
        Instant createdAt) {
}
