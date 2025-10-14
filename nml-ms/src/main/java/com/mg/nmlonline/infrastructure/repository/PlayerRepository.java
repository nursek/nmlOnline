package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.infrastructure.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {
}
