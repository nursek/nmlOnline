package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.infrastructure.entity.SectorEntity;
import com.mg.nmlonline.infrastructure.entity.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<UnitEntity, Long> {
    List<UnitEntity> findBySector(SectorEntity sector);
}

