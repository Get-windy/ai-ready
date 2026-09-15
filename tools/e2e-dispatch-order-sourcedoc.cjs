/*
 * 配送单 · 「运输执行单」建模口径 E2E（2026-09-13）
 *
 *   口径依据（业界）：SAP TM —— Freight Order（怎么运）← Freight Unit（要运什么）← Delivery（货权/库存/金额）。
 *     配送单是**运输执行单**，只承载配送执行信息（承运资源/路线/时间/里程/签收）；
 *     货值与商品明细归**上游销售出库单**，配送单不重复录入、不重算货值。
 *   · 有来源单据：表头 发货数量/发货金额/重量/体积 = 上游单据聚合；配送单量 = 上游单据数；
 *                 本单不保存商品明细（明细由上游穿透）
 *   · 无来源单据（临时配送）：保留本单货物明细与「金额=数量×单价」旧口径（回归保护）
 *
 * 验收：接口 + DB 落库核对（含多单聚合、临时配送回归、清理）
 * 用法：node tools/e2e-dispatch-order-sourcedoc.cjs        （默认后端 5680）
 *      ERP_PORT=5680 node tools/e2e-dispatch-order-sourcedoc.cjs
 */
const http = require('http')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const PORT = Number(process.env.ERP_PORT || 5680)
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
const E2E_USER = process.env.E2E_USER || 'e2e_dispatch'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const MARK = 'E2ESRC' + Date.now().toString().slice(-6)

function safePath(fullPath) {
  const idx = fullPath.indexOf('?')
  if (idx < 0) return encodeURI(fullPath)
  const path = fullPath.slice(0, idx)
  const query = fullPath.slice(idx + 1).split('&').map(kv => {
    const eq = kv.indexOf('=')
    if (eq < 0) return encodeURIComponent(kv)
    let val = kv.slice(eq + 1)
    try { val = decodeURIComponent(val) } catch (e) { /* 未编码 */ }
    return encodeURIComponent(kv.slice(0, eq)) + '=' + encodeURIComponent(val)
  }).join('&')
  return encodeURI(path) + '?' + query
}

function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: safePath('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* ignore */ }
        resolve({ status: res.statusCode, json })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function dbQuery(sql, params) {
  const client = new Client(DSN)
  await client.connect()
  try {
    const res = await client.query(sql, params)
    return res.rows
  } finally {
    await client.end()
  }
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 200) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 160) : ''}`)
}

let TOKEN = null

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 200))
  TOKEN = token
}

const num = (v) => Number(v) || 0

async function main() {
  console.log('配送单「运输执行单」口径 E2E —— 后端 :' + PORT)
  await login()

  // ── 0. 取上游销售出库单 ──
  const obRes = await rawReq('GET', '/erp/sale/outbound/page?pageNum=1&pageSize=20', null, TOKEN)
  const outbounds = (obRes.json?.records || obRes.json?.data?.records || [])
    .filter(o => num(o.totalQuantity) > 0)
  check('销售出库单（上游）数据可用', outbounds.length >= 2, `count=${outbounds.length}`)
  const A = outbounds[0]
  const B = outbounds[1]

  // ── 1. 单张来源单据 → 表头由上游聚合 ──
  const save1 = await rawReq('POST', '/dms/task/save', {
    deliveryDate: new Date().toISOString().slice(0, 10),
    orderType: 1,
    orderNo: MARK + '-1',
    customerName: A.customerName || 'E2E客户',
    remark: MARK + ' 单来源单据',
    sourceDocs: [{
      docType: 1, docId: A.id, docNo: A.outboundNo,
      quantity: num(A.totalQuantity), amount: num(A.totalAmount),
      weight: num(A.totalWeight), volume: num(A.totalVolume), boxCount: num(A.boxCount),
    }],
  }, TOKEN)
  const t1 = save1.json?.data
  check('新建配送单（带来源单据）成功', !!t1?.id, JSON.stringify(save1.json).slice(0, 160))
  if (t1) {
    check('表头 发货数量 = 上游单据数量', num(t1.totalQuantity) === num(A.totalQuantity),
      `${t1.totalQuantity} vs ${A.totalQuantity}`)
    check('表头 发货金额 = 上游单据金额', num(t1.goodsAmount) === num(A.totalAmount),
      `${t1.goodsAmount} vs ${A.totalAmount}`)
    check('表头 重量 = 上游单据重量', num(t1.totalWeight) === num(A.totalWeight),
      `${t1.totalWeight} vs ${A.totalWeight}`)
    check('表头 体积 = 上游单据体积', num(t1.totalVolume) === num(A.totalVolume),
      `${t1.totalVolume} vs ${A.totalVolume}`)
    check('配送单量 = 上游单据数（1）', num(t1.orderCount) === 1, t1.orderCount)
    check('来源单据编号自动回写', t1.sourceBillNo === A.outboundNo, t1.sourceBillNo)

    const d1 = (await rawReq('GET', `/dms/task/${t1.id}`, null, TOKEN)).json?.data || {}
    check('详情返回来源单据（sourceDocs=1）', (d1.sourceDocs || []).length === 1, (d1.sourceDocs || []).length)
    check('本单不再保存商品明细（items=0）', (d1.items || []).length === 0, (d1.items || []).length)

    const docRows = await dbQuery('SELECT doc_no, quantity, amount FROM dms_task_doc WHERE task_id=$1 AND deleted=0', [t1.id])
    const itemRows = await dbQuery('SELECT count(*)::int n FROM dms_task_item WHERE task_id=$1 AND deleted=0', [t1.id])
    check('DB 落库：dms_task_doc 有 1 行', docRows.length === 1, JSON.stringify(docRows))
    check('DB 落库：dms_task_item 为 0 行（明细归上游）', itemRows[0].n === 0, itemRows[0].n)
  }

  // ── 2. 多张来源单据 → 聚合求和 ──
  const save2 = await rawReq('POST', '/dms/task/save', {
    deliveryDate: new Date().toISOString().slice(0, 10),
    orderType: 1,
    orderNo: MARK + '-2',
    customerName: A.customerName || 'E2E客户',
    remark: MARK + ' 多来源单据',
    sourceDocs: [A, B].map(o => ({
      docType: 1, docId: o.id, docNo: o.outboundNo,
      quantity: num(o.totalQuantity), amount: num(o.totalAmount),
      weight: num(o.totalWeight), volume: num(o.totalVolume), boxCount: num(o.boxCount),
    })),
  }, TOKEN)
  const t2 = save2.json?.data
  if (t2?.id) {
    check('多来源单据：数量 = 两单之和', num(t2.totalQuantity) === num(A.totalQuantity) + num(B.totalQuantity),
      `${t2.totalQuantity} vs ${num(A.totalQuantity) + num(B.totalQuantity)}`)
    check('多来源单据：金额 = 两单之和', num(t2.goodsAmount) === num(A.totalAmount) + num(B.totalAmount),
      `${t2.goodsAmount} vs ${num(A.totalAmount) + num(B.totalAmount)}`)
    check('配送单量 = 上游单据数（2）', num(t2.orderCount) === 2, t2.orderCount)
    check('来源单据编号为多单逗号拼接', String(t2.sourceBillNo || '').includes(A.outboundNo)
      && String(t2.sourceBillNo || '').includes(B.outboundNo), t2.sourceBillNo)
  } else {
    check('多来源单据聚合', false, JSON.stringify(save2.json).slice(0, 160))
  }

  // ── 3. 无来源单据（临时配送）回归：仍按明细汇总 ──
  const save3 = await rawReq('POST', '/dms/task/save', {
    deliveryDate: new Date().toISOString().slice(0, 10),
    orderType: 1,
    orderNo: MARK + '-3',
    customerName: 'E2E临时配送客户',
    remark: MARK + ' 临时配送',
    items: [
      { productName: 'E2E临时货物甲', quantity: 2, unitPrice: 50, weight: 1, volume: 0.1 },
      { productName: 'E2E临时货物乙', quantity: 3, unitPrice: 20, weight: 0.5, volume: 0.05 },
    ],
  }, TOKEN)
  const t3 = save3.json?.data
  if (t3?.id) {
    check('临时配送：数量 = 明细汇总（2+3=5）', num(t3.totalQuantity) === 5, t3.totalQuantity)
    check('临时配送：金额 = 明细汇总（2×50+3×20=160）', num(t3.goodsAmount) === 160, t3.goodsAmount)
    const itemRows3 = await dbQuery('SELECT count(*)::int n FROM dms_task_item WHERE task_id=$1 AND deleted=0', [t3.id])
    check('临时配送：本单明细已落库（2 行）', itemRows3[0].n === 2, itemRows3[0].n)
  } else {
    check('临时配送回归', false, JSON.stringify(save3.json).slice(0, 160))
  }

  // ── 4. 清理 ──
  for (const t of [t1, t2, t3]) {
    if (t?.id) await rawReq('DELETE', `/dms/task/${t.id}`, null, TOKEN)
  }
  const left = await dbQuery('SELECT count(*)::int n FROM dms_task WHERE order_no LIKE $1 AND deleted=0', [MARK + '%'])
  check('验收数据已清理', left[0].n === 0, `left=${left[0].n}`)

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 汇总：${pass}/${results.length} 通过${fail ? `，${fail} 项失败` : ''} ═══`)
  if (fail) results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  process.exitCode = fail ? 1 : 0
}

main().catch(e => { console.error('E2E 异常终止:', e.message); process.exitCode = 1 })
