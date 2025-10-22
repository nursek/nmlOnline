package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.infrastructure.entity.EquipmentStackEntity;
import com.mg.nmlonline.infrastructure.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentStackRepository extends JpaRepository<EquipmentStackEntity, Long> {
    List<EquipmentStackEntity> findByPlayer(PlayerEntity player);

    Optional<EquipmentStackEntity> findByPlayerAndEquipmentId(PlayerEntity player, Long equipmentId);
}

