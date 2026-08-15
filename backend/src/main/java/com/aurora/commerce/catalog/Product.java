package com.aurora.commerce.catalog;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 300)
    private String subtitle;

    @Column(length = 2000)
    private String description;

    @Column(name = "cover_image_url", nullable = false, length = 500)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductStatus status;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "sales_count", nullable = false)
    private long salesCount;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    Product(
            Long categoryId,
            Long brandId,
            String name,
            String subtitle,
            String description,
            String coverImageUrl,
            boolean featured
    ) {
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.name = name;
        this.subtitle = subtitle;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.featured = featured;
        this.status = ProductStatus.DRAFT;
        this.salesCount = 0;
        this.rating = new BigDecimal("5.00");
    }

    void update(
            Long categoryId,
            Long brandId,
            String name,
            String subtitle,
            String description,
            String coverImageUrl,
            boolean featured
    ) {
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.name = name;
        this.subtitle = subtitle;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.featured = featured;
    }

    void publish() {
        status = ProductStatus.PUBLISHED;
    }

    void archive() {
        status = ProductStatus.ARCHIVED;
    }

    Long id() {
        return id;
    }

    Long categoryId() {
        return categoryId;
    }

    Long brandId() {
        return brandId;
    }

    String name() {
        return name;
    }

    String subtitle() {
        return subtitle;
    }

    String description() {
        return description;
    }

    String coverImageUrl() {
        return coverImageUrl;
    }

    ProductStatus status() {
        return status;
    }

    boolean featured() {
        return featured;
    }

    long salesCount() {
        return salesCount;
    }

    BigDecimal rating() {
        return rating;
    }

    Instant createdAt() {
        return createdAt;
    }
}
