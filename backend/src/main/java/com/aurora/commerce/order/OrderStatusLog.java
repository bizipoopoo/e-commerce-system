package com.aurora.commerce.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "order_status_logs")
class OrderStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private OrderStatus toStatus;

    @Column(name = "operator_type", nullable = false, length = 32)
    private String operatorType;

    @Column(length = 300)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrderStatusLog() {
    }

    OrderStatusLog(
            Long orderId, OrderStatus fromStatus, OrderStatus toStatus,
            String operatorType, String remark, Instant createdAt
    ) {
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.operatorType = operatorType;
        this.remark = remark;
        this.createdAt = createdAt;
    }

    OrderStatus fromStatus() { return fromStatus; }
    OrderStatus toStatus() { return toStatus; }
    String operatorType() { return operatorType; }
    String remark() { return remark; }
    Instant createdAt() { return createdAt; }
}
