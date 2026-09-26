/*
 * postToLedger 重写验证（2026-09-26）
 *
 * 背景：FINANCE_MODULE_AUDIT §2.1 —— LedgerServiceImpl.postToLedger() 原为**纯累加式**
 * （periodDebit += 本次借额），凭证删改或重复过账后总账只增不减、永久残留。
 * 本轮改为「按凭证分录重算该科目该期」，目标是**幂等 + 自愈**。
 *
 * 本脚本验证的核心断言：
 *   A 正确性：正常过账后，总账该科目该期的发生额 = 凭证分录金额
 *   B **幂等性（关键）**：把凭证状态回退为 audited 后**再过一次账**，
 *     总账**不得翻倍**（旧实现会翻倍，新实现应保持原值）
 *   C 自愈性：人为把总账该行改成一个错误值（模拟历史漂移），
 *     之后任意一次该科目该期的记账都会把它纠正回凭证口径
 *
 * 用法：node tools/verify-post-to-ledger-idempotent.cjs   （需后端 5655 在跑）
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
const Y = 2026, P = 9
// 测试用科目：1001 库存现金（借）/ 6001 主营业务收入（贷）
const SUBJ = { debit: 1001, credit: 6001 }
const AMT = 100

function entryOf(accId, code) {
  const v = q(`select coalesce(period_debit,0)||'/'||coalesce(period_credit,0)||'/'||coalesce(closing_balance,0)
    from finance_ledger where subject_code='${code}' and fiscal_year=${Y} and fiscal_period=${P} and tenant_id=1`)
  return v || 'absent'
}

;(async () => {
  // 测试前快照（跑完按快照恢复，因为总账是会被测试影响的真实数据）
  const snap = q(`select id||'|'||coalesce(opening_debit,0)||'|'||coalesce(opening_credit,0)||'|'||
      coalesce(period_debit,0)||'|'||coalesce(period_credit,0)||'|'||coalesce(closing_debit,0)||'|'||
      coalesce(closing_credit,0)||'|'||coalesce(closing_balance,0)||'|'||coalesce(balance_direction,1)
    from finance_ledger where subject_code in ('${SUBJ.debit}','${SUBJ.credit}')
      and fiscal_year=${Y} and fiscal_period=${P} and tenant_id=1 order by id`)
    .split('\n').map(s => s.trim()).filter(Boolean)
  const snapIds = snap.map(r => r.split('|')[0])
  console.log(`测试前总账快照（${Y}-${P} / ${SUBJ.debit},${SUBJ.credit}）：${snap.length} 行\n`)

  const cap = await call('GET', '/auth/captcha')
  const full = JSON.parse(cap.body)
  const code = [...Buffer.from(full.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const lg = await call('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: full.data.uuid,
  })
  const token = JSON.parse(lg.body).data.token
  console.log('登录(超管):', lg.status, '\n')

  let vid = null
  try {
    // ── 建凭证 → 审核 → 过账 ──
    console.log('【A】正常过账：总账应等于凭证金额')
    const c = await call('POST', '/erp/finance/voucher', {
      voucherDate: `${Y}-09-20`, fiscalYear: Y, fiscalPeriod: P, summary: 'E2E-LEDGER-IDEMPOTENT',
      voucherType: 'manual',
      items: [
        { summary: '借', subjectCode: '1001', subjectName: '库存现金', debitAmount: AMT, creditAmount: 0 },
        { summary: '贷', subjectCode: '6001', subjectName: '主营业务收入', debitAmount: 0, creditAmount: AMT },
      ],
    }, token)
    vid = (() => { try { return JSON.parse(c.body).data?.id } catch { return null } })()
    check('A1 建凭证成功', c.status === 200 && vid, `HTTP ${c.status} id=${vid}`)
    await call('PUT', `/erp/finance/voucher/${vid}/audit`, {}, token)
    const p1 = await call('PUT', `/erp/finance/voucher/${vid}/post`, {}, token)
    check('A2 过账成功', p1.status === 200, `HTTP ${p1.status}`)

    // 本期凭证分录合计（该科目）
    const vouDr = q(`select coalesce(sum(i.debit_amount),0) from finance_voucher_item i join finance_voucher v on v.id=i.voucher_id
      where i.subject_code='${SUBJ.debit}' and v.status='posted' and v.deleted_flag=0 and v.fiscal_year=${Y} and v.fiscal_period=${P}`)
    const ledDr = q(`select coalesce(period_debit,0) from finance_ledger where subject_code='${SUBJ.debit}' and fiscal_year=${Y} and fiscal_period=${P} and tenant_id=1`)
    check('A3 总账本期借方 = 凭证分录合计', ledDr === vouDr, `ledger=${ledDr} voucher_item=${vouDr}`)

    const before = entryOf(0, SUBJ.debit)
    const beforeCr = entryOf(0, SUBJ.credit)

    // ── B 幂等性：把状态回退为 audited，再过一次账 ──
    console.log('\n【B】幂等性（关键）：重复过账不得翻倍')
    q(`update finance_voucher set status='audited' where id=${vid}`)
    const p2 = await call('PUT', `/erp/finance/voucher/${vid}/post`, {}, token)
    check('B1 第二次过账请求发出', p2.status === 200, `HTTP ${p2.status}`)
    const after = entryOf(0, SUBJ.debit)
    const afterCr = entryOf(0, SUBJ.credit)
    check('B2 【核心】借方未翻倍', after === before, `第一次过账后=${before} → 第二次后=${after}`)
    check('B3 【核心】贷方未翻倍', afterCr === beforeCr, `第一次过账后=${beforeCr} → 第二次后=${afterCr}`)
    // 与凭证口径对照
    const ledDr2 = q(`select coalesce(period_debit,0) from finance_ledger where subject_code='${SUBJ.debit}' and fiscal_year=${Y} and fiscal_period=${P} and tenant_id=1`)
    check('B4 仍等于凭证分录合计（未虚增）', ledDr2 === vouDr, `ledger=${ledDr2} voucher_item=${vouDr}`)

    // ── C 自愈性：人为写入漂移，再做一次该科目该期记账 ──
    console.log('\n【C】自愈性：人为制造漂移，下次记账应纠正')
    q(`update finance_ledger set period_debit = period_debit + 9999
       where subject_code='${SUBJ.debit}' and fiscal_year=${Y} and fiscal_period=${P} and tenant_id=1`)
    const polluted = q(`select period_debit from finance_ledger where subject_code='${SUBJ.debit}' and fiscal_year=${Y} and fiscal_period=${P} and tenant_id=1`)
    console.log(`   已人为把借方改成 ${polluted}（原 ${before.split('/')[0]}，模拟历史漂移）`)
    // 再造一张同科目同期的凭证并过账，触发该科目该期重算
    const c2 = await call('POST', '/erp/finance/voucher', {
      voucherDate: `${Y}-09-21`, fiscalYear: Y, fiscalPeriod: P, summary: 'E2E-LEDGER-SELFHEAL',
      voucherType: 'manual',
      items: [
        { summary: '借', subjectCode: '1001', subjectName: '库存现金', debitAmount: 1, creditAmount: 0 },
        { summary: '贷', subjectCode: '6001', subjectName: '主营业务收入', debitAmount: 0, creditAmount: 1 },
      ],
    }, token)
    const vid2 = (() => { try { return JSON.parse(c2.body).data?.id } catch { return null } })()
    await call('PUT', `/erp/finance/voucher/${vid2}/audit`, {}, token)
    await call('PUT', `/erp/finance/voucher/${vid2}/post`, {}, token)
    const healed = q(`select coalesce(period_debit,0) from finance_ledger where subject_code='${SUBJ.debit}' and fiscal_year=${Y} and fiscal_period=${P} and tenant_id=1`)
    const expect = q(`select coalesce(sum(i.debit_amount),0) from finance_voucher_item i join finance_voucher v on v.id=i.voucher_id
      where i.subject_code='${SUBJ.debit}' and v.status='posted' and v.deleted_flag=0 and v.fiscal_year=${Y} and v.fiscal_period=${P}`)
    check('C1 【核心】漂移被纠正回凭证口径', healed === expect, `纠正后=${healed} 凭证口径=${expect}`)
    // 清理第二张凭证
    if (vid2) { q(`delete from finance_voucher_item where voucher_id=${vid2}`); q(`delete from finance_voucher where id=${vid2}`) }
  } finally {
    // ── 清理 ──
    console.log('\n【清理】')
    if (vid) {
      q(`delete from finance_voucher_item where voucher_id=${vid}`)
      q(`delete from finance_voucher where id=${vid}`)
    }
    const leftV = q(`select count(*) from finance_voucher where summary like 'E2E-LEDGER-%'`)
    check('测试凭证已清理', leftV === '0', `残留 ${leftV}`)

    // 按快照恢复总账（本模块无自动回滚，必须显式还原）
    const SCOPE = `subject_code in ('${SUBJ.debit}','${SUBJ.credit}') and fiscal_year=${Y} and fiscal_period=${P} and tenant_id=1`
    if (snapIds.length === 0) {
      q(`delete from finance_ledger where ${SCOPE}`)
    } else {
      q(`delete from finance_ledger where ${SCOPE} and id not in (${snapIds.join(',')})`)
      for (const line of snap) {
        const [id, od, oc, pd, pc, cd, cc, cb, bd] = line.split('|')
        q(`update finance_ledger set opening_debit=${od}, opening_credit=${oc}, period_debit=${pd},
             period_credit=${pc}, closing_debit=${cd}, closing_credit=${cc}, closing_balance=${cb},
             balance_direction=${bd} where id=${id}`)
      }
    }
    const afterIds = q(`select coalesce(string_agg(id::text,',' order by id),'-') from finance_ledger where ${SCOPE}`)
    const beforeIds = snapIds.length ? snapIds.join(',') : '-'
    check('总账已按快照恢复', afterIds === beforeIds, `恢复后=[${afterIds}] 测试前=[${beforeIds}]`)
  }

  console.log(`\n===== 结果：${pass} 通过 / ${fail} 失败 =====`)
  process.exit(fail > 0 ? 1 : 0)
})().catch(e => { console.error('ERR', e.message); process.exit(1) })
