package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.GameWinnerRequestDto;
import com.mg.nmlonline.api.dto.GazetteAdminIssueDto;
import com.mg.nmlonline.api.dto.GazetteContentRequestDto;
import com.mg.nmlonline.api.dto.GazetteIssueSummaryDto;
import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.GameEndService;
import com.mg.nmlonline.domain.service.GazetteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/game")
public class AdminGameController {

    private final GameEndService gameEndService;
    private final GazetteService gazetteService;

    public AdminGameController(GameEndService gameEndService, GazetteService gazetteService) {
        this.gameEndService = gameEndService;
        this.gazetteService = gazetteService;
    }

    @PutMapping("/winner")
    public GameEndService.GameOutcome adjustWinner(@RequestBody GameWinnerRequestDto request,
                                                   HttpServletRequest httpRequest) {
        if (request.playerId() == null) {
            throw new IllegalArgumentException("playerId requis");
        }
        return gameEndService.adjustWinner(boardId(httpRequest), request.playerId());
    }

    @GetMapping("/gazette")
    public List<GazetteIssueSummaryDto> listGazette(HttpServletRequest request) {
        return gazetteService.listAdmin(boardId(request));
    }

    @GetMapping("/gazette/{turn}")
    public GazetteAdminIssueDto getGazette(@PathVariable int turn, HttpServletRequest request) {
        return gazetteService.getAdmin(boardId(request), turn);
    }

    @PutMapping("/gazette/{turn}")
    public GazetteAdminIssueDto saveGazette(@PathVariable int turn,
                                            @RequestBody GazetteContentRequestDto body,
                                            HttpServletRequest request) {
        return gazetteService.update(boardId(request), turn, body);
    }

    @PostMapping("/gazette/{turn}/publish")
    public GazetteAdminIssueDto publishGazette(@PathVariable int turn,
                                               @RequestBody GazetteContentRequestDto body,
                                               HttpServletRequest request) {
        return gazetteService.publish(boardId(request), turn, body, userId(request));
    }

    @DeleteMapping("/gazette/{turn}")
    public ResponseEntity<Void> deleteGazette(@PathVariable int turn, HttpServletRequest request) {
        try {
            gazetteService.deleteDraft(boardId(request), turn);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    private static Long boardId(HttpServletRequest request) {
        return (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE);
    }

    private static Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
