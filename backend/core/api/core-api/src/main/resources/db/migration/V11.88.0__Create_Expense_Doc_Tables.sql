-- ============================================================
-- V11.88.0: 费用单（非主营支出费用登记）建表脚本
--
-- 背景：费用单用于「非主营支出的费用登记」（往来单位费用/内部费用两类），
--   对标 ql361 金标准升级，与其他收入单对称。单号前缀 YBFYD-。
--   1) 主表 erp_expense_doc：费用类型(往来单位费用/内部费用) / 往来单位 /
--      经手人 / 部门 / 单据日期 / 付款账户1-4 / 摘要 / 单据备注 / 本单金额 /
--      单据状态(0草稿 1已记账 2已取消) 等。
--   2) 明细表 erp_expense_item：费用项明细（费用编号/费用名称/费用科目/金额/备注），
--      费用项映射费用类会计科目。
--   3) 记账（P0 红线）：必须经会计凭证（KJPZ-），严禁直接改费用/往来余额；
--      费用项明细必须有映射科目。
--   4) 单号规则：YBFYD-YYYYMMDD-序号。
-- ============================================================

-- ── 1. 费用单主表（头表） ──
CREATE TABLE IF NOT EXISTS erp_expense_doc (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    doc_no VARCHAR(100) NOT NULL,                      -- 单号 YBFYD-YYYYMMDD-序号
    doc_date DATE,                                     -- 单据日期
    expense_type INT DEFAULT 0,                        -- 费用类型 0-往来单位费用 1-内部费用
    partner_id BIGINT,                                 -- 往来单位ID（往来单位费用）
    partner_code VARCHAR(100),                         -- 往来编号
    partner_name VARCHAR(200),                         -- 往来单位名称
    handler_id BIGINT,                                 -- 经手人
    handler_name VARCHAR(100),
    dept_id BIGINT,                                    -- 部门（内部费用）
    dept_name VARCHAR(100),
    pay_account_id BIGINT,                             -- 付款账户1
    pay_account_name VARCHAR(200),
    pay_subject_code VARCHAR(50),                      -- 付款账户1科目编码(凭证用)
    pay_amount DECIMAL(20,2) DEFAULT 0,                -- 付款金额1
    pay_account2_id BIGINT,                            -- 付款账户2
    pay_account2_name VARCHAR(200),
    pay_subject_code2 VARCHAR(50),
    pay_amount2 DECIMAL(20,2) DEFAULT 0,
    pay_account3_id BIGINT,                            -- 付款账户3
    pay_account3_name VARCHAR(200),
    pay_subject_code3 VARCHAR(50),
    pay_amount3 DECIMAL(20,2) DEFAULT 0,
    pay_account4_id BIGINT,                            -- 付款账户4
    pay_account4_name VARCHAR(200),
    pay_subject_code4 VARCHAR(50),
    pay_amount4 DECIMAL(20,2) DEFAULT 0,
    summary VARCHAR(500),                              -- 摘要
    remark VARCHAR(500),                               -- 单据备注
    total_amount DECIMAL(20,2) DEFAULT 0,              -- 本单金额 = Σ费用项金额 = Σ付款金额
    status INT DEFAULT 0,                              -- 0-草稿 1-已记账 2-已取消
    creator_name VARCHAR(100),                         -- 制单人
    bookkeeper_id BIGINT,                              -- 记账人
    bookkeeper_name VARCHAR(100),
    bookkeeping_time TIMESTAMP,                        -- 记账时间
    print_count INT DEFAULT 0,                         -- 打印次数
    red_flag INT DEFAULT 0,                            -- 红冲标记
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT
);
COMMENT ON TABLE erp_expense_doc IS '费用单主表：非主营支出费用登记（往来单位费用/内部费用）';
COMMENT ON COLUMN erp_expense_doc.doc_no IS '单号 YBFYD-YYYYMMDD-序号';
COMMENT ON COLUMN erp_expense_doc.expense_type IS '费用类型 0-往来单位费用 1-内部费用';
COMMENT ON COLUMN erp_expense_doc.partner_name IS '往来单位名称';
COMMENT ON COLUMN erp_expense_doc.pay_amount IS '付款金额1';
COMMENT ON COLUMN erp_expense_doc.total_amount IS '本单金额 = Σ费用项金额 = Σ付款金额';
COMMENT ON COLUMN erp_expense_doc.status IS '0-草稿 1-已记账 2-已取消';
COMMENT ON COLUMN erp_expense_doc.creator_name IS '制单人';
COMMENT ON COLUMN erp_expense_doc.bookkeeper_name IS '记账人';
COMMENT ON COLUMN erp_expense_doc.bookkeeping_time IS '记账时间';
COMMENT ON COLUMN erp_expense_doc.red_flag IS '红冲标记';

CREATE UNIQUE INDEX IF NOT EXISTS uk_expense_doc_docno ON erp_expense_doc(doc_no);
CREATE INDEX IF NOT EXISTS idx_expense_doc_tenant ON erp_expense_doc(tenant_id);
CREATE INDEX IF NOT EXISTS idx_expense_doc_status ON erp_expense_doc(status);
CREATE INDEX IF NOT EXISTS idx_expense_doc_docdate ON erp_expense_doc(doc_date);
CREATE INDEX IF NOT EXISTS idx_expense_doc_partner ON erp_expense_doc(partner_id);

-- ── 2. 费用项明细表 ──
CREATE TABLE IF NOT EXISTS erp_expense_item (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    expense_doc_id BIGINT NOT NULL,                    -- 关联 erp_expense_doc.id
    line_no INT DEFAULT 1,                             -- 行号
    expense_code VARCHAR(100),                         -- 费用编号
    expense_name VARCHAR(200),                         -- 费用名称
    subject_code VARCHAR(50),                          -- 费用科目编码(凭证用)
    subject_name VARCHAR(200),                         -- 费用科目名称
    amount DECIMAL(20,2) DEFAULT 0,                    -- 金额
    remark VARCHAR(500),                               -- 明细备注
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT
);
COMMENT ON TABLE erp_expense_item IS '费用项明细（费用编号/费用名称/费用科目/金额/备注）';
COMMENT ON COLUMN erp_expense_item.expense_doc_id IS '主表ID';
COMMENT ON COLUMN erp_expense_item.expense_code IS '费用编号';
COMMENT ON COLUMN erp_expense_item.expense_name IS '费用名称';
COMMENT ON COLUMN erp_expense_item.subject_code IS '费用科目编码(凭证用)';
COMMENT ON COLUMN erp_expense_item.amount IS '金额';

CREATE INDEX IF NOT EXISTS idx_expense_item_master ON erp_expense_item(expense_doc_id);
CREATE INDEX IF NOT EXISTS idx_expense_item_tenant ON erp_expense_item(tenant_id);
CREATE INDEX IF NOT EXISTS idx_expense_item_subject ON erp_expense_item(subject_code);
