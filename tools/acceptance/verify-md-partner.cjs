/* 其他往来单位（资料 → 往来单位 → 其他往来单位）金标准 接口验证
 *
 * 口径：biz_party.party_type=4（partnerType=OTHER），分类走 biz_party_category.party_type=4。
 * 端口：API_PORT 环境变量（默认 5681，独立实例避免与并行会话互踢）
 * 登录自愈：token 失效（sa-token 互踢）时自动重登重试
 * 依赖：导入验证需要 python + openpyxl 生成夹具（tools/make-partner-import-fixture.py）
 */
const http = require('http')
const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

/** 导出内容校验用的 xlsx 解析库（复用前端依赖，避免新增依赖） */
const EXCEL_LIB = process.env.EXCEL_LIB || 'I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx'

const PORT = Number(process.env.API_PORT || 5681)
const FIXTURE = path.join(__dirname, '../../tool-results', 'partner-import-fixture.xlsx')

function rawReq(method, reqPath, body, token, raw) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + reqPath, method,
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
        try { resolve(JSON.parse(buf.toString('utf8'))) } catch { resolve({ raw: buf.toString('utf8').slice(0, 300), status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

/** multipart/form-data 上传（避免 Node fetch 在本地环境的挂起问题） */
function rawUpload(reqPath, filePath, fields, token) {
  return new Promise((resolve, reject) => {
    const boundary = '----AtCodeBoundary' + Date.now()
    const parts = []
    for (const [k, v] of Object.entries(fields || {})) {
      parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="${k}"\r\n\r\n${v}\r\n`))
    }
    const fileName = path.basename(filePath)
    parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n`))
    parts.push(fs.readFileSync(filePath))
    parts.push(Buffer.from(`\r\n--${boundary}--\r\n`))
    const body = Buffer.concat(parts)
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + reqPath, method: 'POST',
      headers: {
        'Content-Type': `multipart/form-data; boundary=${boundary}`,
        'Content-Length': body.length,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf8')
        try { resolve(JSON.parse(text)) } catch { resolve({ raw: text.slice(0, 300), status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    r.write(body)
    r.end()
  })
}

const LOGIN_USER = process.env.LOGIN_USER || 'e2e_partner'
let TOKEN = null
async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  // 优先使用专用验收账号（避免与并行会话共用 admin 互相踢下线），不可用时回退 admin
  let res = await rawReq('POST', '/auth/login', {
    username: LOGIN_USER, password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  if (!(res?.data?.token || res?.data?.accessToken) && LOGIN_USER !== 'admin') {
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', {
      username: 'admin', password: 'admin123', tenantName: '系统租户',
      captcha: code2, captchaKey: cap2.data.uuid,
    })
  }
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  return token
}

/** 带自愈重登的请求（sa-token 并发互踢时自动恢复） */
async function apiReq(method, reqPath, body, raw) {
  if (!TOKEN) TOKEN = await login()
  let res = await rawReq(method, reqPath, body, TOKEN, raw)
  const expired = raw ? (res.status === 401) : (res?.code === 401)
  if (expired) {
    TOKEN = await login()
    res = await rawReq(method, reqPath, body, TOKEN, raw)
  }
  return res
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}
const enc = encodeURIComponent

function ensureFixture() {
  if (fs.existsSync(FIXTURE)) return true
  try {
    execFileSync('python', [path.join(__dirname, '../../tools', 'make-partner-import-fixture.py')], { stdio: 'ignore' })
    return fs.existsSync(FIXTURE)
  } catch {
    return false
  }
}

async function main() {
  TOKEN = await login()
  console.log('登录成功\n')

  // ═══ A. 口径与分类树 ═══
  const p1 = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=5&partnerType=OTHER')
  check('A1 其他往来单位分页可用', p1?.code === 200, `total=${p1?.data?.total}`)

  const tree = await apiReq('GET', '/erp/partner/categories/tree?categoryType=OTHER')
  const treeNames = (tree?.data || []).map(n => n.categoryName)
  check('A2 单位类别树可用（OTHER 口径）', tree?.code === 200 && Array.isArray(tree?.data), `节点=${treeNames.length}`)
  check('A3 预置类别含 银行/政府机构/劳务公司',
    ['银行', '政府机构', '劳务公司'].every(n => treeNames.includes(n)),
    treeNames.slice(0, 6).join('/'))
  check('A4 类别树不含其它往来单位类型的分类',
    !(tree?.data || []).some(n => String(n.categoryCode || '').startsWith('gysml') || String(n.categoryCode || '').startsWith('khml')))

  const bankNode = (tree?.data || []).find(n => n.categoryName === '银行')

  // 菜单 80513 双入口配置（列表 md/partner/index + 表单 md/partner/form）
  const menuRes = await apiReq('GET', '/menu/user/mega/tenant-admin?userId=1&tenantId=1')
  const flatten = (nodes) => (nodes || []).flatMap(n => [n, ...flatten(n.children)])
  const menu = flatten(menuRes?.data || []).find(m => m.menuCode === 'md:partner' || m.path === 'md/partner/form')
  check('A5 菜单 80513 双入口配置存在（md:partner）',
    !!menu && String(menu.path).includes('md/partner') && String(menu.listPath || menu.list_path || '').includes('md/partner'),
    menu ? `path=${menu.path} listPath=${menu.listPath || menu.list_path}` : 'menu not found')

  // ═══ B. 分类维护（用户可新建类型） ═══
  const newCatName = 'E2E类别' + String(Date.now()).slice(-6)
  const catCreate = await apiReq('POST', '/erp/partner/categories', {
    categoryName: newCatName, categoryType: 'OTHER', sortOrder: 99,
  })
  const tree2 = await apiReq('GET', '/erp/partner/categories/tree?categoryType=OTHER')
  const created = (tree2?.data || []).find(n => n.categoryName === newCatName)
  check('B1 新增单位类别（用户自建类型）', catCreate?.code === 200 && !!created, `id=${created?.id}`)

  let catRenamed = false
  if (created) {
    await apiReq('PUT', `/erp/partner/categories/${created.id}`, { categoryName: newCatName + '改', sortOrder: 99 })
    const tree3 = await apiReq('GET', '/erp/partner/categories/tree?categoryType=OTHER')
    catRenamed = (tree3?.data || []).some(n => n.categoryName === newCatName + '改')
  }
  check('B2 修改单位类别生效', catRenamed)

  // ═══ C. 新增单位（含类别/联系人/期初/多重身份） ═══
  const seq1 = (await apiReq('GET', '/erp/md/customer/next-seq?prefix=WLDW'))?.data?.seq
  const seq2 = (await apiReq('GET', '/erp/md/customer/next-seq?prefix=WLDW'))?.data?.seq
  check('C1 编号号段 next-seq 正常返回', typeof seq1 === 'number' && typeof seq2 === 'number' && seq2 >= seq1, `${seq1} → ${seq2}`)

  const stamp = String(Date.now()).slice(-8)
  const code = 'WLDW-' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '-' + stamp
  const name = '金标准其他往来单位_' + stamp
  const createRes = await apiReq('POST', '/erp/md/customer', {
    partnerType: 'OTHER',
    partnerCode: code,
    partnerName: name,
    partnerCategoryId: bankNode?.id,
    mnemonicCode: 'JZQTHZLDW',
    roles: 'OTHER,CUSTOMER',
    status: 'ENABLED',
    companyFullName: name + '有限公司',
    taxNumber: 'TAX' + stamp,
    address: '纳税人地址' + stamp,
    bankName: '中国银行',
    bankAccount: '6228' + stamp,
    openingReceivable: 1234.56,
    openingPayable: 654.32,
    remark: '金标准验证',
    settleType: '挂账',
  })
  const newId = Number(createRes?.data?.id || 0)
  check('C2 新增其他往来单位返回主键', createRes?.code === 200 && newId > 0, `id=${newId}`)

  if (newId) {
    await apiReq('PUT', `/erp/md/customer/${newId}/primary-contact`, {
      contactPerson: '张联系', contactPhone: '13900000001', address: '联系地址' + stamp,
    })
  }

  // ═══ D. 详情回填（编辑表单数据源） ═══
  const detail = (await apiReq('GET', `/erp/md/customer/${newId}`))?.data || {}
  check('D1 详情：基础字段回填一致',
    detail.partnerName === name && detail.partnerCode === code && detail.mnemonicCode === 'JZQTHZLDW',
    `${detail.partnerCode}/${detail.partnerName}`)
  check('D2 详情：单位类别回填（categoryId + 名称）',
    String(detail.categoryId) === String(bankNode?.id) && detail.categoryName === '银行',
    `categoryId=${detail.categoryId}/${detail.categoryName}`)
  check('D3 详情：联系人分区回填（主联系人）',
    detail.contactPerson === '张联系' && detail.contactPhone === '13900000001' && detail.address === '联系地址' + stamp,
    `${detail.contactPerson}/${detail.contactPhone}/${detail.address}`)
  check('D4 详情：期初应收/应付回填',
    Number(detail.openingReceivable) === 1234.56 && Number(detail.openingPayable) === 654.32,
    `${detail.openingReceivable}/${detail.openingPayable}`)
  check('D5 详情：结款方式口径为挂账/现结', detail.settleType === '挂账', String(detail.settleType))
  check('D6 详情：多重身份 roles 含 OTHER 与 CUSTOMER',
    String(detail.roles || '').includes('OTHER') && String(detail.roles || '').includes('CUSTOMER'),
    String(detail.roles))

  // 默认经手人（表单「其他信息」→ biz_party.default_handler_id/name，供销售/采购单据默认带出）
  const users = ((await apiReq('GET', '/user/list?pageNum=1&pageSize=1'))?.data?.records
    || (await apiReq('GET', '/user/list?pageNum=1&pageSize=1'))?.data || [])
  const someUser = users[0]
  if (someUser) {
    await apiReq('PUT', `/erp/md/customer/${newId}`, {
      partnerType: 'OTHER', partnerCode: code, partnerName: name,
      defaultHandlerId: someUser.id, defaultHandlerName: someUser.nickname || someUser.username,
    })
    const withHandler = (await apiReq('GET', `/erp/md/customer/${newId}`))?.data || {}
    check('D9 默认经手人回写（id + 姓名）',
      String(withHandler.defaultHandlerId) === String(someUser.id) && !!withHandler.defaultHandlerName,
      `${withHandler.defaultHandlerId}/${withHandler.defaultHandlerName}`)
  } else {
    check('D9 默认经手人回写（跳过：无用户数据）', false, 'no user')
  }

  // 证件信息（CertUploadList 口径：erp_partner_attachment, category='CERT'）
  const certCreate = await apiReq('POST', '/erp/partner/attachments', {
    partnerId: newId, fileName: '营业执照', fileUrl: '/uploads/e2e-cert.png',
    fileType: 'image/png', fileSize: 2048, category: 'CERT',
  })
  const certList = await apiReq('GET', `/erp/partner/attachments/by-partner/${newId}`)
  const certs = (certList?.data || []).filter(a => a.category === 'CERT')
  check('D7 证件信息落库（category=CERT）', certCreate?.code === 200 && certs.length === 1, `certs=${certs.length}`)
  const pageWithCert = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=10&partnerType=OTHER&keyword=' + enc(name))
  const rowWithCert = (pageWithCert?.data?.records || [])[0] || {}
  check('D8 列表「附件」列统计到证件', Number(rowWithCert.attachmentCount) >= 1, `attachmentCount=${rowWithCert.attachmentCount}`)

  // ═══ E. 修改闭环（含类别变更与停用） ═══
  const upd = await apiReq('PUT', `/erp/md/customer/${newId}`, {
    partnerType: 'OTHER',
    partnerCode: code,
    partnerName: name + '_改',
    partnerCategoryId: created?.id,
    roles: 'OTHER',
    status: 'DISABLED',
    settleType: '现结',
    openingReceivable: 1,
    openingPayable: 2,
  })
  const afterUpd = (await apiReq('GET', `/erp/md/customer/${newId}`))?.data || {}
  check('E1 修改生效（名称/类别/状态）',
    upd?.code === 200 && afterUpd.partnerName === name + '_改' &&
    String(afterUpd.categoryId) === String(created?.id) && afterUpd.status === 'DISABLED',
    `${afterUpd.partnerName}/${afterUpd.categoryName}/${afterUpd.status}`)
  check('E2 修改生效（结款方式/期初）',
    afterUpd.settleType === '现结' && Number(afterUpd.openingReceivable) === 1 && Number(afterUpd.openingPayable) === 2)

  // 此时单位挂在本轮自建类别下，验证分类引用保护
  if (created) {
    const delCatUsed = await apiReq('DELETE', `/erp/partner/categories/${created.id}`)
    check('E3 类别下有单位时删除被拒绝（引用保护）',
      delCatUsed?.code === 200 && typeof delCatUsed?.data === 'string' && delCatUsed.data.length > 0,
      String(delCatUsed?.data))
  }

  // ═══ F. 列表查询条件 ═══
  const f1 = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=10&partnerType=OTHER&keyword=' + enc(name))
  check('F1 关键字过滤命中新增单位', f1?.code === 200 && (f1?.data?.records || []).some(r => String(r.id) === String(newId)), `total=${f1?.data?.total}`)
  const f2 = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=10&partnerType=OTHER&status=DISABLED&keyword=' + enc(name))
  check('F2 显示状态=已停用过滤生效', f2?.code === 200 && (f2?.data?.records || []).length >= 1, `total=${f2?.data?.total}`)
  const f3 = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=10&partnerType=OTHER&status=ENABLED&keyword=' + enc(name))
  check('F3 显示状态=已启用过滤生效（停用记录不出现）', f3?.code === 200 && (f3?.data?.records || []).length === 0, `total=${f3?.data?.total}`)
  const f4 = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=10&partnerType=OTHER&categoryId=' + created?.id)
  check('F4 按单位类别过滤生效', f4?.code === 200 && (f4?.data?.records || []).every(r => String(r.categoryId) === String(created?.id)), `total=${f4?.data?.total}`)
  const f5 = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=1&partnerType=OTHER')
  check('F5 分页口径：total 与 records 一致', f5?.code === 200 && (f5?.data?.records || []).length <= 1, `total=${f5?.data?.total} records=${(f5?.data?.records || []).length}`)

  // ═══ G. 批量操作 ═══
  const g1 = await apiReq('PUT', '/erp/md/customer/batch-status', { ids: [newId], status: 'ENABLED' })
  const afterG1 = (await apiReq('GET', `/erp/md/customer/${newId}`))?.data || {}
  check('G1 批量启用生效', g1?.code === 200 && afterG1.status === 'ENABLED', String(afterG1.status))
  const g2 = await apiReq('PUT', '/erp/md/customer/batch-move', { ids: [newId], categoryId: bankNode?.id })
  const afterG2 = (await apiReq('GET', `/erp/md/customer/${newId}`))?.data || {}
  check('G2 批量搬移生效', g2?.code === 200 && String(afterG2.categoryId) === String(bankNode?.id), String(afterG2.categoryName))

  // ═══ H. 导出 / 模板 / 导入 ═══
  const exp = await apiReq('GET', '/erp/md/customer/export?partnerType=OTHER&title=' + enc('其他往来单位') + '&keyword=' + enc(name), null, true)
  const isZip = exp?.buf && exp.buf.length > 2 && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b
  check('H1 导出真实 xlsx（PK 头）', !!isZip, `status=${exp?.status} size=${exp?.buf?.length}`)
  if (isZip) {
    try {
      const XLSX = require(EXCEL_LIB)
      const wb = XLSX.read(exp.buf, { type: 'buffer' })
      const sheet = wb.Sheets[wb.SheetNames[0]]
      const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 })
      const headers = (rows[0] || []).map(v => String(v))
      const hitRow = (rows.slice(1)).find(r => (r || []).some(v => String(v).includes(name)))
      check('H1b 导出列含「单位类别/状态」', headers.includes('单位类别') && headers.includes('状态'), headers.join('/'))
      check('H1c 导出内容含当前查询命中的单位', !!hitRow, hitRow ? hitRow.slice(0, 4).join('|') : 'no row')
    } catch (e) {
      check('H1b 导出 xlsx 可解析', false, String(e.message).slice(0, 120))
    }
  }

  const tpl = await apiReq('GET', '/erp/md/customer/import-template?partnerType=other', null, true)
  const tplZip = tpl?.buf && tpl.buf.length > 2 && tpl.buf[0] === 0x50 && tpl.buf[1] === 0x4b
  check('H2 导入模板可下载（真实 xlsx）', !!tplZip, `status=${tpl?.status} size=${tpl?.buf?.length}`)

  let importedIds = []
  if (ensureFixture()) {
    const imp = await rawUpload('/erp/md/customer/import-excel', FIXTURE, { partnerType: 'other' }, TOKEN)
    const ok = imp?.code === 200 && Number(imp?.data?.success) === 2
    check('H3 Excel 导入真实落库（2 行）', ok, `total=${imp?.data?.total} success=${imp?.data?.success} failure=${imp?.data?.failure}`)
    const listImp = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=50&partnerType=OTHER&keyword=' + enc('金标准导入验证单位'))
    const recs = listImp?.data?.records || []
    importedIds = recs.map(r => r.id)
    check('H4 导入行可按关键字检索到', recs.length >= 2, `命中=${recs.length}`)
    check('H5 导入行按单位类别自动归入「银行」', recs.length > 0 && recs.every(r => r.categoryName === '银行'),
      recs.map(r => r.categoryName).join(','))
    check('H6 导入行编号按「WLDW-日期-序号」生成',
      recs.length > 0 && recs.every(r => /^WLDW-\d{8}-\d{3,}$/.test(String(r.partnerCode))),
      recs.map(r => r.partnerCode).join(','))
    check('H7 导入行联系人与联系电话落库', recs.length > 0 && recs.every(r => !!r.contactPerson && !!r.contactPhone),
      recs.map(r => r.contactPerson + '/' + r.contactPhone).join(' '))
  } else {
    check('H3 Excel 导入（跳过早：缺少 python+openpyxl 夹具）', false, 'fixture missing')
  }

  // ═══ I. 清理 ═══
  const atts = (await apiReq('GET', `/erp/partner/attachments/by-partner/${newId}`))?.data || []
  for (const a of atts) await apiReq('DELETE', `/erp/partner/attachments/${a.id}`)
  const del1 = await apiReq('DELETE', `/erp/md/customer/${newId}`)
  const gone1 = await apiReq('GET', `/erp/md/customer/${newId}`)
  check('I2 删除单位（清理）', del1?.code === 200 && !gone1?.data?.id, `del=${del1?.code}`)

  let delImported = 0
  for (const id of importedIds) {
    const r = await apiReq('DELETE', `/erp/md/customer/${id}`)
    if (r?.code === 200) delImported++
  }
  check('I3 删除导入数据（清理）', importedIds.length === 0 || delImported === importedIds.length,
    `${delImported}/${importedIds.length}`)

  if (created) {
    await apiReq('PUT', `/erp/md/customer/batch-status`, { ids: [newId], status: 'ENABLED' })
    const delCat2 = await apiReq('DELETE', `/erp/partner/categories/${created.id}`)
    const treeEnd = await apiReq('GET', '/erp/partner/categories/tree?categoryType=OTHER')
    const stillThere = (treeEnd?.data || []).some(n => n.categoryName === newCatName + '改')
    check('I4 删除自建类别（清理，无引用后可删）', delCat2?.code === 200 && !stillThere)
  }

  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 其他往来单位接口验证：${pass}/${results.length} 通过 =====`)
  if (pass < results.length) {
    console.log('失败项：')
    results.filter(r => !r.ok).forEach(r => console.log('  -', r.name, r.detail || ''))
    process.exit(1)
  }
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
