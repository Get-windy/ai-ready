#!/usr/bin/env node
/**
 * CRM-BREAK-02 专项验证（2026-09-21）
 *
 * 覆盖三件事，**拒绝路径与放行路径都测**：
 *   ① 新建单据不再硬编码 `tenant_id = 1` —— 用**租户 2 的账号**建合同，
 *      落库租户必须是 2（修复前恒为 1，即"租户 2 建的合同出现在租户 1"）。
 *   ② `RuntimeException` → `BusinessException.notFound` —— 查不存在的单据应是
 *      **404「合同不存在」**，而不是 500「系统异常，请稍后重试」。
 *   ③ E-01 crm 批次的注解生效性：本账号有 `crm:contract:create/view`（① 能过）、
 *      未授 `crm:contract:approve`（必须 403）—— 同一控制器上双向都成立。
 *
 * <p><b>前置（本脚本依赖，属测试夹具不在迁移里）</b>：E2E 账号 `e2e_hr_t2` 的角色
 * `E2E_T2_ADMIN` 需持有 `crm:contract:create` 与 `crm:contract:view`。
 * E-01 补注解后这两个码若不授予，本脚本 ① 会变 403 —— 那是**预期行为**
 * （补注解后租户角色必须被显式授权），不是脚本坏了。</p>
 *
 * 用法：
 *   node tools/verify-crm-tenant-fix.cjs
 *   PORT=5655 node tools/verify-crm-tenant-fix.cjs
 *
 * ⚠️ 会**写一条合同**（这是"落库租户"唯一可信的观测方式），结束时**硬删除**该探针行。
 *
 * <p><b>为什么必须硬删、不能只软删（2026-09-21 实踩）</b>：`uk_crm_contract_no` 是
 * `UNIQUE(contract_no)`，**不含 deleted** —— 软删行仍占着那个合同号；而
 * `generateContractNo()` 统计的是 `deleted = 0` 的行 ⇒ 把当天最后一张软删掉之后，
 * 下一次生成又会拿到同一个号，插入直接撞唯一索引报 400「请求数据不完整或存在冲突」。
 * 这正是 `MyBatisPlusConfig` 里 `sys_tenant_menu` 那条注释记的同一类坑，
 * 已另立 CRM-BREAK-03 跟踪（属产品代码缺陷，不是脚本问题）。</p>
 */
const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')
const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`

/** 硬删除探针行，保证脚本可重复执行（见文件头说明） */
function deleteProbe(id) {
  try {
    execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'),
      `DELETE FROM crm_contract WHERE id = ${id}`], { cwd: REPO, encoding: 'utf8' })
    console.log(`\n已硬删除探针合同 id=${id}（表恢复原状，脚本可重复执行）`)
  } catch (e) {
    console.error(`\n!! 探针合同 id=${id} 删除失败，请手工执行：DELETE FROM crm_contract WHERE id = ${id}`)
  }
}

// 租户 2 的非超管账号：修复前它建的合同会落进租户 1
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
  const t2Token = await login(TENANT2)
  console.log(`登录成功: ${TENANT2.u}（非超管 / 租户 2）`)

  let probeId = null
  try {
    // ══ ① 新建落库租户 = 会话租户（不是常量 1） ══
    section('① 租户 2 建合同 → 落库租户应为 2（CRM-BREAK-02 硬编码租户）')
    const stamp = new Date().toISOString().replace(/[-:T.]/g, '').slice(0, 14)
    const created = await req('POST', '/crm/contract', {
      token: t2Token,
      body: {
        contractName: `B2 租户落库探针 ${stamp}`,
        contractType: 1,
        customerName: '探针客户',
        contractAmount: 1234.56,
        currency: 'CNY',
        signDate: '2026-09-21',
        startDate: '2026-09-21',
        endDate: '2027-09-20',
      },
    })
    probeId = created.json?.data?.id ?? created.json?.id ?? null
    const id = probeId
    ok('【放行路径】租户 2 建合同 → 非 4xx/5xx', created.status === 200 && !!id,
      `status=${created.status} id=${id} ${created.json?.message || ''}`)
    if (!id) {
      console.log(`  原始响应: ${created.text.slice(0, 300)}`)
      throw new Error('未取得探针合同 id，后续断言跳过')
    }

    // 【决定断言】用**同一个租户 2 会话**回读列表。
    // 为什么这一条就够判定：列表查询会被租户拦截器注入 `AND tenant_id = 2`，
    //   · 修复前：该行落库 tenant_id = 1 ⇒ 本租户会话**查不到**（数据"消失"在租户 1）；
    //   · 修复后：落库 tenant_id = 2 ⇒ 查得到。
    // 所以"查得到"等价于"落库租户正确"，不依赖响应体是否回显 tenantId（ContractVO 不回显）。
    const page = await req('GET', '/crm/contract/page?pageNum=1&pageSize=200', { token: t2Token })
    const found = (page.json?.data?.records || page.json?.records || [])
      .some(r => String(r.id) === String(id))
    ok('【决定断言】租户 2 的会话能回读到该合同（= 落库租户为 2，不是常量 1）',
      page.status === 200 && found, `status=${page.status} found=${found}`)

    // ══ ② 异常口径：不存在 → 404，不是 500 ══
    section('② 查不存在的合同 → 404「合同不存在」（不是 500「系统异常」）')
    const missing = await req('GET', '/crm/contract/99999999999999', { token: t2Token })
    ok('【拒绝路径】不存在 → HTTP 404', missing.status === 404, `status=${missing.status}`)
    ok('【拒绝路径】文案是业务文案而非兜底文案',
      /合同不存在/.test(missing.json?.message || '') && !/系统异常/.test(missing.json?.message || ''),
      `message=${missing.json?.message || ''}`)

    // ══ ③ 注解生效的双向证据（E-01 crm 批次，2026-09-21）══
    // 本账号有 create/view 两个契约码（放行，见 ①②），但没有 approve
    // ⇒ 同一个控制器上「有码放行 / 无码被拒」都成立，才说明注解真的长在方法上。
    section('③ E-01 注解生效：本账号有 create/view、没有 approve')
    const approve = await req('POST',
      `/crm/contract/${id}/approve?note=probe`, { token: t2Token })
    ok('【拒绝路径】未授的 crm:contract:approve → 403',
      approve.status === 403, `status=${approve.status} ${approve.json?.message || ''}`)
    ok('【拒绝路径】是权限拒绝文案（不是 500/404 之类被业务逻辑挡下）',
      /无权限访问/.test(approve.json?.message || ''), `message=${approve.json?.message || ''}`)
  } finally {
    if (probeId) {
      deleteProbe(probeId)
    }
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('脚本异常:', e.message); process.exit(2) })
