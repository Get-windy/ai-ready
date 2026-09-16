/*
 * 表格底部「经典分页栏统一 + 展开按钮联动 + 冻结列对齐」端到端验收
 *
 * 覆盖三处组件级改动（2026-09-16）：
 *   1. 分页栏统一：DocCenterLayout / BillTableList / VxeTableList 内置的 antd a-pagination
 *      全部换成与「商城订单」同款的 StandardPagination 经典形态
 *      （首页 / 上页 / 第(x/y)页 / 下页 / 尾页 / 跳转到 N 页 / 共 N 条记录 / 每页显示 N 行）。
 *   2. 展开联动：BillDetailTable 点「表格展开显示」时冒泡 table-expand-change，
 *      宿主布局（CategoryListLayout / DocCenterLayout / BillTableList）自动收起表格下方的
 *      分页区 / 页脚区，表格真实长高占满（此前多数列表页没接 expand-change，按钮形同摆设）。
 *   3. 冻结列对齐：BillDetailTable 冻结列偏移改用「实测列宽」累加（操作列实际宽度 ≠ 配置宽度，
 *      按配置累加会把排在它后面的冻结列钉到错误位置，表现为表格中间浮着一个错位的列）。
 *
 * 用法：node tools/e2e-table-pagination-expand.cjs
 *      FE_URL=http://localhost:5656 ERP_PORT=5655 node tools/e2e-table-pagination-expand.cjs
 * 只读：仅登录 + 打开页面 + 点击展开/分页，不写库、不造数；截图落 tool-results/table-pagination-expand/。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const fs = require('fs')
const http = require('http')

const FE = process.env.FE_URL || 'http://localhost:5656'
const API_PORT = Number(process.env.ERP_PORT || 5655)
const SHOTS = 'I:/AI-Ready/tool-results/table-pagination-expand'
const USER = process.env.E2E_USER || 'admin'
const PWD = process.env.E2E_PWD || 'admin123'

let PASS = 0, FAIL = 0
const FAILURES = []
function check(name, ok, detail) {
  if (ok) { PASS++; console.log('  ✓', name) }
  else { const d = detail === undefined ? '' : JSON.stringify(detail); FAIL++; FAILURES.push(`${name} → ${d.slice(0, 200)}`); console.log('  ✗', name, d.slice(0, 200)) }
}

function rawReq(method, p, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: API_PORT, path: '/api' + p, method,
      headers: { 'Content-Type': 'application/json', ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => { try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))) } catch { resolve({}) } })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const cd = cap.data || {}
  const res = await rawReq('POST', '/auth/login', {
    username: USER, password: PWD, tenantName: '系统租户',
    captcha: extractCaptcha(cd.img), captchaKey: cd.uuid,
  })
  const d = res.data || {}
  const token = d.token || d.accessToken || d.tokenValue
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  return { token, tenantId: d.tenantId, tenantName: d.tenantName }
}

/** 页面内统一测量：classic 分页栏是否存在 / 冻结列是否连续 / 表格与下方区域高度 */
const MEASURE = () => {
  const bar = document.querySelector('.classic-pagination')
  const grid = document.querySelector('.spreadsheet-table')
  const firstRow = document.querySelector('.ss-grid tbody tr.ss-row')
  const box = (s) => { const el = document.querySelector(s); return el ? Math.round(el.getBoundingClientRect().height) : null }
  // 冻结列连续性：同一数据行里所有 position:sticky 且 left 生效的单元格，应首尾相接（间隙 ≤1px）
  let frozenGap = null
  if (firstRow && grid) {
    const rect = grid.getBoundingClientRect()
    const cells = [...firstRow.querySelectorAll('td')].filter(td => {
      const cs = getComputedStyle(td)
      return cs.position === 'sticky' && cs.left !== 'auto'
    })
    const spans = cells.map(td => { const r = td.getBoundingClientRect(); return { key: td.dataset.colKey, left: Math.round(r.left - rect.left), right: Math.round(r.right - rect.left) } })
    frozenGap = { cells: spans, gaps: spans.slice(1).map((s, i) => s.left - spans[i].right) }
  }
  return {
    classic: !!bar,
    classicText: bar ? bar.innerText.replace(/\s+/g, ' ').trim() : null,
    oldAntdPagination: !!document.querySelector('.table-pagination .ant-pagination:not(.ant-pagination-disabled)'),
    hasGrid: !!grid,
    tableH: box('.spreadsheet-table'),
    footerH: box('.table-footer-section'),
    paginationH: box('.table-pagination'),
    frozenGap,
    errors: [],
  }
}

// 覆盖三种宿主布局 + 三处冻结列「操作列在前、其它冻结列在后」的高危页
const PAGES = [
  { url: '/trade/mall-order', title: '商城订单（CategoryListLayout + StandardPagination）', layout: 'cat' },
  { url: '/mall/order-process', title: '订单处理（同源页，按单据 Tab）', layout: 'cat' },
  { url: '/sales/order-center', title: '订单处理中心（DocCenterLayout）', layout: 'dcl' },
  { url: '/sales/detail-query', title: '销售明细查询（BillTableList）', layout: 'btl' },
  { url: '/dms/vehicle', title: '车辆管理（BillTableList，操作列后有冻结列）', layout: 'btl' },
  { url: '/finance/receipt-doc/form', title: '收款单表单（BillFormPage，操作列后有冻结列）', layout: 'form' },
  // 页面自挂分页栏的三页（原为内联 a-pagination，2026-09-16 一并统一）
  { url: '/analytics/check-stock', title: '查库存（页面自挂 StandardPagination）', layout: 'page' },
  { url: '/analytics/inventory-analysis', title: '库存分析（页面自挂 StandardPagination）', layout: 'page' },
  { url: '/system/menu/index', title: '菜单管理（页面自挂 StandardPagination）', layout: 'page' },
  // 双入口菜单的「历史」列表页（sys_menu.list_path，不在 path 里 —— 巡检容易整片漏掉）
  { url: '/sales/return-doc/index', title: '销售退货单列表（双入口 list_path）', layout: 'btl' },
  { url: '/finance/receipt-doc/index', title: '收款单列表（双入口 list_path）', layout: 'btl' },
  { url: '/md/bank-account', title: '银行账户（表格自带分页 + 表尾路径条）', layout: 'page' },
]

async function main() {
  console.log(`表格分页栏 / 展开按钮 / 冻结列 组件级验收 —— 前端 ${FE}，后端 :${API_PORT}`)
  fs.mkdirSync(SHOTS, { recursive: true })
  const auth = await login()
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1680, height: 950 } })
  let pageErrors = []
  page.on('console', m => { if (m.type() === 'error') pageErrors.push(m.text()) })
  page.on('pageerror', e => pageErrors.push(String(e)))

  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid, tn]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tid || 1))
    localStorage.setItem('tenantName', tn || '')
  }, [auth.token, auth.tenantId, auth.tenantName])

  const slug = (u) => u.replace(/[^\w]/g, '_')

  for (const p of PAGES) {
    console.log(`\n═══ ${p.title}  (${p.url}) ═══`)
    pageErrors = []
    await page.goto(`${FE}${p.url}`, { waitUntil: 'domcontentloaded' }).catch(() => {})
    await page.waitForTimeout(3200)
    const before = await page.evaluate(MEASURE)
    const title = await page.title()
    check(`${p.url} 页面可打开（非 404）`, !/页面不存在/.test(title), title)

    // ── 分包与否：列表页（非单据表单）必须渲染出经典分页栏 ──
    if (before.hasGrid && p.layout !== 'form') {
      check(`${p.url} 底部为经典分页栏`, before.classic, before.classicText)
      check(`${p.url} 不再残留旧 antd 分页栏`, !before.oldAntdPagination, before.oldAntdPagination)
      check(`${p.url} 分页栏含「共 N 条记录 / 每页显示 N 行」`, /共 \d+ 条记录/.test(before.classicText || '') && /每页显示 \d+ 行/.test(before.classicText || ''), before.classicText)
    }

    // ── 冻结列必须首尾相接（操作列实际宽度 ≠ 配置宽度，按配置累加会留缝） ──
    if (before.frozenGap && before.frozenGap.cells.length) {
      const gaps = before.frozenGap.gaps
      const bad = gaps.filter(g => g > 1 || g < -1)
      check(`${p.url} 冻结列首尾相接无缝隙`, bad.length === 0, { cells: before.frozenGap.cells, gaps })
    }

    // ── 展开按钮：下方区域应收起，表格真实长高 ──
    const btn = page.locator('.detail-expand button').first()
    if (await btn.count()) {
      await btn.click()
      await page.waitForTimeout(1000)
      const after = await page.evaluate(MEASURE)
      const grew = (before.tableH !== null && after.tableH !== null && after.tableH > before.tableH)
      const belowGone = (before.footerH || before.paginationH) ? (after.footerH === null && after.paginationH === null) : null
      check(`${p.url} 展开后表格真实长高`, grew, { before: before.tableH, after: after.tableH })
      if (belowGone !== null) check(`${p.url} 展开后下方区域让位`, belowGone, { footer: after.footerH, pagination: after.paginationH })
      await page.screenshot({ path: `${SHOTS}/${slug(p.url)}-expanded.png` })
      // 收起复原
      await btn.click()
      await page.waitForTimeout(800)
      const restored = await page.evaluate(MEASURE)
      check(`${p.url} 收起后复原`, restored.tableH === before.tableH, { before: before.tableH, restored: restored.tableH })
    } else {
      console.log('  · 本页无「表格展开显示」按钮，跳过展开用例')
    }

    // ── 分包交互：下页 / 每页显示（有多页时） ──
    const bar = page.locator('.classic-pagination').first()
    if (await bar.count()) {
      const next = bar.locator('button:has-text("下页")').first()
      if (await next.isEnabled().catch(() => false)) {
        // 首行文本（兼容两种表格实现：自研 ss-grid 与 antd a-table），用于判断翻页后数据真的换了
        const firstRowOf = () => page.evaluate(() => {
          const el = document.querySelector('.ss-grid tbody tr.ss-row')
            || document.querySelector('.ant-table-tbody tr.ant-table-row')
            || document.querySelector('.vxe-table--body tbody tr')
          return el ? el.innerText.replace(/\s+/g, ' ').slice(0, 80) : null
        })
        const b = await firstRowOf()
        await next.click()
        await page.waitForTimeout(2200)
        const a = await firstRowOf()
        const text = (await bar.innerText()).replace(/\s+/g, ' ').trim()
        check(`${p.url} 下页可用且数据翻页`, /第\(2\//.test(text) && a !== b, text)
      }
    }

    check(`${p.url} 无新增 JS 错误`, pageErrors.length === 0, pageErrors.slice(0, 3))
  }

  await browser.close()
  console.log(`\n结果：${PASS} 通过 / ${FAIL} 失败`)
  if (FAILURES.length) { console.log('失败项：'); FAILURES.forEach(f => console.log(' -', f)) }
  process.exit(FAIL ? 1 : 0)
}

main().catch(e => { console.error(e); process.exit(1) })
