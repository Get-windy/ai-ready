/**
 * 平台级：写入路径「租户自动盖章」实测（会造少量探针数据并在结束时清理）
 *
 * 背景：`MetaObjectHandler.insertFill` 依赖 `MyBatisPlusConfig.getCurrentTenantIdValue()`
 * （优先级：ThreadLocal 临时租户 → Sa-Token Session `tenantId`）。一旦它返回 null，
 * insertFill 不盖章、租户拦截器也不注入 → INSERT 语句里连 tenant_id 列都没有 → 落到列默认值 0
 * → **新数据对自己租户不可见**。
 *
 * 本脚本对**同一条 POST 路径**做对照实验：带 / 不带 `X-Tenant-Id` 头、不同实体（资料域 vs 营销域），
 * 用来把「会话租户解析」与「实体分别盖章与否」两件事分开。
 *
 * 用法：node tools/audit-tenant-context.cjs
 */
const http = require('http')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const API = process.env.E2E_API || 'http://localhost:5655/api'
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }

function req(method, path, body, token, headers) {
  return new Promise((resolve, reject) => {
    const url = new URL(API + path)
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: url.hostname, port: url.port, path: url.pathname + url.search, method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(headers || {}),
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
      },
    }, res => {
      let buf = ''
      res.on('data', c => { buf += c })
      res.on('end', () => {
        let json = null
        try { json = JSON.parse(buf) } catch { /* 非 JSON */ }
        resolve({ status: res.statusCode, json, raw: buf })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login(user, pwd, tenant) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', { username: user, password: pwd, tenantName: tenant, captcha: code, captchaKey: cap.json.data.uuid })
  return res.json?.data?.token || res.json?.data?.accessToken
}

async function db(sql, params) {
  const c = new Client(DSN)
  await c.connect()
  try { return (await c.query(sql, params)).rows } finally { await c.end() }
}

const CASES = [
  { label: '客户（biz_party）· 无头', path: '/erp/md/customer', table: 'biz_party', body: n => ({ partnerType: 'customer', partnerCode: n, partnerName: '探针' + n }) },
  { label: '客户（biz_party）· 带 X-Tenant-Id=1', path: '/erp/md/customer', table: 'biz_party', headers: { 'X-Tenant-Id': '1' }, body: n => ({ partnerType: 'customer', partnerCode: n, partnerName: '探针' + n }) },
  { label: '供应商（biz_party）· 无头', path: '/erp/md/supplier', table: 'biz_party', body: n => ({ partnerType: 'supplier', partnerCode: n, partnerName: '探针' + n }) },
  { label: '优惠券模板（营销域，控制器显式 setTenantId）', path: '/erp/marketing/coupon-template', table: 'mkt_coupon_template', body: n => ({ couponName: '探针' + n, couponType: 'FIXED', faceValue: 1, totalCount: 1 }) },
]

;(async () => {
  const token = await login('admin', 'admin123', '系统租户')
  if (!token) { console.log('登录失败'); return }
  const info = await req('GET', '/auth/userinfo', null, token)
  const sessionTenant = info.json?.data?.tenantId
  console.log(`账号=admin 会话租户（/auth/userinfo 读 session.tenantId）=${sessionTenant}\n`)

  for (const c of CASES) {
    const n = 'TP' + Date.now().toString().slice(-7)
    const res = await req('POST', c.path, c.body(n), c.uploadToken || token, c.headers)
    const id = res.json?.data?.id
    let got = '(无 id)'
    if (id) {
      const row = (await db(`SELECT tenant_id FROM ${c.table} WHERE id = $1`, [id]))[0]
      got = row ? String(row.tenant_id) : '(查不到)'
      await db(`UPDATE ${c.table} SET deleted = 1 WHERE id = $1`, [id]).catch(() => {})
    }
    console.log(`${String(got) === String(sessionTenant) ? '✅' : '❌'} ${c.label.padEnd(40)} HTTP=${res.status} → ${c.table}.tenant_id=${got}`)
  }
  console.log('\n（探针行已逻辑删除）')
})().catch(e => { console.error(e); process.exit(1) })
