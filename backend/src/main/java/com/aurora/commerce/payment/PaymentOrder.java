package com.aurora.commerce.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_orders")
class PaymentOrder {

    enum Status { PENDING, SUCCESS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_no", nullable = false, length = 40)
    private String paymentNo;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "provider_trade_no", length = 100)
    private String providerTradeNo;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentOrder() {
    }

    PaymentOrder(String paymentNo, Long orderId, String orderNo, String channel, BigDecimal amount) {
        this.paymentNo = paymentNo;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.status = Status.PENDING;
        this.channel = channel;
        this.amount = amount;
    }

    void succeed(String providerTradeNo, Instant paidAt) {
        if (status == Status.PENDING) {
            status = Status.SUCCESS;
            this.providerTradeNo = providerTradeNo;
            this.paidAt = paidAt;
        }
    }

    Long id() { return id; }
    String paymentNo() { return paymentNo; }
    Long orderId() { return orderId; }
    String orderNo() { return orderNo; }
    Status status() { return status; }
    String channel() { return channel; }
    BigDecimal amount() { return amount; }
    String providerTradeNo() { return providerTradeNo; }
    Instant paidAt() { return paidAt; }
    Instant createdAt() { return createdAt; }
}
