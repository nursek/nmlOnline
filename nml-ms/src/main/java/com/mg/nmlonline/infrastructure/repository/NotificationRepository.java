package com.mg.nmlonline.infrastructure.repository;

import com.mg.nmlonline.domain.model.notification.Notification;
import com.mg.nmlonline.domain.model.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop100ByUserIdAndBoardIdOrderByCreatedAtDescIdDesc(Long userId,
                                                                              Long boardId);

    long countByUserIdAndBoardIdAndReadAtIsNull(Long userId, Long boardId);

    @Modifying
    @Query("update Notification n set n.readAt = :readAt "
            + "where n.userId = :userId and n.boardId = :boardId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("boardId") Long boardId,
                    @Param("readAt") Instant readAt);

    @Modifying
    @Query("delete from Notification n where n.boardId = :boardId and n.type = :type")
    int deleteByBoardIdAndType(@Param("boardId") Long boardId,
                               @Param("type") NotificationType type);
}
