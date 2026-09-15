-- ============================================================
-- V11.18.0: erp_sale_order_item 补 item_code / pricing_unit 列
--
-- 背景：
--   SaleOrderItem 实体含 itemCode（行号）、pricingUnit（定价单位）字段，
--   erp_sale_order_item 表缺对应列（采购侧 erp_purchase_order_item 已有），
--   MP 全字段 insert/select 会报错。
-- ============================================================

ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS item_code VARCHAR(50);
ALTER TABLE erp_sale_order_item ADD COLUMN IF NOT EXISTS pricing_unit VARCHAR(50);

COMMENT ON COLUMN erp_sale_order_item.item_code IS '行号/行编码';
COMMENT ON COLUMN erp_sale_order_item.pricing_unit IS '定价单位';
