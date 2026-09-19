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
import static org.junit.jupiter.api.Assertions.assertTrue;

@EmbeddedPostgresTest
@DisplayName("ExchangeScenarioSeeder")
class ExchangeScenarioSeederTest {

    @Autowired
    ExchangeScenarioSeeder seeder;

    @Autowired
    ExchangeOfferRepository exchangeOfferRepository;

    @Autowired
    PlayerRepository playerRepository;

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
        assertEquals(offersAfterFirst, exchangeOfferRepository.count(), "Pas de doublon après re-seed");
    }
}
