package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BattleReportDto;
import com.mg.nmlonline.domain.service.BattleReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/battle-reports")
public class BattleReportController {

    private final BattleReportService battleReportService;

    public BattleReportController(BattleReportService battleReportService) {
        this.battleReportService = battleReportService;
    }

    @GetMapping
    public ResponseEntity<List<BattleReportDto>> getReports(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(battleReportService.getReportsForUser(userId));
    }
}
