-- V3__add_premium_column.sql
-- Add premium flag to designs table

ALTER TABLE designs ADD COLUMN premium BOOLEAN DEFAULT FALSE;

-- Update index for section flags queries (optional but recommended)
CREATE INDEX idx_designs_premium ON designs(premium);