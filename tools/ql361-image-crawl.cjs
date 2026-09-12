/* ql361 图片管理对标抓取：登录 → 资料/商品管理/图片管理 → 商品图片列表 + 图片空间 + 上传弹窗
 * 运行： node tools/ql361-image-crawl.cjs
 * 输出： tool-results/ql361/image/*.png + *.json
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const fs = require('fs')
const path = require('path')

const OUT = path.resolve(__dirname, '../tool-results/ql361/image')
const BASE = process.env.QL361_BASE || 'https://www.ql361.com'
const ACCOUNT = process.env.QL361_USER || '15095664266'
const PASSWORD = process.env.QL361_PWD || 'Ysh790921'

function log(...a) { console.log('[img]', ...a) }

async function shot(page, name, fullPage = true) {
  await page.screenshot({ path: path.join(OUT, `${name}.png`), fullPage })
  log('shot ->', `${name}.png`)
}

async function dump(page, name, fn) {
  const data = await page.evaluate(fn)
  fs.writeFileSync(path.join(OUT, `${name}.json`), JSON.stringify(data, null, 2), 'utf8')
  log('json ->', `${name}.json`)
  return data
}

const STRUCT = () => {
  const vis = el => el && el.offsetParent !== null
  const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
  const r = { url: location.href }
  r.tabs = [...new Set([...document.querySelectorAll('div,span,a')].filter(vis)
    .map(txt).filter(t => /^(商品图片列表|图片空间)$/.test(t)))]
  r.buttons = [...new Set([...document.querySelectorAll('button,a[class*=btn],[class*=btn]')].filter(vis)
    .map(txt).filter(t => t && t.length <= 16))]
  r.visibleInputs = [...document.querySelectorAll('input,select,textarea')].filter(vis).map(el => ({
    tag: el.tagName, type: el.type, ph: el.getAttribute('placeholder') || '', val: el.value,
    cls: String(el.className).slice(0, 60),
  }))
  r.labels = [...new Set([...document.querySelectorAll('label,span,i,b')].filter(e => vis(e) && e.children.length === 0)
    .map(txt).filter(t => t && t.length <= 20))]
  r.checkboxes = [...document.querySelectorAll('input[type=checkbox]')].filter(vis)
    .map(el => ({ checked: el.checked, label: txt(el.closest('label') || el.parentElement) }))
  r.headers = [...document.querySelectorAll('table thead th')].map(txt)
  r.rows = document.querySelectorAll('table tbody tr').length
  const firstRow = document.querySelector('table tbody tr')
  r.firstRowCells = firstRow ? [...firstRow.querySelectorAll('td')].map(c => ({ text: txt(c).slice(0, 60), html: c.innerHTML.slice(0, 300) })) : []
  r.panelText = (document.querySelector('[role=tabpanel],.dijitTabPane')?.innerText || document.body.innerText || '').slice(0, 4000)
  return r
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: process.env.HEADED !== '1' })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  page.on('pageerror', e => log('pageerror:', (e.message || '').slice(0, 200)))

  try {
    log('goto login')
    await page.goto(`${BASE}/Account/Logon`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(3000)
    await page.locator('input[type="text"]:visible').first().fill(ACCOUNT)
    await page.locator('input[type="password"]:visible').first().fill(PASSWORD)

    // 等待易盾安全验证组件初始化完成（"安全验证组件初始化中，请稍等"消失）
    for (let i = 0; i < 30; i++) {
      const waiting = await page.evaluate(() => (document.body.innerText || '').includes('安全验证组件初始化中'))
      if (!waiting) break
      await page.waitForTimeout(1000)
    }
    const nac = await page.evaluate(() => {
      const el = document.querySelector('input.yidun_input, input[name=NECaptchaValidate]')
      return { validate: el ? el.value : null, hasWidget: !!document.querySelector('.yidun_control') }
    })
    log('captcha state:', JSON.stringify(nac))

    await page.locator('button:has-text("登录"), input[type="submit"]').first().click()
    await page.waitForTimeout(9000)
    log('url after login click:', page.url())
    await shot(page, '00-after-login', false)

    // 若仍停留在登录页，尝试拖动易盾滑块
    if (page.url().includes('/Account/Logon')) {
      log('still on logon, try yidun drag')
      const slider = page.locator('.yidun_slider').first()
      if (await slider.count()) {
        const box = await slider.boundingBox()
        if (box) {
          await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
          await page.mouse.down()
          for (let i = 1; i <= 20; i++) {
            await page.mouse.move(box.x + box.width / 2 + i * 12, box.y + box.height / 2 + (i % 2), { steps: 2 })
            await page.waitForTimeout(40)
          }
          await page.mouse.up()
          await page.waitForTimeout(6000)
          log('url after drag:', page.url())
          await shot(page, '00b-after-drag', false)
        }
      } else {
        log('no slider widget')
      }
    }

    // 关闭登录后的「预警提醒」等自动弹窗
    const closePopups = async () => {
      for (let i = 0; i < 3; i++) {
        const closed = await page.evaluate(() => {
          const cands = [...document.querySelectorAll('[class*=close],[class*=Close],a,span,i,div')]
            .filter(e => e.offsetParent !== null && (e.innerText || '').trim() === '×')
          if (cands.length) { cands[0].click(); return true }
          return false
        })
        if (!closed) break
        await page.waitForTimeout(800)
      }
      await page.keyboard.press('Escape').catch(() => {})
      await page.waitForTimeout(800)
    }
    await closePopups()
    await shot(page, '00c-after-close-popup', false)

    // 资料 菜单
    await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true })
    await page.waitForTimeout(2000)
    await shot(page, '01-md-panel', false)

    // dump 资料弹出面板结构，便于定位子菜单
    const panelDump = await dump(page, '01-md-panel-dom', () => {
      const vis = el => el && el.offsetParent !== null
      const hits = [...document.querySelectorAll('a,span,div,li,p')].filter(e => vis(e) && e.children.length === 0
        && !!(e.innerText || '').trim() && (e.innerText || '').trim().length <= 12)
      return hits.slice(-200).map(e => ({ tag: e.tagName, cls: String(e.className).slice(0, 60), t: (e.innerText || '').trim() }))
    })
    log('panel last items:', JSON.stringify(panelDump.slice(-40)))

    // 商品管理 → 图片管理（点击弹出面板中的叶子文本）
    const clickVisibleText = async (name) => {
      const ok = await page.evaluate((n) => {
        const el = [...document.querySelectorAll('a,span,div,li')]
          .filter(e => e.offsetParent !== null && e.children.length === 0)
          .find(e => (e.innerText || '').trim() === n)
        if (el) { el.click(); return true }
        return false
      }, name)
      await page.waitForTimeout(1200)
      return ok
    }

    log('open 图片管理')
    const g2 = await clickVisibleText('图片管理')
    log('图片管理 clicked:', g2)
    await page.waitForTimeout(7000)
    await shot(page, '10-tab1-product-image-list')

    const s1 = await dump(page, '10-tab1', STRUCT)
    log('tab1 tabs:', JSON.stringify(s1.tabs))
    log('tab1 headers:', JSON.stringify(s1.headers))
    log('tab1 buttons:', JSON.stringify(s1.buttons.slice(0, 30)))

    // 列配置弹窗（齿轮）
    const gear = page.locator('i[class*=shezhi], i[class*=setting], [class*=icon-shezhi]').first()
    if (await gear.count()) {
      await gear.click({ timeout: 5000 }).catch(e => log('gear fail', e.message))
      await page.waitForTimeout(2000)
      await shot(page, '11-colconfig', false)
      const cc = await dump(page, '11-colconfig', STRUCT)
      log('colconfig text:', (cc.panelText || '').slice(0, 800))
      await page.keyboard.press('Escape').catch(() => {})
      await page.waitForTimeout(1000)
    } else {
      log('no gear found')
    }

    // 切到 图片空间
    log('switch 图片空间')
    const switched = await clickVisibleText('图片空间')
    log('switched:', switched)
    await page.waitForTimeout(5000)
    await shot(page, '20-tab2-image-space')
    const s2 = await dump(page, '20-tab2', STRUCT)
    log('tab2 buttons:', JSON.stringify(s2.buttons.slice(0, 40)))
    log('tab2 checkboxes:', JSON.stringify(s2.checkboxes))
    log('tab2 labels:', JSON.stringify(s2.labels.slice(0, 60)))
    log('tab2 panelText:', (s2.panelText || '').slice(0, 1200))

    // 上传图片 弹窗
    const upBtn = page.locator('button:has-text("上传图片"), [class*=btn]:has-text("上传图片")').first()
    if (await upBtn.count()) {
      await upBtn.click({ timeout: 5000 }).catch(e => log('upload click fail', e.message))
      await page.waitForTimeout(2500)
      await shot(page, '21-upload-modal', false)
      const um = await dump(page, '21-upload-modal', STRUCT)
      log('upload modal text:', (um.panelText || '').slice(0, 1200))
      await page.keyboard.press('Escape').catch(() => {})
      await page.waitForTimeout(1500)
    } else {
      log('no 上传图片 button')
    }

    // 回 商品图片列表，点某行「选择图片」
    await clickVisibleText('商品图片列表')
    await page.waitForTimeout(4000)
    const pick = page.locator('text=选择图片').first()
    if (await pick.count()) {
      await pick.click({ timeout: 5000 }).catch(e => log('pick click fail', e.message))
      await page.waitForTimeout(2500)
      await shot(page, '12-pick-image-modal', false)
      const pm = await dump(page, '12-pick-image-modal', STRUCT)
      log('pick modal text:', (pm.panelText || '').slice(0, 1500))
    } else {
      log('no 选择图片 cell')
    }

    await shot(page, '99-final', false)
  } catch (e) {
    log('FATAL', e.message)
    await shot(page, 'zz-error', false).catch(() => {})
  } finally {
    await browser.close()
  }
})()
