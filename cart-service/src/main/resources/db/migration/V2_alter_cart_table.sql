ALTER TABLE cart_items
ADD CONSTRAINT unique_user_design
UNIQUE (user_id, design_id, is_deleted);