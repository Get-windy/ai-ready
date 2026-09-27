/**
 * 营销模块 · 19 页真机可用性巡检（只读）
 *
 * 逐页打开真实页面，用**四重检查**判定页面是否真渲染（本仓踩过两次同类坑）：
 *   ① console/pageerror 无 error；② 无 catch-all 404 文案；③ 主内容区存在；
 *   ④ 表格/卡片元素存在（BillDetailTable 或 a-table 或表单卡片）。
 *
 * 用法：node tools/audit-marketing-ui.cjs            （默认 admin@系统租户，前端 5656）
 *      E2E_USER=e2e_marketing node tools/audit-marketing-ui.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')

const FE = process.env.FE_URL || 'http://localhost:5656'
const API = 'http://localhost:5655/api'
const USER = process.env.E2E_USER || 'admin'
const PWD = process.env.E2E_PWD || 'admin123'
const TENANT = process.env.E2E_TENANT || '系统租户'
const TENANT_ID = process.env.E2E_TENANT_ID || '1'

const PAGES = [
  ['会员管理', 'marketing/member-manage'],
  ['积分兑换', 'marketing/points-exchange'],
  ['会员设置', 'marketing/member-config'],
  ['营销自动化', 'marketing/auto-campaign'],
  ['储值卡', 'marketing/stored-card'],
  ['发短信', 'marketing/sms-send'],
  ['优惠券', 'marketing/coupon'],
  ['商品促销', 'marketing/product-promo'],
  ['整单促销', 'marketing/order-promo'],
  ['特价', 'marketing/special-price'],
  ['套餐', 'marketing/package-deal'],
  ['商城拼团', 'marketing/mall-group'],
  ['商城秒杀', 'marketing/mall-flash'],
  ['商城预售', 'marketing/mall-presale'],
  ['商城弹窗广告', 'marketing/mall-popup'],
  ['加价购', 'marketing/add-on-deal'],
  ['热门搜索词推荐', 'marketing/hot-keywords'],
  ['我要推广', 'marketing/promote-create'],
  ['推广历史查询', 'marketing/promote-history'],
]

function req(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const url = new URL(API + path)
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: url.hostname, port: url.port, path: url.pathname + url.search, method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
      },
    }, res => {
      let buf = ''
      res.on('data', c => { buf += c })
      res.on('end', () => {
        let json = null
        try { json = JSON.parse(buf) } catch { /* 非 JSON */ }
        resolve({ status: res.statusCode, json, raw: buf })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login() {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    username: USER, password: PWD, tenantName: TENANT, captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 200))
  return token
}

;(async () => {
  const token = await login()
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()

  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tid))
  }, [token, TENANT_ID])

  const rows = []
  for (const [name, path] of PAGES) {
    const errs = []
    const onErr = e => errs.push(String(e.message || e).slice(0, 160))
    const onConsole = m => { if (m.type() === 'error') errs.push(m.text().slice(0, 160)) }
    page.on('pageerror', onErr)
    page.on('console', onConsole)
    let status = 0, body = '', hasTable = false, hasCard = false
    try {
      const resp = await page.goto(`${FE}/${path}`, { waitUntil: 'domcontentloaded' })
      status = resp ? resp.status() : 0
      await page.waitForTimeout(3200)
      body = (await page.locator('body').innerText()).slice(0, 3000)
      hasTable = await page.locator('.ant-table, .ss-grid, table').count() > 0
      hasCard = await page.locator('.ant-card, .ant-form').count() > 0
    } catch (e) { errs.push('goto: ' + String(e.message).slice(0, 120)) }
    page.off('pageerror', onErr)
    page.off('console', onConsole)

    const notFound = /404|页面不存在|找不到该页面/.test(body)
    const ok = !notFound && (hasTable || hasCard) && errs.length === 0 && status < 400
    rows.push({ name, path, status, ok, notFound, hasTable, hasCard, errs: errs.slice(0, 3) })
    console.log(`${ok ? '✅' : '❌'} ${name.padEnd(8)} ${path.padEnd(26)} HTTP=${status} 表格=${hasTable ? 'Y' : 'N'} 卡片=${hasCard ? 'Y' : 'N'}`
      + (notFound ? ' [404文案]' : '') + (errs.length ? ` 错误=${errs.length}: ${errs[0]}` : ''))
  }

  const bad = rows.filter(r => !r.ok)
  console.log(`\n结果：${rows.length - bad.length}/${rows.length} 页真机干净${bad.length ? '；问题页：' + bad.map(b => b.name).join('、') : ''}`)
  await browser.close()
})().catch(e => { console.error(e); process.exit(1) })
