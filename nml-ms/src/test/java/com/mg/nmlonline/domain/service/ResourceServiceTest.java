package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.SellResourceBatchItemDto;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.resource.Resource;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerResourceRepository;
import com.mg.nmlonline.infrastructure.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceService")
class ResourceServiceTest {

    @Mock
    ResourceRepository resourceRepository;

    @Mock
    PlayerResourceRepository playerResourceRepository;

    @Mock
    PlayerRepository playerRepository;

    @InjectMocks
    ResourceService resourceService;

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer");
        player.setId(1L);
        player.setUserId(10L);
        player.getStats().setMoney(1000.0);
    }

    private PlayerResource ownedResource(String name, int quantity) {
        PlayerResource pr = new PlayerResource(name, quantity);
        pr.setPlayer(player);
        return pr;
    }

    @Nested
    @DisplayName("Table des multiplicateurs de vente")
    class SaleMultiplierTests {

        private void stubGoldPrice() {
            when(resourceRepository.findByName("Or")).thenReturn(Optional.of(new Resource("Or", 1700.0)));
        }

        @Test
        @DisplayName("Multiplicateurs aux bornes : 1→1.0, 2→3.0, 5→13.0, 8→33.0, 9→45.0")
        void shouldPinMultiplierTable() {
            stubGoldPrice();

            assertEquals(1700.0, resourceService.calculateSaleValue("Or", 1));
            assertEquals(5100.0, resourceService.calculateSaleValue("Or", 2));
            assertEquals(22100.0, resourceService.calculateSaleValue("Or", 5));
            assertEquals(56100.0, resourceService.calculateSaleValue("Or", 8));
            assertEquals(76500.0, resourceService.calculateSaleValue("Or", 9));
        }

        @Test
        @DisplayName("Au-delà de 9, le multiplicateur est plafonné à 45.0")
        void shouldCapMultiplierAboveNine() {
            stubGoldPrice();

            assertEquals(76500.0, resourceService.calculateSaleValue("Or", 10));
            assertEquals(76500.0, resourceService.calculateSaleValue("Or", 1000));
        }

        @Test
        @DisplayName("Quantité <= 0 donne une valeur nulle")
        void shouldReturnZeroForNonPositiveQuantity() {
            stubGoldPrice();

            assertEquals(0.0, resourceService.calculateSaleValue("Or", 0));
            assertEquals(0.0, resourceService.calculateSaleValue("Or", -5));
        }

        @Test
        @DisplayName("Ressource inconnue est rejetée")
        void shouldRejectUnknownResource() {
            when(resourceRepository.findByName("Diamant")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> resourceService.calculateSaleValue("Diamant", 1));
        }
    }

    @Nested
    @DisplayName("Vente unitaire")
    class SellResourceTests {

        @Test
        @DisplayName("Joueur introuvable est rejeté")
        void shouldRejectUnknownPlayer() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> resourceService.sellResource(7L, 1, 10L));
        }

        @Test
        @DisplayName("Ressource introuvable est rejetée")
        void shouldRejectUnknownResource() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(playerResourceRepository.findById(7L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> resourceService.sellResource(7L, 1, 10L));
        }

        @Test
        @DisplayName("Vendre la ressource d'un autre joueur est interdit")
        void shouldRejectResourceOwnedByAnotherPlayer() {
            Player other = new Player("Other");
            other.setId(2L);
            PlayerResource pr = new PlayerResource("Or", 5);
            pr.setPlayer(other);

            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(playerResourceRepository.findById(7L)).thenReturn(Optional.of(pr));

            assertThrows(SecurityException.class,
                    () -> resourceService.sellResource(7L, 1, 10L));
        }

        @Test
        @DisplayName("Quantité insuffisante est rejetée")
        void shouldRejectInsufficientQuantity() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(playerResourceRepository.findById(7L)).thenReturn(Optional.of(ownedResource("Or", 5)));

            assertThrows(IllegalArgumentException.class,
                    () -> resourceService.sellResource(7L, 6, 10L));
        }

        @Test
        @DisplayName("Vente partielle crédite le prix exact et conserve le stack")
        void shouldSellPartialQuantity() {
            PlayerResource pr = ownedResource("Or", 5);
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(playerResourceRepository.findById(7L)).thenReturn(Optional.of(pr));
            when(resourceRepository.findByName("Or")).thenReturn(Optional.of(new Resource("Or", 1700.0)));

            ResourceService.SaleResult result = resourceService.sellResource(7L, 2, 10L);

            assertEquals("Or", result.resourceName());
            assertEquals(2, result.quantitySold());
            assertEquals(5100.0, result.saleValue());
            assertEquals(6100.0, player.getStats().getMoney());
            assertEquals(3, pr.getQuantity());
            verify(playerResourceRepository).save(pr);
            verify(playerResourceRepository, never()).delete(any(PlayerResource.class));
            verify(playerRepository).save(player);
        }

        @Test
        @DisplayName("Vente totale supprime le stack")
        void shouldDeleteStackWhenQuantityReachesZero() {
            PlayerResource pr = ownedResource("Or", 5);
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(playerResourceRepository.findById(7L)).thenReturn(Optional.of(pr));
            when(resourceRepository.findByName("Or")).thenReturn(Optional.of(new Resource("Or", 1700.0)));

            ResourceService.SaleResult result = resourceService.sellResource(7L, 5, 10L);

            assertEquals(22100.0, result.saleValue());
            assertEquals(0, pr.getQuantity());
            verify(playerResourceRepository).delete(pr);
        }

        @Test
        @DisplayName("Vendre une quantité nulle est un no-op silencieux")
        void shouldPinZeroQuantitySaleAsNoOp() {
            PlayerResource pr = ownedResource("Or", 5);
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(playerResourceRepository.findById(7L)).thenReturn(Optional.of(pr));
            when(resourceRepository.findByName("Or")).thenReturn(Optional.of(new Resource("Or", 1700.0)));

            ResourceService.SaleResult result = resourceService.sellResource(7L, 0, 10L);

            assertEquals(0.0, result.saleValue());
            assertEquals(5, pr.getQuantity());
            assertEquals(1000.0, player.getStats().getMoney());
        }
    }

    @Nested
    @DisplayName("Vente en lot")
    class SellResourcesBatchTests {

        @Test
        @DisplayName("Panier vide est rejeté")
        void shouldRejectEmptyCart() {
            assertThrows(IllegalArgumentException.class,
                    () -> resourceService.sellResourcesBatch(10L, List.of()));
            assertThrows(IllegalArgumentException.class,
                    () -> resourceService.sellResourcesBatch(10L, null));
        }

        @Test
        @DisplayName("Lot de deux ventes crédite les deux montants")
        void shouldSellBatch() {
            PlayerResource gold = ownedResource("Or", 5);
            PlayerResource ivory = ownedResource("Ivoire", 3);
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(playerResourceRepository.findById(7L)).thenReturn(Optional.of(gold));
            when(playerResourceRepository.findById(8L)).thenReturn(Optional.of(ivory));
            when(resourceRepository.findByName("Or")).thenReturn(Optional.of(new Resource("Or", 1700.0)));
            when(resourceRepository.findByName("Ivoire")).thenReturn(Optional.of(new Resource("Ivoire", 1100.0)));

            SellResourceBatchItemDto first = new SellResourceBatchItemDto();
            first.setPlayerResourceId(7L);
            first.setQuantity(2);
            SellResourceBatchItemDto second = new SellResourceBatchItemDto();
            second.setPlayerResourceId(8L);
            second.setQuantity(1);

            List<ResourceService.SaleResult> results =
                    resourceService.sellResourcesBatch(10L, List.of(first, second));

            assertEquals(2, results.size());
            assertEquals(7200.0, player.getStats().getMoney());
        }
    }

    @Nested
    @DisplayName("Transferts et collecte")
    class TransferTests {

        @Test
        @DisplayName("Transfert réussi déplace la quantité")
        void shouldTransferResource() {
            Player from = new Player("From");
            from.addResource("Or", 10);
            Player to = new Player("To");

            boolean result = resourceService.transferResource(from, to, "Or", 4);

            assertTrue(result);
            assertEquals(6, from.getResourceQuantity("Or"));
            assertEquals(4, to.getResourceQuantity("Or"));
        }

        @Test
        @DisplayName("Transfert refusé si source insuffisante ou paramètres invalides")
        void shouldRefuseInvalidTransfer() {
            Player from = new Player("From");
            from.addResource("Or", 3);
            Player to = new Player("To");

            assertFalse(resourceService.transferResource(from, to, "Or", 4));
            assertFalse(resourceService.transferResource(from, to, "Or", 0));
            assertFalse(resourceService.transferResource(from, to, null, 1));
            assertFalse(resourceService.transferResource(null, to, "Or", 1));
            assertFalse(resourceService.transferResource(from, null, "Or", 1));

            assertEquals(3, from.getResourceQuantity("Or"));
            assertEquals(0, to.getResourceQuantity("Or"));
        }

        @Test
        @DisplayName("Collecte ajoute la ressource au joueur")
        void shouldCollectSectorResource() {
            resourceService.collectSectorResource(player, "Or", 500);

            assertEquals(500, player.getResourceQuantity("Or"));
        }

        @Test
        @DisplayName("Collecte ignorée si paramètres invalides")
        void shouldIgnoreInvalidCollection() {
            // Le secteur n'est pas marqué collecté : la ressource reste re-collectable.
            resourceService.collectSectorResource(null, "Or", 500);
            resourceService.collectSectorResource(player, null, 500);
            resourceService.collectSectorResource(player, "Or", 0);

            assertEquals(0, player.getResourceQuantity("Or"));
        }
    }
}
