// 预算执行金标准接口验收（真实数据闭环，无桩无模拟）
// 覆盖：P0 单一口径（剩余=预算−已执行−冻结、只读）/ P1 执行回写（费用记账回写）/
//       P1 超支预警（>100%）/ 冻结占用 / 执行流水 / 概览一致性
//
// 数据隔离：测试数据使用财政年度 2027 + 自建部门（9001/9002），避免与并行会话的 2026 数据互扰
const BASE = 'http://localhost:5655'
const FISCAL_YEAR = 2027
const DEPT_CFO = '9001'   // 财务部
const DEPT_MKT = '9002'   // 市场部
const SUBJECT_MGMT = { code: '6602', name: '管理费用' }
const SUBJECT_DEPR = { code: '6604', name: '折旧费' }

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}
const num = (v) => Number(v || 0)
const money = (v) => num(v).toFixed(2)

let H = null
function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function login() {
  const capRes = await fetch(`${BASE}/api/auth/captcha`).then(r => r.json())
  const capData = capRes.data || capRes
  const captcha = extractCaptcha(capData.img)
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123', tenantName: '系统租户', captcha, captchaKey: capData.uuid }),
  }).then(r => r.json())
  const d = res?.data || {}
  const token = d.token || d.accessToken || d.tokenValue
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  H = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', tenantId: String(d.tenantId || 1) }
}

// 并行会话共用 admin（sa-token is-concurrent=false）会被互踢，401 时自愈重登
async function call(method, path, { params, body } = {}, retry = true) {
  const qs = params ? '?' + new URLSearchParams(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '').map(([k, v]) => [k, String(v)])
  ).toString() : ''
  const res = await fetch(`${BASE}/api${path}${qs}`, {
    method,
    headers: H,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  let json = null
  try { json = await res.json() } catch { json = null }
  if (retry && (res.status === 401 || json?.code === 401)) {
    await login()
    return call(method, path, { params, body }, false)
  }
  if (json && json.success === false) {
    throw new Error(`${method} ${path} 失败: ${json.message || JSON.stringify(json).slice(0, 200)}`)
  }
  return json?.data ?? json
}
const get = (path, params) => call('GET', path, { params })
const post = (path, body, params) => call('POST', path, { body, params })

// ── 测试数据构造（经费/支出单据走真实接口，确保回写闭环） ──
async function ensureBudget({ fiscalYear, deptId, deptName, items, description }) {
  const created = await post('/erp/budget/annual', {
    fiscalYear, departmentId: deptId, departmentName: deptName, description,
    items: items.map((it, i) => ({
      subjectCode: it.code, subjectName: it.name, budgetAmount: it.amount, sortOrder: i + 1, lineNo: i + 1,
    })),
  })
  await post(`/erp/budget/annual/${created.id}/submit`)
  await post(`/erp/budget/annual/${created.id}/approve`)
  await post(`/erp/budget/annual/${created.id}/start-exec`)
  return created
}

async function createExpense({ docDate, deptId, deptName, lines, summary }) {
  const total = lines.reduce((s, l) => s + l.amount, 0)
  const doc = await post('/erp/finance/expense-doc/create', {
    docDate, expenseType: 0, deptId, deptName, summary,
    creatorName: 'admin',
    payAccountId: 1, payAccountName: '基本户-招行', payAmount: total,
    items: lines.map((l, i) => ({
      lineNo: i + 1,
      expenseCode: l.code, expenseName: l.name, subjectCode: l.code, subjectName: l.name,
      amount: l.amount,
    })),
  })
  return doc
}

const confirmExpense = (id) => post('/erp/finance/expense-doc/confirm', null, { id, operatorId: 1, operatorName: 'admin' })

async function main() {
  await login()
  console.log(`登录成功 · 验收年度 ${FISCAL_YEAR}`)

  // ═══ 0. 部门夹具 + 历史测试数据硬清理（保证脚本可重复执行、口径唯一） ═══
  const { execFileSync } = require('child_process')
  const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe'
  const env = { ...process.env, PGPASSWORD: 'devuser123' }
  execFileSync(PSQL, ['-h', 'localhost', '-p', '5432', '-U', 'devuser', '-d', 'devdb', '-q', '-c', `
    INSERT INTO sys_department (id, tenant_id, parent_id, deleted, dept_code, dept_name, sort, status, version)
    VALUES (9001,1,0,0,'D9001','财务部',1,0,0),(9002,1,0,0,'D9002','市场部',2,0,0)
    ON CONFLICT (id) DO NOTHING;
    DELETE FROM budget_execution_log WHERE budget_id IN (SELECT id FROM annual_budget WHERE fiscal_year = ${FISCAL_YEAR});
    DELETE FROM budget_item WHERE budget_id IN (SELECT id FROM annual_budget WHERE fiscal_year = ${FISCAL_YEAR});
    DELETE FROM annual_budget WHERE fiscal_year = ${FISCAL_YEAR};
    DELETE FROM erp_expense_item WHERE expense_doc_id IN (SELECT id FROM erp_expense_doc WHERE dept_id IN (9001,9002));
    DELETE FROM erp_expense_doc WHERE dept_id IN (9001,9002);
  `], { env, stdio: 'ignore' })
  console.log('\n[0] 部门夹具就绪 + 历史测试数据已硬清理（财务部 9001 / 市场部 9002）')

  // ═══ 1. 预算编制 → 审批 → 转入执行 ═══
  console.log('\n[1] 预算计划转入执行（P1 预算来源）')
  const budgetCfo = await ensureBudget({
    fiscalYear: FISCAL_YEAR, deptId: DEPT_CFO, deptName: '财务部', description: 'BUD-TEST 财务部年度预算',
    items: [
      { code: SUBJECT_MGMT.code, name: SUBJECT_MGMT.name, amount: 100000 },
      { code: SUBJECT_DEPR.code, name: SUBJECT_DEPR.name, amount: 20000 },
    ],
  })
  const budgetMkt = await ensureBudget({
    fiscalYear: FISCAL_YEAR, deptId: DEPT_MKT, deptName: '市场部', description: 'BUD-TEST 市场部年度预算',
    items: [{ code: SUBJECT_MGMT.code, name: SUBJECT_MGMT.name, amount: 30000 }],
  })
  const cfoDetail = await get(`/erp/budget/annual/${budgetCfo.id}`)
  check('预算已审批并转入执行', cfoDetail.status === 'executing', `status=${cfoDetail.status}`)
  check('预算科目明细 2 行', (cfoDetail.items || []).length === 2, `items=${(cfoDetail.items || []).length}`)
  check('初始已执行=0', num(cfoDetail.totalUsedAmount) === 0 && num(cfoDetail.executionRate) === 0)

  // ═══ 2. 费用单记账 → 回写预算执行（P1 执行回写） ═══
  console.log('\n[2] 费用单记账回写《预算执行》')
  const cases = [
    { dept: [DEPT_CFO, '财务部'], lines: [{ ...SUBJECT_MGMT, amount: 12000 }], label: '财务部/管理费用 12000（正常）' },
    { dept: [DEPT_CFO, '财务部'], lines: [{ ...SUBJECT_DEPR, amount: 25000 }], label: '财务部/折旧费 25000（超支）' },
    { dept: [DEPT_CFO, '财务部'], lines: [{ ...SUBJECT_MGMT, amount: 83000 }], label: '财务部/管理费用 83000（累计 95%）' },
    { dept: [DEPT_CFO, '财务部'], lines: [{ ...SUBJECT_DEPR, amount: 5000 }], label: '财务部/折旧费 5000（累计 150%）' },
    { dept: [DEPT_MKT, '市场部'], lines: [{ ...SUBJECT_MGMT, amount: 10000 }], label: '市场部/管理费用 10000（正常）' },
  ]
  for (const c of cases) {
    const doc = await createExpense({
      docDate: `${FISCAL_YEAR}-09-10`, deptId: c.dept[0], deptName: c.dept[1],
      lines: c.lines, summary: '预算执行验收 ' + c.label,
    })
    await confirmExpense(doc.id)
    c.docNo = doc.docNo
    c.docId = doc.id
  }
  check('5 张费用单全部记账成功', cases.every(c => !!c.docNo), cases.map(c => c.docNo).join(' '))

  // ═══ 3. 按预算科目明细核对（P0 恒等式） ═══
  console.log('\n[3] 预算科目明细口径（剩余 = 预算 − 已执行 − 冻结）')
  const cfoItems = await get('/erp/budget/execution/items', { fiscalYear: FISCAL_YEAR, departmentId: DEPT_CFO, size: 50 })
  const byCode = Object.fromEntries((cfoItems.records || []).map(r => [r.subjectCode, r]))
  check('财务部预算科目 2 行', (cfoItems.records || []).length === 2, `rows=${(cfoItems.records || []).length}`)

  const mgmt = byCode[SUBJECT_MGMT.code]
  check('管理费用 预算 100000 / 已执行 95000 / 剩余 5000',
    num(mgmt?.budgetAmount) === 100000 && num(mgmt?.usedAmount) === 95000 && num(mgmt?.remainingAmount) === 5000,
    `budget=${money(mgmt?.budgetAmount)} used=${money(mgmt?.usedAmount)} remaining=${money(mgmt?.remainingAmount)}`)
  check('管理费用 执行进度 95%', num(mgmt?.executionRate) === 95, `rate=${mgmt?.executionRate}`)
  check('管理费用 预警级别 = warn（≥90%）', mgmt?.warnLevel === 'warn' && mgmt?.overBudget === false, `warnLevel=${mgmt?.warnLevel}`)

  const depr = byCode[SUBJECT_DEPR.code]
  check('折旧费 预算 20000 / 已执行 30000 / 剩余 -10000（超支）',
    num(depr?.budgetAmount) === 20000 && num(depr?.usedAmount) === 30000 && num(depr?.remainingAmount) === -10000,
    `budget=${money(depr?.budgetAmount)} used=${money(depr?.usedAmount)} remaining=${money(depr?.remainingAmount)}`)
  check('折旧费 执行进度 150% / 超支金额 10000',
    num(depr?.executionRate) === 150 && num(depr?.overAmount) === 10000,
    `rate=${depr?.executionRate} overAmount=${money(depr?.overAmount)}`)
  check('折旧费 预警级别 = over（P1 超支预警）', depr?.warnLevel === 'over' && depr?.overBudget === true, `warnLevel=${depr?.warnLevel}`)

  const invariantOk = (cfoItems.records || []).every(r =>
    Math.abs(num(r.budgetAmount) - num(r.usedAmount) - num(r.frozenAmount) - num(r.remainingAmount)) < 0.001)
  check('全部科目满足 剩余 = 预算 − 已执行 − 冻结', invariantOk)

  // ═══ 4. 按预算单维度汇总 ═══
  console.log('\n[4] 按预算单维度（汇总口径与明细一致）')
  const rows = await get('/erp/budget/execution/rows', { fiscalYear: FISCAL_YEAR, size: 50 })
  const cfoRow = (rows.records || []).find(r => r.departmentId === DEPT_CFO)
  check('财务部预算单 预算 120000 / 已执行 125000 / 剩余 -5000',
    num(cfoRow?.totalAmount) === 120000 && num(cfoRow?.usedAmount) === 125000 && num(cfoRow?.remainingAmount) === -5000,
    `budget=${money(cfoRow?.totalAmount)} used=${money(cfoRow?.usedAmount)} remaining=${money(cfoRow?.remainingAmount)}`)
  check('财务部预算单 执行进度 104.17% / 超支',
    num(cfoRow?.executionRate) === 104.17 && cfoRow?.overBudget === true, `rate=${cfoRow?.executionRate}`)
  check('财务部预算单 超支科目数 1', num(cfoRow?.overItemCount) === 1, `overItemCount=${cfoRow?.overItemCount}`)

  const mktRow = (rows.records || []).find(r => r.departmentId === DEPT_MKT)
  check('市场部预算单 预算 30000 / 已执行 10000 / 进度 33.33%',
    num(mktRow?.totalAmount) === 30000 && num(mktRow?.usedAmount) === 10000 && num(mktRow?.executionRate) === 33.33,
    `budget=${money(mktRow?.totalAmount)} used=${money(mktRow?.usedAmount)} rate=${mktRow?.executionRate}`)
  check('市场部预算单 无预警', mktRow?.warnLevel === 'normal' && mktRow?.overBudget === false, `warnLevel=${mktRow?.warnLevel}`)

  // ═══ 5. 概览卡片口径（隔离年度 2027） ═══
  console.log('\n[5] 执行概览（年度隔离口径）')
  const sum = await get('/erp/budget/report/execution-summary', { fiscalYear: FISCAL_YEAR })
  check('预算总额 150000', num(sum?.totalBudgetAmount) === 150000, money(sum?.totalBudgetAmount))
  check('已执行 135000', num(sum?.totalUsedAmount) === 135000, money(sum?.totalUsedAmount))
  check('冻结金额 0（尚未冻结）', num(sum?.totalFrozenAmount) === 0, money(sum?.totalFrozenAmount))
  check('剩余额度 15000 = 预算 − 已执行 − 冻结', num(sum?.totalRemainingAmount) === 15000, money(sum?.totalRemainingAmount))
  check('执行进度 90.00%', num(sum?.executionRate) === 90, `${sum?.executionRate}`)
  check('超支科目 1 / 预警科目 1', num(sum?.overBudgetCount) === 1 && num(sum?.warningCount) === 1,
    `over=${sum?.overBudgetCount} warn=${sum?.warningCount}`)
  check('已转入执行预算单 2 份', num(sum?.approvedCount) === 2, `approvedCount=${sum?.approvedCount}`)

  // ═══ 6. 超支预警清单 ═══
  console.log('\n[6] 超支预警清单（P1 超支预警）')
  const warn = await get('/erp/budget/execution/warnings', { fiscalYear: FISCAL_YEAR })
  check('超支清单 1 项（折旧费）', num(warn?.overBudgetCount) === 1
    && warn?.overBudgetItems?.[0]?.subjectCode === SUBJECT_DEPR.code, JSON.stringify(warn?.overBudgetItems?.[0]?.subjectCode))
  check('超支金额合计 10000', num(warn?.overBudgetAmount) === 10000, money(warn?.overBudgetAmount))
  check('预警清单 1 项（管理费用 95%）', num(warn?.warningCount) === 1
    && warn?.warningItems?.[0]?.subjectCode === SUBJECT_MGMT.code, JSON.stringify(warn?.warningItems?.[0]?.subjectCode))

  // ═══ 7. 执行流水（来源单据可追溯） ═══
  console.log('\n[7] 执行流水（费用单来源可追溯）')
  const logs = await get('/erp/budget/execution/logs', { budgetItemId: mgmt?.id })
  check('管理费用流水 2 笔（12000 + 83000）', (logs || []).length === 2, `logs=${(logs || []).length}`)
  check('流水类型均为「消耗」', (logs || []).every(l => l.executionType === 'consume' && l.executionTypeName === '消耗'))
  check('流水来源为费用单且带单号',
    (logs || []).every(l => l.sourceType === 'expense' && l.sourceTypeName === '费用单' && !!l.sourceNo),
    (logs || []).map(l => l.sourceNo).join(','))
  check('流水单号与费用单一致',
    cases.filter(c => c.lines[0].code === SUBJECT_MGMT.code && c.dept[0] === DEPT_CFO)
      .every(c => (logs || []).some(l => l.sourceNo === c.docNo)))

  // ═══ 8. 冻结占用（冻结 = 占用待复核） ═══
  console.log('\n[8] 冻结/释放闭环')
  const mktItems = await get('/erp/budget/execution/items', { fiscalYear: FISCAL_YEAR, departmentId: DEPT_MKT, size: 50 })
  const mktItemId = mktItems.records[0].id
  await post('/erp/budget/control/freeze', null,
    { budgetId: budgetMkt.id, budgetItemId: mktItemId, amount: 5000, sourceType: 'manual', sourceNo: 'FREEZE-TEST', sourceId: 1 })
  const afterFreeze = await get('/erp/budget/execution/items', { fiscalYear: FISCAL_YEAR, departmentId: DEPT_MKT, size: 50 })
  const mktItem = afterFreeze.records[0]
  check('冻结 5000 后：冻结=5000 / 剩余=15000',
    num(mktItem.frozenAmount) === 5000 && num(mktItem.remainingAmount) === 15000,
    `frozen=${money(mktItem.frozenAmount)} remaining=${money(mktItem.remainingAmount)}`)
  check('冻结后恒等式仍成立（剩余 = 预算 − 已执行 − 冻结）',
    Math.abs(num(mktItem.budgetAmount) - num(mktItem.usedAmount) - num(mktItem.frozenAmount) - num(mktItem.remainingAmount)) < 0.001)
  const sumAfterFreeze = await get('/erp/budget/report/execution-summary', { fiscalYear: FISCAL_YEAR })
  check('概览冻结金额同步为 5000 / 剩余 10000',
    num(sumAfterFreeze?.totalFrozenAmount) === 5000 && num(sumAfterFreeze?.totalRemainingAmount) === 10000,
    `frozen=${money(sumAfterFreeze?.totalFrozenAmount)} remaining=${money(sumAfterFreeze?.totalRemainingAmount)}`)

  await post('/erp/budget/control/release', null,
    { budgetId: budgetMkt.id, budgetItemId: mktItem.id, amount: 5000, sourceType: 'manual', sourceNo: 'FREEZE-TEST', sourceId: 1 })
  const afterRelease = await get('/erp/budget/execution/items', { fiscalYear: FISCAL_YEAR, departmentId: DEPT_MKT, size: 50 })
  check('释放后冻结归零 / 剩余恢复 20000',
    num(afterRelease.records[0].frozenAmount) === 0 && num(afterRelease.records[0].remainingAmount) === 20000,
    `frozen=${money(afterRelease.records[0].frozenAmount)} remaining=${money(afterRelease.records[0].remainingAmount)}`)

  // ═══ 9. 只读校验（P0 红线：严禁直改已执行/剩余） ═══
  console.log('\n[9] P0 只读校验')
  const writeAttempt = await fetch(`${BASE}/api/erp/budget/execution/items`, { method: 'POST', headers: H, body: '{}' })
  check('预算执行接口无写入入口（POST /items 不被接受）', writeAttempt.status === 404 || writeAttempt.status === 405,
    `status=${writeAttempt.status}`)

  console.log(`\n════ 结果：通过 ${pass} 项，失败 ${fail} 项 ════`)
  process.exit(fail === 0 ? 0 : 1)
}

main().catch(e => { console.error('验收异常:', e.message); process.exit(1) })
