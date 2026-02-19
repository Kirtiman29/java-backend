CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    design_id BIGINT NOT NULL,
    design_identifier VARCHAR(255),
    asset_uuid VARCHAR(255) NOT NULL,
    design_title VARCHAR(255),
    quantity INT NOT NULL DEFAULT 1,
    price_cents BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BIT NOT NULL DEFAULT 0,

    INDEX idx_cart_user_id (user_id),
    INDEX idx_cart_design_id (design_id),
    INDEX idx_cart_user_design (user_id, design_id, is_deleted)
);