#!/usr/bin/env node
/**
 * 菜单可见性「从 sys_permission 派生」· 实机验证（平台-AUTHZ-01，2026-09-22）
 *
 * 背景：`sys_role_menu` 全库只有 3 行（只属 SUPER_ADMIN），`SysMenuServiceImpl.getUserMegaMenus()`
 * 对普通租户走「sys_tenant_menu ∩ sys_role_menu」⇒ 交集为空 ⇒ **除超管外所有角色登录后菜单接口返回空数组**。
 * 用户 2026-09-21 拍板：菜单可见性改由 `sys_permission` 派生（前缀规则），不再双维护 `sys_role_menu`。
 *
 * 用法：
 *   node tools/verify-menu-derivation.cjs
 *   环境变量：PORT（默认 5655）、BASE（默认 http://localhost:${PORT}/api）
 *
 * 前置：必须**重启后**（新实例）运行；跑在改动前的旧实例上会在 ① 报空数组并给出提示。
 *
 * 断言清单（每条都在证伪一件具体的事）：
 *   ① 派生命中：非超管能看到与其权限码前缀对应的菜单。
 *      证伪「改造后仍返回空数组」（这是本任务要修的现象）与「派生把有码菜单也杀了」。
 *   ② 过渡口径：`menu_code` 在权限码库里找不到任何对应权限码的菜单**仍然可见**。
 *      证伪「把『无权限码』误当成『无权限』而隐藏」——那会让导航比改动前缩水（硬性口径）。
 *   ③ 有码但未持有 ⇒ 隐藏：用 4 个权限码的 E2E_T2_ADMIN 与 18 个码的 SYSTEM_ADMIN 对比。
 *      证伪「全放开」实现（不做权限过滤时这 48 个菜单会原样出现），并与超管可见性对照，
 *      排除「接口本来就不返回这些菜单」这一替代解释。
 *   ④ 超管不受影响：admin（系统租户 + SUPER_ADMIN）走原有早退分支，数量与关键项与改动前一致。
 *   ⑤ 无副作用 / 可重复跑：全程只读（GET + SELECT），核对关键表行数与两次调用结果一致。
 *
 * 期望值不是硬编码的，而是脚本自己从库里按同一套规则算出来的（候选菜单 SQL → 权限码前缀集合 →
 * 闸门 / 显隐 / 权限三层过滤 → 重建树可达性），再与接口返回逐 id 比对；不一致时打印差集。
 * ⚠️ 唯一硬编码的是超管基线（改动前实测 410 个节点 / 14 个顶级），菜单数据被改动后才需要重新采集。
 */
const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')
const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`
const CLIENT = 'tenant-admin'

/** 账号：admin=系统租户超管；e2e_hr_ta=系统租户内非超管（SYSTEM_ADMIN，18 码）；
 *  e2e_hr_t2=租户 2 非超管（E2E_T2_ADMIN，4 码，权限很少，用于断言 ③）。 */
const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户', tenantId: '1', client: CLIENT, isSystemTenant: true, isSuper: true }
const TA = { u: 'e2e_hr_ta', p: 'admin123', t: '系统租户', tenantId: '1', client: CLIENT, isSystemTenant: true, isSuper: false }
const T2 = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2', tenantId: '2', client: CLIENT, isSystemTenant: false, isSuper: false }

/** 改动前基线（2026-09-22 旧实例实测）：admin 调 /menu/user/mega/tenant-admin?tenantId=1 */
const ADMIN_BASELINE = { top: 14, total: 410 }
/** 超管树里必须存在的关键菜单码 */
const ADMIN_KEY_CODES = ['dashboard', 'mega:sale', 'purchase:return', 'crm:contract', 'finance:other-income-doc']

/** 断言 ②：未被权限码覆盖的菜单码样例（预期始终可见） */
const UNMAPPED_SAMPLES = ['dashboard', 'mega:sale']
/** 断言 ③：有对应权限码但 E2E_T2_ADMIN 不持有的菜单码样例（预期对其隐藏、对超管可见） */
const MAPPED_NOT_HELD_SAMPLES = ['purchase:return', 'system:menu', 'md:payment-method']
/** 断言 ①：SYSTEM_ADMIN 持有、且能由前缀规则命中的菜单码 */
const DERIVED_HIT_SAMPLES = ['finance:other-income-doc', 'finance:month-closing', 'finance:reconciliation']

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

// ───────────────────────── 数据库读取（只读） ─────────────────────────
function sql(stmt) {
  return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
    { cwd: REPO, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }).trim()
}
/** sql.cjs 输出形如「表头 / 分隔线 / 数据 / (N rows)」，取第一个数据行的字符串 */
function scalar(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').split('\n')
    .map(l => l.trim())
    .filter(l => l && !/^-+$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
  return lines.length > 1 ? lines[1] : ''
}
/** 单行 string_agg 结果 → 数组（sql.cjs 单次最多返回 500 行，长列表必须用 string_agg 汇总） */
function listOf(stmt, sep = '|') {
  const v = scalar(stmt)
  if (!v || v === 'null') return []
  return v.split(sep).filter(s => s !== '')
}

/** 权限码 → 其覆盖的「菜单码前缀」集合：a:b:c 展开为 a:b:c / a:b / a */
function toPrefixes(codes) {
  const out = new Set()
  for (const raw of codes) {
    if (!raw) continue
    let cur = raw.trim()
    out.add(cur)
    let i = cur.lastIndexOf(':')
    while (i > 0) { cur = cur.substring(0, i); out.add(cur); i = cur.lastIndexOf(':') }
  }
  return out
}

/** 全量有效权限码的前缀集合（= 「某菜单码是否有对应权限码」的判据） */
const permissionUniverse = () => toPrefixes(listOf(
  `SELECT string_agg(DISTINCT permission_code, '|') FROM sys_permission WHERE deleted = 0 AND status = 0`))

/** 用户有效权限码（与 SysUserMapper.selectPermissionCodesByUserId 同口径） */
function userPermissions(username) {
  return toPrefixes(listOf(
    `SELECT string_agg(DISTINCT p.permission_code, '|')
       FROM sys_permission p
       JOIN sys_role_permission rp ON p.id = rp.permission_id
       JOIN sys_user_role ur ON rp.role_id = ur.role_id
       JOIN sys_role r ON ur.role_id = r.id
       JOIN sys_user u ON ur.user_id = u.id
      WHERE u.username = '${username}' AND p.deleted = 0 AND p.status = 0
        AND r.deleted = 0 AND r.status = 0
        AND (u.tenant_id IS NULL OR r.tenant_id = u.tenant_id)`))
}

/** 候选菜单（与 getUserMegaMenus 的 wrapper 同口径：clientType/status/visible/deleted + 可选 menuLevel=0） */
function candidateMenus(client, onlyTenantLevel) {
  const where = `client_type = '${client}' AND status = 1 AND visible = 1 AND deleted = 0`
    + (onlyTenantLevel ? ' AND menu_level = 0' : '')
  return listOf(`SELECT string_agg(id::text || '~' || parent_id::text || '~' || coalesce(menu_code, ''), '|')
                   FROM sys_menu WHERE ${where}`, '|')
    .map(s => { const [id, parentId, code] = s.split('~'); return { id, parentId, code } })
}

/** 平台 → 租户的菜单闸门（sys_tenant_menu）。返回 null 表示该租户一行都没有（= 平台尚未配置） */
function tenantGate(tenantId) {
  const ids = listOf(`SELECT string_agg(menu_id::text, '|') FROM sys_tenant_menu WHERE tenant_id = ${tenantId} AND deleted = 0`)
  return ids.length ? new Set(ids) : null
}

/** 租户级菜单显隐（设置 → 菜单配置），存 sys_project_config 的 JSON 数组 */
function hiddenMenuIds(tenantId) {
  const v = scalar(`SELECT coalesce((SELECT config_value FROM sys_project_config
                    WHERE tenant_id = ${tenantId} AND config_key = 'set:menu-config:hidden'
                      AND deleted = 0 AND status = 0 ORDER BY id DESC LIMIT 1), '')`)
  if (!v) return new Set()
  try { return new Set(JSON.parse(v).map(String)) } catch { return new Set() }
}

const countRows = (table, extra = '') => scalar(`SELECT count(*) FROM ${table} ${extra}`)

// ───────────────────────── 期望值与接口调用 ─────────────────────────
/**
 * 按实现口径算出「接口应当返回的节点 id 集合」。
 * 三层过滤顺序与 SysMenuServiceImpl 一致：租户闸门 → 租户显隐 → 权限码派生；
 * 最后按 buildMenuTree 的既有行为（从 parent_id = 0 递归）剔除「父节点被过滤掉」的孤儿。
 */
function expectedNodeIds(user, universe) {
  const onlyTenantLevel = !user.isSystemTenant && !user.isSuper
  const cands = candidateMenus(user.client, onlyTenantLevel)
  const held = user.isSuper ? null : userPermissions(user.u)
  const gate = user.isSystemTenant ? null : tenantGate(user.tenantId)
  const hidden = hiddenMenuIds(user.tenantId)

  const kept = cands.filter(m => {
    if (gate && !gate.has(m.id)) return false
    if (hidden.has(m.id)) return false
    // 过渡口径：menu_code 为空、或权限码库里没有码对应它 ⇒ 保持可见
    if (held && m.code && universe.has(m.code) && !held.has(m.code)) return false
    return true
  })

  const byParent = new Map()
  for (const m of kept) {
    if (!byParent.has(m.parentId)) byParent.set(m.parentId, [])
    byParent.get(m.parentId).push(m)
  }
  const reachable = new Set()
  const stack = ['0']
  while (stack.length) {
    for (const c of (byParent.get(stack.pop()) || [])) {
      if (!reachable.has(c.id)) { reachable.add(c.id); stack.push(c.id) }
    }
  }
  return { cands, kept, reachable, gate, held }
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

/** 拉菜单接口并把树摊平成 { ids, codes } */
async function fetchMenu(user, token) {
  const res = await req('GET', `/menu/user/mega/${user.client}?tenantId=${user.tenantId}`, { token })
  if (res.status !== 200) throw new Error(`${user.u} 菜单接口 ${res.status}: ${res.text.slice(0, 200)}`)
  const tree = res.json?.data || []
  const ids = new Set(), codes = new Set()
  const walk = (nodes) => nodes.forEach(n => {
    ids.add(String(n.id))
    if (n.menuCode) codes.add(n.menuCode)
    if (n.children && n.children.length) walk(n.children)
  })
  walk(tree)
  return { tree, ids, codes, top: tree.length }
}

const setDiff = (a, b) => [...a].filter(x => !b.has(x))

// ───────────────────────── 主流程 ─────────────────────────
;(async () => {
  console.log(`验证目标: ${BASE}`)
  console.log('（只读脚本：仅 GET 菜单接口 + SELECT，不写库；可重复跑）')

  const universe = permissionUniverse()
  console.log(`\n权限码库（deleted=0 且 status=0）前缀集合大小 = ${universe.size}`)
  const gate1 = tenantGate('1'), gate2 = tenantGate('2')
  console.log(`sys_tenant_menu：租户 1 = ${gate1 ? gate1.size + ' 行' : '0 行（未配置，按放行处理）'}，`
    + `租户 2 = ${gate2 ? gate2.size + ' 行' : '0 行（未配置，按放行处理）'}`)

  const before = {
    tenantMenu: countRows('sys_tenant_menu'),
    roleMenu: countRows('sys_role_menu'),
  }

  const tokens = {}
  for (const user of [ADMIN, TA, T2]) tokens[user.u] = await login(user)
  console.log(`登录成功: ${[ADMIN, TA, T2].map(u => u.u).join(' / ')}`)

  const resp = {}
  for (const user of [ADMIN, TA, T2]) resp[user.u] = await fetchMenu(user, tokens[user.u])
  console.log(`菜单接口节点数: ${[ADMIN, TA, T2].map(u => `${u.u}=${resp[u.u].ids.size}`).join(', ')}`)

  // ══ ① 派生命中：非超管能看到与其权限码前缀对应的菜单 ══
  section('① 派生命中（证伪：改造后非超管菜单接口仍返回空数组 / 有码菜单被误杀）')
  ok('【修复】租户 2 的 E2E_T2_ADMIN 菜单接口不再返回空数组',
    resp[T2.u].ids.size > 0, `节点数=${resp[T2.u].ids.size}（改动前为 0）`)
  ok('【修复】系统租户内非超管 SYSTEM_ADMIN 菜单接口不再返回空数组',
    resp[TA.u].ids.size > 0, `节点数=${resp[TA.u].ids.size}（改动前为 0）`)

  const expTA = expectedNodeIds(TA, universe)
  const derivedHitsInCandidates = expTA.kept.filter(m => m.code && universe.has(m.code) && expTA.held.has(m.code))
    .map(m => m.code)
  ok('【派生命中】SYSTEM_ADMIN 由权限码前缀派生命中的菜单码非空',
    derivedHitsInCandidates.length >= DERIVED_HIT_SAMPLES.length,
    `命中=${derivedHitsInCandidates.join(', ')}`)
  for (const code of DERIVED_HIT_SAMPLES) {
    ok(`【派生命中】持有权限码的菜单可见: ${code}`,
      resp[TA.u].codes.has(code), `在响应中=${resp[TA.u].codes.has(code)}`)
  }

  // ══ ② 过渡口径：无对应权限码的菜单仍可见 ══
  section('② 过渡口径（证伪：把「权限码库未覆盖」当成「无权限」而隐藏 ⇒ 导航缩水）')
  for (const user of [TA, T2]) {
    const exp = user === TA ? expTA : expectedNodeIds(T2, universe)
    const unmapped = exp.cands.filter(m => m.code && !universe.has(m.code))
    const missing = unmapped.filter(m => !resp[user.u].ids.has(m.id))
    ok(`【过渡口径】${user.u}: 无对应权限码的菜单全部仍可见`,
      unmapped.length > 0 && missing.length === 0,
      `无码菜单=${unmapped.length}，被误隐藏=${missing.length}`)
    for (const code of UNMAPPED_SAMPLES) {
      const m = exp.cands.find(x => x.code === code)
      if (m) ok(`【过渡口径】${user.u}: 无码菜单可见样例 ${code}`, resp[user.u].ids.has(m.id))
    }
  }

  // ══ ③ 有码但未持有 ⇒ 隐藏（按权限，而非全放开）══
  section('③ 按权限隐藏（证伪：实现变成「全部放开」/ 隐藏过头）')
  const expT2 = expectedNodeIds(T2, universe)
  // ⚠️ 这里必须用候选集 cands 而不是过滤后的 kept：kept 已经把「有码未持有」的菜单剔掉了，
  //    在 kept 上再找一遍等于自证（恒为 0，测不出任何东西）。
  const mappedNotHeldT2 = expT2.cands.filter(m => m.code && universe.has(m.code) && !expT2.held.has(m.code))
  const leakedT2 = mappedNotHeldT2.filter(m => resp[T2.u].ids.has(m.id))
  ok('【按权限隐藏】E2E_T2_ADMIN(4 码) 不持有的有码菜单确实隐藏',
    mappedNotHeldT2.length > 0 && leakedT2.length === 0,
    `应有码未持有=${mappedNotHeldT2.length}，实际泄漏=${leakedT2.length}`)
  for (const code of MAPPED_NOT_HELD_SAMPLES) {
    const m = expT2.cands.find(x => x.code === code)
    if (!m) continue
    ok(`【按权限隐藏】${code} 对 E2E_T2_ADMIN 隐藏`,
      !resp[T2.u].codes.has(code))
    ok(`【对照】${code} 对超管可见（同一菜单确实存在，排除「接口本来就没有」）`,
      resp[ADMIN.u].codes.has(code))
  }
  const expTA2 = expTA
  const mappedNotHeldTA = expTA2.cands.filter(m => m.code && universe.has(m.code) && !expTA2.held.has(m.code))
  const leakedTA = mappedNotHeldTA.filter(m => resp[TA.u].ids.has(m.id))
  ok('【按权限隐藏】SYSTEM_ADMIN(18 码) 不持有的有码菜单确实隐藏',
    mappedNotHeldTA.length > 0 && leakedTA.length === 0,
    `应有码未持有=${mappedNotHeldTA.length}，实际泄漏=${leakedTA.length}`)
  ok('【对比】码多的账号(18 码)看到的菜单多于码少的账号(4 码)',
    resp[TA.u].ids.size > resp[T2.u].ids.size,
    `${TA.u}=${resp[TA.u].ids.size} > ${T2.u}=${resp[T2.u].ids.size}`)

  // 全量集合一致：接口返回必须与「按同口径算出的可达节点集」逐 id 相等
  for (const [user, exp] of [[TA, expTA], [T2, expT2]]) {
    const extra = setDiff(resp[user.u].ids, exp.reachable)
    const missing = setDiff(exp.reachable, resp[user.u].ids)
    ok(`【集合一致】${user.u}: 返回节点集 == 派生期望集（${exp.reachable.size} 个）`,
      extra.length === 0 && missing.length === 0,
      `多出=${extra.length}${extra.length ? ' 例:' + extra.slice(0, 3) : ''}，缺失=${missing.length}${missing.length ? ' 例:' + missing.slice(0, 3) : ''}`)
  }

  // ══ ④ 超管不受影响 ══
  section('④ 超管不受影响（证伪：改造误伤 admin 早退分支）')
  ok('【超管】顶级菜单数与改动前一致', resp[ADMIN.u].top === ADMIN_BASELINE.top,
    `现在=${resp[ADMIN.u].top}，改动前=${ADMIN_BASELINE.top}`)
  ok('【超管】节点总数与改动前一致', resp[ADMIN.u].ids.size === ADMIN_BASELINE.total,
    `现在=${resp[ADMIN.u].ids.size}，改动前=${ADMIN_BASELINE.total}`)
  for (const code of ADMIN_KEY_CODES) {
    ok(`【超管】关键菜单仍在: ${code}`, resp[ADMIN.u].codes.has(code))
  }

  // ══ ⑤ 无副作用 / 可重复跑 ══
  section('⑤ 无副作用、可重复跑')
  const second = await fetchMenu(T2, tokens[T2.u])
  ok('【可重复】同一账号连续两次调用结果一致（节点数）',
    second.ids.size === resp[T2.u].ids.size, `${resp[T2.u].ids.size} / ${second.ids.size}`)
  const after = {
    tenantMenu: countRows('sys_tenant_menu'),
    roleMenu: countRows('sys_role_menu'),
  }
  ok('【无副作用】sys_tenant_menu 行数未被改动',
    after.tenantMenu === before.tenantMenu, `${before.tenantMenu} → ${after.tenantMenu}`)
  ok('【无副作用】sys_role_menu 行数未被改动（仍应是 3 行，本改造不改表）',
    after.roleMenu === before.roleMenu, `${before.roleMenu} → ${after.roleMenu}`)

  if (resp[T2.u].ids.size === 0 && resp[TA.u].ids.size === 0) {
    console.log('\n提示：两个非超管账号都返回空数组 —— 若当前跑的是**未重启的旧实例**，这是改动前的预期现象；'
      + '请在重启后的新实例上重跑本脚本。')
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('脚本异常:', e.message); process.exit(2) })
