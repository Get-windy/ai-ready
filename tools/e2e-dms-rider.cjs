/*
 * 配送员管理（配送 → 人车管理 → 配送员管理）金标准端到端验证
 *   · API 验收：编号 / 按类型差异化新增校验 / 多条件分页 / 详情脱敏规则 / 修改 / 状态机与资质门控 /
 *              审核(收 remark) / 位置上报心跳 / options / 批量审核 / 批量启停 / 导出 xlsx / 导入模板 / Excel 导入 / 删除
 *   · UI 验收：金标准骨架（CategoryListLayout + 表头齿轮列配置 + 页面配置弹窗）/ 类型差异化表单 / 详情抽屉 / 真实增删改
 *
 * 用法：node tools/e2e-dms-rider.cjs
 *      DMS_PORT=5666 FE_URL=http://localhost:5678 node tools/e2e-dms-rider.cjs
 *
 * 前置：
 *   python tools/dbq.py "$(cat tools/e2e-dms-rider-user.sql)"   # 专用验收账号（避免 admin 互踢）
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5678'
const PORT = Number(process.env.DMS_PORT || 5666)
const SHOTS = 'I:/AI-Ready/tool-results/dms-rider'
const XLSX = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx')

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const E2E_USER = process.env.E2E_USER || 'e2e_rider'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const E2E_USER_ID = '2099000000000001041'

function rawReq(method, reqPath, body, token, extraHeaders) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null || body instanceof Buffer ? body : JSON.stringify(body)
    const headers = {
      ...(body instanceof Buffer ? { 'Content-Type': 'application/octet-stream' } : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {}),
    }
    if (data && !(body instanceof Buffer)) headers['Content-Length'] = Buffer.byteLength(data)
    const rawPath = '/api' + reqPath
    // 只编码非 ASCII（脚本传入的查询串中中文需编码，但不能二次编码已有的 %xx）
    // 只编码非 ASCII 字符（保护已存在的 %xx 不被二次编码）
    const safePath = Array.from(rawPath).map(ch => ch.charCodeAt(0) > 127 ? encodeURIComponent(ch) : ch).join('')
    const r = http.request({ hostname: 'localhost', port: PORT, path: safePath, method, headers }, res => {
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

function data(res) { return res.json ? res.json.data : null }
function msg(res) { return (res.json && res.json.message) || '' }

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  let token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) {
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
const D1 = new Date(Date.now() + 86400000 * 180).toISOString().slice(0, 10)   // 未过期
const PAST = new Date(Date.now() - 86400000 * 2).toISOString().slice(0, 10)   // 已过期

let CHANNEL_ID = null
const created = []

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 0) 编号
  const nc = await api('GET', '/dms/rider/next-code')
  check('next-code 返回 PSY 编号', /^PSY\d{4}$/.test(String(data(nc) || '')), data(nc))

  // 1) 企业员工缺「关联系统账号」→ 拒绝
  const bad1 = await api('POST', '/dms/rider', { realName: 'E2E缺账号' + STAMP, phone: '13800000001', riderType: 1 })
  check('企业员工缺关联账号被拒绝', bad1.status !== 200 || Number(bad1.json?.code) !== 200, msg(bad1) + '|' + bad1.status)

  // 2) 手机号格式校验
  const badPhone = await api('POST', '/dms/rider', { realName: 'E2E坏手机' + STAMP, phone: '123', riderType: 2 })
  check('手机号格式校验生效', badPhone.status !== 200 || Number(badPhone.json?.code) !== 200, msg(badPhone))

  // 3) 企业员工（完整）
  const ownStaff = {
    realName: 'E2E员工' + STAMP, phone: '13800000002', riderType: 1,
    userId: E2E_USER_ID, deptName: 'E2E部门', entryDate: '2026-01-05',
    idCard: '110101199001011234', workHoursStart: '09:00', workHoursEnd: '18:00',
    serviceRadius: 5, maxConcurrent: 3, remark: 'E2E企业员工',
  }
  const r1 = await api('POST', '/dms/rider', ownStaff)
  const v1 = data(r1)
  check('新增企业员工成功', Number(r1.json?.code) === 200 && v1?.id, msg(r1))
  check('新增自动生成配送员编号', /^PSY\d{4}$/.test(String(v1?.riderNo || '')), v1?.riderNo)
  check('新增默认待审核(verifyStatus=0)', v1?.verifyStatus === 0, String(v1?.verifyStatus))
  check('新增默认离线(status=0)', v1?.status === 0, String(v1?.status))
  check('企业员工归属回填系统账号名', !!v1?.userName, v1?.userName)
  check('类型文案=企业员工', v1?.riderTypeText === '企业员工', v1?.riderTypeText)
  if (v1?.id) created.push(v1.id)

  // 4) 外部平台缺渠道 → 拒绝
  const bad2 = await api('POST', '/dms/rider', { realName: 'E2E缺渠道' + STAMP, phone: '13800000003', riderType: 3 })
  check('外部平台缺归属渠道被拒绝', bad2.status !== 200 || Number(bad2.json?.code) !== 200, msg(bad2))

  // 5) 先建渠道，再建外部平台配送员
  const chRes = await api('POST', '/dms/channel', {
    channelCode: 'E2ECH' + STAMP, channelName: 'E2E运力渠道' + STAMP, channelType: 3, status: 1, priority: 5,
  })
  CHANNEL_ID = data(chRes)?.id || (data(chRes) && Number(data(chRes)))
  check('渠道创建成功（外部平台前置）', !!CHANNEL_ID, JSON.stringify(data(chRes)).slice(0, 120))

  const platform = {
    realName: 'E2E平台骑手' + STAMP, phone: '13800000004', riderType: 3,
    channelId: CHANNEL_ID, platformRiderId: 'DD-' + STAMP,
    qualificationExpireDate: D1, settleMethod: 1, remark: 'E2E外部平台',
  }
  const r2 = await api('POST', '/dms/rider', platform)
  const v2 = data(r2)
  check('新增外部平台配送员成功', Number(r2.json?.code) === 200 && v2?.id, msg(r2))
  check('外部平台归属渠道名回填', v2?.channelName === 'E2E运力渠道' + STAMP, v2?.channelName)
  check('外部平台骑手ID落库', v2?.platformRiderId === 'DD-' + STAMP, v2?.platformRiderId)
  check('资质到期日回填', v2?.qualificationExpireDate === D1, v2?.qualificationExpireDate)
  check('资质未过期标记', v2?.qualificationExpired === false, String(v2?.qualificationExpired))
  if (v2?.id) created.push(v2.id)

  // 6) 社会车辆缺车牌 → 拒绝
  const bad3 = await api('POST', '/dms/rider', { realName: 'E2E缺车牌' + STAMP, phone: '13800000005', riderType: 4 })
  check('社会车辆司机缺车牌被拒绝', bad3.status !== 200 || Number(bad3.json?.code) !== 200, msg(bad3))

  // 7) 社会车辆（完整）
  const social = {
    realName: 'E2E社会司机' + STAMP, phone: '13800000006', riderType: 4,
    vehicleNo: '苏A' + STAMP, idCard: '320101198505054321', settleMethod: 2,
  }
  const r3 = await api('POST', '/dms/rider', social)
  const v3 = data(r3)
  check('新增社会车辆司机成功', Number(r3.json?.code) === 200 && v3?.id, msg(r3))
  check('结算方式文案回填', v3?.settleMethodText === '月结', v3?.settleMethodText)
  if (v3?.id) created.push(v3.id)

  // 8) 分页多条件
  const p1 = await api('GET', `/dms/rider/page?pageNum=1&pageSize=20&keyword=${'E2E员工' + STAMP}`)
  const p1d = data(p1)
  check('分页按关键字命中', Number(p1d?.total) >= 1, `total=${p1d?.total}`)
  check('分页返回 VO 派生字段', !!(p1d?.records?.[0]?.riderTypeText && p1d?.records?.[0]?.onlineStatusText != null), JSON.stringify(p1d?.records?.[0] || {}).slice(0, 150))
  check('列表身份证脱敏', String(p1d?.records?.[0]?.idCard || '').includes('****'), p1d?.records?.[0]?.idCard)

  const p2 = await api('GET', '/dms/rider/page?pageNum=1&pageSize=50&riderTypes=4')
  const p2d = data(p2)
  check('类型多选过滤生效', (p2d?.records || []).every(r => r.riderType === 4) && Number(p2d?.total) >= 1, `total=${p2d?.total}`)

  const p3 = await api('GET', '/dms/rider/page?pageNum=1&pageSize=50&verifyStatus=0')
  check('审核状态过滤生效', (data(p3)?.records || []).every(r => r.verifyStatus === 0), `total=${data(p3)?.total}`)

  const p4 = await api('GET', `/dms/rider/page?pageNum=1&pageSize=50&channelId=${CHANNEL_ID}`)
  const p4d = data(p4)
  check('归属渠道过滤生效', (p4d?.records || []).every(r => r.channelId === CHANNEL_ID) && Number(p4d?.total) >= 1, `total=${p4d?.total}`)

  const p5 = await api('GET', `/dms/rider/page?pageNum=1&pageSize=50&keyword=${STAMP}`)
  check('关键字按手机号/编号通配命中', Number(data(p5)?.total) >= 3, `total=${data(p5)?.total}`)

  // 9) 详情：身份证不脱敏 + 归属
  const d1 = await api('GET', `/dms/rider/${v1.id}`)
  const d1d = data(d1)
  check('详情返回原始身份证（编辑回显）', d1d?.idCard === '110101199001011234', d1d?.idCard)
  check('详情含部门快照', d1d?.deptName === 'E2E部门', d1d?.deptName)
  check('详情含入职日期', d1d?.entryDate === '2026-01-05', d1d?.entryDate)

  // 10) 修改
  const up = await api('PUT', `/dms/rider/${v1.id}`, { ...ownStaff, realName: 'E2E员工改' + STAMP })
  check('修改配送员成功', Number(up.json?.code) === 200 && data(up)?.realName === 'E2E员工改' + STAMP, msg(up))

  const dupNo = await api('PUT', `/dms/rider/${v1.id}`, { ...ownStaff, riderNo: v2.riderNo })
  check('修改重号被拒绝', dupNo.status !== 200 || Number(dupNo.json?.code) !== 200, msg(dupNo))

  // 11) 状态机 + 资质门控（未审核不可接单）
  const st0 = await api('PUT', `/dms/rider/${v1.id}/status`, { status: 1 })
  check('未审核配送员置「空闲」被拒绝（资质门控）', st0.status !== 200 || Number(st0.json?.code) !== 200, msg(st0))

  // 12) 审核（收 remark）
  const ap = await api('POST', `/dms/rider/${v1.id}/approve`, { verifyStatus: 1, remark: 'E2E审核通过' })
  check('审核通过接口成功', Number(ap.json?.code) === 200, msg(ap))
  const d2 = data(await api('GET', `/dms/rider/${v1.id}`))
  check('审核状态已通过', d2?.verifyStatus === 1, String(d2?.verifyStatus))
  check('审核备注落库', d2?.verifyRemark === 'E2E审核通过', d2?.verifyRemark)

  const st1 = await api('PUT', `/dms/rider/${v1.id}/status`, { status: 1 })
  const d3 = data(await api('GET', `/dms/rider/${v1.id}`))
  check('审核通过后可置「空闲」', Number(st1.json?.code) === 200 && d3?.status === 1, msg(st1) + '|status=' + d3?.status)

  const stBad = await api('PUT', `/dms/rider/${v1.id}/status`, { status: 9 })
  check('非法状态被拒绝', stBad.status !== 200 || Number(stBad.json?.code) !== 200, msg(stBad))

  // 13) 位置上报（在线心跳）
  const loc = await api('POST', '/dms/rider/location', { riderId: v1.id, lat: 31.2304, lng: 121.4737 })
  const d4 = data(await api('GET', `/dms/rider/${v1.id}`))
  check('位置上报成功', Number(loc.json?.code) === 200, msg(loc))
  check('最近上报时间已更新', !!d4?.lastReportTime, d4?.lastReportTime)
  check('心跳判定在线', d4?.onlineStatus === 1 && d4?.onlineStatusText === '在线', d4?.onlineStatusText)
  const p6 = await api('GET', '/dms/rider/page?pageNum=1&pageSize=50&onlineStatus=1')
  check('在线状态过滤生效', (data(p6)?.records || []).some(r => r.id === v1.id), `total=${data(p6)?.total}`)

  // 14) 资质过期门控
  await api('PUT', `/dms/rider/${v2.id}`, { ...platform, qualificationExpireDate: PAST })
  await api('POST', `/dms/rider/${v2.id}/approve`, { verifyStatus: 1, remark: 'E2E背书过期' })
  const expiredSt = await api('PUT', `/dms/rider/${v2.id}/status`, { status: 1 })
  check('资质过期不可置「空闲」', expiredSt.status !== 200 || Number(expiredSt.json?.code) !== 200, msg(expiredSt))
  const p7 = await api('GET', '/dms/rider/page?pageNum=1&pageSize=50&qualifyExpireAlert=1')
  check('资质到期提醒过滤命中过期项', (data(p7)?.records || []).some(r => r.id === v2.id), `total=${data(p7)?.total}`)

  // 15) 审核拒绝 → 强制离线
  const rej = await api('POST', `/dms/rider/${v3.id}/approve`, { verifyStatus: 2, remark: 'E2E资料不全' })
  const d5 = data(await api('GET', `/dms/rider/${v3.id}`))
  check('审核拒绝成功', Number(rej.json?.code) === 200, msg(rej))
  check('拒绝后强制离线', d5?.verifyStatus === 2 && d5?.status === 0, `verify=${d5?.verifyStatus},status=${d5?.status}`)

  // 16) options
  const opt = await api('GET', '/dms/rider/options?assignable=true')
  const optList = data(opt) || []
  check('options 返回选择器数据', Array.isArray(optList) && optList.length >= 1, `count=${optList.length}`)
  check('options(assignable) 仅返回可指派（审核通过且未过期）',
    optList.every(o => o.id !== v2.id), optList.map(o => o.id).join(','))

  // 17) 批量审核 / 批量启停
  const ba = await api('POST', '/dms/rider/batch-approve', {
    ids: [v2.id, v3.id], verifyStatus: 1, remark: 'E2E批量通过',
  })
  check('批量审核成功', Number(ba.json?.code) === 200 && Number(data(ba)) === 2, msg(ba))
  const bs = await api('POST', '/dms/rider/batch-status', { ids: [v2.id, v3.id], status: 3 })
  check('批量启停成功', Number(bs.json?.code) === 200, msg(bs))
  const bsv = data(await api('GET', `/dms/rider/${v3.id}`))
  check('批量启停生效（休息）', bsv?.status === 3, String(bsv?.status))

  // 18) 导出
  const exp = await api('GET', `/dms/rider/export?keyword=${STAMP}`)
  check('导出返回真实 xlsx', exp.buf && exp.buf[0] === 0x50, `status=${exp.status}`)
  try {
    const wb = XLSX.read(exp.buf, { type: 'buffer' })
    const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
    check('导出含配送员编号列', rows.length > 0 && Object.keys(rows[0]).includes('配送员编号'), JSON.stringify(rows[0] || {}).slice(0, 150))
    check('导出归属列有值', rows.some(r => String(r['归属'] || '').includes('E2E')), JSON.stringify(rows[0] || {}).slice(0, 150))
  } catch (e) {
    check('导出 xlsx 可解析', false, e.message)
  }

  // 19) 导入模板
  const tpl = await api('GET', '/dms/rider/import-template')
  check('导入模板返回 xlsx', tpl.buf && tpl.buf[0] === 0x50, `status=${tpl.status}`)

  // 20) Excel 导入（1 成功 + 1 重号失败）
  const wbIn = XLSX.utils.book_new()
  const wsIn = XLSX.utils.aoa_to_sheet([
    ['导入结果', '配送员编号', '姓名', '手机号', '配送员类型', '归属渠道名称', '平台骑手ID', '身份证号',
      '驾驶证号', '健康证号', '资质到期日期', '入职日期', '部门名称', '车牌号', '结算方式', '备注'],
    ['', '', 'E2E导入众包' + STAMP, '13900000001', '众包兼职', '', '', '', '', '', '', '', '', '', '按单结算', 'E2E导入'],
    ['', v1.riderNo, 'E2E导入重号' + STAMP, '13900000002', '众包兼职', '', '', '', '', '', '', '', '', '', '', ''],
  ])
  XLSX.utils.book_append_sheet(wbIn, wsIn, '配送员信息')
  const importBuf = XLSX.write(wbIn, { type: 'buffer', bookType: 'xlsx' })
  const boundary = '----e2eDmsRider' + STAMP
  const pre = Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="rider.xlsx"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n`)
  const post = Buffer.from(`\r\n--${boundary}--\r\n`)
  const imp = await api('POST', '/dms/rider/import-excel', Buffer.concat([pre, importBuf, post]),
    { 'Content-Type': `multipart/form-data; boundary=${boundary}` })
  const impD = data(imp)
  check('Excel 导入返回统计', impD && impD.total === 2, JSON.stringify(impD || {}).slice(0, 150))
  check('Excel 导入成功 1 行', Number(impD?.success) === 1, JSON.stringify(impD || {}).slice(0, 150))
  check('Excel 导入重号行失败 1 行', Number(impD?.failure) === 1, JSON.stringify((impD?.errors || []).slice(0, 2)))

  const pImp = await api('GET', `/dms/rider/page?pageNum=1&pageSize=50&keyword=${'E2E导入众包' + STAMP}`)
  const impRow = (data(pImp)?.records || [])[0]
  check('导入行真实落库', !!impRow, JSON.stringify(impRow || {}).slice(0, 120))
  if (impRow?.id) created.push(impRow.id)

  // 21) list（兼容 dispatch 旧调用）
  const lst = await api('GET', '/dms/rider/list?pageSize=5')
  check('list 接口兼容返回数组', Array.isArray(data(lst)), `isArray=${Array.isArray(data(lst))}`)

  // 22) 删除 / 批量删除
  const del = await api('DELETE', `/dms/rider/${v3.id}`)
  check('删除配送员成功', Number(del.json?.code) === 200, msg(del))
  const delDetail = await api('GET', `/dms/rider/${v3.id}`)
  check('删除后详情不可见', Number(delDetail.json?.code) !== 200, `${delDetail.status}/${msg(delDetail)}`)

  const ids = created.filter(id => id !== v3.id)
  const bd = await api('POST', '/dms/rider/batch-delete', { ids })
  check('批量删除成功', Number(bd.json?.code) === 200 && Number(data(bd)) >= 1, msg(bd))

  return { v1, v2 }
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  const norm = t => String(t || '').replace(/[\s\u00a0\u2002]+/g, '')

  /** 页面内按文本点击（antd 两字中文按钮会自动插空格，规范化空白后再匹配） */
  async function clickByText(text, sel = 'button') {
    const ok = await page.evaluate(([t, s]) => {
      const n = x => String(x || '').replace(/[\s\u00a0\u2002]+/g, '')
      const vis = e => { const r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0 }
      const els = [...document.querySelectorAll(s)].filter(vis)
      const el = els.find(e => n(e.innerText) === t) || els.find(e => n(e.innerText).includes(t))
      if (!el) return false
      el.click()
      return true
    }, [text, sel])
    await page.waitForTimeout(700)
    return ok
  }

  /** 在「行内包含 rowText 的那一行」点击按钮 */
  async function clickInRow(rowText, text) {
    const ok = await page.evaluate(([rt, t]) => {
      const n = x => String(x || '').replace(/[\s\u00a0\u2002]+/g, '')
      const vis = e => { const r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0 }
      const rows = [...document.querySelectorAll('.ss-grid tbody tr')].filter(r => n(r.innerText).includes(rt))
      for (const row of rows) {
        const el = [...row.querySelectorAll('button')].filter(vis).find(e => n(e.innerText) === t || n(e.innerText).includes(t))
        if (el) { el.click(); return true }
      }
      return false
    }, [rowText, text])
    await page.waitForTimeout(700)
    return ok
  }

  async function openPage(url, waitMs = 9000) {
    for (let attempt = 0; attempt < 3; attempt++) {
      await login()
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(([tk]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', '1')
      }, [TOKEN])
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(waitMs)
      await page.waitForSelector('.ss-grid, .basic-info, .ant-form', { timeout: 20000 }).catch(() => {})
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }

  // ── 前置数据 ──
  const uiName = 'E2E界面' + STAMP
  const uiRes = await api('POST', '/dms/rider', {
    realName: uiName, phone: '13700000001', riderType: 2, idCard: '110101199203031234', settleMethod: 1,
  })
  const uiId = data(uiRes)?.id
  check('UI 前置：新增众包配送员', !!uiId, msg(uiRes))

  // ── 列表页 ──
  await openPage(`${FE}/dms/rider`)
  const bodyText = norm(await page.locator('body').innerText())
  check('页面可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())

  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  const headerNorm = headers.map(h => h.replace(/\s+/g, ''))
  for (const h of ['配送员编号', '姓名', '手机号', '类型', '归属', '接单状态', '在线状态', '审核状态', '资质到期', '评分', '累计单量', '当前车辆', '操作']) {
    check(`表头含「${h}」`, headerNorm.some(x => x.includes(h)), headerNorm.join('/'))
  }
  for (const b of ['新增', '导入', '同步外部运力', '刷新', '导出']) {
    check(`工具栏含「${b}」`, bodyText.includes(b))
  }
  check('查询区含「资质到期提醒」', bodyText.includes('资质到期提醒'))
  check('查询区含「配送员类型」多选', bodyText.includes('配送员类型'))
  check('查询区含「归属渠道」', bodyText.includes('归属渠道'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  const gear = await page.evaluate(() => {
    const ths = [...document.querySelectorAll('.ss-grid th')]
    return ths.some(th => th.querySelector('.anticon-setting, .icon-shezhi2, button, i'))
  })
  check('数据表表头存在列配置齿轮', gear)

  const gearClicked = await page.evaluate(() => {
    const vis = e => { const r = e.getBoundingClientRect(); return r.width > 0 && r.height > 0 }
    for (const th of [...document.querySelectorAll('.ss-grid th')]) {
      const el = [...th.querySelectorAll('.anticon-setting, .icon-shezhi2, button, i, a')].filter(vis)[0]
      if (el) { el.click(); return true }
    }
    return false
  })
  if (gearClicked) {
    await page.waitForTimeout(1600)
    const colCfgNorm = norm(await page.locator('.ant-modal-content:visible').first().innerText().catch(() => ''))
    check('表头齿轮可打开列配置弹窗（个人/全局）', colCfgNorm.includes('配置') && (colCfgNorm.includes('个人') || colCfgNorm.includes('全局')), colCfgNorm.slice(0, 70))
    await page.locator('.ant-modal-close').first().click().catch(() => {})
    await page.waitForTimeout(900)
    // 兜底：连续 Esc 关闭所有残留弹窗，避免影响后续「页面配置」断言
    for (let i = 0; i < 3 && (await page.locator('.ant-modal-wrap').first().isVisible().catch(() => false)); i++) {
      await page.keyboard.press('Escape')
      await page.waitForTimeout(700)
    }
    check('列配置弹窗可关闭', !(await page.locator('.ant-modal-wrap').first().isVisible().catch(() => false)))
  } else {
    check('表头齿轮可打开列配置弹窗（个人/全局）', false, '未找到齿轮按钮')
  }

  // ── 页面配置弹窗（查询条件 / 功能按钮，无打印配置） ──
  const cfgBtn = page.locator('.toolbar-right button').first()
  if (await cfgBtn.count()) {
    await cfgBtn.click()
    await page.waitForTimeout(1500)
    const cfgNorm = norm(await page.locator('.ant-modal-content:visible').first().innerText().catch(() => ''))
    check('页面配置弹窗打开', cfgNorm.includes('页面配置'), cfgNorm.slice(0, 60))
    check('页面配置含「查询条件」Tab', cfgNorm.includes('查询条件'))
    check('页面配置含「功能按钮」Tab', cfgNorm.includes('功能按钮'))
    check('页面配置无「打印配置」Tab', !cfgNorm.includes('打印配置'))
    await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png') })
    await page.locator('.ant-modal-close').first().click().catch(() => {})
    await page.waitForTimeout(1000)
    check('页面配置弹窗可关闭', !(await page.locator('.ant-modal-wrap').first().isVisible().catch(() => false)))
  } else {
    check('页面配置弹窗打开', false, '未找到齿轮按钮')
  }

  // ── 查询过滤 ──
  await page.locator('.search-grid input').first().fill(uiName)
  await clickByText('查询')
  await page.waitForTimeout(3000)
  const filtered = await page.evaluate(() => [...document.querySelectorAll('.ss-grid tbody tr')].map(tr => tr.innerText.replace(/\s+/g, '')).join('|'))
  check('关键字查询命中目标行', filtered.includes(uiName), filtered.slice(0, 120))
  check('查询过滤生效（不含其它配送员）', !filtered.includes('E2E平台骑手' + STAMP), filtered.slice(0, 120))

  // ── 详情抽屉 ──
  const viewOk = await clickInRow(uiName, '查看')
  await page.waitForTimeout(2500)
  const drawerNorm = norm(await page.locator('.ant-drawer-content').first().innerText().catch(() => ''))
  check('行内查看打开详情抽屉', viewOk && drawerNorm.includes('配送员详情'), drawerNorm.slice(0, 60))
  check('详情含资质信息区', drawerNorm.includes('资质信息'))
  check('详情含归属与结算区', drawerNorm.includes('归属与结算'))
  check('详情含绩效与设置区', drawerNorm.includes('绩效与设置'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-detail.png') })
  await page.locator('.ant-drawer-close').first().click().catch(() => {})
  await page.waitForTimeout(800)

  // ── 新增 → 独立表单页（双入口） ──
  await clickByText('新增')
  await page.waitForTimeout(3500)
  check('新增跳转独立表单页', page.url().includes('/dms/rider/form'), page.url())
  const formNorm = norm(await page.locator('body').innerText())
  check('表单含「配送员编号」', formNorm.includes('配送员编号'))
  check('表单默认企业员工含「关联系统账号」', formNorm.includes('关联系统账号'), formNorm.slice(0, 80))
  check('表单含「资质到期」', formNorm.includes('资质到期'))

  const typeSelect = page.locator('.form-page-wrapper .ant-select').first()
  if (await typeSelect.count()) {
    await typeSelect.click()
    await page.waitForTimeout(900)
    await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: '外部平台配送员' }).first().click()
    await page.waitForTimeout(1200)
    const formNorm2 = norm(await page.locator('body').innerText())
    check('切换外部平台后出现「归属渠道」', formNorm2.includes('归属渠道'))
    check('切换外部平台后出现「平台骑手ID」', formNorm2.includes('平台骑手ID'))
    check('渠道背书有效期文案生效', formNorm2.includes('渠道背书有效期'))
  } else {
    check('切换外部平台后出现「归属渠道」', false, '未找到类型下拉')
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-form.png'), fullPage: true })

  await clickByText('保存')
  await page.waitForTimeout(1500)
  const warnNorm = norm(await page.locator('.ant-message').innerText().catch(() => ''))
  const inlineErr = await page.locator('.ant-form-item-explain-error').count()
  check('表单必填校验拦截（行内红字 + 提示）', inlineErr > 0 || warnNorm.includes('必填') || warnNorm.includes('姓名') || warnNorm.includes('渠道'),
    `inlineErr=${inlineErr}|${warnNorm.slice(0, 40)}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-form-validate.png'), fullPage: true })

  await page.locator('.form-page-wrapper .ant-select').first().click()
  await page.waitForTimeout(900)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: '众包兼职' }).first().click()
  await page.waitForTimeout(900)
  const inputs = page.locator('.form-page-wrapper input.ant-input')
  await inputs.nth(1).fill('E2E表单' + STAMP)
  await inputs.nth(2).fill('13600000001')
  const saveResp = page.waitForResponse(r => r.url().includes('/api/dms/rider') && r.request().method() === 'POST', { timeout: 20000 }).catch(() => null)
  await clickByText('保存')
  const saved = await saveResp
  check('表单保存调用新增接口', !!saved, saved ? String(saved.status()) : 'timeout')
  await page.waitForTimeout(3000)
  const afterSave = await api('GET', `/dms/rider/page?pageNum=1&pageSize=20&keyword=${'E2E表单' + STAMP}`)
  const savedRow = (data(afterSave)?.records || [])[0]
  check('表单保存后真实落库', !!savedRow, JSON.stringify(savedRow || {}).slice(0, 120))
  if (savedRow?.id) created.push(savedRow.id)
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-save.png'), fullPage: true })

  // ── 编辑回填（双入口） ──
  await openPage(`${FE}/dms/rider/form?id=${uiId}`, 8000)
  const editNorm = norm(await page.locator('body').innerText())
  check('编辑页标记编辑态', editNorm.includes('编辑'), editNorm.slice(0, 60))
  const editValues = await page.evaluate(() => [...document.querySelectorAll('input.ant-input')].map(i => i.value).join('|'))
  check('编辑页回填姓名', editValues.includes(uiName), editValues.slice(0, 120))

  // ── 审核弹窗（列表页） ──
  await openPage(`${FE}/dms/rider`)
  await page.locator('.search-grid input').first().fill(uiName)
  await clickByText('查询')
  await page.waitForTimeout(2500)
  const apOk = await clickInRow(uiName, '审核')
  await page.waitForTimeout(1500)
  const apNorm = norm(await page.locator('.ant-modal-content').first().innerText().catch(() => ''))
  check('审核弹窗打开', apOk && apNorm.includes('配送员审核'), apNorm.slice(0, 60))
  await page.locator('.ant-modal-content .ant-radio-wrapper').filter({ hasText: '通过' }).first().click().catch(() => {})
  await page.waitForTimeout(400)
  await page.locator('.ant-modal-content textarea').first().fill('E2E界面审核').catch(() => {})
  const apResp = page.waitForResponse(r => r.url().includes('/approve'), { timeout: 20000 }).catch(() => null)
  await clickByText('确认', '.ant-modal-content button')
  const apRes = await apResp
  check('审核提交成功', !!apRes && apRes.status() === 200, apRes ? String(apRes.status()) : 'timeout')
  await page.waitForTimeout(1800)
  const apAfter = data(await api('GET', `/dms/rider/${uiId}`))
  check('审核结果落库（含备注）', apAfter?.verifyStatus === 1 && apAfter?.verifyRemark === 'E2E界面审核', `${apAfter?.verifyStatus}/${apAfter?.verifyRemark}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-approve.png') })

  // ── 导出下载 ──
  const dl = page.waitForEvent('download', { timeout: 20000 }).catch(() => null)
  await clickByText('导出')
  const download = await dl
  check('导出触发文件下载', !!download, download ? download.suggestedFilename() : 'timeout')

  // ── 批量操作条 ──
  const cb = page.locator('.ss-grid tbody tr input.ss-checkbox').first()
  if (await cb.count()) {
    await cb.check({ force: true }).catch(() => {})
    await page.waitForTimeout(1200)
    const batchNorm = norm(await page.locator('body').innerText())
    check('勾选后出现批量操作条（批量审核/批量启停）', batchNorm.includes('批量审核') && batchNorm.includes('批量启停'), batchNorm.slice(0, 80))
    await page.screenshot({ path: path.join(SHOTS, 'ui-batch.png'), fullPage: true })
    await page.locator('.ant-btn-link').filter({ hasText: '取消选择' }).first().click().catch(() => {})
    await page.waitForTimeout(600)
  } else {
    check('勾选后出现批量操作条（批量审核/批量启停）', false, '未找到行复选框')
  }

  // ── 删除（真实 DELETE，非假删除） ──
  await page.locator('.search-grid input').first().fill(uiName)
  await clickByText('查询')
  await page.waitForTimeout(2500)
  const delClicked = await clickInRow(uiName, '删除')
  await page.waitForTimeout(1000)
  const delResp = page.waitForResponse(r => r.url().includes('/api/dms/rider/') && r.request().method() === 'DELETE', { timeout: 20000 }).catch(() => null)
  await clickByText('确认删除', '.ant-modal-confirm button')
  const delRes = await delResp
  check('行内删除调用 DELETE 接口（非假删除）', delClicked && !!delRes && delRes.status() === 200, delRes ? String(delRes.status()) : 'timeout')
  await page.waitForTimeout(1500)
  const delAfter = await api('GET', `/dms/rider/${uiId}`)
  check('删除后数据真实不可见', Number(delAfter.json?.code) !== 200, msg(delAfter))

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await login()
    console.log('登录成功:', E2E_USER)
    const apiData = await apiSuite()
    await uiSuite()
  } catch (e) {
    console.error('执行异常:', e && e.stack || e)
    check('脚本执行无异常', false, String(e && e.message || e))
  }

  const ok = results.filter(r => r.ok).length
  const bad = results.filter(r => !r.ok)
  console.log(`\n═══ 验收结果：${ok}/${results.length} 通过 ═══`)
  if (bad.length) {
    console.log('失败项：')
    bad.forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  }
  fs.writeFileSync(path.join(SHOTS, 'result.json'), JSON.stringify({ ok, total: results.length, results }, null, 2))
  process.exit(bad.length ? 1 : 0)
})()
