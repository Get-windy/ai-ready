-- ============================================================
-- V11.33.0: 支付表补 remark + 报价转订单序列
-- ============================================================

-- 支付表补 remark 列
ALTER TABLE md_payment_method ADD COLUMN IF NOT EXISTS remark VARCHAR(500);
ALTER TABLE md_payment_channel ADD COLUMN IF NOT EXISTS remark VARCHAR(500);

-- 报价转订单用的序列
CREATE SEQUENCE IF NOT EXISTS seq_sale_order_no START 1 INCREMENT 1;
