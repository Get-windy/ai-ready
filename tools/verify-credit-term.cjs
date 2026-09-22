#!/usr/bin/env node
/**
 * 「协议账期 → 应收/应付到期日」实机两向验证 —— 2026-09-22
 *
 * 【要钉死的是什么】DOMAIN-MODEL §13.3 说「字段设定版是**运行时配置**、必须被下游消费」，
 * 而「结算口径 → 应收/应付到期日」的第一处真实消费就在这里。本脚本按**两层判定**证明它真的生效：
 *
 *   第 1 层（先取「结算方式」）：
 *     ① 无适用协议        ⇒ 走「无协议无特定客户模式」= **现款现结**，到期日 = 业务日
 *     ⑧ **协议已到期**    ⇒ 同上（到期是正常商业事实，不是缺项）
 *     ⑤-A **没约定结算方式** ⇒ 同上（第 1 层判不了 ⇒ 走无协议模式）
 *   第 2 层（约定的结算方式是哪一种）：
 *     ② 账期结算 + 天数 + 方向一致 ⇒ 到期日 = **业务日期** + 约定天数（不是"今天"+天数）
 *     ③ 应付侧方向正确时同样命中
 *     ⑨ 现款现货 / 滚结          ⇒ 本笔**无账期**，到期日 = 业务日，
 *                                  且**不报「未约定账期」**（用户第 4 轮点名的老毛病）
 *     ⑤-B 账期结算但**缺天数**   ⇒ 条件必填缺失 ⇒ **提交被拒**（不是留空挂人工）
 *     ④ 方向与本笔债权方对不上   ⇒ 账期口径说不清 ⇒ 同样**提交被拒**
 *   兜底：
 *     ⑥ 身份信息不足（无业务日期 / 对方租户 / 主体 id）⇒ 判不了就现款现结（不猜主体、不猜租户）
 *     ⑦ 反向对照：账期缺项换任何业务日期都提交不了（不是"日期挑得巧"）
 *
 * 【口径变更留痕】本脚本此前断言「有协议但未约定 ⇒ 到期日留空、单据照开」，
 *   那是 §13.3 补充口径 3 之前的旧口径，按新裁定**必须拆成两种情形**（⑤-A / ⑤-B）——
 *   拿旧断言去判新行为，会把正确的拒单判成红的。
 *
 * 【为什么必须真机验证】"配置界面能勾、勾了不生效"是本仓最贵的历史包袱。
 *   单测能证明实现的分支正确，但证明不了"运行中的进程里真的接上了" ——
 *   只有对着 5655 上的实例开一张单、再读库看 `finance_receivable.due_date` 才算数。
 *
 * 【⚠️ 探针协议为什么用 SQL 直接落库，而不是走 /agreement 的签署流程】
 *   协议**成立流程**（要约→唯一送达→反签→双签→生效）已由 `tools/verify-agreement.cjs`
 *   55/55 独立覆盖。本脚本的主题是**消费**（协议 → 到期日），因此刻意只把
 *   "一份已生效、已约定账期的协议"作为**夹具**直接落库：
 *     · 避免临时给真实角色授协议权限再还原（verify-agreement.cjs 里那段探针会写真实
 *       `sys_role_permission`，多一个脚本写它就多一份污染风险）；
 *     · 避免"同一账号并发登录顶掉会话"使整段准备流程变红，从而掩盖真正的消费断言。
 *   代价是本脚本不覆盖"协议由谁签成"，这一点在脚本与报告里都写明了。
 *
 * 【探针数据一律**硬删**】前缀 `E2ECT`（协议标题/编号、往来单位、来源单号）。
 *   清理**按前缀扫库删**（不只按内存里记得的 id）：上次运行若被 kill，那些行会永远留下。
 *   清理前后各打一次行数快照，残留不再静默。
 *
 * 【id 一律字符串】协议 id 是本脚本自选的 9001… 大整数（> MAX_SAFE_INTEGER），
 *   `Number()` / `JSON.parse` 都会静默截断末尾几位；因此拼 SQL/URL 前一律过 `mustId`。
 *
 * 【不 sleep】登录后直接开跑；探针夹具在第一步就落库。
 *
 * ⚠️ 【不要并发跑】后端 sa-token 是 `is-concurrent=false`：同账号再登录会顶掉旧会话。
 *   若别的进程正用 admin 登录，本脚本会以中文报"环境冲突"（exit 2），而不是伪装成系统缺陷。
 *
 * 【后端必须是**本次改动之后**重启过的进程】本脚本会校验这一点：
 *   若第 ② 组"有协议+约定 ⇒ 业务日+天数"整组红、且到期日仍等于传入的哨兵值，
 *   说明运行中的进程还是旧代码 —— 脚本会明确提示"需重启后端后复跑"，而不是含糊其辞。
 *
 * 用法：node tools/verify-credit-term.cjs
 *   前置：后端在 5655 运行、Flyway 已执行到 V11.492.0（协议表 + 含 SETTLEMENT_TYPE 的字段字典都在）。
 *   退出码：0 = 全绿；1 = 有 FAIL；2 = 前置不成立或脚本异常。
 */
const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')
const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`

const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }

/** 本端（会话）租户：应收里是卖方/债权方，应付里是买方/债务方。 */
const T1 = 1
/** 对方租户：AG1 —— 账期结算 + 45 天（方向由甲方提供 ⇒ 会话租户是卖方时可命中）。 */
const T2 = 990011
/** 对方租户：AG2 —— 账期结算 + 有方向，但**没填账期天数**（条件必填缺失 ⇒ 应被拒单）。 */
const T3 = 990012
/** 对方租户：没有任何协议（用来验证"无适用协议 ⇒ 现款现结"）。 */
const T4 = 990013
/** 对方租户：AG3 —— 账期结算 + 45 天，账期由**对方**提供（验证应付侧方向正确时的命中）。 */
const T5 = 990014
/** 对方租户：AG4 —— 结算方式 = **现款现结**（无账期；验证"约定现金不报未约定账期"）。 */
const T6 = 990015
/** 对方租户：AG5 —— **完全没约定结算方式**（只有天数与方向 ⇒ 第 1 层判不了 ⇒ 走无协议模式）。 */
const T7 = 990016
/** 对方租户：AG6 —— 结算方式 = **滚结**（同样无账期）。 */
const T8 = 990017
/** 对方租户：AG7 —— 账期结算 45 天，但**协议已到期**（有效期在业务日期之前走完）。 */
const T9 = 990018

/** 所有探针数据的前缀（协议标题/编号、来源单号）。清理按它扫库。 */
const PREFIX = 'E2ECT'

/** 探针主体（往来单位）id —— 不落 biz_party：协议与应收都只把它当不透明 id 用，无需真实主档行。 */
const PARTY_A1 = '9001000000000000301' // 甲方主体（租户 1）
const PARTY_B2 = '9001000000000000302' // 乙方主体（租户 T2）
const PARTY_B3 = '9001000000000000303' // 乙方主体（租户 T3）
const PARTY_B5 = '9001000000000000305' // 乙方主体（租户 T5）
const PARTY_B6 = '9001000000000000306' // 乙方主体（租户 T6）
const PARTY_B7 = '9001000000000000307' // 乙方主体（租户 T7）
const PARTY_B8 = '9001000000000000308' // 乙方主体（租户 T8）
const PARTY_B9 = '9001000000000000309' // 乙方主体（租户 T9）

/** 探针协议 / 版本 / 设定行 id（自选大整数，全程当字符串用）。 */
const AG1 = '9001000000000000001', AG1_V1 = '9001000000000000101'
const AG1_S_TYPE = '9001000000000000201', AG1_S_DAYS = '9001000000000000202', AG1_S_PROVIDER = '9001000000000000203'
const AG2 = '9001000000000000002', AG2_V1 = '9001000000000000102'
const AG2_S_TYPE = '9001000000000000204', AG2_S_PROVIDER = '9001000000000000205'
const AG3 = '9001000000000000003', AG3_V1 = '9001000000000000103'
const AG3_S_TYPE = '9001000000000000206', AG3_S_DAYS = '9001000000000000207', AG3_S_PROVIDER = '9001000000000000208'
const AG4 = '9001000000000000004', AG4_V1 = '9001000000000000104'
const AG4_S_TYPE = '9001000000000000209'
const AG5 = '9001000000000000005', AG5_V1 = '9001000000000000105'
const AG5_S_DAYS = '9001000000000000210', AG5_S_PROVIDER = '9001000000000000211'
const AG6 = '9001000000000000006', AG6_V1 = '9001000000000000106'
const AG6_S_TYPE = '9001000000000000212'
const AG7 = '9001000000000000007', AG7_V1 = '9001000000000000107'
const AG7_S_TYPE = '9001000000000000213', AG7_S_DAYS = '9001000000000000214', AG7_S_PROVIDER = '9001000000000000215'

/** 业务日期与约定天数：1/31 + 45 天 = 3/17（刻意跨月，证明按日历进位、且起算基准是业务日期）。 */
const BUSINESS_DATE = '2026-01-31'
const CREDIT_DAYS = 45
const EXPECTED_DUE_DATE = '2026-03-17'
/** 现款现结的到期日 = 业务日当天。 */
const CASH_DUE_DATE = BUSINESS_DATE
/** 调用方带来的到期日：一眼能认出的哨兵值，用来说明"有没有被本笔口径覆盖"。 */
const LEGACY_DUE_DATE = '2030-12-31'
/** 协议到期用的业务日期：AG7 的有效期在 2026-02-01 走完，这个日期落在之外。 */
const AFTER_EXPIRY_DATE = '2026-06-30'
/** 账期结算缺项被拒时后端返回的状态码（业务校验失败）。 */
const REJECTED = 400

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

/** 脚本**自身**的失败（≠ 被测系统行为不符）：用中文说清哪一步没拿到数据，顶层收口成 exit 2。 */
class ProbeFailure extends Error {}

const firstLine = (e) => (String(e?.stderr || e?.message || e).replace(/\r/g, '').split('\n')
  .map(l => l.trim()).filter(Boolean)[0]) || '(无输出)'

function sql(stmt) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 }).trim()
  } catch (e) {
    throw new ProbeFailure(`SQL 执行失败（脚本自身问题，不是被测系统的响应）: ${stmt.slice(0, 140)} —— ${firstLine(e)}`)
  }
}

/** 取单值查询的第一个数据行（sql.cjs 输出形如：表头 / 分隔线 / 数据 / "(N rows)"） */
function scalar(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').split('\n')
    .map(l => l.trim())
    .filter(l => l && !/^-+$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
  return lines.length > 1 ? lines[1] : '(空)'
}
const num = (stmt) => Number(scalar(stmt))

/** 雪花/大整数 id 守卫：只接受纯数字字符串（见文件头"id 一律字符串"）。 */
const isId = (v) => typeof v === 'string' && /^\d+$/.test(v.trim())

function mustId(v, label) {
  if (!isId(v)) {
    throw new ProbeFailure(`探针 id 不合法（${label} = ${JSON.stringify(v)}）：`
      + `上一步没返回合法 id，无法继续拼 SQL/URL`)
  }
  return v.trim()
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
  if (res.status === 401 && p !== '/auth/login') {
    throw new ProbeFailure(`会话已失效（HTTP 401 ${text.slice(0, 100)}）。可能原因：`
      + `① 有别的进程并发登录了同一账号 —— 后端 is-concurrent=false 会顶掉本脚本的会话；`
      + `② 后端鉴权链路本身异常。请确认没有并发 e2e 后重跑本脚本。`)
  }
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

const msgOf = (r) => r.json?.message || ''
const dataOf = (r) => (r.json && 'data' in r.json ? r.json.data : r.json)

/** 静默执行（清理阶段表可能还不存在，不想把 PSQL 堆栈喷到报告里） */
function sqlQuiet(stmt) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim()
  } catch { return '' }
}

function countQuiet(stmt) {
  const lines = sqlQuiet(stmt).replace(/\r/g, '').split('\n').map(l => l.trim())
  const hit = lines.find(l => /^\d+$/.test(l))
  return hit === undefined ? -1 : Number(hit)
}

/** 探针行数快照（清理前后各取一次，让"有没有清干净"变成可比较的数字）。 */
function probeRowCounts() {
  const inProbe = `SELECT id FROM agreement WHERE agreement_no LIKE '${PREFIX}%'`
  return {
    agreement: countQuiet(`SELECT count(*) FROM agreement WHERE agreement_no LIKE '${PREFIX}%'`),
    version: countQuiet(`SELECT count(*) FROM agreement_version WHERE agreement_id IN (${inProbe})`),
    setting: countQuiet(`SELECT count(*) FROM agreement_setting WHERE agreement_id IN (${inProbe})`),
    receivable: countQuiet(`SELECT count(*) FROM finance_receivable WHERE source_no LIKE '${PREFIX}%'`),
    payable: countQuiet(`SELECT count(*) FROM finance_payable WHERE source_no LIKE '${PREFIX}%'`),
  }
}

/** 硬删全部探针数据（软删会继续占用唯一键，第二次运行直接跑不通）。 */
function hardCleanProbes() {
  const byNo = `SELECT id FROM agreement WHERE agreement_no LIKE '${PREFIX}%'`
  for (const t of ['agreement_setting', 'agreement_version']) {
    sqlQuiet(`DELETE FROM ${t} WHERE agreement_id IN (${byNo})`)
  }
  sqlQuiet(`DELETE FROM agreement WHERE agreement_no LIKE '${PREFIX}%'`)
  sqlQuiet(`DELETE FROM finance_receivable WHERE source_no LIKE '${PREFIX}%'`)
  sqlQuiet(`DELETE FROM finance_payable WHERE source_no LIKE '${PREFIX}%'`)
}

/**
 * 落 7 份探针协议夹具 —— 覆盖「两层判定」的全部格子（§13.3 补充口径 3）。
 *
 * 直接写 agreement / agreement_version / agreement_setting 三张表（理由见文件头）：
 * 只有这三张表齐了，`AgreementRuntime` 才会在业务时点挑到那一版并读出设定。
 * `effective_from` 刻意设在业务日期**之前**；AG7 例外 —— 它的 `effective_to` 在业务日期之前，
 * 用来验证「**协议已到期**」是正常商业事实（走无协议模式、单据照开），而不是缺项。
 *
 * | 夹具 | 结算方式 | 天数 | 方向 | 期望 |
 * |---|---|---|---|---|
 * | AG1 | CREDIT | 45 | 甲 | 协议模式 ⇒ 业务日 + 45 |
 * | AG2 | CREDIT | **缺** | 甲 | 账期缺项 ⇒ **拒单** |
 * | AG3 | CREDIT | 45 | 乙 | 协议模式（应付侧）⇒ 业务日 + 45 |
 * | AG4 | CASH_SPOT | — | — | 本笔无账期 ⇒ 业务日，**且不报"未约定账期"** |
 * | AG5 | **未约定** | 45 | 甲 | 第 1 层判不了 ⇒ 无协议模式 ⇒ 现款现结（业务日） |
 * | AG6 | ROLLING | — | — | 本笔无账期 ⇒ 业务日 |
 * | AG7 | CREDIT | 45 | 甲 | **协议已到期** ⇒ 无协议模式 ⇒ 现款现结（业务日） |
 */
function stageAgreements() {
  sql(`INSERT INTO agreement
        (id, tenant_id, agreement_no, agreement_type, title, party_a_id, party_a_tenant_id,
         party_b_id, party_b_tenant_id, current_version_id, status, effective_from, effective_to,
         create_time, update_time, deleted)
       VALUES
        (${AG1}, 0, '${PREFIX}-AG-1', 'DISTRIBUTION', '${PREFIX} 探针协议（账期结算 45 天）',
         ${PARTY_A1}, ${T1}, ${PARTY_B2}, ${T2}, ${AG1_V1}, 1, '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG2}, 0, '${PREFIX}-AG-2', 'DISTRIBUTION', '${PREFIX} 探针协议（账期结算但没填天数）',
         ${PARTY_A1}, ${T1}, ${PARTY_B3}, ${T3}, ${AG2_V1}, 1, '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG3}, 0, '${PREFIX}-AG-3', 'DISTRIBUTION', '${PREFIX} 探针协议（账期由乙方提供）',
         ${PARTY_A1}, ${T1}, ${PARTY_B5}, ${T5}, ${AG3_V1}, 1, '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG4}, 0, '${PREFIX}-AG-4', 'DISTRIBUTION', '${PREFIX} 探针协议（现款现结，无账期）',
         ${PARTY_A1}, ${T1}, ${PARTY_B6}, ${T6}, ${AG4_V1}, 1, '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG5}, 0, '${PREFIX}-AG-5', 'DISTRIBUTION', '${PREFIX} 探针协议（没约定结算方式）',
         ${PARTY_A1}, ${T1}, ${PARTY_B7}, ${T7}, ${AG5_V1}, 1, '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG6}, 0, '${PREFIX}-AG-6', 'DISTRIBUTION', '${PREFIX} 探针协议（滚结，无账期）',
         ${PARTY_A1}, ${T1}, ${PARTY_B8}, ${T8}, ${AG6_V1}, 1, '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG7}, 0, '${PREFIX}-AG-7', 'DISTRIBUTION', '${PREFIX} 探针协议（已到期）',
         ${PARTY_A1}, ${T1}, ${PARTY_B9}, ${T9}, ${AG7_V1}, 1, '2026-01-01 00:00:00', '2026-02-01 00:00:00',
         now(), now(), 0)`)

  sql(`INSERT INTO agreement_version
        (id, agreement_id, version_no, snapshot_json, status,
         party_a_confirmed_by, party_a_confirmed_at, party_b_confirmed_by, party_b_confirmed_at,
         effective_from, effective_to, create_time, update_time, deleted)
       VALUES
        (${AG1_V1}, ${AG1}, 1, '{"probe":"${PREFIX}"}'::jsonb, 1, 1, now(), 1, now(),
         '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG2_V1}, ${AG2}, 1, '{"probe":"${PREFIX}"}'::jsonb, 1, 1, now(), 1, now(),
         '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG3_V1}, ${AG3}, 1, '{"probe":"${PREFIX}"}'::jsonb, 1, 1, now(), 1, now(),
         '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG4_V1}, ${AG4}, 1, '{"probe":"${PREFIX}"}'::jsonb, 1, 1, now(), 1, now(),
         '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG5_V1}, ${AG5}, 1, '{"probe":"${PREFIX}"}'::jsonb, 1, 1, now(), 1, now(),
         '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG6_V1}, ${AG6}, 1, '{"probe":"${PREFIX}"}'::jsonb, 1, 1, now(), 1, now(),
         '2026-01-01 00:00:00', NULL, now(), now(), 0),
        (${AG7_V1}, ${AG7}, 1, '{"probe":"${PREFIX}"}'::jsonb, 1, 1, now(), 1, now(),
         '2026-01-01 00:00:00', '2026-02-01 00:00:00', now(), now(), 0)`)

  // AG1：账期结算 + 45 天 + 「由甲方（租户 1）提供」⇒ 会话租户 1 作卖方（应收）时命中
  // AG2：账期结算 + 有方向、**没天数** ⇒ 条件必填缺失 ⇒ 应被拒单
  // AG3：账期结算 + 45 天 + 「由乙方（租户 T5）提供」⇒ 会话租户 1 作买方（应付）时命中
  // AG4：现款现结（**不给天数也不给方向**）⇒ 本笔无账期，压根不该去问那两项
  // AG5：**不写结算方式**，只写天数与方向 ⇒ 第 1 层判不了 ⇒ 走无协议模式
  // AG6：滚结 ⇒ 同样无账期
  // AG7：账期结算 + 45 天（有效期在业务日期之前走完）⇒ 协议已到期 ⇒ 走无协议模式
  sql(`INSERT INTO agreement_setting
        (id, tenant_id, agreement_id, version_id, setting_key, value_type, value_text, value_number,
         remark, create_time, update_time, deleted)
       VALUES
        (${AG1_S_TYPE}, 0, ${AG1}, ${AG1_V1}, 'SETTLEMENT_TYPE', 'ENUM', 'CREDIT', NULL,
         '${PREFIX} 探针：账期结算', now(), now(), 0),
        (${AG1_S_DAYS}, 0, ${AG1}, ${AG1_V1}, 'AR_CREDIT_DAYS', 'NUMBER', NULL, ${CREDIT_DAYS},
         '${PREFIX} 探针：账期 ${CREDIT_DAYS} 天', now(), now(), 0),
        (${AG1_S_PROVIDER}, 0, ${AG1}, ${AG1_V1}, 'AR_CREDIT_PROVIDER', 'ENUM', 'PARTY_A', NULL,
         '${PREFIX} 探针：由甲方提供账期', now(), now(), 0),
        (${AG2_S_TYPE}, 0, ${AG2}, ${AG2_V1}, 'SETTLEMENT_TYPE', 'ENUM', 'CREDIT', NULL,
         '${PREFIX} 探针：账期结算但没有天数', now(), now(), 0),
        (${AG2_S_PROVIDER}, 0, ${AG2}, ${AG2_V1}, 'AR_CREDIT_PROVIDER', 'ENUM', 'PARTY_A', NULL,
         '${PREFIX} 探针：只约定方向，不约定天数', now(), now(), 0),
        (${AG3_S_TYPE}, 0, ${AG3}, ${AG3_V1}, 'SETTLEMENT_TYPE', 'ENUM', 'CREDIT', NULL,
         '${PREFIX} 探针：账期结算', now(), now(), 0),
        (${AG3_S_DAYS}, 0, ${AG3}, ${AG3_V1}, 'AR_CREDIT_DAYS', 'NUMBER', NULL, ${CREDIT_DAYS},
         '${PREFIX} 探针：账期 ${CREDIT_DAYS} 天', now(), now(), 0),
        (${AG3_S_PROVIDER}, 0, ${AG3}, ${AG3_V1}, 'AR_CREDIT_PROVIDER', 'ENUM', 'PARTY_B', NULL,
         '${PREFIX} 探针：由乙方提供账期', now(), now(), 0),
        (${AG4_S_TYPE}, 0, ${AG4}, ${AG4_V1}, 'SETTLEMENT_TYPE', 'ENUM', 'CASH_SPOT', NULL,
         '${PREFIX} 探针：现款现货 · 现款现结', now(), now(), 0),
        (${AG5_S_DAYS}, 0, ${AG5}, ${AG5_V1}, 'AR_CREDIT_DAYS', 'NUMBER', NULL, ${CREDIT_DAYS},
         '${PREFIX} 探针：有天数但没约定结算方式', now(), now(), 0),
        (${AG5_S_PROVIDER}, 0, ${AG5}, ${AG5_V1}, 'AR_CREDIT_PROVIDER', 'ENUM', 'PARTY_A', NULL,
         '${PREFIX} 探针：由甲方提供账期', now(), now(), 0),
        (${AG6_S_TYPE}, 0, ${AG6}, ${AG6_V1}, 'SETTLEMENT_TYPE', 'ENUM', 'ROLLING', NULL,
         '${PREFIX} 探针：滚结', now(), now(), 0),
        (${AG7_S_TYPE}, 0, ${AG7}, ${AG7_V1}, 'SETTLEMENT_TYPE', 'ENUM', 'CREDIT', NULL,
         '${PREFIX} 探针：账期结算（协议已到期）', now(), now(), 0),
        (${AG7_S_DAYS}, 0, ${AG7}, ${AG7_V1}, 'AR_CREDIT_DAYS', 'NUMBER', NULL, ${CREDIT_DAYS},
         '${PREFIX} 探针：账期 ${CREDIT_DAYS} 天', now(), now(), 0),
        (${AG7_S_PROVIDER}, 0, ${AG7}, ${AG7_V1}, 'AR_CREDIT_PROVIDER', 'ENUM', 'PARTY_A', NULL,
         '${PREFIX} 探针：由甲方提供账期', now(), now(), 0)`)
}

const dueDateOfReceivable = (sourceNo) => scalar(
  `SELECT COALESCE(due_date::text, '(NULL)') FROM finance_receivable WHERE source_no = '${sourceNo}'`)
const dueDateOfPayable = (sourceNo) => scalar(
  `SELECT COALESCE(due_date::text, '(NULL)') FROM finance_payable WHERE source_no = '${sourceNo}'`)

// ══════════════════════ 主流程 ══════════════════════

;(async () => {
  console.log(`验证目标: ${BASE}`)
  const residueBefore = probeRowCounts()
  console.log(`清理前 ${PREFIX}* 探针行数: ${JSON.stringify(residueBefore)}`)
  try { hardCleanProbes() } catch { /* 表可能还不存在，交给下面 ⓪ 报出来 */ }
  console.log(`清理后 ${PREFIX}* 探针行数: ${JSON.stringify(probeRowCounts())}（应全为 0）`)

  // ══ ⓪ 前置 ══
  section('⓪ 前置：协议表 / 字段字典 / 财务集成权限 都已在库')
  const agTables = num(`SELECT count(*) FROM information_schema.tables
    WHERE table_schema='public' AND table_name IN
    ('agreement','agreement_version','agreement_setting','agreement_setting_def')`)
  const arDef = num(`SELECT count(*) FROM agreement_setting_def
    WHERE deleted=0 AND setting_key='AR_CREDIT_DAYS' AND consumer_point='AR_DUE_DATE'`)
  const providerDef = num(`SELECT count(*) FROM agreement_setting_def
    WHERE deleted=0 AND setting_key='AR_CREDIT_PROVIDER' AND consumer_point='AR_DUE_DATE'`)
  // 「结算方式」是账期天数的**前置字段**（§13.3 铁律①）：它在库里，第 1 层才判得了
  const settleDef = num(`SELECT count(*) FROM agreement_setting_def
    WHERE deleted=0 AND setting_key='SETTLEMENT_TYPE' AND consumer_point='AR_DUE_DATE'
      AND value_type='ENUM' AND required=false
      AND options='CASH_PREPAY,CASH_SPOT,CASH_ON_DELIVERY,CREDIT,ROLLING'`)
  const finPerm = num(`SELECT count(*) FROM sys_role_permission rp JOIN sys_permission p ON p.id=rp.permission_id
    WHERE rp.role_id=1 AND p.permission_code='finance:integration:create'`)

  ok('协议四张表齐备', agTables === 4, `实际 ${agTables} 张`)
  ok('字段字典有 AR_CREDIT_DAYS（消费方 = AR_DUE_DATE）', arDef === 1, `实际 ${arDef} 条`)
  ok('字段字典有 AR_CREDIT_PROVIDER（消费方 = AR_DUE_DATE）', providerDef === 1, `实际 ${providerDef} 条`)
  ok('字段字典有 SETTLEMENT_TYPE（五项扁平枚举 / 非必填 / 消费方 = AR_DUE_DATE）',
    settleDef === 1, `实际 ${settleDef} 条`)
  ok('超管角色已授 finance:integration:create（否则开不了单，本脚本无从验证）', finPerm === 1, `实际 ${finPerm} 条`)

  if (agTables !== 4 || arDef !== 1 || providerDef !== 1 || settleDef !== 1 || finPerm !== 1) {
    console.log(`\n!! 前置不成立：请先重启后端让 Flyway 执行到 V11.492.0（本脚本不代替迁移）。`)
    process.exit(2)
  }

  const adminToken = await login(ADMIN)
  console.log(`登录成功: ${ADMIN.u}（${ADMIN.t}，租户 ${T1}）`)

  const sourceNo = (kind, tag) => `${PREFIX}-${kind}-${tag}`
  const SRC_ID = 90010001 // 探针来源业务 id（小整数，不涉及精度）

  const postReceivable = (tag, { customerId, counterpartyTenantId, businessDate, dueDate }) => {
    const body = {
      sourceType: 'SALE_SHIPMENT', sourceId: SRC_ID, sourceNo: sourceNo('AR', tag),
      customerId, customerName: `${PREFIX} 探针客户`, amount: 100.00,
    }
    if (dueDate !== undefined) body.dueDate = dueDate
    if (businessDate !== undefined) body.businessDate = businessDate
    if (counterpartyTenantId !== undefined) body.counterpartyTenantId = counterpartyTenantId
    return req('POST', '/erp/finance/integration/receivable', { token: adminToken, body })
  }
  const postPayable = (tag, { supplierId, counterpartyTenantId, businessDate, dueDate }) => {
    const body = {
      sourceType: 'PURCHASE_RECEIPT', sourceId: SRC_ID, sourceNo: sourceNo('AP', tag),
      supplierId, supplierName: `${PREFIX} 探针供应商`, amount: 100.00,
    }
    if (dueDate !== undefined) body.dueDate = dueDate
    if (businessDate !== undefined) body.businessDate = businessDate
    if (counterpartyTenantId !== undefined) body.counterpartyTenantId = counterpartyTenantId
    return req('POST', '/erp/finance/integration/payable', { token: adminToken, body })
  }

  /** 夹具自检结果：拿到它才能在收尾处把"夹具不对"与"财务侧没接上"分开说清。 */
  let fixtureReadable = false

  try {
    // ══ 落夹具 ══
    section('准备：直接落"已生效 + 已约定"的探针协议（本脚本主题是**消费**，成立流程见 verify-agreement.cjs）')
    stageAgreements()
    ok('探针协议夹具已落库（7 份：协议模式 3 / 无账期 2 / 未约定结算方式 1 / 已到期 1）',
      probeRowCounts().agreement === 7 && probeRowCounts().version === 7 && probeRowCounts().setting === 15,
      JSON.stringify(probeRowCounts()))

    // 夹具自检：用**部署中**的协议模块读一遍这份夹具。
    // 为什么必须有这一步：它把两种完全不同的失败分开 ——
    //   ① 这里红 ⇒ 夹具本身落得不对（业务时点挑不到版本 / 设定读不出来），下面的红不算数；
    //   ② 这里绿而 ②③ 红 ⇒ 夹具没问题，是**财务侧的消费没接上**（多半是后端进程还没更新）。
    const fx = await req('GET', `/agreement/content/effective?agreementId=${AG1}`
      + `&businessTime=${encodeURIComponent(BUSINESS_DATE)}`, { token: adminToken })
    const fxd = dataOf(fx) || {}
    const fxSettle = (fxd.settings || []).find(s => s.settingKey === 'SETTLEMENT_TYPE') || {}
    const fxDays = (fxd.settings || []).find(s => s.settingKey === 'AR_CREDIT_DAYS') || {}
    const fxProvider = (fxd.settings || []).find(s => s.settingKey === 'AR_CREDIT_PROVIDER') || {}
    ok('夹具自检：部署中的 AgreementRuntime 按业务时点找到了这一版',
      fx.status === 200 && fxd.versionFound === true,
      `status=${fx.status} versionFound=${fxd.versionFound} ${msgOf(fx)}`)
    ok('夹具自检：读出的结算方式 = CREDIT（第 1 层的前置字段）',
      fxSettle.state === 'AGREED' && fxSettle.value === 'CREDIT',
      `state=${fxSettle.state} value=${JSON.stringify(fxSettle.value)}`)
    ok(`夹具自检：读出的账期天数 = ${CREDIT_DAYS}（不是"未约定"）`,
      fxDays.state === 'AGREED' && String(fxDays.value) === String(CREDIT_DAYS),
      `state=${fxDays.state} value=${JSON.stringify(fxDays.value)}`)
    ok('夹具自检：读出的账期方向 = PARTY_A',
      fxProvider.state === 'AGREED' && fxProvider.value === 'PARTY_A',
      `state=${fxProvider.state} value=${JSON.stringify(fxProvider.value)}`)
    fixtureReadable = fxd.versionFound === true && fxSettle.value === 'CREDIT'
      && fxDays.state === 'AGREED' && String(fxDays.value) === String(CREDIT_DAYS)

    // ══ ① 无适用协议 ⇒ 走无协议无特定客户模式（现款现结） ══
    section('① 无适用协议 ⇒ 走「无协议无特定客户模式」= 现款现结（到期日 = 业务日）')
    const r1 = await postReceivable('no-agreement', {
      customerId: PARTY_B2, counterpartyTenantId: T4, businessDate: BUSINESS_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok('开单成功（HTTP 200）', r1.status === 200, `status=${r1.status} ${msgOf(r1)}`)
    ok(`到期日 = 业务日 ${CASH_DUE_DATE}（现款现结、当天结清）`,
      dueDateOfReceivable(sourceNo('AR', 'no-agreement')) === CASH_DUE_DATE,
      `实际 ${dueDateOfReceivable(sourceNo('AR', 'no-agreement'))}（期望 ${CASH_DUE_DATE}）`)
    ok('**不再**是调用方带来的 30 天哨兵值（平台硬编码的默认账期已移除）',
      dueDateOfReceivable(sourceNo('AR', 'no-agreement')) !== LEGACY_DUE_DATE,
      `实际 ${dueDateOfReceivable(sourceNo('AR', 'no-agreement'))}`)

    // ══ ② 有协议 + 已约定 ⇒ 业务日期 + 天数 ══
    section('② 有协议且约定 45 天 ⇒ **应收到期日 = 业务日期 + 45**（1/31 + 45 = 3/17，跨月）')
    const r2 = await postReceivable('agreed', {
      customerId: PARTY_B2, counterpartyTenantId: T2, businessDate: BUSINESS_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok('开单成功（HTTP 200）', r2.status === 200, `status=${r2.status} ${msgOf(r2)}`)
    const ar2 = dueDateOfReceivable(sourceNo('AR', 'agreed'))
    ok(`到期日 = 业务日期 + 约定天数 = ${EXPECTED_DUE_DATE}`, ar2 === EXPECTED_DUE_DATE,
      `实际 ${ar2}（期望 ${EXPECTED_DUE_DATE}）`)
    ok('**不是**"今天 + 天数"，也**不是**调用方带来的哨兵值',
      ar2 !== LEGACY_DUE_DATE && ar2 === EXPECTED_DUE_DATE,
      `哨兵=${LEGACY_DUE_DATE}, 实际=${ar2}`)

    // ══ ③ 应付侧：方向正确时同样命中 ══
    section('③ 应付侧：协议约定由**对方**（供应商）提供账期 ⇒ 应付款到期日 = 业务日期 + 45')
    const r3 = await postPayable('agreed', {
      supplierId: PARTY_B5, counterpartyTenantId: T5, businessDate: BUSINESS_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok('开单成功（HTTP 200）', r3.status === 200, `status=${r3.status} ${msgOf(r3)}`)
    const ap3 = dueDateOfPayable(sourceNo('AP', 'agreed'))
    ok(`应付款到期日 = ${EXPECTED_DUE_DATE}`, ap3 === EXPECTED_DUE_DATE,
      `实际 ${ap3}（期望 ${EXPECTED_DUE_DATE}）`)

    // ══ ④ 账期有向：方向对不上 ⇒ 账期口径说不清 ⇒ 拒单 ══
    section('④ 账期**有向**：协议约定由甲方提供账期，本笔应付的债权方却在乙方 ⇒ 单据提交被拒')
    const r4 = await postPayable('wrong-direction', {
      supplierId: PARTY_B2, counterpartyTenantId: T2, businessDate: BUSINESS_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok(`提交被拒（HTTP ${REJECTED}）`, r4.status === REJECTED, `status=${r4.status} ${msgOf(r4)}`)
    ok('文案说清是「提交不了」并点出方向问题',
      /不能提交/.test(msgOf(r4)) && /方向/.test(msgOf(r4)), `message=${msgOf(r4)}`)
    ok('应付账款**没有落库**（拒单就要真的什么都没记）',
      num(`SELECT count(*) FROM finance_payable WHERE source_no='${sourceNo('AP', 'wrong-direction')}'`) === 0,
      `实际 ${num(`SELECT count(*) FROM finance_payable WHERE source_no='${sourceNo('AP', 'wrong-direction')}'`)} 行`)

    // ══ ⑤-A 第 1 层：没约定结算方式 ⇒ 走无协议模式（单据照开） ══
    section('⑤-A **没约定结算方式** ⇒ 走无协议无特定客户模式，单据照开、到期日 = 业务日')
    const r5a = await postReceivable('no-settlement-type', {
      customerId: PARTY_B7, counterpartyTenantId: T7, businessDate: BUSINESS_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok('单据照常生成，**不阻断**（HTTP 200）', r5a.status === 200, `status=${r5a.status} ${msgOf(r5a)}`)
    ok('应收账款确实落库了（不是在报错前就回滚）',
      num(`SELECT count(*) FROM finance_receivable WHERE source_no='${sourceNo('AR', 'no-settlement-type')}'`) === 1)
    ok(`到期日 = 业务日 ${CASH_DUE_DATE}（现款现结），**不落**哨兵值`,
      dueDateOfReceivable(sourceNo('AR', 'no-settlement-type')) === CASH_DUE_DATE,
      `实际 ${dueDateOfReceivable(sourceNo('AR', 'no-settlement-type'))}`)

    // ══ ⑤-B 第 2 层：账期结算但缺天数 ⇒ 拒单 ══
    section('⑤-B 约定「账期结算」却**没填账期天数** ⇒ 条件必填缺失 ⇒ 单据提交被拒')
    const r5b = await postReceivable('credit-without-days', {
      customerId: PARTY_B3, counterpartyTenantId: T3, businessDate: BUSINESS_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok(`提交被拒（HTTP ${REJECTED}）`, r5b.status === REJECTED, `status=${r5b.status} ${msgOf(r5b)}`)
    ok('文案点出缺的是「账期天数」',
      /不能提交/.test(msgOf(r5b)) && /账期天数/.test(msgOf(r5b)), `message=${msgOf(r5b)}`)
    ok('应收账款**没有落库**（既不是留空挂人工，也不是回落到默认天数）',
      num(`SELECT count(*) FROM finance_receivable WHERE source_no='${sourceNo('AR', 'credit-without-days')}'`) === 0,
      `实际 ${num(`SELECT count(*) FROM finance_receivable WHERE source_no='${sourceNo('AR', 'credit-without-days')}'`)} 行`)

    // ══ ⑥ 身份信息不足 ⇒ 现款现结兜底（不猜主体、不猜租户） ══
    section('⑥ 调用方未提供业务日期 / 对方租户 / 主体 id ⇒ 判不了就走现款现结（不猜）')
    await postReceivable('no-business-date', {
      customerId: PARTY_B2, counterpartyTenantId: T2, dueDate: LEGACY_DUE_DATE,
    })
    ok('未提供业务日期 ⇒ 现款现结也算不出日期 ⇒ 沿用调用方带来的值',
      dueDateOfReceivable(sourceNo('AR', 'no-business-date')) === LEGACY_DUE_DATE,
      `实际 ${dueDateOfReceivable(sourceNo('AR', 'no-business-date'))}`)

    await postReceivable('no-counterparty-tenant', {
      customerId: PARTY_B2, businessDate: BUSINESS_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok(`未提供对方租户 ⇒ 判不了跨/内租户 ⇒ 现款现结，到期日 = 业务日 ${CASH_DUE_DATE}`,
      dueDateOfReceivable(sourceNo('AR', 'no-counterparty-tenant')) === CASH_DUE_DATE,
      `实际 ${dueDateOfReceivable(sourceNo('AR', 'no-counterparty-tenant'))}`)

    await postReceivable('non-numeric-party', {
      customerId: `${PREFIX}-客户甲（只有名字）`, counterpartyTenantId: T2,
      businessDate: BUSINESS_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok(`往来方只有名字、拿不到主体 id ⇒ 查不了协议 ⇒ 现款现结，到期日 = 业务日 ${CASH_DUE_DATE}（不用名字去猜主体）`,
      dueDateOfReceivable(sourceNo('AR', 'non-numeric-party')) === CASH_DUE_DATE,
      `实际 ${dueDateOfReceivable(sourceNo('AR', 'non-numeric-party'))}`)

    // ══ ⑦ 反向对照：账期缺项在任何业务日期下都被拒（不是"日期选得好"） ══
    section('⑦ 反向对照：账期结算缺天数在任何业务日期下都提交不了（不是"日期挑得巧"）')
    const r7 = await postReceivable('credit-without-days-other-date', {
      customerId: PARTY_B3, counterpartyTenantId: T3,
      businessDate: AFTER_EXPIRY_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok(`换一个业务日期仍被拒（HTTP ${REJECTED}）`, r7.status === REJECTED,
      `status=${r7.status} ${msgOf(r7)}`)

    // ══ ⑧ 协议已到期 ⇒ 正常商业事实，单据照开（走无协议模式） ══
    section('⑧ **协议已到期** ⇒ 与「从未有协议」同处理（走无协议模式），单据照开、到期日 = 业务日')
    const r8 = await postReceivable('expired-agreement', {
      customerId: PARTY_B9, counterpartyTenantId: T9,
      businessDate: AFTER_EXPIRY_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok('单据照常生成，**不阻断**（HTTP 200）—— 到期是正常商业事实，不是缺项', r8.status === 200,
      `status=${r8.status} ${msgOf(r8)}`)
    ok(`到期日 = 业务日 ${AFTER_EXPIRY_DATE}（现款现结）`,
      dueDateOfReceivable(sourceNo('AR', 'expired-agreement')) === AFTER_EXPIRY_DATE,
      `实际 ${dueDateOfReceivable(sourceNo('AR', 'expired-agreement'))}（期望 ${AFTER_EXPIRY_DATE}）`)

    // ══ ⑨ 现金 / 滚结 ⇒ 本笔无账期，且**不报"未约定账期"** ══
    section('⑨ 结算方式 = 现款现结 / 滚结 ⇒ 单据正常提交、到期日 = 业务日，**不报「未约定账期」**')
    const r9a = await postReceivable('cash-spot', {
      customerId: PARTY_B6, counterpartyTenantId: T6, businessDate: BUSINESS_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok('约定现款现结 ⇒ 单据正常提交（HTTP 200）', r9a.status === 200, `status=${r9a.status} ${msgOf(r9a)}`)
    ok(`到期日 = 业务日 ${CASH_DUE_DATE}（钱货两清、当天结清）`,
      dueDateOfReceivable(sourceNo('AR', 'cash-spot')) === CASH_DUE_DATE,
      `实际 ${dueDateOfReceivable(sourceNo('AR', 'cash-spot'))}`)
    ok('**且没有**因为「协议没填账期天数」被拦下（这正是用户点名的老毛病）',
      r9a.status === 200 && !/账期/.test(msgOf(r9a)), `message=${msgOf(r9a)}`)

    const r9b = await postPayable('rolling', {
      supplierId: PARTY_B8, counterpartyTenantId: T8, businessDate: BUSINESS_DATE, dueDate: LEGACY_DUE_DATE,
    })
    ok('约定滚结 ⇒ 单据正常提交（HTTP 200）', r9b.status === 200, `status=${r9b.status} ${msgOf(r9b)}`)
    ok(`到期日 = 业务日 ${CASH_DUE_DATE}`,
      dueDateOfPayable(sourceNo('AP', 'rolling')) === CASH_DUE_DATE,
      `实际 ${dueDateOfPayable(sourceNo('AP', 'rolling'))}`)

    if (fail > 0 && fixtureReadable && ar2 !== EXPECTED_DUE_DATE) {
      console.log('\n⚠️ 提示：探针夹具本身是好的（部署中的 AgreementRuntime 已能按业务时点读出结算方式与账期 45 天），'
        + '但财务侧生成的到期日仍是调用方带来的哨兵值'
        + `（${LEGACY_DUE_DATE}）—— 说明**运行中的后端进程还是本次改动之前的代码**。`
        + '请在重启后端后复跑本脚本。这不是脚本断言被放宽的理由。')
    } else if (fail > 0 && !fixtureReadable) {
      console.log('\n⚠️ 提示：**夹具自检本身没过**（部署中的 AgreementRuntime 没能按业务时点读出这份夹具）。'
        + '上面 ②~⑨ 的红不能算作"财务侧没接上"，请先排查夹具与协议侧读取。')
    }
  } finally {
    console.log('\n—— 现场还原（探针数据一律硬删）——')
    hardCleanProbes()
    const left = probeRowCounts()
    ok('【自清理】探针协议/版本/设定与探针应收应付已**硬删**',
      left.agreement === 0 && left.version === 0 && left.setting === 0
      && left.receivable === 0 && left.payable === 0,
      `库内探针行数=${JSON.stringify(left)}`)
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail ? 1 : 0)
})().catch(e => {
  console.error(`脚本异常: ${e instanceof ProbeFailure ? e.message : firstLine(e)}`)
  try { hardCleanProbes() } catch { /* 尽力而为 */ }
  process.exit(2)
})
