#!/usr/bin/env node
/**
 * 商城「入店 / 注册两分支 / 租户审核」实机两向验证（DOMAIN-MODEL §11.3 阶段 1）
 *
 * 【要钉死的是什么】顾客是**系统级身份**（`shop_user`，`tenant_id` 恒 0、已进
 *   IGNORE_TENANT_TABLES），而"他属不属于这家店"是**关联**（`shop_user_tenant`）。
 *   这个拆分一旦没接上，会同时坏两头：
 *     · 入口失守：任何一个系统顾客能登进**任何一家店**（跨租户）；
 *     · 后台失守：租户侧顾客页按 `shop_user.tenant_id` 过滤 ⇒ **永远空列表**，审核永远 404。
 *
 * 【本脚本验的七件事】
 *   ① 新顾客在某店注册 ⇒ **建系统顾客（tenant_id=0）+ 本店关联**（需审核店 ⇒ 待审核）
 *   ② 待审核时登录 ⇒ 拒，且文案是「等待店铺审核」（不是含糊的"登录失败"）
 *   ③ **复用别人的用户名 + 错密码**在本店注册 ⇒ 拒（自证失败 = 防冒名绑定）
 *   ④ **复用 + 正确密码** ⇒ 通过，且**不新建系统顾客**（只补一条本店关联，source=system_reuse）
 *   ⑤ 免审核店（`reg_audit_required=0`）⇒ 关联直接「正常」；此时登录 ⇒ 放行
 *   ⑥ **入店校验**：在 A 店注册过、没在 B 店注册的顾客登录 B 店 ⇒ 拒「还没有在本店注册」
 *   ⑦ 不带店铺标识 ⇒ 拒（绝不放行成"看全表"或"猜一家店"）
 *
 * 【为什么必须真机】这条链路横跨"白名单端点（无登录态）→ 会话租户 → 租户拦截器"，
 *   单测只能覆盖纯判定（见 `MallAuthServiceImplTest`），装配部分只有在真进程上开一次门才算数。
 *
 * 【探针数据一律硬删】前缀 `E2EMSE`（用户名 / 店铺配置行）。清理按前缀扫库删。
 *
 * 用法：node tools/verify-mall-shop-entry.cjs
 *   前置：后端在 5655 运行。
 *   退出码：0 = 全绿；1 = 有 FAIL；2 = 前置不成立或脚本异常。
 */

const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')
const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`

/** 探针前缀（用户名 + 店铺配置行都带它，清理按它扫库）。 */
const PREFIX = 'E2EMSE'
/** 探针店铺：A = 需审核（reg_audit_required=1）；B = 免审核（=0）。 */
const SHOP_A = 990021
const SHOP_B = 990022
/** 探针用户名。 */
const USER_SELF = `${PREFIX.toLowerCase()}_self`    // 只在 A 店注册过的顾客
const USER_REUSE = `${PREFIX.toLowerCase()}_reuse`  // 用来验"复用须自证"的顾客
const PASSWORD = 'Probe@123456'
const WRONG_PASSWORD = 'Wrong@654321'

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

class ProbeFailure extends Error {}

function sql(stmt, quiet = false) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024,
        stdio: quiet ? ['ignore', 'pipe', 'ignore'] : ['ignore', 'pipe', 'pipe'] }).trim()
  } catch (e) {
    if (quiet) return ''
    const first = (String(e.stderr || e.message || e).replace(/\r/g, '').split('\n')
      .map(l => l.trim()).filter(Boolean)[0]) || '(无输出)'
    throw new ProbeFailure(`SQL 执行失败（脚本自身问题）: ${stmt.slice(0, 120)} —— ${first}`)
  }
}

/** 取单值（sql.cjs 输出：表头 / 分隔线 / 数据 / "(N rows)"）。 */
function scalar(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').split('\n').map(l => l.trim())
    .filter(l => l && !/^-+(\+-+)*$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
  return lines.length > 1 ? lines[1] : '(空)'
}
const num = (stmt) => Number(scalar(stmt))

async function req(method, p, { shop, body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (shop !== undefined) headers['X-Tenant-Id'] = String(shop)
  const res = await fetch(BASE + p, {
    method, headers, body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON */ }
  return { status: res.status, json, text }
}
const msgOf = (r) => (r.json && (r.json.message || r.json.msg)) || ''

/** 硬删全部探针数据（软删会占唯一键，第二次跑不通）。 */
function hardCleanProbes() {
  const ids = `SELECT id FROM shop_user WHERE username LIKE '${PREFIX.toLowerCase()}%'`
  sql(`DELETE FROM shop_user_tenant WHERE shop_user_id IN (${ids})`, true)
  sql(`DELETE FROM shop_user WHERE username LIKE '${PREFIX.toLowerCase()}%'`, true)
  sql(`DELETE FROM tenant_shop_config WHERE tenant_id IN (${SHOP_A}, ${SHOP_B})`, true)
}

function probeCounts() {
  const ids = `SELECT id FROM shop_user WHERE username LIKE '${PREFIX.toLowerCase()}%'`
  return {
    user: num(`SELECT count(*) FROM shop_user WHERE username LIKE '${PREFIX.toLowerCase()}%'`),
    link: num(`SELECT count(*) FROM shop_user_tenant WHERE shop_user_id IN (${ids})`),
    config: num(`SELECT count(*) FROM tenant_shop_config WHERE tenant_id IN (${SHOP_A}, ${SHOP_B})`),
  }
}

const userCount = (username) => num(`SELECT count(*) FROM shop_user WHERE username = '${username}'`)
const linkOf = (username, shop) => scalar(`SELECT st.status FROM shop_user_tenant st
  JOIN shop_user u ON u.id = st.shop_user_id
  WHERE u.username = '${username}' AND st.tenant_id = ${shop} AND st.deleted = 0`)

;(async () => {
  console.log(`验证目标: ${BASE}`)
  console.log(`清理前探针行数: ${JSON.stringify(probeCounts())}`)
  try { hardCleanProbes() } catch { /* 表可能还不存在 */ }
  console.log(`清理后探针行数: ${JSON.stringify(probeCounts())}（应全为 0）`)

  try {
    // ══ ⓪ 前置 + 夹具 ══
    section('⓪ 前置：商城配置表可用，并落两家探针店铺（A 需审核 / B 免审核）')
    const hasTables = num(`SELECT count(*) FROM information_schema.tables
      WHERE table_schema='public' AND table_name IN ('shop_user','shop_user_tenant','tenant_shop_config')`)
    ok('三张表齐备（shop_user / shop_user_tenant / tenant_shop_config）', hasTables === 3, `实际 ${hasTables} 张`)
    if (hasTables !== 3) {
      console.log('\n!! 前置不成立：商城顾客/关联表不可用（本脚本不代替迁移）。')
      process.exit(2)
    }

    sql(`INSERT INTO tenant_shop_config (id, tenant_id, reg_audit_required) VALUES
      (990021001, ${SHOP_A}, 1), (990022001, ${SHOP_B}, 0)`)
    ok('两家探针店铺配置已落库（A 需审核=1 / B 免审核=0）', probeCounts().config === 2,
      JSON.stringify(probeCounts()))

    // ══ ① 新顾客在 A 店注册 ⇒ 建系统顾客 + 待审核关联 ══
    section('① 新顾客在「需审核」店注册 ⇒ 建系统顾客（tenant_id=0）+ 本店关联（待审核）')
    const r1 = await req('POST', '/v1/mall/auth/register', {
      shop: SHOP_A, body: { username: USER_SELF, password: PASSWORD, phone: '13800000001' },
    })
    ok('注册成功（HTTP 200）', r1.status === 200, `status=${r1.status} ${msgOf(r1)}`)
    ok('系统顾客已建，且 tenant_id = 0（顾客是**系统级**身份）',
      scalar(`SELECT tenant_id FROM shop_user WHERE username='${USER_SELF}'`) === '0',
      `实际 ${scalar(`SELECT tenant_id FROM shop_user WHERE username='${USER_SELF}'`)}`)
    ok('本店关联 = 待审核(0)（该店 reg_audit_required=1）', linkOf(USER_SELF, SHOP_A) === '0',
      `实际 ${linkOf(USER_SELF, SHOP_A)}`)

    // ══ ② 待审核时登录 ⇒ 拒 ══
    section('② 待审核时登录 ⇒ 拒，且文案说清是「等待店铺审核」')
    const r2 = await req('POST', '/v1/mall/auth/login', {
      shop: SHOP_A, body: { username: USER_SELF, password: PASSWORD },
    })
    ok('登录被拒（非 200）', r2.status !== 200, `status=${r2.status}`)
    ok('文案是「等待店铺审核」（不是含糊的登录失败）',
      /等待店铺审核/.test(msgOf(r2)), `message=${msgOf(r2)}`)

    // ══ ③ 复用 + 错密码 ⇒ 拒（防冒名绑定） ══
    section('③ 用**已存在的用户名 + 错密码**在 B 店注册 ⇒ 拒（自证失败 = 防冒名绑定）')
    const r3 = await req('POST', '/v1/mall/auth/register', {
      shop: SHOP_B, body: { username: USER_SELF, password: WRONG_PASSWORD },
    })
    ok('注册被拒（非 200）', r3.status !== 200, `status=${r3.status}`)
    ok('文案点明「冒名绑定」这一类风险',
      /冒名/.test(msgOf(r3)), `message=${msgOf(r3)}`)
    ok('B 店**没有**建立关联（拒得干净）', linkOf(USER_SELF, SHOP_B) === '(空)',
      `实际 ${linkOf(USER_SELF, SHOP_B)}`)

    // ══ ④ 复用 + 正确密码 ⇒ 通过，且不新建系统顾客 ══
    section('④ 用**正确的原密码**在 B 店注册 ⇒ 通过，且**复用**同一个系统顾客')
    const usersBefore = userCount(USER_SELF)
    const r4 = await req('POST', '/v1/mall/auth/register', {
      shop: SHOP_B, body: { username: USER_SELF, password: PASSWORD },
    })
    ok('注册成功（HTTP 200）', r4.status === 200, `status=${r4.status} ${msgOf(r4)}`)
    ok('系统顾客**没有**被重复创建（复用而不是新建）',
      userCount(USER_SELF) === usersBefore, `注册前 ${usersBefore} 行 / 注册后 ${userCount(USER_SELF)} 行`)
    ok('B 店关联 = 正常(1)（B 店 reg_audit_required=0 ⇒ 默认同意）', linkOf(USER_SELF, SHOP_B) === '1',
      `实际 ${linkOf(USER_SELF, SHOP_B)}`)
    ok('关联来源 = system_reuse（可追溯是"复用系统顾客"而非新注册）',
      scalar(`SELECT st.source FROM shop_user_tenant st JOIN shop_user u ON u.id=st.shop_user_id
        WHERE u.username='${USER_SELF}' AND st.tenant_id=${SHOP_B}`) === 'system_reuse')

    // ══ ⑤ 免审核店登录 ⇒ 放行 ══
    section('⑤ 在「免审核」店登录 ⇒ 放行（默认同意的效果）')
    const r5 = await req('POST', '/v1/mall/auth/login', {
      shop: SHOP_B, body: { username: USER_SELF, password: PASSWORD },
    })
    ok('登录成功（HTTP 200）', r5.status === 200, `status=${r5.status} ${msgOf(r5)}`)
    ok('返回了 token', !!(r5.json && r5.json.data && r5.json.data.token),
      `data=${JSON.stringify((r5.json || {}).data).slice(0, 120)}`)

    // ══ ⑥ 入店校验：在 A 注册、没在 B 注册 ⇒ 登 B 店被拒 ══
    section('⑥ 入店校验：**没在本店注册过**的顾客不能登进本店')
    const rReg = await req('POST', '/v1/mall/auth/register', {
      shop: SHOP_A, body: { username: USER_REUSE, password: PASSWORD, phone: '13800000002' },
    })
    ok('先在 A 店注册一个顾客（夹具）', rReg.status === 200, `status=${rReg.status} ${msgOf(rReg)}`)
    const r6 = await req('POST', '/v1/mall/auth/login', {
      shop: SHOP_B, body: { username: USER_REUSE, password: PASSWORD },
    })
    ok('拿他去登 **B 店** ⇒ 被拒（没在本店注册）', r6.status !== 200, `status=${r6.status}`)
    ok('文案是「还没有在本店注册」', /还没有在本店注册/.test(msgOf(r6)), `message=${msgOf(r6)}`)
    ok('同一账号登 **A 店**也仍被拒（那次是"待审核"）',
      /等待店铺审核/.test(msgOf(await req('POST', '/v1/mall/auth/login', {
        shop: SHOP_A, body: { username: USER_REUSE, password: PASSWORD },
      }))))

    // ══ ⑦ 租户审核通过（写关联表）⇒ 立刻能进 ══
    section('⑦ 租户审核通过（改**关联表**状态）⇒ 立即可以进店')
    sql(`UPDATE shop_user_tenant SET status = 1, audit_time = now()
      WHERE shop_user_id = (SELECT id FROM shop_user WHERE username='${USER_REUSE}')
        AND tenant_id = ${SHOP_A}`)
    ok('关联已置为正常(1)', linkOf(USER_REUSE, SHOP_A) === '1', `实际 ${linkOf(USER_REUSE, SHOP_A)}`)
    const r7 = await req('POST', '/v1/mall/auth/login', {
      shop: SHOP_A, body: { username: USER_REUSE, password: PASSWORD },
    })
    ok('审核通过后登录 A 店 ⇒ 放行（HTTP 200）', r7.status === 200, `status=${r7.status} ${msgOf(r7)}`)

    // ══ ⑧ 不带店铺标识 ⇒ 拒 ══
    section('⑧ 不带店铺标识（无 X-Tenant-Id）⇒ 拒（绝不放行成"看全表"或"猜一家店"）')
    const r8 = await req('POST', '/v1/mall/auth/login', {
      body: { username: USER_SELF, password: PASSWORD },
    })
    ok('登录被拒（非 200）', r8.status !== 200, `status=${r8.status}`)
    ok('文案点明"无法确定哪家店铺"', /哪家店铺/.test(msgOf(r8)), `message=${msgOf(r8)}`)
  } finally {
    console.log('\n—— 现场还原（探针数据一律硬删）——')
    hardCleanProbes()
    const left = probeCounts()
    ok('【自清理】探针顾客 / 关联 / 店铺配置已**硬删**',
      left.user === 0 && left.link === 0 && left.config === 0, `库内探针行数=${JSON.stringify(left)}`)
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail ? 1 : 0)
})().catch(e => {
  console.error(`脚本异常: ${e instanceof ProbeFailure ? e.message : e}`)
  try { hardCleanProbes() } catch { /* 尽力而为 */ }
  process.exit(2)
})
