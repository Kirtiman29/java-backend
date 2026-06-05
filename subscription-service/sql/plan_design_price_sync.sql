INSERT INTO plans (name, plan_type, billing_cycle, price, price_per_design, design_limit, credit_limit, is_active, created_at, updated_at)
SELECT 'Designs 10', 'DESIGN', 'MONTHLY', 15000.00, 1500.00, 10, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM plans
    WHERE name = 'Designs 10'
      AND plan_type = 'DESIGN'
      AND billing_cycle = 'MONTHLY'
);

INSERT INTO plans (name, plan_type, billing_cycle, price, price_per_design, design_limit, credit_limit, is_active, created_at, updated_at)
SELECT 'Designs 20', 'DESIGN', 'MONTHLY', 13000.00, 650.00, 20, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM plans
    WHERE name = 'Designs 20'
      AND plan_type = 'DESIGN'
      AND billing_cycle = 'MONTHLY'
);

INSERT INTO plans (name, plan_type, billing_cycle, price, price_per_design, design_limit, credit_limit, is_active, created_at, updated_at)
SELECT 'Designs 50', 'DESIGN', 'MONTHLY', 50000.00, 1000.00, 50, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM plans
    WHERE name = 'Designs 50'
      AND plan_type = 'DESIGN'
      AND billing_cycle = 'MONTHLY'
);

INSERT INTO plans (name, plan_type, billing_cycle, price, price_per_design, design_limit, credit_limit, is_active, created_at, updated_at)
SELECT 'Designs 100', 'DESIGN', 'MONTHLY', 80000.00, 800.00, 100, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM plans
    WHERE name = 'Designs 100'
      AND plan_type = 'DESIGN'
      AND billing_cycle = 'MONTHLY'
);

INSERT INTO plans (name, plan_type, billing_cycle, price, price_per_design, design_limit, credit_limit, is_active, created_at, updated_at)
SELECT 'Designs 100', 'DESIGN', 'YEARLY', 150000.00, 1500.00, 100, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM plans
    WHERE name = 'Designs 100'
      AND plan_type = 'DESIGN'
      AND billing_cycle = 'YEARLY'
);

INSERT INTO plans (name, plan_type, billing_cycle, price, price_per_design, design_limit, credit_limit, is_active, created_at, updated_at)
SELECT 'Designs 200', 'DESIGN', 'YEARLY', 130000.00, 650.00, 200, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM plans
    WHERE name = 'Designs 200'
      AND plan_type = 'DESIGN'
      AND billing_cycle = 'YEARLY'
);

INSERT INTO plans (name, plan_type, billing_cycle, price, price_per_design, design_limit, credit_limit, is_active, created_at, updated_at)
SELECT 'Designs 500', 'DESIGN', 'YEARLY', 500000.00, 1000.00, 500, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM plans
    WHERE name = 'Designs 500'
      AND plan_type = 'DESIGN'
      AND billing_cycle = 'YEARLY'
);

INSERT INTO plans (name, plan_type, billing_cycle, price, price_per_design, design_limit, credit_limit, is_active, created_at, updated_at)
SELECT 'Designs 1000', 'DESIGN', 'YEARLY', 800000.00, 800.00, 1000, 0, 1, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM plans
    WHERE name = 'Designs 1000'
      AND plan_type = 'DESIGN'
      AND billing_cycle = 'YEARLY'
);

UPDATE plans
SET price = 15000.00,
    price_per_design = 1500.00,
    design_limit = 10,
    credit_limit = 0,
    is_active = 1,
    updated_at = NOW()
WHERE name = 'Designs 10'
  AND plan_type = 'DESIGN'
  AND billing_cycle = 'MONTHLY';

UPDATE plans
SET price = 13000.00,
    price_per_design = 650.00,
    design_limit = 20,
    credit_limit = 0,
    is_active = 1,
    updated_at = NOW()
WHERE name = 'Designs 20'
  AND plan_type = 'DESIGN'
  AND billing_cycle = 'MONTHLY';

UPDATE plans
SET price = 50000.00,
    price_per_design = 1000.00,
    design_limit = 50,
    credit_limit = 0,
    is_active = 1,
    updated_at = NOW()
WHERE name = 'Designs 50'
  AND plan_type = 'DESIGN'
  AND billing_cycle = 'MONTHLY';

UPDATE plans
SET price = 80000.00,
    price_per_design = 800.00,
    design_limit = 100,
    credit_limit = 0,
    is_active = 1,
    updated_at = NOW()
WHERE name = 'Designs 100'
  AND plan_type = 'DESIGN'
  AND billing_cycle = 'MONTHLY';

UPDATE plans
SET price = 150000.00,
    price_per_design = 1500.00,
    design_limit = 100,
    credit_limit = 0,
    is_active = 1,
    updated_at = NOW()
WHERE name = 'Designs 100'
  AND plan_type = 'DESIGN'
  AND billing_cycle = 'YEARLY';

UPDATE plans
SET price = 130000.00,
    price_per_design = 650.00,
    design_limit = 200,
    credit_limit = 0,
    is_active = 1,
    updated_at = NOW()
WHERE name = 'Designs 200'
  AND plan_type = 'DESIGN'
  AND billing_cycle = 'YEARLY';

UPDATE plans
SET price = 500000.00,
    price_per_design = 1000.00,
    design_limit = 500,
    credit_limit = 0,
    is_active = 1,
    updated_at = NOW()
WHERE name = 'Designs 500'
  AND plan_type = 'DESIGN'
  AND billing_cycle = 'YEARLY';

UPDATE plans
SET price = 800000.00,
    price_per_design = 800.00,
    design_limit = 1000,
    credit_limit = 0,
    is_active = 1,
    updated_at = NOW()
WHERE name = 'Designs 1000'
  AND plan_type = 'DESIGN'
  AND billing_cycle = 'YEARLY';
