/* ql361 任意域对标抓取（复用登录态）—— 自动从左侧菜单发现页面并逐页抓取
 *
 * 运行： NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/ql361-domain-crawl.cjs <域> [页面名...]
 *   域     = 销售|采购|仓储|配发收|财务|营销|商城|分析|资料|设置（左侧一级菜单逐字）
 *   页面名 = 可选，只抓指定页面；不传则抓该域全部
 * 输出： tool-results/ql361/<域>-live/<页名>.json + .png ；域菜单结构 → _menu.json
 *
 * 抓取口径（同《抓取结果/抓取方法指南.md》）：
 *   ① 列配置弹窗 = 表头 .icon-shezhi2（读「全局配置」Tab；QL361_CFG_TAB=personal 可切）
 *   ② 页面配置弹窗 = 顶部按钮栏「配置」齿轮（本次未展开，按需补）
 *   ③ 多 Tab 页面：先切 Tab 再点该 Tab 内的 .icon-shezhi2
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const DOMAIN = process.argv[2]
if (!DOMAIN) { console.error('用法: node tools/ql361-domain-crawl.cjs <域> [页面名...]'); process.exit(1) }
const WANT = process.argv.slice(3)

const OUT = path.resolve(__dirname, `../tool-results/ql361/${DOMAIN}-live`)
const SHOTS = path.join(OUT, 'shots')
const STATE = path.resolve(__dirname, '../tool-results/ql361/product/ql361-state.json')
if (!fs.existsSync(OUT)) fs.mkdirSync(OUT, { recursive: true })
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const log = (...a) => console.log(`[${DOMAIN}]`, ...a)
const saveJson = (name, data) => {
  fs.writeFileSync(path.join(OUT, `${name}.json`), JSON.stringify(data, null, 2), 'utf8')
  log('json ->', `${name}.json (${JSON.stringify(data).length}B)`)
}

async function closeAllDialogs(page) {
  await page.evaluate(() => {
    [...document.querySelectorAll('.lkgrid-setting-dialog, .dijitDialog, .dojoxDialog')]
      .filter(d => d.getBoundingClientRect().width > 0)
      .forEach(d => { const b = d.querySelector('.dijitDialogCloseIcon'); if (b) b.click() })
  }).catch(() => {})
  await page.waitForTimeout(300)
}

/** 打开一级菜单（已弹出则复用；否则点击并轮询等待 popupmenu 可见） */
async function openDomainMenu(page) {
  await closeAllDialogs(page)
  const already = await page.evaluate(() => {
    const p = document.querySelector('.popupmenu__repeat')
    return !!p && p.getBoundingClientRect().width > 0
  })
  if (already) return true
  const anchors = page.locator('a.menu-0-item')
  const n = await anchors.count()
  for (let i = 0; i < n; i++) {
    const a = anchors.nth(i)
    if ((await a.innerText().catch(() => '')).trim() !== DOMAIN) continue
    for (let attempt = 1; attempt <= 4; attempt++) {
      await a.click({ timeout: 5000 }).catch(() => {})
      for (let w = 0; w < 10; w++) {
        await page.waitForTimeout(400)
        const ok = await page.evaluate(() => {
          const p = document.querySelector('.popupmenu__repeat')
          return !!p && p.getBoundingClientRect().width > 0
        })
        if (ok) return true
      }
      await page.evaluate((d) => {
        const a2 = [...document.querySelectorAll('a.menu-0-item')].find(x => (x.innerText || '').trim() === d)
        if (a2) ['mouseenter', 'mouseover', 'mousemove'].forEach(ev => a2.dispatchEvent(new MouseEvent(ev, { bubbles: true })))
      }, DOMAIN).catch(() => {})
    }
    return false
  }
  return false
}

/** 读菜单结构：组（.popupmenu__group-title）→ 页面项（.popupmenu__list-item，仅此项可点）
 *  ⚠️ `.popupmenu__list-title` 是**子分组标题**（非页面，点了会打开错误内容），必须排除。 */
async function readMenuStructure(page) {
  return await page.evaluate(() => {
    const root = [...document.querySelectorAll('.popupmenu__repeat')].find(d => d.getBoundingClientRect().width > 0)
    if (!root) return null
    return [...root.querySelectorAll('.popupmenu__group')].map(g => {
      const group = (g.querySelector('.popupmenu__group-title') || {}).innerText || ''
      const list = g.querySelector('.popupmenu__list') || g
      const items = []
      let sub = ''
      for (const child of list.children) {
        const cls = (child.className || '').toString()
        const t = (child.innerText || '').trim()
        if (cls.includes('list-title')) { sub = t; continue }
        if (cls.includes('list-item') && t) items.push({ name: t, subGroup: sub })
      }
      return { group: group.trim(), items }
    }).filter(g => g.items.length)
  })
}

async function clickMenuItem(page, itemName) {
  return await page.evaluate((nm) => {
    const root = [...document.querySelectorAll('.popupmenu__repeat')].find(d => d.getBoundingClientRect().width > 0)
    if (!root) return 'no popup'
    const item = [...root.querySelectorAll('.popupmenu__list-item')]
      .find(e => (e.innerText || '').trim() === nm)
    if (!item) return 'no item'
    item.click()
    return 'clicked'
  }, itemName)
}

/** 对「当前可见的某个弹窗」按元素截图（仓储/采购模块的文档里就是这种弹窗图） */
async function shotDialog(page, dialogSel, outPath) {
  const ok = await page.evaluate((sel) => {
    const d = [...document.querySelectorAll(sel)].filter(x => {
      const r = x.getBoundingClientRect(); return r.width > 150 && r.height > 100 && x.offsetParent !== null
    })[0]
    if (!d) return false
    d.setAttribute('data-shot-target', '1')
    return true
  }, dialogSel).catch(() => false)
  if (!ok) return false
  await page.waitForTimeout(250)
  try {
    await page.locator('[data-shot-target="1"]').screenshot({ path: outPath })
    return true
  } catch (e) {
    return false
  } finally {
    await page.evaluate(() => document.querySelector('[data-shot-target="1"]')?.removeAttribute('data-shot-target')).catch(() => {})
  }
}

/** 打开顶部「配置」齿轮的页面配置弹窗并截图；返回 { found, saved } */
async function capturePageConfig(page, outPath) {
  // 进来前先确保没有残留弹窗（否则齿轮点不到 / 截图截到旧弹窗）
  await closeAllDialogs(page)
  await page.keyboard.press('Escape').catch(() => {})
  await page.waitForTimeout(500)
  const probe = await page.evaluate(() => {
    const all = [...document.querySelectorAll('*')]
    const cand = all.filter(e => {
      const cls = (e.className || '').toString()
      const title = e.getAttribute('title') || ''
      return /iconButtonConfig|Config/.test(cls) || /^配置$/.test(title) || /配置/.test(e.getAttribute('aria-label') || '')
    }).map(e => {
      const r = e.getBoundingClientRect()
      return {
        cls: (e.className || '').toString().slice(0, 60), tag: e.tagName,
        title: e.getAttribute('title') || '', txt: (e.innerText || '').trim().slice(0, 10),
        y: Math.round(r.y), w: Math.round(r.width), h: Math.round(r.height), vis: e.offsetParent !== null
      }
    }).filter(o => o.w > 4 && o.h > 4 && o.vis)
    const hit = all.find(e => {
      const cls = (e.className || '').toString()
      const r = e.getBoundingClientRect()
      if (!(r.width > 8 && r.height > 8 && r.y > 80 && e.offsetParent !== null)) return false
      if (cls.includes('icon-shezhi2')) return false
      return /iconButtonConfig/.test(cls) || /^配置$/.test(e.getAttribute('title') || '')
    })
    if (hit) { hit.click(); return { clicked: true, cand: cand.slice(0, 8) } }
    return { clicked: false, cand: cand.slice(0, 8) }
  }).catch(() => ({ clicked: false, cand: [] }))
  if (!probe.clicked) return { found: false, candidates: probe.cand }
  await page.waitForTimeout(1500)
  // 页面配置弹窗是「Vue dialog」（非 dojo），按文本特征取「最小的那个含查询条件+功能按钮的容器」
  const marked = await page.evaluate(() => {
    // 页面配置弹窗特征：底部固定提示「使用鼠标拖动行可以调整顺序」+「恢复默认值」；
    // ⚠️ 部分页只有「查询条件」Tab（**无「功能按钮」Tab**），故不能强制要求含「功能按钮」。
    const cands = [...document.querySelectorAll('div')].filter(e => {
      const t = e.innerText || ''
      const r = e.getBoundingClientRect()
      if (!(r.width > 200 && r.height > 120 && e.offsetParent !== null)) return false
      const hitFooter = t.includes('使用鼠标拖动') || t.includes('恢复默认值')
      const hitTab = t.includes('查询条件') || t.includes('功能按钮')
      return hitFooter && hitTab
    }).map(e => ({ e, area: e.getBoundingClientRect().width * e.getBoundingClientRect().height }))
      .sort((a, b) => a.area - b.area)
    if (!cands.length) return false
    cands[0].e.setAttribute('data-shot-target', '1')
    return true
  }).catch(() => false)
  let saved = false
  if (marked) {
    await page.waitForTimeout(250)
    try { await page.locator('[data-shot-target="1"]').screenshot({ path: outPath }); saved = true } catch (e) { saved = false }
    await page.evaluate(() => document.querySelector('[data-shot-target="1"]')?.removeAttribute('data-shot-target')).catch(() => {})
  } else {
    saved = await shotDialog(page, '.dijitDialog, .dojoxDialog, [role=dialog]', outPath)
  }
  await closeAllDialogs(page)
  // 关闭 Vue 弹窗：优先点「关闭(Esc)」按钮，其次 Esc
  await page.evaluate(() => {
    const btn = [...document.querySelectorAll('button, .ant-btn, div, span')]
      .filter(e => e.offsetParent !== null && /关闭\s*\(Esc\)/.test((e.innerText || '').trim()))
      .pop()
    if (btn) btn.click()
  }).catch(() => {})
  await page.keyboard.press('Escape').catch(() => {})
  await page.waitForTimeout(500)
  return { found: true, saved, candidates: probe.cand }
}

async function readColumnConfig(page, shotPath) {
  const clicked = await page.evaluate(() => {
    const vis = [...document.querySelectorAll('.icon-shezhi2')].filter(e => {
      const r = e.getBoundingClientRect(); return r.width > 0 && r.x > 0
    })
    vis.sort((a, b) => a.getBoundingClientRect().y - b.getBoundingClientRect().y)
    if (vis[0]) { vis[0].click(); return true }
    return false
  })
  if (!clicked) return { err: 'no gear' }
  await page.waitForTimeout(700)
  const wantTab = process.env.QL361_CFG_TAB === 'personal' ? '个人配置' : '全局配置'
  await page.evaluate((want) => {
    const dlg = [...document.querySelectorAll('.lkgrid-setting-dialog')].filter(d => d.getBoundingClientRect().width > 0)[0]
    if (!dlg) return
    const g = [...dlg.querySelectorAll('div.button-item')].find(e => e.innerText.trim() === want)
    if (g) g.click()
  }, wantTab).catch(() => {})
  await page.waitForTimeout(500)
  const res = await page.evaluate(() => {
    const dlg = [...document.querySelectorAll('.lkgrid-setting-dialog')].filter(d => d.getBoundingClientRect().width > 0)[0]
    if (!dlg) return { err: 'no dialog' }
    const raw = [...dlg.querySelectorAll('tr')].map(r => {
      const cb = r.querySelector('input[type=checkbox]') || r.querySelector('.dijitCheckBox, .dijitCheckBoxInput')
      let checked = false
      if (cb) checked = cb.checked === true || (cb.className || '').includes('Checked') || cb.getAttribute('aria-checked') === 'true'
      return { texts: [...r.querySelectorAll('td')].map(td => td.innerText.trim()).filter(t => t), checked }
    }).filter(r => r.texts.length > 0)
    const seen = new Set(); const cols = []
    for (const r of raw) {
      const m = r.texts[0].match(/^(\d+)\t+([\s\S]+)$/)
      if (!m) continue
      const nm = m[2].replace(/\t/g, '\\')
      if (seen.has(nm)) continue
      seen.add(nm); cols.push({ seq: +m[1], name: nm, def: r.checked })
    }
    return { count: cols.length, defCount: cols.filter(c => c.def).length, cols }
  })
  if (shotPath) res.shot = await shotDialog(page, '.lkgrid-setting-dialog', shotPath)
  await closeAllDialogs(page)
  return res
}

/** 读「当前激活业务页面」容器内的快照 + 页内 Tab（严格限定在该容器内，避免抓到桌面标签栏 / 其它已打开页） */
async function readActivePane(page) {
  return await page.evaluate(() => {
    const panes = [...document.querySelectorAll('.dijitContentPane, .dijitTabPaneWrapper')]
      .filter(p => { const r = p.getBoundingClientRect(); return r.width > 800 && r.height > 300 && p.offsetParent !== null })
    const pane = panes[panes.length - 1]
    if (!pane) return { err: 'no pane' }
    const txt = pane.innerText || ''
    const headers = [...pane.querySelectorAll('th, [class*=columnheader], [class*=dgrid-column]')]
      .map(e => (e.innerText || '').trim().replace(/\s+/g, ''))
      .filter(t => t && t.length < 24 && !/^\d+$/.test(t) && !/^\d{4}-\d{2}/.test(t))
    const tree = [...pane.querySelectorAll('[class*=tree], [class*=Tree]')]
      .map(e => (e.innerText || '').replace(/\s+/g, '|').slice(0, 400))[0] || ''
    // 页内 Tab：桌面标签栏固定在顶部（y≈48，高 32），页内视图 Tab 在 y>80。
    // 用几何位置区分，避免把「工作台/本页名」等桌面标签当成页内 Tab。
    const innerTabEls = [...document.querySelectorAll('.dijitTabInner')].filter(e => {
      const r = e.getBoundingClientRect()
      return r.width > 0 && r.height > 0 && r.height < 46 && r.y > 80 && e.innerText.trim()
    })
    const tabs = [...new Set(innerTabEls.map(e => e.innerText.trim()))]
      .filter(t => t.length <= 14 && !/^(首页|上页|下页|尾页|跳转|查询|重置|保存|关闭|确定|取消|导出|打印)$/.test(t))
    const checked = innerTabEls.filter(e => (e.className || '').includes('Checked')).map(e => e.innerText.trim())
    // 当前激活的桌面标签名（y<60 的那条标签栏）——用于校验「打开的确实是目标页」
    const desktopTab = [...document.querySelectorAll('.dijitTabInner')]
      .filter(e => {
        const r = e.getBoundingClientRect()
        return r.width > 0 && r.height > 0 && r.y < 60 && (e.className || '').includes('Checked')
      })
      .map(e => e.innerText.trim())[0] || ''
    return {
      headers: [...new Set(headers)].slice(0, 50),
      tree,
      record: (txt.match(/共\s*[\d,]+\s*条记录/) || [''])[0],
      head: txt.slice(0, 500).replace(/\s+/g, ' '),
      tabs: tabs.slice(0, 8),
      checkedTab: checked,
      desktopTab
    }
  })
}

/** 点击页内 Tab（几何规则同上：y>80，绝不点桌面标签栏） */
async function clickInnerTab(page, tabName) {
  return await page.evaluate((tn) => {
    const hit = [...document.querySelectorAll('.dijitTabInner')].filter(e => {
      const r = e.getBoundingClientRect()
      return r.width > 0 && r.height > 0 && r.y > 80 && e.innerText.trim() === tn
    })
    if (!hit.length) return false
    hit[0].click()
    return true
  }, tabName)
}

async function crawlPage(page, itemName, group, subGroup) {
  log('▶', itemName)
  // ⚠️ 每页前重载桌面：ql361 是 MDI，已打开页面的 DOM（含其页内 Tab）会残留，
  //    否则会点到「上一个页面」的 Tab 或把上一页的列清单当成本页的。
  await page.goto('https://22stable.ql361.com/desktop.html', { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {})
  await page.waitForTimeout(3500)
  await page.evaluate(() => {
    [...document.querySelectorAll('.dojoxDialogCloseIcon, .dijitDialogCloseIcon')].forEach(e => { try { e.click() } catch (e2) {} })
  }).catch(() => {})
  await page.waitForTimeout(500)
  await openDomainMenu(page)
  const r = await clickMenuItem(page, itemName)
  if (r !== 'clicked') { log('  ✗ 菜单点击失败', r); return { page: itemName, err: r } }
  await page.waitForTimeout(3800)

  const out = { page: itemName, group, subGroup: subGroup || '', domain: DOMAIN, 抓取时间: new Date().toISOString().slice(0, 10), tabs: {} }

  const first = await readActivePane(page)
  if (first.desktopTab && first.desktopTab !== itemName) {
    log(`  ⚠️ 当前激活标签「${first.desktopTab}」≠ 目标「${itemName}」——结果可疑`)
  }
  out.desktopTab = first.desktopTab
  const isListPage = !!(first.headers && first.headers.length > 1)
  // ⚠️ 页内 Tab 判定**不能**依赖「本页是列表页」：像「发短信」这类页，主 Tab 是表单，
  //    但其「短信历史 / 短信模板管理」子 Tab 仍是列表（各有独立列配置）。
  //    旧逻辑用 isListPage 卡一道，导致这些页的 Tab 全部丢失（发短信实测漏抓即此因）。
  const tabNames = first.tabs && first.tabs.length > 1 ? first.tabs : []

  if (tabNames.length) {
    for (const t of tabNames) {
      const ok = await clickInnerTab(page, t)
      await page.waitForTimeout(2200)
      if (!ok) continue
      const snap = await readActivePane(page)
      // 列配置弹窗截图（仓储/采购文档同款「数据表列配置弹窗」图）
      const colShot = path.join(SHOTS, `${itemName}-${t}-列配置弹窗.png`)
      out.tabs[t] = { snapshot: snap, columnConfig: await readColumnConfig(page, colShot) }
      // 页面配置弹窗截图（顶部「配置」齿轮）
      const pcShot = path.join(SHOTS, `${itemName}-${t}-页面配置弹窗.png`)
      out.tabs[t].pageConfig = await capturePageConfig(page, pcShot)
      const c = out.tabs[t].columnConfig
      log(`  · tab ${t}  cols=${c.count} def=${c.defCount} rec=${snap.record} 弹窗图: 列=${!!c.shot} 页=${!!(out.tabs[t].pageConfig || {}).saved}`)
    }
  }
  if (!Object.keys(out.tabs).length) {
    out.snapshot = first
    delete out.snapshot.tabs
    if (isListPage) {
      out.columnConfig = await readColumnConfig(page, path.join(SHOTS, `${itemName}-列配置弹窗.png`))
      out.pageConfig = await capturePageConfig(page, path.join(SHOTS, `${itemName}-页面配置弹窗.png`))
      const c = out.columnConfig
      log(`  · cols=${c.count} def=${c.defCount} rec=${first.record} 弹窗图: 列=${!!c.shot} 页=${!!(out.pageConfig || {}).saved}`)
    } else {
      await capturePageConfig(page, path.join(SHOTS, `${itemName}-页面配置弹窗.png`))
      log('  · 非列表页(表单/配置)')
    }
  }
  await page.screenshot({ path: path.join(OUT, `${itemName}.png`) }).catch(() => {})
  saveJson(itemName, out)
  return out
}

;(async () => {
  const browser = await chromium.launch({ headless: false, args: ['--window-size=1920,1000'] })
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1000 }, storageState: fs.existsSync(STATE) ? STATE : undefined })
  const page = await ctx.newPage()
  try {
    await page.goto('https://22stable.ql361.com/desktop.html', { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(4000)
    if (!(await page.evaluate(() => (document.body.innerText || '').includes('欢迎您')))) {
      log('登录态失效，重新登录…'); await loginQl361(page, ctx, log); await page.waitForTimeout(3000)
    }
    await page.evaluate(() => {
      [...document.querySelectorAll('.dojoxDialogCloseIcon, .dijitDialogCloseIcon')].forEach(e => { try { e.click() } catch (e2) {} })
    }).catch(() => {})
    await page.waitForTimeout(800)

    if (!(await openDomainMenu(page))) throw new Error('打不开一级菜单: ' + DOMAIN)
    const menu = await readMenuStructure(page)
    if (!menu) throw new Error('读不到菜单结构')
    saveJson('_menu', { domain: DOMAIN, 抓取时间: new Date().toISOString().slice(0, 10), menu })
    log('菜单结构:', menu.map(g => `${g.group}(${g.items.length})`).join(' '))

    const tasks = []
    for (const g of menu) for (const it of g.items) {
      if (WANT.length && !WANT.includes(it.name)) continue
      tasks.push({ item: it.name, group: g.group, subGroup: it.subGroup })
    }
    log('待抓', tasks.length, '页')
    for (const t of tasks) {
      try { await crawlPage(page, t.item, t.group, t.subGroup) }
      catch (e) { log('  ✗ 异常', t.item, e.message); saveJson(t.item, { page: t.item, group: t.group, err: e.message }) }
    }
    await ctx.storageState({ path: STATE }).catch(() => {})
  } finally {
    await browser.close()
    log('完成 →', OUT)
  }
})().catch(e => { console.error(e); process.exit(1) })
