package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.sniper.SniperOrder;
import com.mg.nmlonline.domain.model.battle.RandomSource;
import com.mg.nmlonline.domain.model.sniper.SniperOrderStatus;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitClass;
import com.mg.nmlonline.infrastructure.repository.SniperOrderRepository;
import com.mg.nmlonline.infrastructure.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SniperService — collecte du soutien pendant un combat")
class SniperServiceTest {

    private static final int TURN = 1;
    private static final Long BOARD_ID = 1L;
    private static final Long SNIPER_ID = 10L;
    private static final Long PLAYER_ID = 1L;
    private static final int FROM_SECTOR = 4;
    private static final int TARGET_SECTOR = 5;

    @Mock
    private SniperOrderRepository sniperOrderRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private AllianceGraph allianceGraph;
    @Mock
    private RandomSource randomSource;

    @InjectMocks
    private SniperService service;

    private Unit sniper;
    private SniperOrder order;

    @BeforeEach
    void setUp() {
        sniper = new Unit(0.0, UnitClass.SNIPER);
        sniper.setId(SNIPER_ID);
        sniper.setPlayerId(PLAYER_ID);
        Board board = new Board();
        board.setId(BOARD_ID);
        Sector home = new Sector(FROM_SECTOR, "Origine");
        board.addSector(home);
        sniper.setSector(home);

        order = SniperOrder.create(BOARD_ID, PLAYER_ID, TURN, SNIPER_ID, FROM_SECTOR, TARGET_SECTOR);
        order.setId(99L);
        lenient().when(sniperOrderRepository.findByBoardIdAndTurnAndStatus(BOARD_ID, TURN, SniperOrderStatus.PENDING))
                .thenReturn(List.of(order));
        lenient().when(unitRepository.findById(SNIPER_ID)).thenReturn(Optional.of(sniper));
        lenient().when(allianceGraph.areAllied(any(), any(), any())).thenReturn(false);
    }

    @Test
    @DisplayName("Soutien attaquant quand le sniper est dans le camp attaquant")
    void supportsAttackerCamp() {
        SniperService.DuelSupport support = service.collectDuelSupport(
                BOARD_ID, TURN, TARGET_SECTOR, List.of(PLAYER_ID), List.of(2L), Set.of());

        assertEquals(List.of(sniper), support.attackers());
        assertTrue(support.defenders().isEmpty());
        assertEquals(List.of(order), support.resolvedOrders());
    }

    @Test
    @DisplayName("Soutien défenseur, y compris via une alliance, et impasse par index de camp")
    void supportsDefenderCampAndStandoff() {
        SniperService.DuelSupport support = service.collectDuelSupport(
                BOARD_ID, TURN, TARGET_SECTOR, List.of(2L), List.of(3L), Set.of());
        assertTrue(support.attackers().isEmpty());
        assertTrue(support.defenders().isEmpty());
        assertEquals(1, support.independents().size());
        assertEquals(PLAYER_ID, support.independents().getFirst().playerId());
        assertTrue(support.resolvedOrders().contains(order));
        assertEquals(SniperOrderStatus.PENDING, order.getStatus(),
                "Hors camp : rattaché comme camp indépendant, pas encore résolu");

        when(allianceGraph.areAllied(BOARD_ID, 2L, PLAYER_ID)).thenReturn(true);
        SniperService.DuelSupport ally = service.collectDuelSupport(
                BOARD_ID, TURN, TARGET_SECTOR, List.of(2L), List.of(3L), Set.of());
        assertEquals(List.of(sniper), ally.attackers());

        when(allianceGraph.areAllied(any(), any(), any())).thenReturn(false);
        SniperService.StandoffSupport standoff = service.collectStandoffSupport(
                BOARD_ID, TURN, TARGET_SECTOR, List.of(List.of(2L), List.of(PLAYER_ID), List.of(3L)), Set.of());
        assertEquals(Map.of(1, List.of(sniper)), standoff.byCamp());
    }

    @Test
    @DisplayName("Annule quand le secteur d'origine est attaqué")
    void cancelsWhenHomeSectorIsAttacked() {
        SniperService.DuelSupport support = service.collectDuelSupport(
                BOARD_ID, TURN, TARGET_SECTOR, List.of(PLAYER_ID), List.of(2L), Set.of(FROM_SECTOR));

        assertTrue(support.attackers().isEmpty());
        assertEquals(SniperOrderStatus.CANCELLED, order.getStatus());
        assertTrue(order.getStatusMessage().contains("attaqué"));
        verify(sniperOrderRepository).save(order);
    }

    @Test
    @DisplayName("Annule quand l'unité a quitté son secteur ou n'est plus une sniper")
    void cancelsWhenUnitMovedOrNotSniper() {
        Board board = new Board();
        board.setId(BOARD_ID);
        Sector elsewhere = new Sector(6, "Ailleurs");
        board.addSector(elsewhere);
        sniper.setSector(elsewhere);
        service.collectDuelSupport(BOARD_ID, TURN, TARGET_SECTOR, List.of(PLAYER_ID), List.of(2L), Set.of());
        assertEquals(SniperOrderStatus.CANCELLED, order.getStatus());

        SniperOrder second = SniperOrder.create(BOARD_ID, PLAYER_ID, TURN, SNIPER_ID, FROM_SECTOR, TARGET_SECTOR);
        when(sniperOrderRepository.findByBoardIdAndTurnAndStatus(BOARD_ID, TURN, SniperOrderStatus.PENDING))
                .thenReturn(List.of(second));
        Unit notSniper = new Unit(0.0, UnitClass.LEGER);
        notSniper.setId(SNIPER_ID);
        notSniper.setSector(elsewhere);
        when(unitRepository.findById(SNIPER_ID)).thenReturn(Optional.of(notSniper));

        service.collectDuelSupport(BOARD_ID, TURN, TARGET_SECTOR, List.of(PLAYER_ID), List.of(2L), Set.of());
        assertEquals(SniperOrderStatus.CANCELLED, second.getStatus());
    }

    @Test
    @DisplayName("Marque les ordres utilisés comme résolus")
    void marksResolvedOrders() {
        SniperService.DuelSupport support = service.collectDuelSupport(
                BOARD_ID, TURN, TARGET_SECTOR, List.of(PLAYER_ID), List.of(2L), Set.of());

        service.markResolved(support.resolvedOrders());

        assertEquals(SniperOrderStatus.RESOLVED, order.getStatus());
        verify(sniperOrderRepository).saveAll(List.of(order));
    }

    @Test
    @DisplayName("Aucun ordre quand la cible ne correspond pas")
    void ignoresOrdersForOtherSectors() {
        SniperService.DuelSupport support = service.collectDuelSupport(
                BOARD_ID, TURN, 6, List.of(PLAYER_ID), List.of(2L), Set.of());

        assertTrue(support.attackers().isEmpty());
        assertTrue(support.defenders().isEmpty());
        assertFalse(support.resolvedOrders().contains(order));
        assertEquals(SniperOrderStatus.PENDING, order.getStatus());
    }
}
