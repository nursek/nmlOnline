package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.mapper.BoardMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
@CrossOrigin(origins = "*")
public class BoardController {

    private final BoardService boardService;
    private final BoardMapper boardMapper;

    public BoardController(BoardService boardService, BoardMapper boardMapper) {
        this.boardService = boardService;
        this.boardMapper = boardMapper;
    }

    /**
     * Récupère toutes les boards
     */
    @GetMapping
    public ResponseEntity<List<BoardDto>> getAllBoards() {
        List<BoardDto> boards = boardService.getAllBoards().stream()
                .map(boardMapper::toDto)
                .toList();
        return ResponseEntity.ok(boards);
    }

    /**
     * Récupère une board par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardDto> getBoardById(@PathVariable("id") Long id) {
        return boardService.getBoardById(id)
                .map(boardMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère une board par son nom
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<BoardDto> getBoardByName(@PathVariable("name") String name) {
        return boardService.getBoardByName(name)
                .map(boardMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crée ou met à jour une board
     */
    @PostMapping
    public ResponseEntity<BoardDto> createBoard(@RequestBody BoardDto boardDto, @RequestParam("name") String name) {
        Board board = boardMapper.toDomain(boardDto);
        Board savedBoard = boardService.saveBoard(board, name);
        return ResponseEntity.status(HttpStatus.CREATED).body(boardMapper.toDto(savedBoard));
    }

    /**
     * Supprime une board
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable("id") Long id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assigne un propriétaire à un secteur
     */
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

