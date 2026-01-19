-- V1_create_designs.sql
CREATE TABLE IF NOT EXISTS designs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255),
  slug VARCHAR(255) UNIQUE,
  description TEXT,
  asset_uuid VARCHAR(128),
  base_price_cents BIGINT,
  final_price_cents BIGINT,
  category_id BIGINT,
  segment VARCHAR(50),
  active BOOLEAN DEFAULT TRUE,
  draft BOOLEAN DEFAULT TRUE,
  trending BOOLEAN DEFAULT FALSE,
  editors_pick BOOLEAN DEFAULT FALSE,
  new_arrival BOOLEAN DEFAULT FALSE,
  premium BOOLEAN DEFAULT FALSE, -- Column already here
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);