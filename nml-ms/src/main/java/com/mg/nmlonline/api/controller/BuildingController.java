package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BuildingDto;
import com.mg.nmlonline.domain.model.building.*;
import com.mg.nmlonline.domain.service.BuildingService;
import com.mg.nmlonline.mapper.BuildingMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Contrôleur REST pour la gestion des bâtiments.
 * Tous les endpoints vérifient l'ownership via request.getAttribute("userId").
 */
@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildingService;
    private final BuildingMapper buildingMapper;

    public BuildingController(BuildingService buildingService, BuildingMapper buildingMapper) {
        this.buildingService = buildingService;
        this.buildingMapper = buildingMapper;
    }

    /**
     * Extrait et vérifie l'ID utilisateur authentifié depuis la requête.
     */
    private Long getAuthenticatedUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new SecurityException("Utilisateur non authentifié");
        }
        return userId;
    }

    /**
     * Vérifie que le joueur demandé correspond à l'utilisateur authentifié.
     */
    private void verifyOwnership(HttpServletRequest request, Long playerId) {
        Long userId = getAuthenticatedUserId(request);
        if (!userId.equals(playerId)) {
            throw new SecurityException("Accès refusé : vous ne pouvez accéder qu'aux bâtiments de votre propre joueur");
        }
    }

    // === ENDPOINTS GÉNÉRAUX ===

    /**
     * Récupère le QG du joueur authentifié.
     */
    @GetMapping("/headquarters/{playerId}")
    public ResponseEntity<BuildingDto> getHeadquarters(@PathVariable Long playerId, HttpServletRequest request) {
        verifyOwnership(request, playerId);
        return buildingService.getHeadquarters(playerId)
                .map(buildingMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère la banque du joueur authentifié.
     */
    @GetMapping("/bank/{playerId}")
    public ResponseEntity<BuildingDto> getBank(@PathVariable Long playerId, HttpServletRequest request) {
        verifyOwnership(request, playerId);
        return buildingService.getBank(playerId)
                .map(buildingMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère les caches d'armes du joueur authentifié.
     */
    @GetMapping("/weapon-caches/{playerId}")
    public ResponseEntity<List<BuildingDto>> getWeaponCaches(@PathVariable Long playerId, HttpServletRequest request) {
        verifyOwnership(request, playerId);
        List<BuildingDto> caches = buildingService.getWeaponCaches(playerId).stream()
                .map(buildingMapper::toDto)
                .toList();
        return ResponseEntity.ok(caches);
    }

    // === ENDPOINTS QG ===

    /**
     * Vérifie si le joueur authentifié a un QG opérationnel.
     */
    @GetMapping("/headquarters/{playerId}/operational")
    public ResponseEntity<Boolean> isHeadquartersOperational(@PathVariable Long playerId, HttpServletRequest request) {
        verifyOwnership(request, playerId);
        return ResponseEntity.ok(buildingService.hasOperationalHeadquarters(playerId));
    }

    /**
     * Reconstruit le QG sur place (joueur authentifié uniquement).
     */
    @PostMapping("/headquarters/{playerId}/reconstruct-same")
    public ResponseEntity<Void> reconstructHeadquartersSame(@PathVariable Long playerId, HttpServletRequest request) {
        verifyOwnership(request, playerId);
        if (buildingService.reconstructHeadquartersSameLocation(playerId)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    // === ENDPOINTS DÉPLACEMENT ===

    /**
     * Déplace un bâtiment vers un nouveau secteur (ownership vérifiée via le bâtiment).
     */
    @PostMapping("/{buildingId}/move")
    public ResponseEntity<Void> moveBuilding(
            @PathVariable Long buildingId,
            @RequestBody MoveBuildingRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getAuthenticatedUserId(httpRequest);

        // Vérifier que le bâtiment appartient au joueur authentifié
        Optional<Building> buildingOpt = buildingService.findById(buildingId);
        if (buildingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Building building = buildingOpt.get();
        if (!userId.equals(building.getPlayerId())) {
            throw new SecurityException("Accès refusé : ce bâtiment ne vous appartient pas");
        }

        int currentTurnFromServer = buildingService.getCurrentTurn(building.getPlayerId());

        if (buildingService.moveBuilding(buildingId, request.newSectorNumber(), currentTurnFromServer)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    // === RECORDS POUR LES REQUÊTES/RÉPONSES ===

    public record MoveBuildingRequest(int newSectorNumber) {}
}
