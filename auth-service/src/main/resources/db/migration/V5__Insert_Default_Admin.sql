-- Ensure `admin` table exists with a sane schema. This migration is written
-- defensively so it can run on fresh DBs where admin table did not exist
-- and on existing DBs where columns may already be present.

CREATE TABLE IF NOT EXISTS admin (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	email VARCHAR(100) NOT NULL UNIQUE,
	password VARCHAR(255) NOT NULL,
	display_name VARCHAR(255),
	enabled BOOLEAN NOT NULL DEFAULT TRUE,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add OTP columns if they do not already exist (MySQL 8+ supports IF NOT EXISTS)
ALTER TABLE admin ADD COLUMN IF NOT EXISTS otp VARCHAR(6) NULL;
ALTER TABLE admin ADD COLUMN IF NOT EXISTS otp_expiry_date TIMESTAMP NULL;

-- Set a default developer email for admin id=1 when applicable
UPDATE admin SET email = 'developer@ruchitadesigncompany.com' WHERE id = 1 AND (email IS NULL OR email <> 'developer@ruchitadesigncompany.com');
