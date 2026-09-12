/* ql361 图片管理深度探针：选择图片弹窗 / 图片空间布局 / 上传图片 / 自动匹配
 * 运行： node tools/ql361-image-probe.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const fs = require('fs')
const path = require('path')

const OUT = path.resolve(__dirname, '../tool-results/ql361/image')
const BASE = process.env.QL361_BASE || 'https://www.ql361.com'
const ACCOUNT = process.env.QL361_USER || '15095664266'
const PASSWORD = process.env.QL361_PWD || 'Ysh790921'

const log = (...a) => console.log('[probe]', ...a)
const shot = async (page, n, full = false) => {
  await page.screenshot({ path: path.join(OUT, `${n}.png`), fullPage: full })
  log('shot ->', n)
}
const dump = async (page, n, fn) => {
  const d = await page.evaluate(fn)
  fs.writeFileSync(path.join(OUT, `${n}.json`), JSON.stringify(d, null, 2), 'utf8')
  return d
}

// 抓取「弹窗」结构：可见的 layer / modal
const MODAL = () => {
  const vis = el => el && el.offsetParent !== null
  const txt = el => (el ? (el.innerText || '').trim() : '')
  const modals = [...document.querySelectorAll('[class*=layer],[class*=modal],[class*=dialog],[class*=dialogbox],[class*=popup]')]
    .filter(e => vis(e) && (e.innerText || '').length > 5)
  return modals.map(m => ({
    cls: String(m.className).slice(0, 100),
    title: txt(m.querySelector('[class*=title],[class*=header]')).slice(0, 60),
    text: txt(m).replace(/\s+/g, ' ').slice(0, 2000),
    buttons: [...new Set([...m.querySelectorAll('button,[class*=btn],a')].filter(vis).map(txt).filter(t => t && t.length <= 14))],
    inputs: [...m.querySelectorAll('input,select')].filter(vis).map(i => ({ type: i.type, ph: i.getAttribute('placeholder') || '', val: i.value })),
    headers: [...m.querySelectorAll('table thead th')].map(txt),
  }))
}

// 页面主结构（限定当前激活 tabpanel）
const PANEL = () => {
  const vis = el => el && el.offsetParent !== null
  const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
  const panels = [...document.querySelectorAll('[role=tabpanel],.dijitTabPane,.tabpane,.page-panel')].filter(vis)
  const panel = panels.sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0]
  const scope = panel || document.body
  const r = { panelCls: panel ? String(panel.className).slice(0, 80) : 'BODY' }
  r.headers = [...scope.querySelectorAll('table thead th')].map(txt)
  r.treeTexts = [...scope.querySelectorAll('[class*=tree] [class*=node], .dijitTreeNode')].filter(vis).map(txt).slice(0, 40)
  r.gridImages = [...scope.querySelectorAll('img')].filter(vis).map(i => ({ src: (i.src || '').slice(-60), w: i.naturalWidth })).slice(0, 30)
  r.imageNames = [...scope.querySelectorAll('[class*=item],[class*=card],[class*=grid]')].filter(e => vis(e) && e.querySelector('img'))
    .map(e => txt(e).slice(0, 60)).slice(0, 30)
  r.checkboxes = [...scope.querySelectorAll('input[type=checkbox]')].filter(vis).map(i => i.checked)
  r.buttons = [...new Set([...scope.querySelectorAll('button,[class*=btn]')].filter(vis).map(txt).filter(t => t && t.length <= 16))]
  r.selects = [...scope.querySelectorAll('select,[class*=select]')].filter(vis).map(e => txt(e).slice(0, 40)).slice(0, 10)
  r.text = (scope.innerText || '').replace(/\s+/g, ' ').slice(0, 2500)
  return r
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  try {
    await page.goto(`${BASE}/Account/Logon`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(3000)
    await page.locator('input[type="text"]:visible').first().fill(ACCOUNT)
    await page.locator('input[type="password"]:visible').first().fill(PASSWORD)
    for (let i = 0; i < 30; i++) {
      if (!(await page.evaluate(() => (document.body.innerText || '').includes('安全验证组件初始化中')))) break
      await page.waitForTimeout(1000)
    }
    await page.locator('button:has-text("登录"), input[type="submit"]').first().click()
    await page.waitForTimeout(9000)
    log('url:', page.url())

    // 关掉自动弹窗
    for (let i = 0; i < 4; i++) {
      const closed = await page.evaluate(() => {
        const c = [...document.querySelectorAll('*')].filter(e => e.offsetParent !== null && (e.innerText || '').trim() === '×')
        if (c.length) { c[0].click(); return true }
        return false
      })
      if (!closed) break
      await page.waitForTimeout(600)
    }
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(1000)

    await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true })
    await page.waitForTimeout(2000)

    const clickText = async (name, nth = 0) => page.evaluate(({ n, k }) => {
      const els = [...document.querySelectorAll('a,span,div,li,button')]
        .filter(e => e.offsetParent !== null && (e.innerText || '').trim() === n)
      if (els[k]) { els[k].click(); return true }
      return els.length
    }, { n: name, k: nth })

    log('open 图片管理:', await clickText('图片管理'))
    await page.waitForTimeout(7000)
    await shot(page, '30-tab1-clean')
    const t1 = await dump(page, '30-tab1-struct', PANEL)
    log('tab1 headers:', JSON.stringify(t1.headers))
    log('tab1 tree:', JSON.stringify(t1.treeTexts.slice(0, 20)))
    log('tab1 buttons:', JSON.stringify(t1.buttons))

    // 点第一个「选择图片」
    const picked = await clickText('选择图片')
    log('click 选择图片:', picked)
    await page.waitForTimeout(3000)
    await shot(page, '31-pick-modal')
    const pm = await dump(page, '31-pick-modal', MODAL)
    log('pick modal:', JSON.stringify(pm).slice(0, 1500))
    // 关闭弹窗
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(500)
    await page.evaluate(() => {
      const c = [...document.querySelectorAll('*')].filter(e => e.offsetParent !== null && (e.innerText || '').trim() === '×')
      if (c.length) c[0].click()
    })
    await page.waitForTimeout(1500)

    // 切 图片空间
    log('switch 图片空间:', await clickText('图片空间'))
    await page.waitForTimeout(5000)
    await shot(page, '32-space-clean')
    const t2 = await dump(page, '32-space-struct', PANEL)
    log('space text:', (t2.text || '').slice(0, 1500))
    log('space buttons:', JSON.stringify(t2.buttons))
    log('space checkboxes:', JSON.stringify(t2.checkboxes))
    log('space gridImages:', JSON.stringify(t2.gridImages.slice(0, 10)))
    log('space imageNames:', JSON.stringify(t2.imageNames.slice(0, 15)))

    // 图片空间 上传图片
    const upClicked = await clickText('上传图片')
    log('click 上传图片:', upClicked)
    await page.waitForTimeout(2500)
    await shot(page, '33-space-upload')
    const um = await dump(page, '33-space-upload', MODAL)
    log('upload modal:', JSON.stringify(um).slice(0, 1200))
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(800)
    await page.evaluate(() => {
      const c = [...document.querySelectorAll('*')].filter(e => e.offsetParent !== null && (e.innerText || '').trim() === '×')
      if (c.length) c[0].click()
    })
    await page.waitForTimeout(1500)

    // 自动匹配
    const auClicked = await clickText('自动匹配')
    log('click 自动匹配:', auClicked)
    await page.waitForTimeout(2500)
    await shot(page, '34-space-automatch')
    const am = await dump(page, '34-space-automatch', MODAL)
    log('automatch modal:', JSON.stringify(am).slice(0, 1200))
  } catch (e) {
    log('FATAL', e.message)
    await shot(page, 'zz2-error').catch(() => {})
  } finally {
    await browser.close()
  }
})()
