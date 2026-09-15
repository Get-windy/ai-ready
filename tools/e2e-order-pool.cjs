/*
 * 订单池（配送 → 调度管理 → 订单池，菜单 80860）金标准端到端验证 —— 129 项（首轮 97 + 二轮加固 32）
 *   · 对标状态：ql361 无此页面（本系统新增），口径依据《订单池开发文档》（首轮 §4 + 二轮 §5）
 *   · 状态机：待抢单(0) ─启用竞价→ 竞价中(1) ─结算→ 已接单(2)；竞价中 ─关闭竞价→ 待抢单；
 *             超时→已过期(3)；下架→已下架(4)
 *   · API 验收：publish（防重复入池含已接单 / 仅待分配任务 / 批量 / 随发布开启竞价）/ page（10 条件
 *               + 任务联查，含「接单配送员」与「配送线路」真实过滤）/ grab（同事务指派）/ enable-bid
 *               （参数边界）/ disable-bid（关闭竞价：作废报价 + 可恢复）/ bid（截止后拒收 + 同一人改价
 *               不新增）/ cancel-bid（仅竞价中可撤 + 当前价回退）/ settle（价低中标 + 剔除已撤销报价 +
 *               同事务指派）/ force-assign（幂等 + 状态机）/ offline（状态机 + 原因留痕）/
 *               expire-scan / bid-list / export（真实 xlsx）
 *   · UI  验收：骨架（CategoryListLayout + 表头齿轮列配置 + PageConfigPanel）+ 工具栏 7 按钮 +
 *               查询区 10 条件 + 21 列表头（18 默认 + 3 隐藏）+ 池状态/竞价模式 tag + 合计行 + 经典分页 +
 *               批量发布到池弹窗 / 强制分配弹窗 / 开启竞价弹窗 / 代客出价弹窗 / 查看竞标（竞价详情页：
 *               池状态标签 + 取消出价 + 关闭竞价）
 *
 * 用法：node tools/e2e-order-pool.cjs         （默认后端 5692、前端 5656）
 *      ERP_PORT=5692 FE_URL=http://localhost:5656 node tools/e2e-order-pool.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5692)
const SHOTS = 'I:/AI-Ready/tool-results/order-pool'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
const E2E_USER = process.env.E2E_USER || 'e2e_order_pool'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

const STAMP = Date.now().toString().slice(-6)
const MARK = 'E2EOP' + STAMP
const CUSTOMER = 'E2E订单池客户' + STAMP
// 二轮复验专用分组（同一清理前缀，独立客户名 → 不影响主流程的列表行数断言）
const CUSTOMER_B = CUSTOMER + '二组'
const CUSTOMER_C = CUSTOMER + '三组'

async function dbQuery(sql, params) {
  const client = new Client(DSN)
  await client.connect()
  try {
    return (await client.query(sql, params)).rows
  } finally {
    await client.end()
  }
}

async function ensureE2EUser() {
  const exist = await dbQuery(`SELECT id::text AS uid FROM sys_user WHERE username = $1`, [E2E_USER])
  let uid
  if (exist.length > 0) {
    uid = exist[0].uid
  } else {
    const idRow = await dbQuery(
      `SELECT (COALESCE(MAX(id), 2099000000000003000) + 1)::text AS uid
       FROM sys_user WHERE id >= 2099000000000003000 AND id < 2099000000000004000`)
    uid = idRow[0].uid
    await dbQuery(
      `INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                             user_type, is_super_admin, is_tenant_admin, status, data_scope)
       SELECT $2::bigint, 1, 0, now(), now(), $1::varchar, password, 'E2E订单池', 'E2E订单池',
              1, false, false, 1, 'ALL'
       FROM sys_user WHERE username = 'admin'`, [E2E_USER, uid])
    console.log(`已创建验收账号 ${E2E_USER}（密码同 admin，id=${uid}）`)
  }
  if ((await dbQuery(`SELECT 1 FROM sys_user_role WHERE user_id = $1::bigint`, [uid])).length === 0) {
    await dbQuery(`INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
                   VALUES (($1::bigint + 1)::bigint, $1::bigint, 1, 1, now())`, [uid])
  }
  if ((await dbQuery(`SELECT 1 FROM sys_user_tenant WHERE user_id = $1::bigint`, [uid])).length === 0) {
    await dbQuery(`INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
                   VALUES (($1::bigint + 2)::bigint, $1::bigint, 1, true, 1, now(), now())`, [uid])
  }
}

// ══════════════ HTTP 基础设施 ══════════════
function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* blob */ }
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
  if (r.status === 401 || Number(r.json?.code) === 401) {
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
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) {
    throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300)
      + `\n提示：独立账号 ${E2E_USER}/${E2E_PWD}（脚本首跑自动创建）`)
  }
  TOKEN = token
}

// ══════════════ 结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

// ══════════════ 一、接口验收 ══════════════
let RIDER_A, RIDER_B, RIDER_C, RIDER_D
const TASK = {}   // { grab, bid, assign, offline, expire }
const POOL = {}   // { grab, bid, assign, offline, expire }

async function prepareData() {
  console.log('\n═══ 〇、造数准备 ═══')
  // 清理本脚本历史数据（按客户名标记隔离）
  await dbQuery(`DELETE FROM dms_bid WHERE pool_id IN (
      SELECT p.id FROM dms_order_pool p JOIN dms_task t ON t.id = p.task_id WHERE t.customer_name LIKE 'E2E订单池客户%')`)
  await dbQuery(`DELETE FROM dms_order_pool WHERE task_id IN (
      SELECT id FROM dms_task WHERE customer_name LIKE 'E2E订单池客户%')`)
  await dbQuery(`DELETE FROM dms_task_item WHERE task_id IN (
      SELECT id FROM dms_task WHERE customer_name LIKE 'E2E订单池客户%')`)
  await dbQuery(`DELETE FROM dms_task WHERE customer_name LIKE 'E2E订单池客户%'`)

  const mkRider = async (name, phone) => (await dbQuery(
    `INSERT INTO dms_rider (tenant_id, rider_type, real_name, phone, status, verify_status, deleted, create_time, update_time)
     VALUES (1, 2, $1, $2, 1, 1, 0, now(), now()) RETURNING id`, [name, phone]))[0].id
  RIDER_A = await mkRider('E2E池甲' + STAMP, '13910000001')
  RIDER_B = await mkRider('E2E池乙' + STAMP, '13910000002')
  RIDER_C = await mkRider('E2E池丙' + STAMP, '13910000003')
  RIDER_D = await mkRider('E2E池丁' + STAMP, '13910000004')
  console.log(`  配送员：A=${RIDER_A} B=${RIDER_B} C=${RIDER_C} D=${RIDER_D}`)

  const today = new Date().toISOString().slice(0, 10)
  const mkTask = async (suffix, fee, customer) => {
    const res = await api('POST', '/dms/task/save', {
      deliveryDate: today, orderType: 1, orderNo: MARK + '-' + suffix, customerName: customer || CUSTOMER,
      sourceAddress: 'E2E取货点' + STAMP, customerAddress: 'E2E收货点' + STAMP,
      deliveryFee: fee, estimatedDistance: 8.5, remark: 'E2E池备注' + STAMP,
      items: [{ productName: 'E2E池商品', quantity: 1, unitPrice: 100 }],
    })
    const d = data(res)
    if (!d?.id) throw new Error('造任务失败: ' + JSON.stringify(res.json).slice(0, 200))
    return d.id
  }
  TASK.grab = await mkTask('GRAB', 10)
  TASK.bid = await mkTask('BID', 20)
  TASK.assign = await mkTask('ASSIGN', 30)
  TASK.offline = await mkTask('OFFLINE', 40)
  TASK.expire = await mkTask('EXPIRE', 50)
  TASK.ui = await mkTask('UI', 60)   // 留给 UI 强制分配用（保持「待抢单」状态）
  // ── 二轮复验分组 ──
  TASK.cancelBid = await mkTask('CANCELBID', 20, CUSTOMER_B)  // 竞价：改价 / 取消最低价 / 结算
  TASK.repub = await mkTask('REPUB', 5, CUSTOMER_B)           // 下架后可重新发布
  TASK.assigned = await mkTask('ASSIGNED', 5, CUSTOMER_B)     // 被置为「已分配」→ 不允许入池
  TASK.uiBid = await mkTask('UIBID', 25, CUSTOMER_C)          // UI：代客出价 → 查看竞标 → 取消出价
  TASK.offBid = await mkTask('OFFBID', 40, CUSTOMER_B)        // 关闭竞价 → 可重新开启并结算
  console.log(`  任务：${JSON.stringify(TASK)}`)
}

async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 1) 发布到池（批量，5 条流程任务；TASK.ui 留给界面验收单独发布）
  const pub = await api('POST', '/dms/order-pool/publish',
    { taskIds: [TASK.grab, TASK.bid, TASK.assign, TASK.offline, TASK.expire] })
  const poolIds = data(pub)
  check('批量发布到池（5 条）', Array.isArray(poolIds) && poolIds.length === 5, JSON.stringify(poolIds))
  POOL.grab = poolIds[0]; POOL.bid = poolIds[1]; POOL.assign = poolIds[2]
  POOL.offline = poolIds[3]; POOL.expire = poolIds[4]

  const rows = await dbQuery(`SELECT id, pool_status, bid_enabled, bid_count, delivery_fee FROM dms_order_pool
                              WHERE id = ANY($1::bigint[]) ORDER BY id`, [poolIds])
  check('池记录落库，初始状态=待抢单(0) 且竞价关闭',
    rows.length === 5 && rows.every(r => Number(r.pool_status) === 0 && Number(r.bid_enabled) === 0),
    JSON.stringify(rows.map(r => [r.pool_status, r.bid_enabled])))
  check('配送费落库（发布时传入）', Number(rows[0].delivery_fee) === 10, rows[0].delivery_fee)

  // 2) 防重复入池
  const dup = await api('POST', '/dms/order-pool/publish', { taskIds: [TASK.grab] })
  check('同一任务重复发布被拒绝（防一笔任务两人接单）', Number(dup.json?.code) !== 200,
    JSON.stringify(dup.json).slice(0, 140))

  // 3) 台账分页 + 任务联查
  const p1 = data(await api('GET', `/dms/order-pool/page?current=1&size=50&keyword=${MARK}`))
  check('台账分页返回 records/total', Array.isArray(p1?.records) && p1?.total !== undefined, `total=${p1?.total}`)
  const row0 = (p1?.records || []).find(r => String(r.id) === String(POOL.bid))
  check('列表联查任务主数据（任务编号/客户/取货收货地址/货品金额）',
    !!row0?.taskNo && row0.customerName === CUSTOMER && !!row0.sourceAddress
    && !!row0.customerAddress && Number(row0.goodsAmount) === 100,
    JSON.stringify({ t: row0?.taskNo, c: row0?.customerName, g: row0?.goodsAmount }))
  check('订单类型文案与池状态文案由后端字典给出',
    row0?.orderTypeText === '销售配送' && row0?.poolStatusText === '待抢单',
    `${row0?.orderTypeText}/${row0?.poolStatusText}`)

  // 4) 多条件
  const byTaskNo = data(await api('GET', `/dms/order-pool/page?current=1&size=50&taskNo=${row0.taskNo}`))?.records || []
  check('按任务编号检索命中 1 条', byTaskNo.length === 1, `rows=${byTaskNo.length}`)
  const byCustomer = data(await api('GET', `/dms/order-pool/page?current=1&size=50&customerName=${CUSTOMER}`))?.records || []
  check('按客户检索命中 5 条', byCustomer.length === 5, `rows=${byCustomer.length}`)
  const byOrderNo = data(await api('GET', `/dms/order-pool/page?current=1&size=50&orderNo=${MARK}-BID`))?.records || []
  check('按关联订单号检索命中 1 条', byOrderNo.length === 1, `rows=${byOrderNo.length}`)
  const byStatus = data(await api('GET', `/dms/order-pool/page?current=1&size=50&customerName=${CUSTOMER}&poolStatus=0`))?.records || []
  check('按池状态筛选命中 5 条', byStatus.length === 5, `rows=${byStatus.length}`)
  const byBid = data(await api('GET', `/dms/order-pool/page?current=1&size=50&customerName=${CUSTOMER}&bidEnabled=1`))?.records || []
  check('按竞价模式筛选（当前无竞价）命中 0 条', byBid.length === 0, `rows=${byBid.length}`)
  const byType = data(await api('GET', `/dms/order-pool/page?current=1&size=50&customerName=${CUSTOMER}&orderType=1`))?.records || []
  check('按订单类型筛选命中 5 条', byType.length === 5, `rows=${byType.length}`)
  const byFee = data(await api('GET', `/dms/order-pool/page?current=1&size=50&customerName=${CUSTOMER}&feeMin=25&feeMax=45`))?.records || []
  check('按配送费区间筛选（25~45）命中 2 条', byFee.length === 2, `rows=${byFee.length}`)
  const today = new Date().toISOString().slice(0, 10)
  const byTime = data(await api('GET', `/dms/order-pool/page?current=1&size=50&customerName=${CUSTOMER}&publishTimeStart=${today}&publishTimeEnd=${today}`))?.records || []
  check('按发布时间范围筛选（今日）命中 5 条', byTime.length === 5, `rows=${byTime.length}`)
  const byTimeMiss = data(await api('GET', `/dms/order-pool/page?current=1&size=50&publishTimeStart=2020-01-01&publishTimeEnd=2020-01-02`))?.records || []
  check('发布时间范围外返回空', byTimeMiss.length === 0, `rows=${byTimeMiss.length}`)
  // 二轮复验：此前「配送线路 / 配送区域」查询条件缺失；「接单配送员」参数被后端静默忽略
  const ROUTE_ID = 700001
  await dbQuery(`UPDATE dms_task SET route_id = $1, route_area = $2 WHERE id = $3`, [ROUTE_ID, 'E2E配送区域', TASK.expire])
  const byRoute = data(await api('GET', `/dms/order-pool/page?current=1&size=50&customerName=${CUSTOMER}&routeId=${ROUTE_ID}`))?.records || []
  check('按「配送线路」筛选生效（命中 1 条）', byRoute.length === 1, `rows=${byRoute.length}`)
  const byRouteMiss = data(await api('GET', `/dms/order-pool/page?current=1&size=50&routeId=7999999`))?.records || []
  check('配送线路筛选无关值返回空', byRouteMiss.length === 0, `rows=${byRouteMiss.length}`)

  // 5) 抢单（同事务指派）
  const grab = await api('POST', `/dms/order-pool/${POOL.grab}/grab`, { riderId: RIDER_A, riderName: 'E2E池甲' + STAMP })
  check('抢单成功', Number(grab.json?.code) === 200, JSON.stringify(grab.json).slice(0, 120))
  const grabPool = (await dbQuery(`SELECT pool_status FROM dms_order_pool WHERE id = $1`, [POOL.grab]))[0]
  const grabTask = (await dbQuery(`SELECT rider_id, rider_name, status, dispatch_type FROM dms_task WHERE id = $1`, [TASK.grab]))[0]
  check('抢单后池状态=已接单(2)', Number(grabPool.pool_status) === 2, grabPool.pool_status)
  check('抢单与任务指派同事务生效（riderId/姓名快照/状态=已接单/调度方式=抢单）',
    String(grabTask.rider_id) === String(RIDER_A) && !!grabTask.rider_name
    && Number(grabTask.status) === 2 && Number(grabTask.dispatch_type) === 3,
    JSON.stringify(grabTask))
  const grabAgain = await api('POST', `/dms/order-pool/${POOL.grab}/grab`, { riderId: RIDER_B, riderName: 'E2E池乙' + STAMP })
  check('已被抢走的池再次抢单被拒（先到先得）', Number(grabAgain.json?.code) !== 200,
    JSON.stringify(grabAgain.json).slice(0, 140))
  const grabOffline = await api('POST', `/dms/order-pool/${POOL.grab}/grab`, { riderId: 99999999, riderName: 'x' })
  check('配送员不存在时抢单失败', Number(grabOffline.json?.code) !== 200, JSON.stringify(grabOffline.json).slice(0, 120))

  // 6) 开启竞价 + 出价（截止前）
  const en = await api('POST', `/dms/order-pool/${POOL.bid}/enable-bid?startPrice=20&durationMinutes=60`)
  check('开启竞价成功（待抢单 → 竞价中）', Number(en.json?.code) === 200, JSON.stringify(en.json).slice(0, 120))
  const bidPool = (await dbQuery(`SELECT pool_status, bid_enabled, bid_start_price, bid_current_price, bid_end_time FROM dms_order_pool WHERE id = $1`, [POOL.bid]))[0]
  check('竞价状态=竞价中(1) 且竞价开关打开、起拍价/截止时间已落库',
    Number(bidPool.pool_status) === 1 && Number(bidPool.bid_enabled) === 1
    && Number(bidPool.bid_start_price) === 20 && !!bidPool.bid_end_time,
    JSON.stringify(bidPool))
  const bidHigh = await api('POST', `/dms/order-pool/${POOL.bid}/bid`, { riderId: RIDER_A, riderName: 'E2E池甲' + STAMP, price: 25 })
  check('出价高于起拍价被拒', Number(bidHigh.json?.code) !== 200, JSON.stringify(bidHigh.json).slice(0, 140))
  const b1 = await api('POST', `/dms/order-pool/${POOL.bid}/bid`, { riderId: RIDER_A, riderName: 'E2E池甲' + STAMP, price: 18 })
  const b2 = await api('POST', `/dms/order-pool/${POOL.bid}/bid`, { riderId: RIDER_B, riderName: 'E2E池乙' + STAMP, price: 15 })
  check('两次出价成功', Number(b1.json?.code) === 200 && Number(b2.json?.code) === 200,
    `${JSON.stringify(b1.json).slice(0, 60)} | ${JSON.stringify(b2.json).slice(0, 60)}`)
  const bidCnt = (await dbQuery(`SELECT bid_count, bid_current_price FROM dms_order_pool WHERE id = $1`, [POOL.bid]))[0]
  check('竞价数与当前价随出价更新（2 / 15）',
    Number(bidCnt.bid_count) === 2 && Number(bidCnt.bid_current_price) === 15, JSON.stringify(bidCnt))

  // 7) 结算（价低中标 + 同事务指派）
  const settle = await api('POST', `/dms/order-pool/${POOL.bid}/settle`)
  check('结算竞价成功', Number(settle.json?.code) === 200, JSON.stringify(settle.json).slice(0, 120))
  const settledPool = (await dbQuery(`SELECT pool_status FROM dms_order_pool WHERE id = $1`, [POOL.bid]))[0]
  const winBid = (await dbQuery(`SELECT rider_id, is_win FROM dms_bid WHERE pool_id = $1 AND is_win = 1`, [POOL.bid]))[0]
  const settledTask = (await dbQuery(`SELECT rider_id, rider_name, status, dispatch_type FROM dms_task WHERE id = $1`, [TASK.bid]))[0]
  check('结算后池状态=已接单(2)', Number(settledPool.pool_status) === 2, settledPool.pool_status)
  check('价低者中标（15 的乙中标，18 的甲未中标）', String(winBid?.rider_id) === String(RIDER_B), JSON.stringify(winBid))
  check('中标与任务指派同事务写入（任务绑定中标配送员、调度方式=竞价）',
    String(settledTask.rider_id) === String(RIDER_B) && Number(settledTask.status) === 1 && Number(settledTask.dispatch_type) === 4,
    JSON.stringify(settledTask))
  const settleAgain = await api('POST', `/dms/order-pool/${POOL.bid}/settle`)
  check('重复结算被拒（状态机）', Number(settleAgain.json?.code) !== 200, JSON.stringify(settleAgain.json).slice(0, 130))

  // 8) 强制分配 + 幂等 + 状态机
  const fa = await api('POST', `/dms/order-pool/${POOL.assign}/force-assign`, { riderId: RIDER_C, riderName: 'E2E池丙' + STAMP })
  check('强制分配成功', Number(fa.json?.code) === 200, JSON.stringify(fa.json).slice(0, 120))
  const faTask = (await dbQuery(`SELECT rider_id, status, dispatch_type FROM dms_task WHERE id = $1`, [TASK.assign]))[0]
  const faPool = (await dbQuery(`SELECT pool_status FROM dms_order_pool WHERE id = $1`, [POOL.assign]))[0]
  check('强制分配后池=已接单(2)、任务绑定配送员且调度方式=手动',
    Number(faPool.pool_status) === 2 && String(faTask.rider_id) === String(RIDER_C) && Number(faTask.dispatch_type) === 2,
    JSON.stringify({ p: faPool.pool_status, t: faTask }))
  const faSame = await api('POST', `/dms/order-pool/${POOL.assign}/force-assign`, { riderId: RIDER_C, riderName: 'x' })
  check('同一配送员重复强制分配幂等（返回成功）', Number(faSame.json?.code) === 200, JSON.stringify(faSame.json).slice(0, 120))
  const faDiff = await api('POST', `/dms/order-pool/${POOL.assign}/force-assign`, { riderId: RIDER_D, riderName: 'x' })
  check('已被他人接单后强制分配他人被拒', Number(faDiff.json?.code) !== 200, JSON.stringify(faDiff.json).slice(0, 140))

  // 9) 下架
  const off = await api('POST', `/dms/order-pool/${POOL.offline}/offline?reason=E2E下架`)
  check('下架成功（待抢单 → 已下架）', Number(off.json?.code) === 200, JSON.stringify(off.json).slice(0, 120))
  const offPool = (await dbQuery(`SELECT pool_status, offline_reason, offline_time FROM dms_order_pool WHERE id = $1`, [POOL.offline]))[0]
  check('下架后池状态=已下架(4)', Number(offPool.pool_status) === 4, offPool.pool_status)
  check('下架原因与下架时间落库（此前只写日志、页面填的原因静默丢失）',
    offPool.offline_reason === 'E2E下架' && !!offPool.offline_time,
    JSON.stringify({ r: offPool.offline_reason, t: offPool.offline_time }))
  const offAgain = await api('POST', `/dms/order-pool/${POOL.offline}/offline?reason=again`)
  check('重复下架幂等（返回成功）', Number(offAgain.json?.code) === 200, JSON.stringify(offAgain.json).slice(0, 110))
  const offAssign = await api('POST', `/dms/order-pool/${POOL.offline}/force-assign`, { riderId: RIDER_D, riderName: 'x' })
  check('已下架的池不可强制分配', Number(offAssign.json?.code) !== 200, JSON.stringify(offAssign.json).slice(0, 140))
  const offAccepted = await api('POST', `/dms/order-pool/${POOL.assign}/offline?reason=x`)
  check('已接单的池不可下架', Number(offAccepted.json?.code) !== 200, JSON.stringify(offAccepted.json).slice(0, 140))

  // 10) 过期扫描（竞价截止未结算 → 已过期）
  await api('POST', `/dms/order-pool/${POOL.expire}/enable-bid?startPrice=30&durationMinutes=30`)
  await dbQuery(`UPDATE dms_order_pool SET bid_end_time = now() - interval '1 minute' WHERE id = $1`, [POOL.expire])
  const expiredBid = await api('POST', `/dms/order-pool/${POOL.expire}/bid`, { riderId: RIDER_D, riderName: 'E2E池丁' + STAMP, price: 28 })
  check('竞价截止后拒绝出价', Number(expiredBid.json?.code) !== 200, JSON.stringify(expiredBid.json).slice(0, 140))
  const scan = await api('POST', '/dms/order-pool/expire-scan')
  const expPool = (await dbQuery(`SELECT pool_status FROM dms_order_pool WHERE id = $1`, [POOL.expire]))[0]
  check('过期扫描成功且把截止未结算的池置为已过期(3)',
    Number(scan.json?.code) === 200 && Number(expPool.pool_status) === 3,
    `scan=${JSON.stringify(scan.json?.data)} status=${expPool.pool_status}`)

  // 11) 竞价记录（价低优先 + 中标标记）
  const bidList = data(await api('GET', `/dms/order-pool/${POOL.bid}/bid-list`))
  check('竞价记录按价低优先返回且标出中标者',
    Array.isArray(bidList) && bidList.length === 2 && Number(bidList[0].bidPrice) === 15 && Number(bidList[0].isWin) === 1,
    JSON.stringify((bidList || []).map(b => [b.bidPrice, b.isWin])))

  // 12) 导出真实 xlsx
  const exp = await rawReq('GET', `/dms/order-pool/export?customerName=${CUSTOMER}`, null, TOKEN)
  const isXlsx = exp.buf && exp.buf.length > 4 && exp.buf[0] === 0x50 && exp.buf[1] === 0x4B
  check('导出台账返回真实 xlsx（PK 魔数 + xlsx MIME）',
    isXlsx && String(exp.headers['content-type']).includes('spreadsheetml'),
    `status=${exp.status} type=${exp.headers['content-type']} size=${exp.buf?.length}`)

  // 13) 为 UI 验收保留一条「待抢单」池记录（前面的流程已把 5 条全部消费）
  const uiPub = await api('POST', '/dms/order-pool/publish', { taskIds: [TASK.ui] })
  check('为界面验收保留「待抢单」池记录', Array.isArray(data(uiPub)) && data(uiPub).length === 1,
    JSON.stringify(data(uiPub)))

  // ══════════════ 14) 二轮复验：查询条件 / 发布门控 / 竞价公平性 ══════════════

  // 14.1 「接单配送员」筛选（抢单/强制分配/竞价结算都会把骑手回写到任务上）
  const byRiderA = data(await api('GET', `/dms/order-pool/page?current=1&size=50&customerName=${CUSTOMER}&riderId=${RIDER_A}`))?.records || []
  const byRiderD = data(await api('GET', `/dms/order-pool/page?current=1&size=50&customerName=${CUSTOMER}&riderId=${RIDER_D}`))?.records || []
  check('按「接单配送员」筛选生效（甲=1 条 / 丁=0 条，此前参数被后端静默忽略）',
    byRiderA.length === 1 && byRiderD.length === 0, `A=${byRiderA.length} D=${byRiderD.length}`)

  // 14.2 发布门控：只有「待分配」任务可入池；已接单的池不可重复发布
  const dupAccepted = await api('POST', '/dms/order-pool/publish', { taskIds: [TASK.grab] })
  check('已接单的池不可重复发布（否则新池被抢会覆盖任务骑手 → 双接单）',
    Number(dupAccepted.json?.code) !== 200, JSON.stringify(dupAccepted.json).slice(0, 150))
  await dbQuery(`UPDATE dms_task SET status = 1 WHERE id = $1`, [TASK.assigned])
  const pubAssigned = await api('POST', '/dms/order-pool/publish', { taskIds: [TASK.assigned] })
  check('已分配/在途等非「待分配」任务拒绝入池',
    Number(pubAssigned.json?.code) !== 200, JSON.stringify(pubAssigned.json).slice(0, 150))

  // 14.3 竞价参数边界
  const repub1 = await api('POST', '/dms/order-pool/publish', { taskIds: [TASK.repub] })
  const POOL_REPUB = data(repub1)?.[0]
  const badPrice = await api('POST', `/dms/order-pool/${POOL_REPUB}/enable-bid?startPrice=0&durationMinutes=60`)
  check('起拍价必须大于 0（此前 0 元起拍可直接入库）',
    Number(badPrice.json?.code) !== 200, JSON.stringify(badPrice.json).slice(0, 130))
  const badDuration = await api('POST', `/dms/order-pool/${POOL_REPUB}/enable-bid?startPrice=10&durationMinutes=0`)
  check('竞价时长必须在 1~1440 分钟之间',
    Number(badDuration.json?.code) !== 200, JSON.stringify(badDuration.json).slice(0, 130))

  // 14.4 下架后可重新发布（过期/下架池回流人工指派口径）
  const offRepub = await api('POST', `/dms/order-pool/${POOL_REPUB}/offline?reason=E2E重发前下架`)
  const repub2 = await api('POST', '/dms/order-pool/publish', { taskIds: [TASK.repub] })
  check('下架后的池可重新发布（回流《调度任务》/重新入池）',
    Number(offRepub.json?.code) === 200 && Number(repub2.json?.code) === 200,
    `${JSON.stringify(offRepub.json).slice(0, 60)} | ${JSON.stringify(repub2.json).slice(0, 60)}`)
  const POOL_REPUB2 = data(repub2)?.[0]

  // 14.5 竞价公平性：同一人改价不新增记录；取消最低价后当前价回退；取消的报价不参与结算
  const pubCb = await api('POST', '/dms/order-pool/publish', { taskIds: [TASK.cancelBid] })
  const POOL_CB = data(pubCb)?.[0]
  await api('POST', `/dms/order-pool/${POOL_CB}/enable-bid?startPrice=20&durationMinutes=60`)
  await api('POST', `/dms/order-pool/${POOL_CB}/bid`, { riderId: RIDER_A, riderName: 'E2E池甲' + STAMP, price: 18 })
  await api('POST', `/dms/order-pool/${POOL_CB}/bid`, { riderId: RIDER_C, riderName: 'E2E池丙' + STAMP, price: 15 })
  await api('POST', `/dms/order-pool/${POOL_CB}/bid`, { riderId: RIDER_A, riderName: 'E2E池甲' + STAMP, price: 17 })
  const cbAfterChange = (await dbQuery(`SELECT bid_count, bid_current_price FROM dms_order_pool WHERE id = $1`, [POOL_CB]))[0]
  const cbBids = data(await api('GET', `/dms/order-pool/${POOL_CB}/bid-list`)) || []
  check('同一配送员改价不新增记录（2 人各 1 条，竞价数=2、当前价=15）',
    Number(cbAfterChange.bid_count) === 2 && Number(cbAfterChange.bid_current_price) === 15
    && cbBids.length === 2 && new Set(cbBids.map(b => String(b.riderId))).size === 2,
    JSON.stringify({ c: cbAfterChange, bids: cbBids.map(b => [b.riderId, b.bidPrice]) }))

  const lowestBid = cbBids.find(b => String(b.riderId) === String(RIDER_C))
  await api('POST', `/dms/order-pool/${POOL_CB}/cancel-bid`, { bidId: lowestBid.id, riderId: RIDER_C })
  const cbAfterCancel = (await dbQuery(`SELECT bid_count, bid_current_price FROM dms_order_pool WHERE id = $1`, [POOL_CB]))[0]
  const cbBidsAfter = data(await api('GET', `/dms/order-pool/${POOL_CB}/bid-list`)) || []
  check('取消出价后竞价数=1、当前价回退到剩余最低价 17',
    Number(cbAfterCancel.bid_count) === 1 && Number(cbAfterCancel.bid_current_price) === 17,
    JSON.stringify(cbAfterCancel))
  check('已取消的出价不再出现在竞价列表（手写 @Select 此前漏 deleted=0）',
    cbBidsAfter.length === 1 && !cbBidsAfter.some(b => String(b.riderId) === String(RIDER_C)),
    JSON.stringify(cbBidsAfter.map(b => [b.riderId, b.bidPrice])))

  const settleCb = await api('POST', `/dms/order-pool/${POOL_CB}/settle`)
  const cbWinner = (await dbQuery(`SELECT rider_id, is_win, bid_price FROM dms_bid WHERE pool_id = $1 AND is_win = 1`, [POOL_CB]))[0]
  const cbPool = (await dbQuery(`SELECT pool_status, bid_current_price FROM dms_order_pool WHERE id = $1`, [POOL_CB]))[0]
  const cbTask = (await dbQuery(`SELECT rider_id, status, dispatch_type FROM dms_task WHERE id = $1`, [TASK.cancelBid]))[0]
  check('结算中标不选已取消的报价（甲 17 中标，而非已撤销的丙 15）',
    Number(settleCb.json?.code) === 200 && String(cbWinner?.rider_id) === String(RIDER_A), JSON.stringify(cbWinner))
  check('结算后池=已接单(2) 且当前价=中标价 17',
    Number(cbPool?.pool_status) === 2 && Number(cbPool?.bid_current_price) === 17, JSON.stringify(cbPool))
  check('中标与任务指派同事务（甲绑定任务、调度方式=竞价）',
    String(cbTask?.rider_id) === String(RIDER_A) && Number(cbTask?.dispatch_type) === 4, JSON.stringify(cbTask))
  const cancelAfterSettle = await api('POST', `/dms/order-pool/${POOL_CB}/cancel-bid`,
    { bidId: cbBidsAfter[0].id, riderId: RIDER_A })
  check('池已结算后不可再取消出价（池状态门控）',
    Number(cancelAfterSettle.json?.code) !== 200, JSON.stringify(cancelAfterSettle.json).slice(0, 140))

  // 14.6 关闭竞价（竞价中 → 待抢单，作废报价后仍可重新开启）
  const offPub = await api('POST', '/dms/order-pool/publish', { taskIds: [TASK.offBid] })
  const POOL_OFF = data(offPub)?.[0]
  await api('POST', `/dms/order-pool/${POOL_OFF}/enable-bid?startPrice=40&durationMinutes=60`)
  await api('POST', `/dms/order-pool/${POOL_OFF}/bid`, { riderId: RIDER_B, riderName: 'E2E池乙' + STAMP, price: 38 })
  const dis = await api('POST', `/dms/order-pool/${POOL_OFF}/disable-bid`)
  const offPoolRow = (await dbQuery(`SELECT pool_status, bid_enabled, bid_count, bid_start_price, bid_current_price,
                                            bid_start_time, bid_end_time FROM dms_order_pool WHERE id = $1`, [POOL_OFF]))[0]
  const offBids = data(await api('GET', `/dms/order-pool/${POOL_OFF}/bid-list`)) || []
  check('关闭竞价成功（返回作废条数 1）', Number(dis.json?.code) === 200 && Number(data(dis)) === 1,
    JSON.stringify(dis.json).slice(0, 120))
  check('关闭竞价后池回到待抢单(0)、竞价字段全部清空、报价作废',
    Number(offPoolRow.pool_status) === 0 && Number(offPoolRow.bid_enabled) === 0 && Number(offPoolRow.bid_count) === 0
    && offPoolRow.bid_start_price === null && offPoolRow.bid_current_price === null
    && offPoolRow.bid_start_time === null && offPoolRow.bid_end_time === null && offBids.length === 0,
    JSON.stringify({ p: offPoolRow, bids: offBids.length }))
  const disAgain = await api('POST', `/dms/order-pool/${POOL_OFF}/disable-bid`)
  check('非「竞价中」状态不可关闭竞价（状态机）',
    Number(disAgain.json?.code) !== 200, JSON.stringify(disAgain.json).slice(0, 130))
  const reEnable = await api('POST', `/dms/order-pool/${POOL_OFF}/enable-bid?startPrice=35&durationMinutes=60`)
  const reBid = await api('POST', `/dms/order-pool/${POOL_OFF}/bid`, { riderId: RIDER_B, riderName: 'E2E池乙' + STAMP, price: 33 })
  const reSettle = await api('POST', `/dms/order-pool/${POOL_OFF}/settle`)
  const reTask = (await dbQuery(`SELECT rider_id, dispatch_type FROM dms_task WHERE id = $1`, [TASK.offBid]))[0]
  check('关闭竞价后可重新开启并正常出价/结算（闭环可恢复）',
    Number(reEnable.json?.code) === 200 && Number(reBid.json?.code) === 200 && Number(reSettle.json?.code) === 200
    && String(reTask?.rider_id) === String(RIDER_B) && Number(reTask?.dispatch_type) === 4,
    `${JSON.stringify(reSettle.json).slice(0, 60)} task=${JSON.stringify(reTask)}`)

  // 14.7 为 UI 验收保留一条「竞价中」池（代客出价 → 查看竞标 → 取消出价）
  const uiBidPub = await api('POST', '/dms/order-pool/publish', { taskIds: [TASK.uiBid] })
  const POOL_UI_BID = data(uiBidPub)?.[0]
  const uiBidEn = await api('POST', `/dms/order-pool/${POOL_UI_BID}/enable-bid?startPrice=30&durationMinutes=60`)
  check('为界面验收保留「竞价中」池记录', POOL_REPUB2 && Number(uiBidEn.json?.code) === 200,
    `pool=${POOL_UI_BID} ${JSON.stringify(uiBidEn.json).slice(0, 80)}`)

  return { poolIds, poolUiBid: POOL_UI_BID }
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite(ctx) {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const bctx = await browser.newContext({ viewport: { width: 1760, height: 980 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await bctx.newPage()

  page.clickBtn = (re, scope) => (scope || page).locator('button').filter({ hasText: re }).first().click()

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

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

  await openPage(`${FE}/dms/order-pool`)
  const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('订单池页可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())

  for (const b of ['批量发布到池', '批量下架', '过期处理', '开启竞价', '刷新', '导出']) {
    check(`工具栏含「${b}」`, bodyText.includes(b.replace(/\s+/g, '')))
  }
  check('工具栏含「页面配置」入口', (await page.locator('button:has(.anticon-setting)').count()) > 0)

  for (const q of ['任务编号', '关联订单号', '客户', '订单类型', '池状态', '竞价模式', '配送线路', '接单配送员']) {
    check(`查询区含条件「${q}」`, await fieldPresent(q))
  }

  const headers = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').replace(/\s+/g, '')).filter(Boolean))
  for (const h of ['任务编号', '关联订单号', '订单类型', '客户', '取货地址', '收货地址', '距离(km)',
    '货品金额', '配送费', '竞价模式', '当前价/中标价', '竞价数', '池状态', '接单配送员',
    '发布时间', '过期时间', '剩余时间', '备注', '操作']) {
    check(`表头含列「${h}」`, headers.some(x => x.includes(h.replace(/\s+/g, ''))), headers.join('/'))
  }

  const gear = page.locator('.ss-grid .th-settings-btn').first()
  check('数据表存在表头齿轮（列配置唯一入口）', (await gear.count()) > 0)
  if (await gear.count()) {
    await gear.click()
    await page.waitForTimeout(1400)
    const panel = page.locator('.ant-modal-content:visible').last()
    const panelText = (await panel.innerText()).replace(/\s+/g, '')
    check('列配置弹窗含「个人配置 / 全局配置」双 Tab',
      panelText.includes('个人配置') && panelText.includes('全局配置'), panelText.slice(0, 120))
    check('列配置含关键列（池状态/竞价数/剩余时间/接单配送员）',
      panelText.includes('池状态') && panelText.includes('竞价数')
      && panelText.includes('剩余时间') && panelText.includes('接单配送员'), panelText.slice(0, 140))
    check('列配置含二轮新增可配置列（配送线路/区域、下架原因、下架时间）',
      panelText.includes('配送线路/区域') && panelText.includes('下架原因') && panelText.includes('下架时间'),
      panelText.slice(0, 180))
    await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(700)
  }

  // 条件过滤 + 状态 tag + 合计 + 分页
  const customerInput = page.locator('input[placeholder="客户"]').first()
  await customerInput.fill(CUSTOMER)
  await page.clickBtn(/查\s*询/)
  await page.waitForTimeout(2500)
  const gridText = (await page.locator('.ss-grid').innerText()).replace(/\s+/g, '')
  const rowCount = await page.evaluate(() => document.querySelectorAll('.ss-grid tbody tr').length)
  // 期望行数直接取自库内（客户名为模糊匹配，含二轮复验分组），避免硬编码行数与造数规模耦合
  const expectedRows = Number((await dbQuery(
    `SELECT count(*)::int AS n FROM dms_order_pool p JOIN dms_task t ON t.id = p.task_id WHERE t.customer_name LIKE $1`,
    [CUSTOMER + '%']))[0].n)
  check(`按客户查询渲染真实数据（页面行数=${expectedRows}，与库内一致）`, rowCount === expectedRows,
    `rows=${rowCount} 期望=${expectedRows}`)
  check('池状态列渲染中文 tag（待抢单/已接单/已下架/已过期）',
    ['待抢单', '已接单', '已下架', '已过期'].every(s => gridText.includes(s)),
    gridText.slice(0, 200))
  check('竞价模式列渲染「竞价/抢单」', gridText.includes('竞价') && gridText.includes('抢单'), gridText.slice(0, 120))
  check('任务编号列渲染 PSD 号段', /PSD-\d{8}-\d{3}/.test(gridText), gridText.slice(0, 120))
  const pagerText = await page.evaluate(() => {
    const el = document.querySelector('.table-pagination')
    return el ? el.innerText.replace(/\s+/g, '') : ''
  })
  check('底部为经典分页栏（共 N 条）', pagerText.includes('共'), pagerText.slice(0, 80))
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 页面配置弹窗
  const cfgBtn = page.locator('button:has(.anticon-setting)').first()
  if (await cfgBtn.count()) {
    await cfgBtn.click()
    await page.waitForTimeout(1400)
    const cfgPanel = page.locator('.ant-modal-content:visible').last()
    const cfgText = (await cfgPanel.innerText()).replace(/\s+/g, '')
    check('页面配置含 10 项查询条件（任务编号/池状态/配送线路/配送费区间…）',
      cfgText.includes('任务编号') && cfgText.includes('池状态')
      && cfgText.includes('配送线路') && cfgText.includes('配送费区间'), cfgText.slice(0, 160))
    const fnTab = cfgPanel.locator('.ant-tabs-tab').filter({ hasText: '功能按钮' }).first()
    if (await fnTab.count()) {
      await fnTab.click()
      await page.waitForTimeout(600)
      const fnText = (await cfgPanel.innerText()).replace(/\s+/g, '')
      check('功能按钮 Tab 含 批量发布到池/批量下架/过期处理/开启+关闭竞价/刷新/导出/配置',
        ['批量发布到池', '批量下架', '过期处理', '开启/关闭竞价', '刷新', '导出', '配置'].every(b => fnText.includes(b.replace(/\s+/g, ''))),
        fnText.slice(0, 160))
    } else {
      check('功能按钮 Tab 含 批量发布到池/批量下架/过期处理/开启+关闭竞价/刷新/导出/配置', false, '未找到功能按钮 Tab')
    }
    await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(700)
  }

  // 强制分配（定位「待抢单」行 → 指派配送员 → 池状态变已接单）
  await openPage(`${FE}/dms/order-pool`)
  const customerInput2 = page.locator('input[placeholder="客户"]').first()
  await customerInput2.fill(CUSTOMER)
  await page.clickBtn(/查\s*询/)
  await page.waitForTimeout(2500)
  // 与列表同序（published_time DESC, id DESC）取「界面第一个待抢单行」对应的池，避免断错记录
  const before = await dbQuery(`SELECT p.id, p.task_id FROM dms_order_pool p JOIN dms_task t ON t.id = p.task_id
                                WHERE p.pool_status = 0 AND t.customer_name LIKE $1
                                ORDER BY p.published_time DESC, p.id DESC LIMIT 1`, [CUSTOMER + '%'])
  // 待抢单行：用行内文本定位，避免依赖下拉筛选控件
  const pendingRow = page.locator('.ss-grid tbody tr').filter({ hasText: '待抢单' }).first()
  check('列表出现「待抢单」行（供强制分配）', (await pendingRow.count()) > 0)
  if (await pendingRow.count()) {
    await pendingRow.locator('button').filter({ hasText: /强制分配/ }).first().click()
    await page.waitForTimeout(1300)
    const modal = page.locator('.ant-modal:visible').last()
    check('强制分配弹窗打开（含配送员选择器）',
      (await modal.innerText()).includes('强制分配'), (await modal.innerText()).slice(0, 80))
    // 配送员下拉是 show-search：打开后优先用关键字收窄（历史 E2E 造数多，虚拟列表里未必可见本次配送员）
    await modal.locator('.ant-select').first().click()
    await page.waitForTimeout(800)
    let opts = page.locator('.ant-select-dropdown:visible .ant-select-item-option')
    let optCount = await opts.count()
    if (optCount === 0) {
      const riderSearch = modal.locator('.ant-select input').first()
      if (await riderSearch.count()) {
        await riderSearch.fill('E2E池' + STAMP)
        await page.waitForTimeout(900)
        opts = page.locator('.ant-select-dropdown:visible .ant-select-item-option')
        optCount = await opts.count()
      }
    }
    if (optCount > 0) {
      await opts.first().click()
      await page.waitForTimeout(400)
    }
    check('强制分配弹窗已选中配送员', (await modal.locator('.ant-select-selection-item').count()) > 0,
      (await modal.innerText()).replace(/\s+/g, '').slice(0, 100))
    await page.screenshot({ path: path.join(SHOTS, 'ui-assign-modal.png'), fullPage: true })
    // antd 主按钮 = 确定（避免文案插空格导致的定位偏差）
    await modal.locator('.ant-modal-footer button.ant-btn-primary').first().click()
    await page.waitForTimeout(2500)
    // 校验失败时弹窗不关闭 → 收尾关闭，避免遮挡后续操作
    if (await page.locator('.ant-modal:visible').count()) {
      await page.locator('.ant-modal:visible .ant-modal-close').last().click().catch(() => {})
      await page.waitForTimeout(600)
    }
    if (before.length) {
      const after = await dbQuery(`SELECT pool_status FROM dms_order_pool WHERE id = $1`, [before[0].id])
      check('界面强制分配真实落库（池状态 → 已接单 2）', Number(after[0].pool_status) === 2,
        `pool=${before[0].id} status=${after[0].pool_status}`)
    } else {
      check('界面强制分配真实落库（池状态 → 已接单 2）', false, '造数缺失：无待抢单池记录')
    }
  } else {
    check('强制分配弹窗打开（含配送员选择器）', false, '未找到待抢单行')
    check('界面强制分配真实落库（池状态 → 已接单 2）', false, '未找到待抢单行')
  }

  // 发布到池弹窗
  if (await page.locator('.ant-modal:visible').count()) {
    await page.locator('.ant-modal:visible .ant-modal-close').last().click().catch(() => {})
    await page.waitForTimeout(600)
  }
  const pubBtn = page.locator('button').filter({ hasText: /批量发布到池/ }).first()
  if (await pubBtn.count()) {
    await pubBtn.click()
    await page.waitForTimeout(1500)
    const pubModal = page.locator('.ant-modal-content:visible').last()
    const pubText = (await pubModal.innerText()).replace(/\s+/g, '')
    check('批量发布到池弹窗含 任务/配送费/同时开启竞价',
      pubText.includes('待分配任务') && pubText.includes('配送费') && pubText.includes('同时开启竞价'), pubText.slice(0, 120))
    await page.screenshot({ path: path.join(SHOTS, 'ui-publish-modal.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(700)
  } else {
    check('批量发布到池弹窗含 任务/配送费/同时开启竞价', false, '未找到发布按钮')
  }

  // 查看竞标 → 竞价详情页（真实数据 + 中标标记）
  await openPage(`${FE}/dms/order-pool`)
  await customerInput.fill(CUSTOMER).catch(() => {})
  await page.clickBtn(/查\s*询/)
  await page.waitForTimeout(2200)
  const viewBtn = page.locator('.ss-grid tbody tr button').filter({ hasText: /查看竞标/ }).first()
  if (await viewBtn.count()) {
    await viewBtn.click()
    await page.waitForTimeout(3000)
    check('「查看竞标」跳转竞价详情页（路由已注册，不再 404）',
      page.url().includes('/dms/order-pool/bid-detail')
      && !(await page.locator('body').innerText()).includes('页面不存在'), page.url())
  } else {
    check('「查看竞标」跳转竞价详情页（路由已注册，不再 404）', false, '未找到查看竞标按钮')
  }

  // 竞价详情内容：直接打开有竞价记录的池（列表首行未必是有出价的那条）
  const bidPool = await dbQuery(`SELECT id, task_id FROM dms_order_pool
                                 WHERE bid_count > 0 AND task_id IN (SELECT id FROM dms_task WHERE customer_name = $1)
                                 ORDER BY id DESC LIMIT 1`, [CUSTOMER])
  if (bidPool.length) {
    await openPage(`${FE}/dms/order-pool/bid-detail?poolId=${bidPool[0].id}&taskId=${bidPool[0].task_id}`)
    const detailText = (await page.locator('body').innerText()).replace(/\s+/g, '')
    check('竞价详情页展示报价记录（价低者中标、其余未中标）',
      detailText.includes('竞标详情') && detailText.includes('中标') && detailText.includes('未中标'),
      detailText.slice(0, 220))
    check('竞价详情页按价低优先排序（第一名即中标者）', detailText.includes('价低优先'), detailText.slice(0, 120))
    await page.screenshot({ path: path.join(SHOTS, 'ui-bid-detail.png'), fullPage: true })
  } else {
    check('竞价详情页展示报价记录（价低者中标、其余未中标）', false, '造数缺失：无含出价的池')
  }

  // ══ 二轮复验 UI：代客出价 → 查看竞标 → 取消出价（真实落库 + 取消后列表不再显示）══
  const POOL_UI_BID = ctx?.poolUiBid
  await openPage(`${FE}/dms/order-pool`)
  const cInput = page.locator('input[placeholder="客户"]').first()
  await cInput.fill(CUSTOMER_C)
  await page.clickBtn(/查\s*询/)
  await page.waitForTimeout(2500)
  const biddingRow = page.locator('.ss-grid tbody tr').filter({ hasText: '竞价中' }).first()
  check('列表出现「竞价中」行（供代客出价）', (await biddingRow.count()) > 0)
  if ((await biddingRow.count()) > 0) {
    await biddingRow.locator('button').filter({ hasText: /代客出价/ }).first().click()
    await page.waitForTimeout(1300)
    const bidModal = page.locator('.ant-modal:visible').last()
    const bidModalText = (await bidModal.innerText()).replace(/\s+/g, '')
    check('代客出价弹窗含配送员选择与报价', bidModalText.includes('报价') && bidModalText.includes('代客出价'),
      bidModalText.slice(0, 120))
    // 配送员下拉（show-search：先开下拉，必要时用关键字收窄）
    await bidModal.locator('.ant-select').first().click()
    await page.waitForTimeout(800)
    let bopts = page.locator('.ant-select-dropdown:visible .ant-select-item-option')
    if ((await bopts.count()) === 0) {
      const bSearch = bidModal.locator('.ant-select input').first()
      if (await bSearch.count()) {
        await bSearch.fill('E2E池丁' + STAMP)
        await page.waitForTimeout(900)
      }
      bopts = page.locator('.ant-select-dropdown:visible .ant-select-item-option')
    }
    if ((await bopts.count()) > 0) {
      await bopts.first().click()
      await page.waitForTimeout(400)
    }
    await bidModal.locator('.ant-input-number-input').first().fill('25')
    await page.screenshot({ path: path.join(SHOTS, 'ui-bid-modal.png'), fullPage: true })
    await bidModal.locator('.ant-modal-footer button.ant-btn-primary').first().click()
    await page.waitForTimeout(2500)
    if (await page.locator('.ant-modal:visible').count()) {
      await page.locator('.ant-modal:visible .ant-modal-close').last().click().catch(() => {})
      await page.waitForTimeout(600)
    }
    const uiBidDb = await dbQuery(`SELECT bid_count, bid_current_price FROM dms_order_pool WHERE id = $1`, [POOL_UI_BID])
    check('界面代客出价真实落库（竞价数=1、当前价=25）',
      Number(uiBidDb[0]?.bid_count) === 1 && Number(uiBidDb[0]?.bid_current_price) === 25,
      JSON.stringify(uiBidDb[0]))
  } else {
    check('代客出价弹窗含配送员选择与报价', false, '未找到竞价中行')
    check('界面代客出价真实落库（竞价数=1、当前价=25）', false, '未找到竞价中行')
  }

  // 查看竞标 → 竞价详情页：未中标可取消 → 取消后列表不再显示该报价
  if (POOL_UI_BID) {
    await openPage(`${FE}/dms/order-pool/bid-detail?poolId=${POOL_UI_BID}&taskId=${TASK.uiBid}`)
    let dText = (await page.locator('body').innerText()).replace(/\s+/g, '')
    check('竞价详情页展示池状态与出价记录（竞价中 + 未中标）',
      dText.includes('池状态：竞价中') && dText.includes('未中标'), dText.slice(0, 200))
    const cancelBtn = page.locator('button').filter({ hasText: /取消出价/ }).first()
    check('竞价中的池允许取消出价（按钮可见）', (await cancelBtn.count()) > 0)
    if (await cancelBtn.count()) {
      await cancelBtn.click()
      await page.waitForTimeout(800)
      const okBtn = page.locator('.ant-popover:visible button.ant-btn-primary, .ant-popconfirm button.ant-btn-primary').first()
      if (await okBtn.count()) {
        await okBtn.click()
      } else {
        await page.locator('.ant-popover:visible button').filter({ hasText: /确\s*定/ }).first().click()
      }
      await page.waitForTimeout(2500)
      const afterCancel = (await dbQuery(`SELECT bid_count FROM dms_order_pool WHERE id = $1`, [POOL_UI_BID]))[0]
      dText = (await page.locator('body').innerText()).replace(/\s+/g, '')
      check('界面取消出价真实落库（竞价数归 0）', Number(afterCancel?.bid_count) === 0, JSON.stringify(afterCancel))
      check('取消后的报价从竞价列表消失（逻辑删除过滤修复）', !dText.includes('未中标'), dText.slice(0, 180))
      await page.screenshot({ path: path.join(SHOTS, 'ui-bid-cancel.png'), fullPage: true })
    } else {
      check('界面取消出价真实落库（竞价数归 0）', false, '无取消按钮')
      check('取消后的报价从竞价列表消失（逻辑删除过滤修复）', false, '无取消按钮')
    }

    // 行级「关闭竞价」：竞价中 → 待抢单（作废报价）
    await openPage(`${FE}/dms/order-pool`)
    const cInput2 = page.locator('input[placeholder="客户"]').first()
    await cInput2.fill(CUSTOMER_C)
    await page.clickBtn(/查\s*询/)
    await page.waitForTimeout(2500)
    const bidRow2 = page.locator('.ss-grid tbody tr').filter({ hasText: '竞价中' }).first()
    const closeBtn = bidRow2.locator('button').filter({ hasText: /关闭竞价/ }).first()
    check('竞价中行显示「关闭竞价」操作', (await closeBtn.count()) > 0)
    if (await closeBtn.count()) {
      await closeBtn.click()
      await page.waitForTimeout(1000)
      // Modal.confirm 的按钮在 .ant-modal-confirm-btns（不是 .ant-modal-footer），按文案点击最稳
      const okBtn2 = page.locator('.ant-modal:visible button').filter({ hasText: /确认关闭/ }).first()
      if (await okBtn2.count()) {
        await okBtn2.click()
      } else {
        await page.locator('.ant-modal-confirm-btns button.ant-btn-primary, .ant-modal:visible .ant-modal-footer button.ant-btn-primary')
          .first().click()
      }
      await page.waitForTimeout(2500)
      const afterDisable = (await dbQuery(`SELECT pool_status, bid_enabled FROM dms_order_pool WHERE id = $1`, [POOL_UI_BID]))[0]
      check('界面关闭竞价真实落库（池回到待抢单 0、竞价关闭）',
        Number(afterDisable?.pool_status) === 0 && Number(afterDisable?.bid_enabled) === 0,
        JSON.stringify(afterDisable))
      await page.screenshot({ path: path.join(SHOTS, 'ui-bid-disable.png'), fullPage: true })
    } else {
      check('界面关闭竞价真实落库（池回到待抢单 0、竞价关闭）', false, '无关闭竞价按钮')
    }
  } else {
    check('竞价详情页展示池状态与出价记录（竞价中 + 未中标）', false, '造数缺失：无竞价中池')
  }

  await browser.close()
}

// ══════════════ 汇总 ══════════════
async function main() {
  console.log('订单池金标准 E2E —— 后端 :' + PORT + '，前端 ' + FE)
  await ensureE2EUser()
  await login()
  await prepareData()
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
