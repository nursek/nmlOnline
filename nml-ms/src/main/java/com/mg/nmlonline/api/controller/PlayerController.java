package com.mg.nmlonline.api.controller;

import com.mg.nmlonline.api.dto.PlayerDto;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.service.PlayerService;
import com.mg.nmlonline.mapper.PlayerMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;
    private final PlayerMapper playerMapper;

    public PlayerController(PlayerService playerService, PlayerMapper playerMapper) {
        this.playerService = playerService;
        this.playerMapper = playerMapper;
    }

    @GetMapping
    public List<PlayerDto> findAll() {
        return playerService.findAll().stream()
                .map(playerMapper::toDto)
                .toList();
    }

    @GetMapping("/{name}")
    public PlayerDto findByName(@PathVariable("name") String name) {
        Player player = playerService.findByName(name);
        return playerMapper.toDto(player);
    }

    @PostMapping
    public PlayerDto create(@RequestBody PlayerDto dto) {
        System.out.println("Created player: " + dto.getName());
        Player player = playerMapper.toDomain(dto);
        Player created = playerService.create(player);
        System.out.println("Created player: " + created.getName());
        return playerMapper.toDto(created);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        if(!playerService.delete(id)) {
            throw new RuntimeException("Equipment with id " + id + " not found.");
        }
    }
}
