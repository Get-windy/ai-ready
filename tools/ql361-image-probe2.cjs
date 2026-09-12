/* ql361 图片管理探针2：精确点击「选择图片」，抓取弹层结构 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const fs = require('fs')
const path = require('path')
const OUT = path.resolve(__dirname, '../tool-results/ql361/image')
const BASE = process.env.QL361_BASE || 'https://www.ql361.com'
const log = (...a) => console.log('[p2]', ...a)
const shot = async (page, n) => { await page.screenshot({ path: path.join(OUT, `${n}.png`) }); log('shot', n) }
const dump = async (page, n, fn) => {
  const d = await page.evaluate(fn)
  fs.writeFileSync(path.join(OUT, `${n}.json`), JSON.stringify(d, null, 2), 'utf8')
  return d
}

// 抓所有「浮层」：position fixed/absolute 且 z-index 大、可见、有内容
const LAYERS = () => {
  const vis = el => el && el.offsetParent !== null
  const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
  const out = []
  document.querySelectorAll('body *').forEach(el => {
    const cs = getComputedStyle(el)
    const z = parseInt(cs.zIndex || '0', 10)
    if (!vis(el) || (cs.position !== 'fixed' && cs.position !== 'absolute')) return
    if (z < 100) return
    const t = txt(el)
    if (t.length < 8 || t.length > 4000) return
    out.push({
      tag: el.tagName, cls: String(el.className).slice(0, 90), z,
      rect: el.getBoundingClientRect().toJSON(),
      text: t.slice(0, 1500),
      headers: [...el.querySelectorAll('th')].map(txt).slice(0, 30),
      buttons: [...new Set([...el.querySelectorAll('button,a,[class*=btn]')].filter(vis).map(txt).filter(x => x && x.length <= 14))].slice(0, 30),
      inputs: [...el.querySelectorAll('input,select')].filter(vis).map(i => ({ t: i.type, ph: i.placeholder || '', v: i.value })).slice(0, 20),
    })
  })
  return out.sort((a, b) => b.z - a.z).slice(0, 6)
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  try {
    await page.goto(`${BASE}/Account/Logon`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(3000)
    await page.locator('input[type="text"]:visible').first().fill(process.env.QL361_USER || '15095664266')
    await page.locator('input[type="password"]:visible').first().fill(process.env.QL361_PWD || 'Ysh790921')
    for (let i = 0; i < 30; i++) {
      if (!(await page.evaluate(() => (document.body.innerText || '').includes('安全验证组件初始化中')))) break
      await page.waitForTimeout(1000)
    }
    await page.locator('button:has-text("登录"), input[type="submit"]').first().click()
    await page.waitForTimeout(9000)
    for (let i = 0; i < 4; i++) {
      const closed = await page.evaluate(() => {
        const c = [...document.querySelectorAll('*')].filter(e => e.offsetParent !== null && (e.innerText || '').trim() === '×')
        if (c.length) { c[0].click(); return true }
        return false
      })
      if (!closed) break
      await page.waitForTimeout(500)
    }
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(800)

    await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true })
    await page.waitForTimeout(2000)
    await page.evaluate(() => {
      const el = [...document.querySelectorAll('a,span,div,li')].filter(e => e.offsetParent !== null && e.children.length === 0)
        .find(e => (e.innerText || '').trim() === '图片管理')
      el && el.click()
    })
    await page.waitForTimeout(8000)
    log('opened, url=', page.url())

    // 精确点击单元格「选择图片」（真实鼠标点击）
    const cell = page.getByText('选择图片', { exact: true }).first()
    log('cell count:', await page.getByText('选择图片', { exact: true }).count())
    await cell.click({ timeout: 8000 }).catch(e => log('click err', e.message.slice(0, 120)))
    await page.waitForTimeout(3500)
    await shot(page, '40-pick-after-click')
    const l1 = await dump(page, '40-pick-layers', LAYERS)
    log('layers after 选择图片:', JSON.stringify(l1.map(x => ({ cls: x.cls, z: x.z, text: x.text.slice(0, 200) }))))

    // 若出现弹窗，尝试在里面操作（关闭）
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(1000)

    // 图片空间：选中一张图片，观察是否出现「选择/确认」按钮
    await page.evaluate(() => {
      const el = [...document.querySelectorAll('a,span,div,li')].filter(e => e.offsetParent !== null && e.children.length === 0)
        .find(e => (e.innerText || '').trim() === '图片空间')
      el && el.click()
    })
    await page.waitForTimeout(5000)
    // 点击第一张图片
    const img = page.locator('img').filter({ hasNot: page.locator('[alt=""]') }).nth(2)
    log('img count:', await page.locator('img').count())
    await img.click({ timeout: 6000 }).catch(e => log('img click err', e.message.slice(0, 100)))
    await page.waitForTimeout(2500)
    await shot(page, '41-space-after-img-click')
    const l2 = await dump(page, '41-space-layers', LAYERS)
    log('layers after img click:', JSON.stringify(l2.map(x => ({ cls: x.cls, z: x.z, text: x.text.slice(0, 200) }))))

    // 自动匹配（真实点击）
    const au = page.getByText('自动匹配', { exact: true }).first()
    if (await au.count()) {
      await au.click({ timeout: 6000 }).catch(e => log('auto err', e.message.slice(0, 100)))
      await page.waitForTimeout(2500)
      await shot(page, '42-automatch')
      const l3 = await dump(page, '42-automatch-layers', LAYERS)
      log('layers after 自动匹配:', JSON.stringify(l3.map(x => ({ cls: x.cls, z: x.z, text: x.text.slice(0, 300) }))))
    }
  } catch (e) {
    log('FATAL', e.message)
    await shot(page, 'zz3-error').catch(() => {})
  } finally {
    await browser.close()
  }
})()
