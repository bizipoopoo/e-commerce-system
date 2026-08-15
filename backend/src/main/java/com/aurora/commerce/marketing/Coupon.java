package com.aurora.commerce.marketing;

import com.aurora.commerce.shared.error.BusinessException;
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
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupons")
class Coupon {

    enum Status { ACTIVE, INACTIVE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 300)
    private String description;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "threshold_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "total_limit", nullable = false)
    private int totalLimit;

    @Column(name = "claimed_count", nullable = false)
    private int claimedCount;

    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Coupon() {
    }

    Coupon(
            String code, String name, String description, BigDecimal discountAmount,
            BigDecimal thresholdAmount, int totalLimit, Instant startsAt, Instant endsAt
    ) {
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0
                || thresholdAmount.compareTo(discountAmount) < 0 || totalLimit <= 0
                || !endsAt.isAfter(startsAt)) {
            throw new BusinessException("INVALID_COUPON_RULE", "优惠券规则无效", HttpStatus.BAD_REQUEST);
        }
        this.code = code;
        this.name = name;
        this.description = description;
        this.discountAmount = discountAmount;
        this.thresholdAmount = thresholdAmount;
        this.totalLimit = totalLimit;
        this.claimedCount = 0;
        this.perUserLimit = 1;
        this.status = Status.ACTIVE;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    boolean claimableAt(Instant now) {
        return activeAt(now) && claimedCount < totalLimit;
    }

    boolean activeAt(Instant now) {
        return status == Status.ACTIVE && !now.isBefore(startsAt) && now.isBefore(endsAt);
    }

    BigDecimal calculate(BigDecimal goodsAmount) {
        if (goodsAmount.compareTo(thresholdAmount) < 0) {
            throw new BusinessException("COUPON_THRESHOLD_NOT_MET", "订单金额未达到优惠券门槛", HttpStatus.CONFLICT);
        }
        return discountAmount.min(goodsAmount);
    }

    Long id() { return id; }
    String code() { return code; }
    String name() { return name; }
    String description() { return description; }
    BigDecimal discountAmount() { return discountAmount; }
    BigDecimal thresholdAmount() { return thresholdAmount; }
    int totalLimit() { return totalLimit; }
    int claimedCount() { return claimedCount; }
    Status status() { return status; }
    Instant startsAt() { return startsAt; }
    Instant endsAt() { return endsAt; }
}
