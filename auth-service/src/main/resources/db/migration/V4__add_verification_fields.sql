-- Migration V3__add_verification_fields.sql

-- 1. Add the is_verified column to the existing users table
ALTER TABLE users
ADD COLUMN is_verified BOOLEAN NOT NULL DEFAULT 0; -- 0 is false, 1 is true

-- 2. Create the verification_token table
CREATE TABLE verification_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    -- Foreign key linking the token back to the user
    CONSTRAINT FK_VERIFICATION_TOKEN_USER_ID
        FOREIGN KEY (user_id) REFERENCES users(id)
);