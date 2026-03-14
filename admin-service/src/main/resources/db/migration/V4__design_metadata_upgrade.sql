
ALTER TABLE designs
ADD COLUMN repeat_size VARCHAR(50),
ADD COLUMN design_type VARCHAR(30),
ADD COLUMN image_format VARCHAR(30),
ADD COLUMN color_count INT,
ADD COLUMN resolution VARCHAR(50);