-- Ensure `admin` table exists with a sane schema and rename legacy username if present.
-- This script is written defensively for both fresh databases and existing installations.

CREATE TABLE IF NOT EXISTS admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) DEFAULT NULL,
    display_name VARCHAR(255) DEFAULT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- If the legacy username column exists, rename it to email.
SELECT COUNT(*) INTO @username_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'admin'
  AND column_name = 'username';

SET @alter_sql = IF(@username_exists > 0,
    'ALTER TABLE admin CHANGE COLUMN username email VARCHAR(255) NOT NULL UNIQUE',
    'SELECT 1');

PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add missing columns if not already present.
ALTER TABLE admin ADD COLUMN IF NOT EXISTS otp VARCHAR(255) DEFAULT NULL;
ALTER TABLE admin ADD COLUMN IF NOT EXISTS otp_expiry_date TIMESTAMP NULL;

-- Ensure admin id=1 has the expected developer email.
UPDATE admin
SET email = 'developer@ruchitadesigncompany.com'
WHERE id = 1
  AND (email IS NULL OR email <> 'developer@ruchitadesigncompany.com');
