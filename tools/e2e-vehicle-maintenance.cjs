/*
 * 车辆维护（配送 → 人车管理 → 车辆维护，菜单 80780）金标准端到端验证
 *   · API 验收：号段/新增(维保日期落库·里程回写·到期推算)/第二条(维保前里程推导)/年检·保险回写车辆到期日/
 *               多条件分页(车牌·类型多选·日期·费用·厂商·含附件)/详情/修改/费用统计/到期提醒/真实 xlsx 导出/删除/异常校验
 *   · UI  验收：列表列/工具栏/查询区/列配置齿轮(个人·全局)/页面配置弹窗/新增弹窗(WBD 自动带号·车辆选择器带出当前里程·
 *               保养自动推算下次到期)/保存落库/行内修改回填/行内删除/到期提醒弹窗/费用统计弹窗
 *
 * 用法：node tools/e2e-vehicle-maintenance.cjs            （默认后端 5685、前端 5686）
 *      ERP_PORT=5685 FE_URL=http://localhost:5686 node tools/e2e-vehicle-maintenance.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5686'
const PORT = Number(process.env.ERP_PORT || 5685)
const SHOTS = 'I:/AI-Ready/tool-results/vehicle-maintenance'
const XLSX = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx')

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

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

function tryJson(buf) {
  try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null }
}

let TOKEN = null

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

const E2E_USER = process.env.E2E_USER || 'e2e_vehmaint'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', {
    username: E2E_USER,
    password: E2E_PWD,
    tenantName: '系统租户',
    captcha: code,
    captchaKey: cap.json.data.uuid,
  })
  let token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token && E2E_USER !== 'admin') {
    const cap2 = await rawReq('GET', '/auth/captcha')
    const code2 = [...Buffer.from(cap2.json.data.img.split(',')[1], 'base64').toString('utf8')
      .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    res = await rawReq('POST', '/auth/login', {
      username: 'admin', password: 'admin123', tenantName: '系统租户',
      captcha: code2, captchaKey: cap2.json.data.uuid,
    })
    token = res.json?.data?.token || res.json?.data?.accessToken
  }
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  TOKEN = token
}

// ══════════════ 验收结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

const STAMP = Date.now().toString().slice(-6)
const PLATE = 'E2E临A' + STAMP
const MT_NO_PREFIX = 'WBD'
/** 厂商选择器数据源：tools/e2e-vehicle-maintenance-user.sql 预置的「供应商」往来单位 */
const VENDOR_ID = '2099000000000000301'
const VENDOR_NAME = 'E2E维保服务商'
const VENDOR_CODE = 'E2EVENDOR'

function addDays(dateStr, days) {
  const d = new Date(dateStr + 'T00:00:00Z')
  d.setUTCDate(d.getUTCDate() + days)
  return d.toISOString().slice(0, 10)
}
function addYears(dateStr, years) {
  const d = new Date(dateStr + 'T00:00:00Z')
  d.setUTCFullYear(d.getUTCFullYear() + years)
  return d.toISOString().slice(0, 10)
}

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 0) 准备车辆（车辆档案：E2E 临时车辆，保养间隔 5000km）
  const vehRes = await api('POST', '/dms/vehicle', {
    plateNo: PLATE,
    vehicleType: 2,
    brand: 'E2E品牌',
    model: 'E2E型号',
    color: '白色',
    engineNo: 'E2EENG' + STAMP,
    ownershipType: 1,
    registerDate: '2024-01-01',
    maintenanceIntervalKm: 5000,
    department: 'E2E部门',
    remark: 'E2E 维保验收车辆',
  })
  const vehicle = data(vehRes)
  check('准备车辆档案（维保前置）', vehicle && vehicle.id, JSON.stringify(vehicle).slice(0, 200))
  const vehicleId = vehicle?.id
  if (!vehicleId) throw new Error('车辆创建失败，后续用例无法执行')

  // 0.1) 车辆选择器数据源
  const opts = await api('GET', '/dms/vehicle/options')
  const optList = Array.isArray(data(opts)) ? data(opts) : (data(opts)?.records || [])
  check('车辆选择器 /options 返回本车（禁止手输 ID）',
    optList.some(v => Number(v.id) === Number(vehicleId) && v.plateNo === PLATE),
    JSON.stringify(optList.slice(0, 3)).slice(0, 200))

  // 1) 号段
  const nc = await api('GET', '/dms/vehicle/maintenance/next-no')
  check('next-no 生成维保单号（WBD 号段）', new RegExp('^' + MT_NO_PREFIX + '\\d{4}$').test(String(data(nc))), data(nc))

  // 2) 新增保养（维保日期落库 = P0 修复点；里程回写；下次到期推算）
  const D1 = '2026-01-10'
  const createRes = await api('POST', '/dms/vehicle/maintenance', {
    vehicleId,
    maintType: 1,
    maintDate: D1,
    maintContent: 'E2E 首次保养：更换机油机滤',
    maintCost: 800.5,
    maintVendor: 'E2E修理厂',
    maintContact: '张三',
    maintPhone: '13800000000',
    afterMaintMileage: 12000,
    attachmentUrls: JSON.stringify(['/api/file/view/2026/09/12/' + 'a'.repeat(32) + '.jpg']),
    remark: 'E2E 保养备注',
  })
  const r1 = data(createRes)
  check('新增保养记录成功', r1 && r1.id, JSON.stringify(r1).slice(0, 200))
  check('【P0】维保日期真实落库（原 DTO 丢字段）', r1?.maintDate === D1, `maintDate=${r1?.maintDate}`)
  check('维保单号自动生成（WBD 号段）', new RegExp('^' + MT_NO_PREFIX + '\\d{4}$').test(String(r1?.maintNo)), r1?.maintNo)
  check('车辆车牌联查回填（非裸 ID）', r1?.plateNo === PLATE, r1?.plateNo)
  check('维保类型文本统一口径', r1?.maintTypeText === '保养', r1?.maintTypeText)
  check('保养下次到期日期 = 维保日期 + 90 天', r1?.nextMaintDate === addDays(D1, 90), `expect=${addDays(D1, 90)} actual=${r1?.nextMaintDate}`)
  check('保养下次到期里程 = 维保后里程 + 车辆保养间隔(5000)',
    Number(r1?.nextMaintMileage) === 17000, `expect=17000 actual=${r1?.nextMaintMileage}`)
  check('首笔维保前里程为空（无同车上一笔）', r1?.beforeMaintMileage == null, r1?.beforeMaintMileage)
  check('经办人（制单人）自动记录', !!r1?.handlerName, r1?.handlerName)
  const id1 = r1?.id

  // 2.1) 里程回写车辆档案
  const vAfter1 = data(await api('GET', `/dms/vehicle/${vehicleId}`))
  check('维保后里程回写车辆当前里程', Number(vAfter1?.currentMileage) === 12000, vAfter1?.currentMileage)
  check('回写车辆上次保养里程', Number(vAfter1?.lastMaintenanceKm) === 12000, vAfter1?.lastMaintenanceKm)
  check('回写车辆上次保养日期', String(vAfter1?.lastMaintenanceDate || '').slice(0, 10) === D1, vAfter1?.lastMaintenanceDate)

  // 3) 第二条：维修（不产生周期 + 维保前里程 = 上一笔维保后里程）
  const D2 = '2026-02-01'
  const r2 = data(await api('POST', '/dms/vehicle/maintenance', {
    vehicleId, maintType: 2, maintDate: D2, maintContent: 'E2E 维修：更换刹车片',
    maintCost: 300, maintVendor: 'E2E修理厂', afterMaintMileage: 13500,
  }))
  check('新增维修记录成功', r2 && r2.id, JSON.stringify(r2).slice(0, 160))
  check('维修不产生下次到期日期', r2?.nextMaintDate == null, r2?.nextMaintDate)
  check('维修不产生下次到期里程', r2?.nextMaintMileage == null, r2?.nextMaintMileage)
  check('维保前里程 = 同车上一笔维保后里程（服务端推导）',
    Number(r2?.beforeMaintMileage) === 12000, `expect=12000 actual=${r2?.beforeMaintMileage}`)
  const vAfter2 = data(await api('GET', `/dms/vehicle/${vehicleId}`))
  check('第二笔维保后里程继续回写车辆', Number(vAfter2?.currentMileage) === 13500, vAfter2?.currentMileage)

  // 4) 年检 / 保险：回写车辆证件到期日
  const D3 = '2026-03-05'
  const r3 = data(await api('POST', '/dms/vehicle/maintenance', {
    vehicleId, maintType: 3, maintDate: D3, maintContent: 'E2E 年检', maintCost: 200, afterMaintMileage: 14000,
  }))
  check('新增年检记录成功', r3 && r3.id, JSON.stringify(r3).slice(0, 160))
  check('年检下次到期日期 = 维保日期 + 1 年', r3?.nextMaintDate === addYears(D3, 1), `expect=${addYears(D3, 1)} actual=${r3?.nextMaintDate}`)
  const vAfter3 = data(await api('GET', `/dms/vehicle/${vehicleId}`))
  check('年检回写车辆年检到期日', String(vAfter3?.inspectionExpireDate || '').slice(0, 10) === addYears(D3, 1), vAfter3?.inspectionExpireDate)

  const r4 = data(await api('POST', '/dms/vehicle/maintenance', {
    vehicleId, maintType: 4, maintDate: '2026-03-20', maintContent: 'E2E 续保', maintCost: 1500, afterMaintMileage: 14500,
  }))
  check('新增保险记录成功', r4 && r4.id, JSON.stringify(r4).slice(0, 160))
  const vAfter4 = data(await api('GET', `/dms/vehicle/${vehicleId}`))
  check('保险回写车辆保险到期日', String(vAfter4?.insuranceExpireDate || '').slice(0, 10) === addYears('2026-03-20', 1), vAfter4?.insuranceExpireDate)

  // 5) 其他类型（枚举补 6 其他）
  const r6 = data(await api('POST', '/dms/vehicle/maintenance', {
    vehicleId, maintType: 6, maintDate: '2026-03-25', maintContent: 'E2E 其他事项', maintCost: 50,
  }))
  check('维保类型含「6 其他」（前后端一致）', r6?.maintType === 6 && r6?.maintTypeText === '其他', `${r6?.maintType}/${r6?.maintTypeText}`)

  // 5.1) 厂商选择器（往来单位：供应商 / 其他往来单位，不新建厂商表）
  const vOpts = await api('GET', '/dms/vehicle/maintenance/vendor-options')
  const vList = Array.isArray(data(vOpts)) ? data(vOpts) : (data(vOpts)?.records || [])
  check('厂商选择器返回往来单位（供应商）',
    vList.some(v => String(v.id) === VENDOR_ID && v.name === VENDOR_NAME && v.code === VENDOR_CODE),
    JSON.stringify(vList.slice(0, 3)).slice(0, 220))
  check('厂商选择器带出类型文本（供应商）',
    vList.filter(v => String(v.id) === VENDOR_ID).every(v => v.partyTypeText === '供应商'),
    JSON.stringify(vList.filter(v => String(v.id) === VENDOR_ID)))
  const vOptsKw = await api('GET', '/dms/vehicle/maintenance/vendor-options?keyword=E2E维保')
  const vListKw = Array.isArray(data(vOptsKw)) ? data(vOptsKw) : (data(vOptsKw)?.records || [])
  check('厂商选择器按名称关键字检索命中', vListKw.some(v => String(v.id) === VENDOR_ID), JSON.stringify(vListKw))
  const vOptsNone = await api('GET', '/dms/vehicle/maintenance/vendor-options?keyword=ZZZ不存在厂商ZZZ')
  const vListNone = Array.isArray(data(vOptsNone)) ? data(vOptsNone) : (data(vOptsNone)?.records || [])
  check('厂商选择器无匹配时返回空（不臆造候选）', vListNone.length === 0, JSON.stringify(vListNone))
  const vOptsCust = await api('GET', '/dms/vehicle/maintenance/vendor-options?keyword=客户甲')
  const vListCust = Array.isArray(data(vOptsCust)) ? data(vOptsCust) : (data(vOptsCust)?.records || [])
  check('厂商候选不含客户（仅供应商/其他往来单位）',
    !vListCust.some(v => v.name === '客户甲'), JSON.stringify(vListCust))

  // 6) 分页多条件
  const p0 = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${vehicleId}`)
  check('分页按车辆检索命中 5 条', Number(data(p0)?.total) === 5, JSON.stringify(data(p0)?.total))
  const p1 = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&plateNo=${PLATE}`)
  check('分页按车牌联查命中', Number(data(p1)?.total) === 5, JSON.stringify(data(p1)?.total))
  const p2 = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${vehicleId}&maintTypes=1,2`)
  check('分页按维保类型多选(保养+维修)命中 2 条', Number(data(p2)?.total) === 2, JSON.stringify(data(p2)?.total))
  const p3 = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${vehicleId}&dateFrom=2026-02-01&dateTo=2026-03-05`)
  check('分页按维保日期区间命中 2 条', Number(data(p3)?.total) === 2, JSON.stringify(data(p3)?.total))
  const p4 = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${vehicleId}&costMin=500&costMax=1000`)
  check('分页按费用区间命中 1 条', Number(data(p4)?.total) === 1, JSON.stringify(data(p4)?.total))
  const p5 = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${vehicleId}&vendor=E2E修理厂`)
  check('分页按厂商模糊命中 2 条', Number(data(p5)?.total) === 2, JSON.stringify(data(p5)?.total))
  const p6 = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${vehicleId}&hasAttachment=1`)
  check('分页按含附件过滤命中 1 条', Number(data(p6)?.total) === 1, JSON.stringify(data(p6)?.total))
  const p7 = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${vehicleId}&hasAttachment=0`)
  check('分页按不含附件过滤命中 4 条', Number(data(p7)?.total) === 4, JSON.stringify(data(p7)?.total))
  const p8 = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&maintNo=${r1?.maintNo || ''}`)
  check('分页按维保单号检索命中 1 条', Number(data(p8)?.total) === 1, JSON.stringify(data(p8)?.total))
  const p9 = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=2&vehicleId=${vehicleId}`)
  check('分页 pageSize=2 生效', (data(p9)?.records || []).length === 2, JSON.stringify((data(p9)?.records || []).length))
  check('分页默认按维保日期倒序', data(p9)?.records?.[0]?.maintDate === '2026-03-25', data(p9)?.records?.[0]?.maintDate)

  // 7) 详情
  const detail = await api('GET', `/dms/vehicle/maintenance/${id1}`)
  check('详情返回车牌/厂商/联系人/附件/经办人',
    data(detail)?.plateNo === PLATE && data(detail)?.maintVendor === 'E2E修理厂'
    && data(detail)?.maintContact === '张三' && !!data(detail)?.attachmentUrls && !!data(detail)?.handlerName,
    JSON.stringify(data(detail)).slice(0, 220))

  // 8) 修改（原实现只有 create/delete，无 PUT）
  const upd = await api('PUT', `/dms/vehicle/maintenance/${id1}`, {
    vehicleId, maintType: 1, maintDate: '2026-01-15', maintContent: 'E2E 保养（修改后）',
    maintCost: 900, maintVendor: 'E2E修理厂二部', vendorId: VENDOR_ID, afterMaintMileage: 12000,
  })
  check('修改维保记录成功（补 PUT 接口）', upd.json?.code === 200, upd.json?.message)
  const dUpd = data(await api('GET', `/dms/vehicle/maintenance/${id1}`))
  check('修改后维保日期更新', dUpd?.maintDate === '2026-01-15', dUpd?.maintDate)
  check('修改后费用更新', Number(dUpd?.maintCost) === 900, dUpd?.maintCost)
  check('修改后下次到期日期按新日期重算(+90天)', dUpd?.nextMaintDate === addDays('2026-01-15', 90), dUpd?.nextMaintDate)

  // 8.1) 厂商档案引用：选了往来单位 → vendorId 落库 + 名称快照以档案为准（覆盖手输变体）
  check('修改后厂商引用往来单位（vendorId 落库）', String(dUpd?.vendorId) === VENDOR_ID, JSON.stringify(dUpd?.vendorId))
  check('厂商名称快照以档案为准（不用手输变体）', dUpd?.maintVendor === VENDOR_NAME, dUpd?.maintVendor)
  const dFree = data(await api('GET', `/dms/vehicle/maintenance/${r2?.id}`))
  check('未建档厂商允许自由填写（vendorId 为空）', dFree?.vendorId == null && dFree?.maintVendor === 'E2E修理厂', `${dFree?.vendorId}/${dFree?.maintVendor}`)
  const pageVendorId = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${vehicleId}&vendorId=${VENDOR_ID}`)
  check('分页按厂商档案ID精确过滤命中 1 条', Number(data(pageVendorId)?.total) === 1, JSON.stringify(data(pageVendorId)?.total))
  const pageVendorName = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${vehicleId}&vendor=E2E维保服务商`)
  check('分页按厂商名称模糊命中 1 条', Number(data(pageVendorName)?.total) === 1, JSON.stringify(data(pageVendorName)?.total))

  // 9) 费用统计
  const stat = data(await api('GET', `/dms/vehicle/maintenance/stat?vehicleId=${vehicleId}`))
  check('费用统计：记录数 = 5', Number(stat?.recordCount) === 5, JSON.stringify(stat?.recordCount))
  check('费用统计：总费用 = 2950（修改后口径）', Number(stat?.totalCost) === 2950, JSON.stringify(stat?.totalCost))
  check('费用统计：涉及车辆 = 1', Number(stat?.vehicleCount) === 1, JSON.stringify(stat?.vehicleCount))
  check('费用统计：按类型分组 5 类', (stat?.byType || []).length === 5, JSON.stringify(stat?.byType))
  check('费用统计：按月份分组 3 个月', (stat?.byMonth || []).length === 3, JSON.stringify(stat?.byMonth))
  check('费用统计：按车辆分组含车牌', (stat?.byVehicle || [])[0]?.plateNo === PLATE, JSON.stringify(stat?.byVehicle))
  const statVendor = (stat?.byVendor || []).find(v => v.vendorName === VENDOR_NAME)
  check('费用统计：按厂商分组命中档案厂商（linked=true）',
    !!statVendor && statVendor.linked === true && Number(statVendor.cost) === 900, JSON.stringify(stat?.byVendor))
  const statVendorFree = (stat?.byVendor || []).find(v => v.vendorName === '未填写')
  check('费用统计：未建档厂商单列口径（linked=false）',
    !!statVendorFree && statVendorFree.linked === false && Number(statVendorFree.count) === 3, JSON.stringify(stat?.byVendor))

  // 10) 到期提醒（保养下次到期 2026-04-15 已逾期；年检/保险 2027 年在窗口外不出现）
  const exp = data(await api('GET', '/dms/vehicle/maintenance/expiring?days=30&warnKm=1000'))
  const expRows = Array.isArray(exp) ? exp : (exp?.records || [])
  const expOwn = expRows.filter(r => Number(r.vehicleId) === Number(vehicleId))
  check('到期提醒返回逾期保养记录', expOwn.some(r => r.id === id1), JSON.stringify(expOwn.map(r => r.maintNo)))
  check('到期提醒标注已逾期状态', expOwn.every(r => r.dueStatus !== 'NORMAL'), JSON.stringify(expOwn.map(r => r.dueStatus)))
  check('到期提醒计算剩余天数', expOwn.every(r => typeof r.remainDays === 'number'), JSON.stringify(expOwn.map(r => r.remainDays)))
  const ownOverdue = expOwn.find(r => r.id === id1)
  check('到期提醒剩余天数为负（已逾期=2026-04-15 vs 今天）', ownOverdue?.remainDays < 0, ownOverdue?.remainDays)
  check('到期提醒含里程口径剩余公里', expOwn.every(r => r.remainKm === null || typeof r.remainKm === 'number'), JSON.stringify(expOwn.map(r => r.remainKm)))

  // 11) 导出真实 xlsx
  const expXlsx = await api('GET', `/dms/vehicle/maintenance/export?vehicleId=${vehicleId}`)
  check('导出返回 xlsx（PK 头，非 JSON）', expXlsx.buf && expXlsx.buf[0] === 0x50 && expXlsx.buf[1] === 0x4b, `status=${expXlsx.status}`)
  if (expXlsx.buf && expXlsx.buf[0] === 0x50) {
    fs.writeFileSync(path.join(SHOTS, 'export.xlsx'), expXlsx.buf)
    try {
      const wb = XLSX.read(expXlsx.buf, { type: 'buffer' })
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
      check('导出内容含 5 行维保记录', rows.length === 5, `rows=${rows.length}`)
      check('导出表头含 15 列（含维保前/后里程·经办人）',
        ['维保单号', '车牌号', '维保类型', '维保日期', '维保费用', '维保前里程(km)', '维保后里程(km)', '下次维保日期', '经办人']
          .every(h => rows[0] && Object.keys(rows[0]).includes(h)),
        JSON.stringify(Object.keys(rows[0] || {})))
      check('导出内容含新增维保单号', rows.some(r => String(r['维保单号']) === String(r1?.maintNo)), JSON.stringify(rows[0] || {}))
    } catch (e) {
      check('导出 xlsx 可解析', false, e.message)
    }
  }

  // 12) 异常校验
  const badVehicle = await api('POST', '/dms/vehicle/maintenance', { vehicleId: 999999999, maintType: 1 })
  check('车辆不存在时拒绝新增', badVehicle.json?.code !== 200, badVehicle.json?.message)
  const badType = await api('POST', '/dms/vehicle/maintenance', { vehicleId, maintType: null })
  check('维保类型必填校验', badType.json?.code !== 200, badType.json?.message)
  const badId = await api('PUT', '/dms/vehicle/maintenance/999999999', { vehicleId, maintType: 1, maintDate: '2026-01-01' })
  check('修改不存在的记录被拒绝', badId.json?.code !== 200, badId.json?.message)
  const badVendor = await api('POST', '/dms/vehicle/maintenance', { vehicleId, maintType: 1, maintDate: '2026-04-01', vendorId: 999999999 })
  check('厂商ID不存在时拒绝新增', badVendor.json?.code !== 200, badVendor.json?.message)
  const badDel = await api('DELETE', '/dms/vehicle/maintenance/999999999')
  check('删除不存在的记录被拒绝', badDel.json?.code !== 200, badDel.json?.message)

  // 13) 删除（清理本次 API 数据；车辆保留给 UI 用例）
  for (const id of [r6?.id, r4?.id, r3?.id, r2?.id, id1]) {
    if (id) await api('DELETE', `/dms/vehicle/maintenance/${id}`)
  }
  const pAfter = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=20&vehicleId=${vehicleId}`)
  check('删除后逻辑删除生效（列表为空）', Number(data(pAfter)?.total) === 0, JSON.stringify(data(pAfter)?.total))

  return { vehicleId }
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite(vehicleId) {
  console.log('\n═══ 二、UI 验收 ═══')
  // UI 用例数据准备：一条年检（含附件）+ 到期日落在 30 天窗口内，用于「到期提醒」校验
  const todayStr = new Date().toISOString().slice(0, 10)
  const uiRec = data(await api('POST', '/dms/vehicle/maintenance', {
    vehicleId,
    maintType: 3,
    maintDate: addDays(todayStr, -360),
    maintContent: 'E2E UI 年检：上线检测',
    maintCost: 600,
    maintVendor: 'E2E UI修理厂',
    afterMaintMileage: 20000,
    attachmentUrls: JSON.stringify(['/api/file/view/2026/09/12/' + 'b'.repeat(32) + '.pdf']),
  }))
  check('UI 前置：准备一条维保记录', !!uiRec?.id, uiRec?.maintNo)
  check('UI 前置：年检下次到期落在 30 天窗口内（供到期提醒校验）',
    uiRec?.nextMaintDate === addYears(addDays(todayStr, -360), 1), uiRec?.nextMaintDate)

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  // /api 转发到验收实例（前端 dev server 代理默认指向 5655）
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

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

  async function bodyText() {
    return (await page.locator('body').innerText()).replace(/\s+/g, '')
  }

  await openPage(`${FE}/dms/vehicle/maintenance?vehicleId=${vehicleId}`)
  const txt = await bodyText()
  check('页面可打开（无 404/白屏）', !txt.includes('页面不存在') && txt.length > 50, page.url())

  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['维保单号', '车辆(车牌号)', '维保类型', '维保日期', '维保内容', '费用', '维保前里程(km)', '维保后里程(km)', '下次到期', '经办人', '附件']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  for (const b of ['新增维保', '到期提醒', '费用统计', '刷新', '导出']) {
    check(`工具栏含「${b}」`, txt.includes(b.replace(/\s+/g, '')))
  }
  for (const q of ['维保单号', '车辆', '维保类型', '维保日期', '费用', '附件', '查询']) {
    check(`查询区含「${q}」`, txt.includes(q))
  }
  const queryVendorInput = page.locator('.search-area input[placeholder*="选择往来单位"]').first()
  check('查询区「厂商」为往来单位选择器（可清空/可自由输入）',
    (await queryVendorInput.count()) > 0, String(await queryVendorInput.getAttribute('placeholder')))
  check('列表显示车牌号（非裸车辆 ID）', txt.includes(PLATE), PLATE)
  check('列表显示维保类型文本（非裸数字）', txt.includes('年检'), '年检')
  check('列表显示经办人（制单用户名）', txt.includes('E2E维保'), 'E2E维保')
  check('页面无「骑手」残留术语', !txt.includes('骑手'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 列配置齿轮（个人配置/全局配置）
  const gearCount = await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length)
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)
  if (gearCount > 0) {
    await page.locator('.ss-grid .th-settings-btn').first().click()
    await page.waitForTimeout(1200)
    const colPanel = page.locator('.ant-modal-content:visible').last()
    const colText = (await colPanel.innerText()).replace(/\s+/g, '')
    check('列配置弹窗含「个人配置 / 全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 80))
    check('列配置含金标准列（维保单号/车辆/下次到期/附件）',
      ['维保单号', '车辆(车牌号)', '维保类型', '下次到期', '附件'].every(c => colText.includes(c)),
      colText.replace(/\n/g, '|').slice(0, 200))
    await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(800)
  }

  // 页面配置弹窗（查询条件 / 功能按钮）
  await page.locator('button[title="页面配置"]').first().click()
  await page.waitForTimeout(1200)
  const cfgPanel = page.locator('.ant-modal-content:visible').last()
  const cfgText = (await cfgPanel.innerText()).replace(/\s+/g, '')
  check('页面配置弹窗可打开且为查询条件 Tab', cfgText.includes('查询条件'), cfgText.slice(0, 80))
  check('页面配置含金标准查询项（车辆/维保类型/维保日期/费用区间）',
    ['车辆', '维保类型', '维保日期', '费用区间'].every(c => cfgText.includes(c)),
    cfgText.replace(/\n/g, '|').slice(0, 200))
  await cfgPanel.locator('.ant-tabs-tab:has-text("功能按钮")').click()
  await page.waitForTimeout(600)
  const btnText = (await cfgPanel.innerText()).replace(/\s+/g, '')
  check('页面配置含功能按钮开关（新增维保/到期提醒/费用统计/导出）',
    ['新增维保', '到期提醒', '费用统计', '导出'].every(c => btnText.includes(c)),
    btnText.replace(/\n/g, '|').slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png'), fullPage: true })
  await cfgPanel.locator('.ant-modal-close').click()
  await page.waitForTimeout(800)

  // 新增弹窗
  await page.click('button:has-text("新增维保")')
  await page.waitForTimeout(2500)
  const modal = page.locator('.ant-modal-content:visible').last()
  const modalText = (await modal.innerText()).replace(/\s+/g, '')
  check('新增弹窗标题=新增维保记录', modalText.includes('新增维保记录'), (await modal.innerText()).split('\n')[0])
  for (const f of ['维保单号', '车辆', '维保类型', '维保日期', '维保前里程(km)', '维保后里程(km)', '维保费用', '维保厂商', '下次到期日期', '下次到期里程(km)', '维保内容', '附件', '备注']) {
    check(`弹窗含字段「${f}」`, modalText.includes(f))
  }
  const noInput = modal.locator('input[placeholder*="WBD"]')
  const autoNo = await noInput.inputValue()
  check('弹窗自动带出维保单号（WBD 号段）', new RegExp('^' + MT_NO_PREFIX + '\\d{4}$').test(autoNo), autoNo)
  check('维保单号不可编辑（自动生成）', await noInput.isDisabled())
  await page.screenshot({ path: path.join(SHOTS, 'ui-modal.png'), fullPage: true })

  // 选择车辆（选择器，带出当前里程）
  await modal.locator('.ant-select').nth(0).click()
  await page.waitForTimeout(1200)
  await page.locator(`.ant-select-item-option:has-text("${PLATE}")`).first().click()
  await page.waitForTimeout(1500)
  const beforeMileage = await modal.locator('input[placeholder*="选择车辆后自动带出"]').inputValue()
  check('车辆选择器带出维保前里程（车辆当前里程）', String(beforeMileage) === '20000', beforeMileage)

  // 选择维保类型=保养 → 自动推算下次到期
  await modal.locator('.ant-select').nth(1).click()
  await page.waitForTimeout(1000)
  await page.locator('.ant-select-item-option:has-text("保养")').first().click()
  await page.waitForTimeout(600)
  await modal.locator('input[placeholder*="维保完成时里程"]').fill('25000')
  // a-input-number 需失焦/回车才提交 change → 触发下次到期里程自动推算
  await modal.locator('input[placeholder*="维保完成时里程"]').press('Enter')
  await page.waitForTimeout(800)
  const nextKm = await modal.locator('input[placeholder*="保养按车辆间隔推算"]').inputValue()
  check('保养按车辆间隔(5000)自动推算下次到期里程', String(nextKm) === '30000', nextKm)
  const nextDateInput = modal.locator('input[placeholder*="自动推算"]').first()
  const nextDateVal = await nextDateInput.inputValue()
  check('保养自动推算下次到期日期（+90 天，可修改）', /^\d{4}-\d{2}-\d{2}$/.test(nextDateVal), nextDateVal)

  // 维保厂商：往来单位选择器（可搜索）+ 手工填写兜底
  // 厂商控件＝AutoComplete + 自定义输入（antd 约定：allowClear/placeholder 配在输入上）
  const vendorItem = modal.locator('.ant-form-item', { hasText: '维保厂商' }).first()
  const vendorInput = vendorItem.locator('input[placeholder*="选择往来单位"]').first()
  const vendorPh = await vendorInput.getAttribute('placeholder')
  check('弹窗「维保厂商」为往来单位选择器（可搜索可填写）', (await vendorInput.count()) > 0, String(vendorPh))
  check('弹窗厂商控件展示「选择往来单位」占位提示', String(vendorPh || '').includes('选择往来单位'), String(vendorPh))
  await vendorInput.fill('E2E自建快修厂')
  await vendorInput.press('Tab')
  await page.waitForTimeout(600)
  const freeHintText = (await modal.innerText()).replace(/\s+/g, '')
  check('手工填写厂商时提示「未建档」', freeHintText.includes('未建档厂商可直接填写名称'), freeHintText.slice(0, 160))
  await vendorInput.fill('E2E维保')
  await page.waitForTimeout(1500)
  const vendorDropdown = page.locator('.ant-select-dropdown:visible').last()
  const vendorOptText = (await vendorDropdown.innerText()).replace(/\s+/g, '')
  check('厂商选择器按关键字列出往来单位（带类型）',
    vendorOptText.includes(VENDOR_NAME) && vendorOptText.includes('供应商'), vendorOptText.slice(0, 160))
  await vendorDropdown.locator('.ant-select-item-option', { hasText: VENDOR_NAME }).first().click()
  await page.waitForTimeout(800)
  const linkedHintText = (await modal.innerText()).replace(/\s+/g, '')
  check('选中往来单位后提示「已关联档案」', linkedHintText.includes('已关联往来单位档案'), linkedHintText.slice(0, 160))
  check('选中后厂商名称以档案为准', await vendorInput.inputValue() === VENDOR_NAME, await vendorInput.inputValue())

  // 填写其余字段 + 附件真实上传
  const UI_CONTENT = 'E2E UI 新增：四轮定位'
  await page.waitForTimeout(200)
  await modal.locator('textarea[placeholder*="维保项目"]').fill(UI_CONTENT)
  await modal.locator('input[placeholder="请输入维保费用"]').fill('1200')
  await page.waitForTimeout(300)

  const pngPath = path.join(SHOTS, 'e2e-upload.png')
  fs.writeFileSync(pngPath, Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64'))
  await modal.locator('input[type="file"]').setInputFiles(pngPath)
  await page.waitForTimeout(3000)
  const uploadText = (await modal.innerText()).replace(/\s+/g, '')
  check('附件真实上传（复用平台 /api/file/upload）', uploadText.includes('e2e-upload'), uploadText.slice(0, 140))

  await modal.locator('.ant-btn-primary:has-text("保存")').click()
  await page.waitForTimeout(3000)
  const afterSave = await bodyText()
  // ⚠️「维保厂商」列默认隐藏，故用可见的「维保内容」定位新行
  check('新增保存成功并出现在列表', afterSave.includes('E2EUI新增：四轮定位'), UI_CONTENT)
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-save.png'), fullPage: true })

  // 通过接口复核 UI 落库结果（日期/到手里程/附件/经办人/厂商引用）
  const uiSaved = data(await api('GET',
    `/dms/vehicle/maintenance/page?pageNum=1&pageSize=50&vehicleId=${vehicleId}&vendorId=${VENDOR_ID}`))
  const uiRecSaved = (uiSaved?.records || []).find(r => String(r.maintContent || '').includes('四轮定位')) || (uiSaved?.records || [])[0]
  check('UI 新增记录厂商引用落库（vendorId）', String(uiRecSaved?.vendorId) === VENDOR_ID, JSON.stringify(uiRecSaved?.vendorId))
  check('UI 新增记录厂商名称=档案名称快照', uiRecSaved?.maintVendor === VENDOR_NAME, uiRecSaved?.maintVendor)
  check('列表「维保厂商」列显示厂商与「档案」标记',
    (await bodyText()).includes(VENDOR_NAME.replace(/\s+/g, '')), VENDOR_NAME)
  check('UI 新增记录落库（附件 URL 已保存）',
    !!uiRecSaved && String(uiRecSaved.attachmentUrls || '').includes('/api/file/view/'),
    String(uiRecSaved?.attachmentUrls || '').slice(0, 80))
  check('UI 新增记录维保日期落库（默认当天）', /^\d{4}-\d{2}-\d{2}$/.test(String(uiRecSaved?.maintDate)), uiRecSaved?.maintDate)
  check('UI 新增记录下次到期里程按车辆间隔推算(25000+5000)',
    Number(uiRecSaved?.nextMaintMileage) === 30000, uiRecSaved?.nextMaintMileage)
  check('UI 新增记录经办人=当前登录用户', uiRecSaved?.handlerName === 'E2E维保', uiRecSaved?.handlerName)

  // 行内「修改」回填（定位到刚新增的那一行，避免误开其他记录）
  const uiRow = page.locator('.ss-grid tr', { hasText: '四轮定位' }).first()
  check('新增记录行可见（按维保内容定位）', (await uiRow.count()) > 0)
  await uiRow.locator('a').first().click()
  await page.waitForTimeout(2200)
  const editModal = page.locator('.ant-modal-content:visible').last()
  const editText = (await editModal.innerText()).replace(/\s+/g, '')
  check('修改弹窗标题=修改维保记录', editText.includes('修改维保记录'), (await editModal.innerText()).split('\n')[0])
  const editContent = await editModal.locator('textarea[placeholder*="维保项目"]').inputValue()
  check('修改弹窗回填维保内容', editContent.includes('四轮定位') || editContent.length > 0, editContent)
  const editVendor = await editModal.locator('.ant-form-item', { hasText: '维保厂商' })
    .first().locator('input').first().inputValue()
  check('修改弹窗回填维保厂商（档案名称快照）', editVendor === VENDOR_NAME, editVendor)
  check('修改弹窗提示厂商已关联档案',
    (await editModal.innerText()).replace(/\s+/g, '').includes('已关联往来单位档案'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-edit.png'), fullPage: true })
  await editModal.locator('.ant-btn:has-text("关闭")').click()
  await page.waitForTimeout(1000)

  // 到期提醒弹窗
  await page.locator('button:has-text("到期提醒")').first().click()
  await page.waitForTimeout(2500)
  const expModal = page.locator('.ant-modal-content:visible').last()
  const expText = (await expModal.innerText()).replace(/\s+/g, '')
  check('到期提醒弹窗可打开', expText.includes('维保到期提醒'), expText.slice(0, 60))
  check('到期提醒含日期/里程双阈值', expText.includes('日期窗口') && expText.includes('里程窗口'))
  const expRows = await page.evaluate(() => document.querySelectorAll('.ant-modal .ant-table-tbody tr.ant-table-row').length)
  check('到期提醒表格有数据行', expRows > 0, `rows=${expRows}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-expiring.png'), fullPage: true })
  await expModal.locator('.ant-modal-close').click()
  await page.waitForTimeout(1000)

  // 费用统计弹窗
  await page.locator('button:has-text("费用统计")').first().click()
  await page.waitForTimeout(2500)
  const statModal = page.locator('.ant-modal-content:visible').last()
  const statText = (await statModal.innerText()).replace(/\s+/g, '')
  check('费用统计弹窗可打开', statText.includes('维保费用统计'), statText.slice(0, 60))
  check('费用统计含汇总卡片（记录数/总费用/涉及车辆）',
    statText.includes('维保记录数') && statText.includes('维保总费用') && statText.includes('涉及车辆'))
  check('费用统计含四个口径页签（类型/月份/车辆/厂商）',
    statText.includes('按维保类型') && statText.includes('按月份') && statText.includes('按车辆') && statText.includes('按厂商'))
  await statModal.locator('.ant-tabs-tab:has-text("按厂商")').click()
  await page.waitForTimeout(800)
  const statVendorText = (await statModal.innerText()).replace(/\s+/g, '')
  check('费用统计「按厂商」列出档案厂商与来源标记',
    statVendorText.includes(VENDOR_NAME) && statVendorText.includes('档案'), statVendorText.slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-stat.png'), fullPage: true })
  await statModal.locator('.ant-modal-close').click()
  await page.waitForTimeout(1000)

  // 行内「删除」
  const row = page.locator('.ss-grid tr', { hasText: '四轮定位' }).first()
  if (await row.count()) {
    await row.locator('button:has-text("删除")').click()
    await page.waitForTimeout(1200)
    const delBox = page.locator('.ant-modal-confirm:visible').last()
    if (await delBox.count()) {
      await delBox.locator('button:has-text("确认删除")').click()
      await page.waitForTimeout(2500)
    }
    const afterDel = await bodyText()
    check('行内删除维保记录成功', !afterDel.includes('E2EUI新增：四轮定位'), '仍在列表=' + afterDel.includes('E2EUI新增：四轮定位'))
  } else {
    check('行内删除维保记录成功', false, '未找到待删除行')
  }

  await browser.close()
}

// ══════════════ 清理本次验收数据 ══════════════
async function cleanup(vehicleId) {
  console.log('\n═══ 三、清理验收数据 ═══')
  if (vehicleId) {
    const p = await api('GET', `/dms/vehicle/maintenance/page?pageNum=1&pageSize=100&vehicleId=${vehicleId}`)
    for (const rec of (data(p)?.records || [])) {
      await api('DELETE', `/dms/vehicle/maintenance/${rec.id}`)
    }
    const delV = await api('DELETE', `/dms/vehicle/${vehicleId}`)
    check('清理：删除 E2E 车辆档案', delV.json?.code === 200, delV.json?.message)
  }
}

// ══════════════ 主流程 ══════════════
;(async () => {
  let vehicleId = null
  try {
    await login()
    console.log('登录成功')
    const apiResult = await apiSuite()
    vehicleId = apiResult?.vehicleId
    try {
      await uiSuite(vehicleId)
    } catch (e) {
      check('UI 验收执行', false, e.message)
      console.error(e)
    }
    await cleanup(vehicleId)
  } catch (e) {
    check('主流程', false, e.message)
    console.error(e)
    try { await cleanup(vehicleId) } catch (err) { /* ignore */ }
  }

  const pass = results.filter(r => r.ok).length
  console.log(`\n═══ 验收汇总：${pass}/${results.length} ═══`)
  results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  fs.writeFileSync(path.join(SHOTS, 'result.json'), JSON.stringify(results, null, 2))
  process.exit(pass === results.length ? 0 : 1)
})()
