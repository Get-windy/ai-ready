/* ql361「单位组管理」弹窗深探针（第二轮）：
 *   - 弹窗内工具栏/查询/列头/行
 *   - 列配置弹窗（全量列 + 默认显示）
 *   - 「新增单位组」表单字段（含下拉展开）
 *   - 行内「更多」菜单项
 * 运行： node tools/ql361-aux-unitgroup-probe2.cjs
 * 输出： tool-results/ql361/pages/aux-probe4/
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const OUT = path.resolve(__dirname, '../tool-results/ql361/pages/aux-probe4')
fs.mkdirSync(OUT, { recursive: true })
const RUNLOG = path.join(OUT, 'run.log')
try { fs.writeFileSync(RUNLOG, '') } catch {}
const log = (...a) => {
  const line = `[ug2] ${a.map(x => (typeof x === 'string' ? x : JSON.stringify(x))).join(' ')}`
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

  /** 动作级自愈：检测"账号从别处登录"弹窗，填密码 → 确定 */
  async function ensureAlive() {
    const kicked = await page.evaluate(() => {
      const t = document.body ? (document.body.innerText || '') : ''
      const pw = [...document.querySelectorAll('input[type=password]')].some(e => e.getBoundingClientRect().width > 10)
      return t.includes('重新登录') || pw
    }).catch(() => false)
    if (!kicked) return false
    log('!! 检测到重登弹窗，自动重登')
    await page.evaluate(pwd => {
      const inp = [...document.querySelectorAll('input[type=password]')].find(e => e.getBoundingClientRect().width > 10)
      if (inp) {
        const set = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set
        set.call(inp, pwd)
        inp.dispatchEvent(new Event('input', { bubbles: true }))
        inp.dispatchEvent(new Event('change', { bubbles: true }))
      }
      const btns = [...document.querySelectorAll('.dijitButtonText, button')].filter(e => /^(确定|登录|重新登录)$/.test((e.innerText || '').trim()))
      if (btns.length) {
        const b = btns[btns.length - 1]
        ;((b.closest('.dijitButton') || b).querySelector('.dijitButtonNode') || b).click()
      }
    }, process.env.QL361_PWD || 'Ysh790921').catch(() => {})
    await page.waitForTimeout(5000)
    return true
  }

  /** 找「单位组管理」弹窗（含"新增单位组"和"单位关系"的最小可见容器） */
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
    cands.sort((a, b) => (a.getBoundingClientRect().width * a.getBoundingClientRect().height) - (b.getBoundingClientRect().width * b.getBoundingClientRect().height))
    return cands[0] || null
  })()`

  const readUGDialog = () => page.evaluate(`(() => {
    const dlg = ${UG_DLG}
    if (!dlg) return { error: 'no-dialog' }
    const txt = el => (el ? (el.innerText || '').trim().replace(/\\s+/g, ' ') : '')
    const vis = el => { const r = el.getBoundingClientRect(); return r.width > 10 && r.height > 8 }
    return {
      title: txt(dlg.querySelector('[class*=Title], [class*=title]')).slice(0, 60),
      text: txt(dlg).slice(0, 900),
      btns: [...new Set([...dlg.querySelectorAll('.dijitButtonText')].filter(vis).map(txt).filter(Boolean))],
      inputs: [...dlg.querySelectorAll('input, select')].filter(vis).map(i => ({
        type: i.type, ph: i.getAttribute('placeholder') || '', val: i.value || '', cls: String(i.className).slice(0, 40),
      })),
      selects: [...dlg.querySelectorAll('.dijitSelect, [class*=Select]')].filter(vis).map(s => txt(s).slice(0, 30)),
      headers: [...new Set([...dlg.querySelectorAll('.dgrid-header .dgrid-cell, [role=columnheader], th')].filter(vis).map(txt).filter(Boolean))],
      rows: [...dlg.querySelectorAll('.dgrid-row, tbody tr')].filter(vis).slice(0, 7).map(tr => [...tr.querySelectorAll('.dgrid-cell, td')].map(txt).filter(Boolean)),
      gears: [...dlg.querySelectorAll('.icon-shezhi2, [class*=shezhi]')].filter(vis).map(e => ({ cls: String(e.className).slice(0, 50), title: e.getAttribute('title') || '' })),
    }
  })()`)

  const readColCfg = () => page.evaluate(`(() => {
    const txt = el => (el ? (el.innerText || '').trim() : '')
    const vis = el => { const r = el.getBoundingClientRect(); return r.width > 20 && r.height > 10 }
    const dlgs = [...document.querySelectorAll('[class*=Dialog], [role=dialog]')].filter(vis)
      .map(d => ({ t: txt(d), el: d })).filter(d => /全局配置|个人配置/.test(d.t))
      .sort((a, b) => b.t.length - a.t.length)
    if (!dlgs.length) return null
    const dlg = dlgs[0].el
    const rows = [...dlg.querySelectorAll('tr')].map(tr => {
      const tds = [...tr.querySelectorAll('td')].map(txt)
      const cb = tr.querySelector('input[type=checkbox], .dijitCheckBoxInput')
      return { cells: tds.slice(0, 4), checked: cb ? (cb.checked ?? /Checked/.test(String(cb.className))) : null }
    }).filter(r => r.cells.length >= 2)
    return { text: txt(dlg).slice(0, 500), rows }
  })()`)

  /** 读当前最上层的可见表单弹窗 */
  const readTopDialog = () => page.evaluate(`(() => {
    const txt = el => (el ? (el.innerText || '').trim().replace(/\\s+/g, ' ') : '')
    const vis = el => {
      const r = el.getBoundingClientRect()
      if (r.width < 150 || r.height < 60) return false
      let n = el
      while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
      return true
    }
    const cands = [...document.querySelectorAll('div')].filter(e => {
      if (!vis(e)) return false
      const t = e.innerText || ''
      return (t.includes('保存') || t.includes('确定')) && e.querySelectorAll('.dijitButtonText').length >= 1 && e.querySelectorAll('input').length >= 1
    })
    cands.sort((a, b) => (a.getBoundingClientRect().width * a.getBoundingClientRect().height) - (b.getBoundingClientRect().width * b.getBoundingClientRect().height))
    const dlg = cands[0]
    if (!dlg) return { error: 'no-form-dialog' }
    return {
      text: txt(dlg).slice(0, 800),
      rows: [...dlg.querySelectorAll('tr')].filter(vis).map(tr => {
        const tds = [...tr.querySelectorAll('td')]
        const ctrl = tds[1] ? tds[1] : null
        const inp = ctrl ? ctrl.querySelector('input, select') : null
        return {
          label: txt(tds[0]).slice(0, 30),
          ctrlCls: ctrl ? String(ctrl.className).slice(0, 60) : '',
          ctrlText: ctrl ? txt(ctrl).slice(0, 40) : '',
          inputType: inp ? inp.type : '',
          inputValue: inp ? (inp.value || '') : '',
          hasSelect: ctrl ? !!ctrl.querySelector('[class*=Select], [class*=select], [class*=combo]') : false,
        }
      }).filter(r => r.label || r.ctrlCls),
      btns: [...new Set([...dlg.querySelectorAll('.dijitButtonText')].map(txt).filter(Boolean))],
      checks: [...dlg.querySelectorAll('input[type=checkbox]')].filter(vis).map(i => {
        const w = i.closest('tr, div')
        return { label: w ? txt(w).slice(0, 40) : '', checked: i.checked }
      }),
      html: dlg.innerHTML.slice(0, 5000),
    }
  })()`)

  try {
    await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(2500)
    const lr = await loginQl361(page, ctx, log)
    log('login:', JSON.stringify(lr))
    if (!lr.ok) throw new Error('登录失败')
    await page.waitForTimeout(4000)

    // ── 导航（带校验重试） ──
    let opened = false
    for (let attempt = 1; attempt <= 3 && !opened; attempt++) {
      await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true })
      await page.waitForTimeout(2500)
      const grp = page.locator('.popupmenu__nav-text', { hasText: '商品管理' }).first()
      if (await grp.count()) { await grp.hover().catch(() => {}); await page.waitForTimeout(1800) }
      const clicked = await page.evaluate(() => {
        const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品辅助资料')
        if (el) { el.click(); return true }
        return false
      })
      log(`nav attempt ${attempt}, clicked=${clicked}`)
      for (let i = 0; i < 10; i++) {
        await page.waitForTimeout(2000)
        const ok = await page.evaluate(() => {
          const v = document.querySelector('.view.GoodBrandList, .view.GoodUnitList, .view.GoodsTags')
          if (!v) return false
          const r = v.getBoundingClientRect()
          if (r.width < 100) return false
          let n = v
          while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
          return true
        })
        if (ok) { opened = true; break }
      }
      if (!opened) { log('未打开商品辅助资料，重试'); await page.keyboard.press('Escape').catch(() => {}) }
    }
    if (!opened) throw new Error('无法打开商品辅助资料')
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(1500)
    log('商品辅助资料已打开')
    result.diagTabs = await page.evaluate(() => [...document.querySelectorAll('.dijitTab')].map(t => (t.innerText || '').trim()))

    // ── 切到「商品单位」 ──
    // 关键：先 selectChild 当前已激活的「商品品牌」触发子容器初始化，再切「商品单位」
    let switched = false
    for (let i = 0; i < 12 && !switched; i++) {
      const r = await page.evaluate(target => {
        const all = window.registryAll()
        const tcs = all.filter(w => /(^|\.)TabContainer$/.test(String(w.declaredClass || '')))
        for (const tc of tcs) {
          const kids = tc.getChildren ? tc.getChildren() : []
          const kid = kids.find(c => String(c.title || '').trim() === target)
          if (kid) { tc.selectChild(kid); return 'tc:' + kid.id }
        }
        // 子容器未初始化：先 selectChild 当前激活项（商品品牌）
        for (const tc of tcs) {
          const kids = tc.getChildren ? tc.getChildren() : []
          const cur = kids.find(c => String(c.title || '').trim() === '商品品牌')
          if (cur) { tc.selectChild(cur); return 'prime-brand' }
        }
        const btns = all.filter(w => /TabButton/.test(String(w.declaredClass || '')))
        const b = btns.find(w => String(w.label || w.title || '').trim() === target)
        if (b) { if (typeof b.onClick === 'function') b.onClick(); else if (b.domNode) b.domNode.click(); return 'btn' }
        const dom = [...document.querySelectorAll('.dijitTab')].filter(e => (e.innerText || '').trim() === target)
        if (dom.length) { dom[dom.length - 1].click(); return 'dom' }
        return 'no|tc=' + tcs.length + '|btn=' + btns.map(w => String(w.label || '').trim()).join(',')
      }, '商品单位')
      await page.waitForTimeout(1800)
      const ok = await page.evaluate(() => {
        const v = document.querySelector('.view.GoodUnitList')
        if (!v) return false
        const r = v.getBoundingClientRect()
        if (r.width < 100) return false
        let n = v
        while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
        return true
      })
      switched = ok
      if (i < 3 || switched) log(`切单位 Tab[${i}] ${r} -> ${ok}`)
    }
    log('单位 Tab 激活:', switched)
    await shot('C1-unit')

    // ── 打开「单位组管理」弹窗（含被踢自愈重试） ──
    let ugOpen = false
    for (let round = 1; round <= 4 && !ugOpen; round++) {
      if (round > 1) {
        await ensureAlive()
        // 被踢后重新进入：关弹窗 → 重新导航 → 切单位
        await page.keyboard.press('Escape').catch(() => {})
        await page.waitForTimeout(1000)
        const reNav = await page.evaluate(() => !!document.querySelector('.view.GoodUnitList'))
        if (!reNav) {
          await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true }).catch(() => {})
          await page.waitForTimeout(2500)
          const g = page.locator('.popupmenu__nav-text', { hasText: '商品管理' }).first()
          if (await g.count()) { await g.hover().catch(() => {}); await page.waitForTimeout(1500) }
          await page.evaluate(() => {
            const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品辅助资料')
            if (el) el.click()
          })
          await page.waitForTimeout(8000)
        }
        await page.evaluate(() => {
          const all = window.registryAll()
          const b = all.filter(w => /TabButton/.test(String(w.declaredClass || ''))).find(w => String(w.label || w.title || '').trim() === '商品单位')
          if (b) { typeof b.onClick === 'function' ? b.onClick() : (b.domNode && b.domNode.click()) }
        })
        await page.waitForTimeout(3000)
      }
      const clickUG = await page.evaluate(() => {
        const btns = [...document.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '单位组管理')
        if (!btns.length) return 'no-btn'
        const node = btns[btns.length - 1].closest('.dijitButton') || btns[btns.length - 1]
        ;(node.querySelector('.dijitButtonNode') || node).click()
        return 'clicked'
      })
      log(`click 单位组管理(第${round}轮):`, clickUG)
      for (let i = 0; i < 8; i++) {
        await page.waitForTimeout(2000)
        const has = await page.evaluate(`!!${UG_DLG}`)
        if (has) { ugOpen = true; log('单位组管理弹窗出现 @', (i + 1) * 2, 's'); break }
      }
      if (!ugOpen) await shot(`C2-fail-round${round}`)
    }
    log('单位组管理弹窗打开:', ugOpen)
    await shot('C2-ug-dialog')
    result.ugDialog = await readUGDialog()
    log('UG 弹窗:', JSON.stringify(result.ugDialog).slice(0, 400))

    // ── 弹窗内列配置 ──
    const gearClick = await page.evaluate(`(() => {
      const dlg = ${UG_DLG}
      if (!dlg) return 'no-dialog'
      const g = [...dlg.querySelectorAll('.icon-shezhi2, [class*=shezhi]')].find(e => e.getBoundingClientRect().width > 5)
      if (!g) return 'no-gear'
      g.click()
      return 'clicked'
    })()`)
    log('UG 列配置齿轮:', gearClick)
    await page.waitForTimeout(2500)
    await shot('C3-ug-colcfg')
    result.ugColCfg = await readColCfg()
    log('UG 列配置列数:', result.ugColCfg ? result.ugColCfg.rows.length : 'null')
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(1500)

    // ── 新增单位组 ──
    const clickAdd = await page.evaluate(() => {
      const btns = [...document.querySelectorAll('.dijitButtonText')].filter(e => (e.innerText || '').trim() === '新增单位组')
      if (!btns.length) return 'no-btn'
      const node = btns[btns.length - 1].closest('.dijitButton') || btns[btns.length - 1]
      ;(node.querySelector('.dijitButtonNode') || node).click()
      return 'clicked'
    })
    log('click 新增单位组:', clickAdd)
    await page.waitForTimeout(4000)
    await shot('C4-ug-add')
    result.ugAdd = await readTopDialog()
    log('新增单位组表单:', JSON.stringify(result.ugAdd && result.ugAdd.text ? result.ugAdd.text.slice(0, 250) : 'null'))
    // 表单内下拉展开（逐个点击 .dijitSelect / combobox，dump 选项）
    const comboOpts = await page.evaluate(() => {
      const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
      const vis = el => { const r = el.getBoundingClientRect(); return r.width > 10 && r.height > 8 }
      const combos = [...document.querySelectorAll('.dijitSelect, .dijitComboBox, [class*=combo]')].filter(vis)
      return combos.map(c => ({ cls: String(c.className).slice(0, 60), text: txt(c).slice(0, 40) }))
    })
    result.ugAddCombos = comboOpts
    save('probe-result.json', result)
  } catch (e) {
    log('FATAL', e.message)
    save('probe-error.txt', String(e.stack || e.message))
    await shot('Z-fatal').catch(() => {})
  } finally {
    await browser.close()
  }
})()
