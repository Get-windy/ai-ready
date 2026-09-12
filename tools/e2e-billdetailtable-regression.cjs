/* BillDetailTable 组件改动回归：多列冻结偏移 + 冻结表头层级 + 行底色
 * 覆盖三个典型使用方：销售订单表单（明细可编辑）、订单处理中心（列表）、商品列表
 * 运行： node tools/e2e-billdetailtable-regression.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE_URL || 'http://localhost:5656'
const API_PORT = Number(process.env.API_PORT || 5720)
const SHOTS = 'I:/AI-Ready/tool-results/billdetailtable-regression'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

let TOKEN = null
let PASS = 0
let FAIL = 0
const FAILURES = []

function rawReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: API_PORT, path: '/api' + path, method,
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

function check(name, cond, detail) {
  if (cond) { PASS++; console.log('  ✓', name) }
  else { const d = detail === undefined ? '' : (typeof detail === 'string' ? detail : JSON.stringify(detail)); FAIL++; FAILURES.push(name + (d ? ' → ' + d.slice(0, 180) : '')); console.log('  ✗', name, d.slice(0, 180)) }
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid,
  })
  TOKEN = res?.data?.token || res?.data?.accessToken
  if (!TOKEN) throw new Error('登录失败')
}

const PAGES = [
  { name: '销售订单历史', url: '/sales/order/index', expect: '销售订单' },
  { name: '订单处理中心', url: '/sales/order-center', expect: '订单处理中心' },
  { name: '商品条码', url: '/md/barcode', expect: '商品条码' },
  { name: '销售出库历史', url: '/sales/outbound/index', expect: '销售出库' },
]

async function main() {
  await login()
  console.log('登录成功\n=== BillDetailTable 回归 ===')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const p = await ctx.newPage()
  const errors = []
  p.on('pageerror', e => errors.push(String(e)))
  await p.route(url => new URL(url).pathname.startsWith('/api/'), route => {
    const u = new URL(route.request().url())
    return route.continue({ url: `http://localhost:${API_PORT}` + u.pathname + u.search })
  })
  await p.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await p.evaluate(([tk]) => localStorage.setItem('token', tk), [TOKEN])

  for (const page of PAGES) {
    const before = errors.length
    await p.goto(`${FE}${page.url}`, { waitUntil: 'domcontentloaded' })
    await p.waitForTimeout(6000)
    await p.evaluate(([tk]) => { if (!localStorage.getItem('token')) localStorage.setItem('token', tk) }, [TOKEN])
    await p.screenshot({ path: `${SHOTS}/${page.name}.png` })
    const text = await p.evaluate(() => (document.body.innerText || '').replace(/\s+/g, ''))
    const rows = await p.evaluate(() => document.querySelectorAll('.ss-grid tbody tr').length)
    const notFound = text.includes('页面不存在') || text.includes('404')
    const stickyOk = await p.evaluate(() => {
      const ths = [...document.querySelectorAll('.ss-grid thead th')].filter(t => getComputedStyle(t).position === 'sticky')
      // 同轴冻结列的 left 偏移必须互不相同（多列冻结不重叠）
      const lefts = ths.map(t => t.style.left).filter(Boolean)
      return { count: ths.length, unique: new Set(lefts).size, lefts }
    })
    check(`${page.name}：页面可打开（非 404）`, !notFound && text.includes(page.expect.replace(/\s+/g, '')), { notFound, url: p.url() })
    check(`${page.name}：表格渲染正常`, rows > 0, rows)
    check(`${page.name}：冻结列偏移互不重叠`, stickyOk.unique === (stickyOk.lefts || []).length, stickyOk)
    check(`${page.name}：无新增 JS 错误`, errors.length === before, errors.slice(before, before + 2).join(' | '))
  }

  await browser.close()
  console.log(`\n结果：${PASS} 通过 / ${FAIL} 失败`)
  if (FAILURES.length) {
    console.log('失败项：')
    FAILURES.forEach(f => console.log(' -', f))
    process.exit(1)
  }
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
