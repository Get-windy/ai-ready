// 零售单金标准端到端验收：号段 → 建单 → 结算(支付落库 + 库存扣减 + 会员积分) → 挂单/取单 → 作废
// 另覆盖：按单据/按明细多条件查询（经手人/部门/仓库/商品行属性）、打印计数、收款不足与重复结算防护
// 不变量驱动：库存净变化、支付明细与收款金额一致、实收=应收+找零
const BASE = process.env.RETAIL_BASE || 'http://localhost:5655'
const { execFileSync } = require('child_process')
const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe'
const PGENV = { ...process.env, PGPASSWORD: 'devuser123' }

let pass = 0, fail = 0
const failures = []
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  \u2705 ${name}${detail ? ' \u2014 ' + detail : ''}`) }
  else { fail++; failures.push(name); console.log(`  \u274c ${name}${detail ? ' \u2014 ' + detail : ''}`) }
}

function sqlText(q) {
  return execFileSync(PSQL, ['-h', 'localhost', '-p', '5432', '-U', 'devuser', '-d', 'devdb', '-t', '-A', '-F', '|', '-c', q],
    { env: PGENV, encoding: 'utf8' }).trim()
}
function sqlRows(q) {
  const out = sqlText(q)
  return out ? out.split(/\r?\n/).map(l => l.split('|')) : []
}

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

let currentToken = ''
const USER = 'retail_e2e'
async function login() {
  const capRes = await fetch(`${BASE}/api/auth/captcha`).then(r => r.json())
  const capData = capRes.data || capRes
  const captcha = extractCaptcha(capData.img)
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: USER, password: 'admin123', tenantName: '\u7cfb\u7edf\u79df\u6237', captcha, captchaKey: capData.uuid }),
  }).then(r => r.json())
  const token = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue
  if (!token) throw new Error('\u767b\u5f55\u5931\u8d25: ' + JSON.stringify(res).slice(0, 400))
  currentToken = token
}

async function call(method, path, body, retried = false) {
  const res = await fetch(`${BASE}/api${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${currentToken}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  const contentType = res.headers.get('content-type') || ''
  if (text && !contentType.includes('json')) {
    json = text // 裸 String 端点（如 /next-no）直接返回 text/plain
  } else {
    try { json = text ? JSON.parse(text) : null } catch { json = { raw: text } }
  }
  if (json?.code === 401 && !retried) {
    console.log('  \u26a0\ufe0f  \u4f1a\u8bdd\u88ab\u8e22\uff0c\u81ea\u52a8\u91cd\u767b\u2026')
    await login()
    return call(method, path, body, true)
  }
  return { status: res.status, body: json }
}

// ── 固定夹具（devdb 现有主数据；雪花ID以字符串传递避免精度丢失）──
const PRODUCT_ID = '2073239284579586050'
const PRODUCT_CODE = 'SP-20260704-041'
const PRODUCT_NAME = '\u535a\u591a\u65b0\u7c73\u574a\u9152\u917f\u679c\u5473\u9171\u7f50\u5934'
const WAREHOUSE_ID = 1
const CUSTOMER_ID = 2
const QTY = 2
const PRICE = 50
const PAYABLE = QTY * PRICE
const TODAY = new Date().toISOString().slice(0, 10)

function stockOf() {
  const rows = sqlRows(`SELECT COALESCE(quantity,0) FROM erp_stock WHERE product_id=${PRODUCT_ID} AND warehouse_id=${WAREHOUSE_ID} AND deleted=0 LIMIT 1`)
  return rows.length ? Number(rows[0][0]) : null
}
function partyPoints() {
  const rows = sqlRows(`SELECT COALESCE(points,0) FROM biz_party WHERE id=${CUSTOMER_ID}`)
  return rows.length ? Number(rows[0][0]) : null
}

function buildOrder(overrides = {}) {
  return {
    order: {
      orderDate: TODAY,
      warehouseId: WAREHOUSE_ID,
      warehouseName: '\u4e3b\u4ed3\u5e93',
      customerId: CUSTOMER_ID,
      customerName: '\u5ba2\u6237\u7532',
      handlerId: 1,
      handlerName: 'admin',
      departmentId: 1,
      departmentName: '\u603b\u7ecf\u7406\u529e\u516c\u5ba4',
      saleType: 'NORMAL',
      status: 0,
      directDiscount: 0,
      payableAmount: PAYABLE,
      remark: '\u91d1\u6807\u51c6\u9a8c\u6536',
      createdForTest: true,
      ...overrides,
    },
    items: [{
      productId: PRODUCT_ID,
      productCode: PRODUCT_CODE,
      productName: PRODUCT_NAME,
      quantity: QTY,
      unitPrice: PRICE,
      amount: PAYABLE,
      productAttribute: 'POS\u91d1\u6807\u5c5e\u6027',
    }],
  }
}

;(async () => {
  console.log('\u2550\u2550\u2550 \u96f6\u552e\u5355\u91d1\u6807\u51c6\u7aef\u5230\u7aef\u9a8c\u6536 \u2550\u2550\u2550')
  await login()
  console.log(`* \u767b\u5f55\u6210\u529f: ${USER}`)

  // ── 1. 号段 ──
  const n1 = await call('GET', '/sales/retail/next-no')
  const n2 = await call('GET', '/sales/retail/next-no')
  const pickNo = (b) => (typeof b === 'string' ? b : (b?.data ?? b?.raw ?? ''))
  const no1 = String(pickNo(n1.body) || '').trim()
  const no2 = String(pickNo(n2.body) || '').trim()
  check('\u53f7\u6bb5\u63a5\u53e3\u8fd4\u56de\u5355\u53f7', !!no1, `${no1} / ${no2}`)
  check('\u5355\u53f7\u683c\u5f0f LS-yyyyMMdd-NNNN', /^LS-\d{8}-\d{4}$/.test(no1), no1)
  check('\u53f7\u6bb5\u9012\u589e\u4e0d\u91cd\u590d', !!no1 && !!no2 && no1 !== no2,
    `seq ${Number(no1.slice(-4))} \u2192 ${Number(no2.slice(-4))}`)

  // ── 2. 建单（草稿）──
  const stockBefore = stockOf()
  const pointsBefore = partyPoints()
  const createRes = await call('POST', '/sales/retail', buildOrder())
  const order = createRes.body?.order || createRes.body
  const orderId = order?.id
  check('\u5efa\u5355\u6210\u529f\u4e14\u8fd4\u56deID', !!orderId, `id=${orderId}`)
  check('\u4fdd\u5b58\u5355\u53f7\u4e0e\u53f7\u6bb5\u540c\u6e90', /^LS-\d{8}-\d{4}$/.test(order?.retailNo || ''), order?.retailNo)
  check('\u8349\u7a3f\u72b6\u6001\u4e0d\u6263\u5e93\u5b58', stockOf() === stockBefore, `stock=${stockOf()}`)

  // ── 3. 收款不足防护 ──
  const under = await call('POST', `/sales/retail/${orderId}/settle`, { payments: [{ paymentMethod: 'CASH', paymentAmount: 1 }] })
  check('\u6536\u6b3e\u4e0d\u8db3\u62d2\u7edd\u7ed3\u7b97', !!(under.body?.code && under.body.code !== 200) || under.body?.message, under.body?.message)

  // ── 4. 组合结算（现金 + 微信）应收100：现金80 + 微信30 → 找零10 ──
  const settleRes = await call('POST', `/sales/retail/${orderId}/settle`, {
    payments: [
      { paymentMethod: 'CASH', paymentAmount: 80 },
      { paymentMethod: 'WECHAT', paymentAmount: 30, transactionNo: 'WX-E2E-001' },
    ],
  })
  const settled = settleRes.body?.order || settleRes.body
  check('\u7ed3\u7b97\u6210\u529f\u72b6\u6001=\u5df2\u5b8c\u6210', settled?.status === 1, `status=${settled?.status}`)
  check('\u7ec4\u5408\u6536\u6b3e\u6807\u8bb0\u751f\u6548', settled?.combinedPayment === true)
  check('\u652f\u4ed8\u65b9\u5f0f=MIXED', settled?.paymentMethod === 'MIXED', settled?.paymentMethod)
  check('\u5b9e\u6536\u5408\u8ba1=110', Number(settled?.totalReceived) === 110, `totalReceived=${settled?.totalReceived}`)
  check('\u627e\u96f6\u7531\u540e\u7aef\u8ba1\u7b97=10', Number(settled?.changeAmount) === 10, `changeAmount=${settled?.changeAmount}`)
  check('\u73b0\u91d1\u6c47\u603b\u5199\u5165=80', Number(settled?.cashAmount) === 80)
  check('\u5fae\u4fe1\u6c47\u603b\u5199\u5165=30', Number(settled?.wechatAmount) === 30)

  // 支付明细落库
  const payRows = sqlRows(`SELECT payment_method, payment_amount FROM erp_retail_order_payment WHERE order_id=${orderId} AND deleted=0 ORDER BY payment_method`)
  check('\u652f\u4ed8\u660e\u7ec6\u843d\u5e93 2 \u884c', payRows.length === 2, JSON.stringify(payRows))
  const detail = await call('GET', `/sales/retail/${orderId}`)
  check('\u8be6\u60c5\u8fd4\u56de\u652f\u4ed8\u660e\u7ec6', (detail.body?.payments || []).length === 2)

  // ── 5. 库存扣减 ──
  check('\u7ed3\u7b97\u540e\u5e93\u5b58\u51cf\u5c11\u9500\u552e\u6570\u91cf', stockOf() === stockBefore - QTY, `before=${stockBefore} after=${stockOf()} expect=${stockBefore - QTY}`)

  // ── 6. 会员积分闭环 ──
  const pointsAfter = partyPoints()
  check('\u4f1a\u5458\u79ef\u5206\u589e\u52a0 floor(\u5e94\u6536)=100', pointsAfter === pointsBefore + Math.floor(PAYABLE), `before=${pointsBefore} after=${pointsAfter}`)
  check('\u5355\u636e\u8bb0\u5f55\u5f53\u524d\u79ef\u5206', Number(settled?.currentPoints) === pointsAfter, `currentPoints=${settled?.currentPoints}`)
  check('\u8bb0\u8d26\u4eba/\u8bb0\u8d26\u65f6\u95f4\u5df2\u5199\u5165', !!settled?.bookkeepingTime)

  // ── 7. 重复结算防护（同号段再来一单：先结算已完成的单）──
  const again = await call('POST', `/sales/retail/${orderId}/settle`, { payments: [{ paymentMethod: 'CASH', paymentAmount: PAYABLE }] })
  check('\u5df2\u7ed3\u7b97\u5355\u62d2\u7edd\u91cd\u590d\u7ed3\u7b97', !!(again.body?.message), again.body?.message)
  check('\u91cd\u590d\u7ed3\u7b97\u4e0d\u4f1a\u4e8c\u6b21\u6263\u5e93\u5b58', stockOf() === stockBefore - QTY)

  // ── 8. 挂单 / 取单 ──
  const holdCreate = await call('POST', '/sales/retail', buildOrder({ payableAmount: PAYABLE }))
  const holdId = (holdCreate.body?.order || holdCreate.body)?.id
  await call('POST', `/sales/retail/${holdId}/hold`)
  let holdRow = sqlRows(`SELECT status, hold_order_flag FROM erp_retail_order WHERE id=${holdId}`)[0]
  check('\u6302\u5355\u540e\u72b6\u6001=2 \u4e14\u6807\u8bb0\u4e3a\u771f', Number(holdRow[0]) === 2 && holdRow[1] === 't', JSON.stringify(holdRow))
  const holdList = await call('GET', '/sales/retail/hold-list')
  check('\u6302\u5355\u5217\u8868\u53ef\u67e5\u8be2\u5230', (holdList.body || []).some(r => String(r.id) === String(holdId)))
  await call('POST', `/sales/retail/${holdId}/unhold`)
  holdRow = sqlRows(`SELECT status FROM erp_retail_order WHERE id=${holdId}`)[0]
  check('\u53d6\u5355\u540e\u72b6\u6001=0', Number(holdRow[0]) === 0)

  // ── 9. 打印计数（真实接口）──
  const before = Number(sqlRows(`SELECT COALESCE(print_count,0) FROM erp_retail_order WHERE id=${orderId}`)[0][0])
  await call('POST', `/sales/retail/${orderId}/print`)
  const after = Number(sqlRows(`SELECT COALESCE(print_count,0) FROM erp_retail_order WHERE id=${orderId}`)[0][0])
  check('\u6253\u5370\u8ba1\u6570+1', after === before + 1, `${before} \u2192 ${after}`)
  const printData = await call('GET', `/sales/retail/${orderId}/print-data`)
  check('\u6253\u5370\u6570\u636e\u542b\u660e\u7ec6', (printData.body?.items || []).length >= 1)

  // ── 10. 分页查询：按单据（经手人名称 / 商品行属性）──
  const byHandler = await call('GET', `/sales/retail/page/doc?pageNum=1&pageSize=50&handlerName=${encodeURIComponent('admin')}&dateStart=${TODAY}&dateEnd=${TODAY}`)
  check('\u6309\u5355\u636e-\u7ecf\u624b\u4eba\u540d\u79f0\u67e5\u8be2\u547d\u4e2d', (byHandler.body?.records || []).some(r => String(r.id) === String(orderId)))
  const byAttr = await call('GET', `/sales/retail/page/doc?pageNum=1&pageSize=50&productAttribute=${encodeURIComponent('POS\u91d1\u6807\u5c5e\u6027')}`)
  check('\u6309\u5355\u636e-\u5546\u54c1\u884c\u5c5e\u6027\u53cd\u67e5\u547d\u4e2d', (byAttr.body?.records || []).some(r => String(r.id) === String(orderId)))
  const byAttrNone = await call('GET', `/sales/retail/page/doc?pageNum=1&pageSize=50&productAttribute=${encodeURIComponent('\u4e0d\u5b58\u5728\u7684\u5c5e\u6027XYZ')}`)
  check('\u6309\u5355\u636e-\u5c5e\u6027\u65e0\u547d\u4e2d\u8fd4\u56de\u7a7a', Number(byAttrNone.body?.total) === 0)

  // ── 11. 分页查询：按明细（部门/仓库 + 明细字段完整性）──
  const byDept = await call('GET', `/sales/retail/page/detail?pageNum=1&pageSize=50&departmentName=${encodeURIComponent('\u603b\u7ecf\u7406\u529e\u516c\u5ba4')}&dateStart=${TODAY}&dateEnd=${TODAY}`)
  const deptRecords = byDept.body?.records || []
  check('\u6309\u660e\u7ec6-\u90e8\u95e8\u540d\u79f0\u67e5\u8be2\u547d\u4e2d', deptRecords.some(r => String(r.orderId) === String(orderId)))
  check('\u6309\u660e\u7ec6-total \u4e0e\u8fc7\u6ee4\u7ed3\u679c\u4e00\u81f4', Number(byDept.body?.total) === deptRecords.length, `total=${byDept.body?.total} records=${deptRecords.length}`)
  const sample = deptRecords.find(r => String(r.orderId) === String(orderId)) || {}
  check('\u6309\u660e\u7ec6-\u8865\u9f50\u6458\u8981/\u9644\u4ef6/\u8bb0\u8d26\u4eba/\u8bb0\u8d26\u65f6\u95f4\u5b57\u6bb5', 'summary' in sample && 'attachment' in sample && 'bookkeeperName' in sample && 'bookkeepingTime' in sample, Object.keys(sample).length + ' keys')
  const missing = ['itemCode', 'brand', 'conversionRelation', 'conversionResult', 'smallUnitPrice', 'favorableAmount', 'extNum3', 'extText2', 'extPartner', 'extStaff', 'extDept']
    .filter(k => !(k in sample))
  check('\u6309\u660e\u7ec6-52 \u5217\u5b57\u6bb5\u4f9d\u8d56\u5b8c\u6574', missing.length === 0, missing.join(',') || 'all present')

  // ── 12. 作废 ──
  await call('POST', `/sales/retail/${holdId}/void?reason=E2E\u4f5c\u5e9f`)
  const voidRow = sqlRows(`SELECT status, internal_note FROM erp_retail_order WHERE id=${holdId}`)[0]
  check('\u4f5c\u5e9f\u540e\u72b6\u6001=3 \u4e14\u8bb0\u5f55\u539f\u56e0', Number(voidRow[0]) === 3 && (voidRow[1] || '').includes('E2E\u4f5c\u5e9f'), voidRow[1])

  console.log(`\n\u2550\u2550\u2550 \u7ed3\u679c: ${pass} \u901a\u8fc7 / ${fail} \u5931\u8d25 \u2550\u2550\u2550`)
  if (fail) console.log('\u5931\u8d25\u9879: ' + failures.join(' | '))
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('\u274c \u811a\u672c\u5f02\u5e38: ' + (e.stack || e.message)); process.exit(2) })
