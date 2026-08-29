package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.resource.PlayerResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerResourceRepository extends JpaRepository<PlayerResource, Long> {
}
