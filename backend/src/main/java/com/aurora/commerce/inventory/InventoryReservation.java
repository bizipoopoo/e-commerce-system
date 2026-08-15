package com.aurora.commerce.inventory;

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
@Table(name = "inventory_reservations")
class InventoryReservation {

    enum Status {
        ACTIVE,
        RELEASED,
        CONFIRMED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_key", nullable = false, length = 100)
    private String businessKey;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected InventoryReservation() {
    }

    InventoryReservation(String businessKey, Long skuId, int quantity) {
        this.businessKey = businessKey;
        this.skuId = skuId;
        this.quantity = quantity;
        this.status = Status.ACTIVE;
        this.createdAt = Instant.now();
    }

    void release() {
        if (status == Status.ACTIVE) {
            status = Status.RELEASED;
            releasedAt = Instant.now();
        }
    }

    void confirm() {
        if (status == Status.ACTIVE) {
            status = Status.CONFIRMED;
            confirmedAt = Instant.now();
        }
    }

    String businessKey() {
        return businessKey;
    }

    Long skuId() {
        return skuId;
    }

    int quantity() {
        return quantity;
    }

    boolean active() {
        return status == Status.ACTIVE;
    }

    boolean confirmed() {
        return status == Status.CONFIRMED;
    }
}
