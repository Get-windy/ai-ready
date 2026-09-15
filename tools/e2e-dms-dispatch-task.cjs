/*
 * 调度任务（配送 → 调度管理 → 调度任务，菜单 80730 `dms:dispatch-task`）金标准端到端验证
 *
 *   · 接口验收：多条件分页（13 类条件 + 显式红冲状态 + 服务端排序 + 联查富化）/ 候选配送员（约束与拒绝原因、
 *               车辆·坐标·负载·累计单量）/ 指派·改派（负载上限·载重·休息中硬门控 + 调度审计留痕）/
 *               批量指派·批量取消（逐单结果反馈）/ 取消·异常（审计 + 释放配送员）/
 *               交付即释放运力（提交签收 → 释放；驳回 → 重新占用；名下无在途才释放，仍有在途不得误释放）/ 批量打印（回写打印次数）/
 *               调度审计时间线 / 超时升级扫描（换人重派 + 超时告警）/ 自动调度（dryRun 不落库 → 真执行）/
 *               真实 xlsx 导出 / 未登录 401
 *   · UI 验收：CategoryListLayout + BillTableList（序号齿轮列配置 dms-dispatch-task-columns）+ PageConfigPanel
 *               （查询条件 / 功能按钮两 Tab）/ 13 条查询条件 / 金标准列 / 工具栏 8 按钮 / 行级操作 /
 *               指派弹窗（可派过滤 + 不可派禁用 + 拒绝原因）/ 批量指派·批量取消·结果报告 / 调度审计弹窗 /
 *               地图派单弹窗（复用 RouteMapCanvas）/ 状态多选查询 / 页面无 4xx
 *
 * 前置（每次跑之前整文件执行一次）：
 *   python - <<'EOF'
 *   import psycopg2; c=psycopg2.connect("host=localhost port=5432 dbname=devdb user=devuser password=devuser123")
 *   c.autocommit=True; c.cursor().execute(open("tools/e2e-dms-dispatch-task-user.sql",encoding="utf-8").read()); c.close()
 *   EOF
 *   验收后端实例：java -jar dispatch-task-run.jar --server.port=5665   （前端 vite 代理到 5655，本脚本直连 5665）
 *
 * 用法：node tools/e2e-dms-dispatch-task.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5665)
const SHOTS = 'I:/AI-Ready/tool-results/dms-dispatch-task'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

// ── 种子 ID（见 tools/e2e-dms-dispatch-task-user.sql） ──
const R1 = '2099000000000009210'   // 空闲·最近 → 指派首选
const R2 = '2099000000000009211'   // 空闲·较远（已有在途）
const R3 = '2099000000000009212'   // 休息中 → 拒绝
const R4 = '2099000000000009213'   // 并接已满 → 拒绝
const R5 = '2099000000000009214'   // 未实名 → 拒绝
const R6 = '2099000000000009215'   // 无定位 → 拒绝
const R7 = '2099000000000009216'   // 空闲 → 超时升级承接/批量指派
const V1 = '2099000000000009220'
const T301 = '2099000000000009301'
const T302 = '2099000000000009302'
const T303 = '2099000000000009303'
const T304 = '2099000000000009304'
const T305 = '2099000000000009305'
const T306 = '2099000000000009306'
const T307 = '2099000000000009307'
const T308 = '2099000000000009308'
const T309 = '2099000000000009309'
const T310 = '2099000000000009310'
const T311 = '2099000000000009311'
const TBUSY1 = '2099000000000009332'   // Rider D 名下 5 单在途之一（配送中；种子 generate_series 实际生成 9332~9336）
const TDONE1 = '2099000000000009341'   // Rider E 名下唯一在途单（配送中；未实名 → 不会被并行会话抢派）

function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? null : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        resolve({ status: res.statusCode, buf, json: tryJson(buf) })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function tryJson(buf) { try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null } }

let TOKEN = null
/** 派单策略原始值（用例期间临时放宽并接上限，**整轮结束**再还原，UI 段同样需要宽上限） */
let CFG_BEFORE = null
const E2E_USER = process.env.E2E_USER || 'e2e_dispatch_task'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  TOKEN = token
}

async function api(method, reqPath, body) {
  let r = await rawReq(method, reqPath, body, TOKEN)
  if (r.status === 401 || (r.json && Number(r.json.code) === 401)) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN)
  }
  return r
}
const data = (res) => (res.json ? res.json.data : null)

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 180) : ''}`)
}

/**
 * 勾选指定任务行并返回批量操作条文案
 *
 * 逐行点击勾选框（ss-grid 自绘 checkbox，受控组件），等待批量条出现；
 * 「已选择 N 项」同时是**勾选残留**的哨兵：换查询条件后若旧勾选没被清掉，N 会偏大。
 */
async function selectRows(page, taskNos) {
  const bar = page.locator('.batch-bar')
  // 弹窗关闭后可能残留透明遮罩：force 点击会落在遮罩上而**静默无效**，
  // 所以采用「勾选 → 校验批量条 → 按 Esc 关残留层 → 重试」的稳妥写法。
  for (let attempt = 0; attempt < 3; attempt++) {
    for (const no of taskNos) {
      const row = page.locator('.ss-grid tbody tr', { hasText: no }).first()
      const box = row.locator('input.ss-checkbox, input[type=checkbox]').first()
      if (!(await box.isChecked().catch(() => false))) {
        await box.click({ force: true })
        await page.waitForTimeout(500)
      }
    }
    if (await bar.count()) break
    await page.keyboard.press('Escape')
    await page.waitForTimeout(700)
  }
  await bar.waitFor({ state: 'visible', timeout: 8000 }).catch(() => {})
  return (await bar.innerText().catch(() => '')).replace(/\n/g, ' ')
}

/**
 * 点「最上层弹窗」的确认按钮（DOM 层派发，绕开 Playwright 的可点击性检查）
 *
 * 共享环境里常有历史弹窗遮罩，导致 click 被拦截（intercepts pointer events）；
 * 原生 DOM click 不受遮挡影响，且浏览器对 disabled 按钮不派发事件 → 不会误提交。
 */
async function confirmTopModal(page) {
  await page.evaluate(() => {
    const wraps = [...document.querySelectorAll('.ant-modal-wrap')]
      .filter((w) => w instanceof HTMLElement && w.style.display !== 'none')
    const last = wraps[wraps.length - 1]
    if (!(last instanceof HTMLElement)) return
    const btns = last.querySelectorAll('.ant-modal-footer button, .ant-modal-confirm-btns button')
    const ok = btns[btns.length - 1]
    if (ok instanceof HTMLElement) {
      ok.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
    }
  })
  await page.waitForTimeout(400)
}

/**
 * 轮询直到「当前可见弹窗」的文本命中关键字
 *
 * 候选列表 200+ 行，渲染有快有慢；固定 sleep 会把空态/加载态当成结果断言（假失败）。
 */
async function waitForModalText(page, pattern, timeout = 20000) {
  const deadline = Date.now() + timeout
  let text = ''
  while (Date.now() < deadline) {
    text = await page.locator('.ant-modal-wrap:visible .ant-modal-content').last().innerText().catch(() => '')
    if (pattern.test(text)) return text
    await page.waitForTimeout(500)
  }
  return text
}

/**
 * 关掉所有可见弹窗（含无关闭按钮的 Modal.confirm 用 Esc）
 *
 * 共享环境里一次误触/中断就可能留下上一轮的弹窗，后续 click 会被遮罩拦截（表现为「点了没反应」）。
 * 每个可能打开弹窗的步骤前先调一次，失败时把可见弹窗标题打出来便于定位。
 */
/**
 * DOM 层强制关闭所有可见弹窗
 *
 * Playwright 的 click 会被「动画中 / 被遮挡 / 无关闭按钮」挡下（表现为 Element is not visible
 * 或 intercepts pointer events），而原生 DOM click 一定能触发 Vue 的关闭处理。
 */
async function domCloseVisibleModals(page) {
  await page.evaluate(() => {
    document.querySelectorAll('.ant-modal-wrap').forEach((w) => {
      const el = w
      if (el instanceof HTMLElement && el.style.display !== 'none') {
        const btn = el.querySelector('.ant-modal-close')
        if (btn instanceof HTMLElement) {
          btn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
        }
      }
    })
  })
  await page.waitForTimeout(600)
}

async function closeAllModals(page) {
  for (let i = 0; i < 5; i++) {
    if (!(await page.locator('.ant-modal-wrap:visible').count())) return
    await domCloseVisibleModals(page)
    if (await page.locator('.ant-modal-wrap:visible').count()) {
      await page.keyboard.press('Escape').catch(() => {})
      await page.waitForTimeout(700)
    }
  }
  if (await page.locator('.ant-modal-wrap:visible').count()) {
    console.log('  [警告] 仍有可见弹窗未能关闭：' + await visibleModalTitles(page))
  }
}

/** 可见弹窗标题（失败诊断用） */
async function visibleModalTitles(page) {
  const titles = await page.locator('.ant-modal-wrap:visible .ant-modal-title').allInnerTexts().catch(() => [])
  const confirms = await page.locator('.ant-modal-wrap:visible .ant-modal-confirm-title').allInnerTexts().catch(() => [])
  return [...titles, ...confirms].map(t => t.replace(/\s+/g, '')).filter(Boolean).join(' / ')
}

/** 关闭当前可见的最上层弹窗（DOM 中可能残留已隐藏的历史弹窗，不能用 .last()） */
async function closeTopModal(page) {
  if (!page) return
  await domCloseVisibleModals(page)
}

/** 刷新种子配送员心跳：在线口径 = 最近位置上报（默认 2 分钟），UI 段耗时较长需多次重置 */
async function refreshRiderHeartbeats() {
  await dbQuery("UPDATE dms_rider SET last_report_time = now()"
    + " WHERE id BETWEEN 2099000000000009210 AND 2099000000000009216")
}

async function pageRows(query) {
  const r = await api('GET', `/dms/task/page?current=1&size=50&${query}`)
  return data(r)?.records || []
}
async function taskById(id) {
  const r = await api('GET', `/dms/task/${id}`)
  return data(r) || {}
}

/**
 * 取一个「当前可派」的配送员
 *
 * 共享库里种子配送员随时可能被其他会话的自动调度占满（并接上限）或置于离线，
 * 因此凡是要「指派成功」的用例都不能写死配送员，改为从候选里动态挑第一个可派的
 * （优先本脚本自己的种子配送员，便于断言身份）。
 */
async function pickFreeRider(taskId) {
  const list = data(await api('GET', `/dms/dispatch/candidates?taskId=${taskId}`)) || []
  const mine = list.find(c => c.eligible && String(c.riderId).startsWith('20990000000000092'))
  const hit = mine || list.find(c => c.eligible)
  return hit ? String(hit.riderId) : null
}

// ══════════════════════════════════════════════════════════
// 一、接口验收
// ══════════════════════════════════════════════════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // ── 1. 鉴权 ──
  const anon = await rawReq('GET', '/dms/task/page?current=1&size=5')
  check('未登录访问被拒（401）', anon.status === 401 || Number(anon.json?.code) === 401, `status=${anon.status}`)

  // ── 2. 多条件分页 ──
  const base = await api('GET', '/dms/task/page?current=1&size=10&taskNo=E2EDPT')
  const baseD = data(base) || {}
  check('分页返回 records/total（默认隐藏已取消 → 10 条）',
    base.json?.code === 200 && Array.isArray(baseD.records) && Number(baseD.total) === 10,
    `total=${baseD.total}`)
  check('任务编号模糊查询（E2EDPT 命中 11 条）', (baseD.records || []).length > 0 && (baseD.records || []).every(r => (r.taskNo || '').includes('E2EDPT')),
    `rows=${(baseD.records || []).length}`)

  const t1 = await pageRows('taskNo=E2EDPT-01')
  check('按任务编号精确检索', t1.length === 1 && t1[0].taskNo === 'E2EDPT-01', `rows=${t1.length}`)
  const t1rec = t1[0] || {}
  check('列表联查富化：负载/距离/超时字段存在',
    t1rec.activeTaskCount !== undefined && 'distanceKm' in t1rec && 'overdue' in t1rec && 'riderPhone' in t1rec,
    JSON.stringify({ load: t1rec.activeTaskCount, km: t1rec.distanceKm, overdue: t1rec.overdue }))

  const byOrder = await pageRows('orderNo=E2EDPTO-02')
  check('按订单号检索', byOrder.length === 1 && byOrder[0].taskNo === 'E2EDPT-02', `rows=${byOrder.length}`)

  const byCust = await pageRows('customerName=客户甲')
  check('按客户名称检索', byCust.length >= 1 && byCust.every(r => (r.customerName || '').includes('客户甲')), `rows=${byCust.length}`)

  const byVehicle = await pageRows(`vehicleId=${V1}`)
  check('按配送车辆检索（T301 绑京E2E001）', byVehicle.length === 1 && byVehicle[0].taskNo === 'E2EDPT-01', `rows=${byVehicle.length}`)

  const byRider = await pageRows(`riderId=${R2}`)
  const myR2Tasks = ['E2EDPT-04', 'E2EDPT-05', 'E2EDPT-06', 'E2EDPT-08', 'E2EDPT-10']
  check('按配送员检索（R2 名下 5 单齐全，且结果全属于该配送员）',
    byRider.every(r => String(r.riderId) === R2) && myR2Tasks.every(no => byRider.some(r => r.taskNo === no)),
    `rows=${byRider.length}: ${byRider.map(r => r.taskNo).join(',')}`)

  const byStatus = await pageRows('statusList=0,1')
  check('状态多选（0,1）只含待分配/已分配', byStatus.length > 0 && byStatus.every(r => [0, 1].includes(Number(r.status))),
    `rows=${byStatus.length}/status=${[...new Set(byStatus.map(r => r.status))].join(',')}`)

  const cancelled = await pageRows('statusList=7')
  check('显式筛选「已取消」不被红冲口径吞掉', cancelled.length >= 1 && cancelled.every(r => Number(r.status) === 7),
    `rows=${cancelled.length}`)
  const cancelledDefault = await pageRows('taskNo=E2EDPT-07')
  check('默认仍隐藏已取消（红冲口径保留）', cancelledDefault.length === 0, `rows=${cancelledDefault.length}`)

  const byPriority = await pageRows('priority=2')
  check('按优先级筛选（紧急）', byPriority.length >= 1 && byPriority.every(r => Number(r.priority) === 2), `rows=${byPriority.length}`)

  const byType = await pageRows('orderType=3&taskNo=E2EDPT')
  check('按订单类型筛选（退货）', byType.length === 1 && byType[0].taskNo === 'E2EDPT-09', `rows=${byType.length}`)

  const byArea = await pageRows('routeArea=E2E区域乙')
  check('按配送区域筛选', byArea.length >= 3 && byArea.every(r => (r.routeArea || '').includes('E2E区域乙')), `rows=${byArea.length}`)

  const unassigned = await pageRows('unassigned=true&taskNo=E2EDPT')
  check('有无配送员=未分配（riderId 全空）', unassigned.length >= 1 && unassigned.every(r => r.riderId == null),
    `rows=${unassigned.length}`)

  const abnormal = await pageRows('abnormal=true&taskNo=E2EDPT')
  check('是否异常=仅异常（status 全 8）', abnormal.length >= 1 && abnormal.every(r => Number(r.status) === 8), `rows=${abnormal.length}`)
  const notAbnormal = await pageRows('abnormal=false&taskNo=E2EDPT')
  check('是否异常=非异常（排除 status 8）', notAbnormal.length >= 1 && notAbnormal.every(r => Number(r.status) !== 8),
    `rows=${notAbnormal.length}/异常命中=${notAbnormal.filter(r => Number(r.status) === 8).length}`)
  const assignedOnly = await pageRows('unassigned=false&taskNo=E2EDPT')
  check('有无配送员=仅已分配（riderId 全非空）', assignedOnly.length >= 1 && assignedOnly.every(r => r.riderId != null),
    `rows=${assignedOnly.length}/空派=${assignedOnly.filter(r => r.riderId == null).length}`)

  const overdue = await pageRows('overdue=true&taskNo=E2EDPT')
  check('超时在途筛选命中 T306（配送中且已过要求送达）',
    overdue.length >= 1 && overdue.some(r => r.taskNo === 'E2EDPT-06') && overdue.every(r => [1, 2, 3, 4].includes(Number(r.status))),
    `rows=${overdue.length}`)

  const dateRange = await pageRows('deliveryDateStart=2000-01-01&deliveryDateEnd=2099-12-31&taskNo=E2EDPT')
  check('配送日期范围查询', dateRange.length === 10, `rows=${dateRange.length}`)

  const sorted = await pageRows('sortField=goodsAmount&sortOrder=asc&taskNo=E2EDPT')
  const amounts = sorted.map(r => Number(r.goodsAmount || 0))
  check('服务端排序（货品金额升序）', amounts.length > 1 && amounts.every((v, i) => i === 0 || amounts[i - 1] <= v),
    JSON.stringify(amounts.slice(0, 5)))

  const enriched = await pageRows(`riderId=${R2}`)
  const withRider = enriched.find(r => r.riderId != null) || {}
  check('已分配任务富化：配送员电话/负载已联查填充',
    !!withRider.riderPhone && Number(withRider.activeTaskCount) >= 0,
    `${withRider.riderName}/${withRider.riderPhone}/load=${withRider.activeTaskCount}`)

  // ── 3. 候选配送员（约束与拒绝原因） ──
  const cand = await api('GET', `/dms/dispatch/candidates?taskId=${T301}`)
  const candList = data(cand) || []
  check('候选配送员接口返回列表', cand.json?.code === 200 && Array.isArray(candList) && candList.length > 0, `count=${candList.length}`)
  const cand1 = candList.find(c => String(c.riderId) === R1) || {}
  check('候选含车牌/坐标/负载/累计单量（地图派单与选人依据）',
    !!cand1.vehicleNo && cand1.currentLat != null && cand1.activeTasks != null && cand1.totalOrders != null,
    JSON.stringify({ v: cand1.vehicleNo, lat: cand1.currentLat, load: cand1.activeTasks, total: cand1.totalOrders }))
  check('可派候选：eligible=true + 有距离与得分',
    cand1.eligible === true && Number(cand1.distanceMeters) > 0 && cand1.score != null,
    `dist=${cand1.distanceMeters}/score=${cand1.score}`)
  check('不可派原因-休息中', (candList.find(c => String(c.riderId) === R3) || {}).reason === '配送员休息中',
    (candList.find(c => String(c.riderId) === R3) || {}).reason)
  check('不可派原因-已达并接上限', /并接上限/.test(String((candList.find(c => String(c.riderId) === R4) || {}).reason)),
    (candList.find(c => String(c.riderId) === R4) || {}).reason)
  check('不可派原因-实名认证未通过', /实名认证未通过/.test(String((candList.find(c => String(c.riderId) === R5) || {}).reason)),
    (candList.find(c => String(c.riderId) === R5) || {}).reason)
  check('不可派原因-无定位数据', /无定位数据/.test(String((candList.find(c => String(c.riderId) === R6) || {}).reason)),
    (candList.find(c => String(c.riderId) === R6) || {}).reason)
  check('候选排序：可派在前且按得分倒序',
    candList.length > 1 && Number(candList[0]?.score) >= Number(candList[candList.length - 1]?.score),
    `first=${candList[0]?.riderName}/last=${candList[candList.length - 1]?.riderName}`)

  const candNoCoord = await api('GET', `/dms/dispatch/candidates?taskId=${T309}`)
  const noCoordList = data(candNoCoord) || []
  check('任务缺取货点坐标 → 有定位的候选给出「缺少取货点坐标」拒绝原因',
    noCoordList.length > 0 && noCoordList.some(c => /缺少取货点坐标/.test(String(c.reason))) &&
    noCoordList.some(c => /无定位数据/.test(String(c.reason))),
    JSON.stringify(noCoordList.slice(0, 3).map(c => [c.riderName, c.reason])))

  // ── 4. 指派（负载/载重/休息中硬门控 + 审计） ──
  const logs0 = await api('GET', `/dms/task/${T301}/logs`)
  check('新任务调度审计初始为空', Array.isArray(data(logs0)) && data(logs0).length === 0, `logs=${(data(logs0) || []).length}`)

  const assign = await api('POST', `/dms/task/${T301}/assign`, { riderId: R1, reason: 'E2E指派' })
  check('指派成功（/dms/task/{id}/assign）', assign.json?.code === 200, assign.json?.message)
  const t301After = await taskById(T301)
  check('指派后状态=已分配(1) + 配送员/名称/派单方式落库',
    Number(t301After.status) === 1 && String(t301After.riderId) === R1 && t301After.riderName === 'E2E调度任务R1',
    `${t301After.status}/${t301After.riderId}/${t301After.riderName}`)
  const t301Row = (await pageRows('taskNo=E2EDPT-01'))[0] || {}
  check('指派后列表 dispatchType=2（手动）且负载+1',
    Number(t301Row.dispatchType) === 2 && Number(t301Row.activeTaskCount) >= 1,
    `type=${t301Row.dispatchType}/load=${t301Row.activeTaskCount}`)
  const logs1 = data(await api('GET', `/dms/task/${T301}/logs`)) || []
  const assignLog = logs1.find(l => l.action === 'ASSIGN') || {}
  check('指派写调度审计（动作/被指派人/原因/操作人）',
    logs1.length === 1 && assignLog.actionText === '指派' && String(assignLog.toRiderId) === R1 && assignLog.reason === 'E2E指派' && !!assignLog.operatorName,
    JSON.stringify({ a: assignLog.actionText, to: assignLog.toRiderName, r: assignLog.reason, op: assignLog.operatorName }))

  const dup = await api('POST', `/dms/task/${T301}/assign`, { riderId: R2 })
  check('重复指派被拒（状态机不允许）', dup.json?.code !== 200 && /状态不允许/.test(String(dup.json?.message)), dup.json?.message)
  const noRider = await api('POST', `/dms/task/${T301}/assign`, {})
  check('指派缺配送员被拒', noRider.json?.code !== 200, noRider.json?.message)
  const rest = await api('POST', `/dms/task/${T303}/assign`, { riderId: R3 })
  check('休息中配送员不可指派（硬门控）', rest.json?.code !== 200 && /休息/.test(String(rest.json?.message)), rest.json?.message)
  // 目标配送员动态选取：共享库里固定配送员可能被其他会话的自动调度占满（并接上限）→ 假失败。
  // 用**正常重量**的 T301 去挑「负载未满」的配送员，再拿去验 T303 的载重拒绝（否则 T303 全候选不可派挑不出人）。
  // 并接上限临时放宽（步骤 3 的「已达并接上限」候选断言已完成，不影响其结论），用例结束还原。
  CFG_BEFORE = data(await api('GET', '/dms/dispatch/strategy')) || {}
  await api('PUT', '/dms/dispatch/strategy', { ...CFG_BEFORE, maxConcurrent: 999 })
  await refreshRiderHeartbeats()
  const freeRider = await pickFreeRider(T301)
  check('取到可用配送员（候选动态选取，免共享库占满假失败）', !!freeRider, `riderId=${freeRider}`)
  const overloadWeight = await api('POST', `/dms/task/${T303}/assign`, { riderId: freeRider })
  check('超载重任务不可指派（载重上限）', overloadWeight.json?.code !== 200 && /载重/.test(String(overloadWeight.json?.message)),
    overloadWeight.json?.message)
  const noCoordAssign = await api('POST', `/dms/task/${T309}/assign`, { riderId: freeRider })
  check('无坐标任务可人工指派（距离/围栏为优化约束，不拦人工）', noCoordAssign.json?.code === 200, noCoordAssign.json?.message)

  // ── 5. 改派 ──
  const reassign = await api('POST', `/dms/task/${T304}/reassign`, { toRiderId: R7, reason: 'E2E改派' })
  check('改派成功（fromRiderId 缺省取当前配送员）', reassign.json?.code === 200, reassign.json?.message)
  const t304After = await taskById(T304)
  check('改派后任务归属新配送员', String(t304After.riderId) === R7 && t304After.riderName === 'E2E调度任务R7',
    `${t304After.riderId}/${t304After.riderName}`)
  const logs304 = data(await api('GET', `/dms/task/${T304}/logs`)) || []
  const reLog = logs304.find(l => l.action === 'REASSIGN') || {}
  check('改派写审计（原配送员 → 新配送员 + 原因）',
    reLog.fromRiderName === 'E2E调度任务R2' && reLog.toRiderName === 'E2E调度任务R7' && reLog.reason === 'E2E改派',
    `${reLog.fromRiderName} → ${reLog.toRiderName} / ${reLog.reason}`)
  const sameRider = await api('POST', `/dms/task/${T304}/reassign`, { toRiderId: R7 })
  check('改派给同一配送员被拒', sameRider.json?.code !== 200 && /相同/.test(String(sameRider.json?.message)), sameRider.json?.message)

  // ── 6. 批量指派 / 批量取消（逐单结果反馈） ──
  // 配送员动态选取：共享库里种子配送员可能被其他会话的自动调度占满（并接上限），固定用 R1 会假失败
  const pickRider = async (taskId) => {
    const list = data(await api('GET', `/dms/dispatch/candidates?taskId=${taskId}`)) || []
    const hit = list.find(c => c.eligible && String(c.riderId).startsWith('20990000000000092'))
      || list.find(c => c.eligible)
    return hit ? String(hit.riderId) : null
  }
  await refreshRiderHeartbeats()
  const batchRider = await pickRider(T302)
  check('批量指派前取到可用配送员（候选动态选取，免共享库占满假失败）', !!batchRider, `riderId=${batchRider}`)
  const batchAssign = await api('POST', '/dms/task/batch-assign', { taskIds: [T302, T303], riderId: batchRider, reason: 'E2E批量指派' })
  const baD = data(batchAssign) || {}
  check('批量指派返回逐单结果（成功/失败/原因）',
    batchAssign.json?.code === 200 && baD.total === 2 && baD.success === 1 && baD.failed === 1 && baD.items?.[0]?.taskNo === 'E2EDPT-03',
    JSON.stringify(baD).slice(0, 200))
  check('批量指派成功项已落库', Number((await taskById(T302)).status) === 1, (await taskById(T302)).riderName)

  const batchCancel = await api('POST', '/dms/task/batch-cancel', { taskIds: [T308, T310], reason: 'E2E批量取消' })
  const bcD = data(batchCancel) || {}
  check('批量取消返回逐单结果（不可取消项带原因）',
    batchCancel.json?.code === 200 && bcD.total === 2 && bcD.failed === 2 && (bcD.items || []).length === 2,
    JSON.stringify(bcD).slice(0, 220))

  // ── 7. 取消（单条）/ 标记异常（审计 + 释放配送员） ──
  const cancel1 = await api('POST', `/dms/task/${T309}/cancel`, { reason: 'E2E取消' })
  check('取消成功且不依赖配送员', cancel1.json?.code === 200, cancel1.json?.message)
  const t309 = await taskById(T309)
  check('取消后状态=已取消(7)', Number(t309.status) === 7, t309.status)
  const logs309 = data(await api('GET', `/dms/task/${T309}/logs`)) || []
  check('取消写审计（动作=取消任务 + 原因 + 原配送员）',
    logs309.some(l => l.action === 'CANCEL' && l.reason === 'E2E取消'
      && l.fromRiderId != null && String(l.fromRiderId) === String(freeRider)),
    JSON.stringify(logs309.map(l => [l.action, l.reason, l.fromRiderId])).slice(0, 160))

  const cancelBusy = await api('POST', `/dms/task/${T302}/cancel`, { reason: 'E2E取消释放运力' })
  check('取消已分配任务成功（释放配送员）', cancelBusy.json?.code === 200, cancelBusy.json?.message)
  const r1Relaxed = await api('GET', '/dms/task/filter-options')
  check('取消后配送资源下拉仍可用（无副作用）', r1Relaxed.json?.code === 200, '')

  const exc = await api('POST', `/dms/task/${T305}/exception`, { reason: 'E2E异常' })
  check('标记异常成功', exc.json?.code === 200, exc.json?.message)
  const t305 = await taskById(T305)
  check('标记异常后状态=异常(8)', Number(t305.status) === 8, t305.status)
  const logs305 = data(await api('GET', `/dms/task/${T305}/logs`)) || []
  check('标记异常写审计', logs305.some(l => l.action === 'EXCEPTION' && l.reason === 'E2E异常'),
    JSON.stringify(logs305.map(l => [l.action, l.reason])).slice(0, 160))

  // ── 7.5 交付即释放运力（2026-09-14 复核修复）──
  // 缺陷：签收审核通过（任务 → 已完成）从不回置配送员 → 永久停在「忙碌(2)」；
  //      而自动派单候选池默认（dms.dispatch.require.online=true）只取「空闲(1)」
  //      → 运力池随完成单量单调收缩，最终「无人可派」。
  // 修复口径：**提交签收（交付动作完成）**即释放（审核是可能滞后的后台动作），
  //          驳回/删除使任务退回在途时重新占用；释放以「名下再无在途单」为条件（不得误释放）。
  const riderStatus = async (riderId) =>
    Number((await dbQuery(`SELECT status FROM dms_rider WHERE id = ${riderId}`))[0]?.status)
  const inflightCount = async (riderId) =>
    Number((await dbQuery('SELECT count(*)::int AS n FROM dms_task'
      + ` WHERE rider_id = ${riderId} AND deleted = 0 AND status IN (1,2,3,4)`))[0]?.n)
  // 自动派单候选池口径（DispatchService.loadRiderPool：空闲 + 在线心跳），命中即「已回到运力池」
  const inAutoPool = async (riderId) => Number((await dbQuery('SELECT count(*)::int AS n FROM dms_rider'
    + ` WHERE id = ${riderId} AND status = 1 AND last_report_time > now() - interval '2 minutes'`))[0]?.n)
  const signSubmit = async (taskId) => {
    const sub = await api('POST', '/dms/sign/submit', {
      taskId, signType: 1, signLat: 39.9087, signLng: 116.3975,
      customerLat: 39.9087, customerLng: 116.3975,
    })
    return { ok: sub.json?.code === 200 && !!data(sub)?.id, signId: data(sub)?.id, reason: sub.json?.message }
  }

  // ① 提交签收（任务 4 → 已签收(5)）即释放运力：审核是后台动作，不该把配送员一直挂在「忙碌」
  const sub1 = await signSubmit(TDONE1)
  const tAfterSubmit = await taskById(TDONE1)
  check('提交签收 → 任务置「已签收(5)」', sub1.ok && Number(tAfterSubmit.status) === 5,
    `ok=${sub1.ok}/status=${tAfterSubmit.status}/${sub1.reason || ''}`)
  check('提交签收即释放运力（名下已无在途单 → 空闲(1) 且回到候选池）',
    (await riderStatus(R5)) === 1 && (await inAutoPool(R5)) === 1,
    `status=${await riderStatus(R5)}/在途=${await inflightCount(R5)}/候选池=${await inAutoPool(R5)}`)

  // ② 审核驳回（任务 5 → 配送中(4)）→ 重新占用运力（交付未成立）
  const reject1 = await api('POST', `/dms/sign/${sub1.signId}/audit`, { auditStatus: 2, auditRemark: 'E2E驳回重签' })
  const tAfterReject = await taskById(TDONE1)
  check('审核驳回 → 任务退回「配送中(4)」', reject1.json?.code === 200 && Number(tAfterReject.status) === 4,
    `ok=${reject1.json?.code}/status=${tAfterReject.status}`)
  check('驳回后重新占用运力（配送员回到「忙碌(2)」）', (await riderStatus(R5)) === 2,
    `status=${await riderStatus(R5)}/在途=${await inflightCount(R5)}`)

  // ③ 重新签收 → 释放；④ 审核通过 → 任务已完成（运力保持已释放）
  const sub2 = await signSubmit(TDONE1)
  check('驳回后可重新签收（新记录）', sub2.ok && String(sub2.signId) !== String(sub1.signId),
    `signId=${sub2.signId}/${sub1.signId}`)
  const approve2 = await api('POST', `/dms/sign/${sub2.signId}/audit`, { auditStatus: 1 })
  const tDone1 = await taskById(TDONE1)
  check('签收审核通过 → 任务置「已完成(6)」', approve2.json?.code === 200 && Number(tDone1.status) === 6,
    `ok=${approve2.json?.code}/status=${tDone1.status}`)
  check('完成后配送员保持「空闲(1)」且在候选池内',
    (await riderStatus(R5)) === 1 && (await inAutoPool(R5)) === 1,
    `status=${await riderStatus(R5)}/在途=${await inflightCount(R5)}/候选池=${await inAutoPool(R5)}`)

  // ⑤ 反向：名下仍有在途单时不得误释放（R4 有 5 单在途，签收其中 1 单并审核通过）
  const subBusy = await signSubmit(TBUSY1)
  const approveBusy = subBusy.ok
    ? await api('POST', `/dms/sign/${subBusy.signId}/audit`, { auditStatus: 1 })
    : { json: { code: 0 } }
  const r4Status = await riderStatus(R4)
  const r4Flight = await inflightCount(R4)
  check('仍有在途单时完成一单 → 保持「忙碌(2)」，不误释放',
    subBusy.ok && approveBusy.json?.code === 200 && r4Status === 2 && r4Flight >= 1,
    `ok=${subBusy.ok}/audit=${approveBusy.json?.code}/在途=${r4Flight}/status=${r4Status}`)
  check('仍有在途单的配送员不在自动派单候选池（忙碌口径）', (await inAutoPool(R4)) === 0,
    `poolRows=${await inAutoPool(R4)}`)

  // ── 8. 批量打印（回写打印次数） ──
  const beforePrint = Number((await taskById(T304)).printCount || 0)
  const batchPrint = await api('POST', '/dms/task/batch-print', { taskIds: [T304] })
  const afterPrint = Number((await taskById(T304)).printCount || 0)
  check('批量打印回写打印次数', batchPrint.json?.code === 200 && afterPrint === beforePrint + 1,
    `before=${beforePrint}/after=${afterPrint}`)

  // ── 9. 超时升级（换人重派 + 超时告警） ──
  const escalate = await api('POST', '/dms/dispatch/escalate-overdue', {})
  const escD = data(escalate) || {}
  check('超时升级扫描返回逐单结果', escalate.json?.code === 200 && Number(escD.total) >= 2,
    `total=${escD.total}/success=${escD.success}/failed=${escD.failed}`)
  check('配送中超时任务进入告警（不重派）',
    (escD.items || []).some(i => String(i.taskId) === T306 && /超时告警/.test(String(i.reason))),
    JSON.stringify((escD.items || []).filter(i => String(i.taskId) === T306)).slice(0, 160))
  const t311 = await taskById(T311)
  const logs311 = data(await api('GET', `/dms/task/${T311}/logs`)) || []
  const escLog = logs311.find(l => l.action === 'ESCALATE') || {}
  check('超时未接单任务被重派并写审计（超时升级）',
    escLog.actionText === '超时升级重派' && /超时未接单/.test(String(escLog.reason)) && t311.riderId != null,
    `${escLog.fromRiderName} → ${escLog.toRiderName} / ${escLog.reason}`)

  // ── 10. 自动调度（dryRun 不落库 → 真执行） ──
  const t307Before = Number((await taskById(T307)).status)
  const dry = await api('POST', '/dms/dispatch/auto', { dryRun: true, maxTasks: 20 })
  check('自动调度 dryRun 不落库', dry.json?.code === 200 && Number((await taskById(T307)).status) === t307Before,
    `status=${t307Before}`)
  const stat = await api('GET', '/dms/dispatch/stat')
  const stD = data(stat) || {}
  check('调度效果统计可用（autoRate/avgDispatchSeconds/overdueRate）',
    stat.json?.code === 200 && stD.autoRate != null && stD.avgDispatchSeconds != null && stD.overdueRate != null,
    `auto=${stD.autoRate}%/avg=${stD.avgDispatchSeconds}s`)

  // ── 11. 导出（真实 xlsx） ──
  const exp = await rawReq('GET', '/dms/task/export?taskNo=E2EDPT', null, TOKEN)
  const isXlsx = exp.buf && exp.buf.length > 1000 && exp.buf.slice(0, 2).toString('utf8') === 'PK'
  check('导出为真实 xlsx（ZIP 头 PK）', isXlsx, `bytes=${exp.buf?.length}`)

  // ── 12. 下拉口径（选择器数据源） ──
  const opts = data(await api('GET', '/dms/task/filter-options')) || {}
  check('配送员/车辆下拉候选非空（禁止手输 ID 的数据源）',
    Array.isArray(opts.drivers) && opts.drivers.length > 0 && Array.isArray(opts.vehicles) && opts.vehicles.length > 0,
    `drivers=${(opts.drivers || []).length}/vehicles=${(opts.vehicles || []).length}`)

}

// ══════════════════════════════════════════════════════════
// 二、UI 验收
// ══════════════════════════════════════════════════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1760, height: 1000 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()
  page.setDefaultTimeout(25000)
  const errs = []
  const http4xx = []
  const apiCalls = []
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('console', m => { if (m.type() === 'error') errs.push('console: ' + m.text().slice(0, 140)) })
  page.on('response', r => {
    const u = r.url()
    if (u.includes('/api/')) {
      if (u.includes('/dms/task') || u.includes('/dms/dispatch')) apiCalls.push(`${r.status()} ${u.split('/api/')[1]}`)
      if (r.status() >= 400) http4xx.push(`${r.status()} ${u.split('/api/')[1]}`)
    }
  })
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', '1') }, [TOKEN])
  await page.goto(`${FE}/dms/dispatch-task`, { waitUntil: 'domcontentloaded' })
  await refreshRiderHeartbeats()
  await page.waitForTimeout(9000)

  // ── 页面骨架 ──
  const body = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开（无 404/白屏）', !body.includes('页面不存在') && body.length > 200, page.url())
  check('骨架：CategoryListLayout 工具栏 + 查询区 + 数据表',
    await page.locator('.category-list-layout').count() > 0 && await page.locator('.search-area').count() > 0,
    '')
  check('无分类树面板（show-category-panel=false）', await page.locator('.category-panel').count() === 0, '')

  // ── 查询条件 13 项 ──
  for (const label of ['任务编号', '订单号', '客户', '配送员', '配送车辆', '状态', '优先级', '类型', '配送日期', '配送线路', '配送区域', '有无配送员', '是否异常']) {
    check(`查询条件含「${label}」`, body.includes(label), '')
  }
  check('查询区为横向网格（search-row flex）', await page.locator('.search-area .search-row').count() === 1, '')
  check('配送员为选择器（禁止手输 ID）', await page.locator('.ant-select').filter({ hasText: '全部配送员' }).count() > 0, '')
  const riderSel = page.locator('.ant-select').filter({ hasText: '全部配送员' }).first()
  if (await riderSel.count()) {
    await riderSel.click()
    await page.waitForTimeout(1200)
    const items = await page.locator('.ant-select-dropdown:visible .ant-select-item-option').allInnerTexts().catch(() => [])
    check('配送员候选含姓名/电话（非裸 ID）',
      items.length > 0 && items.every(t => !/^\d{6,}$/.test(t.trim())) && items.some(t => /（1\d{10}）/.test(t)),
      JSON.stringify(items.slice(0, 2)))
    await page.keyboard.press('Escape')
    await page.waitForTimeout(400)
  }
  check('状态为多选下拉', await page.locator('.ant-select-multiple').count() >= 1, '')

  // ── 工具栏按钮 ──
  const toolbar = await page.locator('.toolbar-section').innerText()
  for (const btn of ['自动调度', '地图派单', '超时升级', '刷新', '打印(F8)', '导出', '调度策略配置']) {
    check(`工具栏含「${btn}」`, toolbar.includes(btn.replace('(', '（').replace(')', '）')) || toolbar.includes(btn), '')
  }
  check('页面配置齿轮入口存在（表头列配置 + 页面配置）',
    await page.locator('.toolbar-section button').filter({ has: page.locator('.anticon-setting') }).count() >= 1, '')

  // ── 表头列（金标准默认列；BillDetailTable 为自绘 ss-grid） ──
  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  const headText = headers.join('|')
  for (const col of ['任务编号', '订单号', '类型', '客户', '收货地址', '配送员', '配送员电话', '数量', '货品金额', '优先级', '状态', '期望送达', '下单时间']) {
    check(`默认列含「${col}」`, headText.includes(col), headText.slice(0, 140))
  }
  check('表头含操作列与列配置齿轮', headText.includes('操作') &&
    await page.evaluate(() => document.querySelectorAll('.ss-grid .th-settings-btn').length) > 0, `cols=${headers.length}`)
  check('默认可见列 = 操作 + 14 业务列（15 个有标题列）', headers.length === 15, `cols=${headers.length}: ${headText}`)
  check('默认隐藏列不出现在表头（距目的地/负载/超时/打印次数等 14 列）',
    !headText.includes('距目的地') && !headText.includes('打印次数') && !headText.includes('负载'), headText.slice(0, 120))

  // ── 查询交互：任务编号检索 ──
  const searchBox = page.locator('.search-area .search-item', { hasText: '任务编号' }).locator('input').first()
  const queryBtn = page.locator('.search-area button').filter({ hasText: /查\s*询/ }).first()
  const rowCount = () => page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT' }).count()

  await searchBox.fill('E2EDPT')
  await queryBtn.click()
  await page.waitForTimeout(2500)
  let rows = await rowCount()
  check('查询后渲染真实数据行', rows > 0, `rows=${rows}`)

  await searchBox.fill('E2EDPT-01')
  await queryBtn.click()
  await page.waitForTimeout(2200)
  rows = await rowCount()
  check('按任务编号精确定位（1 行）', rows === 1, `rows=${rows}`)

  // ── 行级操作 + 调度审计弹窗 ──
  const row1 = page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-01' }).first()
  const rowText = await row1.innerText()
  check('行内含指派/改派/取消/更多操作', /指派|改派/.test(rowText) && /取消|更多/.test(rowText), rowText.replace(/\n/g, ' ').slice(0, 90))

  await row1.locator('a.cell-link').first().click()
  await page.waitForTimeout(2200)
  const logBody = await page.locator('.ant-modal-wrap:visible .ant-modal-content').last().innerText()
  check('点击任务编号打开调度审计时间线', logBody.includes('调度审计') && /指派|改派|取消/.test(logBody), logBody.replace(/\n/g, ' ').slice(0, 140))
  await closeTopModal(page)
  await page.waitForTimeout(800)

  // ── 状态多选筛选（显式查已取消） ──
  await searchBox.fill('')
  const statusSel = page.locator('.search-area .search-item', { hasText: '状态' }).locator('.ant-select').first()
  await statusSel.click()
  await page.waitForTimeout(900)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: '已取消' }).first().click()
  await page.waitForTimeout(400)
  await page.keyboard.press('Escape')
  await queryBtn.click()
  await page.waitForTimeout(2400)
  check('状态多选「已取消」筛出 E2EDPT-07（显式状态不被红冲吞掉）',
    await page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-07' }).count() >= 1, '')
  // 还原状态筛选
  await statusSel.click()
  await page.waitForTimeout(700)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: '已取消' }).first().click()
  await page.waitForTimeout(300)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)

  // ── 指派弹窗（候选过滤 + 不可派禁用 + 拒绝原因） ──
  await searchBox.fill('E2EDPT-04')
  await queryBtn.click()
  await page.waitForTimeout(2300)
  const t304Row = page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-04' }).first()
  check('E2EDPT-04 为已分配（可改派）', (await t304Row.innerText()).includes('改派'), (await t304Row.innerText()).replace(/\n/g, ' ').slice(0, 80))
  await refreshRiderHeartbeats()
  await t304Row.locator('button').filter({ hasText: '改派' }).first().click()
  await page.waitForTimeout(2600)
  const assignModal = page.locator('.ant-modal-wrap:visible .ant-modal-content').last()
  const assignBody = await assignModal.innerText()
  check('打开改派弹窗（显示当前配送员）', assignBody.includes('改派配送员') && assignBody.includes('当前配送员'), assignBody.replace(/\n/g, ' ').slice(0, 120))
  check('候选含可派/不可派 + 拒绝原因 + 得分/距离列',
    /可派/.test(assignBody) && /综合得分/.test(assignBody) && /负载/.test(assignBody), '')
  check('硬门控说明可见（负载上限/载重容积）', assignBody.includes('负载上限') && assignBody.includes('载重'), '')
  const okDisabled = await assignModal.locator('.ant-modal-footer button.ant-btn-primary').isDisabled().catch(() => true)
  check('未选配送员时「确定」禁用', okDisabled, `disabled=${okDisabled}`)
  // 切到「全部配送员」看不可派候选的拒绝原因与禁用态
  await assignModal.locator('.ant-select').first().click()
  await page.locator('.ant-select-dropdown:visible').last().waitFor({ state: 'visible', timeout: 10000 }).catch(() => {})
  await page.waitForTimeout(500)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: '全部配送员' }).first().click()
  // 全部配送员 249 行，渲染需要数秒 → 轮询等「不可派」出现再断言（固定 sleep 会读到空态）
  const assignBodyAll = await waitForModalText(page, /不可派/, 30000)
  check('候选列表标出不可派并给出拒绝原因',
    /不可派/.test(assignBodyAll) && /休息|并接上限|实名认证|无定位|缺少取货点|载重/.test(assignBodyAll),
    assignBodyAll.replace(/\n/g, ' ').slice(0, 160))
  const disabledRadios = await assignModal.locator('.ant-table-tbody .ant-radio-input:disabled').count()
  check('不可派候选单选禁用（避免提交后才被后端拒绝）', disabledRadios >= 1, `disabled=${disabledRadios}`)
  await page.screenshot({ path: `${SHOTS}/ui-assign-modal.png`, fullPage: true })
  // 目标配送员**由 API 动态挑定**后，用弹窗关键字把候选收敛成一行再点选。
  // （不要在 249 行的虚拟列表里按 nth 读行再点：列表按得分排序会重排，"跳过当前配送员"的判断会失准，
  //   实测会出现「点到原配送员 → 后端以『新配送员与原配送员相同』拒绝」。）
  await refreshRiderHeartbeats()
  const curRiderId = String((await taskById(T304)).riderId || '')
  const candList = data(await api('GET', `/dms/dispatch/candidates?taskId=${T304}`)) || []
  const target = candList.find(c => c.eligible && String(c.riderId) !== curRiderId)
  check('取到可与当前不同的可派配送员（在线且未达并接上限）',
    !!target, `当前=${curRiderId} / 候选可派=${candList.filter(c => c.eligible).length}`)
  // 关键：弹窗里的候选是**打开时**拉的，可能早于上面的心跳刷新（那时目标还显示「离线」被「仅可派」滤掉），
  // 所以先关掉重新打开，让候选按刷新后的在线状态重取，再用关键字锁定唯一一行。
  await closeTopModal(page)
  await page.waitForTimeout(800)
  await refreshRiderHeartbeats()
  await t304Row.locator('button').filter({ hasText: '改派' }).first().click()
  await page.waitForTimeout(2600)
  const assignModal2 = page.locator('.ant-modal-wrap:visible .ant-modal-content').last()
  // 候选接口在共享库下要拉全量配送员（含围栏判定）需数秒，必须等表格真出「可派/不可派」行再填关键字
  await waitForModalText(page, /可派|不可派/, 30000)
  await assignModal2.locator('input[placeholder="姓名 / 电话 / 车牌"]').fill(String(target?.riderName || ''))
  const picked = assignModal2.locator('.ant-table-tbody tr', { hasText: String(target?.riderName || '') }).first()
  await picked.waitFor({ state: 'visible', timeout: 30000 }).catch(() => {})
  const pickedText = await picked.innerText().catch(() => '')
  check('关键字锁定后候选仅剩目标配送员且可派',
    !!target && /可派/.test(pickedText) && pickedText.includes(String(target.riderName)),
    pickedText.replace(/\n/g, ' ').slice(0, 120))
  await refreshRiderHeartbeats()
  await picked.locator('.ant-radio-wrapper').first().click({ force: true })
  await page.waitForTimeout(400)
  await confirmTopModal(page)
  let afterReassign = ''
  for (let i = 0; i < 25; i++) {
    afterReassign = await page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-04' }).first().innerText().catch(() => '')
    if (afterReassign && !!target && afterReassign.includes(String(target.riderName))) break
    await page.waitForTimeout(1000)
  }
  check('改派提交成功（列表配送员已变更为目标配送员）',
    !!afterReassign && !!target && afterReassign.includes(String(target.riderName)),
    `目标=${target?.riderName} / after=${afterReassign.replace(/\n/g, ' ').slice(0, 90)}`)

  // ── 单条「指派」（待分配任务）：任务级约束在弹窗内可见 + 正常任务指派成功 ──
  // API 段已把多数任务推进过状态，这里按需把样本重置回「待分配」（与人工在页面上操作等价）
  await dbQuery("UPDATE dms_task SET status = 0, rider_id = NULL, rider_name = NULL, dispatch_type = NULL,"
    + " dispatch_time = NULL WHERE id IN (2099000000000009301, 2099000000000009302, 2099000000000009303)")
  await refreshRiderHeartbeats()

  // 1) 超载重任务（T303）：候选应全部不可派且给出「载重」原因，「确定」禁用 → 数据不变
  await searchBox.fill('E2EDPT-03')
  await queryBtn.click()
  await page.waitForTimeout(2300)
  const t303Row = page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-03' }).first()
  check('待分配任务行显示「指派」入口', (await t303Row.innerText()).includes('指派'), (await t303Row.innerText()).replace(/\n/g, ' ').slice(0, 70))
  await refreshRiderHeartbeats()
  await t303Row.locator('button').filter({ hasText: /指\s*派/ }).first().click()
  await page.waitForTimeout(2600)
  const assignOnlyModal = page.locator('.ant-modal-wrap:visible .ant-modal-content').last()
  // 等候选渲染出「不可派」行（全库候选多，空态/加载态必须区分开再断言）
  const assignOnlyText = await waitForModalText(page, /不可派/)
  check('打开指派弹窗（标题随模式切换为「指派配送员」）', assignOnlyText.includes('指派配送员'), '')
  check('任务级约束在弹窗内可见（超载重 → 候选全部「不可派 · 载重上限」）',
    /载重/.test(assignOnlyText) && /不可派/.test(assignOnlyText), assignOnlyText.replace(/\n/g, ' ').slice(0, 170))
  check('全部不可派时「确定」禁用（不会误提交）',
    await assignOnlyModal.locator('.ant-modal-footer button.ant-btn-primary').isDisabled().catch(() => false), '')
  await page.screenshot({ path: `${SHOTS}/ui-assign-modal.png`, fullPage: true })
  await closeTopModal(page)
  await page.waitForTimeout(700)
  check('超载重任务未被误指派（仍为待分配）', Number((await taskById(T303)).status) === 0, (await taskById(T303)).riderName)

  // 2) 正常任务（T302）：关键字锁定配送员 → 指派成功 + 审计留痕
  await searchBox.fill('E2EDPT-02')
  await queryBtn.click()
  await page.waitForTimeout(2300)
  const t302Row = page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-02' }).first()
  await refreshRiderHeartbeats()
  await t302Row.locator('button').filter({ hasText: /指\s*派/ }).first().click()
  await page.waitForTimeout(2600)
  const assignOkModal = page.locator('.ant-modal-wrap:visible .ant-modal-content').last()
  // 同样**动态挑人**：写死 R7 会因共享库把它占满/打离线而「候选里根本找不到」
  const t302OkRows = assignOkModal.locator('.ant-table-tbody tr', { hasText: '可派' })
  await t302OkRows.first().waitFor({ state: 'visible', timeout: 30000 }).catch(() => {})
  const t302CandText = await assignOkModal.innerText()
  check('候选可派行可勾选（在线且负载未满）',
    await t302OkRows.count() > 0 && /可派/.test(t302CandText), t302CandText.replace(/\n/g, ' ').slice(0, 140))
  await refreshRiderHeartbeats()
  await t302OkRows.first().locator('.ant-radio-wrapper').first().click({ force: true })
  await page.waitForTimeout(400)
  await assignOkModal.locator('input[placeholder="选填，留痕到调度审计"]').fill('E2E UI 指派')
  const okBtn = assignOkModal.locator('.ant-modal-footer button.ant-btn-primary')
  check('选中候选人后「确定」可用', !(await okBtn.isDisabled().catch(() => true)), '')
  await confirmTopModal(page)
  // 共享库下写接口 3~6s：轮询行文本，固定 sleep 会读脏（任务其实已指派成功）
  let t302AfterAssign = ''
  for (let i = 0; i < 20; i++) {
    t302AfterAssign = await page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-02' }).first().innerText().catch(() => '')
    if (t302AfterAssign.includes('已分配')) break
    await page.waitForTimeout(1000)
  }
  check('UI 指派成功（行更新为已分配 + 配送员）',
    t302AfterAssign.includes('已分配'),
    t302AfterAssign.replace(/\n/g, ' ').slice(0, 110))
  const uiLog = data(await api('GET', `/dms/task/${T302}/logs`)) || []
  check('UI 指派的调度原因写入审计', uiLog.some(l => l.reason === 'E2E UI 指派'), JSON.stringify(uiLog.map(l => l.reason)).slice(0, 140))

  // ── 批量指派（提交 + 逐单结果报告） ──
  await dbQuery("UPDATE dms_task SET status = 0, rider_id = NULL, rider_name = NULL, dispatch_type = NULL,"
    + " dispatch_time = NULL WHERE id IN (2099000000000009302, 2099000000000009303)")
  await refreshRiderHeartbeats()
  await searchBox.fill('E2EDPT-0')
  await queryBtn.click()
  await page.waitForTimeout(2400)
  const baBar = await selectRows(page, ['E2EDPT-02', 'E2EDPT-03'])
  check('批量指派前勾选 2 行且无残留', /已选择\s*2\s*项/.test(baBar), baBar)
  await closeAllModals(page)
  await page.locator('.batch-bar button').filter({ hasText: '批量指派' }).click()
  await page.waitForTimeout(1400)
  const baModal2 = page.locator('.ant-modal-wrap:visible .ant-modal-content').last()
  await baModal2.locator('.ant-select').first().click()
  await page.waitForTimeout(600)
  // 候选虚拟化：直接找 R7 会落空，先输入关键字把选项收敛出来
  await page.keyboard.type('E2E调度任务R7', { delay: 30 })
  await page.waitForTimeout(1400)
  await refreshRiderHeartbeats()
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: 'E2E调度任务R7' }).first().click()
  await page.waitForTimeout(500)
  await confirmTopModal(page)
  const baReport = await waitForModalText(page, /批量指派结果/, 60000)
  check('批量指派提交后返回逐单结果报告（提交 2 / 成功 1 / 失败 1 + 可读拒绝原因）',
    baReport.includes('批量指派结果') && /提交\s*2/.test(baReport) && /失败\/跳过\s*1/.test(baReport)
    && /载重|并接上限|休息|资质|离线|区域分包/.test(baReport),
    baReport.replace(/\n/g, ' ').slice(0, 170))
  await closeTopModal(page)
  await page.waitForTimeout(700)

  // ── 「更多 → 标记异常」（带原因） ──
  await searchBox.fill('E2EDPT-06')
  await queryBtn.click()
  await page.waitForTimeout(2300)
  const t306Row = page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-06' }).first()
  await t306Row.locator('button').filter({ hasText: '更多' }).first().click()
  await page.waitForTimeout(900)
  await page.locator('.ant-dropdown:visible .ant-dropdown-menu-item').filter({ hasText: '标记异常' }).click()
  await page.waitForTimeout(1000)
  await confirmTopModal(page)
  await page.waitForTimeout(2600)
  const t306After = await page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-06' }).first().innerText()
  check('「更多 → 标记异常」生效（行状态变为异常）', t306After.includes('异常'), t306After.replace(/\n/g, ' ').slice(0, 90))
  const excLog = data(await api('GET', `/dms/task/${T306}/logs`)) || []
  check('UI 标记异常写审计', excLog.some(l => l.action === 'EXCEPTION'), JSON.stringify(excLog.map(l => l.action)).slice(0, 120))

  // ── 打印(F8) 与 导出（真实 xlsx） ──
  await searchBox.fill('E2EDPT-06')
  await queryBtn.click()
  await page.waitForTimeout(2300)
  const printBar = await selectRows(page, ['E2EDPT-06'])
  check('勾选行后批量条计数正确（无历史勾选残留）', /已选择\s*1\s*项/.test(printBar), printBar)
  await page.keyboard.press('F8')
  await page.waitForTimeout(2500)
  const printDlg = await page.locator('.ant-modal-wrap:visible .ant-modal-content').last().innerText().catch(() => '')
  check('打印(F8) 打开打印弹窗（模板/打印模式/份数）',
    printDlg.includes('打印模板') && printDlg.includes('打印模式') && printDlg.includes('打印份数'), printDlg.replace(/\n/g, ' ').slice(0, 120))
  await closeTopModal(page)
  await page.waitForTimeout(700)
  const dl = page.waitForEvent('download', { timeout: 20000 }).catch(() => null)
  await page.locator('.toolbar-section button').filter({ hasText: '导出' }).click()
  const download = await dl
  const dlName = download ? download.suggestedFilename() : ''
  const dlPath = download ? await download.path().catch(() => null) : null
  const dlSize = dlPath ? fs.statSync(dlPath).size : 0
  check('导出真实 xlsx（浏览器收到 .xlsx 下载且体积正常）', /\.xlsx$/.test(dlName) && dlSize > 1000, `${dlName}/${dlSize}B`)

  // ── 批量操作：勾选 2 行（1 可取消 + 1 不可取消）→ 批量取消 → 结果报告 ──
  await searchBox.fill('E2EDPT-1')
  await queryBtn.click()
  await page.waitForTimeout(2400)
  const rowT310 = page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-10' }).first()
  const rowT311 = page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-11' }).first()
  check('批量用例任务存在（E2EDPT-10 已完成 / E2EDPT-11 已分配）',
    await rowT310.count() > 0 && await rowT311.count() > 0, '')
  const batchBar = await selectRows(page, ['E2EDPT-10', 'E2EDPT-11'])
  check('勾选后批量条含三项操作且计数为 2（换查询后无残留）',
    batchBar.includes('批量指派') && batchBar.includes('批量取消') && batchBar.includes('批量打印') && /已选择\s*2\s*项/.test(batchBar),
    batchBar)

  // 批量指派弹窗（打开后取消，验证弹窗字段）
  await page.locator('.batch-bar button').filter({ hasText: '批量指派' }).click()
  await page.waitForTimeout(1200)
  const baModal = page.locator('.ant-modal-wrap:visible .ant-modal-content').last()
  const baText = await baModal.innerText()
  check('批量指派弹窗含配送员选择 + 原因 + 跳过说明',
    baText.includes('指派配送员') && baText.includes('调度原因') && baText.includes('并接上限'), baText.replace(/\n/g, ' ').slice(0, 140))
  await baModal.locator('.ant-modal-footer button').filter({ hasText: /取\s*消/ }).first().click()
  await page.waitForTimeout(900)
  check('批量指派弹窗可关闭（未选配送员不提交）', await page.locator('.ant-modal-wrap:visible').count() === 0, '')

  await closeAllModals(page)
  await page.locator('.batch-bar button').filter({ hasText: '批量取消' }).click()
  await page.waitForTimeout(1400)
  const cancelModal = page.locator('.ant-modal-wrap:visible .ant-modal-content').last()
  const cancelText = await cancelModal.innerText()
  check('批量取消弹窗含原因输入 + 状态机提示',
    cancelText.includes('取消原因') && cancelText.includes('待分配'), cancelText.replace(/\n/g, ' ').slice(0, 120))
  await cancelModal.locator('textarea').fill('E2E UI 批量取消')
  await confirmTopModal(page)
  await page.waitForTimeout(3200)
  const reportText = await waitForModalText(page, /批量取消结果/, 60000)
  check('批量取消返回逐单结果报告（提交 2 / 成功 1 / 失败 1）',
    reportText.includes('批量取消结果') && /提交\s*2/.test(reportText) && /失败\/跳过\s*1/.test(reportText) && /已完成/.test(reportText),
    reportText.replace(/\n/g, ' ').slice(0, 150))
  await page.screenshot({ path: `${SHOTS}/ui-batch-report.png`, fullPage: true })
  await closeTopModal(page)
  const t311After = await page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-11' }).count()
  check('批量取消后该单已离开默认列表（红冲口径）', t311After === 0, `rows=${t311After}`)

  // ── 页面配置弹窗（查询条件 / 功能按钮） ──
  await closeAllModals(page)
  await page.locator('.toolbar-section button').filter({ has: page.locator('.anticon-setting') }).first().click()
  await page.waitForTimeout(1200)
  const cfgText = await page.locator('.ant-modal-wrap:visible .ant-modal-content').last().innerText()
  check('页面配置弹窗含「查询条件」「功能按钮」两 Tab',
    cfgText.includes('查询条件') && cfgText.includes('功能按钮'), cfgText.replace(/\n/g, ' ').slice(0, 120))
  const cfgModal = page.locator('.ant-modal-wrap:visible .ant-modal-content').last()
  check('查询条件清单含 13 项（任务编号/客户/配送车辆/有无配送员/是否异常…）',
    ['任务编号', '订单号', '客户', '配送员', '配送车辆', '状态', '优先级', '类型', '配送日期', '配送线路', '配送区域', '有无配送员', '是否异常'].every(t => cfgText.includes(t)),
    '')
  await cfgModal.locator('.ant-tabs-tab').filter({ hasText: '功能按钮' }).click()
  await page.waitForTimeout(900)
  const btnCfgText = await cfgModal.innerText()
  check('功能按钮清单含自动调度/批量指派/批量取消/批量打印/地图派单/超时升级/刷新/打印/导出/调度策略配置',
    ['自动调度', '批量指派', '批量取消', '批量打印', '地图派单', '超时升级', '刷新', '打印(F8)', '导出', '调度策略配置'].every(t => btnCfgText.includes(t)),
    btnCfgText.replace(/\n/g, ' ').slice(0, 200))
  await page.screenshot({ path: `${SHOTS}/ui-page-config.png`, fullPage: true })
  await closeTopModal(page)
  await page.waitForTimeout(700)

  // ── 地图派单（复用 RouteMapCanvas） ──
  // 先准备好「正常重量的待分配任务」（弹窗打开时即拉取待分配清单，重置必须在打开之前）
  await dbQuery("UPDATE dms_task SET status = 0, rider_id = NULL, rider_name = NULL, dispatch_type = NULL,"
    + " dispatch_time = NULL WHERE id = 2099000000000009302")
  await refreshRiderHeartbeats()
  await closeAllModals(page)
  await page.locator('.toolbar-section button').filter({ hasText: '地图派单' }).click()
  await page.waitForTimeout(3000)
  const mapModal = page.locator('.ant-modal-wrap:visible .ant-modal-content').last()
  const mapText = await mapModal.innerText()
  check('地图派单弹窗打开（含任务选择 + 候选列表）',
    mapText.includes('派单地图') && mapText.includes('待分配任务') && mapText.includes('候选配送员'), mapText.replace(/\n/g, ' ').slice(0, 140))
  check('地图画布渲染（RouteMapCanvas）', await mapModal.locator('.route-map').count() > 0, '')
  check('地图含底图降级标识（无 Key 自动回退矢量画布）', /底图/.test(mapText), '')
  // 地图派单闭环：选中任务 → 候选落点 → 直接指派
  await page.locator('.ant-modal-wrap:visible .ant-select').first().click()
  await page.waitForTimeout(800)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: 'E2EDPT-02' }).first().click()
  const mapCandText = await waitForModalText(page, /E2E调度任务R7/)
  const mapCandRow = page.locator('.ant-modal-wrap:visible .ant-table-tbody tr', { hasText: 'E2E调度任务R7' }).first()
  check('地图派单：选中任务后拉出候选配送员（含落点/负载/得分）',
    /E2E调度任务R7/.test(mapCandText) && /可派/.test(mapCandText) && /\dm|km/.test(mapCandText),
    mapCandText.replace(/\n/g, ' ').slice(0, 150))
  await page.screenshot({ path: `${SHOTS}/ui-map-dispatch.png`, fullPage: true })
  await mapCandRow.locator('button').filter({ hasText: /指\s*派/ }).first().click()
  const mapMsg = await page.locator('.ant-message').innerText().catch(() => '')
  // 共享库下指派请求 3~6s：轮询任务状态（固定 sleep 会读脏）
  let t302AfterMap = {}
  for (let i = 0; i < 20; i++) {
    t302AfterMap = await taskById(T302)
    if (Number(t302AfterMap.status) === 1) break
    await page.waitForTimeout(1000)
  }
  check('地图派单：点击候选「指派」真实落库（任务转为已分配）',
    Number(t302AfterMap.status) === 1 && String(t302AfterMap.riderId) === R7,
    `status=${t302AfterMap.status}/rider=${t302AfterMap.riderName}/${mapMsg.replace(/\n/g, ' ').slice(0, 40)}`)
  await page.waitForTimeout(500)

  // ── 自动调度（弹确认 → 结果报告） ──
  await closeAllModals(page)
  await page.locator('.toolbar-section button').filter({ hasText: '自动调度' }).click()
  await page.waitForTimeout(1200)
  const confirmText = await page.locator('.ant-modal-confirm').last().innerText().catch(() => '')
  check('自动调度二次确认弹窗', confirmText.includes('自动调度'), confirmText.replace(/\n/g, ' ').slice(0, 90))
  await closeAllModals(page)
  await page.locator('.toolbar-section button').filter({ hasText: '自动调度' }).click()
  await page.waitForTimeout(1200)
  await confirmTopModal(page)
  // 共享库待分配任务多（自动调度要逐单 × 全量配送员评分/围栏判定），扫描可达数分钟 → 等够
  const autoReport = await waitForModalText(page, /自动调度结果/, 240000)
  check('自动调度返回逐单结果报告（提交/成功/失败）',
    autoReport.includes('自动调度结果') && /提交|成功/.test(autoReport),
    autoReport.replace(/\n/g, ' ').slice(0, 140) + ' || 可见弹窗=' + (await visibleModalTitles(page)))
  await page.waitForTimeout(500)
  await page.screenshot({ path: `${SHOTS}/ui-auto-report.png`, fullPage: true })
  await closeTopModal(page)
  await page.waitForTimeout(700)

  // ── 列配置（表头序号齿轮） ──
  const gear = page.locator('.ss-grid .th-settings-btn').first()
  if (await gear.count()) {
    await gear.click()
    await page.waitForTimeout(1800)
    const colCfg = await page.locator('.ant-modal-wrap:visible .ant-modal-content').last().innerText().catch(() => '')
    check('表头齿轮打开列配置（个人/全局双 Tab + 全量列可见）',
      colCfg.includes('个人配置') && colCfg.includes('全局配置') && colCfg.includes('收货地址') && colCfg.includes('配送员电话'),
      colCfg.replace(/\n/g, ' ').slice(0, 160))
    await closeTopModal(page)
    await page.waitForTimeout(600)
  } else {
    check('表头齿轮打开列配置（个人/全局双 Tab）', false, '未找到序号列表头齿轮')
  }

  // ── 隐藏列可在列配置中开启（证明 defaultHidden 列可配置而非删除） ──
  const gear2 = page.locator('.ss-grid .th-settings-btn').first()
  if (await gear2.count()) {
    await gear2.click()
    await page.waitForTimeout(1600)
    const cfgPanel = await page.locator('.ant-modal-wrap:visible .ant-modal-content').last().innerText().catch(() => '')
    check('列配置面板含全部隐藏列（距目的地/负载/超时/打印次数/备注）',
      ['距目的地', '负载', '超时', '打印次数', '备注'].every(t => cfgPanel.includes(t)), cfgPanel.replace(/\n/g, ' ').slice(0, 180))
    await closeTopModal(page)
    await page.waitForTimeout(600)
  }

  // ── 轨迹跳转（有配送员且在途） ──
  // API 段已把 T305 标记为异常 → 这里把它置回「配送中(4)」并挂配送员，再验轨迹入口
  await dbQuery("UPDATE dms_task SET status = 4, rider_id = 2099000000000009216, rider_name = 'E2E调度任务R7'"
    + " WHERE id = 2099000000000009305")
  await searchBox.fill('E2EDPT-05')
  await queryBtn.click()
  await page.waitForTimeout(2200)
  const t305Row = page.locator('.ss-grid tbody tr', { hasText: 'E2EDPT-05' }).first()
  if (await t305Row.locator('button').filter({ hasText: /轨\s*迹/ }).count()) {
    await t305Row.locator('button').filter({ hasText: /轨\s*迹/ }).first().click()
    await page.waitForTimeout(2000)
    check('「轨迹」跳转实时跟踪并带配送员参数', /dms\/realtime-tracking/.test(page.url()) && /riderId=/.test(page.url()), page.url())
    await page.goto(`${FE}/dms/dispatch-task`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(6000)
  } else {
    check('「轨迹」跳转实时跟踪并带配送员参数', false, '未找到轨迹按钮（任务状态不满足 2/3/4）')
  }

  check('运行期无页面错误', errs.length === 0, errs.slice(0, 2).join(' || '))
  check('运行期无接口 4xx/5xx', http4xx.length === 0, http4xx.slice(0, 3).join(' || '))
  check('关键接口均被真实调用（task 分页/写操作 + dispatch 候选/统计）',
    apiCalls.some(c => c.includes('dms/task/page')) && apiCalls.some(c => c.includes('dms/dispatch/candidates')),
    `${apiCalls.length} 次调用: ${apiCalls.slice(0, 4).join(' | ')}`)

  await page.screenshot({ path: `${SHOTS}/ui-list.png`, fullPage: true })
  await browser.close()
}

// ── 数据库直连：刷新位置上报心跳 ────────────────────────────────
// 《智能调度》已把「在线口径」统一为位置上报心跳（dms.tracking.online.minutes，默认 2 分钟）并作为自动派单约束；
// 本脚本种子里的 last_report_time 是插入时刻，距插入超过阈值会自然离线 → 验收前统一重置。
let DB = null

async function ensureDb() {
  if (DB) return DB
  DB = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
  await DB.connect()
  return DB
}

async function dbQuery(sql) {
  const client = await ensureDb()
  return (await client.query(sql)).rows
}

/** 关闭数据库连接（不关会让 node 事件循环常驻，脚本「跑完不退出」） */
async function closeDb() {
  if (!DB) return
  try {
    await DB.end()
  } catch (e) {
    /* ignore */
  }
  DB = null
}

;(async () => {
  console.log('调度任务金标准验收 —— 后端 :' + PORT + ' / 前端 ' + FE)
  await login()
  await dbQuery("UPDATE dms_rider SET last_report_time = now()"
    + " WHERE id BETWEEN 2099000000000009210 AND 2099000000000009216")
  await apiSuite()
  await uiSuite()

  // 整轮结束：把临时放宽的并接上限还原（API 段与 UI 段都需要宽上限才能稳定挑到可派配送员）
  if (CFG_BEFORE && CFG_BEFORE.maxConcurrent != null) {
    const restored = await api('PUT', '/dms/dispatch/strategy', { ...CFG_BEFORE })
    check('用例结束后派单策略已还原（并接上限回到原值）',
      restored.json?.code === 200 && Number(data(restored)?.maxConcurrent) === Number(CFG_BEFORE.maxConcurrent),
      `maxConcurrent=${data(restored)?.maxConcurrent}`)
  }

  // 自己产生的签收记录（本套用例用签收审核驱动「任务 → 已完成」）事后清理：
  // 共享库里其它套件（《签收管理》等）的台账断言是**全局条件**，留着会污染它们的条数
  await dbQuery('DELETE FROM dms_sign WHERE task_id BETWEEN 2099000000000009300 AND 2099000000000009399')

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 汇总：${pass}/${results.length} 通过${fail ? `，${fail} 项失败` : ''} ═══`)
  if (fail) results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  await closeDb()
  // 显式退出：pg 连接不关会让 node 事件循环常驻 → 进程「跑完不退出」，
  // 多次启动就会堆叠成并发实例互相改数据（本脚本踩过）
  process.exit(fail ? 1 : 0)
})().catch(e => { console.error('E2E 异常终止:', e.message); process.exit(1) })
