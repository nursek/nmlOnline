package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.CreateExchangeOfferRequestDto;
import com.mg.nmlonline.api.dto.ExchangeOfferDto;
import com.mg.nmlonline.api.dto.ExchangeOfferItemDto;
import com.mg.nmlonline.domain.model.bank.ExchangeOffer;
import com.mg.nmlonline.domain.model.bank.ExchangeOfferResource;
import com.mg.nmlonline.domain.model.bank.ExchangeOfferStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.ExchangeOfferRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// noRollbackFor : l'expiration paresseuse doit survivre au refus qui la déclenche, sinon elle est perdue.
@Service
@Transactional(noRollbackFor = IllegalStateException.class)
public class ExchangeOfferService {

    private static final int MAX_PENDING_OFFERS = 3;

    private final ExchangeOfferRepository exchangeOfferRepository;
    private final PlayerRepository playerRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceService resourceService;
    private final TurnService turnService;

    public ExchangeOfferService(ExchangeOfferRepository exchangeOfferRepository,
                                PlayerRepository playerRepository,
                                ResourceRepository resourceRepository,
                                ResourceService resourceService,
                                TurnService turnService) {
        this.exchangeOfferRepository = exchangeOfferRepository;
        this.playerRepository = playerRepository;
        this.resourceRepository = resourceRepository;
        this.resourceService = resourceService;
        this.turnService = turnService;
    }

    public ExchangeOfferDto createOffer(Long userId, CreateExchangeOfferRequestDto request) {
        if (request == null || request.receiverPlayerId() == null) {
            throw new IllegalArgumentException("Destinataire manquant");
        }
        if (!Double.isFinite(request.money()) || request.money() < 0) {
            throw new IllegalArgumentException("Montant invalide");
        }

        Player sender = playerRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable"));
        Player receiver = playerRepository.findById(request.receiverPlayerId())
                .orElseThrow(() -> new IllegalArgumentException("Destinataire introuvable"));
        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Impossible de s'échanger des ressources à soi-même");
        }

        List<ExchangeOfferItemDto> items = validatedItems(request.resources());
        if (request.money() <= 0 && items.isEmpty()) {
            throw new IllegalArgumentException("L'offre doit contenir de l'argent ou des ressources");
        }
        if (request.money() > sender.getTransferableMoney()) {
            throw new IllegalArgumentException("Impossible d'offrir votre dotation de départ");
        }

        long pending = exchangeOfferRepository.countBySenderPlayerIdAndStatus(sender.getId(),
                ExchangeOfferStatus.PENDING);
        if (pending >= MAX_PENDING_OFFERS) {
            throw new IllegalStateException("Trois offres en attente maximum");
        }

        int turn = turnService.getCurrentTurn();
        ExchangeOffer offer = new ExchangeOffer(sender.getId(), receiver.getId(), request.money(),
                turn, turn + 1);
        for (ExchangeOfferItemDto item : items) {
            offer.addResource(new ExchangeOfferResource(item.resourceName(), item.quantity()));
        }
        exchangeOfferRepository.save(offer);
        return toDto(offer, sender.getName(), receiver.getName());
    }

    public List<ExchangeOfferDto> getOffers(Long userId) {
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable"));

        int turn = turnService.getCurrentTurn();
        exchangeOfferRepository.expirePendingOffers(ExchangeOfferStatus.PENDING,
                ExchangeOfferStatus.EXPIRED, turn);

        List<ExchangeOffer> offers = exchangeOfferRepository
                .findBySenderPlayerIdOrReceiverPlayerIdOrderByCreatedAtDesc(player.getId(), player.getId());
        Map<Long, String> names = playerNames(offers);
        return offers.stream()
                .map(offer -> toDto(offer, names.get(offer.getSenderPlayerId()),
                        names.get(offer.getReceiverPlayerId())))
                .toList();
    }

    /** Vue admin : toutes les offres, avec expiration paresseuse des PENDING comme la vue joueur. */
    public Page<ExchangeOfferDto> getAllOffers(String status, Pageable pageable) {
        int turn = turnService.getCurrentTurn();
        exchangeOfferRepository.expirePendingOffers(ExchangeOfferStatus.PENDING,
                ExchangeOfferStatus.EXPIRED, turn);

        Page<ExchangeOffer> offers;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            offers = exchangeOfferRepository.findAllByOrderByCreatedAtDesc(pageable);
        } else {
            offers = exchangeOfferRepository.findByStatusOrderByCreatedAtDesc(parseStatus(status), pageable);
        }

        Map<Long, String> names = playerNames(offers.getContent());
        return offers.map(offer -> toDto(offer, names.get(offer.getSenderPlayerId()),
                names.get(offer.getReceiverPlayerId())));
    }

    private ExchangeOfferStatus parseStatus(String status) {
        try {
            return ExchangeOfferStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut d'offre inconnu : " + status);
        }
    }

    public ExchangeOfferDto acceptOffer(Long userId, Long offerId) {
        ExchangeOffer offer = lockOffer(offerId);

        int turn = turnService.getCurrentTurn();
        ensurePending(offer, turn);

        // Verrouillage des joueurs par id croissant : ordre canonique anti-deadlock.
        Long senderId = offer.getSenderPlayerId();
        Long receiverId = offer.getReceiverPlayerId();
        Player first = lockPlayer(Math.min(senderId, receiverId));
        Player second = lockPlayer(Math.max(senderId, receiverId));
        Player sender = senderId.equals(first.getId()) ? first : second;
        Player receiver = receiverId.equals(first.getId()) ? first : second;

        if (!userId.equals(receiver.getUserId())) {
            throw new SecurityException("Seul le destinataire peut accepter cette offre");
        }
        if (offer.getMoney() > sender.getTransferableMoney()) {
            throw new IllegalArgumentException("La dotation de départ du donneur n'est pas transférable");
        }
        if (sender.getStats().getMoney() < offer.getMoney()) {
            throw new IllegalArgumentException("Solde insuffisant du donneur pour honorer l'offre");
        }
        for (ExchangeOfferResource resource : offer.getResources()) {
            if (!sender.hasResource(resource.getResourceName(), resource.getQuantity())) {
                throw new IllegalArgumentException(
                        "Ressource insuffisante du donneur : " + resource.getResourceName());
            }
        }

        sender.decrementMoney(offer.getMoney());
        receiver.incrementMoney(offer.getMoney());
        for (ExchangeOfferResource resource : offer.getResources()) {
            resourceService.transferResource(sender, receiver, resource.getResourceName(),
                    resource.getQuantity());
        }

        offer.accept(turn);
        playerRepository.save(sender);
        playerRepository.save(receiver);
        exchangeOfferRepository.save(offer);
        return toDto(offer, sender.getName(), receiver.getName());
    }

    public ExchangeOfferDto declineOffer(Long userId, Long offerId) {
        ExchangeOffer offer = lockOffer(offerId);
        int turn = turnService.getCurrentTurn();
        ensurePending(offer, turn);

        Player receiver = playerRepository.findById(offer.getReceiverPlayerId())
                .orElseThrow(() -> new IllegalArgumentException("Destinataire introuvable"));
        if (!userId.equals(receiver.getUserId())) {
            throw new SecurityException("Seul le destinataire peut refuser cette offre");
        }

        offer.decline(turn);
        exchangeOfferRepository.save(offer);
        return toDto(offer, nameOf(offer.getSenderPlayerId()), receiver.getName());
    }

    public ExchangeOfferDto cancelOffer(Long userId, Long offerId) {
        ExchangeOffer offer = lockOffer(offerId);
        int turn = turnService.getCurrentTurn();
        ensurePending(offer, turn);

        Player sender = playerRepository.findById(offer.getSenderPlayerId())
                .orElseThrow(() -> new IllegalArgumentException("Émetteur introuvable"));
        if (!userId.equals(sender.getUserId())) {
            throw new SecurityException("Seul l'émetteur peut annuler cette offre");
        }

        offer.cancel(turn);
        exchangeOfferRepository.save(offer);
        return toDto(offer, sender.getName(), nameOf(offer.getReceiverPlayerId()));
    }

    private ExchangeOffer lockOffer(Long offerId) {
        return exchangeOfferRepository.findByIdForUpdate(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offre introuvable : " + offerId));
    }

    private Player lockPlayer(Long playerId) {
        return playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable : " + playerId));
    }

    private void ensurePending(ExchangeOffer offer, int turn) {
        if (!offer.isPending()) {
            throw new IllegalStateException("Cette offre a déjà été résolue");
        }
        if (offer.isExpiredAt(turn)) {
            offer.expire(turn);
            exchangeOfferRepository.save(offer);
            throw new IllegalStateException("Cette offre a expiré");
        }
    }

    private List<ExchangeOfferItemDto> validatedItems(List<ExchangeOfferItemDto> resources) {
        List<ExchangeOfferItemDto> items = new ArrayList<>();
        Set<String> names = new HashSet<>();
        if (resources == null) {
            return items;
        }
        for (ExchangeOfferItemDto item : resources) {
            if (item == null || item.resourceName() == null || item.resourceName().isBlank()
                    || item.quantity() <= 0) {
                throw new IllegalArgumentException("Ligne de ressource invalide");
            }
            if (!names.add(item.resourceName())) {
                throw new IllegalArgumentException("Ressource en double : " + item.resourceName());
            }
            if (resourceRepository.findByName(item.resourceName()).isEmpty()) {
                throw new IllegalArgumentException("Ressource inconnue : " + item.resourceName());
            }
            items.add(item);
        }
        return items;
    }

    private Map<Long, String> playerNames(List<ExchangeOffer> offers) {
        Set<Long> ids = new HashSet<>();
        for (ExchangeOffer offer : offers) {
            ids.add(offer.getSenderPlayerId());
            ids.add(offer.getReceiverPlayerId());
        }
        Map<Long, String> names = new HashMap<>();
        for (Player player : playerRepository.findAllById(ids)) {
            names.put(player.getId(), player.getName());
        }
        return names;
    }

    private String nameOf(Long playerId) {
        return playerRepository.findById(playerId).map(Player::getName).orElse(null);
    }

    private ExchangeOfferDto toDto(ExchangeOffer offer, String senderName, String receiverName) {
        List<ExchangeOfferItemDto> items = offer.getResources().stream()
                .map(resource -> new ExchangeOfferItemDto(resource.getResourceName(), resource.getQuantity()))
                .toList();
        return new ExchangeOfferDto(offer.getId(), offer.getSenderPlayerId(), senderName,
                offer.getReceiverPlayerId(), receiverName, offer.getMoney(), items, offer.getStatus(),
                offer.getCreatedTurn(), offer.getExpiresTurn(), offer.getResolvedTurn(),
                offer.getCreatedAt());
    }
}
