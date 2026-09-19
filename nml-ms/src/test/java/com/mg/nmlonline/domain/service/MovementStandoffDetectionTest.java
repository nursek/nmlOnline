package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.SectorConflict;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Détection : cercle d'impasse et ancrage du défenseur")
class MovementStandoffDetectionTest {

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
    private static final Long JOUEUR_C = 3L;
    private static final Long JOUEUR_D = 4L;

    private Board board;
    private Sector secteur1, secteur2, secteur3, secteur4;

    @BeforeEach
    void setUp() {
        board = new Board();
        secteur1 = new Sector(1, "Départ A");
        secteur2 = new Sector(2, "Objectif");
        secteur3 = new Sector(3, "Départ B");
        secteur4 = new Sector(4, "Départ C");

        board.addSector(secteur1);
        board.addSector(secteur2);
        board.addSector(secteur3);
        board.addSector(secteur4);

        secteur1.addNeighbor(2); secteur2.addNeighbor(1);
        secteur2.addNeighbor(3); secteur3.addNeighbor(2);
        secteur2.addNeighbor(4); secteur4.addNeighbor(2);

        lenient().when(orderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(allianceGraph.activeAdjacency()).thenReturn(Map.of());
        lenient().when(allianceGraph.endedAlliancesAtTurn(anyInt())).thenReturn(List.of());
    }

    private void addUnit(Sector sector, Long playerId, long id) {
        Unit unit = new Unit(5.0, UnitClass.TIREUR);
        unit.setId(id);
        unit.setPlayerId(playerId);
        unit.setSector(sector);
        sector.getArmy().add(unit);
    }

    @Test
    @DisplayName("3 arrivants : cercle ordonné par 1er ordre envoyé du tour")
    void troisArrivants_cercleOrdonneParHeureDEnvoi() {
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur3, JOUEUR_B, 102L);
        addUnit(secteur4, JOUEUR_C, 103L);

        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        MovementOrder ordreA = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2));
        ordreA.setId(10L);
        ordreA.setSubmittedAt(t0.plusSeconds(10));
        MovementOrder ordreB = MovementOrder.createFootOrder(JOUEUR_B, 1, List.of(102L), List.of(3, 2));
        ordreB.setId(11L);
        ordreB.setSubmittedAt(t0);
        MovementOrder ordreC = MovementOrder.createFootOrder(JOUEUR_C, 1, List.of(103L), List.of(4, 2));
        ordreC.setId(12L);
        ordreC.setSubmittedAt(t0.plusSeconds(30));

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordreA, ordreB, ordreC));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertEquals(1, resultat.getConflicts().size());
        SectorConflict conflit = resultat.getConflicts().getFirst();
        assertTrue(conflit.isStandoff());
        assertEquals(List.of(JOUEUR_B, JOUEUR_A, JOUEUR_C), conflit.participantPlayerIds(),
                "B a envoyé en premier, puis A, puis C");
    }

    @Test
    @DisplayName("Défenseur stationnaire en tête du cercle, puis les arrivants par heure d'envoi")
    void defenseurStationnaire_ancreEnTeteDuCercle() {
        addUnit(secteur2, JOUEUR_B, 102L);
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur4, JOUEUR_C, 103L);

        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        MovementOrder ordreA = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2));
        ordreA.setId(10L);
        ordreA.setSubmittedAt(t0);
        MovementOrder ordreC = MovementOrder.createFootOrder(JOUEUR_C, 1, List.of(103L), List.of(4, 2));
        ordreC.setId(11L);
        ordreC.setSubmittedAt(t0.plusSeconds(5));

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordreA, ordreC));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertEquals(1, resultat.getConflicts().size());
        SectorConflict conflit = resultat.getConflicts().getFirst();
        assertTrue(conflit.isStandoff());
        assertEquals(List.of(JOUEUR_B, JOUEUR_A, JOUEUR_C), conflit.participantPlayerIds(),
                "Le défenseur B ouvre le cercle, A puis C suivent");
    }

    @Test
    @DisplayName("2 camps : duel [arrivant, défenseur] conservé")
    void deuxCamps_duelConserve() {
        addUnit(secteur2, JOUEUR_B, 102L);
        addUnit(secteur1, JOUEUR_A, 101L);

        MovementOrder ordreA = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2));
        ordreA.setId(10L);

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordreA));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertEquals(1, resultat.getConflicts().size());
        SectorConflict conflit = resultat.getConflicts().getFirst();
        assertFalse(conflit.isStandoff());
        assertEquals(List.of(JOUEUR_A, JOUEUR_B), conflit.participantPlayerIds());
    }

    @Test
    @DisplayName("Croiseur : rejoint l'impasse des camps présents, son partenaire d'échange n'est pas défenseur")
    void croiseur_rejointLImpasse() {
        addUnit(secteur2, JOUEUR_B, 102L);
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur3, JOUEUR_C, 103L);
        addUnit(secteur2, JOUEUR_D, 104L);

        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        MovementOrder ordreA = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2));
        ordreA.setId(10L);
        ordreA.setSubmittedAt(t0);
        MovementOrder ordreC = MovementOrder.createFootOrder(JOUEUR_C, 1, List.of(103L), List.of(3, 2));
        ordreC.setId(11L);
        ordreC.setSubmittedAt(t0.plusSeconds(5));
        MovementOrder ordreD = MovementOrder.createFootOrder(JOUEUR_D, 1, List.of(104L), List.of(2, 3));
        ordreD.setId(12L);

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordreA, ordreC, ordreD));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertEquals(1, resultat.getConflicts().size(), "Un unique conflit groupé au secteur 2");
        SectorConflict conflit = resultat.getConflicts().getFirst();
        assertEquals(2, conflit.sectorNumber());
        assertTrue(conflit.isStandoff(), "Défenseur + 2 arrivants dont le croiseur : impasse à 3 camps");
        assertEquals(List.of(JOUEUR_B, JOUEUR_A, JOUEUR_C), conflit.participantPlayerIds(),
                "B ouvre le cercle, puis A et C par heure d'envoi ; le partenaire D n'y est pas");
    }

    @Test
    @DisplayName("Même joueur croiseur et arrivant : un seul conflit, pas de bataille dupliquée")
    void croiseurEtArrivantMemeJoueur_pasDeConflitDuplique() {
        addUnit(secteur2, JOUEUR_B, 102L);
        addUnit(secteur3, JOUEUR_A, 101L);
        addUnit(secteur1, JOUEUR_A, 105L);
        addUnit(secteur2, JOUEUR_C, 103L);

        MovementOrder croisementA = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(3, 2));
        croisementA.setId(10L);
        MovementOrder arriveeA = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(105L), List.of(1, 2));
        arriveeA.setId(11L);
        MovementOrder croisementC = MovementOrder.createFootOrder(JOUEUR_C, 1, List.of(103L), List.of(2, 3));
        croisementC.setId(12L);

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(croisementA, arriveeA, croisementC));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        List<SectorConflict> conflits = resultat.getConflicts().stream()
                .filter(c -> c.sectorNumber() == 2)
                .toList();
        assertEquals(1, conflits.size(), "A est déjà groupé comme arrivant : pas de second conflit A/B");
        assertEquals(List.of(JOUEUR_A, JOUEUR_B), conflits.getFirst().participantPlayerIds());
    }

    @Test
    @DisplayName("Garnison : reste défenseur quand une autre unité du même joueur part en croisement")
    void garnison_resteDefenseurQuandUneAutreUniteCroise() {
        addUnit(secteur2, JOUEUR_B, 102L);
        addUnit(secteur1, JOUEUR_A, 101L);
        addUnit(secteur2, JOUEUR_B, 106L);
        addUnit(secteur3, JOUEUR_C, 103L);

        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        MovementOrder arriveeA = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2));
        arriveeA.setId(10L);
        arriveeA.setSubmittedAt(t0);
        MovementOrder croisementC = MovementOrder.createFootOrder(JOUEUR_C, 1, List.of(103L), List.of(3, 2));
        croisementC.setId(11L);
        croisementC.setSubmittedAt(t0.plusSeconds(5));
        MovementOrder departB = MovementOrder.createFootOrder(JOUEUR_B, 1, List.of(106L), List.of(2, 3));
        departB.setId(12L);

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(arriveeA, croisementC, departB));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertEquals(1, resultat.getConflicts().size(), "Un unique conflit groupé au secteur 2");
        SectorConflict conflit = resultat.getConflicts().getFirst();
        assertTrue(conflit.isStandoff(), "La garnison de B + A + C : impasse à 3 camps");
        assertEquals(List.of(JOUEUR_B, JOUEUR_A, JOUEUR_C), conflit.participantPlayerIds(),
                "B garde sa garnison en défense, puis A et C par heure d'envoi");
    }
}
