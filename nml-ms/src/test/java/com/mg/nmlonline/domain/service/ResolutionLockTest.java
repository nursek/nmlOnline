package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.EmbeddedPostgresTest;
import com.mg.nmlonline.domain.exception.PlayerActionUndoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EmbeddedPostgresTest
@DisplayName("Résolution en cours — les mutations joueur sont fermées")
class ResolutionLockTest {

    private static final Long BOARD_ID = 987_654L;
    private static final Long USER_ID = 1L;

    @Autowired
    private TurnLock turnLock;
    @Autowired
    private AllianceService allianceService;
    @Autowired
    private UnitService unitService;
    @Autowired
    private SniperOrderService sniperOrderService;
    @Autowired
    private VehicleService vehicleService;
    @Autowired
    private PlayerActionService playerActionService;

    @BeforeEach
    void lock() {
        turnLock.tryAcquire(BOARD_ID);
    }

    @AfterEach
    void unlock() {
        turnLock.release(BOARD_ID);
    }

    @Test
    @DisplayName("alliance, annulation d'ordre et achat de véhicule sont refusés avant toute lecture")
    void mutationsAreRefusedWhileLocked() {
        assertAll(
                refused(() -> allianceService.accept(USER_ID, BOARD_ID, 1L)),
                refused(() -> allianceService.betray(USER_ID, BOARD_ID, 1L)),
                refused(() -> unitService.cancelOrder(USER_ID, 1L, BOARD_ID)),
                refused(() -> sniperOrderService.cancel(USER_ID, BOARD_ID, 1L)),
                refused(() -> vehicleService.buyVehicle(USER_ID, "MOTO", 1, BOARD_ID)),
                refused(() -> vehicleService.buyVehiclesBatch(USER_ID, List.of(), BOARD_ID)),
                refused(() -> sniperOrderService.place(USER_ID, BOARD_ID, 1L, 2)),
                refused(PlayerActionUndoException.class,
                        () -> playerActionService.undoFrom(USER_ID, BOARD_ID, 1L)));
    }

    private static Executable refused(Executable mutation) {
        return refused(IllegalStateException.class, mutation);
    }

    private static Executable refused(Class<? extends Throwable> expected, Executable mutation) {
        return () -> assertThrows(expected, mutation);
    }
}
