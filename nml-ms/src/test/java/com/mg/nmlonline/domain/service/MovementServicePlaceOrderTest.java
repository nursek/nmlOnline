package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Placement d'un ordre de déplacement (à pied ou véhicule)")
class MovementServicePlaceOrderTest {

    @Mock
    MovementOrderRepository orderRepository;

    @Mock
    VehicleRepository vehicleRepository;

    @InjectMocks
    MovementService service;

    private static final Long JOUEUR = 1L;
    private static final Long UNITE_1_ID = 101L;
    private static final Long UNITE_2_ID = 102L;
    private static final Long VEHICULE_ID = 201L;

    private Board board;
    private Vehicle vehicule;

    @BeforeEach
    void setUp() {
        board = new Board();
        Sector depart = new Sector(1, "Départ");
        Sector arrivee = new Sector(2, "Arrivée");
        Sector etape = new Sector(3, "Étape");
        board.addSector(depart);
        board.addSector(arrivee);
        board.addSector(etape);
        depart.addNeighbor(2);
        arrivee.addNeighbor(1);
        arrivee.addNeighbor(3);
        etape.addNeighbor(2);

        depart.getArmy().add(unite(UNITE_1_ID, depart));
        depart.getArmy().add(unite(UNITE_2_ID, depart));

        vehicule = new Vehicle(VehicleType.VTT_LEGER, JOUEUR);
        vehicule.setId(VEHICULE_ID);
        vehicule.setSector(depart);
        depart.getVehicles().add(vehicule);

        Unit pilote = new Unit(10.0, UnitClass.PILOTE_DESTRUCTEUR);
        pilote.setId(301L);
        pilote.setPlayerId(JOUEUR);
        vehicule.assignPilot(pilote);
    }

    private Unit unite(Long id, Sector secteur) {
        Unit unite = new Unit(0.0, UnitClass.LEGER);
        unite.setId(id);
        unite.setPlayerId(JOUEUR);
        unite.setSector(secteur);
        return unite;
    }

    @Test
    @DisplayName("Un ordre groupé conserve toutes les entités ciblées")
    void shouldKeepAllEntityIdsInOneOrder() {
        when(orderRepository.findPendingEntityIds(eq(1), any())).thenReturn(List.of());
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MovementOrder ordre = service.placeFootOrder(
                JOUEUR, 1, List.of(UNITE_1_ID, UNITE_2_ID), List.of(1, 2), board);

        assertEquals(List.of(UNITE_1_ID, UNITE_2_ID), ordre.getEntityIds());
        assertEquals(1, ordre.getFromSectorNumber());
        assertEquals(2, ordre.getToSectorNumber());
    }

    @Test
    @DisplayName("Refuse deux fois la même entité dans un ordre")
    void shouldRejectDuplicateEntities() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                service.placeFootOrder(JOUEUR, 1, List.of(UNITE_1_ID, UNITE_1_ID), List.of(1, 2), board));

        assertTrue(erreur.getMessage().contains("deux fois"));
    }

    @Test
    @DisplayName("Refuse une entité déjà engagée dans un ordre en attente")
    void shouldRejectEntityAlreadyInPendingOrder() {
        when(orderRepository.findPendingEntityIds(1, List.of(UNITE_1_ID, UNITE_2_ID)))
                .thenReturn(List.of(UNITE_1_ID));

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                service.placeFootOrder(JOUEUR, 1, List.of(UNITE_1_ID, UNITE_2_ID), List.of(1, 2), board));

        assertTrue(erreur.getMessage().contains("déjà engagée"));
    }

    @Test
    @DisplayName("Refuse une entité qui est dans un véhicule")
    void shouldRejectEntityInVehicle() {
        when(orderRepository.findPendingEntityIds(eq(1), any())).thenReturn(List.of());
        when(vehicleRepository.existsByPilot_Id(UNITE_1_ID)).thenReturn(true);

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                service.placeFootOrder(JOUEUR, 1, List.of(UNITE_1_ID, UNITE_2_ID), List.of(1, 2), board));

        assertTrue(erreur.getMessage().contains("véhicule"));
    }

    @Test
    @DisplayName("Refuse un passager de véhicule")
    void shouldRejectVehiclePassenger() {
        when(orderRepository.findPendingEntityIds(eq(1), any())).thenReturn(List.of());
        when(vehicleRepository.existsByPilot_Id(UNITE_1_ID)).thenReturn(false);
        when(vehicleRepository.existsByPassengers_Id(UNITE_1_ID)).thenReturn(true);

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                service.placeFootOrder(JOUEUR, 1, List.of(UNITE_1_ID, UNITE_2_ID), List.of(1, 2), board));

        assertTrue(erreur.getMessage().contains("véhicule"));
    }

    @Test
    @DisplayName("Refuse un départ et une arrivée identiques")
    void shouldRejectSameStartAndEndSector() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                service.placeFootOrder(JOUEUR, 1, List.of(UNITE_1_ID), List.of(1, 1), board));

        assertTrue(erreur.getMessage().contains("différents"));
    }

    @Test
    @DisplayName("Refuse un secteur de destination inexistant")
    void shouldRejectUnknownDestinationSector() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                service.placeFootOrder(JOUEUR, 1, List.of(UNITE_1_ID), List.of(1, 99), board));

        assertTrue(erreur.getMessage().contains("destination inexistant"));
    }

    @Test
    @DisplayName("Refuse une entité absente du secteur de départ")
    void shouldRejectEntityOutsideStartingSector() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                service.placeFootOrder(JOUEUR, 1, List.of(999L), List.of(1, 2), board));

        assertTrue(erreur.getMessage().contains("n'est pas dans le secteur"));
    }

    @Test
    @DisplayName("Refuse une route plus longue que les hops de l'unité")
    void shouldRejectRouteLongerThanFootHops() {
        Unit tireur = new Unit(0.0, UnitClass.TIREUR);
        tireur.setId(401L);
        tireur.setPlayerId(JOUEUR);
        tireur.setSector(board.getSector(1));
        board.getSector(1).getArmy().add(tireur);

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                service.placeFootOrder(JOUEUR, 1, List.of(401L), List.of(1, 2, 3), board));

        assertTrue(erreur.getMessage().contains("ne peut parcourir que"));
    }

    @Test
    @DisplayName("Un véhicule sans pilote ne peut pas recevoir d'ordre")
    void shouldRejectVehicleWithoutPilot() {
        vehicule.removePilot();
        when(vehicleRepository.findByIdForUpdate(VEHICULE_ID)).thenReturn(Optional.of(vehicule));

        assertThrows(IllegalArgumentException.class,
                () -> service.placeVehicleOrder(JOUEUR, 1, VEHICULE_ID, List.of(1, 2), board));
    }

    @Test
    @DisplayName("Refuse un second ordre en attente pour le même véhicule")
    void shouldRejectSecondPendingOrder() {
        when(vehicleRepository.findByIdForUpdate(VEHICULE_ID)).thenReturn(Optional.of(vehicule));
        when(orderRepository.findByVehicleIdAndTurnAndStatus(VEHICULE_ID, 1, MovementStatus.PENDING))
                .thenReturn(List.of(MovementOrder.createVehicleOrder(JOUEUR, 1, VEHICULE_ID, List.of(1, 2))));

        IllegalStateException erreur = assertThrows(IllegalStateException.class,
                () -> service.placeVehicleOrder(JOUEUR, 1, VEHICULE_ID, List.of(1, 2), board));

        assertTrue(erreur.getMessage().contains("déjà"));
    }

    @Test
    @DisplayName("Refuse le véhicule d'un autre joueur")
    void shouldRejectVehicleOfAnotherPlayer() {
        Vehicle etranger = new Vehicle(VehicleType.VTT_LEGER, 2L);
        etranger.setId(203L);
        etranger.setSector(board.getSector(1));
        when(vehicleRepository.findByIdForUpdate(203L)).thenReturn(Optional.of(etranger));

        assertThrows(SecurityException.class,
                () -> service.placeVehicleOrder(JOUEUR, 1, 203L, List.of(1, 2), board));
    }

    @Test
    @DisplayName("Refuse un véhicule hors de son secteur de départ")
    void shouldRejectVehicleOutsideStartingSector() {
        vehicule.setSector(board.getSector(2));
        when(vehicleRepository.findByIdForUpdate(VEHICULE_ID)).thenReturn(Optional.of(vehicule));

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> service.placeVehicleOrder(JOUEUR, 1, VEHICULE_ID, List.of(1, 2), board));

        assertTrue(erreur.getMessage().contains("secteur de départ"));
    }

    @Test
    @DisplayName("Refuse un véhicule détruit")
    void shouldRejectDestroyedVehicle() {
        vehicule.setDestroyed(true);
        when(vehicleRepository.findByIdForUpdate(VEHICULE_ID)).thenReturn(Optional.of(vehicule));

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> service.placeVehicleOrder(JOUEUR, 1, VEHICULE_ID, List.of(1, 2), board));

        assertTrue(erreur.getMessage().contains("ne peut pas se déplacer"));
    }

    @Test
    @DisplayName("Refuse une route plus longue que la vitesse du véhicule")
    void shouldRejectRouteLongerThanVehicleSpeed() {
        Vehicle tank = new Vehicle(VehicleType.TANK, JOUEUR);
        tank.setId(202L);
        tank.setSector(board.getSector(1));
        Unit pilote = new Unit(10.0, UnitClass.PILOTE_DESTRUCTEUR);
        pilote.setId(302L);
        pilote.setPlayerId(JOUEUR);
        tank.assignPilot(pilote);
        when(vehicleRepository.findByIdForUpdate(202L)).thenReturn(Optional.of(tank));

        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> service.placeVehicleOrder(JOUEUR, 1, 202L, List.of(1, 2, 3), board));

        assertTrue(erreur.getMessage().contains("ne peut parcourir que 1"));
    }
}
