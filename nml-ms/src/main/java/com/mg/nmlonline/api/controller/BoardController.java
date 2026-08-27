package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.domain.service.BoardService;
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

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /**
     * Récupère toutes les boards
     */
    @GetMapping
    public ResponseEntity<List<BoardDto>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoardsDto());
    }

    /**
     * Récupère une board par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardDto> getBoardById(@PathVariable("id") Long id) {
        return boardService.getBoardByIdDto(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère une board par son nom
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<BoardDto> getBoardByName(@PathVariable("name") String name) {
        return boardService.getBoardByNameDto(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crée ou met à jour une board
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BoardDto> createBoard(@RequestBody BoardDto boardDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createBoardDto(boardDto));
    }

    /**
     * Supprime une board
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable("id") Long id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assigne un propriétaire à un secteur
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{boardId}/sectors/{sectorNumber}/owner")
    public ResponseEntity<Void> assignOwnerToSector(
            @PathVariable("boardId") Long boardId,
            @PathVariable("sectorNumber") int sectorNumber,
            @RequestParam("playerId") Long playerId,
            @RequestParam("color") String color) {
        boolean success = boardService.assignOwnerToSector(boardId, sectorNumber, playerId, color);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Vérifie si deux secteurs sont voisins
     */
    @GetMapping("/{boardId}/sectors/neighbors")
    public ResponseEntity<Boolean> areNeighbors(
            @PathVariable("boardId") Long boardId,
            @RequestParam("sector1") int sector1,
            @RequestParam("sector2") int sector2) {
        boolean areNeighbors = boardService.areNeighbors(boardId, sector1, sector2);
        return ResponseEntity.ok(areNeighbors);
    }

    /**
     * Vérifie s'il y a un conflit entre deux secteurs
     */
    @GetMapping("/{boardId}/sectors/conflict")
    public ResponseEntity<Boolean> hasConflict(
            @PathVariable("boardId") Long boardId,
            @RequestParam("sector1") int sector1,
            @RequestParam("sector2") int sector2) {
        boolean hasConflict = boardService.hasConflict(boardId, sector1, sector2);
        return ResponseEntity.ok(hasConflict);
    }
}

