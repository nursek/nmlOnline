package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.api.dto.SectorDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.service.AdminService;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.mapper.PlayerMapper;
import com.mg.nmlonline.mapper.SectorMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    private final SectorMapper sectorMapper;

    public AdminController(AdminService adminService,
                           PlayerService playerService,
                           PlayerMapper playerMapper,
                           BoardService boardService,
                           SectorMapper sectorMapper) {
        this.adminService = adminService;
        this.playerService = playerService;
        this.playerMapper = playerMapper;
        this.boardService = boardService;
        this.sectorMapper = sectorMapper;
    }

    /**
     * Récupère tous les joueurs avec leurs détails complets.
     */
    @GetMapping("/players")
    public List<PlayerDto> getAllPlayers() {
        return playerService.findAll().stream()
                .map(this::enrichPlayerWithSectors)
                .toList();
    }

    /**
     * Exporte un joueur au format JSON (compatible avec l'import).
     */
    @GetMapping("/players/{id}/export")
    public ResponseEntity<Map<String, Object>> exportPlayer(@PathVariable Long id) {
        try {
            Map<String, Object> exportData = adminService.exportPlayer(id);
            return ResponseEntity.ok(exportData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Importe un joueur depuis un fichier JSON.
     * Si un joueur avec le même nom existe, il est remplacé.
     */
    @PostMapping(value = "/players/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importPlayer(@RequestParam("file") MultipartFile file) {
        try {
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            Player player = adminService.importPlayer(jsonContent);
            return ResponseEntity.ok(enrichPlayerWithSectors(player));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Données invalides : " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors de l'import du joueur"));
        }
    }

    /**
     * Supprime complètement un joueur et réinitialise ses secteurs.
     */
    @DeleteMapping("/players/{id}")
    public ResponseEntity<?> deletePlayer(@PathVariable Long id) {
        try {
            adminService.deletePlayer(id);
            return ResponseEntity.ok(Map.of("message", "Joueur supprimé avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Enrichit un PlayerDto avec les secteurs complets depuis la board par défaut.
     * Note : utilise la première board disponible (le jeu ne supporte qu'une board active).
     */
    private PlayerDto enrichPlayerWithSectors(Player player) {
        if (player == null) return null;

        PlayerDto dto = playerMapper.toDto(player);

        Board board = null;
        List<Board> boards = boardService.getAllBoards();
        if (!boards.isEmpty()) {
            board = boards.getFirst();
        }

        List<SectorDto> playerSectors = new ArrayList<>();
        if (board != null && player.getId() != null) {
            for (Sector sector : board.getAllSectors()) {
                if (player.getId().equals(sector.getOwnerId())) {
                    playerSectors.add(sectorMapper.toDto(sector));
                }
            }
        }
        dto.setSectors(playerSectors);

        return dto;
    }
}
