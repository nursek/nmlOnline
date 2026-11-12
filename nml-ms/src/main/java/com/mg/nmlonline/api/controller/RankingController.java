package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.RankingsDto;
import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.RankingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankings")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping
    public RankingsDto getRankings(HttpServletRequest request) {
        return rankingService.getRankings(
                (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE));
    }
}
