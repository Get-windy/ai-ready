#!/usr/bin/env node
/**
 * B2B 租户商城 · 游客浏览与价格显隐验证（2026-09-21）
 *
 * 背景：`tenant_shop_config` 的 `allowGuest`（是否允许游客访问）与 `guestShowPrice`
 * （游客是否显示价格）两个字段**早就存在**、前端开关也早就就位，但后端从未消费 ——
 * 即两个**空开关**。本轮把它们接到真实行为上，本脚本验证接线确实生效。
 *
 * 覆盖（全部按「拒绝路径 + 放行路径」两向断言）：
 *   ① 店铺未开游客（NOT_ALLOW）→ 游客 403
 *   ② 开游客 + 游客隐藏价格（ALLOW/HIDE）→ 游客 200 且价格字段为空
 *   ③ 开游客 + 游客显示价格（ALLOW/SHOW）→ 游客 200 且价格字段有值
 *   ④ 不带店铺标识（无 X-Tenant-Id、未登录）→ 400，不是静默查全表/查 tenant_id=0
 *   ⑤ 已登录买家不受 `guestShowPrice` 影响
 *
 * 用法：node tools/verify-mall-guest.cjs
 * 纪律：脚本会临时改店铺配置，结束前**必定还原**（finally）。
 */
const PORT = process.env.PORT || '5655'
const BASE = `http://localhost:${PORT}/api`
const SHOP_TENANT = process.env.SHOP_TENANT || '1'
const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

async function req(method, p, { token, tenant, body } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(tenant ? { 'X-Tenant-Id': String(tenant) } : {}),
    },
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

/** 游客视角取商品列表第一页 */
const guestProducts = () =>
  req('GET', '/v1/mall/products?page=1&size=3', { tenant: SHOP_TENANT })

;(async () => {
  console.log(`验证目标: ${BASE}  店铺租户: ${SHOP_TENANT}`)
  const admin = await login(ADMIN)
  console.log(`登录成功: ${ADMIN.u}（超管）`)

  // 记录原配置，finally 还原
  const before = (await req('GET', '/erp/mall/admin/config', { token: admin })).json?.data || {}
  const restore = { allowGuest: before.allowGuest ?? 'NOT_ALLOW', guestShowPrice: before.guestShowPrice ?? 'HIDE' }
  console.log(`原配置: allowGuest=${restore.allowGuest}, guestShowPrice=${restore.guestShowPrice}`)

  const setCfg = (body) => req('PUT', '/erp/mall/admin/config', { token: admin, body })

  try {
    // ══ ① 店铺未开游客 → 游客被拒 ══
    section('① allowGuest=NOT_ALLOW：游客进不来')
    await setCfg({ allowGuest: 'NOT_ALLOW', guestShowPrice: 'HIDE' })
    const denied = await guestProducts()
    ok('游客 403', denied.status === 403, `status=${denied.status} ${denied.json?.message || ''}`)

    // ══ ② 开游客 + 隐藏价格 ══
    section('② allowGuest=ALLOW + guestShowPrice=HIDE：能看商品、看不到价格')
    await setCfg({ allowGuest: 'ALLOW', guestShowPrice: 'HIDE' })
    const hide = await guestProducts()
    ok('游客 200', hide.status === 200, `status=${hide.status} ${hide.json?.message || ''}`)
    const hideRows = hide.json?.data?.records || []
    ok('返回了商品（证明确实进得去店）', hideRows.length > 0, `条数=${hideRows.length}`)
    ok('价格字段被隐藏（salePrice/marketPrice 均为空）',
      hideRows.every(r => (r.salePrice == null) && (r.marketPrice == null)),
      `样本 salePrice=${hideRows[0]?.salePrice}, marketPrice=${hideRows[0]?.marketPrice}`)
    ok('非价格字段仍在（商品名）', !!hideRows[0]?.productName, `productName=${hideRows[0]?.productName}`)

    // ══ ③ 开游客 + 显示价格 ══
    section('③ allowGuest=ALLOW + guestShowPrice=SHOW：价格可见')
    await setCfg({ guestShowPrice: 'SHOW' })
    const show = await guestProducts()
    ok('游客 200', show.status === 200, `status=${show.status}`)
    const showRows = show.json?.data?.records || []
    // ⚠️ 断言用 marketPrice 而不是 salePrice：实测本环境 4 个在售商品的
    //    `sale_price`（源自 erp_product.retail_price）**全为 NULL**，
    //    拿它当「价格是否可见」的判据会永远失败（假失败）。
    //    判据取「至少一个价格字段非空」最稳。
    ok('价格字段有值', showRows.some(r => r.marketPrice != null || r.salePrice != null),
      `样本 salePrice=${showRows[0]?.salePrice}, marketPrice=${showRows[0]?.marketPrice}`)

    // ══ ④ 无店铺标识 → 明确报错 ══
    section('④ 不带 X-Tenant-Id 且未登录：明确 400，而不是静默查空/查全表')
    const noShop = await req('GET', '/v1/mall/products?page=1&size=3')
    ok('返回 400 且提示无法确定店铺',
      noShop.status === 400 && /无法确定店铺/.test(noShop.json?.message || ''),
      `status=${noShop.status} ${noShop.json?.message || ''}`)

    // ══ ⑤ 已登录买家不受价格开关影响 ══
    section('⑤ 已登录用户不受 guestShowPrice 影响')
    await setCfg({ guestShowPrice: 'HIDE' })
    const logged = await req('GET', '/v1/mall/products?page=1&size=3', { token: admin })
    const loggedRows = logged.json?.data?.records || []
    ok('已登录调用返回 200', logged.status === 200, `status=${logged.status} ${logged.json?.message || ''}`)
    ok('已登录时价格未被隐藏', loggedRows.length === 0 || loggedRows.some(r => r.marketPrice != null || r.salePrice != null),
      `条数=${loggedRows.length}, marketPrice=${loggedRows[0]?.marketPrice}`)
  } finally {
    section('↩ 还原店铺配置')
    const r = await setCfg(restore)
    ok('已还原为原配置', r.status === 200,
      `allowGuest=${restore.allowGuest}, guestShowPrice=${restore.guestShowPrice}`)
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => {
  console.error('\n验证脚本异常:', e.message)
  process.exit(2)
})
