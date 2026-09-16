/*
 * 全站巡检：「表格展开显示」按钮是否真的把表格顶到内容区底部
 *
 * 判定口径（每页点一次展开按钮，实测几何）：
 *   · 无展开按钮（表格不是 BillDetailTable / 没渲染）→ 不适用；
 *   · 收起态表格底边已贴内容区底边（gap ≤ 12px）→ 本来就没东西可让位，正常；
 *   · 展开后 gap ≤ 12px（表格真的占满到页面底部）→ ✅ 修复到位；
 *   · 展开后 gap 仍 > 12px → ❌ 未占满（按钮效果不完整），并列出此时仍占着下方的元素用于定位。
 *
 * 用法：node tools/audit-table-expand.cjs                  # 全量（约 200 页）
 *      node tools/audit-table-expand.cjs --only=/sales    # 只跑路径含关键字的页
 *      node tools/audit-table-expand.cjs /sales/return-doc/form /trade/mall-order
 * 只读：仅登录 + 点展开/收起，不点任何写库按钮。
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
const URLS = process.argv.filter(a => a.startsWith('/') && !a.startsWith('--'))
const TOL = 12 // 允许的贴底误差（px）

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

/**
 * 线上可访问的页面 URL = 菜单主路径（sys_menu.path）+ 双入口菜单的「历史」列表页（sys_menu.list_path）。
 * ⚠️ 必须带上 list_path！单据类菜单是双入口：主路径是表单页，列表页（「历史」按钮 / 列表页跳回）在
 *    list_path（如 sales/return-doc/index）。只遍历 path 会把 50+ 张列表页整片漏掉。
 */
async function liveMenuPages() {
  const c = new Client(DSN)
  await c.connect()
  try {
    const urls = new Set()
    const r1 = await c.query(
      "SELECT path, component FROM sys_menu WHERE menu_type=1 AND component LIKE 'views/%' AND component LIKE '%.vue' ORDER BY id"
    )
    for (const x of r1.rows) {
      if (fs.existsSync(`${ADMIN_SRC}/${x.component}`)) urls.add('/' + String(x.path).replace(/^\/+/, ''))
    }
    const r2 = await c.query(
      "SELECT list_path FROM sys_menu WHERE display_mode=1 AND list_path IS NOT NULL AND list_path<>'' ORDER BY id"
    )
    for (const x of r2.rows) {
      const p = String(x.list_path).replace(/^views\//, '').replace(/\.vue$/, '')
      const f1 = `${ADMIN_SRC}/views/${p}.vue`
      const f2 = `${ADMIN_SRC}/views/${p}/index.vue`
      if (fs.existsSync(f1) || fs.existsSync(f2)) urls.add('/' + p)
    }
    return [...urls]
  } finally { await c.end() }
}

const MEASURE = () => {
  // ⚠️ 必须取「可见的」表格：多 Tab 页面 DOM 里可能同时存在多个 .spreadsheet-table，
  //    取第一个会量到隐藏 Tab 里高度 1px 的表，得出"未占满"的假结论。
  let grid = null
  let best = 0
  for (const el of document.querySelectorAll('.spreadsheet-table')) {
    const r = el.getBoundingClientRect()
    if (r.height <= 4 || r.width <= 4) continue
    if (r.bottom < 0 || r.top > window.innerHeight) continue
    const area = r.width * r.height
    if (area > best) { best = area; grid = el }
  }
  if (!grid) return { noTable: true }
  const tableRect = grid.getBoundingClientRect()
  const pageEl = document.querySelector('.page-container, .page-content, .content-area') || document.body
  const pageRect = pageEl.getBoundingClientRect()
  const bottom = Math.min(pageRect.bottom || window.innerHeight, window.innerHeight)
  // 表格正下方、与表格同列、仍然可见占位的元素（诊断「为什么没占满」用）
  // 排除左侧导航/顶部栏：只取横向与表格区域重叠的元素
  const below = []
  for (const el of document.querySelectorAll('body *')) {
    if (el === grid || el.contains(grid) || grid.contains(el)) continue
    const r = el.getBoundingClientRect()
    if (r.height <= 4 || r.width <= 4) continue
    if (r.top < tableRect.bottom - 4) continue
    if (r.bottom > bottom + 6) continue
    if (r.right < tableRect.left + 8 || r.left > tableRect.right - 8) continue // 不同列（如左侧菜单）
    const cls = String(el.className || '').split(/\s+/).filter(Boolean).slice(0, 2).join('.')
    below.push(`${el.tagName.toLowerCase()}${cls ? '.' + cls : ''}:${Math.round(r.height)}`)
  }
  return {
    noTable: false,
    // 可见表格数量：全局兜底 CSS（.table-expanded ~ *）会隐藏"表格的后续兄弟节点"，
    // 若某页把多张表放在同一容器里并列渲染，展开一张会误隐藏另一张 —— 这里做个体检。
    visibleTables: [...document.querySelectorAll('.spreadsheet-table')].filter(e => e.getBoundingClientRect().height > 4).length,
    tableH: Math.round(tableRect.height),
    gap: Math.round(bottom - tableRect.bottom),
    below: [...new Set(below)].slice(0, 6).join(' '),
    expanded: !!document.querySelector('.table-expanded'),
  }
}

const measure = (page) => page.evaluate(MEASURE)

async function main() {
  console.log(`「表格展开显示」是否占满到页面底部 —— 前端 ${FE}，后端 ${API}${ONLY ? `，过滤 ${ONLY}` : ''}`)
  const auth = await login()
  let pages = URLS.length ? URLS : await liveMenuPages()
  if (ONLY) pages = pages.filter(p => p.includes(ONLY))
  console.log(`待巡检 ${pages.length} 页\n`)

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1680, height: 950 } })
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid, tn]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tid || 1))
    localStorage.setItem('tenantName', tn || '')
  }, [auth.token, auth.tenantId, auth.tenantName])

  const notFull = []   // ❌ 展开后仍未贴底
  const hidTable = []  // ❌ 展开后同页其它可见表格被误隐藏（全局兜底 CSS 的副作用体检）
  const noButton = []
  const alreadyFull = []
  const ok = []
  let notFound = 0

  for (const [i, p] of pages.entries()) {
    await page.goto(`${FE}${p}`, { waitUntil: 'domcontentloaded' }).catch(() => {})
    await page.waitForTimeout(1700)
    if (/页面不存在/.test(await page.title())) { notFound++; continue }
    const btn = page.locator('.detail-expand button').first()
    if (!(await btn.count())) { noButton.push(p); continue }
    const before = await measure(page)
    if (before.noTable) continue
    if (before.gap <= TOL) { alreadyFull.push(p); continue } // 本来就没东西可让位
    await btn.click().catch(() => {})
    await page.waitForTimeout(900)
    const after = await measure(page)
    await btn.click().catch(() => {}) // 收起复原
    await page.waitForTimeout(300)
    // 占满判定：贴底（≤TOL）即可；页面自带卡片内边距/一行小字这类 ≤40px 的残差不算问题，
    // 但要求表格确实长高了（否则就是"点了没反应"）。
    if (before.visibleTables > 1 && after.visibleTables < before.visibleTables) {
      hidTable.push(`${p}  可见表格 ${before.visibleTables}→${after.visibleTables}`)
    }
    const filled = after.gap <= TOL || (after.gap <= 40 && after.tableH > before.tableH + 4)
    if (filled) ok.push(p)
    else notFull.push(`${p}  收起gap=${before.gap} 展开gap=${after.gap} 表高 ${before.tableH}→${after.tableH}  仍占位: ${after.below || '（未识别）'}`)

    process.stdout.write(`\r  进度 ${i + 1}/${pages.length}  占满 ${ok.length}  未占满 ${notFull.length}  收起即贴底 ${alreadyFull.length}`)
  }
  console.log('\n')

  const dump = (t, arr) => { console.log(`── ${t}（${arr.length}）──`); arr.slice(0, 120).forEach(x => console.log('  ' + x)); if (!arr.length) console.log('  （无）'); console.log('') }
  dump('❌ 展开后仍未占满到页面底部（要修）', notFull)
  dump('❌ 展开后同页其它表格被误隐藏（要修）', hidTable)
  dump('✅ 展开后已占满', ok)
  dump('· 收起态已贴底（无让位空间，正常）', alreadyFull)
  dump('· 无展开按钮（表格非 BillDetailTable）', noButton)
  console.log(`统计：巡检 ${pages.length} 页，404 ${notFound} 页，未占满 ${notFull.length} 项，误隐藏表格 ${hidTable.length} 项`)
  await browser.close()
  process.exit(notFull.length + hidTable.length ? 1 : 0)
}

main().catch(e => { console.error(e); process.exit(1) })
