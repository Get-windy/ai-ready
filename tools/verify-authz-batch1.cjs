#!/usr/bin/env node
/**
 * E-02 批次 1（erp:expense:*）的**两向断言**验证 · 需先重启后端
 *
 * 只测「超管能过」会漏掉最要命的一种：**码根本不在库** —— 那种情况下所有非超管一律 403
 * （超管走 `*` 通配永远能过，看起来一切正常）。所以每条都必须**两向**：
 *   · 超管 → 非 403（证明码在库、注解没把接口锁死到没人能用）
 *   · 非超管（e2e_hr_t2，租户 2，无任何 expense 码）→ **403**（证明注解真的生效）
 *
 * 写类端点（POST）用**必然被拒的入参**（空 body）去打：Spring 的鉴权在参数校验之前，
 * 所以拿到 400 就说明"已过鉴权"，而拿 403 就说明被挡下 —— 不产生任何副作用。
 *
 * 用法：node tools/verify-authz-batch1.cjs
 */
const { execFileSync } = require('child_process')
const path = require('path')

const PORT = process.env.PORT || '5655'
const BASE = `http://localhost:${PORT}/api`
const REPO = path.resolve(__dirname, '..')
const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }
const OUTSIDER = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2' }

// 探针：{ code, method, path, note }
const PROBES = [
  { code: 'erp:expense:payment:list', method: 'GET', path: '/erp/expense/payment/page?page=1&size=1' },
  { code: 'erp:expense:payment:query', method: 'GET', path: '/erp/expense/payment/999999999' },
  { code: 'erp:expense:payment:create', method: 'POST', path: '/erp/expense/payment', body: {} },
  { code: 'erp:expense:payment:confirm', method: 'POST', path: '/erp/expense/payment/999999999/confirm', body: {} },
  { code: 'erp:expense:payment:cancel', method: 'POST', path: '/erp/expense/payment/999999999/cancel', body: {} },
  { code: 'erp:expense:application:list', method: 'GET', path: '/erp/expense/application/page?page=1&size=1' },
  { code: 'erp:expense:application:query', method: 'GET', path: '/erp/expense/application/999999999' },
  { code: 'erp:expense:statistics:list', method: 'GET', path: '/erp/expense/statistics/page?page=1&size=1' },
  { code: 'erp:expense:approval:list', method: 'GET', path: '/erp/expense/approval/records' },
  { code: 'erp:expense:approval:list', method: 'GET', path: '/erp/expense/approval/pending' },
  { code: 'erp:expense:reimbursement:query', method: 'GET', path: '/erp/expense/reimbursement/999999999' },
]

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

function sql(stmt) {
  return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt], {
    cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024,
  })
}
function rowsOf(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').trim().split('\n')
    .filter(l => l.trim() && !/^\(\d+ rows\)$/.test(l.trim()) && !/^-+$/.test(l.trim()))
  return lines.slice(1).map(l => l.split('|').map(s => s.trim()))
}

async function req(method, p, { token, body } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  return { status: res.status, text }
}

async function login(user) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(JSON.parse(cap.text).data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    body: { username: user.u, password: user.p, tenantName: user.t, captcha: code, captchaKey: JSON.parse(cap.text).data.uuid },
  })
  const j = JSON.parse(res.text)
  const token = j?.data?.token || j?.data?.accessToken
  if (!token) throw new Error(`${user.u} 登录失败: ${res.text.slice(0, 200)}`)
  return token
}

;(async () => {
  console.log(`验证目标: ${BASE}`)
  try {
    // ── 前提：11 个码都必须在库，且只有超管持有 ──
    section('① 前提核对（码在库 / 只被超管持有）')
    const codes = [...new Set(PROBES.map(p => p.code))]
    for (const c of codes) {
      const n = Number(rowsOf(`SELECT count(*) FROM sys_permission WHERE permission_code = '${c}'`)[0][0])
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
      const r = await req(p.method, p.path, { token: adminToken, body: p.body })
      ok(`${p.method} ${p.path.split('?')[0]} 非 403`, r.status !== 403, `status=${r.status}`)
    }

    section('③ 非超管被拒（必须 403）：证明注解真的生效')
    for (const p of PROBES) {
      const r = await req(p.method, p.path, { token: outToken, body: p.body })
      ok(`${p.method} ${p.path.split('?')[0]} → 403`, r.status === 403, `status=${r.status}`)
    }

  } catch (e) {
    fail++
    console.error(`\n!! 异常中止: ${e.message}`)
  }
  console.log(`\n结果: PASS=${pass} FAIL=${fail}`)
  if (fail > 0) process.exitCode = 1
})()
