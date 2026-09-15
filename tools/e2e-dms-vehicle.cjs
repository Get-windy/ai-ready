/*
 * 车辆管理（配送 → 人车管理 → 车辆管理，菜单 80770 `dms:vehicle`）金标准端到端验证
 *   · API 验收：号段/新增/唯一校验/多条件分页/详情/修改/状态机/人车一对一绑定解绑/绑定流水/
 *               证件到期清单/options 选择器/批量启停/批量删除/导出真实 xlsx
 *   · UI 验收：列表骨架（表头/工具栏/查询区/页面配置/列配置）+ 新增表单页真实保存 +
 *               状态变更 + 绑定/解绑 + 到期提醒 + 批量启停
 *
 * 前置：
 *   1) python tools/dbq.py 执行 tools/e2e-dms-vehicle-user.sql（验收账号 e2e_vehicle + 3 名配送员）
 *   2) 验收后端实例：java -jar <fat jar> --server.port=5665
 *
 * 用法：node tools/e2e-dms-vehicle.cjs
 *      ERP_PORT=5665 FE_URL=http://localhost:5656 node tools/e2e-dms-vehicle.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5665)
const SHOTS = 'I:/AI-Ready/tool-results/dms-vehicle'
const XLSX = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx')

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

// ══════════════ HTTP 基础 ══════════════
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

/** 取接口 data（后端 ApiResponse 包一层） */
function data(res) {
  return res.json ? res.json.data : null
}

const E2E_USER = process.env.E2E_USER || 'e2e_vehicle'
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
const PLATE_A = '京A' + STAMP // 主车：满字段 + 证件到期 + 绑定解绑 + 状态机
const PLATE_B = '京B' + STAMP
const PLATE_C = '京C' + STAMP
const PLATE_D = '京D' + STAMP
const PLATE_E = '京E' + STAMP
const PLATE_F = '京F' + STAMP

// ⚠️ 雪花ID 超出 JS 安全整数（2^53），必须以字符串传递，否则末位被四舍五入
const RIDER_A = '2099000000000001110'
const RIDER_B = '2099000000000001111'
const RIDER_C = '2099000000000001112'

/** 本机自然日 +N（不能用 toISOString：UTC 与本地时差会把「10 天后」算成 9 天） */
function plusDays(n) {
  const d = new Date()
  d.setDate(d.getDate() + n)
  const p = (v) => String(v).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function baseVehicle(plateNo, extra) {
  return {
    plateNo,
    vehicleType: 3,
    brand: 'E2E五菱',
    model: '荣光V',
    color: '白色',
    engineNo: 'ENG' + STAMP,
    ownershipType: 1,
    ratedLoad: 1500,
    ratedPassenger: 2,
    cargoVolume: 4.5,
    registerDate: '2024-01-15',
    maintenanceIntervalKm: 5000,
    department: 'E2E配送部',
    remark: 'E2E 车辆',
    ...extra,
  }
}

/** 清理本脚本产生的测试车辆（车牌规则：京 + 字母 + 6 位时间戳；含上一轮遗留） */
async function cleanup() {
  const p = await api('GET', '/dms/vehicle/page?pageNum=1&pageSize=200')
  const rows = (data(p)?.records || []).filter(r => /^京[A-Z]\d{6}$/.test(String(r.plateNo || '')))
  for (const row of rows) {
    try { await api('POST', `/dms/vehicle/${row.id}/unbind-rider`, {}) } catch (e) { /* ignore */ }
    try { await api('DELETE', `/dms/vehicle/${row.id}`) } catch (e) { /* ignore */ }
  }
  return rows.length
}

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 0) 号段
  const nc = await api('GET', '/dms/vehicle/next-code')
  check('next-code 生成车辆编码（VH 号段）', /^VH\d{3,}$/.test(String(data(nc))), data(nc))

  // 1) 新增（满字段：归属/载重/三点证件/车主）
  const createA = await api('POST', '/dms/vehicle', baseVehicle(PLATE_A, {
    ownershipType: 2,
    ownerName: 'E2E车主',
    ownerPhone: '13700000001',
    operatingPermitNo: 'YYZ' + STAMP,
    operatingPermitExpireDate: plusDays(200),
    insuranceExpireDate: plusDays(10),
    inspectionExpireDate: plusDays(60),
    vin: 'LSVAA4187N2' + STAMP.slice(0, 1) + '01',
  }))
  const a = data(createA)
  check('新增车辆成功', !!a?.id, JSON.stringify(a).slice(0, 200))
  check('新增自动生成车辆编码', /^VH\d{3,}$/.test(String(a?.vehicleCode)), a?.vehicleCode)
  check('新增默认状态=空闲(0)', a?.status === 0 && a?.statusText === '空闲', `${a?.status}/${a?.statusText}`)
  check('新增回填车辆类型文本', a?.vehicleTypeText === '面包车', a?.vehicleTypeText)
  check('新增回填归属类型文本（个人自带）', a?.ownershipTypeText === '个人自带', a?.ownershipTypeText)
  check('新增回填车主信息', a?.ownerName === 'E2E车主' && a?.ownerPhone === '13700000001', `${a?.ownerName}/${a?.ownerPhone}`)
  check('证件到期提示（取最近一证=保险 10 天）', Number(a?.certDaysLeft) === 10 && String(a?.certWarnText).includes('保险'), `${a?.certDaysLeft}/${a?.certWarnText}`)
  const idA = a?.id

  // 1.1) 唯一 / 必填校验
  const dup = await api('POST', '/dms/vehicle', baseVehicle(PLATE_A))
  check('车牌号重复校验', dup.json?.code !== 200, dup.json?.message)
  const noPlate = await api('POST', '/dms/vehicle', baseVehicle(''))
  check('车牌号必填校验', noPlate.json?.code !== 200, noPlate.json?.message)
  const noType = await api('POST', '/dms/vehicle', { ...baseVehicle(PLATE_A + 'X'), vehicleType: null })
  check('车辆类型必填校验', noType.json?.code !== 200, noType.json?.message)
  const noBrand = await api('POST', '/dms/vehicle', { ...baseVehicle(PLATE_A + 'Y'), brand: '' })
  check('品牌必填校验', noBrand.json?.code !== 200, noBrand.json?.message)

  // 2) 详情
  const detail = await api('GET', `/dms/vehicle/${idA}`)
  check('详情回填营运证/车主/载重字段', data(detail)?.operatingPermitNo === 'YYZ' + STAMP
    && data(detail)?.ownerName === 'E2E车主' && Number(data(detail)?.ratedLoad) === 1500,
    JSON.stringify({ p: data(detail)?.operatingPermitNo, o: data(detail)?.ownerName, l: data(detail)?.ratedLoad }))

  // 3) 修改
  const upd = await api('PUT', `/dms/vehicle/${idA}`, baseVehicle(PLATE_A, {
    brand: 'E2E修改后品牌',
    ownershipType: 2,
    ownerName: 'E2E车主',
    insuranceExpireDate: plusDays(10),
  }))
  check('修改车辆成功', upd.json?.code === 200, upd.json?.message)
  check('修改后品牌生效', data(upd)?.brand === 'E2E修改后品牌', data(upd)?.brand)

  // 4) 多条件分页
  const cases = [
    ['关键词（车牌号）', `keyword=${PLATE_A}`, 1],
    ['车辆类型', 'vehicleType=3', null],
    ['归属类型', 'ownershipType=2', null],
    ['车辆状态', 'status=0', null],
    ['当前配送员', 'currentRiderName=E2E配送员A', 0],
    // ⚠️ 中文查询值不要预先 encodeURIComponent：rawReq 里已统一 encodeURI，二次编码会导致 LIKE 匹配不到
    ['所属部门', 'department=E2E配送部', null],
    ['注册日期范围', 'registerDateStart=2024-01-01&registerDateEnd=2024-12-31', null],
    ['证件到期 30 天内', 'expiringDays=30', null],
    ['证件到期 5 天内（不含）', 'expiringDays=5', 0],
  ]
  for (const [name, qs, expect] of cases) {
    const r = await api('GET', `/dms/vehicle/page?pageNum=1&pageSize=50&${qs}`)
    const total = Number(data(r)?.total)
    check(`分页按${name}过滤`, r.json?.code === 200 && (expect == null ? total >= 1 : total === expect), `total=${total}`)
  }
  const sortRes = await api('GET', '/dms/vehicle/page?pageNum=1&pageSize=50&sortField=plateNo&sortOrder=asc')
  check('服务端排序（plateNo asc）返回 200', sortRes.json?.code === 200, JSON.stringify(data(sortRes)?.total))

  // 5) options 选择器（排除已报废）
  const opt1 = await api('GET', '/dms/vehicle/options')
  check('options 选择器返回列表且含本车', Array.isArray(data(opt1)) && data(opt1).some(v => v.plateNo === PLATE_A),
    `count=${(data(opt1) || []).length}`)

  // 6) 人车一对一绑定
  const bind1 = await api('POST', `/dms/vehicle/${idA}/bind-rider`, { riderId: RIDER_A, mileage: 12000, remark: 'E2E 早班出车' })
  check('绑定配送员成功', bind1.json?.code === 200, bind1.json?.message)
  check('绑定后回填当前配送员（currentRider*）', data(bind1)?.currentRiderId === RIDER_A && data(bind1)?.currentRiderName === 'E2E配送员A',
    `${data(bind1)?.currentRiderId}/${data(bind1)?.currentRiderName}`)
  check('绑定时空闲→已出勤(4)', data(bind1)?.status === 4 && data(bind1)?.statusText === '已出勤', `${data(bind1)?.status}/${data(bind1)?.statusText}`)
  check('绑定时录入出车里程', Number(data(bind1)?.currentMileage) === 12000, data(bind1)?.currentMileage)

  // 创建第二辆车用于一对一冲突校验
  const createB = await api('POST', '/dms/vehicle', baseVehicle(PLATE_B))
  const idB = data(createB)?.id
  const bindDupRider = await api('POST', `/dms/vehicle/${idB}/bind-rider`, { riderId: RIDER_A })
  check('同一配送员不可绑第二辆车', bindDupRider.json?.code !== 200, bindDupRider.json?.message)
  const bindDupVehicle = await api('POST', `/dms/vehicle/${idA}/bind-rider`, { riderId: RIDER_B })
  check('同一车辆不可绑第二名配送员', bindDupVehicle.json?.code !== 200, bindDupVehicle.json?.message)

  // 7) 配送员选择器（仅可指派）
  const riderAssignable = await api('GET', '/dms/rider/options?assignable=true')
  const riderAll = await api('GET', '/dms/rider/options')
  check('配送员 options(assignable=true) 排除未审核', Array.isArray(data(riderAssignable))
    && !data(riderAssignable).some(r => r.id === RIDER_C), `count=${(data(riderAssignable) || []).length}`)
  check('配送员 options 全量含未审核', Array.isArray(data(riderAll)) && data(riderAll).some(r => r.id === RIDER_C),
    `count=${(data(riderAll) || []).length}`)

  // 8) 绑定流水（交车回填）
  const unbind1 = await api('POST', `/dms/vehicle/${idA}/unbind-rider`, { mileage: 12345 })
  check('解绑配送员成功', unbind1.json?.code === 200, unbind1.json?.message)
  check('解绑后清空当前配送员', data(unbind1)?.currentRiderId == null && data(unbind1)?.currentRiderName == null,
    `${data(unbind1)?.currentRiderId}/${data(unbind1)?.currentRiderName}`)
  check('解绑后已出勤→空闲(0)', data(unbind1)?.status === 0, data(unbind1)?.status)
  check('解绑回填交车里程', Number(data(unbind1)?.currentMileage) === 12345, data(unbind1)?.currentMileage)
  const hist = await api('GET', `/dms/vehicle/${idA}/binding-history`)
  const h0 = (data(hist) || [])[0]
  check('绑定流水 1 条且状态=已交车', (data(hist) || []).length === 1 && h0?.status === 1, JSON.stringify(h0))
  check('流水含绑定/交车时间与里程', !!h0?.bindTime && !!h0?.handoverTime
    && Number(h0?.bindMileage) === 12000 && Number(h0?.handoverMileage) === 12345,
    JSON.stringify({ b: h0?.bindTime, h: h0?.handoverTime, bm: h0?.bindMileage, hm: h0?.handoverMileage }))
  check('解绑后重复解绑被拒', (await api('POST', `/dms/vehicle/${idA}/unbind-rider`, {})).json?.code !== 200)

  // 9) 状态机
  const st1 = await api('PUT', `/dms/vehicle/${idA}/status`, { status: 2 })
  check('状态 空闲→维修中 成功', data(st1)?.status === 2, data(st1)?.statusText)
  const st2 = await api('PUT', `/dms/vehicle/${idA}/status`, { status: 4 })
  check('状态 维修中→已出勤 被拒（维修中只能回空闲/报废）', st2.json?.code !== 200, st2.json?.message)
  const st3 = await api('PUT', `/dms/vehicle/${idA}/status`, { status: 0 })
  check('状态 维修中→空闲 成功', data(st3)?.status === 0, data(st3)?.statusText)
  const stBad = await api('PUT', `/dms/vehicle/${idA}/status`, { status: 99 })
  check('非法状态值被拒', stBad.json?.code !== 200, stBad.json?.message)
  const st4 = await api('PUT', `/dms/vehicle/${idA}/status`, { status: 3 })
  check('状态 →已报废 成功', data(st4)?.status === 3, data(st4)?.statusText)
  const st5 = await api('PUT', `/dms/vehicle/${idA}/status`, { status: 0 })
  check('已报废为终态，不可再变更', st5.json?.code !== 200, st5.json?.message)
  const editScrapped = await api('PUT', `/dms/vehicle/${idA}`, baseVehicle(PLATE_A, { brand: 'X' }))
  check('已报废车辆不可编辑', editScrapped.json?.code !== 200, editScrapped.json?.message)
  const opt2 = await api('GET', '/dms/vehicle/options')
  check('options 排除已报废车辆', Array.isArray(data(opt2)) && !data(opt2).some(v => v.plateNo === PLATE_A),
    `count=${(data(opt2) || []).length}`)

  // 10) 报废/维修 前置约束（有绑定不可报废）
  const createE = await api('POST', '/dms/vehicle', baseVehicle(PLATE_E))
  const idE = data(createE)?.id
  await api('POST', `/dms/vehicle/${idE}/bind-rider`, { riderId: RIDER_B })
  const scrapBound = await api('PUT', `/dms/vehicle/${idE}/status`, { status: 3 })
  check('绑定中的车辆不可报废', scrapBound.json?.code !== 200, scrapBound.json?.message)
  const delBound = await api('DELETE', `/dms/vehicle/${idE}`)
  check('绑定中的车辆不可删除', delBound.json?.code !== 200, delBound.json?.message)

  // 11) 批量启停
  const createC = await api('POST', '/dms/vehicle', baseVehicle(PLATE_C))
  const createD = await api('POST', '/dms/vehicle', baseVehicle(PLATE_D))
  const idC = data(createC)?.id
  const idD = data(createD)?.id
  const bs = await api('POST', '/dms/vehicle/batch-status', { ids: [idC, idD], status: 2 })
  check('批量状态变更成功', bs.json?.code === 200 && Number(data(bs)) === 2, JSON.stringify(data(bs)))
  const pgC = await api('GET', `/dms/vehicle/page?pageNum=1&pageSize=10&keyword=${PLATE_C}`)
  const pgD = await api('GET', `/dms/vehicle/page?pageNum=1&pageSize=10&keyword=${PLATE_D}`)
  check('批量变更后 C/D 均为维修中(2)', (data(pgC)?.records || [])[0]?.status === 2 && (data(pgD)?.records || [])[0]?.status === 2,
    `${(data(pgC)?.records || [])[0]?.status}/${(data(pgD)?.records || [])[0]?.status}`)
  const bsBad = await api('POST', '/dms/vehicle/batch-status', { ids: [idA], status: 0 })
  check('批量状态变更校验终态（含已报废则整体拒绝）', bsBad.json?.code !== 200, bsBad.json?.message)

  // 12) 证件到期清单（用独立车辆：A 已在状态机用例中报废，清单按业务口径排除已报废车辆）
  const createF = await api('POST', '/dms/vehicle', baseVehicle(PLATE_F, {
    insuranceExpireDate: plusDays(10),
    inspectionExpireDate: plusDays(45),
  }))
  const idF = data(createF)?.id
  check('新增证件到期测试车辆', !!idF, JSON.stringify(data(createF)?.plateNo))
  const expList = await api('GET', '/dms/vehicle/expiring?days=30')
  const expRow = (data(expList) || []).find(r => r.plateNo === PLATE_F && r.certType === 'INSURANCE')
  check('证件到期清单命中保险（10 天）', !!expRow && Number(expRow.daysLeft) === 10 && expRow.warnLevel === 'NORMAL',
    JSON.stringify(expRow))
  const expIns30 = await api('GET', '/dms/vehicle/expiring?days=30&certType=INSPECTION')
  check('证件到期清单按证件类型过滤（年检 45 天不在 30 天窗口）',
    !(data(expIns30) || []).some(r => r.plateNo === PLATE_F), `count=${(data(expIns30) || []).length}`)
  const expIns = await api('GET', '/dms/vehicle/expiring?days=90&certType=INSURANCE')
  check('证件到期清单按证件类型过滤（仅保险）', (data(expIns) || []).every(r => r.certType === 'INSURANCE'),
    `count=${(data(expIns) || []).length}`)

  // 13) 导出真实 xlsx
  const exp = await api('GET', `/dms/vehicle/export?keyword=${PLATE_A}`)
  check('导出返回 xlsx（非 JSON）', exp.buf && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b, `status=${exp.status}`)
  if (exp.buf && exp.buf[0] === 0x50) {
    fs.writeFileSync(path.join(SHOTS, 'export.xlsx'), exp.buf)
    try {
      const wb = XLSX.read(exp.buf, { type: 'buffer' })
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
      check('导出内容含车牌号行', rows.some(r => String(r['车牌号'] || '') === PLATE_A), JSON.stringify(rows[0] || {}).slice(0, 200))
      check('导出含状态与证件列', Object.keys(rows[0] || {}).includes('状态') && Object.keys(rows[0] || {}).includes('保险到期'),
        JSON.stringify(Object.keys(rows[0] || {})))
    } catch (e) {
      check('导出 xlsx 可解析', false, e.message)
    }
  }

  // 14) 批量删除
  const bd = await api('POST', '/dms/vehicle/batch-delete', { ids: [idC, idD] })
  check('批量删除成功', bd.json?.code === 200, bd.json?.message)
  const afterDel = await api('GET', `/dms/vehicle/page?pageNum=1&pageSize=50&keyword=${PLATE_C}`)
  check('批量删除后查询不到（逻辑删除生效）', Number(data(afterDel)?.total) === 0, data(afterDel)?.total)

  // 15) 单车删除（解绑后可删）
  await api('POST', `/dms/vehicle/${idE}/unbind-rider`, {})
  const delE = await api('DELETE', `/dms/vehicle/${idE}`)
  check('解绑后可删除车辆', delE.json?.code === 200, delE.json?.message)

  // 16) 车牌号唯一索引（软删后可复用）
  const reuse = await api('POST', '/dms/vehicle', baseVehicle(PLATE_E))
  check('软删除后车牌号可复用', reuse.json?.code === 200, reuse.json?.message)

  return { idA, idB, idF: null }
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  // 前端 dev server 的 /api 默认代理到 5655，这里转发到验收实例
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

  const UI_PLATE = '京U' + STAMP
  const bodyText = () => page.locator('body').innerText().then(t => t.replace(/\s+/g, ''))

  // ── 列表页骨架 ──
  await openPage(`${FE}/dms/vehicle`)
  const txt0 = await bodyText()
  check('页面可打开（无 404/白屏）', !txt0.includes('页面不存在') && txt0.length > 50, page.url())
  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['车牌号', '车辆类型', '品牌/型号', '归属类型', '当前配送员', '状态', '证件到期', '操作']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  for (const b of ['新增', '到期提醒', '刷新', '打印(F8)', '导出']) {
    check(`工具栏含「${b}」`, txt0.includes(b.replace(/\s+/g, '')))
  }
  for (const q of ['筛选条件', '车辆类型', '归属类型', '车辆状态', '证件到期', '当前配送员', '所属部门', '注册日期', '查询', '重置']) {
    check(`查询区含「${q}」`, txt0.includes(q))
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 列配置齿轮（序号列表头）
  const gearCount = await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length)
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)
  await page.locator('.ss-grid .th-settings-btn').first().click()
  await page.waitForTimeout(1500)
  const colPanel = page.locator('.ant-modal-content:visible').last()
  const colText = (await colPanel.innerText()).replace(/\s+/g, '')
  check('列配置弹窗含「个人配置/全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 80))
  check('列配置含车辆列', ['车牌号', '车辆类型', '归属类型', '证件到期'].every(c => colText.includes(c)), colText.slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 页面配置（齿轮 → 查询条件 / 功能按钮）
  await page.locator('button[title="页面配置"]').first().click()
  await page.waitForTimeout(1200)
  const pageCfg = page.locator('.ant-modal-content:visible').last()
  const cfgText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置弹窗含「查询条件/功能按钮」双 Tab', cfgText.includes('查询条件') && cfgText.includes('功能按钮'), cfgText.slice(0, 80))
  // 功能按钮 Tab 面板懒渲染，需先切换再断言
  await pageCfg.locator('.ant-tabs-tab:has-text("功能按钮")').click()
  await page.waitForTimeout(800)
  const btnText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置含功能按钮项（新增/导出/批量启停）',
    btnText.includes('新增') && btnText.includes('导出') && btnText.includes('批量启停'), btnText.slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // ── 新增（双入口 → 表单页） ──
  await page.click('button:has-text("新增")')
  await page.waitForTimeout(3000)
  check('新增跳转车辆表单页', page.url().includes('/dms/vehicle/form'), page.url())
  const formText = (await bodyText())
  for (const f of ['车牌号', '车辆类型', '归属类型', '核定载重', '注册日期', '保险到期', '年检到期', '营运证到期', '车主姓名']) {
    check(`表单页含字段「${f}」`, formText.includes(f))
  }
  const codeInput = page.locator('input[placeholder*="系统自动生成"]').first()
  check('表单页自动带出车辆编码', /^VH/.test(await codeInput.inputValue()), await codeInput.inputValue())

  await page.locator('input[placeholder*="京A12345"]').first().fill(UI_PLATE)
  // 车辆类型
  await page.locator('.ant-select:has(.ant-select-selection-placeholder:text-is("请选择"))').nth(0).click()
  await page.waitForTimeout(500)
  await page.locator('.ant-select-item-option:has-text("厢式货车")').last().click()
  await page.waitForTimeout(300)
  await page.locator('input[placeholder*="五菱"]').first().fill('UI五菱')
  await page.locator('input[placeholder*="荣光V"]').first().fill('UI荣光')
  await page.locator('input[placeholder*="白色"]').first().fill('蓝色')
  await page.locator('input[placeholder="请输入发动机号"]').first().fill('UIENG' + STAMP)
  // 归属类型（第二个“请选择”下拉）
  await page.locator('.ant-select:has(.ant-select-selection-placeholder:text-is("请选择"))').nth(0).click()
  await page.waitForTimeout(500)
  await page.locator('.ant-select-item-option:has-text("公司自有")').last().click()
  await page.waitForTimeout(300)
  // 注册日期
  const dateInput = page.locator('.ant-picker input').first()
  await dateInput.click()
  await page.waitForTimeout(400)
  await dateInput.fill('2024-03-08')
  await page.keyboard.press('Enter')
  await page.waitForTimeout(400)
  await page.screenshot({ path: path.join(SHOTS, 'ui-form.png'), fullPage: true })

  await page.locator('button:has-text("保存")').first().click()
  await page.waitForTimeout(3500)
  check('保存后返回列表页', page.url().includes('/dms/vehicle') && !page.url().includes('/form'), page.url())
  // 硬刷新确保读到新数据（真机依赖 keepAlive 的 onActivated 刷新，此处避免缓存时序干扰断言）
  await page.goto(`${FE}/dms/vehicle`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(4000)
  const txtAfterSave = await bodyText()
  check('列表出现新增车辆', txtAfterSave.includes(UI_PLATE), UI_PLATE)
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-save.png'), fullPage: true })

  // ── 编辑（表单页回填） ──
  await page.click(`.ss-grid a:has-text("${UI_PLATE}")`)
  await page.waitForTimeout(3000)
  check('编辑跳转表单页', page.url().includes('/dms/vehicle/form'), page.url())
  const editBrand = await page.locator('input[placeholder*="五菱"]').first().inputValue()
  check('编辑回填品牌', editBrand === 'UI五菱', editBrand)
  const editCode = await page.locator('input[placeholder*="系统自动生成"]').first().inputValue()
  check('编辑回填车辆编码', /^VH/.test(editCode), editCode)
  await page.goto(`${FE}/dms/vehicle`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(4000)

  // ── 绑定配送员（先绑后改状态：维修中不可绑定） ──
  await page.locator('.ss-grid tr', { hasText: UI_PLATE }).first().locator('button:has-text("更多")').click()
  await page.waitForTimeout(800)
  await page.locator('.ant-dropdown-menu-item:has-text("绑定配送员")').last().click()
  await page.waitForTimeout(1500)
  // ⚠️ antd 对「纯两个中文字符」的按钮会插入空格（绑 定），文案选择器不可靠 → 用结构选择器
  const bindModal = page.locator('.ant-modal-content:visible').last()
  check('绑定弹窗可打开', (await bindModal.innerText()).includes('配送员'), (await bindModal.innerText()).replace(/\s+/g, '').slice(0, 80))
  await bindModal.locator('.ant-select').first().click()
  await page.waitForTimeout(1200)
  const riderOpts = page.locator('.ant-select-dropdown:visible .ant-select-item-option')
  const optCount = await riderOpts.count()
  check('绑定弹窗加载配送员选择器（仅可指派）', optCount > 0, `count=${optCount}`)
  // 选项池可能含其它会话测试数据中被占用的配送员（人车一对一），优先选本模块验收账号下的配送员，逐个尝试
  const bindRowLocator = () => page.locator('.ss-grid tr', { hasText: UI_PLATE }).first()
  async function ensureBindModalOpen() {
    if ((await page.locator('.ant-modal-content:visible').filter({ hasText: '绑定配送员' }).count()) > 0) return
    await bindRowLocator().locator('button:has-text("更多")').click()
    await page.waitForTimeout(700)
    await page.locator('.ant-dropdown-menu-item:has-text("绑定配送员")').last().click()
    await page.waitForTimeout(1200)
  }
  const labels = (await riderOpts.allInnerTexts()).map(t => t.trim())
  const order = labels.map((t, i) => ({ t, i }))
    .sort((a, b) => (b.t.includes('E2E配送员') ? 1 : 0) - (a.t.includes('E2E配送员') ? 1 : 0))
    .slice(0, 3)
  let boundRow = ''
  let riderName = ''
  for (let k = 0; k < order.length; k++) {
    const { t, i } = order[k]
    riderName = t.split('（')[0]
    await riderOpts.nth(i).click()
    await page.waitForTimeout(400)
    await bindModal.locator('.ant-modal-footer .ant-btn-primary').click()
    await page.waitForTimeout(2500)
    boundRow = await bindRowLocator().innerText()
    if (boundRow.includes(riderName)) break
    // 未成功（该配送员已绑其它车辆 / 校验失败）：同一弹窗内重新选下一个
    if (k < order.length - 1) {
      await ensureBindModalOpen()
      await page.locator('.ant-modal-content:visible').last().locator('.ant-select').first().click()
      await page.waitForTimeout(1000)
    }
  }
  check('绑定后列表显示当前配送员', !!riderName && boundRow.includes(riderName), `${riderName} / ${boundRow.replace(/\s+/g, ' ').slice(0, 120)}`)
  check('绑定后状态自动为「已出勤」', boundRow.includes('已出勤'), boundRow.replace(/\s+/g, ' ').slice(0, 120))
  await page.screenshot({ path: path.join(SHOTS, 'ui-bound.png'), fullPage: true })

  // ── 解绑 ──
  await page.locator('.ss-grid tr', { hasText: UI_PLATE }).first().locator('button:has-text("更多")').click()
  await page.waitForTimeout(800)
  await page.locator('.ant-dropdown-menu-item:has-text("解绑")').last().click()
  await page.waitForTimeout(1000)
  const confirm = page.locator('.ant-modal-confirm:visible').last()
  if (await confirm.count()) {
    await confirm.locator('.ant-modal-confirm-btns .ant-btn-primary').click()
    await page.waitForTimeout(2500)
  }
  const afterUnbind = await page.locator('.ss-grid tr', { hasText: UI_PLATE }).first().innerText()
  check('解绑后当前配送员列清空', !!riderName && !afterUnbind.includes(riderName), afterUnbind.replace(/\s+/g, ' ').slice(0, 120))

  // ── 状态变更 ──
  await page.locator('.ss-grid tr', { hasText: UI_PLATE }).first().locator('button:has-text("状态变更")').click()
  await page.waitForTimeout(1200)
  const stModal = page.locator('.ant-modal-content:visible').last()
  check('状态变更弹窗展示当前状态', (await stModal.innerText()).includes('空闲'), (await stModal.innerText()).replace(/\s+/g, '').slice(0, 80))
  await stModal.locator('.ant-select').first().click()
  await page.waitForTimeout(500)
  await page.locator('.ant-select-item-option:has-text("维修中")').last().click()
  await page.waitForTimeout(300)
  await stModal.locator('.ant-modal-footer .ant-btn-primary').click()
  await page.waitForTimeout(2500)
  const rowTag = await page.locator('.ss-grid tr', { hasText: UI_PLATE }).first().innerText()
  check('状态变更后列表显示「维修中」', rowTag.includes('维修中'), rowTag.replace(/\s+/g, ' ').slice(0, 120))

  // ── 到期提醒 ──
  await page.click('button:has-text("到期提醒")')
  await page.waitForTimeout(2500)
  const expModal = page.locator('.ant-modal-content:visible').last()
  const expModalText = (await expModal.innerText()).replace(/\s+/g, '')
  check('到期提醒弹窗可打开', expModalText.includes('到期窗口') && expModalText.includes('已过期'), expModalText.slice(0, 100))
  const expRows = await expModal.locator('.ant-table-tbody tr').count()
  check('到期提醒弹窗有清单行（保险 10 天）', expRows > 0, `rows=${expRows}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-expiring.png'), fullPage: true })
  await expModal.locator('.ant-modal-close').click()
  await page.waitForTimeout(1000)

  // ── 批量启停 ──
  const checkbox = page.locator('.ss-grid tbody tr', { hasText: UI_PLATE }).first().locator('input.ss-checkbox, input[type=checkbox]').first()
  await checkbox.click()
  await page.waitForTimeout(800)
  const batchBar = (await bodyText())
  check('勾选后出现批量操作条', batchBar.includes('已选择'), batchBar.slice(0, 60))
  await page.locator('button:has-text("批量启停")').first().click()
  await page.waitForTimeout(1200)
  const batchModal = page.locator('.ant-modal-content:visible').last()
  await batchModal.locator('.ant-select').first().click()
  await page.waitForTimeout(500)
  await page.locator('.ant-select-item-option:has-text("空闲")').last().click()
  await page.waitForTimeout(300)
  await batchModal.locator('.ant-modal-footer .ant-btn-primary').click()
  await page.waitForTimeout(2500)
  const afterBatch = await page.locator('.ss-grid tr', { hasText: UI_PLATE }).first().innerText()
  check('批量启停后状态更新为空闲', afterBatch.includes('空闲'), afterBatch.replace(/\s+/g, ' ').slice(0, 120))
  await page.screenshot({ path: path.join(SHOTS, 'ui-batch.png'), fullPage: true })

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await login()
    console.log('登录成功（' + E2E_USER + '）')
    await cleanup()
    await apiSuite()
    try {
      await uiSuite()
    } catch (e) {
      check('UI 验收执行', false, e.message)
      console.error(e)
    }
    await cleanup()
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
