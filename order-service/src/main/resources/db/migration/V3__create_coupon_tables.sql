CREATE TABLE IF NOT EXISTS coupon_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE,
    discount_type VARCHAR(50) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    max_discount_amount DECIMAL(10,2) NULL,
    min_order_amount DECIMAL(10,2) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date DATETIME NULL,
    end_date DATETIME NULL,
    usage_limit INT NULL,
    used_count INT NOT NULL DEFAULT 0,
    per_user_limit INT NULL,
    audience_type VARCHAR(50) NOT NULL DEFAULT 'ALL',
    coupon_scope VARCHAR(50) NOT NULL DEFAULT 'ORDER',
    auto_apply BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coupon_usages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    discount_amount DECIMAL(10,2) NOT NULL,
    used_at DATETIME NOT NULL,
    CONSTRAINT fk_coupon_usage_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupon_codes(id)
);

CREATE INDEX idx_coupon_usages_coupon_user ON coupon_usages(coupon_id, user_id);
CREATE INDEX idx_coupon_usages_order_id ON coupon_usages(order_id);
