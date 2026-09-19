package com.mg.nmlonline.api.dto;

import java.util.List;

public record CreateExchangeOfferRequestDto(Long receiverPlayerId, double money,
                                            List<ExchangeOfferItemDto> resources) {
}
