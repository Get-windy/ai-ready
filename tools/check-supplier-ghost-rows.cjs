/* 供应商列表：20 行占位空行 + 占位行不渲染行操作/附件（与客户页口径一致）
 * 用法：NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/check-supplier-ghost-rows.cjs
 */
const { chromium } = require('playwright')

const APP = process.env.APP_URL || 'http://localhost:5656'
const API = process.env.API_URL || 'http://localhost:5671'
const OUT = 'I:/AI-Ready/screenshots-customer'

async function solveSvgCaptcha(page) {
  const svg = await page.evaluate(() => {
    const imgs = [...document.querySelectorAll('img')].filter(i => (i.src || '').startsWith('data:image'))
    if (!imgs.length) return ''
    const src = imgs[imgs.length - 1].src
    try {
      return src.includes('base64,')
        ? decodeURIComponent(escape(atob(src.split('base64,')[1])))
        : decodeURIComponent(src.split(',').slice(1).join(','))
    } catch { return '' }
  })
  return [...String(svg).matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: API + u.pathname + u.search })
  })

  await page.goto(APP + '/login', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(4000)
  await page.locator('input[placeholder*="租户"]').first().fill('系统租户')
  await page.locator('input[placeholder*="用户名"]').first().fill('admin')
  await page.locator('input[placeholder*="密码"]').first().fill('admin123')
  await page.locator('input[placeholder*="验证码"]').first().fill(await solveSvgCaptcha(page))
  await page.locator('button:has-text("登 录"), button:has-text("登录")').first().click()
  await page.waitForTimeout(8000)

  await page.goto(APP + '/md/supplier/index', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(8000)

  const url = page.url()
  check('S1 供应商列表页可访问', url.includes('/md/supplier/index'), url)

  const rows = await page.locator('.ss-grid tr.ss-row').count()
  check('S2 数据表默认 20 行（不足补空白占位行）', rows >= 20, `渲染行数=${rows}`)

  const ghostBtns = await page.locator('.ss-grid tr.ss-row a:has-text("订货"), .ss-grid tr.ss-row a:has-text("修改")').count()
  const dataRows = await page.locator('.ss-grid tr.ss-row').filter({ hasNot: page.locator('.ss-empty-cell') }).count()
  check('S3 占位空行不渲染行操作', ghostBtns <= dataRows * 1, `操作按钮数=${ghostBtns} 数据行≈${dataRows}`)

  await page.screenshot({ path: `${OUT}/07-supplier-ghost-rows.png` })

  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 供应商列表占位行检查：${pass}/${results.length} 通过 =====`)
  results.filter(r => !r.ok).forEach(r => console.log('  -', r.name, r.detail || ''))
  await browser.close()
  if (pass < results.length) process.exit(1)
})().catch(e => { console.error('FATAL', e.message); process.exit(1) })
