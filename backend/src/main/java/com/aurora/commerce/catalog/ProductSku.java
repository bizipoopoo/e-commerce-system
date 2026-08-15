package com.aurora.commerce.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "product_skus")
class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_code", nullable = false, unique = true, length = 80)
    private String skuCode;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "spec_values", nullable = false, length = 1000)
    private String specValues;

    @Column(name = "sale_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "market_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal marketPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SkuStatus status;

    protected ProductSku() {
    }

    ProductSku(
            Long productId,
            String skuCode,
            String name,
            String specValues,
            BigDecimal salePrice,
            BigDecimal marketPrice
    ) {
        this.productId = productId;
        this.skuCode = skuCode;
        this.name = name;
        this.specValues = specValues;
        this.salePrice = salePrice;
        this.marketPrice = marketPrice;
        this.status = SkuStatus.ACTIVE;
    }

    Long id() {
        return id;
    }

    Long productId() {
        return productId;
    }

    String skuCode() {
        return skuCode;
    }

    String name() {
        return name;
    }

    String specValues() {
        return specValues;
    }

    BigDecimal salePrice() {
        return salePrice;
    }

    BigDecimal marketPrice() {
        return marketPrice;
    }
}
