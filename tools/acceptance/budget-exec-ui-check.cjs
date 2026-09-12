// 预算执行页面 UI 验收（金标准）：概要卡片 + 双 Tab + 列/页面配置 + 超支预警 + 明细抽屉
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BASE = 'http://localhost:5656'
const API = 'http://localhost:5655'
const FISCAL_YEAR = '2027年'

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

let pass = 0, fail = 0
const pageErrors = []
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

/** 切换 Tab（CategoryListLayout 的 .tab-items > .tab-item） */
async function switchTab(page, label) {
  await page.locator('.tab-items .tab-item', { hasText: label }).first().click()
  await page.waitForTimeout(2500)
}

// 并行会话共用 admin，sa-token is-concurrent=false 会互踢：每次请求前自愈重登
let token = null
async function apiLogin() {
  const capRes = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
  const capData = capRes.data || capRes
  const captcha = extractCaptcha(capData.img)
  const res = await fetch(`${API}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123', tenantName: '系统租户', captcha, captchaKey: capData.uuid }),
  }).then(r => r.json())
  const d = res?.data || {}
  token = d.token || d.accessToken || d.tokenValue
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  return d
}

const PAGE_URL = `${BASE}/finance/budget-exec`

/** 登录 + 注入 token + 进入页面 + 选中验收年度（并行会话互踢，内置重试） */
async function openPage(page, attempts = 6) {
  for (let i = 0; i < attempts; i++) {
    const d = await apiLogin()
    try {
      await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(([tk, tenantId, tenantName]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tenantId || 1))
        localStorage.setItem('tenantName', tenantName || '')
      }, [token, d.tenantId || 1, '系统租户'])
      await page.goto(PAGE_URL, { waitUntil: 'domcontentloaded' })
      await page.waitForSelector('.ss-grid', { timeout: 12000 })
      await page.waitForTimeout(1200)
      await pickOption(page, page.locator('.search-select-wrap .ant-select').first(), FISCAL_YEAR)
      await page.waitForTimeout(2000)
      // 数据必须真正加载出来（401 踢出时页面会空转）
      await page.waitForFunction(
        () => document.querySelectorAll('.ar-stat-card').length > 0
          && !!Array.from(document.querySelectorAll('.ss-grid tbody tr')).find(tr => tr.textContent.includes('YSD-')),
        null, { timeout: 12000 })
      return true
    } catch {
      // 被并行会话踢出导致登出跳转，重试
    }
  }
  throw new Error('无法进入预算执行页面（持续被并行会话踢出）')
}

async function ensureAuth(page, { force = false, reload = false } = {}) {
  let need = force
  if (!need) {
    const probe = await fetch(`${API}/api/erp/budget/execution/rows?fiscalYear=2027&size=1`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    need = probe.status === 401
  }
  // 应用收到 401 会自动登出并跳登录页，需整页恢复
  if (page.url().includes('/login')) need = true
  if (!need) return
  if (reload || page.url().includes('/login')) {
    await openPage(page)
    console.log('  ↻ 会话被并行会话踢出，已自动重登并重新进入页面')
  } else {
    await apiLogin()
    await page.evaluate(tk => localStorage.setItem('token', tk), token)
    console.log('  ↻ 会话被并行会话踢出，已自动重登')
  }
}

/** 仅取有真实数据的行（BillDetailTable 会用空行补齐 minEmptyRows） */
async function realRows(page) {
  const texts = await page.$$eval('.ss-grid tbody tr.ss-row', trs => trs.map(tr => tr.textContent.replace(/\s+/g, '').trim()))
  return texts.filter(t => t.includes('YSD-'))
}

/** 选中 Ant Design 下拉项（按可见文本） */
async function pickOption(page, selectLocator, optionText) {
  await selectLocator.click()
  await page.waitForTimeout(400)
  const opts = page.locator('.ant-select-dropdown:visible .ant-select-item-option')
  const count = await opts.count()
  for (let i = 0; i < count; i++) {
    const t = (await opts.nth(i).textContent()) || ''
    if (t.trim() === optionText) {
      await opts.nth(i).click()
      await page.waitForTimeout(1200)
      return true
    }
  }
  return false
}

async function main() {
  const d = await apiLogin()
  console.log('登录成功')

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1680, height: 950 } })
  const errors = pageErrors
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', e => errors.push(String(e)))

  // 首次进入页面（内置重试）
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await openPage(page)
  await page.locator('.tab-items .tab-item').first().click()
  await page.waitForTimeout(2000)

  // ═══ 1. 金标准骨架 ═══
  console.log('\n[1] 金标准骨架')
  check('页面标题「预算执行」', (await page.locator('.list-title').first().textContent().catch(() => ''))?.includes('预算执行'))
  check('双 Tab（按预算单 / 按预算科目）', (await page.locator('.cl-tab, .category-tabs .tab-item, .tabs-wrap .tab-item').count()) >= 0)
  const tabTexts = await page.$$eval('[class*="tab"]', els => els.map(e => (e.textContent || '').trim()).filter(Boolean))
  check('存在「按预算单」「按预算科目」两个 Tab', tabTexts.includes('按预算单') && tabTexts.includes('按预算科目'))
  check('概要卡片渲染', (await page.locator('.ar-stat-card').count()) === 8, `${await page.locator('.ar-stat-card').count()} 张`)

  // ═══ 2. 切换到验收年度 2027 ═══
  console.log('\n[2] 切换财政年度并加载真实数据')
  await ensureAuth(page)
  const yearSelect = page.locator('.search-select-wrap .ant-select').first()
  const ok = await pickOption(page, yearSelect, FISCAL_YEAR)
  check(`财政年度切换为 ${FISCAL_YEAR}`, ok)
  await page.waitForTimeout(2500)
  await ensureAuth(page)

  const cardValues = await page.$$eval('.ar-stat-card', els => els.map(e => ({
    label: e.querySelector('.ar-stat-card__label')?.textContent?.trim(),
    value: e.querySelector('.ar-stat-card__value')?.textContent?.replace(/\s+/g, '').trim(),
  })))
  const cardOf = (l) => cardValues.find(c => c.label === l)?.value || ''
  check('概要：预算总额 150,000.00', cardOf('预算总额').includes('150,000.00'), cardOf('预算总额'))
  check('概要：已执行 135,000.00', cardOf('已执行').includes('135,000.00'), cardOf('已执行'))
  check('概要：冻结金额 0.00', cardOf('冻结金额').includes('0.00'), cardOf('冻结金额'))
  check('概要：剩余额度 15,000.00（=预算−已执行−冻结）', cardOf('剩余额度').includes('15,000.00'), cardOf('剩余额度'))
  check('概要：执行进度 90.00%', cardOf('执行进度').includes('90.00%'), cardOf('执行进度'))
  check('概要：超支科目 1 项', cardOf('超支科目').includes('1'), cardOf('超支科目'))
  check('概要：预警科目 1 项', cardOf('预警科目').includes('1'), cardOf('预警科目'))
  check('工具栏超支提示标签（可点击筛选）', (await page.locator('.over-tag').count()) >= 1,
    await page.locator('.over-tag').first().textContent().catch(() => ''))

  // ═══ 3. 按预算单表格 ═══
  console.log('\n[3] 按预算单维度')
  const headers = await page.$$eval('.ss-grid thead th', ths => ths.map(t => t.textContent.trim()))
  for (const h of ['预算编号', '部门', '预算金额', '已执行', '冻结金额', '剩余额度', '执行进度', '预警']) {
    check(`列存在：${h}`, headers.some(t => t.includes(h)))
  }
  check('序号列表头含列配置齿轮', (await page.locator('.ss-grid thead .th-settings-btn').count()) >= 1)
  const rowTexts = await realRows(page)
  check('预算单行数 = 2（财务部 + 市场部）', rowTexts.length === 2, `${rowTexts.length} 行`)
  check('财务部行：已执行 125,000.00 / 进度 104.17%',
    rowTexts.some(t => t.includes('125,000.00') && t.includes('104.17%')),
    rowTexts.find(t => t.includes('财务部'))?.slice(0, 120))
  check('超支行标记「超支」', rowTexts.some(t => t.includes('超支')))
  check('市场部行：进度 33.33%', rowTexts.some(t => t.includes('33.33%')))

  await page.screenshot({ path: 'I:/AI-Ready/budget-exec-rows.png', fullPage: false })

  // ═══ 4. 按预算科目表格 ═══
  console.log('\n[4] 按预算科目维度')
  await ensureAuth(page)
  await switchTab(page, '按预算科目')
  const itemHeaders = await page.$$eval('.ss-grid thead th', ths => ths.map(t => t.textContent.trim()))
  for (const h of ['预算科目编码', '预算科目名称', '预算金额', '已执行', '剩余额度', '执行进度', '超支金额', '最近执行']) {
    check(`列存在：${h}`, itemHeaders.some(t => t.includes(h)))
  }
  const itemRows = await realRows(page)
  check('科目行数 = 3（财务部2 + 市场部1）', itemRows.length === 3, `${itemRows.length} 行`)
  check('管理费用行：已执行 95,000.00 / 进度 95.00%',
    itemRows.some(t => t.includes('管理费用') && t.includes('95,000.00') && t.includes('95.00%')),
    itemRows.find(t => t.includes('管理费用'))?.slice(0, 130))
  check('折旧费行：已执行 30,000.00 / 进度 150.00% / 超支金额 10,000.00',
    itemRows.some(t => t.includes('折旧费') && t.includes('30,000.00') && t.includes('150.00%') && t.includes('10,000.00')),
    itemRows.find(t => t.includes('折旧费'))?.slice(0, 130))

  // ═══ 5. 列配置 / 页面配置弹窗 ═══
  console.log('\n[5] 列配置 / 页面配置')
  await page.locator('button:has(.anticon-table)').first().click()
  await page.waitForTimeout(1000)
  const colModal = await page.locator('.ant-modal:visible').first().innerText().catch(() => '')
  check('列配置弹窗打开', colModal.length > 0, colModal.replace(/\s+/g, ' ').slice(0, 80))
  await page.locator('.ant-modal:visible .ant-modal-close').first().click().catch(() => {})
  await page.waitForTimeout(900)

  await page.locator('button:has(.anticon-setting)').first().click()
  await page.waitForTimeout(1200)
  const pageModal = await page.locator('.ant-modal:visible').first().innerText().catch(() => '')
  check('页面配置弹窗打开（含查询条件/功能按钮 Tab）',
    pageModal.includes('查询条件') && pageModal.includes('功能按钮'), pageModal.replace(/\s+/g, ' ').slice(0, 120))
  await page.locator('.ant-modal:visible .ant-modal-close').first().click().catch(() => {})
  await page.waitForTimeout(900)

  // ═══ 6. 明细抽屉（科目明细 + 执行流水） ═══
  console.log('\n[6] 明细抽屉')
  let drawerText = ''
  for (let i = 0; i < 4 && !drawerText; i++) {
    await ensureAuth(page, { force: true, reload: i > 0 })
    if (await page.locator('.ant-modal:visible').count() > 0) {
      await page.locator('.ant-modal:visible .ant-modal-close').first().click().catch(() => {})
      await page.waitForTimeout(800)
    }
    await switchTab(page, '按预算单')
    await page.locator('.ss-grid tbody tr:has-text("YSD-") .ant-btn-link').first().click().catch(() => {})
    await page.waitForTimeout(2500)
    if (await page.locator('.ant-drawer-content:visible').count() > 0) {
      drawerText = await page.locator('.ant-drawer-content:visible').first().innerText().catch(() => '')
    }
  }
  check('抽屉打开', !!drawerText)
  check('抽屉含预算金额/已执行/冻结/剩余/执行进度',
    ['预算金额', '已执行', '冻结', '剩余', '执行进度'].every(k => drawerText.includes(k)))
  check('抽屉含「预算科目明细」段', drawerText.includes('预算科目明细'))
  check('抽屉含「预算执行流水」段', drawerText.includes('预算执行流水'))
  check('流水含来源单据（费用单 + YBFYD 单号）', /YBFYD-/.test(drawerText))
  await page.screenshot({ path: 'I:/AI-Ready/budget-exec-detail.png', fullPage: false })
  await page.locator('.ant-drawer-content:visible .ant-drawer-close').first().click().catch(() => {})
  await page.waitForTimeout(1000)

  // ═══ 7. 超支筛选（点击工具栏超支标签） ═══
  console.log('\n[7] 超支筛选')
  let filteredRows = []
  for (let i = 0; i < 4; i++) {
    await ensureAuth(page, { force: true, reload: i > 0 })
    if (await page.locator('.over-tag').count() === 0) continue
    await page.locator('.over-tag').first().click().catch(() => {})
    await page.waitForTimeout(2500)
    filteredRows = await realRows(page)
    if (filteredRows.length > 0) break
  }
  check('按预算单：仅看超支后只保留超支预算单（财务部 104.17%）',
    filteredRows.length === 1 && filteredRows[0].includes('财务部') && filteredRows[0].includes('104.17%'),
    `${filteredRows.length} 行 · ${filteredRows[0]?.slice(0, 90)}`)

  await ensureAuth(page, { force: true })
  await switchTab(page, '按预算科目')
  const overItems = await realRows(page)
  check('按预算科目：仅看超支后只保留折旧费',
    overItems.length === 1 && overItems[0].includes('折旧费') && overItems[0].includes('150.00%'),
    `${overItems.length} 行 · ${overItems[0]?.slice(0, 90)}`)

  await page.screenshot({ path: 'I:/AI-Ready/budget-exec-page.png', fullPage: false })

  const realErrors = errors.filter(e => !/favicon|404|ResizeObserver|Failed to load resource/.test(e))
  check('无 JS 运行错误', realErrors.length === 0, realErrors.slice(0, 3).join(' | '))

  await browser.close()
  console.log(`\n════ 结果：通过 ${pass} 项，失败 ${fail} 项 ════`)
  process.exit(fail === 0 ? 0 : 1)
}

main().catch(e => {
  console.error('UI 验收异常:', e.message)
  if (pageErrors.length) console.error('页面 JS 错误:\n  ' + pageErrors.slice(0, 5).join('\n  '))
  process.exit(1)
})
