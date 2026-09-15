/* ql361 菜单弹层 DOM 结构探测（用于区分「可点页面项」与「分组/子分组标题」）
 * 运行： NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/ql361-menu-probe.cjs 分析
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const DOMAIN = process.argv[2] || '分析'
const STATE = path.resolve(__dirname, '../tool-results/ql361/product/ql361-state.json')
const OUT = path.resolve(__dirname, `../tool-results/ql361/${DOMAIN}-menu-dom.json`)

;(async () => {
  const browser = await chromium.launch({ headless: false, args: ['--window-size=1920,1000'] })
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1000 }, storageState: fs.existsSync(STATE) ? STATE : undefined })
  const page = await ctx.newPage()
  try {
    await page.goto('https://22stable.ql361.com/desktop.html', { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(4000)
    if (!(await page.evaluate(() => (document.body.innerText || '').includes('欢迎您')))) {
      await loginQl361(page, ctx, console.log); await page.waitForTimeout(3000)
    }
    await page.evaluate(() => {
      [...document.querySelectorAll('.dojoxDialogCloseIcon, .dijitDialogCloseIcon')].forEach(e => { try { e.click() } catch (e2) {} })
    }).catch(() => {})
    const a = page.locator(`a.menu-0-item:has-text("${DOMAIN}")`).first()
    await a.click().catch(() => {})
    await page.waitForTimeout(1500)
    await page.evaluate((d) => {
      const a2 = [...document.querySelectorAll('a.menu-0-item')].find(x => (x.innerText || '').trim() === d)
      if (a2) ['mouseenter', 'mouseover', 'mousemove'].forEach(ev => a2.dispatchEvent(new MouseEvent(ev, { bubbles: true })))
    }, DOMAIN).catch(() => {})
    await page.waitForTimeout(1200)

    const dump = await page.evaluate(() => {
      const root = [...document.querySelectorAll('.popupmenu__repeat')].find(d => d.getBoundingClientRect().width > 0)
      if (!root) return { err: 'no popup' }
      const walk = (el, depth) => {
        const r = el.getBoundingClientRect()
        const out = {
          d: depth,
          tag: el.tagName,
          cls: (el.className || '').toString().slice(0, 70),
          txt: (el.innerText || '').replace(/\s+/g, '|').slice(0, 40),
          h: Math.round(r.height),
          leaf: el.children.length === 0,
          onclick: !!(el.onclick || el.getAttribute('onclick') || (el.className || '').toString().includes('item'))
        }
        if (depth < 4 && el.children.length) out.children = [...el.children].map(c => walk(c, depth + 1))
        return out
      }
      return walk(root, 0)
    })
    fs.writeFileSync(OUT, JSON.stringify(dump, null, 2), 'utf8')
    console.log('dumped ->', OUT)
    await ctx.storageState({ path: STATE }).catch(() => {})
  } finally { await browser.close() }
})().catch(e => { console.error(e); process.exit(1) })
