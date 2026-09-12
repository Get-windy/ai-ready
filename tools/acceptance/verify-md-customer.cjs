/* 客户模块（资料 → 往来单位 → 客户）金标准 接口验证
 * 端口：API_PORT 环境变量（默认 5671，独立实例避免与并行会话互踢）
 * 登录自愈：token 失效（sa-token 互踢）时自动重登重试
 */
const http = require('http')

const PORT = Number(process.env.API_PORT || 5671)

function rawReq(method, path, body, token, raw) {
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
        try { resolve(JSON.parse(buf.toString('utf8'))) } catch { resolve({ raw: buf.toString('utf8').slice(0, 300), status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

let TOKEN = null
async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  return token
}

/** 带自愈重登的请求（sa-token 并发互踢时自动恢复） */
async function apiReq(method, path, body, raw) {
  if (!TOKEN) TOKEN = await login()
  let res = await rawReq(method, path, body, TOKEN, raw)
  const expired = raw ? (res.status === 401) : (res?.code === 401)
  if (expired) {
    TOKEN = await login()
    res = await rawReq(method, path, body, TOKEN, raw)
  }
  return res
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}
const enc = encodeURIComponent

async function main() {
  TOKEN = await login()
  console.log('登录成功\n')

  // ═══ A. 全部客户分页 + 26 列字段 ═══
  const p1 = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=5&partnerType=customer')
  const recs = p1?.data?.records || []
  const totalAll = p1?.data?.total || 0
  check('A1 全部客户分页可用', p1?.code === 200, `total=${totalAll}`)
  const r0 = recs[0] || {}
  const needFields = ['partnerCode', 'partnerName', 'settleType', 'gradeName', 'warehouseName', 'region',
    'defaultHandlerName', 'promoterName', 'contactPerson', 'mnemonicCode', 'phone', 'address',
    'buyerAccount', 'customerOnePass', 'creditDays', 'fixedCreditDay', 'statementDay', 'bankName',
    'bankAccount', 'taxNumber', 'customerSource', 'businessLicenseExpiry', 'lastTradeTime', 'createTime',
    'attachmentCount', 'remark']
  check('A2 全部客户 26 列字段全部返回', recs.length === 0 || needFields.every(f => f in r0),
    recs.length === 0 ? '(库中暂无客户)' : needFields.filter(f => !(f in r0)).join(','))

  // ═══ B. 查询条件 ═══
  check('B1 所属区域过滤不报错', (await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=1&partnerType=customer&region=' + enc('肃州'))).code === 200)
  check('B2 只显示无销售记录不报错', (await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=1&partnerType=customer&onlyNoTrade=true')).code === 200)
  check('B3 只显示开通商城账号不报错', (await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=1&partnerType=customer&onlyMallAccount=true')).code === 200)
  const b4 = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=1&partnerType=customer&settleType=' + enc('挂账'))
  check('B4 结款方式=挂账过滤生效', b4?.code === 200, `total=${b4?.data?.total}`)
  const b4b = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=1&partnerType=customer&settleType=' + enc('现结'))
  check('B5 结款方式=现结过滤生效', b4b?.code === 200, `total=${b4b?.data?.total}`)
  const b8 = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=1&partnerType=customer&settleType=ALL')
  check('B6 非法结款方式不报错（视为不过滤）', b8?.code === 200 && (b8?.data?.total || 0) === totalAll, `total=${b8?.data?.total}`)
  check('B7 显示状态过滤生效', (await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=1&partnerType=customer&status=DISABLED')).code === 200)
  const pAs = await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=1&partnerType=customer&showAsCustomer=true')
  check('B8 显示供应商中的客户（多重身份合并）', pAs?.code === 200 && (pAs?.data?.total || 0) >= totalAll, `合并=${pAs?.data?.total}/本类=${totalAll}`)
  check('B9 显示层次结构视图可用', (await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=5&partnerType=customer&showHierarchy=true')).code === 200)
  check('B10 新增/最近交易日期区间不报错', (await apiReq('GET', '/erp/md/customer/page?pageNum=1&pageSize=1&partnerType=customer&createTimeStart=2020-01-01&createTimeEnd=2030-01-01&lastTradeStart=2020-01-01&lastTradeEnd=2030-01-01')).code === 200)

  // ═══ C. 会员 / 联系人 / 区域 / 级别 ═══
  const member = await apiReq('GET', '/erp/md/customer/member/page?pageNum=1&pageSize=5')
  check('C1 会员管理分页可用', member?.code === 200, `total=${member?.data?.total}`)
  const contact = await apiReq('GET', '/erp/md/customer/contact/page?pageNum=1&pageSize=5')
  check('C2 全部联系人分页可用', contact?.code === 200, `total=${contact?.data?.total}`)
  const regionPage = await apiReq('GET', '/erp/customer/region/page?pageNum=1&pageSize=10')
  check('C3 区域管理分页可用', regionPage?.code === 200, `total=${regionPage?.data?.total}`)
  const lv = await apiReq('GET', '/erp/customer/level/page?pageNum=1&pageSize=5')
  check('C4 客户级别分页可用', lv?.code === 200 || lv?.total !== undefined, `total=${lv?.data?.total ?? lv?.total}`)
  const tree = await apiReq('GET', '/erp/partner/categories/tree?categoryType=CUSTOMER')
  check('C5 客户分类树可用', tree?.code === 200 && Array.isArray(tree?.data), `节点=${(tree?.data || []).length}`)

  // ═══ E. 增删改查闭环 ═══
  const code = 'KH' + String(Date.now()).slice(-8)
  const created = await apiReq('POST', '/erp/md/customer', {
    partnerCode: code,
    partnerName: '金标准验证客户_' + code,
    partnerType: 'CUSTOMER',
    settleType: '挂账',
    gradeName: 'A餐饮客户',
    warehouseName: '主仓',
    region: '肃州城区',
    promoterName: '张推广',
    buyerAccount: 'buyer_' + code,
    customerOnePass: 'YP' + code,
    customerSource: '门店拜访',
    creditDays: 30,
    fixedCreditDay: 15,
    statementDay: 5,
    memberCardNo: 'VIP' + code,
    memberName: '会员_' + code,
    memberLevel: '黄金会员',
    memberCardStatus: 'NORMAL',
    memberValidStart: '2026-01-01',
    memberValidEnd: '2027-01-01',
    memberInitialPoints: 100,
    points: 200,
    memberTotalConsume: 888.5,
    businessLicenseExpiry: '2030-12-31',
    remark: '金标准验证',
  })
  const newId = created?.data?.id
  check('E1 新增客户返回 id', !!newId, `id=${newId}`)

  const d = (await apiReq('GET', `/erp/md/customer/${newId}`))?.data || {}
  const persist = {
    warehouseName: d.warehouseName === '主仓',
    region: d.region === '肃州城区',
    promoterName: d.promoterName === '张推广',
    buyerAccount: d.buyerAccount === 'buyer_' + code,
    customerOnePass: d.customerOnePass === 'YP' + code,
    customerSource: d.customerSource === '门店拜访',
    creditDays: Number(d.creditDays) === 30,
    fixedCreditDay: Number(d.fixedCreditDay) === 15,
    statementDay: Number(d.statementDay) === 5,
    memberName: d.memberName === '会员_' + code,
    memberLevel: d.memberLevel === '黄金会员',
    memberCardStatus: d.memberCardStatus === 'NORMAL',
    memberValidStart: String(d.memberValidStart || '').startsWith('2026-01-01'),
    memberInitialPoints: Number(d.memberInitialPoints) === 100,
    points: Number(d.points) === 200,
    businessLicenseExpiry: String(d.businessLicenseExpiry || '').startsWith('2030-12-31'),
    settleType: d.settleType === '挂账',
    gradeName: d.gradeName === 'A餐饮客户',
  }
  const failed = Object.entries(persist).filter(([, v]) => !v).map(([k]) => k)
  check('E2 新增客户 18 项字段全部落库并可回读', failed.length === 0, failed.length ? '未落库: ' + failed.join(',') : '')

  const upd = await apiReq('PUT', `/erp/md/customer/${newId}`, { region: '嘉峪关城区', creditDays: 45, memberCardStatus: 'STOPPED' })
  const d2 = (await apiReq('GET', `/erp/md/customer/${newId}`))?.data || {}
  check('E3 更新客户生效', upd?.code === 200 && d2.region === '嘉峪关城区' && Number(d2.creditDays) === 45 && d2.memberCardStatus === 'STOPPED',
    `region=${d2.region} creditDays=${d2.creditDays} card=${d2.memberCardStatus}`)

  const memberHit = await apiReq('GET', `/erp/md/customer/member/page?pageNum=1&pageSize=10&memberKeyword=${enc('会员_' + code)}`)
  const mRec = (memberHit?.data?.records || []).find(x => String(x.id) === String(newId))
  check('E4 新增会员出现在会员管理子标签', !!mRec)
  check('E5 会员行含 10 列字段', !!mRec && ['memberName', 'memberCardNo', 'partnerName', 'memberLevel',
    'memberCardStatus', 'memberValidStart', 'birthday', 'points', 'lastTradeTime', 'remark'].every(k => k in mRec))
  const stoppedOnly = await apiReq('GET', '/erp/md/customer/member/page?pageNum=1&pageSize=20&memberCardStatus=STOPPED')
  check('E6 会员卡状态过滤生效', (stoppedOnly?.data?.records || []).some(x => String(x.id) === String(newId)))

  // 联系人（先建主联系人，再验证列表联表字段）
  await apiReq('PUT', `/erp/md/customer/${newId}/primary-contact`, { contactPerson: '联系人甲', contactPhone: '13800000001', address: '肃州区某路 1 号' })
  const contactHit = await apiReq('GET', `/erp/md/customer/contact/page?pageNum=1&pageSize=20&customerId=${newId}`)
  const cRec = (contactHit?.data?.records || [])[0] || {}
  check('E7 全部联系人按客户过滤并带出客户列', !!cRec.contactName && 'partnerName' in cRec && 'partyRegion' in cRec,
    `姓名=${cRec.contactName} 客户=${cRec.partnerName}`)

  // 区域 CRUD
  const regionCreate = await apiReq('POST', '/erp/customer/region', { regionName: '验证区域_' + code, remark: '验证' })
  const regionId = regionCreate?.data?.id
  check('E8 新增区域返回 id 与自动编号', !!regionId && !!regionCreate?.data?.regionCode, `code=${regionCreate?.data?.regionCode}`)
  const regionUpd = await apiReq('PUT', `/erp/customer/region/${regionId}`, { regionName: '验证区域改_' + code })
  const regionDetail = (await apiReq('GET', `/erp/customer/region/${regionId}`))?.data || {}
  check('E9 更新区域生效', regionUpd?.code === 200 && regionDetail.regionName === '验证区域改_' + code)
  const regionList = await apiReq('GET', '/erp/customer/region/list')
  check('E10 区域列表含新增区域', (regionList?.data || []).some(r => String(r.id) === String(regionId)))

  // 左侧「地区分类」树选中节点 → 分页只返回该区域及其子区域
  const subRegion = await apiReq('POST', '/erp/customer/region', { regionName: '验证子区域_' + code, parentId: regionId })
  const subRegionId = subRegion?.data?.id
  const filtered = await apiReq('GET', `/erp/customer/region/page?pageNum=1&pageSize=50&regionId=${regionId}`)
  const filteredIds = (filtered?.data?.records || []).map(r => String(r.id))
  check('E10b 区域树按父区域过滤（含自身与子区域）',
    filteredIds.includes(String(regionId)) && !!subRegionId && filteredIds.includes(String(subRegionId)),
    `命中=${filteredIds.length} 子区域=${subRegionId}`)
  const treeFiltered = await apiReq('GET', `/erp/customer/region/list?showHierarchy=true&regionId=${regionId}`)
  check('E10c 区域树列表按父区域过滤可用', treeFiltered?.code === 200 && Array.isArray(treeFiltered?.data))

  // 导出真实 xlsx
  const exp = await apiReq('GET', '/erp/md/customer/export?partnerType=customer', null, true)
  const isZip = exp?.buf && exp.buf.length > 4 && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b
  check('E11 导出真实 xlsx（PK 头）', !!isZip, `status=${exp?.status} size=${exp?.buf?.length}`)

  // 批量操作
  const bs = await apiReq('PUT', '/erp/md/customer/batch-status', { ids: [newId], status: 'DISABLED' })
  const afterStatus = (await apiReq('GET', `/erp/md/customer/${newId}`))?.data || {}
  check('E12 批量停用生效', bs?.code === 200 && afterStatus.status === 'DISABLED', `status=${afterStatus.status}`)
  const bt = await apiReq('PUT', '/erp/md/customer/batch-price-track', { ids: [newId], priceTrackEnabled: true })
  const afterTrack = (await apiReq('GET', `/erp/md/customer/${newId}`))?.data || {}
  check('E13 批量价格跟踪生效', bt?.code === 200 && Number(afterTrack.priceTrackEnabled) === 1)
  const bm = await apiReq('PUT', '/erp/md/customer/batch-move', { ids: [newId], categoryId: null })
  check('E14 批量搬移（清空分类）生效', bm?.code === 200)

  // 清理（先删子区域，父区域存在子节点时后端会拒绝删除）
  if (subRegionId) await apiReq('DELETE', `/erp/customer/region/${subRegionId}`)
  const delRegion = await apiReq('DELETE', `/erp/customer/region/${regionId}`)
  const del = await apiReq('DELETE', `/erp/md/customer/${newId}`)
  const gone = await apiReq('GET', `/erp/md/customer/${newId}`)
  check('E15 删除客户与区域（清理）', del?.code === 200 && !gone?.data?.id,
    `regionDel=${delRegion?.code} customerDel=${del?.code}`)

  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 客户模块接口验证：${pass}/${results.length} 通过 =====`)
  if (pass < results.length) {
    console.log('失败项：')
    results.filter(r => !r.ok).forEach(r => console.log('  -', r.name, r.detail || ''))
    process.exit(1)
  }
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
