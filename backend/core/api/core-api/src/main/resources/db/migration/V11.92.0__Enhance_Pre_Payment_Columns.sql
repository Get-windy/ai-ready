-- ============================================================
-- V11.86.0: 预付款单增强 + 付款账户明细表
--
-- 背景：预付款单（erp_pre_payment）对标 ql361 金标准升级：
--   1) 补供应商编号/经手人/部门/审核人/记账人/摘要/此前预付/源单未结金额/打印次数/附件等列。
--   2) 新增「付款账户明细」表（erp_pre_payment_item），实现一单多账户
--      （付款账户编号/付款账户/付款金额/备注），本单金额=Σ付款金额。
--   3) 状态机对齐：draft(草稿)→confirmed(已记账)→offset(已冲抵)/recovered(已收回)/refunded(已退款)。
--   4) status 一致改为 VARCHAR（历史建表若误为 INTEGER 会与 Java String 状态机冲突→500）。
-- ============================================================

-- ── 0. 状态列类型统一为 VARCHAR ──
ALTER TABLE erp_pre_payment ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

-- ── 1. 主表补列（防御性 IF NOT EXISTS，兼容既有列） ──
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS amount DECIMAL(20,2) DEFAULT 0;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS source_type VARCHAR(50);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS source_id BIGINT;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS source_no VARCHAR(100);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS source_unsettled_amount DECIMAL(20,2) DEFAULT 0;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS partner_code VARCHAR(100);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS prev_amount DECIMAL(20,2) DEFAULT 0;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS deposit_type VARCHAR(50);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS handler_id BIGINT;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS handler_name VARCHAR(100);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS dept_id BIGINT;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS dept_name VARCHAR(100);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS creator_name VARCHAR(100);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS bookkeeper_id BIGINT;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(100);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS auditor_id BIGINT;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS auditor_name VARCHAR(100);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS auditor_time TIMESTAMP;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS print_count INT DEFAULT 0;
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS attachment VARCHAR(1000);
ALTER TABLE erp_pre_payment ADD COLUMN IF NOT EXISTS version_no INT;

COMMENT ON COLUMN erp_pre_payment.amount IS '本次预付金额（Σ付款账户明细）';
COMMENT ON COLUMN erp_pre_payment.prev_amount IS '此前预付：记账前预付余额快照';
COMMENT ON COLUMN erp_pre_payment.partner_code IS '供应商编号（结算单位编号）';
COMMENT ON COLUMN erp_pre_payment.source_unsettled_amount IS '源单未结金额';
COMMENT ON COLUMN erp_pre_payment.summary IS '摘要';
COMMENT ON COLUMN erp_pre_payment.handler_name IS '经手人';
COMMENT ON COLUMN erp_pre_payment.dept_name IS '部门';
COMMENT ON COLUMN erp_pre_payment.creator_name IS '制单人';
COMMENT ON COLUMN erp_pre_payment.bookkeeper_name IS '记账人';
COMMENT ON COLUMN erp_pre_payment.bookkeeping_time IS '记账时间';
COMMENT ON COLUMN erp_pre_payment.auditor_name IS '审核人';
COMMENT ON COLUMN erp_pre_payment.auditor_time IS '审核时间';
COMMENT ON COLUMN erp_pre_payment.print_count IS '打印次数';
COMMENT ON COLUMN erp_pre_payment.attachment IS '附件';
COMMENT ON COLUMN erp_pre_payment.version_no IS '版本号(乐观锁)';

-- ── 2. 付款账户明细表 ──
CREATE TABLE IF NOT EXISTS erp_pre_payment_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    pre_payment_id BIGINT NOT NULL,
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
COMMENT ON TABLE erp_pre_payment_item IS '预付款单-付款账户明细（一单多账户）';
COMMENT ON COLUMN erp_pre_payment_item.pre_payment_id IS '预付款单ID';
COMMENT ON COLUMN erp_pre_payment_item.account_no IS '付款账户编号';
COMMENT ON COLUMN erp_pre_payment_item.account_name IS '付款账户';
COMMENT ON COLUMN erp_pre_payment_item.amount IS '付款金额';
CREATE INDEX IF NOT EXISTS idx_pre_payment_item_master ON erp_pre_payment_item(pre_payment_id);
CREATE INDEX IF NOT EXISTS idx_pre_payment_item_tenant ON erp_pre_payment_item(tenant_id);
