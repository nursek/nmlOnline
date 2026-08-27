package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.api.dto.BoardDto;
import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.mapper.BoardMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service simplifié pour Board - utilise directement les classes du domaine (fusionnées avec JPA)
 */
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
     * Crée ou met à jour une board.
     * <p>
     * Uphsert NON destructif : on fusionne les secteurs par numéro. Un secteur déjà présent
     * (avec son ownerId / color / army / buildings) est conservé ; seules income, name,
     * resourceName, x/y et les neighbors sont rafraîchis depuis le JSON. Les nouveaux secteurs
     * du JSON sont ajoutés comme neutres.
     * <p>
     * ponytail: on ne fait JAMAIS getSectorsList().clear() ici — l'ancienne version supprimait
     * en cascade tous les secteurs + armées en base à chaque re-import (boot prod ou API admin),
     * ce qui réinitialisait l'appartenance des joueurs à leurs quartiers (bug prod).
     * Ceiling : retirer un secteur n'est plus possible via ce chemin (intentionnel — c'est une
     * opération destructive qui doit rester explicite, à outiller plus tard).
     */
    public Board saveBoard(Board board, String boardName) {
        // Chercher si une board avec ce nom existe déjà
        Optional<Board> existingBoardOpt = boardRepository.findByName(boardName);

        if (existingBoardOpt.isPresent()) {
            // Board existe → Mettre à jour (sans détruire l'existant)
            Board existingBoard = existingBoardOpt.get();

            // Mettre à jour les URLs de la carte
            existingBoard.setMapImageUrl(board.getMapImageUrl());
            existingBoard.setSvgOverlayUrl(board.getSvgOverlayUrl());

            // Index des secteurs existants par numéro
            Map<Integer, Sector> existingByNumber = new HashMap<>();
            for (Sector s : existingBoard.getSectorsList()) {
                existingByNumber.put(s.getNumber(), s);
            }

            // Fusionner secteur par secteur
            for (Sector incoming : board.getSectorsList()) {
                Sector existing = existingByNumber.get(incoming.getNumber());
                if (existing != null) {
                    // Rafraîchir la géométrie SANS toucher à owner_id / color / army / buildings
                    existing.setName(incoming.getName());
                    existing.setIncome(incoming.getIncome());
                    if (incoming.getResourceName() != null) {
                        existing.setResourceName(incoming.getResourceName());
                    }
                    existing.setX(incoming.getX());
                    existing.setY(incoming.getY());
                    existing.setNeighbors(new ArrayList<>(incoming.getNeighbors()));
                } else {
                    // Nouveau secteur neutre non couvert par le board.json précédent
                    incoming.setBoard(existingBoard);
                    existingBoard.getSectorsList().add(incoming);
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

    // === Mapping dans la transaction (sectorsList et sous-collections sont LAZY) ===

    public List<BoardDto> getAllBoardsDto() {
        return getAllBoards().stream().map(boardMapper::toDto).toList();
    }

    public Optional<BoardDto> getBoardByIdDto(Long id) {
        return getBoardById(id).map(boardMapper::toDto);
    }

    public Optional<BoardDto> getBoardByNameDto(String name) {
        return getBoardByName(name).map(boardMapper::toDto);
    }

    public BoardDto createBoardDto(BoardDto boardDto) {
        Board board = boardMapper.toDomain(boardDto);
        Board saved = saveBoard(board, boardDto.getName());
        return boardMapper.toDto(saved);
    }
}
