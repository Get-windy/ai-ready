/* 商品表单页（3 Tab + 商品单位明细 + 批量价格计算）浏览器端验收
 * 运行：node tools/e2e-product-form-ui.cjs [apiPort] [vitePort]
 */
const http = require('http')
const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

const API = Number(process.argv[2] || 5677)
const VITE = Number(process.argv[3] || 5656)
const OUT = path.resolve(__dirname, '../tool-results/product-ui')
const results = []

function apiReq(method, p, body) {
  return new Promise((resolve, reject) => {
    const d = body == null ? null : JSON.stringify(body)
    const h = { 'Content-Type': 'application/json' }
    if (d) h['Content-Length'] = Buffer.byteLength(d)
    const r = http.request({ hostname: '127.0.0.1', port: API, path: '/api' + p, method, headers: h }, rs => {
      const ch = []
      rs.on('data', c => ch.push(c))
      rs.on('end', () => { try { resolve(JSON.parse(Buffer.concat(ch).toString('utf8'))) } catch { resolve({}) } })
    })
    r.on('error', reject)
    if (d) r.write(d)
    r.end()
  })
}

async function getToken() {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username: process.env.E2E_USER || 'e2e_product', password: process.env.E2E_PASS || 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  return res?.data?.token
}

function check(name, ok, detail) {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail !== undefined ? '  — ' + detail : ''}`)
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const token = await getToken()
  if (!token) throw new Error('接口登录失败')

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  await page.route(u => u.pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    if (u.pathname.includes('/sse/')) return route.abort().catch(() => {})
    const resp = await route.fetch({ url: `http://127.0.0.1:${API}${u.pathname}${u.search}` })
    await route.fulfill({ response: resp })
  })
  await page.addInitScript(t => {
    localStorage.setItem('token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
  }, token)

  const url = `http://localhost:${VITE}/erp/product/create`
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(9000)

  const isLoginPage = async () => (await page.locator('input[placeholder="请输入租户名称"]').count()) > 0
  if (await isLoginPage()) {
    console.log('  [heal] 检测到登录页，重登后再来')
    const t = await getToken()
    await page.evaluate(tk => localStorage.setItem('token', tk), t)
    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(9000)
  }

  const title = await page.locator('.create-title').first().innerText().catch(() => '')
  check('F1 表单页标题(新增商品)', title.includes('新增商品'), title)

  const tabs = await page.$$eval('.create-tabs-left .ant-tabs-tab, .ant-tabs-tab', els =>
    els.map(e => (e.innerText || '').trim()).filter(Boolean))
  check('F2 三个Tab(基本信息/商品图片/商城信息)',
    ['基本信息', '商品图片', '商城信息'].every(t => tabs.includes(t)), JSON.stringify(tabs.slice(0, 8)))

  const cards = await page.$$eval('.create-card .ant-card-head-title', els => els.map(e => (e.innerText || '').trim()))
  check('F3 基本信息分区(商品特性/类型认定/基础信息/商品单位)',
    cards.some(c => c.includes('商品特性')) && cards.some(c => c.includes('商品类型认定')) &&
    cards.some(c => c.includes('基础信息')) && cards.some(c => c.includes('商品单位')), JSON.stringify(cards.slice(0, 6)))

  // 商品单位明细表列（含 8 个价格等级）
  const unitHeads = await page.$$eval('.ss-grid thead th', ths => ths.map(t => (t.innerText || '').trim()).filter(Boolean))
  const unitExpect = ['单位名称', '换算关系', '条码', '预设进价', '参考成本', '最近进价', '批发价', '零售价', '最低售价', '最低折扣(%)']
  check('F4 商品单位明细基础列', unitExpect.every(h => unitHeads.includes(h)), JSON.stringify(unitHeads))

  // 单位选择器（销售常用单位/采购常用单位/库存单位）
  const unitSelects = await page.locator('text=销售常用单位').count()
  const purchaseSelects = await page.locator('text=采购常用单位').count()
  const stockSelects = await page.locator('text=库存单位').count()
  check('F5 单位选择(销售/采购/库存常用单位)', unitSelects > 0 && purchaseSelects > 0 && stockSelects > 0)

  // 新增默认 3 行单位
  const unitRows = await page.$$eval('.ss-grid tbody tr', trs => trs.length)
  check('F6 新增默认小/中/大单位行', unitRows >= 3, `${unitRows} 行`)

  // 批量价格计算（真实弹窗，非桩）
  await page.locator('button:has-text("批量价格计算")').first().click().catch(() => {})
  await page.waitForTimeout(1200)
  const calcTitle = await page.locator('.ant-modal-title:has-text("批量价格计算")').count()
  const calcFields = await page.locator('text=零售价加价率(%)').count()
  check('F7 批量价格计算弹窗(基准价/加价率/等级折扣)', calcTitle > 0 && calcFields > 0)
  await page.screenshot({ path: path.join(OUT, 'form-price-calc.png') })
  await page.keyboard.press('Escape')
  await page.waitForTimeout(600)

  // 选择单位组
  const groupBtn = await page.locator('button:has-text("选择单位组")').count()
  check('F8 选择单位组入口', groupBtn > 0)

  // 切到商品图片 / 商城信息
  await page.locator('.ant-tabs-tab:has-text("商品图片")').first().click()
  await page.waitForTimeout(1500)
  const imgHint = await page.evaluate(() => (document.body.innerText || '').includes('最多支持5张'))
  check('F9 商品图片Tab(主图说明/上传/主图视频)', imgHint)
  await page.screenshot({ path: path.join(OUT, 'form-images.png') })

  await page.locator('.ant-tabs-tab:has-text("商城信息")').first().click()
  await page.waitForTimeout(1500)
  const mallText = await page.evaluate(() => (document.body.innerText || '').replace(/\s+/g, ' '))
  check('F10 商城信息Tab(上架/标题/标签/排序/积分/关键字)',
    mallText.includes('立即上架') && mallText.includes('商城显示标题') && mallText.includes('商品标签') &&
    mallText.includes('排序方式') && mallText.includes('商品积分') && mallText.includes('关键字'),
    mallText.slice(0, 60))
  await page.screenshot({ path: path.join(OUT, 'form-mall.png') })

  const passed = results.filter(r => r.ok).length
  console.log(`\n===== 商品表单页 UI 验收 ${passed}/${results.length} =====`)
  const failed = results.filter(r => !r.ok)
  if (failed.length) failed.forEach(f => console.log(' - ' + f.name))
  await browser.close()
  process.exit(failed.length ? 1 : 0)
})().catch(e => { console.error('脚本异常:', e.message); process.exit(1) })
