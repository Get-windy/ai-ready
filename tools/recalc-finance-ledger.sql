-- 总账重算：以「已过账凭证分录」为唯一口径重算 finance_ledger
--
-- 背景（FINANCE_MODULE_AUDIT §2.1，本模块最严重的 P0）：
--   LedgerServiceImpl.postToLedger() 是**纯累加式**（period_debit += 分录借额），
--   没有「按凭证重算 / 回滚」的入口 ⇒ 凭证被删改后总账**永久残留**，只增不减。
--   后果：总账 ≠ 凭证分录，而利润表/资产负债表/总账账簿都读 finance_ledger
--   ⇒ 对外财务数字失真。实测重算前总账借方合计 1,000,361，凭证口径仅 949,921。
--
-- 本脚本做的事：按凭证分录重算每个 (租户, 科目, 年, 期) 的四段余额。
--   期初 = 该期之前所有期的净额累计（纯凭证口径，与《辅助核算余额表》一致）
--   本期发生 = 该期「已过账且未删」凭证分录的借贷合计
--   期末 = 期初 + 本期借 − 本期贷（按符号拆到借/贷两列）
--
-- 幂等：可重复执行；重复执行结果不变。
-- 用法：psql -h localhost -p 5432 -U devuser -d devdb -f tools/recalc-finance-ledger.sql
--
-- ⚠️ 执行前请先备份：CREATE TABLE finance_ledger_backup_YYYYMMDD AS SELECT * FROM finance_ledger;
-- ⚠️ 这是「订正数据」而非「修复病根」——postToLedger 的累加式逻辑未改，
--    今后再删改凭证仍会产生漂移，需按 §2.1 的后续步骤（改为按凭证重算该期）根治。

BEGIN;

-- 前置①：回填凭证分录缺失的 subject_id。
-- 实证：前端手工录单只传 subjectCode（不传 id），而 buildItem 原先原样落库 ⇒
-- 145 条分录中有 36 条 subject_id 为 NULL。总账重算是按 subject_id 维度汇总的，
-- 这些分录会被整批漏算。2026-09-26 已在 VoucherServiceImpl.buildItem 补了兜底解析
-- （新数据不再产生），此处负责订正存量。
UPDATE finance_voucher_item i
SET subject_id = s.id
FROM finance_account_subject s
WHERE i.subject_id IS NULL
  AND i.deleted_flag = 0
  AND i.subject_code = s.subject_code
  AND s.deleted_flag = 0;

-- 前置②：修正 tenant_id 为 NULL 的总账行。
-- 实测存在 1 行（科目 2203）：这类行因 tenant_id 为空，重算的 JOIN 匹配不上会被**静默跳过**，
-- 表现为「重算后仍有一行与凭证口径不符」。归属与同表其它行一致的租户 1。
UPDATE finance_ledger SET tenant_id = 1 WHERE tenant_id IS NULL;

WITH v AS (
  -- 凭证口径：已过账 + 未删除的凭证分录，按 (租户, 科目, 年, 期) 汇总
  SELECT v.tenant_id, i.subject_code, v.fiscal_year, v.fiscal_period,
         SUM(i.debit_amount)  AS dr,
         SUM(i.credit_amount) AS cr
  FROM finance_voucher_item i
  JOIN finance_voucher v ON v.id = i.voucher_id
  WHERE v.status = 'posted' AND v.deleted_flag = 0
  GROUP BY 1, 2, 3, 4
),
allp AS (
  -- 目标期间集合 = 有凭证的期间 ∪ 现有总账已有行的期间
  -- （后者保证「有总账行但凭证已删」的期间也会被重算为 0，而不是保留虚增值）
  SELECT tenant_id, subject_code, fiscal_year, fiscal_period FROM v
  UNION
  SELECT tenant_id, subject_code, fiscal_year, fiscal_period FROM finance_ledger
),
agg AS (
  SELECT a.tenant_id, a.subject_code, a.fiscal_year, a.fiscal_period,
         COALESCE(v.dr, 0) AS dr,
         COALESCE(v.cr, 0) AS cr,
         COALESCE(v.dr, 0) - COALESCE(v.cr, 0) AS net
  FROM allp a
  LEFT JOIN v ON v.tenant_id   = a.tenant_id
             AND v.subject_code = a.subject_code
             AND v.fiscal_year  = a.fiscal_year
             AND v.fiscal_period = a.fiscal_period
),
cum AS (
  SELECT *,
    -- 期初净额 = 该期之前所有期的净额累计
    COALESCE(SUM(net) OVER (PARTITION BY tenant_id, subject_code
                            ORDER BY fiscal_year, fiscal_period
                            ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) AS opening_net,
    -- 期末净额 = 含本期的累计
    SUM(net) OVER (PARTITION BY tenant_id, subject_code
                   ORDER BY fiscal_year, fiscal_period) AS closing_net
  FROM agg
)
UPDATE finance_ledger l SET
  opening_debit     = CASE WHEN c.opening_net >= 0 THEN  c.opening_net ELSE 0 END,
  opening_credit    = CASE WHEN c.opening_net <  0 THEN -c.opening_net ELSE 0 END,
  period_debit      = c.dr,
  period_credit     = c.cr,
  closing_debit     = CASE WHEN c.closing_net >= 0 THEN  c.closing_net ELSE 0 END,
  closing_credit    = CASE WHEN c.closing_net <  0 THEN -c.closing_net ELSE 0 END,
  closing_balance   = c.closing_net,
  balance_direction = CASE WHEN c.closing_net >= 0 THEN 1 ELSE 2 END,
  updated_at        = now()
FROM cum c
WHERE l.tenant_id     = c.tenant_id
  AND l.subject_code  = c.subject_code
  AND l.fiscal_year   = c.fiscal_year
  AND l.fiscal_period = c.fiscal_period;

-- 自检（事务内立即校验，避免并发窗口）
-- 期望：mismatch = 0；各年度 balanced = true
SELECT 'mismatch' AS chk, count(*)::text AS val
FROM finance_ledger l
LEFT JOIN (
  SELECT v.tenant_id tv, v.fiscal_year, v.fiscal_period, i.subject_code,
         SUM(i.debit_amount) dr, SUM(i.credit_amount) cr
  FROM finance_voucher_item i
  JOIN finance_voucher v ON v.id = i.voucher_id
  WHERE v.status = 'posted' AND v.deleted_flag = 0
  GROUP BY 1, 2, 3, 4
) v ON v.fiscal_year  = l.fiscal_year
   AND v.fiscal_period = l.fiscal_period
   AND v.subject_code  = l.subject_code
   AND v.tv            = l.tenant_id
WHERE l.period_debit <> COALESCE(v.dr, 0) OR l.period_credit <> COALESCE(v.cr, 0)
UNION ALL
SELECT 'balanced_' || fiscal_year::text, (SUM(period_debit) = SUM(period_credit))::text
FROM finance_ledger GROUP BY fiscal_year
ORDER BY 1;

COMMIT;
