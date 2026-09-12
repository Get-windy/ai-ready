/* ql361 商品价格管理对标抓取（第三轮）：会话自愈 + 限定 tab pane 内点击 + 表头齿轮探测
 * 运行： node tools/ql361-product-price-crawl3.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const fs = require('fs')
const path = require('path')

const OUT = path.resolve(__dirname, '../tool-results/ql361/product-price')
const BASE = process.env.QL361_BASE || 'https://www.ql361.com'
const ACCOUNT = process.env.QL361_USER || '15095664266'
const PASSWORD = process.env.QL361_PWD || 'Ysh790921'

function log(...a) { console.log('[pp3]', ...a) }

async function shot(page, name, fullPage = false) {
  await page.screenshot({ path: path.join(OUT, `${name}.png`), fullPage })
  log('shot ->', `${name}.png`)
}

async function dump(page, name, fn) {
  const data = await page.evaluate(fn)
  fs.writeFileSync(path.join(OUT, `${name}.json`), JSON.stringify(data, null, 2), 'utf8')
  log('json ->', `${name}.json`)
  return data
}

/** 会话被踢自愈：检测「重新登录」弹窗并输密码 */
async function healRelogin(page) {
  for (let i = 0; i < 4; i++) {
    const has = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null
      return [...document.querySelectorAll('[class*=dialog],[role=dialog]')].filter(vis)
        .some(e => (e.innerText || '').includes('重新登录'))
    }).catch(() => false)
    if (!has) return true
    log('RE-LOGIN detected, healing')
    await page.evaluate((pwd) => {
      const vis = el => el && el.offsetParent !== null
      const dlg = [...document.querySelectorAll('[class*=dialog],[role=dialog]')].filter(vis)
        .find(e => (e.innerText || '').includes('重新登录'))
      if (!dlg) return
      const inp = dlg.querySelector('input[type=password]')
      if (inp) {
        const s = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set
        s.call(inp, pwd)
        inp.dispatchEvent(new Event('input', { bubbles: true }))
        inp.dispatchEvent(new Event('change', { bubbles: true }))
      }
      const btns = [...dlg.querySelectorAll('button,[class*=btn],span,div')].filter(vis)
      const ok = btns.find(b => (b.innerText || '').trim() === '确定')
      if (ok) ok.click()
    }, PASSWORD).catch(() => {})
    await page.waitForTimeout(4000)
  }
  return false
}

/** 取当前活动页签内容容器（限定点击范围，避免点到桌面/其它标签） */
const PANE = (page) => {
  const vis = el => el && el.offsetParent !== null
  const panes = [...document.querySelectorAll('.dijitTabPane, .dijitContentPane, [role=tabpanel]')].filter(vis)
  return panes.filter(p => /商品价格批量修改|等级指定价|客户指定价|级别指定价/.test(p.innerText || '')).pop() || null
}

async function clickInPane(page, name) {
  const ok = await page.evaluate(({ n, pwd }) => {
    const vis = el => el && el.offsetParent !== null
    const panes = [...document.querySelectorAll('.dijitTabPane, .dijitContentPane,[role=tabpanel]')].filter(vis)
    const pane = panes.filter(p => /商品价格批量修改|指定价|折扣设置/.test(p.innerText || '')).pop()
    if (!pane) return 'NO_PANE'
    const el = [...pane.querySelectorAll('a,span,div,button,td')].filter(e => vis(e) && e.children.length === 0
      && (e.innerText || '').trim() === n)
    if (!el.length) return 'NO_EL'
    el[el.length - 1].click()
    return 'OK'
  }, { n: name, pwd: PASSWORD })
  await page.waitForTimeout(3000)
  return ok
}

const DLG = () => {
  const vis = el => el && el.offsetParent !== null
  const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
  const roots = [...document.querySelectorAll('[role=dialog], [class*=dialog], [class*=Dialog]')].filter(vis)
  roots.sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)
  const root = roots[0]
  if (!root) return { none: true }
  return {
    dialogClass: String(root.className).slice(0, 80),
    text: (root.innerText || '').slice(0, 9000),
    tabs: [...root.querySelectorAll('[role=tab], .dijitTab, [class*=tab]')].filter(vis).map(txt).filter(t => t && t.length < 20),
    checks: [...root.querySelectorAll('input[type=checkbox]')].filter(vis).map(el => ({
      checked: el.checked, label: txt(el.closest('label') || el.parentElement).slice(0, 40),
    })),
    inputs: [...root.querySelectorAll('input,select,textarea')].filter(vis).map(el => ({
      tag: el.tagName, type: el.type, ph: el.getAttribute('placeholder') || '', val: el.value,
      cls: String(el.className).slice(0, 50),
    })),
  }
}

async function closeDlg(page) {
  await page.keyboard.press('Escape').catch(() => {})
  await page.waitForTimeout(1000)
  const still = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null
    return [...document.querySelectorAll('[role=dialog],[class*=dialog]')].filter(vis)
      .filter(e => (e.innerText || '').length > 40).length
  })
  if (still) {
    await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null
      const dlg = [...document.querySelectorAll('[role=dialog],[class*=dialog]')].filter(vis)
        .sort((a, b) => (b.innerText || '').length - (a.innerText || '').length)[0]
      if (!dlg) return
      const x = [...dlg.querySelectorAll('[class*=close],[class*=Close],span,i,a')].filter(vis)
        .find(e => (e.innerText || '').trim() === '×' || /close/i.test(String(e.className)))
      if (x) x.click()
    }).catch(() => {})
    await page.waitForTimeout(1000)
  }
}

async function colConfig(page, tag) {
  const info = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null
    const panes = [...document.querySelectorAll('.dijitTabPane, .dijitContentPane')].filter(vis)
    const pane = panes.filter(p => /商品价格批量修改|指定价|折扣设置/.test(p.innerText || '')).pop()
    if (!pane) return { none: true }
    const all = [...pane.querySelectorAll('[class*=shezhi],[class*=setting],[class*=Setting],[class*=column]')].filter(vis)
    return {
      cands: all.map(e => ({ tag: e.tagName, cls: String(e.className).slice(0, 70), title: e.getAttribute('title') || '', t: (e.innerText || '').trim().slice(0, 20) })),
    }
  })
  log(tag, 'gear candidates:', JSON.stringify(info.cands || info))
  const clicked = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null
    const panes = [...document.querySelectorAll('.dijitTabPane, .dijitContentPane')].filter(vis)
    const pane = panes.filter(p => /商品价格批量修改|指定价|折扣设置/.test(p.innerText || '')).pop()
    if (!pane) return 'NO_PANE'
    const g = pane.querySelector('.dgrid-column-set .icon-shezhi2') ||
      pane.querySelector('.dgrid-header .icon-shezhi2') ||
      pane.querySelector('[class*=shezhi]')
    if (!g) return 'NO_GEAR'
    g.click()
    return 'OK'
  })
  log(tag, 'gear click:', clicked)
  await page.waitForTimeout(2500)
  await shot(page, `${tag}-colconfig`)
  const d = await dump(page, `${tag}-colconfig`, DLG)
  log(tag, 'colconfig text:', (d.text || '').slice(0, 2000))
  log(tag, 'colconfig checks:', JSON.stringify((d.checks || []).slice(0, 130)))
  await closeDlg(page)
  return d
}

async function toolbar(page, name, tag) {
  const r = await clickInPane(page, name)
  log(`toolbar ${name}:`, r)
  if (r !== 'OK') return null
  await page.waitForTimeout(2500)
  await shot(page, tag)
  const d = await dump(page, `${tag}-dlg`, DLG)
  log(`${tag} text:`, (d.text || '').slice(0, 3000))
  log(`${tag} checks:`, JSON.stringify((d.checks || []).slice(0, 60)))
  log(`${tag} inputs:`, JSON.stringify((d.inputs || []).slice(0, 40)))
  log(`${tag} tabs:`, JSON.stringify(d.tabs))
  await closeDlg(page)
  return d
}

async function switchTab(page, label) {
  const via = await page.evaluate((label) => {
    try {
      const reg = window.dijit && window.dijit.registry
      if (reg) {
        for (const tc of reg.toArray().filter(w => (w.declaredClass || '').includes('TabContainer'))) {
          const hit = (tc.getChildren() || []).find(c => ((c.title || '').trim()) === label)
          if (hit) { tc.selectChild(hit); return 'selectChild' }
        }
      }
    } catch (e) {}
    return null
  }, label)
  if (!via) {
    await page.evaluate((label) => {
      const vis = el => el && el.offsetParent !== null
      const t = [...document.querySelectorAll('.dijitTab')].filter(vis).find(e => (e.innerText || '').trim() === label)
      if (t) t.click()
    }, label)
  }
  await page.waitForTimeout(3000)
  log('switch', label, '->', via || 'click')
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: process.env.HEADED !== '1' })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  page.on('pageerror', e => log('pageerror:', (e.message || '').slice(0, 200)))

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
    if (page.url().includes('/Account/Logon')) {
      const slider = page.locator('.yidun_slider').first()
      if (await slider.count()) {
        const box = await slider.boundingBox()
        if (box) {
          await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
          await page.mouse.down()
          for (let i = 1; i <= 24; i++) {
            await page.mouse.move(box.x + box.width / 2 + i * 11, box.y + box.height / 2 + (i % 3), { steps: 2 })
            await page.waitForTimeout(40)
          }
          await page.mouse.up()
          await page.waitForTimeout(6000)
        }
      }
    }
    log('url:', page.url())
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(800)

    const openPage = async () => {
      await page.evaluate(() => {
        const vis = el => el && el.offsetParent !== null
        for (const t of [...document.querySelectorAll('.dijitTab')].filter(vis)) {
          const text = (t.innerText || '').trim()
          if (t.classList.contains('dijitTabSelected') || text.includes('商品价格管理')) continue
          const close = t.querySelector('.dijitTabCloseButton')
          if (close) close.click()
        }
      }).catch(() => {})
      await page.waitForTimeout(1000)
      await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true })
      await page.waitForTimeout(2200)
      await page.evaluate(() => {
        const el = [...document.querySelectorAll('a,span,div,li')]
          .filter(e => e.offsetParent !== null && e.children.length === 0)
          .find(e => (e.innerText || '').trim() === '商品管理')
        if (el) el.click()
      })
      await page.waitForTimeout(1500)
      await page.evaluate(() => {
        const el = [...document.querySelectorAll('a,span,div,li')]
          .filter(e => e.offsetParent !== null && e.children.length === 0)
          .find(e => (e.innerText || '').trim() === '商品价格管理')
        if (el) el.click()
      })
      await page.waitForTimeout(9000)
    }
    await openPage()
    await healRelogin(page)
    await shot(page, '200-open')

    // 子标签1
    await colConfig(page, '211-tab1')
    await toolbar(page, '批量修改', '212-tab1-batchmod')

    // 子标签2
    await switchTab(page, '客户级别折扣设置')
    await healRelogin(page)
    await shot(page, '220-tab2')
    await colConfig(page, '221-tab2')
    await toolbar(page, '新增', '222-tab2-add')

    // 子标签3
    await switchTab(page, '级别指定价设置')
    await healRelogin(page)
    await shot(page, '230-tab3')
    await colConfig(page, '231-tab3')
    await toolbar(page, '新增', '232-tab3-add')

    // 子标签4
    await switchTab(page, '客户指定价设置')
    await healRelogin(page)
    await shot(page, '240-tab4')
    await colConfig(page, '241-tab4')
    await toolbar(page, '新增', '242-tab4-add')

    await shot(page, '299-final')
  } catch (e) {
    log('FATAL', e.message)
    await shot(page, 'zz-error').catch(() => {})
  } finally {
    await browser.close()
  }
}

main()
