package com.mg.nmlonline.domain.model.building;

import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.equipment.EquipmentCategory;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.resource.PlayerResource;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de régression sur les règles métier des bâtiments :
 * stats par type, cooldowns de déplacement, stockage de fortune,
 * capture, destruction et vampirisation de la Banque.
 */
@DisplayName("Bâtiments — Règles métier")
class BuildingRulesTest {

    @Nested
    @DisplayName("Stats et destruction")
    class StatsTests {

        @Test
        @DisplayName("Stats de base : QG 100/200, Cache 100/100, Banque 50/50")
        void shouldPinBaseStats() {
            Headquarters hq = new Headquarters(1L);
            WeaponCache cache = new WeaponCache(1L);
            Bank bank = new Bank(1L);

            assertEquals(100.0, hq.getAttack());
            assertEquals(200.0, hq.getDefense());
            assertEquals(100.0, cache.getAttack());
            assertEquals(100.0, cache.getDefense());
            assertEquals(50.0, bank.getAttack());
            assertEquals(50.0, bank.getDefense());
        }

        @Test
        @DisplayName("Bâtiment détruit : stats à 0")
        void shouldZeroStatsWhenDestroyed() {
            WeaponCache cache = new WeaponCache(1L);
            cache.setDestroyed(true);
            cache.recalculateBaseStats();

            assertEquals(0.0, cache.getAttack());
            assertEquals(0.0, cache.getDefense());
        }
    }

    @Nested
    @DisplayName("Quartier Général")
    class HeadquartersTests {

        @Test
        @DisplayName("Déplaçable si jamais déplacé")
        void shouldMoveWhenNeverMoved() {
            assertTrue(new Headquarters(1L).canMove(1));
        }

        @Test
        @DisplayName("Cooldown de 5 tours entre deux déplacements")
        void shouldEnforceFiveTurnCooldown() {
            Headquarters hq = new Headquarters(1L);
            hq.setLastMovedTurn(3);

            assertFalse(hq.canMove(7));  // 7-3 = 4 < 5
            assertTrue(hq.canMove(8));   // 8-3 = 5
            assertTrue(hq.canMove(20));
        }

        @Test
        @DisplayName("Non opérationnel ou détruit : immobile")
        void shouldNotMoveWhenDestroyedOrNonOperational() {
            Headquarters hq = new Headquarters(1L);
            hq.setOperational(false);
            assertFalse(hq.canMove(10));

            Headquarters destroyed = new Headquarters(1L);
            destroyed.destroy();
            assertFalse(destroyed.canMove(10));
        }

        @Test
        @DisplayName("Stocke 25% de la fortune")
        void shouldStoreQuarterOfWealth() {
            assertEquals(2500.0, new Headquarters(1L).getStoredWealth(10000.0));
        }

        @Test
        @DisplayName("Destruction sans capture immobilise l'armée, avec capture non")
        void shouldPinArmyImmobilizationRule() {
            Headquarters hq = new Headquarters(1L);
            hq.destroy();

            assertTrue(hq.isArmyImmobilized());
            assertFalse(hq.isOperational());

            hq.onCapture(2L, 5);
            assertFalse(hq.isArmyImmobilized());
        }

        @Test
        @DisplayName("Reconstruction restaure 100/200 et l'état opérationnel")
        void shouldRestoreStatsOnReconstruction() {
            Headquarters hq = new Headquarters(1L);
            hq.destroy();

            hq.reconstructSameLocation();

            assertFalse(hq.isDestroyed());
            assertTrue(hq.isOperational());
            assertEquals(100.0, hq.getAttack());
            assertEquals(200.0, hq.getDefense());
        }
    }

    @Nested
    @DisplayName("Banque")
    class BankTests {

        @Test
        @DisplayName("Déplaçable uniquement à partir du tour 5")
        void shouldMoveOnlyFromTurnFive() {
            Bank bank = new Bank(1L);

            assertFalse(bank.canMove(4));
            assertTrue(bank.canMove(5));
        }

        @Test
        @DisplayName("Déplaçable une seule fois")
        void shouldMoveOnlyOnce() {
            Bank bank = new Bank(1L);
            bank.recordMove(5);

            assertTrue(bank.isHasMoved());
            assertFalse(bank.canMove(6));
        }

        @Test
        @DisplayName("Détruite : immobile")
        void shouldNotMoveWhenDestroyed() {
            Bank bank = new Bank(1L);
            bank.setDestroyed(true);

            assertFalse(bank.canMove(10));
        }

        @Test
        @DisplayName("Stocke 75% de la fortune")
        void shouldStoreThreeQuartersOfWealth() {
            Bank bank = new Bank(1L);
            bank.updateStoredMoney(10000.0);

            assertEquals(7500.0, bank.getStoredMoney());
        }

        @Test
        @DisplayName("Transfert vide la banque")
        void shouldEmptyBankOnTransfer() {
            Bank bank = new Bank(1L);
            bank.setStoredMoney(5000.0);
            bank.getStoredResources().add(new PlayerResource("Or", 100));

            assertEquals(5000.0, bank.transferMoney());
            assertEquals(1, bank.transferResources().size());

            assertEquals(0.0, bank.getStoredMoney());
            assertTrue(bank.getStoredResources().isEmpty());
        }

        @Test
        @DisplayName("Vampirisation : 0% si non capturée")
        void shouldNotVampirizeWhenNotCaptured() {
            assertEquals(0.0, new Bank(1L).getVampirizeRate(10));
        }

        @Test
        @DisplayName("Vampirisation : 15% + 10% par tour depuis la capture")
        void shouldPinVampirizeProgression() {
            Bank bank = new Bank(1L);
            bank.onCapture(2L, 3);

            assertEquals(0.15, bank.getVampirizeRate(3), 1e-9);
            assertEquals(0.25, bank.getVampirizeRate(4), 1e-9);
            assertEquals(0.55, bank.getVampirizeRate(7), 1e-9);
        }

        @Test
        @DisplayName("Vampirisation plafonnée à 75%")
        void shouldCapVampirizeRateAtSeventyFivePercent() {
            Bank bank = new Bank(1L);
            bank.onCapture(2L, 3);

            assertEquals(0.75, bank.getVampirizeRate(9), 1e-9);  // 15 + 6×10
            assertEquals(0.75, bank.getVampirizeRate(100), 1e-9);
        }

        @Test
        @DisplayName("Montant vampirisé = revenu × taux")
        void shouldComputeVampirizedAmount() {
            Bank bank = new Bank(1L);
            bank.onCapture(2L, 3);

            assertEquals(1500.0, bank.calculateVampirizedAmount(10000.0, 3), 1e-9);
        }
    }

    @Nested
    @DisplayName("Cache d'armes")
    class WeaponCacheTests {

        private EquipmentStack stackOf(String name, int quantity) {
            Equipment eq = new Equipment(name, 100, 0, 0, 0, 0,
                    Set.of(UnitClass.TIREUR), EquipmentCategory.FIREARM);
            EquipmentStack stack = new EquipmentStack(eq);
            for (int i = 1; i < quantity; i++) {
                stack.increment();
            }
            return stack;
        }

        @Test
        @DisplayName("Déplaçable tous les tours (cooldown 0)")
        void shouldMoveEveryTurn() {
            WeaponCache cache = new WeaponCache(1L);

            assertTrue(cache.canMove(1));
            cache.recordMove(1);
            assertTrue(cache.canMove(2));
        }

        @Test
        @DisplayName("Détruite : immobile")
        void shouldNotMoveWhenDestroyed() {
            WeaponCache cache = new WeaponCache(1L);
            cache.setDestroyed(true);

            assertFalse(cache.canMove(10));
        }

        @Test
        @DisplayName("Capacité maximale de 300 équipements")
        void shouldEnforceMaxCapacity() {
            WeaponCache cache = new WeaponCache(1L);
            cache.getStoredEquipments().add(stackOf("Pistolet", 299));

            assertEquals(299, cache.getTotalStoredCount());
            assertTrue(cache.hasCapacity(1));
            assertFalse(cache.hasCapacity(2));
            assertEquals(1, cache.getAvailableCapacity());
        }

        @Test
        @DisplayName("discardEquipment décrémente et supprime le stack vide")
        void shouldDiscardEquipment() {
            WeaponCache cache = new WeaponCache(1L);
            cache.getStoredEquipments().add(stackOf("Pistolet", 3));

            assertTrue(cache.discardEquipment("Pistolet", 2));
            assertEquals(1, cache.getTotalStoredCount());

            assertTrue(cache.discardEquipment("Pistolet", 1));
            assertTrue(cache.getStoredEquipments().isEmpty());
        }

        @Test
        @DisplayName("discardEquipment refuse si quantité insuffisante ou nom inconnu")
        void shouldRefuseInvalidDiscard() {
            WeaponCache cache = new WeaponCache(1L);
            cache.getStoredEquipments().add(stackOf("Pistolet", 2));

            assertFalse(cache.discardEquipment("Pistolet", 3));
            assertFalse(cache.discardEquipment("Fusil", 1));
            assertEquals(2, cache.getTotalStoredCount());
        }

        @Test
        @DisplayName("Capture : transfert de tous les équipements et vidage")
        void shouldTransferAllEquipmentsOnCapture() {
            WeaponCache cache = new WeaponCache(1L);
            cache.getStoredEquipments().add(stackOf("Pistolet", 5));

            assertEquals(1, cache.transferAllEquipments().size());
            assertTrue(cache.getStoredEquipments().isEmpty());
        }
    }

    @Nested
    @DisplayName("Capture générique")
    class CaptureTests {

        @Test
        @DisplayName("onCapture enregistre le conquérant et le tour, reclaim annule")
        void shouldRecordCaptureAndReclaim() {
            WeaponCache cache = new WeaponCache(1L);

            cache.onCapture(2L, 7);

            assertTrue(cache.isCaptured());
            assertEquals(2L, cache.getCapturedByPlayerId());
            assertEquals(7, cache.getCapturedTurn());

            cache.reclaim();

            assertFalse(cache.isCaptured());
            assertNull(cache.getCapturedByPlayerId());
        }
    }
}
