package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.api.dto.GazetteEditorNotesDto;
import com.mg.nmlonline.api.dto.GazetteIssueDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.domain.model.alliance.Announcement;
import com.mg.nmlonline.domain.model.alliance.AnnouncementType;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.SectorCapture;
import com.mg.nmlonline.domain.model.player.EliminationCause;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.player.PlayerRace;
import com.mg.nmlonline.domain.model.press.EliminationEvent;
import com.mg.nmlonline.infrastructure.repository.AnnouncementRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GazetteDraftFactory")
class GazetteDraftFactoryTest {

    private final AnnouncementRepository announcementRepository = mock(AnnouncementRepository.class);
    private final GazetteDraftFactory factory = new GazetteDraftFactory(announcementRepository);

    @Test
    void prefillsTheFrontPageAndKeepsPrivateFactsInAdminNotes() {
        Board board = board();
        List<Player> players = List.of(player(1L, "nursek"), player(2L, "lurio"),
                player(3L, "trazyn"));
        List<EliminationEvent> events = List.of(
                event(2L, "lurio", 0, EliminationCause.MAIN_CHARACTER_KILLED, 1L, "nursek",
                        List.of(3L)),
                event(3L, "trazyn", 1, EliminationCause.HEADQUARTERS_CAPTURED, 2L, "lurio",
                        List.of()));
        ResolvedBattleDto battle = battle();
        when(announcementRepository.findByBoardIdAndTurnCreatedOrderByIdAsc(1L, 5))
                .thenReturn(List.of(Announcement.create(AnnouncementType.ALLIANCE_FORMED, 1L, 1L,
                        2L, 5)));

        GazetteDraftFactory.GazetteDraft draft = factory.build(board, 5, events, outcome(false),
                List.of(new SectorCapture(43, 1L, false)), players, List.of(battle));

        assertEquals(2, draft.frontPage().size());
        assertEquals("ÉLIMINATION — trazyn", draft.frontPage().getFirst().getHeadline());
        assertEquals("trazyn", draft.frontPage().getFirst().getPhotoPlayerName());
        assertEquals("LA UNE", draft.frontPage().getFirst().getSection());
        assertTrue(draft.briefs().isEmpty());
        GazetteEditorNotesDto notes = draft.editorNotes();
        assertEquals(2, notes.getEliminations().size());
        assertEquals(List.of("trazyn"), notes.getEliminations().getFirst().getAssists());
        assertEquals(1, notes.getBattles().size());
        assertEquals(1, notes.getCaptures().size());
        assertEquals(1, notes.getAnnouncements().size());
        assertEquals(3, notes.getPlayers().size());
        assertFalse(notes.getOutcome().isFinished());
        assertFalse(draft.finalEdition());
        assertEquals("nursek", notes.getBattles().getFirst().getWinnerName());
        assertEquals("nursek", notes.getCaptures().getFirst().getOwnerName());
    }

    @Test
    void capsTheFrontPageAtThreeAndMovesTheRestToBriefs() {
        Board board = board();
        List<Player> players = List.of(player(1L, "a"), player(2L, "b"), player(3L, "c"),
                player(4L, "d"), player(5L, "e"));
        List<EliminationEvent> events = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            events.add(event((long) i + 1, "victime" + i, i, EliminationCause.MAIN_CHARACTER_KILLED,
                    1L, "a", List.of()));
        }
        when(announcementRepository.findByBoardIdAndTurnCreatedOrderByIdAsc(1L, 5))
                .thenReturn(List.of());

        GazetteDraftFactory.GazetteDraft draft = factory.build(board, 5, events, outcome(true),
                List.of(), players, List.of());

        assertEquals(3, draft.frontPage().size());
        assertEquals(1, draft.briefs().size());
        assertEquals("AUSSI — victime1", draft.briefs().getFirst().getHeadline());
        assertTrue(draft.finalEdition());
        assertTrue(draft.editorNotes().getOutcome().isFinished());
    }

    private static Board board() {
        Board board = new Board();
        board.setId(1L);
        board.setName("Carte Principale");
        return board;
    }

    private static Player player(Long id, String name) {
        Player player = new Player(name);
        player.setId(id);
        player.setRace(PlayerRace.ORKS);
        return player;
    }

    private static EliminationEvent event(Long victimId, String victimName, int index,
                                          EliminationCause cause, Long killerId, String killerName,
                                          List<Long> assists) {
        EliminationEvent event = new EliminationEvent();
        event.setVictimPlayerId(victimId);
        event.setVictimName(victimName);
        event.setVictimRace(PlayerRace.ORKS);
        event.setVictimCharacterName(victimName + "-char");
        event.setDeathIndex(index);
        event.setEliminatedTurn(5);
        event.setCause(cause);
        event.setKillerPlayerId(killerId);
        event.setKillerName(killerName);
        event.setAssistPlayerIds(assists.isEmpty() ? null
                : String.join(",", assists.stream().map(String::valueOf).toList()));
        event.setBattleSector(43);
        return event;
    }

    private static ResolvedBattleDto battle() {
        ResolvedBattleDto battle = new ResolvedBattleDto();
        battle.setSectorNumber(43);
        battle.setWinnerName("nursek");
        battle.setAttackerName("nursek");
        battle.setDefenderName("lurio");
        battle.setAttackerCasualties(2);
        battle.setDefenderCasualties(5);
        battle.setDefenderCharacterLost(true);
        ResolvedBattleDto.BattleLogEntryDto entry = new ResolvedBattleDto.BattleLogEntryDto();
        entry.setMessage("Le QG tombe.");
        battle.setBattleLog(List.of(entry));
        return battle;
    }

    private static GameEndService.GameOutcome outcome(boolean finished) {
        return new GameEndService.GameOutcome(finished, 5, finished ? 1L : null,
                finished ? "nursek" : null, false, false);
    }
}
