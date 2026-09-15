-- ============================================================
-- V9.41.0: 销售出库明细补充字段
--
-- 为 erp_sale_outbound_item 表添加表单页所需字段
-- ============================================================

ALTER TABLE erp_sale_outbound_item
    ADD COLUMN IF NOT EXISTS piece_quantity       DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS conversion_relation  VARCHAR(200),
    ADD COLUMN IF NOT EXISTS discount_rate        DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discounted_amount    DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discounted_price     DECIMAL(18,4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS shelf_life           VARCHAR(100);

-- ══ 验证 ═══
DO $$
DECLARE
    col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO col_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_outbound_item' AND column_name = 'piece_quantity';

    RAISE NOTICE '=== V9.41.0 销售出库明细字段补充 ===';
    RAISE NOTICE 'piece_quantity 列: %', CASE WHEN col_count > 0 THEN 'OK' ELSE 'MISSING' END;
END $$;
