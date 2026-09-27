/* ql361 单据打印模板探测（只读！）
 *
 * 用户给定的取法：打开**有数据的**单据表单 → 点「打印」→ 弹出打印模板选择弹窗 →
 *   弹窗**右上角**「模板设置」→ 模板编辑页。
 * 空白表单点打印会被阻止，所以必须挑一张有数据的单据。
 *
 * ⚠️ 铁律：本脚本**只点** 打印 / 模板设置 / 关闭 这类只读入口，
 *    绝不编辑字段、绝不保存、绝不提交 —— 对标数据不可改。
 *
 * 运行： NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/ql361-print-template-probe.cjs <域> [页面名]
 *   例： node tools/ql361-print-template-probe.cjs 销售 销售出库单
 * 输出： tool-results/ql361/print-template/<页面名>/ 下 截图 + 弹窗DOM + API响应(fetch/xhr)
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const { loginQl361 } = require('./ql361-lib.cjs')

const DOMAIN = process.argv[2] || '销售'
const PAGE = process.argv[3] || ''
const OUT = path.resolve(__dirname, `../tool-results/ql361/print-template/${PAGE || DOMAIN}`)
fs.mkdirSync(OUT, { recursive: true })

const log = (...a) => console.log('[probe]', ...a)
const shot = (page, n) => page.screenshot({ path: path.join(OUT, `${n}.png`), fullPage: false }).catch(() => {})

/** 记录所有 API 响应（模板 JSON 很可能只在响应体里，DOM 里拿不到） */
function wireApiLog(page, sink) {
  page.on('response', async (res) => {
    const url = res.url()
    if (!/\.(js|css|png|jpg|svg|woff2?)($|\?)/i.test(url) && /api|ashx|json|Print|print|Template|template/i.test(url)) {
      let body = ''
      // 模板 JSON 就在 getprintcfgs 的响应体里，不能截断（20k 会把内容切掉）
      try { body = (await res.text()).slice(0, 800000) } catch { /* ignore */ }
      sink.push({ url, status: res.status(), body })
    }
  })
}

async function main() {
  const browser = await chromium.launch({ headless: false, args: ['--window-size=1920,1000'] })
  const STATE = path.resolve(__dirname, '../tool-results/ql361/product/ql361-state.json')
  const ctx = await browser.newContext({
    viewport: { width: 1920, height: 1000 },
    storageState: fs.existsSync(STATE) ? STATE : undefined,
  })
  const page = await ctx.newPage()
  const api = []
  wireApiLog(page, api)

  // ⚠️ 「模板设置」是 **window.open 新开标签页**加载 Vue 打印设计器
  //   （vue/web.print/dist/index.html?billtype=&billid=&templateid=…）。
  //   直接访问该 URL 会一直卡在 loading —— 它依赖父窗口上下文。
  //   所以必须捕获新页面，在它上面同样挂响应监听（模板 JSON 就在新页的请求里）。
  ctx.on('page', async (p) => {
    log('↗ 新开标签页:', p.url().slice(0, 140))
    wireApiLog(p, api)
    await p.waitForLoadState('domcontentloaded').catch(() => {})
    await p.waitForTimeout(12000)
    await p.screenshot({ path: path.join(OUT, '5-设计器新页.png'), fullPage: false }).catch(() => {})
  })

  // ⚠️ 工作台域名与登录页不同：登录页 www.ql361.com，实际操作台是 22stable.ql361.com/desktop.html
  await page.goto('https://22stable.ql361.com/desktop.html', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(4000)
  if (!(await page.evaluate(() => (document.body.innerText || '').includes('欢迎您')))) {
    log('登录态失效，重新登录…')
    await loginQl361(page, ctx, log)
    await page.waitForTimeout(3000)
    await page.goto('https://22stable.ql361.com/desktop.html', { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(4000)
  }
  log('工作台就绪:', page.url())

  // ── 打开一级菜单 ──
  const anchors = page.locator('a.menu-0-item')
  const n = await anchors.count()
  const names = []
  for (let i = 0; i < n; i++) names.push((await anchors.nth(i).innerText().catch(() => '')).trim())
  log('一级菜单:', names.join(' | '))
  const idx = names.indexOf(DOMAIN)
  if (idx < 0) { log(`✗ 未找到域「${DOMAIN}」`); await browser.close(); return }
  await anchors.nth(idx).click()
  await page.waitForTimeout(1500)

  // ── 列出该域的二级页面项，供人工确认页面名 ──
  const items = await page.evaluate(() => [...document.querySelectorAll('.popupmenu__list-item')]
    .filter(e => e.getBoundingClientRect().width > 0)
    .map(e => (e.innerText || '').trim()).filter(Boolean))
  log('二级项:', items.slice(0, 60).join(' | '))
  fs.writeFileSync(path.join(OUT, '_menu.json'), JSON.stringify({ domain: DOMAIN, items }, null, 2), 'utf8')

  if (!PAGE) { log('未指定页面名，只导出菜单'); await browser.close(); return }
  // 菜单项文本带换行后缀（如「销售出库单\n历史」），用前缀匹配
  if (!items.some(i => i.replace(/\s+/g, '').startsWith(PAGE))) {
    log(`✗ 菜单里没有「${PAGE}」`); await browser.close(); return
  }

  // ── 用户明确的流程：点「销售出库单 **历史**」标签直接进列表 ──
  // ⚠️ 千万不要点菜单名本身 —— 那进的是**新建空白表单**，空白单据点打印不弹模板窗（前几轮就栽在这）。
  const menu = page.locator('.popupmenu__repeat')
  const item = page.locator('.popupmenu__list-item').filter({ hasText: PAGE }).first()
  await item.hover().catch(() => {})
  await page.waitForTimeout(1500)
  let hist = menu.locator('text=历史').first()
  if (!(await hist.count().catch(() => 0))) {
    // 悬停没露出标签就点开菜单项再找
    await item.click().catch(() => {})
    await page.waitForTimeout(1800)
    hist = menu.locator('text=历史').first()
  }
  const hasHist = (await hist.count().catch(() => 0)) > 0
  if (hasHist) await hist.click({ timeout: 8000 }).catch(() => {})
  log('点「历史」标签:', hasHist)
  await page.waitForTimeout(6500)
  await shot(page, '1b-列表')

  // 判据：确认**真的进了列表**（出现单据号前缀 / 分页 / 共 N 条），否则重试点历史
  const listOk = await page.evaluate(() => {
    const t = document.body.innerText || ''
    return /XSCKD|共\s*\d+\s*[条行]|首页|上一页|下一页/.test(t)
  }).catch(() => false)
  log('列表已加载:', listOk)
  if (!listOk && hasHist) {
    await hist.click({ timeout: 8000 }).catch(() => {})
    await page.waitForTimeout(6000)
    await shot(page, '1c-列表重试')
  }

  // ── 先看清列表里有什么（行容器 class + 首行文本），据此挑一张有数据的单据 ──
  const grid = await page.evaluate(() => {
    const cands = ['.lkgrid-row', 'tr[class*=row]', '.dojoxGridRow', '.lkgrid-body tr', '[class*=grid-row]']
    const out = []
    for (const sel of cands) {
      const els = [...document.querySelectorAll(sel)].filter(e => e.getBoundingClientRect().height > 0)
      if (els.length) out.push({ sel, n: els.length, first: (els[0].innerText || '').replace(/\s+/g, ' ').trim().slice(0, 160) })
    }
    return out
  }).catch(() => [])
  log('列表行容器:', JSON.stringify(grid))
  fs.writeFileSync(path.join(OUT, '_列表行.json'), JSON.stringify(grid, null, 2), 'utf8')
  if (!grid.length) { log('✗ 列表里没有可见数据行'); await shot(page, '1b-无数据'); await browser.close(); return }

  // ── 先 dump 列表的真实 DOM 结构，精确定位「单据编号」列（别再猜索引） ──
  const gridDump = await page.evaluate(() => {
    const out = { headerFound: false, headerIndex: -1, headerCls: '', rows: [] }
    const all = [...document.querySelectorAll('*')]
    // 表头里文本恰为「单据编号」的单元格
    const hdr = all.find(e => (e.innerText || '').trim() === '单据编号' && e.getBoundingClientRect().height < 80)
    if (hdr) {
      out.headerFound = true
      out.headerCls = String(hdr.className)
      const p = hdr.parentElement
      if (p && p.parentElement) out.headerIndex = [...p.parentElement.children].indexOf(p)
      out.headerSiblings = p && p.parentElement
        ? [...p.parentElement.children].slice(0, 6).map(c => (c.innerText || '').trim().slice(0, 14)) : []
    }
    // 数据行结构：取前 3 个 grid-row
    const rows = [...document.querySelectorAll('[class*=grid-row]')].slice(0, 3)
    for (const r of rows) {
      out.rows.push({
        cls: String(r.className).slice(0, 80),
        childCount: r.children.length,
        kids: [...r.children].slice(0, 8).map(c => ({
          tag: c.tagName,
          cls: String(c.className).slice(0, 55),
          txt: (c.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 28),
          inp: c.querySelector && c.querySelector('input') ? (c.querySelector('input').value || '').slice(0, 26) : '',
        })),
      })
    }
    return out
  }).catch(() => ({}))
  fs.writeFileSync(path.join(OUT, '_grid结构.json'), JSON.stringify(gridDump, null, 2), 'utf8')
  log('列表结构已 dump → _grid结构.json（表头找到:', gridDump.headerFound, '列索引:', gridDump.headerIndex, '）')

  // ── 用户明确：点击「单据编号」列的单元格打开表单 ──
  // ⚠️ 编号很可能在 <input value> 里（innerText 取不到，所以正则匹配文本会失败）；
  //    先找带该值的输入框；找不到再退回"按列索引点第 1 个数据行的第 3 列"
  //    （列表列序实测：操作 | 单据日期 | **单据编号** | 单据状态 | 仓库 | 客户 | …）
  let picked = ''
  const dataRows = page.locator('tr[class*=dojoDndContainer]')
  const rn = await dataRows.count().catch(() => 0)
  log('数据行数:', rn)
  if (rn) {
    const cells = dataRows.nth(0).locator('td')
    const cn = await cells.count().catch(() => 0)
    log('首行单元格数:', cn)
    // 单据编号是第 3 列（索引 2）；兜底：逐个找"文本或 input 值像单据号"的单元格
    let target = cells.nth(2)
    for (let i = 0; i < Math.min(cn, 10); i++) {
      const t = (await cells.nth(i).innerText().catch(() => '')).trim()
      const v = await cells.nth(i).locator('input').first().inputValue().catch(() => '')
      if (/^[A-Z]{2,6}-?\d{6,}/.test(t) || /^[A-Z]{2,6}-?\d{6,}/.test(v)) {
        target = cells.nth(i); picked = `第${i}格(按编号样式命中)`; break
      }
    }
    if (!picked) picked = '第2格(按列序=单据编号)'
    await target.scrollIntoViewIfNeeded().catch(() => {})
    await target.click({ timeout: 8000 }).catch(() => {})
  }
  log('点击单据编号单元格 →', picked)
  await page.waitForTimeout(8000)
  await shot(page, '2-表单页')
  // 判据要能区分「有数据的表单」与「空白表单」。
  // ⚠️ 单据号在 <input value> 里，**不在 innerText** —— 必须连 input 的值一起看，
  //    否则进了有数据的表单也会判 false（前几次就是这么误判的）。
  const inForm = await page.evaluate(() => {
    const t = document.body.innerText || ''
    const vals = [...document.querySelectorAll('input')].map(i => i.value || '').join(' ')
    const hasNo = /XSCKD-?\d{8}-?\d+/.test(t) || /XSCKD-?\d{8}-?\d+/.test(vals)
    return t.includes('打印(F8)') && t.includes('单据备注') && hasNo
  }).catch(() => false)
  log('进入有数据表单:', inForm)
  if (!inForm) {
    // 兜底：把当前页面的 input 值 dump 出来，便于下一轮精确定位
    const dump = await page.evaluate(() => ({
      inputs: [...document.querySelectorAll('input')].map(i => i.value).filter(Boolean).slice(0, 30),
      head: (document.body.innerText || '').slice(0, 300),
    })).catch(() => ({}))
    fs.writeFileSync(path.join(OUT, '_未进表单.json'), JSON.stringify(dump, null, 2), 'utf8')
    log('  已 dump input 值供排查')
  }

  // ── 点「打印」 ──
  // ⚠️ 按钮文本是「打印(F8)」带快捷键后缀，不能写死等号；用前缀匹配并限长避开长文本容器
  const printed = await page.evaluate(() => {
    const cand = [...document.querySelectorAll('button, a, span, div')]
      .filter(e => {
        const t = (e.innerText || '').trim()
        return e.getBoundingClientRect().width > 0 && /^打印/.test(t) && t.length <= 12
      })
    if (!cand.length) return false
    cand[0].click(); return true
  })
  log('点打印:', printed)
  await page.waitForTimeout(6000)
  await shot(page, '3-打印弹窗')

  // 打印弹窗 DOM（找「模板设置」入口）
  const dlg = await page.evaluate(() => {
    const ds = [...document.querySelectorAll('.dijitDialog, .dojoxDialog, .lkgrid-setting-dialog, [class*=dialog]')]
      .filter(d => d.getBoundingClientRect().width > 0)
    return ds.map(d => ({
      cls: d.className,
      text: (d.innerText || '').slice(0, 1500),
      // 右上角按钮：close 图标 / 设置图标
      corners: [...d.querySelectorAll('*')]
        .filter(e => e.getBoundingClientRect().width > 0 && e.getBoundingClientRect().width < 60)
        .map(e => ({ cls: String(e.className).slice(0, 60), title: e.getAttribute('title') || '', txt: (e.innerText || '').trim().slice(0, 20) }))
        .filter(o => o.title || o.txt).slice(0, 40),
    }))
  }).catch(() => [])
  fs.writeFileSync(path.join(OUT, '_打印弹窗.json'), JSON.stringify(dlg, null, 2), 'utf8')
  log('打印弹窗数:', dlg.length)

  // ── 找并点「模板设置」 ──
  const toTpl = await page.evaluate(() => {
    const all = [...document.querySelectorAll('*')]
      .filter(e => e.getBoundingClientRect().width > 0)
    const hit = all.find(e => ['模板设置', '模版设置', '模板设计'].includes((e.innerText || '').trim()))
    if (!hit) return false
    hit.click(); return true
  })
  log('点模板设置:', toTpl)
  await page.waitForTimeout(7000)
  await shot(page, '4-模板编辑页')
  const tplDom = await page.evaluate(() => (document.body.innerText || '').slice(0, 4000)).catch(() => '')
  fs.writeFileSync(path.join(OUT, '_模板编辑页.txt'), tplDom, 'utf8')

  fs.writeFileSync(path.join(OUT, '_api.json'), JSON.stringify(api, null, 2), 'utf8')
  log('API 响应', api.length, '条 →', path.join(OUT, '_api.json'))
  log('完成 →', OUT)
  await browser.close()
}

main().catch(e => { console.error(e); process.exit(1) })
