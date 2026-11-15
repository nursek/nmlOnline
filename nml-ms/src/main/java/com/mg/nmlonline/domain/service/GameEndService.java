package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.audit.AdminAuditAction;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.press.EliminationEvent;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.EliminationEventRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class GameEndService {

    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;
    private final EliminationEventRepository eliminationEventRepository;
    private final AdminAuditService adminAuditService;

    public GameEndService(BoardRepository boardRepository, PlayerRepository playerRepository,
                          EliminationEventRepository eliminationEventRepository,
                          AdminAuditService adminAuditService) {
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.eliminationEventRepository = eliminationEventRepository;
        this.adminAuditService = adminAuditService;
    }

    @Transactional(readOnly = true)
    public GameOutcome getState(Long boardId) {
        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null || !board.isFinished()) {
            return GameOutcome.running();
        }
        Player winner = board.getWinnerPlayerId() == null
                ? null
                : playerRepository.findById(board.getWinnerPlayerId()).orElse(null);
        return new GameOutcome(true, board.getCurrentTurn(), board.getWinnerPlayerId(),
                winner != null ? winner.getName() : null,
                winner != null && winner.isEliminated(),
                board.isWinnerAdjusted());
    }

    @Transactional
    public GameOutcome finishIfOver(Board board, List<Player> players, List<EliminationEvent> newEvents) {
        if (board.isFinished()) {
            return getState(board.getId());
        }
        if (players.size() < 2) {
            return GameOutcome.running();
        }
        List<Player> alive = players.stream().filter(player -> !player.isEliminated()).toList();
        if (alive.size() > 1) {
            return GameOutcome.running();
        }
        Player winner = alive.size() == 1 ? alive.getFirst() : posthumousWinner(board, newEvents);
        if (winner == null) {
            return GameOutcome.running();
        }
        board.setFinishedAt(Instant.now());
        board.setWinnerPlayerId(winner.getId());
        board.setWinnerAdjusted(false);
        return new GameOutcome(true, board.getCurrentTurn(), winner.getId(), winner.getName(),
                winner.isEliminated(), false);
    }

    private Player posthumousWinner(Board board, List<EliminationEvent> newEvents) {
        EliminationEvent last = newEvents.isEmpty()
                ? eliminationEventRepository.findFirstByBoardIdOrderByDeathIndexDesc(board.getId())
                        .orElse(null)
                : newEvents.getLast();
        return last == null
                ? null
                : playerRepository.findById(last.getVictimPlayerId()).orElse(null);
    }

    @Transactional
    public GameOutcome adjustWinner(Long boardId, Long playerId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Plateau " + boardId + " introuvable"));
        if (!board.isFinished()) {
            throw new IllegalStateException("La partie n'est pas terminée");
        }
        boolean survivor = playerRepository.findByBoardId(boardId).stream()
                .anyMatch(player -> !player.isEliminated());
        if (survivor) {
            throw new IllegalStateException("Le champion n'est ajustable qu'en victoire posthume");
        }
        Player target = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Joueur " + playerId + " introuvable"));
        if (!boardId.equals(target.getBoardId()) || !target.isEliminated()) {
            throw new IllegalArgumentException("Le champion doit être un joueur éliminé de cette partie");
        }
        Long natural = eliminationEventRepository.findFirstByBoardIdOrderByDeathIndexDesc(boardId)
                .map(EliminationEvent::getVictimPlayerId)
                .orElse(null);
        board.setWinnerPlayerId(playerId);
        board.setWinnerAdjusted(!playerId.equals(natural));
        boardRepository.save(board);
        adminAuditService.record(boardId, playerId, AdminAuditAction.GAME_WINNER_OVERRIDE,
                (board.isWinnerAdjusted() ? "Champion désigné" : "Champion naturel")
                        + " : " + target.getName());
        return getState(boardId);
    }

    public record GameOutcome(boolean finished, Integer finalTurn, Long winnerPlayerId,
                              String winnerName, boolean posthumous, boolean winnerAdjusted) {

        private static GameOutcome running() {
            return new GameOutcome(false, null, null, null, false, false);
        }
    }
}
