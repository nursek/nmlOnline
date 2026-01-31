package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final SectorService sectorService;

    public PlayerService(PlayerRepository playerRepository,
                         SectorService sectorService) {
        this.playerRepository = playerRepository;
        this.sectorService = sectorService;
    }

    // --- Lecture ---
    public List<Player> findAll() {
        return playerRepository.findAll();
    }

    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id);
    }

    /**
     * Find a player by name.
     * @param name the player name to search for
     * @return the Player if found, null otherwise
     */
    public Player findByName(String name) {
        return playerRepository.findByName(name).orElse(null);
    }

    @Transactional
    public Player create(Player player) {
        return playerRepository.save(player);
    }

    @Transactional
    public Player save(Player player) {
        return playerRepository.save(player);
    }

    /**
     * Recalcule les statistiques d'un joueur en utilisant le Board.
     */
    @Transactional
    public void recalculatePlayerStats(Long playerId, Board board) {
        Optional<Player> playerOpt = playerRepository.findById(playerId);
        if (playerOpt.isPresent() && board != null) {
            Player player = playerOpt.get();
            PlayerStatsService statsService = new PlayerStatsService();
            statsService.recalculateStats(player, board);
            playerRepository.save(player);
        }
    }

    @Transactional
    public boolean delete(Long id) {
        if (!playerRepository.existsById(id)) return false;
        playerRepository.deleteById(id);
        sectorService.removePlayerFromSectors(id);
        return true;
    }
}
