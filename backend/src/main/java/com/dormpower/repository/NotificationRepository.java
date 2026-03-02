package com.dormpower.repository;

import com.dormpower.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知仓库接口
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    Page<Notification> findByUsernameAndReadOrderByCreatedAtDesc(String username, boolean read, Pageable pageable);

    List<Notification> findByUsernameAndReadOrderByCreatedAtDesc(String username, boolean read);

    long countByUsernameAndRead(String username, boolean read);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.id = :id")
    void markAsRead(@Param("id") Long id, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.username = :username AND n.read = false")
    void markAllAsRead(@Param("username") String username, @Param("readAt") LocalDateTime readAt);

    void deleteByCreatedAtBefore(LocalDateTime date);

    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.username = :username AND n.read = false GROUP BY n.type")
    List<Object[]> countUnreadByType(@Param("username") String username);
}
