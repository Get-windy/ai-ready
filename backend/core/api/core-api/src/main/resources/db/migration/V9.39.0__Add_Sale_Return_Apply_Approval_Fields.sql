-- ============================================================
-- V9.39.0: 销售退货申请表添加审批字段
--
-- 为 erp_sale_return 表添加审批相关字段
-- ============================================================

ALTER TABLE erp_sale_return
    ADD COLUMN IF NOT EXISTS approved_by     BIGINT,
    ADD COLUMN IF NOT EXISTS approved_time   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS approved_note   VARCHAR(500);

-- ══ 验证 ═══
DO $$
DECLARE
    col_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO col_count FROM information_schema.columns
    WHERE table_name = 'erp_sale_return' AND column_name = 'approved_by';

    RAISE NOTICE '=== V9.39.0 销售退货申请表审批字段 ===';
    RAISE NOTICE 'approved_by 列: %', CASE WHEN col_count > 0 THEN 'OK' ELSE 'MISSING' END;
END $$;
