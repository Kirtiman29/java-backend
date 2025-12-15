-- File: src/main/resources/db/migration/V2__Add_ResetToken_To_Users.sql

-- Add the column for the password reset token (VARCHAR for UUID string)
ALTER TABLE users
ADD COLUMN reset_token VARCHAR(255) NULL;

-- Add the column for the token expiration date (TIMESTAMP for Instant/Date)
ALTER TABLE users
ADD COLUMN token_expiry_date TIMESTAMP NULL;