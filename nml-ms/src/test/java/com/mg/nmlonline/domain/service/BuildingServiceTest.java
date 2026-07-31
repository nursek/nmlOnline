package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.BuildingType;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BuildingRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
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
 * Tests de régression sur BuildingService : reconstruction du QG (75 000),
 * captures (QG, cache, banque), vampirisation et déplacements.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BuildingService")
class BuildingServiceTest {

    @Mock
    BuildingRepository buildingRepository;

    @Mock
    PlayerRepository playerRepository;

    @Mock
    BoardService boardService;

    @Mock
    TurnService turnService;

    @InjectMocks
    BuildingService buildingService;

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer");
        player.setId(1L);
        player.getStats().setMoney(100000.0);
    }

    private void stubHeadquarters(Headquarters hq) {
        when(buildingRepository.findByPlayerIdAndBuildingType(1L, BuildingType.HEADQUARTERS))
                .thenReturn(Optional.of(hq));
    }

    @Nested
    @DisplayName("Reconstruction du QG (75 000)")
    class ReconstructHeadquartersTests {

        @Test
        @DisplayName("Succès : coût débité et QG restauré")
        void shouldReconstructForSeventyFiveThousand() {
            Headquarters hq = new Headquarters(1L);
            hq.destroy();
            stubHeadquarters(hq);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            boolean result = buildingService.reconstructHeadquartersSameLocation(1L);

            assertTrue(result);
            assertEquals(25000.0, player.getStats().getMoney());
            assertTrue(hq.isOperational());
            assertEquals(100.0, hq.getAttack());
            assertEquals(200.0, hq.getDefense());
            verify(playerRepository).save(player);
            verify(buildingRepository).save(hq);
        }

        @Test
        @DisplayName("Fonds insuffisants → false, sans mutation")
        void shouldRefuseWhenInsufficientFunds() {
            Headquarters hq = new Headquarters(1L);
            hq.destroy();
            stubHeadquarters(hq);
            player.getStats().setMoney(74999.0);
            when(playerRepository.findById(1L)).thenReturn(Optional.of(player));

            boolean result = buildingService.reconstructHeadquartersSameLocation(1L);

            assertFalse(result);
            assertEquals(74999.0, player.getStats().getMoney());
            assertTrue(hq.isDestroyed());
            verify(buildingRepository, never()).save(any(Building.class));
        }

        @Test
        @DisplayName("QG absent ou joueur introuvable → false")
        void shouldReturnFalseWhenHeadquartersOrPlayerMissing() {
            when(buildingRepository.findByPlayerIdAndBuildingType(1L, BuildingType.HEADQUARTERS))
                    .thenReturn(Optional.empty());

            assertFalse(buildingService.reconstructHeadquartersSameLocation(1L));
        }
    }

    @Nested
    @DisplayName("Capture du QG")
    class CaptureHeadquartersTests {

        @Test
        @DisplayName("Capture : QG + tous les bâtiments non détruits passent au conquérant")
        void shouldCaptureAllNonDestroyedBuildings() {
            Headquarters hq = new Headquarters(1L);
            stubHeadquarters(hq);
            WeaponCache cache = new WeaponCache(1L);
            Bank bank = new Bank(1L);
            when(buildingRepository.findByPlayerIdAndIsDestroyedFalse(1L))
                    .thenReturn(List.of(hq, cache, bank));

            buildingService.captureHeadquarters(1L, 2L, 7);

            for (Building b : List.of(hq, cache, bank)) {
                assertEquals(2L, b.getCapturedByPlayerId());
                assertEquals(7, b.getCapturedTurn());
            }
            verify(buildingRepository).saveAll(List.of(hq, cache, bank));
        }

        @Test
        @DisplayName("QG absent → rien ne se passe")
        void shouldDoNothingWhenHeadquartersMissing() {
            when(buildingRepository.findByPlayerIdAndBuildingType(1L, BuildingType.HEADQUARTERS))
                    .thenReturn(Optional.empty());

            buildingService.captureHeadquarters(1L, 2L, 7);

            verify(buildingRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Capture de banque")
    class CaptureBankTests {

        @Test
        @DisplayName("Succès : argent et ressources transférés au conquérant, vampirisation activée")
        void shouldTransferMoneyAndResourcesToCapturer() {
            Bank bank = new Bank(1L);
            bank.setStoredMoney(5000.0);
            bank.getStoredResources().add(new PlayerResource("Or", 100));
            when(buildingRepository.findById(50L)).thenReturn(Optional.of(bank));
            Player capturer = new Player("Capturer");
            capturer.setId(2L);
            capturer.getStats().setMoney(1000.0);
            when(playerRepository.findById(2L)).thenReturn(Optional.of(capturer));

            BuildingService.CaptureResult result = buildingService.captureBank(50L, 2L, 3);

            assertEquals(5000.0, result.money());
            assertEquals(1, result.resources().size());
            assertEquals(6000.0, capturer.getStats().getMoney());
            assertEquals(0.0, bank.getStoredMoney());
            assertTrue(bank.getStoredResources().isEmpty());
            assertTrue(bank.isCaptured());
            assertSame(capturer, result.resources().getFirst().getPlayer());
        }

        @Test
        @DisplayName("Conquérant introuvable → IllegalArgumentException")
        void shouldThrowWhenCapturerMissing() {
            Bank bank = new Bank(1L);
            when(buildingRepository.findById(50L)).thenReturn(Optional.of(bank));
            when(playerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> buildingService.captureBank(50L, 99L, 3));
        }

        @Test
        @DisplayName("Cible absente ou pas une banque → résultat vide")
        void shouldReturnEmptyResultWhenNotABank() {
            when(buildingRepository.findById(50L)).thenReturn(Optional.empty());
            when(buildingRepository.findById(51L)).thenReturn(Optional.of(new WeaponCache(1L)));

            assertEquals(0.0, buildingService.captureBank(50L, 2L, 3).money());
            assertEquals(0.0, buildingService.captureBank(51L, 2L, 3).money());
        }
    }

    @Nested
    @DisplayName("Vampirisation des revenus")
    class VampirizeTests {

        @Test
        @DisplayName("Pas de banque ou banque non capturée → 0")
        void shouldReturnZeroWhenBankNotCaptured() {
            when(buildingRepository.findByPlayerIdAndBuildingType(1L, BuildingType.BANK))
                    .thenReturn(Optional.empty());
            assertEquals(0.0, buildingService.calculateVampirizedIncome(1L, 10000.0, 5));

            Bank bank = new Bank(1L);
            when(buildingRepository.findByPlayerIdAndBuildingType(1L, BuildingType.BANK))
                    .thenReturn(Optional.of(bank));
            assertEquals(0.0, buildingService.calculateVampirizedIncome(1L, 10000.0, 5));
        }

        @Test
        @DisplayName("Banque capturée : revenu × taux progressif plafonné à 75%")
        void shouldComputeVampirizedIncome() {
            Bank bank = new Bank(1L);
            bank.onCapture(2L, 3);
            when(buildingRepository.findByPlayerIdAndBuildingType(1L, BuildingType.BANK))
                    .thenReturn(Optional.of(bank));

            assertEquals(1500.0, buildingService.calculateVampirizedIncome(1L, 10000.0, 3), 1e-9);
            assertEquals(3500.0, buildingService.calculateVampirizedIncome(1L, 10000.0, 5), 1e-9);
            assertEquals(7500.0, buildingService.calculateVampirizedIncome(1L, 10000.0, 50), 1e-9);
        }
    }

    @Nested
    @DisplayName("Déplacement de bâtiment")
    class MoveBuildingTests {

        @Test
        @DisplayName("Bâtiment introuvable → IllegalArgumentException")
        void shouldThrowWhenBuildingMissing() {
            when(buildingRepository.findById(50L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> buildingService.moveBuilding(50L, 100L, 3, 10));
        }

        @Test
        @DisplayName("Cooldown non respecté → IllegalStateException")
        void shouldThrowWhenOnCooldown() {
            Headquarters hq = new Headquarters(1L);
            hq.setLastMovedTurn(8); // tour 10 : 10-8 = 2 < 5
            when(buildingRepository.findById(50L)).thenReturn(Optional.of(hq));

            assertThrows(IllegalStateException.class,
                    () -> buildingService.moveBuilding(50L, 100L, 3, 10));
        }

        @Test
        @DisplayName("Secteur cible introuvable → IllegalArgumentException")
        void shouldThrowWhenSectorMissing() {
            WeaponCache cache = new WeaponCache(1L);
            when(buildingRepository.findById(50L)).thenReturn(Optional.of(cache));
            when(boardService.getSectorFromBoard(100L, 3)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> buildingService.moveBuilding(50L, 100L, 3, 10));
        }

        @Test
        @DisplayName("Secteur cible non possédé → IllegalStateException")
        void shouldThrowWhenSectorNotOwned() {
            WeaponCache cache = new WeaponCache(1L);
            when(buildingRepository.findById(50L)).thenReturn(Optional.of(cache));
            Sector sector = new Sector(3, "Secteur 3");
            sector.setOwnerId(99L);
            when(boardService.getSectorFromBoard(100L, 3)).thenReturn(Optional.of(sector));

            assertThrows(IllegalStateException.class,
                    () -> buildingService.moveBuilding(50L, 100L, 3, 10));
        }

        @Test
        @DisplayName("Déplacement réussi : secteur assigné et mouvement enregistré")
        void shouldMoveBuildingToOwnedSector() {
            WeaponCache cache = new WeaponCache(1L);
            when(buildingRepository.findById(50L)).thenReturn(Optional.of(cache));
            Sector sector = new Sector(3, "Secteur 3");
            sector.setOwnerId(1L);
            when(boardService.getSectorFromBoard(100L, 3)).thenReturn(Optional.of(sector));

            boolean result = buildingService.moveBuilding(50L, 100L, 3, 10);

            assertTrue(result);
            assertSame(sector, cache.getSector());
            assertEquals(10, cache.getLastMovedTurn());
            verify(buildingRepository).save(cache);
        }

        @Test
        @DisplayName("Déplacement de la banque la marque comme déplacée")
        void shouldMarkBankAsMoved() {
            Bank bank = new Bank(1L);
            when(buildingRepository.findById(50L)).thenReturn(Optional.of(bank));
            Sector sector = new Sector(3, "Secteur 3");
            sector.setOwnerId(1L);
            when(boardService.getSectorFromBoard(100L, 3)).thenReturn(Optional.of(sector));

            buildingService.moveBuilding(50L, 100L, 3, 5);

            assertTrue(bank.isHasMoved());
            assertFalse(bank.canMove(6));
        }
    }

    @Nested
    @DisplayName("Tour courant")
    class CurrentTurnTests {

        @Test
        @DisplayName("getCurrentTurn délègue à TurnService (source unique de vérité)")
        void shouldDelegateToTurnService() {
            when(turnService.getCurrentTurn()).thenReturn(7);
            assertEquals(7, buildingService.getCurrentTurn(1L));
        }
    }
}
