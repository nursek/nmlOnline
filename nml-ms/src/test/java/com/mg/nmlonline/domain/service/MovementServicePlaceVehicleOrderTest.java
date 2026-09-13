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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Placement d'un ordre de véhicule")
class MovementServicePlaceVehicleOrderTest {

    @Mock
    MovementOrderRepository orderRepository;

    @Mock
    VehicleRepository vehicleRepository;

    @InjectMocks
    MovementService service;

    private static final Long JOUEUR = 1L;
    private static final Long VEHICULE_ID = 201L;

    private Board board;
    private Vehicle vehicule;

    @BeforeEach
    void setUp() {
        board = new Board();
        Sector depart = new Sector(1, "Départ");
        Sector arrivee = new Sector(2, "Arrivée");
        board.addSector(depart);
        board.addSector(arrivee);
        depart.addNeighbor(2);
        arrivee.addNeighbor(1);

        vehicule = new Vehicle(VehicleType.VTT_LEGER, JOUEUR);
        vehicule.setId(VEHICULE_ID);
        vehicule.setSector(depart);
        depart.getVehicles().add(vehicule);

        Unit pilote = new Unit(10.0, UnitClass.PILOTE_DESTRUCTEUR);
        pilote.setId(301L);
        pilote.setPlayerId(JOUEUR);
        vehicule.assignPilot(pilote);
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
}
