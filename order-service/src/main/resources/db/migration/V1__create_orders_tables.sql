-- ==============================
-- ORDERS TABLE
-- ==============================

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- ==============================
    -- CUSTOMER DETAILS
    -- ==============================

    customer_name VARCHAR(255),
    customer_email VARCHAR(255),
    customer_phone VARCHAR(20),

    organization_name VARCHAR(255),

    customer_gstin VARCHAR(50),

    -- ==============================
    -- BILLING ADDRESS
    -- ==============================

    billing_address_line1 VARCHAR(255),
    billing_address_line2 VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(100),
    billing_pincode VARCHAR(20),
    billing_country VARCHAR(100) DEFAULT 'India',

    -- ==============================
    -- INVOICE TYPE
    -- ==============================

    invoice_type VARCHAR(20),

    -- ==============================
    -- TAX CALCULATIONS
    -- ==============================

    invoice_subtotal_cents BIGINT,
    invoice_cgst_cents BIGINT,
    invoice_sgst_cents BIGINT,
    invoice_igst_cents BIGINT,

    grand_total_cents BIGINT,

    amount_in_words VARCHAR(512),

    -- ==============================
    -- PAYMENT INFO
    -- ==============================

    transaction_id VARCHAR(255),
    payment_mode VARCHAR(50),

    -- ==============================
    -- INDEX
    -- ==============================

    INDEX idx_order_user (user_id)

);


-- ==============================
-- ORDER ITEMS TABLE
-- ==============================

CREATE TABLE order_items (

    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    order_id BIGINT NOT NULL,

    design_id BIGINT NOT NULL,

    design_identifier VARCHAR(255),

    asset_uuid VARCHAR(255),

    design_title VARCHAR(255),

    price_cents BIGINT NOT NULL,

    quantity INT NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE

);


-- ==============================
-- INDEXES
-- ==============================

CREATE INDEX idx_order_items_design
ON order_items(design_id);