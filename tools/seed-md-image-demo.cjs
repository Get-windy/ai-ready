/* 图片管理演示数据：向图片空间上传若干素材，便于 UI 验收截图 */
const http = require('http')
const fs = require('fs')

const PORT = Number(process.env.BE_PORT || 5710)

function apiReq(method, path, body, token, raw) {
  return new Promise((resolve, reject) => {
    const isBuf = Buffer.isBuffer(body)
    const data = body == null ? null : (isBuf ? body : JSON.stringify(body))
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + path, method,
      headers: {
        ...(isBuf ? { 'Content-Type': raw.contentType } : { 'Content-Type': 'application/json' }),
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      let s = ''
      res.on('data', c => { s += c })
      res.on('end', () => { try { resolve(JSON.parse(s)) } catch { resolve({ raw: s }) } })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function multipart(fields, fileName, fileBuffer, contentType) {
  const boundary = '----DemoBoundary' + Date.now()
  const parts = []
  for (const [k, v] of Object.entries(fields)) {
    parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="${k}"\r\n\r\n${v}\r\n`))
  }
  parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\nContent-Type: ${contentType}\r\n\r\n`))
  parts.push(fileBuffer)
  parts.push(Buffer.from(`\r\n--${boundary}--\r\n`))
  return { body: Buffer.concat(parts), contentType: `multipart/form-data; boundary=${boundary}` }
}

async function login() {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  return res?.data?.token
}

;(async () => {
  const token = await login()
  const files = [
    ['I:/AI-Ready/tool-results/md-image/ui-01-tab1.png', '奶茶粉主图-1.png'],
    ['I:/AI-Ready/tool-results/md-image/ui-02-space.png', '果酱瓶装-1.png'],
    ['I:/AI-Ready/tool-results/md-image/ui-03-colconfig.png', 'SP-TEST-002-1.png'],
  ]
  for (const [path, name] of files) {
    if (!fs.existsSync(path)) {
      console.log('skip missing', path)
      continue
    }
    const buf = fs.readFileSync(path)
    const mp = multipart({}, name, buf, 'image/png')
    const res = await apiReq('POST', '/erp/md/image/upload', mp.body, token, { contentType: mp.contentType })
    console.log('upload', name, '=>', res?.data?.id ? 'OK id=' + res.data.id : JSON.stringify(res).slice(0, 160))
  }
  const list = await apiReq('GET', '/erp/md/image/space-page?pageNum=1&pageSize=10', null, token)
  console.log('图片空间总数 =', list?.data?.total)

  // 演示：把首张素材绑定到首个商品并设为主图（Tab1 图片列可见）
  const first = list?.data?.records?.[0]
  const page = await apiReq('GET', '/erp/md/image/page?pageNum=1&pageSize=1', null, token)
  const product = page?.data?.records?.[0]
  if (first && product) {
    const bind = await apiReq('POST', '/erp/md/image/bind',
      { imageId: first.id, productId: product.productId, isMain: 1 }, token)
    console.log('绑定首图到商品:', product.productName, '=>', bind?.data)
  }
  // 演示：按货号自动匹配（SP-TEST-002-1.png → SP-TEST-002）
  const auto = await apiReq('POST', '/erp/md/image/auto-match?matchType=CODE', null, token)
  console.log('自动匹配结果:', JSON.stringify(auto?.data))
})()
