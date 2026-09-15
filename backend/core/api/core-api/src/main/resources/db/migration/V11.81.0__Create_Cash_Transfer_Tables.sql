-- ============================================================
-- V11.81.0: 提存（提存现金转账）建表脚本
--
-- 背景：提存实现「资金在企业账户间移动」（银行提现、现金存行、账户互转），
--   不涉及往来单位。对标 ql361 金标准升级：
--   1) 头表 erp_cash_transfer：转出账户 / 转出金额 / 手续费 / 经手人 / 部门 /
--      制单人 / 记账人 / 单据备注 / 摘要 / 单据状态(0草稿 1已记账 2已取消) 等。
--   2) 明细表 erp_cash_transfer_item：转入账户明细（一单多转入账户），
--      转入账户编号/转入账户名称/转入金额/备注。
--   3) 资金守恒（P1）：本单金额 = Σ转入金额 + 手续费 = 转出金额。
--   4) 记账（P0 红线）：必须经会计凭证（KJPZ-），严禁绕过凭证直改账户余额；
--      且不涉及往来单位（不产生应收/应付）。
--   5) 单号规则：YHZKD-YYYYMMDD-序号。
-- ============================================================

-- ── 1. 提存主表（头表） ──
CREATE TABLE IF NOT EXISTS erp_cash_transfer (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    doc_no VARCHAR(100) NOT NULL,                      -- 单号 YHZKD-YYYYMMDD-序号
    doc_date DATE,                                     -- 单据日期
    from_account_id BIGINT,                            -- 转出账户ID
    from_account_name VARCHAR(200),                    -- 转出账户名称
    from_account_type INT,                             -- 转出账户类型(1银行 2现金 3内部 4外部)
    from_subject_code VARCHAR(50),                     -- 转出账户科目编码(凭证用)
    from_amount DECIMAL(20,2) DEFAULT 0,               -- 转出金额 = Σ转入金额 + 手续费
    fee DECIMAL(20,2) DEFAULT 0,                       -- 手续费
    to_amount DECIMAL(20,2) DEFAULT 0,                 -- 转入金额合计 = Σ转入账户明细金额
    total_amount DECIMAL(20,2) DEFAULT 0,              -- 本单金额 = 转出金额 = Σ转入 + 手续费
    handler_id BIGINT,                                 -- 经手人
    handler_name VARCHAR(100),
    dept_id BIGINT,                                    -- 部门
    dept_name VARCHAR(100),
    status INT DEFAULT 0,                              -- 0-草稿 1-已记账 2-已取消
    creator_name VARCHAR(100),                         -- 制单人
    bookkeeper_id BIGINT,                              -- 记账人
    bookkeeper_name VARCHAR(100),
    bookkeeping_time TIMESTAMP,                        -- 记账时间
    summary VARCHAR(500),                              -- 摘要
    attachment VARCHAR(500),                           -- 附件
    remark VARCHAR(500),                               -- 单据备注
    print_count INT DEFAULT 0,                         -- 打印次数
    red_flag INT DEFAULT 0,                            -- 红冲标记
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT
);
COMMENT ON TABLE erp_cash_transfer IS '提存（提存现金转账）主表：资金在企业账户间移动';
COMMENT ON COLUMN erp_cash_transfer.doc_no IS '单号 YHZKD-YYYYMMDD-序号';
COMMENT ON COLUMN erp_cash_transfer.doc_date IS '单据日期';
COMMENT ON COLUMN erp_cash_transfer.from_account_name IS '转出账户名称';
COMMENT ON COLUMN erp_cash_transfer.from_amount IS '转出金额 = Σ转入金额 + 手续费';
COMMENT ON COLUMN erp_cash_transfer.fee IS '手续费';
COMMENT ON COLUMN erp_cash_transfer.to_amount IS '转入金额合计 = Σ转入账户明细金额';
COMMENT ON COLUMN erp_cash_transfer.total_amount IS '本单金额 = 转出金额 = Σ转入 + 手续费';
COMMENT ON COLUMN erp_cash_transfer.status IS '0-草稿 1-已记账 2-已取消';
COMMENT ON COLUMN erp_cash_transfer.creator_name IS '制单人';
COMMENT ON COLUMN erp_cash_transfer.bookkeeper_name IS '记账人';
COMMENT ON COLUMN erp_cash_transfer.bookkeeping_time IS '记账时间';
COMMENT ON COLUMN erp_cash_transfer.red_flag IS '红冲标记';

CREATE UNIQUE INDEX IF NOT EXISTS uk_cash_transfer_docno ON erp_cash_transfer(doc_no);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_tenant ON erp_cash_transfer(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_status ON erp_cash_transfer(status);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_docdate ON erp_cash_transfer(doc_date);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_fromaccount ON erp_cash_transfer(from_account_id);

-- ── 2. 提存明细表（转入账户明细） ──
CREATE TABLE IF NOT EXISTS erp_cash_transfer_item (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    transfer_id BIGINT NOT NULL,                       -- 关联 erp_cash_transfer.id
    line_no INT DEFAULT 1,                             -- 行号
    to_account_id BIGINT,                              -- 转入账户ID
    to_account_no VARCHAR(100),                        -- 转入账户编号(账号)
    to_account_name VARCHAR(200),                      -- 转入账户名称
    to_account_type INT,                               -- 转入账户类型(1银行 2现金 3内部 4外部)
    to_subject_code VARCHAR(50),                       -- 转入账户科目编码(凭证用)
    amount DECIMAL(20,2) DEFAULT 0,                    -- 转入金额
    remark VARCHAR(500),                               -- 备注
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT
);
COMMENT ON TABLE erp_cash_transfer_item IS '提存-转入账户明细（一单多转入账户）';
COMMENT ON COLUMN erp_cash_transfer_item.transfer_id IS '主表ID';
COMMENT ON COLUMN erp_cash_transfer_item.to_account_no IS '转入账户编号(账号)';
COMMENT ON COLUMN erp_cash_transfer_item.to_account_name IS '转入账户名称';
COMMENT ON COLUMN erp_cash_transfer_item.amount IS '转入金额';

CREATE INDEX IF NOT EXISTS idx_cash_transfer_item_master ON erp_cash_transfer_item(transfer_id);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_item_tenant ON erp_cash_transfer_item(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cash_transfer_item_account ON erp_cash_transfer_item(to_account_id);
