package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.RankingEntryDto;
import com.mg.nmlonline.api.dto.RankingsDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Classements militaire (globalPower) et économique (totalEconomyPower), disponibles à partir du tour 2. */
@Service
public class RankingService {

    private static final int FIRST_RANKED_TURN = 2;

    private final PlayerRepository playerRepository;
    private final BoardRepository boardRepository;
    private final PlayerStatsService playerStatsService;

    public RankingService(PlayerRepository playerRepository, BoardRepository boardRepository,
                          PlayerStatsService playerStatsService) {
        this.playerRepository = playerRepository;
        this.boardRepository = boardRepository;
        this.playerStatsService = playerStatsService;
    }

    @Transactional(readOnly = true)
    public RankingsDto getRankings() {
        Board board = boardRepository.findAll().stream().findFirst().orElse(null);
        if (board == null) {
            return emptyRankings(0);
        }
        return buildRankings(board, playerRepository.findAll());
    }

    @Transactional
    public void updateRankingComment(Long playerId, String comment) {
        // Verrou : le flush réécrit toutes les colonnes du joueur (pas de @DynamicUpdate).
        Player player = playerRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur introuvable"));
        player.setRankingComment(comment == null || comment.isBlank() ? null : comment.trim());
    }

    /**
     * Recalcul en mémoire, jamais persisté : globalPower/totalEconomyPower ne sont rafraîchis
     * par aucun hook de fin de tour, et la transaction read-only ne flushe pas ces mutations.
     */
    RankingsDto buildRankings(Board board, List<Player> players) {
        int turn = board.getCurrentTurn();
        if (turn < FIRST_RANKED_TURN) {
            return emptyRankings(turn);
        }

        players.forEach(player -> playerStatsService.recalculateStats(player, board));

        RankingsDto dto = new RankingsDto();
        dto.setTurn(turn);
        dto.setMilitary(rank(players, player -> player.getStats().getGlobalPower()));
        dto.setEconomic(rank(players, player -> player.getStats().getTotalEconomyPower()));
        return dto;
    }

    private static RankingsDto emptyRankings(int turn) {
        RankingsDto dto = new RankingsDto();
        dto.setTurn(turn);
        dto.setMilitary(List.of());
        dto.setEconomic(List.of());
        return dto;
    }

    private static List<RankingEntryDto> rank(List<Player> players, ToDoubleFunction<Player> power) {
        return players.stream()
                .sorted(Comparator.comparingDouble(power).reversed().thenComparing(Player::getName))
                .map(player -> {
                    RankingEntryDto entry = new RankingEntryDto();
                    entry.setPlayerId(player.getId());
                    entry.setName(player.getName());
                    entry.setPower(power.applyAsDouble(player));
                    entry.setComment(player.getRankingComment());
                    return entry;
                })
                .toList();
    }
}
