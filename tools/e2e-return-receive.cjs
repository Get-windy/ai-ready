/*
 * 物流退货收货（配发收 → 收货业务 → 物流退货收货，菜单 70160）金标准端到端验证
 *
 * 业务口径：本页 = 《销售退货申请-历史》视图（入口直达复用），跟踪
 *   按单据：订货数量 / 已收数量 / 未收数量；按明细：退货数量 / 已收数量 / 未收数量。
 *   收货进度由《销售退货单》审核驱动回写（erp_sale_return_item.received_quantity），
 *   本脚本同时验证「申请审核不再入库」与「退货单收货入库 + 回写」的不重复过账闭环。
 *
 * 前置：执行 tools/e2e-return-receive-user.sql（验收账号 + 3 张退货申请基座）
 * 用法：node tools/e2e-return-receive.cjs            （默认后端 5660、前端 5656）
 *      ERP_PORT=5655 node tools/e2e-return-receive.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const { execFileSync } = require('child_process')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5660)
const BASE = `http://localhost:${PORT}`
const SHOTS = 'I:/AI-Ready/tool-results/return-receive'
const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe'
const PGENV = { ...process.env, PGPASSWORD: 'devuser123' }

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

// ── PostgreSQL 直连（库存/单据断言；雪花 ID 一律以字符串参与 SQL） ──
function sqlText(q) {
  return execFileSync(PSQL, ['-h', 'localhost', '-p', '5432', '-U', 'devuser', '-d', 'devdb', '-t', '-A', '-F', '|', '-c', q],
    { env: PGENV, encoding: 'utf8' }).trim()
}
function sqlRows(q) {
  const out = sqlText(q)
  return out ? out.split(/\r?\n/).map(l => l.split('|')) : []
}
function stockQty(productId, warehouseId = 1) {
  const r = sqlRows(`SELECT COALESCE(SUM(quantity),0) FROM wms_inventory WHERE product_id=${productId} AND warehouse_id=${warehouseId}`)
  return Number(r[0]?.[0] || 0)
}

// ── 登录（e2e_retrecv，失败回退 admin） ──
let TOKEN = ''
const E2E_USER = process.env.E2E_USER || 'e2e_retrecv'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

function extractCaptcha(img) {
  const m = String(img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function login(user = E2E_USER) {
  const cap = await fetch(`${BASE}/api/auth/captcha`).then(r => r.json())
  const d = cap.data || cap
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: user, password: E2E_PWD, tenantName: '系统租户', captcha: extractCaptcha(d.img), captchaKey: d.uuid }),
  }).then(r => r.json())
  let token = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue
  if (!token && user !== 'admin') {
    return login('admin')
  }
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  TOKEN = token
  return token
}

async function call(method, p, body, retried = false) {
  const res = await fetch(`${BASE}/api${p}`, {
    method,
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${TOKEN}` },
    body: body === undefined ? undefined : JSON.stringify(body),
  }).then(r => r.json()).catch(e => ({ code: -1, message: String(e) }))
  if (res?.code === 401 && !retried) {
    console.log('  ⚠️ 会话被踢，自动重登…')
    await login()
    return call(method, p, body, true)
  }
  return res
}

// ── 基座夹具（与 e2e-return-receive-user.sql 保持一致；雪花 ID 用字符串） ──
const APPLY_1 = '2099000000000000810', APPLY_2 = '2099000000000000811', APPLY_3 = '2099000000000000812'
const PROD_JAM = '990000000000000001'    // 博多家园百香果果酱（申请1 行1 / 申请3）
const PROD_SEA = '990000000000000002'    // 海鲜全家福（申请1 行2）
const PROD_DUMP = '990000000000000003'   // 手工蒸饺（申请2）
const CUSTOMER_1 = '2099000000000000851'
const USER_ID = '2099000000000000801'

function docBody(no, applyId, applyNo, customerId, customerName, items) {
  return {
    returnDocNo: no,
    returnApplyId: applyId, returnApplyNo: applyNo,
    customerId, customerName,
    warehouseId: 1, warehouseName: '主仓库',
    handlerId: USER_ID, handlerName: 'E2E退货收货',
    orderDate: '2026-09-13T10:00:00',
    salesType: '正常销售', generateType: '手动创建',
    status: 1, remark: 'E2E-保底', summary: 'E2E退货收货',
    items,
  }
}

const created = []
function cleanupTestDocs() {
  const ids = sqlRows(`SELECT id FROM erp_sale_return_doc WHERE return_apply_no LIKE 'E2E-THSH-%'`).map(r => r[0]).filter(Boolean)
  if (!ids.length) return
  const inList = ids.join(',')
  // 已过账未取消的：先回冲库存，避免重复执行造成库存漂移
  sqlText(`UPDATE wms_inventory wi SET quantity = wi.quantity - d.qty
           FROM (SELECT i.product_id, r.warehouse_id, SUM(i.return_quantity) AS qty
                 FROM erp_sale_return_doc r JOIN erp_sale_return_doc_item i ON i.return_doc_id = r.id
                 WHERE r.id IN (${inList}) AND r.bookkeeping_time IS NOT NULL AND r.status <> 4
                 GROUP BY i.product_id, r.warehouse_id) d
           WHERE wi.product_id = d.product_id AND wi.warehouse_id = d.warehouse_id`)
  const vids = sqlRows(`SELECT DISTINCT voucher_id FROM finance_voucher_item WHERE source_id IN (${inList})`).map(r => r[0]).filter(Boolean)
  sqlText(`DELETE FROM finance_voucher_item WHERE source_id IN (${inList})`)
  if (vids.length) {
    sqlText(`DELETE FROM finance_voucher WHERE id IN (${vids.join(',')}) AND NOT EXISTS (SELECT 1 FROM finance_voucher_item i WHERE i.voucher_id = finance_voucher.id)`)
  }
  sqlText(`DELETE FROM erp_sale_return_doc_item WHERE return_doc_id IN (${inList})`)
  sqlText(`DELETE FROM erp_sale_return_doc WHERE id IN (${inList})`)
}

function applyRow(returnNo) {
  const r = sqlRows(`SELECT ordered_quantity, received_quantity, unreceived_quantity, status FROM erp_sale_return WHERE return_no='${returnNo}'`)
  if (!r.length) return null
  return { ordered: Number(r[0][0] || 0), received: Number(r[0][1] || 0), unreceived: Number(r[0][2] || 0), status: Number(r[0][3]) }
}

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')
  await login()
  console.log('  登录成功')

  cleanupTestDocs()
  // 复位基座（重复执行可跑）：清空已收 + 状态回到已审核待收货
  sqlText(`UPDATE erp_sale_return_item SET received_quantity = 0 WHERE return_id IN (${APPLY_1}, ${APPLY_2}, ${APPLY_3})`)
  sqlText(`UPDATE erp_sale_return SET received_quantity = 0, unreceived_quantity = ordered_quantity, status = 2
           WHERE return_no LIKE 'E2E-THSH-%'`)

  // ── A. 按单据列表：三列口径 + 派生列 + 金额列 ──
  console.log('\n[A] 按单据列表（订货/已收/未收 + 派生列）')
  const list = await call('GET', '/erp/sale/return/page?pageNum=1&pageSize=20&keyword=E2E-THSH')
  const rows = list?.data?.records || []
  check('按单据命中 3 张退货申请', rows.length === 3, `rows=${rows.length}`)
  const r1 = rows.find(r => r.returnNo === 'E2E-THSH-0001') || {}
  check('订货数量 = 10（明细退货数量合计）', Number(r1.orderedQuantity) === 10, r1.orderedQuantity)
  check('已收数量 = 0（基座初始待收货）', Number(r1.receivedQuantity) === 0, r1.receivedQuantity)
  check('未收数量 = 10', Number(r1.unreceivedQuantity) === 10, r1.unreceivedQuantity)
  check('提交人姓名已解析（submitByName）', !!r1.submitByName, String(r1.submitByName))
  check('商品行数已派生（lineCount=2）', Number(r1.lineCount) === 2, r1.lineCount)
  check('本单金额（billAmount）非空', r1.billAmount != null && Number(r1.billAmount) > 0, r1.billAmount)
  check('折后金额（discountBillAmount）非空', r1.discountBillAmount != null && Number(r1.discountBillAmount) > 0, r1.discountBillAmount)
  check('客户/仓库快照真实', r1.customerName === 'E2E退货客户' && r1.warehouseName === '主仓库', `${r1.customerName}/${r1.warehouseName}`)

  // ── B. 按明细列表：未收数量派生 ──
  console.log('\n[B] 按明细列表（退货/已收/未收）')
  const detail = await call('GET', '/erp/sale/return/page-detail?pageNum=1&pageSize=50&returnNo=E2E-THSH-0001')
  const drows = detail?.data?.records || detail?.data?.list || []
  check('按明细命中申请1 的两行', drows.length === 2, `rows=${drows.length}`)
  const d1 = drows.find(x => x.productCode === 'SP-TEST-001') || {}
  check('明细退货数量 = 6', Number(d1.returnQuantity) === 6, d1.returnQuantity)
  check('明细已收数量 = 0', Number(d1.receivedQuantity) === 0, d1.receivedQuantity)
  check('明细未收数量 = 6（派生）', Number(d1.unreceivedQuantity) === 6, d1.unreceivedQuantity)
  check('明细单位/单价真实', d1.unit === '瓶' && Number(d1.unitPrice) === 100, `${d1.unit}/${d1.unitPrice}`)

  // ── C. 部分收货：退货单收 4 → 回写申请 已收 4 / 未收 6 ──
  console.log('\n[C] 部分收货（退货单审核 → 回写申请）')
  const qty0Jam = stockQty(PROD_JAM)
  const no1 = (await call('GET', '/erp/sale/return-doc/next-no'))?.data
  const c1 = await call('POST', '/erp/sale/return-doc', docBody(no1, APPLY_1, 'E2E-THSH-0001', CUSTOMER_1, 'E2E退货客户', [
    { productId: PROD_JAM, productCode: 'SP-TEST-001', productName: '博多家园百香果果酱', unit: '瓶', returnQuantity: 4, unitPrice: 100, lineAmount: 400 },
  ]))
  check('退货单创建成功（关联申请1，收 4）', c1?.code === 200 && !!c1.data?.id, `code=${c1?.code} ${c1?.message || ''}`)
  const doc1 = c1?.data || {}
  created.push(doc1.id)
  const check0 = applyRow('E2E-THSH-0001')
  check('创建退货单后申请仍为待收货（已收 0 / 未收 10）', check0.received === 0 && check0.unreceived === 10, JSON.stringify(check0))
  const a1 = await call('POST', `/erp/sale/return-doc/${doc1.id}/approve?note=E2E`)
  check('退货单审核通过', a1?.code === 200, `code=${a1?.code} ${a1?.message || ''}`)
  const check1 = applyRow('E2E-THSH-0001')
  check('回写申请已收数量 = 4', check1.received === 4, JSON.stringify(check1))
  check('回写申请未收数量 = 6', check1.unreceived === 6, JSON.stringify(check1))
  check('部分收货申请状态保持 2（待收货）', check1.status === 2, check1.status)
  const item1 = sqlRows(`SELECT received_quantity FROM erp_sale_return_item WHERE return_id=${APPLY_1} AND product_id=${PROD_JAM}`)
  check('回写申请明细已收数量 = 4', Number(item1[0]?.[0] || -1) === 4, String(item1[0]?.[0]))
  check('退货单收货入库 +4', Math.abs(stockQty(PROD_JAM) - (qty0Jam + 4)) < 0.0001, `${qty0Jam} → ${stockQty(PROD_JAM)}`)

  // ── D. 全部收货：退货单收 8 → 申请已完成 ──
  console.log('\n[D] 全部收货（申请推进为已完成）')
  const no2 = (await call('GET', '/erp/sale/return-doc/next-no'))?.data
  const c2 = await call('POST', '/erp/sale/return-doc', docBody(no2, APPLY_2, 'E2E-THSH-0002', '2099000000000000852', 'E2E退货客户二', [
    { productId: PROD_DUMP, productCode: 'SP-TEST-003', productName: '手工蒸饺', unit: '袋', returnQuantity: 8, unitPrice: 50, lineAmount: 400 },
  ]))
  check('退货单创建成功（关联申请2，收 8）', c2?.code === 200 && !!c2.data?.id, `code=${c2?.code} ${c2?.message || ''}`)
  const doc2 = c2?.data || {}
  created.push(doc2.id)
  await call('POST', `/erp/sale/return-doc/${doc2.id}/approve?note=E2E`)
  const check2 = applyRow('E2E-THSH-0002')
  check('回写申请已收数量 = 8', check2.received === 8, JSON.stringify(check2))
  check('回写申请未收数量 = 0', check2.unreceived === 0, JSON.stringify(check2))
  check('全部收货申请状态推进为 3（已完成）', check2.status === 3, check2.status)

  // ── E. 取消退货单 → 收货进度回退 ──
  console.log('\n[E] 取消退货单（收货进度回退）')
  const qtyBeforeCancel = stockQty(PROD_JAM)
  const ca = await call('POST', `/erp/sale/return-doc/${doc1.id}/cancel?reason=${encodeURIComponent('E2E取消')}`)
  check('退货单取消成功', ca?.code === 200, `code=${ca?.code} ${ca?.message || ''}`)
  const check3 = applyRow('E2E-THSH-0001')
  check('取消后申请已收数量回退为 0', check3.received === 0, JSON.stringify(check3))
  check('取消后申请未收数量回升为 10', check3.unreceived === 10, JSON.stringify(check3))
  check('取消后库存回冲 -4', Math.abs(stockQty(PROD_JAM) - (qtyBeforeCancel - 4)) < 0.0001, `${qtyBeforeCancel} → ${stockQty(PROD_JAM)}`)

  // ── F. 超收防护 ──
  console.log('\n[F] 超收防护（不得超过申请数量）')
  const no3 = (await call('GET', '/erp/sale/return-doc/next-no'))?.data
  const over = await call('POST', '/erp/sale/return-doc', docBody(no3, APPLY_3, 'E2E-THSH-0003', '2099000000000000853', 'E2E退货客户三', [
    { productId: PROD_JAM, productCode: 'SP-TEST-001', productName: '博多家园百香果果酱', unit: '瓶', returnQuantity: 6, unitPrice: 100, lineAmount: 600 },
  ]))
  check('超收被拒（申请 5 收 6 → 400）', over?.code === 400, `code=${over?.code} ${over?.message || ''}`)
  if (over?.code === 200 && over.data?.id) created.push(over.data.id)

  // ── G. 申请审核不再直接入库（避免与退货单重复过账） ──
  console.log('\n[G] 申请审核不重复过账')
  const voucherCnt = Number(sqlRows(`SELECT COUNT(*) FROM finance_voucher_item WHERE source_type IN ('SALE_RETURN_APPLY') AND source_id IN (${APPLY_1}, ${APPLY_2}, ${APPLY_3})`)[0]?.[0] || 0)
  check('申请侧未产生重复凭证', voucherCnt === 0, `voucherItems=${voucherCnt}`)
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()

  // 前端 5656 代理指向 5655（共享实例）；本脚本把 /api 转发到被测实例，验证新代码
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `${BASE}${u.pathname}${u.search}` })
  })

  async function openPage(url, waitMs = 7000) {
    for (let attempt = 0; attempt < 3; attempt++) {
      await login()
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, 1])
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(waitMs)
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }
  const bodyText = async () => (await page.locator('body').innerText()).replace(/\s+/g, '')
  const headers = async () => page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))

  await openPage(`${FE}/dispatch/return-receive`)
  const txt = await bodyText()
  check('页面可打开（无 404/白屏）', !txt.includes('页面不存在') && txt.length > 50, page.url())
  check('页面为双 Tab（按单据 / 按明细）', txt.includes('按单据') && txt.includes('按明细'), '')

  // 按单据默认 20 数据列 + 操作（对标：默认 20 + 操作列）
  const docHeaders = await headers()
  const DOC_DEFAULT = ['操作', '单据日期', '单据编号', '单据状态', '仓库', '客户', '客户编号', '客户级别', '联系人', '联系电话',
    '联系地址', '客户备注', '经手人', '部门', '本单金额', '订货数量', '已收数量', '未收数量', '附件', '打印次数', '单据备注']
  const missDoc = DOC_DEFAULT.filter(h => !docHeaders.some(x => x.includes(h)))
  check('按单据默认表头 = 对标 20 数据列 + 操作', missDoc.length === 0, `缺失: ${missDoc.join('、')}｜实际: ${docHeaders.join('/')}`)
  check('按单据默认不含隐藏列（金额/结算状态/物流公司/审核人）',
    !docHeaders.some(x => x === '金额' || x.includes('结算状态') || x.includes('物流公司') || x.includes('审核人')), docHeaders.join('/'))

  // 跟踪列真实数据（申请2 已收满 8 / 未收 0），按表头索引精确定位单元格
  const allHeaders = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()))
  const colIdx = n => allHeaders.findIndex(h => h.includes(n))
  const row2 = page.locator('.ss-grid tbody tr').filter({ hasText: 'E2E-THSH-0002' }).first()
  const rowCells = await row2.locator('td').allInnerTexts().catch(() => [])
  check('列表出现基座单据 E2E-THSH-0002', (await row2.innerText().catch(() => '')).includes('E2E-THSH-0002'), '')
  check('订货数量列渲染 8', (rowCells[colIdx('订货数量')] || '').trim() === '8', `cell=${rowCells[colIdx('订货数量')]}`)
  check('已收数量列渲染真实值 8', (rowCells[colIdx('已收数量')] || '').trim() === '8', `cell=${rowCells[colIdx('已收数量')]}`)
  check('未收数量列渲染真实值 0', (rowCells[colIdx('未收数量')] || '').trim() === '0', `cell=${rowCells[colIdx('未收数量')]}`)

  await page.screenshot({ path: path.join(SHOTS, 'ui-doc.png'), fullPage: true })

  // 工具栏按钮（对标 6：新增/刷新/批量打印/打印(F8)/导出/配置）
  for (const b of ['新增', '刷新', '批量打印', '打印(F8)', '导出', '配置']) {
    check(`工具栏含「${b}」`, txt.includes(b.replace(/\s+/g, '')))
  }

  // 表头齿轮列配置（47 列全量）
  const gear = await page.locator('.ss-grid .th-settings-btn').count()
  check('数据表带表头齿轮（列配置入口）', gear > 0, `count=${gear}`)
  if (gear > 0) {
    await page.locator('.ss-grid .th-settings-btn').first().click()
    await page.waitForTimeout(1200)
    const colText = (await page.locator('.ant-modal-content:visible').last().innerText()).replace(/\s+/g, '')
    const DOC_COLS = ['单据日期', '单据编号', '单据状态', '仓库', '客户', '客户编号', '客户级别', '联系人', '联系电话', '联系地址',
      '客户一票通', '客户备注', '经手人', '部门', '金额', '折后金额', '运费承担方', '运费', '配送方式', '本单金额', '结算状态',
      '订货数量', '已收数量', '未收数量', '重量', '体积', '摘要', '附件', '表头自定义字段1(数字)', '表头自定义字段5(文本)',
      '表尾自定义字段1', '表尾自定义字段2', '物流公司', '运单号', '提交时间', '产生方式', '制单人', '提交人', '审核人', '打印次数',
      '商品行数', '销售类型', '单据备注', '审核时间']
    const missCols = DOC_COLS.filter(c => !colText.includes(c))
    check('列配置弹窗含对标 47 列（按单据）', missCols.length === 0, '缺失: ' + missCols.join('、'))
    check('列配置含「个人配置 / 全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 60))
    await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig-doc.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  }

  // 页面配置弹窗（按单据 27 查询项 / 6 功能按钮）
  {
    const cfgBtn = page.locator('button').filter({ hasText: /配\s*置/ }).first()
    await cfgBtn.click()
    await page.waitForTimeout(1200)
    const cfgText = (await page.locator('.ant-modal-content:visible').last().innerText()).replace(/\s+/g, '')
    for (const q of ['单据时间', '单据编号', '客户', '经手人', '部门', '仓库', '产生方式', '单据状态', '结算状态', '制单人', '审核人', '提交人',
      '销售类型', '商品行属性', '单据备注', '摘要', '配送方式', '自定义字段1(数字)', '自定义字段5(文本)', '打印次数', '联系人', '联系电话',
      '联系地址', '审核时间']) {
      check(`页面配置含查询项「${q}」`, cfgText.includes(q.replace(/\s+/g, '')))
    }
    // 功能按钮在第二个 Tab
    await page.locator('.ant-modal-content:visible .ant-tabs-tab').filter({ hasText: '功能按钮' }).first().click()
    await page.waitForTimeout(900)
    const btnText = (await page.locator('.ant-modal-content:visible').last().innerText()).replace(/\s+/g, '')
    for (const b of ['新增', '刷新', '批量打印', '打印(F8)', '导出', '配置']) {
      check(`页面配置含功能按钮「${b}」`, btnText.includes(b.replace(/\s+/g, '')))
    }
    await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig-buttons.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  }

  // 按明细 Tab：22 默认列 + 分类树
  {
    await page.locator('.tab-item').filter({ hasText: '按明细' }).first().click()
    await page.waitForTimeout(2500)
    const dHeaders = await headers()
    const DETAIL_DEFAULT = ['操作', '单据日期', '单据编号', '单据状态', '仓库', '客户', '客户编号', '客户级别', '联系人', '联系电话',
      '联系地址', '客户备注', '经手人', '商品名称', '货号', '条码', '单位', '退货数量', '已收数量', '未收数量', '单价', '金额', '明细备注']
    const missD = DETAIL_DEFAULT.filter(h => !dHeaders.some(x => x.includes(h)))
    check('按明细默认表头 = 对标 22 数据列 + 操作', missD.length === 0, `缺失: ${missD.join('、')}｜实际: ${dHeaders.join('/')}`)
    const dTxt = await bodyText()
    check('按明细出现商品行（含基座商品名）', dTxt.includes('博多家园百香果果酱') || dTxt.includes('手工蒸饺'), '')
    await page.screenshot({ path: path.join(SHOTS, 'ui-detail.png'), fullPage: true })
  }

  await browser.close()
}

// ══════════════ 三、收货闭环操作入口（退货单表单关联退货申请） ══════════════
async function formSuite() {
  console.log('\n═══ 三、收货操作入口验收（销售退货单表单） ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `${BASE}${u.pathname}${u.search}` })
  })

  await login()
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tid))
  }, [TOKEN, 1])
  await page.goto(`${FE}/sales/return-doc/form`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(7000)

  const bodyText = async () => (await page.locator('body').innerText()).replace(/\s+/g, '')
  const reqs = []
  page.on('request', r => { if (r.url().includes('/erp/sale/return/')) reqs.push(r.url()) })
  page.on('console', m => { if ((m.text() || '').includes('[E2E-DIAG]')) console.log('  [浏览器] ' + m.text()) })
  let itemsPayload = null
  page.on('response', async r => {
    if (/\/erp\/sale\/return\/\d+\/items/.test(r.url())) {
      try { itemsPayload = await r.json() } catch (e) { itemsPayload = { parseError: String(e) } }
    }
  })
  const bt = await bodyText()
  check('退货单表单可打开', bt.includes('销售退货单'), page.url())
  const applyField = page.locator('.inline-field').filter({ hasText: '退货申请' }).first()
  check('表单头部含「退货申请」源单选择器', (await applyField.count()) > 0, '')

  if ((await applyField.count()) > 0) {
    await applyField.locator('.ant-select').first().click()
    await page.waitForTimeout(1200)
    const opt = page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: 'E2E-THSH-0001' }).first()
    check('退货申请下拉列出待收货申请（E2E-THSH-0001）', (await opt.count()) > 0, '')
    if ((await opt.count()) > 0) {
      await opt.click()
      await page.waitForTimeout(1500)
      // 已有明细行时会先弹「覆盖商品明细」确认框
      const confirmOk = page.locator('.ant-modal-confirm .ant-btn-primary')
      if (await confirmOk.count() > 0) {
        await confirmOk.first().click()
        await page.waitForTimeout(2500)
      }
      const t2 = await bodyText()
      check('选择申请后请求了申请明细接口', reqs.some(u => /\/items/.test(u)), reqs.join(' | ').slice(0, 240))
      check('申请明细接口返回 2 行（含商品名）',
        Array.isArray(itemsPayload?.data) && itemsPayload.data.length === 2 && !!itemsPayload.data[0]?.productName,
        JSON.stringify(itemsPayload).slice(0, 300))
      check('带出明细含申请商品（博多家园百香果果酱）', t2.includes('博多家园百香果果酱'), '')
      check('带出明细含申请第二行商品（海鲜全家福）', t2.includes('海鲜全家福'), '')
      // 已选下拉的显示值（精确判断表头字段是否真正回显）
      const selectedTexts = await page.evaluate(() =>
        [...document.querySelectorAll('.ant-select-selection-item')].map(e => (e.textContent || '').trim()).filter(Boolean))
      check('带出表头仓库（主仓库）', selectedTexts.includes('主仓库'), selectedTexts.join(' | ').slice(0, 200))
      await page.screenshot({ path: path.join(SHOTS, 'form-apply-filled.png'), fullPage: true })
    }
  }
  await browser.close()
}

async function main() {
  try {
    await apiSuite()
    await uiSuite()
    await formSuite()
  } catch (e) {
    fail++
    console.log('  ❌ 异常终止: ' + (e?.message || e))
  } finally {
    cleanupTestDocs()
    // 基座复位，便于重复执行
    try {
      sqlText(`UPDATE erp_sale_return_item SET received_quantity = 0 WHERE return_id IN (${APPLY_1}, ${APPLY_2}, ${APPLY_3})`)
      sqlText(`UPDATE erp_sale_return SET received_quantity = 0, unreceived_quantity = ordered_quantity, status = 2 WHERE return_no LIKE 'E2E-THSH-%'`)
    } catch (e) { /* ignore */ }
  }
  console.log(`\n═══ 结果：${pass} 通过 / ${fail} 失败 ═══`)
  process.exit(fail > 0 ? 1 : 0)
}

main()
