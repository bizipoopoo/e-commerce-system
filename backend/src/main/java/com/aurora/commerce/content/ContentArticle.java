package com.aurora.commerce.content;

import com.aurora.commerce.shared.error.BusinessException;
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
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Entity
@Table(name = "content_articles")
class ContentArticle {

    enum Status { DRAFT, PUBLISHED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String slug;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "cover_image_url", nullable = false, length = 500)
    private String coverImageUrl;

    @Column(name = "content_text", nullable = false, length = 5000)
    private String contentText;

    @Column(name = "channel_code", nullable = false, length = 80)
    private String channelCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "published_at")
    private Instant publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContentArticle() {
    }

    ContentArticle(
            String slug, String title, String summary, String coverImageUrl,
            String contentText, String channelCode, boolean featured
    ) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.coverImageUrl = coverImageUrl;
        this.contentText = contentText;
        this.channelCode = channelCode;
        this.featured = featured;
        this.status = Status.DRAFT;
    }

    void publish(Instant now) {
        if (status == Status.PUBLISHED) return;
        if (title.isBlank() || contentText.isBlank()) {
            throw new BusinessException("CONTENT_NOT_READY", "文章内容不完整", HttpStatus.CONFLICT);
        }
        status = Status.PUBLISHED;
        publishedAt = now;
    }

    Long id() { return id; }
    String slug() { return slug; }
    String title() { return title; }
    String summary() { return summary; }
    String coverImageUrl() { return coverImageUrl; }
    String contentText() { return contentText; }
    String channelCode() { return channelCode; }
    Status status() { return status; }
    boolean featured() { return featured; }
    Instant publishedAt() { return publishedAt; }
}
