package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.infrastructure.entity.PlayerEntity;
import com.mg.nmlonline.infrastructure.entity.SectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<SectorEntity, Long> {
    List<SectorEntity> findByPlayer(PlayerEntity player);

    Optional<SectorEntity> findByPlayerAndNumber(PlayerEntity player, int number);
}

