SET @designs_slug_column_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'designs'
          AND column_name = 'slug'
    ),
    'SELECT 1',
    'ALTER TABLE designs ADD COLUMN slug VARCHAR(255) NULL'
);
PREPARE designs_slug_column_stmt FROM @designs_slug_column_sql;
EXECUTE designs_slug_column_stmt;
DEALLOCATE PREPARE designs_slug_column_stmt;

SET @fabrics_slug_column_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'fabrics'
          AND column_name = 'slug'
    ),
    'SELECT 1',
    'ALTER TABLE fabrics ADD COLUMN slug VARCHAR(255) NULL'
);
PREPARE fabrics_slug_column_stmt FROM @fabrics_slug_column_sql;
EXECUTE fabrics_slug_column_stmt;
DEALLOCATE PREPARE fabrics_slug_column_stmt;

SET @categories_slug_column_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'categories'
          AND column_name = 'slug'
    ),
    'SELECT 1',
    'ALTER TABLE categories ADD COLUMN slug VARCHAR(255) NULL'
);
PREPARE categories_slug_column_stmt FROM @categories_slug_column_sql;
EXECUTE categories_slug_column_stmt;
DEALLOCATE PREPARE categories_slug_column_stmt;

ALTER TABLE blog_posts
    MODIFY COLUMN slug VARCHAR(255) NOT NULL;

SET @designs_slug_index_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'designs'
          AND index_name = 'uq_designs_slug'
    ),
    'SELECT 1',
    'CREATE UNIQUE INDEX uq_designs_slug ON designs (slug)'
);
PREPARE designs_slug_index_stmt FROM @designs_slug_index_sql;
EXECUTE designs_slug_index_stmt;
DEALLOCATE PREPARE designs_slug_index_stmt;

SET @fabrics_slug_index_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'fabrics'
          AND index_name = 'uq_fabrics_slug'
    ),
    'SELECT 1',
    'CREATE UNIQUE INDEX uq_fabrics_slug ON fabrics (slug)'
);
PREPARE fabrics_slug_index_stmt FROM @fabrics_slug_index_sql;
EXECUTE fabrics_slug_index_stmt;
DEALLOCATE PREPARE fabrics_slug_index_stmt;

SET @categories_slug_index_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'categories'
          AND index_name = 'uq_categories_slug'
    ),
    'SELECT 1',
    'CREATE UNIQUE INDEX uq_categories_slug ON categories (slug)'
);
PREPARE categories_slug_index_stmt FROM @categories_slug_index_sql;
EXECUTE categories_slug_index_stmt;
DEALLOCATE PREPARE categories_slug_index_stmt;
