package com.aurora.commerce.recommendation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "behavior_events")
class BehaviorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "anonymous_id", length = 100)
    private String anonymousId;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "context_json", length = 2000)
    private String contextJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected BehaviorEvent() {
    }

    BehaviorEvent(
            Long userId, String anonymousId, String eventType, String targetType,
            Long targetId, String contextJson, Instant occurredAt
    ) {
        this.userId = userId;
        this.anonymousId = anonymousId;
        this.eventType = eventType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.contextJson = contextJson;
        this.occurredAt = occurredAt;
    }

    String eventType() { return eventType; }
    String targetType() { return targetType; }
    Long targetId() { return targetId; }
}
