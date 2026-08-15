CREATE TABLE inventories (
    sku_id BIGINT PRIMARY KEY,
    total_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    warning_quantity INT NOT NULL DEFAULT 10,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventories_sku FOREIGN KEY (sku_id) REFERENCES product_skus (id),
    CONSTRAINT ck_inventories_total CHECK (total_quantity >= 0),
    CONSTRAINT ck_inventories_reserved CHECK (reserved_quantity >= 0 AND reserved_quantity <= total_quantity)
);

CREATE TABLE inventory_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_key VARCHAR(100) NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP NULL,
    CONSTRAINT uk_inventory_reservation UNIQUE (business_key, sku_id),
    CONSTRAINT fk_inventory_reservations_sku FOREIGN KEY (sku_id) REFERENCES product_skus (id),
    CONSTRAINT ck_inventory_reservation_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_inventory_reservations_business ON inventory_reservations (business_key, status);

CREATE TABLE inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    quantity_delta INT NOT NULL,
    total_after INT NOT NULL,
    reserved_after INT NOT NULL,
    reference_no VARCHAR(100) NULL,
    reason VARCHAR(300) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_transactions_sku FOREIGN KEY (sku_id) REFERENCES product_skus (id)
);

CREATE INDEX idx_inventory_transactions_sku_time ON inventory_transactions (sku_id, created_at);

CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_user_sku UNIQUE (user_id, sku_id),
    CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_cart_items_sku FOREIGN KEY (sku_id) REFERENCES product_skus (id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0 AND quantity <= 99)
);

CREATE INDEX idx_cart_items_user_updated ON cart_items (user_id, updated_at);
