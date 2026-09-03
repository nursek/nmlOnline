package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.domain.service.TurnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tour courant du plateau actif — information globale, aucune donnée joueur. */
@RestController
@RequestMapping("/api/turn")
public class TurnController {

    private final TurnService turnService;

    public TurnController(TurnService turnService) {
        this.turnService = turnService;
    }

    @GetMapping("/current")
    public ResponseEntity<Integer> getCurrentTurn() {
        return ResponseEntity.ok(turnService.getCurrentTurn());
    }
}
