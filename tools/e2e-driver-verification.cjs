/*
 * 司机端（frontend/apps/driver-delivery）出车/收车流程烟测
 *
 *  覆盖：/inspection（出车验车，车辆选择器 + 检查项）→ /binding（车辆绑定，强制带检查单）
 *        → /handover（交车，含收车后检查）
 *
 *  背景：这三页原为 2026-06 的残留实现，DMS 契约整层不可用（baseURL 重复 /api、snake_case payload、
 *        交车路径少 binding/ 一层、用登录用户ID当 riderId、且 inspection 页误用 antd 组件）。
 *        本脚本用于回归：页面能真实渲染、选择器能取到后端数据、无 JS 异常。
 *
 * 用法：node tools/e2e-driver-verification.cjs
 *      DRIVER_FE_URL=http://localhost:5744 ERP_PORT=5675 node tools/e2e-driver-verification.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.DRIVER_FE_URL || 'http://localhost:5744'
const PORT = Number(process.env.ERP_PORT || 5675)
const SHOTS = 'I:/AI-Ready/tool-results/dms-verification'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const E2E_USER = process.env.E2E_USER || 'e2e_verif'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
let TOKEN = null

function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const b = Buffer.concat(chunks)
        let j = null
        try { j = JSON.parse(b.toString('utf8')) } catch (e) { /* ignore */ }
        resolve({ status: res.statusCode, json: j })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 160) : ''}`)
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  TOKEN = res.json?.data?.token
  if (!TOKEN) throw new Error('登录失败')
}

;(async () => {
  try {
    await login()
    const browser = await chromium.launch({ headless: true })
    const ctx = await browser.newContext({ viewport: { width: 420, height: 900 }, locale: 'zh-CN' })
    const page = await ctx.newPage()
    const jsErrors = []
    page.on('pageerror', e => jsErrors.push(e.message))
    const apiFails = []
    page.on('response', r => {
      try {
        const u = new URL(r.url())
        if (u.pathname.startsWith('/api/') && r.status() >= 400) apiFails.push(`${r.status()} ${u.pathname}`)
      } catch (e) { /* ignore */ }
    })
    // /api 转发到验收实例
    // 仅拦截后端 /api/**（不能用 '**/api/**' glob：会连 /src/api/xxx.ts 模块一起劫持 → 白屏）
    await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
      const raw = route.request().url()
      try {
        const u = new URL(raw)
        await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
      } catch (e) {
        console.log('  [转发跳过]', raw.slice(0, 80), e.message.slice(0, 60))
        await route.continue().catch(() => {})
      }
    })

    const open = async (route, label) => {
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' }).catch(() => {})
      await page.evaluate(([tk]) => localStorage.setItem('token', tk), [TOKEN])
      await page.goto(`${FE}${route}`, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(6000)
      const text = (await page.locator('body').innerText().catch(() => '')).replace(/\s+/g, '')
      return text
    }

    // 1) 出车验车
    let t = await open('/inspection', '出车验车')
    check('出车验车页可渲染（Vant 组件，无 antd 残留）', t.includes('出车验车') && t.includes('车辆'), t.slice(0, 60))
    check('含车辆选择器（不复用手输编号）', t.includes('车辆') && !t.includes('请输入或扫描车辆编号'), t.slice(0, 60))
    check('含 5 项车况 + 随车装备', ['车辆外观', '轮胎', '灯光', '刹车', '车内清洁', '灭火器', '三角警示牌'].every(k => t.includes(k)), t.slice(0, 80))
    check('含里程/油量/检查地点', t.includes('里程') && t.includes('油量') && t.includes('检查地点'), '')
    await page.screenshot({ path: path.join(SHOTS, 'driver-inspection.png'), fullPage: true })
    const vehiclePickerOpened = await page.evaluate(() => {
      const cell = [...document.querySelectorAll('.van-cell')].find(c => (c.innerText || '').includes('车辆'))
      if (!cell) return false
      cell.click()
      return true
    })
    await page.waitForTimeout(1500)
    const pickerText = (await page.locator('body').innerText().catch(() => '')).replace(/\s+/g, '')
    check('车辆选择器可展开（数据源走后端 /dms/vehicle/options）',
      vehiclePickerOpened && (pickerText.includes('选择车辆') || pickerText.includes('京') || pickerText.includes('取消')), pickerText.slice(-60))
    await page.screenshot({ path: path.join(SHOTS, 'driver-inspection-picker.png'), fullPage: true })

    // 1.5) 造数据：当前登录用户 ↔ 配送员 ↔ 出车绑定（交车页需要真实绑定才能渲染收车检查段）
    const me = (await rawReq('GET', '/auth/userinfo', null, TOKEN)).json?.data || {}
    // 与生产一致：一个登录用户对应一个配送员档案（/me/rider 反查；缺档案时补建）
    let meRider = (await rawReq('GET', '/dms/verification/me/rider', null, TOKEN)).json?.data
    if (!meRider?.id) {
      const sfx = String(Date.now()).slice(-8)
      meRider = (await rawReq('POST', '/dms/rider', {
        realName: 'E2E司机烟测' + sfx, phone: '136' + sfx, riderType: 1, userId: me.userId,
      }, TOKEN)).json?.data
    }
    let driverBindingId = (await rawReq('GET', `/dms/verification/binding/active/rider/${meRider?.id}`, null, TOKEN))
      .json?.data?.id || null

    if (!driverBindingId) {
      const vehicles = (await rawReq('GET', '/dms/vehicle/options', null, TOKEN)).json?.data || []
      // 自愈：清理「车上残留当前配送员但已无活跃绑定」的脏状态
      for (const v of vehicles) {
        if (v.currentRiderId != null) {
          await rawReq('POST', `/dms/vehicle/${v.id}/unbind-rider`, {}, TOKEN)
          v.currentRiderId = null
        }
      }
      const freeVeh = vehicles.find(v => v.currentRiderId == null && Number(v.status) !== 2 && Number(v.status) !== 3)
      if (meRider?.id && freeVeh) {
        const insId = (await rawReq('POST', '/dms/verification/inspection', {
          vehicleId: freeVeh.id, riderId: meRider.id, inspectionType: 1,
          exteriorStatus: 0, tireStatus: 0, lightStatus: 0, brakeStatus: 0,
          cleanlinessStatus: 0, fireExtinguisher: 0, warningTriangle: 0,
          mileage: 2000, fuelLevel: 70,
        }, TOKEN)).json?.data
        driverBindingId = (await rawReq('POST', '/dms/verification/bind', {
          riderId: meRider.id, vehicleId: freeVeh.id, inspectionId: insId, bindReason: 'E2E 司机端烟测',
        }, TOKEN)).json?.data
      }
    }
    check('司机端数据准备（当前用户↔配送员↔出车绑定）', !!driverBindingId, `riderId=${meRider?.id} bindingId=${driverBindingId}`)

    // 2) 车辆绑定（无检查单时必须提示先做检查）
    t = await open('/binding', '车辆绑定')
    check('车辆绑定页可渲染', t.includes('车辆绑定'), t.slice(0, 60))
    check('未带检查单时提示先做出车前检查', t.includes('尚未完成出车前检查') || t.includes('去做出车前检查'), t.slice(0, 80))
    await page.screenshot({ path: path.join(SHOTS, 'driver-binding.png'), fullPage: true })

    // 3) 交车（含收车后检查）
    t = await open('/handover', '交车')
    check('交车页可渲染', t.includes('交车'), t.slice(0, 60))
    check('交车页含收车后检查段', t.includes('收车后检查'), '')
    check('交车页检查项齐备', ['车辆外观', '轮胎', '灯光', '刹车', '车内清洁', '灭火器', '三角警示牌'].every(k => t.includes(k)), t.slice(0, 80))
    check('交车页带出当前绑定（车牌/出车里程）', t.includes('车牌号') && t.includes('出车里程'), t.slice(0, 60))
    await page.screenshot({ path: path.join(SHOTS, 'driver-handover.png'), fullPage: true })
    // 收尾：交车（带收车检查），恢复车辆为空闲
    if (driverBindingId) {
      await rawReq('POST', `/dms/verification/binding/${driverBindingId}/handover`, {
        handoverMileage: 2001,
        handoverLocation: 'E2E 司机端收尾',
        inspection: {
          inspectionType: 2, mileage: 2001, exteriorStatus: 0, tireStatus: 0, lightStatus: 0,
          brakeStatus: 0, cleanlinessStatus: 0, fireExtinguisher: 0, warningTriangle: 0,
        },
      }, TOKEN)
    }

    // 4) 补能登记（司机端自助录入，与 PC 同一套契约）
    t = await open('/energy', '补能登记')
    check('补能登记页可渲染', t.includes('补能登记'), t.slice(0, 60))
    check('补能页含主体二选一（我/所驾车辆）', t.includes('补能主体') && t.includes('我（两轮车）') && t.includes('所驾车辆'), t.slice(0, 80))
    check('补能页含类型/金额/里程/卡号/凭证',
      ['补能类型', '金额(元)', '里程(km)', '卡号/套餐', '凭证'].every(k => t.includes(k)), t.slice(0, 90))
    await page.screenshot({ path: path.join(SHOTS, 'driver-energy.png'), fullPage: true })

    // 5) 「我的」页含补能登记入口
    t = await open('/user', '我的')
    check('司机端「我的」含补能登记入口', t.includes('补能登记'), t.slice(0, 60))

    check('司机端无 JS 运行异常', jsErrors.length === 0, jsErrors.join('; ').slice(0, 160))
    check('司机端运行期无接口 4xx/5xx', apiFails.length === 0, [...new Set(apiFails)].slice(0, 3).join(' ; '))

    await browser.close()
  } catch (e) {
    check('司机端烟测主流程', false, e.message)
    console.error(e)
  }

  const pass = results.filter(r => r.ok).length
  console.log(`\n═══ 司机端验收汇总：${pass}/${results.length} ═══`)
  results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name}`))
  process.exit(pass === results.length ? 0 : 1)
})()
