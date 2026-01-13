package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.equipment.EquipmentStack;
import com.mg.nmlonline.domain.model.player.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentStackRepository extends JpaRepository<EquipmentStack, Long> {
    List<EquipmentStack> findByPlayer(Player player);

    Optional<EquipmentStack> findByPlayerAndEquipmentId(Player player, Long equipmentId);
}
