package com.aurora.commerce.notification;

import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationFacade {

    private final NotificationRepository notificationRepository;
    private final NotificationStreamHub streamHub;

    NotificationFacade(NotificationRepository notificationRepository, NotificationStreamHub streamHub) {
        this.notificationRepository = notificationRepository;
        this.streamHub = streamHub;
    }

    @Transactional
    public NotificationView notifyUser(
            Long userId, String type, String title, String content,
            String referenceType, String referenceNo
    ) {
        Notification notification = notificationRepository.save(new Notification(
                userId, type, title, content, referenceType, referenceNo));
        NotificationView view = toView(notification);
        publishAfterCommit(userId, view);
        return view;
    }

    @Transactional(readOnly = true)
    public NotificationCenter center(Long userId) {
        return new NotificationCenter(
                notificationRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(this::toView).toList(),
                notificationRepository.countByUserIdAndReadAtIsNull(userId)
        );
    }

    @Transactional
    public NotificationView markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(
                        "NOTIFICATION_NOT_FOUND", "消息不存在", HttpStatus.NOT_FOUND));
        notification.markRead(Instant.now());
        return toView(notification);
    }

    public SseEmitter subscribe(Long userId) {
        return streamHub.subscribe(userId);
    }

    private void publishAfterCommit(Long userId, NotificationView view) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            streamHub.publish(userId, view);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                streamHub.publish(userId, view);
            }
        });
    }

    private NotificationView toView(Notification notification) {
        return new NotificationView(
                notification.id(), notification.notificationType(), notification.title(),
                notification.contentText(), notification.referenceType(), notification.referenceNo(),
                notification.readAt(), notification.createdAt());
    }

    public record NotificationCenter(List<NotificationView> items, long unreadCount) {
    }

    public record NotificationView(
            Long id, String type, String title, String content,
            String referenceType, String referenceNo, Instant readAt, Instant createdAt
    ) {
    }
}
