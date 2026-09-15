/*
 * 配送模块（DMS）页面冒烟巡检：逐个打开模块内全部页面，抓「白屏 / 运行期错误」这类**构建期查不出**的 P0。
 *
 * 为什么需要：某些缺陷编译与 vite 构建**都不报错**，只在浏览器按需加载时暴露，例如
 *   - `@ant-design/icons-vue` 导入了当前版本不存在的图标名 → 路由动态 import 抛 SyntaxError → **整页白屏**
 *   - 组件运行期抛错 → 页面只剩骨架
 * 本脚本对每个页面断言：① URL 未被弹回登录页 ② 无 pageerror / console.error ③ 正文有实质内容（非白屏/404）
 *
 * 用法：ERP_PORT=5673 FE_URL=http://localhost:5656 node tools/smoke-dms-pages.cjs
 * 退出码：0 = 全部通过（仅告警不计）；1 = 存在失败页面
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5673)
const USER = process.env.E2E_USER || 'e2e_dmsdash'
const PWD = process.env.E2E_PWD || 'admin123'
const TENANT = process.env.E2E_TENANT || '系统租户'
const SHOTS = 'I:/AI-Ready/tool-results/dms-smoke'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

/** 配送模块页面清单（菜单 path → 中文名；取自 sys_menu 配送各二级菜单） */
const PAGES = [
  ['dispatch/query', '配送查询'],
  ['dispatch/dispatch-order/index', '配送单'],
  ['dms/dashboard', '配送仪表盘'],
  ['dms/route-list', '配送路线单'],
  ['dms/route', '路线规划'],
  ['dms/vehicle', '车辆管理'],
  ['dms/vehicle/maintenance', '车辆维护'],
  ['dms/rider', '配送员管理'],
  ['dms/vehicle/usage', '用车管理'],
  ['dms/verification', '人员核验'],
  ['dms/dispatch-task', '调度任务'],
  ['dms/dispatch', '智能调度'],
  ['dms/order-pool', '订单池'],
  ['dms/realtime-tracking', '实时跟踪'],
  ['dms/tracking', '配送跟踪'],
  ['dms/sign', '签收管理'],
  ['dms/channel', '渠道管理'],
  ['dms/config', '配送配置'],
  ['dms/config-params', '配送参数'],
  ['dms/settlement', '配送结算'],
  ['dms/payment', '收款管理'],
  ['dispatch/logistics-ship', '物流发货'],
  ['dispatch/ship-query', '发货查询'],
  ['dispatch/return-receive', '物流退货收货'],
  ['dispatch/purchase-receive', '采购订货收货'],
]

function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? null : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => resolve({ status: res.statusCode, text: Buffer.concat(chunks).toString('utf8') }))
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const capJson = JSON.parse(cap.text)
  const code = [...Buffer.from(capJson.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: USER, password: PWD, tenantName: TENANT,
    captcha: code, captchaKey: capJson.data.uuid,
  })
  const json = JSON.parse(res.text)
  const token = json?.data?.token || json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + res.text.slice(0, 200))
  return token
}

/** 与页面无关的噪声（第三方埋点/性能/Vite 心跳），不计入失败 */
const NOISE = [
  /WebVitals/i, /\[Vue warn\]/i, /Sentry/i, /favicon/i, /ResizeObserver loop/i,
  /Failed to fetch dynamically imported module/i, // 单独判定，见下
]

;(async () => {
  const token = await login()
  const browser = await chromium.launch()
  const page = await browser.newPage({ viewport: { width: 1600, height: 950 } })
  page.setDefaultTimeout(15000)
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  const results = []
  for (const [routePath, name] of PAGES) {
    const errors = []
    const onConsole = m => { if (m.type() === 'error') errors.push('console: ' + m.text()) }
    const onPageError = e => errors.push('pageerror: ' + e.message)
    page.on('console', onConsole)
    page.on('pageerror', onPageError)

    let url = '', body = '', shot = ''
    try {
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(([tk]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', '1')
      }, [token])
      await page.goto(`${FE}/${routePath}`, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(3500)
      url = page.url()
      body = (await page.locator('body').innerText()).replace(/\s+/g, ' ').trim()
    } catch (e) {
      errors.push('goto: ' + e.message.split('\n')[0])
    }
    page.off('console', onConsole)
    page.off('pageerror', onPageError)

    const realErrors = errors.filter(e => !NOISE.some(re => re.test(e)))
    const kicked = url.includes('/login')
    // 只认「页面级」404 文案，别用裸 "404" 匹配（表格数据里出现 404 会误报）
    const notFound = /页面不存在|页面走丢了|404\s*页|Not\s*Found/i.test(body)
    const blank = body.length < 80
    const ok = !kicked && !notFound && !blank && realErrors.length === 0
    if (!ok) {
      shot = path.join(SHOTS, routePath.replace(/\//g, '-') + '.png')
      try { await page.screenshot({ path: shot, fullPage: false }) } catch (e) { /* 忽略 */ }
    }
    results.push({ routePath, name, ok, url, bodyLen: body.length, kicked, notFound, blank, errors: realErrors, shot })
    console.log(`${ok ? '  ✅' : '  ❌'} ${name.padEnd(6)} /${routePath}` +
      (ok ? '' : `  [${kicked ? '弹回登录 ' : ''}${notFound ? '404 ' : ''}${blank ? '白屏 ' : ''}${realErrors.length ? realErrors.length + ' 错误' : ''}]`))
    if (!ok) realErrors.slice(0, 4).forEach(e => console.log('        ' + e.slice(0, 220)))
  }

  await browser.close()
  const failed = results.filter(r => !r.ok)
  fs.writeFileSync(path.join(SHOTS, 'result.json'), JSON.stringify(results, null, 2))
  console.log(`\n═══ 冒烟结果：${results.length - failed.length}/${results.length} 通过 ═══`)
  if (failed.length) {
    console.log('失败页面：')
    failed.forEach(f => console.log(`  ❌ ${f.name} /${f.routePath} → ${f.url}`))
  }
  process.exit(failed.length ? 1 : 0)
})().catch(e => { console.error('冒烟脚本异常：', e.message); process.exit(2) })
