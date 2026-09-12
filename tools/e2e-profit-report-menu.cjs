/* 利润表：菜单入口 + 导出 验证 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')

const FE = 'http://localhost:5656'
const SHOTS = 'I:/AI-Ready/tool-results/profit-report'

function apiReq(method, path, body) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: 5655, path: '/api' + path, method,
      headers: { 'Content-Type': 'application/json', ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}) },
    }, res => { let s = ''; res.on('data', c => { s += c }); res.on('end', () => resolve(JSON.parse(s))) })
    r.on('error', reject); if (data) r.write(data); r.end()
  })
}

;(async () => {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const lg = await apiReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = lg.data.token

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 950 }, acceptDownloads: true })
  const page = await ctx.newPage()
  await page.addInitScript(t => {
    localStorage.setItem('token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
  }, token)

  await page.goto(FE + '/finance/profit-report', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForSelector('tbody tr', { timeout: 60000 })
  await page.waitForTimeout(2500)

  // ── 点击侧边栏「财务」→ mega 菜单「财务报表」→「利润表」 ──
  const sidebarItems = await page.evaluate(() =>
    [...document.querySelectorAll('nav *, aside *, [class*=sidebar] *, [class*=menu] *')]
      .map(e => e.textContent.trim()).filter(t => t.length <= 6).slice(0, 30))
  console.log('侧边栏候选:', JSON.stringify([...new Set(sidebarItems)]))
  await page.getByText('财务', { exact: true }).first().click()
  await page.waitForTimeout(1500)
  await page.screenshot({ path: SHOTS + '/06-menu-finance.png' })
  const menuText = await page.evaluate(() => document.body.innerText.includes('财务报表'))
  console.log('侧边栏出现「财务报表」分组:', menuText)
  await page.click('text=财务报表')
  await page.waitForTimeout(1200)
  await page.screenshot({ path: SHOTS + '/07-menu-report-group.png' })
  const links = await page.evaluate(() => [...document.querySelectorAll('a,li,div')]
    .map(e => e.textContent.trim()).filter(t => t === '利润表').length)
  console.log('菜单中出现「利润表」节点数:', links)
  await page.click('text=利润表')
  await page.waitForTimeout(4000)
  console.log('跳转后 URL:', page.url())
  const tableOk = await page.evaluate(() => document.querySelectorAll('tbody tr').length)
  console.log('利润表行数:', tableOk)
  await page.screenshot({ path: SHOTS + '/08-from-menu.png' })

  // ── 导出 CSV ──
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 30000 }),
    page.click('button:has-text("导出")'),
  ])
  const path = await download.path()
  console.log('导出文件名:', download.suggestedFilename())
  const fs = require('fs')
  console.log('导出内容:\n' + fs.readFileSync(path, 'utf8').split('\n').slice(0, 8).join('\n'))

  // ── F8 打印快捷键不报错 ──
  await page.keyboard.press('F8')
  await page.waitForTimeout(1500)
  console.log('F8 快捷键触发完成（无异常）')

  await browser.close()
})().catch(e => { console.error('FAILED:', e.message); process.exit(1) })
