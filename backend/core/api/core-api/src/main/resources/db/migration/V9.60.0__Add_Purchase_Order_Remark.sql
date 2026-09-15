-- V9.60.0 添加采购订单备注字段
-- 采购订单列表页39列中的"单据备注"列需要此字段
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS remark VARCHAR(500);

COMMENT ON COLUMN erp_purchase_order.remark IS '单据备注';
