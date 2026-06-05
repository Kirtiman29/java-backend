SET @categories_scope_column_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'categories'
          AND column_name = 'scope'
    ),
    'SELECT 1',
    'ALTER TABLE categories ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT ''BOTH'' AFTER image_url'
);
PREPARE categories_scope_column_stmt FROM @categories_scope_column_sql;
EXECUTE categories_scope_column_stmt;
DEALLOCATE PREPARE categories_scope_column_stmt;

SET @fabrics_price_per_swatch_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'fabrics'
          AND column_name = 'price_per_swatch'
    ),
    'SELECT 1',
    'ALTER TABLE fabrics ADD COLUMN price_per_swatch DOUBLE NULL AFTER price_per_meter'
);
PREPARE fabrics_price_per_swatch_stmt FROM @fabrics_price_per_swatch_sql;
EXECUTE fabrics_price_per_swatch_stmt;
DEALLOCATE PREPARE fabrics_price_per_swatch_stmt;

SET @fabrics_price_per_quarter_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'fabrics'
          AND column_name = 'price_per_quarter'
    ),
    'SELECT 1',
    'ALTER TABLE fabrics ADD COLUMN price_per_quarter DOUBLE NULL AFTER price_per_swatch'
);
PREPARE fabrics_price_per_quarter_stmt FROM @fabrics_price_per_quarter_sql;
EXECUTE fabrics_price_per_quarter_stmt;
DEALLOCATE PREPARE fabrics_price_per_quarter_stmt;

SET @fabrics_price_per_yard_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'fabrics'
          AND column_name = 'price_per_yard'
    ),
    'SELECT 1',
    'ALTER TABLE fabrics ADD COLUMN price_per_yard DOUBLE NULL AFTER price_per_quarter'
);
PREPARE fabrics_price_per_yard_stmt FROM @fabrics_price_per_yard_sql;
EXECUTE fabrics_price_per_yard_stmt;
DEALLOCATE PREPARE fabrics_price_per_yard_stmt;

SET @fabrics_stock_quantity_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'fabrics'
          AND column_name = 'stock_quantity'
    ),
    'SELECT 1',
    'ALTER TABLE fabrics ADD COLUMN stock_quantity INT NULL AFTER stock_meters'
);
PREPARE fabrics_stock_quantity_stmt FROM @fabrics_stock_quantity_sql;
EXECUTE fabrics_stock_quantity_stmt;
DEALLOCATE PREPARE fabrics_stock_quantity_stmt;

SET @fabrics_special_offer_after_column = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'fabrics'
          AND column_name = 'asset_uuid'
    ),
    'asset_uuid',
    'active'
);

SET @fabrics_special_offer_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'fabrics'
          AND column_name = 'special_offer'
    ),
    'SELECT 1',
    CONCAT(
        'ALTER TABLE fabrics ADD COLUMN special_offer BOOLEAN NOT NULL DEFAULT FALSE AFTER ',
        @fabrics_special_offer_after_column
    )
);
PREPARE fabrics_special_offer_stmt FROM @fabrics_special_offer_sql;
EXECUTE fabrics_special_offer_stmt;
DEALLOCATE PREPARE fabrics_special_offer_stmt;

SET @fabrics_discount_percent_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'fabrics'
          AND column_name = 'discount_percent'
    ),
    'SELECT 1',
    'ALTER TABLE fabrics ADD COLUMN discount_percent INT NOT NULL DEFAULT 0 AFTER special_offer'
);
PREPARE fabrics_discount_percent_stmt FROM @fabrics_discount_percent_sql;
EXECUTE fabrics_discount_percent_stmt;
DEALLOCATE PREPARE fabrics_discount_percent_stmt;
