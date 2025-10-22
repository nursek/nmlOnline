package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.infrastructure.entity.UnitEntity;
import com.mg.nmlonline.infrastructure.entity.UnitEquipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitEquipmentRepository extends JpaRepository<UnitEquipmentEntity, Long> {
    List<UnitEquipmentEntity> findByUnit(UnitEntity unit);

    void deleteByUnitAndEquipmentId(UnitEntity unit, Long equipmentId);
}

