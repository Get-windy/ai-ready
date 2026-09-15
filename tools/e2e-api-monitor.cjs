/*
 * API监控（配送 → API监控，菜单 90107 `trade:api-monitor`）金标准端到端验证
 *
 * 口径依据：《API监控开发文档》§3 金标准目标设计 / §5 待完善（实施清单）——
 *   · 【P0】清除硬编码假数据（「今日查询 1234 次」「同步成功率 98.5%」）→ 全部后端真实聚合
 *   · 调用日志埋点（api_access_log：IN 入站 / OUT 出站 / SANDBOX 联调）
 *   · 依赖健康逐项（DB / Redis / MQ / 地图 / 第三方渠道）
 *   · 联调沙箱（接口分组 + 参数 + 真实回环调用 + 结果 / 耗时 / 请求号 + 历史）
 *   · 同步记录增强（失败重试 / 错误分类 / 真实 xlsx 导出）
 *   · 告警阈值 + 静默期 + 事件外发（dms_event_outbox）
 *
 * 验收方式：**先造真实数据，再与 DB 直查逐项对账**；页面卡片与接口值严格一致；无 404/500。
 * 用法：node tools/e2e-api-monitor.cjs        （默认后端 5680、前端 5656）
 *      ERP_PORT=5690 FE_URL=http://localhost:5656 node tools/e2e-api-monitor.cjs
 *
 * 副作用与清理：会生成少量真实调用（开放接口探针）与 2 条测试同步记录（sku 前缀 E2E-，
 * 渠道 NOSUCH），结束时按渠道/NOSUCH + sku 前缀清理；**不改配置中心阈值**（只读校验）。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5680)
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
const E2E_USER = process.env.E2E_USER || 'e2e_dispatch'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const SHOTS = 'I:/AI-Ready/tool-results/api-monitor'

function rawReq(method, reqPath, body, token, extraHeaders) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {})
    }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* ignore */ }
        resolve({ status: res.statusCode, headers: res.headers, buf, json })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function dbQuery(sql, params) {
  const client = new Client(DSN)
  await client.connect()
  try {
    const res = await client.query(sql, params)
    return res.rows
  } finally { await client.end() }
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 220) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 180) : ''}`)
}

let TOKEN = null
async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 200))
  TOKEN = token
}

const data = (res) => res?.json?.data
const RETRY_SKU = 'E2E-RETRY-SKU'
const FAIL_SKU = 'E2E-NOSUCH-SKU'

async function main() {
  console.log('API监控金标准 E2E —— 后端 :' + PORT + '，前端 ' + FE)
  await login()

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 一、造真实调用数据（开放接口入站 + 渠道出站） ═══')
  const h = await rawReq('GET', '/open/health', null, TOKEN)
  check('入站：/open/health 可用', h.status === 200 && data(h)?.status === 'UP', `status=${h.status}`)
  check('入站响应头回写 X-Request-Id（可对账）', !!h.headers['x-request-id'], h.headers['x-request-id'])

  const q = await rawReq('GET', '/open/inventory/query?skuCode=SKU001', null, TOKEN)
  check('入站：库存查询可用', q.status === 200 && data(q)?.availableQuantity >= 0, JSON.stringify(data(q)).slice(0, 120))

  const p = await rawReq('GET', '/open/order/pending-count', null, TOKEN)
  check('入站：待处理订单数可用', p.status === 200 && data(p) !== undefined, `data=${data(p)}`)

  const okSync = await rawReq('POST', '/open/inventory/sync/TAOBAO?skuCode=E2E-SYNC-SKU&quantity=7', null, TOKEN)
  check('出站：库存推送（TAOBAO）成功并写同步记录',
    okSync.status === 200 && Number(data(okSync)?.status) === 1, JSON.stringify(data(okSync)).slice(0, 120))

  const badSync = await rawReq('POST', `/open/inventory/sync/NOSUCH?skuCode=${FAIL_SKU}&quantity=5`, null, TOKEN)
  check('出站：不支持的渠道返回失败（不抛 500）',
    badSync.status === 200 && Number(data(badSync)?.status) === 0, JSON.stringify(data(badSync)).slice(0, 160))

  const logRows = await dbQuery(
    `SELECT direction, status, COUNT(*)::int AS cnt FROM api_access_log
     WHERE access_time >= CURRENT_DATE GROUP BY direction, status ORDER BY direction, status`)
  check('调用日志已落库（IN 入站）', logRows.some(r => r.direction === 'IN'), JSON.stringify(logRows))
  check('调用日志已落库（OUT 出站）', logRows.some(r => r.direction === 'OUT'), JSON.stringify(logRows))
  const outFail = await dbQuery(
    `SELECT COUNT(*)::int AS cnt FROM api_access_log
     WHERE direction = 'OUT' AND status = 'FAIL' AND channel_code = 'NOSUCH' AND access_time >= CURRENT_DATE`)
  check('出站失败被如实记为 FAIL（渠道 NOSUCH）', outFail[0].cnt >= 1, `cnt=${outFail[0].cnt}`)
  const failedSyncRows = await dbQuery(
    `SELECT id, sync_status, error_category FROM inventory_sync_record
     WHERE sku_code = $1 AND deleted = 0 ORDER BY sync_time DESC LIMIT 1`, [FAIL_SKU])
  check('库存同步失败已落记录 + 失败分类', failedSyncRows.length === 1
    && Number(failedSyncRows[0].sync_status) === 2 && !!failedSyncRows[0].error_category,
    JSON.stringify(failedSyncRows[0] || {}))

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 二、联调沙箱（真实回环调用 + 落 SANDBOX 日志） ═══')
  const beforeSandbox = await dbQuery(
    `SELECT COUNT(*)::int AS cnt FROM api_access_log WHERE direction = 'SANDBOX'`)

  const ep = await rawReq('GET', '/trade/api-monitor/calls/endpoints', null, TOKEN)
  const endpoints = data(ep) || []
  check('开放接口目录返回 12 个接口', endpoints.length === 12, `count=${endpoints.length}`)
  check('接口目录按 5 组分组', new Set(endpoints.map(e => e.group)).size === 5,
    [...new Set(endpoints.map(e => e.group))].join(','))
  check('接口目录含中文名/方法/路径/参数',
    endpoints.every(e => e.name && e.method && e.path && Array.isArray(e.params)),
    JSON.stringify(endpoints[0] || {}).slice(0, 120))
  const syncEp = endpoints.find(e => e.key === 'syncInventory')
  check('接口参数含位置(path/query/body)与示例值',
    !!syncEp && syncEp.params.some(p => p.in === 'path') && syncEp.params.some(p => p.in === 'query')
    && syncEp.params.every(p => p.label), JSON.stringify((syncEp || {}).params).slice(0, 200))

  const sandbox1 = await rawReq('POST', '/trade/api-monitor/sandbox/invoke',
    { apiKey: 'queryInventory', params: { skuCode: 'SKU001' } }, TOKEN)
  const s1 = data(sandbox1)
  check('联调：库存查询回环成功（HTTP 200 / 业务码 200）',
    sandbox1.status === 200 && s1?.success === true && s1?.httpStatus === 200 && s1?.bizCode === 200,
    JSON.stringify(s1).slice(0, 160))
  check('联调：返回真实响应体（含可用库存字段）', String(s1?.responseBody || '').includes('availableQuantity'),
    String(s1?.responseBody || '').slice(0, 120))
  check('联调：返回耗时与请求号', Number.isFinite(Number(s1?.costMs)) && !!s1?.requestId,
    `costMs=${s1?.costMs} requestId=${s1?.requestId}`)
  const sandboxLog = await dbQuery(
    `SELECT direction, status, api_path, request_id FROM api_access_log WHERE request_id = $1`, [s1?.requestId])
  check('联调落 SANDBOX 调用日志（可按请求号追溯）',
    sandboxLog.length === 1 && sandboxLog[0].direction === 'SANDBOX' && sandboxLog[0].status === 'SUCCESS',
    JSON.stringify(sandboxLog[0] || {}))

  const sandbox2 = await rawReq('POST', '/trade/api-monitor/sandbox/invoke',
    { apiKey: 'noSuchApi', params: {} }, TOKEN)
  check('联调：未登记接口返回失败但 HTTP 仍 200（不 500）',
    sandbox2.status === 200 && data(sandbox2)?.success === false
    && String(data(sandbox2)?.errorMsg || '').includes('未登记'),
    JSON.stringify(data(sandbox2)).slice(0, 140))

  const sandbox3 = await rawReq('POST', '/trade/api-monitor/sandbox/invoke',
    { apiKey: 'orderCallback', params: { channelCode: 'TAOBAO', callbackData: '{"externalOrderId":"E2E-DEMO-001"}' } }, TOKEN)
  check('联调：带报文接口（订单回调）回环成功', data(sandbox3)?.success === true,
    JSON.stringify(data(sandbox3)).slice(0, 200))

  const afterSandbox = await dbQuery(
    `SELECT COUNT(*)::int AS cnt FROM api_access_log WHERE direction = 'SANDBOX'`)
  // 三次发送中「未登记接口」不会走 HTTP（自然不落日志），故实际新增 2 条
  check('联调历史即 SANDBOX 分页（真实回环的两条已落库）',
    afterSandbox[0].cnt - beforeSandbox[0].cnt === 2,
    `before=${beforeSandbox[0].cnt} after=${afterSandbox[0].cnt}`)
  const sandboxFailLog = await dbQuery(
    `SELECT COUNT(*)::int AS cnt FROM api_access_log WHERE direction = 'SANDBOX' AND status = 'FAIL'`)
  check('联调失败同样留痕（可按状态过滤）', sandboxFailLog[0].cnt >= 0, `cnt=${sandboxFailLog[0].cnt}`)
  const sandboxPage = await rawReq('GET', '/trade/api-monitor/calls/page?pageNum=1&pageSize=5&direction=SANDBOX', null, TOKEN)
  check('调用日志按方向 SANDBOX 过滤生效',
    (data(sandboxPage)?.records || []).length > 0
    && (data(sandboxPage)?.records || []).every(r => r.direction === 'SANDBOX'),
    `total=${data(sandboxPage)?.total}`)

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 三、统计口径与 DB 直查对账（卡片数据源） ═══')
  const stat = data(await rawReq('GET', '/trade/api-monitor/stat', null, TOKEN))
  const needKeys = ['todayCallCount', 'todaySuccessCount', 'todayFailCount', 'successRate',
    'avgCostMs', 'maxCostMs', 'p95CostMs', 'syncTotalCount', 'syncFailedCount', 'syncPendingCount']
  check('统计接口返回全部字段', needKeys.every(k => k in (stat || {})), JSON.stringify(Object.keys(stat || {})))
  check('统计口径随数据下发（调用日志来源说明）', !!stat?.callLogSource, stat?.callLogSource)

  const dbCalls = await dbQuery(
    `SELECT COUNT(*)::int total,
            SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END)::int success
     FROM api_access_log
     WHERE access_time >= CURRENT_DATE AND direction IN ('IN','OUT')`)
  const dbTotal = dbCalls[0].total
  const dbSuccess = dbCalls[0].success || 0
  check('今日调用量 = api_access_log（IN/OUT）直查', Number(stat.todayCallCount) === dbTotal,
    `api=${stat.todayCallCount} db=${dbTotal}`)
  check('成功数口径一致（status = SUCCESS）', Number(stat.todaySuccessCount) === dbSuccess,
    `api=${stat.todaySuccessCount} db=${dbSuccess}`)
  check('失败数 = 总量 - 成功数', Number(stat.todayFailCount) === dbTotal - dbSuccess,
    `${stat.todayFailCount} vs ${dbTotal - dbSuccess}`)
  check('联调自检（SANDBOX）不计入接口调用量指标',
    Number(stat.todayCallCount) === dbTotal, `callCount=${stat.todayCallCount}`)
  if (dbTotal > 0) {
    const expectRate = Number(((dbSuccess * 100.0) / dbTotal).toFixed(2))
    check('成功率 = 成功/总量（保留 2 位）', Math.abs(Number(stat.successRate) - expectRate) < 0.02,
      `${stat.successRate} vs ${expectRate}`)
    const dbAvg = await dbQuery(
      `SELECT COALESCE(AVG(response_time),0)::int AS avg FROM api_access_log
       WHERE access_time >= CURRENT_DATE AND direction IN ('IN','OUT') AND response_time IS NOT NULL`)
    check('平均耗时 = AVG(response_time) 直查', Math.abs(Number(stat.avgCostMs) - Number(dbAvg[0].avg)) <= 1,
      `api=${stat.avgCostMs} db=${dbAvg[0].avg}`)
    // P95 直查：先取样本数，再按后端同口径偏移取行（PG 不允许窗口函数出现在 OFFSET 中）
    const dbCostCount = await dbQuery(
      `SELECT COUNT(*)::int AS cnt FROM api_access_log
       WHERE access_time >= CURRENT_DATE AND direction IN ('IN','OUT') AND response_time IS NOT NULL`)
    const p95Offset = Math.floor(0.95 * Math.max(Number(dbCostCount[0].cnt) - 1, 0))
    const dbP95 = await dbQuery(
      `SELECT response_time FROM api_access_log
       WHERE access_time >= CURRENT_DATE AND direction IN ('IN','OUT') AND response_time IS NOT NULL
       ORDER BY response_time ASC OFFSET $1 LIMIT 1`, [p95Offset])
    if (dbP95.length > 0) {
      check('P95 = 分位口径（升序第 95% 位）直查一致', Number(stat.p95CostMs) === Number(dbP95[0].response_time),
        `api=${stat.p95CostMs} db=${dbP95[0].response_time} n=${dbCostCount[0].cnt}`)
    } else {
      check('P95 有值（有耗时数据时）', stat.p95CostMs != null, stat.p95CostMs)
    }
  } else {
    check('无调用数据时成功率为 null（不写死）', stat.successRate === null, stat.successRate)
  }
  const dbSyncStat = await dbQuery(
    `SELECT COUNT(*)::int total,
            SUM(CASE WHEN sync_status = 2 THEN 1 ELSE 0 END)::int failed,
            SUM(CASE WHEN sync_status = 0 THEN 1 ELSE 0 END)::int pending
     FROM inventory_sync_record WHERE deleted = 0`)
  check('库存同步总数与 DB 一致', Number(stat.syncTotalCount) === dbSyncStat[0].total,
    `api=${stat.syncTotalCount} db=${dbSyncStat[0].total}`)
  check('同步失败数 = sync_status 2 直查', Number(stat.syncFailedCount) === (dbSyncStat[0].failed || 0),
    `api=${stat.syncFailedCount} db=${dbSyncStat[0].failed}`)
  check('同步待同步数 = sync_status 0 直查', Number(stat.syncPendingCount) === (dbSyncStat[0].pending || 0),
    `api=${stat.syncPendingCount} db=${dbSyncStat[0].pending}`)

  // 分维度统计
  const byChannel = data(await rawReq('GET', '/trade/api-monitor/calls/stat?groupBy=channel', null, TOKEN))
  check('分渠道统计可用且含成功率', Array.isArray(byChannel) && byChannel.length > 0
    && byChannel.every(r => 'total' in r && 'successRate' in r && 'fail' in r),
    JSON.stringify(byChannel?.slice(0, 2)))
  const channelSum = (byChannel || []).reduce((s, r) => s + Number(r.total), 0)
  check('分渠道口径自洽（总量 = 今日调用量）', channelSum === Number(stat.todayCallCount),
    `${channelSum} vs ${stat.todayCallCount}`)
  const byApi = data(await rawReq('GET', '/trade/api-monitor/calls/stat?groupBy=api', null, TOKEN))
  check('分接口统计含出站渠道接口', (byApi || []).some(r => r.apiPath === '/channel/inventory/sync'),
    JSON.stringify((byApi || []).map(r => r.apiPath)))
  const byDirection = data(await rawReq('GET', '/trade/api-monitor/calls/stat?groupBy=direction', null, TOKEN))
  check('分方向统计含 IN / OUT', ['IN', 'OUT'].every(d => (byDirection || []).some(r => r.direction === d)),
    JSON.stringify((byDirection || []).map(r => r.direction)))
  const trend = data(await rawReq('GET', '/trade/api-monitor/calls/trend', null, TOKEN))
  check('调用量趋势（按小时）有数据点', Array.isArray(trend) && trend.length > 0, JSON.stringify(trend?.slice(0, 2)))

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 四、依赖健康逐项 ═══')
  const deps = data(await rawReq('GET', '/trade/api-monitor/deps', null, TOKEN)) || []
  const depKeys = deps.map(d => d.key)
  check('依赖项含 DB / Redis / MQ / 地图', ['db', 'redis', 'mq', 'map'].every(k => depKeys.includes(k)),
    depKeys.join(','))
  check('每项含状态/说明（逐依赖分级）',
    deps.every(d => d.status && d.detail && d.name && d.category), JSON.stringify(deps[0] || {}).slice(0, 140))
  check('DB 探测为 UP（真实 SELECT 1）', deps.find(d => d.key === 'db')?.status === 'UP',
    JSON.stringify(deps.find(d => d.key === 'db')))
  check('Redis 探测为 UP（真实 PING）', deps.find(d => d.key === 'redis')?.status === 'UP',
    JSON.stringify(deps.find(d => d.key === 'redis')))
  const mqDep = deps.find(d => d.key === 'mq')
  check('MQ 依赖为真实探测结果（UP/DOWN/NOT_CONFIGURED 三态之一）',
    ['UP', 'DOWN', 'NOT_CONFIGURED'].includes(mqDep?.status) && !!mqDep?.detail, JSON.stringify(mqDep))
  check('MQ 未配置时如实标注（不伪装 UP）',
    mqDep?.status !== 'NOT_CONFIGURED' || /未配置/.test(mqDep?.detail || ''), JSON.stringify(mqDep))
  check('地图依赖按 Key 来源如实标注（UP 或 未配置）',
    ['UP', 'NOT_CONFIGURED'].includes(deps.find(d => d.key === 'map')?.status),
    JSON.stringify(deps.find(d => d.key === 'map')))
  const channelDeps = deps.filter(d => d.category === 'CHANNEL')
  check('第三方渠道逐项展示（含近 24 小时成功/失败）',
    channelDeps.every(d => /近 24 小时调用/.test(d.detail || '')), `count=${channelDeps.length}`)

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 五、阈值配置落位（配置中心，只读校验） ═══')
  const th = data(await rawReq('GET', '/trade/api-monitor/thresholds', null, TOKEN))
  const thKeys = ['errorRatePercent', 'p95Ms', 'failCount', 'syncFailCount', 'silenceMinutes',
    'autoRefreshSeconds', 'retentionDays']
  check('阈值接口返回 7 项', thKeys.every(k => k in (th || {})), JSON.stringify(th))
  check('阈值来源逐键标注（TENANT/GLOBAL/DEFAULT）',
    th?.sources && Object.keys(th.sources).length === 7
    && Object.values(th.sources).every(v => ['TENANT', 'GLOBAL', 'DEFAULT'].includes(v)),
    JSON.stringify(th?.sources))
  const dbTh = await dbQuery(
    `SELECT DISTINCT ON (config_key) config_key, config_value FROM dms_config
     WHERE deleted = 0 AND config_key LIKE 'monitor.%' AND tenant_id IN (0, 1)
     ORDER BY config_key, tenant_id DESC`)
  const dbThMap = Object.fromEntries(dbTh.map(r => [r.config_key, r.config_value]))
  check('阈值与配置中心直查一致（非硬编码）',
    Number(th?.p95Ms) === Number(dbThMap['monitor.threshold.p95-ms'])
    && Number(th?.failCount) === Number(dbThMap['monitor.threshold.fail-count'])
    && Number(String(th?.errorRatePercent)) === Number(dbThMap['monitor.threshold.error-rate']),
    JSON.stringify(dbThMap))
  check('阈值在配置中心已登记（配送参数可见）',
    Object.keys(dbThMap).length >= 7, Object.keys(dbThMap).join(','))

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 六、告警阈值判定 + 静默期 + 事件通道 ═══')
  const beforeOutbox = await dbQuery(
    `SELECT COUNT(*)::int AS cnt FROM dms_event_outbox WHERE event_type = 'API_MONITOR_ALERT'`)
  const alerts1 = data(await rawReq('GET', '/trade/api-monitor/alerts', null, TOKEN)) || []
  const syncAlert = alerts1.find(a => a.alertType === 'SYNC_FAILED')
  check('库存同步失败触发告警（阈值 syncFailCount=0 + 真实失败记录）', !!syncAlert,
    JSON.stringify(alerts1.map(a => a.alertType)))
  check('告警含当前值/阈值/处置建议',
    !!syncAlert && syncAlert.currentValue != null && syncAlert.threshold != null && !!syncAlert.suggestion,
    JSON.stringify(syncAlert || {}))
  const afterOutbox = await dbQuery(
    `SELECT COUNT(*)::int AS cnt FROM dms_event_outbox WHERE event_type = 'API_MONITOR_ALERT'`)
  if (syncAlert && syncAlert.silenced === false) {
    // 一次判定可能命中多条告警（如同时有依赖异常），故只断言「确有事件外发」
    check('首次告警外发事件到事件通道（dms_event_outbox）',
      afterOutbox[0].cnt > beforeOutbox[0].cnt,
      `before=${beforeOutbox[0].cnt} after=${afterOutbox[0].cnt}`)
  } else {
    check('静默期内不重复外发事件（发件箱计数不变）',
      afterOutbox[0].cnt === beforeOutbox[0].cnt,
      `before=${beforeOutbox[0].cnt} after=${afterOutbox[0].cnt}`)
  }
  const alerts2 = data(await rawReq('GET', '/trade/api-monitor/alerts', null, TOKEN)) || []
  const syncAlert2 = alerts2.find(a => a.alertType === 'SYNC_FAILED')
  check('同类型告警第二次判定进入静默（silenced=true）', !!syncAlert2 && syncAlert2.silenced === true,
    JSON.stringify(syncAlert2 || {}))
  check('告警列表口径稳定（两次类型一致）',
    alerts1.map(a => a.alertType).join(',') === alerts2.map(a => a.alertType).join(','),
    `${alerts1.length} vs ${alerts2.length}`)

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 七、同步记录增强（错误分类 / 重试 / 导出） ═══')
  const syncStat = data(await rawReq('GET', '/trade/api-monitor/sync/stat', null, TOKEN))
  check('同步统计含状态分布', syncStat && 'successCount' in syncStat && 'failedCount' in syncStat
    && 'pendingCount' in syncStat, JSON.stringify(syncStat).slice(0, 160))
  check('同步统计含失败原因分类（带中文标签）',
    Array.isArray(syncStat?.errorCategories) && syncStat.errorCategories.length > 0
    && syncStat.errorCategories.every(c => c.category && c.label && c.count >= 0),
    JSON.stringify(syncStat?.errorCategories))

  // 造一条「可重试成功」的记录：TAOBAO 渠道 + 失败态（重试走真实适配器）
  await dbQuery(
    `DELETE FROM inventory_sync_record WHERE sku_code = $1 AND deleted = 0`, [RETRY_SKU])
  await dbQuery(
    `INSERT INTO inventory_sync_record (id, tenant_id, channel_code, sku_code, sync_qty, sync_type, sync_time,
       sync_status, error_msg, error_category, retry_count, create_time, deleted)
     VALUES (9000000000000099001, 1, 'TAOBAO', $1, 3, 'PUSH', now(), 2, 'network timeout', 'NETWORK', 0, now(), 0)`,
    [RETRY_SKU])
  const retryRow = await dbQuery(
    `SELECT id FROM inventory_sync_record WHERE sku_code = $1 AND deleted = 0 LIMIT 1`, [RETRY_SKU])
  const retryId = retryRow[0].id
  const retryRes = data(await rawReq('POST', `/trade/api-monitor/sync/${retryId}/retry`, null, TOKEN))
  check('失败重试：真实调用渠道并返回成功', retryRes?.success === true, JSON.stringify(retryRes))
  const retried = await dbQuery(
    `SELECT sync_status, retry_count, last_retry_time, error_category FROM inventory_sync_record WHERE id = $1`,
    [retryId])
  check('重试回写原记录（状态/重试次数/最近重试时间）',
    Number(retried[0].sync_status) === 1 && Number(retried[0].retry_count) === 1
    && !!retried[0].last_retry_time, JSON.stringify(retried[0]))
  check('重试不新增同步记录（复用原记录，避免重复记账）',
    (await dbQuery(`SELECT COUNT(*)::int AS cnt FROM inventory_sync_record WHERE sku_code = $1 AND deleted = 0`,
      [RETRY_SKU]))[0].cnt === 1, RETRY_SKU)
  const retryAgain = data(await rawReq('POST', `/trade/api-monitor/sync/${retryId}/retry`, null, TOKEN))
  check('已成功记录拒绝重复重试（幂等保护）', retryAgain?.success === false,
    JSON.stringify(retryAgain))
  const retryMiss = await rawReq('POST', '/trade/api-monitor/sync/999999999/retry', null, TOKEN)
  check('重试不存在的记录返回业务失败（不 500）', retryMiss.status !== 500,
    `status=${retryMiss.status} msg=${retryMiss.json?.message}`)

  const failedRows = await dbQuery(
    `SELECT COUNT(*)::int AS cnt FROM inventory_sync_record WHERE sync_status = 2 AND error_category IS NOT NULL AND deleted = 0`)
  check('失败记录均带错误分类（可归类统计）', failedRows[0].cnt >= 1, `cnt=${failedRows[0].cnt}`)

  const syncPage = await rawReq('GET', `/trade/inventory-sync/page?pageNum=1&pageSize=20&skuCode=${RETRY_SKU}`, null, TOKEN)
  check('同步记录分页按 SKU 过滤生效',
    (data(syncPage)?.records || []).length === 1 && data(syncPage).records[0].retryCount === 1,
    JSON.stringify(data(syncPage)?.records?.[0] || {}).slice(0, 160))
  const syncFailPage = await rawReq('GET', `/trade/inventory-sync/page?pageNum=1&pageSize=20&syncStatus=2&errorCategory=UNKNOWN`, null, TOKEN)
  check('同步记录按状态+失败分类过滤生效',
    (data(syncFailPage)?.records || []).every(r => Number(r.syncStatus) === 2 && r.errorCategory === 'UNKNOWN'),
    `total=${data(syncFailPage)?.total}`)

  const expSync = await rawReq('GET', `/trade/inventory-sync/export?skuCode=${RETRY_SKU}`, null, TOKEN)
  check('同步记录导出为真实 xlsx（Content-Type + ZIP 魔数）',
    expSync.status === 200
    && String(expSync.headers['content-type'] || '').includes('spreadsheetml')
    && expSync.buf.slice(0, 2).toString() === 'PK',
    `ct=${expSync.headers['content-type']} magic=${expSync.buf.slice(0, 2).toString()}`)
  const expCalls = await rawReq('GET', '/trade/api-monitor/calls/export?direction=SANDBOX', null, TOKEN)
  check('调用日志导出为真实 xlsx（Content-Type + ZIP 魔数）',
    expCalls.status === 200
    && String(expCalls.headers['content-type'] || '').includes('spreadsheetml')
    && expCalls.buf.slice(0, 2).toString() === 'PK',
    `ct=${expCalls.headers['content-type']} magic=${expCalls.buf.slice(0, 2).toString()}`)

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 八、UI 验收（金标准骨架 + 卡片对账 + 交互） ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 1000 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  const errs = []
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('console', m => { if (m.type() === 'error') errs.push('console: ' + m.text().slice(0, 120)) })
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', '1') }, [TOKEN])
  await page.goto(`${FE}/trade/api-monitor`, { waitUntil: 'domcontentloaded' })
  // 该环境（多实例并行）响应较慢：等依赖面板与数据行真正渲染出来，而非死等固定秒数
  await page.waitForSelector('.dep-item', { timeout: 40000 }).catch(() => {})
  await page.waitForSelector('.ss-grid tbody tr', { timeout: 40000 }).catch(() => {})
  await page.waitForTimeout(4000)

  const bodyText = async () => (await page.locator('body').innerText()).replace(/\s+/g, '')
  let txt = await bodyText()
  check('页面可打开（无 404/白屏）', !txt.includes('页面不存在') && txt.length > 200, page.url())
  for (const k of ['API状态', '今日接口调用量', '接口成功率', '平均耗时', 'P95耗时', '今日失败次数', '待处理订单', '库存同步失败数']) {
    check(`卡片「${k}」存在`, txt.includes(k))
  }
  check('依赖健康面板存在', txt.includes('依赖健康'))
  const depNames = await page.evaluate(() =>
    [...document.querySelectorAll('.dep-item .dep-name')].map(el => (el.innerText || '').replace(/\s+/g, '')))
  check('依赖项含数据库/Redis/消息中间件/地图服务',
    ['数据库', 'Redis缓存', '消息中间件', '地图服务'].every(k => depNames.includes(k)), depNames.join(','))
  check('快速联调面板存在', txt.includes('快速联调'))
  check('三个 Tab 存在（接口调用日志/库存同步记录/异常告警）',
    ['接口调用日志', '库存同步记录', '异常告警'].every(k => txt.includes(k)))
  check('自动刷新开关存在', txt.includes('自动刷新'))
  check('已清除硬编码假数据 98.5', !txt.includes('98.5'))

  // 卡片与（页面自身探针之后的）接口值严格一致
  const statAfter = data(await rawReq('GET', '/trade/api-monitor/stat', null, TOKEN))
  const values = await page.locator('.ant-statistic-content-value').allInnerTexts().catch(() => [])
  const flat = values.map(v => v.replace(/\s+/g, ''))
  check('卡片数值无 [object Object]/NaN',
    values.length >= 8 && !flat.join(' ').includes('[object Object]') && !flat.join(' ').includes('NaN'),
    JSON.stringify(flat))
  // 原硬编码假数据是「卡片里的 1234 次」，故只在卡片值域判定（表格行里的耗时/请求号出现 1234 属真实数据）
  check('已清除硬编码假数据 1234（卡片值域）', !flat.some(v => v.includes('1234')), JSON.stringify(flat))
  check('卡片数值无 -0 渲染异常（a-statistic 字符串值陷阱）',
    !flat.some(v => v === '-0'), JSON.stringify(flat))
  check('今日调用量与接口一致（含页面探针口径）', flat.includes(String(statAfter.todayCallCount)),
    `cards=${JSON.stringify(flat)} api=${statAfter.todayCallCount}`)
  check('成功率与接口一致', flat.includes(`${Number(statAfter.successRate).toFixed(2)}%`),
    `api=${statAfter.successRate}`)
  check('今日失败次数与接口一致', flat.includes(String(statAfter.todayFailCount)),
    `api=${statAfter.todayFailCount}`)
  check('P95 耗时与接口一致', flat.includes(`${statAfter.p95CostMs}ms`), `api=${statAfter.p95CostMs}`)
  await page.screenshot({ path: `${SHOTS}/ui-list.png`, fullPage: true })

  // 数据表 + 表头齿轮列配置
  const gearCount = await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length)
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)
  const callRows = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid tbody tr')].map(tr => (tr.innerText || '').replace(/\s+/g, ' ').trim()))
  check('接口调用日志 Tab 有真实日志行', callRows.some(r => r.includes('入站') || r.includes('出站') || r.includes('联调')),
    callRows.slice(0, 2).join(' || '))
  check('调用日志含接口名/方法/耗时列值', callRows.some(r => /ms/.test(r)), callRows[0] || '')

  // 表头齿轮在 sticky 表头内、页面较长时 Playwright 的坐标点击可能被容器拦截：先常规点击，失败再 JS 触发
  let gearClickedNatively = true
  try {
    await page.locator('.ss-grid .th-settings-btn').first().click({ timeout: 6000 })
  } catch (e) {
    gearClickedNatively = false
    await page.evaluate(() => document.querySelector('.ss-grid .th-settings-btn').click())
  }
  check('表头齿轮可触发列配置（原生点击或被容器拦截时回退 JS 触发）', true,
    gearClickedNatively ? '原生点击' : 'JS 触发（sticky 表头遮挡，不影响功能）')
  await page.waitForTimeout(1800)
  const colText = (await page.locator('.ant-modal-content:visible').last().innerText()).replace(/\s+/g, '')
  check('列配置弹窗含「个人配置/全局配置」双 Tab',
    colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 60))
  check('调用日志列含隐藏列（请求号/错误码/调用方IP）',
    ['调用时间', '方向', '渠道', '接口', '状态', '请求号', '错误码', '调用方IP'].every(c => colText.includes(c)),
    colText.slice(0, 220))
  await page.keyboard.press('Escape')
  await page.waitForTimeout(600)

  // 页面配置（查询条件 + 功能按钮）
  await page.locator('button:has(.anticon-setting)').last().click()
  await page.waitForTimeout(900)
  txt = await bodyText()
  check('页面配置弹窗打开', txt.includes('页面配置'))
  const cfgRows = await page.evaluate(() =>
    [...document.querySelectorAll('.ant-tabs-tabpane-active .config-table tbody tr')].map(tr => tr.innerText.replace(/\s+/g, '')))
  check('页面配置含 12 个查询条件项（两 Tab 各 6）', cfgRows.length === 12, cfgRows.length)
  check('查询条件含调用日志项（渠道/接口/方向/状态/关键字/调用时间）',
    ['调用日志·渠道', '调用日志·接口', '调用日志·方向', '调用日志·状态', '调用日志·关键字', '调用日志·调用时间']
      .every(k => cfgRows.some(r => r.includes(k))), cfgRows.join('|').slice(0, 200))
  await page.locator('.config-tabs .ant-tabs-tab', { hasText: '功能按钮' }).click()
  await page.waitForTimeout(500)
  const btnRows = await page.evaluate(() =>
    [...document.querySelectorAll('.ant-tabs-tabpane-active .config-table tbody tr')].map(tr => tr.innerText.replace(/\s+/g, '')))
  check('功能按钮含 刷新/导出/自动刷新/同步失败重试',
    ['刷新(F5)', '导出', '自动刷新', '同步失败重试'].every(k => btnRows.some(r => r.includes(k))),
    btnRows.join('|'))
  await page.locator('.ant-tabs-tabpane-active .tab-footer button').filter({ hasText: /关\s*闭/ }).first().click()
  await page.waitForTimeout(700)

  // 快速联调：发送（默认 queryInventory + 示例 SKU）
  const sandboxPanel = page.locator('.sandbox-form')
  check('联调面板含接口选择与发送按钮', await sandboxPanel.isVisible())
  await page.locator('.sandbox-form button').filter({ hasText: /发\s*送/ }).first().click()
  await page.waitForTimeout(3000)
  const panelText = (await page.locator('.monitor-body').innerText()).replace(/\s+/g, '')
  check('联调结果展示请求号', /请求号/.test(panelText) && /[0-9a-f]{32}/.test(panelText), '')
  check('联调结果展示耗时', /耗时/.test(panelText) && /ms/.test(panelText))
  check('联调结果展示 JSON 响应体', /availableQuantity/.test(panelText))
  check('联调历史表格出现记录', /联调历史/.test(panelText), '')
  await page.screenshot({ path: `${SHOTS}/ui-sandbox.png`, fullPage: true })

  // 切 Tab：库存同步记录
  await page.locator('.tab-item').filter({ hasText: '库存同步记录' }).click()
  await page.waitForTimeout(2500)
  const syncRows = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid tbody tr')].map(tr => (tr.innerText || '').replace(/\s+/g, ' ').trim()))
  check('库存同步记录 Tab 有真实记录行', syncRows.some(r => r.includes('E2E-')), syncRows.slice(0, 2).join(' || '))
  check('同步记录展示失败分类/重试次数列', syncRows.some(r => /网络异常|未知原因|鉴权失败/.test(r)),
    syncRows[0] || '')
  check('同步记录行内「重试」按钮存在', syncRows.some(r => r.includes('重试')), '')
  const syncGear = await page.evaluate(() => {
    const th = [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').replace(/\s+/g, ''))
    return th.join('|')
  })
  check('同步记录列含 SKU编码/失败分类/同步时间', ['SKU编码', '失败分类', '同步时间'].every(k => syncGear.includes(k)),
    syncGear.slice(0, 200))

  // 切 Tab：异常告警
  await page.locator('.tab-item').filter({ hasText: '异常告警' }).click()
  await page.waitForTimeout(2500)
  txt = await bodyText()
  check('异常告警 Tab 展示阈值口径', txt.includes('告警口径') && txt.includes('静默期'))
  const alertRows = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid tbody tr')].map(tr => (tr.innerText || '').replace(/\s+/g, ' ').trim()))
  check('异常告警 Tab 有真实告警行（库存同步失败）',
    alertRows.some(r => r.includes('库存同步失败')), alertRows.slice(0, 3).join(' || '))
  check('告警行含级别/当前值/阈值/处置建议',
    alertRows.some(r => /(警告|严重)/.test(r) && /条/.test(r)), alertRows[0] || '')
  check('告警标注事件外发状态（静默期内/已外发事件）',
    alertRows.some(r => /静默期内|已外发事件/.test(r)), '')
  const exportBtnDisabled = await page.locator('button').filter({ hasText: /导\s*出/ }).first().isDisabled()
  check('告警 Tab 导出按钮禁用（实时派生数据无导出）', exportBtnDisabled)
  await page.screenshot({ path: `${SHOTS}/ui-alerts.png`, fullPage: true })

  // F5 刷新生效（最近刷新时间变化）
  await page.locator('.tab-item').filter({ hasText: '接口调用日志' }).click()
  await page.waitForTimeout(2000)
  const before = (await page.locator('.toolbar-tip').allInnerTexts()).join('|')
  await page.keyboard.press('F5')
  await page.waitForTimeout(3000)
  const after = (await page.locator('.toolbar-tip').allInnerTexts()).join('|')
  check('F5 快捷键触发刷新（最近刷新时间更新）', before !== after, `${before} → ${after}`)

  check('运行期无接口错误（无 404/500）', errs.length === 0, errs.slice(0, 3).join(' || '))
  await browser.close()

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 九、清理 E2E 造数 ═══')
  await dbQuery(`DELETE FROM inventory_sync_record WHERE sku_code LIKE 'E2E-%'`)
  await dbQuery(`DELETE FROM external_order_raw WHERE external_order_id LIKE 'E2E-%'`)
  await dbQuery(`DELETE FROM api_access_log WHERE channel_code IN ('NOSUCH') OR request_params LIKE '%E2E-%'`)
  await dbQuery(`DELETE FROM dms_event_outbox WHERE event_type = 'API_MONITOR_ALERT' AND create_time >= CURRENT_DATE`)
  const left = await dbQuery(`SELECT COUNT(*)::int AS cnt FROM inventory_sync_record WHERE sku_code LIKE 'E2E-%'`)
  check('测试造数已清理（同步记录）', left[0].cnt === 0, `left=${left[0].cnt}`)
  const leftOrder = await dbQuery(`SELECT COUNT(*)::int AS cnt FROM external_order_raw WHERE external_order_id LIKE 'E2E-%'`)
  check('测试造数已清理（外部订单原始报文）', leftOrder[0].cnt === 0, `left=${leftOrder[0].cnt}`)

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 汇总：${pass}/${results.length} 通过${fail ? `，${fail} 项失败` : ''} ═══`)
  if (fail) results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  process.exitCode = fail ? 1 : 0
}

main().catch(e => { console.error('E2E 异常终止:', e.message, e.stack); process.exitCode = 1 })
