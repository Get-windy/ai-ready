/**
 * 诊断：分析模块「采购分析」页为何未被 e2e-analytics.cjs 判定为已改造
 * （E2E 判据 = 页面出现 .table-area 或 .ss-grid；该页两次运行均未命中）
 *
 * 运行：node tools/diag-analytics-purchase-analysis.cjs
 * 用 e2e_hr 登录（避开 e2e-analytics.cjs 占用的 e2e_analytics，防 sa-token 互踢）
 */
const http = require('http')
const fs = require('fs')
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BE = 5655
const FE = process.env.FE || 'http://localhost:5656'
const SHOTS = 'I:/AI-Ready/tool-results/e2e-analytics'

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

async function login() {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const r = await req('POST', '/auth/login', { username: 'e2e_hr', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid })
  return r.data
}

;(async () => {
  const info = await login()
  const token = info.token || info.accessToken
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 1000 } })
  const page = await ctx.newPage()
  const logs = []
  page.on('console', m => { if (m.type() === 'error' || m.type() === 'warning') logs.push(`[${m.type()}] ${m.text().slice(0, 300)}`) })
  page.on('pageerror', e => logs.push('[pageerror] ' + e.message.slice(0, 300)))
  page.on('response', r => { if (r.status() >= 400) logs.push(`[HTTP ${r.status()}] ${r.url().replace(FE, '')}`) })

  await page.addInitScript(({ token, tenantId, tenantName }) => {
    localStorage.setItem('token', token)
    localStorage.setItem('tenantId', String(tenantId || 1))
    localStorage.setItem('tenantName', tenantName || '系统租户')
  }, { token, tenantId: info.tenantId, tenantName: info.tenantName })

  await page.goto(FE + '/analytics/purchase-analysis', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(6000)

  const diag = await page.evaluate(() => {
    const txt = (document.body.innerText || '').replace(/\s+/g, ' ').slice(0, 600)
    return {
      url: location.href,
      hasTableArea: !!document.querySelector('.table-area'),
      hasSsGrid: !!document.querySelector('.ss-grid'),
      hasBillDetailTable: !!document.querySelector('.bill-detail-table, [class*=bill-detail]'),
      mainHtmlSample: (document.querySelector('#app, .layout-content, .main-content') || document.body).innerHTML.slice(0, 900),
      bodyText: txt,
      tabs: [...document.querySelectorAll('.tab-items .tab-item')].map(e => e.textContent.trim()),
      // Vue 错误边界通常会渲染这类文案
      errorBoundaryText: (document.querySelector('.error-boundary, [class*=error]') || {}).innerText || null
    }
  })

  fs.mkdirSync(SHOTS, { recursive: true })
  await page.screenshot({ path: SHOTS + '/采购分析-诊断.png', fullPage: false }).catch(() => {})

  console.log(JSON.stringify({ diag, logs: [...new Set(logs)] }, null, 1))
  await browser.close()
})().catch(e => { console.error('诊断异常', e); process.exitCode = 1 })
