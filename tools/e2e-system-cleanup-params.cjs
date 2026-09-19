/*
 * 系统模块 · 清理规则(62305) / 平台参数(62501) 金标准改版运行时验证
 *
 * 覆盖：外壳（ErrorBoundary + CategoryListLayout 五插槽）、BillDetailTable 列配置齿轮、
 *       经典分页栏、查询区、诚实性标注（a-alert）、行内操作、无 JS 错误、无 4xx/5xx。
 *
 * 运行： node tools/e2e-system-cleanup-params.cjs
 * 依赖： 后端 5655、前端 5656 均在运行（共享 dev 环境）
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE_URL || 'http://localhost:5656'
const API_PORT = Number(process.env.API_PORT || 5655)
const SHOTS = 'I:/AI-Ready/tool-results/system-module'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

let TOKEN = null
let PASS = 0
let FAIL = 0
const FAILURES = []

function rawReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: API_PORT, path: '/api' + path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', tenantId: '1' } : {}),
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => { try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))) } catch { resolve({}) } })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function check(name, cond, detail) {
  const d = detail === undefined ? '' : (typeof detail === 'string' ? detail : JSON.stringify(detail))
  if (cond) { PASS++; console.log('  ✓', name) }
  else { FAIL++; FAILURES.push(name + (d ? ' → ' + d.slice(0, 200) : '')); console.log('  ✗', name, d.slice(0, 200)) }
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid,
  })
  TOKEN = res?.data?.token || res?.data?.accessToken
  if (!TOKEN) throw new Error('登录失败')
}

const PAGES = [
  {
    name: '清理规则(62305)', url: '/admin/data/cleanup', expectTitle: '清理规则',
    expectHeaders: ['操作', '规则名称', '数据表', '条件列', '保留天数', '执行周期', '状态', '更新时间'],
    searchLabels: ['规则名称', '目标表', '状态'],
    alertText: '不会删除任何数据',
    storageKey: 'system-data-cleanup-table-columns',
    pageConfigKey: 'system-data-cleanup-page-config',
  },
  {
    name: '平台参数(62501)', url: '/admin/platform/params', expectTitle: '平台参数',
    expectHeaders: ['操作', '参数名称', '参数键', '参数值', '类型', '分组', '内置', '更新时间'],
    searchLabels: ['参数键', '分组'],
    alertText: '真实读写数据库',
    storageKey: 'system-platform-params-table-columns',
    pageConfigKey: 'system-platform-params-page-config',
  },
]

async function main() {
  await login()
  console.log('登录成功\n=== 系统模块 · 清理规则 / 平台参数 运行时验证 ===')

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const p = await ctx.newPage()

  const errors = []
  const consoleErrors = []
  const badResponses = []
  p.on('pageerror', e => errors.push(String(e)))
  p.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()) })
  p.on('response', r => {
    const u = new URL(r.url())
    if (u.pathname.startsWith('/api/') && r.status() >= 400) badResponses.push(`${r.status()} ${u.pathname}${u.search}`)
  })

  await p.route(url => new URL(url).pathname.startsWith('/api/'), route => {
    const u = new URL(route.request().url())
    return route.continue({ url: `http://localhost:${API_PORT}` + u.pathname + u.search })
  })

  await p.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await p.evaluate(([tk]) => localStorage.setItem('token', tk), [TOKEN])

  /** 共享环境 sa-token 会被并行会话互踢 → 401 跳登录时自愈重登并重试 */
  async function gotoWithSelfHeal(url, tries = 3) {
    for (let i = 0; i < tries; i++) {
      await p.goto(`${FE}${url}`, { waitUntil: 'domcontentloaded' })
      await p.waitForTimeout(4000)
      if (!/\/login/.test(p.url())) return true
      await login()
      await p.evaluate(([tk]) => localStorage.setItem('token', tk), [TOKEN])
    }
    return false
  }

  const reqLog = []
  p.on('request', r => {
    const u = new URL(r.url())
    if (u.pathname.startsWith('/api/')) reqLog.push(u.pathname + u.search)
  })

  for (const page of PAGES) {
    console.log(`\n── ${page.name} (${page.url}) ──`)
    const errBefore = errors.length
    const consBefore = consoleErrors.length
    const badBefore = badResponses.length

    await gotoWithSelfHeal(page.url)
    await p.evaluate(([tk]) => { if (!localStorage.getItem('token')) localStorage.setItem('token', tk) }, [TOKEN])
    await p.waitForTimeout(1500)
    await p.screenshot({ path: `${SHOTS}/gold-${page.url.replace(/\//g, '_')}.png`, fullPage: false })

    const text = (await p.evaluate(() => document.body.innerText || '')).replace(/\s+/g, '')
    const info = await p.evaluate(() => {
      const ths = [...document.querySelectorAll('.ss-grid thead th')].map(t => (t.innerText || '').trim())
      return {
        headers: ths,
        rows: document.querySelectorAll('.ss-grid tbody tr').length,
        hasClassicPagination: !!document.querySelector('.classic-pagination'),
        hasGear: !!document.querySelector('.ss-grid thead th[data-col-key="rowNo"]'),
        searchLabels: [...document.querySelectorAll('.search-label')].map(e => (e.innerText || '').trim()),
        hasAlert: !!document.querySelector('.table-alert'),
        hasErrorBoundary: !!document.querySelector('.page-container'),
      }
    })

    check(`${page.name}：页面可打开且标题正确`, text.includes(page.expectTitle), { url: p.url() })
    check(`${page.name}：常驻 a-alert 诚实性标注`, info.hasAlert && text.includes(page.alertText), { hasAlert: info.hasAlert })
    check(`${page.name}：查询区为横向 search-row`, info.searchLabels.length >= 2, info.searchLabels)
    check(`${page.name}：查询项齐备`, page.searchLabels.every(l => info.searchLabels.includes(l)), info.searchLabels)
    check(`${page.name}：表头含 rowNo 齿轮列`, info.hasGear, info.headers)
    check(`${page.name}：表头列齐备`,
      page.expectHeaders.every(h => info.headers.includes(h)), { got: info.headers, want: page.expectHeaders })
    check(`${page.name}：经典分页栏渲染`, info.hasClassicPagination)
    check(`${page.name}：表格渲染（空表也有占位行）`, info.rows > 0, info.rows)

    // 表头齿轮可打开列配置面板（个人/全局两个 Tab = storage-key / global-config-key 接线）
    const gearOpened = await p.evaluate(async () => {
      const btn = document.querySelector('.ss-grid thead th[data-col-key="rowNo"] .th-settings-btn')
      if (!btn) return { opened: false }
      btn.click()
      await new Promise(r => setTimeout(r, 800))
      const modal = [...document.querySelectorAll('.ant-modal-title')].find(e => (e.innerText || '').includes('配置'))
      const text = document.body.innerText || ''
      return { opened: !!modal, hasPersonal: text.includes('个人配置'), hasGlobal: text.includes('全局配置') }
    })
    check(`${page.name}：表头齿轮可打开列配置面板（个人/全局）`,
      gearOpened.opened && gearOpened.hasPersonal && gearOpened.hasGlobal, gearOpened)
    await p.keyboard.press('Escape')
    await p.waitForTimeout(500)

    // ═══ 页面级功能断言 ═══
    if (page.url === '/admin/platform/params') {
      check(`${page.name}：渲染真实库数据（含「内置」标签）`, text.includes('内置'), { rows: info.rows })
      const total0 = await p.evaluate(() => (document.querySelector('.total-box')?.innerText || ''))
      reqLog.length = 0
      await p.fill('input[placeholder*="参数键"]', 'system.')
      // antd 会在两个汉字间插空格（「查 询」），故按第一个按钮定位（查询在重置之前）
      await p.locator('.search-row button').first().click()
      await p.waitForTimeout(2500)
      const filtered = reqLog.filter(u => u.includes('/config/page'))
      check(`${page.name}：参数键查询下发到后端（/config/page?configKey=）`,
        filtered.some(u => u.includes('configKey=system.')), filtered)
      const total1 = await p.evaluate(() => (document.querySelector('.total-box')?.innerText || ''))
      check(`${page.name}：查询后条数变化（后端真过滤）`, total1 !== total0, { total0, total1 })
    }
    if (page.url === '/admin/data/cleanup') {
      // 后端表 0 行 → 页面必须如实显示空态，不得有假数据
      const total0 = await p.evaluate(() => (document.querySelector('.total-box')?.innerText || ''))
      check(`${page.name}：无假数据（如实显示 0 条记录）`, /共\s*0\s*条记录/.test(total0), total0)
      check(`${page.name}：列表请求真实发出`, reqLog.some(u => u.includes('/data-source/cleanup/list')), reqLog.slice(0, 5))
    }

    check(`${page.name}：无新增 JS 错误（pageerror）`, errors.length === errBefore, errors.slice(errBefore).join(' | '))
    const cons = consoleErrors.slice(consBefore)
    check(`${page.name}：无 console.error`, cons.length === 0, cons.join(' | ').slice(0, 200))
    const bad = badResponses.slice(badBefore)
    check(`${page.name}：接口无 4xx/5xx`, bad.length === 0, bad)
  }

  // ═══ 平台参数：新增 → 回读 → 删除 → 回读（UI 写路径闭环，跑完必清理） ═══
  const tempKey = `e2e.gold.probe.${Date.now()}`
  console.log(`\n── 平台参数写路径闭环（临时键 ${tempKey}） ──`)
  try {
    await gotoWithSelfHeal('/admin/platform/params')
    await p.locator('.toolbar-left button').first().click() // 新增参数
    await p.waitForTimeout(800)
    await p.fill('input[placeholder*="system.version"]', tempKey)
    await p.fill('input[placeholder="请输入参数名称"]', 'E2E临时参数')
    await p.fill('textarea[placeholder="请输入参数值"]', 'v1')
    await p.locator('.ant-modal-footer button').last().click() // 确定
    await p.waitForTimeout(2500)
    const created = await rawReq('GET', `/config/page?configKey=${encodeURIComponent(tempKey)}&pageNum=1&pageSize=10`, null, TOKEN)
    check('新增参数：提交后落库且回读可见', (created?.records || []).some(r => r.configKey === tempKey), created)
    check('新增参数：UI 提示成功', (await p.evaluate(() => document.body.innerText)).includes('参数已新增'))
  } finally {
    // 无论成败都按临时键清理，保证共享库无残留
    await rawReq('DELETE', `/config/${encodeURIComponent(tempKey)}`, null, TOKEN)
    const after = await rawReq('GET', `/config/page?configKey=${encodeURIComponent(tempKey)}&pageNum=1&pageSize=10`, null, TOKEN)
    check('临时参数已清理（共享库无残留）', (after?.records || []).length === 0, after)
  }

  await browser.close()
  console.log(`\n结果：${PASS} 通过 / ${FAIL} 失败`)
  if (FAILURES.length) {
    console.log('失败项：')
    FAILURES.forEach(f => console.log(' -', f))
    process.exit(1)
  }
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
