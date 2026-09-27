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
 *   ④ **反向对照**：`party.tenant_id` 恒 0（共享层），而边的 `tenant_id` = 建档租户；
 *   ⑤ **双写失败不得连累建档**（用户裁定「选项乙」的硬要求）—— 用临时 CHECK 约束
 *      确定性地让 SALE 边写不进去，断言建档仍成功且**真落库**、镜像侧整体回滚；
 *   ⑥ 批 2 的 B 组商务条件 + **方向纯度** —— `credit_days` 是应收、`payment_days` 是应付，
 *      **绝不许跨方向照抄**（DOMAIN-MODEL §6.1 裁定 ⑲「账期不可传递」）；
 *   ⑦ 批 2b 的三张子表（证件/银行/地址）也被双写覆盖 + **清空即软删**（不留陈旧账户）；
 *   ⑧ 删主体时**子表也一起软删**（只加"写"忘加"删"会留下悬空子表行）。
 *
 * 【探针一律硬删】按名称前缀扫库删三张表（软删会占唯一键，第二次跑不通）；
 *   ⑤ 用的诱导约束也在 `finally`/`cleanup` 两处必摘（留在这里会静默停掉所有 SALE 边）。
 *
 * 用法：node tools/verify-party-dual-write.cjs
 *   前置：后端在 5655 运行；`V11.497.0`/`V11.506.0`/`V11.507.x`/`V11.520.0` 已应用。
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

/** 当前 token（会因"被别处的 admin 登录顶下线"而失效，见 req 的自动重登） */
let TOKEN = null

async function req(method, p, { token, body, retried } = {}) {
  // `token === undefined` ⇒ 用当前会话 token；显式传 null ⇒ 不带（登录接口自己用）
  const t = token === undefined ? TOKEN : token
  // ⚠️ 传输层重试：Node 的 fetch(undici) 会复用 keep-alive 连接，而本脚本在两次请求之间
  //    夹着几十次慢 SQL（spawn 一个 node 进程），空闲连接很容易被服务端先关掉 ⇒
  //    下一次请求抛 `TypeError: fetch failed`（ECONNRESET）。这与业务无关，
  //    2026-09-27 实测在完全正常的服务上偶发命中，重试即可，别让它冒充功能 FAIL。
  let res
  for (let attempt = 0; ; attempt++) {
    try {
      res = await fetch(BASE + p, {
        method,
        headers: { 'Content-Type': 'application/json', ...(t ? { Authorization: `Bearer ${t}` } : {}) },
        body: body === undefined ? undefined : JSON.stringify(body),
      })
      break
    } catch (e) {
      if (attempt >= 2) {
        // 把底层原因带出来：undici 的 `fetch failed` 只是个壳，
        // 真因在 e.cause（ECONNRESET / ECONNREFUSED / timeout…），不带出来没法定位
        const c = e.cause
        throw new Error(`fetch 失败 ${method} ${p}（重试 3 次）：${e.message}` +
          ` / cause=${c ? (c.code || c.message || JSON.stringify(c)) : '(无)'}`)
      }
      await new Promise(r => setTimeout(r, 300))
    }
  }
  const text = await res.text()
  if (res.status === 401 && !retried) {
    // ⚠️ 本仓 admin 是**单会话**（Sa-Token 后登录的把前一个顶下线，日志里是
    //    「用户被顶替下线（多地登录）」）⇒ 本仓工作区长期有并行会话，本脚本手里的 token
    //    随时可能被别处的一次登录作废。不自动重登的话，整段会以 401 假失败，
    //    把"被顶下线"误报成"双写坏了"（2026-09-27 实踩）。
    await login(ADMIN)
    return req(method, p, { token: TOKEN, body, retried: true })
  }
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON */ }
  return { status: res.status, json, text }
}

async function login(user) {
  const cap = await req('GET', '/auth/captcha', { token: null })
  if (!cap.json?.data?.img) throw new ProbeFailure(`验证码接口异常: ${cap.text.slice(0, 150)}`)
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    token: null,
    body: { username: user.u, password: user.p, tenantName: user.t, captcha: code, captchaKey: cap.json.data.uuid },
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new ProbeFailure(`${user.u} 登录失败: ${res.text.slice(0, 200)}`)
  TOKEN = token
  return token
}

/** 硬删探针（三张表一起）。 */
function cleanup() {
  // ⑤ 用的诱导约束必须一并摘掉：上一轮被 Ctrl-C 打断就可能留在这里，
  // 而它会**静默**让所有 SALE 边写不进去（建档照旧成功，所以没人会发现）。
  sql(`ALTER TABLE party_tenant DROP CONSTRAINT IF EXISTS zz_e2e_probe_no_sale`, true)
  const ids = `SELECT id FROM biz_party WHERE party_code LIKE 'E2EDW%' OR party_name LIKE '${PREFIX}%'`
  // 三张子表（批 2b 起也被双写覆盖）没有外键 ⇒ 必须显式删，否则探针会以"子表孤儿"的形式留下来
  sql(`DELETE FROM party_cert    WHERE party_id IN (${ids})`, true)
  sql(`DELETE FROM party_bank    WHERE party_id IN (${ids})`, true)
  sql(`DELETE FROM party_address WHERE party_id IN (${ids})`, true)
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

    const nB = num(`SELECT count(*) FROM information_schema.columns
      WHERE table_schema='public' AND table_name='party_tenant' AND column_name IN
        ('default_handler_id','default_handler_name','current_debt','credit_days','payment_days',
         'payment_term_type','fixed_credit_day','settlement_day','promoter_id','promoter_name',
         'buyer_account','customer_source','roles','category_id','warehouse_name','last_trade_time')`)
    ok('party_tenant 已含批 2 的 16 个 B 组列（V11.520.0）', nB === 16, `实际 ${nB} 列`)
    if (nB !== 16) process.exit(2)

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

    section('⑤ 反向验证：双写失败**不得连累建档**（用户裁定「选项乙」的硬要求）')
    // 为什么要专门验这条：2026-09-27 实测过一次**静默丢数据** ——
    //   `party_tenant.price_track_enabled` 是布尔列却收到整型参数 ⇒ 边写入报错
    //   ⇒ PostgreSQL 把整个事务打成 aborted ⇒ **同事务里刚 insert 的 biz_party 行一起没了**，
    //   而接口照样返回 HTTP 200（`data: null`）。也就是说"catch 住异常"根本没兜住这条裁定，
    //   必须回滚到保存点。这个用例就是钉住它。
    //
    // 怎么确定性触发镜像失败（不改一行业务代码、跑完必摘）：给 `party_tenant` 挂一条
    // **只对新行生效**的 CHECK，让 SALE 边插不进去。`NOT VALID` = 不校验存量行。
    const R = `${PREFIX}${Date.now()}`
    const CODE_FAIL = `E2EDWFAIL${Date.now()}`
    const CODE_OK = `E2EDWOK${Date.now()}`
    sql(`ALTER TABLE party_tenant ADD CONSTRAINT zz_e2e_probe_no_sale
         CHECK (direction <> 'SALE') NOT VALID`)
    try {
      const r = await req('POST', '/erp/md/customer', {
        token, body: {
          partnerCode: CODE_FAIL, partnerName: R + '甲',
          partnerType: 'customer', partyType: 1, roles: 'CUSTOMER', status: 1,
        },
      })
      ok('建档仍返回 200', r.status === 200, `status=${r.status}`)
      // ⚠️ 这条是"事务有没有被拖坏"的**直接观测点**：id 为空说明控制器拿到的是 null 实体
      //    （只剩这一步能证明"事务还活着"，因为响应体本身不带错误信息）。
      ok('响应里带回了 id（⇒ 事务没被镜像失败拖坏）', r.json?.data?.id != null,
        `data=${JSON.stringify(r.json?.data)?.slice(0, 120)}`)
      ok('biz_party **有**这一行（双写失败没有连累建档）',
        num(`SELECT count(*) FROM biz_party WHERE party_code='${CODE_FAIL}' AND deleted=0`) === 1,
        `实际 ${num(`SELECT count(*) FROM biz_party WHERE party_code='${CODE_FAIL}' AND deleted=0`)} 行`)
      ok('party 侧**没有**镜像行（本次双写已整体回滚，符合预期）',
        num(`SELECT count(*) FROM party WHERE party_code='${CODE_FAIL}'`) === 0,
        `实际 ${num(`SELECT count(*) FROM party WHERE party_code='${CODE_FAIL}'`)} 行`)
      ok('缺口可被对账脚本看见（party 行数 < biz_party 未删行数）',
        num(`SELECT count(*) FROM party`) < num(`SELECT count(*) FROM biz_party WHERE deleted=0`),
        `party=${num(`SELECT count(*) FROM party`)} vs biz_party=${num(`SELECT count(*) FROM biz_party WHERE deleted=0`)}`)
    } finally {
      sql(`ALTER TABLE party_tenant DROP CONSTRAINT IF EXISTS zz_e2e_probe_no_sale`, true)
    }

    // 约束摘掉后再建一次：证明①失败真的只是那条约束造成的 ②补齐路径是通的
    const r2 = await req('POST', '/erp/md/customer', {
      token, body: {
        partnerCode: CODE_OK, partnerName: R + '乙',
        partnerType: 'customer', partyType: 1, roles: 'CUSTOMER', status: 1,
      },
    })
    const id2 = r2.json?.data?.id
    ok('约束摘掉后建档成功且双写补齐（party 行 + SALE 边都在）',
      id2 != null
        && num(`SELECT count(*) FROM party WHERE party_code='${CODE_OK}'`) === 1
        && num(`SELECT count(*) FROM party_tenant WHERE party_id=${id2} AND direction='SALE'`) === 1,
      `id=${id2}, party=${num(`SELECT count(*) FROM party WHERE party_code='${CODE_OK}'`)}, ` +
      `边=${id2 == null ? 'n/a' : num(`SELECT count(*) FROM party_tenant WHERE party_id=${id2} AND direction='SALE'`)}`)

    section('⑥ 批 2：B 组商务条件 + ⚠️ 方向纯度（裁定 ⑲「账期不可传递」）')
    // 这一组钉的是最容易写错、且错了最贵的一条：`credit_days` 是**应收**、`payment_days` 是**应付**
    // （BusinessAccountingServiceImpl.resolveIntraTenantDueDate 的 receivableSide 分支）。
    // 若镜像图省事"照抄到每条边"，就会把客户的账期搬到供应商那边 —— 正是 ⑲ 明令禁止的"可传递"。
    const R6 = `${PREFIX}${Date.now()}`
    const CODE_SALE = `E2EDWSALE${Date.now()}`
    const CODE_BUY = `E2EDWBUY${Date.now()}`
    const rs = await req('POST', '/erp/md/customer', {
      token, body: {
        partnerCode: CODE_SALE, partnerName: R6 + '售', partnerType: 'customer', roles: 'CUSTOMER',
        // 应收侧：这三项是建档入参（⚠️ `currentDebt` **不是**入参 —— 它是账务写入的派生列，
        // 传了会被静默忽略，别拿它当"传进去了"的证据）
        creditDays: 30, creditLimit: 100000, fixedCreditDay: 10,
        // 应付侧两项（**不该**落 SALE 边）
        paymentDays: 45, fixedPaymentDay: 20,
        // 逐边照抄项 + A 组此前漏写的 4 列
        settlementDays: 30, partyLevel: 'VIP', buyerAccount: 'BA001', customerSource: '推荐',
        warehouseName: 'WH1', promoterName: '推广人甲', defaultHandlerName: '业务员甲',
        companyFullName: R6 + '有限公司', mnemonicCode: 'MNC1',
        website: 'https://e2e.example', fax: '021-00000000',
      },
    })
    const idS = rs.json?.data?.id
    const eS = (col) => scalar(`SELECT ${col} FROM party_tenant WHERE party_id=${idS} AND direction='SALE'`)
    const cS = (cond) => num(`SELECT count(*) FROM party_tenant WHERE party_id=${idS} AND direction='SALE' AND ${cond}`)
    ok('建档带回 id', idS != null, `data.id=${idS}`)
    ok('SALE 边：应收四项都写上了',
      cS('credit_days = 30') === 1 && cS('credit_limit = 100000') === 1
        && cS('fixed_credit_day = 10') === 1
        && cS(`current_debt IS NOT DISTINCT FROM (SELECT current_debt FROM biz_party WHERE id = ${idS})`) === 1,
      `credit_days=${eS('credit_days')}, credit_limit=${eS('credit_limit')}, ` +
      `current_debt=${eS('current_debt')}, fixed_credit_day=${eS('fixed_credit_day')}`)
    ok('⚠️ SALE 边：应付两列**必须是 NULL**（账期不可传递）',
      cS('payment_days IS NULL') === 1 && cS('fixed_payment_day IS NULL') === 1,
      `payment_days=${eS('payment_days')}, fixed_payment_day=${eS('fixed_payment_day')}`)
    ok('settlement_type 落本模块词表（现结）', eS('settlement_type') === '现结', `实际 ${eS('settlement_type')}`)
    ok('逐边照抄项也在（party_level / buyer_account / customer_source / roles）',
      eS('party_level') === 'VIP' && eS('buyer_account') === 'BA001'
        && eS('customer_source') === '推荐' && eS('roles') === 'CUSTOMER',
      `level=${eS('party_level')}, buyer=${eS('buyer_account')}, ` +
      `source=${eS('customer_source')}, roles=${eS('roles')}`)
    ok('A 组补洞：此前漏写的 4 列也进了 party',
      scalar(`SELECT company_full_name FROM party WHERE id=${idS}`) === R6 + '有限公司'
        && scalar(`SELECT mnemonic_code FROM party WHERE id=${idS}`) === 'MNC1'
        && scalar(`SELECT website FROM party WHERE id=${idS}`) === 'https://e2e.example'
        && scalar(`SELECT fax FROM party WHERE id=${idS}`) === '021-00000000',
      `company=${scalar(`SELECT company_full_name FROM party WHERE id=${idS}`)}, ` +
      `mnemonic=${scalar(`SELECT mnemonic_code FROM party WHERE id=${idS}`)}`)

    // 改一次结算方式：验证 updateById 路径把 B 组也刷过去（而不是只在建档时写一次）
    await req('PUT', `/erp/md/customer/${idS}`, { token, body: { settleType: '挂账' } })
    ok('改成「挂账」后 settlement_type 跟着变（B 组在写路径上真的通了）',
      eS('settlement_type') === '挂账', `实际 ${eS('settlement_type')}`)

    // 供应商侧：应付列同样只落自己那条边
    const rb = await req('POST', '/erp/md/customer', {
      token, body: {
        partnerCode: CODE_BUY, partnerName: R6 + '购', partnerType: 'supplier', roles: 'SUPPLIER',
        paymentDays: 45, fixedPaymentDay: 20,
        // 反方向的应收项也一起传：**它们必须落不到 PURCHASE 边上**
        creditDays: 30, creditLimit: 50000, fixedCreditDay: 10,
      },
    })
    const idB = rb.json?.data?.id
    const cB = (cond) => num(`SELECT count(*) FROM party_tenant WHERE party_id=${idB} AND direction='PURCHASE' AND ${cond}`)
    ok('PURCHASE 边：应付两项写上了',
      cB('payment_days = 45') === 1 && cB('fixed_payment_day = 20') === 1,
      `payment_days=${scalar(`SELECT payment_days FROM party_tenant WHERE party_id=${idB} AND direction='PURCHASE'`)}`)
    ok('⚠️ PURCHASE 边：应收四项**必须是 NULL**（账期不可传递）',
      cB('credit_limit IS NULL') === 1 && cB('current_debt IS NULL') === 1
        && cB('credit_days IS NULL') === 1 && cB('fixed_credit_day IS NULL') === 1,
      `credit_limit=${scalar(`SELECT credit_limit FROM party_tenant WHERE party_id=${idB} AND direction='PURCHASE'`)}, ` +
      `credit_days=${scalar(`SELECT credit_days FROM party_tenant WHERE party_id=${idB} AND direction='PURCHASE'`)}`)

    section('⑦ 批 2b：三张子表（证件 / 银行 / 地址）也被双写了')
    // 为什么单列一组：这三张表批 1 就建好并回填了，但**双写一开始没覆盖**它们 ——
    // 客户表单里税号/银行/地址都是可编辑项 ⇒ 不覆盖就是"填一次就漂移、切读即丢数据"。
    const R7 = `${PREFIX}${Date.now()}`
    const CODE_SUB = `E2EDWSUB${Date.now()}`
    const r7 = await req('POST', '/erp/md/customer', {
      token, body: {
        partnerCode: CODE_SUB, partnerName: R7 + '子', partnerType: 'customer', roles: 'CUSTOMER',
        taxNumber: '91310000E2E0001X', bankName: 'E2E银行', bankAccount: '6222000000000001',
        bankAddress: 'E2E开户行地址', address: 'E2E路 1 号', province: '上海市', city: '上海市', district: '浦东新区',
      },
    })
    const id7 = r7.json?.data?.id
    ok('建档带回 id', id7 != null, `data.id=${id7}`)
    ok('party_cert：税务登记落成 TAX 一条',
      num(`SELECT count(*) FROM party_cert
            WHERE party_id=${id7} AND cert_type='TAX' AND cert_no='91310000E2E0001X' AND deleted=0`) === 1,
      `cert_no=${scalar(`SELECT cert_no FROM party_cert WHERE party_id=${id7} AND cert_type='TAX'`)}`)
    ok('party_bank：落成**默认**账户一条（键 = (party_id) WHERE is_default=1）',
      num(`SELECT count(*) FROM party_bank
            WHERE party_id=${id7} AND is_default=1 AND bank_account='6222000000000001' AND deleted=0`) === 1,
      `account=${scalar(`SELECT bank_account FROM party_bank WHERE party_id=${id7} AND is_default=1`)}`)
    ok('party_address：落成注册地址一条（address_type=1）',
      num(`SELECT count(*) FROM party_address
            WHERE party_id=${id7} AND address_type=1 AND city='上海市' AND deleted=0`) === 1,
      `city=${scalar(`SELECT city FROM party_address WHERE party_id=${id7} AND address_type=1`)}`)

    // ⚠️ 清空语义：只 upsert 不软删的话，用户删掉银行账号后镜像里还留着一条旧账户，
    //    而这类"多出来的"漂移**对账脚本看不见**（它只比对"缺"）。
    await req('PUT', `/erp/md/customer/${id7}`, { token, body: { bankName: '', bankAccount: '' } })
    ok('清空银行信息后，镜像的默认账户被**软删**（不留陈旧账户）',
      num(`SELECT count(*) FROM party_bank WHERE party_id=${id7} AND deleted=0`) === 0
        && num(`SELECT count(*) FROM party_bank WHERE party_id=${id7} AND deleted=1`) === 1,
      `未删=${num(`SELECT count(*) FROM party_bank WHERE party_id=${id7} AND deleted=0`)}`)

    section('⑧ 删除路径：删主体必须把**子表也一起软删**（否则留下悬空行）')
    // 为什么单列一组：批 2b 把三张子表纳入双写时**只加了"写"、忘了"删"** ⇒
    // 删掉主体后它的证件/银行/地址行还挂着 deleted=0、指向一个已软删的主体。
    // 而 V11.520.0/V11.521.0 的迁移自检里恰好有"子表不悬空"这条断言 ⇒ 这个洞会在
    // **下一次跑迁移**时才炸出来（那时已经隔了一版，难定位）。删主体一并软删才是对的语义：
    // 证件/银行/地址是主体的附属物，主体没了它们就不该在。
    const R8 = `${PREFIX}${Date.now()}`
    const CODE_DEL = `E2EDWDEL${Date.now()}`
    const r8 = await req('POST', '/erp/md/customer', {
      token, body: {
        partnerCode: CODE_DEL, partnerName: R8 + '删', partnerType: 'customer', roles: 'CUSTOMER',
        taxNumber: '91310000E2EDEL001', bankName: 'E2E待删银行', bankAccount: '6222000000000099',
        address: 'E2E待删路 9 号', province: '北京市', city: '北京市', district: '朝阳区',
      },
    })
    const id8 = r8.json?.data?.id
    /** 五类"跟着主体走"的行各还剩几条未删（0 = 已全部软删） */
    const alive8 = () => ({
      主档: num(`SELECT count(*) FROM party          WHERE id=${id8}         AND deleted=0`),
      边: num(`SELECT count(*) FROM party_tenant   WHERE party_id=${id8}   AND deleted=0`),
      证件: num(`SELECT count(*) FROM party_cert     WHERE party_id=${id8}   AND deleted=0`),
      银行: num(`SELECT count(*) FROM party_bank     WHERE party_id=${id8}   AND deleted=0`),
      地址: num(`SELECT count(*) FROM party_address  WHERE party_id=${id8}   AND deleted=0`),
    })
    // ⚠️ 删前必须确认这些行**本来就在**：否则"删后都是 0"可能只是因为从来没建过（假绿）
    const before8 = alive8()
    ok('删前：主档 + 边 + 证件 + 银行 + 地址五类行都在',
      Object.values(before8).every(v => v === 1), JSON.stringify(before8))

    const del = await req('DELETE', `/erp/md/customer/${id8}`, { token })
    ok('删除成功（HTTP 200）', del.status === 200, `status=${del.status}`)
    const after8 = alive8()
    ok('主档 / 边 / 证件 / 银行 / 地址**全部**跟着软删（不留悬空行）',
      Object.values(after8).every(v => v === 0), JSON.stringify(after8))
    ok('biz_party 自己也软删了',
      num(`SELECT count(*) FROM biz_party WHERE id=${id8} AND deleted=0`) === 0,
      `未删=${num(`SELECT count(*) FROM biz_party WHERE id=${id8} AND deleted=0`)}`)

    section('⑨ 角色变了：不再适用的方向必须**软删**（同属"写≠删"不对称）')
    // ⑬ 裁定"角色可以并存"，同样意味着**角色可以变**（客户 → 供应商）。
    // 只 upsert 适用方向、不清理旧方向的话，会留下一条"曾经是客户"的陈旧 SALE 边 ——
    // 而它**对账脚本看不见**（脚本只数"缺边"、不数"多边"）⇒ 切读后这个主体会同时是客户和供应商。
    const R9 = `${PREFIX}${Date.now()}`
    const CODE_ROLE = `E2EDWROLE${Date.now()}`
    const r9 = await req('POST', '/erp/md/customer', {
      token, body: { partnerCode: CODE_ROLE, partnerName: R9 + '转', partnerType: 'customer', roles: 'CUSTOMER' },
    })
    const id9 = r9.json?.data?.id
    const edge9 = (dir, del) =>
      num(`SELECT count(*) FROM party_tenant WHERE party_id=${id9} AND direction='${dir}' AND deleted=${del}`)
    ok('建时是客户 ⇒ SALE 边在、PURCHASE 边不在', edge9('SALE', 0) === 1 && edge9('PURCHASE', 0) === 0,
      `SALE=${edge9('SALE', 0)}, PURCHASE=${edge9('PURCHASE', 0)}`)

    const conv = await req('PUT', `/erp/md/customer/${id9}`, {
      token, body: { partnerType: 'supplier', roles: 'SUPPLIER' },
    })
    ok('改类型成功（HTTP 200）', conv.status === 200, `status=${conv.status}`)
    ok('改成供应商后：PURCHASE 边在、**SALE 边被软删**（不留"曾经是客户"的陈旧边）',
      edge9('PURCHASE', 0) === 1 && edge9('SALE', 0) === 0 && edge9('SALE', 1) === 1,
      `SALE(未删/已删)=${edge9('SALE', 0)}/${edge9('SALE', 1)}, PURCHASE(未删)=${edge9('PURCHASE', 0)}`)
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
