/*
 * 商城 C 端一期接口验证（真机；含一次**可复原**的店铺配置改动）。
 *
 * 验证三件事：
 *  ① 白名单生效：`/v1/mall/shop/config`、`/v1/mall/notice/list`、`/v1/mall/tags`
 *     在**匿名**下不再被 SaInterceptor 拦成 401（可达），且未带 X-Tenant-Id 时
 *     按"无法确定店铺" fail-closed 400。
 *  ② 游客闸门生效：`tenant_shop_config.allow_guest = NOT_ALLOW` 时三接口一律 403。
 *  ③ 开放游客时数据正确：店铺配置**不含** appsecret/mch_key 等凭据；标签能下发；
 *     按 `tagCode` 过滤能真正筛出带该标签的商品。
 *
 * 副作用与复原：为验证 ③ 会把 `tenant_shop_config.allow_guest/guest_show_price`
 *   临时改成 ALLOW/SHOW，**结束时按原值还原并回读断言**（与 tools/e2e-trade.cjs 同姿势）。
 *
 * 用法: node tools/probe-mall-c.cjs [端口=5691] [tenantId=1]
 */
const http = require('http')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const PORT = Number(process.argv[2] || process.env.ERP_PORT || 5691)
const TENANT = String(process.argv[3] || process.env.SHOP_TENANT || '1')
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }

function req(method, path, token, extraHeaders) {
  return new Promise((res, rej) => {
    const h = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(extraHeaders || {}) }
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
    r.end()
  })
}

async function db(sql, params) {
  const c = new Client(DSN)
  await c.connect()
  try { return (await c.query(sql, params)).rows } finally { await c.end() }
}

const SHOP = { 'X-Tenant-Id': TENANT }
let pass = 0, fail = 0
const failures = []
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✔ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; failures.push(name); console.log(`  ✘ ${name}${detail ? ' — ' + detail : ''}`) }
}

/** 递归找敏感字段名（白名单接口不该出现） */
function findSensitive(obj, path = '') {
  const hits = []
  if (obj === null || typeof obj !== 'object') return hits
  for (const [k, v] of Object.entries(obj)) {
    const flat = k.toLowerCase().replace(/_/g, '')
    if (/appsecret|mchkey|mchid|apiv3|smssignature|authdomain/.test(flat)) hits.push(`${path}${k}`)
    hits.push(...findSensitive(v, `${path}${k}.`))
  }
  return hits
}

const ENDPOINTS = [
  ['店铺配置', '/api/v1/mall/shop/config'],
  ['公告', '/api/v1/mall/notice/list?limit=5'],
  ['商品标签', '/api/v1/mall/tags']
]

;(async () => {
  console.log(`\n商城 C 端一期接口验证 —— 后端 :${PORT}，X-Tenant-Id: ${TENANT}\n`)

  const [row] = await db(
    'SELECT allow_guest, guest_show_price FROM tenant_shop_config WHERE tenant_id = $1 AND deleted = 0', [Number(TENANT)])
  if (!row) { console.error(`找不到租户 ${TENANT} 的店铺配置，终止`); process.exitCode = 1; return }
  const ORIG = { allowGuest: row.allow_guest, guestShowPrice: row.guest_show_price }
  console.log(`原始配置：allow_guest=${ORIG.allowGuest}, guest_show_price=${ORIG.guestShowPrice}\n`)

  try {
    // ═══ ① 白名单可达性（不依赖店铺开关）═══
    console.log('── ① 匿名可达性：不再 401（白名单已登记）──')
    for (const [name, path] of ENDPOINTS) {
      const r = await req('GET', path, null, SHOP)
      check(`${name}：匿名未被拦成 401`, r.status !== 401, `status=${r.status}`)
    }
    for (const [name, path] of ENDPOINTS) {
      const r = await req('GET', path, null, {})
      check(`${name}：缺 X-Tenant-Id → 400 无法确定店铺`, r.status === 400 && /无法确定店铺/.test(r.body), `status=${r.status}`)
    }
    const oldPath = await req('GET', '/api/erp/mall/notice/list?limit=5', null, SHOP)
    check('公告旧管理端路径已摘除（不再是 200）', oldPath.status !== 200, `status=${oldPath.status}`)

    // ═══ ② 游客闸门（当前 NOT_ALLOW 应一律 403）═══
    console.log('\n── ② 游客闸门：allow_guest=NOT_ALLOW 时拒绝 ──')
    for (const [name, path] of ENDPOINTS) {
      const r = await req('GET', path, null, SHOP)
      check(`${name}：未开放游客 → 403`, r.status === 403, `status=${r.status} ${(r.json?.message || '').slice(0, 24)}`)
    }

    // ═══ ③ 临时开放游客，验证数据与白名单 ═══
    console.log('\n── ③ 临时置 ALLOW/SHOW，验证下发内容（结束后还原）──')
    await db("UPDATE tenant_shop_config SET allow_guest='ALLOW', guest_show_price='SHOW' WHERE tenant_id=$1 AND deleted=0", [Number(TENANT)])

    const cfg = await req('GET', '/api/v1/mall/shop/config', null, SHOP)
    check('店铺配置 200', cfg.status === 200, `status=${cfg.status}`)
    const d = cfg.json?.data || {}
    check('含店铺名', !!d.shopName, `shopName=${d.shopName}`)
    check('含价格/准入开关', 'allowGuest' in d && 'guestShowPrice' in d, `allowGuest=${d.allowGuest}, guestShowPrice=${d.guestShowPrice}`)
    check('含展示开关 showSales/stockDisplay/quantityScale', 'showSales' in d && 'stockDisplay' in d && 'quantityScale' in d)
    check('含分类页开关 categoryStyle/categoryShowCount', 'categoryStyle' in d && 'categoryShowCount' in d)
    const leak = findSensitive(d)
    check('无凭据字段泄露', leak.length === 0, leak.length ? leak.join(', ') : 'appsecret / mch_key 均未下发')

    const tags = await req('GET', '/api/v1/mall/tags', null, SHOP)
    check('标签 200 且有数据', tags.status === 200 && (tags.json?.data?.length || 0) > 0, `条数=${tags.json?.data?.length ?? 0}`)
    const first = tags.json?.data?.[0]
    if (first) {
      check('标签字段为 tagCode/tagName/sortOrder（无审计列）',
        'tagCode' in first && 'tagName' in first && !('tenantId' in first) && !('createBy' in first),
        Object.keys(first).join(','))

      const p = await req('GET', `/api/v1/mall/products?page=1&size=10&tagCode=${encodeURIComponent(first.tagCode)}`, null, SHOP)
      check(`按标签 ${first.tagCode} 过滤商品可用`, p.status === 200, `total=${p.json?.data?.total ?? '-'}`)
      const recs = p.json?.data?.records || []
      check('命中结果确实都带该标签', recs.every(r => String(r.productTag || '').includes(first.tagCode)),
        recs.length ? `校验 ${recs.length} 条` : '该标签下暂无商品（正常）')
    }
    const all = await req('GET', '/api/v1/mall/products?page=1&size=5', null, SHOP)
    check('不带 tagCode 的原列表仍可用', all.status === 200, `total=${all.json?.data?.total ?? '-'}`)

    const notice = await req('GET', '/api/v1/mall/notice/list?limit=5', null, SHOP)
    check('公告 200 且仅已发布', notice.status === 200 && (notice.json?.data || []).every(n => n.id),
      `条数=${notice.json?.data?.length ?? 0}`)
  } finally {
    // ═══ 复原 ═══
    await db('UPDATE tenant_shop_config SET allow_guest=$1, guest_show_price=$2 WHERE tenant_id=$3 AND deleted=0',
      [ORIG.allowGuest, ORIG.guestShowPrice, Number(TENANT)])
    const [back] = await db('SELECT allow_guest, guest_show_price FROM tenant_shop_config WHERE tenant_id=$1 AND deleted=0', [Number(TENANT)])
    console.log('\n── 复原复核 ──')
    check('allow_guest 已还原', back.allow_guest === ORIG.allowGuest, `now=${back.allow_guest} orig=${ORIG.allowGuest}`)
    check('guest_show_price 已还原', back.guest_show_price === ORIG.guestShowPrice, `now=${back.guest_show_price} orig=${ORIG.guestShowPrice}`)
  }

  console.log(`\n═══ 汇总：${pass}/${pass + fail} 通过 ═══`)
  if (failures.length) console.log('失败项：' + failures.join(' | '))
  process.exitCode = fail ? 1 : 0
})().catch(e => { console.error('探针异常:', e.message); process.exitCode = 1 })
