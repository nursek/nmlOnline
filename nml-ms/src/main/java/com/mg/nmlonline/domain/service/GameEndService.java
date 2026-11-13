package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameEndService {

    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;

    public GameEndService(BoardRepository boardRepository, PlayerRepository playerRepository) {
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
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

    public record GameOutcome(boolean finished, Integer finalTurn, Long winnerPlayerId,
                              String winnerName, boolean posthumous, boolean winnerAdjusted) {

        private static GameOutcome running() {
            return new GameOutcome(false, null, null, null, false, false);
        }
    }
}
