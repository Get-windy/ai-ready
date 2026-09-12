/* ql361「单位组管理」表单探针（第三轮）：新增/修改表单字段、下拉选项、行内「更多」、显示状态选项
 * 运行： node tools/ql361-aux-unitgroup-probe3.cjs
 * 输出： tool-results/ql361/pages/aux-probe5/
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const OUT = path.resolve(__dirname, '../tool-results/ql361/pages/aux-probe6')
fs.mkdirSync(OUT, { recursive: true })
const RUNLOG = path.join(OUT, 'run.log')
try { fs.writeFileSync(RUNLOG, '') } catch {}
const log = (...a) => {
  const line = `[ug3] ${a.map(x => (typeof x === 'string' ? x : JSON.stringify(x))).join(' ')}`
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

  const UG_DLG = `(() => {
    const cands = [...document.querySelectorAll('div')].filter(e => {
      const t = e.innerText || ''
      if (!t.includes('新增单位组') || !t.includes('单位关系')) return false
      const r = e.getBoundingClientRect()
      if (r.width < 300 || r.height < 150) return false
      let n = e
      while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
      return true
    })
    cands.sort((a, b) => { const ra = a.getBoundingClientRect(), rb = b.getBoundingClientRect(); return ra.width * ra.height - rb.width * rb.height })
    return cands[0] || null
  })()`

  /** 最上层可见 dojo 弹窗（新增/修改表单），按标题区分 */
  const topDialogs = () => page.evaluate(`(() => {
    const txt = el => (el ? (el.innerText || '').trim().replace(/\\s+/g, ' ') : '')
    const vis = el => {
      const r = el.getBoundingClientRect()
      if (r.width < 200 || r.height < 80) return false
      let n = el
      while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
      return true
    }
    const all = [...document.querySelectorAll('div, table')].filter(e => {
      if (!vis(e)) return false
      if (!/Dialog|dialog/.test(String(e.className))) return false
      return (e.innerText || '').trim().length > 20
    })
    return all.map(e => {
      const r = e.getBoundingClientRect()
      return { cls: String(e.className).slice(0, 80), w: Math.round(r.width), h: Math.round(r.height), text: txt(e).slice(0, 400) }
    }).sort((a, b) => a.w * a.h - b.w * b.h).slice(0, 8)
  })()`)

  /** 读表单最上层弹窗的字段（表格 label/控件） */
  const readForm = () => page.evaluate(`(() => {
    const txt = el => (el ? (el.innerText || '').trim().replace(/\\s+/g, ' ') : '')
    const vis = el => {
      const r = el.getBoundingClientRect()
      if (r.width < 150 || r.height < 60) return false
      let n = el
      while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
      return true
    }
    const ug = ${UG_DLG}
    const dlgs = [...document.querySelectorAll('.dojoxDialog, [class*=Dialog]')].filter(vis)
      .filter(e => e !== ug && !e.contains(ug))
    const inner = dlgs.filter(e => !dlgs.some(o => o !== e && o.contains(e)))
    const dlg = inner.map(e => {
      const r = e.getBoundingClientRect()
      return { e, area: r.width * r.height, t: txt(e) }
    }).filter(d => d.t.length > 10).sort((a, b) => a.area - b.area)[0]
    if (!dlg) return { error: 'no-form' }
    const el = dlg.e
    return {
      cls: String(el.className).slice(0, 80),
      text: dlg.t.slice(0, 700),
      inputs: [...el.querySelectorAll('input, textarea, select')].filter(vis).map(i => {
        const tr = i.closest('tr')
        const label = tr ? txt(tr.querySelector('td:first-child')).slice(0, 30) : ''
        return { type: i.type, label, value: i.value || '', cls: String(i.className).slice(0, 40), readonly: i.readOnly }
      }),
      selects: [...el.querySelectorAll('.dijitSelect, .dijitComboBox, [class*=combo], select')].filter(vis).map(s => ({
        cls: String(s.className).slice(0, 60),
        text: txt(s).slice(0, 40),
        options: [...s.querySelectorAll('option')].map(o => (o.innerText || '').trim()).slice(0, 20),
      })),
      btns: [...new Set([...el.querySelectorAll('.dijitButtonText')].map(txt).filter(Boolean))],
      checks: [...el.querySelectorAll('input[type=checkbox]')].filter(vis).map(i => {
        const tr = i.closest('tr, div')
        return { label: tr ? txt(tr).slice(0, 40) : '', checked: i.checked }
      }),
      rows: [...el.querySelectorAll('tr')].filter(vis).map(tr => {
        const tds = [...tr.querySelectorAll('td')]
        if (tds.length < 2) return null
        const ctrl = tds[1]
        return {
          label: txt(tds[0]).slice(0, 30),
          ctrlCls: String(ctrl.className).slice(0, 60),
          ctrlText: txt(ctrl).slice(0, 50),
          tag: ctrl.firstElementChild ? ctrl.firstElementChild.tagName : '',
        }
      }).filter(Boolean),
      html: el.innerHTML.slice(0, 6000),
    }
  })()`)

  try {
    await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(2500)
    const lr = await loginQl361(page, ctx, log)
    log('login:', JSON.stringify(lr))
    if (!lr.ok) throw new Error('登录失败')
    await page.waitForTimeout(4000)

    // 导航（重试）：点「资料」→ 展开「商品管理」→ 点「商品辅助资料」
    let opened = false
    for (let a = 1; a <= 5 && !opened; a++) {
      await page.keyboard.press('Escape').catch(() => {})
      await page.waitForTimeout(600)
      await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {})
      await page.waitForTimeout(2500)
      let items = await page.evaluate(() => [...document.querySelectorAll('.popupmenu__nav-text')].map(e => (e.innerText || '').trim().replace(/\s+/g, '')))
      if (!items.includes('商品管理')) {
        // 菜单未展开：再点一次 / hover
        await page.locator('a.menu-0-item', { hasText: '资料' }).first().hover().catch(() => {})
        await page.waitForTimeout(1500)
        await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {})
        await page.waitForTimeout(2500)
        items = await page.evaluate(() => [...document.querySelectorAll('.popupmenu__nav-text')].map(e => (e.innerText || '').trim().replace(/\s+/g, '')))
      }
      if (items.includes('商品管理')) {
        await page.locator('.popupmenu__nav-text', { hasText: '商品管理' }).first().hover().catch(() => {})
        await page.waitForTimeout(2000)
      }
      const clicked = await page.evaluate(() => {
        const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品辅助资料')
        if (el) { el.click(); return true }
        return false
      })
      for (let i = 0; i < 10; i++) {
        await page.waitForTimeout(2000)
        if (await page.evaluate(() => !!document.querySelector('.view.GoodBrandList, .view.GoodUnitList, .view.GoodsTags'))) { opened = true; break }
      }
      log(`nav ${a}: items=${JSON.stringify(items.slice(0, 8))} clicked=${clicked} opened=${opened}`)
    }
    if (!opened) throw new Error('导航失败')
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(1200)

    // 切单位 Tab
    let switched = false
    for (let i = 0; i < 12 && !switched; i++) {
      const r = await page.evaluate(() => {
        const all = window.registryAll()
        const tcs = all.filter(w => /(^|\.)TabContainer$/.test(String(w.declaredClass || '')))
        for (const tc of tcs) {
          const kid = (tc.getChildren ? tc.getChildren() : []).find(c => String(c.title || '').trim() === '商品单位')
          if (kid) { tc.selectChild(kid); return 'tc' }
        }
        const btns = all.filter(w => /TabButton/.test(String(w.declaredClass || '')))
        const b = btns.find(w => String(w.label || '').trim() === '商品单位')
        if (b) { b.onClick && b.onClick(); return 'btn' }
        return 'no'
      })
      await page.waitForTimeout(1500)
      switched = await page.evaluate(() => {
        const v = document.querySelector('.view.GoodUnitList')
        return !!v && v.getBoundingClientRect().width > 100
      })
      if (i === 0) log('切单位:', r)
    }
    log('单位 Tab:', switched)

    // 开单位组管理
    let ugOpen = false
    for (let i = 0; i < 6 && !ugOpen; i++) {
      await page.evaluate(() => {
        const b = [...document.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '单位组管理')
        if (!b.length) return
        const n = b[b.length - 1].closest('.dijitButton') || b[b.length - 1]
        ;(n.querySelector('.dijitButtonNode') || n).click()
      })
      for (let j = 0; j < 5; j++) {
        await page.waitForTimeout(2000)
        if (await page.evaluate(`!!${UG_DLG}`)) { ugOpen = true; break }
      }
    }
    log('单位组弹窗:', ugOpen)
    if (!ugOpen) throw new Error('单位组弹窗未打开')

    // ── A. 查询区：dojo 下拉（显示状态）选项（直接读 widget，比点开菜单可靠） ──
    result.ugSelects = await page.evaluate(`(() => {
      const dlg = ${UG_DLG}
      if (!dlg) return 'no-dialog'
      const all = window.registryAll()
      return all.filter(w => /Select|ComboBox/.test(String(w.declaredClass || '')) && w.domNode && dlg.contains(w.domNode)).map(w => ({
        id: w.id,
        cls: String(w.declaredClass),
        displayedValue: w.get ? String(w.get('displayedValue') || '') : '',
        value: w.value,
        options: (w.options || []).map(o => ({ label: o.label, value: o.value })),
      }))
    })()`)
    log('单位组弹窗内下拉:', JSON.stringify(result.ugSelects).slice(0, 400))
    await shot('D1-ug-list')

    // ── A2. 行内「更多」菜单（在打开表单之前做，避免弹窗栈被关） ──
    const moreClicked = await page.evaluate(`(() => {
      const dlg = ${UG_DLG}
      if (!dlg) return 'no-dialog'
      const b = [...dlg.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '更多')
      if (!b.length) return 'no-more'
      const n = b[0].closest('.dijitButton') || b[0]
      ;(n.querySelector('.dijitButtonNode') || n).click()
      return 'clicked'
    })()`)
    log('点行内更多:', moreClicked)
    await page.waitForTimeout(2500)
    await shot('E1-more-menu')
    result.moreMenu = await page.evaluate(() => {
      const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
      const vis = el => { const r = el.getBoundingClientRect(); return r.width > 20 && r.height > 10 }
      return [...document.querySelectorAll('.dijitMenuPopup, .dijitPopup, .dijitMenu, [class*=Menu]')].filter(vis).map(txt).filter(t => t && t.length < 60).slice(0, 10)
    })
    log('更多菜单:', JSON.stringify(result.moreMenu))
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(1500)
    await shot('E2-after-more')

    // ── A3. 查询区「显示状态」下拉选项 ──
    const st = await page.evaluate(`(() => {
      const dlg = ${UG_DLG}
      if (!dlg) return 'no-dialog'
      const nodes = [...dlg.querySelectorAll('.dijitSelect, .dijitDownArrowButton, [class*=Select]')].filter(e => e.getBoundingClientRect().width > 10)
      if (!nodes.length) return 'no-select'
      nodes[nodes.length - 1].click()
      return 'clicked:' + nodes.length
    })()`)
    log('点显示状态:', st)
    await page.waitForTimeout(2500)
    await shot('E3-status-menu')
    result.statusMenu = await page.evaluate(() => {
      const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
      const vis = el => { const r = el.getBoundingClientRect(); return r.width > 20 && r.height > 10 }
      return [...document.querySelectorAll('.dijitMenuPopup, .dijitPopup, .dijitMenu')].filter(vis).map(txt).filter(Boolean).slice(0, 8)
    })
    log('显示状态选项:', JSON.stringify(result.statusMenu))
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(1200)
    await shot('E4-after-status')

    // ── A4. 行内「修改」回填 ──
    const editClicked1 = await page.evaluate(`(() => {
      const dlg = ${UG_DLG}
      if (!dlg) return 'no-dialog'
      const b = [...dlg.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '修改')
      if (!b.length) return 'no-edit'
      const n = b[0].closest('.dijitButton') || b[0]
      ;(n.querySelector('.dijitButtonNode') || n).click()
      return 'clicked'
    })()`)
    log('点行内修改:', editClicked1)
    await page.waitForTimeout(4000)
    await shot('E5-edit-form')
    result.editForm = await readForm()
    log('修改回填:', JSON.stringify(result.editForm && result.editForm.text ? result.editForm.text.slice(0, 220) : 'null'))
    save('probe-result-partial.json', result)

    // ── B. 新增单位组表单 ──
    const addClicked = await page.evaluate(`(() => {
      const dlg = ${UG_DLG}
      if (!dlg) return 'no-dialog'
      const b = [...dlg.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '新增单位组')
      if (!b.length) return 'no-btn'
      const n = b[0].closest('.dijitButton') || b[0]
      ;(n.querySelector('.dijitButtonNode') || n).click()
      return 'clicked'
    })()`)
    log('点新增单位组:', addClicked)
    await page.waitForTimeout(4000)
    await shot('D2-ug-add-form')
    result.dialogsAfterAdd = await topDialogs()
    result.addForm = await readForm()
    log('新增表单:', JSON.stringify(result.addForm).slice(0, 500))

    // 表单内 dojo 下拉（单位选择 / 换算率等）——直接读 widget
    result.addFormSelects = await page.evaluate(`(() => {
      const ug = ${UG_DLG}
      const all = window.registryAll()
      return all.filter(w => /Select|ComboBox/.test(String(w.declaredClass || '')) && w.domNode).map(w => ({
        id: w.id,
        cls: String(w.declaredClass),
        inUGList: ug ? ug.contains(w.domNode) : false,
        displayedValue: w.get ? String(w.get('displayedValue') || '') : '',
        value: w.value,
        options: (w.options || []).map(o => ({ label: o.label, value: o.value })).slice(0, 30),
      }))
    })()`)
    log('全部下拉 widget:', JSON.stringify(result.addFormSelects).slice(0, 600))
    await shot('D3-ug-add-dropdown')

    // ── C. 关闭新增表单 → 行内「修改」回填 ──
    await page.evaluate(() => {
      const b = [...document.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '关闭(Esc)')
      if (b.length) { const n = b[b.length - 1].closest('.dijitButton') || b[b.length - 1]; (n.querySelector('.dijitButtonNode') || n).click() }
    })
    await page.waitForTimeout(2500)
    const editClicked = await page.evaluate(`(() => {
      const dlg = ${UG_DLG}
      if (!dlg) return 'no-dialog'
      const b = [...dlg.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '修改')
      if (!b.length) return 'no-edit'
      const n = b[0].closest('.dijitButton') || b[0]
      ;(n.querySelector('.dijitButtonNode') || n).click()
      return 'clicked'
    })()`)
    log('点行内修改:', editClicked)
    await page.waitForTimeout(4000)
    await shot('D4-ug-edit-form')
    result.editForm = await readForm()
    log('修改表单回填:', JSON.stringify(result.editForm && result.editForm.text ? result.editForm.text.slice(0, 200) : 'null'))

    // ── D. 关闭 → 行内「更多」菜单 ──
    await page.evaluate(() => {
      const b = [...document.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '关闭(Esc)')
      if (b.length) { const n = b[b.length - 1].closest('.dijitButton') || b[b.length - 1]; (n.querySelector('.dijitButtonNode') || n).click() }
    })
    await page.waitForTimeout(2500)
    const moreClicked1 = await page.evaluate(`(() => {
      const dlg = ${UG_DLG}
      if (!dlg) return 'no-dialog'
      const b = [...dlg.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '更多')
      if (!b.length) return 'no-more'
      const n = b[0].closest('.dijitButton') || b[0]
      ;(n.querySelector('.dijitButtonNode') || n).click()
      return 'clicked'
    })()`)
    log('点行内更多:', moreClicked1)
    await page.waitForTimeout(2000)
    await shot('D5-ug-more-menu')
    result.moreMenu = await page.evaluate(() => {
      const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
      const vis = el => { const r = el.getBoundingClientRect(); return r.width > 20 && r.height > 10 }
      return [...document.querySelectorAll('.dijitMenuPopup, .dijitPopup, .dijitMenu')].filter(vis).map(txt).filter(Boolean).slice(0, 6)
    })
    log('更多菜单:', JSON.stringify(result.moreMenu))
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(1000)

    // ── E. 查询区「显示状态」下拉选项（点开读菜单） ──
    const statusOpen = await page.evaluate(`(() => {
      const dlg = ${UG_DLG}
      if (!dlg) return 'no-dialog'
      const s = [...dlg.querySelectorAll('.dijitSelect, .dijitDownArrowButton')].filter(e => e.getBoundingClientRect().width > 10)
      if (!s.length) return 'no-select'
      s[s.length - 1].click()
      return 'clicked'
    })()`)
    await page.waitForTimeout(2000)
    await shot('D6-ug-status-dropdown')
    result.statusMenu = await page.evaluate(() => {
      const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
      const vis = el => { const r = el.getBoundingClientRect(); return r.width > 20 && r.height > 10 }
      return [...document.querySelectorAll('.dijitMenuPopup, .dijitPopup, .dijitMenu')].filter(vis).map(txt).filter(Boolean).slice(0, 6)
    })
    log('显示状态选项:', JSON.stringify(result.statusMenu), 'open=', statusOpen)

    save('probe-result.json', result)
  } catch (e) {
    log('FATAL', e.message)
    save('probe-error.txt', String(e.stack || e.message))
    await shot('Z-fatal').catch(() => {})
  } finally {
    await browser.close()
  }
})()
