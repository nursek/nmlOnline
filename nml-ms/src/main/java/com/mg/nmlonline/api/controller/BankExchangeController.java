package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.CreateExchangeOfferRequestDto;
import com.mg.nmlonline.api.dto.ExchangeOfferDto;
import com.mg.nmlonline.domain.service.ExchangeOfferService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bank/offers")
public class BankExchangeController {

    private final ExchangeOfferService exchangeOfferService;

    public BankExchangeController(ExchangeOfferService exchangeOfferService) {
        this.exchangeOfferService = exchangeOfferService;
    }

    private Long getAuthenticatedUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @GetMapping
    public ResponseEntity<List<ExchangeOfferDto>> getOffers(HttpServletRequest request) {
        Long userId = getAuthenticatedUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(exchangeOfferService.getOffers(userId));
    }

    @PostMapping
    public ResponseEntity<ExchangeOfferDto> createOffer(
            @RequestBody CreateExchangeOfferRequestDto request,
            HttpServletRequest httpRequest) {
        Long userId = getAuthenticatedUserId(httpRequest);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(exchangeOfferService.createOffer(userId, request));
    }

    @PostMapping("/{offerId}/accept")
    public ResponseEntity<ExchangeOfferDto> acceptOffer(@PathVariable Long offerId,
                                                        HttpServletRequest request) {
        Long userId = getAuthenticatedUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(exchangeOfferService.acceptOffer(userId, offerId));
    }

    @PostMapping("/{offerId}/decline")
    public ResponseEntity<ExchangeOfferDto> declineOffer(@PathVariable Long offerId,
                                                         HttpServletRequest request) {
        Long userId = getAuthenticatedUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(exchangeOfferService.declineOffer(userId, offerId));
    }

    @PostMapping("/{offerId}/cancel")
    public ResponseEntity<ExchangeOfferDto> cancelOffer(@PathVariable Long offerId,
                                                        HttpServletRequest request) {
        Long userId = getAuthenticatedUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(exchangeOfferService.cancelOffer(userId, offerId));
    }
}
