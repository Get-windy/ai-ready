-- ============================================================
-- V11.37.0: 财务辅助核算模块建表 + 种子数据
--
-- 新增：
--   1. finance_auxiliary_type    —— 辅助核算类型表
--   2. finance_auxiliary_item    —— 辅助核算项目表
--   3. finance_auxiliary_balance —— 辅助核算余额表
--
-- 种子数据：5种默认辅助核算类型（部门/项目/客户/供应商/员工）
--
-- 幂等：IF NOT EXISTS / ON CONFLICT DO NOTHING 守卫
-- ============================================================

-- ----------------------------------------------------------
-- 1. finance_auxiliary_type 辅助核算类型表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS finance_auxiliary_type (
    id              BIGINT          PRIMARY KEY,
    tenant_id       BIGINT          DEFAULT 0,
    deleted         INTEGER         NOT NULL DEFAULT 0,
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    create_by       BIGINT,
    update_by       BIGINT,
    type_code       VARCHAR(50)     NOT NULL,
    type_name       VARCHAR(100)    NOT NULL,
    enabled         BOOLEAN         DEFAULT TRUE,
    sort            INTEGER         DEFAULT 0,
    remark          VARCHAR(500)
);

COMMENT ON TABLE finance_auxiliary_type IS '财务辅助核算类型表';
COMMENT ON COLUMN finance_auxiliary_type.id IS '主键ID';
COMMENT ON COLUMN finance_auxiliary_type.tenant_id IS '租户ID';
COMMENT ON COLUMN finance_auxiliary_type.deleted IS '逻辑删除(0=未删除)';
COMMENT ON COLUMN finance_auxiliary_type.type_code IS '类型编码';
COMMENT ON COLUMN finance_auxiliary_type.type_name IS '类型名称';
COMMENT ON COLUMN finance_auxiliary_type.enabled IS '是否启用';
COMMENT ON COLUMN finance_auxiliary_type.sort IS '排序号';
COMMENT ON COLUMN finance_auxiliary_type.remark IS '备注';

-- 类型编码按租户唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_finance_aux_type_tenant_code
    ON finance_auxiliary_type(tenant_id, type_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_aux_type_tenant_id
    ON finance_auxiliary_type(tenant_id);

-- ----------------------------------------------------------
-- 2. finance_auxiliary_item 辅助核算项目表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS finance_auxiliary_item (
    id                  BIGINT          PRIMARY KEY,
    tenant_id           BIGINT          DEFAULT 0,
    deleted             INTEGER         NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    create_by           BIGINT,
    update_by           BIGINT,
    auxiliary_type_id   BIGINT          NOT NULL,
    item_code           VARCHAR(50)     NOT NULL,
    item_name           VARCHAR(200)    NOT NULL,
    parent_id           BIGINT          DEFAULT 0,
    enabled             BOOLEAN         DEFAULT TRUE,
    sort                INTEGER         DEFAULT 0,
    remark              VARCHAR(500)
);

COMMENT ON TABLE finance_auxiliary_item IS '财务辅助核算项目表';
COMMENT ON COLUMN finance_auxiliary_item.id IS '主键ID';
COMMENT ON COLUMN finance_auxiliary_item.tenant_id IS '租户ID';
COMMENT ON COLUMN finance_auxiliary_item.deleted IS '逻辑删除(0=未删除)';
COMMENT ON COLUMN finance_auxiliary_item.auxiliary_type_id IS '辅助核算类型ID';
COMMENT ON COLUMN finance_auxiliary_item.item_code IS '项目编码';
COMMENT ON COLUMN finance_auxiliary_item.item_name IS '项目名称';
COMMENT ON COLUMN finance_auxiliary_item.parent_id IS '上级ID(0=顶级)';
COMMENT ON COLUMN finance_auxiliary_item.enabled IS '是否启用';
COMMENT ON COLUMN finance_auxiliary_item.sort IS '排序号';
COMMENT ON COLUMN finance_auxiliary_item.remark IS '备注';

CREATE INDEX IF NOT EXISTS idx_finance_aux_item_type_id
    ON finance_auxiliary_item(auxiliary_type_id);
CREATE INDEX IF NOT EXISTS idx_finance_aux_item_tenant_id
    ON finance_auxiliary_item(tenant_id);
CREATE INDEX IF NOT EXISTS idx_finance_aux_item_code
    ON finance_auxiliary_item(item_code);

-- ----------------------------------------------------------
-- 3. finance_auxiliary_balance 辅助核算余额表
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS finance_auxiliary_balance (
    id                      BIGINT          PRIMARY KEY,
    tenant_id               BIGINT          DEFAULT 0,
    deleted                 INTEGER         NOT NULL DEFAULT 0,
    create_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    accounting_period_id    BIGINT          NOT NULL,
    subject_id              BIGINT          NOT NULL,
    auxiliary_type_id       BIGINT          NOT NULL,
    auxiliary_item_id       BIGINT          NOT NULL,
    begin_debit             DECIMAL(18,2)   DEFAULT 0,
    begin_credit            DECIMAL(18,2)   DEFAULT 0,
    period_debit            DECIMAL(18,2)   DEFAULT 0,
    period_credit           DECIMAL(18,2)   DEFAULT 0,
    end_debit               DECIMAL(18,2)   DEFAULT 0,
    end_credit              DECIMAL(18,2)   DEFAULT 0,
    year_debit              DECIMAL(18,2)   DEFAULT 0,
    year_credit             DECIMAL(18,2)   DEFAULT 0
);

COMMENT ON TABLE finance_auxiliary_balance IS '财务辅助核算余额表';
COMMENT ON COLUMN finance_auxiliary_balance.id IS '主键ID';
COMMENT ON COLUMN finance_auxiliary_balance.tenant_id IS '租户ID';
COMMENT ON COLUMN finance_auxiliary_balance.deleted IS '逻辑删除(0=未删除)';
COMMENT ON COLUMN finance_auxiliary_balance.accounting_period_id IS '会计期间ID';
COMMENT ON COLUMN finance_auxiliary_balance.subject_id IS '科目ID';
COMMENT ON COLUMN finance_auxiliary_balance.auxiliary_type_id IS '辅助核算类型ID';
COMMENT ON COLUMN finance_auxiliary_balance.auxiliary_item_id IS '辅助核算项目ID';
COMMENT ON COLUMN finance_auxiliary_balance.begin_debit IS '期初借方余额';
COMMENT ON COLUMN finance_auxiliary_balance.begin_credit IS '期初贷方余额';
COMMENT ON COLUMN finance_auxiliary_balance.period_debit IS '本期借方发生额';
COMMENT ON COLUMN finance_auxiliary_balance.period_credit IS '本期贷方发生额';
COMMENT ON COLUMN finance_auxiliary_balance.end_debit IS '期末借方余额';
COMMENT ON COLUMN finance_auxiliary_balance.end_credit IS '期末贷方余额';
COMMENT ON COLUMN finance_auxiliary_balance.year_debit IS '年累计借方发生额';
COMMENT ON COLUMN finance_auxiliary_balance.year_credit IS '年累计贷方发生额';

-- 会计期间+科目+类型+项目 唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_finance_aux_balance_period_subj_type_item
    ON finance_auxiliary_balance(accounting_period_id, subject_id, auxiliary_type_id, auxiliary_item_id)
    WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_aux_balance_tenant_id
    ON finance_auxiliary_balance(tenant_id);

-- ----------------------------------------------------------
-- 4. 种子数据：默认辅助核算类型
--    tenant_id=0 表示系统级默认数据，所有租户共享
-- ----------------------------------------------------------
INSERT INTO finance_auxiliary_type
    (id, tenant_id, deleted, create_time, update_time, type_code, type_name, enabled, sort, remark)
VALUES
    (1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'DEPT',     '部门',   TRUE, 1, '按部门进行辅助核算'),
    (2, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'PROJECT',  '项目',   TRUE, 2, '按项目进行辅助核算'),
    (3, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'CUSTOMER', '客户',   TRUE, 3, '按客户进行辅助核算'),
    (4, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SUPPLIER', '供应商', TRUE, 4, '按供应商进行辅助核算'),
    (5, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'EMPLOYEE', '员工',   TRUE, 5, '按员工进行辅助核算')
ON CONFLICT (id) DO NOTHING;
