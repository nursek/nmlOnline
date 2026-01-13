package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.unit.Unit;
import com.mg.nmlonline.domain.model.unit.UnitEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitEquipmentRepository extends JpaRepository<UnitEquipment, Long> {
    List<UnitEquipment> findByUnit(Unit unit);

    void deleteByUnitAndEquipmentId(Unit unit, Long equipmentId);
}
