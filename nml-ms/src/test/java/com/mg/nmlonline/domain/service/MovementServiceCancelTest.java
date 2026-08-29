package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("MovementService — cancelOrderOrThrow (404/403/409)")
class MovementServiceCancelTest {

    @Mock
    MovementOrderRepository orderRepository;

    @Mock
    VehicleRepository vehicleRepository;

    @InjectMocks
    MovementService service;

    private static final Long OWNER = 10L;
    private static final Long INTRUS = 99L;
    private static final Long ORDER_ID = 777L;

    private MovementOrder pendingOrder(Long playerId) {
        return MovementOrder.createFootOrder(playerId, 1, List.of(1L), List.of(2, 3));
    }

    @BeforeEach
    void seedId() {
    }

    @Test
    @DisplayName("Ordre introuvable → EntityNotFoundException (404)")
    void shouldThrowEntityNotFoundWhenOrderMissing() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(
                EntityNotFoundException.class,
                () -> service.cancelOrderOrThrow(OWNER, ORDER_ID));
        assertTrue(ex.getMessage().contains(String.valueOf(ORDER_ID)));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ordre d'un autre joueur → SecurityException (403)")
    void shouldThrowSecurityWhenNotOwner() {
        MovementOrder order = pendingOrder(INTRUS);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        SecurityException ex = assertThrows(
                SecurityException.class,
                () -> service.cancelOrderOrThrow(OWNER, ORDER_ID));
        assertTrue(ex.getMessage().contains(String.valueOf(ORDER_ID)));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ordre déjà résolu (non PENDING) → IllegalStateException (409)")
    void shouldThrowIllegalStateWhenNotPending() {
        MovementOrder order = pendingOrder(OWNER);
        order.setStatus(MovementStatus.RESOLVED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.cancelOrderOrThrow(OWNER, ORDER_ID));
        assertTrue(ex.getMessage().contains(String.valueOf(ORDER_ID)));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ordre PENDING du bon joueur → annulé et persisté (204)")
    void shouldCancelPendingOrderOfOwner() {
        MovementOrder order = pendingOrder(OWNER);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        service.cancelOrderOrThrow(OWNER, ORDER_ID);

        assertEquals(MovementStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }
}