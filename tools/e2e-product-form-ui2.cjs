/* 商品新增表单补充验收（本次改动项）
 *   1) 商品单位明细表：8 个标准价格等级列 + 重量（kg）/体积（m³）
 *   2) 商城信息 Tab：固定 19 个商品标签（与「商品上架」列不一致，无超市商店）
 *   3) 商品图片 Tab：引用图片空间入口
 * 运行：NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/e2e-product-form-ui2.cjs [apiPort] [vitePort]
 */
const http = require('http')
const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')
const { bootstrapAuthedPage } = require('./e2e-ui-login.cjs')

const API = Number(process.argv[2] || 5678)
const VITE = Number(process.argv[3] || 5656)
const OUT = path.resolve(__dirname, '../tool-results/product-ui')
const results = []

let TOKEN = ''
function apiReq(method, p, body) {
  return new Promise((resolve, reject) => {
    const d = body == null ? null : JSON.stringify(body)
    const h = { 'Content-Type': 'application/json' }
    if (TOKEN) h.Authorization = `Bearer ${TOKEN}`
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
  // 独立 E2E 用户：并行会话共用 admin 会触发 sa-token 互踢（页面与脚本互相顶掉，随机跳登录页）
  const res = await apiReq('POST', '/auth/login', {
    username: process.env.E2E_USER || 'e2e_product',
    password: process.env.E2E_PASS || 'admin123',
    tenantName: '系统租户',
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
  TOKEN = token

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  // 共享环境：vite 代理固定指向 5655（会被并行会话占用），改为「route 统一转发 +
  // 页面内真实登录」拿完整会话，避免只注入 token 时被 401 踢回登录页
  const boot = await bootstrapAuthedPage(page, API, VITE, { target: '/erp/product/create', wait: 9000 })
  if (boot?.token) TOKEN = boot.token
  await page.keyboard.press('Escape').catch(() => {})

  // ── 1. 商品单位明细表列（BillDetailTable 渲染为 .ss-grid） ──
  const unitHeaders = await page.$$eval('.ss-grid thead th', ths =>
    ths.map(t => (t.innerText || '').trim().replace(/\s+/g, ' ')).filter(Boolean))
  const gradeCols = unitHeaders.filter(h => /^价格等级\d$/.test(h))
  check('U1 商品单位表含 8 个标准价格等级列', gradeCols.length === 8, gradeCols.join(','))
  check('U2 商品单位表含「重量（kg）」', unitHeaders.includes('重量（kg）'), unitHeaders.slice(-3).join(','))
  check('U3 商品单位表含「体积（m³）」', unitHeaders.includes('体积（m³）'))
  check('U4 重量/体积排在等级价之后（对标列序）',
    unitHeaders.indexOf('重量（kg）') > unitHeaders.indexOf('价格等级8') &&
    unitHeaders.indexOf('体积（m³）') === unitHeaders.indexOf('重量（kg）') + 1,
    `idx 重量=${unitHeaders.indexOf('重量（kg）')} 体积=${unitHeaders.indexOf('体积（m³）')}`)
  check('U5 单位表默认 3 行（小/中/大）', (await page.locator('.ss-grid tbody tr').count()) >= 3)
  const gridH = await page.$eval('.unit-table-wrap', el => Math.round(el.getBoundingClientRect().height)).catch(() => 0)
  check('U5b 单位表可视高度正常（未塌陷）', gridH >= 110, `${gridH}px`)
  await page.screenshot({ path: path.join(OUT, 'form2-unit-table.png'), fullPage: true }).catch(() => {})

  // ── 2. 商品图片 Tab：引用图片空间 ──
  await page.click('.ant-tabs-tab:has-text("商品图片")').catch(() => {})
  await page.waitForTimeout(2500)
  const hasSpaceBtn = await page.locator('button:has-text("引用图片空间")').count()
  check('U6 商品图片 Tab 有「引用图片空间」入口', hasSpaceBtn > 0)
  if (hasSpaceBtn > 0) {
    await page.click('button:has-text("引用图片空间")')
    await page.waitForTimeout(3000)
    const modalTitle = await page.locator('.ant-modal-title:has-text("引用图片空间")').count()
    check('U7 引用图片空间弹窗可打开', modalTitle > 0)
    await page.screenshot({ path: path.join(OUT, 'form2-image-space.png'), fullPage: false }).catch(() => {})
    await page.keyboard.press('Escape')
    await page.waitForTimeout(1000)
  } else {
    check('U7 引用图片空间弹窗可打开', false, '按钮缺失')
  }

  // ── 3. 商城信息 Tab：19 个固定标签 ──
  await page.click('.ant-tabs-tab:has-text("商城信息")').catch(() => {})
  await page.waitForTimeout(2500)
  const tagLabels = await page.$$eval('.tag-item .ant-checkbox-wrapper', els =>
    els.map(e => (e.innerText || '').trim()).filter(Boolean))
  check('U8 商城信息标签 = 标签字典槽位（20 个）', tagLabels.length === 20, `实际 ${tagLabels.length}`)
  check('U9 标签显示名为字典昵称（默认「标签N」，前两槽位为用户已改昵称）',
    tagLabels[2] === '标签3' && tagLabels[19] === '标签20',
    tagLabels.slice(0, 3).join('/') + ' … ' + tagLabels[19])
  check('U10 标签不再硬编码业务昵称（无「早餐面点 / 超市商店」）',
    !tagLabels.includes('早餐面点') && !tagLabels.includes('超市商店'), tagLabels.join('/'))
  check('U11 标签为固定项（无新增/删除入口）',
    (await page.locator('.tag-add-row').count()) === 0 && (await page.locator('.tag-delete-btn').count()) === 0)
  await page.screenshot({ path: path.join(OUT, 'form2-mall-tags.png'), fullPage: true }).catch(() => {})

  // ── 4. 自定义昵称联动（价格等级按标准槽位 GRADE_N 定位，昵称仅作显示名）──
  const gr = await apiReq('GET', '/erp/product-grade/list')
  const g1 = (gr.data || [])[0]
  const oldName = g1?.gradeName
  if (g1) {
    await apiReq('PUT', `/erp/product-grade/${g1.id}`, { gradeName: '餐饮店' })
    await page.reload({ waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(9000)
    await page.keyboard.press('Escape').catch(() => {})
    const heads2 = await page.$$eval('.ss-grid thead th', ths =>
      ths.map(t => (t.innerText || '').trim().replace(/\s+/g, ' ')).filter(Boolean))
    check('U12 自定义昵称生效（GRADE_1 昵称改为「餐饮店」）', heads2.includes('餐饮店'), heads2.slice(12, 22).join(','))
    check('U13 槽位数不因改昵称而变化（仍 8 列）', heads2.filter(h => /^(价格等级|餐饮店)\d*$/.test(h) || h === '餐饮店').length >= 8,
      String(heads2.filter(h => h.startsWith('价格等级') || h === '餐饮店').length))
    await page.screenshot({ path: path.join(OUT, 'form2-grade-nickname.png'), fullPage: true }).catch(() => {})
    await apiReq('PUT', `/erp/product-grade/${g1.id}`, { gradeName: oldName })
  } else {
    check('U12 自定义昵称生效（GRADE_1 昵称改为「餐饮店」）', false, '等级接口无数据')
    check('U13 槽位数不因改昵称而变化（仍 8 列）', false, '等级接口无数据')
  }

  // ── 5. 单位表槽位规则 + 区域高度自适应 ──
  await page.click('.ant-tabs-tab:has-text("基本信息")').catch(() => {})
  await page.waitForTimeout(2000)
  const wrapH = () => page.$eval('.unit-table-wrap', el => Math.round(el.getBoundingClientRect().height))
  const rowCount = () => page.locator('.unit-table-wrap .ss-grid tbody tr').count()
  // 类型列渲染为单元格文本（第二列：序号 | 类型 | …）
  const typeTexts = () => page.$$eval('.unit-table-wrap .ss-grid tbody tr', trs =>
    trs.map(tr => {
      const td = tr.querySelector('td:nth-child(2)')
      return td ? (td.innerText || '').trim() : ''
    }))
  const actionBtn = (n) => page.locator(`.unit-table-wrap .ss-grid tbody tr:nth-child(${n}) button`).first()

  const h0 = await wrapH()
  const types0 = await typeTexts()
  check('U16 默认 3 行且类型预填 小/中/大单位',
    types0.slice(0, 3).join('/') === '小单位/中单位/大单位', types0.join('/'))
  check('U17 区域预留表头+3 行高度', h0 >= 110 && h0 < 220, `${h0}px / ${await rowCount()} 行`)

  await page.click('.unit-actions button')
  await page.waitForTimeout(900)
  const h1 = await wrapH()
  const types1 = await typeTexts()
  check('U18 新增行类型预填「单位4」', types1[3] === '单位4', types1.join('/'))
  check('U19 新增后区域高度同步增加约 1 行', h1 - h0 > 20, `${h0} → ${h1}px`)

  await page.click('.unit-actions button')
  await page.waitForTimeout(900)
  const h2 = await wrapH()
  const types2 = await typeTexts()
  check('U20 第 5 行类型预填「单位5」', types2[4] === '单位5', types2.join('/'))
  check('U21 高度再增加 1 行', h2 - h1 > 20, `${h1} → ${h2}px`)

  const btn4Disabled = await actionBtn(4).isDisabled().catch(() => null)
  check('U22 有第 5 行时第 4 行删除按钮禁用（必须依次删）', btn4Disabled === true, String(btn4Disabled))

  // 固定列会被单元格遮挡，用原生 click 触发
  const clickRowBtn = (n) => page.evaluate((idx) => {
    const tr = document.querySelectorAll('.unit-table-wrap .ss-grid tbody tr')[idx - 1]
    const b = tr && tr.querySelector('button')
    if (b) b.click()
  }, n)

  await clickRowBtn(5)
  await page.waitForTimeout(1200)
  const h3 = await wrapH()
  check('U23 删掉第 5 行后高度回落 1 行', h1 === h3, `${h2} → ${h3}px`)
  const btn4Disabled2 = await actionBtn(4).isDisabled().catch(() => null)
  check('U24 第 5 行删除后第 4 行可删', btn4Disabled2 === false, String(btn4Disabled2))

  // 前三行：清空（不删行）
  const rowsBeforeClear = await rowCount()
  const btn1Title = await actionBtn(1).getAttribute('title').catch(() => '')
  check('U25 第 1 行是「清空」而非删除', String(btn1Title).includes('清空'), String(btn1Title))
  await clickRowBtn(1)
  await page.waitForTimeout(1200)
  check('U26 清空后行数不变（固定槽位不可删行）', (await rowCount()) === rowsBeforeClear, `${rowsBeforeClear} → ${await rowCount()}`)

  await page.screenshot({ path: path.join(OUT, 'form2-unit-rows.png'), fullPage: true }).catch(() => {})

  // ── 6. 价格等级管理页（多租户过滤修复后应能列出 8 条标准等级）──
  await page.goto(`http://localhost:${VITE}/erp/product/grade`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(8000)
  const gradeRows = await page.locator('.ant-table-tbody tr.ant-table-row').count()
  check('U14 价格等级管理页显示 8 条标准等级', gradeRows === 8, `实际 ${gradeRows} 行`)
  const codes = await page.$$eval('.ant-table-tbody tr.ant-table-row td:nth-child(2)', tds =>
    tds.map(t => (t.innerText || '').trim()))
  check('U15 等级编码展示为 GRADE_1..8',
    JSON.stringify(codes) === JSON.stringify(['GRADE_1', 'GRADE_2', 'GRADE_3', 'GRADE_4', 'GRADE_5', 'GRADE_6', 'GRADE_7', 'GRADE_8']),
    codes.join(','))
  await page.screenshot({ path: path.join(OUT, 'form2-grade-page.png'), fullPage: true }).catch(() => {})

  // ── 7. 编辑只有 1 个单位的商品：仍占 3 行、类型预填小/中/大，占位行不落库 ──
  const stamp2 = Date.now()
  const tree2 = await apiReq('GET', '/erp/product-category/tree')
  const flat2 = []
  const walk2 = (ns) => (ns || []).forEach(n => { flat2.push(n); walk2(n.children) })
  walk2(tree2.data)
  const catId2 = flat2[0]?.id
  const created1u = await apiReq('POST', '/erp/product/batch-create', {
    product: {
      productName: `E2E单单位-${stamp2}`, productCodeAlias: `E2E1U${stamp2}`,
      categoryId: catId2, industryCategory: '其他', productType: 'SINGLE',
    },
    units: [{ unitName: '瓶', unitType: 'SMALL', isBaseUnit: 1, conversionRate: 1, sortOrder: 1, retailPrice: 9.9 }],
  })
  const pid2 = created1u?.data?.productId
  check('U27a 造数：创建仅 1 个单位的商品', !!pid2, `productId=${pid2}`)
  const f0 = await apiReq('GET', `/erp/product/${pid2}/form`)
  const uid0 = f0?.data?.units?.[0]?.id
  await apiReq('PUT', `/erp/product/${pid2}`, {
    productName: `E2E单单位-${stamp2}`, categoryId: catId2, industryCategory: '其他',
    defaultSalesUnitId: uid0, defaultPurchaseUnitId: uid0, defaultStockUnitId: uid0,
  })

  await page.goto(`http://localhost:${VITE}/erp/product/form/${pid2}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(9000)
  const rowsE = await rowCount()
  const typesE = await typeTexts()
  check('U27 编辑单单位商品仍显示 3 行', rowsE === 3, `${rowsE} 行`)
  check('U28 类型列预填 小/中/大单位（占位行不为空）',
    typesE.join('/') === '小单位/中单位/大单位', typesE.join('/'))
  await page.screenshot({ path: path.join(OUT, 'form2-edit-single-unit.png'), fullPage: true }).catch(() => {})

  await page.locator('button:has-text("保存")').first().click().catch(() => {})
  await page.waitForTimeout(7000)
  const f1 = await apiReq('GET', `/erp/product/${pid2}/form`)
  const afterUnits = f1?.data?.units || []
  check('U29 占位行不落库（保存后仍只有 1 个单位）', afterUnits.length === 1,
    `保存后 ${afterUnits.length} 个：${afterUnits.map(u => u.unitName).join(',')}`)
  check('U30 原单位类型与换算关系保留',
    afterUnits[0]?.unitType === 'SMALL' && Number(afterUnits[0]?.conversionRate) === 1,
    `${afterUnits[0]?.unitType}/${afterUnits[0]?.conversionRate}`)
  await apiReq('DELETE', `/erp/product/${pid2}`)

  await browser.close()
  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 商品表单补充验收 ${pass}/${results.length} =====`)
  if (pass !== results.length) process.exit(1)
})().catch(e => { console.error('FATAL', e); process.exit(1) })
