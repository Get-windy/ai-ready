-- ============================================================
-- V11.36.0: 采购费用分摊表字段对齐（对标采购费用分摊开发文档）
-- 主表补：经手人/部门/摘要/附件/记账人/记账时间/制单人名称
-- 入库单分摊明细补：单据编号/供应商/结算单位/计价单位/优惠后单价/优惠后金额
-- 新增：费用单明细表（费用单明细9列）
-- ============================================================

-- ── 主表 erp_cost_sharing ──
ALTER TABLE erp_cost_sharing ADD COLUMN IF NOT EXISTS handler_id BIGINT;
ALTER TABLE erp_cost_sharing ADD COLUMN IF NOT EXISTS handler_name VARCHAR(100);
ALTER TABLE erp_cost_sharing ADD COLUMN IF NOT EXISTS department_id BIGINT;
ALTER TABLE erp_cost_sharing ADD COLUMN IF NOT EXISTS department_name VARCHAR(100);
ALTER TABLE erp_cost_sharing ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
ALTER TABLE erp_cost_sharing ADD COLUMN IF NOT EXISTS attachment INT DEFAULT 0;
ALTER TABLE erp_cost_sharing ADD COLUMN IF NOT EXISTS bookkeeper_id BIGINT;
ALTER TABLE erp_cost_sharing ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(100);
ALTER TABLE erp_cost_sharing ADD COLUMN IF NOT EXISTS account_time TIMESTAMP;
ALTER TABLE erp_cost_sharing ADD COLUMN IF NOT EXISTS create_by_name VARCHAR(100);

-- ── 采购入库单分摊明细 erp_cost_sharing_item ──
ALTER TABLE erp_cost_sharing_item ADD COLUMN IF NOT EXISTS inbound_no VARCHAR(64);
ALTER TABLE erp_cost_sharing_item ADD COLUMN IF NOT EXISTS supplier_id BIGINT;
ALTER TABLE erp_cost_sharing_item ADD COLUMN IF NOT EXISTS supplier_name VARCHAR(255);
ALTER TABLE erp_cost_sharing_item ADD COLUMN IF NOT EXISTS supplier_code VARCHAR(64);
ALTER TABLE erp_cost_sharing_item ADD COLUMN IF NOT EXISTS settle_unit_id VARCHAR(64);
ALTER TABLE erp_cost_sharing_item ADD COLUMN IF NOT EXISTS settle_unit VARCHAR(255);
ALTER TABLE erp_cost_sharing_item ADD COLUMN IF NOT EXISTS pricing_unit VARCHAR(32);
ALTER TABLE erp_cost_sharing_item ADD COLUMN IF NOT EXISTS discounted_unit_price NUMERIC(18,4);
ALTER TABLE erp_cost_sharing_item ADD COLUMN IF NOT EXISTS discounted_amount NUMERIC(18,2);

-- ── 费用单明细表 ──
CREATE TABLE IF NOT EXISTS erp_cost_sharing_expense_item (
    id                BIGINT          PRIMARY KEY,
    cost_sharing_id   BIGINT          NOT NULL,
    expense_no        VARCHAR(64),
    partner_id        BIGINT,
    partner_name      VARCHAR(255),
    partner_code      VARCHAR(64),
    settle_unit_id    VARCHAR(64),
    settle_unit       VARCHAR(255),
    expense_type      VARCHAR(64),
    expense_amount    NUMERIC(18,2)   DEFAULT 0,
    remark            VARCHAR(500),
    deleted           INTEGER         NOT NULL DEFAULT 0,
    create_time       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by         BIGINT,
    update_by         BIGINT
);

CREATE INDEX IF NOT EXISTS idx_cost_sharing_expense_item_parent ON erp_cost_sharing_expense_item(cost_sharing_id);
