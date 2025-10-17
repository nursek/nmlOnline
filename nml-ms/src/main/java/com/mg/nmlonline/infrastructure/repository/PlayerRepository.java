package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.infrastructure.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {
    @Query("SELECT p FROM PlayerEntity p WHERE p.username = :username")
    java.util.Optional<PlayerEntity> findByUsername(@Param("username") String username);
}
