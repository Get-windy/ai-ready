#!/usr/bin/env node
/**
 * 双写实机验收：**建一个往来单位**，看它是否同时落到 `party` / `party_tenant`。
 *
 * 【为什么必须真机】双写接在 `PartyServiceImpl` 覆盖的 6 个写方法上，
 *   单测只能证明"方向判定"这类纯函数（`PartyMirrorWriterTest`），
 *   而"**这条链路真的被走到了吗**"只有对着运行中的实例建一次档才算数
 *   （本仓最贵的历史包袱就是"配了不生效 / 接了没走通"）。
 *
 * 【验什么】
 *   ① `POST /api/erp/md/customer` 建档成功；
 *   ② `party` 出现同一 id 的行（id **复用** `biz_party.id`）；
 *   ③ `party_tenant` 出现 `direction='SALE'` 的边（该客户 roles 含 CUSTOMER）；
 *   ④ **反向对照**：`party.tenant_id` 恒 0（共享层），而边的 `tenant_id` = 建档租户。
 *
 * 【探针一律硬删】按名称前缀扫库删三张表（软删会占唯一键，第二次跑不通）。
 *
 * 用法：node tools/verify-party-dual-write.cjs
 *   前置：后端在 5655 运行；`V11.497.0`/`V11.506.0`/`V11.507.0` 已应用。
 *   退出码：0 = 全绿；1 = 有 FAIL；2 = 前置不成立或脚本异常。
 */

const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')
const BASE = process.env.BASE || `http://localhost:${process.env.PORT || '5655'}/api`
const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }
const PREFIX = 'E2EDW探针客户'
const PROBE_NAME = `${PREFIX}${Date.now()}`
/** 探针编码（唯一，且比名称更适合当回查键：名称可能被清洗/截断） */
const PROBE_CODE = `E2EDW${Date.now()}`

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
    throw new ProbeFailure(`SQL 失败: ${stmt.slice(0, 120)}`)
  }
}
function scalar(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').split('\n').map(l => l.trim())
    .filter(l => l && !/^-+(\+-+)*$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
  return lines.length > 1 ? lines[1] : '(空)'
}
const num = (s) => Number(scalar(s))

async function req(method, p, { token, body } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON */ }
  return { status: res.status, json, text }
}

async function login(user) {
  const cap = await req('GET', '/auth/captcha')
  if (!cap.json?.data?.img) throw new ProbeFailure(`验证码接口异常: ${cap.text.slice(0, 150)}`)
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    body: { username: user.u, password: user.p, tenantName: user.t, captcha: code, captchaKey: cap.json.data.uuid },
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new ProbeFailure(`${user.u} 登录失败: ${res.text.slice(0, 200)}`)
  return token
}

/** 硬删探针（三张表一起）。 */
function cleanup() {
  const ids = `SELECT id FROM biz_party WHERE party_code LIKE 'E2EDW%' OR party_name LIKE '${PREFIX}%'`
  sql(`DELETE FROM party_tenant WHERE party_id IN (${ids})`, true)
  sql(`DELETE FROM party WHERE id IN (${ids})`, true)
  sql(`DELETE FROM biz_party WHERE party_code LIKE 'E2EDW%' OR party_name LIKE '${PREFIX}%'`, true)
}

;(async () => {
  console.log(`验证目标: ${BASE}`)
  console.log(`清理前探针残留: ${num(`SELECT count(*) FROM biz_party WHERE party_code LIKE 'E2EDW%' OR party_name LIKE '${PREFIX}%'`)}`)
  try { cleanup() } catch { /* 表可能还不存在，交给 ⓪ */ }

  try {
    section('⓪ 前置：三张表与新载体都在')
    const n = num(`SELECT count(*) FROM information_schema.tables
      WHERE table_schema='public' AND table_name IN ('biz_party','party','party_tenant')`)
    ok('biz_party / party / party_tenant 齐备', n === 3, `实际 ${n} 张`)
    if (n !== 3) process.exit(2)
    const seq = scalar(`SELECT column_default FROM information_schema.columns
      WHERE table_schema='public' AND table_name='party_tenant' AND column_name='id'`)
    ok('party_tenant.id 已挂序列默认值（V11.507.0）', /nextval/.test(seq), `实际 ${seq}`)

    const token = await login(ADMIN)
    console.log(`登录成功: ${ADMIN.u}`)

    section('① 建档：POST /erp/md/customer')
    const created = await req('POST', '/erp/md/customer', {
      // ⚠️ 两个坑都实踩过：
      //   ① `biz_party.party_code` / `party_name` 都是 **NOT NULL 且无默认值** ⇒ 必须给编码，
      //      否则底层 NOT NULL 违反被兜底成 400「请求数据不完整或存在冲突」
      //      （文案听着像参数问题，实际是数据库约束 —— 别被带偏）；
      //   ② 本接口的**入参名是 `partnerName`/`partnerCode` 而不是 `partyName`/`partyCode`**
      //      （见 `MdCustomerController` 类注释的字段名映射）⇒ 传 `partyName` 会被**静默忽略**，
      //      建档"成功"（HTTP 200）但库里没有该行。两个变体都传，避免再踩。
      token, body: {
        partnerCode: PROBE_CODE, partyCode: PROBE_CODE,
        partnerName: PROBE_NAME, partyName: PROBE_NAME,
        partnerType: 'customer', partyType: 1, roles: 'CUSTOMER', status: 1,
      },
    })
    ok('建档成功（HTTP 200）', created.status === 200, `status=${created.status} ${created.text.slice(0, 200)}`)
    // ⚠️ 不从响应里取 id 作为**唯一**来源：控制器在 `!success || id == null` 时会返回 `data: null`
    //    （实测就是这个形态）。**回查库**拿 id 更稳，也顺带证明"行真的落库了"。
    const dbId = scalar(`SELECT id FROM biz_party WHERE party_code = '${PROBE_CODE}' AND deleted = 0`)
    const respId = created.json?.data?.id
    const id = respId != null ? respId : (dbId === '(空)' ? null : dbId)
    ok('拿得到新建主体的 id（响应或回查库）', id != null, `响应里 id=${respId}，库里 id=${dbId}`)
    if (id == null) throw new ProbeFailure('建档后库里也查不到该行，无法继续断言')

    section('② 双写：party 出现同一 id 的行')
    ok(`party 有 id=${id} 的行`, num(`SELECT count(*) FROM party WHERE id = ${id}`) === 1,
      `实际 ${num(`SELECT count(*) FROM party WHERE id = ${id}`)} 行`)
    ok('party.tenant_id 恒 0（共享层）', scalar(`SELECT tenant_id FROM party WHERE id=${id}`) === '0',
      `实际 ${scalar(`SELECT tenant_id FROM party WHERE id=${id}`)}`)
    ok('party.party_name 与建档一致', scalar(`SELECT party_name FROM party WHERE id=${id}`) === PROBE_NAME,
      `实际 ${scalar(`SELECT party_name FROM party WHERE id=${id}`)}`)

    section('③ 双写：party_tenant 出现 SALE 边（roles 含 CUSTOMER）')
    ok('有 direction=SALE 的边', num(`SELECT count(*) FROM party_tenant
        WHERE party_id = ${id} AND direction = 'SALE' AND deleted = 0`) === 1,
      `实际 ${num(`SELECT count(*) FROM party_tenant WHERE party_id=${id} AND direction='SALE' AND deleted=0`)} 条`)
    ok('边 id 来自独立序列（≥9.0e18）',
      num(`SELECT count(*) FROM party_tenant WHERE party_id=${id} AND id >= 9000000000000000000`) === 1,
      `边 id=${scalar(`SELECT id FROM party_tenant WHERE party_id=${id} LIMIT 1`)}`)
    ok('边的 tenant_id = 建档租户（不是 0）',
      num(`SELECT count(*) FROM party_tenant WHERE party_id=${id} AND tenant_id > 0`) === 1,
      `实际 ${scalar(`SELECT tenant_id FROM party_tenant WHERE party_id=${id} LIMIT 1`)}`)

    section('④ 反向对照：改一次再验（updateById 也接上了双写）')
    const upd = await req('PUT', `/erp/md/customer/${id}`, {
      token, body: { partyName: PROBE_NAME + '改名' },
    })
    ok('更新成功（HTTP 200）', upd.status === 200, `status=${upd.status}`)
    ok('party 里的名称跟着变了（写后回读整行再镜像，不会把别的列擦成 NULL）',
      scalar(`SELECT party_name FROM party WHERE id=${id}`) === PROBE_NAME + '改名',
      `实际 ${scalar(`SELECT party_name FROM party WHERE id=${id}`)}`)
    ok('前一次写入的字段没被擦掉（phone 之类本就没写，验 code 仍在）',
      scalar(`SELECT party_type FROM party WHERE id=${id}`) === '1',
      `party_type=${scalar(`SELECT party_type FROM party WHERE id=${id}`)}`)
  } finally {
    console.log('\n—— 现场还原（探针一律硬删）——')
    cleanup()
    ok('【自清理】探针已从 biz_party / party / party_tenant 三张表硬删',
      num(`SELECT count(*) FROM biz_party WHERE party_code LIKE 'E2EDW%' OR party_name LIKE '${PREFIX}%'`) === 0)
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail ? 1 : 0)
})().catch(e => {
  console.error(`脚本异常: ${e instanceof ProbeFailure ? e.message : e}`)
  try { cleanup() } catch { /* 尽力而为 */ }
  process.exit(2)
})
