package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.GamePodiumDto;
import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.GamePodiumService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GamePodiumService gamePodiumService;

    public GameController(GamePodiumService gamePodiumService) {
        this.gamePodiumService = gamePodiumService;
    }

    @GetMapping("/podium")
    public GamePodiumDto podium(HttpServletRequest request) {
        return gamePodiumService.getPodium(
                (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE));
    }
}
