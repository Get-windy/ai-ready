#!/usr/bin/env node
/**
 * SAL-BREAK + PUR-BREAK 修复验证（2026-09-21）· 需先重启后端
 *
 * 覆盖两处代码级缺陷（数据统计口径的更正见 MASTER_TODO，不在此脚本内）：
 *
 *  ① **改单不能删光明细**（SAL-BREAK-02 顺带查出）：`SaleOrderServiceImpl.updateOrder`
 *     原为「无条件删旧明细 + 有条件（items != null）重建」—— 删建条件不一致，
 *     于是任何一次**不带明细的更新**都会把该单全部明细删掉且不补回，而主表照常更新。
 *     现在语义钉死：`items == null` → 不动明细；`items == []` → 显式清空；非空 → 先删后插。
 *
 *  ② **采购分析的供应商取数**（PUR-BREAK-03）：原先 `LEFT JOIN erp_supplier`（该表 0 行）
 *     且标签取 `MAX(po.supplier_name)`（订单表该列 0/11 非空）⇒ 「按供应商」维度的
 *     标签与编码**双双恒空**。现改为「订单表冗余列 → 供应商快照 → biz_party 档案」三级取值。
 *     断言用**响应体里能不能搜到那两个字面值**，不依赖报表的 JSON 形状。
 *
 * 用法：node tools/verify-sale-pur-breaks.cjs
 * 副作用：创建 1 张销售订单并在 finally 里删除。
 */
const { execFileSync } = require('child_process')
const path = require('path')

const PORT = process.env.PORT || '5655'
const BASE = `http://localhost:${PORT}/api`
const REPO = path.resolve(__dirname, '..')
const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

function sql(stmt) {
  return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt], {
    cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024,
  })
}
function rowsOf(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').trim().split('\n')
    .filter(l => l.trim() && !/^\(\d+ rows\)$/.test(l.trim()) && !/^-+$/.test(l.trim()))
  return lines.slice(1).map(l => l.split('|').map(s => s.trim()))
}
const scalar = (stmt) => { const r = rowsOf(stmt); return r.length ? r[0][0] : null }

async function req(method, p, { token, body } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON */ }
  return { status: res.status, json, text }
}

async function login(user) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    body: { username: user.u, password: user.p, tenantName: user.t, captcha: code, captchaKey: cap.json.data.uuid },
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`${user.u} 登录失败: ${res.text.slice(0, 300)}`)
  return token
}

/** 明细行数（erp_sale_order_item 无 deleted 列，是物理删除） */
const itemCount = (orderId) =>
  Number(scalar(`SELECT count(*) FROM erp_sale_order_item WHERE order_id = ${orderId}`))

const item = (lineNo, qty, price) => ({
  lineNo, productId: '990000000000000001',
  productName: '博多家园百香果果酱', unit: '瓶',
  quantity: qty, unitPrice: price, calculatedPrice: price,
})

let orderId = null

;(async () => {
  console.log(`验证目标: ${BASE}`)
  try {
    const token = await login(ADMIN)
    console.log(`登录成功: ${ADMIN.u}`)

    // ══════════ ① 改单不得删光明细 ══════════
    section('① 改单语义 —— items 缺省 = 不动明细（修复前会被删光）')

    const created = await req('POST', '/erp/sale/order', {
      token,
      body: {
        customerId: 1, orderDate: '2026-09-21', saleType: 1,
        items: [item(1, 2, 100), item(2, 1, 50)],
      },
    })
    orderId = created.json?.data
    ok('建单成功（带 2 行明细）', created.status === 200 && !!orderId, `id=${orderId}`)
    if (!orderId) { throw new Error('建单失败，后续断言无意义: ' + created.text.slice(0, 200)) }
    ok('前置：明细落库 2 行', itemCount(orderId) === 2, `实际=${itemCount(orderId)}`)

    // 只改表头（不改明细）：body 里**不带 items**
    const headerOnly = await req('PUT', `/erp/sale/order/${orderId}`, {
      token,
      body: { id: orderId, customerId: 1, orderDate: '2026-09-21', saleType: 1, summary: '只改摘要' },
    })
    ok('不带 items 的更新返回成功', headerOnly.status === 200, `status=${headerOnly.status}`)
    ok('★ 明细仍是 2 行（未被删光）', itemCount(orderId) === 2, `实际=${itemCount(orderId)}`)
    ok('表头确实被更新了（说明这次 PUT 生效了，不是"整单没保存"）',
      scalar(`SELECT summary FROM erp_sale_order WHERE id = ${orderId}`) === '只改摘要',
      `summary=${scalar(`SELECT summary FROM erp_sale_order WHERE id = ${orderId}`)}`)

    // 带 1 行明细更新 → 先删后插，应替换为 1 行
    const replaced = await req('PUT', `/erp/sale/order/${orderId}`, {
      token,
      body: { id: orderId, customerId: 1, orderDate: '2026-09-21', saleType: 1, items: [item(1, 5, 20)] },
    })
    ok('带 items 的更新返回成功', replaced.status === 200, `status=${replaced.status}`)
    ok('明细被替换为 1 行（先删后插语义保留）', itemCount(orderId) === 1, `实际=${itemCount(orderId)}`)
    ok('留存的那行是新的数量 5', scalar(`SELECT quantity FROM erp_sale_order_item WHERE order_id = ${orderId}`) === '5.00',
      `实际=${scalar(`SELECT quantity FROM erp_sale_order_item WHERE order_id = ${orderId}`)}`)

    // 显式传空数组 → 明确清空（这是"我要清空"的表达，与"没传"区分开）
    await req('PUT', `/erp/sale/order/${orderId}`, {
      token, body: { id: orderId, customerId: 1, orderDate: '2026-09-21', saleType: 1, items: [] },
    })
    ok('显式传 items: [] → 明细被清空（空数组≠缺省）', itemCount(orderId) === 0, `实际=${itemCount(orderId)}`)

    // ══════════ ② 采购分析「按供应商」维度不再恒空 ══════════
    section('② 采购分析 —— 供应商标签/编码有值（修复前两者恒空）')
    const pre = rowsOf(`SELECT count(*) FROM erp_purchase_order po `
      + `LEFT JOIN erp_purchase_order_partner_snapshot ps ON ps.order_id = po.id`)
    ok('前置：确有采购订单可供分析', Number(pre[0][0]) > 0, `订单数=${pre[0][0]}`)

    const sup = await req('GET', '/erp/purchase/analytics/page?tab=supplier&page=1&size=5', { token })
    ok('分析接口返回 200', sup.status === 200, `status=${sup.status}`)

    // 前提：`erp_supplier` 是空表 ⇒ 下面两个值**不可能**来自它，只能来自新链路
    const supRows = Number(scalar('SELECT count(*) FROM erp_supplier'))
    ok('前置：erp_supplier 实测为空表（原取数来源）', supRows === 0, `行数=${supRows}`)

    // 供应商 2099000000000000901 的两张订单：名称/编码都存在**供应商快照**里
    // （订单表 supplier_name 恒空、biz_party 里也没有这个 id）
    const snap = rowsOf(`SELECT supplier_name, supplier_code FROM erp_purchase_order_partner_snapshot `
      + `WHERE supplier_code = 'E2EGYS001' LIMIT 1`)
    ok('前置：该供应商的名称/编码只存在于「供应商快照」表',
      snap.length === 1 && snap[0][0] === 'E2E采购供应商',
      JSON.stringify(snap))

    ok('★ 供应商编码有值（修复前 `MAX(erp_supplier.supplier_code)` 恒空）',
      sup.text.includes('E2EGYS001'), sup.text.slice(0, 200).replace(/\s+/g, ' '))
    ok('★ 供应商标签有值（修复前 `MAX(po.supplier_name)` 恒空——订单表不写该列）',
      sup.text.includes('E2E采购供应商'))
    const rec = (sup.json?.data?.records || []).find(r => r.dimKey === '2099000000000000901')
    ok('该维度行同时带上 dimLabel 与 supplierCode',
      !!rec && !!rec.dimLabel && !!rec.supplierCode,
      rec ? JSON.stringify({ dimLabel: rec.dimLabel, supplierCode: rec.supplierCode }) : '未找到该维度行')

    const other = await req('GET', '/erp/purchase/analytics/page?tab=product&page=1&size=5', { token })
    ok('对照：切到「按商品」维度时不带出供应商字段（不是无条件拼进响应）',
      other.status === 200 && !other.text.includes('E2EGYS001'), `status=${other.status}`)

  } catch (e) {
    fail++
    console.error(`\n!! 异常中止: ${e.message}`)
  } finally {
    section('清理')
    if (orderId) {
      const del = await req('DELETE', `/erp/sale/order/${orderId}`, { token: undefined }).catch(() => null)
      // DELETE 需要鉴权：重新登录一次再删，避免留下测试单
      try {
        const tk = await login(ADMIN)
        await req('DELETE', `/erp/sale/order/${orderId}`, { token: tk })
      } catch { /* ignore */ }
      void del
      const left = scalar(`SELECT count(*) FROM erp_sale_order WHERE id = ${orderId}`)
      console.log(`  [清理] 测试订单 ${orderId}：` + (String(left) === '0' ? '已删除' : `⚠️ 仍存在（deleted=${scalar(`SELECT deleted FROM erp_sale_order WHERE id = ${orderId}`)}）`))
    }
  }

  console.log(`\n结果: PASS=${pass} FAIL=${fail}`)
  if (fail > 0) process.exitCode = 1
})()
