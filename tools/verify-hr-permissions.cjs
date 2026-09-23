/*
 * 人力资源模块 · 权限落点真机探针（只读，不写业务数据）
 *
 * 验证 tools/grant-hr-permissions.py + 迁移 V11.502.0 之后：
 *   ① 非超管（e2e_hr_ta / SYSTEM_ADMIN）持有 hr:* ⇒ HR 接口**不再 403**
 *   ② 新权限码 hr:salary:delete / hr:performance:delete 真正生效（挂在删除端点上）
 *   ③ 没被授予的 hr:salary:* 仍然**必须 403**（薪资保密没被顺手放开）
 *
 * ⚠️ 用的是**不存在的 id**（999999999999）⇒ 走到业务层报「记录不存在」，
 *    不会真的删掉任何数据；只看 HTTP 状态区分 403 与「已放行”。
 *
 * 用法：node tools/verify-hr-permissions.cjs
 */
const http = require('http')

const PORT = Number(process.env.HR_PORT || 5655)
const TENANT = process.env.E2E_TENANT || '系统租户'
const MISSING_ID = '999999999999'

let pass = 0
let fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log('PASS\t' + name + (detail ? '\t' + detail : '')) }
  else { fail++; console.log('FAIL\t' + name + (detail ? '\t' + detail : '')) }
}

function req(method, p, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers.Authorization = `Bearer ${token}`
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: '/api' + p, method, headers }, res => {
      let buf = ''
      res.on('data', c => { buf += c })
      res.on('end', () => {
        let json = null
        try { json = JSON.parse(buf) } catch (e) { /* 非 JSON */ }
        resolve({ status: res.statusCode, json, raw: buf })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login(username) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    username, password: process.env.E2E_PWD || 'admin123', tenantName: TENANT,
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`登录失败(${username}): ` + JSON.stringify(res.json).slice(0, 200))
  return token
}

const is403 = r => r.status === 403 || Number(r.json?.code) === 403
const label = r => `status=${r.status} code=${r.json?.code} msg=${String(r.json?.message || '').slice(0, 40)}`

;(async () => {
  const token = await login('e2e_hr_ta')   // 非超管：SYSTEM_ADMIN

  const info = await req('GET', '/auth/userinfo', null, token)
  const perms = info.json?.data?.permissions || []
  console.log(`账号=e2e_hr_ta 权限码数=${perms.length}`)
  console.log(`  含 hr:salary:delete      : ${perms.includes('hr:salary:delete')}`)
  console.log(`  含 hr:performance:delete : ${perms.includes('hr:performance:delete')}`)
  console.log(`  含 hr:salary:list        : ${perms.includes('hr:salary:list')}（按设计应为 false）`)
  console.log()

  // ① 员工查询（此前非超管必 403）
  const emp = await req('GET', '/hr/employees/page?pageNum=1&pageSize=1', null, token)
  check('① 员工查询已放行（此前非超管必 403）', !is403(emp), label(emp))

  // ② 删除类新权限码生效：非超管点删除 → 不放行即通过（应报业务错/200，而不是 403）
  const delPerf = await req('DELETE', `/hr/performance/${MISSING_ID}`, null, token)
  check('② hr:performance:delete 已生效（删除端点不再 403，止于业务校验）', !is403(delPerf), label(delPerf))

  // ③ 薪资保密：hr:salary:* **整组**没授给系统管理员（含新增的 :delete）
  //    ⇒ 薪资删除也必须 403 —— 这是设计口径，不是缺陷。
  const delPay = await req('DELETE', `/hr/salary/payment/${MISSING_ID}`, null, token)
  check('③ 薪资删除仍被拒（hr:salary:* 整组未授，薪资保密口径一致）', is403(delPay), label(delPay))

  // ④ 薪资保密：没授予的 hr:salary:* 必须仍然 403
  const salList = await req('GET', '/hr/salary/payment/page?pageNum=1&pageSize=1', null, token)
  check('④ 薪资查询仍被拒（薪资保密未被顺手放开）', is403(salList), label(salList))
  const salGen = await req('POST', '/hr/salary/payment/generate?paymentMonth=2026-09', null, token)
  check('⑤ 薪资生成仍被拒', is403(salGen), label(salGen))

  // ⑤ 对照：超管不受影响
  const adminToken = await login('e2e_hr')
  const adminSal = await req('GET', '/hr/salary/payment/page?pageNum=1&pageSize=1', null, adminToken)
  check('⑥ 超管看薪资不受影响（对照组）', !is403(adminSal), label(adminSal))

  console.log(`\n合计 PASS=${pass} FAIL=${fail}`)
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => { console.error(e); process.exit(1) })
