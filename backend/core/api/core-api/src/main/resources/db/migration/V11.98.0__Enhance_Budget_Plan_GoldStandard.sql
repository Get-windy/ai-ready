-- ============================================================================
-- V11.96.0 预算编制（年度预算）金标准升级
-- 1) annual_budget 补编制/审批/打印字段
-- 2) budget_item 补明细行号/科目ID/科目类别/备注/最近执行日期
-- 3) 菜单 finance:budget-plan 收敛到金标准列表页（单入口）
-- 说明：全部使用 IF NOT EXISTS，可重复执行；编译口径与 JPA 实体保持一致。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. annual_budget 主表
-- ------------------------------------------------------------
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS budget_date DATE;
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS handler_id BIGINT;
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS handler_name VARCHAR(100);
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS creator_name VARCHAR(100);
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS auditor_id BIGINT;
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS auditor_name VARCHAR(100);
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS audit_time TIMESTAMP;
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS audit_remark VARCHAR(500);
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS print_count INTEGER DEFAULT 0;
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS total_frozen_amount NUMERIC(15, 2) DEFAULT 0;

COMMENT ON COLUMN annual_budget.budget_date IS '编制日期';
COMMENT ON COLUMN annual_budget.creator_name IS '制单人（快照）';
COMMENT ON COLUMN annual_budget.auditor_name IS '审核人（快照）';
COMMENT ON COLUMN annual_budget.audit_remark IS '审批意见';
COMMENT ON COLUMN annual_budget.total_frozen_amount IS '冻结金额汇总：剩余 = 预算 − 已执行 − 冻结';

CREATE INDEX IF NOT EXISTS idx_annual_budget_fiscal_year ON annual_budget (fiscal_year);
CREATE INDEX IF NOT EXISTS idx_annual_budget_status ON annual_budget (status);
CREATE INDEX IF NOT EXISTS idx_annual_budget_budget_date ON annual_budget (budget_date);

-- ------------------------------------------------------------
-- 2. budget_item 明细表
-- ------------------------------------------------------------
ALTER TABLE budget_item ADD COLUMN IF NOT EXISTS last_exec_date DATE;
ALTER TABLE budget_item ADD COLUMN IF NOT EXISTS line_no INTEGER;
ALTER TABLE budget_item ADD COLUMN IF NOT EXISTS subject_id BIGINT;
ALTER TABLE budget_item ADD COLUMN IF NOT EXISTS subject_type VARCHAR(50);
ALTER TABLE budget_item ADD COLUMN IF NOT EXISTS remark VARCHAR(500);

COMMENT ON COLUMN budget_item.line_no IS '行号';
COMMENT ON COLUMN budget_item.subject_id IS '预算科目ID（会计科目）';
COMMENT ON COLUMN budget_item.subject_type IS '科目类别';

CREATE INDEX IF NOT EXISTS idx_budget_item_budget_id ON budget_item (budget_id);

-- ------------------------------------------------------------
-- 3. 菜单升级：预算编制（双入口，与预付款单/费用单金标准一致）
--    主路由 = 编制表单页，list_path = 列表页（隐藏）
--    列表页内部「新增/查看/修改」通过 /finance/budget-plan/form 跳转
-- ------------------------------------------------------------
UPDATE sys_menu
SET path = 'finance/budget-plan/form',
    component = 'views/finance/budget-plan/form.vue',
    list_path = 'finance/budget-plan/index',
    display_mode = 1
WHERE menu_code = 'finance:budget-plan';
