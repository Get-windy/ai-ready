-- ============================================================
-- V11.70.0: 盘点明细补「货位转移差异」目标货位
--
-- 背景：
--   库存货位错位（账面在货位A、实盘发现应在货位B，数量一致）无法用报损/报溢表达。
--   为支持「盘点点差异→移库单」自动下推，明细补 to_location_id/to_location_code：
--   非空即标记为「货位转移差异」，盘点处理时生成移库单（sourceType=盘点差异，sourceNo=盘点单号）。
-- ============================================================

ALTER TABLE erp_stock_take_item ADD COLUMN IF NOT EXISTS to_location_id BIGINT;
ALTER TABLE erp_stock_take_item ADD COLUMN IF NOT EXISTS to_location_code VARCHAR(100);
COMMENT ON COLUMN erp_stock_take_item.to_location_id IS '目标货位ID（货位转移差异：账面在 location，应移至 to_location）';
COMMENT ON COLUMN erp_stock_take_item.to_location_code IS '目标货位编码';
