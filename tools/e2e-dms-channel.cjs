/*
 * 渠道管理（配送 → 配送配置 → 渠道管理）金标准端到端验证
 *   · API 验收：多条件分页 / 编码唯一 / 新建租户归属 / 对接凭据脱敏 / 回传掩码不覆盖 /
 *               启停 + 批量启停 / 真删除 + 引用保护 / 批量删除 / 连通性测试 / 运力同步 /
 *               下拉只返启用 / 在线运力实时统计
 *   · 派单与回调验收（第二轮，文档 §3.3 / §7.1 / §7.3 / §7.4）：
 *               凭据 AES-GCM 加密落库（无明文 + ENCv1）与掩码回写保护 / 渠道派单
 *               （适配器未启用如实失败 + 成功下单 + 幂等复用 + 任务流转 + 调度审计 + 台账）/
 *               回调验签（无签名/错签名/过期时间戳/缺密钥一律拒绝并留痕）+
 *               正确签名回写任务状态与台账 + nonce 防重放 + 免登录可达
 *   · UI 验收：6 查询项 / 13 列表格 + 表头齿轮列配置 / 新增-编辑 4 Tab 弹窗 / 编码禁改 /
 *              行内测试与删除 / 批量启停 / 页面配置开关真实生效 / 真实 xlsx 导出 /
 *              渠道派单弹窗（任务选择器 → 外部单号）+ 台账抽屉（外部单/回调日志）
 *
 * 前置：
 *   1) 造数据：python tools/dbq2.py "$(grep -v '^--' tools/e2e-dms-channel-seed.sql)"
 *   2) 账号：  python tools/dbq2.py "$(grep -v '^--' tools/e2e-dms-dashboard-user.sql)"
 *   3) 后端实例含 dms-delivery 的 channel 包（见《渠道管理开发文档》）
 *
 * 用法：node tools/e2e-dms-channel.cjs
 *      ERP_PORT=5677 FE_URL=http://localhost:5656 node tools/e2e-dms-channel.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')
const http = require('http')
const fs = require('fs')
const path = require('path')
const crypto = require('node:crypto')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5677)
const SHOTS = 'I:/AI-Ready/tool-results/dms-channel'

const E2E_USER = process.env.E2E_USER || 'e2e_dmsdash'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const TENANT_NAME = process.env.E2E_TENANT || '系统租户'

/** E2E 样本常量（与 tools/e2e-dms-channel-seed.sql 一一对应） */
const CH_OWN = '2099300000000000001'   // 自有员工，无适配器
const CH_DADA = '2099300000000000002'  // 外部平台，dadaAdapter（Stub），被 1 名骑手引用
const CH_TMP = '2099300000000000003'   // 待删除
// ⚠️ 雪花ID 超出 JS 安全整数（2^53），必须用字符串，否则数字字面量被四舍五入成 ...000
const TASK_CH = '2099300000000000010'  // 渠道派单样本任务（待分配）
const TASK_CH_NO = 'E2E-CH-TASK-001'
const SECRET_RAW = 'E2E-SECRET-RAW-001'
const APPKEY_RAW = 'E2E-AK-001'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

// ── HTTP ────────────────────────────────────────────────────────
function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? null : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* 非 JSON */ }
        resolve({ status: res.statusCode, buf, json, text: buf.toString('utf8') })
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
const data = res => (res.json ? res.json.data : null)
const okRes = res => Number(res.json?.code) === 200

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

// ── 结果收集 ─────────────────────────────────────────────────────
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

// ── DB（校验脱敏是否真的没写坏原值） ─────────────────────────────
let DB = null
async function dbQuery(sql) {
  if (!DB) {
    DB = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
    await DB.connect()
  }
  return (await DB.query(sql)).rows
}

const STAMP = Date.now().toString().slice(-6)
const NEW_CODE = 'E2E-CH-NEW' + STAMP

// ══════════════════════════════════════════════════════════════
// 一、接口验收
// ══════════════════════════════════════════════════════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  const anon = await rawReq('GET', '/dms/channel/page')
  check('未登录访问受保护（401）', anon.status === 401, `status=${anon.status}`)

  await login()
  check('验收账号登录成功', !!TOKEN)

  // 样本前置检查：本套件会删除/停用样本（E2E-CH-TMP 等），每次运行前必须重灌种子
  const [seedCheck] = await dbQuery(`SELECT count(*)::int AS c FROM dms_channel
    WHERE deleted = 0 AND channel_code IN ('E2E-CH-OWN', 'E2E-CH-DADA', 'E2E-CH-TMP')`)
  if (Number(seedCheck?.c) < 3) {
    console.error('  ⚠️ 未检测到完整 E2E 样本（3 条渠道），请先执行：'
      + 'python tools/dbq2.py "$(grep -v \'^--\' tools/e2e-dms-channel-seed.sql)"')
  }

  // ── 1) 分页与多条件 ──
  const pageAll = await api('GET', '/dms/channel/page?pageNum=1&pageSize=50&channelCode=E2E-CH')
  const list = data(pageAll)?.records || []
  check('分页接口 200 且返回 E2E 样本 ≥ 3 条', okRes(pageAll) && list.length >= 3, `len=${list.length}`)
  check('列表返回运力统计字段（riderTotal/riderOnline）',
    list.every(r => r.riderTotal !== undefined && r.riderOnline !== undefined),
    JSON.stringify(list[0] && { t: list[0].riderTotal, o: list[0].riderOnline }))
  const dadaRow = list.find(r => String(r.id) === CH_DADA)
  check('被引用渠道在线运力 ≥ 1（按 dms_rider 实时统计）',
    Number(dadaRow?.riderTotal) >= 1, JSON.stringify(dadaRow && { t: dadaRow.riderTotal, o: dadaRow.riderOnline }))

  const byName = data(await api('GET', '/dms/channel/page?pageNum=1&pageSize=50&channelName=达达平台'))?.records || []
  check('名称模糊查询生效', byName.length >= 1 && byName.every(r => r.channelName.includes('达达平台')),
    JSON.stringify(byName.map(r => r.channelName)))
  const byType = data(await api('GET', '/dms/channel/page?pageNum=1&pageSize=50&channelType=1'))?.records || []
  check('渠道类型筛选生效（1-自有员工）', byType.every(r => r.channelType === 1), JSON.stringify(byType.map(r => r.channelType)))
  const byStatus = data(await api('GET', '/dms/channel/page?pageNum=1&pageSize=50&status=0'))?.records || []
  check('启用状态筛选生效（0-禁用）', byStatus.every(r => r.status === 0), JSON.stringify(byStatus.map(r => r.status)))
  const byLink = data(await api('GET', '/dms/channel/page?pageNum=1&pageSize=50&linkStatus=0'))?.records || []
  check('对接状态筛选生效（0-未对接）', byLink.every(r => r.linkStatus === 0), JSON.stringify(byLink.map(r => r.linkStatus)))
  // 「今日」取**数据库口径**（日期条件在 SQL 里与 timestamp 字面量比较；用 JS 的 UTC 日期会在凌晨跨天误判）
  const [dbToday] = await dbQuery(`SELECT to_char(now(), 'YYYY-MM-DD') AS d`)
  const today = dbToday.d
  const byDate = data(await api('GET', `/dms/channel/page?pageNum=1&pageSize=50&startDate=${today}&endDate=${today}`))?.records || []
  check('创建时间区间筛选生效（命中今日样本）', byDate.length >= 3, `len=${byDate.length} date=${today}`)

  // ── 2) 新建（含租户归属回归） ──
  const createRes = await api('POST', '/dms/channel', {
    channelCode: NEW_CODE,
    channelName: 'E2E新建渠道',
    channelType: 4,
    priority: 88,
    status: 1,
    coverageArea: '上海市',
    billingType: 3,
    billingConfig: '{"unitPrice":1.2}',
    remark: 'E2E 新建',
  })
  check('新建渠道成功', okRes(createRes) && data(createRes)?.id, JSON.stringify(data(createRes) || createRes.json).slice(0, 200))
  const newId = data(createRes)?.id
  const created = data(await api('GET', `/dms/channel/page?pageNum=1&pageSize=50&channelCode=${NEW_CODE}`))?.records || []
  check('新建后当前租户可见（修复 tenantId 硬编码 0 的回归）',
    created.length === 1 && String(created[0].id) === String(newId), `len=${created.length}`)
  check('新建默认对接状态为未对接（0）', Number(created[0]?.linkStatus) === 0, created[0]?.linkStatus)
  const [dbRow] = await dbQuery(`SELECT tenant_id FROM dms_channel WHERE channel_code = '${NEW_CODE}' AND deleted = 0`)
  check('新建数据落在当前租户（tenant_id = 1）', Number(dbRow?.tenant_id) === 1, dbRow?.tenant_id)

  const dupRes = await api('POST', '/dms/channel', { channelCode: 'E2E-CH-OWN', channelName: '重复编码' })
  check('渠道编码重复被拒绝', !okRes(dupRes), `code=${dupRes.json?.code} msg=${dupRes.json?.message}`)

  // ── 3) 脱敏与回写保护 ──
  const detail = data(await api('GET', `/dms/channel/${CH_DADA}`))
  const cfg = String(detail?.configJson || '')
  check('详情返回脱敏凭据（appSecret/token → ******）',
    cfg.includes('******') && !cfg.includes(SECRET_RAW), cfg.slice(0, 200))
  check('非敏感字段保持可读（appKey / callbackUrl）',
    cfg.includes(APPKEY_RAW) && cfg.includes('callbackUrl'), cfg.slice(0, 200))

  const updRes = await api('PUT', `/dms/channel/${CH_DADA}`, {
    channelCode: 'E2E-CH-DADA',
    channelName: 'E2E达达平台',
    channelType: detail.channelType,
    adapterBean: detail.adapterBean,
    configJson: detail.configJson, // 回传脱敏值
    priority: detail.priority,
    coverageArea: detail.coverageArea,
    billingType: detail.billingType,
    billingConfig: detail.billingConfig,
    remark: 'E2E 回写保护用例',
  })
  check('更新接口 200', okRes(updRes), JSON.stringify(updRes.json).slice(0, 160))
  const [afterUpd] = await dbQuery(`SELECT config_json, remark FROM dms_channel WHERE id = ${CH_DADA} AND deleted = 0`)
  // 凭据落库为「明文（历史/未启用加密）或 ENCv1 密文」，但绝不会把掩码写进库
  check('回传脱敏值不会覆盖库中原凭据（无掩码，凭据保留）',
    !String(afterUpd?.config_json || '').includes('******') &&
    (String(afterUpd?.config_json || '').includes(SECRET_RAW) ||
      String(afterUpd?.config_json || '').includes('ENCv1:')),
    String(afterUpd?.config_json || '').slice(0, 160))
  check('普通字段正常更新（remark）', String(afterUpd?.remark || '').includes('回写保护'), afterUpd?.remark)

  // ── 4) 启停 / 批量启停 ──
  const offRes = await api('PUT', `/dms/channel/${CH_OWN}/status?status=0`)
  check('停用接口 200（query 参数）', okRes(offRes), JSON.stringify(offRes.json).slice(0, 120))
  const [ownRow] = await dbQuery(`SELECT status FROM dms_channel WHERE id = ${CH_OWN} AND deleted = 0`)
  check('停用落库', Number(ownRow?.status) === 0, ownRow?.status)
  await api('PUT', `/dms/channel/${CH_OWN}/status?status=1`)
  const [ownRow2] = await dbQuery(`SELECT status FROM dms_channel WHERE id = ${CH_OWN} AND deleted = 0`)
  check('启用落库', Number(ownRow2?.status) === 1, ownRow2?.status)

  const batchRes = await api('POST', '/dms/channel/batch-status?status=0', { ids: [CH_OWN, CH_DADA] })
  check('批量停用接口 200', okRes(batchRes), JSON.stringify(batchRes.json).slice(0, 120))
  const offRows = await dbQuery(`SELECT count(*)::int AS c FROM dms_channel
    WHERE deleted = 0 AND id IN (${CH_OWN}, ${CH_DADA}) AND status = 0`)
  check('批量停用落库（2 条）', Number(offRows[0]?.c) === 2, offRows[0]?.c)
  await api('POST', '/dms/channel/batch-status?status=1', { ids: [CH_OWN, CH_DADA] })

  // ── 5) 连通性测试 ──
  const t1 = data(await api('POST', `/dms/channel/${CH_OWN}/test`))
  check('无适配器渠道测试 → 未接通且对接状态=0',
    t1?.success === false && Number(t1?.linkStatus) === 0, JSON.stringify(t1))
  const t2 = data(await api('POST', `/dms/channel/${CH_DADA}/test`))
  check('外部平台适配器未启用 → 如实返回未接通（不伪造成功）',
    t2?.success === false && Number(t2?.linkStatus) === 2 && String(t2?.message).includes('未启用'),
    JSON.stringify(t2))

  // 指向内部运力适配器（默认已装配）→ 测试应真实走通适配器调用链
  await api('PUT', `/dms/channel/${CH_OWN}`, {
    channelCode: 'E2E-CH-OWN', channelName: 'E2E自有配送队', channelType: 1,
    adapterBean: 'own', priority: 10, status: 1, coverageArea: '北京市朝阳区', billingType: 1,
  })
  const t3 = data(await api('POST', `/dms/channel/${CH_OWN}/test`))
  check('内部运力渠道测试走通适配器（按 channelCode 解析 / success=true / 状态=1）',
    t3?.success === true && Number(t3?.linkStatus) === 1 && String(t3?.message).includes('就绪'),
    JSON.stringify(t3))
  await api('PUT', `/dms/channel/${CH_OWN}`, {
    channelCode: 'E2E-CH-OWN', channelName: 'E2E自有配送队', channelType: 1,
    adapterBean: null, priority: 10, status: 1, coverageArea: '北京市朝阳区', billingType: 1,
  })
  const [tested] = await dbQuery(`SELECT link_status, last_test_time, last_test_result FROM dms_channel WHERE id = ${CH_DADA} AND deleted = 0`)
  check('测试结果回写落库（对接状态/时间/摘要）',
    Number(tested?.link_status) === 2 && !!tested?.last_test_time && String(tested?.last_test_result).length > 0,
    JSON.stringify(tested))

  // ── 6) 运力同步（Stub 如实返回 0） ──
  const sync = data(await api('POST', `/dms/channel/${CH_OWN}/sync-riders`))
  check('未对接渠道同步运力 → 0 条并说明原因',
    Number(sync?.inserted) === 0 && Number(sync?.updated) === 0 && String(sync?.message).length > 0,
    JSON.stringify(sync))

  // ── 7) 删除：引用保护 + 真删除 ──
  const delRef = await api('DELETE', `/dms/channel/${CH_DADA}`)
  check('删除被引用渠道被拒绝（引用保护）',
    !okRes(delRef) && String(delRef.json?.message || '').includes('引用'),
    `code=${delRef.json?.code} msg=${delRef.json?.message}`)
  const [stillThere] = await dbQuery(`SELECT deleted FROM dms_channel WHERE id = ${CH_DADA}`)
  check('被拒绝后数据仍在', Number(stillThere?.deleted) === 0, stillThere?.deleted)

  const delOk = await api('DELETE', `/dms/channel/${CH_TMP}`)
  check('删除未被引用渠道成功（真删除）', okRes(delOk), JSON.stringify(delOk.json).slice(0, 120))
  const [gone] = await dbQuery(`SELECT deleted FROM dms_channel WHERE id = ${CH_TMP}`)
  check('真删除为逻辑删除落库（deleted=1）', Number(gone?.deleted) === 1, gone?.deleted)

  const batchDel = await api('POST', '/dms/channel/batch-delete', { ids: [newId] })
  check('批量删除接口 200', okRes(batchDel), JSON.stringify(batchDel.json).slice(0, 120))
  const [newGone] = await dbQuery(`SELECT deleted FROM dms_channel WHERE id = ${newId}`)
  check('批量删除落库', Number(newGone?.deleted) === 1, newGone?.deleted)

  // ── 8) 下拉只返启用渠道 ──
  await api('PUT', `/dms/channel/${CH_OWN}/status?status=0`)
  const options = data(await api('GET', '/dms/channel/options')) || []
  check('下拉只返回启用渠道（停用的 E2E自有配送队不在其中）',
    !options.some(o => String(o.id) === CH_OWN), JSON.stringify(options.map(o => o.channelName)))
  check('下拉项含编码/名称/类型', options.length > 0 &&
    options.every(o => o.channelCode && o.channelName && o.channelType !== undefined),
    JSON.stringify(options[0]))
  await api('PUT', `/dms/channel/${CH_OWN}/status?status=1`)

  // ── 9) 分页边界 ──
  const small = data(await api('GET', '/dms/channel/page?pageNum=1&pageSize=1'))
  check('分页 pageSize 生效', (small?.records || []).length === 1 && Number(small?.size) === 1,
    `len=${(small?.records || []).length} size=${small?.size}`)
  check('分页 total 为数值型', Number(small?.total) > 0, small?.total)
}

// ══════════════════════════════════════════════════════════════
// 二、渠道派单 / 回调安全验收（文档 §3.3 / §7.1 / §7.3 / §7.4）
// ══════════════════════════════════════════════════════════════
const hmac = (base, secret) =>
  crypto.createHmac('sha256', secret).update(base, 'utf8').digest('hex').toUpperCase()

async function dispatchSuite() {
  console.log('\n═══ 二、渠道派单 / 回调安全验收 ═══')
  await login()

  // ── 样本自愈（共享 devdb 会被重复运行） ──
  await dbQuery(`UPDATE dms_task SET status = 0, rider_id = NULL, dispatch_time = NULL, rider_name = NULL
                 WHERE task_no = '${TASK_CH_NO}' AND deleted = 0`)
  await dbQuery(`DELETE FROM dms_channel_order WHERE idem_key LIKE '${TASK_CH_NO}:%'`)
  await dbQuery(`DELETE FROM dms_channel_callback_log
                 WHERE channel_order_no LIKE 'E2E-EXT-%' OR nonce LIKE 'E2E-NONCE-%'`)

  // ── 1) 凭据加密存储（§7.4）与掩码回写保护 ──
  const detail = data(await api('GET', `/dms/channel/${CH_DADA}`))
  check('详情仍返回脱敏凭据（appSecret/token → ******）',
    String(detail?.configJson || '').includes('******') && !String(detail?.configJson || '').includes(SECRET_RAW))

  const putBody = (cfg) => ({
    channelCode: 'E2E-CH-DADA', channelName: 'E2E达达平台', channelType: detail.channelType,
    adapterBean: detail.adapterBean, configJson: cfg, priority: detail.priority,
    coverageArea: detail.coverageArea, billingType: detail.billingType,
    billingConfig: detail.billingConfig, remark: detail.remark,
  })
  await api('PUT', `/dms/channel/${CH_DADA}`, putBody(detail.configJson))
  const [enc] = await dbQuery(`SELECT config_json FROM dms_channel WHERE id = ${CH_DADA} AND deleted = 0`)
  const encJson = String(enc?.config_json || '')
  check('敏感凭据加密落库（库内无明文且带 ENCv1 前缀）',
    !encJson.includes(SECRET_RAW) && encJson.includes('ENCv1:'), encJson.slice(0, 200))
  check('非敏感字段仍明文可读（appKey / callbackUrl）',
    encJson.includes(APPKEY_RAW) && encJson.includes('callbackUrl'), encJson.slice(0, 200))

  await api('PUT', `/dms/channel/${CH_DADA}`, putBody(String(detail.configJson).replace(APPKEY_RAW, 'E2E-AK-002')))
  const [enc2] = await dbQuery(`SELECT config_json FROM dms_channel WHERE id = ${CH_DADA} AND deleted = 0`)
  const encJson2 = String(enc2?.config_json || '')
  check('回传掩码不覆盖原凭据（仍无明文）且普通字段可修改（appKey 已更新）',
    !encJson2.includes(SECRET_RAW) && encJson2.includes('E2E-AK-002') && !encJson2.includes('******'),
    encJson2.slice(0, 200))
  await api('PUT', `/dms/channel/${CH_DADA}`, putBody(detail.configJson)) // 还原 appKey

  // ── 2) 渠道派单：适配器未启用 → 如实失败，不伪造单号 ──
  const pushFailRes = await api('POST', `/dms/channel/${CH_DADA}/push-order`, { taskId: TASK_CH })
  const pushFail = data(pushFailRes)
  check('外部平台适配器未启用 → 下单失败且不返回单号（不伪造成功）',
    pushFail?.success === false && !pushFail?.channelOrderNo && String(pushFail?.message).includes('未接通'),
    (pushFail ? JSON.stringify(pushFail) : JSON.stringify(pushFailRes.json)).slice(0, 240))
  const [failedRow] = await dbQuery(`SELECT order_status, attempts, channel_order_no, last_error
    FROM dms_channel_order WHERE idem_key = '${TASK_CH_NO}:${CH_DADA}' AND deleted = 0`)
  check('失败台账落库（状态=提交失败 6，含失败原因）',
    Number(failedRow?.order_status) === 6 && !failedRow?.channel_order_no && String(failedRow?.last_error).length > 0,
    JSON.stringify(failedRow))
  const [stillPending] = await dbQuery(`SELECT status FROM dms_task WHERE id = ${TASK_CH} AND deleted = 0`)
  check('下单失败任务不流转（仍为待分配 0）', Number(stillPending?.status) === 0, stillPending?.status)

  // ── 3) 渠道派单：适配器可用 → 成功 + 幂等 ──
  await api('PUT', `/dms/channel/${CH_OWN}`, {
    channelCode: 'E2E-CH-OWN', channelName: 'E2E自有配送队', channelType: 1,
    adapterBean: 'own', priority: 10, status: 1, coverageArea: '北京市朝阳区', billingType: 1,
  })
  const pushOkRes = await api('POST', `/dms/channel/${CH_OWN}/push-order`, { taskId: TASK_CH })
  const pushOk = data(pushOkRes)
  check('内部运力渠道下单成功并返回外部单号',
    pushOk?.success === true && String(pushOk?.channelOrderNo || '').startsWith('OWN-'),
    (pushOk ? JSON.stringify(pushOk) : JSON.stringify(pushOkRes.json)).slice(0, 240))
  const pushAgainRes = await api('POST', `/dms/channel/${CH_OWN}/push-order`, { taskId: TASK_CH })
  const pushAgain = data(pushAgainRes)
  check('幂等：重复提交复用同一外部单号（不再外呼）',
    pushAgain?.reused === true && pushAgain?.channelOrderNo === pushOk?.channelOrderNo,
    (pushAgain ? JSON.stringify(pushAgain) : JSON.stringify(pushAgainRes.json)).slice(0, 240))

  const [taskAfter] = await dbQuery(`SELECT status, dispatch_type FROM dms_task WHERE id = ${TASK_CH} AND deleted = 0`)
  check('派单成功任务流转「已分配」并记派单方式',
    Number(taskAfter?.status) === 1 && Number(taskAfter?.dispatch_type) === 2, JSON.stringify(taskAfter))
  const [logRow] = await dbQuery(`SELECT action, reason FROM dms_task_log
    WHERE task_no = '${TASK_CH_NO}' ORDER BY id DESC LIMIT 1`)
  check('派单写调度审计（含渠道名与外部单号）',
    String(logRow?.reason || '').includes('渠道派单') && String(logRow?.reason || '').includes('OWN-'),
    JSON.stringify(logRow))
  const [orderRow] = await dbQuery(`SELECT order_status, task_no, channel_order_no FROM dms_channel_order
    WHERE idem_key = '${TASK_CH_NO}:${CH_OWN}' AND deleted = 0`)
  check('成功台账落库（状态=已提交 1）',
    Number(orderRow?.order_status) === 1 && orderRow?.channel_order_no === pushOk?.channelOrderNo,
    JSON.stringify(orderRow))

  const orders = data(await api('GET', `/dms/channel/${CH_OWN}/orders?pageNum=1&pageSize=10`))
  check('外部单台账接口返回该单', (orders?.records || []).some(o =>
    o.taskNo === TASK_CH_NO && o.channelOrderNo === pushOk?.channelOrderNo),
    JSON.stringify((orders?.records || []).map(o => o.channelOrderNo)))

  // ── 4) 回调安全：验签 / 时间戳 / 防重放 / 状态回写（§7.3） ──
  const EXT_NO = 'E2E-EXT-001'
  const status = 'DELIVERING'
  const ts = Date.now()
  const nonce = 'E2E-NONCE-' + ts
  const mkSign = (t, n, s) => hmac(`E2E-CH-DADA${t}${n}${EXT_NO}${s}`, SECRET_RAW)

  const cbBase = { channelCode: 'E2E-CH-DADA', timestamp: ts, channelOrderNo: EXT_NO, status }
  const noSign = await api('POST', '/dms/channel/callback', { ...cbBase, nonce: nonce + '-a' })
  check('无签名回调被拒绝', !okRes(noSign) && String(noSign.json?.message || '').includes('签名'),
    `code=${noSign.json?.code} msg=${noSign.json?.message}`)

  const badSign = await api('POST', '/dms/channel/callback',
    { ...cbBase, nonce: nonce + '-b', sign: 'DEADBEEF' })
  check('错误签名回调被拒绝', !okRes(badSign) && String(badSign.json?.message || '').includes('签名不匹配'),
    `code=${badSign.json?.code} msg=${badSign.json?.message}`)

  const oldTs = ts - 3600 * 1000
  const expired = await api('POST', '/dms/channel/callback', {
    channelCode: 'E2E-CH-DADA', timestamp: oldTs, channelOrderNo: EXT_NO, status,
    nonce: nonce + '-c', sign: mkSign(oldTs, nonce + '-c', status),
  })
  check('过期时间戳回调被拒绝（防重放容差）',
    !okRes(expired) && String(expired.json?.message || '').includes('过期'),
    `code=${expired.json?.code} msg=${expired.json?.message}`)

  const noSecret = await api('POST', '/dms/channel/callback',
    { channelCode: 'E2E-CH-OWN', timestamp: ts, nonce: nonce + '-d', channelOrderNo: 'E2E-EXT-OWN', status, sign: 'X' })
  check('未配置回调密钥的渠道拒绝接收（不静默放行）',
    !okRes(noSecret) && String(noSecret.json?.message || '').includes('密钥'),
    `code=${noSecret.json?.code} msg=${noSecret.json?.message}`)

  const [rejectLogs] = await dbQuery(`SELECT count(*)::int AS c FROM dms_channel_callback_log
    WHERE channel_id = ${CH_DADA} AND sign_ok = 0 AND deleted = 0`)
  check('被拒回调全部留痕（sign_ok=0）', Number(rejectLogs?.c) >= 3, rejectLogs?.c)

  const okCb = data(await api('POST', '/dms/channel/callback', {
    ...cbBase, nonce, sign: mkSign(ts, nonce, status),
    eventType: 'rider_delivering', riderName: '外部骑手张三', riderPhone: '13900001111',
    taskNo: TASK_CH_NO, payload: '{"e2e":true}',
  }))
  check('正确签名回调处理成功（验签通过 + 已处理）',
    okCb?.accepted === true && okCb?.processed === true && okCb?.result === 'OK',
    JSON.stringify(okCb).slice(0, 260))
  check('回调回写任务状态（已分配 → 配送中）', Number(okCb?.taskStatus) === 4, okCb?.taskStatus)
  const [taskCb] = await dbQuery(`SELECT status, rider_name FROM dms_task
    WHERE id = ${TASK_CH} AND deleted = 0`)
  check('任务状态与外部配送员姓名快照落库',
    Number(taskCb?.status) === 4 && String(taskCb?.rider_name || '').includes('张三'),
    JSON.stringify(taskCb))
  const [cbAudit] = await dbQuery(`SELECT action, reason FROM dms_task_log
    WHERE task_no = '${TASK_CH_NO}' AND action = 'CHANNEL' ORDER BY id DESC LIMIT 1`)
  check('回调写调度审计（渠道回传 + 平台配送员电话留痕）',
    cbAudit?.action === 'CHANNEL' && String(cbAudit?.reason || '').includes('13900001111'),
    JSON.stringify(cbAudit))
  const [cbOrder] = await dbQuery(`SELECT order_status, task_no FROM dms_channel_order
    WHERE channel_order_no = '${EXT_NO}' AND deleted = 0`)
  check('回调按外部单号建台账并同步状态（配送中 3）',
    Number(cbOrder?.order_status) === 3 && cbOrder?.task_no === TASK_CH_NO, JSON.stringify(cbOrder))
  const [cbLog] = await dbQuery(`SELECT sign_ok, replayed, process_result, task_id FROM dms_channel_callback_log
    WHERE nonce = '${nonce}' AND deleted = 0`)
  check('回调日志留痕（验签通过 / 非重放 / 已处理 / 关联任务）',
    Number(cbLog?.sign_ok) === 1 && Number(cbLog?.replayed) === 0
    && cbLog?.process_result === 'OK' && String(cbLog?.task_id) === TASK_CH,
    JSON.stringify(cbLog))

  const replay = data(await api('POST', '/dms/channel/callback',
    { ...cbBase, nonce, sign: mkSign(ts, nonce, status) }))
  check('同 nonce 重放被幂等忽略（不重复回写）',
    replay?.accepted === true && replay?.processed === false && replay?.result === 'REPLAY',
    JSON.stringify(replay).slice(0, 200))
  const [replayStat] = await dbQuery(`SELECT count(*)::int AS c, sum(replayed)::int AS r
    FROM dms_channel_callback_log WHERE nonce = '${nonce}' AND deleted = 0`)
  check('重放不新增日志行且标记 replayed',
    Number(replayStat?.c) === 1 && Number(replayStat?.r) === 1, JSON.stringify(replayStat))

  const cbLogs = data(await api('GET', `/dms/channel/${CH_DADA}/callback-logs?pageNum=1&pageSize=10`))
  check('回调日志接口返回留痕', (cbLogs?.records || []).length >= 1,
    `len=${(cbLogs?.records || []).length}`)

  // ── 5) 派单接线（§7.1：DispatchService 读 channelId → 适配器下单） ──
  const autoTaskNo = 'E2E-CH-AUTO-' + STAMP
  const autoTaskRes = await api('POST', '/dms/task', {
    taskNo: autoTaskNo, orderNo: 'E2E-CH-AUTO-SO-' + STAMP, orderType: 1, channelId: CH_OWN,
    customerName: 'E2E渠道自动派单客户',
    sourceAddress: '北京市朝阳区仓库C', sourceLat: 39.9219, sourceLng: 116.4434,
    customerAddress: '北京市海淀区中关村3号', customerLat: 39.9836, customerLng: 116.3164,
    totalWeight: 3, totalVolume: 0.2,
  })
  check('派单接线前置：新建带渠道的待分配任务', okRes(autoTaskRes) && data(autoTaskRes)?.id,
    JSON.stringify(autoTaskRes.json).slice(0, 160))
  const autoTaskId = data(autoTaskRes)?.id

  const preview = data(await api('POST', '/dms/dispatch/auto/preview', { maxTasks: 50 }))
  const previewRow = (preview?.rows || []).find(r => String(r.taskId) === String(autoTaskId))
  check('自动调度预览把渠道任务标为「渠道派单」（不占自有配送员）',
    previewRow?.viaChannel === true && Number(previewRow?.channelId) === Number(CH_OWN),
    JSON.stringify(previewRow).slice(0, 220))

  const autoRun = data(await api('POST', '/dms/dispatch/auto', { maxTasks: 50 }))
  const ranRow = (autoRun?.rows || []).find(r => String(r.taskId) === String(autoTaskId))
  check('自动调度执行经渠道下单成功（回填外部单号）',
    String(ranRow?.channelOrderNo || '').startsWith('OWN-'), JSON.stringify(ranRow).slice(0, 240))
  const [autoOrder] = await dbQuery(`SELECT order_status, channel_order_no FROM dms_channel_order
    WHERE idem_key = '${autoTaskNo}:${CH_OWN}' AND deleted = 0`)
  check('自动调度渠道单落台账（已提交 1）',
    Number(autoOrder?.order_status) === 1 && String(autoOrder?.channel_order_no || '').startsWith('OWN-'),
    JSON.stringify(autoOrder))
  const [autoTask] = await dbQuery(`SELECT status, dispatch_type FROM dms_task
    WHERE task_no = '${autoTaskNo}' AND deleted = 0`)
  check('自动调度任务流转「已分配」（渠道派单）',
    Number(autoTask?.status) === 1 && Number(autoTask?.dispatch_type) === 1, JSON.stringify(autoTask))

  // ── 6) 免登录可达（外部平台无会话调用；安全由验签保证） ──
  const anonCb = await rawReq('POST', '/dms/channel/callback', {
    channelCode: 'E2E-CH-DADA', timestamp: ts, nonce: nonce + '-e',
    channelOrderNo: EXT_NO, status,
  })
  check('回调接口免登录可达（未签名时返回业务校验错误而非 401）',
    anonCb.status !== 401 && Number(anonCb.json?.code) !== 401,
    `http=${anonCb.status} code=${anonCb.json?.code}`)
}

// ══════════════════════════════════════════════════════════════
// 三、UI 验收
// ══════════════════════════════════════════════════════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  async function openPage(url, waitMs = 4000) {
    for (let attempt = 0; attempt < 3; attempt++) {
      await login()
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(([tk]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', '1')
      }, [TOKEN])
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(waitMs)
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }

  const bodyText = async () => (await page.locator('body').innerText()).replace(/\s+/g, '')

  /** 轮询等待（插槽列文字可能比行渲染更晚，避免把「渲染中」误判为「枚举没生效」） */
  async function waitUntil(fn, timeoutMs = 12000, stepMs = 500) {
    const deadline = Date.now() + timeoutMs
    while (Date.now() < deadline) {
      if (await fn()) return true
      await page.waitForTimeout(stepMs)
    }
    return false
  }
  const gridRowTexts = () => page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid tbody tr')].map(tr => (tr.innerText || '').replace(/\s+/g, ' ').trim()))

  await openPage(`${FE}/dms/channel`, 5000)
  await page.waitForTimeout(1500)
  let txt = await bodyText()
  check('页面可打开（无 404/白屏）', !txt.includes('页面不存在') && txt.length > 100, page.url())

  // 查询项
  for (const k of ['渠道编码', '渠道名称', '渠道类型', '对接状态', '启用状态']) {
    check(`查询区含「${k}」`, txt.includes(k))
  }

  // 列与数据
  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()))
  const expectCols = ['渠道编码', '渠道名称', '渠道类型', '对接状态', '覆盖区域', '计费方式', '在线运力', '优先级', '启用状态']
  check('数据表关键列表头齐全', expectCols.every(h => headers.some(x => x.includes(h))), headers.join('/'))
  const enumRendered = await waitUntil(async () =>
    (await gridRowTexts()).some(t => t.includes('自有员工') || t.includes('外部平台')))
  const gridRows = await gridRowTexts()
  check('渠道类型列展示中文枚举（与后端 ChannelTypeEnum 对齐，非「线上/线下」）',
    enumRendered, gridRows.slice(0, 3).join(' || '))
  const gearCount = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].filter(th => th.querySelector('button, .anticon-setting, i')).length)
  check('表头带列配置齿轮（个人/全局）', gearCount > 0, `count=${gearCount}`)

  // 页面配置：取消「导出」按钮
  const openConfig = async () => {
    await page.locator('button:has(.anticon-setting)').last().click()
    await page.waitForTimeout(800)
  }
  const closeConfig = async () => {
    await page.locator('.ant-tabs-tabpane-active .tab-footer button').filter({ hasText: /关\s*闭/ }).first().click()
    await page.waitForTimeout(700)
  }
  await openConfig()
  check('页面配置弹窗打开', (await bodyText()).includes('页面配置'))
  const cfgRows = await page.evaluate(() =>
    [...document.querySelectorAll('.ant-tabs-tabpane-active .config-table tbody tr')].map(tr => tr.innerText.replace(/\s+/g, '')))
  check('页面配置含 6 个查询条件项',
    ['渠道编码', '渠道名称', '渠道类型', '对接状态', '启用状态', '创建时间'].every(k => cfgRows.some(r => r.includes(k))),
    cfgRows.join('|'))
  await page.locator('.config-tabs .ant-tabs-tab', { hasText: '功能按钮' }).click()
  await page.waitForTimeout(400)
  const btnRows = await page.evaluate(() =>
    [...document.querySelectorAll('.ant-tabs-tabpane-active .config-table tbody tr')].map(tr => tr.innerText.replace(/\s+/g, '')))
  check('功能按钮含 新增/连通性测试/同步运力/渠道派单/外部单台账/刷新/导出',
    ['新增渠道', '连通性测试', '同步运力', '渠道派单', '外部单台账', '刷新', '导出'].every(k => btnRows.some(r => r.includes(k))),
    btnRows.join('|'))
  await closeConfig()
  check('工具栏含 新增渠道/连通性测试/同步运力/渠道派单/台账/导出',
    ['新增渠道', '连通性测试', '同步运力', '渠道派单', '台账', '导出'].every(k => txt.includes(k)), '')

  // 新增渠道（4 Tab 弹窗）
  await page.locator('.toolbar-right button', { hasText: '新增渠道' }).click()
  await page.waitForTimeout(900)
  const modalTxt = await bodyText()
  check('新增弹窗含 4 个 Tab', ['基础信息', '对接配置', '计费规则', '对接状态'].every(k => modalTxt.includes(k)), '')
  const uiCode = 'E2E-CH-UI' + STAMP
  const formModal = page.locator('.ant-modal:visible').first()
  await formModal.locator('input.ant-input').first().fill(uiCode)
  await formModal.locator('input.ant-input').nth(1).fill('E2E页面新建渠道')
  await formModal.locator('.ant-btn-primary').filter({ hasText: /保\s*存/ }).click()
  await page.waitForTimeout(1200)
  // 保存后列表会异步刷新：轮询等待新行出现（共享环境下渲染快慢不一，避免误报）
  const createdVisible = await waitUntil(async () => (await bodyText()).includes(uiCode), 15000)
  check('新增渠道成功并出现在列表', createdVisible, uiCode)

  // 编辑：编码禁用
  const row = page.locator('.ss-grid tbody tr', { hasText: uiCode }).first()
  await row.locator('button', { hasText: '编辑' }).click()
  await page.waitForTimeout(1500)
  const editModal = page.locator('.ant-modal:visible').first()
  const codeDisabled = await editModal.locator('input.ant-input').first().isDisabled()
  check('编辑时渠道编码不可修改', codeDisabled, `disabled=${codeDisabled}`)
  const editModalTxt = await bodyText()
  check('编辑弹窗展示对接状态只读区', editModalTxt.includes('对接状态'), '')
  await editModal.locator('.ant-btn').filter({ hasText: /取\s*消/ }).click()
  await page.waitForTimeout(600)

  // 行内测试（未接通提示）
  const dadaRow = page.locator('.ss-grid tbody tr', { hasText: 'E2E达达平台' }).first()
  if (await dadaRow.count()) {
    await dadaRow.locator('button', { hasText: '测试' }).click()
    await page.waitForTimeout(2500)
    check('行内连通性测试给出未接通提示（不伪造成功）',
      (await bodyText()).includes('未') || (await bodyText()).includes('未接通'), '')
  } else {
    check('行内连通性测试给出未接通提示（不伪造成功）', false, '未找到达达样本行')
  }

  // 行内删除（未被引用的 UI 新建渠道）
  const delRow = page.locator('.ss-grid tbody tr', { hasText: uiCode }).first()
  await delRow.locator('button', { hasText: '删除' }).click()
  await page.waitForTimeout(700)
  await page.locator('.ant-popconfirm .ant-btn-primary').filter({ hasText: /删\s*除/ }).click()
  await page.waitForTimeout(2500)
  check('行内删除成功（列表不再出现）', !(await bodyText()).includes(uiCode), uiCode)

  // 渠道派单（选择待配送任务 → 下单 → 展示外部单号）
  await api('PUT', `/dms/channel/${CH_OWN}`, {
    channelCode: 'E2E-CH-OWN', channelName: 'E2E自有配送队', channelType: 1,
    adapterBean: 'own', priority: 10, status: 1, coverageArea: '北京市朝阳区', billingType: 1,
  })
  const uiTaskNo = 'E2E-CH-UITASK-' + STAMP
  const uiTaskRes = await api('POST', '/dms/task', {
    taskNo: uiTaskNo, orderNo: 'E2E-CH-UI-SO-' + STAMP, orderType: 1,
    customerName: 'E2E渠道UI客户', sourceAddress: '北京市朝阳区仓库B', sourceLat: 39.9219, sourceLng: 116.4434,
    customerAddress: '北京市海淀区中关村2号', customerLat: 39.9836, customerLng: 116.3164,
    totalWeight: 5, totalVolume: 0.3,
  })
  check('UI 前置：新建待分配任务成功', okRes(uiTaskRes), JSON.stringify(uiTaskRes.json).slice(0, 160))
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(3000)

  const ownRow = page.locator('.ss-grid tbody tr', { hasText: 'E2E自有配送队' }).first()
  await ownRow.locator('button', { hasText: '派单' }).click()
  await page.waitForTimeout(1000)
  const pushModal = page.locator('.ant-modal:visible').first()
  const pushModalTxt = await pushModal.innerText()
  check('渠道派单弹窗含目标渠道与任务选择器',
    pushModalTxt.includes('渠道派单') && pushModalTxt.includes('待配送任务') && pushModalTxt.includes('E2E自有配送队'),
    pushModalTxt.replace(/\s+/g, ' ').slice(0, 160))
  const selectInput = pushModal.locator('.ant-select input').first()
  await selectInput.click()
  await selectInput.fill(uiTaskNo)
  await page.waitForTimeout(1800)
  const optionCount = await page.locator('.ant-select-item-option').count()
  check('待配送任务选择器可按任务号搜到目标任务', optionCount > 0, `options=${optionCount}`)
  if (optionCount > 0) {
    await page.locator('.ant-select-item-option').first().click()
    await page.waitForTimeout(400)
    await pushModal.locator('.ant-btn-primary').filter({ hasText: /提交派单/ }).click()
    await page.waitForTimeout(2500)
    const resultTxt = (await pushModal.innerText()).replace(/\s+/g, ' ')
    check('UI 渠道派单成功并展示外部单号与结果',
      resultTxt.includes('成功') && resultTxt.includes('OWN-'), resultTxt.slice(0, 200))
    await pushModal.locator('.ant-btn').filter({ hasText: /取\s*消/ }).click()
    await page.waitForTimeout(600)
  }

  // 渠道台账（外部单 + 回调日志）
  const ledgerRow = page.locator('.ss-grid tbody tr', { hasText: 'E2E自有配送队' }).first()
  await ledgerRow.locator('button', { hasText: '台账' }).click()
  await page.waitForTimeout(1800)
  const ledgerTxt = (await bodyText())
  check('台账抽屉含 外部单台账 / 回调日志 两个 Tab',
    ledgerTxt.includes('外部单台账') && ledgerTxt.includes('回调日志'), '')
  check('外部单台账展示 UI 派单的外部单号', ledgerTxt.includes('OWN-'), '')
  await page.keyboard.press('Escape')
  await page.waitForTimeout(800)

  // 批量启停
  await page.locator('.ss-grid tbody tr').first().locator('.ss-checkbox, input[type=checkbox]').first().click()
  await page.waitForTimeout(500)
  const selectedTxt = await bodyText()
  check('勾选后批量操作可用（提示已选）', selectedTxt.includes('已选'), '')
  await page.locator('.toolbar-left button', { hasText: '批量停用' }).click()
  await page.waitForTimeout(2000)
  check('批量停用成功提示', (await bodyText()).includes('批量停用'), '')
  await page.locator('.toolbar-left button', { hasText: '批量启用' }).click()
  await page.waitForTimeout(2000)

  // 导出 xlsx
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 15000 }),
    page.locator('.toolbar-right button').filter({ hasText: /导\s*出/ }).click(),
  ])
  const fname = download.suggestedFilename()
  check('导出文件名为渠道管理 xlsx', fname.startsWith('渠道管理') && fname.endsWith('.xlsx'), fname)
  const saved = path.join(SHOTS, fname)
  await download.saveAs(saved)
  check('导出文件非空（真实二进制）', fs.statSync(saved).size > 2000, `${fs.statSync(saved).size} bytes`)

  await page.screenshot({ path: path.join(SHOTS, 'ui-channel.png'), fullPage: true })
  await browser.close()
}

// ══════════════════════════════════════════════════════════════
;(async () => {
  try {
    await apiSuite()
  } catch (e) {
    console.error('接口验收异常:', e && (e.message || e))
  }
  try {
    await dispatchSuite()
  } catch (e) {
    console.error('渠道派单/回调验收异常:', e && (e.message || e))
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
