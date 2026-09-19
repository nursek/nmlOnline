package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.alliance.Alliance;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.SectorConflict;
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
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mouvement allié : conflits de rupture et capture de camp")
class AllianceMovementTest {

    @Mock
    MovementOrderRepository orderRepository;

    @Mock
    VehicleRepository vehicleRepository;

    @Mock
    AllianceGraph allianceGraph;

    @Mock
    PendingCaptureService pendingCaptureService;

    @InjectMocks
    MovementService service;

    private static final Long JOUEUR_A = 1L;
    private static final Long JOUEUR_B = 2L;

    private Board board;
    private Sector secteur1, secteur2;

    @BeforeEach
    void setUp() {
        board = new Board();
        secteur1 = new Sector(1, "Cantonnement");
        secteur2 = new Sector(2, "Ailleurs");
        board.addSector(secteur1);
        board.addSector(secteur2);
        secteur1.addNeighbor(2);
        secteur2.addNeighbor(1);

        lenient().when(orderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderRepository.findPendingByTurn(1)).thenReturn(List.of());
        lenient().when(allianceGraph.activeAdjacency()).thenReturn(Map.of());
    }

    private void addUnit(Sector sector, Long playerId, long id) {
        Unit unit = new Unit(5.0, UnitClass.ELEMENTAIRE);
        unit.setId(id);
        unit.setPlayerId(playerId);
        unit.setSector(sector);
        sector.getArmy().add(unit);
    }

    private Alliance endedAllianceAtTurnOne() {
        Alliance alliance = Alliance.create(JOUEUR_A, JOUEUR_B, 0);
        alliance.end(1, JOUEUR_A, true);
        return alliance;
    }

    private void allyAWithB() {
        lenient().when(allianceGraph.activeAdjacency())
                .thenReturn(Map.of(JOUEUR_A, Set.of(JOUEUR_B), JOUEUR_B, Set.of(JOUEUR_A)));
    }

    @Test
    @DisplayName("Co-localisés le tour de la trahison : conflit détecté même sans ordre de mouvement")
    void colocatedExAlliesFightWithoutOrders() {
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur1, JOUEUR_B, 102L);
        when(allianceGraph.endedAlliancesAtTurn(1)).thenReturn(List.of(endedAllianceAtTurnOne()));

        MovementResolutionResult result = service.resolveAllMovements(1, board);

        assertEquals(1, result.getConflicts().size());
        SectorConflict conflict = result.getConflicts().getFirst();
        assertEquals(1, conflict.sectorNumber());
        assertEquals(List.of(List.of(JOUEUR_A), List.of(JOUEUR_B)), conflict.camps());
    }

    @Test
    @DisplayName("Deux alliances rompues le même tour : un seul conflit par secteur")
    void twoEndedAlliancesSameSectorSingleConflict() {
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur1, JOUEUR_B, 102L);
        addUnit(secteur1, 3L, 103L);
        addUnit(secteur1, 4L, 104L);
        Alliance second = Alliance.create(3L, 4L, 0);
        second.end(1, 3L, true);
        when(allianceGraph.endedAlliancesAtTurn(1))
                .thenReturn(List.of(endedAllianceAtTurnOne(), second));

        MovementResolutionResult result = service.resolveAllMovements(1, board);

        assertEquals(1, result.getConflicts().size());
        assertEquals(List.of(List.of(JOUEUR_A), List.of(JOUEUR_B), List.of(3L), List.of(4L)),
                result.getConflicts().getFirst().camps());
    }

    @Test
    @DisplayName("Véhicule tiers : pas de camp fantôme dans le conflit de rupture")
    void thirdPartyVehicleDoesNotCreateGhostCamp() {
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur1, JOUEUR_B, 102L);
        Vehicle vehicle = new Vehicle(VehicleType.VTT_LEGER, 5L);
        vehicle.setId(201L);
        vehicle.setSector(secteur1);
        secteur1.getVehicles().add(vehicle);
        when(allianceGraph.endedAlliancesAtTurn(1)).thenReturn(List.of(endedAllianceAtTurnOne()));

        MovementResolutionResult result = service.resolveAllMovements(1, board);

        assertEquals(1, result.getConflicts().size());
        assertEquals(List.of(List.of(JOUEUR_A), List.of(JOUEUR_B)), result.getConflicts().getFirst().camps(),
                "Le véhicule du tiers ne compte pas comme camp : pas d'impasse fantôme");
    }

    @Test
    @DisplayName("Ex-alliés séparés : aucun conflit")
    void separatedExAlliesDoNotFight() {
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur2, JOUEUR_B, 102L);
        when(allianceGraph.endedAlliancesAtTurn(1)).thenReturn(List.of(endedAllianceAtTurnOne()));

        MovementResolutionResult result = service.resolveAllMovements(1, board);

        assertTrue(result.getConflicts().isEmpty());
    }

    @Test
    @DisplayName("Aucune rupture ce tour : pas de conflit spontané entre co-localisés")
    void noRuptureNoSpontaneousConflict() {
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur1, JOUEUR_B, 102L);
        when(allianceGraph.endedAlliancesAtTurn(1)).thenReturn(List.of());

        MovementResolutionResult result = service.resolveAllMovements(1, board);

        assertTrue(result.getConflicts().isEmpty());
    }

    @Test
    @DisplayName("Défenseurs alliés gagnants dans leur propre secteur : pas de neutralisation")
    void alliedDefendersKeepTheirOwnSector() {
        allyAWithB();
        secteur1.setOwnerId(JOUEUR_A);
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur1, JOUEUR_B, 102L);
        MovementService.ResolutionContext ctx = new MovementService.ResolutionContext(1);

        service.captureAfterBattle(board, ctx, 1, List.of(JOUEUR_A, JOUEUR_B));

        assertEquals(JOUEUR_A, secteur1.getOwnerId());
        assertTrue(ctx.captures.isEmpty());
        verify(pendingCaptureService, never()).queueNeutral(any(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("Camp allié vainqueur sur secteur neutre : neutralisé et attribution en attente")
    void alliedCampWinsNeutralSectorQueuesPendingCapture() {
        allyAWithB();
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur1, JOUEUR_B, 102L);
        MovementService.ResolutionContext ctx = new MovementService.ResolutionContext(1);

        service.captureAfterBattle(board, ctx, 1, List.of(JOUEUR_A, JOUEUR_B));

        assertTrue(ctx.captures.isEmpty());
        verify(pendingCaptureService).queueNeutral(board, 1, 1, List.of(JOUEUR_A, JOUEUR_B));
    }
}
