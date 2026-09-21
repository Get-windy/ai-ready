#!/usr/bin/env node
/**
 * 模块鉴权注解验证（E-01 / E-04）· 重启后运行（2026-09-21）
 *
 * 用途：验证「先补权限码种子 → 再补 @SaCheckPermission」这一对改动的**运行时**效果。
 *
 * ⚠️ 为什么必须跑真机：补注解有两类静默失败，编译期完全看不出来 ——
 *   ① **码不在库中** ⇒ 该接口对**所有非超管一律 403**（含本应有权限的角色）；
 *   ② 注解**没真正插到方法上**（如建在错误位置）⇒ 看起来改了、实际没生效，**静默 fail-open**。
 *   只测「超管返回 200」两条都发现不了（超管走 ["*"] 通配）——
 *   必须同时断言**非超管被拒**（证明注解生效）与**超管放行**（证明码存在）。
 *
 * 用法：
 *   node tools/verify-module-authz.cjs budget
 *   node tools/verify-module-authz.cjs budget --port 5655
 *
 * 只读验证：仅发 GET / 无副作用的探测请求；control:check 用业务上必然被拒的入参，
 * 目的是观察**鉴权层**是否放行，不实际改动预算数据。
 */
const fs = require('fs')
const path = require('path')

const moduleName = process.argv[2]
if (!moduleName) {
  console.error('用法: node tools/verify-module-authz.cjs <module>')
  process.exit(2)
}
const portArg = process.argv.indexOf('--port')
const PORT = portArg > -1 ? process.argv[portArg + 1] : (process.env.PORT || '5655')
const BASE = `http://localhost:${PORT}/api`

const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }
// 租户 2 的非超管账号：无任何 budget:* 码
const OUTSIDER = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2' }

// 每个模块的探测端点：至少覆盖「读」与「写」各一个资源，
// 证明注解是按方法粒度插的，而不是只落在某一个上。
const PROBES = {
  budget: [
    { m: 'GET', p: '/erp/budget/annual/page?current=1&size=1', code: 'budget:annual:list' },
    { m: 'GET', p: '/erp/budget/adjustment/page?current=1&size=1', code: 'budget:adjustment:list' },
    { m: 'GET', p: '/erp/budget/template/page?current=1&size=1', code: 'budget:template:list' },
    { m: 'GET', p: '/erp/budget/execution/rows', code: 'budget:execution:list' },
    { m: 'GET', p: '/erp/budget/report/trend', code: 'budget:report:view' },
    { m: 'GET', p: '/erp/budget/item/list-by-budget/1', code: 'budget:item:list' },
    // 写接口：用一个必然不存在的预算去校验，观察是否停在鉴权层（期望超管非 403）
    { m: 'POST', p: '/erp/budget/control/check', code: 'budget:control:check',
      body: { budgetId: -1, amount: 1 } },
  ],
  // ⚠️ 以下 p 是**不含 /api 前缀**的路径（BASE 里已含 /api）。
  //    路径与码均取自 `tools/gen-module-permission-seed.py <模块>` 的真实产出，不要手猜 ——
  //    猜错探针会把「入参缺失 400」误判成失败（本项目踩过一次）。
  fixedasset: [
    { m: 'GET', p: '/erp/fixed-asset/asset/page', code: 'fixed-asset:asset:list' },
    { m: 'GET', p: '/erp/fixed-asset/category/list', code: 'fixed-asset:category:list' },
  ],
  stock: [
    { m: 'GET', p: '/erp/stock/in/page', code: 'stock:in:list' },
    { m: 'GET', p: '/erp/stock/take/page', code: 'stock:take:list' },
    { m: 'GET', p: '/erp/product-category/tree', code: 'product:category:list' },
    { m: 'GET', p: '/erp/md/image/page', code: 'md:image:list' },
  ],
  invoice: [
    { m: 'GET', p: '/erp/invoice/application/list', code: 'invoice:application:list' },
  ],
  payment: [
    { m: 'GET', p: '/erp/capital-flow/page', code: 'finance:capital-flow:list' },
    // 2026-09-21 域名归一：`/api/erp/payment`（ERP 付款单）原落到 `payment:*`，
    // 与历史「第三方支付网关」域同名不同物，已改名 `finance:payment:*`
    { m: 'GET', p: '/erp/payment/page', code: 'finance:payment:list' },
  ],
  party: [
    { m: 'GET', p: '/erp/contact/page', code: 'party:contact:list' },
    // 2026-09-21 域名归一：`/api/erp/partner/*` 原落到 `partner:*`，与 `/api/erp/party` 是
    // 同一业务对象被拆到两个域，已统一到 `party:*`
    { m: 'GET', p: '/erp/partner/roles/page', code: 'party:roles:list' },
  ],
  marketing: [
    { m: 'GET', p: '/erp/marketing/addon-rule/page', code: 'marketing:addon-rule:list' },
    { m: 'GET', p: '/erp/marketing/auto-campaign/page', code: 'marketing:auto-campaign:list' },
  ],
}

const probes = PROBES[moduleName]
if (!probes) {
  console.error(`未配置模块 ${moduleName} 的探测端点（在 PROBES 里补）`)
  process.exit(2)
}

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)
const warn = (t) => console.log(`  [WARN] ${t}`)

async function req(method, p, { token, tenantHeader, body } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(tenantHeader ? { 'X-Tenant-Id': String(tenantHeader) } : {}),
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
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    body: { username: user.u, password: user.p, tenantName: user.t, captcha: code, captchaKey: cap.json.data.uuid },
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`${user.u} 登录失败: ${res.text.slice(0, 300)}`)
  return token
}

;(async () => {
  console.log(`验证目标: ${BASE}  模块: ${moduleName}`)

  const admin = await login(ADMIN)
  const outsider = await login(OUTSIDER)
  console.log(`登录成功: ${ADMIN.u}（超管 / 租户 1）、${OUTSIDER.u}（非超管 / 租户 2）`)

  const adminFails = [], outsiderLeaks = [], unknownCodes = []

  section('① 超管放行（证明权限码真的在库里 —— 码不存在会让所有人 403）')
  for (const pr of probes) {
    const r = await req(pr.m, pr.p, { token: admin, tenantHeader: 1, body: pr.body })
    // 超管的判据不是「必须 200」：某些端点带业务前置校验（如必填入参），
    // 关键是**没被鉴权层挡下**（403）且不是服务端错误。
    const okStatus = r.status !== 403 && r.status < 500
    if (!okStatus) adminFails.push(`${pr.m} ${pr.p} → ${r.status} ${r.json?.message || ''}`)
    ok(`[${pr.code}] 超管未 403/5xx`, okStatus, `status=${r.status}`)
    if (r.status === 403 && /无权限访问/.test(r.json?.message || '')) {
      unknownCodes.push(`${pr.code}（${r.json.message}）`)
    }
  }

  section('② 非超管被拒（证明注解真的插到了方法上，而不是静默 fail-open）')
  for (const pr of probes) {
    const r = await req(pr.m, pr.p, { token: outsider, tenantHeader: 2, body: pr.body })
    const denied = r.status === 403
    if (!denied) outsiderLeaks.push(`${pr.m} ${pr.p} → ${r.status}`)
    ok(`[${pr.code}] 非超管 403`, denied, `status=${r.status} ${r.json?.message || ''}`)
  }

  section('③ 小结')
  if (unknownCodes.length) {
    warn(`以下码可能在库中缺失（超管被 403）：${unknownCodes.join(', ')}`)
  }
  if (adminFails.length) {
    warn(`超管侧异常：\n    - ${adminFails.join('\n    - ')}`)
  }
  if (outsiderLeaks.length) {
    console.log(`  ⚠️ 以下端点非超管**未被拒**（注解可能没插上）：\n    - ${outsiderLeaks.join('\n    - ')}`)
  }
  if (!adminFails.length && !outsiderLeaks.length) {
    console.log('  ✅ 两向断言均满足：码在库中（超管放行）+ 注解生效（非超管被拒）')
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => {
  console.error('\n验证脚本异常:', e.message)
  process.exit(2)
})
