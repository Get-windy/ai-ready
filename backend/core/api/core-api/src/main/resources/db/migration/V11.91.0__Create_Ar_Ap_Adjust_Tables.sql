-- ============================================================
-- V11.87.0: 应收应付调整建表脚本
--
-- 背景：应收应付调整实现「不动资金账户的往来余额调整」，
--   应收增加/应收减少/应付增加/应付减少四向调整。对标 ql361 金标准：
--   1) 主表 erp_ar_ap_adjust：单据日期 / 结算单位 / 经手人 / 调整方向 /
--      部门 / 单据备注 / 制单人 / 记账人 / 打印次数 / 本单金额等。
--   2) 明细表 erp_ar_ap_adjust_item：科目编号 / 科目名称 / 金额 / 备注
--      （调整实现对转科目，金额可多行拆分）。
--   3) 本单金额 = Σ明细金额。
--   4) 记账（P0 红线）：必须经会计凭证（KJPZ-），严禁绕过凭证直改余额；
--      无账户字段、不产生资金流水，仅调整往来余额与对应科目。
--   5) 单号规则：YSKZJ-YYYYMMDD-序号。
--   6) 调整方向 1=应收增加 2=应收减少 3=应付增加 4=应付减少；
--      应收方向结算单位为客户(customer)，应付方向为供应商(supplier)。
-- ============================================================

-- ── 1. 应收应付调整主表（头表） ──
CREATE TABLE IF NOT EXISTS erp_ar_ap_adjust (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    doc_no VARCHAR(100) NOT NULL,                      -- 单号 YSKZJ-YYYYMMDD-序号
    doc_date DATE,                                     -- 单据日期
    direction INT DEFAULT 1,                           -- 调整方向 1应收增加 2应收减少 3应付增加 4应付减少
    direction_name VARCHAR(50),                        -- 调整方向显示名（应收增加等）
    partner_type VARCHAR(30),                          -- 结算单位类型 customer-客户 supplier-供应商
    partner_id BIGINT,                                 -- 结算单位ID
    partner_code VARCHAR(100),                         -- 结算单位编号
    partner_name VARCHAR(200),                         -- 结算单位名称
    handler_id BIGINT,                                 -- 经手人
    handler_name VARCHAR(100),
    dept_id BIGINT,                                    -- 部门
    dept_name VARCHAR(100),
    total_amount DECIMAL(20,2) DEFAULT 0,              -- 本单金额 = Σ明细金额
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
COMMENT ON TABLE erp_ar_ap_adjust IS '应收应付调整主表：不动资金账户的往来余额调整';
COMMENT ON COLUMN erp_ar_ap_adjust.doc_no IS '单号 YSKZJ-YYYYMMDD-序号';
COMMENT ON COLUMN erp_ar_ap_adjust.doc_date IS '单据日期';
COMMENT ON COLUMN erp_ar_ap_adjust.direction IS '调整方向 1应收增加 2应收减少 3应付增加 4应付减少';
COMMENT ON COLUMN erp_ar_ap_adjust.direction_name IS '调整方向显示名（应收增加等）';
COMMENT ON COLUMN erp_ar_ap_adjust.partner_type IS '结算单位类型 customer-客户 supplier-供应商';
COMMENT ON COLUMN erp_ar_ap_adjust.partner_id IS '结算单位ID';
COMMENT ON COLUMN erp_ar_ap_adjust.partner_code IS '结算单位编号';
COMMENT ON COLUMN erp_ar_ap_adjust.partner_name IS '结算单位名称';
COMMENT ON COLUMN erp_ar_ap_adjust.total_amount IS '本单金额 = Σ明细金额';
COMMENT ON COLUMN erp_ar_ap_adjust.status IS '0-草稿 1-已记账 2-已取消';
COMMENT ON COLUMN erp_ar_ap_adjust.creator_name IS '制单人';
COMMENT ON COLUMN erp_ar_ap_adjust.bookkeeper_name IS '记账人';
COMMENT ON COLUMN erp_ar_ap_adjust.bookkeeping_time IS '记账时间';
COMMENT ON COLUMN erp_ar_ap_adjust.red_flag IS '红冲标记';

CREATE UNIQUE INDEX IF NOT EXISTS uk_ar_ap_adjust_docno ON erp_ar_ap_adjust(doc_no);
CREATE INDEX IF NOT EXISTS idx_ar_ap_adjust_tenant ON erp_ar_ap_adjust(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ar_ap_adjust_status ON erp_ar_ap_adjust(status);
CREATE INDEX IF NOT EXISTS idx_ar_ap_adjust_docdate ON erp_ar_ap_adjust(doc_date);
CREATE INDEX IF NOT EXISTS idx_ar_ap_adjust_direction ON erp_ar_ap_adjust(direction);
CREATE INDEX IF NOT EXISTS idx_ar_ap_adjust_partner ON erp_ar_ap_adjust(partner_id);

-- ── 2. 应收应付调整明细表（科目明细） ──
CREATE TABLE IF NOT EXISTS erp_ar_ap_adjust_item (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT DEFAULT 1,
    adjust_id BIGINT NOT NULL,                         -- 关联 erp_ar_ap_adjust.id
    line_no INT DEFAULT 1,                             -- 行号
    subject_code VARCHAR(50),                          -- 科目编号（实现对转科目）
    subject_name VARCHAR(200),                         -- 科目名称
    amount DECIMAL(20,2) DEFAULT 0,                    -- 金额
    remark VARCHAR(500),                               -- 备注
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    version_no INT
);
COMMENT ON TABLE erp_ar_ap_adjust_item IS '应收应付调整-科目明细（实现对转科目，金额可多行拆分）';
COMMENT ON COLUMN erp_ar_ap_adjust_item.adjust_id IS '主表ID';
COMMENT ON COLUMN erp_ar_ap_adjust_item.subject_code IS '科目编号（实现对转科目）';
COMMENT ON COLUMN erp_ar_ap_adjust_item.subject_name IS '科目名称';
COMMENT ON COLUMN erp_ar_ap_adjust_item.amount IS '金额';

CREATE INDEX IF NOT EXISTS idx_ar_ap_adjust_item_master ON erp_ar_ap_adjust_item(adjust_id);
CREATE INDEX IF NOT EXISTS idx_ar_ap_adjust_item_tenant ON erp_ar_ap_adjust_item(tenant_id);
