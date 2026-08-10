package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.AdminMovementOrderDto;
import com.mg.nmlonline.api.dto.MovementResolutionResultDto;
import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.movement.MovementStatus;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.service.AdminService;
import com.mg.nmlonline.domain.service.BoardAssetStorageService;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.MovementAdminService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.domain.service.TurnService;
import com.mg.nmlonline.mapper.BoardMapper;
import com.mg.nmlonline.mapper.PlayerMapper;
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

/**
 * Controller d'administration pour la gestion avancée des joueurs.
 * Protégé par le rôle ADMIN via SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final PlayerService playerService;
    private final PlayerMapper playerMapper;
    private final BoardService boardService;
    private final BoardMapper boardMapper;
    private final BoardAssetStorageService boardAssetStorageService;
    private final TurnService turnService;
    private final MovementAdminService movementAdminService;

    public AdminController(AdminService adminService,
                           PlayerService playerService,
                           PlayerMapper playerMapper,
                           BoardService boardService,
                           BoardMapper boardMapper,
                           BoardAssetStorageService boardAssetStorageService,
                           TurnService turnService,
                           MovementAdminService movementAdminService) {
        this.adminService = adminService;
        this.playerService = playerService;
        this.playerMapper = playerMapper;
        this.boardService = boardService;
        this.boardMapper = boardMapper;
        this.boardAssetStorageService = boardAssetStorageService;
        this.turnService = turnService;
        this.movementAdminService = movementAdminService;
    }

    /**
     * Récupère tous les joueurs avec leurs détails complets (paginé).
     */
    @GetMapping("/players")
    public Page<PlayerDto> getAllPlayers(Pageable pageable) {
        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
        return playerService.findAll(pageable)
                .map(player -> playerMapper.toDtoWithSectors(player, board));
    }

    /**
     * Exporte un joueur au format JSON (compatible avec l'import).
     */
    @GetMapping("/players/{id}/export")
    public ResponseEntity<Map<String, Object>> exportPlayer(@PathVariable Long id) {
        Map<String, Object> exportData = adminService.exportPlayer(id);
        return ResponseEntity.ok(exportData);
    }

    /**
     * Importe un joueur depuis un fichier JSON.
     * Si un joueur avec le même nom existe, il est remplacé.
     * Si {@code password} est fourni, un compte {@link com.mg.nmlonline.domain.model.user.User}
     * (rôle USER) est créé/mis à jour avec ce mot de passe haché+pepper, pour permettre
     * la connexion sous le nom du joueur. Sans password, le joueur est importé sans compte
     * (utile en dev où les comptes sont seedés par ailleurs).
     */
    @PostMapping(value = "/players/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlayerDto> importPlayer(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "password", required = false) String password
    ) throws java.io.IOException {
        String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        Player player = adminService.importPlayer(jsonContent, password);
        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
        return ResponseEntity.ok(playerMapper.toDtoWithSectors(player, board));
    }

    /**
     * Supprime complètement un joueur et réinitialise ses secteurs.
     */
    @DeleteMapping("/players/{id}")
    public ResponseEntity<Map<String, String>> deletePlayer(@PathVariable Long id) {
        adminService.deletePlayer(id);
        return ResponseEntity.ok(Map.of("message", "Joueur supprimé avec succès"));
    }

    /**
     * Upload des assets visuels du Board (image de fond + SVG overlay des secteurs).
     * Étape 1 du flux de création du board en prod : durations les fichiers sur disque,
     * renvoie les URLs à passer à {@code /import} + le compte de secteurs détectés dans le SVG
     * pour valider la cohérence avec le board.json que l'admin uploadera ensuite.
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
     * Importe le Board depuis un board.json (liste plate de secteurs).
     * Étape 2 : prend en plus les URLs renvoyées par {@code /boards/assets} pour
     * override celles éventuellement présentes dans le JSON.
     */
    @PostMapping(value = "/boards/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BoardDto> importBoard(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mapImageUrl", required = false) String mapImageUrl,
            @RequestParam(value = "svgOverlayUrl", required = false) String svgOverlayUrl) throws java.io.IOException {
        String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        Board board = adminService.importBoard(jsonContent, mapImageUrl, svgOverlayUrl);
        return ResponseEntity.ok(boardMapper.toDto(board));
    }

    // ==========================================================
    // === GESTION DU TOUR (source unique : TurnService) =========
    // ==========================================================

    /**
     * Tour courant du plateau.
     */
    @GetMapping("/turn/current")
    public Map<String, Object> getCurrentTurn() {
        return Map.of("currentTurn", turnService.getCurrentTurn());
    }

    /**
     * Termine le tour courant : résout les ordres de déplacement PENDING puis
     * incrémente le compteur. Retourne le nouveau numéro de tour.
     *
     * <p>ponytail: déclenchement manuel admin — remplacé plus tard par un scheduler
     * automatique end-of-turn (avec calcul des revenus/effets de bâtiments).
     */
    @PostMapping("/turn/next")
    public Map<String, Object> advanceTurn() {
        int newTurn = turnService.advanceTurn();
        logger.info("[ADMIN] Tour avancé -> {}", newTurn);
        return Map.of("currentTurn", newTurn);
    }

    // ==========================================================
    // === ORDRES DE DÉPLACEMENT (admin) =========================
    // ==========================================================

    /**
     * Liste tous les ordres de déplacement du tour courant, optionnellement
     * filtrés par statut (PENDING/RESOLVED/BLOCKED/CANCELLED). Sans
     * {@code status}, retourne tous les statuts du tour courant.
     */
    @GetMapping("/turn/orders")
    public List<AdminMovementOrderDto> getOrders(
            @RequestParam(value = "status", required = false) MovementStatus status) {
        return movementAdminService.getOrdersForTurn(turnService.getCurrentTurn(), status);
    }

    /**
     * Aperçu (dry-run) de la résolution des mouvements du tour courant :
     * calcule conflits potentiels et combats de transit <strong>sans</strong>
     * persister les déplacements ni marquer les ordres. Les ordres restent
     * PENDING après l'appel.
     */
    @PostMapping("/turn/movements/preview")
    public MovementResolutionResultDto previewMovements() {
        return movementAdminService.previewMovements(turnService.getCurrentTurn());
    }

    /**
     * Applique la résolution des mouvements du tour courant : déplace les
     * entités, marque les ordres RESOLVED/BLOCKED et persiste. Renvoie le
     * compte-rendu (ordres résolus/bloqués, conflits, combats de transit).
     *
     * <p>Indépendant de {@link #advanceTurn()} : ne décale pas le numéro de
     * tour. L'admin enchaîne ensuite « Finir le tour » pour passer au suivant
     * (la résolution sera alors un no-op car plus aucun ordre PENDING).
     */
    @PostMapping("/turn/movements/resolve")
    public MovementResolutionResultDto resolveMovements() {
        MovementResolutionResultDto report = movementAdminService.resolveMovements(turnService.getCurrentTurn());
        logger.info("[ADMIN] Mouvements résolus : {} résolus, {} bloqués, {} conflits",
                report.getResolved().size(), report.getBlocked().size(), report.getConflicts().size());
        return report;
    }
}
