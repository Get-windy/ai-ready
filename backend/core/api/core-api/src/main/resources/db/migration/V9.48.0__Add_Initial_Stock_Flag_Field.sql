-- V9.48.0: 添加期初库存标记字段
-- 为 erp_stock 表添加 is_initial 字段，用于区分期初库存数据和普通库存数据

-- 添加字段
ALTER TABLE erp_stock ADD COLUMN IF NOT EXISTS is_initial INTEGER DEFAULT 0;

-- 添加注释
COMMENT ON COLUMN erp_stock.is_initial IS '是否为期初库存：0-否，1-是';

-- 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_stock_is_initial ON erp_stock(is_initial);

-- 验证
DO $$
DECLARE
    col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO col_count
    FROM information_schema.columns
    WHERE table_name = 'erp_stock'
      AND column_name = 'is_initial';

    RAISE NOTICE '=== V9.48.0 期初库存标记字段 ===';
    RAISE NOTICE 'is_initial 字段创建: %', CASE WHEN col_count > 0 THEN 'OK' ELSE 'MISSING' END;
END $$;