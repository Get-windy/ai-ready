-- ============================================================================
-- 预算执行金标准增强
-- 1) annual_budget 补「冻结金额汇总」——剩余=预算−已执行−冻结 必须成立，
--    概览卡片《冻结金额》与「剩余额度」以真实冻结合计为准（原表无该列，卡片恒为 0）。
-- 2) budget_item 补「最近执行日期」——按科目列示"最近执行"，用于执行跟踪与识别停滞预算。
-- 3) 补执行链索引：科目编码（费用/支出单据按科目回写匹配）、执行日期（趋势与区间统计）。
-- 4) 存量数据校准：按明细重算剩余与汇总，保证恒等式对历史数据同样成立。
--
-- 口径说明（P0 红线）：已执行/冻结/剩余 仅由预算控制服务（冻结/释放/消耗）维护并同步至
--   annual_budget 汇总，页面只读展示，严禁直接修改已执行或剩余额。
-- 说明：add column 全部 IF NOT EXISTS，与预算编制迁移（V11.96.0）共用列可重复执行。
-- ============================================================================

-- ------------------------------------------------------------
-- 1. 预算执行口径列
-- ------------------------------------------------------------
ALTER TABLE annual_budget ADD COLUMN IF NOT EXISTS total_frozen_amount numeric(15,2) DEFAULT 0;
ALTER TABLE budget_item ADD COLUMN IF NOT EXISTS last_exec_date date;

COMMENT ON COLUMN annual_budget.total_frozen_amount IS '冻结金额汇总：剩余 = 预算 − 已执行 − 冻结';
COMMENT ON COLUMN budget_item.last_exec_date IS '最近执行日期（冻结/释放/消耗取最近一次）';

-- ------------------------------------------------------------
-- 2. 执行链索引
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_budget_item_budget_id ON budget_item (budget_id);
CREATE INDEX IF NOT EXISTS idx_budget_item_subject_code ON budget_item (subject_code);
CREATE INDEX IF NOT EXISTS idx_budget_exec_log_budget_item ON budget_execution_log (budget_item_id);
CREATE INDEX IF NOT EXISTS idx_budget_exec_log_exec_date ON budget_execution_log (execution_date);
CREATE INDEX IF NOT EXISTS idx_annual_budget_fiscal_year ON annual_budget (fiscal_year);

-- ------------------------------------------------------------
-- 3. 存量数据校准
-- ------------------------------------------------------------
-- 保证 剩余 = 预算 − 已执行 − 冻结 对历史数据同样成立
UPDATE budget_item
SET remaining_amount = COALESCE(budget_amount, 0) - COALESCE(used_amount, 0) - COALESCE(frozen_amount, 0)
WHERE remaining_amount IS DISTINCT FROM COALESCE(budget_amount, 0) - COALESCE(used_amount, 0) - COALESCE(frozen_amount, 0);

UPDATE annual_budget b
SET total_used_amount   = agg.used,
    total_frozen_amount = agg.frozen,
    total_remaining_amount = COALESCE(b.total_amount, 0) - agg.used - agg.frozen,
    execution_rate = CASE WHEN COALESCE(b.total_amount, 0) > 0
                          THEN ROUND(agg.used * 100 / b.total_amount, 2) ELSE 0 END
FROM (
    SELECT budget_id,
           COALESCE(SUM(used_amount), 0)   AS used,
           COALESCE(SUM(frozen_amount), 0) AS frozen
    FROM budget_item
    WHERE deleted = false
    GROUP BY budget_id
) agg
WHERE b.id = agg.budget_id AND b.deleted = false;
