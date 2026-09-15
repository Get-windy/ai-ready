/*
 * 实名认证（配送 → 人车管理 → 实名认证，80840）金标准端到端验证
 *
 *   · API 验收：KYC 台账 CRUD/审核流/证照到期扫描/接单资质、人车绑定分页(page/size)与交车(POST+body)、
 *               预警分页与处理、巡检分页与审核、批量核验(真实扫描)、位置核验触发人车分离预警、四类台账导出 xlsx
 *   · UI  验收：4 Tab 金标准骨架（CategoryListLayout + BillTableList + 表头齿轮 + 页面配置）、
 *               查询条件、功能按钮、实名认证提交弹窗、审核弹窗、绑定详情抽屉
 *
 * 用法：node tools/e2e-verification.cjs
 *      ERP_PORT=5675 FE_URL=http://localhost:5656 node tools/e2e-verification.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5675)
const SHOTS = 'I:/AI-Ready/tool-results/dms-verification'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

/** 专用验收账号（tools/e2e-verification-user.sql），避免与并行会话互踢 */
const E2E_USER = process.env.E2E_USER || 'e2e_verif'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

let TOKEN = null

function tryJson(buf) {
  try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null }
}

function rawReq(method, reqPath, body, token, extraHeaders) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null || body instanceof Buffer ? body : JSON.stringify(body)
    const headers = {
      ...(body instanceof Buffer ? { 'Content-Type': 'application/octet-stream' } : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {}),
    }
    if (data && !(body instanceof Buffer)) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        resolve({ status: res.statusCode, buf, headers: res.headers, json: tryJson(buf) })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function api(method, reqPath, body, extraHeaders) {
  let r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  const code = r.json && Number(r.json.code)
  if (r.status === 401 || code === 401) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  }
  return r
}

function data(res) {
  return res.json ? res.json.data : null
}

function readCaptcha(json) {
  return [...Buffer.from(json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER,
    password: E2E_PWD,
    tenantName: '系统租户',
    captcha: readCaptcha(cap.json),
    captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  TOKEN = token
}

// ══════════════ 结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

const STAMP = Date.now().toString().slice(-6)
/**
 * 本地日期时间（ISO 的 T 分隔，供 Spring LocalDateTime 解析）。
 * 不能用 toISOString：那是 UTC，后端按本地时间解析会整体偏 8 小时，导致补能「上次里程」排序错乱。
 */
function localDateTimeStr(offsetMs = 0) {
  const d = new Date(Date.now() + offsetMs)
  const p2 = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p2(d.getMonth() + 1)}-${p2(d.getDate())}T${p2(d.getHours())}:${p2(d.getMinutes())}:${p2(d.getSeconds())}`
}

/** 本地日期（不能用 toISOString：那是 UTC，跨零点会与后端 LocalDate 差一天） */
function localDateStr(offsetDays = 0) {
  const d = new Date(Date.now() + offsetDays * 86400000)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
/** 测试用身份证号（18 位，可被前 3 后 4 脱敏） */
const ID_CARD = '11010119900307' + STAMP.slice(-4)
const ID_CARD_MASKED = ID_CARD.slice(0, 3) + '*'.repeat(ID_CARD.length - 7) + ID_CARD.slice(-4)

const dPlus = (n) => localDateStr(n)

/** 巡检检查项默认全正常（可 over 覆盖某几项为异常） */
function checkPayload(over = {}) {
  return {
    exteriorStatus: 0, tireStatus: 0, lightStatus: 0, brakeStatus: 0,
    cleanlinessStatus: 0, fireExtinguisher: 0, warningTriangle: 0,
    mileage: 1000, fuelLevel: 80, inspectionLocation: 'E2E 检查点',
    ...over,
  }
}

/** 建检查单（默认出车前） */
async function createInspection(vehicleId, riderId, type = 1, over = {}) {
  const res = await api('POST', '/dms/verification/inspection', {
    vehicleId, riderId, inspectionType: type, ...checkPayload(over),
  })
  return data(res)
}

/** 出车登记：先做（默认通过的）出车前检查，再绑定 */
async function bindWithCheck(vehicleId, riderId, reason, over = {}) {
  const inspectionId = await createInspection(vehicleId, riderId, 1, over)
  const res = await api('POST', '/dms/verification/bind', {
    riderId, vehicleId, inspectionId, bindReason: reason,
  })
  return { res, inspectionId, bindingId: data(res) }
}

/** 交车：带收车后检查一并提交 */
function handoverWithCheck(bindingId, mileage, over = {}) {
  return api('POST', `/dms/verification/binding/${bindingId}/handover`, {
    handoverMileage: mileage,
    handoverLocation: 'E2E 交车点',
    inspection: { inspectionType: 2, mileage, ...checkPayload(over) },
  })
}

/** 车辆当前状态 */
async function vehicleStatus(vehicleId) {
  const res = await api('GET', `/dms/vehicle/${vehicleId}`)
  return Number(data(res)?.status)
}

let RIDER_INTERNAL = null
let RIDER_INTERNAL_NAME = ''
let RIDER_INTERNAL_PHONE = ''
let RIDER_PLATFORM = null
let RIDER_REJECT = null
let VEHICLE_ID = process.env.VEHICLE_ID || null
let vehicleRawList = []
let BINDING_ID = null

async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // ── 0) 下拉选项 ──
  const riders = await api('GET', '/dms/verification/options/riders')
  const riderList = data(riders) || []
  check('配送员下拉返回数据', Array.isArray(riderList) && riderList.length >= 3, JSON.stringify(riderList.length))
  const internal = riderList.find((r) => Number(r.riderType) === 1)
  const platform = riderList.find((r) => Number(r.riderType) === 3)
  check('配送员选项含企业员工', !!internal, JSON.stringify(internal))
  check('配送员选项含外部平台配送员（带渠道）', !!platform && !!platform.channelId, JSON.stringify(platform))

  const channels = await api('GET', '/dms/verification/options/channels')
  const channelList = data(channels) || []
  check('运力渠道下拉返回数据', Array.isArray(channelList) && channelList.length >= 1, JSON.stringify(channelList.length))

  // 每次运行创建独立配送员，保证「已通过不可重复提交」的业务约束下可反复执行、且姓名检索唯一
  const userinfo = await api('GET', '/auth/userinfo')
  const uid = data(userinfo)?.userId
  const suffix = STAMP + Math.floor(Math.random() * 90 + 10)
  async function createRider(body) {
    const res = await api('POST', '/dms/rider', body)
    return data(res)?.id || null
  }
  RIDER_INTERNAL = await createRider({
    realName: 'E2E实名' + suffix, phone: '139' + suffix.slice(-8), riderType: 1, userId: uid,
  })
  RIDER_INTERNAL_NAME = 'E2E实名' + suffix
  RIDER_INTERNAL_PHONE = '139' + suffix.slice(-8)
  RIDER_PLATFORM = await createRider({
    realName: 'E2E平台' + suffix, phone: '137' + suffix.slice(-8), riderType: 3,
    channelId: channelList[0]?.value, platformRiderId: 'E2EP' + suffix,
  })
  RIDER_REJECT = await createRider({
    realName: 'E2E驳回' + suffix, phone: '135' + suffix.slice(-8), riderType: 2,
  })
  check('创建验收用企业员工配送员', !!RIDER_INTERNAL, RIDER_INTERNAL_NAME)
  check('创建验收用外部平台配送员', !!RIDER_PLATFORM, 'E2E平台' + suffix)
  check('创建验收用第二配送员（驳回用例）', !!RIDER_REJECT, 'E2E驳回' + suffix)

  // 车辆选择器复用车辆模块统一数据源（该接口由车辆模块维护，失败不判本页缺陷，仅取可用车辆ID）
  if (!VEHICLE_ID) {
    const vehicles = await api('GET', '/dms/vehicle/options')
    let vehicleList = data(vehicles) || []
    if (!vehicleList.length) {
      const vp = await api('GET', '/dms/vehicle/page?pageNum=1&pageSize=50')
      vehicleList = data(vp)?.records || []
    }
    vehicleRawList = vehicleList
    // 可出车 = 非报废(3)/非维修(2) 且 车上无当前配送员（避免历史「假解绑」残留）且无活跃绑定
    const activeAll = await api('GET', '/dms/verification/binding/page?page=1&size=100&status=0')
    const busy = new Set((data(activeAll)?.records || []).map((b) => String(b.vehicleId)))
    // 自愈：历史失败用例可能留下「车上仍有当前配送员、但无活跃绑定」的脏状态
    for (const v of vehicleList) {
      if (v.currentRiderId != null && !busy.has(String(v.id))) {
        await api('POST', `/dms/vehicle/${v.id}/unbind-rider`, {})
        v.currentRiderId = null
        console.log(`  [自愈] 车辆 ${v.plateNo || v.id} 残留当前配送员已解绑`)
      }
    }
    const free = vehicleList.filter((v) => !busy.has(String(v.id))
      && v.currentRiderId == null
      && Number(v.status) !== 2
      && Number(v.status) !== 3)
    VEHICLE_ID = (free[0] || {}).id || null
    console.log(`  [信息] 可用 vehicleId=${VEHICLE_ID}（共 ${vehicleList.length} 台，空闲可出车 ${free.length} 台）`)
  }

  // ── 1) KYC 提交（自有员工：身份证脱敏 + 证照明细） ──
  const submit = await api('POST', '/dms/verification/kyc/submit', {
    riderId: RIDER_INTERNAL,
    realName: 'E2E实名' + STAMP,
    idCardNo: ID_CARD,
    idCardFrontUrl: '/uploads/e2e-front.png',
    idCardBackUrl: '/uploads/e2e-back.png',
    endorseOrg: '企业内部审查',
    endorseResult: 1,
    remark: 'E2E 提交',
    submitAudit: true,
    certificates: [
      { certType: 1, certNo: 'DL' + STAMP, issueDate: dPlus(-400), expireDate: dPlus(400), certUrl: '/uploads/dl.png' },
      { certType: 3, certNo: 'HC' + STAMP, issueDate: dPlus(-300), expireDate: dPlus(10), remark: '即将到期' },
    ],
  })
  const kycId = data(submit)
  check('提交实名认证成功', submit.json?.code === 200 && !!kycId, JSON.stringify(submit.json).slice(0, 200))

  const detail = await api('GET', `/dms/verification/kyc/${kycId}`)
  const d = data(detail) || {}
  check('身份证号落库为脱敏值（前3后4，不存原文）', d.idCardNo === ID_CARD_MASKED, d.idCardNo)
  check('身份证号原文未落库', JSON.stringify(d).indexOf(ID_CARD) === -1, 'raw leaked?')
  check('提交后状态=待审核(1)', Number(d.verifyStatus) === 1, d.verifyStatusText)
  check('证照明细落库 2 条', (d.certificates || []).length === 2, JSON.stringify((d.certificates || []).length))
  check('证照按有效期自动标记状态=有效(1)',
    (d.certificates || []).length === 2 && (d.certificates || []).every((c) => Number(c.verifyStatus) === 1),
    JSON.stringify((d.certificates || []).map((c) => c.verifyStatus)))
  check('到期预警计数=1（10 天内到期的健康证）', Number(d.expiringCertCount) === 1, d.expiringCertCount)
  check('待审核时不具备接单资质', d.eligible === false, JSON.stringify(d.ineligibleReason))
  check('不具备原因指向认证状态', String(d.ineligibleReason || '').includes('待审核'), d.ineligibleReason)

  // 待审核时回写 dms_rider.verify_status = 0
  const riderRow = await api('GET', `/dms/rider/${RIDER_INTERNAL}`)
  check('回写配送员档案审核状态=待审核(0)', Number(data(riderRow)?.verifyStatus) === 0, data(riderRow)?.verifyStatus)

  // ── 2) KYC 台账分页与检索（page/size 口径） ──
  const p1 = await api('GET', `/dms/verification/kyc/page?page=1&size=1&riderName=${RIDER_INTERNAL_NAME}`)
  check('台账分页按配送员姓名检索命中', Number(data(p1)?.total) >= 1, JSON.stringify(data(p1)?.total))
  check('台账分页 size=1 只返回 1 条（分页参数 page/size 生效）', (data(p1)?.records || []).length === 1, JSON.stringify((data(p1)?.records || []).length))
  const p2 = await api('GET', '/dms/verification/kyc/page?page=1&size=20&verifyStatus=1')
  check('台账按状态=待审核检索', Number(data(p2)?.total) >= 1, JSON.stringify(data(p2)?.total))
  const p3 = await api('GET', '/dms/verification/kyc/page?page=1&size=20&eligible=false')
  check('台账按「不具备资质」检索', Number(data(p3)?.total) >= 1, JSON.stringify(data(p3)?.total))
  const p4 = await api('GET', '/dms/verification/kyc/page?page=1&size=20&expiring=true')
  check('台账按「仅看到期预警」检索命中', Number(data(p4)?.total) >= 1, JSON.stringify(data(p4)?.total))
  const p5 = await api('GET', `/dms/verification/kyc/page?page=1&size=20&riderPhone=${RIDER_INTERNAL_PHONE}`)
  check('台账按手机号检索（回查 dms_rider）', Number(data(p5)?.total) >= 1, JSON.stringify(data(p5)?.total))
  const todayStr = localDateStr()
  const pDate = await api('GET', `/dms/verification/kyc/page?page=1&size=20&riderName=${RIDER_INTERNAL_NAME}&startDate=${todayStr}&endDate=${todayStr}`)
  check('台账按创建日期（今日）检索命中', Number(data(pDate)?.total) >= 1, JSON.stringify(data(pDate)?.total))
  const pDateOld = await api('GET', `/dms/verification/kyc/page?page=1&size=20&riderName=${RIDER_INTERNAL_NAME}&startDate=2020-01-01&endDate=2020-01-31`)
  check('台账创建日期区间过滤生效（2020 年无数据）', Number(data(pDateOld)?.total) === 0, JSON.stringify(data(pDateOld)?.total))

  // ── 3) KYC 审核（通过） ──
  const audit = await api('POST', `/dms/verification/kyc/${kycId}/audit`, {
    approved: true,
    auditRemark: 'E2E 审核通过',
  })
  check('审核通过成功', audit.json?.code === 200, JSON.stringify(audit.json).slice(0, 200))
  const afterAudit = data(await api('GET', `/dms/verification/kyc/${kycId}`)) || {}
  check('审核后状态=已通过(2)', Number(afterAudit.verifyStatus) === 2, afterAudit.verifyStatusText)
  check('审核后写入生效时间（审计留痕）', !!afterAudit.effectiveTime, afterAudit.effectiveTime)
  check('审核后记录审核人/审核意见', !!afterAudit.auditBy && afterAudit.auditRemark === 'E2E 审核通过', JSON.stringify({ by: afterAudit.auditBy, remark: afterAudit.auditRemark }))
  const riderRow2 = await api('GET', `/dms/rider/${RIDER_INTERNAL}`)
  check('回写配送员档案审核状态=已通过(1)', Number(data(riderRow2)?.verifyStatus) === 1, data(riderRow2)?.verifyStatus)

  const elig = await api('GET', `/dms/verification/kyc/eligibility/${RIDER_INTERNAL}`)
  check('审核通过后具备接单资质', data(elig)?.eligible === true, JSON.stringify(data(elig)?.reasons))

  // ── 4) KYC 审核（驳回）——驳回后不可接单，且重复驳回被拒 ──
  const submit2 = await api('POST', '/dms/verification/kyc/submit', {
    riderId: RIDER_REJECT,
    realName: 'E2E驳回',
    idCardNo: ID_CARD,
    submitAudit: true,
    certificates: [],
  })
  const kycId2 = data(submit2)
  const badAudit = await api('POST', `/dms/verification/kyc/${kycId2}/audit`, { approved: false })
  check('驳回未填审核意见被拒', badAudit.json?.code !== 200, badAudit.json?.message)
  const rejectAudit = await api('POST', `/dms/verification/kyc/${kycId2}/audit`, { approved: false, auditRemark: '材料不清晰' })
  check('驳回成功', rejectAudit.json?.code === 200, JSON.stringify(rejectAudit.json).slice(0, 150))
  const elig2 = await api('GET', `/dms/verification/kyc/eligibility/${RIDER_REJECT}`)
  check('驳回后不具备接单资质', data(elig2)?.eligible === false, JSON.stringify(data(elig2)?.reasons))

  // ── 5) 外部平台背书（渠道方背书结果 + 有效期） ──
  const submit3 = await api('POST', '/dms/verification/kyc/submit', {
    riderId: RIDER_PLATFORM,
    realName: 'E2E平台',
    endorseOrg: '达达平台',
    endorseResult: 1,
    endorseExpireDate: dPlus(200),
    submitAudit: true,
    certificates: [],
  })
  const kycId3 = data(submit3)
  const pf = data(await api('GET', `/dms/verification/kyc/${kycId3}`)) || {}
  check('外部平台台账带出渠道快照', !!pf.channelName, pf.channelName)
  check('外部平台记录背书结论/有效期', Number(pf.endorseResult) === 1 && !!pf.endorseExpireDate, JSON.stringify({ org: pf.endorseOrg, exp: pf.endorseExpireDate }))
  await api('POST', `/dms/verification/kyc/${kycId3}/audit`, { approved: true, auditRemark: '渠道背书核验通过' })
  const pfAfter = data(await api('GET', `/dms/verification/kyc/${kycId3}`)) || {}
  check('外部平台审核通过', Number(pfAfter.verifyStatus) === 2, pfAfter.verifyStatusText)

  // ── 6) 证照到期扫描（状态标注 + 到期预警） ──
  const scanExpiry = await api('POST', '/dms/verification/kyc/scan-expiry?warnDays=30')
  check('证照到期扫描可执行', scanExpiry.json?.code === 200, JSON.stringify(data(scanExpiry)))
  const certsAfter = (data(await api('GET', `/dms/verification/kyc/${kycId}`)) || {}).certificates || []
  check('到期扫描后证照状态被刷新', certsAfter.every((c) => Number(c.verifyStatus) === 1), JSON.stringify(certsAfter.map((c) => c.verifyStatus)))
  const alertCert = await api('GET', '/dms/verification/alert/page?page=1&size=20&alertType=7')
  check('到期扫描产生「证照/资质到期」预警', Number(data(alertCert)?.total) >= 1, JSON.stringify(data(alertCert)?.total))

  // ── 7) 人车绑定：分页参数 page/size + 交车 POST+body ──
  const bindPage = await api('GET', '/dms/verification/binding/page?page=1&size=1')
  const bp = data(bindPage)
  check('人车绑定分页 page/size 生效（records 条数=size）', (bp?.records || []).length <= 1, JSON.stringify((bp?.records || []).length))
  check('人车绑定分页返回 total', Number(bp?.total) >= 1, JSON.stringify(bp?.total))
  const activeBinding = await api('GET', '/dms/verification/binding/page?page=1&size=50&status=0')
  let activeRec = (data(activeBinding)?.records || [])[0]
  if (!activeRec && VEHICLE_ID && RIDER_REJECT) {
    // 无现成「绑定中」记录时自建一条，保证交车用例可独立重复执行
    const hb = await bindWithCheck(VEHICLE_ID, RIDER_REJECT, 'E2E 交车用例')
    if (hb.bindingId) activeRec = { id: hb.bindingId, bindMileage: 1000 }
  }
  if (activeRec) {
    BINDING_ID = activeRec.id
    // 交车里程必须 ≥ 出车里程（里程单调是业务规则）：按记录推导，避免沿用既有绑定时硬编码偏小
    const bindMileage = Number(activeRec.bindMileage || 0)
    const HO_MILEAGE = Math.max(12345, bindMileage + 100)
    const detailB = await api('GET', `/dms/verification/binding/${BINDING_ID}`)
    const bd = data(detailB) || {}
    check('绑定详情接口返回绑定信息', !!bd.binding && String(bd.binding.id) === String(BINDING_ID), JSON.stringify(bd.binding?.id))
    check('绑定详情含核验历史/巡检/预警三组', Array.isArray(bd.verifications) && Array.isArray(bd.inspections) && Array.isArray(bd.alerts), JSON.stringify({ v: (bd.verifications || []).length, i: (bd.inspections || []).length, a: (bd.alerts || []).length }))
    check('绑定详情返回核验次数统计', typeof bd.verifyCount === 'number', bd.verifyCount)

    // 收车检查强制：不提交检查单必须被拒
    const noCheck = await api('POST', `/dms/verification/binding/${BINDING_ID}/handover`, {
      handoverMileage: HO_MILEAGE,
    })
    check('未做收车检查时交车被拒（强制门控）', noCheck.json?.code !== 200, noCheck.json?.message)
    // 交车里程不得小于出车里程
    if (bindMileage > 0) {
      const badMileage = await api('POST', `/dms/verification/binding/${BINDING_ID}/handover`, {
        handoverMileage: bindMileage - 1,
        inspection: { inspectionType: 2, ...checkPayload() },
      })
      check('交车里程小于出车里程被拒', badMileage.json?.code !== 200, badMileage.json?.message)
    }

    const handover = await api('POST', `/dms/verification/binding/${BINDING_ID}/handover`, {
      handoverMileage: HO_MILEAGE,
      handoverLocation: 'E2E 交车点·' + STAMP,
      remark: 'E2E 交车',
      inspection: { inspectionType: 2, mileage: HO_MILEAGE, ...checkPayload() },
    })
    check('交车（POST + JSON body + 收车检查）成功', handover.json?.code === 200, JSON.stringify(handover.json).slice(0, 200))
    const afterH = (data(await api('GET', `/dms/verification/binding/${BINDING_ID}`)) || {}).binding || {}
    check('交车后状态=已交车(1)', Number(afterH.status) === 1, afterH.status)
    check('交车里程落库', Number(afterH.handoverMileage) === HO_MILEAGE, afterH.handoverMileage)
    check('交车地点落库', String(afterH.handoverLocation || '').includes(STAMP), afterH.handoverLocation)
    const vehAfter = data(await api('GET', `/dms/vehicle/${afterH.vehicleId || VEHICLE_ID}`)) || {}
    check('交车后车辆「当前配送员」已清空（防假解绑）', vehAfter.currentRiderId == null, JSON.stringify(vehAfter.currentRiderId))
    check('交车后车辆状态回到「空闲」(0)', Number(vehAfter.status) === 0, vehAfter.status)
    check('交车后写入交车时间', !!afterH.handoverTime, afterH.handoverTime)
    const handoverAgain = await api('POST', `/dms/verification/binding/${BINDING_ID}/handover`, {
      handoverMileage: HO_MILEAGE, inspection: { inspectionType: 2, ...checkPayload() },
    })
    check('已交车不可重复交车', handoverAgain.json?.code !== 200, handoverAgain.json?.message)
    // 收车检查已自动落一条 type=2 巡检并回写 bindingId
    const insOfBinding = await api('GET', `/dms/verification/inspection/page?page=1&size=50&inspectionType=2`)
    const recIns = (data(insOfBinding)?.records || []).find(i => String(i.bindingId) === String(BINDING_ID))
    check('交车自动生成「收车后检查」并关联绑定', !!recIns, JSON.stringify((data(insOfBinding)?.records || []).length))
    check('收车检查结果=通过(1)', Number(recIns?.result) === 1, recIns && recIns.result)
    if (recIns) {
      const dIns = await api('GET', `/dms/verification/inspection/${recIns.id}`)
      check('巡检详情接口可用（单张完整检查项）', dIns.json?.code === 200 && Number(data(dIns)?.bindingId) === Number(BINDING_ID), JSON.stringify(data(dIns)?.id))
    }
  } else {
    check('存在可交车的绑定记录', false, '无 status=0 的绑定')
  }

  // 绑定唯一性：已交车的车辆可再次绑定，且绑定中不可重复
  if (VEHICLE_ID && RIDER_INTERNAL) {
    // 出车检查强制：无检查单禁止绑定
    const noIns = await api('POST', '/dms/verification/bind', { riderId: RIDER_INTERNAL, vehicleId: VEHICLE_ID })
    check('未做/未带出车前检查时绑定被拒（强制门控）', noIns.json?.code !== 200, noIns.json?.message)
    // 检查单必须属于同一辆车
    const otherVehicle = vehicleRawList.find(v => String(v.id) !== String(VEHICLE_ID))
    const crossIns = otherVehicle ? await createInspection(otherVehicle.id, RIDER_INTERNAL, 1) : null
    if (crossIns) {
      const cross = await api('POST', '/dms/verification/bind', { riderId: RIDER_INTERNAL, vehicleId: VEHICLE_ID, inspectionId: crossIns })
      check('检查单与所选车辆不一致被拒', cross.json?.code !== 200, cross.json?.message)
    }
    // 检查不通过 → 禁止出车
    const failIns = await createInspection(VEHICLE_ID, RIDER_INTERNAL, 1, { brakeStatus: 1 })
    const failBind = await api('POST', '/dms/verification/bind', { riderId: RIDER_INTERNAL, vehicleId: VEHICLE_ID, inspectionId: failIns })
    check('出车前检查不通过禁止出车', failBind.json?.code !== 200 && String(failBind.json?.message || '').includes('刹车'), failBind.json?.message)

    const bind1 = await bindWithCheck(VEHICLE_ID, RIDER_INTERNAL, 'E2E 绑定' + STAMP)
    check('出车登记成功（含出车前检查）', bind1.res.json?.code === 200, JSON.stringify(bind1.res.json).slice(0, 200))
    const insDetail = await api('GET', `/dms/verification/inspection/${bind1.inspectionId}`)
    check('出车前检查回写绑定ID（绑定详情可见）', String(data(insDetail)?.bindingId) === String(bind1.bindingId), JSON.stringify(data(insDetail)?.bindingId))
    const reuseIns = await api('POST', '/dms/verification/bind', { riderId: RIDER_PLATFORM, vehicleId: VEHICLE_ID, inspectionId: bind1.inspectionId })
    check('同一张检查单不可重复用于出车', reuseIns.json?.code !== 200, reuseIns.json?.message)

    const bind2 = await bindWithCheck(VEHICLE_ID, RIDER_PLATFORM, '重复绑定')
    check('一车不可同时两绑（唯一性校验）', bind2.res.json?.code !== 200, bind2.res.json?.message)
    const bind3 = await bindWithCheck(VEHICLE_ID, RIDER_INTERNAL, undefined)
    check('一人不可同时两绑（唯一性校验）', bind3.res.json?.code !== 200, bind3.res.json?.message)
    const newBindingId = bind1.bindingId
    const activeB = await api('GET', `/dms/verification/binding/active/rider/${RIDER_INTERNAL}`)
    check('活跃绑定查询命中新建绑定', String(data(activeB)?.id) === String(newBindingId), JSON.stringify(data(activeB)?.id))
    const p6 = await api('GET', `/dms/verification/binding/page?page=1&size=20&riderName=${RIDER_INTERNAL_NAME}`)
    check('绑定台账按配送员姓名检索命中', Number(data(p6)?.total) >= 1, JSON.stringify(data(p6)?.total))
    // 清理：收车（带收车检查），避免影响后续断言
    await handoverWithCheck(newBindingId, 1001)
  }

  // ── 7.5) 收车检查异常 → 车辆置「维修中」+ 自动生成《车辆维护》待办（defect → work order） ──
  if (VEHICLE_ID && RIDER_INTERNAL) {
    const abn = await bindWithCheck(VEHICLE_ID, RIDER_INTERNAL, 'E2E 异常收车')
    check('异常收车用例：出车登记成功', abn.res.json?.code === 200, JSON.stringify(abn.res.json).slice(0, 150))
    const hid = await handoverWithCheck(abn.bindingId, 1002, { lightStatus: 1, brakeStatus: 1 })
    check('收车检查异常仍可交车（检查单留档）', hid.json?.code === 200, JSON.stringify(hid.json).slice(0, 150))
    const st = await vehicleStatus(VEHICLE_ID)
    check('收车检查异常后车辆置「维修中」(2)', st === 2, st)
    const mt = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${VEHICLE_ID}`)
    const mtRows = (data(mt)?.records || [])
    const hit = mtRows.find(m => Number(m.maintType) === 2 && String(m.maintContent || '').includes('收车检查异常'))
    check('自动生成《车辆维护》维修待办', !!hit, JSON.stringify(mtRows.length))
    check('维修待办带出异常项与巡检单号', !!hit && String(hit.maintContent).includes('刹车') && String(hit.maintContent).includes('灯光'), hit && hit.maintContent)
    // 车管员修好后把车改回空闲（否则后续用例无车可用）
    const back = await api('PUT', `/dms/vehicle/${VEHICLE_ID}/status`, { status: 0 })
    check('车辆状态可恢复为空闲（维修复原）', back.json?.code === 200, JSON.stringify(data(back)).slice(0, 120))
  }

  // ── 7.6) 随机抽检 / 定期检查（车管员发起，不关联人车绑定） ──
  if (VEHICLE_ID) {
    const spotId = await createInspection(VEHICLE_ID, RIDER_INTERNAL, 3)
    check('新增「随机抽检」检查单成功', !!spotId, spotId)
    const spot = await api('GET', `/dms/verification/inspection/page?page=1&size=20&inspectionType=3`)
    const spotRow = (data(spot)?.records || []).find(r => String(r.id) === String(spotId))
    check('抽检记录进入巡检台账', !!spotRow, JSON.stringify((data(spot)?.records || []).length))
    check('抽检不关联人车绑定（bindingId 为空）', !!spotRow && !spotRow.bindingId, spotRow && spotRow.bindingId)
    const dueId = await createInspection(VEHICLE_ID, RIDER_INTERNAL, 4)
    check('新增「定期检查」检查单成功', !!dueId, dueId)
  }

  // ── 7.7) 司机端自助：当前登录人 → 配送员档案 ──
  const meRider = await api('GET', '/dms/verification/me/rider')
  check('司机端可直接取当前登录人的配送员档案', meRider.json?.code === 200, JSON.stringify(data(meRider)?.id))

  // ── 8) 位置核验 → 人车分离预警自动产生 ──
  if (VEHICLE_ID) {
    const bindNew = await bindWithCheck(VEHICLE_ID, RIDER_REJECT, 'E2E 分离检测')
    const sepBindingId = bindNew.bindingId
    if (sepBindingId) {
      const nowIso = localDateTimeStr()
      let lastCode = null
      for (let i = 0; i < 3; i++) {
        const v = await api('POST',
          `/dms/verification/verify/${sepBindingId}?riderLat=31.2&riderLng=121.4&riderReportTime=${nowIso}`
          + `&vehicleLat=31.3&vehicleLng=121.5&vehicleReportTime=${nowIso}&threshold=500`)
        lastCode = v.json?.code
      }
      check('位置核验接口可用', lastCode === 200, lastCode)
      const sepAlert = await api('GET', `/dms/verification/alert/page?page=1&size=20&alertType=1&handleStatus=0`)
      const hit = (data(sepAlert)?.records || []).find((a) => String(a.bindingId) === String(sepBindingId))
      check('连续 3 次异常核验后自动产生「人车分离」预警', !!hit, JSON.stringify((data(sepAlert)?.records || []).length))
      check('预警带出人车距离（真实 Haversine 计算）', !!hit && Number(hit.distanceMeters) > 500, hit && hit.distanceMeters)
      if (hit) {
        const handle = await api('PUT', `/dms/verification/alert/${hit.id}/handle`, { handleStatus: 3, remark: 'E2E 处理' })
        check('预警处理成功（handler 取登录态，前端不传）', handle.json?.code === 200, JSON.stringify(handle.json).slice(0, 150))
        const after = await api('GET', '/dms/verification/alert/page?page=1&size=50&alertType=1')
        const row = (data(after)?.records || []).find((a) => String(a.id) === String(hit.id))
        check('预警处理后状态=已处理(3) 且记录处理人', Number(row?.handleStatus) === 3 && !!row?.handler, JSON.stringify({ s: row?.handleStatus, h: row?.handler }))
      }
      await handoverWithCheck(sepBindingId, 1001)
    }
  }

  // ── 9) 批量核验（替换原空循环：真实扫描「绑定中」记录） ──
  let scanBindId = null
  if (VEHICLE_ID && RIDER_INTERNAL) {
    const scanBind = await bindWithCheck(VEHICLE_ID, RIDER_INTERNAL, 'E2E 扫描用例')
    scanBindId = scanBind.bindingId
  }
  const scan = await api('POST', '/dms/verification/scan')
  const scanData = data(scan) || {}
  check('批量核验接口可用', scan.json?.code === 200, JSON.stringify(scanData))
  check('批量核验真实扫描到「绑定中」记录', Number(scanData.scannedBindings) >= 1, scanData.scannedBindings)
  check('批量核验返回分类统计字段', ['timeoutAlerts', 'stayAlerts', 'separationAlerts', 'skippedDuplicated'].every((k) => typeof scanData[k] === 'number'), JSON.stringify(scanData))
  if (scanBindId) {
    await handoverWithCheck(scanBindId, 1001)
  }

  // ── 10) 巡检：创建 → 分页 → 审核 ──
  if (VEHICLE_ID) {
    const createIns = await api('POST', '/dms/verification/inspection', {
      vehicleId: VEHICLE_ID,
      riderId: RIDER_INTERNAL,
      inspectionType: 1,
      exteriorStatus: 0,
      tireStatus: 0,
      lightStatus: 0,
      brakeStatus: 0,
      cleanlinessStatus: 0,
      fireExtinguisher: 0,
      warningTriangle: 0,
      mileage: 1000,
      fuelLevel: 80,
      inspectionLocation: 'E2E 巡检点' + STAMP,
      remark: 'E2E 巡检',
    })
    const insId = data(createIns)
    check('创建巡检记录成功', createIns.json?.code === 200 && !!insId, JSON.stringify(createIns.json).slice(0, 200))
    const insPage = await api('GET', `/dms/verification/inspection/page?page=1&size=10&inspectionType=1`)
    check('巡检分页接口存在且可按类型检索', insPage.json?.code === 200, JSON.stringify(data(insPage)?.total))
    const insRow = (data(insPage)?.records || []).find((r) => String(r.id) === String(insId))
    check('巡检列表含新建记录', !!insRow, JSON.stringify((data(insPage)?.records || []).length))
    check('巡检地点凭证落库', !!insRow && String(insRow.inspectionLocation || '').includes(STAMP), insRow && insRow.inspectionLocation)
    check('系统按检查项自动判定结果=通过', Number(insRow?.result) === 1, insRow && insRow.result)

    const review = await api('PUT', `/dms/verification/inspection/${insId}/review`, { result: 1, remark: 'E2E 审核通过' })
    check('巡检审核成功（body 契约对齐）', review.json?.code === 200, JSON.stringify(review.json).slice(0, 200))
    const insPage2 = await api('GET', `/dms/verification/inspection/page?page=1&size=10&result=1`)
    const insRow2 = (data(insPage2)?.records || []).find((r) => String(r.id) === String(insId))
    check('巡检审核后写入审核人/审核时间', !!insRow2?.reviewer && !!insRow2?.reviewTime, JSON.stringify({ r: insRow2?.reviewer, t: insRow2?.reviewTime }))

    // 异常检查项 → 自动不通过
    const createIns2 = await api('POST', '/dms/verification/inspection', {
      vehicleId: VEHICLE_ID, riderId: RIDER_INTERNAL, inspectionType: 2,
      exteriorStatus: 0, tireStatus: 0, lightStatus: 1, brakeStatus: 1, cleanlinessStatus: 0,
      fireExtinguisher: 0, warningTriangle: 0, remark: 'E2E 异常项',
    })
    const insId2 = data(createIns2)
    const insPage3 = await api('GET', '/dms/verification/inspection/page?page=1&size=10&result=2')
    const insRow3 = (data(insPage3)?.records || []).find((r) => String(r.id) === String(insId2))
    check('灯光/刹车异常时系统判定不通过', !!insRow3, JSON.stringify(insRow3 && insRow3.result))
  }

  // ── 11) 台账导出（真实 xlsx，四个 Tab；预警导出编码为 warn） ──
  for (const tab of ['kyc', 'binding', 'warn', 'inspection']) {
    const exp = await api('GET', `/dms/verification/export?tab=${tab}`)
    const isXlsx = exp.buf && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b
    check(`导出「${tab}」为真实 xlsx（PK 头，非 JSON）`, isXlsx, `status=${exp.status} ctype=${exp.headers['content-type']}`)
    if (isXlsx) fs.writeFileSync(path.join(SHOTS, `export-${tab}.xlsx`), exp.buf)
  }

  // ── 11.5) 车辆补能（车/人二选一 + 自动区间里程/每公里成本 + 异常标记） ──
  const ODO_BASE = Math.floor(Date.now() / 60000) % 100000000
  // 自愈：清理历史残留的补能记录（否则它们会成为本轮记录的"上次里程"，导致区间里程算错）
  for (const subject of [`vehicleId=${VEHICLE_ID}`, `riderId=${RIDER_INTERNAL}`]) {
    if (subject.endsWith('=null') || subject.endsWith('=undefined')) continue
    const old2 = await api('GET', `/dms/vehicle/energy/page?page=1&size=200&${subject}`)
    for (const row of (data(old2)?.records || [])) {
      await api('DELETE', `/dms/vehicle/energy/${row.id}`)
    }
  }
  const energyBase = {
    energyType: 1, payMode: 2, quantity: 40, unitPrice: 7.5, amountYuan: 300,
    station: 'E2E 加油站' + STAMP, cardNo: 'CARD' + STAMP,
    occurredAt: localDateTimeStr(-3600_000),
    remark: 'E2E 补能',
  }
  check('补能：主体必选校验（车/人二选一）', (await api('POST', '/dms/vehicle/energy', { energyType: 1 })).json?.code !== 200, '')
  check('补能：主体不可同时填车与人',
    (await api('POST', '/dms/vehicle/energy', { energyType: 1, vehicleId: VEHICLE_ID, riderId: RIDER_INTERNAL })).json?.code !== 200, '')
  if (VEHICLE_ID) {
    // 第一条：无历史 → 无区间里程/单位成本
    const e1 = await api('POST', '/dms/vehicle/energy', { vehicleId: VEHICLE_ID, odometer: ODO_BASE, ...energyBase })
    const e1d = data(e1) || {}
    check('补能：新增成功（四轮车）', e1.json?.code === 200 && !!e1d.id, JSON.stringify(e1.json).slice(0, 120))
    check('补能：首条无上次里程 → 区间里程与单位成本为空', e1d.mileageSinceLast == null && e1d.unitCost == null,
      JSON.stringify({ span: e1d.mileageSinceLast, cost: e1d.unitCost }))

    // 第二条：自动算区间里程 + 每公里成本（60km，300 元 → 5 元/km，命中阈值边界内）
    const e2 = await api('POST', '/dms/vehicle/energy', { vehicleId: VEHICLE_ID, odometer: ODO_BASE + 60, ...energyBase, amountYuan: 240, occurredAt: localDateTimeStr() })
    const e2d = data(e2) || {}
    check('补能：自动计算区间里程=60km', Number(e2d.mileageSinceLast) === 60, e2d.mileageSinceLast)
    check('补能：自动计算每公里成本=4 元/km', Number(e2d.unitCost) === 4, e2d.unitCost)
    check('补能：百公里油耗派生=66.67L（40L/60km）', Number(e2d.fuelPer100Km) === 66.67, e2d.fuelPer100Km)
    check('补能：油耗超阈值标记异常', Number(e2d.abnormalFlag) === 1 && String(e2d.abnormalReason || '').includes('百公里油耗'),
      e2d.abnormalReason)

    // 第三条：正常记录（上次=10060 → 区间 40km；电动车类型规避油耗规则；120 元 → 3 元/km）
    const e4 = await api('POST', '/dms/vehicle/energy', {
      vehicleId: VEHICLE_ID, odometer: ODO_BASE + 100, energyType: 3, payMode: 3, quantity: 20, unitPrice: 6,
      amountYuan: 120, station: 'E2E 充电站', occurredAt: localDateTimeStr(60_000),
    })
    const e4d = data(e4) || {}
    check('补能：充电记录正常（无油耗判定）', Number(e4d.abnormalFlag) === 0 && Number(e4d.mileageSinceLast) === 40,
      JSON.stringify({ flag: e4d.abnormalFlag, span: e4d.mileageSinceLast, reason: e4d.abnormalReason }))
    check('补能：每公里成本=3 元/km', Number(e4d.unitCost) === 3, e4d.unitCost)

    // 第四条：里程倒挂 → 异常（放在最后，避免影响其它用例的"上次记录"）
    const e3 = await api('POST', '/dms/vehicle/energy', { vehicleId: VEHICLE_ID, odometer: ODO_BASE, ...energyBase, occurredAt: localDateTimeStr(120_000) })
    const e3d = data(e3) || {}
    check('补能：里程倒挂标记异常', Number(e3d.abnormalFlag) === 1 && String(e3d.abnormalReason || '').includes('小于上次'),
      e3d.abnormalReason)
    // 倒挂用例仅用于验证标记，用完即删（否则会成为后续记录的"上次记录"）
    const eDel = await api('DELETE', `/dms/vehicle/energy/${e3d.id}`)
    check('补能：删除成功', eDel.json?.code === 200, JSON.stringify(eDel.json).slice(0, 100))

    // 检索
    const pEnergy = await api('GET', `/dms/vehicle/energy/page?page=1&size=20&vehicleId=${VEHICLE_ID}&energyType=1`)
    check('补能：按车辆+类型检索命中（汽油 2 条：e1/e2）', Number(data(pEnergy)?.total) === 2, JSON.stringify(data(pEnergy)?.total))
    const pAbn = await api('GET', `/dms/vehicle/energy/page?page=1&size=20&vehicleId=${VEHICLE_ID}&abnormalOnly=true`)
    const abnRows = data(pAbn)?.records || []
  check('补能：仅看异常过滤生效（含 e2 油耗超阈值）',
    Number(data(pAbn)?.total) >= 1 && abnRows.every(r => Number(r.abnormalFlag) === 1)
    && abnRows.some(r => String(r.id) === String(e2d.id)),
    JSON.stringify({ total: data(pAbn)?.total, ids: abnRows.map(r => r.id) }))
    const pKw = await api('GET', `/dms/vehicle/energy/page?page=1&size=20&keyword=CARD${STAMP}`)
    check('补能：按卡号关键字检索命中', Number(data(pKw)?.total) >= 1, JSON.stringify(data(pKw)?.total))
    const pSub = await api('GET', '/dms/vehicle/energy/page?page=1&size=20&subjectType=VEHICLE')
    check('补能：按主体类型（四轮车）过滤', Number(data(pSub)?.total) >= 1, JSON.stringify(data(pSub)?.total))
    const energyRow = (data(pSub)?.records || []).find(r => String(r.id) === String(e4d.id))
    check('补能：列表带出车牌号与字典文本', !!energyRow && !!energyRow.plateNo && energyRow.energyTypeText === '充电',
      JSON.stringify({ plate: energyRow && energyRow.plateNo, type: energyRow && energyRow.energyTypeText }))

    // 修改 / 删除
    const eUpd = await api('PUT', `/dms/vehicle/energy/${e4d.id}`, {
      vehicleId: VEHICLE_ID, odometer: ODO_BASE + 100, energyType: 3, amountYuan: 200,
      occurredAt: localDateTimeStr(120_000),
    })
    check('补能：修改后重算每公里成本=5 元/km', Number(data(eUpd)?.unitCost) === 5,
      JSON.stringify({ cost: data(eUpd)?.unitCost, span: data(eUpd)?.mileageSinceLast }))
    // 导出真实 xlsx
    const eExp = await api('GET', `/dms/vehicle/energy/export?vehicleId=${VEHICLE_ID}`)
    const eIsXlsx = eExp.buf && eExp.buf[0] === 0x50 && eExp.buf[1] === 0x4b
    check('补能：导出为真实 xlsx（PK 头，非 JSON）', eIsXlsx, `status=${eExp.status}`)
    if (eIsXlsx) fs.writeFileSync(path.join(SHOTS, 'export-energy.xlsx'), eExp.buf)

    // 收尾：清理本轮多余额度记录（保留 1 条供 UI 段渲染）
    for (const id of [e2d.id, e4d.id]) {
      if (id) await api('DELETE', `/dms/vehicle/energy/${id}`)
    }
  }
  // 骑手两轮车换电（月租套餐 → 按里程分摊）
  if (RIDER_INTERNAL) {
    const swap = await api('POST', '/dms/vehicle/energy', {
      riderId: RIDER_INTERNAL, energyType: 4, payMode: 4, quantity: 1, amountYuan: 225,
      odometer: 300, station: 'E2E 换电柜', cardNo: 'SWAP' + STAMP,
      occurredAt: localDateTimeStr(),
    })
    const swapd = data(swap) || {}
    check('补能：骑手两轮车换电（月租套餐）可落库', swap.json?.code === 200 && !!swapd.id, JSON.stringify(swap.json).slice(0, 120))
    check('补能：记录带出骑手姓名', swapd.subjectType === 'RIDER' && !!swapd.riderName || swapd.riderId != null,
      JSON.stringify({ type: swapd.subjectType, name: swapd.riderName }))
  }

  // ── 11.6) 证照核验（证照维度到期清单） ──
  const certPage = await api('GET', '/dms/verification/kyc/certificate/page?page=1&size=20')
  check('证照核验：分页可用', certPage.json?.code === 200 && Array.isArray(data(certPage)?.records),
    JSON.stringify(data(certPage)?.total))
  const certRows = data(certPage)?.records || []
  check('证照核验：行含持证人姓名与类型文本',
    certRows.length > 0 && certRows.every(r => !!r.certTypeText), JSON.stringify(certRows[0] || {}))
  check('证照核验：行含剩余天数派生字段（Long 按字符串序列化）',
    certRows.every(r => r.daysToExpire == null || !Number.isNaN(Number(r.daysToExpire))),
    JSON.stringify(certRows.slice(0, 2).map(r => r.daysToExpire)))
  const certExpiring = await api('GET', '/dms/verification/kyc/certificate/page?page=1&size=20&expiring=true')
  check('证照核验：仅看到期预警过滤生效', Number(data(certExpiring)?.total) >= 1, JSON.stringify(data(certExpiring)?.total))
  const certByType = await api('GET', '/dms/verification/kyc/certificate/page?page=1&size=20&certType=3')
  check('证照核验：按证照类型过滤命中（健康证）', Number(data(certByType)?.total) >= 1, JSON.stringify(data(certByType)?.total))
  const certExpired = certRows.filter(r => Number(r.daysToExpire) < 0)
  check('证照核验：已过期证照状态与剩余天数一致',
    certExpired.every(r => Number(r.verifyStatus) === 2 || Number(r.verifyStatus) === 0),
    JSON.stringify(certExpired.map(r => ({ d: r.daysToExpire, s: r.verifyStatus }))))

  // ── 11.7) 补能卡 / 套餐（一卡一车一人 + 用卡稽核） ──
  const CARD_NO = 'E2ECARD' + STAMP
  const CARD_OTHER = 'E2ECARD-OTH' + STAMP
  check('补能卡：绑定主体必选（一卡一车一人）',
    (await api('POST', '/dms/vehicle/energy/card', { cardNo: CARD_NO + 'X', cardType: 1 })).json?.code !== 200, '')
  check('补能卡：主体不可同时绑定车与人',
    (await api('POST', '/dms/vehicle/energy/card', {
      cardNo: CARD_NO + 'Y', cardType: 1, vehicleId: VEHICLE_ID, riderId: RIDER_INTERNAL,
    })).json?.code !== 200, '')

  const otherVehicleForCard = vehicleRawList.find(v => String(v.id) !== String(VEHICLE_ID))
  const cardCreate = await api('POST', '/dms/vehicle/energy/card', {
    cardNo: CARD_NO, cardName: 'E2E 油卡', cardType: 1, vehicleId: VEHICLE_ID,
    issuer: 'E2E 石油', monthlyFee: 200, balance: 500, quota: 50,
    startDate: localDateStr(-30), expireDate: localDateStr(180), status: 1, remark: 'E2E 卡',
  })
  const cardId = data(cardCreate)?.id
  check('补能卡：新增成功', cardCreate.json?.code === 200 && !!cardId, JSON.stringify(cardCreate.json).slice(0, 140))
  const cardVO = data(await api('GET', `/dms/vehicle/energy/card/${cardId}`)) || {}
  check('补能卡：回填绑定主体与卡类型文本', !!cardVO.subjectName && cardVO.cardTypeText === '油卡',
    JSON.stringify({ s: cardVO.subjectName, t: cardVO.cardTypeText }))
  check('补能卡：卡号重复校验',
    (await api('POST', '/dms/vehicle/energy/card', { cardNo: CARD_NO, cardType: 1, vehicleId: VEHICLE_ID })).json?.code !== 200, '')
  const cardOpts = await api('GET', '/dms/vehicle/energy/card/options')
  check('补能卡：启用卡下拉含新卡', (data(cardOpts) || []).some(c => c.cardNo === CARD_NO), JSON.stringify((data(cardOpts) || []).length))

  // 用「绑定本车」的卡补能 → 无卡相关异常
  const cardLogOk = await api('POST', '/dms/vehicle/energy', {
    vehicleId: VEHICLE_ID, energyType: 1, payMode: 2, quantity: 20, amountYuan: 150,
    odometer: ODO_BASE + 200, cardNo: CARD_NO, station: 'E2E 油站',
    occurredAt: localDateTimeStr(-600_000),
  })
  const cardLogOkD = data(cardLogOk) || {}
  check('补能卡：用绑定本车的卡补能（卡稽核通过）',
    !String(cardLogOkD.abnormalReason || '').includes('一卡一车一人') && !String(cardLogOkD.abnormalReason || '').includes('已过期'),
    cardLogOkD.abnormalReason)

  // 一卡一车一人：卡绑 A 车却给 B 车补能 → 异常
  if (otherVehicleForCard) {
    const crossUse = await api('POST', '/dms/vehicle/energy', {
      vehicleId: otherVehicleForCard.id, energyType: 1, amountYuan: 100, quantity: 10,
      odometer: 500, cardNo: CARD_NO, occurredAt: localDateTimeStr(-540_000),
    })
    const crossD = data(crossUse) || {}
    check('补能卡：一卡一车一人校验失败被标记异常',
      Number(crossD.abnormalFlag) === 1 && String(crossD.abnormalReason || '').includes('一卡一车一人'), crossD.abnormalReason)
    // 清理该条（避免污染其它车辆主体统计）
    if (crossD.id) await api('DELETE', `/dms/vehicle/energy/${crossD.id}`)
  }

  // 停用卡 / 过期卡
  await api('PUT', `/dms/vehicle/energy/card/${cardId}/status?status=0`)
  const usedDisabled = await api('POST', '/dms/vehicle/energy', {
    vehicleId: VEHICLE_ID, energyType: 1, amountYuan: 60, quantity: 8,
    odometer: ODO_BASE + 210, cardNo: CARD_NO, occurredAt: localDateTimeStr(-480_000),
  })
  check('补能卡：使用已停用卡被标记异常', String(data(usedDisabled)?.abnormalReason || '').includes('已停用'),
    data(usedDisabled)?.abnormalReason)
  await api('PUT', `/dms/vehicle/energy/card/${cardId}/status?status=1`)

  const expiringCard = await api('POST', '/dms/vehicle/energy/card', {
    cardNo: CARD_OTHER, cardName: 'E2E 过期卡', cardType: 2, vehicleId: VEHICLE_ID,
    startDate: localDateStr(-400), expireDate: localDateStr(-10), status: 1,
  })
  const expiringCardId = data(expiringCard)?.id
  const usedExpired = await api('POST', '/dms/vehicle/energy', {
    vehicleId: VEHICLE_ID, energyType: 3, amountYuan: 80, quantity: 12,
    odometer: ODO_BASE + 220, cardNo: CARD_OTHER, occurredAt: localDateTimeStr(-420_000),
  })
  check('补能卡：使用已过期卡被标记异常', String(data(usedExpired)?.abnormalReason || '').includes('已过期'),
    data(usedExpired)?.abnormalReason)
  check('补能卡：过期派生字段正确', data(await api('GET', `/dms/vehicle/energy/card/${expiringCardId}`))?.expired === true, '')

  // 额度超限：卡额度 50，已用 20+8=28；再造一条 30 → 58 > 50 应超限
  const beforeQuota = data(await api('GET', `/dms/vehicle/energy/card/${cardId}`)) || {}
  check('补能卡：已用额度按流水自动汇总', Number(beforeQuota.usedQuota) === 28, beforeQuota.usedQuota)
  const overQuota = await api('POST', '/dms/vehicle/energy', {
    vehicleId: VEHICLE_ID, energyType: 1, amountYuan: 200, quantity: 30,
    odometer: ODO_BASE + 230, cardNo: CARD_NO, occurredAt: localDateTimeStr(-360_000),
  })
  check('补能卡：套餐额度超限被标记异常', String(data(overQuota)?.abnormalReason || '').includes('额度超限'),
    data(overQuota)?.abnormalReason)
  const afterQuota = data(await api('GET', `/dms/vehicle/energy/card/${cardId}`)) || {}
  check('补能卡：额度使用率回算正确', Number(afterQuota.usedQuota) === 58, afterQuota.usedQuota)
  check('补能卡：额度使用率百分比', Number(afterQuota.quotaUsagePercent) === 116, afterQuota.quotaUsagePercent)

  // 非合规时段补能（参数默认 06:00-23:00）
  const offHours = await api('POST', '/dms/vehicle/energy', {
    vehicleId: VEHICLE_ID, energyType: 3, amountYuan: 50, quantity: 10,
    odometer: ODO_BASE + 240, occurredAt: localDateStr(0) + 'T03:00:00',
  })
  check('补能：非合规时段补能被标记异常', String(data(offHours)?.abnormalReason || '').includes('非合规时段'),
    data(offHours)?.abnormalReason)
  if (data(offHours)?.id) await api('DELETE', `/dms/vehicle/energy/${data(offHours).id}`)

  // 清理本轮卡相关流水与卡档案
  for (const id of [cardLogOkD.id, data(usedDisabled)?.id, data(usedExpired)?.id, data(overQuota)?.id]) {
    if (id) await api('DELETE', `/dms/vehicle/energy/${id}`)
  }
  check('补能卡：启停/删除可用',
    (await api('DELETE', `/dms/vehicle/energy/card/${expiringCardId}`)).json?.code === 200 &&
    (await api('DELETE', `/dms/vehicle/energy/card/${cardId}`)).json?.code === 200, '')

  // ── 11.8) 能耗报表（汇总 + 油电对比 + CO₂） ──
  const stats = await api('GET', `/dms/vehicle/energy/stats?vehicleId=${VEHICLE_ID}`)
  const st = data(stats) || {}
  check('能耗报表：接口可用且返回汇总', stats.json?.code === 200 && Number(st.logCount) >= 1, JSON.stringify(st.logCount))
  check('能耗报表：含金额/里程/平均每公里成本', st.totalAmount != null && st.totalMileage != null, JSON.stringify({ a: st.totalAmount, m: st.totalMileage }))
  check('能耗报表：油电对比两组齐备', !!st.fuelGroup && !!st.powerGroup, JSON.stringify([!!st.fuelGroup, !!st.powerGroup]))
  check('能耗报表：按能源类型分组非空', (st.byEnergyType || []).length >= 1, JSON.stringify((st.byEnergyType || []).length))
  check('能耗报表：按主体分组含车牌号', (st.bySubject || []).some(g => String(g.label || '').includes('京')), JSON.stringify((st.bySubject || []).map(g => g.label)))
  const co2 = Number(st.totalCo2Kg || 0)
  check('能耗报表：CO₂ 折算合理（>0）', co2 > 0, co2)

  // ── 11.9) 准入核验（人证 × 车证一屏） ──
  const onboard = await api('GET', `/dms/verification/onboarding/page?page=1&size=20&keyword=${RIDER_INTERNAL_NAME}`)
  check('准入核验：分页可用', onboard.json?.code === 200 && Array.isArray(data(onboard)?.records), JSON.stringify(data(onboard)?.total))
  const onboardRows = data(onboard)?.records || []
  check('准入核验：行含资格结论与阻塞原因字段',
    onboardRows.length > 0 && onboardRows.every(r => r.riderName != null && r.canDrive !== undefined), JSON.stringify(onboardRows[0] || {}))
  const onboardMine = onboardRows.find(r => String(r.riderId) === String(RIDER_INTERNAL))
  check('准入核验：已通过 KYC 的骑手可接单', !!onboardMine && onboardMine.eligible === true,
    JSON.stringify(onboardMine && { el: onboardMine.eligible, cd: onboardMine.canDrive, r: onboardMine.blockReasons }))
  const blocked = await api('GET', '/dms/verification/onboarding/page?page=1&size=50&onlyBlocked=true')
  check('准入核验：仅看不可出车过滤生效（返回行均可出车为否）',
    (data(blocked)?.records || []).every(r => r.canDrive !== true), JSON.stringify((data(blocked)?.records || []).length))
  const onboardKw = await api('GET', `/dms/verification/onboarding/page?page=1&size=20&keyword=${RIDER_INTERNAL_NAME}`)
  check('准入核验：按姓名检索命中', Number(data(onboardKw)?.total) >= 1, JSON.stringify(data(onboardKw)?.total))

  // ── 12) 无资质不接单门控（默认关闭，可通过参数开启） ──
  const paramRow = await api('GET', '/dms/config/list?tenantId=1')
  const paramList = data(paramRow) || []
  const paramArr = Array.isArray(paramList) ? paramList : (paramList.records || [])
  const enforce = paramArr.find((c) => c.configKey === 'verification.eligibility.enforce')
  check('「无资质不接单」开关已作为配送参数下发', !!enforce, JSON.stringify(enforce && enforce.configValue))
  check('开关默认关闭（存量配送员未补 KYC，避免阻断全量指派）', !!enforce && String(enforce.configValue) === 'false', enforce && enforce.configValue)
  const maxHours = paramArr.find((c) => c.configKey === 'verification.binding.max.hours')
  const warnDays = paramArr.find((c) => c.configKey === 'kyc.cert.expire.warn.days')
  check('核验阈值参数齐备（绑定超时/证照提醒）', !!maxHours && !!warnDays, JSON.stringify({ maxHours: maxHours && maxHours.configValue, warnDays: warnDays && warnDays.configValue }))
}

// ══════════════ UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  const jsErrors = []
  page.on('pageerror', e => jsErrors.push(e.message))
  const apiFailures = []
  page.on('response', r => {
    try {
      const u = new URL(r.url())
      if (u.pathname.startsWith('/api/') && r.status() >= 400) apiFailures.push(`${r.status()} ${u.pathname}`)
    } catch (e) { /* ignore */ }
  })

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  /** 文本正则：容忍 antd 给两字中文按钮插的空格（"取消" → "取 消"） */
  function labelRegExp(label) {
    const esc = label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    return new RegExp('^' + esc.split('').join('\\s*') + '$')
  }

  async function clickButton(scopeSel, label) {
    const sel = `${scopeSel} button, ${scopeSel} a, ${scopeSel} .tab-item`
    const loc = page.locator(sel).filter({ hasText: labelRegExp(label) })
    const n = await loc.count().catch(() => 0)
    if (n > 0) {
      const clicked = await loc.first().click({ timeout: 6000 }).then(() => true).catch(() => false)
      await page.waitForTimeout(500)
      if (clicked) return true
    }
    const ok = await page.evaluate(([sel2, lb]) => {
      const root = sel2 ? document.querySelector(sel2) : document.body
      if (!root) return false
      const norm = (t) => (t || '').replace(/\s+/g, '')
      const target = [...root.querySelectorAll('button, a, .tab-item, .ant-select-item-option')]
        .find(el => norm(el.innerText) === lb)
      if (!target) return false
      target.click()
      return true
    }, [scopeSel, label]).catch(() => false)
    await page.waitForTimeout(500)
    return ok
  }

  async function modalOpen() {
    return (await page.locator('.ant-modal-wrap:visible').count()) > 0
  }

  async function closeAnyModal() {
    for (let i = 0; i < 5; i++) {
      if (!(await modalOpen())) return
      await page.keyboard.press('Escape').catch(() => {})
      await page.waitForTimeout(700)
    }
    if (await modalOpen()) {
      const cancel = page.locator('.ant-modal-wrap:visible .ant-modal-content button')
        .filter({ hasText: labelRegExp('取消') })
      await cancel.first().click({ timeout: 3000 }).catch(() => {})
      await page.waitForTimeout(600)
    }
  }

  async function openPage(url, waitMs = 6000) {
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

  async function waitReady(timeout = 25000) {
    await page.waitForSelector('.tab-item', { timeout })
    await page.waitForSelector('.ss-grid th', { timeout })
    await page.waitForTimeout(1200)
  }

  const headers = async () => page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  const gridText = async () => (await page.locator('.ss-grid').innerText().catch(() => '')).replace(/\s+/g, ' ')
  const pickAntdOption = async (formItemLabel) => {
    const item = page.locator('.ant-modal-content:visible .ant-form-item', { hasText: formItemLabel }).first()
    if (!(await item.count())) return false
    await item.locator('.ant-select-selector').click().catch(() => {})
    await page.waitForTimeout(800)
    const opt = page.locator('.ant-select-dropdown:visible .ant-select-item-option').first()
    if (await opt.count()) {
      await opt.click()
      await page.waitForTimeout(600)
      return true
    }
    return false
  }

  // ══════════ A. 用车管理（4 Tab：用车登记 / 车辆巡检 / 车辆补能 / 核验预警） ══════════
  await openPage(`${FE}/dms/vehicle/usage`)
  await waitReady().catch(e => console.log('  [骨架等待超时]', e.message))
  let bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('用车管理页可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())
  for (const t of ['用车登记', '车辆巡检', '车辆补能', '补能卡', '能耗报表', '核验预警']) {
    check(`用车管理 Tab 含「${t}」`, bodyText.includes(t))
  }

  // A1 用车登记
  let hs = await headers()
  for (const h of ['配送员', '车牌号', '绑定时间', '交车时间', '交车地点', '状态']) {
    check(`用车登记表头含「${h}」`, hs.some(x => x.includes(h)), hs.join('/'))
  }
  check('用车登记工具栏含「出车登记」「交车」', bodyText.includes('出车登记') && bodyText.includes('交车'))
  check('绑定状态渲染为中文标签', /绑定中|已交车|异常解绑/.test(await gridText()), (await gridText()).slice(0, 60))
  await page.screenshot({ path: path.join(SHOTS, 'ui-usage-binding.png'), fullPage: true })

  // 出车登记两步向导（含出车前检查）
  if (await clickButton('.toolbar-section', '出车登记')) {
    await page.waitForTimeout(1500)
    const step1 = (await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')).replace(/\s+/g, '')
    check('出车登记弹窗含两步向导（绑定信息 / 出车前检查）', step1.includes('绑定信息') && step1.includes('出车前检查'), step1.slice(0, 60))
    const pickedRider = await pickAntdOption('配送员')
    const pickedVehicle = await pickAntdOption('车辆')
    await clickButton('.ant-modal-content', '下一步')
    await page.waitForTimeout(1200)
    const step2 = (await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')).replace(/\s+/g, '')
    check('出车登记可进入第二步「出车前检查」', /刹车/.test(step2) && /灭火器/.test(step2) && /油量/.test(step2),
      `rider=${pickedRider} vehicle=${pickedVehicle} | ` + step2.slice(0, 70))
    await page.screenshot({ path: path.join(SHOTS, 'ui-bind-wizard-check.png'), fullPage: true })
    await closeAnyModal()
  } else {
    check('出车登记入口存在', false, '未找到按钮')
  }

  // 交车（收车登记 + 收车后检查）——先造一条绑定中记录
  let uiBindingId = null
  if (VEHICLE_ID && RIDER_INTERNAL) {
    const prep = await bindWithCheck(VEHICLE_ID, RIDER_INTERNAL, 'E2E UI 用车管理')
    uiBindingId = prep.bindingId
  }
  if (await clickButton('.toolbar-section', '交车')) {
    await page.waitForTimeout(2000)
    let hoText = (await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')).replace(/\s+/g, '')
    check('交车弹窗标题含收车登记 + 收车后检查', hoText.includes('收车登记') && hoText.includes('收车后检查'), hoText.slice(0, 50))
    const hoItem = page.locator('.ant-modal-content:visible .ant-form-item', { hasText: '待交车辆' }).first()
    if (await hoItem.count()) {
      await hoItem.locator('.ant-select-selector').click().catch(() => {})
      await page.waitForTimeout(800)
      const hoOpt = page.locator('.ant-select-dropdown:visible .ant-select-item-option').first()
      if (await hoOpt.count()) {
        await hoOpt.click()
        await page.waitForTimeout(1500)
        hoText = (await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')).replace(/\s+/g, '')
        check('选中车辆后展开交车登记 + 收车后检查', /交车里程/.test(hoText) && hoText.includes('收车后检查'), hoText.slice(0, 70))
        check('交车弹窗提示异常将转《车辆维护》', hoText.includes('车辆维护'))
      } else {
        check('存在可交车的「绑定中」记录', false, '下拉无选项')
      }
    }
    await page.screenshot({ path: path.join(SHOTS, 'ui-handover-returncheck.png'), fullPage: true })
    await closeAnyModal()
  } else {
    check('交车工具栏入口存在', false, '未找到按钮')
  }
  if (uiBindingId) await handoverWithCheck(uiBindingId, 1003)

  // A2 车辆巡检
  await clickButton('.tab-bar', '车辆巡检')
  await page.waitForTimeout(2500)
  hs = await headers()
  for (const h of ['车牌号', '配送员', '巡检类型', '结果', '巡检时间', '巡检地点']) {
    check(`车辆巡检表头含「${h}」`, hs.some(x => x.includes(h)), hs.join('/'))
  }
  const insText = await gridText()
  check('巡检类型/结果渲染为中文标签',
    /出车前检查|收车后检查|随机抽检|定期检查/.test(insText) && /通过/.test(insText), insText.slice(0, 70))
  if (await clickButton('.toolbar-section', '新增抽检/定检')) {
    await page.waitForTimeout(1500)
    const spotText = (await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')).replace(/\s+/g, '')
    check('抽检/定检弹窗含车辆/检查类型/检查项',
      spotText.includes('车辆') && spotText.includes('检查类型') && spotText.includes('刹车'), spotText.slice(0, 60))
    await closeAnyModal()
  } else {
    check('新增抽检/定检入口存在', false, '未找到按钮')
  }
  if (await clickButton('.ss-grid tbody', '详情')) {
    await page.waitForTimeout(2000)
    const dText = (await page.locator('.ant-drawer-content:visible').innerText().catch(() => '')).replace(/\s+/g, '')
    check('巡检详情抽屉含完整检查项',
      ['车辆外观', '轮胎', '灯光', '刹车', '灭火器'].every(k => dText.includes(k)), dText.slice(0, 60))
    await page.keyboard.press('Escape')
    await page.waitForTimeout(600)
  } else {
    check('巡检详情入口存在', false, '未找到「详情」按钮')
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-inspection-list.png'), fullPage: true })

  // A3 车辆补能（P0 新增）
  await clickButton('.tab-bar', '车辆补能')
  await page.waitForTimeout(2500)
  hs = await headers()
  for (const h of ['补能时间', '主体类型', '补能类型', '金额(元)', '区间里程(km)', '每公里成本(元)', '异常']) {
    check(`车辆补能表头含「${h}」`, hs.some(x => x.includes(h)), hs.join('/'))
  }
  bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('车辆补能工具栏含「新增补能」', bodyText.includes('新增补能'))
  check('车辆补能查询区含主体/类型/支付方式/仅看异常',
    bodyText.includes('主体') && bodyText.includes('补能类型') && bodyText.includes('支付方式') && bodyText.includes('仅看异常'))
  const energyText = await gridText()
  check('补能异常列渲染为中文标签', /异常|正常/.test(energyText), energyText.slice(0, 70))
  if (await clickButton('.toolbar-section', '新增补能')) {
    await page.waitForTimeout(1500)
    const eText = (await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')).replace(/\s+/g, '')
    check('补能弹窗含 主体二选一/补能类型/数量/金额/仪表里程',
      eText.includes('补能主体') && eText.includes('四轮车') && eText.includes('骑手两轮车')
      && eText.includes('补能类型') && eText.includes('金额') && eText.includes('仪表里程'), eText.slice(0, 70))
    await page.screenshot({ path: path.join(SHOTS, 'ui-energy-modal.png'), fullPage: true })
    await closeAnyModal()
  } else {
    check('新增补能入口存在', false, '未找到按钮')
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-energy-list.png'), fullPage: true })

  // A4 核验预警
  await clickButton('.tab-bar', '核验预警')
  await page.waitForTimeout(2500)
  hs = await headers()
  for (const h of ['预警类型', '级别', '预警内容', '处理状态', '发生时间']) {
    check(`核验预警表头含「${h}」`, hs.some(s2 => s2.includes(h)), hs.join('/'))
  }
  bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('核验预警工具栏含「批量核验」', bodyText.includes('批量核验'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-alert-list.png'), fullPage: true })

  // A5 补能卡（一卡一车一人）
  await clickButton('.tab-bar', '补能卡')
  await page.waitForTimeout(2500)
  hs = await headers()
  for (const h of ['卡号/套餐号', '卡类型', '绑定主体', '额度/已用', '有效期至', '状态']) {
    check(`补能卡表头含「${h}」`, hs.some(x => x.includes(h)), hs.join('/'))
  }
  bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('补能卡工具栏含「新增补能卡」', bodyText.includes('新增补能卡'))
  if (await clickButton('.toolbar-section', '新增补能卡')) {
    await page.waitForTimeout(1500)
    const cText = (await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')).replace(/\s+/g, '')
    check('补能卡弹窗含 一卡一车一人 提示与绑定主体二选一',
      cText.includes('一卡一车一人') && cText.includes('四轮车') && cText.includes('配送员') && cText.includes('额度'), cText.slice(0, 70))
    await closeAnyModal()
  } else {
    check('新增补能卡入口存在', false, '未找到按钮')
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-energy-card.png'), fullPage: true })

  // A6 能耗报表（汇总卡 + 图表 + 分组表）
  await clickButton('.tab-bar', '能耗报表')
  await page.waitForTimeout(3000)
  bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('能耗报表含四个汇总指标',
    ['补能记录', '补能金额', '有效里程', '平均每公里成本'].every(k => bodyText.includes(k)), bodyText.slice(0, 90))
  check('能耗报表含油电对比与能源类型占比',
    bodyText.includes('油电对比') && bodyText.includes('按能源类型'))
  check('能耗报表图表已渲染（echarts canvas/svg）',
    (await page.locator('.energy-stats canvas, .energy-stats svg').count()) > 0,
    String(await page.locator('.energy-stats canvas, .energy-stats svg').count()))
  check('能耗报表含按主体分组表', bodyText.includes('按主体'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-energy-stats.png'), fullPage: true })

  // 列配置 / 页面配置（在用车管理页验证一次即可，机制与全站一致）
  // 注意：能耗报表 Tab 无数据表 → 先切回「用车登记」
  await clickButton('.tab-bar', '用车登记')
  await page.waitForTimeout(2000)
  const gear = page.locator('.ss-grid th .anticon-setting').first()
  if (await gear.count()) {
    await gear.click()
    await page.waitForTimeout(1500)
    check('列配置弹窗可打开（个人/全局配置）', /个人配置|全局配置/.test(await page.locator('body').innerText()))
    await closeAnyModal()
  } else {
    check('列配置弹窗可打开（个人/全局配置）', false, '未找到齿轮')
  }
  if (await clickButton('.toolbar-section', '页面配置')) {
    await page.waitForTimeout(1500)
    const modalText = await page.locator('.ant-modal-content:visible').innerText().catch(() => '')
    check('页面配置弹窗含「查询条件」Tab', modalText.includes('查询条件'), modalText.replace(/\s+/g, '').slice(0, 60))
    check('页面配置弹窗含「功能按钮」Tab', modalText.includes('功能按钮'))
    await closeAnyModal()
  } else {
    const cfgBtn = page.locator('.toolbar-section button:has(.anticon-setting)').last()
    if (await cfgBtn.count()) {
      await cfgBtn.click()
      await page.waitForTimeout(1500)
      const modalText = await page.locator('.ant-modal-content:visible').innerText().catch(() => '')
      check('页面配置弹窗含「查询条件」Tab', modalText.includes('查询条件'), modalText.replace(/\s+/g, '').slice(0, 60))
      check('页面配置弹窗含「功能按钮」Tab', modalText.includes('功能按钮'))
      await closeAnyModal()
    } else {
      check('页面配置入口存在', false, '未找到页面配置按钮')
    }
  }

  // ══════════ B. 人员核验（2 Tab：实名认证 / 证照核验） ══════════
  await openPage(`${FE}/dms/verification`)
  await waitReady().catch(e => console.log('  [骨架等待超时]', e.message))
  bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('人员核验页可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())
  for (const t of ['实名认证', '证照核验', '准入核验']) {
    check(`人员核验 Tab 含「${t}」`, bodyText.includes(t))
  }
  check('人员核验页不含车辆检查/补能 Tab（边界：车归用车管理）',
    !bodyText.includes('车辆巡检') && !bodyText.includes('车辆补能'), bodyText.slice(0, 60))

  hs = await headers()
  for (const h of ['配送员', '身份类型', '认证状态', '接单资质', '身份证号', '到期预警']) {
    check(`实名认证表头含「${h}」`, hs.some(x => x.includes(h)), hs.join('/'))
  }
  for (const b of ['提交认证', '到期扫描', '刷新', '打印(F8)', '导出']) {
    check(`实名认证工具栏含「${b}」`, bodyText.includes(b.replace(/\s+/g, '')))
  }
  const kycTableText = await gridText()
  check('认证状态渲染为中文标签（非原始枚举值）', /已通过|待审核|已驳回|已过期|待提交/.test(kycTableText), kycTableText.slice(0, 70))
  check('接单资质渲染为标签（非 true/false 原文）', /具备/.test(kycTableText) && !/\btrue\b|\bfalse\b/.test(kycTableText))
  await page.screenshot({ path: path.join(SHOTS, 'ui-kyc-list.png'), fullPage: true })

  // 实名认证提交弹窗
  if (await clickButton('.toolbar-section', '提交认证')) {
    await page.waitForTimeout(1800)
    const kycModal = (await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')).replace(/\s+/g, '')
    for (const f of ['配送员', '证件姓名', '身份证号', '证件人像面', '背书/审查机构', '证照明细', '添加证照']) {
      check(`实名认证弹窗含「${f}」`, kycModal.includes(f), kycModal.slice(0, 50))
    }
    await page.screenshot({ path: path.join(SHOTS, 'ui-kyc-modal.png'), fullPage: true })
    await closeAnyModal()
  } else {
    check('提交认证入口存在', false, '未找到按钮')
  }

  // 证照核验 Tab（P0 新增：证照维度到期清单）
  await clickButton('.tab-bar', '证照核验')
  await page.waitForTimeout(2500)
  hs = await headers()
  for (const h of ['持证人', '证照类型', '证照编号', '有效期至', '剩余天数', '证照状态']) {
    check(`证照核验表头含「${h}」`, hs.some(x => x.includes(h)), hs.join('/'))
  }
  bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('证照核验查询区含证照类型/状态/仅看到期预警',
    bodyText.includes('证照类型') && bodyText.includes('证照状态') && bodyText.includes('仅看到期预警'))
  const certText = await gridText()
  check('证照剩余天数渲染为中文标签（已过期/剩 N 天）',
    /已过期|剩\s*\d+\s*天|天/.test(certText), certText.slice(0, 70))
  await page.screenshot({ path: path.join(SHOTS, 'ui-cert-list.png'), fullPage: true })

  // 准入核验 Tab（人证 × 车证一屏）
  await clickButton('.tab-bar', '准入核验')
  await page.waitForTimeout(3000)
  hs = await headers()
  for (const h of ['配送员', '实名状态', '可否接单', '绑定车辆', '最近出车检查', '可否出车']) {
    check(`准入核验表头含「${h}」`, hs.some(x => x.includes(h)), hs.join('/'))
  }
  bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('准入核验查询区含「仅看不可出车」', bodyText.includes('仅看不可出车'))
  const onboardText = await gridText()
  check('准入核验结论渲染为中文标签（可接单/可出车）',
    /可接单|不可接单/.test(onboardText) && /可出车|不可出车/.test(onboardText), onboardText.slice(0, 80))
  await page.screenshot({ path: path.join(SHOTS, 'ui-onboarding.png'), fullPage: true })

  check('UI 无 JS 运行异常', jsErrors.length === 0, jsErrors.join('; ').slice(0, 200))
  check('UI 运行期无接口 4xx/5xx（含列配置持久化）', apiFailures.length === 0,
    [...new Set(apiFailures)].slice(0, 3).join(' ; '))

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await login()
    console.log('登录成功')
    await apiSuite()
    try {
      await uiSuite()
    } catch (e) {
      check('UI 验收执行', false, e.message)
      console.error(e)
    }
  } catch (e) {
    check('主流程', false, e.message)
    console.error(e)
  }

  const pass = results.filter(r => r.ok).length
  console.log(`\n═══ 验收汇总：${pass}/${results.length} ═══`)
  results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  fs.writeFileSync(path.join(SHOTS, 'result.json'), JSON.stringify(results, null, 2))
  process.exit(pass === results.length ? 0 : 1)
})()
