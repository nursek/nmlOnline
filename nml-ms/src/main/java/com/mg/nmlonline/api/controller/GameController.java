package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.GamePodiumDto;
import com.mg.nmlonline.api.dto.GazetteIssueDto;
import com.mg.nmlonline.api.dto.GazetteIssueSummaryDto;
import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.GamePodiumService;
import com.mg.nmlonline.domain.service.GazetteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GamePodiumService gamePodiumService;
    private final GazetteService gazetteService;

    public GameController(GamePodiumService gamePodiumService, GazetteService gazetteService) {
        this.gamePodiumService = gamePodiumService;
        this.gazetteService = gazetteService;
    }

    @GetMapping("/podium")
    public GamePodiumDto podium(HttpServletRequest request) {
        return gamePodiumService.getPodium(boardId(request));
    }

    @GetMapping("/gazette")
    public List<GazetteIssueSummaryDto> gazette(HttpServletRequest request) {
        return gazetteService.listPublished(boardId(request));
    }

    @GetMapping("/gazette/{turn}")
    public GazetteIssueDto gazetteIssue(@PathVariable int turn, HttpServletRequest request) {
        return gazetteService.getPublished(boardId(request), turn);
    }

    private static Long boardId(HttpServletRequest request) {
        return (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE);
    }
}
