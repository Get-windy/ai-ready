/* ql361 商品页面对标抓取 v3（一次运行只做一个任务，避免状态污染）
 * 用法： NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/probe-product.cjs <task>
 *   task = list | sub:套餐 | sub:商品上架 | sub:商品授权 | form | form:商品图片 | form:商城信息 | flags
 * 输出： tool-results/ql361/product2/*
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const OUT = path.resolve(__dirname, '../tool-results/ql361/product2')
const TASK = process.argv[2] || 'list'
fs.mkdirSync(OUT, { recursive: true })

const log = (...a) => console.log('[p]', ...a)
const saveJson = (name, data) => {
  fs.writeFileSync(path.join(OUT, `${name}.json`), JSON.stringify(data, null, 2), 'utf8')
  log('json ->', `${name}.json`)
}
const shot = async (page, name, full = true) => {
  await page.screenshot({ path: path.join(OUT, `${name}.png`), fullPage: full }).catch(() => {})
  log('shot ->', `${name}.png`)
}

/** 当前业务 pane 的文本 */
const paneText = (page) => page.evaluate(() => {
  const vis = el => el && el.offsetParent !== null
  const panes = [...document.querySelectorAll('.dijitTabPane')].filter(vis)
  return panes.length ? (panes[panes.length - 1].innerText || '') : ''
}).catch(() => '')

/** 打开 资料 → 商品 列表页 */
async function openProductList(page) {
  for (let i = 1; i <= 5; i++) {
    await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {})
    await page.waitForTimeout(2500)
    const r = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null
      const items = [...document.querySelectorAll('.popupmenu__nav-text')].filter(vis)
      const el = items.find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品')
      if (!el) return 'notfound'
      el.click()
      return 'ok'
    }).catch(e => 'err:' + e.message)
    log(`openProductList #${i} ->`, r)
    if (r !== 'ok') { await page.waitForTimeout(2000); continue }
    await page.waitForTimeout(9000)
    const t = await paneText(page)
    if (t.includes('全部商品')) return true
    log('  未就绪，重试')
  }
  return false
}

/** dojo registry 切子 Tab */
async function selectTab(page, label) {
  const r = await page.evaluate((name) => {
    const key = name.replace(/[\s*]/g, '')
    try {
      const reg = window.dijit && window.dijit.registry
      if (reg) {
        const cs = reg.toArray().filter(w => w && String(w.declaredClass || '').indexOf('TabContainer') >= 0)
        for (let i = cs.length - 1; i >= 0; i--) {
          const kids = cs[i].getChildren ? cs[i].getChildren() : []
          const hit = kids.find(k => String(k.title || k.label || '').replace(/[\s*]/g, '') === key)
          if (hit) { cs[i].selectChild(hit); return 'dojo#' + (cs[i].id || i) }
        }
      }
      // DOM 兜底：点击 tabLabel / dijitTab 文本
      const vis = el => el && (el.offsetParent !== null || getComputedStyle(el).position === 'fixed')
      const cands = [...document.querySelectorAll('.tabLabel,.dijitTab,.dijitTabButton,[role=tab],li,div,span')]
        .filter(vis).filter(e => (e.innerText || '').trim().replace(/[\s*]/g, '') === key)
      if (!cands.length) return 'notfound'
      cands.sort((a, b) => a.querySelectorAll('*').length - b.querySelectorAll('*').length)
      cands[0].click()
      return 'dom'
    } catch (e) { return 'err:' + e.message }
  }, label).catch(e => 'exc:' + e.message)
  log('selectTab', label, '->', r)
  return r === 'dojo' || String(r).startsWith('dojo#') || r === 'dom'
}

/** dump 当前 pane 结构 */
async function dumpPane(page, name, extra = {}) {
  const data = await page.evaluate((extraKeys) => {
    const vis = el => el && el.offsetParent !== null
    const panes = [...document.querySelectorAll('.dijitTabPane')].filter(vis)
    const scope = panes[panes.length - 1] || document.body
    const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
    const q = sel => [...scope.querySelectorAll(sel)].filter(vis)
    const out = {
      paneText: (scope.innerText || '').slice(0, 12000),
      headers: [...new Set(q('th,.dgrid-header .dgrid-cell').map(txt).filter(t => t && t.length < 30))],
      buttons: [...new Set(q('button,[class*=btn],[class*=Button]').map(txt).filter(t => t && t.length < 16))],
      labels: [...new Set(q('label,.label,[class*=label]').map(txt).filter(t => t && t.length < 30))],
      inputs: q('input,select,textarea').map(e => ({
        name: e.name || e.id || '', ph: e.placeholder || '', type: e.type || '', val: (e.value || '').slice(0, 60),
      })),
      cbs: q('input[type=checkbox]').map(e => {
        const lab = e.closest('label') || e.parentElement
        return { label: (lab ? lab.innerText || '' : '').trim().replace(/\s+/g, ' ').slice(0, 40), checked: e.checked }
      }),
      radios: q('input[type=radio]').map(e => {
        const lab = e.closest('label') || e.parentElement
        return { label: (lab ? lab.innerText || '' : '').trim().replace(/\s+/g, ' ').slice(0, 40), checked: e.checked }
      }),
      tabs: [...new Set(q('.dijitTab,.tabLabel').map(txt).filter(t => t && t.length < 20))],
      rows: q('tbody tr').slice(0, 8).map(tr => [...tr.querySelectorAll('td')].map(td => (td.innerText || '').trim().replace(/\s+/g, ' '))),
    }
    if (extraKeys.includes('grids')) {
      out.grids = [...document.querySelectorAll('.dijitTabPane')].filter(vis).slice(-1).flatMap(p =>
        [...p.querySelectorAll('.dgrid,table')].slice(0, 6).map(g => ({
          cls: String(g.className).slice(0, 50),
          headers: [...g.querySelectorAll('th')].map(t => (t.innerText || '').trim()),
        })))
    }
    return out
  }, Object.keys(extra)).catch(e => ({ err: String(e) }))
  saveJson(name, data)
  log(name, 'buttons=', JSON.stringify(data.buttons))
  log(name, 'headers=', JSON.stringify(data.headers))
  log(name, 'labels=', JSON.stringify(data.labels))
  log(name, 'cbs=', JSON.stringify(data.cbs))
  log(name, 'radios=', JSON.stringify(data.radios))
  return data
}

/** 打开列配置弹窗并读取 */
async function readColCfg(page, name) {
  const clicked = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null
    const gears = [...document.querySelectorAll('.icon-shezhi2,.icon-shezhi,[class*=shezhi]')].filter(vis)
    const inPane = gears.filter(g => { const p = g.closest('.dijitTabPane'); return p && p.offsetParent !== null })
    const el = inPane[inPane.length - 1] || gears[gears.length - 1]
    if (!el) return false
    el.click()
    return true
  }).catch(() => false)
  log('colcfg gear clicked =', clicked)
  if (!clicked) return null
  await page.waitForTimeout(3500)
  await shot(page, name, false)
  const data = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null
    const dlgs = [...document.querySelectorAll('[role=dialog],[class*=Dialog],[class*=dialog]')].filter(vis)
      .map(d => ({ d, text: d.innerText || '' }))
      .filter(x => x.text.includes('列名') || x.text.includes('个人配置'))
      .sort((a, b) => b.text.length - a.text.length)
    const win = dlgs[0] && dlgs[0].d
    if (!win) return { found: false }
    const lines = (win.innerText || '').split('\n').map(s => s.trim()).filter(Boolean)
    const cols = []
    lines.forEach(l => {
      const m = l.match(/^(\d+)\t*\s*(.+)$/)
      if (m) cols.push({ no: Number(m[1]), name: m[2].trim() })
    })
    // 勾选状态
    const checks = [...win.querySelectorAll('input[type=checkbox]')].map(c => c.checked)
    const tabTxt = [...win.querySelectorAll('[role=tab],[class*=tab]')].filter(vis).map(t => (t.innerText || '').trim()).filter(Boolean)
    return { found: true, cols, checks, tabTxt: [...new Set(tabTxt)], raw: (win.innerText || '').slice(0, 12000) }
  }).catch(e => ({ err: String(e) }))
  saveJson(name, data)
  log(name, 'cols=', JSON.stringify((data.cols || []).map(c => `${c.no}|${c.name}`)))
  log(name, 'checks=', JSON.stringify(data.checks))
  return data
}

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  const res = await loginQl361(page, ctx, log)
  log('login:', JSON.stringify(res))
  if (!res.ok) { await shot(page, 'zz-login-fail', false); await browser.close(); process.exit(1) }
  await page.waitForTimeout(6000)
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null
    ;[...document.querySelectorAll('.dijitTabCloseButton,.dijitTabCloseButtonSingleton')].filter(vis).forEach(b => b.click())
    ;[...document.querySelectorAll('.dojoxDialogCloseIcon,.dijitDialogCloseIcon')].forEach(b => { try { b.click() } catch {} })
  }).catch(() => {})
  await page.waitForTimeout(1500)

  if (TASK.startsWith('sub:')) {
    const t = TASK.slice(4)
    const ok = await openProductList(page)
    log('openProductList =', ok)
    if (await selectTab(page, t)) {
      await page.waitForTimeout(6000)
      await shot(page, `03-tab-${t}`)
      await dumpPane(page, `03-tab-${t}`, { grids: true })
      await readColCfg(page, `04-colcfg-${t}`)
    } else log('!! 切换失败', t)
    await browser.close(); log('DONE'); return
  }

  if (TASK === 'list') {
    const ok = await openProductList(page)
    log('openProductList =', ok)
    await shot(page, '02-product-list')
    await dumpPane(page, '02-product-list', { grids: true })
    await readColCfg(page, '04-colcfg-全部商品')
    await browser.close(); log('DONE'); return
  }

  if (TASK === 'form' || TASK.startsWith('form:')) {
    const ok = await openProductList(page)
    log('openProductList =', ok)
    // 点新增
    const clicked = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null
      const panes = [...document.querySelectorAll('.dijitTabPane')].filter(vis)
      const scope = panes[panes.length - 1] || document.body
      const cands = [...scope.querySelectorAll('a,span,button,[class*=btn],[class*=Button]')]
        .filter(vis).filter(e => (e.innerText || '').trim().replace(/\s+/g, '') === '新增')
      if (!cands.length) return 'no-btn'
      cands.sort((a, b) => a.querySelectorAll('*').length - b.querySelectorAll('*').length)
      cands[0].click()
      return 'ok'
    }).catch(e => 'err:' + e.message)
    log('click 新增 ->', clicked)
    await page.waitForTimeout(12000)
    if (TASK.startsWith('form:')) {
      const t = TASK.slice(5)
      if (!(await selectTab(page, t))) { log('!! 表单 tab 切换失败', t) }
      await page.waitForTimeout(5000)
    }
    const suffix = TASK.startsWith('form:') ? TASK.slice(5) : '基本信息'
    await shot(page, `05-form-${suffix}`)
    const d = await dumpPane(page, `05-form-${suffix}`, { grids: true })
    log('=== FORM TEXT ===\n' + (d.paneText || '').slice(0, 6000))
    await browser.close(); log('DONE'); return
  }

  if (TASK === 'flags') {
    await openProductList(page)
    const clicked = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null
      const a = [...document.querySelectorAll('a,span,button')].filter(vis)
        .find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品字段及规则配置')
      if (!a) return false
      a.click(); return true
    }).catch(() => false)
    log('click 商品字段及规则配置 ->', clicked)
    await page.waitForTimeout(4000)
    await shot(page, '07-field-rules', false)
    await dumpPane(page, '07-field-rules')
    await browser.close(); log('DONE'); return
  }

  log('unknown task', TASK)
  await browser.close()
})().catch(e => { console.error('FATAL', e); process.exit(1) })
