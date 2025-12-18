CREATE TABLE assets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  uuid VARCHAR(60) NOT NULL UNIQUE,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  filename VARCHAR(512) NOT NULL,
  content_type VARCHAR(128),
  size_bytes BIGINT,
  seller_id BIGINT NOT NULL,
  is_published BOOLEAN DEFAULT FALSE,
  is_deleted BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_assets_seller ON assets(seller_id);
CREATE INDEX idx_assets_uuid ON assets(uuid);