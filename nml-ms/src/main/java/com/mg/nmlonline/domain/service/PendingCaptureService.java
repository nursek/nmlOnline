package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.PendingCaptureDto;
import com.mg.nmlonline.domain.model.alliance.PendingCapture;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.player.Player;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PendingCaptureRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PendingCaptureService {

    private final PendingCaptureRepository repository;
    private final BoardRepository boardRepository;
    private final PlayerRepository playerRepository;

    public PendingCaptureService(PendingCaptureRepository repository,
                                 BoardRepository boardRepository,
                                 PlayerRepository playerRepository) {
        this.repository = repository;
        this.boardRepository = boardRepository;
        this.playerRepository = playerRepository;
    }

    /** Camp allié gagnant : le secteur redevient neutre, l'admin tranchera entre les candidats. */
    public void queueNeutral(Board board, int sectorNumber, int turn, List<Long> candidates) {
        if (board == null || candidates == null || candidates.isEmpty()) {
            return;
        }
        Sector sector = board.getSector(sectorNumber);
        if (sector == null) {
            return;
        }
        // captureAfterBattle puis captureByPresence repassent sur le même secteur au même tour : une seule ligne.
        if (repository.existsByBoardIdAndSectorNumberAndTurnAndResolvedFalse(board.getId(), sectorNumber, turn)) {
            return;
        }
        sector.setOwnerAndColor(null, null);
        repository.save(PendingCapture.create(board.getId(), sectorNumber, turn, candidates));
    }

    @Transactional(readOnly = true)
    public List<PendingCaptureDto> list(Long boardId) {
        List<PendingCapture> pendings = repository.findByBoardIdAndResolvedFalseOrderByIdAsc(boardId);
        Set<Long> ids = pendings.stream()
                .flatMap(pending -> pending.candidateIds().stream())
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> names = playerRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Player::getId, Player::getName, (a, b) -> a));
        return pendings.stream().map(pending -> {
            PendingCaptureDto dto = new PendingCaptureDto();
            dto.setId(pending.getId());
            dto.setBoardId(pending.getBoardId());
            dto.setSectorNumber(pending.getSectorNumber());
            dto.setTurn(pending.getTurn());
            dto.setCandidates(pending.candidateIds().stream().map(playerId -> {
                PendingCaptureDto.CandidateDto candidate = new PendingCaptureDto.CandidateDto();
                candidate.setPlayerId(playerId);
                candidate.setPlayerName(names.get(playerId));
                return candidate;
            }).toList());
            return dto;
        }).toList();
    }

    /** Décision admin : attribution immédiate, sans attendre un tour. */
    public void resolve(Long boardId, Long pendingId, Long playerId) {
        PendingCapture pending = requirePendingOfBoard(boardId, pendingId);
        if (pending.isResolved()) {
            throw new IllegalStateException("Cette attribution est déjà résolue.");
        }
        Board board = boardRepository.findById(pending.getBoardId())
                .orElseThrow(() -> new EntityNotFoundException("Plateau introuvable : " + pending.getBoardId()));
        if (playerRepository.findById(playerId)
                .map(player -> !boardId.equals(player.getBoardId()))
                .orElse(true)) {
            throw new IllegalArgumentException("Le joueur ciblé ne joue pas dans cette partie");
        }
        String color = board.getSectorsByOwner(playerId).stream()
                .map(Sector::getColor)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("#ffffff");
        board.assignOwner(pending.getSectorNumber(), playerId, color);
        boardRepository.save(board);
        pending.setResolved(true);
        repository.save(pending);
    }

    public void dismiss(Long boardId, Long pendingId) {
        PendingCapture pending = requirePendingOfBoard(boardId, pendingId);
        pending.setResolved(true);
        repository.save(pending);
    }

    private PendingCapture requirePendingOfBoard(Long boardId, Long pendingId) {
        PendingCapture pending = repository.findById(pendingId)
                .orElseThrow(() -> new EntityNotFoundException("Attribution en attente introuvable : " + pendingId));
        if (!pending.getBoardId().equals(boardId)) {
            throw new SecurityException("Cette attribution appartient à une autre partie.");
        }
        return pending;
    }
}
