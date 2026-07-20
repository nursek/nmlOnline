package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service simplifié pour Sector - utilise directement les classes du domaine
 */
@Service
public class SectorService {

    private final SectorRepository sectorRepository;

    public SectorService(SectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
    }

    /**
     * Récupère tous les secteurs appartenant à un joueur
     */
    public List<Sector> findByOwnerId(Long ownerId) {
        return sectorRepository.findByOwnerId(ownerId);
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
