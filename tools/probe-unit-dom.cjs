/* 探测商品单位明细表的 DOM 结构（固定列/按钮/行高），用于编写准确的验收断言
 * 运行：NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/probe-unit-dom.cjs [apiPort] [vitePort]
 */
const http = require('http')
const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

const API = Number(process.argv[2] || 5655)
const VITE = Number(process.argv[3] || 5656)
const OUT = path.resolve(__dirname, '../tool-results/product-ui')

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

async function getToken() {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  return res?.data?.token
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
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
  await page.addInitScript(t => {
    localStorage.setItem('token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
  }, token)
  await page.goto(`http://localhost:${VITE}/erp/product/create`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(9000)
  // 点两次「新增行」后再 dump，验证第 4/5 行与删除按钮禁用态
  await page.click('.unit-actions button').catch(() => {})
  await page.waitForTimeout(900)
  await page.click('.unit-actions button').catch(() => {})
  await page.waitForTimeout(900)
  // 点击第 5 行（末行）的删除按钮，验证行是否真的被移除
  const before = await page.locator('.unit-table-wrap .ss-grid tbody tr').count()
  await page.evaluate(() => {
    const tr = document.querySelectorAll('.unit-table-wrap .ss-grid tbody tr')[4]
    const b = tr && tr.querySelector('button')
    if (b) b.click()
  })
  await page.waitForTimeout(1500)
  const after = await page.locator('.unit-table-wrap .ss-grid tbody tr').count()
  console.log('DELETE-ROW-TEST before=', before, 'after=', after)
  // 是否出现「表格展开显示」折叠提示（说明末行被折叠）
  const foldHint = await page.locator('.unit-table-wrap').innerText()
    .then(t => t.includes('表格展开显示')).catch(() => false)
  console.log('FOLD-HINT-PRESENT =', foldHint)

  const info = await page.evaluate(() => {
    const wrap = document.querySelector('.unit-table-wrap')
    if (!wrap) return { err: 'no .unit-table-wrap' }
    const grids = [...wrap.querySelectorAll('.ss-grid')]
    const tables = [...wrap.querySelectorAll('table')].map(t => ({
      cls: String(t.className).slice(0, 60),
      headRows: t.querySelectorAll('thead tr').length,
      bodyRows: t.querySelectorAll('tbody tr').length,
      firstRowCells: [...(t.querySelector('tbody tr')?.children || [])].map(c => (c.innerText || '').trim().slice(0, 20)),
    }))
    const wrapRect = wrap.getBoundingClientRect()
    const gridRect = grids[0]?.getBoundingClientRect()
    const headRect = wrap.querySelector('thead')?.getBoundingClientRect()
    const rows = [...wrap.querySelectorAll('tbody tr')].map(tr => ({
      h: Math.round(tr.getBoundingClientRect().height),
      cells: [...tr.children].map(td => (td.innerText || '').trim().replace(/\s+/g, ' ').slice(0, 18)),
      buttons: [...tr.querySelectorAll('button')].map(b => ({
        title: b.getAttribute('title') || '',
        disabled: b.disabled,
        cls: String(b.className).slice(0, 40),
      })),
    }))
    return {
      wrapH: Math.round(wrapRect.height),
      gridH: gridRect ? Math.round(gridRect.height) : null,
      headH: headRect ? Math.round(headRect.height) : null,
      gridCount: grids.length,
      tableCount: tables.length,
      tables,
      rows,
      wrapStyle: wrap.getAttribute('style'),
      wrapInnerHead: wrap.innerHTML.slice(0, 1200),
    }
  })
  fs.writeFileSync(path.join(OUT, 'unit-dom.json'), JSON.stringify(info, null, 2), 'utf8')
  console.log(JSON.stringify(info, null, 2).slice(0, 4000))
  await page.screenshot({ path: path.join(OUT, 'unit-dom.png'), fullPage: true }).catch(() => {})
  await browser.close()
})().catch(e => { console.error('FATAL', e); process.exit(1) })
