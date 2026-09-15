/*
 * 物流发货（配发收 → 发货业务 → 物流发货，菜单 70155 / menuId 47001 / billType 2341）金标准端到端验证
 *   · 口径依据：《物流发货开发文档》—— 入口直达复用《订单处理中心》并落在「2.拣货/发货」：
 *               37 列（默认 17）/ 查询 16 项 / 功能按钮 6 个 / 行级操作 取消·拣完·发货·更多 /
 *               合计行（销售金额 | 商品数量）/ 行对象=销售订单 XSDD-
 *   · API 验收：拣货/发货分页（区域/司机/打印次数/仓库/自定义字段多条件）+ 合计接口
 *               （同查询条件全量汇总）+ 拣完（明细 picked_quantity 回写 + 主表汇总）+
 *               批量拣完 + 拣完批量发货 → 发货出库（状态流转）
 *   · UI  验收：/dispatch/logistics-ship 直达「2.拣货/发货」+ 16 查询项（含区域）+ 6 按钮 +
 *               默认 17 列表头 + 列配置 37 列（20 列默认隐藏）+ 行级操作 + 合计行 + 区域过滤生效
 *   · DB  核对：erp_sale_order / erp_sale_order_item 真实落库与回写
 *
 * 用法：node tools/e2e-logistics-ship.cjs            （默认后端 5685、前端 5656）
 *      ERP_PORT=5685 FE_URL=http://localhost:5656 node tools/e2e-logistics-ship.cjs
 *
 * 前置：验收实例需包含 erp-sales 最新代码；devdb 已应用 V11.99.0（picked_quantity 列）。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5685)
const SHOTS = 'I:/AI-Ready/tool-results/logistics-ship'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }

// 专用验收账号：并行会话共用 admin 会被 sa-token 互踢（验收中途 401）
const E2E_USER = process.env.E2E_USER || 'e2e_logistics_ship'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

const STAMP = Date.now().toString().slice(-6)
const DD = 'E2ELS'                 // order_no 前缀（造数标记，便于清理与筛选）
const REGION = DD + STAMP          // 区域（唯一，用于 UI/接口过滤）
// 区域二：UI 交互验收专用单（保持未拣完）。注意不能用区域一前缀拼接 —— 后端为 LIKE 匹配
const REGION2 = DD + 'B' + STAMP
const DRIVER = 'E2E司机' + STAMP
const EXT1 = 'EXT' + STAMP
const CUSTOMER_ID = 2              // 客户甲
const WAREHOUSE_ID = 1             // 主仓库

async function dbQuery(sql, params) {
  const client = new Client(DSN)
  await client.connect()
  try {
    const res = await client.query(sql, params)
    return res.rows
  } finally {
    await client.end()
  }
}

/** 幂等创建验收账号（等权 admin：role_id=1 SUPER_ADMIN + 系统租户） */
async function ensureE2EUser() {
  const exist = await dbQuery(`SELECT id FROM sys_user WHERE username = $1`, [E2E_USER])
  if (exist.length > 0) return
  const uid = Number(String(Date.now()).slice(-15) + '001')
  await dbQuery(
    `INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                           user_type, is_super_admin, is_tenant_admin, status, data_scope)
     SELECT $2, 1, 0, now(), now(), $1, password, 'E2E物流发货', 'E2E物流发货',
            1, false, false, 1, 'ALL'
     FROM sys_user WHERE username = 'admin'`, [E2E_USER, uid])
  await dbQuery(`INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
                 VALUES ($1, $2, 1, 1, now())`, [uid + 1, uid])
  await dbQuery(`INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
                 VALUES ($1, $2, 1, true, 1, now(), now())`, [uid + 2, uid])
  console.log(`已创建验收账号 ${E2E_USER}（密码同 admin，id=${uid}）`)
}

// ══════════════ HTTP 基础设施 ══════════════
function safePath(fullPath) {
  const idx = fullPath.indexOf('?')
  if (idx < 0) return encodeURI(fullPath)
  const path = fullPath.slice(0, idx)
  const query = fullPath.slice(idx + 1).split('&').map(kv => {
    const eq = kv.indexOf('=')
    if (eq < 0) return encodeURIComponent(kv)
    let val = kv.slice(eq + 1)
    try { val = decodeURIComponent(val) } catch (e) { /* 未编码则原样 */ }
    return encodeURIComponent(kv.slice(0, eq)) + '=' + encodeURIComponent(val)
  }).join('&')
  return encodeURI(path) + '?' + query
}

function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: safePath('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* blob 等非 JSON */ }
        resolve({ status: res.statusCode, buf, headers: res.headers, json })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

let TOKEN = null

async function api(method, reqPath, body) {
  let r = await rawReq(method, reqPath, body, TOKEN)
  const code = r.json && Number(r.json.code)
  if (r.status === 401 || code === 401) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN)
  }
  return r
}

function data(res) { return res.json ? res.json.data : null }

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER,
    password: E2E_PWD,
    tenantName: '系统租户',
    captcha: code,
    captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) {
    throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300)
      + `\n提示：请用独立账号（默认 ${E2E_USER}/${E2E_PWD}），见 tools/e2e-logistics-ship-user.sql。`)
  }
  TOKEN = token
}

// ══════════════ 验收结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

// ══════════════ 造数（真实落库，供 UI 与接口共用） ══════════════
const ORDER = {}   // { a, b, shipped }
const TODAY = new Date().toISOString().slice(0, 10)

async function prepareData() {
  console.log('\n═══ 〇、造数准备 ═══')
  // 清理上一次遗留（物理删除，标记隔离）
  await dbQuery(`DELETE FROM erp_sale_order_item WHERE order_id IN (SELECT id FROM erp_sale_order WHERE order_no LIKE '${DD}%')`)
  await dbQuery(`DELETE FROM erp_sale_order WHERE order_no LIKE '${DD}%'`)

  const base = Number(String(Date.now()).slice(-15))
  const mk = async (suffix, status, amount, qty, items, extra) => {
    const id = base + suffix
    const region = extra.region || REGION
    await dbQuery(
      `INSERT INTO erp_sale_order (id, tenant_id, deleted, create_time, update_time, order_no, order_date, sale_type, status,
        customer_id, customer_name, customer_code, warehouse_id, warehouse_name, picking_warehouse, collection_location,
        product_amount, bill_amount, total_quantity, picked_quantity, shipped_quantity, unshipped_quantity,
        region, driver_name, delivery_method, logistics_company, salesman_name, print_count, summary, pickup_address,
        expected_ship_time, order_source, order_remark, ext_num1, ext_num2, ext_text1, ext_text2, ext_text3,
        receiver_name, receiver_phone, shipping_address)
       VALUES ($1,1,0,now(),now(),$2,$3,0,$4, $5,$6,'C0002',$7,$8,$8,'A-01',
         $9,$9,$10,0,0,$10, $11,$12,'物流','顺丰速运','E2E经手人',$13,$14,'E2E提货地址',
         now(), 1, 'E2E单据备注', 11, 22, $15, '文本4', '文本5',
         'E2E收货人','13800000000','E2E收货地址')`,
      [id, `${DD}-${suffix}-${STAMP}`, TODAY, status, CUSTOMER_ID, '客户甲', WAREHOUSE_ID, '主仓库',
        amount, qty, region, DRIVER, extra.printCount, extra.summary, EXT1])
    let lineNo = 1
    for (const it of items) {
      await dbQuery(
        `INSERT INTO erp_sale_order_item (order_id, tenant_id, line_no, product_id, product_code, product_name, unit,
          quantity, shipped_quantity, shipped_quantity_detail, picked_quantity, unit_price, amount, warehouse_id)
         VALUES ($1,1,$2,$3,$4,$5,'件',$6,0,0,0,10,$7,$8)`,
        [id, lineNo++, base + 1000 + lineNo, 'P' + lineNo + STAMP, 'E2E商品' + lineNo + '-' + STAMP,
          it.qty, Number(it.qty) * 10, WAREHOUSE_ID])
    }
    return id
  }

  // A：状态 2 待发货，2 行明细 1+2=3 件、金额 300.50 → 供单张「拣完 / 发货」验收
  ORDER.a = await mk(1, 2, 300.50, 3, [{ qty: 1 }, { qty: 2 }], { printCount: 0, summary: 'E2E摘要A' })
  // B：状态 2 待发货，1 行明细 2 件、金额 202.50 → 供「批量拣完 / 拣完批量发货」验收
  ORDER.b = await mk(2, 2, 202.50, 2, [{ qty: 2 }], { printCount: 0, summary: 'E2E摘要B' })
  // C：状态 4 已发货 → 不应出现在拣货/发货列表，且不允许拣完
  ORDER.shipped = await mk(3, 4, 100.00, 1, [{ qty: 1 }], { printCount: 0, summary: 'E2E摘要C' })
  // D：状态 2 待发货（独立区域，保持未拣完）→ 供 UI 端「拣完」真实回写验收
  ORDER.d = await mk(4, 2, 100.00, 1, [{ qty: 1 }], { printCount: 0, summary: 'E2E摘要D', region: REGION2 })

  console.log(`  造数：A=${ORDER.a} B=${ORDER.b} 已发货单=${ORDER.shipped} UI交互单=${ORDER.d}`)
  console.log(`  区域一=${REGION}（合计 503.00 / 5）；区域二=${REGION2}`)
}

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')
  const base = `/erp/sale/order`

  // 1) 拣货/发货分页：状态 in (2,3)
  const page1 = await api('GET', `${base}/center/picking-shipping?tenantId=1&pageNum=1&pageSize=50&region=${encodeURIComponent(REGION)}`)
  const rows1 = data(page1)?.records || []
  check('拣货/发货分页返回 200', page1.status === 200 && page1.json?.code === 200, `status=${page1.status}`)
  check('区域过滤命中 2 张待发货单', rows1.length === 2, `records=${rows1.length}`)
  check('已发货(status=4)单据不在拣货/发货列表', !rows1.some(r => String(r.id) === String(ORDER.shipped)))
  check('行对象为销售订单编号 XSDD/E2ELS 口径', rows1.every(r => String(r.orderNo || '').startsWith(DD)), rows1.map(r => r.orderNo).join(','))

  // 2) 多条件过滤真实生效
  const byDriver = await api('GET', `${base}/center/picking-shipping?tenantId=1&pageNum=1&pageSize=50&region=${encodeURIComponent(REGION)}&driverName=${encodeURIComponent(DRIVER)}`)
  check('配送司机条件生效', (data(byDriver)?.records || []).length === 2, `records=${(data(byDriver)?.records || []).length}`)
  const byDriverMiss = await api('GET', `${base}/center/picking-shipping?tenantId=1&pageNum=1&pageSize=50&region=${encodeURIComponent(REGION)}&driverName=E2E不存在司机`)
  check('配送司机条件不匹配时为空', (data(byDriverMiss)?.records || []).length === 0)
  const byWarehouse = await api('GET', `${base}/center/picking-shipping?tenantId=1&pageNum=1&pageSize=50&region=${encodeURIComponent(REGION)}&warehouseName=${encodeURIComponent('主仓库')}`)
  check('仓库条件生效', (data(byWarehouse)?.records || []).length === 2)
  const byPrint = await api('GET', `${base}/center/picking-shipping?tenantId=1&pageNum=1&pageSize=50&region=${encodeURIComponent(REGION)}&printCount=0`)
  check('打印次数条件生效', (data(byPrint)?.records || []).length === 2)
  const byExt = await api('GET', `${base}/center/picking-shipping?tenantId=1&pageNum=1&pageSize=50&region=${encodeURIComponent(REGION)}&extText1=${encodeURIComponent(EXT1)}`)
  check('表头自定义字段3(文本)条件生效', (data(byExt)?.records || []).length === 2)
  const byRegionMiss = await api('GET', `${base}/center/picking-shipping?tenantId=1&pageNum=1&pageSize=50&region=E2ELS不存在`)
  check('区域条件不匹配时为空', (data(byRegionMiss)?.records || []).length === 0)

  // 3) 合计接口（同查询条件全量汇总）
  const sum = await api('GET', `${base}/center/picking-shipping-summary?tenantId=1&region=${encodeURIComponent(REGION)}`)
  const sumD = data(sum) || {}
  check('合计接口返回 200', sum.status === 200 && sum.json?.code === 200, `status=${sum.status}`)
  check('销售金额合计 = 503.00（300.50 + 202.50）', Number(sumD.productAmount) === 503, `productAmount=${sumD.productAmount}`)
  check('商品数量合计 = 5（3 + 2）', Number(sumD.totalQuantity) === 5, `totalQuantity=${sumD.totalQuantity}`)
  check('合计口径单据数 = 2', Number(sumD.totalOrders) === 2, `totalOrders=${sumD.totalOrders}`)
  const sumRegion2 = await api('GET', `${base}/center/picking-shipping-summary?tenantId=1&region=E2ELS不存在`)
  check('合计随查询条件联动（不匹配区域为 0）', Number((data(sumRegion2) || {}).productAmount) === 0)

  // 4) 拣完（单张）：明细 picked_quantity 回写 + 主表汇总
  const pickA = await api('POST', `${base}/${ORDER.a}/pick-complete`)
  check('拣完接口返回 200', pickA.status === 200 && pickA.json?.code === 200, JSON.stringify(pickA.json).slice(0, 120))
  const itemsA = await dbQuery(`SELECT quantity, picked_quantity FROM erp_sale_order_item WHERE order_id=$1 ORDER BY line_no`, [ORDER.a])
  check('明细已拣货数量回写 = 订货数量', itemsA.every(r => Number(r.picked_quantity) === Number(r.quantity)),
    itemsA.map(r => `${r.picked_quantity}/${r.quantity}`).join(','))
  const orderA = (await dbQuery(`SELECT picked_quantity, picking_warehouse FROM erp_sale_order WHERE id=$1`, [ORDER.a]))[0]
  check('主表已拣货数量汇总 = 3', Number(orderA.picked_quantity) === 3, `picked_quantity=${orderA.picked_quantity}`)
  check('拣货仓库按订单仓库兜底', orderA.picking_warehouse === '主仓库', `picking_warehouse=${orderA.picking_warehouse}`)
  const rowsAfterPick = (data(await api('GET', `${base}/center/picking-shipping?tenantId=1&pageNum=1&pageSize=50&region=${encodeURIComponent(REGION)}`)) || {}).records || []
  const rowA = rowsAfterPick.find(r => String(r.id) === String(ORDER.a))
  check('列表返回未拣货数量 = 0（拣货/发货数量分离跟踪）', Number(rowA?.unpickedQuantity) === 0, `unpicked=${rowA?.unpickedQuantity}`)
  check('列表返回已拣货数量 = 3', Number(rowA?.pickedQuantity) === 3, `picked=${rowA?.pickedQuantity}`)

  // 5) 拣完状态门控
  const pickShipped = await api('POST', `${base}/${ORDER.shipped}/pick-complete`)
  check('已发货单据拣完被拒（状态门控）', Number(pickShipped.json?.code) !== 200, JSON.stringify(pickShipped.json).slice(0, 120))

  // 6) 批量拣完
  const batchPick = await api('POST', `${base}/batch-pick-complete`, [ORDER.b])
  check('批量拣完返回成功单数 = 1', Number(data(batchPick)) === 1, `data=${data(batchPick)}`)
  const orderB = (await dbQuery(`SELECT picked_quantity FROM erp_sale_order WHERE id=$1`, [ORDER.b]))[0]
  check('批量拣完后 B 单已拣货数量 = 2', Number(orderB.picked_quantity) === 2, `picked_quantity=${orderB.picked_quantity}`)

  // 7) 发货（拣完批量发货的第二步）：状态流转 + 出库单生成
  const shipA = await api('POST', `${base}/${ORDER.a}/ship?warehouseId=${WAREHOUSE_ID}`)
  check('发货接口返回 200', shipA.status === 200 && shipA.json?.code === 200, JSON.stringify(shipA.json).slice(0, 120))
  const afterShip = (await dbQuery(`SELECT status, shipped_quantity, unshipped_quantity FROM erp_sale_order WHERE id=$1`, [ORDER.a]))[0]
  check('发货后状态流转为 4（发货完成）', Number(afterShip.status) === 4, `status=${afterShip.status}`)
  check('发货后已发数量 = 3', Number(afterShip.shipped_quantity) === 3, `shipped=${afterShip.shipped_quantity}`)
  const rowsFinal = (data(await api('GET', `${base}/center/picking-shipping?tenantId=1&pageNum=1&pageSize=50&region=${encodeURIComponent(REGION)}`)) || {}).records || []
  check('发货后单据移出拣货/发货列表', !rowsFinal.some(r => String(r.id) === String(ORDER.a)), `remaining=${rowsFinal.length}`)
  check('拣完记录不因发货被清空', Number((await dbQuery(`SELECT picked_quantity FROM erp_sale_order WHERE id=$1`, [ORDER.a]))[0].picked_quantity) === 3)

  // 8) 物流/备注（ql361 `OrderRemarks` 弹窗实测 15 字段；空值不改动）
  const WAYBILL = 'SF' + STAMP
  const lr = await api('POST', `${base}/batch-logistics-remark`, {
    ids: [ORDER.b],
    waybillNo: WAYBILL,
    logisticsCompany: '顺丰速运',
    driverName: 'E2E司机' + STAMP,
    receiverName: 'E2E收货人New',
    receiverPhone: '13900000001',
    salesmanName: 'E2E经手人New',
    shippingAddress: 'E2E新收货地址',
    saleType: 1,
    extNum1: 11.5,
    extNum2: 22.5,
    extText1: 'T1' + STAMP,
    extText2: 'T2' + STAMP,
    extText3: 'T3' + STAMP,
    orderRemark: 'E2E物流备注' + STAMP,
  })
  check('物流/备注批量更新返回 200', lr.status === 200 && lr.json?.code === 200, JSON.stringify(lr.json).slice(0, 160))
  check('返回实际更新单数 = 1', Number(data(lr)) === 1, `data=${data(lr)}`)
  const lrRow = (await dbQuery(
    `SELECT waybill_no, logistics_company, driver_name, receiver_name, receiver_phone, salesman_name,
            shipping_address, sale_type, ext_num1, ext_num2, ext_text1, ext_text2, ext_text3,
            order_remark, delivery_method
       FROM erp_sale_order WHERE id=$1`, [ORDER.b]))[0]
  check('运单号落库', lrRow.waybill_no === WAYBILL, `waybill_no=${lrRow.waybill_no}`)
  check('物流公司落库', lrRow.logistics_company === '顺丰速运', `logistics_company=${lrRow.logistics_company}`)
  check('收货人/联系电话/收货地址落库',
    lrRow.receiver_name === 'E2E收货人New' && lrRow.receiver_phone === '13900000001' && lrRow.shipping_address === 'E2E新收货地址',
    `${lrRow.receiver_name}/${lrRow.receiver_phone}/${lrRow.shipping_address}`)
  check('司机/经手人落库', lrRow.driver_name === 'E2E司机' + STAMP && lrRow.salesman_name === 'E2E经手人New',
    `${lrRow.driver_name}/${lrRow.salesman_name}`)
  check('销售类型落库', Number(lrRow.sale_type) === 1, `sale_type=${lrRow.sale_type}`)
  check('自定义字段1-5 落库',
    Number(lrRow.ext_num1) === 11.5 && Number(lrRow.ext_num2) === 22.5
    && lrRow.ext_text1 === 'T1' + STAMP && lrRow.ext_text2 === 'T2' + STAMP && lrRow.ext_text3 === 'T3' + STAMP,
    `${lrRow.ext_num1}/${lrRow.ext_num2}/${lrRow.ext_text1}/${lrRow.ext_text2}/${lrRow.ext_text3}`)
  check('单据备注落库', lrRow.order_remark === 'E2E物流备注' + STAMP, `order_remark=${lrRow.order_remark}`)
  check('未传字段保持原值（配送方式仍为「物流」）', lrRow.delivery_method === '物流', `delivery_method=${lrRow.delivery_method}`)

  // 空值不改动：只传运单号
  const lr2 = await api('POST', `${base}/batch-logistics-remark`, { ids: [ORDER.b], waybillNo: WAYBILL + 'X' })
  const lrRow2 = (await dbQuery(`SELECT waybill_no, logistics_company, receiver_name FROM erp_sale_order WHERE id=$1`, [ORDER.b]))[0]
  check('空值不改动其余字段（只改运单号）',
    lr2.json?.code === 200 && lrRow2.waybill_no === WAYBILL + 'X' && lrRow2.logistics_company === '顺丰速运'
    && lrRow2.receiver_name === 'E2E收货人New',
    `${lrRow2.waybill_no}/${lrRow2.logistics_company}/${lrRow2.receiver_name}`)

  // 校验①：所选单据状态不一致 → 整批拒绝
  const lrMixed = await api('POST', `${base}/batch-logistics-remark`, { ids: [ORDER.a, ORDER.b], waybillNo: 'MIX' })
  check('批量所选单据状态不一致被拒（ql361 口径）',
    Number(lrMixed.json?.code) !== 200 && /状态相同/.test(lrMixed.json?.message || ''), lrMixed.json?.message)

  // 校验②：配送中（status ≥ 3）不允许改配送方式
  const lrDelivering = await api('POST', `${base}/batch-logistics-remark`, { ids: [ORDER.a], deliveryMethod: '自提' })
  const aMethod = (await dbQuery(`SELECT delivery_method FROM erp_sale_order WHERE id=$1`, [ORDER.a]))[0].delivery_method
  check('配送中订单不能修改配送方式（且未落库）',
    Number(lrDelivering.json?.code) !== 200 && /配送方式/.test(lrDelivering.json?.message || '') && aMethod === '物流',
    `${lrDelivering.json?.message} / delivery_method=${aMethod}`)

  // 校验③：空请求（未填任何字段）被拒
  const lrEmpty = await api('POST', `${base}/batch-logistics-remark`, { ids: [ORDER.b] })
  check('未填任何字段时整批拒绝', Number(lrEmpty.json?.code) !== 200, lrEmpty.json?.message)

  return rowsFinal
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  // antd 会给「无图标 + 恰好两个汉字」的按钮插空格（查询→「查 询」），统一用正则匹配
  const clickBtn = (re, scope) => (scope || page).locator('button').filter({ hasText: re }).first().click()

  // /api 转发到验收实例（前端 dev server 代理指向别的后端）
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  async function openPage(url, waitMs = 6000) {
    for (let attempt = 0; attempt < 4; attempt++) {
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

  // ── 1. 入口直达：物流发货 → 订单处理中心 2.拣货/发货 ──
  await openPage(`${FE}/dispatch/logistics-ship`)
  const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())
  const activeMain = await page.locator('.main-tabs .tab-item.active').first().innerText().catch(() => '')
  check('直达落在「2.拣货/发货」阶段', activeMain.replace(/\s+/g, '').includes('拣货/发货'), `active=${activeMain}`)
  check('URL 携带 tab=picking', page.url().includes('tab=picking'), page.url())
  const mainTabsText = (await page.locator('.main-tabs .tab-item').allInnerTexts()).join('|').replace(/\s+/g, '')
  check('主 Tab 命名 = 全部 / 1.待审核 / 2.拣货/发货',
    mainTabsText.includes('全部') && mainTabsText.includes('1.待审核') && mainTabsText.includes('2.拣货/发货'), mainTabsText)

  // ── 2. 功能按钮 6 个 ──
  for (const b of ['刷新', '批量打印', '商品汇总', '拣完批量发货', '物流备注', '配置']) {
    check(`工具栏含「${b}」`, bodyText.includes(b.replace(/\s+/g, '')))
  }
  check('拣货/发货视图无统计卡片（对齐 ql361 实测：仅查询区 + 表格 + 合计行）',
    (await page.locator('.stat-card').count()) === 0, `stat-card=${await page.locator('.stat-card').count()}`)

  // ── 3. 查询条件 16 项（前 9 项直接可见，其余点「更多条件」展开） ──
  await page.locator('.search-more-toggle button').first().click().catch(() => {})
  await page.waitForTimeout(500)
  const QUERY_FIELDS = ['单据日期', '客户', '经手人', '仓库', '单据编号', '提交时间', '打印次数', '区域',
    '物流公司', '配送方式', '配送司机', '表头自定义字段1(数字)', '表头自定义字段2(数字)',
    '表头自定义字段3(文本)', '表头自定义字段4(文本)', '表头自定义字段5(文本)']
  let queryHit = 0
  for (const q of QUERY_FIELDS) {
    const n = await page.locator(`input[placeholder="${q}"]`).count()
    if (n > 0) queryHit++
  }
  check(`查询条件 16 项齐全（含区域）`, queryHit === QUERY_FIELDS.length, `命中=${queryHit}/${QUERY_FIELDS.length}`)

  // ── 4. 默认表头 17 个业务列（+ 行号/勾选/操作 系统列） ──
  const DEFAULT_COLS = ['单据日期', '单据编号', '单据状态', '打印次数', '拣货仓库', '集货位', '客户', '销售金额',
    '商品数量', '预计发货时间', '经手人', '摘要', '提货地址', '配送方式', '物流公司', '单据来源', '单据备注']
  const HIDDEN_COLS = ['结算状态', '体积(m³)', '重量(kg)', '商品行数', '已拣货数量', '未拣货数量', '已发货数量',
    '未发货数量', '收货人', '联系电话', '收货地址', '配送司机', '制单时间', '表头自定义字段1(数字)',
    '表头自定义字段2(数字)', '表头自定义字段3(文本)', '表头自定义字段4(文本)', '表头自定义字段5(文本)', '排序', '排序值']
  const headerTexts = (await page.locator('.ss-grid thead th').allInnerTexts()).map(t => t.replace(/\s+/g, '')).filter(Boolean)
  const missDefault = DEFAULT_COLS.filter(c => !headerTexts.includes(c.replace(/\s+/g, '')))
  check('默认显示 17 个业务列齐全', missDefault.length === 0, missDefault.length ? `缺:${missDefault.join(',')}` : `表头=${headerTexts.length}`)
  const leakHidden = HIDDEN_COLS.filter(c => headerTexts.includes(c.replace(/\s+/g, '')))
  check('20 个默认隐藏列不出现在表头', leakHidden.length === 0, leakHidden.length ? `泄漏:${leakHidden.join(',')}` : '')
  check('表头含系统列（勾选 / 行号齿轮 / 操作）',
    (await page.locator('.ss-checkbox-header').count()) > 0
    && (await page.locator('.th-settings-btn').count()) > 0
    && headerTexts.includes('操作'),
    `表头单元格=${headerTexts.length}`)

  // ── 5. 列配置：37 列全部可配置（系统列不参与） ──
  await page.locator('.th-settings-btn').first().click()
  await page.waitForTimeout(800)
  const cfgRows = await page.evaluate(() =>
    [...document.querySelectorAll('.col-setting-row')].map(r => ({
      // 标题节点含拖拽手柄字符（⠿），去噪后再比对
      title: (r.querySelector('.col-setting-title')?.textContent || '').replace(/[⠿\s]/g, ''),
      checked: r.querySelector('input[type=checkbox]')?.checked === true,
    })))
  check('列配置弹窗打开且含「个人配置 / 全局配置」双 Tab',
    (await page.locator('.col-config-tabs').count()) > 0)
  check('列配置共 37 个业务列', cfgRows.length === 37, `配置行=${cfgRows.length}`)
  check('列配置无空白（系统列已剔除）', cfgRows.every(r => r.title.length > 0))
  const cfgAll = cfgRows.map(r => r.title.replace(/\s+/g, ''))
  const missCfg = [...DEFAULT_COLS, ...HIDDEN_COLS].filter(c => !cfgAll.includes(c.replace(/\s+/g, '')))
  check('37 列清单与对标一致', missCfg.length === 0, missCfg.length ? `缺:${missCfg.join(',')}` : '')
  check('默认勾选 17 列 / 未勾选 20 列',
    cfgRows.filter(r => r.checked).length === 17 && cfgRows.filter(r => !r.checked).length === 20,
    `勾选=${cfgRows.filter(r => r.checked).length}`)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(400)

  // ── 6. 区域过滤（区域一：API 已拣完的 B 单）+ 合计行 + 行级操作 ──
  const regionInput = page.locator(`input[placeholder="区域"]`).first()
  await regionInput.fill(REGION)
  await clickBtn(/查\s*询/)
  await page.waitForTimeout(3000)
  const gridText = (await page.locator('.ss-grid').innerText()).replace(/\s+/g, '')
  check('区域查询命中 E2E 单据（区域一仅剩 B 单）', gridText.includes(`${DD}-2-${STAMP}`), `${DD}-2-${STAMP}`)
  check('已发货单据不在拣货/发货列表', !gridText.includes(`${DD}-3-${STAMP}`))
  check('其它区域单据被过滤（区域二 D 单不出现）', !gridText.includes(`${DD}-4-${STAMP}`))

  const summaryText = await page.locator('.ss-summary-tr').first().innerText().catch(() => '')
  check('底部合计行存在', (await page.locator('.ss-summary-tr').count()) > 0)
  check('合计行含「合计」标签', summaryText.replace(/\s+/g, '').includes('合计'), summaryText.slice(0, 80))
  check('合计行销售金额 = 202.5（仅剩 B 单）', summaryText.replace(/\s+/g, '').includes('202.5'), summaryText.replace(/\s+/g, '').slice(0, 120))

  const rowB = page.locator('.ss-grid tbody tr', { hasText: `${DD}-2-${STAMP}` }).first()
  const rowActions = (await rowB.innerText()).replace(/\s+/g, '')
  for (const a of ['取消', '拣完', '发货', '更多']) {
    check(`行级操作含「${a}」`, rowActions.includes(a), rowActions.slice(0, 140))
  }
  check('单据状态以 tag 口径显示（待发货）', rowActions.includes('待发货'))
  check('单据来源显示为文字（销售订单）', rowActions.includes('销售订单'))
  check('API 已拣完的单「拣完」按钮置灰（未拣货数量为 0）',
    await rowB.locator('button', { hasText: /^拣\s*完$/ }).first().isDisabled().catch(() => false))
  await page.screenshot({ path: `${SHOTS}/ui-picking-tab.png`, fullPage: false })

  // ── 7. 「更多」下拉：查看 / 打印 / 物流·备注（ql361 实测行内动作含 物流/备注） ──
  await rowB.locator('button', { hasText: /^更\s*多$/ }).first().click()
  await page.waitForTimeout(600)
  const menuText = (await page.locator('.ant-dropdown-menu').first().innerText().catch(() => '')).replace(/\s+/g, '')
  check('「更多」下拉含 查看 / 打印 / 物流·备注',
    menuText.includes('查看') && menuText.includes('打印') && menuText.includes('物流'), menuText)
  await page.screenshot({ path: `${SHOTS}/ui-more-menu.png`, fullPage: false })
  await page.keyboard.press('Escape')
  await page.waitForTimeout(400)

  // ── 7.1 行内「更多 → 物流/备注」弹窗：15 字段 + 真实落库 ──
  await rowB.locator('button', { hasText: /^更\s*多$/ }).first().click()
  await page.waitForTimeout(600)
  await page.locator('.ant-dropdown-menu-item', { hasText: '物流' }).first().click()
  await page.waitForTimeout(900)
  const lrLabels = (await page.locator('.ant-modal-content label').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
  const LR_FIELDS = ['配送方式', '司机', '运单号', '物流公司', '收货人', '联系电话', '经手人', '销售类型',
    '自定义字段1(数字)', '自定义字段2(数字)', '自定义字段3(文本)', '自定义字段4(文本)', '自定义字段5(文本)',
    '收货地址', '单据备注']
  const missLr = LR_FIELDS.filter(f => !lrLabels.includes(f))
  check('物流/备注弹窗 15 字段齐全（ql361 OrderRemarks 口径）',
    missLr.length === 0, missLr.length ? `缺:${missLr.join(',')}` : `字段=${lrLabels.length}`)
  const UIWAY = 'UI' + STAMP
  await page.locator('.ant-modal-content input[placeholder="运单号 / 物流单号"]').first().fill(UIWAY)
  // 物流公司已升级为「档案引用 + 可手输」的 AutoComplete：placeholder 不落在 <input> 上，
  // 因此按「表单项」定位（标签 + 其内输入框），不要再依赖 placeholder 选择器
  await page.locator('.ant-modal-content .ant-form-item', { hasText: '物流公司' })
    .locator('input').first().fill('圆通速递')
  await page.screenshot({ path: `${SHOTS}/ui-logistics-remark.png`, fullPage: false })
  await page.locator('.ant-modal-footer button', { hasText: /保\s*存|确\s*定/ }).first().click()
  await page.waitForTimeout(2600)
  const uiLrRow = (await dbQuery(`SELECT waybill_no, logistics_company, receiver_name FROM erp_sale_order WHERE id=$1`, [ORDER.b]))[0]
  check('行内物流/备注保存后落库（运单号 + 物流公司）',
    uiLrRow.waybill_no === UIWAY && uiLrRow.logistics_company === '圆通速递',
    `${uiLrRow.waybill_no}/${uiLrRow.logistics_company}`)
  check('弹窗未填字段不覆盖原值（收货人保持 API 阶段写入值）',
    uiLrRow.receiver_name === 'E2E收货人New', `receiver_name=${uiLrRow.receiver_name}`)

  // ── 8. 「拣完」真实回写（区域二 D 单，未拣完） ──
  await regionInput.fill(REGION2)
  await clickBtn(/查\s*询/)
  await page.waitForTimeout(3000)
  const rowD = page.locator('.ss-grid tbody tr', { hasText: `${DD}-4-${STAMP}` }).first()
  check('区域二查询命中 D 单', (await rowD.count()) > 0, `${DD}-4-${STAMP}`)
  check('未拣完单据「拣完」按钮可点击', !(await rowD.locator('button', { hasText: /^拣\s*完$/ }).first().isDisabled()))
  await rowD.locator('button', { hasText: /^拣\s*完$/ }).first().click()
  await page.waitForTimeout(600)
  await page.locator('.ant-modal-confirm button').filter({ hasText: /确认拣完/ }).first().click()
  await page.waitForTimeout(2500)
  const pickedD = (await dbQuery(`SELECT picked_quantity FROM erp_sale_order WHERE id=$1`, [ORDER.d]))[0]
  check('UI「拣完」真实回写已拣货数量 = 1', Number(pickedD.picked_quantity) === 1, `picked=${pickedD.picked_quantity}`)
  const rowDAfter = page.locator('.ss-grid tbody tr', { hasText: `${DD}-4-${STAMP}` }).first()
  check('拣完后「拣完」按钮置灰（未拣货数量归零）',
    await rowDAfter.locator('button', { hasText: /^拣\s*完$/ }).first().isDisabled().catch(() => false))

  // ── 9. 「拣完批量发货」按钮可触发（勾选 → 二次确认，不落地以保留数据） ──
  await rowDAfter.locator('input.ss-checkbox').first().check().catch(() => {})
  await page.waitForTimeout(300)
  await clickBtn(/拣完批量发货/)
  await page.waitForTimeout(800)
  const confirmText = (await page.locator('.ant-modal-confirm').first().innerText().catch(() => '')).replace(/\s+/g, '')
  check('「拣完批量发货」弹出批量确认（拣货完成 + 发货）', confirmText.includes('拣完批量发货') && confirmText.includes('发货'), confirmText.slice(0, 80))
  await page.locator('.ant-modal-confirm button').filter({ hasText: /取\s*消/ }).first().click()
  await page.waitForTimeout(400)

  // ── 9. 页面配置弹窗：查询条件 16 / 功能按钮 6 ──
  await clickBtn(/^配\s*置$/)
  await page.waitForTimeout(1000)
  const cfgTabs = (await page.locator('.config-tabs .ant-tabs-tab').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
  check('页面配置弹窗含「查询条件 / 功能按钮」Tab',
    cfgTabs.includes('查询条件') && cfgTabs.includes('功能按钮'), cfgTabs.join('|'))
  const cfgQueryRows = (await page.locator('.config-tabs .ant-tabs-tabpane-active .config-table tbody tr').allInnerTexts())
    .map(t => t.replace(/[⠿\s]/g, ''))
  check('页面配置-查询条件 16 项', cfgQueryRows.length === 16, `行数=${cfgQueryRows.length}`)
  const missCfgQ = QUERY_FIELDS.filter(q => !cfgQueryRows.some(r => r.startsWith(q.replace(/\s+/g, ''))))
  check('页面配置-查询条件与查询区同源（含区域等 16 项）', missCfgQ.length === 0, missCfgQ.join(','))
  await page.locator('.config-tabs .ant-tabs-tab', { hasText: '功能按钮' }).first().click()
  await page.waitForTimeout(600)
  const cfgBtnRows = (await page.locator('.config-tabs .ant-tabs-tabpane-active .config-table tbody tr').allInnerTexts())
    .map(t => t.replace(/\s+/g, ''))
  check('页面配置-功能按钮 6 项', cfgBtnRows.length === 6, `行数=${cfgBtnRows.length}`)
  const missCfgB = ['刷新', '批量打印', '商品汇总', '拣完批量发货', '物流备注', '配置']
    .filter(b => !cfgBtnRows.some(r => r.startsWith(b)))
  check('页面配置-功能按钮与工具栏同源', missCfgB.length === 0, missCfgB.join(','))
  await page.screenshot({ path: `${SHOTS}/ui-page-config.png`, fullPage: false })

  await browser.close()
}

// ══════════════ 主流程 ══════════════
async function main() {
  console.log('═══ 物流发货金标准 E2E（后端 :' + PORT + ' / 前端 ' + FE + '）═══')
  await ensureE2EUser()
  await login()
  console.log('登录成功')
  await prepareData()
  await apiSuite()
  await uiSuite()

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 结果：${pass}/${results.length} 通过${fail ? `，${fail} 失败` : ''} ═══`)
  if (fail) {
    console.log('失败项：')
    results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name}${r.detail ? ' — ' + r.detail : ''}`))
  }
  process.exit(fail ? 1 : 0)
}

main().catch(e => { console.error('E2E 异常:', e); process.exit(1) })
