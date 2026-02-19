CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_amount_cents BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Invoice & Tax Columns
    invoice_subtotal_cents BIGINT,
    invoice_cgst_cents BIGINT,
    invoice_sgst_cents BIGINT,
    grand_total_cents BIGINT,
    amount_in_words VARCHAR(512),

    -- Customer & Payment Info
    customer_name VARCHAR(255),
    customer_gstin VARCHAR(50),
    transaction_id VARCHAR(255),
    payment_mode VARCHAR(50),

    INDEX idx_order_user (user_id)
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    design_id BIGINT NOT NULL,
    asset_uuid VARCHAR(255),
    design_title VARCHAR(255),
    price_cents BIGINT NOT NULL,
    quantity INT NOT NULL,
    design_identifier VARCHAR(255), -- Added as requested

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE CASCADE
);