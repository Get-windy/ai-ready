// 销售换货单金标准验收
// 不变量驱动：① 单号来自后端号段 ② 保存不丢字段（主表全字段 + 明细全字段）
// ③ 审核真实过账（换入 +、换出 −） ④ 取消真实回滚（净变化归零） ⑤ 导出为真实 xlsx
const BASE = process.env.SEX_BASE || 'http://localhost:5655'
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
function sqlExec(q) {
  execFileSync(PSQL, ['-h', 'localhost', '-p', '5432', '-U', 'devuser', '-d', 'devdb', '-c', q],
    { env: PGENV, encoding: 'utf8' })
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
    body: JSON.stringify({ username: 'sex_e2e', password: 'admin123', tenantName: '系统租户', captcha, captchaKey: capData.uuid }),
  }).then(r => r.json())
  const token = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  currentToken = token
}

async function call(method, path, body, retried = false) {
  const raw = await fetch(`${BASE}/api${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${currentToken}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  }).then(r => r.text())
  let res
  try { res = JSON.parse(raw) } catch { res = raw }
  if (res?.code === 401 && !retried) {
    console.log('  ⚠️ 会话被踢，自动重登…')
    await login()
    return call(method, path, body, true)
  }
  return res
}

// ── 夹具 ──
const PRODUCT_ID = '2073239284579586050'
const PRODUCT_CODE = 'SP-20260704-041'
const PRODUCT_NAME = '博多新米坊酒酿果味酱罐头'
const IN_WH = 2      // 换入仓库（华东分仓，通常无库存，用于验证 +）
const OUT_WH = 1     // 换出仓库（主仓库，有库存，用于验证 −）
const QTY = 3
const PRICE = 100
const DISCOUNT = 80
const MARK = 'E2E-SEX'
const CUSTOMER_ID = 2
const CUSTOMER_NAME = '客户甲'

function stockQty(productId, whId) {
  const r = sqlRows(`SELECT COALESCE(SUM(quantity),0) FROM erp_stock WHERE product_id=${productId} AND warehouse_id=${whId} AND deleted=0`)
  return Number(r[0]?.[0] || 0)
}

function cleanup() {
  sqlExec(`DELETE FROM erp_sale_exchange_item WHERE exchange_id IN (SELECT id FROM erp_sale_exchange WHERE remark LIKE '%${MARK}%' OR ext_text1='${MARK}')`)
  sqlExec(`DELETE FROM erp_sale_exchange_approval_record WHERE exchange_id IN (SELECT id FROM erp_sale_exchange WHERE ext_text1='${MARK}')`)
  sqlExec(`DELETE FROM erp_sale_exchange WHERE ext_text1='${MARK}'`)
}

function buildPayload(no, status) {
  return {
    exchangeNo: no,
    status,
    exchangeDate: new Date().toISOString().slice(0, 10),
    customerId: CUSTOMER_ID,
    customerName: CUSTOMER_NAME,
    customerCode: 'C-002',
    customerLevel: 'A',
    contactName: '张三',
    contactPhone: '13800000000',
    contactAddress: '上海市浦东新区',
    bankName: '工商银行',
    bankAccount: '6222000000000001',
    taxNo: 'TAX-91310000',
    inWarehouseId: IN_WH,
    inWarehouseName: '华东分仓',
    outWarehouseId: OUT_WH,
    outWarehouseName: '主仓库',
    handlerId: 1,
    handlerName: '经手人A',
    deptId: 1,
    deptName: '销售部',
    salesType: '普通销售',
    settleStatus: 'unsettled',
    remark: '换货单验收数据 ' + MARK,
    summary: '换货验收摘要',
    attachment: 'a.pdf,b.pdf',
    paymentAccount: '基本户',
    moreAccounts: '备用金账户, 收款账户B',
    receivedAmount: 0,
    useAdvance: 10,
    collectionDeadline: '2026-10-01',
    extNum1: 11.5,
    extNum2: 22.5,
    extText1: MARK,
    extText2: '文本4',
    extText3: '文本5',
    items: [
      // 换入仓库行（换货回来的商品）
      {
        warehouseType: 1, productId: PRODUCT_ID, productCode: PRODUCT_CODE, productName: PRODUCT_NAME,
        barcode: '6901234567890', specification: '500g/罐', model: 'M-500', origin: '福建', brand: '博多',
        unit: '罐', productLineAttr: 'normal', location: 'A-01', area: '华东', image: 'img.png',
        availableStock: 0, stockConverted: 0, bookStock: 0,
        batchBarcode: 'B20260910', productionDate: '2026-09-01', shelfLife: 365, expiryDate: '2027-09-01',
        quantity: QTY, conversionRate: 12, pieceScatterQty: 1,
        largePackage: 1, mediumPackage: 2, smallPackage: 3,
        recentSaleDate: '2026-09-05', recentSalePrice: 95, retailPrice: 108, wholesalePrice: 90,
        minSalePrice: 85, unitPrice: PRICE, amount: QTY * PRICE,
        smallUnit: '袋', smallUnitPrice: 8, smallUnitQty: 36, costPrice: 60, costAmount: 180,
        discount: DISCOUNT, discountPrice: 80, discountAmount: QTY * 80,
        volume: 1.5, weight: 2.5, isGift: false, remark: '换入行备注',
        priceLevel1: 1, priceLevel2: 2, priceLevel3: 3, priceLevel4: 4,
        priceLevel5: 5, priceLevel6: 6, priceLevel7: 7, priceLevel8: 8,
        extNum1: 101, extNum2: 102, extNum3: 103, extText1: '行文本1', extText2: '行文本2',
        extNum6: 106, extNum7: 107, extPartner: CUSTOMER_ID, extStaff: 1, extDept: 1,
      },
      // 换出仓库行（换出去的商品）
      {
        warehouseType: 2, productId: PRODUCT_ID, productCode: PRODUCT_CODE, productName: PRODUCT_NAME,
        barcode: '6901234567890', specification: '500g/罐', model: 'M-500', origin: '福建', brand: '博多',
        unit: '罐', productLineAttr: 'gift', location: 'A-01', area: '华东',
        quantity: QTY, unitPrice: PRICE, amount: QTY * PRICE,
        discount: 100, discountPrice: 100, discountAmount: QTY * 100,
        costPrice: 60, costAmount: 180, weight: 2.5, volume: 1.5,
        remark: '换出行备注', priceLevel8: 88,
        extNum1: 201, extText1: '出行文本',
      },
    ],
  }
}

// ═══════════════════════════════════════════════
;(async () => {
  console.log('\n═══ 销售换货单金标准验收 ═══\n')
  await login()
  console.log('  ✔ 登录成功 (sex_e2e)')

  cleanup()
  const inQty0 = stockQty(PRODUCT_ID, IN_WH)
  const outQty0 = stockQty(PRODUCT_ID, OUT_WH)

  // ── 1. 号段 ──
  console.log('\n── 1. 单据号段（红线：必须来自后端 /next-no） ──')
  const no1 = (await call('GET', '/erp/sale/exchange/next-no'))?.data
  check('next-no 返回 XSHHD-yyyyMMdd-NNNN 格式', /^XSHHD-\d{8}-\d{4}$/.test(String(no1 || '')), `实测 ${no1}`)
  check('无随机 UUID 残留', !/^SE\d{8}[0-9A-F]{6}$/.test(String(no1 || '')), `实测 ${no1}`)

  // ── 2. 创建草稿并校验全字段落库 ──
  console.log('\n── 2. 创建（主表/明细全字段落库） ──')
  const created = await call('POST', '/erp/sale/exchange', buildPayload(no1, 0))
  const id = created?.data?.id || created?.id
  check('创建成功返回主键', !!id, `id=${id} resp=${JSON.stringify(created).slice(0, 160)}`)
  check('创建后状态=草稿(0)', Number(created?.data?.status ?? created?.status) === 0, `status=${created?.data?.status ?? created?.status}`)
  const noNext = (await call('GET', '/erp/sale/exchange/next-no'))?.data
  check('号段随单据落库递增', noNext === no1.replace(/(\d{4})$/, m => String(Number(m) + 1).padStart(4, '0')),
    `${no1} → ${noNext}`)

  const m = sqlRows(`SELECT customer_code, customer_level, contact_name, contact_phone, contact_address,
    bank_name, bank_account, tax_no, dept_name, handler_name, more_accounts, payment_account,
    use_advance, collection_deadline, ext_num1, ext_num2, ext_text1, ext_text2, ext_text3,
    summary, attachment, creator_name, exchange_date, in_warehouse_name, out_warehouse_name,
    product_amount, discount_amount, total_amount, in_quantity_total, out_quantity_total
    FROM erp_sale_exchange WHERE id=${id}`)[0] || []
  check('主表-客户快照全落库', m[0] === 'C-002' && m[1] === 'A' && m[2] === '张三' && m[3] === '13800000000' && m[4] === '上海市浦东新区')
  check('主表-银行/税号落库', m[5] === '工商银行' && m[6] === '6222000000000001' && m[7] === 'TAX-91310000')
  check('主表-部门/经手人落库', m[8] === '销售部' && m[9] === '经手人A')
  check('主表-更多账户落库', m[10] === '备用金账户, 收款账户B', `实测 ${m[10]}`)
  check('主表-收款账户落库', m[11] === '基本户')
  check('主表-使用预收款/收款期限落库', Number(m[12]) === 10 && m[13] === '2026-10-01')
  check('主表-表头自定义字段落库', Number(m[14]) === 11.5 && Number(m[15]) === 22.5 && m[16] === MARK && m[17] === '文本4' && m[18] === '文本5')
  check('主表-摘要/附件落库', m[19] === '换货验收摘要' && m[20] === 'a.pdf,b.pdf')
  check('主表-制单人落库', !!m[21], `creator_name=${m[21]}`)
  check('主表-单据日期落库', String(m[22]).startsWith(new Date().toISOString().slice(0, 10)))
  check('主表-双仓快照落库', m[23] === '华东分仓' && m[24] === '主仓库')
  check('主表-金额链（金额=Σ数量×单价=3*100*2行=600）', Number(m[25]) === 600, `product_amount=${m[25]}`)
  check('主表-折后金额（换入 3*80 + 换出 3*100 = 540）', Number(m[26]) === 540, `discount_amount=${m[26]}`)
  check('主表-本单金额=折后金额', Number(m[27]) === 540, `total_amount=${m[27]}`)
  check('主表-入/出数量分仓汇总', Number(m[28]) === QTY && Number(m[29]) === QTY, `in=${m[28]} out=${m[29]}`)

  const items = sqlRows(`SELECT warehouse_type, product_code, barcode, specification, model, origin, brand, unit,
    product_line_attr, location, area, quantity, conversion_rate, piece_scatter_qty, batch_barcode,
    production_date, shelf_life, expiry_date, retail_price, wholesale_price, min_sale_price, unit_price,
    amount, small_unit, small_unit_price, small_unit_qty, cost_price, cost_amount, discount,
    discount_price, discount_amount, volume, weight, is_gift, remark, price_level1, price_level8,
    ext_num1, ext_text1, ext_partner, ext_staff, ext_dept
    FROM erp_sale_exchange_item WHERE exchange_id=${id} ORDER BY warehouse_type`)
  check('明细落库 2 行（换入/换出各1）', items.length === 2, `rows=${items.length}`)
  const inRow = items.find(r => r[0] === '1') || []
  const outRow = items.find(r => r[0] === '2') || []
  check('明细-商品快照全落库', inRow[1] === PRODUCT_CODE && inRow[2] === '6901234567890' && inRow[3] === '500g/罐'
    && inRow[4] === 'M-500' && inRow[5] === '福建' && inRow[6] === '博多' && inRow[7] === '罐')
  check('明细-货位/区域/行属性落库', inRow[8] === 'normal' && inRow[9] === 'A-01' && inRow[10] === '华东')
  check('明细-数量/换算/件散落库', Number(inRow[11]) === QTY && Number(inRow[12]) === 12 && Number(inRow[13]) === 1)
  check('明细-批次/日期落库', inRow[14] === 'B20260910' && String(inRow[15]).startsWith('2026-09-01')
    && Number(inRow[16]) === 365 && String(inRow[17]).startsWith('2027-09-01'))
  check('明细-价格落库', Number(inRow[18]) === 108 && Number(inRow[19]) === 90 && Number(inRow[20]) === 85 && Number(inRow[21]) === 100)
  check('明细-小单位价格落库', inRow[23] === '袋' && Number(inRow[24]) === 8 && Number(inRow[25]) === 36)
  check('明细-成本落库', Number(inRow[26]) === 60 && Number(inRow[27]) === 180)
  check('明细-折扣链落库（折扣/折后单价/折后金额）', Number(inRow[28]) === DISCOUNT && Number(inRow[29]) === 80 && Number(inRow[30]) === 240)
  check('明细-体积/重量落库', Number(inRow[31]) === 1.5 && Number(inRow[32]) === 2.5)
  check('明细-赠品/备注落库', String(inRow[33]).toLowerCase() === 'f' && inRow[34] === '换入行备注')
  check('明细-价格等级落库', Number(inRow[35]) === 1 && Number(inRow[36]) === 8 && Number(outRow[36]) === 88)
  check('明细-自定义字段/关联字段落库', Number(inRow[37]) === 101 && inRow[38] === '行文本1'
    && Number(inRow[39]) === CUSTOMER_ID && Number(inRow[40]) === 1 && Number(inRow[41]) === 1)

  // ── 3. 详情回读 ──
  console.log('\n── 3. 详情回读 ──')
  const detail = await call('GET', `/erp/sale/exchange/${id}`)
  const detailItems = detail?.items || detail?.data?.items || []
  check('详情返回明细 2 行', detailItems.length === 2, `rows=${detailItems.length}`)
  const wh1 = await call('GET', `/erp/sale/exchange/${id}/items/1`)
  const wh1Rows = Array.isArray(wh1) ? wh1 : (wh1?.data || [])
  check('详情按仓库类型可分仓取数（换入仅1行）', wh1Rows.length === 1, `rows=${wh1Rows.length}`)

  // ── 4. 查询（类型安全 + 明细级条件） ──
  console.log('\n── 4. 列表查询 ──')
  const pStatus = await call('GET', '/erp/sale/exchange/page?pageNum=1&pageSize=5&status=0')
  check('按状态查询不报错（旧实现 ClassCastException）', pStatus?.code === 200 || !!pStatus?.records, JSON.stringify(pStatus).slice(0, 120))
  const pAttr = await call('GET', '/erp/sale/exchange/page?pageNum=1&pageSize=10&productLineAttr=normal')
  const attrIds = (pAttr?.records || []).map(r => String(r.id))
  check('商品行属性（明细级）过滤命中本单', attrIds.includes(String(id)), `hits=${attrIds.length}`)
  const pNone = await call('GET', '/erp/sale/exchange/page?pageNum=1&pageSize=10&productLineAttr=__none__')
  check('商品行属性无命中时返回空集（不返回全表）', (pNone?.records || []).length === 0, `rows=${(pNone?.records || []).length}`)
  const pHidden = await call('GET', '/erp/sale/exchange/page?pageNum=1&pageSize=50&startDate=2020-01-01&endDate=2030-01-01')
  check('默认隐藏已取消（显示红冲未勾选）', !(pHidden?.records || []).some(r => String(r.id) === String(id) && Number(r.status) === 6))

  // ── 5. 审批流 + 库存过账 ──
  console.log('\n── 5. 审批流与库存过账（P0 红线） ──')
  const sub = await call('POST', `/erp/sale/exchange/${id}/submit`)
  check('提交后状态=待审核(1)', Number(sub?.data?.status ?? sub?.status) === 1, `status=${sub?.data?.status ?? sub?.status}`)

  const app = await call('POST', `/erp/sale/exchange/${id}/approve?remark=${encodeURIComponent('同意换货')}`)
  check('审核后状态=已审核(2)', Number(app?.data?.status ?? app?.status) === 2, `status=${app?.data?.status ?? app?.status}`)
  const inQty1 = stockQty(PRODUCT_ID, IN_WH)
  const outQty1 = stockQty(PRODUCT_ID, OUT_WH)
  check('换入仓库库存 +3', inQty1 - inQty0 === QTY, `${inQty0} → ${inQty1}`)
  check('换出仓库库存 −3', outQty0 - outQty1 === QTY, `${outQty0} → ${outQty1}`)
  const recs = await call('GET', `/erp/sale/exchange/${id}/approval-records`)
  const recRows = recs?.data || recs || []
  check('审批轨迹落库（提交+审核）', recRows.length >= 2, `records=${recRows.length}`)

  const done = await call('POST', `/erp/sale/exchange/${id}/complete`)
  check('完成后状态=已完成(4)', Number(done?.data?.status ?? done?.status) === 4, `status=${done?.data?.status ?? done?.status}`)

  // ── 6. 导出 xlsx ──
  console.log('\n── 6. 导出（真实 xlsx） ──')
  const expRes = await fetch(`${BASE}/api/erp/sale/exchange/export?startDate=2020-01-01&endDate=2030-01-01&showRedFlush=true`, {
    headers: { Authorization: `Bearer ${currentToken}` },
  })
  const buf = Buffer.from(await expRes.arrayBuffer())
  const ctype = expRes.headers.get('content-type') || ''
  check('导出返回 xlsx MIME', ctype.includes('spreadsheetml'), ctype)
  check('导出内容为 ZIP/xlsx 魔数(PK)', buf.length > 1000 && buf[0] === 0x50 && buf[1] === 0x4b, `bytes=${buf.length}`)

  // ── 7. 取消回滚 ──
  console.log('\n── 7. 取消与库存回滚（P0 红线） ──')
  const cancel = await call('POST', `/erp/sale/exchange/${id}/cancel?reason=${encodeURIComponent('验收取消')}`)
  check('取消后状态=已取消(6)', Number(cancel?.data?.status ?? cancel?.status) === 6, `status=${cancel?.data?.status ?? cancel?.status}`)
  const inQty2 = stockQty(PRODUCT_ID, IN_WH)
  const outQty2 = stockQty(PRODUCT_ID, OUT_WH)
  check('换入仓库库存回滚（净变化 0）', inQty2 - inQty0 === 0, `${inQty1} → ${inQty2}`)
  check('换出仓库库存回滚（净变化 0）', outQty2 - outQty0 === 0, `${outQty1} → ${outQty2}`)

  // ── 8. 状态字典一致性 ──
  console.log('\n── 8. 状态字典（0/1/2/4/5/6） ──')
  const draft = await call('POST', '/erp/sale/exchange', buildPayload(noNext, 0))
  const draftId = draft?.data?.id || draft?.id
  const rejSubmit = await call('POST', `/erp/sale/exchange/${draftId}/submit`)
  const rej = await call('POST', `/erp/sale/exchange/${draftId}/reject?remark=${encodeURIComponent('验收拒绝')}`)
  check('拒绝后状态=已拒绝(5)', Number(rej?.data?.status ?? rej?.status) === 5, `status=${rej?.data?.status ?? rej?.status}`)

  // ── 9. 删除（级联明细） ──
  console.log('\n── 9. 删除与级联 ──')
  const delRes = await call('DELETE', `/erp/sale/exchange/${draftId}`)
  const left = sqlRows(`SELECT count(*) FROM erp_sale_exchange_item WHERE exchange_id=${draftId} AND deleted=0`)[0]?.[0]
  check('删除成功', delRes?.code === 200 || delRes?.data === true || delRes === true, JSON.stringify(delRes).slice(0, 100))
  check('明细级联逻辑删除', Number(left) === 0, `remaining=${left}`)

  cleanup()
  console.log('\n═══ 验收结果 ═══')
  console.log(`  ✅ 通过 ${pass} 项   ❌ 失败 ${fail} 项\n`)
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('验收异常:', e); cleanup(); process.exit(2) })
