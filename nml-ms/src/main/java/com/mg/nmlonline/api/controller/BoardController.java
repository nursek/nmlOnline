package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.api.dto.SectorNoteRequestDto;
import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.AdminService;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.SectorNoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;
    private final SectorNoteService sectorNoteService;
    private final AdminService adminService;

    private boolean isCurrentBoard(Long boardId, HttpServletRequest request) {
        return boardId != null
                && boardId.equals(request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE));
    }

    public BoardController(BoardService boardService, SectorNoteService sectorNoteService,
                           AdminService adminService) {
        this.boardService = boardService;
        this.sectorNoteService = sectorNoteService;
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<BoardDto>> getAllBoards(HttpServletRequest request) {
        return ResponseEntity.ok(boardService.getAllBoardsDto(
                (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE),
                (Long) request.getAttribute("userId"), request.isUserInRole("ADMIN")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardDto> getBoardById(@PathVariable("id") Long id, HttpServletRequest request) {
        if (!isCurrentBoard(id, request)) {
            return ResponseEntity.notFound().build();
        }
        return boardService.getBoardByIdDto(id, (Long) request.getAttribute("userId"),
                        request.isUserInRole("ADMIN"))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{boardId}/sector-notes")
    public ResponseEntity<Map<Integer, String>> getSectorNotes(
            @PathVariable("boardId") Long boardId, HttpServletRequest request) {
        if (!isCurrentBoard(boardId, request)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(sectorNoteService.getNotes((Long) request.getAttribute("userId"), boardId));
    }

    @PutMapping("/{boardId}/sectors/{sectorNumber}/note")
    public ResponseEntity<Void> saveSectorNote(
            @PathVariable("boardId") Long boardId,
            @PathVariable("sectorNumber") int sectorNumber,
            @Valid @RequestBody SectorNoteRequestDto noteRequest,
            HttpServletRequest request) {
        if (!isCurrentBoard(boardId, request)) {
            return ResponseEntity.notFound().build();
        }
        sectorNoteService.saveNote((Long) request.getAttribute("userId"), boardId, sectorNumber,
                noteRequest.getNote());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BoardDto> createBoard(@RequestBody BoardDto boardDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createBoardDto(boardDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable("id") Long id) {
        adminService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INSTANCE_ADMIN')")
    @PutMapping("/{boardId}/sectors/{sectorNumber}/owner")
    public ResponseEntity<Void> assignOwnerToSector(
            @PathVariable("boardId") Long boardId,
            @PathVariable("sectorNumber") int sectorNumber,
            @RequestParam("playerId") Long playerId,
            @RequestParam("color") String color,
            HttpServletRequest request) {
        if (!isCurrentBoard(boardId, request)) {
            return ResponseEntity.notFound().build();
        }
        boolean success = boardService.assignOwnerToSector(boardId, sectorNumber, playerId, color);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{boardId}/sectors/neighbors")
    public ResponseEntity<Boolean> areNeighbors(
            @PathVariable("boardId") Long boardId,
            @RequestParam("sector1") int sector1,
            @RequestParam("sector2") int sector2,
            HttpServletRequest request) {
        if (!isCurrentBoard(boardId, request)) {
            return ResponseEntity.notFound().build();
        }
        boolean areNeighbors = boardService.areNeighbors(boardId, sector1, sector2);
        return ResponseEntity.ok(areNeighbors);
    }

    @GetMapping("/{boardId}/sectors/conflict")
    public ResponseEntity<Boolean> hasConflict(
            @PathVariable("boardId") Long boardId,
            @RequestParam("sector1") int sector1,
            @RequestParam("sector2") int sector2,
            HttpServletRequest request) {
        if (!isCurrentBoard(boardId, request)) {
            return ResponseEntity.notFound().build();
        }
        boolean hasConflict = boardService.hasConflict(boardId, sector1, sector2);
        return ResponseEntity.ok(hasConflict);
    }
}

