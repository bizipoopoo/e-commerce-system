package com.aurora.commerce.fulfillment;

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

import java.time.Instant;

@Entity
@Table(name = "shipments")
class Shipment {

    enum Status { IN_TRANSIT, DELIVERED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_no", nullable = false, length = 40)
    private String shipmentNo;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_no", nullable = false, length = 40)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(nullable = false, length = 80)
    private String carrier;

    @Column(name = "tracking_no", nullable = false, length = 100)
    private String trackingNo;

    @Column(name = "shipped_at", nullable = false)
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Shipment() {
    }

    Shipment(
            String shipmentNo, Long orderId, String orderNo,
            String carrier, String trackingNo, Instant shippedAt
    ) {
        this.shipmentNo = shipmentNo;
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.status = Status.IN_TRANSIT;
        this.carrier = carrier;
        this.trackingNo = trackingNo;
        this.shippedAt = shippedAt;
    }

    void deliver(Instant now) {
        if (status == Status.IN_TRANSIT) {
            status = Status.DELIVERED;
            deliveredAt = now;
        }
    }

    Long id() { return id; }
    String shipmentNo() { return shipmentNo; }
    String orderNo() { return orderNo; }
    Status status() { return status; }
    String carrier() { return carrier; }
    String trackingNo() { return trackingNo; }
    Instant shippedAt() { return shippedAt; }
    Instant deliveredAt() { return deliveredAt; }
}
