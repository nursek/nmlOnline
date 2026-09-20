package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementResolutionResult;
import com.mg.nmlonline.domain.model.movement.SectorCapture;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.GameCharacter;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Capture des secteurs en fin de tour")
class SectorCaptureTest {

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

    private Board board;
    private Sector secteur1, secteur2, secteur3, secteur4, secteur5;

    @BeforeEach
    void setUp() {
        board = new Board();
        secteur1 = new Sector(1, "Départ A");
        secteur2 = new Sector(2, "Intermédiaire");
        secteur3 = new Sector(3, "Arrivée A");
        secteur4 = new Sector(4, "Départ B");
        secteur5 = new Sector(5, "Zone C");

        board.addSector(secteur1);
        board.addSector(secteur2);
        board.addSector(secteur3);
        board.addSector(secteur4);
        board.addSector(secteur5);

        secteur1.addNeighbor(2); secteur2.addNeighbor(1);
        secteur2.addNeighbor(3); secteur3.addNeighbor(2);
        secteur3.addNeighbor(4); secteur4.addNeighbor(3);
        secteur4.addNeighbor(5); secteur5.addNeighbor(4);

        lenient().when(orderRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(allianceGraph.activeAdjacency()).thenReturn(Map.of());
        lenient().when(allianceGraph.endedAlliancesAtTurn(anyInt())).thenReturn(List.of());
    }

    private Unit addUnit(Sector sector, Long playerId, long id, UnitClass unitClass) {
        Unit unit = new Unit(5.0, unitClass);
        unit.setId(id);
        unit.setPlayerId(playerId);
        unit.setSector(sector);
        sector.getArmy().add(unit);
        return unit;
    }

    private SectorCapture captureFor(MovementResolutionResult resultat, int sectorNumber) {
        return resultat.getCaptures().stream()
                .filter(c -> c.sectorNumber() == sectorNumber)
                .findFirst()
                .orElse(null);
    }

    @Test
    @DisplayName("une unité qui termine sur un secteur neutre vide le capture")
    void uniteSurSecteurNeutre_capture() {
        secteur1.setOwnerId(JOUEUR_A);
        addUnit(secteur1, JOUEUR_A, 101L, UnitClass.TIREUR);
        MovementOrder ordre = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2));
        ordre.setId(1L);
        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordre));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertEquals(JOUEUR_A, secteur2.getOwnerId(), "Le secteur neutre vide doit être capturé");
        SectorCapture capture = captureFor(resultat, 2);
        assertNotNull(capture);
        assertEquals(JOUEUR_A, capture.playerId());
        assertFalse(capture.onTheFly(), "Arrivée simple : capture de fin de tour, pas à la volée");
    }

    @Test
    @DisplayName("une unité qui termine sur un secteur ennemi vide le capture")
    void uniteSurSecteurEnnemiVide_capture() {
        secteur1.setOwnerId(JOUEUR_A);
        secteur2.setOwnerId(JOUEUR_B);
        addUnit(secteur1, JOUEUR_A, 101L, UnitClass.TIREUR);
        MovementOrder ordre = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2));
        ordre.setId(1L);
        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordre));

        service.resolveAllMovements(1, board);

        assertEquals(JOUEUR_A, secteur2.getOwnerId(), "Le secteur ennemi laissé vide doit être pris");
    }

    @Test
    @DisplayName("une entité adverse survivante bloque la capture")
    void entiteAdverseSurvivante_bloqueLaCapture() {
        secteur1.setOwnerId(JOUEUR_A);
        secteur2.setOwnerId(JOUEUR_B);
        addUnit(secteur1, JOUEUR_A, 101L, UnitClass.TIREUR);
        addUnit(secteur2, JOUEUR_B, 102L, UnitClass.TIREUR);
        MovementOrder ordre = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2));
        ordre.setId(1L);
        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordre));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertEquals(JOUEUR_B, secteur2.getOwnerId(), "Deux camps présents ⇒ le secteur ne change pas de main");
        assertNull(captureFor(resultat, 2), "Aucune capture ne doit être rapportée pour un secteur contesté");
    }

    @Test
    @DisplayName("une unité LEGER capture à la volée le secteur intermédiaire qu'elle traverse")
    void legerTraversantSecteurNeutre_captureALaVolee() {
        secteur1.setOwnerId(JOUEUR_A);
        addUnit(secteur1, JOUEUR_A, 101L, UnitClass.LEGER);
        MovementOrder ordre = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2, 3));
        ordre.setId(1L);
        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordre));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertEquals(JOUEUR_A, secteur2.getOwnerId(), "Le secteur traversé doit être capturé");
        assertTrue(captureFor(resultat, 2).onTheFly(), "Capture du secteur traversé marquée à la volée");
        assertEquals(JOUEUR_A, secteur3.getOwnerId(), "Le secteur d'arrivée est capturé par présence");
        assertFalse(captureFor(resultat, 3).onTheFly(), "Arrivée finale : capture de fin de tour");
    }

    @Test
    @DisplayName("un combat dans le secteur intermédiaire bloque la capture à la volée")
    void combatEnIntermediaire_bloqueLaCaptureALaVolee() {
        secteur1.setOwnerId(JOUEUR_A);
        addUnit(secteur1, JOUEUR_A, 101L, UnitClass.LEGER);
        Headquarters qgB = new Headquarters(JOUEUR_B);
        qgB.setId(201L);
        qgB.setSector(secteur2);
        secteur2.getBuildings().add(qgB);
        MovementOrder ordre = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2, 3));
        ordre.setId(1L);
        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordre));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertFalse(resultat.getConflicts().isEmpty(), "Prérequis : un conflit est détecté en secteur 2");
        assertNull(secteur2.getOwnerId(), "Combat ⇒ pas de capture à la volée, et le QG seul ne capture pas");
        assertNull(captureFor(resultat, 2));
    }

    @Test
    @DisplayName("une autre arrivée dans le secteur intermédiaire bloque la capture à la volée")
    void autreArriveeEnIntermediaire_bloqueLaCaptureALaVolee() {
        secteur1.setOwnerId(JOUEUR_A);
        addUnit(secteur1, JOUEUR_A, 101L, UnitClass.LEGER);
        addUnit(secteur1, JOUEUR_A, 102L, UnitClass.LEGER);
        MovementOrder ordre1 = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2, 3));
        ordre1.setId(1L);
        MovementOrder ordre2 = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(102L), List.of(1, 2, 3));
        ordre2.setId(2L);
        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordre1, ordre2));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertNull(secteur2.getOwnerId(), "Deux passages dans le même tour ⇒ pas de capture à la volée");
        assertNull(captureFor(resultat, 2));
        assertEquals(JOUEUR_A, secteur3.getOwnerId(), "Le secteur d'arrivée reste capturé par présence");
    }

    @Test
    @DisplayName("un véhicule adverse stationnaire annule la capture à la volée sans capture fantôme")
    void vehiculeAdverseStationnaire_annuleLaCaptureALaVolee() {
        secteur1.setOwnerId(JOUEUR_A);
        secteur2.setOwnerId(JOUEUR_B);
        Vehicle vehicule = new Vehicle(VehicleType.VTT_LEGER, JOUEUR_B);
        vehicule.setId(201L);
        vehicule.setSector(secteur2);
        secteur2.getVehicles().add(vehicule);
        addUnit(secteur1, JOUEUR_A, 101L, UnitClass.LEGER);
        MovementOrder ordre = MovementOrder.createFootOrder(JOUEUR_A, 1, List.of(101L), List.of(1, 2, 3));
        ordre.setId(1L);
        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of(ordre));

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertEquals(JOUEUR_B, secteur2.getOwnerId(), "Le véhicule stationnaire reste maître du secteur");
        assertNull(captureFor(resultat, 2), "Aucune capture ne doit être rapportée pour ce secteur");
    }

    @Test
    @DisplayName("véhicule ou personnage seul présent sur un secteur neutre le capture en fin de tour")
    void vehiculeEtPersonnage_capturentParPresence() {
        Vehicle vehicule = new Vehicle(VehicleType.VTT_LEGER, JOUEUR_C);
        vehicule.setId(201L);
        vehicule.setSector(secteur2);
        secteur2.getVehicles().add(vehicule);

        GameCharacter personnage = new GameCharacter("Perso C", 10, 0, 0, 10, 0, 0);
        personnage.setId(301L);
        personnage.setPlayerId(JOUEUR_C);
        personnage.setSector(secteur5);
        secteur5.getCharacters().add(personnage);

        when(orderRepository.findPendingByTurn(1)).thenReturn(List.of());

        MovementResolutionResult resultat = service.resolveAllMovements(1, board);

        assertEquals(JOUEUR_C, secteur2.getOwnerId(), "Le véhicule capture le secteur où il stationne");
        assertEquals(JOUEUR_C, secteur5.getOwnerId(), "Le personnage capture le secteur où il stationne");
        assertEquals(2, resultat.getCaptures().size());
    }
}
