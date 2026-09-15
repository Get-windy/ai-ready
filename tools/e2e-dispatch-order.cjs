/*
 * 配送单（配送 → 配送业务 → 配送单[历史]，菜单 80760，双入口）金标准端到端验证
 *   · 口径依据：《配送单开发文档》§2 列表（16 查询条件 + 9 段快捷时间 + 24 列默认隐藏 4 列
 *               + 页面配置弹窗 + 序号齿轮列配置 + 合计行 + 经典分页）
 *               §3 表单（基本信息分组 / 商品明细 / 资金区 / 配置 3 Tab）
 *               §4 单号 PSD-YYYYMMDD-序号 / 状态机 0-8 / 上下游
 *   · API 验收：next-no（号段）/ save（头+明细，含退货口径）/ detail / page（多条件）/
 *               page-detail（按明细）/ audit·unaudit（状态机）/ delete（可删规则）/
 *               print（打印次数）/ export（真实 xlsx）/ filter-options（选择器候选）
 *   · UI  验收：列表骨架（CategoryListLayout + 表头齿轮列配置 + PageConfigPanel）+
 *               工具栏 / 查询区 / 26 列表头 / 默认隐藏 4 列 / 勾选 / 合计行 / 经典分页 /
 *               状态三值 tag / 条件过滤真实生效 / 表单页（编号号段 / 明细 / 配置 3 Tab /
 *               保存草稿真实落库 / 编辑回填 / 打印(F8) 回写打印次数）
 *
 * 用法：node tools/e2e-dispatch-order.cjs            （默认后端 5690、前端 5656）
 *      ERP_PORT=5690 FE_URL=http://localhost:5656 node tools/e2e-dispatch-order.cjs
 *
 * 前置：验收实例需包含 DMS 模块；devdb 需已应用 dms_task 台账列迁移（V11.199.0）。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5690)
const SHOTS = 'I:/AI-Ready/tool-results/dispatch-order'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const DSN = {
  host: 'localhost',
  port: 5432,
  database: 'devdb',
  user: 'devuser',
  password: 'devuser123',
}

// 专用验收账号：与并行会话（配送查询共用 admin）隔离，避免 sa-token 互踢
const E2E_USER = process.env.E2E_USER || 'e2e_dispatch_order'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

const STAMP = Date.now().toString().slice(-6)
const MARK = 'E2EDO' + STAMP

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
  const exist = await dbQuery(`SELECT id::text AS uid FROM sys_user WHERE username = $1`, [E2E_USER])
  let uid
  if (exist.length > 0) {
    uid = exist[0].uid
  } else {
    // 主键为雪花 ID（超出 JS 安全整数），由 DB 在专用号段内分配并全程以字符串传递
    const idRow = await dbQuery(
      `SELECT (COALESCE(MAX(id), 2099000000000002000) + 1)::text AS uid
       FROM sys_user WHERE id >= 2099000000000002000 AND id < 2099000000000003000`)
    uid = idRow[0].uid
    await dbQuery(
      `INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                             user_type, is_super_admin, is_tenant_admin, status, data_scope)
       SELECT $2::bigint, 1, 0, now(), now(), $1::varchar, password, 'E2E配送单', 'E2E配送单',
              1, false, false, 1, 'ALL'
       FROM sys_user WHERE username = 'admin'`, [E2E_USER, uid])
    console.log(`已创建验收账号 ${E2E_USER}（密码同 admin，id=${uid}）`)
  }
  const role = await dbQuery(`SELECT 1 FROM sys_user_role WHERE user_id = $1::bigint`, [uid])
  if (role.length === 0) {
    await dbQuery(`INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
                   VALUES (($1::bigint + 1)::bigint, $1::bigint, 1, 1, now())`, [uid])
  }
  const ten = await dbQuery(`SELECT 1 FROM sys_user_tenant WHERE user_id = $1::bigint`, [uid])
  if (ten.length === 0) {
    await dbQuery(`INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
                   VALUES (($1::bigint + 2)::bigint, $1::bigint, 1, true, 1, now(), now())`, [uid])
  }
}

// ══════════════ HTTP 基础设施 ══════════════
function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
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

function data(res) {
  return res.json ? res.json.data : null
}

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
      + `\n提示：请用独立账号（默认 ${E2E_USER}/${E2E_PWD}），见 tools/e2e-dispatch-order-user.sql。`)
  }
  TOKEN = token
}

// ══════════════ 验收结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

// ══════════════ 一、接口验收 ══════════════
let RIDER = null, VEHICLE = null, DELIVERYMAN = null, ROUTE = null

async function prepareResources() {
  console.log('\n═══ 〇、造数准备 ═══')
  await dbQuery(`DELETE FROM dms_task_item WHERE task_id IN (SELECT id FROM dms_task WHERE customer_name LIKE 'E2E配送单客户%')`)
  await dbQuery(`DELETE FROM dms_task WHERE customer_name LIKE 'E2E配送单客户%'`)

  const r1 = await dbQuery(
    `INSERT INTO dms_rider (tenant_id, rider_type, real_name, phone, status, verify_status, deleted, create_time, update_time)
     VALUES (1, 2, $1, '13900000001', 1, 1, 0, now(), now()) RETURNING id`, ['E2E单司机' + STAMP])
  RIDER = r1[0].id
  const r2 = await dbQuery(
    `INSERT INTO dms_rider (tenant_id, rider_type, real_name, phone, status, verify_status, deleted, create_time, update_time)
     VALUES (1, 1, $1, '13900000002', 1, 1, 0, now(), now()) RETURNING id`, ['E2E单送货员' + STAMP])
  DELIVERYMAN = r2[0].id
  const v1 = await dbQuery(
    `INSERT INTO dms_vehicle (tenant_id, vehicle_code, plate_no, vehicle_type, status, deleted, create_time, update_time)
     VALUES (1, $1, $2, 2, 1, 0, now(), now()) RETURNING id`, ['E2EDOV' + STAMP, '京F' + STAMP])
  VEHICLE = v1[0].id
  const rt = await dbQuery(
    `INSERT INTO erp_route (tenant_id, route_code, route_name, route_self, route_logistics, status, deleted, create_time, update_time)
     VALUES (1, $1, $2, 1, 0, 'ENABLED', 0, now(), now()) RETURNING id`, ['E2EDOR' + STAMP, 'E2E单线路' + STAMP])
  ROUTE = rt[0].id
  console.log(`  司机=${RIDER} 送货员=${DELIVERYMAN} 车辆=${VEHICLE} 线路=${ROUTE}`)
}

async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')
  const today = new Date().toISOString().slice(0, 10)
  const CUSTOMER = 'E2E配送单客户' + STAMP

  // 1) 号段
  // 号段接口返回裸字符串单号（非 JSON wrapper），需读原始 body
  const readNo = (r) => (r.json ? (r.json.data || '') : String(r.buf.toString('utf8') || '')).replace(/^"|"$/g, '')
  const no1Str = readNo(await api('GET', '/dms/task/next-no'))
  check('next-no 返回统一号段 PSD-YYYYMMDD-三位序号',
    /^PSD-\d{8}-\d{3}$/.test(no1Str), no1Str)
  const no2Str = readNo(await api('GET', '/dms/task/next-no'))
  check('未落库时取号幂等（号段以已落库单据为基准）', no2Str === no1Str, `${no1Str} → ${no2Str}`)

  // 2) 保存草稿（头 + 明细，后端重算合计）
  const items = [
    { productCode: 'P-A', productName: 'E2E商品甲', barcode: '690000000001', spec: '10kg', unit: '箱', quantity: 2, unitPrice: 50, weight: 1.5, volume: 0.2, remark: '行1' },
    { productCode: 'P-B', productName: 'E2E商品乙', barcode: '690000000002', spec: '5kg', unit: '袋', quantity: 3, unitPrice: 20, weight: 0.8, volume: 0.1, remark: '行2' },
  ]
  const saveRes = await api('POST', '/dms/task/save', {
    taskNo: no1Str,
    deliveryDate: today,
    orderType: 1,
    priority: 2,
    orderNo: MARK + '-SO',
    sourceBillNo: MARK + '-XSCKD-001',
    customerName: CUSTOMER,
    customerPhone: '13700000001',
    customerAddress: 'E2E收货地址' + STAMP,
    sourceAddress: 'E2E发货仓' + STAMP,
    deadlineTime: today + 'T00:00:00',
    routeId: ROUTE,
    routeArea: 'E2E配送区域' + STAMP,
    riderId: RIDER,
    riderName: 'E2E单司机' + STAMP,
    vehicleId: VEHICLE,
    vehicleName: '京F' + STAMP,
    deliverymanId: DELIVERYMAN,
    deliverymanName: 'E2E单送货员' + STAMP,
    deliveryFee: 30,
    collectOnDelivery: 500,
    depositAmount: 120,
    boxQuantity: 6,
    estimatedDistance: 12.5,
    remark: 'E2E单备注' + STAMP,
    items,
  })
  const created = data(saveRes)
  const TASK_ID = created?.id
  check('保存草稿返回主键与单号', !!TASK_ID && !!created?.taskNo, JSON.stringify({ id: TASK_ID, taskNo: created?.taskNo }))
  check('新建任务初始状态=待分配(0)', Number(created?.status) === 0, created?.status)
  check('明细 2 行落库', (created?.items || []).length === 2, `items=${(created?.items || []).length}`)
  check('发货数量=明细合计(2+3=5)', Number(created?.totalQuantity) === 5, created?.totalQuantity)
  check('发货金额=明细合计(2×50+3×20=160)', Number(created?.goodsAmount) === 160, created?.goodsAmount)
  check('金额由后端按 数量×单价 重算（金额列非前端传值）',
    (created?.items || []).every(i => Number(i.amount) === Number(i.quantity) * Number(i.unitPrice)),
    JSON.stringify((created?.items || []).map(i => i.amount)))
  check('重量/体积汇总（1.5+0.8=2.3 / 0.2+0.1=0.3）',
    Number(created?.totalWeight) === 2.3 && Number(created?.totalVolume) === 0.3,
    `${created?.totalWeight}/${created?.totalVolume}`)
  check('配送单量=来源单据去重计数(1)', Number(created?.orderCount) === 1, created?.orderCount)
  check('装箱数量落库(6)', Number(created?.boxQuantity) === 6, created?.boxQuantity)
  check('订金金额落库(120)', Number(created?.depositAmount) === 120, created?.depositAmount)
  check('制单人为登录用户快照（非前端传值）', !!created?.creatorName, created?.creatorName)

  // 2.1) 号段以已落库单据为基准递进
  const no3Str = readNo(await api('GET', '/dms/task/next-no'))
  check('落库后取号递进（新号 > 已用号）', no3Str > created.taskNo, `${created.taskNo} → ${no3Str}`)

  // 3) 明细行号连续
  const rows = await dbQuery(`SELECT line_no, product_name FROM dms_task_item WHERE task_id = $1 ORDER BY line_no`, [TASK_ID])
  check('明细行号从 1 连续（1,2）', rows.length === 2 && rows[0].line_no === 1 && rows[1].line_no === 2,
    JSON.stringify(rows.map(r => r.line_no)))

  // 4) 缺必填校验
  const bad = await api('POST', '/dms/task/save', { deliveryDate: today, orderType: 1, customerName: '', orderNo: '' })
  check('缺关联订单号/客户时校验失败（非 200）', Number(bad.json?.code) !== 200, JSON.stringify(bad.json).slice(0, 120))

  // 5) 同号再存 → 后端重新分配
  const dup = await api('POST', '/dms/task/save', {
    taskNo: no1Str, deliveryDate: today, orderType: 1, orderNo: MARK + '-SO2', customerName: CUSTOMER, items: [],
  })
  const dupData = data(dup)
  check('重复单号由后端重新分配（非前端自增）', !!dupData?.taskNo && dupData.taskNo !== no1Str,
    `${no1Str} → ${dupData?.taskNo}`)
  const DUP_ID = dupData?.id
  check('空明细保存成功且合计归零',
    Number(dupData?.totalQuantity) === 0 && Number(dupData?.goodsAmount) === 0,
    `${dupData?.totalQuantity}/${dupData?.goodsAmount}`)

  // 6) 退货类型口径：明细计入退货三列
  const retRes = await api('POST', '/dms/task/save', {
    deliveryDate: today, orderType: 3, orderNo: MARK + '-TH', customerName: CUSTOMER,
    items: [{ productName: 'E2E退货商品', quantity: 4, unitPrice: 25, weight: 1, volume: 0.1 }],
  })
  const retData = data(retRes)
  check('退货类型（订单类型=退货）明细计入退货数量/金额（4 / 100）',
    Number(retData?.returnQuantity) === 4 && Number(retData?.returnAmount) === 100,
    `${retData?.returnQuantity}/${retData?.returnAmount}`)
  check('退货类型同时记录退货单量(1)', Number(retData?.returnOrderCount) === 1, retData?.returnOrderCount)
  const RET_ID = retData?.id

  // 6.2) 待配送任务（供 UI 三值状态 / 红冲过滤使用；不带资源，避免干扰资源筛选计数）
  const pendRes = await api('POST', '/dms/task/save', {
    deliveryDate: today, orderType: 1, orderNo: MARK + '-SO3', customerName: CUSTOMER,
    items: [{ productName: 'E2E商品丁', quantity: 1, unitPrice: 10 }],
  })
  const PEND_ID = data(pendRes)?.id
  check('待配送任务造数成功（供 UI 三值口径）', !!PEND_ID, `id=${PEND_ID}`)

  // 7) 详情
  const detail = data(await api('GET', `/dms/task/${TASK_ID}`))
  check('详情返回头 + 明细（含条码/规格/单位）',
    detail?.items?.length === 2 && detail.items[0].barcode === '690000000001' && detail.items[0].spec === '10kg',
    JSON.stringify(detail?.items?.[0] || {}).slice(0, 160))
  check('详情回填配送资源（司机/车辆/送货员/线路/区域）',
    String(detail?.riderId) === String(RIDER) && String(detail?.vehicleId) === String(VEHICLE)
    && String(detail?.deliverymanId) === String(DELIVERYMAN) && String(detail?.routeId) === String(ROUTE)
    && !!detail?.routeArea, JSON.stringify({ r: detail?.riderName, v: detail?.vehicleName, m: detail?.deliverymanName }))

  // 8) 多条件分页
  const p1 = data(await api('GET', `/dms/task/page?current=1&size=50&customerName=${CUSTOMER}`))
  check('分页接口返回 records/total', Array.isArray(p1?.records) && p1?.total !== undefined, `total=${p1?.total}`)
  check('按客户模糊检索命中 4 条（销售配送×3 + 退货×1）', (p1?.records || []).length === 4, `rows=${(p1?.records || []).length}`)
  const byTaskNo = data(await api('GET', `/dms/task/page?current=1&size=50&taskNo=${created.taskNo}`))?.records || []
  check('按任务编号检索命中 1 条', byTaskNo.length === 1, `rows=${byTaskNo.length}`)
  const byBill = data(await api('GET', `/dms/task/page?current=1&size=50&sourceBillNo=${MARK}-XSCKD-001`))?.records || []
  check('按来源单据编号检索命中 1 条（任务与单据一对多口径）', byBill.length === 1, `rows=${byBill.length}`)
  const byOrder = data(await api('GET', `/dms/task/page?current=1&size=50&orderNo=${MARK}-SO2`))?.records || []
  check('按订单号检索命中 1 条', byOrder.length === 1, `rows=${byOrder.length}`)
  const byRider = data(await api('GET', `/dms/task/page?current=1&size=50&riderId=${RIDER}&customerName=${CUSTOMER}`))?.records || []
  check('按配送司机筛选命中 1 条', byRider.length === 1, `rows=${byRider.length}`)
  const byVehicle = data(await api('GET', `/dms/task/page?current=1&size=50&vehicleId=${VEHICLE}`))?.records || []
  check('按配送车辆筛选命中 1 条', byVehicle.length === 1, `rows=${byVehicle.length}`)
  const byMan = data(await api('GET', `/dms/task/page?current=1&size=50&deliverymanId=${DELIVERYMAN}`))?.records || []
  check('按送货员筛选命中 1 条', byMan.length === 1, `rows=${byMan.length}`)
  const byRoute = data(await api('GET', `/dms/task/page?current=1&size=50&routeId=${ROUTE}`))?.records || []
  check('按配送线路筛选命中 1 条', byRoute.length === 1, `rows=${byRoute.length}`)
  const byArea = data(await api('GET', `/dms/task/page?current=1&size=50&routeArea=${'E2E配送区域' + STAMP}`))?.records || []
  check('按配送区域筛选命中 1 条', byArea.length === 1, `rows=${byArea.length}`)
  const byRemark = data(await api('GET', `/dms/task/page?current=1&size=50&remark=${'E2E单备注' + STAMP}`))?.records || []
  check('按备注筛选命中 1 条', byRemark.length === 1, `rows=${byRemark.length}`)
  const byReceiver = data(await api('GET', `/dms/task/page?current=1&size=50&receiverKeyword=13700000001`))?.records || []
  check('按收货人/联系电话筛选命中 1 条', byReceiver.length === 1, `rows=${byReceiver.length}`)
  const byCreator = data(await api('GET', `/dms/task/page?current=1&size=50&creatorName=${created.creatorName}&customerName=${CUSTOMER}`))?.records || []
  check('按制单人筛选命中 4 条', byCreator.length === 4, `rows=${byCreator.length}`)
  const byDate = data(await api('GET', `/dms/task/page?current=1&size=50&deliveryDateStart=${today}&deliveryDateEnd=${today}&customerName=${CUSTOMER}`))?.records || []
  check('按指定配送日期范围筛选命中 4 条', byDate.length === 4, `rows=${byDate.length}`)
  const byDateMiss = data(await api('GET', `/dms/task/page?current=1&size=50&deliveryDateStart=2020-01-01&deliveryDateEnd=2020-01-02`))?.records || []
  check('配送日期范围外返回空', byDateMiss.length === 0, `rows=${byDateMiss.length}`)
  const byCreate = data(await api('GET', `/dms/task/page?current=1&size=50&createTimeStart=${today}&createTimeEnd=${today}&customerName=${CUSTOMER}`))?.records || []
  check('按制单时间范围筛选命中 4 条', byCreate.length === 4, `rows=${byCreate.length}`)
  const byStatus = data(await api('GET', `/dms/task/page?current=1&size=50&statusList=0,1,2&customerName=${CUSTOMER}`))?.records || []
  check('按配送状态多选（待配送 0,1,2）筛选命中 4 条', byStatus.length === 4, `rows=${byStatus.length}`)

  // 9) 按明细视图
  const pd = data(await api('GET', `/dms/task/page-detail?current=1&size=50&taskNo=${created.taskNo}`))
  check('按明细视图返回 2 个商品行（含任务号/客户）',
    (pd?.records || []).length === 2 && pd.records[0].taskNo === created.taskNo && !!pd.records[0].customerName,
    `rows=${(pd?.records || []).length}`)
  const pdDup = data(await api('GET', `/dms/task/page-detail?current=1&size=50&taskNo=${dupData?.taskNo}`))
  check('无明细任务在明细视图保留一行（台账不丢单）', (pdDup?.records || []).length === 1, `rows=${(pdDup?.records || []).length}`)

  // 10) 更新（头 + 明细替换）
  const updRes = await api('POST', '/dms/task/save', {
    id: TASK_ID,
    taskNo: created.taskNo,
    deliveryDate: today,
    orderType: 1,
    orderNo: MARK + '-SO',
    customerName: CUSTOMER,
    deliveryFee: 30,
    items: [{ productName: 'E2E商品丙', quantity: 10, unitPrice: 8 }],
  })
  const upd = data(updRes)
  check('修改后明细被替换（1 行）且合计重算（80）',
    (upd?.items || []).length === 1 && Number(upd?.goodsAmount) === 80,
    `items=${(upd?.items || []).length} amt=${upd?.goodsAmount}`)
  const staleItems = await dbQuery(`SELECT count(*)::int AS n FROM dms_task_item WHERE task_id = $1 AND deleted = 0`, [TASK_ID])
  check('旧明细行已清理（无残留）', staleItems[0].n === 1, `n=${staleItems[0].n}`)
  check('修改不覆盖制单人快照', upd?.creatorName === created.creatorName, `${created.creatorName} → ${upd?.creatorName}`)

  // 11) 审核 / 反审核（状态机）
  await api('POST', `/dms/task/${DUP_ID}/audit`)
  const afterAudit = data(await api('GET', `/dms/task/${DUP_ID}`))
  check('审核后状态=已分配(1)', Number(afterAudit?.status) === 1, afterAudit?.status)
  const auditAgain = await api('POST', `/dms/task/${DUP_ID}/audit`)
  check('重复审核被拒绝（非 200）', Number(auditAgain.json?.code) !== 200, JSON.stringify(auditAgain.json).slice(0, 120))
  await api('POST', `/dms/task/${DUP_ID}/unaudit`)
  const afterUnaudit = data(await api('GET', `/dms/task/${DUP_ID}`))
  check('反审核后状态回到待分配(0)', Number(afterUnaudit?.status) === 0, afterUnaudit?.status)
  // 已指派司机的不可反审核
  await api('POST', `/dms/task/${TASK_ID}/audit`)
  const unauditWithRider = await api('POST', `/dms/task/${TASK_ID}/unaudit`)
  check('已指派司机的单据不可反审核（业务规则）', Number(unauditWithRider.json?.code) !== 200,
    JSON.stringify(unauditWithRider.json).slice(0, 120))

  // 12) 打印次数
  const before = Number((await dbQuery(`SELECT print_count FROM dms_task WHERE id = $1`, [DUP_ID]))[0]?.print_count || 0)
  await api('POST', `/dms/task/${DUP_ID}/print`)
  const after = Number((await dbQuery(`SELECT print_count FROM dms_task WHERE id = $1`, [DUP_ID]))[0]?.print_count || 0)
  check('打印次数真实累加（+1）', after === before + 1, `${before} → ${after}`)

  // 13) 删除规则
  const delOk = await api('DELETE', `/dms/task/${DUP_ID}`)
  check('待分配单据可删除（逻辑删除）', Number(delOk.json?.code) === 200, JSON.stringify(delOk.json).slice(0, 100))
  const delRows = await dbQuery(`SELECT deleted FROM dms_task WHERE id = $1`, [DUP_ID])
  check('删除为逻辑删除（deleted=1，行仍在）', delRows.length === 1 && Number(delRows[0].deleted) === 1,
    JSON.stringify(delRows[0]))
  const delList = data(await api('GET', `/dms/task/page?current=1&size=50&taskNo=${dupData?.taskNo}`))?.records || []
  check('已删除单据不再出现在列表', delList.length === 0, `rows=${delList.length}`)
  // 进入配送执行的不可删
  await api('PUT', `/dms/task/${TASK_ID}/status`, { fromStatus: 1, toStatus: 2 })
  await api('PUT', `/dms/task/${TASK_ID}/status`, { fromStatus: 2, toStatus: 3 })
  const delExec = await api('DELETE', `/dms/task/${TASK_ID}`)
  check('已进入配送执行的单据不可删除', Number(delExec.json?.code) !== 200, JSON.stringify(delExec.json).slice(0, 120))
  const delExecList = data(await api('GET', `/dms/task/page?current=1&size=50&taskNo=${created.taskNo}`))?.records || []
  check('已进入配送执行的单据仍可查询', delExecList.length === 1, `rows=${delExecList.length}`)

  // 14) 取消（红冲）+ 显示红冲
  const cancelRes = await api('POST', `/dms/task/${RET_ID}/cancel`)
  check('待分配单据可取消（0→7）', Number(cancelRes.json?.code) === 200, JSON.stringify(cancelRes.json).slice(0, 100))
  const hideRed = data(await api('GET', `/dms/task/page?current=1&size=50&customerName=${CUSTOMER}`))?.records || []
  const showRed = data(await api('GET', `/dms/task/page?current=1&size=50&customerName=${CUSTOMER}&showRed=true`))?.records || []
  check('默认隐藏已取消（红冲）单据', hideRed.length === showRed.length - 1 && showRed.length >= 2,
    `hide=${hideRed.length} show=${showRed.length}`)

  // 15) 选择器候选（复用 DMS 统一口径）
  const opts = data(await api('GET', '/dms/task/filter-options'))
  check('查询下拉返回 司机/车辆/送货员/制单人 四类',
    Array.isArray(opts?.drivers) && Array.isArray(opts?.vehicles)
    && Array.isArray(opts?.deliverymen) && Array.isArray(opts?.creators),
    JSON.stringify(Object.keys(opts || {})))
  check('下拉含本次造出的司机/车辆/送货员',
    (opts?.drivers || []).some(o => String(o.id) === String(RIDER))
    && (opts?.vehicles || []).some(o => String(o.id) === String(VEHICLE))
    && (opts?.deliverymen || []).some(o => String(o.id) === String(DELIVERYMAN)),
    `drivers=${(opts?.drivers || []).length} vehicles=${(opts?.vehicles || []).length}`)

  // 16) 导出真实 xlsx
  const exp = await rawReq('GET', `/dms/task/export?customerName=${CUSTOMER}&showRed=true`, null, TOKEN)
  const isXlsx = exp.buf && exp.buf.length > 4 && exp.buf[0] === 0x50 && exp.buf[1] === 0x4B
  check('导出接口返回真实 xlsx（PK 魔数 + xlsx MIME）',
    isXlsx && String(exp.headers['content-type']).includes('spreadsheetml'),
    `status=${exp.status} type=${exp.headers['content-type']} size=${exp.buf?.length}`)

  return { TASK_ID, DUP_ID, RET_ID, CUSTOMER, taskNo: created.taskNo, taskNoPending: afterUnaudit?.taskNo }
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite(ctx) {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const bctx = await browser.newContext({
    viewport: { width: 1720, height: 980 }, locale: 'zh-CN', acceptDownloads: true,
  })
  const page = await bctx.newPage()

  // antd 会给「无图标 + 恰好两个汉字」按钮插空格，统一用正则匹配
  page.clickBtn = (re, scope) => (scope || page).locator('button').filter({ hasText: re }).first().click()

  /**
   * 字段是否真实渲染：antd 的 a-input / a-date-picker 把标签放在 placeholder 属性上
   * （不进入 innerText），下拉/勾选框则以文本节点呈现，故两类都查。
   */
  async function fieldPresent(label) {
    return await page.evaluate((lb) => {
      const key = lb.replace(/\s+/g, '')
      if (document.querySelector(`input[placeholder="${lb}"], textarea[placeholder="${lb}"]`)) return true
      const nodes = [...document.querySelectorAll(
        '.ant-select-selection-placeholder, .ant-picker-input input, label, th, .ant-checkbox-wrapper, .ant-form-item-label')]
      return nodes.some((e) => {
        const t = ((e.textContent || '') + (e.getAttribute('placeholder') || '')).replace(/\s+/g, '')
        return t.includes(key)
      })
    }, label)
  }

  /** 明细表格列序号（按表头文案定位，避免硬编码列顺序） */
  async function colIndex(label) {
    return await page.evaluate((lb) => {
      const ths = [...document.querySelectorAll('.ss-grid thead th')]
      return ths.findIndex(t => (t.innerText || '').replace(/\s+/g, '').includes(lb.replace(/\s+/g, '')))
    }, label)
  }

  /** 明细单元格录入（BillDetailTable 为「点击进入编辑」，编辑器是原生 input.ss-native-input） */
  async function fillCell(rowIdx, idx, value) {
    const td = page.locator('.ss-grid tbody tr').nth(rowIdx).locator('td').nth(idx)
    await td.click()
    await page.waitForTimeout(300)
    const inp = td.locator('input').first()
    if ((await inp.count()) === 0) return false
    await inp.fill(String(value))
    await page.waitForTimeout(150)
    await page.keyboard.press('Tab')
    await page.waitForTimeout(300)
    return true
  }

  // /api 转发到验收实例（前端 dev server 的代理指向别处）
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

  // ── 列表页 ──
  await openPage(`${FE}/dispatch/dispatch-order/index`)
  let bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('列表页可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())
  check('Tab 为「配送单-历史」', bodyText.includes('配送单-历史'), bodyText.slice(0, 60))

  for (const b of ['新增', '刷新', '批量打印', '打印(F8)', '导出', '删除']) {
    check(`工具栏含「${b}」`, bodyText.includes(b.replace(/\s+/g, '')))
  }
  check('工具栏含「页面配置」入口', (await page.locator('button:has(.anticon-setting)').count()) > 0)
  check('工具栏不含「列配置」按钮（列配置走表头齿轮）', !bodyText.includes('列配置'))

  for (const q of ['昨日', '今日', '近两日', '近一周', '近一月', '本周', '上周', '本月', '上月']) {
    check(`快捷时间段含「${q}」`, bodyText.includes(q))
  }

  for (const q of ['任务编号', '来源单据编号', '订单号', '客户', '收货人/联系电话', '配送司机',
    '配送车辆', '送货员', '配送线路', '配送区域', '制单人', '备注', '显示红冲']) {
    check(`查询区含条件「${q}」`, await fieldPresent(q))
  }
  check('配送状态为多选控件', (await page.locator('.ant-select-multiple').count()) > 0)

  const headers = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').replace(/\s+/g, '')).filter(Boolean))
  const defaultCols = ['指定配送日期', '任务编号', '配送状态', '配送开始时间', '配送结束时间', '司机名称',
    '配送车辆', '送货员', '配送单量', '订金金额', '退货单量', '发货数量', '发货金额', '退货数量',
    '退货金额', '装箱数量', '配送里程(km)', '备注', '制单人', '制单时间', '操作']
  for (const h of defaultCols) {
    check(`表头含默认列「${h}」`, headers.some(x => x.includes(h.replace(/\s+/g, ''))), headers.join('/'))
  }
  check('默认隐藏 4 列（司机编号/体积/重量/打印次数）',
    !headers.includes('司机编号') && !headers.some(h => h.includes('体积'))
    && !headers.some(h => h.includes('重量')) && !headers.includes('打印次数'), headers.join('/'))

  // 表头齿轮列配置
  const gear = page.locator('.ss-grid .th-settings-btn').first()
  check('数据表存在表头齿轮（列配置唯一入口）', (await gear.count()) > 0)
  if (await gear.count()) {
    await gear.click()
    await page.waitForTimeout(1500)
    const panel = page.locator('.ant-modal-content:visible').last()
    const panelText = (await panel.innerText()).replace(/\s+/g, '')
    check('列配置弹窗含「个人配置 / 全局配置」双 Tab',
      panelText.includes('个人配置') && panelText.includes('全局配置'), panelText.slice(0, 120))
    for (const c of ['指定配送日期', '任务编号', '配送状态', '司机编号', '司机名称', '配送车辆', '送货员',
      '配送单量', '订金金额', '退货单量', '发货数量', '发货金额', '退货数量', '退货金额', '装箱数量',
      '配送里程', '体积', '重量', '备注', '打印次数', '制单人', '制单时间']) {
      check(`列配置含列「${c}」`, panelText.includes(c.replace(/\s+/g, '')))
    }
    await page.screenshot({ path: path.join(SHOTS, 'ui-list-colconfig.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  }

  // 列表数据 + 条件过滤真实生效
  const rowCount = () => page.evaluate(() => document.querySelectorAll('.ss-grid tbody tr').length)
  const customerInput = page.locator('input[placeholder="客户"]').first()
  await customerInput.fill(ctx.CUSTOMER)
  await page.clickBtn(/查\s*询/)
  await page.waitForTimeout(2500)
  const rowsNow = await rowCount()
  check('按客户查询渲染真实数据（2 行；已取消 1 张默认隐藏）', rowsNow === 2, `rows=${rowsNow}`)

  const gridText = (await page.locator('.ss-grid').innerText()).replace(/\s+/g, '')
  check('配送状态列显示三值口径（待配送/配送中）',
    gridText.includes('待配送') && (gridText.includes('配送中') || gridText.includes('已配送')), gridText.slice(0, 150))
  check('任务编号列为可点击链接（PSD- 号段）', /PSD-\d{8}-\d{3}/.test(gridText), gridText.slice(0, 120))
  check('底部合计行存在', bodyText.includes('合计') || gridText.includes('合计'))
  const pagerText = await page.evaluate(() => {
    const el = document.querySelector('.table-pagination')
    return el ? el.innerText.replace(/\s+/g, '') : ''
  })
  check('底部为经典分页栏（共 N 条）', pagerText.includes('共'), pagerText.slice(0, 80))
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 勾选 + 合计
  const firstCheckbox = page.locator('.ss-grid tbody tr .ss-checkbox').first()
  check('数据表支持勾选（批量打印/删除前置）', (await firstCheckbox.count()) > 0)
  if (await firstCheckbox.count()) {
    await firstCheckbox.click()
    await page.waitForTimeout(500)
    check('勾选后出现批量操作提示', (await page.locator('.batch-bar').count()) > 0)
  }

  // 显示红冲
  const redBox = page.locator('.ant-checkbox-wrapper:has-text("显示红冲")').first()
  if (await redBox.count()) {
    await redBox.click()
    await page.waitForTimeout(400)
    await page.clickBtn(/查\s*询/)
    await page.waitForTimeout(2500)
    const rowsRed = await rowCount()
    check('勾选「显示红冲」后已取消单据可见（3 行）', rowsRed === 3, `rows=${rowsRed}`)
    await page.screenshot({ path: path.join(SHOTS, 'ui-list-show-red.png'), fullPage: true })
    await redBox.click()
    await page.waitForTimeout(400)
  } else {
    check('勾选「显示红冲」后已取消单据可见（3 行）', false, '未找到显示红冲勾选框')
  }

  // 页面配置弹窗
  const cfgBtn = page.locator('button:has(.anticon-setting)').first()
  if (await cfgBtn.count()) {
    await cfgBtn.click()
    await page.waitForTimeout(1500)
    const cfgPanel = page.locator('.ant-modal-content:visible').last()
    const cfgText = (await cfgPanel.innerText()).replace(/\s+/g, '')
    check('页面配置弹窗含查询条件 Tab 与 16 项条件',
      cfgText.includes('查询条件') && cfgText.includes('任务编号') && cfgText.includes('收货人/联系电话'),
      cfgText.slice(0, 140))
    check('页面配置弹窗含功能按钮 Tab（新增/刷新/批量打印/打印(F8)/导出/删除）',
      cfgText.includes('功能按钮') && cfgText.includes('打印配置'), cfgText.slice(0, 160))
    // 切到「功能按钮」Tab 校验功能开关清单真实渲染
    const fnTab = cfgPanel.locator('.ant-tabs-tab').filter({ hasText: '功能按钮' }).first()
    if (await fnTab.count()) {
      await fnTab.click()
      await page.waitForTimeout(700)
      const fnText = (await cfgPanel.innerText()).replace(/\s+/g, '')
      check('功能按钮 Tab 含 新增/刷新/批量打印/打印(F8)/导出/删除 开关',
        ['新增', '刷新', '批量打印', '打印(F8)', '导出', '删除'].every(b => fnText.includes(b.replace(/\s+/g, ''))),
        fnText.slice(0, 160))
      await page.screenshot({ path: path.join(SHOTS, 'ui-list-pageconfig-buttons.png'), fullPage: true })
    } else {
      check('功能按钮 Tab 含 新增/刷新/批量打印/打印(F8)/导出/删除 开关', false, '未找到功能按钮 Tab')
    }
    await page.screenshot({ path: path.join(SHOTS, 'ui-list-pageconfig.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  }

  // 导出（真实下载 xlsx）
  try {
    const [download] = await Promise.all([
      page.waitForEvent('download', { timeout: 20000 }),
      page.clickBtn(/导\s*出/),
    ])
    const name = download.suggestedFilename()
    check('导出触发真实 xlsx 下载', /\.xlsx$/.test(name), name)
    await download.saveAs(path.join(SHOTS, 'export.xlsx'))
  } catch (e) {
    check('导出触发真实 xlsx 下载', false, String(e.message).slice(0, 120))
  }

  // ── 表单页（新增） ──
  await openPage(`${FE}/dispatch/dispatch-order/form`)
  bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('表单页可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())
  check('表单页标题为「配送单」', bodyText.includes('配送单'), bodyText.slice(0, 60))

  const headerNo = await page.locator('input[placeholder="编号"]').first().inputValue().catch(() => '')
  check('表单编号取后端号段 PSD-YYYYMMDD-序号', /^PSD-\d{8}-\d{3}$/.test(headerNo), headerNo)

  for (const f of ['指定配送日期', '订单类型', '关联订单号', '客户名称', '联系电话', '收货地址',
    '配送线路', '配送司机', '配送车辆', '送货员', '配送费', '代收货款']) {
    // 「订单类型」默认已选中「销售配送」，此时 select 不渲染 placeholder，以选中值文案为准
    const ok = (await fieldPresent(f)) || (f === '订单类型' && bodyText.includes('销售配送'))
    check(`表单基本信息含「${f}」`, ok)
  }
  const detailHeaders = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').replace(/\s+/g, '')).filter(Boolean))
  for (const c of ['商品名称', '货号', '条码', '单位', '数量', '单价', '金额', '重量（kg）', '体积（m³）', '明细备注']) {
    check(`明细列含「${c}」`, detailHeaders.some(x => x.includes(c)), detailHeaders.join('/'))
  }

  // 配置弹窗 3 Tab
  const cfgAction = page.locator('button').filter({ hasText: /配\s*置/ }).first()
  if (await cfgAction.count()) {
    await cfgAction.click()
    await page.waitForTimeout(1500)
    const formCfg = page.locator('.ant-modal-content:visible').last()
    const formCfgText = (await formCfg.innerText()).replace(/\s+/g, '')
    check('表单配置弹窗含 3 Tab（页面配置/录单默认值/打印设置）',
      formCfgText.includes('页面配置') && formCfgText.includes('录单默认值') && formCfgText.includes('打印设置'),
      formCfgText.slice(0, 140))
    check('页面配置 Tab 含「回车键跳转」列（对标金标准）', formCfgText.includes('回车键跳转'))
    await page.screenshot({ path: path.join(SHOTS, 'ui-form-config.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  } else {
    check('表单配置弹窗含 3 Tab（页面配置/录单默认值/打印设置）', false, '未找到配置按钮')
  }

  // 填单 + 保存草稿
  const NEW_MARK = MARK + 'UI'
  const NEW_CUSTOMER = 'E2E配送单客户UI' + STAMP
  await page.locator('input[placeholder="关联订单号"]').first().fill(NEW_MARK + '-SO')
  await page.locator('input[placeholder="客户名称"]').first().fill(NEW_CUSTOMER)
  await page.locator('input[placeholder="收货人/联系电话"]').first().fill('13600000009').catch(() => {})
  const phoneInput = page.locator('input[placeholder="联系电话"]').first()
  if (await phoneInput.count()) await phoneInput.fill('13600000009')
  // 明细录入：BillDetailTable 为「点击单元格进入编辑」，编辑器是原生 input
  const nameIdx = await colIndex('商品名称')
  const qtyIdx = await colIndex('数量')
  const priceIdx = await colIndex('单价')
  const nameTd = page.locator('.ss-grid tbody tr').first().locator('td').nth(nameIdx)
  await nameTd.click()
  await page.waitForTimeout(400)
  check('明细行可点击进入编辑（商品名称列出现编辑器）',
    (await nameTd.locator('input').count()) > 0, `colIdx=${nameIdx}`)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(200)
  const okQty = await fillCell(0, qtyIdx, 5)
  const okPrice = await fillCell(0, priceIdx, 12)
  check('明细行数量/单价可界面录入', okQty && okPrice, `qtyIdx=${qtyIdx} priceIdx=${priceIdx}`)

  await page.locator('button').filter({ hasText: /保存草稿/ }).first().click()
  await page.waitForTimeout(4000)
  const afterSaveUrl = page.url()
  check('保存草稿后跳转列表页', afterSaveUrl.includes('/dispatch/dispatch-order') && !afterSaveUrl.includes('/form'),
    afterSaveUrl)
  const saved = await dbQuery(
    `SELECT id, task_no, total_quantity, goods_amount, status, delivery_date FROM dms_task
     WHERE customer_name = $1 ORDER BY id DESC LIMIT 1`, [NEW_CUSTOMER])
  check('表单保存真实落库（按客户名反查）', saved.length === 1, JSON.stringify(saved[0] || {}))
  if (saved.length) {
    check('界面录入的数量/金额被后端重算（5×12=60）',
      Number(saved[0].total_quantity) === 5 && Number(saved[0].goods_amount) === 60,
      `${saved[0].total_quantity}/${saved[0].goods_amount}`)
    check('新建单据号为 PSD 号段', /^PSD-\d{8}-\d{3}$/.test(saved[0].task_no), saved[0].task_no)
    const uiItems = await dbQuery(`SELECT count(*)::int AS n FROM dms_task_item WHERE task_id = $1`, [saved[0].id])
    check('界面明细行落库（1 行）', uiItems[0].n === 1, `n=${uiItems[0].n}`)
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-form-saved-list.png'), fullPage: true })

  // ── 编辑回填 ──
  const editId = saved[0]?.id
  if (editId) {
    await openPage(`${FE}/dispatch/dispatch-order/form?id=${editId}`)
    const editNo = await page.locator('input[placeholder="编号"]').first().inputValue().catch(() => '')
    check('编辑页回填单据编号', editNo === saved[0].task_no, `${editNo} vs ${saved[0].task_no}`)
    const editCustomer = await page.locator('input[placeholder="客户名称"]').first().inputValue().catch(() => '')
    check('编辑页回填客户名称', editCustomer === NEW_CUSTOMER, editCustomer)
    const editOrder = await page.locator('input[placeholder="关联订单号"]').first().inputValue().catch(() => '')
    check('编辑页回填关联订单号', editOrder === NEW_MARK + '-SO', editOrder)
    const editRows = await page.evaluate(() => document.querySelectorAll('.ss-grid tbody tr').length)
    check('编辑页回填明细行（≥1）', editRows >= 1, `rows=${editRows}`)
    const editText = (await page.locator('body').innerText()).replace(/\s+/g, '')
    check('编辑页显示单据状态与制单人', editText.includes('单据状态') && editText.includes('制单人'))
    await page.screenshot({ path: path.join(SHOTS, 'ui-form-edit.png'), fullPage: true })
  }

  // ── 打印(F8)：列表勾选 → 跳表单 → 打印次数回写 ──
  await openPage(`${FE}/dispatch/dispatch-order/index`)
  const customerInput2 = page.locator('input[placeholder="客户"]').first()
  await customerInput2.fill(ctx.CUSTOMER)
  await page.clickBtn(/查\s*询/)
  await page.waitForTimeout(2500)
  const cb = page.locator('.ss-grid tbody tr .ss-checkbox').first()
  if (await cb.count()) {
    await cb.click()
    await page.waitForTimeout(500)
    // 从列表首行的任务编号取目标单据
    const targetNo = await page.evaluate(() => {
      const tra = [...document.querySelectorAll('.ss-grid tbody tr')]
      const link = tra.length ? tra[0].querySelector('button') : null
      return link ? (link.innerText || '').trim() : ''
    })
    const targetRow = targetNo
      ? await dbQuery(`SELECT id, print_count FROM dms_task WHERE task_no = $1`, [targetNo])
      : []
    await page.clickBtn(/打印\(F8\)|打\s*印\(F8\)/)
    await page.waitForTimeout(3500)
    check('打印(F8) 跳转表单页并带打印标记',
      page.url().includes('/dispatch/dispatch-order/form') && page.url().includes('print=1'), page.url())
    if (targetRow.length) {
      const afterPrint = await dbQuery(`SELECT print_count FROM dms_task WHERE id = $1`, [targetRow[0].id])
      check('打印(F8) 真实回写打印次数（+1）',
        Number(afterPrint[0].print_count) === Number(targetRow[0].print_count) + 1,
        `${targetNo}: ${targetRow[0].print_count} → ${afterPrint[0].print_count}`)
    } else {
      check('打印(F8) 真实回写打印次数（+1）', false, `未定位目标单据 taskNo=${targetNo}`)
    }
    await page.screenshot({ path: path.join(SHOTS, 'ui-print-f8.png'), fullPage: true })
  } else {
    check('打印(F8) 跳转表单页并带打印标记', false, '未找到行勾选框')
  }

  // ── 列表行「查看」进入表单（只读） ──
  await openPage(`${FE}/dispatch/dispatch-order/index`)
  const customerInput3 = page.locator('input[placeholder="客户"]').first()
  await customerInput3.fill(ctx.CUSTOMER)
  await page.clickBtn(/查\s*询/)
  await page.waitForTimeout(2500)
  const viewBtn = page.locator('.ss-grid tbody tr button').filter({ hasText: /查\s*看/ }).first()
  if (await viewBtn.count()) {
    await viewBtn.click()
    await page.waitForTimeout(3000)
    check('列表「查看」进入表单页并带 mode=view', page.url().includes('mode=view'), page.url())
    const viewText = (await page.locator('body').innerText()).replace(/\s+/g, '')
    check('查看模式显示单据状态', viewText.includes('单据状态'))
  } else {
    check('列表「查看」进入表单页并带 mode=view', false, '未找到查看按钮')
  }

  await browser.close()
}

// ══════════════ 汇总 ══════════════
async function main() {
  console.log('配送单金标准 E2E —— 后端 :' + PORT + '，前端 ' + FE)
  await ensureE2EUser()
  await login()
  await prepareResources()
  const ctx = await apiSuite()
  await uiSuite(ctx)
  const ok = results.filter(r => r.ok).length
  const fail = results.length - ok
  console.log(`\n═══ 验收汇总：${ok}/${results.length} 通过，${fail} 失败 ═══`)
  if (fail) {
    console.log('失败项：')
    results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  }
  console.log(`截图目录：${SHOTS}`)
  process.exit(fail ? 1 : 0)
}

main().catch(e => { console.error('E2E 异常终止:', e); process.exit(2) })
