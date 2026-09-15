-- V11.90.0: 其他收入单金标准增强
-- 1) 主表补列（列表按单据25列 / 表单17字段）
-- 2) income_type 改为 INTEGER（1-往来单位收入 2-内部收入）
-- 3) 新建收入项明细表 fin_other_income_doc_item
-- 4) 双入口菜单幂等对齐（80116）

-- ============================================================
-- 1. 主表补列
-- ============================================================
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS partner_code VARCHAR(200);
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS handler_id BIGINT;
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS handler_name VARCHAR(200);
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS receipt_account1 VARCHAR(200);
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS receipt_amount1 NUMERIC(18,4) DEFAULT 0;
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS receipt_account2 VARCHAR(200);
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS receipt_amount2 NUMERIC(18,4) DEFAULT 0;
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS receipt_account3 VARCHAR(200);
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS receipt_amount3 NUMERIC(18,4) DEFAULT 0;
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS receipt_account4 VARCHAR(200);
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS receipt_amount4 NUMERIC(18,4) DEFAULT 0;
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS account_subject_code VARCHAR(50);
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS summary VARCHAR(500);
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS attachment VARCHAR(500);
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS settle_status INTEGER DEFAULT 0;
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS bookkeeper_id BIGINT;
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS bookkeeper_name VARCHAR(200);
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS bookkeeping_time TIMESTAMP;
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS print_count INTEGER DEFAULT 0;
ALTER TABLE fin_other_income_doc ADD COLUMN IF NOT EXISTS voucher_no VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_other_income_settle_status ON fin_other_income_doc(settle_status);
CREATE INDEX IF NOT EXISTS idx_other_income_partner ON fin_other_income_doc(partner_id);
CREATE INDEX IF NOT EXISTS idx_other_income_handler ON fin_other_income_doc(handler_id);

-- ============================================================
-- 2. income_type 类型转换 VARCHAR -> INTEGER
--    事务单采用字符串语义（如 INTEREST/RENT/OTHER），统一转：
--    1-往来单位收入 2-内部收入
-- ============================================================
-- 先移除列默认值，避免 VARCHAR 类型的默认值无法转换为 INTEGER（SQL State 42804）
ALTER TABLE fin_other_income_doc ALTER COLUMN income_type DROP DEFAULT;
ALTER TABLE fin_other_income_doc ALTER COLUMN income_type TYPE INTEGER USING (
    CASE
        WHEN income_type IS NULL OR income_type = '' THEN 2
        WHEN income_type ~ '^[0-9]+$' THEN income_type::integer
        WHEN income_type IN ('PARTNER', 'CUSTOMER', 'SUPPLIER', '往来单位收入') THEN 1
        ELSE 2
    END
);

COMMENT ON COLUMN fin_other_income_doc.income_type IS '收入类型: 1-往来单位收入 2-内部收入';
COMMENT ON COLUMN fin_other_income_doc.settle_status IS '结算状态: 0-未结算 1-已结算';
COMMENT ON COLUMN fin_other_income_doc.status IS '状态: 0-草稿 1-已记账/已入账';

-- ============================================================
-- 3. 收入项明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS fin_other_income_doc_item (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    doc_id BIGINT NOT NULL,
    line_no INTEGER DEFAULT 1,
    income_no VARCHAR(50),
    income_name VARCHAR(200),
    amount NUMERIC(18,4) DEFAULT 0,
    subject_code VARCHAR(50),
    remark VARCHAR(500),
    deleted INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT
);

CREATE INDEX IF NOT EXISTS idx_other_income_item_doc ON fin_other_income_doc_item(doc_id);

COMMENT ON TABLE fin_other_income_doc_item IS '其他收入单-收入项明细';
COMMENT ON COLUMN fin_other_income_doc_item.subject_code IS '收益类科目编码';

-- ============================================================
-- 4. 双入口菜单幂等对齐（80116：主入口表单 / 历史标签列表）
-- ============================================================
UPDATE sys_menu
SET display_mode = 1,
    tag_label = '历史',
    list_path = 'finance/other-income-doc/index',
    component = 'views/finance/other-income-doc/form.vue',
    update_time = CURRENT_TIMESTAMP
WHERE id = 80116 AND deleted = 0;
