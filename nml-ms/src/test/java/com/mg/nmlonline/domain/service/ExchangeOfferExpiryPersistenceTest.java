package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.bank.ExchangeOffer;
import com.mg.nmlonline.domain.model.bank.ExchangeOfferStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.ExchangeOfferRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EmbeddedPostgresTest
@DisplayName("ExchangeOfferService — l'expiration paresseuse est persistée malgré le refus")
class ExchangeOfferExpiryPersistenceTest {

    @Autowired
    ExchangeOfferService exchangeOfferService;

    @Autowired
    ExchangeOfferRepository exchangeOfferRepository;

    @Autowired
    PlayerRepository playerRepository;

    @Autowired
    TurnService turnService;

    @Test
    @DisplayName("Accepter une offre expirée l'a marque EXPIRED en base")
    void expiredOfferIsPersistedWhenAcceptFails() {
        Player sender = playerRepository.findByName("lurio").orElseThrow();
        Player receiver = playerRepository.findByName("cegorach").orElseThrow();
        int turn = turnService.getCurrentTurn();
        ExchangeOffer offer = new ExchangeOffer(sender.getId(), receiver.getId(), 0.0, turn - 1, turn);
        exchangeOfferRepository.saveAndFlush(offer);

        try {
            assertThrows(IllegalStateException.class,
                    () -> exchangeOfferService.acceptOffer(receiver.getUserId(), offer.getId()));
            assertEquals(ExchangeOfferStatus.EXPIRED,
                    exchangeOfferRepository.findById(offer.getId()).orElseThrow().getStatus());
        } finally {
            // Offre technique : ne pas polluer les comptages d'offres des autres tests.
            exchangeOfferRepository.deleteById(offer.getId());
        }
    }
}
