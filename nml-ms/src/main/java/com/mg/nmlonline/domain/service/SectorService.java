package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.entity.BoardEntity;
import com.mg.nmlonline.infrastructure.entity.SectorEntity;
import com.mg.nmlonline.infrastructure.repository.BoardRepository;
import com.mg.nmlonline.mapper.SectorMapper;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;
    private final SectorMapper sectorMapper;
    private final BoardRepository boardRepository;

    public SectorService(SectorRepository sectorRepository, SectorMapper sectorMapper, BoardRepository boardRepository) {
        this.sectorRepository = sectorRepository;
        this.sectorMapper = sectorMapper;
        this.boardRepository = boardRepository;
    }

    /**
     * Récupère tous les secteurs d'un board
     */
    public List<Sector> findByBoardId(Long boardId) {
        return sectorRepository.findByBoard_Id(boardId).stream()
                .map(sectorMapper::toDomain)
                .toList();
    }

    /**
     * Récupère tous les secteurs appartenant à un joueur
     */
    public List<Sector> findByOwnerId(Long ownerId) {
        return sectorRepository.findByOwnerId(ownerId).stream()
                .map(sectorMapper::toDomain)
                .toList();
    }

    /**
     * Récupère un secteur par board et numéro
     */
    public Optional<Sector> findByBoardIdAndNumber(Long boardId, int number) {
        return sectorRepository.findByBoard_IdAndNumber(boardId, number)
                .map(sectorMapper::toDomain);
    }

    /**
     * Sauvegarde ou met à jour un secteur
     */
    @Transactional
    public Sector save(Sector sector, Long boardId) {
        sector.recalculateMilitaryPower();
        BoardEntity board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Board with ID " + boardId + " not found"));
        SectorEntity entity = sectorMapper.toEntity(sector, board);
        SectorEntity saved = sectorRepository.save(entity);
        return sectorMapper.toDomain(saved);
    }

    /**
     * Retire le propriétaire de tous les secteurs d'un joueur (lors de suppression du joueur)
     */
    @Transactional
    public void removePlayerFromSectors(Long playerId) {
        List<SectorEntity> playerSectors = sectorRepository.findByOwnerId(playerId);
        for (SectorEntity sectorEntity : playerSectors) {
            sectorEntity.setOwnerId(null);
            sectorEntity.setColor("#ffffff");
            sectorEntity.getArmy().clear();
            sectorEntity.getStats().setTotalAtk(0.0);
            sectorEntity.getStats().setTotalPdf(0.0);
            sectorEntity.getStats().setTotalPdc(0.0);
            sectorEntity.getStats().setTotalDef(0.0);
            sectorEntity.getStats().setTotalArmor(0.0);
            sectorEntity.getStats().setTotalOffensive(0.0);
            sectorEntity.getStats().setTotalDefensive(0.0);
            sectorEntity.getStats().setGlobalStats(0.0);
            sectorRepository.save(sectorEntity);
        }
    }
}

