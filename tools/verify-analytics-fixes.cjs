/**
 * 分析模块审计修复 · 后端侧验证（2026-09-23）
 *
 * 覆盖三条修复：
 *   ① DocQueryService EXPENSE 分支补租户/软删 → 经营历程、待审批、草稿三个接口仍正常（SQL 可执行）
 *   ② sys_menu.menu_level 3→0（V11.499.0）→ 非系统租户可见分析模块
 *   ③ mall/admin/user/page 拆箱 NPE 修复 → 接口 200（该修复在 erp-mall 模块，需该模块已重新构建）
 *
 * 路由侧（80421 采购分析）验证见 tools/verify-menu-route-collision.cjs
 * 运行：node tools/verify-analytics-fixes.cjs
 */
const http = require('http')

const BE = Number(process.env.BE_PORT || 5655)

function req(method, p, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: BE, path: '/api' + p, method,
      headers: { 'Content-Type': 'application/json', ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    }, res => {
      const c = []
      res.on('data', d => c.push(d))
      res.on('end', () => { const s = Buffer.concat(c).toString('utf8'); try { resolve({ status: res.statusCode, body: JSON.parse(s) }) } catch { resolve({ status: res.statusCode, body: s }) } })
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
  const r = await req('POST', '/auth/login', { username, password: 'admin123', tenantName, captcha: code, captchaKey: cap.body.data.uuid })
  const d = r.body && r.body.data
  if (!d || !(d.token || d.accessToken)) throw new Error(username + ' 登录失败: ' + JSON.stringify(r.body).slice(0, 200))
  return d.token || d.accessToken
}

let pass = 0, fail = 0
const failures = []
function check(name, ok, detail) {
  if (ok) { pass++; console.log('  ✔', name, detail ? '-> ' + detail : '') }
  else { fail++; failures.push(`${name} -> ${detail}`); console.log('  ✘', name, '->', detail) }
}

;(async () => {
  // ── ① 综合单据三接口（验证 EXPENSE 分支 SQL 可执行）──
  console.log('\n=== ① DocQueryService EXPENSE 分支修复（SQL 可执行性）===')
  const tk = await login('e2e_hr', '系统租户')
  for (const [label, p] of [
    ['经营历程', '/docquery/business-history/page?page=1&size=5'],
    ['待审批单据', '/docquery/pending-docs/page?page=1&size=5'],
    ['业务草稿', '/docquery/draft-docs/page?page=1&size=5']
  ]) {
    const r = await req('GET', p, null, tk)
    const code = r.body && r.body.code
    check(`${label} 接口`, code === 200, `HTTP ${r.status} code=${code}${code !== 200 ? ' msg=' + (r.body && r.body.message) : ''}`)
    if (code === 200) {
      const total = r.body.data && r.body.data.total
      console.log(`     （total=${total}，13 类单据 UNION 含 EXPENSE 分支）`)
    }
  }

  // ── ② 非系统租户可见性（menu_level 3→0）──
  console.log('\n=== ② menu_level 修复（非系统租户应能看到分析模块）===')
  try {
    const t2 = await login('e2e_hr_t2', 'E2E验收租户2')
    const m = await req('GET', '/menu/user/mega/tenant-admin?tenantId=2', null, t2)
    const flat = []
    const walk = ns => (ns || []).forEach(n => { flat.push(n); walk(n.children) })
    walk(m.body && m.body.data)
    const ana = flat.filter(x => (x.path || '').startsWith('analytics/'))
    check('tenant 2 菜单包含 analytics 页面', ana.length > 0, `共 ${ana.length} 条（修复前为 0）`)
    const levels = {}
    flat.forEach(x => { levels[x.menuLevel] = (levels[x.menuLevel] || 0) + 1 })
    console.log('     返回菜单的 menuLevel 分布:', JSON.stringify(levels))
    console.log('     样例:', ana.slice(0, 5).map(x => x.path).join(', ') || '无')
  } catch (e) {
    check('tenant 2 菜单检查', false, e.message)
  }

  // ── ③ 商城用户分页（拆箱 NPE）──
  console.log('\n=== ③ mall/admin/user/page 拆箱 NPE 修复 ===')
  const r3 = await req('GET', '/erp/mall/admin/user/page?pageNum=1&pageSize=20', null, tk)
  const c3 = r3.body && r3.body.code
  check('mall/admin/user/page', c3 === 200, `HTTP ${r3.status} code=${c3}${c3 !== 200 ? ' msg=' + (r3.body && r3.body.message) : ''}`)
  if (c3 !== 200) {
    console.log('     ⚠️ 若仍为 500，说明 erp-mall 模块的修复尚未重新构建进 fat jar（该文件属交易模块会话改动）')
  }

  console.log(`\n============ 修复验证：通过 ${pass} / 失败 ${fail} ============`)
  if (failures.length) failures.forEach(f => console.log('  -', f))
  if (fail > 0) process.exitCode = 1
})().catch(e => { console.error('执行异常', e); process.exitCode = 1 })
