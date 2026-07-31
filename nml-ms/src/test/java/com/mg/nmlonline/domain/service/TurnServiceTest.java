package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration de {@link TurnService} : la source unique de vérité du
 * tour courant. {@code advanceTurn} doit (1) résoudre les ordres de déplacement
 * PENDING du tour qui se termine, puis (2) incrémenter le compteur persisted.
 *
 * <p>Données fournies par {@code PlayerStartupImporter} (nursek/lurio) au
 * démarrage du profil {@code test}.
 */
@SpringBootTest
@ActiveProfiles("test")
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

        // Un ordre PENDING factice pour le tour courant : entité inexistante,
        // donc resolveAllMovements le marquera BLOCKED (et non plus PENDING).
        MovementOrder order = MovementOrder.createFootOrder(
                99L, initialTurn, List.of(999L), List.of(2, 10));
        order = movementOrderRepository.save(order);
        assertEquals(MovementStatus.PENDING, order.getStatus());

        int newTurn = turnService.advanceTurn();

        assertEquals(initialTurn + 1, newTurn, "advanceTurn doit incrémenter le tour");
        assertEquals(newTurn, turnService.getCurrentTurn(), "Le tour courant doit être persisted après advanceTurn");

        // L'ordre PENDING du tour précédent n'est plus PENDING après résolution.
        MovementOrder reloaded = movementOrderRepository.findById(order.getId()).orElseThrow();
        assertNotEquals(MovementStatus.PENDING, reloaded.getStatus(),
                "advanceTurn doit résoudre les ordres PENDING du tour qui se termine");

        movementOrderRepository.deleteById(order.getId());
    }
}