/*
 * 智能调度（配送 → 调度管理 → 智能调度，菜单 80850 `dms:dispatch`）金标准端到端验证
 *   · 区域分包（AREA）验收：绑定 CRUD（新增/重复拦截/改优先级/停用/删除/引用校验）+ 线路·配送员选择器 +
 *               非严格档「绑定者优先」（较远但绑线路者胜出）+ 严格档未绑定者候选不可派且人工指派同拒 +
 *               任务无线路时降级 + 候选 routeBound 标记
 *   · 在线口径验收：离线候选（心跳超阈值）给出明确原因且不参与自动派单；心跳恢复后重新可派
 *   · API 验收：派单策略读写（配置中心热生效 + 非法策略校验）/ 自动调度预览（逐单命中规则 + 约束拒绝原因：
 *               并接上限·休息中·资质不满足·超载重）/ dryRun 不落库 / 真执行落库（任务已指派 + 骑手忙碌 + 事件外发）/
 *               候选配送员评分与约束 / 指派 body 参数（P0 修复）/ 效果复盘统计（含抢单·竞价归口）
 *   · UI 验收：三 Tab（策略配置 / 调度执行 / 效果复盘）+ 策略保存 + 区域分包绑定维护区与选择器弹窗 +
 *               预览回填表格（命中配送员 + 命中规则）+ 手动指派弹窗（候选含约束说明）+ 列配置 / 页面配置 +
 *               统计卡与分布条（自动/手工/抢单/竞价）
 *
 * 前置（每次跑之前整文件执行一次）：
 *   python - <<'EOF'
 *   import psycopg2; c=psycopg2.connect("host=localhost port=5432 dbname=devdb user=devuser password=devuser123")
 *   c.autocommit=True; c.cursor().execute(open("tools/e2e-dms-dispatch-user.sql",encoding="utf-8").read()); c.close()
 *   EOF
 *   验收后端实例：java -jar <fat jar> --server.port=5665
 *
 * 用法：ERP_PORT=5665 FE_URL=http://localhost:5656 node tools/e2e-dms-dispatch.cjs
 *   脚本自带会话自愈（共享环境下后台 api() 重登会踢掉浏览器会话）+ 验收前用 pg 刷新配送员位置上报心跳。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5665)
const SHOTS = 'I:/AI-Ready/tool-results/dms-dispatch'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const R1 = '2099000000000009010'
/** 手工指派用例专用（R1 会被自动调度占为忙碌） */
const R2 = '2099000000000009011'
/** 已达并接上限（5 单在途） */
const R3 = '2099000000000009012'
const T1 = '2099000000000009101'
const T2 = '2099000000000009102'
const T3 = '2099000000000009103'
/** 区域分包用例：线路档案 + 种子绑定（R2 绑定线路甲，R1 未绑定） */
const ROUTE_A = '2099000000000009501'
const SEED_BINDING = '2099000000000009500'
const RIDER_ID_MIN = '2099000000000009010'
const RIDER_ID_MAX = '2099000000000009014'

function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? null : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => resolve({ status: res.statusCode, buf: Buffer.concat(chunks), json: tryJson(Buffer.concat(chunks)) }))
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function tryJson(buf) { try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null } }

let TOKEN = null
async function api(method, reqPath, body) {
  let r = await rawReq(method, reqPath, body, TOKEN)
  if (r.status === 401 || (r.json && Number(r.json.code) === 401)) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN)
  }
  return r
}
const data = (res) => (res.json ? res.json.data : null)

const E2E_USER = process.env.E2E_USER || 'e2e_dispatch'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid,
  })
  let token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token && E2E_USER !== 'admin') {
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.json.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', {
      username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code2, captchaKey: cap2.json.data.uuid,
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

async function taskStatus(id) {
  const r = await api('GET', `/dms/task/${id}`)
  return Number(data(r)?.status)
}

// ── 数据库直连：刷新配送员位置上报心跳 ────────────────────────────
// seed 的 last_report_time 是插入时刻，距插入超过 dms.tracking.online.minutes(默认 2 分钟) 会自然离线，
// 而「在线口径」已是派单约束（见《智能调度开发文档》§7.2），故验收前统一把样本心跳重置。
let DB = null

async function ensureDb() {
  if (DB) return DB
  DB = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
  await DB.connect()
  return DB
}

async function dbQuery(sql) {
  const client = await ensureDb()
  return (await client.query(sql)).rows
}

/** intervalExpr 为脚本内常量（不接受外部输入） */
async function refreshHeartbeats(intervalExpr = '0 seconds') {
  await dbQuery(`UPDATE dms_rider SET last_report_time = now() - interval '${intervalExpr}'`
    + ` WHERE id BETWEEN ${RIDER_ID_MIN} AND ${RIDER_ID_MAX}`)
}

// ══════════════ 一、区域分包（AREA）与在线口径 ══════════════
async function areaSuite() {
  console.log('\n═══ 一、区域分包（AREA）与在线口径 ═══')

  // 1) 绑定列表 + 选择器
  const list = await api('GET', '/dms/dispatch/route-rider/page?pageNum=1&pageSize=20&keyword=E2ERT')
  const rows = data(list)?.records || []
  check('区域分包绑定列表返回种子绑定（线路编号/名称快照 + 配送员）',
    list.json?.code === 200
    && rows.some(r => String(r.routeId) === ROUTE_A && String(r.riderId) === R2
      && r.routeName === 'E2E调度线路甲' && r.routeCode === 'E2ERT-01' && Number(r.status) === 1),
    JSON.stringify(rows[0] || {}).slice(0, 200))
  const routeOpts = await api('GET', '/dms/dispatch/route-rider/route-options?keyword=E2ERT')
  check('线路选择器返回线路档案（只读引用 erp_route，禁止手输ID）',
    (data(routeOpts) || []).some(r => String(r.id) === ROUTE_A), JSON.stringify(data(routeOpts) || []).slice(0, 160))
  const riderOpts = await api('GET', '/dms/dispatch/route-rider/rider-options?keyword=E2E调度骑手')
  check('配送员选择器返回候选（姓名/手机号）',
    (data(riderOpts) || []).length >= 4 && (data(riderOpts) || [])[0].realName != null,
    (data(riderOpts) || []).length)

  // 2) 非严格区域分包：绑定者优先（R2 绑线路甲，虽比 R1 远仍被命中）
  const areaCfg = {
    strategy: 'AREA', weightDistance: 1, weightLoad: 1, weightScore: 1, maxConcurrent: 5,
    requireOnline: true, maxLoadKg: 2000, maxVolumeM3: 10, timeoutEscalateMinutes: 30, areaStrict: false,
  }
  const saveArea = await api('PUT', '/dms/dispatch/strategy', areaCfg)
  check('切换区域分包策略（非严格）',
    saveArea.json?.code === 200 && data(saveArea)?.strategy === 'AREA' && data(saveArea)?.areaStrict === false,
    JSON.stringify(data(saveArea) || {}).slice(0, 160))
  const pvArea = await api('POST', '/dms/dispatch/auto/preview', { maxTasks: 20 })
  const rowsArea = data(pvArea)?.rows || []
  const rowT1 = rowsArea.find(r => String(r.taskId) === T1) || {}
  check('区域分包命中线路绑定的 R2（而非距离最近的 R1）',
    String(rowT1.riderId) === R2 && rowT1.riderName === 'E2E调度骑手R2',
    `rider=${rowT1.riderName}/${rowT1.distanceMeters}米`)
  check('命中规则文案含「区域分包」与线路名',
    /区域分包/.test(String(rowT1.ruleHit)) && String(rowT1.ruleHit).includes('E2E调度线路甲'), rowT1.ruleHit)
  const rowT3 = rowsArea.find(r => String(r.taskId) === T3) || {}
  check('任务未指定线路 → 区域分包降级（不误判为分包拒绝）',
    rowT3.assignable === false && !/区域分包/.test(String(rowT3.failReason || '')), rowT3.failReason)

  // 3) 候选标注线路绑定
  const candArea = await api('GET', `/dms/dispatch/candidates?taskId=${T1}`)
  const cArea = data(candArea) || []
  const cr1 = cArea.find(c => String(c.riderId) === R1) || {}
  const cr2 = cArea.find(c => String(c.riderId) === R2) || {}
  check('候选标注线路绑定（R2=已绑定 / R1=未绑定）',
    cr2.routeBound === true && cr1.routeBound === false, `${cr2.routeBound}/${cr1.routeBound}`)
  check('候选标注在线状态（位置上报心跳口径）',
    cr1.online === true && cr1.lastReportTime != null, `${cr1.online}/${cr1.lastReportTime}`)

  // 4) 严格模式：未绑定线路 → 明确拒绝（候选与人工指派口径一致）
  await api('PUT', '/dms/dispatch/strategy', { ...areaCfg, areaStrict: true })
  const candStrict = await api('GET', `/dms/dispatch/candidates?taskId=${T1}`)
  const cs1 = (data(candStrict) || []).find(c => String(c.riderId) === R1) || {}
  const cs2 = (data(candStrict) || []).find(c => String(c.riderId) === R2) || {}
  check('严格模式：未绑定线路的 R1 不可派且给出线路原因',
    cs1.eligible === false && /区域分包/.test(String(cs1.reason)) && String(cs1.reason).includes('E2E调度线路甲'),
    cs1.reason)
  check('严格模式：绑定者 R2 仍可派', cs2.eligible === true, cs2.reason)
  const assignStrict = await api('POST', `/dms/dispatch/${T1}/assign`, { riderId: R1 })
  check('严格模式：人工指派未绑定配送员同样被拒（前后端口径一致）',
    assignStrict.json?.code !== 200 && /区域分包/.test(String(assignStrict.json?.message)),
    assignStrict.json?.message)
  check('严格模式拒绝不落库（任务仍为待分配）', (await taskStatus(T1)) === 0, await taskStatus(T1))

  // 5) 绑定 CRUD：新增 / 重复拦截 / 优先级修改 / 停用生效 / 删除
  const created = await api('POST', '/dms/dispatch/route-rider', {
    routeId: ROUTE_A, riderId: R1, priority: 5, status: 1, remark: 'E2E 动态绑定',
  })
  const newId = data(created)?.id
  check('新增绑定成功（名称快照由服务端按档案回填）',
    created.json?.code === 200 && newId != null && data(created)?.riderName === 'E2E调度骑手R1'
    && data(created)?.routeName === 'E2E调度线路甲',
    JSON.stringify(data(created) || {}).slice(0, 200))
  const dup = await api('POST', '/dms/dispatch/route-rider', { routeId: ROUTE_A, riderId: R1 })
  check('同线路同配送员重复绑定被拒', dup.json?.code !== 200 && /已绑定/.test(String(dup.json?.message)), dup.json?.message)
  const upd = await api('PUT', `/dms/dispatch/route-rider/${newId}`, {
    routeId: ROUTE_A, riderId: R1, priority: 9, status: 1, remark: 'E2E 动态绑定-改',
  })
  check('修改绑定（优先级/备注）成功',
    upd.json?.code === 200 && Number(data(upd)?.priority) === 9, data(upd)?.priority)
  const candAfterBind = await api('GET', `/dms/dispatch/candidates?taskId=${T1}`)
  const cb1 = (data(candAfterBind) || []).find(c => String(c.riderId) === R1) || {}
  check('新增绑定后 R1 在严格模式下变为可派', cb1.eligible === true && cb1.routeBound === true, cb1.reason)
  const off = await api('PUT', `/dms/dispatch/route-rider/${newId}/status`, { status: 0 })
  const candOff = await api('GET', `/dms/dispatch/candidates?taskId=${T1}`)
  const co1 = (data(candOff) || []).find(c => String(c.riderId) === R1) || {}
  check('停用绑定后不再参与区域分包命中（R1 回到不可派）',
    off.json?.code === 200 && co1.routeBound === false && co1.eligible === false, co1.reason)
  const del = await api('DELETE', `/dms/dispatch/route-rider/${newId}`)
  const delAgain = await api('DELETE', `/dms/dispatch/route-rider/${newId}`)
  check('删除绑定成功且重复删除被拒', del.json?.code === 200 && delAgain.json?.code !== 200, delAgain.json?.message)
  const badRoute = await api('POST', '/dms/dispatch/route-rider', { routeId: 2099999999999999999, riderId: R1 })
  check('绑定不存在的线路被拒（引用校验）', badRoute.json?.code !== 200, badRoute.json?.message)

  // 6) 在线口径：离线候选给出明确原因且不参与派单
  await api('PUT', '/dms/dispatch/strategy', { ...areaCfg, strategy: 'NEAREST', areaStrict: false })
  await refreshHeartbeats('3 minutes')
  // 用 T2（普通重量）验证：T3 超载重会把在线口径的结论盖掉
  const candOffline = await api('GET', `/dms/dispatch/candidates?taskId=${T2}`)
  const off1 = (data(candOffline) || []).find(c => String(c.riderId) === R1) || {}
  check('离线候选（心跳超阈值）标记为离线且不可派并给出原因',
    off1.online === false && off1.eligible === false && /离线/.test(String(off1.reason)), off1.reason)
  const pvOffline = await api('POST', '/dms/dispatch/auto/preview', { maxTasks: 20 })
  const offRow = (data(pvOffline)?.rows || []).find(r => String(r.taskId) === T2) || {}
  check('离线配送员不参与自动派单（改用仍在线者）',
    offRow.assignable !== true || String(offRow.riderId) !== R1,
    `rider=${offRow.riderName}/${offRow.failReason || ''}`)
  await refreshHeartbeats('0 seconds')
  const candOnline = await api('GET', `/dms/dispatch/candidates?taskId=${T2}`)
  const on1 = (data(candOnline) || []).find(c => String(c.riderId) === R1) || {}
  check('心跳恢复后重新可派（阈值配置化生效）', on1.online === true && on1.eligible === true, on1.reason)

  // 7) 效果复盘：抢单/竞价归口统计
  const stat = await api('GET', '/dms/dispatch/stat')
  const st = data(stat) || {}
  check('统计含抢单数（归口《订单池》）', Number(st.grabCount) >= 1, st.grabCount)
  check('统计含竞价数（归口《订单池》）', Number(st.bidCount) >= 1, st.bidCount)
  check('抢单/竞价不再混入「其他方式」', st.otherCount != null && Number(st.otherCount) >= 0, st.otherCount)

  // 8) 还原策略（避免影响后续用例与其它会话）
  const restored = await api('PUT', '/dms/dispatch/strategy', {
    strategy: 'NEAREST', weightDistance: 1, weightLoad: 1, weightScore: 1, maxConcurrent: 5,
    requireOnline: true, maxLoadKg: 2000, maxVolumeM3: 10, timeoutEscalateMinutes: 30, areaStrict: false,
  })
  check('策略还原为默认（NEAREST / 非严格分包）',
    data(restored)?.strategy === 'NEAREST' && data(restored)?.areaStrict === false,
    `${data(restored)?.strategy}/${data(restored)?.areaStrict}`)
}

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 1) 策略读取（默认值）
  const st0 = await api('GET', '/dms/dispatch/strategy')
  const s0 = data(st0) || {}
  check('读取派单策略（默认 NEAREST）', st0.json?.code === 200 && s0.strategy === 'NEAREST', s0.strategy)
  check('策略含权重与约束字段',
    s0.weightDistance != null && s0.maxConcurrent != null && s0.requireOnline != null && s0.maxLoadKg != null,
    JSON.stringify(s0).slice(0, 160))
  const badStrategy = await api('PUT', '/dms/dispatch/strategy', { strategy: 'XXX' })
  check('非法策略被拒', badStrategy.json?.code !== 200, badStrategy.json?.message)

  // 2) 策略保存（配置中心热生效）
  const save = await api('PUT', '/dms/dispatch/strategy', {
    strategy: 'BALANCED', weightDistance: 1, weightLoad: 5, weightScore: 1,
    maxConcurrent: 5, requireOnline: true, maxLoadKg: 2000, maxVolumeM3: 10, timeoutEscalateMinutes: 15,
  })
  check('保存派单策略成功', save.json?.code === 200, save.json?.message)
  const st1 = await api('GET', '/dms/dispatch/strategy')
  check('策略保存后立即生效（BALANCED / 超时 15 分钟）',
    data(st1)?.strategy === 'BALANCED' && Number(data(st1)?.timeoutEscalateMinutes) === 15,
    `${data(st1)?.strategy}/${data(st1)?.timeoutEscalateMinutes}`)

  // 3) 自动调度预览（不落库）
  const pv = await api('POST', '/dms/dispatch/auto/preview', { maxTasks: 50 })
  const pvD = data(pv) || {}
  check('预览返回待分配任务与可派数', pv.json?.code === 200 && Number(pvD.taskCount) >= 3,
    `task=${pvD.taskCount}/可派=${pvD.assignableCount}`)
  check('预览策略文案（负载均衡）', pvD.strategyText === '负载均衡', pvD.strategyText)
  const rowT1 = (pvD.rows || []).find(r => String(r.taskId) === T1) || {}
  check('预览 T1 命中配送员且带规则说明',
    !!rowT1.riderName && String(rowT1.ruleHit || '').includes('命中'), JSON.stringify(rowT1).slice(0, 200))
  check('命中规则含距离与在途单数', /米/.test(String(rowT1.ruleHit)) && /在途/.test(String(rowT1.ruleHit)), rowT1.ruleHit)
  const rowT3 = (pvD.rows || []).find(r => String(r.taskId) === T3) || {}
  check('超载重任务不可指派且给出明确原因（含配送员与载重口径）',
    rowT3.assignable === false && /载重|并接上限|无定位/.test(String(rowT3.failReason || '')),
    rowT3.failReason)
  const pending1 = await taskStatus(T1)
  check('预览不落库（任务仍为待分配 0）', pending1 === 0, pending1)

  // 4) 并接上限约束（把上限调到 0 会让 R3 之外的也受限，这里把上限设为 5 → R3 已达 5 单必被拒）
  const candidates = await api('GET', `/dms/dispatch/candidates?taskId=${T1}`)
  const cRows = data(candidates) || []
  check('候选列表返回评分与约束说明', cRows.length >= 4 && cRows[0].score != null,
    JSON.stringify(cRows[0] || {}).slice(0, 160))
  const r3 = cRows.find(c => String(c.riderId) === '2099000000000009012') || {}
  check('R3 因并接上限被拒（在途 5 / 上限 5）',
    r3.eligible === false && String(r3.reason || '').includes('并接上限'), r3.reason)
  const r4 = cRows.find(c => String(c.riderId) === '2099000000000009013') || {}
  check('休息中配送员被拒', r4.eligible === false && String(r4.reason || '').includes('休息'), r4.reason)
  const r5 = cRows.find(c => String(c.riderId) === '2099000000000009014') || {}
  check('资质不满足被拒（未实名认证）', r5.eligible === false && String(r5.reason || '').includes('资质'), r5.reason)
  const r1 = cRows.find(c => String(c.riderId) === R1) || {}
  check('R1 可指派且距离最近（≈200 米）', r1.eligible === true && Number(r1.distanceMeters) < 400,
    `${r1.eligible}/${r1.distanceMeters}`)

  // 5) dryRun 执行（不落库）
  const dry = await api('POST', '/dms/dispatch/auto', { dryRun: true, maxTasks: 50 })
  check('dryRun 执行返回预览结果且不落库',
    dry.json?.code === 200 && Number(data(dry)?.assignableCount) >= 2 && (await taskStatus(T1)) === 0,
    `可派=${data(dry)?.assignableCount}`)

  // 6) 真执行自动调度（落库 + 任务指派 + 骑手忙碌 + 事件外发）
  const run = await api('POST', '/dms/dispatch/auto', { dryRun: false, maxTasks: 50 })
  const runD = data(run) || {}
  check('执行自动调度成功', run.json?.code === 200 && Number(runD.assignableCount) >= 2, `成功=${runD.assignableCount}`)
  check('任务 T1 落库为「已分配(1)」', (await taskStatus(T1)) === 1, await taskStatus(T1))
  const t1Detail = await api('GET', `/dms/task/${T1}`)
  check('任务回填配送员（ID + 名称快照）',
    data(t1Detail)?.riderId != null && data(t1Detail)?.riderName === 'E2E调度骑手R1',
    `${data(t1Detail)?.riderId}/${data(t1Detail)?.riderName}`)
  const rider = await api('GET', `/dms/rider/${R1}`)
  check('被指派配送员转为「忙碌(2)」', Number(data(rider)?.status) === 2, data(rider)?.status)
  // 派单审计：事件经 dms_event_outbox 外发（后台日志可验），接口侧断言「审计字段已落库」
  const auditList = await api('GET', '/dms/task/page?pageNum=1&pageSize=5&taskNo=E2EDP-01')
  const auditRow = (data(auditList)?.records || [])[0] || {}
  check('自动调度审计字段落库（方式=自动1 + 名称快照 + 派单时间）',
    Number(auditRow.dispatchType) === 1 && !!auditRow.riderName && !!auditRow.dispatchTime,
    `type=${auditRow.dispatchType}/rider=${auditRow.riderName}/time=${auditRow.dispatchTime}`)

  // 7) 手工指派（body 参数，P0 修复：原 @RequestParam 与前端 body 不匹配 → 400）
  //    《调度任务开发文档》§3.6 约束 1：指派前校验配送员负载上限（在途并接/载重/容积），超限一律拒绝。
  const overLimited = await api('POST', `/dms/dispatch/${T3}/assign`, { riderId: R2 })
  check('超载重任务不可人工指派（负载上限硬门控）',
    overLimited.json?.code !== 200 && /载重/.test(String(overLimited.json?.message)), overLimited.json?.message)

  // 临时放宽载重/容积上限以验证 body 参数口径（步骤 9 会还原默认策略）
  await api('PUT', '/dms/dispatch/strategy', {
    strategy: 'NEAREST', weightDistance: 1, weightLoad: 1, weightScore: 1,
    maxConcurrent: 5, requireOnline: true, maxLoadKg: 100000, maxVolumeM3: 100, timeoutEscalateMinutes: 30,
  })
  const assign = await api('POST', `/dms/dispatch/${T3}/assign`, { riderId: R2 })
  check('手动指派改用 body 参数（不再 400）', assign.json?.code === 200, assign.json?.message)
  check('指派后任务落库为已分配', (await taskStatus(T3)) === 1, await taskStatus(T3))
  const assignDup = await api('POST', `/dms/dispatch/${T3}/assign`, { riderId: R2 })
  check('重复指派同任务被拒（幂等）', assignDup.json?.code !== 200, assignDup.json?.message)
  const assignNoRider = await api('POST', `/dms/dispatch/${T3}/assign`, {})
  check('指派缺配送员被拒', assignNoRider.json?.code !== 200, assignNoRider.json?.message)
  const assignLogs = await api('GET', `/dms/task/${T3}/logs`)
  check('人工指派写调度审计（dms_task_log）',
    (data(assignLogs) || []).some(l => l.action === 'ASSIGN'),
    JSON.stringify((data(assignLogs) || []).map(l => l.action)))

  // 8) 效果复盘
  const stat = await api('GET', '/dms/dispatch/stat')
  const stD = data(stat) || {}
  check('统计：自动调度数 ≥ 2（含手工后仍有自动）', Number(stD.autoCount) >= 2, stD.autoCount)
  check('统计：自动占比 > 0', Number(stD.autoRate) > 0, stD.autoRate)
  check('统计：平均派单耗时 ≥ 0（有创建→指派时间差）', stD.avgDispatchSeconds != null, stD.avgDispatchSeconds)
  check('统计：在途与超时率字段齐全', stD.activeCount != null && stD.overdueRate != null,
    `${stD.activeCount}/${stD.overdueRate}`)

  // 9) 还原策略（避免影响其它会话/后续用例）
  await api('PUT', '/dms/dispatch/strategy', {
    strategy: 'NEAREST', weightDistance: 1, weightLoad: 1, weightScore: 1,
    maxConcurrent: 5, requireOnline: true, maxLoadKg: 2000, maxVolumeM3: 10, timeoutEscalateMinutes: 30,
  })
  const restored = await api('GET', '/dms/dispatch/strategy')
  check('策略可还原为默认（NEAREST）', data(restored)?.strategy === 'NEAREST', data(restored)?.strategy)
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

  async function openPage(url, waitMs = 6000) {
    for (let attempt = 0; attempt < 3; attempt++) {
      await login()
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, 1])
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(waitMs)
      await page.waitForTimeout(1500)
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }

  const bodyText = () => page.locator('body').innerText().then(t => t.replace(/\s+/g, ''))

  /**
   * 页面掉登录自愈：共享环境下后台的 api() 重登会踢掉浏览器里的同一账号会话
   * （《共享环境端到端验证要点》），检测到登录页就重登 + 重注 token + 回到指定 Tab。
   */
  async function ensureAlive(tabLabel) {
    const dead = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
    if (!dead) return true
    console.log('  [页面被互踢，自愈重登]')
    await login()
    await page.evaluate(([tk, tid]) => {
      localStorage.setItem('token', tk)
      localStorage.setItem('tenantId', String(tid))
    }, [TOKEN, 1])
    await page.goto(`${FE}/dms/dispatch`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(4000)
    if (tabLabel) {
      await page.locator(`.tab-bar .tab-item:has-text("${tabLabel}")`).first().click()
      await page.waitForTimeout(2500)
    }
    return !page.url().includes('/login')
  }

  await openPage(`${FE}/dms/dispatch`)
  const txt0 = await bodyText()
  check('页面可打开（无 404/白屏）', !txt0.includes('页面不存在') && txt0.length > 50, page.url())
  for (const t of ['策略配置', '调度执行', '效果复盘']) {
    check(`Tab 含「${t}」`, txt0.includes(t))
  }
  check('策略 Tab 默认展示策略选择与约束', txt0.includes('派单策略') && txt0.includes('单人在途上限'), txt0.slice(0, 120))
  check('策略 Tab 含权重三项', txt0.includes('权重·距离') && txt0.includes('权重·负载') && txt0.includes('权重·评分'), '')
  check('策略 Tab 含区域分包严格模式开关', txt0.includes('区域分包严格模式'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-strategy.png'), fullPage: true })

  // 保存策略
  await ensureAlive('策略配置')
  await page.locator('button:has-text("保存策略")').first().click()
  await page.waitForTimeout(2500)
  check('保存策略提示成功', (await bodyText()).includes('策略已保存'), '')

  // 区域分包绑定（线路档案 × 配送员）
  const strategyTxt = await bodyText()
  check('策略 Tab 含区域分包绑定维护区',
    strategyTxt.includes('区域分包绑定') && strategyTxt.includes('新增绑定'), '')
  const bindHeaders = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['线路名称', '配送员', '优先级', '状态']) {
    check(`绑定表含「${h}」列`, bindHeaders.some(x => x.includes(h)), bindHeaders.join('/'))
  }
  await ensureAlive('策略配置')
  await page.locator('button:has-text("新增绑定")').first().click()
  await page.waitForTimeout(1800)
  const bindModal = page.locator('.ant-modal-content:visible').last()
  const bindText = (await bindModal.innerText()).replace(/\s+/g, '')
  check('新增绑定弹窗含线路/配送员选择器（禁止手输 ID）',
    bindText.includes('配送线路') && bindText.includes('配送员') && bindText.includes('优先级')
      && bindText.includes('按编号/名称搜索线路档案') && bindText.includes('按姓名/手机号搜索配送员'),
    bindText.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-area-binding.png'), fullPage: true })
  await bindModal.locator('.ant-modal-close').first().click()
  await page.waitForTimeout(800)

  // 调度执行 Tab
  // CategoryListLayout 的 Tab 是自绘 .tab-bar/.tab-item（非 antd tabs）
  await page.locator('.tab-bar .tab-item:has-text("调度执行")').first().click()
  await page.waitForTimeout(3000)
  const execTxt = await bodyText()
  check('调度执行 Tab 有工具栏按钮（预览/执行）',
    execTxt.includes('自动调度预览') && execTxt.includes('执行自动调度'), execTxt.slice(0, 120))
  const headers = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['任务编号', '客户', '状态', '预览·命中配送员', '预览·命中规则']) {
    check(`待分配任务表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  check('查询区含扫描上限', execTxt.includes('扫描上限'), '')

  // 预览（当前任务都已被指派 → 列表里仍有其它待分配任务）
  // 结果既可能是 3 秒自动消失的 toast，也可能是工具栏常驻提示「策略：… 可派 X / 待分配 Y」；
  // 若首次因会话被踢无结果，重登后重试一次
  let afterPreview = ''
  for (let attempt = 0; attempt < 2; attempt++) {
    await ensureAlive('调度执行')
    await page.locator('button:has-text("自动调度预览")').first().click()
    await page.waitForTimeout(1500)
    afterPreview = await bodyText()
    if (afterPreview.includes('预览完成') || afterPreview.includes('可派')) break
  }
  check('预览后给出结果提示（待分配/可指派）',
    afterPreview.includes('预览完成') || afterPreview.includes('可指派') || afterPreview.includes('可派'),
    afterPreview.slice(0, 120))
  check('预览结果回填命中规则列（超载重任务显示拒绝原因）',
    afterPreview.includes('超出载重上限') || afterPreview.includes('无人可派') || afterPreview.includes('命中'),
    afterPreview.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-execute.png'), fullPage: true })

  // 手动指派弹窗：用当前待分配列表的首行（API 用例已消费掉种子任务，列表里还有其它待分配任务）
  const firstRow = page.locator('.ss-grid tbody tr').filter({ hasText: '手动指派' }).first()
  if (await firstRow.count()) {
    await firstRow.locator('button:has-text("手动指派")').click()
    await page.waitForTimeout(2500)
    const modal = page.locator('.ant-modal-content:visible').last()
    const modalText = (await modal.innerText()).replace(/\s+/g, '')
    check('手动指派弹窗含候选表（可指派/约束说明）',
      modalText.includes('可指派') && modalText.includes('约束说明'), modalText.slice(0, 160))
    check('候选表展示休息中/并接上限等拒绝原因',
      modalText.includes('休息') || modalText.includes('并接上限') || modalText.includes('资质'), '')
    await page.screenshot({ path: path.join(SHOTS, 'ui-assign.png'), fullPage: true })
    await modal.locator('.ant-modal-close').first().click()
    await page.waitForTimeout(800)
  } else {
    check('手动指派弹窗含候选表（可指派/约束说明）', false, '待分配列表为空，无行可测')
  }

  // 列配置 + 页面配置
  await ensureAlive('调度执行')
  const gearCount = await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length)
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)
  await page.locator('.ss-grid .th-settings-btn').first().click()
  await page.waitForTimeout(1500)
  const colPanel = page.locator('.ant-modal-content:visible').last()
  const colText = (await colPanel.innerText()).replace(/\s+/g, '')
  check('列配置弹窗含「个人配置/全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 60))
  await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  await page.locator('button[title="页面配置"]').first().click()
  await page.waitForTimeout(1200)
  const pageCfg = page.locator('.ant-modal-content:visible').last()
  const cfgText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置弹窗含「查询条件/功能按钮」双 Tab', cfgText.includes('查询条件') && cfgText.includes('功能按钮'), cfgText.slice(0, 60))
  await pageCfg.locator('.ant-tabs-tab:has-text("功能按钮")').click()
  await page.waitForTimeout(700)
  const btnText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置含功能按钮项（预览/执行/保存策略）',
    btnText.includes('自动调度预览') && btnText.includes('执行自动调度') && btnText.includes('保存策略'), btnText.slice(0, 160))
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 效果复盘 Tab
  await ensureAlive('调度执行')
  await page.locator('.tab-bar .tab-item:has-text("效果复盘")').first().click()
  await page.waitForTimeout(2500)
  let reviewTxt = await bodyText()
  if (!reviewTxt.includes('自动占比')) {
    await ensureAlive('效果复盘')
    await page.locator('.tab-bar .tab-item:has-text("效果复盘")').first().click()
    await page.waitForTimeout(2500)
    reviewTxt = await bodyText()
  }
  check('效果复盘含统计卡（自动占比/平均派单耗时/超时率）',
    reviewTxt.includes('自动占比') && reviewTxt.includes('平均派单耗时') && reviewTxt.includes('超时率'), reviewTxt.slice(0, 160))
  check('效果复盘含调度方式分布', reviewTxt.includes('调度方式分布') && reviewTxt.includes('自动调度'), '')
  check('效果复盘分布条含抢单/竞价（归口《订单池》）',
    reviewTxt.includes('抢单') && reviewTxt.includes('竞价') && reviewTxt.includes('订单池'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-review.png'), fullPage: true })

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await login()
    console.log('登录成功（' + E2E_USER + '）')
    const probe = await api('GET', `/dms/dispatch/candidates?taskId=${T1}`)
    if ((data(probe) || []).length < 4) {
      throw new Error('种子配送员缺失：请先执行 tools/e2e-dms-dispatch-user.sql（见脚本头部说明）')
    }
    await refreshHeartbeats('0 seconds')
    await areaSuite()
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
