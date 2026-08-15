package com.aurora.commerce.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "inventory_transactions")
class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "transaction_type", nullable = false, length = 32)
    private String transactionType;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "total_after", nullable = false)
    private int totalAfter;

    @Column(name = "reserved_after", nullable = false)
    private int reservedAfter;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(length = 300)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InventoryTransaction() {
    }

    InventoryTransaction(
            Long skuId,
            String transactionType,
            int quantityDelta,
            int totalAfter,
            int reservedAfter,
            String referenceNo,
            String reason
    ) {
        this.skuId = skuId;
        this.transactionType = transactionType;
        this.quantityDelta = quantityDelta;
        this.totalAfter = totalAfter;
        this.reservedAfter = reservedAfter;
        this.referenceNo = referenceNo;
        this.reason = reason;
        this.createdAt = Instant.now();
    }
}
