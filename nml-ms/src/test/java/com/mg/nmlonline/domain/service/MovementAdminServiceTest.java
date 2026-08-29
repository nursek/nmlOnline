package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.AdminMovementOrderDto;
import com.mg.nmlonline.api.dto.MovementResolutionResultDto;
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

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MovementAdminService — aperçu (dry-run) vs application")
class MovementAdminServiceTest {

    @Autowired
    private MovementAdminService movementAdminService;

    @Autowired
    private MovementOrderRepository movementOrderRepository;

    @Autowired
    private TurnService turnService;

    // destination inexistante (99999) pour bloquer l'ordre de façon déterministe.
    @Test
    @DisplayName("preview ne persiste pas, resolve mute le statut de l'ordre")
    void previewDoesNotPersistButResolveDoes() {
        int turn = turnService.getCurrentTurn();

        MovementOrder order = MovementOrder.createFootOrder(
                99L, turn, List.of(999L), List.of(2, 99999));
        MovementOrder saved = movementOrderRepository.save(order);
        final Long orderId = saved.getId();
        assertEquals(MovementStatus.PENDING, saved.getStatus());

        try {
            MovementResolutionResultDto preview = movementAdminService.previewMovements(turn);

            // REQUIRES_NEW roulé en arrière : l'ordre reste PENDING.
            MovementOrder afterPreview = movementOrderRepository.findById(orderId).orElseThrow();
            assertEquals(MovementStatus.PENDING, afterPreview.getStatus(),
                    "L'aperçu ne doit pas modifier le statut de l'ordre en base");

            assertTrue(
                    preview.getBlocked().stream().anyMatch(o -> orderId.equals(o.getId())),
                    "L'aperçu doit lister l'ordre bloqué dans le compte-rendu");

            MovementResolutionResultDto resolved = movementAdminService.resolveMovements(turn);

            MovementOrder afterResolve = movementOrderRepository.findById(orderId).orElseThrow();
            assertNotEquals(MovementStatus.PENDING, afterResolve.getStatus(),
                    "L'application doit marquer l'ordre comme RESOLVED/BLOCKED en base");
            assertEquals(MovementStatus.BLOCKED, afterResolve.getStatus(),
                    "L'ordre à destination inexistante doit être BLOCKED après application");

            assertTrue(
                    resolved.getBlocked().stream().anyMatch(o -> orderId.equals(o.getId())),
                    "L'application doit lister l'ordre bloqué dans le compte-rendu");
        } finally {
            movementOrderRepository.deleteById(orderId);
        }
    }

    @Test
    @DisplayName("getOrdersForTurn enrichit chaque ordre du nom du joueur")
    void getOrdersForTurnResolvesPlayerName() {
        int turn = turnService.getCurrentTurn();
        MovementOrder order = MovementOrder.createFootOrder(
                99L, turn, List.of(999L), List.of(2, 99999));
        MovementOrder saved = movementOrderRepository.save(order);
        final Long orderId = saved.getId();

        try {
            List<AdminMovementOrderDto> dtos = movementAdminService.getOrdersForTurn(turn, null);

            AdminMovementOrderDto mine = dtos.stream()
                    .filter(d -> orderId.equals(d.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Ordre créé absent de la liste admin"));

            // playerId 99L absent du seed : nom null, champ présent (pas l'ID brut).
            assertNotNull(mine.getPlayerId());
            assertEquals(99L, mine.getPlayerId());
        } finally {
            movementOrderRepository.deleteById(orderId);
        }
    }
}