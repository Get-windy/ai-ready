/* 商品辅助资料金标准 端到端验收（直连 API + 独立 headless 浏览器）
 * 覆盖：品牌/单位/标签 CRUD、单位组 CRUD、列配置、打印、导出 xlsx、DB 终态
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = 'http://localhost:5656'
// 共享 5655 会被并行会话反复重启，验收默认打独立副本 5659（可用 AUX_PORT 覆盖）
const PORT = Number(process.env.AUX_PORT || 5659)
const SHOTS = 'I:/AI-Ready/tool-results/product-supplement'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

function rawReq(method, p, body, token, raw = false) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: PORT, path: '/api' + p, method,
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
        if (raw) return resolve({ status: res.statusCode, buf })
        try { resolve({ status: res.statusCode, ...JSON.parse(buf.toString('utf8')) }) }
        catch { resolve({ status: res.statusCode, raw: buf.toString('utf8').slice(0, 300) }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

let TOKEN = null
async function api(method, p, body, raw) {
  let r = await rawReq(method, p, body, TOKEN, raw)
  if (!raw && r && (Number(r.code) === 401 || r.status === 401)) {
    await login()
    r = await rawReq(method, p, body, TOKEN, raw)
  }
  return r
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: process.env.E2E_USER || 'e2e_product', password: process.env.E2E_PASS || 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  TOKEN = token
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

const stamp = Date.now().toString().slice(-6)

async function apiSuite() {
  console.log('\n===== A. 接口层 =====')

  // ── A1 品牌 CRUD ──
  const brandName = `E2E品牌${stamp}`
  const c1 = await api('POST', '/erp/product-brand', {
    brandName, mnemonicCode: `E2EPP${stamp}`, remark: '端到端验收',
  })
  check('A1.1 新增品牌', c1.code === 200, JSON.stringify(c1).slice(0, 120))

  let bp = await api('GET', `/erp/product-brand/page?keyword=${encodeURIComponent(brandName)}&pageNum=1&pageSize=10`)
  const brandRow = (bp.data?.records || [])[0]
  check('A1.2 品牌分页 keyword 命中', !!brandRow, `total=${bp.data?.total}`)
  check('A1.3 品牌字段落库正确',
    brandRow?.brandName === brandName && brandRow?.mnemonicCode === `E2EPP${stamp}`,
    `name=${brandRow?.brandName} code=${brandRow?.mnemonicCode}`)

  const u1 = await api('PUT', `/erp/product-brand/${brandRow?.id}`, {
    brandName: brandName + '改', mnemonicCode: 'E2EGAI', remark: '改后备注',
  })
  check('A1.4 修改品牌', u1.code === 200)
  bp = await api('GET', `/erp/product-brand/page?keyword=${encodeURIComponent(brandName + '改')}&pageNum=1&pageSize=10`)
  check('A1.5 修改后读回一致',
    (bp.data?.records || [])[0]?.mnemonicCode === 'E2EGAI',
    `code=${(bp.data?.records || [])[0]?.mnemonicCode}`)

  // 助记码搜索（对标筛选条件 名称/助记码）
  const bs = await api('GET', `/erp/product-brand/page?keyword=E2EGAI&pageNum=1&pageSize=10`)
  check('A1.6 品牌按助记码可搜', (bs.data?.total || 0) >= 1, `total=${bs.data?.total}`)

  // ── A2 单位 CRUD + isDefault ──
  const unitName = `E2E单位${stamp}`
  const c2 = await api('POST', '/erp/product-unit-dict', {
    unitName, mnemonicCode: `E2EDW${stamp}`, remark: '单位备注30字', isDefault: 1,
  })
  check('A2.1 新增单位', c2.code === 200)
  let up = await api('GET', `/erp/product-unit-dict/page?keyword=${encodeURIComponent(unitName)}&pageNum=1&pageSize=10`)
  const unitRow = (up.data?.records || [])[0]
  check('A2.2 单位「是否默认」落库', unitRow?.isDefault === 1, `isDefault=${unitRow?.isDefault}`)
  check('A2.3 单位备注落库', unitRow?.remark === '单位备注30字', `remark=${unitRow?.remark}`)
  check('A2.4 单位按助记码可搜',
    ((await api('GET', `/erp/product-unit-dict/page?keyword=E2EDW${stamp}&pageNum=1&pageSize=10`)).data?.total || 0) >= 1)

  const u2 = await api('PUT', `/erp/product-unit-dict/${unitRow?.id}`, {
    unitName, mnemonicCode: `E2EDW${stamp}`, remark: '改后备注', isDefault: 0,
  })
  check('A2.5 修改单位', u2.code === 200)
  up = await api('GET', `/erp/product-unit-dict/page?keyword=${encodeURIComponent(unitName)}&pageNum=1&pageSize=10`)
  check('A2.6 是否默认改回0', (up.data?.records || [])[0]?.isDefault === 0,
    `isDefault=${(up.data?.records || [])[0]?.isDefault}`)

  // ── A3 标签 page + 对应商品聚合 + 停用/启用 ──
  const tagName = `E2E标签${stamp}`
  const c3 = await api('POST', '/erp/mall-tag', { tagName, sortOrder: 99 })
  check('A3.1 新增标签', c3.code === 200)
  let tp = await api('GET', `/erp/mall-tag/page?keyword=${encodeURIComponent(tagName)}&pageNum=1&pageSize=10`)
  const tagRow = (tp.data?.records || [])[0]
  check('A3.2 标签分页命中', !!tagRow, `total=${tp.data?.total}`)
  check('A3.3 标签含「对应商品」字段',
    tagRow !== undefined && 'productCount' in tagRow && 'productNames' in tagRow,
    `count=${tagRow?.productCount} names="${tagRow?.productNames}"`)
  check('A3.4 新建标签默认启用', tagRow?.status === 1, `status=${tagRow?.status}`)

  const s3 = await api('PUT', `/erp/mall-tag/${tagRow?.id}/status?status=0`)
  check('A3.5 停用标签', s3.code === 200)
  tp = await api('GET', `/erp/mall-tag/page?keyword=${encodeURIComponent(tagName)}&pageNum=1&pageSize=10`)
  check('A3.6 停用后读回 status=0', (tp.data?.records || [])[0]?.status === 0,
    `status=${(tp.data?.records || [])[0]?.status}`)
  const s3b = await api('PUT', `/erp/mall-tag/${tagRow?.id}/status?status=1`)
  check('A3.7 重新启用标签', s3b.code === 200)

  // ── A4 单位组 CRUD（对标：小/中/大单位 + 换算关系） ──
  const c4 = await api('POST', '/erp/product-unit-group', {
    status: 1,
    items: [
      { unitType: 'SMALL', unitName: '个', conversionRate: 1 },
      { unitType: 'MEDIUM', unitName: '件', conversionRate: 12 },
      { unitType: 'LARGE', unitName: '箱', conversionRate: 48 },
    ],
  })
  check('A4.1 新增单位组', c4.code === 200 && !!c4.data, JSON.stringify(c4).slice(0, 140))
  const groupId = c4.data

  // 查询条件「单位」= 组内单位名（EXISTS 子查询）
  let gp = await api('GET', `/erp/product-unit-group/page?keyword=${encodeURIComponent('件')}&pageNum=1&pageSize=10`)
  const gRow = (gp.data?.records || []).find(r => String(r.id) === String(groupId))
  check('A4.2 按单位名分页命中', !!gRow, `total=${gp.data?.total}`)
  check('A4.3 单位聚合串正确', gRow?.unitNames === '个,件,箱', `unitNames=${gRow?.unitNames}`)
  check('A4.4 单位关系聚合串正确', gRow?.unitRates === '1:12:48', `unitRates=${gRow?.unitRates}`)

  const d4 = await api('GET', `/erp/product-unit-group/${groupId}`)
  check('A4.5 单位组详情含 3 行明细', d4.code === 200 && (d4.data?.items || []).length === 3)
  check('A4.5b 明细含单位类型', (d4.data?.items || []).every(it => ['SMALL', 'MEDIUM', 'LARGE'].includes(it.unitType)),
    JSON.stringify((d4.data?.items || []).map(i => i.unitType)))

  const u4 = await api('PUT', `/erp/product-unit-group/${groupId}`, {
    status: 1,
    items: [
      { unitType: 'SMALL', unitName: '个', conversionRate: 1 },
      { unitType: 'MEDIUM', unitName: '包', conversionRate: 10 },
    ],
  })
  check('A4.6 修改单位组', u4.code === 200)
  const d4c = await api('GET', `/erp/product-unit-group/${groupId}`)
  check('A4.7 明细全量覆盖生效', (d4c.data?.items || []).length === 2 && d4c.data?.unitNames === '个,包' &&
    d4c.data?.unitRates === '1:10', `unitNames=${d4c.data?.unitNames} unitRates=${d4c.data?.unitRates}`)

  // 校验 1：缺小单位必须拒绝
  const bad1 = await api('POST', '/erp/product-unit-group', {
    items: [{ unitType: 'MEDIUM', unitName: '件', conversionRate: 12 }],
  })
  check('A4.8 缺小单位校验拦截', bad1.code !== 200, `code=${bad1.code} msg=${bad1.message}`)
  // 校验 2：中/大单位「单位关系」必须大于 0（对标 GoodsUnitTemplateEditor validate min:0/excMin）
  const bad2 = await api('POST', '/erp/product-unit-group', {
    items: [{ unitType: 'SMALL', unitName: '个', conversionRate: 1 },
            { unitType: 'MEDIUM', unitName: '件', conversionRate: 0 }],
  })
  check('A4.9 单位关系必须大于0', bad2.code !== 200, `code=${bad2.code} msg=${bad2.message}`)
  // 校验 3：同类型重复必须拒绝
  const bad3 = await api('POST', '/erp/product-unit-group', {
    items: [{ unitType: 'SMALL', unitName: '个', conversionRate: 1 },
            { unitType: 'SMALL', unitName: '只', conversionRate: 2 }],
  })
  check('A4.10 同类型重复校验拦截', bad3.code !== 200, `code=${bad3.code} msg=${bad3.message}`)

  // 行内「停用 / 启用」（对标 GoodsUnitTemplateList 操作列第二项）
  const stOff = await api('PUT', `/erp/product-unit-group/${groupId}/status?status=0`)
  const d4off = await api('GET', `/erp/product-unit-group/${groupId}`)
  check('A4.11 停用单位组', stOff.code === 200 && Number(d4off.data?.status) === 0,
    `code=${stOff.code} status=${d4off.data?.status}`)
  const stOn = await api('PUT', `/erp/product-unit-group/${groupId}/status?status=1`)
  const d4on = await api('GET', `/erp/product-unit-group/${groupId}`)
  check('A4.12 重新启用单位组', stOn.code === 200 && Number(d4on.data?.status) === 1,
    `code=${stOn.code} status=${d4on.data?.status}`)

  // ── A5 导出真实 xlsx ──
  for (const [label, p] of [
    ['品牌', '/erp/product-brand/export'],
    ['单位', '/erp/product-unit-dict/export'],
    ['标签', '/erp/mall-tag/export'],
    ['单位组', '/erp/product-unit-group/export'],
  ]) {
    const r = await api('GET', p, null, true)
    const isXlsx = r.buf && r.buf.length > 4 && r.buf[0] === 0x50 && r.buf[1] === 0x4b
    check(`A5 导出${label}为真实 xlsx`, r.status === 200 && isXlsx,
      `status=${r.status} bytes=${r.buf?.length}`)
  }

  // ── A6 单位组「显示状态」过滤 ──
  const st1 = await api('GET', '/erp/product-unit-group/page?status=1&pageNum=1&pageSize=50')
  check('A6.1 显示状态=已启用可查', st1.code === 200 && (st1.data?.records || []).length > 0,
    `total=${st1.data?.total}`)
  const st0 = await api('GET', '/erp/product-unit-group/page?status=0&pageNum=1&pageSize=50')
  check('A6.2 显示状态=已停用可查', st0.code === 200, `total=${st0.data?.total}`)

  // ── A7 清理 ──
  const d1 = await api('DELETE', `/erp/product-brand/${brandRow?.id}`)
  const d2 = await api('DELETE', `/erp/product-unit-dict/${unitRow?.id}`)
  const d3 = await api('DELETE', `/erp/mall-tag/${tagRow?.id}`)
  const d4b = await api('DELETE', `/erp/product-unit-group/${groupId}`)
  check('A7 清理测试数据', [d1, d2, d3, d4b].every(r => r.code === 200),
    [d1.code, d2.code, d3.code, d4b.code].join('/'))

  const after = await api('GET', `/erp/product-brand/page?keyword=${encodeURIComponent(brandName)}&pageNum=1&pageSize=10`)
  check('A7.1 品牌删除后不可见', Number(after.data?.total) === 0, `total=${after.data?.total}`)
  const afterG = await api('GET', `/erp/product-unit-group/${groupId}`)
  check('A7.2 单位组删除后详情不可读', afterG.code !== 200, `code=${afterG.code} msg=${afterG.message}`)
}

async function uiSuite() {
  console.log('\n===== B. 页面层（浏览器） =====')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  const errors = []
  page.on('pageerror', e => errors.push(String(e.message)))
  page.on('console', m => { if (m.type() === 'error') errors.push('console:' + m.text().slice(0, 160)) })

  // dev server 的 /api 反代指向共享 5655（可能已停），此处改写到独立副本并注入最新 token。
  // ⚠️ 必须按 pathname 前缀匹配：'**/api/**' 会误拦 Vite 的 /src/api/*.ts 模块请求。
  await page.route(
    (url) => url.pathname.startsWith('/api/'),
    async (route) => {
      const req = route.request()
      const u = new URL(req.url())
      try {
        const resp = await route.fetch({
          url: `http://localhost:${PORT}${u.pathname}${u.search}`,
          headers: { ...req.headers(), authorization: `Bearer ${TOKEN}` },
        })
        await route.fulfill({ response: resp })
      } catch (e) {
        await route.abort().catch(() => {})
      }
    },
  )

  // 并行会话共用 admin 会被 sa-token 互踢：导航前重登并注入最新 token
  const shot = async (n) => page.screenshot({ path: path.join(SHOTS, n) }).catch(() => {})
  const bodyText = async () => (await page.locator('body').innerText().catch(() => '')) || ''
  const headers = async () => page.evaluate(() =>
    [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim()).filter(Boolean)).catch(() => [])

  async function openPage(url, waitMs = 7000) {
    await login()
    // localStorage 需先处于 FE 源下才可写
    if (!page.url().startsWith(FE)) {
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(1200)
    }
    await page.evaluate(([tk, tid]) => {
      localStorage.setItem('token', tk)
      localStorage.setItem('tenantId', String(tid))
    }, [TOKEN, 1])
    await page.goto(url, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(waitMs)
  }

  await openPage(`${FE}/md/product-aux`, 9000)

  /** 并行会话共用 admin 会被 sa-token 互踢：页面被踢回登录页时重登并重开 */
  async function ensurePage() {
    const alive = await page.locator('.tab-item').count().catch(() => 0)
    if (alive > 0) return true
    console.log('  [页面被踢回登录，自愈重登]')
    await openPage(`${FE}/md/product-aux`, 9000)
    return (await page.locator('.tab-item').count().catch(() => 0)) > 0
  }

  /** antd 会在两个中文字符间插入空格（"查 询"），比较前统一去空白 */
  const flat = (s) => (s || '').replace(/\s+/g, '')
  const bodyFlat = async () => flat(await bodyText())

  let txt = await bodyText()
  let ftxt = flat(txt)
  check('B1 页面可打开（含 3 个子标签）',
    ['商品品牌', '商品单位', '商品标签'].every(t => ftxt.includes(t)), txt.slice(0, 90).replace(/\n/g, '|'))
  check('B2 无「商品分类」Tab（对标 3 Tab）', !ftxt.includes('商品分类'))
  check('B3 品牌 Tab 工具栏齐备',
    ['新增品牌', '刷新', '打印(F8)', '导出', '查询'].every(t => ftxt.includes(t)),
    ftxt.slice(0, 140))
  let hs = await headers()
  check('B4 品牌列表列头正确',
    ['操作', '品牌名称', '助记码', '备注'].every(t => hs.includes(t)), hs.join('/'))
  await shot('01-brand.png')

  // 单位 Tab
  await ensurePage()
  await page.locator('.tab-item', { hasText: '商品单位' }).click()
  await page.waitForTimeout(3500)
  ftxt = await bodyFlat()
  check('B5 单位 Tab 工具栏含「单位组管理」+「新增单位」',
    ftxt.includes('单位组管理') && ftxt.includes('新增单位'))
  hs = await headers()
  check('B6 单位列表列头正确',
    ['操作', '商品单位', '助记码', '计量单位备注', '是否默认'].every(t => hs.includes(t)), hs.join('/'))
  await shot('02-unit.png')

  // 标签 Tab
  await ensurePage()
  await page.locator('.tab-item', { hasText: '商品标签' }).click()
  await page.waitForTimeout(3500)
  hs = await headers()
  check('B7 标签列表列头正确',
    ['操作', '标签名称', '对应商品'].every(t => hs.includes(t)), hs.join('/'))
  ftxt = await bodyFlat()
  check('B8 标签行内操作为 修改/停用', ftxt.includes('停用') || ftxt.includes('启用'))
  await shot('03-tag.png')

  // 回到品牌，打开新增弹窗
  await ensurePage()
  await page.locator('.tab-item', { hasText: '商品品牌' }).click()
  await page.waitForTimeout(2500)
  await page.locator('button', { hasText: '新增品牌' }).first().click()
  await page.waitForTimeout(1800)
  ftxt = await bodyFlat()
  check('B9 新增品牌弹窗字段', ftxt.includes('品牌名称') && ftxt.includes('助记码') && ftxt.includes('备注'))
  await shot('04-brand-add.png')

  // ── UI 写路径：新增品牌 → 列表出现 → 删除 ──
  const uiBrand = `UI品牌${stamp}`
  await page.locator('.ant-modal input').first().fill(uiBrand)
  await page.waitForTimeout(600)
  const mnemonicVal = await page.locator('.ant-modal input').nth(1).inputValue().catch(() => '')
  check('B9.1 助记码随名称自动生成', mnemonicVal.length > 0, `mnemonic=${mnemonicVal}`)
  await page.locator('.ant-modal button', { hasText: '保存' }).first().click()
  await page.waitForTimeout(3000)
  await page.locator('.search-section input').first().fill(uiBrand)
  await page.locator('.search-section .btn-search').first().click()
  await page.waitForTimeout(3000)
  ftxt = await bodyFlat()
  check('B9.2 UI 新增品牌后列表可查得', ftxt.includes(uiBrand), ftxt.slice(0, 160))
  await shot('04b-brand-created.png')

  // UI 删除（确认弹窗）
  const rowIdx = await page.evaluate((name) => {
    const rows = [...document.querySelectorAll('.ss-grid tbody tr')]
    return rows.findIndex(r => (r.innerText || '').includes(name))
  }, uiBrand).catch(() => -1)
  if (rowIdx >= 0) {
    await page.locator('.ss-grid tbody tr').nth(rowIdx).locator('button', { hasText: '删除' }).first().click()
    await page.waitForTimeout(1200)
    await page.locator('.ant-modal-confirm button', { hasText: '确认删除' }).first().click()
    await page.waitForTimeout(3000)
    ftxt = await bodyFlat()
    check('B9.3 UI 删除品牌后列表不再出现', !ftxt.includes(uiBrand), ftxt.slice(0, 160))
  } else {
    check('B9.3 UI 删除品牌后列表不再出现', false, '未定位到新增行')
  }
  await page.locator('.search-section input').first().fill('')
  await page.locator('.search-section .btn-search').first().click()
  await page.waitForTimeout(2000)

  // 单位弹窗（是否默认）
  await ensurePage()
  await page.locator('.tab-item', { hasText: '商品单位' }).click()
  await page.waitForTimeout(2500)
  await page.locator('button', { hasText: '新增单位' }).first().click()
  await page.waitForTimeout(1800)
  ftxt = await bodyFlat()
  check('B10 新增单位弹窗含「是否默认」', ftxt.includes('是否默认'))
  await shot('05-unit-add.png')
  await page.keyboard.press('Escape')
  await page.waitForTimeout(800)

  // 单位组管理弹窗（对标：工具栏 新增单位组|打印(F8)|导出|刷新 + 查询 单位|显示状态）
  await ensurePage()
  await page.locator('button', { hasText: '单位组管理' }).first().click()
  await page.waitForTimeout(1000)
  await page.locator('.ant-modal button', { hasText: '新增单位组' }).first().waitFor({ state: 'visible', timeout: 15000 }).catch(() => {})
  await page.waitForTimeout(2500)
  ftxt = await bodyFlat()
  check('B11 单位组管理弹窗打开',
    ftxt.includes('单位组管理') && ftxt.includes('新增单位组') && ftxt.includes('刷新'),
    ftxt.slice(-220))
  check('B11.0 单位组弹窗工具栏/查询对齐对标',
    ftxt.includes('打印(F8)') && ftxt.includes('导出') && ftxt.includes('单位关系') && ftxt.includes('已启用'),
    ftxt.slice(-260))
  await shot('06-unitgroup.png')
  // 打开新增单位组表单，校验字段（对标：类型 / 单位名称 / 换算关系）
  await page.locator('.ant-modal button', { hasText: '新增单位组' }).first().click()
  await page.waitForTimeout(2000)
  ftxt = await bodyFlat()
  check('B11.1 单位组新增编辑表单字段对齐对标',
    ftxt.includes('单位组新增编辑') && ftxt.includes('类型') && ftxt.includes('单位名称') && ftxt.includes('换算关系') &&
    ftxt.includes('小单位') && ftxt.includes('中单位') && ftxt.includes('大单位') && ftxt.includes('保存(Enter)'),
    ftxt.slice(-200))
  await shot('06b-unitgroup-form.png')
  // 单位名称下拉来自单位字典
  await page.locator('.ant-modal .ug-edit-grid .ant-select-selector').first().click({ force: true }).catch(() => {})
  await page.waitForTimeout(1500)
  const ugOpts = await page.locator('.ant-select-item-option').count().catch(() => 0)
  check('B11.2 单位名称下拉来自单位字典', ugOpts > 0, `options=${ugOpts}`)
  await shot('06c-unitgroup-select.png')
  await page.keyboard.press('Escape')
  await page.waitForTimeout(700)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(700)

  // 列配置齿轮（表头 rowNo 列内嵌 .th-settings-btn）
  await ensurePage()
  const gear = page.locator('.th-settings-btn').first()
  const gearCount = await page.locator('.th-settings-btn').count().catch(() => 0)
  check('B12 数据表表头齿轮（列配置入口）存在', gearCount > 0, `count=${gearCount}`)
  if (gearCount > 0) {
    await gear.click({ force: true }).catch(() => {})
    await page.waitForTimeout(1500)
    ftxt = await bodyFlat()
    check('B12.1 列配置弹窗含个人/全局配置', ftxt.includes('个人配置') && ftxt.includes('全局配置'))
    await shot('07-colconfig.png')
    await page.keyboard.press('Escape')
    await page.waitForTimeout(600)
  }

  check('B13 页面无 JS 运行错误',
    errors.filter(e => !/favicon|ResizeObserver|Failed to load resource/i.test(e)).length === 0,
    errors.slice(0, 3).join(' || ').slice(0, 260))

  await browser.close()
}

(async () => {
  await login()
  await apiSuite()
  // SKIP_UI=1 只跑接口层（并行会话抢占前端/sa-token 时用）
  if (process.env.SKIP_UI !== '1') await uiSuite()
  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 汇总：${pass}/${results.length} =====`)
  const fails = results.filter(r => !r.ok)
  if (fails.length) {
    console.log('失败项：')
    fails.forEach(f => console.log(`  ❌ ${f.name} — ${f.detail || ''}`))
  }
  fs.writeFileSync(path.join(SHOTS, 'e2e-result.json'), JSON.stringify({ pass, total: results.length, results }, null, 1), 'utf-8')
  process.exit(fails.length ? 1 : 0)
})().catch(e => { console.error('FATAL', e.message, e.stack); process.exit(2) })
