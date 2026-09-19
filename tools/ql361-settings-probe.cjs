/* ql361 设置域深度探针：
 *   ① 系统参数（设置→系统配置→系统参数）：逐个点开 8 个左标签，抓每张卡片的标题/配置项/控件/取值域/帮助气泡/温馨提示
 *   ② 审核设置（设置→系统配置→审核设置）：逐行点开 16 类单据的「设置」，抓弹窗完整配置结构 + 摘要列文案
 *   ③ 打印设置（设置→系统配置→打印设置）：展开「打印内容」下拉读全部选项 + 逐个点开 6 个模板类目
 *
 * 运行：
 *   node tools/ql361-settings-probe.cjs discover                      # 结构勘探 → 设置-deep/_discover/
 *   node tools/ql361-settings-probe.cjs probe                          # 正式抓取全部三页
 *   node tools/ql361-settings-probe.cjs probe 系统参数 审核设置 打印设置   # 只抓指定页
 *   node tools/ql361-settings-probe.cjs summary                        # 由 JSON 生成 _summary.md
 * 依赖： NODE_PATH=I:/AI-Ready/frontend/node_modules （ql361-lib.cjs 内 require('playwright')）
 *
 * 抓取口径与坑（沿用 tools/ql361-domain-crawl.cjs 既有约定）：
 *   ① ql361 是 MDI 桌面，已打开页面 DOM 会残留 → 每页开抓前先 goto desktop.html 重载；
 *      每次切页/切标签都回读「激活标签」并与目标比对，不等即标记 verified=false（结果不可信）。
 *   ② 左侧菜单 `.popupmenu__list-title` 不可点，只能点 `.popupmenu__list-item`。
 *   ③ 多 Tab/左标签页必须切过去再抓（打印设置的右栏是懒加载，只渲染当前标签内容）。
 *   ④ `?` 帮助气泡是 dojo tooltip，弹层在 body 末尾 → 全 document 找，不能只在卡片内找。
 *   ⑤ 下拉是 dijit Select 浮层（div.dijitPopup），click 后要等渲染再读 option；本脚本绝不点选任何 option。
 *   ⑥ 抓不到一律写「未取到」，不编造。
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const MODE = process.argv[2] || 'probe'
const WANT = process.argv.slice(3)
/** 只跑指定标签（重跑用）：QL361_ONLY_TABS="单据设置,库存设置" */
const ONLY_TABS = (process.env.QL361_ONLY_TABS || '').split(',').map(s => s.trim()).filter(Boolean)
/** 逐个标签重载页面（为了拿到每个标签的 live 帮助气泡：dojo Tooltip 切标签后失效） */
const RELOAD_PER_TAB = process.env.QL361_RELOAD_PER_TAB === '1'
const tabFilter = (list) => ONLY_TABS.length ? list.filter(t => ONLY_TABS.includes(t)) : list
const BASE = 'https://22stable.ql361.com/desktop.html'
const OUT = path.resolve(__dirname, '../tool-results/ql361/设置-deep')
const DISC = path.join(OUT, '_discover')
const SHOTS = path.join(OUT, 'shots')
const STATE = path.resolve(__dirname, '../tool-results/ql361/product/ql361-state.json')

const log = (...a) => console.log('[设置探针]', ...a)
const saveJson = (name, data) => {
  fs.writeFileSync(path.join(OUT, `${name}.json`), JSON.stringify(data, null, 2), 'utf8')
  log('json ->', `${name}.json (${JSON.stringify(data).length}B)`)
  return data
}
const loadJson = (name) => {
  try { return JSON.parse(fs.readFileSync(path.join(OUT, `${name}.json`), 'utf8')) } catch (e) { return null }
}
const saveDisc = (name, data) => {
  fs.mkdirSync(DISC, { recursive: true })
  fs.writeFileSync(path.join(DISC, `${name}.json`), JSON.stringify(data, null, 2), 'utf8')
}
const saveHtml = (name, html) => {
  fs.mkdirSync(DISC, { recursive: true })
  fs.writeFileSync(path.join(DISC, `${name}.html`), html, 'utf8')
  log('disc ->', `${name}.html (${html.length}B)`)
}
const shotPage = async (page, name) => {
  fs.mkdirSync(SHOTS, { recursive: true })
  await page.screenshot({ path: path.join(SHOTS, `${name}.png`) }).catch(() => {})
  return `${name}.png`
}

/* ================= 浏览器内通用小工具（必须自包含，evaluate 会序列化源码） ================= */

const popupVisibleFn = () => {
  const p = document.querySelector('.popupmenu__repeat')
  return !!p && p.getBoundingClientRect().width > 0
}

/** 读取可见的 dojo tooltip（帮助气泡）文本；返回最长的那个 */
const READ_TOOLTIP = () => {
  const vis = e => {
    if (!e) return false
    const r = e.getBoundingClientRect()
    if (r.width < 8 || r.height < 8) return false
    let n = e
    while (n && n !== document.body) {
      const s = getComputedStyle(n)
      if (s.display === 'none' || s.visibility === 'hidden' || s.opacity === '0') return false
      n = n.parentElement
    }
    return true
  }
  const sels = ['.dijitTooltipContainer', '.dijitTooltip', '.dijitTooltipDialog', '.dijitTooltipFocusNode',
    '[class*=tooltip]', '[class*=Tooltip]', '[class*=popover]', '[class*=Popover]']
  const hits = sels.flatMap(s => [...document.querySelectorAll(s)]).filter(vis)
  const texts = [...new Set(hits.map(e => (e.innerText || '').replace(/\s+/g, ' ').trim()).filter(t => t))]
  return { text: texts.sort((a, b) => b.length - a.length)[0] || '', layers: texts.length }
}

/** 读 dijit 下拉浮层里的全部选项（只读不点） */
const READ_POPUP = () => {
  const vis = e => {
    if (!e) return false
    const r = e.getBoundingClientRect()
    return e.offsetParent !== null && r.width > 20 && r.height > 10
  }
  const txt = e => (e ? (e.innerText || '').replace(/\s+/g, ' ').trim() : '')
  const pops = [...document.querySelectorAll('.dijitPopup, .dijitMenuPopup, .dijitComboBoxMenuPopup, .dijitPopupMenuWrapper')]
    .filter(vis)
    // 排除 help 气泡（dojo Tooltip 也是 .dijitPopup，会污染下拉读取）
    .filter(p => !p.querySelector('.dijitTooltipContainer') && !/Tooltip/i.test(p.className || ''))
    // 必须真的像菜单（有菜单项或 tr）
    .filter(p => p.querySelector('.dijitMenuItem, table.dijitSelectMenu, tr'))
  const p = pops[pops.length - 1]
  if (!p) return { found: false, why: 'no popup layer' }
  let items = [...p.querySelectorAll('.dijitMenuItem')].map(e => ({
    label: txt(e),
    selected: /Selected/.test(e.className || ''),
    disabled: /Disabled/.test(e.className || '') || e.getAttribute('aria-disabled') === 'true',
  })).filter(o => o.label)
  if (!items.length) {
    items = [...p.querySelectorAll('tr')].map(e => ({
      label: txt(e), selected: /Selected/.test(e.className || ''), disabled: false,
    })).filter(o => o.label)
  }
  return { found: true, cls: String(p.className).slice(0, 80), count: items.length, items, raw: txt(p).slice(0, 800) }
}

/** 页面主体提取（卡片/字段/开关/帮助归属），限定在当前可见的 tabcontent 内 */
const EXTRACT = (opt) => {
  const vis = e => {
    if (!e) return false
    const r = e.getBoundingClientRect()
    return e.offsetParent !== null && r.width > 0 && r.height > 0
  }
  const txt = e => (e ? (e.innerText || '').replace(/\s+/g, ' ').trim() : '')
  const tipOf = el => {
    if (!el) return ''
    const cands = []
    if (el.getAttribute && el.getAttribute('data-dojo-props')) cands.push(el.getAttribute('data-dojo-props'))
    const inner = el.querySelector ? el.querySelector('[data-dojo-props*="toolTipContent"]') : null
    if (inner) cands.push(inner.getAttribute('data-dojo-props'))
    for (const dp of cands) {
      const m = dp.match(/toolTipContent\s*:\s*'((?:[^'\\]|\\.)*)'/)
      if (m) return m[1].replace(/\\'/g, "'")
    }
    return ''
  }
  // 当前可见的 tab 内容容器（系统参数=div.tabcontent；打印设置的模板类目=div[data-id] 设计器，无 tabcontent 类）
  let scopes = [...document.querySelectorAll('div.tabcontent')]
    .filter(e => e.offsetParent !== null && e.getBoundingClientRect().width > 300)
  if (!scopes.length) scopes = [...document.querySelectorAll('[data-id]')]
    .filter(e => e.offsetParent !== null && e.getBoundingClientRect().width > 300 && e.getBoundingClientRect().height > 150)
  const scope = scopes[scopes.length - 1] || null
  if (!scope) return { err: 'no visible tabcontent', tabId: '' }
  const labelOf = e => {
    const lab = e.querySelector(':scope > label')
    if (!lab) return ''
    const s = lab.querySelector('span:not(.required-code):not(.lk-form-tooltip-help)')
    return (s ? (s.innerText || '') : (lab.innerText || '')).replace(/\s+/g, ' ').trim()
  }
  // 最内层 FormField（有非空 label）
  const allFF = [...scope.querySelectorAll('[data-dojo-type="widget/FormField"]')]
  const innerFF = allFF.filter(e => {
    const kids = [...e.querySelectorAll('[data-dojo-type="widget/FormField"]')].filter(x => labelOf(x))
    return kids.length === 0 && labelOf(e)
  })
  const ctrlOf = e => {
    const out = { 控件: '', 当前值: '', 细节: {} }
    const cb = e.querySelector('input[type=checkbox]')
    const sel = e.querySelector('.dijitSelect')
    const item = e.querySelector('.dijitItemField')
    const radios = [...e.querySelectorAll('input[type=radio]')]
    const inputs = [...e.querySelectorAll('input.dijitInputInner')].filter(i => i.type !== 'checkbox' && i.type !== 'radio' && i.type !== 'hidden')
    if (cb) {
      const wrap = cb.closest('.dijitCheckBox')
      out.控件 = '开关/勾选'
      out.当前值 = (cb.checked || (wrap && /Checked/.test(wrap.className || ''))) ? '开/勾选' : '关/未勾选'
      out.细节 = { name: cb.name || '', checked: !!cb.checked }
    } else if (sel) {
      out.控件 = '下拉(dijitSelect)'
      out.当前值 = txt(sel.querySelector('.dijitSelectLabel'))
      const hid = sel.querySelector('input[type=hidden]')
      out.细节 = { id: sel.id || '', rawValue: hid ? hid.value : '' }
    } else if (item) {
      out.控件 = '选择器(ItemField)'
      const inp = item.querySelector('input.dijitInputInner')
      const rm = item.querySelector('[data-dojo-attach-point="removeBtn"]')
      out.当前值 = inp ? inp.value : ''
      out.细节 = { id: item.id || '', 已选标记: rm ? (rm.style.display === 'none' ? '无' : '有(removeBtn可见)') : '未取到' }
    } else if (radios.length) {
      out.控件 = '单选'
      const arr = radios.map(r => ({ label: txt((r.closest('label') || r.parentElement) || r), checked: !!r.checked }))
      out.当前值 = (arr.find(a => a.checked) || {}).label || ''
      out.细节 = { 选项: arr }
    } else if (inputs.length) {
      const i0 = inputs[0]
      const isNum = /Number|number/.test((i0.type || '') + (i0.className || ''))
      out.控件 = isNum ? '数字输入' : '文本输入'
      out.当前值 = i0.value
      out.细节 = { id: i0.id || '', type: i0.type || '' }
    } else {
      const t = txt(e)
      out.控件 = t ? '只读文本' : '未取到'
      out.当前值 = t.slice(0, 120)
    }
    return out
  }
  const fieldOf = e => {
    const c = ctrlOf(e)
    return {
      名称: labelOf(e),
      控件: c.控件,
      当前值: c.当前值,
      细节: c.细节,
      帮助文案_属性: tipOf(e),
      温馨提示: txt(e.querySelector('.sys-form__tips-text')),
      可见: vis(e),
    }
  }
  // 卡片：带 .sys-list__titlebar 的 li
  const cards = []
  for (const tb of [...scope.querySelectorAll('.sys-list__titlebar')]) {
    const li = tb.closest('li') || tb.parentElement
    const title = txt(tb.querySelector('.sys-list__title > div')) || txt(tb.querySelector('.sys-list__title'))
    const toggleEl = tb.querySelector('label.dijitToggle') || tb.querySelector('label:has(> .toggletext)')
    let toggle = null
    if (toggleEl) {
      const cbi = toggleEl.querySelector('input[type=checkbox]')
      toggle = {
        文案: txt(toggleEl.querySelector('.toggletext')),
        勾选: cbi ? !!cbi.checked : null,
        name: cbi ? cbi.name : '',
        帮助文案_属性: tipOf(toggleEl.parentElement) || tipOf(tb),
      }
    }
    const bodies = [...li.querySelectorAll('.sys-card')]
    const fields = []
    for (const b of bodies) for (const f of [...b.querySelectorAll('[data-dojo-type="widget/FormField"]')]) {
      if (!labelOf(f)) continue
      const kids = [...f.querySelectorAll('[data-dojo-type="widget/FormField"]')].filter(x => labelOf(x))
      if (kids.length) continue
      fields.push(fieldOf(f))
    }
    // 卡片内的子分组标题
    const subTitles = [...li.querySelectorAll('.sys-sublist__title')].map(txt).filter(Boolean)
    cards.push({
      卡片标题: title,
      卡片帮助文案_属性: tipOf(tb.querySelector('.sys-list__title') || tb),
      卡片开关: toggle,
      卡片体可见: bodies.some(vis),
      卡片体数量: bodies.length,
      子分组标题: subTitles,
      配置项: fields,
    })
  }
  // 不在任何卡片里的 FormField（如打印设置主区）
  const loose = []
  for (const f of innerFF) {
    if (f.closest('.sys-card')) continue
    loose.push(fieldOf(f))
  }
  // 帮助图标归属清单（供 live 点击）
  const ownerOfEl = (el) => {
    const tb2 = el.closest('.sys-list__titlebar')
    if (tb2) {
      const t = txt(tb2.querySelector('.sys-list__title > div')) || txt(tb2.querySelector('.sys-list__title'))
      return { owner: (t || '(无标题)').slice(0, 40), ownerKind: '卡片标题' }
    }
    let node = el.parentElement
    while (node && node !== document.body) {
      const labs = [...node.querySelectorAll('label')].filter(l => {
        const t = txt(l)
        if (!t) return false
        return !!(l.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING)
      })
      if (labs.length) return { owner: txt(labs[labs.length - 1]).slice(0, 40), ownerKind: '配置项' }
      node = node.parentElement
    }
    const t = txt(el.closest('[data-dojo-type="widget/FormField"]') || el)
    return { owner: (t || '(未归属)').slice(0, 40), ownerKind: '未知' }
  }
  // ⚠️ scope 里同时装着 8 个标签的内容（非激活标签靠 display:none 隐藏），
  //    innerText 只返回渲染中的文本，但 querySelectorAll 会连隐藏的一起找到 →
  //    打标记必须只打「可见」的，否则会把别的标签的气泡/下拉算进本标签（计数虚高、归属串台）。
  const helpsAll = [...scope.querySelectorAll('.lk-form-tooltip-help')]
  const helpIcons = []
  let hiddenHelp = 0
  helpsAll.forEach((ic) => {
    if (!vis(ic)) { hiddenHelp++; return }
    const i = helpIcons.length
    ic.setAttribute('data-probe-help', String(i))
    helpIcons.push({ idx: i, ...ownerOfEl(ic) })
  })
  // 下拉清单（供 live 展开）
  const selsAll = [...scope.querySelectorAll('.dijitSelect')]
  const selects = []
  let hiddenSel = 0
  selsAll.forEach((s) => {
    if (!vis(s)) { hiddenSel++; return }
    const i = selects.length
    s.setAttribute('data-probe-sel', String(i))
    selects.push({
      idx: i, id: s.id || '',
      owner: ownerOfEl(s).owner,
      当前值: txt(s.querySelector('.dijitSelectLabel')),
      rawValue: (s.querySelector('input[type=hidden]') || {}).value || '',
    })
  })
  return {
    tabId: scope.getAttribute('data-id') || '',
    tabText: txt(scope.querySelector('.tab-list-main .tab-list-title')) || '',
    cards, looseFields: loose, helpIcons, selects,
    隐藏帮助图标数: hiddenHelp, 隐藏下拉数: hiddenSel,
    panelText: txt(scope).slice(0, 3000),
  }
}

/** 通用形态提取：表单控件清单（带最近前置 label）/列表项/按钮/表格/iframe ——
 *  用于「打印模板设计器」这类非 FormField 结构的右栏，避免因结构不同而漏抓。 */
const EXTRACT_SHAPE = () => {
  const vis = e => {
    if (!e) return false
    const r = e.getBoundingClientRect()
    return e.offsetParent !== null && r.width > 0 && r.height > 0
  }
  const txt = e => (e ? (e.innerText || '').replace(/\s+/g, ' ').trim() : '')
  let scopes = [...document.querySelectorAll('div.tabcontent')]
    .filter(e => e.offsetParent !== null && e.getBoundingClientRect().width > 300)
  if (!scopes.length) scopes = [...document.querySelectorAll('[data-id]')]
    .filter(e => e.offsetParent !== null && e.getBoundingClientRect().width > 300 && e.getBoundingClientRect().height > 150)
  const scope = scopes[scopes.length - 1] || document.body
  const labelBefore = (el) => {
    // ⚠️ 只能取「可见」且在本标签容器内、且在控件之前的 label，
    //    否则会串到其它（已隐藏）标签的 label 上，出现「值配错名」的假结论。
    let node = el.parentElement
    while (node && node !== document.body) {
      const labs = [...node.querySelectorAll('label')].filter(l => {
        if (!vis(l)) return false
        const t = txt(l)
        if (!t) return false
        return !!(l.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING)
      })
      if (labs.length) return txt(labs[labs.length - 1]).slice(0, 40)
      if (node === scope) break
      node = node.parentElement
    }
    return ''
  }
  // 卡片级的「开/关」拨动开关不算配置项（已在卡片小节单独记录），这里排掉，避免兜底清单变噪音
  const isToggle = e => !!(e.closest && e.closest('label.dijitToggle'))
  const controls = [...scope.querySelectorAll('input,textarea,select')]
    .filter(c => vis(c) && !isToggle(c))
    .map(c => {
      const kind = c.tagName === 'INPUT' ? (c.type || 'text') : c.tagName.toLowerCase()
      return {
        控件: kind,
        标签: labelBefore(c) || c.placeholder || c.getAttribute('aria-label') || '',
        值: (c.type === 'checkbox' || c.type === 'radio') ? (c.checked ? '勾选' : '未勾选') : (c.value || ''),
        placeholder: c.placeholder || '',
      }
    })
    .filter(c => !/^(开|关|是|否)$/.test(c.标签))
  const tables = [...scope.querySelectorAll('table')].filter(vis).slice(0, 6).map(t => ({
    cls: String(t.className).slice(0, 60),
    headers: [...t.querySelectorAll('thead th')].map(txt),
    rowCount: t.rows.length,
  }))
  return {
    tabId: (scope.getAttribute && scope.getAttribute('data-id')) || '',
    cls: String(scope.className || '').slice(0, 90),
    尺寸: { w: Math.round(scope.scrollWidth), h: Math.round(scope.scrollHeight) },
    controls,
    labels: [...new Set([...scope.querySelectorAll('label')].filter(l => vis(l) && !l.closest('label.dijitToggle')).map(txt).filter(t => t && !/^(开|关|是|否)$/.test(t)))].slice(0, 80),
    字段候选: [...new Set([...scope.querySelectorAll('[class*=input],[class*=field],[class*=form-item],[class*=label],[class*=item],[contenteditable]')]
      .filter(vis).map(txt).filter(t => t && t.length <= 30))].slice(0, 150),
    列表项: [...new Set([...scope.querySelectorAll('li')].filter(vis).map(txt).filter(t => t && t.length < 90))].slice(0, 60),
    按钮: [...new Set([...scope.querySelectorAll('button,[class*=btn],a')].filter(vis).map(txt).filter(t => t && t.length <= 14))].slice(0, 30),
    tables,
    iframes: [...scope.querySelectorAll('iframe')].map(f => ({ src: (f.src || '').slice(0, 120) })),
    text: txt(scope).slice(0, 3000),
  }
}

/** 审核设置：读 dgrid 表格全部行（单据 / 审核设置 / 摘要）
 *  ⚠️ 必须用 td.dgrid-cell 取真单元格：dgrid 行内有嵌套 table/td，直接取全部 td 会把整行文本当成第一列。 */
const READ_AUDIT_ROWS = () => {
  const txt = e => (e ? (e.innerText || '').replace(/\s+/g, ' ').trim() : '')
  const out = []
  for (const r of [...document.querySelectorAll('.dgrid-row')]) {
    const tds = [...r.querySelectorAll('td.dgrid-cell')]
    if (tds.length < 3) continue
    const cells = tds.map(txt)
    const name = cells[0]
    if (!name || /^设置$/.test(name)) continue
    if (out.some(o => o.单据 === name)) continue
    out.push({ 单据: name, 审核设置: cells[1], 摘要: cells[2], 单元格数: tds.length, 列类名: tds.map(t => String(t.className).slice(0, 50)) })
  }
  return out
}

/** 按「单据名」点该行的「设置」链接（不按序号，避免列错位/重复行导致点错） */
const CLICK_AUDIT_SET = (bill) => {
  const vis = e => e.offsetParent !== null
  const txt = e => (e ? (e.innerText || '').replace(/\s+/g, ' ').trim() : '')
  const links = [...document.querySelectorAll('a.grid-cell-link')].filter(e => vis(e) && (e.innerText || '').trim() === '设置')
  const info = links.map((a, i) => {
    const row = a.closest('.dgrid-row') || a.closest('tr')
    const tds = row ? [...row.querySelectorAll('td.dgrid-cell')] : []
    return { i, name: tds.length ? txt(tds[0]) : '' }
  })
  const hit = info.find(x => x.name === bill)
  if (!hit) return { ok: false, why: 'no row match', 可用行名: [...new Set(info.map(x => x.name))] }
  links[hit.i].click()
  return { ok: true, idx: hit.i }
}

/** 读当前可见弹窗（dojo dialog / 通用 layer）结构 */
const READ_DIALOG = () => {
  const vis = e => {
    if (!e) return false
    const r = e.getBoundingClientRect()
    if (r.width < 150 || r.height < 80 || e.offsetParent === null) return false
    let n = e
    while (n && n !== document.body) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
    return true
  }
  const txt = e => (e ? (e.innerText || '').replace(/\s+/g, ' ').trim() : '')
  const cands = [...document.querySelectorAll('.dojoxDialog, .dijitDialog, [role=dialog]')].filter(vis)
  const modal = cands.sort((a, b) => {
    const ra = a.getBoundingClientRect(), rb = b.getBoundingClientRect()
    return rb.width * rb.height - ra.width * ra.height
  })[0]
  if (!modal) return { err: 'no visible dialog' }
  const tipOf = el => {
    if (!el) return ''
    const dp = el.getAttribute('data-dojo-props') || ''
    const m = dp.match(/toolTipContent\s*:\s*'((?:[^'\\]|\\.)*)'/)
    return m ? m[1].replace(/\\'/g, "'") : ''
  }
  const labelOf = e => {
    const lab = e.querySelector(':scope > label')
    if (!lab) return ''
    const s = lab.querySelector('span:not(.required-code):not(.lk-form-tooltip-help)')
    return (s ? (s.innerText || '') : (lab.innerText || '')).replace(/\s+/g, ' ').trim()
  }
  const ctrlOf = e => {
    const cb = e.querySelector('input[type=checkbox]')
    const sel = e.querySelector('.dijitSelect')
    const item = e.querySelector('.dijitItemField')
    const radios = [...e.querySelectorAll('input[type=radio]')]
    const inputs = [...e.querySelectorAll('input.dijitInputInner')].filter(i => !['checkbox', 'radio', 'hidden'].includes(i.type))
    if (cb) {
      const wrap = cb.closest('.dijitCheckBox')
      return { 控件: '开关/勾选', 当前值: (cb.checked || (wrap && /Checked/.test(wrap.className || ''))) ? '开/勾选' : '关/未勾选', 细节: { name: cb.name || '' } }
    }
    if (sel) return { 控件: '下拉(dijitSelect)', 当前值: txt(sel.querySelector('.dijitSelectLabel')), 细节: { id: sel.id || '' } }
    if (item) {
      const rm = item.querySelector('[data-dojo-attach-point="removeBtn"]')
      return {
        控件: '审核人选择器(ItemField)', 当前值: (item.querySelector('input.dijitInputInner') || {}).value || '',
        细节: { id: item.id || '', 已选标记: rm ? (rm.style.display === 'none' ? '无' : '有(removeBtn可见)') : '未取到' },
      }
    }
    if (radios.length) {
      const arr = radios.map(r => ({ label: txt(r.closest('label') || r.parentElement), checked: !!r.checked }))
      return { 控件: '单选', 当前值: (arr.find(a => a.checked) || {}).label || '', 细节: { 选项: arr } }
    }
    if (inputs.length) {
      const i0 = inputs[0]
      const isNum = /Number/i.test(i0.type + i0.className)
      return { 控件: isNum ? '数字输入' : '文本输入', 当前值: i0.value, 细节: { id: i0.id || '', type: i0.type || '' } }
    }
    return { 控件: '未取到', 当前值: '', 细节: {} }
  }
  const ff = [...modal.querySelectorAll('[data-dojo-type="widget/FormField"]')]
  const fields = []
  for (const f of ff) {
    if (!labelOf(f)) continue
    const kids = [...f.querySelectorAll('[data-dojo-type="widget/FormField"]')].filter(x => labelOf(x))
    if (kids.length) continue
    const c = ctrlOf(f)
    fields.push({
      名称: labelOf(f), 控件: c.控件, 当前值: c.当前值, 细节: c.细节,
      帮助文案_属性: tipOf(f) || tipOf(f.querySelector('[data-dojo-props*="toolTipContent"]')),
      可见: f.offsetParent !== null,
      所属li可见: (f.closest('li') || f).offsetParent !== null,
    })
  }
  // 帮助图标（打标供 live 点击）
  const helpIcons = []
  ;[...modal.querySelectorAll('.lk-form-tooltip-help')].forEach((ic, i) => {
    ic.setAttribute('data-probe-help', String(i))
    const f = ic.closest('[data-dojo-type="widget/FormField"]')
    helpIcons.push({ idx: i, owner: f ? labelOf(f) : '(未归属)' })
  })
  // 审核人选择器的搜索按钮（打标供 live 点开）
  const pickers = []
  ;[...modal.querySelectorAll('.dijitItemField')].forEach((it, i) => {
    const btn = it.querySelector('[data-dojo-attach-point="_searchbtn"]')
    const shown = it.offsetParent !== null
    // 只标记「可见」的搜索按钮：隐藏条件项（li.dijitHidden）里的按钮点了没反应
    if (btn && shown) btn.setAttribute('data-probe-picker', String(i))
    const f = it.closest('[data-dojo-type="widget/FormField"]')
    pickers.push({ idx: i, labeled: f ? labelOf(f) : '', id: it.id || '', 可见: shown, 有搜索按钮: !!btn })
  })
  return {
    title: txt(modal.querySelector('.dojoxDialogTitle, .dijitDialogTitle, [class*=DialogTitle]')) || (txt(modal).split(' ')[0] || ''),
    cls: String(modal.className).slice(0, 120),
    text: txt(modal).slice(0, 4000),
    fields, helpIcons, pickers,
    条件清单_按序: fields.filter(f => /ItemField/.test(f.控件)).map(f => f.名称),
    全部名称_按序: fields.map(f => f.名称),
    buttons: [...new Set([...modal.querySelectorAll('button,[class*=btn]')].filter(vis).map(txt).filter(t => t && t.length <= 12))],
    tableHeaders: [...modal.querySelectorAll('table thead th')].map(txt),
    // 组合方式线索：文案里出现「且/或/任一/同时」等的上下文，用于判定多条件/多审核人的组合语义
    组合方式线索: (() => {
      const t = txt(modal)
      const out = []
      for (const k of ['且', '或', '任一', '同时', '全部', '满足']) {
        let i = t.indexOf(k)
        while (i >= 0 && out.length < 12) { out.push(t.slice(Math.max(0, i - 45), i + 45)); i = t.indexOf(k, i + 1) }
      }
      return [...new Set(out)]
    })(),
    单选项: [...modal.querySelectorAll('input[type=radio]')].filter(vis).map(r => ({
      label: txt(r.closest('label') || r.parentElement), checked: !!r.checked,
    })),
  }
}

/** 列出当前所有可见弹窗（审核人选择器是叠在审核设置弹窗之上的第二层，不能只取“最大那个”） */
const READ_ALL_DIALOGS = () => {
  const vis = e => {
    if (!e) return false
    const r = e.getBoundingClientRect()
    if (r.width < 150 || r.height < 80 || e.offsetParent === null) return false
    let n = e
    while (n && n !== document.body) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
    return true
  }
  const txt = e => (e ? (e.innerText || '').replace(/\s+/g, ' ').trim() : '')
  return [...document.querySelectorAll('.dojoxDialog, .dijitDialog, [role=dialog]')].filter(vis).map(m => {
    const r = m.getBoundingClientRect()
    return {
      cls: String(m.className).slice(0, 100),
      z: getComputedStyle(m).zIndex,
      尺寸: `${Math.round(r.width)}x${Math.round(r.height)}`,
      title: txt(m.querySelector('.dojoxDialogTitle, .dijitDialogTitle, [class*=DialogTitle]')),
      text: txt(m).slice(0, 900),
      tableHeaders: [...m.querySelectorAll('table thead th')].map(txt),
      tableFirstRows: [...m.querySelectorAll('tbody tr')].slice(0, 6).map(tr => txt(tr).slice(0, 90)),
      buttons: [...new Set([...m.querySelectorAll('button,[class*=btn]')].filter(e2 => e2.offsetParent !== null).map(txt).filter(t => t && t.length <= 12))],
      inputs: [...m.querySelectorAll('input')].filter(i => i.offsetParent !== null).map(i => ({ type: i.type, ph: i.placeholder || '', val: i.value })).slice(0, 15),
      checkboxes: [...m.querySelectorAll('input[type=checkbox]')].map(c => ({ checked: !!c.checked, label: txt(c.closest('label') || c.parentElement).slice(0, 40) })).slice(0, 30),
    }
  })
}

/* ================= 整页操作 ================= */

async function closeAllDialogs(page) {
  for (let i = 0; i < 4; i++) {
    const n = await page.evaluate(() => {
      let c = 0
      ;[...document.querySelectorAll('.dijitDialogCloseIcon, .dojoxDialogCloseIcon')]
        .filter(e => e.offsetParent !== null)
        .forEach(e => { try { e.click(); c++ } catch (e2) {} })
      return c
    }).catch(() => 0)
    await page.waitForTimeout(250)
    if (!n) break
  }
  await page.keyboard.press('Escape').catch(() => {})
  await page.waitForTimeout(250)
}

/** 重载桌面并打开「设置」一级菜单下某页；返回 {ok, tab} */
async function openPage(page, itemName) {
  await page.goto(BASE, { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {})
  await page.waitForTimeout(4000)
  await closeAllDialogs(page)
  const anchors = await page.evaluate(() => [...document.querySelectorAll('a.menu-0-item')].map(a => (a.innerText || '').trim()))
  if (!anchors.includes('设置')) return { ok: false, why: '桌面未加载出一级菜单' }
  let opened = await page.evaluate(popupVisibleFn)
  if (!opened) {
    const loc = page.locator('a.menu-0-item')
    const n = await loc.count()
    for (let i = 0; i < n && !opened; i++) {
      const a = loc.nth(i)
      if ((await a.innerText().catch(() => '')).trim() !== '设置') continue
      for (let attempt = 1; attempt <= 4 && !opened; attempt++) {
        await a.click({ timeout: 5000 }).catch(() => {})
        for (let w = 0; w < 10; w++) {
          await page.waitForTimeout(400)
          opened = await page.evaluate(popupVisibleFn)
          if (opened) break
        }
        if (!opened) {
          await page.evaluate(() => {
            const a2 = [...document.querySelectorAll('a.menu-0-item')].find(x => (x.innerText || '').trim() === '设置')
            if (a2) ['mouseenter', 'mouseover', 'mousemove'].forEach(ev => a2.dispatchEvent(new MouseEvent(ev, { bubbles: true })))
          }).catch(() => {})
        }
      }
    }
  }
  if (!opened) return { ok: false, why: '打开设置菜单失败' }
  const clicked = await page.evaluate((nm) => {
    const root = [...document.querySelectorAll('.popupmenu__repeat')].find(d => d.getBoundingClientRect().width > 0)
    if (!root) return 'no popup'
    const item = [...root.querySelectorAll('.popupmenu__list-item')].find(e => (e.innerText || '').trim() === nm)
    if (!item) return 'no item'
    item.click()
    return 'clicked'
  }, itemName)
  if (clicked !== 'clicked') return { ok: false, why: clicked }
  await page.waitForTimeout(4000)
  const tab = await page.evaluate(() => [...document.querySelectorAll('.dijitTabInner')]
    .filter(e => { const r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0 && r.y < 60 && (e.className || '').includes('Checked') })
    .map(e => e.innerText.trim())[0] || '')
  return { ok: tab === itemName, tab, why: tab === itemName ? '' : `激活标签「${tab}」≠ 目标「${itemName}」` }
}

/** 等左列纵向标签渲染出来（重载页面后标签是异步挂的，不等就会点空） */
async function waitLeftTabs(page, minCount = 8, timeoutMs = 30000) {
  const t0 = Date.now()
  let n = 0
  while (Date.now() - t0 < timeoutMs) {
    n = await page.evaluate(() => [...document.querySelectorAll('ul.tablist > li')].filter(li => li.offsetParent !== null).length).catch(() => 0)
    if (n >= minCount) return n
    await page.waitForTimeout(500)
  }
  return n
}

/** 点击左列纵向标签（系统参数 8 个 / 打印设置 7 个），并回读激活项校验 */
async function clickLeftTab(page, tabName) {
  await waitLeftTabs(page, 1, 15000)
  let r = { ok: false, why: 'no li', avail: [] }
  for (let attempt = 0; attempt < 3; attempt++) {
    r = await page.evaluate((nm) => {
      const vis = e => e.offsetParent !== null && e.getBoundingClientRect().width > 0
      const lis = [...document.querySelectorAll('ul.tablist > li')].filter(vis)
      const txt = e => ((e.querySelector('.tab-text') || {}).innerText || '').trim()
      const li = lis.find(x => txt(x) === nm)
      if (!li) return { ok: false, why: 'no li', avail: lis.map(txt) }
      li.click()
      return { ok: true, href: li.getAttribute('data-href') || '' }
    }, tabName).catch(e => ({ ok: false, why: e.message }))
    if (r.ok) break
    log('  左标签未就绪，重试…', tabName, JSON.stringify(r.avail || []).slice(0, 120))
    await page.waitForTimeout(2000)
  }
  await page.waitForTimeout(2200)
  const selected = await page.evaluate(() => {
    const vis = e => e.offsetParent !== null && e.getBoundingClientRect().width > 0
    const li = [...document.querySelectorAll('ul.tablist > li')].filter(vis).find(x => (x.className || '').includes('selected'))
    return li ? (((li.querySelector('.tab-text') || {}).innerText) || '').trim() : ''
  })
  return { ...r, selected, verified: selected === tabName }
}

/** 清掉上一轮打的探针标记（避免上一标签/上一个弹窗的残留把计数污染） */
async function clearMarks(page) {
  await page.evaluate(() => {
    ;[...document.querySelectorAll('[data-probe-help],[data-probe-sel],[data-probe-arrow],[data-probe-picker]')]
      .forEach(e => {
        e.removeAttribute('data-probe-help'); e.removeAttribute('data-probe-sel')
        e.removeAttribute('data-probe-arrow'); e.removeAttribute('data-probe-picker')
      })
  }).catch(() => {})
}

/** live 点开全部帮助气泡，读 dojo tooltip 文本 */
async function captureHelpBubbles(page, hostName, keyPrefix) {
  const list = await page.evaluate(() => {
    const vis = e => {
      if (!e) return false
      const r = e.getBoundingClientRect()
      if (r.width < 6 || r.height < 6 || e.offsetParent === null) return false
      let n = e
      while (n && n !== document.body) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
      return true
    }
    // 全 document 找（标记已在每轮开始前清空），这样弹窗里的气泡也能覆盖
    const scope = document.body
    const out = []
    const txt2 = e => (e ? (e.innerText || '').replace(/\s+/g, ' ').trim() : '')
    ;[...scope.querySelectorAll('[data-probe-help]')].forEach(ic => {
      let owner = '(未归属)'
      const tb = ic.closest('.sys-list__titlebar')
      if (tb) {
        owner = (txt2(tb.querySelector('.sys-list__title > div')) || txt2(tb.querySelector('.sys-list__title')) || '(无标题)').slice(0, 40)
      } else {
        const vis2 = e => {
          if (!e) return false
          const r = e.getBoundingClientRect()
          if (r.width < 2 || r.height < 2 || e.offsetParent === null) return false
          let n = e
          while (n && n !== document.body) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
          return true
        }
        let node = ic.parentElement
        while (node && node !== document.body) {
          const labs = [...node.querySelectorAll('label')].filter(l => {
            if (!vis2(l)) return false
            const t = txt2(l)
            if (!t) return false
            return !!(l.compareDocumentPosition(ic) & Node.DOCUMENT_POSITION_FOLLOWING)
          })
          if (labs.length) { owner = txt2(labs[labs.length - 1]).slice(0, 40); break }
          node = node.parentElement
        }
      }
      out.push({ idx: ic.getAttribute('data-probe-help'), owner, visible: vis(ic) })
    })
    return out
  }).catch(() => [])
  const results = []
  for (const h of list) {
    if (!h.visible) { results.push({ idx: h.idx, owner: h.owner, 取到: false, 原因: '图标不可见(配置项隐藏)' }); continue }
    const sel = `[data-probe-help="${h.idx}"]`
    const loc = page.locator(sel).first()
    let tip = { text: '', layers: 0 }
    let how = '未弹出'
    // 策略1：真实 hover（dojo Tooltip 监听 mouseover，是最可靠的触发方式）
    try { await loc.scrollIntoViewIfNeeded({ timeout: 2000 }) } catch (e) {}
    try { await loc.hover({ timeout: 2500 }) } catch (e) {}
    await page.waitForTimeout(500)
    tip = await page.evaluate(READ_TOOLTIP).catch(() => ({ text: '', layers: 0 }))
    if (tip.text) how = 'hover'
    // 策略2：真实 click
    if (!tip.text) {
      try { await loc.click({ timeout: 2500 }) } catch (e) {}
      await page.waitForTimeout(500)
      tip = await page.evaluate(READ_TOOLTIP).catch(() => ({ text: '', layers: 0 }))
      if (tip.text) how = 'click'
    }
    // 策略3：JS 派发完整鼠标事件序列
    if (!tip.text) {
      await page.evaluate((i) => {
        const el = document.querySelector(`[data-probe-help="${i}"]`)
        if (!el) return
        const r = el.getBoundingClientRect()
        const opt = { bubbles: true, clientX: r.x + 2, clientY: r.y + 2 }
        ;['mouseenter', 'mouseover', 'mousemove', 'mousedown', 'mouseup', 'click'].forEach(ev => el.dispatchEvent(new MouseEvent(ev, opt)))
      }, h.idx).catch(() => {})
      await page.waitForTimeout(600)
      tip = await page.evaluate(READ_TOOLTIP).catch(() => ({ text: '', layers: 0 }))
      if (tip.text) how = 'JS事件序列'
    }
    // 收尾：把鼠标挪开，让 dojo Tooltip 自己按 closeDelay 收回去。
    // ⚠️ 两个坑：
    //   ① 这里**不能按 Escape**：在「审核设置」这类 dojo 弹窗里按 Esc 会把弹窗本身关掉，
    //      后续（读选择器/下一行）就再也找不到弹窗内元素了；
    //   ② 也不能硬把气泡容器 `display:none`：那会让 dojo Tooltip 的状态机以为它还开着，
    //      之后所有气泡都再也弹不出来（实测「只有本标签第一个气泡能弹」就是此因）。
    try { await page.mouse.move(900, 12) } catch (e) {}
    await page.waitForTimeout(500)
    results.push({ idx: h.idx, owner: h.owner, 取到: !!tip.text, 气泡正文: tip.text || '', 层数: tip.layers, 触发方式: how })
  }
  // 汇总纯文本清单（去重保序）
  const bubbles = {}
  for (const r of results) if (r.取到) bubbles[r.owner] = r.气泡正文
  saveJson(`${hostName}-帮助气泡${keyPrefix ? '-' + keyPrefix : ''}`, { 页面: hostName, 共: results.length, 取到: results.filter(r => r.取到).length, results })
  return { results, bubbles, ok: results.filter(r => r.取到).length, total: results.length }
}

/** live 展开全部 dijit 下拉，读全部 option（不点选任何项，读完 Esc 关闭） */
async function captureDropdowns(page, hostName, keyPrefix) {
  const list = await page.evaluate(() => {
    const out = []
    const ownerOf = (ic) => {
      const tb = ic.closest('.sys-list__titlebar')
      if (tb) {
        const t = (tb.querySelector('.sys-list__title > div') || tb).innerText || ''
        return (t || '').replace(/\s+/g, ' ').trim().slice(0, 40) || '(无标题)'
      }
      let node = ic.parentElement
      while (node && node !== document.body) {
        const labs = [...node.querySelectorAll('label')].filter(l => {
          const t = (l.innerText || '').replace(/\s+/g, ' ').trim()
          if (!t) return false
          return !!(l.compareDocumentPosition(ic) & Node.DOCUMENT_POSITION_FOLLOWING)
        })
        if (labs.length) return (labs[labs.length - 1].innerText || '').replace(/\s+/g, ' ').trim().slice(0, 40)
        node = node.parentElement
      }
      const t = (ic.closest('[data-dojo-type="widget/FormField"]') || ic).innerText || ''
      return (t || '(未归属)').replace(/\s+/g, ' ').trim().slice(0, 40)
    }
    document.querySelectorAll('[data-probe-sel]').forEach(s => {
      const arrow = s.querySelector('.dijitButtonNode') || s
      arrow.setAttribute('data-probe-arrow', s.getAttribute('data-probe-sel'))
      out.push({
        idx: s.getAttribute('data-probe-sel'),
        id: s.id || '',
        owner: ownerOf(s),
        visible: s.offsetParent !== null,
        当前值: ((s.querySelector('.dijitSelectLabel') || {}).innerText || '').trim(),
        rawValue: ((s.querySelector('input[type=hidden]') || {}).value) || '',
        cls: String(s.className).slice(0, 90),
      })
    })
    return out
  }).catch(() => [])
  const clearTooltip = async () => {
    await page.keyboard.press('Escape').catch(() => {})
    await page.mouse.move(3, 3).catch(() => {})
    await page.evaluate(() => {
      ;[...document.querySelectorAll('.dijitTooltipContainer')].forEach(e => { e.style.display = 'none' })
    }).catch(() => {})
    await page.waitForTimeout(150)
  }
  const results = []
  for (const d of list) {
    if (!d.visible) { results.push({ ...d, 展开: false, 原因: '控件不可见' }); continue }
    await clearTooltip()
    // 真实鼠标点击箭头节点（dijit Select 的开关挂在 arrow 的 onclick 上，JS .click() 无效）
    let opened = false
    try {
      await page.locator(`[data-probe-arrow="${d.idx}"]`).first().scrollIntoViewIfNeeded({ timeout: 2000 })
      await page.locator(`[data-probe-arrow="${d.idx}"]`).first().click({ timeout: 3000 })
      opened = true
    } catch (e) {
      await page.evaluate((i) => {
        const el = document.querySelector(`[data-probe-arrow="${i}"]`)
        if (el) { el.scrollIntoView({ block: 'center' }); el.click() }
      }, d.idx).catch(() => {})
    }
    await page.waitForTimeout(700)
    let pop = await page.evaluate(READ_POPUP).catch(() => ({ found: false }))
    if (!pop.found || !pop.items || !pop.items.length) {
      // 兜底 1：等更久（dijit 浮层渲染慢）
      await page.waitForTimeout(900)
      pop = await page.evaluate(READ_POPUP).catch(() => ({ found: false }))
    }
    if (!pop.found || !pop.items || !pop.items.length) {
      // 兜底 2：RealClick 落在外层容器上再试一次
      try {
        await page.locator(`[data-probe-sel="${d.idx}"]`).first().click({ timeout: 2500 })
        await page.waitForTimeout(700)
        pop = await page.evaluate(READ_POPUP).catch(() => ({ found: false }))
      } catch (e) {}
    }
    // 交叉校验：直接读 widget 的选项集（来源标注不同）
    const widgetOpts = await page.evaluate((id) => {
      try {
        const w = (window.dijit && (dijit.byId(id) || (dijit.registry && dijit.registry.byId(id))))
        if (!w) return null
        let arr = null
        if (typeof w.getOptions === 'function') arr = w.getOptions()
        else if (Array.isArray(w.options)) arr = w.options
        else if (w.options && typeof w.options.getOptions === 'function') arr = w.options.getOptions()
        if (!Array.isArray(arr)) return null
        return arr.map(o => ({ label: o && o.label, value: o && o.value }))
      } catch (e) { return null }
    }, d.id).catch(() => null)
    await page.keyboard.press('Escape').catch(() => {})
    await page.waitForTimeout(200)
    results.push({
      ...d,
      展开: !!pop.found && !!(pop.items && pop.items.length),
      选项数: (pop.items || []).length,
      选项: (pop.items || []).map(o => o.label),
      选中项: (pop.items || []).filter(o => o.selected).map(o => o.label),
      浮层原始文本: pop.raw || '',
      widget选项集: widgetOpts ? widgetOpts.map(o => o.label) : null,
      选项来源: (pop.items || []).length ? '展开下拉浮层' : (widgetOpts ? 'widget.options(兜底)' : '未取到'),
    })
  }
  saveJson(`${hostName}-下拉选项${keyPrefix ? '-' + keyPrefix : ''}`, { 页面: hostName, 共: results.length, 展开成功: results.filter(r => r.展开).length, results })
  return results
}

/** 抓当前页里的内嵌 iframe（打印模板类目=iframed 设计器，内容全在 iframe 里，父文档查不到）
 *  ⚠️ 不用 DOM 序号对应 frame（page.frames() 是创建序，与 DOM 序不一致），改为按 URL 匹配 +
 *     取内容最多的那个 frame，避免抓到空 frame。 */
async function extractFrames(page) {
  const info = await page.evaluate(() => [...document.querySelectorAll('iframe')].map((f, i) => {
    const r = f.getBoundingClientRect()
    return {
      i, src: f.src || '', w: Math.round(r.width), h: Math.round(r.height),
      visible: f.offsetParent !== null && r.width > 100 && r.height > 80,
    }
  })).catch(() => [])
  const kids = page.frames().filter(f => f !== page.mainFrame())
  const shapeOf = async (fr) => {
    for (let a = 0; a < 3; a++) {
      const r = await fr.evaluate(EXTRACT_SHAPE).catch(e => ({ err: e.message }))
      if (r && !r.err && (r.text || (r.controls || []).length || (r.列表项 || []).length)) return r
      await page.waitForTimeout(1200)
    }
    return await fr.evaluate(EXTRACT_SHAPE).catch(e => ({ err: e.message }))
  }
  const res = []
  for (const it of info) {
    if (!it.src || it.src === 'about:blank') continue
    const matched = kids.filter(f => f.url() === it.src || (it.src && f.url().split('?')[0] === it.src.split('?')[0]))
    const rec = { idx: it.i, src: it.src, 尺寸: `${it.w}x${it.h}`, 可见: it.visible, 命中frame数: matched.length, 内容: null }
    if (!it.visible) { res.push(rec); continue }
    let best = null
    for (const fr of matched) {
      const r = await shapeOf(fr)
      const score = (r && !r.err) ? ((r.text || '').length + 40 * ((r.controls || []).length + (r.列表项 || []).length)) : -1
      if (!best || score > best.score) best = { score, r }
    }
    rec.内容 = best ? best.r : { err: '未命中可见 iframe 的 frame（按 URL 未匹配到）' }
    res.push(rec)
  }
  return res
}

/* ================= 三页抓取 ================= */

const TABS_SYS = ['行业设置', '流程启用', '单据设置', '库存设置', '财务设置', '数据权限', '消息提醒', '其他']
const TABS_PRINT = ['打印设置', '箱号模板', '物流模板', '条码模板', '套餐条码', '批次条码', '货位码']
const BILL_TYPES = ['销售出库单', '销售订单', '销售退货申请单', '商城订单取消', '采购入库单', '采购订单', '费用单',
  '收款单', '付款单', '预收款单', '预付款单', '会计凭证', '预订货单', '调拨单', '费用合同', '调拨申请单']

async function probeSystemParams(page) {
  const r = await openPage(page, '系统参数')
  if (!r.ok) return saveJson('系统参数-抓取状态', { ok: false, ...r })
  const prevSys = loadJson('系统参数-汇总') || {}
  const out = { 页面: '系统参数', 激活标签: r.tab, 抓取时间: new Date().toISOString(), 标签: { ...(prevSys.标签 || {}) } }
  for (const t of tabFilter(TABS_SYS)) {
    log('系统参数 ▶ 标签', t)
    try {
    // dojo Tooltip 是「一个页面实例只认第一个标签」的状态机：切过一次标签后气泡就再也不弹。
    // 需要每个标签都拿到 live 气泡时，用 QL361_RELOAD_PER_TAB=1 逐个标签重载页面。
    if (RELOAD_PER_TAB) { const rr = await openPage(page, '系统参数'); if (!rr.ok) log('  重载失败', rr.why) }
    const sw = await clickLeftTab(page, t)
    await clearMarks(page)
    const shot = await shotPage(page, `系统参数-${t}`)
    const data = await page.evaluate(EXTRACT, {}).catch(e => ({ err: e.message }))
    const generic = await page.evaluate(EXTRACT_SHAPE).catch(e => ({ err: e.message }))
    const help = await captureHelpBubbles(page, '系统参数', t)
    const ddl = await captureDropdowns(page, '系统参数', t)
    out.标签[t] = {
      ...sw, 截图: shot, 内容: data, 通用形态: generic,
      帮助气泡_取到: `${help.ok}/${help.total}`,
      帮助气泡明细: help.results,
      帮助属性汇总: help.bubbles,
      下拉_展开成功: `${ddl.filter(d => d.展开).length}/${ddl.length}`,
      下拉明细: ddl,
    }
    saveJson(`系统参数-${t}`, out.标签[t])
    log(`  ${t}: href=${sw.href || '-'} verified=${sw.verified} 卡片=${(data.cards || []).length} 散项=${(data.looseFields || []).length} 帮助=${help.ok}/${help.total} 下拉=${ddl.filter(d => d.展开).length}/${ddl.length}`)
    } catch (e) {
      log(`  ✗ ${t} 异常`, e.message)
      out.标签[t] = { err: e.message, verified: false }
      saveJson(`系统参数-${t}`, out.标签[t])
    }
  }
  return saveJson('系统参数-汇总', out)
}

async function probeAudit(page) {
  const r = await openPage(page, '审核设置')
  if (!r.ok) return saveJson('审核设置-抓取状态', { ok: false, ...r })
  const rows = await page.evaluate(READ_AUDIT_ROWS).catch(() => [])
  log('审核设置 表格行数:', rows.length)
  const out = { 页面: '审核设置', 激活标签: r.tab, 抓取时间: new Date().toISOString(), 页顶提示: '', 行: [] }
  out.页顶提示 = await page.evaluate(() => {
    const t = (document.body.innerText || '')
    const m = t.match(/当同一个审核条件[^\n]*/)
    return m ? m[0].trim() : ''
  }).catch(() => '')
  await shotPage(page, '审核设置-列表')
  out.表格行 = rows
  saveJson('审核设置-列表', { 页面: '审核设置', 提示: out.页顶提示, 行: rows })

  // 支持只重跑部分单据，并把结果合并回汇总（保持表格原顺序）
  const prevAud = loadJson('审核设置-汇总') || {}
  const prevRows = new Map((prevAud.行 || []).map(x => [x.单据, x]))
  const newRows = new Map()
  const runRows = ONLY_TABS.length ? rows.filter(r => ONLY_TABS.includes(r.单据)) : rows

  for (let i = 0; i < runRows.length; i++) {
    const bill = runRows[i].单据
    const rowIdx = rows.findIndex(r => r.单据 === bill)
    log('审核设置 ▶', rowIdx + 1, bill)
    try {
    await closeAllDialogs(page)
    await clearMarks(page)
    const clicked = await page.evaluate(CLICK_AUDIT_SET, bill)
    if (!clicked.ok) log('  ✗ 点不到该行设置按钮', JSON.stringify(clicked))
    await page.waitForTimeout(3500)
    const dlg = await page.evaluate(READ_DIALOG).catch(e => ({ err: e.message }))
    const shot = await shotPage(page, `审核设置-${bill}`)
    // live 读弹窗内全部 ? 气泡
    const help = await captureHelpBubbles(page, '审核设置', bill)
    // live 点开第一个审核人选择器，看选择方式（只开不选）
    let picker = null
    const hasPicker = await page.evaluate(() => !!document.querySelector('[data-probe-picker]')).catch(() => false)
    if (hasPicker) {
      // ⚠️ 选择器的搜索按钮绑在 onmousedown 上（data-dojo-attach-event="onmousedown:_onClick"），
      //    JS .click() 不触发，必须用真实鼠标事件。
      const pk = page.locator('[data-probe-picker]').first()
      const pkVisible = page.locator('[data-probe-picker]:visible').first()
      try { await pk.scrollIntoViewIfNeeded({ timeout: 2000 }) } catch (e) {}
      try {
        await pkVisible.click({ timeout: 3000 })
      } catch (e) {
        try { await pk.click({ timeout: 2500 }) } catch (e2) {
          try { await pk.dispatchEvent('mousedown', { timeout: 2000 }) } catch (e3) {}
        }
      }
      await page.waitForTimeout(3200)
      const dlgCount = await page.evaluate(READ_ALL_DIALOGS).catch(e => [{ err: e.message }])
      await shotPage(page, `审核设置-${bill}-审核人选择器`)
      picker = { 弹窗数: dlgCount.length, 弹窗: dlgCount }
      await closeAllDialogs(page)
      await page.waitForTimeout(500)
    }
    out.行.push({
      序号: i + 1, 单据: bill, 审核设置摘要_表格: rows[i].摘要,
      截图: shot, 弹窗: dlg, 帮助气泡_取到: `${help.ok}/${help.total}`, 帮助气泡明细: help.results,
      审核人选择器: picker || { 取到: false },
    })
    newRows.set(bill, out.行[out.行.length - 1])
    saveJson(`审核设置-${bill}`, out.行[out.行.length - 1])
    const pkTitle = picker ? (picker.弹窗 || []).map(x => x.title).filter(Boolean).join(' + ') : ''
    log(`  ${bill}: 字段=${(dlg.fields || []).length} 条件=${(dlg.条件清单_按序 || []).length} 帮助=${help.ok}/${help.total} 选择器弹窗=${picker ? picker.弹窗数 : 0}个[${pkTitle}]`)
    await closeAllDialogs(page)
    await page.waitForTimeout(600)
    } catch (e) {
      log(`  ✗ ${bill} 异常`, e.message)
      const rec = { 序号: rowIdx + 1, 单据: bill, 审核设置摘要_表格: rows[rowIdx] ? rows[rowIdx].摘要 : '', err: e.message }
      newRows.set(bill, rec)
      saveJson(`审核设置-${bill}`, rec)
      await closeAllDialogs(page).catch(() => {})
      await page.waitForTimeout(600)
    }
  }
  // 合并：表格原顺序；本轮没跑的单据沿用上次结果
  out.行 = rows.map(r => newRows.get(r.单据) || prevRows.get(r.单据)).filter(Boolean)
  return saveJson('审核设置-汇总', out)
}

async function probePrint(page) {
  const r = await openPage(page, '打印设置')
  if (!r.ok) return saveJson('打印设置-抓取状态', { ok: false, ...r })
  const prevPrint = loadJson('打印设置-汇总') || {}
  const out = { 页面: '打印设置', 激活标签: r.tab, 抓取时间: new Date().toISOString(), 标签: { ...(prevPrint.标签 || {}) } }
  for (const t of tabFilter(TABS_PRINT)) {
    log('打印设置 ▶ 标签', t)
    try {
    const sw = await clickLeftTab(page, t)
    await clearMarks(page)
    const shot = await shotPage(page, `打印设置-${t}`)
    const data = await page.evaluate(EXTRACT, {}).catch(e => ({ err: e.message }))
    const generic = await page.evaluate(EXTRACT_SHAPE).catch(e => ({ err: e.message }))
    const help = await captureHelpBubbles(page, '打印设置', t)
    const ddl = await captureDropdowns(page, '打印设置', t)
    // 模板类目形态：表格列/按钮/文本
    const shape = await page.evaluate(() => {
      const vis = e => e && e.offsetParent !== null && e.getBoundingClientRect().width > 0
      const txt = e => (e ? (e.innerText || '').replace(/\s+/g, ' ').trim() : '')
      const cs = [...document.querySelectorAll('div.tabcontent')].filter(e => vis(e) && e.getBoundingClientRect().width > 300)
      const sc = cs[cs.length - 1] || document.body
      const tables = [...sc.querySelectorAll('table')].filter(vis).slice(0, 6).map(t => ({
        cls: String(t.className).slice(0, 60),
        headers: [...t.querySelectorAll('thead th')].map(txt),
        rowCount: t.rows.length,
        firstRow: t.rows[0] ? [...t.rows[0].cells].map(c => txt(c)).slice(0, 12) : [],
      }))
      return {
        panelTitle: txt(sc.querySelector('.tab-list-title')),
        tables,
        buttons: [...new Set([...sc.querySelectorAll('button,[class*=btn]')].filter(vis).map(txt).filter(x => x && x.length <= 14))],
        gridHeaders: [...new Set([...sc.querySelectorAll('th,[class*=columnheader]')].filter(vis).map(txt).filter(x => x && x.length < 24))],
        text: txt(sc).slice(0, 2500),
      }
    }).catch(e => ({ err: e.message }))
    out.标签[t] = {
      ...sw, 截图: shot, 内容: data, 形态: shape, 通用形态: generic,
      内嵌iframe: await extractFrames(page),
      帮助气泡_取到: `${help.ok}/${help.total}`, 帮助气泡明细: help.results, 帮助属性汇总: help.bubbles,
      下拉_展开成功: `${ddl.filter(d => d.展开).length}/${ddl.length}`, 下拉明细: ddl,
    }
    saveJson(`打印设置-${t}`, out.标签[t])
    log(`  ${t}: verified=${sw.verified} 字段=${(data.cards || []).length}卡/${(data.looseFields || []).length}散 帮助=${help.ok}/${help.total} 下拉=${ddl.filter(d => d.展开).length}/${ddl.length} 表=${shape.tables ? shape.tables.length : 0}`)
    } catch (e) {
      log(`  ✗ ${t} 异常`, e.message)
      out.标签[t] = { err: e.message, verified: false }
      saveJson(`打印设置-${t}`, out.标签[t])
    }
  }
  return saveJson('打印设置-汇总', out)
}

/* ================= 汇总报告 ================= */

function genSummary() {
  const rd = n => { try { return JSON.parse(fs.readFileSync(path.join(OUT, `${n}.json`), 'utf8')) } catch (e) { return null } }
  const L = []
  const P = s => L.push(s)
  const esc = s => String(s == null ? '' : s).replace(/\|/g, '\\|').replace(/\n/g, ' ')
  // 通用形态渲染（非卡片布局 / 卡片漏掉的配置项，都靠它兜底）
  const renderGeneric = (g, d, onlyLabels, bubbles) => {
    if (!g || g.err) { P(`> 通用形态提取失败：${esc(g ? g.err : '未采集')}`); P(''); return }
    const keep = (t) => !onlyLabels || onlyLabels.has(t)
    const bub = bubbles || {}
    const labs = (g.labels || []).filter(keep)
    const ctrls = (g.controls || []).filter(c => keep(c.标签))
    const lis = (g.列表项 || []).filter(keep)
    const btns = (g.按钮 || []).filter(keep)
    if (labs.length) P(`- label 清单（按 DOM 顺序，共 ${labs.length} 个）：${labs.map(x => '`' + esc(x) + '`').join(' ')}`)
    if (ctrls.length) {
      P('')
      P('| 控件 | 最近前置 label（自动配对，仅供参考） | 当前值 | 帮助文案（按 label 匹配 live 气泡） | placeholder |')
      P('| --- | --- | --- | --- | --- |')
      for (const x of ctrls.slice(0, 60)) P(`| ${esc(x.控件)} | ${esc(x.标签) || '-'} | ${esc(x.值) || '(空)'} | ${esc(bub[x.标签] || '未取到')} | ${esc(x.placeholder) || '-'} |`)
    }
    if (lis.length) { P(''); P(`- 列表项：${lis.slice(0, 40).map(x => '`' + esc(x) + '`').join(' ')}`) }
    if (btns.length) P(`- 按钮：${btns.map(esc).join(' / ')}`)
    P('')
    if (d && d.内容 && d.内容.panelText) P(`- 面板正文（逐字节选）：${esc(d.内容.panelText.slice(0, 1500))}`)
    P('')
  }
  // 同一字段名下可能有多个下拉（如「批次条码规则生成」由 3 个下拉拼成）→ 全部列出
  const rangeOf = (ddls, name) => {
    const hits = (ddls || []).filter(x => x.owner === name)
    if (!hits.length) return ''
    return hits.map((x, i) => {
      const pre = hits.length > 1 ? `[${i + 1}] ` : ''
      if (x.展开) return pre + (x.选项 || []).join(' / ')
      return pre + (x.原因 ? `未取到（${x.原因}）` : '未取到')
    }).join(' ； ')
  }
  P('# ql361 设置域深度取证结论（系统参数 / 审核设置 / 打印设置）')
  P('')
  P(`- 抓取时间：${new Date().toISOString().slice(0, 10)}`)
  P('- 对标系统：https://22stable.ql361.com/desktop.html （登录 www.ql361.com，账号杨生淮）')
  P('- 采集方式：Playwright 实机登录，逐个点开左标签/弹窗/下拉，只读不写（不点选任何下拉项、不改任何配置）')
  P('- 「帮助文案」两路来源：① 点击 `?` 气泡读出的 dojo tooltip 正文（live）；② 控件 `data-dojo-props` 的 `toolTipContent` 属性值（静态）。两路不一致时以 live 为准并在表中标注。')
  P('')

  // ---- 系统参数 ----（逐标签文件汇总，支持部分重跑后仍能出完整报告）
  const sysTabFiles = {}
  for (const t of TABS_SYS) { const d = rd(`系统参数-${t}`); if (d) sysTabFiles[t] = d }
  const sys = Object.keys(sysTabFiles).length ? { 标签: sysTabFiles } : null
  P('## 一、系统参数（设置 → 系统配置 → 系统参数）')
  P('')
  if (!sys) P('> 未取到：未生成汇总 JSON')
  else {
    P('左列 8 个纵向标签：`行业设置` `流程启用` `单据设置` `库存设置` `财务设置` `数据权限` `消息提醒` `其他`')
    P('')
    for (const t of TABS_SYS) {
      const d = sys.标签[t]
      P(`### 标签：${t}`)
      if (!d) { P('> 未取到'); P(''); continue }
      P(`- 切换校验：点击后激活标签 = 「${esc(d.selected)}」，${d.verified ? '与目标一致（可信）' : '**与目标不一致 → 本标签结果不可信**'}`)
      P(`- 截图：\`tool-results/ql361/设置-deep/shots/${esc(d.截图)}\`　帮助气泡 ${esc(d.帮助气泡_取到)}　下拉展开 ${esc(d.下拉_展开成功)}`)
      const c = d.内容 || {}
      if (c.err) { P(`> 主体提取失败：${esc(c.err)}`); P(''); continue }
      P('')
      const cards = c.cards || []
      if (!cards.length) {
        // 非卡片布局（如 财务设置 / 消息提醒）：改用通用形态（label 清单 + 控件清单 + 面板正文）
        P('**本标签不是卡片布局，改用「通用形态」取证（label 清单 + 控件清单 + 面板正文）：**')
        P('')
        renderGeneric(d.通用形态, d, null, d.帮助属性汇总)
      }
      for (const card of cards) {
        P(`#### 卡片：${esc(card.卡片标题) || '(无标题)'}`)
        if (card.卡片开关) {
          P(`- 卡片开关：${esc(card.卡片开关.文案)}（${card.卡片开关.勾选 ? '已勾选' : '未勾选'}，name=${esc(card.卡片开关.name)}）`)
        }
        P(`- 卡片体可见：${card.卡片体可见 ? '是' : '否（因开关关闭/条件不满足而隐藏）'}；卡片体数量 ${card.卡片体数量}`)
        const chelp = (d.帮助属性汇总 || {})[card.卡片标题]
        if (chelp) P(`- 卡片标题帮助气泡：${esc(chelp)}`)
        if ((card.子分组标题 || []).length) P(`- 子分组标题：${card.子分组标题.map(esc).join(' / ')}`)
        P('')
        if ((card.配置项 || []).length) {
          P('| 配置项名称 | 控件类型 | 当前值 | 取值域/选项集 | 帮助文案 | 温馨提示 |')
          P('| --- | --- | --- | --- | --- | --- |')
          for (const f of card.配置项) {
            let range = rangeOf(d.下拉明细, f.名称)
            if (!range && f.细节 && f.细节.选项) range = f.细节.选项.map(o => `${esc(o.label)}${o.checked ? '(选中)' : ''}`).join(' / ')
            else if (!range && f.细节 && f.细节.rawValue !== undefined) range = `rawValue=${esc(f.细节.rawValue)}`
            const bubble = (d.帮助属性汇总 || {})[f.名称]
            const helpTxt = bubble ? bubble : (f.帮助文案_属性 ? f.帮助文案_属性 + '（来自 data-dojo-props）' : '未取到')
            P(`| ${esc(f.名称)} | ${esc(f.控件)} | ${esc(f.当前值) || '(空)'} | ${esc(range) || '-'} | ${esc(helpTxt)} | ${esc(f.温馨提示) || '-'} |`)
          }
          P('')
        } else {
          P('（该卡片体当前不可见/无配置项）')
          P('')
        }
      }
      // 卡片结构之外的配置项兜底（有些配置项既不在 .sys-card 里、也不是 FormField 包裹 → 卡片表里会漏）
      if (cards.length) {
        const known = new Set()
        for (const card of cards) for (const f of card.配置项 || []) known.add(f.名称)
        const g2 = d.通用形态 || {}
        const extra = (g2.labels || []).filter(l => !known.has(l))
        if (extra.length) {
          P('**补充（通用形态兜底）：卡片结构之外的配置项**')
          P('')
          renderGeneric(g2, d, new Set(extra), d.帮助属性汇总)
        }
      }
      if ((c.looseFields || []).length) {
        P('**卡片之外的配置项：**')
        P('')
        P('| 配置项名称 | 控件类型 | 当前值 | 取值域/选项集 | 帮助文案 | 温馨提示 |')
        P('| --- | --- | --- | --- | --- | --- |')
        for (const f of c.looseFields) {
          const range = rangeOf(d.下拉明细, f.名称) || (f.细节 && f.细节.rawValue !== undefined ? `rawValue=${esc(f.细节.rawValue)}` : '')
          P(`| ${esc(f.名称)} | ${esc(f.控件)} | ${esc(f.当前值) || '(空)'} | ${esc(range) || '-'} | ${esc((d.帮助属性汇总 || {})[f.名称] || f.帮助文案_属性 || '未取到')} | ${esc(f.温馨提示) || '-'} |`)
        }
        P('')
      }
      // 未归属帮助气泡
      const orphans = (d.帮助气泡明细 || []).filter(h => h.取到 && !(d.帮助属性汇总 || {})[h.owner])
      if (orphans.length) {
        P('**其他帮助气泡（未匹配到配置项）：**')
        for (const o of orphans) P(`- ${esc(o.owner)}：${esc(o.气泡正文)}`)
        P('')
      }
      const failed = (d.帮助气泡明细 || []).filter(h => !h.取到)
      if (failed.length) {
        P(`**本标签未取到的帮助气泡（${failed.length} 个）：**`)
        for (const o of failed) P(`- ${esc(o.owner)}：${esc(o.原因 || '气泡未弹出/无文本')}`)
        P('')
      }
    }
  }

  // ---- 审核设置 ----（逐单据文件汇总）
  const audRowFiles = []
  for (const b of BILL_TYPES) { const d = rd(`审核设置-${b}`); if (d) audRowFiles.push(d) }
  const aud = audRowFiles.length ? { 行: audRowFiles } : null
  P('## 二、审核设置（设置 → 系统配置 → 审核设置）')
  P('')
  const audList = rd('审核设置-列表')
  if (audList) {
    P('### 2.1 表格「摘要」列实际文案（三列：单据 | 审核设置 | 摘要）')
    P('')
    P('| # | 单据 | 摘要（逐字） |')
    P('| --- | --- | --- |')
    ;(audList.行 || []).forEach((r, i) => P(`| ${i + 1} | ${esc(r.单据)} | ${esc(r.摘要) || '(空)'} |`))
    P('')
    if (audList.提示) P(`页顶提示（逐字）：${esc(audList.提示)}`)
    P('')
  } else P('> 未取到：表格列表')
  if (!aud) P('> 未取到：16 类单据弹窗')
  else {
    P('### 2.2 16 类单据「设置」弹窗结构')
    P('')
    for (const row of aud.行 || []) {
      const d = row.弹窗 || {}
      P(`#### ${row.序号}. ${esc(row.单据)}`)
      P(`- 弹窗标题：${esc(d.title) || '(未取到)'}　截图：\`shots/${esc(row.截图)}\``)
      P(`- 表格摘要列：${esc(row.审核设置摘要_表格) || '(空)'}`)
      if (d.err) { P(`- 弹窗未打开：${esc(d.err)}`); P(''); continue }
      P(`- 帮助气泡：${esc(row.帮助气泡_取到)}`)
      P(`- 弹窗按钮：${(d.buttons || []).map(esc).join(' / ') || '-'}`)
      P('')
      const conditions = (d.fields || []).filter(f => /ItemField/.test(f.控件))
      const others = (d.fields || []).filter(f => !/ItemField/.test(f.控件))
      P('| 字段/条件名称 | 控件类型 | 当前值 | 可见 | 帮助文案 |')
      P('| --- | --- | --- | --- | --- |')
      for (const f of [...others, ...conditions]) {
        const helpTxt = f.帮助文案_属性 ? f.帮助文案_属性 + '（data-dojo-props）' : ((row.帮助气泡明细 || []).find(h => h.owner === f.名称 && h.取到) || {}).气泡正文 || '未取到'
        P(`| ${esc(f.名称)} | ${esc(f.控件)} | ${esc(f.当前值) || '(空)'} | ${f.可见 ? '是' : '否'} | ${esc(helpTxt)} |`)
      }
      P('')
      P(`- 条件清单（按弹窗内顺序，共 ${conditions.length} 条）：${conditions.map(f => esc(f.名称)).join(' / ') || '未取到'}`)
      P(`- 「审核人」选择控件：${[...new Set((d.fields || []).map(f => f.控件))].map(esc).join(' / ')}`)
      P(`- 弹窗内出现的组合语义线索：${(d.组合方式线索 || []).map(x => '「' + esc(x) + '」').join(' ') || '(弹窗内未出现 且/或/任一 等字样)'}`)
      P(`- 弹窗内单选项：${(d.单选项 || []).map(o => esc(o.label) + (o.checked ? '(选中)' : '')).join(' / ') || '(无 radio)'}`)
      const pk = row.审核人选择器 || {}
      if (pk.弹窗数 !== undefined) {
        const picker = (pk.弹窗 || []).find(x => x.title && x.title !== '审核设置') || (pk.弹窗 || [])[1]
        if (picker) {
          const rec = (picker.text || '').match(/共\s*[\d,]+\s*条记录/)
          const listRows = (picker.tableFirstRows || []).filter(t => /\d/.test(t)).slice(0, 12)
          P(`- 点开「审核人」选择器后打开的弹层：**「${esc(picker.title)}」**（尺寸 ${esc(picker.尺寸)}，z-index ${esc(picker.z)}）`)
          P(`  - 该弹层内表头：${(picker.tableHeaders || []).map(esc).join(' / ') || '(未取到表头)'}`)
          P(`  - 该弹层内前若干行：${listRows.map(x => '「' + esc(x) + '」').join(' ') || '(未取到)'}`)
          P(`  - 该弹层内按钮：${(picker.buttons || []).map(esc).join(' / ') || '(未取到)'}`)
          P(`  - 该弹层内勾选框：${(picker.checkboxes || []).length} 个（含表头全选/全选列），勾选态：${(picker.checkboxes || []).map(c => c.checked ? '√' : '×').join('')}`)
          P(`  - 查询输入框：${(picker.inputs || []).filter(i => i.ph).map(i => '「' + esc(i.ph) + '」').join(' ') || '(无带 placeholder 的输入框)'}`)
          P(`  - 记录数：${rec ? esc(rec[0]) : '(未取到)'}`)
        } else {
          P(`- 点开「审核人」选择器后的弹层：未取到（只看到 ${pk.弹窗数} 个弹窗，无第二层）`)
        }
      } else P(`- 点开「审核人」选择器后的弹层：未取到`)
      P('')
    }
    // 全局结论
    const allConds = new Set()
    for (const row of aud.行 || []) for (const c of (row.弹窗 || {}).条件清单_按序 || []) allConds.add(c)
    const pickerTitles = new Set()
    for (const row of aud.行 || []) for (const m of ((row.审核人选择器 || {}).弹窗 || [])) if (m.title && m.title !== '审核设置') pickerTitles.add(m.title)
    P('### 2.3 适用条件全集 / 审批人选择方式 / 组合方式（跨 16 类单据汇总）')
    P('')
    P(`- 全站出现过的审核条件名称（16 类单据弹窗并集，共 ${allConds.size} 个）：${[...allConds].map(x => '`' + esc(x) + '`').join(' ')}`)
    P(`- 点开「审核人」选择器打开的第二层弹窗标题（去重）：${[...pickerTitles].map(x => '「' + esc(x) + '」').join(' ') || '未取到'}`)
    const withPicker = (aud.行 || []).filter(r => ((r.审核人选择器 || {}).弹窗 || []).some(m => m.title && m.title !== '审核设置')).length
    P(`- 成功打开「操作员选择」层级的单据数：${withPicker}/${(aud.行 || []).length}`)
    P('')
    P('> 说明：上表「条件清单」是该单据弹窗内实际渲染出来的条件项（含当前不可见项，已在字段表标出「可见」）；')
    P('> 「审批人」字段的画布控件是 ItemField（搜索框 + 放大镜按钮），点击放大镜打开第二层「操作员选择」弹窗；')
    P('> 组合方式：弹窗内**没有**出现 且/或 单选控件，唯一的口径文案是页面顶部提示（见 2.1）与弹窗首行，原文为：')
    P('> 「当同一个审核条件设置多个审核人时，其中任何一个审核人审核之后不再需要其他职员审核」。')
    P('> 即：**同一条件的多个审核人之间是「任一通过即结束」**；上述为原文证据，未做额外推断。')
    P('')
  }

  // ---- 打印设置 ----（逐标签文件汇总）
  const prtTabFiles = {}
  for (const t of TABS_PRINT) { const d = rd(`打印设置-${t}`); if (d) prtTabFiles[t] = d }
  const prt = Object.keys(prtTabFiles).length ? { 标签: prtTabFiles } : null
  P('## 三、打印设置（设置 → 系统配置 → 打印设置）')
  P('')
  if (!prt) P('> 未取到：汇总 JSON')
  else {
    const main = prt.标签['打印设置']
    P('### 3.1 「打印设置」主标签')
    P('')
    if (main) {
      P(`- 切换校验：激活标签「${esc(main.selected)}」${main.verified ? '（可信）' : '**不一致 → 不可信**'}　截图：\`shots/${esc(main.截图)}\``)
      P('')
      const allFields = [...((main.内容 || {}).looseFields || [])]
      for (const c of (main.内容 || {}).cards || []) for (const f of c.配置项 || []) allFields.push(f)
      P('| 配置项名称 | 控件类型 | 当前值 | 可见 | 取值域/选项集 | 帮助文案 | 温馨提示 |')
      P('| --- | --- | --- | --- | --- | --- | --- |')
      for (const f of allFields) {
        const range = rangeOf(main.下拉明细, f.名称)
        const helpTxt = (main.帮助属性汇总 || {})[f.名称] || f.帮助文案_属性 || '未取到'
        P(`| ${esc(f.名称)} | ${esc(f.控件)} | ${esc(f.当前值) || '(空)'} | ${f.可见 ? '是' : '否'} | ${esc(range) || '-'} | ${esc(helpTxt)} | ${esc(f.温馨提示) || '-'} |`)
      }
      P('')
      P('**「打印内容」下拉完整选项集（重点）：**')
      P('')
      for (const d of main.下拉明细 || []) {
        P(`- 控件 ${esc(d.id)}（归属：${esc(d.owner)}）当前值「${esc(d.当前值)}」rawValue=${esc(d.rawValue)}`)
        P(`  - 展开成功：${d.展开 ? '是' : '否'}；选项数 ${d.选项数 || 0}；来源：${esc(d.选项来源)}`)
        P(`  - 完整选项集：${(d.选项 || []).map(o => '`' + esc(o) + '`').join(' ') || '未取到'}`)
        if (d.选中项 && d.选中项.length) P(`  - 浮层中标记选中：${d.选中项.map(o => '`' + esc(o) + '`').join(' ')}`)
        if (d.widget选项集) P(`  - 交叉校验（widget.options）：${d.widget选项集.map(o => '`' + esc(o) + '`').join(' ')}`)
      }
      P('')
    } else P('> 未取到')
    P('### 3.2 左列 6 个模板类目点开后的形态与字段')
    P('')
    for (const t of TABS_PRINT.slice(1)) {
      const d = prt.标签[t]
      P(`#### ${t}`)
      if (!d) { P('> 未取到'); P(''); continue }
      const s = d.形态 || {}
      const frames = (d.内嵌iframe || []).filter(f => f.可见)
      const fr = frames[0] || null
      const fc = (fr && fr.内容) || {}
      P(`- 切换校验：激活标签「${esc(d.selected)}」${d.verified ? '（可信）' : '**不一致 → 不可信**'}　截图：\`shots/${esc(d.截图)}\``)
      P(`- 右栏面板标题：${esc(s.panelTitle) || '(未取到)'}`)
      P(`- 形态判定：${fr ? `**内嵌 iframe 设计器**（模板设计画布，父文档内无表单 DOM）` : ((s.tables && s.tables.length) ? '表格/列表' : ((d.内容 || {}).looseFields || []).length ? '表单' : '仅静态说明文本（无表单、无表格）')}`)
      if (fr) {
        P(`- 内嵌 iframe 地址：\`${esc(fr.src)}\``)
        P(`- iframe 内工具按钮：${(fc.按钮 || []).map(esc).join(' / ') || '(未取到)'}`)
        if ((fc.tables || []).length) for (const tb of fc.tables) P(`- iframe 内表格：表头 [${(tb.headers || []).map(esc).join(' / ') || '(无 thead)'}]，行数 ${tb.rowCount}`)
        P('')
        if ((fc.labels || []).length) {
          P(`- **字段清单（label，逐字，共 ${(fc.labels || []).length} 个）**：${(fc.labels || []).map(x => '`' + esc(x) + '`').join(' ')}`)
          P('')
        }
        if ((fc.controls || []).length) {
          P('| 控件 | 关联标签 | 当前值 | placeholder |')
          P('| --- | --- | --- | --- |')
          for (const c of (fc.controls || []).slice(0, 40)) P(`| ${esc(c.控件)} | ${esc(c.标签)} | ${esc(c.值)} | ${esc(c.placeholder)} |`)
          P('')
        }
        if ((fc.字段候选 || []).length) {
          P(`- 页面内可见字段/数据项文本（按序去重，前 80 条）：${(fc.字段候选 || []).slice(0, 80).map(x => '`' + esc(x) + '`').join(' ')}`)
          P('')
        }
      } else {
        if (s.tables && s.tables.length) {
          for (const tb of s.tables) P(`  - 表格 ${esc(tb.cls)}：表头 [${(tb.headers || []).map(esc).join(' / ') || '(无 thead)'}]，行数 ${tb.rowCount}`)
        }
        if ((s.gridHeaders || []).length) P(`- 表头（th 汇总）：${s.gridHeaders.map(esc).join(' / ')}`)
        if ((s.buttons || []).length) P(`- 按钮：${s.buttons.map(esc).join(' / ')}`)
      }
      const fields = [...((d.内容 || {}).looseFields || [])]
      for (const c of (d.内容 || {}).cards || []) for (const f of c.配置项 || []) fields.push(f)
      if (fields.length) {
        P('')
        P('| 字段名称 | 控件类型 | 当前值 | 取值域/选项集 | 帮助文案 |')
        P('| --- | --- | --- | --- | --- |')
        for (const f of fields) {
          const range = rangeOf(d.下拉明细, f.名称)
          P(`| ${esc(f.名称)} | ${esc(f.控件)} | ${esc(f.当前值) || '(空)'} | ${esc(range) || '-'} | ${esc((d.帮助属性汇总 || {})[f.名称] || f.帮助文案_属性 || '未取到')} |`)
        }
      }
      if ((d.下拉明细 || []).length) {
        P('')
        for (const x of d.下拉明细) {
          P(`- 下拉 ${esc(x.id)}（归属：${esc(x.owner)}）当前值「${esc(x.当前值)}」展开=${x.展开 ? '是' : '否'} 选项：${(x.选项 || []).map(o => '`' + esc(o) + '`').join(' ') || '未取到'}`)
        }
      }
      const bodyText = (fc.text || s.text || '')
      if (bodyText) P(`\n- 右栏正文（逐字节选，前 800 字）：${esc(bodyText.slice(0, 800))}`)
      P('')
    }
  }

  // ---- 未取到清单 ----
  P('## 四、未取到清单')
  P('')
  const 硬缺口 = []   // 结构性缺口：整标签/整弹窗没抓到
  const 气泡缺口 = [] // 只影响「live 点开气泡」，文案仍可从 data-dojo-props 取到
  const 其它缺口 = []
  const 折叠 = (arr, n = 6) => arr.length <= n ? arr.join('、') : arr.slice(0, n).join('、') + ` 等 ${arr.length} 项`

  if (sys) {
    for (const t of TABS_SYS) {
      const d = sys.标签[t]
      if (!d) { 硬缺口.push(`系统参数 / ${t}：标签切换失败，整标签未取到`); continue }
      if (!d.verified) 硬缺口.push(`系统参数 / ${t}：点击后激活标签为「${d.selected}」，与目标不一致，结果不可信`)
      const c = d.内容 || {}
      if (c.err) 硬缺口.push(`系统参数 / ${t}：主体结构提取失败（${c.err}）`)
      const 不可见 = (d.帮助气泡明细 || []).filter(x => !x.取到 && /不可见/.test(x.原因 || ''))
      const 未弹出 = (d.帮助气泡明细 || []).filter(x => !x.取到 && !/不可见/.test(x.原因 || ''))
      if (不可见.length) 气泡缺口.push(`系统参数 / ${t}：${不可见.length} 个 help 气泡因所在配置项当前隐藏而未点开（${折叠(不可见.map(x => x.owner))}）——对应文案已在正文表格以 data-dojo-props 来源给出`)
      if (未弹出.length) 气泡缺口.push(`系统参数 / ${t}：${未弹出.length} 个可见 help 气泡点开后无气泡层（${折叠(未弹出.map(x => x.owner))}）——对应文案已在正文表格以 data-dojo-props 来源给出`)
      const 未展开 = (d.下拉明细 || []).filter(x => !x.展开 && x.visible !== false)
      if (未展开.length) 其它缺口.push(`系统参数 / ${t}：${未展开.length} 个可见下拉未展开出选项集（${折叠(未展开.map(x => x.owner))}）`)
      const 隐藏卡 = (c.cards || []).filter(x => !x.卡片体可见)
      if (隐藏卡.length) 其它缺口.push(`系统参数 / ${t}：${隐藏卡.length} 张卡片体当前隐藏（因卡片开关=关，仅取到标题/开关/隐藏体内部的配置项列表）（${折叠(隐藏卡.map(x => x.卡片标题))}）`)
    }
  } else 硬缺口.push('系统参数：整页未取到')

  if (aud) {
    const got = new Set((aud.行 || []).map(r => r.单据))
    const 缺 = BILL_TYPES.filter(b => !got.has(b))
    if (缺.length) 硬缺口.push(`审核设置：${缺.length} 类单据整行未取到（${折叠(缺)}）`)
    const 弹窗缺 = (aud.行 || []).filter(r => (r.弹窗 || {}).err).map(r => r.单据)
    if (弹窗缺.length) 硬缺口.push(`审核设置：${弹窗缺.length} 类单据弹窗未打开（${折叠(弹窗缺)}）`)
    const 选择器缺 = (aud.行 || []).filter(r => !((r.审核人选择器 || {}).弹窗 || []).some(m => m.title && m.title !== '审核设置')).map(r => r.单据)
    if (选择器缺.length) 其它缺口.push(`审核设置：${选择器缺.length} 类单据的「操作员选择」第二层弹层未打开（${折叠(选择器缺)}）——已取到该单据弹窗内全部字段与条件清单`)
    const 隐气泡 = (aud.行 || []).filter(r => (r.帮助气泡明细 || []).some(h => !h.取到 && /不可见/.test(h.原因 || ''))).map(r => r.单据)
    if (隐气泡.length) 气泡缺口.push(`审核设置：${隐气泡.length} 类单据弹窗内唯一的 ? 图标所在字段（「待审核订单允许取消」）被隐藏，气泡点不开（${折叠(隐气泡)}）——对应文案已按 data-dojo-props 写入该单据表格`)
    const 无气泡 = (aud.行 || []).filter(r => (r.帮助气泡明细 || []).some(h => !h.取到 && !/不可见/.test(h.原因 || ''))).map(r => r.单据)
    if (无气泡.length) 气泡缺口.push(`审核设置：${无气泡.length} 类单据的可见 ? 图标点开后无气泡层（${折叠(无气泡)}）`)
  } else 硬缺口.push('审核设置：整页未取到')

  if (prt) {
    for (const t of TABS_PRINT) {
      const d = prt.标签[t]
      if (!d) { 硬缺口.push(`打印设置 / ${t}：标签切换失败，整标签未取到`); continue }
      if (!d.verified) 硬缺口.push(`打印设置 / ${t}：点击后激活标签为「${d.selected}」，与目标不一致，结果不可信`)
      const 未展开 = (d.下拉明细 || []).filter(x => !x.展开 && x.visible !== false)
      if (未展开.length) 其它缺口.push(`打印设置 / ${t}：${未展开.length} 个可见下拉未展开出选项集（${折叠(未展开.map(x => x.owner))}）`)
      const 隐 = (d.下拉明细 || []).filter(x => !x.展开 && x.visible === false)
      if (隐.length) 其它缺口.push(`打印设置 / ${t}：${隐.length} 个下拉因控件当前隐藏未展开（${折叠(隐.map(x => x.owner))}）`)
      const 坏气泡 = (d.帮助气泡明细 || []).filter(x => !x.取到)
      if (坏气泡.length) 气泡缺口.push(`打印设置 / ${t}：${坏气泡.length} 个 help 气泡未点开（${折叠(坏气泡.map(x => x.owner))}）`)
      // 当前隐藏的配置项（如「自动打印出库单」关着时其打印机/模板下拉是隐藏的，无法展开选项集）
      const 隐字段 = [...((d.内容 || {}).looseFields || [])].filter(f => f.可见 === false)
      if (隐字段.length) 其它缺口.push(`打印设置 / ${t}：${隐字段.length} 个配置项当前隐藏（控件不可见 → 选项集无法展开，仅取到字段名/类型/默认值）（${折叠(隐字段.map(f => f.名称))}）`)
      const visFrames = (d.内嵌iframe || []).filter(f => f.可见)
      for (const fr of visFrames) {
        const c = fr.内容 || {}
        if (c.err || (!(c.text || '').trim() && !(c.controls || []).length && !(c.列表项 || []).length)) {
          硬缺口.push(`打印设置 / ${t}：可见 iframe（${fr.src}）内容未取到（${c.err || '空内容'}）`)
        }
      }
    }
  } else 硬缺口.push('打印设置：整页未取到')

  P('### 4.1 结构性缺口（整标签/整弹窗没抓到）')
  P('')
  if (!硬缺口.length) P('- 无')
  else for (const m of 硬缺口) P(`- ${m}`)
  P('')
  P('### 4.2 live 气泡缺口（`?` 未点开；但同一条文案已从控件 `data-dojo-props` 取到并写进正文表格）')
  P('')
  if (!气泡缺口.length) P('- 无')
  else for (const m of 气泡缺口) P(`- ${m}`)
  P('')
  P('### 4.3 其它缺口（下拉未展开 / 第二层弹层未打开 / 卡片体隐藏）')
  P('')
  if (!其它缺口.length) P('- 无')
  else for (const m of 其它缺口) P(`- ${m}`)
  P('')
  fs.writeFileSync(path.join(OUT, '_summary.md'), L.join('\n'), 'utf8')
  log('summary ->', path.join(OUT, '_summary.md'), `(${L.length} 行)`)
}

/* ================= 入口 ================= */

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  fs.mkdirSync(SHOTS, { recursive: true })
  if (MODE === 'summary') { genSummary(); return }

  const headless = process.env.QL361_HEADLESS === '1'
  const browser = await chromium.launch({ headless, args: ['--window-size=1920,1000'] })
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1000 }, storageState: fs.existsSync(STATE) ? STATE : undefined })
  const page = await ctx.newPage()
  try {
    await page.goto(BASE, { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {})
    await page.waitForTimeout(4000)
    const logged = await page.evaluate(() => (document.body.innerText || '').includes('欢迎您')).catch(() => false)
    if (!logged) { log('登录态失效，重新登录…'); await loginQl361(page, ctx, log); await page.waitForTimeout(3000) }
    await closeAllDialogs(page)
    await ctx.storageState({ path: STATE }).catch(() => {})

    if (MODE === 'debug') {
      const pageName = WANT[0] || '打印设置'
      const tabName = WANT[1] || '箱号模板'
      const r = await openPage(page, pageName)
      log('debug 打开', pageName, JSON.stringify(r))
      const sw = await clickLeftTab(page, tabName)
      log('debug 切标签', tabName, JSON.stringify(sw))
      for (const wait of [2000, 4000, 6000]) {
        await page.waitForTimeout(wait)
        const d = await page.evaluate(() => {
          const txt = e => (e ? (e.innerText || '').replace(/\s+/g, ' ').trim() : '')
          const all = [...document.querySelectorAll('[data-id]')].map(e => {
            const rr = e.getBoundingClientRect()
            return {
              tag: e.tagName, dataId: e.getAttribute('data-id'), cls: String(e.className).slice(0, 80),
              off: e.offsetParent !== null, w: Math.round(rr.width), h: Math.round(rr.height), x: Math.round(rr.x), y: Math.round(rr.y),
              textLen: (e.innerText || '').length, disp: getComputedStyle(e).display,
            }
          })
          const tcs = [...document.querySelectorAll('.tabcontent')].map(e => ({
            dataId: e.getAttribute('data-id'), cls: String(e.className).slice(0, 80),
            off: e.offsetParent !== null, w: Math.round(e.getBoundingClientRect().width), disp: getComputedStyle(e).display,
          }))
          return { dataIdEls: all, tabcontents: tcs, bodyTextTail: txt(document.body).slice(-600) }
        }).catch(e => ({ err: e.message }))
        log('debug@', wait, JSON.stringify(d))
      }
      await shotPage(page, `_debug-${pageName}-${tabName}`)
      return
    }

    if (MODE === 'discover') {
      for (const p of ['系统参数', '打印设置', '审核设置']) {
        const r = await openPage(page, p)
        log('勘探', p, JSON.stringify(r))
        if (r.ok) {
          await shotPage(page, `_discover-${p}`)
          saveHtml(p, await page.evaluate(() => {
            const cs = [...document.querySelectorAll('div.tabcontent')].filter(e => e.offsetParent !== null && e.getBoundingClientRect().width > 300)
            const sc = cs[cs.length - 1] || document.body
            return sc.outerHTML.slice(0, 600000)
          }))
        }
      }
      log('勘探完成 →', DISC)
    } else {
      const pages = WANT.length ? WANT : ['系统参数', '审核设置', '打印设置']
      if (pages.includes('系统参数')) await probeSystemParams(page)
      if (pages.includes('审核设置')) await probeAudit(page)
      if (pages.includes('打印设置')) await probePrint(page)
      await ctx.storageState({ path: STATE }).catch(() => {})
      genSummary()
    }
  } finally {
    await browser.close()
    log('完成 →', OUT)
  }
})().catch(e => { console.error(e); process.exit(1) })
