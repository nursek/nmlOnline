package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.alliance.Alliance;
import com.mg.nmlonline.domain.model.alliance.Announcement;
import com.mg.nmlonline.domain.model.alliance.AnnouncementType;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.AllianceRepository;
import com.mg.nmlonline.infrastructure.repository.AnnouncementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FinalDuelService — dernière alliance")
class FinalDuelServiceTest {

    private final AllianceRepository allianceRepository = mock(AllianceRepository.class);
    private final AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
    private final FinalDuelService service =
            new FinalDuelService(allianceRepository, announcementRepository);

    @Test
    void breaksTheAllianceWhenOnlyTwoAlliesRemain() {
        Board board = new Board();
        board.setId(1L);
        Player first = player(2L, "nursek");
        Player second = player(3L, "lurio");
        Alliance alliance = Alliance.create(2L, 3L, 1L, 4);
        when(allianceRepository.findActiveBetween(2L, 3L)).thenReturn(Optional.of(alliance));

        service.breakLastAllianceIfNeeded(board, List.of(first, second), 7);

        assertEquals(7, alliance.getEndedTurn());
        assertNull(alliance.getEndedByPlayerId());
        assertFalse(alliance.isBetrayal());
        verify(allianceRepository).save(alliance);
        ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(captor.capture());
        Announcement announcement = captor.getValue();
        assertEquals(AnnouncementType.FINAL_DUEL, announcement.getType());
        assertEquals(1L, announcement.getBoardId());
        assertEquals(2L, announcement.getActorPlayerId());
        assertEquals(3L, announcement.getTargetPlayerId());
        assertEquals(7, announcement.getTurnCreated());
        assertEquals(8, announcement.getVisibleAtTurn());
    }

    @Test
    void ignoresTwoPlayersWithoutAlliance() {
        Board board = new Board();
        board.setId(1L);
        when(allianceRepository.findActiveBetween(2L, 3L)).thenReturn(Optional.empty());

        service.breakLastAllianceIfNeeded(board, List.of(player(2L, "nursek"), player(3L, "lurio")), 7);

        verify(announcementRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresMoreThanTwoSurvivors() {
        Board board = new Board();
        board.setId(1L);

        service.breakLastAllianceIfNeeded(board, List.of(player(1L, "a"), player(2L, "b"),
                player(3L, "c")), 7);

        verify(allianceRepository, never()).findActiveBetween(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void ignoresAlliesWhenOnlyOneIsAlive() {
        Board board = new Board();
        board.setId(1L);
        Player eliminated = player(3L, "lurio");
        eliminated.eliminate(com.mg.nmlonline.domain.model.player.EliminationCause.MAIN_CHARACTER_KILLED, 7);

        service.breakLastAllianceIfNeeded(board, List.of(player(2L, "nursek"), eliminated), 7);

        verify(allianceRepository, never()).findActiveBetween(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    private static Player player(Long id, String name) {
        Player player = new Player(name);
        player.setId(id);
        return player;
    }
}
