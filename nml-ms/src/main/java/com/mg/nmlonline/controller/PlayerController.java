package com.mg.nmlonline.controller;

import com.mg.nmlonline.dto.PlayerDto;
import com.mg.nmlonline.entity.player.PlayerEntity;
import com.mg.nmlonline.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public List<PlayerDto> listAll() {
        return playerService.listAllDtos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerDto> getOne(@PathVariable Long id) {
        return playerService.findDtoById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<PlayerDto> exportPlayer(@PathVariable Long id) {
        return getOne(id);
    }

    @PostMapping
    public ResponseEntity<PlayerDto> create(@RequestBody PlayerDto payload) {
        PlayerDto saved = playerService.createFromDto(payload);
        return ResponseEntity.created(URI.create("/api/players/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerDto> update(@PathVariable Long id, @RequestBody PlayerDto payload) {
        PlayerDto updated = playerService.updateFromDto(id, payload);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = playerService.deleteEntity(id);
        if (!deleted) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }
}
