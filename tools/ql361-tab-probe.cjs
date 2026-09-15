/* ql361 页内 Tab 容器探测：打开指定页面后 dump 所有 dijitTabContainer 及其标签/几何
 * 运行： NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/ql361-tab-probe.cjs 分析 查库存
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const DOMAIN = process.argv[2] || '分析'
const PAGE = process.argv[3] || '查库存'
const STATE = path.resolve(__dirname, '../tool-results/ql361/product/ql361-state.json')

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
    // 打开菜单
    await page.locator(`a.menu-0-item:has-text("${DOMAIN}")`).first().click().catch(() => {})
    await page.waitForTimeout(1500)
    await page.evaluate((d) => {
      const a2 = [...document.querySelectorAll('a.menu-0-item')].find(x => (x.innerText || '').trim() === d)
      if (a2) ['mouseenter', 'mouseover', 'mousemove'].forEach(ev => a2.dispatchEvent(new MouseEvent(ev, { bubbles: true })))
    }, DOMAIN).catch(() => {})
    await page.waitForTimeout(1200)
    // 点页面项
    const clicked = await page.evaluate((nm) => {
      const root = [...document.querySelectorAll('.popupmenu__repeat')].find(d => d.getBoundingClientRect().width > 0)
      if (!root) return 'no popup'
      const it = [...root.querySelectorAll('.popupmenu__list-item')].find(e => (e.innerText || '').trim() === nm)
      if (!it) return 'no item'
      it.click(); return 'ok'
    }, PAGE)
    console.log('click:', clicked)
    await page.waitForTimeout(5000)

    const dump = await page.evaluate(() => {
      const out = { containers: [], panes: [], visibleTabs: [] }
      document.querySelectorAll('.dijitTabContainer').forEach((c, i) => {
        const r = c.getBoundingClientRect()
        const tabs = [...c.querySelectorAll(':scope > .dijitTabContainerTop-container > .dijitTabContainerTop-tabs .dijitTabInner, :scope > .dijitTabContainerTop-tabs .dijitTabInner')]
          .map(e => ({ t: e.innerText.trim(), checked: (e.className || '').includes('Checked'), y: Math.round(e.getBoundingClientRect().y), w: Math.round(e.getBoundingClientRect().width) }))
        out.containers.push({
          i, id: c.id, cls: (c.className || '').slice(0, 60),
          rect: { x: Math.round(r.x), y: Math.round(r.y), w: Math.round(r.width), h: Math.round(r.height) },
          inViewport: r.width > 0 && r.top >= 0 && r.top < window.innerHeight,
          tabCount: tabs.length, tabs
        })
      })
      document.querySelectorAll('.dijitTabPaneWrapper').forEach((p, i) => {
        const r = p.getBoundingClientRect()
        out.panes.push({ i, id: p.id, w: Math.round(r.width), h: Math.round(r.height), vis: p.offsetParent !== null })
      })
      document.querySelectorAll('.dijitTabInner').forEach(e => {
        const r = e.getBoundingClientRect()
        if (r.width > 0 && r.height > 0) out.visibleTabs.push({ t: e.innerText.trim(), y: Math.round(r.y), checked: (e.className || '').includes('Checked') })
      })
      return out
    })
    console.log(JSON.stringify(dump, null, 1).slice(0, 6000))
    await ctx.storageState({ path: STATE }).catch(() => {})
  } finally { await browser.close() }
})().catch(e => { console.error(e); process.exit(1) })
