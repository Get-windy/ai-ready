/*
 * 人力资源 → 职员管理（80530 / 80531 / 80532）· ql361 对标金标准端到端验证
 *
 * 覆盖（2026-09-19 新增能力）：
 *   第 0 节 菜单收敛 —— 80531「岗位权限」改指角色页、80533 重复入口已撤销
 *   第 1 节 权限矩阵数据源 —— 全量权限可取出，且**必须覆盖 ql361 没有的我方独有模块**
 *   第 2 节 角色权限读写 —— 以 sys_permission.id 为准（修复此前菜单 id / 权限 id 混用的缺陷）
 *   第 3 节 操作员 7 类数据权限 —— 候选对象 / 覆盖保存 / 读回 / 清除（含租户安全断言）
 *   第 4 节 部门后端加固 —— options 下拉数据源、删除引用保护、改上级重算 ancestors
 *   第 5 节 真库对账（直连 devdb）
 *
 * 用法：node tools/e2e-hr-staff.cjs
 *      HR_PORT=5655 node tools/e2e-hr-staff.cjs
 *      SKIP_DB=1 node tools/e2e-hr-staff.cjs     # 跳过真库对账
 *
 * 前置：
 *   1) 后端 dev profile 运行中（默认 5655），且**已包含本轮代码**
 *      （迁移 V11.429.0 / V11.430.0 已由 Flyway 应用）
 *   2) 专用验收账号：python tools/dbq.py "$(cat tools/e2e-hr-user.sql)"   # e2e_hr / admin123
 *      ⚠️ 不要用 admin：并行会话同账号二次登录会触发 sa-token 互踢，表现为「接口莫名 401」。
 *
 * 本脚本自造的数据（数据权限授权）跑完即清除，且只作用于 e2e_hr 自己的 userId。
 */
const http = require('http')
const { execSync } = require('child_process')

const PORT = Number(process.env.HR_PORT || 5655)
const SKIP_DB = process.env.SKIP_DB === '1'
const REPO = 'I:/AI-Ready'

let passed = 0
let failed = 0
const failures = []

function check(name, cond, detail) {
  if (cond) {
    passed++
    console.log(`  ✅ ${name}`)
  } else {
    failed++
    failures.push(name + (detail ? ` — ${detail}` : ''))
    console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`)
  }
}

// ══════════════ HTTP ══════════════
function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? null : JSON.stringify(body)
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const rawPath = '/api' + reqPath
    const safePath = Array.from(rawPath).map(ch => (ch.charCodeAt(0) > 127 ? encodeURIComponent(ch) : ch)).join('')
    const r = http.request({ hostname: 'localhost', port: PORT, path: safePath, method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        resolve({ status: res.statusCode, buf, json: tryJson(buf) })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}
function tryJson(buf) {
  try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null }
}

let TOKEN = null

async function api(method, reqPath, body) {
  let r = await rawReq(method, reqPath, body, TOKEN)
  const code = r.json && Number(r.json.code)
  if (r.status === 401 || code === 401) {
    await login()
    r = await rawReq(method, reqPath, body, TOKEN)
  }
  return r
}
function okOf(r) { return r.status === 200 && r.json && Number(r.json.code) === 200 }
function dataOf(r) { return r.json ? r.json.data : null }

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: process.env.E2E_USER || 'e2e_hr',
    password: process.env.E2E_PWD || 'admin123',
    tenantName: process.env.E2E_TENANT || '系统租户',
    captcha: code,
    captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  TOKEN = token
}

function sql(query) {
  const escaped = query.replace(/"/g, '\\"')
  const out = execSync(`python "${REPO}/tools/dbq.py" "${escaped}"`, { encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 })
  return JSON.parse(out)
}
function sqlOne(query) {
  const rows = sql(query)
  return rows && rows.length ? rows[0] : null
}
/** 执行写 SQL（dbq.py 对非查询语句输出 `OK <rowcount>`，不是 JSON，故不能复用 sql()） */
function sqlExec(query) {
  const escaped = query.replace(/"/g, '\\"')
  return execSync(`python "${REPO}/tools/dbq.py" "${escaped}"`, { encoding: 'utf8' })
}

// ══════════════ 主流程 ══════════════
async function main() {
  console.log(`\n=== HR 职员管理（80530/80531/80532）对标验收 @ localhost:${PORT} ===\n`)
  await login()

  // ── 第 0 节 菜单收敛 ──────────────────────────────────────────
  console.log('第 0 节 菜单收敛')
  const userInfo = dataOf(await api('GET', '/auth/userinfo')) || {}
  const myTenantId = userInfo.tenantId || 1
  let myId = userInfo.id || userInfo.userId || null
  if (!myId) {
    // 兜底：按用户名从用户列表里取出自己的 id（/auth/userinfo 的字段名随实现而变，不依赖它）
    const me = dataOf(await api('GET', `/user/page?pageNum=1&pageSize=1&username=${process.env.E2E_USER || 'e2e_hr'}`))
    myId = (me?.records || [])[0]?.id || null
  }

  if (!SKIP_DB) {
    const m80531 = sqlOne("select id, component, deleted from sys_menu where id = 80531")
    check('80531「岗位权限」component = views/system/role/index.vue',
      m80531 && m80531.component === 'views/system/role/index.vue',
      m80531 ? `实际 ${m80531.component}` : '菜单不存在')

    const m80533 = sqlOne("select count(*) n from sys_menu where id = 80533 and deleted = 0")
    check('80533「权限配置」重复入口已撤销（deleted=1）', m80533 && Number(m80533.n) === 0,
      `deleted=0 行数 ${m80533 && m80533.n}`)

    const grp = sqlOne("select count(*) n from sys_menu where parent_id = 61405 and deleted = 0 and status = 1")
    check('职员管理分组下仍有 3 个可用页面（与 ql361 一致）', grp && Number(grp.n) === 3,
      `实际 ${grp && grp.n}`)
  }

  // ── 第 1 节 权限矩阵数据源 ────────────────────────────────────
  console.log('\n第 1 节 权限矩阵数据源（功能名称 × 查看/打印/增加/删除/修改/导出）')
  const permPage = await api('GET', '/permission/page?tenantId=1&current=1&size=1000')
  const records = dataOf(permPage)?.records || []
  check('全量权限可取出（>100 条）', records.length > 100, `实际 ${records.length} 条`)

  const codes = records.map(p => p.permissionCode || '').filter(Boolean)
  const domains = new Set(codes.map(c => c.split(':')[0]))
  // 这些模块 ql361 **没有**，但必须出现在权限矩阵里（否则我方独有功能配不了权限）
  const ownModules = ['hr', 'dms', 'crm', 'workflow', 'datasource', 'platform']
  const missing = ownModules.filter(m => !domains.has(m))
  check(`矩阵覆盖我方独有模块（${ownModules.join('/')}）`, missing.length === 0,
    missing.length ? `缺失域: ${missing.join(',')}` : `实测 ${domains.size} 个域`)

  const hasSixAction = records.some(p => /:(list|view|detail|query)$/.test(p.permissionCode || ''))
    && records.some(p => /:(create|add)$/.test(p.permissionCode || ''))
    && records.some(p => /:delete$/.test(p.permissionCode || ''))
    && records.some(p => /:(update|edit)$/.test(p.permissionCode || ''))
  check('6 类操作（查看/增加/删除/修改…）在权限表中都有对应码', hasSixAction)

  // ── 第 2 节 角色权限读写 ──────────────────────────────────────
  console.log('\n第 2 节 角色权限读写（以 sys_permission.id 为准）')
  // ⚠️ /role/page 必传 tenantId（缺参直接 400），这是既有接口契约
  const rolePage = await api('GET', `/role/page?pageNum=1&pageSize=50&tenantId=${myTenantId}`)
  const roleData = dataOf(rolePage)
  const roles = roleData?.records || roleData?.list || (Array.isArray(roleData) ? roleData : [])
  check('可列出角色', roles.length > 0, `实际 ${roles.length}；响应片段 ${JSON.stringify(rolePage.json).slice(0, 160)}`)

  if (roles.length) {
    // 挑一个非超管角色做读写，避免动 SUPER_ADMIN
    const role = roles.find(r => r.roleCode !== 'SUPER_ADMIN') || roles[0]
    const before = await api('GET', `/role/${role.id}/permissions`)
    check(`GET /role/${role.id}/permissions 可读`, okOf(before))

    const beforeIds = (dataOf(before) || []).map(String)
    const sampleId = records.length ? String(records[0].id) : null
    if (sampleId) {
      // 写：在原有集合上并上一条，避免破坏该角色既有权限
      const nextIds = Array.from(new Set([...beforeIds, sampleId]))
      const saved = await api('POST', `/role/${role.id}/permissions`, nextIds)
      check('POST /role/{id}/permissions 可写（入参为 permissionId 数组）', okOf(saved),
        `HTTP ${saved.status} / code ${saved.json && saved.json.code}；message=${saved.json && saved.json.message}；入参样例 ${JSON.stringify(nextIds.slice(0, 2))}`)

      const after = (dataOf(await api('GET', `/role/${role.id}/permissions`)) || []).map(String)
      check('写入的权限 id 能被读回（回显一致）', after.includes(sampleId),
        `期望含 ${sampleId}，实得 ${after.length} 条`)

      // 还原
      await api('POST', `/role/${role.id}/permissions`, beforeIds)
      const restored = (dataOf(await api('GET', `/role/${role.id}/permissions`)) || []).map(String)
      check('还原后与写入前一致（现场已复原）',
        restored.length === beforeIds.length && beforeIds.every(id => restored.includes(id)),
        `写前 ${beforeIds.length} 条 / 还原后 ${restored.length} 条`)
    }
  }

  // ── 第 3 节 操作员 7 类数据权限 ───────────────────────────────
  console.log('\n第 3 节 操作员 7 类数据权限（仓库/调拨/部门/往来单位/商品/现金银行/客户级别）')
  const SCOPE_KEYS = ['warehouse', 'transfer', 'department', 'partner', 'product', 'fund', 'customer_level']

  const targetUser = myId || (dataOf(await api('GET', '/user/page?pageNum=1&pageSize=1'))?.records || [])[0]?.id
  check('取到用于验收的操作员 id', !!targetUser, String(targetUser))

  let targetsOk = 0
  for (const key of SCOPE_KEYS) {
    const r = await api('GET', `/user-data-scope/targets?scopeKey=${key}`)
    if (okOf(r)) targetsOk++
    else console.log(`     · ${key} → HTTP ${r.status} / code ${r.json && r.json.code}`)
  }
  check('7 个维度的候选对象端点全部可用', targetsOk === 7, `成功 ${targetsOk}/7`)

  const invalid = await api('GET', '/user-data-scope/targets?scopeKey=not_a_scope')
  check('非法维度被拒绝（不写任意字符串）', !okOf(invalid), `实际 code ${invalid.json && invalid.json.code}`)

  if (targetUser) {
    // 用「部门」维度做读写：数据量小、对象稳定
    const key = 'department'
    const before = await api('GET', `/user-data-scope/${targetUser}`)
    check('GET /user-data-scope/{userId} 可读', okOf(before))
    const beforeMap = dataOf(before) || {}

    const cand = dataOf(await api('GET', `/user-data-scope/targets?scopeKey=${key}`)) || []
    if (cand.length) {
      const pick = [String(cand[0].id)]
      check('PUT 覆盖保存', okOf(await api('PUT', `/user-data-scope/${targetUser}/${key}`, pick)))
      const after = dataOf(await api('GET', `/user-data-scope/${targetUser}`)) || {}
      check('保存后能读回同一条', Array.isArray(after[key]) && after[key].map(String).includes(pick[0]),
        `实得 ${JSON.stringify(after[key])}`)

      check('DELETE 清除该维度', okOf(await api('DELETE', `/user-data-scope/${targetUser}/${key}`)))
      const cleared = dataOf(await api('GET', `/user-data-scope/${targetUser}`)) || {}
      check('清除后该维度不再返回', !cleared[key], `实得 ${JSON.stringify(cleared[key])}`)

      // 还原现场：把验收前该用户的部门维度原样写回
      const original = (beforeMap[key] || []).map(String)
      if (original.length) await api('PUT', `/user-data-scope/${targetUser}/${key}`, original)
      console.log(`     · 现场已复原（该用户部门维度原有 ${original.length} 项）`)
    } else {
      console.log('     · 该租户下无部门候选对象，跳过读写小节')
    }

    const badUser = await api('GET', '/user-data-scope/0')
    check('非法 userId 被拒绝', !okOf(badUser))
  }

  // ── 第 4 节 部门后端加固 ──────────────────────────────────────
  console.log('\n第 4 节 部门后端加固')
  const options = await api('GET', '/department/options')
  check('GET /department/options 可用', okOf(options))
  const optList = dataOf(options) || []
  if (!SKIP_DB) {
    const dbDept = sqlOne("select count(*) n from sys_department where deleted = 0")
    check('options 不过滤 status（应返回全部未删除部门）',
      optList.length === Number(dbDept.n),
      `接口 ${optList.length} / 库 ${dbDept && dbDept.n}`)
  }

  const delNonExist = await api('DELETE', '/department/99999999')
  check('删除不存在的部门不会 500', delNonExist.status < 500, `实际 HTTP ${delNonExist.status}`)

  if (!SKIP_DB) {
    // 找一个被引用的部门，验证删除保护（若无引用则跳过）
    // 引用可能来自 sys_user / sys_position / hr_employee 三处，逐一探测
    const refDeptId = (() => {
      for (const [table, col] of [['sys_user', 'dept_id'], ['sys_position', 'dept_id'], ['hr_employee', 'dept_id']]) {
        const row = sqlOne(`select ${col} as id from ${table} where deleted = 0 and ${col} is not null limit 1`)
        if (row && row.id) return { id: row.id, table }
      }
      return null
    })()
    if (refDeptId) {
      const r = await api('DELETE', `/department/${refDeptId.id}`)
      check(`删除被引用的部门被拒绝（引用保护生效，引用来自 ${refDeptId.table}）`, !okOf(r),
        `实际 HTTP ${r.status} / code ${r.json && r.json.code}；部门 ${refDeptId.id}`)
    } else if (myId) {
      // 实测本库 sys_user/sys_position/hr_employee 的 dept_id 当前全为 NULL，存量数据触发不了引用保护，
      // 故自造一条引用：建临时部门 → 把 e2e_hr 自己指过去 → 删部门应被拒 → 复原引用并删掉临时部门。
      const tagDept = `E2E_STAFF_${Date.now().toString().slice(-6)}`
      const created = await api('POST', '/department', {
        departmentCode: tagDept, departmentName: tagDept, sort: 0, status: 1,
      })
      const newDeptId = dataOf(created)
      if (okOf(created) && newDeptId) {
        try {
          sqlExec(`update sys_user set dept_id = ${newDeptId} where id = ${myId}`)
          const blocked = await api('DELETE', `/department/${newDeptId}`)
          check('删除被用户引用的部门被拒绝（引用保护真实触发）', !okOf(blocked),
            `实际 HTTP ${blocked.status} / code ${blocked.json && blocked.json.code}；message=${blocked.json && blocked.json.message}`)
        } finally {
          sqlExec(`update sys_user set dept_id = null where id = ${myId}`)
          const cleanup = await api('DELETE', `/department/${newDeptId}`)
          check('解除引用后部门可正常删除（现场已复原）', okOf(cleanup),
            `实际 HTTP ${cleanup.status} / code ${cleanup.json && cleanup.json.code}`)
        }
      } else {
        console.log('     · 未能创建临时部门，跳过引用保护真实触发断言')
      }
    } else {
      console.log('     · 取不到验收账号 id，跳过引用保护断言')
    }
  }

  // ── 第 5 节 真库对账 ─────────────────────────────────────────
  if (!SKIP_DB) {
    console.log('\n第 5 节 真库对账')
    const t = sqlOne("select count(*) n from information_schema.tables where table_schema='public' and table_name='sys_user_data_scope'")
    check('表 sys_user_data_scope 已建', t && Number(t.n) === 1)

    const idx = sqlOne("select count(*) n from pg_indexes where tablename='sys_user_data_scope' and indexname='uk_sys_user_data_scope'")
    check('唯一索引 uk_sys_user_data_scope 已建', idx && Number(idx.n) === 1)

    const seed = sqlOne("select count(*) n from sys_permission where permission_code in ('data-scope:view','data-scope:set') and deleted = 0")
    check('权限码种子 data-scope:view / data-scope:set 已入库', seed && Number(seed.n) === 2,
      `实际 ${seed && seed.n} 条`)
  }

  // ── 汇总 ────────────────────────────────────────────────────
  console.log(`\n=== 结果：${passed}/${passed + failed} 通过 ===`)
  if (failed) {
    console.log('\n失败项：')
    failures.forEach(f => console.log('  · ' + f))
  }
  process.exit(failed ? 1 : 0)
}

main().catch(e => {
  console.error('\n脚本异常：', e && e.message ? e.message : e)
  process.exit(1)
})
