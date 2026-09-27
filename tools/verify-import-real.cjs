/**
 * 真机验证：导入链路是否真的落库（2026-09-26 系统模块审计修复的验证脚本）
 *
 * 验证三件事：
 *   ① 新链路 POST /api/erp/md/customer/import-excel?partnerType=customer → 真的写入 erp_partner
 *   ② 旧链路 POST /api/import/v2/excel/customer → **明确失败**，不再回执「成功 N 条」
 *   ③ 验证结束后清理本次写入的测试数据（按名称精确匹配）
 *
 * 运行：node tools/verify-import-real.cjs   （后端 5655 需在跑；测试文件 tools/tmp-import-test.xlsx）
 */
const http = require('http')
const fs = require('fs')
const path = require('path')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const BE = Number(process.env.BE_PORT || 5655)
const TEST_NAME = '审计验证客户请删除'
const XLSX = path.join(__dirname, 'tmp-import-test.xlsx')

function req(method, p, { token, json, form } = {}) {
  return new Promise((resolve, reject) => {
    let body = null, headers = {}
    if (json) { body = Buffer.from(JSON.stringify(json)); headers['Content-Type'] = 'application/json' }
    if (form) { body = form.body; headers = { ...headers, ...form.headers } }
    const r = http.request({
      hostname: 'localhost', port: BE, path: '/api' + p, method,
      headers: { ...headers, ...(body ? { 'Content-Length': body.length } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    }, res => {
      const c = []
      res.on('data', d => c.push(d))
      res.on('end', () => {
        const s = Buffer.concat(c).toString('utf8')
        try { resolve({ status: res.statusCode, body: JSON.parse(s) }) } catch { resolve({ status: res.statusCode, body: s }) }
      })
    })
    r.on('error', reject)
    if (body) r.write(body)
    r.end()
  })
}

/** 手写 multipart/form-data（避免引入 form-data 依赖） */
function multipart(filePath, fieldName = 'file') {
  const boundary = '----AtCodeBoundary' + Date.now()
  const head = Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="${fieldName}"; filename="${path.basename(filePath)}"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n`)
  const tail = Buffer.from(`\r\n--${boundary}--\r\n`)
  return { body: Buffer.concat([head, fs.readFileSync(filePath), tail]), headers: { 'Content-Type': `multipart/form-data; boundary=${boundary}` } }
}

async function login(username, tenantName) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.body.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    json: { username, password: 'admin123', tenantName, captcha: code, captchaKey: cap.body.data.uuid },
  })
  const d = res.body && res.body.data
  if (!d || !(d.token || d.accessToken)) throw new Error(`${username} 登录失败: ` + JSON.stringify(res.body).slice(0, 200))
  return d.token || d.accessToken
}

;(async () => {
  const db = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
  await db.connect()
  const count = async () => Number((await db.query('SELECT count(*) c FROM biz_party WHERE party_name = $1 AND deleted = 0', [TEST_NAME])).rows[0].c)

  const before = await count()
  console.log(`前置：库中已存在同名往来单位 ${before} 条`)
  if (before > 0) {
    await db.query('DELETE FROM biz_party WHERE party_name = $1', [TEST_NAME])
    console.log('  （已清理历史残留）')
  }

  const token = await login('admin', '系统租户')
  console.log('✓ admin 登录成功\n')

  // ① 新链路：真实导入端点
  console.log('=== ① 真实端点 POST /erp/md/customer/import-excel?partnerType=customer ===')
  const r1 = await req('POST', '/erp/md/customer/import-excel?partnerType=customer', { token, form: multipart(XLSX) })
  console.log('  HTTP', r1.status, JSON.stringify(r1.body && r1.body.data || r1.body).slice(0, 300))
  const after = await count()
  console.log(`  库中同名记录: ${after} 条 ${after > before ? '✅ 真的落库了' : '❌ 未落库'}`)
  if (after > 0) {
    const row = (await db.query('SELECT party_code, party_type, phone, remark FROM biz_party WHERE party_name = $1 AND deleted = 0', [TEST_NAME])).rows[0]
    console.log(`  落库内容: 编码=${row.party_code} 类型=${row.party_type} 电话=${row.phone} 备注=${row.remark}`)
  }

  // ② 旧链路：应明确失败
  console.log('\n=== ② 旧端点 POST /import/v2/excel/customer（修复后应明确失败） ===')
  const XLSX2 = path.join(__dirname, 'tmp-import-test2.xlsx')
  const r2 = await req('POST', '/import/v2/excel/customer', { token, form: multipart(XLSX2) })
  const d2 = r2.body && (r2.body.data || r2.body)
  console.log('  HTTP', r2.status)
  console.log('  响应:', JSON.stringify(d2).slice(0, 400))
  const oldOk = d2 && (d2.success === true)
  console.log(`  判定: ${oldOk ? '❌ 仍回执成功（修复未生效）' : '✅ 未回执成功（符合预期）'}`)

  // ③ 清理
  console.log('\n=== ③ 清理测试数据 ===')
  const del = await db.query('DELETE FROM biz_party WHERE party_name = $1', [TEST_NAME])
  console.log(`  已删除 ${del.rowCount} 条；剩余 ${await count()} 条`)
  await db.end()
})().catch(e => { console.error('✗ 失败:', e && e.message || e); process.exit(1) })
