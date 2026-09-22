#!/usr/bin/env node
/**
 * 租户模块「开通 / 停用」写端点 · 实机验证（平台-MODULE-01 遗留①，2026-09-22）
 *
 * 要钉死的是**这道 entitlement 门从"只能拦"变成"能开能关"**：
 *   ① 超管给租户开通模块 → 该租户用户打该模块下的接口，从「403 模块未开通」
 *      变成「不是模块门拦的」（200，或 403 但文案不含「模块未开通」）；
 *   ② 停用后 → 同一人、同一接口 **403 且文案为「模块未开通」**；
 *   ③ 再次开通 → 回到 ① 的状态（停用不是单向的）；
 *   ④ 探针数据自清理，且清理后与原状逐字段一致。
 *
 * 外加四条**只有真库才能证明**的断言（都读 SQL，不采信接口回包）：
 *   · 幂等：同一模块连续开通两次，`sys_tenant_module` 里该 (tenant, module) **只有 1 行**；
 *   · 复活墓碑：停用（deleted=1）后再开通，**行数仍为 1 且主键不变** ——
 *     这条正对 `assignModule` 的原缺陷：BaseMapper 受 @TableLogic 限制看不见墓碑行，
 *     会 INSERT 第二行；而本表实测**没有 (tenant_id, module_code) 唯一索引**，插了也不报错。
 *   · 业务约束：`remove(tenantId=1, moduleCode='system')` 被拒且给中文提示，
 *     拒绝后库里的「系统」模块仍为开通（V11.455.0 的不变式不许被本接口破坏）；
 *   · 越权：租户用户 token 调 assign/remove → 403（不是靠租户隔离豁免，见 assertPlatformAdmin）。
 *
 * 探针选择：租户 2 的 `e2e_hr_t2` 持有 `tenant-admin:user:list`
 * （前缀 `tenant-admin:` 归属模块 `settings`）⇒ 用 `GET /user/page` 当探针。
 * 之所以能观察到「模块门」的文案，是因为该用户**有权限码**，权限检查会放行、
 * 只剩模块门说话（对比 `verify-module-entitlement.cjs` 的②：无码时会先报「无权限访问」）。
 *
 * ⚠️ **会写 `sys_tenant_module`（settings × 租户 2）**：先存原值，结束时按原值还原并核对。
 * ⚠️ **本脚本不 sleep**：`ModuleEntitlementService` 的租户开通集合有 30s 进程内缓存，
 *    而写端点成功后调了 `evictTenant`。故「改完立刻生效」本身就是要验证的行为
 *    （`verify-module-entitlement.cjs` 需要 31s×2 的等待，那是直接改库、没有 evict）。
 *    若某次运行在②/③失败且重试后才通过，说明 evict 没生效。
 *
 * 用法：node tools/verify-module-assign.cjs
 *   前置：后端在 5655 端口运行、`V11.485.0` 迁移已执行（两个权限码已入库并授超管）。
 *   退出码：0 = 全绿；1 = 有 FAIL；2 = 脚本异常（如登录失败）。
 *
 * 未被本脚本验证的部分（留给人工集成验证）：
 *   · 前端 `views/admin/tenant/module-auth` 页面是否已接上这两个端点（前端不在本次改动范围）；
 *   · `purchaseType=auto_renew/manual` 与 `expireTime` 的非空业务语义（只验证了传参与落库，
 *     未验证到期后是否真的被 `getValidModuleCodesStrict` 过滤 —— 那需要改系统时间或插过期行）；
 *   · 「开通/停用后自动派生/回收该租户的授权」—— 原 MASTER_TODO 提到的后续能力，本次未实现。
 */
const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')
const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`

const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }
const TENANT2 = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2' }

const T2 = 2
const T1 = 1
const MODULE = 'settings'
/** 探针接口：@SaCheckPermission("tenant-admin:user:list") ⇒ 归属模块 settings（前缀 tenant-admin:） */
const PROBE = '/user/page'
const ASSIGN_PATH = '/tenant-module/assign'
const REMOVE_PATH = `/tenant-module/remove?tenantId=${T2}&moduleCode=${MODULE}`

const CODE_ASSIGN = 'system:tenant-module:assign'
const CODE_REMOVE = 'system:tenant-module:remove'

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

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

/** 探针行的原始现场（用于还原与逐字段核对） */
const readProbeRow = () => {
  const out = scalar(`SELECT id || '|' || deleted || '|' || status || '|' || purchase_type
      || '|' || COALESCE(expire_time::text, 'NULL')
    FROM sys_tenant_module WHERE tenant_id = ${T2} AND module_code = '${MODULE}'`)
  if (out === '(空)') return null
  const [id, deleted, status, purchaseType, expireTime] = out.split('|')
  return { id, deleted, status, purchaseType, expireTime }
}

/** 该 (tenant, module) 在库里的**总行数（含墓碑）**：幂等/复活断言的依据 */
const probeRowCount = () => Number(scalar(
  `SELECT count(*) FROM sys_tenant_module WHERE tenant_id = ${T2} AND module_code = '${MODULE}'`))

const probeDeleted = () => scalar(
  `SELECT deleted FROM sys_tenant_module WHERE tenant_id = ${T2} AND module_code = '${MODULE}'`)

const systemRowOfT1 = () => scalar(
  `SELECT deleted || '|' || status FROM sys_tenant_module
    WHERE tenant_id = ${T1} AND module_code = 'system'`)

/** 用 SQL 把探针行按原值硬还原（接口不可用时的兜底，保证不留脏数据） */
function forceRestore(row) {
  if (!row) {
    sql(`DELETE FROM sys_tenant_module WHERE tenant_id = ${T2} AND module_code = '${MODULE}'`)
    return
  }
  sql(`UPDATE sys_tenant_module SET deleted = ${row.deleted}, status = ${row.status},
        purchase_type = ${row.purchaseType === 'NULL' ? 'NULL' : `'${row.purchaseType}'`},
        expire_time = ${row.expireTime === 'NULL' ? 'NULL' : `'${row.expireTime}'`},
        update_time = now()
       WHERE id = ${row.id}`)
}

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

/** 开通某模块（返回接口回包），purchaseType 固定 permanent 以便与原值一致 */
const apiAssign = (token, tenantId, moduleCode, extra = {}) =>
  req('POST', ASSIGN_PATH, { token, body: { tenantId, moduleCode, purchaseType: 'permanent', ...extra } })

const apiRemove = (token, tenantId, moduleCode) =>
  req('DELETE', `/tenant-module/remove?tenantId=${tenantId}&moduleCode=${moduleCode}`, { token })

const msgOf = (r) => r.json?.message || ''

;(async () => {
  console.log(`验证目标: ${BASE}`)

  const original = readProbeRow()
  if (!original) {
    throw new Error(`前置不成立：租户 ${T2} 的「${MODULE}」开通记录不存在（本脚本的还原依赖它）`)
  }
  console.log(`前置：租户 ${T2}「${MODULE}」原始行 = ${JSON.stringify(original)}`)
  const originalSystemT1 = systemRowOfT1()
  console.log(`前置：租户 ${T1}「system」原始行 = ${originalSystemT1}`)

  // 前置 0：权限码必须在库且已授超管（铁律；不成立则后面必然 403，先给明确诊断）
  section('⓪ 前置：权限码已入库并授予超管角色')
  const codeCount = Number(scalar(
    `SELECT count(*) FROM sys_permission WHERE deleted = 0 AND permission_code IN ('${CODE_ASSIGN}', '${CODE_REMOVE}')`))
  const grantCount = Number(scalar(
    `SELECT count(*) FROM sys_role_permission rp JOIN sys_permission p ON p.id = rp.permission_id
      WHERE rp.role_id = 1 AND p.permission_code IN ('${CODE_ASSIGN}', '${CODE_REMOVE}')`))
  ok('两个新权限码已在 sys_permission', codeCount === 2, `count=${codeCount}`)
  ok('两个新权限码已授予超管角色(role_id=1)', grantCount === 2, `count=${grantCount}`)

  const adminToken = await login(ADMIN)
  const t2Token = await login(TENANT2)
  console.log(`登录成功: admin（超管） / ${TENANT2.u}（租户 ${T2}）`)

  // 现场是否干净由 finally 保证；这里先记录本次是否发生过写操作
  let mutated = false
  try {
    // 保证起点为「已开通」，否则基线断言无从谈起
    if (probeDeleted() !== '0') {
      const r = await apiAssign(adminToken, T2, MODULE)
      ok('前置修复：探针模块此前处于停用态，已自动开通', r.status === 200, `status=${r.status} ${msgOf(r)}`)
    }

    // ══ ① 放行基线 + 停用后立即拦截 ══
    section('① 停用 → 立即（不等待缓存过期）403「模块未开通」')
    const base = await req('GET', PROBE, { token: t2Token })
    ok('【基线】停用前，租户 2 用户打探针接口 → 200', base.status === 200,
      `status=${base.status} ${msgOf(base)}`)

    const rm1 = await apiRemove(adminToken, T2, MODULE)
    mutated = true
    ok('【停用】DELETE /remove → 调用成功', rm1.status === 200, `status=${rm1.status} ${msgOf(rm1)}`)
    ok('【停用】库里该行被软删（deleted=1），且**总行数仍为 1**（无重复行）',
      probeDeleted() === '1' && probeRowCount() === 1,
      `deleted=${probeDeleted()}, rows=${probeRowCount()}`)

    const closed = await req('GET', PROBE, { token: t2Token })
    const msgClosed = msgOf(closed)
    ok('【停用 · 立即生效】同一人同一接口 → 403', closed.status === 403, `status=${closed.status}`)
    ok('【停用 · 立即生效】文案是「模块未开通」（无 sleep，证明 evictTenant 生效）',
      /模块未开通/.test(msgClosed), `message=${msgClosed}`)

    // ══ ② 开通 → 门立刻打开（复活墓碑行，不是插第二行） ══
    section('② 开通 → 立即放行；且是**复活墓碑行**而非 INSERT 第二行')
    const as1 = await apiAssign(adminToken, T2, MODULE)
    ok('【开通】POST /assign → 调用成功', as1.status === 200, `status=${as1.status} ${msgOf(as1)}`)

    const rowAfterAssign = readProbeRow()
    ok('【开通】总行数仍为 1（若是盲插，此处会是 2 —— 本表无唯一索引，不会报错）',
      probeRowCount() === 1, `rows=${probeRowCount()}`)
    ok('【开通】复用的是原主键（墓碑被复活，未新建行）',
      rowAfterAssign !== null && rowAfterAssign.id === original.id,
      `id=${rowAfterAssign?.id} 原 id=${original.id}`)
    ok('【开通】行已恢复有效（deleted=0, status=0）',
      rowAfterAssign?.deleted === '0' && rowAfterAssign?.status === '0',
      `deleted=${rowAfterAssign?.deleted}, status=${rowAfterAssign?.status}`)

    const reopened = await req('GET', PROBE, { token: t2Token })
    ok('【开通 · 立即生效】门已打开：不再被模块门拦（200 或 403 但文案不再是「模块未开通」）',
      !/模块未开通/.test(msgOf(reopened)),
      `status=${reopened.status} message=${msgOf(reopened)}`)

    // ══ ③ 幂等 + 可重复：再走一轮 停用/开通 ══
    section('③ 可重复：再走一轮「停用 → 开通」，行数与主键都不变')
    const rm2 = await apiRemove(adminToken, T2, MODULE)
    ok('【重复】第二次停用成功', rm2.status === 200, `status=${rm2.status} ${msgOf(rm2)}`)
    const closed2 = await req('GET', PROBE, { token: t2Token })
    ok('【重复】第二次停用后 → 403「模块未开通」',
      closed2.status === 403 && /模块未开通/.test(msgOf(closed2)),
      `status=${closed2.status} message=${msgOf(closed2)}`)

    const as2 = await apiAssign(adminToken, T2, MODULE)
    ok('【重复】第三次开通成功', as2.status === 200, `status=${as2.status} ${msgOf(as2)}`)
    const rowAfter3 = readProbeRow()
    ok('【幂等】累计 3 次开通 / 2 次停用后，仍然是 1 行、同一主键',
      probeRowCount() === 1 && rowAfter3?.id === original.id,
      `rows=${probeRowCount()}, id=${rowAfter3?.id}`)
    const reopened2 = await req('GET', PROBE, { token: t2Token })
    ok('【重复】再次开通后门又打开', !/模块未开通/.test(msgOf(reopened2)),
      `status=${reopened2.status} message=${msgOf(reopened2)}`)

    // ══ ④ 业务约束与越权（不改变探针现场） ══
    section('④ 业务约束与越权：拒绝路径都必须给明确中文提示')
    const sysRemove = await apiRemove(adminToken, T1, 'system')
    ok('【约束】停用租户 1 的「system」模块 → 被拒绝（400）',
      sysRemove.status === 400, `status=${sysRemove.status} ${msgOf(sysRemove)}`)
    ok('【约束】拒绝文案是中文且说明原因',
      /系统.*模块|平台级/.test(msgOf(sysRemove)), `message=${msgOf(sysRemove)}`)
    ok('【约束】拒绝后库里的「system」仍为开通（不变式未被破坏）',
      systemRowOfT1() === originalSystemT1, `now=${systemRowOfT1()} 原=${originalSystemT1}`)

    const sysAssign = await apiAssign(adminToken, T2, 'system')
    ok('【约束】把「system」开给租户 2 → 被拒绝（400）', sysAssign.status === 400,
      `status=${sysAssign.status} ${msgOf(sysAssign)}`)
    ok('【约束】拒绝后租户 2 仍未开通「system」',
      probeRowCount() === 1 && scalar(
        `SELECT count(*) FROM sys_tenant_module WHERE tenant_id = ${T2} AND module_code = 'system' AND deleted = 0`) === '0')

    const junkAssign = await apiAssign(adminToken, T2, 'no-such-module')
    ok('【约束】开通目录里不存在的模块码 → 被拒绝（400）', junkAssign.status === 400,
      `status=${junkAssign.status} ${msgOf(junkAssign)}`)
    ok('【约束】拒绝文案指明模块码不存在',
      /不存在/.test(msgOf(junkAssign)), `message=${msgOf(junkAssign)}`)

    const junkRemove = await apiRemove(adminToken, T2, 'no-such-module')
    ok('【约束】停用不存在的模块码 → 被拒绝（400）', junkRemove.status === 400,
      `status=${junkRemove.status} ${msgOf(junkRemove)}`)

    const t2Assign = await apiAssign(t2Token, T2, MODULE)
    ok('【越权】租户用户调 assign → 403（平台管理员硬校验）', t2Assign.status === 403,
      `status=${t2Assign.status} ${msgOf(t2Assign)}`)
    const t2Remove = await apiRemove(t2Token, T1, 'settings')
    ok('【越权】租户用户调 remove（且指向别的租户）→ 403', t2Remove.status === 403,
      `status=${t2Remove.status} ${msgOf(t2Remove)}`)
    ok('【越权】被拒后未产生任何写效果（探针行仍有效）', probeDeleted() === '0', `deleted=${probeDeleted()}`)
  } finally {
    console.log('\n—— 现场还原 ——')
    // 优先用接口还原（顺带把进程内缓存也刷对）；失败则以 SQL 硬还原兜底
    try {
      if (adminToken) {
        const r = original.deleted === '0'
          ? await apiAssign(adminToken, T2, MODULE)
          : await apiRemove(adminToken, T2, MODULE)
        console.log(`  接口还原（deleted 目标=${original.deleted}）: status=${r.status} ${msgOf(r)}`)
      }
    } catch (e) {
      console.log(`  接口还原失败：${e.message}`)
    }
    const now = readProbeRow()
    if (!now || now.deleted !== original.deleted || now.status !== original.status
        || now.purchaseType !== original.purchaseType || now.expireTime !== original.expireTime) {
      console.log('  接口还原后与原值不一致，改用 SQL 硬还原')
      forceRestore(original)
    }
    const after = readProbeRow()
    console.log(`  还原核对：${JSON.stringify(after)}（应为 ${JSON.stringify(original)}）`)
    ok('【自清理】探针行逐字段还原为原值', after !== null
      && after.id === original.id && after.deleted === original.deleted
      && after.status === original.status && after.purchaseType === original.purchaseType
      && after.expireTime === original.expireTime)
    ok('【自清理】探针行无重复（含墓碑仍为 1 行）', probeRowCount() === 1, `rows=${probeRowCount()}`)
    ok('【自清理】租户 1 的「system」未被本次运行改动', systemRowOfT1() === originalSystemT1,
      `now=${systemRowOfT1()} 原=${originalSystemT1}`)
    if (mutated) console.log('  （本次运行发生过写操作，已全部还原）')
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('脚本异常:', e.message); process.exit(2) })
