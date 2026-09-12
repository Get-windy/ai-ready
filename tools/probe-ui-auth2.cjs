/* 诊断2：在页面上下文内直连独立后端完成登录（绕开 vite 代理指向的共享 5655）
 * 运行：NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/probe-ui-auth2.cjs [apiPort] [vitePort]
 */
const { chromium } = require('playwright')

const API = Number(process.argv[2] || 5678)
const VITE = Number(process.argv[3] || 5656)

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  const bad = []
  page.on('response', r => {
    if (r.status() === 401 || r.status() === 403) bad.push(`${r.status()} ${r.url().replace(/^https?:\/\/[^/]+/, '')}`)
  })

  await page.goto(`http://localhost:${VITE}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(3000)

  const loginRes = await page.evaluate(async ({ base }) => {
    try {
      const cap = await fetch(base + '/api/auth/captcha').then(r => r.json())
      const svg = atob(cap.data.img.split(',')[1])
      const code = [...svg.matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
      const res = await fetch(base + '/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'e2e_product', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid }),
      }).then(r => r.json())
      if (res?.data?.token) {
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('tenantId', String(res.data.tenantId || 1))
        localStorage.setItem('tenantName', res.data.tenantName || '系统租户')
      }
      return { code: res?.code, hasToken: !!res?.data?.token, msg: res?.message }
    } catch (e) { return { err: String(e) } }
  }, { base: `http://127.0.0.1:${API}` })
  console.log('页面内登录:', JSON.stringify(loginRes))

  // 页面后续请求仍会走 vite 代理（→5655）与本实例混用，故再用 route 把 /api/ 统一转发到本实例
  await page.route(u => u.pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    if (u.pathname.includes('/sse/')) return route.abort().catch(() => {})
    const resp = await route.fetch({ url: `http://127.0.0.1:${API}${u.pathname}${u.search}` })
    await route.fulfill({ response: resp })
  })

  await page.goto(`http://localhost:${VITE}/erp/product/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(12000)
  const st = await page.evaluate(() => ({
    url: location.href,
    onLogin: !!document.querySelector('input[placeholder="请输入用户名"]'),
    rows: document.querySelectorAll('.ss-grid tbody tr').length,
  }))
  console.log('after goto:', JSON.stringify(st))
  console.log('401/403:', JSON.stringify(bad.slice(0, 6)))
  await browser.close()
})().catch(e => { console.error('FATAL', e); process.exit(1) })
