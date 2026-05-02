SET @subscription_only_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'designs'
      AND COLUMN_NAME = 'subscription_only'
);
SET @sql = IF(
    @subscription_only_exists = 0,
    'ALTER TABLE designs ADD COLUMN subscription_only BIT NOT NULL DEFAULT b''0''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @download_tiff_uuid_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'designs'
      AND COLUMN_NAME = 'download_tiff_uuid'
);
SET @sql = IF(
    @download_tiff_uuid_exists = 0,
    'ALTER TABLE designs ADD COLUMN download_tiff_uuid VARCHAR(128)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS design_download_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    design_id BIGINT NOT NULL,
    design_identifier VARCHAR(100) NOT NULL,
    design_title VARCHAR(255) NOT NULL,
    order_id BIGINT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    INDEX idx_design_download_requests_user_id (user_id),
    INDEX idx_design_download_requests_design_id (design_id),
    INDEX idx_design_download_requests_order_id (order_id)
);
