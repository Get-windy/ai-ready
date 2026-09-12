/* ql361 商品页面对标抓取（复用登录态）
 * 运行： node tools/ql361-product-crawl.cjs [step]
 *   step: explore|page|all
 * 输出： tool-results/ql361/product/*
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const OUT = path.resolve(__dirname, '../tool-results/ql361/product')
const STATE = path.join(OUT, 'ql361-state.json')
const STEP = process.argv[2] || 'all'

function log(...a) { console.log('[p]', ...a) }

async function shot(page, name, fullPage = false) {
  await page.screenshot({ path: path.join(OUT, `${name}.png`), fullPage })
  log('shot ->', `${name}.png`)
}
function saveJson(name, data) {
  fs.writeFileSync(path.join(OUT, `${name}.json`), JSON.stringify(data, null, 2), 'utf8')
  log('json ->', `${name}.json`)
}

/** 在 page 及其所有 frame 中查找可见元素并点击 */
async function clickText(page, text, opts = {}) {
  for (const ctx of [page, ...page.frames().filter(f => f !== page.mainFrame())]) {
    const cands = [
      `text="${text}"`,
      `a:has-text("${text}")`,
      `li:has-text("${text}")`,
      `span:has-text("${text}")`,
      `div:has-text("${text}")`,
    ]
    for (const sel of cands) {
      const loc = ctx.locator(sel)
      const n = await loc.count().catch(() => 0)
      for (let i = 0; i < Math.min(n, 6); i++) {
        const el = loc.nth(i)
        const vis = await el.isVisible().catch(() => false)
        if (!vis) continue
        const t = (await el.innerText().catch(() => '')).trim()
        if (t.length > (opts.maxLen || 30)) continue
        await el.click({ timeout: 4000, force: !!opts.force }).catch(() => {})
        return true
      }
    }
  }
  return false
}

/** 返回当前"业务页面" frame（排除 desktop 外壳） */
function bizFrames(page) {
  return page.frames().filter(f => {
    const u = f.url()
    return u && u.includes('ql361.com') && !u.includes('desktop.html') && u !== 'about:blank' && !u.includes('errorlog')
  })
}

async function dumpPage(page, name) {
  const targets = bizFrames(page)
  const target = targets.length ? targets[targets.length - 1] : page
  const data = await target.evaluate(() => {
    const txt = e => (e ? (e.innerText || '').trim().replace(/\s+/g, ' ') : '')
    const vis = e => e.offsetParent !== null
    const r = { url: location.href, title: document.title }
    r.tabs = [...new Set([...document.querySelectorAll('[class*=tab] li, [class*=tab] [class*=item], [role=tab], .tab')]
      .filter(vis).map(txt).filter(t => t && t.length < 20))]
    r.buttons = [...new Set([...document.querySelectorAll('button, [class*=btn], [role=button]')]
      .filter(vis).map(txt).filter(t => t && t.length < 14))]
    r.labels = [...new Set([...document.querySelectorAll('label, .label, [class*=label]')]
      .filter(vis).map(txt).filter(t => t && t.length < 24))]
    r.columns = [...new Set([...document.querySelectorAll('[role=columnheader], th, .dgrid-header .dgrid-cell, [class*=header-cell]')]
      .filter(vis).map(txt).filter(t => t && t.length < 26))]
    r.placeholders = [...document.querySelectorAll('input[placeholder]')].filter(vis).map(e => e.getAttribute('placeholder'))
    r.bodyPreview = (document.body.innerText || '').replace(/\s+/g, ' ').slice(0, 1500)
    return r
  })
  saveJson(name, data)
  log(name, 'tabs=' + data.tabs.length, 'btns=' + data.buttons.length, 'cols=' + data.columns.length)
  log('  tabs:', JSON.stringify(data.tabs))
  log('  cols:', JSON.stringify(data.columns))
  log('  btns:', JSON.stringify(data.buttons))
  log('  placeholders:', JSON.stringify(data.placeholders))
  return data
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  page.on('pageerror', e => log('pageerror:', e.message.slice(0, 150)))

  const res = await loginQl361(page, ctx, log)
  log('login result:', JSON.stringify(res))
  if (!res.ok) { await shot(page, 'zz-login-fail'); await browser.close(); process.exit(1) }
  await page.waitForTimeout(6000)
  await shot(page, '10-desktop')
  log('url', page.url(), 'frames', page.frames().length)

  saveJson('10-desktop-frames', page.frames().map(f => f.url()))
  const menu = await page.evaluate(() => {
    const txt = e => (e ? (e.innerText || '').trim().replace(/\s+/g, ' ') : '')
    const vis = e => e.offsetParent !== null
    return {
      all: [...new Set([...document.querySelectorAll('a,li,span,div')].filter(vis).map(txt).filter(t => t && t.length <= 10))].slice(0, 200),
    }
  })
  saveJson('10-desktop-menu', menu)
  log('menu sample:', JSON.stringify(menu.all.slice(0, 80)))
  if (STEP === 'explore') { await browser.close(); return }

  // 资料 → 商品管理 → 商品
  log('click 资料:', await clickText(page, '资料'))
  await page.waitForTimeout(2500)
  await shot(page, '11-md-panel')
  log('click 商品管理:', await clickText(page, '商品管理'))
  await page.waitForTimeout(2500)
  await shot(page, '12-md-product-group')
  log('click 商品:', await clickText(page, '商品'))
  await page.waitForTimeout(6000)
  await shot(page, '13-product-list')
  await dumpPage(page, '13-product-list')

  await browser.close()
  log('done')
})()
