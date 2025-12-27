-- V2__update_cart_items_for_designs.sql
-- Migration to update cart_items table for design-based cart

-- Step 1: Rename asset_id to design_id
ALTER TABLE cart_items CHANGE COLUMN asset_id design_id BIGINT NOT NULL;

-- Step 2: Add design_title column for caching
ALTER TABLE cart_items ADD COLUMN design_title VARCHAR(255) AFTER asset_uuid;

-- Step 3: Add indexes for performance
CREATE INDEX idx_cart_user_id ON cart_items(user_id);
CREATE INDEX idx_cart_design_id ON cart_items(design_id);
CREATE INDEX idx_cart_user_design ON cart_items(user_id, design_id, is_deleted);
