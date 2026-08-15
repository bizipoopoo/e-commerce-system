package com.aurora.commerce.inventory;

import com.aurora.commerce.shared.error.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Entity
@Table(name = "inventories")
class Inventory {

    @Id
    @Column(name = "sku_id")
    private Long skuId;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "warning_quantity", nullable = false)
    private int warningQuantity;

    @Version
    @Column(nullable = false)
    private long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() {
    }

    Inventory(Long skuId, int totalQuantity, int warningQuantity) {
        this.skuId = skuId;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = 0;
        this.warningQuantity = warningQuantity;
    }

    void setQuantities(int totalQuantity, int warningQuantity) {
        if (totalQuantity < reservedQuantity) {
            throw new BusinessException(
                    "INVENTORY_BELOW_RESERVED",
                    "总库存不能低于已预占库存",
                    HttpStatus.CONFLICT
            );
        }
        if (warningQuantity < 0) {
            throw new BusinessException("INVALID_WARNING_QUANTITY", "预警库存不能小于 0", HttpStatus.BAD_REQUEST);
        }
        this.totalQuantity = totalQuantity;
        this.warningQuantity = warningQuantity;
    }

    Long skuId() {
        return skuId;
    }

    int totalQuantity() {
        return totalQuantity;
    }

    int reservedQuantity() {
        return reservedQuantity;
    }

    int availableQuantity() {
        return totalQuantity - reservedQuantity;
    }

    int warningQuantity() {
        return warningQuantity;
    }

    boolean warning() {
        return availableQuantity() <= warningQuantity;
    }
}
