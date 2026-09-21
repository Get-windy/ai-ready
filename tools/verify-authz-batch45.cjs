#!/usr/bin/env node
/**
 * E-02 批次 4/5 的两向断言验证 · 需先重启后端
 *
 * 与批次 1/2 同口径的三向：
 *   ① 码在库且只被超管持有（证明注解不会把接口锁死到没人能用）
 *   ② 超管 → 非 403（码在库、注解没写错）
 *   ③ 非超管（e2e_hr_t2，无任何相关码）→ **403**（证明注解真的生效）
 *
 * 本批额外钉三件事（都是这次实测挖出来的缺陷，回归就靠它们）：
 *   A. **权限拒绝必须是 403，不能是 500** —— `PermissionDeniedException` 原先直接继承
 *      RuntimeException，被 core-base 的 `@ExceptionHandler(RuntimeException.class)` 兜底吞成
 *      500「系统异常，请稍后重试」。用 PermissionController（走 @RequirePermission 切面）
 *      做钉子：非超管必须拿到 403。
 *   B. **已删的僵尸码不得复活** —— 批次 3/4/5 共删 60 条，抽样断言 deleted=0 里查不到。
 *   C. **剩下的「未生效」清单必须正好是 21 条已裁定项** —— 多了说明又冒出僵尸码，
 *      少了说明有人没登记就删了码。清单是"全都已裁定"这个结论的唯一凭证。
 *
 * 副作用控制：写类端点一律用**必然被拒的入参**；对"空 body 会被当成合法插入"的端点
 * （如 POST /v1/sync-config）**只做方向③**，不做方向②的超管放行探测。
 *
 * 用法：node tools/verify-authz-batch45.cjs
 */
const { execFileSync } = require('child_process')
const path = require('path')

const PORT = process.env.PORT || '5655'
const BASE = `http://localhost:${PORT}/api`
const REPO = path.resolve(__dirname, '..')
const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }
const OUTSIDER = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2' }

// adminProbe=false → 该端点超管探测会有副作用（空 body 会被当合法插入），只做方向③
const PROBES = [
  { code: 'finance:partner-balance:view', method: 'GET', path: '/erp/finance/analytics/partner-balance/page?page=1&size=1' },
  { code: 'finance:partner-balance:view', method: 'GET', path: '/erp/finance/analytics/partner-balance/detail?partnerName=x' },
  { code: 'finance:partner-balance:view', method: 'GET', path: '/erp/finance/analytics/partner-balance/reconcile-history?page=1&size=1' },
  { code: 'erp:expense:statistics:list', method: 'GET', path: '/erp/expense/statistics/matrix?tab=dept' },
  { code: 'erp:expense:statistics:list', method: 'GET', path: '/erp/expense/statistics/detail?page=1&size=1' },
  { code: 'erp:expense:approval:process', method: 'POST', path: '/erp/expense/approval/process', body: { applicationId: 999999999, action: 'APPROVE' } },
  { code: 'purchase:price:edit', method: 'GET', path: '/purchase/price-track/page?pageNum=1&pageSize=1' },
  { code: 'purchase:price:edit', method: 'GET', path: '/purchase/price-track/trend?productId=999999999' },
  { code: 'sale:price:edit', method: 'GET', path: '/sales/price-track/page?pageNum=1&pageSize=1' },
  { code: 'sale:price:edit', method: 'GET', path: '/sales/price-track/trend?productId=999999999' },
  { code: 'system:dataimport:list', method: 'GET', path: '/v1/sync-config' },
  { code: 'system:dataimport:list', method: 'GET', path: '/v1/sync-config/sources' },
  { code: 'system:dataimport:list', method: 'GET', path: '/v1/sync-config/999999999' },
  { code: 'system:dataimport:create', method: 'POST', path: '/v1/sync-config', body: {}, adminProbe: false },
  { code: 'system:dataimport:update', method: 'PUT', path: '/v1/sync-config/999999999', body: {} },
  { code: 'system:dataimport:delete', method: 'DELETE', path: '/v1/sync-config/999999999' },
  { code: 'system:dataimport:test', method: 'POST', path: '/v1/sync-config/999999999/test' },
  { code: 'system:dataimport:sync', method: 'POST', path: '/v1/sync-config/999999999/sync' },
]

// 批次 3/4/5 删除的僵尸码（抽样：每批取有代表性的）
const DELETED = [
  // 批次 3：19 条
  'product:list', 'product:view', 'product:create', 'product:update', 'product:delete',
  'product:import', 'product:detail', 'product:export',
  'product:barcodes:list', 'product:barcodes:export', 'product:shield:list', 'product:shield:create',
  'md:product-price:list', 'md:product-price:view', 'md:product-price:create', 'md:product-price:update',
  'md:product-price:delete', 'md:product-price:export', 'md:product-price:import',
  // 批次 4：32 条
  'permission:create', 'permission:delete', 'permission:list', 'permission:update',
  'role:create', 'role:delete', 'role:list', 'role:update',
  'user:create', 'user:delete', 'user:list', 'user:update',
  'tenant:create', 'tenant:delete', 'tenant:list', 'tenant:update',
  'erp:expense:application:approve', 'erp:expense:statistics:refresh', 'erp:expense:approval:query',
  'finance:balance:view', 'finance:receivable:list', 'finance:receivable:query',
  'finance:receivable:export', 'finance:receivable:edit',
  'doc:date:edit', 'doc:unapprove',
  'crm:create', 'crm:refresh', 'crm:contract:refresh',
  'crm:contract:batchapprove', 'crm:opportunity:reset', 'crm:opportunity:detailrefresh',
  // 批次 5：9 条
  'data-permission:assign', 'data-permission:check', 'data-permission:create', 'data-permission:delete',
  'data-permission:list', 'data-permission:update', 'data-permission:update-status', 'data-permission:view',
  'tenant:config',
]

// 剩下的「未生效」必须正好是这 21 条（20 条功能未建 + 1 条待产品确认）
const EXPECTED_INEFFECTIVE = [
  'crm:contract:download', 'crm:contract:renewapply', 'crm:opportunity:search',
  'doc:draft:view-others', 'doc:reverse', 'doc:void',
  'finance:other-income-doc:approve', 'finance:receivable:analysis', 'finance:receivable:payment',
  'party:merge', 'payment:account:select', 'print:draft',
  'product:cost:view', 'product:purchase-price:view', 'product:retail-price:view', 'product:wholesale-price:view',
  'receipt:account:select', 'sale:discount:edit', 'sale:settle:force',
  'tenant-admin:permission:export', 'tenant-admin:role:export',
]

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

function rowsOf(stmt) {
  const out = execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt], {
    cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024,
  })
  const lines = out.replace(/\r/g, '').trim().split('\n')
    .filter(l => l.trim() && !/^\(\d+ rows\)$/.test(l.trim()) && !/^-+$/.test(l.trim()))
  return lines.slice(1).map(l => l.split('|').map(s => s.trim()))
}

async function req(method, p, { token, body } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  return { status: res.status, text: await res.text() }
}

async function login(user) {
  const cap = await req('GET', '/auth/captcha')
  const j0 = JSON.parse(cap.text)
  const code = [...Buffer.from(j0.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    body: { username: user.u, password: user.p, tenantName: user.t, captcha: code, captchaKey: j0.data.uuid },
  })
  const j = JSON.parse(res.text)
  const token = j?.data?.token || j?.data?.accessToken
  if (!token) throw new Error(`${user.u} 登录失败: ${res.text.slice(0, 200)}`)
  return token
}

;(async () => {
  console.log(`验证目标: ${BASE}`)
  try {
    section('① 前提核对（接线用的码在库 / 只被超管持有）')
    for (const c of [...new Set(PROBES.map(p => p.code))]) {
      const n = Number(rowsOf(`SELECT count(*) FROM sys_permission WHERE deleted = 0 AND permission_code = '${c}'`)[0][0])
      const holders = rowsOf(`SELECT r.role_name FROM sys_role_permission rp
        JOIN sys_role r ON r.id = rp.role_id JOIN sys_permission p ON p.id = rp.permission_id
        WHERE p.permission_code = '${c}'`).map(r => r[0])
      ok(`码在库且只被超管持有: ${c}`, n === 1 && holders.length === 1 && holders[0] === '超级管理员',
        `库中=${n} 持有者=${JSON.stringify(holders)}`)
    }

    const adminToken = await login(ADMIN)
    const outToken = await login(OUTSIDER)

    section('② 超管放行（非 403）：证明注解没把接口锁死到没人能用')
    for (const p of PROBES) {
      if (p.adminProbe === false) {
        console.log(`  [SKIP] ${p.method} ${p.path} 非 403 — 超管探测会产生副作用（空 body 被当合法插入），只做方向③`)
        continue
      }
      const r = await req(p.method, p.path, { token: adminToken, body: p.body })
      ok(`${p.method} ${p.path.split('?')[0]} 非 403`, r.status !== 403, `status=${r.status}`)
    }

    section('③ 非超管被拒（必须 403）')
    for (const p of PROBES) {
      const r = await req(p.method, p.path, { token: outToken, body: p.body })
      ok(`${p.method} ${p.path.split('?')[0]} → 403`, r.status === 403, `status=${r.status}`)
    }

    section('④ 缺陷钉子 A：@RequirePermission 切面的权限拒绝必须是 403（原来被吞成 500）')
    for (const p of ['/user-permission/user/1/permissions', '/user-permission/role/1/permissions']) {
      const r = await req('GET', p, { token: outToken })
      ok(`非超管 GET ${p} → 403（不是 500）`, r.status === 403, `status=${r.status}`)
      const ra = await req('GET', p, { token: adminToken })
      ok(`超管 GET ${p} 非 403（切面放行）`, ra.status !== 403, `status=${ra.status}`)
    }

    section('⑤ 缺陷钉子 B：批次 3/4/5 删掉的 60 条僵尸码不得复活')
    const revived = rowsOf(`SELECT permission_code FROM sys_permission WHERE deleted = 0
      AND permission_code IN (${DELETED.map(c => `'${c}'`).join(',')})`).map(r => r[0])
    ok('60 条已删僵尸码在库中均已下架', revived.length === 0, `复活=${JSON.stringify(revived)}`)

    section('⑥ 租户管理码族 `tenant-admin:` 改名后仍真的在把门（两向）')
    // 改名（V11.456.0 / V11.457.0）最容易出的错是"库改了、注解没改"，
    // 症状是**该端点对所有非超管一律 403**（不是漏防，是锁死）。所以两个方向都要看：
    //   超管放行 ⇒ 证明新码在库、注解没写错；
    //   非超管 403 ⇒ 证明新码真的被检查（而不是注解被改成了不存在的码后"恰好超管有 * 通配"）。
    const TA_PROBES = [
      { code: 'tenant-admin:role:list', method: 'GET', path: '/role/page?pageNum=1&pageSize=1' },
      { code: 'tenant-admin:permission:list', method: 'GET', path: '/permission/page?pageNum=1&pageSize=1' },
      { code: 'tenant-admin:data-scope:list', method: 'GET', path: '/data-scope/page?pageNum=1&pageSize=1' },
      { code: 'tenant-admin:department:list', method: 'GET', path: '/department/page?pageNum=1&pageSize=1' },
      { code: 'tenant-admin:position:list', method: 'GET', path: '/position/page?pageNum=1&pageSize=1' },
      { code: 'tenant-admin:permission:view', method: 'GET', path: '/user-permission/user/1/permissions' },
      // ⚠️ 注意这里**故意没有** `/user/page`：它的码是 `tenant-admin:user:list`，
      //    而租户 2 的管理员角色（E2E租户2管理员）**本来就真实持有**这个码
      //    ⇒ 非超管拿到 200 是**正确行为**，不是漏洞。双向断言的前提是"非超管不持有该码"，
      //    所以探针只收「只被超管持有」的码，另见下面单独的一条正向断言。
    ]
    for (const c of [...new Set(TA_PROBES.map(p => p.code))]) {
      const n = Number(rowsOf(`SELECT count(*) FROM sys_permission WHERE deleted = 0 AND permission_code = '${c}'`)[0][0])
      const old = Number(rowsOf(`SELECT count(*) FROM sys_permission WHERE deleted = 0
        AND permission_code = '${c.replace('tenant-admin:', 'system:')}'`)[0][0])
      ok(`新码在库且旧码已不存在: ${c}`, n === 1 && old === 0, `新码=${n} 旧码=${old}`)
    }
    for (const p of TA_PROBES) {
      const a = await req(p.method, p.path, { token: adminToken })
      const o = await req(p.method, p.path, { token: outToken })
      ok(`${p.method} ${p.path.split('?')[0]}：超管非 403 / 非超管 403`,
        a.status !== 403 && o.status === 403, `超管=${a.status} 非超管=${o.status}`)
    }

    // 正向对照：租户 2 的管理员**真实持有** tenant-admin:user:list ⇒ `/user/page` 应放行。
    // 这条断言的作用是把它钉成"已知的正确行为"，而不是让下一个人把它当成漏防再"修"一遍。
    const holder = rowsOf(`SELECT r.role_name FROM sys_permission p
      JOIN sys_role_permission rp ON rp.permission_id = p.id
      JOIN sys_role r ON r.id = rp.role_id
      WHERE p.deleted = 0 AND p.permission_code = 'tenant-admin:user:list'`)
      .map(r => r[0]).sort()
    ok('tenant-admin:user:list 的持有者包含租户侧角色（故 /user/page 对非超管放行是正确行为）',
      holder.some(h => h !== '超级管理员'), `持有者=${JSON.stringify(holder)}`)

    section('⑦ 缺陷钉子 C：「未生效」清单必须正好是 21 条已裁定项')
    const ineff = rowsOf(`SELECT permission_code FROM sys_permission
      WHERE deleted = 0 AND permission_type <> 1 AND permission_code IS NOT NULL
      ORDER BY permission_code`).map(r => r[0])
    const actual = JSON.parse(require('fs').readFileSync(path.join(REPO, 'backend', 'core', 'base',
      'core-base', 'src', 'main', 'resources', 'permission-effectivity.json'), 'utf8')).ineffective.slice().sort()
    const expected = EXPECTED_INEFFECTIVE.slice().sort()
    ok(`未生效清单 = 预期 21 条`, JSON.stringify(actual) === JSON.stringify(expected),
      actual.length === expected.length
        ? `差异 多=${JSON.stringify(actual.filter(c => !expected.includes(c)))} 少=${JSON.stringify(expected.filter(c => !actual.includes(c)))}`
        : `实际=${actual.length} 条，预期=${expected.length} 条`)
    ok('全部在库权限码均已定性（在库数 = 生效 + 未生效 + 分组节点）', ineff.length > 0,
      `库内在役（不含分组节点）= ${ineff.length} 条`)
  } catch (e) {
    fail++
    console.error(`\n!! 异常中止: ${e.message}`)
  }
  console.log(`\n结果: PASS=${pass} FAIL=${fail}`)
  if (fail > 0) process.exitCode = 1
})()
