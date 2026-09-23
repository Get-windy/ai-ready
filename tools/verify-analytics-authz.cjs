/**
 * 分析模块 · 权限与菜单实测（只读）
 *
 * 目的：验证三个问题
 *   ① 非超管角色（SYSTEM_ADMIN / DEPT_ADMIN）能否调用分析模块的取数接口
 *   ② 非系统租户（tenant 2）的菜单接口里，分析模块是否被 menu_level 过滤掉
 *   ③ 超管对照（证明接口本身是通的，403 不是接口不存在）
 *
 * 运行：node tools/verify-analytics-authz.cjs
 * 依赖：后端 5655 在跑；账号由 tools/e2e-hr-user.sql 建立（密码统一 admin123）
 */
const http = require('http')

const BE = Number(process.env.BE_PORT || 5655)

function req(method, p, body, token, tenantId) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: BE, path: '/api' + p, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(tenantId ? { 'X-Tenant-Id': String(tenantId) } : {})
      }
    }, res => {
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

async function login(username, tenantName) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.body.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    username, password: 'admin123', tenantName,
    captcha: code, captchaKey: cap.body.data.uuid
  })
  const d = res.body && res.body.data
  if (!d || !(d.token || d.accessToken)) throw new Error(`${username} 登录失败: ` + JSON.stringify(res.body).slice(0, 200))
  return { token: d.token || d.accessToken, info: d }
}

// 分析模块 31 页实际调用的取数接口（每页取一个代表端点）
const PROBES = [
  ['经营历程', 'GET', '/docquery/business-history/page?page=1&size=1'],
  ['待审批单据', 'GET', '/docquery/pending-docs/page?page=1&size=1'],
  ['销售分析', 'GET', '/erp/sale/analysis/sales-analysis/page?page=1&size=1'],
  ['客户活跃分析', 'GET', '/erp/sale/analysis/customer-active/page?page=1&size=1'],
  ['推广分析', 'GET', '/erp/sale/analysis/promotion-funnel/page'],
  ['预订货查询', 'GET', '/erp/sale/pre-order/analysis/page?tab=product&page=1&size=1'],
  ['采购分析', 'GET', '/erp/purchase/analytics/page?tab=supplier&page=1&size=1'],
  ['进销存分析', 'GET', '/erp/stock/analytics/page?tab=product&page=1&size=1'],
  ['查库存', 'GET', '/erp/stock/page?pageNum=1&pageSize=1'],
  ['库存明细', 'GET', '/erp/stock/flow/page?page=1&size=1'],
  ['业务员提成', 'GET', '/erp/marketing/commission/staff-summary/page?page=1&size=1'],
  ['业绩提成中心', 'GET', '/erp/marketing/commission/analytics/overview'],
  ['回款统计', 'GET', '/erp/finance/collection-stats'],
  ['发票统计', 'GET', '/erp/invoice/date-range?startDate=2026-01-01&endDate=2026-12-31'],
  ['往来余额表', 'GET', '/erp/finance/partner-balance/page?pageNum=1&pageSize=1'],
  ['查应收', 'GET', '/erp/finance/auxiliary/balance/page?pageNum=1&pageSize=1&subjectCode=1122&auxType=CUSTOMER'],
  ['查费用', 'GET', '/erp/expense/statistics/by-department'],
  ['交易分析', 'GET', '/erp/mall/admin/trade-analysis?startDate=2026-01-01&endDate=2026-12-31'],
]

;(async () => {
  const results = []

  // ── ① 超管对照 ──
  // ⚠️ 不用 e2e_analytics：它被 e2e-analytics.cjs 占用，同 loginId 二次登录会互踢。
  //    改用同为 SUPER_ADMIN 的 e2e_hr 做对照（不同 loginId，互不影响）。
  const admin = await login('e2e_hr', '系统租户')
  console.log('\n=== ① 超管 e2e_hr（tenant 1, SUPER_ADMIN）对照 ===')
  for (const [name, m, p] of PROBES) {
    const r = await req(m, p, null, admin.token)
    const code = r.body && r.body.code
    console.log(`  ${code === 200 ? '✔' : '✘'} ${name.padEnd(12)} ${String(r.status).padEnd(4)} code=${code}`)
    results.push({ user: 'SUPER_ADMIN', name, status: r.status, code })
  }

  // ── ② 非超管：SYSTEM_ADMIN ──
  let ta
  try { ta = await login('e2e_hr_ta', '系统租户') } catch (e) { console.log('\n[跳过] e2e_hr_ta 登录失败：' + e.message) }
  if (ta) {
    console.log('\n=== ② 非超管 e2e_hr_ta（tenant 1, SYSTEM_ADMIN，持 222 权限）===')
    let denied = 0
    for (const [name, m, p] of PROBES) {
      const r = await req(m, p, null, ta.token)
      const code = r.body && r.body.code
      if (code !== 200) denied++
      console.log(`  ${code === 200 ? '✔ 可访问' : '✘ ' + (r.body && r.body.message || r.status)} ${name.padEnd(12)} ${String(r.status).padEnd(4)} code=${code}`)
      results.push({ user: 'SYSTEM_ADMIN', name, status: r.status, code, msg: r.body && r.body.message })
    }
    console.log(`  → SYSTEM_ADMIN 下被拒 ${denied}/${PROBES.length} 个分析接口`)
  }

  // ── ③ 非超管：DEPT_ADMIN ──
  const deptUser = await req('GET', '/auth/captcha')
  // DEPT_ADMIN 无独立账号，改用 e2e_hr_t2（另租户靶子）验证跨租户菜单
  console.log('\n=== ③ 非系统租户 e2e_hr_t2（tenant 2）菜单接口 ===')
  try {
    const t2 = await login('e2e_hr_t2', 'E2E验收租户2')
    const menu = await req('GET', '/menu/user/mega/tenant-admin', null, t2.token, 2)
    const flat = []
    const walk = (ns) => (ns || []).forEach(n => { flat.push(n); walk(n.children) })
    walk(menu.body && menu.body.data)
    const anaMenus = flat.filter(m => (m.path || '').startsWith('analytics/'))
    console.log(`  tenant 2 菜单接口 HTTP ${menu.status} code=${menu.body && menu.body.code}，共返回 ${flat.length} 个节点`)
    console.log(`  其中 analytics/* 菜单：${anaMenus.length} 个 [${anaMenus.map(m => m.path).join(', ') || '无'}]`)
    results.push({ user: 'TENANT2_MENU', name: 'menu-tree', status: menu.status, code: menu.body && menu.body.code, anaMenuCount: anaMenus.length, totalNodes: flat.length })
  } catch (e) {
    console.log('  [跳过] e2e_hr_t2 登录失败：' + e.message)
  }

  const fs = require('fs')
  fs.mkdirSync('I:/AI-Ready/tool-results/analytics-audit', { recursive: true })
  fs.writeFileSync('I:/AI-Ready/tool-results/analytics-audit/authz-probe.json', JSON.stringify(results, null, 1), 'utf8')
  console.log('\n结果已写 tool-results/analytics-audit/authz-probe.json')
})().catch(e => { console.error('执行异常', e); process.exitCode = 1 })
