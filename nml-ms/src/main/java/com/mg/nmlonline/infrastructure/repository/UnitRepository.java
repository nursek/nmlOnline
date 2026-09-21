package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.unit.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByPlayerIdAndSectorIsNull(Long playerId);

    // Projection : ne charge pas les entités avant les verrous joueurs (sinon snapshot périmé).
    @Query("select distinct u.playerId from Unit u where u.sector is null and u.playerId is not null")
    List<Long> findPlayerIdsWithReserveUnits();
}
