#!/usr/bin/env node
/**
 * 「配置按租户隔离」验证（2026-09-21 专项）· 需先重启后端
 *
 * 用途：验证消灭硬编码租户（`SysConfigServiceImpl.CURRENT_TENANT_ID = 1L` 与
 * `UserPageConfigController` 的 `config.setTenantId(1L)`）之后，**运行时**真的按租户隔离。
 *
 * ⚠️ 为什么必须跑真机：单测只能钉住「传进 Mapper 的租户是谁」，钉不住
 *    「会话里到底解析出哪个租户」—— 而正是后者错了（会话租户被忽略、一律读租户 1）。
 *    两向断言才有意义：
 *      ① 租户 2 **读不到**租户 1 的值（隔离生效）；
 *      ② 租户 2 **读得到**平台行（0）的值（回落没被一起改坏）。
 *    只断言①的话，「所有配置都读不到」也能过。
 *
 * 副作用与还原（脚本自清理，finally 必执行）：
 *   - 临时给角色 `E2E租户2管理员` 挂 3 个 `system:config:*` 码（租户 2 原本没有，
 *     否则拿不到任何配置接口的 200，只能观察到 403）；
 *   - 写入的配置键一律带 `e2e.cfgtenant.` / `user_page_config:e2e-verify:` 前缀，
 *     结束时整片删除。
 *
 * 用法：
 *   node tools/verify-config-tenant.cjs
 *   PORT=5655 node tools/verify-config-tenant.cjs
 */
const { execFileSync } = require('child_process')
const path = require('path')

const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`
const REPO = path.resolve(__dirname, '..')

const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }            // 租户 1
const TENANT2 = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2' }  // 租户 2

const ROLE_T2_ADMIN = 2099000000000009031n
// 临时的权限绑定 id（固定值，便于精确清理；不与既有 id 冲突）
const TEMP_RP_IDS = [2099000000000099001n, 2099000000000099002n, 2099000000000099003n]
const TEMP_PERMS = [2065122955521396737n, 2065122955575922689n, 2065122955626254338n]

const KEY_T2 = 'e2e.cfgtenant.probe'        // 租户 2 自己写
const KEY_PLATFORM = 'e2e.cfgtenant.platform' // 平台行（0）
const KEY_T1ONLY = 'e2e.cfgtenant.t1only'     // 只有租户 1 有
const USERCFG_MODULE = 'e2e-verify'
const USERCFG_PAGE = 'probe'

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

/** 走 JDBC（tools/sql.cjs）执行 SQL；返回原始文本 */
function sql(stmt) {
  return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt], {
    cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024,
  })
}

/**
 * 解析 sql.cjs 的表格输出 → 二维数组。
 *
 * ⚠️ 输出长这样：表头 / 一串 '-' / 数据行 / 末尾还有一行 `(N rows)` 页脚。
 * 第一版只按「行号 ≥ 2」取数据，把页脚 `(1 rows)` 也当成了数据行。
 */
function rowsOf(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').trim().split('\n')
    .filter(l => l.trim() && !/^\(\d+ rows\)$/.test(l.trim()) && !/^-+$/.test(l.trim()))
  return lines.slice(1).map(l => l.split('|').map(s => s.trim()))
}

/** 执行 SQL 并取出「单值」结果（第一行第一列）；无行返回 null */
function scalar(stmt) {
  const rows = rowsOf(stmt)
  if (!rows.length) return null
  return rows[0][0] === 'null' ? null : rows[0][0]
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

/** 登录（走验证码流程，与其它 e2e 脚本一致） */
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

const rpIdSql = () => TEMP_RP_IDS.map(String).join(',')

function cleanup() {
  try {
    sql(`DELETE FROM sys_project_config WHERE config_key LIKE 'e2e.cfgtenant.%' `
      + `OR config_key LIKE 'user_page_config:${USERCFG_MODULE}:%'`)
    console.log('  [清理] 已删除本脚本写入的配置行')
  } catch (e) { console.log(`  [清理] 配置行删除失败: ${e.message}`) }
  try {
    sql(`DELETE FROM sys_role_permission WHERE id IN (${rpIdSql()})`)
    console.log('  [清理] 已撤销临时授予的 system:config:* 权限')
  } catch (e) { console.log(`  [清理] 权限撤销失败: ${e.message}`) }
}

/** 读取某键在库中的行（→ 列表 ["<tenant_id>@<deleted>"]），用于断言"写到了哪个租户" */
function dbRows(key) {
  return rowsOf(`SELECT tenant_id || '@' || deleted AS r FROM sys_project_config `
    + `WHERE config_key = '${key}' ORDER BY tenant_id`).map(r => r[0])
}

;(async () => {
  console.log(`验证目标: ${BASE}`)
  let adminToken = null, t2Token = null

  try {
    // ── 授权 → 登录（顺序不能反：先给权限，登录后才拿得到新权限）──
    sql(`DELETE FROM sys_role_permission WHERE id IN (${rpIdSql()})`)
    TEMP_RP_IDS.forEach((id, i) => {
      sql(`INSERT INTO sys_role_permission (id, role_id, permission_id, tenant_id, create_time) `
        + `VALUES (${id}, ${ROLE_T2_ADMIN}, ${TEMP_PERMS[i]}, 2, CURRENT_TIMESTAMP)`)
    })
    console.log('临时授权完成: 角色 E2E租户2管理员 += system:config:{list,update,delete}')

    adminToken = await login(ADMIN)
    console.log(`登录成功: ${ADMIN.u}（超管 / 租户 1）`)
    t2Token = await login(TENANT2)
    console.log(`登录成功: ${TENANT2.u}（非超管 / 租户 2）`)

    // ── 探针自检：403 说明授权或登录没生效，后面全部结论都无意义 ──
    const canary = await req('GET', `/system/config/value/${KEY_T2}`, { token: t2Token })
    if (canary.status !== 200) {
      console.error(`\n!! 探针自检失败：租户 2 访问配置接口返回 ${canary.status} —— `
        + `先把授权/登录修好，否则后续断言没有意义。响应: ${canary.text.slice(0, 200)}`)
      process.exitCode = 1
      return
    }

    // ══════════ ① 写：落在会话租户上 ══════════
    section('① 写隔离 —— 租户 2 写的配置必须落在租户 2 名下')
    const w = await req('POST', '/system/config/set', {
      token: t2Token,
      body: { key: KEY_T2, value: 'T2-ONLY', type: 'string', group: 'e2e', description: '租户隔离验证' },
    })
    ok('租户 2 写配置返回 200', w.status === 200, `status=${w.status}`)

    const rowsT2 = dbRows(KEY_T2)
    ok('库里只有 1 行（没插重复行）', rowsT2.length === 1, `实际=${JSON.stringify(rowsT2)}`)
    ok('那一行的 tenant_id = 2（**不是** 1）', rowsT2[0] === '2@0', `实际=${rowsT2[0]}`)

    // ══════════ ② 读：本租户能读，别的租户读不到 ══════════
    section('② 读隔离 —— 租户 2 读得到自己的，租户 1 读不到租户 2 的')
    const rT2 = await req('GET', `/system/config/value/${KEY_T2}`, { token: t2Token })
    ok('租户 2 读回自己写的值', rT2.json?.data === 'T2-ONLY', `data=${JSON.stringify(rT2.json?.data)}`)

    const rT1 = await req('GET', `/system/config/value/${KEY_T2}`, { token: adminToken })
    ok('租户 1 读同一键 → 拿不到租户 2 的值',
      rT1.json?.data !== 'T2-ONLY', `data=${JSON.stringify(rT1.json?.data)}`)

    // 给"租户 1 读不到"再加一条正向证据：库里有值、接口却不给
    ok('前置：该键在库中确实存在（不是靠"数据不存在"蒙对的）', dbRows(KEY_T2).length === 1)

    // ══════════ ③ 回落：只回落到平台行（0），不回落到别的租户 ══════════
    section('③ 平台行回落 —— 本租户没有时读平台行；绝不读"某个租户"的值')
    sql(`INSERT INTO sys_project_config (id, tenant_id, config_key, config_value, config_type, config_group, status, deleted) `
      + `VALUES (2099000000000099101, 0, '${KEY_PLATFORM}', 'PLAT', 'string', 'e2e', 0, 0)`)
    sql(`INSERT INTO sys_project_config (id, tenant_id, config_key, config_value, config_type, config_group, status, deleted) `
      + `VALUES (2099000000000099102, 1, '${KEY_T1ONLY}', 'T1-ONLY', 'string', 'e2e', 0, 0)`)

    const p2 = await req('GET', `/system/config/value/${KEY_PLATFORM}`, { token: t2Token })
    ok('租户 2 没配过 → 回落读到平台行(0)的 PLAT', p2.json?.data === 'PLAT', `data=${JSON.stringify(p2.json?.data)}`)

    const t1only = await req('GET', `/system/config/value/${KEY_T1ONLY}`, { token: t2Token })
    ok('租户 1 独有键：租户 2 读不到（回落平台行 → 平台行也没有 → null）',
      t1only.json?.data == null, `data=${JSON.stringify(t1only.json?.data)}`)

    const own1 = await req('GET', `/system/config/value/${KEY_T1ONLY}`, { token: adminToken })
    ok('同一键租户 1 自己读得到（证明上一条不是"接口坏了"）',
      own1.json?.data === 'T1-ONLY', `data=${JSON.stringify(own1.json?.data)}`)

    // ══════════ ④ 写不覆盖平台行（"更新了别人的行"这一类缺陷）══════════
    section('④ 写不越界 —— 租户 2 保存同名键，不得覆盖平台行')
    await req('POST', '/system/config/set', {
      token: t2Token,
      body: { key: KEY_PLATFORM, value: 'T2-OVERRIDE', type: 'string', group: 'e2e' },
    })
    const plat = scalar(`SELECT config_value FROM sys_project_config WHERE config_key='${KEY_PLATFORM}' AND tenant_id=0`)
    ok('平台行(0)的值仍是 PLAT，未被租户 2 改写', plat === 'PLAT', `实际=${plat}`)
    const t2Row = scalar(`SELECT config_value FROM sys_project_config WHERE config_key='${KEY_PLATFORM}' AND tenant_id=2`)
    ok('租户 2 的值写进了自己的新行', t2Row === 'T2-OVERRIDE', `实际=${t2Row}`)
    const p2b = await req('GET', `/system/config/value/${KEY_PLATFORM}`, { token: t2Token })
    ok('租户 2 读到的是自己那份（本租户优先）', p2b.json?.data === 'T2-OVERRIDE', `data=${JSON.stringify(p2b.json?.data)}`)
    const p1b = await req('GET', `/system/config/value/${KEY_PLATFORM}`, { token: adminToken })
    ok('租户 1 仍读到平台行的 PLAT（没被串过去）', p1b.json?.data === 'PLAT', `data=${JSON.stringify(p1b.json?.data)}`)

    // ══════════ ⑤ 删不越界 ══════════
    section('⑤ 删不越界 —— 租户 2 删同名键，只能删掉自己那一行')
    const del = await req('DELETE', `/system/config/${KEY_PLATFORM}`, { token: t2Token })
    ok('租户 2 删配置返回 200', del.status === 200, `status=${del.status}`)
    const platDel = scalar(`SELECT deleted FROM sys_project_config WHERE config_key='${KEY_PLATFORM}' AND tenant_id=0`)
    ok('平台行仍是 deleted=0（没被连带删掉）', platDel === '0', `实际=${platDel}`)
    const t2Del = scalar(`SELECT deleted FROM sys_project_config WHERE config_key='${KEY_PLATFORM}' AND tenant_id=2`)
    ok('租户 2 自己的行已逻辑删除', t2Del === '1', `实际=${t2Del}`)
    const p2c = await req('GET', `/system/config/value/${KEY_PLATFORM}`, { token: t2Token })
    ok('删掉自己的行后又回落到平台行的 PLAT', p2c.json?.data === 'PLAT', `data=${JSON.stringify(p2c.json?.data)}`)

    // ══════════ ⑥ 列表隔离 ══════════
    section('⑥ 列表隔离 —— 配置 Map 里不得出现别的租户的键')
    const map2 = await req('GET', '/system/config/map', { token: t2Token })
    ok('租户 2 的配置 Map 不含租户 1 独有键',
      map2.json?.data && !(KEY_T1ONLY in map2.json.data), `keys=${JSON.stringify(Object.keys(map2.json?.data || {}))}`)
    ok('租户 2 的配置 Map 含自己写的键', map2.json?.data && (KEY_T2 in map2.json.data))

    // ══════════ ⑦ UserPageConfigController 的另一处硬编码租户 ══════════
    section('⑦ 用户页面配置 —— 写入必须落会话租户（原为 setTenantId(1L)）')
    const uc2 = await req('POST', `/system/user-config/${USERCFG_MODULE}/${USERCFG_PAGE}`, {
      token: t2Token, body: { value: '{"who":2}' },
    })
    ok('租户 2 保存页面配置返回 200', uc2.status === 200, `status=${uc2.status}`)
    const ucRowTenant = scalar(`SELECT tenant_id FROM sys_project_config WHERE config_key LIKE `
      + `'user_page_config:${USERCFG_MODULE}:%' ORDER BY id DESC LIMIT 1`)
    ok('该行 tenant_id = 2（**不是** 1）', ucRowTenant === '2', `实际=${ucRowTenant}`)

    const ucRead2 = await req('GET', `/system/user-config/${USERCFG_MODULE}/${USERCFG_PAGE}`, { token: t2Token })
    ok('租户 2 读回自己的页面配置', ucRead2.json?.data === '{"who":2}', `data=${JSON.stringify(ucRead2.json?.data)}`)

    const uc1 = await req('POST', `/system/user-config/${USERCFG_MODULE}/${USERCFG_PAGE}`, {
      token: adminToken, body: { value: '{"who":1}' },
    })
    ok('租户 1 保存同 module/page 返回 200', uc1.status === 200, `status=${uc1.status}`)
    const ucRowTenant1 = scalar(`SELECT tenant_id FROM sys_project_config WHERE config_key LIKE `
      + `'user_page_config:${USERCFG_MODULE}:%' ORDER BY id DESC LIMIT 1`)
    ok('租户 1 的行 tenant_id = 1', ucRowTenant1 === '1', `实际=${ucRowTenant1}`)
    const ucRead2b = await req('GET', `/system/user-config/${USERCFG_MODULE}/${USERCFG_PAGE}`, { token: t2Token })
    ok('租户 2 的值未被租户 1 的保存动到', ucRead2b.json?.data === '{"who":2}', `data=${JSON.stringify(ucRead2b.json?.data)}`)

    // ══════════ ⑧ 回归：租户 1 自己的读写照常 ══════════
    section('⑧ 回归 —— 租户 1 仍是自己的配置的主人')
    await req('POST', '/system/config/set', {
      token: adminToken, body: { key: 'e2e.cfgtenant.admin', value: 'T1-OK', type: 'string', group: 'e2e' },
    })
    const a1 = await req('GET', '/system/config/value/e2e.cfgtenant.admin', { token: adminToken })
    ok('租户 1 写后立即读得到', a1.json?.data === 'T1-OK', `data=${JSON.stringify(a1.json?.data)}`)
    const a1Row = scalar(`SELECT tenant_id FROM sys_project_config WHERE config_key='e2e.cfgtenant.admin'`)
    ok('该行 tenant_id = 1', a1Row === '1', `实际=${a1Row}`)

    // 存量数据回归：租户 1 原有的 21 行配置仍可读（改动前它们就读租户 1）
    const legacy = await req('GET', '/system/config/value/set:menu-config:hidden', { token: adminToken })
    ok('存量键 set:menu-config:hidden 仍可被租户 1 读到',
      legacy.json?.data != null, `data=${JSON.stringify(legacy.json?.data)}`)

  } catch (e) {
    fail++
    console.error(`\n!! 异常中止: ${e.message}`)
  } finally {
    section('清理')
    if (t2Token) {
      await req('DELETE', `/system/config/${KEY_T2}`, { token: t2Token }).catch(() => {})
      await req('DELETE', `/system/config/e2e.cfgtenant.admin`, { token: adminToken }).catch(() => {})
    }
    cleanup()
  }

  console.log(`\n结果: PASS=${pass} FAIL=${fail}`)
  if (fail > 0) process.exitCode = 1
})()
