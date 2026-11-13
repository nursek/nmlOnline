package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.GameWinnerRequestDto;
import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.GameEndService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/game")
public class AdminGameController {

    private final GameEndService gameEndService;

    public AdminGameController(GameEndService gameEndService) {
        this.gameEndService = gameEndService;
    }

    @PutMapping("/winner")
    public GameEndService.GameOutcome adjustWinner(@RequestBody GameWinnerRequestDto request,
                                                   HttpServletRequest httpRequest) {
        if (request.playerId() == null) {
            throw new IllegalArgumentException("playerId requis");
        }
        return gameEndService.adjustWinner(
                (Long) httpRequest.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE),
                request.playerId());
    }
}
