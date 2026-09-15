-- ============================================================
-- V11.12.0: erp_stock 补齐 unit_price 列
--
-- 背景：
--   Stock 实体含 unitPrice 字段，但 erp_stock 表缺 unit_price 列，
--   导致任何全列查询（/api/erp/stock/by-product、库存回写）500。
--   该缺陷在采购入库回写库存链路上实测暴露。
-- ============================================================

ALTER TABLE erp_stock ADD COLUMN IF NOT EXISTS unit_price DECIMAL(18,4);

COMMENT ON COLUMN erp_stock.unit_price IS '库存单价（成本价）';

-- erp_purchase_inbound 补齐实体已有但表缺失的流程/物流/扩展列
-- （表为旧版结构，PurchaseInbound 实体后扩展了收货/质检/审批/入库确认/版本等字段）
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(100);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS logistics_company VARCHAR(100);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS expected_arrival_time TIMESTAMP;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS actual_arrival_time TIMESTAMP;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS received_by BIGINT;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS received_time TIMESTAMP;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS quality_checked_by BIGINT;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS quality_checked_time TIMESTAMP;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS quality_check_result VARCHAR(500);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS approved_by BIGINT;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS approved_time TIMESTAMP;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS approved_note VARCHAR(500);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS warehouse_confirmed_by BIGINT;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS warehouse_confirmed_time TIMESTAMP;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS completed_by BIGINT;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS completed_time TIMESTAMP;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS internal_note VARCHAR(1000);
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS ext_info TEXT;
ALTER TABLE erp_purchase_inbound ADD COLUMN IF NOT EXISTS version_no INTEGER DEFAULT 0;
