-- ============================================================
-- V11.86.0: 付款单金标准列补全
--
-- 业务：付款单按「资金付 → 抵 → 退」主线，页面/列配置对齐金标准。
--       主表补优惠/预付/多付/此前应付/应付余额/预付余额/付款账户1-4；
--       明细表补结算/往来/商品金额/采购金额/费用/运费/本单已结未结/本次优惠结算/源单经手人/付款期限。
-- ============================================================

-- 主表 erp_payment
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_payment.discount_amount IS '优惠金额';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS use_prepaid_amount DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_payment.use_prepaid_amount IS '使用预付款';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS overpay_amount DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_payment.overpay_amount IS '多付金额（付款超应付转供应商预付）';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS prev_payable DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_payment.prev_payable IS '此前应付';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS payable_balance DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_payment.payable_balance IS '应付余额';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS prepaid_balance DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_payment.prepaid_balance IS '预付款余额/可用预付';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS payment_account1 VARCHAR(100);
COMMENT ON COLUMN erp_payment.payment_account1 IS '付款账户1';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS payment_amount1 DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_payment.payment_amount1 IS '付款金额1';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS payment_account2 VARCHAR(100);
COMMENT ON COLUMN erp_payment.payment_account2 IS '付款账户2';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS payment_amount2 DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_payment.payment_amount2 IS '付款金额2';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS payment_account3 VARCHAR(100);
COMMENT ON COLUMN erp_payment.payment_account3 IS '付款账户3';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS payment_amount3 DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_payment.payment_amount3 IS '付款金额3';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS payment_account4 VARCHAR(100);
COMMENT ON COLUMN erp_payment.payment_account4 IS '付款账户4';
ALTER TABLE erp_payment ADD COLUMN IF NOT EXISTS payment_amount4 DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_payment.payment_amount4 IS '付款金额4';

-- 明细表 erp_payment_item
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS settle_unit_code VARCHAR(50);
COMMENT ON COLUMN erp_payment_item.settle_unit_code IS '结算单位编号';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS settle_unit VARCHAR(200);
COMMENT ON COLUMN erp_payment_item.settle_unit IS '结算单位';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS trade_unit VARCHAR(200);
COMMENT ON COLUMN erp_payment_item.trade_unit IS '往来单位';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS settlement_no VARCHAR(100);
COMMENT ON COLUMN erp_payment_item.settlement_no IS '结算单据编号';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS payment_term VARCHAR(100);
COMMENT ON COLUMN erp_payment_item.payment_term IS '付款期限';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS product_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_payment_item.product_amount IS '总商品金额';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS purchase_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_payment_item.purchase_amount IS '采购金额';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_payment_item.discount_amount IS '优惠金额';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS other_fee DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_payment_item.other_fee IS '费用/其他费用';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS freight DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_payment_item.freight IS '运费';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS bill_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_payment_item.bill_amount IS '本单金额';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS settled_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_payment_item.settled_amount IS '已结金额';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS unsettled_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_payment_item.unsettled_amount IS '未结金额';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS current_discount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_payment_item.current_discount IS '本次优惠';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS current_settle DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_payment_item.current_settle IS '本次结算';
ALTER TABLE erp_payment_item ADD COLUMN IF NOT EXISTS source_handler_name VARCHAR(100);
COMMENT ON COLUMN erp_payment_item.source_handler_name IS '源单经手人';
