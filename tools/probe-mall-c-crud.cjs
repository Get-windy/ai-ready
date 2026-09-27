/*
 * 商城 C 端「写路径」真机往返验证（2026-09-26）。
 *
 * 为什么单独写：`verify-mall-c-ui*.cjs` 只证明**页面能渲染、无报错**，
 * 而地址簿 / 购物车这两处此前的 500 出在**读写语句**上（映射了不存在的列），
 * 只有真的走一遍 增 → 查 → 改 → 删 才知道修没修好。
 *
 * 做法：用演示买家（tools/mall-demo-seed.sql 里的 demo_buyer）走 C 端接口，
 *   全程**自建自清**（最后删掉自己造的数据并断言归零），不改他人数据。
 *
 * 用法: node tools/probe-mall-c-crud.cjs [后端端口=5691]
 */
const http = require('http')
const PORT = Number(process.argv[2] || process.env.ERP_PORT || 5691)
const TENANT = process.env.SHOP_TENANT || '1'

function req(m, p, b, t) {
  return new Promise((res, rej) => {
    const d = b ? JSON.stringify(b) : null
    const h = { 'Content-Type': 'application/json', 'X-Tenant-Id': TENANT, ...(t ? { Authorization: 'Bearer ' + t } : {}) }
    if (d) h['Content-Length'] = Buffer.byteLength(d)
    const r = http.request({ hostname: 'localhost', port: PORT, path: p, method: m, headers: h }, x => {
      const c = []
      x.on('data', d => c.push(d))
      x.on('end', () => {
        const s = Buffer.concat(c).toString()
        let j = null
        try { j = JSON.parse(s) } catch (e) { /* ignore */ }
        res({ s: x.statusCode, j, b: s })
      })
    })
    r.on('error', rej); if (d) r.write(d); r.end()
  })
}

let pass = 0, fail = 0
const failures = []
const ck = (n, ok, d) => {
  if (ok) { pass++; console.log(`  ✔ ${n}${d ? ' — ' + d : ''}`) }
  else { fail++; failures.push(n); console.log(`  ✘ ${n}${d ? ' — ' + d : ''}`) }
}

;(async () => {
  console.log(`\nC 端写路径往返验证 —— 后端 :${PORT}\n`)
  const lg = await req('POST', '/api/v1/mall/auth/login', { username: 'demo_buyer', password: 'admin123' })
  const t = lg.j?.data?.token
  ck('演示买家登录', !!t, lg.s === 200 ? '' : lg.b.slice(0, 120))
  if (!t) { process.exitCode = 1; return }

  // ═══ 地址簿 CRUD ═══
  console.log('\n── ① 地址簿 CRUD（原先映射了 6 个不存在的列 ⇒ 全 500）──')
  const before = await req('GET', '/api/v1/mall/user/addresses', null, t)
  ck('列表可读', before.s === 200, `status=${before.s} 条数=${before.j?.data?.length ?? '-'}`)

  const created = await req('POST', '/api/v1/mall/user/addresses', {
    consignee: '往返测试收货人', phone: '13700000001',
    region: '内蒙古自治区包头市青山区', address: '审计临时地址 1 号', isDefault: false
  }, t)
  ck('新增成功', created.s === 200 && !!created.j?.data?.id, `status=${created.s} id=${created.j?.data?.id}`)
  const aid = created.j?.data?.id

  const listed = await req('GET', '/api/v1/mall/user/addresses', null, t)
  const hit = (listed.j?.data || []).find(a => String(a.id) === String(aid))
  ck('读回字段完整（consignee/region/address 都对）',
    !!hit && hit.consignee === '往返测试收货人' && hit.region === '内蒙古自治区包头市青山区' && hit.address === '审计临时地址 1 号',
    hit ? `${hit.consignee} | ${hit.region} | ${hit.address}` : '未找到')

  const upd = await req('PUT', `/api/v1/mall/user/addresses/${aid}`, {
    consignee: '往返测试收货人改', phone: '13700000002',
    region: '内蒙古自治区包头市昆都仑区', address: '审计临时地址 2 号', isDefault: false
  }, t)
  ck('更新成功', upd.s === 200, `status=${upd.s}`)
  const afterUpd = await req('GET', '/api/v1/mall/user/addresses', null, t)
  const u2 = (afterUpd.j?.data || []).find(a => String(a.id) === String(aid))
  ck('更新已落库', u2?.address === '审计临时地址 2 号' && u2?.phone === '13700000002', `address=${u2?.address}`)

  const def = await req('PUT', `/api/v1/mall/user/addresses/${aid}/default`, null, t)
  const afterDef = await req('GET', '/api/v1/mall/user/addresses', null, t)
  const d2 = (afterDef.j?.data || []).find(a => String(a.id) === String(aid))
  ck('设默认成功且落库', def.s === 200 && d2?.isDefault === true, `status=${def.s} isDefault=${d2?.isDefault}`)

  const del = await req('DELETE', `/api/v1/mall/user/addresses/${aid}`, null, t)
  const afterDel = await req('GET', '/api/v1/mall/user/addresses', null, t)
  ck('删除成功且列表不再含它',
    del.s === 200 && !(afterDel.j?.data || []).some(a => String(a.id) === String(aid)),
    `剩余 ${afterDel.j?.data?.length ?? '-'} 条`)

  // ═══ 购物车 CRUD ═══
  console.log('\n── ② 购物车 CRUD（原先映射 customer_id/subtotal/checked 等不存在的列）──')
  const cart0 = await req('GET', '/api/v1/mall/cart', null, t)
  ck('购物车可读', cart0.s === 200, `status=${cart0.s} 条数=${cart0.j?.data?.length ?? (Array.isArray(cart0.j) ? cart0.j.length : '-')}`)

  // 取一个在售商品（product_id 用 v_mall_product 的 product_id 口径）
  const prods = await req('GET', '/api/v1/mall/products?page=1&size=1', null, t)
  const p = prods.j?.data?.records?.[0]
  ck('能取到在售商品用于加购', !!p, p ? `${p.productName}` : '无商品')
  if (p) {
    const add = await req('POST', '/api/v1/mall/cart', { productId: p.productId, quantity: 1 }, t)
    ck('加入购物车成功', add.s === 200, `status=${add.s} ${add.b.slice(0, 80)}`)
    const cart1 = await req('GET', '/api/v1/mall/cart', null, t)
    const items = Array.isArray(cart1.j) ? cart1.j : (cart1.j?.data ?? [])
    ck('购物车能读回该行', cart1.s === 200 && items.length > 0, `条数=${items.length}`)
    const rowId = items[0]?.id
    if (rowId) {
      const delRow = await req('DELETE', `/api/v1/mall/cart/${rowId}`, null, t)
      ck('删除购物车行成功', delRow.s === 200, `status=${delRow.s}`)
      const cart2 = await req('GET', '/api/v1/mall/cart', null, t)
      const items2 = Array.isArray(cart2.j) ? cart2.j : (cart2.j?.data ?? [])
      ck('删后购物车为空（自清理完成）', items2.length === 0, `条数=${items2.length}`)
    }
  }

  console.log(`\n═══ 汇总：${pass}/${pass + fail} 通过 ═══`)
  if (failures.length) console.log('失败项：' + failures.join(' | '))
  process.exitCode = fail ? 1 : 0
})().catch(e => { console.error('异常:', e.message); process.exitCode = 1 })
