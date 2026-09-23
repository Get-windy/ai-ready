/*
 * 交易模块审计探针（只读优先；写入探针一律回滚/清理）
 * 用法: node tools/probe-trade-audit.cjs [端口，默认 5655]
 */
const http = require('http')
const PORT = Number(process.argv[2] || process.env.ERP_PORT || 5655)

function req(method, path, body, token) {
  return new Promise((res, rej) => {
    const data = body === undefined || body === null ? null
      : (typeof body === 'string' ? body : JSON.stringify(body))
    const h = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) h['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path, method, headers: h }, resp => {
      const c = []; resp.on('data', d => c.push(d)); resp.on('end', () => {
        const b = Buffer.concat(c).toString('utf8')
        let j = null; try { j = JSON.parse(b) } catch (e) {}
        res({ status: resp.statusCode, body: b, json: j })
      })
    })
    r.on('error', rej); if (data) r.write(data); r.end()
  })
}

async function login(user = 'admin', pwd = 'admin123', tenant = '系统租户') {
  const cap = await req('GET', '/api/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/api/auth/login',
    { username: user, password: pwd, tenantName: tenant, captcha: code, captchaKey: cap.json.data.uuid })
  const tk = res.json?.data?.token || res.json?.data?.accessToken
  if (!tk) throw new Error('登录失败: ' + res.body.slice(0, 300))
  return tk
}

function brief(r, n = 200) { return `${r.status} ${r.body.replace(/\s+/g, ' ').slice(0, n)}` }

;(async () => {
  const token = await login()
  console.log('✔ 登录成功 admin/系统租户')

  console.log('\n── A. 开放 API（/api/open/**，宣称「供外部平台调用」）──')
  const anon = await req('GET', '/api/open/health')
  console.log('  匿名 GET /api/open/health        ->', brief(anon))
  const auth = await req('GET', '/api/open/health', null, token)
  console.log('  带登录态 GET /api/open/health    ->', brief(auth))

  console.log('\n── B. 支付回调（无鉴权注解，宣称需验签）──')
  const cb = await req('POST', '/api/payment/callback/ALIPAY', '{"out_trade_no":"PROBE-NOT-EXIST","trade_status":"TRADE_SUCCESS","total_amount":"0.01"}', token)
  console.log('  已登录 POST /api/payment/callback/ALIPAY ->', brief(cb))
  const cb2 = await req('POST', '/api/payment/callback/ALIPAY', '{"x":1}')
  console.log('  匿名   POST /api/payment/callback/ALIPAY ->', brief(cb2))

  console.log('\n── C. 商城管理端（应受权限码保护）──')
  for (const p of ['/api/erp/mall/admin/order/page?pageNum=1&pageSize=1',
    '/api/erp/mall/admin/user/page?pageNum=1&pageSize=1',
    '/api/erp/mall/admin/config',
    '/api/erp/mall/notice/list?limit=3',
    '/api/trade/channel/page?pageNum=1&pageSize=1',
    '/api/trade/external-order/page?pageNum=1&pageSize=1',
    '/api/trade/inventory-sync/page?pageNum=1&pageSize=1']) {
    const r = await req('GET', p, null, token)
    console.log(`  GET ${p}\n      -> ${brief(r, 160)}`)
  }

  console.log('\n── D. C 端商城（/api/v1/mall/**，声称游客可浏览商品）──')
  for (const [m, p, b] of [
    ['GET', '/api/v1/mall/products?page=1&size=1', null],
    ['GET', '/api/v1/mall/products/banners', null],
    ['GET', '/api/v1/mall/orders?page=1&size=1', null],
    ['GET', '/api/v1/mall/user/info', null],
    ['GET', '/api/v1/mall/cart', null],
    ['GET', '/api/v1/mall/orders/999999/track', null],
  ]) {
    const r = await req(m, p, b)
    console.log(`  匿名 ${m} ${p}\n      -> ${brief(r, 140)}`)
  }

  console.log('\n── E. 商城订单管理动作（越权/归属）──')
  const o = await req('GET', '/api/erp/mall/admin/order/page?pageNum=1&pageSize=5', null, token)
  console.log('  订单分页 ->', brief(o, 400))
})().catch(e => { console.error('ERR', e.message); process.exit(1) })
