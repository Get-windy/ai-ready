/**
 * 销售退货单金标准验收
 * 覆盖：号段 → 创建 → 提交 → 审核（库存+凭证） → 完成 → 取消（回冲+反向凭证） → 红冲过滤 → 明细/复制链路
 * 传输层用 curl（共享环境下 Node fetch 偶发挂起，curl 稳定），JSON 解析与断言在 Node 内完成。
 * 用法：node verify-sale-return-doc.cjs   （默认打 5655；可用 E2E_BASE 指向自建实例）
 * 说明：请求体写 UTF-8 文件后用 -d @file 提交 —— Windows 命令行参数会按本地代码页编码，含中文的 JSON 直传会被服务端判为「请求参数格式错误」。
 */
const BASE = process.env.E2E_BASE || 'http://localhost:5655'
const fs = require('fs')
const { execFileSync } = require('child_process')
const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe'
const CURL = 'curl.exe'
const PGENV = Object.assign({}, process.env, { PGPASSWORD: 'devuser123' })

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
const MARK = 'E2E-SRD'

let pass = 0
let fail = 0
const lines = []
function log(s) { lines.push(s); console.log(s) }
function check(name, ok, detail) {
  if (ok) { pass++; log('  [OK] ' + name + (detail ? ' — ' + detail : '')) }
  else { fail++; log('  [NG] ' + name + (detail ? ' — ' + detail : '')) }
}

function sql(q) {
  const out = execFileSync(PSQL, ['-h', '127.0.0.1', '-p', '5432', '-U', 'devuser', '-d', 'devdb', '-t', '-A', '-F', '|', '-c', q],
    { env: PGENV, encoding: 'utf8' })
  return String(out).trim()
}
function rows(q) {
  const t = sql(q)
  return t ? t.split(/\r?\n/).map(l => l.split('|')) : []
}
function invQty() {
  const r = rows(`SELECT COALESCE(SUM(quantity),0) FROM wms_inventory WHERE product_id=${PRODUCT_ID} AND warehouse_id=${WAREHOUSE_ID}`)
  return Number(r[0] ? r[0][0] : 0)
}

let token = ''
// 请求体写入 UTF-8 文件后用 -d @file 提交：Windows 下把含中文的 JSON 作为命令行参数传递会被本地代码页破坏
const BODY_FILE = 'C:/Users/Administrator/_srd_body.json'
function httpJson(method, path, body) {
  const args = ['-s', '-m', '40', '-X', method, BASE + '/api' + path, '-H', 'Content-Type: application/json; charset=UTF-8']
  if (token) args.push('-H', 'Authorization: Bearer ' + token)
  if (body !== undefined) {
    fs.writeFileSync(BODY_FILE, JSON.stringify(body), 'utf8')
    args.push('-d', '@' + BODY_FILE)
  }
  const out = execFileSync(CURL, args, { encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 })
  try {
    return JSON.parse(out)
  } catch (e) {
    return { code: -1, message: 'non-json: ' + String(out).slice(0, 200) }
  }
}

function relogin() {
  const cap = httpJson('GET', '/auth/captcha')
  const d = cap.data || cap
  const m = String(d.img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  const svg = m ? Buffer.from(m[1], 'base64').toString('utf8') : ''
  const captcha = [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
  const res = httpJson('POST', '/auth/login', {
    username: 'sra_e2e', password: 'admin123', tenantName: '系统租户', captcha: captcha, captchaKey: d.uuid,
  })
  token = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue || ''
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
}
function call(method, path, body) {
  let res = httpJson(method, path, body)
  if (res && res.code === 401) { relogin(); res = httpJson(method, path, body) }
  return res
}

function cleanup(ids) {
  // 雪花ID必须以字符串参与 SQL：转 Number 会丢精度导致 DELETE 匹配不到任何行
  const list = (ids || []).map(String).filter(s => s && s !== 'undefined' && s !== 'null')
  const olds = rows(`SELECT id FROM erp_sale_return_doc WHERE summary LIKE '${MARK}%' OR remark='${MARK}'`).map(r => r[0]).filter(Boolean)
  olds.forEach(o => { if (list.indexOf(o) < 0) list.push(o) })
  if (!list.length) return
  const inList = list.join(',')
  const vids = rows(`SELECT DISTINCT voucher_id FROM finance_voucher_item WHERE source_id IN (${inList})`).map(r => r[0]).filter(Boolean)
  sql(`DELETE FROM finance_voucher_item WHERE source_id IN (${inList})`)
  if (vids.length) {
    sql(`DELETE FROM finance_voucher WHERE id IN (${vids.join(',')}) AND NOT EXISTS (SELECT 1 FROM finance_voucher_item i WHERE i.voucher_id = finance_voucher.id)`)
  }
  // 已过账且未取消的单据：直接删单据会让库存留在已入库状态，先按明细回冲库存，保证脚本可重复执行且无残留漂移
  sql(`UPDATE wms_inventory wi SET quantity = wi.quantity - d.qty
       FROM (SELECT i.product_id, r.warehouse_id, SUM(i.return_quantity) AS qty
             FROM erp_sale_return_doc r JOIN erp_sale_return_doc_item i ON i.return_doc_id = r.id
             WHERE r.id IN (${inList}) AND r.bookkeeping_time IS NOT NULL AND r.status <> 4
             GROUP BY i.product_id, r.warehouse_id) d
       WHERE wi.product_id = d.product_id AND wi.warehouse_id = d.warehouse_id`)
  sql(`DELETE FROM erp_sale_return_doc_item WHERE return_doc_id IN (${inList})`)
  sql(`DELETE FROM erp_sale_return_doc WHERE id IN (${inList})`)
}

function docBody(no) {
  return {
    returnDocNo: no,
    customerId: CUSTOMER_ID, customerName: CUSTOMER_NAME, customerCode: 'C002', customerLevel: 'A',
    warehouseId: WAREHOUSE_ID, warehouseName: WAREHOUSE_NAME,
    handlerId: 1, handlerName: 'admin',
    orderDate: '2026-09-10T00:00:00',
    salesType: 'return', generateType: '手动创建',
    status: 0, remark: MARK, summary: MARK + ' 销售退货单',
    receiverName: '收货人A', receiverPhone: '13800000000', shippingAddress: '测试地址',
    items: [{
      productId: PRODUCT_ID, productCode: PRODUCT_CODE, productName: PRODUCT_NAME, unit: '瓶',
      returnQuantity: QTY, unitPrice: PRICE, lineAmount: QTY * PRICE,
      refCostPrice: 80, refCostAmount: 80 * QTY, itemRemark: 'E2E 明细', productLineAttr: '正常',
    }],
  }
}

function main() {
  relogin()
  log('登录成功（BASE=' + BASE + '）')
  cleanup(null)
  log('历史测试数据已清理')

  const qty0 = invQty()
  const created = []
  try {
    log('')
    log('[A] 单据号段')
    const no1 = call('GET', '/erp/sale/return-doc/next-no').data
    check('号段格式 XSTHD-yyyyMMdd-NNNN', /^XSTHD-\d{8}-\d{4}$/.test(String(no1)), String(no1))

    log('')
    log('[B] 创建退货单')
    const c1 = call('POST', '/erp/sale/return-doc', docBody(no1))
    check('创建接口 200', c1.code === 200, 'code=' + c1.code + ' ' + (c1.message || ''))
    const doc = c1.data
    created.push(doc && String(doc.id))
    check('单号原样落库（非 UUID）', doc && doc.returnDocNo === no1, String(doc && doc.returnDocNo))
    check('主表金额=明细汇总', Number(doc && doc.totalAmount) === QTY * PRICE, 'totalAmount=' + (doc && doc.totalAmount))
    check('主表数量汇总', Number(doc && doc.totalQuantity) === QTY, 'totalQuantity=' + (doc && doc.totalQuantity))
    check('明细落库', (doc && doc.items && doc.items.length) === 1, 'items=' + (doc && doc.items && doc.items.length))
    check('制单人回填', !!(doc && doc.creatorName), String(doc && doc.creatorName))
    const no2 = call('GET', '/erp/sale/return-doc/next-no').data
    check('号段推进 +1', Number(String(no2).slice(-4)) === Number(String(no1).slice(-4)) + 1, no1 + ' → ' + no2)

    log('')
    log('[C] 提交 → 审核（库存 + 凭证）')
    const sub = call('POST', '/erp/sale/return-doc/' + doc.id + '/submit')
    check('提交后 status=1', sub.data && sub.data.status === 1, 'status=' + (sub.data && sub.data.status))
    const app = call('POST', '/erp/sale/return-doc/' + doc.id + '/approve?note=' + encodeURIComponent('E2E审核'))
    check('审核接口 200', app.code === 200, 'code=' + app.code + ' ' + (app.message || ''))
    check('审核后 status=2', app.data && app.data.status === 2, 'status=' + (app.data && app.data.status))
    check('bookkeepingTime 非空', !!(app.data && app.data.bookkeepingTime), String(app.data && app.data.bookkeepingTime))
    check('记账人 bookkeeperName 回填', !!(app.data && app.data.bookkeeperName), String(app.data && app.data.bookkeeperName))
    check('审核人/审核时间回填', !!(app.data && app.data.auditorName && app.data.auditTime),
      (app.data && app.data.auditorName) + ' / ' + (app.data && app.data.auditTime))

    const qty1 = invQty()
    check('库存增加 ' + QTY + '（退货入库）', Math.abs(qty1 - (qty0 + QTY)) < 0.0001, qty0 + ' → ' + qty1)

    const vr = rows(`SELECT v.status, i.subject_code, i.debit_amount, i.credit_amount, i.aux_unit
      FROM finance_voucher v JOIN finance_voucher_item i ON i.voucher_id = v.id
      WHERE i.source_type='SALE_RETURN_DOC' AND i.source_id=${doc.id} ORDER BY i.subject_code`)
    check('冲销凭证 4 分录', vr.length === 4, 'count=' + vr.length)
    const vm = {}
    vr.forEach(r => { vm[r[1]] = { d: Number(r[2]), c: Number(r[3]), aux: r[4] } })
    check('Dr.6001 = 退货金额', vm['6001'] && vm['6001'].d === QTY * PRICE, '借 ' + (vm['6001'] && vm['6001'].d))
    check('Cr.1122 = 退货金额（挂客户核算项）', vm['1122'] && vm['1122'].c === QTY * PRICE && vm['1122'].aux === CUSTOMER_NAME,
      '贷 ' + (vm['1122'] && vm['1122'].c) + ' aux=' + (vm['1122'] && vm['1122'].aux))
    check('Dr.1403 = 退货成本', vm['1403'] && vm['1403'].d === COST, '借 ' + (vm['1403'] && vm['1403'].d))
    check('Cr.6401 = 退货成本', vm['6401'] && vm['6401'].c === COST, '贷 ' + (vm['6401'] && vm['6401'].c))
    const td = vr.reduce((s, r) => s + Number(r[2]), 0)
    const tc = vr.reduce((s, r) => s + Number(r[3]), 0)
    check('凭证借贷平衡', Math.abs(td - tc) < 0.005, '借 ' + td + ' / 贷 ' + tc)
    check('凭证已过账', vr.length > 0 && vr.every(r => r[0] === 'posted'), '状态=' + [...new Set(vr.map(r => r[0]))].join('/'))

    log('')
    log('[D] 查询条件（含原空转参数 EXISTS 下推）')
    const q1 = call('GET', '/erp/sale/return-doc/page?pageNum=1&pageSize=10&keyword=' + no1)
    check('keyword 命中', (q1.data && q1.data.records.length) > 0, 'records=' + (q1.data && q1.data.records.length))
    const q2 = call('GET', '/erp/sale/return-doc/page?pageNum=1&pageSize=10&productName=' + encodeURIComponent('博多'))
    check('productName 生效', (q2.data && q2.data.records.length) > 0, 'records=' + (q2.data && q2.data.records.length))
    const q3 = call('GET', '/erp/sale/return-doc/page?pageNum=1&pageSize=10&productName=' + encodeURIComponent('不存在XYZ'))
    check('productName 不匹配为 0', (q3.data && q3.data.records.length) === 0, 'records=' + (q3.data && q3.data.records.length))
    const q4 = call('GET', '/erp/sale/return-doc/page?pageNum=1&pageSize=10&itemRemark=' + encodeURIComponent('E2E 明细'))
    check('itemRemark 生效', (q4.data && q4.data.records.length) > 0, 'records=' + (q4.data && q4.data.records.length))
    const q5 = call('GET', '/erp/sale/return-doc/page?pageNum=1&pageSize=10&categoryId=' + CATEGORY_ID)
    check('categoryId 生效', (q5.data && q5.data.records.length) > 0, 'records=' + (q5.data && q5.data.records.length))
    const q6 = call('GET', '/erp/sale/return-doc/page?pageNum=1&pageSize=10&categoryId=999999')
    check('categoryId 不匹配为 0', (q6.data && q6.data.records.length) === 0, 'records=' + (q6.data && q6.data.records.length))
    const q7 = call('GET', '/erp/sale/return-doc/page?pageNum=1&pageSize=10&bookkeeperName=' + encodeURIComponent(String(app.data.bookkeeperName)))
    check('bookkeeperName 生效', (q7.data && q7.data.records.length) > 0, 'records=' + (q7.data && q7.data.records.length))

    log('')
    log('[E] 完成 + 状态门控')
    const cmp = call('POST', '/erp/sale/return-doc/' + doc.id + '/complete')
    check('完成后 status=3', cmp.data && cmp.data.status === 3, 'status=' + (cmp.data && cmp.data.status))
    check('完成后库存不二次过账', Math.abs(invQty() - qty1) < 0.0001, '库存=' + invQty())
    const c400 = call('POST', '/erp/sale/return-doc/' + doc.id + '/cancel?reason=' + encodeURIComponent('x'))
    check('已完成不可取消（业务异常 400）', c400.code === 400, 'code=' + c400.code + ' ' + (c400.message || ''))

    log('')
    log('[F] 第二张单：审核后取消（回冲 + 反向凭证）')
    const no3 = call('GET', '/erp/sale/return-doc/next-no').data
    const c2 = call('POST', '/erp/sale/return-doc', docBody(no3))
    const doc2 = c2.data
    created.push(doc2 && String(doc2.id))
    check('第二张单创建', c2.code === 200 && !!doc2.id, 'no=' + (doc2 && doc2.returnDocNo))
    call('POST', '/erp/sale/return-doc/' + doc2.id + '/submit')
    const app2 = call('POST', '/erp/sale/return-doc/' + doc2.id + '/approve?note=' + encodeURIComponent('E2E审核2'))
    check('第二张单审核通过', app2.data && app2.data.status === 2, 'status=' + (app2.data && app2.data.status))
    const qtyA = invQty()
    const can = call('POST', '/erp/sale/return-doc/' + doc2.id + '/cancel?reason=' + encodeURIComponent('E2E取消'))
    check('取消后 status=4', can.data && can.data.status === 4, 'status=' + (can.data && can.data.status))
    const qtyB = invQty()
    check('库存回冲 -' + QTY, Math.abs(qtyB - (qtyA - QTY)) < 0.0001, qtyA + ' → ' + qtyB)
    const rr = rows(`SELECT i.subject_code, i.debit_amount, i.credit_amount FROM finance_voucher v
      JOIN finance_voucher_item i ON i.voucher_id = v.id
      WHERE i.source_type='SALE_RETURN_DOC_CANCEL' AND i.source_id=${doc2.id}`)
    check('反向凭证 4 分录', rr.length === 4, 'count=' + rr.length)
    const rm = {}
    rr.forEach(r => { rm[r[0]] = { d: Number(r[1]), c: Number(r[2]) } })
    check('反向凭证 Dr.1122 / Cr.6001', rm['1122'] && rm['1122'].d === QTY * PRICE && rm['6001'] && rm['6001'].c === QTY * PRICE,
      '1122借=' + (rm['1122'] && rm['1122'].d) + ' 6001贷=' + (rm['6001'] && rm['6001'].c))
    const dup = call('POST', '/erp/sale/return-doc/' + doc2.id + '/cancel?reason=' + encodeURIComponent('y'))
    check('重复取消被拦截（400）', dup.code === 400, 'code=' + dup.code + ' ' + (dup.message || ''))

    log('')
    log('[G] 显示红冲')
    const off = call('GET', '/erp/sale/return-doc/page?pageNum=1&pageSize=10&keyword=' + no3)
    check('默认隐藏红冲单据', (off.data && off.data.records.length) === 0, 'records=' + (off.data && off.data.records.length))
    const on = call('GET', '/erp/sale/return-doc/page?pageNum=1&pageSize=10&keyword=' + no3 + '&showRedFlush=true')
    check('勾选显示红冲可见', (on.data && on.data.records.length) > 0, 'records=' + (on.data && on.data.records.length))
    const st = call('GET', '/erp/sale/return-doc/page?pageNum=1&pageSize=10&keyword=' + no3 + '&status=4&showRedFlush=true')
    check('按已取消状态筛选命中', (st.data && st.data.records.length) > 0, 'records=' + (st.data && st.data.records.length))

    log('')
    log('[H] 明细页 + 复制链路')
    const pd = call('GET', '/erp/sale/return-doc/page-detail?pageNum=1&pageSize=50&returnDocNo=' + no1)
    check('明细页返回商品行', (pd.data && pd.data.records.length) > 0, 'records=' + (pd.data && pd.data.records.length))
    const det = call('GET', '/erp/sale/return-doc/' + doc.id)
    check('详情返回主表+明细', !!det.data && det.data.items.length === 1, 'items=' + (det.data && det.data.items.length))
    check('详情返回单据编号（编辑回显）', det.data && det.data.returnDocNo === no1, String(det.data && det.data.returnDocNo))
  } catch (e) {
    fail++
    log('  [NG] 脚本异常: ' + (e && e.stack ? e.stack : e))
  } finally {
    cleanup(created)
    log('')
    log('清理完成，库存=' + invQty() + '（初始 ' + qty0 + '）')
  }

  log('')
  log('══════ 验收结果：' + pass + ' 通过 / ' + fail + ' 失败 ══════')
  fs.writeFileSync('I:/AI-Ready/srd-e2e-result.txt', 'pass=' + pass + ' fail=' + fail + '\n' + lines.join('\n'))
}

main()
