CREATE TABLE fabrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    price_per_meter DOUBLE,
    stock_meters DOUBLE,
    material VARCHAR(100),
    width DOUBLE,
    gsm INT,
    category_id BIGINT,
    active BOOLEAN
);

CREATE TABLE fabric_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fabric_id BIGINT NOT NULL,
    asset_uuid VARCHAR(128) NOT NULL,
    asset_type VARCHAR(50),
    media_role VARCHAR(50),
    sort_order INT,

    CONSTRAINT fk_fabric FOREIGN KEY (fabric_id)
    REFERENCES fabrics(id)
    ON DELETE CASCADE
);