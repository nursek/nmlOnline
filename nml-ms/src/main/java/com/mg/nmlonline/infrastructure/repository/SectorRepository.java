package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.sector.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Sector.SectorId> {
    /**
     * Trouve tous les secteurs appartenant à un joueur
     */
    List<Sector> findByOwnerId(Long ownerId);

    /**
     * Trouve un secteur par son propriétaire et son numéro
     */
    Optional<Sector> findByOwnerIdAndNumber(Long ownerId, int number);

    /**
     * Trouve un secteur par son board et son numéro
     */
    Optional<Sector> findByBoard_IdAndNumber(Long boardId, int number);

    /**
     * Trouve tous les secteurs d'un board
     */
    List<Sector> findByBoard_Id(Long boardId);
}
