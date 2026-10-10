package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.model.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("GlobalExceptionHandler — verrou optimiste")
class GlobalExceptionHandlerTest {

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException → 409 ProblemDetail")
    void optimisticLockingMapsTo409() {
        var response = new GlobalExceptionHandler()
                .handleOptimisticLock(new ObjectOptimisticLockingFailureException(Player.class, 1L));

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Conflit de version", response.getBody().getTitle());
    }
}
