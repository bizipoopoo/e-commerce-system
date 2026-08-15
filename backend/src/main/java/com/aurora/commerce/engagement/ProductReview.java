package com.aurora.commerce.engagement;

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

import java.time.Instant;

@Entity
@Table(name = "product_reviews")
class ProductReview {

    enum Status { PUBLISHED, HIDDEN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int rating;

    @Column(name = "content_text", nullable = false, length = 1000)
    private String contentText;

    @Column(name = "image_urls", length = 2000)
    private String imageUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "admin_reply", length = 1000)
    private String adminReply;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductReview() {
    }

    ProductReview(
            Long userId, Long orderId, Long orderItemId, Long productId,
            int rating, String contentText, String imageUrls
    ) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException("INVALID_REVIEW_RATING", "评分必须在 1 到 5 之间", HttpStatus.BAD_REQUEST);
        }
        this.userId = userId;
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.rating = rating;
        this.contentText = contentText;
        this.imageUrls = imageUrls;
        this.status = Status.PUBLISHED;
    }

    void reply(String reply, Instant now) {
        adminReply = reply;
        repliedAt = now;
    }

    void hide() {
        if (status == Status.HIDDEN) return;
        status = Status.HIDDEN;
    }

    Long id() { return id; }
    Long userId() { return userId; }
    Long orderId() { return orderId; }
    Long orderItemId() { return orderItemId; }
    Long productId() { return productId; }
    int rating() { return rating; }
    String contentText() { return contentText; }
    String imageUrls() { return imageUrls; }
    Status status() { return status; }
    String adminReply() { return adminReply; }
    Instant repliedAt() { return repliedAt; }
    Instant createdAt() { return createdAt; }
}
