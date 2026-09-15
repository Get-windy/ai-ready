/*
 * 收款管理（配送 → 结算收款 → 收款管理，菜单 80920）金标准端到端验证
 *
 * 口径依据：《收款管理开发文档》§3 金标准目标设计 ——
 *   第一轮（§2.3 五项缺陷）：收款码配置化（杜绝 pay.example.com 假 URL）、支付回调幂等、
 *     台账分页、资金上交/稽核、命名歧义（paymentType / payChannel 分离）；
 *   第二轮（§6.9 遗留）：
 *     ① 未付管理闭环：挂账 → 催收 → 承诺付款 → 核销（含金额自洽校验）
 *     ② 支付流水对账：导入（幂等）→ 逐笔匹配（系统无此笔 / 金额不符 / 掉单）→ 人工匹配 / 忽略
 *     ③ 财务打通：推送 ERP（幂等 + 批量逐单反馈）
 *     ④ 资金安全：现金单笔/单日限额（超限拒绝）+ 交款时限超时预警
 *
 * 用法：node tools/e2e-dms-payment.cjs            （默认后端 5680，前端 5656）
 *      ERP_PORT=5696 node tools/e2e-dms-payment.cjs
 */
const http = require('http')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const PORT = Number(process.env.ERP_PORT || 5680)
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
const E2E_USER = process.env.E2E_USER || 'e2e_dispatch'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const MARK = 'E2EPAY' + Date.now().toString().slice(-6)

function safePath(fullPath) {
  const idx = fullPath.indexOf('?')
  if (idx < 0) return encodeURI(fullPath)
  const path = fullPath.slice(0, idx)
  const query = fullPath.slice(idx + 1).split('&').map(kv => {
    const eq = kv.indexOf('=')
    if (eq < 0) return encodeURIComponent(kv)
    let val = kv.slice(eq + 1)
    try { val = decodeURIComponent(val) } catch (e) { /* keep */ }
    return encodeURIComponent(kv.slice(0, eq)) + '=' + encodeURIComponent(val)
  }).join('&')
  return encodeURI(path) + '?' + query
}

function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: safePath('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* ignore */ }
        resolve({ status: res.statusCode, json })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function dbQuery(sql, params) {
  const client = new Client(DSN)
  await client.connect()
  try { return (await client.query(sql, params)).rows } finally { await client.end() }
}

let TOKEN = null

/** 带 401 自动重登的请求（并行会话共用账号会被 sa-token 互踢） */
async function api(method, reqPath, body) {
  let r = await rawReq(method, reqPath, body, TOKEN)
  const code = r.json && Number(r.json.code)
  if (r.status === 401 || code === 401 || code === 403) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN)
  }
  return r
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 200) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 160) : ''}`)
}
const num = (v) => Number(v) || 0
const dataOf = (r) => (r && r.json && r.json.data !== undefined) ? r.json.data : null

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 200))
  TOKEN = token
}

/** 造一个带代收货款的配送任务（可选配送员/代收金额） */
async function seedTask(suffix, riderId = 990002, cod = 200.0) {
  const rows = await dbQuery(
    `INSERT INTO dms_task (tenant_id, task_no, order_no, order_type, rider_id, rider_name, customer_name,
                           collect_on_delivery, delivery_fee, status, deleted, create_time, update_time, version, print_count)
     VALUES (1, $1, $2, 1, $3, $4, 'E2E收款客户', $5, 15.00, 6, 0, NOW(), NOW(), 0, 0)
     RETURNING id`,
    [`PSD-${MARK}-${suffix}`, `${MARK}-ORD-${suffix}`, riderId, 'E2E收款配送员' + riderId, cod])
  return rows[0].id
}

async function setConfig(key, value) {
  await dbQuery(`UPDATE dms_config SET config_value = $1 WHERE config_key = $2 AND tenant_id = 0 AND deleted = 0`, [value, key])
  await new Promise(r => setTimeout(r, 400))
}

;(async () => {
  console.log('收款管理金标准 E2E —— 后端 :' + PORT)
  await login()

  // ══════════════════════════════════════════════
  // 一、字典与收款码（配置化，杜绝假二维码）
  // ══════════════════════════════════════════════
  console.log('\n═══ 一、字典与收款码（配置化） ═══')
  const dict = dataOf(await api('GET', '/dms/payment/dict', null)) || {}
  check('字典返回支付方式/收款类型', !!dict.payChannels && !!dict.paymentTypes,
    `channels=${Object.keys(dict.payChannels || {}).length} types=${Object.keys(dict.paymentTypes || {}).length}`)
  check('收款类型=代收货款/配送费（与支付方式分离）',
    dict.paymentTypes && dict.paymentTypes['1'] === '代收货款' && dict.paymentTypes['2'] === '配送费',
    JSON.stringify(dict.paymentTypes))
  check('字典下发资金安全规则（现金限额 / 交款时限）',
    dict.cashLimitPerOrder !== undefined && dict.cashLimitDaily !== undefined && dict.handoverDeadlineHours !== undefined,
    `perOrder=${dict.cashLimitPerOrder} daily=${dict.cashLimitDaily} deadline=${dict.handoverDeadlineHours}`)

  const taskA = await seedTask('A')
  check('造数：带代收货款的配送任务', !!taskA, `taskId=${taskA}`)

  await setConfig('dms.payment.qrcode.base-url', '')
  const qr1 = dataOf(await api('POST', `/dms/payment/qrcode?taskId=${taskA}&amount=200`, null))
  check('★ 未开通扫码收款时不生成二维码（消除 pay.example.com 假 URL）',
    qr1 && (qr1.qrcodeUrl === null || qr1.qrcodeUrl === undefined || qr1.qrcodeUrl === '')
    && !String(qr1.qrcodeUrl || '').includes('example.com'),
    `qrcodeUrl=${qr1?.qrcodeUrl}`)

  await setConfig('dms.payment.qrcode.base-url', 'https://pay.e2e.local/collect')
  const qr2 = dataOf(await api('POST', `/dms/payment/qrcode?taskId=${taskA}&amount=200`, null))
  check('★ 配置收款码服务后生成真实二维码（配置化生效）',
    String(qr2?.qrcodeUrl || '').startsWith('https://pay.e2e.local/collect') && String(qr2?.qrcodeUrl || '').includes('amount=200'),
    JSON.stringify(qr2).slice(0, 180))
  await setConfig('dms.payment.qrcode.base-url', '')

  // ══════════════════════════════════════════════
  // 二、线下收款确认 + 支付回调（幂等）
  // ══════════════════════════════════════════════
  console.log('\n═══ 二、线下收款确认 ═══')
  const confirmRes = dataOf(await api('POST',
    `/dms/payment/confirm?taskId=${taskA}&payChannel=3&amount=200&externalOrderNo=${MARK}-POS`, null))
  check('线下确认收款成功（现金）', confirmRes?.status === 1, JSON.stringify(confirmRes || {}).slice(0, 140))
  check('支付方式落库（现金）', confirmRes?.payChannelName === '现金', confirmRes?.payChannelName)
  check('收款类型按任务代收货款推断为「代收货款」', confirmRes?.paymentType === 1, confirmRes?.paymentType)

  console.log('\n═══ 二·2 支付回调（幂等） ═══')
  const tradeNo = MARK + '-TRADE'
  const cb1 = dataOf(await api('POST',
    `/dms/payment/callback?taskId=${taskA}&tradeNo=${tradeNo}&payChannel=1&amount=200`, null))
  check('支付回调入账成功', cb1 && cb1.idempotent === false && cb1.status === 1, JSON.stringify(cb1 || {}).slice(0, 140))
  const cb2 = dataOf(await api('POST',
    `/dms/payment/callback?taskId=${taskA}&tradeNo=${tradeNo}&payChannel=1&amount=200`, null))
  check('★ 回调幂等（重复回调不重复置账，返回同一条记录）',
    cb2 && cb2.idempotent === true && String(cb2.paymentId) === String(cb1.paymentId),
    `idempotent=${cb2?.idempotent} paymentId=${cb2?.paymentId}`)
  // 科目完整性：收款码/回调创建的记录也必须落「收款类型」，否则代收货款与配送费无法分科目记账
  const typeRows = (dataOf(await api('GET', `/dms/payment/page?current=1&size=10&taskNo=PSD-${MARK}-A`, null)) || {}).records || []
  check('★ 收款码/回调创建的记录收款类型已落定（科目不丢）',
    typeRows.length >= 2 && typeRows.every(r => r.paymentType === 1 || r.paymentType === 2),
    JSON.stringify(typeRows.map(r => ({ id: r.id, type: r.paymentType }))).slice(0, 160))

  // ══════════════════════════════════════════════
  // 三、台账 / 统计（含关键词、超时筛选）
  // ══════════════════════════════════════════════
  console.log('\n═══ 三、台账与统计 ═══')
  const pageRes = dataOf(await api('GET', `/dms/payment/page?current=1&size=10&taskNo=PSD-${MARK}-A`, null))
  const row = (pageRes?.records || [])[0]
  check('台账分页可查（按任务编号）', (pageRes?.records || []).length >= 1, `total=${pageRes?.total}`)
  check('台账联查任务/客户/配送员快照',
    row?.taskNo === `PSD-${MARK}-A` && row?.customerName === 'E2E收款客户' && row?.riderName === 'E2E收款配送员990002',
    JSON.stringify({ taskNo: row?.taskNo, customer: row?.customerName, rider: row?.riderName }))
  check('台账含支付方式与交款状态列', 'payChannelName' in (row || {}) && 'handoverStatus' in (row || {}),
    `payChannelName=${row?.payChannelName} handoverStatus=${row?.handoverStatus}`)
  check('台账含催收/核销/超时派生列',
    'urgeCount' in (row || {}) && 'writeOffAmount' in (row || {}) && 'overdue' in (row || {}),
    `urgeCount=${row?.urgeCount} writeOff=${row?.writeOffAmount} overdue=${row?.overdue}`)

  const stat = dataOf(await api('GET', `/dms/payment/stat?startDate=2000-01-01&endDate=2099-12-31`, null)) || {}
  check('统计返回已收/代收货款/待上交字段',
    'paidAmount' in stat && 'codAmount' in stat && 'unhandoverAmount' in stat, JSON.stringify(stat).slice(0, 170))
  check('代收货款计入 codAmount（分科目记账）', num(stat.codAmount) >= 200, `codAmount=${stat.codAmount}`)
  check('统计含交款超时指标（笔数/金额）',
    'overdueCount' in stat && 'overdueAmount' in stat && 'deadlineHours' in stat,
    `overdue=${stat.overdueCount} deadline=${stat.deadlineHours}`)

  // ══════════════════════════════════════════════
  // 四、资金上交 / 稽核（含交款时限超时预警）
  // ══════════════════════════════════════════════
  console.log('\n═══ 四、资金上交与稽核 ═══')
  const payId = row?.id
  const h1 = dataOf(await api('POST', `/dms/payment/${payId}/handover?amount=120&operatorName=E2E交款员`, null))
  check('部分交款登记（120/200 → 部分交）', h1?.handoverStatus === 1 && num(h1?.handoverAmount) === 120,
    `status=${h1?.handoverStatus} amount=${h1?.handoverAmount}`)
  const h2 = dataOf(await api('POST', `/dms/payment/${payId}/handover?amount=80&operatorName=E2E交款员`, null))
  check('交清后状态置「已交」', h2?.handoverStatus === 2 && num(h2?.handoverAmount) === 200,
    `status=${h2?.handoverStatus} amount=${h2?.handoverAmount}`)
  const over = await api('POST', `/dms/payment/${payId}/handover?amount=0`, null)
  check('交款金额必须大于 0（校验）', over.json?.code !== 200, over.json?.message)

  const summary = dataOf(await api('GET',
    `/dms/payment/handover/summary?startDate=2000-01-01&endDate=2099-12-31&riderId=990002`, null)) || {}
  check('稽核汇总返回应上交/已上交/未上交',
    'dueTotal' in summary && 'handoverTotal' in summary && 'unhandoverTotal' in summary,
    JSON.stringify({ due: summary.dueTotal, done: summary.handoverTotal, left: summary.unhandoverTotal }))
  check('稽核汇总金额自洽（已交 200；未交 = 应上交 − 已上交）',
    num(summary.handoverTotal) >= 200
    && num(summary.unhandoverTotal) === num(summary.dueTotal) - num(summary.handoverTotal),
    `due=${summary.dueTotal} done=${summary.handoverTotal} left=${summary.unhandoverTotal}`)

  // 超时预警：造一条 30 小时前收款、未交清的记录
  await setConfig('dms.payment.handover.deadline.hours', '24')
  const taskE = await seedTask('E')
  const overdueRows = await dbQuery(
    `INSERT INTO dms_payment (tenant_id, task_id, rider_id, payment_type, pay_channel, pay_channel_name, amount,
                              status, handover_status, handover_amount, pay_time, deleted, create_time, update_time, version, finance_push_status)
     VALUES (1, $1, 990002, 1, 3, '现金', 60.00, 1, 0, 0, NOW() - INTERVAL '30 hour', 0, NOW() - INTERVAL '30 hour', NOW(), 0, 0)
     RETURNING id`, [taskE])
  const overduePayId = overdueRows[0].id
  const overduePage = dataOf(await api('GET',
    `/dms/payment/page?current=1&size=50&taskNo=PSD-${MARK}-E&overdueOnly=true`, null))
  const overdueRow = (overduePage?.records || [])[0]
  check('★ 交款超时预警：overdueOnly 查询命中超时记录',
    !!overdueRow && overdueRow.overdue === true && num(overdueRow.overdueHours) >= 5,
    `overdue=${overdueRow?.overdue} hours=${overdueRow?.overdueHours}`)
  const summary2 = dataOf(await api('GET',
    `/dms/payment/handover/summary?startDate=2000-01-01&endDate=2099-12-31&riderId=990002`, null)) || {}
  check('★ 稽核汇总含超时笔数与超时明细',
    num(summary2.overdueCount) >= 1
    && (summary2.overdueList || []).some(x => String(x.paymentId) === String(overduePayId)),
    `overdueCount=${summary2.overdueCount} list=${(summary2.overdueList || []).length}`)
  // 超时口径优先：即便同时传了矛盾的支付状态（3=未付），也必须按「已支付且未交清且超时」返回
  const overdueConflict = dataOf(await api('GET',
    `/dms/payment/page?current=1&size=50&taskNo=PSD-${MARK}-E&overdueOnly=true&status=3`, null))
  check('「仅看交款超时」口径优先于矛盾的支付状态（不返回空集）',
    (overdueConflict?.records || []).length >= 1, `total=${overdueConflict?.total}`)

  // ══════════════════════════════════════════════
  // 五、资金安全：现金单笔 / 单日限额
  // ══════════════════════════════════════════════
  console.log('\n═══ 五、资金安全：现金限额 ═══')
  const taskB = await seedTask('B', 991001, 300.0)
  await setConfig('dms.payment.cash.limit.per.order', '100')
  const overPer = await api('POST', `/dms/payment/confirm?taskId=${taskB}&payChannel=3&amount=150`, null)
  check('★ 超单笔现金限额被拒绝（引导改扫码/POS）',
    overPer.json?.code !== 200 && String(overPer.json?.message || '').includes('限额'),
    overPer.json?.message)
  const withinPer = dataOf(await api('POST', `/dms/payment/confirm?taskId=${taskB}&payChannel=3&amount=90`, null))
  check('限额内现金收款成功', withinPer?.status === 1, JSON.stringify(withinPer || {}).slice(0, 120))

  await setConfig('dms.payment.cash.limit.per.order', '0')
  await setConfig('dms.payment.cash.limit.daily', '100')
  const taskC = await seedTask('C', 991002, 300.0)
  const taskD = await seedTask('D', 991002, 300.0)
  const d1 = dataOf(await api('POST', `/dms/payment/confirm?taskId=${taskC}&payChannel=3&amount=60`, null))
  check('单日限额内第一笔成功（60/100）', d1?.status === 1, JSON.stringify(d1 || {}).slice(0, 120))
  const d2 = await api('POST', `/dms/payment/confirm?taskId=${taskD}&payChannel=3&amount=60`, null)
  check('★ 超单日现金限额被拒绝（同配送员累计 120 > 100）',
    d2.json?.code !== 200 && String(d2.json?.message || '').includes('限额'),
    d2.json?.message)
  await setConfig('dms.payment.cash.limit.per.order', '0')
  await setConfig('dms.payment.cash.limit.daily', '0')

  // ══════════════════════════════════════════════
  // 六、未付管理（挂账 → 催收 → 承诺付款 → 核销）
  // ══════════════════════════════════════════════
  console.log('\n═══ 六、未付管理（挂账闭环） ═══')
  const taskF = await seedTask('F', 992001, 100.0)
  await api('POST', `/dms/payment/mark-unpaid?taskId=${taskF}&remark=E2E客户拒付`, null)
  const unpaidPage = dataOf(await api('GET',
    `/dms/payment/unpaid/page?current=1&size=20&taskNo=PSD-${MARK}-F`, null))
  const unpaidRow = (unpaidPage?.records || [])[0]
  check('★ 挂账台账可查（status=3）',
    !!unpaidRow && unpaidRow.status === 3 && unpaidRow.unpaidRemark === 'E2E客户拒付',
    `status=${unpaidRow?.status} remark=${unpaidRow?.unpaidRemark}`)
  const unpaidStat = dataOf(await api('GET', `/dms/payment/unpaid/stat?startDate=2000-01-01&endDate=2099-12-31`, null)) || {}
  check('挂账汇总返回未付笔数/金额/催收/核销',
    'unpaidCount' in unpaidStat && 'unpaidAmount' in unpaidStat
    && 'urgedCount' in unpaidStat && 'writeOffAmount' in unpaidStat,
    JSON.stringify(unpaidStat).slice(0, 170))
  const dueAmt = num(unpaidRow?.amount)
  check('★ 挂账金额记清应收（代收货款 + 配送费）', dueAmt >= 100,
    `amount=${unpaidRow?.amount}`)
  check('未付汇总口径正确（未付金额 ≥ 本单应收）',
    num(unpaidStat.unpaidAmount) >= dueAmt, `unpaidAmount=${unpaidStat.unpaidAmount} due=${dueAmt}`)

  const urgeRes = dataOf(await api('POST',
    `/dms/payment/${unpaidRow.id}/urge?content=${encodeURIComponent('电话催收，客户答复本周内付')}&promiseDate=2026-10-01&operatorName=E2E催收员`, null))
  check('★ 催收登记成功（第 1 次）', urgeRes && num(urgeRes.urgeCount) >= 1, JSON.stringify(urgeRes || {}).slice(0, 140))
  const noContent = await api('POST', `/dms/payment/${unpaidRow.id}/urge?content=`, null)
  check('催收说明必填（校验）', noContent.json?.code !== 200, noContent.json?.message)

  const collections = dataOf(await api('GET', `/dms/payment/${unpaidRow.id}/collections`, null)) || []
  check('催收记录可追溯（含承诺付款日）',
    collections.some(c => c.actionType === 1 && String(c.promiseDate || '').startsWith('2026-10-01')),
    `actions=${collections.map(c => c.actionType).join(',')}`)

  const part = 40
  const wo1 = dataOf(await api('POST',
    `/dms/payment/${unpaidRow.id}/write-off?amount=${part}&payChannel=3&content=${encodeURIComponent('客户现金交付 ' + part)}`, null))
  check(`部分核销（${part}/${dueAmt} → 未结清，仍在挂账）`,
    wo1 && wo1.cleared === false && num(wo1.writeOffTotal) === part && wo1.status === 3,
    JSON.stringify(wo1 || {}).slice(0, 140))
  const woOver = await api('POST', `/dms/payment/${unpaidRow.id}/write-off?amount=${dueAmt - part + 1}`, null)
  check('★ 核销金额超出应收被拒绝（累计 > 应收）',
    woOver.json?.code !== 200 && String(woOver.json?.message || '').includes('超出应收'),
    woOver.json?.message)
  const rest = dueAmt - part
  const wo2 = dataOf(await api('POST', `/dms/payment/${unpaidRow.id}/write-off?amount=${rest}&payChannel=3`, null))
  check('★ 核销收清 → 自动置「已支付」（status 3→1）',
    wo2 && wo2.cleared === true && num(wo2.writeOffTotal) === dueAmt && wo2.status === 1,
    JSON.stringify(wo2 || {}).slice(0, 140))
  const woAgain = await api('POST', `/dms/payment/${unpaidRow.id}/write-off?amount=10`, null)
  check('非挂账记录不可再核销（校验）', woAgain.json?.code !== 200, woAgain.json?.message)
  const dbWriteOff = await dbQuery(
    `SELECT count(*)::int n FROM dms_payment_collection WHERE payment_id = $1 AND action_type = 2 AND deleted = 0`,
    [unpaidRow.id])
  check('DB 落库：核销流水 2 条（40 + 60）', dbWriteOff[0].n === 2, `rows=${dbWriteOff[0].n}`)

  // ══════════════════════════════════════════════
  // 七、财务打通（推送 ERP，幂等 + 批量逐单反馈）
  // ══════════════════════════════════════════════
  console.log('\n═══ 七、财务打通（推送） ═══')
  const pf1 = dataOf(await api('POST', `/dms/payment/push-finance?paymentId=${payId}`, null))
  check('★ 推送财务成功（生成事件发件箱 traceId）',
    pf1 && pf1.idempotent === false && !!pf1.traceId, JSON.stringify(pf1 || {}).slice(0, 170))
  const pf2 = dataOf(await api('POST', `/dms/payment/push-finance?paymentId=${payId}`, null))
  check('★ 推送财务幂等（重复推送返回既有 traceId，不重复出账）',
    pf2 && pf2.idempotent === true && pf2.traceId === pf1?.traceId,
    `idempotent=${pf2?.idempotent} sameTrace=${pf2?.traceId === pf1?.traceId}`)
  const outbox = await dbQuery(
    `SELECT count(*)::int n FROM dms_event_outbox WHERE event_type = 'PAYMENT_PUSH_FINANCE' AND payload LIKE $1`,
    [`%paymentId=${payId},%`])
  check('DB 落库：财务推送事件仅 1 条（幂等）', outbox[0].n === 1, `rows=${outbox[0].n}`)

  const taskG = await seedTask('G', 992002, 50.0)
  const qrG = dataOf(await api('POST', `/dms/payment/qrcode?taskId=${taskG}&amount=50`, null))
  const unpaidId = qrG?.id
  const pfBatch = dataOf(await api('POST',
    `/dms/payment/push-finance?paymentIds=${unpaidId},${payId}`, null))
  check('★ 批量推送逐单反馈（1 成功 + 1 幂等命中 + 1 失败）',
    pfBatch && Number(pfBatch.total) === 2 && Number(pfBatch.success) === 1
    && Number(pfBatch.idempotentCount) === 1 && Number(pfBatch.failedCount) === 1,
    JSON.stringify(pfBatch || {}).slice(0, 190))
  check('批量推送失败原因可读（待支付不可推）',
    (pfBatch?.failed || []).some(f => String(f.reason || '').includes('已支付')),
    JSON.stringify(pfBatch?.failed || []).slice(0, 140))

  // ══════════════════════════════════════════════
  // 八、支付流水导入 / 对账
  // ══════════════════════════════════════════════
  console.log('\n═══ 八、支付流水与对账 ═══')
  const taskH = await seedTask('H', 993001, 150.0)
  const cbH = dataOf(await api('POST',
    `/dms/payment/callback?taskId=${taskH}&tradeNo=${MARK}-TRADE-H&payChannel=1&amount=150&externalOrderNo=${MARK}-EXT-H`, null))
  check('造数：线上支付记录（微信，含交易号/商户单号）', cbH?.status === 1, JSON.stringify(cbH || {}).slice(0, 140))
  const payIdH = cbH?.paymentId
  const taskI = await seedTask('I', 993002, 77.0)
  const cbI = dataOf(await api('POST',
    `/dms/payment/callback?taskId=${taskI}&tradeNo=${MARK}-TRADE-I&payChannel=1&amount=77`, null))
  const payIdI = cbI?.paymentId
  // 第二笔线上支付记录：专供「金额不符」对账（避免与「重复流水」判定撞车）
  const taskH2 = await seedTask('H2', 993003, 300.0)
  const cbH2 = dataOf(await api('POST',
    `/dms/payment/callback?taskId=${taskH2}&tradeNo=${MARK}-TRADE-H3&payChannel=1&amount=300&externalOrderNo=${MARK}-EXT-H3`, null))
  check('造数：第二笔线上支付记录（金额不符对账用）', cbH2?.status === 1, JSON.stringify(cbH2 || {}).slice(0, 120))

  const nowStr = new Date(Date.now() + 8 * 3600 * 1000).toISOString().slice(0, 19).replace('T', ' ')
  const importRows = [
    { channelCode: 'WECHAT', tradeNo: `${MARK}-TRADE-H`, outTradeNo: `${MARK}-EXT-H`, amount: 150, tradeTime: nowStr, payer: '张三' },
    { channelCode: 'WECHAT', tradeNo: `${MARK}-TRADE-ORPHAN`, outTradeNo: `${MARK}-NOBODY`, amount: 88, tradeTime: nowStr, payer: '李四' },
    { channelCode: 'WECHAT', tradeNo: `${MARK}-TRADE-H2`, outTradeNo: `${MARK}-EXT-H3`, amount: 999, tradeTime: nowStr, payer: '王五' },
    { channelCode: 'WECHAT', tradeNo: `${MARK}-TRADE-DUP`, outTradeNo: `${MARK}-EXT-H`, amount: 150, tradeTime: nowStr, payer: '赵六' },
  ]
  const imp1 = dataOf(await api('POST', '/dms/payment/flow/import?defaultChannel=WECHAT', importRows))
  check('★ 流水导入成功（4 行入库）',
    imp1 && Number(imp1.inserted) === 4 && Number(imp1.failedCount) === 0,
    JSON.stringify(imp1 || {}).slice(0, 170))
  const imp2 = dataOf(await api('POST', '/dms/payment/flow/import?defaultChannel=WECHAT',
    [{ channelCode: 'WECHAT', tradeNo: `${MARK}-TRADE-H`, amount: 150 }]))
  check('★ 重复导入幂等跳过（同渠道同交易号）',
    imp2 && Number(imp2.skipped) === 1 && Number(imp2.inserted) === 0,
    JSON.stringify(imp2 || {}).slice(0, 140))
  const impBad = dataOf(await api('POST', '/dms/payment/flow/import?defaultChannel=WECHAT',
    [{ tradeNo: '', amount: 1 }, { tradeNo: `${MARK}-TRADE-BADAMT`, amount: 'abc' }]))
  check('导入逐行校验（缺交易号 / 金额非法均返回原因）',
    impBad && Number(impBad.failedCount) === 2 && (impBad.failed || []).every(f => !!f.reason),
    JSON.stringify(impBad || {}).slice(0, 170))

  const flowPage = dataOf(await api('GET',
    `/dms/payment/flow/page?current=1&size=50&channelCode=WECHAT&tradeNo=${MARK}`, null))
  check('流水分页可查（按渠道+交易号）', (flowPage?.records || []).length >= 3, `total=${flowPage?.total}`)
  const flowStat = dataOf(await api('GET', `/dms/payment/flow/stat?channelCode=WECHAT`, null)) || {}
  check('流水统计返回已匹配/未匹配/差异',
    'matched' in flowStat && 'unmatched' in flowStat && 'diff' in flowStat,
    JSON.stringify(flowStat).slice(0, 170))

  const today = new Date(Date.now() + 8 * 3600 * 1000).toISOString().slice(0, 10)
  const rec = dataOf(await api('POST',
    `/dms/payment/reconcile?startDate=${today}&endDate=${today}&channelCode=WECHAT`, null))
  const details = rec?.details || []
  check('★ 对账执行成功（返回三类差异计数）',
    rec && 'matched' in rec && 'flowOnly' in rec && 'amountMismatch' in rec && 'systemOnly' in rec,
    JSON.stringify({ m: rec?.matched, fo: rec?.flowOnly, am: rec?.amountMismatch, so: rec?.systemOnly }).slice(0, 170))
  check('★ 对账：交易号一致 + 金额一致 → 匹配成功',
    details.some(d => d.type === 'MATCHED' && d.tradeNo === `${MARK}-TRADE-H`
      && String(d.paymentId) === String(payIdH)),
    JSON.stringify(details.find(d => d.tradeNo === `${MARK}-TRADE-H`) || {}).slice(0, 170))
  check('★ 对账：系统无此笔（流水有、系统无记录）',
    details.some(d => d.type === 'FLOW_ONLY' && d.tradeNo === `${MARK}-TRADE-ORPHAN`),
    JSON.stringify(details.find(d => d.tradeNo === `${MARK}-TRADE-ORPHAN`) || {}).slice(0, 170))
  check('★ 对账：金额不符（商户单号命中但金额不一致）',
    details.some(d => d.type === 'AMOUNT_MISMATCH' && d.tradeNo === `${MARK}-TRADE-H2`
      && String(d.paymentId) === String(cbH2?.paymentId)),
    JSON.stringify(details.find(d => d.tradeNo === `${MARK}-TRADE-H2`) || {}).slice(0, 170))
  check('★ 对账：掉单（系统已支付但平台流水缺失）',
    details.some(d => d.type === 'SYSTEM_ONLY' && String(d.paymentId) === String(payIdI)),
    JSON.stringify(details.find(d => d.type === 'SYSTEM_ONLY' && String(d.paymentId) === String(payIdI)) || {}).slice(0, 170))
  check('★ 对账：重复流水（同一收款记录被两笔流水命中）',
    details.some(d => d.type === 'DUPLICATE' && d.tradeNo === `${MARK}-TRADE-DUP`),
    JSON.stringify(details.find(d => d.tradeNo === `${MARK}-TRADE-DUP`) || {}).slice(0, 170))
  const dbFlow = await dbQuery(
    `SELECT match_status FROM dms_payment_flow WHERE trade_no = $1 AND deleted = 0`, [`${MARK}-TRADE-H`])
  check('DB 落库：匹配结果写回流水（match_status=1）', dbFlow[0]?.match_status === 1, JSON.stringify(dbFlow[0]))

  // 人工匹配 / 忽略
  await api('POST', '/dms/payment/flow/import?defaultChannel=WECHAT',
    [{ channelCode: 'WECHAT', tradeNo: `${MARK}-TRADE-M`, outTradeNo: `${MARK}-EXT-I`, amount: 77, tradeTime: nowStr }])
  const flowM = ((dataOf(await api('GET',
    `/dms/payment/flow/page?current=1&size=10&channelCode=WECHAT&tradeNo=${MARK}-TRADE-M`, null)) || {}).records || [])[0]
  const matchRes = dataOf(await api('POST', `/dms/payment/flow/${flowM?.id}/match?paymentId=${payIdI}`, null))
  check('★ 流水人工匹配到收款记录（matchType=2）',
    matchRes && matchRes.matchStatus === 1 && matchRes.matchType === 2
    && String(matchRes.paymentId) === String(payIdI),
    JSON.stringify(matchRes || {}).slice(0, 170))
  await api('POST', '/dms/payment/flow/import?defaultChannel=WECHAT',
    [{ channelCode: 'WECHAT', tradeNo: `${MARK}-TRADE-X`, amount: 1, tradeTime: nowStr }])
  const flowX = ((dataOf(await api('GET',
    `/dms/payment/flow/page?current=1&size=10&channelCode=WECHAT&tradeNo=${MARK}-TRADE-X`, null)) || {}).records || [])[0]
  const ignoreRes = dataOf(await api('POST', `/dms/payment/flow/${flowX?.id}/ignore?remark=渠道测试单`, null))
  check('★ 流水差异可人工忽略（matchStatus=3）',
    ignoreRes && ignoreRes.matchStatus === 3, JSON.stringify(ignoreRes || {}).slice(0, 140))
  // 忽略是人工判定，再次对账不得改判回差异
  await api('POST', `/dms/payment/reconcile?startDate=${today}&endDate=${today}&channelCode=WECHAT`, null)
  const flowX2 = ((dataOf(await api('GET',
    `/dms/payment/flow/page?current=1&size=10&channelCode=WECHAT&tradeNo=${MARK}-TRADE-X`, null)) || {}).records || [])[0]
  check('★ 已人工忽略的流水在再次对账后仍保持忽略（不改判）',
    flowX2?.matchStatus === 3, `matchStatus=${flowX2?.matchStatus}`)
  // 渠道编码大小写归一：'wechat' 与 'WECHAT' 是同一渠道，重复导入必须被幂等拦截
  const impLower = dataOf(await api('POST', '/dms/payment/flow/import',
    [{ channelCode: 'wechat', tradeNo: `${MARK}-TRADE-H`, amount: 150 }]))
  check('★ 渠道编码大小写归一（wechat 与 WECHAT 视为同渠道，重复导入被拦）',
    impLower && Number(impLower.skipped) === 1 && Number(impLower.inserted) === 0,
    JSON.stringify(impLower || {}).slice(0, 140))

  // ══════════════════════════════════════════════
  // 九、批量线下确认 + 导出（审计）
  // ══════════════════════════════════════════════
  console.log('\n═══ 九、批量确认与导出 ═══')
  const taskJ = await seedTask('J', 994001, 80.0)
  const batch = dataOf(await api('POST', '/dms/payment/confirm-batch',
    [{ taskId: taskJ, payChannel: 3, amount: 80 }, { taskId: 999999999, payChannel: 3, amount: 1 }]))
  check('★ 批量线下确认逐单反馈（1 成功 + 1 失败）',
    batch && Number(batch.success) === 1 && Number(batch.failedCount) === 1,
    JSON.stringify(batch || {}).slice(0, 170))
  const exportRes = dataOf(await api('GET', `/dms/payment/export?taskNo=PSD-${MARK}-A`, null))
  check('★ 导出接口返回台账数据（后端留审计）',
    Array.isArray(exportRes) && exportRes.length >= 1
    && String(exportRes[0]?.taskNo || '').startsWith(`PSD-${MARK}-A`),
    `rows=${Array.isArray(exportRes) ? exportRes.length : 'n/a'}`)

  // ══════════════════════════════════════════════
  // 十、DB 一致性核对
  // ══════════════════════════════════════════════
  console.log('\n═══ 十、DB 一致性 ═══')
  const dbTrade = await dbQuery(`SELECT count(*)::int n FROM dms_payment WHERE trade_no = $1 AND status = 1`, [tradeNo])
  check('DB 落库：回调交易号已入账（trade_no + status=1）', dbTrade[0].n === 1, `rows=${dbTrade[0].n}`)
  const dbRow = await dbQuery(`SELECT status, handover_status, handover_amount FROM dms_payment WHERE id = $1`, [payId])
  check('DB 落库：交款状态一致（已交 200）',
    dbRow[0]?.status === 1 && dbRow[0]?.handover_status === 2 && Number(dbRow[0]?.handover_amount) === 200,
    JSON.stringify(dbRow[0]))
  const dbConfig = await dbQuery(
    `SELECT config_key FROM dms_config WHERE config_key IN
     ('dms.payment.cash.limit.per.order','dms.payment.cash.limit.daily','dms.payment.handover.deadline.hours')
     AND tenant_id = 0 AND deleted = 0`)
  check('DB 落库：资金安全 3 配置键已预置', dbConfig.length === 3, dbConfig.map(c => c.config_key).join(','))

  // ══════════════════════════════════════════════
  // 十一、UI 验收（四 Tab）
  // ══════════════════════════════════════════════
  console.log('\n═══ 十一、UI 验收 ═══')
  await login() // UI 前刷新 token（并行会话共用同一账号会被 sa-token 互踢，表现为跳登录页）
  // 留一条挂账数据，供「未付管理」行级操作弹窗验收（其余挂账已在前序用例收清）
  const taskL = await seedTask('L', 995001, 60.0)
  await api('POST', `/dms/payment/mark-unpaid?taskId=${taskL}&remark=${encodeURIComponent('E2E挂账（UI 验收）')}`, null)
  const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
  const FE = process.env.FE_URL || 'http://localhost:5656'
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  const errs = []
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('console', m => { if (m.type() === 'error') errs.push('console: ' + m.text().slice(0, 120)) })
  const badApi = []
  page.on('response', r => {
    const u = r.url()
    if (u.includes('/api/dms/payment') && r.status() >= 400) badApi.push(`${r.status()} ${u.slice(u.indexOf('/api'))}`)
  })
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', '1') }, [TOKEN])
  await page.goto(`${FE}/dms/payment`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(8000)

  const body = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开（非登录页 / 无 404 白屏）',
    page.url().includes('/dms/payment') && !body.includes('页面不存在') && body.length > 50, page.url())
  for (const t of ['收款台账', '未付管理', '交款稽核', '支付流水']) check(`Tab「${t}」存在`, body.includes(t))
  for (const b of ['刷新']) check(`工具栏含「${b}」`, body.includes(b))
  check('收款码服务状态提示（配置化）', body.includes('收款码服务'), body.slice(0, 80).replace(/\s/g, '').slice(0, 60))
  check('提示代收货款与配送费分科目记账', body.includes('代收货款') && body.includes('配送费'))
  check('工具栏展示交款时限/现金限额（资金安全口径）',
    body.includes('交款时限') || body.includes('现金限额'), body.slice(0, 120).replace(/\s/g, ''))
  const headers = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').replace(/\s+/g, '')).filter(Boolean))
  for (const h of ['操作', '任务编号', '客户', '配送员', '收款类型', '应收金额', '支付方式', '支付状态', '交款状态', '已上交', '交款超时']) {
    check(`台账表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  check('列表渲染真实收款数据', body.includes(`PSD-${MARK}`), `PSD-${MARK}`)
  const shot = 'I:/AI-Ready/tool-results/dms-payment/'
  await page.screenshot({ path: shot + 'ui-ledger.png', fullPage: true })

  // 页面配置弹窗（查询条件 / 功能按钮）
  await page.locator('.toolbar-right button:has(.anticon-setting)').first().click()
  await page.waitForTimeout(1500)
  const cfgPanel = page.locator('.ant-modal-wrap:visible, .ant-drawer-content:visible').last()
  const cfgTxt = (await cfgPanel.innerText().catch(() => '')).replace(/\s+/g, '')
  check('页面配置弹窗可打开（查询条件 / 功能按钮）',
    cfgTxt.includes('查询条件') && cfgTxt.includes('功能按钮'), cfgTxt.slice(0, 100))
  await page.keyboard.press('Escape')
  await page.waitForTimeout(800)

  // 未付管理 Tab（先按任务编号过滤，避免被其它会话的挂账挤到第二页）
  await page.click('.tab-item:has-text("未付管理")')
  await page.waitForTimeout(2500)
  const unpaidBody0 = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('未付管理 Tab 渲染（挂账汇总指标）',
    unpaidBody0.includes('未付金额') && unpaidBody0.includes('已核销'), unpaidBody0.slice(0, 100))
  await page.fill('input[placeholder="任务编号"]', `PSD-${MARK}-L`)
  await page.locator('.search-area .ant-btn-primary').first().click()
  await page.waitForTimeout(2000)
  const unpaidBody = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('未付台账按任务编号可查（挂账行渲染）', unpaidBody.includes(`PSD-${MARK}-L`), `PSD-${MARK}-L`)
  // 行级「催收」弹窗
  const urgeBtn = page.locator('.ss-grid tbody tr', { hasText: `PSD-${MARK}-L` })
    .locator('button:has-text("催")').first()
  if (await urgeBtn.count()) {
    await urgeBtn.click()
    await page.waitForTimeout(1500)
    const urgeTxt = (await page.locator('.ant-modal-wrap:visible').last().innerText().catch(() => '')).replace(/\s+/g, '')
    check('未付管理行级「催收」弹窗可打开（含承诺付款日）',
      urgeTxt.includes('催收登记') && urgeTxt.includes('催收说明'), urgeTxt.slice(0, 100))
    await page.keyboard.press('Escape')
    await page.waitForTimeout(800)
  } else {
    check('未付管理行级「催收」弹窗可打开（含承诺付款日）', false, '未找到行级催收按钮')
  }
  await page.screenshot({ path: shot + 'ui-unpaid.png', fullPage: true })

  // 交款稽核 Tab
  await page.click('.tab-item:has-text("交款稽核")')
  await page.waitForTimeout(2500)
  const handBody = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('交款稽核 Tab 渲染（应上交/已上交/未上交 + 超时限笔数）',
    handBody.includes('应上交合计') && handBody.includes('已上交合计') && handBody.includes('超时限未交笔数'),
    handBody.slice(0, 120))
  await page.screenshot({ path: shot + 'ui-handover.png', fullPage: true })

  // 支付流水 Tab
  await page.click('.tab-item:has-text("支付流水")')
  await page.waitForTimeout(2500)
  const flowBody = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('支付流水 Tab 渲染（统计 + 导入/对账按钮）',
    flowBody.includes('流水笔数') && flowBody.includes('导入流水') && flowBody.includes('对账'),
    flowBody.slice(0, 120))
  check('流水列表渲染真实渠道流水', flowBody.includes(MARK), MARK)
  // 「对账」弹窗（选日期范围 → 开始对账 → 五类差异指标）
  await page.locator('button:has-text("对账")').first().click()
  await page.waitForTimeout(1500)
  const recModal = page.locator('.ant-modal-wrap:visible').last()
  const recTxt = (await recModal.innerText().catch(() => '')).replace(/\s+/g, '')
  check('支付流水 Tab「对账」弹窗可打开（日期范围 + 渠道 + 开始对账）',
    recTxt.includes('开始对账') && recTxt.includes('渠道'), recTxt.slice(0, 100))
  if (recTxt.includes('开始对账')) {
    await recModal.locator('button.ant-btn-primary').first().click()
    await page.waitForTimeout(3000)
    const recTxt2 = (await page.locator('.ant-modal-wrap:visible').last().innerText().catch(() => '')).replace(/\s+/g, '')
    check('★ 对账弹窗内执行并回显五类差异结果',
      ['已匹配', '系统无此笔', '金额不符', '掉单', '差异合计'].every(t => recTxt2.includes(t)), recTxt2.slice(0, 110))
    await page.screenshot({ path: shot + 'ui-reconcile.png', fullPage: true })
  } else {
    check('★ 对账弹窗内执行并回显五类差异结果', false, '未找到开始对账按钮')
  }
  await page.keyboard.press('Escape')
  await page.waitForTimeout(600)
  await page.screenshot({ path: shot + 'ui-flow.png', fullPage: true })

  check('运行期无接口错误（无 4xx/5xx）', badApi.length === 0, badApi.slice(0, 3).join(' || '))
  check('运行期无 JS 错误（无 pageerror/console.error）', errs.length === 0, errs.slice(0, 3).join(' || '))
  await browser.close()

  // ══════════════════════════════════════════════
  // 十二、清理
  // ══════════════════════════════════════════════
  console.log('\n═══ 十二、清理 ═══')
  await dbQuery(`DELETE FROM dms_payment_collection WHERE payment_id IN
                 (SELECT id FROM dms_payment WHERE task_id IN (SELECT id FROM dms_task WHERE task_no LIKE $1))`,
    [`PSD-${MARK}%`])
  await dbQuery(`DELETE FROM dms_payment WHERE task_id IN (SELECT id FROM dms_task WHERE task_no LIKE $1)`, [`PSD-${MARK}%`])
  await dbQuery(`DELETE FROM dms_payment_flow WHERE trade_no LIKE $1`, [`${MARK}%`])
  await dbQuery(`DELETE FROM dms_event_outbox WHERE event_type = 'PAYMENT_PUSH_FINANCE' AND payload LIKE $1`, [`%${MARK}%`])
  await dbQuery(`DELETE FROM dms_payment WHERE task_id = $1`, [taskE])
  await dbQuery(`DELETE FROM dms_task WHERE task_no LIKE $1`, [`PSD-${MARK}%`])
  await setConfig('dms.payment.cash.limit.per.order', '0')
  await setConfig('dms.payment.cash.limit.daily', '0')
  await setConfig('dms.payment.handover.deadline.hours', '24')
  await setConfig('dms.payment.qrcode.base-url', '')
  const left = await dbQuery(`SELECT count(*)::int n FROM dms_task WHERE task_no LIKE $1`, [`PSD-${MARK}%`])
  const leftFlow = await dbQuery(`SELECT count(*)::int n FROM dms_payment_flow WHERE trade_no LIKE $1`, [`${MARK}%`])
  check('验收数据已清理', left[0].n === 0 && leftFlow[0].n === 0, `task=${left[0].n} flow=${leftFlow[0].n}`)

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 汇总：${pass}/${results.length} 通过${fail ? `，${fail} 项失败` : ''} ═══`)
  if (fail) results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  process.exitCode = fail ? 1 : 0
})().catch(e => { console.error('E2E 异常终止:', e.stack || e.message); process.exitCode = 1 })
