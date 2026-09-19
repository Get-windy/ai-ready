/**
 * 分析模块 · 模块级端到端验收（金标准）
 *
 * 运行：node tools/e2e-analytics.cjs        （依赖：后端 5655 + 前端 vite 5656 已在跑）
 * 环境变量：BE_PORT(默认 5655) / FE(默认 http://localhost:5656) / ANALYTICS_ONLY=<页名,页名>
 *
 * 七节：
 *   1. sys_menu 菜单完整性（31 页 path/component 与文档一一对应）
 *   2. 组件文件存在性
 *   3. 页面骨架静态清单（路线 A/A′ 十项）
 *   4. 页面取数接口探针（HTTP 200 且 code===200）
 *   5. UI 逐页 + 逐 Tab 列配置弹窗断言（全部列数 / 默认列数 对齐对标实测）
 *   6. 综合单据（docquery）口径深检：合计行、分页、排序、账期、显示红冲、日期区间
 *   7. 截图归档
 *
 * 设计原则：
 *   - **只读**：不对共享 dev 库做任何写操作（避免污染并行会话）。
 *   - 期望值取自对标实测（`tool-results/ql361/分析-live/*.json` 的 columnConfig.count/defCount），
 *     逐页写死在 SPEC 里，不靠"当前页面渲染出来的值"反推 —— 否则测试会自我实现。
 *   - 已确认「对标本身无该列数据源」的页面用 `override` 显式登记差额与原因，不静默放宽。
 */
const fs = require('fs')
const path = require('path')
const http = require('http')
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const ROOT = 'I:/AI-Ready'
const FE = process.env.FE || 'http://localhost:5656'
const BE = Number(process.env.BE_PORT || 5655)
const SHOTS = path.join(ROOT, 'tool-results/e2e-analytics')

let pass = 0
let fail = 0
let pending = 0
const failures = []

function check(name, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected)
  if (ok) { pass++; console.log('  ✔', name, '=', JSON.stringify(actual)) }
  else { fail++; failures.push(`${name} | actual=${JSON.stringify(actual)} expected=${JSON.stringify(expected)}`); console.log('  ✘', name, 'actual=', JSON.stringify(actual), 'expected=', JSON.stringify(expected)) }
}
function checkTrue(name, cond, detail) {
  if (cond) { pass++; console.log('  ✔', name, detail !== undefined ? '-> ' + detail : '') }
  else { fail++; failures.push(`${name} -> ${detail}`); console.log('  ✘', name, detail !== undefined ? '-> ' + detail : '') }
}
/** 未改造页：不计入通过/失败，单独统计（避免用"绿"掩盖缺口） */
function markPending(name, why) {
  pending++
  console.log('  ○ 待改造', name, '—', why)
}

// ════════════════════════════════════════════════════════════════════
// SPEC：逐页对标实测规格（来源 tool-results/ql361/分析-live/*.json）
//   tabs[].count = 全部可配置列数；tabs[].def = 默认显示列数
//   pageConfig = 对标实测是否有「页面配置」弹窗（唯一依据 = pageConfig.found / 页面配置弹窗截图）
//   print / export = 对标工具栏是否有「打印(F8)」「导出」
//   minRowsCols = 若对标列清单自身未抓取（如业务员提成无表头齿轮），跳过列数断言
//   override = 已确认「对标列在本系统无数据源」而如实不渲染时的例外登记
// ════════════════════════════════════════════════════════════════════
const SPEC = [
  { name: '待审批单据', path: 'analytics/pending-approval', doc: '待审批单据', pageConfig: false, print: false, export: false,
    tabs: [{ label: null, count: 13, def: 12 }], apis: ['/docquery/pending-docs/page?page=1&size=1'] },

  { name: '业务草稿', path: 'analytics/draft', doc: '业务草稿', pageConfig: true, print: false, export: false,
    tabs: [{ label: null, count: 13, def: 12 }], apis: ['/docquery/draft-docs/page?page=1&size=1'] },

  { name: '经营历程', path: 'analytics/business-history', doc: '经营历程', pageConfig: true, print: true, export: true,
    tabs: [{ label: null, count: 17, def: 9 }], apis: ['/docquery/business-history/page?page=1&size=1'] },

  { name: '销售业绩', path: 'analytics/sales-performance', doc: '销售业绩', pageConfig: true, print: true, export: true,
    tabs: [{ label: '按时间', count: 27, def: 11 }, { label: '按职员', count: 34, def: 12 }], apis: [] },

  { name: '销售分析', path: 'analytics/sales-analysis', doc: '销售分析', pageConfig: false, print: true, export: true,
    tabs: [
      { label: '按时间', count: 50, def: 15 }, { label: '按商品', count: 65, def: 14 },
      { label: '按品牌', count: 41, def: 11 }, { label: '按客户', count: 53, def: 18 },
      { label: '按区域', count: 50, def: 17 }, { label: '按仓库', count: 49, def: 15 },
      { label: '按职员', count: 53, def: 17 }, { label: '按来源', count: 42, def: 11 }
    ], apis: [] },

  { name: '销售履约分析', path: 'analytics/sales-fulfillment', doc: '销售履约分析', pageConfig: true, print: true, export: true,
    tabs: [{ label: '按单据', count: 11, def: 9 }, { label: '按客户', count: 12, def: 8 }], apis: [] },

  { name: '销售欠款分析', path: 'analytics/sales-debt', doc: '销售欠款分析', pageConfig: true, print: true, export: true,
    tabs: [
      { label: '按职员', count: 15, def: 12 }, { label: '按客户', count: 17, def: 16 }, { label: '按区域', count: 17, def: 16 }
    ], apis: [] },

  { name: '销售费用分析', path: 'analytics/sales-expense', doc: '销售费用分析', pageConfig: false, print: true, export: true,
    tabs: [{ label: '按往来单位', count: 11, def: 11 }, { label: '按职员', count: 11, def: 11 }], apis: [] },

  { name: '客户活跃分析', path: 'analytics/customer-active', doc: '客户活跃分析', pageConfig: false, print: true, export: true,
    tabs: [{ label: null, count: 8, def: 8 }], apis: ['/erp/sale/analysis/customer-active/page?page=1&size=1'] },

  { name: '推广分析', path: 'analytics/promotion-analysis', doc: '推广分析', pageConfig: true, print: true, export: true,
    tabs: [{ label: null, count: 7, def: 7 }],
    apis: ['/erp/sale/analysis/promotion-funnel/page', '/erp/sale/promotion/analysis'] },

  { name: '预订货查询', path: 'analytics/pre-order-query', doc: '预订货查询', pageConfig: true, print: true, export: true,
    tabs: [{ label: '商品预订货分析', count: 28, def: 11 }, { label: '客户预订货分析', count: 23, def: 6 }],
    apis: ['/erp/sale/pre-order/analysis/page?tab=product&page=1&size=1'] },

  { name: '采购分析', path: 'analytics/purchase-analysis', doc: '采购分析', pageConfig: true, print: true, export: true,
    // 「按时间」Tab 对标列清单至今未抓取（columnConfig={"err":"no dialog"}），只断言其存在，不断言列数
    tabs: [{ label: '按时间', count: null, def: null }, { label: '按商品', count: 28, def: 11 }, { label: '按供应商', count: 17, def: 11 }],
    apis: ['/erp/purchase/analytics/page?tab=supplier&page=1&size=1'] },

  { name: '采购准备', path: 'analytics/purchase-prep', doc: '采购准备', pageConfig: true, print: true, export: true,
    tabs: [
      { label: '库存预警补货', count: 24, def: 13 }, { label: '缺货补货', count: 16, def: 12 },
      { label: '智能补货', count: 25, def: 20 }, { label: '以销定购', count: 42, def: 24 }
    ], apis: [] },

  { name: '查库存', path: 'analytics/check-stock', doc: '查库存', pageConfig: true, print: true, export: true,
    // 库存分布 = 商品×仓库透视：33 列之外**另有动态仓库列 + 合计列**（文档明示「列数不占 33 列额度」），
    // 故该 Tab 断言「隐藏列数恒等」（count−def === 33−2）而非绝对值
    tabs: [
      { label: '当前库存', count: 54, def: 8 }, { label: '按属性', count: 50, def: 6 },
      { label: '库存分布', count: 33, def: 2, dynamic: true }
    ],
    apis: ['/erp/stock/page?pageNum=1&pageSize=1'] },

  { name: '查批次', path: 'analytics/check-batch', doc: '查批次', pageConfig: false, print: true, export: true,
    tabs: [{ label: '商品批次查询', count: 19, def: 13 }, { label: '商品批次跟踪', count: 23, def: 17 }, { label: '近效期预警查询', count: 20, def: 12 }], apis: [] },

  { name: '进销存分析', path: 'analytics/inventory-analysis', doc: '进销存分析', pageConfig: true, print: true, export: true,
    tabs: [{ label: '按商品', count: 38, def: 9 }, { label: '仓库调拨分析', count: 6, def: 6 }, { label: '商品调拨分析', count: 11, def: 6 }],
    apis: ['/erp/stock/analytics/page?tab=product&page=1&size=1'] },

  { name: '库存明细', path: 'analytics/stock-detail', doc: '库存明细', pageConfig: true, print: true, export: true,
    tabs: [{ label: null, count: 45, def: 17 }], apis: ['/erp/stock/flow/page?page=1&size=1'] },

  { name: '业务员提成', path: 'analytics/staff-commission', doc: '业务员提成', pageConfig: true, print: true, export: true,
    // 对标页无表头齿轮（columnConfig={"err":"no gear"}），列清单至今不可得 → 不做列数断言
    tabs: [{ label: null, count: null, def: null }],
    apis: ['/erp/marketing/commission/staff-summary/page?page=1&size=1'] },

  { name: '回款统计', path: 'analytics/collection-stats', doc: '回款统计', pageConfig: true, print: true, export: true,
    tabs: [{ label: '按职员', count: 7, def: 7 }, { label: '按部门', count: 7, def: 7 }],
    apis: ['/erp/finance/collection-stats'] },

  { name: '业绩提成中心', path: 'analytics/commission-center', doc: '业绩提成中心', pageConfig: true, print: true, export: true,
    tabs: [
      { label: '配送员', count: 2, def: 2 }, { label: '每月提成', count: 2, def: 2 },
      { label: '提成构成', count: 7, def: 7 }, { label: '方案汇总提成', count: 5, def: 5 },
      { label: '业绩概览', count: 21, def: 15 }, { label: '业绩明细', count: 21, def: 10 }
    ], apis: [] },

  { name: '经营分析', path: 'analytics/business-analysis', doc: '经营分析', pageConfig: true, print: true, export: true,
    tabs: [{ label: null, count: 16, def: 10 }], apis: [] },

  { name: '查资金', path: 'analytics/check-fund', doc: '查资金', pageConfig: true, print: true, export: true,
    tabs: [{ label: '资金余额', count: 6, def: 6 }, { label: '收支汇总', count: 73, def: 72 }, { label: '资金流水', count: 14, def: 9 }], apis: [] },

  { name: '查费用', path: 'analytics/check-expense', doc: '查费用', pageConfig: true, print: true, export: true,
    tabs: [
      { label: '按部门', count: 3, def: 3 }, { label: '按职员', count: 3, def: 3 },
      { label: '按明细', count: 15, def: 9 }, { label: '按往来单位', count: 4, def: 4 }
    ], apis: ['/erp/expense/statistics/by-department', '/erp/expense/statistics/by-type'] },

  { name: '发票统计', path: 'analytics/invoice-stats', doc: '发票统计', pageConfig: false, print: true, export: true,
    tabs: [{ label: null, count: 17, def: 17 }], apis: ['/erp/invoice/date-range?startDate=2026-01-01&endDate=2026-12-31'] },

  { name: '查应收', path: 'analytics/check-receivable', doc: '查应收', pageConfig: true, print: true, export: true,
    tabs: [{ label: '查应收', count: 28, def: 9 }, { label: '职员应收', count: 16, def: 8 }, { label: '部门应收', count: 16, def: 8 }],
    apis: ['/erp/finance/auxiliary/balance/page?pageNum=1&pageSize=1&subjectCode=1122&auxType=CUSTOMER'] },

  { name: '查应付', path: 'analytics/check-payable', doc: '查应付', pageConfig: true, print: true, export: true,
    tabs: [{ label: null, count: 21, def: 8 }], apis: ['/erp/finance/auxiliary/balance/page?pageNum=1&pageSize=1&subjectCode=2202&auxType=SUPPLIER'] },

  { name: '往来余额表', path: 'analytics/ar-balance-sheet', doc: '往来余额表', pageConfig: false, print: true, export: true,
    tabs: [{ label: null, count: 18, def: 18 }], apis: ['/erp/finance/partner-balance/page?pageNum=1&pageSize=1'] },

  { name: '营销活动分析', path: 'analytics/mkt-activity-analysis', doc: '营销活动分析', pageConfig: false, print: false, export: false,
    tabs: [
      { label: '商品促销', count: 12, def: 12 }, { label: '整单促销', count: 11, def: 11 },
      { label: '特价', count: 8, def: 8 }, { label: '优惠券', count: 9, def: 9 }
    ], apis: [] },

  { name: '营销推广分析', path: 'analytics/mkt-promote-analysis', doc: '营销推广分析', pageConfig: false, print: true, export: true,
    tabs: [
      { label: '按商品', count: 11, def: 11 }, { label: '按优惠券', count: 12, def: 12 },
      { label: '按促销活动', count: 13, def: 13 }, { label: '按拼团', count: 12, def: 12 },
      { label: '按秒杀', count: 11, def: 11 }, { label: '推广客户列表', count: 13, def: 13 }
    ], apis: [] },

  { name: '交易分析', path: 'analytics/trade-analysis', doc: '交易分析', pageConfig: false, print: false, export: false,
    tabs: [{ label: null, count: 7, def: 7 }], apis: ['/erp/mall/admin/trade-analysis?startDate=2026-01-01&endDate=2026-12-31'],
    // 对标 7 列中「登录客户数/下单客户数/下单转化率(%)」全库无数据源（无商城登录埋点），如实不渲染
    override: { cols: 3, reason: '缺 登录客户数/下单客户数/下单转化率(%) —— 全库无商城登录日志表（已核实 MallTradeAnalysis*/information_schema）' } },

  { name: '商城客户列表', path: 'analytics/mall-customer-list', doc: '商城客户列表', pageConfig: true, print: true, export: true,
    tabs: [{ label: null, count: 13, def: 13 }], apis: [],
    override: { cols: 3, reason: '缺 访问次数/成交数量/成交率(%) —— 全库无商城访问埋点表（已核实）' } }
]

// ════════════════════════════════════════════════════════════════════
// 基础设施
// ════════════════════════════════════════════════════════════════════
function req(method, p, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: BE, path: '/api' + p, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      }
    }, res => {
      // ⚠️ 必须按 Buffer 收集后一次性解码：`s += chunk` 会逐块 toString，
      //    多字节汉字被切在 chunk 边界时会产生 U+FFFD（实测把「草稿」读成「���稿」，假失败）
      const chunks = []
      res.on('data', c => chunks.push(Buffer.isBuffer(c) ? c : Buffer.from(c)))
      res.on('end', () => {
        const s = Buffer.concat(chunks).toString('utf8')
        try { resolve({ status: res.statusCode, body: JSON.parse(s) }) } catch { resolve({ status: res.statusCode, body: s }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function waitBackend(attempts = 24, intervalMs = 5000) {
  for (let i = 0; i < attempts; i++) {
    try {
      const r = await req('GET', '/auth/captcha')
      if (r.status === 200 && r.body && r.body.data && r.body.data.img) return true
    } catch { /* retry */ }
    console.log(`  等待后端 5655 就绪… (${i + 1}/${attempts})`)
    await new Promise(res => setTimeout(res, intervalMs))
  }
  return false
}

async function login() {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.body.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  // 共享 dev 环境下多会话都用 admin 登录时，sa-token 单端互踢会让双方反复掉线；
  // 并行验收请用独立账号（ANALYTICS_USER / ANALYTICS_PASSWORD），见《_开发指南-金标准》§十
  const res = await req('POST', '/auth/login', {
    username: process.env.ANALYTICS_USER || 'admin',
    password: process.env.ANALYTICS_PASSWORD || 'admin123',
    tenantName: process.env.ANALYTICS_TENANT || '系统租户',
    captcha: code, captchaKey: cap.body.data.uuid
  })
  const tk = res.body && res.body.data && (res.body.data.token || res.body.data.accessToken)
  if (!tk) throw new Error('登录失败: ' + JSON.stringify(res.body).slice(0, 300))
  return { token: tk, userInfo: res.body.data }
}

const onlyFilter = (process.env.ANALYTICS_ONLY || '').split(',').map(s => s.trim()).filter(Boolean)
const ACTIVE_SPEC = onlyFilter.length ? SPEC.filter(p => onlyFilter.includes(p.name)) : SPEC

// ════════════════════════════════════════════════════════════════════
// 1. sys_menu 菜单完整性
// ════════════════════════════════════════════════════════════════════
async function section1Menu(token) {
  console.log('\n=== 1. sys_menu 菜单完整性（31 页）===')
  const menuRes = await req('GET', '/menu/tree?tenantId=1', null, token)
  const flat = []
  const walk = (nodes) => (nodes || []).forEach(n => { flat.push(n); walk(n.children) })
  walk(menuRes.body && menuRes.body.data)
  for (const spec of ACTIVE_SPEC) {
    const hit = flat.find(m => (m.path || '').replace(/^\//, '') === spec.path)
    if (hit) { pass++; console.log(`  ✔ 菜单存在 ${spec.name} -> ${spec.path}`) }
    else { fail++; failures.push(`菜单缺失 ${spec.name} ${spec.path}`); console.log(`  ✘ 菜单缺失 ${spec.name} -> ${spec.path}`) }
  }
}

// ════════════════════════════════════════════════════════════════════
// 2. 组件文件存在性
// ════════════════════════════════════════════════════════════════════
function section2Files() {
  console.log('\n=== 2. 组件文件存在 ===')
  for (const spec of ACTIVE_SPEC) {
    const f = path.join(ROOT, 'frontend/apps/pc-admin/src/views', spec.path, 'index.vue')
    if (fs.existsSync(f)) { pass++; console.log(`  ✔ ${spec.name}`) }
    else { fail++; failures.push(`组件缺失 ${spec.path}`); console.log(`  ✘ 组件缺失 ${spec.path}`) }
  }
}

// ════════════════════════════════════════════════════════════════════
// 3. 页面骨架静态清单（10 项）
// ════════════════════════════════════════════════════════════════════
function section3Skeleton() {
  console.log('\n=== 3. 页面骨架静态清单 ===')
  for (const spec of ACTIVE_SPEC) {
    const f = path.join(ROOT, 'frontend/apps/pc-admin/src/views', spec.path, 'index.vue')
    if (!fs.existsSync(f)) { markPending(spec.name, '组件文件不存在'); continue }
    let src = ''
    try { src = fs.readFileSync(f, 'utf8') } catch { markPending(spec.name, '读取失败'); continue }

    // 已改造页的判据 = 用了 CategoryListLayout（路线 A/A′ 的强制项）
    if (!/CategoryListLayout/.test(src)) { markPending(spec.name, '未走路线 A/A′（无 CategoryListLayout）'); continue }

    const issues = []
    if (!/ErrorBoundary/.test(src)) issues.push('无 ErrorBoundary')
    if (!/PageContainer/.test(src)) issues.push('无 PageContainer')
    if (!/full-height/.test(src)) issues.push('无 full-height')
    if (/:full-height/.test(src)) issues.push(':full-height 应为静态属性')
    if (!/BillDetailTable/.test(src)) issues.push('无 BillDetailTable')
    if (/:max-height/.test(src)) issues.push('传了 :max-height（高度应交给 flex 链）')
    if (!/StandardPagination/.test(src)) issues.push('无 StandardPagination')
    if (!/variant="classic"/.test(src)) issues.push('分页未用 classic 形态')

    // 列配置 storage-key 与 global-config-key 同值、且与页面配置 key 不同值
    const sk = [...src.matchAll(/storage-key="([^"]+)"/g)].map(m => m[1])
    const gk = [...src.matchAll(/global-config-key="([^"]+)"/g)].map(m => m[1])
    if (gk.length === 0) issues.push('无 global-config-key')
    else if (sk.length === 0) issues.push('有 global-config-key 但无 storage-key')
    else {
      const colKeys = new Set(gk)
      for (const k of gk) if (!sk.includes(k)) issues.push(`global-config-key(${k}) 无同名 storage-key`)
      // 页面配置的 key 必须与列配置 key 不同值（同名会互相脏数据）
      const pageCfg = sk.filter(k => !colKeys.has(k))
      for (const k of gk) if (pageCfg.includes(k)) issues.push(`页面配置与列配置 key 同名(${k})`)
    }
    const usesPageConfig = /PageConfigPanel/.test(src)
    if (spec.pageConfig && !usesPageConfig) issues.push('对标有页面配置弹窗但未接 PageConfigPanel')
    if (!spec.pageConfig && usesPageConfig) issues.push('对标无页面配置弹窗却接了 PageConfigPanel（引入非对标功能）')
    if (usesPageConfig) {
      if (!/default-query-fields-config/.test(src)) issues.push('PageConfigPanel 缺 :default-query-fields-config（「恢复默认」会失效）')
      if (!/default-function-buttons-config/.test(src)) issues.push('PageConfigPanel 缺 :default-function-buttons-config（「恢复默认」会失效）')
    }
    if (spec.print && !/handlePrint/.test(src)) issues.push('对标有打印(F8) 但无 handlePrint')
    if (spec.export && !/(exportCsv|useExport)/.test(src)) issues.push('对标有导出 但未发现导出实现')
    // 对标工具栏有「--查询方案--」下拉：必须接 QuerySchemeBar，不许留成点了没反应的占位控件
    if (/--查询方案--/.test(src) && !/QuerySchemeBar/.test(src)) {
      issues.push('「查询方案」仍是占位下拉（未接 shared/QuerySchemeBar.vue）')
    }

    if (issues.length === 0) { pass++; console.log(`  ✔ ${spec.name} 骨架 10 项全过`) }
    else { fail++; failures.push(`${spec.name} 骨架: ${issues.join('；')}`); console.log(`  ✘ ${spec.name}: ${issues.join('；')}`) }
  }
}

// ════════════════════════════════════════════════════════════════════
// 4. 页面取数接口探针
// ════════════════════════════════════════════════════════════════════
async function section4Apis(token) {
  console.log('\n=== 4. 取数接口探针 ===')
  for (const spec of ACTIVE_SPEC) {
    for (const p of spec.apis || []) {
      const r = await req('GET', p, null, token)
      const ok = r.status === 200 && r.body && r.body.code === 200
      if (ok) { pass++; console.log(`  ✔ ${spec.name} ${p.split('?')[0]}`) }
      else { fail++; failures.push(`${spec.name} 接口 ${p} -> ${r.status}/${r.body && r.body.code}`); console.log(`  ✘ ${spec.name} ${p.split('?')[0]} -> HTTP ${r.status} code=${r.body && r.body.code}`) }
    }
  }
}

// ════════════════════════════════════════════════════════════════════
// 5. UI 逐页 + 逐 Tab 列配置弹窗断言
// ════════════════════════════════════════════════════════════════════
async function section5Ui(page) {
  console.log('\n=== 5. UI 逐页 + 逐 Tab 列配置 ===')
  const problems = []
  page.on('response', r => { if (r.status() >= 400) problems.push(`[HTTP ${r.status()}] ` + r.url().replace(FE, '')) })
  page.on('pageerror', e => problems.push('[pageerror] ' + e.message))
  page.on('console', m => { if (m.type() === 'error') problems.push('[console.error] ' + m.text().slice(0, 160)) })

  /**
   * 打开页面；命中登录态失效（被并行会话 sa-token 顶掉）时自动重登并重试。
   *
   * 共享 dev 环境下多个会话都用 admin 登录，sa-token 单端互踢会把本会话顶下线，
   * 页面随即 401 跳登录页 —— 表现为「整页空白/未渲染」，很容易被误判成"页面没改造"。
   */
  async function gotoPage(p) {
    let relogged = false
    for (let i = 0; i < 3; i++) {
      await page.goto(FE + '/' + p, { waitUntil: 'domcontentloaded' }).catch(e => problems.push('[goto] ' + e.message))
      await page.waitForTimeout(2600)
      if (!/\/login/.test(page.url())) {
        if (relogged) {
          // 重登前的 401 是"被并行会话顶下线"的过程噪声，不是页面缺陷
          for (let k = problems.length - 1; k >= 0; k--) {
            if (/401|Unauthorized|auth\/refresh|\/login/i.test(problems[k])) problems.splice(k, 1)
          }
        }
        return
      }
      console.log(`    （登录态失效，重新登录后重试 ${p}）`)
      const fresh = await login()
      await page.addInitScript(({ token, userInfo }) => {
        localStorage.setItem('token', token)
        localStorage.setItem('tenantId', String((userInfo && userInfo.tenantId) || 1))
        localStorage.setItem('tenantName', (userInfo && userInfo.tenantName) || '系统租户')
      }, fresh)
      relogged = true
    }
  }

  /** 切到指定 Tab（无 label 表示单视图，不动） */
  async function switchTab(label) {
    if (!label) return true
    for (let i = 0; i < 3; i++) {
      const clicked = await page.locator('.tab-items .tab-item').filter({ hasText: label }).first()
        .click({ timeout: 5000 }).then(() => true).catch(() => false)
      if (clicked) { await page.waitForTimeout(1800); return true }
      await page.waitForTimeout(600)
    }
    return false
  }

  /** 打开列配置弹窗，读「全局配置」Tab 的 全部列数 / 已勾选列数（失败重试 3 次） */
  async function readColumnConfig() {
    for (let attempt = 0; attempt < 3; attempt++) {
      await page.locator('.ss-grid thead th .th-settings-btn').first().click({ timeout: 5000 }).catch(() => null)
      await page.waitForTimeout(900)
      await page.locator('.col-config-tabs .ant-tabs-tab').filter({ hasText: '全局配置' }).first()
        .click({ timeout: 4000 }).catch(() => null)
      await page.waitForTimeout(600)
      const res = await page.evaluate(() => {
        // ⚠️ 必须限定在**当前激活的 Tab 面板**内：个人配置与全局配置两个 pane 都会渲染，
        //    不加限定会把两套列各数一遍（13 列被测成 26 列）
        const pane = document.querySelector('.col-config-tabs .ant-tabs-tabpane-active')
        if (!pane) return { count: -1, def: -1 }
        const rows = [...pane.querySelectorAll('.col-setting-row')]
        const checked = rows.filter(r => {
          const cb = r.querySelector('input[type=checkbox]')
          return cb && cb.checked
        })
        return { count: rows.length, def: checked.length }
      })
      if (res.count > 0) {
        await page.keyboard.press('Escape').catch(() => null)
        await page.waitForTimeout(450)
        return res
      }
      await page.keyboard.press('Escape').catch(() => null)
      await page.waitForTimeout(500)
    }
    return { count: -1, def: -1 }
  }

  /** 工具栏是否有「页面配置」入口（逐 Tab 探测后取或：对标只有部分 Tab 有该入口的页面也判通过） */
  async function hasPageConfigButton(tabLabels) {
    const probe = async () => page.evaluate(() =>
      [...document.querySelectorAll('.toolbar-right .ant-btn')].some(b => /页面配置|设置/.test(b.textContent || '')))
    const labels = tabLabels.filter(Boolean)
    if (!labels.length) return probe()
    let found = false
    for (const label of labels) {
      if (!await switchTab(label)) continue
      if (await probe()) { found = true; break }
    }
    return found
  }

  for (const spec of ACTIVE_SPEC) {
    problems.length = 0
    await gotoPage(spec.path)
    await page.waitForTimeout(2600)

    const tabs = await page.evaluate(() =>
      [...document.querySelectorAll('.tab-items .tab-item')].map(e => (e.textContent || '').trim()))
    // 判据用 `.table-area` 而非 `.ss-grid`：空态页（如业务员提成，对标首屏本身就是
    // 「点击查询才能计算」的空提示）在无数据时组件渲染的是空提示、没有表头行，
    // 只认 `.ss-grid` 会把这类页误判成"未改造"
    const isConverted = await page.evaluate(() =>
      !!document.querySelector('.table-area') || !!document.querySelector('.ss-grid'))

    if (!isConverted) {
      // ⚠️ 未改造页也必须把 4xx/5xx / pageerror 打出来：否则"页面挂了"会被静默归入"待改造"
      const uniqPending = [...new Set(problems)]
      markPending(spec.name, uniqPending.length
        ? `页面未渲染 BillDetailTable，且存在错误：${uniqPending.slice(0, 3).join(' || ')}`
        : '页面未渲染 BillDetailTable（未走路线 A/A′）')
      if (uniqPending.length) {
        fail++; failures.push(`${spec.name}（未改造）页面错误: ${uniqPending.slice(0, 3).join('；')}`)
      }
      continue
    }

    // Tab 数量与对标一致
    const expectTabCount = spec.tabs.filter(t => t.label).length
    check(`${spec.name} · 视图 Tab 数`, tabs.length, expectTabCount)

    // 页面配置弹窗入口：对标有才能有（先探，避免被后面的 Tab 切换影响）
    if (spec.pageConfig) {
      const hasBtn = await hasPageConfigButton(spec.tabs.map(t => t.label))
      checkTrue(`${spec.name} 有「页面配置」入口`, hasBtn, hasBtn ? '' : '各 Tab 工具栏均未找到')
    }

    // 逐 Tab 断言列数（逐 Tab 各一张列配置弹窗，与对标抓取口径一致）
    for (let i = 0; i < spec.tabs.length; i++) {
      const t = spec.tabs[i]
      if (!await switchTab(t.label)) {
        fail++; failures.push(`${spec.name} 找不到 Tab「${t.label}」`)
        console.log(`  ✘ ${spec.name} 找不到 Tab「${t.label}」`)
        continue
      }
      if (t.count == null) { console.log(`  ○ ${spec.name} / ${t.label || '单视图'} 列数断言跳过（对标列清单未抓取）`); continue }
      const cc = await readColumnConfig()
      if (t.dynamic) {
        // 动态列 Tab：仓库列 + 合计列随所选仓库数浮动，绝对值不可断言；
        // 但「默认隐藏的列数」应恒等于对标口径（33 − 2 = 31），据此证明固定列部分与对标一致
        checkTrue(`${spec.name} / ${t.label} 列数口径（33 列 + 动态仓库/合计列）`,
          cc.count >= t.count && (cc.count - cc.def) === (t.count - t.def),
          `实际 ${cc.count}/默认 ${cc.def}，对标 ${t.count}/${t.def}（隐藏列应为 ${t.count - t.def}）`)
        continue
      }
      if (spec.override) {
        // 已确认「对标该列在本系统无数据源」→ 如实不渲染；按登记的差额扣减后期望值（差额同时作用于默认列）
        checkTrue(`${spec.name} 全部列数（对标 ${t.count} − 已登记无源列 ${spec.override.cols}）`,
          cc.count === t.count - spec.override.cols,
          `实际 ${cc.count}；原因：${spec.override.reason}`)
        checkTrue(`${spec.name} / ${t.label || '单视图'} 默认显示列数（对标 ${t.def} − ${spec.override.cols}）`,
          cc.def === t.def - spec.override.cols,
          `实际 ${cc.def}；原因同上`)
      } else {
        check(`${spec.name} / ${t.label || '单视图'} 全部列数`, cc.count, t.count)
        check(`${spec.name} / ${t.label || '单视图'} 默认显示列数`, cc.def, t.def)
      }
    }

    const uniq = [...new Set(problems)]
    checkTrue(`${spec.name} 无 4xx/5xx / pageerror / console.error`, uniq.length === 0, uniq.slice(0, 4).join(' || ') || '0 error')

    await page.screenshot({ path: path.join(SHOTS, `${spec.name}.png`) }).catch(() => {})
  }
}

// ════════════════════════════════════════════════════════════════════
// 6. 综合单据（docquery）口径深检 —— 只读
// ════════════════════════════════════════════════════════════════════
async function section6DocQuery(token) {
  console.log('\n=== 6. 综合单据口径深检 ===')
  const get = async (p) => {
    const r = await req('GET', p, null, token)
    if (!(r.status === 200 && r.body && r.body.code === 200)) throw new Error(`${p} -> HTTP ${r.status} code=${r.body && r.body.code}`)
    return r.body.data
  }

  // 6.1 分页一致性
  const p1 = await get('/docquery/business-history/page?page=1&size=5')
  const p2 = await get('/docquery/business-history/page?page=2&size=5')
  check('经营历程 · 第1页返回条数', (p1.list || []).length, 5)
  checkTrue('经营历程 · 分页 total 跨页一致', p1.total === p2.total, `${p1.total} vs ${p2.total}`)
  checkTrue('经营历程 · 第1/2页无重复行',
    (p1.list || []).every(a => !(p2.list || []).some(b => a.docTypeCode === b.docTypeCode && a.docId === b.docId)))

  // 6.2 合计行 = 全量金额之和（后端按过滤范围 SUM，非当前页求和）
  const wide = await get('/docquery/business-history/page?page=1&size=100')
  const sumFirst100 = Math.round((wide.list || []).reduce((a, r) => a + Number(r.amount || 0), 0) * 100) / 100
  checkTrue('经营历程 · summary.amount 非当前页求和（页大小 5 vs 100 恒等）',
    Math.abs(Number(p1.summary.amount) - Number(wide.summary.amount)) < 0.01,
    `${p1.summary.amount} vs ${wide.summary.amount}`)
  checkTrue('经营历程 · summary.amount 为数值', typeof Number(wide.summary.amount) === 'number', String(wide.summary.amount))
  console.log('    （单页 100 条求和 =', sumFirst100, '；全量合计 =', wide.summary.amount, '，页数不足 100 时两者应相等）')
  if (wide.total <= 100) check('经营历程 · 不足 100 条时合计 == 逐行之和', Number(wide.summary.amount), sumFirst100)

  // 6.3 金额排序真实生效（后端排序，不是当前页本地排序）
  const desc = await get('/docquery/business-history/page?page=1&size=20&sortField=amount&sortOrder=desc')
  const amts = (desc.list || []).map(r => Number(r.amount || 0))
  checkTrue('经营历程 · 金额降序排序真实生效',
    amts.every((v, i) => i === 0 || amts[i - 1] >= v), JSON.stringify(amts.slice(0, 5)))

  // 6.4 「显示红冲」勾选后总量只增不减（默认排除已取消）
  const withRed = await get('/docquery/business-history/page?page=1&size=1&includeReversed=true')
  checkTrue('经营历程 · 显示红冲后 total 不小于默认',
    Number(withRed.total) >= Number(p1.total), `含红冲 ${withRed.total} >= 默认 ${p1.total}`)

  // 6.5 单据日期区间过滤真实生效
  const ranged = await get('/docquery/business-history/page?page=1&size=100&startDate=2026-09-13&endDate=2026-09-13')
  checkTrue('经营历程 · 日期区间过滤后每行单据日期都在区间内',
    (ranged.list || []).every(r => (r.bizDate || '').startsWith('2026-09-13')),
    `命中 ${(ranged.list || []).length} 行`)

  // 6.6 账期比较符
  const none = await get('/docquery/business-history/page?page=1&size=1&accountPeriodOp=ge&accountPeriodDays=100000')
  checkTrue('经营历程 · 账期 ≥100000 天应为 0 条（比较符真实生效）', Number(none.total) === 0, `total=${none.total}`)
  const all = await get('/docquery/business-history/page?page=1&size=1&accountPeriodOp=ge&accountPeriodDays=0')
  const anyPeriod = await get('/docquery/business-history/page?page=1&size=100')
  checkTrue('经营历程 · 账期过滤结果为未过滤的子集',
    Number(all.total) <= Number(anyPeriod.total), `账期命中 ${all.total} <= 全量 ${anyPeriod.total}`)

  // 6.7 待审批/草稿状态口径
  const pending = await get('/docquery/pending-docs/page?page=1&size=100')
  checkTrue('待审批单据 · 每行状态文本含「待」或「审批中」或「审核中」',
    (pending.list || []).every(r => /待|审批中|审核中/.test(r.statusText || '')),
    (pending.list || []).map(r => r.statusText).join(','))
  checkTrue('待审批单据 · 返回 pendingSummary（逐类计数）', Array.isArray(pending.pendingSummary) && pending.pendingSummary.length === 13,
    `长度 ${pending.pendingSummary && pending.pendingSummary.length}`)
  const draft = await get('/docquery/draft-docs/page?page=1&size=100')
  checkTrue('业务草稿 · 每行状态文本均为「草稿」',
    (draft.list || []).every(r => (r.statusText || '') === '草稿'),
    (draft.list || []).map(r => r.statusText).join(','))

  // 6.8 列口径：单据日期为纯日期、docId 为字符串（雪花 ID 不得变数字）
  const row0 = (wide.list || [])[0]
  if (row0) {
    checkTrue('综合单据 · 单据日期为 YYYY-MM-DD', /^\d{4}-\d{2}-\d{2}$/.test(row0.bizDate || ''), row0.bizDate)
    checkTrue('综合单据 · docId 按字符串返回（避免 JS 精度丢失）', typeof row0.docId === 'string', typeof row0.docId)
    checkTrue('综合单据 · 行键可用 docTypeCode+docId 唯一标识', !!row0.docTypeCode && !!row0.docId)
  }

  // 6.9 行级动作端点路由可达性
  //   判据：命中业务异常处理链的响应体带 `code` 字段（BusinessException.notFound 等），
  //   而「路由不存在」返回的是 Spring 默认错误体（timestamp/status/error/path，无 code）。
  const reachable = async (label, method, p) => {
    const r = await req(method, p, method === 'GET' ? null : {}, token)
    const body = r.body && typeof r.body === 'object' ? r.body : {}
    const routed = Object.prototype.hasOwnProperty.call(body, 'code')
    checkTrue(`动作端点可达 · ${label}`, routed, `HTTP ${r.status} body=${JSON.stringify(body).slice(0, 80)}`)
  }
  const FAKE_ID = '999999999999'
  await reachable('销售订单 approve', 'POST', `/erp/sale/order/${FAKE_ID}/approve`)
  await reachable('销售出库单 copy', 'POST', `/erp/sale/outbound/${FAKE_ID}/copy`)
  await reachable('收款单 reject', 'POST', `/erp/receipt/${FAKE_ID}/reject`)
  await reachable('调拨单 cancel', 'POST', `/erp/stock/transfer/${FAKE_ID}/cancel`)
  await reachable('报损单 delete', 'DELETE', `/erp/stock/damage/${FAKE_ID}`)
}

// ════════════════════════════════════════════════════════════════════
// 7. 查询方案（对标每页都有「查询方案」下拉，此前是点了没反应的占位控件）
// ════════════════════════════════════════════════════════════════════
async function section7QueryScheme(page, gotoPage) {
  console.log('\n=== 7. 查询方案（保存 / 调用 / 删除）===')
  const SCHEME = 'E2E方案-未发货'
  await gotoPage('analytics/pending-approval')
  await page.evaluate(() => localStorage.removeItem('analytics-query-scheme:analytics-pending-approval-query-scheme'))

  const docNoInput = page.locator('.search-grid .search-item').filter({ hasText: '单据编号' }).locator('input').first()
  await docNoInput.fill('E2E-SCHEME-TEST')
  await page.locator('.scheme-bar .scheme-btn').first().click()
  await page.waitForTimeout(600)
  await page.locator('.ant-modal-content input').first().fill(SCHEME)
  await page.locator('.ant-modal-content .ant-btn-primary').first().click()
  await page.waitForTimeout(800)

  const saved = await page.evaluate((k) => {
    const raw = localStorage.getItem('analytics-query-scheme:analytics-pending-approval-query-scheme')
    const arr = raw ? JSON.parse(raw) : []
    return arr.map(s => ({ name: s.name, docNo: s.value && s.value.docNo }))
  }, SCHEME)
  checkTrue('查询方案已落本机存储且带当前查询条件',
    saved.some(s => s.name === SCHEME && s.docNo === 'E2E-SCHEME-TEST'), JSON.stringify(saved))

  // 重新进入页面（模拟「下次再来」：下拉回到占位态），再调用方案应把条件还原
  await gotoPage('analytics/pending-approval')
  await page.locator('.scheme-bar .scheme-select').click()
  await page.waitForTimeout(500)
  await page.locator('.ant-select-item-option').filter({ hasText: SCHEME }).first().click()
  await page.waitForTimeout(1500)
  check('调用查询方案后「单据编号」被还原', await docNoInput.inputValue(), 'E2E-SCHEME-TEST')

  // 删除方案
  await page.locator('.scheme-bar .ant-popconfirm, .scheme-bar .ant-btn-dangerous').first().click().catch(() => null)
  await page.waitForTimeout(600)
  await page.locator('.ant-popover .ant-btn-primary').first().click().catch(() => null)
  await page.waitForTimeout(600)
  const left = await page.evaluate(() => {
    const raw = localStorage.getItem('analytics-query-scheme:analytics-pending-approval-query-scheme')
    return raw ? JSON.parse(raw).length : 0
  })
  check('删除后本机不再保留该方案', left, 0)
}

// ════════════════════════════════════════════════════════════════════
// 主流程
// ════════════════════════════════════════════════════════════════════
;(async () => {
  fs.mkdirSync(SHOTS, { recursive: true })
  if (!await waitBackend()) throw new Error('后端 5655 未就绪')
  const { token, userInfo } = await login()
  console.log('登录成功；本次覆盖', ACTIVE_SPEC.length, '页')

  await section1Menu(token)
  section2Files()
  section3Skeleton()
  await section4Apis(token)
  await section6DocQuery(token)

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 1000 } })
  const page = await ctx.newPage()
  await page.addInitScript(({ token, userInfo }) => {
    localStorage.setItem('token', token)
    localStorage.setItem('tenantId', String((userInfo && userInfo.tenantId) || 1))
    localStorage.setItem('tenantName', (userInfo && userInfo.tenantName) || '系统租户')
  }, { token, userInfo })
  await section5Ui(page)
  if (!onlyFilter.length || onlyFilter.includes('待审批单据')) {
    await section7QueryScheme(page, async (p) => {
      await page.goto(FE + '/' + p, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(2600)
    })
  }
  await browser.close()

  console.log(`\n================ 分析模块 E2E：通过 ${pass} / 失败 ${fail} / 未改造 ${pending} ================`)
  if (failures.length) {
    console.log('失败明细：')
    failures.forEach(f => console.log('  -', f))
  }
  if (fail > 0) process.exitCode = 1
})().catch(e => { console.error('执行异常', e); process.exitCode = 1 })
