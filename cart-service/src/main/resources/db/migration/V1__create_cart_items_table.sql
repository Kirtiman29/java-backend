-- File: V1__create_cart_items_table.sql
CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    design_id BIGINT NOT NULL, -- Reference to Admin Design
    asset_uuid VARCHAR(255) NOT NULL, -- Added for frontend preview
    quantity INT NOT NULL,
    price_cents BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    is_deleted BIT NOT NULL DEFAULT 0
);