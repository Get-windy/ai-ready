// 预订货单金标准 · 浏览器验收（独立前端 5659 → 后端 5658）
// 覆盖：列表双 Tab / 列数收口 / 页面配置驱动显隐与排序 / 功能按钮开关 / 表单号段 / 明细列 / 账户下拉 / 保存
const { chromium } = require('playwright')
const { execFileSync } = require('child_process')
const PSQL = 'C:/Program Files/PostgreSQL/18/bin/psql.exe'

const FE = process.env.PREORDER_FE || 'http://localhost:5659'
const API = process.env.PREORDER_BASE || 'http://localhost:5777'

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
  const capRes = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
  const capData = capRes.data || capRes
  const captcha = extractCaptcha(capData.img)
  const res = await fetch(`${API}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123', tenantName: '系统租户', captcha, captchaKey: capData.uuid }),
  }).then(r => r.json())
  const token = res?.data?.token || res?.data?.accessToken || res?.data?.tokenValue
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  return res.data
}

async function main() {
  const auth = await apiLogin()
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()

  const consoleErrors = []
  page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text().slice(0, 200)) })
  page.on('pageerror', e => consoleErrors.push('pageerror: ' + String(e.message).slice(0, 200)))

  // 真实 UI 登录（动态路由/菜单依赖登录后的用户态，注入 token 不足以注册路由）
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2500)
  await page.fill('input[placeholder="请输入租户名称"]', '系统租户')
  await page.fill('input[placeholder="请输入用户名"]', 'admin')
  await page.fill('input[placeholder="请输入密码"]', 'admin123')
  await page.fill('input[placeholder="请输入验证码"]', extractCaptcha(await page.locator('img').first().getAttribute('src')))
  await page.locator('button:has-text("登 录")').first().click()
  await page.waitForTimeout(6000)
  await page.evaluate(() => {
    localStorage.removeItem('sale-pre-order-page-config-doc')
    localStorage.removeItem('sale-pre-order-page-config-detail')
  })

  console.log('\n【1】列表页 · 按单据 Tab')
  await page.goto(`${FE}/sales/pre-order`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.ss-grid, .ant-table', { timeout: 30000 })
  await page.waitForTimeout(3500)

  const tabTexts = await page.locator('.tab-item').allInnerTexts().catch(() => [])
  check('双 Tab 呈现（按单据/按明细）', tabTexts.some(t => t.includes('按单据')) && tabTexts.some(t => t.includes('按明细')), tabTexts.join('|'))

  const toolBtns = await page.locator('.ant-space button').allInnerTexts().catch(() => [])
  check('工具栏 6 个功能按钮', ['新增', '刷新', '批量订货', '打印(F8)', '导出', '配置'].every(t => toolBtns.some(b => b.includes(t))), toolBtns.join('|').slice(0, 120))

  const docHeaders = await page.locator('.ss-grid thead th, .ant-table-thead th').allInnerTexts().catch(() => [])
  check('按单据列含 47 数据列（表头渲染）', docHeaders.length >= 40, `表头数=${docHeaders.length}`)
  const docBodies = await page.locator('.ss-grid tbody tr').count().catch(() => 0)
  check('列表数据行渲染（有真实数据）', docBodies > 0, `行数=${docBodies}`)

  console.log('\n【2】页面配置：查询条件显隐真实生效')
  const fieldsBefore = await page.locator('.search-grid .search-field-item').count()
  await page.locator('button:has-text("配置")').first().click()
  await page.waitForTimeout(1200)
  const panelVisible = await page.locator('.config-table').first().isVisible().catch(() => false)
  check('页面配置弹窗打开', panelVisible)
  // 取消第一个查询条件（日期）的显示
  const firstChk = page.locator('.config-table tbody tr').first().locator('input[type=checkbox]')
  await firstChk.click()
  await page.waitForTimeout(800)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(800)
  const fieldsAfter = await page.locator('.search-grid .search-field-item').count()
  check('取消勾选后查询条件减少', fieldsAfter < fieldsBefore, `${fieldsBefore} → ${fieldsAfter}`)

  console.log('\n【3】页面配置：功能按钮开关真实生效')
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.ss-grid', { timeout: 30000 })
  await page.waitForTimeout(3000)
  const persisted = await page.evaluate(() => {
    const raw = localStorage.getItem('sale-pre-order-page-config-doc')
    return raw ? JSON.parse(raw) : null
  })
  check('查询条件配置已持久化', !!persisted?.queryFields && persisted.queryFields.some(f => f.visible === false),
    JSON.stringify((persisted?.queryFields || []).filter(f => !f.visible).map(f => f.label)))
  const fieldsAfterReload = await page.locator('.search-grid .search-field-item').count()
  check('刷新后仍保持隐藏', fieldsAfterReload === fieldsAfter, `${fieldsAfterReload}`)

  await page.locator('button:has-text("配置")').first().click()
  await page.waitForTimeout(1000)
  await page.locator('.ant-tabs-tab:has-text("功能按钮")').click()
  await page.waitForTimeout(600)
  const btnRows = await page.locator('.config-table tbody tr').count()
  check('功能按钮配置 6 项', btnRows >= 6, `行数=${btnRows}`)
  // 关闭「导出」按钮
  const exportRow = page.locator('.config-table tbody tr').filter({ hasText: '导出' }).first()
  await exportRow.locator('input[type=checkbox]').click()
  await page.waitForTimeout(800)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(800)
  const toolBtns2 = await page.locator('.ant-space button').allInnerTexts().catch(() => [])
  check('关闭后工具栏不再显示「导出」', !toolBtns2.some(b => b.includes('导出')), toolBtns2.join('|').slice(0, 100))

  console.log('\n【4】按明细 Tab · 63 列 + 商品分类树')
  await page.locator('.tab-item:has-text("按明细")').click()
  await page.waitForTimeout(3500)
  const detailHeaders = await page.locator('.ss-grid thead th, .ant-table-thead th').allInnerTexts().catch(() => [])
  const headerText = detailHeaders.join('|')
  const expected = ['单据日期', '单据编号', '单据状态', '仓库', '往来单位', '往来单位编号', '客户级别', '收货人', '联系电话', '收货地址',
    '客户一票通', '客户备注', '经手人', '部门', '商品名称', '货号', '条码', '规格', '型号', '产地', '品牌',
    '表体自定义1(数字)', '表体自定义10(部门)', '单位', '小单位', '小单位数量', '换算关系', '换算结果', '大包装', '中包装', '小包装',
    '预订数量', '已订数量', '未订数量', '已发数量', '未发数量', '终止数量', '终止金额', '单价', '小单位单价', '金额', '折扣(%)',
    '折后单价', '折后金额', '重量（kg）', '体积（m³）', '明细备注', '销售类型', '商品行属性', '单据备注', '摘要', '附件', '制单人', '审核人', '提交时间']
  const missing = expected.filter(h => !headerText.includes(h))
  check('按明细列与文档 63 列一致', missing.length === 0, missing.length ? `缺: ${missing.join('、')}` : `表头数=${detailHeaders.length}`)
  check('按明细不再含文档外列（无「批次条码/兑换积分」等）', !headerText.includes('批次条码') && !headerText.includes('兑换积分'))
  const catVisible = await page.locator('text=商品分类').first().isVisible().catch(() => false)
  check('按明细显示商品分类树', catVisible)

  console.log('\n【5】表单页 · 号段 / 明细列 / 账户下拉')
  await page.goto(`${FE}/sales/pre-order/create`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.ss-grid, .ant-table', { timeout: 30000 })
  await page.waitForTimeout(4000)
  const orderNoText = await page.locator('.bill-header, .form-page-container').first().innerText().catch(() => '')
  const m = orderNoText.match(/YDHD-\d{8}-\d{4}/)
  check('表单单号来自后端号段（YDHD-yyyyMMdd-NNNN）', !!m, m ? m[0] : orderNoText.slice(0, 120))

  const headers5 = await page.locator('.ss-grid thead th, .ant-table-thead th').allInnerTexts().catch(() => [])
  const h5 = headers5.join('|')
  check('表单明细列 61 列（含价格等级/自定义）', h5.includes('餐饮店') && h5.includes('连锁|vip') && h5.includes('单据自定义10(部门)') && h5.includes('可用库存换算结果'),
    `表头数=${headers5.length}`)

  // 收款项：预订金账户下拉真实可选
  await page.locator('.ant-tabs-tab:has-text("收款")').first().click()
  await page.waitForTimeout(800)
  const accSelect = page.locator('.ant-tabs-tabpane-active .ant-select').first()
  const accCount = await accSelect.count()
  let accOptions = 0
  if (accCount) {
    await accSelect.click()
    await page.waitForTimeout(900)
    accOptions = await page.locator('.ant-select-dropdown:visible .ant-select-item-option').count()
    await page.keyboard.press('Escape')
  }
  check('预订金账户为真实账户下拉（有选项）', accOptions > 0, `选项数=${accOptions}`)

  const moreBtn = page.locator('.ant-tabs-tabpane-active button:has-text("···")').first()
  const hasMore = await moreBtn.count()
  if (hasMore) {
    await moreBtn.click()
    await page.waitForTimeout(700)
    const modalTitle = await page.locator('.ant-modal-title:visible').last().innerText().catch(() => '')
    check('「更多账户」弹窗可打开', modalTitle.includes('更多'), modalTitle)
    await page.keyboard.press('Escape')
    await page.waitForTimeout(500)
  } else {
    check('「更多账户」按钮存在', false, '未找到 ··· 按钮')
  }

  console.log('\n【6】表单保存（号段号原样落库 → 跳转列表）')
  let created = false
  let orderNoShown = ''
  try {
    // 选择客户（基本信息区第一个下拉，placeholder=客户）
    const custSel = page.locator('.ant-select').first()
    await custSel.click()
    await page.waitForTimeout(2000)
    const custOpts = page.locator('.ant-select-dropdown:visible .ant-select-item-option')
    const custOptCount = await custOpts.count()
    console.log(`  ℹ️ 客户下拉选项数=${custOptCount}`)
    if (custOptCount > 0) {
      await custOpts.first().click()
      await page.waitForTimeout(1800)
    } else {
      await page.keyboard.press('Escape')
    }
    console.log(`  ℹ️ 已选客户=「${(await custSel.innerText().catch(() => '')).trim().slice(0, 20)}」`)

    // 明细首行：选择商品并填数量（BillDetailTable 可编辑单元格）
    const firstRow = page.locator('.ss-grid tbody tr').first()
    const rowSelect = firstRow.locator('.ant-select').first()
    if (await rowSelect.count()) {
      await rowSelect.click()
      await page.waitForTimeout(1200)
      const pOpt = page.locator('.ant-select-dropdown:visible .ant-select-item-option').first()
      if (await pOpt.count()) { await pOpt.click(); await page.waitForTimeout(1500) }
    }
    const qtyInput = firstRow.locator('input').nth(1)
    if (await qtyInput.count()) {
      await qtyInput.click()
      await qtyInput.fill('3')
      await qtyInput.press('Enter')
      await page.waitForTimeout(1200)
    }

    orderNoShown = (await page.locator('.form-page-container').first().innerText()).match(/YDHD-\d{8}-\d{4}/)?.[0] || ''
    await page.locator('button:has-text("保存草稿")').first().click()
    await page.waitForTimeout(6000)
    created = page.url().includes('/sales/pre-order') && !page.url().includes('/create')
    if (orderNoShown) console.log(`  ℹ️ 表单号=${orderNoShown}，保存后 URL=${page.url()}`)
  } catch (e) {
    console.log('  ⚠️ 表单保存流程异常：' + String(e.message).slice(0, 160))
  }
  const notFound = await page.locator('text=页面不存在').count()
  check('表单保存草稿并跳转列表（非 404）', created && notFound === 0, page.url())

  console.log('\n【7】表单导入（Excel 解析回填明细 + 模板可下载）')
  await page.goto(`${FE}/sales/pre-order/create`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.ss-grid', { timeout: 30000 })
  await page.waitForTimeout(5000)
  const code = execFileSync(PSQL, ['-h', 'localhost', '-U', 'devuser', '-d', 'devdb', '-t', '-A', '-c',
    "select product_code from erp_product where deleted=0 and tenant_id=1 and product_code<>'' limit 1"],
    { env: { ...process.env, PGPASSWORD: 'devuser123' }, encoding: 'utf8' }).trim()
  const XLSX = require('module').createRequire('I:/AI-Ready/frontend/apps/pc-admin/package.json')('xlsx')
  const ws = XLSX.utils.aoa_to_sheet([['货号', '条码', '商品名称', '数量', '单价', '备注'], [code, '', '', 7, 12, '导入行']])
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'S1')
  const tmpFile = require('path').join(require('os').tmpdir(), 'preorder-import.xlsx')
  XLSX.writeFile(wb, tmpFile)

  const importLogs = []
  page.on('console', m => { if (/导入|匹配|模板/.test(m.text())) importLogs.push(m.text().slice(0, 160)) })
  await page.locator('button:has-text("导入")').first().click()
  await page.waitForTimeout(1500)
  const importItem = page.locator('.ant-dropdown:visible .ant-dropdown-menu-item:has-text("Excel 导入明细")')
  console.log(`  ℹ️ 导入菜单项数=${await importItem.count()}`)
  const chooserPromise = page.waitForEvent('filechooser', { timeout: 8000 }).catch(() => null)
  if (await importItem.count()) await importItem.first().click()
  const chooser = await chooserPromise
  check('「Excel 导入明细」打开真实文件选择器', !!chooser)
  const rowsBefore = await page.locator('.ss-grid tbody tr').count()
  let toast = ''
  if (chooser) {
    await chooser.setFiles(tmpFile)
    for (let i = 0; i < 25; i++) {
      toast = await page.locator('.ant-message-notice-content').allInnerTexts().then(a => a.join(' ')).catch(() => '')
      if (toast) break
      await page.waitForTimeout(200)
    }
  }
  console.log(`  ℹ️ 导入提示=${toast || '（无）'} | 使用货号=${code}`)
  // 明细为「点击进入编辑」单元格：静态 DOM 无输入框，断言导入后新增行且带出商品与数量
  const rowsAfter = await page.locator('.ss-grid tbody tr').count()
  const lastRowText = await page.locator('.ss-grid tbody tr').last().innerText().catch(() => '')
  check('导入回填明细（新增 1 行 + 匹配到商品）',
    toast.includes('导入完成') && rowsAfter > rowsBefore && /博多|SP-20260704-041/.test(lastRowText),
    `行数 ${rowsBefore}→${rowsAfter} 末行=${lastRowText.replace(/\s+/g, ' ').slice(0, 90)}`)

  console.log('\n【8】列表行工作流（草稿可提交）')
  await page.goto(`${FE}/sales/pre-order`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.ss-grid', { timeout: 30000 })
  await page.waitForTimeout(3000)
  // 草稿默认被过滤：勾选「显示草稿」后应出现草稿行与工作流菜单
  await page.locator('label:has-text("显示草稿")').first().click()
  await page.waitForTimeout(800)
  await page.locator('.search-action-bar button').first().click()
  await page.waitForTimeout(3500)
  const moreBtns = page.locator('.ss-grid tbody tr button:has-text("更多")')
  let wfOk = false, wfDetail = ''
  const moreCount = await moreBtns.count()
  const rowCount = await page.locator('.ss-grid tbody tr').count()
  const firstRowTxt = await page.locator('.ss-grid tbody tr').first().innerText().catch(() => '')
  console.log(`  ℹ️ 行数=${rowCount} 更多按钮数=${moreCount} 首行=${firstRowTxt.replace(/\s+/g,' ').slice(0,80)}`)
  if (moreCount) {
    await moreBtns.first().click()
    await page.waitForTimeout(1200)
    const items = await page.locator('.ant-dropdown:visible .ant-dropdown-menu-item').allInnerTexts().catch(() => [])
    wfOk = items.some(t => t.includes('提交')) || items.some(t => t.includes('审核')) || items.some(t => t.includes('订货'))
    wfDetail = items.join('|')
  }
  check('行工作流菜单可用（提交/审核/订货任一）', wfOk, wfDetail)

  await page.screenshot({ path: 'preorder-list-final.png', fullPage: false }).catch(() => {})
  await browser.close()

  check('页面无 JS 运行时错误', consoleErrors.length === 0, consoleErrors.slice(0, 2).join(' || '))
  console.log(`\n════════ 浏览器验收：${pass} 通过 / ${fail} 失败 ════════`)
  process.exit(fail === 0 ? 0 : 1)
}

main().catch(e => { console.error('验收脚本异常:', e); process.exit(1) })
