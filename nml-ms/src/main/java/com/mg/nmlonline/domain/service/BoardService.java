package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.PlayerRepository;
import com.mg.nmlonline.mapper.BoardMapper;
import com.mg.nmlonline.mapper.SectorMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardMapper boardMapper;
    private final PlayerRepository playerRepository;
    private final AllianceGraph allianceGraph;

    public BoardService(BoardRepository boardRepository, BoardMapper boardMapper,
                        PlayerRepository playerRepository, AllianceGraph allianceGraph) {
        this.boardRepository = boardRepository;
        this.boardMapper = boardMapper;
        this.playerRepository = playerRepository;
        this.allianceGraph = allianceGraph;
    }

    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }

    public Optional<Board> getBoardById(Long id) {
        return boardRepository.findById(id);
    }

    public Optional<Board> getBoardByName(String name) {
        return boardRepository.findByName(name);
    }

    public Board findByName(String name) {
        return boardRepository.findByName(name).orElse(null);
    }

    // Upsert non-destructif : ne jamais getSectorsList().clear() — cascade-delete des secteurs + armées.
    public Board saveBoard(Board board, String boardName) {
        Optional<Board> existingBoardOpt = boardRepository.findByName(boardName);

        if (existingBoardOpt.isPresent()) {
            Board existingBoard = existingBoardOpt.get();

            existingBoard.setMapImageUrl(board.getMapImageUrl());
            existingBoard.setSvgOverlayUrl(board.getSvgOverlayUrl());

            Map<Integer, Sector> existingByNumber = new HashMap<>();
            for (Sector s : existingBoard.getSectorsList()) {
                existingByNumber.put(s.getNumber(), s);
            }

            for (Sector incoming : board.getSectorsList()) {
                Sector existing = existingByNumber.get(incoming.getNumber());
                if (existing != null) {
                    // Rafraîchir la géométrie SANS toucher à owner_id / color / army / buildings.
                    existing.setName(incoming.getName());
                    existing.setIncome(incoming.getIncome());
                    if (incoming.getResourceName() != null) {
                        existing.setResourceName(incoming.getResourceName());
                    }
                    existing.setX(incoming.getX());
                    existing.setY(incoming.getY());
                    existing.setNeighbors(new ArrayList<>(incoming.getNeighbors()));
                } else {
                    incoming.setBoard(existingBoard);
                    existingBoard.getSectorsList().add(incoming);
                }
            }

            return boardRepository.save(existingBoard);
        } else {
            board.setName(boardName);
            return boardRepository.save(board);
        }
    }

    public Board save(Board board) {
        return boardRepository.save(board);
    }

    public void deleteBoard(Long id) {
        boardRepository.deleteById(id);
    }

    public Optional<Sector> getSectorFromBoard(Long boardId, int sectorNumber) {
        return getBoardById(boardId)
                .map(board -> board.getSector(sectorNumber));
    }

    public boolean assignOwnerToSector(Long boardId, int sectorNumber, Long playerId, String colorHex) {
        Optional<Board> boardOpt = getBoardById(boardId);
        if (boardOpt.isEmpty()) {
            return false;
        }

        Board board = boardOpt.get();
        try {
            board.assignOwner(sectorNumber, playerId, colorHex);
            boardRepository.save(board);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean areNeighbors(Long boardId, int sector1, int sector2) {
        return getBoardById(boardId)
                .map(board -> board.areNeighbors(sector1, sector2))
                .orElse(false);
    }

    public boolean hasConflict(Long boardId, int sector1, int sector2) {
        return getBoardById(boardId)
                .map(board -> board.hasConflict(sector1, sector2))
                .orElse(false);
    }

    // Mapping dans la transaction (sectorsList et sous-collections sont LAZY).

    public List<BoardDto> getAllBoardsDto(Long requesterUserId) {
        SectorMapper.Visibility visibility = visibilityFor(requesterUserId);
        return getAllBoards().stream().map(board -> boardMapper.toPublicDto(board, visibility)).toList();
    }

    public Optional<BoardDto> getBoardByIdDto(Long id, Long requesterUserId) {
        SectorMapper.Visibility visibility = visibilityFor(requesterUserId);
        return getBoardById(id).map(board -> boardMapper.toPublicDto(board, visibility));
    }

    public Optional<BoardDto> getBoardByNameDto(String name, Long requesterUserId) {
        SectorMapper.Visibility visibility = visibilityFor(requesterUserId);
        return getBoardByName(name).map(board -> boardMapper.toPublicDto(board, visibility));
    }

    private SectorMapper.Visibility visibilityFor(Long requesterUserId) {
        if (requesterUserId == null) {
            return SectorMapper.Visibility.none();
        }
        return playerRepository.findByUserId(requesterUserId)
                .map(player -> new SectorMapper.Visibility(
                        player.getId(), allianceGraph.alliedPlayerIds(player.getId())))
                .orElseGet(SectorMapper.Visibility::none);
    }

    public BoardDto createBoardDto(BoardDto boardDto) {
        Board board = boardMapper.toDomain(boardDto);
        Board saved = saveBoard(board, boardDto.getName());
        return boardMapper.toDto(saved);
    }
}
