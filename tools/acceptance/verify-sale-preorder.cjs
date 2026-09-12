// 预订货单金标准验收：号段 → 创建 → 提交 → 审核 → 批量订货（生成销售订单 + 回写已订/未订）→ 明细口径
// 关键红线校验：单号必须来自后端号段（非前端演示号）；页面配置字段真实落库
const BASE = process.env.PREORDER_BASE || 'http://localhost:5655'
const { execFileSync } = require('child_process')
const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe'
const PGENV = { ...process.env, PGPASSWORD: 'devuser123' }

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
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
async function login() {
  const capRes = await fetch(`${BASE}/api/auth/captcha`).then(r => r.json())
  const capData = capRes.data || capRes
  const captcha = extractCaptcha(capData.img)
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123', tenantName: '系统租户', captcha, captchaKey: capData.uuid }),
  }).then(r => r.json())
  const token = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  currentToken = token
}

async function call(method, path, body, retried = false) {
  const res = await fetch(`${BASE}/api${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${currentToken}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  }).then(r => r.json())
  if ((res?.code === 401 || res?.code === 500 && /未登录|token/i.test(res?.message || '')) && !retried) {
    console.log('  ⚠️ 会话被踢，自动重登…')
    await login()
    return call(method, path, body, true)
  }
  return res
}

const MARK = 'E2E-PREORDER'

async function main() {
  await login()
  console.log('\n【1】后端号段 /next-no')
  const noRes = await call('GET', '/erp/sale/pre-order/next-no')
  const orderNo = noRes?.data
  check('next-no 返回成功', noRes?.code === 200, JSON.stringify(noRes).slice(0, 160))
  check('单号格式 YDHD-yyyyMMdd-NNNN', /^YDHD-\d{8}-\d{4}$/.test(orderNo || ''), String(orderNo))

  console.log('\n【2】主数据准备')
  const prod = sqlRows(`SELECT id, product_code, product_name, category_id, COALESCE(unit,'') FROM erp_product WHERE deleted=0 AND tenant_id=1 AND category_id IS NOT NULL ORDER BY id LIMIT 1`)[0] || []
  const cust = sqlRows(`SELECT id, party_name, COALESCE(credit_limit,0) FROM biz_party WHERE deleted=0 AND tenant_id=1 ORDER BY id LIMIT 1`)[0] || []
  const wh = sqlRows(`SELECT id, COALESCE(warehouse_name,'') FROM erp_warehouse WHERE deleted=0 AND tenant_id=1 ORDER BY id LIMIT 1`)[0] || []
  check('存在可售商品', !!prod[0], prod[2])
  check('存在往来单位', !!cust[0], cust[1])
  check('存在仓库', !!wh[0], wh[1])
  if (!prod[0] || !cust[0]) { console.log('缺主数据，终止'); process.exit(1) }

  const PRODUCT_ID = prod[0]           // 雪花ID → 字符串透传
  const CUSTOMER_ID = Number(cust[0])
  const WAREHOUSE_ID = Number(wh[0])
  const QTY = 5
  const PRICE = 30

  console.log('\n【3】创建（号段直落 + 汇总口径 + 表尾自定义字段）')
  const createBody = {
    orderNo,
    customerId: CUSTOMER_ID,
    customerName: cust[1] || '',
    warehouseId: WAREHOUSE_ID,
    warehouseName: wh[1] || '',
    orderDate: new Date().toISOString().slice(0, 10),
    saleType: 0,
    status: 0,
    totalAmount: QTY * PRICE,
    discountedAmount: 0,
    orderAmount: QTY * PRICE,
    depositAccount1: '测试账户',
    depositAmount: 0,
    creditLimit: 0,
    remark: MARK,
    summary: MARK + '-摘要',
    footerExtText1: '尾部A',
    footerExtText2: '尾部B',
    items: [{
      productId: PRODUCT_ID,
      productName: prod[2],
      productCode: prod[1],
      unit: prod[4],
      pricingUnit: prod[4],
      quantity: QTY,
      unitPrice: PRICE,
      amount: QTY * PRICE,
      weight: 1.5,
      volume: 0.2,
      productAttribute: '正常',
      remark: MARK + '-明细备注',
    }],
  }
  const created = await call('POST', '/erp/sale/pre-order', createBody)
  check('创建返回成功', created?.code === 200, JSON.stringify(created).slice(0, 200))
  const orderId = created?.data?.id
  check('返回单号 = 号段号', created?.data?.orderNo === orderNo, created?.data?.orderNo)

  const row = sqlRows(`SELECT order_no, status, COALESCE(creator_name,''), COALESCE(footer_ext_text1,''), COALESCE(footer_ext_text2,''),
      COALESCE(pre_order_quantity,0), COALESCE(un_ordered_quantity,0), COALESCE(total_weight,0), COALESCE(total_volume,0)
    FROM erp_sale_pre_order WHERE id=${orderId}`)[0] || []
  check('落库：单号/状态', row[0] === orderNo && row[1] === '0', `${row[0]} status=${row[1]}`)
  check('落库：制单人真实姓名', !!row[2], row[2])
  check('落库：表尾自定义1/2', row[3] === '尾部A' && row[4] === '尾部B', `${row[3]}/${row[4]}`)
  check('汇总：预订数量 = Σ明细数量', Number(row[5]) === QTY, `pre=${row[5]}`)
  check('汇总：未订数量 = 预订数量', Number(row[6]) === QTY, `un=${row[6]}`)
  check('汇总：重量 = 单位重量×数量', Number(row[7]) === 1.5 * QTY, `w=${row[7]}`)
  check('汇总：体积 = 单位体积×数量', Number(row[8]) === 0.2 * QTY, `v=${row[8]}`)

  const itemRow = sqlRows(`SELECT COALESCE(un_ordered_quantity,-1), COALESCE(amount,-1), COALESCE(product_attribute,'') FROM erp_sale_pre_order_item WHERE order_id=${orderId} AND deleted=0`)[0] || []
  check('明细：未订数量滚动值', Number(itemRow[0]) === QTY, `un=${itemRow[0]}`)
  check('明细：金额落库', Number(itemRow[1]) === QTY * PRICE, `amt=${itemRow[1]}`)

  console.log('\n【4】提交 → 审核')
  await call('POST', `/erp/sale/pre-order/${orderId}/submit`)
  const afterSubmit = sqlRows(`SELECT status, COALESCE(submitter_name,''), submit_time IS NOT NULL FROM erp_sale_pre_order WHERE id=${orderId}`)[0] || []
  check('提交后状态=1 审核中', afterSubmit[0] === '1', `status=${afterSubmit[0]}`)
  check('提交人/提交时间落库', !!afterSubmit[1] && afterSubmit[2] === 't', `${afterSubmit[1]}/${afterSubmit[2]}`)

  await call('POST', `/erp/sale/pre-order/${orderId}/approve`)
  const afterApprove = sqlRows(`SELECT status, COALESCE(auditor_name,'') FROM erp_sale_pre_order WHERE id=${orderId}`)[0] || []
  check('审核后状态=2 待订货', afterApprove[0] === '2', `status=${afterApprove[0]}`)
  check('审核人落库', !!afterApprove[1], afterApprove[1])

  console.log('\n【5】批量订货 → 生成销售订单并回写')
  const batch = await call('POST', '/erp/sale/pre-order/batch-order', { ids: [orderId] })
  check('批量订货成功', batch?.code === 200 && batch?.data?.successCount === 1, JSON.stringify(batch).slice(0, 260))

  const so = sqlRows(`SELECT id, order_no, COALESCE(source_order,''), COALESCE(bill_amount,0) FROM erp_sale_order WHERE source_order='${orderNo}' LIMIT 1`)[0] || []
  check('生成销售订单（source_order 关联预订单号）', !!so[0] && so[2] === orderNo, `${so[1]}`)
  const soItem = sqlRows(`SELECT COALESCE(pre_order_no,''), COALESCE(quantity,0), COALESCE(unit_price,0) FROM erp_sale_order_item WHERE order_id=${so[0]}`)[0] || []
  check('销售订单明细：预订单号回写 + 数量价格', soItem[0] === orderNo && Number(soItem[1]) === QTY && Number(soItem[2]) === PRICE,
    `${soItem[0]} qty=${soItem[1]} price=${soItem[2]}`)

  const afterOrder = sqlRows(`SELECT status, COALESCE(ordered_quantity,0), COALESCE(un_ordered_quantity,0) FROM erp_sale_pre_order WHERE id=${orderId}`)[0] || []
  check('预订单状态 → 4 已订货', afterOrder[0] === '4', `status=${afterOrder[0]}`)
  check('预订单已订数量回写', Number(afterOrder[1]) === QTY, `ordered=${afterOrder[1]}`)
  check('预订单未订数量归零', Number(afterOrder[2]) === 0, `un=${afterOrder[2]}`)
  const itemAfter = sqlRows(`SELECT COALESCE(ordered_quantity,0), COALESCE(un_ordered_quantity,0) FROM erp_sale_pre_order_item WHERE order_id=${orderId} AND deleted=0`)[0] || []
  check('明细已订/未订回写', Number(itemAfter[0]) === QTY && Number(itemAfter[1]) === 0, `${itemAfter[0]}/${itemAfter[1]}`)

  console.log('\n【6】列表接口（按单据 / 按明细 / 分类下钻）')
  // 分页接口直接返回 MyBatis-Plus Page（顶层即 records/total），兼容 ApiResponse 包裹
  const page = await call('GET', `/erp/sale/pre-order/page?keyword=${orderNo}&status=-1,0,1,2,3,4,5&pageNum=1&pageSize=10`)
  const p = page?.data ?? page
  check('按单据分页可查', (p?.records || []).some(r => r.orderNo === orderNo), `total=${p?.total}`)
  const detail = await call('GET', `/erp/sale/pre-order/page-detail?orderNo=${orderNo}&status=-1,0,1,2,3,4,5&pageNum=1&pageSize=10`)
  const d = detail?.data ?? detail
  const d0 = (d?.records || [])[0] || {}
  check('按明细分页可查', !!d0.orderNo, `total=${d?.total}`)
  check('明细行：单据备注 ≠ 明细备注（字段串位修复）', d0.orderRemark === MARK && d0.remark === MARK + '-明细备注', `${d0.orderRemark} / ${d0.remark}`)
  check('明细行：真实商品主数据字段', !!d0.productCode && Number(d0.quantity) === QTY && d0.unit === prod[4],
    `${d0.productCode} qty=${d0.quantity} unit=${d0.unit}`)
  const cat = await call('GET', `/erp/sale/pre-order/page-detail?categoryId=${prod[3]}&status=-1,0,1,2,3,4,5&pageNum=1&pageSize=5`)
  const c = cat?.data ?? cat
  check('按明细：商品分类下钻生效', (c?.records || []).length >= 1, `total=${c?.total}`)

  console.log('\n【7】清理测试数据')
  const delSO = sqlText(`DELETE FROM erp_sale_order_item WHERE order_id=${so[0]}; DELETE FROM erp_sale_order WHERE id=${so[0]}; SELECT 1`)
  const delPre = sqlText(`DELETE FROM erp_sale_pre_order_item WHERE order_id=${orderId}; DELETE FROM erp_sale_pre_order WHERE id=${orderId}; SELECT 1`)
  const left = sqlRows(`SELECT COUNT(*) FROM erp_sale_pre_order WHERE id=${orderId}`)[0]?.[0]
  check('测试数据已清理', left === '0', `剩余=${left} ${delSO ? '' : ''}${delPre ? '' : ''}`)

  console.log(`\n════════ 结果：${pass} 通过 / ${fail} 失败 ════════`)
  process.exit(fail === 0 ? 0 : 1)
}

main().catch(e => { console.error('验收脚本异常:', e); process.exit(1) })
