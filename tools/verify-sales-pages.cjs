// 销售模块页面「打开后的真实健康状况」巡检（只读）。
//
// 与 verify-sales-routes.cjs 的分工：
//   · routes 脚本只回答「这个 URL 有没有路由」；
//   · 本脚本打开菜单的真实入口，收集途中**失败的接口请求**与**前端 console 报错**，
//     回答「这个页面打开后是不是好的」。
//
// 前置：后端 5655 + 前端 5656 都在跑。
// 用法：node tools/verify-sales-pages.cjs
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BASE = process.env.BASE || 'http://localhost:5656'
const API = process.env.ERP_API || 'http://localhost:5655'

// 菜单真实入口（path）+ 双入口的列表标签（list_path）
const PAGES = [
  ['70010 销售订单(表单入口)', '/sales/order/form'],
  ['70010 销售订单(列表标签)', '/sales/order/index'],
  ['70011 销售退货申请(表单入口)', '/sales/return-apply/form'],
  ['70011 销售退货申请(列表标签)', '/sales/return-apply/index'],
  ['70012 预订货单(表单入口)', '/sales/pre-order/form'],
  ['70012 预订货单(列表标签)', '/sales/pre-order/index'],
  ['70020 零售单(表单入口)', '/sales/retail/form'],
  ['70020 零售单(列表标签)', '/sales/retail/index'],
  ['70021 销售出库单(表单入口)', '/sales/outbound/form'],
  ['70021 销售出库单(列表标签)', '/sales/outbound/index'],
  ['70022 销售退货单(表单入口)', '/sales/return-doc/form'],
  ['70022 销售退货单(列表标签)', '/sales/return-doc/index'],
  ['70023 销售换货单(表单入口)', '/sales/exchange/form'],
  ['70023 销售换货单(列表标签)', '/sales/exchange/index'],
  ['70030 销售单据查询', '/sales/doc-query'],
  ['70031 销售明细查询', '/sales/detail-query'],
  ['70032 销售价格跟踪', '/sales/price-track'],
  ['80050 订单处理中心', '/sales/order-center'],
]

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function apiLogin() {
  const cap = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
  const cd = cap.data || cap
  const lg = await fetch(`${API}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: process.env.E2E_USER || 'sra_e2e',
      password: process.env.E2E_PASS || 'admin123',
      tenantName: '系统租户',
      captcha: extractCaptcha(cd.img), captchaKey: cd.uuid,
    }),
  }).then(r => r.json())
  const d = lg?.data || {}
  return { token: d.token || d.accessToken || d.tokenValue, tenantId: d.tenantId }
}

const IGNORE = [/favicon/i, /\.map$/, /hot-update/, /sockjs/, /\/api\/auth\/check$/, /\/api\/menu\/user/]

async function main() {
  const { token, tenantId } = await apiLogin()
  if (!token) { console.log('登录失败'); process.exit(1) }
  const browser = await chromium.launch({ headless: true })
  // ⚠️ 必须全程共用**同一个 context**（= 同一个 localStorage token）。
  //    第一版每个页面各 newPage + 重新 login：sa-token 是单端登录，第二次登录会把上一次踢下线，
  //    于是从被踢那页起，后面每一页都刷出成片的 `500 /api/auth/captcha`、`500 /api/auth/logout`
  //    与大量业务接口 500 —— 全是会话失效的连锁反应，不是页面自身缺陷。
  const context = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  await context.addInitScript(([tk, tid]) => {
    localStorage.setItem('token', tk); localStorage.setItem('tenantId', String(tid || 1))
  }, [token, tenantId])
  let problems = 0

  for (const [name, path] of PAGES) {
    const page = await context.newPage()
    const bad = []
    const cerr = []
    page.on('response', async r => {
      const u = r.url()
      if (IGNORE.some(re => re.test(u))) return
      if (r.status() >= 400) {
        // 只记业务接口，忽略静态资源
        if (u.includes('/api/')) bad.push(`${r.status()} ${r.request().method()} ${u.replace(API, '').replace(BASE, '')}`)
      }
    })
    page.on('console', m => { if (m.type() === 'error') cerr.push(m.text().slice(0, 160)) })
    page.on('pageerror', e => cerr.push('pageerror: ' + String(e).slice(0, 160)))

    if (path === PAGES[0][1]) {
      await page.goto(`${BASE}/dashboard`, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(4000)   // 首次进站等动态路由加载完成
    }

    await page.goto(`${BASE}${path}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(4500)
    const probe = await page.evaluate(() => ({
      notFound: !!document.querySelector('.not-found'),
      text: (document.body.innerText || '').slice(0, 300),
    })).catch(() => ({ notFound: false, text: '' }))

    const line = []
    if (probe.notFound) line.push('渲染 404 页')
    if (probe.text.includes('页面组件未找到')) line.push('组件未解析')
    if (bad.length) line.push('接口失败: ' + [...new Set(bad)].join(' | '))
    if (cerr.length) line.push('console: ' + [...new Set(cerr)].slice(0, 3).join(' | '))

    if (line.length) { problems++; console.log(`❌ ${name}  ${path}\n     ${line.join('\n     ')}`) }
    else console.log(`✅ ${name}  ${path}`)
    await page.close()
  }
  await browser.close()
  console.log(`\n有问题的页面：${problems} / ${PAGES.length}`)
}

main().catch(e => { console.error(e); process.exit(1) })
