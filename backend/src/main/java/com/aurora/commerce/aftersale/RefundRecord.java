package com.aurora.commerce.aftersale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refund_records")
class RefundRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_no", nullable = false, length = 40)
    private String refundNo;

    @Column(name = "after_sale_id", nullable = false)
    private Long afterSaleId;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "refunded_at", nullable = false)
    private Instant refundedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefundRecord() {
    }

    RefundRecord(
            String refundNo, Long afterSaleId, String orderNo, BigDecimal amount, Instant refundedAt
    ) {
        this.refundNo = refundNo;
        this.afterSaleId = afterSaleId;
        this.orderNo = orderNo;
        this.amount = amount;
        this.channel = "MOCK";
        this.status = "SUCCESS";
        this.refundedAt = refundedAt;
    }

    String refundNo() { return refundNo; }
    BigDecimal amount() { return amount; }
    String status() { return status; }
    Instant refundedAt() { return refundedAt; }
}
