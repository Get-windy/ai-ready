-- ============================================================
-- V11.80.0: 收款单金标准列补全
--
-- 业务：收款单按「资金收 → 抵 → 退」主线，页面/列配置对齐金标准。
--       主表补优惠/预收/多收/应收预收余额/收款账户1-4；
--       明细表补结算/往来/商品金额/费用运费/本单已结未结/本次优惠结算/收货信息。
-- ============================================================

-- 主表 erp_receipt
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_receipt.discount_amount IS '优惠金额';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS use_prepaid_amount DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_receipt.use_prepaid_amount IS '使用预收款';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS overpay_amount DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_receipt.overpay_amount IS '多收金额（收款超应收转客户预收）';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS prev_receivable DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_receipt.prev_receivable IS '此前应收';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS receivable_balance DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_receipt.receivable_balance IS '应收余额';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS prepaid_balance DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_receipt.prepaid_balance IS '预收余额/可用预收';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS receipt_account1 VARCHAR(100);
COMMENT ON COLUMN erp_receipt.receipt_account1 IS '收款账户1';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS receipt_amount1 DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_receipt.receipt_amount1 IS '收款金额1';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS receipt_account2 VARCHAR(100);
COMMENT ON COLUMN erp_receipt.receipt_account2 IS '收款账户2';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS receipt_amount2 DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_receipt.receipt_amount2 IS '收款金额2';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS receipt_account3 VARCHAR(100);
COMMENT ON COLUMN erp_receipt.receipt_account3 IS '收款账户3';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS receipt_amount3 DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_receipt.receipt_amount3 IS '收款金额3';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS receipt_account4 VARCHAR(100);
COMMENT ON COLUMN erp_receipt.receipt_account4 IS '收款账户4';
ALTER TABLE erp_receipt ADD COLUMN IF NOT EXISTS receipt_amount4 DECIMAL(20,2) DEFAULT 0;
COMMENT ON COLUMN erp_receipt.receipt_amount4 IS '收款金额4';

-- 明细表 erp_receipt_item
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS settle_unit_code VARCHAR(50);
COMMENT ON COLUMN erp_receipt_item.settle_unit_code IS '结算单位编号';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS settle_unit VARCHAR(200);
COMMENT ON COLUMN erp_receipt_item.settle_unit IS '结算单位';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS trade_unit VARCHAR(200);
COMMENT ON COLUMN erp_receipt_item.trade_unit IS '往来单位';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS settlement_no VARCHAR(100);
COMMENT ON COLUMN erp_receipt_item.settlement_no IS '结算单据编号';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS product_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_receipt_item.product_amount IS '商品金额';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_receipt_item.discount_amount IS '优惠金额';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS other_fee DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_receipt_item.other_fee IS '费用/其他费用';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS freight DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_receipt_item.freight IS '运费';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS bill_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_receipt_item.bill_amount IS '本单金额';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS settled_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_receipt_item.settled_amount IS '已结金额';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS unsettled_amount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_receipt_item.unsettled_amount IS '未结金额';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS current_discount DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_receipt_item.current_discount IS '本次优惠';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS current_settle DECIMAL(18,4) DEFAULT 0;
COMMENT ON COLUMN erp_receipt_item.current_settle IS '本次结算';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS source_handler_name VARCHAR(100);
COMMENT ON COLUMN erp_receipt_item.source_handler_name IS '源单经手人';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS receiver_name VARCHAR(100);
COMMENT ON COLUMN erp_receipt_item.receiver_name IS '收货人';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS receiver_phone VARCHAR(30);
COMMENT ON COLUMN erp_receipt_item.receiver_phone IS '联系电话';
ALTER TABLE erp_receipt_item ADD COLUMN IF NOT EXISTS receiver_address VARCHAR(500);
COMMENT ON COLUMN erp_receipt_item.receiver_address IS '收货地址';
