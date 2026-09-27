/*
 * 商城 C 端「需登录」页面的 UI 验证（2026-09-26）。
 *
 * 为什么单独写：首页/分类/消息是游客可达的（已有 verify-mall-c-ui.cjs 覆盖），
 * 而本轮修的**购物车 / 我的 / 订单 / 地址 / 个人资料 / 搜索 / 商品详情**都必须登录态才进得去。
 * 这些页恰是"调用了不存在的接口方法 / 响应体解包错 / 假数据兜底"的重灾区
 * （如订单列表把 PageResult 当数组 ⇒ 恒空；搜索把 keyword 传成对象 ⇒ 恒搜不到）。
 *
 * 做法：用**演示买家**（`tools/mall-demo-seed.sql` 里的 demo_buyer/admin123）
 *   走 C 端登录接口拿 token，注入 localStorage 后逐页访问，断言：
 *   ① 未被弹回登录页 ② 无 JS 报错 ③ 关键元素渲染。
 * 只读，不改任何数据；跑完不清理（演示账号是种子的一部分，长期保留）。
 *
 * 用法: node tools/verify-mall-c-ui-auth.cjs [前端地址=http://localhost:3012] [后端端口=5691]
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
async function db(sql, params) {
  const c = new Client(DSN); await c.connect()
  try { return (await c.query(sql, params)).rows } finally { await c.end() }
}

const FE = process.argv[2] || process.env.FE_URL || 'http://localhost:3012'
const PORT = Number(process.argv[3] || process.env.ERP_PORT || 5691)
const TENANT = process.env.SHOP_TENANT || '1'
const SHOTS = 'I:/AI-Ready/tool-results/mall-c'

function req(method, path, body, headers) {
  return new Promise((res, rej) => {
    const data = body ? JSON.stringify(body) : null
    const h = { 'Content-Type': 'application/json', 'X-Tenant-Id': TENANT, ...(headers || {}) }
    if (data) h['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path, method, headers: h }, resp => {
      const c = []
      resp.on('data', d => c.push(d))
      resp.on('end', () => {
        const b = Buffer.concat(c).toString('utf8')
        let j = null
        try { j = JSON.parse(b) } catch (e) { /* ignore */ }
        res({ status: resp.statusCode, body: b, json: j })
      })
    })
    r.on('error', rej)
    if (data) r.write(data)
    r.end()
  })
}

let pass = 0, fail = 0
const failures = []
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✔ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; failures.push(name); console.log(`  ✘ ${name}${detail ? ' — ' + detail : ''}`) }
}

;(async () => {
  console.log(`\nC 端「需登录」页面验证 —— 前端 ${FE}，后端 :${PORT}\n`)

  // ── 登录（C 端买家）──
  const login = await req('POST', '/api/v1/mall/auth/login', { username: 'demo_buyer', password: 'admin123' })
  const token = login.json?.data?.token
  const user = login.json?.data?.user
  check('演示买家登录成功（demo_buyer）', !!token, login.status === 200 ? '' : login.body.slice(0, 120))
  if (!token) { console.log('\n无法继续：登录失败'); process.exitCode = 1; return }

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 2 })

  const consoleErrors = []
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()) })
  page.on('pageerror', e => consoleErrors.push('pageerror: ' + e.message))

  // 注入登录态（user store 读的就是这两个键）
  await page.goto(`${FE}/?tenantId=${TENANT}`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([t, u]) => {
    localStorage.setItem('token', t)
    localStorage.setItem('user', JSON.stringify(u))
  }, [token, user])

  /** 访问一页并做通用断言 */
  async function visit(path, name, expectText, shotName) {
    consoleErrors.length = 0
    await page.goto(`${FE}${path}${path.includes('?') ? '&' : '?'}tenantId=${TENANT}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(3000)
    const url = page.url()
    check(`${name}：未被弹回登录页`, !url.includes('/login'), url.replace(FE, ''))
    const body = await page.evaluate(() => document.body.innerText)
    // 文本断言比类名可靠：类名会随组件版本变，而"页面上有没有这几个字"是用户可见的事实
    const hit = !expectText || body.includes(expectText)
    check(`${name}：页面内容渲染`, hit, expectText ? `期望含「${expectText}」` : '')
    const realErrors = consoleErrors.filter(t => !/status of 40[0-9]|Failed to load resource/i.test(t))
    check(`${name}：无 JS 报错`, realErrors.length === 0, realErrors.slice(0, 2).join(' | '))
    if (shotName) await page.screenshot({ path: `${SHOTS}/${shotName}.png`, fullPage: false })
  }

  console.log('── 逐页验证 ──')
  await visit('/user', '我的', '订单', 'auth-user')
  await visit('/cart', '购物车', '购物车', 'auth-cart')
  await visit('/order/list', '我的订单', '订单', 'auth-order-list')
  await visit('/user/address', '收货地址', '地址', 'auth-address')
  await visit('/user/profile', '个人资料', '昵称', 'auth-profile')
  await visit('/search?keyword=%E7%BD%90%E5%A4%B4', '搜索（罐头）', '', 'auth-search')

  // 搜到东西 = 搜索链路真的通了（原先 keyword 传成对象恒为空）
  const searchCount = await page.evaluate(() => document.querySelectorAll('.product-card').length)
  check('搜索能返回结果（keyword 正确传递）', searchCount > 0, `${searchCount} 张商品卡`)

  // 商品详情（游客也可，但顺带验）
  await page.goto(`${FE}/?tenantId=${TENANT}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2000)
  const firstCard = await page.$('.product-card')
  if (firstCard) {
    await firstCard.click()
    await page.waitForTimeout(2500)
    const url = page.url()
    check('商品详情：可打开', url.includes('/product/'), url.replace(FE, ''))
    const errs = consoleErrors.filter(t => !/status of 40[0-9]|Failed to load resource/i.test(t))
    check('商品详情：无 JS 报错', errs.length === 0, errs.slice(0, 2).join(' | '))
    await page.screenshot({ path: `${SHOTS}/auth-product-detail.png` })
  } else {
    check('商品详情：可打开', false, '首页没有商品卡，无法进入详情')
  }

  await browser.close()
  console.log(`\n═══ 汇总：${pass}/${pass + fail} 通过 ═══`)
  if (failures.length) console.log('失败项：' + failures.join(' | '))
  console.log(`截图：${SHOTS}/auth-*.png`)
  process.exitCode = fail ? 1 : 0
})().catch(e => { console.error('验证异常:', e.message); process.exitCode = 1 })
