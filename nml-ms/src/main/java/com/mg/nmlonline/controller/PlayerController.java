package com.mg.nmlonline.controller;

import com.mg.nmlonline.entity.player.PlayerEntity;
import com.mg.nmlonline.repository.PlayerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerRepository playerRepository;

    public PlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @GetMapping
    public List<PlayerEntity> listAll() {
        return playerRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerEntity> getOne(@PathVariable Long id) {
        return playerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PlayerEntity> create(@RequestBody PlayerEntity payload) {
        PlayerEntity saved = playerRepository.save(payload);
        return ResponseEntity.created(URI.create("/api/players/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerEntity> update(@PathVariable Long id, @RequestBody PlayerEntity payload) {
        if (!playerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        payload.setId(id);
        PlayerEntity saved = playerRepository.save(payload);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!playerRepository.existsById(id)) return ResponseEntity.notFound().build();
        playerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
