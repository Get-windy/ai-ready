/**
 * 费用统计金标准 端到端验证（独立 headless 浏览器，避开 MCP 浏览器占用）
 *
 * 设计原则：**期望值全部由实时数据推导**（明细行 / 审批工作台 / 分组接口），
 * 不硬编码任何绝对数字，避免共享 dev 库被其他会话写入后出现假失败。
 * 校验的是「口径自洽」：卡片 ⇄ 明细 ⇄ 分组 ⇄ 审批 ⇄ 页面 UI 五者必须一致。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')

const FE = 'http://localhost:5656'
const SHOTS = 'I:/AI-Ready/tool-results/expense-stats'
/** 覆盖全部业务数据的查询区间（费用单单据日期） */
const RANGE = 'dateStart=2026-01-01&dateEnd=2026-12-31'

let pass = 0
let fail = 0
function check(name, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected)
  if (ok) { pass++; console.log('  ✔', name, '=', actual) }
  else { fail++; console.log('  ✘', name, ' actual=', JSON.stringify(actual), ' expected=', JSON.stringify(expected)) }
}
function checkTrue(name, cond, detail) {
  if (cond) { pass++; console.log('  ✔', name, detail !== undefined ? '-> ' + detail : '') }
  else { fail++; console.log('  ✘', name, detail !== undefined ? '-> ' + detail : '') }
}
const num = (v) => Number(v ?? 0)
const money = (v) => num(v).toFixed(2)
/** 页面卡片千分位格式 */
const cardMoney = (v) => num(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const sum = (rows, fn) => rows.reduce((a, r) => a + fn(r), 0)

function apiReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: 5655, path: '/api' + path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      let s = ''
      res.on('data', c => { s += c })
      res.on('end', () => {
        try { resolve({ status: res.statusCode, body: JSON.parse(s) }) }
        catch { resolve({ status: res.statusCode, body: s }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

/** 后端可能被并行会话重启，登录前重试等待其就绪 */
async function waitBackend(attempts = 12, intervalMs = 5000) {
  for (let i = 0; i < attempts; i++) {
    try {
      const r = await apiReq('GET', '/auth/captcha')
      if (r.status === 200 && r.body?.data?.img) return true
    } catch { /* retry */ }
    console.log(`  等待后端就绪… (${i + 1}/${attempts})`)
    await new Promise(res => setTimeout(res, intervalMs))
  }
  return false
}

async function login(token) {
  if (token) {
    // 支持外部传入 token（并行会话 sa-token 互踢时用）
    const t = await apiReq('GET', '/erp/finance/expense-stats/summary?pageNum=1', null, token)
    if (t.status === 200) return { token, userInfo: { tenantId: 1 } }
  }
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.body.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.body.data.uuid,
  })
  const tk = res.body?.data?.token || res.body?.data?.accessToken
  if (!tk) throw new Error('登录失败: ' + JSON.stringify(res.body).slice(0, 300))
  return { token: tk, userInfo: res.body.data }
}

/** 拉取全量费用明细（与统计同口径：page-detail 复用同一过滤，pageSize 放大到 500） */
async function fetchDetailRows(token, extra = '') {
  const res = await apiReq('GET',
    `/erp/finance/expense-doc/page-detail?pageNum=1&pageSize=500&${RANGE}&showRed=false${extra}`, null, token)
  return { rows: res.body?.data?.records || [], total: num(res.body?.data?.total), status: res.status }
}

async function main() {
  if (!await waitBackend()) throw new Error('后端 5655 未就绪（可能被并行会话重启中，请稍后重试）')
  const { token, userInfo } = await login(process.env.EXPENSE_STATS_TOKEN)
  console.log('登录成功')

  // ════════════════ 0. 基准：从明细行推导全部期望值 ════════════════
  console.log('\n=== 0. 基准数据（由费用明细实时推导） ===')
  const { rows: detailRows, total: detailTotal } = await fetchDetailRows(token)
  const base = {
    itemCount: detailTotal,
    expenseCount: new Set(detailRows.map(r => String(r.expenseDocId))).size,
    totalAmount: sum(detailRows, r => num(r.amount)),
    approvedAmount: sum(detailRows.filter(r => String(r.approvalStatus) === '2'), r => num(r.amount)),
    pendingAmount: sum(detailRows.filter(r => String(r.approvalStatus) === '1'), r => num(r.amount)),
    rejectedAmount: sum(detailRows.filter(r => String(r.approvalStatus) === '3'), r => num(r.amount)),
    paidAmount: sum(detailRows.filter(r => String(r.status) === '1'), r => num(r.amount)),
    unpaidAmount: sum(detailRows.filter(r => String(r.status) === '0'), r => num(r.amount)),
    partnerAmount: sum(detailRows.filter(r => String(r.expenseType) === '0'), r => num(r.amount)),
    internalAmount: sum(detailRows.filter(r => String(r.expenseType) === '1'), r => num(r.amount)),
    approvedDocCount: new Set(detailRows.filter(r => String(r.approvalStatus) === '2').map(r => String(r.expenseDocId))).size,
    pendingDocCount: new Set(detailRows.filter(r => String(r.approvalStatus) === '1').map(r => String(r.expenseDocId))).size,
    rejectedDocCount: new Set(detailRows.filter(r => String(r.approvalStatus) === '3').map(r => String(r.expenseDocId))).size,
  }
  console.log('  基准:', JSON.stringify({
    ...base,
    totalAmount: money(base.totalAmount), approvedAmount: money(base.approvedAmount),
    pendingAmount: money(base.pendingAmount), rejectedAmount: money(base.rejectedAmount),
    paidAmount: money(base.paidAmount), unpaidAmount: money(base.unpaidAmount),
  }))
  checkTrue('基准数据非空（库内有费用单）', base.expenseCount > 0 && base.itemCount > 0,
    `${base.expenseCount} 单 / ${base.itemCount} 笔`)

  // ════════════════ 1. /summary 与明细逐项一致 ════════════════
  console.log('\n=== 1. 统计汇总 /summary（期望=明细推导） ===')
  const summaryRes = await apiReq('GET', `/erp/finance/expense-stats/summary?${RANGE}`, null, token)
  check('HTTP 状态', summaryRes.status, 200)
  const s = summaryRes.body.data || {}
  check('单据数', num(s.expenseCount), base.expenseCount)
  check('费用笔数', num(s.itemCount), base.itemCount)
  check('费用总额', money(s.totalAmount), money(base.totalAmount))
  check('已审批金额', money(s.approvedAmount), money(base.approvedAmount))
  check('待审批金额', money(s.pendingAmount), money(base.pendingAmount))
  check('已拒绝金额', money(s.rejectedAmount), money(base.rejectedAmount))
  check('已记账(实付)', money(s.paidAmount), money(base.paidAmount))
  check('未记账(待付)', money(s.unpaidAmount), money(base.unpaidAmount))
  check('往来单位费用', money(s.partnerAmount), money(base.partnerAmount))
  check('内部费用', money(s.internalAmount), money(base.internalAmount))
  const expectAvg = base.expenseCount > 0
    ? (base.totalAmount / base.expenseCount).toFixed(2) : '0.00'
  check('平均单额 = 总额/单数', money(s.averageAmount), expectAvg)
  checkTrue('四状态之和 = 费用总额',
    money(num(s.approvedAmount) + num(s.pendingAmount) + num(s.rejectedAmount) + num(s.draftAmount)) === money(base.totalAmount),
    money(num(s.approvedAmount) + num(s.pendingAmount) + num(s.rejectedAmount) + num(s.draftAmount)))
  checkTrue('已记账+未记账 = 费用总额',
    money(num(s.paidAmount) + num(s.unpaidAmount)) === money(base.totalAmount),
    money(num(s.paidAmount) + num(s.unpaidAmount)))
  checkTrue('往来单位+内部 = 费用总额',
    money(num(s.partnerAmount) + num(s.internalAmount)) === money(base.totalAmount),
    `往来=${money(s.partnerAmount)} 内部=${money(s.internalAmount)}`)
  const byType = s.byType || {}
  const byDept = s.byDepartment || {}
  checkTrue('byType 分组映射非空', Object.keys(byType).length > 0, JSON.stringify(byType))
  checkTrue('byDepartment 分组映射非空', Object.keys(byDept).length > 0, JSON.stringify(byDept))
  checkTrue('byType 金额合计 = 费用总额',
    money(sum(Object.values(byType), v => num(v))) === money(base.totalAmount),
    money(sum(Object.values(byType), v => num(v))))
  checkTrue('byDepartment 金额合计 = 费用总额',
    money(sum(Object.values(byDept), v => num(v))) === money(base.totalAmount),
    money(sum(Object.values(byDept), v => num(v))))
  const trend = s.monthlyTrend || []
  checkTrue('月度趋势非空且合计 = 费用总额',
    trend.length > 0 && money(sum(trend, t => num(t.totalAmount))) === money(base.totalAmount),
    trend.map(t => `${t.month}:${money(t.totalAmount)}`).join(' , '))
  check('趋势单据数合计', sum(trend, t => num(t.docCount)), base.expenseCount)

  // ════════════════ 2. 分组明细口径 ════════════════
  console.log('\n=== 2. 分组明细 /by-department /by-type ===')
  const deptRes = await apiReq('GET', `/erp/finance/expense-stats/by-department?${RANGE}`, null, token)
  const deptRows = deptRes.body.data || []
  checkTrue('部门分组行数 = 主数据分组数（非空）', deptRows.length > 0, deptRows.length + ' 行')
  check('部门合计金额', money(sum(deptRows, r => num(r.totalAmount))), money(base.totalAmount))
  check('部门合计笔数', sum(deptRows, r => num(r.itemCount)), base.itemCount)
  check('部门合计单据数', sum(deptRows, r => num(r.docCount)), base.expenseCount)
  checkTrue('部门按金额降序', deptRows.every((r, i) => i === 0 || num(deptRows[i - 1].totalAmount) >= num(r.totalAmount)),
    deptRows.map(r => `${r.groupKey}:${money(r.totalAmount)}`).join(' , '))
  checkTrue('部门占比合计≈100', Math.abs(sum(deptRows, r => num(r.ratio)) - 100) < 0.1,
    deptRows.map(r => r.ratio).join(' + '))
  checkTrue('部门行占比 = 金额/总额×100（2位）',
    deptRows.every(r => num(r.ratio).toFixed(2) === (num(r.totalAmount) / base.totalAmount * 100).toFixed(2)),
    deptRows.map(r => `${r.groupKey}:${r.ratio}`).join(' , '))
  checkTrue('部门行含单均金额', deptRows.every(r => r.avgAmount !== undefined && r.avgAmount !== null),
    deptRows.map(r => r.avgAmount).join(','))

  const typeRes = await apiReq('GET', `/erp/finance/expense-stats/by-type?${RANGE}`, null, token)
  const typeRows = typeRes.body.data || []
  checkTrue('费用类型分组行数 > 0', typeRows.length > 0, typeRows.length + ' 行')
  check('类型合计金额', money(sum(typeRows, r => num(r.totalAmount))), money(base.totalAmount))
  check('类型合计笔数', sum(typeRows, r => num(r.itemCount)), base.itemCount)
  checkTrue('类型按金额降序', typeRows.every((r, i) => i === 0 || num(typeRows[i - 1].totalAmount) >= num(r.totalAmount)),
    typeRows.map(r => `${r.groupKey}:${money(r.totalAmount)}`).join(' , '))
  checkTrue('类型占比合计≈100', Math.abs(sum(typeRows, r => num(r.ratio)) - 100) < 0.1,
    typeRows.map(r => r.ratio).join(' + '))
  checkTrue('类型行金额 = 明细按费用名称聚合值',
    typeRows.every(r => money(r.totalAmount) === money(sum(detailRows.filter(d => (d.expenseName || '未指定费用项') === r.groupKey), d => num(d.amount)))),
    JSON.stringify(typeRows.map(r => [r.groupKey, money(r.totalAmount)])))

  // ════════════════ 3. 过滤条件（与明细同条件对比） ════════════════
  console.log('\n=== 3. 查询条件过滤口径 ===')
  for (const st of ['1', '2', '3']) {
    const r = await apiReq('GET', `/erp/finance/expense-stats/summary?${RANGE}&approvalStatus=${st}`, null, token)
    const d = r.body.data || {}
    const rows = detailRows.filter(x => String(x.approvalStatus) === st)
    check(`审批状态=${st}：单据数`, num(d.expenseCount), new Set(rows.map(x => String(x.expenseDocId))).size)
    check(`审批状态=${st}：总额`, money(d.totalAmount), money(sum(rows, x => num(x.amount))))
  }
  // 费用名称过滤（取实时第一个费用类型名，避免硬编码业务数据）
  const sampleName = typeRows[0].groupKey
  const nameRes = await apiReq('GET', `/erp/finance/expense-stats/summary?${RANGE}&expenseName=${encodeURIComponent(sampleName)}`, null, token)
  const nameRows = detailRows.filter(d => (d.expenseName || '').includes(sampleName))
  check(`费用名称=${sampleName}：总额`, money(nameRes.body.data.totalAmount), money(sum(nameRows, x => num(x.amount))))
  check(`费用名称=${sampleName}：单据数`, num(nameRes.body.data.expenseCount), new Set(nameRows.map(x => String(x.expenseDocId))).size)
  // 部门过滤（取实时第一个部门名）
  const sampleDept = deptRows[0].groupKey
  if (sampleDept !== '未指定部门') {
    const deptF = await apiReq('GET', `/erp/finance/expense-stats/summary?${RANGE}&deptName=${encodeURIComponent(sampleDept)}`, null, token)
    check(`部门=${sampleDept}：总额与分组行一致`, money(deptF.body.data.totalAmount), money(deptRows[0].totalAmount))
  }
  // 费用类别过滤
  const typeF = await apiReq('GET', `/erp/finance/expense-stats/summary?${RANGE}&expenseType=1`, null, token)
  check('费用类别=内部费用：总额', money(typeF.body.data.totalAmount), money(base.internalAmount))
  // 无数据区间
  const emptyRes = await apiReq('GET', `/erp/finance/expense-stats/summary?dateStart=2030-01-01&dateEnd=2030-12-31`, null, token)
  check('无数据区间：单据数', num(emptyRes.body.data.expenseCount), 0)
  check('无数据区间：总额', money(emptyRes.body.data.totalAmount), '0.00')
  check('无数据区间：平均单额', money(emptyRes.body.data.averageAmount), '0.00')

  // ════════════════ 4. 与《费用审批》工作台口径一致（P1 红线） ════════════════
  console.log('\n=== 4. 与费用审批口径一致性 ===')
  for (const st of ['1', '2']) {
    const appr = await apiReq('GET', `/erp/finance/expense-approval/pending?pageNum=1&pageSize=500&approvalStatus=${st}&onlyMine=false`, null, token)
    const rows = appr.body.data?.records || []
    const label = st === '1' ? '审批中' : '审批通过'
    check(`审批工作台 ${label} 单据数 = 统计单据数`,
      num(appr.body.data?.total),
      st === '1' ? base.pendingDocCount : base.approvedDocCount)
    check(`审批工作台 ${label} 金额合计 = 统计金额`,
      money(sum(rows, r => num(r.totalAmount))),
      money(st === '1' ? base.pendingAmount : base.approvedAmount))
  }

  // ════════════════ 5. 下钻明细（复用费用单 page-detail） ════════════════
  console.log('\n=== 5. 下钻明细 /erp/finance/expense-doc/page-detail ===')
  check('明细总条数 = 费用笔数', detailTotal, base.itemCount)
  check('明细金额合计 = 费用总额', money(sum(detailRows, r => num(r.amount))), money(base.totalAmount))
  checkTrue('明细行含审批状态字段', detailRows.every(r => r.approvalStatus !== undefined && r.approvalStatus !== null),
    'approvalStatus 示例=' + detailRows[0]?.approvalStatus)
  const dApproved = await fetchDetailRows(token, '&approvalStatus=2')
  check('下钻=审批通过：条数 = 统计费用笔数', dApproved.total,
    num((await apiReq('GET', `/erp/finance/expense-stats/summary?${RANGE}&approvalStatus=2`, null, token)).body.data.itemCount))
  check('下钻=审批通过：金额合计', money(sum(dApproved.rows, r => num(r.amount))), money(base.approvedAmount))
  const dInternal = await fetchDetailRows(token, '&expenseType=1')
  checkTrue('下钻=内部费用：仅返回内部费用单据',
    dInternal.rows.every(r => String(r.expenseType) === '1'),
    '条数=' + dInternal.total)

  // ════════════════ 6. 浏览器端到端 ════════════════
  console.log('\n=== 6. 浏览器端到端 ===')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 1000 } })
  const page = await ctx.newPage()
  const httpErrors = []
  page.on('response', r => { if (r.status() >= 400) httpErrors.push(`[HTTP ${r.status()}] ` + r.url().replace(FE, '')) })
  page.on('pageerror', e => httpErrors.push('[pageerror] ' + e.message))
  page.on('console', m => { if (m.type() === 'error') httpErrors.push('[console.error] ' + m.text().slice(0, 200)) })

  await page.addInitScript(({ token, userInfo }) => {
    localStorage.setItem('token', token)
    localStorage.setItem('tenantId', String(userInfo?.tenantId || 1))
    localStorage.setItem('tenantName', userInfo?.tenantName || '系统租户')
  }, { token, userInfo })

  const readRows = `[...document.querySelectorAll('.table-area tbody tr')]
    .map(tr => [...tr.querySelectorAll('td')].map(td => (td.textContent || '').trim()))
    .filter(cells => cells.slice(1).some(v => v !== '' && v !== '-'))`

  /** DOM 稳定等待：连续两次快照一致（表头数 + 真实行数），规避整页重载中途读数 */
  const waitStable = async () => {
    let prev = ''
    for (let i = 0; i < 20; i++) {
      const snap = await page.evaluate(([expr]) => {
        const rows = eval(expr)
        return document.querySelectorAll('.table-area thead th').length + '|' + rows.length
      }, [readRows]).catch(() => '0|0')
      if (snap !== '0|0' && snap === prev) return snap
      prev = snap
      await page.waitForTimeout(500)
    }
    return prev
  }

  /** 并行会话改共享文件会触发 Vite 整页重载 → 表头丢失时自动重新导航（最多 3 次） */
  const ensurePageAlive = async () => {
    for (let i = 0; i < 3; i++) {
      const thCount = await page.evaluate(() => document.querySelectorAll('.table-area thead th').length).catch(() => 0)
      if (thCount > 0) return true
      console.log('  [自愈] 页面被整页重载，重新导航…')
      await page.goto(FE + '/finance/expense-stats', { waitUntil: 'domcontentloaded', timeout: 60000 })
      await page.waitForSelector('.table-area thead th', { timeout: 60000 }).catch(() => {})
      await page.waitForTimeout(2500)
    }
    return false
  }

  await page.goto(FE + '/finance/expense-stats', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForSelector('.table-area tbody tr', { timeout: 60000 })
  await page.waitForTimeout(2500)
  await waitStable()

  // 浏览器阶段前刷新基准（共享库可能刚被并行会话写入）
  const liveDept = (await apiReq('GET', `/erp/finance/expense-stats/by-department?${RANGE}`, null, token)).body.data || []
  const liveType = (await apiReq('GET', `/erp/finance/expense-stats/by-type?${RANGE}`, null, token)).body.data || []
  const liveDetail = await fetchDetailRows(token)

  const structure = await page.evaluate(([expr]) => {
    const rows = eval(expr)
    return {
      url: location.pathname,
      tabs: [...document.querySelectorAll('.tab-item')].map(e => (e.textContent || '').trim()),
      cards: [...document.querySelectorAll('.ar-stat-card')].map(c => ({
        label: (c.querySelector('.ar-stat-card__label') || {}).textContent?.trim(),
        value: (c.querySelector('.ar-stat-card__value') || {}).textContent?.trim(),
      })),
      charts: [...document.querySelectorAll('.ar-report-chart')].map(c => ({
        title: (c.querySelector('.ar-report-chart__title') || {}).textContent?.trim(),
        hasCanvas: !!c.querySelector('canvas'),
      })),
      headers: [...document.querySelectorAll('.table-area thead th')].map(th => (th.textContent || '').trim()).filter(Boolean),
      rowCount: rows.length,
      rows: rows.slice(0, 5),
      hasGear: !!document.querySelector('.th-settings-btn'),
      toolbar: (document.querySelector('.toolbar-section') || document.body).textContent || '',
      footer: (document.querySelector('.table-footer') || {}).textContent?.replace(/\s+/g, ' ').trim(),
      meta: (document.querySelector('.stats-meta') || {}).textContent?.replace(/\s+/g, ' ').trim(),
      queryScheme: !!document.querySelector('.query-scheme-wrap'),
      quickDateActive: (document.querySelector('.quick-dates .ant-btn-primary') || {}).textContent?.trim(),
    }
  }, [readRows])
  check('路由', structure.url, '/finance/expense-stats')
  check('Tab 三视图', structure.tabs, ['按部门', '按费用类型', '按明细'])
  check('统计卡片数', structure.cards.length, 6)
  const after = (await apiReq('GET', `/erp/finance/expense-stats/summary?${RANGE}`, null, token)).body.data || {}
  check('卡片-费用总额 = 接口费用总额', structure.cards[0]?.value, '¥' + cardMoney(after.totalAmount))
  check('卡片-已审批金额 = 接口已审批金额', structure.cards[1]?.value, '¥' + cardMoney(after.approvedAmount))
  check('卡片-待审批金额 = 接口待审批金额', structure.cards[2]?.value, '¥' + cardMoney(after.pendingAmount))
  check('卡片-已拒绝金额 = 接口已拒绝金额', structure.cards[3]?.value, '¥' + cardMoney(after.rejectedAmount))
  check('卡片-单据数 = 接口单据数', structure.cards[4]?.value, num(after.expenseCount) + '单')
  check('卡片-平均单额 = 接口平均单额', structure.cards[5]?.value, '¥' + cardMoney(after.averageAmount))
  check('图表数', structure.charts.length, 3)
  check('图表标题', structure.charts.map(c => c.title), ['按费用类型', '按部门', '月度趋势'])
  checkTrue('图表已渲染 canvas', structure.charts.every(c => c.hasCanvas), JSON.stringify(structure.charts.map(c => c.hasCanvas)))
  checkTrue('列设置齿轮存在（列配置唯一入口）', structure.hasGear)
  checkTrue('工具栏含 刷新/导出', structure.toolbar.includes('刷新') && structure.toolbar.includes('导出'),
    structure.toolbar.replace(/\s+/g, ' ').trim())
  checkTrue('工具栏无重复列配置按钮', !structure.toolbar.includes('列配置'))
  checkTrue('查询方案 + 快捷日期默认近三月', structure.queryScheme && structure.quickDateActive === '近三月', structure.quickDateActive)
  checkTrue('底部合计行 = 费用总额', (structure.footer || '').includes(cardMoney(after.totalAmount)), structure.footer)
  checkTrue('口径说明条（已记账/未记账）',
    (structure.meta || '').includes(cardMoney(after.paidAmount)) && (structure.meta || '').includes(cardMoney(after.unpaidAmount)),
    structure.meta)
  check('默认视图(按部门)行数 = 部门分组数', structure.rowCount, liveDept.length)
  checkTrue('部门行含占比', (structure.rows[0] || []).some(v => v.endsWith('%')), JSON.stringify(structure.rows[0]))

  await page.screenshot({ path: SHOTS + '/01-dept.png' })

  // ── 图表按钮提示：浮层在按钮下方，不遮挡按钮 ──
  await page.evaluate(() => {
    const btns = [...document.querySelectorAll('.search-action-group .ant-btn')]
    const reset = btns.find(b => (b.textContent || '').replace(/\s+/g, '') === '重置')
    if (reset) reset.click()
  })
  await page.waitForTimeout(2000)
  const chartBtnBox = await page.evaluate(() => {
    const btn = document.querySelector('.toolbar-right .ant-btn .anticon-bar-chart')?.closest('button')
    if (!btn) return null
    const r = btn.getBoundingClientRect()
    return { x: r.x, y: r.y, w: r.width, h: r.height }
  })
  if (chartBtnBox) {
    await page.mouse.move(chartBtnBox.x + chartBtnBox.w / 2, chartBtnBox.y + chartBtnBox.h / 2)
    await page.waitForTimeout(1200)
    const tip = await page.evaluate(() => {
      const el = [...document.querySelectorAll('.ant-tooltip')]
        .find(e => !e.classList.contains('ant-tooltip-hidden') && getComputedStyle(e).display !== 'none')
      if (!el) return null
      const r = el.getBoundingClientRect()
      return { text: (el.textContent || '').trim(), top: r.top, bottom: r.bottom }
    })
    console.log('\n图表按钮提示:', JSON.stringify(tip))
    checkTrue('图表按钮有悬停提示', !!tip && (tip.text === '隐藏图表' || tip.text === '显示图表'),
      tip ? tip.text : 'no tooltip')
    checkTrue('提示浮层位于按钮下方（不遮挡按钮）',
      !!tip && tip.top >= chartBtnBox.y + chartBtnBox.h - 2,
      tip ? `tip.top=${Math.round(tip.top)} vs btn.bottom=${Math.round(chartBtnBox.y + chartBtnBox.h)}` : 'n/a')
    await page.mouse.move(10, 650)
    await page.waitForTimeout(500)
  } else {
    checkTrue('找到图表按钮', false, '未找到 .anticon-bar-chart')
  }

  // ── 列配置：入口在数据表表头齿轮（工具栏无重复按钮） ──
  const toolbarHasColumnBtn = await page.evaluate(() =>
    !!document.querySelector('.toolbar-right .ant-btn .anticon-table'))
  checkTrue('工具栏不再有重复的列配置按钮', !toolbarHasColumnBtn, String(toolbarHasColumnBtn))

  await page.click('.table-area thead .th-settings-btn')
  await page.waitForTimeout(900)
  const colPanel = await page.evaluate(() => {
    const body = document.querySelector('.ant-modal-body, .ant-drawer-body')
    return {
      tabs: [...document.querySelectorAll('.ant-tabs-tab')].map(e => (e.textContent || '').trim()),
      bodyText: (body || {}).textContent?.replace(/\s+/g, ' ').slice(0, 200),
    }
  })
  console.log('\n数据表列配置弹窗（表头齿轮）:', JSON.stringify(colPanel))
  checkTrue('表头齿轮打开数据表列配置弹窗（含列清单与恢复默认）',
    (colPanel.bodyText || '').includes('恢复默认') &&
    (colPanel.bodyText || '').includes('部门') &&
    (colPanel.bodyText || '').includes('单据数'),
    colPanel.bodyText)
  checkTrue('列配置含个人/全局配置页签',
    colPanel.tabs.includes('个人配置') && colPanel.tabs.includes('全局配置'), JSON.stringify(colPanel.tabs))
  await page.screenshot({ path: SHOTS + '/05-column-config.png' })
  await page.keyboard.press('Escape')
  await page.waitForTimeout(800)

  // ── 页面配置弹窗 ──
  await page.click('.toolbar-right .ant-btn:has(.anticon-setting)')
  await page.waitForTimeout(800)
  const pagePanel = await page.evaluate(() => {
    const bodyText = (document.body.textContent || '').replace(/\s+/g, ' ')
    return {
      tabs: [...document.querySelectorAll('.ant-tabs-tab')].map(e => (e.textContent || '').trim()),
      hasApplyDate: bodyText.includes('申请日期'),
      hasApproval: bodyText.includes('审批状态'),
      hasExpenseName: bodyText.includes('费用名称'),
    }
  })
  console.log('\n页面配置弹窗:', JSON.stringify(pagePanel))
  checkTrue('页面配置含查询条件（申请日期/审批状态/费用名称）',
    pagePanel.hasApplyDate && pagePanel.hasApproval && pagePanel.hasExpenseName, JSON.stringify(pagePanel))
  await page.screenshot({ path: SHOTS + '/06-page-config.png' })
  await page.keyboard.press('Escape')
  await page.waitForTimeout(800)


  // ── 切到「按费用类型」 ──
  await ensurePageAlive()
  await page.click('.tab-item:nth-child(2)')
  await page.waitForTimeout(1200)
  await waitStable()
  const typeView = await page.evaluate(([expr]) => {
    const rows = eval(expr)
    return {
      headers: [...document.querySelectorAll('.table-area thead th')].map(th => (th.textContent || '').trim()).filter(Boolean),
      rowCount: rows.length,
      rows: rows.slice(0, 5),
      footer: (document.querySelector('.table-footer') || {}).textContent?.replace(/\s+/g, ' ').trim(),
    }
  }, [readRows])
  console.log('\n按费用类型视图:', JSON.stringify(typeView, null, 1))
  check('按费用类型行数 = 类型分组数', typeView.rowCount, liveType.length)
  checkTrue('按费用类型表头随视图切换（费用名称而非部门）',
    typeView.headers.includes('费用名称') && !typeView.headers.includes('部门'),
    JSON.stringify(typeView.headers))
  checkTrue('按费用类型首行 = 接口首行（名称+金额）',
    (typeView.rows[0] || []).join('|').includes(liveType[0].groupKey) &&
    (typeView.rows[0] || []).join('|').includes(cardMoney(liveType[0].totalAmount)),
    JSON.stringify(typeView.rows[0]))
  await page.screenshot({ path: SHOTS + '/02-type.png' })

  // ── 排序：点击「费用金额」列头排序图标 ──
  const sortIcon = await page.evaluateHandle(() => {
    const ths = [...document.querySelectorAll('.table-area thead th')]
    const th = ths.find(t => (t.textContent || '').includes('费用金额'))
    return th ? th.querySelector('.th-sort-icon') : null
  })
  if (sortIcon && sortIcon.asElement()) {
    await sortIcon.asElement().click()
    await page.waitForTimeout(900)
  }
  const sortedAsc = await page.evaluate(([expr]) => eval(expr).map(c => c.join('|')), [readRows])
  const minAmount = Math.min(...liveType.map(r => num(r.totalAmount)))
  checkTrue('列头排序生效（升序后首行为最小值）',
    sortedAsc.length > 1 && sortedAsc[0].includes(cardMoney(minAmount)),
    `首行=${sortedAsc[0]} / 最小金额=${cardMoney(minAmount)}`)

  // ── 切到「按明细」 ──
  await ensurePageAlive()
  await page.click('.tab-item:nth-child(3)')
  await page.waitForTimeout(1200)
  await waitStable()
  const docView = await page.evaluate(([expr]) => {
    const rows = eval(expr)
    return {
      headers: [...document.querySelectorAll('.table-area thead th')].map(th => (th.textContent || '').trim()).filter(Boolean),
      rowCount: rows.length,
      first: rows[0] || [],
      pager: (document.querySelector('.table-pagination') || {}).textContent?.replace(/\s+/g, ' ').trim(),
      footer: (document.querySelector('.table-footer') || {}).textContent?.replace(/\s+/g, ' ').trim(),
    }
  }, [readRows])
  console.log('\n按明细视图:', JSON.stringify(docView, null, 1))
  check('按明细本页行数（默认 20/页）', docView.rowCount, Math.min(20, liveDetail.total))
  checkTrue('明细列含 单据编号/审批状态/费用名称',
    docView.headers.includes('单据编号') && docView.headers.includes('审批状态') && docView.headers.includes('费用名称'),
    JSON.stringify(docView.headers))
  checkTrue('分页器显示共 N 条（= 费用笔数）', (docView.pager || '').includes(String(liveDetail.total)), docView.pager)
  checkTrue('按明细底部合计为本页口径（本页 + 其中已审批非 0 或本页无已审批）',
    (docView.footer || '').includes('（本页）'),
    docView.footer)
  await page.screenshot({ path: SHOTS + '/03-doc.png' })

  // ── 下钻：回到按部门，点击部门行 ──
  await ensurePageAlive()
  await page.click('.tab-item:nth-child(1)')
  await page.waitForTimeout(1200)
  await waitStable()
  const drillTarget = liveDept.find(r => r.groupKey !== '未指定部门')?.groupKey || ''
  const deptLinkText = await page.evaluate(([t]) => {
    const btns = [...document.querySelectorAll('tbody tr .drill-link')]
    const target = btns.find(b => (b.textContent || '').trim() === t) || btns[0]
    return target ? target.textContent.trim() : null
  }, [drillTarget])
  checkTrue('部门行有下钻链接', !!deptLinkText, deptLinkText)
  if (deptLinkText) {
    await page.evaluate(([t]) => {
      const btns = [...document.querySelectorAll('tbody tr .drill-link')]
      const target = btns.find(b => (b.textContent || '').trim() === t) || btns[0]
      if (target) target.click()
    }, [deptLinkText])
    await page.waitForTimeout(2200)
    const drilled = await page.evaluate(([expr]) => {
      const rows = eval(expr)
      const activeTab = (document.querySelector('.tab-item.active') || {}).textContent
      const deptInput = [...document.querySelectorAll('.search-grid input')].find(i => i.placeholder === '部门')
      return {
        activeTab: (activeTab || '').trim(),
        rowCount: rows.length,
        deptFilter: deptInput ? deptInput.value : '',
        pager: (document.querySelector('.table-pagination') || {}).textContent?.replace(/\s+/g, ' ').trim(),
      }
    }, [readRows])
    console.log('\n下钻结果:', JSON.stringify(drilled))
    const drillRows = liveDetail.rows.filter(d => (d.deptName || '') === deptLinkText)
    check('下钻后切到按明细', drilled.activeTab, '按明细')
    check('下钻后部门过滤已带出', drilled.deptFilter, deptLinkText)
    checkTrue('下钻后分页总数 = 该部门费用笔数',
      (drilled.pager || '').includes(String(drillRows.length)) && String(drillRows.length) !== '0',
      `${drilled.pager} / 期望 ${drillRows.length}`)
    await page.screenshot({ path: SHOTS + '/04-drill-down.png' })
  }

  // ── 快捷日期：本年 = 接口同区间；昨日 = 接口昨日区间 ──
  const clickQuickDate = async (label) => {
    const btns = await page.$$('.quick-dates .ant-btn')
    for (const b of btns) {
      const t = (await b.textContent())?.replace(/\s+/g, '').trim()
      if (t === label) { await b.click(); return true }
    }
    return false
  }
  const today = new Date()
  const ymd = (d) => d.toISOString().slice(0, 10)
  // 页面「本年」口径 = 年初 ~ 今天（与快捷日期实现一致）
  const yearStart = `${today.getFullYear()}-01-01`
  const yearEnd = ymd(today)
  const yesterday = new Date(today.getTime() - 86400000)

  /** 读数与接口对齐：刷新 → 读卡片 → 取接口，漂移（并行写入）时重试 3 次 */
  const readCardsAligned = async (apiParams) => {
    for (let attempt = 1; attempt <= 3; attempt++) {
      // 主动刷新：让页面用当前区间重新取数，缩小读数与接口的时间窗
      await page.click('.toolbar-right .ant-btn:has(.anticon-reload)').catch(() => {})
      await page.waitForTimeout(1500)
      await waitStable()
      const cards = await page.evaluate(() =>
        [...document.querySelectorAll('.ar-stat-card__value')].map(e => e.textContent.trim()))
      const api = (await apiReq('GET', `/erp/finance/expense-stats/summary?${apiParams}`, null, token)).body.data || {}
      if (cards[0] === '\u00a5' + cardMoney(api.totalAmount) || attempt === 3) {
        return { cards, api, attempt }
      }
      console.log('  [重试] 卡片与接口不一致，重新刷新…', cards[0], 'vs', '\u00a5' + cardMoney(api.totalAmount))
    }
  }

  // 先清掉下钻带出的过滤条件，再校验快捷日期（否则比较的是「部门子集 vs 全量」）
  const okReset = await page.evaluate(() => {
    const btns = [...document.querySelectorAll('.search-action-group .ant-btn')]
    const reset = btns.find(b => (b.textContent || '').replace(/\s+/g, '') === '重置')
    if (reset) { reset.click(); return true }
    return false
  })
  checkTrue('下钻后可重置查询条件', okReset)
  await page.waitForTimeout(2000)
  await waitStable()

  const okYear = await clickQuickDate('本年')
  await page.waitForTimeout(2200)
  const yearActive = await page.evaluate(() =>
    (document.querySelector('.quick-dates .ant-btn-primary') || {}).textContent?.replace(/\s+/g, '').trim())
  const yearAligned = await readCardsAligned(`dateStart=${yearStart}&dateEnd=${yearEnd}`)
  console.log('\n快捷日期「本年」:', JSON.stringify({ active: yearActive, total: yearAligned.cards[0], api: money(yearAligned.api.totalAmount), attempt: yearAligned.attempt }))
  checkTrue('快捷日期「本年」切换成功', okYear && yearActive === '本年', yearActive)
  check('「本年」费用总额 = 接口同区间', yearAligned.cards[0], '¥' + cardMoney(yearAligned.api.totalAmount))
  check('「本年」单据数 = 接口同区间', yearAligned.cards[4], num(yearAligned.api.expenseCount) + '单')
  await page.screenshot({ path: SHOTS + '/07-quick-date-year.png' })

  // ── 导出 CSV（按部门视图可见列） ──
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 20000 }).catch(() => null),
    page.click('.toolbar-right .ant-btn:has(.anticon-export)'),
  ])
  if (download) {
    const fname = download.suggestedFilename()
    checkTrue('导出 CSV 文件名', fname.startsWith('费用统计_') && fname.endsWith('.csv'), fname)
    await download.saveAs(SHOTS + '/export.csv').catch(() => {})
  } else {
    checkTrue('导出 CSV', false, '未触发下载')
  }

  // ── 快捷日期：昨日（与接口同区间对齐，证明日期口径真实生效） ──
  await clickQuickDate('昨日')
  await page.waitForTimeout(2200)
  const yAligned = await readCardsAligned(`dateStart=${ymd(yesterday)}&dateEnd=${ymd(yesterday)}`)
  console.log('\n快捷日期「昨日」:', JSON.stringify({ total: yAligned.cards[0], count: yAligned.cards[4], api: money(yAligned.api.totalAmount), attempt: yAligned.attempt }))
  check('「昨日」费用总额 = 接口昨日区间', yAligned.cards[0], '¥' + cardMoney(yAligned.api.totalAmount))
  check('「昨日」单据数 = 接口昨日区间', yAligned.cards[4], num(yAligned.api.expenseCount) + '单')
  await page.screenshot({ path: SHOTS + '/08-quick-date-yesterday.png' })

  checkTrue('无页面/接口错误', httpErrors.length === 0, httpErrors.length ? httpErrors.slice(0, 8).join(' || ') : '0 error')

  await browser.close()

  console.log(`\n================ 结果：通过 ${pass} 项 / 失败 ${fail} 项 ================`)
  if (fail > 0) process.exitCode = 1
}

main().catch(e => { console.error('执行异常', e); process.exitCode = 1 })
