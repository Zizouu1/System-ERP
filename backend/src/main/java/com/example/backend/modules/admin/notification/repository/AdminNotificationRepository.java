package com.example.backend.modules.admin.notification.repository;

import com.example.backend.modules.admin.notification.entity.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    List<AdminNotification> findAllByOrderByCreatedAtDesc();

    List<AdminNotification> findBySeenFalse();

    List<AdminNotification> findTop5BySeenFalseOrderByCreatedAtDesc();

    long countBySeenTrue();

    @Query("""
        SELECT COUNT(n)
        FROM AdminNotification n
        WHERE n.seen = false
        """)
    long countUnread();

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AdminNotification n
        SET n.seen = true
        WHERE n.id = :id
        """)
    int markSeenById(@Param("id") Long id);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE AdminNotification n
        SET n.seen = true
        WHERE n.seen = false
        """)
    int markAllUnseenAsSeen();

    Optional<AdminNotification> findTopByEventKeyAndSeenFalseOrderByCreatedAtDesc(String eventKey);

    List<AdminNotification> findAllByEventKeyAndSeenFalse(String eventKey);
}
