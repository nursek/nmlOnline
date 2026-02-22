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
    private final PlayerService playerService;

    public BuildingController(BuildingService buildingService, BuildingMapper buildingMapper, PlayerService playerService) {
        this.buildingService = buildingService;
        this.buildingMapper = buildingMapper;
        this.playerService = playerService;
    }

    /**
     * Récupère le joueur authentifié à partir du SecurityContext.
     * @return le Player correspondant à l'utilisateur connecté, ou null si non trouvé
     */
    private Player getAuthenticatedPlayer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        String username = auth.getPrincipal().toString();
        return playerService.findByName(username);
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
        // TODO: remplacer cet appel par une récupération du tour réel via un service de gestion de tours.
        int currentTurnFromServer = /* turnService.getCurrentTurnForBuilding(buildingId) */ 0;

        if (buildingService.moveBuilding(buildingId, request.newSectorNumber(), currentTurnFromServer)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    // === ENDPOINTS CAPTURE ===



    /**
     * Valide une requête de capture : authentification, existence de la victime,
     * propriété du bâtiment et interdiction d'auto-capture.
     *
     * @param request    la requête de capture contenant l'ID de la victime
     * @param buildingId l'ID du bâtiment à capturer (null pour le QG, résolu via victimPlayerId)
     * @return le joueur capturant si toutes les vérifications passent, ou une ResponseEntity d'erreur
     */

    private CaptureValidation validateCaptureRequest(CaptureRequest request, Long buildingId) {
        Player capturingPlayer = getAuthenticatedPlayer();
        if (capturingPlayer == null) {
            return CaptureValidation.failure(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        // Vérifier que le joueur victime existe
        if (playerService.findById(request.victimPlayerId()).isEmpty()) {
            return CaptureValidation.failure(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        }

        // Vérifier que le joueur ne tente pas de capturer son propre bâtiment
        if (capturingPlayer.getId().equals(request.victimPlayerId())) {
            return CaptureValidation.failure(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }

        // Si un buildingId est fourni, vérifier que le bâtiment existe et appartient à la victime
        if (buildingId != null) {
            Building building = buildingService.findById(buildingId).orElse(null);
            if (building == null) {
                return CaptureValidation.failure(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
            }
            if (!building.getPlayerId().equals(request.victimPlayerId())) {
                return CaptureValidation.failure(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
            }
        }

        return CaptureValidation.success(capturingPlayer);
    }

    /**
     * Capture un QG (entraîne la défaite).
     * Le joueur capturant est déterminé par l'utilisateur authentifié.
     */
    @PostMapping("/headquarters/capture")
    public ResponseEntity<Void> captureHeadquarters(@RequestBody CaptureRequest request) {
        CaptureValidation validation = validateCaptureRequest(request, null);
        if (validation.hasError()) {
            return ResponseEntity.status(validation.error().getStatusCode()).build();
        }

        // Vérifier que le QG appartient bien au joueur victime
        if (buildingService.getHeadquarters(request.victimPlayerId()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Player capturingPlayer = validation.capturingPlayer();
        int currentTurn = buildingService.getCurrentTurn(capturingPlayer.getId());
        buildingService.captureHeadquarters(
                request.victimPlayerId(),
                capturingPlayer.getId(),
                currentTurn
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Capture une cache d'armes.
     * Le joueur capturant est déterminé par l'utilisateur authentifié.
     */
    @PostMapping("/weapon-cache/{cacheId}/capture")
    public ResponseEntity<CaptureWeaponCacheResponse> captureWeaponCache(
            @PathVariable Long cacheId,
            @RequestBody CaptureRequest request) {
        CaptureValidation validation = validateCaptureRequest(request, cacheId);
        if (validation.hasError()) {
            return ResponseEntity.status(validation.error().getStatusCode()).build();
        }

        Player capturingPlayer = validation.capturingPlayer();
        int currentTurn = buildingService.getCurrentTurn(capturingPlayer.getId());
        List<EquipmentStack> transferred = buildingService.captureWeaponCache(
                cacheId,
                capturingPlayer.getId(),
                currentTurn
        );
        return ResponseEntity.ok(new CaptureWeaponCacheResponse(transferred.size()));
    }

    /**
     * Capture une banque.
     * Le joueur capturant est déterminé par l'utilisateur authentifié.
     */
    @PostMapping("/bank/{bankId}/capture")
    public ResponseEntity<CaptureBankResponse> captureBank(
            @PathVariable Long bankId,
            @RequestBody CaptureRequest request) {
        CaptureValidation validation = validateCaptureRequest(request, bankId);
        if (validation.hasError()) {
            return ResponseEntity.status(validation.error().getStatusCode()).build();
        }

        Player capturingPlayer = validation.capturingPlayer();
        int currentTurn = buildingService.getCurrentTurn(capturingPlayer.getId());
        BuildingService.CaptureResult result = buildingService.captureBank(
                bankId,
                capturingPlayer.getId(),
                currentTurn
        );
        return ResponseEntity.ok(new CaptureBankResponse(
                result.money(),
                result.resources().size()
        ));
    }

    // === RECORDS POUR LES REQUÊTES/RÉPONSES ===

    public record MoveBuildingRequest(int newSectorNumber) {}

    /**
     * Requête de capture - ne contient que l'ID du joueur victime.
     * Le joueur capturant est déterminé par l'authentification.
     */
    public record CaptureRequest(Long victimPlayerId) {}

    public record CaptureWeaponCacheResponse(int equipmentsTransferred) {}

    public record CaptureBankResponse(double moneyTransferred, int resourcesTransferred) {}

    private record CaptureValidation(Player capturingPlayer, ResponseEntity<?> error) {
        static CaptureValidation success(Player player) { return new CaptureValidation(player, null); }
        static CaptureValidation failure(ResponseEntity<?> error) { return new CaptureValidation(null, error); }
        boolean hasError() { return error != null; }
    }
}
