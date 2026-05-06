SET @coupon_code_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'coupon_code'
);
SET @coupon_code_sql = IF(
    @coupon_code_exists = 0,
    'ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(100) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @coupon_code_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @discount_amount_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'discount_amount_cents'
);
SET @discount_amount_sql = IF(
    @discount_amount_exists = 0,
    'ALTER TABLE orders ADD COLUMN discount_amount_cents BIGINT NOT NULL DEFAULT 0',
    'SELECT 1'
);
PREPARE stmt FROM @discount_amount_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @subtotal_amount_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'subtotal_amount_cents'
);
SET @subtotal_amount_sql = IF(
    @subtotal_amount_exists = 0,
    'ALTER TABLE orders ADD COLUMN subtotal_amount_cents BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @subtotal_amount_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @final_amount_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'final_amount_cents'
);
SET @final_amount_sql = IF(
    @final_amount_exists = 0,
    'ALTER TABLE orders ADD COLUMN final_amount_cents BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @final_amount_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
