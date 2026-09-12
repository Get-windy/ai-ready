/* 商品辅助资料 端点探测：确认新后端是否已生效 */
const http = require('http')
const PORT = Number(process.env.AUX_PORT || 5655)
let TOKEN = null

function rawReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        try { resolve({ status: res.statusCode, ...JSON.parse(buf.toString('utf8')) }) }
        catch { resolve({ status: res.statusCode, raw: buf.toString('utf8').slice(0, 300) }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  TOKEN = token
}

(async () => {
  await login()
  const probes = [
    ['GET', '/erp/product-unit-group/page?pageNum=1&pageSize=5'],
    ['GET', '/erp/mall-tag/page?pageNum=1&pageSize=5'],
    ['GET', '/erp/product-brand/page?pageNum=1&pageSize=2'],
    ['GET', '/erp/product-unit-dict/page?pageNum=1&pageSize=2'],
    ['GET', '/erp/product-brand/export'],
  ]
  for (const [m, p] of probes) {
    const r = await rawReq(m, p, null, TOKEN)
    const brief = r.code !== undefined
      ? `code=${r.code} msg=${r.message || ''} total=${r.data?.total ?? '-'}`
      : `status=${r.status} raw=${String(r.raw || '').slice(0, 120)}`
    console.log(`${m} ${p}\n   -> ${brief}`)
  }
})().catch(e => { console.error('FATAL', e.message); process.exit(1) })
