package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.service.AdminService;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.domain.service.TurnService;
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
    private final TurnService turnService;

    public AdminController(AdminService adminService,
                           PlayerService playerService,
                           PlayerMapper playerMapper,
                           BoardService boardService,
                           TurnService turnService) {
        this.adminService = adminService;
        this.playerService = playerService;
        this.playerMapper = playerMapper;
        this.boardService = boardService;
        this.turnService = turnService;
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
}
