package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.entity.BoardEntity;
import com.mg.nmlonline.infrastructure.entity.SectorEntity;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.mapper.BoardMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardMapper boardMapper;

    public BoardService(BoardRepository boardRepository, BoardMapper boardMapper) {
        this.boardRepository = boardRepository;
        this.boardMapper = boardMapper;
    }

    /**
     * Récupère toutes les boards
     */
    public List<Board> getAllBoards() {
        return boardRepository.findAll().stream()
                .map(boardMapper::toDomain)
                .toList();
    }

    /**
     * Récupère une board par son ID
     */
    public Optional<Board> getBoardById(Long id) {
        return boardRepository.findById(id)
                .map(boardMapper::toDomain);
    }

    /**
     * Récupère une board par son nom
     */
    public Optional<Board> getBoardByName(String name) {
        return boardRepository.findByName(name)
                .map(boardMapper::toDomain);
    }

    /**
     * Récupère une board par son nom (retourne null si non trouvée)
     */
    public Board findByName(String name) {
        return boardRepository.findByName(name)
                .map(boardMapper::toDomain)
                .orElse(null);
    }

    /**
     * Crée ou met à jour une board
     * Note : Pour l'import initial, on sauvegarde UNE SEULE FOIS à la fin,
     * donc pas besoin de logique de MERGE complexe
     */
    public Board saveBoard(Board board, String boardName) {
        // Chercher si une board avec ce nom existe déjà
        Optional<BoardEntity> existingBoardOpt = boardRepository.findByName(boardName);

        BoardEntity entity;
        if (existingBoardOpt.isPresent()) {
            // Board existe → Remplacer complètement
            // Note: Pour l'import, on n'arrive jamais ici car on sauvegarde qu'une fois
            // Pour le jeu en cours, on peut mettre à jour le board entier
            entity = existingBoardOpt.get();
            BoardEntity updatedEntity = boardMapper.toEntity(board, boardName);

            // Remplacer les secteurs (DELETE + INSERT via orphanRemoval)
            entity.getSectors().clear();
            if (updatedEntity.getSectors() != null) {
                for (var sector : updatedEntity.getSectors()) {
                    sector.setBoard(entity);
                    entity.getSectors().add(sector);
                }
            }
        } else {
            // Nouvelle board → INSERT tout
            entity = boardMapper.toEntity(board, boardName);
        }

        BoardEntity savedEntity = boardRepository.save(entity);
        return boardMapper.toDomain(savedEntity);
    }

    /**
     * Supprime une board
     */
    public void deleteBoard(Long id) {
        boardRepository.deleteById(id);
    }

    /**
     * Récupère un secteur spécifique d'une board
     */
    public Optional<Sector> getSectorFromBoard(Long boardId, int sectorNumber) {
        return boardRepository.findById(boardId)
                .map(boardMapper::toDomain)
                .map(board -> board.getSector(sectorNumber));
    }

    /**
     * Assigne un propriétaire à un secteur
     */
    public boolean assignOwnerToSector(Long boardId, int sectorNumber, Integer playerId, String colorHex) {
        Optional<BoardEntity> boardEntityOpt = boardRepository.findById(boardId);
        if (boardEntityOpt.isEmpty()) {
            return false;
        }

        Board board = boardMapper.toDomain(boardEntityOpt.get());
        try {
            // Conversion Integer -> Long
            Long playerIdLong = playerId != null ? playerId.longValue() : null;
            board.assignOwner(sectorNumber, playerIdLong, colorHex);
            saveBoard(board, boardEntityOpt.get().getName());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Vérifie si deux secteurs sont voisins
     */
    public boolean areNeighbors(Long boardId, int sector1, int sector2) {
        return boardRepository.findById(boardId)
                .map(boardMapper::toDomain)
                .map(board -> board.areNeighbors(sector1, sector2))
                .orElse(false);
    }

    /**
     * Vérifie s'il y a un conflit entre deux secteurs
     */
    public boolean hasConflict(Long boardId, int sector1, int sector2) {
        return boardRepository.findById(boardId)
                .map(boardMapper::toDomain)
                .map(board -> board.hasConflict(sector1, sector2))
                .orElse(false);
    }
}

