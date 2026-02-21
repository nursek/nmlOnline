package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BuildingDto;
import com.mg.nmlonline.domain.model.building.*;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.service.BuildingService;
import com.mg.nmlonline.mapper.BuildingMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des bâtiments.
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

    // === ENDPOINTS GÉNÉRAUX ===

    /**
     * Récupère le QG d'un joueur.
     */
    @GetMapping("/headquarters/{playerId}")
    public ResponseEntity<BuildingDto> getHeadquarters(@PathVariable Long playerId) {
        return buildingService.getHeadquarters(playerId)
                .map(buildingMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère la banque d'un joueur.
     */
    @GetMapping("/bank/{playerId}")
    public ResponseEntity<BuildingDto> getBank(@PathVariable Long playerId) {
        return buildingService.getBank(playerId)
                .map(buildingMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Récupère les caches d'armes d'un joueur.
     */
    @GetMapping("/weapon-caches/{playerId}")
    public ResponseEntity<List<BuildingDto>> getWeaponCaches(@PathVariable Long playerId) {
        List<BuildingDto> caches = buildingService.getWeaponCaches(playerId).stream()
                .map(buildingMapper::toDto)
                .toList();
        return ResponseEntity.ok(caches);
    }

    // === ENDPOINTS QG ===

    /**
     * Vérifie si un joueur a un QG opérationnel.
     */
    @GetMapping("/headquarters/{playerId}/operational")
    public ResponseEntity<Boolean> isHeadquartersOperational(@PathVariable Long playerId) {
        return ResponseEntity.ok(buildingService.hasOperationalHeadquarters(playerId));
    }

    /**
     * Reconstruit le QG sur place.
     */
    @PostMapping("/headquarters/{playerId}/reconstruct-same")
    public ResponseEntity<Void> reconstructHeadquartersSame(@PathVariable Long playerId) {
        if (buildingService.reconstructHeadquartersSameLocation(playerId)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    // === ENDPOINTS DÉPLACEMENT ===

    /**
     * Déplace un bâtiment vers un nouveau secteur.
     */
    @PostMapping("/{buildingId}/move")
    public ResponseEntity<Void> moveBuilding(
            @PathVariable Long buildingId,
            @RequestBody MoveBuildingRequest request) {
        if (buildingService.moveBuilding(buildingId, request.newSectorNumber(), request.currentTurn())) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    // === ENDPOINTS CAPTURE ===

    /**
     * Capture un QG (entraîne la défaite).
     */
    @PostMapping("/headquarters/capture")
    public ResponseEntity<Void> captureHeadquarters(@RequestBody CaptureRequest request) {
        buildingService.captureHeadquarters(
                request.victimPlayerId(),
                request.capturingPlayerId(),
                request.currentTurn()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Capture une cache d'armes.
     */
    @PostMapping("/weapon-cache/{cacheId}/capture")
    public ResponseEntity<CaptureWeaponCacheResponse> captureWeaponCache(
            @PathVariable Long cacheId,
            @RequestBody CaptureRequest request) {
        List<EquipmentStack> transferred = buildingService.captureWeaponCache(
                cacheId,
                request.capturingPlayerId(),
                request.currentTurn()
        );
        return ResponseEntity.ok(new CaptureWeaponCacheResponse(transferred.size()));
    }

    /**
     * Capture une banque.
     */
    @PostMapping("/bank/{bankId}/capture")
    public ResponseEntity<CaptureBankResponse> captureBank(
            @PathVariable Long bankId,
            @RequestBody CaptureRequest request) {
        BuildingService.CaptureResult result = buildingService.captureBank(
                bankId,
                request.capturingPlayerId(),
                request.currentTurn()
        );
        return ResponseEntity.ok(new CaptureBankResponse(
                result.money(),
                result.resources().size()
        ));
    }

    // === RECORDS POUR LES REQUÊTES/RÉPONSES ===

    public record MoveBuildingRequest(int newSectorNumber, int currentTurn) {}

    public record CaptureRequest(Long victimPlayerId, Long capturingPlayerId, int currentTurn) {}

    public record CaptureWeaponCacheResponse(int equipmentsTransferred) {}

    public record CaptureBankResponse(double moneyTransferred, int resourcesTransferred) {}
}

