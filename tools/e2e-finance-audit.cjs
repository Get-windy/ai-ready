/*
 * 财务模块全入口可用性验证（2026-09-23 财务审计用）
 *
 * 目的：逐个打开「财务」mega 菜单下的全部叶子入口页，回答三个问题——
 *   ① 页面能不能打开（是否落到 404 / 空白）；
 *   ② 打开过程中有没有失败的业务请求（4xx/5xx）；
 *   ③ 列表页有没有渲染出表格。
 *
 * ⚠️ 会话陷阱（本仓已踩过）：sa-token 是单端登录，脚本**必须全程共用一个登录会话**。
 *    绝不能在循环里重新 login()，否则后一次登录会把前一次踢下线，
 *    从那页起会刷出成片的 401/500，整份结果失真。
 *    本脚本只登录一次，token 注入 localStorage，全程复用同一个 browser context。
 *
 * 用法：node tools/e2e-finance-audit.cjs
 * 前提：后端 5655、前端 5656 已在运行。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = 'http://localhost:5656'
const OUT = 'I:/AI-Ready/tool-results/finance-audit'
const SHOTS = OUT + '/shots'

// 财务菜单全部入口（path 取自 sys_menu，见 menu-permission-db.md §1.2）
const ENTRIES = [
  ['80140', '财务总览', '/finance/index'],
  ['80100', '按单收款', '/finance/receipt-by-doc'],
  ['80101', '收款单', '/finance/receipt-doc/form'],
  ['80102', '预收款单', '/finance/advance-receipt/form'],
  ['80103', '提现存现转款', '/finance/cash-transfer/form'],
  ['80104', '待确认款项', '/finance/pending-confirm'],
  ['80105', '在线支付对账单', '/finance/online-payment-reconcile'],
  ['80141', '应收账款管理', '/finance/receivable'],
  ['80110', '按单付款', '/finance/payment-by-doc'],
  ['80111', '付款单', '/finance/payment-doc/form'],
  ['80112', '预付款单', '/finance/advance-payment/form'],
  ['80142', '应付账款管理', '/finance/payable'],
  ['80115', '费用单', '/finance/expense-doc/form'],
  ['80116', '其他收入', '/finance/other-income-doc/form'],
  ['80117', '应收应付调整', '/finance/ar-ap-adjust/form'],
  ['70204', '账款交账', '/finance/account-delivery'],
  ['80144', '往来对冲', '/finance/offset'],
  ['80145', '定金押金', '/finance/deposit'],
  ['80120', '会计凭证', '/finance/voucher/form'],
  ['80121', '月结', '/finance/month-closing'],
  ['80122', '对账', '/finance/reconciliation'],
  ['80143', '收付款核销', '/finance/write-off'],
  ['70220', '总账', '/finance/general-ledger'],
  ['70221', '明细账', '/finance/detail-ledger'],
  ['70222', '科目余额表', '/finance/balance-sheet'],
  ['70223', '辅助核算余额表', '/finance/aux-balance'],
  ['80146', '辅助核算', '/finance/auxiliary'],
  ['70230', '资产负债表', '/finance/balance-report'],
  ['70231', '利润表', '/finance/profit-report'],
  ['80147', '资产总览', '/fixed-asset/index'],
  ['80148', '预算总览', '/budget/index'],
  ['80130', '预算编制', '/finance/budget-plan/form'],
  ['80131', '预算执行', '/finance/budget-exec'],
  ['80149', '预算模板', '/budget/template/index'],
  ['80150', '预算调整', '/budget/adjustment/index'],
  ['80151', '预算报表', '/budget/report/index'],
  ['70242', '费用审批', '/finance/expense-approval'],
  ['70244', '费用统计', '/finance/expense-stats'],
  // 以下为「有菜单指向表单、列表页由 display_mode=1 的 list_path 生成」的列表页
  ['80101L', '收款单列表', '/finance/receipt-doc/index'],
  ['80111L', '付款单列表', '/finance/payment-doc/index'],
  ['80115L', '费用单列表', '/finance/expense-doc/index'],
  ['80120L', '会计凭证列表', '/finance/voucher/index'],
  ['80130L', '预算编制列表', '/finance/budget-plan/index'],
  ['80102L', '预收款单列表', '/finance/advance-receipt/index'],
  ['80112L', '预付款单列表', '/finance/advance-payment/index'],
  ['80103L', '提现存现转款列表', '/finance/cash-transfer/index'],
  ['80116L', '其他收入列表', '/finance/other-income-doc/index'],
  ['80117L', '应收应付调整列表', '/finance/ar-ap-adjust/index'],
]

function apiReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers['Authorization'] = token
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: 5655, path: '/api' + path, method, headers },
      res => { let s = ''; res.on('data', c => { s += c }); res.on('end', () => { try { resolve(JSON.parse(s)) } catch (e) { resolve({ raw: s }) } }) })
    r.on('error', reject); if (data) r.write(data); r.end()
  })
}

;(async () => {
  if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

  // ── 唯一一次登录 ──
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const lg = await apiReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  if (!lg.data || !lg.data.token) { console.error('登录失败:', JSON.stringify(lg).slice(0, 300)); process.exit(1) }
  const token = lg.data.token
  console.log('登录成功，token 长度', token.length)

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 950 } })
  await ctx.addInitScript(t => {
    localStorage.setItem('token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
  }, token)
  const page = await ctx.newPage()

  let consoleErrors = []
  let failedReqs = []
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text().slice(0, 250)) })
  page.on('response', async r => {
    const u = r.url()
    if (u.includes('/api/') && r.status() >= 400) failedReqs.push(`${r.status()} ${r.request().method()} ${u.replace(FE, '')}`)
  })

  const results = []
  for (const [id, name, path] of ENTRIES) {
    consoleErrors = []; failedReqs = []
    let err = null, is404 = false, rows = 0, bodyLen = 0, title = ''
    try {
      await page.goto(FE + path, { waitUntil: 'domcontentloaded', timeout: 45000 })
      await page.waitForTimeout(3200)
      const info = await page.evaluate(() => ({
        notFound: !!document.querySelector('.not-found') ||
                  /页面不存在|404|Not Found|找不到页面/.test(document.body.innerText.slice(0, 1500)),
        rows: document.querySelectorAll('.v-data-table tbody tr, table tbody tr').length,
        len: document.body.innerText.length,
        title: (document.querySelector('h1,h2,.page-title') || {}).innerText || '',
      }))
      is404 = info.notFound; rows = info.rows; bodyLen = info.len; title = titleOf(info.title)
    } catch (e) { err = e.message.slice(0, 160) }

    const uniqFailed = [...new Set(failedReqs)]
    const status = err ? 'ERR' : (is404 ? '404' : 'OK')
    results.push({ id, name, path, status, rows, bodyLen, title, err, failed: uniqFailed, console: [...new Set(consoleErrors)].slice(0, 3) })
    console.log(`${status.padEnd(4)} ${name.padEnd(16)} ${path.padEnd(38)} rows=${String(rows).padEnd(4)} bad=${uniqFailed.length} ${err || ''}`)
    if (path === '/finance/index' || is404) {
      try { await page.screenshot({ path: `${SHOTS}/${id}-${name}.png` }) } catch (e) {}
    }
  }

  function titleOf(s) { return (s || '').replace(/\s+/g, ' ').trim().slice(0, 40) }

  const bad = results.filter(r => r.status !== 'OK')
  const withFailedReq = results.filter(r => r.failed.length > 0)
  const withConsole = results.filter(r => r.console.length > 0)

  fs.writeFileSync(OUT + '/e2e-result.json', JSON.stringify(results, null, 2))
  const lines = []
  lines.push('# 财务模块全入口可用性实跑结果', '')
  lines.push(`- 时间：${new Date().toISOString()}`)
  lines.push(`- 后端 http://localhost:5655 · 前端 ${FE} · 账号 admin / 系统租户（超管，菜单不受 menu_level 过滤）`)
  lines.push(`- 入口总数 **${ENTRIES.length}**｜打不开/404 **${bad.length}**｜有失败请求 **${withFailedReq.length}**｜有 console 错误 **${withConsole.length}**`, '')
  lines.push('## 汇总表', '')
  lines.push('| 菜单ID | 页面 | 路径 | 结果 | 表格行数 | 失败请求 | 备注 |')
  lines.push('|---|---|---|---|---|---|---|')
  for (const r of results) {
    const note = r.err ? `异常:${r.err}` : (r.console.length ? `console:${r.console[0]}` : '')
    lines.push(`| ${r.id} | ${r.name} | \`${r.path}\` | **${r.status}** | ${r.rows} | ${r.failed.length ? r.failed.map(f => '`' + f + '`').join('<br>') : '—'} | ${note.replace(/\|/g, '/')} |`)
  }
  if (withFailedReq.length) {
    lines.push('', '## 失败请求明细', '')
    for (const r of withFailedReq) {
      lines.push(`### ${r.name} \`${r.path}\``, '')
      r.failed.forEach(f => lines.push(`- \`${f}\``))
      lines.push('')
    }
  }
  if (withConsole.length) {
    lines.push('', '## console 错误明细', '')
    for (const r of withConsole) {
      lines.push(`### ${r.name} \`${r.path}\``, '')
      r.console.forEach(c => lines.push(`- ${c.replace(/\n/g, ' ')}`))
      lines.push('')
    }
  }
  fs.writeFileSync(OUT + '/e2e-result.md', lines.join('\n'))

  console.log(`\n===== 汇总 =====`)
  console.log(`入口 ${ENTRIES.length}｜异常 ${bad.length}｜失败请求页 ${withFailedReq.length}｜console 错误页 ${withConsole.length}`)
  await browser.close()
})().catch(e => { console.error('FAILED:', e.message); process.exit(1) })
