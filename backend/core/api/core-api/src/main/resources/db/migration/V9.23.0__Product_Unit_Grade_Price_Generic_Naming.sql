-- =====================================================
-- V9.23.0: 产品单位8个等级价格列改为通用命名
-- 原硬编码的等级价格列改为 grade_price_1 ~ grade_price_8
-- 等级名称由用户在 erp_product_grade 中自定义
-- 幂等设计：如果旧列已不存在则跳过重命名
-- =====================================================

-- 1. 将原有6个硬编码列重命名为通用名称（仅在旧列存在时执行）
DO $$
BEGIN
    -- restaurant_price → grade_price_1
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'erp_product_unit' AND column_name = 'restaurant_price') THEN
        ALTER TABLE erp_product_unit RENAME COLUMN restaurant_price TO grade_price_1;
    END IF;
    -- canteen_price → grade_price_2
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'erp_product_unit' AND column_name = 'canteen_price') THEN
        ALTER TABLE erp_product_unit RENAME COLUMN canteen_price TO grade_price_2;
    END IF;
    -- outer_restaurant_price → grade_price_3
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'erp_product_unit' AND column_name = 'outer_restaurant_price') THEN
        ALTER TABLE erp_product_unit RENAME COLUMN outer_restaurant_price TO grade_price_3;
    END IF;
    -- self_vip_price → grade_price_4
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'erp_product_unit' AND column_name = 'self_vip_price') THEN
        ALTER TABLE erp_product_unit RENAME COLUMN self_vip_price TO grade_price_4;
    END IF;
    -- group_meal_price → grade_price_5
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'erp_product_unit' AND column_name = 'group_meal_price') THEN
        ALTER TABLE erp_product_unit RENAME COLUMN group_meal_price TO grade_price_5;
    END IF;
    -- key_vip_price → grade_price_6
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'erp_product_unit' AND column_name = 'key_vip_price') THEN
        ALTER TABLE erp_product_unit RENAME COLUMN key_vip_price TO grade_price_6;
    END IF;
END $$;

-- 2. 添加全部8个通用等级价格列（IF NOT EXISTS 保证幂等）
ALTER TABLE erp_product_unit
    ADD COLUMN IF NOT EXISTS grade_price_1 DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS grade_price_2 DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS grade_price_3 DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS grade_price_4 DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS grade_price_5 DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS grade_price_6 DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS grade_price_7 DECIMAL(20,2),
    ADD COLUMN IF NOT EXISTS grade_price_8 DECIMAL(20,2);

-- 3. 更新列注释
COMMENT ON COLUMN erp_product_unit.grade_price_1 IS '价格等级1(默认)';
COMMENT ON COLUMN erp_product_unit.grade_price_2 IS '价格等级2(默认)';
COMMENT ON COLUMN erp_product_unit.grade_price_3 IS '价格等级3(默认)';
COMMENT ON COLUMN erp_product_unit.grade_price_4 IS '价格等级4(默认)';
COMMENT ON COLUMN erp_product_unit.grade_price_5 IS '价格等级5(默认)';
COMMENT ON COLUMN erp_product_unit.grade_price_6 IS '价格等级6(默认)';
COMMENT ON COLUMN erp_product_unit.grade_price_7 IS '价格等级7(默认)';
COMMENT ON COLUMN erp_product_unit.grade_price_8 IS '价格等级8(默认)';

-- 4. 清理旧的硬编码列（如果还存在的话）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'erp_product_unit' AND column_name = 'chain_vip_price') THEN
        ALTER TABLE erp_product_unit DROP COLUMN chain_vip_price;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'erp_product_unit' AND column_name = 'special_customer_price') THEN
        ALTER TABLE erp_product_unit DROP COLUMN special_customer_price;
    END IF;
END $$;

-- 5. 初始化默认8个价格等级（通用名称，用户可在等级管理中自定义昵称）
--    使用基于时间戳的ID（表为BIGINT主键，无序列）
INSERT INTO erp_product_grade (id, tenant_id, grade_code, grade_name, grade_level, sort_order, status, deleted)
SELECT
    (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT + sort,
    0,
    code, name, level, sort, 1, 0
FROM (VALUES
    ('GRADE_1', '价格等级1', 1, 1),
    ('GRADE_2', '价格等级2', 2, 2),
    ('GRADE_3', '价格等级3', 3, 3),
    ('GRADE_4', '价格等级4', 4, 4),
    ('GRADE_5', '价格等级5', 5, 5),
    ('GRADE_6', '价格等级6', 6, 6),
    ('GRADE_7', '价格等级7', 7, 7),
    ('GRADE_8', '价格等级8', 8, 8)
) AS t(code, name, level, sort)
WHERE NOT EXISTS (SELECT 1 FROM erp_product_grade WHERE deleted = 0 LIMIT 1);
