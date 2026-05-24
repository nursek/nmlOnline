package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BuyEquipmentItemDto;
import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.api.dto.ResourceSaleResponseDto;
import com.mg.nmlonline.api.dto.SectorDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.equipment.Equipment;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.EquipmentService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.domain.service.ResourceService;
import com.mg.nmlonline.mapper.PlayerMapper;
import com.mg.nmlonline.mapper.SectorMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerMapper playerMapper;
    private final BoardService boardService;
    private final SectorMapper sectorMapper;
    private final ResourceService resourceService;
    private final EquipmentService equipmentService;

    public PlayerController(PlayerService playerService, PlayerMapper playerMapper,
                          BoardService boardService, SectorMapper sectorMapper,
                          ResourceService resourceService, EquipmentService equipmentService) {
        this.playerService = playerService;
        this.playerMapper = playerMapper;
        this.boardService = boardService;
        this.sectorMapper = sectorMapper;
        this.resourceService = resourceService;
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public List<PlayerDto> findAll() {
        return playerService.findAll().stream()
                .map(this::enrichPlayerWithSectors)
                .toList();
    }

    @GetMapping("/{name}")
    public ResponseEntity<PlayerDto> findByName(@PathVariable String name) {
        Player player = playerService.findByName(name);
        if (player == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(enrichPlayerWithSectors(player));
    }

    /**
     * Vend une ressource de l'inventaire d'un joueur
     * @param resourceId L'ID de la ressource à vendre (PlayerResource)
     * @param quantity La quantité à vendre
     * @return 200 OK avec les détails de la vente (nom, quantité, montant) si la vente est réussie
     */
    @PostMapping("/resources/sell/{resourceId}")
    public ResponseEntity<ResourceSaleResponseDto> sellResource(@PathVariable Long resourceId, @RequestParam("quantity") int quantity, HttpServletRequest request) {
        if (quantity <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ResourceSaleResponseDto("Quantity must be greater than 0", 0, null, 0));
        }
        Long authenticatedUserId = (Long) request.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401)
                    .body(new ResourceSaleResponseDto("User not authenticated", 0, null, 0));
        }
        try {
            ResourceService.SaleResult result = resourceService.sellResource(resourceId, quantity, authenticatedUserId);
            ResourceSaleResponseDto response = new ResourceSaleResponseDto(
                "Resource sold successfully",
                result.saleValue(),
                result.resourceName(),
                result.quantitySold()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResourceSaleResponseDto(e.getMessage(), 0, null, 0));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(new ResourceSaleResponseDto(e.getMessage(), 0, null, 0));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(new ResourceSaleResponseDto(e.getMessage(), 0, null, 0));
        }
    }

    /**
     * Achète une liste d'équipements pour le joueur authentifié.
     * Le coût total est déduit de son solde.
     */
    @PostMapping("/equipment/buy")
    public ResponseEntity<PlayerDto> buyEquipments(@RequestBody List<BuyEquipmentItemDto> items,
                                                   HttpServletRequest request) {
        //TODO: L’achat d’équipements est géré dans le controller, item par item, avec un continue silencieux pour les quantités <= 0 et un échec possible après des débits déjà effectués en mémoire. Déplacer la logique dans un service métier @Transactional qui valide d’abord toute la commande (items valides + coût total + existence), puis applique les changements ; et remplacer le TODO par l’implémentation.
        Long authenticatedUserId = (Long) request.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).build();
        }
        Player player = playerService.findByUserId(authenticatedUserId);
        if (player == null) {
            return ResponseEntity.notFound().build();
        }
        for (BuyEquipmentItemDto item : items) {
            if (item.getQuantity() <= 0) continue;
            Equipment equipment = equipmentService.findByName(item.getName()).orElse(null);
            if (equipment == null) {
                return ResponseEntity.badRequest().build();
            }
            boolean success = player.buyEquipment(equipment, item.getQuantity());
            if (!success) {
                return ResponseEntity.status(402).build();
            }
        }
        Player saved = playerService.save(player);
        return ResponseEntity.ok(enrichPlayerWithSectors(saved));
    }
    // TODO :Achat non atomique : si un item échoue (fonds insuffisants) après plusieurs buyEquipment réussis, l’état du joueur a déjà été modifié en mémoire (et peut potentiellement être flushé selon le contexte JPA). Déplacez la logique dans un service @Transactional qui (1) valide d’abord tous les items (existence + coût total), puis (2) applique les modifications, afin d’avoir un rollback automatique en cas d’échec.

    /**
     * Enrichit un PlayerDto avec les secteurs complets depuis la board par défaut.
     * Note : utilise la première board disponible (le jeu ne supporte qu'une board active).
     */
    private PlayerDto enrichPlayerWithSectors(Player player) {
        if (player == null) {
            return null;
        }

        PlayerDto dto = playerMapper.toDto(player);

        // Récupérer la première board disponible
        Board board = null;
        List<Board> boards = boardService.getAllBoards();
        if (!boards.isEmpty()) {
            board = boards.getFirst();
        }

        // Enrichir avec les secteurs du joueur
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
