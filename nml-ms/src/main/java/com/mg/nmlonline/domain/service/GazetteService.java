package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.GazetteAdminIssueDto;
import com.mg.nmlonline.api.dto.GazetteContentRequestDto;
import com.mg.nmlonline.api.dto.GazetteEditorNotesDto;
import com.mg.nmlonline.api.dto.GazetteIssueDto;
import com.mg.nmlonline.api.dto.GazetteIssueSummaryDto;
import com.mg.nmlonline.domain.model.audit.AdminAuditAction;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.press.GazetteIssue;
import com.mg.nmlonline.domain.model.press.GazetteStatus;
import com.mg.nmlonline.infrastructure.repository.GazetteIssueRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class GazetteService {

    private static final int MAX_FRONT_PAGE = 3;
    private static final int MAX_BRIEFS = 4;
    private static final int MAX_HEADLINE = 120;
    private static final int MAX_QUOTE = 220;
    private static final int MAX_BODY = 4000;
    private static final int MAX_AUTHOR = 80;
    private static final int MAX_CAPTION = 160;

    private final GazetteIssueRepository repository;
    private final GazetteMastheadService mastheadService;
    private final AdminAuditService adminAuditService;
    private final ObjectMapper objectMapper;

    public GazetteService(GazetteIssueRepository repository, GazetteMastheadService mastheadService,
                          AdminAuditService adminAuditService, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mastheadService = mastheadService;
        this.adminAuditService = adminAuditService;
        this.objectMapper = objectMapper;
    }

    public GazetteAdminIssueDto createDraft(Board board, int turn, GazetteDraftFactory.GazetteDraft draft) {
        GazetteIssue existing = repository.findByBoardIdAndTurn(board.getId(), turn).orElse(null);
        if (existing != null) {
            return toAdmin(existing);
        }
        int issueNumber = repository.findMaxIssueNumberByBoardId(board.getId()) + 1;
        Instant now = Instant.now();

        GazetteIssueDto issue = new GazetteIssueDto();
        issue.setTurn(turn);
        issue.setIssueNumber(issueNumber);
        issue.setUpdatedAt(now.toString());
        issue.setFinalEdition(draft.finalEdition());
        issue.setMasthead(mastheadService.generate(board, turn, issueNumber, now));
        issue.setFrontPage(draft.frontPage());
        issue.setBriefs(draft.briefs());

        GazetteIssue entity = new GazetteIssue();
        entity.setBoardId(board.getId());
        entity.setTurn(turn);
        entity.setIssueNumber(issueNumber);
        entity.setStatus(GazetteStatus.DRAFT);
        entity.setPayload(write(issue));
        entity.setEditorNotes(write(draft.editorNotes()));
        entity.setFinalEdition(draft.finalEdition());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toAdmin(repository.save(entity));
    }

    public GazetteAdminIssueDto update(Long boardId, int turn, GazetteContentRequestDto request) {
        GazetteIssue issue = require(boardId, turn);
        GazetteIssueDto payload = read(issue);
        boolean published = issue.getStatus() == GazetteStatus.PUBLISHED;
        applyContent(payload, request, published);
        Instant now = Instant.now();
        payload.setUpdatedAt(now.toString());
        issue.setPayload(write(payload));
        issue.setUpdatedAt(now);
        issue.setMainHeadline(headline(payload));
        repository.save(issue);
        adminAuditService.record(boardId, null, AdminAuditAction.GAZETTE_UPDATED,
                "Édition tour " + turn + " mise à jour");
        return toAdmin(issue);
    }

    public GazetteAdminIssueDto publish(Long boardId, int turn, GazetteContentRequestDto request,
                                        Long userId) {
        GazetteIssue issue = require(boardId, turn);
        if (issue.getStatus() == GazetteStatus.PUBLISHED) {
            throw new IllegalStateException("Cette édition est déjà publiée");
        }
        GazetteIssueDto payload = read(issue);
        applyContent(payload, request, true);
        Instant now = Instant.now();
        payload.setPublishedAt(now.toString());
        payload.setUpdatedAt(now.toString());
        issue.setPayload(write(payload));
        issue.setStatus(GazetteStatus.PUBLISHED);
        issue.setMainHeadline(headline(payload));
        issue.setPublishedAt(now);
        issue.setUpdatedAt(now);
        issue.setPublishedByUserId(userId);
        repository.save(issue);
        adminAuditService.record(boardId, null, AdminAuditAction.GAZETTE_PUBLISHED,
                "Édition tour " + turn + " publiée");
        return toAdmin(issue);
    }

    public void deleteDraft(Long boardId, int turn) {
        GazetteIssue issue = require(boardId, turn);
        if (issue.getStatus() == GazetteStatus.PUBLISHED) {
            throw new IllegalStateException("Une édition publiée ne peut pas être supprimée");
        }
        repository.delete(issue);
        adminAuditService.record(boardId, null, AdminAuditAction.GAZETTE_DRAFT_DELETED,
                "Brouillon du tour " + turn + " supprimé");
    }

    @Transactional(readOnly = true)
    public List<GazetteIssueSummaryDto> listAdmin(Long boardId) {
        return repository.findByBoardIdOrderByTurnDesc(boardId).stream()
                .map(this::summary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GazetteIssueSummaryDto> listPublished(Long boardId) {
        return repository.findByBoardIdAndStatusOrderByTurnDesc(boardId, GazetteStatus.PUBLISHED)
                .stream()
                .map(this::summary)
                .toList();
    }

    @Transactional(readOnly = true)
    public GazetteAdminIssueDto getAdmin(Long boardId, int turn) {
        return toAdmin(require(boardId, turn));
    }

    @Transactional(readOnly = true)
    public GazetteIssueDto getPublished(Long boardId, int turn) {
        GazetteIssue issue = require(boardId, turn);
        if (issue.getStatus() != GazetteStatus.PUBLISHED) {
            throw new EntityNotFoundException("Édition non publiée pour le tour " + turn);
        }
        return read(issue);
    }

    @Transactional(readOnly = true)
    public Integer findLatestPublishedTurn(Long boardId) {
        return repository.findFirstByBoardIdAndStatusOrderByTurnDesc(boardId, GazetteStatus.PUBLISHED)
                .map(GazetteIssue::getTurn)
                .orElse(null);
    }

    private GazetteIssue require(Long boardId, int turn) {
        if (turn < 1) {
            throw new IllegalArgumentException("Tour invalide : " + turn);
        }
        return repository.findByBoardIdAndTurn(boardId, turn)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Aucune édition pour le tour " + turn));
    }

    private void applyContent(GazetteIssueDto payload, GazetteContentRequestDto request,
                              boolean published) {
        List<GazetteIssueDto.ArticleDto> frontPage =
                request.getFrontPage() == null ? List.of() : request.getFrontPage();
        List<GazetteIssueDto.ArticleDto> briefs =
                request.getBriefs() == null ? List.of() : request.getBriefs();
        if (frontPage.size() > MAX_FRONT_PAGE) {
            throw new IllegalArgumentException("Maximum " + MAX_FRONT_PAGE + " articles de une");
        }
        if (briefs.size() > MAX_BRIEFS) {
            throw new IllegalArgumentException("Maximum " + MAX_BRIEFS + " brèves");
        }
        frontPage.forEach(this::validateArticle);
        briefs.forEach(this::validateArticle);

        if (published) {
            List<GazetteIssueDto.ArticleDto> completeFrontPage = frontPage.stream()
                    .filter(GazetteService::completeMain)
                    .toList();
            if (completeFrontPage.isEmpty()) {
                throw new IllegalArgumentException(
                        "Au moins une une complète (titre, corps, auteur) est requise");
            }
            payload.setFrontPage(completeFrontPage);
            payload.setBriefs(briefs.stream().filter(GazetteService::completeBrief).toList());
        } else {
            payload.setFrontPage(frontPage);
            payload.setBriefs(briefs);
        }
    }

    private void validateArticle(GazetteIssueDto.ArticleDto article) {
        checkLength(article.getHeadline(), MAX_HEADLINE, "titre");
        checkLength(article.getQuote(), MAX_QUOTE, "citation");
        checkLength(article.getBody(), MAX_BODY, "corps");
        checkLength(article.getAuthor(), MAX_AUTHOR, "auteur");
        checkLength(article.getPhotoCaption(), MAX_CAPTION, "légende");
    }

    private static void checkLength(String value, int max, String label) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException("Le " + label + " dépasse " + max + " caractères");
        }
    }

    private static boolean completeMain(GazetteIssueDto.ArticleDto article) {
        return filled(article.getHeadline()) && filled(article.getBody()) && filled(article.getAuthor());
    }

    private static boolean completeBrief(GazetteIssueDto.ArticleDto article) {
        return filled(article.getHeadline()) && filled(article.getBody());
    }

    private static boolean filled(String value) {
        return value != null && !value.isBlank();
    }

    private static String headline(GazetteIssueDto payload) {
        return payload.getFrontPage() == null ? null : payload.getFrontPage().stream()
                .map(GazetteIssueDto.ArticleDto::getHeadline)
                .filter(GazetteService::filled)
                .findFirst()
                .orElse(null);
    }

    private GazetteAdminIssueDto toAdmin(GazetteIssue issue) {
        GazetteAdminIssueDto dto = new GazetteAdminIssueDto();
        dto.setIssue(read(issue));
        dto.setStatus(issue.getStatus().name());
        dto.setEditorNotes(readNotes(issue.getEditorNotes()));
        dto.setMainHeadline(issue.getMainHeadline());
        dto.setPublishedAt(iso(issue.getPublishedAt()));
        dto.setUpdatedAt(iso(issue.getUpdatedAt()));
        return dto;
    }

    private GazetteIssueSummaryDto summary(GazetteIssue issue) {
        return new GazetteIssueSummaryDto(issue.getTurn(), issue.getIssueNumber(),
                issue.getStatus().name(), issue.getMainHeadline(),
                iso(issue.getPublishedAt()), iso(issue.getUpdatedAt()), issue.isFinalEdition());
    }

    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private GazetteIssueDto read(GazetteIssue issue) {
        try {
            return objectMapper.readValue(issue.getPayload(), GazetteIssueDto.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Édition " + issue.getId() + " illisible", e);
        }
    }

    private GazetteEditorNotesDto readNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(notes, GazetteEditorNotesDto.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Notes d'édition illisibles", e);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Sérialisation de la gazette impossible", e);
        }
    }
}
