package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service simplifié pour Board - utilise directement les classes du domaine (fusionnées avec JPA)
 */
@Service
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    /**
     * Récupère toutes les boards
     */
    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }

    /**
     * Récupère une board par son ID
     */
    public Optional<Board> getBoardById(Long id) {
        return boardRepository.findById(id);
    }

    /**
     * Récupère une board par son nom
     */
    public Optional<Board> getBoardByName(String name) {
        return boardRepository.findByName(name);
    }

    /**
     * Récupère une board par son nom (retourne null si non trouvée)
     */
    public Board findByName(String name) {
        return boardRepository.findByName(name).orElse(null);
    }

    /**
     * Crée ou met à jour une board
     */
    public Board saveBoard(Board board, String boardName) {
        // Chercher si une board avec ce nom existe déjà
        Optional<Board> existingBoardOpt = boardRepository.findByName(boardName);

        if (existingBoardOpt.isPresent()) {
            // Board existe → Mettre à jour
            Board existingBoard = existingBoardOpt.get();

            // Mettre à jour les URLs de la carte
            existingBoard.setMapImageUrl(board.getMapImageUrl());
            existingBoard.setSvgOverlayUrl(board.getSvgOverlayUrl());

            // Remplacer les secteurs
            existingBoard.getSectorsList().clear();
            if (board.getSectorsList() != null) {
                for (Sector sector : board.getSectorsList()) {
                    sector.setBoard(existingBoard);
                    existingBoard.getSectorsList().add(sector);
                }
            }

            return boardRepository.save(existingBoard);
        } else {
            // Nouvelle board
            board.setName(boardName);
            return boardRepository.save(board);
        }
    }

    /**
     * Sauvegarde simple d'une board
     */
    public Board save(Board board) {
        return boardRepository.save(board);
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
        return getBoardById(boardId)
                .map(board -> board.getSector(sectorNumber));
    }

    /**
     * Assigne un propriétaire à un secteur
     */
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

    /**
     * Vérifie si deux secteurs sont voisins
     */
    public boolean areNeighbors(Long boardId, int sector1, int sector2) {
        return getBoardById(boardId)
                .map(board -> board.areNeighbors(sector1, sector2))
                .orElse(false);
    }

    /**
     * Vérifie s'il y a un conflit entre deux secteurs
     */
    public boolean hasConflict(Long boardId, int sector1, int sector2) {
        return getBoardById(boardId)
                .map(board -> board.hasConflict(sector1, sector2))
                .orElse(false);
    }
}
