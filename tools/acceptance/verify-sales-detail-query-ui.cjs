// 销售明细查询页 UI 验收（独立 Chromium，避免与 MCP 浏览器互斥）
// 覆盖：96 列收口 · 查询项 48 项 · 默认 2 排折叠 + 更多条件 · 页面配置真实生效 · 列配置齿轮入口 · 导出按钮 · 批次号/明细备注去桩
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BASE = 'http://localhost:5656'
const API = 'http://localhost:5655'
const PAGE_URL = `${BASE}/sales/detail-query`
const SHOTS = 'I:/AI-Ready/tool-results/sales-detail-query'
require('fs').mkdirSync(SHOTS, { recursive: true })

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function apiLogin() {
  const cap = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
  const cd = cap.data || cap
  const lg = await fetch(`${API}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    // 独立验收账号（与并行会话共用 admin 会因 sa-token 单端登录互踢）
    body: JSON.stringify({ username: 'dq_e2e', password: 'admin123', tenantName: '系统租户', captcha: extractCaptcha(cd.img), captchaKey: cd.uuid }),
  }).then(r => r.json())
  const d = lg?.data || {}
  return { token: d.token || d.accessToken || d.tokenValue, tenantId: d.tenantId }
}

async function main() {
  let { token, tenantId } = await apiLogin()
  if (!token) { console.log('登录失败（dq_e2e 账号不存在？）'); process.exit(1) }

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1760, height: 950 } })
  const errors = []
  const netlog = []
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', e => errors.push(String(e)))
  page.on('request', r => { if (r.url().includes('user-config')) netlog.push(r.url()) })

  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid]) => {
    localStorage.setItem('token', tk); localStorage.setItem('tenantId', String(tid || 1))
  }, [token, tenantId])

  async function open(url, waitSel = '.ss-grid') {
    for (let i = 1; i <= 5; i++) {
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(3500)
      const onLogin = await page.evaluate(() => !!document.querySelector('input[type="password"]')).catch(() => true)
      if (!onLogin) {
        await page.waitForSelector(waitSel, { timeout: 20000 }).catch(() => { })
        await page.waitForTimeout(2500)
        return true
      }
      console.log(`  ⚠️ 第 ${i} 次被踢下线，自动重登…`)
      const r = await apiLogin()
      await page.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk); localStorage.setItem('tenantId', String(tid || 1))
      }, [r.token, r.tenantId])
    }
    return false
  }

  const headTexts = () => page.$$eval('.ss-grid thead th', ths => ths.map(t => t.textContent.trim()))
  const visibleBtns = () => page.$$eval('button', bs => bs.filter(b => b.offsetParent !== null).map(b => b.textContent.trim()).filter(Boolean))

  // ═══ 1. 列表页基础 ═══
  console.log('\n[1] 列表页 · 查询区与 96 列')
  await open(PAGE_URL)
  const rows = await page.$$eval('.ss-grid tbody tr', trs => trs.length)
  check('明细行渲染', rows > 0, `${rows} 行`)

  let ths = await headTexts()
  const dataThs = ths.filter(Boolean).filter(t => t !== '操作')
  check('默认显示列 = 57（其余 96 列默认隐藏，可配置开启）', dataThs.length === 57, `实际 ${dataThs.length}`)
  for (const n of ['单据日期', '单据编号', '单据类型', '客户', '商品名称', '销售数量', '金额', '毛利率(%)']) {
    check(`默认显示列「${n}」存在`, ths.includes(n))
  }
  for (const n of ['默认经手人', '来源订单日期', '表体自定义6', '配送司机']) {
    check(`默认隐藏列「${n}」不在表头（符合默认隐藏配置）`, !ths.includes(n))
  }

  // 默认行值：单据类型/销售类型中文化 + 来源订单日期真实
  const firstRow = await page.$$eval('.ss-grid tbody tr', trs => {
    const tds = [...trs[0].querySelectorAll('td')].map(td => td.textContent.trim())
    return tds
  }).catch(() => [])
  check('首行有数据单元格', firstRow.length > 10, `${firstRow.length} 单元格`)

  // ═══ 2. 搜索区默认 2 排 + 查询操作行 ═══
  console.log('\n[2] 搜索区 · 默认 2 排 + 查询/重置/勾选')
  const btns1 = await visibleBtns()
  check('查询按钮默认可见', btns1.includes('查询'), btns1.filter(b => b.length < 8).join('/').slice(0, 80))
  check('重置按钮默认可见', btns1.includes('重置'))
  check('更多条件默认可见', btns1.includes('更多条件'))
  const cbs = await page.$$eval('.search-action-row .ant-checkbox-wrapper', els => els.map(e => e.textContent.trim()))
  check('操作行勾选项 = 显示红冲/仅统计车辆库/包含有来源订单', cbs.length === 3, cbs.join('/'))
  const actionRowBox = await page.$eval('.search-action-row', el => el.getBoundingClientRect().toJSON())
  const containerBox = await page.$eval('.search-container', el => el.getBoundingClientRect().toJSON())
  check('查询操作行未被折叠区裁掉', actionRowBox.top + actionRowBox.height <= containerBox.bottom + 2,
    `actionRow.bottom=${Math.round(actionRowBox.top + actionRowBox.height)} container.bottom=${Math.round(containerBox.bottom)}`)
  await page.screenshot({ path: `${SHOTS}/list-collapsed.png` })

  const collapsed = await page.evaluate(() => document.querySelector('.search-container')?.getAttribute('data-expanded'))
  await page.click('.search-more-toggle button')
  await page.waitForTimeout(600)
  const expanded = await page.evaluate(() => document.querySelector('.search-container')?.getAttribute('data-expanded'))
  check('「更多条件」可展开（折叠态→展开态）', collapsed === null && expanded !== null, `collapsed=${collapsed} expanded=${expanded}`)
  const gridClipped = await page.$eval('.search-grid', el => {
    const c = document.querySelector('.search-container')
    return c?.getAttribute('data-expanded') === null && el.scrollHeight > el.clientHeight + 2
  })
  check('展开后折叠裁剪解除', gridClipped === false)
  await page.screenshot({ path: `${SHOTS}/list-expanded.png` })
  await page.click('.search-more-toggle button')
  await page.waitForTimeout(400)

  // ═══ 3. 工具栏导出按钮 ═══
  console.log('\n[3] 工具栏')
  const btns2 = await visibleBtns()
  check('导出按钮存在（与功能按钮配置一致）', btns2.includes('导出'), btns2.filter(b => b.length < 8).join('/').slice(0, 80))
  check('刷新按钮存在', btns2.includes('刷新'))

  // ═══ 4. 列配置齿轮（表头 rowNo 内嵌） ═══
  console.log('\n[4] 列配置入口')
  const gear = await page.$('.ss-grid thead th.ss-header-settings .th-settings-btn, .ss-grid thead th.ss-header-settings svg, .ss-grid thead th.ss-header-settings')
  check('表头序号列存在齿轮入口', !!gear)
  if (gear) {
    await gear.click()
    await page.waitForTimeout(1200)
    const tabs = await page.$$eval('.col-config-tabs .ant-tabs-tab', ts => ts.map(t => t.textContent.trim())).catch(() => [])
    check('列配置弹窗打开（个人配置/全局配置）', tabs.includes('个人配置') && tabs.includes('全局配置'), tabs.join('/'))
    const colRows = await page.$$eval('.col-setting-row', els => els.length).catch(() => 0)
    check('列配置列出全部 96 数据列（+序号/操作）', colRows >= 96, `${colRows} 行`)
    const colText = await page.$$eval('.col-setting-row', els => els.map(e => e.textContent).join('|')).catch(() => '')
    for (const n of ['默认经手人', '来源订单日期', '表体自定义6', '表体自定义8(往来单位)', '表体自定义9(职员)', '表体自定义10(部门)', '配送司机', '毛利率(%)']) {
      check(`列配置含「${n}」`, colText.includes(n))
    }
    await page.screenshot({ path: `${SHOTS}/column-config.png` })
    // 全局配置 Tab 切换列显示 → 落独立存储键（不再与他人串味）
    await page.evaluate(() => {
      const tab = [...document.querySelectorAll('.col-config-tabs .ant-tabs-tab')].find(t => t.textContent.trim() === '全局配置')
      tab?.click()
    })
    await page.waitForTimeout(800)
    await page.evaluate(() => {
      const row = [...document.querySelectorAll('.col-setting-row')].find(r => r.textContent.includes('配送司机'))
      row?.querySelector('.ant-checkbox-wrapper')?.click()
    })
    await page.waitForTimeout(2500)
    const globalKey = await page.evaluate(() => Object.keys(localStorage).filter(k => k.includes('col') || k.includes('column')))
    check('全局列配置不与其它页面串味（无 col-config-global-* 共用键）',
      !globalKey.some(k => k.startsWith('col-config-global-')),
      globalKey.join(','))
    check('全局列配置写入后端（/system/user-config/col-config/sales-detail-query-columns）',
      netlog.some(u => u.includes('/system/user-config/col-config/sales-detail-query-columns')),
      netlog.filter(u => u.includes('user-config')).join(' | '))
    await page.evaluate(() => document.querySelectorAll('.ant-modal-close').forEach(b => b.click()))
    await page.waitForTimeout(900)
  }

  // ═══ 5. 页面配置：3 Tab + 48 查询项 + 真实生效 ═══
  console.log('\n[5] 页面配置')
  await page.click('button:has(svg[data-icon="setting"])')
  await page.waitForTimeout(1200)
  const pTabs = await page.$$eval('.ant-modal .ant-tabs-tab', ts => ts.map(t => t.textContent.trim())).catch(() => [])
  check('页面配置 3 个 Tab', pTabs.includes('查询条件') && pTabs.includes('功能按钮') && pTabs.includes('打印配置'), pTabs.join('/'))
  const qItems = await page.$$eval('.ant-modal .ant-tabs-tabpane-active tbody tr', trs => trs.length).catch(() => 0)
  check('查询条件条目 = 48', qItems === 48, `实际 ${qItems}`)
  const qText = await page.$$eval('.ant-modal .ant-tabs-tabpane-active', els => els.map(e => e.textContent).join('')).catch(() => '')
  for (const n of ['表体自定义6(数字)', '表体自定义7(数字)', '表体自定义8(往来单位)', '表体自定义9(职员)', '表体自定义10(部门)', '默认经手人', '来源']) {
    check(`查询条件配置含「${n}」`, qText.includes(n))
  }
  await page.screenshot({ path: `${SHOTS}/page-config-query.png` })

  // 勾选「表体自定义6(数字)」→ 搜索区新增该查询项
  const gridBefore = await page.$$eval('.search-grid .search-field-item', els => els.length)
  await page.evaluate(() => {
    const rows = [...document.querySelectorAll('.ant-modal .ant-tabs-tabpane-active tbody tr')]
    const target = rows.find(r => r.textContent.includes('表体自定义6(数字)'))
    target?.querySelector('.ant-checkbox-wrapper')?.click()
  })
  await page.waitForTimeout(600)
  // 切到功能按钮 Tab 取消「导出」
  await page.evaluate(() => {
    const tab = [...document.querySelectorAll('.ant-modal .ant-tabs-tab')].find(t => t.textContent.trim() === '功能按钮')
    tab?.click()
  })
  await page.waitForTimeout(600)
  await page.screenshot({ path: `${SHOTS}/page-config-buttons.png` })
  await page.evaluate(() => {
    const rows = [...document.querySelectorAll('.ant-modal .ant-tabs-tabpane-active tbody tr')]
    const target = rows.find(r => r.textContent.includes('导出'))
    const cb = target?.querySelector('input[type="checkbox"]')
    if (cb && cb.checked) cb.click()
  })
  await page.waitForTimeout(600)
  await page.evaluate(() => document.querySelectorAll('.ant-modal-close').forEach(b => b.click()))
  await page.waitForTimeout(1200)

  const btns3 = await visibleBtns()
  check('取消勾选「导出」后工具栏导出按钮消失（配置真实生效）', !btns3.includes('导出'))

  // 展开更多条件，确认新查询项渲染
  await page.click('.search-more-toggle button')
  await page.waitForTimeout(800)
  const gridAfter = await page.$$eval('.search-grid .search-field-item', els => els.length)
  check('勾选「表体自定义6(数字)」后搜索区字段 +1（页面配置真实生效）', gridAfter === gridBefore + 1,
    `${gridBefore} → ${gridAfter}`)
  const rangeTitles = await page.$$eval('.search-grid .range-field', els => els.map(e => e.getAttribute('title'))).catch(() => [])
  check('数值区间查询项带字段名提示（表体自定义6）', rangeTitles.includes('表体自定义6(数字)'), rangeTitles.join('/'))
  await page.screenshot({ path: `${SHOTS}/search-ext-fields.png` })
  await page.click('.search-more-toggle button')
  await page.waitForTimeout(400)

  // ═══ 6. 行级操作去桩：批次号 / 明细备注 ═══
  console.log('\n[6] 行级操作（去桩）')
  const warnCountBefore = errors.length
  await page.click('.ss-grid tbody tr:first-child button:has-text("批次号")')
  await page.waitForTimeout(1500)
  const batchTitle = await page.$$eval('.ant-modal-title', ts => ts.map(t => t.textContent.trim())).catch(() => [])
  check('批次号弹窗打开（真实弹层，非 console.warn）', batchTitle.includes('批次追溯'), batchTitle.join('/'))
  const batchBody = await page.$$eval('.ant-modal-body', els => els.map(e => e.textContent).join('|')).catch(() => '')
  check('批次弹窗含明细行批次信息', batchBody.includes('批次条码') && batchBody.includes('销售数量'))
  await page.screenshot({ path: `${SHOTS}/batch-modal.png` })
  await page.evaluate(() => document.querySelectorAll('.ant-modal-close').forEach(b => b.click()))
  await page.waitForTimeout(900)

  await page.click('.ss-grid tbody tr:first-child button:has-text("明细备注")')
  await page.waitForTimeout(1200)
  const remarkTitles = await page.$$eval('.ant-modal-title', ts => ts.map(t => t.textContent.trim())).catch(() => [])
  check('明细备注弹窗打开（真实弹层）', remarkTitles.includes('明细备注'), remarkTitles.join('/'))
  const remarkBody = await page.$$eval('.ant-modal-body', els => els.map(e => e.textContent).join('|')).catch(() => '')
  check('备注弹窗含 明细/单据/买家/客户 备注', ['明细备注', '单据备注', '买家备注', '客户备注'].every(k => remarkBody.includes(k)))
  await page.screenshot({ path: `${SHOTS}/remark-modal.png` })
  await page.evaluate(() => document.querySelectorAll('.ant-modal-close').forEach(b => b.click()))
  await page.waitForTimeout(900)

  // ═══ 7. 配置持久化（刷新后仍生效） ═══
  console.log('\n[7] 配置持久化')
  await open(PAGE_URL)
  const btns4 = await visibleBtns()
  check('刷新后「导出」仍按配置隐藏', !btns4.includes('导出'))
  await page.click('.search-more-toggle button')
  await page.waitForTimeout(800)
  const gridText2 = await page.$$eval('.search-grid', els => els.map(e => e.textContent).join('')).catch(() => '')
  check('刷新后查询项显隐仍生效（已勾选的表体自定义6仍在）', gridText2.includes('表体自定义6'))
  check('刷新后已隐藏的默认查询项仍未出现（表体自定义10）', !gridText2.includes('表体自定义10'))

  // ═══ 8. 查询方案（保存 + 复用） ═══
  console.log('\n[8] 查询方案')
  const SCHEME = '金标准方案-A'
  await page.evaluate((name) => {
    localStorage.setItem('sales-detail-query-scheme-' + name, JSON.stringify({ productName: '测试' }))
  }, SCHEME)
  await open(PAGE_URL)
  await page.click('.query-scheme-wrap .ant-select')
  await page.waitForTimeout(900)
  const schemeOptions = await page.$$eval('.ant-select-item-option-content', els => els.map(e => e.textContent.trim())).catch(() => [])
  check('已保存查询方案出现在下拉中（保存后可复用）', schemeOptions.includes(SCHEME), schemeOptions.join('/') || '空')
  await page.keyboard.press('Escape')
  await page.waitForTimeout(400)
  await page.click('.query-scheme-wrap button')
  await page.waitForTimeout(1500)
  const saveModal = await page.$$eval('.ant-modal-confirm-title, .ant-modal-title', ts => ts.map(t => t.textContent.trim())).catch(() => [])
  check('「+」打开保存查询方案弹窗（非死按钮）', saveModal.includes('保存查询方案'), saveModal.join('/') || '未打开')
  await page.evaluate(() => document.querySelectorAll('.ant-modal-close').forEach(b => b.click()))
  await page.waitForTimeout(600)

  check('无页面 JS 错误', errors.filter(e => !/favicon|ResizeObserver|404|net::ERR/i.test(e)).length === 0,
    errors.filter(e => !/favicon|ResizeObserver|404|net::ERR/i.test(e)).slice(0, 2).join(' || ') || '无')

  await browser.close()
  console.log(`\n结果: ${pass}/${pass + fail} 通过` + (fail ? `（失败 ${fail}）` : ''))
  if (fail) process.exit(1)
}

main().catch(e => { console.error('异常:', e); process.exit(1) })
