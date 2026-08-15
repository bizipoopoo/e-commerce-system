package com.aurora.commerce.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "product_name", nullable = false, length = 180)
    private String productName;

    @Column(name = "sku_name", nullable = false, length = 180)
    private String skuName;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "payable_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal payableAmount;

    protected OrderItem() {
    }

    OrderItem(
            Long orderId, Long productId, Long skuId, String productName, String skuName,
            String imageUrl, BigDecimal unitPrice, int quantity,
            BigDecimal discountAmount, BigDecimal payableAmount
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.skuId = skuId;
        this.productName = productName;
        this.skuName = skuName;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.discountAmount = discountAmount;
        this.payableAmount = payableAmount;
    }

    Long id() { return id; }
    Long productId() { return productId; }
    Long skuId() { return skuId; }
    String productName() { return productName; }
    String skuName() { return skuName; }
    String imageUrl() { return imageUrl; }
    BigDecimal unitPrice() { return unitPrice; }
    int quantity() { return quantity; }
    BigDecimal discountAmount() { return discountAmount; }
    BigDecimal payableAmount() { return payableAmount; }
}
