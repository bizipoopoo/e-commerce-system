package com.aurora.commerce.aftersale;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "after_sales")
class AfterSale {

    enum Type { REFUND_ONLY, RETURN_REFUND }
    enum Status { PENDING_REVIEW, APPROVED, WAITING_RETURN, RETURNED, REJECTED, REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "after_sale_no", nullable = false, length = 40)
    private String afterSaleNo;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "after_sale_type", nullable = false, length = 32)
    private Type type;

    @Column(name = "source_order_status", nullable = false, length = 32)
    private String sourceOrderStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "reason_code", nullable = false, length = 40)
    private String reasonCode;

    @Column(name = "description_text", nullable = false, length = 1000)
    private String descriptionText;

    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "return_carrier", length = 80)
    private String returnCarrier;

    @Column(name = "return_tracking_no", length = 100)
    private String returnTrackingNo;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AfterSale() {
    }

    AfterSale(
            String afterSaleNo, Long orderId, String orderNo, Long userId, Type type,
            String sourceOrderStatus, String reasonCode, String descriptionText, BigDecimal refundAmount
    ) {
        this.afterSaleNo = afterSaleNo;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.userId = userId;
        this.type = type;
        this.sourceOrderStatus = sourceOrderStatus;
        this.status = Status.PENDING_REVIEW;
        this.reasonCode = reasonCode;
        this.descriptionText = descriptionText;
        this.refundAmount = refundAmount;
    }

    Status approve(String note, Instant now) {
        require(Status.PENDING_REVIEW);
        status = type == Type.REFUND_ONLY ? Status.APPROVED : Status.WAITING_RETURN;
        adminNote = note;
        reviewedAt = now;
        return status;
    }

    void reject(String note, Instant now) {
        require(Status.PENDING_REVIEW);
        status = Status.REJECTED;
        adminNote = note;
        reviewedAt = now;
    }

    void submitReturn(String carrier, String trackingNo, Instant now) {
        require(Status.WAITING_RETURN);
        status = Status.RETURNED;
        returnCarrier = carrier;
        returnTrackingNo = trackingNo;
        returnedAt = now;
    }

    void refund(Instant now) {
        if (status == Status.REFUNDED) return;
        if (status != Status.APPROVED && status != Status.RETURNED) throw stateConflict();
        status = Status.REFUNDED;
        refundedAt = now;
    }

    private void require(Status expected) {
        if (status != expected) throw stateConflict();
    }

    private BusinessException stateConflict() {
        return new BusinessException(
                "AFTER_SALE_STATE_CONFLICT", "当前售后状态不允许此操作", HttpStatus.CONFLICT);
    }

    Long id() { return id; }
    String afterSaleNo() { return afterSaleNo; }
    Long orderId() { return orderId; }
    String orderNo() { return orderNo; }
    Long userId() { return userId; }
    Type type() { return type; }
    String sourceOrderStatus() { return sourceOrderStatus; }
    Status status() { return status; }
    String reasonCode() { return reasonCode; }
    String descriptionText() { return descriptionText; }
    BigDecimal refundAmount() { return refundAmount; }
    String returnCarrier() { return returnCarrier; }
    String returnTrackingNo() { return returnTrackingNo; }
    String adminNote() { return adminNote; }
    Instant reviewedAt() { return reviewedAt; }
    Instant returnedAt() { return returnedAt; }
    Instant refundedAt() { return refundedAt; }
    Instant createdAt() { return createdAt; }
}
