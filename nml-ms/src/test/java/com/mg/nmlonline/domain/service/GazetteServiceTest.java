package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GazetteAdminIssueDto;
import com.mg.nmlonline.api.dto.GazetteContentRequestDto;
import com.mg.nmlonline.api.dto.GazetteEditorNotesDto;
import com.mg.nmlonline.api.dto.GazetteIssueDto;
import com.mg.nmlonline.domain.model.audit.AdminAuditAction;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.press.GazetteIssue;
import com.mg.nmlonline.domain.model.press.GazetteStatus;
import com.mg.nmlonline.infrastructure.repository.GazetteIssueRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GazetteService")
class GazetteServiceTest {

    private final GazetteIssueRepository repository = mock(GazetteIssueRepository.class);
    private final GazetteMastheadService mastheadService = mock(GazetteMastheadService.class);
    private final AdminAuditService adminAuditService = mock(AdminAuditService.class);
    private final GazetteService service = new GazetteService(repository, mastheadService,
            adminAuditService, new ObjectMapper());

    private static Board board() {
        Board board = new Board();
        board.setId(1L);
        board.setName("Carte Principale");
        return board;
    }

    private static GazetteIssueDto.MastheadDto masthead() {
        GazetteIssueDto.MastheadDto masthead = new GazetteIssueDto.MastheadDto();
        masthead.setJournalName("Brigade des Morts");
        masthead.setFakeUrl("www.brigade-des-morts.nml/vol-5/n-1/le-front");
        masthead.setVolume(5);
        return masthead;
    }

    private static GazetteIssueDto.ArticleDto article(String headline, String body, String author) {
        GazetteIssueDto.ArticleDto article = new GazetteIssueDto.ArticleDto();
        article.setKind("ELIMINATION");
        article.setSection("LA UNE");
        article.setHeadline(headline);
        article.setBody(body);
        article.setAuthor(author);
        return article;
    }

    private GazetteAdminIssueDto createDraft(AtomicReference<GazetteIssue> saved) {
        when(repository.findByBoardIdAndTurn(1L, 5)).thenReturn(Optional.empty());
        when(repository.findMaxIssueNumberByBoardId(1L)).thenReturn(0);
        when(mastheadService.generate(any(Board.class), eq(5), eq(1), any(Instant.class)))
                .thenReturn(masthead());
        when(repository.save(any(GazetteIssue.class))).thenAnswer(invocation -> {
            GazetteIssue issue = invocation.getArgument(0);
            saved.set(issue);
            return issue;
        });
        GazetteEditorNotesDto notes = new GazetteEditorNotesDto();
        notes.setOutcome(new GazetteEditorNotesDto.OutcomeNote());
        GazetteDraftFactory.GazetteDraft draft = new GazetteDraftFactory.GazetteDraft(
                List.of(article("ÉLIMINATION — lurio", "", "")), List.of(), notes, false);
        return service.createDraft(board(), 5, draft);
    }

    @Test
    void createsADraftWithPersistedMastheadAndAdminNotes() {
        AtomicReference<GazetteIssue> saved = new AtomicReference<>();

        GazetteAdminIssueDto admin = createDraft(saved);

        GazetteIssue issue = saved.get();
        assertNotNull(issue);
        assertEquals(GazetteStatus.DRAFT, issue.getStatus());
        assertEquals(1, issue.getIssueNumber());
        assertEquals(5, issue.getTurn());
        assertTrue(issue.getPayload().contains("Brigade des Morts"));
        assertNotNull(issue.getEditorNotes());
        assertEquals("DRAFT", admin.getStatus());
        assertNotNull(admin.getIssue());
        assertEquals("Brigade des Morts", admin.getIssue().getMasthead().getJournalName());
    }

    @Test
    void publishesACompleteIssueAndAudits() {
        AtomicReference<GazetteIssue> saved = new AtomicReference<>();
        createDraft(saved);
        GazetteIssue issue = saved.get();
        when(repository.findByBoardIdAndTurn(1L, 5)).thenReturn(Optional.of(issue));

        GazetteContentRequestDto content = new GazetteContentRequestDto();
        content.setFrontPage(List.of(article("TITRE", "Corps de l'article", "Auteur")));
        content.setBriefs(List.of());
        GazetteAdminIssueDto published = service.publish(1L, 5, content, 9L);

        assertEquals(GazetteStatus.PUBLISHED, issue.getStatus());
        assertNotNull(issue.getPublishedAt());
        assertEquals("TITRE", issue.getMainHeadline());
        assertEquals(9L, issue.getPublishedByUserId());
        assertEquals("PUBLISHED", published.getStatus());
        assertEquals(1, published.getIssue().getFrontPage().size());
        assertNotNull(published.getIssue().getPublishedAt());
        verify(adminAuditService).record(eq(1L), eq(null), eq(AdminAuditAction.GAZETTE_PUBLISHED),
                any());
    }

    @Test
    void refusesToPublishWithoutACompleteFrontPage() {
        AtomicReference<GazetteIssue> saved = new AtomicReference<>();
        createDraft(saved);
        GazetteIssue issue = saved.get();
        when(repository.findByBoardIdAndTurn(1L, 5)).thenReturn(Optional.of(issue));
        GazetteContentRequestDto content = new GazetteContentRequestDto();
        content.setFrontPage(List.of(article("TITRE", "", "")));

        assertThrows(IllegalArgumentException.class, () -> service.publish(1L, 5, content, 9L));
        assertEquals(GazetteStatus.DRAFT, issue.getStatus());
    }

    @Test
    void hidesDraftsFromThePublicEndpoint() {
        AtomicReference<GazetteIssue> saved = new AtomicReference<>();
        createDraft(saved);
        GazetteIssue issue = saved.get();
        when(repository.findByBoardIdAndTurn(1L, 5)).thenReturn(Optional.of(issue));

        assertThrows(EntityNotFoundException.class, () -> service.getPublished(1L, 5));
    }

    @Test
    void refusesToDeleteAPublishedIssue() {
        AtomicReference<GazetteIssue> saved = new AtomicReference<>();
        createDraft(saved);
        GazetteIssue issue = saved.get();
        issue.setStatus(GazetteStatus.PUBLISHED);
        when(repository.findByBoardIdAndTurn(1L, 5)).thenReturn(Optional.of(issue));

        assertThrows(IllegalStateException.class, () -> service.deleteDraft(1L, 5));
        verify(repository, never()).delete(any(GazetteIssue.class));
    }

    @Test
    void updatesAPublishedIssueWithoutChangingThePublicationDate() {
        AtomicReference<GazetteIssue> saved = new AtomicReference<>();
        createDraft(saved);
        GazetteIssue issue = saved.get();
        issue.setStatus(GazetteStatus.PUBLISHED);
        Instant publishedAt = Instant.parse("2026-11-13T20:14:00Z");
        issue.setPublishedAt(publishedAt);
        when(repository.findByBoardIdAndTurn(1L, 5)).thenReturn(Optional.of(issue));
        GazetteContentRequestDto content = new GazetteContentRequestDto();
        content.setFrontPage(List.of(article("TITRE CORRIGÉ", "Corps", "Auteur")));

        service.update(1L, 5, content);

        assertEquals(publishedAt, issue.getPublishedAt());
        assertEquals("TITRE CORRIGÉ", issue.getMainHeadline());
        verify(adminAuditService).record(eq(1L), eq(null), eq(AdminAuditAction.GAZETTE_UPDATED),
                any());
    }

    @Test
    void listsOnlyPublishedIssuesForPlayers() {
        GazetteIssue published = new GazetteIssue();
        published.setBoardId(1L);
        published.setTurn(5);
        published.setIssueNumber(1);
        published.setStatus(GazetteStatus.PUBLISHED);
        published.setPayload("{}");
        when(repository.findByBoardIdAndStatusOrderByTurnDesc(1L, GazetteStatus.PUBLISHED))
                .thenReturn(List.of(published));

        assertEquals(1, service.listPublished(1L).size());
        assertEquals("PUBLISHED", service.listPublished(1L).getFirst().status());
    }
}
