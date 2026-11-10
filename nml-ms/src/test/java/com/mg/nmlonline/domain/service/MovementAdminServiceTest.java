package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.api.dto.AdminMovementOrderDto;
import com.mg.nmlonline.api.dto.MovementResolutionResultDto;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EmbeddedPostgresTest
@DisplayName("MovementAdminService — aperçu (dry-run) vs application")
class MovementAdminServiceTest {

    @Autowired
    private MovementAdminService movementAdminService;

    @Autowired
    private MovementOrderRepository movementOrderRepository;

    @Autowired
    private TurnService turnService;

    @Autowired
    private PlayerRepository playerRepository;

    private Long boardId() {
        return playerRepository.findAll().getFirst().getBoardId();
    }

    // destination inexistante (99999) pour bloquer l'ordre de façon déterministe.
    @Test
    @DisplayName("preview ne persiste pas, resolve mute le statut de l'ordre")
    void previewDoesNotPersistButResolveDoes() {
        int turn = turnService.getCurrentTurn(boardId());

        MovementOrder order = MovementOrder.createFootOrder(
                99L, boardId(), turn, List.of(999L), List.of(2, 99999));
        MovementOrder saved = movementOrderRepository.save(order);
        final Long orderId = saved.getId();
        assertEquals(MovementStatus.PENDING, saved.getStatus());

        try {
            MovementResolutionResultDto preview = movementAdminService.previewMovements(boardId(), turn);

            // REQUIRES_NEW roulé en arrière : l'ordre reste PENDING.
            MovementOrder afterPreview = movementOrderRepository.findById(orderId).orElseThrow();
            assertEquals(MovementStatus.PENDING, afterPreview.getStatus(),
                    "L'aperçu ne doit pas modifier le statut de l'ordre en base");

            assertTrue(
                    preview.getBlocked().stream().anyMatch(o -> orderId.equals(o.getId())),
                    "L'aperçu doit lister l'ordre bloqué dans le compte-rendu");

            movementOrderRepository.deleteById(orderId);
        } finally {
            movementOrderRepository.deleteById(orderId);
        }
    }

    @Test
    @DisplayName("getOrdersForTurn enrichit chaque ordre du nom du joueur")
    void getOrdersForTurnResolvesPlayerName() {
        int turn = turnService.getCurrentTurn(boardId());
        Player player = playerRepository.findByBoardIdAndName(playerRepository.findAll().getFirst().getBoardId(), "nursek").orElseThrow();
        MovementOrder order = MovementOrder.createFootOrder(
                player.getId(), boardId(), turn, List.of(999L), List.of(2, 99999));
        MovementOrder saved = movementOrderRepository.save(order);
        final Long orderId = saved.getId();

        try {
            List<AdminMovementOrderDto> dtos = movementAdminService.getOrdersForTurn(boardId(), turn, null);

            AdminMovementOrderDto mine = dtos.stream()
                    .filter(d -> orderId.equals(d.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Ordre créé absent de la liste admin"));

            assertEquals(player.getId(), mine.getPlayerId());
            assertEquals("nursek", mine.getPlayerName());
        } finally {
            movementOrderRepository.deleteById(orderId);
        }
    }
}
