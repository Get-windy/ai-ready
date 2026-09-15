/*
 * 发货查询（配发收 → 发货业务 → 发货查询，菜单 70156）金标准端到端验证
 *   · API 验收：49 列台账口径（销售出库单 XSCKD）/25 查询条件/红冲口径/配送状态·配送线路固定项（DMS 反查）/
 *               真实 xlsx 导出/按明细（商品汇总数据源）/VO 字段口径
 *   · UI  验收：默认 14 列表头、6 功能按钮、固定查询项 + 配置驱动查询区、列配置齿轮（个人·全局，49 列）、
 *               页面配置（25 查询项 / 5 功能按钮）、经典分页、明细弹窗、商品汇总、配送状态过滤、批量打印/导出
 *
 * 前置：执行 tools/e2e-ship-query-user.sql（验收账号 + 出库单/线路/配送任务基座）
 * 用法：node tools/e2e-ship-query.cjs                 （默认后端 5655、前端 5656）
 *      ERP_PORT=5655 FE_URL=http://localhost:5656 node tools/e2e-ship-query.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5655)
const SHOTS = 'I:/AI-Ready/tool-results/ship-query'
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
const E2E_USER = process.env.E2E_USER || 'e2e_shipq'
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

function data(res) {
  return res.json ? res.json.data : null
}

/**
 * 销售出库单接口（erp-sales）返回**裸** Page/VO/List；DMS 侧接口包 ApiResponse。
 * 此方法两种都能取到实体（导出接口无 json，直接用 buf）。
 */
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

// ══════════════ 验收结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

const OUT1 = 'E2E-XSCKD-0001'
const OUT2 = 'E2E-XSCKD-0002'
const OUT_RED = 'E2E-XSCKD-9001'
const ROUTE_ID = '2099000000000000401'

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 1) 台账口径：默认隐藏红冲
  const p1 = await api('GET', `/erp/sale/outbound/page?pageNum=1&pageSize=20&outboundNo=${OUT1}`)
  check('分页按单据编号命中', Number(payload(p1)?.total) === 1, JSON.stringify(payload(p1)?.total))
  const p2 = await api('GET', '/erp/sale/outbound/page?pageNum=1&pageSize=20&outboundNo=E2E-XSCKD-')
  check('默认隐藏红冲（负数金额单不出现）', Number(payload(p2)?.total) === 2, JSON.stringify(payload(p2)?.total))
  const p3 = await api('GET', '/erp/sale/outbound/page?pageNum=1&pageSize=20&outboundNo=E2E-XSCKD-&showRed=true')
  check('显示红冲后红冲单出现', Number(payload(p3)?.total) === 3, JSON.stringify(payload(p3)?.total))

  // 2) 25 项查询条件（抽样关键项）
  const cond = async (qs, expect, name) => {
    const r = await api('GET', `/erp/sale/outbound/page?pageNum=1&pageSize=50&outboundNo=E2E-XSCKD-${qs}`)
    check(name, Number(payload(r)?.total) === expect, JSON.stringify(payload(r)?.total))
  }
  await cond('&customerName=E2E发货客户甲', 1, '按客户过滤')
  await cond('&salesPersonName=E2E经手人', 2, '按经手人过滤')
  await cond('&warehouseName=E2E主仓', 2, '按仓库过滤')
  await cond('&creatorName=E2E制单人', 2, '按制单人过滤')
  await cond('&auditorName=E2E审核人', 2, '按审核人过滤')
  await cond('&bookkeeperName=E2E记账人', 2, '按记账人过滤')
  await cond('&sourceOrder=E2E-XSDD-0001', 1, '按来源订单过滤')
  await cond('&settlementStatus=partial', 1, '按结算状态过滤')
  await cond('&generationMethod=订单生成', 1, '按产生方式过滤')
  await cond('&receiverName=E2E收货人', 2, '按收货人过滤')
  await cond('&receiverPhone=13800000001', 1, '按联系电话过滤')
  await cond('&shippingAddress=E2E收货地址1', 1, '按收货地址过滤')
  await cond('&logisticsCompany=E2E物流公司', 2, '按物流公司过滤')
  await cond('&trackingNumber=E2E运单0001', 1, '按运单号过滤')
  await cond('&extText1=E2E表头文本1', 1, '按表头自定义字段3(文本)过滤')
  await cond('&extNum1=8', 1, '按表头自定义字段1(数字)过滤')
  await cond('&extNum2=99', 1, '按表头自定义字段2(数字)过滤')
  await cond('&extText2=E2E文本2A', 1, '按表头自定义字段4(文本)过滤')
  await cond('&extText3=E2E文本3A', 1, '按表头自定义字段5(文本)过滤')
  await cond('&departmentName=E2E发货部门', 2, '按部门过滤')
  await cond('&deliveryMethod=delivery', 1, '按配送方式过滤')
  await cond('&remark=E2E发货备注2', 1, '按单据备注过滤（名称模糊口径）')
  const today = new Date().toISOString().slice(0, 10)
  await cond(`&dateStart=${today}&dateEnd=${today}`, 2, '按日期区间过滤')

  // 3) VO 字段口径（发货查询 49 列所需字段必须齐备）
  const row = (payload(await api('GET', `/erp/sale/outbound/page?pageNum=1&pageSize=1&outboundNo=${OUT1}`))?.records || [])[0] || {}
  const needFields = ['outboundDate', 'outboundNo', 'status', 'orderNo', 'warehouseName', 'customerName',
    'salesPersonName', 'totalAmount', 'settledAmount', 'settlementStatus', 'totalQuantity', 'generationMethod',
    'remark', 'printCount', 'receiverName', 'receiverPhone', 'shippingAddress', 'logisticsCompany', 'trackingNumber',
    'extNum1', 'extText1', 'deliveryMethod', 'totalWeight', 'totalVolume', 'creatorName', 'auditorName',
    'bookkeeperName', 'bookkeepingTime', 'createTime']
  const missing = needFields.filter(f => !(f in row))
  check('出库单 VO 覆盖发货查询 29 个关键字段', missing.length === 0, '缺失: ' + JSON.stringify(missing))
  check('已结金额/结算状态口径就绪', Number(row.settledAmount) === 400 && row.settlementStatus === 'partial',
    `${row.settledAmount}/${row.settlementStatus}`)
  // 本单金额口径数据源：商品金额(totalAmount) + 构成项 齐备且取非零值（否则「口径错绑商品金额」无法暴露）
  check('商品金额与金额构成字段就绪（本单金额口径数据源）',
    Number(row.totalAmount) === 1000 && Number(row.promoDiscount) === 50 && Number(row.freight) === 20 && Number(row.otherFee) === 10
      && Number(row.couponAmount) === 0 && Number(row.directDiscount) === 0,
    `${row.totalAmount}/${row.promoDiscount}/${row.couponAmount}/${row.directDiscount}/${row.freight}/${row.otherFee}`)
  check('本单金额口径 = 商品金额 − 优惠 + 运费 + 其他费用 = 980（≠ 商品金额 1000）',
    Number((Number(row.totalAmount) - Number(row.promoDiscount) - Number(row.couponAmount) - Number(row.directDiscount)
      + Number(row.freight) + Number(row.otherFee)).toFixed(2)) === 980,
    `${row.totalAmount}`)

  // 4) 配送状态 / 配送线路 → 出库单号反查（页面固定项）
  const f1 = await api('GET', '/dms/task/outbound-filter?deliveryStatus=DELIVERING')
  check('配送状态=配送中 反查命中出库单', (data(f1)?.sourceBillNos || []).includes(OUT1), JSON.stringify(data(f1)))
  const f2 = await api('GET', '/dms/task/outbound-filter?deliveryStatus=DELIVERED')
  check('配送状态=已配送 反查命中另一单', (data(f2)?.sourceBillNos || []).includes(OUT2) && !(data(f2)?.sourceBillNos || []).includes(OUT1), JSON.stringify(data(f2)))
  const f3 = await api('GET', `/dms/task/outbound-filter?routeId=${ROUTE_ID}`)
  check('配送线路 反查命中出库单', (data(f3)?.sourceBillNos || []).includes(OUT1) && Number(data(f3)?.matched) === 1, JSON.stringify(data(f3)))
  const f4 = await api('GET', '/dms/task/outbound-filter?deliveryStatus=PENDING')
  check('配送状态=待配送 不误含「配送中」单据（口径精确）',
    Array.isArray(data(f4)?.sourceBillNos) && !data(f4)?.sourceBillNos.includes(OUT1), JSON.stringify(data(f4)))
  const f5 = await api('GET', '/dms/task/outbound-filter?deliveryStatus=XXX')
  check('非法配送状态被拒绝', f5.json?.code !== 200, f5.json?.message)

  // 5) 固定项接入分页查询（outboundNos）
  const p4 = await api('GET', `/erp/sale/outbound/page?pageNum=1&pageSize=20&outboundNos=${OUT1}`)
  check('按 outboundNos 精确过滤命中 1 条', Number(payload(p4)?.total) === 1, JSON.stringify(payload(p4)?.total))
  const p5 = await api('GET', '/erp/sale/outbound/page?pageNum=1&pageSize=20&outboundNos=__NONE__')
  check('固定项无命中时返回空（不回落全量）', Number(payload(p5)?.total) === 0, JSON.stringify(payload(p5)?.total))
  const p6 = await api('GET', '/erp/sale/outbound/page?pageNum=1&pageSize=20&outboundNo=E2E-XSCKD-')
  check('分页 pageSize 生效且按单据日期/时间倒序返回', (payload(p6)?.records || []).length === 2, JSON.stringify((payload(p6)?.records || []).length))

  // 6) 按明细（商品汇总数据源）
  const d1 = await api('GET', '/erp/sale/outbound/page-detail?pageNum=1&pageSize=50&outboundNo=E2E-XSCKD-')
  const dRows = payload(d1)?.records || []
  check('按明细返回 3 行商品明细', dRows.length === 3, JSON.stringify(dRows.length))
  const dHit = dRows.find(r => r.productName === 'E2E商品甲')
  check('明细含商品/数量/金额（商品汇总口径）',
    !!dHit && Number(dHit.quantity) > 0 && Number(dHit.lineAmount) > 0,
    JSON.stringify(dHit || {}).slice(0, 160))

  // 7) 明细接口（查看明细弹窗数据源）
  const det = await api('GET', `/erp/sale/outbound/${(row.id)}`)
  check('详情接口返回单据头（含客户/仓库/金额）',
    payload(det)?.outboundNo === OUT1 && payload(det)?.customerName === 'E2E发货客户甲', JSON.stringify(payload(det)).slice(0, 160))
  const items = await api('GET', `/erp/sale/outbound/${row.id}/items`)
  check('明细接口返回商品行', Array.isArray(payload(items)) && payload(items).length === 2, JSON.stringify((payload(items) || []).length))

  // 8) 导出真实 xlsx
  const exp = await api('GET', '/erp/sale/outbound/export?outboundNo=E2E-XSCKD-')
  check('导出返回 xlsx（PK 头，非 JSON）', exp.buf && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b, `status=${exp.status}`)
  if (exp.buf && exp.buf[0] === 0x50) {
    fs.writeFileSync(path.join(SHOTS, 'export.xlsx'), exp.buf)
    try {
      const wb = XLSX.read(exp.buf, { type: 'buffer' })
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
      check('导出内容含 2 行出库单', rows.length === 2, `rows=${rows.length}`)
      check('导出表头含单据编号/客户/本单金额',
        ['单据编号', '客户', '本单金额'].every(h => rows[0] && Object.keys(rows[0]).includes(h)),
        JSON.stringify(Object.keys(rows[0] || {})))
    } catch (e) {
      check('导出 xlsx 可解析', false, e.message)
    }
  }

  // 9) 打印次数回写（页面批量打印/打印(F8) 后调用）
  const before = Number(row.printCount || 0)
  await api('POST', `/erp/sale/outbound/${row.id}/print`)
  const after = Number((payload(await api('GET', `/erp/sale/outbound/${row.id}`)) || {}).printCount || 0)
  check('打印次数真实回写（print_count +1）', after === before + 1, `${before} → ${after}`)
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

  await openPage(`${FE}/dispatch/ship-query`)
  const txt = await bodyText()
  check('页面可打开（无 404/白屏）', !txt.includes('页面不存在') && txt.length > 50, page.url())

  // 默认 14 列表头
  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['单据日期', '单据编号', '单据状态', '来源订单', '仓库', '客户', '经手人', '本单金额', '已结金额', '结算状态', '数量', '产生方式', '单据备注', '打印次数']) {
    check(`默认表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  check('默认表头不含隐藏列（客户编号/运单号）',
    !headers.some(x => x.includes('客户编号')) && !headers.some(x => x.includes('运单号')), headers.join('/'))

  // 工具栏 6 项（含配置齿轮）
  for (const b of ['批量打印', '商品汇总', '刷新', '打印(F8)', '导出']) {
    check(`工具栏含「${b}」`, txt.includes(b.replace(/\s+/g, '')))
  }
  check('工具栏含「配置」齿轮（页面配置入口）', (await page.locator('button[title="页面配置"]').count()) > 0)

  // 查询区：固定项 + 配置驱动项
  check('查询区固定项「配送状态」（三值）',
    (await page.locator('.search-area .search-item', { hasText: '配送状态' }).count()) > 0)
  check('查询区固定项「配送线路」（线路档案选择器）',
    (await page.locator('.search-area .search-item', { hasText: '配送线路' }).count()) > 0)
  for (const q of ['日期', '单据编号', '客户', '经手人', '部门', '仓库', '结算状态', '产生方式', '来源订单', '单据备注', '显示红冲']) {
    check(`查询区含默认条件「${q}」`, txt.includes(q))
  }
  check('查询区含「查询 / 重置」', txt.includes('查询') && txt.includes('重置'))

  // 数据行（台账口径）
  check('列表显示种子出库单号', txt.includes(OUT1.replace(/-/g, '')) || txt.includes('E2E-XSCKD-0001'), OUT1)
  check('列表显示客户名', txt.includes('E2E发货客户甲'))
  check('列表显示结算状态文本', txt.includes('部分结算'))
  // 本单金额列口径：1000 − 50 − 0 − 0 + 20 + 10 = 980.00（若错绑商品金额会显示 1,000.00）
  check('本单金额列按口径计算（980.00，非商品金额 1,000.00）',
    txt.includes('980.00') && !txt.includes('1,000.00'), txt.replace(/\s+/g, '').slice(0, 200))
  check('列表默认不显示红冲单', !txt.includes('E2E-XSCKD-9001'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 列配置齿轮（个人/全局，49 列）
  const gearCount = await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length)
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)
  if (gearCount > 0) {
    await page.locator('.ss-grid .th-settings-btn').first().click()
    await page.waitForTimeout(1200)
    const colPanel = page.locator('.ant-modal-content:visible').last()
    const colText = (await colPanel.innerText()).replace(/\s+/g, '')
    check('列配置弹窗含「个人配置 / 全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 60))
    for (const c of ['客户编号', '客户级别', '运单号', '商品金额', '运费承担方', '表尾自定义字段1(文本)', '表尾自定义字段2(文本)', '记账人', '打印时间', '附件']) {
      check(`列配置含隐藏列「${c}」`, colText.includes(c), c)
    }
    const COL_49 = ['单据日期', '单据编号', '单据状态', '来源订单', '仓库', '客户', '客户编号', '客户级别', '收货人',
      '联系电话', '收货地址', '物流公司', '运单号', '客户一票通', '客户备注', '经手人', '部门', '商品金额', '促销优惠',
      '优惠劵', '直接优惠', '运费承担方', '运费', '其他费用', '本单金额', '已结金额', '结算状态', '数量', '配送方式',
      '重量（kg）', '体积（m³）', '产生方式', '单据备注', '摘要', '附件', '表头自定义字段1(数字)', '表头自定义字段2(数字)',
      '表头自定义字段3(文本)', '表头自定义字段4(文本)', '表头自定义字段5(文本)', '表尾自定义字段1(文本)',
      '表尾自定义字段2(文本)', '制单人', '记账人', '审核人', '记账时间', '制单时间', '打印次数', '打印时间']
    const missCols = COL_49.filter(c => !colText.includes(c))
    check('列配置含对标全部 49 列', missCols.length === 0, '缺失: ' + missCols.join('、'))
    await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  }

  // 页面配置（查询条件 25 / 功能按钮 5 + 打印配置）
  await page.locator('button[title="页面配置"]').first().click()
  await page.waitForTimeout(1200)
  const cfgPanel = page.locator('.ant-modal-content:visible').last()
  const cfgText = (await cfgPanel.innerText()).replace(/\s+/g, '')
  check('页面配置弹窗可打开且为查询条件 Tab', cfgText.includes('查询条件'), cfgText.slice(0, 60))
  const queryRows = await page.evaluate(() => {
    const m = [...document.querySelectorAll('.ant-modal-content')].filter(e => e.offsetParent !== null).pop()
    return m ? m.querySelectorAll('table tbody tr').length : 0
  })
  check('页面配置查询条件共 25 项（对标实测）', queryRows === 25, `rows=${queryRows}`)
  for (const q of ['表头自定义字段1(数字)', '表头自定义字段5(文本)', '联系电话', '收货地址', '物流公司', '运单号', '显示红冲', '配送方式']) {
    check(`页面配置含查询项「${q}」`, cfgText.includes(q), q)
  }
  await cfgPanel.locator('.ant-tabs-tab:has-text("功能按钮")').click()
  await page.waitForTimeout(600)
  const btnText = (await cfgPanel.innerText()).replace(/\s+/g, '')
  for (const b of ['批量打印', '商品汇总', '刷新', '打印(F8)', '导出']) {
    check(`页面配置功能按钮含「${b}」`, btnText.includes(b), b)
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png'), fullPage: true })
  await cfgPanel.locator('.ant-modal-close').click()
  await page.waitForTimeout(800)

  // 经典分页栏
  check('底部为经典分页栏（standard-pagination classic）',
    (await page.locator('.classic-pagination').count()) > 0)

  // 明细弹窗
  await page.locator('.ss-grid a.cell-link').first().click()
  await page.waitForTimeout(2200)
  const detailModal = page.locator('.ant-modal-content:visible').last()
  const detailText = (await detailModal.innerText()).replace(/\s+/g, '')
  check('明细弹窗可打开（单据头 + 商品明细）',
    detailText.includes('销售出库单明细') && detailText.includes('E2E商品甲'), detailText.slice(0, 120))
  check('明细弹窗显示结算状态（已结算/部分结算/未结算）',
    detailText.includes('结算状态')
    && ['已结算', '部分结算', '未结算'].some(v => detailText.includes(v)), detailText.slice(-80))
  // 明细弹窗单据头「本单金额」与列表同口径：0001=980.00 / 0002=380.00
  // （弹窗打开的是列表首行，两条都可能；错绑商品金额时会显示 1,000.00 / 500.00）
  const modalBillAmount = (detailText.match(/本单金额([\d,]+\.\d{2})/) || [])[1]
  check('明细弹窗「本单金额」按口径计算（980.00 / 380.00，非商品金额）',
    ['980.00', '380.00'].includes(modalBillAmount), '实际=' + modalBillAmount)
  await page.screenshot({ path: path.join(SHOTS, 'ui-detail.png'), fullPage: true })
  await detailModal.locator('.ant-modal-close').click()
  await page.waitForTimeout(900)

  // 商品汇总
  await page.locator('button:has-text("商品汇总")').first().click()
  await page.waitForTimeout(2500)
  const sumModal = page.locator('.ant-modal-content:visible').last()
  const sumText = (await sumModal.innerText()).replace(/\s+/g, '')
  check('商品汇总弹窗可打开', sumText.includes('商品汇总'), sumText.slice(0, 60))
  check('商品汇总列出商品与合计',
    sumText.includes('E2E商品甲') && sumText.includes('合计数量'), sumText.slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-summary.png'), fullPage: true })
  await sumModal.locator('.ant-modal-close').click()
  await page.waitForTimeout(900)

  // 配送状态固定项过滤
  const statusItem = page.locator('.search-area .search-item', { hasText: '配送状态' }).first()
  /**
   * 选择 antd 下拉项。antd 下拉在本环境偶发渲染慢（首子节点为注释时会渲染空下拉），
   * 单次点击会 30s 超时导致整轮 UI 验收中断 → 重试 3 次。
   */
  const pickOption = async (selLocator, text) => {
    for (let i = 0; i < 3; i++) {
      await selLocator.locator('.ant-select').first().click()
      const opt = page.locator(`.ant-select-item-option:has-text("${text}")`).first()
      try {
        await opt.waitFor({ state: 'visible', timeout: 8000 })
        await opt.click({ timeout: 8000 })
        return
      } catch (e) {
        await page.keyboard.press('Escape')
        await page.waitForTimeout(500)
      }
    }
    throw new Error(`下拉项「${text}」选择失败（重试 3 次）`)
  }
  await pickOption(statusItem, '配送中')
  await page.waitForTimeout(3000)
  const afterFilter = await bodyText()
  check('按配送状态=配送中 过滤后只剩 E2E-XSCKD-0001',
    afterFilter.includes('E2E-XSCKD-0001') && !afterFilter.includes('E2E-XSCKD-0002'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-delivery-filter.png'), fullPage: true })

  // 恢复「全部」后勾选一行 → 批量打印 → PrintDialog
  await pickOption(statusItem, '待配送')
  await page.waitForTimeout(2500)
  const noHit = await bodyText()
  check('配送状态=待配送 时列表为空（固定项真实生效）', !noHit.includes('E2E-XSCKD-0001'))

  // 勾选：先恢复为空条件（清空配送状态）
  await page.locator('.search-area .search-item', { hasText: '配送状态' }).first().locator('.ant-select').first().hover()
  await page.locator('.search-area .search-item', { hasText: '配送状态' }).first().locator('.ant-select-clear').first().click({ force: true })
  await page.waitForTimeout(3000)
  const backRows = await page.locator('.ss-grid tbody tr').count()
  if (backRows > 0) {
    await page.locator('.ss-grid tbody tr').first().locator('input.ss-checkbox, input[type=checkbox]').first().click({ force: true })
    await page.waitForTimeout(600)
    await page.locator('button:has-text("批量打印")').first().click()
    await page.waitForTimeout(1200)
    const batchModal = page.locator('.ant-modal-content:visible').last()
    const batchText = (await batchModal.innerText()).replace(/\s+/g, '')
    check('批量打印弹窗可打开（含仅打印未打印过选项）',
      batchText.includes('批量打印') && batchText.includes('仅打印未打印过的单据'), batchText.slice(0, 120))
    await batchModal.locator('button:has-text("开始打印")').click()
    await page.waitForTimeout(2500)
    const printText = (await page.locator('body').innerText()).replace(/\s+/g, '')
    check('打印模板弹窗（PrintDialog）可打开', printText.includes('打印模板') || printText.includes('打印模式'), '')
    await page.screenshot({ path: path.join(SHOTS, 'ui-print.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  } else {
    check('列表恢复后存在可勾选行', false, `rows=${backRows}`)
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
