package com.aurora.commerce.aftersale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "after_sale_logs")
class AfterSaleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "after_sale_id", nullable = false)
    private Long afterSaleId;

    @Column(name = "from_status", length = 32)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 32)
    private String toStatus;

    @Column(name = "operator_type", nullable = false, length = 32)
    private String operatorType;

    @Column(nullable = false, length = 500)
    private String remark;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AfterSaleLog() {
    }

    AfterSaleLog(Long afterSaleId, String fromStatus, String toStatus, String operatorType, String remark) {
        this.afterSaleId = afterSaleId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.operatorType = operatorType;
        this.remark = remark;
    }

    String fromStatus() { return fromStatus; }
    String toStatus() { return toStatus; }
    String operatorType() { return operatorType; }
    String remark() { return remark; }
    Instant createdAt() { return createdAt; }
}
