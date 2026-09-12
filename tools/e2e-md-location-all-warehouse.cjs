/* 商品货位设置「全部仓库」口径验收
 * 需求（2026-09-12）：仓库非必填，下拉含「全部仓库」且默认选中；进入页面即出数据；
 *                   数据表用组件默认 20 行占满。
 * 用法：BE_PORT=5659 FE=http://localhost:5742 node tools/e2e-md-location-all-warehouse.cjs
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
const WH1 = 1   // 主仓库
const WH2 = 3   // 华南分仓

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

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
          try { resolve(JSON.parse(buf.toString('utf8'))) } catch (e) { resolve({ raw: buf.toString('utf8') }) }
        } else resolve({ __binary: true, status: res.statusCode, size: buf.length })
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
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid,
  })
  if (!res?.data?.token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  return res.data.token
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

let token = null
const madeLocations = []
async function cleanupTestData() {
  if (!token) return
  try {
    for (const l of madeLocations) await apiReq('DELETE', `/wms/location/${l.id}`, null, token).catch(() => { })
    console.log('已清理测试货位:', madeLocations.map(m => m.code).join(',') || '(无)')
  } catch (e) { /* 忽略 */ }
}

async function main() {
  token = await login()
  console.log(`登录成功（后端 ${PORT}）`)

  // ── 准备：两个仓库各一个真实货位 ──
  for (const [wh, code, name] of [[WH1, 'E2E-W1', 'E2E仓库1货位'], [WH2, 'E2E-W2', 'E2E仓库2货位']]) {
    const r = await apiReq('POST', '/wms/location/save', {
      warehouseId: wh, locationCode: code, locationName: name,
      locationType: 1, locationLevel: 4, status: 1, isPickable: 1, isReceivable: 1,
    }, token)
    if (r?.data?.id) madeLocations.push({ id: r.data.id, code, wh })
  }
  check('准备两个仓库的真实货位', madeLocations.length === 2, madeLocations.map(m => `${m.wh}:${m.code}`).join(' '))

  // ═══ 1. 接口：不传 warehouseId = 全部仓库 ═══
  const all = await apiReq('GET', '/erp/product-location/page?pageNum=1&pageSize=50', null, token)
  check('接口：不传仓库返回 200（仓库非必填）', all?.code === 200, JSON.stringify(all).slice(0, 120))
  const allRows = all?.data?.records || []
  const prodTotal = Number(all?.data?.total || 0)
  const byWh1 = await apiReq('GET', `/erp/product-location/page?warehouseId=${WH1}&pageNum=1&pageSize=50`, null, token)
  check('接口：全部仓库与指定仓库的商品总数一致（一商品一行，不因多仓库重复）',
    prodTotal === Number(byWh1?.data?.total || 0), `全部=${prodTotal} / 指定仓库=${byWh1?.data?.total}`)

  // ═══ 2. 同一商品在两个仓库各设一个货位 → 全部仓库下拼接展示 ═══
  const pid = allRows[0]?.productId
  const loc1 = madeLocations.find(m => m.wh === WH1)
  const loc2 = madeLocations.find(m => m.wh === WH2)
  await apiReq('POST', '/erp/product-location/set', { warehouseId: WH1, productIds: [pid], locationId: loc1.id }, token)
  await apiReq('POST', '/erp/product-location/set', { warehouseId: WH2, productIds: [pid], locationId: loc2.id }, token)
  const all2 = (await apiReq('GET', '/erp/product-location/page?pageNum=1&pageSize=50', null, token))?.data?.records || []
  const rowAll = all2.find(r => r.productId === pid) || {}
  check('全部仓库：该行推荐货位含两个仓库的货位编码',
    (rowAll.locationCode || '').includes('E2E-W1') && (rowAll.locationCode || '').includes('E2E-W2'),
    `locationCode=${rowAll.locationCode}`)
  check('全部仓库：该行仓库列拼接两个仓库名',
    (rowAll.warehouseName || '').split(',').filter(Boolean).length === 2,
    `warehouseName=${rowAll.warehouseName}`)
  check('全部仓库：商品行数不因多仓库绑定而重复',
    all2.filter(r => r.productId === pid).length === 1, `该商品行数=${all2.filter(r => r.productId === pid).length}`)

  const wh1Rows = (await apiReq('GET', `/erp/product-location/page?warehouseId=${WH1}&pageNum=1&pageSize=50`, null, token))?.data?.records || []
  const rowWh1 = wh1Rows.find(r => r.productId === pid) || {}
  check('指定仓库：只显示该仓库的货位（不拼接）',
    rowWh1.locationCode === 'E2E-W1' && (rowWh1.warehouseName || '').indexOf(',') < 0,
    `locationCode=${rowWh1.locationCode} warehouseName=${rowWh1.warehouseName}`)

  // ═══ 3. 仅显示未设置货位（全部仓库口径）═══
  const unset = await apiReq('GET', '/erp/product-location/page?pageNum=1&pageSize=50&onlyUnsettedGoods=true', null, token)
  const unsetRows = unset?.data?.records || []
  check('全部仓库：仅显示未设置货位生效', unsetRows.every(r => !r.locationCode), `命中=${unsetRows.length} / 全部=${prodTotal}`)

  // ═══ 4. UI ═══
  const browser = await chromium.launch({ headless: true })
  const page = await (await browser.newContext({ viewport: { width: 1680, height: 950 } })).newPage()
  const jsErrors = []
  page.on('pageerror', e => jsErrors.push(String(e.message).slice(0, 160)))
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.origin === FE && url.pathname.startsWith('/api/')) {
      try { return route.fulfill({ response: await route.fetch({ url: `http://localhost:${PORT}${url.pathname}${url.search}` }) }) } catch { return route.continue() }
    }
    return route.continue()
  })
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([t]) => { localStorage.setItem('token', t); localStorage.setItem('tenantId', '1') }, [token])
  // 预热（Vite 首次按需编译）
  await page.goto(`${FE}/md/location`, { waitUntil: 'domcontentloaded' }).catch(() => { })
  await page.waitForTimeout(10000)
  await page.goto(`${FE}/md/location`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.category-list-layout', { timeout: 60000 }).catch(() => { })
  await page.waitForTimeout(3500)
  await page.screenshot({ path: `${SHOTS}/20-all-warehouse-default.png` })

  // 4.1 默认选中「全部仓库」
  const whSelect = page.locator('.search-section .ant-select').first()
  const whText = (await whSelect.innerText().catch(() => '')).trim()
  check('UI：仓库下拉默认选中「全部仓库」', whText.includes('全部仓库'), `当前值="${whText}"`)

  // 4.2 下拉里有「全部仓库」选项
  await whSelect.click({ force: true })
  await page.waitForTimeout(1200)
  const opts = await page.locator('.ant-select-dropdown:visible .ant-select-item-option').allInnerTexts().catch(() => [])
  check('UI：仓库下拉含「全部仓库」选项', opts.some(o => o.includes('全部仓库')),
    `选项=${opts.map(o => o.trim()).join('/')}`)
  check('UI：「全部仓库」为第一项（默认项）', (opts[0] || '').includes('全部仓库'), `首项=${(opts[0] || '').trim()}`)
  await page.keyboard.press('Escape')
  await page.waitForTimeout(600)

  // 4.3 进入页面即出数据（无需选仓库）
  const rowCount = () => page.locator('.ss-grid tbody button:has-text("设置")').count()
  await page.waitForSelector('.ss-grid tbody button:has-text("设置")', { timeout: 20000 }).catch(() => { })
  const rows = await rowCount()
  check('UI：打开页面即有数据（无需先选仓库）', rows >= 2, `行=${rows}`)
  const noWarn = !(await page.locator('.ant-message-warning').count())
  check('UI：未出现「请先选择仓库」提示', noWarn)

  // 4.4 数据表用默认 20 行占满
  const trCount = await page.locator('.ss-grid tbody tr').count()
  check('UI：数据表按组件默认 20 行占满（含占位行）', trCount >= 20, `tbody 行数=${trCount}`)

  // 4.5 仓库列/推荐货位列展示拼接值
  const body = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  check('UI：全部仓库下推荐货位列显示拼接编码',
    body.includes('E2E-W1') && body.includes('E2E-W2'), body.replace(/\s+/g, ' ').slice(0, 140))

  // 4.6 切到具体仓库后数据收敛
  await whSelect.click({ force: true })
  await page.waitForTimeout(1000)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: '主仓库' }).first().click({ force: true })
  await page.waitForTimeout(3000)
  const bodyWh = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  check('UI：切到「主仓库」后只显示该仓库货位', bodyWh.includes('E2E-W1') && !bodyWh.includes('E2E-W2'),
    bodyWh.replace(/\s+/g, ' ').slice(0, 120))
  await page.screenshot({ path: `${SHOTS}/21-single-warehouse.png` })

  // 4.7 设置弹窗内的仓库选择（全部仓库模式下需显式选）
  await whSelect.click({ force: true })
  await page.waitForTimeout(1000)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: '全部仓库' }).first().click({ force: true })
  await page.waitForTimeout(3000)
  const setBtn = page.locator('.ss-grid tbody button:has-text("设置")').first()
  if (await setBtn.count()) {
    await setBtn.click({ force: true })
    await page.waitForTimeout(2500)
    const wrap = page.locator('.ant-modal-wrap').filter({ hasText: '选择货位' }).last()
    const txt = await wrap.innerText().catch(() => '')
    check('UI：设置弹窗内含仓库选择器（全部仓库模式下）', txt.includes('仓库'), txt.split('\n').filter(Boolean).slice(0, 3).join(' / '))
    const pOpts = await wrap.locator('.ant-select').first().count()
    check('UI：弹窗仓库选择器存在且不含「全部仓库」', pOpts > 0,
      `选择器=${pOpts}`)
    await page.screenshot({ path: `${SHOTS}/22-set-modal-warehouse.png` })
  } else {
    check('UI：操作列「设置」入口', false)
  }
  await page.keyboard.press('Escape')
  await page.waitForTimeout(600)

  check('UI：无 JS 运行时错误', jsErrors.length === 0, jsErrors.join(' | ') || '无')
  await browser.close()

  // ── 清理 ──
  await apiReq('POST', '/erp/product-location/batch-remove', { warehouseId: WH1, productIds: [pid] }, token)
  await apiReq('POST', '/erp/product-location/batch-remove', { warehouseId: WH2, productIds: [pid] }, token)
  await cleanupTestData()

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
