package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.board.Board;
import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service simplifié pour Sector - utilise directement les classes du domaine
 */
@Service
public class SectorService {

    private final SectorRepository sectorRepository;
    private final BoardRepository boardRepository;

    public SectorService(SectorRepository sectorRepository, BoardRepository boardRepository) {
        this.sectorRepository = sectorRepository;
        this.boardRepository = boardRepository;
    }

    /**
     * Récupère tous les secteurs d'un board
     */
    public List<Sector> findByBoardId(Long boardId) {
        return sectorRepository.findByBoard_Id(boardId);
    }

    /**
     * Récupère tous les secteurs appartenant à un joueur
     */
    public List<Sector> findByOwnerId(Long ownerId) {
        return sectorRepository.findByOwnerId(ownerId);
    }

    /**
     * Récupère un secteur par board et numéro
     */
    public Optional<Sector> findByBoardIdAndNumber(Long boardId, int number) {
        return sectorRepository.findByBoard_IdAndNumber(boardId, number);
    }

    /**
     * Sauvegarde ou met à jour un secteur
     */
    @Transactional
    public Sector save(Sector sector, Long boardId) {
        sector.recalculateMilitaryPower();

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Board with ID " + boardId + " not found"));

        sector.setBoard(board);
        return sectorRepository.save(sector);
    }

    /**
     * Retire le propriétaire de tous les secteurs d'un joueur (lors de suppression du joueur)
     */
    @Transactional
    public void removePlayerFromSectors(Long playerId) {
        List<Sector> playerSectors = sectorRepository.findByOwnerId(playerId);
        for (Sector sector : playerSectors) {
            sector.setOwnerId(null);
            sector.setColor("#ffffff");
            sector.getArmy().clear();
            sector.getStats().setTotalAtk(0.0);
            sector.getStats().setTotalPdf(0.0);
            sector.getStats().setTotalPdc(0.0);
            sector.getStats().setTotalDef(0.0);
            sector.getStats().setTotalArmor(0.0);
            sector.getStats().setTotalOffensive(0.0);
            sector.getStats().setTotalDefensive(0.0);
            sector.getStats().setGlobalStats(0.0);
            sectorRepository.save(sector);
        }
    }
}
