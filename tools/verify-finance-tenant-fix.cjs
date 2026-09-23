/*
 * 财务模块「租户硬编码清理」验证（2026-09-23）
 *
 * 背景：本次把财务域 19 处查询侧的 DEFAULT_TENANT_ID 换成会话租户（currentTenantId()），
 *       并删除了 17 处写入侧的 setTenantId(1L)（改由 MyBatisPlusConfig.insertFill 填会话租户）。
 *
 * 本脚本验证「没改坏」：
 *   ① 建单后 tenantId 不再靠硬编码 → 落库值应等于**会话租户**（超管会话=1），且**不得为 NULL**
 *      （若 insertFill 未生效，tenantId 会是 NULL —— 这正是要抓的失败模式）；
 *   ② 建单前的重名校验（走 currentTenantId()）在同一租户内仍能命中；
 *   ③ 辅助核算 enable 端点仍正常（本次改动过该文件的租户取值）。
 *
 * ⚠️ 口径说明：租户 2 无任何财务权限码（`sys_role` 中 E2E_T2_ADMIN 财务码 0 个），
 *    调财务接口必 403，故**无法在非 1 租户上真机验证**。
 *    「非 1 租户会落成 2」的依据是代码证据：insertFill → getCurrentTenantIdValue()
 *    → Sa-Token session 的 tenantId（登录时写入）。本脚本只覆盖回归面。
 *
 * 用法：node tools/verify-finance-tenant-fix.cjs     （需后端 5655 在跑）
 */
const http = require('http')
const { execFileSync } = require('child_process')
const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe'
const PGENV = { ...process.env, PGPASSWORD: 'devuser123' }
const TEST_CODE = 'E2E_TENANT_FIX'

function q(sql) {
  return execFileSync(PSQL, ['-h', 'localhost', '-p', '5432', '-U', 'devuser', '-d', 'devdb', '-t', '-A', '-c', sql],
    { env: PGENV, encoding: 'utf8' }).trim()
}
function call(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers['Authorization'] = 'Bearer ' + token
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: 5655, path: '/api' + path, method, headers },
      res => { let s = ''; res.on('data', c => s += c); res.on('end', () => resolve({ status: res.statusCode, body: s })) })
    r.on('error', reject); if (data) r.write(data); r.end()
  })
}

let pass = 0, fail = 0
function check(desc, ok, detail) {
  console.log(`${ok ? '✅' : '❌'} ${desc}${detail ? ' → ' + detail : ''}`)
  ok ? pass++ : fail++
}

;(async () => {
  // 先清理可能的历史残留
  q(`delete from finance_auxiliary_type where type_code='${TEST_CODE}'`)

  const cap = await call('GET', '/auth/captcha')
  const full = JSON.parse(cap.body)
  const code = [...Buffer.from(full.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const lg = await call('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: full.data.uuid,
  })
  const token = JSON.parse(lg.body).data.token
  console.log('登录(系统租户/超管):', lg.status, '\n')

  // ① 建单 → 落库 tenant_id 必须等于会话租户，且不能为 NULL
  const created = await call('POST', '/erp/finance/auxiliary/type',
    { typeCode: TEST_CODE, typeName: '租户修复验证', enabled: true, sort: 999 }, token)
  check('创建辅助核算类型返回 200', created.status === 200, `HTTP ${created.status}`)
  const row = q(`select coalesce(tenant_id::text,'NULL') from finance_auxiliary_type where type_code='${TEST_CODE}'`)
  check('落库 tenant_id = 会话租户(1)', row === '1', `tenant_id=${row}`)
  check('insertFill 已生效（tenant_id 非 NULL）', row !== 'NULL' && row !== '', `tenant_id=${row}`)

  // ② 同租户内重名校验仍命中（走 currentTenantId() 的查询路径）
  const dup = await call('POST', '/erp/finance/auxiliary/type',
    { typeCode: TEST_CODE, typeName: '重复验证', enabled: true }, token)
  check('同租户重复编码被拒（重名校验查询有效）', dup.status >= 400 || /已存在/.test(dup.body), `HTTP ${dup.status} ${dup.body.slice(0, 60)}`)

  // ③ 列表能查到（证明查询侧租户条件正确、数据可见）
  const list = await call('GET', '/erp/finance/auxiliary/type/list', null, token)
  check('列表能查到新建项（查询侧租户条件正确）', list.status === 200 && list.body.includes(TEST_CODE), `HTTP ${list.status}`)

  // ④ enable 端点仍正常（本次改动过该文件的租户取值）
  const newId = q(`select id from finance_auxiliary_type where type_code='${TEST_CODE}'`)
  if (newId) {
    const en = await call('PUT', `/erp/finance/auxiliary/type/${newId}/enable?enabled=false`, null, token)
    const enAfter = q(`select enabled from finance_auxiliary_type where id=${newId}`)
    check('enable 端点仍正常且真实落库', en.status === 200 && enAfter === 'f', `HTTP ${en.status}, enabled=${enAfter}`)
  }

  // 清理测试数据
  q(`delete from finance_auxiliary_type where type_code='${TEST_CODE}'`)
  const left = q(`select count(*) from finance_auxiliary_type where type_code='${TEST_CODE}'`)
  check('测试数据已清理', left === '0', `残留 ${left} 行`)

  console.log(`\n===== 结果：${pass} 通过 / ${fail} 失败 =====`)
  process.exit(fail > 0 ? 1 : 0)
})().catch(e => { console.error('ERR', e.message); process.exit(1) })
