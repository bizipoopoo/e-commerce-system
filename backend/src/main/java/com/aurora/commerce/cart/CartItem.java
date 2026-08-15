package com.aurora.commerce.cart;

import com.aurora.commerce.shared.error.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Entity
@Table(name = "cart_items")
class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean selected;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartItem() {
    }

    CartItem(Long userId, Long skuId, int quantity) {
        this.userId = userId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.selected = true;
    }

    void increase(int delta, int availableQuantity) {
        setQuantity(quantity + delta, availableQuantity);
    }

    void setQuantity(int quantity, int availableQuantity) {
        if (quantity < 1 || quantity > 99) {
            throw new BusinessException(
                    "INVALID_CART_QUANTITY", "购物车数量必须在 1 到 99 之间", HttpStatus.BAD_REQUEST);
        }
        if (quantity > availableQuantity) {
            throw new BusinessException("INSUFFICIENT_STOCK", "商品库存不足", HttpStatus.CONFLICT);
        }
        this.quantity = quantity;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    Long id() {
        return id;
    }

    Long skuId() {
        return skuId;
    }

    int quantity() {
        return quantity;
    }

    boolean selected() {
        return selected;
    }
}
