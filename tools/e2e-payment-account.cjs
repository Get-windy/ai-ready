/*
 * 支付账户（资料 → 支付管理 → 支付账户）金标准端到端验证
 *   · API 验收：编号建议/新增/校验/多条件分页/详情/修改/统计(后端)/启停/余额调整/删除保护/导出 xlsx/单一口径同源
 *   · UI 验收：统计卡片 + 列表列 + 固定查询项 + 列配置齿轮 + 页面配置弹窗 + 新增/修改/余额调整/停用/删除闭环
 *
 * 用法：node tools/e2e-payment-account.cjs                    （默认后端 5668、前端 5656）
 *      ERP_PORT=5668 FE_URL=http://localhost:5656 node tools/e2e-payment-account.cjs
 *
 * 前置：验收专用账号 tools/e2e-payment-account-user.sql（避免与并行会话共用 admin 被 sa-token 互踢）
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5668)
const SHOTS = 'I:/AI-Ready/tool-results/md-payment-account'
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
      res.on('end', () => resolve({ status: res.statusCode, buf: Buffer.concat(chunks), headers: res.headers, json: tryJson(Buffer.concat(chunks)) }))
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

function data(res) {
  return res.json ? res.json.data : null
}

/** 专用验收账号（tools/e2e-payment-account-user.sql 创建） */
const E2E_USER = process.env.E2E_USER || 'e2e_payacct'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

function readCaptcha(capJson) {
  return [...Buffer.from(capJson.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
}

async function loginAs(username, password) {
  const cap = await rawReq('GET', '/auth/captcha')
  const res = await rawReq('POST', '/auth/login', {
    username, password, tenantName: '系统租户',
    captcha: readCaptcha(cap.json), captchaKey: cap.json.data.uuid,
  })
  return res.json?.data?.token || res.json?.data?.accessToken
}

async function login() {
  let token = await loginAs(E2E_USER, E2E_PWD)
  if (!token && E2E_USER !== 'admin') {
    // 专用账号不可用时回退 admin（并发环境会被互踢，仅作兜底）
    console.log(`  [专用账号 ${E2E_USER} 不可用，回退 admin]`)
    token = await loginAs('admin', 'admin123')
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
const NAME_A = 'E2E支付账户' + STAMP
const NAME_UI = 'E2EUI支付账户' + STAMP

async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 0) 编号建议
  const nc = await api('GET', '/erp/finance/account/next-code')
  const ncVal = String(data(nc))
  check('next-code 生成账户编号（4 位起）', /^\d{4,}$/.test(ncVal), ncVal)

  // 1) 新增
  const createRes = await api('POST', '/erp/finance/account', {
    accountName: NAME_A,
    subjectCode: 'E2E' + STAMP,
    accountType: 1,
    accountLevel: 1,
    bankName: 'E2E 招商银行',
    bankAccount: '6225' + STAMP + '0000',
    currency: 'CNY',
    remark: 'E2E 新增备注',
  })
  const created = data(createRes)
  check('新增支付账户成功', !!(created && created.id), JSON.stringify(created).slice(0, 200))
  check('新增默认状态=启用', created?.status === 1, created?.status)
  check('新增回填账户类型/等级', created?.accountType === 1 && created?.accountLevel === 1, `${created?.accountType}/${created?.accountLevel}`)
  check('新增默认币种=CNY', created?.currency === 'CNY', created?.currency)
  check('新增默认余额=0', Number(created?.balance) === 0, created?.balance)
  const idA = created?.id
  const codeA = created?.subjectCode

  // 2) 必填/唯一校验
  const badName = await api('POST', '/erp/finance/account', { accountName: '', subjectCode: 'E2ENO' + STAMP, accountType: 1 })
  check('账户名称必填校验', badName.json?.code !== 200, badName.json?.message)
  const badCode = await api('POST', '/erp/finance/account', { accountName: 'E2E无编号' + STAMP, subjectCode: '', accountType: 1 })
  check('账户编号必填校验', badCode.json?.code !== 200, badCode.json?.message)
  const dup = await api('POST', '/erp/finance/account', { accountName: 'E2E重号' + STAMP, subjectCode: codeA, accountType: 1 })
  check('账户编号重复校验', dup.json?.code !== 200, dup.json?.message)

  // 3) 分页多条件
  const p1 = await api('GET', `/erp/finance/account/page?pageNum=1&pageSize=20&keyword=${NAME_A}&showDisabled=1`)
  check('分页按账户名称检索命中', Number(data(p1)?.total) >= 1, JSON.stringify(data(p1)?.total))
  const p1b = await api('GET', `/erp/finance/account/page?pageNum=1&pageSize=20&keyword=${codeA}&showDisabled=1`)
  check('分页按账户编号检索命中', Number(data(p1b)?.total) >= 1, JSON.stringify(data(p1b)?.total))
  const p2 = await api('GET', `/erp/finance/account/page?pageNum=1&pageSize=20&keyword=${NAME_A}&accountType=1&showDisabled=1`)
  check('分页按账户类型过滤命中', Number(data(p2)?.total) >= 1, JSON.stringify(data(p2)?.total))
  const p3 = await api('GET', `/erp/finance/account/page?pageNum=1&pageSize=20&keyword=${NAME_A}&accountType=2&showDisabled=1`)
  check('分页按账户类型过滤排除生效', Number(data(p3)?.total) === 0, JSON.stringify(data(p3)?.total))
  const p4 = await api('GET', `/erp/finance/account/page?pageNum=1&pageSize=20&keyword=${NAME_A}&accountLevel=1&showDisabled=1`)
  check('分页按账户等级过滤命中', Number(data(p4)?.total) >= 1, JSON.stringify(data(p4)?.total))
  const p5 = await api('GET', `/erp/finance/account/page?pageNum=1&pageSize=20&keyword=${NAME_A}&currency=CNY&showDisabled=1`)
  check('分页按币种过滤命中', Number(data(p5)?.total) >= 1, JSON.stringify(data(p5)?.total))
  const p6 = await api('GET', `/erp/finance/account/page?pageNum=1&pageSize=20&keyword=${NAME_A}&status=0`)
  check('分页 status=0（停用）不命中启用账户', Number(data(p6)?.total) === 0, JSON.stringify(data(p6)?.total))
  const p7 = await api('GET', `/erp/finance/account/page?pageNum=1&pageSize=1&showDisabled=1`)
  check('分页 pageSize=1 生效', (data(p7)?.records || []).length === 1 && Number(data(p7)?.total) >= 1, `records=${(data(p7)?.records || []).length}, total=${data(p7)?.total}`)
  const p8 = await api('GET', '/erp/finance/account/page?pageNum=1&pageSize=20')
  check('分页默认仅启用（不传 showDisabled）', (data(p8)?.records || []).every(r => r.status === 1), JSON.stringify((data(p8)?.records || []).map(r => r.status)))

  // 4) 详情
  const detail = await api('GET', `/erp/finance/account/${idA}`)
  check('详情返回账户编号/备注', data(detail)?.subjectCode === codeA && data(detail)?.remark === 'E2E 新增备注', JSON.stringify(data(detail)).slice(0, 200))

  // 5) 修改
  const upd = await api('PUT', `/erp/finance/account/${idA}`, {
    accountName: NAME_A + '改',
    subjectCode: codeA,
    accountType: 3,
    accountLevel: 3,
    bankName: 'E2E 工商银行',
    currency: 'USD',
    remark: 'E2E 修改备注',
  })
  check('修改支付账户成功', upd.json?.code === 200, upd.json?.message)
  const detail2 = await api('GET', `/erp/finance/account/${idA}`)
  check('修改后账户名称/类型/等级/币种生效',
    data(detail2)?.accountName === NAME_A + '改' && data(detail2)?.accountType === 3
    && data(detail2)?.accountLevel === 3 && data(detail2)?.currency === 'USD',
    JSON.stringify(data(detail2)).slice(0, 220))

  // 6) 统计（走后端，与列表同口径）
  const st1 = await api('GET', `/erp/finance/account/statistics?keyword=${NAME_A + '改'}&showDisabled=1`)
  check('统计接口按筛选返回账户总数=1', Number(data(st1)?.total) === 1, JSON.stringify(data(st1)))
  check('统计接口返回启用/停用/余额合计字段', data(st1) != null && 'enabled' in data(st1) && 'disabled' in data(st1) && 'totalBalance' in data(st1), JSON.stringify(data(st1)))
  const stAll = await api('GET', '/erp/finance/account/statistics')
  check('统计接口无参兼容（统计全部）', Number(data(stAll)?.total) >= 1, JSON.stringify(data(stAll)))

  // 7) 启停
  const off = await api('PUT', `/erp/finance/account/status/${idA}`, 0)
  check('停用支付账户成功', off.json?.code === 200, off.json?.message)
  const pOff = await api('GET', `/erp/finance/account/page?pageNum=1&pageSize=20&keyword=${codeA}&status=0`)
  check('停用后 status=0 查询命中', Number(data(pOff)?.total) >= 1, JSON.stringify(data(pOff)?.total))
  const stOff = await api('GET', `/erp/finance/account/statistics?keyword=${NAME_A + '改'}&status=0`)
  check('统计随状态过滤联动（disabled=1）', Number(data(stOff)?.disabled) === 1 && Number(data(stOff)?.enabled) === 0, JSON.stringify(data(stOff)))
  const on = await api('PUT', `/erp/finance/account/status/${idA}`, 1)
  check('启用支付账户成功', on.json?.code === 200, on.json?.message)

  // 8) 余额调整（增量口径）
  const bal1 = await api('PUT', `/erp/finance/account/balance/${idA}`, 1234.56)
  check('余额调整 +1234.56 成功', bal1.json?.code === 200, bal1.json?.message)
  const dBal1 = await api('GET', `/erp/finance/account/${idA}`)
  check('余额调整后余额=1234.56', Number(data(dBal1)?.balance) === 1234.56, data(dBal1)?.balance)
  await api('PUT', `/erp/finance/account/balance/${idA}`, -234.56)
  const dBal2 = await api('GET', `/erp/finance/account/${idA}`)
  check('余额负向调整后余额=1000.00', Number(data(dBal2)?.balance) === 1000, data(dBal2)?.balance)
  const stBal = await api('GET', `/erp/finance/account/statistics?keyword=${codeA}`)
  check('统计余额合计随余额调整联动', Number(data(stBal)?.totalBalance) === 1000, JSON.stringify(data(stBal)))

  // 9) 单一口径（红线）：与《银行账户》同源 finance_account
  const bankPage = await api('GET', `/erp/finance/bank-account/page?pageNum=1&pageSize=20&keyword=${codeA}&showDisabled=1`)
  const bankRow = (data(bankPage)?.records || []).find(r => r.id === idA)
  check('单一口径：银行账户页可查到支付账户新建的账户', !!bankRow, JSON.stringify((data(bankPage)?.records || []).length))
  check('单一口径：两页余额一致', bankRow && Number(bankRow.balance) === 1000, bankRow?.balance)

  // 10) 导出真实 xlsx
  const exp = await api('GET', `/erp/finance/account/export?keyword=${codeA}`)
  check('导出返回 xlsx（非 JSON）', exp.buf && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b, `status=${exp.status}`)
  if (exp.buf && exp.buf[0] === 0x50) {
    fs.writeFileSync(path.join(SHOTS, 'export.xlsx'), exp.buf)
    try {
      const wb = XLSX.read(exp.buf, { type: 'buffer' })
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
      check('导出内容含该支付账户', rows.some(r => String(r['账户名称'] || '').includes(NAME_A)), JSON.stringify(rows[0] || {}).slice(0, 200))
      check('导出含 10 列（账户名称…备注）', Object.keys(rows[0] || {}).length === 10, JSON.stringify(Object.keys(rows[0] || {})))
      const row = rows.find(r => String(r['账户名称'] || '').includes(NAME_A)) || {}
      check('导出账户等级/状态为中文文本', row['账户等级'] === '专用账户' && row['状态'] === '启用', `${row['账户等级']}/${row['状态']}`)
    } catch (e) {
      check('导出 xlsx 可解析', false, e.message)
    }
  }

  // 11) 删除保护（余额非 0 拒绝）→ 归零后删除
  const delBusy = await api('DELETE', `/erp/finance/account/${idA}`)
  check('余额非 0 时删除被拒', delBusy.json?.code !== 200, delBusy.json?.message)
  await api('PUT', `/erp/finance/account/balance/${idA}`, -1000)
  const delOk = await api('DELETE', `/erp/finance/account/${idA}`)
  check('余额归零后删除成功', delOk.json?.code === 200, delOk.json?.message)
  const after = await api('GET', `/erp/finance/account/page?pageNum=1&pageSize=20&keyword=${codeA}&showDisabled=1`)
  check('删除后查询不到（逻辑删除生效）', Number(data(after)?.total) === 0, JSON.stringify(data(after)?.total))

  // 12) 兼容接口 /list（全平台账户下拉用）
  const listRes = await api('GET', '/erp/finance/account/list?status=1')
  check('/list 兼容返回数组（下拉口径）', Array.isArray(data(listRes)), JSON.stringify(data(listRes) == null ? null : data(listRes).length))
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

  await openPage(`${FE}/md/payment-account`)
  const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())
  check('不再出现「只读/待接入端点」旧提示', !bodyText.includes('暂无新增/编辑/删除端点'), bodyText.slice(0, 60))

  // 统计卡片（后端 /statistics）
  const statLabels = await page.evaluate(() => [...document.querySelectorAll('.ar-stat-card__label')].map(e => (e.innerText || '').trim()))
  for (const l of ['账户总数', '启用账户', '停用账户', '余额合计']) {
    check(`统计卡片含「${l}」`, statLabels.includes(l), statLabels.join('/'))
  }

  // 表头
  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean))
  for (const h of ['账户名称', '账户编号', '账户类型', '账户等级', '开户银行', '银行账号', '账户余额', '币种', '状态']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }

  // 工具栏 / 查询区
  for (const b of ['新增账户', '刷新', '打印(F8)', '导出']) {
    check(`工具栏含「${b}」`, bodyText.includes(b.replace(/\s+/g, '')), '')
  }
  for (const q of ['筛选条件', '账户类型', '账户等级', '状态', '查询', '重置']) {
    check(`查询区含「${q}」`, bodyText.includes(q), '')
  }
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 列配置齿轮（个人配置 / 全局配置）
  await page.locator('.ss-grid .th-settings-btn').first().click()
  await page.waitForTimeout(1500)
  const colPanel = page.locator('.ant-modal-content:visible').last()
  const colText = (await colPanel.innerText()).replace(/\s+/g, '')
  check('列配置弹窗可打开（个人配置/全局配置）', colText.includes('个人配置') && colText.includes('全局配置'), colText.slice(0, 80))
  check('列配置含支付账户列', ['账户名称', '账户编号', '账户类型', '账户等级', '开户银行', '银行账号', '账户余额', '币种', '状态'].every(c => colText.includes(c)), colText.replace(/\n/g, '|').slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 页面配置弹窗
  await page.locator('button.btn-config').first().click()
  await page.waitForTimeout(1200)
  const pcModal = page.locator('.ant-modal-content:visible').last()
  const pcText = (await pcModal.innerText()).replace(/\s+/g, '')
  check('页面配置弹窗可打开', pcText.includes('页面配置'), pcText.slice(0, 60))
  check('页面配置含「查询条件 / 功能按钮」双 Tab', pcText.includes('查询条件') && pcText.includes('功能按钮'))
  check('页面配置列出查询字段', ['筛选条件', '账户类型', '账户等级', '状态'].every(c => pcText.includes(c)), pcText.replace(/\n/g, '|').slice(0, 200))
  await page.screenshot({ path: path.join(SHOTS, 'ui-pageconfig.png'), fullPage: true })
  await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
  await page.waitForTimeout(800)

  // 新增弹窗
  await page.click('button:has-text("新增账户")')
  await page.waitForTimeout(2000)
  const modal = page.locator('.ant-modal-content:visible').last()
  const modalText = (await modal.innerText()).replace(/\s+/g, '')
  for (const f of ['账户名称', '账户编号', '账户类型', '账户等级', '开户银行', '银行账号', '币种', '期初余额', '备注']) {
    check(`新增弹窗含字段「${f}」`, modalText.includes(f), '')
  }
  const codeVal = await modal.locator('input[placeholder*="资金科目编号"]').inputValue()
  check('弹窗自动带出账户编号', /^\d{4,}$/.test(codeVal), codeVal)

  await modal.locator('input[placeholder*="基本户-招行"]').fill(NAME_UI)
  await modal.locator('input[placeholder*="招商银行"]').fill('UI 招商银行')
  await modal.locator('input[placeholder*="银行账号"]').fill('62258888' + STAMP)
  // 账户等级 = 基本账户
  await modal.locator('.ant-select').nth(1).click()
  await page.waitForTimeout(500)
  await page.locator('.ant-select-item-option:has-text("基本账户")').first().click()
  await page.waitForTimeout(300)
  // 期初余额
  await modal.locator('.ant-input-number input').first().fill('888.88')
  await page.waitForTimeout(300)
  await page.screenshot({ path: path.join(SHOTS, 'ui-add-modal.png'), fullPage: true })
  await modal.locator('.ant-btn-primary:has-text("保存")').click()
  await page.waitForTimeout(3000)
  const afterSave = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('新增保存后列表出现该账户', afterSave.includes(NAME_UI.replace(/\s+/g, '')), NAME_UI)
  check('新增后余额按「期初余额」登记（888.88）', afterSave.includes('888.88'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-save.png'), fullPage: true })

  // 余额调整
  const rowUi = page.locator('.ss-grid tr', { hasText: NAME_UI }).first()
  await rowUi.locator('button:has-text("余额调整")').click()
  await page.waitForTimeout(1200)
  const balModal = page.locator('.ant-modal-content:visible').last()
  check('余额调整弹窗打开且显示当前余额', (await balModal.innerText()).includes('当前余额'), (await balModal.innerText()).replace(/\s+/g, ' ').slice(0, 80))
  await balModal.locator('.ant-input-number input').first().fill('111.12')
  await page.waitForTimeout(400)
  await balModal.locator('.ant-btn-primary:has-text("确认调整")').click()
  await page.waitForTimeout(3000)
  const afterBal = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('余额调整后列表余额=1,000.00', afterBal.includes('1,000.00'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-balance.png'), fullPage: true })

  // 行内「修改」
  await page.click(`.ss-grid a:has-text("${NAME_UI}")`)
  await page.waitForTimeout(2000)
  const editModal = page.locator('.ant-modal-content:visible').last()
  const editName = await editModal.locator('input[placeholder*="基本户-招行"]').inputValue()
  check('修改弹窗回填账户名称', editName === NAME_UI, editName)
  check('修改弹窗回填账户等级', (await editModal.innerText()).includes('基本账户'), '')
  await editModal.locator('input[placeholder*="招商银行"]').fill('UI 工商银行改')
  await page.waitForTimeout(300)
  await editModal.locator('.ant-btn-primary:has-text("保存")').click()
  await page.waitForTimeout(3000)
  const afterEdit = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('修改保存后列表显示新开户银行', afterEdit.includes('UI工商银行改'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-edit.png'), fullPage: true })

  // 行内「停用」（默认状态=全部，停用后行仍在但状态标签变「停用」）
  /** 取该行「状态」列文字（避开操作列的 停用/启用 按钮文案） */
  async function rowStatusText(rowLocator) {
    const tds = rowLocator.locator('td')
    const n = await tds.count()
    for (let i = 0; i < n; i++) {
      const t = ((await tds.nth(i).innerText()) || '').trim()
      if (t === '启用' || t === '停用') return t
    }
    return ''
  }
  const row2 = page.locator('.ss-grid tr', { hasText: NAME_UI }).first()
  const statusBefore = await rowStatusText(row2)
  check('停用前状态标签为「启用」', statusBefore === '启用', statusBefore)
  await row2.locator('button:has-text("停用")').click()
  await page.waitForTimeout(800)
  const confirmBox = page.locator('.ant-modal-confirm:visible').last()
  if (await confirmBox.count()) {
    await confirmBox.locator('.ant-btn-primary').click()
    await page.waitForTimeout(2500)
  }
  const row3 = page.locator('.ss-grid tr', { hasText: NAME_UI }).first()
  const statusAfter = await rowStatusText(row3)
  check('停用后状态标签变为「停用」', statusAfter === '停用', statusAfter)
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-disable.png'), fullPage: true })

  // 行内「删除」（余额需先归零：调整 -1000）
  await row3.locator('button:has-text("余额调整")').click()
  await page.waitForTimeout(1200)
  const balModal2 = page.locator('.ant-modal-content:visible').last()
  await balModal2.locator('.ant-input-number input').first().fill('-1000')
  await page.waitForTimeout(400)
  await balModal2.locator('.ant-btn-primary:has-text("确认调整")').click()
  await page.waitForTimeout(2800)
  const row4 = page.locator('.ss-grid tr', { hasText: NAME_UI }).first()
  await row4.locator('button:has-text("删除")').click()
  await page.waitForTimeout(1200)
  const delBox = page.locator('.ant-modal-confirm:visible').last()
  if (await delBox.count()) {
    await delBox.locator('button:has-text("确认删除")').click()
    await page.waitForTimeout(2800)
  }
  const afterDel = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('行内删除支付账户成功', !afterDel.includes(NAME_UI.replace(/\s+/g, '')), NAME_UI)
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-delete.png'), fullPage: true })

  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await login()
    console.log(`登录成功（${E2E_USER}）`)
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
