-- ============================================================
-- V11.21.0: erp_purchase_order 补 settled_amount / expected_receive_time
--
-- 背景：PurchaseOrder 实体含 settledAmount/expectedReceiveTime 字段，
-- 表缺列导致采购订单创建/单号查询 500（影子审批 E2E 实测暴露）。
-- ============================================================

ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS settled_amount DECIMAL(18,2) DEFAULT 0;
ALTER TABLE erp_purchase_order ADD COLUMN IF NOT EXISTS expected_receive_time TIMESTAMP;

COMMENT ON COLUMN erp_purchase_order.settled_amount IS '已结算金额';
COMMENT ON COLUMN erp_purchase_order.expected_receive_time IS '预计到货时间';
