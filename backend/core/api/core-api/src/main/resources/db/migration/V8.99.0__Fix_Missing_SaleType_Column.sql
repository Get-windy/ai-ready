-- 修复 erp_sale_order 表缺失字段
-- 执行时间: 2026-07-12

-- 添加 sale_type 字段（如果不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'erp_sale_order' AND column_name = 'sale_type'
    ) THEN
        ALTER TABLE erp_sale_order ADD COLUMN sale_type INTEGER DEFAULT 1;
        COMMENT ON COLUMN erp_sale_order.sale_type IS '销售类型：1-正常销售 2-样品销售 3-促销销售';
    END IF;
END $$;
