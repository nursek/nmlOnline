package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.PlayerActionDto;
import com.mg.nmlonline.domain.service.PlayerActionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players/actions")
public class PlayerActionController {

    private final PlayerActionService playerActionService;

    public PlayerActionController(PlayerActionService playerActionService) {
        this.playerActionService = playerActionService;
    }

    @GetMapping
    public ResponseEntity<List<PlayerActionDto>> getActions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(playerActionService.getCurrentTurnActions(userId));
    }

    @PostMapping("/{actionId}/undo")
    public ResponseEntity<List<PlayerActionDto>> undoFrom(@PathVariable Long actionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(playerActionService.undoFrom(userId, actionId));
    }

    @PostMapping("/undo-all")
    public ResponseEntity<List<PlayerActionDto>> undoAll(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(playerActionService.undoAll(userId));
    }
}
