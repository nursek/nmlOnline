package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.AdminStartingSectorsRequestDto;
import com.mg.nmlonline.api.dto.AdminTemplatePlayerRequestDto;
import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.GameSetupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSetupController {

    private final GameSetupService gameSetupService;

    public AdminSetupController(GameSetupService gameSetupService) {
        this.gameSetupService = gameSetupService;
    }

    @PostMapping("/players/template")
    public PlayerDto createTemplatePlayer(@Valid @RequestBody AdminTemplatePlayerRequestDto request,
                                          HttpServletRequest httpRequest) {
        return gameSetupService.createTemplatePlayer(boardId(httpRequest), request);
    }

    @PutMapping("/players/{id}/starting-sectors")
    public ResponseEntity<Void> assignStartingSectors(
            @PathVariable Long id,
            @Valid @RequestBody AdminStartingSectorsRequestDto request,
            HttpServletRequest httpRequest) {
        gameSetupService.assignStartingSectors(boardId(httpRequest), id, request.sectorNumbers());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/board/start")
    public ResponseEntity<Void> startGame(HttpServletRequest httpRequest) {
        gameSetupService.startGame(boardId(httpRequest));
        return ResponseEntity.noContent().build();
    }

    private Long boardId(HttpServletRequest request) {
        return (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
