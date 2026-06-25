package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.BuyEquipmentItemDto;
import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.api.dto.ResourceBatchSaleResponseDto;
import com.mg.nmlonline.api.dto.ResourceSaleResponseDto;
import com.mg.nmlonline.api.dto.SellResourceBatchRequestDto;
import com.mg.nmlonline.domain.exception.InsufficientFundsException;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.domain.service.ResourceService;
import com.mg.nmlonline.mapper.PlayerMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerMapper playerMapper;
    private final BoardService boardService;
    private final ResourceService resourceService;

    public PlayerController(PlayerService playerService, PlayerMapper playerMapper,
                          BoardService boardService, ResourceService resourceService) {
        this.playerService = playerService;
        this.playerMapper = playerMapper;
        this.boardService = boardService;
        this.resourceService = resourceService;
    }

    @GetMapping
    public Page<PlayerDto> findAll(Pageable pageable) {
        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
        return playerService.findAll(pageable)
                .map(player -> playerMapper.toDtoWithSectors(player, board));
    }

    @GetMapping("/{name}")
    public ResponseEntity<PlayerDto> findByName(@PathVariable String name) {
        Player player = playerService.findByName(name);
        if (player == null) {
            return ResponseEntity.notFound().build();
        }
        Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
        return ResponseEntity.ok(playerMapper.toDtoWithSectors(player, board));
    }

    /**
     * Vend une ressource de l'inventaire d'un joueur
     * @param resourceId L'ID de la ressource à vendre (PlayerResource)
     * @param quantity La quantité à vendre
     * @return 200 OK avec les détails de la vente (nom, quantité, montant) si la vente est réussie
     */
    @PostMapping("/resources/{playerResourceId}/sell")
    public ResponseEntity<ResourceSaleResponseDto> sellResource(@PathVariable Long playerResourceId, @RequestParam("quantity") int quantity, HttpServletRequest request) {
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
            ResourceService.SaleResult result = resourceService.sellResource(playerResourceId, quantity, authenticatedUserId);
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
        }
    }

    /**
     * Vend un lot de ressources de l'inventaire du joueur authentifié de manière atomique.
     */
    @PostMapping("/resources/sell-batch")
    public ResponseEntity<ResourceBatchSaleResponseDto> sellResourcesBatch(@Valid @RequestBody SellResourceBatchRequestDto requestDto,
                                                                           HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401)
                    .body(new ResourceBatchSaleResponseDto("User not authenticated", 0, List.of()));
        }
        try {
            List<ResourceService.SaleResult> results = resourceService.sellResourcesBatch(authenticatedUserId, requestDto.getItems());
            List<ResourceSaleResponseDto> sales = results.stream()
                    .map(r -> new ResourceSaleResponseDto(
                            "Resource sold successfully",
                            r.saleValue(),
                            r.resourceName(),
                            r.quantitySold()
                    ))
                    .toList();
            double totalValue = results.stream().mapToDouble(ResourceService.SaleResult::saleValue).sum();
            return ResponseEntity.ok(new ResourceBatchSaleResponseDto("Batch sale successful", totalValue, sales));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ResourceBatchSaleResponseDto(e.getMessage(), 0, List.of()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(new ResourceBatchSaleResponseDto(e.getMessage(), 0, List.of()));
        }
    }

    /**
     * Achète une liste d'équipements pour le joueur authentifié.
     * Le coût total est déduit de son solde de manière atomique.
     */
    @PostMapping("/equipment/buy")
    public ResponseEntity<PlayerDto> buyEquipments(@RequestBody List<BuyEquipmentItemDto> items,
                                                   HttpServletRequest request) {
        Long authenticatedUserId = (Long) request.getAttribute("userId");
        if (authenticatedUserId == null) {
            return ResponseEntity.status(401).build();
        }
        Player player = playerService.findByUserId(authenticatedUserId);
        if (player == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Player saved = playerService.buyEquipments(player.getId(), items);
            Board board = boardService.getAllBoards().stream().findFirst().orElse(null);
            return ResponseEntity.ok(playerMapper.toDtoWithSectors(saved, board));
        } catch (InsufficientFundsException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(null);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
