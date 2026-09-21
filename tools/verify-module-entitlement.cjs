#!/usr/bin/env node
/**
 * 模块 entitlement 门 · 实机验证（2026-09-21）
 *
 * 要钉死的是**三层判定链**：`@SaCheckPermission` 权限码 → 归属模块
 * （`sys_module_permission` 最长前缀）→ 该模块是否已给本租户开通 → 未开通 403。
 *
 * 覆盖四条（拒绝路径与放行路径都测，且**总是恢复现场**）：
 *   ① 放行路径：超管（豁免）与「有码 + 模块已开通」的租户用户 → 200
 *   ② 顺序验证：租户 2 调一个它**既没权限码、模块也没开通**的接口（/tenant/page）→
 *      文案必须是「无权限访问: system:tenant:list」，**不能**是「模块未开通」。
 *      这条是 order 3 落在 `@SaCheckPermission` 之后的**唯一可证伪证据** ——
 *      顺序错了只会换个文案、不会报错，所以必须显式断言。
 *   ③ 拒绝路径：临时把租户 2 的「设置」模块置为停用（sys_tenant_module.status=1）→
 *      同一个「有码 + 模块被停用」的用户再调 → 403「模块未开通：设置（settings）」
 *   ④ 恢复路径：改回 status=0 → 又变回 200（证明这条 403 确实是模块开关造成的）
 *
 * ⚠️ 会**写一行** `sys_tenant_module.status`（最后恢复）。任何一步失败都会在 finally 里恢复。
 * ⚠️ 有 **31s × 2** 的等待：判定结果有 30s 进程内缓存（`ModuleEntitlementService.TENANT_TTL_MS`），
 *    不等就可能读到旧值，从而把"缓存未过期"误判成功能失效。
 *
 * 用法：node tools/verify-module-entitlement.cjs
 */
const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')
const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`

const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }
const TENANT2 = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2' }

/** 探针接口：@SaCheckPermission("tenant-admin:user:list") ⇒ 归属模块 settings（前缀 tenant-admin:） */
const PROBE_WITH_CODE = '/user/page'
/** 对照接口：@SaCheckPermission("system:tenant:list") ⇒ 归属模块 system（租户 2 未开通） */
const PROBE_OTHER_MODULE = '/tenant/page'

const T2 = 2
const MODULE = 'settings'
const CACHE_WAIT_MS = 31_000

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)
const sleep = (ms) => new Promise(r => setTimeout(r, ms))

function sql(stmt) {
  return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
    { cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 }).trim()
}

/** 取单值查询的第一个数据行（sql.cjs 输出形如：表头 / 分隔线 / 数据 / "(N rows)"） */
function scalar(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').split('\n')
    .map(l => l.trim())
    .filter(l => l && !/^-+$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
  return lines.length > 1 ? lines[1] : '(空)'
}

const setModuleStatus = (status) =>
  sql(`UPDATE sys_tenant_module SET status = ${status}, update_time = now()
       WHERE tenant_id = ${T2} AND module_code = '${MODULE}' AND deleted = 0`)

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
  try { json = JSON.parse(text) } catch { /* 非 JSON */ }
  return { status: res.status, json, text }
}

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

  const before = scalar(`SELECT status FROM sys_tenant_module
    WHERE tenant_id = ${T2} AND module_code = '${MODULE}' AND deleted = 0`)
  console.log(`前置：租户 ${T2} 的「${MODULE}」模块 status = ${before}`)

  const adminToken = await login(ADMIN)
  const t2Token = await login(TENANT2)
  console.log(`登录成功: admin（超管） / ${TENANT2.u}（租户 ${T2}）`)

  let mutated = false
  try {
    // ══ ① 放行路径 ══
    section('① 放行路径：模块已开通时不受影响')
    const admin1 = await req('GET', PROBE_WITH_CODE, { token: adminToken })
    ok('【放行路径】超管（entitlement 豁免）→ 200', admin1.status === 200, `status=${admin1.status}`)

    const t2Open = await req('GET', PROBE_WITH_CODE, { token: t2Token })
    ok('【放行路径】租户 2 有码 + 「设置」已开通 → 200', t2Open.status === 200,
      `status=${t2Open.status} ${t2Open.json?.message || ''}`)

    // ══ ② 顺序：权限检查必须排在模块门之前 ══
    section('② 顺序验证：无权限时文案必须是「无权限访问」，不能被模块门抢先')
    const t2Other = await req('GET', PROBE_OTHER_MODULE, { token: t2Token })
    const msgOther = t2Other.json?.message || ''
    ok('【顺序断言】租户 2 调 /tenant/page → 403', t2Other.status === 403, `status=${t2Other.status}`)
    ok('【顺序断言】文案是权限拒绝，不是模块拦截',
      /无权限访问/.test(msgOther) && !/模块未开通/.test(msgOther), `message=${msgOther}`)

    // ══ ③ 拒绝路径：模块停用 ══
    section(`③ 拒绝路径：停用租户 ${T2} 的「${MODULE}」模块 → 该用户被拦`)
    setModuleStatus(1); mutated = true
    console.log(`  已把 sys_tenant_module(tenant_id=${T2}, ${MODULE}).status 置为 1，等 ${CACHE_WAIT_MS / 1000}s 让缓存过期…`)
    await sleep(CACHE_WAIT_MS)

    const t2Closed = await req('GET', PROBE_WITH_CODE, { token: t2Token })
    const msgClosed = t2Closed.json?.message || ''
    ok('【拒绝路径】同一个人、同一个接口 → 403', t2Closed.status === 403, `status=${t2Closed.status}`)
    ok('【拒绝路径】文案是模块拦截（可与「无权限访问」区分）',
      /模块未开通/.test(msgClosed), `message=${msgClosed}`)

    const adminClosed = await req('GET', PROBE_WITH_CODE, { token: adminToken })
    ok('【放行路径】模块被停用也不影响超管（豁免口径）', adminClosed.status === 200,
      `status=${adminClosed.status}`)

    // ══ ④ 恢复路径 ══
    section('④ 恢复路径：改回开通 → 立即恢复放行')
    setModuleStatus(0); mutated = false
    console.log(`  已恢复 status=0，等 ${CACHE_WAIT_MS / 1000}s…`)
    await sleep(CACHE_WAIT_MS)

    const t2Restored = await req('GET', PROBE_WITH_CODE, { token: t2Token })
    ok('【恢复路径】又回到 200（证明上面那条 403 就是模块开关造成的）',
      t2Restored.status === 200, `status=${t2Restored.status} ${t2Restored.json?.message || ''}`)
  } finally {
    if (mutated) {
      console.log('\n!! 异常退出：正在把模块状态恢复为 0')
      setModuleStatus(0)
    }
    const after = scalar(`SELECT status FROM sys_tenant_module
      WHERE tenant_id = ${T2} AND module_code = '${MODULE}' AND deleted = 0`)
    console.log(`\n现场恢复核对：租户 ${T2} 的「${MODULE}」status = ${after}（应为 0）`)
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('脚本异常:', e.message); process.exit(2) })
