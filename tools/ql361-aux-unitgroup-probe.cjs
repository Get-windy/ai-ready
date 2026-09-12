/* ql361「单位组管理」深探针（商品辅助资料 → 商品单位 → 单位组管理）
 * 目标：该视图的工具栏/查询条件/列/行操作/列配置/新增弹窗字段（真实结构）
 * 运行： node tools/ql361-aux-unitgroup-probe.cjs
 * 输出： tool-results/ql361/pages/aux-probe3/
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const OUT = path.resolve(__dirname, '../tool-results/ql361/pages/aux-probe3')
fs.mkdirSync(OUT, { recursive: true })
const RUNLOG = path.join(OUT, 'run.log')
try { fs.writeFileSync(RUNLOG, '') } catch {}
const log = (...a) => {
  const line = `[ug] ${a.map(x => (typeof x === 'string' ? x : JSON.stringify(x))).join(' ')}`
  console.log(line)
  try { fs.appendFileSync(RUNLOG, line + '\n') } catch {}
}
const save = (n, o) => {
  fs.writeFileSync(path.join(OUT, n), typeof o === 'string' ? o : JSON.stringify(o, null, 1), 'utf8')
  log('saved ->', n)
}

;(async () => {
  const browser = await chromium.launch({ headless: true, args: ['--disable-blink-features=AutomationControlled'] })
  const ctx = await browser.newContext({
    viewport: { width: 1680, height: 950 },
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
  })
  await ctx.addInitScript(() => {
    Object.defineProperty(navigator, 'webdriver', { get: () => false })
    window.registryAll = () => {
      const reg = window.dijit && (window.dijit.registry || window.dijit._registry)
      if (!reg) return []
      if (typeof reg.toArray === 'function') return reg.toArray()
      if (reg._hash) return Object.values(reg._hash)
      const out = []
      if (typeof reg.forEach === 'function') reg.forEach(w => out.push(w))
      return out
    }
  })
  const page = await ctx.newPage()
  const shot = (n, full = false) => page.screenshot({ path: path.join(OUT, `${n}.png`), fullPage: full })
  const result = {}

  const visibleViews = () => page.evaluate(() => [...document.querySelectorAll('.view')].filter(e => {
    const r = e.getBoundingClientRect()
    if (r.width < 200 || r.height < 80) return false
    let n = e
    while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden' || s.opacity === '0') return false; n = n.parentElement }
    return true
  }).map(e => String(e.className)))

  /** dump 指定 view 内的结构；clsPrefix 为空则取"最大可见 view" */
  const dumpView = clsPrefix => page.evaluate(prefix => {
    const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
    const visible = el => {
      const r = el.getBoundingClientRect()
      if (r.width < 20 || r.height < 10) return false
      let n = el
      while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
      return true
    }
    const views = [...document.querySelectorAll('.view')].filter(e => {
      const r = e.getBoundingClientRect()
      if (r.width < 200 || r.height < 80) return false
      let n = e
      while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
      return true
    })
    const root = prefix ? views.find(v => String(v.className).includes(prefix)) : views.sort((a, b) => b.getBoundingClientRect().width - a.getBoundingClientRect().width)[0]
    if (!root) return { error: 'no-root', views: views.map(v => String(v.className)) }
    const out = { rootCls: String(root.className), scopeText: txt(root).slice(0, 1500) }
    out.dijitBtns = [...new Set([...root.querySelectorAll('.dijitButtonText')].filter(visible).map(txt).filter(Boolean))]
    out.plainBtns = [...new Set([...root.querySelectorAll('button, [role=button], a[class*=btn]')].filter(visible).map(txt).filter(t => t && t.length < 24))]
    out.inputs = [...root.querySelectorAll('input, select, textarea')].filter(visible).map(e => ({ type: e.type, ph: e.getAttribute('placeholder') || '', val: e.value || '', cls: String(e.className).slice(0, 40) }))
    out.labels = [...root.querySelectorAll('label, span, td, div')].filter(visible).map(txt).filter(t => t && t.length < 14).slice(0, 60)
    out.headers = [...new Set([...root.querySelectorAll('.dgrid-header .dgrid-cell, [role=columnheader], th')].filter(visible).map(txt).filter(Boolean))]
    out.rows = [...root.querySelectorAll('.dgrid-row, tbody tr')].filter(visible).slice(0, 6).map(tr => ({
      cells: [...tr.querySelectorAll('.dgrid-cell, td')].map(txt).filter(Boolean),
      acts: [...tr.querySelectorAll('[class*=icon-], .dijitButtonText, a')].filter(visible).map(e => String(e.className).slice(0, 40)).filter(Boolean),
    }))
    out.gears = [...root.querySelectorAll('.icon-shezhi2, [class*=shezhi]')].filter(visible).map(e => ({ cls: String(e.className).slice(0, 60), title: e.getAttribute('title') || '' }))
    return out
  }, clsPrefix || '')

  /** 打开列配置弹窗并读列 */
  const readColCfg = () => page.evaluate(() => {
    const txt = el => (el ? (el.innerText || '').trim() : '')
    const visible = el => { const r = el.getBoundingClientRect(); return r.width > 20 && r.height > 10 }
    const dlgs = [...document.querySelectorAll('[class*=Dialog], [role=dialog]')].filter(visible)
      .map(d => ({ t: txt(d), el: d })).filter(d => /全局配置|个人配置/.test(d.t))
      .sort((a, b) => b.t.length - a.t.length)
    if (!dlgs.length) return null
    const dlg = dlgs[0].el
    const rows = [...dlg.querySelectorAll('tr')].map(tr => {
      const tds = [...tr.querySelectorAll('td')].map(txt)
      const cb = tr.querySelector('input[type=checkbox], .dijitCheckBoxInput, .dijitCheckBox')
      return { cells: tds.filter((_, i) => i < 5), checked: cb ? (cb.checked ?? /Checked/.test(String(cb.className))) : null }
    }).filter(r => r.cells.length >= 2)
    return { text: txt(dlg).slice(0, 600), rows }
  })

  /** 当前可见的 dojo 弹窗（用于编辑/新增表单） */
  const readFormDialog = () => page.evaluate(() => {
    const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
    const visible = el => {
      const r = el.getBoundingClientRect()
      if (r.width < 150 || r.height < 80) return false
      let n = el
      while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
      return true
    }
    const dlgs = [...document.querySelectorAll('.dojoxDialog, .dojoxDialogWrapper, [role=dialog], [class*=Dialog]')].filter(visible)
    const target = dlgs.map(d => ({ t: txt(d), el: d })).filter(d => d.t.length > 20).sort((a, b) => b.t.length - a.t.length)[0]
    if (!target) return null
    const el = target.el
    return {
      text: target.t.slice(0, 900),
      fields: [...el.querySelectorAll('input, textarea, select')].filter(visible).map(i => {
        let label = ''
        const wrap = i.closest('tr, .dijitFieldset, div')
        if (wrap) label = (wrap.innerText || '').trim().split('\n')[0].slice(0, 30)
        return { tag: i.tagName, type: i.type, value: i.value, readonly: i.readOnly, label }
      }),
      checks: [...el.querySelectorAll('input[type=checkbox]')].filter(visible).map(i => {
        const wrap = i.closest('tr, div')
        return { label: wrap ? (wrap.innerText || '').trim().slice(0, 30) : '', checked: i.checked }
      }),
      buttons: [...new Set([...el.querySelectorAll('.dijitButtonText, button')].map(txt).filter(Boolean))],
    }
  })

  try {
    await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(2500)
    const lr = await loginQl361(page, ctx, log)
    log('login:', JSON.stringify(lr))
    if (!lr.ok) throw new Error('登录失败')
    await page.waitForTimeout(4000)

    // 导航：资料 → 商品管理 → 商品辅助资料
    await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true })
    await page.waitForTimeout(2000)
    const grp = page.locator('.popupmenu__nav-text', { hasText: '商品管理' }).first()
    if (await grp.count()) { await grp.hover().catch(() => {}); await page.waitForTimeout(1500) }
    await page.evaluate(() => {
      const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品辅助资料')
      if (el) el.click()
    })
    for (let i = 0; i < 15; i++) { await page.waitForTimeout(2000); if (await page.evaluate(() => !!document.querySelector('.view.GoodBrandList'))) break }
    await page.keyboard.press('Escape').catch(() => {})
    log('page ready')
    result.diagTabs = await page.evaluate(() => ({
      views: [...document.querySelectorAll('.view')].map(e => String(e.className)),
      tabs: [...document.querySelectorAll('.dijitTab')].map(t => (t.innerText || '').trim()),
      tablist: [...document.querySelectorAll('[class*=tablist], [class*=TabList]')].map(e => String(e.className).slice(0, 70)),
      registryTabBtns: window.registryAll().filter(w => /TabButton/.test(String(w.declaredClass || ''))).map(w => String(w.label || '').trim()),
    }))
    log('diag tabs:', JSON.stringify(result.diagTabs.tabs), '| views:', JSON.stringify(result.diagTabs.views.slice(0, 6)))

    // 切到「商品单位」：优先 TabButton.onClick，回退 TabContainer.selectChild
    // 子 tab 容器为惰性创建，需轮询等待
    let sel = 'none'
    for (let i = 0; i < 20; i++) {
      sel = await page.evaluate(label => {
      const all = window.registryAll()
      const btns = all.filter(w => /TabButton/.test(String(w.declaredClass || '')))
      const b = btns.find(w => String(w.label || w.title || (w.containerNode ? w.containerNode.innerText : '') || '').trim() === label)
      if (b) { if (typeof b.onClick === 'function') b.onClick(); else if (b.domNode) b.domNode.click(); return 'btn:' + b.id }
      const tcs = all.filter(w => /TabContainer/.test(String(w.declaredClass || '')))
      for (const tc of tcs) {
        const kid = (tc.getChildren ? tc.getChildren() : []).find(c => String(c.title || '').trim() === label)
        if (kid) { tc.selectChild(kid); return 'tc:' + kid.id }
      }
      // DOM 点击子 tab
      const domTabs = [...document.querySelectorAll('.dijitTab, .dijitTabButton, [role=tab]')].filter(e => (e.innerText || '').trim() === label)
      if (domTabs.length) { domTabs[domTabs.length - 1].click(); return 'dom:' + domTabs.length }
        return 'none|btns=' + btns.map(w => String(w.label || '').trim()).join(',') + '|tcs=' + tcs.length
      }, '商品单位')
      if (sel !== 'none' && !String(sel).startsWith('none')) break
      if (i === 0 && String(sel).startsWith('none')) log('等待子 tab 容器创建…', sel)
      await page.waitForTimeout(1500)
    }
    log('switch 商品单位:', sel)
    await page.waitForTimeout(3000)
    result.viewsBeforeUG = await visibleViews()
    result.unitBeforeUG = await dumpView('GoodUnitList')
    await shot('B1-unit')

    // 点「单位组管理」
    const clickUG = await page.evaluate(() => {
      const btns = [...document.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '单位组管理')
      if (!btns.length) return 'no-btn'
      const node = btns[btns.length - 1].closest('.dijitButton') || btns[btns.length - 1]
      ;(node.querySelector('.dijitButtonNode') || node).click()
      return 'clicked'
    })
    log('click 单位组管理:', clickUG)
    await page.waitForTimeout(5000)
    result.viewsAfterUG = await visibleViews()
    result.ugView = await dumpView("GoodUnitList")
    result.ugViewTop = await dumpView("")
    result.tabButtons = await page.evaluate(() => window.registryAll().filter(w => /TabButton/.test(String(w.declaredClass || ''))).map(w => String(w.label || w.title || '').trim()))
    await shot('B2-unitgroup')
    log('UG view cls:', result.ugView.rootCls, '| btns:', JSON.stringify(result.ugView.dijitBtns), '| headers:', JSON.stringify(result.ugView.headers))

    // 列配置
    const gear = page.locator('.icon-shezhi2').filter({ visible: true }).first()
    if (await gear.count()) {
      await gear.click({ force: true }).catch(() => {})
      await page.waitForTimeout(2500)
      await shot('B3-ug-colcfg')
      result.ugColCfg = await readColCfg()
      log('UG 列配置行数:', result.ugColCfg ? result.ugColCfg.rows.length : 'null')
      await page.keyboard.press('Escape').catch(() => {})
      await page.waitForTimeout(1500)
    }

    // 新增单位组
    const clickAdd = await page.evaluate(() => {
      const btns = [...document.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '新增单位组')
      if (!btns.length) return 'no-btn'
      const node = btns[btns.length - 1].closest('.dijitButton') || btns[btns.length - 1]
      ;(node.querySelector('.dijitButtonNode') || node).click()
      return 'clicked'
    })
    log('click 新增单位组:', clickAdd)
    await page.waitForTimeout(4000)
    await shot('B4-ug-add')
    result.ugAddDialog = await readFormDialog()
    log('新增单位组弹窗:', JSON.stringify(result.ugAddDialog && result.ugAddDialog.text ? result.ugAddDialog.text.slice(0, 300) : 'null'))
    // 下拉展开（单位组名称旁的选项 / 单位选择）
    const combos = await page.evaluate(() => {
      const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
      const visible = el => { const r = el.getBoundingClientRect(); return r.width > 20 && r.height > 10 }
      const dlgs = [...document.querySelectorAll('.dojoxDialog, [class*=Dialog]')].filter(visible)
      const dlg = dlgs.map(d => ({ t: txt(d), el: d })).filter(d => d.t.length > 20).sort((a, b) => b.t.length - a.t.length)[0]
      if (!dlg) return null
      const rows = [...dlg.el.querySelectorAll('tr')].filter(visible).map(tr => ({
        label: txt(tr.querySelector('td:first-child')),
        control: (() => { const c = tr.querySelector('td:nth-child(2)'); return c ? String(c.className).slice(0, 60) : '' })(),
        value: (() => { const i = tr.querySelector('input, select'); return i ? (i.value || i.getAttribute('value') || '') : '' })(),
      })).filter(r => r.label || r.control)
      return { rows, html: dlg.el.innerHTML.slice(0, 4000) }
    })
    result.ugAddRows = combos
    save('probe-result.json', result)
  } catch (e) {
    log('FATAL', e.message)
    save('probe-error.txt', String(e.stack || e.message))
    await shot('Z-fatal').catch(() => {})
  } finally {
    await browser.close()
  }
})()
