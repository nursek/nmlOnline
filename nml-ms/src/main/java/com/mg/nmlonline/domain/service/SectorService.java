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

/**
 * Service simplifié pour Sector - utilise directement les classes du domaine
 */
@Service
public class SectorService {

    private final SectorRepository sectorRepository;

    @Autowired
    private EntityManager em;

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
     * Retire le propriétaire de tous les secteurs d'un joueur (lors de suppression du joueur).
     *
     * <p>Phase 3 : {@code Sector.army} ne porte plus {@code orphanRemoval} (voir
     * {@code docs/jpa-pitfalls.md} §1), donc {@code .clear()} seul ne DELETE plus
     * les rows {@code combat_entities}. On lève {@code em.remove(unit)} pour chaque
     * unité (cascade REMOVE vers {@code unit_equipments} via {@code Unit.unitEquipments}
     * cascade=ALL), puis on vide la collection en mémoire.</p>
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