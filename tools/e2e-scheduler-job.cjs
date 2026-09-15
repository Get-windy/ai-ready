/*
 * 定时任务（系统管理 → 开发工具 → 定时任务，菜单 62405 `system:dev:scheduler`）端到端验证
 *
 * 覆盖（2026-09-14 复核修复后的行为基线）：
 *   · 白名单执行目标：handlers 清单返回已注册处理器；未注册 job_key 配置阶段即被拒（400 + 可读原因）
 *   · **反射调用已移除**：只提交 executeClass/executeMethod（不提交 job_key）→ 直接拒绝（旧实现会反射调用任意类）
 *   · 默认关闭：迁移插入的「配送·超时任务升级扫描」行 enabled=0
 *   · 手动执行 → 执行日志 EXECUTE_RESULT 为处理器自报摘要（含「演练」）
 *   · 演练不落库：dryRun 执行前后，在途任务归属/调度审计条数均无变化
 *   · 多实例互斥：用 redis-cli 抢占锁后再触发 → 摘要为「跳过：另一实例正在执行」
 *   · 运行期兜底：库里的 job_key 指向未注册处理器 → 执行失败且失败原因可读
 *   · 权限：非超管账号（SYSTEM_ADMIN 角色，未授予 system:dev:scheduler:*）→ 403
 *
 * 前置：
 *   1) 种子：node -e "…执行 tools/e2e-scheduler-job-user.sql…"（见文件头注释）
 *   2) 验收后端：java -jar core-api-0.3.18-exec.jar --server.port=5665 --spring.profiles.active=dev
 *
 * 用法：node tools/e2e-scheduler-job.cjs
 */
const http = require('http')
const fs = require('fs')
const { execFileSync } = require('child_process')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const PORT = Number(process.env.ERP_PORT || 5665)
const FE = process.env.FE_URL || 'http://localhost:5656'
const SHOTS = 'I:/AI-Ready/tool-results/scheduler-job'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

// ── 种子/夹具 ID ──
const HANDLER_KEY = 'dms.dispatch.escalateOverdue'
const SUPER_USER = process.env.E2E_USER || 'e2e_scheduler_job'
const SUPER_PWD = process.env.E2E_PWD || 'admin123'
const PLAIN_USER = 'e2e_scheduler_plain'
const PLAIN_PWD = 'admin123'
const TMP_TASK_ID = 2099000000000009701
const TMP_TASK_ID_BAD = 2099000000000009702
const REDIS_CLI = process.env.REDIS_CLI || 'C:/Program Files/Redis/redis-cli.exe'
const LOCK_KEY = 'dms:job:lock:' + HANDLER_KEY

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

function rawReq(method, reqPath, body, token, hostPort) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? null : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const port = hostPort || PORT
    const r = http.request({ hostname: 'localhost', port, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* 非 JSON */ }
        resolve({ status: res.statusCode, json, buf })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login(username, password) {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username, password, tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid,
  })
  return res.json?.data?.token || res.json?.data?.accessToken || null
}

let TOKEN = null
let PLAIN_TOKEN = null
async function api(method, reqPath, body, token) {
  return rawReq(method, reqPath, body, token === undefined ? TOKEN : token)
}
const data = (res) => (res.json ? res.json.data : null)

let DB = null
async function dbQuery(sql) {
  if (!DB) {
    DB = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
    await DB.connect()
  }
  return (await DB.query(sql)).rows
}
async function closeDb() {
  if (!DB) return
  try { await DB.end() } catch (e) { /* ignore */ }
  DB = null
}

/** 读取某任务最近一条执行日志 */
async function lastLog(taskId) {
  const res = await api('GET', `/scheduler/task/log/page?current=1&size=10&taskId=${taskId}`)
  const rows = data(res)?.records || []
  return rows[0] || null
}

/** 轮询等执行日志落库（异步执行） */
async function waitLog(taskId, predicate, timeoutMs = 20000) {
  const started = Date.now()
  let last = null
  while (Date.now() - started < timeoutMs) {
    last = await lastLog(taskId)
    if (last && predicate(last)) return last
    await new Promise(r => setTimeout(r, 500))
  }
  return last
}

function redisCli(args) {
  try {
    return execFileSync(REDIS_CLI, args, { encoding: 'utf8' }).trim()
  } catch (e) {
    return null
  }
}

/**
 * UI 验收：页面骨架 + 处理器下拉（白名单）+ 新增/删除闭环 + 无 4xx
 */
async function uiSuite() {
  console.log('\n═══ UI 验收（开发工具 → 定时任务）═══')
  const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 1000 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  page.setDefaultTimeout(25000)
  const errs = []
  const http4xx = []
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('console', m => { if (m.type() === 'error') errs.push('console: ' + m.text().slice(0, 140)) })
  page.on('response', r => {
    const u = r.url()
    if (u.includes('/api/scheduler') && r.status() >= 400) http4xx.push(`${r.status()} ${u.split('/api/')[1]}`)
  })
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', '1') }, [TOKEN])
  await page.goto(`${FE}/admin/dev/scheduler`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(6000)

  const body = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开且非空（无白屏）', body.includes('定时任务') && body.length > 150, page.url())
  check('列表列含「任务处理器」（原桩字段 taskGroup/taskClass 已修正）',
    body.includes('任务处理器') && !body.includes('任务分组'), '')
  check('列表渲染迁移插入的任务行',
    body.includes('配送·超时任务升级扫描'), '')
  check('页面提示处理器白名单口径', body.includes('已注册的任务处理器'), '')

  // 新增 → 处理器下拉（白名单）→ 提交
  // 注意：表单控件要用**标签定位**（a-select 自带搜索框，按 input 顺序取会错位 —— 踩过）
  const E2E_UI_NAME = 'E2E-UI定时任务'
  await page.locator('button').filter({ hasText: '新增任务' }).first().click()
  await page.waitForSelector('.ant-modal:visible', { timeout: 10000 })
  const modal = page.locator('.ant-modal:visible').first()
  const item = (label) => modal.locator('.ant-form-item').filter({ hasText: label }).first()
  await item('任务名称').locator('input').first().fill(E2E_UI_NAME)
  await item('任务处理器').locator('.ant-select').first().click()
  const opt = page.locator('.ant-select-dropdown:visible .ant-select-item-option')
    .filter({ hasText: '配送·超时任务升级扫描' }).first()
  await opt.waitFor({ timeout: 10000 })
  await opt.click()
  await page.waitForTimeout(400)
  await item('Cron表达式').locator('input').first().fill('0 0 5 * * ?')
  const picked = await item('任务处理器').locator('.ant-select-selection-item').allInnerTexts()
  check('新增弹窗处理器下拉为白名单（选项即已注册处理器，选中回显）',
    picked.join('|').includes('配送·超时任务升级扫描'), picked.join('|'))
  await modal.locator('.ant-btn-primary').first().click()
  await page.waitForTimeout(3000)
  const bodyAfter = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('新增任务成功（列表出现新行）', bodyAfter.includes(E2E_UI_NAME),
    (await page.locator('.ant-message-notice-content').allInnerTexts()).join('|'))

  // 删除刚创建的行
  const row = page.locator('.ant-table-row').filter({ hasText: E2E_UI_NAME }).first()
  if (await row.count()) {
    await row.locator('a.text-danger').first().click()
    await page.waitForTimeout(800)
    await page.locator('.ant-popconfirm:visible .ant-btn-primary, .ant-popover:visible .ant-btn-primary')
      .first().click()
    await page.waitForTimeout(2000)
    const gone = !(await page.locator('body').innerText()).includes(E2E_UI_NAME)
    check('删除任务后行消失', gone, '')
  } else {
    check('删除任务后行消失', false, '未找到待删除行')
  }

  check('UI 运行期无接口 4xx/5xx（scheduler 相关）', http4xx.length === 0, http4xx.slice(0, 4).join(' | '))
  check('UI 运行期无页面错误', errs.filter(e => !/favicon|ResizeObserver/.test(e)).length === 0,
    errs.slice(0, 3).join(' | '))

  await browser.close()
}

;(async () => {
  console.log('定时任务验收 —— 后端 :' + PORT)

  // ── 0. 鉴权基线 ──
  const noAuth = await rawReq('GET', '/scheduler/task/page', null, null)
  check('未登录访问被拒（401）', noAuth.status === 401 || Number(noAuth.json?.code) === 401, `http=${noAuth.status}`)

  TOKEN = await login(SUPER_USER, SUPER_PWD)
  check('超管账号登录成功', !!TOKEN)
  if (!TOKEN) throw new Error('登录失败，请先执行 tools/e2e-scheduler-job-user.sql')

  // ── 1. 白名单处理器清单 ──
  const handlers = await api('GET', '/scheduler/task/handlers')
  const list = data(handlers) || []
  check('handlers 返回已注册处理器清单',
    handlers.json?.code === 200 && Array.isArray(list) && list.length >= 1,
    JSON.stringify(list).slice(0, 200))
  check('清单含配送超时升级处理器（key + 中文名）',
    list.some(h => h.key === HANDLER_KEY && h.name && h.name.length > 0),
    JSON.stringify(list.find(h => h.key === HANDLER_KEY) || {}).slice(0, 160))

  // ── 2. 迁移基线：默认关闭 + 参数为演练 ──
  const page = await api('GET', '/scheduler/task/page?current=1&size=100')
  const rows = data(page)?.records || []
  const jobRow = rows.find(r => r.jobKey === HANDLER_KEY) || {}
  check('台账可见（scheduled_task 为平台级表，会话租户不应过滤掉迁移行）',
    rows.length > 0, `rows=${rows.length}`)
  check('迁移已插入超时升级任务行且**默认停用**（enabled=0）',
    !!jobRow.id && Number(jobRow.enabled) === 0, `id=${jobRow.id}/enabled=${jobRow.enabled}`)
  check('任务行只带 job_key（无 executeClass/executeMethod 字段）',
    jobRow.executeClass === undefined && jobRow.executeMethod === undefined,
    JSON.stringify({ executeClass: jobRow.executeClass, executeMethod: jobRow.executeMethod }))
  const legacyRows = rows.filter(r => !r.jobKey)
  check('历史演示任务已停用（job_key 为空的行 enabled 全为 0）',
    legacyRows.length > 0 && legacyRows.every(r => Number(r.enabled) === 0),
    `无 job_key 行数=${legacyRows.length}`)

  // ── 3. 配置阶段即拦截：未注册处理器 / 只提交类名方法名 ──
  const badKey = await api('POST', '/scheduler/task', {
    taskName: 'E2E-未注册处理器', jobKey: 'not.exist.handler', cronExpression: '0 0 3 * * ?', taskType: 'CRON',
  })
  check('未注册 job_key 创建被拒（400 + 可读原因）',
    badKey.status === 400 && /未注册的任务处理器/.test(String(badKey.json?.message)),
    `http=${badKey.status}/${badKey.json?.message}`)

  const legacyReflect = await api('POST', '/scheduler/task', {
    taskName: 'E2E-反射调用已移除',
    executeClass: 'java.lang.Class', executeMethod: 'forName',
    cronExpression: '0 0 3 * * ?', taskType: 'CRON',
  })
  check('只提交「类名 + 方法名」被拒（反射调用已移除，不再按请求体反射）',
    legacyReflect.status === 400 && /请选择任务处理器/.test(String(legacyReflect.json?.message)),
    `http=${legacyReflect.status}/${legacyReflect.json?.message}`)

  // ── 4. 手动执行（演练）→ 执行日志为处理器自报摘要 ──
  // 任务行默认参数是「真执行」（enabled=0 承担默认关闭）；本脚本只在共享库上验证**演练**路径，
  // 真重派路径由《调度任务》E2E 的 escalate-overdue 断言覆盖（含 ESCALATE 审计与超时告警）。
  await api('PUT', `/scheduler/task/${jobRow.id}`, { executeParams: '{"dryRun": true}' })
  const beforeAudit = Number((await dbQuery('SELECT count(*)::int n FROM dms_task_log'))[0]?.n)
  // 用**数据库本地时间**做前后对比基准（进程时区与 DB 时区不一致时，用 JS 本地时间会全量误判）
  const runStartDb = (await dbQuery("SELECT to_char(now(), 'YYYY-MM-DD HH24:MI:SS') AS t"))[0].t
  const exec = await api('POST', `/scheduler/task/${jobRow.id}/execute`)
  check('立即执行接口返回成功', exec.json?.code === 200, exec.json?.message)
  const log = await waitLog(jobRow.id, l => l.executeStatus && l.executeStatus !== 'RUNNING')
  check('执行日志状态 SUCCESS', log?.executeStatus === 'SUCCESS', `${log?.executeStatus}/${log?.errorMessage || ''}`)
  check('执行结果=处理器自报摘要（演练 + 扫描租户/命中单数）',
    /演练/.test(String(log?.executeResult)) && /扫描/.test(String(log?.executeResult)) && /命中/.test(String(log?.executeResult)),
    log?.executeResult)

  // ── 5. 演练不落库（任务归属/调度审计均未变化） ──
  const afterAudit = Number((await dbQuery('SELECT count(*)::int n FROM dms_task_log'))[0]?.n)
  check('演练未写调度审计（dms_task_log 条数不变）', beforeAudit === afterAudit, `before=${beforeAudit}/after=${afterAudit}`)
  const reDispatched = Number((await dbQuery(
    `SELECT count(*)::int n FROM dms_task WHERE deleted = 0 AND dispatch_time IS NOT NULL`
    + ` AND dispatch_time >= '${runStartDb}'`))[0]?.n)
  check('演练未改动任务归属（无任务 dispatch_time 被刷新）', reDispatched === 0, `rows=${reDispatched}`)

  // ── 6. 多实例互斥（用 redis-cli 抢占锁） ──
  const redisOk = redisCli(['PING']) === 'PONG'
  check('redis-cli 可用（用于模拟另一实例持锁）', redisOk, REDIS_CLI)
  if (redisOk) {
    redisCli(['SET', LOCK_KEY, 'held-by-e2e', 'PX', '60000'])
    const execLocked = await api('POST', `/scheduler/task/${jobRow.id}/execute`)
    const logLocked = await waitLog(jobRow.id, l => l.executeStatus && l.executeStatus !== 'RUNNING')
    check('锁被占用时执行 → 摘要为「跳过：另一实例正在执行」',
      execLocked.json?.code === 200 && /另一实例正在执行/.test(String(logLocked?.executeResult))
        && logLocked?.executeStatus === 'SUCCESS',
      `${logLocked?.executeStatus}/${logLocked?.executeResult}`)
    redisCli(['DEL', LOCK_KEY])
    const releaseCheck = redisCli(['GET', LOCK_KEY])
    check('锁已释放（DEL 后 GET 为空）', !releaseCheck, `value=${releaseCheck}`)
    const execAgain = await api('POST', `/scheduler/task/${jobRow.id}/execute`)
    const logAgain = await waitLog(jobRow.id, l => l.executeStatus && l.executeStatus !== 'RUNNING'
      && !/另一实例正在执行/.test(String(l.executeResult)))
    check('锁释放后恢复正常执行', execAgain.json?.code === 200 && !/另一实例正在执行/.test(String(logAgain?.executeResult)),
      `${logAgain?.executeStatus}/${logAgain?.executeResult}`)
  }

  // ── 7. 运行期兜底：库里 job_key 指向未注册处理器 → 失败原因可读 ──
  // 上次异常退出可能留下同一 id 的临时行 → 先清再插（脚本可重复执行）
  await dbQuery(`DELETE FROM scheduled_task_log WHERE task_id = ${TMP_TASK_ID}`)
  await dbQuery(`DELETE FROM scheduled_task WHERE id = ${TMP_TASK_ID}`)
  await dbQuery(`INSERT INTO scheduled_task (id, task_name, task_desc, task_type, cron_expression, execute_params,
      job_key, status, retry_count, retry_interval, timeout, enabled, execute_count, success_count, fail_count,
      tenant_id, create_time, update_time, deleted)
    VALUES (${TMP_TASK_ID}, 'E2E-未注册处理器（运行期）', 'E2E', 'CRON', '0 0 3 * * ?', NULL,
      'e2e.unregistered.handler', 'STOPPED', 0, 60, 300, 0, 0, 0, 0, 0, now(), now(), 0)`)
  const runBad = await api('POST', `/scheduler/task/${TMP_TASK_ID}/execute`)
  const logBad = await waitLog(TMP_TASK_ID, l => l.executeStatus && l.executeStatus !== 'RUNNING')
  check('未注册处理器执行失败且原因可读',
    runBad.json?.code === 200 && logBad?.executeStatus === 'FAILURE'
      && /未注册的任务处理器/.test(String(logBad?.errorMessage)),
    `${logBad?.executeStatus}/${String(logBad?.errorMessage).slice(0, 120)}`)

  // ── 8. 权限：非超管账号被拒 ──
  PLAIN_TOKEN = await login(PLAIN_USER, PLAIN_PWD)
  check('普通账号（SYSTEM_ADMIN 角色）登录成功', !!PLAIN_TOKEN)
  if (PLAIN_TOKEN) {
    const plainPage = await api('GET', '/scheduler/task/page', null, PLAIN_TOKEN)
    check('普通账号访问定时任务台账 → 403（权限码 system:dev:scheduler:*）',
      plainPage.status === 403 && Number(plainPage.json?.code) === 403,
      `http=${plainPage.status}/${plainPage.json?.message}`)
    const plainExec = await api('POST', `/scheduler/task/${jobRow.id}/execute`, null, PLAIN_TOKEN)
    check('普通账号触发执行 → 403', plainExec.status === 403, `http=${plainExec.status}`)
    const plainHandlers = await api('GET', '/scheduler/task/handlers', null, PLAIN_TOKEN)
    check('普通账号读取处理器清单 → 403', plainHandlers.status === 403, `http=${plainHandlers.status}`)
  }

  // ── 9. 增删改闭环（超管） ──
  const created = await api('POST', '/scheduler/task', {
    taskName: 'E2E-白名单处理器任务', jobKey: HANDLER_KEY,
    cronExpression: '0 0 4 * * ?', taskType: 'CRON',
    executeParams: '{"dryRun": true}', taskDesc: 'E2E 创建', enabled: 0,
  })
  const createdId = data(created)?.id
  check('创建任务（合法 job_key）成功', created.json?.code === 200 && !!createdId, created.json?.message)
  const updated = await api('PUT', `/scheduler/task/${createdId}`, { taskDesc: 'E2E 更新', cronExpression: '0 30 4 * * ?' })
  check('更新任务成功（未提交 jobKey 时保持原值）',
    updated.json?.code === 200 && data(updated)?.jobKey === HANDLER_KEY
      && data(updated)?.cronExpression === '0 30 4 * * ?',
    `${data(updated)?.jobKey}/${data(updated)?.cronExpression}`)
  const enableBad = await api('POST', `/scheduler/task/${TMP_TASK_ID}/enable`)
  check('启用未注册处理器的任务被拒（启用前即校验）',
    enableBad.status === 400 && /未注册的任务处理器/.test(String(enableBad.json?.message)),
    `http=${enableBad.status}/${enableBad.json?.message}`)
  const removed = await api('DELETE', `/scheduler/task/${createdId}`)
  check('删除任务成功', removed.json?.code === 200, removed.json?.message)

  // ── 10. UI 验收（页面原为「字段名全错」的桩：taskClass/taskGroup/description 与实体不匹配）──
  await uiSuite()

  // ── 汇总 ──
  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 汇总：${pass}/${results.length} 通过${fail ? `，${fail} 项失败` : ''} ═══`)
  if (fail) results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))

  // 还原任务行参数为「真执行」（脚本为验证演练路径临时改过）
  await api('PUT', `/scheduler/task/${jobRow.id}`, { executeParams: '{"dryRun": false}' })

  // 清理本脚本产生的任务行与日志（种子里的 job 行保留，供页面使用）
  await dbQuery(`DELETE FROM scheduled_task_log WHERE task_id IN (${TMP_TASK_ID})`)
  await dbQuery(`DELETE FROM scheduled_task WHERE id = ${TMP_TASK_ID}`)

  await closeDb()
  process.exit(fail ? 1 : 0)
})().catch(async e => {
  console.error('E2E 异常终止:', e.message)
  try { await closeDb() } catch (err) { /* ignore */ }
  process.exit(1)
})
