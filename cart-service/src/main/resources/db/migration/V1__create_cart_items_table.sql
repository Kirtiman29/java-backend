CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price_cents BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    is_deleted BIT NOT NULL DEFAULT 0
);
