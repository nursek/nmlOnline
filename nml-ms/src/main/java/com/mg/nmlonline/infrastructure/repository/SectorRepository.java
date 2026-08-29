package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.sector.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Sector.SectorId> {
    List<Sector> findByOwnerId(Long ownerId);

    Optional<Sector> findByBoard_IdAndNumber(Long boardId, int number);
}
