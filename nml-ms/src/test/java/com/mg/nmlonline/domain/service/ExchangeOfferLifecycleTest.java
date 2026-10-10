package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.ExchangeScenarioSummaryDto;
import com.mg.nmlonline.domain.model.bank.ExchangeOffer;
import com.mg.nmlonline.domain.model.bank.ExchangeOfferStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.ExchangeOfferRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EmbeddedPostgresTest
@DisplayName("Échange — expiration persistée et scénario de dev")
class ExchangeOfferLifecycleTest {

    @Autowired
    ExchangeOfferService exchangeOfferService;

    @Autowired
    ExchangeScenarioSeeder seeder;

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

    @Test
    @Transactional
    @DisplayName("Seed une offre en attente et un échange accepté, re-jouable sans doublon")
    void shouldSeedPendingAndAcceptedOffersIdempotently() {
        ExchangeScenarioSummaryDto first = seeder.seedExchangeScenario();

        assertNotNull(first.pendingOfferId());
        assertNotNull(first.acceptedOfferId());

        ExchangeOffer pending = exchangeOfferRepository.findById(first.pendingOfferId()).orElseThrow();
        assertEquals(ExchangeOfferStatus.PENDING, pending.getStatus());
        assertEquals(1500.0, pending.getMoney());

        ExchangeOffer accepted = exchangeOfferRepository.findById(first.acceptedOfferId()).orElseThrow();
        assertEquals(ExchangeOfferStatus.ACCEPTED, accepted.getStatus());
        assertEquals(1, accepted.getResources().size());
        assertEquals(1000.0, accepted.getMoney());

        Player receiver = playerRepository.findByName("nursek").orElseThrow();
        assertTrue(receiver.getResourceQuantity("Or") >= 2, "Les ressources acceptées sont transférées");

        long offersAfterFirst = exchangeOfferRepository.count();

        ExchangeScenarioSummaryDto second = seeder.seedExchangeScenario();

        assertNotEquals(first.pendingOfferId(), second.pendingOfferId());

        // 6 seeds = 12 « Or » demandés pour 10 disponibles au départ : couvre l'épuisement de la ressource.
        for (int i = 0; i < 4; i++) {
            seeder.seedExchangeScenario();
        }

        assertEquals(offersAfterFirst, exchangeOfferRepository.count(), "Pas de doublon après re-seed");
    }
}
