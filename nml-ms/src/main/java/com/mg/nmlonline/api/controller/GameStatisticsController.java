package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.GameStatisticsDto;
import com.mg.nmlonline.domain.service.GameStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class GameStatisticsController {

    private final GameStatisticsService gameStatisticsService;

    public GameStatisticsController(GameStatisticsService gameStatisticsService) {
        this.gameStatisticsService = gameStatisticsService;
    }

    @GetMapping
    public GameStatisticsDto getStatistics() {
        return gameStatisticsService.getStatistics();
    }
}
