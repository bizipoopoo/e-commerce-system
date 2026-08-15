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
import jakarta.persistence.Version;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Entity
@Table(name = "user_coupons")
class UserCoupon {

    enum Status { AVAILABLE, LOCKED, USED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    @Column(name = "locked_order_no", length = 40)
    private String lockedOrderNo;

    @Column(name = "used_at")
    private Instant usedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected UserCoupon() {
    }

    UserCoupon(Long userId, Long couponId, Instant claimedAt) {
        this.userId = userId;
        this.couponId = couponId;
        this.status = Status.AVAILABLE;
        this.claimedAt = claimedAt;
    }

    void lock(String orderNo) {
        if (status == Status.LOCKED && orderNo.equals(lockedOrderNo)) return;
        if (status != Status.AVAILABLE) throw unavailable();
        status = Status.LOCKED;
        lockedOrderNo = orderNo;
    }

    void release(String orderNo) {
        if (status == Status.LOCKED && orderNo.equals(lockedOrderNo)) {
            status = Status.AVAILABLE;
            lockedOrderNo = null;
        }
    }

    void use(String orderNo, Instant now) {
        if (status == Status.USED) return;
        if (status != Status.LOCKED || !orderNo.equals(lockedOrderNo)) throw unavailable();
        status = Status.USED;
        usedAt = now;
    }

    private BusinessException unavailable() {
        return new BusinessException("COUPON_NOT_AVAILABLE", "优惠券当前不可用", HttpStatus.CONFLICT);
    }

    Long id() { return id; }
    Long userId() { return userId; }
    Long couponId() { return couponId; }
    Status status() { return status; }
    Instant claimedAt() { return claimedAt; }
    String lockedOrderNo() { return lockedOrderNo; }
    Instant usedAt() { return usedAt; }
}
