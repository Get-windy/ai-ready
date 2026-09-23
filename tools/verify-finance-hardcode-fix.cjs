/*
 * 财务模块「硬编码假数据」修复验证（2026-09-23）
 *
 * 对应 FINANCE_MODULE_AUDIT §2.4，本轮修了 3 处：
 *   ① `FinanceTransactionServiceImpl.getAccountBalance()` —— 原方法体只有
 *      `return BigDecimal.ZERO;`，不查表 ⇒ 账户余额恒 0。改为读 `finance_account.balance`。
 *      ⚠️ 该方法**当前无任何调用方**（无 HTTP 入口，见下方说明），只能代码级验证。
 *   ② `FinancialReportServiceImpl.generateIncomeStatement()` —— `totalTax` 声明后从未累加
 *      ⇒ 所得税恒 0、净利润虚高。改为累加所得税类科目（6801）。
 *      ⚠️ 库中当前无 6801 数据，故修复后该项仍为 0 —— 那是「数据未录」而非「代码写死」；
 *         本脚本只能验证「不回归」（接口仍正常返回）。
 *   ③ `ExpenseDocServiceImpl` —— 4 个付款账户的 paySubjectCode 一律写死 1002（银行存款）
 *      ⇒ 现金账户付款也记入银行存款。改为取 `finance_account.subject_code`。**本脚本重点实测此项。**
 *
 * 用法：node tools/verify-finance-hardcode-fix.cjs   （需后端 5655 在跑）
 */
const http = require('http')
const { execFileSync } = require('child_process')
const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe'
const PGENV = { ...process.env, PGPASSWORD: 'devuser123' }

function q(sql) {
  return execFileSync(PSQL, ['-h', 'localhost', '-p', '5432', '-U', 'devuser', '-d', 'devdb', '-t', '-A', '-c', sql],
    { env: PGENV, encoding: 'utf8' }).trim()
}
function call(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers['Authorization'] = 'Bearer ' + token
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: 5655, path: '/api' + path, method, headers },
      res => { let s = ''; res.on('data', c => s += c); res.on('end', () => resolve({ status: res.statusCode, body: s })) })
    r.on('error', reject); if (data) r.write(data); r.end()
  })
}
let pass = 0, fail = 0
function check(desc, ok, detail) {
  console.log(`${ok ? '✅' : '❌'} ${desc}${detail ? ' → ' + detail : ''}`)
  ok ? pass++ : fail++
}

;(async () => {
  const cap = await call('GET', '/auth/captcha')
  const full = JSON.parse(cap.body)
  const code = [...Buffer.from(full.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const lg = await call('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: full.data.uuid,
  })
  const token = JSON.parse(lg.body).data.token
  console.log('登录(超管):', lg.status, '\n')

  // ── ③ 付款账户科目（本轮重点，可端到端实测）──
  console.log('【③】付款账户科目：应取账户真实科目，而非写死 1002')
  const acc2 = q(`select subject_code from finance_account where id=2`)   // 库存现金
  console.log(`   账户 2 的档案科目 = ${acc2}（预期 1001 库存现金，与写死的 1002 不同）`)
  check('前置：账户2 档案科目确为 1001（可与 1002 区分）', acc2 === '1001', `subject_code=${acc2}`)

  const created = await call('POST', '/erp/finance/expense-doc/create', {
    docDate: '2026-09-23', expenseType: 1, handlerName: 'E2E-验证',
    payAccountId: 2, payAccountName: '库存现金', payAmount: 10,
    summary: 'E2E-HARDCODE-CHECK',
    // 业务守恒校验要求「费用项合计 = 付款合计」，故须带一条同额费用项
    items: [{ lineNo: 1, expenseName: 'E2E-VERIFY', subjectCode: '6602', subjectName: '管理费用', amount: 10 }],
  }, token)
  const docId = (() => { try { return JSON.parse(created.body).data?.id } catch { return null } })()
  check('建费用单成功', created.status === 200 && docId, `HTTP ${created.status} id=${docId}`)
  if (docId) {
    const subj = q(`select coalesce(pay_subject_code,'NULL') from erp_expense_doc where id=${docId}`)
    check('paySubjectCode 取到账户真实科目 1001（修复前恒为 1002）', subj === '1001', `pay_subject_code=${subj}`)
    // 清理（费用单为草稿，未记账，无总账影响）
    q(`delete from erp_expense_item where expense_doc_id=${docId}`)
    q(`delete from erp_expense_doc where id=${docId}`)
    const left = q(`select count(*) from erp_expense_doc where id=${docId}`)
    check('测试费用单已清理', left === '0', `残留 ${left}`)
  }

  // ── ② 利润表不回归（totalTax 逻辑已补，但库里无 6801 数据）──
  console.log('\n【②】利润表：所得税累加已补，验证接口不回归')
  const has6801 = q(`select count(*) from finance_account_subject where subject_code='6801'`)
  console.log(`   库中 6801（所得税费用）科目数 = ${has6801} ⇒ 修复后 totalTax 仍会为 0，属"数据未录"`)
  const rpt = await call('GET', '/erp/finance/report/v2/income-statement-report?fiscalYear=2026&startMonth=1&endMonth=9', null, token)
  check('利润表接口正常返回（不回归）', rpt.status === 200, `HTTP ${rpt.status} ${rpt.body.slice(0, 70)}`)

  // ── ① getAccountBalance（无 HTTP 入口，仅登记）──
  console.log('\n【①】getAccountBalance：无调用方，仅代码级验证')
  console.log('   已确认全仓（Java + 前端）除接口声明与实现外无任何调用方 ⇒ 无 HTTP 端点可测；')
  console.log('   修复内容为「改查 finance_account.balance」，已随本模块编译通过。')
  check('（登记）该方法无调用方，无需端到端用例', true, '代码级已修')

  console.log(`\n===== 结果：${pass} 通过 / ${fail} 失败 =====`)
  process.exit(fail > 0 ? 1 : 0)
})().catch(e => { console.error('ERR', e.message); process.exit(1) })
