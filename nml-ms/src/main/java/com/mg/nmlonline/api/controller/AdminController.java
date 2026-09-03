package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.AdminMovementOrderDto;
import com.mg.nmlonline.api.dto.MovementResolutionResultDto;
import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.api.dto.ResolvedBattleDto;
import com.mg.nmlonline.api.dto.TurnFinalizeResultDto;
import com.mg.nmlonline.api.dto.TurnResolutionStateDto;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.service.AdminService;
import com.mg.nmlonline.domain.service.BoardAssetStorageService;
import com.mg.nmlonline.domain.service.MovementAdminService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.domain.service.TurnResolutionOrchestrator;
import com.mg.nmlonline.domain.service.TurnService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final PlayerService playerService;
    private final BoardAssetStorageService boardAssetStorageService;
    private final TurnService turnService;
    private final MovementAdminService movementAdminService;
    private final TurnResolutionOrchestrator turnResolutionOrchestrator;

    public AdminController(AdminService adminService,
                           PlayerService playerService,
                           BoardAssetStorageService boardAssetStorageService,
                           TurnService turnService,
                           MovementAdminService movementAdminService,
                           TurnResolutionOrchestrator turnResolutionOrchestrator) {
        this.adminService = adminService;
        this.playerService = playerService;
        this.boardAssetStorageService = boardAssetStorageService;
        this.turnService = turnService;
        this.movementAdminService = movementAdminService;
        this.turnResolutionOrchestrator = turnResolutionOrchestrator;
    }

    @GetMapping("/players")
    public Page<PlayerDto> getAllPlayers(Pageable pageable) {
        return playerService.findAllDto(pageable);
    }

    @GetMapping("/players/{id}/export")
    public ResponseEntity<Map<String, Object>> exportPlayer(@PathVariable Long id) {
        Map<String, Object> exportData = adminService.exportPlayer(id);
        return ResponseEntity.ok(exportData);
    }

    /**
     * Import upsert par nom. Avec password : crée/met à jour un compte User (hash+pepper).
     * Sans password : joueur seul (compte géré par ailleurs en dev).
     */
    @PostMapping(value = "/players/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlayerDto> importPlayer(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password
    ) throws java.io.IOException {
        String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(adminService.importPlayerDto(jsonContent, password));
    }

    /**
     * Supprime le joueur et réinitialise ses secteurs.
     */
    @DeleteMapping("/players/{id}")
    public ResponseEntity<Map<String, String>> deletePlayer(@PathVariable Long id) {
        adminService.deletePlayer(id);
        return ResponseEntity.ok(Map.of("message", "Joueur supprimé avec succès"));
    }

    /**
     * Étape 1/2 création board prod : stocke les assets, renvoie URLs + compte de secteurs
     * du SVG pour valider le board.json à venir.
     */
    @PostMapping(value = "/boards/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadBoardAssets(
            @RequestParam("mapImage") MultipartFile mapImage,
            @RequestParam("svgOverlay") MultipartFile svgOverlay) throws java.io.IOException {
        String mapUrl = boardAssetStorageService.storeImage(mapImage);
        BoardAssetStorageService.StoredSvg svg = boardAssetStorageService.storeSvg(svgOverlay);
        return ResponseEntity.ok(Map.of(
                "mapImageUrl", mapUrl,
                "svgOverlayUrl", svg.url(),
                "svgSectorCount", svg.sectorCount()
        ));
    }

    /**
     * Étape 2/2 : import board.json (liste plate) ; les URLs de /boards/assets override celles du JSON.
     */
    @PostMapping(value = "/boards/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BoardDto> importBoard(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mapImageUrl", required = false) String mapImageUrl,
            @RequestParam(value = "svgOverlayUrl", required = false) String svgOverlayUrl) throws java.io.IOException {
        String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(adminService.importBoardDto(jsonContent, mapImageUrl, svgOverlayUrl));
    }

    @GetMapping("/turn/current")
    public Map<String, Object> getCurrentTurn() {
        return Map.of("currentTurn", turnService.getCurrentTurn());
    }

    /**
     * Résout les ordres PENDING puis incrémente le numéro de tour.
     */
    @PostMapping("/turn/next")
    public Map<String, Object> advanceTurn() {
        int newTurn = turnService.advanceTurn();
        logger.info("[ADMIN] Tour avancé -> {}", newTurn);
        return Map.of("currentTurn", newTurn);
    }

    @GetMapping("/turn/orders")
    public List<AdminMovementOrderDto> getOrders(
            @RequestParam(value = "status", required = false) MovementStatus status) {
        return movementAdminService.getOrdersForTurn(turnService.getCurrentTurn(), status);
    }

    /**
     * Dry-run : calcule la résolution sans persister (ordres restent PENDING).
     */
    @PostMapping("/turn/movements/preview")
    public MovementResolutionResultDto previewMovements() {
        return movementAdminService.previewMovements(turnService.getCurrentTurn());
    }

    /**
     * Résout et persiste les mouvements du tour courant sans avancer le numéro de tour.
     */
    @PostMapping("/turn/movements/resolve")
    public MovementResolutionResultDto resolveMovements() {
        MovementResolutionResultDto report = movementAdminService.resolveMovements(turnService.getCurrentTurn());
        logger.info("[ADMIN] Mouvements résolus : {} résolus, {} bloqués, {} conflits",
                report.getResolved().size(), report.getBlocked().size(), report.getConflicts().size());
        return report;
    }

    /**
     * Démarre une session pas-à-pas ; verrouille /turn/next et les autres sessions ; 409 si déjà active.
     */
    @PostMapping("/turn/resolve/start")
    public TurnResolutionStateDto startResolution() {
        TurnResolutionStateDto state = turnResolutionOrchestrator.startSession();
        logger.info("[ADMIN] Session pas-à-pas démarrée — tour à résoudre: {}", state.getTurnEnding());
        return state;
    }

    @GetMapping("/turn/resolve/state")
    public TurnResolutionStateDto getResolutionState() {
        return turnResolutionOrchestrator.getState();
    }

    /**
     * Avance d'un hop ; 409 si batailles en attente ou tous hops déjà faits.
     */
    @PostMapping("/turn/resolve/next-hop")
    public TurnResolutionStateDto advanceHop() {
        return turnResolutionOrchestrator.advanceHop();
    }

    /**
     * Résout la bataille conflictId (du DTO d'état) ; 404 si introuvable/déjà résolu.
     */
    @PostMapping("/turn/resolve/resolve-battle")
    public ResolvedBattleDto resolveBattle(@RequestParam int conflictId) {
        ResolvedBattleDto report = turnResolutionOrchestrator.resolveBattle(conflictId);
        logger.info("[ADMIN] Bataille résolue au secteur {} — {} pertes attaquant, {} pertes défenseur",
                report.getSectorNumber(), report.getAttackerCasualties(), report.getDefenderCasualties());
        return report;
    }

    /**
     * Finalise : incrémente le tour ; 409 si hops/batailles incomplets ; libère le verrou.
     */
    @PostMapping("/turn/resolve/finalize")
    public TurnFinalizeResultDto finalizeResolution() {
        return turnResolutionOrchestrator.finalizeTurn();
    }

    /**
     * Abandon soft : libère le verrou sans rollback des positions/combats déjà effectués.
     */
    @DeleteMapping("/turn/resolve")
    public ResponseEntity<Map<String, String>> abortResolution() {
        turnResolutionOrchestrator.abort();
        return ResponseEntity.ok(Map.of("message", "Session pas-à-pas abandonnée (positions déjà déplacées conservées)"));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }
}
