/*
 * 预算台账防篡改验证（2026-09-26）
 *
 * 对应 FINANCE_MODULE_AUDIT §2.5，本轮修了两处：
 *   ① `BudgetItemServiceImpl.update` 原用 BeanUtil.copyProperties 全量覆盖 ⇒ 请求体只要带上
 *      usedAmount / frozenAmount / remainingAmount / executionRate 就能**直接改写预算台账**，
 *      而 `BudgetControlServiceImpl` 类注释明示这四列是「唯一事实来源，禁止任何页面/接口直接改写」。
 *      修法：把四列从拷贝中排除，并按恒等式重算 remaining / executionRate。
 *   ② `BudgetControlServiceImpl` 的 freeze/release/consume 三个入口原先**无 amount > 0 校验** ⇒
 *      传负数可把「冻结」变「解冻」、「消耗」变「回冲」。修法：三处均加正数校验。
 *
 * 用法：node tools/verify-budget-ledger-protection.cjs   （需后端 5655 在跑）
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

;(async () => {
  // 取一个未执行过的预算科目作为样本
  const row = q(`select id||'|'||budget_id||'|'||budget_amount||'|'||used_amount||'|'||frozen_amount||'|'||remaining_amount
                  from budget_item order by id limit 1`)
  const [ITEM, BUDGET, BUDGET_AMT, USED, FROZEN, REMAIN] = row.split('|')
  console.log(`样本：budget_item id=${ITEM} budget=${BUDGET} 预算额=${BUDGET_AMT} 已执行=${USED} 冻结=${FROZEN} 剩余=${REMAIN}\n`)

  const cap = await call('GET', '/auth/captcha')
  const full = JSON.parse(cap.body)
  const code = [...Buffer.from(full.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const lg = await call('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: full.data.uuid,
  })
  const token = JSON.parse(lg.body).data.token
  console.log('登录(超管):', lg.status, '\n')

  try {
    // ── ① 篡改执行态字段应被忽略 ──
    console.log('【①】请求体带 usedAmount/remainingAmount —— 不得改写台账')
    const r1 = await call('PUT', `/erp/budget/item/${ITEM}`,
      { id: Number(ITEM), budgetId: Number(BUDGET), usedAmount: 99999, frozenAmount: 88888, remainingAmount: 1 }, token)
    const after1 = q(`select used_amount||'|'||frozen_amount||'|'||remaining_amount from budget_item where id=${ITEM}`)
    const [u1, f1, rm1] = after1.split('|')
    check('①-1 update 请求返回成功', r1.status === 200, `HTTP ${r1.status}`)
    check('①-2 used_amount 未被篡改', u1 === USED, `期望 ${USED}，实际 ${u1}`)
    check('①-3 frozen_amount 未被篡改', f1 === FROZEN, `期望 ${FROZEN}，实际 ${f1}`)
    check('①-4 remaining_amount 未被篡改', rm1 === REMAIN, `期望 ${REMAIN}，实际 ${rm1}`)

    // ── ② 调整预算额后，恒等式仍成立 ──
    console.log('\n【②】调整 budgetAmount 后 remaining = budget − used − frozen')
    const NEW_BUDGET = 30000
    const r2 = await call('PUT', `/erp/budget/item/${ITEM}`,
      { id: Number(ITEM), budgetId: Number(BUDGET), budgetAmount: NEW_BUDGET }, token)
    check('②-1 update 请求成功', r2.status === 200, `HTTP ${r2.status}`)
    const after2 = q(`select budget_amount||'|'||used_amount||'|'||frozen_amount||'|'||remaining_amount||'|'||execution_rate
                      from budget_item where id=${ITEM}`)
    const [b2, u2, f2, rm2, er2] = after2.split('|')
    check('②-2 budget_amount 已更新', Number(b2) === NEW_BUDGET, `实际 ${b2}`)
    const expectRemain = (Number(b2) - Number(u2) - Number(f2)).toFixed(2)
    check('②-3 恒等式成立', Number(rm2).toFixed(2) === expectRemain,
      `remaining=${rm2} 期望 ${expectRemain}（budget ${b2} − used ${u2} − frozen ${f2}）`)
    check('②-4 执行率按 used/budget 重算', Number(er2) >= 0, `execution_rate=${er2}`)

    // ── ③ 负金额被拒（三个入口）──
    console.log('\n【③】负数金额应被拒绝（防「冻结变解冻、消耗变回冲」）')
    for (const [name, ep] of [['冻结', 'freeze'], ['释放', 'release'], ['消耗', 'consume']]) {
      const r = await call('POST',
        `/erp/budget/control/${ep}?budgetId=${BUDGET}&budgetItemId=${ITEM}&amount=-100&sourceType=manual`, null, token)
      const rejected = r.status >= 400 || /必须大于 0/.test(r.body)
      check(`③-${name}负数被拒`, rejected, `HTTP ${r.status} ${r.body.slice(0, 80)}`)
    }
    // 正数对照组（说明接口本身可用，不是被别的原因拦掉）
    const rOk = await call('POST',
      `/erp/budget/control/check?budgetId=${BUDGET}&budgetItemId=${ITEM}&amount=100`, null, token)
    check('③-对照 正数 check 接口正常', rOk.status === 200, `HTTP ${rOk.status}`)
  } finally {
    // 恢复预算额
    await call('PUT', `/erp/budget/item/${ITEM}`,
      { id: Number(ITEM), budgetId: Number(BUDGET), budgetAmount: Number(BUDGET_AMT) }, token)
    const restored = q(`select budget_amount||'|'||used_amount||'|'||frozen_amount||'|'||remaining_amount from budget_item where id=${ITEM}`)
    check('测试数据已恢复', restored === `${BUDGET_AMT}|${USED}|${FROZEN}|${REMAIN}`, `实际 ${restored}`)
  }

  console.log(`\n===== 结果：${pass} 通过 / ${fail} 失败 =====`)
  process.exit(fail > 0 ? 1 : 0)
})().catch(e => { console.error('ERR', e.message); process.exit(1) })
