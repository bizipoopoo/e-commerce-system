CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(300) NULL,
    discount_amount DECIMAL(18,2) NOT NULL,
    threshold_amount DECIMAL(18,2) NOT NULL,
    total_limit INT NOT NULL,
    claimed_count INT NOT NULL DEFAULT 0,
    per_user_limit INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_coupons_code UNIQUE (code),
    CONSTRAINT ck_coupons_amount CHECK (discount_amount > 0 AND threshold_amount >= discount_amount),
    CONSTRAINT ck_coupons_limit CHECK (total_limit > 0 AND claimed_count >= 0 AND claimed_count <= total_limit)
);

CREATE TABLE user_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    claimed_at TIMESTAMP NOT NULL,
    locked_order_no VARCHAR(40) NULL,
    used_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_coupons_user_coupon UNIQUE (user_id, coupon_id),
    CONSTRAINT fk_user_coupons_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_coupons_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id)
);

CREATE INDEX idx_user_coupons_user_status ON user_coupons (user_id, status);
CREATE INDEX idx_user_coupons_locked_order ON user_coupons (locked_order_no);

CREATE TABLE order_discounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL,
    user_coupon_id BIGINT NOT NULL,
    discount_type VARCHAR(32) NOT NULL,
    discount_amount DECIMAL(18,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_discounts_order UNIQUE (order_no),
    CONSTRAINT fk_order_discounts_user_coupon FOREIGN KEY (user_coupon_id) REFERENCES user_coupons (id)
);

CREATE TABLE content_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(140) NOT NULL,
    title VARCHAR(180) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    cover_image_url VARCHAR(500) NOT NULL,
    content_text VARCHAR(5000) NOT NULL,
    channel_code VARCHAR(80) NOT NULL,
    status VARCHAR(32) NOT NULL,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_content_articles_slug UNIQUE (slug)
);

CREATE INDEX idx_content_articles_channel_status ON content_articles (channel_code, status, published_at);

CREATE TABLE behavior_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    anonymous_id VARCHAR(100) NULL,
    event_type VARCHAR(40) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id BIGINT NULL,
    context_json VARCHAR(2000) NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_behavior_events_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_behavior_events_user_time ON behavior_events (user_id, occurred_at);
CREATE INDEX idx_behavior_events_target ON behavior_events (target_type, target_id, occurred_at);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    content_text VARCHAR(1000) NOT NULL,
    reference_type VARCHAR(40) NULL,
    reference_no VARCHAR(100) NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_notifications_user_read_time ON notifications (user_id, read_at, created_at);
