package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BuyEquipmentItemDto;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService — Achat d'équipements")
class PlayerServiceTest {

    @Mock
    PlayerRepository playerRepository;

    @Mock
    SectorService sectorService;

    @Mock
    EquipmentService equipmentService;

    @Mock
    PlayerActionService playerActionService;

    @InjectMocks
    PlayerService playerService;

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer");
        player.setId(1L);
        player.getStats().setMoney(2000.0);
    }

    private BuyEquipmentItemDto item(String name, int quantity) {
        BuyEquipmentItemDto dto = new BuyEquipmentItemDto();
        dto.setName(name);
        dto.setQuantity(quantity);
        return dto;
    }

    private BuyEquipmentItemDto item(String name, int quantity, Long cacheId) {
        BuyEquipmentItemDto dto = item(name, quantity);
        dto.setCacheId(cacheId);
        return dto;
    }

    private WeaponCache cache(long id, int stored) {
        WeaponCache cache = new WeaponCache(1L);
        cache.setId(id);
        player.getBuildings().add(cache);
        if (stored > 0) {
            EquipmentStack stack = new EquipmentStack(equipment("Stock", 1));
            stack.setQuantity(stored);
            stack.setAvailable(stored);
            cache.getStoredEquipments().add(stack);
        }
        return cache;
    }

    private Equipment equipment(String name, int cost) {
        return new Equipment(name, cost, 0, 0, 0, 0,
                Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
    }

    @Test
    @DisplayName("playerId null est rejeté")
    void shouldRejectNullPlayerId() {
        assertThrows(IllegalArgumentException.class,
                () -> playerService.buyEquipments(null, List.of(item("Pistolet", 1))));
    }

    @Test
    @DisplayName("Joueur introuvable est rejeté")
    void shouldRejectUnknownPlayer() {
        when(playerRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> playerService.buyEquipments(99L, List.of(item("Pistolet", 1))));
    }

    @Test
    @DisplayName("Panier null ou vide est rejeté")
    void shouldRejectNullOrEmptyCart() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));

        assertThrows(IllegalArgumentException.class,
                () -> playerService.buyEquipments(1L, null));
        assertThrows(IllegalArgumentException.class,
                () -> playerService.buyEquipments(1L, List.of()));
    }

    @Test
    @DisplayName("Item null dans le panier est rejeté")
    void shouldRejectNullItemInCart() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        List<BuyEquipmentItemDto> items = new ArrayList<>();
        items.add(null);

        assertThrows(IllegalArgumentException.class,
                () -> playerService.buyEquipments(1L, items));
    }

    @Test
    @DisplayName("Quantité <= 0 est rejetée")
    void shouldRejectNonPositiveQuantity() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));

        assertThrows(IllegalArgumentException.class,
                () -> playerService.buyEquipments(1L, List.of(item("Pistolet", 0))));
    }

    @Test
    @DisplayName("Équipement inconnu est rejeté")
    void shouldRejectUnknownEquipment() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(equipmentService.findByName("Inconnu")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> playerService.buyEquipments(1L, List.of(item("Inconnu", 1))));
    }

    @Test
    @DisplayName("Coût total supérieur au solde lève InsufficientFundsException avant tout débit")
    void shouldThrowInsufficientFundsBeforeAnyDebit() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        Equipment gun = equipment("Pistolet", 500);
        Equipment rifle = equipment("Fusil", 300);
        when(equipmentService.findByName("Pistolet")).thenReturn(Optional.of(gun));
        when(equipmentService.findByName("Fusil")).thenReturn(Optional.of(rifle));

        assertThrows(InsufficientFundsException.class,
                () -> playerService.buyEquipments(1L, List.of(item("Pistolet", 3), item("Fusil", 2))));

        assertEquals(2000.0, player.getStats().getMoney());
        assertTrue(player.getEquipments().isEmpty());
        verify(playerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Achat réussi débite le coût total et empile les équipements")
    void shouldBuyAllItemsAtomically() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(equipmentService.findByName("Pistolet")).thenReturn(Optional.of(equipment("Pistolet", 500)));
        when(equipmentService.findByName("Fusil")).thenReturn(Optional.of(equipment("Fusil", 300)));
        when(playerRepository.save(player)).thenReturn(player);

        Player result = playerService.buyEquipments(1L,
                List.of(item("Pistolet", 2), item("Fusil", 1)));

        assertSame(player, result);
        assertEquals(700.0, player.getStats().getMoney());
        assertEquals(2, player.getEquipments().size());
        assertEquals(1300.0, player.getStats().getTotalEquipmentValue());
        verify(playerRepository).save(player);
    }

    @Test
    @DisplayName("Achat à coût exact égal au solde est accepté")
    void shouldAcceptPurchaseAtExactBalance() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(equipmentService.findByName("Pistolet")).thenReturn(Optional.of(equipment("Pistolet", 1000)));
        when(playerRepository.save(player)).thenReturn(player);

        playerService.buyEquipments(1L, List.of(item("Pistolet", 2)));

        assertEquals(0.0, player.getStats().getMoney());
    }

    @Test
    @DisplayName("Une seule cache avec de la place : l'achat y est stocké automatiquement")
    void shouldStorePurchaseInSingleWeaponCache() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(equipmentService.findByName("Pistolet")).thenReturn(Optional.of(equipment("Pistolet", 500)));
        when(playerRepository.save(player)).thenReturn(player);
        WeaponCache cache = cache(10L, 0);

        playerService.buyEquipments(1L, List.of(item("Pistolet", 2)));

        assertTrue(player.getEquipments().isEmpty());
        assertEquals(2, cache.getTotalStoredCount());
        assertEquals(1000.0, player.getStats().getTotalEquipmentValue());
    }

    @Test
    @DisplayName("Deux caches avec de la place et aucun choix : l'achat est refusé")
    void shouldRejectAmbiguousCacheChoice() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(equipmentService.findByName("Pistolet")).thenReturn(Optional.of(equipment("Pistolet", 500)));
        cache(10L, 0);
        cache(11L, 0);

        assertThrows(IllegalArgumentException.class,
                () -> playerService.buyEquipments(1L, List.of(item("Pistolet", 1))));

        assertEquals(2000.0, player.getStats().getMoney());
        assertTrue(player.getEquipments().isEmpty());
        verify(playerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cache pleine : l'achat est refusé avant tout débit")
    void shouldRejectPurchaseWhenOnlyCacheIsFull() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(equipmentService.findByName("Pistolet")).thenReturn(Optional.of(equipment("Pistolet", 500)));
        cache(10L, 300);

        assertThrows(IllegalStateException.class,
                () -> playerService.buyEquipments(1L, List.of(item("Pistolet", 1))));

        assertEquals(2000.0, player.getStats().getMoney());
        verify(playerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Une cache pleine et une libre : la libre est choisie automatiquement")
    void shouldPickTheOnlyCacheWithSpace() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(equipmentService.findByName("Pistolet")).thenReturn(Optional.of(equipment("Pistolet", 500)));
        when(playerRepository.save(player)).thenReturn(player);
        cache(10L, 300);
        WeaponCache free = cache(11L, 0);

        playerService.buyEquipments(1L, List.of(item("Pistolet", 1)));

        assertEquals(1, free.getTotalStoredCount());
        assertTrue(player.getEquipments().isEmpty());
    }

    @Test
    @DisplayName("Cache explicite : l'achat est stocké dans la cache demandée")
    void shouldStorePurchaseInRequestedCache() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(equipmentService.findByName("Pistolet")).thenReturn(Optional.of(equipment("Pistolet", 500)));
        when(playerRepository.save(player)).thenReturn(player);
        WeaponCache first = cache(10L, 0);
        WeaponCache second = cache(11L, 0);

        playerService.buyEquipments(1L, List.of(item("Pistolet", 2, 11L)));

        assertEquals(0, first.getTotalStoredCount());
        assertEquals(2, second.getTotalStoredCount());
    }

    @Test
    @DisplayName("Panier dépassant la capacité de la cache : refusé avant tout débit")
    void shouldRejectCartOverCacheCapacity() {
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(equipmentService.findByName("Pistolet")).thenReturn(Optional.of(equipment("Pistolet", 500)));
        cache(10L, 299);

        assertThrows(IllegalStateException.class,
                () -> playerService.buyEquipments(1L, List.of(item("Pistolet", 1), item("Pistolet", 1))));

        assertEquals(2000.0, player.getStats().getMoney());
        verify(playerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Verrou joueur : l'inventaire hérité est rangé dans la cache")
    void shouldMoveLegacyInventoryOnLock() {
        player.addEquipmentToStack(equipment("Pistolet", 500), 1);
        WeaponCache cache = cache(10L, 0);
        when(playerRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(player));

        assertSame(player, playerService.findByUserIdForUpdate(10L));

        assertTrue(player.getEquipments().isEmpty());
        assertEquals(1, cache.getTotalStoredCount());
    }

    @Test
    @DisplayName("Un achat range aussi l'inventaire hérité dans la cache")
    void shouldMoveLegacyInventoryBeforePurchase() {
        player.addEquipmentToStack(equipment("Fusil", 300), 1);
        WeaponCache cache = cache(10L, 0);
        when(playerRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(player));
        when(equipmentService.findByName("Pistolet")).thenReturn(Optional.of(equipment("Pistolet", 500)));
        when(playerRepository.save(player)).thenReturn(player);

        playerService.buyEquipments(1L, List.of(item("Pistolet", 1)));

        assertTrue(player.getEquipments().isEmpty());
        assertEquals(2, cache.getTotalStoredCount());
    }
}
