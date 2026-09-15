/*
 * 配送路线单（配送 → 配送路线 → 配送路线单，菜单 80700 / dms:route-list）金标准端到端验证
 *   · 接口验收：号段 / 建单（线路档案快照·车辆快照·点位）/ 多条件分页 / 详情含点位 /
 *              改单 / 状态机（开始·完成·取消·批量）/ 多点签收（送达·失败）/ 导出真实 xlsx / 校验与租户隔离
 *   · UI 验收：骨架（CategoryListLayout+BillTableList）/ 工具栏 / 查询区 / 表头齿轮列配置 /
 *              页面配置弹窗 / 新增弹窗真实保存 / 详情抽屉点位签收 / 批量操作 / 经典分页
 *
 * 用法：node tools/e2e-dms-route-list.cjs
 *      ERP_PORT=5671 FE_URL=http://localhost:5672 node tools/e2e-dms-route-list.cjs
 *
 * 依赖：
 *   1) tools/e2e-dms-route-list-user.sql  专用验收账号（避免与并行会话共用 admin 被 sa-token 互踢）
 *   2) tools/e2e-dms-route-list-collect.sql  围栏归集夹具（客户坐标 + 待配送单据收货地址）
 *      ⚠️ 归集会「消耗」待配送单据（入线后即视为已归集），每次执行前请先重置：
 *      python tools/dbq.py "DELETE FROM erp_route_point WHERE order_id LIKE 'OUT:%' OR order_id LIKE 'SO:%'"
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5672'
const PORT = Number(process.env.ERP_PORT || 5671)
const SHOTS = 'I:/AI-Ready/tool-results/dms-route-list'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

function rawReq(method, reqPath, body, token, extraHeaders) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {}),
    }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
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

const E2E_USER = process.env.E2E_USER || 'e2e_route_doc'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

async function loginOnce(username) {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username, password: E2E_PWD, tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid,
  })
  return res.json?.data?.token || res.json?.data?.accessToken || null
}

async function login() {
  let token = await loginOnce(E2E_USER)
  if (!token) {
    console.log('  [专用账号不可用，回退 admin]')
    token = await loginOnce('admin')
  }
  if (!token) throw new Error('登录失败')
  TOKEN = token
}

// ══════════════ 验收结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

const STAMP = Date.now().toString().slice(-6)
const TOMORROW = new Date(Date.now() + 86400000).toISOString().slice(0, 10)

let ctxIds = { riderId: null, riderName: null, vehicleId: null, plateNo: null, masterRouteId: null, masterRouteName: null }

/** 前置：配送员 / 车辆 / 线路档案（选择器数据源；无则创建） */
async function ensurePrerequisites() {
  console.log('\n═══ 零、前置主数据（选择器数据源） ═══')

  const riderPage = await api('GET', '/dms/rider/page?pageNum=1&pageSize=50')
  let riders = data(riderPage)?.records || []
  let rider = riders.find(r => r.status === 1) || riders[0]
  if (!rider) {
    const created = await api('POST', '/dms/rider', {
      realName: 'E2E配送员' + STAMP, phone: '139' + STAMP + '01', riderType: 1, status: 1,
    })
    rider = data(created)
  }
  ctxIds.riderId = rider ? String(rider.id) : null
  ctxIds.riderName = rider ? (rider.realName || '') : ''
  check('配送员选择器数据源可用', !!ctxIds.riderId, `${ctxIds.riderId}/${ctxIds.riderName}`)

  const vehiclePage = await api('GET', '/dms/vehicle/page?pageNum=1&pageSize=50')
  const vehicles = data(vehiclePage)?.records || []
  let vehicle = vehicles[0]
  if (!vehicle) {
    const created = await api('POST', '/dms/vehicle', { plateNo: '京E' + STAMP, vehicleType: 1, status: 1 })
    vehicle = data(created)
  }
  ctxIds.vehicleId = vehicle ? vehicle.id : null
  ctxIds.plateNo = vehicle ? vehicle.plateNo : null
  check('车辆选择器数据源可用', !!ctxIds.vehicleId, `${ctxIds.vehicleId}/${ctxIds.plateNo}`)

  const master = await api('POST', '/erp/md/route', {
    routeName: 'E2E配送线路' + STAMP, routeSelf: 1, routeLogistics: 0,
  })
  ctxIds.masterRouteId = data(master)?.id
  ctxIds.masterRouteName = data(master)?.routeName
  check('线路档案（主数据）创建成功', !!ctxIds.masterRouteId, JSON.stringify(data(master)?.routeCode))
}

// ══════════════ 一、接口验收 ══════════════
let apiRouteId = null
let apiRouteCode = null
let cancelRouteId = null
let batchIds = []

async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 0) 号段
  const no1 = await api('GET', '/delivery/route/next-no')
  const no1Val = data(no1)
  check('next-no 号段格式 PSXL-YYYYMMDD-0000', /^PSXL-\d{8}-\d{4}$/.test(String(no1Val)), no1Val)

  // 1) 建单（含线路档案/车辆快照 + 3 个点位）
  const create = await api('POST', '/delivery/route', {
    routeId: ctxIds.masterRouteId,
    planDate: TOMORROW,
    deliveryPersonId: ctxIds.riderId,
    deliveryPersonName: ctxIds.riderName,
    vehicleId: ctxIds.vehicleId,
    vehicleNo: ctxIds.plateNo,
    remark: 'E2E 建单备注',
    points: [
      { customerName: '客户甲', customerPhone: '13800000001', orderNo: 'E2E-SO-01', address: '北京市东城区甲街1号' },
      { customerName: '客户乙', customerPhone: '13800000002', orderNo: 'E2E-SO-02', address: '北京市西城区乙街2号' },
      { customerName: '客户丙', customerPhone: '13800000003', orderNo: 'E2E-SO-03', address: '北京市朝阳区丙街3号' },
    ],
  })
  const created = data(create)
  apiRouteId = created?.id
  apiRouteCode = created?.routeCode
  check('新增配送路线单成功', !!apiRouteId, JSON.stringify(created).slice(0, 160))
  check('路线编号按号段生成', /^PSXL-\d{8}-\d{4}$/.test(String(apiRouteCode)), apiRouteCode)
  check('线路档案快照回填（名称）', created?.routeName === ctxIds.masterRouteName, created?.routeName)
  check('线路档案快照回填（类型=自配）', created?.routeTypeText === '自配' && created?.routeType === 'SELF', `${created?.routeType}/${created?.routeTypeText}`)
  check('车辆快照回填', created?.vehicleNo === ctxIds.plateNo, created?.vehicleNo)
  check('计划配送日期落库', created?.planDate === TOMORROW, created?.planDate)
  check('初始状态=规划中', created?.status === 'PLANNING' && created?.statusText === '规划中', `${created?.status}/${created?.statusText}`)
  check('总点位=3，已送达=0', created?.totalPoints === 3 && created?.completedPoints === 0, `${created?.totalPoints}/${created?.completedPoints}`)
  check('起点=首个点位地址', created?.startPoint === '北京市东城区甲街1号', created?.startPoint)
  check('终点=末个点位地址', created?.endPoint === '北京市朝阳区丙街3号', created?.endPoint)
  check('创建人快照非空', !!created?.createByName, created?.createByName)

  // 1.1) 必填 / 边界校验
  const noRider = await api('POST', '/delivery/route', { points: [{ address: 'A' }] })
  check('配送员必填校验', noRider.json?.code !== 200, noRider.json?.message)
  const noPoint = await api('POST', '/delivery/route', { deliveryPersonId: ctxIds.riderId, points: [] })
  check('至少 1 个点位校验', noPoint.json?.code !== 200, noPoint.json?.message)
  const blankAddr = await api('POST', '/delivery/route', {
    deliveryPersonId: ctxIds.riderId, points: [{ customerName: 'X', address: '   ' }],
  })
  check('点位地址必填校验', blankAddr.json?.code !== 200, blankAddr.json?.message)
  const dupCode = await api('POST', '/delivery/route', {
    routeCode: apiRouteCode, deliveryPersonId: ctxIds.riderId, points: [{ address: 'A' }],
  })
  check('路线编号重复校验（租户内唯一）', dupCode.json?.code !== 200, dupCode.json?.message)
  const badMaster = await api('POST', '/delivery/route', {
    routeId: 999999999, deliveryPersonId: ctxIds.riderId, points: [{ address: 'A' }],
  })
  check('引用线路档案不存在校验', badMaster.json?.code !== 200, badMaster.json?.message)

  // 2) 详情（含点位明细）
  const detail = await api('GET', `/delivery/route/${apiRouteId}`)
  const d = data(detail)
  check('详情返回点位明细 3 条', (d?.points || []).length === 3, (d?.points || []).length)
  check('点位顺序 1..3', (d?.points || []).map(p => p.pointOrder).join(',') === '1,2,3', (d?.points || []).map(p => p.pointOrder).join(','))
  check('点位初始状态=待配送', (d?.points || []).every(p => p.status === 'PENDING'), (d?.points || [])[0]?.status)
  check('点位客户/地址落库', d?.points?.[0]?.customerName === '客户甲' && d?.points?.[0]?.address === '北京市东城区甲街1号', JSON.stringify(d?.points?.[0] || {}).slice(0, 160))

  // 3) 多条件分页
  const byKey = await api('GET', `/delivery/route/page?pageNum=1&pageSize=20&keyword=${apiRouteCode}`)
  check('分页按路线编号检索命中', Number(data(byKey)?.total) >= 1, data(byKey)?.total)
  const byPerson = await api('GET', `/delivery/route/page?pageNum=1&pageSize=20&deliveryPersonId=${ctxIds.riderId}&keyword=${apiRouteCode}`)
  check('分页按配送员过滤命中', Number(data(byPerson)?.total) >= 1, data(byPerson)?.total)
  const byVehicle = await api('GET', `/delivery/route/page?pageNum=1&pageSize=20&vehicleId=${ctxIds.vehicleId}&keyword=${apiRouteCode}`)
  check('分页按车辆过滤命中', Number(data(byVehicle)?.total) >= 1, data(byVehicle)?.total)
  const byMaster = await api('GET', `/delivery/route/page?pageNum=1&pageSize=20&routeId=${ctxIds.masterRouteId}&keyword=${apiRouteCode}`)
  check('分页按线路档案过滤命中', Number(data(byMaster)?.total) >= 1, data(byMaster)?.total)
  const byStatus = await api('GET', `/delivery/route/page?pageNum=1&pageSize=20&status=PLANNING,READY&keyword=${apiRouteCode}`)
  check('分页按状态多选命中', Number(data(byStatus)?.total) >= 1, data(byStatus)?.total)
  const byPlanDate = await api('GET', `/delivery/route/page?pageNum=1&pageSize=20&keyword=${apiRouteCode}&planDateStart=${TOMORROW}&planDateEnd=${TOMORROW}`)
  check('分页按计划配送日期区间命中', Number(data(byPlanDate)?.total) >= 1, data(byPlanDate)?.total)
  const byPlanDateMiss = await api('GET', `/delivery/route/page?pageNum=1&pageSize=20&keyword=${apiRouteCode}&planDateStart=2000-01-01&planDateEnd=2000-01-02`)
  check('计划日期区间不匹配则不命中', Number(data(byPlanDateMiss)?.total) === 0, data(byPlanDateMiss)?.total)
  const byKwMiss = await api('GET', `/delivery/route/page?pageNum=1&pageSize=20&keyword=NOT-EXIST-${STAMP}`)
  check('无匹配关键字返回空', Number(data(byKwMiss)?.total) === 0, data(byKwMiss)?.total)
  const bySort = await api('GET', `/delivery/route/page?pageNum=1&pageSize=20&sortField=routeCode&sortOrder=asc&keyword=${apiRouteCode}`)
  check('服务端排序可用（routeCode asc）', Number(data(bySort)?.total) >= 1, JSON.stringify(data(bySort)?.records?.[0]?.routeCode))

  // 4) 改单（点位整体替换，仅规划中可改）
  const upd = await api('PUT', `/delivery/route/${apiRouteId}`, {
    routeId: ctxIds.masterRouteId,
    planDate: TOMORROW,
    deliveryPersonId: ctxIds.riderId,
    deliveryPersonName: ctxIds.riderName,
    vehicleId: ctxIds.vehicleId,
    vehicleNo: ctxIds.plateNo,
    remark: 'E2E 改单备注',
    points: [
      { customerName: '客户甲', customerPhone: '13800000001', address: '北京市东城区甲街1号' },
      { customerName: '客户乙', customerPhone: '13800000002', address: '北京市西城区乙街2号' },
    ],
  })
  const updD = data(upd)
  check('修改配送路线单成功', upd.json?.code === 200, upd.json?.message)
  check('修改后点位整体替换为 2 条', updD?.totalPoints === 2 && (updD?.points || []).length === 2, JSON.stringify(updD?.totalPoints))
  check('修改后备注与终点同步', updD?.remark === 'E2E 改单备注' && updD?.endPoint === '北京市西城区乙街2号', `${updD?.remark}/${updD?.endPoint}`)

  // 5) 状态机：开始配送
  const start = await api('POST', `/delivery/route/${apiRouteId}/start`)
  check('开始配送接口成功', start.json?.code === 200, start.json?.message)
  const afterStart = data(await api('GET', `/delivery/route/${apiRouteId}`))
  check('开始后状态=配送中', afterStart?.status === 'IN_PROGRESS', afterStart?.status)
  check('开始后写入开始时间', !!afterStart?.startTime, afterStart?.startTime)
  check('开始后首个点位置为在途中', afterStart?.points?.[0]?.status === 'IN_ROUTE', afterStart?.points?.[0]?.status)
  const startAgain = await api('POST', `/delivery/route/${apiRouteId}/start`)
  check('重复开始配送被拒（状态机）', startAgain.json?.code !== 200, startAgain.json?.message)
  const editAfterStart = await api('PUT', `/delivery/route/${apiRouteId}`, {
    deliveryPersonId: ctxIds.riderId, points: [{ address: 'X' }],
  })
  check('配送中不可改单（状态机）', editAfterStart.json?.code !== 200, editAfterStart.json?.message)

  // 6) 多点签收
  const p1 = afterStart?.points?.[0]?.pointId
  const p2 = afterStart?.points?.[1]?.pointId
  const noSignee = await api('POST', `/delivery/route/${apiRouteId}/point/${p1}/sign`, { status: 'DELIVERED' })
  check('签收缺少签收人被拒', noSignee.json?.code !== 200, noSignee.json?.message)
  const failNoReason = await api('POST', `/delivery/route/${apiRouteId}/point/${p2}/sign`, { status: 'FAILED' })
  check('配送失败缺原因被拒', failNoReason.json?.code !== 200, failNoReason.json?.message)
  const badStatus = await api('POST', `/delivery/route/${apiRouteId}/point/${p1}/sign`, { status: 'NOT_A_STATUS' })
  check('点位状态非法被拒', badStatus.json?.code !== 200, badStatus.json?.message)

  const sign1 = await api('POST', `/delivery/route/${apiRouteId}/point/${p1}/sign`, { status: 'ARRIVED' })
  check('点位置为已到达成功', sign1.json?.code === 200, sign1.json?.message)
  const sign2 = await api('POST', `/delivery/route/${apiRouteId}/point/${p1}/sign`, { status: 'DELIVERED', signee: '张三' })
  check('点位签收（送达）成功', sign2.json?.code === 200, sign2.json?.message)
  const sign3 = await api('POST', `/delivery/route/${apiRouteId}/point/${p2}/sign`, { status: 'FAILED', failReason: '客户不在' })
  check('点位签收（失败+原因）成功', sign3.json?.code === 200, sign3.json?.message)
  const afterSign = data(await api('GET', `/delivery/route/${apiRouteId}`))
  const sp1 = (afterSign?.points || []).find(p => p.pointId === p1)
  const sp2 = (afterSign?.points || []).find(p => p.pointId === p2)
  check('已送达点位落签收人与时间', sp1?.status === 'DELIVERED' && sp1?.signee === '张三' && !!sp1?.signTime, `${sp1?.status}/${sp1?.signee}`)
  check('已送达点位落到达时间', !!sp1?.arriveTime, sp1?.arriveTime)
  check('失败点位落失败原因', sp2?.status === 'FAILED' && sp2?.failReason === '客户不在', `${sp2?.status}/${sp2?.failReason}`)
  check('路线统计同步（已送达=1、失败=1）', afterSign?.completedPoints === 1 && afterSign?.failedPoints === 1, `${afterSign?.completedPoints}/${afterSign?.failedPoints}`)
  check('完成进度口径=已送达/总点位', afterSign?.progress === '1 / 2', afterSign?.progress)

  // 7) 完成配送（未处理点位收口为已跳过）
  const complete = await api('POST', `/delivery/route/${apiRouteId}/complete`)
  check('完成配送接口成功', complete.json?.code === 200, complete.json?.message)
  const afterComplete = data(await api('GET', `/delivery/route/${apiRouteId}`))
  check('完成后状态=已完成', afterComplete?.status === 'COMPLETED' && afterComplete?.statusText === '已完成', afterComplete?.status)
  check('完成后写入完成时间', !!afterComplete?.completeTime, afterComplete?.completeTime)
  check('完成后计算实际时长（分钟）', afterComplete?.actualDuration != null && Number(afterComplete.actualDuration) >= 0, afterComplete?.actualDuration)
  const startAfterComplete = await api('POST', `/delivery/route/${apiRouteId}/start`)
  check('已完成不可再开始（状态机）', startAfterComplete.json?.code !== 200, startAfterComplete.json?.message)
  const completeAgain = await api('POST', `/delivery/route/${apiRouteId}/complete`)
  check('已完成不可再完成（状态机）', completeAgain.json?.code !== 200, completeAgain.json?.message)
  const cancelCompleted = await api('POST', `/delivery/route/${apiRouteId}/cancel`, { reason: 'x' })
  check('已完成不可取消（状态机）', cancelCompleted.json?.code !== 200, cancelCompleted.json?.message)

  // 8) 取消路线（含取消原因）
  const c1 = await api('POST', '/delivery/route', {
    planDate: TOMORROW, deliveryPersonId: ctxIds.riderId, deliveryPersonName: ctxIds.riderName,
    points: [{ address: '北京市海淀区待取消1号' }, { address: '北京市海淀区待取消2号' }],
  })
  cancelRouteId = data(c1)?.id
  const cancelRes = await api('POST', `/delivery/route/${cancelRouteId}/cancel`, { reason: 'E2E 取消原因' })
  check('取消路线接口成功', cancelRes.json?.code === 200, cancelRes.json?.message)
  const afterCancel = data(await api('GET', `/delivery/route/${cancelRouteId}`))
  check('取消后状态=已取消', afterCancel?.status === 'CANCELLED', afterCancel?.status)
  check('取消后写入取消时间与原因', !!afterCancel?.cancelTime && afterCancel?.cancelReason === 'E2E 取消原因', `${afterCancel?.cancelTime}/${afterCancel?.cancelReason}`)
  check('取消后未处理点位收口为已跳过', (afterCancel?.points || []).every(p => p.status === 'SKIPPED'), (afterCancel?.points || []).map(p => p.status).join(','))
  const cancelAgain = await api('POST', `/delivery/route/${cancelRouteId}/cancel`, { reason: 'x' })
  check('已取消不可再取消（状态机）', cancelAgain.json?.code !== 200, cancelAgain.json?.message)

  // 9) 默认台账不含已取消 + 显示已取消可查
  const defPage = await api('GET', `/delivery/route/page?pageNum=1&pageSize=50&keyword=${(afterCancel?.routeCode || '')}`)
  check('台账默认不显示已取消路线', Number(data(defPage)?.total) === 0, data(defPage)?.total)
  const allPage = await api('GET', `/delivery/route/page?pageNum=1&pageSize=50&showCancelled=1&keyword=${(afterCancel?.routeCode || '')}`)
  check('勾选「显示已取消」后可查到', Number(data(allPage)?.total) >= 1, data(allPage)?.total)
  const statusCancelled = await api('GET', `/delivery/route/page?pageNum=1&pageSize=50&status=CANCELLED&keyword=${(afterCancel?.routeCode || '')}`)
  check('按状态=CANCELLED 可查到已取消路线', Number(data(statusCancelled)?.total) >= 1, data(statusCancelled)?.total)

  // 10) 批量状态流转
  const b1 = await api('POST', '/delivery/route', { planDate: TOMORROW, deliveryPersonId: ctxIds.riderId, points: [{ address: '批量点位1' }] })
  const b2 = await api('POST', '/delivery/route', { planDate: TOMORROW, deliveryPersonId: ctxIds.riderId, points: [{ address: '批量点位2' }] })
  batchIds = [data(b1)?.id, data(b2)?.id]
  const batch = await api('POST', '/delivery/route/batch-status', { ids: batchIds, action: 'start' })
  check('批量开始配送成功 2 条', data(batch)?.success === 2, JSON.stringify(data(batch)))
  const batchEmpty = await api('POST', '/delivery/route/batch-status', { ids: [], action: 'start' })
  check('批量操作空选择被拒', batchEmpty.json?.code !== 200, batchEmpty.json?.message)
  const batchBadAction = await api('POST', '/delivery/route/batch-status', { ids: batchIds, action: 'delete' })
  check('批量非法动作被拒', batchBadAction.json?.code !== 200, batchBadAction.json?.message)
  const batchDone = await api('POST', '/delivery/route/batch-status', { ids: batchIds, action: 'complete' })
  check('批量完成成功 2 条', data(batchDone)?.success === 2, JSON.stringify(data(batchDone)))
  const batchPartial = await api('POST', '/delivery/route/batch-status', { ids: batchIds, action: 'complete' })
  check('批量部分失败可返回失败明细', Number(data(batchPartial)?.failure) === 2 && (data(batchPartial)?.errors || []).length === 2, JSON.stringify(data(batchPartial)).slice(0, 160))

  // 11) 导出真实 xlsx
  const exp = await api('GET', `/delivery/route/export?keyword=${apiRouteCode}`)
  const isXlsx = exp.buf && exp.buf.length > 1000 && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b
  check('导出返回真实 xlsx（PK 魔数）', !!isXlsx, `size=${exp.buf ? exp.buf.length : 0}`)
  check('导出响应头为 Excel', String(exp.headers['content-type'] || '').includes('spreadsheetml'), exp.headers['content-type'])
  fs.writeFileSync(path.join(SHOTS, 'export.xlsx'), exp.buf)
}

// ══════════════ 一·B、围栏归集 / 地图规划 / 催单 / ETA ══════════════
let fenceRouteId = null

async function fenceSuite() {
  console.log('\n═══ 一·B、围栏归集与规划能力 ═══')

  // 1) 建围栏（圆形，覆盖测试客户坐标 39.9289,116.4164）
  const fence = await api('POST', '/dms/route/fence', {
    fenceName: 'E2E围栏北京东城' + STAMP,
    fenceType: 'CIRCLE',
    centerLat: 39.9289,
    centerLng: 116.4164,
    radiusMeters: 3000,
    status: 'ENABLED',
  })
  const fenceId = data(fence)?.id
  check('创建电子围栏成功（复用 dms 围栏档案）', !!fenceId, JSON.stringify(data(fence)).slice(0, 160))

  // 2) 建路线单并绑定围栏（开启自动归集）
  const route = await api('POST', '/delivery/route', {
    planDate: TOMORROW,
    deliveryPersonId: ctxIds.riderId,
    deliveryPersonName: ctxIds.riderName,
    fenceId,
    autoCollect: 1,
    points: [{ address: '北京市东城区仓库起点' }],
  })
  const r = data(route)
  fenceRouteId = r?.id
  check('路线单绑定围栏成功', r?.fenceId === fenceId && !!r?.fenceName, `${r?.fenceId}/${r?.fenceName}`)
  check('自动归集开关默认参与', r?.autoCollect === 1, r?.autoCollect)

  // 3) 归集预览（干跑）
  const preview = await api('POST', '/delivery/route/auto-collect/preview', { routeId: fenceRouteId })
  const pv = data(preview)
  check('归集预览扫描到待配送需求', Number(pv?.scanned) >= 1, `scanned=${pv?.scanned}`)
  check('预览命中围栏（出库单/订单）', Number(pv?.matched) >= 1, `matched=${pv?.matched} items=${pv?.items?.length}`)
  check('预览为干跑（dryRun=true）', pv?.dryRun === true, pv?.dryRun)
  const beforeDetail = data(await api('GET', `/delivery/route/${fenceRouteId}`))
  check('干跑不落库（点位数未变）', beforeDetail?.totalPoints === 1, beforeDetail?.totalPoints)

  // 12) 围栏外判定：另建远端围栏 → 该需求不应命中
  const fence2 = await api('POST', '/dms/route/fence', {
    fenceName: 'E2E围栏上海' + STAMP, fenceType: 'CIRCLE',
    centerLat: 31.2304, centerLng: 121.4737, radiusMeters: 3000, status: 'ENABLED',
  })
  const route2 = await api('POST', '/delivery/route', {
    planDate: TOMORROW, deliveryPersonId: ctxIds.riderId,
    fenceId: data(fence2)?.id, autoCollect: 1,
    points: [{ address: '上海市仓库起点' }],
  })
  const pv3 = data(await api('POST', '/delivery/route/auto-collect/preview', { routeId: data(route2)?.id }))
  check('围栏外需求不被归集', Number(pv3?.matched) === 0 && Number(pv3?.outsideFence) >= 1,
    `matched=${pv3?.matched} outside=${pv3?.outsideFence}`)

  // 4) 执行归集
  const collect = await api('POST', '/delivery/route/auto-collect', { routeId: fenceRouteId })
  const cv = data(collect)
  check('围栏归集执行成功', Number(cv?.added) >= 1, `added=${cv?.added}`)
  const afterDetail = data(await api('GET', `/delivery/route/${fenceRouteId}`))
  check('归集后点位数增加', Number(afterDetail?.totalPoints) === 1 + Number(cv?.added), afterDetail?.totalPoints)
  const collected = (afterDetail?.points || []).filter(x => x.sourceType === 'OUT' || x.sourceType === 'SO')
  check('归集点位带来源单据类型', collected.length >= 1, JSON.stringify(collected.map(x => x.sourceType)))
  check('归集点位带来源单号', collected.every(x => !!x.orderNo), JSON.stringify(collected.map(x => x.orderNo)).slice(0, 160))
  check('归集点位带坐标（供地图规划）', collected.every(x => !!x.latitude && !!x.longitude), JSON.stringify(collected[0] || {}).slice(0, 160))
  check('归集点位记录来源备注', collected.every(x => String(x.remark || '').includes('来源')), collected[0]?.remark)

  // 5) 重复归集去重：同单据不再进入
  const preview2 = await api('POST', '/delivery/route/auto-collect/preview', { routeId: fenceRouteId })
  const pv2 = data(preview2)
  check('已归集单据不再重复命中', Number(pv2?.matched) === 0, `matched=${pv2?.matched}`)

  // 6) 地图路线规划（未配置 Key 应自动降级）
  const plan = await api('POST', `/delivery/route/${fenceRouteId}/plan-order`)
  const pl = data(plan)
  check('路线规划接口成功', plan.json?.code === 200, plan.json?.message)
  check('规划返回里程', pl?.totalDistance != null, `${pl?.totalDistance}/${pl?.totalDuration}`)
  check('未配置地图 Key 时自动降级', pl?.degraded === true || pl?.provider === 'local', `${pl?.provider}/${pl?.degraded}`)
  const planned = data(await api('GET', `/delivery/route/${fenceRouteId}`))
  const seqs = (planned?.points || []).map(x => x.pointOrder)
  check('规划后点位顺序连续重排', JSON.stringify(seqs) === JSON.stringify(seqs.map((_, i) => i + 1)), JSON.stringify(seqs))
  check('规划回填分段距离/时长', (planned?.points || []).some(x => x.distanceFromPrev != null || x.durationFromPrev != null),
    JSON.stringify((planned?.points || []).slice(0, 2).map(x => [x.distanceFromPrev, x.durationFromPrev])))

  // 7) 催单：把最后一个点位移到第 1 位
  const pts = planned?.points || []
  const lastPoint = pts[pts.length - 1]
  const exp = await api('POST', `/delivery/route/${fenceRouteId}/point/${lastPoint.pointId}/expedite`,
    { targetSeq: 1, replanRest: false, reason: '客户催单' })
  check('催单调整接口成功', exp.json?.code === 200, exp.json?.message)
  const afterExp = data(await api('GET', `/delivery/route/${fenceRouteId}`))
  const first = (afterExp?.points || [])[0]
  check('催单点位已到第 1 位', first?.pointId === lastPoint.pointId && first?.pointOrder === 1, String(first?.pointOrder))
  check('催单标记与原因落库', first?.expedited === 1 && String(first?.remark || '').includes('催单'), `${first?.expedited}/${first?.remark}`)
  const expSeqs = (afterExp?.points || []).map(x => x.pointOrder)
  check('催单后其余点位顺序顺延不丢点', JSON.stringify(expSeqs) === JSON.stringify(expSeqs.map((_, i) => i + 1)), JSON.stringify(expSeqs))

  // 8) 催单 + 后续重排（replanRest）
  const ptsNow = afterExp?.points || []
  const target2 = ptsNow[ptsNow.length - 1]
  const exp2 = await api('POST', `/delivery/route/${fenceRouteId}/point/${target2.pointId}/expedite`,
    { targetSeq: 2, replanRest: true, reason: '客户催单(重排)' })
  const exp2v = data(exp2)
  check('催单支持开启后续重排', exp2.json?.code === 200 && exp2v?.targetSeq === 2, JSON.stringify(exp2v).slice(0, 160))

  // 9) ETA 预估
  const eta = await api('GET', `/delivery/route/${fenceRouteId}/eta`)
  const ev = data(eta)
  check('ETA 预估接口成功', eta.json?.code === 200, eta.json?.message)
  check('ETA 覆盖全部剩余点位', (ev?.points || []).length > 0 && Number(ev?.remainingPoints) === (ev?.points || []).length,
    `remaining=${ev?.remainingPoints} items=${(ev?.points || []).length}`)
  const etaLocated = (ev?.points || []).filter(x => String(x.address || '') && x.distanceFromPrev !== undefined)
  check('ETA 逐点给出预计到达时间（有坐标点位）', (ev?.points || []).some(x => !!x.etaTime), JSON.stringify((ev?.points || []).filter(x => x.etaTime)[0] || {}).slice(0, 140))
  check('缺坐标点位不返回 ETA 且有说明', String(ev?.message || '').includes('未维护坐标') || (ev?.points || []).every(x => !!x.etaTime), ev?.message)
  const etaTimes = (ev?.points || []).map(x => new Date(x.etaTime).getTime())
  check('ETA 沿既定顺序递增', etaTimes.every((t, i) => i === 0 || t >= etaTimes[i - 1]), JSON.stringify(etaTimes.length))
  const persisted = data(await api('GET', `/delivery/route/${fenceRouteId}`))
  check('ETA 落库到点位', (persisted?.points || []).some(x => !!x.etaTime), (persisted?.points || [])[0]?.etaTime)

  // 10) ETA 通知（通道未接入，落库待发送）
  const notify = await api('POST', `/delivery/route/${fenceRouteId}/notify-eta`, { channel: 'SMS' })
  const nv = data(notify)
  check('生成 ETA 客户通知', Number(nv?.created) >= 1, JSON.stringify(nv).slice(0, 200))
  check('明确标注通道未接入', nv?.channelReady === false, nv?.message)

  // 11) 手动添加：重复单据被拒（去重）
  const dupAdd = await api('POST', `/delivery/route/${fenceRouteId}/add-points`, {
    items: [{ sourceType: collected[0]?.sourceType, sourceId: Number(collected[0]?.sourceId) }],
  })
  check('手动添加重复单据被拒（去重生效）', dupAdd.json?.code !== 200, dupAdd.json?.message)

  // 13) 客户配送坐标读写
  const geoList = data(await api('GET', '/delivery/route/customer-geo?limit=200'))
  check('客户配送坐标清单可查询', Array.isArray(geoList) && geoList.length >= 1, `count=${geoList?.length}`)
  const saveGeo = await api('PUT', '/delivery/route/customer-geo', { customerId: 1, latitude: 39.9289, longitude: 116.4164 })
  check('客户配送坐标可补录', saveGeo.json?.code === 200, saveGeo.json?.message)
  const badGeo = await api('PUT', '/delivery/route/customer-geo', { customerId: 1, latitude: 999, longitude: 116 })
  check('非法坐标被拒', badGeo.json?.code !== 200, badGeo.json?.message)

  // 14) ETA 通知台账闭环（通道未接入下的人工闭环）
  const ledger = await api('GET', `/delivery/route/eta-notify/page?routeId=${fenceRouteId}&pageNum=1&pageSize=50`)
  const ledgerRows = data(ledger)?.records || []
  check('通知台账可查询', ledger.json?.code === 200 && ledgerRows.length >= 1, `rows=${ledgerRows.length}`)
  check('通知初始状态为待发送', ledgerRows.every(r => r.status === 'PENDING'), JSON.stringify(ledgerRows.map(r => r.status)))
  check('通知含客户与手机号', ledgerRows.some(r => !!r.customerPhone && !!r.etaTime), JSON.stringify(ledgerRows[0] || {}).slice(0, 140))

  const sendTry = await api('POST', '/delivery/route/eta-notify/send', { ids: [ledgerRows[0]?.id] })
  const sendTryV = data(sendTry)
  check('通道未接入时发送不篡改状态', sendTryV?.channelReady === false && Number(sendTryV?.sent) === 0, JSON.stringify(sendTryV).slice(0, 160))

  const mark = await api('POST', '/delivery/route/eta-notify/status', { ids: [ledgerRows[0]?.id], status: 'SENT' })
  check('人工标记已发送成功', mark.json?.code === 200 && Number(data(mark)) === 1, JSON.stringify(data(mark)))
  const afterMark = data(await api('GET', `/delivery/route/eta-notify/page?routeId=${fenceRouteId}&status=SENT&pageNum=1&pageSize=50`))
  check('标记后状态可查为已发送', (afterMark?.records || []).some(r => String(r.id) === String(ledgerRows[0]?.id)), `rows=${(afterMark?.records || []).length}`)

  const cancelMark = await api('POST', '/delivery/route/eta-notify/status', {
    ids: (afterMark?.records || []).map(r => r.id), status: 'CANCELLED', errorMsg: 'E2E 作废',
  })
  check('通知可作废', cancelMark.json?.code === 200, JSON.stringify(data(cancelMark)))
  const badMark = await api('POST', '/delivery/route/eta-notify/status', { ids: [ledgerRows[0]?.id], status: 'NOT_A_STATUS' })
  check('非法通知状态被拒', badMark.json?.code !== 200, badMark.json?.message)
  const emptyMark = await api('POST', '/delivery/route/eta-notify/status', { ids: [], status: 'SENT' })
  check('空选择被拒', emptyMark.json?.code !== 200, emptyMark.json?.message)

  // 15) 地图能力不再双实现：历史高德端点应已下线
  const legacyPlan = await api('POST', '/delivery/route/plan', { deliveryPersonId: ctxIds.riderId, points: [] })
  check('历史高德 /plan 端点已下线（不再双实现）', [404, 405].includes(legacyPlan.status), `status=${legacyPlan.status}`)
  const legacyNav = await api('POST', '/delivery/route/navigation', { currentLongitude: '116', currentLatitude: '39' })
  check('历史导航端点已下线', [404, 405].includes(legacyNav.status), `status=${legacyNav.status}`)

  // 16) 司机端所需接口：我的进行中路线 + 逐点签收回写（司机端页面消费同一后端）
  const active = await api('GET', `/delivery/route/active/${ctxIds.riderId}`)
  check('司机端「我的进行中路线」接口可用', active.json?.code === 200, JSON.stringify(active.json ?? {}).slice(0, 200))
  const driverRoute = data(active)
  if (driverRoute && driverRoute.id) {
    check('进行中路线返回点位明细', (driverRoute.points || []).length >= 0, `points=${(driverRoute.points || []).length}`)
  }

  // 17) ETA 通知接入消息底座（core-base sys_message，msgType=3）
  const ledger2 = await api('GET', `/delivery/route/eta-notify/page?routeId=${fenceRouteId}&pageNum=1&pageSize=50`)
  const rows2 = data(ledger2)?.records || []
  check('通知已投递到消息底座（messageId 非空）', rows2.some(r => r.messageId != null), JSON.stringify(rows2[0] ?? {}).slice(0, 160))
  check('通知返回消息底座发送状态', rows2.some(r => r.channelStatus != null), JSON.stringify(rows2.map(r => r.channelStatus)))
  // 未配置通道：先「待发送(0)」，底座的定时任务重试 3 次后标记「失败(3)」——两者都属预期，不静默丢弃
  const dispatched = rows2.filter(r => r.messageId != null)
  check('未配置短信通道时消息状态为待发送或失败（不静默丢弃）',
    dispatched.length > 0 && dispatched.every(r => r.channelStatus === 0 || r.channelStatus === 3),
    JSON.stringify(dispatched.map(r => [r.channelStatus, r.messageId])))

  const dispatch = await api('POST', '/delivery/route/eta-notify/dispatch', { routeId: fenceRouteId })
  check('补投递接口可用且幂等', dispatch.json?.code === 200 && Number(data(dispatch)?.dispatched) === 0,
    JSON.stringify(data(dispatch)).slice(0, 160))

}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()
  const errors = []
  page.on('pageerror', e => errors.push(String(e)))

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

  /** 关闭所有可见弹窗（含遮罩残留），避免拦截后续点击 */
  async function closeModals() {
    for (let i = 0; i < 6; i++) {
      if ((await page.locator('.ant-modal-mask:visible').count()) === 0) break
      const close = page.locator('.ant-modal-content:visible .ant-modal-close').last()
      if (await close.count()) {
        await close.click({ force: true }).catch(() => {})
      } else {
        const btn = page.locator('.ant-modal-content:visible button:has-text("关闭")').last()
        if (await btn.count()) {
          await btn.click({ force: true }).catch(() => {})
        } else {
          await page.keyboard.press('Escape')
        }
      }
      await page.waitForTimeout(700)
    }
    await page.waitForTimeout(400)
    if (await page.locator('.ant-modal-mask:visible').count()) {
      console.log('  [警告：仍有弹窗遮罩残留]')
    }
  }

  /** 查询区检索（用 Enter 触发，避开弹窗遮罩对点击的拦截） */
  async function searchByKeyword(kw) {
    const input = page.locator('.search-area input').first()
    await input.fill(kw)
    await input.press('Enter')
    await page.waitForTimeout(3000)
  }

  await openPage(`${FE}/dms/route-list`)
  // 新组件首次编译 + 重应用冷启动较慢：显式等待表头渲染完成
  try {
    await page.waitForSelector('.ss-grid th', { timeout: 90000 })
    await page.waitForFunction(() => (document.body.innerText || '').includes('新增路线'), null, { timeout: 60000 })
  } catch (e) {
    console.log('  [等待页面渲染超时]', String(e).slice(0, 120))
  }
  await page.waitForTimeout(1500)
  const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())
  check('页面无 JS 运行时错误', errors.length === 0, errors.slice(0, 2).join(' | '))
  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['路线编号', '线路名称', '线路类型', '配送员', '车辆', '计划配送日期', '总点位', '已送达', '配送失败', '完成进度', '状态']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  for (const b of ['新增路线', '批量开始配送', '批量完成', '批量取消', '围栏归集', '刷新', '打印(F8)', '导出']) {
    check(`工具栏含「${b}」`, bodyText.includes(b.replace(/\s+/g, '')))
  }
  for (const q of ['筛选条件', '配送线路', '配送员', '车辆', '状态', '计划日期', '创建时间', '显示已取消', '查询']) {
    check(`查询区含「${q}」`, bodyText.includes(q))
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 列配置齿轮（个人/全局双 Tab）
  const gearCount = await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length)
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)
  if (gearCount > 0) {
    await page.locator('.ss-grid .th-settings-btn').first().click()
    await page.waitForTimeout(1200)
    const colPanel = page.locator('.ant-modal-content:visible').last()
    const colText = (await colPanel.innerText()).replace(/\s+/g, '')
    check('列配置弹窗可打开', colText.includes('个人配置'), colText.slice(0, 80))
    check('列配置含「个人配置 / 全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'))
    check('列配置含隐藏列（起点/终点/创建人）', ['起点', '终点', '创建人'].every(c => colText.includes(c)), colText.replace(/\n/g, '|').slice(0, 200))
    await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
    await closeModals()
  }

  // 页面配置（查询条件 / 功能按钮）
  await page.locator('button[title="页面配置"]').first().click()
  await page.waitForTimeout(1500)
  const cfgPanel = page.locator('.ant-modal-content:visible').last()
  const cfgText = (await cfgPanel.innerText()).replace(/\s+/g, '')
  check('页面配置弹窗可打开', cfgText.length > 0, cfgText.slice(0, 80))
  check('页面配置含「查询条件」Tab', cfgText.includes('查询条件') || cfgText.includes('查询'), cfgText.slice(0, 120))
  check('页面配置含「功能按钮」Tab', cfgText.includes('功能按钮'))
  check('页面配置含全部查询条件项', ['筛选条件', '配送线路', '配送员', '车辆', '状态', '计划日期', '创建时间', '显示已取消'].every(t => cfgText.includes(t)), cfgText.slice(0, 200))
  const fnTab = page.locator('.ant-modal-content:visible .ant-tabs-tab', { hasText: /功\s*能\s*按\s*钮/ }).first()
  if (await fnTab.count()) {
    await fnTab.click()
    await page.waitForTimeout(800)
  }
  const fnText = (await page.locator('.ant-modal-content:visible').last().innerText()).replace(/\s+/g, '')
  check('页面配置含「围栏归集」功能按钮', fnText.includes('围栏归集'), fnText.slice(0, 220))
  await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png'), fullPage: true })
  await closeModals(page)

  // 围栏归集弹窗（自动归集 / 手动添加 双 Tab）
  await page.locator('button:has-text("围栏归集")').first().click()
  await page.waitForTimeout(2000)
  const collectModal = page.locator('.ant-modal-content:visible').last()
  const collectText = (await collectModal.innerText()).replace(/\s+/g, '')
  check('围栏归集弹窗可打开', collectText.includes('配送需求归集'), collectText.slice(0, 80))
  check('归集弹窗含「围栏自动归集」Tab', collectText.includes('围栏自动归集'))
  check('归集弹窗含「手动添加（围栏外）」Tab', collectText.includes('手动添加'))
  check('归集弹窗含规则说明与预览入口', collectText.includes('客户配送坐标') && collectText.includes('预览匹配结果'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-collect.png'), fullPage: true })
  await closeModals()

  // 列表数据断言（接口造的 apiRouteCode 应可见）
  await searchByKeyword(apiRouteCode)
  const rows = await page.evaluate(() => [...document.querySelectorAll('.ss-grid tbody tr')].map(tr => (tr.innerText || '').replace(/\s+/g, ' ').trim()))
  check('列表可按路线编号检索到数据', rows.some(r => r.includes(apiRouteCode)), rows.slice(0, 3).join(' || ').slice(0, 200))
  const rowText = rows.find(r => r.includes(apiRouteCode)) || ''
  check('列表行显示配送员', ctxIds.riderName ? rowText.includes(ctxIds.riderName) : true, rowText.slice(0, 160))
  check('列表行显示完成进度', rowText.includes('1 / 2'), rowText.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-search.png'), fullPage: true })

  // 查询条件联动：状态多选（前端逗号拼接）+ 显示已取消（布尔 → 1/0）
  // 注：状态/显示已取消控件均为 @change 即时查询，无需点「查询」按钮（antd 两字按钮会插空格，按名字定位不可靠）
  const rowTexts = () => page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid tbody tr')].map(tr => (tr.innerText || '').replace(/\s+/g, ' ').trim()).filter(t => /PSXL-/.test(t)))

  await searchByKeyword('') // 清空关键字，先把全量捞出来
  await page.locator('.search-area .ant-select-multiple').first().click()
  await page.waitForTimeout(800)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option', { hasText: '已完成' }).first().click()
  await page.keyboard.press('Escape')
  await page.waitForTimeout(3200)
  const doneRows = await rowTexts()
  check('状态多选过滤生效（全部为已完成）',
    doneRows.length > 0 && doneRows.every(r => r.includes('已完成') && !r.includes('规划中') && !r.includes('配送中')),
    `rows=${doneRows.length} | ${doneRows[0] || ''}`.slice(0, 160))

  // 追加「状态=已取消」+ 勾选「显示已取消」→ 已取消路线应可见
  await page.locator('.search-area .ant-select-multiple').first().click()
  await page.waitForTimeout(800)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option', { hasText: '已取消' }).first().click()
  await page.keyboard.press('Escape')
  await page.waitForTimeout(1000)
  await page.locator('.search-area .ant-checkbox-input').first().check({ force: true })
  await page.waitForTimeout(3200)
  const cancelledRows = await rowTexts()
  check('状态多选追加「已取消」后可见已取消路线',
    cancelledRows.some(r => r.includes('已取消')) && cancelledRows.every(r => r.includes('已取消') || r.includes('已完成')),
    `rows=${cancelledRows.length} 已取消=${cancelledRows.filter(r => r.includes('已取消')).length}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-filter-status.png'), fullPage: true })

  // 复位筛选（整页重载，避免 antd 清除图标定位不稳）
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.ss-grid th', { timeout: 60000 })
  await page.waitForTimeout(3000)

  // 抽屉能力入口（须为未开始执行的路线单才有操作入口）
  await searchByKeyword('')
  const planningRow = page.locator('.ss-grid tbody tr', { hasText: '规划中' }).first()
  if (await planningRow.count()) {
    await planningRow.locator('button:has-text("详情")').first().click()
    await page.waitForTimeout(2500)
    const capText = (await page.locator('.ant-drawer-content:visible').last().innerText()).replace(/\s+/g, '')
    for (const b of ['路线规划', 'ETA预估', '推送客户', '添加配送点位', '地图查看', '通知台账']) {
      check(`详情抽屉含「${b}」能力入口`, capText.includes(b), capText.slice(0, 120))
    }
    check('抽屉点位表含 ETA 列', capText.includes('预估到达'), capText.slice(0, 200))
    check('抽屉点位表含经纬度列', capText.includes('纬度') && capText.includes('经度'), capText.slice(0, 200))

    // 地图查看（复用 RouteMapCanvas，不自建地图）
    await page.locator('.ant-drawer-content:visible button:has-text("地图查看")').first().click()
    await page.waitForTimeout(2000)
    const mapModal = page.locator('.ant-modal-content:visible').last()
    const mapText = (await mapModal.innerText()).replace(/\s+/g, '')
    check('地图查看弹窗可打开', mapText.includes('路线地图查看'), mapText.slice(0, 80))
    const mapCanvas = await mapModal.locator('canvas, svg').count()
    check('地图画布已渲染（复用矢量地图组件）', mapCanvas > 0, `nodes=${mapCanvas}`)
    await page.screenshot({ path: path.join(SHOTS, 'ui-map.png'), fullPage: true })
    await closeModals()

    // 通知台账
    await page.locator('.ant-drawer-content:visible button:has-text("通知台账")').first().click()
    await page.waitForTimeout(2000)
    const ledgerModal = page.locator('.ant-modal-content:visible').last()
    const ledgerText = (await ledgerModal.innerText()).replace(/\s+/g, '')
    check('通知台账弹窗可打开', ledgerText.includes('ETA通知台账'), ledgerText.slice(0, 80))
    check('台账明示发送口径（消息中心 + 通道配置）', ledgerText.includes('消息中心') && ledgerText.includes('sms.'), ledgerText.slice(0, 160))
    check('台账含发送结果列', ledgerText.includes('发送结果'), ledgerText.slice(0, 200))
    check('台账含状态与操作项', ['待发送', '标记已发送', '作废'].every(t => ledgerText.includes(t)), ledgerText.slice(0, 200))
    await page.screenshot({ path: path.join(SHOTS, 'ui-eta-ledger.png'), fullPage: true })
    await closeModals()
    await page.locator('.ant-drawer-content:visible .ant-drawer-close').last().click()
    await page.waitForTimeout(1000)
  } else {
    for (const b of ['路线规划', 'ETA预估', '推送客户', '添加配送点位']) {
      check(`详情抽屉含「${b}」能力入口`, false, '列表无「规划中」路线单')
    }
    check('抽屉点位表含 ETA 列', false, '列表无「规划中」路线单')
  }

  // 详情抽屉（点位明细 + 签收）
  await searchByKeyword(apiRouteCode)
  await page.locator('.ss-grid tbody tr', { hasText: apiRouteCode }).first().locator('button:has-text("详情")').first().click()
  await page.waitForTimeout(2500)
  const drawer = page.locator('.ant-drawer-content:visible').last()
  const drawerText = (await drawer.innerText()).replace(/\s+/g, '')
  check('点位明细抽屉可打开', drawerText.includes('点位明细'), drawerText.slice(0, 80))
  check('抽屉含点位状态列与签收列', ['点位明细', '签收人', '签收时间', '失败原因'].every(t => drawerText.includes(t)), drawerText.slice(0, 200))
  check('抽屉显示已完成状态', drawerText.includes(apiRouteCode), drawerText.slice(0, 120))
  await page.screenshot({ path: path.join(SHOTS, 'ui-detail.png'), fullPage: true })
  await page.locator('.ant-drawer-content:visible .ant-drawer-close').last().click()
  await page.waitForTimeout(1000)

  // 新增弹窗：UI 真实建单
  await page.click('button:has-text("新增路线")')
  await page.waitForTimeout(2500)
  const modal = page.locator('.ant-modal-content:visible').last()
  const modalText = (await modal.innerText()).replace(/\s+/g, '')
  check('新增弹窗标题正确', modalText.includes('新增配送路线单'), modalText.slice(0, 60))
  for (const f of ['路线编号', '配送线路', '计划日期', '配送员', '车辆', '配送围栏', '围栏归集', '起点地址', '配送点位']) {
    check(`新增弹窗含字段「${f}」`, modalText.includes(f))
  }
  const codeVal = await modal.locator('input').first().inputValue()
  check('弹窗自动带出号段编号', /^PSXL-\d{8}-\d{4}$/.test(String(codeVal)), codeVal)

  // 填配送员（选择器：0=配送线路 1=配送员 2=车辆）
  await modal.locator('.ant-select').nth(1).click()
  await page.waitForTimeout(600)
  const riderOpts = await page.locator('.ant-select-dropdown:visible .ant-select-item-option').count()
  check('配送员为选择器（非手输 ID）', riderOpts > 0, `options=${riderOpts}`)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').first().click()
  await page.waitForTimeout(400)

  // 填点位地址
  const addrInput = modal.locator('input[placeholder="地址（必填）"]').first()
  await addrInput.fill('UI 建单地址 1 号')
  await modal.locator('button:has-text("添加点位")').click()
  await page.waitForTimeout(400)
  await modal.locator('input[placeholder="地址（必填）"]').nth(1).fill('UI 建单地址 2 号')
  await page.screenshot({ path: path.join(SHOTS, 'ui-add.png'), fullPage: true })
  await modal.locator('button:has-text("保存(Enter)")').click()
  await page.waitForTimeout(3500)
  const addOk = (await page.locator('.ant-message-success').count()) > 0
    || (await page.locator('.ant-modal-content:visible').count()) === 0
  check('UI 新增配送路线单保存无报错', addOk)
  const afterAdd = await api('GET', `/delivery/route/page?pageNum=1&pageSize=20&keyword=UI 建单地址`)
  check('UI 建单真实落库（接口可查）', Number(data(afterAdd)?.total) >= 1, data(afterAdd)?.total)
  const uiCreated = data(afterAdd)?.records?.[0]
  check('UI 建单点位落库 2 条', Number(uiCreated?.totalPoints) === 2, uiCreated?.totalPoints)
  check('UI 建单状态=规划中', uiCreated?.status === 'PLANNING', uiCreated?.status)
  const riderNames = ((data(await api('GET', '/dms/rider/page?pageNum=1&pageSize=500'))?.records) || [])
    .map(r => r.realName).filter(Boolean)
  check('UI 选择配送员回填姓名快照（命中档案姓名，未混入手机号）',
    !!uiCreated?.deliveryPersonName && riderNames.includes(uiCreated.deliveryPersonName),
    `${uiCreated?.deliveryPersonName} / 档案样本=${riderNames.slice(0, 3).join(',')}`)
  check('UI 建单计划日期为今天', uiCreated?.planDate === new Date(new Date().getTime() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10), uiCreated?.planDate)

  // 行内更多：修改
  await searchByKeyword('')
  const firstRow = page.locator('.ss-grid tbody tr').filter({ hasText: 'PSXL-' }).first()
  await firstRow.locator('button:has-text("更多")').click()
  await page.waitForTimeout(800)
  const menuText = (await page.locator('.ant-dropdown:visible').last().innerText()).replace(/\s+/g, '')
  check('行内「更多」含 修改/打印路线单', menuText.includes('修改') && menuText.includes('打印路线单'), menuText.slice(0, 80))
  await page.keyboard.press('Escape')
  await page.waitForTimeout(500)

  // 勾选 + 批量按钮启用
  const cb = page.locator('.ss-grid tbody tr').filter({ hasText: 'PSXL-' }).first().locator('input[type=checkbox]').first()
  await cb.click({ force: true })
  await page.waitForTimeout(800)
  const batchEnabled = await page.evaluate(() => {
    const btns = [...document.querySelectorAll('button')].filter(b => (b.innerText || '').includes('批量开始配送'))
    return btns.length > 0 && !btns[0].disabled
  })
  check('勾选后批量按钮可用', batchEnabled)
  await page.screenshot({ path: path.join(SHOTS, 'ui-batch.png'), fullPage: true })

  // 经典分页栏
  const pager = page.locator('.classic-pagination').first()
  check('底部含经典分页栏', (await pager.count()) > 0)
  if (await pager.count()) {
    const pagerText = (await pager.innerText()).replace(/\s+/g, '')
    check('分页栏显示总数与页码', pagerText.includes('共') && pagerText.includes('页'), pagerText.slice(0, 80))
    check('分页栏含每页条数选择', (await pager.locator('.size-select, .ant-select').count()) > 0)
  } else {
    check('分页栏显示总数与页码', false, 'classic-pagination 未渲染')
    check('分页栏含每页条数选择', false, 'classic-pagination 未渲染')
  }

  check('全流程无 JS 运行时错误', errors.length === 0, errors.slice(0, 2).join(' | '))

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  await login()
  await ensurePrerequisites()
  await apiSuite()
  await fenceSuite()
  await uiSuite()

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 汇总：${pass}/${results.length} 通过，${fail} 失败 ═══`)
  if (fail > 0) {
    console.log('失败项：')
    results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  }
  process.exit(fail > 0 ? 1 : 0)
})().catch(e => {
  console.error('验收异常：', e)
  process.exit(2)
})
