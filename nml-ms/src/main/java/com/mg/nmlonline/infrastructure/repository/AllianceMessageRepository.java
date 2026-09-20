package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.alliance.AllianceMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllianceMessageRepository extends JpaRepository<AllianceMessage, Long> {

    List<AllianceMessage> findByAllianceIdOrderByIdDesc(Long allianceId, Pageable pageable);
}
