package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.api.dto.SectorDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.service.BoardService;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.mapper.PlayerMapper;
import com.mg.nmlonline.mapper.SectorMapper;
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

    public PlayerController(PlayerService playerService, PlayerMapper playerMapper,
                          BoardService boardService, SectorMapper sectorMapper) {
        this.playerService = playerService;
        this.playerMapper = playerMapper;
        this.boardService = boardService;
        this.sectorMapper = sectorMapper;
    }

    @GetMapping
    public List<PlayerDto> findAll() {
        return playerService.findAll().stream()
                .map(this::enrichPlayerWithSectors)
                .toList();
    }

    @GetMapping("/{name}")
    public ResponseEntity<PlayerDto> findByName(@PathVariable("name") String name) {
        Player player = playerService.findByName(name);
        if (player == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(enrichPlayerWithSectors(player));
    }

    @PostMapping
    public PlayerDto create(@RequestBody PlayerDto dto) {
        System.out.println("Created player: " + dto.getName());
        Player player = playerMapper.toDomain(dto);
        Player created = playerService.create(player);
        System.out.println("Created player: " + created.getName());
        return enrichPlayerWithSectors(created);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        if(!playerService.delete(id)) {
            throw new RuntimeException("Player with id " + id + " not found.");
        }
    }

    /**
     * Enrichit un PlayerDto avec les secteurs complets depuis la board par défaut
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
