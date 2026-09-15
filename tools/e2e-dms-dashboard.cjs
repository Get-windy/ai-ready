/*
 * 配送仪表盘（配送 → 配送业务 → 配送仪表盘）金标准端到端验证
 *   · API 验收：KPI 聚合口径 / 状态分布 / 趋势补零 / 配送员绩效 / 渠道与订单类型分布 /
 *               人车绑定与待处理预警分页 / 渠道与订单类型筛选生效 / 自定义区间 / 参数校验 / 零数据边界
 *   · UI 验收：14 张 KPI 卡（无占位符）/ 三张图表 / 待办两张表 / 配送员 Top /
 *              时间范围切换 / 渠道筛选 / 卡片下钻 / 页面配置开关真实生效 / 真实 xlsx 导出 / 自动刷新
 *
 * 断言策略：共享 devdb 会被并行会话写入，故 KPI 期望值统一「按同口径在库上算一遍再与接口比对」
 *          （dbTaskAgg / dbStatusCounts / dbCapacity / dbTodoCounts），而非硬编码总数。
 *
 * 前置：
 *   1) 造数据：python tools/dbq2.py "$(grep -v '^--' tools/e2e-dms-dashboard-seed.sql)"
 *   2) 账号：  python tools/dbq2.py "$(grep -v '^--' tools/e2e-dms-dashboard-user.sql)"
 *   3) 后端实例需包含 dms-delivery 的 dashboard 包（见《配送仪表盘开发文档》§6）
 *
 * 用法：node tools/e2e-dms-dashboard.cjs
 *      ERP_PORT=5677 FE_URL=http://localhost:5656 node tools/e2e-dms-dashboard.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5677)
const SHOTS = 'I:/AI-Ready/tool-results/dms-dashboard'

/** 专用验收账号（tools/e2e-dms-dashboard-user.sql 创建），避免与并行会话共用 admin 互相踢下线 */
const E2E_USER = process.env.E2E_USER || 'e2e_dmsdash'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const TENANT_NAME = process.env.E2E_TENANT || '系统租户'

/** E2E 样本常量（与 tools/e2e-dms-dashboard-seed.sql 一一对应） */
const CHANNEL_DADA = '2099210000000000001'
const RIDER_ONLINE_IDS = '2099230000000000001, 2099230000000000004'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

// ── HTTP 小工具（走 node http，避免引入 axios 依赖） ─────────────
function rawReq(method, reqPath, body, token, extraHeaders) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? null : JSON.stringify(body)
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {}),
    }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* 非 JSON 响应 */ }
        resolve({ status: res.statusCode, buf, json, text: buf.toString('utf8') })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

let TOKEN = null

async function api(method, reqPath, body, extraHeaders) {
  let r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  const code = r.json && Number(r.json.code)
  if (r.status === 401 || code === 401) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  }
  return r
}

/** 取接口 data（后端 ApiResponse 包一层） */
function data(res) {
  return res.json ? res.json.data : null
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: TENANT_NAME,
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  let token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token && E2E_USER !== 'admin') {
    // 专用账号不可用时回退 admin（并发环境会被互踢，仅作兜底）
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.json.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', {
      username: 'admin', password: 'admin123', tenantName: TENANT_NAME,
      captcha: code2, captchaKey: cap2.json.data.uuid,
    })
    token = res.json?.data?.token || res.json?.data?.accessToken
  }
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  TOKEN = token
}

// ── 验收结果收集 ────────────────────────────────────────────────
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

// ── 数据库直连：刷新在线样本 + 按同口径计算期望值 ────────────────
let DB = null

async function ensureDb() {
  if (DB) return DB
  DB = new Client({
    host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123',
  })
  await DB.connect()
  return DB
}

async function dbQuery(sql) {
  const client = await ensureDb()
  return (await client.query(sql)).rows
}

/**
 * 刷新「在线配送员」样本的上报时间。
 * seed 里的 last_report_time 是插入时刻，验收距插入超过 2 分钟阈值后会自然离线，
 * 因此每次验收前把两名在线骑手重置为「1 分钟前上报」。
 */
async function refreshOnlineRiders() {
  await dbQuery(`UPDATE dms_rider SET last_report_time = now() - interval '1 minute'
                 WHERE id IN (${RIDER_ONLINE_IDS})`)
  // 并行会话可能改动/归还了样本绑定，验收前把专属两条样本恢复为「绑定中」
  await dbQuery(`UPDATE dms_rider_vehicle_binding SET status = 0, handover_time = NULL
                 WHERE deleted = 0 AND id IN (2099250000000000001, 2099250000000000002)`)
}

/** 区间常量（与后端 RangeWindow 同口径：左闭右开） */
const TODAY_START = "date_trunc('day', now())"
const TODAY_END = "date_trunc('day', now()) + interval '1 day'"
const LAST7_START = "date_trunc('day', now()) - interval '6 days'"

/**
 * 按固化口径在库上算一遍 KPI 期望值（与 DashboardMapper 同一口径）。
 * startExpr / endExpr / extra 均为脚本内常量，不接受外部输入。
 */
async function dbTaskAgg(startExpr, endExpr, extra = '') {
  const rows = await dbQuery(`
    SELECT
      count(*)::int AS "orderCount",
      count(*) FILTER (WHERE status = 0)::int AS "pendingOrders",
      count(*) FILTER (WHERE status IN (1, 2))::int AS "assignedOrders",
      count(*) FILTER (WHERE status IN (3, 4))::int AS "inTransitOrders",
      count(*) FILTER (WHERE status IN (5, 6))::int AS "completedOrders",
      count(*) FILTER (WHERE status = 7)::int AS "cancelledOrders",
      count(*) FILTER (WHERE status = 8)::int AS "exceptionOrders",
      count(*) FILTER (WHERE status IN (0,1,2,3,4) AND deadline_time IS NOT NULL
                         AND deadline_time < now())::int AS "overdueOrders",
      COALESCE(sum(delivery_fee), 0)::float AS "deliveryFee",
      COALESCE(sum(collect_on_delivery), 0)::float AS "collectOnDelivery",
      COALESCE(sum(goods_amount), 0)::float AS "goodsAmount"
    FROM dms_task
    WHERE deleted = 0 AND tenant_id = 1
      AND create_time >= ${startExpr} AND create_time < ${endExpr}
      ${extra}
  `)
  return rows[0]
}

/** 运力口径期望（与 DashboardMapper.aggregateRider / aggregateVehicle 同口径） */
async function dbCapacity() {
  const [row] = await dbQuery(`
    SELECT
      (SELECT count(*) FROM dms_rider WHERE deleted = 0 AND tenant_id = 1)::int AS "totalRiders",
      (SELECT count(*) FROM dms_rider WHERE deleted = 0 AND tenant_id = 1 AND status IN (1,2))::int AS "activeRiders",
      (SELECT count(*) FROM dms_rider WHERE deleted = 0 AND tenant_id = 1
         AND last_report_time IS NOT NULL AND last_report_time >= now() - interval '2 minutes')::int AS "onlineRiders",
      (SELECT count(*) FROM dms_vehicle WHERE deleted = 0 AND tenant_id = 1)::int AS "totalVehicles",
      (SELECT count(*) FROM dms_vehicle WHERE deleted = 0 AND tenant_id = 1 AND status IN (1,4))::int AS "activeVehicles"
  `)
  return row
}

/** 任务状态分布期望（状态 → 数量） */
async function dbStatusCounts(startExpr, endExpr, extra = '') {
  const rows = await dbQuery(`
    SELECT status::int AS status, count(*)::int AS count
    FROM dms_task
    WHERE deleted = 0 AND tenant_id = 1
      AND create_time >= ${startExpr} AND create_time < ${endExpr}
      ${extra}
    GROUP BY status ORDER BY status
  `)
  const map = {}
  rows.forEach(r => { map[r.status] = r.count })
  return map
}

/** 待办计数期望（实时快照，与时间范围/渠道无关） */
async function dbTodoCounts() {
  const [row] = await dbQuery(`
    SELECT
      (SELECT count(*) FROM dms_rider_vehicle_binding
        WHERE deleted = 0 AND tenant_id = 1 AND status = 0)::int AS "activeBindingCount",
      (SELECT count(*) FROM dms_verification_alert
        WHERE deleted = 0 AND tenant_id = 1 AND handle_status = 0)::int AS "pendingAlertCount"
  `)
  return row
}

// ══════════════════════════════════════════════════════════════
// 一、接口验收
// ══════════════════════════════════════════════════════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  await refreshOnlineRiders()
  check('测试数据在线样本已刷新（2 名骑手 1 分钟前上报）', true)

  // 0) 未登录拦截
  const anon = await rawReq('GET', '/dms/dashboard/stats')
  check('未登录访问受保护（401）', anon.status === 401, `status=${anon.status}`)

  await login()
  check('验收账号登录成功', !!TOKEN)

  // ── 1) KPI 聚合：接口值 === 库内同口径值 ──
  // 共享库中可能并存其它会话写入的任务/绑定/预警，故不做硬编码总数断言，
  // 统一「接口值 === 库口径值」：既抗并发污染，又能暴露聚合实现与口径的偏差。
  const expToday = await dbTaskAgg(TODAY_START, TODAY_END)
  const todoCounts = await dbTodoCounts()
  const capacity = await dbCapacity()
  const statsRes = await api('GET', '/dms/dashboard/stats?range=today')
  const s = data(statsRes)
  check('stats 接口 200', statsRes.status === 200 && !!s, `status=${statsRes.status}`)
  for (const [key, expect] of Object.entries({ ...expToday, ...todoCounts, ...capacity })) {
    check(`今日 ${key} 与库口径一致（${expect}）`, Number(s?.[key]) === Number(expect), `接口 ${s?.[key]}`)
  }
  check('stats 回显统计口径', s?.range === 'today' && s?.rangeLabel === '今日', `${s?.range}/${s?.rangeLabel}`)
  check('stats 回显区间（左闭右开转闭区间）',
    String(s?.startTime).includes('00:00:00') && String(s?.endTime).includes('23:59:59'),
    `${s?.startTime} ~ ${s?.endTime}`)
  check('在线阈值为 2 分钟', Number(s?.onlineThresholdMinutes) === 2, s?.onlineThresholdMinutes)
  check('刷新间隔默认 30 秒（配送参数缺省值）', Number(s?.refreshSeconds) === 30, s?.refreshSeconds)
  check('在线配送员 ≥ 2（E2E 在线样本）', Number(s?.onlineRiders) >= 2, s?.onlineRiders)
  check('KPI 字段齐全（无 null 计数）',
    ['orderCount', 'pendingOrders', 'inTransitOrders', 'completedOrders', 'exceptionOrders', 'overdueOrders']
      .every(k => s?.[k] !== null && s?.[k] !== undefined),
    JSON.stringify(s))

  // ── 2) 近 7 日 ──
  const exp7 = await dbTaskAgg(LAST7_START, TODAY_END)
  const s7 = data(await api('GET', '/dms/dashboard/stats?range=last7'))
  for (const [key, expect] of Object.entries(exp7)) {
    check(`近 7 日 ${key} 与库口径一致（${expect}）`, Number(s7?.[key]) === Number(expect), `接口 ${s7?.[key]}`)
  }
  check('区间放大单量不减（近 7 日 ≥ 今日）', Number(s7?.orderCount) >= Number(s?.orderCount),
    `${s7?.orderCount} vs ${s?.orderCount}`)

  // ── 3) 任务状态分布：9 态全量 + 与库一致 ──
  const expStatus = await dbStatusCounts(TODAY_START, TODAY_END)
  const summary = data(await api('GET', '/dms/dashboard/task-summary?range=today'))
  check('task-summary 返回 9 个状态（区间无数据补 0）',
    Array.isArray(summary) && summary.length === 9, `len=${summary?.length}`)
  check('task-summary 状态码覆盖 0..8',
    JSON.stringify((summary || []).map(i => i.status)) === JSON.stringify([0, 1, 2, 3, 4, 5, 6, 7, 8]),
    JSON.stringify((summary || []).map(i => i.status)))
  check('task-summary 各状态计数与库一致',
    (summary || []).every(i => Number(i.count) === Number(expStatus[i.status] || 0)),
    JSON.stringify((summary || []).map(i => [i.status, i.count, expStatus[i.status] || 0])))
  check('task-summary 占比合计 100%（±0.5）',
    Number(s?.orderCount) > 0 &&
    Math.abs((summary || []).reduce((a, i) => a + Number(i.ratio), 0) - 100) <= 0.5,
    `sum=${(summary || []).reduce((a, i) => a + Number(i.ratio), 0)}`)
  check('task-summary 状态名来自后端枚举',
    summary?.[0]?.statusName === '待分配' && summary?.[8]?.statusName === '异常',
    `${summary?.[0]?.statusName}/${summary?.[8]?.statusName}`)

  // ── 4) 趋势 ──
  const trend = data(await api('GET', '/dms/dashboard/trend?range=today'))
  check('trend 默认 7 个点（今日 → 近 7 日）', Array.isArray(trend) && trend.length === 7, `len=${trend?.length}`)
  const todayStr = new Date().toISOString().slice(0, 10)
  check('trend 末点为今天', trend?.[trend.length - 1]?.statDate === todayStr, trend?.[trend.length - 1]?.statDate)
  check('trend 今日单量与 KPI 一致',
    Number(trend?.[trend.length - 1]?.orderCount) === Number(s?.orderCount),
    `${trend?.[trend.length - 1]?.orderCount} vs ${s?.orderCount}`)
  check('trend 含准时率与平均时长字段',
    trend?.[trend.length - 1]?.onTimeRate !== undefined && trend?.[trend.length - 1]?.avgMinutes !== undefined,
    JSON.stringify(trend?.[trend.length - 1]))
  const trend30 = data(await api('GET', '/dms/dashboard/trend?range=last30&days=30'))
  check('trend days=30 返回 30 个点', Array.isArray(trend30) && trend30.length === 30, `len=${trend30?.length}`)
  // 回归：跨月区间曾因 Period.getDays() 只返回「天数部分」而得到 0 天，导致只出 1 个点
  const trendCustom = data(await api('GET', '/dms/dashboard/trend?range=custom&startDate=2026-09-01&endDate=2026-09-30'))
  check('trend 自定义区间返回 30 个点（跨月天数正确）',
    Array.isArray(trendCustom) && trendCustom.length === 30, `len=${trendCustom?.length}`)
  check('trend 自定义区间首末日期与统计区间一致',
    trendCustom?.[0]?.statDate === '2026-09-01' && trendCustom?.[29]?.statDate === '2026-09-30',
    `${trendCustom?.[0]?.statDate} ~ ${trendCustom?.[29]?.statDate}`)

  // ── 5) 配送员绩效 Top ──
  const riders = data(await api('GET', '/dms/dashboard/top-riders?range=today&limit=50'))
  const e2eNames = ['E2E张小明', 'E2E李快跑', 'E2E王众包']
  check('top-riders 含 E2E 有单样本 3 名',
    e2eNames.every(n => (riders || []).some(r => r.riderName === n)),
    JSON.stringify((riders || []).map(r => r.riderName)))
  check('top-riders 不含 0 单配送员（绩效榜语义）',
    (riders || []).every(r => Number(r.orderCount) > 0),
    JSON.stringify((riders || []).map(r => `${r.riderName}:${r.orderCount}`)))
  check('top-riders 按接单量倒序',
    (riders || []).every((r, i, arr) => i === 0 || Number(arr[i - 1].orderCount) >= Number(r.orderCount)),
    JSON.stringify((riders || []).map(r => `${r.riderName}:${r.orderCount}`)))
  const zhang = (riders || []).find(r => r.riderName === 'E2E张小明')
  check('E2E张小明 今日 3 单 / 准时率 100% / 平均 30 分钟',
    Number(zhang?.orderCount) === 3 && Number(zhang?.onTimeRate) === 100 && zhang?.avgMinutes === 30,
    JSON.stringify(zhang))
  const wang = (riders || []).find(r => r.riderName === 'E2E王众包')
  check('有单但无完成样本时准时率为 null（不显示假 0）',
    Number(wang?.orderCount) === 1 && wang?.onTimeRate === null, JSON.stringify(wang))

  // ── 6) 分布：渠道 / 订单类型 ──
  const byChannel = data(await api('GET', '/dms/dashboard/distribution?by=channel&range=today'))
  const expChannel = await dbQuery(`
    SELECT COALESCE(channel_id, -1)::text AS key, count(*)::int AS cnt
    FROM dms_task WHERE deleted = 0 AND tenant_id = 1
      AND create_time >= ${TODAY_START} AND create_time < ${TODAY_END}
    GROUP BY 1`)
  check('渠道分布与库一致（项数与计数）',
    (byChannel || []).length === expChannel.length &&
    (byChannel || []).every(i => {
      const e = expChannel.find(x => x.key === i.itemKey)
      return e && Number(i.orderCount) === Number(e.cnt)
    }),
    JSON.stringify((byChannel || []).map(i => [i.itemKey, i.orderCount])))
  const dada = (byChannel || []).find(i => i.itemKey === CHANNEL_DADA)
  check('E2E达达渠道 ≥ 5 单（E2E 样本）', Number(dada?.orderCount) >= 5, dada?.orderCount)
  check('未被指派渠道的任务归入「未分配渠道」',
    (byChannel || []).some(i => i.itemKey === '-1' && i.itemName === '未分配渠道'))

  const byType = data(await api('GET', '/dms/dashboard/distribution?by=orderType&range=today'))
  // 期望值同口径：接口把未填 order_type 归入 key=-1（否则 null 键会撞不可变 Map 的 hash → 500）
  const expType = await dbQuery(`
    SELECT COALESCE(order_type, -1)::text AS key, count(*)::int AS cnt
    FROM dms_task WHERE deleted = 0 AND tenant_id = 1
      AND create_time >= ${TODAY_START} AND create_time < ${TODAY_END}
    GROUP BY 1`)
  check('订单类型分布与库一致（中文名由后端字典映射）',
    (byType || []).length === expType.length &&
    (byType || []).every(i => {
      const e = expType.find(x => String(x.key) === i.itemKey)
      return e && Number(i.orderCount) === Number(e.cnt) && i.itemName && !/^\d+$/.test(i.itemName)
    }),
    JSON.stringify(byType))
  const salesType = (byType || []).find(i => i.itemName === '销售配送')
  check('销售配送单量 ≥ 7（E2E 样本贡献）', Number(salesType?.orderCount) >= 7, salesType?.orderCount)
  // 回归守卫：order_type 为 NULL 的历史/导入任务必须归入「未指定」，且整页不得 500
  const unspec = (byType || []).find(i => i.itemKey === '-1')
  const unspecDb = Number(expType.find(x => x.key === '-1')?.cnt || 0)
  check('未填订单类型归入「未指定」（null 键不崩）',
    unspecDb === 0 ? !unspec : (unspec && unspec.itemName === '未指定' && Number(unspec.orderCount) === unspecDb),
    unspec ? `${unspec.itemName} · ${unspec.orderCount} vs ${unspecDb}` : `库中 ${unspecDb} 单无 order_type`)

  // ── 7) 待办：活跃绑定 / 待处理预警 ──
  const bindings = data(await api('GET', '/dms/dashboard/active-bindings?page=1&size=5'))
  check('active-bindings 总数与库一致', Number(bindings?.total) === Number(todoCounts.activeBindingCount),
    `${bindings?.total} vs ${todoCounts.activeBindingCount}`)
  check('active-bindings 全部为绑定中（status=0）',
    (bindings?.records || []).every(r => Number(r.status) === 0),
    JSON.stringify((bindings?.records || []).map(r => r.status)))
  const bindTimes = (bindings?.records || []).map(r => new Date(r.bindTime).getTime())
  check('active-bindings 按绑定时间倒序', bindTimes.every((t, i) => i === 0 || bindTimes[i - 1] >= t),
    JSON.stringify(bindTimes))

  const alerts = data(await api('GET', '/dms/dashboard/pending-alerts?page=1&size=5'))
  check('pending-alerts 默认仅未处理且与库一致', Number(alerts?.total) === Number(todoCounts.pendingAlertCount),
    `${alerts?.total} vs ${todoCounts.pendingAlertCount}`)
  check('pending-alerts 全部 handleStatus=0',
    (alerts?.records || []).every(r => Number(r.handleStatus) === 0),
    JSON.stringify((alerts?.records || []).map(r => r.handleStatus)))
  const alertLevels = (alerts?.records || []).map(r => Number(r.alertLevel))
  check('pending-alerts 按级别倒序（严重在前）', alertLevels.every((l, i) => i === 0 || alertLevels[i - 1] >= l),
    JSON.stringify(alertLevels))
  const [doneAlert] = await dbQuery(`SELECT count(*)::int AS c FROM dms_verification_alert
    WHERE deleted = 0 AND tenant_id = 1 AND handle_status = 1`)
  const allAlerts = data(await api('GET', '/dms/dashboard/pending-alerts?page=1&size=10&handleStatus=1'))
  check('pending-alerts 可按已处理过滤', Number(allAlerts?.total) === Number(doneAlert.c),
    `${allAlerts?.total} vs ${doneAlert.c}`)

  // ── 8) 筛选生效（对照库口径） ──
  const expDada = await dbTaskAgg(TODAY_START, TODAY_END, `AND channel_id = ${CHANNEL_DADA}`)
  const sc = data(await api('GET', `/dms/dashboard/stats?range=today&channelId=${CHANNEL_DADA}`))
  check('渠道筛选与库口径一致', Number(sc?.orderCount) === Number(expDada.orderCount),
    `${sc?.orderCount} vs ${expDada.orderCount}`)
  check('渠道筛选「E2E达达」≥ 5 单（E2E 样本）', Number(sc?.orderCount) >= 5, sc?.orderCount)
  const expDadaType1 = await dbTaskAgg(TODAY_START, TODAY_END, `AND channel_id = ${CHANNEL_DADA} AND order_type = 1`)
  const sc2 = data(await api('GET', `/dms/dashboard/stats?range=today&channelId=${CHANNEL_DADA}&orderType=1`))
  check('渠道 + 订单类型筛选与库口径一致', Number(sc2?.orderCount) === Number(expDadaType1.orderCount),
    `${sc2?.orderCount} vs ${expDadaType1.orderCount}`)
  const expType2 = await dbTaskAgg(TODAY_START, TODAY_END, 'AND order_type = 2')
  const sc3 = data(await api('GET', '/dms/dashboard/stats?range=today&orderType=2'))
  check('订单类型筛选与库口径一致', Number(sc3?.orderCount) === Number(expType2.orderCount),
    `${sc3?.orderCount} vs ${expType2.orderCount}`)
  check('筛选不改变运力口径（车辆/配送员为全量）',
    Number(sc3?.totalVehicles) === Number(s?.totalVehicles) && Number(sc3?.totalRiders) === Number(s?.totalRiders),
    `${sc3?.totalVehicles}/${sc3?.totalRiders} vs ${s?.totalVehicles}/${s?.totalRiders}`)
  check('待办计数为实时快照，不随时间范围收敛（今日 vs 近 7 日一致）',
    Number(s?.activeBindingCount) === Number(s7?.activeBindingCount) &&
    Number(s?.pendingAlertCount) === Number(s7?.pendingAlertCount),
    `${s?.activeBindingCount}/${s7?.activeBindingCount} · ${s?.pendingAlertCount}/${s7?.pendingAlertCount}`)
  check('待办计数不随渠道筛选收敛',
    Number(sc?.activeBindingCount) === Number(s?.activeBindingCount) &&
    Number(sc?.pendingAlertCount) === Number(s?.pendingAlertCount),
    `${sc?.activeBindingCount}/${sc?.pendingAlertCount}`)
  const expStatusDada = await dbStatusCounts(TODAY_START, TODAY_END, `AND channel_id = ${CHANNEL_DADA}`)
  const summaryFiltered = data(await api('GET', `/dms/dashboard/task-summary?range=today&channelId=${CHANNEL_DADA}`))
  check('状态分布随筛选收敛且与库一致',
    (summaryFiltered || []).every(i => Number(i.count) === Number(expStatusDada[i.status] || 0)),
    JSON.stringify((summaryFiltered || []).map(i => i.count)))

  // ── 9) 自定义区间 ──
  const expCustom = await dbTaskAgg("'2026-09-01'::timestamp", "'2026-10-01'::timestamp")
  const custom = data(await api('GET', '/dms/dashboard/stats?range=custom&startDate=2026-09-01&endDate=2026-09-30'))
  check('自定义区间与库口径一致', Number(custom?.orderCount) === Number(expCustom.orderCount),
    `${custom?.orderCount} vs ${expCustom.orderCount}`)
  check('自定义区间回显范围文案',
    custom?.range === 'custom' && String(custom?.rangeLabel).includes('2026-09-01'), custom?.rangeLabel)

  // ── 9.5) 零数据区间：数值型 0 而非 null/占位 ──
  const empty = data(await api('GET', '/dms/dashboard/stats?range=custom&startDate=2030-01-01&endDate=2030-01-31'))
  check('零数据区间：单量与金额为 0（非 null）',
    Number(empty?.orderCount) === 0 && Number(empty?.deliveryFee) === 0 && Number(empty?.goodsAmount) === 0,
    JSON.stringify({ o: empty?.orderCount, f: empty?.deliveryFee, g: empty?.goodsAmount }))
  check('零数据区间：准时率/平均时长为 null（前端显示「-」而非假 0）',
    empty?.onTimeRate === null && empty?.avgDeliveryMinutes === null,
    JSON.stringify({ r: empty?.onTimeRate, a: empty?.avgDeliveryMinutes }))
  const emptyTrend = data(await api('GET', '/dms/dashboard/trend?range=custom&startDate=2030-01-01&endDate=2030-01-31'))
  check('零数据区间：趋势补零为 31 个点',
    Array.isArray(emptyTrend) && emptyTrend.length === 31 && emptyTrend.every(p => Number(p.orderCount) === 0),
    `len=${emptyTrend?.length}`)
  const emptyRiders = data(await api('GET', '/dms/dashboard/top-riders?range=custom&startDate=2030-01-01&endDate=2030-01-31'))
  check('零数据区间：配送员绩效榜为空', Array.isArray(emptyRiders) && emptyRiders.length === 0, `len=${emptyRiders?.length}`)
  const emptyStatus = data(await api('GET', '/dms/dashboard/task-summary?range=custom&startDate=2030-01-01&endDate=2030-01-31'))
  check('零数据区间：状态分布仍返回 9 项且全为 0',
    (emptyStatus || []).length === 9 && (emptyStatus || []).every(i => Number(i.count) === 0),
    JSON.stringify((emptyStatus || []).map(i => i.count)))

  // ── 10) 参数校验 ──
  const bad = await api('GET', '/dms/dashboard/stats?range=custom')
  check('自定义区间缺少开始日期被拒绝', Number(bad.json?.code) !== 200, `code=${bad.json?.code} msg=${bad.json?.message}`)
  const bad2 = await api('GET', '/dms/dashboard/stats?range=custom&startDate=2026-09-30&endDate=2026-09-01')
  check('结束日期早于开始日期被拒绝', Number(bad2.json?.code) !== 200, `code=${bad2.json?.code}`)

  // ── 11) 昨日区间 ──
  const expY = await dbTaskAgg("date_trunc('day', now()) - interval '1 day'", TODAY_START)
  const yesterday = data(await api('GET', '/dms/dashboard/stats?range=yesterday'))
  check('昨日区间与库口径一致', Number(yesterday?.orderCount) === Number(expY.orderCount),
    `${yesterday?.orderCount} vs ${expY.orderCount}`)
  check('昨日区间数值非 null（无数据日为 0）',
    yesterday?.orderCount !== null && yesterday?.orderCount !== undefined, yesterday?.orderCount)
}

// ══════════════════════════════════════════════════════════════
// 二、UI 验收
// ══════════════════════════════════════════════════════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  // UI 验收期间重新取一次期望值（与接口验收相隔较久，期间可能有并行写入）
  const expToday = await dbTaskAgg(TODAY_START, TODAY_END)
  const todoCounts = await dbTodoCounts()
  const exp7 = await dbTaskAgg(LAST7_START, TODAY_END)

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  // /api 转发到验收实例（前端 dev server 的代理默认指向 5655）
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  /** 注入 token 打开页面（并行会话共用账号会被互踢，失败则重登） */
  async function openPage(url, waitMs = 4000) {
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

  /** 等 KPI 卡渲染完成（首次加载时组件先显示骨架屏） */
  async function waitCards() {
    await page.locator('.ar-stat-card').first().waitFor({ state: 'visible', timeout: 20000 })
    await page.waitForTimeout(600)
  }

  /** 读取 KPI 卡（返回卡片数组 + label→value 映射） */
  async function readCards() {
    const cards = await page.evaluate(() => [...document.querySelectorAll('.ar-stat-card')].map(el => ({
      label: (el.querySelector('.ar-stat-card__label')?.textContent || '').trim(),
      value: (el.querySelector('.ar-stat-card__value')?.textContent || '').trim(),
    })))
    const map = {}
    cards.forEach(c => { map[c.label] = c.value })
    return { cards, map }
  }

  /** 卡片下钻 + 等待路由跳转（静默刷新重渲染可能吞掉首次点击，故带一次重试） */
  async function drillAndWait(cardLabel, urlPattern, attempts = 2) {
    for (let i = 0; i < attempts; i++) {
      await waitCards()
      await page.locator('.ar-stat-card', { hasText: cardLabel }).click()
      try {
        await page.waitForURL(urlPattern, { timeout: 6000 })
        break
      } catch (e) {
        console.log(`  [下钻未跳转，重试 ${i + 1}/${attempts}] ${cardLabel} → 当前 ${page.url()}`)
      }
    }
    await page.waitForTimeout(1500)
  }

  // ── 1. 页面加载与 KPI 卡 ──
  await openPage(`${FE}/dms/dashboard`, 5000)
  await waitCards()
  let txt = await bodyText()
  check('页面可打开（无 404/白屏）', !txt.includes('页面不存在') && txt.length > 100, page.url())

  const { cards, map: cardValue } = await readCards()
  check('KPI 卡共 14 张', cards.length === 14, `count=${cards.length}`)
  const expectLabels = [
    '配送单量', '待分配任务', '在途任务', '已完成', '异常任务', '超时未签收',
    '准时率', '平均配送时长', '配送费', '代收货款', '在线配送员', '活跃车辆', '活跃人车绑定', '待处理预警',
  ]
  check('KPI 卡标题齐全', expectLabels.every(l => cards.some(c => c.label === l)), cards.map(c => c.label).join('/'))

  check('配送单量卡与库口径一致', cardValue['配送单量'] === String(expToday.orderCount),
    `${cardValue['配送单量']} vs ${expToday.orderCount}`)
  check('待分配任务卡与库一致', cardValue['待分配任务'] === String(expToday.pendingOrders),
    `${cardValue['待分配任务']} vs ${expToday.pendingOrders}`)
  check('在途任务卡与库一致', cardValue['在途任务'] === String(expToday.inTransitOrders),
    `${cardValue['在途任务']} vs ${expToday.inTransitOrders}`)
  check('已完成卡与库一致', cardValue['已完成'] === String(expToday.completedOrders),
    `${cardValue['已完成']} vs ${expToday.completedOrders}`)
  check('异常任务卡与库一致（金标准 §3.3 待办区指标）',
    cardValue['异常任务'] === String(expToday.exceptionOrders),
    `${cardValue['异常任务']} vs ${expToday.exceptionOrders}`)
  check('超时未签收卡与库一致（金标准 §3.3 待办区指标）',
    cardValue['超时未签收'] === String(expToday.overdueOrders),
    `${cardValue['超时未签收']} vs ${expToday.overdueOrders}`)
  check('待处理预警卡与库一致', cardValue['待处理预警'] === String(todoCounts.pendingAlertCount),
    `${cardValue['待处理预警']} vs ${todoCounts.pendingAlertCount}`)
  check('活跃人车绑定卡与库一致', cardValue['活跃人车绑定'] === String(todoCounts.activeBindingCount),
    `${cardValue['活跃人车绑定']} vs ${todoCounts.activeBindingCount}`)
  check('配送费卡带 ¥ 前缀与两位小数',
    cardValue['配送费'].includes('¥') && cardValue['配送费'].includes(Number(expToday.deliveryFee).toFixed(2)),
    cardValue['配送费'])
  check('代收货款卡带 ¥ 前缀', cardValue['代收货款'].includes('¥'), cardValue['代收货款'])
  check('准时率卡为百分比或「-」', /^\d+(\.\d+)?%$/.test(cardValue['准时率']) || cardValue['准时率'] === '-',
    cardValue['准时率'])
  check('平均配送时长卡为分钟或「-」',
    /\d+(\.\d+)?分钟$/.test(cardValue['平均配送时长']) || cardValue['平均配送时长'] === '-',
    cardValue['平均配送时长'])
  check('在线配送员卡为 x/y', /^\d+\/\d+$/.test(cardValue['在线配送员']), cardValue['在线配送员'])
  check('活跃车辆卡为 x/y', /^\d+\/\d+$/.test(cardValue['活跃车辆']), cardValue['活跃车辆'])
  check('卡片无「-」占位（有数据的指标不得为占位符）',
    ['配送单量', '待分配任务', '在途任务', '已完成', '异常任务', '超时未签收', '配送费', '代收货款']
      .every(l => cardValue[l] !== '-' && cardValue[l] !== ''),
    JSON.stringify(cardValue))

  // 口径回显条
  txt = await bodyText()
  for (const k of ['统计口径', '今日', '在线判定', '2分钟内上报', '更新时间']) {
    check(`口径回显含「${k}」`, txt.includes(k))
  }

  // ── 2. 图表 ──
  const canvasCount = await page.locator('.ar-report-chart canvas').count()
  check('三张图表均渲染 canvas', canvasCount >= 3, `count=${canvasCount}`)
  const chartTitles = await page.evaluate(() =>
    [...document.querySelectorAll('.ar-report-chart__title')].map(e => e.textContent.trim()))
  check('图表标题齐全',
    ['单量时效趋势', '任务状态分布', '渠道分布'].every(t => chartTitles.includes(t)),
    chartTitles.join('/'))
  const emptyCharts = await page.locator('.ar-report-chart__empty').count()
  check('无空态图表（数据真实存在）', emptyCharts === 0, `empty=${emptyCharts}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-dashboard.png'), fullPage: true })

  // ── 3. 待办区与绩效表 ──
  txt = await bodyText()
  for (const k of ['活跃人车绑定', '待处理预警', '配送员绩效Top']) {
    check(`待办区含「${k}」`, txt.includes(k))
  }
  const bindingRows = await page.locator('.section-card', { hasText: '活跃人车绑定' }).locator('tbody tr.ant-table-row').count()
  check('活跃绑定表行数与库一致（最多 5 行）',
    bindingRows === Math.min(5, todoCounts.activeBindingCount),
    `${bindingRows} vs min(5, ${todoCounts.activeBindingCount})`)
  const alertRows = await page.locator('.section-card', { hasText: '待处理预警' }).locator('tbody tr.ant-table-row').count()
  check('待处理预警表行数与库一致（最多 5 行）',
    alertRows === Math.min(5, todoCounts.pendingAlertCount),
    `${alertRows} vs min(5, ${todoCounts.pendingAlertCount})`)
  const topBox = page.locator('.section-card', { hasText: '配送员绩效' })
  const topRows = await topBox.locator('tbody tr.ant-table-row').count()
  const topText = await topBox.innerText()
  check('配送员绩效表仅列有单配送员（含 3 名 E2E）',
    topRows >= 3 && (topText.match(/E2E/g) || []).length >= 3,
    `rows=${topRows} e2e=${(topText.match(/E2E/g) || []).length}`)
  check('预警表展示中文类型（字典常量，非「类型1」）',
    txt.includes('人车分离') || txt.includes('异常滞留') || txt.includes('偏离路线'), '')
  check('绑定表展示「绑定中」（状态字典 0=绑定中）',
    todoCounts.activeBindingCount === 0 || txt.includes('绑定中'),
    todoCounts.activeBindingCount === 0 ? '库中无活跃绑定样本，跳过' : '')

  // ── 4. 时间范围切换（近 7 日） ──
  await page.locator('.range-switch button', { hasText: '近 7 日' }).click()
  await page.waitForTimeout(2500)
  let now = await readCards()
  check('切换「近 7 日」后配送单量与库口径一致',
    now.map['配送单量'] === String(exp7.orderCount),
    `${now.map['配送单量']} vs ${exp7.orderCount}`)
  check('切换口径后回显区间文案为「近 7 日」', (await bodyText()).includes('近7日'))

  // ── 5. 渠道筛选（今日口径） ──
  await page.locator('.range-switch button', { hasText: '今日' }).click()
  await page.waitForTimeout(2200)
  // antd 会对两字按钮插入空格（「查 询」），比对前需去空白
  const searchButtons = await page.evaluate(() =>
    [...document.querySelectorAll('.search-area button')].map(b => (b.innerText || '').replace(/\s+/g, '')))
  check('查询区含 查询/重置 按钮',
    searchButtons.includes('查询') && searchButtons.includes('重置'), searchButtons.join('/'))
  await page.locator('.search-item', { hasText: '运力渠道' }).locator('.ant-select').click()
  await page.waitForTimeout(600)
  await page.locator('.ant-select-item-option', { hasText: 'E2E达达' }).first().click()
  await page.waitForTimeout(2500)
  const expDada = await dbTaskAgg(TODAY_START, TODAY_END, `AND channel_id = ${CHANNEL_DADA}`)
  now = await readCards()
  check('渠道筛选「E2E达达」后与库口径一致',
    now.map['配送单量'] === String(expDada.orderCount),
    `${now.map['配送单量']} vs ${expDada.orderCount}`)
  check('渠道筛选后 ≥ 5 单（E2E 样本）', Number(now.map['配送单量']) >= 5, now.map['配送单量'])
  check('筛选后图表仍渲染（随筛选刷新）', (await page.locator('.ar-report-chart canvas').count()) >= 3,
    `canvas=${await page.locator('.ar-report-chart canvas').count()}`)

  // 清除渠道（a-select 清空图标）
  const channelSelect = page.locator('.search-item', { hasText: '运力渠道' }).locator('.ant-select')
  await channelSelect.hover()
  await page.waitForTimeout(300)
  await page.locator('.ant-select-clear').click()
  await page.waitForTimeout(2500)
  now = await readCards()
  check('清除渠道后回到今日口径', now.map['配送单量'] === String(expToday.orderCount),
    `${now.map['配送单量']} vs ${expToday.orderCount}`)

  // ── 6. 卡片下钻 ──
  await drillAndWait('待分配任务', /\/dms\/dispatch-task/)
  check('点击「待分配任务」下钻到调度任务并带 status=0',
    page.url().includes('/dms/dispatch-task') && page.url().includes('status=0'), page.url())
  await page.screenshot({ path: path.join(SHOTS, 'ui-drill-dispatch-task.png') })

  // 同路由重复下钻：调度任务页被复用，需响应 query 变化（status 0 → 8）
  await openPage(`${FE}/dms/dashboard`, 4500)
  await drillAndWait('异常任务', /\/dms\/dispatch-task/)
  check('同路由重复下钻：异常任务 → status=8（组件复用仍生效）',
    page.url().includes('/dms/dispatch-task') && page.url().includes('status=8'), page.url())

  await openPage(`${FE}/dms/dashboard`, 4500)
  await drillAndWait('待处理预警', /\/dms\/verification/)
  check('点击「待处理预警」下钻到实名认证并带预警 Tab 参数',
    page.url().includes('/dms/verification') && page.url().includes('tab=alert') &&
    page.url().includes('handleStatus=0'), page.url())
  await page.screenshot({ path: path.join(SHOTS, 'ui-drill-verification.png') })

  await openPage(`${FE}/dms/dashboard`, 4500)
  await drillAndWait('超时未签收', /\/dispatch\/query/)
  check('超时未签收下钻：配送查询承接多值 status（PENDING,DELIVERING）',
    page.url().includes('/dispatch/query') && page.url().includes('PENDING'), page.url())

  await openPage(`${FE}/dms/dashboard`, 4500)
  await drillAndWait('在途任务', /\/dispatch\/query/)
  check('点击「在途任务」下钻到配送查询并带 status=DELIVERING',
    page.url().includes('/dispatch/query') && page.url().includes('DELIVERING'), page.url())
  check('配送查询承接下钻条件（三值筛选显示已选配送中）', (await bodyText()).includes('配送中'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-drill-dispatch-query.png') })

  // ── 7. 页面配置：查询条件显隐 + 功能按钮开关真实生效 ──
  await openPage(`${FE}/dms/dashboard`, 4500)
  await waitCards()
  const openConfig = async () => {
    await page.locator('button:has(.anticon-setting)').first().click()
    await page.waitForTimeout(900)
  }
  const closeConfig = async () => {
    await page.locator('.ant-tabs-tabpane-active .tab-footer button').filter({ hasText: /关\s*闭/ }).first().click()
    await page.waitForTimeout(900)
  }
  await openConfig()
  check('页面配置弹窗打开', (await bodyText()).includes('页面配置'))
  const configRows = await page.evaluate(() =>
    [...document.querySelectorAll('.ant-tabs-tabpane-active .config-table tbody tr')].map(tr => tr.innerText.replace(/\s+/g, '')))
  check('页面配置含 3 个查询条件项（时间范围/运力渠道/订单类型）',
    ['时间范围', '运力渠道', '订单类型'].every(k => configRows.some(r => r.includes(k))),
    configRows.join('|'))

  // 功能按钮 Tab：勾选项齐全；关闭「导出」后工具栏按钮真实消失
  await page.locator('.config-tabs .ant-tabs-tab', { hasText: '功能按钮' }).click()
  await page.waitForTimeout(400)
  const btnRows = await page.evaluate(() =>
    [...document.querySelectorAll('.ant-tabs-tabpane-active .config-table tbody tr')].map(tr => tr.innerText.replace(/\s+/g, '')))
  check('功能按钮 Tab 含 图表/刷新/自动刷新/导出/全屏',
    ['图表', '刷新', '自动刷新', '导出', '全屏'].every(k => btnRows.some(r => r.includes(k))),
    btnRows.join('|'))
  await page.locator('.ant-tabs-tabpane-active .config-table tbody tr', { hasText: '导出' }).locator('.ant-checkbox-input').click()
  await page.waitForTimeout(400)
  await closeConfig()
  check('关闭「导出」按钮后工具栏不再显示导出',
    (await page.locator('.toolbar-right button').filter({ hasText: /导\s*出/ }).count()) === 0)

  // 恢复默认（需在「功能按钮」Tab 上执行恢复）：导出按钮回归
  await openConfig()
  await page.locator('.config-tabs .ant-tabs-tab', { hasText: '功能按钮' }).click()
  await page.waitForTimeout(400)
  await page.locator('.ant-tabs-tabpane-active .tab-footer button').filter({ hasText: /恢复\s*默认/ }).first().click()
  await page.waitForTimeout(400)
  await closeConfig()
  check('「恢复默认」后导出按钮回归（功能按钮配置真实生效）',
    (await page.locator('.toolbar-right button').filter({ hasText: /导\s*出/ }).count()) === 1)

  // 查询条件显隐：隐藏「运力渠道」后查询区不再渲染该条件
  await openConfig()
  await page.locator('.ant-tabs-tabpane-active .config-table tbody tr', { hasText: '运力渠道' }).locator('.ant-checkbox-input').click()
  await page.waitForTimeout(400)
  await closeConfig()
  check('取消「运力渠道」后查询区不再渲染该条件',
    (await page.locator('.search-item', { hasText: '运力渠道' }).count()) === 0)
  await openConfig()
  await page.locator('.ant-tabs-tabpane-active .tab-footer button').filter({ hasText: /恢复\s*默认/ }).first().click()
  await page.waitForTimeout(400)
  await closeConfig()
  check('恢复默认后「运力渠道」条件回归',
    (await page.locator('.search-item', { hasText: '运力渠道' }).count()) === 1)

  // ── 8. 「显示图表」勾选框与图表区联动 ──
  await page.locator('.search-area .ant-checkbox-wrapper', { hasText: '显示图表' }).click()
  await page.waitForTimeout(1200)
  check('取消「显示图表」后图表区隐藏', (await page.locator('.ar-report-chart').count()) === 0,
    `charts=${await page.locator('.ar-report-chart').count()}`)
  await page.locator('.search-area .ant-checkbox-wrapper', { hasText: '显示图表' }).click()
  await page.waitForTimeout(1200)
  check('重新勾选后图表区恢复', (await page.locator('.ar-report-chart').count()) >= 3,
    `charts=${await page.locator('.ar-report-chart').count()}`)

  // ── 9. 导出真实 xlsx ──
  await page.waitForTimeout(600)
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 15000 }),
    page.locator('.toolbar-right button').filter({ hasText: /导\s*出/ }).click(),
  ])
  const fname = download.suggestedFilename()
  check('导出文件名为配送仪表盘 xlsx', fname.startsWith('配送仪表盘') && fname.endsWith('.xlsx'), fname)
  const savedPath = path.join(SHOTS, fname)
  await download.saveAs(savedPath)
  const size = fs.statSync(savedPath).size
  check('导出文件非空（真实二进制）', size > 2000, `${size} bytes`)

  // ── 10. 自动刷新 ──
  const autoBtn = page.locator('.toolbar-right button:has(.anticon-sync)').first()
  check('自动刷新按钮存在且显示倒计时',
    (await autoBtn.count()) > 0 && /\d+s/.test(await autoBtn.innerText()),
    await autoBtn.innerText().catch(() => ''))
  await autoBtn.click()
  await page.waitForTimeout(800)
  check('关闭自动刷新后提示停止', (await bodyText()).includes('已停止自动刷新'))

  await page.screenshot({ path: path.join(SHOTS, 'ui-final.png'), fullPage: true })
  await browser.close()
}

// ══════════════════════════════════════════════════════════════
// 主流程
// ══════════════════════════════════════════════════════════════
;(async () => {
  try {
    await apiSuite()
  } catch (e) {
    console.error('接口验收异常:', e && (e.message || e))
  }
  try {
    await uiSuite()
  } catch (e) {
    console.error('UI 验收异常:', e && (e.message || e))
  }

  if (DB) await DB.end().catch(() => {})

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 验收结果：${pass}/${results.length} 通过${fail ? `，${fail} 失败` : ''} ═══`)
  if (fail) {
    console.log('\n失败项：')
    results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  }
  fs.writeFileSync(path.join(SHOTS, 'result.json'), JSON.stringify(results, null, 2), 'utf8')
  process.exit(fail ? 1 : 0)
})()
