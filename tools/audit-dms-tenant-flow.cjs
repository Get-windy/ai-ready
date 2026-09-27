/**
 * 配送 · 「平台开模块 → 租户自助配权限」闭环验证（2026-09-26）
 *
 * 验证目标（用租户管理员账号实测，而非超管）：
 *   1. 模块门已开（settings/dms）→ 租户管理员能调「角色/权限管理」接口
 *   2. 租户管理员能读到可分配权限清单（含 dms: 码）
 *   3. 租户管理员能把 dms 权限赋给本租户角色
 *   4. 赋权后，配送接口从 403 变为可用
 *
 * ⚠️ 依赖：本脚本会**写数据**（给租户2管理员角色补 1 个 dms 权限码）。
 *    assignPermissions 是**覆盖式**（先删后插）⇒ 提交时带「现有 + 新增」全量。
 *
 * 用法：node tools/audit-dms-tenant-flow.cjs
 */
const http = require('http')

const API = process.env.E2E_API || 'http://localhost:5655/api'
const ACC = { user: 'e2e_hr_t2', pwd: 'admin123', tenant: 'E2E验收租户2' }
// ⚠️ 必须是**字符串**：2099000000000009031 > Number.MAX_SAFE_INTEGER(9007199254740991)，
//    写成数字字面量会被舍入成 2099000000000009000 → 打错角色 ⇒ 500（2026-09-27 实踩）
const T2_ROLE_ID = '2099000000000009031'
const PROBE_DMS = '/dms/vehicle/page?pageNum=1&pageSize=1'
const WANT_CODE = 'dms:vehicle:list'   // 最小集：只补 1 个码证明链路

function req(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const url = new URL(API + path)
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: url.hostname, port: url.port, path: url.pathname + url.search, method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
      },
    }, res => {
      let buf = ''
      res.on('data', c => { buf += c })
      res.on('end', () => {
        let json = null
        try { json = JSON.parse(buf) } catch { /* 非 JSON */ }
        resolve({ status: res.statusCode, json, raw: buf })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login(acc) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    username: acc.user, password: acc.pwd, tenantName: acc.tenant,
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  return token
}

const sleep = ms => new Promise(r => setTimeout(r, ms))

function walkPerms(nodes, out = []) {
  for (const n of nodes || []) { out.push(n); if (n.children) walkPerms(n.children, out) }
  return out
}

;(async () => {
  const token = await login(ACC)
  const info = await req('GET', '/auth/userinfo', null, token)
  const d = info.json?.data || {}
  console.log(`账号 ${ACC.user}  角色=${JSON.stringify(d.roles)}  权限码数=${(d.permissions || []).length}`)

  // ① 能否调「角色管理」接口（模块门 + 权限码双重放行）
  const before = await req('GET', PROBE_DMS, null, token)
  console.log(`\n① 赋权前  ${PROBE_DMS}  →  HTTP ${before.status}`)

  const rolePerms = await req('GET', `/role/${T2_ROLE_ID}/permissions`, null, token)
  console.log(`② GET /role/{id}/permissions  →  HTTP ${rolePerms.status}${rolePerms.status === 200 ? '（租户管理员可读本租户角色权限 ✓）' : ''}`)
  if (rolePerms.status !== 200) {
    console.log('   ⇒ 第二层仍未打通，终止。响应：' + String(rolePerms.raw).slice(0, 200)); process.exit(1)
  }
  const currentIds = rolePerms.json?.data || []

  // ③ 可分配权限清单里有没有 dms: 码
  const tree = await req('GET', '/permission/tree?tenantId=2', null, token)
  const flat = walkPerms(tree.json?.data || [])
  const dmsPerms = flat.filter(p => String(p.permissionCode || '').startsWith('dms:'))
  console.log(`③ GET /permission/tree?tenantId=2  →  HTTP ${tree.status}；清单含权限码 ${flat.length} 个，其中 dms: ${dmsPerms.length} 个`)

  const want = flat.find(p => p.permissionCode === WANT_CODE)
  if (!want) { console.log(`   !! 清单里找不到 ${WANT_CODE}，终止`); process.exit(1) }
  // 权限 id 同样可能是雪花 ID ⇒ 统一按字符串比对/提交
  const wantId = String(want.id)
  const rolePermRes = await req('GET', `/role/${T2_ROLE_ID}/permissions`, null, token)
  const nowIds = (rolePermRes.json?.data || []).map(String)
  const isAlready = nowIds.includes(wantId)
  if (isAlready) {
    console.log(`   角色已持有 ${WANT_CODE}，跳过赋权直接验证`)
  } else {
    // ④ 赋权（覆盖式 ⇒ 现有 + 新增）
    const payload = Array.from(new Set([...nowIds, wantId]))
    const assign = await req('POST', `/role/${T2_ROLE_ID}/permissions`, payload, token)
    console.log(`④ POST /role/${T2_ROLE_ID}/permissions  →  HTTP ${assign.status}（提交 ${payload.length} 个权限 id = 现有 ${nowIds.length} + 新增 1）`)
    if (assign.status !== 200) { console.log('   !! 赋权失败：' + String(assign.raw).slice(0, 300)); process.exit(1) }
  }

  // ⑤ 等权限缓存过期（UnifiedPermissionCacheService L1 30s）后重新登录验证
  console.log('\n⑤ 等待权限缓存过期（32s）后重新登录验证…')
  await sleep(32000)
  const token2 = await login(ACC)
  const info2 = await req('GET', '/auth/userinfo', null, token2)
  const p2 = (info2.json?.data?.permissions) || []
  console.log(`   重新登录后权限码数=${p2.length}；含 ${WANT_CODE} = ${p2.includes(WANT_CODE)}`)
  const after = await req('GET', PROBE_DMS, null, token2)
  console.log(`   赋权后  ${PROBE_DMS}  →  HTTP ${after.status}`)
  console.log(`\n结论：${before.status} → ${after.status}` +
    (after.status !== 403 ? '  ✅ 链路打通（403 已消除）' : '  ❌ 仍 403'))
  process.exit(0)
})()
