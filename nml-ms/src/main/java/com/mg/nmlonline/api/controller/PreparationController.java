package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.DistributeUnitsRequestDto;
import com.mg.nmlonline.api.dto.PreparationPlacementRequestDto;
import com.mg.nmlonline.config.BoardScopeFilter;
import com.mg.nmlonline.domain.service.PreparationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/preparation")
public class PreparationController {

    private final PreparationService preparationService;

    public PreparationController(PreparationService preparationService) {
        this.preparationService = preparationService;
    }

    @PostMapping("/character")
    public ResponseEntity<Void> placeCharacter(
            @Valid @RequestBody PreparationPlacementRequestDto request,
            HttpServletRequest httpRequest) {
        preparationService.placeCharacter(userId(httpRequest), boardId(httpRequest),
                request.sectorNumber());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/buildings/{buildingId}")
    public ResponseEntity<Void> placeBuilding(
            @PathVariable Long buildingId,
            @Valid @RequestBody PreparationPlacementRequestDto request,
            HttpServletRequest httpRequest) {
        preparationService.placeBuilding(userId(httpRequest), boardId(httpRequest),
                buildingId, request.sectorNumber());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/units")
    public ResponseEntity<Void> distributeUnits(
            @Valid @RequestBody DistributeUnitsRequestDto request,
            HttpServletRequest httpRequest) {
        preparationService.distributeUnits(userId(httpRequest), boardId(httpRequest),
                request.sectorNumber(), request.quantity(), request.classes());
        return ResponseEntity.noContent().build();
    }

    private Long userId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new SecurityException("Non authentifié");
        }
        return userId;
    }

    private Long boardId(HttpServletRequest request) {
        return (Long) request.getAttribute(BoardScopeFilter.BOARD_ID_ATTRIBUTE);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handlePreparation(IllegalStateException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    }
}
