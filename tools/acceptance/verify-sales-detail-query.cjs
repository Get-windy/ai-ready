/* 销售明细查询页金标准 接口验证（直连 API，独立脚本） */
const http = require('http')

const PORT = 5655
const BASE = '/sales/detail-query'

function apiReq(method, path, body, token, raw) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        if (raw) return resolve({ status: res.statusCode, headers: res.headers, buf })
        try { resolve(JSON.parse(buf.toString('utf8'))) } catch { resolve({ raw: buf.toString('utf8'), status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login() {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  return token
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}
const qs = (o) => Object.entries(o).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')
const num = (v) => Number(v)

async function main() {
  const token = await login()
  console.log('登录成功\n')

  // ═══ A. 基础分页与 96 列 ═══
  const page = await apiReq('GET', `${BASE}/page?current=1&size=5`, null, token)
  const totalAll = num(page.total)
  const rec = (page.records || [])[0]
  check('分页接口可用', Array.isArray(page.records), `total=${totalAll} 行`)
  check('分页 total 为明细行数', totalAll > 0, `total=${totalAll}`)
  check('分页按明细行分页（返回行数 ≤ size）', (page.records || []).length <= 5, `返回=${(page.records || []).length}`)
  const page2 = await apiReq('GET', `${BASE}/page?current=2&size=2`, null, token)
  check('第2页数据与第1页不重复', (page2.records || []).length === 0 || page2.records[0].docNo !== rec?.docNo || page2.records[0].lineKey !== rec?.lineKey,
    `page2=${(page2.records || []).length} 行`)

  const expectedKeys = ['docDate', 'docNo', 'docType', 'sourceOrderDate', 'defaultHandlerName', 'settlementStatus',
    'deliveryMethod', 'salesType', 'itemExtNum6', 'itemExtNum7', 'itemExtPartner', 'itemExtStaff', 'itemExtDept',
    'salesQuantity', 'unitPrice', 'amount', 'grossProfit', 'grossProfitRate', 'costAmount', 'salesRevenue',
    'priceLevel1', 'priceLevel8', 'batchBarcode', 'productionDate', 'expiryDate', 'printCount', 'deliveryDriver',
    'remark', 'bookkeeperName', 'creatorName', 'settlementCompleteTime']
  if (rec) {
    const missing = expectedKeys.filter(k => !(k in rec))
    check('返回行含 96 列关键字段', missing.length === 0, missing.length ? '缺: ' + missing.join(',') : `${Object.keys(rec).length} 字段`)
    check('单据类型为中文且不等于销售类型', typeof rec.docType === 'string' && rec.docType !== rec.salesType,
      `单据类型=${rec.docType} 销售类型=${rec.salesType}`)
    check('结算状态中文化（非 unsettled 原文）', rec.settlementStatus !== 'unsettled', `结算状态=${rec.settlementStatus || '(空)'}`)
    check('配送方式中文化（非 express 原文）', rec.deliveryMethod !== 'express', `配送方式=${rec.deliveryMethod || '(空)'}`)
    check('来源订单日期为真实值（非硬编码 null）', rec.sourceOrderDate !== null && rec.sourceOrderDate !== undefined,
      `来源订单=${rec.sourceOrder} 日期=${rec.sourceOrderDate}`)
  }

  // ═══ B. 过滤项生效 ═══
  const t = async (q) => num((await apiReq('GET', `${BASE}/page?current=1&size=5&${qs(q)}`, null, token)).total)

  const tDoc0 = await t({ documentType: '0' })
  const tDoc1 = await t({ documentType: '1' })
  const tDoc3 = await t({ documentType: '3' })
  check('单据类型过滤生效（可按类型切分）', tDoc0 + tDoc1 === totalAll && tDoc3 === 0,
    `销售出库=${tDoc0} 换货出库=${tDoc1} 其他出库=${tDoc3} 全部=${totalAll}`)

  const tSrcPC = await t({ source: 'PC' })
  const tSrcAPI = await t({ source: 'API' })
  check('来源过滤生效（PC 存量回填 / API 无数据）', tSrcPC === totalAll && tSrcAPI === 0,
    `PC=${tSrcPC} API=${tSrcAPI} 全部=${totalAll}`)

  const tIndMock = await t({ industryCategory: '不存在的行业类别XYZ' })
  check('行业类别过滤生效（无匹配=0 行，非全量）', tIndMock === 0, `无匹配 total=${tIndMock}`)

  const tHandlerMock = await t({ defaultHandlerName: '不存在的经手人XYZ' })
  check('默认经手人过滤生效（无匹配=0 行）', tHandlerMock === 0, `无匹配 total=${tHandlerMock}`)

  const tExt6 = await t({ itemExtNum6Min: '99999' })
  check('表体自定义6 数值区间过滤生效', tExt6 === 0, `total=${tExt6}`)

  const tExt7 = await t({ itemExtNum7Min: '99999' })
  check('表体自定义7 数值区间过滤生效', tExt7 === 0, `total=${tExt7}`)

  const tExtPd = await t({ itemExtPartner: '999999' })
  check('表体自定义8（往来单位）过滤生效', tExtPd === 0, `total=${tExtPd}`)

  const tExtStaff = await t({ itemExtStaff: '999999' })
  check('表体自定义9（职员）过滤生效', tExtStaff === 0, `total=${tExtStaff}`)

  const tExtDept = await t({ itemExtDept: '999999' })
  check('表体自定义10（部门）过滤生效', tExtDept === 0, `total=${tExtDept}`)

  if (rec) {
    const firstWord = String(rec.productName || '').slice(0, 2)
    const tProduct = await t({ productName: firstWord })
    check('商品名称过滤生效（有匹配）', tProduct > 0, `商品"${firstWord}" total=${tProduct}`)
    const tDocNo = await t({ documentNo: rec.docNo })
    check('单据编号过滤生效', tDocNo > 0, `${rec.docNo} total=${tDocNo}`)
  }

  // 正向验证（含红冲口径，覆盖有数据的样本）+ 展示中文化
  const red = await apiReq('GET', `${BASE}/page?current=1&size=3&${qs({ showRed: 'true' })}`, null, token)
  const redTotal = num(red.total)
  const redRow = (red.records || [])[0] || {}
  check('显示红冲开关生效（开启后行数增加）', redTotal > totalAll, `${totalAll} → ${redTotal}`)
  check('结算状态中文化', redRow.settlementStatus === '未结算', `值=${redRow.settlementStatus}`)
  check('配送方式中文化', redRow.deliveryMethod === '快递', `值=${redRow.deliveryMethod}`)
  check('产生方式为真实值', redRow.generationMethod === '手工创建' || redRow.generationMethod === '订单生成', `值=${redRow.generationMethod}`)

  const tSettle = await t({ settlementStatus: 'unsettled', showRed: 'true' })
  const tSettleMiss = await t({ settlementStatus: 'paid', showRed: 'true' })
  check('结算状态过滤生效（可切分）', tSettle > 0 && tSettleMiss === 0, `未结算=${tSettle} 已结算=${tSettleMiss}`)
  const tDelivery = await t({ deliveryMethod: 'express', showRed: 'true' })
  check('配送方式过滤生效', tDelivery > 0, `快递=${tDelivery}`)
  const tGen = await t({ generationMethod: '手工创建', showRed: 'true' })
  check('产生方式过滤生效', tGen > 0, `手工创建=${tGen}`)
  const tDocType0 = await t({ documentType: '0', showRed: 'true' })
  check('单据类型过滤正向命中（销售出库）', tDocType0 > 0, `销售出库=${tDocType0}`)

  // ═══ C. 导出真实 xlsx ═══
  const exp = await apiReq('GET', `${BASE}/export`, null, token, true)
  const ct = String(exp.headers['content-type'] || '')
  check('导出返回 Excel 流', exp.status === 200 && ct.includes('spreadsheetml'), `status=${exp.status} type=${ct.split(';')[0]}`)
  check('导出文件为有效 xlsx（PK 头 + 非空）', exp.buf.length > 2000 && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b,
    `size=${exp.buf.length}`)
  const expFiltered = await apiReq('GET', `${BASE}/export?${qs({ documentType: '3' })}`, null, token, true)
  check('导出与查询同一过滤口径（空结果仍返回 xlsx）', expFiltered.status === 200 && expFiltered.buf[0] === 0x50,
    `size=${expFiltered.buf.length}`)

  // ═══ D. 客户主数据默认经手人闭环 ═══
  const HANDLER = '金标准默认经手人'
  const custPage = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=5&partnerType=customer', null, token)
  const cust = (custPage?.data?.records || [])[0]
  check('客户主数据可查询', !!cust, `首条 id=${cust?.id} ${cust?.partnerName}`)
  if (cust) {
    const upd = await apiReq('PUT', `/erp/md/customer/${cust.id}`, {
      partnerCode: cust.partnerCode, partnerName: cust.partnerName, partnerType: 'customer',
      defaultHandlerId: 1, defaultHandlerName: HANDLER,
    }, token)
    check('客户主数据保存默认经手人（原先静默丢弃）', upd?.data === true || upd?.success === true, JSON.stringify(upd).slice(0, 100))
    const detail = await apiReq('GET', `/erp/md/customer/${cust.id}`, null, token)
    check('客户详情回读默认经手人', detail?.data?.defaultHandlerName === HANDLER, `值=${detail?.data?.defaultHandlerName}`)

    // 该客户（含已红冲单据）的明细行应回填默认经手人
    const rows = await apiReq('GET', `${BASE}/page?current=1&size=100&${qs({ customerName: cust.partnerName, showRed: 'true' })}`, null, token)
    const rowList = rows.records || []
    check('明细行默认经手人按客户主数据回填（非硬编码 null）',
      rowList.length > 0 && rowList.every(r => r.defaultHandlerName === HANDLER),
      `行数=${rowList.length} 值=${rowList[0]?.defaultHandlerName}`)
    const filtered = await apiReq('GET', `${BASE}/page?current=1&size=5&${qs({ defaultHandlerName: HANDLER, showRed: 'true' })}`, null, token)
    check('按默认经手人筛选命中该客户明细（筛选=行数）', num(filtered.total) === rowList.length,
      `筛选 total=${filtered.total} / 客户行数=${rowList.length}`)
  }

  // ═══ E. 左树分类过滤（categoryId） ═══
  const tCat = await t({ categoryId: '999999999' })
  check('商品分类过滤生效（无匹配=0 行）', tCat === 0, `total=${tCat}`)
  // 正向：茶酱果酱分类的商品存在于已红冲单据中，开 showRed 后应命中
  const tCatHit = await t({ categoryId: '2072844550224740354', showRed: 'true' })
  const tCatMissRedOff = await t({ categoryId: '2072844550224740354' })
  check('商品分类过滤可命中（父/子分类覆盖）', tCatHit > 0 && tCatMissRedOff === 0,
    `命中=${tCatHit} 默认口径=${tCatMissRedOff}`)

  // ═══ 汇总 ═══
  const failed = results.filter(r => !r.ok)
  console.log(`\n结果: ${results.length - failed.length}/${results.length} 通过`)
  if (failed.length) {
    console.log('失败项: ' + failed.map(f => f.name).join(' | '))
    process.exit(1)
  }
}

main().catch(e => { console.error('异常:', e); process.exit(1) })
