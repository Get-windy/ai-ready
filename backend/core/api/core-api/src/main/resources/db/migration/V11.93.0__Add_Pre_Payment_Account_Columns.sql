-- ============================================================
-- V11.93.0: 预付款单补付款账户相关列
--
-- 背景：PrePayment 实体含 paymentMethod/bankAccount/bankName/transactionNo，
--   但 erp_pre_payment 历史建表未提供这4列，导致 MyBatisPlus 生成 SELECT 时
--   引用缺失列报 "字段 payment_method 不存在" → next-no/page 500。
--   （V11.92.0 只补了业务/审计列，未包含付款账户列。）
-- ============================================================

ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS payment_method INT;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS bank_account VARCHAR(100);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS bank_name VARCHAR(100);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS transaction_no VARCHAR(100);

COMMENT ON COLUMN erp_pre_payment.payment_method IS '支付方式';
COMMENT ON COLUMN erp_pre_payment.bank_account IS '付款账户编号';
COMMENT ON COLUMN erp_pre_payment.bank_name IS '付款账户';
COMMENT ON COLUMN erp_pre_payment.transaction_no IS '交易流水号';
