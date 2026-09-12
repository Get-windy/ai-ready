/*
 * 线路主数据（资料 → 配送管理 → 线路）金标准端到端验证
 *   · API 验收：编号/新增(含配送区域子表)/分页多条件/详情/修改/启停/导出 xlsx/导入模板/Excel 导入/删除
 *   · UI 验收：列表 6 列 + 固定查询项 + 列配置齿轮 + 新增弹窗（配送路线）真实保存 + 行内 修改/停用
 *
 * 用法：node tools/e2e-md-route.cjs            （默认后端 5665、前端 5656）
 *      ERP_PORT=5665 FE_URL=http://localhost:5656 node tools/e2e-md-route.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5665)
const SHOTS = 'I:/AI-Ready/tool-results/md-route'
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

/** 取接口 data（后端 ApiResponse 包一层） */
function data(res) {
  return res.json ? res.json.data : null
}

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
    // 专用账号不可用时回退 admin（并发环境会被互踢，仅作兜底）
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

/** 专用验收账号（tools/e2e-md-route-user.sql 创建），避免与并行会话共用 admin 互相踢下线 */
const E2E_USER = process.env.E2E_USER || 'e2e_route'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

const STAMP = Date.now().toString().slice(-6)
const CODE_A = 'XLT' + STAMP
const CODE_B = 'XLT' + STAMP + 'B'

async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 0) 生成编号
  const nc = await api('GET', '/erp/md/route/next-code')
  check('next-code 生成线路编号', /^XL\d{3,}$/.test(String(data(nc))), data(nc))

  // 1) 新增（含配送区域行政区划多选；线路类型为互斥单选）
  const createRes = await api('POST', '/erp/md/route', {
    routeCode: CODE_A,
    routeName: 'E2E 自配线路' + STAMP,
    routeSelf: 1,
    routeLogistics: 0,
    expressName: '',
    remark: 'E2E 新增备注',
    areas: [
      { areaCode: '11' },
      { areaCode: '110101' },
    ],
  })
  const created = data(createRes)
  check('新增线路成功', created && created.id, JSON.stringify(created).slice(0, 200))
  check('新增回填线路类型文本（自配）', created?.routeTypeText === '自配', created?.routeTypeText)
  check('新增按行政区划编码回填区域名称', String(created?.areaText || '').includes('北京市') && String(created?.areaText || '').includes('东城区'), created?.areaText)
  check('新增按行政区划层级回填区域类型', (created?.areas || []).some(a => a.areaType === 'PROVINCE') && (created?.areas || []).some(a => a.areaType === 'DISTRICT'), JSON.stringify((created?.areas || []).map(a => a.areaType)))
  const idA = created?.id

  // 1.1) 必填/互斥校验
  const badType = await api('POST', '/erp/md/route', { routeCode: CODE_B + 'X', routeName: '缺类型', routeSelf: 0, routeLogistics: 0 })
  check('线路类型必填校验', badType.json?.code !== 200, badType.json?.message)
  const bothType = await api('POST', '/erp/md/route', { routeCode: CODE_B + 'Z', routeName: '双类型', routeSelf: 1, routeLogistics: 1 })
  check('线路类型互斥校验（不可同时自配+物流）', bothType.json?.code !== 200, bothType.json?.message)
  const badName = await api('POST', '/erp/md/route', { routeCode: CODE_B + 'Y', routeName: '', routeSelf: 1, routeLogistics: 0 })
  check('线路名称必填校验', badName.json?.code !== 200, badName.json?.message)
  const dup = await api('POST', '/erp/md/route', { routeCode: CODE_A, routeName: '重号', routeSelf: 1, routeLogistics: 0 })
  check('线路编号重复校验', dup.json?.code !== 200, dup.json?.message)

  // 2) 分页多条件
  const p1 = await api('GET', `/erp/md/route/page?pageNum=1&pageSize=20&keyword=${CODE_A}`)
  check('分页按线路编号检索命中', Number(data(p1)?.total) >= 1, JSON.stringify(data(p1)?.total))
  const p2 = await api('GET', `/erp/md/route/page?pageNum=1&pageSize=20&keyword=E2E 自配线路`)
  check('分页按线路名称检索命中', Number(data(p2)?.total) >= 1, JSON.stringify(data(p2)?.total))
  const p3 = await api('GET', `/erp/md/route/page?pageNum=1&pageSize=20&keyword=东城区`)
  check('分页按配送区域检索命中', Number(data(p3)?.total) >= 1, JSON.stringify(data(p3)?.total))
  const p4 = await api('GET', '/erp/md/route/page?pageNum=1&pageSize=20&routeType=LOGISTICS')
  check('分页按线路类型=物流检索', p4.json?.code === 200, JSON.stringify(data(p4)?.total))
  const p5 = await api('GET', `/erp/md/route/page?pageNum=1&pageSize=20&keyword=${CODE_A}&status=DISABLED`)
  check('分页显示状态=已停用过滤生效', Number(data(p5)?.total) === 0, JSON.stringify(data(p5)?.total))

  // 3) 详情
  const detail = await api('GET', `/erp/md/route/${idA}`)
  check('详情返回配送区域子表 2 行', (data(detail)?.areas || []).length === 2, JSON.stringify((data(detail)?.areas || []).length))

  // 4) 修改（改名 + 换区域为 1 项 + 切换线路类型为物流）
  const upd = await api('PUT', `/erp/md/route/${idA}`, {
    routeCode: CODE_A,
    routeName: 'E2E 修改后线路' + STAMP,
    routeSelf: 0,
    routeLogistics: 1,
    expressName: 'E2E 物流公司',
    remark: 'E2E 修改备注',
    areas: [{ areaCode: '1101' }],
  })
  check('修改线路成功', upd.json?.code === 200, upd.json?.message)
  const detail2 = await api('GET', `/erp/md/route/${idA}`)
  check('修改后区域子表替换为 1 项', (data(detail2)?.areas || []).length === 1, JSON.stringify((data(detail2)?.areas || []).length))
  check('修改后线路类型切换为物流', data(detail2)?.routeTypeText === '物流', data(detail2)?.routeTypeText)
  check('修改后物流公司保留', data(detail2)?.expressName === 'E2E 物流公司', data(detail2)?.expressName)
  check('修改后区域按编码回填名称', data(detail2)?.areaText === '市辖区', data(detail2)?.areaText)

  // 5) 启停
  const st1 = await api('PUT', `/erp/md/route/${idA}/status`, { status: 'DISABLED' })
  check('停用线路成功', data(st1)?.status === 'DISABLED', JSON.stringify(st1.json).slice(0, 200))
  const st2 = await api('PUT', `/erp/md/route/${idA}/status`, { status: 'ENABLED' })
  check('启用线路成功', data(st2)?.status === 'ENABLED', JSON.stringify(st2.json).slice(0, 200))
  const stBad = await api('PUT', `/erp/md/route/${idA}/status`, { status: 'XXX' })
  check('非法状态校验', stBad.json?.code !== 200, stBad.json?.message)

  // 6) 下拉
  const opts = await api('GET', '/erp/md/route/options')
  check('启用线路下拉返回数据', Array.isArray(data(opts)) && data(opts).length >= 1, JSON.stringify((data(opts) || []).length))

  // 7) 导出真实 xlsx
  const exp = await api('GET', `/erp/md/route/export?keyword=${CODE_A}`)
  check('导出返回 xlsx（非 JSON）', exp.buf && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b, `status=${exp.status}`)
  if (exp.buf && exp.buf[0] === 0x50) {
    fs.writeFileSync(path.join(SHOTS, 'export.xlsx'), exp.buf)
    try {
      const wb = XLSX.read(exp.buf, { type: 'buffer' })
      const sheet = wb.Sheets[wb.SheetNames[0]]
      const rows = XLSX.utils.sheet_to_json(sheet)
      check('导出内容含线路编号行', rows.some(r => String(r['线路编号'] || '').includes(CODE_A)), JSON.stringify(rows[0] || {}))
      check('导出含 7 列（含显示状态）', Object.keys(rows[0] || {}).length === 7, JSON.stringify(Object.keys(rows[0] || {})))
    } catch (e) {
      check('导出 xlsx 可解析', false, e.message)
    }
  }

  // 8) 导入模板
  const tpl = await api('GET', '/erp/md/route/import-template')
  check('导入模板返回 xlsx', tpl.buf && tpl.buf[0] === 0x50, `status=${tpl.status}`)

  // 9) Excel 导入（真实落库：1 行成功 + 1 行重号失败）
  const wbIn = XLSX.utils.book_new()
  const wsIn = XLSX.utils.aoa_to_sheet([
    ['导入结果', '线路编号', '线路名称(必填)', '线路类型(自配/物流)', '物流公司', '配送区域编码', '备注'],
    ['', CODE_B, 'E2E 导入线路' + STAMP, '物流', 'E2E 导入物流', '110101', '导入备注'],
    ['', CODE_A, 'E2E 重复编号', '自配', '', '', ''],
  ])
  XLSX.utils.book_append_sheet(wbIn, wsIn, '线路信息')
  const importBuf = XLSX.write(wbIn, { type: 'buffer', bookType: 'xlsx' })
  const boundary = '----e2eMdRoute' + STAMP
  const pre = Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="route.xlsx"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n`)
  const post = Buffer.from(`\r\n--${boundary}--\r\n`)
  const multipart = Buffer.concat([pre, importBuf, post])
  const imp = await api('POST', '/erp/md/route/import-excel', multipart, { 'Content-Type': `multipart/form-data; boundary=${boundary}` })
  const impData = data(imp)
  check('导入成功 1 行', Number(impData?.success) === 1, JSON.stringify(impData?.success))
  check('导入失败 1 行（重号被拒）', Number(impData?.failure) === 1, JSON.stringify(impData?.errors))

  // 10) 删除（含导入的 B）
  const pb = await api('GET', `/erp/md/route/page?pageNum=1&pageSize=20&keyword=${CODE_B}`)
  const idB = (data(pb)?.records || [])[0]?.id
  if (idB) {
    const delB = await api('DELETE', `/erp/md/route/${idB}`)
    check('删除导入的线路', delB.json?.code === 200, delB.json?.message)
  } else {
    check('删除导入的线路', false, '未查到导入的线路')
  }
  const pa = await api('GET', `/erp/md/route/page?pageNum=1&pageSize=20&keyword=${CODE_A}`)
  const idA2 = (data(pa)?.records || [])[0]?.id
  const delA = await api('DELETE', `/erp/md/route/${idA2}`)
  check('删除线路', delA.json?.code === 200, delA.json?.message)
  const after = await api('GET', `/erp/md/route/page?pageNum=1&pageSize=20&keyword=${CODE_A}`)
  check('删除后查询不到（逻辑删除生效）', Number(data(after)?.total) === 0, JSON.stringify(data(after)?.total))

  return { idA2 }
}

// ══════════════ UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  // /api 转发到验收实例（前端 dev server 的代理默认指向 5655）
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  /** 注入 token 打开页面（并行会话共用 admin 会被互踢，失败则重登一次） */
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

  // 打开线路页
  await openPage(`${FE}/md/route`)
  const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())
  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['线路编号', '线路名称', '线路类型', '物流公司', '配送区域', '备注']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  for (const b of ['新增', '导入', '刷新', '打印(F8)', '导出']) {
    check(`工具栏含「${b}」`, bodyText.includes(b.replace(/\s+/g, '')))
  }
  for (const q of ['筛选条件', '显示状态', '线路类型', '查询']) {
    check(`查询区含「${q}」`, bodyText.includes(q))
  }
  check('列表中无「页面配置」齿轮之外的多余配置入口', !bodyText.includes('页面配置'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 列配置齿轮（表头 rowNo 列右上角）
  const gearCount = await page.evaluate(() => {
    const ths = [...document.querySelectorAll('.ss-grid th')]
    return ths.filter(th => th.querySelector('button, .anticon-setting, i')).length
  })
  check('数据表带表头齿轮/按钮（列配置入口）', gearCount > 0, `count=${gearCount}`)

  // 空行占位行（__ghost）不得渲染名称链接与行内操作按钮，避免误点
  const ghostStats = await page.evaluate(() => {
    const rows = [...document.querySelectorAll('.ss-grid tbody tr')]
    // 空行占位行残留内容仅剩序号（rowNo），故同时剔除数字与行内按钮文案
    const stripAction = t => (t || '').replace(/[\s\d]+/g, '').replace(/修改|删除|停用|启用/g, '')
    const ghosts = rows.filter(tr => !stripAction(tr.innerText))
    return {
      total: rows.length,
      ghost: ghosts.length,
      ghostWithButton: ghosts.filter(tr => tr.querySelector('button')).length,
    }
  })
  check('空行占位行不渲染行内操作按钮', ghostStats.ghostWithButton === 0, JSON.stringify(ghostStats))

  // 列配置弹窗（个人配置 / 全局配置 双 Tab，对标 6 列全默认显示）
  await page.locator('.ss-grid .th-settings-btn').first().click()
  await page.waitForTimeout(1500)
  const colPanel = page.locator('.ant-modal-content:visible').last()
  const colPanelText = (await colPanel.innerText()).replace(/\s+/g, '')
  check('列配置弹窗可打开', colPanelText.includes('个人配置'), colPanelText.slice(0, 80))
  check('列配置弹窗含「个人配置 / 全局配置」双 Tab', colPanelText.includes('个人配置') && colPanelText.includes('全局配置'))
  check('列配置含对标 6 列', ['线路编号', '线路名称', '线路类型', '物流公司', '配送区域', '备注'].every(c => colPanelText.includes(c)), colPanelText.replace(/\n/g, '|').slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(1000)

  // 新增弹窗
  await page.click('button:has-text("新增")')
  await page.waitForTimeout(2000)
  const modal = page.locator('.ant-modal-content:visible').last()
  const modalText = (await modal.innerText()).replace(/\s+/g, '')
  check('新增弹窗标题=配送路线', modalText.includes('配送路线'), (await modal.innerText()).split('\n')[0])
  for (const f of ['线路类型', '线路编号', '线路名称', '配送区域', '备注']) {
    check(`弹窗含字段「${f}」`, modalText.includes(f))
  }
  const codeInput = modal.locator('input[placeholder="请输入线路编号"]')
  const codeVal = await codeInput.inputValue()
  check('弹窗自动带出线路编号', /^XL/.test(codeVal), codeVal)

  // 填写并保存（线路类型=互斥单选，默认自配；配送区域=行政区划多选）
  const UI_NAME = 'E2E UI 线路' + STAMP
  check('线路类型默认选中「自配」（对标默认值）', await modal.locator('.ant-radio-wrapper:has-text("自配") input').isChecked())
  await modal.locator('input[placeholder="请输入线路名称"]').fill(UI_NAME)
  await page.waitForTimeout(300)
  await modal.locator('.ant-radio-wrapper:has-text("物流")').click()
  await page.waitForTimeout(400)
  const expressInput = modal.locator('input[placeholder*="承运物流公司"]')
  check('选中「物流」后显示物流公司字段', (await expressInput.count()) > 0)
  if (await expressInput.count()) await expressInput.fill('UI 物流公司')
  await modal.locator('.ant-radio-wrapper:has-text("自配")').click()
  await page.waitForTimeout(400)
  check('切回「自配」后物流公司字段隐藏（互斥单选）', (await modal.locator('input[placeholder*="承运物流公司"]').count()) === 0)

  // 配送区域：行政区划多选（city-list），已选项以标签呈现
  await modal.locator('.area-select').click()
  await page.waitForTimeout(1500)
  await page.locator('.ant-select-tree-treenode:has-text("北京市") .ant-select-tree-checkbox').first().click()
  await page.waitForTimeout(600)
  // 收起下拉：点击弹窗标题（勿用 Escape，antd Modal 会把 Esc 当作关闭弹窗）
  await page.locator('.ant-modal-content:visible .ant-modal-header').click()
  await page.waitForTimeout(600)
  const modalAreaText = (await modal.innerText()).replace(/\s+/g, '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-modal.png'), fullPage: true })
  check('配送区域已选（多选标签呈现）', modalAreaText.includes('北京市'), modalAreaText.slice(0, 160))

  await modal.locator('.ant-btn-primary:has-text("保存")').click()
  await page.waitForTimeout(3000)
  const afterSave = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('新增保存后列表出现该线路', afterSave.includes(UI_NAME.replace(/\s+/g, '')), UI_NAME)
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-save.png'), fullPage: true })

  // 行内「修改」
  await page.click(`.ss-grid a:has-text("${UI_NAME}")`)
  await page.waitForTimeout(2000)
  const editModal = page.locator('.ant-modal-content:visible').last()
  const editName = await editModal.locator('input[placeholder="请输入线路名称"]').inputValue()
  check('修改弹窗回填线路名称', editName === UI_NAME, editName)
  const selfChecked = await editModal.locator('.ant-radio-wrapper:has-text("自配") input').isChecked()
  check('修改弹窗回填线路类型（自配）', selfChecked)
  const editAreaText = (await editModal.innerText()).replace(/\s+/g, '')
  check('修改弹窗回填配送区域（行政区划标签）', editAreaText.includes('北京市'), editAreaText.slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-edit.png'), fullPage: true })
  await editModal.locator('.ant-btn:has-text("关闭")').click()
  await page.waitForTimeout(1000)

  // 行内「停用」
  const row = page.locator('.ss-grid tr', { hasText: UI_NAME }).first()
  await row.locator('button:has-text("停用")').click()
  await page.waitForTimeout(800)
  const confirmBox = page.locator('.ant-modal-confirm:visible').last()
  if (await confirmBox.count()) {
    await confirmBox.locator('.ant-btn-primary').click()
    await page.waitForTimeout(2500)
  }
  const afterDisable = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('停用后默认列表不再显示（默认只看已启用）', !afterDisable.includes(UI_NAME.replace(/\s+/g, '')), '仍在列表=' + afterDisable.includes(UI_NAME.replace(/\s+/g, '')))
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-disable.png'), fullPage: true })

  // 清理：显示状态改「全部」→ 删除
  await page.locator('.search-area .ant-select').first().click()
  await page.waitForTimeout(600)
  await page.locator('.ant-select-item-option:has-text("全部")').last().click()
  await page.waitForTimeout(2500)
  const row2 = page.locator('.ss-grid tr', { hasText: UI_NAME }).first()
  if (await row2.count()) {
    await row2.locator('button:has-text("删除")').click()
    await page.waitForTimeout(1200)
    const delBox = page.locator('.ant-modal-confirm:visible').last()
    if (await delBox.count()) {
      // okType='danger' → 按钮为 ant-btn-dangerous，用文案定位更稳
      await delBox.locator('button:has-text("确认删除")').click()
      await page.waitForTimeout(2500)
    }
    check('行内删除线路成功', !(await page.locator('body').innerText()).replace(/\s+/g, '').includes(UI_NAME.replace(/\s+/g, '')))
  } else {
    check('行内删除线路成功', false, '未找到待删除行')
  }

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
