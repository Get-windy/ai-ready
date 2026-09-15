/*
 * 采购订货收货（配发收 → 收货业务 → 采购订货收货，菜单 70161）金标准端到端验证
 *   · API 验收：按单据/按明细双查询口径（应收·已收·未收 / 订货·已收货·待收货）、13/11 查询条件、
 *               批量收货 → 生成《采购入库单》（带待收明细 + 名称快照）
 *   · UI  验收：双 Tab 各自默认列、6 功能按钮、配置驱动查询区、列配置齿轮（个人/全局，各 Tab 独立 storage-key）、
 *               页面配置（按单据 13 / 按明细 11）、经典分页、明细弹窗、批量收货、导出真实 xlsx
 *
 * 前置：执行 tools/e2e-purchase-receive-user.sql（验收账号 + 3 张采购订单/明细/快照/扩展信息基座）
 * 用法：node tools/e2e-purchase-receive.cjs              （默认后端 5655、前端 5656）
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5655)
const SHOTS = 'I:/AI-Ready/tool-results/purchase-receive'
const XLSX = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx')

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

function rawReq(method, reqPath, body, token, extraHeaders) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null || body instanceof Buffer ? body : JSON.stringify(body)
    const headers = {
      ...(body instanceof Buffer ? { 'Content-Type': 'application/octet-stream' } : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {}),
    }
    if (data && !(body instanceof Buffer)) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        resolve({ status: res.statusCode, buf, headers: res.headers, json: tryJson(buf) })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function tryJson(buf) {
  try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null }
}

let TOKEN = null
const E2E_USER = process.env.E2E_USER || 'e2e_purrecv'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

async function api(method, reqPath, body, extraHeaders) {
  let r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  if (r.status === 401 || (r.json && Number(r.json.code) === 401)) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  }
  return r
}

/** 后端 ApiResponse 包装与裸对象两种都兼容 */
function payload(res) {
  const j = res && res.json
  if (!j) return null
  return j.data !== undefined ? j.data : j
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  let token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token && E2E_USER !== 'admin') {
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.json.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', {
      username: 'admin', password: 'admin123', tenantName: '系统租户',
      captcha: code2, captchaKey: cap2.json.data.uuid,
    })
    token = res.json?.data?.token || res.json?.data?.accessToken
  }
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  TOKEN = token
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

const ORD1 = 'E2E-CGD-0001'
const ORD2 = 'E2E-CGD-0002'
const ORD3 = 'E2E-CGD-0003'
const ORDER_ID_1 = '2099000000000000701'
const ORDER_ID_2 = '2099000000000000702'
const ORDER_ID_3 = '2099000000000000703'

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 1) 按单据：应收/已收/未收 + 名称解析 + 结算金额
  const d1 = await api('GET', '/erp/purchase/order/doc-query/page?current=1&size=20&orderNo=E2E-CGD-')
  const rows = payload(d1)?.records || []
  check('按单据查询命中 3 张采购订单', rows.length === 3, `rows=${rows.length}`)
  const r1 = rows.find(r => r.orderNo === ORD1) || {}
  check('应收数量口径（totalQuantity=10）', Number(r1.totalQuantity) === 10, r1.totalQuantity)
  check('已收数量口径（receivedQuantity=4）', Number(r1.receivedQuantity) === 4, r1.receivedQuantity)
  check('未收数量口径（unreceiveQuantity=6）', Number(r1.unreceiveQuantity) === 6, r1.unreceiveQuantity)
  check('供应商名称解析（伙伴快照）', r1.supplierName === 'E2E采购供应商', r1.supplierName)
  check('仓库名称解析', r1.warehouseName === '主仓库', r1.warehouseName)
  check('预计收货日期非空', !!r1.expectedReceiveTime, r1.expectedReceiveTime)
  const r2 = rows.find(r => r.orderNo === ORD2) || {}
  check('待收货单未收数量 = 应收（8）', Number(r2.unreceiveQuantity) === 8, r2.unreceiveQuantity)
  const r3 = rows.find(r => r.orderNo === ORD3) || {}
  check('已收货单未收数量 = 0', Number(r3.unreceiveQuantity) === 0, r3.unreceiveQuantity)
  check('结算金额字段齐备（billAmount/settledAmount）',
    Number(r1.billAmount) === 1000 && Number(r3.settledAmount) === 200, `${r1.billAmount}/${r3.settledAmount}`)

  // 2) 按单据 13 项查询条件抽样
  const cond = async (qs, expect, name) => {
    const r = await api('GET', `/erp/purchase/order/doc-query/page?current=1&size=50&orderNo=E2E-CGD-${qs}`)
    check(name, Number(payload(r)?.total) === expect, JSON.stringify(payload(r)?.total))
  }
  const today = new Date().toISOString().slice(0, 10)
  await cond('&supplierName=E2E采购供应商', 3, '按往来单位过滤')
  await cond('&warehouseName=主仓库', 3, '按仓库过滤')
  await cond('&status=3', 2, '按单据状态过滤（已下达 2 张）')
  await cond(`&dateStart=${today}&dateEnd=${today}`, 3, '按单据日期区间过滤')
  await cond('&extNum1Start=11&extNum1End=11', 1, '按自定义字段1(数字)过滤')
  await cond('&extText1=E2E文本1', 1, '按自定义字段3(文本)过滤')

  // 3) 按明细：订货/已收货/待收货 + 商品扩展
  const d2 = await api('GET', '/erp/purchase/order/detail-query/page?current=1&size=50&orderNo=E2E-CGD-')
  const items = payload(d2)?.records || []
  check('按明细查询命中 4 行', items.length === 4, `rows=${items.length}`)
  const i1 = items.find(i => i.productName === 'E2E采购商品甲' && Number(i.quantity) === 6) || {}
  check('明细订货数量（quantity=6）', Number(i1.quantity) === 6, i1.quantity)
  check('明细已收货数量（receivedQuantity=2）', Number(i1.receivedQuantity) === 2, i1.receivedQuantity)
  check('明细待收货数量（unreceiveQuantity=4）', Number(i1.unreceiveQuantity) === 4, i1.unreceiveQuantity)
  check('明细行带 orderId/itemId（批量收货可用）', !!i1.orderId && !!i1.itemId, `${i1.orderId}/${i1.itemId}`)
  check('明细行带商品扩展（货号/规格/单位）', i1.itemCode === 'E2E-P101' && i1.specification === '规格A' && i1.unit === '件',
    `${i1.itemCode}/${i1.specification}/${i1.unit}`)
  const d2f = await api('GET', '/erp/purchase/order/detail-query/page?current=1&size=50&orderNo=E2E-CGD-&productName=E2E采购商品甲')
  check('按明细按商品过滤命中 2 行', Number(payload(d2f)?.total) === 2, JSON.stringify(payload(d2f)?.total))
  const d2r = await api('GET', '/erp/purchase/order/detail-query/page?current=1&size=50&orderNo=E2E-CGD-&itemRemark=E2E明细备注4')
  check('按明细按明细备注过滤命中 1 行', Number(payload(d2r)?.total) === 1, JSON.stringify(payload(d2r)?.total))

  // 4) 收货闭环：由采购订单生成《采购入库单》（带待收明细 + 名称快照）
  const recv = await api('POST', `/erp/purchase/inbound/from-order/${ORDER_ID_2}`)
  const inbound = payload(recv) || {}
  check('收货生成采购入库单成功', !!inbound.id, JSON.stringify(inbound).slice(0, 200))
  check('入库单挂来源订单（orderId/orderNo）',
    String(inbound.orderId) === ORDER_ID_2 && inbound.orderNo === ORD2, `${inbound.orderId}/${inbound.orderNo}`)
  check('入库单为草稿态（后续在《采购入库单》确认收货/记账）', Number(inbound.status) === 0, inbound.status)
  check('入库单带出供应商名称快照', inbound.supplierName === 'E2E采购供应商', inbound.supplierName)
  check('入库单带出仓库名称快照', inbound.warehouseName === '主仓库', inbound.warehouseName)
  const inItems = payload(await api('GET', `/erp/purchase/inbound/${inbound.id}/items`)) || []
  check('入库单带出待收明细 1 行', inItems.length === 1, `items=${inItems.length}`)
  check('待收明细数量 = 订货 − 已收（8）', Number(inItems[0]?.orderQuantity) === 8, inItems[0]?.orderQuantity)
  check('待收明细商品/单价带出', inItems[0]?.productName === 'E2E采购商品甲' && Number(inItems[0]?.unitPrice) === 100,
    `${inItems[0]?.productName}/${inItems[0]?.unitPrice}`)

  // 4.1) 已全部收完的订单：不产生待收明细（避免生成空入库单）
  const recv3 = await api('POST', `/erp/purchase/inbound/from-order/${ORDER_ID_3}`)
  const inbound3 = payload(recv3) || {}
  const inItems3 = payload(await api('GET', `/erp/purchase/inbound/${inbound3.id}/items`)) || []
  check('已收满订单不产生待收明细（0 行）', inItems3.length === 0, `items=${inItems3.length}`)

  // 4.2) 订单不存在
  const bad = await api('POST', '/erp/purchase/inbound/from-order/999999999')
  check('订单不存在时拒绝收货', bad.json?.code !== 200 || bad.status >= 400, JSON.stringify(bad.json).slice(0, 160))

  // 4.3) 打印次数回写：batch-print 以字符串雪花 ID 透传并递增 ext_info.print_count
  const pcOf = async () => Number(payload(await api(
    'GET', '/erp/purchase/order/doc-query/page?current=1&size=20&orderNo=E2E-CGD-0001'))?.records?.[0]?.printCount)
  const pcBefore = await pcOf()
  const printRes = await api('POST', '/erp/purchase/order/batch-print', { ids: [ORDER_ID_1], template: 'default' })
  check('打印次数回写接口可用（字符串雪花ID）', printRes.status === 200 && printRes.json?.code === 200,
    `${printRes.status}/${printRes.json?.message}`)
  const pcAfter = await pcOf()
  check('打印次数递增 1', pcAfter === pcBefore + 1, `${pcBefore} → ${pcAfter}`)

  // 5) 部分收货订单同样可收货（待收 4+2=6）
  const recv1 = await api('POST', `/erp/purchase/inbound/from-order/${ORDER_ID_1}`)
  const inbound1 = payload(recv1) || {}
  const inItems1 = payload(await api('GET', `/erp/purchase/inbound/${inbound1.id}/items`)) || []
  check('部分收货订单带出 2 行待收明细', inItems1.length === 2, `items=${inItems1.length}`)
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  async function openPage(url, waitMs = 7000) {
    for (let attempt = 0; attempt < 3; attempt++) {
      await login()
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, 1])
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(waitMs)
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }
  const bodyText = async () => (await page.locator('body').innerText()).replace(/\s+/g, '')
  const headers = async () => page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))

  await openPage(`${FE}/dispatch/purchase-receive`)
  const txt = await bodyText()
  check('页面可打开（无 404/白屏）', !txt.includes('页面不存在') && txt.length > 50, page.url())

  // 双 Tab
  check('页面为双 Tab（按单据 / 按明细）', txt.includes('按单据') && txt.includes('按明细'), '')

  // 按单据默认 11 列表头（本系统 10，因无「结算单位」口径）
  const docHeaders = await headers()
  for (const h of ['单据日期', '单据编号', '单据状态', '结算状态', '仓库', '往来单位', '应收数量', '未收数量', '预计收货日期', '打印次数']) {
    check(`按单据默认表头含「${h}」`, docHeaders.some(x => x.includes(h)), docHeaders.join('/'))
  }
  check('按单据默认不含隐藏列（已收数量/订单金额）',
    !docHeaders.some(x => x.includes('已收数量')) && !docHeaders.some(x => x.includes('订单金额')), docHeaders.join('/'))
  // 精确集合断言：默认 10 个数据列 + 操作（对标 11 列，减去本系统无的「结算单位」）
  const DOC_DEFAULT_HEADERS = ['操作', '单据日期', '单据编号', '单据状态', '结算状态', '仓库', '往来单位',
    '应收数量', '未收数量', '预计收货日期', '打印次数']
  check('按单据默认表头 = 对标 10 数据列 + 操作',
    JSON.stringify(docHeaders) === JSON.stringify(DOC_DEFAULT_HEADERS), docHeaders.join('/'))

  // 工具栏按钮（对标 5：刷新/批量收货/打印(F8)/导出/配置）
  for (const b of ['批量收货', '刷新', '打印(F8)', '导出']) {
    check(`工具栏含「${b}」`, txt.includes(b.replace(/\s+/g, '')))
  }
  check('工具栏含「配置」齿轮', (await page.locator('button[title="页面配置"]').count()) > 0)

  // 查询区（按单据 13 项）
  for (const q of ['单据日期', '单据编号', '往来单位', '经手人', '仓库', '单据状态']) {
    check(`查询区含「${q}」`, txt.includes(q))
  }
  check('查询区含「查询 / 重置」', txt.includes('查询') && txt.includes('重置'))

  // 数据行（三种收货进度 + 三种结算状态）
  check('列表含三种收货进度标签', txt.includes('部分收货') && txt.includes('待收货') && txt.includes('已收货'), '')
  check('列表含结算状态标签', txt.includes('未结算') || txt.includes('已结算') || txt.includes('部分结算'), '')
  check('列表显示供应商名称', txt.includes('E2E采购供应商'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-doc.png'), fullPage: true })

  // 自定义数字字段：页面配置开启后，查询区应给出「最小/最大」区间控件，且真实过滤
  {
    await page.locator('button[title="页面配置"]').first().click()
    await page.waitForTimeout(1000)
    const cfg = page.locator('.ant-modal-content:visible').last()
    await cfg.locator('tr:has-text("自定义字段1(数字)") input[type=checkbox]').first().check()
    await page.waitForTimeout(500)
    await cfg.locator('.ant-modal-close').click()
    await page.waitForTimeout(900)

    const rangeInputs = page.locator('.search-item:has-text("自定义字段1(数字)") input')
    const inputCount = await rangeInputs.count()
    check('「自定义字段1(数字)」查询区为区间控件（最小/最大）', inputCount === 2, `inputs=${inputCount}`)
    if (inputCount === 2) {
      await rangeInputs.nth(0).fill('11'); await rangeInputs.nth(0).press('Tab')
      await rangeInputs.nth(1).fill('11'); await rangeInputs.nth(1).press('Tab')
      // antd 两字按钮会在汉字间插入空格（「查 询」），故用正则匹配
      await page.locator('.search-area button').filter({ hasText: /查\s*询/ }).first().click()
      await page.waitForTimeout(2500)
      const filtered = await bodyText()
      check('自定义数字字段区间查询生效（仅命中 E2E-CGD-0001）',
        filtered.includes('E2E-CGD-0001') && !filtered.includes('E2E-CGD-0002'), '')
      // 还原：重置条件 + 关闭该查询字段，避免影响后续断言
      await page.locator('.search-area button').filter({ hasText: /重\s*置/ }).first().click()
      await page.waitForTimeout(1500)
      await page.locator('button[title="页面配置"]').first().click()
      await page.waitForTimeout(1000)
      const cfg2 = page.locator('.ant-modal-content:visible').last()
      await cfg2.locator('tr:has-text("自定义字段1(数字)") input[type=checkbox]').first().uncheck()
      await page.waitForTimeout(500)
      await cfg2.locator('.ant-modal-close').click()
      await page.waitForTimeout(900)
    }
  }

  // 列配置（按单据）
  const gear = await page.locator('.ss-grid .th-settings-btn').count()
  check('数据表带表头齿轮（列配置入口）', gear > 0, `count=${gear}`)
  if (gear > 0) {
    await page.locator('.ss-grid .th-settings-btn').first().click()
    await page.waitForTimeout(1200)
    const colPanel = page.locator('.ant-modal-content:visible').last()
    const colText = (await colPanel.innerText()).replace(/\s+/g, '')
    check('列配置弹窗含「个人配置 / 全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 60))
    const DOC_COLS = ['单据日期', '单据编号', '单据状态', '结算状态', '仓库', '往来编号', '往来单位', '经手人', '部门', '应收数量',
      '已收数量', '未收数量', '订单金额', '预计收货日期', '重量（kg）', '体积（m³）', '单据备注', '表头自定义字段1(数字)',
      '表头自定义字段2(数字)', '表头自定义字段3(文本)', '表头自定义字段4(文本)', '表头自定义字段5(文本)', '摘要', '附件', '制单人', '打印次数']
    const missDoc = DOC_COLS.filter(c => !colText.includes(c))
    check('按单据列配置含对标 26 列（除「结算单位」本系统无该口径）', missDoc.length === 0, '缺失: ' + missDoc.join('、'))
    await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig-doc.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  }

  // 页面配置（按单据 13 项）
  await page.locator('button[title="页面配置"]').first().click()
  await page.waitForTimeout(1200)
  const cfgPanel = page.locator('.ant-modal-content:visible').last()
  const cfgRows = await page.evaluate(() => {
    const m = [...document.querySelectorAll('.ant-modal-content')].filter(e => e.offsetParent !== null).pop()
    const pane = m ? m.querySelector('.ant-tabs-tabpane-active') : null
    return pane ? pane.querySelectorAll('table tbody tr').length : 0
  })
  const cfgText = (await cfgPanel.innerText()).replace(/\s+/g, '')
  check('按单据页面配置查询条件 13 项', cfgRows === 13, `rows=${cfgRows}`)
  await cfgPanel.locator('.ant-tabs-tab:has-text("功能按钮")').click()
  await page.waitForTimeout(600)
  const btnText = (await cfgPanel.innerText()).replace(/\s+/g, '')
  for (const b of ['批量收货', '刷新', '打印(F8)', '导出']) {
    check(`按单据功能按钮含「${b}」`, btnText.includes(b), b)
  }
  await cfgPanel.locator('.ant-modal-close').click()
  await page.waitForTimeout(800)

  // 经典分页
  check('底部为经典分页栏', (await page.locator('.classic-pagination').count()) > 0)

  // 明细弹窗
  await page.locator('.ss-grid a.cell-link').first().click()
  await page.waitForTimeout(2200)
  const detailModal = page.locator('.ant-modal-content:visible').last()
  const detailText = (await detailModal.innerText()).replace(/\s+/g, '')
  check('明细弹窗可打开（订单头 + 数量三口径）',
    detailText.includes('采购订单明细') && detailText.includes('应收数量') && detailText.includes('未收数量'),
    detailText.slice(0, 140))
  await page.screenshot({ path: path.join(SHOTS, 'ui-detail.png'), fullPage: true })
  await detailModal.locator('.ant-modal-close').click()
  await page.waitForTimeout(900)

  // 切到按明细 Tab
  await page.locator('.tab-item:has-text("按明细")').first().click()
  await page.waitForTimeout(3000)
  const detailHeaders = await headers()
  for (const h of ['商品名称', '单位', '订货数量', '待收货数量']) {
    check(`按明细默认表头含「${h}」`, detailHeaders.some(x => x.includes(h)), detailHeaders.join('/'))
  }
  // 精确集合断言：默认 16 个数据列 + 操作。
  // 回归点：切 Tab 时同名列（如按单据隐藏的「已收数量/重量/体积/附件/经手人」）会串到按明细，
  // 修复见 BillDetailTable.loadStoredSettings（切换 storage-key 时必须按当前视图默认重建）。
  const DETAIL_DEFAULT_HEADERS = ['操作', '单据日期', '单据编号', '单据状态', '结算状态', '仓库', '往来单位',
    '经手人', '商品名称', '单位', '订货数量', '已收货数量', '待收货数量', '重量（kg）', '体积（m³）', '明细备注', '附件']
  check('按明细默认表头 = 对标 16 数据列 + 操作（切 Tab 不串列）',
    JSON.stringify(detailHeaders) === JSON.stringify(DETAIL_DEFAULT_HEADERS), detailHeaders.join('/'))
  // 完整列（含视口外的 已收货数量/重量/体积/附件）以列配置弹窗为准
  if (await page.locator('.ss-grid .th-settings-btn').count() > 0) {
    await page.locator('.ss-grid .th-settings-btn').first().click()
    await page.waitForTimeout(1200)
    const detailColText = (await page.locator('.ant-modal-content:visible').last().innerText()).replace(/\s+/g, '')
    const DETAIL_COLS = ['单据日期', '单据编号', '单据状态', '结算状态', '仓库', '往来编号', '往来单位', '经手人', '部门',
      '商品名称', '货号', '条码', '规格', '型号', '产地', '品牌', '单据自定义1(数字字段)', '单据自定义10(部门)', '单位',
      '小单位', '小单位数量', '换算关系', '换算结果', '大包装', '中包装', '小包装', '订货数量', '已收货数量', '待收货数量',
      '单价', '小单位单价', '金额', '优惠折扣(%)', '优惠后单价', '优惠后金额', '重量（kg）', '体积（m³）', '明细备注',
      '单据备注', '摘要', '附件', '制单人', '打印次数']
    const missDetail = DETAIL_COLS.filter(c => !detailColText.includes(c))
    check('按明细列配置含对标 43 列（抽样，覆盖默认 16 列）', missDetail.length === 0, '缺失: ' + missDetail.join('、'))
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  }
  const detailTxt = await bodyText()
  check('按明细显示商品行', detailTxt.includes('E2E采购商品甲'), '')
  check('按明细查询区含「商品 / 明细备注」', detailTxt.includes('商品') && detailTxt.includes('明细备注'))

  // 按明细 Tab 勾选明细行 → 批量收货可用（按 orderId 去重后同样生成《采购入库单》）
  {
    const headCheck = page.locator('.ss-grid thead input[type=checkbox], .ss-grid thead input.ss-checkbox').first()
    if (await headCheck.count()) {
      await headCheck.check({ force: true })
      await page.waitForTimeout(1000)
      const btxt = (await page.locator('button').filter({ hasText: /批量收货/ }).first().innerText()).replace(/\s+/g, '')
      check('按明细勾选明细行后「批量收货」带选中计数', /批量收货\(\d+\)/.test(btxt), btxt)
      await headCheck.uncheck({ force: true })
      await page.waitForTimeout(800)
      const btxt2 = (await page.locator('button').filter({ hasText: /批量收货/ }).first().innerText()).replace(/\s+/g, '')
      check('取消勾选后「批量收货」恢复空态', btxt2 === '批量收货', btxt2)
    }
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-detail-tab.png'), fullPage: true })

  // 按明细页面配置 11 项
  await page.locator('button[title="页面配置"]').first().click()
  await page.waitForTimeout(1200)
  const cfgRows2 = await page.evaluate(() => {
    const m = [...document.querySelectorAll('.ant-modal-content')].filter(e => e.offsetParent !== null).pop()
    const pane = m ? m.querySelector('.ant-tabs-tabpane-active') : null
    return pane ? pane.querySelectorAll('table tbody tr').length : 0
  })
  check('按明细页面配置查询条件 11 项', cfgRows2 === 11, `rows=${cfgRows2}`)
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 回到按单据：批量收货闭环
  await page.locator('.tab-item:has-text("按单据")').first().click()
  await page.waitForTimeout(3000)
  // 先按单据编号精确定位（共享 devdb 中存在其它会话的采购订单）
  const noInput = page.locator('.search-area input[placeholder*="请输入"]').first()
  await noInput.fill(ORD2)
  await noInput.press('Enter')   // 查询条件面板支持回车查询（避免 antd 两字按钮空格导致的定位歧义）
  await page.waitForTimeout(3000)
  const rowTarget = page.locator('.ss-grid tbody tr', { hasText: ORD2 }).first()
  check('存在待收货目标行（E2E-CGD-0002）', (await rowTarget.count()) > 0)
  if (await rowTarget.count()) {
    await rowTarget.locator('input.ss-checkbox, input[type=checkbox]').first().click({ force: true })
    await page.waitForTimeout(600)

    // F8 快捷键 → 打开打印弹窗（与「打印(F8)」按钮同入口）
    await page.keyboard.press('F8')
    await page.waitForTimeout(2200)
    const printModal = page.locator('.ant-modal-content:visible').last()
    const printTxt = (await printModal.innerText().catch(() => '')).replace(/\s+/g, '')
    check('F8 快捷键打开打印弹窗', printTxt.includes('打印'), printTxt.slice(0, 60))
    const printClose = printModal.locator('.ant-modal-close')
    if (await printClose.count()) { await printClose.last().click(); await page.waitForTimeout(900) }

    await page.locator('button:has-text("批量收货")').first().click()
    await page.waitForTimeout(1200)
    const confirmBox = page.locator('.ant-modal-confirm:visible').last()
    const confirmText = (await confirmBox.innerText()).replace(/\s+/g, '')
    check('批量收货二次确认弹窗', confirmText.includes('批量收货') && confirmText.includes('采购入库单'), confirmText.slice(0, 120))
    await confirmBox.locator('button:has-text("确认收货")').click()
    await page.waitForTimeout(3500)
    const afterRecv = await bodyText()
    check('批量收货成功提示（生成采购入库单）',
      afterRecv.includes('已生成') || (afterRecv.includes('采购入库单') && afterRecv.includes('成功')),
      afterRecv.slice(-140))
    // 收货后勾选态必须清除，避免对同一批单据重复收货
    await page.waitForTimeout(800)
    const afterBtnText = (await page.locator('button').filter({ hasText: /批量收货/ }).first().innerText()).replace(/\s+/g, '')
    check('批量收货后勾选态清除（按钮回到空态）', afterBtnText === '批量收货', afterBtnText)
    await page.screenshot({ path: path.join(SHOTS, 'ui-batch-receive.png'), fullPage: true })
    // 若弹出「收货结果」警告（如并行会话残留单据），先关闭再继续
    const warnModal = page.locator('.ant-modal-confirm:visible').last()
    if (await warnModal.count()) {
      const okBtn = warnModal.locator('button:has-text("知道了")')
      if (await okBtn.count()) await okBtn.click()
      await page.waitForTimeout(800)
    }
  }

  // 导出（真实下载）
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 30000 }).catch(() => null),
    page.locator('button:has-text("导出")').first().click(),
  ])
  check('导出触发真实 xlsx 下载', !!download && /\.xlsx$/i.test(download.suggestedFilename() || ''),
    download ? download.suggestedFilename() : '无下载事件')

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await login()
    console.log('登录成功')
    await apiSuite()
    try {
      await uiSuite()
    } catch (e) {
      check('UI 验收执行', false, e.message)
      console.error(e)
    }
  } catch (e) {
    check('主流程', false, e.message)
    console.error(e)
  }

  const pass = results.filter(r => r.ok).length
  console.log(`\n═══ 验收汇总：${pass}/${results.length} ═══`)
  results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  fs.writeFileSync(path.join(SHOTS, 'result.json'), JSON.stringify(results, null, 2))
  process.exit(pass === results.length ? 0 : 1)
})()
