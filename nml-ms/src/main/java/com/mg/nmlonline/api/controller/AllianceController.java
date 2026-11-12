package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.AllianceMeDto;
import com.mg.nmlonline.api.dto.AllianceProposalDto;
import com.mg.nmlonline.api.dto.AnnouncementDto;
import com.mg.nmlonline.domain.model.alliance.ProposalKind;
import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.AllianceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AllianceController {

    private final AllianceService allianceService;

    public AllianceController(AllianceService allianceService) {
        this.allianceService = allianceService;
    }

    @GetMapping("/alliances/me")
    public ResponseEntity<AllianceMeDto> getMe(HttpServletRequest request) {
        return ResponseEntity.ok(allianceService.getMe(userId(request), boardId(request)));
    }

    @PostMapping("/alliances/proposals")
    public ResponseEntity<AllianceProposalDto> propose(@RequestBody ProposalRequest body,
                                                       HttpServletRequest request) {
        if (body.kind() == null || body.kind().isBlank()) {
            throw new IllegalArgumentException("Type de proposition manquant.");
        }
        ProposalKind kind = ProposalKind.valueOf(body.kind());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(allianceService.propose(userId(request), boardId(request), kind, body.targetPlayerId()));
    }

    @PostMapping("/alliances/proposals/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable("id") Long id, HttpServletRequest request) {
        allianceService.accept(userId(request), boardId(request), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/alliances/proposals/{id}/decline")
    public ResponseEntity<Void> decline(@PathVariable("id") Long id, HttpServletRequest request) {
        allianceService.decline(userId(request), boardId(request), id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/alliances/proposals/{id}")
    public ResponseEntity<Void> withdraw(@PathVariable("id") Long id, HttpServletRequest request) {
        allianceService.withdraw(userId(request), boardId(request), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/alliances/{id}/betray")
    public ResponseEntity<Void> betray(@PathVariable("id") Long id, HttpServletRequest request) {
        allianceService.betray(userId(request), boardId(request), id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/announcements")
    public ResponseEntity<List<AnnouncementDto>> getAnnouncements(HttpServletRequest request) {
        return ResponseEntity.ok(allianceService.getAnnouncements(boardId(request)));
    }

    private Long boardId(HttpServletRequest request) {
        return (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE);
    }

    private Long userId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        return userId;
    }

    public record ProposalRequest(String kind, Long targetPlayerId) {
    }
}
