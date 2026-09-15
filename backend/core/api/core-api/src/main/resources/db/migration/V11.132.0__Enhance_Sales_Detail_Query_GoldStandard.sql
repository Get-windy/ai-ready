-- ============================================================================
-- V11.132.0 销售明细查询页金标准升级（2026-09-11）
--
-- 背景：销售明细查询页（sales/detail-query）96 列中有两列为硬编码 null（来源订单日期、
--      默认经手人），4 个查询项（单据类型 / 所属行业类别 / 来源 / 默认经手人）后端未消费。
--
-- 处理：
--   1) 「默认经手人」溯源到客户主数据（biz_party）——往来单位表单早已有该录入项，
--      但 V9.14.0 列重构时被删除，导致录入值静默丢弃。此处按业务目标态补回（幂等）。
--   2) 「来源」（PC/MOBILE/API/IMPORT）为销售出库单自身属性，erp_sale_outbound 补 source 列。
--
-- 全部 IF NOT EXISTS，幂等；存量数据回填为 'PC'（历史单据均由电脑端录入），对既有行为零影响。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. biz_party（往来单位/客户主数据）补「默认经手人」
-- ------------------------------------------------------------
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS default_handler_id   BIGINT;
ALTER TABLE biz_party ADD COLUMN IF NOT EXISTS default_handler_name VARCHAR(100);

-- ------------------------------------------------------------
-- 2. erp_sale_outbound（销售出库单头）补「来源」
-- ------------------------------------------------------------
ALTER TABLE erp_sale_outbound ADD COLUMN IF NOT EXISTS source VARCHAR(20);
UPDATE erp_sale_outbound SET source = 'PC' WHERE source IS NULL OR source = '';
ALTER TABLE erp_sale_outbound ALTER COLUMN source SET DEFAULT 'PC';

-- ------------------------------------------------------------
-- 3. 验证
-- ------------------------------------------------------------
DO $$
DECLARE
    v_party_cols INTEGER;
    v_outbound_cols INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_party_cols
    FROM information_schema.columns
    WHERE table_name = 'biz_party'
      AND column_name IN ('default_handler_id', 'default_handler_name');

    SELECT COUNT(*) INTO v_outbound_cols
    FROM information_schema.columns
    WHERE table_name = 'erp_sale_outbound' AND column_name = 'source';

    RAISE NOTICE 'sales detail query gold standard: biz_party new cols=%, erp_sale_outbound.source=%',
        v_party_cols, v_outbound_cols;
END $$;
