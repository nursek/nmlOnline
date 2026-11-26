package com.mg.nmlonline.config;

import com.mg.nmlonline.domain.exception.HarvestClosedException;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import com.mg.nmlonline.domain.exception.PlayerActionUndoException;
import com.mg.nmlonline.domain.model.player.Player;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("GlobalExceptionHandler — mappings HTTP")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("ObjectOptimisticLockingFailureException → 409 ProblemDetail")
    void optimisticLockingMapsTo409() {
        ResponseEntity<ProblemDetail> response = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException(Player.class, 1L));

        assertProblem(response, 409, "Conflit de version", null);
    }

    @Test
    @DisplayName("Exceptions métier → 400, 402, 403, 404 et 409")
    void businessExceptionsMapToHttpStatuses() {
        assertAll(
                () -> assertProblem(handler.handleIllegalArgument(new IllegalArgumentException("bad")),
                        400, "Bad Request", "bad"),
                () -> assertProblem(handler.handleIllegalState(new IllegalStateException("state")),
                        400, "Bad Request", "state"),
                () -> assertProblem(handler.handleInsufficientFunds(
                        new InsufficientFundsException("fonds insuffisants")),
                        402, "Payment Required", "fonds insuffisants"),
                () -> assertProblem(handler.handleSecurity(new SecurityException("interdit")),
                        403, "Forbidden", "interdit"),
                () -> assertProblem(handler.handleEntityNotFound(new EntityNotFoundException("absent")),
                        404, "Not Found", "absent"),
                () -> assertProblem(handler.handleHarvestClosed(new HarvestClosedException("récolte fermée")),
                        409, "Conflict", "récolte fermée"),
                () -> assertProblem(handler.handleActionUndo(new PlayerActionUndoException("annulation fermée")),
                        409, "Conflict", "annulation fermée"));
    }

    private static void assertProblem(ResponseEntity<ProblemDetail> response, int status,
                                      String title, String detail) {
        assertEquals(status, response.getStatusCode().value());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(title, body.getTitle());
        if (detail != null) {
            assertEquals(detail, body.getDetail());
        }
    }
}
