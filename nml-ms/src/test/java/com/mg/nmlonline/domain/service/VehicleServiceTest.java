package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BuyVehicleRequestDto;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de régression sur VehicleService : validation des achats,
 * atomicité du batch, règles de placement sur secteur.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleService")
class VehicleServiceTest {

    @Mock
    PlayerRepository playerRepository;

    @Mock
    VehicleRepository vehicleRepository;

    @Mock
    SectorRepository sectorRepository;

    @InjectMocks
    VehicleService vehicleService;

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer");
        player.setId(1L);
        player.setUserId(10L);
        player.getStats().setMoney(10000.0);
    }

    private BuyVehicleRequestDto item(String type, int quantity) {
        BuyVehicleRequestDto dto = new BuyVehicleRequestDto();
        dto.setVehicleType(type);
        dto.setQuantity(quantity);
        return dto;
    }

    @Nested
    @DisplayName("Achat unitaire")
    class BuyVehicleTests {

        @Test
        @DisplayName("Type null, vide ou invalide est rejeté")
        void shouldRejectInvalidType() {
            assertThrows(IllegalArgumentException.class,
                    () -> vehicleService.buyVehicle(10L, null, 1));
            assertThrows(IllegalArgumentException.class,
                    () -> vehicleService.buyVehicle(10L, "  ", 1));
            assertThrows(IllegalArgumentException.class,
                    () -> vehicleService.buyVehicle(10L, "SOUS_MARIN", 1));
        }

        @Test
        @DisplayName("Quantité < 1 est rejetée")
        void shouldRejectQuantityBelowOne() {
            assertThrows(IllegalArgumentException.class,
                    () -> vehicleService.buyVehicle(10L, "TANK", 0));
        }

        @Test
        @DisplayName("Joueur introuvable est rejeté")
        void shouldRejectUnknownPlayer() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> vehicleService.buyVehicle(10L, "TANK", 1));
        }

        @Test
        @DisplayName("Fonds insuffisants lèvent InsufficientFundsException")
        void shouldThrowWhenInsufficientFunds() {
            player.getStats().setMoney(5000.0); // TANK = 7 500
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));

            assertThrows(InsufficientFundsException.class,
                    () -> vehicleService.buyVehicle(10L, "TANK", 1));
        }

        @Test
        @DisplayName("Achat réussi crée les véhicules et débite le coût")
        void shouldCreateVehiclesAndDebitCost() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Vehicle> created = vehicleService.buyVehicle(10L, "VTT_LEGER", 2); // 2 × 4 000

            assertEquals(2, created.size());
            assertEquals(2000.0, player.getStats().getMoney());
            assertEquals(8000.0, player.getStats().getTotalVehiclesValue());
            verify(vehicleRepository, times(2)).save(any(Vehicle.class));
            verify(playerRepository).save(player);
        }

        @Test
        @DisplayName("Achat partiel : le débit est séquentiel (pas de pré-validation du total)")
        void shouldPinSequentialDebitBehavior() {
            // comportement actuel piné : buyVehicle débite véhicule par véhicule.
            // En test unitaire (pas de transaction), l'argent reste débité après l'exception ;
            // en production c'est le rollback @Transactional qui annule tout.
            player.getStats().setMoney(9000.0); // 2 VTT_LEGER (8000) OK, le 3e échoue
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThrows(InsufficientFundsException.class,
                    () -> vehicleService.buyVehicle(10L, "VTT_LEGER", 3));

            assertEquals(1000.0, player.getStats().getMoney());
        }
    }

    @Nested
    @DisplayName("Achat en lot (atomique)")
    class BuyVehiclesBatchTests {

        @Test
        @DisplayName("Panier vide est rejeté")
        void shouldRejectEmptyCart() {
            assertThrows(IllegalArgumentException.class,
                    () -> vehicleService.buyVehiclesBatch(10L, List.of()));
            assertThrows(IllegalArgumentException.class,
                    () -> vehicleService.buyVehiclesBatch(10L, null));
        }

        @Test
        @DisplayName("Coût total validé AVANT tout débit")
        void shouldValidateTotalCostBeforeAnyDebit() {
            player.getStats().setMoney(9000.0); // TANK(7500) + VTT_LEGER(4000) = 11500 > 9000
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));

            assertThrows(InsufficientFundsException.class,
                    () -> vehicleService.buyVehiclesBatch(10L,
                            List.of(item("TANK", 1), item("VTT_LEGER", 1))));

            assertEquals(9000.0, player.getStats().getMoney());
            assertEquals(0.0, player.getStats().getTotalVehiclesValue());
            verify(vehicleRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ligne invalide dans le lot est rejetée")
        void shouldRejectInvalidLineInBatch() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));

            assertThrows(IllegalArgumentException.class,
                    () -> vehicleService.buyVehiclesBatch(10L, List.of(item("TANK", 0))));
            assertThrows(IllegalArgumentException.class,
                    () -> vehicleService.buyVehiclesBatch(10L, List.of(item(" ", 1))));
            assertThrows(IllegalArgumentException.class,
                    () -> vehicleService.buyVehiclesBatch(10L, List.of(item("MOTO", 1))));
        }

        @Test
        @DisplayName("Lot réussi crée tous les véhicules")
        void shouldCreateAllVehiclesInBatch() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

            List<Vehicle> created = vehicleService.buyVehiclesBatch(10L,
                    List.of(item("TOURELLE", 2), item("VTT_LEGER", 1))); // 2600 + 4000 = 6600

            assertEquals(3, created.size());
            assertEquals(3400.0, player.getStats().getMoney());
            assertEquals(6600.0, player.getStats().getTotalVehiclesValue());
        }
    }

    @Nested
    @DisplayName("Placement sur secteur")
    class PlaceVehicleTests {

        private Vehicle ownedVehicle() {
            return new Vehicle(VehicleType.VTT_LEGER, 1L);
        }

        @Test
        @DisplayName("Véhicule d'un autre joueur est rejeté")
        void shouldRejectVehicleOwnedByAnotherPlayer() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            Vehicle foreign = new Vehicle(VehicleType.TANK, 99L);
            when(vehicleRepository.findById(5L)).thenReturn(Optional.of(foreign));

            assertThrows(SecurityException.class,
                    () -> vehicleService.placeVehicle(5L, 100L, 3, 10L));
        }

        @Test
        @DisplayName("Secteur introuvable est rejeté")
        void shouldRejectUnknownSector() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(vehicleRepository.findById(5L)).thenReturn(Optional.of(ownedVehicle()));
            when(sectorRepository.findByBoard_IdAndNumber(100L, 3)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> vehicleService.placeVehicle(5L, 100L, 3, 10L));
        }

        @Test
        @DisplayName("Secteur non possédé est rejeté")
        void shouldRejectSectorNotOwned() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            when(vehicleRepository.findById(5L)).thenReturn(Optional.of(ownedVehicle()));
            Sector sector = new Sector(3, "Secteur 3");
            sector.setOwnerId(99L);
            when(sectorRepository.findByBoard_IdAndNumber(100L, 3)).thenReturn(Optional.of(sector));

            assertThrows(SecurityException.class,
                    () -> vehicleService.placeVehicle(5L, 100L, 3, 10L));
        }

        @Test
        @DisplayName("Placement réussi sur secteur possédé")
        void shouldPlaceVehicleOnOwnedSector() {
            when(playerRepository.findByUserId(10L)).thenReturn(Optional.of(player));
            Vehicle vehicle = ownedVehicle();
            when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));
            Sector sector = new Sector(3, "Secteur 3");
            sector.setOwnerId(1L);
            when(sectorRepository.findByBoard_IdAndNumber(100L, 3)).thenReturn(Optional.of(sector));
            when(vehicleRepository.save(vehicle)).thenReturn(vehicle);

            Vehicle result = vehicleService.placeVehicle(5L, 100L, 3, 10L);

            assertSame(sector, result.getSector());
        }
    }
}
