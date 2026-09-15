-- ============================================================
-- V9.40.0: 销售出库单补充字段
--
-- 为 erp_sale_outbound 表添加表单页和列表页所需字段
-- ============================================================

ALTER TABLE erp_sale_outbound
    ADD COLUMN IF NOT EXISTS location            VARCHAR(200),
    ADD COLUMN IF NOT EXISTS settlement_method   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS settled_amount      DECIMAL(18,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS settlement_status   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bookkeeper_name     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS creator_name        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS auditor_name        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS print_count         INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS buyer_remark        VARCHAR(500);

-- 为常用查询字段建索引
CREATE INDEX IF NOT EXISTS idx_erp_sale_outbound_date ON erp_sale_outbound(outbound_date);
CREATE INDEX IF NOT EXISTS idx_erp_sale_outbound_customer ON erp_sale_outbound(customer_name);
CREATE INDEX IF NOT EXISTS idx_erp_sale_outbound_status ON erp_sale_outbound(status);

-- ══ 验证 ═══
DO $$
DECLARE
    col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO col_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_outbound' AND column_name = 'location';

    RAISE NOTICE '=== V9.40.0 销售出库单字段补充 ===';
    RAISE NOTICE 'location 列: %', CASE WHEN col_count > 0 THEN 'OK' ELSE 'MISSING' END;
END $$;
