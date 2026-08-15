package com.aurora.commerce.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notifications")
class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "notification_type", nullable = false, length = 40)
    private String notificationType;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "content_text", nullable = false, length = 1000)
    private String contentText;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(name = "read_at")
    private Instant readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    Notification(
            Long userId, String notificationType, String title, String contentText,
            String referenceType, String referenceNo
    ) {
        this.userId = userId;
        this.notificationType = notificationType;
        this.title = title;
        this.contentText = contentText;
        this.referenceType = referenceType;
        this.referenceNo = referenceNo;
    }

    void markRead(Instant now) { if (readAt == null) readAt = now; }
    Long id() { return id; }
    Long userId() { return userId; }
    String notificationType() { return notificationType; }
    String title() { return title; }
    String contentText() { return contentText; }
    String referenceType() { return referenceType; }
    String referenceNo() { return referenceNo; }
    Instant readAt() { return readAt; }
    Instant createdAt() { return createdAt; }
}
