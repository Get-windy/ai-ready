#!/usr/bin/env node
/**
 * 双层租户加固 · 重启后验证（2026-09-21）
 *
 * 覆盖「夜间批次 A」五项改动的**运行时**行为：
 *   ① F-02  `TenantHeaderInterceptor`：X-Tenant-Id 与会话租户比对
 *   ② F-03  `SystemConfigServiceImpl.effectiveTenant` 非超管强制会话租户
 *   ③ F-04  `TenantController` 读接口补鉴权（system:tenant:list / query）
 *   ④ F-06  `SysTenantMenuController` 平台管理员硬校验
 *   ⑤ 回归：超管不受影响、菜单树仍可读
 *
 * ⚠️ 纪律（来自记忆 build-flyway-traps）：安全类改动**必须同时测拒绝路径与放行路径**。
 *    只测「应当被拒绝」的请求会 100% 漏掉放行路径上的 P0 ——
 *    拒绝在建立连接/进入业务前就返回了，永远走不到真正有问题的那段代码。
 *
 * 用法：
 *   node tools/verify-tenant-hardening.cjs
 *   PORT=5655 node tools/verify-tenant-hardening.cjs
 *
 * 只读验证：不写任何业务数据（F-03 的写路径仅做权限判定观察，不发写请求）。
 */
const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`

// 超管：租户 1（SYSTEM，admin 所在）
const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }
// 非超管：租户 2，角色 E2E_T2_ADMIN（无 system:tenant:* / tenant:menu:* 权限码）
const TENANT2 = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2' }

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

async function req(method, p, { token, headers = {}, body } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON 响应 */ }
  return { status: res.status, json, text }
}

/** 登录（走验证码流程，与其它 e2e 脚本一致） */
async function login(user) {
  const cap = await req('GET', '/auth/captcha')
  if (!cap.json?.data?.img) throw new Error(`验证码接口异常: ${cap.text.slice(0, 200)}`)
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    body: {
      username: user.u, password: user.p, tenantName: user.t,
      captcha: code, captchaKey: cap.json.data.uuid,
    },
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`${user.u} 登录失败: ${res.text.slice(0, 300)}`)
  return token
}

;(async () => {
  console.log(`验证目标: ${BASE}`)

  const adminToken = await login(ADMIN)
  console.log(`登录成功: ${ADMIN.u}（超管 / 租户 1）`)
  const t2Token = await login(TENANT2)
  console.log(`登录成功: ${TENANT2.u}（非超管 / 租户 2）`)

  // ══ ① F-02 租户请求头与会话一致性 ══
  // 探针必须选「不带权限码」的端点，否则会把「权限拒绝 403」误判成「头拒绝 403」。
  // `/tenant/current` 刻意不加 @SaCheckPermission（租户读自己企业信息），正好隔离本拦截器。
  section('① F-02 X-Tenant-Id 与会话租户比对（探针 /tenant/current）')
  const cross = await req('GET', '/tenant/current', { token: t2Token, headers: { 'X-Tenant-Id': '1' } })
  ok('【拒绝路径】非超管带别的租户头 → 403 且是拦截器文案',
    cross.status === 403 && /无权访问其他租户的数据/.test(cross.json?.message || ''),
    `status=${cross.status} ${cross.json?.message || ''}`)

  const match = await req('GET', '/tenant/current', { token: t2Token, headers: { 'X-Tenant-Id': '2' } })
  ok('【放行路径】非超管带自己的租户头 → 200', match.status === 200, `status=${match.status}`)

  const noHeader = await req('GET', '/tenant/current', { token: t2Token })
  ok('【放行路径】不带头（由会话决定租户）→ 200', noHeader.status === 200, `status=${noHeader.status}`)

  const adminCross = await req('GET', '/tenant/current', { token: adminToken, headers: { 'X-Tenant-Id': '2' } })
  ok('【放行路径】超管带任意租户头 → 非 403（切换租户是超管能力）',
    adminCross.status !== 403, `status=${adminCross.status}`)

  const badHeader = await req('GET', '/tenant/current', { token: t2Token, headers: { 'X-Tenant-Id': 'abc' } })
  ok('【容错路径】非法头值不炸接口 → 非 403/500', badHeader.status !== 403 && badHeader.status < 500,
    `status=${badHeader.status}`)

  // ══ ② F-04 平台侧租户读接口补鉴权 ══
  section('② F-04 /api/tenant/** 读接口补鉴权码')
  const tenantPageAdmin = await req('GET', '/tenant/page?current=1&size=5', { token: adminToken })
  ok('【放行路径】超管读租户列表 → 200', tenantPageAdmin.status === 200, `status=${tenantPageAdmin.status}`)

  const tenantPageT2 = await req('GET', '/tenant/page?current=1&size=5', { token: t2Token })
  ok('【拒绝路径】非超管无 system:tenant:list → 403', tenantPageT2.status === 403,
    `status=${tenantPageT2.status} ${tenantPageT2.json?.message || ''}`)

  const tenantDetailT2 = await req('GET', '/tenant/1', { token: t2Token })
  ok('【拒绝路径】非超管无 system:tenant:query → 403', tenantDetailT2.status === 403,
    `status=${tenantDetailT2.status}`)

  const current = await req('GET', '/tenant/current', { token: t2Token })
  ok('【放行路径】租户读自己企业信息 /tenant/current 刻意不加码 → 非 403',
    current.status !== 403, `status=${current.status}`)

  // ══ ③ F-06 平台管理员硬校验 ══
  section('③ F-06 SysTenantMenuController 平台管理员硬校验')
  const menuAdmin = await req('GET', '/tenant-menu/2', { token: adminToken })
  ok('【放行路径】超管查租户菜单授权 → 200', menuAdmin.status === 200, `status=${menuAdmin.status}`)

  const menuT2 = await req('GET', '/tenant-menu/2', { token: t2Token })
  ok('【拒绝路径】非平台管理员查租户菜单授权 → 403', menuT2.status === 403,
    `status=${menuT2.status} ${menuT2.json?.message || ''}`)

  // ══ ④ F-03 系统参数作用域 ══
  section('④ F-03 /api/config/** 作用域（effectiveTenant 加固）')
  const cfgT2 = await req('GET', '/config/list', { token: t2Token, headers: { 'X-Tenant-Id': '0' } })
  ok('非超管带头 X-Tenant-Id:0 不再报 5xx（头被忽略改走会话租户）', cfgT2.status < 500,
    `status=${cfgT2.status}`)

  // ══ ⑤ 回归 ══
  section('⑤ 回归：既有能力未被破坏')
  // ⚠️ /menu/tree 需要显式 tenantId 参数，缺参会 400「缺少必要参数: tenantId」——
  //    这是接口自身的入参要求，与租户加固无关，别把它当成回归。
  const menuTreeAdmin = await req('GET', '/menu/tree?tenantId=1', { token: adminToken,
    headers: { 'X-Tenant-Id': '1' } })
  ok('超管菜单树仍 200', menuTreeAdmin.status === 200, `status=${menuTreeAdmin.status}`)
  // ⚠️ `tenantId` 是 @RequestParam（必填查询参数），不是请求头 —— 缺参会 400「缺少必要参数: tenantId」。
  const permPage = await req('GET', '/permission/page?tenantId=0&current=1&size=1', { token: adminToken,
    headers: { 'X-Tenant-Id': '1' } })
  ok('权限列表仍 200', permPage.status === 200,
    `status=${permPage.status}${permPage.json?.message ? ' ' + permPage.json.message : ''}`)

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => {
  console.error('\n验证脚本异常:', e.message)
  process.exit(2)
})
