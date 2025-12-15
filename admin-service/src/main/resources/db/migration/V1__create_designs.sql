CREATE TABLE designs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255),
  slug VARCHAR(255) UNIQUE,
  description TEXT,
  asset_url VARCHAR(1024),
  price_cents BIGINT,
  status VARCHAR(50),
  designer_choice BOOLEAN DEFAULT false,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
