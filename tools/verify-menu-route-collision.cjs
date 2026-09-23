/**
 * 验证：menu_code 重复的菜单，因前端路由 name 冲突导致页面 404
 *
 * 原理（读码确认）：
 *   frontend/apps/pc-admin/src/router/dynamicRoutes.ts:966
 *     name: menu.routeName || menu.menuCode
 *   ⇒ 两条菜单若 route_name 都为空且 menu_code 相同，会生成**同名路由**；
 *     Vue Router 对同名路由是「后者覆盖前者」⇒ path 不同的那一条永远无路由，落到 catch-all 404 页
 *     （症状隐蔽：URL 不变、无 console 错误，只是渲染 404 文案）
 *
 * 用例来自 sys_menu 的 menu_code 重复组（2026-09-23 实测 4 组）。
 * 运行：node tools/verify-menu-route-collision.cjs
 */
const http = require('http')
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BE = 5655
const FE = process.env.FE || 'http://localhost:5656'

// [菜单ID, 菜单名, path, 同组菜单ID]
const CASES = [
  [80421, '采购分析', 'analytics/purchase-analysis', '80422 采购/准备'],
  [80422, '采购/准备', 'analytics/purchase-prep', '80421 采购分析'],
  [70051, '库存预警补货', 'purchase/alert-replenish', '70052/70053 同 menu_code'],
  [70052, '缺货补货', 'purchase/shortage-replenish', '70051/70053 同 menu_code'],
  [70053, '智能补货', 'purchase/smart-replenish', '70051/70052 同 menu_code'],
  [70060, '采购订单', 'purchase/order/form', '70071 采购明细查询'],
  [70071, '采购明细查询', 'purchase/detail-query', '70060 采购订单'],
  [80091, '销售退货申请(80091)', 'sales/return-apply/form', '70011 同 path'],
  [70011, '销售退货申请(70011)', 'sales/return-apply/form', '80091 同 path']
]

function req(method, p, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: BE, path: '/api' + p, method,
      headers: { 'Content-Type': 'application/json', ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    }, res => {
      const c = []
      res.on('data', d => c.push(d))
      res.on('end', () => { const s = Buffer.concat(c).toString('utf8'); try { resolve(JSON.parse(s)) } catch { resolve(s) } })
    })
    r.on('error', reject); if (data) r.write(data); r.end()
  })
}

;(async () => {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const r = await req('POST', '/auth/login', { username: 'e2e_hr', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid })
  const info = r.data, token = info.token || info.accessToken

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 1000 } })
  const page = await ctx.newPage()
  await page.addInitScript(({ token, tenantId, tenantName }) => {
    localStorage.setItem('token', token)
    localStorage.setItem('tenantId', String(tenantId || 1))
    localStorage.setItem('tenantName', tenantName || '系统租户')
  }, { token, tenantId: info.tenantId, tenantName: info.tenantName })

  const rows = []
  for (const [id, name, path, note] of CASES) {
    await page.goto(FE + '/' + path, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(3200)
    const res = await page.evaluate(() => {
      const t = (document.body.innerText || '').replace(/\s+/g, ' ')
      return {
        is404: /404|页面不存在/.test(t),
        hasTable: !!document.querySelector('.table-area') || !!document.querySelector('.ss-grid'),
        rendered: (document.querySelector('.main-content, .layout-content, #app') || document.body).innerText.slice(0, 120).replace(/\s+/g, ' ')
      }
    })
    const verdict = res.is404 ? '❌ 404' : (res.hasTable ? '✅ 正常' : '⚠️ 打开但无表格')
    rows.push({ id, name, path, note, verdict, rendered: res.rendered.slice(0, 60) })
    console.log(`  ${verdict.padEnd(14)} ${id} ${name.padEnd(16)} /${path}`)
  }
  await browser.close()

  const fs = require('fs')
  const md = ['# menu_code 重复组 · 路由可达性实测（2026-09-23）', '',
    '原理：`dynamicRoutes.ts:966` 路由名 = `route_name || menu_code`；route_name 为空且 menu_code 相同的菜单会生成同名路由，Vue Router 后者覆盖前者。', '',
    '| 菜单ID | 菜单名 | path | 实测 | 同组 |', '|---|---|---|---|---|',
    ...rows.map(x => `| ${x.id} | ${x.name} | \`/${x.path}\` | ${x.verdict} | ${x.note} |`)]
  fs.mkdirSync('I:/AI-Ready/tool-results/analytics-audit', { recursive: true })
  fs.writeFileSync('I:/AI-Ready/tool-results/analytics-audit/route-collision.md', md.join('\n'), 'utf8')
  console.log('\n结果 → tool-results/analytics-audit/route-collision.md')
})().catch(e => { console.error('异常', e); process.exitCode = 1 })
