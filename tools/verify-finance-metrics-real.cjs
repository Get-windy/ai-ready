/*
 * 经营看板「财务指标」真实取数验证（2026-09-26）
 *
 * 背景（FINANCE_MODULE_AUDIT §3.11）：MetricCollectorServiceImpl.collectFinanceMetrics()
 * 原先 5 个指标（今日收入/支出/应收/应付/毛利率）**全部是 generateRandomValue(...) 随机数**，
 * 且 MetricCollectorConfig 每 5 分钟采集一次 ⇒ 看板上的财务数字与实际账务毫无关系。
 * 本轮改为按真实数据取数（口径见该方法注释）。
 *
 * 验证思路：**独立用 SQL 按同一口径算出期望值**，再等一次采集周期（默认 5 分钟）后
 * 对比库中最新一条 finance 指标。若是随机数，两者不可能相等。
 *
 * 用法：node tools/verify-finance-metrics-real.cjs [--wait]
 *       不带 --wait 时若库中尚无「本次重启后」的新数据，会提示等待。
 */
const { execFileSync } = require('child_process')
const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe'
const PGENV = { ...process.env, PGPASSWORD: 'devuser123' }

function q(sql) {
  return execFileSync(PSQL, ['-h', 'localhost', '-p', '5432', '-U', 'devuser', '-d', 'devdb', '-t', '-A', '-c', sql],
    { env: PGENV, encoding: 'utf8' }).trim()
}
let pass = 0, fail = 0
function check(desc, ok, detail) {
  console.log(`${ok ? '✅' : '❌'} ${desc}${detail ? ' → ' + detail : ''}`)
  ok ? pass++ : fail++
}
const TENANT = 1

// ── 按方法注释里的口径，独立算出期望值 ──
const EXPECT = {
  finance_today_revenue: q(`SELECT COALESCE(SUM(i.credit_amount - i.debit_amount), 0)
    FROM finance_voucher_item i JOIN finance_voucher v ON v.id = i.voucher_id
    JOIN finance_account_subject s ON s.id = i.subject_id
    WHERE v.status='posted' AND v.deleted_flag=0 AND i.deleted_flag=0
      AND s.subject_type=5 AND s.direction=2
      AND v.voucher_date = CURRENT_DATE AND v.tenant_id=${TENANT}`),
  finance_today_expense: q(`SELECT COALESCE(SUM(i.debit_amount - i.credit_amount), 0)
    FROM finance_voucher_item i JOIN finance_voucher v ON v.id = i.voucher_id
    JOIN finance_account_subject s ON s.id = i.subject_id
    WHERE v.status='posted' AND v.deleted_flag=0 AND i.deleted_flag=0
      AND s.subject_type=5 AND s.direction=1
      AND v.voucher_date = CURRENT_DATE AND v.tenant_id=${TENANT}`),
  finance_receivable: q(`SELECT COALESCE(SUM(remaining_amount), 0) FROM finance_receivable
    WHERE deleted_flag=0 AND tenant_id=${TENANT} AND COALESCE(status,'normal') <> 'cancelled'`),
  finance_payable: q(`SELECT COALESCE(SUM(remaining_amount), 0) FROM finance_payable
    WHERE deleted_flag=0 AND tenant_id=${TENANT} AND COALESCE(status,'normal') <> 'cancelled'`),
}
const yrRev = q(`SELECT COALESCE(SUM(i.credit_amount - i.debit_amount), 0)
  FROM finance_voucher_item i JOIN finance_voucher v ON v.id = i.voucher_id
  JOIN finance_account_subject s ON s.id = i.subject_id
  WHERE v.status='posted' AND v.deleted_flag=0 AND i.deleted_flag=0
    AND s.subject_type=5 AND s.direction=2
    AND v.fiscal_year = EXTRACT(YEAR FROM CURRENT_DATE) AND v.tenant_id=${TENANT}`)
const yrCost = q(`SELECT COALESCE(SUM(i.debit_amount - i.credit_amount), 0)
  FROM finance_voucher_item i JOIN finance_voucher v ON v.id = i.voucher_id
  JOIN finance_account_subject s ON s.id = i.subject_id
  WHERE v.status='posted' AND v.deleted_flag=0 AND i.deleted_flag=0
  AND s.subject_type=4
    AND v.fiscal_year = EXTRACT(YEAR FROM CURRENT_DATE) AND v.tenant_id=${TENANT}`)
EXPECT.finance_gross_margin = Number(yrRev) === 0
  ? '0.00'
  : ((Number(yrRev) - Number(yrCost)) * 100 / Number(yrRev)).toFixed(2)

console.log('按同一口径独立算出的期望值：')
for (const [k, v] of Object.entries(EXPECT)) console.log(`  ${k.padEnd(24)} ${v}`)
console.log('')

// ── 库中最新一条（采集器写入的）──
const latestTime = q(`select coalesce(max(stat_time)::text,'-') from erp_business_metric where metric_code like 'finance%'`)
console.log(`库中 finance 指标最新采集时间：${latestTime}\n`)

for (const [code, expected] of Object.entries(EXPECT)) {
  const actual = q(`select metric_value from erp_business_metric where metric_code='${code}' order by stat_time desc limit 1`)
  const a = Number(actual), e = Number(expected)
  check(`${code} 与真实口径一致`, Math.abs(a - e) < 0.01, `库中=${actual} 期望=${expected}`)
}

console.log(`\n===== 结果：${pass} 通过 / ${fail} 失败 =====`)
if (fail > 0) {
  console.log('提示：若后端刚重启、尚未到 5 分钟采集周期，库中仍是重启前写入的随机数 —— 稍等一轮再跑。')
}
process.exit(fail > 0 ? 1 : 0)
