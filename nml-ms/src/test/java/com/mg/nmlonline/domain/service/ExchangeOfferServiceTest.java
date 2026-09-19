package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.CreateExchangeOfferRequestDto;
import com.mg.nmlonline.api.dto.ExchangeOfferDto;
import com.mg.nmlonline.api.dto.ExchangeOfferItemDto;
import com.mg.nmlonline.domain.model.bank.ExchangeOffer;
import com.mg.nmlonline.domain.model.bank.ExchangeOfferResource;
import com.mg.nmlonline.domain.model.bank.ExchangeOfferStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.Resource;
import com.mg.nmlonline.infrastructure.repository.ExchangeOfferRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeOfferService")
class ExchangeOfferServiceTest {

    @Mock
    ExchangeOfferRepository exchangeOfferRepository;

    @Mock
    PlayerRepository playerRepository;

    @Mock
    ResourceRepository resourceRepository;

    @Mock
    ResourceService resourceService;

    @Mock
    TurnService turnService;

    @InjectMocks
    ExchangeOfferService exchangeOfferService;

    private Player sender;
    private Player receiver;

    @BeforeEach
    void setUp() {
        sender = new Player("Sender");
        sender.setId(1L);
        sender.setUserId(11L);
        sender.getStats().setMoney(1000.0);

        receiver = new Player("Receiver");
        receiver.setId(2L);
        receiver.setUserId(22L);
    }

    private ExchangeOffer offerOf(double money, int createdTurn, int expiresTurn) {
        ExchangeOffer offer = new ExchangeOffer(1L, 2L, money, createdTurn, expiresTurn);
        offer.setId(10L);
        return offer;
    }

    @Nested
    @DisplayName("Création")
    class CreateTests {

        @Test
        @DisplayName("Auto-échange refusé")
        void shouldRejectSelfOffer() {
            when(playerRepository.findByUserId(11L)).thenReturn(Optional.of(sender));

            assertThrows(IllegalArgumentException.class,
                    () -> exchangeOfferService.createOffer(11L,
                            new CreateExchangeOfferRequestDto(1L, 10.0, List.of())));

            verify(exchangeOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("Quatrième offre en attente refusée")
        void shouldRejectWhenTooManyPendingOffers() {
            when(playerRepository.findByUserId(11L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));
            when(exchangeOfferRepository.countBySenderPlayerIdAndStatus(1L, ExchangeOfferStatus.PENDING))
                    .thenReturn(3L);

            assertThrows(IllegalStateException.class,
                    () -> exchangeOfferService.createOffer(11L,
                            new CreateExchangeOfferRequestDto(2L, 10.0, List.of())));

            verify(exchangeOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("Succès : statut PENDING et expiration au tour suivant")
        void shouldCreateOfferExpiringNextTurn() {
            when(playerRepository.findByUserId(11L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));
            when(resourceRepository.findByName("Or")).thenReturn(Optional.of(new Resource("Or", 10.0)));
            when(turnService.getCurrentTurn()).thenReturn(4);
            when(exchangeOfferRepository.save(any(ExchangeOffer.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ExchangeOfferDto dto = exchangeOfferService.createOffer(11L,
                    new CreateExchangeOfferRequestDto(2L, 250.0,
                            List.of(new ExchangeOfferItemDto("Or", 5))));

            assertEquals(ExchangeOfferStatus.PENDING, dto.status());
            assertEquals(4, dto.createdTurn());
            assertEquals(5, dto.expiresTurn());
            assertEquals(250.0, dto.money());
            assertEquals(1, dto.resources().size());
            assertEquals("Sender", dto.senderName());
            assertEquals("Receiver", dto.receiverName());
        }

        @Test
        @DisplayName("Offre vide refusée")
        void shouldRejectEmptyOffer() {
            when(playerRepository.findByUserId(11L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));

            assertThrows(IllegalArgumentException.class,
                    () -> exchangeOfferService.createOffer(11L,
                            new CreateExchangeOfferRequestDto(2L, 0.0, List.of())));
        }

        @Test
        @DisplayName("Verrous joueurs par id croissant et purge des expirées avant le quota")
        void shouldLockPlayersInAscendingOrderAndExpireBeforeCounting() {
            Player senderHighId = new Player("SenderHighId");
            senderHighId.setId(9L);
            senderHighId.setUserId(11L);
            senderHighId.getStats().setMoney(1000.0);
            when(playerRepository.findByUserId(11L)).thenReturn(Optional.of(senderHighId));
            when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));
            when(playerRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(senderHighId));
            when(turnService.getCurrentTurn()).thenReturn(4);
            when(exchangeOfferRepository.save(any(ExchangeOffer.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            exchangeOfferService.createOffer(11L,
                    new CreateExchangeOfferRequestDto(2L, 10.0, List.of()));

            InOrder inOrder = inOrder(exchangeOfferRepository, playerRepository);
            inOrder.verify(exchangeOfferRepository).expirePendingOffers(
                    ExchangeOfferStatus.PENDING, ExchangeOfferStatus.EXPIRED, 4);
            inOrder.verify(playerRepository).findByIdForUpdate(2L);
            inOrder.verify(playerRepository).findByIdForUpdate(9L);
            inOrder.verify(exchangeOfferRepository).countBySenderPlayerIdAndStatus(
                    9L, ExchangeOfferStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Acceptation")
    class AcceptTests {

        @Test
        @DisplayName("Transfert atomique argent + ressources, offre ACCEPTED")
        void shouldTransferMoneyAndResourcesOnAccept() {
            ExchangeOffer offer = offerOf(200.0, 1, 2);
            offer.getResources().add(new ExchangeOfferResource("Or", 10));
            sender.addResource("Or", 50);
            when(exchangeOfferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(offer));
            when(turnService.getCurrentTurn()).thenReturn(1);
            when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));

            ExchangeOfferDto dto = exchangeOfferService.acceptOffer(22L, 10L);

            assertEquals(ExchangeOfferStatus.ACCEPTED, dto.status());
            assertEquals(800.0, sender.getStats().getMoney());
            assertEquals(200.0, receiver.getStats().getMoney());
            verify(resourceService).transferResource(sender, receiver, "Or", 10);
            verify(exchangeOfferRepository).save(offer);
        }

        @Test
        @DisplayName("Solde insuffisant du donneur : refus sans mutation, l'offre reste PENDING")
        void shouldKeepOfferPendingWhenSenderHasInsufficientFunds() {
            ExchangeOffer offer = offerOf(2000.0, 1, 2);
            when(exchangeOfferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(offer));
            when(turnService.getCurrentTurn()).thenReturn(1);
            when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));

            assertThrows(IllegalArgumentException.class,
                    () -> exchangeOfferService.acceptOffer(22L, 10L));

            assertEquals(ExchangeOfferStatus.PENDING, offer.getStatus());
            assertEquals(1000.0, sender.getStats().getMoney());
            verify(exchangeOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("Offre expirée : marquée EXPIRED et refusée")
        void shouldExpireOfferOnAccept() {
            ExchangeOffer offer = offerOf(100.0, 1, 2);
            when(exchangeOfferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(offer));
            when(turnService.getCurrentTurn()).thenReturn(2);

            assertThrows(IllegalStateException.class,
                    () -> exchangeOfferService.acceptOffer(22L, 10L));

            assertEquals(ExchangeOfferStatus.EXPIRED, offer.getStatus());
        }

        @Test
        @DisplayName("Un autre joueur que le destinataire ne peut pas accepter")
        void shouldRejectAcceptByNonReceiver() {
            ExchangeOffer offer = offerOf(100.0, 1, 2);
            when(exchangeOfferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(offer));
            when(turnService.getCurrentTurn()).thenReturn(1);
            when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));

            assertThrows(SecurityException.class,
                    () -> exchangeOfferService.acceptOffer(11L, 10L));

            assertEquals(ExchangeOfferStatus.PENDING, offer.getStatus());
        }
    }

    @Nested
    @DisplayName("Dotation de départ")
    class StartingMoneyTests {

        @Test
        @DisplayName("Offre refusée tant que la dotation n'est pas dépensée")
        void shouldRefuseOfferFromStartingMoney() {
            sender.initializeStartingMoney();
            when(playerRepository.findByUserId(11L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));

            assertThrows(IllegalArgumentException.class,
                    () -> exchangeOfferService.createOffer(11L,
                            new CreateExchangeOfferRequestDto(2L, 500.0, List.of())));

            verify(exchangeOfferRepository, never()).save(any());
        }

        @Test
        @DisplayName("Offre possible avec l'argent gagné, plafonnée à celui-ci")
        void shouldCapOfferToEarnedMoney() {
            sender.initializeStartingMoney();
            sender.incrementMoney(400.0);
            when(playerRepository.findByUserId(11L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));
            when(exchangeOfferRepository.countBySenderPlayerIdAndStatus(1L, ExchangeOfferStatus.PENDING))
                    .thenReturn(0L);
            when(turnService.getCurrentTurn()).thenReturn(2);
            when(exchangeOfferRepository.save(any(ExchangeOffer.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ExchangeOfferDto dto = exchangeOfferService.createOffer(11L,
                    new CreateExchangeOfferRequestDto(2L, 400.0, List.of()));

            assertEquals(400.0, dto.money());
            assertThrows(IllegalArgumentException.class,
                    () -> exchangeOfferService.createOffer(11L,
                            new CreateExchangeOfferRequestDto(2L, 500.0, List.of())));
        }

        @Test
        @DisplayName("Acceptation refusée si la dotation est intacte, offre laissée PENDING")
        void shouldRefuseAcceptFromStartingMoney() {
            ExchangeOffer offer = offerOf(100.0, 1, 2);
            sender.initializeStartingMoney();
            when(exchangeOfferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(offer));
            when(turnService.getCurrentTurn()).thenReturn(1);
            when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
            when(playerRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));

            assertThrows(IllegalArgumentException.class,
                    () -> exchangeOfferService.acceptOffer(22L, 10L));

            assertEquals(ExchangeOfferStatus.PENDING, offer.getStatus());
            verify(exchangeOfferRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Vue admin")
    class AdminListTests {

        @Test
        @DisplayName("Liste toutes les offres avec noms, puis filtre par statut")
        void shouldListAllOffersWithNamesAndFilter() {
            ExchangeOffer offer = offerOf(100.0, 1, 2);
            Pageable pageable = PageRequest.of(0, 20);
            when(turnService.getCurrentTurn()).thenReturn(1);
            when(exchangeOfferRepository.findAllByOrderByCreatedAtDesc(pageable))
                    .thenReturn(new PageImpl<>(List.of(offer)));
            when(playerRepository.findAllById(any())).thenReturn(List.of(sender, receiver));

            Page<ExchangeOfferDto> page = exchangeOfferService.getAllOffers(null, pageable);

            assertEquals(1, page.getTotalElements());
            assertEquals("Sender", page.getContent().getFirst().senderName());
            assertEquals("Receiver", page.getContent().getFirst().receiverName());
            verify(exchangeOfferRepository).expirePendingOffers(
                    ExchangeOfferStatus.PENDING, ExchangeOfferStatus.EXPIRED, 1);

            when(exchangeOfferRepository.findByStatusOrderByCreatedAtDesc(
                    ExchangeOfferStatus.ACCEPTED, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            assertTrue(exchangeOfferService.getAllOffers("ACCEPTED", pageable).isEmpty());
        }
    }

    @Nested
    @DisplayName("Annulation et refus")
    class CancelAndDeclineTests {
        @Test
        @DisplayName("L'émetteur annule son offre, le destinataire la refuse")
        void shouldCancelAndDecline() {
            ExchangeOffer toCancel = offerOf(100.0, 1, 2);
            when(exchangeOfferRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(toCancel));
            when(turnService.getCurrentTurn()).thenReturn(1);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(sender));
            when(playerRepository.findById(2L)).thenReturn(Optional.of(receiver));

            ExchangeOfferDto cancelled = exchangeOfferService.cancelOffer(11L, 10L);

            assertEquals(ExchangeOfferStatus.CANCELLED, cancelled.status());

            ExchangeOffer toDecline = offerOf(100.0, 1, 2);
            when(exchangeOfferRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(toDecline));

            ExchangeOfferDto declined = exchangeOfferService.declineOffer(22L, 20L);

            assertEquals(ExchangeOfferStatus.DECLINED, declined.status());
        }
    }
}
