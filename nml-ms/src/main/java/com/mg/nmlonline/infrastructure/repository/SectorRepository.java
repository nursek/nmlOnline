package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.infrastructure.entity.SectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<SectorEntity, Long> {
    /**
     * Trouve tous les secteurs appartenant à un joueur
     * @param ownerId ID du joueur propriétaire
     * @return Liste des secteurs du joueur
     */
    List<SectorEntity> findByOwnerId(Long ownerId);

    /**
     * Trouve un secteur par son propriétaire et son numéro
     * @param ownerId ID du joueur propriétaire
     * @param number Numéro du secteur
     * @return Le secteur si trouvé
     */
    Optional<SectorEntity> findByOwnerIdAndNumber(Long ownerId, int number);

    /**
     * Trouve un secteur par son board et son numéro
     * @param boardId ID du board
     * @param number Numéro du secteur
     * @return Le secteur si trouvé
     */
    Optional<SectorEntity> findByBoard_IdAndNumber(Long boardId, int number);

    /**
     * Trouve tous les secteurs d'un board
     * @param boardId ID du board
     * @return Liste des secteurs du board
     */
    List<SectorEntity> findByBoard_Id(Long boardId);
}

