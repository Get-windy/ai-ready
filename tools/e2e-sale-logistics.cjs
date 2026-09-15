/*
 * 销售物流域端到端验收（《物流发货-业界做法调研》P0 / P1 / P2）
 *
 * 覆盖：
 *   P0-1 物流信息接 1:N 子表 erp_sale_order_logistics + 主表快照同步（消除双口径）
 *   P0-2 承运商引用《资料→物流公司》档案（id + 名称快照）
 *   P1   一单多包（新增/编辑/删除包裹、默认包裹快照、最后一个不可删、运单号唯一）
 *   P2-4 运费规则（首重/续重）→ 自动试算落 shipping_fee → 承运商账单金额对账出差异 → 标记已对账
 *   P2-5 电子面单取号：未配置时**明确报错**（不造假号）；配置后走真实 HTTP 取号
 *   P2-6 发货通知 ASN：发货后幂等生成台账 → 按配置回调推送 → 未配置只落台账
 *
 * 依赖两个验收实例（同一 DB）：
 *   A: --server.port=5695                              （未配置取号/ASN → 验证降级与拦截）
 *   B: --server.port=5697 --erp.logistics.waybill.enabled=true
 *        --erp.logistics.waybill.url=http://localhost:5696/waybill
 *        --erp.shipment.asn.callback-url=http://localhost:5696/asn        （验证真实取号与推送）
 *   本脚本内启动 mock 承运商服务（5696）。
 *
 * 用法：node tools/e2e-sale-logistics.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const PORT_A = Number(process.env.PORT_A || 5695)
const PORT_B = Number(process.env.PORT_B || 5697)
const MOCK_PORT = Number(process.env.MOCK_PORT || 5696)
const FE = process.env.FE_URL || 'http://localhost:5690'
const SHOTS = 'I:/AI-Ready/tool-results/sale-logistics'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
const E2E_USER = process.env.E2E_USER || 'e2e_sale_logi'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

const DD = 'E2ELG'
const STAMP = Date.now().toString().slice(-6)
const CARRIER_NAME = 'E2E承运商' + STAMP

async function dbQuery(sql, params) {
  const client = new Client(DSN)
  await client.connect()
  try { return (await client.query(sql, params)).rows } finally { await client.end() }
}

// ══════════════ mock 承运商服务 ══════════════
let mockWaybillSeq = 0
let mockAsnHits = []
function startMock() {
  return new Promise((resolve, reject) => {
    const server = http.createServer((req, res) => {
      let body = ''
      req.on('data', c => { body += c })
      req.on('end', () => {
        if (req.url.startsWith('/waybill')) {
          mockWaybillSeq++
          res.writeHead(200, { 'Content-Type': 'application/json' })
          res.end(JSON.stringify({ data: { waybillNo: `MOCK${STAMP}${mockWaybillSeq}` } }))
        } else if (req.url.startsWith('/asn')) {
          mockAsnHits.push(body)
          res.writeHead(200, { 'Content-Type': 'application/json' })
          res.end(JSON.stringify({ ok: true }))
        } else {
          res.writeHead(404); res.end('{}')
        }
      })
    })
    // 上一次异常退出可能残留 mock 进程占用端口：给出明确提示而不是裸 EADDRINUSE
    server.on('error', (e) => {
      if (e.code === 'EADDRINUSE') {
        reject(new Error(`mock 端口 ${MOCK_PORT} 被占用（可能是上一轮验收残留的 node 进程），`
          + `请先结束它再重跑：netstat -ano | findstr :${MOCK_PORT}`))
      } else {
        reject(e)
      }
    })
    server.listen(MOCK_PORT, () => resolve(server))
  })
}

// ══════════════ HTTP ══════════════
function rawReq(port, method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? null : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* ignore */ }
        resolve({ status: res.statusCode, json })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

let TOKEN = null
const apiA = (m, p, b) => rawReq(PORT_A, m, p, b, TOKEN)
const apiB = (m, p, b) => rawReq(PORT_B, m, p, b, TOKEN)
const data = r => (r.json ? r.json.data : null)
const okCode = r => Number(r.json?.code) === 200

async function login() {
  const cap = await rawReq(PORT_A, 'GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq(PORT_A, 'POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  TOKEN = res.json?.data?.token || res.json?.data?.accessToken || null
  if (!TOKEN) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
}

const results = []
function check(name, pass, detail) {
  results.push({ name, ok: !!pass, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${pass ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

const ORDER = {}
const TODAY = new Date().toISOString().slice(0, 10)

/** 幂等创建验收账号（等权 admin：role_id=1 SUPER_ADMIN + 系统租户） */
async function ensureE2EUser() {
  const exist = await dbQuery(`SELECT id FROM sys_user WHERE username = $1`, [E2E_USER])
  if (exist.length > 0) return
  const uid = Number(String(Date.now()).slice(-15) + '001')
  await dbQuery(
    `INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                           user_type, is_super_admin, is_tenant_admin, status, data_scope)
     SELECT $2, 1, 0, now(), now(), $1, password, 'E2E销售物流', 'E2E销售物流',
            1, false, false, 1, 'ALL'
     FROM sys_user WHERE username = 'admin'`, [E2E_USER, uid])
  await dbQuery(`INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
                 VALUES ($1, $2, 1, 1, now())`, [uid + 1, uid])
  await dbQuery(`INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
                 VALUES ($1, $2, 1, true, 1, now(), now())`, [uid + 2, uid])
  console.log(`已创建验收账号 ${E2E_USER}`)
}

async function prepareData() {
  console.log('\n═══ 〇、造数 ═══')
  await dbQuery(`DELETE FROM erp_sale_order_logistics WHERE order_id IN (SELECT id FROM erp_sale_order WHERE order_no LIKE '${DD}%')`)
  await dbQuery(`DELETE FROM erp_sale_order_item WHERE order_id IN (SELECT id FROM erp_sale_order WHERE order_no LIKE '${DD}%')`)
  await dbQuery(`DELETE FROM erp_sale_order WHERE order_no LIKE '${DD}%'`)
  await dbQuery(`DELETE FROM erp_freight_rule WHERE carrier_name LIKE 'E2E承运商%' OR remark LIKE '${DD}%'`)
  await dbQuery(`DELETE FROM erp_shipment_notify WHERE order_no LIKE '${DD}%'`)

  const base = Number(String(Date.now()).slice(-15))
  const mk = async (suffix, status, qty) => {
    const id = base + suffix
    await dbQuery(
      `INSERT INTO erp_sale_order (id, tenant_id, deleted, create_time, update_time, order_no, order_date, sale_type, status,
        customer_id, customer_name, customer_code, warehouse_id, warehouse_name,
        product_amount, bill_amount, total_quantity, picked_quantity, shipped_quantity, unshipped_quantity,
        delivery_method, receiver_name, receiver_phone, shipping_address, order_source)
       VALUES ($1,1,0,now(),now(),$2,$3,0,$4, 2,'客户甲','C0002',1,'主仓库',
        $5,$5,$6,0,0,$6, '物流','E2E收货人','13800000000','E2E收货地址',1)`,
      [id, `${DD}-${suffix}-${STAMP}`, TODAY, status, Number(qty) * 10, qty])
    await dbQuery(
      `INSERT INTO erp_sale_order_item (order_id, tenant_id, line_no, product_id, product_code, product_name, unit,
        quantity, shipped_quantity, shipped_quantity_detail, picked_quantity, unit_price, amount, warehouse_id)
       VALUES ($1,1,1,$2,$3,$4,'件',$5,0,0,$5,10,$6,1)`,
      [id, base + 500, 'P' + suffix + STAMP, 'E2E商品' + suffix, qty, Number(qty) * 10])
    return id
  }
  ORDER.a = await mk(1, 2, 3)   // 主测订单（待发货 → 接口链路后会发货）
  ORDER.b = await mk(2, 2, 2)   // 包裹多包/取号
  ORDER.c = await mk(3, 2, 1)   // 发货 + ASN
  ORDER.d = await mk(4, 2, 2)   // UI 专用（保持待发货）
  console.log(`  A=${ORDER.a} B=${ORDER.b} C=${ORDER.c} D=${ORDER.d}`)
}

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // ── P0-1 物流备注 → 子表 + 主表快照 ──
  const lr1 = await apiA('POST', '/erp/sale/order/batch-logistics-remark', {
    ids: [ORDER.a], deliveryMethod: '物流', waybillNo: 'WB' + STAMP, logisticsCompany: '顺丰速运',
    driverName: 'E2E司机', receiverName: 'E2E收货人', salesmanName: 'E2E经手人',
  })
  check('P0-1 物流备注更新返回 200', okCode(lr1), JSON.stringify(lr1.json).slice(0, 160))
  const pkgRows = await dbQuery(`SELECT * FROM erp_sale_order_logistics WHERE order_id=$1 ORDER BY id`, [ORDER.a])
  check('P0-1 子表 erp_sale_order_logistics 已落库（1 条默认包裹）', pkgRows.length === 1, `rows=${pkgRows.length}`)
  check('P0-1 子表运单号/承运商落库',
    pkgRows[0]?.waybill_no === 'WB' + STAMP && pkgRows[0]?.logistics_company === '顺丰速运',
    `${pkgRows[0]?.waybill_no}/${pkgRows[0]?.logistics_company}`)
  const orderA = (await dbQuery(`SELECT logistics_company, waybill_no, delivery_method, driver_name FROM erp_sale_order WHERE id=$1`, [ORDER.a]))[0]
  check('P0-1 主表快照与子表一致（列表展示口径）',
    orderA.waybill_no === 'WB' + STAMP && orderA.logistics_company === '顺丰速运' && orderA.driver_name === 'E2E司机',
    `${orderA.waybill_no}/${orderA.logistics_company}/${orderA.driver_name}`)
  // 二次更新不新增包裹（默认包裹复用）
  await apiA('POST', '/erp/sale/order/batch-logistics-remark', { ids: [ORDER.a], waybillNo: 'WB2' + STAMP })
  const pkgRows2 = await dbQuery(`SELECT id, waybill_no FROM erp_sale_order_logistics WHERE order_id=$1 ORDER BY id`, [ORDER.a])
  check('P0-1 重复更新复用默认包裹（不新增行）', pkgRows2.length === 1 && pkgRows2[0].waybill_no === 'WB2' + STAMP,
    `rows=${pkgRows2.length}, waybill=${pkgRows2[0]?.waybill_no}`)
  // 未传字段不清空
  const orderA2 = (await dbQuery(`SELECT logistics_company FROM erp_sale_order WHERE id=$1`, [ORDER.a]))[0]
  check('P0-1 空值不改动（承运商保持）', orderA2.logistics_company === '顺丰速运', orderA2.logistics_company)

  // ── P0-2 承运商档案引用 ──
  const carrier = await dbQuery(
    `INSERT INTO biz_party (id, tenant_id, deleted, create_time, update_time, party_type, party_name, party_code, mnemonic_code, status)
     VALUES ($1, 1, 0, now(), now(), 3, $2, $3, $4, 1) RETURNING id`,
    [Number(String(Date.now()).slice(-15) + '777'), CARRIER_NAME, 'E2EC' + STAMP, 'E2EC'])
  const carrierId = carrier[0].id
  const lrCarrier = await apiA('POST', '/erp/sale/order/batch-logistics-remark', {
    ids: [ORDER.a], logisticsCompanyId: carrierId,
  })
  check('P0-2 传承运商档案ID 更新成功', okCode(lrCarrier), JSON.stringify(lrCarrier.json).slice(0, 160))
  const pkgCarrier = (await dbQuery(`SELECT logistics_company_id, logistics_company FROM erp_sale_order_logistics WHERE order_id=$1`, [ORDER.a]))[0]
  check('P0-2 子表落档案ID + 名称快照',
    String(pkgCarrier.logistics_company_id) === String(carrierId) && pkgCarrier.logistics_company === CARRIER_NAME,
    `${pkgCarrier.logistics_company_id}/${pkgCarrier.logistics_company}`)
  const carrierOpts = await apiA('GET', `/erp/md/customer/list?partnerType=LOGISTICS&status=1&pageSize=500`)
  check('P0-2 档案下拉可取到该承运商',
    (data(carrierOpts) || []).some(p => String(p.id) === String(carrierId)),
    `count=${(data(carrierOpts) || []).length}`)

  // ── P1 一单多包 ──
  const pkg2 = await apiA('POST', `/erp/sale/logistics/${ORDER.a}/packages`, {
    waybillNo: 'WB-B' + STAMP, logisticsCompany: '圆通速递', packageCount: 2, packageWeight: 3,
  })
  check('P1 新增第二个包裹成功', okCode(pkg2), JSON.stringify(pkg2.json).slice(0, 160))
  const pkgList = await dbQuery(`SELECT package_no, waybill_no FROM erp_sale_order_logistics WHERE order_id=$1 ORDER BY id`, [ORDER.a])
  check('P1 订单下已有 2 个包裹且自动编号', pkgList.length === 2 && pkgList[1].package_no === 'P2',
    JSON.stringify(pkgList))
  const dup = await apiA('POST', `/erp/sale/logistics/${ORDER.a}/packages`, { waybillNo: 'WB-B' + STAMP })
  check('P1 运单号重复被拒（唯一性）', !okCode(dup) && /重复|已存在/.test(dup.json?.message || ''), dup.json?.message)
  const orderA3 = (await dbQuery(`SELECT waybill_no FROM erp_sale_order WHERE id=$1`, [ORDER.a]))[0]
  check('P1 主表快照仍取默认包裹（第一条）', orderA3.waybill_no === 'WB2' + STAMP, orderA3.waybill_no)
  const secondId = (await dbQuery(`SELECT id FROM erp_sale_order_logistics WHERE order_id=$1 ORDER BY id DESC LIMIT 1`, [ORDER.a]))[0].id
  const delExtra = await apiA('DELETE', `/erp/sale/logistics/${ORDER.a}/packages/${secondId}`)
  check('P1 删除多余包裹成功（2→1）', okCode(delExtra), delExtra.json?.message)
  const delLast = await apiA('DELETE', `/erp/sale/logistics/${ORDER.a}/packages/${pkgRows2[0].id}`)
  check('P1 删除最后一个包裹被拒', !okCode(delLast) && /至少保留/.test(delLast.json?.message || ''), delLast.json?.message)

  // ── P2-4 运费规则 / 试算 / 对账 ──
  const rule = await apiA('POST', '/erp/sale/logistics/freight/rules', {
    carrierName: CARRIER_NAME, carrierId: carrierId, firstWeight: 1, firstPrice: 8,
    addStep: 1, addPrice: 2, baseFee: 0, enabled: 1, priority: 10, remark: DD + STAMP,
  })
  check('P2-4 新增运费规则成功', okCode(rule), JSON.stringify(rule.json).slice(0, 160))
  const calc = await apiA('GET', `/erp/sale/logistics/freight/calc?carrierId=${carrierId}&weight=3`)
  check('P2-4 运费试算 = 首重8 + 续重2×2 = 12', okCode(calc) && Number(data(calc)?.freight) === 12,
    JSON.stringify(data(calc)))
  const calcMiss = await apiA('GET', '/erp/sale/logistics/freight/calc?weight=3')
  check('P2-4 无匹配规则时明确返回未命中', data(calcMiss)?.matched === false, JSON.stringify(data(calcMiss)))

  const pkgForFreight = await apiA('POST', `/erp/sale/logistics/${ORDER.b}/packages`, {
    waybillNo: 'WB-F' + STAMP, logisticsCompany: CARRIER_NAME, logisticsCompanyId: carrierId,
    packageWeight: 3, packageCount: 1,
  })
  check('P2-4 保存包裹时按规则自动带入运费',
    okCode(pkgForFreight) && Number(data(pkgForFreight)?.shippingFee) === 12,
    `shippingFee=${data(pkgForFreight)?.shippingFee}`)
  const pkgIdB = data(pkgForFreight)?.id
  const bill = await apiA('POST', `/erp/sale/logistics/freight/bill?packageId=${pkgIdB}&billAmount=15`)
  check('P2-4 录入承运商账单金额并算出差异 = 3',
    okCode(bill) && Number(data(bill)?.freightDiff) === 3, `diff=${data(bill)?.freightDiff}`)
  const rec = await apiA('GET', `/erp/sale/logistics/freight/reconcile?carrierId=${carrierId}`)
  const recD = data(rec) || {}
  check('P2-4 对账汇总：我方计费 / 账单 / 差异',
    Number(recD.chargedAmount) === 12 && Number(recD.billedAmount) === 15 && Number(recD.diffAmount) === 3,
    JSON.stringify({ c: recD.chargedAmount, b: recD.billedAmount, d: recD.diffAmount }))
  check('P2-4 差异计数 = 1', Number(recD.diffCount) === 1, `diffCount=${recD.diffCount}`)
  const mark = await apiA('POST', '/erp/sale/logistics/freight/reconcile-mark', [pkgIdB])
  const reconciled = (await dbQuery(`SELECT freight_reconciled FROM erp_sale_order_logistics WHERE id=$1`, [pkgIdB]))[0]
  check('P2-4 标记已对账落库', okCode(mark) && Number(reconciled.freight_reconciled) === 1,
    `reconciled=${reconciled.freight_reconciled}`)

  // ── P2-5 电子面单取号 ──
  const stA = await apiA('GET', '/erp/sale/logistics/waybill/status')
  check('P2-5 实例A（未配置）取号状态=false', data(stA)?.enabled === false, JSON.stringify(data(stA)))
  const acqOff = await apiA('POST', `/erp/sale/logistics/packages/${pkgIdB}/acquire-waybill`)
  check('P2-5 未开通时明确报错且不造假号',
    !okCode(acqOff) && /未开通/.test(acqOff.json?.message || ''), acqOff.json?.message)
  const afterOff = (await dbQuery(`SELECT waybill_no FROM erp_sale_order_logistics WHERE id=$1`, [pkgIdB]))[0]
  check('P2-5 取号失败不改写原运单号', afterOff.waybill_no === 'WB-F' + STAMP, afterOff.waybill_no)

  const stB = await apiB('GET', '/erp/sale/logistics/waybill/status')
  check('P2-5 实例B（已配置）取号状态=true', data(stB)?.enabled === true, JSON.stringify(data(stB)))
  const pkgNoWaybill = await apiA('POST', `/erp/sale/logistics/${ORDER.b}/packages`, { packageCount: 1, packageWeight: 1 })
  const noWaybillId = data(pkgNoWaybill)?.id
  const acqOn = await apiB('POST', `/erp/sale/logistics/packages/${noWaybillId}/acquire-waybill`)
  check('P2-5 已配置时真实取号成功（mock 承运商）',
    okCode(acqOn) && /^MOCK/.test(data(acqOn)?.waybillNo || ''), `waybillNo=${data(acqOn)?.waybillNo}`)

  // ── P2-6 发货通知 ASN ──
  // 先给 C 单造一个包裹（无包裹时发货不会置包裹状态）
  await apiA('POST', `/erp/sale/logistics/${ORDER.c}/packages`, { packageCount: 1, packageWeight: 1 })
  const shipC = await apiA('POST', `/erp/sale/order/${ORDER.c}/ship?warehouseId=1`)
  check('P2-6 发货接口调用成功', okCode(shipC), JSON.stringify(shipC.json).slice(0, 160))
  const pkgShipped = (await dbQuery(`SELECT package_status FROM erp_sale_order_logistics WHERE order_id=$1`, [ORDER.c]))[0]
  check('P1 发货后包裹置「已发货(1)」', Number(pkgShipped?.package_status) === 1, `status=${pkgShipped?.package_status}`)
  // 无运单号则不生成 ASN
  const notifies0 = await dbQuery(`SELECT id FROM erp_shipment_notify WHERE order_id=$1`, [ORDER.c])
  check('P2-6 无运单号时不生成 ASN（避免空通知）', notifies0.length === 0, `rows=${notifies0.length}`)

  // 造一条带运单号的订单用于 ASN
  await apiA('POST', '/erp/sale/order/batch-logistics-remark', { ids: [ORDER.a], waybillNo: 'ASN' + STAMP })
  await apiA('POST', `/erp/sale/order/${ORDER.a}/ship?warehouseId=1`)
  const notifies1 = await dbQuery(`SELECT id, status, payload FROM erp_shipment_notify WHERE order_id=$1`, [ORDER.a])
  check('P2-6 发货后按运单号幂等生成 ASN 台账', notifies1.length === 1, `rows=${notifies1.length}`)
  const again = await apiA('POST', `/erp/sale/order/${ORDER.a}/ship?warehouseId=1`)
  const notifies2 = await dbQuery(`SELECT id FROM erp_shipment_notify WHERE order_id=$1`, [ORDER.a])
  check('P2-6 重复发货不重复生成（幂等）', notifies2.length === 1, `rows=${notifies2.length}, code=${again.json?.code}`)

  // 未配置的实例 A 先试：只落台账并说明原因，不算失败、不改状态
  const sendA = await apiA('POST', `/erp/sale/logistics/shipment-notify/${notifies1[0].id}/send`)
  check('P2-6 未配置回调时保持台账并说明原因（不算失败）',
    okCode(sendA) && Number(data(sendA)?.status) === 0 && /未配置/.test(data(sendA)?.lastError || ''),
    `${data(sendA)?.status}/${data(sendA)?.lastError}`)
  const sendB = await apiB('POST', `/erp/sale/logistics/shipment-notify/${notifies1[0].id}/send`)
  check('P2-6 已配置回调时发送成功（mock 收到报文）',
    okCode(sendB) && Number(data(sendB)?.status) === 1, `status=${data(sendB)?.status}`)
  check('P2-6 mock 侧确收到 ASN 报文', mockAsnHits.length >= 1, `hits=${mockAsnHits.length}`)
  const page = await apiA('GET', `/erp/sale/logistics/shipment-notify/page?orderNo=${DD}-1-${STAMP}&current=1&size=10`)
  check('P2-6 ASN 台账分页可按订单号查询', Number(data(page)?.total) === 1, `total=${data(page)?.total}`)

  // 为 UI 预留：D 单（保持待发货）+ 一个带运单号的包裹
  await apiA('POST', `/erp/sale/logistics/${ORDER.d}/packages`, {
    waybillNo: 'UIDR' + STAMP, logisticsCompany: CARRIER_NAME, logisticsCompanyId: carrierId,
    packageCount: 1, packageWeight: 2,
  })

  return { carrierId, countAsn: notifies1.length }
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite(ctxData) {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  const errors = []
  page.on('pageerror', e => errors.push(String(e)))

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT_A}${u.pathname}${u.search}` })
  })

  const clickBtn = (re) => page.locator('button').filter({ hasText: re }).first().click()

  async function openPage(url, waitMs = 6000) {
    for (let i = 0; i < 3; i++) {
      await login()
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk); localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, 1])
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(waitMs)
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
    }
  }
  const bodyText = () => page.locator('body').innerText().then(t => t.replace(/\s+/g, ''))

  // ① 新菜单页：物流运费对账（三 Tab）
  await openPage(`${FE}/dispatch/freight-reconcile`, 8000)
  await page.waitForSelector('.ant-tabs-tab', { timeout: 45000 }).catch(() => {})
  await page.waitForTimeout(1500)
  const txt = await bodyText()
  check('UI 物流运费对账页可打开（新菜单 70162）', !txt.includes('页面不存在') && txt.length > 50, page.url())
  check('UI 三个 Tab 齐全（运费规则/运费对账/发货通知）',
    txt.includes('运费规则') && txt.includes('运费对账') && txt.includes('发货通知'))
  const ruleRows = await page.locator('.ant-tabs-tabpane-active tbody tr').count()
  check('UI 运费规则列表渲染（含新增的 E2E 规则）', ruleRows >= 1, `rows=${ruleRows}`)
  await page.screenshot({ path: `${SHOTS}/ui-freight-rule.png`, fullPage: true })

  // 切到「运费对账」→ 汇总卡 + 明细
  await page.locator('.ant-tabs-tab', { hasText: '运费对账' }).first().click()
  await page.waitForTimeout(2500)
  const recTxt = await bodyText()
  check('UI 运费对账渲染统计卡（我方计费/承运商账单/差异）',
    recTxt.includes('我方计费') && recTxt.includes('承运商账单') && recTxt.includes('差异'), recTxt.slice(0, 120))
  await page.screenshot({ path: `${SHOTS}/ui-reconcile.png`, fullPage: true })

  // 切到「发货通知」
  await page.locator('.ant-tabs-tab', { hasText: '发货通知' }).first().click()
  await page.waitForTimeout(2500)
  const notifyTxt = await bodyText()
  check('UI 发货通知列表有 ASN 记录', notifyTxt.includes(DD + '-1-' + STAMP), notifyTxt.slice(0, 160))
  await page.screenshot({ path: `${SHOTS}/ui-notify.png`, fullPage: true })

  // ② 订单处理中心：行内「更多 → 包裹/运单」抽屉（用 D 单：保持「待发货」才在拣货/发货列表里）
  await openPage(`${FE}/dispatch/logistics-ship`, 7000)
  await page.waitForSelector('.ss-grid tbody tr', { timeout: 45000 }).catch(() => {})
  const q = page.locator('input[placeholder="单据编号"]').first()
  await q.fill(`${DD}-4-${STAMP}`)
  await clickBtn(/查\s*询/)
  await page.waitForTimeout(3500)
  const row = page.locator('.ss-grid tbody tr', { hasText: `${DD}-4-${STAMP}` }).first()
  check('UI 拣货/发货列表命中 E2E 单据', (await row.count()) > 0)
  await row.locator('button', { hasText: /^更\s*多$/ }).first().click()
  await page.waitForTimeout(800)
  const menu = (await page.locator('.ant-dropdown-menu').first().innerText().catch(() => '')).replace(/\s+/g, '')
  check('UI 行内「更多」含 包裹/运单', menu.includes('包裹'), menu)
  await page.locator('.ant-dropdown-menu-item', { hasText: '包裹' }).first().click()
  await page.waitForTimeout(2500)
  const drawerTxt = await bodyText()
  check('UI 包裹/运单抽屉打开并列出运单号', drawerTxt.includes('UIDR' + STAMP), drawerTxt.slice(0, 200))
  await page.screenshot({ path: `${SHOTS}/ui-package-drawer.png`, fullPage: true })

  check('UI 无 JS 运行时错误', errors.length === 0, errors.slice(0, 2).join(' | '))
  await browser.close()
}

// ══════════════ 汇总 ══════════════
;(async () => {
  const mock = await startMock()
  try {
    await ensureE2EUser()
    await prepareData()
    await login()
    const ctxData = await apiSuite()
    await uiSuite(ctxData)
  } finally {
    mock.close()
  }
  const pass = results.filter(r => r.ok).length
  console.log(`\n═══ 验收汇总：${pass}/${results.length} ═══`)
  fs.writeFileSync(`${SHOTS}/result.json`, JSON.stringify({
    total: results.length, pass, failed: results.filter(r => !r.ok),
  }, null, 2), 'utf8')
  process.exit(pass === results.length ? 0 : 1)
})().catch(e => {
  console.error('验收脚本异常:', e)
  process.exit(1)
})
