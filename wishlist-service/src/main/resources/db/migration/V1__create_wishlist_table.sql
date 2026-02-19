CREATE TABLE wishlist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    design_id BIGINT NOT NULL,
    design_identifier VARCHAR(255),
    design_title VARCHAR(255),
    asset_uuid VARCHAR(255),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure a user can't add the same design twice
    UNIQUE KEY uk_user_design (user_id, design_id),
    INDEX idx_wishlist_user (user_id)
);