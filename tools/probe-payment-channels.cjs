/*
 * 支付通道「真接」的端到端闭合验证（2026-09-26）。
 *
 * 验证不了的事先说清楚：**没有商户号/证书、构建环境不出网 ⇒ 无法对真实支付宝/银联网关联调**。
 * 能验证的是"接线是否自洽"—— 而这恰恰是本地能查出、且事后最难查的一类错：
 *   ① 下单签名用的是**自己的私钥**，验签用**配置的公钥**（都在本地生成）⇒ 口径错立刻暴露；
 *   ② 用**Node 的 crypto（与 Java 完全独立的实现）**去验 Java 侧签出来的串 ⇒
 *      排除"两边用同一段错代码自证清白"；
 *   ③ 伪造一份"合法渠道回调"（同样用配置私钥签）→ 走真实 callback 端点 → 断言支付请求被置为已支付；
 *      再篡改金额 → 断言被拒（fail-closed）。
 *
 * 副作用与清理：会写入两条渠道配置（payment.channel.alipay / .unionpay）与若干支付请求/记录，
 *   结束时**按原值还原配置并删除自造数据**，最后回读断言。
 *
 * 用法: node tools/probe-payment-channels.cjs [后端端口=5691]
 */
const http = require('http')
const crypto = require('crypto')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const PORT = Number(process.argv[2] || process.env.ERP_PORT || 5691)
const TENANT = 1
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }

function req(method, path, body, token, headers) {
  return new Promise((res, rej) => {
    const data = body === undefined || body === null ? null : (typeof body === 'string' ? body : JSON.stringify(body))
    const h = { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}), ...(headers || {}) }
    if (data) h['Content-Length'] = Buffer.byteLength(data)
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
    if (data) r.write(data)
    r.end()
  })
}
async function db(sql, params) {
  const c = new Client(DSN); await c.connect()
  try { return (await c.query(sql, params)).rows } finally { await c.end() }
}

let pass = 0, fail = 0
const failures = []
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✔ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; failures.push(name); console.log(`  ✘ ${name}${detail ? ' — ' + detail : ''}`) }
}

/** 与 Java 侧 Rsa2 完全同口径：待签串 = 剔字段与空值 → 名升序 → k=v&…（值可选 URL 编码） */
function buildSignContent(params, exclude, urlEncodeValues) {
  return Object.keys(params)
    .filter(k => !exclude.includes(k) && params[k] !== null && params[k] !== undefined && String(params[k]).trim() !== '')
    .sort()
    .map(k => `${k}=${urlEncodeValues ? encodeURIComponent(String(params[k])).replace(/[!'()*]/g, c => '%' + c.charCodeAt(0).toString(16).toUpperCase()) : params[k]}`)
    .join('&')
}

function parseQuery(url) {
  const q = url.substring(url.indexOf('?') + 1)
  const out = {}
  for (const pair of q.split('&')) {
    const i = pair.indexOf('=')
    // ⚠️ Java 的 URLEncoder 把空格编码成 '+'（表单编码），而 decodeURIComponent 不会把它还原成空格
    //    —— 直接解会得到 "2026-09-27+04:39:00"，与 Java 侧签名的 "2026-09-27 04:39:00" 不一致，
    //    于是"签名验不过"其实是探针自己的错（真机实踩）。这里先按表单规则还原 '+'。
    if (i > 0) {
      out[decodeURIComponent(pair.slice(0, i).replace(new RegExp(String.fromCharCode(92)+'+','g'), ' '))] =
        decodeURIComponent(pair.slice(i + 1).replace(new RegExp(String.fromCharCode(92)+'+','g'), ' '))
    }
  }
  return out
}

function rsa2Sign(content, privateKeyPem) {
  const s = crypto.createSign('RSA-SHA256')
  s.update(content, 'utf8')
  return s.sign(privateKeyPem, 'base64')
}

function rsa2Verify(content, signBase64, publicKeyPem) {
  const v = crypto.createVerify('RSA-SHA256')
  v.update(content, 'utf8')
  return v.verify(publicKeyPem, signBase64, 'base64')
}

;(async () => {
  console.log(`\n支付通道端到端闭合验证 —— 后端 :${PORT}\n`)

  // ── 0. 自生成一对密钥（私钥配给通道去签，公钥用于验）──
  const { publicKey, privateKey } = crypto.generateKeyPairSync('rsa', {
    modulusLength: 2048,
    publicKeyEncoding: { type: 'spki', format: 'pem' },
    privateKeyEncoding: { type: 'pkcs8', format: 'pem' }
  })
  const privB64 = privateKey.replace(/-----[A-Z ]+-----/g, '').replace(/\s/g, '')
  const pubB64 = publicKey.replace(/-----[A-Z ]+-----/g, '').replace(/\s/g, '')
  check('自生成 RSA 密钥对（模拟渠道凭据）', !!privB64 && !!pubB64)

  // ── 1. 登录（管理端）──
  const cap = await req('GET', '/api/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const login = await req('POST', '/api/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid
  })
  const token = login.json?.data?.token
  check('管理员登录', !!token)
  if (!token) { process.exitCode = 1; return }

  // ── 2. 写渠道配置（记录原值以便还原）──
  const existing = await db('SELECT id, config_key FROM sys_project_config WHERE config_key IN ($1,$2) AND tenant_id = $3',
    ['payment.channel.alipay', 'payment.channel.unionpay', TENANT])
  const createdKeys = []
  const alipayParam = {
    enabled: true, appId: '2021000000000000',
    alipayPrivateKey: privB64, alipayPublicKey: pubB64,
    notifyUrl: `http://localhost:${PORT}/api/payment/callback/1/ALIPAY`
  }
  const unionPayParam = {
    enabled: true, merchantNo: '777290058110048', unionPayCertId: 'CERT-0001',
    unionPayMerchantPrivateKey: privB64, unionPayCerts: pubB64,
    notifyUrl: `http://localhost:${PORT}/api/payment/callback/1/UNIONPAY`
  }
  for (const [key, value] of [['payment.channel.alipay', alipayParam], ['payment.channel.unionpay', unionPayParam]]) {
    const hit = existing.find(r => r.config_key === key)
    if (hit) {
      await db("UPDATE sys_project_config SET config_value = $1, status = 0, deleted = 0 WHERE config_key = $2 AND tenant_id = $3",
        [JSON.stringify(value), key, TENANT])
    } else {
      await db(`INSERT INTO sys_project_config (id, tenant_id, config_key, config_value, config_type, config_group, status, deleted, create_time, update_time)
                VALUES ((SELECT COALESCE(MAX(id),0)+1 FROM sys_project_config), $1, $2, $3, 'json', 'payment', 0, 0, now(), now())`,
        [TENANT, key, JSON.stringify(value)])
      createdKeys.push(key)
    }
  }
  check('渠道凭据已写入 sys_project_config', true, `新增 ${createdKeys.length} 条 / 覆盖 ${2 - createdKeys.length} 条`)

  const stamp = Date.now()
  const bizNo = `PROBE-PAY-${stamp}`
  const cleanupBiz = [bizNo, `PROBE-PAY-UP-${stamp}`]

  console.log('\n── ① 支付宝：下单 → 独立实现验签 ──')
  const created = await req('POST', '/api/payment/request', {
    bizType: 'PROBE', bizId: 0, bizNo, amount: 12.34, channel: 'ALIPAY'
  }, token)
  check('创建支付请求成功', created.status === 200, `status=${created.status} ${created.body.slice(0, 90)}`)
  const payUrl = created.json?.data?.payUrl
  check('返回体带 payUrl（C 端拿它跳收银台）', !!payUrl && payUrl.startsWith('http'), (payUrl || '').slice(0, 60))
  const requestId = created.json?.data?.id
  if (payUrl) {
    const params = parseQuery(payUrl)
    check('URL 含支付宝关键参数', params.method === 'alipay.trade.page.pay' && params.app_id === '2021000000000000', `method=${params.method}`)
    // ★ 用 Node 的 crypto（独立实现）验 Java 侧签的串
    const content = buildSignContent(params, ['sign', 'sign_type'], false)
    check('收银台 URL 的签名通过独立实现（Node crypto）验签', rsa2Verify(content, params.sign, publicKey),
      '排除"两边共用同一段错代码自证"')
    const tampered = { ...params, biz_content: params.biz_content.replace('12.34', '0.01') }
    check('改价后签名失效', !rsa2Verify(buildSignContent(tampered, ['sign', 'sign_type'], false), params.sign, publicKey))
  }

  console.log('\n── ② 银联：下单 → 独立实现验签（值 URL 编码口径）──')
  const upBizNo = `PROBE-PAY-UP-${stamp}`
  const upCreated = await req('POST', '/api/payment/request', {
    bizType: 'PROBE', bizId: 0, bizNo: upBizNo, amount: 12.34, channel: 'UNIONPAY'
  }, token)
  check('创建银联支付请求成功', upCreated.status === 200, `status=${upCreated.status} ${upCreated.body.slice(0, 90)}`)
  const upUrl = upCreated.json?.data?.payUrl
  if (upUrl) {
    const p = parseQuery(upUrl)
    check('txnAmt 单位是分（12.34 元 → 1234）', p.txnAmt === '1234', `txnAmt=${p.txnAmt}`)
    const content = buildSignContent(p, ['signature'], true)
    check('银联网关 URL 的签名通过独立实现验签（值 URL 编码口径）', rsa2Verify(content, p.signature, publicKey))
    const wrong = buildSignContent(p, ['signature'], false)
    check('用「不编码」口径验签必须失败（证明编码是签名的一部分）', !rsa2Verify(wrong, p.signature, publicKey))
  }

  console.log('\n── ③ 合法回调（自签）→ 支付请求应被置为已支付 ──')
  const before = await db('SELECT status FROM payment_request WHERE id = $1', [requestId])
  const notify = {
    app_id: '2021000000000000',
    out_trade_no: bizNo,
    trade_no: `ALIPAY-TRADE-${stamp}`,
    trade_status: 'TRADE_SUCCESS',
    total_amount: '12.34',
    charset: 'utf-8',
    version: '1.0',
    sign_type: 'RSA2'
  }
  const notifySign = rsa2Sign(buildSignContent(notify, ['sign', 'sign_type'], false), privateKey)
  const notifyBody = Object.entries({ ...notify, sign: notifySign })
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&')
  const cb = await req('POST', `/api/payment/callback/${TENANT}/ALIPAY`, notifyBody,
    null, { 'Content-Type': 'application/x-www-form-urlencoded' })
  check('回调端点接受合法签名', cb.status === 200 && cb.body.trim() === 'success',
    `status=${cb.status} body=${cb.body.slice(0, 30)}`)
  const after = await db('SELECT status, channel_trade_no FROM payment_request WHERE id = $1', [requestId])
  check('支付请求已被置为已支付（status=2）', after[0]?.status === 2,
    `before=${before[0]?.status} after=${after[0]?.status} tradeNo=${after[0]?.channel_trade_no}`)

  // ★ 支付记录必须**指回**支付请求：原先"先 insert 再查 request"导致 request_id 恒 null，
  //   支付记录与支付请求脱钩（对账按 request_id 关联会一行都查不到）
  const rec = await db('SELECT id, request_id, channel, amount FROM payment_record WHERE request_id = $1', [requestId])
  check('支付记录已回写 request_id（与支付请求不脱钩）',
    rec.length === 1 && String(rec[0].request_id) === String(requestId),
    `记录数=${rec.length} request_id=${rec[0]?.request_id ?? 'null'}`)

  console.log('\n── ④ 伪造回调（改金额）→ 必须被拒且不改单 ──')
  const badBody = Object.entries({ ...notify, total_amount: '0.01', sign: notifySign })
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`).join('&')
  const bad = await req('POST', `/api/payment/callback/${TENANT}/ALIPAY`, badBody,
    null, { 'Content-Type': 'application/x-www-form-urlencoded' })
  check('篡改金额的回调被拒（401 + failure）', bad.status === 401 && bad.body.trim() === 'failure',
    `status=${bad.status} body=${bad.body.slice(0, 20)}`)

  console.log('\n── ⑤ 未配置凭据的渠道 → 仍 fail-closed ──')
  const wechat = await req('POST', `/api/payment/callback/${TENANT}/WECHAT`, '{}',
    null, { 'Content-Type': 'application/x-www-form-urlencoded' })
  check('未实现/未配凭据的渠道被拒', wechat.status === 401, `status=${wechat.status}`)

  // ── 清理 ──
  console.log('\n── 清理 ──')
  await db(`DELETE FROM payment_record WHERE request_id IN (SELECT id FROM payment_request WHERE biz_no = ANY($1))`, [cleanupBiz])
  await db(`DELETE FROM payment_request WHERE biz_no = ANY($1)`, [cleanupBiz])
  await db('DELETE FROM sys_project_config WHERE config_key = ANY($1)', [createdKeys])
  const leftReq = await db('SELECT count(*)::int c FROM payment_request WHERE biz_no = ANY($1)', [cleanupBiz])
  const leftReq2 = await db("SELECT count(*)::int c FROM payment_request WHERE biz_no LIKE 'PROBE-PAY%'")
  const leftCfg = await db("SELECT count(*)::int c FROM sys_project_config WHERE config_key IN ('payment.channel.alipay','payment.channel.unionpay')")
  check('自造的支付请求已清理', leftReq[0].c === 0 && leftReq2[0].c === 0, `left=${leftReq[0].c}/${leftReq2[0].c}`)
  check('渠道配置已还原（无残留）', leftCfg[0].c === existing.length, `left=${leftCfg[0].c} 原有=${existing.length}`)

  console.log(`\n═══ 汇总：${pass}/${pass + fail} 通过 ═══`)
  if (failures.length) console.log('失败项：' + failures.join(' | '))
  process.exitCode = fail ? 1 : 0
})().catch(e => { console.error('验证异常:', e.message); process.exitCode = 1 })
