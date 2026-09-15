-- ============================================================
-- V10.4.0 补齐零售单表遗漏字段
-- 修复: 实体中持久化字段对应的列在之前的迁移中未创建
--       导致 MyBatis Plus SELECT 报 "column does not exist" 500 错误
-- ============================================================

-- ═══════════════════════════════════════════════════════════════
-- 1. 零售单主表 erp_retail_order 遗漏字段（2个）
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS prepaid_balance NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.prepaid_balance IS '预收余额';

ALTER TABLE erp_retail_order ADD COLUMN IF NOT EXISTS prev_points INTEGER DEFAULT 0;
COMMENT ON COLUMN erp_retail_order.prev_points IS '此前积分';

-- ═══════════════════════════════════════════════════════════════
-- 2. 零售单明细表 erp_retail_order_item 遗漏字段（8个）
-- ═══════════════════════════════════════════════════════════════

-- 包装单位
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS big_pack NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.big_pack IS '大包装';

ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS mid_pack NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.mid_pack IS '中包装';

ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS small_pack NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.small_pack IS '小包装';

-- 可用库存（原始值，区别于已有的 available_stock_converted 换算结果）
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS available_stock NUMERIC(18,2) DEFAULT 0;
COMMENT ON COLUMN erp_retail_order_item.available_stock IS '可用库存';

-- 批次编码（区别于已有的 batch_no 批次条码）
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS batch_code VARCHAR(100);
COMMENT ON COLUMN erp_retail_order_item.batch_code IS '批次编码';

-- 生产日期/保质期/到期日期
ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS production_date DATE;
COMMENT ON COLUMN erp_retail_order_item.production_date IS '生产日期';

ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS shelf_life INTEGER;
COMMENT ON COLUMN erp_retail_order_item.shelf_life IS '保质期(天)';

ALTER TABLE erp_retail_order_item ADD COLUMN IF NOT EXISTS expiry_date DATE;
COMMENT ON COLUMN erp_retail_order_item.expiry_date IS '到期日期';
