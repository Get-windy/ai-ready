/* 探索 ql361 桌面导航：资料 → 商品管理 → 商品 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const OUT = path.resolve(__dirname, '../tool-results/ql361/product')
const { loginQl361 } = require('./ql361-lib.cjs')

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  const log = (...a) => console.log('[nav]', ...a)

  const r = await loginQl361(page, ctx, log)
  log('login', JSON.stringify(r))
  await page.waitForTimeout(6000)

  // 左侧主菜单结构
  const nav = await page.evaluate(() => {
    const vis = e => { let n = e; while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement } return true }
    return [...document.querySelectorAll('*')]
      .filter(e => e.children.length === 0 && vis(e))
      .filter(e => ['资料', '商品管理', '商品'].includes((e.textContent || '').trim()))
      .map(e => {
        const path = []
        let n = e
        for (let i = 0; i < 5 && n; i++) { path.push(n.tagName + '.' + (typeof n.className === 'string' ? n.className.split(' ').slice(0, 3).join('.') : '')); n = n.parentElement }
        const r = e.getBoundingClientRect()
        return { text: e.textContent.trim(), path, rect: { x: Math.round(r.x), y: Math.round(r.y), w: Math.round(r.width), h: Math.round(r.height) } }
      })
  })
  fs.writeFileSync(path.join(OUT, 'nav-probe.json'), JSON.stringify(nav, null, 2), 'utf8')
  log('nav candidates:', JSON.stringify(nav, null, 1).slice(0, 2000))

  // 点击左侧「资料」
  const zl = nav.find(n => n.text === '资料' && n.rect.x < 120)
  if (zl) {
    await page.mouse.click(zl.rect.x + zl.rect.w / 2, zl.rect.y + zl.rect.h / 2)
    await page.waitForTimeout(2500)
  }
  await page.screenshot({ path: path.join(OUT, 'nav-01-after-ziliao.png') })
  const after = await page.evaluate(() => (document.body.innerText || '').replace(/\s+/g, ' ').slice(0, 1200))
  fs.writeFileSync(path.join(OUT, 'nav-after-ziliao.txt'), after, 'utf8')
  log('body after 资料:', after.slice(0, 600))
  await browser.close()
})()
