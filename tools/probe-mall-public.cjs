#!/usr/bin/env node
/**
 * 商城 C 端公开性探测（2026-09-21，只读）
 *
 * 背景：`SaTokenConfig` 的白名单里**只有** `/api/v1/mall/auth/**`。
 * 对商城而言至少三类路径的公开性需要核实：
 *   ① 游客浏览商品（列表/详情/分类/搜索/推荐/热门/轮播）
 *   ② 支付网关回调 `POST /api/v1/mall/payments/callback` —— 微信/支付宝服务器**不带商城用户 token**，
 *      若被要求登录就会 401，订单永远停在"待支付"
 *   ③ 其余需登录、但只操作本人数据的（购物车/订单/用户）
 *
 * 判据：不带任何 token 直接打，看是否被 SaInterceptor 拦成 401/「请先登录」。
 * 注意区分「未登录被拦(401)」与「未登录但业务层拒绝(400/403)」—— 后者说明路径是通的。
 */
const PORT = process.env.PORT || '5655'
const BASE = `http://localhost:${PORT}/api`

const PROBES = [
  ['① 商品浏览', 'GET', '/v1/mall/products'],
  ['① 商品浏览', 'GET', '/v1/mall/products/categories'],
  ['① 商品浏览', 'GET', '/v1/mall/products/hot'],
  ['① 商品浏览', 'GET', '/v1/mall/products/banners'],
  ['② 支付回调', 'POST', '/v1/mall/payments/callback'],
  ['③ 购物车', 'GET', '/v1/mall/cart'],
  ['③ 我的订单', 'GET', '/v1/mall/orders'],
  ['③ 用户信息', 'GET', '/v1/mall/user'],
]

;(async () => {
  console.log(`探测目标: ${BASE}\n`)
  for (const [group, method, p] of PROBES) {
    let status = 0, msg = ''
    try {
      const res = await fetch(BASE + p, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: method === 'POST' ? '{}' : undefined,
      })
      status = res.status
      const t = await res.text()
      try { msg = JSON.parse(t).message || '' } catch { msg = t.slice(0, 60) }
    } catch (e) { msg = 'ERR ' + e.message }
    const blocked = status === 401 || /请先登录|未登录/.test(msg)
    console.log(`  ${group}  ${method.padEnd(5)} ${p.padEnd(34)} → ${String(status).padEnd(4)} ${blocked ? '【需登录】' : '【未登录可通过】'}  ${msg}`)
  }
})()
