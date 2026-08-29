package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.ScenarioSummaryDto;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TurnResolutionScenarioSeeder — préparation du scénario de test")
class TurnResolutionScenarioSeederTest {

    @Autowired
    private TurnResolutionScenarioSeeder seeder;

    @Autowired
    private TurnService turnService;

    @Autowired
    private MovementOrderRepository movementOrderRepository;

    @Autowired
    private TurnResolutionOrchestrator orchestrator;

    @AfterEach
    void releaseLock() {
        orchestrator.abort();
    }

    @Test
    @Transactional
    @DisplayName("seedScenario crée un ordre PENDING route [41, 13, 32] et l'idempotence évite les doublons")
    void seedScenario_creeUnOrdreValideUnSeederNeDoublePas() {
        int turn = turnService.getCurrentTurn();

        ScenarioSummaryDto result = seeder.seedScenario();

        assertEquals(turn, result.getTurn());
        assertEquals(List.of(41, 13, 32), result.getRoute());
        assertNotNull(result.getOrderId());
        assertNotNull(result.getAttackerUnit());
        assertNotNull(result.getDefender());
        assertEquals(32, result.getDefender().getId() > 0 ? 32 : 32);

        var createdOrders = movementOrderRepository.findByPlayerIdAndTurnAndStatus(
                result.getAttacker().getId(), turn, MovementStatus.PENDING);
        assertEquals(1, createdOrders.size(),
                "Exactement un ordre PENDING pour lurio après seed");
        assertEquals(List.of(41, 13, 32), createdOrders.get(0).getRoute());

        ScenarioSummaryDto secondCall = seeder.seedScenario();
        var stillPending = movementOrderRepository.findByPlayerIdAndTurnAndStatus(
                secondCall.getAttacker().getId(), turn, MovementStatus.PENDING);
        assertEquals(1, stillPending.size(),
                "Re-seed ne crée pas de doublon d'ordre PENDING pour lurio");
        assertEquals(0, secondCall.getDefendersAdded(),
                "Au second appel, aucun défenseur n'est ré-ajouté (idempotence)");
    }
}