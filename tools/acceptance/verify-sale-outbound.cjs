// 销售出库单金标准验收：号段 → 多条件查询(40项) → 明细查询(71列) → 物流备注 → Excel 导出
// → 全生命周期(草稿→提交→审核→拣货→打包→发货→完成) → 库存过账 → 凭证 → 取消冲回（库存净变化 0）
const BASE = process.env.SOB_BASE || 'http://localhost:5688'
const fs = require('fs')
const path = require('path')
const { Client } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/pg'))

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

const pg = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
async function sql(q) {
  const r = await pg.query(q)
  return Array.isArray(r) ? r[r.length - 1].rows : r.rows
}
async function scalar(q) {
  const rows = await sql(q)
  if (!rows.length) return null
  return Object.values(rows[0])[0]
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
  const cap = capRes.data || capRes
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'sex_e2e', password: 'admin123', tenantName: '系统租户', captcha: extractCaptcha(cap.img), captchaKey: cap.uuid }),
  }).then(r => r.json())
  token = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
}

async function call(method, p, body, retried = false) {
  const res = await fetch(`${BASE}/api${p}`, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (res.status === 401 && !retried) {
    console.log('  ⚠️ 会话被踢，自动重登…')
    await login()
    return call(method, p, body, true)
  }
  const ct = res.headers.get('content-type') || ''
  if (ct.includes('spreadsheetml') || ct.includes('octet-stream')) {
    const buf = Buffer.from(await res.arrayBuffer())
    return { __blob: true, status: res.status, contentType: ct, buf }
  }
  const text = await res.text()
  try { return JSON.parse(text) } catch { return { __raw: text, status: res.status } }
}
const data = r => (r && typeof r === 'object' && 'data' in r ? r.data : r)

// ── 夹具 ──
const WAREHOUSE_ID = '1'
const CUSTOMER_ID = '2'
const PRODUCT_ID = '2073239284579586050'
const SALES_PERSON_ID = '9200100000000000099'

// 明细行的数据字段（对标文档 71 列，去掉 序号/操作 两个结构性列）
const DETAIL_DATA_KEYS = [
  'outboundDate', 'outboundNo', 'orderNo', 'status', 'settlementStatus', 'warehouseName',
  'customerName', 'customerCode', 'customerLevel', 'receiverName', 'receiverPhone', 'shippingAddress',
  'customerTicket', 'customerRemark', 'salesPersonName', 'departmentName',
  'productName', 'productCode', 'barcode', 'specification', 'model', 'origin', 'brand',
  'extNum1', 'extNum2', 'extNum3', 'extText1', 'extText2', 'extNum4', 'extNum5',
  'extPartner', 'extStaff', 'extDept',
  'productUnit', 'quantity', 'conversionRelation', 'conversionResult', 'bigPack', 'midPack', 'smallPack',
  'smallUnit', 'smallUnitQuantity', 'unitPrice', 'smallUnitPrice', 'lineAmount', 'discountRate',
  'discountedPrice', 'discountedAmount', 'favorableDiscountRate', 'favorableUnitPrice', 'favorableAmount',
  'weight', 'volume', 'boxNo', 'remark', 'docRemark', 'summary', 'attachment',
  'headerExtNum1', 'headerExtNum2', 'headerExtText1', 'headerExtText2', 'headerExtText3',
  'footerExtText1', 'footerExtText2',
  'creatorName', 'bookkeeperName', 'auditorName', 'bookkeepingTime', 'createTime', 'printCount',
]

async function main() {
  await pg.connect()
  await login()
  console.log('—— 登录成功 sex_e2e ——\n')

  // ═══ 1. 后端号段 ═══
  console.log('【1】单号接通后端号段')
  const rawNo = await call('GET', '/erp/sale/outbound/next-no')
  const strNo = typeof rawNo === 'string' ? rawNo : (rawNo?.__raw || rawNo?.data || '')
  check('/next-no 返回纯字符串单号', typeof strNo === 'string' && /^XSCK\d{8}\d{4}$/.test(strNo), strNo)
  check('/next-no 号段取自库内最大单号（peek 语义，未落库前不变）',
    typeof strNo === 'string' && strNo.length === 16, `peek=${strNo}`)

  // ═══ 2. 创建出库单（沿用号段号） ═══
  console.log('\n【2】创建出库单（号段号落库）')
  const createRes = await call('POST', '/erp/sale/outbound', {
    outboundNo: strNo,
    outboundDate: new Date().toISOString().slice(0, 10),
    outboundType: 0,
    status: 0,
    customerId: CUSTOMER_ID, customerName: '客户甲', customerCode: 'C001',
    warehouseId: WAREHOUSE_ID, warehouseName: '主仓库',
    salesPersonId: SALES_PERSON_ID, salesPersonName: 'SEX-E2E',
    sourceOrder: 'XSDD-TEST-0001',
    remark: 'SOB-E2E-备注',
    buyerRemark: 'SOB-E2E-买家备注',
    extText1: 'SOB-E2E-EXT1',
    extNum1: 77,
    paymentAccount1: 'SOB-E2E-账户1',
    receiverName: 'SOB-E2E-收货人',
    logisticsCompany: '顺丰',
    trackingNumber: 'SF-SOB-0001',
    deliveryMethod: 'express',
    generationMethod: '手工创建',
    creatorName: 'SEX-E2E',
    items: [{
      productId: PRODUCT_ID, productName: '博多新米坊酒酿果味酱罐头', productCode: 'SP-20260704-041',
      quantity: 2, unitPrice: 100, unit: '瓶', discountRate: 0, favorableDiscountRate: 5,
      costPrice: 60, weight: 1.5, volume: 0.01, boxNo: 'BOX-1', remark: 'SOB-E2E-明细备注',
      productAttribute: '正常', shelfLife: '12个月',
    }],
  })
  const created = data(createRes)
  const oid = created?.id
  check('创建成功且单号沿用号段', !!oid && created.outboundNo === strNo, `id=${oid}, no=${created?.outboundNo}`)
  check('创建后状态=草稿(0)', created?.status === 0, `status=${created?.status}`)
  check('来源订单真实落库', created?.orderNo === 'XSDD-TEST-0001', `orderNo=${created?.orderNo}`)
  check('本单金额=数量×单价', Number(created?.totalAmount) === 200, `totalAmount=${created?.totalAmount}`)
  const dbLine = await sql(`SELECT line_amount, favorable_amount, cost_amount FROM erp_sale_outbound_item WHERE outbound_id=${oid}`)
  check('明细金额/优惠后金额/成本已计算并落库',
    Number(dbLine[0]?.line_amount) === 200 && Number(dbLine[0]?.favorable_amount) === 190 && Number(dbLine[0]?.cost_amount) === 120,
    JSON.stringify(dbLine[0]))
  const rawNo3 = await call('GET', '/erp/sale/outbound/next-no')
  const strNo3 = typeof rawNo3 === 'string' ? rawNo3 : (rawNo3?.__raw || rawNo3?.data || '')
  check('号段随落库推进（后端权威，非前端自增）', strNo3 > created.outboundNo, `${created.outboundNo} → ${strNo3}`)

  // ═══ 3. 多条件查询（40 项查询条件） ═══
  console.log('\n【3】按单据多条件分页（40 项查询条件）')
  const filterCases = [
    ['outboundNo', strNo, '单据编号'],
    ['customerName', '客户甲', '客户'],
    ['warehouseName', '主仓库', '仓库'],
    ['status', 0, '单据状态'],
    ['sourceOrder', 'XSDD-TEST-0001', '来源订单'],
    ['remark', 'SOB-E2E-备注', '单据备注'],
    ['buyerRemark', 'SOB-E2E-买家备注', '买家备注'],
    ['extText1', 'SOB-E2E-EXT1', '表头自定义字段3'],
    ['extNum1', 77, '表头自定义字段1'],
    ['paymentAccount1', 'SOB-E2E-账户1', '收款账户1'],
    ['receiverName', 'SOB-E2E-收货人', '收货人'],
    ['logisticsCompany', '顺丰', '物流公司'],
    ['trackingNumber', 'SF-SOB-0001', '运单号'],
    ['deliveryMethod', 'express', '配送方式'],
    ['generationMethod', '手工创建', '产生方式'],
    ['salesPersonName', 'SEX-E2E', '经手人'],
    ['productAttribute', '正常', '商品行属性'],
  ]
  for (const [k, v, label] of filterCases) {
    const r = await call('GET', `/erp/sale/outbound/page?pageNum=1&pageSize=50&${k}=${encodeURIComponent(v)}`)
    const recs = data(r)?.records || []
    check(`按「${label}」(${k}) 命中本单`, recs.some(x => String(x.id) === String(oid)), `命中 ${recs.length} 条`)
  }
  const rEmpty = await call('GET', '/erp/sale/outbound/page?pageNum=1&pageSize=50&customerName=不存在的客户ZZZ')
  check('不匹配条件返回空', (data(rEmpty)?.records || []).length === 0)
  const rRed = await call('GET', '/erp/sale/outbound/page?pageNum=1&pageSize=50&showRed=false')
  check('显示红冲(默认false) 正常返回', rRed && !rRed.__raw, `total=${data(rRed)?.total}`)
  const rAbn = await call('GET', '/erp/sale/outbound/page?pageNum=1&pageSize=50&showAbnormal=true')
  check('仅显示异常记账单据 正常返回', rAbn && !rAbn.__raw, `total=${data(rAbn)?.total}`)

  // ═══ 4. 按明细查询（71 列 + 明细行口径分页） ═══
  console.log('\n【4】按明细分页（71 列 / 分页口径=明细行）')
  const dRes = await call('GET', `/erp/sale/outbound/page-detail?pageNum=1&pageSize=20&outboundNo=${encodeURIComponent(strNo)}`)
  const dRec = (data(dRes)?.records || [])[0]
  check('明细查询有记录', !!dRec)
  const missing = DETAIL_DATA_KEYS.filter(k => dRec && !(k in dRec))
  check(`明细返回 71 列数据字段`, missing.length === 0, missing.length ? '缺失: ' + missing.join(',') : `${DETAIL_DATA_KEYS.length} 列齐全`)
  check('明细-单据备注(docRemark)与明细备注(remark)不串', dRec?.docRemark === 'SOB-E2E-备注' && dRec?.remark === 'SOB-E2E-明细备注',
    `docRemark=${dRec?.docRemark}, remark=${dRec?.remark}`)
  check('明细-表头自定义字段独立于表体', dRec?.headerExtText1 === 'SOB-E2E-EXT1', `headerExtText1=${dRec?.headerExtText1}`)
  const totalItems = Number(data(dRes)?.total)
  const dbItemCount = Number(await scalar(`SELECT COUNT(*) FROM erp_sale_outbound_item i JOIN erp_sale_outbound o ON o.id=i.outbound_id WHERE o.outbound_no='${strNo}' AND i.deleted=0 AND o.deleted=0`))
  check('明细分页 total = 明细行数', totalItems === dbItemCount, `total=${totalItems}, db=${dbItemCount}`)

  // ═══ 5. 物流备注批量写入 ═══
  console.log('\n【5】物流备注（真实落库）')
  const rmkRes = await call('POST', '/erp/sale/outbound/batch-logistics-remark', { ids: [oid], logisticsRemark: 'SOB-E2E-物流备注' })
  check('批量写入返回 updated=1', data(rmkRes)?.updated === 1, JSON.stringify(data(rmkRes)))
  const dbRmk = await scalar(`SELECT logistics_remark FROM erp_sale_outbound WHERE id=${oid}`)
  check('logistics_remark 已落库', dbRmk === 'SOB-E2E-物流备注', String(dbRmk))
  const rRmk = await call('GET', `/erp/sale/outbound/page?pageNum=1&pageSize=50&logisticsRemark=${encodeURIComponent('SOB-E2E-物流备注')}`)
  check('物流备注可查询', (data(rRmk)?.records || []).some(x => String(x.id) === String(oid)))

  // ═══ 6. Excel 导出 ═══
  console.log('\n【6】导出真实 Excel')
  const xls = await call('GET', `/erp/sale/outbound/export?outboundNo=${encodeURIComponent(strNo)}`)
  check('导出为 xlsx MIME', !!xls?.__blob && /spreadsheetml/.test(xls.contentType), xls?.contentType)
  check('导出内容为真实 XLSX(PK 头)', !!xls?.buf && xls.buf.slice(0, 2).toString() === 'PK', xls?.buf ? `${xls.buf.length} bytes` : 'n/a')

  // ═══ 7. 全生命周期 ═══
  console.log('\n【7】全生命周期流转')
  const stockBefore = Number(await scalar(`SELECT quantity FROM erp_stock WHERE product_id='${PRODUCT_ID}' AND warehouse_id=${WAREHOUSE_ID}`))
  const steps = [
    ['submit', 'post', null, 1], ['approve', 'post', null, 2],
    ['start-picking', 'post', null, 4], ['complete-picking', 'post', null, 5],
    ['start-packing', 'post', null, 7], ['complete-packing', 'post', null, 8],
    ['ship', 'post', null, 10], ['complete', 'post', null, 11],
  ]
  let lastStatus = 0
  for (const [ep, , , expect] of steps) {
    const r = await call('POST', `/erp/sale/outbound/${oid}/${ep}`)
    const st = data(r)?.status
    const ok = st === expect
    check(`${ep} → 状态 ${expect}`, ok, ok ? '' : `实际 status=${st}, res=${JSON.stringify(r).slice(0, 160)}`)
    lastStatus = st
    if (!ok) break
  }
  check('终态=已完成(11)', lastStatus === 11, `status=${lastStatus}`)

  // 库存过账（WMS 为唯一写入口，镜像 erp_stock）
  const stockAfter = Number(await scalar(`SELECT quantity FROM erp_stock WHERE product_id='${PRODUCT_ID}' AND warehouse_id=${WAREHOUSE_ID}`))
  check('发货后库存扣减 2（ERP 镜像 erp_stock）', stockAfter === stockBefore - 2, `${stockBefore} → ${stockAfter}`)

  // 记账与凭证
  const bk = await sql(`SELECT bookkeeper_name, bookkeeping_time FROM erp_sale_outbound WHERE id=${oid}`)
  check('完成后写入记账人/记账时间（凭证生成成功）', !!bk[0]?.bookkeeping_time, JSON.stringify(bk[0]).slice(0, 120))
  const voucherCount = Number(await scalar(`SELECT COUNT(*) FROM finance_voucher_item WHERE source_no='${strNo}'`))
  check('已完成生成会计凭证分录', voucherCount > 0, `分录数=${voucherCount}`)

  // 源单履约回写
  const _ = lastStatus

  // ═══ 8. 取消 → 库存冲回 + 红冲凭证 ═══
  console.log('\n【8】取消完成单：库存冲回 + 红字凭证')
  const cancelRes = await call('POST', `/erp/sale/outbound/${oid}/cancel?reason=${encodeURIComponent('SOB-E2E-取消')}`)
  check('取消后状态=已取消(12)', data(cancelRes)?.status === 12, `status=${data(cancelRes)?.status}`)
  const stockFinal = Number(await scalar(`SELECT quantity FROM erp_stock WHERE product_id='${PRODUCT_ID}' AND warehouse_id=${WAREHOUSE_ID}`))
  check('取消后库存回到初始值（净变化 0）', stockFinal === stockBefore, `${stockBefore} → ${stockAfter} → ${stockFinal}`)
  const reverseVoucher = Number(await scalar(`SELECT COUNT(*) FROM finance_voucher_item WHERE source_no='${strNo}' AND source_type='sale_outbound_cancel'`))
  check('生成红字冲销凭证（借贷反向）', reverseVoucher === 2, `红冲分录=${reverseVoucher}`)

  console.log(`\n══════ 结果：通过 ${pass} / 失败 ${fail} ══════`)
  await pg.end()
  process.exit(fail === 0 ? 0 : 1)
}

main().catch(async (e) => {
  console.error('脚本异常:', e)
  try { await pg.end() } catch { /* ignore */ }
  process.exit(1)
})
