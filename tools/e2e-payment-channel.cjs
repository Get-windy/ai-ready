/*
 * 支付渠道主数据（资料 → 支付管理 → 支付渠道）金标准端到端验证
 *   · API 验收：新增(必填/唯一/支付方式有效性)/多条件分页/服务端排序/详情回填/修改(编码禁改)/启停/启用下拉/
 *               导出 xlsx/导入模板/Excel 导入(成功+重号+方式不存在)/删除+逻辑删除
 *   · UI 验收：列表列 + 固定查询项 + 表头齿轮列配置 + 页面配置弹窗 + 新增弹窗真实保存 + 行内 修改/停用/删除
 *
 * 用法：node tools/e2e-payment-channel.cjs
 *      ERP_PORT=5802 FE_URL=http://localhost:5656 node tools/e2e-payment-channel.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5665)
const SHOTS = 'I:/AI-Ready/tool-results/md-payment-channel'
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

/** 取接口 data（后端 Result 包一层） */
function data(res) {
  return res.json ? res.json.data : null
}

const E2E_USER = process.env.E2E_USER || 'e2e_pay'
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
const CODE_A = 'E2EPC' + STAMP
const CODE_B = 'E2EPC' + STAMP + 'B'

async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 0) 取一个支付方式用于关联
  const methods = await api('GET', '/erp/md/payment-method/list')
  const methodList = Array.isArray(data(methods)) ? data(methods) : []
  check('支付方式下拉可用（新增渠道前置数据）', methodList.length >= 1, `count=${methodList.length}`)
  const method = methodList[0]
  const methodId = method?.id

  // 1) 新增
  const createRes = await api('POST', '/erp/md/payment-channel', {
    channelCode: CODE_A,
    channelName: 'E2E 微信渠道' + STAMP,
    methodId,
    merchantNo: '1600000' + STAMP,
    configJson: '{"appId":"wx-e2e","apiKey":"k1"}',
    sort: 5,
    status: 1,
    remark: 'E2E 新增备注',
  })
  const created = data(createRes)
  check('新增支付渠道成功', created && created.id, JSON.stringify(created).slice(0, 200))
  check('新增回填支付方式编码/名称', !!created?.methodCode && !!created?.methodName, `${created?.methodCode}/${created?.methodName}`)
  check('新增回填状态文本', created?.statusText === '已启用', created?.statusText)
  const idA = created?.id

  // 1.1) 校验
  const badCode = await api('POST', '/erp/md/payment-channel', { channelCode: '', channelName: '缺编码', methodId })
  check('渠道编码必填校验', badCode.json?.code !== 200, badCode.json?.message)
  const badName = await api('POST', '/erp/md/payment-channel', { channelCode: CODE_B + 'N', channelName: '', methodId })
  check('渠道名称必填校验', badName.json?.code !== 200, badName.json?.message)
  const badMethod = await api('POST', '/erp/md/payment-channel', { channelCode: CODE_B + 'M', channelName: '缺方式', methodId: null })
  check('支付方式必填校验', badMethod.json?.code !== 200, badMethod.json?.message)
  const badMethod2 = await api('POST', '/erp/md/payment-channel', { channelCode: CODE_B + 'M2', channelName: '方式不存在', methodId: 999999999 })
  check('支付方式存在性校验', badMethod2.json?.code !== 200, badMethod2.json?.message)
  const dup = await api('POST', '/erp/md/payment-channel', { channelCode: CODE_A, channelName: '重号', methodId })
  check('渠道编码重复校验', dup.json?.code !== 200, dup.json?.message)
  const badJson = await api('POST', '/erp/md/payment-channel', { channelCode: CODE_B + 'J', channelName: '非法JSON', methodId, configJson: '{not-json' })
  check('渠道配置 JSON 合法性校验', badJson.json?.code !== 200, badJson.json?.message)

  // 2) 分页多条件
  const p1 = await api('GET', `/erp/md/payment-channel/page?pageNum=1&pageSize=20&keyword=${CODE_A}`)
  check('分页按渠道编码检索命中', Number(data(p1)?.total) >= 1, JSON.stringify(data(p1)?.total))
  const p2 = await api('GET', `/erp/md/payment-channel/page?pageNum=1&pageSize=20&keyword=E2E 微信渠道`)
  check('分页按渠道名称检索命中', Number(data(p2)?.total) >= 1, JSON.stringify(data(p2)?.total))
  const p3 = await api('GET', `/erp/md/payment-channel/page?pageNum=1&pageSize=20&keyword=1600000${STAMP}`)
  check('分页按商户号检索命中', Number(data(p3)?.total) >= 1, JSON.stringify(data(p3)?.total))
  const p4 = await api('GET', `/erp/md/payment-channel/page?pageNum=1&pageSize=20&methodId=${methodId}&keyword=${CODE_A}`)
  check('分页按支付方式过滤生效', Number(data(p4)?.total) >= 1, JSON.stringify(data(p4)?.total))
  const p5 = await api('GET', `/erp/md/payment-channel/page?pageNum=1&pageSize=20&keyword=${CODE_A}&status=0`)
  check('分页按状态=停用过滤生效（当前启用应为 0 条）', Number(data(p5)?.total) === 0, JSON.stringify(data(p5)?.total))
  const p6 = await api('GET', `/erp/md/payment-channel/page?pageNum=1&pageSize=20&keyword=${CODE_A}&sortField=channelCode&sortOrder=asc`)
  check('服务端排序参数生效', p6.json?.code === 200, JSON.stringify(data(p6)?.records?.[0]?.channelCode))

  // 3) 详情
  const detail = await api('GET', `/erp/md/payment-channel/${idA}`)
  check('详情回填商户号', data(detail)?.merchantNo === '1600000' + STAMP, data(detail)?.merchantNo)
  check('详情回填渠道配置 JSON', String(data(detail)?.configJson || '').includes('wx-e2e'), data(detail)?.configJson)

  // 4) 修改（编码禁改 + 其余字段更新）
  const upd = await api('PUT', `/erp/md/payment-channel/${idA}`, {
    channelCode: CODE_A,
    channelName: 'E2E 修改后渠道' + STAMP,
    methodId,
    merchantNo: '1600000' + STAMP + 'X',
    configJson: '{"appId":"wx-e2e-2"}',
    sort: 9,
    status: 1,
    remark: 'E2E 修改备注',
  })
  check('修改支付渠道成功', upd.json?.code === 200, upd.json?.message)
  const detail2 = await api('GET', `/erp/md/payment-channel/${idA}`)
  check('修改后名称已更新', String(data(detail2)?.channelName || '').includes('修改后渠道'), data(detail2)?.channelName)
  check('修改后排序已更新', Number(data(detail2)?.sort) === 9, data(detail2)?.sort)
  const codeChange = await api('PUT', `/erp/md/payment-channel/${idA}`, { channelCode: CODE_A + 'X', channelName: '改编码' })
  check('渠道编码不允许修改', codeChange.json?.code !== 200, codeChange.json?.message)

  // 5) 启停
  const st1 = await api('PUT', `/erp/md/payment-channel/${idA}/status`, { status: 0 })
  check('停用支付渠道成功', data(st1)?.status === 0, JSON.stringify(st1.json).slice(0, 200))
  const st2 = await api('PUT', `/erp/md/payment-channel/${idA}/status`, { status: 1 })
  check('启用支付渠道成功', data(st2)?.status === 1, JSON.stringify(st2.json).slice(0, 200))
  const stBad = await api('PUT', `/erp/md/payment-channel/${idA}/status`, { status: 7 })
  check('非法状态校验', stBad.json?.code !== 200, stBad.json?.message)

  // 6) 启用下拉
  const opts = await api('GET', `/erp/md/payment-channel/list?methodId=${methodId}`)
  check('启用渠道下拉返回数据', Array.isArray(data(opts)) && data(opts).length >= 1, JSON.stringify((data(opts) || []).length))
  check('下拉仅返回启用渠道', (data(opts) || []).every(x => x.status === 1), JSON.stringify((data(opts) || []).map(x => x.status)))

  // 7) 导出真实 xlsx
  const exp = await api('GET', `/erp/md/payment-channel/export?keyword=${CODE_A}`)
  check('导出返回 xlsx（非 JSON）', exp.buf && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b, `status=${exp.status}`)
  if (exp.buf && exp.buf[0] === 0x50) {
    fs.writeFileSync(path.join(SHOTS, 'export.xlsx'), exp.buf)
    try {
      const wb = XLSX.read(exp.buf, { type: 'buffer' })
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
      check('导出内容含渠道编码行', rows.some(r => String(r['渠道编码'] || '').includes(CODE_A)), JSON.stringify(rows[0] || {}))
      check('导出含 8 列', Object.keys(rows[0] || {}).length === 8, JSON.stringify(Object.keys(rows[0] || {})))
    } catch (e) {
      check('导出 xlsx 可解析', false, e.message)
    }
  }

  // 8) 导入模板
  const tpl = await api('GET', '/erp/md/payment-channel/import-template')
  check('导入模板返回 xlsx', tpl.buf && tpl.buf[0] === 0x50, `status=${tpl.status}`)

  // 9) Excel 导入（真实落库：1 成功 + 1 重号 + 1 支付方式不存在）
  const wbIn = XLSX.utils.book_new()
  const wsIn = XLSX.utils.aoa_to_sheet([
    ['导入结果', '渠道编码(必填)', '渠道名称(必填)', '支付方式(必填)', '商户号', '排序', '备注'],
    ['', CODE_B, 'E2E 导入渠道' + STAMP, method?.methodCode || 'CASH', '1600000' + STAMP + 'B', '3', '导入备注'],
    ['', CODE_A, 'E2E 重复编码', method?.methodCode || 'CASH', '', '', ''],
    ['', CODE_B + 'Z', 'E2E 方式不存在', 'NOT_EXIST_METHOD', '', '', ''],
  ])
  XLSX.utils.book_append_sheet(wbIn, wsIn, '支付渠道')
  const importBuf = XLSX.write(wbIn, { type: 'buffer', bookType: 'xlsx' })
  const boundary = '----e2ePaymentChannel' + STAMP
  const pre = Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="payment-channel.xlsx"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n`)
  const post = Buffer.from(`\r\n--${boundary}--\r\n`)
  const multipart = Buffer.concat([pre, importBuf, post])
  const imp = await api('POST', '/erp/md/payment-channel/import-excel', multipart, { 'Content-Type': `multipart/form-data; boundary=${boundary}` })
  const impData = data(imp)
  check('导入成功 1 行', Number(impData?.success) === 1, JSON.stringify(impData?.success))
  check('导入失败 2 行（重号 + 支付方式不存在）', Number(impData?.failure) === 2, JSON.stringify(impData?.errors))

  // 10) 删除
  const pb = await api('GET', `/erp/md/payment-channel/page?pageNum=1&pageSize=20&keyword=${CODE_B}`)
  const idB = (data(pb)?.records || [])[0]?.id
  if (idB) {
    const delB = await api('DELETE', `/erp/md/payment-channel/${idB}`)
    check('删除导入的支付渠道', delB.json?.code === 200, delB.json?.message)
  } else {
    check('删除导入的支付渠道', false, '未查到导入的渠道')
  }
  const delA = await api('DELETE', `/erp/md/payment-channel/${idA}`)
  check('删除支付渠道', delA.json?.code === 200, delA.json?.message)
  const after = await api('GET', `/erp/md/payment-channel/page?pageNum=1&pageSize=20&keyword=${CODE_A}`)
  check('删除后查询不到（逻辑删除生效）', Number(data(after)?.total) === 0, JSON.stringify(data(after)?.total))
}

// ══════════════ UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  // 收集列表请求 URL（用于断言服务端排序/查询参数真实下发）
  const listUrls = []
  page.on('request', r => {
    const u = r.url()
    if (u.includes('/erp/md/payment-channel/page')) listUrls.push(decodeURIComponent(u))
  })

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

  await openPage(`${FE}/md/payment-channel`)
  const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())
  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['渠道编码', '渠道名称', '支付方式', '商户号', '排序', '状态', '创建时间']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  for (const b of ['新增', '导入', '刷新', '打印(F8)', '导出']) {
    check(`工具栏含「${b}」`, bodyText.includes(b.replace(/\s+/g, '')))
  }
  for (const q of ['筛选条件', '支付方式', '显示状态', '查询']) {
    check(`查询区含「${q}」`, bodyText.includes(q))
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 列配置齿轮（表头 rowNo 列右上角）
  const gearCount = await page.evaluate(() => {
    const ths = [...document.querySelectorAll('.ss-grid th')]
    return ths.filter(th => th.querySelector('button, .anticon-setting, i')).length
  })
  check('数据表带表头齿轮（列配置入口）', gearCount > 0, `count=${gearCount}`)

  await page.locator('.ss-grid .th-settings-btn').first().click()
  await page.waitForTimeout(1500)
  const colPanel = page.locator('.ant-modal-content:visible').last()
  const colPanelText = (await colPanel.innerText()).replace(/\s+/g, '')
  check('列配置弹窗可打开（个人配置 / 全局配置）', colPanelText.includes('个人配置') && colPanelText.includes('全局配置'), colPanelText.slice(0, 60))
  check('列配置含基础 7 列', ['渠道编码', '渠道名称', '支付方式', '商户号', '排序', '状态', '创建时间'].every(c => colPanelText.includes(c)), colPanelText.replace(/\n/g, '|').slice(0, 200))
  check('列配置含隐藏列（备注 / 渠道配置）', colPanelText.includes('备注') && colPanelText.includes('渠道配置'), colPanelText.replace(/\n/g, '|').slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(1000)

  // 页面配置弹窗
  await page.locator('button[title="页面配置"]').click()
  await page.waitForTimeout(1200)
  const pageCfg = page.locator('.ant-modal-content:visible').last()
  const pageCfgText = (await pageCfg.innerText()).replace(/\s+/g, '')
  check('页面配置弹窗可打开', pageCfgText.includes('查询条件'), pageCfgText.slice(0, 60))
  check('页面配置含查询条件 3 项', ['筛选条件', '支付方式', '显示状态'].every(c => pageCfgText.includes(c)), pageCfgText.replace(/\n/g, '|').slice(0, 200))
  await page.locator('.ant-modal-content:visible .ant-tabs-tab:has-text("功能按钮")').click()
  await page.waitForTimeout(800)
  const btnTabText = (await page.locator('.ant-modal-content:visible').last().innerText()).replace(/\s+/g, '')
  check('页面配置含功能按钮 5 项', ['新增', '导入', '刷新', '打印(F8)', '导出'].every(c => btnTabText.includes(c.replace(/\s+/g, ''))), btnTabText.replace(/\n/g, '|').slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(1000)

  // 新增弹窗
  await page.click('button:has-text("新增")')
  await page.waitForTimeout(2000)
  const modal = page.locator('.ant-modal-content').last()
  const modalText = (await modal.innerText()).replace(/\s+/g, '')
  check('新增弹窗标题=支付渠道', modalText.includes('支付渠道'), (await modal.innerText()).split('\n')[0])
  for (const f of ['渠道编码', '渠道名称', '支付方式', '商户号', '排序', '状态', '渠道配置', '备注']) {
    check(`弹窗含字段「${f}」`, modalText.includes(f))
  }

  const UI_CODE = 'E2EPC' + STAMP + 'U'
  const UI_NAME = 'E2E UI 支付渠道' + STAMP
  await modal.locator('input[placeholder*="WECHAT_MP"]').fill(UI_CODE)
  await modal.locator('input[placeholder*="公众号支付"]').fill(UI_NAME)
  await modal.locator('input[placeholder*="支付宝PID"]').fill('UI' + STAMP)
  await modal.locator('.ant-select').first().click()
  await page.waitForTimeout(600)
  await page.locator('.ant-select-item-option').first().click()
  await page.waitForTimeout(400)
  await modal.locator('textarea[placeholder*="appId"]').fill('{"appId":"wx-ui"}')
  await modal.locator('textarea[placeholder*="备注"]').fill('UI 备注')
  await page.screenshot({ path: path.join(SHOTS, 'ui-modal.png'), fullPage: true })

  await modal.locator('.ant-btn-primary:has-text("保存")').click()
  await page.waitForTimeout(3000)
  const afterSave = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('新增保存后列表出现该渠道', afterSave.includes(UI_NAME.replace(/\s+/g, '')), UI_NAME)
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-save.png'), fullPage: true })

  // 创建时间已格式化（无 T 分隔与微秒）
  const gridText = await page.locator('.ss-grid').innerText()
  check('创建时间格式化（yyyy-MM-dd HH:mm:ss）', /\d{4}-\d{2}-\d{2} \d{2}:\d{2}/.test(gridText) && !/\d{4}-\d{2}-\d{2}T\d{2}/.test(gridText), (gridText.match(/\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}:\d{2}/) || [''])[0])

  // 表头服务端排序：点击「渠道编码」列排序图标 → 请求带 sortField/sortOrder
  listUrls.length = 0
  await page.locator('.ss-grid th').filter({ hasText: '渠道编码' }).first().locator('.th-sort-icon').first().click()
  await page.waitForTimeout(2200)
  check('点击表头排序图标触发服务端排序请求（sortField=channelCode）', listUrls.some(u => u.includes('sortField=channelCode')), listUrls[listUrls.length - 1] || 'no-request')

  // 行内「修改」（编码禁用 + 名称回填）
  await page.click(`.ss-grid a:has-text("${UI_NAME}")`)
  await page.waitForTimeout(2000)
  const editModal = page.locator('.ant-modal-content').last()
  const editName = await editModal.locator('input[placeholder*="公众号支付"]').inputValue()
  check('修改弹窗回填渠道名称', editName === UI_NAME, editName)
  const codeDisabled = await editModal.locator('input[placeholder*="WECHAT_MP"]').isDisabled()
  check('修改弹窗渠道编码禁用', codeDisabled)
  await page.screenshot({ path: path.join(SHOTS, 'ui-edit.png'), fullPage: true })
  await editModal.locator('.ant-btn:has-text("关闭")').click()
  await page.waitForTimeout(1000)

  // 行内「停用」（本页状态查询默认「全部」，停用后仍在列表）
  const row = page.locator('.ss-grid tr', { hasText: UI_NAME }).first()
  await row.locator('button:has-text("停用")').click()
  await page.waitForTimeout(800)
  const confirmBox = page.locator('.ant-modal-confirm:visible').last()
  if (await confirmBox.count()) {
    await confirmBox.locator('.ant-btn-primary').click()
    await page.waitForTimeout(2500)
  }
  const rowAfter = page.locator('.ss-grid tr', { hasText: UI_NAME }).first()
  const rowAfterText = (await rowAfter.innerText()).replace(/\s+/g, '')
  check('停用后状态标签变为「已停用」', rowAfterText.includes('已停用'), rowAfterText.slice(0, 120))
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-disable.png'), fullPage: true })

  // 行内「删除」
  await rowAfter.locator('button:has-text("删除")').click()
  await page.waitForTimeout(1200)
  const delBox = page.locator('.ant-modal-confirm:visible').last()
  if (await delBox.count()) {
    await delBox.locator('button:has-text("确认删除")').click()
    await page.waitForTimeout(2500)
  }
  check('行内删除支付渠道成功', !(await page.locator('body').innerText()).replace(/\s+/g, '').includes(UI_NAME.replace(/\s+/g, '')))

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
