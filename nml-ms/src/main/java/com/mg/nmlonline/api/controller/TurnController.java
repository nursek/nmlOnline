package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.TurnService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/turn")
public class TurnController {

    private final TurnService turnService;

    public TurnController(TurnService turnService) {
        this.turnService = turnService;
    }

    @GetMapping("/current")
    public ResponseEntity<Integer> getCurrentTurn(HttpServletRequest request) {
        return ResponseEntity.ok(turnService.getCurrentTurn(
                (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE)));
    }
}
