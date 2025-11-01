package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.entity.PlayerEntity;
import com.mg.nmlonline.mapper.PlayerMapper;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final SectorService sectorService;

    public PlayerService(PlayerRepository playerRepository,
                         PlayerMapper playerMapper,
                         SectorService sectorService) {
        this.playerRepository = playerRepository;
        this.playerMapper = playerMapper;
        this.sectorService = sectorService;
    }

    // --- Lecture (DTO) pour controllers ---
    public List<Player> findAll() {
        return playerRepository.findAll().stream()
                .map(playerMapper::toDomain)
                .toList();
    }

    public Player findByName(String name) {
        return playerRepository.findByName(name)
                .map(playerMapper::toDomain)
                .orElse(null);
    }

    @Transactional
    public Player create(Player player) {
        // Version simple : toujours créer/remplacer
        PlayerEntity entity = playerMapper.toEntity(player);
        PlayerEntity saved = playerRepository.save(entity);
        return playerMapper.toDomain(saved);
    }

    /**
     * Recalcule les statistiques d'un joueur en utilisant le Board.
     * À appeler après avoir assigné des secteurs au joueur.
     *
     * @param playerId ID du joueur
     * @param board Le plateau de jeu
     */
    @Transactional
    public void recalculatePlayerStats(Long playerId, Board board) {
        Player player = playerRepository.findById(playerId)
                .map(playerMapper::toDomain)
                .orElse(null);
        if (player != null && board != null) {
            PlayerStatsService statsService = new PlayerStatsService();
            statsService.recalculateStats(player, board);
            // Sauvegarder les stats mises à jour
            PlayerEntity entity = playerMapper.toEntity(player);
            playerRepository.save(entity);
        }
    }

    public boolean delete(Long id) {
        if (!playerRepository.existsById(id)) return false;
        playerRepository.deleteById(id);
        sectorService.removePlayerFromSectors(id);
        return true;
    }
}
