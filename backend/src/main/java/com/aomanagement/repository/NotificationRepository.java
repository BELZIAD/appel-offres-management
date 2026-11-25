package com.aomanagement.repository;

import com.aomanagement.entity.Notification;
import com.aomanagement.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    long countByRecipientIdAndReadFalse(Long recipientId);
    boolean existsByRecipientIdAndTenderIdAndTypeAndTitleAndCreatedAtAfter(Long recipientId, Long tenderId, NotificationType type, String title, LocalDateTime createdAt);
}
