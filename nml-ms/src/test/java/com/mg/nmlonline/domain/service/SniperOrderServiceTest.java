package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.SniperOrderDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sniper.SniperOrder;
import com.mg.nmlonline.domain.model.sniper.SniperOrderStatus;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.MovementOrderRepository;
import com.mg.nmlonline.infrastructure.repository.SniperOrderRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import com.mg.nmlonline.infrastructure.repository.VehicleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ordres sniper — pose, validation et annulation")
class SniperOrderServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long PLAYER_ID = 1L;
    private static final Long UNIT_ID = 10L;
    private static final int TURN = 1;

    @Mock
    private PlayerService playerService;
    @Mock
    private BoardService boardService;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private SniperOrderRepository sniperOrderRepository;
    @Mock
    private MovementOrderRepository movementOrderRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private TurnService turnService;
    @Mock
    private TurnLock turnLock;
    @Mock
    private AllianceGraph allianceGraph;

    @InjectMocks
    private SniperOrderService service;

    private Board board;
    private Player player;
    private Unit sniper;
    private Sector from;

    @BeforeEach
    void setUp() {
        board = new Board();
        board.setId(1L);
        board.setName("Plateau");
        from = new Sector(1, "Un");
        Sector adjacent = new Sector(2, "Deux");
        Sector far = new Sector(3, "Trois");
        board.addSector(from);
        board.addSector(adjacent);
        board.addSector(far);
        from.addNeighbor(2);
        adjacent.addNeighbor(1);

        player = new Player("Joueur");
        player.setId(PLAYER_ID);

        sniper = new Unit(0.0, UnitClass.SNIPER);
        sniper.setId(UNIT_ID);
        sniper.setPlayerId(PLAYER_ID);
        sniper.setSector(from);
        from.getArmy().add(sniper);

        lenient().when(turnLock.isLocked()).thenReturn(false);
        lenient().when(playerService.findByUserIdForUpdate(USER_ID)).thenReturn(player);
        lenient().when(playerService.findByUserId(USER_ID)).thenReturn(player);
        lenient().when(boardService.getAllBoards()).thenReturn(List.of(board));
        lenient().when(turnService.getCurrentTurn()).thenReturn(TURN);
        lenient().when(unitRepository.findById(UNIT_ID)).thenReturn(Optional.of(sniper));
        lenient().when(vehicleRepository.existsByPilot_Id(UNIT_ID)).thenReturn(false);
        lenient().when(vehicleRepository.existsByPassengers_Id(UNIT_ID)).thenReturn(false);
        lenient().when(movementOrderRepository.findPendingEntityIds(eq(TURN), anyList()))
                .thenReturn(List.of());
        lenient().when(sniperOrderRepository.existsByUnitIdAndTurnAndStatus(
                UNIT_ID, TURN, SniperOrderStatus.PENDING)).thenReturn(false);
        lenient().when(allianceGraph.areAllied(anyLong(), anyLong())).thenReturn(false);
        lenient().when(sniperOrderRepository.save(any())).thenAnswer(invocation -> {
            SniperOrder saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
    }

    @Test
    @DisplayName("Pose l'ordre sur un secteur adjacent et dérive Tir de précision / Surveillance")
    void placesOrderAndDerivesAction() {
        SniperOrderDto neutral = service.place(USER_ID, UNIT_ID, 2);
        assertEquals("PENDING", neutral.getStatus());
        assertEquals(1, neutral.getFromSectorNumber());
        assertEquals(2, neutral.getTargetSectorNumber());
        assertEquals("TIR_PRECISION", neutral.getAction());

        board.getSector(2).setOwnerId(PLAYER_ID);
        assertEquals("SURVEILLANCE", service.place(USER_ID, UNIT_ID, 2).getAction());

        board.getSector(2).setOwnerId(55L);
        when(allianceGraph.areAllied(PLAYER_ID, 55L)).thenReturn(true);
        assertEquals("SURVEILLANCE", service.place(USER_ID, UNIT_ID, 2).getAction());
    }

    @Test
    @DisplayName("Refuse un secteur identique, non adjacent ou inconnu")
    void rejectsInvalidTargetSector() {
        assertThrows(IllegalArgumentException.class, () -> service.place(USER_ID, UNIT_ID, 1));
        assertThrows(IllegalArgumentException.class, () -> service.place(USER_ID, UNIT_ID, 3));
        assertThrows(EntityNotFoundException.class, () -> service.place(USER_ID, UNIT_ID, 99));
    }

    @Test
    @DisplayName("Refuse une unité non sniper, étrangère ou embarquée")
    void rejectsIneligibleUnit() {
        Unit leger = new Unit(0.0, UnitClass.LEGER);
        leger.setId(UNIT_ID);
        leger.setPlayerId(PLAYER_ID);
        leger.setSector(from);
        when(unitRepository.findById(UNIT_ID)).thenReturn(Optional.of(leger));
        assertThrows(IllegalArgumentException.class, () -> service.place(USER_ID, UNIT_ID, 2));

        lenient().when(unitRepository.findById(UNIT_ID)).thenReturn(Optional.of(sniper));
        sniper.setPlayerId(99L);
        assertThrows(SecurityException.class, () -> service.place(USER_ID, UNIT_ID, 2));
        sniper.setPlayerId(PLAYER_ID);

        when(vehicleRepository.existsByPilot_Id(UNIT_ID)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.place(USER_ID, UNIT_ID, 2));
    }

    @Test
    @DisplayName("Refuse un ordre de déplacement ou un ordre sniper déjà en attente")
    void rejectsConflictingOrders() {
        when(movementOrderRepository.findPendingEntityIds(TURN, List.of(UNIT_ID)))
                .thenReturn(List.of(UNIT_ID));
        assertThrows(IllegalArgumentException.class, () -> service.place(USER_ID, UNIT_ID, 2));

        when(movementOrderRepository.findPendingEntityIds(TURN, List.of(UNIT_ID)))
                .thenReturn(List.of());
        when(sniperOrderRepository.existsByUnitIdAndTurnAndStatus(
                UNIT_ID, TURN, SniperOrderStatus.PENDING)).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.place(USER_ID, UNIT_ID, 2));
    }

    @Test
    @DisplayName("Annule un ordre PENDING du propriétaire, refuse un autre statut ou un autre joueur")
    void cancelsPendingOrder() {
        SniperOrder order = SniperOrder.create(PLAYER_ID, TURN, UNIT_ID, 1, 2);
        order.setId(99L);
        when(sniperOrderRepository.findById(99L)).thenReturn(Optional.of(order));

        service.cancel(USER_ID, 99L);
        assertEquals(SniperOrderStatus.CANCELLED, order.getStatus());
        assertThrows(IllegalStateException.class, () -> service.cancel(USER_ID, 99L));

        SniperOrder foreign = SniperOrder.create(99L, TURN, UNIT_ID, 1, 2);
        foreign.setId(100L);
        when(sniperOrderRepository.findById(100L)).thenReturn(Optional.of(foreign));
        assertThrows(SecurityException.class, () -> service.cancel(USER_ID, 100L));
    }

    @Test
    @DisplayName("Liste les ordres PENDING du joueur pour le tour courant avec l'action calculée")
    void listsPendingOrdersWithAction() {
        SniperOrder order = SniperOrder.create(PLAYER_ID, TURN, UNIT_ID, 1, 2);
        order.setId(42L);
        board.getSector(2).setOwnerId(PLAYER_ID);
        when(sniperOrderRepository.findByPlayerIdAndTurnAndStatus(
                PLAYER_ID, TURN, SniperOrderStatus.PENDING)).thenReturn(List.of(order));

        List<SniperOrderDto> orders = service.currentPending(USER_ID);

        assertEquals(1, orders.size());
        assertEquals(42L, orders.getFirst().getId());
        assertEquals("SURVEILLANCE", orders.getFirst().getAction());
    }
}
