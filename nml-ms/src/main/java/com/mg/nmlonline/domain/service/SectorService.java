package com.mg.nmlonline.domain.service;

import com.mg.nmlonline.domain.model.sector.Sector;
import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.infrastructure.repository.SectorRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SectorService {

    private final SectorRepository sectorRepository;

    @Autowired
    private EntityManager em;

    public SectorService(SectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
    }

    public List<Sector> findByOwnerId(Long ownerId) {
        return sectorRepository.findByOwnerId(ownerId);
    }

    /**
     * Retire le propriétaire des secteurs d'un joueur (lors de sa suppression).
     * {@code Sector.army} n'a plus orphanRemoval : {@code .clear()} seul ne DELETE plus
     * les rows, d'où {@code em.remove(unit)} explicite (cascade vers unit_equipments).
     */
    @Transactional
    public void removePlayerFromSectors(Long playerId) {
        List<Sector> playerSectors = sectorRepository.findByOwnerId(playerId);
        for (Sector sector : playerSectors) {
            for (Unit unit : new ArrayList<>(sector.getArmy())) {
                em.remove(unit);
            }
            sector.getArmy().clear();
            sector.setOwnerId(null);
            sector.setColor("#ffffff");
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