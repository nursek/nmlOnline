package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.domain.model.vehicle.Vehicle;
import com.mg.nmlonline.domain.model.vehicle.VehicleType;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
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
import static org.mockito.Mockito.when;

// Propriété clé : une entité arrivée au step N est tangible pour tout arrivant au step N+1.
@ExtendWith(MockitoExtension.class)
@DisplayName("Simulation : résolution incrémentale multi-step")
class MovementSimulationTest {

    @Mock
    MovementOrderRepository orderRepository;

    @Mock
    VehicleRepository vehicleRepository;

    @InjectMocks
    MovementService service;

    private static final Long JOUEUR_A = 1L;
    private static final Long JOUEUR_B = 2L;
    private static final Long JOUEUR_C = 3L;

    private static final Long UNITE_A_ID  = 101L;
    private static final Long UNITE_B_ID  = 102L;
    private static final Long VEHICULE_C_ID = 201L;
    private static final Long PILOTE_C_ID   = 301L;

    private Board  board;
    private Sector secteur1, secteur2, secteur3, secteur4;
    private Unit   uniteA, uniteB;
    private Vehicle vehiculeC;

    @BeforeEach
    void setUp() {
        board    = new Board();
        secteur1 = new Sector(1, "Zone A (départ A)");
        secteur2 = new Sector(2, "Objectif (B défend)");
        secteur3 = new Sector(3, "Zone intermédiaire");
        secteur4 = new Sector(4, "Zone C (départ C)");

        board.addSector(secteur1);
        board.addSector(secteur2);
        board.addSector(secteur3);
        board.addSector(secteur4);

        secteur1.addNeighbor(2); secteur2.addNeighbor(1);
        secteur2.addNeighbor(3); secteur3.addNeighbor(2);
        secteur3.addNeighbor(4); secteur4.addNeighbor(3);

        secteur1.setOwnerId(JOUEUR_A);
        secteur2.setOwnerId(JOUEUR_B);
        secteur4.setOwnerId(JOUEUR_C);

        uniteA = new Unit(5.0, UnitClass.ELEMENTAIRE);
        uniteA.setId(UNITE_A_ID);
        uniteA.setPlayerId(JOUEUR_A);
        uniteA.setSector(secteur1);
        secteur1.getArmy().add(uniteA);

        uniteB = new Unit(5.0, UnitClass.ELEMENTAIRE);
        uniteB.setId(UNITE_B_ID);
        uniteB.setPlayerId(JOUEUR_B);
        uniteB.setSector(secteur2);
        secteur2.getArmy().add(uniteB);

        vehiculeC = new Vehicle(VehicleType.VTT_LEGER, JOUEUR_C);
        vehiculeC.setId(VEHICULE_C_ID);
        vehiculeC.setSector(secteur4);
        Unit pilote = new Unit(10.0, UnitClass.PILOTE_DESTRUCTEUR);
        pilote.setId(PILOTE_C_ID);
        pilote.setPlayerId(JOUEUR_C);
        vehiculeC.assignPilot(pilote);
        secteur4.getVehicles().add(vehiculeC);

        when(orderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Les entités arrivées au step 1 doivent être visibles comme défenseurs au step 2")
    void shouldDetectConflictsWithEntitiesArrivedInPreviousStep() {
        MovementOrder ordreA = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(UNITE_A_ID), List.of(1, 2));
        ordreA.setId(1L);

        MovementOrder ordreC = MovementOrder.createVehicleOrder(JOUEUR_C, 1, VEHICULE_C_ID, List.of(4, 3, 2));
        ordreC.setId(2L);

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordreA, ordreC));
        when(vehicleRepository.findById(VEHICULE_C_ID)).thenReturn(Optional.of(vehiculeC));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertTrue(
                resultat.getConflicts().stream().anyMatch(c ->
                        c.sectorNumber() == 2 &&
                        c.attackerPlayerId().equals(JOUEUR_A) &&
                        c.defenderPlayerId().equals(JOUEUR_B)),
                "Conflit step 1 attendu : Joueur A attaque Joueur B en secteur 2"
        );

        assertTrue(
                resultat.getConflicts().stream().anyMatch(c ->
                        c.sectorNumber() == 2 &&
                        c.attackerPlayerId().equals(JOUEUR_C) &&
                        c.defenderPlayerId().equals(JOUEUR_A)),
                "Conflit step 2 attendu : Joueur C affronte Joueur A (arrivé au step 1) en secteur 2"
        );

        assertTrue(
                resultat.getConflicts().stream().anyMatch(c ->
                        c.sectorNumber() == 2 &&
                        c.attackerPlayerId().equals(JOUEUR_C) &&
                        c.defenderPlayerId().equals(JOUEUR_B)),
                "Conflit step 2 attendu : Joueur C affronte Joueur B (défenseur originel) en secteur 2"
        );

        assertTrue(resultat.getBlocked().isEmpty(), "Aucun ordre ne doit être bloqué");

        assertEquals(2, resultat.getResolved().size(), "Les deux ordres doivent être résolus");

        assertTrue(
                secteur2.getArmy().stream().anyMatch(u -> u.getId().equals(UNITE_A_ID)),
                "Unité A doit être en secteur 2 après résolution"
        );

        assertEquals(secteur2, vehiculeC.getSector(),
                "Véhicule C doit être en secteur 2 après 2 steps");

        assertTrue(
                secteur1.getArmy().stream().noneMatch(u -> u.getId().equals(UNITE_A_ID)),
                "Unité A ne doit plus être en secteur 1"
        );
    }

        @Test
        @DisplayName("Secteur intermédiaire vide ne doit pas générer de conflit")
        void shouldNotGenerateConflictWhenTransitingThroughEmptySector() {
        // Le combat de transit ne s'applique qu'aux secteurs intermédiaires, pas à l'arrivée.
        MovementOrder ordreC = MovementOrder.createVehicleOrder(JOUEUR_C, 1, VEHICULE_C_ID, List.of(4, 3, 2));
        ordreC.setId(1L);

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordreC));
        when(vehicleRepository.findById(VEHICULE_C_ID)).thenReturn(Optional.of(vehiculeC));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertTrue(
                resultat.getConflicts().stream().noneMatch(c -> c.sectorNumber() == 3),
                "Aucun conflit ne doit être généré pour le secteur intermédiaire vide"
        );

        assertTrue(
                resultat.getConflicts().stream().anyMatch(c ->
                        c.sectorNumber() == 2 &&
                        c.attackerPlayerId().equals(JOUEUR_C) &&
                        c.defenderPlayerId().equals(JOUEUR_B)),
                "Conflit attendu en secteur 2 : C vs B"
        );

        assertTrue(resultat.getTransitCombats().isEmpty(),
                "Pas de combat de transit attendu (secteur intermédiaire vide)");
    }
}
