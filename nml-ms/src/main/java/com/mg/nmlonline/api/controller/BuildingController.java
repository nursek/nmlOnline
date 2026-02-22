package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BuildingDto;
import com.mg.nmlonline.domain.model.building.*;
import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.service.BuildingService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.mapper.BuildingMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    //TODO : verify authentication and authorization to prevent data leak (ex: only allow access to own character or admin access)
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
    //TODO : verify authentication and authorization to prevent data leak (ex: only allow access to own character or admin access)
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
    //TODO : verify authentication and authorization to prevent data leak (ex: only allow access to own character or admin access)
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
    //TODO : verify authentication and authorization to prevent data leak (ex: only allow access to own character or admin access)
    @GetMapping("/headquarters/{playerId}/operational")
    public ResponseEntity<Boolean> isHeadquartersOperational(@PathVariable Long playerId) {
        return ResponseEntity.ok(buildingService.hasOperationalHeadquarters(playerId));
    }

    /**
     * Reconstruit le QG sur place.
     */
    //TODO : verify authentication and authorization to prevent data leak (ex: only allow access to own character or admin access)
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
    //TODO : verify authentication and authorization to prevent data leak (ex: only allow access to own character or admin access)
    @PostMapping("/{buildingId}/move")
    public ResponseEntity<Void> moveBuilding(
            @PathVariable Long buildingId,
            @RequestBody MoveBuildingRequest request) {
        // TODO: remplacer cet appel par une récupération du tour réel via un service de gestion de tours.
        int currentTurnFromServer = /* turnService.getCurrentTurnForBuilding(buildingId) */ 0;

        if (buildingService.moveBuilding(buildingId, request.newSectorNumber(), currentTurnFromServer)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    // === RECORDS POUR LES REQUÊTES/RÉPONSES ===

    public record MoveBuildingRequest(int newSectorNumber) {}
}
