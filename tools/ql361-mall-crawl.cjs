/* ql361「商城」域对标抓取（复用登录态）—— 交易模块文档金标准取证
 * 运行： node tools/ql361-mall-crawl.cjs [页面名...]
 *   不带参数 = 抓全部 13 个商城页面；带参数 = 只抓指定页面（如 商品上架 单位显示）
 * 输出： tool-results/ql361/mall-live/<页名>.json + <页名>.png
 *
 * 抓取口径（与《抓取结果/抓取方法指南.md》一致）：
 *   ① 列配置弹窗 = 数据表表头 .icon-shezhi2（个人配置/全局配置 两 Tab，读「全局配置」为准）
 *   ② 页面配置弹窗 = 顶部按钮栏「配置」齿轮（查询条件 / 功能按钮 两 Tab）
 *   ③ 多 Tab 页面逐 Tab 抓（先点 Tab 再点该 Tab 内的 .icon-shezhi2）
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const OUT = path.resolve(__dirname, '../tool-results/ql361/mall-live')
const STATE = path.resolve(__dirname, '../tool-results/ql361/product/ql361-state.json')
if (!fs.existsSync(OUT)) fs.mkdirSync(OUT, { recursive: true })

function log(...a) { console.log('[mall]', ...a) }
function saveJson(name, data) {
  fs.writeFileSync(path.join(OUT, `${name}.json`), JSON.stringify(data, null, 2), 'utf8')
  log('json ->', `${name}.json  (${JSON.stringify(data).length} bytes)`)
}

const PAGES = [
  { name: '订单处理', group: '订单处理', item: '订单处理', tabs: ['按单据', '按明细'], kind: 'list' },
  { name: '退货申请处理', group: '订单处理', item: '退货申请处理', tabs: ['按单据', '按明细'], kind: 'list' },
  { name: '商品上架', group: '基础业务', item: '商品上架', kind: 'list' },
  { name: '单位显示', group: '基础业务', item: '单位显示', kind: 'list' },
  { name: '买家申请管理', group: '基础业务', item: '买家申请管理', kind: 'list' },
  { name: '买家账号', group: '基础业务', item: '买家账号', kind: 'list' },
  { name: '商品组合', group: '基础业务', item: '商品组合', kind: 'list' },
  { name: '基础设置', group: '商城设置', item: '基础设置', kind: 'config' },
  { name: '店铺设置', group: '商城设置', item: '店铺设置', kind: 'config', subTabs: ['店铺参数', '注册设置', '支付设置'] },
  { name: '运费设置', group: '商城设置', item: '运费设置', kind: 'config', subTabs: ['物流', '到店自提'] },
  { name: '商城装修', group: '商城设置', item: '商城装修', kind: 'config' },
  { name: '公告设置', group: '商城设置', item: '公告设置', kind: 'list' },
  { name: '关键词库', group: '商城设置', item: '关键词库', kind: 'list' }
]

/** 打开「商城」域菜单（点击左侧一级菜单，弹出 popupmenu） */
async function openMallMenu(page) {
  await closeAllDialogs(page)
  // 已弹出则直接复用（避免二次点击把已开的菜单 toggle 关掉）
  const already = await page.evaluate(() => !!document.querySelector('.popupmenu__repeat') && document.querySelector('.popupmenu__repeat').getBoundingClientRect().width > 0)
  if (already) return true
  const anchors = page.locator('a.menu-0-item')
  const n = await anchors.count()
  for (let i = 0; i < n; i++) {
    const a = anchors.nth(i)
    const t = (await a.innerText().catch(() => '')).trim()
    if (t !== '商城') continue
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
      // 兜底：直接派发 hover 事件
      await page.evaluate(() => {
        const a2 = [...document.querySelectorAll('a.menu-0-item')].find(x => (x.innerText || '').trim() === '商城')
        if (a2) { ['mouseenter', 'mouseover', 'mousemove'].forEach(ev => a2.dispatchEvent(new MouseEvent(ev, { bubbles: true }))) }
      }).catch(() => {})
    }
    return false
  }
  return false
}

/** 在当前弹出的商城菜单里点某个菜单项 */
async function clickMenuItem(page, itemName) {
  return await page.evaluate((nm) => {
    const el = [...document.querySelectorAll('.popupmenu__repeat')].find(d => d.getBoundingClientRect().width > 0)
    if (!el) return 'no popup'
    const item = [...el.querySelectorAll('a, li, div')].filter(e => {
      const r = e.getBoundingClientRect()
      return r.width > 0 && r.height > 0 && r.height < 40 && e.children.length === 0 && (e.innerText || '').trim() === nm
    })[0]
    if (!item) return 'no item'
    item.click()
    return 'clicked'
  }, itemName)
}

/** 关闭所有可见的列配置 / 配置弹窗 */
async function closeAllDialogs(page) {
  await page.evaluate(() => {
    const dlgs = [...document.querySelectorAll('.lkgrid-setting-dialog, .dijitDialog, .dojoxDialog')]
      .filter(d => d.getBoundingClientRect().width > 0)
    dlgs.forEach(d => { const b = d.querySelector('.dijitDialogCloseIcon'); if (b) b.click() })
  }).catch(() => {})
  await page.waitForTimeout(300)
}

/** 读列配置弹窗（先切到「全局配置」Tab） */
async function readColumnConfig(page) {
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
  // 默认读「全局配置」；QL361_CFG_TAB=personal 时读「个人配置」（两 Tab 可能因用户自定义而不一致）
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
      const texts = [...r.querySelectorAll('td')].map(td => td.innerText.trim()).filter(t => t)
      return { texts, checked }
    }).filter(r => r.texts.length > 0)
    const seen = new Set(); const cols = []
    for (const r of raw) {
      const m = r.texts[0].match(/^(\d+)\t+([\s\S]+)$/)
      if (!m) continue
      const nm = m[2].replace(/\t/g, '\\')
      if (seen.has(nm)) continue
      seen.add(nm); cols.push({ seq: +m[1], name: nm, def: r.checked })
    }
    const title = (dlg.innerText.match(/^列配置/) || [''])[0]
    return { title, count: cols.length, defCount: cols.filter(c => c.def).length, cols }
  })
  await closeAllDialogs(page)
  return res
}

/** 读页面快照：工具栏 / 筛选区 / 表头 / 记录数 / 左侧树 */
async function readPageSnapshot(page) {
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
    const record = (txt.match(/共\s*[\d,]+\s*条记录/) || [''])[0]
    return {
      headers: [...new Set(headers)].slice(0, 45),
      tree,
      record,
      head: txt.slice(0, 420).replace(/\s+/g, ' ')
    }
  })
}

/** 抓单页 */
async function crawlPage(page, p) {
  log('▶ 打开', p.name)
  await openMallMenu(page)
  const r = await clickMenuItem(page, p.item)
  if (r !== 'clicked') { log('  ✗ 菜单点击失败', r); return { err: r } }
  await page.waitForTimeout(3500)

  const out = { page: p.name, group: p.group, item: p.item, 抓取时间: new Date().toISOString().slice(0, 10), tabs: {} }

  if (p.kind === 'list') {
    const tabNames = p.tabs && p.tabs.length ? p.tabs : [null]
    for (const t of tabNames) {
      if (t) {
        const ok = await page.evaluate((tn) => {
          const vis = [...document.querySelectorAll('.dijitTabInner')].filter(e => {
            const rr = e.getBoundingClientRect(); return rr.width > 0 && rr.height > 0 && e.innerText.trim() === tn
          })
          if (!vis.length) return false
          vis[vis.length - 1].click()
          return true
        }, t)
        await page.waitForTimeout(2500)
        if (!ok) { out.tabs[t] = { err: 'tab not found' }; continue }
      }
      const snap = await readPageSnapshot(page)
      const cols = await readColumnConfig(page)
      const checkedTab = await page.evaluate(() => {
        const vis = [...document.querySelectorAll('.dijitTabInner')].filter(e => {
          const rr = e.getBoundingClientRect(); return rr.width > 0 && rr.height > 0 && /^按单据$|^按明细$/.test(e.innerText.trim())
        })
        return vis.filter(e => (e.className || '').includes('Checked')).map(e => e.innerText.trim())
      })
      out.tabs[t || 'default'] = { snapshot: snap, columnConfig: cols, checkedTab }
      log('  · tab', t || 'default', 'cols=', cols.count, 'def=', cols.defCount, 'rec=', snap && snap.record)
    }
  } else {
    // 配置/表单型页面
    if (p.subTabs) {
      const tabs = await page.evaluate(() => {
        const vis = [...document.querySelectorAll('.dijitTabInner, [class*=nav], [class*=Nav], li, div')]
          .filter(e => { const rr = e.getBoundingClientRect(); return rr.width > 0 && rr.height > 0 && rr.height < 46 && e.children.length === 0 && (e.innerText || '').trim() })
          .map(e => e.innerText.trim()).filter(t => t.length < 12 && /设置|参数|物流|自提|导航|装修|分类页|首页|模板/.test(t))
        return [...new Set(vis)].slice(0, 20)
      })
      out.subTabCandidates = tabs
      out.tabs = {}
      for (const st of p.subTabs) {
        const ok = await page.evaluate((tn) => {
          const el = [...document.querySelectorAll('*')].filter(e => {
            const rr = e.getBoundingClientRect()
            return rr.width > 0 && rr.height > 0 && rr.height < 46 && e.children.length === 0 && (e.innerText || '').trim() === tn
          })[0]
          if (!el) return false
          el.click(); return true
        }, st)
        await page.waitForTimeout(2200)
        const snap = await readPageSnapshot(page)
        out.tabs[st] = { ok, snapshot: snap }
        log('  · 子项', st, ok ? 'ok' : 'MISS', snap && snap.head ? snap.head.slice(0, 60) : '')
      }
    } else {
      out.snapshot = await readPageSnapshot(page)
      log('  · config page:', out.snapshot && out.snapshot.head ? out.snapshot.head.slice(0, 80) : '')
    }
  }

  await page.screenshot({ path: path.join(OUT, `${p.name}.png`) }).catch(() => {})
  saveJson(p.name, out)
  return out
}

;(async () => {
  const want = process.argv.slice(2)
  const targets = want.length ? PAGES.filter(p => want.includes(p.name)) : PAGES
  const browser = await chromium.launch({ headless: false, args: ['--window-size=1920,1000'] })
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1000 }, storageState: fs.existsSync(STATE) ? STATE : undefined })
  const page = await ctx.newPage()
  try {
    await page.goto('https://22stable.ql361.com/desktop.html', { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(4000)
    const logged = await page.evaluate(() => (document.body.innerText || '').includes('欢迎您'))
    if (!logged) { log('登录态失效，重新登录…'); await loginQl361(page, ctx, log); await page.waitForTimeout(3000) }
    // 关闭「预警提醒」等弹窗
    await page.evaluate(() => {
      [...document.querySelectorAll('.dojoxDialogCloseIcon, .dijitDialogCloseIcon')].forEach(e => { try { e.click() } catch (e2) {} })
    }).catch(() => {})
    await page.waitForTimeout(800)
    log('开始抓取', targets.length, '个页面')
    for (const p of targets) {
      try { await crawlPage(page, p) } catch (e) { log('  ✗ 抓取异常', p.name, e.message); saveJson(p.name, { page: p.name, err: e.message }) }
    }
    await ctx.storageState({ path: STATE }).catch(() => {})
  } finally {
    await browser.close()
    log('完成 →', OUT)
  }
})().catch(e => { console.error(e); process.exit(1) })
