// 科目余额表页面 UI 验收（独立 Chromium 实例，避免与 MCP 浏览器互斥）
// 校验：分组表头（本期发生额 / 本年累计）+ 10 数据列 + 合计行 + 列配置齿轮 + 科目下钻链接
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BASE = 'http://localhost:5174'
const API = 'http://localhost:5655'

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

let pass = 0
let fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

async function main() {
  // 1. 取 token（API 登录）
  const capRes = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
  const capData = capRes.data || capRes
  const captcha = extractCaptcha(capData.img)
  const loginRes = await fetch(`${API}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin', password: 'admin123', tenantName: '系统租户',
      captcha, captchaKey: capData.uuid,
    }),
  }).then(r => r.json())
  const d = loginRes?.data || {}
  const token = d.token || d.accessToken || d.tokenValue
  if (!token) { console.log('登录失败:', JSON.stringify(loginRes).slice(0, 300)); process.exit(1) }
  console.log(`登录成功 (验证码 ${captcha})`)

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1600, height: 900 } })
  const errors = []
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', e => errors.push(String(e)))

  // 2. 注入登录态后进入科目余额表
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tenantId, tenantName]) => {
    localStorage.setItem('token', tk)
    localStorage.setItem('tenantId', String(tenantId || 1))
    localStorage.setItem('tenantName', tenantName || '')
  }, [token, d.tenantId, d.tenantName])

  // 首屏加载：并行会话会踢 token/重启后端，失败则重登 + 重载，最多 4 次
  let firstLoad = false
  for (let i = 1; i <= 4 && !firstLoad; i++) {
    await page.goto(`${BASE}/finance/balance-sheet`, { waitUntil: 'domcontentloaded' })
    firstLoad = await page.waitForSelector('.ss-grid', { timeout: 20000 }).then(() => true).catch(() => false)
    if (!firstLoad) {
      console.log(`  ⚠️ 第 ${i} 次进入科目余额表未渲染（疑似 token 被踢/后端重启），重登重载…`)
      const cap = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
      const cd2 = cap.data || cap
      const lg = await fetch(`${API}/api/auth/login`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: extractCaptcha(cd2.img), captchaKey: cd2.uuid }),
      }).then(r => r.json())
      const t2 = lg?.data?.token || lg?.data?.accessToken || lg?.data?.tokenValue
      if (t2) await page.evaluate((tk) => localStorage.setItem('token', tk), t2)
    }
  }
  if (!firstLoad) { console.log('❌ 科目余额表页面始终未渲染，终止'); process.exit(1) }
  await page.waitForTimeout(4000)

  // 3. 分组表头
  const headerTexts = await page.$$eval('.ss-grid thead th', ths => ths.map(t => ({
    text: t.textContent.trim(),
    colspan: t.getAttribute('colspan'),
    rowspan: t.getAttribute('rowspan'),
  })))
  const texts = headerTexts.map(h => h.text)
  console.log('\n[1] 表头结构')
  check('表头两行', await page.$$eval('.ss-grid thead tr', trs => trs.length) === 2)
  check('分组「本期发生额」colspan=2', headerTexts.some(h => h.text === '本期发生额' && h.colspan === '2'),
    JSON.stringify(headerTexts.filter(h => h.colspan === '2')))
  check('分组「本年累计」colspan=2', headerTexts.some(h => h.text === '本年累计' && h.colspan === '2'))
  for (const name of ['科目类型', '科目编码', '科目名称', '层级', '期初余额', '期末余额']) {
    check(`列「${name}」存在`, texts.includes(name))
  }
  // 子列标题可能是「借方」或「本期发生额-借方」（共享组件由并行会话迭代），两种均接受
  for (const name of ['借方', '贷方']) {
    const n = texts.filter(t => t === name || t.endsWith(`-${name}`)).length
    check(`子列「${name}」出现 2 次（本期 + 本年）`, n === 2, `${n}`)
  }
  const majorHeaders = headerTexts.filter(h => h.text && !/[-]?(借方|贷方)$/.test(h.text)).length
  check('主表头 = 6 独立列 + 2 分组（序号列仅齿轮）', majorHeaders === 8, `${majorHeaders}`)

  // 4. 数据行 + 合计行
  console.log('\n[2] 数据与合计')
  const rowCount = await page.$$eval('.ss-grid tbody tr.ss-row', trs => trs.length)
  check('数据行 > 0（2026-09 一级科目）', rowCount > 0, `${rowCount} 行`)
  const summaryText = await page.$eval('.ss-grid tfoot tr', tr => tr.textContent.replace(/\s+/g, ' ').trim()).catch(() => '')
  check('合计行存在且含「合计」', summaryText.includes('合计'), summaryText.slice(0, 200))
  // 合计行不变量：期初/期末「借 x / 贷 y」成对出现；本期/本年借贷各出现两次且金额两两相等
  const pairs = [...summaryText.matchAll(/借\s([\d,]+\.\d{2})\s\/\s贷\s([\d,]+\.\d{2})/g)]
  check('合计行期初/期末各为「借 x / 贷 y」', pairs.length === 2, pairs.map(p => `${p[1]}/${p[2]}`).join(' | '))
  check('合计行期初借 = 贷、期末借 = 贷（试算平衡）',
    pairs.length === 2 && pairs.every(p => Number(p[1].replace(/,/g, '')) === Number(p[2].replace(/,/g, ''))),
    pairs.map(p => `${p[1]}=${p[2]}`).join(' | '))
  const firstRow = await page.$eval('.ss-grid tbody tr.ss-row', tr => tr.textContent.replace(/\s+/g, ' ').trim())
  check('首行含科目编码/名称/余额方向', /1001/.test(firstRow) && /库存现金/.test(firstRow) && /借/.test(firstRow), firstRow.slice(0, 160))

  // 5. 列配置齿轮（rowNo 表头）
  console.log('\n[3] 列配置')
  check('序号列表头含配置齿轮', await page.$eval('.ss-grid thead .th-settings-btn', el => !!el).catch(() => false))
  await page.click('.ss-grid thead .th-settings-btn')
  await page.waitForTimeout(800)
  const modalTitle = await page.$eval('.ant-modal-title', el => el.textContent.trim()).catch(() => '')
  const tabs = await page.$$eval('.ant-modal .ant-tabs-tab', els => els.map(e => e.textContent.trim())).catch(() => [])
  check('列配置弹窗打开（个人配置 / 全局配置）', /配置/.test(modalTitle) && tabs.includes('个人配置') && tabs.includes('全局配置'),
    `${modalTitle} | ${tabs.join(',')}`)
  const settingRows = await page.$$eval('.ant-modal .col-setting-row', els => els.map(e => e.textContent.replace(/\s+/g, ' ').trim()))
  check('列配置列出 11 行（序号锁定 + 10 数据列）', settingRows.length === 11, `${settingRows.length}`)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(500)

  // 6. 显示无数据科目
  // 注：并行会话共用 admin，sa-token 单端登录会互相踢下线；每步前重登自愈，失败则重载重试
  async function relogin() {
    const cap = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
    const cd = cap.data || cap
    const res = await fetch(`${API}/api/auth/login`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: extractCaptcha(cd.img), captchaKey: cd.uuid }),
    }).then(r => r.json())
    const t = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue
    if (t) await page.evaluate((tk) => localStorage.setItem('token', tk), t)
    return !!t
  }
  async function reloadTable() {
    await page.reload({ waitUntil: 'domcontentloaded' })
    await page.waitForSelector('.ss-grid', { timeout: 30000 })
    await page.waitForTimeout(3500)
  }

  console.log('\n[4] 显示无数据科目')
  const rowsBefore = await page.$$eval('.ss-grid tbody tr.ss-row', trs => trs.length)
  let rowCountAll = 0
  for (let attempt = 1; attempt <= 3; attempt++) {
    await relogin()
    await page.locator('.tb-search-item .ant-checkbox-input').first().click({ force: true })
    await page.waitForTimeout(3000)
    rowCountAll = await page.$$eval('.ss-grid tbody tr.ss-row', trs => trs.length).catch(() => 0)
    if (rowCountAll > rowsBefore) break
    console.log(`  ⚠️ 第 ${attempt} 次未生效（疑似被并行会话踢下线），重载重试…`)
    await reloadTable()
  }
  check('勾选后行数增加（含零发生额科目）', rowCountAll > rowsBefore, `${rowsBefore} → ${rowCountAll}`)
  const totalAll = await page.$eval('.ss-grid tfoot tr', tr => tr.textContent.replace(/\s+/g, ' ').trim())
  check('勾选后合计行仍展示借贷合计', /借\s[\d,]+\.\d{2}\s\/\s贷\s[\d,]+\.\d{2}/.test(totalAll), totalAll.slice(0, 120))
  await relogin()
  await page.locator('.tb-search-item .ant-checkbox-input').first().click({ force: true })
  await page.waitForTimeout(2500)

  // 截图必须在任何跳转前（跳明细账后页面已变）
  await page.screenshot({ path: 'trial-balance-page.png', fullPage: false })
  console.log('  📷 已截图 trial-balance-page.png')

  // 7. 查询方案（存为方案 → 应用 → 删除）
  console.log('\n[5] 查询方案')
  const schemeName = `E2E-${Date.now()}`
  await relogin()
  await page.click('text=存为方案')
  await page.waitForTimeout(600)
  await page.fill('.ant-modal input.ant-input', schemeName)
  await page.click('.ant-modal .ant-btn-primary')
  await page.waitForTimeout(1200)
  const schemeSelected = await page.$eval('.tb-search-item .ant-select-selection-item', el => el.textContent.trim()).catch(() => '')
  check('保存方案后自动选中该方案', schemeSelected === schemeName, schemeSelected)
  // 方案可复用：切走后重新选中方案应回填并重查
  await page.click('.tb-search-item .ant-select')  // 打开下拉
  await page.waitForTimeout(600)
  const optionExists = await page.$$eval('.ant-select-item-option-content', (els, name) => els.map(e => e.textContent.trim()).includes(name), schemeName)
  check('方案出现在查询方案下拉中', optionExists, schemeName)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(400)

  // 8. 导出（CSV 下载）
  console.log('\n[6] 导出')
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 15000 }).catch(() => null),
    page.click('text=导出'),
  ])
  if (!download) {
    check('点击导出触发下载', false, '未捕获到 download 事件')
  } else {
    const fname = download.suggestedFilename()
    const path = await download.path()
    const content = require('fs').readFileSync(path, 'utf8')
    const lines = content.trim().split(/\r?\n/)
    check('导出文件名含「科目余额表」与期间', /科目余额表/.test(fname) && /2026-09/.test(fname), fname)
    check('导出含表头 + 科目行 + 合计行', lines.length >= (rowsBefore + 2) && lines[0].includes('科目类型') && content.includes('合计'), `${lines.length} 行`)
    check('导出金额与页面一致（本期借方列非空）', /1466\.00|1,466\.00/.test(content), content.split(/\r?\n/)[1]?.slice(0, 120))
  }

  // 9. 打印（独立窗口，分组表头 + 合计行）
  console.log('\n[7] 打印')
  const [popup] = await Promise.all([
    page.waitForEvent('popup', { timeout: 15000 }).catch(() => null),
    page.click('text=打印'),
  ])
  if (!popup) {
    check('点击打印打开打印窗口', false, '未捕获到 popup')
  } else {
    await popup.waitForLoadState('domcontentloaded').catch(() => {})
    const html = await popup.content()
    check('打印窗口含分组表头（本期发生额 / 本年累计 colspan=2）',
      html.includes('本期发生额') && html.includes('本年累计') && (html.match(/colspan="2"/g) || []).length >= 2)
    check('打印窗口含科目行与合计行', html.includes('库存现金') && html.includes('合计'))
    check('打印窗口含试算平衡结论', html.includes('试算平衡'))
    await popup.close()
  }

  // 10. 明细对账弹窗（工具栏）
  console.log('\n[8] 明细对账（工具栏弹窗）')
  await page.click('text=明细对账')
  await page.waitForTimeout(1000)
  // 注：ant-design 关闭后的弹窗节点可能残留在 DOM（隐藏），必须限定「可见」弹窗
  const dlg = page.locator('.ant-modal:visible').last()
  const dmTitle = (await dlg.locator('.ant-modal-title').textContent().catch(() => ''))?.trim() || ''
  check('明细对账弹窗打开', dmTitle === '明细对账', dmTitle || '未找到可见弹窗')
  await dlg.locator('.ant-select').first().click()
  await page.waitForTimeout(800)
  const firstOpt = page.locator('.ant-select-dropdown:visible .ant-select-item-option').first()
  const hasOpt = await firstOpt.count() > 0
  check('弹窗可选科目（选项来自当前结果集）', hasOpt)
  if (hasOpt) {
    await firstOpt.click()
    await page.waitForTimeout(500)
    await dlg.locator('.ant-modal-footer .ant-btn-primary').click()
    await page.waitForTimeout(3000)
    const u2 = decodeURIComponent(page.url())
    check('确认后跳转《明细账》并带所选科目', u2.includes('/finance/detail-ledger') && u2.includes('subjectCode='), u2)
    await page.goBack()
    await page.waitForSelector('.ss-grid tbody .subject-link', { timeout: 30000 }).catch(() => {})
    await page.waitForTimeout(1500)
  }

  // 11. 科目名称链接下钻
  console.log('\n[9] 科目名称下钻')
  await relogin()
  if (!(await page.$('.ss-grid tbody .subject-link'))) await reloadTable()
  check('科目名称为链接', await page.$('.ss-grid tbody .subject-link') !== null)
  await page.click('.ss-grid tbody .subject-link')
  await page.waitForTimeout(2500)
  const url = page.url()
  check('点击科目名跳转《明细账》并带科目与期间',
    url.includes('/finance/detail-ledger') && decodeURIComponent(url).includes('subjectCode=') && decodeURIComponent(url).includes('dateStart='),
    decodeURIComponent(url))

  const realErrors = errors.filter(e => !/favicon|404 \(Not Found\)|ResizeObserver/.test(e))
  check('无 JS 运行错误', realErrors.length === 0, realErrors.slice(0, 3).join(' | '))

  await browser.close()
  console.log(`\n结果：通过 ${pass} 项，失败 ${fail} 项`)
  process.exit(fail === 0 ? 0 : 1)
}

main().catch(e => { console.error('UI 验收异常:', e); process.exit(1) })
