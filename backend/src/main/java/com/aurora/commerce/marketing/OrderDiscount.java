package com.aurora.commerce.marketing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_discounts")
class OrderDiscount {

    enum Status { LOCKED, APPLIED, RELEASED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Column(name = "user_coupon_id", nullable = false)
    private Long userCouponId;

    @Column(name = "discount_type", nullable = false, length = 32)
    private String discountType;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderDiscount() {
    }

    OrderDiscount(String orderNo, Long userCouponId, BigDecimal discountAmount) {
        this.orderNo = orderNo;
        this.userCouponId = userCouponId;
        this.discountType = "COUPON";
        this.discountAmount = discountAmount;
        this.status = Status.LOCKED;
    }

    void apply() { status = Status.APPLIED; }
    void release() { if (status == Status.LOCKED) status = Status.RELEASED; }
    String orderNo() { return orderNo; }
    Long userCouponId() { return userCouponId; }
    BigDecimal discountAmount() { return discountAmount; }
    Status status() { return status; }
}
