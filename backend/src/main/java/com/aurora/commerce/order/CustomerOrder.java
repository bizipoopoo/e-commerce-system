package com.aurora.commerce.order;

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
@Table(name = "customer_orders")
class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "goods_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal goodsAmount;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "shipping_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingAmount;

    @Column(name = "payable_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal payableAmount;

    @Column(name = "receiver_name", nullable = false, length = 80)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 30)
    private String receiverPhone;

    @Column(name = "address_line", nullable = false, length = 300)
    private String addressLine;

    @Column(name = "customer_note", length = 300)
    private String customerNote;

    @Column(name = "expire_at", nullable = false)
    private Instant expireAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerOrder() {
    }

    CustomerOrder(
            String orderNo,
            Long userId,
            String idempotencyKey,
            BigDecimal goodsAmount,
            BigDecimal discountAmount,
            BigDecimal shippingAmount,
            BigDecimal payableAmount,
            String receiverName,
            String receiverPhone,
            String addressLine,
            String customerNote,
            Instant expireAt
    ) {
        this.orderNo = orderNo;
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.goodsAmount = goodsAmount;
        this.discountAmount = discountAmount;
        this.shippingAmount = shippingAmount;
        this.payableAmount = payableAmount;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.addressLine = addressLine;
        this.customerNote = customerNote;
        this.expireAt = expireAt;
    }

    void markPaid(Instant now) {
        requireStatus(OrderStatus.PENDING_PAYMENT);
        status = OrderStatus.PAID;
        paidAt = now;
    }

    void close(Instant now) {
        requireStatus(OrderStatus.PENDING_PAYMENT);
        status = OrderStatus.CLOSED;
        closedAt = now;
    }

    void ship(Instant now) {
        requireStatus(OrderStatus.PAID);
        status = OrderStatus.SHIPPED;
        shippedAt = now;
    }

    void complete(Instant now) {
        requireStatus(OrderStatus.SHIPPED);
        status = OrderStatus.COMPLETED;
        completedAt = now;
    }

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw new BusinessException(
                    "ORDER_STATE_CONFLICT", "当前订单状态不允许此操作", HttpStatus.CONFLICT);
        }
    }

    Long id() { return id; }
    String orderNo() { return orderNo; }
    Long userId() { return userId; }
    OrderStatus status() { return status; }
    BigDecimal goodsAmount() { return goodsAmount; }
    BigDecimal discountAmount() { return discountAmount; }
    BigDecimal shippingAmount() { return shippingAmount; }
    BigDecimal payableAmount() { return payableAmount; }
    String receiverName() { return receiverName; }
    String receiverPhone() { return receiverPhone; }
    String addressLine() { return addressLine; }
    String customerNote() { return customerNote; }
    Instant expireAt() { return expireAt; }
    Instant paidAt() { return paidAt; }
    Instant shippedAt() { return shippedAt; }
    Instant completedAt() { return completedAt; }
    Instant closedAt() { return closedAt; }
    Instant createdAt() { return createdAt; }
}
