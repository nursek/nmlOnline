package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.alliance.Announcement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByVisibleAtTurnLessThanEqualOrderByIdDesc(int turn, Pageable pageable);
}
