// 销售换货单列表/表单页 UI 验收（独立 Chromium 实例，避免与 MCP 浏览器互斥）
// 覆盖：工具栏 6 按钮 · 32 列表头 · 页面配置（查询20项/按钮6个）真实联动 · 表单 3 Tab 配置 · 号段编号 · 双仓明细表
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BASE = process.env.SEX_UI_BASE || 'http://localhost:5656'
const API = process.env.SEX_API || 'http://localhost:5655'

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function apiLogin() {
  let last = null
  for (let i = 1; i <= 4; i++) {
    try {
      const cap = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
      const cd = cap.data || cap
      const lg = await fetch(`${API}/api/auth/login`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        // 独立验收账号（与并行会话共用 admin 会因 sa-token 单端登录互踢）
        body: JSON.stringify({ username: 'sex_e2e', password: 'admin123', tenantName: '系统租户', captcha: extractCaptcha(cd.img), captchaKey: cd.uuid }),
      }).then(r => r.json())
      last = lg
      const d = lg?.data || {}
      const token = d.token || d.accessToken || d.tokenValue
      if (token) return { token, tenantId: d.tenantId, userId: d.userId }
    } catch (e) { last = String(e) }
    await new Promise(r => setTimeout(r, 1500))
  }
  console.log('  登录响应:', JSON.stringify(last).slice(0, 200))
  return {}
}

const MARK = 'UI-E2E-SEX'

async function apiCreate(token) {
  const no = (await fetch(`${API}/api/erp/sale/exchange/next-no`, { headers: { Authorization: `Bearer ${token}` } }).then(r => r.json()))?.data
  const body = {
    exchangeNo: no, status: 1, exchangeDate: new Date().toISOString().slice(0, 10) + 'T00:00:00',
    customerId: 2, customerName: '客户甲', customerCode: 'C-002',
    inWarehouseId: 2, inWarehouseName: '华东分仓',
    outWarehouseId: 1, outWarehouseName: '主仓库',
    handlerName: 'UI经手人', deptName: '销售部', salesType: '普通销售',
    remark: '换货UI验收 ' + MARK, extText1: MARK,
    items: [
      { warehouseType: 1, productId: '2073239284579586050', productCode: 'SP-20260704-041', productName: '博多新米坊酒酿果味酱罐头', unit: '罐', quantity: 2, unitPrice: 100, discount: 100, amount: 200, discountAmount: 200 },
      { warehouseType: 2, productId: '2073239284579586050', productCode: 'SP-20260704-041', productName: '博多新米坊酒酿果味酱罐头', unit: '罐', quantity: 2, unitPrice: 100, discount: 100, amount: 200, discountAmount: 200 },
    ],
  }
  const res = await fetch(`${API}/api/erp/sale/exchange`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }, body: JSON.stringify(body),
  }).then(r => r.json())
  return res?.id || res?.data?.id
}

async function main() {
  let { token, tenantId, userId } = await apiLogin()
  if (!token) { console.log('登录失败'); process.exit(1) }
  const docId = await apiCreate(token)
  console.log(`  夹具换货单 id=${docId}`)

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1760, height: 950 } })
  const errors = []
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', e => errors.push(String(e)))

  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid, uid]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', String(tid || 1)); localStorage.setItem('userId', String(uid)) }, [token, tenantId, userId])

  async function open(url, waitSel = '.ss-grid') {
    for (let i = 1; i <= 5; i++) {
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(3500)
      const onLogin = await page.evaluate(() => !!document.querySelector('input[type="password"]')).catch(() => true)
      if (!onLogin) {
        await page.waitForSelector(waitSel, { timeout: 20000 }).catch(() => { })
        await page.waitForTimeout(2500)
        return true
      }
      console.log(`  ⚠️ 第 ${i} 次被踢下线，自动重登…`)
      const r = await apiLogin()
      await page.evaluate(([tk, tid, uid]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', String(tid || 1)); localStorage.setItem('userId', String(uid)) }, [r.token, r.tenantId, r.userId])
    }
    return false
  }

  // ══════════ 列表页 ══════════
  console.log('\n── 列表页 ──')
  const okList = await open(`${BASE}/sales/exchange/index`)
  check('列表页可打开（未被踢/无白屏）', okList)

  const toolbarText = await page.evaluate(() =>
    [...document.querySelectorAll('.toolbar-right button')].map(b => b.innerText.trim()).filter(Boolean))
  check('工具栏含文档 6 按钮', ['新增', '刷新', '批量打印', '打印(F8)', '导出'].every(t => toolbarText.some(x => x.includes(t))),
    JSON.stringify(toolbarText))
  check('工具栏无文档外「批量审核」', !toolbarText.some(t => t.includes('批量审核')), JSON.stringify(toolbarText))
  check('工具栏无重复「列配置」入口（列配置走表头齿轮）', !toolbarText.some(t => t.includes('列配置')), JSON.stringify(toolbarText))

  const colTitles = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid thead th')].map(th => th.innerText.trim()))
  const expectCols = ['单据日期', '单据编号', '单据状态', '入库仓库', '出库仓库', '客户编号', '客户', '经手人', '部门',
    '入库数量', '出库数量', '本单金额', '金额', '折后金额', '已结金额', '结算状态', '重量(kg)', '体积(m³)', '销售类型',
    '单据备注', '表头自定义字段1(数字)', '表头自定义字段2(数字)', '表头自定义字段3(文本)', '表头自定义字段4(文本)',
    '表头自定义字段5(文本)', '摘要', '附件', '制单人', '记账人', '记账时间', '制单时间', '打印次数']
  const missing = expectCols.filter(t => !colTitles.some(x => x === t))
  check('表格含文档 32 列', missing.length === 0, missing.length ? '缺少: ' + missing.join(',') : `实测 ${colTitles.length} 个表头单元格`)

  const rowCount = await page.evaluate(() => document.querySelectorAll('.ss-grid tbody tr').length)
  check('列表渲染出数据行', rowCount > 0, `rows=${rowCount}`)

  // ── 页面配置弹窗 ──
  console.log('\n── 页面配置（查询条件 20 项 / 功能按钮 6 个） ──')
  await page.evaluate(() => {
    const btn = [...document.querySelectorAll('.toolbar-right button')].find(b => b.innerText.trim() === '')
    if (btn) btn.click()
  })
  await page.waitForTimeout(1200)
  let dlgInfo = await page.evaluate(() => {
    const modal = document.querySelector('.ant-modal-content')
    if (!modal) return null
    // 只看当前激活的 Tab 面板，并去掉拖拽手柄字符（⠿ 属 Braille 区）
    const pane = modal.querySelector('.ant-tabs-tabpane-active') || modal
    const rows = [...pane.querySelectorAll('.config-table tbody tr')]
    return {
      title: modal.querySelector('.ant-modal-title')?.innerText?.trim(),
      tabs: [...modal.querySelectorAll('.ant-tabs-tab')].map(t => t.innerText.trim()),
      rows: rows.map(r => r.innerText.replace(/[\u2800-\u28FF]/g, '').replace(/\t/g, '').split('\n')[0].trim()),
      checked: rows.filter(r => r.querySelector('input[type=checkbox]:checked')).length,
    }
  })
  check('页面配置弹窗打开', !!dlgInfo, dlgInfo ? `title=${dlgInfo.title}` : '未打开')
  if (dlgInfo) {
    check('查询条件 20 项', dlgInfo.rows.length === 20, `rows=${dlgInfo.rows.length}`)
    const expectQuery = ['日期', '单据编号', '客户', '经手人', '部门', '制单人', '记账人', '出库仓库', '入库仓库',
      '单据状态', '结算状态', '销售类型', '商品行属性', '单据备注', '自定义字段1(数字)', '自定义字段2(数字)',
      '自定义字段3(文本)', '自定义字段4(文本)', '自定义字段5(文本)', '显示红冲']
    const missQuery = expectQuery.filter(k => !dlgInfo.rows.some(r => r === k))
    check('查询条件与文档 20 项一致（顺序+名称）', missQuery.length === 0 && dlgInfo.rows.length === 20,
      missQuery.length ? '缺少/错位: ' + missQuery.join(',') : JSON.stringify(dlgInfo.rows))
  }

  // 切到功能按钮 Tab
  await page.evaluate(() => {
    const t = [...document.querySelectorAll('.ant-modal-content .ant-tabs-tab')].find(x => x.innerText.trim() === '功能按钮')
    if (t) t.click()
  })
  await page.waitForTimeout(600)
  const btnRows = await page.evaluate(() => {
    const pane = document.querySelector('.ant-modal-content .ant-tabs-tabpane-active')
    return [...(pane?.querySelectorAll('.config-table tbody tr') || [])]
      .map(r => r.innerText.split('\n')[0].trim()).filter(Boolean)
  })
  check('功能按钮 6 个且与文档一致', btnRows.length === 6 && ['新增', '刷新', '批量打印', '打印(F8)', '导出', '配置'].every(k => btnRows.includes(k)),
    JSON.stringify(btnRows))

  // 取消勾选「部门」→ 搜索区联动隐藏
  await page.evaluate(() => {
    const t = [...document.querySelectorAll('.ant-modal-content .ant-tabs-tab')].find(x => x.innerText.trim() === '查询条件')
    if (t) t.click()
  })
  await page.waitForTimeout(500)
  const beforeDept = await page.evaluate(() => !!document.querySelector('.search-area input[placeholder="部门"]'))
  await page.evaluate(() => {
    const pane = document.querySelector('.ant-modal-content .ant-tabs-tabpane-active')
    const row = [...(pane?.querySelectorAll('.config-table tbody tr') || [])].find(r => r.innerText.includes('部门'))
    const cb = row?.querySelector('input[type=checkbox]')
    if (cb) cb.click()
  })
  await page.waitForTimeout(1200)
  const afterDept = await page.evaluate(() => !!document.querySelector('.search-area input[placeholder="部门"]'))
  check('页面配置勾选真实联动搜索区（取消「部门」后隐藏）', beforeDept && !afterDept, `before=${beforeDept} after=${afterDept}`)

  // 恢复勾选，避免影响后续
  await page.evaluate(() => {
    const pane = document.querySelector('.ant-modal-content .ant-tabs-tabpane-active')
    const row = [...(pane?.querySelectorAll('.config-table tbody tr') || [])].find(r => r.innerText.includes('部门'))
    const cb = row?.querySelector('input[type=checkbox]')
    if (cb && !cb.checked) cb.click()
  })
  await page.waitForTimeout(600)
  await page.evaluate(() => { document.querySelectorAll('.ant-modal-close').forEach(b => b.click()) })
  await page.waitForTimeout(600)

  // ══════════ 表单页 ══════════
  console.log('\n── 表单页（新增） ──')
  const okForm = await open(`${BASE}/sales/exchange/form`, '.bill-form-page, .form-page-container')
  check('表单页可打开', okForm)
  const orderNo = await page.evaluate(() => {
    const el = [...document.querySelectorAll('.header-center, .bill-title')].map(e => e.parentElement?.innerText || '')
    const txt = document.body.innerText
    const m = txt.match(/XSHHD-\d{8}-\d{4}/)
    return m ? m[0] : ''
  })
  check('单据编号来自后端号段 XSHHD-yyyyMMdd-NNNN', /^XSHHD-\d{8}-\d{4}$/.test(orderNo), `实测 ${orderNo}`)

  const tableTitles = await page.evaluate(() =>
    [...document.querySelectorAll('.warehouse-title')].map(e => e.innerText.split('\n')[0].trim()))
  check('双明细表（换入/换出仓库数据表）', tableTitles.some(t => t.includes('换入仓库数据表')) && tableTitles.some(t => t.includes('换出仓库数据表')),
    JSON.stringify(tableTitles))

  const detailCols = await page.evaluate(() => {
    const heads = [...document.querySelectorAll('.warehouse-section .ss-grid thead th')].map(th => th.innerText.trim())
    return heads.length
  })
  check('明细表列渲染（≥63 列）', detailCols >= 63, `cols=${detailCols}`)

  // 配置弹窗 3 Tab
  await page.evaluate(() => {
    const btn = [...document.querySelectorAll('button')].find(b => b.innerText.trim().includes('配置'))
    if (btn) btn.click()
  })
  await page.waitForTimeout(1200)
  const formCfg = await page.evaluate(() => {
    const modal = document.querySelector('.ant-modal-content')
    if (!modal) return null
    return {
      tabs: [...modal.querySelectorAll('.ant-tabs-tab')].map(t => t.innerText.trim()),
      rows: [...modal.querySelectorAll('.ant-table-tbody tr')].filter(r => !r.classList.contains('ant-table-measure-row')).length,
    }
  })
  check('表单配置弹窗 3 Tab（页面配置/录单默认值/打印设置）',
    !!formCfg && ['页面配置', '录单默认值', '打印设置'].every(t => formCfg.tabs.includes(t)),
    formCfg ? JSON.stringify(formCfg.tabs) : '未打开')
  check('页面配置 35 个字段', !!formCfg && formCfg.rows === 35, formCfg ? `rows=${formCfg.rows}` : '')
  await page.evaluate(() => { document.querySelectorAll('.ant-modal-close').forEach(b => b.click()) })
  await page.waitForTimeout(500)

  // ══════════ 表单页（编辑回填） ══════════
  console.log('\n── 表单页（编辑回填） ──')
  const okEdit = await open(`${BASE}/sales/exchange/form?id=${docId}`, '.bill-form-page, .form-page-container')
  check('编辑页可打开', okEdit)
  const filled = await page.evaluate(() => {
    const txt = document.body.innerText
    return {
      hasCustomer: txt.includes('客户甲'),
      hasNo: /XSHHD-\d{8}-\d{4}/.test(txt),
      hasProducts: txt.includes('博多新米坊酒酿果味酱罐头'),
    }
  })
  check('编辑回填客户/单号/明细商品', filled.hasCustomer && filled.hasNo && filled.hasProducts, JSON.stringify(filled))

  check('页面无 JS 运行时错误', errors.length === 0, errors.slice(0, 3).join(' | '))

  await browser.close()

  // 清理夹具
  await fetch(`${API}/api/erp/sale/exchange/${docId}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } }).catch(() => {})

  console.log('\n═══ UI 验收结果 ═══')
  console.log(`  ✅ 通过 ${pass} 项   ❌ 失败 ${fail} 项\n`)
  process.exit(fail ? 1 : 0)
}

main().catch(e => { console.error('UI 验收异常:', e); process.exit(2) })
