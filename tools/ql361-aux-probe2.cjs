/* ql361 商品辅助资料（资料→商品管理→商品辅助资料）深度探针 v2
 * 目的：
 *   1. 三 Tab（商品品牌/商品单位/商品标签）结构 + 截图
 *   2. 各 Tab 列配置弹窗（全量列 + 默认显示）
 *   3. 「单位组管理」真实响应（重点：单位字典为空时是否禁用）
 *   4. 标签 Tab 工具栏/查询区是否为空
 * 运行： node tools/ql361-aux-probe2.cjs
 * 输出： tool-results/ql361/pages/aux-probe2/
 */
const BOOTLOG = 'I:/AI-Ready/tool-results/ql361/pages/aux-probe2/boot.log'
try { require('fs').appendFileSync(BOOTLOG, `boot ${new Date().toISOString()}\n`) } catch {}
process.on('uncaughtException', e => {
  try { require('fs').appendFileSync(BOOTLOG, `UNCAUGHT ${e.stack || e.message}\n`) } catch {}
  console.error('UNCAUGHT', e)
})
process.on('unhandledRejection', e => {
  try { require('fs').appendFileSync(BOOTLOG, `UNHANDLED ${(e && e.stack) || e}\n`) } catch {}
  console.error('UNHANDLED', e)
})
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
try { require('fs').appendFileSync(BOOTLOG, 'require playwright OK\n') } catch {}
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const OUT = path.resolve(__dirname, '../tool-results/ql361/pages/aux-probe2')
fs.mkdirSync(OUT, { recursive: true })

const RUNLOG = path.join(OUT, 'run.log')
try { fs.writeFileSync(RUNLOG, '') } catch {}
const log = (...a) => {
  const line = `[aux2] ${a.map(x => (typeof x === 'string' ? x : JSON.stringify(x))).join(' ')}`
  console.log(line)
  try { fs.appendFileSync(RUNLOG, line + '\n') } catch {}
  try { fs.appendFileSync(BOOTLOG, line + '\n') } catch {}
}
const save = (name, obj) => {
  fs.writeFileSync(path.join(OUT, name), typeof obj === 'string' ? obj : JSON.stringify(obj, null, 1), 'utf8')
  log('saved ->', name)
}

/** 动作级重登：检测重登弹窗/登录页 */
async function ensureAlive(page) {
  const need = await page.evaluate(() => {
    if (/\/Account\/Logon/i.test(location.href)) return true
    const pw = [...document.querySelectorAll('input[type=password]')].some(e => e.offsetParent !== null || e.getBoundingClientRect().width > 10)
    return pw
  }).catch(() => false)
  if (!need) return true
  log('!! 会话失效，重登中')
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {})
  const r = await loginQl361(page, page.context(), log)
  log('重登结果', JSON.stringify(r))
  await page.waitForTimeout(3000)
  if (!r.ok) return false
  // 重新回到商品辅助资料
  await navToAux(page)
  return true
}

/** 关闭 dojo 弹窗：仅当存在真实可见 dialog 时才点，且只在 dialog 容器内点关闭
 * （踩坑：全页 [title=关闭] 会命中应用标签的关闭按钮，把整个业务页关掉） */
async function closeDialogs(page) {
  await page.keyboard.press('Escape').catch(() => {})
  const closed = await page.evaluate(() => {
    const reallyVisible = el => {
      const r = el.getBoundingClientRect()
      if (r.width < 120 || r.height < 80) return false
      let n = el
      while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
      return true
    }
    const dlgs = [...document.querySelectorAll('.dojoxDialog, .dojoxDialogWrapper, [role=dialog]')].filter(reallyVisible)
    let n = 0
    for (const d of dlgs) {
      const btn = [...d.querySelectorAll('.dojoxDialogCloseIcon, .lk-dialog-close, [title=关闭], [aria-label=Close]')]
        .find(b => b.getBoundingClientRect().width > 0)
      if (btn) { btn.click(); n++ }
    }
    return n
  }).catch(() => 0)
  await page.waitForTimeout(800)
  return closed
}

/** 桌面导航：资料 → 商品管理 → 商品辅助资料 */
async function navToAux(page) {
  await closeDialogs(page)
  await page.locator('a.menu-0-item', { hasText: '资料' }).first().click({ force: true })
  await page.waitForTimeout(2000)
  const items = await page.evaluate(() => [...document.querySelectorAll('.popupmenu__nav-text')].map(e => (e.innerText || '').trim().replace(/\s+/g, '')))
  save('menu-items.json', items)
  const grp = page.locator('.popupmenu__nav-text', { hasText: '商品管理' }).first()
  if (await grp.count()) { await grp.hover().catch(() => {}); await page.waitForTimeout(1500) }
  const ok = await page.evaluate(() => {
    const el = [...document.querySelectorAll('.popupmenu__nav-text')].find(e => (e.innerText || '').trim().replace(/\s+/g, '') === '商品辅助资料')
    if (el) { el.click(); return true }
    return false
  })
  log('click 商品辅助资料:', ok)
  // 轮询等待业务视图渲染（ql361 桌面版加载较重）
  for (let i = 0; i < 15; i++) {
    await page.waitForTimeout(2000)
    const ready = await page.evaluate(() => !!document.querySelector('.view.GoodBrandList, .view.GoodUnitList, .view.GoodsTags'))
    if (ready) { log('aux view ready @', (i + 1) * 2, 's'); break }
    if (i === 14) {
      const diag = await page.evaluate(() => ({
        viewLikeClasses: [...new Set([...document.querySelectorAll('[class*=view]')].map(e => String(e.className)))].slice(0, 25),
        tabs: [...document.querySelectorAll('.dijitTab')].map(t => (t.innerText || '').trim()),
        btns: [...new Set([...document.querySelectorAll('.dijitButtonText, button, [class*=btn]')].map(e => (e.innerText || '').trim()))].filter(Boolean).slice(0, 40),
      }))
      log('DIAG(未就绪):', JSON.stringify(diag))
      save('diag-not-ready.json', diag)
    }
  }
  // 不关闭其它标签：目标视图用 .view.GoodXxx 精确限定作用域即可
  // （曾用关闭其它标签规避抓错，但会把目标页本身关掉）
  await page.keyboard.press('Escape').catch(() => {})
  await page.waitForTimeout(1500)
}

/** 切换到子标签（商品品牌/商品单位/商品标签）：优先 dojo registry.selectChild */
async function switchAuxTab(page, label) {
  const viewMap = { 商品品牌: 'GoodBrandList', 商品单位: 'GoodUnitList', 商品标签: 'GoodsTags' }
  const want = viewMap[label]
  const byRegistry = await page.evaluate(l => {
    const all = registryAll()
    const tcs = all.filter(w => /TabContainer/.test(String(w.declaredClass || '')))
    if (!tcs.length) return 'no-tabcontainer:' + all.length
    for (const tc of tcs) {
      const kids = tc.getChildren ? tc.getChildren() : []
      const kid = kids.find(c => String(c.title || '').trim() === l)
      if (kid) { tc.selectChild(kid); return 'selected:' + kid.id }
    }
    return 'no-child:' + tcs.map(t => (t.getChildren ? t.getChildren() : []).map(c => String(c.title || '')).join(',')).join(' || ')
  }, label).catch(e => 'err:' + e.message)
  log(`switchAuxTab(${label}) registry ->`, byRegistry)

  // 回退 1：dojo TabButton widget（ContentPane 懒创建时 getChildren 里还没有）
  if (!String(byRegistry).startsWith('selected:')) {
    const byBtn = await page.evaluate(l => {
      const btns = (window.registryAll ? window.registryAll() : []).filter(w => w && /TabButton/.test(String(w.declaredClass || '')))
      const b = btns.find(w => String(w.label || w.title || (w.containerNode && w.containerNode.innerText) || '').trim() === l)
      if (!b) return 'no-btn:' + btns.map(w => String(w.label || w.title || '').trim()).join('|')
      if (typeof b.onClick === 'function') b.onClick()
      else if (b.domNode) b.domNode.click()
      return 'clicked:' + b.id
    }, label).catch(e => 'err:' + e.message)
    log(`switchAuxTab(${label}) tabButton ->`, byBtn)
  }
  // 回退 2：DOM 点击
  if (!String(byRegistry).startsWith('selected:')) {
    await page.evaluate(l => {
      const cand = [...document.querySelectorAll('.dijitTab, .dijitTabInnerDiv, [role=tab]')]
        .filter(e => (e.innerText || '').trim() === l)
      if (cand.length) cand[cand.length - 1].click()
    }, label).catch(() => {})
  }
  // 轮询等待目标视图出现且可见
  for (let i = 0; i < 10; i++) {
    await page.waitForTimeout(1000)
    const shown = await page.evaluate(v => {
      const el = document.querySelector(`.view.${v}`)
      if (!el) return false
      const r = el.getBoundingClientRect()
      if (r.width < 100) return false
      let n = el
      while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
      return true
    }, want).catch(() => false)
    if (shown) { log(`switchAuxTab(${label}) 视图就绪 @${i + 1}s`); return true }
  }
  const diag = await page.evaluate(() => [...document.querySelectorAll('.view')].map(e => String(e.className)))
  log(`switchAuxTab(${label}) 失败，现有 view:`, JSON.stringify(diag.slice(0, 10)))
  return false
}

/** 当前激活的业务面板（GoodBrandList / GoodUnitList / GoodsTags） */
const dumpPanel = page => page.evaluate(() => {
  const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
  const reallyVisible = el => {
    if (!el) return false
    const r = el.getBoundingClientRect()
    if (r.width < 50 || r.height < 20) return false
    let n = el
    while (n) {
      const s = getComputedStyle(n)
      if (s.display === 'none' || s.visibility === 'hidden') return false
      n = n.parentElement
    }
    return true
  }
  const vis = el => !!el && el.getBoundingClientRect().width > 0 && el.getBoundingClientRect().height > 0
  const out = { url: location.href }
  // 业务视图容器
  const roots = [...document.querySelectorAll('.view.GoodBrandList, .view.GoodUnitList, .view.GoodsTags')]
  out.viewClasses = roots.map(e => String(e.className))
  const root = roots.find(reallyVisible) || roots[0]
  out.rootCls = root ? String(root.className) : ''
  const scope = root || document
  out.scopeText = txt(scope).slice(0, 1200)

  // 工具栏按钮：dijit 按钮 + 普通按钮
  out.dijitBtns = [...scope.querySelectorAll('.dijitButton, .dijitButtonNode')].filter(vis).map(txt).filter(Boolean)
  out.plainBtns = [...scope.querySelectorAll('button, [role=button], a[class*=btn]')].filter(vis).map(txt).filter(t => t && t.length < 24)

  // 查询区输入
  out.inputs = [...scope.querySelectorAll('input, select')].filter(vis)
    .map(e => ({ type: e.type, ph: e.getAttribute('placeholder') || '', cls: String(e.className).slice(0, 40) }))

  // 表头
  out.headers = [...scope.querySelectorAll('.dgrid-header .dgrid-cell, [role=columnheader], th')].filter(vis).map(txt).filter(Boolean)

  // 行操作（前 3 行）
  out.rows = [...scope.querySelectorAll('.dgrid-row, tbody tr')].filter(vis).slice(0, 5).map(tr => {
    const cells = [...tr.querySelectorAll('.dgrid-cell, td')].filter(vis).map(txt)
    const acts = [...tr.querySelectorAll('[class*=icon-], .dijitButton, a')].filter(vis).map(e => String(e.className).slice(0, 40))
    return { cells, acts }
  })

  // 分页
  out.pager = [...scope.querySelectorAll('[class*=pager], [class*=footer], [class*=Pagination]')].filter(vis).map(txt).filter(Boolean).slice(0, 4)

  // 齿轮/配置入口
  out.gears = [...scope.querySelectorAll('.icon-shezhi2, [class*=shezhi]')].filter(vis).map(e => ({ cls: String(e.className).slice(0, 60), title: e.getAttribute('title') || '' }))
  out.configGear = [...document.querySelectorAll('[id*=iconButtonConfig], [class*=iconButtonConfig]')].filter(vis).map(e => ({ id: e.id || '', cls: String(e.className).slice(0, 60), title: e.getAttribute('title') || '' }))
  return out
})

/** 读列配置弹窗（个人/全局配置） */
const dumpColConfig = page => page.evaluate(() => {
  const txt = el => (el ? (el.innerText || '').trim() : '')
  const vis = el => !!el && el.getBoundingClientRect().width > 0
  const dlgs = [...document.querySelectorAll('[class*=Dialog], [role=dialog]')].filter(vis)
    .map(d => ({ text: txt(d), el: d }))
    .filter(d => /全局配置/.test(d.text) || /个人配置/.test(d.text))
    .sort((a, b) => b.text.length - a.text.length)
  if (!dlgs.length) return null
  const dlg = dlgs[0].el
  const rows = [...dlg.querySelectorAll('tr')].map(tr => {
    const tds = [...tr.querySelectorAll('td')].map(txt)
    const cb = tr.querySelector('input[type=checkbox], .dijitCheckBox, .dijitCheckBoxInput')
    return { cells: tds, checked: cb ? (cb.checked ?? /Checked/.test(String(cb.className))) : null }
  }).filter(r => r.cells.length >= 2)
  return { text: txt(dlg).slice(0, 400), rows }
})

;(async () => {
  log('IIFE start')
  const browser = await chromium.launch({
    headless: true,
    args: ['--disable-blink-features=AutomationControlled'],
  })
  log('browser launched')
  log('browser launched')
  const ctx = await browser.newContext({
    viewport: { width: 1680, height: 950 },
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
  })
  await ctx.addInitScript(() => {
    Object.defineProperty(navigator, 'webdriver', { get: () => false })
    Object.defineProperty(navigator, 'languages', { get: () => ['zh-CN', 'zh'] })
    // dojo registry 全量取（1.x：toArray / _hash / forEach 三种形态）
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
  const errors = []
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text().slice(0, 200)) })
  const shot = (n, full = false) => page.screenshot({ path: path.join(OUT, `${n}.png`), fullPage: full })
  const result = {}

  try {
    await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(2500)
    const lr = await loginQl361(page, ctx, log)
    log('login:', JSON.stringify(lr))
    if (!lr.ok) throw new Error('登录失败')
    await page.waitForTimeout(4000)
    await closeDialogs(page)
    await navToAux(page)
    await shot('A0-page')
    result.menuItems = (await page.evaluate(() => [...document.querySelectorAll('.popupmenu__nav-text')].map(e => (e.innerText || '').trim()))).slice(0, 5)

    // ── Tab1 商品品牌 ──
    await switchAuxTab(page, '商品品牌')
    await shot('A1-brand')
    result.brand = await dumpPanel(page)
    // dojo registry 全量诊断（TabButton / TabContainer / ContentPane）
    result.registry = await page.evaluate(() => {
      const all = window.registryAll ? window.registryAll() : []
      const pick = (re, map) => all.filter(w => re.test(String(w.declaredClass || ''))).map(map)
      return {
        count: all.length,
        tabButtons: pick(/TabButton/, w => ({ id: w.id, cls: String(w.declaredClass), label: String(w.label || w.title || (w.containerNode ? w.containerNode.innerText : '') || '').trim() })),
        tabContainers: pick(/TabContainer/, w => ({ id: w.id, cls: String(w.declaredClass), kids: (w.getChildren ? w.getChildren() : []).map(c => ({ id: c.id, title: String(c.title || ''), cls: String(c.declaredClass) })) })),
      }
    })
    save('registry.json', result.registry)
    log('registry tabButtons:', JSON.stringify(result.registry.tabButtons))

    // ── Tab2 商品单位 ──
    await switchAuxTab(page, '商品单位')
    await shot('A2-unit')
    result.unit = await dumpPanel(page)

    // 单位组管理：点击并连续观察 24s
    const ugLog = []
    const beforeDialogs = await page.evaluate(() => document.querySelectorAll('[class*=Dialog]').length)
    const clickedUG = await page.evaluate(() => {
      const all = [...document.querySelectorAll('*')].filter(e => (e.textContent || '').trim() === '单位组管理')
      if (!all.length) return { ok: false, reason: 'no-text' }
      const deepest = all[all.length - 1]
      let target = deepest
      for (let i = 0; i < 6 && target; i++) {
        if (/dijitButton|ButtonNode|btn/i.test(String(target.className))) break
        target = target.parentElement
      }
      const el = target || deepest
      ;(el.querySelector && el.querySelector('.dijitButtonNode')) ? el.querySelector('.dijitButtonNode').click() : el.click()
      return { ok: true, tag: el.tagName, cls: String(el.className).slice(0, 80) }
    }).catch(e => ({ ok: false, reason: String(e.message) }))
    log('click 单位组管理:', JSON.stringify(clickedUG))
    for (let i = 1; i <= 6; i++) {
      await page.waitForTimeout(4000)
      const st = await page.evaluate(() => {
        const txt = el => (el ? (el.innerText || '').trim().replace(/\s+/g, ' ') : '')
        const vis = el => { const r = el.getBoundingClientRect(); return r.width > 30 && r.height > 20 }
        const dlgs = [...document.querySelectorAll('[class*=Dialog], [role=dialog], .dojoxDialog')].filter(vis)
          .map(d => txt(d).slice(0, 500)).filter(t => t.length > 10)
        const menus = [...document.querySelectorAll('[class*=popupmenu], [class*=Menu]')].filter(vis).map(d => txt(d).slice(0, 200)).filter(t => t)
        return { tabCount: document.querySelectorAll('.dijitTab').length, dlgs, menus }
      })
      ugLog.push({ t: i * 4, ...st })
      log(`UG +${i * 4}s dialogs=`, JSON.stringify(st.dlgs).slice(0, 300), 'menus=', JSON.stringify(st.menus).slice(0, 150))
      if (st.dlgs.length > 0) { await shot(`A2b-unitgroup-${i * 4}s`); break }
    }
    result.unitGroupProbe = { clicked: clickedUG, beforeDialogs, log: ugLog }
    await shot('A2z-after-unitgroup')

    // ── Tab3 商品标签 ──
    await switchAuxTab(page, '商品标签')
    await shot('A3-tag')
    result.tag = await dumpPanel(page)
    result.aliveAfterTabs = await page.evaluate(() => [...document.querySelectorAll('.view.GoodBrandList, .view.GoodUnitList, .view.GoodsTags')].map(e => String(e.className)))

    // ── 各 Tab 列配置（放最后，避免拖垮前面关键信息） ──
    const colCfgTargets = [
      { label: '商品品牌', key: 'brandColCfg', shot: 'A1b-brand-colcfg' },
      { label: '商品单位', key: 'unitColCfg', shot: 'A2c-unit-colcfg' },
      { label: '商品标签', key: 'tagColCfg', shot: 'A3b-tag-colcfg' },
    ]
    for (const t of colCfgTargets) {
      await switchAuxTab(page, t.label)
      const gear = page.locator('.icon-shezhi2').filter({ visible: true }).first()
      if (!(await gear.count())) { log(`列配置(${t.label}) 无齿轮`); continue }
      await gear.click({ force: true }).catch(() => {})
      await page.waitForTimeout(2500)
      await shot(t.shot)
      result[t.key] = await dumpColConfig(page)
      log(`列配置(${t.label}) ->`, JSON.stringify(result[t.key] && result[t.key].rows ? result[t.key].rows.filter(r => r.checked !== null).length : 'null'), '列')
      await closeDialogs(page)
    }

    // 页面配置齿轮（判定有无「页面配置弹窗」）
    result.pageConfigGears = await page.evaluate(() => [...document.querySelectorAll('[id*=iconButtonConfig], [class*=iconButtonConfig], [title=配置]')]
      .filter(e => e.getBoundingClientRect().width > 5)
      .map(e => ({ id: e.id || '', cls: String(e.className).slice(0, 80), title: e.getAttribute('title') || '' })))

    result.consoleErrors = errors
    save('probe-result.json', result)
  } catch (e) {
    log('FATAL', e.message)
    save('probe-error.txt', String(e.stack || e.message))
    await shot('Z-fatal').catch(() => {})
  } finally {
    await browser.close()
  }
})()
