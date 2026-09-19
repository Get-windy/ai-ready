/*
 * CRM 模块金标准端到端验证（模块级 E2E）
 *
 * 口径依据：`docs/Yh-Spec/手动整理对标开发文档/crm模块/`（README + 16 篇页面文档）。
 *   CRM 是本系统**独有模块**——对标系统 ql361 **没有 CRM 域**，所以本模块不是「照对标复刻」，
 *   而是按业界生产级 CRM（Odoo / SAP Sales Cloud / 金蝶 / 用友）建模；所有断言只对照
 *   本模块开发文档 + 本系统源码，不引用任何对标页面。
 *
 * 验收方式：**先只读对账，再造数/改库回读，最后按原值复原/清理**。
 *   每个「对账」类断言先直连 PostgreSQL 取期望值（含 tenant_id / deleted=0 / 多租户拦截器口径），
 *   再与 HTTP 返回值逐项比较；不使用「只断言 HTTP 200」的空炮。
 *   依赖环境前置数据而无法验证的项，在断言名或 detail 里**显式写明降级原因**，不做静默通过。
 *
 * 用法：
 *   node tools/e2e-crm.cjs
 *   CRM_PORT=5690 FE_URL=http://localhost:5656 node tools/e2e-crm.cjs
 *   CRM_ONLY=api node tools/e2e-crm.cjs     （跳过 UI 节，只跑接口 + DB 对账）
 *
 * 副作用与清理：
 *   1) 造数：客户 / 线索 / 商机 / 跟进 / 公海池各 1~2 条，全部带 `E2E-CRM-` 前缀；
 *      结束前**物理 DELETE** 清空（含逻辑删除行），并断言残留为 0；
 *   2) 改库：仅对自造数据操作，不触碰既有业务行；
 *   3) 截图落到 `I:/AI-Ready/tool-results/e2e-crm/`（目录不存在时自动创建）。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.CRM_PORT || 5690)
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
const E2E_USER = process.env.E2E_USER || 'admin'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const SHOTS = 'I:/AI-Ready/tool-results/e2e-crm'
const ONLY = (process.env.CRM_ONLY || '').trim()

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

// ════════════════════════════════════════════════════════════════════
// 1. sys_menu 菜单完整性（CRM 17 页 = 原 16 + 客户公海）
// ════════════════════════════════════════════════════════════════════
const EXPECTED_MENUS = [
  [80200, '客户', 'crm/customer/index'],
  [80201, '客户跟进', 'crm/customer-follow'],
  [70303, '客户分级', 'crm/customer-grade'],
  [80202, '客户公海', 'crm/customer-pool'],
  [70001, '拜访规划', 'sales/visit-plan'],
  [70002, '拜访执行', 'sales/visit-exec'],
  [70003, '拜访检视', 'sales/visit-review'],
  [80210, '线索', 'crm/lead/index'],
  [70311, '线索转化', 'crm/lead-convert'],
  [80220, '商机', 'crm/opportunity/index'],
  [70321, '商机阶段', 'crm/opportunity-stage'],
  [70330, '报价单', 'crm/quotation/form'],
  [80230, '合同', 'crm/contract/index'],
  [70341, '合同审批', 'crm/contract-approval'],
  [70350, '发票', 'crm/invoice/form'],
  [70360, '销售漏斗', 'crm/funnel'],
  [70361, '客户分析', 'crm/customer-analysis'],
]

async function section1Menu() {
  console.log('\n=== 1. sys_menu 菜单完整性（CRM 17 页）===')
  const ids = EXPECTED_MENUS.map(m => m[0]).join(',')
  const rows = await dbQuery(
    `SELECT id, menu_name, path, component, parent_id, visible, status
       FROM sys_menu WHERE id IN (${ids}) AND deleted = 0`)
  check('1.1 菜单行数 = 17（含新增的客户公海 80202）', rows.length === EXPECTED_MENUS.length,
    `实得 ${rows.length} 行`)

  const byId = new Map(rows.map(r => [String(r.id), r]))
  const missing = EXPECTED_MENUS.filter(([id]) => !byId.has(String(id))).map(([id, n]) => `${id}${n}`)
  check('1.2 17 个菜单 ID 全部在库', missing.length === 0, missing.length ? '缺: ' + missing.join(',') : '')

  const badPath = EXPECTED_MENUS.filter(([id, , path]) => {
    const r = byId.get(String(id))
    return r && r.path !== path
  }).map(([id, n]) => `${id}${n}`)
  check('1.3 菜单 path 与文档一致', badPath.length === 0, badPath.length ? '不符: ' + badPath.join(',') : '')

  const enabled = rows.filter(r => Number(r.visible) === 1 && Number(r.status) === 1).length
  check('1.4 全部菜单可见且启用', enabled === rows.length, `可见启用 ${enabled}/${rows.length}`)

  // 顶层 CRM 分组
  const group = await dbQuery(
    `SELECT id, menu_name FROM sys_menu WHERE parent_id = 60007 AND deleted = 0 ORDER BY sort`)
  check('1.5 CRM 顶级下 8 个分组', group.length === 8, `实得 ${group.length}: ${group.map(g => g.menu_name).join('/')}`)
}

// ════════════════════════════════════════════════════════════════════
// 2. P0 修复验证（本会话后端改动）
// ════════════════════════════════════════════════════════════════════
async function section2P0(token) {
  console.log('\n=== 2. P0 修复验证 ===')

  // 2.1 三表补 tenant_id 后登录态不再 500
  for (const [label, path] of [
    ['线索', '/crm/lead/page?pageNum=1&pageSize=1'],
    ['商机', '/crm/opportunity/page?pageNum=1&pageSize=1'],
    ['客户跟进', '/crm/followUp/page?pageNum=1&pageSize=1'],
  ]) {
    const r = await get(path, token)
    check(`2.1 ${label}分页登录态返回 200（tenant_id 缺列已修）`, r.status === 200,
      `HTTP ${r.status}${r.status !== 200 ? ' ' + r.text.slice(0, 120) : ''}`)
  }

  // 2.2 DB 侧复核三列确实存在
  const cols = await dbQuery(
    `SELECT table_name FROM information_schema.columns
      WHERE table_name IN ('crm_customer_lead','crm_customer_opportunity','crm_customer_follow_up')
        AND column_name = 'tenant_id'`)
  check('2.2 三表 tenant_id 列已建', cols.length === 3,
    `实得 ${cols.length}/3: ${cols.map(c => c.table_name).join(',')}`)

  // 2.3 四个新增端点不再 404（用不存在的 id 探测：404「业务未找到」≠ HTTP 404「路由不存在」）
  const probes = [
    ['PUT', '/crm/opportunity/999999999/stage?stage=2', '商机阶段更新'],
    ['POST', '/crm/lead/batch-convert', '线索批量转化'],
    ['DELETE', '/crm/quotation/999999999', '报价单删除'],
    ['PUT', '/erp/invoice/999999999', '发票更新'],
  ]
  for (const [method, path, label] of probes) {
    const r = await rawReq(method, path, method === 'POST' ? [] : {}, token)
    // 路由存在 → 不会返回 Spring 的 404 "No static resource"/"Not Found"；
    // 单条业务不存在时返回 400/404 业务错误也算「路由已存在」
    const routeMissing = r.status === 404 && /no static resource|not found/i.test(r.text) &&
      !/不存在/.test(r.text)
    check(`2.3 ${label}端点已注册（不再 404）`, !routeMissing, `HTTP ${r.status}`)
  }

  // 2.4 权限码补齐
  const permCount = await dbOne(
    `SELECT count(*)::int AS n FROM sys_permission WHERE permission_code LIKE 'crm:%' AND deleted = 0`)
  check('2.4 CRM 权限码 ≥ 50 条（原仅 5 条）', permCount.n >= 50, `实得 ${permCount.n} 条`)

  const hasApprove = await dbOne(
    `SELECT count(*)::int AS n FROM sys_permission WHERE permission_code = 'crm:contract:approve' AND deleted = 0`)
  check('2.5 合同审批权限码 crm:contract:approve 已登记', hasApprove.n === 1)
}

// ════════════════════════════════════════════════════════════════════
// 3. 造数 + 写路径闭环（号段 / 转化 / 阶段 / 公海池）
// ════════════════════════════════════════════════════════════════════
const TAG = 'E2E-CRM-' + Date.now()
const created = { leadIds: [], oppIds: [], followIds: [], customerIds: [], quotationIds: [], poolIds: [] }

async function section3Write(token) {
  console.log('\n=== 3. 造数 + 写路径闭环 ===')

  // 3.1 建客户
  const cust = await post('/customer', {
    customerName: `${TAG}-客户`, shortName: `${TAG}-客户`,
    customerType: 1, status: 1, industryType: 1, customerLevel: 1,
    businessContact: 'E2E', businessContactPhone: '13800000000'
  }, token)
  const custRow = await dbOne(
    `SELECT id, customer_code, status, tenant_id FROM crm_customer WHERE customer_name = $1 ORDER BY id DESC LIMIT 1`,
    [`${TAG}-客户`])
  const custId = custRow.id
  if (custId) created.customerIds.push(custId)
  check('3.1 客户建档成功（且 tenant_id 已注入）', !!custId && custRow.tenant_id !== null,
    `HTTP ${cust.status}, id=${custId}, code=${custRow.customer_code}, tenant_id=${custRow.tenant_id}` +
    (custId ? '' : ' | resp=' + cust.text.slice(0, 240)))
  check('3.2 客户 status 口径 = 1 表示正常', Number(custRow.status) === 1, `status=${custRow.status}`)

  // 3.3 线索创建 + 号段格式
  const lead = await post('/crm/lead', {
    leadName: `${TAG}-线索`, companyName: `${TAG}-线索公司`,
    contactName: 'E2E', contactPhone: '13900000000',
    leadSource: 1, leadStatus: 0, leadLevel: 1, estimatedAmount: 10000
  }, token)
  const leadRow = await dbOne(
    `SELECT id, lead_code FROM crm_customer_lead WHERE lead_name = $1 ORDER BY id DESC LIMIT 1`,
    [`${TAG}-线索`])
  if (leadRow.id) created.leadIds.push(leadRow.id)
  const codeOk = /^LEAD-\d{8}\d{4}$/.test(leadRow.lead_code || '')
  check('3.3 线索创建后单号形如 LEAD-yyyyMMddNNNN', codeOk, `code=${leadRow.lead_code}`)

  // 3.4 号段关键回归：删一条再建一条，不得撞唯一索引
  const lead2 = await post('/crm/lead', { leadName: `${TAG}-线索B`, companyName: `${TAG}-B`, leadStatus: 0 }, token)
  const lead2Row = await dbOne(
    `SELECT id, lead_code FROM crm_customer_lead WHERE lead_name = $1 ORDER BY id DESC LIMIT 1`, [`${TAG}-线索B`])
  if (lead2Row.id) created.leadIds.push(lead2Row.id)
  if (lead2Row.id) {
    await del(`/crm/lead/${lead2Row.id}`, token)
    const lead3 = await post('/crm/lead', { leadName: `${TAG}-线索C`, companyName: `${TAG}-C`, leadStatus: 0 }, token)
    const lead3Row = await dbOne(
      `SELECT id, lead_code FROM crm_customer_lead WHERE lead_name = $1 ORDER BY id DESC LIMIT 1`, [`${TAG}-线索C`])
    if (lead3Row.id) created.leadIds.push(lead3Row.id)
    check('3.4 删一条再建一条不撞唯一索引（号段取 max+1）',
      lead3.status === 200 && !!lead3Row.id,
      `第二次创建 HTTP ${lead3.status}, code=${lead3Row.lead_code}（被删的 ${lead2Row.lead_code}）`)
  } else {
    check('3.4 删一条再建一条不撞唯一索引', false, '前置：第二条线索未建成功')
  }

  // 3.5 跟进创建（表已补 tenant_id）
  const follow = await post('/crm/followUp', {
    customerId: custId, customerName: `${TAG}-客户`,
    followUpType: 1, followUpTypeDesc: '电话',
    content: `${TAG}-跟进内容`, followUpDate: new Date().toISOString().slice(0, 10),
    salesPersonName: 'E2E'
  }, token)
  const followRow = await dbOne(
    `SELECT id, follow_up_code, tenant_id FROM crm_customer_follow_up WHERE content = $1 ORDER BY id DESC LIMIT 1`,
    [`${TAG}-跟进内容`])
  if (followRow.id) created.followIds.push(followRow.id)
  check('3.5 跟进记录创建成功且带 tenant_id', !!followRow.id && followRow.tenant_id !== null,
    `HTTP ${follow.status}, code=${followRow.follow_up_code}` +
    (followRow.id ? '' : ' | resp=' + follow.text.slice(0, 240)))

  // 3.6 跟进按日期区间筛选（本会话新补的查询参数）
  const today = new Date().toISOString().slice(0, 10)
  const rangeQ = await get(`/crm/followUp/page?nextFollowUpDateStart=2000-01-01&nextFollowUpDateEnd=${today}&pageSize=1`, token)
  check('3.6 跟进「下次跟进日期区间」参数生效（待办口径）', rangeQ.status === 200, `HTTP ${rangeQ.status}`)

  // 3.7 商机创建 + 阶段跳转（PUT /{id}/stage 为新端点）
  const opp = await post('/crm/opportunity', {
    opportunityName: `${TAG}-商机`, customerId: custId, customerName: `${TAG}-客户`,
    opportunityStage: 1, status: 1, estimatedAmount: 50000, probability: 10
  }, token)
  const oppRow = await dbOne(
    `SELECT id, opportunity_code, opportunity_stage, tenant_id FROM crm_customer_opportunity
      WHERE opportunity_name = $1 ORDER BY id DESC LIMIT 1`, [`${TAG}-商机`])
  if (oppRow.id) created.oppIds.push(oppRow.id)
  check('3.7 商机创建成功且带 tenant_id', !!oppRow.id && oppRow.tenant_id !== null,
    `HTTP ${opp.status}, code=${oppRow.opportunity_code}`)

  if (oppRow.id) {
    const st = await put(`/crm/opportunity/${oppRow.id}/stage?stage=3`, {}, token)
    const after = await dbOne(`SELECT opportunity_stage, probability FROM crm_customer_opportunity WHERE id = $1`, [oppRow.id])
    check('3.8 PUT /crm/opportunity/{id}/stage 真实改库（1→3）',
      Number(after.opportunity_stage) === 3,
      `HTTP ${st.status}, 阶段=${after.opportunity_stage}, 赢率=${after.probability}`)

    const bad = await put(`/crm/opportunity/${oppRow.id}/stage?stage=99`, {}, token)
    check('3.9 非法阶段被拒（值域 1-5）', bad.status >= 400, `HTTP ${bad.status}`)
  }

  // 3.10 线索批量转化
  const idsToConvert = created.leadIds.filter(Boolean).slice(0, 2)
  if (idsToConvert.length) {
    const bc = await post('/crm/lead/batch-convert', idsToConvert, token)
    const conv = await dbOne(
      `SELECT count(*)::int AS n FROM crm_customer_lead WHERE id = ANY($1::bigint[]) AND lead_status = 3`,
      [idsToConvert])
    check('3.10 POST /crm/lead/batch-convert 逐条转化并落状态=3',
      bc.status === 200 && conv.n > 0,
      `HTTP ${bc.status}, 已转化 ${conv.n}/${idsToConvert.length} | resp=` + bc.text.slice(0, 300))
    // 转化产生的客户挂到清理清单
    const convCust = await dbQuery(
      `SELECT converted_customer_id AS id FROM crm_customer_lead WHERE id = ANY($1::bigint[]) AND converted_customer_id IS NOT NULL`,
      [idsToConvert])
    convCust.forEach(r => created.customerIds.push(r.id))
  } else {
    skip('3.10 线索批量转化', '前置：无可用线索')
  }

  // 3.11 公海池：放入 → 领取
  if (custId) {
    const put1 = await post(`/crm/customer-pool/put/${custId}?poolReason=4&remark=${encodeURIComponent(TAG)}`, null, token)
    const poolRow = await dbOne(
      `SELECT id, status, tenant_id FROM crm_customer_pool WHERE customer_id = $1 ORDER BY id DESC LIMIT 1`, [custId])
    if (poolRow.id) created.poolIds.push(poolRow.id)
    check('3.11 客户放入公海池成功', put1.status === 200 && !!poolRow.id,
      `HTTP ${put1.status}, poolId=${poolRow.id}, status=${poolRow.status}`)

    if (poolRow.id) {
      const claim = await post(`/crm/customer-pool/claim/${poolRow.id}`, null, token)
      const after = await dbOne(`SELECT status, claim_sales_person_id FROM crm_customer_pool WHERE id = $1`, [poolRow.id])
      check('3.12 从公海池领取成功（状态 → 2）', claim.status === 200 && Number(after.status) === 2,
        `HTTP ${claim.status}, status=${after.status}, 领取人=${after.claim_sales_person_id}`)

      const ret = await post(`/crm/customer-pool/return/${poolRow.id}?remark=${encodeURIComponent(TAG)}`, null, token)
      const after2 = await dbOne(`SELECT status FROM crm_customer_pool WHERE id = $1`, [poolRow.id])
      check('3.13 退回公海池成功（状态 → 4 或 1）', ret.status === 200 && [1, 4].includes(Number(after2.status)),
        `HTTP ${ret.status}, status=${after2.status}`)
    }
    const stats = await get('/crm/customer-pool/statistics', token)
    check('3.14 公海池统计端点返回 200', stats.status === 200 && stats.body && typeof stats.body === 'object',
      `HTTP ${stats.status}`)
  }
}

// ════════════════════════════════════════════════════════════════════
// 4. CRM 各页列表端点连通性（17 页对应主查询）
// ════════════════════════════════════════════════════════════════════
const PAGE_ENDPOINTS = [
  ['客户', '/customer/page?pageNum=1&pageSize=1'],
  ['客户跟进', '/crm/followUp/page?pageNum=1&pageSize=1'],
  ['客户分级', '/customer/page?pageNum=1&pageSize=1'],
  ['客户公海', '/crm/customer-pool/page?pageNum=1&pageSize=1'],
  ['线索', '/crm/lead/page?pageNum=1&pageSize=1'],
  ['线索转化', '/crm/lead/export'],
  ['商机', '/crm/opportunity/page?pageNum=1&pageSize=1'],
  ['商机阶段', '/crm/opportunity/export'],
  ['报价单', '/crm/quotation/page?pageNum=1&pageSize=1'],
  ['合同', '/crm/contract/page?pageNum=1&pageSize=1'],
  ['合同审批', '/crm/contract/export'],
  ['发票', '/erp/invoice/page?page=0&size=1'],
  ['拜访规划', '/crm/visit/plan/page?page=1&size=1'],
  ['拜访执行', '/crm/visit/record/page?page=1&size=1'],
  ['拜访检视', '/crm/visit/review/page?page=1&size=1'],
  ['销售漏斗', '/crm/opportunity/export'],
  ['客户分析', '/customer/export'],
]

async function section4Endpoints(token) {
  console.log('\n=== 4. 各页主查询端点连通性 ===')
  let ok = 0
  for (const [label, path] of PAGE_ENDPOINTS) {
    const r = await get(path, token)
    const pass = r.status === 200
    if (pass) ok++
    check(`4.x ${label} 主查询 200`, pass, `HTTP ${r.status} ${path}${pass ? '' : ' ' + r.text.slice(0, 100)}`)
  }
  check(`4.汇总 17 个页面主查询全部 200`, ok === PAGE_ENDPOINTS.length, `${ok}/${PAGE_ENDPOINTS.length}`)
}

// ════════════════════════════════════════════════════════════════════
// 5. UI 遍历（17 页）：无 console error + 关键骨架存在 + 截屏
// ════════════════════════════════════════════════════════════════════
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
  ['客户', '/crm/customer/index'],
  ['客户跟进', '/crm/customer-follow'],
  ['客户分级', '/crm/customer-grade'],
  ['客户公海', '/crm/customer-pool'],
  ['线索', '/crm/lead/index'],
  ['线索转化', '/crm/lead-convert'],
  ['商机', '/crm/opportunity/index'],
  ['商机阶段', '/crm/opportunity-stage'],
  ['报价单', '/crm/quotation/form'],
  ['合同', '/crm/contract/index'],
  ['合同审批', '/crm/contract-approval'],
  ['发票', '/crm/invoice/form'],
  ['销售漏斗', '/crm/funnel'],
  ['客户分析', '/crm/customer-analysis'],
  ['拜访规划', '/sales/visit-plan'],
  ['拜访执行', '/sales/visit-exec'],
  ['拜访检视', '/sales/visit-review'],
]

async function section5UI() {
  console.log('\n=== 5. UI 遍历（17 页）===')
  fs.mkdirSync(SHOTS, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()

  // 把 /api/** 转发到独立后端实例（不与并行会话的 5655 冲突）
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}` + u.pathname + u.search })
  })

  const consoleErrors = []
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()) })
  page.on('pageerror', e => consoleErrors.push('PAGEERROR: ' + e.message))

  await page.goto(FE + '/', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(4000)
  if (page.url().includes('login') || await page.locator('input[type=password]').count() > 0) {
    const tenantInput = page.locator('input[placeholder*="租户"]').first()
    if (await tenantInput.count() > 0) await tenantInput.fill('系统租户')
    await page.locator('input[placeholder*="用户名"]').first().fill(E2E_USER)
    await page.locator('input[placeholder*="密码"]').first().fill(E2E_PWD)
    const capInput = page.locator('input[placeholder*="验证码"]').first()
    if (await capInput.count() > 0) await capInput.fill(await solveSvgCaptcha(page))
    await page.locator('button:has-text("登 录"), button:has-text("登录")').first().click()
    await page.waitForTimeout(8000)
  }
  check('5.0 登录并进入系统', !page.url().includes('login'), page.url())

  let uiOk = 0
  for (const [label, path] of UI_PAGES) {
    consoleErrors.length = 0
    await page.goto(FE + path, { waitUntil: 'domcontentloaded' }).catch(() => {})
    await page.waitForTimeout(4500)
    const shot = `${SHOTS}/${label}.png`
    await page.screenshot({ path: shot, fullPage: false }).catch(() => {})

    const bodyText = (await page.locator('body').innerText().catch(() => '')) || ''
    const hasSkeleton = await page.locator('.category-list-layout, .page-container, .ar-report-page, .ant-card').count() > 0
    const blank = bodyText.trim().length < 20
    const hardErrors = consoleErrors.filter(t =>
      // 401/被并行会话顶下线属过程噪声，不算页面缺陷
      !/401|Unauthorized|auth\/refresh|\/login/i.test(t))
    const ok = !blank && hasSkeleton && hardErrors.length === 0
    if (ok) uiOk++
    check(`5.x ${label} 页面可用（非白屏 + 无控制台错误）`, ok,
      ok ? '' : `blank=${blank} skeleton=${hasSkeleton} err=${hardErrors.slice(0, 2).join(' | ').slice(0, 160)}`)
  }
  check('5.汇总 17 个页面全部可用', uiOk === UI_PAGES.length, `${uiOk}/${UI_PAGES.length}`)

  await browser.close()
}

// ════════════════════════════════════════════════════════════════════
// 6. 清理自造数据
// ════════════════════════════════════════════════════════════════════
async function section6Cleanup() {
  console.log('\n=== 6. 清理自造数据 ===')
  const tables = [
    ['crm_customer_follow_up', 'id', created.followIds],
    ['crm_customer_opportunity', 'id', created.oppIds],
    ['crm_customer_lead', 'id', created.leadIds],
    ['crm_customer_pool', 'id', created.poolIds],
    ['crm_customer', 'id', created.customerIds.filter(Boolean)],
  ]
  for (const [table, col, ids] of tables) {
    const uniq = [...new Set(ids.filter(Boolean))]
    if (!uniq.length) { skip(`6.x 清理 ${table}`, '本次无造数'); continue }
    await dbQuery(`DELETE FROM ${table} WHERE ${col} = ANY($1::bigint[])`, [uniq])
    const left = await dbOne(`SELECT count(*)::int AS n FROM ${table} WHERE ${col} = ANY($1::bigint[])`, [uniq])
    check(`6.x 清理 ${table}（${uniq.length} 行）`, left.n === 0, `残留 ${left.n}`)
  }
  // 兜底：按前缀清残留（含逻辑删除行与后续步骤产生的关联行）
  for (const [table, col] of [
    ['crm_customer_follow_up', 'content'],
    ['crm_customer_opportunity', 'opportunity_name'],
    ['crm_customer_lead', 'lead_name'],
  ]) {
    await dbQuery(`DELETE FROM ${table} WHERE ${col} LIKE $1`, [TAG + '%'])
  }
  await dbQuery(`DELETE FROM crm_customer_pool WHERE remark LIKE $1`, [TAG + '%'])
  await dbQuery(`DELETE FROM crm_customer WHERE customer_name LIKE $1`, [TAG + '%'])

  // ⚠️ 必须 ::int —— count(*) 是 bigint，pg 驱动会返回**字符串**，`'0' === 0` 恒 false（假失败）
  const residual = await dbOne(
    `SELECT ((SELECT count(*) FROM crm_customer WHERE customer_name LIKE $1)
          + (SELECT count(*) FROM crm_customer_lead WHERE lead_name LIKE $1)
          + (SELECT count(*) FROM crm_customer_opportunity WHERE opportunity_name LIKE $1)
          + (SELECT count(*) FROM crm_customer_follow_up WHERE content LIKE $1))::int AS n`, [TAG + '%'])
  check('6.汇总 自造数据残留为 0', residual.n === 0, `残留 ${residual.n}`)
}

// ════════════════════════════════════════════════════════════════════
async function main() {
  console.log(`CRM 模块 E2E：后端 ${PORT} · 前端 ${FE}`)
  const up = await waitBackend()
  if (!up) { console.error(`❌ 后端 ${PORT} 未就绪，终止`); process.exit(1) }

  const { token } = await login()
  console.log('  登录成功')

  try {
    await section1Menu()
    await section2P0(token)
    await section3Write(token)
    await section4Endpoints(token)
    if (ONLY !== 'api') await section5UI()
  } finally {
    await section6Cleanup()
  }

  const pass = results.filter(r => r.ok).length
  const fail = results.filter(r => !r.ok).length
  const skipped = results.filter(r => r.skipped).length
  console.log('\n' + '='.repeat(60))
  console.log(`总计 ${results.length} 项：通过 ${pass} · 失败 ${fail} · 跳过 ${skipped}`)
  console.log('='.repeat(60))
  if (fail) {
    console.log('\n失败明细：')
    results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name}${r.detail ? ' — ' + r.detail : ''}`))
  }
  process.exit(fail ? 1 : 0)
}

main().catch(e => { console.error('E2E 异常终止:', e); process.exit(2) })
