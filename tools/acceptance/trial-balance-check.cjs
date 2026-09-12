// 科目余额表金标准接口验收（不变量驱动，不依赖固定测试金额）
// A 表结构/四段字段 · B 试算平衡不变量 · C 逐行勾稽 · D 四段口径自洽 · E 过滤（层级/无数据/关键字） · F 跨页勾稽（↔明细账）
const BASE = 'http://localhost:5655'

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

let pass = 0
let fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}
const num = (v) => Number(v || 0)
const near = (a, b) => Math.abs(num(a) - num(b)) < 0.005
const Y = 2026

async function main() {
  const capRes = await fetch(`${BASE}/api/auth/captcha`).then(r => r.json())
  const capData = capRes.data || capRes
  const captcha = extractCaptcha(capData.img)
  const loginRes = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin', password: 'admin123', tenantName: '系统租户',
      captcha, captchaKey: capData.uuid,
    }),
  }).then(r => r.json())
  const token = loginRes?.data?.token || loginRes?.data?.accessToken || loginRes?.data?.tokenValue
  if (!token) { console.log('登录失败:', JSON.stringify(loginRes).slice(0, 300)); process.exit(1) }
  console.log(`登录成功 (验证码 ${captcha})`)

  let currentToken = token
  const login = async () => {
    const cap = await fetch(`${BASE}/api/auth/captcha`).then(r => r.json())
    const cd = cap.data || cap
    const code = extractCaptcha(cd.img)
    const res = await fetch(`${BASE}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cd.uuid }),
    }).then(r => r.json())
    const t = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue
    if (t) currentToken = t
    return t
  }
  // sa-token 单端登录：并行会话共用 admin 会互相踢下线，401 时自愈重登
  const get = async (path, params, retried = false) => {
    const qs = params ? '?' + new URLSearchParams(params).toString() : ''
    const res = await fetch(`${BASE}/api${path}${qs}`, { headers: { Authorization: `Bearer ${currentToken}` } }).then(r => r.json())
    if (res?.code === 401 && !retried) {
      console.log('  ⚠️ 会话被并行会话踢下线，自动重新登录…')
      await login()
      return get(path, params, true)
    }
    if (res?.code !== 200) throw new Error(`${path} 返回 code=${res?.code} ${res?.message || ''}`)
    return res?.data
  }
  const tb = (p) => get('/erp/finance/report/v2/trial-balance-page', p)

  // ═══ A. 表结构 / 四段字段 ═══
  console.log('\n[A] 表结构与四段字段')
  const r1 = await tb({ fiscalYear: Y, startPeriod: 9, endPeriod: 9, subjectLevel: 1 })
  if (!r1 || !Array.isArray(r1.records)) { console.log('❌ 接口无数据:', JSON.stringify(r1).slice(0, 300)); process.exit(1) }
  const byCode = Object.fromEntries(r1.records.map(r => [r.subjectCode, r]))
  check('科目行非空且科目编码唯一', r1.records.length > 0 && new Set(r1.records.map(r => r.subjectCode)).size === r1.records.length,
    `${r1.records.length} 行 / total=${r1.total}`)
  check('科目类型名称齐备', r1.records.every(r => !!r.subjectTypeName),
    [...new Set(r1.records.map(r => r.subjectTypeName))].join('/'))
  check('层级过滤生效（全部 level=1）', r1.records.every(r => r.level === 1))
  check('四段字段齐全（期初/本期借贷/累计借贷/期末）', r1.records.every(r =>
    r.openingBalance !== undefined && r.periodDebit !== undefined && r.periodCredit !== undefined
    && r.yearDebit !== undefined && r.yearCredit !== undefined && r.closingBalance !== undefined))
  check('余额方向字段为 借/贷', r1.records.every(r => ['借', '贷'].includes(r.openingDirection) && ['借', '贷'].includes(r.closingDirection)))

  // ═══ B. 试算平衡不变量（P0 红线，与数据量无关） ═══
  console.log('\n[B] 试算平衡不变量')
  const s1 = r1.summary || {}
  check('Σ本期借方 = Σ本期贷方', near(s1.periodDebit, s1.periodCredit) && s1.periodBalanced === true, `${s1.periodDebit} / ${s1.periodCredit}`)
  check('Σ本年累计借方 = Σ本年累计贷方', near(s1.yearDebit, s1.yearCredit) && s1.yearBalanced === true, `${s1.yearDebit} / ${s1.yearCredit}`)
  check('Σ期初借 = Σ期初贷', near(s1.openingDebit, s1.openingCredit), `${s1.openingDebit} / ${s1.openingCredit}`)
  check('Σ期末借 = Σ期末贷', near(s1.closingDebit, s1.closingCredit), `${s1.closingDebit} / ${s1.closingCredit}`)
  const netClosing = r1.records.reduce((a, r) => a + num(r.closingBalance), 0)
  check('Σ期末净额 = 0（顶级科目录入不重不漏）', near(netClosing, 0), `Σ净额=${netClosing.toFixed(2)}`)

  // ═══ C. 逐行勾稽 ═══
  console.log('\n[C] 逐行勾稽 期末 = 期初 + 本期借 − 本期贷')
  const badRows = r1.records.filter(r => !near(num(r.openingBalance) + num(r.periodDebit) - num(r.periodCredit), num(r.closingBalance)))
  check('全部行勾稽一致', badRows.length === 0, badRows.slice(0, 3).map(r => `${r.subjectCode}:${r.openingBalance}+${r.periodDebit}-${r.periodCredit}≠${r.closingBalance}`).join(' | ') || '逐行一致')

  // ═══ D. 四段口径自洽 ═══
  console.log('\n[D] 四段口径自洽')
  const rPrev = await tb({ fiscalYear: Y, startPeriod: 1, endPeriod: 8, subjectLevel: 1, showNoData: true })
  const rPrevRows = Object.fromEntries((rPrev.records || []).map(r => [r.subjectCode, r]))
  const rAll = await tb({ fiscalYear: Y, startPeriod: 1, endPeriod: 9, subjectLevel: 1, showNoData: true })
  const rAllRows = Object.fromEntries((rAll.records || []).map(r => [r.subjectCode, r]))
  const rCurRows = Object.fromEntries((await tb({ fiscalYear: Y, startPeriod: 9, endPeriod: 9, subjectLevel: 1, showNoData: true })).records.map(r => [r.subjectCode, r]))

  check('累计口径与起月无关：9 月止月的 yearDebit/YearCredit 在两种起始月下一致',
    Object.keys(rCurRows).every(c => near(rCurRows[c].yearDebit, rAllRows[c]?.yearDebit) && near(rCurRows[c].yearCredit, rAllRows[c]?.yearCredit)),
    `样例 ${Object.keys(rCurRows)[0]}: ${rCurRows[Object.keys(rCurRows)[0]]?.yearDebit} vs ${rAllRows[Object.keys(rCurRows)[0]]?.yearDebit}`)
  check('1~8 月期末 = 9 月期初（期初为会计月起时点）',
    Object.keys(rCurRows).every(c => near(rPrevRows[c]?.closingBalance, rCurRows[c]?.openingBalance)),
    '逐科目比对一致')
  check('全区间(1~9)合计仍平衡', rAll.summary?.periodBalanced === true && rAll.summary?.yearBalanced === true,
    `本期 ${rAll.summary?.periodDebit}/${rAll.summary?.periodCredit} 累计 ${rAll.summary?.yearDebit}/${rAll.summary?.yearCredit}`)

  // ═══ E. 过滤：层级 / 无数据 / 关键字 ═══
  console.log('\n[E] 过滤（层级 / 显示无数据 / 科目关键字）')
  const hidden = await tb({ fiscalYear: Y, startPeriod: 9, endPeriod: 9, subjectLevel: 1, showNoData: false })
  const shown = await tb({ fiscalYear: Y, startPeriod: 9, endPeriod: 9, subjectLevel: 1, showNoData: true })
  check('勾选后行数增加（含零发生额科目）', shown.records.length > hidden.records.length, `${hidden.records.length} → ${shown.records.length}`)
  check('勾选后合计不变量不变', near(shown.summary?.periodDebit, hidden.summary?.periodDebit) && near(shown.summary?.closingDebit, hidden.summary?.closingDebit))

  const lv2 = await tb({ fiscalYear: Y, startPeriod: 9, endPeriod: 9, subjectLevel: 2, showNoData: true })
  check('层级 2 行数 ≥ 层级 1', lv2.records.length >= shown.records.length, `${shown.records.length} → ${lv2.records.length}`)
  check('层级 2 含下级科目（level=2 存在或有 hasChildren=true 的父级）',
    lv2.records.some(r => r.level === 2) || lv2.records.some(r => r.hasChildren === true))
  check('父子同屏合计不重复累加', near(lv2.summary?.periodDebit, shown.summary?.periodDebit) && near(lv2.summary?.yearDebit, shown.summary?.yearDebit),
    `本期 ${lv2.summary?.periodDebit}/${shown.summary?.periodDebit} 累计 ${lv2.summary?.yearDebit}/${shown.summary?.yearDebit}`)

  const kw = await tb({ fiscalYear: Y, startPeriod: 9, endPeriod: 9, subjectLevel: 2, subjectKeyword: '库存', showNoData: true })
  const kwCodes = (kw.records || []).map(r => r.subjectCode)
  check('关键字「库存」命中且含下级', kwCodes.length > 0 && (await tb({ fiscalYear: Y, startPeriod: 9, endPeriod: 9, subjectLevel: 2, showNoData: true, subjectKeyword: '库存现金' })).records.length >= 1,
    kwCodes.join(','))

  // ═══ F. 跨页勾稽（科目余额表 ↔ 明细账，同源凭证分录） ═══
  console.log('\n[F] 跨页勾稽 ↔ 明细账')
  const dlAll = await get('/erp/finance/ledger/detail-page', { dateStart: `${Y}-09-01`, dateEnd: `${Y}-09-30`, pageNum: 1, pageSize: 500 })
  check('全科目：科目余额表 Σ期末净额 = 明细账期末余额', near(netClosing, num(dlAll?.closingBalance)),
    `TB Σ净额=${netClosing.toFixed(2)} / 明细账期末=${dlAll?.closingBalance}`)
  check('明细账自身勾稽 期末 = 期初 + Σ借 − Σ贷',
    near(num(dlAll?.openingBalance) + num(dlAll?.totalDebit) - num(dlAll?.totalCredit), num(dlAll?.closingBalance)),
    `${dlAll?.openingBalance} + ${dlAll?.totalDebit} − ${dlAll?.totalCredit} = ${dlAll?.closingBalance}`)

  // 逐科目：用科目树取 subjectId（含下级），与科目余额表 roll-up 口径比对
  const tree = await get('/erp/finance/ledger/subject-tree', {})
  const idByCode = {}
  const walk = (nodes) => (nodes || []).forEach(n => {
    if (n.subjectId) {
      const code = String(n.categoryName || '').split(' ')[0]
      idByCode[code] = n.subjectId
    }
    walk(n.children)
  })
  walk(tree)
  const sample = r1.records.filter(r => num(r.closingBalance) !== 0).slice(0, 3)
  for (const row of sample) {
    const sid = idByCode[row.subjectCode]
    if (!sid) { console.log(`  ⚠️ 科目树未找到 ${row.subjectCode}，跳过`); continue }
    const dlSub = await get('/erp/finance/ledger/detail-page', { subjectId: sid, dateStart: `${Y}-09-01`, dateEnd: `${Y}-09-30`, pageNum: 1, pageSize: 500 })
    check(`${row.subjectCode} 期末一致（科目余额表 vs 明细账）`, near(num(row.closingBalance), num(dlSub?.closingBalance)),
      `TB=${row.closingBalance} 明细账=${dlSub?.closingBalance}`)
  }

  await auxCrossCheck(get, tb, Y, check, num, near)

  console.log(`\n结果：通过 ${pass} 项，失败 ${fail} 项`)
  process.exit(fail === 0 ? 0 : 1)
}

/** [G] 辅助核算余额表对照：仅统计挂了核算项的科目，其核算项期末净额是该科目期末的子集 */
async function auxCrossCheck(get, tb, Y, check, num, near) {
  console.log('\n[G] 跨页对照 ↔ 辅助核算余额表（按核算项）')
  const aux = await get('/erp/finance/auxiliary/balance/page', { fiscalYear: Y, startPeriod: 9, endPeriod: 9, pageNum: 1, pageSize: 200 })
  const rows = aux?.records || []
  if (!rows.length) {
    console.log('  ⚠️ 辅助核算余额表无数据，跳过对照')
    return
  }
  const tbAll = await tb({ fiscalYear: Y, startPeriod: 9, endPeriod: 9, subjectLevel: 1, showNoData: true })
  const tbMap = Object.fromEntries((tbAll.records || []).map(r => [r.subjectCode, r]))
  const auxNet = {}
  rows.forEach(r => {
    auxNet[r.subjectCode] = (auxNet[r.subjectCode] || 0) + (num(r.endDebit) - num(r.endCredit))
  })
  let eq = 0
  for (const [code, net] of Object.entries(auxNet)) {
    const tbRow = tbMap[code]
    const same = tbRow && near(net, tbRow.closingBalance)
    const dirOk = tbRow && Math.sign(net) === Math.sign(num(tbRow.closingBalance))
    if (same) eq++
    console.log(`  ${same ? '✅' : 'ℹ️'} ${code} ${tbRow?.subjectName || ''}：核算项期末净额 ${net.toFixed(2)} / 科目余额表期末 ${tbRow?.closingBalance ?? 'N/A'}${same ? '（完全一致）' : (dirOk ? '（方向一致，科目含未挂核算项的分录）' : '⚠️ 方向不一致')}`)
  }
  check('挂核算项科目的方向与科目余额表一致', Object.entries(auxNet).every(([code, net]) => {
    const tbRow = tbMap[code]
    return tbRow && Math.sign(net) === Math.sign(num(tbRow.closingBalance))
  }))
  // 注：辅助核算余额表按核算项列示，借贷两侧核算维度不同（如 借1122客户/贷6001无核算项），
  //     故其表级 Σ借 ≠ Σ贷 属正常；此处只校验逐行勾稽（与科目余额表同源凭证，口径一致）。
  const badAux = rows.filter(r => !near(num(r.beginDebit) - num(r.beginCredit) + num(r.periodDebit) - num(r.periodCredit), num(r.endDebit) - num(r.endCredit)))
  check('辅助核算余额表逐行勾稽 期末 = 期初 + 本期借 − 本期贷', badAux.length === 0,
    badAux.slice(0, 2).map(r => `${r.subjectCode}/${r.auxName}`).join(' | ') || '逐行一致')
  check('至少 1 个科目核算项净额与科目余额表期末完全一致', eq >= 1, `一致科目数 ${eq}/${Object.keys(auxNet).length}`)
}

main().catch(e => { console.error('验收异常:', e); process.exit(1) })
