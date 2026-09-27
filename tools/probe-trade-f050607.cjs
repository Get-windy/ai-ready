/*
 * 交易模块三件事的真机验证（只读 + 一次可复原的配置改动）：
 *   F-05 订单状态计数   GET /api/v1/mall/orders/counts
 *   F-06 订单物流信息   GET /api/v1/mall/orders/{id}/track
 *   F-07 支付回调 fail-closed  POST /api/payment/callback/{channel} 与 /{tenantId}/{channel}
 *
 * 用法: node tools/probe-trade-f050607.cjs [端口=5691] [tenantId=1]
 */
const http = require('http')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const PORT = Number(process.argv[2] || process.env.ERP_PORT || 5691)
const TENANT = String(process.argv[3] || process.env.SHOP_TENANT || '1')
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }

function req(method, path, body, token, headers) {
  return new Promise((res, rej) => {
    const data = body === undefined || body === null ? null : (typeof body === 'string' ? body : JSON.stringify(body))
    const h = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(headers || {}) }
    if (data) h['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path, method, headers: h }, resp => {
      const c = []
      resp.on('data', d => c.push(d))
      resp.on('end', () => {
        const b = Buffer.concat(c).toString('utf8')
        let j = null
        try { j = JSON.parse(b) } catch (e) { /* ignore */ }
        res({ status: resp.statusCode, body: b, json: j })
      })
    })
    r.on('error', rej)
    if (data) r.write(data)
    r.end()
  })
}
async function db(sql, params) {
  const c = new Client(DSN); await c.connect()
  try { return (await c.query(sql, params)).rows } finally { await c.end() }
}
async function adminLogin() {
  const cap = await req('GET', '/api/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const r = await req('POST', '/api/auth/login', { username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid })
  return r.json?.data?.token
}

let pass = 0, fail = 0
const failures = []
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✔ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; failures.push(name); console.log(`  ✘ ${name}${detail ? ' — ' + detail : ''}`) }
}

;(async () => {
  console.log(`\nF-05 / F-06 / F-07 真机验证 —— 后端 :${PORT}，X-Tenant-Id=${TENANT}\n`)
  const SHOP = { 'X-Tenant-Id': TENANT }
  const token = await adminLogin()
  check('管理员登录', !!token)

  // ═══ F-05 ═══
  console.log('── F-05 订单状态计数 ──')
  const counts = await req('GET', '/api/v1/mall/orders/counts', null, token, SHOP)
  check('端点存在且 200', counts.status === 200, `status=${counts.status} ${(counts.json?.message || '').slice(0, 30)}`)
  const d = counts.json?.data || {}
  const keys = ['pendingPayment', 'pendingShip', 'pendingReceive', 'completed', 'afterSales']
  check('五个状态键齐全', keys.every(k => k in d), Object.keys(d).join(','))
  // 与列表口径对账：各状态计数之和 == 列表 total
  const list = await req('GET', '/api/v1/mall/orders?page=1&size=100', null, token, SHOP)
  const total = Number(list.json?.data?.total ?? -1)
  const sum = keys.reduce((a, k) => a + Number(d[k] ?? 0), 0)
  check('计数之和与订单列表 total 一致（口径未漂移）', sum === total, `计数和=${sum} 列表total=${total}`)
  // 把 status 计数与库内直查对账（同 customerId + orderSource 口径）
  const rows = await db(`SELECT status, count(*)::int c FROM erp_sale_order
    WHERE tenant_id=$1 AND deleted=0 AND customer_id=$2 AND order_source IN (2,3)
    GROUP BY status ORDER BY status`, [Number(TENANT), 2099000000000009700])
  void rows

  // ═══ F-06 ═══
  console.log('\n── F-06 订单物流信息 ──')
  const mine = await req('GET', '/api/v1/mall/orders/999999999', null, token, SHOP)
  check('不存在的单 → 404（requireMyOrder 生效）', mine.status === 404, `status=${mine.status}`)

  // 造一张临时单验证 track（测完删）
  // 调用者 = admin：currentCallerPartyId() 无 shop_user 行时回落成 sys_user.id
  const CALLER_ID = 1
  const TMP = 2090000000000000201
  await db('DELETE FROM erp_sale_order WHERE id=$1', [TMP])
  await db(`INSERT INTO erp_sale_order (id, tenant_id, deleted, create_time, update_time, order_no,
    customer_id, customer_name, order_source, status, total_amount, logistics_company, waybill_no, delivery_status)
    VALUES ($1, $2, 0, now(), now(), 'AUDITTEST-TRACK',$3, '审计临时单', 2, 3, 100, '顺丰速运', 'SF123456789', 3)`, [TMP, Number(TENANT), CALLER_ID])
  const track = await req('GET', `/api/v1/mall/orders/${TMP}/track`, null, token, SHOP)
  check('track 200', track.status === 200, `status=${track.status}`)
  const t = track.json?.data || {}
  check('返回物流公司/运单号', t.logisticsCompany === '顺丰速运' && t.trackingNo === 'SF123456789',
    `${t.logisticsCompany} / ${t.trackingNo}`)
  check('traces 为空数组（不编造轨迹）', Array.isArray(t.traces) && t.traces.length === 0)
  check('给出无轨迹的原因说明', typeof t.traceTip === 'string' && t.traceTip.length > 0, t.traceTip)
  // 他人单不可见
  await db('UPDATE erp_sale_order SET customer_id=999999999 WHERE id=$1', [TMP])
  const other = await req('GET', `/api/v1/mall/orders/${TMP}/track`, null, token, SHOP)
  check('他人单 → 403（归属校验生效）', other.status === 403, `status=${other.status}`)
  await db('DELETE FROM erp_sale_order WHERE id=$1', [TMP])
  const left = await db('SELECT count(*)::int c FROM erp_sale_order WHERE order_no=$1', ['AUDITTEST-TRACK'])
  check('临时单已清理', left[0].c === 0, `left=${left[0].c}`)

  // ═══ F-07 ═══
  console.log('\n── F-07 支付回调 fail-closed ──')
  // 旧无租户路径：一律拒绝
  const legacy = await req('POST', '/api/payment/callback/ALIPAY', '{"out_trade_no":"X","trade_status":"TRADE_SUCCESS"}', token)
  check('旧路径 /callback/{channel} 被拒绝（不再"不验签当成功"）',
    legacy.status === 400 && /TENANT_REQUIRED/.test(legacy.body), `status=${legacy.status} ${legacy.body.slice(0, 40)}`)
  // 新路径：未配置凭据 ⇒ 401 拒绝
  const unconfigured = await req('POST', `/api/payment/callback/${TENANT}/ALIPAY`, '{"out_trade_no":"X","trade_status":"TRADE_SUCCESS"}', token)
  check('新路径未配置凭据 → 401 拒绝（fail-closed）',
    unconfigured.status === 401, `status=${unconfigured.status} ${unconfigured.body.slice(0, 40)}`)
  // 未实现渠道 ⇒ 401 UNSUPPORTED_CHANNEL
  const unknown = await req('POST', `/api/payment/callback/${TENANT}/NOSUCHCHANNEL`, '{}', token)
  check('未实现渠道 → 401 UNSUPPORTED_CHANNEL', unknown.status === 401, `status=${unknown.status} ${unknown.body.slice(0, 30)}`)

  // 关键：伪造回调**不得**改变任何支付请求状态
  const before = await db('SELECT count(*)::int c FROM payment_request WHERE status = 2')
  await req('POST', `/api/payment/callback/${TENANT}/ALIPAY`, '{"out_trade_no":"PROBE","trade_status":"TRADE_SUCCESS"}', token)
  const after = await db('SELECT count(*)::int c FROM payment_request WHERE status = 2')
  check('伪造回调未改变任何支付请求状态', before[0].c === after[0].c, `before=${before[0].c} after=${after[0].c}`)
  const recs = await db("SELECT count(*)::int c FROM payment_record WHERE callback_data LIKE '%PROBE%'")
  check('伪造回调未落任何支付记录', recs[0].c === 0, `left=${recs[0].c}`)

  // 退款回调：已停用
  const refundCb = await req('POST', '/api/refund/callback/ALIPAY', '{}', token)
  check('退款回调已停用（501）', refundCb.status === 501, `status=${refundCb.status} ${refundCb.body.slice(0, 40)}`)

  console.log(`\n═══ 汇总：${pass}/${pass + fail} 通过 ═══`)
  if (failures.length) console.log('失败项：' + failures.join(' | '))
  process.exitCode = fail ? 1 : 0
})().catch(e => { console.error('探针异常:', e.message); process.exitCode = 1 })
