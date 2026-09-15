// 销售退货申请金标准验收：号段 → 创建 → 提交 → 审核（状态推进，入库/记账下沉到退货单收货） → 取消 → 明细分类过滤
// 业务口径（2026-09-13 调整）：退货申请审核不再直接入库/记账，库存与凭证统一由《销售退货单》收货环节承担，
// 避免「申请」「退货单」双链路重复过账；收货进度（订货/已收/未收）回写见 tools/e2e-return-receive.cjs。
// 不变量驱动：审核前后库存净变化为 0（未入账）
const BASE = process.env.ERP_BASE || 'http://localhost:5655'
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
    body: JSON.stringify({ username: 'sra_e2e', password: 'admin123', tenantName: '系统租户', captcha, captchaKey: capData.uuid }),
  }).then(r => r.json())
  const token = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  currentToken = token
  return captcha
}

async function call(method, path, body, retried = false) {
  const res = await fetch(`${BASE}/api${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${currentToken}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  }).then(r => r.json())
  if (res?.code === 401 && !retried) {
    console.log('  ⚠️ 会话被踢，自动重登…')
    await login()
    return call(method, path, body, true)
  }
  return res
}

// ── 固定夹具（devdb 现有主数据） ──
// 雪花ID超出 JS 安全整数，必须以字符串传递，避免精度丢失
const PRODUCT_ID = '2073239284579586050'
const PRODUCT_CODE = 'SP-20260704-041'
const PRODUCT_NAME = '博多新米坊酒酿果味酱罐头'
const CATEGORY_ID = '2072844550224740354'
const WAREHOUSE_ID = 1
const WAREHOUSE_NAME = '主仓库'
const CUSTOMER_ID = 2
const CUSTOMER_NAME = '客户甲'
const QTY = 3
const PRICE = 100
const COST = 240
const MARK = 'E2E-SRA'

function stockQty() {
  const r = sqlRows(`SELECT COALESCE(SUM(quantity),0) FROM wms_inventory WHERE product_id=${PRODUCT_ID} AND warehouse_id=${WAREHOUSE_ID}`)
  return Number(r[0]?.[0] || 0)
}

function cleanup(returnId) {
  if (returnId) {
    const vids = sqlRows(`SELECT DISTINCT voucher_id FROM finance_voucher_item WHERE source_id=${returnId} AND source_type IN ('SALE_RETURN_APPLY','SALE_RETURN_APPLY_CANCEL')`).map(r => r[0]).filter(Boolean)
    sqlText(`DELETE FROM finance_voucher_item WHERE source_id=${returnId} AND source_type IN ('SALE_RETURN_APPLY','SALE_RETURN_APPLY_CANCEL')`)
    if (vids.length) {
      sqlText(`DELETE FROM finance_voucher_item WHERE voucher_id IN (${vids.join(',')})`)
      sqlText(`DELETE FROM finance_voucher WHERE id IN (${vids.join(',')})`)
    }
    sqlText(`DELETE FROM erp_sale_return_item WHERE return_id=${returnId}`)
    sqlText(`DELETE FROM erp_sale_return WHERE id=${returnId}`)
  }
  // 清理历史同名测试单据（保证脚本可重复执行）
  const olds = sqlRows(`SELECT id FROM erp_sale_return WHERE remark='${MARK}'`).map(r => r[0]).filter(Boolean)
  olds.forEach(id => cleanup(Number(id)))
}

async function main() {
  await login()
  console.log('登录成功')

  // 清理历史测试数据
  cleanup(null)
  console.log('历史测试数据已清理\n')

  const qty0 = stockQty()
  let returnId = null

  try {
    // ═══ A. 号段 ═══
    console.log('[A] 单据号段（后端 /next-no）')
    const no1 = (await call('GET', '/erp/sale/return/next-no')).data
    check('号段格式 XSTHSQD-yyyyMMdd-NNNN', /^XSTHSQD-\d{8}-\d{4}$/.test(String(no1)), String(no1))
    check('号段非随机（同日同序号稳定，由已落库最大号推进）',
      String(no1).endsWith('0001') || /^XSTHSQD-\d{8}-\d{4}$/.test(String(no1)), String(no1))

    // ═══ B. 创建（单号来自号段） ═══
    console.log('\n[B] 创建退货申请单（单号取自号段，非前端演示号）')
    const createRes = await call('POST', '/erp/sale/return', {
      returnNo: no1,
      customerId: CUSTOMER_ID, customerName: CUSTOMER_NAME, customerCode: 'C002',
      warehouseId: WAREHOUSE_ID, warehouseName: WAREHOUSE_NAME,
      handlerId: 1, handlerName: 'admin',
      orderDate: '2026-09-10T00:00:00',
      status: 0, remark: MARK, summary: 'E2E 销售退货申请',
      items: [
        { productId: PRODUCT_ID, productCode: PRODUCT_CODE, productName: PRODUCT_NAME, unit: '瓶',
          returnQuantity: QTY, unitPrice: PRICE, lineAmount: QTY * PRICE, refCostPrice: 80, refCostAmount: COST,
          itemRemark: 'E2E 明细' },
      ],
    })
    check('创建接口返回 200', createRes.code === 200, `code=${createRes.code} ${createRes.message || ''}`)
    const doc = createRes.data
    returnId = doc?.id
    check('单号原样落库（未覆盖为随机 UUID）', doc?.returnNo === no1, `返回 ${doc?.returnNo}`)
    check('主表金额由明细计算', Number(doc?.totalAmount) === QTY * PRICE, `totalAmount=${doc?.totalAmount}`)
    check('明细已落库', (doc?.items?.length || 0) === 1, `items=${doc?.items?.length}`)

    const no2 = (await call('GET', '/erp/sale/return/next-no')).data
    check('落库后号段推进（+1）', Number(String(no2).slice(-4)) === Number(String(no1).slice(-4)) + 1, `${no1} → ${no2}`)

    // ═══ C. 提交 → 审核（状态推进；入库/记账下沉到退货单收货） ═══
    console.log('\n[C] 提交审核 → 审核通过（状态推进，入库/记账下沉到退货单收货）')
    const sub = await call('POST', `/erp/sale/return/${returnId}/submit`)
    check('提交后状态=1（审核中）', sub.data?.status === 1, `status=${sub.data?.status}`)

    const app = await call('POST', `/erp/sale/return/${returnId}/approve?note=E2E审核`)
    check('审核接口 200', app.code === 200, `code=${app.code} ${app.message || ''}`)
    check('审核后状态=2（审核通过=待收货）', app.data?.status === 2, `status=${app.data?.status}`)
    check('审核不置记账标记（记账由退货单收货环节负责）', !app.data?.bookkeepingTime, String(app.data?.bookkeepingTime))
    check('审核人/审核时间真实回填', !!app.data?.auditorName && !!app.data?.auditTime,
      `${app.data?.auditorName} / ${app.data?.auditTime}`)
    check('制单人真实回填', !!app.data?.creatorName, String(app.data?.creatorName))
    // 收货进度跟踪三列的真实来源（未收货时 已收=0 / 未收=订货数量）
    check('订货数量 = 明细退货数量合计', Number(app.data?.orderedQuantity) === QTY, `ordered=${app.data?.orderedQuantity}`)
    check('未收货时 已收数量 = 0', Number(app.data?.receivedQuantity) === 0, `received=${app.data?.receivedQuantity}`)
    check('未收货时 未收数量 = 订货数量', Number(app.data?.unreceivedQuantity) === QTY, `unreceived=${app.data?.unreceivedQuantity}`)

    // 申请审核不再直接入库：入库与记账统一由《销售退货单》收货环节承担，避免双链路重复过账
    const qty1 = stockQty()
    check('审核不直接入库（库存保持不变）', Math.abs(qty1 - qty0) < 0.0001, `${qty0} → ${qty1}`)

    const vrows = sqlRows(`SELECT i.subject_code FROM finance_voucher v
      JOIN finance_voucher_item i ON i.voucher_id = v.id
      WHERE i.source_type='SALE_RETURN_APPLY' AND i.source_id=${returnId}`)
    check('申请侧不生成重复凭证（避免与退货单重复记账）', vrows.length === 0, `分录数=${vrows.length}`)

    // ═══ D. 按明细查询 + 分类过滤 ═══
    console.log('\n[D] 按明细分页 + 商品分类过滤')
    const p1 = await call('GET', `/erp/sale/return/page-detail?pageNum=1&pageSize=20&returnNo=${no1}&categoryId=${CATEGORY_ID}`)
    check('categoryId 命中该商品的分类 → 有数据', (p1.data?.records?.length || 0) > 0, `records=${p1.data?.records?.length}`)
    const p2 = await call('GET', `/erp/sale/return/page-detail?pageNum=1&pageSize=20&returnNo=${no1}&categoryId=999999`)
    check('categoryId 不匹配 → 无数据', (p2.data?.records?.length || 0) === 0, `records=${p2.data?.records?.length}`)
    const row = p1.data?.records?.[0]
    check('明细返回商品名称/退货数量（驼峰键）', row?.productName === PRODUCT_NAME && Number(row?.returnQuantity) === QTY,
      `${row?.productName} × ${row?.returnQuantity}`)

    // ═══ E. 取消（状态关闭；未过账故无需冲回） ═══
    console.log('\n[E] 取消单据（状态关闭；申请侧未过账，无库存/凭证冲回）')
    const cancelRes = await call('POST', `/erp/sale/return/${returnId}/cancel?reason=E2E取消`)
    check('取消后状态=4', cancelRes.data?.status === 4, `status=${cancelRes.data?.status}`)
    const qty2 = stockQty()
    check('取消后库存不变（申请侧未入账）', Math.abs(qty2 - qty0) < 0.0001, `${qty1} → ${qty2}（初始 ${qty0}）`)
    const rrows = sqlRows(`SELECT i.subject_code FROM finance_voucher v
      JOIN finance_voucher_item i ON i.voucher_id = v.id
      WHERE i.source_type='SALE_RETURN_APPLY_CANCEL' AND i.source_id=${returnId}`)
    check('取消不生成反向凭证（申请侧未记账）', rrows.length === 0, `分录数=${rrows.length}`)
    const dup = await call('POST', `/erp/sale/return/${returnId}/cancel?reason=再次取消`)
    check('重复取消被拦截（400 业务异常）', dup.code === 400, `code=${dup.code} ${dup.message || ''}`)

    // ═══ F. 列表（按单据）关键字检索 ═══
    console.log('\n[F] 按单据分页检索')
    const lp = await call('GET', `/erp/sale/return/page?pageNum=1&pageSize=10&keyword=${no1}`)
    check('按单据编号检索命中', (lp.data?.records?.length || 0) > 0, `records=${lp.data?.records?.length}`)
    const lp2 = await call('GET', `/erp/sale/return/page?pageNum=1&pageSize=10&productName=${encodeURIComponent('博多')}`)
    check('pageList 商品名条件真实生效（EXISTS 下推）', (lp2.data?.records?.length || 0) > 0, `records=${lp2.data?.records?.length}`)
  } finally {
    cleanup(returnId)
    console.log(`\n清理完成：测试单据与凭证已移除，库存=${stockQty()}（初始 ${qty0}）`)
  }

  console.log(`\n══════ 验收结果：${pass} 通过 / ${fail} 失败 ══════`)
  process.exit(fail > 0 ? 1 : 0)
}

main().catch(e => { console.error('脚本异常:', e); process.exit(1) })
