package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GazetteEditorNotesDto;
import com.mg.nmlonline.api.dto.GazetteIssueDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.domain.model.alliance.Announcement;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.SectorCapture;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.press.EliminationEvent;
import com.mg.nmlonline.infrastructure.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class GazetteDraftFactory {

    private static final int MAX_FRONT_PAGE = 3;

    private final AnnouncementRepository announcementRepository;

    public GazetteDraftFactory(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    public GazetteDraft build(Board board, int turn, List<EliminationEvent> events,
                              GameEndService.GameOutcome outcome, List<SectorCapture> captures,
                              List<Player> players, List<ResolvedBattleDto> battles) {
        Map<Long, String> names = new HashMap<>();
        players.forEach(player -> names.put(player.getId(), player.getName()));

        List<EliminationEvent> ordered = events.stream()
                .sorted(Comparator.comparingInt(EliminationEvent::getDeathIndex).reversed())
                .toList();
        List<GazetteIssueDto.ArticleDto> frontPage = new ArrayList<>();
        List<GazetteIssueDto.ArticleDto> briefs = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            EliminationEvent event = ordered.get(i);
            GazetteIssueDto.ArticleDto article = new GazetteIssueDto.ArticleDto();
            article.setKind("ELIMINATION");
            article.setPhotoPlayerName(event.getVictimName());
            if (i < MAX_FRONT_PAGE) {
                article.setSection("LA UNE");
                article.setHeadline("ÉLIMINATION — " + event.getVictimName());
                frontPage.add(article);
            } else {
                article.setSection("LES AUTRES FAITS");
                article.setHeadline("AUSSI — " + event.getVictimName());
                briefs.add(article);
            }
        }

        GazetteEditorNotesDto notes = new GazetteEditorNotesDto();
        notes.setEliminations(eliminations(events, names));
        notes.setBattles(battles(battles));
        notes.setCaptures(captures(captures, names));
        notes.setAnnouncements(announcements(board.getId(), turn, names));
        notes.setPlayers(players(players, board));
        notes.setOutcome(outcome(outcome));
        return new GazetteDraft(frontPage, briefs, notes, outcome.finished());
    }

    private static List<GazetteEditorNotesDto.EliminationNote> eliminations(
            List<EliminationEvent> events, Map<Long, String> names) {
        List<GazetteEditorNotesDto.EliminationNote> notes = new ArrayList<>();
        for (EliminationEvent event : events) {
            GazetteEditorNotesDto.EliminationNote note = new GazetteEditorNotesDto.EliminationNote();
            note.setVictimName(event.getVictimName());
            note.setEliminatedTurn(event.getEliminatedTurn());
            note.setCause(event.getCause() != null ? event.getCause().name() : null);
            note.setKillerName(event.getKillerName());
            note.setAssists(event.assistIds().stream()
                    .map(id -> names.getOrDefault(id, "#" + id))
                    .toList());
            note.setCharacterName(event.getVictimCharacterName());
            note.setBattleSector(event.getBattleSector());
            notes.add(note);
        }
        return notes;
    }

    private static List<GazetteEditorNotesDto.BattleNote> battles(List<ResolvedBattleDto> battles) {
        List<GazetteEditorNotesDto.BattleNote> notes = new ArrayList<>();
        for (ResolvedBattleDto battle : battles) {
            GazetteEditorNotesDto.BattleNote note = new GazetteEditorNotesDto.BattleNote();
            note.setSectorNumber(battle.getSectorNumber());
            note.setStandoff(battle.isStandoff());
            note.setWinnerName(battle.getWinnerName());
            note.setAttackerName(battle.getAttackerName());
            note.setDefenderName(battle.getDefenderName());
            note.setAttackerCasualties(battle.getAttackerCasualties());
            note.setDefenderCasualties(battle.getDefenderCasualties());
            note.setAttackerCharacterLost(battle.isAttackerCharacterLost());
            note.setDefenderCharacterLost(battle.isDefenderCharacterLost());
            note.setHeadquartersCaptured(battle.isDefenderHeadquartersCaptured());
            note.setLog(battle.getBattleLog() == null ? List.of()
                    : battle.getBattleLog().stream()
                            .map(ResolvedBattleDto.BattleLogEntryDto::getMessage)
                            .toList());
            notes.add(note);
        }
        return notes;
    }

    private static List<GazetteEditorNotesDto.CaptureNote> captures(
            List<SectorCapture> captures, Map<Long, String> names) {
        List<GazetteEditorNotesDto.CaptureNote> notes = new ArrayList<>();
        for (SectorCapture capture : captures) {
            GazetteEditorNotesDto.CaptureNote note = new GazetteEditorNotesDto.CaptureNote();
            note.setSectorNumber(capture.sectorNumber());
            note.setOwnerName(names.getOrDefault(capture.playerId(), "#" + capture.playerId()));
            note.setOnTheFly(capture.onTheFly());
            notes.add(note);
        }
        return notes;
    }

    private List<GazetteEditorNotesDto.AnnouncementNote> announcements(Long boardId, int turn,
                                                                       Map<Long, String> names) {
        return announcementRepository.findByBoardIdAndTurnCreatedOrderByIdAsc(boardId, turn).stream()
                .map(announcement -> announcement(announcement, names))
                .toList();
    }

    private static GazetteEditorNotesDto.AnnouncementNote announcement(Announcement announcement,
                                                                       Map<Long, String> names) {
        GazetteEditorNotesDto.AnnouncementNote note = new GazetteEditorNotesDto.AnnouncementNote();
        note.setType(announcement.getType().name());
        note.setActorName(names.getOrDefault(announcement.getActorPlayerId(),
                "#" + announcement.getActorPlayerId()));
        note.setTargetName(announcement.getTargetPlayerId() == null ? null
                : names.getOrDefault(announcement.getTargetPlayerId(),
                        "#" + announcement.getTargetPlayerId()));
        note.setTurnCreated(announcement.getTurnCreated());
        return note;
    }

    private static List<GazetteEditorNotesDto.PlayerNote> players(List<Player> players,
                                                                   Board board) {
        return players.stream()
                .map(player -> {
                    GazetteEditorNotesDto.PlayerNote note = new GazetteEditorNotesDto.PlayerNote();
                    note.setName(player.getName());
                    note.setAlive(!player.isEliminated());
                    note.setSectorCount(board.getSectorsByOwner(player.getId()).size());
                    return note;
                })
                .toList();
    }

    private static GazetteEditorNotesDto.OutcomeNote outcome(GameEndService.GameOutcome outcome) {
        GazetteEditorNotesDto.OutcomeNote note = new GazetteEditorNotesDto.OutcomeNote();
        note.setFinished(outcome.finished());
        note.setWinnerName(outcome.winnerName());
        note.setPosthumous(outcome.posthumous());
        note.setAdjusted(outcome.winnerAdjusted());
        return note;
    }

    public record GazetteDraft(List<GazetteIssueDto.ArticleDto> frontPage,
                               List<GazetteIssueDto.ArticleDto> briefs,
                               GazetteEditorNotesDto editorNotes,
                               boolean finalEdition) {
    }
}
