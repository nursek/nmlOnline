package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BuildingDto;
import com.mg.nmlonline.domain.model.building.Bank;
import com.mg.nmlonline.domain.model.building.Building;
import com.mg.nmlonline.domain.model.building.Headquarters;
import com.mg.nmlonline.domain.model.building.WeaponCache;
import com.mg.nmlonline.domain.service.AuthorizationService;
import com.mg.nmlonline.domain.service.BuildingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildingService;
    private final AuthorizationService authorizationService;

    public BuildingController(BuildingService buildingService,
                              AuthorizationService authorizationService) {
        this.buildingService = buildingService;
        this.authorizationService = authorizationService;
    }

    private Long getAuthenticatedUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @GetMapping("/headquarters/{playerId}")
    public ResponseEntity<BuildingDto> getHeadquarters(@PathVariable Long playerId,
                                                       HttpServletRequest request) {
        Long userId = getAuthenticatedUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        if (!authorizationService.isPlayerOwner(userId, playerId)) return ResponseEntity.status(403).build();

        return buildingService.getHeadquartersDto(playerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/bank/{playerId}")
    public ResponseEntity<BuildingDto> getBank(@PathVariable Long playerId,
                                               HttpServletRequest request) {
        Long userId = getAuthenticatedUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        if (!authorizationService.isPlayerOwner(userId, playerId)) return ResponseEntity.status(403).build();

        return buildingService.getBankDto(playerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/weapon-caches/{playerId}")
    public ResponseEntity<List<BuildingDto>> getWeaponCaches(@PathVariable Long playerId,
                                                             HttpServletRequest request) {
        Long userId = getAuthenticatedUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        if (!authorizationService.isPlayerOwner(userId, playerId)) return ResponseEntity.status(403).build();

        List<BuildingDto> caches = buildingService.getWeaponCachesDto(playerId);
        return ResponseEntity.ok(caches);
    }

    @GetMapping("/headquarters/{playerId}/operational")
    public ResponseEntity<Boolean> isHeadquartersOperational(@PathVariable Long playerId,
                                                             HttpServletRequest request) {
        Long userId = getAuthenticatedUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        if (!authorizationService.isPlayerOwner(userId, playerId)) return ResponseEntity.status(403).build();

        return ResponseEntity.ok(buildingService.hasOperationalHeadquarters(playerId));
    }

    @PostMapping("/headquarters/{playerId}/reconstruct-same")
    public ResponseEntity<Void> reconstructHeadquartersSame(@PathVariable Long playerId,
                                                            HttpServletRequest request) {
        Long userId = getAuthenticatedUserId(request);
        if (userId == null) return ResponseEntity.status(401).build();
        if (!authorizationService.isPlayerOwner(userId, playerId)) return ResponseEntity.status(403).build();

        if (buildingService.reconstructHeadquartersSameLocation(playerId)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{buildingId}/move")
    public ResponseEntity<Void> moveBuilding(
            @PathVariable Long buildingId,
            @RequestBody MoveBuildingRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getAuthenticatedUserId(httpRequest);
        if (userId == null) return ResponseEntity.status(401).build();
        if (!authorizationService.isBuildingOwner(userId, buildingId)) return ResponseEntity.status(403).build();

        buildingService.moveBuilding(buildingId, request.boardId(), request.newSectorNumber(), request.currentTurn());
        return ResponseEntity.ok().build();
    }

    public record MoveBuildingRequest(Long boardId, int newSectorNumber, int currentTurn) {}
}
