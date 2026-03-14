
CREATE TABLE design_categories (
    design_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (design_id, category_id),
    FOREIGN KEY (design_id) REFERENCES designs(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);