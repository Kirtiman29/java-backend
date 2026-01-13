CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    design_id BIGINT NOT NULL, -- Matches designId in Entity
    asset_uuid VARCHAR(255),
    design_title VARCHAR(255),
    price_cents BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
        ON DELETE CASCADE
);