package com.aomanagement.service;

import com.aomanagement.dto.TenderDtos;
import com.aomanagement.entity.Notification;
import com.aomanagement.entity.NotificationType;
import com.aomanagement.entity.Tender;
import com.aomanagement.entity.User;
import com.aomanagement.exception.ForbiddenOperationException;
import com.aomanagement.exception.NotFoundException;
import com.aomanagement.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MapperService mapperService;

    @Transactional
    public void create(User recipient, Tender tender, NotificationType type, String title, String message) {
        if (recipient == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTender(tender);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public boolean existsSince(Long recipientId, Long tenderId, NotificationType type, String title, LocalDateTime since) {
        return notificationRepository.existsByRecipientIdAndTenderIdAndTypeAndTitleAndCreatedAtAfter(recipientId, tenderId, type, title, since);
    }

    @Transactional(readOnly = true)
    public Page<TenderDtos.NotificationResponse> list(Long recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(mapperService::toNotificationResponse);
    }

    @Transactional(readOnly = true)
    public TenderDtos.UnreadCountResponse unreadCount(Long recipientId) {
        return new TenderDtos.UnreadCountResponse(notificationRepository.countByRecipientIdAndReadFalse(recipientId));
    }

    @Transactional
    public TenderDtos.NotificationResponse markRead(Long id, Long recipientId) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new NotFoundException("Notification introuvable"));
        if (!notification.getRecipient().getId().equals(recipientId)) {
            throw new ForbiddenOperationException("Accès interdit à cette notification");
        }
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        return mapperService.toNotificationResponse(notification);
    }

    @Transactional
    public void markAllRead(Long recipientId) {
        notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, Pageable.unpaged()).forEach(notification -> {
            if (!notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(LocalDateTime.now());
            }
        });
    }
}
