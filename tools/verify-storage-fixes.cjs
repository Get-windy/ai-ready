/**
 * 仓储模块修复验收（2026-09-23 那批 D1–D8 的接口级验证）
 *
 * 断言：
 *   A1 发货单取消端点已存在      POST /api/wms/ship/cancel   → 非 404（业务错误即可）
 *   A2 事件 outbox 派发已删除    POST /api/wms/event/outbox/process → 404
 *   A3 收货接口正常              GET  /api/wms/receipt/page   → 200
 *   A4 非超管可用（权限授权）    GET  /api/wms/receipt/page   → 200（系统管理员账号）
 *   A5 盘点死链路已删除          GET  /api/erp/stock/check/page → 404
 *
 * 用法: NODE_PATH=<pc-admin>/node_modules node tools/verify-storage-fixes.cjs
 *       EXTRA_USER=<系统管理员用户名> 可选，用于 A4
 */
const http = require('http')

const BASE = { host: '127.0.0.1', port: Number(process.env.API_PORT || 5655) }
const USER = process.env.E2E_USER || 'admin'
const PWD = process.env.E2E_PWD || 'admin123'
const TENANT = process.env.E2E_TENANT || '系统租户'

function rawReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const req = http.request({
      ...BASE, method, path,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      let buf = ''
      res.on('data', c => buf += c)
      res.on('end', () => {
        let json = null
        try { json = JSON.parse(buf) } catch { /* 非 JSON 忽略 */ }
        resolve({ status: res.statusCode, json, raw: buf.slice(0, 200) })
      })
    })
    req.on('error', reject)
    if (data) req.write(data)
    req.end()
  })
}

async function loginAs(username) {
  const cap = await rawReq('GET', '/api/auth/captcha')
  if (!cap.json?.data?.img) throw new Error('取验证码失败: ' + cap.raw)
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/api/auth/login', {
    username, password: PWD, tenantName: TENANT, captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`登录失败(${username}): ` + JSON.stringify(res.json).slice(0, 300))
  return token
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '  PASS' : '  FAIL'}  ${name}    ${detail}`)
}

async function main() {
  console.log(`\n== 仓储修复验收 @ ${BASE.host}:${BASE.port} ==\n`)
  console.log('[1] 以超管登录 ' + USER)
  const token = await loginAs(USER)
  console.log('    token ok\n')

  console.log('[2] 接口断言')
  const r1 = await rawReq('POST', '/api/wms/ship/cancel?taskId=99999999&reason=%E9%AA%8C%E6%94%B6', null, token)
  check('A1 发货取消端点存在（非 404）', r1.status !== 404, `HTTP ${r1.status}`)

  const r2 = await rawReq('POST', '/api/wms/event/outbox/process', {}, token)
  check('A2 事件派发端点已删除（404）', r2.status === 404, `HTTP ${r2.status}`)

  const r3 = await rawReq('GET', '/api/wms/receipt/page?current=1&size=1', null, token)
  check('A3 收货分页可用（200）', r3.status === 200, `HTTP ${r3.status}`)

  // 注意口径：不能断言 404 —— StockCheckController 删除后，两段路径
  // /api/erp/stock/check/page 会被 StockController 的 @GetMapping("/{productId}/{warehouseId}")
  // 捕获，"check" 转 Long 失败 ⇒ 返回 400 而非 404。判据是「不再返回盘点列表」。
  const r5 = await rawReq('GET', '/api/erp/stock/check/page?current=1&size=1', null, token)
  check('A5 盘点死链路已移除（不返回 200 数据）', r5.status !== 200, `HTTP ${r5.status}（被 /api/erp/stock/{a}/{b} 泛路由捕获，属预期）`)

  const extra = process.env.EXTRA_USER
  if (extra) {
    console.log('\n[3] 非超管可用性（权限授权）')
    const t2 = await loginAs(extra)
    const r4 = await rawReq('GET', '/api/wms/receipt/page?current=1&size=1', null, t2)
    check(`A4 非超管(${extra})可访问收货接口（200）`, r4.status === 200, `HTTP ${r4.status}`)
    const r4b = await rawReq('GET', '/api/erp/stock/in/page?current=1&size=1', null, t2)
    check(`A4b 非超管可访问其他入库单（200）`, r4b.status === 200, `HTTP ${r4b.status}`)
  } else {
    console.log('\n[3] 跳过 A4（未提供 EXTRA_USER）')
  }

  const failed = results.filter(r => !r.ok)
  console.log(`\n== 合计 ${results.length} 项，通过 ${results.length - failed.length}，失败 ${failed.length} ==\n`)
  process.exit(failed.length ? 1 : 0)
}

main().catch(e => { console.error('验收脚本异常:', e.message); process.exit(2) })
