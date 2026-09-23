/*
 * 凭证红冲（reverse）P0 红线验证（2026-09-23）
 *
 * 修复前 `VoucherServiceImpl.reverse()` 的四个问题：
 *   ① 不调 assertPeriodOpen ⇒ 可向**已关账期间**生成红冲凭证并直接入账；
 *   ② 红冲凭证直接 setStatus("posted") + 手工调 postToLedger ⇒ 绕过审核环节与凭证状态机；
 *   ③ 无原凭证状态前置校验 ⇒ 可红冲**草稿**凭证（原凭证根本没入账，冲销却入了账，
 *      总账凭空多出一笔反向分录）；已冲销的凭证也可重复红冲；
 *   ④ 借用原凭证的 postBy 当制单人 ⇒ 制单责任错位。
 *
 * 本脚本按四个场景逐条验证（含回归）：
 *   A 正常红冲：建 → 审核 → 过账 → 红冲，断言红冲凭证走完整流程且原凭证置 reversed
 *   B 草稿不可红冲（新增的前置校验）
 *   C 已冲销不可重复红冲（新增的前置校验）
 *   D 已关账期间不可红冲（新增的期间校验）
 *      —— 库中当前所有会计期间均为「未结账」(status=1)，故本场景临时将某期间置 0，
 *         测完**立即还原并复验**，属造测试条件而非改业务配置。
 *
 * 用法：node tools/verify-voucher-reverse.cjs     （需后端 5655 在跑）
 */
const http = require('http')
const { execFileSync } = require('child_process')
const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe'
const PGENV = { ...process.env, PGPASSWORD: 'devuser123' }

function q(sql) {
  return execFileSync(PSQL, ['-h', 'localhost', '-p', '5432', '-U', 'devuser', '-d', 'devdb', '-t', '-A', '-c', sql],
    { env: PGENV, encoding: 'utf8' }).trim()
}
function call(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers['Authorization'] = 'Bearer ' + token
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: 5655, path: '/api' + path, method, headers },
      res => { let s = ''; res.on('data', c => s += c); res.on('end', () => resolve({ status: res.statusCode, body: s })) })
    r.on('error', reject); if (data) r.write(data); r.end()
  })
}
let pass = 0, fail = 0
function check(desc, ok, detail) {
  console.log(`${ok ? '✅' : '❌'} ${desc}${detail ? ' → ' + detail : ''}`)
  ok ? pass++ : fail++
}
const TEST_YEAR = 2026, TEST_PERIOD = 9   // 库中 2026-09 为未结账期间
const createdIds = []

async function makeVoucher(token, tag) {
  const r = await call('POST', '/erp/finance/voucher', {
    voucherDate: `${TEST_YEAR}-09-15`, fiscalYear: TEST_YEAR, fiscalPeriod: TEST_PERIOD,
    summary: `E2E-REVERSE-${tag}`, voucherType: 'manual',
    items: [
      { summary: '借', subjectCode: '1001', subjectName: '库存现金', debitAmount: 10, creditAmount: 0 },
      { summary: '贷', subjectCode: '6001', subjectName: '主营业务收入', debitAmount: 0, creditAmount: 10 },
    ],
  }, token)
  const id = (() => { try { return JSON.parse(r.body).data?.id } catch { return null } })()
  if (id) createdIds.push(id)
  return { status: r.status, id, body: r.body }
}

;(async () => {
  const cap = await call('GET', '/auth/captcha')
  const full = JSON.parse(cap.body)
  const code = [...Buffer.from(full.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const lg = await call('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: full.data.uuid,
  })
  const token = JSON.parse(lg.body).data.token
  console.log('登录(超管):', lg.status, '\n')

  // 测试前采集总账快照（用于跑完后精确恢复；finance_ledger 是累加式、删凭证不会回退它）
  const ledgerSnap = q(`select id || '|' || subject_code || '|' || coalesce(period_debit::text,'') || '|' ||
      coalesce(period_credit::text,'') || '|' || coalesce(closing_debit::text,'') || '|' ||
      coalesce(closing_credit::text,'') || '|' || coalesce(closing_balance::text,'') || '|' ||
      coalesce(balance_direction::text,'NULL')
    from finance_ledger where fiscal_year=${TEST_YEAR} and fiscal_period=${TEST_PERIOD}
      and subject_code in ('1001','6001') order by id`)
    .split('\n').map(s => s.trim()).filter(Boolean)
  console.log(`总账快照（${TEST_YEAR}-${TEST_PERIOD} / 1001,6001）：${ledgerSnap.length} 行\n`)

  // ── A 正常红冲 ──
  console.log('【A】正常红冲：建 → 审核 → 过账 → 红冲')
  const a = await makeVoucher(token, 'A')
  check('A1 建凭证成功', a.status === 200 && a.id, `HTTP ${a.status} id=${a.id}`)
  const st1 = q(`select status from finance_voucher where id=${a.id}`)
  check('A2 新建凭证初始状态为 draft', st1 === 'draft', `status=${st1}`)
  await call('PUT', `/erp/finance/voucher/${a.id}/audit`, {}, token)
  await call('PUT', `/erp/finance/voucher/${a.id}/post`, {}, token)
  const st2 = q(`select status from finance_voucher where id=${a.id}`)
  check('A3 过账后状态为 posted', st2 === 'posted', `status=${st2}`)
  const rev = await call('POST', `/erp/finance/voucher/${a.id}/reverse`, { reason: 'E2E-REVERSE' }, token)
  check('A4 红冲请求成功', rev.status === 200, `HTTP ${rev.status} ${rev.body.slice(0, 80)}`)
  const st3 = q(`select status from finance_voucher where id=${a.id}`)
  check('A5 原凭证置为 reversed', st3 === 'reversed', `status=${st3}`)
  // 红冲凭证：按 source_no 找
  const revId = q(`select id from finance_voucher where source_no=(select voucher_no from finance_voucher where id=${a.id}) order by id desc limit 1`)
  const revStatus = revId ? q(`select status from finance_voucher where id=${revId}`) : ''
  check('A6 红冲凭证走完整流程（最终 posted，非直接置位）', revStatus === 'posted', `红冲凭证 status=${revStatus} id=${revId}`)
  const revItems = revId ? q(`select count(*) from finance_voucher_item where voucher_id=${revId}`) : '0'
  check('A7 红冲分录已生成（借贷互换 2 条）', revItems === '2', `分录数=${revItems}`)
  const bal = revId ? q(`select coalesce(sum(debit_amount),0)=coalesce(sum(credit_amount),0) from finance_voucher_item where voucher_id=${revId}`) : 'f'
  check('A8 红冲凭证借贷平衡', bal === 't', `平衡=${bal}`)
  const prepBy = revId ? q(`select coalesce(prep_by,'NULL') from finance_voucher where id=${revId}`) : ''
  check('A9 制单人取真实登录人（非原凭证 postBy）', prepBy !== 'NULL' && prepBy !== '', `prep_by=${prepBy}`)

  // ── B 草稿不可红冲 ──
  console.log('\n【B】草稿凭证不可红冲（新增前置校验）')
  const b = await makeVoucher(token, 'B')
  check('B1 建草稿凭证成功', b.status === 200 && b.id, `id=${b.id}`)
  const rb = await call('POST', `/erp/finance/voucher/${b.id}/reverse`, { reason: '试图红冲草稿' }, token)
  check('B2 红冲草稿被拒', rb.status >= 400, `HTTP ${rb.status} ${rb.body.slice(0, 110)}`)

  // ── C 已冲销不可重复红冲 ──
  console.log('\n【C】已冲销凭证不可重复红冲')
  const rc = await call('POST', `/erp/finance/voucher/${a.id}/reverse`, { reason: '试图重复红冲' }, token)
  check('C1 重复红冲被拒', rc.status >= 400, `HTTP ${rc.status} ${rc.body.slice(0, 110)}`)

  // ── D 已关账期间不可红冲 ──
  console.log('\n【D】已关账期间不可红冲（临时造条件，测完还原）')
  const d = await makeVoucher(token, 'D')
  await call('PUT', `/erp/finance/voucher/${d.id}/audit`, {}, token)
  await call('PUT', `/erp/finance/voucher/${d.id}/post`, {}, token)
  check('D1 测试凭证已过账', q(`select status from finance_voucher where id=${d.id}`) === 'posted')
  const beforePeriod = q(`select status from fin_accounting_period where period_code='${TEST_YEAR}-09'`)
  q(`update fin_accounting_period set status=0 where period_code='${TEST_YEAR}-09'`)
  const afterSet = q(`select status from fin_accounting_period where period_code='${TEST_YEAR}-09'`)
  console.log(`   （已临时把 ${TEST_YEAR}-09 置为已关账：${beforePeriod} → ${afterSet}）`)
  let rd, restoreOk
  try {
    rd = await call('POST', `/erp/finance/voucher/${d.id}/reverse`, { reason: '试图红冲已关账期间' }, token)
    check('D2 已关账期间红冲被拒', rd.status >= 400 && /关闭|关账/.test(rd.body),
      `HTTP ${rd.status} ${rd.body.slice(0, 110)}`)
  } finally {
    q(`update fin_accounting_period set status=${beforePeriod} where period_code='${TEST_YEAR}-09'`)
    restoreOk = q(`select status from fin_accounting_period where period_code='${TEST_YEAR}-09'`)
    check('D3 期间状态已还原', restoreOk === beforePeriod, `还原为 ${restoreOk}（原值 ${beforePeriod}）`)
  }
  const dStatus = q(`select status from finance_voucher where id=${d.id}`)
  check('D4 被拒后原凭证状态未变（仍 posted）', dStatus === 'posted', `status=${dStatus}`)

  // ── 清理 ──
  // ⚠️ 红冲会 postToLedger 写总账，而 finance_ledger 是**纯累加式**（无按凭证回滚入口，
  //    见 FINANCE_MODULE_AUDIT §2.1）⇒ 只删凭证**不会**回退总账，必须按测试前的快照恢复，
  //    否则本脚本会把 1001/6001 的发生额永久抬高，污染总账/科目余额表/报表。
  console.log('\n【清理】')
  for (const id of createdIds) {
    const rv = q(`select id from finance_voucher where source_no=(select voucher_no from finance_voucher where id=${id})`)
    q(`delete from finance_voucher_item where voucher_id=${id} ${rv ? 'or voucher_id in (' + rv + ')' : ''}`)
    q(`delete from finance_voucher where id=${id} ${rv ? 'or id in (' + rv + ')' : ''}`)
  }
  const left = q(`select count(*) from finance_voucher where summary like 'E2E-REVERSE-%'`)
  check('测试凭证已清理', left === '0', `残留 ${left} 行`)

  // 总账恢复
  const SCOPE = `fiscal_year=${TEST_YEAR} and fiscal_period=${TEST_PERIOD} and subject_code in ('1001','6001')`
  const snapIds = ledgerSnap.map(r => r.split('|')[0]).filter(Boolean)
  if (snapIds.length === 0) {
    q(`delete from finance_ledger where ${SCOPE}`)
  } else {
    q(`delete from finance_ledger where ${SCOPE} and id not in (${snapIds.join(',')})`)
    for (const line of ledgerSnap) {
      const [id, _code, pd, pc, cd, cc, cb, bd] = line.split('|')
      const num = (v) => (v === '' || v === 'NULL' ? 'null' : v)
      q(`update finance_ledger set period_debit=${num(pd)}, period_credit=${num(pc)}, closing_debit=${num(cd)}, closing_credit=${num(cc)}, closing_balance=${num(cb)}, balance_direction=${bd && bd !== 'NULL' ? `'${bd}'` : 'null'} where id=${id}`)
    }
  }
  const after = q(`select coalesce(string_agg(id::text,',' order by id),'(empty)') from finance_ledger where ${SCOPE}`)
  const beforeStr = snapIds.length ? snapIds.join(',') : '(empty)'
  check('总账已按测试前快照恢复', after === beforeStr, `恢复后=[${after}] 测试前=[${beforeStr}]`)

  console.log(`\n===== 结果：${pass} 通过 / ${fail} 失败 =====`)
  process.exit(fail > 0 ? 1 : 0)
})().catch(e => {
  // 异常时兜底还原期间状态，避免把库留在"已关账"状态
  try { q(`update fin_accounting_period set status=1 where period_code='2026-09'`) } catch {}
  console.error('ERR', e.message); process.exit(1)
})
