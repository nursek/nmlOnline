package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@DisplayName("TurnService — résolution + incrément du tour")
class TurnServiceTest {

    @Autowired
    private TurnService turnService;

    @Autowired
    private MovementOrderRepository movementOrderRepository;

    @Test
    @DisplayName("advanceTurn résout les ordres PENDING puis incrémente currentTurn")
    void advanceTurnResolvesPendingAndIncrements() {
        int initialTurn = turnService.getCurrentTurn();
        assertTrue(initialTurn >= 1, "Le tour courant initial doit être >= 1");

        MovementOrder order = MovementOrder.createFootOrder(
                99L, initialTurn, List.of(999L), List.of(2, 10));
        order = movementOrderRepository.save(order);
        assertEquals(MovementStatus.PENDING, order.getStatus());

        int newTurn = turnService.advanceTurn();

        assertEquals(initialTurn + 1, newTurn, "advanceTurn doit incrémenter le tour");
        assertEquals(newTurn, turnService.getCurrentTurn(), "Le tour courant doit être persisted après advanceTurn");

        MovementOrder reloaded = movementOrderRepository.findById(order.getId()).orElseThrow();
        assertNotEquals(MovementStatus.PENDING, reloaded.getStatus(),
                "advanceTurn doit résoudre les ordres PENDING du tour qui se termine");

        movementOrderRepository.deleteById(order.getId());
    }

    @Test
    @DisplayName("advanceTurn rejette un 2e appel concurrent (garde anti double-clic)")
    void advanceTurnRejectsConcurrentCall() throws Exception {
        int initialTurn = turnService.getCurrentTurn();
        MovementOrder order = MovementOrder.createFootOrder(
                99L, initialTurn, List.of(999L), List.of(2, 10));
        order = movementOrderRepository.save(order);

        int attempts = 12;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService race = Executors.newFixedThreadPool(attempts);
        AtomicInteger wins = new AtomicInteger(0);
        AtomicInteger rejects = new AtomicInteger(0);
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        for (int i = 0; i < attempts; i++) {
            race.submit(() -> {
                try {
                    start.await();
                    try {
                        turnService.advanceTurn();
                        wins.incrementAndGet();
                    } catch (IllegalStateException e) {
                        rejects.incrementAndGet();
                    }
                } catch (Throwable e) {
                    unexpected.compareAndSet(null, e);
                }
            });
        }
        start.countDown();
        race.shutdown();
        assertTrue(race.awaitTermination(60, TimeUnit.SECONDS), "Les threads doivent terminer");

        assertNull(unexpected.get(), "Aucune erreur inattendue pendant la course");
        assertTrue(wins.get() >= 1, "Au moins un appel doit réussir à incrémenter le tour");
        assertTrue(rejects.get() >= 1, "Au moins un appel concurrent doit être rejeté (409)");

        movementOrderRepository.deleteById(order.getId());
    }
}
