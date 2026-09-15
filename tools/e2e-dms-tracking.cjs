/*
 * 配送跟踪（配送 → 配送跟踪 → 配送跟踪，菜单 80870 `dms:tracking`）金标准端到端验证
 *   · API 验收：台账分页多条件（配送员/任务/时间/来源/速度阈值/仅有关联任务/左面板在线·离线·今日/人工状态）/
 *               联查配送员与任务 / 方向与来源字典 / 速度异常标记 / 段里程 / 里程聚合（配送员·任务·日）/
 *               轨迹抽稀（首尾必留）/ 位置上报（精度·地址·来源）/ 导出真实 xlsx / 旧接口兼容 /
 *               §3.5.5 异常联动预警（超速·异常停留推《人员核验》+ 去重）/ §3.5.4 查询审计留痕（sys_oper_log）
 *   · UI 验收：左配送员过滤面板 + 表头 + 工具栏 + 查询区（含快捷时间）+ 里程摘要 +
 *               列配置 / 页面配置 双弹窗 + 勾选后「在地图中查看」抽屉（轨迹折线/里程/点数）
 *
 *   ⚠️ 左面板「在线/离线」= 位置上报心跳口径（last_report_time 在 dms.tracking.online.minutes 内），
 *      与《配送员管理》《实时跟踪》一致；不是人工状态 dms_rider.status（该口径保留为 riderStatus 参数）。
 *
 * 前置（每次跑之前整文件执行一次，重置 31 个轨迹点）：
 *   python - <<'EOF'
 *   import psycopg2; c=psycopg2.connect("host=localhost port=5432 dbname=devdb user=devuser password=devuser123")
 *   c.autocommit=True; c.cursor().execute(open("tools/e2e-dms-tracking-user.sql",encoding="utf-8").read()); c.close()
 *   EOF
 *   验收后端实例：java -jar <fat jar> --server.port=5665
 *
 * 用法：node tools/e2e-dms-tracking.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5665)
const SHOTS = 'I:/AI-Ready/tool-results/dms-tracking'
const XLSX = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx')

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const RIDER_A = '2099000000000006010'
const RIDER_B = '2099000000000006011'
/// 位置上报专用配送员（不参与计数断言，保证脚本可重复执行）
const RIDER_C = '2099000000000006012'
/// 异常联动预警专用配送员（种子含 1 个 20 分钟前的停留点；姓名不含「E2E跟踪」故不进任何计数断言）
const RIDER_D = '2099000000000006013'
const TASK_ID = '2099000000000006100'

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

const data = (res) => (res.json ? res.json.data : null)

const E2E_USER = process.env.E2E_USER || 'e2e_tracking'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

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

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 1) 台账：总数与联查
  const p1 = await api('GET', '/dms/tracking/page?pageNum=1&pageSize=50')
  const total = Number(data(p1)?.total)
  check('台账返回种子轨迹（≥30 点；并行会话可能另有写入）', p1.json?.code === 200 && total >= 30, `total=${total}`)
  const rec0 = (data(p1)?.records || [])[0] || {}
  const recs = data(p1)?.records || []
  check('默认按上报时间倒序',
    recs.length > 1 && String(recs[0].reportTime) >= String(recs[1].reportTime),
    `${recs[0]?.reportTime} ≥ ${recs[1]?.reportTime}`)
  const aPoint = (data(p1)?.records || []).find(r => String(r.riderId) === RIDER_A) || {}
  check('联查配送员姓名/电话', aPoint.riderName === 'E2E跟踪配送员A' && aPoint.riderPhone === '13900003001',
    `${aPoint.riderName}/${aPoint.riderPhone}`)
  check('联查任务编号/客户', aPoint.taskNo === 'E2ETRK-01' && aPoint.customerName === 'E2E跟踪客户',
    `${aPoint.taskNo}/${aPoint.customerName}`)
  check('方向字典（八方位）', ['北', '东北', '东', '东南', '南', '西南', '西', '西北'].includes(aPoint.directionText),
    `${aPoint.direction}/${aPoint.directionText}`)
  check('来源字典（APP上报/后台补录/渠道回传）',
    ['APP上报', '后台补录', '渠道回传'].includes(aPoint.sourceText), aPoint.sourceText)
  check('经纬度 6 位小数返回', aPoint.lng === 116.4 && String(aPoint.lat).startsWith('39.'), `${aPoint.lng}/${aPoint.lat}`)
  check('段间里程派生（同页相邻点 > 0）',
    (data(p1)?.records || []).some(r => Number(r.segmentMeters) > 0),
    `max=${Math.max(...(data(p1)?.records || []).map(r => Number(r.segmentMeters) || 0))}`)
  const fast = (data(p1)?.records || []).find(r => Number(r.speed) === 80)
  check('速度异常标记（80 km/h）', fast?.speedAbnormal === true, `${fast?.speed}/${fast?.speedAbnormal}`)

  // 2) 多条件过滤
  const cases = [
    ['配送员', `riderId=${RIDER_A}`, 20],
    ['任务ID', `taskId=${TASK_ID}`, 20],
    ['任务编号（模糊）', 'taskNo=E2ETRK', 20],
    ['配送员手机号（模糊）', 'riderKeyword=13900003002', 10],
    ['来源=APP', 'sources=1&riderKeyword=E2E跟踪', 16],
    ['来源=后台补录+渠道', 'sources=2,3&riderKeyword=E2E跟踪', 14],
    ['速度≥60', 'minSpeed=60&riderKeyword=E2E跟踪', 1],
    ['仅显示有任务关联的点', 'onlyWithTask=true&riderKeyword=E2E跟踪', 20],
    ['左面板：在线配送员（心跳口径）', 'online=true&riderKeyword=E2E跟踪', 20],
    ['左面板：离线配送员（心跳口径）', 'online=false&riderKeyword=E2E跟踪', 10],
    ['左面板：今日有上报', 'activeToday=true&riderKeyword=E2E跟踪', 20],
    ['人工状态=空闲（riderStatus 保留可用）', 'riderStatus=1&riderKeyword=E2E跟踪', 20],
  ]
  for (const [name, qs, expect] of cases) {
    const r = await api('GET', `/dms/tracking/page?pageNum=1&pageSize=50&${qs}`)
    const t = Number(data(r)?.total)
    check(`台账按${name}过滤`, r.json?.code === 200 && t === expect, `total=${t}/期望 ${expect}`)
  }
  const today = new Date().toISOString().slice(0, 10)
  const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10)
  const tToday = await api('GET', `/dms/tracking/page?pageNum=1&pageSize=50&riderKeyword=E2E跟踪&startTime=${today} 00:00:00&endTime=${today} 23:59:59`)
  check('台账按今日时间范围过滤', Number(data(tToday)?.total) === 20, data(tToday)?.total)
  const tYest = await api('GET', `/dms/tracking/page?pageNum=1&pageSize=50&riderKeyword=E2E跟踪&startTime=${yesterday}&endTime=${yesterday}`)
  check('台账按昨日日期过滤（yyyy-MM-dd 自动补时分秒）', Number(data(tYest)?.total) === 10, data(tYest)?.total)
  const badTime = await api('GET', '/dms/tracking/page?startTime=2026/09/13')
  check('非法时间格式被拒', badTime.json?.code !== 200, badTime.json?.message)
  const sortRes = await api('GET', '/dms/tracking/page?pageNum=1&pageSize=5&sortField=speed&sortOrder=asc')
  const sortRows = data(sortRes)?.records || []
  check('服务端排序（速度升序）', sortRows.length > 1
    && sortRows.every((r, i) => i === 0 || Number(sortRows[i - 1].speed) <= Number(r.speed)),
    sortRows.map(r => r.speed).join(' ≤ '))

  // 3) 里程聚合
  const mRider = await api('GET', `/dms/tracking/mileage?groupBy=rider&riderId=${RIDER_A}`)
  const mRows = data(mRider) || []
  check('里程聚合（配送员维度）返回 1 组', mRows.length === 1 && Number(mRows[0].pointCount) === 20,
    JSON.stringify(mRows[0]).slice(0, 160))
  const km = Number(mRows[0]?.mileageKm)
  check('里程数值合理（19 段 ×≈111 米 ≈ 2.1 km）', km > 2.0 && km < 2.2, km)
  check('里程分组名=配送员姓名', mRows[0]?.groupName === 'E2E跟踪配送员A', mRows[0]?.groupName)
  const mTask = await api('GET', `/dms/tracking/mileage?groupBy=task&taskId=${TASK_ID}`)
  check('里程聚合（任务维度）', (data(mTask) || []).length === 1 && data(mTask)[0].groupName === 'E2ETRK-01',
    JSON.stringify(data(mTask)?.[0]).slice(0, 120))
  const mDay = await api('GET', '/dms/tracking/mileage?groupBy=day&riderKeyword=E2E跟踪')
  check('里程聚合（日维度）2 组（今日 + 昨日）', (data(mDay) || []).length === 2,
    (data(mDay) || []).map(r => `${r.groupKey}:${r.mileageKm}km`).join(','))
  const mBad = await api('GET', '/dms/tracking/mileage?groupBy=xxx')
  check('非法聚合维度被拒', mBad.json?.code !== 200, mBad.json?.message)

  // 4) 轨迹抽稀
  const full = await api('GET', `/dms/tracking/track-vo?riderId=${RIDER_A}`)
  check('按配送员取轨迹（全量 20 点）', (data(full) || []).length === 20, (data(full) || []).length)
  const thin = await api('GET', `/dms/tracking/track-vo?riderId=${RIDER_A}&maxPoints=5`)
  const thinRows = data(thin) || []
  check('轨迹抽稀生效（≤6 点）', thinRows.length <= 6 && thinRows.length >= 2, thinRows.length)
  check('抽稀保留首尾点', thinRows.length >= 2
    && Number(thinRows[0].lat) === 39.9
    && Number(thinRows[thinRows.length - 1].lat) === 39.919,
    `${thinRows[0]?.lat} → ${thinRows[thinRows.length - 1]?.lat}`)
  const byTask = await api('GET', `/dms/tracking/task-vo/${TASK_ID}`)
  check('按任务取轨迹', (data(byTask) || []).length === 20, (data(byTask) || []).length)

  // 5) 位置上报（扩展字段）
  const before = Number(data(await api('GET', '/dms/tracking/page?pageNum=1&pageSize=1'))?.total)
  const rep = await api('POST', `/dms/tracking/report?riderId=${RIDER_C}&taskId=${TASK_ID}&lat=39.9205&lng=116.4005&speed=25&direction=95&accuracy=4.2&address=${encodeURIComponent('E2E上报地址')}&source=3`)
  check('位置上报成功（含精度/地址/来源）', rep.json?.code === 200, rep.json?.message)
  const after = Number(data(await api('GET', '/dms/tracking/page?pageNum=1&pageSize=1'))?.total)
  check('上报后台账总数 +1', after === before + 1, `${before} → ${after}`)
  const latest = await api('GET', `/dms/tracking/latest/${RIDER_C}`)
  check('最新位置返回精度与来源', Number(data(latest)?.accuracy) === 4.2 && Number(data(latest)?.source) === 3,
    `${data(latest)?.accuracy}/${data(latest)?.source}`)
  const repBad = await api('POST', `/dms/tracking/report?riderId=${RIDER_C}&lng=116.4`)
  check('上报缺纬度被拒', repBad.json?.code !== 200, repBad.json?.message)
  const repBad2 = await api('POST', `/dms/tracking/report?riderId=${RIDER_C}&lat=39.9&lng=116.4&accuracy=-1`)
  check('上报精度为负被拒', repBad2.json?.code !== 200, repBad2.json?.message)

  // 6) 旧接口兼容
  const legacy = await api('GET', `/dms/tracking/track?riderId=${RIDER_A}&startTime=${today}T00:00:00&endTime=${today}T23:59:59`)
  check('旧接口 /track 兼容（ISO 时间）', (data(legacy) || []).length >= 20, (data(legacy) || []).length)
  const legacyTask = await api('GET', `/dms/tracking/task/${TASK_ID}`)
  check('旧接口 /task/{id} 兼容', (data(legacyTask) || []).length >= 20, (data(legacyTask) || []).length)

  // 7) 导出
  const exp = await api('GET', `/dms/tracking/export?riderId=${RIDER_A}`)
  check('导出返回 xlsx（非 JSON）', exp.buf && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b, `status=${exp.status}`)
  if (exp.buf && exp.buf[0] === 0x50) {
    fs.writeFileSync(path.join(SHOTS, 'export.xlsx'), exp.buf)
    try {
      const wb = XLSX.read(exp.buf, { type: 'buffer' })
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
      check('导出含上报时间与来源列且行数匹配', rows.length >= 20
        && Object.keys(rows[0]).includes('上报时间') && Object.keys(rows[0]).includes('来源'),
        `${rows.length} 行 / ${JSON.stringify(rows[0] || {}).slice(0, 140)}`)
    } catch (e) {
      check('导出 xlsx 可解析', false, e.message)
    }
  }

  // 8) 异常联动预警（§3.5.5：超速 / 异常停留 → 推《人员核验》预警体系，复用 dms_verification_alert）
  const stayRep = await api('POST', `/dms/tracking/report?riderId=${RIDER_D}&taskId=${TASK_ID}&lat=39.9300&lng=116.4200&speed=12`)
  check('异常停留上报成功（与 20 分钟前的种子点同位置）', stayRep.json?.code === 200, stayRep.json?.message)
  const fastRep = await api('POST', `/dms/tracking/report?riderId=${RIDER_D}&taskId=${TASK_ID}&lat=39.9400&lng=116.4300&speed=95`)
  check('超速上报成功（95 km/h）', fastRep.json?.code === 200, fastRep.json?.message)

  const stayPage = await api('GET', '/dms/verification/alert/page?page=1&size=50&alertType=2')
  const stayHits = (data(stayPage)?.records || []).filter(a => String(a.riderId) === RIDER_D)
  check('异常停留已推《人员核验》预警（type=2）', stayHits.length === 1,
    `${stayHits.length} 条 / ${stayHits[0]?.alertContent || ''}`)
  const fastPage = await api('GET', '/dms/verification/alert/page?page=1&size=50&alertType=3')
  const fastHits = (data(fastPage)?.records || []).filter(a => String(a.riderId) === RIDER_D)
  check('超速已推《人员核验》预警（type=3）', fastHits.length === 1,
    `${fastHits.length} 条 / ${fastHits[0]?.alertContent || ''}`)
  check('预警带位置与关联任务（口径可追溯）',
    Number(fastHits[0]?.riderLat) === 39.94 && String(fastHits[0]?.taskId) === TASK_ID,
    `${fastHits[0]?.riderLat}/${fastHits[0]?.taskId}`)
  await api('POST', `/dms/tracking/report?riderId=${RIDER_D}&taskId=${TASK_ID}&lat=39.9500&lng=116.4400&speed=99`)
  const fastPage2 = await api('GET', '/dms/verification/alert/page?page=1&size=50&alertType=3')
  check('预警去重（同人同类型仍有未处理时不重复产生）',
    (data(fastPage2)?.records || []).filter(a => String(a.riderId) === RIDER_D).length === 1,
    (data(fastPage2)?.records || []).filter(a => String(a.riderId) === RIDER_D).length)

  // 9) 查询审计（§3.5.4：轨迹属敏感数据，「谁查了谁的轨迹」须留痕 → sys_oper_log）
  let auditRows = []
  for (let i = 0; i < 12; i++) {
    // ⚠️ 中文模块名必须原样拼进 query：rawReq 里的 encodeURI 会再编码一次，预先 encodeURIComponent 会变成双重编码 → 查不到
    const r = await api('GET', '/system/log/oper/page?page=1&pageSize=100&module=配送跟踪')
    auditRows = data(r)?.records || []
    if (auditRows.length > 0) break
    await new Promise(res => setTimeout(res, 500))
  }
  const auditUrls = auditRows.map(r => `${r.action}:${r.requestUrl}`)
  check('台账/明细查询已留痕 sys_oper_log（module=配送跟踪）', auditRows.length > 0, `${auditRows.length} 条`)
  check('审计含操作人 username（可回答「谁查的」）',
    auditRows.every(r => !!r.username), auditRows[0]?.username)
  check('台账分页留痕 action=QUERY',
    auditRows.some(r => r.action === 'QUERY' && String(r.requestUrl).includes('/dms/tracking/page')),
    auditUrls.slice(0, 3).join(' | '))
  check('轨迹明细查询留痕（track-vo / task-vo / replay）',
    auditRows.some(r => String(r.requestUrl).includes('/track-vo') || String(r.requestUrl).includes('/task-vo')
      || String(r.requestUrl).includes('/replay')), '')
  check('导出台账留痕 action=EXPORT',
    auditRows.some(r => r.action === 'EXPORT' && String(r.requestUrl).includes('/dms/tracking/export')), '')
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

  /// 记录台账分页请求（用于断言左面板切换确实带上了对应过滤参数）
  const netLog = []
  page.on('request', r => {
    const url = decodeURIComponent(r.url())
    if (url.includes('/api/dms/tracking/page')) netLog.push(url.split('/api')[1])
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
      await page.waitForSelector('.ss-grid th', { timeout: 60000 }).catch(() => {})
      await page.waitForTimeout(1000)
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }

  const bodyText = () => page.locator('body').innerText().then(t => t.replace(/\s+/g, ''))

  await openPage(`${FE}/dms/tracking`)
  const txt0 = await bodyText()
  check('页面可打开（无 404/白屏）', !txt0.includes('页面不存在') && txt0.length > 50, page.url())
  for (const n of ['全部配送员', '在线', '离线', '今日有上报']) {
    check(`左面板含「${n}」`, txt0.includes(n))
  }
  const headers = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['上报时间', '配送员', '任务编号', '经度', '纬度', '速度(km/h)', '方向', '来源']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  for (const q of ['配送员', '任务编号', '上报时间', '来源', '速度≥', '仅显示有任务关联的点', '快捷时间', '今日', '昨日', '近7日', '查询', '重置']) {
    check(`查询区含「${q}」`, txt0.includes(q))
  }
  for (const b of ['在地图中查看', '刷新', '导出']) {
    check(`工具栏含「${b}」`, txt0.includes(b))
  }
  check('里程摘要展示（合计里程）', txt0.includes('合计里程'), txt0.slice(0, 100))
  check('列表出现轨迹行（配送员/任务编号）', txt0.includes('E2E跟踪配送员') && txt0.includes('E2ETRK-01'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 列配置齿轮
  const gearCount = await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length)
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)
  await page.locator('.ss-grid .th-settings-btn').first().click()
  await page.waitForTimeout(1500)
  const colPanel = page.locator('.ant-modal-content:visible').last()
  const colText = (await colPanel.innerText()).replace(/\s+/g, '')
  check('列配置弹窗含「个人配置/全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 60))
  check('列配置含轨迹列（含隐藏列：定位精度/地址）',
    ['上报时间', '配送员', '经度', '来源', '定位精度(米)', '地址'].every(c => colText.includes(c)), colText.slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 页面配置
  await page.locator('button[title="页面配置"]').first().click()
  await page.waitForTimeout(1200)
  const pageCfg = page.locator('.ant-modal-content:visible').last()
  const cfgText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置弹窗含「查询条件/功能按钮」双 Tab', cfgText.includes('查询条件') && cfgText.includes('功能按钮'), cfgText.slice(0, 60))
  await pageCfg.locator('.ant-tabs-tab:has-text("功能按钮")').click()
  await page.waitForTimeout(700)
  const btnText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置含功能按钮项（在地图中查看/刷新/导出）',
    btnText.includes('在地图中查看') && btnText.includes('刷新') && btnText.includes('导出'), btnText.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 左面板过滤：切到「离线」应只剩离线骑手的点。
  // ⚠️ 共享 devdb 里并行的《实时跟踪》E2E 也在写轨迹点（`E2E实时骑手O*`，同为离线），
  //    离线结果集会被它们占满，故先用「配送员=E2E跟踪配送员B」限定范围再切面板，
  //    并断言切换确实带上了心跳口径参数 online=false（不依赖环境的干净程度）。
  await page.locator('.search-item:has-text("配送员") .ant-select-selector').first().click()
  await page.waitForTimeout(400)
  await page.keyboard.type('E2E跟踪配送员B', { delay: 30 })
  await page.waitForTimeout(900)
  await page.locator('.ant-select-item-option', { hasText: 'E2E跟踪配送员B' }).first().click()
  await page.waitForTimeout(2500)
  netLog.length = 0
  await page.locator('.category-panel .ant-tree-node-content-wrapper', { hasText: '离线' }).first().click()
  await page.waitForTimeout(2500)
  check('左面板「离线」按心跳口径重查（请求带 online=false）',
    netLog.some(u => u.includes('online=false')), netLog.join(' | ').slice(0, 160))
  const gridTxt = await page.locator('.ss-grid').innerText().then(t => t.replace(/\s+/g, ''))
  check('左面板「离线」+配送员B：仅保留 B、不含在线配送员 A',
    gridTxt.includes('E2E跟踪配送员B') && !gridTxt.includes('E2E跟踪配送员A'), gridTxt.slice(0, 120))
  await page.screenshot({ path: path.join(SHOTS, 'ui-panel-offline.png'), fullPage: true })
  // 复位（重置会同时清掉「配送员=B」筛选与面板 key；后续用例依赖回到全量）
  // ⚠️ antd 对纯两字中文按钮插空格（重 置），故按结构取第一行查询区最后一个按钮
  await page.locator('.search-row').first().locator('button').last().click()
  await page.waitForTimeout(2500)

  // 勾选一行 → 在地图中查看
  await page.locator('.ss-grid tbody tr').filter({ hasText: 'E2E跟踪配送员A' }).first()
    .locator('input.ss-checkbox, input[type=checkbox]').first().click()
  await page.waitForTimeout(800)
  check('勾选后出现已选提示', (await bodyText()).includes('已选'), '')
  await page.locator('button:has-text("在地图中查看")').first().click()
  await page.waitForTimeout(3000)
  const drawer = page.locator('.ant-drawer-content:visible').last()
  const drawerText = (await drawer.innerText()).replace(/\s+/g, '')
  check('地图抽屉打开（轨迹标题 + 里程 + 点数）',
    drawerText.includes('轨迹地图') && drawerText.includes('里程') && drawerText.includes('点数'), drawerText.slice(0, 120))
  check('地图抽屉含回放按钮', drawerText.includes('轨迹回放'), drawerText.slice(0, 160))
  const polyPoints = await page.evaluate(() => document.querySelectorAll('.ant-drawer-content svg polyline, .ant-drawer-content svg path').length)
  check('地图折线已渲染（svg polyline/path）', polyPoints > 0, `count=${polyPoints}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-map-drawer.png'), fullPage: true })
  await page.locator('.ant-drawer-close').first().click()
  await page.waitForTimeout(1000)

  // 快捷时间「今日」
  // ⚠️ antd 对纯两字中文按钮会插空格（今 日），按结构定位快捷时间按钮
  await page.locator('.quick-row button').first().click()
  await page.waitForTimeout(2500)
  check('快捷时间「今日」过滤生效', (await bodyText()).includes('E2E跟踪配送员A'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-quick-today.png'), fullPage: true })

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await login()
    console.log('登录成功（' + E2E_USER + '）')
    const probe = await api('GET', '/dms/tracking/page?pageNum=1&pageSize=1')
    if (Number(data(probe)?.total) < 30) {
      throw new Error('种子轨迹缺失：请先执行 tools/e2e-dms-tracking-user.sql（见脚本头部说明）')
    }
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
