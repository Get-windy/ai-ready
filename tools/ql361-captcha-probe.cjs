/* 探测 ql361 登录页安全验证组件结构，并尝试自动完成滑块 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const OUT = path.resolve(__dirname, '../tool-results/ql361/product')

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(4000)
  await page.locator('input[type="text"]:visible').first().fill('15095664266')
  await page.locator('input[type="password"]:visible').first().fill('Ysh790921')
  await page.waitForTimeout(1500)

  const dump = await page.evaluate(() => {
    const out = { html: '', candidates: [] }
    const sel = ['.yidun_control', '.yidun_popup', '[class*="yidun"]', '[class*="captcha"]', '[class*="verify"]', '[class*="slide"]']
    for (const s of sel) {
      const els = document.querySelectorAll(s)
      if (els.length) {
        els.forEach((e, i) => {
          if (i === 0) out.candidates.push({ sel: s, count: els.length, html: e.outerHTML.slice(0, 4000) })
        })
      }
    }
    return out
  })
  fs.writeFileSync(path.join(OUT, 'probe-captcha.json'), JSON.stringify(dump, null, 2), 'utf8')
  console.log('candidates:', dump.candidates.map(c => c.sel + '#' + c.count).join(', '))

  // 列出 iframe
  const frames = page.frames().map(f => f.url())
  console.log('frames:', JSON.stringify(frames))
  await page.screenshot({ path: path.join(OUT, 'probe-login.png'), fullPage: false })
  await browser.close()
})()
