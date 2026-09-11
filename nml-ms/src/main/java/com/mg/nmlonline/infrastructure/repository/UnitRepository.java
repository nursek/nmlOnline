package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.unit.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
}
