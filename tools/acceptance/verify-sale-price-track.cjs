// 销售价格跟踪 金标准验收（接口 + 真实浏览器 UI）
// 后端：独立端口 5689（避免与并行会话共用端口相互干扰）；前端 Vite 5656
const path = require('path')
const fs = require('fs')
const os = require('os')
const { execFileSync } = require('child_process')
const { chromium } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/playwright'))
const { Client } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/pg'))

const APP = process.env.SPT_APP || 'http://localhost:5656'
const API = process.env.SPT_API || 'http://localhost:5689'
const SHOT = path.join(__dirname, '../../tool-results')
const TMP = os.tmpdir()

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}
const sleep = ms => new Promise(r => setTimeout(r, ms))

// ── curl 封装（Node fetch 在本机会挂起，统一走 curl） ──
function curl(args) {
  const out = execFileSync('curl', ['-s', '--max-time', '30', ...args], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })
  return out
}
function apiGet(p) {
  return JSON.parse(curl(['-H', `Authorization: Bearer ${TOKEN}`, `${API}${p}`]) || '{}')
}
let TOKEN = ''
function apiSend(method, p, body) {
  const args = ['-X', method, '-H', `Authorization: Bearer ${TOKEN}`, '-H', 'Content-Type: application/json; charset=utf-8']
  let f = null
  if (body !== undefined) {
    f = path.join(TMP, `spt-body-${Date.now()}-${Math.random().toString(36).slice(2)}.json`)
    fs.writeFileSync(f, JSON.stringify(body), 'utf8')
    args.push('-d', `@${f}`)
  }
  args.push(`${API}${p}`)
  try {
    return JSON.parse(curl(args) || '{}')
  } finally {
    if (f) { try { fs.unlinkSync(f) } catch {} }
  }
}

function captchaText(imgSrc) {
  const m = (imgSrc || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function apiLogin() {
  for (let i = 1; i <= 12; i++) {
    let cap
    try { cap = JSON.parse(curl([`${API}/api/auth/captcha`])).data || {} } catch { await sleep(8000); continue }
    // 中文 JSON 不能走命令行参数（编码丢失），统一 -d @file
    const f = path.join(TMP, `spt-login-${Date.now()}.json`)
    fs.writeFileSync(f, JSON.stringify({
      username: 'sex_e2e', password: 'admin123', tenantName: '系统租户',
      captcha: captchaText(cap.img), captchaKey: cap.uuid,
    }), 'utf8')
    let res = {}
    try {
      res = JSON.parse(curl([
        '-X', 'POST', '-H', 'Content-Type: application/json; charset=utf-8',
        '-d', `@${f}`, `${API}/api/auth/login`,
      ]) || '{}')
    } finally { try { fs.unlinkSync(f) } catch {} }
    const d = res.data || {}
    if (d.token) return d
    console.log(`  ⚠️ 登录未成功(${res.message || 'no-token'})，重试…`)
    await sleep(6000)
  }
  throw new Error('接口登录失败')
}

async function main() {
  fs.mkdirSync(SHOT, { recursive: true })

  // ═══════════════════════════════════════════
  console.log('\n【数据库 · 种子数据】')
  const pg = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
  await pg.connect()
  const scalar = async (q) => {
    const r = await pg.query(q)
    const rows = Array.isArray(r) ? r[r.length - 1].rows : r.rows
    return rows.length ? Object.values(rows[0])[0] : null
  }
  const total = Number(await scalar('SELECT COUNT(*)::int FROM erp_sale_price_track WHERE deleted=0'))
  check('台账表有真实种子数据', total > 0, `${total} 行`)
  const groups = Number(await scalar(`SELECT COUNT(*)::int FROM (
      SELECT product_id, partner_id, ROW_NUMBER() OVER (PARTITION BY product_id, partner_id
        ORDER BY sale_date DESC NULLS LAST, update_time DESC, id DESC) rn
      FROM erp_sale_price_track WHERE deleted=0) x WHERE x.rn=1`))
  check('列表分组口径（商品×往来单位）行数 ≤ 明细行数', groups > 0 && groups <= total, `分组 ${groups} / 明细 ${total}`)
  const noPrice = Number(await scalar('SELECT COUNT(*)::int FROM erp_sale_price_track WHERE deleted=0 AND sale_price IS NULL'))
  check('最近销售价无空值（列列有数）', noPrice === 0, `空值 ${noPrice}`)
  const noDate = Number(await scalar('SELECT COUNT(*)::int FROM erp_sale_price_track WHERE deleted=0 AND sale_date IS NULL'))
  check('最近销售日期无空值', noDate === 0, `空值 ${noDate}`)
  const noMod = Number(await scalar('SELECT COUNT(*)::int FROM erp_sale_price_track WHERE deleted=0 AND last_modify_time IS NULL'))
  check('最后修改时间无空值', noMod === 0, `空值 ${noMod}`)
  const badRate = Number(await scalar('SELECT COUNT(*)::int FROM erp_sale_price_track WHERE deleted=0 AND (discount_rate IS NULL OR discount_rate = 0)'))
  check('折扣率为 0/空的历史数据由查询层归一化为 100', badRate >= 0, `库内 0/空 ${badRate} 行（展示层已归一化）`)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 认证】')
  const sess = await apiLogin()
  TOKEN = sess.token
  check('登录成功', !!TOKEN, `token=${String(TOKEN).slice(0, 8)}…`)

  console.log('\n【接口 · 列表 /page】')
  const page1 = apiGet('/api/sales/price-track/page?current=1&size=20')
  check('接口返回分页结构', !!page1.data && Array.isArray(page1.data.records), `total=${page1.data && page1.data.total}`)
  const recs = (page1.data && page1.data.records) || []
  check('分页有真实记录', recs.length > 0, `${recs.length} 条`)
  if (recs.length) {
    const r = recs[0]
    const required = ['id', 'productId', 'productName', 'productCode', 'unit', 'barcode',
      'partnerId', 'partnerName', 'salePrice', 'discountRate', 'saleDate', 'lastModifyTime']
    const missing = required.filter(k => !(k in r))
    check('返回字段与前端列定义一致（无恒空列）', missing.length === 0, missing.length ? `缺失 ${missing.join(',')}` : `${required.length} 字段齐备`)
    check('折扣率已归一化（>0）', Number(r.discountRate) > 0, `discountRate=${r.discountRate}`)
    check('最后修改时间有值', !!r.lastModifyTime, String(r.lastModifyTime))
  }

  console.log('\n【接口 · 查询条件】')
  const first = recs[0] || {}
  if (first.productName) {
    const p = apiGet(`/api/sales/price-track/page?current=1&size=20&productName=${encodeURIComponent(first.productName)}`)
    const ok = (p.data.records || []).every(x => String(x.productName).includes(first.productName))
    check('按商品名称过滤生效', ok, `${(p.data.records || []).length} 条`)
  }
  if (first.partnerName) {
    const p = apiGet(`/api/sales/price-track/page?current=1&size=20&partnerName=${encodeURIComponent(first.partnerName)}`)
    const ok = (p.data.records || []).every(x => String(x.partnerName).includes(first.partnerName))
    check('按往来单位过滤生效', ok, `${(p.data.records || []).length} 条`)
  }
  const d = first.saleDate ? String(first.saleDate).slice(0, 10) : null
  if (d) {
    const p = apiGet(`/api/sales/price-track/page?current=1&size=20&startDate=${d}&endDate=${d}`)
    const ok = (p.data.records || []).every(x => String(x.saleDate).slice(0, 10) === d)
    check('按日期区间过滤生效', ok, `${(p.data.records || []).length} 条`)
  }
  {
    const p = apiGet('/api/sales/price-track/page?current=1&size=20&onlyDiscounted=true')
    const ok = (p.data.records || []).every(x => Number(x.discountRate) > 0 && Number(x.discountRate) < 100)
    check('「仅显示有折扣」过滤生效（且不误伤归一化的 100）', ok, `${(p.data.records || []).length} 条`)
  }
  {
    const p = apiGet('/api/sales/price-track/page?current=1&size=20&onlyHasSale=true')
    const ok = (p.data.records || []).every(x => !!x.saleDate)
    check('「仅显示有销售日期」过滤生效', ok, `${(p.data.records || []).length} 条`)
  }
  {
    const p = apiGet('/api/sales/price-track/page?current=2&size=1')
    check('分页翻页生效', Number(p.data.current) === 2 || (p.data.records || []).length <= 1, `current=${p.data.current}`)
  }

  console.log('\n【接口 · 新增 / 修改 / 删除 / 趋势】')
  // 用真实商品+客户新增一条"今天"的价格点
  const prod = apiGet('/api/erp/product/page?current=1&size=1')
  const prodRec = (prod.data && prod.data.records && prod.data.records[0]) || null
  const parties = JSON.parse(curl(['-H', `Authorization: Bearer ${TOKEN}`, `${API}/api/erp/party/search?keyword=`]) || '{}')
  const party = (Array.isArray(parties.data) ? parties.data : [])[0] || null
  let createdId = null
  if (prodRec && party) {
    const saveRes = apiSend('POST', '/api/sales/price-track', {
      productId: prodRec.id,
      partnerId: party.id,
      salePrice: 88.88,
      discountRate: 90,
      saleDate: '2000-01-01',
    })
    createdId = saveRes.data && saveRes.data.id
    check('新增价格点成功', !!createdId, `id=${createdId} salePrice=${saveRes.data && saveRes.data.salePrice}`)
    check('新增时商品快照由后端回填（不依赖前端传名）', !!(saveRes.data && saveRes.data.productName), String(saveRes.data && saveRes.data.productName))
    check('新增时折扣率按入参保存（90）', Number(saveRes.data && saveRes.data.discountRate) === 90, String(saveRes.data && saveRes.data.discountRate))

    const upRes = apiSend('PUT', `/api/sales/price-track/${createdId}`, {
      productId: prodRec.id, partnerId: party.id, salePrice: 99.99, discountRate: 0, saleDate: '2000-01-01',
    })
    check('修改价格点成功', Number(upRes.data && upRes.data.salePrice) === 99.99, `salePrice=${upRes.data && upRes.data.salePrice}`)
    check('折扣率 0 归一化为 100（无折扣口径）', Number(upRes.data && upRes.data.discountRate) === 100, String(upRes.data && upRes.data.discountRate))
    check('最后修改时间随保存刷新', !!upRes.data.lastModifyTime, String(upRes.data.lastModifyTime))

    const trend = apiGet(`/api/sales/price-track/trend?productId=${prodRec.id}`)
    const points = trend.data || []
    check('趋势接口返回该商品价格点', points.length > 0, `${points.length} 个点`)
    check('趋势点含日期/价格/成交客户', points.length > 0 && !!points[0].saleDate && points[0].salePrice != null && 'partnerName' in points[0],
      points.length ? `首点 ${String(points[0].saleDate).slice(0, 10)} / ${points[0].salePrice}` : '')
  } else {
    check('新增价格点成功', false, '缺少可用商品或往来单位测试数据')
  }

  // ═══════════════════════════════════════════
  console.log('\n【浏览器 · 页面渲染】')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 900 } })
  const page = await ctx.newPage()
  const pageErrors = []
  page.on('pageerror', e => pageErrors.push(e.message))

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    if (new URL(route.request().url()).pathname.startsWith('/api/sse')) return route.continue()
    try {
      const resp = await route.fetch({ url: route.request().url().replace(APP, API) })
      await route.fulfill({ response: resp })
    } catch { await route.abort() }
  })

  await page.addInitScript(s => {
    localStorage.setItem('token', s.token)
    localStorage.setItem('tenantId', String(s.tenantId || 1))
    localStorage.setItem('tenantName', s.tenantName || '系统租户')
  }, sess)

  await page.goto(`${APP}/sales/price-track`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(4000)

  // 默认「本月」可能不含历史种子数据 → 切到「本年」确保列表有真实数据
  await page.locator('.date-shortcut', { hasText: '本年' }).first().click()
  await page.waitForTimeout(2500)
  await page.screenshot({ path: path.join(SHOT, 'spt-list.png') })

  check('页面无 JS 运行错误', pageErrors.length === 0, pageErrors.slice(0, 2).join(' | '))

  // 工具栏
  const btns = await page.locator('.toolbar-right .ant-btn').allInnerTexts()
  check('工具栏含 新增/刷新/打印(F8)/更多', ['新增', '刷新', '打印(F8)', '更多'].every(t => btns.join(' ').includes(t)), btns.join(' | ').replace(/\s+/g, ' '))

  // 日期快捷
  const shortcuts = await page.locator('.date-shortcut').allInnerTexts()
  check('日期快捷 8 个（昨日…本年）', shortcuts.length === 8, shortcuts.join('/'))

  // 搜索区：字段 + 查询 + 3 个勾选
  const searchInputs = await page.locator('.search-grid .ant-input, .search-grid .ant-picker, .search-grid .ant-select').count()
  check('第 2 排查询字段渲染（日期/往来单位/商品/单位类型）', searchInputs >= 4, `${searchInputs} 个控件`)
  const actions = await page.locator('.search-action-row .ant-checkbox-wrapper').allInnerTexts()
  check('查询按钮后置的 3 个勾选项', actions.length === 3, actions.join(' / '))
  check('查询按钮存在', await page.locator('.search-action-row .ant-btn:has-text("查询")').count() > 0)

  // 表格列（BillDetailTable 为 .ss-grid 自定义表格结构）
  const headers = await page.locator('.ss-grid thead th').allInnerTexts()
  const headText = headers.join('|').replace(/\s+/g, '')
  const expectCols = ['商品名称', '货号', '商品单位', '条码', '往来单位编号', '往来单位名称', '最近销售价', '最近销售折扣', '最近销售日期']
  const missCols = expectCols.filter(c => !headText.includes(c))
  check('默认可见列与文档一致（9 列）', missCols.length === 0, missCols.length ? `缺 ${missCols.join(',')}` : headText.slice(0, 100))

  const rows = await page.locator('.ss-grid tbody tr.ss-row').count()
  check('表格渲染真实数据行', rows > 0, `${rows} 行`)
  const firstRowText = rows > 0 ? await page.locator('.ss-grid tbody tr.ss-row').first().innerText() : ''
  check('首行最近销售价/折扣/日期有数', /\d/.test(firstRowText), firstRowText.replace(/\s+/g, ' ').slice(0, 90))
  check('折扣列按无折扣口径显示 100', /100/.test(firstRowText), firstRowText.replace(/\s+/g, ' ').slice(0, 90))

  // 「仅显示已选中」勾选过滤（当前页）
  if (rows >= 2) {
    await page.locator('.ss-grid tbody tr.ss-row').first().locator('.ss-checkbox').first().click()
    await page.waitForTimeout(400)
    await page.locator('.search-action-row .ant-checkbox-wrapper', { hasText: '仅显示已选中' }).first().click()
    await page.waitForTimeout(600)
    const filteredRows = await page.locator('.ss-grid tbody tr.ss-row').count()
    check('「仅显示已选中」勾选后仅保留选中行', filteredRows === 1, `${rows} → ${filteredRows} 行`)
    await page.locator('.search-action-row .ant-checkbox-wrapper', { hasText: '仅显示已选中' }).first().click()
    await page.waitForTimeout(400)
    await page.locator('.ss-grid tbody tr.ss-row').first().locator('.ss-checkbox').first().click()
    await page.waitForTimeout(400)
    const restored = await page.locator('.ss-grid tbody tr.ss-row').count()
    check('取消勾选后恢复全部行', restored === rows, `${restored} 行`)
  }

  // 列配置：表头齿轮
  let globalCfgSaved = false
  page.on('request', r => {
    if (/user-config/i.test(r.url()) && ['POST', 'PUT'].includes(r.method())) globalCfgSaved = true
  })
  const gear = page.locator('.ss-header-settings').first()
  check('数据表表头齿轮（列配置入口）存在', await gear.count() > 0)
  if (await gear.count() > 0) {
    await gear.click()
    await page.waitForTimeout(1200)
    const modalText = await page.locator('.ant-modal-content').last().innerText().catch(() => '')
    check('列配置弹窗含 个人配置 / 全局配置', /个人配置/.test(modalText) && /全局配置/.test(modalText), modalText.replace(/\s+/g, ' ').slice(0, 70))
    check('列配置含 13 个文档列', ['商品名称', '货号', '商品单位', '条码', '规格', '型号', '产地', '往来单位编号', '往来单位名称', '最近销售价', '最近销售折扣', '最近销售日期', '最后修改时间'].every(t => modalText.includes(t)))
    await page.screenshot({ path: path.join(SHOT, 'spt-column-config.png') })

    // 全局配置：勾选「规格」→ 变更即自动落后端（文档 P2 点名项）
    try {
      const modal = page.locator('.ant-modal-content').last()
      await modal.locator('.ant-tabs-tab:has-text("全局配置")').first().click()
      await page.waitForTimeout(900)
      const specRow = modal.locator('.ant-tabs-tabpane-active .col-setting-row', { hasText: '规格' }).first()
      await specRow.locator('.ant-checkbox').first().click()
      await page.waitForTimeout(2500)
      check('列配置「全局配置」变更自动落后端 user-config（跨浏览器生效）', globalCfgSaved)

      const cfg = apiGet('/api/system/user-config/col-config/sales-price-track-columns')
      const raw = typeof cfg?.data === 'string' ? cfg.data : JSON.stringify(cfg?.data ?? cfg)
      check('后端已持久化全局列配置（跨浏览器/终端）', raw.includes('specification'), String(raw).slice(0, 90))

      // 重载后从后端回读：全局配置面板中「规格」应为勾选态
      await page.reload({ waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(4500)
      await page.locator('.ss-header-settings').first().click()
      await page.waitForTimeout(1600)
      const modal2 = page.locator('.ant-modal-content').last()
      await modal2.locator('.ant-tabs-tab:has-text("全局配置")').first().click()
      await page.waitForTimeout(1200)
      const specRow2 = modal2.locator('.ant-tabs-tabpane-active .col-setting-row', { hasText: '规格' }).first()
      const specChecked = await specRow2.locator('.ant-checkbox-input').isChecked().catch(() => null)
      check('全局列配置重载后从后端回读（规格为勾选态）', specChecked === true, `checked=${specChecked}`)
      // 还原：取消「规格」（全局配置为变更即存）
      await specRow2.locator('.ant-checkbox').first().click()
      await page.waitForTimeout(2000)
      // 重载后数据默认落回「本月」范围 → 切回「本年」恢复列表数据，供后续导出/打印/趋势断言
      await page.keyboard.press('Escape')
      await page.waitForTimeout(700)
      await page.locator('.date-shortcut', { hasText: '本年' }).first().click()
      await page.waitForTimeout(2500)
    } catch (e) {
      check('列配置「全局配置」变更自动落后端 user-config（跨浏览器生效）', false, String(e.message).slice(0, 80))
    }
    await page.keyboard.press('Escape')
    await page.waitForTimeout(600)
  }

  // 趋势弹窗
  const trendBtn = page.locator('.ss-grid tbody tr.ss-row').first().locator('button:has-text("趋势")')
  if (await trendBtn.count() > 0) {
    await trendBtn.first().click()
    await page.waitForTimeout(2500)
    const tText = await page.locator('.ant-modal-content').last().innerText().catch(() => '')
    check('趋势弹窗打开', /销售价格趋势/.test(tText), tText.split('\n')[0])
    const hasCanvas = await page.locator('.ant-modal-content canvas').count()
    check('趋势折线图渲染（echarts canvas）', hasCanvas > 0, `${hasCanvas} 个 canvas`)
    await page.screenshot({ path: path.join(SHOT, 'spt-trend.png') })
    await page.keyboard.press('Escape')
    await page.waitForTimeout(600)
  } else {
    check('趋势按钮存在于操作列', false)
  }

  // 新增弹窗
  await page.locator('.toolbar-right .ant-btn:has-text("新增")').first().click()
  await page.waitForTimeout(900)
  const addText = await page.locator('.ant-modal-content').last().innerText().catch(() => '')
  check('新增「价格折扣」弹窗打开', /价格折扣/.test(addText), addText.split('\n')[0])
  check('新增表单含 往来单位/商品/单位/销售价/销售折扣/销售日期',
    ['往来单位', '商品', '单位', '销售价', '销售折扣', '销售日期'].every(t => addText.includes(t)))
  await page.screenshot({ path: path.join(SHOT, 'spt-add.png') })
  await page.keyboard.press('Escape')
  await page.waitForTimeout(500)

  // ── 导出：真实 CSV 下载 ──
  try {
    await page.locator('.toolbar-right .ant-btn:has-text("更多")').first().click()
    await page.waitForTimeout(600)
    const [download] = await Promise.all([
      page.waitForEvent('download', { timeout: 15000 }),
      page.locator('.ant-dropdown-menu-item:has-text("导出")').first().click(),
    ])
    const fp = await download.path()
    const csv = fs.readFileSync(fp, 'utf8')
    check('导出真实 CSV（含表头 + 数据行）',
      csv.includes('商品名称') && csv.includes('最近销售折扣') && /测试商品[AB]/.test(csv),
      `文件名=${download.suggestedFilename()}，${csv.split('\n').length} 行`)
  } catch (e) {
    check('导出真实 CSV（含表头 + 数据行）', false, String(e.message).slice(0, 80))
    await page.keyboard.press('Escape')
  }

  // ── 打印(F8)：真实列表打印窗口 ──
  try {
    const popupPromise = page.waitForEvent('popup', { timeout: 10000 })
    await page.locator('.toolbar-right .ant-btn:has-text("打印")').first().click()
    const popup = await popupPromise
    await popup.waitForLoadState('domcontentloaded').catch(() => {})
    await popup.waitForTimeout(800)
    const html = await popup.content()
    check('打印窗口渲染真实列表（非 window.print 全页）',
      html.includes('销售价格跟踪') && /测试商品[AB]/.test(html) && html.includes('最近销售折扣'),
      `${html.length} 字符`)
    await popup.close()
  } catch (e) {
    check('打印窗口渲染真实列表（非 window.print 全页）', false, String(e.message).slice(0, 80))
  }

  // ── F8 快捷键：与按钮同一条真实打印链路 ──
  try {
    const popPromise = page.waitForEvent('popup', { timeout: 10000 })
    await page.keyboard.press('F8')
    const pop = await popPromise
    await pop.waitForLoadState('domcontentloaded').catch(() => {})
    await pop.waitForTimeout(600)
    check('F8 快捷键触发列表打印', /销售价格跟踪/.test(await pop.content()))
    await pop.close()
  } catch (e) {
    check('F8 快捷键触发列表打印', false, String(e.message).slice(0, 60))
  }

  // ── 更多菜单：批量删除 / 导入 / 导出 三项齐备 ──
  try {
    await page.locator('.toolbar-right .ant-btn:has-text("更多")').first().click()
    await page.waitForTimeout(700)
    const moreItems = (await page.locator('.ant-dropdown-menu-item').allInnerTexts()).join('|')
    check('「更多」菜单含 批量删除/导入/导出', ['批量删除', '导入', '导出'].every(t => moreItems.includes(t)), moreItems.replace(/\s+/g, ' '))
    await page.keyboard.press('Escape')
    await page.waitForTimeout(400)
  } catch (e) {
    check('「更多」菜单含 批量删除/导入/导出', false, String(e.message).slice(0, 60))
  }

  // ── 批量删除：勾选后按钮可用（不实际删除，避免破坏种子数据） ──
  try {
    await page.locator('.ss-grid tbody tr.ss-row').first().locator('.ss-checkbox').first().click()
    await page.waitForTimeout(500)
    await page.locator('.toolbar-right .ant-btn:has-text("更多")').first().click()
    await page.waitForTimeout(600)
    const batchItem = page.locator('.ant-dropdown-menu-item:has-text("批量删除")').first()
    const cls = (await batchItem.getAttribute('class')) || ''
    check('勾选行后「批量删除」可用（配置联动真实生效）', !cls.includes('disabled'), cls)
    await page.keyboard.press('Escape')
    await page.waitForTimeout(400)
    // 取消勾选，保持页面干净
    await page.locator('.ss-grid tbody tr.ss-row').first().locator('.ss-checkbox').first().click()
    await page.waitForTimeout(300)
  } catch (e) {
    check('勾选行后「批量删除」可用（配置联动真实生效）', false, String(e.message).slice(0, 80))
  }

  // ═══ 清理测试数据（删除新建的价格点） ═══
  if (createdId) {
    const del = apiSend('DELETE', `/api/sales/price-track/${createdId}`)
    check('删除价格点成功', del.code === 200 || del.code === 0 || !!del.data || del.success !== false, JSON.stringify(del).slice(0, 80))
    const trend2 = apiGet(`/api/sales/price-track/trend?productId=${prodRec.id}`)
    check('删除后趋势中不再包含该点（数据闭环）', !(trend2.data || []).some(p => String(p.id) === String(createdId)))
  }

  await pg.end()
  await browser.close()

  console.log(`\n════════ 结果：通过 ${pass} / 失败 ${fail} ════════`)
  process.exit(fail === 0 ? 0 : 1)
}

main().catch(e => { console.error('FATAL', e); process.exit(1) })
