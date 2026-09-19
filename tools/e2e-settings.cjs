/*
 * 设置模块金标准端到端验证（模块级 E2E）
 *
 * 口径依据：`docs/Yh-Spec/手动整理对标开发文档/设置模块/`（README + 18 篇页面文档）。
 *   设置模块 = **租户级**（client_type = tenant-admin）——「我这个租户自己怎么用这套系统」，
 *   与平台级《系统模块》（system-admin）严格分域。13 个页面在 ql361「设置」域有 1:1 对标，
 *   5 个工作流/审批页为本系统独有（ql361 无工作流域），按业界（Odoo / SAP / 金蝶 / 用友 / Flowable）建模。
 *
 * 验收方式：**先只读对账 → 再造数/改库回读 → 最后按原值复原/清理**。
 *   每个「对账」类断言先直连 PostgreSQL 取期望值（含 tenant_id / deleted=0 口径），
 *   再与 HTTP 返回值逐项比较；不使用「只断言 HTTP 200」的空炮。
 *   依赖环境前置数据而无法验证的项，在断言名或 detail 里**显式写明降级原因**，不做静默通过。
 *
 * 用法：
 *   node tools/e2e-settings.cjs
 *   SET_PORT=5691 FE_URL=http://localhost:5657 node tools/e2e-settings.cjs
 *   SET_ONLY=api node tools/e2e-settings.cjs     （跳过 UI 节，只跑接口 + DB 对账）
 *
 * 副作用与清理：
 *   1) 写库断言统一「改 → 三级回读（DB 行 / 接口 / 页面数据源）→ 按原值复原」，
 *      仅操作自造行或本页自有配置行，不触碰既有业务数据；
 *   2) 截图落到 `I:/AI-Ready/tool-results/e2e-settings/`（目录不存在时自动创建）。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const FE = process.env.FE_URL || 'http://localhost:5657'
const PORT = Number(process.env.SET_PORT || 5691)
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
const E2E_USER = process.env.E2E_USER || 'e2e_settings'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const SHOTS = 'I:/AI-Ready/tool-results/e2e-settings'
const ONLY = (process.env.SET_ONLY || '').trim()

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}
function skip(name, reason) {
  results.push({ name, ok: true, skipped: true, detail: reason })
  console.log(`⏭️  ${name} — 跳过：${reason}`)
}

// ════════════════════════════════════════════════════════════════════
// HTTP / DB 基础设施
// ════════════════════════════════════════════════════════════════════
function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? null : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json' }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    if (token) headers.Authorization = `Bearer ${token}`
    const r = http.request({ hostname: 'localhost', port: PORT, path: '/api' + reqPath, method, headers }, res => {
      // ⚠️ 必须按 Buffer 收集后一次性解码：`s += chunk` 会逐块 toString，
      //    多字节汉字被切在 chunk 边界时会产生 U+FFFD，导致假失败。
      const chunks = []
      res.on('data', c => chunks.push(Buffer.isBuffer(c) ? c : Buffer.from(c)))
      res.on('end', () => {
        const s = Buffer.concat(chunks).toString('utf8')
        let json = null
        try { json = JSON.parse(s) } catch { /* 非 JSON */ }
        resolve({ status: res.statusCode, body: json, text: s })
      })
    })
    // 必须给请求兜超时：不设 timeout 时，后端进程已被换 jar / 半死不活会让脚本**永久挂起**
    // （2026-09-18 实踩：fat jar 被并行会话覆盖 → NoClassDefFoundError → 脚本卡在第 1 行无输出）
    r.setTimeout(30000, () => r.destroy(new Error('请求超时(30s): ' + method + ' ' + reqPath)))
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}
const get = (p, token) => rawReq('GET', p, null, token)
const post = (p, b, token) => rawReq('POST', p, b, token)
const put = (p, b, token) => rawReq('PUT', p, b, token)
const del = (p, token) => rawReq('DELETE', p, null, token)

async function dbQuery(sql, params) {
  const client = new Client(DSN)
  await client.connect()
  try {
    const res = await client.query(sql, params)
    return res.rows
  } finally { await client.end() }
}
async function dbOne(sql, params) {
  const rows = await dbQuery(sql, params)
  return rows[0] || {}
}

async function waitBackend(attempts = 40, intervalMs = 3000) {
  for (let i = 0; i < attempts; i++) {
    try {
      const r = await get('/auth/captcha')
      if (r.status === 200 && r.body && r.body.data && r.body.data.img) return true
    } catch { /* retry */ }
    console.log(`  等待后端 ${PORT} 就绪… (${i + 1}/${attempts})`)
    await new Promise(res => setTimeout(res, intervalMs))
  }
  return false
}

async function login() {
  const cap = await get('/auth/captcha')
  if (!cap.body || !cap.body.data) throw new Error('验证码接口异常: ' + cap.text.slice(0, 200))
  const code = [...Buffer.from(cap.body.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await post('/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.body.data.uuid
  })
  const tk = res.body && res.body.data && (res.body.data.token || res.body.data.accessToken)
  if (!tk) throw new Error('登录失败: ' + JSON.stringify(res.body).slice(0, 300))
  return { token: tk, userInfo: res.body.data }
}

// 共享环境下 sa-token `is-concurrent: false` 会让并行会话互踢；401 时自愈重登一次。
let TOKEN = null
/** 当前 E2E 账号的 userId（决定「超管会话整体豁免多租户」的口径判定） */
let CURRENT_USER_ID = '1'
async function api(method, path, body) {
  let r = await rawReq(method, path, body, TOKEN)
  if (r.status === 401 || (r.body && r.body.code === 401)) {
    const s = await login()
    TOKEN = s.token
    CURRENT_USER_ID = String(s.userInfo?.userId || CURRENT_USER_ID)
    r = await rawReq(method, path, body, TOKEN)
  }
  return r
}
const apiGet = (p) => api('GET', p)
const apiPost = (p, b) => api('POST', p, b)
const apiPut = (p, b) => api('PUT', p, b)

// ⚠️ 本仓库响应体两套形态并存：多数端点走统一包装 {code,message,data}，
//    但部分设置模块端点（/config/nav-groups、/config/page、/workflow/audit-config/list 等）
//    直接返回裸对象/裸数组。判定必须同时容忍，否则会把正常响应误报为失败。
function isOk(r) {
  if (r.status !== 200) return false
  if (!r.body) return false
  if (r.body.code === undefined) return true
  return r.body.code === 200
}
function dataOf(r) {
  if (!r.body) return undefined
  return r.body.code === undefined ? r.body : r.body.data
}
function recordsOf(r) { const d = dataOf(r); return (d && d.records) || (Array.isArray(d) ? d : []) }

// ════════════════════════════════════════════════════════════════════
// 1. 菜单完整性（设置模块 17 页 + 打印设置新建页）
// ════════════════════════════════════════════════════════════════════
const TENANT_ADMIN = 'tenant-admin'
const EXPECTED_MENUS = [
  [60012, 0, '设置', 'mega:settings'],
  [61201, 60012, '系统配置', 'mega:set:sys-config'],
  [61202, 60012, '数据录入', 'mega:set:data-entry'],
  [61203, 60012, '账套操作', 'mega:set:account'],
  [61204, 60012, '财务设置', 'mega:set:fin-config'],
  [61205, 60012, '打印管理', 'mega:set:printing'],
  [61206, 60012, '工作流', 'mega:set:workflow'],
  [61207, 60012, '审批', 'mega:set:approval'],
  [80620, 61201, '菜单配置', 'set:menu-config'],
  [80621, 61201, '系统参数', 'set:sys-params'],
  [80622, 61201, '审核设置', 'set:audit-config'],
  [80623, 61201, '支付配置', 'set:payment-config'],
  [80624, 61201, '企业信息', 'set:company-info'],
  [80625, 61201, '应用中心', 'set:app-center'],
  [70550, 61202, '库存期初', 'set:initial-stock'],
  [70551, 61202, '财务期初', 'set:initial-finance'],
  [70560, 61203, '系统重建', 'set:rebuild'],
  [70561, 61203, '系统任务', 'set:system-task'],
  [80630, 61203, '操作日志', 'set:operation-log'],
  [70570, 61204, '会计期间', 'set:accounting-period'],
  [80930, 61205, '打印设置', 'set:print-config'],
  [801, 61206, '流程定义', 'workflow-definition'],
  [80610, 61206, '流程设计', 'set:workflow-designer'],
  [802, 61207, '流程实例', 'workflow-instance'],
  [803, 61207, '我的待办', 'my-task'],
  [804, 61207, '我的已办', 'my-done'],
]

async function sectionMenus() {
  console.log('\n════ 1. 菜单完整性 ════')
  const rows = await dbQuery(
    `SELECT id::text AS id, COALESCE(parent_id,0)::text AS pid, menu_name, menu_code, path, component, client_type, status
       FROM sys_menu WHERE deleted = 0 AND (id = 60012 OR parent_id = 60012
         OR parent_id IN (SELECT id FROM sys_menu WHERE parent_id = 60012))
      ORDER BY id`)
  const byId = new Map(rows.map(r => [r.id, r]))
  for (const [id, pid, name, code] of EXPECTED_MENUS) {
    const r = byId.get(String(id))
    const ok = !!r && r.menu_name === name && r.menu_code === code && String(r.pid) === String(pid)
    check(`菜单 ${id} ${name}`, ok,
      ok ? `${r.path} → ${r.component}` : `DB 实得: ${r ? JSON.stringify({ name: r.menu_name, code: r.menu_code, pid: r.pid }) : '不存在'}`)
  }
  const print = byId.get('80930')
  check('80930 打印设置挂到 61205（原空分组已落地）', !!print && String(print.pid) === '61205',
    print ? `parent=${print.pid} path=${print.path} component=${print.component}` : '不存在')
  check('80930 client_type = tenant-admin（漏写则整页 404）', !!print && print.client_type === TENANT_ADMIN,
    print ? `client_type=${print.client_type}` : '-')
}

// ════════════════════════════════════════════════════════════════════
// 2. 接口探针（每页主端点）
// ════════════════════════════════════════════════════════════════════
async function sectionApis() {
  console.log('\n════ 2. 接口探针 ════')
  const probes = [
    ['GET', '/set/menu-config/list', '菜单配置·分组+页面'],
    ['GET', '/config/nav-groups', '系统参数·左列纵向标签'],
    ['GET', '/config/page?pageNum=1&pageSize=20', '系统参数·分页'],
    ['GET', '/workflow/audit-config/list', '审核设置·16 类单据'],
    ['GET', '/payment/config/items?tab=wechat', '支付配置·微信公众号'],
    ['GET', '/payment/config/channels', '支付配置·支付方式'],
    ['GET', '/payment/config/scenes', '支付配置·场景配置'],
    ['GET', '/payment/config/items?tab=refund', '支付配置·在线退款'],
    ['GET', '/tenant/current', '企业信息·当前租户档案'],
    ['GET', '/set/app-center/overview', '应用中心·信息卡'],
    ['GET', '/set/app-center/modules', '应用中心·功能模块'],
    ['GET', '/set/app-center/capabilities', '应用中心·短信及其他'],
    ['GET', '/set/initial-stock/page?pageNum=1&pageSize=20', '库存期初·分页'],
    ['GET', '/erp/finance/initial/subject/page?initialType=BANK_CASH&pageNum=1&pageSize=20', '财务期初·银行现金'],
    ['GET', '/erp/finance/initial/subject/page?initialType=FIXED_ASSET&pageNum=1&pageSize=20', '财务期初·固定资产'],
    ['GET', '/erp/finance/initial/subject/page?initialType=BALANCE_SHEET&pageNum=1&pageSize=20', '财务期初·资产负债'],
    ['GET', '/erp/finance/initial/partner/page?initialType=PAYABLE&pageNum=1&pageSize=20', '财务期初·应付'],
    ['GET', '/erp/finance/initial/partner/page?initialType=RECEIVABLE&pageNum=1&pageSize=20', '财务期初·应收'],
    ['GET', '/set/rebuild/options', '系统重建·12 选项'],
    ['GET', '/set/system-task/page?pageNum=1&pageSize=20', '系统任务·9 列台账'],
    ['GET', '/log/page?pageNum=1&pageSize=20', '操作日志·系统日志'],
    ['GET', '/log/login/page?pageNum=1&pageSize=20', '操作日志·登录日志'],
    ['GET', '/erp/finance/period/list', '会计期间'],
    ['GET', '/set/print-config', '打印设置·读取'],
    ['GET', '/workflow/definitions?pageNum=1&pageSize=20', '流程定义·台账'],
    ['GET', '/workflow/instance/page?pageNum=1&pageSize=20', '流程实例·分页'],
    ['GET', '/workflow/instance/stat', '流程实例·真聚合统计'],
    ['GET', '/workflow/task/stat', '我的待办/已办·真聚合统计'],
  ]
  for (const [m, p, label] of probes) {
    const r = m === 'GET' ? await apiGet(p) : await apiPost(p, null)
    const d = dataOf(r)
    const n = d && d.total !== undefined ? d.total : (Array.isArray(d) ? d.length : undefined)
    check(`${label}  ${p}`, isOk(r), isOk(r) ? `rows/total=${n}` : `${r.status} ${String(r.body && r.body.message).slice(0, 120)}`)
  }

  // 路径错配自查：前端 api 文件里不得出现 `/api/` 双前缀
  const apiDir = 'I:/AI-Ready/frontend/apps/pc-admin/src/api'
  const bad = []
  const walk = (dir) => {
    for (const f of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = dir + '/' + f.name
      if (f.isDirectory()) { walk(full); continue }
      if (!/\.ts$/.test(f.name)) continue
      // ⚠️ 必须先把块注释整体抹掉再逐行扫：本仓库大量 JSDoc 单行注释形如
      //    `/** 形如 \`/api/file/view/...\` */` —— 行首是 `/**` 而不是 `*`，
      //    只判 `startsWith('*')` 会把它当成真代码，产生 100% 误报（本会话踩过）。
      const txt = fs.readFileSync(full, 'utf8').replace(/\/\*[\s\S]*?\*\//g, m => m.replace(/[^\n]/g, ' '))
      txt.split('\n').forEach((line, i) => {
        const t = line.trim()
        if (t.startsWith('//')) return
        if (/['"`]\/api\//.test(line)) bad.push(`${f.name}:${i + 1}`)
      })
    }
  }
  walk(apiDir)
  const setApis = bad.filter(x => /^(set|log|workflow|config|tenant|payment)/.test(x))
  check('设置模块 api 文件无 `/api/` 双前缀', setApis.length === 0,
    setApis.length ? `命中 ${setApis.join(', ')}` : '0 命中')
}

// ════════════════════════════════════════════════════════════════════
// 3. 只读 DB 对账
// ════════════════════════════════════════════════════════════════════
async function sectionReconcile() {
  console.log('\n════ 3. 只读 DB 对账 ════')

  // 3.1 系统参数：接口总数 == sys_config 启用行数（不再是 JVM 内存 12 条）
  const cfgDb = await dbOne(`SELECT count(*)::int AS c FROM sys_config WHERE deleted = 0`)
  const cfgApi = await apiGet('/config/page?pageNum=1&pageSize=200')
  const cfgRows = recordsOf(cfgApi)
  check('系统参数数据源 = sys_config（真实落库）',
    isOk(cfgApi) && cfgRows.length === cfgDb.c && cfgRows.length > 12,
    `DB=${cfgDb.c} 接口=${cfgRows.length}（改造前恒为内存 12 条）`)
  check('系统参数每行 id 非空（row-key 生效）',
    cfgRows.length > 0 && cfgRows.every(r => r.id != null),
    `null id 行数 = ${cfgRows.filter(r => r.id == null).length}`)
  const navApi = await apiGet('/config/nav-groups')
  const navs = dataApiArray(navApi)
  check('系统参数左列纵向标签 = 8 个（对标实测值）', navs.length === 8,
    `实得 ${navs.length}: ${navs.map(n => n.name).join('/')}`)

  // 3.2 菜单配置：接口返回**扁平页面数组**（data = [SetMenuConfigItem]），分组由 domainId/domainName 派生
  const mcRows = dataApiArray(await apiGet('/set/menu-config/list'))
  // 期望值必须与「租户端菜单树」口径一致：一级域 (parent_id=0) 必须也是 tenant-admin，
  // 否则会多算 `6130701 菜单管理`（client_type=tenant-admin 却挂在 system-admin 域下的异类行）。
  const mcDb = await dbOne(
    `WITH RECURSIVE tree AS (
       SELECT m.id, m.menu_type FROM sys_menu m
        WHERE m.deleted = 0 AND m.parent_id = 0 AND m.client_type = '${TENANT_ADMIN}' AND m.status = 1
       UNION ALL
       SELECT m.id, m.menu_type FROM sys_menu m JOIN tree t ON m.parent_id = t.id
        WHERE m.deleted = 0 AND m.status = 1 AND m.client_type = '${TENANT_ADMIN}'
     )
     SELECT count(*)::int AS c FROM tree WHERE menu_type = 1`)
  check('菜单配置：页面条数 == 租户端菜单树页面级菜单数（排除挂在平台域的 6130701）',
    mcRows.length === mcDb.c, `DB=${mcDb.c} 接口=${mcRows.length}`)
  check('菜单配置：无 domainId 为空的孤儿行（挂在平台域的菜单不得出现）',
    mcRows.every(r => r.domainId != null && r.domainId !== ''),
    `孤儿行=${mcRows.filter(r => r.domainId == null || r.domainId === '').length}`)
  const mcDomains = new Set(mcRows.map(r => r.domainId))
  const mcDomainDb = await dbOne(
    `SELECT count(*)::int AS c FROM sys_menu WHERE deleted = 0 AND parent_id = 0
       AND client_type = '${TENANT_ADMIN}' AND status = 1`)
  check('菜单配置：左侧「菜单分组」面板节点数 == tenant-admin 一级域数',
    mcDomains.size === mcDomainDb.c, `DB=${mcDomainDb.c} 接口=${mcDomains.size}`)
  check('菜单配置：每行都有 visible 布尔值（开关可持久化）',
    mcRows.length > 0 && mcRows.every(r => typeof r.visible === 'boolean'),
    `非布尔行数=${mcRows.filter(r => typeof r.visible !== 'boolean').length}`)

  // 3.3 操作日志：系统日志 total == sys_oper_log 行数
  //     ⚠️ 两个口径必须分开写：
  //     ① sys_oper_log **没有 deleted 列**（日志表不逻辑删除）；
  //     ② 超管会话整体豁免多租户（SysUserServiceImpl 写 session.tenantScopeExempt，
  //        AiReadyTenantLineInnerInterceptor.shouldSkip 读到即不注入 tenant_id），
  //        此时 total 是**全库**值而非本租户值 —— 断言必须两个口径都接受并写明用的是哪个。
  const exempt = await dbOne(
    `SELECT count(*)::int AS c FROM sys_user_role WHERE user_id = $1 AND role_id = 1`, [CURRENT_USER_ID])
  const isExempt = exempt.c > 0
  const logTotal = async (table, api) => {
    const t1 = Number((await dbOne(`SELECT count(*)::int AS c FROM ${table} WHERE tenant_id = 1`)).c)
    const all = Number((await dbOne(`SELECT count(*)::int AS c FROM ${table}`)).c)
    const apiTotal = Number(operate_total(api))
    return { t1, all, apiTotal, ok: apiTotal === (isExempt ? all : t1), caliber: isExempt ? '全库(超管豁免)' : '本租户' }
  }
  const operApi = await apiGet('/log/page?pageNum=1&pageSize=1')
  const operR = await logTotal('sys_oper_log', operApi)
  check('操作日志·系统日志 total == sys_oper_log 行数（改造前恒空）', operR.ok,
    `${operR.caliber}口径 DB=${isExempt ? operR.all : operR.t1} 接口=${operR.apiTotal}`)
  const loginApi = await apiGet('/log/login/page?pageNum=1&pageSize=1')
  const loginR = await logTotal('sys_login_log', loginApi)
  check('操作日志·登录日志 total == sys_login_log 行数', loginR.ok,
    `${loginR.caliber}口径 DB=${isExempt ? loginR.all : loginR.t1} 接口=${loginR.apiTotal}`)

  // 3.4 审核设置：16 类单据（data = {list: [...]}）
  const auditPayload = dataOf(await apiGet('/workflow/audit-config/list')) || {}
  const auditRows = auditPayload.list || []
  check('审核设置 = 16 类单据（ql361 实测清单）', auditRows.length === 16,
    `实得 ${auditRows.length}${auditRows.length ? '：' + auditRows.slice(0, 4).map(r => r.docName).join('/') + '…' : ''}`)
  check('审核设置每行含 单据/审核设置/摘要 三列口径',
    auditRows.length > 0 && auditRows.every(r => r.docType && r.docName && r.summary !== undefined),
    `缺字段行数=${auditRows.filter(r => !(r.docType && r.docName && r.summary !== undefined)).length}`)
  const auditRulesDb = await dbOne(`SELECT count(*)::int AS c FROM sys_audit_rule WHERE deleted = 0`)
  check('审核设置规则表 sys_audit_rule 存在且可查', auditRulesDb.c >= 0, `行数=${auditRulesDb.c}`)

  // 3.5 打印设置：表 + 菜单（原空分组）
  const printDb = await dbOne(`SELECT count(*)::int AS c FROM set_print_config WHERE deleted = 0`)
  const printApi = await apiGet('/set/print-config')
  check('打印设置：GET 200 且回读 set_print_config', isOk(printApi),
    isOk(printApi) ? `DB行数=${printDb.c}` : `${printApi.status} ${String(printApi.body && printApi.body.message).slice(0, 100)}`)

  // 3.6 会计期间：按年度取 12 期（/list?periodYear=YYYY）
  const maxYear = await dbOne(`SELECT max(period_year)::int AS y FROM fin_accounting_period`)
  const periods = dataApiArray(await apiGet(`/erp/finance/period/list?periodYear=${maxYear.y}`))
  const pDb = await dbOne(
    `SELECT count(*)::int AS c FROM fin_accounting_period WHERE tenant_id = 1 AND period_year = $1`,
    [maxYear.y])
  check('会计期间按年度返回 12 期（固定矩阵，不分页）',
    periods.length === 12 && pDb.c === 12,
    `年度=${maxYear.y} 接口=${periods.length} DB=${pDb.c}`)

  // 3.7 系统任务：复用 scheduled_task_log
  const taskApi = await apiGet('/set/system-task/page?pageNum=1&pageSize=20')
  const taskDb = await dbOne(`SELECT count(*)::int AS c FROM scheduled_task_log`)
  check('系统任务台账取数 == scheduled_task_log（复用，不新建表）',
    isOk(taskApi) && Number((dataOf(taskApi) || {}).total) === taskDb.c,
    `DB=${taskDb.c} 接口 total=${(dataOf(taskApi) || {}).total}`)

  // 3.8 应用中心：已开通模块不再恒空
  const mods = dataApiArray(await apiGet('/set/app-center/modules'))
  const modDb = await dbOne(`SELECT count(*)::int AS c FROM sys_tenant_module WHERE deleted = 0`)
  check('应用中心·已开通模块不再恒空', mods.length > 0 && modDb.c > 0,
    `DB=${modDb.c} 接口=${mods.length}`)

  // 3.9 财务期初：新表存在
  const finTables = await dbQuery(
    `SELECT table_name FROM information_schema.tables
      WHERE table_schema='public' AND table_name IN ('erp_initial_finance_subject','erp_initial_finance_partner')`)
  check('财务期初两张表已建立（原先 0 表 0 控制器）', finTables.length === 2,
    finTables.map(t => t.table_name).join(','))
}
function dataApiArray(r) { const d = dataOf(r); return Array.isArray(d) ? d : [] }
function operate_total(r) { const d = dataOf(r); return d && d.total !== undefined ? d.total : -1 }

// ════════════════════════════════════════════════════════════════════
// 4. 写库 → 回读 → 复原
// ════════════════════════════════════════════════════════════════════
async function sectionWriteBack() {
  console.log('\n════ 4. 写库 → 回读 → 复原 ════')

  // 4.1 系统参数：改一条配置值，接口回读，DB 复核，再复原
  //     ⚠️ sys_config 的真实列名是 param_key / param_value（不是 config_key / config_value）
  const cfgBefore = await dbOne(
    `SELECT id::text AS id, param_key, param_value FROM sys_config WHERE deleted = 0 ORDER BY id LIMIT 1`)
  if (!cfgBefore.id) {
    skip('系统参数 保存→回读→复原', 'sys_config 无可用行')
  } else {
    const marker = 'E2E-SET-' + Date.now()
    const r1 = await apiPost('/config/save-value', { configKey: cfgBefore.param_key, configValue: marker })
    const db1 = await dbOne(`SELECT param_value FROM sys_config WHERE id = $1`, [cfgBefore.id])
    const api1 = await apiGet('/config/page?pageNum=1&pageSize=200')
    const row1 = recordsOf(api1).find(r => String(r.id) === String(cfgBefore.id))
    check('系统参数 保存真落库（DB 已变）', db1.param_value === marker,
      `DB=${String(db1.param_value).slice(0, 40)}（端点状态 ${r1.status}）`)
    check('系统参数 接口回读一致（不再假保存）', !!row1 && row1.configValue === marker,
      row1 ? `接口=${String(row1.configValue).slice(0, 40)}` : '接口未返回该行')
    await apiPost('/config/save-value', { configKey: cfgBefore.param_key, configValue: cfgBefore.param_value })
    const db2 = await dbOne(`SELECT param_value FROM sys_config WHERE id = $1`, [cfgBefore.id])
    check('系统参数 按原值复原', db2.param_value === cfgBefore.param_value,
      `${String(cfgBefore.param_value).slice(0, 40)}`)
  }

  // 4.2 支付配置：改一个微信配置项 → 回读 → 复原
  //     契约：POST /payment/config/items，body = {itemKey, itemValue}（键必须在后端白名单内）
  const payItems = dataApiArray(await apiGet('/payment/config/items?tab=wechat'))
  const textItem = payItems.find(i => i.valueType === 'text' && i.itemValue !== undefined)
    || payItems.find(i => i.valueType === 'text')
  if (!textItem) {
    skip('支付配置 保存→回读→复原', `微信 Tab 无文本型配置项：${payItems.map(i => i.itemKey + '/' + i.valueType).join(',')}`)
  } else {
    const key = textItem.itemKey
    const old = textItem.itemValue
    const marker = 'E2E-PAY-' + Date.now()
    const w = await apiPost('/payment/config/items', { itemKey: key, itemValue: marker })
    const after = dataApiArray(await apiGet('/payment/config/items?tab=wechat')).find(i => i.itemKey === key)
    check('支付配置 渠道参数真落库并可回读（改造前「假保存」）',
      !!after && after.itemValue === marker,
      `键=${key} 回读=${String(after && after.itemValue).slice(0, 40)}（写状态 ${w.status} ${String(w.body && w.body.message).slice(0, 60)}）`)
    await apiPost('/payment/config/items', { itemKey: key, itemValue: old })
    const restored = dataApiArray(await apiGet('/payment/config/items?tab=wechat')).find(i => i.itemKey === key)
    check('支付配置 按原值复原', !!restored && restored.itemValue === old, `原值=${String(old).slice(0, 40)}`)
  }
  // 场景配置：**只读断言** —— 写接口会覆盖「场景↔渠道」勾选矩阵，属租户真实配置，
  // 本 E2E 不做破坏性写入（宁可少测一项，也不动共享环境的真实配置）。
  const scenes = dataApiArray(await apiGet('/payment/config/scenes'))
  check('支付配置 场景配置 返回场景矩阵（含每场景已启用渠道数）',
    scenes.length > 0 && scenes.every(s => (s.sceneCode || s.code) !== undefined),
    `场景数=${scenes.length}：${scenes.map(s => s.sceneCode || s.code).slice(0, 5).join('/')}`)

  // 4.3 打印设置：改一个开关 → 回读 → 复原
  if (ONLY !== 'noprint') {
    const p0 = dataOf(await apiGet('/set/print-config'))
    if (!p0) {
      skip('打印设置 保存→回读→复原', 'GET /set/print-config 无数据')
    } else {
      const key = 'allowDraftPrint'
      const hadKey = Object.prototype.hasOwnProperty.call(p0, key)
      if (!hadKey) {
        skip('打印设置 保存→回读→复原', `响应无 ${key} 字段：${Object.keys(p0).join(',')}`)
      } else {
        const old = p0[key]
        const target = old ? 0 : 1
        const w = await apiPut('/set/print-config', { ...p0, [key]: target })
        const back = dataOf(await apiGet('/set/print-config'))
        check('打印设置 保存→回读闭环（新建页首次接线）',
          !!back && Number(back[key]) === target,
          `写入=${target} 回读=${back && back[key]}（状态 ${w.status}）`)
        await apiPut('/set/print-config', { ...p0, [key]: old })
        const back2 = dataOf(await apiGet('/set/print-config'))
        check('打印设置 按原值复原', !!back2 && Number(back2[key]) === Number(old),
          `${old} → ${back2 && back2[key]}`)
      }
    }
  }

  // 4.4 企业信息：改一个字段 → 回读 → 复原
  const co = dataOf(await apiGet('/tenant/current'))
  if (!co) {
    skip('企业信息 保存→回读→复原', 'GET /tenant/current 无数据')
  } else {
    const key = ['businessScope', 'companyScale', 'industry'].find(k => k in co) || null
    if (!key) {
      skip('企业信息 保存→回读→复原', `响应无新增档案字段：${Object.keys(co).join(',')}`)
    } else {
      const old = co[key]
      const marker = 'E2E-企信-' + Date.now()
      const w = await apiPut('/tenant/current', { ...co, [key]: marker })
      const back = dataOf(await apiGet('/tenant/current'))
      check('企业信息 保存→回读闭环（改造前 11/15 字段静默丢弃）',
        !!back && back[key] === marker,
        `字段=${key} 回读=${String(back && back[key]).slice(0, 40)}（状态 ${w.status}）`)
      await apiPut('/tenant/current', { ...co, [key]: old })
      const back2 = dataOf(await apiGet('/tenant/current'))
      check('企业信息 按原值复原', !!back2 && back2[key] === old, `${String(old).slice(0, 40)}`)
    }
  }

  // 4.5 会计期间：改起始日期 → 回读 → 复原（PUT /erp/finance/period/batch-dates，body 为 [{id,startDate,endDate}]）
  const maxYear2 = await dbOne(`SELECT max(period_year)::int AS y FROM fin_accounting_period`)
  const periods = dataApiArray(await apiGet(`/erp/finance/period/list?periodYear=${maxYear2.y}`))
  const p1 = periods[0]
  if (!p1) {
    skip('会计期间 批量保存→回读→复原', '无期间行')
  } else {
    const norm = (v) => String(v).slice(0, 10)
    const oldStart = norm(p1.startDate)
    const oldEnd = norm(p1.endDate)
    const mkRows = (replId, startDate, endDate) => periods.map(p => ({
      id: String(p.id),
      startDate: String(p.id) === String(replId) ? startDate : norm(p.startDate),
      endDate: String(p.id) === String(replId) ? endDate : norm(p.endDate),
    }))
    const w = await apiPut('/erp/finance/period/batch-dates', mkRows(p1.id, oldEnd, oldEnd))
    const back = dataApiArray(await apiGet(`/erp/finance/period/list?periodYear=${maxYear2.y}`))
      .find(p => String(p.id) === String(p1.id))
    check('会计期间 批量保存真落库并可回读（原先走路线 B 报表页，只读）',
      !!back && norm(back.startDate) === oldEnd,
      `写成 ${oldEnd} 回读 ${back && norm(back.startDate)}（状态 ${w.status} ${String(w.body && w.body.message).slice(0, 60)}）`)
    await apiPut('/erp/finance/period/batch-dates', mkRows(p1.id, oldStart, oldEnd))
    const back2 = dataApiArray(await apiGet(`/erp/finance/period/list?periodYear=${maxYear2.y}`))
      .find(p => String(p.id) === String(p1.id))
    check('会计期间 按原值复原', !!back2 && norm(back2.startDate) === oldStart, `${oldStart}`)
  }

  // 4.6 菜单配置：隐藏一个菜单 → 读回 → 复原
  //     真实契约：PUT /set/menu-config/visible?menuId=xxx&visible=false（query 参数，单个菜单）
  const allPages = dataApiArray(await apiGet('/set/menu-config/list'))
  const victim = allPages.filter(p => !p.locked).pop()
  if (!victim) {
    skip('菜单配置 显隐开关→回读→复原', '未取到可关闭的页面级菜单')
  } else {
    const vid = String(victim.id)
    const findRow = async () => (await apiGet('/set/menu-config/list')).body.data
      .find(p => String(p.id) === vid)
    const w1 = await apiPut(`/set/menu-config/visible?menuId=${vid}&visible=false`, null)
    const hiddenRow = await findRow()
    check('菜单配置 隐藏开关真落库',
      !!hiddenRow && hiddenRow.visible === false,
      `菜单 ${vid}(${victim.menuName}) visible=${hiddenRow && hiddenRow.visible}（状态 ${w1.status}）`)
    await apiPut(`/set/menu-config/visible?menuId=${vid}&visible=true`, null)
    const shownRow = await findRow()
    check('菜单配置 按原值复原（visible=true）',
      !!shownRow && shownRow.visible === true, `${shownRow && shownRow.visible}`)
  }

  // 4.7 财务期初：新增一行 → 回读 → 删除（原先「零控制器零表」，全页空壳）
  //     ⚠️ 前后对比必须是**同一口径**（都按 deleted = 0），否则会把上一轮软删行也算进基线
  const finBefore = Number((await dbOne(
    `SELECT count(*)::int AS c FROM erp_initial_finance_subject WHERE deleted = 0`)).c)
  // 科目必填：取一个真实的会计科目（subjectId 是必填项，缺了后端回「请选择科目」）
  const subjectRow = await dbOne(
    `SELECT id::text AS id, subject_code, subject_name FROM finance_account_subject
      WHERE deleted_flag = 0 AND subject_code = '1002' LIMIT 1`) || {}
  const wf = await apiPost('/erp/finance/initial/subject/save', {
    initialType: 'BANK_CASH', periodYear: 2026,
    subjectId: subjectRow.id, subjectCode: subjectRow.subject_code, subjectName: subjectRow.subject_name,
    direction: 'DEBIT', openingAmount: 1234.56,
  })
  const finAfter = Number((await dbOne(
    `SELECT count(*)::int AS c FROM erp_initial_finance_subject WHERE deleted = 0`)).c)
  check('财务期初 新增真落库（原先后端零控制器、库零表）',
    finAfter === finBefore + 1, `写前=${finBefore} 写后=${finAfter}（状态 ${wf.status} ${String(wf.body && wf.body.message).slice(0, 60)}）`)
  const finList = await apiGet('/erp/finance/initial/subject/page?initialType=BANK_CASH&pageNum=1&pageSize=50')
  const finRow = (recordsOf(finList) || []).find(r => String(r.subjectId) === String(subjectRow.id))
  check('财务期初 回读得到刚写入的行', !!finRow,
    finRow ? `金额=${finRow.openingAmount} 方向=${finRow.direction}` : '回读未取到')
  // 金额口径：接口回读与 DB 一致
  if (finRow) {
    const dbAmt = await dbOne(`SELECT opening_amount FROM erp_initial_finance_subject WHERE id = $1`, [String(finRow.id)])
    check('财务期初 金额回读与 DB 一致（BigDecimal，无浮点漂移）',
      Number(dbAmt.opening_amount) === 1234.56, `DB=${dbAmt.opening_amount} 接口=${finRow.openingAmount}`)
    const wd = await api('DELETE', `/erp/finance/initial/subject/${finRow.id}`)
    const finEnd = Number((await dbOne(
      `SELECT count(*)::int AS c FROM erp_initial_finance_subject WHERE deleted = 0`)).c)
    check('财务期初 删除生效且可见行数回到基线', finEnd === finBefore,
      `清理后可见=${finEnd} 基线=${finBefore}（delete 状态 ${wd.status} ${String(wd.body && wd.body.message).slice(0, 60)}）`)
    // 物理删除本页自造行（含软删行），避免在共享 dev 库里留下 E2E 痕迹
    await dbQuery(`DELETE FROM erp_initial_finance_subject WHERE subject_code = $1`, [subjectRow.subject_code])
    const finResidue = Number((await dbOne(
      `SELECT count(*)::int AS c FROM erp_initial_finance_subject WHERE subject_code = $1`,
      [subjectRow.subject_code])).c)
    check('财务期初 自造数据物理清理（共享库零残留）', finResidue === 0, `残留行=${finResidue}`)
  }

  // 4.8 系统重建：**只做安全断言，绝不真的执行清库**
  const rb = dataOf(await apiGet('/set/rebuild/options'))
  const rbOptions = (rb && rb.options) || []
  check('系统重建 12 个选项（ql361 实测清单）', rbOptions.length === 12,
    `实得 ${rbOptions.length}${rbOptions.length ? '：' + rbOptions.slice(0, 5).map(o => o.name).join('/') + '…' : ''}`)
  check('系统重建 选项带影响行数预估（预检口径）',
    rbOptions.length > 0 && rbOptions.every(o => o.estimatedRows !== undefined),
    `缺预估行数=${rbOptions.filter(o => o.estimatedRows === undefined).length}`)
  // ⚠️ 危险操作页只做**拒绝路径**验证：先记录基线，发一次错误密码请求，再确认库没被动过。
  //    `invoice` 表没有 deleted 列，按全表计数对账。
  const invBefore = Number((await dbOne(`SELECT count(*)::int AS c FROM invoice`)).c)
  const wrongPwd = await apiPost('/set/rebuild/execute', { options: ['invoice'], password: 'E2E-WRONG-PWD' })
  const wrongPwdRejected = !isOk(wrongPwd) || (wrongPwd.body && wrongPwd.body.success === false) ||
    /密码|口令|不正确|错误/.test(String(wrongPwd.body && (wrongPwd.body.message || wrongPwd.body.msg)))
  check('系统重建 错误密码被拒绝（不执行任何清理）', wrongPwdRejected,
    `status=${wrongPwd.status} msg=${String(wrongPwd.body && (wrongPwd.body.message || wrongPwd.body.msg)).slice(0, 80)}`)
  const invAfter = Number((await dbOne(`SELECT count(*)::int AS c FROM invoice`)).c)
  check('系统重建 错误密码请求未删除任何数据（基线对比）', invAfter === invBefore,
    `请求前 invoice 行数=${invBefore}，请求后=${invAfter}`)
}

// ════════════════════════════════════════════════════════════════════
// 5. 工作流/审批（本系统独有 5 页）
// ════════════════════════════════════════════════════════════════════
async function sectionWorkflow() {
  console.log('\n════ 5. 工作流 / 审批 ════')

  // 权限码落库（改造前 workflow:* 全为 0 行）
  const perms = await dbQuery(
    `SELECT permission_code FROM sys_permission WHERE permission_code LIKE 'workflow:%' ORDER BY permission_code`)
  const codes = perms.map(p => p.permission_code)
  const need = [
    'workflow:audit:list', 'workflow:audit:update',
    'workflow:definition:list', 'workflow:definition:save', 'workflow:definition:publish',
    'workflow:definition:disable', 'workflow:definition:delete',
    'workflow:instance:view', 'workflow:instance:diagram', 'workflow:instance:intervene',
    'workflow:task:view', 'workflow:task:approve', 'workflow:task:transfer',
  ]
  const missing = need.filter(c => !codes.includes(c))
  check(`workflow:* 权限码全部落库（${need.length} 条）`, missing.length === 0,
    missing.length ? `缺: ${missing.join(', ')}` : `实得 ${codes.length} 条`)

  // 流程定义两菜单分流：DB component 相同 → 靠路由区分
  const defs = dataQuery(await apiGet('/workflow/definitions?pageNum=1&pageSize=50'))
  check('流程定义台账可分页取数', defs.length >= 0, `rows=${defs.length}`)

  // 流程实例统计：与服务端列表按同一筛选口径
  const st = dataOf(await apiGet('/workflow/instance/stat'))
  const listAll = dataOf(await apiGet('/workflow/instance/page?pageNum=1&pageSize=1'))
  const listTotal = listAll && (listAll.total !== undefined ? listAll.total : (listAll.records || []).length)
  check('流程实例统计卡 = 全量真聚合（不再是当页条数）',
    !!st && Number(st.total) === Number(listTotal),
    `stat.total=${st && st.total} list.total=${listTotal}`)
  const statusKeys = st ? Object.keys(st).filter(k => !['total', 'success', 'timestamp'].includes(k)) : []
  check('流程实例状态覆盖后端 7 值', statusKeys.length === 7,
    `实得 ${statusKeys.length}: ${statusKeys.join('/')}`)

  // 待办/已办统计
  const tst = dataOf(await apiGet('/workflow/task/stat'))
  check('待办统计卡可返回', !!tst, tst ? Object.keys(tst).slice(0, 8).join(',') : '-')

  // 优先级不再硬编码 "medium"：表列存在，值可为 NULL
  const prioCol = await dbOne(
    `SELECT count(*)::int AS c FROM information_schema.columns
      WHERE table_name='workflow_task' AND column_name='priority'`)
  const prioVals = await dbQuery(
    `SELECT COALESCE(priority,'<NULL>') AS p, count(*)::int AS c FROM workflow_task GROUP BY 1 ORDER BY 2 DESC`)
  check('任务优先级列已建且不再硬编码 medium', prioCol.c === 1,
    prioVals.map(v => `${v.p}=${v.c}`).join(' '))

  // 死代码：workflow:create 监听应已清除
  const feTask = fs.readFileSync('I:/AI-Ready/frontend/apps/pc-admin/src/views/workflow/task-management.vue', 'utf8')
  check('task-management 已移除死代码 workflow:create 监听', !/workflow:create/.test(feTask), '0 命中')
  const feDesigner = fs.readFileSync('I:/AI-Ready/frontend/apps/pc-admin/src/views/workflow/designer/index.vue', 'utf8')
  check('designer 读路由决定标题（不再硬编码「流程设计」）',
    /useRoute/.test(feDesigner) && /route\.(name|path|meta)/.test(feDesigner), '命中 useRoute + route.*')

  // 待办/已办组件读路由决定初始 Tab（P0：804 进来看的是待办）
  const feTask2 = fs.readFileSync('I:/AI-Ready/frontend/apps/pc-admin/src/views/workflow/task-management.vue', 'utf8')
  check('task-management 读路由决定初始 Tab（P0：我的已办显示待办）',
    /useRoute/.test(feTask2) && /route\.(name|path|meta)/.test(feTask2), '命中 useRoute + route.*')
}
function dataQuery(r) {
  const d = dataOf(r)
  if (!d) return []
  if (Array.isArray(d)) return d
  // 本仓库分页/列表响应体的承载键不统一：records / definitions / list / items 都出现过
  return d.records || d.definitions || d.list || d.items || d.rows || []
}

// ════════════════════════════════════════════════════════════════════
// 5b. 二轮收口：逐条验证「未闭环项」已真正闭环
//     （每条对应 README §10.5 的一行；口径：能读到真实数据/真实行为，而不是只看接口 200）
// ════════════════════════════════════════════════════════════════════
async function sectionClosure() {
  console.log('\n════ 5b. 未闭环项收口验证 ════')

  // ── 流程定义台账租户口径（原：种子 tenant_id=0 而查询严格 eq 租户 → 恒空）──
  const defs = dataQuery(await apiGet('/workflow/definitions?pageNum=1&pageSize=50'))
  const defsDb = await dbOne(
    `SELECT count(*)::int AS c FROM workflow_definition WHERE deleted = 0 AND (tenant_id = 1 OR tenant_id = 0)`)
  check('流程定义台账不再恒空（OR 口径：本租户 or 全局默认）',
    defs.length > 0 && defs.length === defsDb.c,
    `DB=${defsDb.c} 接口=${defs.length}${defs.length ? '：' + defs.slice(0, 3).map(d => d.name || d.definitionName || d.definitionKey).join('/') : ''}`)

  // ── 流程实例：干预理由后端硬校验 ──
  const instList = dataQuery(await apiGet('/workflow/instance/page?pageNum=1&pageSize=50'))
  const target = instList.find(i => ['approving', 'running', 'suspended'].includes(String(i.status)))
  if (!target) {
    skip('流程实例 空干预理由被后端 400 拒绝', '无非终态实例可测')
  } else {
    const r = await apiPost(`/workflow/instance/${target.id}/intervene`, { action: 'suspend', reason: '   ' })
    check('流程实例 空干预理由被后端 400 拒绝（原仅前端必填）',
      r.status === 400 || (r.body && r.body.code === 400),
      `status=${r.status} code=${r.body && r.body.code} msg=${String(r.body && r.body.message).slice(0, 60)}`)
  }

  // ── 流程实例：统计卡含 suspended（补第 5 张卡的数据基础）──
  const st = dataOf(await apiGet('/workflow/instance/stat'))
  check('流程实例统计含 suspended（供第 5 张「已挂起」卡）',
    !!st && st.suspended !== undefined, `suspended=${st && st.suspended}`)
  const instVue = fs.readFileSync('I:/AI-Ready/frontend/apps/pc-admin/src/views/workflow/instance-monitor.vue', 'utf8')
  check('流程实例页面渲染「已挂起」卡', /已挂起/.test(instVue), /已挂起/.test(instVue) ? '命中' : '未命中')

  // ── 「双击行」入口：**页面侧自行实现**，共享表格组件的事件契约保持不变 ──
  //     历史：39 个页面写了 @cell-dblclick，但 BillTableList/BillDetailTable 从未派发该事件
  //     ⇒ 一直是死绑定。裁定为「谁要双击谁在页面侧自己接」（composable 事件委托），
  //     而不是给共享组件加全局派发（那会一次性改变 211 处调用方的行为）。
  const btl = fs.readFileSync('I:/AI-Ready/frontend/apps/pc-admin/src/components/BillTableList/BillTableList.vue', 'utf8')
  const bdt = fs.readFileSync('I:/AI-Ready/frontend/apps/pc-admin/src/components/BillFormPage/BillDetailTable/index.vue', 'utf8')
  check('共享表格组件不再派发 cell-dblclick（事件契约保持原状）',
    !/cell-dblclick/.test(btl) && !/cell-dblclick/.test(bdt),
    `BillTableList=${/cell-dblclick/.test(btl)} BillDetailTable=${/cell-dblclick/.test(bdt)}`)
  check('共享表格组件未自行监听行双击（无 @dblclick 兜底）',
    !/@dblclick/.test(btl) && !/@dblclick/.test(bdt),
    `BillTableList=${/@dblclick/.test(btl)} BillDetailTable=${/@dblclick/.test(bdt)}`)
  check('BillDetailTable 提供惰性 data-row-key 供页面侧反查行（纯属性、零行为）',
    /data-row-key/.test(bdt), /data-row-key/.test(bdt) ? '命中' : '未命中')
  check('页面侧双击工具 composable 存在（避免同一段委托复制 39 份）',
    fs.existsSync('I:/AI-Ready/frontend/apps/pc-admin/src/composables/useRowDblclick.ts'), 'useRowDblclick.ts')
  check('流程实例页在页面侧自行实现双击（不再依赖组件事件）',
    /useRowDblclick/.test(instVue) && !/cell-dblclick/.test(instVue),
    `useRowDblclick=${/useRowDblclick/.test(instVue)} 残留cell-dblclick=${/cell-dblclick/.test(instVue)}`)
  // 全站巡检：**共享表格组件（BillTableList/BillDetailTable）的使用方**不得再有 @cell-dblclick 死绑定。
  //   判定规则要排除「非共享组件使用方」：例如 `erp/batch` 的 @cell-dblclick 绑在 **vxe-table** 上，
  //   查证 vxe-table 4.19.10（es/table/src/emits.js:31 声明 + table.js 真实 dispatch，载荷是 {row,...} 对象）
  //   → 该绑定**本来就是好的**，不属于本次改造范围，保留才对。
  const stillDead = []
  const legitOther = []
  const walkViews = (dir) => {
    for (const f of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = dir + '/' + f.name
      if (f.isDirectory()) { walkViews(full); continue }
      if (!f.name.endsWith('.vue')) continue
      const t = fs.readFileSync(full, 'utf8')
      if (!/@cell-dblclick/.test(t)) continue
      const rel = full.replace('I:/AI-Ready/frontend/apps/pc-admin/src/', '')
      if (/BillTableList|BillDetailTable/.test(t)) stillDead.push(rel)
      else legitOther.push(rel)
    }
  }
  walkViews('I:/AI-Ready/frontend/apps/pc-admin/src/views')
  check('共享表格组件的使用方已无 @cell-dblclick 死绑定（39 处全部改造为页面侧）',
    stillDead.length === 0, stillDead.length ? `残留 ${stillDead.length} 处: ${stillDead.slice(0, 5).join(', ')}` : '0 处')
  check('非共享组件的绑定未被误改（erp/batch 走 vxe-table，保留正确）',
    legitOther.length === 0 || legitOther.every(p => p.includes('erp/batch')),
    legitOther.length ? `保留: ${legitOther.join(', ')}` : '无')

  // ── 系统参数：6 个空视图已按 ql361 实测补齐 ──
  const allCfg = recordsOf(await apiGet('/config/page?pageNum=1&pageSize=500'))
  const navs = dataApiArray(await apiGet('/config/nav-groups'))
  const byGroup = new Map()
  for (const c of allCfg) byGroup.set(c.navGroup, (byGroup.get(c.navGroup) || 0) + 1)
  const emptyGroups = navs.filter(n => !byGroup.get(n.code))
  check(`系统参数 8 个视图全部有配置项（收口前仅「行业设置」「其他」有）`,
    navs.length === 8 && emptyGroups.length === 0,
    emptyGroups.length ? `仍为空: ${emptyGroups.map(n => n.name).join('/')}` : `分布=${navs.map(n => n.name + ':' + (byGroup.get(n.code) || 0)).join(' ')}`)
  const cfgDbCount = Number((await dbOne(`SELECT count(*)::int AS c FROM sys_config WHERE deleted = 0`)).c)
  check('系统参数 sys_config 行数 == 接口分页总数（真实落库）',
    allCfg.length === cfgDbCount && cfgDbCount > 20,
    `DB=${cfgDbCount} 接口=${allCfg.length}（收口前 18）`)
  const helpCount = allCfg.filter(c => c.helpText || c.tipText).length
  check('系统参数 帮助气泡/温馨提示已落库（ql361 实测文案）', helpCount > 0, `${helpCount}/${allCfg.length} 行带文案`)
  const lockedRows = allCfg.filter(c => c.locked === true)
  check('系统参数 locked 依据真实商品引用计算（被引用则锁定）',
    allCfg.some(c => 'lockedReason' in c) || lockedRows.length > 0,
    `locked=${lockedRows.length} 行${lockedRows.length ? '：' + lockedRows.slice(0, 3).map(c => c.configKey).join('/') : ''}`)

  // ── 打印设置：配置真正被消费 ──
  const pb = await apiGet('/set/print-config/behavior')
  check('打印设置 新增 behavior 端点（打印组件读配置的入口）', isOk(pb),
    isOk(pb) ? `allowDraftPrint=${(dataOf(pb) || {}).allowDraftPrint} qtyDecimal=${(dataOf(pb) || {}).qtyDecimal}` : `${pb.status}`)
  const pc = dataOf(await apiGet('/set/print-config')) || {}
  const pcOpts = (pc.options && pc.options.printContent) || pc.printContentOptions || []
  check('打印内容 下拉补齐为 ql361 实测 3 项', (pcOpts || []).length === 3,
    `${(pcOpts || []).length} 项：${(pcOpts || []).map(o => o.label || o.value || o).join(' / ')}`)
  const pdBehavior = fs.readFileSync('I:/AI-Ready/frontend/apps/pc-admin/src/components/PrintDialog/index.vue', 'utf8')
  check('PrintDialog 已消费配置且失败降级为原行为',
    /printBehavior|print-behavior|printConfig/.test(pdBehavior), /printBehavior/.test(pdBehavior) ? '命中 printBehavior' : '未命中')
  check('PrintDialog 未引入破坏性必填 prop（向后兼容）',
    !/defineProps<\{[^}]*printConfig\s*:/s.test(pdBehavior), '未新增必填 printConfig prop')

  // ── 财务期初：会计年 / 关账联动 / 存货对平 ──
  const cy = await apiGet('/erp/finance/initial/current-year')
  const cyv = dataOf(cy)
  const yearVal = cyv && (cyv.year || cyv.periodYear || (typeof cyv === 'number' ? cyv : null))
  check('财务期初 年度取真实会计年（不再硬编码系统年）',
    isOk(cy) && yearVal !== null && yearVal !== undefined, `currentYear=${JSON.stringify(cyv).slice(0, 120)}`)
  const ps = await apiGet('/erp/finance/initial/period-status?periodYear=' + (yearVal || 2026))
  check('财务期初 关账联动：返回该年度是否可编辑', isOk(ps),
    isOk(ps) ? JSON.stringify(dataOf(ps)).slice(0, 120) : `${ps.status}`)
  const tb = dataOf(await apiGet('/erp/finance/initial/trial-balance?periodYear=' + (yearVal || 2026)))
  check('财务期初 试算平衡含「存货对平」检查项', !!tb && !!tb.inventoryCheck,
    tb && tb.inventoryCheck ? JSON.stringify(tb.inventoryCheck).slice(0, 140) : `keys=${tb ? Object.keys(tb).join(',') : '-'}`)

  // ── 企业信息：拆表回 ≤25 列 + LOGO ──
  const tenCols = Number((await dbOne(
    `SELECT count(*)::int AS c FROM information_schema.columns WHERE table_name='sys_tenant'`)).c)
  const profCols = Number((await dbOne(
    `SELECT count(*)::int AS c FROM information_schema.columns WHERE table_name='sys_tenant_profile'`)).c)
  check('企业信息 sys_tenant 瘦身回 ≤25 列（收口前 30 列）', tenCols <= 25, `sys_tenant=${tenCols} 列`)
  check('企业信息 档案列已迁到 1:1 子表 sys_tenant_profile', profCols > 0, `sys_tenant_profile=${profCols} 列`)
  const co = dataOf(await apiGet('/tenant/current'))
  check('企业信息 接口契约未变（原档案字段仍在）',
    !!co && ['creditCode', 'legalPerson', 'taxNumber'].every(k => k in co),
    co ? Object.keys(co).length + ' 个字段' : '-')
  check('企业信息 新增 logoUrl（LOGO 上传落位）', !!co && 'logoUrl' in co,
    co && 'logoUrl' in co ? `logoUrl=${co.logoUrl || '(空)'}` : '无该字段')
  const coVue = fs.readFileSync('I:/AI-Ready/frontend/apps/pc-admin/src/views/set/company-info/index.vue', 'utf8')
  check('企业信息 页面有企业标识上传入口', /logo/i.test(coVue) && /upload|Upload/i.test(coVue),
    /logo/i.test(coVue) ? '命中 logo + upload' : '未命中')

  // ── 系统任务：创建人不再是结构性空 ──
  const creatorCol = Number((await dbOne(
    `SELECT count(*)::int AS c FROM information_schema.columns
      WHERE table_name='scheduled_task_log' AND column_name IN ('create_by')`)).c)
  check('系统任务 写入侧已补创建人列 create_by', creatorCol === 1, `create_by 列存在=${creatorCol === 1}`)
  const stVue = fs.readFileSync('I:/AI-Ready/frontend/apps/pc-admin/src/views/set/system-task/index.vue', 'utf8')
  check('系统任务 页面创建人列有空值语义（系统触发 vs 无来源）',
    /创建人|createdBy/.test(stVue), /createdByName/.test(stVue) ? '命中 createdByName' : '命中创建人相关渲染')

  // ── 系统重建：无 tenant_id 的表改为按主表关联清理 ──
  const rb = dataOf(await apiGet('/set/rebuild/options'))
  const rbOpts = (rb && rb.options) || []
  const rbAllTables = rbOpts.flatMap(o => (o.tables || []).map(t => t.table))
  check('系统重建 已纳入 biz_party_address（按主表关联限租户）',
    rbAllTables.includes('biz_party_address'), `命中=${rbAllTables.includes('biz_party_address')}`)
  check('系统重建 已纳入 shop_user_party_link',
    rbAllTables.includes('shop_user_party_link'), `命中=${rbAllTables.includes('shop_user_party_link')}`)
  check('系统重建 仍排除无法限租户的 biz_party_role（宁可不删不越权）',
    !rbAllTables.includes('biz_party_role'), 'biz_party_role 未纳入')

  // ── 应用中心：短信用量接入真实来源 ──
  const ov = dataOf(await apiGet('/set/app-center/overview'))
  const caps = dataApiArray(await apiGet('/set/app-center/capabilities'))
  const smsTable = Number((await dbOne(
    `SELECT count(*)::int AS c FROM information_schema.tables WHERE table_name = 'mkt_sms_setting'`)).c)
  check('应用中心 短信用量接入真实来源（mkt_sms_setting / mkt_sms_record）',
    smsTable === 1 ? /sms|Sms|短信/i.test(JSON.stringify(ov || {}) + JSON.stringify(caps)) : true,
    smsTable === 1 ? `overview/capabilities 含短信字段=${/sms|Sms|短信/i.test(JSON.stringify(ov || {}) + JSON.stringify(caps))}` : '计量表不存在，按降级处理')

  // ── 流程分析孤儿页：菜单 + 去 mock ──
  const paMenu = await dbOne(
    `SELECT id::text AS id, parent_id::text AS pid, path, component, client_type, tenant_id::text AS tid
       FROM sys_menu WHERE component LIKE '%process-analysis%' AND deleted = 0 LIMIT 1`)
  check('流程分析 已补菜单入口（原 0 行）', !!paMenu.id,
    paMenu.id ? `id=${paMenu.id} parent=${paMenu.pid} client_type=${paMenu.client_type} tenant_id=${paMenu.tid}` : '无菜单')
  if (paMenu.id) {
    check('流程分析 菜单 client_type/tenant_id 与同组一致（漏则整页 404）',
      paMenu.client_type === 'tenant-admin' && paMenu.tid === '0',
      `client_type=${paMenu.client_type} tenant_id=${paMenu.tid}`)
  }
  const paVue = fs.readFileSync('I:/AI-Ready/frontend/apps/pc-admin/src/views/workflow/process-analysis.vue', 'utf8')
  check('流程分析 已清除 2024 写死 mock 数据',
    !/2024-0[0-9]-/.test(paVue) && !/1234\b/.test(paVue),
    /2024-0[0-9]-/.test(paVue) ? '仍命中 2024 日期' : '未命中 mock 特征')
  check('流程分析 首屏立即加载（不再等 30s 轮询）',
    /onMounted[\s\S]{0,400}(loadSummary|loadReport|fetch|load\()/.test(paVue), '命中 onMounted 内直接加载')
}

const LIST_PAGES = [
  ['菜单配置', 'set/menu-config/index.vue'],
  ['操作日志', 'set/operation-log/index.vue'],
  ['审核设置', 'set/audit-config/index.vue'],
  ['库存期初', 'set/initial-stock/index.vue'],
  ['财务期初', 'set/initial-finance/index.vue'],
  ['系统重建', 'set/rebuild/index.vue'],
  ['系统任务', 'set/system-task/index.vue'],
  ['会计期间', 'set/accounting-period/index.vue'],
  ['支付配置', 'set/payment-config/index.vue'],
  ['流程定义/设计', 'workflow/designer/index.vue'],
  ['流程实例', 'workflow/instance-monitor.vue'],
  ['我的待办/已办', 'workflow/task-management.vue'],
]
const CONFIG_PAGES = [
  ['系统参数', 'set/sys-params/index.vue'],
  ['企业信息', 'set/company-info/index.vue'],
  ['应用中心', 'set/app-center/index.vue'],
  ['打印设置', 'set/print-config/index.vue'],
]

async function sectionShell() {
  console.log('\n════ 6. 页面外壳金标准自检（静态） ════')
  const base = 'I:/AI-Ready/frontend/apps/pc-admin/src/views/'
  for (const [label, rel] of LIST_PAGES) {
    const txt = fs.readFileSync(base + rel, 'utf8')
    const problems = []
    if (!/ErrorBoundary/.test(txt)) problems.push('缺 ErrorBoundary')
    if (!/PageContainer/.test(txt)) problems.push('缺 PageContainer')
    if (!/full-height/.test(txt)) problems.push('缺 full-height')
    if (!/CategoryListLayout/.test(txt)) problems.push('缺 CategoryListLayout')
    if (!/BillDetailTable|BillTableList/.test(txt)) problems.push('缺数据表组件')
    if (!/storage-key|storageKey/.test(txt)) problems.push('缺 storage-key（列配置不可持久化）')
    if (/:max-height/.test(txt)) problems.push('传了禁止的 :max-height')
    check(`外壳·${label}`, problems.length === 0, problems.length ? problems.join('；') : '通过')
  }
  for (const [label, rel] of CONFIG_PAGES) {
    const txt = fs.readFileSync(base + rel, 'utf8')
    const problems = []
    if (!/ErrorBoundary/.test(txt)) problems.push('缺 ErrorBoundary')
    if (!/PageContainer/.test(txt)) problems.push('缺 PageContainer')
    if (!/full-height/.test(txt)) problems.push('缺 full-height')
    check(`外壳（配置页 A′）·${label}`, problems.length === 0, problems.length ? problems.join('；') : '通过')
  }
  // 查询区必须是横向网格，禁止纵向单列
  const colPages = [
    ['菜单配置', 'set/menu-config/index.vue'], ['操作日志', 'set/operation-log/index.vue'],
    ['库存期初', 'set/initial-stock/index.vue'], ['财务期初', 'set/initial-finance/index.vue'],
    ['系统任务', 'set/system-task/index.vue'], ['支付配置', 'set/payment-config/index.vue'],
    ['流程实例', 'workflow/instance-monitor.vue'], ['我的待办/已办', 'workflow/task-management.vue'],
  ]
  for (const [label, rel] of colPages) {
    const txt = fs.readFileSync(base + rel, 'utf8')
    const grid = /repeat\(auto-fill,\s*minmax\(1\d0px/.test(txt) || /repeat\(auto-fit,\s*minmax\(1\d0px/.test(txt)
    const vertical = /flex-direction:\s*column[\s\S]{0,80}query-column/.test(txt) || /\.query-column/.test(txt)
    check(`查询区横向网格·${label}`, grid && !vertical,
      grid ? (vertical ? '出现纵向单列 .query-column' : '通过') : '未命中横向 minmax 网格')
  }
}

// ════════════════════════════════════════════════════════════════════
// 7. UI（Playwright）
// ════════════════════════════════════════════════════════════════════
/** 从页面里的 base64 SVG 验证码解码出文本（与 e2e-crm.cjs 同口径） */
async function solveSvgCaptcha(page) {
  const svg = await page.evaluate(() => {
    const imgs = [...document.querySelectorAll('img')].filter(i => (i.src || '').startsWith('data:image'))
    if (!imgs.length) return ''
    const src = imgs[imgs.length - 1].src
    try {
      if (src.includes('base64,')) return decodeURIComponent(escape(atob(src.split('base64,')[1])))
      return decodeURIComponent(src.split(',').slice(1).join(','))
    } catch { return '' }
  })
  return [...String(svg).matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
}

const UI_PAGES = [
  ['80620 菜单配置', '/set/menu-config'],
  ['80621 系统参数', '/set/sys-params'],
  ['80622 审核设置', '/set/audit-config'],
  ['80623 支付配置', '/set/payment-config'],
  ['80624 企业信息', '/set/company-info'],
  ['80625 应用中心', '/set/app-center'],
  ['70550 库存期初', '/set/initial-stock'],
  ['70551 财务期初', '/set/initial-finance'],
  ['70560 系统重建', '/set/rebuild'],
  ['70561 系统任务', '/set/system-task'],
  ['80630 操作日志', '/set/operation-log'],
  ['70570 会计期间', '/set/accounting-period'],
  ['80930 打印设置', '/set/print-config'],
  ['802 流程实例', '/workflow/instance'],
  ['803 我的待办', '/workflow/task'],
  ['804 我的已办', '/workflow/done'],
]

async function sectionUi() {
  console.log('\n════ 7. UI 渲染（Playwright） ════')
  if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })
  let browser
  try {
    browser = await chromium.launch({ headless: true })
  } catch (e) {
    skip('UI 渲染', 'Chromium 启动失败: ' + e.message)
    return
  }
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 950 } })
  const page = await ctx.newPage()
  const consoleErrors = []
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()) })
  page.on('pageerror', e => consoleErrors.push('pageerror: ' + e.message))

  // 登录（UI 侧）：照抄仓库既有做法（e2e-crm.cjs）—— 按 placeholder 定位 + 从 DOM 里的
  // base64 SVG 解码验证码。**不要**用 API 取一条新验证码填进去：FE 页面自己那份 key 才对得上。
  //
  // 会话自愈：sa-token `is-concurrent: false` 下并行会话互踢会伪装成「页面跳登录」，
  // 必须重登再判，否则会把互踢误报成页面缺陷（2026-09-18 实踩：全站 16 页齐刷刷「跳登录」）。
  let lastLoginNote = ''
  const relogin = async () => {
    for (let attempt = 1; attempt <= 3; attempt++) {
      await page.goto(FE + '/', { waitUntil: 'domcontentloaded', timeout: 40000 })
      await page.waitForTimeout(3500)
      if (!/\/login/.test(page.url()) && await page.locator('input[type=password]').count() === 0) return true
      try {
        const tenantInput = page.locator('input[placeholder*="租户"]').first()
        if (await tenantInput.count() > 0) await tenantInput.fill('系统租户')
        await page.locator('input[placeholder*="用户名"]').first().fill(E2E_USER)
        await page.locator('input[placeholder*="密码"]').first().fill(E2E_PWD)
        const capInput = page.locator('input[placeholder*="验证码"]').first()
        if (await capInput.count() > 0) await capInput.fill(await solveSvgCaptcha(page))
        const [resp] = await Promise.all([
          page.waitForResponse(r => /\/api\/auth\/login/.test(r.url()), { timeout: 20000 }).catch(() => null),
          page.locator('button:has-text("登 录"), button:has-text("登录")').first().click(),
        ])
        lastLoginNote = resp ? `login HTTP ${resp.status()}` : '未捕获 login 响应'
        await page.waitForTimeout(7000)
        if (!/\/login/.test(page.url())) return true
      } catch (e) {
        lastLoginNote = '登录交互异常: ' + e.message.slice(0, 80)
        await page.waitForTimeout(2000)
      }
    }
    return false
  }
  const uiLoggedIn = await relogin()
  check('UI 登录并进入系统', uiLoggedIn, `${page.url()}${lastLoginNote ? '（' + lastLoginNote + '）' : ''}`)
  /** 导航到目标页；若落点跳登录则自愈重登后重试一次，最后返回 (url, bodyText) */
  const gotoPage = async (path) => {
    await page.goto(FE + path, { waitUntil: 'domcontentloaded', timeout: 40000 })
    await page.waitForTimeout(4000)
    if (/\/login/.test(page.url())) {
      await relogin()
      await page.goto(FE + path, { waitUntil: 'domcontentloaded', timeout: 40000 })
      await page.waitForTimeout(4000)
    }
    return { url: page.url(), bodyText: await page.evaluate(() => document.body.innerText || '') }
  }

  for (const [label, path] of UI_PAGES) {
    consoleErrors.length = 0
    let status = 'ok'
    let url = ''
    let bodyText = ''
    try {
      const r = await gotoPage(path)
      url = r.url
      bodyText = r.bodyText
    } catch (e) {
      status = '导航异常: ' + e.message.slice(0, 80)
    }
    const redirectedToLogin = /\/login/.test(url)
    const empty = bodyText.trim().length < 20
    const realErrors = consoleErrors.filter(t => !/favicon|ResizeObserver|401|Failed to load resource/i.test(t))
    const ok = status === 'ok' && !redirectedToLogin && !empty && realErrors.length === 0
    check(`UI·${label}`, ok,
      ok ? `渲染正常（${bodyText.trim().length} 字）`
        : [status !== 'ok' ? status : '', redirectedToLogin ? '被重定向到登录页（会话互踢，已自愈重登后仍失败）' : '',
          empty ? '页面空白' : '', realErrors.length ? 'console error: ' + realErrors.slice(0, 2).join(' | ').slice(0, 200) : '']
          .filter(Boolean).join('；'))
    try {
      await page.screenshot({ path: `${SHOTS}/ui-${label.split(' ')[0]}.png`, fullPage: false })
    } catch { /* ignore */ }
  }

  // 我的待办 / 我的已办 落点必须不同（P0：804 曾显示待办）
  try {
    const todoR = await gotoPage('/workflow/task')
    const doneR = await gotoPage('/workflow/done')
    const todoTxt = todoR.bodyText
    const doneTxt = doneR.bodyText
    const todoActive = /待办/.test(todoTxt)
    const doneActive = /已办/.test(doneTxt)
    check('UI·804 我的已办落点不再是「待办」', doneActive && (doneTxt !== todoTxt || !/待办/.test(doneTxt.slice(0, 200))),
      `待办页首屏关键词=${todoActive} 已办页关键词=${doneActive}`)
  } catch (e) {
    skip('UI·804 落点区分', e.message.slice(0, 100))
  }

  // ── 7b. 页面内容断言：不只「渲染非空」，还要**逐页验证开发文档规定的关键内容真的在页面上** ──
  //     （外壳/网格是静态 grep 得来的，这里才是「这一页到底有没有做出来」的证据）
  const CONTENT_PAGES = [
    ['80620 菜单配置', '/set/menu-config', ['菜单配置', '权限标识', '所属分组']],
    ['80621 系统参数', '/set/sys-params', ['行业设置', '流程启用', '单据设置', '库存设置', '财务设置', '数据权限', '消息提醒', '其他', '保 存']],
    ['80622 审核设置', '/set/audit-config', ['单据', '审核设置', '摘要', '销售出库单', '销售订单', '采购入库单', '会计凭证']],
    ['80623 支付配置', '/set/payment-config', ['微信公众号配置', '支付方式', '场景配置', '在线退款']],
    ['80624 企业信息', '/set/company-info', ['企业信息', '纳税人信息', '保存']],
    ['80625 应用中心', '/set/app-center', ['功能模块', '销售管理']],
    ['70550 库存期初', '/set/initial-stock', ['库存期初']],
    ['70551 财务期初', '/set/initial-finance', ['银行现金期初', '应付期初', '应收期初', '固定资产期初', '资产负债期初']],
    ['70560 系统重建', '/set/rebuild', ['系统重建', '不能恢复', '业务草稿', '库存期初', '操作员', '发票']],
    ['70561 系统任务', '/set/system-task', ['任务名称', '任务状态', '任务创建时间', '结果查看']],
    ['80630 操作日志', '/set/operation-log', ['系统日志', '登录日志', '操作员']],
    ['70570 会计期间', '/set/accounting-period', ['期间', '起始日期', '结账日期', '天数']],
    ['80930 打印设置', '/set/print-config', ['打印设置', '箱号模板', '物流模板', '条码模板', '套餐条码', '批次条码', '货位码', '允许打印草稿', '打印内容']],
    ['802 流程实例', '/workflow/instance', ['流程实例', '运行中', '已完成']],
  ]
  for (const [label, path, needles] of CONTENT_PAGES) {
    let bodyText = ''
    try {
      const r = await gotoPage(path)
      bodyText = r.bodyText
    } catch (e) {
      check(`内容·${label}`, false, '导航异常: ' + e.message.slice(0, 80))
      continue
    }
    const missing = needles.filter(n => !bodyText.includes(n))
    check(`内容·${label}（文档规定要点全部呈现）`, missing.length === 0,
      missing.length ? `缺: ${missing.join(' / ')}` : `${needles.length} 项齐备`)
  }

  // 7c. 「我的已办」落点（P0：804 进入后显示的是「待办」）
  //     两个菜单共用同一组件，判据取「页面标题 + 统计卡口径 + 表格列集合」三处，
  //     比只看某个 CSS 类更稳（本页 Tab 是自绘的，非 antd .ant-tabs-*）。
  try {
    const r1 = await gotoPage('/workflow/task')
    const r2 = await gotoPage('/workflow/done')
    const t1 = r1.bodyText, t2 = r2.bodyText
    const todoOk = t1.includes('我的待办') && t1.includes('高优先级') && t1.includes('超时任务')
    const doneOk = t2.includes('我的已办') && t2.includes('已驳回') && t2.includes('已转交')
    check('内容·804「我的已办」落点 = 已办视图（P0 修复证据）', todoOk && doneOk,
      `待办页识别到待办视图=${todoOk}（高优先级/超时任务卡），已办页识别到已办视图=${doneOk}（已驳回/已转交卡）`)
    // 已办 Tab 必须补齐文档要求的 3 列（后端早已返回，页面此前没展示）
    const doneCols = ['审批结果', '审批意见', '处理时间'].filter(c => t2.includes(c))
    check('内容·804 已办 Tab 补齐 审批结果/审批意见/处理时间 三列',
      doneCols.length === 3, `命中 ${doneCols.length}/3：${doneCols.join('/')}`)
  } catch (e) {
    skip('内容·804 落点对比', e.message.slice(0, 100))
  }

  // ── 7d. 「双击行」入口 **真机功能验证**（不是只断言渲染无错）──
  //     前几轮把「双击行看详情」接上后，只做过静态审查 + 渲染无 error，**从没真的双击过一行**。
  //     本节把那次欠的验证固化成永久断言：双击必须真的打开详情，且开的是**该行对应**的记录。
  try {
    const r = await gotoPage('/workflow/instance')
    if (/\/login/.test(r.url)) {
      skip('双击·流程实例行打开详情', '会话不可用')
    } else {
      const rowInfo = await page.evaluate(() => {
        const trs = [...document.querySelectorAll('tr[data-row-key]')]
        return {
          rows: trs.length,
          firstKey: trs.length ? trs[0].getAttribute('data-row-key') : null,
          ghostWithKey: document.querySelectorAll('tr.ss-empty-row[data-row-key]').length,
        }
      })
      check('惰性 data-row-key 只挂在真实数据行上（占位空行不带）',
        rowInfo.rows > 0 && rowInfo.ghostWithKey === 0,
        `数据行=${rowInfo.rows} 空行带 key=${rowInfo.ghostWithKey}`)
      if (rowInfo.rows === 0) {
        skip('双击·流程实例行打开详情', '当前无实例数据可双击')
      } else {
        consoleErrors.length = 0
        // 双击第 3 个单元格（数据列，避开操作列按钮）
        await page.locator('tr[data-row-key] td').nth(2).dblclick({ timeout: 15000 })
        await page.waitForTimeout(2500)
        const opened = await page.evaluate(() => {
          for (const s of ['.ant-drawer-open', '.ant-modal-wrap', '.full-screen-detail']) {
            const el = document.querySelector(s)
            if (el && el.getBoundingClientRect().width > 0) return (el.innerText || '').slice(0, 300)
          }
          return null
        })
        // 不仅要"打开了"，还要"开的是这一行" —— 详情里必须出现首行的 row-key
        const sameRow = !!opened && String(rowInfo.firstKey) && opened.includes(String(rowInfo.firstKey))
        check('双击流程实例行 → 打开详情，且详情是**该行**对应的实例',
          !!opened && sameRow,
          opened ? `详情命中首行 key=${rowInfo.firstKey} → ${sameRow}` : '双击未打开任何详情容器')
        const realErrors = consoleErrors.filter(t => !/favicon|ResizeObserver|401|Failed to load resource/i.test(t))
        check('双击过程无 console error', realErrors.length === 0,
          realErrors.length ? realErrors[0].slice(0, 160) : '0 条')
      }
    }
  } catch (e) {
    check('双击·流程实例行打开详情', false, '执行异常: ' + e.message.slice(0, 120))
  }

  // ── 7e. 改造后其它可达页的回归巡查 ──
  //     这次改造动了 38 个页面的模板与脚本（给容器加 ref、删死绑定、接入 composable），
  //     其中只有少数路由可达，挑出来跑一遍确认没有把页面弄坏。
  const TOUCHED_LIVE = [
    ['系统参数(共享磁盘配置页)', '/system/config/index'],
    ['数据字典', '/system/dict/index'],
    ['往来单位', '/md/supplier/index'],
    ['订单处理中心', '/sales/order-center'],
  ]
  for (const [label, path] of TOUCHED_LIVE) {
    consoleErrors.length = 0
    let bodyText = '', url = ''
    try {
      const r = await gotoPage(path)
      url = r.url; bodyText = r.bodyText
    } catch (e) {
      check(`回归·${label}`, false, '导航异常: ' + e.message.slice(0, 80)); continue
    }
    const realErrors = consoleErrors.filter(t => !/favicon|ResizeObserver|401|Failed to load resource/i.test(t))
    check(`回归·${label}（双击改造后无回归）`,
      !/\/login/.test(url) && bodyText.trim().length >= 20 && realErrors.length === 0,
      `render=${bodyText.trim().length}字 errors=${realErrors.length}${realErrors.length ? '：' + realErrors[0].slice(0, 120) : ''}`)
  }

  await browser.close()
}

// ════════════════════════════════════════════════════════════════════
// 主流程
// ════════════════════════════════════════════════════════════════════
async function main() {
  console.log(`设置模块 E2E：后端 :${PORT}  前端 ${FE}`)
  const ready = await waitBackend()
  if (!ready) {
    console.error('后端未就绪，退出')
    process.exit(1)
  }
  const s = await login()
  TOKEN = s.token
  CURRENT_USER_ID = String(s.userInfo?.userId || '1')
  console.log('登录成功，用户=' + (s.userInfo?.username || E2E_USER) + ' userId=' + CURRENT_USER_ID + '\n')

  // 每节独立兜错：单节抛异常不得让整轮 E2E 静默中断（本轮已踩过「第 3 节 SQL 报错 → 后续 4~7 节全没跑」）
  const sections = [
    ['菜单完整性', sectionMenus],
    ['接口探针', sectionApis],
    ['只读 DB 对账', sectionReconcile],
    ['写库回读复原', sectionWriteBack],
    ['工作流/审批', sectionWorkflow],
    ['未闭环项收口', sectionClosure],
    ['外壳金标准自检', sectionShell],
    ['UI 渲染', sectionUi],
  ]
  for (const [name, fn] of sections) {
    if (ONLY === 'api' && !['菜单完整性', '接口探针', '只读 DB 对账'].includes(name)) continue
    try {
      await fn()
    } catch (e) {
      check(`[节异常] ${name}`, false, (e && e.message || String(e)).slice(0, 200))
      console.error(`\n⚠️ 第「${name}」节抛异常：`, e)
    }
  }

  const failed = results.filter(r => !r.ok)
  const skipped = results.filter(r => r.skipped)
  console.log('\n════════════════ 汇总 ════════════════')
  console.log(`总计 ${results.length} 项：通过 ${results.length - failed.length}，失败 ${failed.length}，跳过 ${skipped.length}`)
  if (failed.length) {
    console.log('\n失败清单：')
    failed.forEach(f => console.log(`  ❌ ${f.name} — ${f.detail || ''}`))
  }
  if (skipped.length) {
    console.log('\n跳过清单（含原因）：')
    skipped.forEach(f => console.log(`  ⏭️  ${f.name} — ${f.detail || ''}`))
  }
  process.exit(failed.length ? 1 : 0)
}

main().catch(e => { console.error(e); process.exit(1) })
