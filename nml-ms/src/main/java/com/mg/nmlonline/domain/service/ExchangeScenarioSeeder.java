package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.CreateExchangeOfferRequestDto;
import com.mg.nmlonline.api.dto.ExchangeOfferDto;
import com.mg.nmlonline.api.dto.ExchangeOfferItemDto;
import com.mg.nmlonline.api.dto.ExchangeScenarioSummaryDto;
import com.mg.nmlonline.domain.model.bank.ExchangeOffer;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.ExchangeOfferRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExchangeScenarioSeeder {

    private static final String SENDER = "lurio";
    private static final String RECEIVER = "cegorach";
    private static final String SECOND_SENDER = "imotekh";
    private static final String SECOND_RECEIVER = "nursek";
    private static final double PENDING_MONEY = 1500.0;
    private static final double ACCEPTED_MONEY = 1000.0;

    private final PlayerRepository playerRepository;
    private final ExchangeOfferRepository exchangeOfferRepository;
    private final ExchangeOfferService exchangeOfferService;
    private final TurnService turnService;
    private final EntityManager entityManager;

    public ExchangeScenarioSeeder(PlayerRepository playerRepository,
                                  ExchangeOfferRepository exchangeOfferRepository,
                                  ExchangeOfferService exchangeOfferService,
                                  TurnService turnService,
                                  EntityManager entityManager) {
        this.playerRepository = playerRepository;
        this.exchangeOfferRepository = exchangeOfferRepository;
        this.exchangeOfferService = exchangeOfferService;
        this.turnService = turnService;
        this.entityManager = entityManager;
    }

    @Transactional
    public ExchangeScenarioSummaryDto seedExchangeScenario() {
        Player sender = resolvePlayer(SENDER);
        Player receiver = resolvePlayer(RECEIVER);
        Player secondSender = resolvePlayer(SECOND_SENDER);
        Player secondReceiver = resolvePlayer(SECOND_RECEIVER);

        purgeOffers(List.of(sender, receiver, secondSender, secondReceiver));
        ensureEarnedMoney(sender, PENDING_MONEY);
        ensureEarnedMoney(secondSender, ACCEPTED_MONEY);
        ensureResource(secondSender, "Or", 2);

        ExchangeOfferDto pending = exchangeOfferService.createOffer(requireUserId(sender),
                new CreateExchangeOfferRequestDto(receiver.getId(), PENDING_MONEY,
                        List.of(new ExchangeOfferItemDto("Cigares", 3))));

        ExchangeOfferDto created = exchangeOfferService.createOffer(requireUserId(secondSender),
                new CreateExchangeOfferRequestDto(secondReceiver.getId(), ACCEPTED_MONEY,
                        List.of(new ExchangeOfferItemDto("Or", 2))));
        exchangeOfferService.acceptOffer(requireUserId(secondReceiver), created.id());

        String message = "Offre #" + pending.id() + " en attente (" + SENDER + " → " + RECEIVER
                + "), échange #" + created.id() + " accepté (" + SECOND_SENDER + " → " + SECOND_RECEIVER + ").";
        return new ExchangeScenarioSummaryDto(turnService.getCurrentTurn(), pending.id(),
                created.id(), message);
    }

    private Player resolvePlayer(String name) {
        return playerRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException(
                        "Joueur de fixture introuvable : " + name + " (import démo requis)"));
    }

    private Long requireUserId(Player player) {
        if (player.getUserId() == null) {
            throw new IllegalStateException("Le joueur " + player.getName() + " n'a pas de compte utilisateur");
        }
        return player.getUserId();
    }

    private void purgeOffers(List<Player> players) {
        Map<Long, ExchangeOffer> toDelete = new LinkedHashMap<>();
        for (Player player : players) {
            exchangeOfferRepository
                    .findBySenderPlayerIdOrReceiverPlayerIdOrderByCreatedAtDesc(player.getId(), player.getId())
                    .forEach(offer -> toDelete.put(offer.getId(), offer));
        }
        if (!toDelete.isEmpty()) {
            exchangeOfferRepository.deleteAll(toDelete.values());
            entityManager.flush();
        }
    }

    /** L'argent importé est de la dotation : crédite des revenus pour autoriser une offre en argent. */
    private void ensureEarnedMoney(Player player, double target) {
        double missing = target - player.getTransferableMoney();
        if (missing > 0) {
            player.incrementMoney(missing);
            playerRepository.save(player);
        }
    }

    /** L'échange accepté sort définitivement les « Or » du donneur : re-crédit pour rester re-jouable. */
    private void ensureResource(Player player, String resourceName, int quantity) {
        if (player.getResourceQuantity(resourceName) < quantity) {
            player.addResource(resourceName, quantity);
            playerRepository.save(player);
        }
    }
}
