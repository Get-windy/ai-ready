/*
 * 全站巡检：表格底部分页栏形态 + 「表格展开显示」联动 + 冻结列连续性 + console 错误
 *
 * 背景（2026-09-16 组件级收口）：
 *   1. 分页栏全站只保留一种形态 —— StandardPagination classic（首页/上页/第(x/y)页/下页/尾页/跳转/共 N 条记录/每页显示 N 行）；
 *      DocCenterLayout / BillTableList / VxeTableList 的内置分页已统一，页面不应再各写内联 a-pagination。
 *   2. BillDetailTable 展开时冒泡 table-expand-change，宿主布局自动收起表格下方的分页/页脚区。
 *   3. 冻结列偏移按实测列宽累加，同排冻结列必须首尾相接（间隙 >1px 即为错位）。
 *
 * 用法：node tools/audit-pagination-expand.cjs            # 全量（约 200 页）
 *      node tools/audit-pagination-expand.cjs --only=/dms # 只跑路径含关键字的页
 *      FE_URL=... ERP_PORT=... node tools/audit-pagination-expand.cjs
 * 只读：仅登录 + 打开页面，不点击任何写库按钮。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')
const fs = require('fs')

const FE = process.env.FE_URL || 'http://localhost:5656'
const API = process.env.BASE || 'http://localhost:5655'
const ADMIN_SRC = 'i:/AI-Ready/frontend/apps/pc-admin/src'
const USER = process.env.E2E_USER || 'admin'
const PWD = process.env.E2E_PWD || 'admin123'
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
const ONLY = (process.argv.find(a => a.startsWith('--only=')) || '').replace('--only=', '')

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function login() {
  const cap = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
  const cd = cap.data || cap
  const res = await fetch(`${API}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: USER, password: PWD, tenantName: '系统租户', captcha: extractCaptcha(cd.img), captchaKey: cd.uuid }),
  }).then(r => r.json())
  const d = res?.data || {}
  const token = d.token || d.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  return { token, tenantId: d.tenantId, tenantName: d.tenantName }
}

/** 线上菜单页（menu_type=1 且有真实存在的 .vue 文件），用于全量巡检 */
async function liveMenuPages() {
  const c = new Client(DSN)
  await c.connect()
  try {
    const r = await c.query(
      "SELECT path, component FROM sys_menu WHERE menu_type=1 AND component LIKE 'views/%' AND component LIKE '%.vue' ORDER BY id"
    )
    const seen = new Set()
    return r.rows
      .filter(x => fs.existsSync(`${ADMIN_SRC}/${x.component}`))
      .map(x => '/' + String(x.path).replace(/^\/+/, ''))
      .filter(p => !seen.has(p) && seen.add(p))
  } finally { await c.end() }
}

const MEASURE = () => {
  const grid = document.querySelector('.spreadsheet-table')
  const firstRow = document.querySelector('.ss-grid tbody tr.ss-row')
  let frozenGap = null
  if (firstRow && grid) {
    const rect = grid.getBoundingClientRect()
    const cells = [...firstRow.querySelectorAll('td')].filter(td => {
      const cs = getComputedStyle(td)
      return cs.position === 'sticky' && cs.left !== 'auto'
    })
    if (cells.length > 1) {
      const spans = cells.map(td => { const r = td.getBoundingClientRect(); return { key: td.dataset.colKey, left: Math.round(r.left - rect.left), right: Math.round(r.right - rect.left) } })
      frozenGap = spans.slice(1).map((s, i) => s.left - spans[i].right)
    }
  }
  return {
    notFound: /页面不存在/.test(document.title),
    hasTable: !!grid,
    classic: !!document.querySelector('.classic-pagination'),
    // 分页栏容器：布局的插槽容器 / BillTableList 自带容器 / BillDetailTable 自带分页（:show-pagination）
    paginationSlot: !!document.querySelector('.table-pagination, .table-footer-section, .bill-detail-table .standard-pagination'),
    // 用了「分页已内置」的表格组件：这两类页面若渲染不出经典分页栏 = 硬失败
    usesBuiltinPager: !!document.querySelector('.bill-table-list-container, .doc-center-layout, .vxe-table-list-container'),
    frozenGap,
  }
}

async function main() {
  console.log(`全站分页栏/展开/冻结列巡检 —— 前端 ${FE}，后端 ${API}${ONLY ? `，过滤 ${ONLY}` : ''}`)
  const auth = await login()
  let pages = await liveMenuPages()
  if (ONLY) pages = pages.filter(p => p.includes(ONLY))
  console.log(`待巡检 ${pages.length} 页\n`)

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1680, height: 950 } })
  let consoleErrors = []
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()) })
  page.on('pageerror', e => consoleErrors.push('PAGEERROR ' + String(e)))

  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid, tn]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tid || 1))
    localStorage.setItem('tenantName', tn || '')
  }, [auth.token, auth.tenantId, auth.tenantName])

  const leftovers = []   // ❌ 硬失败：用了内置分页的表格组件，却没渲染出经典分页栏
  const bespoke = []     // ⚠️ 待收敛：页面自建的 a-pagination（不属组件内置，尚未统一）
  const frozenBad = []   // 冻结列错位
  const jsErr = []       // 页面 JS 错误（排除资源 404 噪声）
  const noPager = []     // 有表格但完全没有分页栏（多为表单页，仅记录）
  let notFound = 0

  for (const [i, p] of pages.entries()) {
    consoleErrors = []
    await page.goto(`${FE}${p}`, { waitUntil: 'domcontentloaded' }).catch(() => {})
    await page.waitForTimeout(1800)
    const m = await page.evaluate(MEASURE)
    if (m.notFound) { notFound++; continue }
    const noisy = consoleErrors.filter(e => !/Failed to load resource|Resource Error/i.test(e))
    if (noisy.length) jsErr.push(`${p} :: ${noisy[0].slice(0, 120)}`)
    if (m.hasTable && !m.classic && m.paginationSlot) {
      (m.usesBuiltinPager ? leftovers : bespoke).push(p)
    }
    if (m.hasTable && !m.classic && !m.paginationSlot) noPager.push(p)
    if (m.frozenGap && m.frozenGap.some(g => g > 1 || g < -1)) frozenBad.push(`${p} :: gaps=${JSON.stringify(m.frozenGap)}`)
    process.stdout.write(`\r  进度 ${i + 1}/${pages.length}  旧分页残留 ${leftovers.length}  自建分页 ${bespoke.length}  冻结错位 ${frozenBad.length}  JS错误 ${jsErr.length}`)
  }
  console.log('\n')

  const dump = (title, arr) => {
    console.log(`── ${title}（${arr.length}）──`)
    arr.slice(0, 40).forEach(x => console.log('  ' + x))
    if (!arr.length) console.log('  （无）')
    console.log('')
  }
  dump('❌ 旧分页残留（用了内置分页的表格组件，却没渲染出经典分页栏）', leftovers)
  dump('❌ 冻结列错位（同排冻结列间隙 >1px）', frozenBad)
  dump('⚠️ 页面 JS 错误', jsErr)
  dump('⚠️ 页面自建 a-pagination，尚未统一（非组件内置，需逐页判断）', bespoke)
  dump('ℹ️ 有表格但无分页栏（多为表单页/明细表，仅记录）', noPager)
  // 硬失败只算「本次改动应当消除的问题」；JS 错误里的 [API Error] 多为后端未实现/报错，属既有问题，只报告不判失败
  const hardFail = leftovers.length + frozenBad.length
  console.log(`统计：巡检 ${pages.length} 页，404 ${notFound} 页，硬失败 ${hardFail} 项（另有页面 JS 错误 ${jsErr.length} 项，需人工判断是否既有问题）`)
  await browser.close()
  process.exit(hardFail ? 1 : 0)
}

main().catch(e => { console.error(e); process.exit(1) })
