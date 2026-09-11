package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementOrder;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Placement d'un ordre de déplacement à pied")
class MovementServicePlaceFootOrderTest {

    @Mock
    MovementOrderRepository orderRepository;

    @Mock
    VehicleRepository vehicleRepository;

    @InjectMocks
    MovementService service;

    private static final Long JOUEUR = 1L;
    private static final Long UNITE_1_ID = 101L;
    private static final Long UNITE_2_ID = 102L;

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
        Sector depart = new Sector(1, "Départ");
        Sector arrivee = new Sector(2, "Arrivée");
        board.addSector(depart);
        board.addSector(arrivee);
        depart.addNeighbor(2);
        arrivee.addNeighbor(1);

        depart.getArmy().add(unite(UNITE_1_ID, depart));
        depart.getArmy().add(unite(UNITE_2_ID, depart));
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
}
