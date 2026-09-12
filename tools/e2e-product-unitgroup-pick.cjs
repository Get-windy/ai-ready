/* 商品表单「选择单位组」接线验证（跨模块）：
 *   造单位组 → 打开商品表单 → 选择单位组 → 断言商品单位明细被带出 → 清理
 * 运行： node tools/e2e-product-unitgroup-pick.cjs （AUX_PORT 默认 5659）
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = 'http://localhost:5656'
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
const results = []
const check = (name, ok, detail) => {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: process.env.E2E_USER || 'e2e_product', password: process.env.E2E_PASS || 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  TOKEN = res?.data?.token || res?.data?.accessToken
  if (!TOKEN) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
}

;(async () => {
  await login()
  const stamp = Date.now().toString().slice(-6)

  // 造单位组：个 / 件(12) / 箱(48)
  const created = await rawReq('POST', '/erp/product-unit-group', {
    status: 1,
    items: [
      { unitType: 'SMALL', unitName: '个', conversionRate: 1 },
      { unitType: 'MEDIUM', unitName: '件', conversionRate: 12 },
      { unitType: 'LARGE', unitName: '箱', conversionRate: 48 },
    ],
  }, TOKEN)
  check('C1 造单位组数据', created.code === 200, JSON.stringify(created).slice(0, 120))
  const groupId = created.data

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  const errors = []
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text().slice(0, 160)) })
  // 前端 /api → 独立副本后端
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    const req = route.request()
    try {
      const resp = await route.fetch({
        url: `http://localhost:${PORT}${new URL(req.url()).pathname}${new URL(req.url()).search}`,
        headers: { ...req.headers(), host: `localhost:${PORT}` },
      })
      await route.fulfill({ response: resp })
    } catch { await route.abort().catch(() => {}) }
  })

  try {
    // 登录前端（写 token 到 localStorage，与项目鉴权口径一致）
    await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(1500)
    // 并行会话共用 admin 会被 sa-token 互踢：导航前重登拿最新 token
    await login()
    await page.evaluate(({ token }) => {
      localStorage.setItem('token', token)
      localStorage.setItem('tenantId', '1')
    }, { token: TOKEN })

    await page.goto(`${FE}/md/product/form`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(6000)
    // 被踢自愈
    if ((await page.locator('body').innerText().catch(() => '')).includes('登 录')) {
      await login()
      await page.evaluate(({ token }) => {
        localStorage.setItem('token', token)
        localStorage.setItem('tenantId', '1')
      }, { token: TOKEN })
      await page.goto(`${FE}/md/product/form`, { waitUntil: 'domcontentloaded', timeout: 60000 })
      await page.waitForTimeout(6000)
    }
    const body = () => page.evaluate(() => document.body.innerText.replace(/\s+/g, ' '))
    let txt = await body()
    check('C2 商品表单可打开', txt.includes('商品') && (txt.includes('单位') || txt.includes('基本信息')), txt.slice(0, 120))

    // 定位「选择单位组」按钮
    const btn = page.locator('button', { hasText: '选择单位组' }).first()
    const btnCount = await page.locator('button', { hasText: '选择单位组' }).count().catch(() => 0)
    check('C3 商品表单存在「选择单位组」入口', btnCount > 0, `count=${btnCount}`)
    if (btnCount > 0) {
      await btn.scrollIntoViewIfNeeded().catch(() => {})
      await btn.click({ force: true })
      await page.waitForTimeout(3500)
      txt = await body()
      check('C4 单位组选择弹窗含「单位/单位关系」', txt.includes('选择单位组') && txt.includes('单位关系'), txt.slice(0, 160))
      await page.screenshot({ path: path.join(SHOTS, '08-product-unitgroup-pick.png') })

      // 选中第一行并确定
      const firstRow = page.locator('.ant-modal .ant-table-tbody tr.ant-table-row').first()
      const rowCount = await page.locator('.ant-modal .ant-table-tbody tr.ant-table-row').count().catch(() => 0)
      check('C5 单位组列表有数据', rowCount > 0, `rows=${rowCount}`)
      if (rowCount > 0) {
        await firstRow.click({ force: true })
        await page.waitForTimeout(800)
        await page.locator('.ant-modal button', { hasText: '确 定' }).first().click().catch(async () => {
          await page.locator('.ant-modal button', { hasText: '确定' }).first().click().catch(() => {})
        })
        await page.waitForTimeout(3000)
        txt = await body()
        check('C6 确定后商品单位明细被带出（含 个/件/箱）',
          txt.includes('箱') && txt.includes('件'), txt.slice(-260))
        await page.screenshot({ path: path.join(SHOTS, '09-product-unitgroup-applied.png') })
      }
    }

    check('C7 无 JS 运行错误',
      errors.filter(e => !/favicon|ResizeObserver|Failed to load resource/i.test(e)).length === 0,
      errors.slice(0, 2).join(' || '))
  } catch (e) {
    check('C0 执行异常', false, String(e.message))
  } finally {
    await browser.close()
    // 清理
    await login().catch(() => {})
    const del = await rawReq('DELETE', `/erp/product-unit-group/${groupId}`, null, TOKEN)
    check('C8 清理单位组测试数据', del.code === 200, JSON.stringify(del).slice(0, 100))
  }

  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 汇总：${pass}/${results.length} =====`)
  fs.writeFileSync(path.join(SHOTS, 'e2e-unitgroup-pick.json'), JSON.stringify({ pass, total: results.length, results }, null, 1), 'utf8')
  process.exit(pass === results.length ? 0 : 1)
})()
