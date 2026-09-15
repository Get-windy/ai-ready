/*
 * 配送查询（配发收 → 配送业务 → 配送查询，菜单 70150）金标准端到端验证
 *   · 口径依据：《配送查询开发文档》—— 24 列（默认 20）/ 无页面配置弹窗 /
 *               13 项查询条件 + 9 段快捷时间 / 工具栏 刷新·批量打印·打印(F8)·导出 /
 *               底部合计行 / 经典分页栏 / 配送状态对外三值（待配送·配送中·已配送）
 *   · API 验收：多条件分页（日期范围/三值状态/司机/车辆/送货员/任务编号/备注/
 *               配送单据编号/制单人/制单时间/显示红冲）+ 下拉候选 + 打印计数 + 导出 xlsx
 *   · UI  验收：骨架（CategoryListLayout + BillTableList）+ 工具栏按钮 + 横向网格查询区 +
 *               24 列表头（默认隐藏 4）+ 表头齿轮列配置（个人/全局）+ 勾选 + 合计行 +
 *               经典分页 + 状态三值 tag + 条件过滤真实生效
 *   · DB  核对：造数落库（dms_task / dms_task_item）与打印次数回写
 *
 * 用法：node tools/e2e-dispatch-query.cjs            （默认后端 5680、前端 5656）
 *      ERP_PORT=5680 FE_URL=http://localhost:5656 node tools/e2e-dispatch-query.cjs
 *
 * 前置：验收实例需包含 DMS 模块；devdb 需已应用 dms_task 台账列迁移。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5680)
const SHOTS = 'I:/AI-Ready/tool-results/dispatch-query'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const DSN = {
  host: 'localhost',
  port: 5432,
  database: 'devdb',
  user: 'devuser',
  password: 'devuser123',
}

// 专用验收账号：并行会话共用 admin 会被 sa-token 互踢（验收中途 401）
const E2E_USER = process.env.E2E_USER || 'e2e_dispatch'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

const STAMP = Date.now().toString().slice(-6)
const DD = 'E2EDQ'           // order_no 前缀（造数标记，便于清理与筛选）
const MARK = DD + STAMP
const BILL = 'XSCKD-' + MARK // 配送单据编号

/** 造数用指定配送日期：必须落在页面默认「近一周」范围内，否则 UI 查询看不到 */
function dayOffset(n) {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d.toISOString().slice(0, 10)
}
const D1 = dayOffset(1)
const D2 = dayOffset(2)
const D3 = dayOffset(3)
const D4 = dayOffset(4)

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
  // 主键用时间戳派生，避免与其它会话的固定验收 id 撞键
  const uid = Number(String(Date.now()).slice(-15) + '001')
  await dbQuery(
    `INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                           user_type, is_super_admin, is_tenant_admin, status, data_scope)
     SELECT $2, 1, 0, now(), now(), $1, password, 'E2E配送查询', 'E2E配送查询',
            1, false, false, 1, 'ALL'
     FROM sys_user WHERE username = 'admin'`, [E2E_USER, uid])
  await dbQuery(`INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
                 VALUES ($1, $2, 1, 1, now())`, [uid + 1, uid])
  await dbQuery(`INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
                 VALUES ($1, $2, 1, true, 1, now(), now())`, [uid + 2, uid])
  console.log(`已创建验收账号 ${E2E_USER}（密码同 admin，id=${uid}）`)
}

// ══════════════ HTTP 基础设施 ══════════════
/**
 * 规范化请求路径：先按段解码再编码一次，保证中文查询参数编码为 UTF-8 %XX
 * （直接 encodeURI 已编码串或原样发中文都会导致后端收到乱码）
 */
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
      + `\n提示：请用独立账号（默认 ${E2E_USER}/${E2E_PWD}），见 tools/e2e-dispatch-query-user.sql。`)
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
let RIDER_A = null, RIDER_B = null, VEHICLE_A = null
const TASK_IDS = {}   // { pending, delivering, delivered, cancelled }

async function prepareData() {
  console.log('\n═══ 〇、造数准备 ═══')
  // 清理上一次遗留（物理删除，标记隔离）
  await dbQuery(`DELETE FROM dms_task_item WHERE task_id IN (SELECT id FROM dms_task WHERE order_no LIKE '${DD}%')`)
  await dbQuery(`DELETE FROM dms_task WHERE order_no LIKE '${DD}%'`)

  // 配送员（司机 / 送货员同源档案）与车辆
  const r1 = await dbQuery(
    `INSERT INTO dms_rider (tenant_id, rider_type, real_name, phone, status, verify_status, deleted, create_time, update_time)
     VALUES (1, 2, $1, '13800000001', 1, 1, 0, now(), now()) RETURNING id`, ['E2E司机' + STAMP])
  RIDER_A = r1[0].id
  const r2 = await dbQuery(
    `INSERT INTO dms_rider (tenant_id, rider_type, real_name, phone, status, verify_status, deleted, create_time, update_time)
     VALUES (1, 1, $1, '13800000002', 1, 1, 0, now(), now()) RETURNING id`, ['E2E送货员' + STAMP])
  RIDER_B = r2[0].id
  const v1 = await dbQuery(
    `INSERT INTO dms_vehicle (tenant_id, vehicle_code, plate_no, vehicle_type, status, deleted, create_time, update_time)
     VALUES (1, $1, $2, 1, 1, 0, now(), now()) RETURNING id`, ['E2EV' + STAMP, '京E' + STAMP])
  VEHICLE_A = v1[0].id
  console.log(`  司机=${RIDER_A} 送货员=${RIDER_B} 车辆=${VEHICLE_A}`)

  // 4 个配送任务：待配送(0) / 配送中(4) / 已配送(6) / 已取消(7)
  const base = {
    orderType: 1,
    customerName: 'E2E配送客户' + STAMP,
    sourceWarehouseId: null,
    riderId: RIDER_A,
    riderName: 'E2E司机' + STAMP,
    vehicleId: VEHICLE_A,
    vehicleName: '京E' + STAMP,
    deliverymanId: RIDER_B,
    deliverymanName: 'E2E送货员' + STAMP,
    depositAmount: 120,
    boxQuantity: 6,
    remark: 'E2E备注' + STAMP,
    items: [
      { productName: 'E2E商品甲', quantity: 2, unitPrice: 50, weight: 1.5, volume: 0.2 },
      { productName: 'E2E商品乙', quantity: 3, unitPrice: 20, weight: 0.8, volume: 0.1 }
    ]
  }
  const mk = async (suffix, deliveryDate, sourceBillNo) => {
    const res = await api('POST', '/dms/task/save', {
      ...base,
      deliveryDate,
      orderNo: MARK + suffix,
      sourceBillNo,
      taskNo: null
    })
    const d = data(res)
    if (!d || !d.id) throw new Error('造数失败: ' + JSON.stringify(res.json).slice(0, 300))
    return d.id
  }

  TASK_IDS.pending = await mk('-P', D1, BILL + '-P')
  TASK_IDS.delivering = await mk('-D', D2, BILL + '-D')
  TASK_IDS.delivered = await mk('-F', D3, BILL + '-F')
  TASK_IDS.cancelled = await mk('-C', D4, BILL + '-C')

  // 状态流转：配送中 0→1→2→3→4；已配送 继续 →5→6；已取消 0→7
  const flow = async (id, chain) => {
    for (const [from, to] of chain) {
      const r = await api('PUT', `/dms/task/${id}/status`, { fromStatus: from, toStatus: to })
      if (!r.json || Number(r.json.code) !== 200) {
        throw new Error(`状态流转失败 ${from}->${to}: ` + JSON.stringify(r.json).slice(0, 200))
      }
    }
  }
  await flow(TASK_IDS.delivering, [[0, 1], [1, 2], [2, 3], [3, 4]])
  await flow(TASK_IDS.delivered, [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6]])
  await flow(TASK_IDS.cancelled, [[0, 7]])
  console.log(`  任务：待配送=${TASK_IDS.pending} 配送中=${TASK_IDS.delivering} 已配送=${TASK_IDS.delivered} 已取消=${TASK_IDS.cancelled}`)
}

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  const page = await api('GET', `/dms/task/page?current=1&size=50&keyword=${MARK}`)
  const rows = data(page)?.records || []
  check('分页接口返回 records/total', Array.isArray(rows) && data(page)?.total !== undefined,
    JSON.stringify({ total: data(page)?.total }))
  check('关键字检索命中 4 个 E2E 任务（不含已取消则 3）', rows.length >= 3, `rows=${rows.length}`)
  const byId = (id) => rows.find(r => String(r.id) === String(id))
  check('列表回填台账列（指定配送日期/司机/车辆/送货员）',
    !!byId(TASK_IDS.pending)?.deliveryDate && !!byId(TASK_IDS.pending)?.riderName
    && !!byId(TASK_IDS.pending)?.vehicleName && !!byId(TASK_IDS.pending)?.deliverymanName,
    JSON.stringify(byId(TASK_IDS.pending) || {}).slice(0, 220))
  check('配送单量/装箱数量落库（明细口径聚合）',
    Number(byId(TASK_IDS.pending)?.orderCount) >= 1 && Number(byId(TASK_IDS.pending)?.boxQuantity) === 6,
    `orderCount=${byId(TASK_IDS.pending)?.orderCount} boxQuantity=${byId(TASK_IDS.pending)?.boxQuantity}`)
  check('发货数量/发货金额为明细合计（2×50+3×20=160）',
    Number(byId(TASK_IDS.pending)?.totalQuantity) === 5 && Number(byId(TASK_IDS.pending)?.goodsAmount) === 160,
    `qty=${byId(TASK_IDS.pending)?.totalQuantity} amt=${byId(TASK_IDS.pending)?.goodsAmount}`)
  check('制单人写入登录用户快照', !!byId(TASK_IDS.pending)?.creatorName, byId(TASK_IDS.pending)?.creatorName)

  // 1) 指定配送日期范围（D3 起 ~ D2 止 命中 2 条：delivering=D2 / delivered=D3）
  const r1 = data(await api('GET', `/dms/task/page?current=1&size=50&deliveryDateStart=${D3}&deliveryDateEnd=${D2}&keyword=${MARK}`))?.records || []
  check(`指定配送日期范围过滤（${D3} ~ ${D2} 命中 2 条）`, r1.length === 2, `rows=${r1.length}`)

  // 2) 三值状态筛选（对接前端「配送状态」多选）
  const rPending = data(await api('GET', `/dms/task/page?current=1&size=50&statusList=0&statusList=1&statusList=2&keyword=${MARK}&showRed=true`))?.records || []
  check('三值筛选-待配送（statusList 0,1,2）只含待配送任务',
    rPending.length === 1 && String(rPending[0].id) === String(TASK_IDS.pending),
    `rows=${rPending.length}`)
  const rDelivering = data(await api('GET', `/dms/task/page?current=1&size=50&statusList=3&statusList=4&keyword=${MARK}&showRed=true`))?.records || []
  check('三值筛选-配送中（statusList 3,4）只含配送中任务',
    rDelivering.length === 1 && String(rDelivering[0].id) === String(TASK_IDS.delivering),
    `rows=${rDelivering.length}`)
  const rDelivered = data(await api('GET', `/dms/task/page?current=1&size=50&statusList=5&statusList=6&keyword=${MARK}&showRed=true`))?.records || []
  check('三值筛选-已配送（statusList 5,6）只含已配送任务',
    rDelivered.length === 1 && String(rDelivered[0].id) === String(TASK_IDS.delivered),
    `rows=${rDelivered.length}`)

  // 3) 资源筛选
  const rRider = data(await api('GET', `/dms/task/page?current=1&size=50&riderId=${RIDER_A}&keyword=${MARK}&showRed=true`))?.records || []
  check('按配送司机筛选命中 4 条', rRider.length === 4, `rows=${rRider.length}`)
  const rVehicle = data(await api('GET', `/dms/task/page?current=1&size=50&vehicleId=${VEHICLE_A}&keyword=${MARK}&showRed=true`))?.records || []
  check('按配送车辆筛选命中 4 条', rVehicle.length === 4, `rows=${rVehicle.length}`)
  const rMan = data(await api('GET', `/dms/task/page?current=1&size=50&deliverymanId=${RIDER_B}&keyword=${MARK}&showRed=true`))?.records || []
  check('按送货员筛选命中 4 条', rMan.length === 4, `rows=${rMan.length}`)
  const rNoMatch = data(await api('GET', `/dms/task/page?current=1&size=50&riderId=99999999&keyword=${MARK}`))?.records || []
  check('司机不存在时返回空（非全表）', rNoMatch.length === 0, `rows=${rNoMatch.length}`)

  // 4) 文本条件
  const rTaskNo = data(await api('GET', `/dms/task/page?current=1&size=50&taskNo=${byId(TASK_IDS.delivered).taskNo}`))?.records || []
  check('按任务编号模糊检索命中 1 条', rTaskNo.length === 1, `rows=${rTaskNo.length}`)
  const rBill = data(await api('GET', `/dms/task/page?current=1&size=50&sourceBillNo=${BILL}-F`))?.records || []
  check('按配送单据编号检索命中 1 条（任务与单据关联）',
    rBill.length === 1 && String(rBill[0].id) === String(TASK_IDS.delivered), `rows=${rBill.length}`)
  const rRemark = data(await api('GET', `/dms/task/page?current=1&size=50&remark=E2E备注${STAMP}&showRed=true`))?.records || []
  check('按备注模糊检索命中 4 条', rRemark.length === 4, `rows=${rRemark.length}`)
  const creator = byId(TASK_IDS.pending).creatorName
  const rCreator = data(await api('GET', `/dms/task/page?current=1&size=50&keyword=${MARK}&showRed=true&creatorName=${creator}`))?.records || []
  check('按制单人筛选命中 4 条', rCreator.length === 4, `rows=${rCreator.length}`)

  // 5) 制单时间范围
  const today = new Date().toISOString().slice(0, 10)
  const rCreate = data(await api('GET', `/dms/task/page?current=1&size=50&createTimeStart=${today}&createTimeEnd=${today}&keyword=${MARK}&showRed=true`))?.records || []
  check('按制单时间范围筛选（今日）命中 4 条', rCreate.length === 4, `rows=${rCreate.length}`)
  const rCreatePast = data(await api('GET', `/dms/task/page?current=1&size=50&createTimeStart=2020-01-01&createTimeEnd=2020-01-02&keyword=${MARK}`))?.records || []
  check('制单时间范围外返回空', rCreatePast.length === 0, `rows=${rCreatePast.length}`)

  // 6) 显示红冲
  const rHideRed = data(await api('GET', `/dms/task/page?current=1&size=50&keyword=${MARK}`))?.records || []
  check('默认隐藏已取消（红冲）任务：3 条', rHideRed.length === 3, `rows=${rHideRed.length}`)
  const rShowRed = data(await api('GET', `/dms/task/page?current=1&size=50&keyword=${MARK}&showRed=true`))?.records || []
  check('勾选显示红冲后：4 条（含已取消）', rShowRed.length === 4, `rows=${rShowRed.length}`)

  // 7) 下拉候选
  const opts = data(await api('GET', '/dms/task/filter-options'))
  check('查询条件下拉返回 司机/车辆/送货员/制单人 四类',
    Array.isArray(opts?.drivers) && Array.isArray(opts?.vehicles)
    && Array.isArray(opts?.deliverymen) && Array.isArray(opts?.creators),
    JSON.stringify(Object.keys(opts || {})))
  check('下拉含本次造出的司机/车辆/送货员',
    (opts?.drivers || []).some(o => String(o.id) === String(RIDER_A))
    && (opts?.vehicles || []).some(o => String(o.id) === String(VEHICLE_A))
    && (opts?.deliverymen || []).some(o => String(o.id) === String(RIDER_B)),
    `drivers=${(opts?.drivers || []).length} vehicles=${(opts?.vehicles || []).length}`)

  // 8) 打印次数回写
  const before = Number((await dbQuery(`SELECT print_count FROM dms_task WHERE id = $1`, [TASK_IDS.pending]))[0]?.print_count || 0)
  await api('POST', `/dms/task/${TASK_IDS.pending}/print`)
  const after = Number((await dbQuery(`SELECT print_count FROM dms_task WHERE id = $1`, [TASK_IDS.pending]))[0]?.print_count || 0)
  check('打印次数真实累加（+1）', after === before + 1, `${before} → ${after}`)

  // 9) 导出（后端真实 xlsx）
  const exp = await rawReq('GET', `/dms/task/export?keyword=${MARK}`, null, TOKEN)
  const isXlsx = exp.buf && exp.buf.length > 4 && exp.buf[0] === 0x50 && exp.buf[1] === 0x4B
  check('导出接口返回真实 xlsx（PK 魔数）', isXlsx,
    `status=${exp.status} type=${exp.headers['content-type']} size=${exp.buf?.length}`)

  // 10) 明细视图（按明细 Tab 复用）
  const detail = data(await api('GET', `/dms/task/page-detail?current=1&size=50&taskNo=${byId(TASK_IDS.pending).taskNo}`))
  check('按明细视图返回商品行（2 行）', (detail?.records || []).length === 2, `rows=${(detail?.records || []).length}`)

  // 11) 详情
  const d = data(await api('GET', `/dms/task/${TASK_IDS.pending}`))
  check('详情返回头 + 2 条商品明细', d?.items?.length === 2 && String(d?.id) === String(TASK_IDS.pending),
    `items=${d?.items?.length}`)

  // 12) 操作列「取消」显隐依据：可取消状态集合（0/1）——不在此处真取消，避免影响后续 UI 断言

  return { tasks: TASK_IDS, rows }
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  // antd 会给「无图标 + 恰好两个汉字」的按钮插空格（查询→「查 询」），统一用正则
  page.clickBtn = (re, scope) => (scope || page).locator('button').filter({ hasText: re }).first().click()

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

  await openPage(`${FE}/dispatch/query`)
  const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())

  // 工具栏（对标：刷新 / 批量打印 / 打印(F8) / 导出）
  for (const b of ['刷新', '批量打印', '打印(F8)', '导出']) {
    check(`工具栏含「${b}」`, bodyText.includes(b.replace(/\s+/g, '')))
  }

  // 9 段快捷时间
  for (const q of ['昨日', '今日', '近两日', '近一周', '近一月', '本周', '上周', '本月', '上月']) {
    check(`快捷时间段含「${q}」`, bodyText.includes(q))
  }

  // 13 项查询条件：input 类看 placeholder 属性，选择器类看 placeholder 文本
  for (const q of ['配送起', '配送止', '任务编号', '备注', '配送单据编号', '制单起', '制单止']) {
    const n = await page.locator(`input[placeholder="${q}"]`).count()
    check(`查询区含条件「${q}」`, n > 0)
  }
  for (const q of ['配送司机', '配送车辆', '送货员', '制单人', '配送状态', '显示红冲']) {
    check(`查询区含条件「${q}」`, bodyText.includes(q))
  }
  check('配送状态为多选控件（三值口径）', (await page.locator('.ant-select-multiple').count()) > 0)
  // 查询区版式：横向自适应网格（对齐系统其它单据页），不得回归为 240px 纵向单列
  const layout = await page.evaluate(() => {
    const g = document.querySelector('.search-grid')
    if (!g) return null
    const cs = getComputedStyle(g)
    return {
      display: cs.display,
      cols: cs.gridTemplateColumns.split(' ').filter(Boolean).length,
      height: g.clientHeight,
      hasOldColumn: !!document.querySelector('.query-column'),
    }
  })
  check('查询区为横向多列网格（列数 ≥ 5）',
    !!layout && layout.display === 'grid' && layout.cols >= 5 && !layout.hasOldColumn,
    JSON.stringify(layout))
  check('查询区高度收敛为单行（< 120px，不再占半屏）',
    !!layout && layout.height < 120, layout && `${layout.height}px`)
  check('本页无「页面配置」齿轮（对标无页面配置弹窗）', !bodyText.includes('页面配置'))

  // 表头（默认 20 列 + 操作 = 21 列）
  const headers = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').replace(/\s+/g, '')).filter(Boolean))
  const defaultCols = ['操作', '指定配送日期', '任务编号', '配送状态', '配送开始时间', '配送结束时间',
    '司机名称', '配送车辆', '送货员', '配送单量', '订金金额', '退货单量', '发货数量', '发货金额',
    '退货数量', '退货金额', '装箱数量', '配送里程(km)', '备注', '制单人', '制单时间']
  for (const h of defaultCols) {
    check(`表头含默认列「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  check('默认隐藏 4 列（司机编号/体积/重量/打印次数）',
    !headers.includes('司机编号') && !headers.some(h => h.includes('体积'))
    && !headers.some(h => h.includes('重量')) && !headers.includes('打印次数'), headers.join('/'))

  // 表头齿轮列配置
  const gear = page.locator('.ss-grid .th-settings-btn').first()
  check('数据表存在表头齿轮（列配置入口）', (await gear.count()) > 0)
  if (await gear.count()) {
    await gear.click()
    await page.waitForTimeout(1500)
    const colPanel = page.locator('.ant-modal-content:visible').last()
    const colPanelText = (await colPanel.innerText()).replace(/\s+/g, '')
    check('列配置弹窗含「个人配置 / 全局配置」双 Tab',
      colPanelText.includes('个人配置') && colPanelText.includes('全局配置'), colPanelText.slice(0, 120))
    for (const c of ['指定配送日期', '任务编号', '配送状态', '司机编号', '司机名称', '配送车辆', '送货员',
      '配送单量', '订金金额', '退货单量', '发货数量', '发货金额', '退货数量', '退货金额', '装箱数量',
      '配送里程', '体积', '重量', '备注', '打印次数', '制单人', '制单时间']) {
      check(`列配置含列「${c}」`, colPanelText.includes(c.replace(/\s+/g, '')))
    }
    await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  }

  // 数据与过滤基准：以「配送单据编号」公共前缀（ASCII）锁定本次造数，避免并行会话数据干扰
  const totalText = async () => {
    const t = await page.locator('.classic-pagination .total-box').first().innerText().catch(() => '')
    const m = t.match(/(\d+)/)
    return m ? Number(m[1]) : -1
  }
  const billInput = page.locator('input[placeholder="配送单据编号"]').first()
  const taskNoInput = page.locator('input[placeholder="任务编号"]').first()
  await billInput.fill(BILL + '-')
  await page.clickBtn(/查\s*询/)
  await page.waitForTimeout(2800)
  const totalNoRed = await totalText()
  check('按配送单据编号过滤命中本次造数 3 条（已取消默认隐藏）', totalNoRed === 3, `total=${totalNoRed}`)

  const gridText = (await page.locator('.ss-grid tbody').innerText().catch(() => '')).replace(/\s+/g, '')
  check('配送状态列显示三值口径（待配送/配送中/已配送）',
    gridText.includes('待配送') && gridText.includes('配送中') && gridText.includes('已配送'), gridText.slice(0, 160))
  check('操作列含「单据明细」（对标操作列）', gridText.includes('单据明细'), gridText.slice(0, 80))
  check('操作列含「打印」（打印次数 0 → 打印 / 已打印 → 补打）', gridText.includes('打印'), gridText.slice(0, 80))

  // 合计行
  check('底部合计行存在', bodyText.includes('合计'), bodyText.slice(-120))

  // 经典分页栏（对标 ql361：首页/上页/第(x/y)页/下页/尾页/跳转到 N 页/共 N 条记录/每页显示 N 行）
  const pagerText = await page.evaluate(() => {
    const el = document.querySelector('.classic-pagination')
    return el ? el.innerText.replace(/\s+/g, '') : ''
  })
  for (const seg of ['首页', '上页', '页', '下页', '尾页', '跳转到', '跳转', '共', '条记录', '每页显示', '行']) {
    check(`经典分页栏含「${seg}」`, pagerText.includes(seg), pagerText.slice(0, 100))
  }

  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // ── 条件真实生效：叠加任务编号（ASCII）→ 仅 1 条 ──
  const taskNo = (await dbQuery(`SELECT task_no FROM dms_task WHERE id = $1`, [TASK_IDS.delivered]))[0].task_no
  await taskNoInput.fill(taskNo)
  await page.clickBtn(/查\s*询/)
  await page.waitForTimeout(2800)
  const totalFiltered = await totalText()
  check('叠加任务编号后仅 1 条（条件真实生效）', totalFiltered === 1, `total=${totalFiltered}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-filtered.png'), fullPage: true })

  // ── 显示红冲：勾选后已取消任务可见（3 → 4） ──
  await taskNoInput.click()
  await taskNoInput.press('Control+a')
  await taskNoInput.press('Delete')
  await page.waitForTimeout(500)
  const redBox = page.locator('.ant-checkbox-wrapper:has-text("显示红冲")').first()
  if (await redBox.count()) {
    await redBox.click()
    await page.waitForTimeout(800)
    await page.clickBtn(/查\s*询/)
    await page.waitForTimeout(2800)
    const totalRed = await totalText()
    check('勾选「显示红冲」后含已取消任务（3 → 4）', totalRed === 4, `total=${totalRed}`)
    await page.screenshot({ path: path.join(SHOTS, 'ui-show-red.png'), fullPage: true })
  } else {
    check('勾选「显示红冲」后含已取消任务（3 → 4）', false, '未找到显示红冲勾选框')
  }

  // ── 批量打印：未勾选提示 / 勾选后弹「打印模板选择」 ──
  const batchBtn = page.locator('button').filter({ hasText: /批量打印/ }).first()
  await batchBtn.click()
  await page.waitForTimeout(1200)
  const warnText = (await page.locator('.ant-message').innerText().catch(() => '')).replace(/\s+/g, '')
  check('批量打印未勾选时提示「请选中至少一条数据！」', warnText.includes('请选中至少一条数据'), warnText.slice(0, 60))

  // 勾选第一行（自绘 checkbox：优先 .ss-checkbox，回退原生 checkbox）
  const firstCheckbox = page.locator('.ss-grid tbody .ss-checkbox, .ss-grid tbody input[type=checkbox]').first()
  if (await firstCheckbox.count()) {
    await firstCheckbox.click({ force: true })
    await page.waitForTimeout(1200)
    const picked = (await page.locator('body').innerText()).replace(/\s+/g, '').includes('已选择')
    check('勾选行后出现批量操作提示（已选择 N 项）', picked, '')
    await batchBtn.click()
    await page.waitForTimeout(2200)
    const bpModal = page.locator('.ant-modal-content:visible').last()
    const hasModal = (await bpModal.count()) > 0
    const bpText = hasModal ? (await bpModal.innerText().catch(() => '')).replace(/\s+/g, '') : ''
    check('勾选后弹出「打印模板选择」（含「仅打印未打印过的单据」）',
      bpText.includes('打印模板选择') && bpText.includes('仅打印未打印过的单据'), bpText.slice(0, 120))
    await page.screenshot({ path: path.join(SHOTS, 'ui-batch-print.png'), fullPage: true })
    if (hasModal) {
      await page.locator('.ant-modal-content:visible .ant-modal-close').last().click({ timeout: 5000 }).catch(() => {})
      await page.waitForTimeout(800)
    }
  } else {
    check('勾选行后出现批量操作提示（已选择 N 项）', false, '未找到行勾选框')
    check('勾选后弹出「打印模板选择」（含「仅打印未打印过的单据」）', false, '未找到行勾选框')
  }

  // ── 打印(F8)：打开打印弹窗（需先勾选一条） ──
  await page.clickBtn(/打\s*印\(F8\)/)
  await page.waitForTimeout(2500)
  const printModal = page.locator('.ant-modal-content:visible').last()
  const hasPrintModal = (await printModal.count()) > 0
  const printText = hasPrintModal ? (await printModal.innerText().catch(() => '')).replace(/\s+/g, '') : ''
  check('打印(F8) 打开打印弹窗（模板渲染/选择）', hasPrintModal && printText.length > 0, printText.slice(0, 120))
  await page.screenshot({ path: path.join(SHOTS, 'ui-print.png'), fullPage: true })

  await browser.close()
}

// ══════════════ 汇总 ══════════════
async function main() {
  console.log('配送查询金标准 E2E —— 后端 :' + PORT + '，前端 ' + FE)
  await ensureE2EUser()
  await login()
  await prepareData()
  await apiSuite()
  await uiSuite()

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 汇总：${pass}/${results.length} 通过${fail ? `，${fail} 项失败` : ''} ═══`)
  if (fail) {
    console.log('失败项：')
    results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  }
  fs.writeFileSync(path.join(SHOTS, 'e2e-result.json'), JSON.stringify(results, null, 2))
  process.exitCode = fail ? 1 : 0
}

main().catch(e => { console.error('E2E 异常终止:', e); process.exitCode = 1 })
