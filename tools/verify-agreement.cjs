#!/usr/bin/env node
/**
 * 协议模块（阶段 A）实机两向验证 —— 2026-09-22
 *
 * 【要钉死的是什么】协议层三条不变量（已生效版本只读 · 双签缺一不可 · 必填条款没选完不许生效）
 * 与两条裁定（裁定⑤ 平台码归「系统」/ 裁定⑥ 主档系统级、可见性按两端判定）
 * **必须落在真机上可观察的行为上**，而不是只写在注释里。
 *
 * 【为什么必须真机验证裁定⑥】协议全部 12 张表登记在 `MyBatisPlusConfig.IGNORE_TENANT_TABLES`
 * ⇒ 租户拦截器在它们上面**不生效**，"看不到别人的协议"完全靠自己写对。
 * 因此本脚本第 ⑥ 组断言：**会话租户不是任一端时，看不到那份协议，而该行在库里确实存在**
 * —— 只有真机能证明"可见性条件真的被应用了"，而不是"刚好库里没数据"。
 *
 * 【断言清单】
 *   ⓪ 前置：表 ≥15 张、权限码 ≥26 条且**全部**已授超管、模块名册与 5 行菜单在（分挂设置/系统两棵树）
 *      ⚠️ 规模断言用**下界**：协议是分批发行的，写死等号会让"又长了一批"伪装成"前置失败"（已连踩两次）
 *   ① 字典表**没有** default_option 列（防止后人"顺手"把平台默认值加回来，§3.4.4d1）
 *   ② 必填条款没选完 → activate 被拒，且文案指出缺的是哪一项
 *   ③ 只一方确认 → activate 被拒（直接对应"不能单方改"）
 *   ④ 选齐 + 双签齐 → activate 成功：新版 ACTIVE、旧版 SUPERSEDED、主档指向新版本
 *   ⑤ 对已 ACTIVE 版本写条款/快照 → 被拒（HTTP 400），读库核对快照与条款**逐字未变**
 *      ⚠️ 载荷必须**本身合法**（用该 term_code 真实存在的选项）：否则会先被"选项不在字典里"拦下，
 *      拒绝理由不是"已生效不可写"，这条不变量就没被真正证明（旧版就是这么写的，等于白验）。
 *   ⑥ 一方拒绝 → 该版本 REJECTED，而**原 ACTIVE 版本仍是 ACTIVE、主档仍指向它**（变更谈成前交易照常）
 *      并核对旧版本行除 status 外逐字段一致（变更不追溯 = 不回写任何既有数据）
 *   ⑦ 第三方会话查不到别人的协议（detail 404 + 列表不含），而该行在库里存在
 *   ⑧ 平台侧字典维护码归「系统」模块、租户侧码归「协议」模块（裁定⑤，按最长前缀优先实测）
 *
 * 【探针数据一律**硬删**】本仓 CRM-BREAK-03 教训：软删会占唯一键，脚本第二次跑不通。
 *   探针：字典选项（term_code 以 `E2E_AG_` 开头）、协议（title 以 `E2E ` 开头，及其版本/条款）、
 *         以及**临时授予租户 2 角色的全部协议权限码**（结束按原值还原并逐项核对）。
 *   ⚠️ 清理**按 title/term_code 前缀扫库删**，不只按内存里记得的 id：
 *      上次运行若被 kill / 超时，`probeAgreements` 随进程消失，那些行会永远留在库里
 *      （2026-09-22 实测留下过 4 行 agreement + 4 行 agreement_version，而脚本还报"残留=[]"）。
 *      清理前后各打一次行数快照，残留不再静默。
 *
 * 【id 一律字符串，且拼 SQL/URL 前必须过 mustId】雪花 id 约 2.1e18 > MAX_SAFE_INTEGER(9.007e15)，
 *   `Number()` / `JSON.parse` 都会静默截断末尾几位。两处实测过的坑见 `isId` 上方的长注释。
 *
 * 【不 sleep】角色授权走 `/api/role/{id}/permissions` 接口，它内部会
 *   `clearUserPermissionCache` 清掉该角色下用户的权限缓存，改完立刻生效。
 *
 * ⚠️ 【不要并发跑】后端 sa-token 是 `is-concurrent=false`：同一个账号再次登录会**顶掉旧会话**。
 *   别的进程（并发的 e2e 脚本 / 有人手动登录）一登录 admin，本脚本后面几十条断言就整齐地红成
 *   401「请先登录」。脚本已把这种情形识别成环境冲突（中文报错 + exit 2），而不是伪装成系统缺陷。
 *
 * 用法：node tools/verify-agreement.cjs
 *   前置：后端在 5655 运行、且已执行 V11.486.0 ~ V11.489.0（未执行时脚本会在 ⓪ 明确报出）。
 *   退出码：0 = 全绿；1 = 有 FAIL；2 = 前置不成立或脚本异常。
 */
const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')
const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`

const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }
const TENANT2 = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2' }

const T1 = 1
const T2 = 2
/** 用于构造"会话租户不是任一端"的场景（只存在于探针数据里，不与任何真实租户冲突） */
const TX = 990001
/** 探针字典的类别前缀（清理时按它硬删） */
const TERM_PREFIX = 'E2E_AG_'
const TERM_REQUIRED = TERM_PREFIX + 'FREIGHT'
const TERM_OPTIONAL = TERM_PREFIX + 'CANCEL'
const CODES = [
  'agreement:list', 'agreement:view', 'agreement:create', 'agreement:update', 'agreement:delete',
  'agreement:version:create', 'agreement:version:confirm', 'agreement:version:activate',
  'agreement:term-option:list', 'agreement:platform:term-option:manage',
]

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

/** 脚本**自身**的失败（≠ 被测系统行为不符）：用中文说清是哪一步没拿到数据，顶层收口成 exit 2。 */
class ProbeFailure extends Error {}

/** 从子进程异常里只取一行可读信息：PSQL 的 Java 栈对看报告的人没有价值，只会把真实失败掩盖掉 */
const firstLine = (e) => (String(e?.stderr || e?.message || e).replace(/\r/g, '').split('\n')
  .map(l => l.trim()).filter(Boolean)[0]) || '(无输出)'

function sql(stmt) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 }).trim()
  } catch (e) {
    // 不让 `Command failed: ... PSQLException` 直接冒出来（见文件头"空 id 入 SQL"那条）
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
/**
 * 雪花 id 守卫：**只接受纯数字字符串**（`typeof === 'string'` 是硬条件，见下）。
 *
 * ⚠️ 本脚本里的 id **一律用字符串，绝不过 `Number()`** —— 雪花 id 约 2.1e18，
 *    远超 `Number.MAX_SAFE_INTEGER`(9.007e15)，转换会**静默截断末尾几位**。
 *    症状有两级：① 「新建成功却查不到它自己的版本」（`WHERE agreement_id=<被截断的 id>` 匹配 0 行）；
 *    ② 更隐蔽的是**自清理也用被截断的 id 去删 ⇒ 删不掉，而残留自检同样用错 id ⇒ 报「残留=[]」**，
 *    于是探针垃圾静默留在库里（2026-09-22 实测踩到，协议表留下 2 行）。
 *    本仓已有同类教训：前端雪花 ID 精度丢失，id 一律改 string。
 *
 * ⚠️ 两处**已实测**的精度坑，后人别再踩：
 *    ① **URL / 路径变量**：`/api/role/2099000000000009000/permissions` 里的 2099000000000009000
 *       是 `Number(2099000000000009031)` 的结果 ⇒ 后端按"角色不存在"抛 RuntimeException，
 *       被兜底 advice 吞成 **500「系统异常，请稍后重试」**，把"脚本传错 id"伪装成"服务故障"。
 *       同一坑还会让 `sys_role_permission` 的"还原"写入一堆**不存在的 permission_id**，
 *       把租户 2 角色原有的权限删掉（探针污染真实数据）。
 *    ② **JSON 数字**：若后端把雪花 id 当 JSON number 返回，`JSON.parse` 那一刻就已经丢精度，
 *       再 `String()` 也救不回来。所以这里的守卫要求原始类型必须是 `string` ——
 *       万一后端改成输出 number，本脚本会**明确报"id 不合法"**，而不是拿一个看着像 id 的错值继续跑。
 */
const isId = (v) => typeof v === 'string' && /^\d+$/.test(v.trim())

/**
 * 拼 SQL / 拼 URL 之前的统一 id 守卫。
 *
 * 为什么必须有它：id 为空（如 `String(undefined)` → `"undefined"`，或 POST 失败后 `"(空)"`）时
 * 直接插进 `WHERE id=` 会得到 `WHERE id=` 这种**语法错误**，sql.cjs 会把整段 PSQLException 栈喷出来
 * —— 真实失败（"上一步没拿到 id"）反被底层报错掩盖。守卫把它换成人能读懂的中文失败。
 */
function mustId(v, label) {
  if (!isId(v)) {
    throw new ProbeFailure(`探针 id 不合法（${label} = ${JSON.stringify(v)}）：`
      + `上一步没返回合法 id，无法继续拼 SQL/URL（不再让底层 PSQL 报语法错误）`)
  }
  return v.trim()
}

/**
 * 从接口回执里取探针 id。
 *
 * ⚠️ 刻意**不做 `String(v)` 兜底**：后端必须把雪花 id 当 **JSON 字符串**返回。
 *    若它哪天改成返回 number，`JSON.parse` 那一刻精度就已经丢了，
 *    `String()` 只会把一个错值伪装成"看着合法的 id"继续往下跑。
 *    所以这里取不到字符串就返回 null，由调用方以中文 FAIL / ProbeFailure 明确报出来。
 */
function replyId(r) {
  const v = dataOf(r)
  return isId(v) ? v.trim() : null
}

/**
 * 比较两个"纯数字字符串"的大小（还原核对用）。
 * 走**长度 + 字典序**而不是 `Number()`：雪花 id 过 Number 会被截断，
 * 那样"还原前后的权限 id 列表"比较就失去意义（两边同时被截断，错值也能比对相等）。
 */
function cmpId(a, b) {
  if (a.length !== b.length) return a.length - b.length
  return a < b ? -1 : a > b ? 1 : 0
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
  // 会话被顶下线：后端 sa-token 是 is-concurrent=false + is-login-kick=true，同账号再登录会踢掉旧会话。
  // 若别的进程正并发用 admin 登录（并发跑本脚本、别的 e2e 脚本、有人手动登录），本脚本的 token 会立刻失效，
  // 后面几十条断言整齐地红成 401「请先登录」—— 那是**环境冲突**，不是被测系统的缺陷。
  // 与其喷一屏误导性的 FAIL，不如在这里用中文说清（这不改任何断言口径，只是把环境问题如实报出来）。
  if (res.status === 401 && p !== '/auth/login') {
    throw new ProbeFailure(`会话已失效（HTTP 401 ${text.slice(0, 100)}）。可能原因：`
      + `① 有别的进程并发登录了同一账号 —— 后端 is-concurrent=false 会顶掉本脚本的会话（实测踩过）；`
      + `② 后端鉴权链路本身就异常。请确认没有并发 e2e 后重跑本脚本。`)
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

// ══════════════════════ 探针数据的清理 ══════════════════════

/**
 * 探针协议的标题前缀。
 *
 * ⚠️ 清理**必须按它扫库**，不能只按内存里记得的 id 删：
 *   上一次运行若被 kill / 超时（进程没走到 finally），`probeAgreements` 随进程一起消失，
 *   而那些行还留在库里 —— 下次运行既看不见也删不掉，日积月累就是"历史残留"。
 *   按标题前缀扫能连**上一次异常退出**留下的行一起清掉（2026-09-22 实测残留过 `E2E 精度探针`）。
 */
const TITLE_PREFIX = 'E2E '

/** 探针协议 id 集合（本次运行创建的；核对残留用，不作为唯一删除依据） */
const probeAgreements = []

/** 静默执行（清理阶段表可能还不存在，不想把 PSQL 堆栈喷到报告里） */
function sqlQuiet(stmt) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim()
  } catch { return '' }
}

/** 静默取 count(*)（表不存在 / 语句失败一律返回 -1，由调用方决定怎么报） */
function countQuiet(stmt) {
  const lines = sqlQuiet(stmt).replace(/\r/g, '').split('\n').map(l => l.trim())
  const hit = lines.find(l => /^\d+$/.test(l))
  return hit === undefined ? -1 : Number(hit)
}

/** 探针协议的行数快照（清理前后各取一次，让"有没有清干净"变成可比较的数字） */
function probeRowCounts() {
  const inProbe = `SELECT id FROM agreement WHERE title LIKE '${TITLE_PREFIX}%'`
  return {
    agreement: countQuiet(`SELECT count(*) FROM agreement WHERE title LIKE '${TITLE_PREFIX}%'`),
    version: countQuiet(`SELECT count(*) FROM agreement_version WHERE agreement_id IN (${inProbe})`),
    term: countQuiet(`SELECT count(*) FROM agreement_term WHERE agreement_id IN (${inProbe})`),
    option: countQuiet(`SELECT count(*) FROM agreement_term_option WHERE term_code LIKE '${TERM_PREFIX}%'`),
  }
}

/**
 * 硬删全部探针数据。
 *
 * ⚠️ **一律硬删（DELETE）**，不用软删：软删会继续占用唯一键
 *   （`agreement_term_option` 的 `(term_code, option_code)`、`agreement_version` 的"一协议一 ACTIVE"），
 *   于是第二次运行直接跑不通 —— 本仓 CRM-BREAK-03 的教训。
 */
function hardCleanProbes() {
  const byTitle = `SELECT id FROM agreement WHERE title LIKE '${TITLE_PREFIX}%'`
  // 内容层各表先删（无外键，但保持"先子后父"的可读顺序），再删主档
  for (const t of ['agreement_fulfillment_mode', 'agreement_narrative', 'agreement_setting',
    'agreement_term', 'agreement_version']) {
    sqlQuiet(`DELETE FROM ${t} WHERE agreement_id IN (${byTitle})`)
  }
  sqlQuiet(`DELETE FROM agreement WHERE title LIKE '${TITLE_PREFIX}%'`)
  // 探针字典：term_code 前缀是 `E2E_AG_`，与任何真实条款不冲突；含历史软删行一并清
  sqlQuiet(`DELETE FROM agreement_term_option WHERE term_code LIKE '${TERM_PREFIX}%'`)
  sqlQuiet(`DELETE FROM agreement_term WHERE term_code LIKE '${TERM_PREFIX}%'`)
}

/** 某探针协议是否还在库里（清理核对；id 必须是合法字符串，否则直接抛中文错误） */
const probeExists = (id) => num(`SELECT count(*) FROM agreement WHERE id = ${mustId(id, 'probeExists')}`)

// ══════════════════════ 主流程 ══════════════════════

;(async () => {
  console.log(`验证目标: ${BASE}`)
  // 先清一次上次可能的残留（尤其是唯一键 (term_code, option_code) 上的探针字典行），
  // 并把"清理前后各几行"打出来 —— 残留不再静默（旧版只删本次记得的 id，上次崩溃留下的行永远清不掉）
  const residueBefore = probeRowCounts()
  console.log(`清理前 agreement* 探针行数: ${JSON.stringify(residueBefore)}`)
  try { hardCleanProbes() } catch { /* 表可能还不存在，交给下面 ⓪ 报出来 */ }
  const residueAfter = probeRowCounts()
  console.log(`清理后 agreement* 探针行数: ${JSON.stringify(residueAfter)}（应全为 0）`)

  // ══ ⓪ 前置 ══
  section('⓪ 前置：表 / 权限码 / 模块名册 / 菜单 都已在库')
  const tableCount = num(`SELECT count(*) FROM information_schema.tables
    WHERE table_schema='public' AND table_name IN
    ('agreement','agreement_version','agreement_term','agreement_term_option',
     'agreement_setting','agreement_setting_def','agreement_narrative','agreement_fulfillment_mode',
     'agreement_template','agreement_template_term','agreement_template_setting','agreement_template_narrative',
     'agreement_invite','agreement_signature','agreement_termination')`)
  const codeCount = num(`SELECT count(*) FROM sys_permission
    WHERE deleted=0 AND permission_code LIKE 'agreement:%'`)
  const granted = num(`SELECT count(*) FROM sys_role_permission rp JOIN sys_permission p ON p.id=rp.permission_id
    WHERE rp.role_id=1 AND p.permission_code LIKE 'agreement:%'`)
  const moduleRow = num(`SELECT count(*) FROM sys_module WHERE module_code='agreement' AND deleted=0`)
  const menuRows = num(`SELECT count(*) FROM sys_menu
    WHERE id IN (61208, 81016, 61308, 62506, 62507) AND deleted=0`)

  // ⚠️ 规模断言一律用**下界**（>= 基线），不要写死等号。
  //    教训（2026-09-22 连踩两次）：协议模块是分批发行的，每加一批表/码，写死总数的断言就会红，
  //    而它红的原因**不是系统退化**、只是"又长了一批"—— 这种红会训练人忽略真正的前置失败。
  //    真正该钉死的不变量是"**所有 agreement: 码都授给了超管**"（`granted === codeCount`），
  //    它不会随规模增长而失效，且一旦漏授就是真故障。
  const TABLES_BASE = 15   // V11.486.0 地基 4 + V11.488.0 内容层 8 + V11.490.0 生命周期 3
  const CODES_BASE = 27    // V11.487.0 十 + V11.489.0 十 + V11.491.0 六 + V11.493.0 一（平台合规抽查读）
  ok(`协议表已建齐（下界 ${TABLES_BASE}：地基 4 + 内容层 8 + 生命周期 3）`,
    tableCount >= TABLES_BASE, `实际 ${tableCount} 张`)
  ok(`agreement: 权限码不少于 ${CODES_BASE} 条`,
    codeCount >= CODES_BASE, `实际 ${codeCount} 条`)
  ok('**全部** agreement: 码都已授予超管角色(role_id=1)（漏授 = 平台管理员自己点不动）',
    granted === codeCount, `已授 ${granted} / 共 ${codeCount} 条`)
  ok('sys_module 名册已有「协议」（租户级模块）', moduleRow === 1, `实际 ${moduleRow} 条`)
  ok('协议菜单 5 行已落库（分挂设置/系统两棵树，tenant_id 必须为 0）', menuRows === 5, `实际 ${menuRows} 行`)

  if (tableCount < TABLES_BASE || codeCount < CODES_BASE || granted !== codeCount
      || moduleRow !== 1 || menuRows !== 5) {
    console.log(`\n!! 前置不成立：请先重启后端让 Flyway 执行到 V11.493.0（本脚本不代替迁移）。`)
    process.exit(2)
  }

  // 供后续断言使用的基础数据
  const adminToken = await login(ADMIN)
  const t2Token = await login(TENANT2)
  console.log(`登录成功: admin（系统租户 ${T1}） / ${TENANT2.u}（租户 ${T2}）`)

  // `biz_party.id` 现存都是**小整数**（实测 2 / 3），这里**刻意保留 JSON 数字**：
  // 后端 DTO 的 partyAId / partyBId 是 Long，真机已验证接受 JSON number（新建探针协议返回 200）。
  // 但仍加"够不够小"的守卫 —— 万一往来单位哪天也换成雪花 id，
  // Number 会静默截断成另一个主体（等于拿别人的身份签约），宁可在这里明确报错。
  const smallId = (stmt, label) => {
    const raw = scalar(stmt).trim()
    if (!/^\d+$/.test(raw) || raw.length > 15) {
      throw new ProbeFailure(`${label} 不是小整数（${raw}）：本脚本按 JSON 数字传 partyId，`
        + `换成雪花 id 会丢精度截断成别的法人，必须先把口径改成字符串再跑`)
    }
    return Number(raw)
  }
  const partyOfT1 = smallId(
    `SELECT id FROM biz_party WHERE tenant_id=${T1} AND deleted=0 ORDER BY id LIMIT 1`, '甲方 party id')
  // ⚠️ 两端必须是**两个不同的主体**，不要图省事复用同一个 id：
  //   `party` 是**系统级主体**（公司/个人），两端同一个 party = 同一法人跟自己签约，
  //   `AgreementServiceImpl#validateParties` 会以「甲方与乙方不能是同一个主体」拒绝。
  //   （本文件最初两端复用同一个 id，该校验加上后整段准备流程即红 —— 这是真实踩过的。）
  // ⚠️ 也别把两端写成**同一个租户**：同租户内两个主体之间的约定属购销框架/合同（§12.6 边界表），
  //   本模块只处理"租户之间"与"租户与平台"，两端同租户会被拒。
  // 注：`biz_party` 现存行都属租户 1（主体升系统级是 §5.3 的迁移工作，尚未做），
  //   故本脚本**借用**租户 1 的两个 id 去构造跨租户探针 —— 这是探针构造手法，
  //   不代表真实数据里的归属关系。
  const partyOfT1B = smallId(
    `SELECT id FROM biz_party WHERE tenant_id=${T1} AND deleted=0 ORDER BY id LIMIT 1 OFFSET 1`, '乙方 party id')
  if (!Number.isFinite(partyOfT1B) || partyOfT1B <= 0 || partyOfT1B === partyOfT1) {
    throw new Error('前置不成立：租户 1 的 biz_party 不足 2 行（协议两端必须是两个不同主体）')
  }
  console.log(`探针主体：租户 ${T1} 的往来单位 id=${partyOfT1}（甲方） / id=${partyOfT1B}（乙方）`)

  // 角色授权探针：先存原值。
  // ⚠️ t2Role 是**雪花 id**（实测 2099000000000009031），必须原样当字符串用：
  //   过 `Number()` 会变 2099000000000009000 ⇒ `POST /api/role/2099000000000009000/permissions`
  //   后端按"角色不存在"抛 RuntimeException，被兜底吞成 500「系统异常，请稍后重试」，
  //   于是租户 2 拿不到权限码 → 乙方确认签署 403 → 双签永远齐不了（整条链都红在这一个数字上）。
  const t2Role = mustId(scalar(
    `SELECT r.id FROM sys_role r JOIN sys_user_role ur ON ur.role_id=r.id
      JOIN sys_user u ON u.id=ur.user_id WHERE u.username='${TENANT2.u}' AND r.deleted=0 LIMIT 1`),
    `${TENANT2.u} 的角色 id`)
  // 同理 permission_id 也可能是雪花 id（实测该角色原有 2065122954464432130 这类值）：
  // 一旦 map(Number) 截断，"还原"就会写进一堆不存在的 permission_id，把租户 2 角色原有权限删掉。
  const rolePermIdsBefore = sql(`SELECT permission_id FROM sys_role_permission
    WHERE role_id=${t2Role} ORDER BY permission_id`)
    .replace(/\r/g, '').split('\n').map(l => l.trim())
    .filter(l => /^\d+$/.test(l))

  let roleGranted = false

  try {
    // ══ ① 字典表结构：没有默认值列 ══
    section('① 平台字典**没有** default_option 列（平台给默认值 = 替双方做决定）')
    const defaultCols = num(`SELECT count(*) FROM information_schema.columns
      WHERE table_name='agreement_term_option'
        AND column_name IN ('default_option','default_option_code','default_value')`)
    ok('agreement_term_option 不存在 default* 列', defaultCols === 0, `实际 ${defaultCols} 列`)
    const snapNotNull = num(`SELECT count(*) FROM information_schema.columns
      WHERE table_name='agreement_version' AND column_name='snapshot_json' AND is_nullable='NO'`)
    ok('agreement_version.snapshot_json 是 NOT NULL（平台必须留快照）', snapNotNull === 1)

    // ══ 准备：探针字典 + 两份探针协议 ══
    section('准备：探针字典选项 + 两份探针协议（结束时全部硬删）')
    const optReq = await req('POST', '/agreement/term-options', {
      token: adminToken,
      body: { termCode: TERM_REQUIRED, optionCode: 'TO_SUPPLIER', optionLabel: 'E2E 退回运费由供货方承担',
        semantics: '退货时退回运费由供货方承担，并从本期结算中扣除', required: true, sort: 1, status: 1 },
    })
    const optOpt = await req('POST', '/agreement/term-options', {
      token: adminToken,
      body: { termCode: TERM_OPTIONAL, optionCode: 'BOTH_AGREE', optionLabel: 'E2E 取消需双方同意',
        semantics: '任一方要求取消协议时需对方书面同意', required: false, sort: 1, status: 1 },
    })
    ok('平台侧可新增条款字典（agreement:platform:term-option:manage 生效）',
      optReq.status === 200 && optOpt.status === 200,
      `status=${optReq.status}/${optOpt.status} ${msgOf(optReq)}`)

    // S1：两端为租户 1 与租户 2（正向流程 + 双签用）
    const c1 = await req('POST', '/agreement', {
      token: adminToken,
      body: { agreementType: 'DISTRIBUTION', partyAId: partyOfT1, partyATenantId: T1,
        partyBId: partyOfT1B, partyBTenantId: T2, title: 'E2E 协议验证（双签用）' },
    })
    const a1 = replyId(c1)
    if (a1) probeAgreements.push(a1)
    ok('租户级协议可新建（agreement:create 生效，甲方租户=1）', c1.status === 200 && a1 !== null,
      `status=${c1.status} id=${a1 ?? '(回执里没有字符串 id: ' + JSON.stringify(dataOf(c1)) + ')'} ${msgOf(c1)}`)

    // S2：两端为租户 2 与"不存在的第三方租户"（用于第三方不可见断言）
    const c2 = await req('POST', '/agreement', {
      token: adminToken,
      body: { agreementType: 'DISTRIBUTION', partyAId: partyOfT1, partyATenantId: T2,
        partyBId: partyOfT1B, partyBTenantId: TX, title: 'E2E 协议验证（第三方不可见用）' },
    })
    const a2 = replyId(c2)
    if (a2) probeAgreements.push(a2)
    ok(`第二份探针协议可新建（两端租户=${T2} 与 ${TX}）`, c2.status === 200 && a2 !== null,
      `status=${c2.status} id=${a2 ?? '(回执里没有字符串 id: ' + JSON.stringify(dataOf(c2)) + ')'}`)

    // a1 / a2 不合法 ⇒ 后面每一条都无从谈起：在这里用中文说清，
    // 而不是拿空 id 去拼 SQL（`WHERE agreement_id=` 是语法错误，会喷 PSQL 栈）或拼 URL。
    // 后续一律用 A1/A2/V1/V2（已过 mustId），原始 a1/a2 只在错误信息与 finally 里用。
    const A1 = mustId(a1, 'a1（第一份探针协议 id）')
    const A2 = mustId(a2, 'a2（第二份探针协议 id）')

    const v1 = scalar(
      `SELECT id FROM agreement_version WHERE agreement_id=${A1}`
      + ` AND deleted=0 ORDER BY version_no LIMIT 1`).trim()
    ok('新建协议自动带出第 1 个草稿版本', isId(v1), `versionId=${v1}`)
    const V1 = mustId(v1, 'v1（第一份探针协议的草稿版本 id）')

    // ══ ② 必填条款没选完 ⇒ 不许生效 ══
    section('② 必填条款没选完 → activate 被拒，且文案指出缺什么')
    const act0 = await req('POST', `/agreement/version/${V1}/activate`, { token: adminToken })
    ok('一项条款都没选时置生效 → 被拒（不是 500）', act0.status === 400,
      `status=${act0.status} ${msgOf(act0)}`)
    ok('文案指出缺的必填条款（' + TERM_REQUIRED + '）', msgOf(act0).includes(TERM_REQUIRED),
      `message=${msgOf(act0)}`)
    ok('文案说明平台不提供默认值（未约定必须双方选定）', /默认值/.test(msgOf(act0)),
      `message=${msgOf(act0)}`)

    // 保存条款：只选可选项，必填仍缺 ⇒ 保存回执要能看出缺什么（契约要求）
    const save1 = await req('PUT', `/agreement/${A1}`, {
      token: adminToken,
      body: { versionId: V1, terms: [{ termCode: TERM_OPTIONAL, optionCode: 'BOTH_AGREE' }] },
    })
    const save1Body = dataOf(save1) || {}
    ok('保存草稿条款成功', save1.status === 200, `status=${save1.status} ${msgOf(save1)}`)
    ok('保存回执列出仍缺的必填条款', Array.isArray(save1Body.missingRequiredTerms)
      && save1Body.missingRequiredTerms.includes(TERM_REQUIRED),
      `missingRequiredTerms=${JSON.stringify(save1Body.missingRequiredTerms)}`)
    const act1 = await req('POST', `/agreement/version/${V1}/activate`, { token: adminToken })
    ok('只选可选项（必填仍缺）时置生效 → 仍被拒', act1.status === 400,
      `status=${act1.status} ${msgOf(act1)}`)

    // ══ ③ 双签缺一不可 ══
    section('③ 只一方确认 → activate 被拒（这是"不能单方改协议"的技术保证）')
    const save2 = await req('PUT', `/agreement/${A1}`, {
      token: adminToken,
      body: { versionId: V1, terms: [
        { termCode: TERM_REQUIRED, optionCode: 'TO_SUPPLIER' },
        { termCode: TERM_OPTIONAL, optionCode: 'BOTH_AGREE' },
      ] },
    })
    ok('把必填条款也选上 → 保存成功', save2.status === 200, `status=${save2.status} ${msgOf(save2)}`)

    const confA = await req('POST', `/agreement/version/${V1}/confirm`, { token: adminToken })
    ok('甲方（会话租户=1）确认签署 → 成功', confA.status === 200, `status=${confA.status} ${msgOf(confA)}`)
    const act2 = await req('POST', `/agreement/version/${V1}/activate`, { token: adminToken })
    ok('只有甲方确认就置生效 → 被拒', act2.status === 400, `status=${act2.status} ${msgOf(act2)}`)
    ok('文案指出缺乙方确认', /乙方/.test(msgOf(act2)), `message=${msgOf(act2)}`)
    ok('被拒后库里没有生效版本（status 仍为草稿）',
      scalar(`SELECT status FROM agreement_version WHERE id=${V1}`) === '0',
      `status=${scalar(`SELECT status FROM agreement_version WHERE id=${V1}`)}`)

    // 让租户 2 的账号具备协议权限（临时探针，结束还原）
    section('准备：临时把**租户级**协议权限码授予租户 2 角色（结束按原值还原）')
    // ⚠️ 同样**不过 Number()**：这是"临时授权 + 还原"要写进真实数据表的 id，截断一位就是写错权限
    // ⚠️ **刻意排除 `agreement:platform:%`**：平台码归「系统」模块、只开给系统租户（V11.455.0）。
    //    授给业务租户角色等于把"读任意租户协议"的能力散出去 —— 那正是 §13.9 加独立码要防的事。
    //    排除之后，⑨ 组才能拿这个会话验**拒绝路径**（有租户级码、无平台码 ⇒ 403）。
    const permIds = sql(`SELECT id FROM sys_permission WHERE deleted=0 AND permission_code LIKE 'agreement:%'
        AND permission_code NOT LIKE 'agreement:platform:%'`)
      .replace(/\r/g, '').split('\n').map(l => l.trim())
      .filter(l => /^\d+$/.test(l))
    ok('租户级协议权限码 id 已全部取到（用于临时授予租户 2）',
      permIds.length >= CODES_BASE - 4, `实际 ${permIds.length} 条（已排除 4 条 agreement:platform:*）`)
    const grant = await req('POST', `/role/${t2Role}/permissions`, {
      token: adminToken, body: [...new Set([...rolePermIdsBefore, ...permIds])],
    })
    ok('授予成功（接口内部会清该角色用户的权限缓存，故无需 sleep）', grant.status === 200,
      `status=${grant.status} ${msgOf(grant)}`)
    roleGranted = true

    const t2Token2 = await login(TENANT2)
    const confB = await req('POST', `/agreement/version/${V1}/confirm`, { token: t2Token2 })
    ok('乙方（会话租户=2）确认签署 → 成功', confB.status === 200, `status=${confB.status} ${msgOf(confB)}`)

    // ══ ④ 双签齐 + 必填齐 ⇒ 生效，且旧版本被取代 ══
    section('④ 必填齐 + 双签齐 → 置生效成功，主档指向新版本')
    const act3 = await req('POST', `/agreement/version/${V1}/activate`, { token: adminToken })
    ok('置生效成功', act3.status === 200, `status=${act3.status} ${msgOf(act3)}`)
    ok('版本状态 = ACTIVE(1)', scalar(`SELECT status FROM agreement_version WHERE id=${V1}`) === '1')
    ok('主档 current_version_id 指向该版本',
      scalar(`SELECT current_version_id FROM agreement WHERE id=${A1}`) === V1)
    ok('主档状态 = ACTIVE(1)', scalar(`SELECT status FROM agreement WHERE id=${A1}`) === '1')
    const snapshotAfterActivate = scalar(`SELECT snapshot_json::text FROM agreement_version WHERE id=${V1}`)

    // ══ ⑤ 已生效版本只读 ══
    section('⑤ 对已 ACTIVE 版本写条款/快照 → 被拒，且库里快照逐字未变')
    // ⚠️ 载荷必须**本身合法**（这里用的就是本次已选定的那两项：必填→TO_SUPPLIER、可选→BOTH_AGREE）。
    //    旧版给必填条款 TERM_REQUIRED 选了 `BOTH_AGREE` —— 那个选项**不属于这个 term_code**，
    //    于是被"选项不在字典里"先拦下，证明不了"已生效版本不可写"这条不变量（拒绝理由根本不是我们要验的那条）。
    //    换成合法载荷后，唯一的拒绝理由只能是**主档已 ACTIVE ⇒ 条款不可写**（§12.2 不变量 1）。
    const tamper = await req('PUT', `/agreement/${A1}`, {
      token: adminToken,
      body: { versionId: V1, terms: [
        { termCode: TERM_REQUIRED, optionCode: 'TO_SUPPLIER' },
        { termCode: TERM_OPTIONAL, optionCode: 'BOTH_AGREE' },
      ] },
    })
    ok('对已生效版本写条款 → 被拒（400）', tamper.status === 400,
      `status=${tamper.status} ${msgOf(tamper)}`)
    ok('文案说明已生效版本不可修改 / 只能发起变更',
      /已生效|不可修改|发起变更/.test(msgOf(tamper)), `message=${msgOf(tamper)}`)
    ok('库里快照**逐字未变**',
      scalar(`SELECT snapshot_json::text FROM agreement_version WHERE id=${V1}`) === snapshotAfterActivate)
    ok('库里条款未被改写（仍是 2 条、且必填项仍是 TO_SUPPLIER）',
      scalar(`SELECT count(*) FROM agreement_term WHERE version_id=${V1} AND deleted=0`) === '2'
      && scalar(`SELECT option_code FROM agreement_term WHERE version_id=${V1} AND term_code='${TERM_REQUIRED}'`)
        === 'TO_SUPPLIER')

    // ══ ⑥ 一方拒绝 ⇒ 现行版本继续有效 ══
    section('⑥ 发起变更后一方拒绝 → 该草稿 REJECTED，**原 ACTIVE 版本仍是 ACTIVE、主档不动**')
    const rowBefore = scalar(`SELECT status || '|' || snapshot_json::text || '|' || version_no
      FROM agreement_version WHERE id=${V1}`)
    const currentBefore = scalar(`SELECT current_version_id FROM agreement WHERE id=${A1}`)

    const newVer = await req('POST', `/agreement/${A1}/versions`, {
      token: adminToken, body: { changeReason: 'E2E：试着改佣金比例' },
    })
    const v2 = replyId(newVer)
    ok('发起变更 → 生成新的草稿版本', newVer.status === 200 && v2 !== null,
      `status=${newVer.status} versionId=${v2 ?? '(回执里没有字符串 id: ' + JSON.stringify(dataOf(newVer)) + ')'} ${msgOf(newVer)}`)
    const V2 = mustId(v2, 'v2（变更产生的新草稿版本 id）')
    ok('新版本状态 = 草稿(0)', scalar(`SELECT status FROM agreement_version WHERE id=${V2}`) === '0')
    ok('新版本是从现行版本复制而来的条款（条款数与生效版一致）',
      scalar(`SELECT count(*) FROM agreement_term WHERE version_id=${V2} AND deleted=0`)
        === scalar(`SELECT count(*) FROM agreement_term WHERE version_id=${V1} AND deleted=0`))

    const rej = await req('POST',
      `/agreement/version/${V2}/reject?reason=${encodeURIComponent('E2E 不同意改价')}`, { token: t2Token2 })
    ok('乙方否决该草稿版本 → 成功', rej.status === 200, `status=${rej.status} ${msgOf(rej)}`)
    ok('该草稿版本状态 = REJECTED(3)',
      scalar(`SELECT status FROM agreement_version WHERE id=${V2}`) === '3')
    ok('**原生效版本仍是 ACTIVE(1)**（变更谈成前交易照常）',
      scalar(`SELECT status FROM agreement_version WHERE id=${V1}`) === '1')
    ok('主档 current_version_id 未变', scalar(`SELECT current_version_id FROM agreement WHERE id=${A1}`)
      === currentBefore)
    ok('主档状态仍是 ACTIVE(1)', scalar(`SELECT status FROM agreement WHERE id=${A1}`) === '1')
    ok('原生效版本行除 status 外逐字段未变（变更不追溯、不回写既有数据）',
      scalar(`SELECT status || '|' || snapshot_json::text || '|' || version_no
        FROM agreement_version WHERE id=${V1}`) === rowBefore)

    // ══ ⑦ 第三方会话看不到别人的协议 ══
    section('⑦ 会话租户不是任一端 → 查不到那份协议，但该行在库里确实存在')
    ok('探针协议 a2 的两端是租户 2 与 ' + TX + '（会话租户 1 不是任一端）',
      scalar(`SELECT party_a_tenant_id || ',' || party_b_tenant_id FROM agreement WHERE id=${A2}`)
        === `${T2},${TX}`)
    const d2 = await req('GET', `/agreement/${A2}`, { token: adminToken })
    ok('会话租户 1 读 a2 详情 → 404「协议不存在」', d2.status === 404,
      `status=${d2.status} ${msgOf(d2)}`)
    ok('回话不泄露协议的存在性（文案与真的不存在一致）',
      /不存在/.test(msgOf(d2)), `message=${msgOf(d2)}`)
    const pageAdmin = await req('GET', '/agreement/page?current=1&size=200', { token: adminToken })
    const adminRecords = (dataOf(pageAdmin) || {}).records || []
    ok('会话租户 1 的协议列表里**没有** a2',
      !adminRecords.some(r => String(r.id) === A2),
      `共 ${adminRecords.length} 条`)
    ok('但 a2 在**库里确实存在**（证明是可见性条件拦住的，不是没数据）', probeExists(a2) === 1)

    const d2t2 = await req('GET', `/agreement/${A2}`, { token: t2Token2 })
    ok('会话租户 2（是其中一端）读 a2 详情 → 200（反向对照）', d2t2.status === 200,
      `status=${d2t2.status} ${msgOf(d2t2)}`)
    const d1t2 = await req('GET', `/agreement/${A1}`, { token: t2Token2 })
    ok('会话租户 2 读 a1（两端为 1 与 2）→ 200（两端对称可见）', d1t2.status === 200,
      `status=${d1t2.status} ${msgOf(d1t2)}`)

    // ══ ⑧ 权限码前缀归属（裁定⑤） ══
    section('⑧ 平台侧码归「系统」、租户侧码归「协议」（最长前缀优先，真库实测）')
    const ownerOf = (code) => scalar(`SELECT p.module_code FROM sys_module_permission p
      WHERE p.deleted=0 AND '${code}' LIKE p.permission_prefix || '%'
      ORDER BY length(p.permission_prefix) DESC, p.sort LIMIT 1`)
    ok('agreement:platform:term-option:manage → system', ownerOf('agreement:platform:term-option:manage') === 'system',
      `实际 ${ownerOf('agreement:platform:term-option:manage')}`)
    ok('agreement:list → agreement', ownerOf('agreement:list') === 'agreement')
    ok('agreement:version:activate → agreement', ownerOf('agreement:version:activate') === 'agreement')
    ok('agreement:platform:compliance:read → system（平台合规抽查读靠它天然只有平台侧能拿）',
      ownerOf('agreement:platform:compliance:read') === 'system',
      `实际 ${ownerOf('agreement:platform:compliance:read')}`)

    // ══ ⑨ 平台合规抽查读：平台能读"自己不是任一端"的协议；租户侧读不了 ══
    // 这一组是 ⑦ 的**反向对照**：同一份 a2（两端是租户 2 与 TX，会话租户 1 不是任一端），
    //   · 租户级读端点 → ⑦ 已证 404；
    //   · 平台合规读端点 + 平台码 → 200，且读到的就是它（§13.9 用户明确要的能力）；
    //   · 租户会话（有租户级码、**无**平台码）→ 403（拒绝路径）。
    section('⑨ 平台合规抽查读：平台读得到「自己不是任一端」的协议，租户侧拿不到这个能力')
    const compDetail = await req('GET', `/agreement/platform/compliance/${A2}`, { token: adminToken })
    ok('平台侧（超管，持 agreement:platform:compliance:read）读 a2 详情 → 200',
      compDetail.status === 200, `status=${compDetail.status} ${msgOf(compDetail)}`)
    ok('读到的确实是 a2（不是"刚好读到了自己的"）',
      String((dataOf(compDetail) || {}).id || '') === A2,
      `实际 id=${(dataOf(compDetail) || {}).id}`)
    const compPage = await req('GET', '/agreement/platform/compliance/page?current=1&size=200',
      { token: adminToken })
    const compRecords = (dataOf(compPage) || {}).records || []
    ok('合规抽查列表里**也有** a2（列表与详情口径一致，不是只有详情放行）',
      compRecords.some(r => String(r.id) === A2), `共 ${compRecords.length} 条`)

    const compByTenant = await req('GET', `/agreement/platform/compliance/${A2}`, { token: t2Token2 })
    ok('租户会话（有租户级码、无平台码）走合规端端点 → 403（拒绝路径）',
      compByTenant.status === 403, `status=${compByTenant.status} ${msgOf(compByTenant)}`)

    // 反向对照：租户会话走它**自己**的租户级读端点仍然正常（证明 403 是"没平台码"，不是"整个账号废了"）
    const tenantReadOk = await req('GET', `/agreement/${A2}`, { token: t2Token2 })
    ok('同一会话走租户级端点仍 200（证明 403 只来自平台码缺失，不是账号权限被清空）',
      tenantReadOk.status === 200, `status=${tenantReadOk.status} ${msgOf(tenantReadOk)}`)
  } finally {
    console.log('\n—— 现场还原（探针数据一律硬删）——')
    // 1) 角色权限还原（**字符串** id，绝不 Number()：截断会把"还原"写成"删掉原有权限"）
    if (roleGranted) {
      try {
        const restore = await req('POST', `/role/${t2Role}/permissions`, {
          token: adminToken, body: rolePermIdsBefore,
        })
        console.log(`  角色 ${t2Role} 权限还原: status=${restore.status} ${msgOf(restore)}`)
      } catch (e) {
        // 还原本身失败也必须往下走：让下面的"逐项还原"断言**红出来**（而不是整段 finally 被跳过、
        // 探针授权静默留在真实角色上 —— 那样重跑时它还会被当成"原值"，污染永久固化）
        console.log(`  角色 ${t2Role} 权限还原失败: ${e.message}`)
      }
    }
    const rolePermAfter = sql(`SELECT permission_id FROM sys_role_permission WHERE role_id=${t2Role}`)
      .replace(/\r/g, '').split('\n').map(l => l.trim())
      .filter(l => /^\d+$/.test(l)).sort(cmpId)
    const expected = [...rolePermIdsBefore].sort(cmpId)
    ok('【自清理】租户 2 角色权限已逐项还原',
      JSON.stringify(rolePermAfter) === JSON.stringify(expected),
      `now=${JSON.stringify(rolePermAfter)} 原=${JSON.stringify(expected)}`)

    // 2) 协议/字典探针硬删：**按标题前缀扫库删**（上次异常退出留下的行也能清掉）
    hardCleanProbes()
    const leftRows = probeRowCounts()
    const leftAgreements = probeAgreements.filter(id => isId(id) && probeExists(id) !== 0)
    ok('【自清理】探针协议/版本/条款已**硬删**（软删会占唯一键，第二次跑不通）',
      leftAgreements.length === 0 && leftRows.agreement === 0 && leftRows.version === 0 && leftRows.term === 0,
      `本次未删净的 id=${JSON.stringify(leftAgreements)}，库内探针行数=${JSON.stringify(leftRows)}`)
    ok('【自清理】探针字典选项已硬删（含已软删行）', leftRows.option === 0,
      `库内还剩 ${leftRows.option} 行`)
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail ? 1 : 0)
})().catch(e => {
  // 只打一行中文说明：PSQL/子进程的 Java 栈只会掩盖真实失败，不该出现在报告里
  console.error(`脚本异常: ${e instanceof ProbeFailure ? e.message : firstLine(e)}`)
  try { hardCleanProbes() } catch { /* 尽力而为 */ }
  process.exit(2)
})
