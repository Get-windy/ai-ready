-- =====================================================
-- V9.29.0: 等级价格从产品单位表剥离到独立价格表 (Odoo 架构)
-- 1. erp_product_grade_price 增加 unit_id 列，支持按单位定价
-- 2. 删除 erp_product_unit 表的 grade_price_1~8 列
-- =====================================================

-- 1. 等级价格表增加 unit_id（支持多单位各自有等级价格）
ALTER TABLE erp_product_grade_price
    ADD COLUMN IF NOT EXISTS unit_id BIGINT;

COMMENT ON COLUMN erp_product_grade_price.unit_id IS '产品单位ID，NULL表示产品级别价格';

CREATE INDEX IF NOT EXISTS idx_product_grade_price_unit ON erp_product_grade_price(unit_id);

-- 2. 删除产品单位表中的等级价格列（已迁移到 erp_product_grade_price）
ALTER TABLE erp_product_unit
    DROP COLUMN IF EXISTS grade_price_1,
    DROP COLUMN IF EXISTS grade_price_2,
    DROP COLUMN IF EXISTS grade_price_3,
    DROP COLUMN IF EXISTS grade_price_4,
    DROP COLUMN IF EXISTS grade_price_5,
    DROP COLUMN IF EXISTS grade_price_6,
    DROP COLUMN IF EXISTS grade_price_7,
    DROP COLUMN IF EXISTS grade_price_8;
