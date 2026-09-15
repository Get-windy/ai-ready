/*
 * 实时跟踪（配送 → 配送跟踪 → 实时跟踪，菜单 80740 `dms:realtime-tracking`）金标准端到端验证
 *   · API 验收：配送员位置分页聚合（在线态/今日里程/今日单量/在途负载/车辆快照，替代 N+1）/
 *               统计卡后端聚合 / 异常预警**四类**（超速·异常停留·**偏航**·超时在途）/ 轨迹回放点序列（抽稀）/
 *               在线阈值**配置化**反证（脚本用 SQL 把边界骑手 last_report_time 精确置为约 140 秒前 →
 *               租户阈值 3 分钟判在线，默认 2 分钟会误判离线；不受环境准备耗时影响）/
 *               **合规**（采集时段改窄→上报被拒；400 天前旧点按保留策略清理）
 *   · SSE 验收：`GET /tracking/stream` 建连 + 收到 `locations` 帧且含 riderId/lat/lng
 *   · UI 验收：左在线状态分组 + 统计卡 + 矢量地图（轨迹折线）+ 双 Tab 明细表（配送员位置 / 轨迹点明细）
 *               + 列配置 / 页面配置双弹窗 + 异常预警抽屉 + 全屏地图抽屉 + 自动刷新开关 + 推送状态
 *
 * 前置（每次跑之前整文件执行一次）：
 *   python - <<'EOF'
 *   import psycopg2; c=psycopg2.connect("host=localhost port=5432 dbname=devdb user=devuser password=devuser123")
 *   c.autocommit=True; c.cursor().execute(open("tools/e2e-dms-realtime-tracking-user.sql",encoding="utf-8").read()); c.close()
 *   EOF
 *   验收后端实例：java -jar <fat jar> --server.port=<空闲端口>（用 ERP_PORT 传给脚本；5665 可能被并行会话占用）
 *
 * 用法：ERP_PORT=5675 FE_URL=http://localhost:5691 node tools/e2e-dms-realtime-tracking.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5665)
const SHOTS = 'I:/AI-Ready/tool-results/dms-realtime-tracking'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

/** 直连数据库执行 SQL（种子时间/合规配置的反证需要精确写入） */
function sqlExec(sql) {
  const py = [
    'import psycopg2',
    'c=psycopg2.connect("host=localhost port=5432 dbname=devdb user=devuser password=devuser123")',
    'c.autocommit=True',
    'c.cursor().execute(' + JSON.stringify(sql) + ')',
    'c.close()',
  ].join('\n')
  execFileSync('python', ['-c', py], { stdio: 'pipe' })
}

const RIDER_O1 = '2099000000000008010'
const RIDER_O2 = '2099000000000008011'
const RIDER_O3 = '2099000000000008012'
const TASK_ID = '2099000000000008200'
/** 收敛到本页种子数据的过滤词（并行会话也会写 dms_rider / dms_tracking） */
const SCOPE = 'E2E实时骑手'

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

const E2E_USER = process.env.E2E_USER || 'e2e_realtime'
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

  // 0) 校准「最近上报时间」：种子时间是相对值，环境准备耗时会让其漂移（尤其 O3 必须落在
  //    120s~180s 之间才能反证「租户阈值 3 分钟 ≠ 默认 2 分钟」）。直接用 SQL 精确落值，
  //    不新增轨迹点（保持「今日里程 ≈ 2.11 km / 今日点数」断言稳定）。
  sqlExec(`
    UPDATE dms_rider SET last_report_time = now()                        WHERE id = ${RIDER_O1};
    UPDATE dms_rider SET last_report_time = now() - interval '30 seconds' WHERE id = ${RIDER_O2};
    UPDATE dms_rider SET last_report_time = now() - interval '140 seconds' WHERE id = ${RIDER_O3};
    UPDATE dms_config SET config_value = '00:00-23:59:59', update_time = now()
      WHERE tenant_id = 1 AND config_key = 'dms.tracking.collect.hours' AND deleted = 0;
  `)

  // 1) 配送员位置分页聚合
  const p1 = await api('GET', `/dms/tracking/rider-page?pageNum=1&pageSize=50&keyword=${SCOPE}`)
  const rows = data(p1)?.records || []
  check('位置分页返回种子配送员 4 名', p1.json?.code === 200 && rows.length === 4, rows.length)
  const o1 = rows.find(r => String(r.riderId) === RIDER_O1) || {}
  const o2 = rows.find(r => String(r.riderId) === RIDER_O2) || {}
  const o3 = rows.find(r => String(r.riderId) === RIDER_O3) || {}
  const n1 = rows.find(r => String(r.riderId) === '2099000000000008013') || {}
  check('O1 返回最新位置（经纬度）', Number(o1.lat) > 39.9 && Number(o1.lng) === 116.4, `${o1.lat}/${o1.lng}`)
  check('O1 在线（just now 上报）', o1.online === true, `${o1.online}/${o1.lastReportAgoSeconds}s`)
  check('O1 今日里程 ≈ 2.11 km', Number(o1.todayMileageKm) > 2.0 && Number(o1.todayMileageKm) < 2.2, o1.todayMileageKm)
  check('O1 今日轨迹点数 ≥ 20（含刷新上报的 1 点）', Number(o1.todayPointCount) >= 20, o1.todayPointCount)
  check('O1 在途负载 = 1（超时任务）', Number(o1.activeTasks) === 1, o1.activeTasks)
  check('O1 车辆快照（京A88888）', o1.vehicleName === '京A88888', o1.vehicleName)
  check('O2 工作状态文案（忙碌）', o2.statusText === '忙碌', o2.statusText)
  check('在线阈值读租户配置 = 3 分钟（非硬编码默认 2）', Number(o1.onlineMinutes) === 3, o1.onlineMinutes)
  check('无上报记录判为离线（N1 lastReportTime 为空）', n1.online === false, `${n1.online}`)
  check('N1 无位置数据（无经纬度）', n1.lat == null && Number(n1.todayMileageKm) === 0,
    `${n1.lat}/${n1.todayMileageKm}`)
  check('O3 按租户阈值 3 分钟判在线（约 140 秒 > 默认 120 秒，反证配置生效）', o3.online === true,
    `${o3.online}/${o3.lastReportAgoSeconds}s/阈值${o3.onlineMinutes}分`)

  const onlyOnline = await api('GET', `/dms/tracking/rider-page?pageNum=1&pageSize=50&keyword=${SCOPE}&onlineOnly=true`)
  check('仅看在线过滤（刷新后 O1/O2 在线）', (data(onlyOnline)?.records || []).length >= 2,
    (data(onlyOnline)?.records || []).length)
  const onlyOffline = await api('GET', `/dms/tracking/rider-page?pageNum=1&pageSize=50&keyword=${SCOPE}&status=0`)
  check('按工作状态过滤（离线/休息 → N1）', (data(onlyOffline)?.records || []).length === 1,
    (data(onlyOffline)?.records || []).length)
  const byPhone = await api('GET', '/dms/tracking/rider-page?pageNum=1&pageSize=50&keyword=13900004002')
  check('按手机号检索配送员', (data(byPhone)?.records || []).length === 1, (data(byPhone)?.records || []).length)

  // 2) 统计卡（后端聚合）
  const st = await api('GET', '/dms/tracking/stat')
  const s = data(st) || {}
  check('统计：在线数 ≥ 2', Number(s.onlineCount) >= 2, s.onlineCount)
  check('统计：在线=总数-离线', Number(s.riderTotal) - Number(s.offlineCount) === Number(s.onlineCount),
    `${s.riderTotal}-${s.offlineCount}=${s.onlineCount}`)
  check('统计：有位置 ≥ 2 且 无位置 ≥ 1', Number(s.withLocationCount) >= 2 && Number(s.noLocationCount) >= 1,
    `${s.withLocationCount}/${s.noLocationCount}`)
  check('统计：在途任务 ≥ 1', Number(s.activeTaskCount) >= 1, s.activeTaskCount)
  check('统计：超时在途 ≥ 1', Number(s.overdueTaskCount) >= 1, s.overdueTaskCount)
  check('统计：今日轨迹点 ≥ 23', Number(s.todayPointCount) >= 23, s.todayPointCount)
  check('统计：在线阈值读配置=3 分钟', Number(s.onlineMinutes) === 3, s.onlineMinutes)
  check('统计：异常预警数 ≥ 3', Number(s.alertCount) >= 3, s.alertCount)

  // 3) 异常预警
  const al = await api('GET', '/dms/tracking/alerts')
  const alerts = data(al) || []
  const overspeed = alerts.find(a => a.alertType === 'OVERSPEED' && String(a.riderId) === RIDER_O1)
  const stay = alerts.find(a => a.alertType === 'STAY' && String(a.riderId) === RIDER_O2)
  const overdue = alerts.find(a => a.alertType === 'OVERDUE' && String(a.taskId) === TASK_ID)
  check('预警-超速（O1 88 km/h）', !!overspeed && Number(overspeed.value) === 88, JSON.stringify(overspeed || {}).slice(0, 160))
  check('预警-超速带配送员与时间', !!overspeed?.riderName && !!overspeed?.eventTime, overspeed?.riderName)
  check('预警-异常停留（O2 同坐标 20 分钟）', !!stay && Number(stay.value) >= 15, JSON.stringify(stay || {}).slice(0, 160))
  check('预警-异常停留说明含位移', String(stay?.alertText || '').includes('位移'), stay?.alertText)
  check('预警-超时在途（E2ERT-01）', !!overdue && overdue.taskNo === 'E2ERT-01', JSON.stringify(overdue || {}).slice(0, 160))
  check('预警按时间倒序返回', alerts.length > 1 && String(alerts[0].eventTime) >= String(alerts[1].eventTime),
    `${alerts[0]?.eventTime} / ${alerts[1]?.eventTime}`)
  const deviation = alerts.find(a => a.alertType === 'DEVIATION' && String(a.riderId) === RIDER_O1)
  check('预警-偏航（O1 偏离「起点→客户」基线 > 1000 米）', !!deviation && Number(deviation.value) >= 1000,
    JSON.stringify(deviation || {}).slice(0, 160))

  // 4) 回放点序列（抽稀）
  const rp = await api('GET', `/dms/tracking/replay?riderId=${RIDER_O1}`)
  check('回放返回 O1 全量点（≥20）', (data(rp) || []).length >= 20, (data(rp) || []).length)
  const rpThin = await api('GET', `/dms/tracking/replay?riderId=${RIDER_O1}&maxPoints=5`)
  const thin = data(rpThin) || []
  check('回放抽稀（≤6 点且首尾保留）', thin.length <= 6 && thin.length >= 2
    && Number(thin[0].lat) === 39.9 && Number(thin[thin.length - 1].lat) === 39.919,
    `${thin.length} 点 ${thin[0]?.lat}→${thin[thin.length - 1]?.lat}`)
  const rpTask = await api('GET', `/dms/tracking/replay?taskId=${TASK_ID}`)
  check('回放按任务取轨迹（≥20 点）', (data(rpTask) || []).length >= 20, (data(rpTask) || []).length)
  const rpBad = await api('GET', '/dms/tracking/replay?riderId=')
  check('回放缺 riderId 被拒', rpBad.json?.code !== 200, rpBad.json?.message)

  // 5) 合规：采集时段限制 + 保留策略
  sqlExec(`UPDATE dms_config SET config_value='00:00-00:01', update_time=now()
           WHERE tenant_id=1 AND config_key='dms.tracking.collect.hours' AND deleted=0;`)
  const denied = await api('POST', `/dms/tracking/report?riderId=${RIDER_O3}&lat=39.930000&lng=116.420000&speed=10&source=1`)
  check('合规-采集时段外上报被拒', denied.json?.code !== 200, denied.json?.message)

  sqlExec(`UPDATE dms_config SET config_value='00:00-23:59:59', update_time=now()
           WHERE tenant_id=1 AND config_key='dms.tracking.collect.hours' AND deleted=0;`)
  const allowed = await api('POST', `/dms/tracking/report?riderId=${RIDER_O3}&lat=39.930000&lng=116.420000&speed=10&source=1`)
  check('合规-时段内上报放行', allowed.json?.code === 200, allowed.json?.message)

  sqlExec(`INSERT INTO dms_tracking (id, tenant_id, rider_id, lat, lng, speed, report_time, source, deleted, create_time, update_time, version)
           VALUES (2099000000000008150, 1, ${RIDER_O1}, 39.900000, 116.400000, 0,
                   now() - interval '400 days', 1, 0, now() - interval '400 days', now(), 1)
           ON CONFLICT (id) DO UPDATE SET report_time = now() - interval '400 days', create_time = now() - interval '400 days';`)
  const cleaned = await api('POST', '/dms/tracking/clean-expired?days=365')
  check('合规-保留策略按天数清理（返回删除条数 ≥ 1）', cleaned.json?.code === 200 && Number(cleaned.json?.data) >= 1,
    JSON.stringify(cleaned.json?.data))
}

// ══════════════ 三、实时推送（SSE）验收 ══════════════
async function sseSuite() {
  console.log('\n═══ 三、实时推送（SSE）验收 ═══')
  const got = await new Promise(resolve => {
    const req = http.request({
      hostname: 'localhost', port: PORT, path: '/api/dms/tracking/stream', method: 'GET',
      headers: { Authorization: `Bearer ${TOKEN}`, Accept: 'text/event-stream' },
    }, res => {
      if (res.statusCode !== 200) {
        resolve({ status: res.statusCode, text: '' })
        req.destroy()
        return
      }
      let buf = ''
      const timer = setTimeout(() => { req.destroy(); resolve({ status: 200, text: buf }) }, 20000)
      res.on('data', chunk => {
        buf += chunk.toString('utf8')
        // SSE 帧可能分多个 TCP 分片到达：必须等到 locations 帧以空行收尾，data 才完整
        const idx = buf.indexOf('event:locations')
        if (idx >= 0 && buf.indexOf('\n\n', idx) > 0) {
          clearTimeout(timer)
          req.destroy()
          resolve({ status: 200, text: buf })
        }
      })
      res.on('error', () => { clearTimeout(timer); resolve({ status: 200, text: buf }) })
    })
    req.on('error', err => { if (err.code !== 'ECONNRESET') resolve({ status: 0, text: '', error: err.message }) })
    req.end()
  })

  const text = got.text || ''
  check('SSE 连接建立（HTTP 200 + 事件流）', got.status === 200 && text.includes('event:'), `${got.status}${got.error ? '/' + got.error : ''}`)
  check('SSE 推送在线配送员位置帧（locations）', text.includes('event:locations'), text.slice(0, 120).replace(/\n/g, '|'))
  check('SSE 帧含 riderId/lat/lng 字段', text.includes('riderId') && text.includes('lat') && text.includes('lng'), '')
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
      await page.waitForTimeout(1200)
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }

  const bodyText = () => page.locator('body').innerText().then(t => t.replace(/\s+/g, ''))

  await openPage(`${FE}/dms/realtime-tracking`)
  // 并行会话有 160+ 配送员：先按关键词收敛到本页种子数据，后续断言才稳定
  await page.locator('.search-area input[placeholder="姓名/手机号"]').first().fill(SCOPE)
  // ⚠️ antd 对纯两字中文按钮会插空格（查 询），用结构选择器（查询是查询区唯一的 primary）
  await page.locator('.search-area button.ant-btn-primary').first().click()
  await page.waitForTimeout(3000)
  const txt0 = await bodyText()
  check('页面可打开（无 404/白屏）', !txt0.includes('页面不存在') && txt0.length > 50, page.url())
  check('已移除「未接入地图」能力边界声明', !txt0.includes('后端暂无轨迹分页接口'), '')
  for (const n of ['全部配送员', '在线', '离线']) {
    check(`左面板含「${n}」`, txt0.includes(n))
  }
  for (const c of ['配送员总数', '无位置', '超时在途', '异常预警']) {
    check(`统计卡含「${c}」`, txt0.includes(c))
  }
  const headers = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['配送员', '在线状态', '当前位置', '最近上报', '今日里程(km)', '在途负载']) {
    check(`配送员位置表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  for (const q of ['配送员', '在线状态', '仅看在线', '回放时间', '查询', '重置']) {
    check(`查询区含「${q}」`, txt0.includes(q))
  }
  check('工具栏含自动刷新开关', (await page.locator('.auto-refresh .ant-switch').count()) > 0, '')
  for (const b of ['异常预警', '全屏地图', '刷新', '导出']) {
    check(`工具栏含「${b}」`, txt0.includes(b))
  }
  check('工具栏含实时推送状态（SSE/兜底轮询）', txt0.includes('实时推送'), '')
  check('列表出现种子配送员', txt0.includes('E2E实时骑手O1'), '')
  check('地图区已渲染（矢量画布/空态）', txt0.includes('实时位置'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 列配置齿轮
  const gearCount = await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length)
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)
  await page.locator('.ss-grid .th-settings-btn').first().click()
  await page.waitForTimeout(1500)
  const colPanel = page.locator('.ant-modal-content:visible').last()
  const colText = (await colPanel.innerText()).replace(/\s+/g, '')
  check('列配置弹窗含「个人配置/全局配置」双 Tab', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 60))
  check('列配置含配送员位置列（含隐藏列：车辆/定位精度）',
    ['配送员', '在线状态', '今日里程(km)', '车辆', '定位精度(米)'].every(c => colText.includes(c)), colText.slice(0, 180))
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
  check('页面配置含功能按钮项（异常预警/全屏地图/自动刷新）',
    btnText.includes('异常预警') && btnText.includes('全屏地图') && btnText.includes('自动刷新'), btnText.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 查看轨迹 → 地图折线
  await page.locator('.ss-grid tbody tr', { hasText: 'E2E实时骑手O1' }).first()
    .locator('button:has-text("查看轨迹")').click()
  await page.waitForTimeout(3000)
  const afterPick = await bodyText()
  check('选中配送员后地图标题切换为轨迹回放', afterPick.includes('轨迹回放'), '')
  check('已选提示出现', afterPick.includes('已选：E2E实时骑手O1'), '')
  const polyCount = await page.locator('svg polyline').count()
  check('轨迹折线已渲染（svg polyline）', polyCount > 0, `polyline=${polyCount}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-replay.png'), fullPage: true })

  // 切到「轨迹点明细」Tab
  await page.locator('.detail-tabs .ant-tabs-tab:has-text("轨迹点明细")').click()
  await page.waitForTimeout(2000)
  const pointHeaders = await page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  check('轨迹点明细表头切换（上报时间/来源）',
    pointHeaders.some(x => x.includes('上报时间')) && pointHeaders.some(x => x.includes('来源')), pointHeaders.join('/'))
  const pointTxt = await bodyText()
  check('轨迹点明细出现来源字典（APP上报）', pointTxt.includes('APP上报'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-point-tab.png'), fullPage: true })

  // 回放播放/重置
  // 地图工具条按钮顺序：[播放, 重置]（两字按钮文案含空格，不能按文案定位）
  const playBtn = page.locator('.map-toolbar button').first()
  check('轨迹回放按钮可用（已加载轨迹点）', await playBtn.isEnabled(),
    `disabled=${!(await playBtn.isEnabled())}`)
  if (await playBtn.isEnabled()) {
    await playBtn.click()
    await page.waitForTimeout(1200)
    check('回放播放按钮切换为暂停', (await bodyText()).includes('暂停'), '')
  }
  await page.locator('.map-toolbar button').nth(1).click()
  await page.waitForTimeout(600)

  // 异常预警抽屉
  await page.locator('button:has-text("异常预警")').first().click()
  await page.waitForTimeout(2500)
  const alertDrawer = page.locator('.ant-drawer-content:visible').last()
  const alertText = (await alertDrawer.innerText()).replace(/\s+/g, '')
  check('预警抽屉打开并含三类预警',
    alertText.includes('超速') && alertText.includes('异常停留') && alertText.includes('超时在途'), alertText.slice(0, 160))
  check('预警抽屉含任务编号 E2ERT-01', alertText.includes('E2ERT-01'), alertText.slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-alerts.png'), fullPage: true })
  await page.locator('.ant-drawer-close').first().click()
  await page.waitForTimeout(1000)

  // 全屏地图抽屉
  await page.locator('button:has-text("全屏地图")').first().click()
  await page.waitForTimeout(2500)
  const mapDrawer = page.locator('.ant-drawer-content:visible').last()
  const mapText = (await mapDrawer.innerText()).replace(/\s+/g, '')
  check('全屏地图抽屉打开', mapText.includes('全屏地图'), mapText.slice(0, 80))
  const fsPoly = await mapDrawer.locator('svg polyline, svg circle').count()
  check('全屏地图渲染配送员标记/轨迹', fsPoly > 0, `count=${fsPoly}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-fullscreen-map.png'), fullPage: true })
  await page.locator('.ant-drawer-close').first().click()
  await page.waitForTimeout(1000)

  // 左面板「在线」过滤
  await page.locator('.category-panel .ant-tree-node-content-wrapper', { hasText: '在线' }).first().click()
  await page.waitForTimeout(2500)
  const onlineTxt = await bodyText()
  check('左面板「在线」过滤生效（不含无位置骑手 N1）', !onlineTxt.includes('E2E实时骑手N1'), onlineTxt.slice(0, 80))
  await page.screenshot({ path: path.join(SHOTS, 'ui-panel-online.png'), fullPage: true })

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await login()
    console.log('登录成功（' + E2E_USER + '）')
    const probe = await api('GET', `/dms/tracking/rider-page?pageNum=1&pageSize=50&keyword=${SCOPE}`)
    if ((data(probe)?.records || []).length < 4) {
      throw new Error('种子配送员缺失：请先执行 tools/e2e-dms-realtime-tracking-user.sql（见脚本头部说明）')
    }
    await apiSuite()
    await sseSuite()
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
