-- V9.47.0: 创建其他收入单表
-- Ref: Odoo 18.0 account.move - 其他收入类型
CREATE TABLE IF NOT EXISTS fin_other_income_doc (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    doc_no VARCHAR(50) NOT NULL,
    income_type VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    currency VARCHAR(10) DEFAULT 'CNY',
    partner_id BIGINT,
    partner_name VARCHAR(200),
    income_date DATE NOT NULL DEFAULT CURRENT_DATE,
    source VARCHAR(200),
    status INTEGER NOT NULL DEFAULT 0,
    settlement_method VARCHAR(50),
    bank_account VARCHAR(100),
    bank_name VARCHAR(200),
    transaction_no VARCHAR(100),
    department_id BIGINT,
    department_name VARCHAR(200),
    creator_id BIGINT,
    creator_name VARCHAR(200),
    remark VARCHAR(2000),
    approved_by BIGINT,
    approved_time TIMESTAMP,
    approved_note VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    version_no INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_other_income_tenant ON fin_other_income_doc(tenant_id);
CREATE INDEX IF NOT EXISTS idx_other_income_doc_no ON fin_other_income_doc(doc_no);
CREATE INDEX IF NOT EXISTS idx_other_income_date ON fin_other_income_doc(income_date);
CREATE INDEX IF NOT EXISTS idx_other_income_status ON fin_other_income_doc(status);

COMMENT ON TABLE fin_other_income_doc IS '其他收入单';
COMMENT ON COLUMN fin_other_income_doc.income_type IS '收入类型: INTEREST/RENT/ PENALTY/INSURANCE/OTHER';
COMMENT ON COLUMN fin_other_income_doc.status IS '状态: 0-草稿 1-待审核 2-已审核 3-已入账 4-已作废';
