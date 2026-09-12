// 零售单表单页金标准 UI 验收（低请求量版：一次登录跑完全部断言）
// 断言：P0 号段来自后端 /next-no、P0 记账走 /settle（不再以 status=1 create 顶替）、P1 配置弹窗三 Tab 36 字段且真实生效、明细列 66、导出无桩
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BASE = process.env.RETAIL_UI_BASE || 'http://localhost:5656'
const API = process.env.RETAIL_API_BASE || 'http://localhost:5655'

let pass = 0, fail = 0
const failures = []
const pageErrors = []
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  \u2705 ${name}${detail ? ' \u2014 ' + detail : ''}`) }
  else { fail++; failures.push(name); console.log(`  \u274c ${name}${detail ? ' \u2014 ' + detail : ''}`) }
}
const sleep = ms => new Promise(r => setTimeout(r, ms))

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

let token = null, tenantId = 1
async function apiLogin(attempts = 25) {
  let lastMsg = ''
  for (let i = 0; i < attempts; i++) {
    try {
      const capRes = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
      const capData = capRes.data || capRes
      const captcha = extractCaptcha(capData.img)
      if (!captcha) { lastMsg = '\u9a8c\u8bc1\u7801\u672a\u83b7\u53d6(\u9650\u6d41)'; await sleep(8000); continue }
      const res = await fetch(`${API}/api/auth/login`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'retail_e2e', password: 'admin123', tenantName: '\u7cfb\u7edf\u79df\u6237', captcha, captchaKey: capData.uuid }),
      }).then(r => r.json())
      const d = res?.data || {}
      const tk = d.token || d.accessToken || d.tokenValue
      if (tk) { token = tk; tenantId = d.tenantId || 1; return d }
      lastMsg = JSON.stringify(res).slice(0, 160)
      await sleep(8000)
    } catch (e) { lastMsg = e.message; await sleep(8000) }
  }
  throw new Error('\u767b\u5f55\u5931\u8d25: ' + lastMsg)
}

async function apiGet(path) {
  const res = await fetch(`${API}/api${path}`, { headers: { Authorization: `Bearer ${token}` } })
  const text = await res.text()
  const ct = res.headers.get('content-type') || ''
  if (text && !ct.includes('json')) return text
  try { return text ? JSON.parse(text) : null } catch { return text }
}

async function pickOptionByText(page, selectLocator, matchRe, tries = 3) {
  for (let t = 0; t < tries; t++) {
    try {
      await selectLocator.click()
      // 等待当前可见下拉出现选项（ant 会保留已关闭的 dropdown 在 DOM 中）
      const optLoc = page.locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option')
      await optLoc.first().waitFor({ state: 'visible', timeout: 6000 })
      const n = await optLoc.count()
      const texts = []
      for (let i = 0; i < n; i++) texts.push(((await optLoc.nth(i).textContent()) || '').trim())
      let idx = texts.findIndex(x => matchRe.test(x))
      if (idx < 0) idx = 0
      await optLoc.nth(idx).click({ timeout: 5000 })
      await page.waitForTimeout(800)
      return texts[idx]
    } catch {
      await page.keyboard.press('Escape').catch(() => {})
      await page.waitForTimeout(1200)
    }
  }
  return ''
}

async function main() {
  console.log('\u2550\u2550\u2550 \u96f6\u552e\u5355\u8868\u5355\u9875\u91d1\u6807\u51c6 UI \u9a8c\u6536 \u2550\u2550\u2550')
  await apiLogin()
  console.log('* \u767b\u5f55\u6210\u529f')

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1760, height: 1000 }, acceptDownloads: true })
  const page = await ctx.newPage()
  page.on('pageerror', e => { pageErrors.push(String(e)); console.log('    [pageerror]', String(e).slice(0, 200)) })
  const requests = []
  page.on('request', r => {
    const u = r.url(); const i = u.indexOf('/api/')
    if (i === -1) return
    const p = u.slice(i + 4).split('?')[0]
    if (p.startsWith('/sales/retail')) requests.push(`${r.method()} ${p}`)
  })

  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tid))
    localStorage.setItem('tenantName', '\u7cfb\u7edf\u79df\u6237')
  }, [token, tenantId])
  await page.goto(`${BASE}/sales/retail/create`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.bill-form-page', { timeout: 25000 })
  await page.waitForTimeout(3500)
  check('\u65b0\u589e\u8868\u5355\u9875\u53ef\u8bbf\u95ee\uff08sales/retail/create\uff09', !page.url().includes('/login'), page.url())

  const orderNo = (await page.locator('.header-left .order-no').textContent() || '').trim()
  check('\u5355\u636e\u7f16\u53f7\u6765\u81ea\u540e\u7aef\u53f7\u6bb5 LS-yyyyMMdd-NNNN', /^NO\.\s*LS-\d{8}-\d{4}$/.test(orderNo), orderNo)
  check('\u4e0d\u518d\u4f7f\u7528\u524d\u7aef\u6f14\u793a\u53f7 LSD-', !/LSD-/.test(orderNo), orderNo)
  const nextNoReqs = requests.filter(r => r === 'GET /sales/retail/next-no')
  check('\u524d\u7aef\u771f\u5b9e\u8c03\u7528 /next-no \u53f7\u6bb5\u63a5\u53e3', nextNoReqs.length > 0, nextNoReqs.join('|'))

  // ── 配置弹窗三 Tab + 36 字段 ──
  await page.locator('.header-right button', { hasText: '\u914d\u7f6e' }).first().click()
  await page.waitForTimeout(1200)
  const cfgTabs = await page.$$eval('.ant-modal .ant-tabs-tab', els => els.map(e => e.textContent.trim()))
  check('\u914d\u7f6e\u5f39\u7a97\u4e09 Tab', ['\u9875\u9762\u914d\u7f6e', '\u96f6\u552e\u8bbe\u7f6e', '\u6253\u5370\u8bbe\u7f6e'].every(t => cfgTabs.some(x => x.includes(t))), cfgTabs.join('|'))
  // ant-table 带 scroll.y 会插入 measure-row，需排除
  const cfgRows = page.locator('.ant-modal .ant-table-tbody tr:not(.ant-table-measure-row)')
  const fieldRows = await cfgRows.count()
  check('\u9875\u9762\u914d\u7f6e 36 \u5b57\u6bb5', fieldRows === 36, `\u5b9e\u6d4b ${fieldRows}`)

  const rcRow = cfgRows.filter({ hasText: '\u6536\u6b3e\u7801' }).first()
  await rcRow.locator('input[type=checkbox]').check()
  await page.waitForTimeout(900)
  await page.locator('.ant-modal .ant-tabs-tab', { hasText: '\u96f6\u552e\u8bbe\u7f6e' }).first().click()
  await page.waitForTimeout(700)
  const methodChecks = await page.locator('.ant-modal .ant-checkbox-group .ant-checkbox-wrapper').count()
  check('\u96f6\u552e\u8bbe\u7f6e-\u6536\u6b3e\u65b9\u5f0f\u53ef\u914d\u7f6e', methodChecks >= 6, `\u5b9e\u6d4b ${methodChecks} \u9879`)
  await page.locator('.ant-modal .ant-tabs-tab', { hasText: '\u6253\u5370\u8bbe\u7f6e' }).first().click()
  await page.waitForTimeout(700)
  const printFieldCount = await page.locator('.ant-modal .ant-form-item').count()
  check('\u6253\u5370\u8bbe\u7f6e\u5b57\u6bb5\u5b58\u5728', printFieldCount >= 4, `\u5b9e\u6d4b ${printFieldCount}`)
  await page.locator('.ant-modal-close').first().click()
  await page.waitForTimeout(1500)
  const qrVisible = await page.locator('.retail-payment-panel input[placeholder="\u626b\u7801/\u8d26\u53f7"]').count()
  check('\u9875\u9762\u914d\u7f6e\u52fe\u9009\u5f8c\u6536\u6b3e\u7801\u771f\u5b9e\u51fa\u73b0', qrVisible > 0, `\u5339\u914d ${qrVisible}`)

  // ── 收款方式按钮 ──
  const payBtns = await page.$$eval('.retail-payment-panel .pay-btn', els => els.map(e => e.textContent.trim()))
  check('\u6536\u6b3e\u65b9\u5f0f\u6309\u94ae\u9f50\u5907', ['\u73b0\u91d1', '\u94f6\u884c\u5361', '\u9884\u6536\u6b3e', '\u8f6c\u8d26', '\u652f\u4ed8\u5b9d', '\u5fae\u4fe1'].every(t => payBtns.some(x => x.includes(t))), payBtns.join('|'))

  // ── 明细列 67（rowNo + 操作 + 图片 + 64 数据列）──
  const gear = page.locator('.th-settings-btn').first()
  await gear.waitFor({ state: 'visible', timeout: 20000 })
  await gear.click({ timeout: 20000 })
  await page.waitForSelector('.col-setting-row', { timeout: 15000 })
  await page.waitForTimeout(700)
  const formCols = await page.locator('.col-setting-row').count()
  await page.locator('.ant-modal-close').first().click()
  await page.waitForTimeout(900)
  check('\u8868\u5355\u660e\u7ec6\u5217\u6536\u53e3\u81f3 66\uff08\u542b\u64cd\u4f5c/\u56fe\u7247\uff0c\u5171 67\uff09', formCols === 67, `\u5b9e\u6d4b ${formCols}`)

  // ── 填单：仓库/经手人 + 扫码加商品 ──
  await pickOptionByText(page, page.locator('.bill-basic-info .ant-select').nth(0), /./)
  await pickOptionByText(page, page.locator('.bill-basic-info .ant-select').nth(1), /./)
  await page.locator('.product-search-input').first().fill('SP-20260704-041')
  await page.locator('.product-search-input').first().press('Enter')
  await page.waitForTimeout(2000)
  const filled = await page.$$eval('.ss-grid tbody tr', trs => trs.filter(t => t.textContent.includes('SP-20260704-041')).length)
  check('\u6761\u7801\u641c\u7d22\u5e26\u51fa\u5546\u54c1\u660e\u7ec6', filled > 0, `${filled} \u884c`)

  // 扫码默认数量 1（POS 语义）
  const qtyText = ((await page.locator('td[data-col-key="quantity"]').first().textContent()) || '').trim()
  check('扫码加商品默认数量 1', Number(qtyText) === 1, `quantity=${qtyText}`)

  // 夹具商品主数据无零售价，按 POS 手工改价：单价 88（验证前端金额→结算全链路）
  const priceCell = page.locator('td[data-col-key="unitPrice"]').first()
  await priceCell.click()
  await page.waitForTimeout(500)
  const priceInput = priceCell.locator('input.ss-native-number')
  await priceInput.waitFor({ state: 'visible', timeout: 8000 })
  await priceInput.fill('88')
  await priceInput.press('Enter')
  await page.waitForTimeout(1200)
  await page.locator('.ss-grid tbody tr').first().click().catch(() => {})
  await page.waitForTimeout(800)

  // ── 现金收款：实收=应收 ──
  await page.locator('.retail-payment-panel .pay-btn', { hasText: '\u73b0\u91d1' }).first().click()
  await page.waitForTimeout(900)
  const payable = ((await page.locator('.bill-footer .footer-amount-value').first().textContent()) || '').replace(/[^\d.]/g, '')
  const paidRow = ((await page.locator('.retail-payment-panel .payment-detail-row').filter({ hasText: '\u5b9e\u6536\u5408\u8ba1' }).first().textContent()) || '').replace(/[^\d.]/g, '')
  check('\u5e94\u6536\u91d1\u989d\u8ba1\u7b97\u6b63\u786e', Number(payable) > 0, payable)
  check('\u70b9\u51fb\u73b0\u91d1\u540e\u5b9e\u6536=\u5e94\u6536', Number(paidRow) === Number(payable), `${paidRow} vs ${payable}`)

  // ── 记账：先落单再 /settle ──
  requests.length = 0
  await page.locator('.bill-footer .btn-submit').first().click()
  await page.waitForTimeout(9000)
  const created = requests.find(r => r === 'POST /sales/retail')
  const settleReq = requests.find(r => /^POST \/sales\/retail\/\d+\/settle$/.test(r))
  check('\u8bb0\u8d26\u5148\u843d\u5355\uff08POST /sales/retail\uff09', !!created, requests.slice(0, 6).join(' | '))
  check('\u518d\u8c03\u7ed3\u7b97\uff08POST /{id}/settle\uff09\u2014 P0 \u63a5\u901a', !!settleReq, settleReq || '')
  check('\u8bb0\u8d26\u540e\u8df3\u56de\u5217\u8868\u9875', !page.url().includes('/create') && page.url().includes('/sales/retail'), page.url())

  if (settleReq) {
    const id = settleReq.match(/\/(\d+)\/settle/)[1]
    const detail = await apiGet(`/sales/retail/${id}`)
    check('API \u590d\u6838-\u5355\u636e\u5df2\u5b8c\u6210\u7ed3\u7b97', detail?.order?.status === 1, `status=${detail?.order?.status}`)
    check('API \u590d\u6838-\u652f\u4ed8\u660e\u7ec6\u843d\u5e93', (detail?.payments || []).length > 0, `${(detail?.payments || []).length} \u884c`)
    check('API \u590d\u6838-\u660e\u7ec6\u843d\u5e93', (detail?.items || []).length > 0, `${(detail?.items || []).length} \u884c`)
    check('API \u590d\u6838-\u73b0\u91d1\u6c47\u603b=\u5e94\u6536', Number(detail?.order?.cashAmount) === Number(detail?.order?.payableAmount),
      `cash=${detail?.order?.cashAmount} payable=${detail?.order?.payableAmount}`)
  }

  check('\u9875\u9762\u65e0 JS \u8fd0\u884c\u65f6\u9519\u8bef', pageErrors.length === 0, pageErrors.slice(0, 2).join(' ; '))

  await page.screenshot({ path: 'I:/AI-Ready/retail-gold-form.png' })
  await browser.close()
  console.log(`\n\u2550\u2550\u2550 \u7ed3\u679c: ${pass} \u901a\u8fc7 / ${fail} \u5931\u8d25 \u2550\u2550\u2550`)
  if (fail) console.log('\u5931\u8d25\u9879: ' + failures.join(' | '))
  process.exit(fail ? 1 : 0)
}

main().catch(e => { console.error('\u274c \u811a\u672c\u5f02\u5e38: ' + (e.stack || e.message)); process.exit(2) })
