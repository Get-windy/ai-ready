/* 资料模块回归：客户/供应商/物流 三页在 PartnerListPage 改造后仍正常渲染 */
const http = require('http')
const { chromium } = require('playwright')
const API = Number(process.argv[2] || 5677), VITE = Number(process.argv[3] || 5656)
function apiReq(method, p, body) {
  return new Promise((res, rej) => {
    const d = body == null ? null : JSON.stringify(body)
    const h = { 'Content-Type': 'application/json' }
    if (d) h['Content-Length'] = Buffer.byteLength(d)
    const r = http.request({ hostname: '127.0.0.1', port: API, path: '/api' + p, method, headers: h }, rs => {
      const ch = []; rs.on('data', c => ch.push(c)); rs.on('end', () => { try { res(JSON.parse(Buffer.concat(ch).toString())) } catch { res({}) } })
    }); r.on('error', rej); if (d) r.write(d); r.end()
  })
}
async function getToken() {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8').matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const r = await apiReq('POST', '/auth/login', { username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid })
  return r?.data?.token
}
;(async () => {
  const token = await getToken()
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  await page.route(u => u.pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    if (u.pathname.includes('/sse/')) return route.abort().catch(() => {})
    const resp = await route.fetch({ url: `http://127.0.0.1:${API}${u.pathname}${u.search}` })
    await route.fulfill({ response: resp })
  })
  await page.addInitScript(t => { localStorage.setItem('token', t); localStorage.setItem('tenantId', '1'); localStorage.setItem('tenantName', '系统租户') }, token)
  let fail = 0
  for (const [path, name, expectBtn] of [['/md/customer/index', '客户', '批量修改'], ['/md/supplier/index', '供应商', '导出'], ['/md/logistics/index', '物流公司', '导出']]) {
    await page.goto(`http://localhost:${VITE}${path}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(8000)
    if (await page.locator('input[placeholder="请输入租户名称"]').count()) {
      const t = await getToken()
      await page.evaluate(tk => localStorage.setItem('token', tk), t)
      await page.goto(`http://localhost:${VITE}${path}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
      await page.waitForTimeout(8000)
    }
    const info = await page.evaluate(() => ({
      tabs: document.querySelectorAll('.tab-item').length,
      table: document.querySelectorAll('.ss-grid').length,
      treeTitle: (document.querySelector('.category-title') || {}).innerText || '',
      buttons: [...document.querySelectorAll('.toolbar-right button, .toolbar-left button')].map(b => (b.innerText || '').trim()),
      rows: document.querySelectorAll('.ss-grid tbody tr').length,
      path: (document.querySelector('.category-breadcrumb') || {}).innerText || '',
    }))
    const ok = info.table > 0 && info.buttons.some(b => b.includes(expectBtn))
    if (!ok) fail++
    console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}页   tabs=${info.tabs} 树=${info.treeTitle} 行=${info.rows} 路径=${info.path.replace(/\s/g, '')} 按钮=${JSON.stringify(info.buttons)}`)
  }
  await page.screenshot({ path: 'I:/AI-Ready/tool-results/product-ui/md-customer.png' })
  await browser.close()
  console.log(fail ? `\n回归失败 ${fail} 页` : '\n资料模块回归 3/3 通过')
  process.exit(fail ? 1 : 0)
})()
