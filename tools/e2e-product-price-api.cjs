/* 商品价格管理（资料 → 商品管理 → 商品价格管理）接口端到端验证
 *
 * 覆盖四子标签：商品价格批量修改 / 客户级别折扣设置 / 级别指定价设置 / 客户指定价设置
 * 运行： node tools/e2e-product-price-api.cjs
 * 环境： API_PORT（默认 5720，本次验证实例）
 */
const http = require('http')

const API_PORT = Number(process.env.API_PORT || 5720)
let TOKEN = null
let PASS = 0
let FAIL = 0
const FAILURES = []

function rawReq(method, path, body, extraHeaders = {}, raw = false) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: API_PORT, path: '/api' + path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(TOKEN ? { Authorization: `Bearer ${TOKEN}` } : {}),
        ...extraHeaders,
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        if (raw) return resolve({ status: res.statusCode, headers: res.headers, buf: Buffer.concat(chunks) })
        const text = Buffer.concat(chunks).toString('utf8')
        try { resolve(JSON.parse(text)) } catch { resolve({ raw: text.slice(0, 300), status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

/**
 * 统一请求包装：sa-token 同一账号被并行会话顶下线时会返回 401，
 * 这里自动重登并重试一次，避免整轮验证被偶发踢线打断。
 */
async function withRetry(method, path, body, extraHeaders = {}, raw = false) {
  let res = await rawReq(method, path, body, extraHeaders, raw)
  const code = res?.code ?? res?.status
  if (code === 401) {
    await login()
    res = await rawReq(method, path, body, extraHeaders, raw)
  }
  return res
}

const get = p => withRetry('GET', p)
const post = (p, b) => withRetry('POST', p, b)
const put = (p, b) => withRetry('PUT', p, b)
const del = p => withRetry('DELETE', p)

function check(name, cond, detail) {
  if (cond) { PASS++; console.log('  ✓', name) }
  else { FAIL++; FAILURES.push(name + (detail ? ' → ' + JSON.stringify(detail).slice(0, 240) : '')); console.log('  ✗', name, detail ? JSON.stringify(detail).slice(0, 240) : '') }
}

async function login() {
  const cap = await get('/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await post('/auth/login', {
    username: process.env.E2E_USER || 'e2e_product', password: process.env.E2E_PASS || 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  TOKEN = token
  console.log('login ok')
}

async function main() {
  await login()

  // ═══ 子标签 1：商品价格批量修改 ═══
  console.log('\n[1] 商品价格批量修改')
  const page = await get('/erp/md/product-price/page?pageNum=1&pageSize=5')
  check('分页返回 200 且有记录', page.code === 200 && Array.isArray(page.data?.records) && page.data.records.length > 0, page.msg || page.message)
  const first = page.data?.records?.[0] || {}
  check('行含单位/商品/价格列', !!first.unitId && !!first.productName && first.retailPrice !== undefined, first)
  check('行含 8 个价格等级字段', ['gradePrice1', 'gradePrice2', 'gradePrice8'].every(k => k in first), Object.keys(first).slice(0, 12))
  check('行含换算关系口径字段', 'conversionRelation' in first, Object.keys(first))

  const byKeyword = await get('/erp/md/product-price/page?pageNum=1&pageSize=5&keyword=' + encodeURIComponent(first.productName || ''))
  check('关键词过滤命中', (byKeyword.data?.records || []).length > 0, byKeyword.data?.total)

  const brands = await get('/erp/md/product-price/brands')
  check('品牌下拉 200', brands.code === 200 && Array.isArray(brands.data), brands.msg)
  const grades = await get('/erp/md/product-price/customer-grades')
  check('客户级别下拉 200', grades.code === 200 && Array.isArray(grades.data), grades.msg)

  // 导出 excel
  const exp = await withRetry('GET', '/erp/md/product-price/export', null, {}, true)
  check('导出返回 xlsx 流', exp.status === 200 && exp.buf.length > 2000 && String(exp.headers['content-type']).includes('spreadsheet'), { status: exp.status, len: exp.buf?.length })

  // 批量修改：选一条「批发价与零售价都有值」的记录做规则改价（基础价为空的行会被明确拒绝）
  const allRows = (await get('/erp/md/product-price/page?pageNum=1&pageSize=200')).data?.records || []
  // 规则改价需「基础价（批发价）非空」的行；目标价列（零售价）原值可为空
  const target = allRows.find(r => Number(r.wholesalePrice) > 0) || first
  const beforeRetail = Number(target.retailPrice || 0)
  const beforeWholesale = Number(target.wholesalePrice || 0)
  const mod = await put('/erp/md/product-price/batch-modify', {
    unitIds: [String(target.unitId)],
    items: [{ field: 'retailPrice', mode: 'RULE', basePriceField: 'wholesalePrice', calcOperator: '*', calcValue: 1 }],
  })
  check('批量修改接口 200（按规则改价）', mod.code === 200, mod.msg || mod.message)

  // 基础价为空（NULL，不是 0）的行：按规则改价必须明确拒绝（不得静默 0 条）
  const emptyBaseRow = allRows.find(r => r.wholesalePrice === null || r.wholesalePrice === undefined)
  if (emptyBaseRow) {
    const emptyBase = await put('/erp/md/product-price/batch-modify', {
      unitIds: [String(emptyBaseRow.unitId)],
      items: [{ field: 'retailPrice', mode: 'RULE', basePriceField: 'wholesalePrice', calcOperator: '*', calcValue: 1 }],
    })
    check('基础价为空时按规则改价被拒绝并给出原因',
      emptyBase.code !== 200 && /基础价/.test(String(emptyBase.msg || emptyBase.message || '')), emptyBase.msg || emptyBase.message)
  }
  const after = await get('/erp/md/product-price/page?pageNum=1&pageSize=200')
  const rowAfter = (after.data?.records || []).find(r => String(r.unitId) === String(target.unitId))
  check('批量修改按规则落库（零售价=批发价×1）', rowAfter && Math.abs(Number(rowAfter.retailPrice) - beforeWholesale) < 0.01,
    { beforeRetail, beforeWholesale, after: rowAfter?.retailPrice })
  const restore = await put('/erp/md/product-price/batch-modify', {
    unitIds: [String(target.unitId)],
    items: [{ field: 'retailPrice', mode: 'FIXED', value: beforeRetail }],
  })
  check('批量修改支持直接改价（回滚原值）', restore.code === 200, restore.msg)
  const afterRestore = await get('/erp/md/product-price/page?pageNum=1&pageSize=200')
  const rowRestored = (afterRestore.data?.records || []).find(r => String(r.unitId) === String(target.unitId))
  check('直接改价结果正确', rowRestored && Math.abs(Number(rowRestored.retailPrice) - beforeRetail) < 0.01, rowRestored?.retailPrice)

  const badMod = await put('/erp/md/product-price/batch-modify', { unitIds: [], items: [] })
  check('空选中被拒绝（非 200 业务错误）', badMod.code !== 200, badMod.msg)

  // ═══ 子标签 2：客户级别折扣设置 ═══
  console.log('\n[2] 客户级别折扣设置')
  const gradeName = 'A餐饮客户'
  const saveGd = await post('/erp/md/product-price/grade-discount/save', {
    gradeName, basePriceType: '批发价', calcOperator: '*', calcValue: 1.2,
  })
  check('新增级别折扣 200', saveGd.code === 200, saveGd.msg || saveGd.message || saveGd.data)
  const gdPage = await get('/erp/md/product-price/grade-discount/page?pageNum=1&pageSize=20')
  const gdRow = (gdPage.data?.records || []).find(r => r.gradeName === gradeName)
  check('级别折扣落库且规则文本正确', !!gdRow && /批发价\*1.2/.test(gdRow.previewText || ''), gdRow)
  check('级别折扣带回最后修改时间', !!gdRow?.updateTime, gdRow?.updateTime)
  const dupGd = await post('/erp/md/product-price/grade-discount/save', {
    gradeName, basePriceType: '零售价', calcOperator: '*', calcValue: 1,
  })
  check('同级别重复新增被拒绝', dupGd.code !== 200, dupGd.msg)
  const updGd = await post('/erp/md/product-price/grade-discount/save', {
    id: gdRow?.id, gradeName, basePriceType: '零售价', calcOperator: '+', calcValue: 2,
  })
  check('修改级别折扣 200', updGd.code === 200, updGd.msg)
  const gdPage2 = await get('/erp/md/product-price/grade-discount/page?pageNum=1&pageSize=20')
  const gdRow2 = (gdPage2.data?.records || []).find(r => r.gradeName === gradeName)
  check('修改后规则文本更新', /零售价\+2/.test(gdRow2?.previewText || ''), gdRow2?.previewText)
  const gdExport = await withRetry('GET', '/erp/md/product-price/grade-discount/export', null, {}, true)
  check('级别折扣导出 xlsx', gdExport.status === 200 && gdExport.buf.length > 1000, gdExport.status)
  const delGd = await del('/erp/md/product-price/grade-discount/' + gdRow?.id)
  check('删除级别折扣 200', delGd.code === 200, delGd.msg)
  const gdPage3 = await get('/erp/md/product-price/grade-discount/page?pageNum=1&pageSize=20')
  check('删除后不再出现', !(gdPage3.data?.records || []).some(r => r.gradeName === gradeName), gdPage3.data?.total)

  // ═══ 子标签 3：级别指定价设置 ═══
  console.log('\n[3] 级别指定价设置')
  const prod = page.data.records[0]
  const saveLp = await post('/erp/md/product-price/level-price/save', {
    gradeName: 'A餐饮客户', productId: prod.productId, productName: prod.productName,
    productCode: prod.productCode, unitName: prod.unitName, basePriceType: '零售价', calcOperator: '+', calcValue: 1.5,
  })
  check('新增级别指定价 200', saveLp.code === 200, saveLp.msg || saveLp.data)
  const lpPage = await get('/erp/md/product-price/level-price/page?pageNum=1&pageSize=50')
  const lpRow = (lpPage.data?.records || []).find(r => r.gradeName === 'A餐饮客户' && String(r.productId) === String(prod.productId))
  check('级别指定价落库', !!lpRow, lpPage.data?.total)
  check('价格规则文本=零售价+1.5', lpRow?.priceRule === '零售价+1.5', lpRow?.priceRule)
  check('带回商品档案条码/规格列', 'barcode' in (lpRow || {}), Object.keys(lpRow || {}).slice(0, 10))
  const lpFilter = await get('/erp/md/product-price/level-price/page?pageNum=1&pageSize=50&gradeName=' + encodeURIComponent('A餐饮客户'))
  check('按客户级别过滤生效', (lpFilter.data?.records || []).every(r => r.gradeName === 'A餐饮客户'), lpFilter.data?.total)
  const lpExport = await withRetry('GET', '/erp/md/product-price/level-price/export', null, {}, true)
  check('级别指定价导出 xlsx', lpExport.status === 200 && lpExport.buf.length > 1000, lpExport.status)
  const emptyRule = await post('/erp/md/product-price/level-price/save', { gradeName: 'A餐饮客户', calcValue: 1 })
  check('未选商品被拒绝', emptyRule.code !== 200, emptyRule.msg)

  // 导入（走 multipart 之外的 JSON rows 通道 + 真实 Excel 通道）
  const tpl = await withRetry('GET', '/erp/md/product-price/import-template?type=level', null, {}, true)
  check('级别指定价模板下载 xlsx', tpl.status === 200 && tpl.buf.length > 1000, tpl.status)

  // ═══ 子标签 4：客户指定价设置 ═══
  console.log('\n[4] 客户指定价设置')
  const custList = await get('/erp/md/customer/list?pageSize=50')
  const customers = custList?.data?.records || custList?.data || []
  check('客户下拉可用（往来单位）', Array.isArray(customers) && customers.length > 0, customers?.length)
  const customer = customers[0]
  const saveCp = await post('/erp/md/product-price/customer-price/save', {
    customerId: customer.id, productId: prod.productId, productName: prod.productName,
    productCode: prod.productCode, unitName: prod.unitName, calcOperator: '+', calcValue: 3.34,
  })
  check('新增客户指定价 200', saveCp.code === 200, saveCp.msg || saveCp.data)
  const cpPage = await get('/erp/md/product-price/customer-price/page?pageNum=1&pageSize=50')
  const cpRow = (cpPage.data?.records || []).find(r => String(r.productId) === String(prod.productId))
  check('客户指定价落库', !!cpRow, cpPage.data?.total)
  check('指定价规则文本=指定价 3.34', cpRow?.priceRule === '指定价 3.34', cpRow?.priceRule)
  check('带回客户名称/编号', !!cpRow?.customerName && !!cpRow?.customerCode, { n: cpRow?.customerName, c: cpRow?.customerCode })
  const cpFilter = await get('/erp/md/product-price/customer-price/page?pageNum=1&pageSize=50&productId=' + prod.productId)
  check('按商品过滤生效', (cpFilter.data?.records || []).every(r => String(r.productId) === String(prod.productId)), cpFilter.data?.total)
  const cpExport = await withRetry('GET', '/erp/md/product-price/customer-price/export', null, {}, true)
  check('客户指定价导出 xlsx', cpExport.status === 200 && cpExport.buf.length > 1000, cpExport.status)
  const cpTpl = await withRetry('GET', '/erp/md/product-price/import-template?type=customer', null, {}, true)
  check('客户指定价模板下载 xlsx', cpTpl.status === 200 && cpTpl.buf.length > 1000, cpTpl.status)
  const cpImport = await post('/erp/md/product-price/customer-price/import', {
    rows: [{ customerId: customer.id, productName: prod.productName, calcOperator: '*', calcValue: 0.9 }],
  })
  check('客户指定价导入（JSON 行）200 且成功 1 条', cpImport.code === 200 && cpImport.data?.success >= 1, cpImport.data)
  const badImport = await post('/erp/md/product-price/customer-price/import', {
    rows: [{ customerName: '不存在的客户XYZ', productName: prod.productName, calcValue: 1 }],
  })
  check('导入不存在的客户被计入失败', badImport.code === 200 && badImport.data?.failure >= 1 && (badImport.data?.errors || []).length > 0, badImport.data)

  // 批量删除
  const cpPage2 = await get('/erp/md/product-price/customer-price/page?pageNum=1&pageSize=100')
  const ids = (cpPage2.data?.records || []).filter(r => String(r.productId) === String(prod.productId)).map(r => String(r.id))
  const batchDel = await post('/erp/md/product-price/customer-price/batch-delete', { ids })
  check('客户指定价批量删除 200', batchDel.code === 200, batchDel.msg)
  const cpPage3 = await get('/erp/md/product-price/customer-price/page?pageNum=1&pageSize=100')
  check('批量删除后无残留', !(cpPage3.data?.records || []).some(r => String(r.productId) === String(prod.productId)), cpPage3.data?.total)

  const lpDel = await post('/erp/md/product-price/level-price/batch-delete', { ids: lpRow ? [String(lpRow.id)] : [] })
  check('级别指定价批量删除 200', lpDel.code === 200, lpDel.msg)

  // ═══ 统一取价 / 入参校验 ═══
  console.log('\n[5] 统一取价与入参校验')
  const gradesList = await get('/erp/md/product-price/grades')
  check('价格等级名称接口 200 且 8 个槽位', gradesList.code === 200 && (gradesList.data || []).length === 8, gradesList.data)

  // /erp/md/customer/list 返回全部往来单位，需按 partnerType=customer 过滤；级别字段为 gradeName
  const custs = await get('/erp/md/customer/list?pageSize=100')
  const custRows = (custs?.data?.records || custs?.data || []).filter((c) => c.partnerType === 'customer')
  const custWithLevel = custRows.find((c) => c.gradeName) || custRows[0]
  const level = custWithLevel?.gradeName
  const prodRow = page.data.records[0]
  check('存在带客户级别的客户（取价链路的级别命中依赖）', !!custWithLevel && !!level,
    { id: custWithLevel?.id, level, customerCount: custRows.length })

  const r0 = await get(`/erp/md/product-price/resolve?productId=${prodRow.productId}&customerId=${custWithLevel.id}`)
  check('取价兜底走商品单位价', r0.code === 200 && r0.data?.source === 'PRODUCT', r0.data)

  await post('/erp/md/product-price/grade-discount/save', {
    gradeName: level, basePriceType: '零售价', calcOperator: '*', calcValue: 1.5,
  })
  const r1 = await get(`/erp/md/product-price/resolve?productId=${prodRow.productId}&customerId=${custWithLevel.id}`)
  check('取价命中「客户级别折扣」', r1.data?.source === 'GRADE_DISCOUNT', r1.data)
  check('折扣价 = 商品零售价 × 1.5', Math.abs(Number(r1.data?.price) - Number(r0.data?.price) * 1.5) < 0.6,
    { base: r0.data?.price, rule: r1.data?.price })

  await post('/erp/md/product-price/level-price/save', {
    gradeName: level, productId: prodRow.productId, productName: prodRow.productName,
    basePriceType: '零售价', calcOperator: '+', calcValue: 2,
  })
  const r2 = await get(`/erp/md/product-price/resolve?productId=${prodRow.productId}&customerId=${custWithLevel.id}`)
  check('取价命中「级别指定价」（优先于级别折扣）', r2.data?.source === 'GRADE_PRICE', r2.data)

  await post('/erp/md/product-price/customer-price/save', {
    customerId: custWithLevel.id, productId: prodRow.productId, productName: prodRow.productName,
    calcOperator: '+', calcValue: 3.34,
  })
  const r3 = await get(`/erp/md/product-price/resolve?productId=${prodRow.productId}&customerId=${custWithLevel.id}`)
  check('取价命中「客户指定价」（最高优先级）', r3.data?.source === 'CUSTOMER', r3.data)
  check('客户指定价 = 指定价 3.34', Math.abs(Number(r3.data?.price) - 3.34) < 0.01, r3.data)

  const cpClean = await get(`/erp/md/product-price/customer-price/page?pageNum=1&pageSize=50&customerId=${custWithLevel.id}`)
  const cpIds = (cpClean.data?.records || []).map((r) => String(r.id))
  if (cpIds.length) await post('/erp/md/product-price/customer-price/batch-delete', { ids: cpIds })
  const lpClean = await get(`/erp/md/product-price/level-price/page?pageNum=1&pageSize=50&gradeName=${encodeURIComponent(level)}`)
  const lpIds = (lpClean.data?.records || []).map((r) => String(r.id))
  if (lpIds.length) await post('/erp/md/product-price/level-price/batch-delete', { ids: lpIds })
  const gdClean = await get('/erp/md/product-price/grade-discount/page?pageNum=1&pageSize=50')
  const gdRowClean = (gdClean.data?.records || []).find((r) => r.gradeName === level)
  if (gdRowClean) await del('/erp/md/product-price/grade-discount/' + gdRowClean.id)
  const r4 = await get(`/erp/md/product-price/resolve?productId=${prodRow.productId}&customerId=${custWithLevel.id}`)
  check('规则清理后取价回落商品单位价', r4.data?.source === 'PRODUCT', r4.data)

  const negFixed = await put('/erp/md/product-price/batch-modify', {
    unitIds: [String(prodRow.unitId)], items: [{ field: 'retailPrice', mode: 'FIXED', value: -1 }],
  })
  check('负数改价被拒绝', negFixed.code !== 200, negFixed.msg)
  const emptyFixed = await put('/erp/md/product-price/batch-modify', {
    unitIds: [String(prodRow.unitId)], items: [{ field: 'retailPrice', mode: 'FIXED' }],
  })
  check('直接改价缺值被拒绝（不再静默 0 条）', emptyFixed.code !== 200, emptyFixed.msg)
  const negRule = await put('/erp/md/product-price/batch-modify', {
    unitIds: [String(prodRow.unitId)],
    items: [{ field: 'retailPrice', mode: 'RULE', basePriceField: 'wholesalePrice', calcOperator: '-', calcValue: 999999 }],
  })
  check('规则计算结果为负被拒绝', negRule.code !== 200, negRule.msg)

  console.log(`\n结果：${PASS} 通过 / ${FAIL} 失败`)
  if (FAILURES.length) {
    console.log('失败项：')
    FAILURES.forEach(f => console.log(' -', f))
    process.exit(1)
  }
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
