package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.entity.PlayerEntity;
import com.mg.nmlonline.mapper.PlayerMapper;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    public PlayerService(PlayerRepository playerRepository,
                         PlayerMapper playerMapper) {
        this.playerRepository = playerRepository;
        this.playerMapper = playerMapper;
    }

    // --- Lecture (DTO) pour controllers ---
    public List<Player> findAll() {
        return playerRepository.findAll().stream()
                .map(playerMapper::toDomain)
                .collect(Collectors.toList());
    }

    public Player findByName(String name) {
        return playerRepository.findByName(name)
                .map(playerMapper::toDomain)
                .orElse(null);
    }

    @Transactional
    public Player create(Player player) {
        player.recalculateStats();
        PlayerEntity entity = playerMapper.toEntity(player);
        PlayerEntity saved = playerRepository.save(entity);
        return playerMapper.toDomain(saved);
    }

    public boolean delete(Long id) {
        if (!playerRepository.existsById(id)) return false;
        playerRepository.deleteById(id);
        return true;
    }
}
