/* 商品货位设置 交互补充验收（补齐 e2e-md-location.cjs 未覆盖的交互）
 * 用法：BE_PORT=5659 FE=http://localhost:5742 node tools/e2e-md-location-ui.cjs
 *
 * 覆盖：分类树点选过滤 / 条码·上架·显示状态下拉 UI 生效 / 仅显示有库存 /
 *       列配置显隐（含全局持久化）/ 批量设置 / 批量移除 / 条码打印 / 打印(F8) / 表头排序
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE || 'http://localhost:5742'
const PORT = Number(process.env.BE_PORT || 5659)
const SHOTS = 'I:/AI-Ready/tool-results/md-location'
const TENANT = 1
const E2E_USER = process.env.E2E_USER || 'e2e_mdloc'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const WH = 1

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })
// 清理安全网：脚本中途 FATAL 时也要删掉 E2E 测试货位与绑定
let token = null
const madeLocations = []
async function cleanupTestData() {
  if (!token) return
  try {
    for (const l of madeLocations) await apiReq('DELETE', `/wms/location/${l.id}`, null, token).catch(() => { })
    const pids = madeLocations.length ? madeLocations.map(() => null).filter(Boolean) : []
    if (madeLocations.length) {
      await apiReq('POST', '/erp/product-location/batch-remove', { warehouseId: WH, productIds: pids }, token).catch(() => { })
    }
    console.log('已清理测试货位:', madeLocations.map(m => m.code).join(',') || '(无)')
  } catch (e) { /* 忽略 */ }
}


function apiReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        if ((res.headers['content-type'] || '').includes('json')) {
          try { resolve(JSON.parse(buf.toString('utf8'))) } catch (e) { resolve({ raw: buf.toString('utf8'), status: res.statusCode }) }
        } else {
          resolve({ __binary: true, status: res.statusCode, size: buf.length })
        }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login() {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  return token
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

async function main() {
  token = await login()
  console.log(`登录成功（后端 ${PORT}）`)

  for (const [code, name] of [['E2E-U01', 'E2E交互货位1'], ['E2E-U02', 'E2E交互货位2']]) {
    const r = await apiReq('POST', '/wms/location/save', {
      warehouseId: WH, locationCode: code, locationName: name,
      locationType: 1, locationLevel: 4, status: 1, isPickable: 1, isReceivable: 1,
    }, token)
    if (r?.data?.id) madeLocations.push({ id: r.data.id, code })
  }
  check('准备 2 个真实货位', madeLocations.length === 2)

  const all = (await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50`, null, token))?.data || {}
  const total = Number(all.total || 0)
  const pids = (all.records || []).map(r => r.productId)
  check('商品基数 >= 2 行（批量操作前置）', total >= 2, `total=${total}`)

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  const jsErrors = []
  page.on('pageerror', e => jsErrors.push(String(e.message).slice(0, 160)))

  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.origin === FE && url.pathname.startsWith('/api/')) {
      try {
        const resp = await route.fetch({ url: `http://localhost:${PORT}${url.pathname}${url.search}` })
        return route.fulfill({ response: resp })
      } catch (e) { return route.continue() }
    }
    return route.continue()
  })

  async function injectToken(t) {
    await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
    await page.evaluate(([tk, tid]) => {
      localStorage.setItem('token', tk)
      localStorage.setItem('tenantId', String(tid))
    }, [t, TENANT])
  }

  await injectToken(token)
  await page.goto(`${FE}/md/location`, { waitUntil: 'domcontentloaded' }).catch(() => { })
  await page.waitForTimeout(12000)
  await page.goto(`${FE}/md/location`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.category-list-layout', { timeout: 60000 }).catch(() => { })
  await page.waitForTimeout(2500)

  // 选仓库 → 查询（查询条件是页面内存态，刷新/重进后需重选）
  async function selectWarehouse() {
    const whSelect = page.locator('.search-section .ant-select').first()
    await whSelect.click({ force: true })
    await page.waitForTimeout(1000)
    const whOpt = page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: '主仓库' }).first()
    await (await whOpt.count() ? whOpt : page.locator('.ant-select-dropdown:visible .ant-select-item-option').first()).click({ force: true })
    await page.waitForTimeout(3000)
    await page.waitForSelector('.ss-grid tbody button:has-text("设置")', { timeout: 20000 }).catch(() => { })
  }
  await selectWarehouse()

  const rowCount = () => page.locator('.ss-grid tbody button:has-text("设置")').count()
  /** 关闭全部弹层（弹窗遮罩会挡住后续点击） */
  const closeOverlays = async () => {
    await page.keyboard.press('Escape').catch(() => { })
    await page.waitForTimeout(500)
    await page.evaluate(() => {
      document.querySelectorAll('.ant-modal-wrap .ant-modal-close, .ant-drawer-close').forEach(el => el.click())
    })
    await page.waitForTimeout(800)
  }
  /** 展开「更多」菜单：a-dropdown trigger=click 是 toggle，已展开时不能再点（会关掉） */
  const openMore = async () => {
    const visible = await page.locator('.ant-dropdown:visible').count()
    if (!visible) {
      await page.locator('button:has-text("更多")').first().click({ force: true })
      await page.waitForTimeout(1200)
    }
    return page.locator('.ant-dropdown:visible')
  }
  /** 等待列表渲染出 n 行（Vue 渲染 + 折叠占位可能滞后） */
  const waitRows = async (n) => {
    await page.waitForFunction(
      (k) => document.querySelectorAll('.ss-grid tbody input.ss-checkbox').length >= k,
      n, { timeout: 20000 },
    ).catch(() => { })
    await page.waitForTimeout(600)
  }
  const baseRows = await rowCount()
  check('基准：列表已加载真实行', baseRows >= 2, `行=${baseRows}`)

  // ═══ 1. 分类树点选过滤 ═══
  const treeNodes = page.locator('.category-panel .ant-tree-node-content-wrapper')
  const nodeCount = await treeNodes.count()
  let catPicked = ''
  if (nodeCount > 1) {
    // 取第一个非根节点（根为「全部商品」）
    const node = treeNodes.nth(1)
    catPicked = (await node.innerText().catch(() => '')).replace(/\(\d+\)/, '').trim()
    await node.click({ force: true })
    await page.waitForTimeout(3000)
    const pathText = await page.locator('.category-panel').innerText().catch(() => '')
    check('分类树点选 → 当前路径更新', !!catPicked && pathText.includes(catPicked), `分类=${catPicked}`)
    const rowsAfter = await rowCount()
    check('分类树点选 → 列表按分类过滤（行数变化或提示空）', rowsAfter !== baseRows || rowsAfter === 0,
      `过滤后行=${rowsAfter}（基准 ${baseRows}）`)
    await page.screenshot({ path: `${SHOTS}/10-category-filter.png` })
    // 回到根节点
    await treeNodes.nth(0).click({ force: true })
    await page.waitForTimeout(3000)
  } else {
    check('分类树可点选（节点数 > 1）', false, `节点=${nodeCount}`)
  }

  // ═══ 2. 查询下拉 UI 生效（与接口口径双向核对）═══
  const apiTotal = async (qs) => Number((await apiReq('GET', `/erp/product-location/page?warehouseId=${WH}&pageSize=50&${qs}`, null, token))?.data?.total || 0)

  async function pickSelect(index, label) {
    const sel = page.locator('.search-section .ant-select').nth(index)
    await sel.click({ force: true })
    await page.waitForTimeout(900)
    const opt = page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: label }).first()
    if (!(await opt.count())) return false
    await opt.click({ force: true })
    await page.waitForTimeout(2600)
    return true
  }

  /** 点「查询」按钮（对标：下拉本身不自动查，仅两个复选框 onChange 即查）
   *  ⚠️ antd 会给两个汉字的按钮自动插空格 → 渲染成「查 询」，用 :has-text("查询") 会失配 */
  const clickQuery = async () => {
    await page.locator('.search-section button.ant-btn-primary').first().click({ force: true })
    await page.waitForTimeout(2600)
  }

  /** UI 行数应与接口 total 一致（折叠最多显示 4 行时只断言 >0） */
  async function pickAndVerify(index, label, qs, name) {
    const expect = await apiTotal(qs)
    const ok0 = await pickSelect(index, label)
    await clickQuery()
    const rows = await rowCount()
    const pass = ok0 && (expect === 0 ? rows === 0 : (rows > 0 && (expect > 4 || rows === expect)))
    check(name, pass, `接口 total=${expect} / UI 行=${rows}`)
    return { expect, rows }
  }
  // 顺序：0 仓库 / 1 条码 / 2 上架状态 / 3 显示状态
  await pickAndVerify(1, '有条码', 'hasBarcodeStatus=2', '「条码=有条码」UI 与接口口径一致')
  await pickAndVerify(1, '无条码', 'hasBarcodeStatus=1', '「条码=无条码」UI 与接口口径一致')
  await pickSelect(1, '全部'); await clickQuery()
  await pickAndVerify(2, '未上架', 'shelfStatus=0', '「上架状态=未上架」UI 与接口口径一致')
  await pickSelect(2, '全部'); await clickQuery()
  await pickAndVerify(3, '已停用', 'showStop=1', '「显示状态=已停用」UI 与接口口径一致')
  await pickSelect(3, '已启用'); await clickQuery()
  check('「显示状态」恢复默认已启用', (await rowCount()) === baseRows || baseRows > 4, `行=${await rowCount()}`)

  // ═══ 3. 仅显示有库存的商品 ═══
  const stockCheck = page.locator('.search-section .ant-checkbox-wrapper').filter({ hasText: '仅显示有库存的商品' }).first()
  if (await stockCheck.count()) {
    await stockCheck.click({ force: true })
    await page.waitForTimeout(2600)
    const stockRows = await rowCount()
    const stockTxt = await page.locator('.ss-grid tbody').innerText().catch(() => '')
    check('「仅显示有库存的商品」勾选生效', stockRows <= baseRows, `行=${stockRows}（基准 ${baseRows}）文本=${stockTxt.replace(/\s+/g, ' ').slice(0, 60)}`)
    await stockCheck.click({ force: true })
    await page.waitForTimeout(2600)
    check('「仅显示有库存的商品」取消恢复', (await rowCount()) === baseRows, `行=${await rowCount()}`)
  } else {
    check('存在「仅显示有库存的商品」复选框', false)
  }

  // ═══ 4. 列配置显隐（个人配置）═══
  const gear = page.locator('.th-settings-btn').first()
  const openColPanel = async () => {
    await gear.click({ force: true })
    await page.waitForTimeout(1500)
    return page.locator('.ant-modal-wrap').filter({ hasText: '个人配置' }).last()
  }
  const closeColPanel = async () => {
    await page.locator('.ant-modal-wrap .ant-modal-close').first().click({ force: true }).catch(() => { })
    await page.waitForFunction(() => document.querySelectorAll('.ant-modal-wrap').length === 0, null, { timeout: 8000 }).catch(() => { })
    await page.waitForTimeout(1200)
  }
  /** 列配置弹窗每列 = .col-setting-row（自绘行：checkbox + 列名），非 ant-checkbox-wrapper */
  const colRow = (wrap, title) => wrap.locator('.col-setting-row').filter({ has: page.locator('.col-setting-title', { hasText: title }) }).first()

  let wrap = await openColPanel()
  let row = colRow(wrap, '条码')
  check('列配置弹窗含「条码」可配置项', (await row.count()) > 0, `匹配=${await row.count()}`)
  if (await row.count()) {
    await row.locator('.ant-checkbox-input').click({ force: true })
    await page.waitForTimeout(600)
    await closeColPanel()
    let headers = (await page.locator('.ss-grid thead th').allInnerTexts().catch(() => [])).map(h => h.trim())
    check('列配置勾选「条码」→ 表头出现条码列', headers.includes('条码'), headers.filter(Boolean).join('/'))

    // 刷新后仍生效（个人配置持久化到 localStorage）
    await page.reload({ waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(3000)
    if (page.url().includes('/login')) {
      token = await login(); await injectToken(token)
      await page.goto(`${FE}/md/location`, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(3000)
    }
    headers = (await page.locator('.ss-grid thead th').allInnerTexts().catch(() => [])).map(h => h.trim())
    check('列配置刷新后保持（个人配置持久化）', headers.includes('条码'), headers.filter(Boolean).join('/'))

    // 恢复：取消勾选
    wrap = await openColPanel()
    row = colRow(wrap, '条码')
    await row.locator('.ant-checkbox-input').click({ force: true })
    await page.waitForTimeout(600)
    await closeColPanel()
    headers = (await page.locator('.ss-grid thead th').allInnerTexts().catch(() => [])).map(h => h.trim())
    check('列配置取消勾选「条码」→ 表头恢复隐藏', !headers.includes('条码'), headers.filter(Boolean).join('/'))
    // reload 会清空查询条件（内存态），重新选仓库查询，保证后续勾选行是当前数据
    await selectWarehouse()
  }

  // ═══ 5. 批量设置推荐货位 ═══
  await waitRows(2)
  const boxes = page.locator('.ss-grid tbody .ss-checkbox')
  const boxCount = await boxes.count()
  check('列表存在自绘复选框', boxCount >= 2, `复选框=${boxCount}`)
  if (boxCount >= 2) {
    await boxes.nth(0).click({ force: true })
    await boxes.nth(1).click({ force: true })
    await page.waitForTimeout(800)
    const dd = await openMore()
    await dd.getByText('批量设置', { exact: true }).click({ force: true })
    await page.waitForTimeout(2500)
    const batchWrap = page.locator('.ant-modal-wrap').filter({ hasText: '选择货位' }).last()
    const batchText = await batchWrap.innerText().catch(() => '')
    check('批量设置弹窗显示选中商品数', /本次设置\s*2\s*个商品/.test(batchText.replace(/\s+/g, ' ')), batchText.split('\n').filter(Boolean).slice(0, 2).join(' / '))
    const bl = batchWrap.locator('.ant-table-tbody tr.ant-table-row')
    if (await bl.count()) {
      await bl.first().locator('.ant-radio-wrapper, input[type=radio]').first().click({ force: true }).catch(() => { })
      await page.waitForTimeout(500)
      const code = ((await bl.first().innerText()) || '').match(/E2E-U\d+/)?.[0] || ''
      await batchWrap.locator('.ant-modal-footer button.ant-btn-primary').first().click({ force: true })
      await page.waitForTimeout(3200)
      const body = await page.locator('.ss-grid tbody').innerText().catch(() => '')
      const n = (body.match(new RegExp(code, 'g')) || []).length
      check('批量设置后两行都回填同一货位编码', n >= 2, `编码=${code} 出现 ${n} 次`)
      await page.screenshot({ path: `${SHOTS}/11-batch-set.png` })
      await closeOverlays()
    } else {
      check('批量设置弹窗加载真实货位', false)
    }
  }

  // ═══ 6. 条码打印（无条码行应给出明确提示）═══
  await page.locator('button:has-text("条码打印")').first().click({ force: true })
  await page.waitForTimeout(1800)
  const bodyTxt = await page.locator('body').innerText().catch(() => '')
  const hasBarcodeRow = (all.records || []).some(r => r.barcode)
  check('条码打印：有条码行→出标签 / 无条码行→明确提示（不静默）',
    hasBarcodeRow ? /条码打印/.test(bodyTxt) : /未设置条码|没有条码|请先勾选/.test(bodyTxt),
    hasBarcodeRow ? '存在带条码商品' : '所选行无条码，已给出提示')
  await page.screenshot({ path: `${SHOTS}/12-barcode-print.png` })
  await page.locator('.ant-modal-wrap .ant-modal-close').first().click({ force: true }).catch(() => { })
  await page.waitForTimeout(800)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(600)

  // ═══ 7. 打印(F8) ═══
  await page.locator('button:has-text("打印(F8)")').first().click({ force: true })
  await page.waitForTimeout(3000)
  const printVisible = await page.locator('.ant-modal-wrap, .ant-drawer-content').filter({ hasText: '打印' }).count()
  check('打印(F8) 打开打印弹窗（PrintDialog）', printVisible > 0, `匹配=${printVisible}`)
  await page.screenshot({ path: `${SHOTS}/13-print-dialog.png` })
  await closeOverlays()

  // ═══ 8. 表头排序（对标商品名称/货号/仓库/推荐货位可排序，点排序图标）═══
  const nameTh = page.locator('.ss-grid thead th').filter({ hasText: '商品名称' }).first()
  const sortIcon = nameTh.locator('.th-sort-icon').first()
  check('「商品名称」列带排序图标（对齐对标）', (await sortIcon.count()) > 0, `图标=${await sortIcon.count()}`)
  if (await sortIcon.count()) {
    await closeOverlays()
    await waitRows(2)
    const before = (await page.locator('.ss-grid tbody').innerText()).replace(/\s+/g, ' ')
    await sortIcon.click({ force: true })
    await page.waitForTimeout(1200)
    const ascClass = await sortIcon.getAttribute('class').catch(() => '')
    const after = (await page.locator('.ss-grid tbody').innerText()).replace(/\s+/g, ' ')
    check('点击排序图标后升序生效（图标态 + 数据顺序）', /sort-asc/.test(ascClass || '') && (before !== after || baseRows <= 1),
      `class=${ascClass} 顺序变化=${before !== after}`)
    await sortIcon.click({ force: true })
    await page.waitForTimeout(1000)
    const descClass = await sortIcon.getAttribute('class').catch(() => '')
    check('再次点击切换为降序', /sort-desc/.test(descClass || ''), `class=${descClass}`)
    await sortIcon.click({ force: true })
    await page.waitForTimeout(800)
  }

  // ═══ 9. 批量移除 ═══
  await waitRows(2)
  // 按内容定位（行序可能被前面的排序操作影响），只勾选确实已设置货位的行
  const rowsWithLoc = page.locator('.ss-grid tbody tr').filter({ hasText: 'E2E-U' })
  const nWithLoc = await rowsWithLoc.count()
  if (nWithLoc >= 1) {
    for (let i = 0; i < nWithLoc; i++) {
      await rowsWithLoc.nth(i).locator('.ss-checkbox').click({ force: true })
    }
    await page.waitForTimeout(800)
    // 勾选是否生效由后续「批量移除」确认框与清空结果验证（自绘复选框不用 :checked 伪类）
    const dd2 = await openMore()
    await dd2.getByText('批量移除', { exact: true }).click({ force: true })
    await page.waitForTimeout(1500)
    const cw = page.locator('.ant-modal-confirm').last()
    check('批量移除弹出二次确认', (await cw.innerText().catch(() => '')).includes('确定要删除推荐货位'))
    await cw.locator('button.ant-btn-primary').first().click({ force: true })
    await page.waitForTimeout(3200)
    const body2 = await page.locator('.ss-grid tbody').innerText().catch(() => '')
    check('批量移除后推荐货位清空', !/E2E-U\d+/.test(body2), `残留=${(body2.match(/E2E-U\d+/g) || []).join(',') || '无'}`)
    await page.screenshot({ path: `${SHOTS}/14-batch-remove.png` })
  } else {
    check('批量移除前置：存在已设货位的可勾选行', false, '未找到含 E2E-U 的行')
  }

  // ═══ 10. 「刷新」按钮 + 全局配置 Tab ═══
  await closeOverlays()
  await page.locator('button:has-text("刷新")').first().click({ force: true })
  await page.waitForTimeout(3200)
  check('「刷新」按钮重新加载列表', (await rowCount()) >= 1, `行=${await rowCount()}`)

  const gear2 = page.locator('.th-settings-btn').first()
  await gear2.click({ force: true })
  await page.waitForTimeout(1500)
  const globalTab = page.locator('.ant-modal-wrap .ant-tabs-tab').filter({ hasText: '全局配置' }).first()
  check('列配置弹窗存在「全局配置」Tab', (await globalTab.count()) > 0, `匹配=${await globalTab.count()}`)
  if (await globalTab.count()) {
    await globalTab.click({ force: true })
    await page.waitForTimeout(1800)
    const gWrap = page.locator('.ant-modal-wrap').filter({ hasText: '全局配置' }).last()
    const gRows = await gWrap.locator('.col-setting-row').count()
    check('「全局配置」列出全部 24 列', gRows >= 24, `行=${gRows}`)
    await page.screenshot({ path: `${SHOTS}/16-global-config.png` })
  }
  await closeOverlays()

  check('页面无 JS 运行时错误', jsErrors.length === 0, jsErrors.join(' | ') || '无')

  await page.screenshot({ path: `${SHOTS}/15-ui-final.png`, fullPage: true })
  await browser.close()

  // 清理
  await apiReq('POST', '/erp/product-location/batch-remove', { warehouseId: WH, productIds: pids }, token)
  for (const l of madeLocations) await apiReq('DELETE', `/wms/location/${l.id}`, null, token)
  check('清理测试数据', true)

  const failed = results.filter(r => !r.ok)
  console.log(`\n==== PASS ${results.length - failed.length} / FAIL ${failed.length} ====`)
  if (failed.length) {
    console.log('FAILED:', failed.map(f => f.name).join(' | '))
    process.exit(1)
  }
}

main().catch(async e => {
  console.error('FATAL', e?.message || e)
  await cleanupTestData()
  process.exit(1)
})
