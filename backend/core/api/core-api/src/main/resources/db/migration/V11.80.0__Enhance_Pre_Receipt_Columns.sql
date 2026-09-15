-- ============================================================
-- V11.80.0: 预收款单增强 + 收款账户明细表
--
-- 背景：预收款单（erp_pre_receipt）对标 ql361 金标准升级：
--   1) 补结算单位编号/经手人/部门/审核人/记账人/摘要/本单赠送金额/总金额/此前预收/打印次数等列。
--   2) 新增「收款账户明细」表（erp_pre_receipt_item），实现一单多账户
--      （收款账户编号/收款账户/收款金额/备注），本单金额=Σ收款金额。
--   3) 状态机对齐：draft(草稿)→confirmed(已记账)→offset(已冲抵)/forfeited(已没收)/refunded(已退款)。
-- ============================================================

-- ── 1. 主表补列（防御性 IF NOT EXISTS，兼容既有列） ──
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS amount DECIMAL(20,2) DEFAULT 0;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS source_type VARCHAR(50);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS source_id BIGINT;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS source_no VARCHAR(100);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS source_unsettled_amount DECIMAL(20,2) DEFAULT 0;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS partner_code VARCHAR(100);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS gift_amount DECIMAL(20,2) DEFAULT 0;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS total_amount DECIMAL(20,2) DEFAULT 0;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS prev_amount DECIMAL(20,2) DEFAULT 0;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS deposit_type VARCHAR(50);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS handler_id BIGINT;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS handler_name VARCHAR(100);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS dept_name VARCHAR(100);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS creator_name VARCHAR(100);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS bookkeeper_id BIGINT;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(100);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS auditor_id BIGINT;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS auditor_name VARCHAR(100);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS auditor_time TIMESTAMP;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS print_count INT DEFAULT 0;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS payment_method INT;
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS bank_account VARCHAR(100);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS bank_name VARCHAR(100);
ALTER TABLE erp_pre_receipt ADD COLUMN IF NOT EXISTS transaction_no VARCHAR(100);

COMMENT ON COLUMN erp_pre_receipt.amount IS '本次预收金额（Σ收款账户明细）';
COMMENT ON COLUMN erp_pre_receipt.gift_amount IS '本单赠送金额：计入预收余额但不计资金账户';
COMMENT ON COLUMN erp_pre_receipt.total_amount IS '总金额 = 本次预收 + 赠送金额';
COMMENT ON COLUMN erp_pre_receipt.prev_amount IS '此前预收：记账前预收余额快照';
COMMENT ON COLUMN erp_pre_receipt.partner_code IS '结算单位编号（客户编号）';
COMMENT ON COLUMN erp_pre_receipt.summary IS '摘要';
COMMENT ON COLUMN erp_pre_receipt.handler_name IS '经手人';
COMMENT ON COLUMN erp_pre_receipt.dept_name IS '部门';
COMMENT ON COLUMN erp_pre_receipt.creator_name IS '制单人';
COMMENT ON COLUMN erp_pre_receipt.bookkeeper_name IS '记账人';
COMMENT ON COLUMN erp_pre_receipt.bookkeeping_time IS '记账时间';
COMMENT ON COLUMN erp_pre_receipt.auditor_name IS '审核人';
COMMENT ON COLUMN erp_pre_receipt.auditor_time IS '审核时间';
COMMENT ON COLUMN erp_pre_receipt.print_count IS '打印次数';

-- ── 2. 收款账户明细表 ──
CREATE TABLE IF NOT EXISTS erp_pre_receipt_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    pre_receipt_id BIGINT NOT NULL,
    line_no INT DEFAULT 1,
    account_no VARCHAR(100),
    account_name VARCHAR(200),
    amount DECIMAL(20,2) DEFAULT 0,
    remark VARCHAR(500),
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT
);
COMMENT ON TABLE erp_pre_receipt_item IS '预收款单-收款账户明细（一单多账户）';
COMMENT ON COLUMN erp_pre_receipt_item.pre_receipt_id IS '预收款单ID';
COMMENT ON COLUMN erp_pre_receipt_item.account_no IS '收款账户编号';
COMMENT ON COLUMN erp_pre_receipt_item.account_name IS '收款账户';
COMMENT ON COLUMN erp_pre_receipt_item.amount IS '收款金额';
CREATE INDEX IF NOT EXISTS idx_pre_receipt_item_master ON erp_pre_receipt_item(pre_receipt_id);
CREATE INDEX IF NOT EXISTS idx_pre_receipt_item_tenant ON erp_pre_receipt_item(tenant_id);
