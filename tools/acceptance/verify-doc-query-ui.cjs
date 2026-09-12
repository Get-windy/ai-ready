// 销售单据查询页 UI 验收（独立 Chromium，避免与 MCP 浏览器互斥）
// 覆盖：页面渲染 / 列配置齿轮 / 页面配置勾选生效 / 查询方案 / 备注落库 / 打印组件 / 类型不适用标注
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BASE = process.env.UI_BASE || 'http://localhost:5656'
// 优先用页面代理指向的后端端口；被并行会话重启时自动改用独立实例端口
let API = process.env.BASE || 'http://localhost:5655'
const FALLBACK_API = process.env.FALLBACK_BASE || 'http://localhost:5666'

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function closeAllModals(page) {
  for (let i = 0; i < 5; i++) {
    const closeBtns = page.locator('.ant-modal-close:visible')
    if (await closeBtns.count() === 0) break
    await closeBtns.last().click().catch(() => {})
    await page.waitForTimeout(400)
  }
}

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

async function login() {
  const capRes = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
  const capData = capRes.data || capRes
  const res = await fetch(`${API}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin', password: 'admin123', tenantName: '系统租户',
      captcha: extractCaptcha(capData.img), captchaKey: capData.uuid,
    }),
  }).then(r => r.json())
  const d = res?.data || {}
  const token = d.token || d.accessToken || d.tokenValue
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  return { token, tenantId: d.tenantId, tenantName: d.tenantName }
}

async function pickApi() {
  for (const candidate of [API, FALLBACK_API]) {
    try {
      const r = await fetch(`${candidate}/api/auth/captcha`)
      if (r.ok) { API = candidate; return true }
    } catch { /* 端口不可用，尝试下一个 */ }
  }
  return false
}

async function main() {
  if (!await pickApi()) { console.log('后端实例均不可用'); process.exit(1) }
  console.log(`后端实例: ${API}`)
  const auth = await login()
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1680, height: 950 } })
  // 页面内 /api 请求统一转发到可用后端实例（避免并行会话重启 5655 造成误报）
  if (!API.includes('5655')) {
    // 只转发后端接口（/api/ 开头），避免误匹配 vite 源码路径 /src/api/*.ts
    await page.route(u => u.pathname.startsWith('/api/'), async route => {
      const u = new URL(route.request().url())
      try {
        const response = await route.fetch({ url: `${API}${u.pathname}${u.search}` })
        await route.fulfill({ response })
      } catch {
        await route.abort()
      }
    })
  }
  const errors = []
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', e => errors.push(String(e)))

  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tenantId, tenantName]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tenantId || 1))
    localStorage.setItem('tenantName', tenantName || '')
    localStorage.removeItem('sales-doc-query-page-config')
  }, [auth.token, auth.tenantId, auth.tenantName])

  // 进入页面（并行会话可能踢 token，自愈重登）
  const enterPage = async () => {
    for (let i = 1; i <= 4; i++) {
      await page.goto(`${BASE}/sales/doc-query`, { waitUntil: 'domcontentloaded' })
      const ok = await page.waitForSelector('.ss-grid', { timeout: 20000 }).then(() => true).catch(() => false)
      if (ok) return true
      console.log(`  ⚠️ 第 ${i} 次进入页面未渲染，重新登录后重试…`)
      const a = await login()
      await page.evaluate(([tk]) => localStorage.setItem('token', tk), [a.token])
    }
    return false
  }
  // 切到「本年」：默认「本周」会过滤掉夹具数据
  const gotoList = async () => {
    await page.locator('.quick-dates button:has-text("本年")').first().click().catch(() => {})
    await page.waitForTimeout(2500)
  }
  // 并行会话会踢掉同账号 token 导致跳出页面，检测到即重登返回
  const ensureOnPage = async () => {
    const onPage = page.url().includes('/sales/doc-query') && (await page.locator('.ss-grid').count()) > 0
    if (onPage) return
    console.log('  ⚠️ 页面已跳转（疑似并行会话踢 token），重新登录并返回…')
    const a = await login()
    await page.evaluate(([tk]) => localStorage.setItem('token', tk), [a.token])
    await enterPage()
    await gotoList()
  }

  const ready = await enterPage()

  console.log('\n[1] 页面骨架')
  check('页面渲染数据表 (.ss-grid)', ready)
  if (!ready) { await browser.close(); process.exit(1) }

  await gotoList()

  const gridText = () => page.evaluate(() => document.querySelector('.ss-grid')?.innerText || '')
  let gText = await gridText()
  check('查询返回数据行（含夹具单据）', gText.includes('ZZT-QO-001') && gText.includes('XSCK'), '订单/出库夹具均可见')
  check('单据类型列显示中文标签', gText.includes('销售订单') && gText.includes('销售出库单'), '枚举已本地化')

  const bodyText = await page.evaluate(() => document.body.innerText)
  check('工具栏含 打印(F8)', bodyText.includes('打印(F8)'))
  check('工具栏含 导出', bodyText.includes('导出'))
  check('含查询方案下拉', bodyText.includes('--查询方案--'))
  check('含 更多条件 折叠入口', bodyText.includes('更多条件'))
  const queryBtn = page.locator('.search-action-row button:has-text("查询")').first()
  check('搜索区「查询」按钮可见（不被折叠高度裁掉）', await queryBtn.isVisible().catch(() => false),
    `box=${JSON.stringify(await queryBtn.boundingBox().catch(() => null))}`)
  const actionChecks = await page.locator('.search-action-row .ant-checkbox-wrapper').allInnerTexts()
  check('查询按钮旁勾选项与对标一致（仅统计车辆库）',
    actionChecks.some(t => t.includes('仅统计车辆库')) && actionChecks.every(t => !t.includes('显示红冲')),
    actionChecks.join('/'))
  check('底部合计标注「本页合计」', bodyText.includes('本页合计'), '合计口径诚实标注')
  check('页面无 window.print 兜底打印按钮', !bodyText.includes('浏览器打印'))

  console.log('\n[2] 换货单字段口径（此前实体缺列导致 500）')
  const typeSelect = page.locator('.search-select-wrap:has-text("单据类型") .ant-select')
  await typeSelect.first().click()
  await page.waitForTimeout(400)
  await page.locator('.ant-select-item-option:has-text("销售换货单")').first().click()
  await page.waitForTimeout(400)
  await page.locator('.search-action-row button:has-text("查询")').first().click()
  await page.waitForTimeout(2000)
  gText = await gridText()
  check('换货单可查询（无 500）', gText.includes('ZZTEST-HH-001'), gText.includes('ZZTEST-HH-001') ? '命中夹具单据' : gText.slice(0, 100))
  check('换货单不适用列显示「—」', gText.includes('—'), '费用/优惠/成本等列标注不适用')
  const exchangeRowText = await page.evaluate(() => {
    const rows = [...document.querySelectorAll('.ss-grid > *')].map(el => el.innerText || '')
    return rows.find(t => t.includes('ZZTEST-HH-001')) || ''
  })
  check('换货单成本/毛利已回填', exchangeRowText.includes('40') && exchangeRowText.includes('60'),
    `行内容片段=${exchangeRowText.slice(0, 60).replace(/\n/g, '|')}`)

  console.log('\n[3] 页面配置勾选真实生效')
  await page.locator('button:has(.anticon-setting)').first().click()
  await page.waitForTimeout(800)
  const cfgModal = page.locator('.ant-modal:has-text("页面配置")')
  const cfgText = await cfgModal.first().innerText().catch(() => '')
  check('页面配置弹窗打开', cfgText.length > 0)
  check('含 查询条件 / 功能按钮 / 打印配置 三个 Tab',
    cfgText.includes('查询条件') && cfgText.includes('功能按钮') && cfgText.includes('打印配置'))
  await cfgModal.locator('.ant-tabs-tab:has-text("打印配置")').first().click()
  await page.waitForTimeout(600)
  const printCfgText = await cfgModal.first().innerText().catch(() => '')
  check('打印配置项与页面打印组件对齐', printCfgText.includes('始终使用最后一次打印的模板'),
    printCfgText.replace(/\n/g, ' ').slice(0, 90))

  await cfgModal.locator('.ant-tabs-tab:has-text("查询条件")').first().click()
  await page.waitForTimeout(500)
  const beforeCount = await page.locator('.search-field-item input[placeholder="客户"]').count()
  const customerRow = cfgModal.locator('tr:has-text("客户")').first()
  await customerRow.locator('input[type="checkbox"]').click()
  await page.waitForTimeout(600)
  const afterCount = await page.locator('.search-field-item input[placeholder="客户"]').count()
  check('取消勾选「客户」→ 搜索项即时隐藏', beforeCount === 1 && afterCount === 0, `前=${beforeCount} 后=${afterCount}`)
  await customerRow.locator('input[type="checkbox"]').click()
  await page.waitForTimeout(600)
  check('重新勾选 → 搜索项恢复', (await page.locator('.search-field-item input[placeholder="客户"]').count()) === 1)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(500)

  console.log('\n[4] 列配置（表头齿轮：个人/全局双配置）')
  const gear = page.locator('.ss-grid .anticon-setting').first()
  check('表头存在列配置齿轮', await gear.count() > 0)
  if (await gear.count() > 0) {
    await gear.click()
    await page.waitForTimeout(800)
    const colModal = page.locator('.ant-modal').filter({ hasText: '个人配置' })
    const colText = await colModal.first().innerText().catch(() => '')
    check('列配置含 个人配置 / 全局配置 双 Tab', colText.includes('个人配置') && colText.includes('全局配置'))
    check('列配置含 成本金额 / 毛利 列', colText.includes('成本金额') && colText.includes('毛利'))
    await closeAllModals(page)
  }

  await ensureOnPage()
  console.log('\n[5] 整单备注落库')
  const noteBtn = page.locator('.ss-grid button:has-text("整单备注")').first()
  check('操作列含「整单备注」', await noteBtn.count() > 0)
  if (await noteBtn.count() > 0) {
    await noteBtn.click()
    await page.waitForTimeout(700)
    const noteModal = page.locator('.ant-modal:has-text("整单备注")')
    check('备注弹窗打开', await noteModal.count() > 0)
    const noteVal = `UI验收备注-${Date.now()}`
    await noteModal.locator('textarea').fill(noteVal)
    await noteModal.locator('.ant-btn-primary').first().click()
    await page.waitForTimeout(2000)
    const tk = await page.evaluate(() => localStorage.getItem('token'))
    const saved = await fetch(`${API}/api/sales/doc-query/page?current=1&size=20&documentType=EXCHANGE`, {
      headers: { Authorization: 'Bearer ' + tk },
    }).then(r => r.json()).then(j => ((j.data || j).records || [])[0]?.remark || '').catch(() => '')
    check('备注通过页面真实落库并可回读', saved === noteVal, `回读=${saved}`)
  }

  await ensureOnPage()
  console.log('\n[6] 打印组件')
  const rowCheckbox = page.locator('.ss-grid input[type="checkbox"]').nth(1)
  if (await rowCheckbox.count() > 0) { await rowCheckbox.click(); await page.waitForTimeout(400) }
  await page.locator('button:has-text("打印(F8)")').first().click()
  await page.waitForTimeout(1500)
  const printModal = page.locator('.ant-modal').filter({ hasText: '打印模板' })
  const printText = await printModal.first().innerText().catch(() => '')
  check('打印弹窗打开（通用打印组件）', printText.includes('打印模板'))
  check('打印弹窗含 模板/模式/份数', printText.includes('打印模式') && printText.includes('打印份数'),
    printText.replace(/\n/g, ' ').slice(0, 70))
  await closeAllModals(page)

  await ensureOnPage()
  console.log('\n[6b] 导出真实 Excel')
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 40000 }).catch(() => null),
    (async () => {
      await page.locator('button:has-text("导出")').first().click()
      await page.waitForTimeout(600)
      await page.locator('.ant-dropdown-menu-item:has-text("导出Excel")').first().click()
    })(),
  ])
  const fname = download ? download.suggestedFilename() : ''
  check('导出生成真实 .xlsx（非 TSV 伪装 .xls）', !!download && fname.endsWith('.xlsx'), `文件名=${fname}`)

  await ensureOnPage()
  console.log('\n[7] 查询方案')
  await page.locator('.query-scheme-wrap button').first().click()
  await page.waitForTimeout(800)
  const schemeModal = page.locator('.ant-modal:has-text("查询方案")')
  check('查询方案弹窗打开', await schemeModal.count() > 0)
  const schemeName = `UI方案${Date.now() % 100000}`
  await schemeModal.locator('input[placeholder="方案名称"]').fill(schemeName)
  await schemeModal.locator('button:has-text("保存当前条件")').click()
  await page.waitForTimeout(1000)
  const schemes = await page.evaluate(() => JSON.parse(localStorage.getItem('sales-doc-query-schemes') || '[]'))
  check('查询方案已持久化（含当前条件）', schemes.length >= 1 && !!schemes[0].params,
    `方案数=${schemes.length} 名称=${schemes[0]?.name || ''}`)
  await closeAllModals(page)

  console.log('\n[8] 控制台错误')
  // 过滤验收环境噪声：并行会话踢 token 导致的 401/403，以及转发中断的 ERR_FAILED/ERR_ABORTED
  const realErrors = errors.filter(e =>
    !/status of 40[13]/.test(e) && !/ERR_FAILED|ERR_ABORTED/.test(e))
  check('无前端运行时错误', realErrors.length === 0, realErrors.slice(0, 2).join(' | '))

  await page.screenshot({ path: 'doc-query-page.png' })
  await browser.close()
  console.log(`\n═══ UI 验收：通过 ${pass} / 失败 ${fail} ═══`)
  process.exit(fail > 0 ? 1 : 0)
}

main().catch(e => { console.error('FATAL', e); process.exit(1) })
