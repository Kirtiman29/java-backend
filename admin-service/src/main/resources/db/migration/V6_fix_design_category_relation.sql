
ALTER TABLE designs
DROP FOREIGN KEY IF EXISTS FK_DESIGN_CATEGORY;

ALTER TABLE designs
DROP COLUMN IF EXISTS category_id;

ALTER TABLE designs DROP COLUMN slug;

ALTER TABLE job_applications ADD COLUMN portfolio_asset_uuid VARCHAR(128) NULL;

UPDATE designs
SET final_price_cents = FLOOR(final_price_cents / 100) * 100;