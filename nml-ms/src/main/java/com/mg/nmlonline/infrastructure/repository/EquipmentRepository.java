package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.infrastructure.entity.EquipmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentRepository extends JpaRepository<EquipmentEntity, Long> {
}
