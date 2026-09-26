/*
 * 「记账失败 → 业务单据回滚」验证（2026-09-26）
 *
 * 背景（FINANCE_MODULE_AUDIT §2.2）：记账失败原先被 `catch { log }` 静默吞掉 ⇒
 * 单据已提交、凭证缺失，且无人知晓。按业界对标（SAP FI/MM 的同一 LUW、
 * Oracle SLA 的 Final Mode）改为**失败即整体回滚**，本轮改了 6 个文件共 16 处。
 *
 * 验证手法：用「关闭会计期间」构造一个必然失败的记账场景 ——
 *   VoucherServiceImpl.assertPeriodOpen 在凭证 create/audit/post 时会抛「会计期间已关闭」，
 *   从而让整条「业务单据动作 + 记账」链路失败。然后断言：
 *   ① 接口返回失败；
 *   ② **单据状态未被推进**（仍停留原状态）—— 这是「回滚生效」的直接证据；
 *   ③ 没有产生任何凭证。
 *
 * 目标接口选「其他收入单 confirm」：它直接调用 BusinessAccountingService.createVoucherFromBusiness，
 * 且单据状态字段（status/settle_status）明确，便于断言。
 *
 * 用法：node tools/verify-accounting-failure-rollback.cjs   （需后端 5655 在跑）
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
// 用当前月作为测试期间（避免影响历史数据）
const now = new Date()
const Y = now.getFullYear(), P = now.getMonth() + 1
const PERIOD = `${Y}-${String(P).padStart(2, '0')}`
const vcount = () => q(`select count(*) from finance_voucher where fiscal_year=${Y} and fiscal_period=${P}`)

;(async () => {
  const beforePeriod = q(`select status from fin_accounting_period where period_code='${PERIOD}'`)
  console.log(`测试期间 ${PERIOD}（当前状态 status=${beforePeriod}，1=未结账）\n`)

  const cap = await call('GET', '/auth/captcha')
  const full = JSON.parse(cap.body)
  const code = [...Buffer.from(full.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const lg = await call('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: full.data.uuid,
  })
  const token = JSON.parse(lg.body).data.token
  console.log('登录(超管):', lg.status, '\n')

  let docId = null
  const vBefore = vcount()
  try {
    // ── 建一张其他收入单（草稿）──
    const created = await call('POST', '/erp/finance/other-income-doc/save-draft', {
      incomeType: 1,
      incomeDate: `${Y}-${String(P).padStart(2, '0')}-20`,
      accountSubjectCode: '6001',
      summary: 'E2E-ROLLBACK-CHECK',
      receiptAccount1: '1002', receiptAmount1: 100,
      items: [{ lineNo: 1, incomeName: 'E2E-VERIFY', amount: 100 }],
    }, token)
    docId = (() => { try { return JSON.parse(created.body).data?.id } catch { return null } })()
    const listed = docId ? '' : q(`select id from fin_other_income_doc where summary='E2E-ROLLBACK-CHECK' order by id desc limit 1`)
    if (!docId && listed) docId = listed
    check('① 其他收入单已创建', !!docId, `HTTP ${created.status} id=${docId}`)
    if (!docId) throw new Error('无法创建其他收入单，验证中止')

    const st0 = q(`select coalesce(status::text,'-') from fin_other_income_doc where id=${docId}`)
    console.log(`   单据初始状态 status=${st0}`)

    // ── 关闭会计期间，令记账必然失败 ──
    q(`update fin_accounting_period set status=0 where period_code='${PERIOD}'`)
    console.log(`   已临时关闭期间 ${PERIOD}（1 → 0），使记账必然失败\n`)

    // ── 调 confirm（会触发记账）──
    console.log('【核心】记账失败时应「接口失败 + 单据状态不推进」')
    const cf = await call('POST', `/erp/finance/other-income-doc/${docId}/confirm`, {}, token)
    const failed = cf.status >= 400 || /失败|已关闭|回滚/.test(cf.body)
    check('② 接口返回失败（不再静默成功）', failed, `HTTP ${cf.status} ${cf.body.slice(0, 100)}`)

    const st1 = q(`select coalesce(status::text,'-') from fin_other_income_doc where id=${docId}`)
    check('③ 【核心】单据状态未被推进为「已记账」', st1 === st0, `记账前 ${st0} → 记账失败后 ${st1}`)

    const vAfter = vcount()
    check('④ 未产生任何凭证（该期凭证数未增加）', vAfter === vBefore, `${vBefore} → ${vAfter}`)

    // ── 对照组：恢复期间后再 confirm，应成功 ──
    console.log('\n【对照】恢复期间后再 confirm —— 应正常成功')
    q(`update fin_accounting_period set status=${beforePeriod} where period_code='${PERIOD}'`)
    const cf2 = await call('POST', `/erp/finance/other-income-doc/${docId}/confirm`, {}, token)
    check('⑤ 恢复期间后 confirm 成功', cf2.status === 200, `HTTP ${cf2.status} ${cf2.body.slice(0, 80)}`)
    const st2 = q(`select coalesce(status::text,'-') from fin_other_income_doc where id=${docId}`)
    check('⑥ 单据状态已推进（对照组成立）', st2 !== st0, `status ${st0} → ${st2}`)
  } finally {
    // 兜底还原期间状态，避免把库留在「已关账」
    q(`update fin_accounting_period set status=${beforePeriod || 1} where period_code='${PERIOD}'`)
    const restored = q(`select status from fin_accounting_period where period_code='${PERIOD}'`)
    check('期间状态已还原', restored === beforePeriod, `还原为 ${restored}（原值 ${beforePeriod}）`)

    // 清理测试数据（单据 + 其凭证）
    if (docId) {
      const vnos = q(`select string_agg(voucher_no, ',') from finance_voucher where summary like '%E2E-ROLLBACK%' or remark like '%E2E-ROLLBACK%'`)
      if (vnos) {
        q(`delete from finance_voucher_item where voucher_id in (select id from finance_voucher where voucher_no in (${vnos.split(',').map(v => `'${v}'`).join(',')}))`)
        q(`delete from finance_voucher where voucher_no in (${vnos.split(',').map(v => `'${v}'`).join(',')})`)
      }
      q(`delete from fin_other_income_doc_item where doc_id=${docId}`)
      q(`delete from fin_other_income_doc where id=${docId}`)
    }
    const left = q(`select count(*) from fin_other_income_doc where summary='E2E-ROLLBACK-CHECK'`)
    check('测试单据已清理', left === '0', `残留 ${left}`)
  }

  console.log(`\n===== 结果：${pass} 通过 / ${fail} 失败 =====`)
  process.exit(fail > 0 ? 1 : 0)
})().catch(e => {
  try { q(`update fin_accounting_period set status=1 where period_code='${PERIOD}'`) } catch {}
  console.error('ERR', e.message); process.exit(1)
})
