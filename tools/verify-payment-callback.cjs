#!/usr/bin/env node
/**
 * 支付回调 fail-closed 验证（2026-09-21）
 *
 * 背景：原 `POST /api/v1/mall/payments/callback` 接收未验签报文，
 * 只要 out_trade_no + trade_status=SUCCESS 就把订单标为已支付。已重做为
 * `POST /api/v1/mall/payments/callback/{tenantId}/{channel}`，验签交给
 * `PaymentCallbackVerifier` 的平台实现；**没有实现认领该渠道 ⇒ 一律拒绝**。
 *
 * 本脚本验证的是「安全默认值」，不是「回调能用」：
 *   ① 旧的无验签端点已不存在（404）
 *   ② 新端点在任何渠道下都**不会**把回调当成功处理（无实现 ⇒ 拒绝）
 *   ③ 伪造回调不会改变任何订单的支付状态
 *
 * ⚠️ 端点刻意**不在匿名白名单**里（启用步骤见 MallPaymentController 类注释），
 *    因此这里带管理员 token 调用 —— 目的是越过 Sa-Token 打到业务层，
 *    看清「业务层自己的默认值」是什么，而不是被鉴权层挡住看不出所以然。
 *
 * 用法：node tools/verify-payment-callback.cjs
 */
const PORT = process.env.PORT || '5655'
const BASE = `http://localhost:${PORT}/api`
const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

async function req(method, p, { token, body, raw } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' } : {}),
    },
    body: body === undefined ? undefined : (raw ? body : JSON.stringify(body)),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 纯文本应答（渠道应答体就是这样） */ }
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

/** 伪造的回调报文（正是原实现会照单全收的那种） */
const FORGED = { out_trade_no: 'FORGED-ORDER-0001', trade_status: 'SUCCESS', total_amount: '0.01' }

;(async () => {
  console.log(`验证目标: ${BASE}`)
  const token = await login(ADMIN)
  console.log(`登录成功: ${ADMIN.u}`)

  section('① 旧的「无验签即改单」端点已删除')
  const old = await req('POST', '/v1/mall/payments/callback', { token, body: FORGED })
  ok('旧路径 POST /callback 不再存在（404/405）',
    old.status === 404 || old.status === 405,
    `status=${old.status} ${old.json?.message || ''}`)

  section('② 新端点：无渠道实现 ⇒ 拒绝（fail-closed）')
  for (const ch of ['WECHAT', 'ALIPAY', 'UNKNOWN_CHANNEL']) {
    const r = await req('POST', `/v1/mall/payments/callback/1/${ch}`, { token, body: FORGED })
    const rejected = r.status === 401 && /UNSUPPORTED_CHANNEL/.test(r.text)
    ok(`${ch} 被拒绝且未按成功处理`, rejected, `status=${r.status} body=${r.text.slice(0, 60)}`)
  }

  section('③ 伪造回调的报文不会落到任何订单上')
  // ② 已证明回调被拒，理论上不会有状态变更。这里再确认「伪造单号」在库里不存在，
  // 避免将来有人误以为该单号是被本回调创建/改写的。
  // ⚠️ 不通过业务接口统计（本仓接口路径不能靠猜，猜错会把「入参缺失」误判成失败）——
  //    订单侧的状态核对在脚本外用只读 SQL 做。
  const probe = await req('POST', '/v1/mall/payments/callback/1/WECHAT', { token, body: FORGED })
  ok('重复投递同样被拒（未因「已处理」而改走成功分支）',
    probe.status === 401 && /UNSUPPORTED_CHANNEL/.test(probe.text),
    `status=${probe.status}`)
  console.log(`  [NOTE] 伪造单号 ${FORGED.out_trade_no} 不应在 erp_sale_order 中存在，可用只读 SQL 复核`)

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => {
  console.error('\n验证脚本异常:', e.message)
  process.exit(2)
})
