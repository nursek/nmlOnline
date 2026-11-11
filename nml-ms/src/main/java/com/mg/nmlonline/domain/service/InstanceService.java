package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.InstanceDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.infrastructure.repository.BoardAdminRepository;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstanceService {

    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;
    private final BoardAdminRepository boardAdminRepository;

    public InstanceService(BoardRepository boardRepository, PlayerRepository playerRepository,
                           BoardAdminRepository boardAdminRepository) {
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
        this.boardAdminRepository = boardAdminRepository;
    }

    @Transactional(readOnly = true)
    public List<InstanceDto> listForUser(Long userId, boolean platformAdmin) {
        return boardRepository.findAll().stream()
                .sorted((first, second) -> first.getName().compareToIgnoreCase(second.getName()))
                .map(board -> toDto(board, userId, platformAdmin))
                .filter(instance -> instance.role() != null)
                .toList();
    }

    private InstanceDto toDto(Board board, Long userId, boolean platformAdmin) {
        Player player = userId == null ? null
                : playerRepository.findByUserIdAndBoardId(userId, board.getId()).orElse(null);
        boolean gameMaster = userId != null && boardAdminRepository.existsByBoardIdAndUserId(board.getId(), userId);
        String role = platformAdmin ? "ADMIN" : gameMaster ? "MJ" : player != null ? "PLAYER" : null;
        boolean participant = player != null || gameMaster;
        long playerCount = playerRepository.countByBoardId(board.getId());
        return new InstanceDto(board.getId(), board.getName(), board.getCurrentTurn(), playerCount,
                player == null ? null : player.getId(),
                player == null ? null : player.getName(),
                player != null && player.isEliminated(),
                role, participant);
    }
}
