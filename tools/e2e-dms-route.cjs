/*
 * 路线规划（配送 → 配送路线 → 路线规划）金标准端到端验证
 *   · 接口验收：配置状态 / 规划(兼容 waypoints+destinations) / 重规划 / 距离 / 编码 / 逆编码 /
 *              围栏校验(圆形·多边形·内联·批量) / 围栏 CRUD / 坐标转换 / 鉴权
 *   · UI 验收：左侧能力菜单 5 项 / 降级横幅 / 矢量地图(选点·折线·圆·多边形) / 规划结果表 /
 *              候选地址 / 坐标转换 / 围栏台账(新增·停用·删除·列配置齿轮) / 一键生成配送路线单 / 导出真实 xlsx
 *
 * 用法：node tools/e2e-dms-route.cjs                       （默认后端 5678、前端 5656）
 *      ROUTE_PORT=5678 FE_URL=http://localhost:5656 E2E_USER=admin node tools/e2e-dms-route.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const { execFileSync } = require('child_process')
const fs = require('fs')
const path = require('path')
const XLSX = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ROUTE_PORT || 5678)
/** 第二个实例端口（用于验证 Redis 广播 / 共享缓存跨实例生效） */
const SECOND_PORT = Number(process.env.SECOND_PORT || 5679)
const SHOTS = 'I:/AI-Ready/tool-results/dms-route'
const E2E_USER = process.env.E2E_USER || 'admin'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

function tryJson(buf) {
  try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null }
}

function rawReq(method, reqPath, body, token, extraHeaders, port) {
  const targetPort = port || PORT
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null || body instanceof Buffer ? body : JSON.stringify(body)
    const headers = {
      ...(body instanceof Buffer ? { 'Content-Type': 'application/octet-stream' } : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {}),
    }
    if (body instanceof Buffer) headers['Content-Length'] = body.length
    else if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: targetPort, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        resolve({ status: res.statusCode, buf, headers: res.headers, json: tryJson(buf) })
      })
    })
    r.on('error', reject)
    // 兜底超时：远端地图服务不可达时避免脚本挂死（connect 超时 10s + 读 30s 已由后端控制）
    r.setTimeout(60000, () => r.destroy(new Error('request timeout')))
    if (data) r.write(data)
    r.end()
  })
}

/**
 * 发送《配送参数》保存体：与前端 `configApi.update` 完全一致（body = JSON.stringify(值)）
 * 用于验证「页面保存 → 配置中心热生效」的真实链路（含空值清空）。
 */
function rawConfigValue(method, reqPath, value, token) {
  return rawReq(method, reqPath, Buffer.from(JSON.stringify(value), 'utf8'), token,
    { 'Content-Type': 'application/json' })
}

let TOKEN = null

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  let res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.json.data.uuid,
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

const data = res => (res.json ? res.json.data : null)

/** 指定实例发请求（跨实例验证用） */
function apiAt(port, method, reqPath, body) {
  return rawReq(method, reqPath, body, TOKEN, undefined, port)
}

/** 直接操作 Redis（植入/清理地理编码缓存条目，验证跨实例共享缓存） */
function redisRun(code) {
  execFileSync('python', ['-c', code], { stdio: 'pipe' })
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

const STAMP = Date.now().toString().slice(-6)
const FENCE_NAME = 'E2E路线围栏' + STAMP
const FENCE_POLY_NAME = 'E2E多边形围栏' + STAMP

// 杭州地区测试坐标
const ORIGIN = { lat: 30.2469, lng: 120.1562, address: 'E2E起点-城站' }
const NEAR = { lat: 30.2741, lng: 120.1551, address: 'E2E西湖近点' }
const FAR = { lat: 30.30, lng: 120.20, address: 'E2E城北远点' }

async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // ── 1. 配置状态 ──
  const cfg = data(await api('GET', '/dms/route/config'))
  check('配置状态返回坐标体系 GCJ02', cfg?.coordSystem === 'GCJ02', cfg?.coordSystem)
  check('配置状态返回三种出行方式', (cfg?.travelModes || []).length === 3, JSON.stringify(cfg?.travelModes))
  check('未配置地图 Key 时降级标记生效', cfg?.degraded === true && cfg?.provider === 'local', `provider=${cfg?.provider}, degraded=${cfg?.degraded}`)
  check('配置状态含 Key 配置指引', String(cfg?.hint || '').includes('环境变量'), cfg?.hint)
  check('配置状态给出环境变量名与配置中心键', cfg?.envVarName === 'AMAP_API_KEY' && cfg?.configKey === 'map.amap.api-key', `${cfg?.envVarName} / ${cfg?.configKey}`)
  check('配置状态返回 Key 来源枚举', ['ENV', 'SPRING', 'CONFIG', 'NONE'].includes(String(cfg?.keySource)), cfg?.keySource)
  check('配置指引覆盖三种落位（环境变量/启动参数/配送参数）',
    /环境变量/.test(String(cfg?.hint || '')) && /启动参数|配置文件/.test(String(cfg?.hint || '')) && /配送参数/.test(String(cfg?.hint || '')),
    String(cfg?.hint || '').slice(0, 140))
  check('服务商状态含来源/环境变量/配置键',
    (cfg?.providers || []).length >= 3 && (cfg?.providers || []).every(p => p.source && p.envVarName && p.configKey),
    JSON.stringify(cfg?.providers?.[0]))

  // 《配送参数》配置中心已预置地图配置键（全局默认行 tenant_id=0，租户可见）
  const configList = data(await api('GET', '/dms/config/list?tenantId=0'))
  const mapConfigKeys = (configList || []).map(c => c.configKey).filter(k => String(k).startsWith('map.'))
  check('《配送参数》已预置地图配置键（4 条）', mapConfigKeys.length >= 4, JSON.stringify(mapConfigKeys))

  // ── 配置中心热生效链路（仅当 Key 未由环境变量/配置文件提供时验证，避免覆盖真实 Key）──
  if (cfg?.keySource === 'NONE') {
    await rawConfigValue('PUT', '/dms/config/map.amap.api-key?tenantId=0', 'e2e-dummy-key', TOKEN)
    const cfgHot = data(await api('GET', '/dms/route/config'))
    check('《配送参数》保存后立即生效（无需重启）',
      cfgHot?.provider === 'amap' && cfgHot?.configured === true && cfgHot?.degraded === false && cfgHot?.keySource === 'CONFIG',
      `provider=${cfgHot?.provider}, source=${cfgHot?.keySource}, degraded=${cfgHot?.degraded}`)
    const planWithBadKey = data(await api('POST', '/dms/route/plan', { origin: ORIGIN, destinations: [NEAR] }))
    check('远端 Key 无效时自动降级直线估算（不阻断业务）',
      planWithBadKey?.success === true && planWithBadKey?.degraded === true && planWithBadKey?.provider === 'local',
      `provider=${planWithBadKey?.provider}, degraded=${planWithBadKey?.degraded}`)
    await api('DELETE', '/dms/config/map.amap.api-key?tenantId=0')
    const cfgBack = data(await api('GET', '/dms/route/config'))
    check('清空配置后回到降级模式', cfgBack?.degraded === true && cfgBack?.provider === 'local',
      `provider=${cfgBack?.provider}, degraded=${cfgBack?.degraded}`)
    check('清空（DELETE）后配置中心回到未配置态',
      (data(await api('GET', '/dms/config/map.amap.api-key?tenantId=0')) || {}).configured === false,
      JSON.stringify(data(await api('GET', '/dms/config/map.amap.api-key?tenantId=0'))))
  } else {
    check(`配置中心热生效链路（跳过：Key 已由${cfg?.keySource}提供，不覆盖真实 Key）`, true, `source=${cfg?.keySource}`)
  }

  // ── 2. 路线规划 ──
  const planRes = data(await api('POST', '/dms/route/plan', {
    origin: ORIGIN, destinations: [FAR, NEAR], direction: 'DRIVING', strategy: 0,
  }))
  check('规划成功（destinations 字段生效，不再被忽略）', planRes?.success === true, planRes?.message)
  check('规划返回起点+2个点位', (planRes?.stops || []).length === 3, JSON.stringify((planRes?.stops || []).map(s => s.type)))
  check('起点类型为 start', planRes?.stops?.[0]?.type === 'start', planRes?.stops?.[0]?.type)
  check('末点类型为 destination', planRes?.stops?.[2]?.type === 'destination', planRes?.stops?.[2]?.type)
  check('最近邻排序把近点排在远点之前', Number(planRes?.optimizedOrder?.[0]) === 1, JSON.stringify(planRes?.optimizedOrder))
  check('最近邻排序后近点距起点更短', Number(planRes?.stops?.[1]?.distanceFromPrev) < Number(planRes?.stops?.[2]?.distanceFromPrev), `${planRes?.stops?.[1]?.distanceFromPrev} vs ${planRes?.stops?.[2]?.distanceFromPrev}`)
  check('总距离与总时长>0', Number(planRes?.totalDistance) > 0 && Number(planRes?.totalDuration) > 0, `${planRes?.totalDistance}m/${planRes?.totalDuration}s`)
  check('降级标记与提示回传', planRes?.degraded === true && String(planRes?.message || '').includes('直线'), planRes?.message)

  const planWp = data(await api('POST', '/dms/route/plan', {
    origin: ORIGIN, waypoints: [NEAR], destination: FAR,
  }))
  check('waypoints 字段兼容（回归验证）', planWp?.success === true && (planWp?.stops || []).length === 3, JSON.stringify((planWp?.stops || []).length))

  const planSingle = data(await api('POST', '/dms/route/plan', { origin: ORIGIN, destinations: [NEAR] }))
  check('单点规划返回 2 个点位', planSingle?.success === true && (planSingle?.stops || []).length === 2, JSON.stringify((planSingle?.stops || []).length))

  const planCycle = data(await api('POST', '/dms/route/plan', { origin: ORIGIN, destinations: [NEAR], direction: 'CYCLING' }))
  check('骑行方式回填 travelMode', planCycle?.travelMode === 'CYCLING', planCycle?.travelMode)
  const planWalk = data(await api('POST', '/dms/route/plan', { origin: ORIGIN, destinations: [NEAR], direction: 'WALKING' }))
  check('步行方式回填 travelMode', planWalk?.travelMode === 'WALKING', planWalk?.travelMode)

  const noOrigin = await api('POST', '/dms/route/plan', { destinations: [NEAR] })
  check('缺少起点返回 400 校验', noOrigin.status === 400, `${noOrigin.status} ${noOrigin.json?.message}`)
  const noStops = data(await api('POST', '/dms/route/plan', { origin: ORIGIN, destinations: [] }))
  check('无配送点时 success=false 且提示', noStops?.success === false && String(noStops?.message || '').includes('空'), noStops?.message)

  // 坐标体系：WGS84 入参应被转换为 GCJ02（点位坐标发生偏移）
  const planWgs = data(await api('POST', '/dms/route/plan', { origin: { lat: 30.2469, lng: 120.1562 }, destinations: [NEAR], coordSystem: 'WGS84' }))
  check('WGS84 入参自动转换为 GCJ02', Math.abs(Number(planWgs?.stops?.[0]?.lat) - ORIGIN.lat) > 1e-4, `in=${ORIGIN.lat} out=${planWgs?.stops?.[0]?.lat}`)
  check('结果声明坐标体系 GCJ02', planWgs?.coordSystem === 'GCJ02', planWgs?.coordSystem)

  // ── 3. 重新规划 ──
  const reopt = data(await api('POST', '/dms/route/reoptimize', {
    origin: { lat: 30.28, lng: 120.16, address: '当前位置' }, destinations: [NEAR, FAR], visitedCount: 3,
  }))
  check('重规划成功', reopt?.success === true, reopt?.message)
  check('重规划点位序号按已送达续编', Number(reopt?.stops?.[0]?.index) === 4, JSON.stringify((reopt?.stops || []).map(s => s.index)))
  check('重规划提示已按当前位置规划', String(reopt?.message || '').includes('重新规划'), reopt?.message)

  // ── 4. 距离计算 ──
  const dist = data(await api('POST', '/dms/route/distance', { origin: ORIGIN, destinations: [NEAR, FAR], type: 2 }))
  check('批量距离返回 2 条', (dist?.distances || []).length === 2, JSON.stringify(dist?.distances?.length))
  check('距离条目回填起点/终点坐标', dist?.distances?.[0]?.originLat === ORIGIN.lat && dist?.distances?.[0]?.destLat === NEAR.lat, JSON.stringify(dist?.distances?.[0]))
  check('驾车距离带时长估算', Number(dist?.distances?.[0]?.duration) > 0, dist?.distances?.[0]?.duration)

  // ── 5. 编码 / 逆编码（未配置 Key 的降级口径） ──
  const geo = data(await api('GET', '/dms/route/geocode?address=浙江省杭州市西湖区'))
  check('地址编码在未配置 Key 时明确降级（不返回假坐标）', geo?.success === false && geo?.degraded === true && geo?.lat == null, geo?.message)
  check('地址编码降级提示含配置指引', String(geo?.message || '').includes('Key'), geo?.message)
  const rev = data(await api('GET', '/dms/route/reverse-geocode?lat=30.2741&lng=120.1551'))
  check('逆编码在未配置 Key 时明确降级', rev?.success === false && rev?.degraded === true, rev?.message)
  check('逆编码回显入参坐标', Number(rev?.lat) === 30.2741 && Number(rev?.lng) === 120.1551, `${rev?.lat},${rev?.lng}`)
  const revBad = await api('GET', '/dms/route/reverse-geocode?lat=999&lng=120')
  check('非法坐标逆编码返回 400', revBad.status === 400, `${revBad.status} ${revBad.json?.message}`)

  // ── 6. 坐标转换 ──
  const conv = data(await api('POST', '/dms/route/convert', { lat: 30.2741, lng: 120.1551, from: 'WGS84', to: 'GCJ02' }))
  check('WGS84→GCJ02 发生偏移', Math.abs(conv?.lat - 30.2741) > 1e-4 && Math.abs(conv?.lng - 120.1551) > 1e-4, `${conv?.lat},${conv?.lng}`)
  const convBack = data(await api('POST', '/dms/route/convert', { lat: conv.lat, lng: conv.lng, from: 'GCJ02', to: 'WGS84' }))
  check('GCJ02→WGS84 回环误差 < 10m', Math.abs(convBack?.lat - 30.2741) < 1e-4 && Math.abs(convBack?.lng - 120.1551) < 1e-4, `${convBack?.lat},${convBack?.lng}`)
  const convBatch = data(await api('POST', '/dms/route/convert', { points: [{ lat: 30.1, lng: 120.1 }, { lat: 30.2, lng: 120.2 }], from: 'WGS84', to: 'GCJ02' }))
  check('批量坐标转换返回 2 条', (convBatch?.results || []).length === 2, JSON.stringify(convBatch?.results?.length))
  const convBad = await api('POST', '/dms/route/convert', { lat: 30.1, lng: 120.1, from: 'XXX', to: 'GCJ02' })
  check('非法坐标体系返回 400', convBad.status === 400, `${convBad.status} ${convBad.json?.message}`)

  // ── 7. 电子围栏 CRUD ──
  const nc = data(await api('GET', '/dms/route/fence/next-code'))
  check('围栏编码生成（WL+序号）', /^WL\d{3,}$/.test(String(nc)), nc)

  const createCircle = await api('POST', '/dms/route/fence', {
    fenceName: FENCE_NAME, fenceType: 'CIRCLE',
    centerLat: NEAR.lat, centerLng: NEAR.lng, radiusMeters: 1500,
    bizType: 'ROUTE', bizName: 'E2E线路档案', remark: 'E2E圆形围栏', status: 'ENABLED',
  })
  const circle = data(createCircle)
  check('新增圆形围栏成功', createCircle.json?.code === 200 && circle?.id, JSON.stringify(circle).slice(0, 200))
  check('圆形围栏回填编码', /^WL\d{3,}$/.test(String(circle?.fenceCode)), circle?.fenceCode)

  const createPoly = await api('POST', '/dms/route/fence', {
    fenceName: FENCE_POLY_NAME, fenceType: 'POLYGON',
    polygonPoints: '120.150,30.240;120.230,30.240;120.230,30.310;120.150,30.310',
  })
  const poly = data(createPoly)
  check('新增多边形围栏成功', createPoly.json?.code === 200 && poly?.id, JSON.stringify(poly).slice(0, 200))
  check('多边形顶点串规范化入库', String(poly?.polygonPoints || '').split(';').length === 4, poly?.polygonPoints)

  const badPoly = await api('POST', '/dms/route/fence', { fenceName: 'E2E坏多边形', fenceType: 'POLYGON', polygonPoints: '120.1,30.1' })
  check('多边形顶点不足 3 个被拒', badPoly.status === 400, `${badPoly.status} ${badPoly.json?.message}`)
  const noName = await api('POST', '/dms/route/fence', { fenceType: 'CIRCLE', centerLat: 30.1, centerLng: 120.1, radiusMeters: 100 })
  check('围栏名称必填校验', noName.status === 400, `${noName.status} ${noName.json?.message}`)
  const badRadius = await api('POST', '/dms/route/fence', { fenceName: 'E2E零半径', fenceType: 'CIRCLE', centerLat: 30.1, centerLng: 120.1, radiusMeters: 0 })
  check('圆形围栏半径必须大于 0', badRadius.status === 400, `${badRadius.status} ${badRadius.json?.message}`)
  const dupCode = await api('POST', '/dms/route/fence', { fenceName: 'E2E重号', fenceCode: circle.fenceCode, fenceType: 'CIRCLE', centerLat: 30.1, centerLng: 120.1, radiusMeters: 100 })
  check('围栏编码唯一校验', dupCode.status === 400, `${dupCode.status} ${dupCode.json?.message}`)

  const pageKw = data(await api('GET', `/dms/route/fence/page?pageNum=1&pageSize=20&keyword=${FENCE_NAME}`))
  check('围栏分页按名称检索命中', Number(pageKw?.total) >= 1, JSON.stringify(pageKw?.total))
  const pageType = data(await api('GET', '/dms/route/fence/page?pageNum=1&pageSize=50&fenceType=POLYGON'))
  check('围栏分页按类型过滤', (pageType?.records || []).every(r => r.fenceType === 'POLYGON'), JSON.stringify((pageType?.records || []).map(r => r.fenceType)))
  const pageBiz = data(await api('GET', '/dms/route/fence/page?pageNum=1&pageSize=50&bizType=ROUTE'))
  check('围栏分页按绑定类型过滤', (pageBiz?.records || []).some(r => r.id === circle.id), JSON.stringify((pageBiz?.records || []).length))
  const detail = data(await api('GET', `/dms/route/fence/${circle.id}`))
  check('围栏详情回填绑定对象', detail?.bizType === 'ROUTE' && detail?.bizName === 'E2E线路档案', `${detail?.bizType}/${detail?.bizName}`)

  // 圆形 → 多边形：验证「清空字段」真实落库（null 也要更新）
  const upd = await api('PUT', `/dms/route/fence/${circle.id}`, {
    fenceName: FENCE_NAME, fenceType: 'POLYGON',
    polygonPoints: '120.150,30.250;120.180,30.250;120.180,30.280',
    status: 'ENABLED',
  })
  const updData = data(upd)
  check('围栏类型切换为多边形', updData?.fenceType === 'POLYGON', updData?.fenceType)
  check('切换类型后圆形字段被清空（null 落库）', updData?.centerLat == null && updData?.radiusMeters == null, `centerLat=${updData?.centerLat}, radius=${updData?.radiusMeters}`)
  check('切换类型后备注清空生效（null 落库）', updData?.remark == null, updData?.remark)

  // 校验：圆形围栏
  const inC = data(await api('POST', '/dms/route/fence-check', { fenceId: poly.id, lat: 30.26, lng: 120.16 }))
  check('多边形围栏内点判定为在围栏内', inC?.inside === true, JSON.stringify(inC?.message))
  const outC = data(await api('POST', '/dms/route/fence-check', { fenceId: poly.id, lat: 30.20, lng: 120.16 }))
  check('多边形围栏外点判定为在围栏外', outC?.inside === false, outC?.message)
  check('多边形判定回传距边界距离', Number(outC?.distanceMeters) > 0, outC?.distanceMeters)
  // 边界点（落在 AC 斜边上）应按「在围栏内」处理，且距边界≈0
  const onEdge = data(await api('POST', '/dms/route/fence-check', { fenceId: circle.id, lat: 30.26, lng: 120.16 }))
  check('多边形边界点判定为在围栏内', onEdge?.inside === true, `${onEdge?.inside} / ${onEdge?.distanceMeters}`)
  check('边界点距边界距离≈0', Number(onEdge?.distanceMeters) <= 0.01, onEdge?.distanceMeters)
  const inC2 = data(await api('POST', '/dms/route/fence-check', { fenceType: 'CIRCLE', centerLat: NEAR.lat, centerLng: NEAR.lng, radiusMeters: 1000, lat: NEAR.lat + 0.001, lng: NEAR.lng }))
  check('内联圆形围栏内点判定', inC2?.inside === true, inC2?.message)
  check('内联围栏标记为 CIRCLE', inC2?.fenceType === 'CIRCLE', inC2?.fenceType)
  const batch = data(await api('POST', '/dms/route/fence-check', { fenceId: poly.id, points: [{ lat: 30.26, lng: 120.16 }, { lat: 30.20, lng: 120.16 }] }))
  check('批量围栏校验返回 2 条结果', (batch?.results || []).length === 2, JSON.stringify(batch?.results?.length))
  check('批量结果内/外各一条', batch?.results?.[0]?.inside === true && batch?.results?.[1]?.inside === false, JSON.stringify(batch?.results?.map(r => r.inside)))
  const fenceNoPoint = await api('POST', '/dms/route/fence-check', { fenceId: poly.id })
  check('缺少待校验点位返回 400', fenceNoPoint.status === 400, `${fenceNoPoint.status} ${fenceNoPoint.json?.message}`)

  // 停用 → 下拉不含；启用 → 下拉含
  const stDown = data(await api('PUT', `/dms/route/fence/${poly.id}/status?status=DISABLED`))
  check('停用围栏成功', stDown?.status === 'DISABLED', stDown?.status)
  const opts = data(await api('GET', '/dms/route/fence/options'))
  check('启用围栏下拉不含已停用围栏', !(opts || []).some(f => f.id === poly.id), JSON.stringify((opts || []).map(f => f.fenceName)))
  await api('PUT', `/dms/route/fence/${poly.id}/status?status=ENABLED`)
  const opts2 = data(await api('GET', '/dms/route/fence/options'))
  check('启用后下拉重新包含该围栏', (opts2 || []).some(f => f.id === poly.id), JSON.stringify((opts2 || []).map(f => f.fenceName)))
  const badStatus = await api('PUT', `/dms/route/fence/${poly.id}/status?status=XXX`)
  check('非法状态值被拒', badStatus.status === 400, `${badStatus.status} ${badStatus.json?.message}`)

  // 清理：删除围栏
  const del = await api('DELETE', `/dms/route/fence/${poly.id}`)
  check('删除围栏成功', del.json?.code === 200, JSON.stringify(del.json).slice(0, 160))
  const afterDel = data(await api('GET', `/dms/route/fence/page?pageNum=1&pageSize=20&keyword=${FENCE_POLY_NAME}`))
  check('删除后分页不再命中', Number(afterDel?.total) === 0, JSON.stringify(afterDel?.total))
  await api('DELETE', `/dms/route/fence/${circle.id}`)

  // ── 8. 连通性自检（配置可用性） ──
  const verifyNone = data(await api('POST', '/dms/route/verify'))
  check('连通性自检：未配置 Key 时明确未通过并给出落位指引',
    verifyNone?.ok === false && /Key|配置/.test(String(verifyNone?.message || '')) && verifyNone?.envVarName === 'AMAP_API_KEY',
    `${verifyNone?.ok} | ${String(verifyNone?.message || '').slice(0, 100)}`)
  check('连通性自检返回样本地址/耗时/来源',
    !!verifyNone?.sampleAddress && Number(verifyNone?.latencyMs) >= 0 && !!verifyNone?.keySource,
    `${verifyNone?.sampleAddress} / ${verifyNone?.latencyMs}ms / ${verifyNone?.keySource}`)

  // ── 9. 敏感配置：脱敏 / 留空不改 / 显式清除 ──
  const cfgList0 = data(await api('GET', '/dms/config/list?tenantId=0'))
  const amap0 = (cfgList0 || []).find(c => c.configKey === 'map.amap.api-key')
  check('敏感配置标记 secret（未配置时无明文）',
    amap0?.secret === true && amap0?.configured === false && !amap0?.configValue, JSON.stringify(amap0))
  await rawConfigValue('PUT', '/dms/config/map.amap.api-key?tenantId=0', 'e2e-secret-key-123456', TOKEN)
  const amapSaved = data(await api('GET', '/dms/config/map.amap.api-key?tenantId=0'))
  check('敏感配置保存后只回掩码（明文不出服务端）',
    amapSaved?.configured === true && !!amapSaved?.configValue
      && !String(amapSaved.configValue).includes('e2e-secret-key')
      && String(amapSaved.configValue).includes('****'),
    amapSaved?.configValue)
  const verifyBad = data(await api('POST', '/dms/route/verify'))
  check('无效 Key 自检失败并给出原因', verifyBad?.ok === false && String(verifyBad?.message || '').length > 10,
    String(verifyBad?.message || '').slice(0, 140))
  await rawConfigValue('PUT', '/dms/config/map.amap.api-key?tenantId=0', '', TOKEN)
  const amapKeep = data(await api('GET', '/dms/config/map.amap.api-key?tenantId=0'))
  check('敏感配置留空保存 = 不修改（原值保留）', amapKeep?.configured === true, JSON.stringify(amapKeep?.configured))
  const cleared = data(await api('DELETE', '/dms/config/map.amap.api-key?tenantId=0'))
  check('显式清除后回到未配置态', cleared?.configured === false, JSON.stringify(cleared?.configValue))
  check('清除后地图能力回到降级', data(await api('GET', '/dms/route/config'))?.degraded === true)

  // ── 10. Redis 地理编码缓存（跨实例共享） ──
  const cacheAddr = 'E2E cache addr ' + STAMP
  const cacheKey = 'dms:route:geocode:1:' + Buffer.from('|' + cacheAddr.toLowerCase(), 'utf8').toString('base64url')
  const plantedJson = JSON.stringify({
    success: true, provider: 'e2e-planted', coordSystem: 'GCJ02', cached: false,
    lat: 30.123456, lng: 120.654321, formattedAddress: 'E2E planted',
    province: 'Zhejiang', city: 'Hangzhou', district: 'Xihu',
  })
  const plantedB64 = Buffer.from(plantedJson, 'utf8').toString('base64')
  redisRun(`import redis,base64;redis.Redis(host='localhost',port=6379,db=0).set('${cacheKey}', base64.b64decode('${plantedB64}'), ex=600)`)
  try {
    const hit1 = data(await api('GET', '/dms/route/geocode?address=' + cacheAddr))
    check('地理编码命中 Redis 缓存（cached=true 且坐标来自缓存）',
      hit1?.success === true && hit1?.cached === true && Number(hit1?.lat) === 30.123456,
      JSON.stringify(hit1).slice(0, 200))
    const hit2 = data(await apiAt(SECOND_PORT, 'GET', '/dms/route/geocode?address=' + cacheAddr))
    check('第二个实例命中同一缓存（Redis 跨实例共享）',
      hit2?.success === true && hit2?.cached === true, JSON.stringify(hit2).slice(0, 160))
  } finally {
    redisRun(`import redis;redis.Redis(host='localhost',port=6379,db=0).delete('${cacheKey}')`)
  }

  // ── 11. 跨实例配置广播（不等 30s TTL） ──
  await apiAt(SECOND_PORT, 'GET', '/dms/route/config')   // 预热第二实例缓存（此时应为 NONE）
  await rawConfigValue('PUT', '/dms/config/map.amap.api-key?tenantId=0', 'e2e-broadcast-key', TOKEN)
  const cfgInst2 = data(await apiAt(SECOND_PORT, 'GET', '/dms/route/config'))
  check('配置保存后另一个实例即时生效（Redis 广播，不等 TTL）',
    cfgInst2?.keySource === 'CONFIG' && cfgInst2?.degraded === false,
    `${cfgInst2?.keySource}/${cfgInst2?.degraded}`)
  await api('DELETE', '/dms/config/map.amap.api-key?tenantId=0')
  const cfgInst2b = data(await apiAt(SECOND_PORT, 'GET', '/dms/route/config'))
  check('清除后另一个实例同步回到降级', cfgInst2b?.degraded === true, `${cfgInst2b?.provider}/${cfgInst2b?.degraded}`)

  // ── 12. VRP 基础约束（容量分批 + 时间窗） ──
  const vrp = data(await api('POST', '/dms/route/plan', {
    origin: ORIGIN,
    destinations: [
      { lat: 30.2741, lng: 120.1551, address: 'E2E-A窗08:10', demand: 3, timeWindowEnd: '08:10' },
      { lat: 30.2680, lng: 120.1600, address: 'E2E-B无窗', demand: 4 },
      { lat: 30.2800, lng: 120.1700, address: 'E2E-C窗09:00', demand: 5, timeWindowEnd: '09:00' },
      { lat: 30.2600, lng: 120.1500, address: 'E2E-D无窗', demand: 2 },
    ],
    vehicleCapacity: 8, vehicleCount: 3, departureTime: '08:00',
  }))
  const vrpBatches = vrp?.batches || []
  check('VRP：按载重分批（需求 14 / 载重 8 → 2 车）', vrpBatches.length === 2,
    JSON.stringify(vrpBatches.map(b => `${b.batchNo}车:载${b.load}`)))
  check('VRP：各批载重均不超上限', vrpBatches.length > 0 && vrpBatches.every(b => Number(b.load) <= 8),
    JSON.stringify(vrpBatches.map(b => b.load)))
  check('VRP：全部点位均分入批次且带批次号',
    (vrp?.stops || []).filter(s => s.type !== 'start').every(s => !!s.batchNo),
    JSON.stringify((vrp?.stops || []).map(s => `${s.address}=${s.batchNo}`)))
  const vrpStops = (vrp?.stops || []).filter(s => s.type !== 'start')
  const idxA = vrpStops.findIndex(s => String(s.address).includes('A窗'))
  const idxC = vrpStops.findIndex(s => String(s.address).includes('C窗'))
  check('VRP：时间窗早的点优先配送', idxA >= 0 && idxC >= 0 && idxA < idxC, `A@${idxA}, C@${idxC}`)
  check('VRP：点位回传 ETA（HH:mm）且按出发时间推算',
    vrpStops.every(s => /^\d{2}:\d{2}$/.test(String(s.eta || ''))) && String(vrpStops[0]?.eta || '') >= '08:00',
    JSON.stringify(vrpStops.map(s => s.eta)))
  check('VRP：批次汇总含距离/时长', vrpBatches.every(b => Number(b.totalDistance) >= 0 && Number(b.totalDuration) >= 0),
    JSON.stringify(vrpBatches.map(b => `${b.totalDistance}m`)))

  const vrpLate = data(await api('POST', '/dms/route/plan', {
    origin: ORIGIN, departureTime: '08:00',
    destinations: [{ lat: 30.2741, lng: 120.1551, address: 'E2E-超窗点', demand: 1, timeWindowEnd: '07:30' }],
  }))
  check('VRP：晚于时间窗产生告警（软约束不阻断）',
    vrpLate?.success === true && (vrpLate?.warnings || []).some(w => String(w).includes('晚于时间窗')),
    JSON.stringify(vrpLate?.warnings))

  const vrpOver = data(await api('POST', '/dms/route/plan', {
    origin: ORIGIN, vehicleCapacity: 8,
    destinations: [{ lat: 30.2741, lng: 120.1551, address: 'E2E-超载点', demand: 20 }],
  }))
  check('VRP：单点超载独占一批（最大车型兜底）并告警',
    (vrpOver?.batches || []).length === 1
      && (vrpOver?.batches || [])[0]?.stops?.some(s => String(s.address || '').includes('超载点'))
      && (vrpOver?.warnings || []).some(w => String(w).includes('超过单车载重') || String(w).includes('超过最大车型载重')),
    JSON.stringify(vrpOver?.warnings))

  const vrpCars = data(await api('POST', '/dms/route/plan', {
    origin: ORIGIN, vehicleCapacity: 3, vehicleCount: 1,
    destinations: [
      { lat: 30.2741, lng: 120.1551, address: 'E2E-车1', demand: 3 },
      { lat: 30.2680, lng: 120.1600, address: 'E2E-车2', demand: 3 },
    ],
  }))
  check('VRP：车辆数不足时超出批次移出排程并告警',
    (vrpCars?.batches || []).length === 1 && (vrpCars?.unassigned || []).length > 0
      && (vrpCars?.warnings || []).some(w => String(w).includes('可用车辆数')),
    JSON.stringify({ batches: (vrpCars?.batches || []).length, unassigned: (vrpCars?.unassigned || []).length, warnings: vrpCars?.warnings }))

  // ── 13. 调度围栏判定接入围栏档案（线路档案绑定） ──
  const fenceGeo = await prepareDispatchFence()
  if (fenceGeo) {
    const inFence = data(await api('POST', `/dms/dispatch/${fenceGeo.taskId}/fence-check?riderId=${fenceGeo.riderId}`))
    check('调度围栏：线路档案绑定的围栏优先于 5000m 兜底', inFence === true, `inFence=${inFence}`)
    await moveRider(fenceGeo.riderId, 30.3000, 120.3000)   // 距取货点 <5000m，但在围栏外
    const outFence = data(await api('POST', `/dms/dispatch/${fenceGeo.taskId}/fence-check?riderId=${fenceGeo.riderId}`))
    check('调度围栏：围栏外的近点判为不在范围内', outFence === false, `outFence=${outFence}`)
    await cleanupDispatchFence(fenceGeo)
  }

  // ── 14. P2：配置变更审计与回滚 ──
  const AUDIT_KEY = 'map.amap.api-key'
  await rawConfigValue('PUT', `/dms/config/${AUDIT_KEY}?tenantId=0`, 'e2e-audit-key-1', TOKEN)
  await rawConfigValue('PUT', `/dms/config/${AUDIT_KEY}?tenantId=0`, 'e2e-audit-key-2', TOKEN)
  const audit1 = data(await api('GET', `/dms/config/${AUDIT_KEY}/history?tenantId=0`))
  check('审计：变更历史可查且含多条记录',
    Array.isArray(audit1) && audit1.length >= 2 && audit1.every(h => h.configKey === AUDIT_KEY),
    JSON.stringify((audit1 || []).slice(0, 2)))
  check('审计：敏感键历史值只存掩码（不含明文）',
    (audit1 || []).every(h => !String(h.newValue || '').includes('e2e-audit-key')
      && !String(h.oldValue || '').includes('e2e-audit-key')),
    JSON.stringify((audit1 || []).map(h => h.newValue).slice(0, 3)))
  check('审计：记录操作人与客户端IP',
    (audit1 || []).some(h => h.operatorName) && (audit1 || []).some(h => h.clientIp),
    JSON.stringify(audit1 && audit1[0]))
  const rollTarget = (audit1 || [])[0]
  await api('POST', `/dms/config/${AUDIT_KEY}/rollback?tenantId=0&historyId=${rollTarget.id}`, null)
  const audit2 = data(await api('GET', `/dms/config/${AUDIT_KEY}/history?tenantId=0`))
  check('审计：回滚后新增 ROLLBACK 记录',
    (audit2 || []).some(h => h.changeType === 'ROLLBACK'),
    JSON.stringify((audit2 || []).map(h => h.changeType).slice(0, 5)))
  await api('DELETE', `/dms/config/${AUDIT_KEY}?tenantId=0`)
  const audit3 = data(await api('GET', `/dms/config/${AUDIT_KEY}/history?tenantId=0`))
  check('审计：清除动作同样留痕（CLEAR）',
    (audit3 || []).some(h => h.changeType === 'CLEAR'),
    JSON.stringify((audit3 || []).map(h => h.changeType).slice(0, 6)))

  // ── 15. P2：地理编码缓存「租户隔离 + 换 Key 清空」 ──
  const tenantAddr = 'E2E tenant cache ' + STAMP
  const plantKey = (tenantId, provider) => {
    const key = 'dms:route:geocode:' + tenantId + ':'
      + Buffer.from('|' + tenantAddr.toLowerCase(), 'utf8').toString('base64url')
    const b64 = Buffer.from(JSON.stringify({
      success: true, provider, cached: false, lat: 31.111111, lng: 121.222222, formattedAddress: provider,
    }), 'utf8').toString('base64')
    redisRun(`import redis,base64;redis.Redis(host='localhost',port=6379,db=0).set('${key}', base64.b64decode('${b64}'), ex=300)`)
    return key
  }
  const plantedKeys = [plantKey(0, 'planted-tenant0')]
  const missOtherTenant = data(await api('GET', '/dms/route/geocode?address=' + tenantAddr))
  check('缓存租户隔离：其它租户的缓存不被命中', missOtherTenant?.cached !== true,
    JSON.stringify(missOtherTenant).slice(0, 140))
  plantedKeys.push(plantKey(1, 'planted-tenant1'))
  const hitOwnTenant = data(await api('GET', '/dms/route/geocode?address=' + tenantAddr))
  check('缓存租户隔离：本租户缓存正常命中',
    hitOwnTenant?.cached === true && hitOwnTenant?.provider === 'planted-tenant1',
    JSON.stringify(hitOwnTenant).slice(0, 160))
  await rawConfigValue('PUT', '/dms/config/map.amap.api-key?tenantId=0', 'e2e-rotate-key', TOKEN)
  const afterRotate = data(await api('GET', '/dms/route/geocode?address=' + tenantAddr))
  check('换 Key 后地理编码缓存整体失效（避免新旧混用）', afterRotate?.cached !== true,
    JSON.stringify(afterRotate).slice(0, 140))
  await api('DELETE', '/dms/config/map.amap.api-key?tenantId=0')
  plantedKeys.forEach(k => redisRun(`import redis;redis.Redis(host='localhost',port=6379,db=0).delete('${k}')`))

  // ── 16. P2：VRP 进阶（多车型 / 多起点 / 硬时间窗 / 时长来源） ──
  const advanced = data(await api('POST', '/dms/route/plan', {
    origin: ORIGIN, departureTime: '08:00',
    origins: [
      { lat: ORIGIN.lat, lng: ORIGIN.lng, address: 'E2E一号仓' },
      { lat: 30.2800, lng: 120.1700, address: 'E2E二号仓' },
    ],
    vehicles: [
      { name: '小面(载3)', capacity: 3, count: 1 },
      { name: '厢货(载8)', capacity: 8, count: 1 },
    ],
    destinations: [
      { lat: 30.2741, lng: 120.1551, address: 'E2E-A点', demand: 2 },
      { lat: 30.2680, lng: 120.1600, address: 'E2E-B点', demand: 2 },
      { lat: 30.2810, lng: 120.1710, address: 'E2E-C点', demand: 2 },
    ],
  }))
  check('VRP进阶：多车型按「能装下的最小车型」分配',
    (advanced?.batches || []).length > 0 && (advanced?.batches || []).every(b => !!b.vehicleName && b.capacity != null),
    JSON.stringify((advanced?.batches || []).map(b => `${b.batchNo}:${b.vehicleName}/${b.capacity}`)))
  check('VRP进阶：多起点标明出发点序号与地址',
    (advanced?.batches || []).every(b => b.originIndex !== null && b.originIndex !== undefined && !!b.originAddress),
    JSON.stringify((advanced?.batches || []).map(b => `${b.batchNo}:${b.originIndex}/${b.originAddress}`)))
  check('VRP进阶：时长来源标记 ESTIMATE（未配地图 Key）',
    advanced?.durationSource === 'ESTIMATE', advanced?.durationSource)

  const hardWindow = data(await api('POST', '/dms/route/plan', {
    origin: ORIGIN, departureTime: '08:00', hardTimeWindow: true, vehicleCapacity: 8,
    destinations: [
      { lat: 30.2741, lng: 120.1551, address: 'E2E-硬窗超时点', demand: 2, timeWindowEnd: '07:30' },
      { lat: 30.2680, lng: 120.1600, address: 'E2E-可送点', demand: 2, timeWindowEnd: '12:00' },
    ],
  }))
  const unassignedAddrs = (hardWindow?.unassigned || []).map(u => String(u.address || ''))
  const stopAddrs = (hardWindow?.stops || []).map(s => String(s.address || ''))
  check('VRP进阶：硬时间窗把超时点移出排程（进入 unassigned）',
    unassignedAddrs.some(a => a.includes('硬窗超时')) && !stopAddrs.some(a => a.includes('硬窗超时')),
    JSON.stringify({ unassigned: unassignedAddrs, stops: stopAddrs }))
  check('VRP进阶：硬时间窗仍正常派出可送点',
    (hardWindow?.batches || []).length === 1 && stopAddrs.some(a => a.includes('可送点')),
    JSON.stringify((hardWindow?.batches || []).map(b => `${b.vehicleName}/${b.load}`)))

  // ── 17. 鉴权 ──
  const noAuth = await rawReq('POST', '/dms/route/fence-check', { lat: 30, lng: 120, fenceType: 'CIRCLE', centerLat: 30, centerLng: 120, radiusMeters: 100 })
  check('未登录访问围栏校验返回 401', noAuth.status === 401, String(noAuth.status))

  return { circleId: circle.id, polyId: poly.id }
}


// ══════════════ 调度围栏测试数据（直连 DB 造数，验收后清理） ══════════════
const DB_PY = "import psycopg2,json,sys;"
  + "c=psycopg2.connect(host='localhost',port=5432,dbname='devdb',user='devuser',password='devuser123');"
  + "c.autocommit=True;cur=c.cursor();cur.execute(sys.argv[1]);"
  + "print(json.dumps(cur.fetchall() if cur.description else [], default=str))"

function dbRun(sql, args) {
  const out = execFileSync('python', ['-c', DB_PY, sql, ...(args || [])], { stdio: 'pipe' }).toString()
  try { return JSON.parse(out.trim().split('\n').pop()) } catch (e) { return [] }
}

/**
 * 造数：线路档案 id + 取货点(距围栏约 26km) + 圆心围栏(r=3000) + 骑手(围栏内)
 * 若围栏档案未生效（仍走 5000m 兜底）则「围栏内点」会被判为不在范围，断言即失败。
 */
async function prepareDispatchFence() {
  const ROUTE_ID = 990001
  const TASK_NO = 'E2E-FENCE-' + STAMP
  const fence = data(await api('POST', '/dms/route/fence', {
    fenceName: 'E2E调度围栏' + STAMP, fenceType: 'CIRCLE',
    centerLat: 30.1000, centerLng: 120.1000, radiusMeters: 3000,
    bizType: 'ROUTE', bizId: String(ROUTE_ID), status: 'ENABLED',
  }))
  if (!fence?.id) {
    check('调度围栏：准备围栏档案', false, JSON.stringify(fence))
    return null
  }
  const taskRows = dbRun(
    `INSERT INTO dms_task (tenant_id, task_no, route_id, source_lat, source_lng, customer_lat, customer_lng, status, deleted) `
    + `VALUES (1, '${TASK_NO}', ${ROUTE_ID}, 30.3000, 120.3000, 30.3000, 120.3000, 0, 0) RETURNING id`)
  const riderRows = dbRun(
    `INSERT INTO dms_rider (tenant_id, real_name, current_lat, current_lng, status, deleted) `
    + `VALUES (1, 'E2E-FENCE-RIDER', 30.1050, 120.1050, 1, 0) RETURNING id`)
  const taskId = taskRows?.[0]?.[0]
  const riderId = riderRows?.[0]?.[0]
  if (!taskId || !riderId) {
    check('调度围栏：准备任务与配送员数据', false, `task=${taskId}, rider=${riderId}`)
    return null
  }
  return { fenceId: fence.id, taskId, riderId, taskNo: TASK_NO }
}

async function moveRider(riderId, lat, lng) {
  dbRun(`UPDATE dms_rider SET current_lat=${lat}, current_lng=${lng} WHERE id=${riderId}`)
}

async function cleanupDispatchFence(geo) {
  dbRun(`DELETE FROM dms_task WHERE id=${geo.taskId}`)
  dbRun(`DELETE FROM dms_rider WHERE id=${geo.riderId}`)
  await api('DELETE', `/dms/route/fence/${geo.fenceId}`)
}

async function uiSuite(ids) {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  // /api 转发到验收实例（前端 dev server 的代理默认指向 5655）
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  async function openPage(url, waitMs = 7000) {
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

  await openPage(`${FE}/dms/route`)
  const body = () => page.locator('body').innerText().then(t => t.replace(/\s+/g, ''))
  let text = await body()
  check('页面可打开（无 404/白屏）', !text.includes('页面不存在') && text.length > 50, page.url())

  // ── 能力菜单 / 工具栏 ──
  for (const cap of ['路线规划', '地址编码', '逆编码', '坐标转换', '围栏管理']) {
    check(`左侧能力菜单含「${cap}」`, text.includes(cap))
  }
  for (const b of ['规划路线', '生成配送路线单', '刷新', '打印(F8)', '导出']) {
    check(`工具栏含「${b}」`, text.includes(b.replace(/\s+/g, '')))
  }
  check('未配置地图 Key 时展示降级横幅', text.includes('降级') && text.includes('Key'), text.slice(0, 120))
  check('地图服务状态标签可见', text.includes('地图服务'), '')
  const svgBox = await page.locator('.route-map svg').first().boundingBox()
  check('矢量地图画布已渲染', !!svgBox && svgBox.width > 200 && svgBox.height > 150, JSON.stringify(svgBox))
  const gridLines = await page.locator('.route-map svg .map-grid line').count()
  check('地图渲染网格参考线', gridLines > 0, `lines=${gridLines}`)
  check('地图渲染比例尺', (await page.locator('.route-map .scale-bar').count()) > 0)
  await page.screenshot({ path: path.join(SHOTS, 'ui-plan-empty.png'), fullPage: true })

  // ── 地图选点回填起点 ──
  // .coord-row #0 = 起点，#1 = 目的地1，#2 = 目的地2（点击行内按钮后在地图上点击即回填该行）
  async function pickFromMap(rowIndex, fx = 0.4, fy = 0.4) {
    // 拾取按钮按图标定位：#0=起点、#1=目的地1…（coord-row 因约束行增多已不可按序号定位）
    await page.locator('.form-pane button:has(.anticon-environment)').nth(rowIndex).click()
    await page.waitForTimeout(300)
    const b = await page.locator('.route-map svg').first().boundingBox()
    await page.mouse.click(b.x + b.width * fx, b.y + b.height * fy)
    await page.waitForTimeout(600)
  }

  await pickFromMap(0, 0.35, 0.4)
  const originLat = await page.locator('input[placeholder="纬度"]').first().inputValue()
  const originLng = await page.locator('input[placeholder="经度"]').first().inputValue()
  check('地图点击拾取起点坐标', Number(originLat) > 0 && Number(originLng) > 0, `${originLat},${originLng}`)

  // ── 目的地：1 个手工填写 + 1 个地图拾取，随后规划 ──
  const latInputs = page.locator('input[placeholder="纬度"]')
  const lngInputs = page.locator('input[placeholder="经度"]')
  const addrInputs = page.locator('input[placeholder="地址/客户名称"]')
  await addrInputs.first().fill('E2E西湖近点')
  await latInputs.nth(1).fill(String(NEAR.lat))
  await lngInputs.nth(1).fill(String(NEAR.lng))
  await page.locator('button:has-text("添加目的地")').click()
  await page.waitForTimeout(400)
  check('添加目的地后出现第 2 个目的地行', (await addrInputs.count()) === 2, `addr=${await addrInputs.count()}`)
  await pickFromMap(2, 0.6, 0.55)
  await addrInputs.nth(1).fill('E2E城北远点')
  const dest2Lat = await latInputs.nth(2).inputValue()
  check('地图拾取回填目的地坐标', Number(dest2Lat) > 0, dest2Lat)

  await page.locator('button:has-text("规划路线")').first().click()
  await page.waitForTimeout(3000)
  text = await body()
  check('点击规划路线后出现结果区', text.includes('总距离') && text.includes('预计时长'))
  const stopRows = await page.locator('.ant-table-tbody tr.ant-table-row').count()
  check('规划结果表渲染 3 行（起点+2目的地）', stopRows === 3, `rows=${stopRows}`)
  const routePolyline = await page.locator('.route-map svg .route-line').count()
  check('规划折线落图', routePolyline > 0, `polyline=${routePolyline}`)
  const markerCount = await page.locator('.route-map svg .marker').count()
  check('地图标记点渲染', markerCount >= 3, `markers=${markerCount}`)
  check('结果区展示服务商与降级标记', text.includes('local') && text.includes('降级'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-plan-result.png'), fullPage: true })

  // ── P2：底图指示（未配 JS Key → 矢量画布）与硬时间窗开关 ──
  const baseMapText = await body()
  check('底图未配 JS Key 时使用矢量画布', baseMapText.includes('底图：矢量'),
    baseMapText.slice(baseMapText.indexOf('底图：'), baseMapText.indexOf('底图：') + 40))
  check('规划参数含「硬时间窗」开关', baseMapText.includes('硬时间窗'), '')

  // ── 地图标记拖拽微调（回写表单坐标） ──
  const latBefore = await page.locator('input[placeholder="纬度"]').first().inputValue()
  const mapBox = await page.locator('.route-map svg').first().boundingBox()
  const markerBox = await page.locator('.route-map svg .marker').first().boundingBox()
  await page.mouse.move(markerBox.x + markerBox.width / 2, markerBox.y + markerBox.height / 2)
  await page.mouse.down()
  await page.mouse.move(mapBox.x + mapBox.width * 0.22, mapBox.y + mapBox.height * 0.32, { steps: 10 })
  await page.mouse.up()
  await page.waitForTimeout(1000)
  const latAfter = await page.locator('input[placeholder="纬度"]').first().inputValue()
  check('地图标记可拖拽并回写起点坐标',
    Number(latAfter) > 0 && Number(latAfter) !== Number(latBefore), `${latBefore} → ${latAfter}`)

  // ── VRP：单车载重 + 需求量 → 分车批次 ──
  await page.locator('.search-item').filter({ hasText: '单车载重' }).locator('input').fill('8')
  await page.locator('input[placeholder="需求量"]').nth(0).fill('5')
  await page.locator('input[placeholder="需求量"]').nth(1).fill('6')
  await page.locator('button:has-text("规划路线")').first().click()
  await page.waitForTimeout(3500)
  text = await body()
  check('VRP：结果区展示分车批次汇总（2 车）', text.includes('第1车') && text.includes('第2车'), text.slice(text.indexOf('第1车'), text.indexOf('第1车') + 80))
  check('VRP：结果表出现批次列', text.includes('批次'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-vrp.png'), fullPage: true })

  // ── 连通性自检按钮 ──
  await page.locator('button').filter({ hasText: /连通性自检/ }).first().click()
  await page.waitForTimeout(3000)
  text = await body()
  check('连通性自检按钮可执行并给出结论', text.includes('连通性异常') || text.includes('连通性正常'), text.slice(0, 120))

  // ── 导出真实 xlsx（规划结果） ──
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('button:has-text("导出")').first().click(),
  ])
  const file = path.join(SHOTS, 'ui-plan-export.xlsx')
  await download.saveAs(file)
  check('导出文件为 xlsx', /\.xlsx$/.test(download.suggestedFilename()), download.suggestedFilename())
  try {
    const wb = XLSX.read(fs.readFileSync(file), { type: 'buffer' })
    const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
    check('导出 xlsx 含规划点位行', rows.length >= 3, `rows=${rows.length}`)
    check('导出 xlsx 含「距上点米」列', Object.keys(rows[0] || {}).includes('距上点米'), JSON.stringify(Object.keys(rows[0] || {})))
    check('导出 xlsx 含 VRP 约束列（批次/需求/时间窗/预计到达）',
      ['批次', '需求量', '时间窗起', '时间窗止', '预计到达'].every(k => Object.keys(rows[0] || {}).includes(k)),
      JSON.stringify(Object.keys(rows[0] || {})))
  } catch (e) {
    check('导出 xlsx 可解析', false, e.message)
  }

  // ── 一键生成配送路线单 ──
  await page.locator('button:has-text("生成配送路线单")').first().click()
  await page.waitForTimeout(1500)
  const genModal = page.locator('.ant-modal-content:visible').last()
  const genText = (await genModal.innerText()).replace(/\s+/g, '')
  check('生成配送路线单弹窗打开', genText.includes('配送路线单'), genText.slice(0, 60))
  check('弹窗含配送员/线路档案/计划日期字段', genText.includes('配送员') && genText.includes('线路档案') && genText.includes('计划日期'), '')
  const pointCountText = genText.match(/点位数量(\d+)个/)
  check('弹窗显示待生成点位数量（≥1）', pointCountText && Number(pointCountText[1]) >= 1, pointCountText?.[0])
  await page.screenshot({ path: path.join(SHOTS, 'ui-generate-modal.png'), fullPage: true })
  await genModal.locator('.ant-modal-close').click()
  await page.waitForTimeout(600)

  // ── 地址编码 ──
  await page.locator('.category-tree-container').getByText('地址编码', { exact: true }).click()
  await page.waitForTimeout(1200)
  await page.locator('textarea').first().fill('浙江省杭州市西湖区文一西路969号')
  await page.locator('button:has-text("查询候选")').click()
  await page.waitForTimeout(2500)
  text = await body()
  check('地址编码切换能力页', text.includes('地址→坐标'), '')
  check('未配置 Key 时编码给出明确告警', text.includes('未配置地图服务Key'), text.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-geocode.png'), fullPage: true })

  // ── 逆编码 ──
  await page.locator('.category-tree-container').getByText('逆编码', { exact: true }).click()
  await page.waitForTimeout(1200)
  text = await body()
  check('逆编码能力页切换', text.includes('坐标→地址'))
  await page.locator('button:has-text("查询地址")').click()
  await page.waitForTimeout(2000)
  text = await body()
  check('逆编码未填坐标时给出校验提示', text.includes('请填写') || text.includes('未配置'), text.slice(0, 120))

  // ── 坐标转换 ──
  await page.locator('.category-tree-container').getByText('坐标转换', { exact: true }).click()
  await page.waitForTimeout(1200)
  await page.locator('input[placeholder="纬度"]').first().fill('30.2741')
  await page.locator('input[placeholder="经度"]').first().fill('120.1551')
  await page.locator('button:has-text("转换为")').click()
  await page.waitForTimeout(2000)
  text = await body()
  check('坐标转换结果显示 GCJ02 坐标', text.includes('30.2717') || text.includes('30.27'), text.slice(0, 200))
  check('坐标转换显示体系说明', text.includes('GCJ-02'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-convert.png'), fullPage: true })

  // ── 围栏管理 ──
  await page.locator('.category-tree-container').getByText('围栏管理', { exact: true }).click()
  await page.waitForTimeout(2500)
  const fenceHeaders = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['围栏编码', '围栏名称', '围栏类型', '几何范围', '绑定对象', '状态', '备注']) {
    check(`围栏台账表头含「${h}」`, fenceHeaders.some(x => x.includes(h)), fenceHeaders.join('/'))
  }
  check('围栏台账带表头列配置齿轮', (await page.locator('.ss-grid .th-settings-btn').count()) > 0)
  text = await body()
  check('围栏校验面板可见', text.includes('围栏校验') && text.includes('检查'))

  // 列配置弹窗
  await page.locator('.ss-grid .th-settings-btn').first().click()
  await page.waitForTimeout(1500)
  const colPanel = page.locator('.ant-modal-content:visible').last()
  const colText = (await colPanel.innerText()).replace(/\s+/g, '')
  check('围栏列配置弹窗含个人/全局双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 80))
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 新增圆形围栏（地图拾取中心点）
  await page.locator('button:has-text("新增围栏")').click()
  await page.waitForTimeout(2000)
  const modal = page.locator('.ant-modal-content:visible').last()
  const modalText = (await modal.innerText()).replace(/\s+/g, '')
  check('围栏弹窗标题=电子围栏', modalText.includes('电子围栏'), modalText.slice(0, 40))
  check('围栏弹窗含编码/名称/类型/绑定对象/状态字段', ['围栏编码', '围栏名称', '围栏类型', '绑定对象', '状态'].every(f => modalText.includes(f)), '')
  const fenceCodeVal = await modal.locator('input[placeholder="留空自动生成"]').inputValue()
  check('新增弹窗自动带出围栏编码', /^WL/.test(fenceCodeVal), fenceCodeVal)
  await modal.locator('input[placeholder="请输入围栏名称"]').fill(FENCE_NAME + 'UI')
  // 地图拾取中心点
  await modal.locator('.fence-form .coord-row button').first().click()
  await page.waitForTimeout(300)
  const modalMap = modal.locator('.fence-map svg')
  const mBox = await modalMap.boundingBox()
  await page.mouse.click(mBox.x + mBox.width * 0.5, mBox.y + mBox.height * 0.5)
  await page.waitForTimeout(600)
  const mCenterLat = await modal.locator('.fence-form input[placeholder="纬度"]').inputValue()
  check('围栏中心点支持地图拾取', Number(mCenterLat) > 0, mCenterLat)
  check('圆形围栏地图渲染圆心+半径', (await modal.locator('.fence-map svg .fence-circle').count()) > 0)
  await modal.locator('button:has-text("保存")').click()
  await page.waitForTimeout(2500)
  text = await body()
  check('新增圆形围栏后台账出现该围栏', text.includes(FENCE_NAME + 'UI'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-fence-list.png'), fullPage: true })

  // 地图绘制多边形围栏
  await page.locator('button:has-text("新增围栏")').click()
  await page.waitForTimeout(2000)
  const modal2 = page.locator('.ant-modal-content:visible').last()
  await modal2.locator('.ant-radio-wrapper:has-text("多边形")').click()
  await page.waitForTimeout(500)
  await modal2.locator('input[placeholder="请输入围栏名称"]').fill(FENCE_POLY_NAME + 'UI')
  await modal2.locator('button:has-text("地图绘制")').click()
  await page.waitForTimeout(500)
  const modalMap2 = modal2.locator('.fence-map svg')
  const mBox2 = await modalMap2.boundingBox()
  for (const [dx, dy] of [[0.35, 0.35], [0.65, 0.35], [0.65, 0.65], [0.35, 0.65]]) {
    await page.mouse.click(mBox2.x + mBox2.width * dx, mBox2.y + mBox2.height * dy)
    await page.waitForTimeout(300)
  }
  const polyText = await modal2.locator('textarea[placeholder^="顶点串"]').inputValue()
  check('地图绘制多边形写入顶点串', String(polyText).split(';').filter(Boolean).length === 4, polyText)
  check('地图渲染多边形填充', (await modal2.locator('.fence-map svg .fence-polygon').count()) > 0)
  await modal2.locator('button:has-text("保存")').click()
  await page.waitForTimeout(2500)
  text = await body()
  check('新增多边形围栏后台账出现该围栏', text.includes(FENCE_POLY_NAME + 'UI'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-fence-polygon.png'), fullPage: true })

  // 选择围栏 → 围栏落图 + 点位校验
  const checkFenceSelect = page.locator('.fence-check-pane .ant-select').first()
  await checkFenceSelect.click()
  await page.waitForTimeout(800)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').first().click()
  await page.waitForTimeout(1200)
  const geoShapes = (await page.locator('.fence-map-pane svg .fence-circle').count())
    + (await page.locator('.fence-map-pane svg .fence-polygon').count())
    + (await page.locator('.fence-map-pane svg .fence-polygon-line').count())
  check('选中围栏后地图渲染几何范围', geoShapes > 0, `shapes=${geoShapes}`)
  await page.locator('.fence-check-pane input[placeholder="纬度"]').fill('30.26')
  await page.locator('.fence-check-pane input[placeholder="经度"]').fill('120.16')
  // antd 对「纯两字中文且无图标」的按钮会插入空格（检 查），故用正则匹配
  await page.locator('.fence-check-pane button').filter({ hasText: /检\s*查/ }).first().click()
  await page.waitForTimeout(2000)
  text = await body()
  check('围栏校验返回在围栏内/外结论', text.includes('在围栏内') || text.includes('在围栏外'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-fence-check.png'), fullPage: true })

  // 行内停用 + 删除
  const UI_FENCE = FENCE_POLY_NAME + 'UI'
  const row = page.locator('.ss-grid tbody tr', { hasText: UI_FENCE }).first()
  wait: {
    if (await row.count() === 0) {
      check('围栏行内操作行可见', false, '未找到行')
      break wait
    }
    await row.locator('button').filter({ hasText: /停\s*用/ }).first().click()
    await page.waitForTimeout(700)
    await page.locator('.ant-modal-confirm:visible button').filter({ hasText: /确\s*定/ }).last().click()
    await page.waitForTimeout(2500)
    text = await body()
    check('围栏行内停用生效', text.includes('已停用'), '')
    check('停用后默认(已启用)过滤下该行不再显示', !text.includes(UI_FENCE), '')

    // 切「已停用」找回该行并删除（同时验证状态筛选落库口径）
    await page.locator('.search-item').filter({ hasText: '状态' }).locator('.ant-select').first().click()
    await page.waitForTimeout(600)
    await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: '已停用' }).first().click()
    await page.waitForTimeout(2500)
    const row2 = page.locator('.ss-grid tbody tr', { hasText: UI_FENCE }).first()
    check('已停用过滤可查到该围栏', (await row2.count()) > 0, `rows=${await row2.count()}`)
    if (await row2.count() > 0) {
      await row2.locator('button').filter({ hasText: /删\s*除/ }).first().click()
      await page.waitForTimeout(700)
      await page.locator('.ant-modal-confirm:visible button:has-text("确认删除")').last().click()
      await page.waitForTimeout(2500)
      text = await body()
      check('围栏行内删除生效', !text.includes(UI_FENCE), '')
    }
  }

  // ── 配置中心《配送参数》：敏感配置脱敏展示 + 留空语义 ──
  await openPage(`${FE}/dms/config-params`)
  // 配置项较多（各模块共用同一张配置表），先按关键字过滤出地图配置再断言
  const cfgSearch = page.locator('input[placeholder="配置键 / 描述"]').first()
  if (await cfgSearch.count() > 0) {
    await cfgSearch.fill('map.amap.api-key')
    await page.locator('button').filter({ hasText: /查\s*询/ }).first().click()
    await page.waitForTimeout(2000)
  }
  text = await body()
  check('《配送参数》可见地图配置键', text.includes('map.amap.api-key'), text.slice(0, 100))
  check('《配送参数》标记敏感配置', text.includes('敏感') && text.includes('未配置'), text.slice(text.indexOf('map.amap.api-key'), text.indexOf('map.amap.api-key') + 60))
  const cfgRow = page.locator('tbody tr', { hasText: 'map.amap.api-key' }).first()
  if (await cfgRow.count() > 0) {
    await cfgRow.locator('button').filter({ hasText: /编\s*辑/ }).first().click()
    await page.waitForTimeout(1200)
    const cfgModal = page.locator('.ant-modal-content:visible').last()
    const cfgModalText = (await cfgModal.innerText()).replace(/\s+/g, '')
    const secretPlaceholder = await cfgModal.locator('textarea').first().getAttribute('placeholder')
    check('配置编辑弹窗提示「留空 = 不修改」（输入框占位 + 提示文案）',
      String(secretPlaceholder || '').includes('留空表示不修改') && cfgModalText.includes('留空保存'),
      `${secretPlaceholder} | ${cfgModalText.slice(0, 90)}`)
    await page.screenshot({ path: path.join(SHOTS, 'ui-config-secret.png'), fullPage: true })
    await cfgModal.locator('.ant-modal-close').first().click()
    await page.waitForTimeout(600)
  } else {
    check('配置编辑弹窗提示「留空表示不修改」', false, '未找到 map.amap.api-key 行')
  }

  // ── P2：变更历史抽屉（审计 + 回滚） ──
  if (await cfgRow.count() > 0) {
    await page.locator('button').filter({ hasText: /历\s*史/ }).first().click()
    await page.waitForTimeout(1800)
    const drawer = page.locator('.ant-drawer-content:visible').last()
    const drawerText = (await drawer.innerText()).replace(/\s+/g, '')
    check('《配送参数》变更历史抽屉可打开并含回滚入口',
      drawerText.includes('变更历史') && (drawerText.includes('回滚到此') || drawerText.includes('旧值')),
      drawerText.slice(0, 120))
    await page.screenshot({ path: path.join(SHOTS, 'ui-config-history.png'), fullPage: true })
    await page.locator('.ant-drawer-close:visible').first().click()
    await page.waitForTimeout(600)
  }

  // ── P2：无效 JS Key → 底图加载失败自动回退（不阻断页面） ──
  await rawConfigValue('PUT', '/dms/config/map.amap.js-key?tenantId=0', 'e2e-invalid-jskey', TOKEN)
  try {
    await openPage(`${FE}/dms/route`)
    await page.waitForTimeout(12000)
    const fbText = (await page.locator('body').innerText()).replace(/\s+/g, '')
    check('无效 JS Key：底图加载失败并自动回退矢量画布',
      fbText.includes('底图：') && (fbText.includes('回退') || fbText.includes('矢量'))
        && (await page.locator('.route-map svg').count()) > 0,
      fbText.slice(fbText.indexOf('底图：'), fbText.indexOf('底图：') + 60))
    await page.screenshot({ path: path.join(SHOTS, 'ui-basemap-fallback.png'), fullPage: true })
  } finally {
    await api('DELETE', '/dms/config/map.amap.js-key?tenantId=0')
  }

  await browser.close()
  return ids
}

/** 清理本次验收产生的围栏（按名称前缀精确匹配本轮 STAMP，不触碰其它数据） */
async function cleanupFences() {
  let removed = 0
  for (const kw of [FENCE_NAME, FENCE_POLY_NAME]) {
    const p = data(await api('GET', `/dms/route/fence/page?pageNum=1&pageSize=100&keyword=${kw}`))
    for (const r of (p?.records || [])) {
      await api('DELETE', `/dms/route/fence/${r.id}`)
      removed++
    }
  }
  console.log(`  [清理] 删除本轮验收围栏 ${removed} 条`)
}

;(async () => {
  let ids = {}
  try {
    await login()
    console.log('登录成功')
    ids = await apiSuite()
    try {
      await uiSuite(ids)
    } catch (e) {
      check('UI 验收执行', false, e.message)
      console.error(e)
    }
  } catch (e) {
    check('主流程', false, e.message)
    console.error(e)
  }

  try {
    await cleanupFences()
  } catch (e) {
    console.log('  [清理] 失败: ' + e.message)
  }

  const pass = results.filter(r => r.ok).length
  console.log(`\n═══ 验收汇总：${pass}/${results.length} ═══`)
  results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  fs.writeFileSync(path.join(SHOTS, 'result.json'), JSON.stringify(results, null, 2))
  process.exit(pass === results.length ? 0 : 1)
})()
