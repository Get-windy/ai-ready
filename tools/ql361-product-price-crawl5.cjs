/* ql361 商品价格管理对标抓取（第五轮）：修正 pane 可见性判定 + 会话自愈（点 submit）
 * 运行： NODE_PATH=frontend/node_modules node tools/ql361-product-price-crawl5.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const fs = require('fs')
const path = require('path')

const OUT = path.resolve(__dirname, '../tool-results/ql361/product-price')
const BASE = process.env.QL361_BASE || 'https://www.ql361.com'
const ACCOUNT = process.env.QL361_USER || '15095664266'
const PASSWORD = process.env.QL361_PWD || 'Ysh790921'

function log(...a) { console.log('[pp5]', ...a) }
async function shot(page, name, fullPage = false) {
  await page.screenshot({ path: path.join(OUT, `${name}.png`), fullPage }); log('shot ->', `${name}.png`)
}
async function dump(page, name, fn) {
  const data = await page.evaluate(fn)
  fs.writeFileSync(path.join(OUT, `${name}.json`), JSON.stringify(data, null, 2), 'utf8'); log('json ->', `${name}.json`)
  return data
}

// 注入到页面的工具（用字符串函数避免每次重复）
const HELPERS = `
function __vis(el){
  let n = el
  while (n && n.nodeType === 1) {
    const s = getComputedStyle(n)
    if (s.display === 'none' || s.visibility === 'hidden' || s.opacity === '0') return false
    n = n.parentElement
  }
  return true
}
function __txt(el){ return el ? (el.innerText || '').trim().replace(/\\s+/g,' ') : '' }
function __pane(needle){
  const panes = [...document.querySelectorAll('.dijitTabPane')].filter(__vis)
  if (needle) { const hit = panes.find(p => (p.innerText||'').includes(needle)); if (hit) return hit }
  return panes[panes.length-1] || null
}
function __dlg(){
  const ds = [...document.querySelectorAll('.dijitDialog, [role=dialog]')].filter(__vis)
    .filter(d => __txt(d).length > 30)
  ds.sort((a,b)=> __txt(b).length - __txt(a).length)
  return ds[0] || null
}
`

const DLG = `(() => {
  ${HELPERS}
  const dlg = __dlg()
  if (!dlg) return { none: true }
  const rows = [...dlg.querySelectorAll('tr')].map(tr => {
    const cb = tr.querySelector('input[type=checkbox]')
    return { cells: [...tr.querySelectorAll('td')].map(__txt).slice(0,6), checked: cb ? cb.checked : null }
  }).filter(r => r.cells.some(c => c))
  return {
    dialogClass: String(dlg.className).slice(0,80),
    text: __txt(dlg).slice(0, 12000),
    tabs: [...dlg.querySelectorAll('.dijitTab,[role=tab]')].filter(__vis).map(__txt).filter(t=>t&&t.length<20),
    checks: [...dlg.querySelectorAll('input[type=checkbox]')].filter(__vis).map(el => ({ checked: el.checked, label: __txt(el.closest('label')||el.parentElement).slice(0,40) })),
    inputs: [...dlg.querySelectorAll('input,select,textarea')].filter(__vis).map(el => ({ tag: el.tagName, type: el.type, ph: el.getAttribute('placeholder')||'', val: el.value, cls: String(el.className).slice(0,40) })),
    rows: rows.slice(0,120),
  }
})()`

async function healRelogin(page) {
  for (let i = 0; i < 3; i++) {
    const has = await page.evaluate(`(() => { ${HELPERS}
      const dlg = [...document.querySelectorAll('.dijitDialog,[role=dialog]')].filter(__vis).find(d => __txt(d).includes('重新登录'))
      return !!dlg })()`).catch(() => false)
    if (!has) return true
    log('RE-LOGIN heal')
    await page.evaluate(`(() => { ${HELPERS}
      const dlg = [...document.querySelectorAll('.dijitDialog,[role=dialog]')].filter(__vis).find(d => __txt(d).includes('重新登录'))
      if (!dlg) return
      const inp = dlg.querySelector('input[type=password]')
      if (inp) { const s = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set; s.call(inp, ${JSON.stringify(PASSWORD)}); inp.dispatchEvent(new Event('input',{bubbles:true})); inp.dispatchEvent(new Event('change',{bubbles:true})) }
      const sub = dlg.querySelector('input[type=submit]')
      if (sub) { sub.click(); return }
      const node = [...dlg.querySelectorAll('.dijitButtonNode')].filter(__vis).find(b => __txt(b).includes('确定'))
      if (node) node.click()
    })()`).catch(() => {})
    await page.waitForTimeout(4500)
  }
  return false
}

async function closeDlg(page) {
  for (let i = 0; i < 4; i++) {
    const open = await page.evaluate(`(() => { ${HELPERS} return !!__dlg() })()`)
    if (!open) return true
    await page.evaluate(`(() => { ${HELPERS}
      const dlg = __dlg(); if (!dlg) return
      const x = dlg.querySelector('.dijitDialogCloseIcon, [class*=CloseIcon], .closeText')
      if (x) { x.click(); return }
      const btns = [...dlg.querySelectorAll('.dijitButtonNode,button,span')].filter(__vis).filter(e => /^(关闭|取消|Esc)/.test(__txt(e)))
      if (btns.length) btns[btns.length-1].click()
    })()`).catch(() => {})
    await page.waitForTimeout(1200)
  }
  return false
}

async function switchTab(page, label) {
  await page.evaluate((label) => {
    try {
      const reg = window.dijit && window.dijit.registry
      if (reg) for (const tc of reg.toArray().filter(w => (w.declaredClass || '').includes('TabContainer'))) {
        const hit = (tc.getChildren() || []).find(c => ((c.title || '').trim()) === label)
        if (hit) { tc.selectChild(hit); return }
      }
    } catch (e) {}
    const t = [...document.querySelectorAll('.dijitTab')].filter(e => e.offsetParent !== null).find(e => (e.innerText || '').trim() === label)
    if (t) t.click()
  }, label)
  await page.waitForTimeout(3500)
  log('switched to', label)
}

async function colConfig(page, tag, needle) {
  await healRelogin(page)
  const r = await page.evaluate(`(() => { ${HELPERS}
    const pane = __pane(${JSON.stringify(needle)})
    if (!pane) return 'NO_PANE'
    const g = pane.querySelector('.dgrid-column-set .icon-shezhi2')
    if (!g) return 'NO_GEAR|' + __txt(pane).slice(0,120)
    g.click(); return 'OK'
  })()`)
  log(tag, 'gear:', r)
  await page.waitForTimeout(2500)
  await shot(page, `${tag}-colconfig`)
  const d = await dump(page, `${tag}-colconfig`, DLG)
  log(tag, 'text:', (d.text || '').slice(0, 1600))
  log(tag, 'rows:', JSON.stringify((d.rows || []).map(r => r.cells.join('|')).slice(0, 40)))
  await closeDlg(page)
  return d
}

async function clickAndDump(page, name, tag, needle) {
  await healRelogin(page)
  const r = await page.evaluate(`(() => { ${HELPERS}
    const pane = __pane(${JSON.stringify(needle || '')})
    if (!pane) return 'NO_PANE'
    const els = [...pane.querySelectorAll('a,span,div,button,td')].filter(e => __vis(e) && e.children.length === 0 && __txt(e) === ${JSON.stringify(name)})
    if (!els.length) return 'NO_EL'
    els[els.length-1].click(); return 'OK'
  })()`)
  log(`${tag} click ${name}:`, r)
  if (r !== 'OK') return null
  await page.waitForTimeout(3000)
  await shot(page, tag)
  const d = await dump(page, `${tag}-dlg`, DLG)
  log(`${tag} text:`, (d.text || '').slice(0, 2500))
  log(`${tag} inputs:`, JSON.stringify((d.inputs || []).slice(0, 30)))
  log(`${tag} checks:`, JSON.stringify((d.checks || []).slice(0, 30)))
  await closeDlg(page)
  return d
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: process.env.HEADED !== '1' })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  page.on('pageerror', e => log('pageerror:', (e.message || '').slice(0, 150)))

  try {
    const { loginQl361 } = require('./ql361-lib.cjs')
    const lg = await loginQl361(page, ctx, (...a) => log('login:', ...a))
    log('login result:', JSON.stringify(lg))
    if (!lg.ok) throw new Error('login failed')
    await page.waitForTimeout(2000)
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(1000)
    await page.evaluate(`(() => { ${HELPERS}
      for (const t of [...document.querySelectorAll('.dijitTab')].filter(__vis)) {
        const text = __txt(t)
        if (t.classList.contains('dijitTabSelected') || text.includes('商品价格管理')) continue
        const c = t.querySelector('.dijitTabCloseButton'); if (c) c.click()
      }
    })()`).catch(() => {})
    await page.waitForTimeout(1200)
    await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true })
    await page.waitForTimeout(2200)
    for (const name of ['商品管理', '商品价格管理']) {
      await page.evaluate((n) => {
        const el = [...document.querySelectorAll('a,span,div,li')].filter(e => e.offsetParent !== null && e.children.length === 0)
          .find(e => (e.innerText || '').trim() === n)
        if (el) el.click()
      }, name)
      await page.waitForTimeout(name === '商品管理' ? 1500 : 9000)
    }
    await healRelogin(page)
    await shot(page, '400-open')

    // ===== tab1 批量修改（先勾选行） =====
    const sel = await page.evaluate(`(() => { ${HELPERS}
      const pane = __pane('商品名称')
      if (!pane) return 'NO_PANE'
      const row = pane.querySelector('.dgrid-row')
      const sels = row ? [...row.querySelectorAll('td')].map(td => String(td.className).slice(0,60)) : []
      const cbCell = row && row.querySelector('.dgrid-selector, .dgrid-checkbox, input[type=checkbox]')
      if (cbCell) { (cbCell.tagName === 'INPUT' ? cbCell : cbCell).click(); return 'CLICKED|' + JSON.stringify(sels) }
      return 'NOSEL|' + JSON.stringify(sels)
    })()`)
    log('tab1 row select:', sel)
    await page.waitForTimeout(1500)
    await shot(page, '401-tab1-selected')
    await clickAndDump(page, '批量修改', '402-tab1-batchmod', '商品名称')

    // ===== tab2 =====
    await switchTab(page, '客户级别折扣设置')
    await colConfig(page, '411-tab2', '级别默认价')
    await clickAndDump(page, '修改', '412-tab2-edit', '级别默认价')

    // ===== tab3 =====
    await switchTab(page, '级别指定价设置')
    await colConfig(page, '421-tab3', '价格规则')
    await clickAndDump(page, '导入', '422-tab3-import', '价格规则')

    // ===== tab4 =====
    await switchTab(page, '客户指定价设置')
    await colConfig(page, '431-tab4', '客户编号')
    await clickAndDump(page, '导入', '432-tab4-import', '客户编号')

    await shot(page, '499-final')
  } catch (e) {
    log('FATAL', e.message)
    await shot(page, 'zz-error').catch(() => {})
  } finally {
    await browser.close()
  }
}

main()
