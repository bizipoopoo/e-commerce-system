ALTER TABLE customer_orders ADD COLUMN refunded_at TIMESTAMP NULL;

CREATE TABLE after_sales (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    after_sale_no VARCHAR(40) NOT NULL,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    after_sale_type VARCHAR(32) NOT NULL,
    source_order_status VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason_code VARCHAR(40) NOT NULL,
    description_text VARCHAR(1000) NOT NULL,
    refund_amount DECIMAL(18,2) NOT NULL,
    return_carrier VARCHAR(80) NULL,
    return_tracking_no VARCHAR(100) NULL,
    admin_note VARCHAR(500) NULL,
    reviewed_at TIMESTAMP NULL,
    returned_at TIMESTAMP NULL,
    refunded_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_after_sales_no UNIQUE (after_sale_no),
    CONSTRAINT uk_after_sales_order UNIQUE (order_id),
    CONSTRAINT fk_after_sales_order FOREIGN KEY (order_id) REFERENCES customer_orders (id),
    CONSTRAINT fk_after_sales_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_after_sales_amount CHECK (refund_amount > 0)
);

CREATE INDEX idx_after_sales_user_time ON after_sales (user_id, created_at);
CREATE INDEX idx_after_sales_status_time ON after_sales (status, created_at);

CREATE TABLE after_sale_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    after_sale_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    operator_type VARCHAR(32) NOT NULL,
    remark VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_after_sale_logs_after_sale FOREIGN KEY (after_sale_id) REFERENCES after_sales (id)
);

CREATE INDEX idx_after_sale_logs_case_time ON after_sale_logs (after_sale_id, created_at);

CREATE TABLE refund_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_no VARCHAR(40) NOT NULL,
    after_sale_id BIGINT NOT NULL,
    order_no VARCHAR(40) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    refunded_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refund_records_no UNIQUE (refund_no),
    CONSTRAINT uk_refund_records_after_sale UNIQUE (after_sale_id),
    CONSTRAINT fk_refund_records_after_sale FOREIGN KEY (after_sale_id) REFERENCES after_sales (id)
);

CREATE TABLE product_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_favorites_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_product_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_product_favorites_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_product_favorites_user_time ON product_favorites (user_id, created_at);

CREATE TABLE product_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content_text VARCHAR(1000) NOT NULL,
    image_urls VARCHAR(2000) NULL,
    status VARCHAR(32) NOT NULL,
    admin_reply VARCHAR(1000) NULL,
    replied_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_reviews_order_item UNIQUE (order_item_id),
    CONSTRAINT fk_product_reviews_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_product_reviews_order FOREIGN KEY (order_id) REFERENCES customer_orders (id),
    CONSTRAINT fk_product_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_product_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_product_reviews_product_status_time ON product_reviews (product_id, status, created_at);
CREATE INDEX idx_product_reviews_user_time ON product_reviews (user_id, created_at);
