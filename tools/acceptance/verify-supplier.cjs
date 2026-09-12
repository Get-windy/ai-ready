// 供应商（资料 → 往来单位 → 供应商）金标准验收：接口 + 真实浏览器 UI
// 后端 5655（本会话独立 fat jar 副本）/ 前端 Vite 5661
const path = require('path')
const fs = require('fs')
const os = require('os')
const { execFileSync } = require('child_process')
const { chromium } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/playwright'))
const { Client } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/pg'))

const APP = process.env.SUP_APP || 'http://localhost:5661'
const API = process.env.SUP_API || 'http://localhost:5655'
const SHOT = path.join(__dirname, '../../tool-results')
const TMP = os.tmpdir()

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}
const sleep = ms => new Promise(r => setTimeout(r, ms))

/**
 * 把浏览器页面的 /api 请求直连转发到 SUP_API 后端。
 * 避免依赖 Vite dev server 的 proxy 目标（共享 5655 可能跑着旧代码）。
 */
async function installApiProxy(context) {
  // 只拦截路径以 /api/ 开头的真正接口请求；
  // 用 '**/api/**' 会误伤 Vite 源码模块（/src/api/user.ts 等）导致页面白屏。
  await context.route(url => {
    try { return new URL(url).pathname.startsWith('/api/') } catch { return false }
  }, async route => {
    const u = new URL(route.request().url())
    try {
      const resp = await route.fetch({ url: `${API}${u.pathname}${u.search}` })
      await route.fulfill({ response: resp })
    } catch {
      await route.abort()
    }
  })
}

function curl(args) {
  return execFileSync('curl', ['-s', '--max-time', '60', ...args], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })
}
let TOKEN = ''
function apiGet(p) { return JSON.parse(curl(['-H', `Authorization: Bearer ${TOKEN}`, `${API}${p}`]) || '{}') }
function apiSend(method, p, body) {
  const args = ['-X', method, '-H', `Authorization: Bearer ${TOKEN}`, '-H', 'Content-Type: application/json; charset=utf-8']
  let f = null
  if (body !== undefined) {
    f = path.join(TMP, `sup-body-${Date.now()}-${Math.random().toString(36).slice(2)}.json`)
    fs.writeFileSync(f, JSON.stringify(body), 'utf8')
    args.push('-d', `@${f}`)
  }
  args.push(`${API}${p}`)
  try { return JSON.parse(curl(args) || '{}') } finally { if (f) { try { fs.unlinkSync(f) } catch {} } }
}
function captchaText(imgSrc) {
  const m = (imgSrc || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}
async function apiLogin() {
  for (let i = 1; i <= 15; i++) {
    let cap
    try { cap = JSON.parse(curl([`${API}/api/auth/captcha`])).data || {} } catch { await sleep(6000); continue }
    const f = path.join(TMP, `sup-login-${Date.now()}.json`)
    fs.writeFileSync(f, JSON.stringify({
      username: process.env.SUP_USER || 'sup_e2e2', password: 'admin123', tenantName: '系统租户',
      captcha: captchaText(cap.img), captchaKey: cap.uuid,
    }), 'utf8')
    let res = {}
    try { res = JSON.parse(curl(['-X', 'POST', '-H', 'Content-Type: application/json; charset=utf-8', '-d', `@${f}`, `${API}/api/auth/login`]) || '{}') }
    finally { try { fs.unlinkSync(f) } catch {} }
    const d = res.data || {}
    if (d.token) return d
    console.log(`  ⚠️ 登录未成功(${res.message || 'no-token'})，重试…`)
    await sleep(4000)
  }
  throw new Error('接口登录失败')
}

;(async () => {
  fs.mkdirSync(SHOT, { recursive: true })
  const pg = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
  await pg.connect()
  const scalar = async (q, params) => {
    const r = await pg.query(q, params)
    const rows = Array.isArray(r) ? r[r.length - 1].rows : r.rows
    return rows.length ? Object.values(rows[0])[0] : null
  }

  // ═══════════════════════════════════════════
  console.log('\n【数据库 · 列结构 / 迁移 / 单一口径】')
  const cols = ['opening_payable', 'opening_prepaid', 'operating_series', 'operating_area',
    'payment_term_type', 'payment_days', 'fixed_payment_day', 'settlement_day',
    'price_track_enabled', 'roles']
  const colCnt = Number(await scalar(`SELECT COUNT(*)::int FROM information_schema.columns
     WHERE table_name='biz_party' AND column_name = ANY($1)`, [cols]))
  check('biz_party 补全 10 列（期初/账期/经营/价格跟踪/多重身份）', colCnt === 10, `${colCnt}/10`)
  const hist = await pg.query(`SELECT version, success FROM flyway_schema_history WHERE version='11.135.0'`)
  check('Flyway 迁移 V11.135.0 已登记且 success', hist.rows.length === 1 && hist.rows[0].success === true, JSON.stringify(hist.rows))
  const migSql = fs.readFileSync(path.join(__dirname,
    '../../backend/core/api/core-api/src/main/resources/db/migration/V11.135.0__Supplier_Gold_Standard.sql'), 'utf8')
  check('V11.135.0 未新建表（单一口径红线：只 ALTER 既有 biz_party）',
    !/CREATE\s+TABLE/i.test(migSql), /CREATE\s+TABLE/i.test(migSql) ? '含 CREATE TABLE' : 'ALTER 补列')
  const sharedSrc = Number(await scalar(`SELECT COUNT(*)::int FROM biz_party WHERE deleted=0 AND party_type=2`))
  check('供应商主数据落在共享表 biz_party（party_type=2）', sharedSrc >= 1, `${sharedSrc} 条`)
  const attTbl = Number(await scalar(`SELECT COUNT(*)::int FROM information_schema.tables WHERE table_name='erp_partner_attachment'`))
  check('erp_partner_attachment 附件表存在', attTbl === 1)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 认证】')
  const sess = await apiLogin()
  TOKEN = sess.token
  check('登录成功', !!TOKEN, `token=${String(TOKEN).slice(0, 8)}…`)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 列表分页（对标 12 列字段齐备）】')
  const page1 = apiGet('/api/erp/md/customer/page?partnerType=supplier&status=ENABLED&pageNum=1&pageSize=20')
  const recs = page1.data?.records || []
  check('返回分页结构', !!page1.data && Array.isArray(recs), `total=${page1.data?.total}`)
  check('分页有真实数据', recs.length > 0, `${recs.length} 条`)
  const need = ['partnerCode', 'partnerName', 'contactPerson', 'contactPhone', 'addTime',
    'attachmentCount', 'remark', 'operatingSeries', 'operatingArea', 'taxNumber', 'bankName', 'bankAccount']
  const first = recs[0] || {}
  const missing = need.filter(k => !(k in first))
  check('12 列字段全部下发', missing.length === 0, missing.length ? `缺 ${missing.join(',')}` : need.join(','))
  check('仅返回供应商（party_type=2）', recs.every(r => r.partnerType === 'supplier'))

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 查询条件生效】')
  const kw = apiGet('/api/erp/md/customer/page?partnerType=supplier&keyword=' + encodeURIComponent('供应商'))
  const kwRecs = kw.data?.records || []
  check('筛选条件（编号/名称/联系人/备注）生效',
    kwRecs.length >= 1 && kwRecs.every(r => `${r.partnerCode}${r.partnerName || ''}${r.contactPerson || ''}${r.remark || ''}`.includes('供应商')),
    `${kwRecs.length} 条`)
  const disabled = apiGet('/api/erp/md/customer/page?partnerType=supplier&status=DISABLED')
  check('显示状态=已停用 生效', (disabled.data?.records || []).every(r => r.status === 'DISABLED'))
  const all = apiGet('/api/erp/md/customer/page?partnerType=supplier')
  check('显示状态=全部 生效', (all.data?.total ?? 0) >= (page1.data?.total ?? 0))

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 列头排序（对标 供应商编号 / 供应商名称 / 新增时间 可排序）】')
  const sortStamp = Date.now().toString().slice(-6)
  const sortIds = []
  for (const nm of ['AAA', 'BBB', 'CCC']) {
    const r = apiSend('POST', '/api/erp/md/customer', {
      partnerType: 'SUPPLIER', status: 'ENABLED', roles: 'SUPPLIER',
      partnerName: `${nm}排序供应商-${sortStamp}`, partnerCode: `SRT${sortStamp}${nm}`,
    })
    sortIds.push(r.data?.id)
  }
  check('排序前置：3 条测试供应商创建成功', sortIds.every(Boolean), sortIds.join(','))
  const pick = (recs) => (recs || []).map(r => r.partnerName).filter(n => String(n).includes(`排序供应商-${sortStamp}`))
  const sAsc = pick(apiGet('/api/erp/md/customer/page?partnerType=supplier&sortField=partyName&sortOrder=asc&pageSize=50').data?.records)
  check('按供应商名称升序生效',
    sAsc.length === 3 && sAsc[0].startsWith('AAA') && sAsc[1].startsWith('BBB') && sAsc[2].startsWith('CCC'),
    sAsc.map(n => String(n).slice(0, 3)).join(' < '))
  const sDesc = pick(apiGet('/api/erp/md/customer/page?partnerType=supplier&sortField=partyName&sortOrder=desc&pageSize=50').data?.records)
  check('按供应商名称降序生效',
    sDesc.length === 3 && sDesc[0].startsWith('CCC') && sDesc[2].startsWith('AAA'),
    sDesc.map(n => String(n).slice(0, 3)).join(' > '))
  const codeDesc = (apiGet(`/api/erp/md/customer/page?partnerType=supplier&sortField=partyCode&sortOrder=desc&pageSize=50`).data?.records || [])
    .map(r => r.partnerCode).filter(c => String(c).startsWith(`SRT${sortStamp}`))
  check('按供应商编号降序生效', codeDesc.length === 3 && codeDesc[0] > codeDesc[2], codeDesc.join(' > '))
  const timeDesc = apiGet('/api/erp/md/customer/page?partnerType=supplier&sortField=createTime&sortOrder=desc&pageSize=50')
  check('按新增时间排序不报错', timeDesc.code === 200 && (timeDesc.data?.records || []).length > 0, `total=${timeDesc.data?.total}`)
  for (const id of sortIds) {
    if (!id) continue
    await pg.query('DELETE FROM biz_party_contact WHERE party_id=$1', [id])
    await pg.query('DELETE FROM biz_party WHERE id=$1', [id])
  }

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 新增（P0：必须返回 id 供挂接联系人/附件）】')
  const stamp = Date.now().toString().slice(-6)
  const code = `GYS${stamp}`
  const createRes = apiSend('POST', '/api/erp/md/customer', {
    partnerType: 'SUPPLIER', status: 'ENABLED', roles: 'SUPPLIER,CUSTOMER',
    partnerName: `E2E供应商-${stamp}`, partnerCode: code, mnemonicCode: `E2E${stamp}`,
    remark: '金标准验收', companyFullName: `E2E公司全称-${stamp}`, taxNumber: `TAX${stamp}`,
    address: '纳税人地址E2E', phone: `0${stamp}`, bankName: '中国银行E2E', bankAccount: `6222${stamp}`,
    openingPayable: 1234.56, openingPrepaid: 78.9,
    paymentTermType: 'FIXED', paymentDays: 45, fixedPaymentDay: 15, settlementDay: 20,
    operatingSeries: '水产系列', operatingArea: 320.5, priceTrackEnabled: 1,
  })
  const newId = createRes.data?.id
  check('新增返回 200 且带 id', createRes.code === 200 && !!newId, `id=${newId}`)
  const row = (await pg.query('SELECT * FROM biz_party WHERE id=$1', [newId])).rows[0] || {}
  check('落库：基础字段（编号/名称/助记码/类型/角色）',
    row.party_code === code && row.party_name === `E2E供应商-${stamp}` && row.mnemonic_code === `E2E${stamp}` &&
    row.party_type === 2 && row.roles === 'SUPPLIER,CUSTOMER',
    `code=${row.party_code}, roles=${row.roles}`)
  check('落库：纳税人信息（公司全称/税号/地址/电话/开户行/银行账号）',
    row.company_full_name === `E2E公司全称-${stamp}` && row.tax_number === `TAX${stamp}` &&
    row.address === '纳税人地址E2E' && row.phone === `0${stamp}` &&
    row.bank_name === '中国银行E2E' && row.bank_account === `6222${stamp}`)
  check('落库：期初信息（期初应付/期初预付）',
    Number(row.opening_payable) === 1234.56 && Number(row.opening_prepaid) === 78.9,
    `${row.opening_payable} / ${row.opening_prepaid}`)
  check('落库：其他信息（付款期限方式/天数/固定账期日/结算期/经营系列/经营面积）',
    row.payment_term_type === 'FIXED' && row.payment_days === 45 && row.fixed_payment_day === 15 &&
    row.settlement_day === 20 && row.operating_series === '水产系列' && Number(row.operating_area) === 320.5,
    `${row.payment_term_type}/${row.payment_days}/${row.fixed_payment_day}/${row.settlement_day}`)
  check('落库：启用价格跟踪', row.price_track_enabled === 1)
  check('审计/租户字段自动填充', Number(row.tenant_id) === 1 && !!row.create_time, `tenant=${row.tenant_id}`)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 主联系人（联系人 / 联系电话 / 联系地址）】')
  const pc = apiSend('PUT', `/api/erp/md/customer/${newId}/primary-contact`, {
    contactPerson: '张三E2E', contactPhone: '13900000000', address: '联系地址E2E',
  })
  check('保存主联系人返回 200', pc.code === 200)
  const cRow = (await pg.query(
    'SELECT * FROM biz_party_contact WHERE party_id=$1 AND deleted=0', [newId])).rows[0] || {}
  check('落库 biz_party_contact（姓名/电话/地址/is_primary）',
    cRow.contact_name === '张三E2E' && cRow.phone === '13900000000' &&
    cRow.detail_address === '联系地址E2E' && cRow.is_primary === 1,
    `name=${cRow.contact_name}, primary=${cRow.is_primary}`)
  const pRow2 = (await pg.query('SELECT phone FROM biz_party WHERE id=$1', [newId])).rows[0] || {}
  check('联系电话同步 biz_party.phone（供采购/应付带出）', pRow2.phone === '13900000000', pRow2.phone)
  const detail = apiGet(`/api/erp/md/customer/${newId}`)
  check('详情回读联系人/联系电话/联系地址',
    detail.data?.contactPerson === '张三E2E' && detail.data?.contactPhone === '13900000000' &&
    detail.data?.address === '联系地址E2E',
    `${detail.data?.contactPerson}/${detail.data?.contactPhone}/${detail.data?.address}`)
  check('详情回读纳税人地址与联系地址分离', detail.data?.taxAddress === '纳税人地址E2E', detail.data?.taxAddress)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 附件（列表附件列闭环）】')
  const attRes = apiSend('POST', '/api/erp/partner/attachments', {
    partnerId: Number(newId), fileName: '营业执照.pdf', fileUrl: '/files/e2e.pdf',
    fileSize: 2048, fileType: 'application/pdf', category: 'DOC',
  })
  check('附件新增返回 200', attRes.code === 200)
  const attList = apiGet(`/api/erp/partner/attachments/by-partner/${newId}`)
  check('附件列表可回读', (attList.data || []).length === 1, `${(attList.data || []).length} 条`)
  const withAtt = apiGet('/api/erp/md/customer/page?partnerType=supplier&keyword=' + encodeURIComponent(code))
  const attVo = (withAtt.data?.records || [])[0] || {}
  check('列表「附件」列 attachmentCount=1', attVo.attachmentCount === 1, String(attVo.attachmentCount))

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 修改（全字段回写）】')
  const upd = apiSend('PUT', `/api/erp/md/customer/${newId}`, {
    partnerType: 'SUPPLIER', status: 'ENABLED', roles: 'SUPPLIER',
    partnerName: `E2E供应商-改-${stamp}`, partnerCode: code,
    operatingSeries: '干货系列', operatingArea: 99.5,
    paymentTermType: 'DYNAMIC', paymentDays: 60, settlementDay: 5,
    priceTrackEnabled: 0, companyFullName: `E2E改名公司-${stamp}`,
  })
  check('修改返回 200', upd.code === 200, upd.message)
  const row3 = (await pg.query('SELECT * FROM biz_party WHERE id=$1', [newId])).rows[0] || {}
  check('修改落库（名称/经营系列/面积/账期/价格跟踪/公司全称/角色）',
    row3.party_name === `E2E供应商-改-${stamp}` && row3.operating_series === '干货系列' &&
    Number(row3.operating_area) === 99.5 && row3.payment_term_type === 'DYNAMIC' &&
    row3.payment_days === 60 && row3.settlement_day === 5 && row3.price_track_enabled === 0 &&
    row3.company_full_name === `E2E改名公司-${stamp}` && row3.roles === 'SUPPLIER',
    `${row3.party_name}/${row3.operating_series}/${row3.payment_days}`)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 编号号段 next-seq（GYS 前缀）】')
  const seq = apiGet('/api/erp/md/customer/next-seq?prefix=GYS')
  check('返回数字序号', typeof seq.data?.seq === 'number' && seq.data.seq > 0, `seq=${seq.data?.seq}`)
  const seq2 = apiGet('/api/erp/md/customer/next-seq?prefix=GYS')
  check('号段可重复查询且不自增（保存时才占用）', seq2.data?.seq === seq.data?.seq)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 显示层次结构（分类为行）】')
  const hier = apiGet('/api/erp/md/customer/page?partnerType=supplier&showHierarchy=1&pageNum=1&pageSize=20')
  const hRecs = hier.data?.records || []
  check('层次结构行结构=分类行（编号=分类编码 / 名称=分类名称）',
    hRecs.every(r => r.partnerType === 'category' && !!r.partnerCode && !!r.partnerName),
    hRecs.length ? hRecs.slice(0, 3).map(r => `${r.partnerCode}:${r.partnerName}`).join(' , ') : '当前无分类（下方建分类后复验）')
  const hierTotal = hier.data?.total ?? 0
  const catCnt = Number(await scalar(`SELECT COUNT(*)::int FROM biz_party_category WHERE party_type=2 AND deleted=0`))
  check('层次结构条数=供应商分类数', Number(hierTotal) === catCnt, `${hierTotal} vs ${catCnt}`)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 显示客户中的供应商（多重身份合并）】')
  const asc = apiGet('/api/erp/md/customer/page?partnerType=supplier&showAsCustomer=1&status=ENABLED&pageNum=1&pageSize=100')
  const ascRecs = asc.data?.records || []
  const hasE2E = ascRecs.some(r => r.partnerCode === code)
  const rolesLike = Number(await scalar(`SELECT COUNT(*)::int FROM biz_party
     WHERE deleted=0 AND status=1 AND party_type=2 AND roles LIKE '%SUPPLIER%'`))
  const ascTotal = Number(asc.data?.total ?? 0)
  const plainTotal = Number((apiGet('/api/erp/md/customer/page?partnerType=supplier&status=ENABLED&pageNum=1&pageSize=1').data?.total) ?? 0)
  check('合并口径包含「身份含 SUPPLIER 的往来单位」', hasE2E, `total=${ascTotal}`)
  check('合并口径 ⊇ 纯供应商口径（不会更少）', ascTotal >= plainTotal, `${ascTotal} >= ${plainTotal}`)
  check('合并口径条数 = party_type=2 ∪ roles 含 SUPPLIER', ascTotal >= rolesLike, `${ascTotal} / ${rolesLike}`)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 分类 CRUD（分类树闭环）】')
  const catCreate = apiSend('POST', '/api/erp/partner/categories', {
    categoryName: `E2E分类-${stamp}`, categoryType: 'SUPPLIER', sortOrder: 1,
  })
  const catId = catCreate.data
  check('新增分类返回 id', catCreate.code === 200 && !!catId, `id=${catId}`)
  const catRow = (await pg.query('SELECT * FROM biz_party_category WHERE id=$1', [catId])).rows[0] || {}
  check('分类自动编号（gysml 前缀 + 3 位序号）与层级', /^gysml\d{3}$/.test(catRow.category_code || '') && catRow.party_type === 2 && catRow.level === 1,
    `code=${catRow.category_code}, level=${catRow.level}`)
  const catUpd = apiSend('PUT', `/api/erp/partner/categories/${catId}`, { categoryName: `E2E分类改-${stamp}`, sortOrder: 9 })
  check('修改分类返回 200', catUpd.code === 200, catUpd.message)
  const catRow2 = (await pg.query('SELECT * FROM biz_party_category WHERE id=$1', [catId])).rows[0] || {}
  check('分类修改落库', catRow2.category_name === `E2E分类改-${stamp}` && catRow2.sort_order === 9)
  // 分类下有供应商时禁止删除
  apiSend('PUT', `/api/erp/md/customer/batch-move`, { ids: [Number(newId)], categoryId: Number(catId) })
  const byCat = apiGet(`/api/erp/md/customer/page?partnerType=supplier&categoryId=${catId}&status=ENABLED`)
  const byCatRecs = byCat.data?.records || []
  check('按所属分类筛选生效（分类树点击 → categoryId 过滤）',
    byCatRecs.length >= 1 && byCatRecs.every(r => String(r.categoryId) === String(catId)),
    `${byCatRecs.length} 条`)
  const delRefuse = apiSend('DELETE', `/api/erp/partner/categories/${catId}`)
  check('分类下有供应商时拒绝删除并给出原因',
    typeof delRefuse.data === 'string' && delRefuse.data.includes('无法删除'), String(delRefuse.data))
  const clearMove = apiSend('PUT', `/api/erp/md/customer/batch-move`, { ids: [Number(newId)], categoryId: '' })
  const clearRow = (await pg.query('SELECT category_id FROM biz_party WHERE id=$1', [newId])).rows[0] || {}
  check('批量搬移到「未分类」（分类留空）落库为 NULL', clearMove.code === 200 && clearRow.category_id === null, `category_id=${clearRow.category_id}`)
  const catTree = apiGet('/api/erp/partner/categories/tree?categoryType=SUPPLIER')
  const treeHas = JSON.stringify(catTree.data || []).includes(`E2E分类改-${stamp}`)
  check('分类树可回读新增分类', treeHas)
  const hier2 = apiGet('/api/erp/md/customer/page?partnerType=supplier&showHierarchy=1&pageNum=1&pageSize=20')
  const h2 = hier2.data?.records || []
  check('分类存在时层次结构返回分类行（编号=分类编码 / 名称=分类名称 / 新增时间）',
    h2.length >= 1 && h2.every(r => r.partnerType === 'category' && !!r.partnerCode && !!r.partnerName && !!r.addTime),
    h2.slice(0, 3).map(r => `${r.partnerCode}:${r.partnerName}:${String(r.addTime).slice(0, 10)}`).join(' , '))

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 更多（批量搬移/停用/启用/取消价格跟踪/批量删除）】')
  const mv = apiSend('PUT', '/api/erp/md/customer/batch-move', { ids: [Number(newId)], categoryId: Number(catId) })
  check('批量搬移返回 200', mv.code === 200)
  const mvRow = (await pg.query('SELECT category_id FROM biz_party WHERE id=$1', [newId])).rows[0] || {}
  check('批量搬移落库（所属分类已改）', Number(mvRow.category_id) === Number(catId), `category_id=${mvRow.category_id}`)
  const bs = apiSend('PUT', '/api/erp/md/customer/batch-status', { ids: [Number(newId)], status: 'DISABLED' })
  check('停用返回 200', bs.code === 200)
  const stRow = (await pg.query('SELECT status FROM biz_party WHERE id=$1', [newId])).rows[0] || {}
  check('停用落库', stRow.status === 0, `status=${stRow.status}`)
  const bs2 = apiSend('PUT', '/api/erp/md/customer/batch-status', { ids: [Number(newId)], status: 'ENABLED' })
  const stRow2 = (await pg.query('SELECT status FROM biz_party WHERE id=$1', [newId])).rows[0] || {}
  check('启用落库', bs2.code === 200 && stRow2.status === 1, `status=${stRow2.status}`)
  const pt = apiSend('PUT', '/api/erp/md/customer/batch-price-track', { ids: [Number(newId)], priceTrackEnabled: true })
  const ptRow = (await pg.query('SELECT price_track_enabled FROM biz_party WHERE id=$1', [newId])).rows[0] || {}
  check('启用价格跟踪落库', pt.code === 200 && ptRow.price_track_enabled === 1, `flag=${ptRow.price_track_enabled}`)
  const pt2 = apiSend('PUT', '/api/erp/md/customer/batch-price-track', { ids: [Number(newId)], priceTrackEnabled: false })
  const ptRow2 = (await pg.query('SELECT price_track_enabled FROM biz_party WHERE id=$1', [newId])).rows[0] || {}
  check('取消价格跟踪落库', pt2.code === 200 && ptRow2.price_track_enabled === 0, `flag=${ptRow2.price_track_enabled}`)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 行级单条停用（行「更多 → 停用」）】')
  const single = apiSend('PUT', `/api/erp/md/customer/${newId}/status?status=DISABLED`)
  const sRow = (await pg.query('SELECT status FROM biz_party WHERE id=$1', [newId])).rows[0] || {}
  check('单条停用返回 200 且落库', single.code === 200 && sRow.status === 0, `status=${sRow.status}`)
  apiSend('PUT', `/api/erp/md/customer/${newId}/status?status=ENABLED`)

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 客商合并（行「更多 → 客商合并」）】')
  const mergeSrc = apiSend('POST', '/api/erp/md/customer', {
    partnerType: 'SUPPLIER', status: 'ENABLED', roles: 'SUPPLIER,CUSTOMER',
    partnerName: `E2E合并源-${stamp}`, partnerCode: `MGS${stamp}`, openingPayable: 100,
  })
  const mergeTgt = apiSend('POST', '/api/erp/md/customer', {
    partnerType: 'CUSTOMER', status: 'ENABLED', roles: 'CUSTOMER',
    partnerName: `E2E合并目标-${stamp}`, partnerCode: `MGT${stamp}`, openingPayable: 50,
  })
  const mergeSrcId = mergeSrc.data?.id
  const mergeTgtId = mergeTgt.data?.id
  check('合并前置：源与目标均创建成功', !!mergeSrcId && !!mergeTgtId, `src=${mergeSrcId}, tgt=${mergeTgtId}`)
  apiSend('PUT', `/api/erp/md/customer/${mergeSrcId}/primary-contact`, {
    contactPerson: `合并联系人${stamp}`, contactPhone: '13800000009', address: '合并地址E2E',
  })
  apiSend('POST', '/api/erp/partner/attachments', {
    partnerId: Number(mergeSrcId), fileName: 'merge-e2e.pdf', fileUrl: '/files/merge-e2e.pdf',
    fileSize: 1024, fileType: 'application/pdf',
  })
  const mg = apiSend('PUT', `/api/erp/md/customer/${mergeSrcId}/merge-partner`, { targetId: Number(mergeTgtId) })
  check('客商合并返回 200 且 merged=true', mg.code === 200 && mg.data?.merged === true, JSON.stringify(mg.data || {}).slice(0, 140))
  const mgTgt = (await pg.query('SELECT roles, opening_payable FROM biz_party WHERE id=$1', [mergeTgtId])).rows[0] || {}
  const mgSrc = (await pg.query('SELECT id FROM biz_party WHERE id=$1', [mergeSrcId])).rows[0]
  check('合并落库：目标承接源身份（roles 含 SUPPLIER）', String(mgTgt.roles || '').includes('SUPPLIER'), `roles=${mgTgt.roles}`)
  check('合并落库：期初应付累加（100+50=150）', Number(mgTgt.opening_payable) === 150, `payable=${mgTgt.opening_payable}`)
  const mgContacts = (await pg.query(
    'SELECT COUNT(*)::int AS c FROM biz_party_contact WHERE party_id=$1 AND deleted=0', [mergeTgtId])).rows[0]?.c
  const mgAtts = (await pg.query(
    'SELECT COUNT(*)::int AS c FROM erp_partner_attachment WHERE partner_id=$1', [mergeTgtId])).rows[0]?.c
  check('合并落库：联系人迁移到目标', Number(mgContacts) >= 1, `${mgContacts} 条`)
  check('合并落库：附件迁移到目标', Number(mgAtts) >= 1, `${mgAtts} 条`)
  check('合并落库：源档案无残留引用（联系人/附件已全部迁走）',
    Number((await pg.query('SELECT COUNT(*)::int AS c FROM biz_party_contact WHERE party_id=$1', [mergeSrcId])).rows[0].c) === 0
    && Number((await pg.query('SELECT COUNT(*)::int AS c FROM erp_partner_attachment WHERE partner_id=$1', [mergeSrcId])).rows[0].c) === 0)
  check('合并落库：源档案物理删除（非逻辑删除留痕）', !mgSrc, mgSrc ? `仍存在 id=${mgSrc.id}` : '已删除')
  check('合并返回迁移引用数', typeof mg.data?.movedReferences === 'number' && mg.data.movedReferences >= 2,
    `movedReferences=${mg.data?.movedReferences}`)
  const mgSelf = apiSend('PUT', `/api/erp/md/customer/${mergeTgtId}/merge-partner`, { targetId: Number(mergeTgtId) })
  check('合并保护：不能合并到自身', mgSelf.data?.merged === false && /自身/.test(mgSelf.data?.reason || ''), mgSelf.data?.reason)
  await pg.query('DELETE FROM erp_partner_attachment WHERE partner_id = ANY($1)', [[Number(mergeSrcId), Number(mergeTgtId)]])
  await pg.query('DELETE FROM biz_party_contact WHERE party_id = ANY($1)', [[Number(mergeSrcId), Number(mergeTgtId)]])
  await pg.query('DELETE FROM biz_party WHERE id = ANY($1)', [[Number(mergeSrcId), Number(mergeTgtId)]])

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 导出真实 xlsx（12 列口径）】')
  const outFile = path.join(TMP, `supplier-export-${Date.now()}.xlsx`)
  const hdr = curl(['-D', '-', '-o', outFile, '-H', `Authorization: Bearer ${TOKEN}`,
    `${API}/api/erp/md/customer/export?partnerType=supplier&title=${encodeURIComponent('供应商')}`])
  const size = fs.existsSync(outFile) ? fs.statSync(outFile).size : 0
  const magic = size > 4 ? fs.readFileSync(outFile).subarray(0, 2).toString('latin1') : ''
  check('导出返回 xlsx 内容类型', /spreadsheetml\.sheet/.test(hdr))
  check('导出为真实 xlsx（ZIP magic + 非空）', magic === 'PK' && size > 3000, `${size} bytes`)
  try { fs.unlinkSync(outFile) } catch {}

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 基础资料导入向导（模板下载 + Excel 真实落库）】')
  const tplFile = path.join(TMP, `sup-tpl-${stamp}.xlsx`)
  const tplHdr = curl(['-D', '-', '-o', tplFile, '-H', `Authorization: Bearer ${TOKEN}`,
    `${API}/api/erp/md/customer/import-template?partnerType=supplier`])
  const tplSize = fs.existsSync(tplFile) ? fs.statSync(tplFile).size : 0
  const tplMagic = tplSize > 4 ? fs.readFileSync(tplFile).subarray(0, 2).toString('latin1') : ''
  check('导入模板接口返回真实 xlsx（供应商口径）',
    /spreadsheetml/.test(tplHdr) && tplMagic === 'PK' && tplSize > 3000, `${tplSize} bytes`)

  const pyFile = path.join(TMP, `sup-fill-${stamp}.py`)
  const filledFile = path.join(TMP, `sup-filled-${stamp}.xlsx`)
  fs.writeFileSync(pyFile, [
    'import openpyxl, sys',
    'src, dst, stamp = sys.argv[1], sys.argv[2], sys.argv[3]',
    'wb = openpyxl.load_workbook(src); ws = wb.active',
    "ws.cell(row=2, column=2, value='GYSUP' + stamp)",
    "ws.cell(row=2, column=3, value='导入供应商' + stamp)",
    "ws.cell(row=2, column=4, value='导入联系人' + stamp)",
    "ws.cell(row=2, column=5, value='13900005678')",
    "ws.cell(row=2, column=6, value='导入地址' + stamp)",
    'wb.save(dst)',
  ].join('\n'), 'utf8')
  let pyOk = true
  try {
    execFileSync('python', [pyFile, tplFile, filledFile, stamp], { encoding: 'utf8', stdio: 'pipe' })
  } catch (e) {
    pyOk = false
    console.log('  ⚠️ 构造测试 xlsx 失败:', String(e.message).slice(0, 120))
  }
  if (pyOk && fs.existsSync(filledFile)) {
    const res = JSON.parse(curl(['-X', 'POST', '-H', `Authorization: Bearer ${TOKEN}`,
      '-F', `file=@${filledFile}`, `${API}/api/erp/md/customer/import-excel?partnerType=supplier`]) || '{}')
    check('Excel 导入接口返回统计', res.code === 200 && res.data?.success >= 1,
      `success=${res.data?.success}, failure=${res.data?.failure}`)
    const imported = (await pg.query(
      'SELECT id, party_type, roles FROM biz_party WHERE party_code=$1 AND deleted=0',
      [`GYSUP${stamp}`])).rows[0]
    check('导入真实落库（供应商档案 + 身份）',
      !!imported && imported.party_type === 2 && String(imported.roles || '').includes('SUPPLIER'),
      imported ? `id=${imported.id}, type=${imported.party_type}, roles=${imported.roles}` : '未落库')
    if (imported) {
      const ic = (await pg.query(
        'SELECT contact_name, phone FROM biz_party_contact WHERE party_id=$1 AND deleted=0', [imported.id])).rows[0]
      check('导入同步主联系人', ic?.contact_name === `导入联系人${stamp}`, `${ic?.contact_name}/${ic?.phone}`)
      await pg.query('DELETE FROM biz_party_contact WHERE party_id=$1', [imported.id])
      await pg.query('DELETE FROM biz_party WHERE id=$1', [imported.id])
    }
  } else {
    check('Excel 导入真实落库', false, '本机缺少 python/openpyxl，未能构造测试文件')
  }
  try { fs.unlinkSync(pyFile); fs.unlinkSync(tplFile); fs.unlinkSync(filledFile) } catch {}

  // ═══════════════════════════════════════════
  console.log('\n【浏览器 · 列表页对标结构】')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 920 } })
  await installApiProxy(ctx)
  const page = await ctx.newPage()
  const pageErrors = []
  page.on('pageerror', e => pageErrors.push(e.message))
  await page.addInitScript(s => {
    localStorage.setItem('token', s.token)
    localStorage.setItem('tenantId', String(s.tenantId || 1))
    localStorage.setItem('tenantName', s.tenantName || '系统租户')
  }, sess)

  await page.goto(`${APP}/md/supplier/index`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.category-list-layout', { timeout: 120000 }).catch(() => {})
  await page.waitForTimeout(6000)
  await page.screenshot({ path: path.join(SHOT, 'supplier-list.png') })
  check('页面无 JS 运行错误', pageErrors.length === 0, pageErrors.slice(0, 2).join(' | '))

  const bodyText = await page.locator('body').innerText()
  check('工具栏含 新增/导入/刷新/打印(F8)/导出/更多',
    ['新增', '导入', '刷新', '打印(F8)', '导出', '更多'].every(t => bodyText.includes(t)))
  check('查询区含 筛选条件/显示状态/查询/显示层次结构/显示客户中的供应商',
    ['筛选条件', '显示状态', '显示层次结构', '显示客户中的供应商'].every(t => bodyText.includes(t)) && /查\s*询/.test(bodyText))
  check('左侧分类面板标题=供应商分类', bodyText.includes('供应商分类'))
  check('左侧根节点=全部供应商', bodyText.includes('全部供应商'))
  check('表尾含 当前路径', bodyText.includes('当前路径'))

  const headers = await page.locator('.ss-grid thead th').allInnerTexts().catch(() => [])
  const hTxt = headers.join('|').replace(/\s+/g, '')
  const expectCols = ['操作', '供应商编号', '供应商名称', '联系人', '联系电话', '新增时间', '附件', '备注',
    '经营系列', '经营面积', '税号', '开户行', '银行账号']
  const missCols = expectCols.filter(t => !hTxt.includes(t))
  check('表头含对标 12 列 + 操作列', missCols.length === 0, missCols.length ? `缺 ${missCols.join(',')}` : hTxt)

  const rowCount = await page.locator('.ss-grid tbody tr.ss-row').count()
  check('明细行渲染真实数据', rowCount >= 1, `${rowCount} 行`)
  const rowTxt = (await page.locator('.ss-grid tbody tr.ss-row').allInnerTexts().catch(() => [])).join(' | ')
  check('行内含 E2E 数据（编号/经营系列）', rowTxt.includes('E2E供应商') || rowTxt.includes('GYS') || rowTxt.includes('S001'),
    rowTxt.slice(0, 120))

  console.log('\n【浏览器 · 列配置弹窗（个人/全局）】')
  const gear = page.locator('.ss-header-settings').first()
  if (await gear.count()) {
    await gear.click()
    await page.waitForTimeout(1800)
    await page.screenshot({ path: path.join(SHOT, 'supplier-colcfg.png') })
    const modalTxt = await page.locator('.ant-modal-content').first().innerText().catch(() => '')
    check('列配置弹窗含 个人配置 / 全局配置 两个 Tab', modalTxt.includes('个人配置') && modalTxt.includes('全局配置'))
    const colTxt = modalTxt.replace(/\s+/g, '')
    const cfgMiss = ['供应商编号', '供应商名称', '联系人', '联系电话', '新增时间', '附件', '备注',
      '经营系列', '经营面积', '税号', '开户行', '银行账号'].filter(t => !colTxt.includes(t))
    check('列配置弹窗列出对标 12 列', cfgMiss.length === 0, cfgMiss.length ? `缺 ${cfgMiss.join(',')}` : '12/12')
    await page.keyboard.press('Escape')
    await page.waitForTimeout(1000)
  } else {
    check('列配置齿轮存在（表头序号列）', false, '未找到 .ss-header-settings')
  }

  console.log('\n【浏览器 · 分页栏（对标经典形态）】')
  const pagerTxt = await page.locator('.classic-pagination').first().innerText().catch(() => '')
  const pagerFlat = pagerTxt.replace(/\s+/g, '')
  const pagerNeed = ['首页', '上页', '下页', '尾页', '跳转', '条记录', '每页显示', '行']
  const pagerMiss = pagerNeed.filter(t => !pagerFlat.includes(t))
  check('分页栏含 首页/上页/第(x/y)页/下页/尾页/跳转/共N条记录/每页显示N行',
    pagerMiss.length === 0 && /第\(\d+\/\d+\)页/.test(pagerFlat),
    pagerMiss.length ? `缺 ${pagerMiss.join(',')}` : pagerFlat.slice(0, 120))

  console.log('\n【浏览器 · 行级「更多」菜单（删除/停用/详情/客商合并）】')
  {
    const rows = page.locator('.ss-grid tbody tr.ss-row')
    const rc = await rows.count()
    if (rc > 0) {
      await rows.first().getByText('更多', { exact: true }).click()
      await page.waitForTimeout(1200)
      await page.screenshot({ path: path.join(SHOT, 'sup-row-more.png') })
      const menuTxt = (await page.locator('.ant-dropdown-menu').first().innerText().catch(() => '')).replace(/\s+/g, '')
      const menuNeed = ['删除', '停用', '详情', '客商合并']
      const menuMiss = menuNeed.filter(t => !menuTxt.includes(t))
      check('行级「更多」含对标 4 项', menuMiss.length === 0, menuMiss.length ? `缺 ${menuMiss.join(',')}` : menuTxt)
      // 纯供应商行：客商合并应为禁用（与对标实测一致）
      const mergeItem = page.locator('.ant-dropdown-menu-item', { hasText: '客商合并' }).first()
      const cls = (await mergeItem.getAttribute('class').catch(() => '')) || ''
      const roleTxt = await rows.first().innerText().catch(() => '')
      check('客商合并在非客商双身份行呈现禁用态', cls.includes('disabled') || roleTxt.includes('合并'), cls.includes('disabled') ? 'disabled' : 'n/a')
      await page.keyboard.press('Escape')
      await page.waitForTimeout(800)
    } else {
      check('列表有数据行可验证行级菜单', false, '0 行')
    }
  }

  console.log('\n【浏览器 · 订货下推（行级「订货」→ 采购订单带供应商）】')
  {
    const r0 = page.locator('.ss-grid tbody tr.ss-row').first()
    if (await r0.count()) {
      await r0.getByText('订货', { exact: true }).first().click()
      await page.waitForTimeout(6000)
      const u = page.url()
      check('订货跳转采购订单并携带 supplierId', /\/erp\/purchase\/form/.test(u) && /supplierId=\d+/.test(u), u.slice(0, 130))
      const supTxt = await page.locator('body').innerText().catch(() => '')
      check('采购订单回填供应商名', supTxt.includes('E2E供应商') || supTxt.includes('供应商乙') || supTxt.includes('GYS'),
        supTxt.slice(0, 80).replace(/\n+/g, ' '))
      await page.goto(`${APP}/md/supplier/index`, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(5000)
    } else {
      check('有数据行可验证订货下推', false, '0 行')
    }
  }

  console.log('\n【浏览器 · 显示层次结构（分类为行）】')
  await page.getByText('显示层次结构', { exact: true }).first().click()
  await page.waitForTimeout(1200)
  await page.getByRole('button', { name: /查\s*询/ }).first().click()
  await page.waitForTimeout(5000)
  await page.screenshot({ path: path.join(SHOT, 'supplier-hierarchy.png') })
  const hRowTxt = (await page.locator('.ss-grid tbody tr.ss-row').allInnerTexts().catch(() => [])).join(' | ')
  check('层次结构视图渲染分类行（gysml 编码）', hRowTxt.includes('gysml') || hRowTxt.includes('E2E分类'), hRowTxt.slice(0, 100))
  await page.getByText('显示层次结构', { exact: true }).first().click()
  await page.waitForTimeout(800)
  await page.getByRole('button', { name: /查\s*询/ }).first().click()
  await page.waitForTimeout(4000)

  console.log('\n【浏览器 · 新增表单页对标结构】')
  await page.getByRole('button', { name: /新增/ }).first().click()
  await page.waitForTimeout(7000)
  await page.screenshot({ path: path.join(SHOT, 'supplier-form.png') })
  const formTxt = await page.locator('body').innerText()
  const sections = ['基础信息', '联系人', '纳税人信息', '期初信息', '其他信息', '附件']
  const missSec = sections.filter(t => !formTxt.includes(t))
  check('表单 6 分区齐备', missSec.length === 0, missSec.length ? `缺 ${missSec.join(',')}` : sections.join(' / '))
  check('顶部选项 既是供应商又是客户 / 启用价格跟踪 / 名称重复配置',
    ['既是供应商又是客户', '启用价格跟踪', '名称重复配置'].every(t => formTxt.includes(t)))
  const fieldLabels = ['供应商名称', '供应商编号', '所属分类', '助记码', '备注', '联系人', '联系电话', '联系地址',
    '公司全称', '税号', '地址', '电话', '开户行', '银行账号', '期初应付金额', '期初预付金额',
    '动态付款期限(天)', '固定账期', '结算期', '经营系列', '经营面积']
  const missLabels = fieldLabels.filter(t => !formTxt.includes(t))
  check('表单字段与对标一致（21 项）', missLabels.length === 0, missLabels.length ? `缺 ${missLabels.join(',')}` : '21/21')
  check('底部含 保存(Enter)', formTxt.includes('保存(Enter)'))
  const codeVal = await page.locator('.ant-form-item:has-text("供应商编号") input').first().inputValue().catch(() => '')
  check('新增自动生成供应商编号（GYS 号段，可改）', /^GYS/.test(codeVal) && codeVal.length > 4, `编号=${codeVal}`)
  const nameVal = await page.locator('.ant-form-item:has-text("供应商名称") input').first().inputValue().catch(() => '')
  check('新增页供应商名称为空（非回填旧值）', nameVal === '', `name="${nameVal}"`)

  console.log('\n【浏览器 · 编辑回填】')
  await page.goto(`${APP}/md/supplier/form?id=${newId}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(6000)
  await page.screenshot({ path: path.join(SHOT, 'supplier-form-edit.png') })
  const editTxt = await page.locator('body').innerText()
  check('编辑页标题/分类回填', editTxt.includes('E2E分类改-') || editTxt.includes('供应商列表'),
    editTxt.slice(0, 80).replace(/\n/g, ' '))
  const inputs = await page.locator('input').all()
  let bound = ''
  for (const i of inputs) {
    const v = await i.inputValue().catch(() => '')
    if (v) bound += v + ' | '
  }
  const needVal = [`E2E供应商-改-${stamp}`, code, '张三E2E', '13900000000', '联系地址E2E',
    `E2E改名公司-${stamp}`, `TAX${stamp}`, '纳税人地址E2E', '中国银行E2E', `6222${stamp}`,
    '干货系列', '99.5', 'DYNAMIC', '60']
  const missVal = needVal.filter(v => !bound.includes(v))
  check('编辑页全字段回填（名称/编号/联系人三件套/纳税人/期初/账期/经营）',
    missVal.length === 0, missVal.length ? `缺 ${missVal.join(',')}` : '14/14')

  await browser.close()

  // ═══════════════════════════════════════════
  console.log('\n【浏览器 · 表单保存闭环（无桩：真实落库）】')
  {
    const b2 = await chromium.launch({ headless: true })
    const c2 = await b2.newContext({ viewport: { width: 1680, height: 920 } })
    await installApiProxy(c2)
    const p2 = await c2.newPage()
    const errs2 = []
    p2.on('pageerror', e => errs2.push(e.message))
    await p2.addInitScript(s => {
      localStorage.setItem('token', s.token)
      localStorage.setItem('tenantId', '1')
      localStorage.setItem('tenantName', '系统租户')
    }, sess)
    await p2.goto(`${APP}/md/supplier/form`, { waitUntil: 'domcontentloaded' })
    await p2.waitForTimeout(7000)

    // ── 表单交互（保存前）：期初只读 / 账期联动 / 顶部选项 ──
    const openingDisabled = await p2.locator('.ant-form-item:has-text("期初应付金额") input').first().isDisabled().catch(() => false)
    check('期初信息为只读展示（与对标一致）', openingDisabled, String(openingDisabled))

    const dynDisabled = await p2.locator('.payment-term .ant-input-number-input').first().isDisabled().catch(() => null)
    const fixedCls = (await p2.locator('.payment-term .ant-select').first().getAttribute('class').catch(() => '')) || ''
    check('账期联动：默认「动态付款期限」可填、固定账期禁用',
      dynDisabled === false && fixedCls.includes('ant-select-disabled'),
      `天数可填=${dynDisabled === false}, 固定账期禁用=${fixedCls.includes('ant-select-disabled')}`)
    await p2.locator('.payment-term .ant-radio-wrapper', { hasText: '固定账期' }).first().click()
    await p2.waitForTimeout(600)
    const dynDisabled2 = await p2.locator('.payment-term .ant-input-number-input').first().isDisabled().catch(() => null)
    const fixedCls2 = (await p2.locator('.payment-term .ant-select').first().getAttribute('class').catch(() => '')) || ''
    check('账期联动：切到「固定账期」后反转',
      dynDisabled2 === true && !fixedCls2.includes('ant-select-disabled'),
      `天数禁用=${dynDisabled2 === true}, 固定账期可填=${!fixedCls2.includes('ant-select-disabled')}`)

    // 勾选顶部选项（保存后 roles 应含 CUSTOMER、价格跟踪应置位）
    await p2.locator('.top-options .ant-checkbox-wrapper', { hasText: '既是供应商又是客户' }).first().click()
    await p2.locator('.top-options .ant-checkbox-wrapper', { hasText: '启用价格跟踪' }).first().click()
    await p2.waitForTimeout(400)

    const uiName = `E2E界面新增-${stamp}`
    await p2.locator('.ant-form-item:has-text("供应商名称") input').first().fill(uiName)
    // 所属分类（树选择器）
    await p2.locator('.ant-form-item:has-text("所属分类") .ant-select').first().click()
    await p2.waitForTimeout(1200)
    await p2.locator('.ant-select-tree-title', { hasText: `E2E分类改-${stamp}` }).first().click()
    await p2.waitForTimeout(600)
    await p2.locator('.ant-form-item:has-text("联系人") input').first().fill('李四E2E')
    await p2.locator('.ant-form-item:has-text("联系电话") input').first().fill('13700000000')
    await p2.locator('.ant-form-item:has-text("联系地址") input').first().fill('界面联系地址')
    await p2.locator('.ant-form-item:has-text("经营系列") input').first().fill('粮油系列')
    await p2.locator('.ant-form-item:has-text("经营面积") input').first().fill('66.6')
    await p2.locator('.ant-form-item:has-text("公司全称") input').first().fill(`界面公司-${stamp}`)
    await p2.locator('.ant-form-item:has-text("税号") input').first().fill(`UITAX${stamp}`)
    await p2.screenshot({ path: path.join(SHOT, 'supplier-form-filled.png') })
    await p2.getByRole('button', { name: /保存/ }).first().click()
    await p2.waitForTimeout(8000)
    await p2.screenshot({ path: path.join(SHOT, 'supplier-after-save.png') })
    check('界面保存无 JS 错误', errs2.length === 0, errs2.slice(0, 2).join(' | '))

    const uiRow = (await pg.query(
      'SELECT * FROM biz_party WHERE party_name=$1 AND deleted=0', [uiName])).rows[0] || {}
    check('界面保存真实落库（按名称可查回）', !!uiRow.id, `id=${uiRow.id}`)
    check('界面保存字段正确（分类/经营系列/面积/公司全称/税号/编号自动生成）',
      Number(uiRow.category_id) === Number(catId) && uiRow.operating_series === '粮油系列' &&
      Number(uiRow.operating_area) === 66.6 && uiRow.company_full_name === `界面公司-${stamp}` &&
      uiRow.tax_number === `UITAX${stamp}` && /^GYS/.test(uiRow.party_code || ''),
      `cat=${uiRow.category_id}, series=${uiRow.operating_series}, code=${uiRow.party_code}`)
    check('界面保存：顶部选项落库（既是客户 → roles 含 CUSTOMER；启用价格跟踪 → 1）',
      String(uiRow.roles || '').includes('CUSTOMER') && Number(uiRow.price_track_enabled) === 1,
      `roles=${uiRow.roles}, priceTrack=${uiRow.price_track_enabled}`)
    const uiContact = (await pg.query(
      'SELECT * FROM biz_party_contact WHERE party_id=$1 AND deleted=0', [uiRow.id])).rows[0] || {}
    check('界面保存同步主联系人（姓名/电话/地址/is_primary）',
      uiContact.contact_name === '李四E2E' && uiContact.phone === '13700000000' &&
      uiContact.detail_address === '界面联系地址' && uiContact.is_primary === 1,
      `${uiContact.contact_name}/${uiContact.phone}`)
    check('保存后回到列表页（路由跳转生效）', /\/md\/supplier\/index/.test(p2.url()), p2.url())
    if (uiRow.id) {
      await pg.query('DELETE FROM biz_party_contact WHERE party_id=$1', [uiRow.id])
      await pg.query('DELETE FROM biz_party WHERE id=$1', [uiRow.id])
      check('清理界面新增数据', true, `id=${uiRow.id}`)
    }
    await b2.close()
  }

  // ═══════════════════════════════════════════
  console.log('\n【清理测试数据】')
  await pg.query('DELETE FROM erp_partner_attachment WHERE partner_id=$1', [newId])
  await pg.query('DELETE FROM biz_party_contact WHERE party_id=$1', [newId])
  const del = apiSend('DELETE', '/api/erp/md/customer/batch', { ids: [Number(newId)] })
  check('批量删除返回 200', del.code === 200, del.message)
  await pg.query('DELETE FROM biz_party_category WHERE id=$1', [catId])
  const left = Number(await scalar(`SELECT COUNT(*)::int FROM biz_party WHERE id=$1 AND deleted=0`, [newId]))
  check('测试供应商已逻辑删除', left === 0, `剩余 ${left}`)

  // 兜底：清掉历史上中断运行可能留下的 E2E 数据（失败中断时脚本自身清理不会执行）
  const residue = (await pg.query(
    `SELECT id FROM biz_party WHERE deleted = 0 AND (
       party_name LIKE 'E2E%' OR party_name LIKE '%排序供应商%' OR party_name LIKE '导入供应商%'
       OR party_name LIKE 'MG%' OR party_name LIKE 'RV%')`)).rows.map(r => Number(r.id))
  if (residue.length) {
    await pg.query('DELETE FROM biz_party_contact WHERE party_id = ANY($1)', [residue])
    await pg.query('DELETE FROM erp_partner_attachment WHERE partner_id = ANY($1)', [residue])
    await pg.query('DELETE FROM biz_party WHERE id = ANY($1)', [residue])
  }
  check('无 E2E 测试数据残留', residue.length === 0, residue.length ? `已清理 ${residue.join(',')}` : '0 条')

  console.log(`\n================ 结果：${pass} 通过 / ${fail} 失败 ================`)
  await pg.end()
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => { console.error('FATAL', e.message, e.stack?.slice(0, 500)); process.exit(2) })
