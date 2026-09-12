/* 诊断：UI 脚本在「独立后端端口」下为何被踢回登录页
 * 运行：NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/probe-ui-auth.cjs [apiPort] [vitePort]
 */
const http = require('http')
const { chromium } = require('playwright')

const API = Number(process.argv[2] || 5678)
const VITE = Number(process.argv[3] || 5656)

function apiReq(method, p, body) {
  return new Promise((resolve, reject) => {
    const d = body == null ? null : JSON.stringify(body)
    const h = { 'Content-Type': 'application/json' }
    if (d) h['Content-Length'] = Buffer.byteLength(d)
    const r = http.request({ hostname: '127.0.0.1', port: API, path: '/api' + p, method, headers: h }, rs => {
      const ch = []
      rs.on('data', c => ch.push(c))
      rs.on('end', () => { try { resolve(JSON.parse(Buffer.concat(ch).toString('utf8'))) } catch { resolve({}) } })
    })
    r.on('error', reject)
    if (d) r.write(d)
    r.end()
  })
}

;(async () => {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username: 'e2e_product', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token
  console.log('token =', String(token).slice(0, 12))

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  const bad = []
  page.on('response', r => {
    const u = r.url()
    if (r.status() === 401 || r.status() === 403) bad.push(`${r.status()} ${u.replace(/^https?:\/\/[^/]+/, '')}`)
  })
  await page.route(u => u.pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    if (u.pathname.includes('/sse/')) return route.abort().catch(() => {})
    const resp = await route.fetch({ url: `http://127.0.0.1:${API}${u.pathname}${u.search}` })
    await route.fulfill({ response: resp })
  })
  await page.addInitScript(t => {
    localStorage.setItem('token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
  }, token)

  await page.goto(`http://localhost:${VITE}/erp/product/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(12000)
  const st = await page.evaluate(() => ({
    url: location.href,
    onLogin: !!document.querySelector('input[placeholder="请输入用户名"]'),
    tok: (localStorage.getItem('token') || '').slice(0, 12),
    rows: document.querySelectorAll('.ss-grid tbody tr').length,
  }))
  console.log('after goto:', JSON.stringify(st))
  console.log('401/403 请求:', JSON.stringify(bad.slice(0, 8)))
  await browser.close()
})().catch(e => { console.error('FATAL', e); process.exit(1) })
