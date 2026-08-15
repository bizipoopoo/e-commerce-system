ALTER TABLE inventory_reservations ADD COLUMN confirmed_at TIMESTAMP NULL;

CREATE TABLE customer_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL,
    user_id BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    goods_amount DECIMAL(18,2) NOT NULL,
    discount_amount DECIMAL(18,2) NOT NULL,
    shipping_amount DECIMAL(18,2) NOT NULL,
    payable_amount DECIMAL(18,2) NOT NULL,
    receiver_name VARCHAR(80) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    address_line VARCHAR(300) NOT NULL,
    customer_note VARCHAR(300) NULL,
    expire_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    shipped_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_customer_orders_no UNIQUE (order_no),
    CONSTRAINT uk_customer_orders_idempotency UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_customer_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_customer_orders_user_created ON customer_orders (user_id, created_at);
CREATE INDEX idx_customer_orders_status_expire ON customer_orders (status, expire_at);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(180) NOT NULL,
    sku_name VARCHAR(180) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    unit_price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    discount_amount DECIMAL(18,2) NOT NULL,
    payable_amount DECIMAL(18,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES customer_orders (id),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE order_status_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    operator_type VARCHAR(32) NOT NULL,
    remark VARCHAR(300) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_status_logs_order FOREIGN KEY (order_id) REFERENCES customer_orders (id)
);

CREATE INDEX idx_order_status_logs_order_time ON order_status_logs (order_id, created_at);

CREATE TABLE payment_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_no VARCHAR(40) NOT NULL,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    provider_trade_no VARCHAR(100) NULL,
    paid_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_orders_no UNIQUE (payment_no),
    CONSTRAINT uk_payment_orders_order UNIQUE (order_id),
    CONSTRAINT fk_payment_orders_order FOREIGN KEY (order_id) REFERENCES customer_orders (id)
);

CREATE TABLE payment_callbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    provider_event_id VARCHAR(100) NOT NULL,
    payload VARCHAR(2000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_callbacks_event UNIQUE (provider_event_id),
    CONSTRAINT fk_payment_callbacks_payment FOREIGN KEY (payment_id) REFERENCES payment_orders (id)
);

CREATE TABLE shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_no VARCHAR(40) NOT NULL,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    carrier VARCHAR(80) NOT NULL,
    tracking_no VARCHAR(100) NOT NULL,
    shipped_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_shipments_no UNIQUE (shipment_no),
    CONSTRAINT uk_shipments_order UNIQUE (order_id),
    CONSTRAINT uk_shipments_tracking UNIQUE (tracking_no),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES customer_orders (id)
);

CREATE TABLE shipment_tracks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    description VARCHAR(300) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_shipment_tracks_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id)
);

CREATE INDEX idx_shipment_tracks_shipment_time ON shipment_tracks (shipment_id, occurred_at);
