/*
 * 配送结算（配送 → 结算收款 → 配送结算，菜单 80910）金标准端到端验证
 *
 * 口径依据：《配送结算开发文档》—— 消除「费率硬编码 / 无结算单实体 / 只写 outbox 不记账 / 无对账」四项缺陷：
 *   · 计费规则**配置化**（`dms_config` 的 `dms.settlement.*`）+ **计费规则表**（按 结算对象×渠道/线路×生效期×优先级）
 *   · 结算单闭环：生成（草稿）→ 确认（锁定金额）→ 推送 ERP（**真实生成凭证/应付**，幂等）
 *   · 结算对象：配送员（自有）/ 渠道（外部运力）
 *   · 部分签收按实际签收数量折算（起步价全收），拒收不计费
 *   · 对账：结算单 vs 财务（凭证金额 / 应付核销 / 配送费收款），输出差异清单
 *
 * 验收：接口 + DB 落库核对（含**费率配置化反证**与**规则命中反证**，证明非硬编码且规则真实生效）
 * 用法：node tools/e2e-dms-settlement.cjs          （默认后端 5680）
 */
const http = require('http')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const PORT = Number(process.env.ERP_PORT || 5680)
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
const E2E_USER = process.env.E2E_USER || 'e2e_dispatch'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const MARK = 'E2ESET' + Date.now().toString().slice(-6)

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

function doRawReq(method, reqPath, body, token) {
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

/**
 * 带 401 自愈的请求：共享 dev 环境用同一验收账号时，sa-token 单端登录会被并行会话互踢，
 * 表现为「请先登录」——此处自动重登并重试一次（登录请求自身 token 为空，不会递归）。
 */
async function rawReq(method, reqPath, body, token) {
  const res = await doRawReq(method, reqPath, body, token)
  if (token && res.json && res.json.code === 401) {
    console.log('  ⟳ 令牌失效（并行会话互踢），自动重登重试')
    try {
      await login()
      return await doRawReq(method, reqPath, body, TOKEN)
    } catch (e) {
      return res
    }
  }
  return res
}

async function dbQuery(sql, params) {
  const client = new Client(DSN)
  await client.connect()
  try { return (await client.query(sql, params)).rows } finally { await client.end() }
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 200) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 160) : ''}`)
}
const num = (v) => Number(v) || 0

let TOKEN = null
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

const TODAY = new Date().toISOString().slice(0, 10)
const created = { ruleId: null, signIds: [], settlementIds: [], voucherNos: [], channelId: null, channelName: null }

/** 造 3 个「已完成」任务（可结算）：A 普通 / B 加急 / C 渠道任务 */
async function seedTasks(channelId) {
  await dbQuery(`DELETE FROM dms_task WHERE task_no LIKE $1`, ['PSD-' + MARK + '%'])
  const rows = await dbQuery(
    `INSERT INTO dms_task (tenant_id, task_no, order_no, order_type, rider_id, rider_name, channel_id, status,
                           estimated_distance, priority, completed_time, update_time, deleted, create_time, version, print_count)
     VALUES
      (1, $1, $2, 1, 990001, $3, NULL, 6, 12.0, 1, NOW(), NOW(), 0, NOW(), 0, 0),
      (1, $4, $5, 1, 990001, $3, NULL, 6, 3.0, 2, NOW(), NOW(), 0, NOW(), 0, 0),
      (1, $6, $7, 1, 990002, $8, $9, 6, 8.0, 1, NOW(), NOW(), 0, NOW(), 0, 0)
     RETURNING id`,
    [`PSD-${MARK}-A`, MARK + '-A', 'E2E结算司机', `PSD-${MARK}-B`, MARK + '-B',
     `PSD-${MARK}-C`, MARK + '-C', 'E2E渠道骑手', channelId])
  return rows.map(r => r.id)
}

/** 造专用渠道（保证渠道结算只聚合本次任务，避免被库里既有数据污染），用后清理；同一轮只造一次 */
async function seedChannel() {
  if (created.channelId) {
    return { id: created.channelId, channel_name: created.channelName }
  }
  const ins = await dbQuery(
    `INSERT INTO dms_channel (tenant_id, channel_code, channel_name, channel_type, status, deleted, create_time, update_time, version, sort_order)
     VALUES (1, $1, $2, 3, 1, 0, NOW(), NOW(), 0, 0) RETURNING id, channel_name`,
    ['E2ECH' + MARK.slice(-6), 'E2E渠道' + MARK])
  created.channelId = ins[0].id
  created.channelName = ins[0].channel_name
  return ins[0]
}

/**
 * 回退凭证对分类账（finance_ledger）的累加
 *
 * 凭证过账会累加 finance_ledger 本期发生额并重算期末余额；删除凭证**不会**自动回退，
 * 故 E2E 收尾必须按凭证明细精确回退，避免污染总账/科目余额表。
 */
async function rollbackLedger(voucherNo) {
  if (!voucherNo) return
  const vs = await dbQuery(`SELECT id, fiscal_year, fiscal_period, tenant_id FROM finance_voucher WHERE voucher_no = $1`, [voucherNo])
  if (!vs.length) return
  const v = vs[0]
  const items = await dbQuery(
    `SELECT subject_code, debit_amount, credit_amount FROM finance_voucher_item WHERE voucher_id = $1 AND deleted_flag = 0`, [v.id])
  for (const it of items) {
    await dbQuery(`UPDATE finance_ledger SET
        period_debit = period_debit - $1,
        period_credit = period_credit - $2,
        closing_balance = (opening_debit - opening_credit) + (period_debit - $1) - (period_credit - $2),
        closing_debit = GREATEST((opening_debit - opening_credit) + (period_debit - $1) - (period_credit - $2), 0),
        closing_credit = GREATEST(-((opening_debit - opening_credit) + (period_debit - $1) - (period_credit - $2)), 0),
        balance_direction = CASE WHEN (opening_debit - opening_credit) + (period_debit - $1) - (period_credit - $2) >= 0 THEN 1 ELSE 2 END
      WHERE tenant_id = $3 AND subject_code = $4 AND fiscal_year = $5 AND fiscal_period = $6 AND deleted_flag = 0`,
      [num(it.debit_amount), num(it.credit_amount), v.tenant_id, it.subject_code, v.fiscal_year, v.fiscal_period])
  }
}

/** 清理本次 E2E 产生的全部数据（业务 + 财务），异常路径也会被调用 */
async function cleanup() {
  for (const vno of created.voucherNos.filter(Boolean)) {
    await rollbackLedger(vno)
  }
  if (created.settlementIds.length) {
    await dbQuery(`DELETE FROM finance_voucher_item WHERE source_type = 'DMS_SETTLEMENT' AND source_id = ANY($1)`, [created.settlementIds])
    await dbQuery(`DELETE FROM finance_payable WHERE source_type = 'DMS_SETTLEMENT' AND source_id = ANY($1)`, [created.settlementIds])
    await dbQuery(`DELETE FROM dms_settlement_item WHERE settlement_id = ANY($1)`, [created.settlementIds])
    await dbQuery(`DELETE FROM dms_settlement WHERE id = ANY($1)`, [created.settlementIds])
  }
  const vnos = created.voucherNos.filter(Boolean)
  if (vnos.length) {
    await dbQuery(`DELETE FROM finance_voucher WHERE voucher_no = ANY($1)`, [vnos])
  }
  if (created.signIds.length) {
    await dbQuery(`DELETE FROM dms_sign WHERE id = ANY($1)`, [created.signIds])
  }
  if (created.ruleId) {
    await dbQuery(`DELETE FROM dms_settlement_rule WHERE id = $1`, [created.ruleId])
  }
  if (created.channelId) {
    await dbQuery(`DELETE FROM dms_channel WHERE id = $1`, [created.channelId])
  }
  await dbQuery(`DELETE FROM dms_task WHERE task_no LIKE $1`, ['PSD-' + MARK + '%'])
  const left = await dbQuery(`SELECT count(*)::int n FROM dms_settlement WHERE remark = $1`, [MARK])
  const leftRule = await dbQuery(`SELECT count(*)::int n FROM dms_settlement_rule WHERE remark = $1`, [MARK])
  check('验收数据已清理（结算单 + 规则）', left[0].n === 0 && leftRule[0].n === 0, `settlement=${left[0].n} rule=${leftRule[0].n}`)
}

/** 造签收记录（部分签收 / 拒收 计费依据） */
async function seedSign(taskId, signType, planned, actual, auditStatus) {
  const rows = await dbQuery(
    `INSERT INTO dms_sign (tenant_id, task_id, sign_type, planned_quantity, actual_quantity, audit_status, sign_time, deleted, create_time, update_time, version)
     VALUES (1, $1, $2, $3, $4, $5, NOW(), 0, NOW(), NOW(), 0) RETURNING id`,
    [taskId, signType, planned, actual, auditStatus])
  created.signIds.push(rows[0].id)
  return rows[0].id
}

;(async () => {
  console.log('配送结算金标准 E2E —— 后端 :' + PORT)
  await login()

  // ══ 一、全局缺省费率（配置化） ══
  console.log('\n═══ 一、全局缺省费率（配置化） ═══')
  const rule = (await rawReq('GET', '/dms/settlement/rule', null, TOKEN)).json?.data
  const needKeys = ['baseFee', 'freeDistanceKm', 'perKmRate', 'timeSurchargeRate', 'urgentSurcharge']
  check('缺省费率接口返回 5 项', needKeys.every(k => k in (rule || {})), JSON.stringify(rule))
  const cfg = await dbQuery(`SELECT config_key, config_value FROM dms_config WHERE config_key LIKE 'dms.settlement.%' AND deleted = 0 AND tenant_id = 0`)
  const cfgMap = Object.fromEntries(cfg.map(r => [r.config_key, num(r.config_value)]))
  check('接口费率与 dms_config 一致（非硬编码）',
    num(rule.baseFee) === cfgMap['dms.settlement.base.fee'] && num(rule.perKmRate) === cfgMap['dms.settlement.per.km.rate'],
    `api=${rule.baseFee}/${rule.perKmRate} db=${cfgMap['dms.settlement.base.fee']}/${cfgMap['dms.settlement.per.km.rate']}`)
  check('记账科目配置已落位（借方/贷方/自有）',
    !!cfgMap['dms.settlement.account.debit.subject'] && !!cfgMap['dms.settlement.account.credit.subject'] && !!cfgMap['dms.settlement.account.staff.subject'],
    `${cfgMap['dms.settlement.account.debit.subject']}/${cfgMap['dms.settlement.account.credit.subject']}/${cfgMap['dms.settlement.account.staff.subject']}`)

  // ══ 二、计费规则表（CRUD + 命中反证） ══
  console.log('\n═══ 二、计费规则表（按对象/渠道/生效期） ═══')
  const rulePayload = {
    ruleName: 'E2E按单规则-' + MARK, targetType: 1, billingType: 1,
    baseFee: 20, freeDistanceKm: 0, perKmRate: 0, perKgRate: 0,
    timeSurchargeRate: 0, urgentSurcharge: 0, settleCycle: 1,
    effectiveStart: TODAY, effectiveEnd: TODAY, priority: 1, status: 1, remark: MARK
  }
  const ruleCreate = await rawReq('POST', '/dms/settlement/rule', rulePayload, TOKEN)
  const newRule = ruleCreate.json?.data
  created.ruleId = newRule?.id
  check('新增计费规则成功', !!newRule?.id, JSON.stringify(ruleCreate.json).slice(0, 140))
  check('规则编码自动生成（JSR+序号）', /^JSR\d{3,}$/.test(newRule?.ruleCode || ''), newRule?.ruleCode)

  const rulePage = (await rawReq('GET', `/dms/settlement/rule/page?current=1&size=10&ruleName=${encodeURIComponent('E2E按单规则-' + MARK)}`, null, TOKEN)).json?.data
  check('规则分页可查', (rulePage?.records || []).length === 1, `total=${rulePage?.total}`)

  const dupCode = await rawReq('POST', '/dms/settlement/rule', { ...rulePayload, ruleCode: newRule.ruleCode }, TOKEN)
  check('规则编码重复被拒绝', dupCode.json?.code !== 200, dupCode.json?.message)

  const [taskA, taskB, taskC] = await seedTasks((await seedChannel()).id)
  check('已签收/已完成任务造数成功', !!taskA && !!taskB && !!taskC, `${taskA}/${taskB}/${taskC}`)

  const feeByRule = (await rawReq('GET', `/dms/settlement/fee/${taskA}`, null, TOKEN)).json?.data
  check('★ 规则命中生效（按单 20 元，覆盖缺省费率）',
    num(feeByRule.totalFee) === 20 && num(feeByRule.ruleId) === num(newRule.id),
    `total=${feeByRule.totalFee} ruleId=${feeByRule.ruleId}`)

  await rawReq('POST', `/dms/settlement/rule/${newRule.id}/status?status=0`, null, TOKEN)
  const feeNoRule = (await rawReq('GET', `/dms/settlement/fee/${taskA}`, null, TOKEN)).json?.data
  check('★ 规则停用后回落缺省费率（12km × 2 + 起步 5）',
    num(feeNoRule.totalFee) === num((num(rule.baseFee) + 12 * num(rule.perKmRate)).toFixed(2)) && !feeNoRule.ruleId,
    `total=${feeNoRule.totalFee}`)
  const ruleToggle = (await rawReq('POST', `/dms/settlement/rule/${newRule.id}/status?status=1`, null, TOKEN)).json?.data
  check('规则可再次启用', ruleToggle?.status === 1, `status=${ruleToggle?.status}`)
  await rawReq('POST', `/dms/settlement/rule/${newRule.id}/status?status=0`, null, TOKEN)

  // ══ 三、算费口径（配置化反证 + 部分签收折算） ══
  console.log('\n═══ 三、算费口径（配置化 + 部分签收） ═══')
  const feeA = (await rawReq('GET', `/dms/settlement/fee/${taskA}`, null, TOKEN)).json?.data
  const expectMileage = (12 - num(rule.freeDistanceKm)) * num(rule.perKmRate)
  check('算费=起步价+里程费（无加急）',
    num(feeA.totalFee) === num((num(rule.baseFee) + expectMileage).toFixed(2)),
    `total=${feeA.totalFee} 期望=${(num(rule.baseFee) + expectMileage).toFixed(2)}`)
  const feeB = (await rawReq('GET', `/dms/settlement/fee/${taskB}`, null, TOKEN)).json?.data
  check('加急任务含加急附加费（优先级≥2）',
    num(feeB.urgentSurcharge) === num(rule.urgentSurcharge),
    `urgent=${feeB.urgentSurcharge} 期望=${rule.urgentSurcharge}`)

  // ★ 配置化反证：改费率 → 算费随之变化 → 还原
  const oldBase = cfgMap['dms.settlement.base.fee']
  await dbQuery(`UPDATE dms_config SET config_value = $1 WHERE config_key = 'dms.settlement.base.fee' AND tenant_id = 0 AND deleted = 0`,
    [String(oldBase + 3)])
  const feeAfter = (await rawReq('GET', `/dms/settlement/fee/${taskA}`, null, TOKEN)).json?.data
  check('★ 费率配置化生效（改配置后算费随之变化，反证非硬编码）',
    num(feeAfter.baseFee) === oldBase + 3 && num(feeAfter.totalFee) === num(feeA.totalFee) + 3,
    `base ${feeA.baseFee} → ${feeAfter.baseFee}`)
  await dbQuery(`UPDATE dms_config SET config_value = $1 WHERE config_key = 'dms.settlement.base.fee' AND tenant_id = 0 AND deleted = 0`,
    [String(oldBase)])
  const feeRestored = (await rawReq('GET', `/dms/settlement/fee/${taskA}`, null, TOKEN)).json?.data
  check('费率已还原', num(feeRestored.baseFee) === oldBase, feeRestored.baseFee)

  // 部分签收：B 应签收 10 / 实签收 6 → 系数 0.6（起步价全收，里程与加急折算）
  await seedSign(taskB, 2, 10, 6, 1)
  const feeBPartial = (await rawReq('GET', `/dms/settlement/fee/${taskB}`, null, TOKEN)).json?.data
  const expectPartial = num((num(rule.baseFee) + num(feeB.mileageFee) * 0.6 + num(rule.urgentSurcharge) * 0.6).toFixed(2))
  check('★ 部分签收按实际数量折算（系数 0.6，起步价全收）',
    num(feeBPartial.billingRatio) === 0.6 && num(feeBPartial.totalFee) === expectPartial,
    `ratio=${feeBPartial.billingRatio} total=${feeBPartial.totalFee} 期望=${expectPartial}`)
  check('算费返回签收口径（应签收/实签收）',
    num(feeBPartial.plannedQuantity) === 10 && num(feeBPartial.actualQuantity) === 6,
    `${feeBPartial.plannedQuantity}/${feeBPartial.actualQuantity}`)

  // 拒收不计费
  await seedSign(taskC, 3, 8, 0, 1)
  const feeCReject = (await rawReq('GET', `/dms/settlement/fee/${taskC}`, null, TOKEN)).json?.data
  check('★ 拒收不计费（合计 0）', num(feeCReject.totalFee) === 0 && num(feeCReject.billingRatio) === 0,
    `total=${feeCReject.totalFee} ratio=${feeCReject.billingRatio}`)
  await dbQuery(`DELETE FROM dms_sign WHERE task_id = $1`, [taskC])

  // ══ 四、结算单闭环（配送员） ══
  console.log('\n═══ 四、结算单闭环（生成 → 确认 → 推送记账） ═══')
  const gen = await rawReq('POST',
    `/dms/settlement/generate?periodStart=${TODAY}&periodEnd=${TODAY}&targetType=1&targetId=990001&remark=${MARK}`, null, TOKEN)
  const st = gen.json?.data
  created.settlementIds.push(st?.id)
  check('生成结算单（草稿）成功', !!st?.id && st.status === 0, JSON.stringify(gen.json).slice(0, 160))
  check('结算单号符合 JSD-YYYYMMDD-序号', /^JSD-\d{8}-\d{3}$/.test(st?.settlementNo || ''), st?.settlementNo)
  check('结算单含计费规则快照', !!st?.ruleSnapshot, st?.ruleSnapshot?.slice(0, 80))
  check('结算金额 = 明细合计（2 单，含部分签收折算）',
    num(st.totalAmount) === num((num(feeA.totalFee) + num(feeBPartial.totalFee)).toFixed(2)),
    `total=${st.totalAmount} vs ${num((num(feeA.totalFee) + num(feeBPartial.totalFee)).toFixed(2))}`)

  const items = (await rawReq('GET', `/dms/settlement/${st.id}/items`, null, TOKEN)).json?.data || []
  check('明细 2 行且含费用构成', items.length === 2 && items.every(i => 'baseFee' in i && 'totalFee' in i), `rows=${items.length}`)
  const itemsSum = items.reduce((a, i) => a + num(i.totalFee), 0)
  check('明细合计与结算单金额一致', itemsSum === num(st.totalAmount), `${itemsSum} vs ${st.totalAmount}`)
  const partialItem = items.find(i => num(i.taskId) === num(taskB))
  check('明细保留部分签收口径（签收类型/应签收/实签收/系数）',
    num(partialItem?.signType) === 2 && num(partialItem?.plannedQuantity) === 10
    && num(partialItem?.actualQuantity) === 6 && num(partialItem?.billingRatio) === 0.6,
    `signType=${partialItem?.signType} ratio=${partialItem?.billingRatio}`)

  const pageRes = (await rawReq('GET', `/dms/settlement/page?current=1&size=10&settlementNo=${st.settlementNo}`, null, TOKEN)).json?.data
  check('结算单分页可查', (pageRes?.records || []).length === 1 && num(pageRes.total) === 1, `total=${pageRes?.total}`)

  const okRes = await rawReq('POST', `/dms/settlement/${st.id}/confirm`, null, TOKEN)
  check('确认结算单（草稿→已确认）', okRes.json?.data?.status === 1, JSON.stringify(okRes.json?.data || {}).slice(0, 120))
  const again = await rawReq('POST', `/dms/settlement/${st.id}/confirm`, null, TOKEN)
  check('重复确认被拒绝（仅草稿可确认）', again.json?.code !== 200, again.json?.message)

  const push1 = await rawReq('POST', `/dms/settlement/${st.id}/push-erp`, null, TOKEN)
  const p1 = push1.json?.data || {}
  created.voucherNos.push(p1.voucherNo)
  check('推送 ERP 成功（已确认→已推送）', !!p1.traceId && p1.idempotent === false, JSON.stringify(p1).slice(0, 140))
  check('★ 推送生成真实记账凭证（返回凭证号 KJPZ-…）',
    /^KJPZ-/.test(p1.voucherNo || '') && p1.accounted === true, `voucherNo=${p1.voucherNo} msg=${p1.accountMessage}`)
  const push2 = await rawReq('POST', `/dms/settlement/${st.id}/push-erp`, null, TOKEN)
  const p2 = push2.json?.data || {}
  check('★ 推送幂等（重复推送返回同一 traceId 与凭证号）',
    p2.idempotent === true && p2.traceId === p1.traceId && p2.voucherNo === p1.voucherNo,
    `${p1.traceId?.slice(0, 8)} vs ${p2.traceId?.slice(0, 8)}`)
  const delPushed = await rawReq('DELETE', `/dms/settlement/${st.id}`, null, TOKEN)
  check('已推送结算单不可删除（保留审计留痕）', delPushed.json?.code !== 200, delPushed.json?.message)

  const outbox = await dbQuery(`SELECT count(*)::int n FROM dms_event_outbox WHERE event_type = 'SETTLEMENT_PUSH_ERP' AND trace_id = $1`, [p1.traceId])
  check('ERP 事件已落发件箱（dms_event_outbox）', outbox[0].n === 1, `rows=${outbox[0].n}`)

  const voucherItems = await dbQuery(
    `SELECT subject_code, debit_amount, credit_amount FROM finance_voucher_item WHERE source_type = 'DMS_SETTLEMENT' AND source_id = $1 AND deleted_flag = 0 ORDER BY id`,
    [st.id])
  check('★ 凭证明细落库（借贷两行，金额=结算金额）',
    voucherItems.length === 2
    && num(voucherItems[0].debit_amount) === num(st.totalAmount)
    && num(voucherItems[1].credit_amount) === num(st.totalAmount),
    JSON.stringify(voucherItems))
  const debitCode = voucherItems[0]?.subject_code
  const creditCode = voucherItems[1]?.subject_code
  check('记账科目与配置一致（借 6602 / 贷 2241 自有配送员）',
    debitCode === String(cfgMap['dms.settlement.account.debit.subject']) && creditCode === String(cfgMap['dms.settlement.account.staff.subject']),
    `${debitCode}/${creditCode}`)
  const settleRow = await dbQuery(`SELECT erp_voucher_no, status FROM dms_settlement WHERE id = $1`, [st.id])
  check('结算单回写凭证号', settleRow[0]?.erp_voucher_no === p1.voucherNo, settleRow[0]?.erp_voucher_no)

  // ══ 五、渠道结算（外部运力 → 应付） ══
  console.log('\n═══ 五、渠道结算（外部运力） ═══')
  const channel = await seedChannel()
  const genCh = await rawReq('POST',
    `/dms/settlement/generate?periodStart=${TODAY}&periodEnd=${TODAY}&targetType=2&targetId=${channel.id}&remark=${MARK}`, null, TOKEN)
  const stCh = genCh.json?.data
  created.settlementIds.push(stCh?.id)
  check('按渠道生成结算单成功', !!stCh?.id && stCh.targetType === 2, JSON.stringify(genCh.json).slice(0, 160))
  check('结算对象名称取渠道档案', stCh?.targetName === channel.channel_name, `${stCh?.targetName} vs ${channel.channel_name}`)
  const chItems = (await rawReq('GET', `/dms/settlement/${stCh.id}/items`, null, TOKEN)).json?.data || []
  check('渠道结算只聚合该渠道任务', chItems.length === 1 && num(chItems[0].taskId) === num(taskC), `rows=${chItems.length}`)
  check('未选结算对象时拒绝生成', (await rawReq('POST',
    `/dms/settlement/generate?periodStart=${TODAY}&periodEnd=${TODAY}&targetType=1`, null, TOKEN)).json?.code !== 200)

  await rawReq('POST', `/dms/settlement/${stCh.id}/confirm`, null, TOKEN)
  const pushCh = (await rawReq('POST', `/dms/settlement/${stCh.id}/push-erp`, null, TOKEN)).json?.data || {}
  created.voucherNos.push(pushCh.voucherNo)
  check('★ 渠道推送生成应付单（payableNo）', !!pushCh.payableNo && pushCh.accounted === true, JSON.stringify(pushCh).slice(0, 160))
  const payables = await dbQuery(
    `SELECT total_amount, paid_amount, remaining_amount, supplier_name FROM finance_payable WHERE source_type = 'DMS_SETTLEMENT' AND source_id = $1 AND deleted_flag = 0`,
    [stCh.id])
  check('★ 应付落库（金额=结算金额，供应商=渠道名）',
    payables.length === 1 && num(payables[0].total_amount) === num(stCh.totalAmount) && payables[0].supplier_name === channel.channel_name,
    JSON.stringify(payables))
  const chVoucherItems = await dbQuery(
    `SELECT subject_code, debit_amount FROM finance_voucher_item WHERE source_type = 'DMS_SETTLEMENT' AND source_id = $1 AND deleted_flag = 0 ORDER BY id`,
    [stCh.id])
  check('渠道记账贷方走应付账款科目', chVoucherItems[1]?.subject_code === String(cfgMap['dms.settlement.account.credit.subject']),
    chVoucherItems.map(v => v.subject_code).join('/'))

  // ══ 六、对账 ══
  console.log('\n═══ 六、对账（结算 vs 财务） ═══')
  const rec = (await rawReq('GET', `/dms/settlement/reconcile?startDate=${TODAY}&endDate=${TODAY}`, null, TOKEN)).json?.data
  check('对账返回汇总与明细行', Array.isArray(rec?.rows) && !!rec?.summary, `rows=${rec?.rows?.length}`)
  const recSelf = (rec?.rows || []).find(r => r.settlementNo === st.settlementNo)
  check('对账行含凭证号与记账状态', recSelf?.erpVoucherNo === p1.voucherNo && recSelf?.accounted === true,
    JSON.stringify(recSelf || {}).slice(0, 140))
  check('对账行输出差异（已结 − 结算；未收款为负）',
    num(recSelf?.diff) === 0 - num(st.totalAmount) && recSelf?.matched === false,
    `settled=${recSelf?.settledAmount} diff=${recSelf?.diff}`)
  const recCh = (rec?.rows || []).find(r => r.settlementNo === stCh.settlementNo)
  check('渠道对账行关联应付单号', recCh?.erpPayableNo === pushCh.payableNo, recCh?.erpPayableNo)
  check('对账汇总金额一致（结算合计 ≥ 本次两张单）',
    num(rec?.summary?.totalAmount) >= num(st.totalAmount) + num(stCh.totalAmount),
    `total=${rec?.summary?.totalAmount}`)
  const recDiffOnly = (await rawReq('GET', `/dms/settlement/reconcile?startDate=${TODAY}&endDate=${TODAY}&onlyDiff=true`, null, TOKEN)).json?.data
  check('「仅看差异」不返回已对平行',
    (recDiffOnly?.rows || []).every(r => num(r.diff) !== 0),
    `rows=${recDiffOnly?.rows?.length}`)

  // ══ 七、报表口径 ══
  console.log('\n═══ 七、周期报表 ═══')
  const report = (await rawReq('GET', `/dms/settlement/report?startDate=${TODAY}&endDate=${TODAY}`, null, TOKEN)).json?.data
  check('报表按已签收/已完成口径统计（含本次造数）', num(report?.totalTasks) >= 3, `totalTasks=${report?.totalTasks}`)
  check('报表返回按配送员/按日聚合', Array.isArray(report?.byRider) && Array.isArray(report?.byDay),
    `byRider=${report?.byRider?.length} byDay=${report?.byDay?.length}`)

  // ══ 八、UI 验收（金标准四视图） ══
  console.log('\n═══ 八、UI 验收 ═══')
  const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
  const FE = process.env.FE_URL || 'http://localhost:5656'
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  const errs = []
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('console', m => { if (m.type() === 'error') errs.push('console: ' + m.text().slice(0, 120)) })
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', '1') }, [TOKEN])
  await page.goto(`${FE}/dms/settlement`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(7000)

  const body = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开（无 404/白屏）', !body.includes('页面不存在') && body.length > 50, page.url())
  const tabTexts = await page.evaluate(() =>
    [...document.querySelectorAll('.tab-item')].map(t => (t.innerText || '').replace(/\s+/g, '')))
  for (const t of ['结算单', '计费规则', '对账', '周期报表']) {
    check(`Tab「${t}」存在`, tabTexts.some(x => x.includes(t)), tabTexts.join('/'))
  }
  for (const b of ['生成结算单', '批量确认', '刷新', '导出']) {
    check(`工具栏含「${b}」`, body.includes(b.replace(/\s+/g, '')))
  }
  check('缺省费率条展示配置化费率', /缺省费率/.test(body) && body.includes('元/km'), body.slice(0, 90))
  const headers = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').replace(/\s+/g, '')).filter(Boolean))
  for (const h of ['操作', '结算单号', '结算对象', '周期起', '周期止', '单量', '结算金额', '状态']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  check('列配置含「记账凭证号」', headers.some(x => x.includes('记账凭证号')), headers.join('/'))
  // 按单号查询后断言列表渲染（共享库里可能还有他人在跑的数据，避免分页错页误判）
  await page.getByPlaceholder('结算单号').first().fill(st.settlementNo)
  await page.locator('.search-grid button').filter({ hasText: /查\s*询/ }).first().click()
  await page.waitForTimeout(2500)
  const filteredBody = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('列表渲染真实结算单数据（按单号查询命中）', filteredBody.includes(st.settlementNo), st.settlementNo)
  await page.screenshot({ path: 'I:/AI-Ready/tool-results/dms-settlement/ui-list.png', fullPage: true })

  // 切到「计费规则」Tab
  await page.locator('.tab-item').filter({ hasText: '计费规则' }).first().click()
  await page.waitForTimeout(2000)
  const ruleBody = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('计费规则 Tab 渲染规则列表（含验收规则名）', ruleBody.includes('E2E按单规则-' + MARK), ruleBody.slice(0, 120))
  const ruleHeaders = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').replace(/\s+/g, '')).filter(Boolean))
  for (const h of ['规则编码', '规则名称', '计价方式', '起步价', '优先级', '状态']) {
    check(`规则表头含「${h}」`, ruleHeaders.some(x => x.includes(h)), ruleHeaders.join('/'))
  }
  check('规则 Tab 工具栏含「新增规则」', ruleBody.includes('新增规则'))
  check('规则计价方式渲染为中文（非原值）', ruleBody.includes('按单（固定）'), ruleBody.slice(0, 120))

  // 新增规则弹窗：分区卡片 + 计价方式联动字段显隐
  await page.locator('.ant-btn').filter({ hasText: /新增规则/ }).first().click()
  await page.waitForTimeout(1500)
  const modalFlat = (await page.locator('.ant-modal-wrap:visible').last().innerText()).replace(/\s+/g, '')
  check('新增规则弹窗含三分区卡片与关键字段',
    ['基本信息', '适用范围', '费率与加价', '规则名称', '计价方式', '起步价'].every(k => modalFlat.includes(k)),
    modalFlat.slice(0, 100))
  await page.screenshot({ path: 'I:/AI-Ready/tool-results/dms-settlement/ui-rule-modal.png', fullPage: true })
  await page.locator('.ant-modal-wrap:visible .ant-form-item').filter({ hasText: '计价方式' }).locator('.ant-select').first().click()
  await page.waitForTimeout(700)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: /按单/ }).first().click()
  await page.waitForTimeout(900)
  const kmFieldVisible = await page.locator('.ant-modal-wrap:visible .ant-form-item')
    .filter({ hasText: '每公里单价' }).first().isVisible().catch(() => false)
  check('计价方式=按单时隐藏里程字段（联动生效）', !kmFieldVisible, 'kmFieldVisible=' + kmFieldVisible)
  await page.locator('.ant-modal-wrap:visible .ant-modal-close').last().click()
  await page.waitForTimeout(700)

  // 切到「对账」Tab
  await page.locator('.tab-item').filter({ hasText: '对账' }).first().click()
  await page.waitForTimeout(2500)
  const recBody = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('对账 Tab 渲染汇总指标', ['结算单数', '结算金额', '已结金额', '差异合计', '差异单数', '未记账'].every(k => recBody.includes(k)), recBody.slice(0, 140))
  check('对账 Tab 渲染本期结算单', recBody.includes(st.settlementNo), st.settlementNo)
  check('对账行渲染记账/对平状态标签（含凭证号与差异值）',
    recBody.includes('已记账') && (recBody.includes('有差异') || recBody.includes('已对平'))
    && recBody.includes(p1.voucherNo || 'KJPZ'), recBody.slice(0, 160))
  check('运行期无接口错误（无 404/500）', errs.length === 0, errs.slice(0, 3).join(' || '))
  await page.locator('.tab-item').filter({ hasText: '计费规则' }).first().click()
  await page.waitForTimeout(2000)
  await page.screenshot({ path: 'I:/AI-Ready/tool-results/dms-settlement/ui-rule.png', fullPage: true })
  await page.locator('.tab-item').filter({ hasText: '对账' }).first().click()
  await page.waitForTimeout(2000)
  await page.screenshot({ path: 'I:/AI-Ready/tool-results/dms-settlement/ui-reconcile.png', fullPage: true })
  await browser.close()

  // ══ 九、清理 ══
  console.log('\n═══ 九、清理 ═══')
  await cleanup()

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 汇总：${pass}/${results.length} 通过${fail ? `，${fail} 项失败` : ''} ═══`)
  if (fail) results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  process.exitCode = fail ? 1 : 0
})().catch(async e => {
  console.error('E2E 异常终止:', e.message, e.stack?.split('\n')[1] || '')
  try {
    console.log('\n═══ 异常收尾清理 ═══')
    await cleanup()
  } catch (x) {
    console.error('清理失败:', x.message)
  }
  process.exitCode = 1
})
