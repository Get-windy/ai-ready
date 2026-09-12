// 销售单据查询页金标准验收
// 覆盖：4 类单据查询 / 分页完整性 / 成本毛利真实聚合 / 整单备注落库 / 查询条件生效 / 无 500
const BASE = process.env.BASE || 'http://localhost:5655'
const path = require('path')
const { Client } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/pg'))

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

let token = ''
async function login() {
  const capRes = await fetch(`${BASE}/api/auth/captcha`).then(r => r.json())
  const capData = capRes.data || capRes
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: extractCaptcha(capData.img), captchaKey: capData.uuid }),
  }).then(r => r.json())
  const t = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue
  if (!t) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  token = t
}

async function api(method, p, body, retried = false) {
  const r = await fetch(`${BASE}/api${p}`, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await r.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON 响应 */ }
  if ((r.status === 401 || json?.code === 401) && !retried) {
    console.log('  ⚠️ 会话被踢，自动重登…')
    await login()
    return api(method, p, body, true)
  }
  return { status: r.status, json, text }
}

async function page(query) {
  const qs = new URLSearchParams(query).toString()
  const res = await api('GET', `/sales/doc-query/page?${qs}`)
  const data = res.json?.data || res.json
  return { status: res.status, total: Number(data?.total) || 0, records: data?.records || [], raw: res }
}

;(async () => {
  await login()
  const db = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
  await db.connect()

  // ═══ 1. 四类单据均可查询（无 500） ═══
  console.log('\n[1] 四类单据查询')
  const types = ['SALE_ORDER', 'OUTBOUND', 'RETURN', 'EXCHANGE']
  const typeCounts = {}
  for (const t of types) {
    const r = await page({ current: 1, size: 20, documentType: t })
    typeCounts[t] = r.total
    check(`${t} 查询返回 200`, r.status === 200, `total=${r.total}`)
  }
  check('销售订单有数据', typeCounts.SALE_ORDER >= 1, `total=${typeCounts.SALE_ORDER}`)
  check('销售出库单有数据', typeCounts.OUTBOUND >= 1, `total=${typeCounts.OUTBOUND}`)
  check('销售退货单有数据', typeCounts.RETURN >= 1, `total=${typeCounts.RETURN}`)
  check('销售换货单有数据（此前实体列缺失导致 500）', typeCounts.EXCHANGE >= 1, `total=${typeCounts.EXCHANGE}`)

  // ═══ 2. 成本金额 / 毛利真实聚合 ═══
  console.log('\n[2] 成本金额 / 毛利（来自明细真实聚合）')
  const orderPage = await page({ current: 1, size: 50, documentType: 'SALE_ORDER' })
  const orderRow = orderPage.records.find(r => r.documentNo === 'ZZT-QO-001')
  check('订单成本金额 = Σ明细 cost_amount (120+80)', orderRow?.costAmount === 200, `实际=${orderRow?.costAmount}`)
  check('订单毛利 = Σ明细 gross_profit (80+60)', orderRow?.grossProfit === 140, `实际=${orderRow?.grossProfit}`)

  const returnPage = await page({ current: 1, size: 50, documentType: 'RETURN' })
  const returnRow = returnPage.records.find(r => r.documentNo === 'ZZT-RET-001')
  check('退货单成本金额 = Σ明细 ref_cost_amount (60)', returnRow?.costAmount === 60, `实际=${returnRow?.costAmount}`)
  check('退货单毛利 = 本单金额 - 成本 (100-60=40)', returnRow?.grossProfit === 40, `实际=${returnRow?.grossProfit}`)

  const exchangePage = await page({ current: 1, size: 50, documentType: 'EXCHANGE' })
  const exchangeRow = exchangePage.records.find(r => r.documentNo === 'ZZTEST-HH-001')
  check('换货单成本金额 = Σ明细 cost_amount (40)', exchangeRow?.costAmount === 40, `实际=${exchangeRow?.costAmount}`)
  check('换货单毛利 = 本单金额 - 成本 (100-40=60)', exchangeRow?.grossProfit === 60, `实际=${exchangeRow?.grossProfit}`)

  const outboundPage = await page({ current: 1, size: 50, documentType: 'OUTBOUND' })
  const outboundRow = outboundPage.records.find(r => r.costAmount !== null && r.costAmount !== undefined)
  if (outboundRow) {
    const agg = await db.query(
      `SELECT COALESCE(SUM(cost_amount),0) c, COALESCE(SUM(gross_profit),0) p FROM erp_sale_outbound_item WHERE outbound_id=$1`,
      [outboundRow.id])
    const c = Number(agg.rows[0].c), p = Number(agg.rows[0].p)
    check('出库单成本金额与明细聚合一致', Number(outboundRow.costAmount) === c, `接口=${outboundRow.costAmount} DB=${c}`)
    check('出库单毛利与明细聚合一致', Number(outboundRow.grossProfit) === p, `接口=${outboundRow.grossProfit} DB=${p}`)
  } else {
    check('出库单成本/毛利已回填（非 null）', false, '所有出库单成本均为 null')
  }

  // ═══ 3. 字段口径 ═══
  console.log('\n[3] 字段口径')
  check('结算状态由已结算金额派生（部分结算）', orderRow?.settlementStatus === 'PARTIAL_PAID', `实际=${orderRow?.settlementStatus}`)
  check('出库单结算状态非空', outboundPage.records[0]?.settlementStatus != null, `实际=${outboundPage.records[0]?.settlementStatus}`)
  check('换货单出/入库仓库已映射', !!exchangeRow?.inboundWarehouse && !!exchangeRow?.outboundWarehouse,
    `入库=${exchangeRow?.inboundWarehouse} 出库=${exchangeRow?.outboundWarehouse}`)
  check('换货单经手人/部门已映射', exchangeRow?.handlerName === '张三' && exchangeRow?.departmentName === '销售部',
    `经手人=${exchangeRow?.handlerName} 部门=${exchangeRow?.departmentName}`)
  check('换货单结算状态已映射', !!exchangeRow?.settlementStatus, `实际=${exchangeRow?.settlementStatus}`)
  check('订单运费承担方已映射', orderRow?.freightPayer === '卖方', `实际=${orderRow?.freightPayer}`)
  check('订单积分抵扣已映射(usedPoints)', Number(orderRow?.pointsDeduction) === 4, `实际=${orderRow?.pointsDeduction}`)
  check('订单制单时间非空(createTime)', !!orderRow?.createTime, `实际=${orderRow?.createTime}`)
  check('退货单记账人字段可用', returnRow?.bookkeeperName !== undefined, `实际=${returnRow?.bookkeeperName}`)

  // ═══ 4. 分页完整性（无重复 / 无遗漏） ═══
  console.log('\n[4] 分页完整性')
  const allPage = await page({ current: 1, size: 1 })
  const totalAll = allPage.total
  const collected = []
  for (let i = 1; i <= Math.min(totalAll, 12); i++) {
    const p = await page({ current: i, size: 1 })
    if (p.records[0]) collected.push(`${p.records[0].documentType}:${p.records[0].id}`)
  }
  const unique = new Set(collected)
  check('逐页取数无重复', unique.size === collected.length, `采集=${collected.length} 去重=${unique.size}`)
  check('逐页取数与 total 一致', collected.length === Math.min(totalAll, 12), `total=${totalAll}`)

  // ═══ 5. 查询条件生效 ═══
  console.log('\n[5] 查询条件')
  const byNo = await page({ current: 1, size: 20, documentNo: 'ZZT-RET' })
  check('单据编号模糊查询', byNo.total === 1 && byNo.records[0]?.documentNo === 'ZZT-RET-001', `total=${byNo.total}`)

  const byCustomer = await page({ current: 1, size: 20, documentType: 'RETURN', customerName: '验收测试客户' })
  check('客户名称模糊查询', byCustomer.total >= 1, `total=${byCustomer.total}`)

  const hitDate = await page({ current: 1, size: 20, documentType: 'RETURN', dateType: 'documentDate', startDate: '2026-09-03', endDate: '2026-09-03' })
  check('单据日期区间命中（含当天）', hitDate.total === 1, `total=${hitDate.total}`)

  const missDate = await page({ current: 1, size: 20, documentType: 'RETURN', dateType: 'documentDate', startDate: '2026-09-04', endDate: '2026-09-04' })
  check('单据日期区间排除非命中日', missDate.total === 0, `total=${missDate.total}`)

  const byAttr = await page({ current: 1, size: 20, documentType: 'SALE_ORDER', productAttribute: 'GIFT' })
  check('商品行属性过滤（明细 EXISTS）', byAttr.total === 1, `total=${byAttr.total}`)
  const byAttrNone = await page({ current: 1, size: 20, documentType: 'SALE_ORDER', productAttribute: 'COMBO' })
  check('商品行属性过滤排除不匹配', byAttrNone.total === 0, `total=${byAttrNone.total}`)

  const byExt = await page({ current: 1, size: 20, documentType: 'SALE_ORDER', extText1: 'EXT-T1' })
  check('表头自定义文本字段过滤', byExt.total === 1, `total=${byExt.total}`)

  const byExtNum = await page({ current: 1, size: 20, documentType: 'SALE_ORDER', extNum1Min: 11, extNum1Max: 11 })
  check('表头自定义数字区间过滤', byExtNum.total === 1, `total=${byExtNum.total}`)

  const byAmount = await page({ current: 1, size: 20, documentType: 'SALE_ORDER', minTotalAmount: 150, maxTotalAmount: 250 })
  check('本单金额区间过滤', byAmount.total >= 1, `total=${byAmount.total}`)

  const byWarehouse = await page({ current: 1, size: 20, documentType: 'RETURN', warehouseName: '主仓库' })
  check('仓库名称过滤（返回行全部命中该仓库）',
    byWarehouse.total >= 1 && byWarehouse.records.every(r => (r.inboundWarehouse || '').includes('主仓库')),
    `total=${byWarehouse.total}`)

  const redHidden = await page({ current: 1, size: 20, documentType: 'SALE_ORDER' })
  const redShown = await page({ current: 1, size: 20, documentType: 'SALE_ORDER', showRed: true })
  check('默认隐藏已取消单据（订单 status=6）', redShown.total > redHidden.total,
    `默认=${redHidden.total} 显示红冲=${redShown.total}`)
  check('勾选显示红冲后可见已取消单据', redShown.total - redHidden.total >= 1, `差额=${redShown.total - redHidden.total}`)
  const partShipped = redHidden.records.find(r => r.documentNo === 'QO202607232493')
  check('部分发货(status=3)订单不再被误判为红冲', !!partShipped, `可见=${!!partShipped}`)

  // ═══ 6. 整单备注落库（此前前端 URL 与后端路由不一致 → 404） ═══
  console.log('\n[6] 整单备注落库')
  const noteTargets = [
    { docType: 'SALE_ORDER', id: orderRow?.id, table: 'erp_sale_order', key: 'id' },
    { docType: 'OUTBOUND', id: outboundPage.records[0]?.id, table: 'erp_sale_outbound', key: 'id' },
    { docType: 'RETURN', id: returnRow?.id, table: 'erp_sale_return_doc', key: 'id' },
    { docType: 'EXCHANGE', id: exchangeRow?.id, table: 'erp_sale_exchange', key: 'id' },
  ]
  for (const t of noteTargets) {
    if (!t.id) { check(`${t.docType} 备注保存`, false, '缺少夹具单据'); continue }
    const note = `验收备注-${t.docType}-${Date.now()}`
    const res = await api('PUT', `/sales/doc-query/${t.docType}/${t.id}/remark`, { remark: note })
    const ok200 = res.status === 200 && (res.json?.data?.success ?? res.json?.success) === true
    check(`${t.docType} 备注接口返回成功（无 404）`, ok200, `status=${res.status} body=${res.text.slice(0, 120)}`)
    const rs = await db.query(`SELECT remark FROM ${t.table} WHERE ${t.key}=$1`, [t.id])
    check(`${t.docType} 备注已落库`, rs.rows[0]?.remark === note, `DB=${rs.rows[0]?.remark}`)
  }

  const notExist = await api('PUT', '/sales/doc-query/SALE_ORDER/1/remark', { remark: 'x' })
  check('不存在的单据返回失败而非 500', notExist.status === 200 && (notExist.json?.data?.success ?? notExist.json?.success) === false,
    `status=${notExist.status}`)

  const tooLong = await api('PUT', `/sales/doc-query/${'SALE_ORDER'}/${orderRow?.id}/remark`, { remark: 'x'.repeat(600) })
  check('超长备注被拒绝', tooLong.status === 200 && (tooLong.json?.data?.success ?? tooLong.json?.success) === false,
    `status=${tooLong.status}`)

  const badType = await api('PUT', `/sales/doc-query/UNKNOWN/${orderRow?.id}/remark`, { remark: 'x' })
  check('非法单据类型返回错误（非 500 静默）', badType.status >= 400, `status=${badType.status}`)

  // ═══ 7. 备注回读（列表展示一致） ═══
  console.log('\n[7] 备注回读')
  const after = await page({ current: 1, size: 50, documentType: 'SALE_ORDER' })
  const afterRow = after.records.find(r => r.id === orderRow?.id)
  check('列表返回的备注与落库一致', !!afterRow?.remark && afterRow.remark.startsWith('验收备注-'), `实际=${afterRow?.remark}`)

  await db.end()
  console.log(`\n═══ 验收结果：通过 ${pass} / 失败 ${fail} ═══`)
  process.exit(fail > 0 ? 1 : 0)
})().catch(e => { console.error('FATAL', e); process.exit(1) })
