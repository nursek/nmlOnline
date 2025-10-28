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
    public ResponseEntity<BoardDto> getBoardById(@PathVariable Long id) {
        return boardService.getBoardById(id)
                .map(boardMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère une board par son nom
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<BoardDto> getBoardByName(@PathVariable String name) {
        return boardService.getBoardByName(name)
                .map(boardMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Crée ou met à jour une board
     */
    @PostMapping
    public ResponseEntity<BoardDto> createBoard(@RequestBody BoardDto boardDto, @RequestParam String name) {
        Board board = boardMapper.toDomain(boardDto);
        Board savedBoard = boardService.saveBoard(board, name);
        return ResponseEntity.status(HttpStatus.CREATED).body(boardMapper.toDto(savedBoard));
    }

    /**
     * Supprime une board
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assigne un propriétaire à un secteur
     */
    @PutMapping("/{boardId}/sectors/{sectorNumber}/owner")
    public ResponseEntity<Void> assignOwnerToSector(
            @PathVariable Long boardId,
            @PathVariable int sectorNumber,
            @RequestParam Integer playerId,
            @RequestParam String color) {
        boolean success = boardService.assignOwnerToSector(boardId, sectorNumber, playerId, color);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Vérifie si deux secteurs sont voisins
     */
    @GetMapping("/{boardId}/sectors/neighbors")
    public ResponseEntity<Boolean> areNeighbors(
            @PathVariable Long boardId,
            @RequestParam int sector1,
            @RequestParam int sector2) {
        boolean areNeighbors = boardService.areNeighbors(boardId, sector1, sector2);
        return ResponseEntity.ok(areNeighbors);
    }

    /**
     * Vérifie s'il y a un conflit entre deux secteurs
     */
    @GetMapping("/{boardId}/sectors/conflict")
    public ResponseEntity<Boolean> hasConflict(
            @PathVariable Long boardId,
            @RequestParam int sector1,
            @RequestParam int sector2) {
        boolean hasConflict = boardService.hasConflict(boardId, sector1, sector2);
        return ResponseEntity.ok(hasConflict);
    }
}

