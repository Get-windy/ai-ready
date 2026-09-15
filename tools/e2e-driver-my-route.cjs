/*
 * 司机端「我的配送路线」端到端验收（配送路线单能力在司机端的落点）
 *
 * 覆盖：
 *   1) 司机档案绑定 → 「我的配送路线」读取本人路线（待出发 + 进行中）
 *   2) 点位卡片渲染（客户/电话/地址/ETA/状态）
 *   3) 司机逐点回写：到达 / 送达（必填签收人）/ 失败（必填原因）
 *   4) 重新规划 / 刷新 ETA 按钮可用；无 JS 运行时错误
 *
 * 用法（依赖后端与司机端 dev server 在跑）：
 *   DRIVER_URL=http://localhost:3004 ERP_PORT=5671 node tools/e2e-driver-my-route.cjs
 *
 * 说明：司机身份由 `dms_rider.user_id` 关联登录账号（见 VerificationService.currentRider），
 *       故本脚本会先把验收账号绑定到一名配送员，验收结束后解绑（仅动 user_id 一列）。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const { execFileSync } = require('child_process')

const DRIVER_URL = process.env.DRIVER_URL || 'http://localhost:3004'
const PORT = Number(process.env.ERP_PORT || 5671)
const E2E_USER = process.env.E2E_USER || 'e2e_route_doc'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
// ⚠️ 雪花ID必须保持字符串：Number() 超过 MAX_SAFE_INTEGER 会精度丢失（...200 会变成 ...300）
const E2E_USER_ID = String(process.env.E2E_USER_ID || '2099000000000001200')
const ROOT = 'I:/AI-Ready'

function db(sql) {
  return execFileSync('python', ['tools/dbq.py', sql], { cwd: ROOT, encoding: 'utf8' })
}

function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* ignore */ }
        resolve({ status: res.statusCode, json })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 200) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 160) : ''}`)
}

let TOKEN = null
const api = (m, p, b) => rawReq(m, p, b, TOKEN)
const data = r => (r.json ? r.json.data : null)

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid,
  })
  TOKEN = res.json?.data?.token || res.json?.data?.accessToken || null
  if (!TOKEN) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 200))
}

let boundRiderId = null
let driverRouteId = null

;(async () => {
  await login()

  // ═══ 一、司机身份绑定 + 造一条本人路线 ═══
  console.log('\n═══ 一、司机档案与路线准备 ═══')
  // ⚠️ 专用司机：并行会话也会用 dms_rider.user_id 绑定各自验收账号，共用同一条会被互相覆盖
  const RIDER_NAME = 'E2E配送路线司机'
  const allRiders = data(await api('GET', '/dms/rider/page?pageNum=1&pageSize=200'))?.records || []
  let rider = allRiders.find(r => r.realName === RIDER_NAME)
  if (!rider) {
    // 企业员工型配送员必须关联系统账号（RiderService 校验），故创建时即带上验收账号
    rider = data(await api('POST', '/dms/rider', {
      realName: RIDER_NAME, phone: '13900001288', riderType: 1, status: 1, userId: E2E_USER_ID,
    }))
    check('创建专用司机档案（企业员工并关联账号）', !!rider?.id, JSON.stringify(rider || {}).slice(0, 140))
  }
  check('专用司机档案就绪', !!rider?.id, JSON.stringify(rider || {}).slice(0, 120))
  if (!rider?.id) { finish(1); return }

  // 兜底：确保 user_id 绑定到验收账号（已绑定则无影响）
  db(`UPDATE dms_rider SET user_id = ${E2E_USER_ID} WHERE id = ${rider.id}`)
  boundRiderId = rider.id

  const meRider = data(await api('GET', '/dms/verification/me/rider'))
  check('验收账号已绑定司机档案（currentRider 命中）', String(meRider?.id) === String(rider.id), JSON.stringify(meRider || {}).slice(0, 120))

  const created = data(await api('POST', '/delivery/route', {
    planDate: new Date().toISOString().slice(0, 10),
    deliveryPersonId: String(rider.id),
    deliveryPersonName: rider.realName,
    points: [
      { customerName: '司机端客户甲', customerPhone: '13900002222', address: '北京市东城区司机路1号', latitude: '39.9289', longitude: '116.4164' },
      { customerName: '司机端客户乙', customerPhone: '13900003333', address: '北京市东城区司机路2号', latitude: '39.9300', longitude: '116.4200' },
    ],
  }))
  driverRouteId = created?.id
  check('为司机创建配送路线单', !!driverRouteId, created?.routeCode)
  await api('POST', `/delivery/route/${driverRouteId}/start`)
  const detail = data(await api('GET', `/delivery/route/${driverRouteId}`))
  check('路线已进入配送中（司机可操作）', detail?.status === 'IN_PROGRESS', detail?.status)

  // ═══ 二、司机端页面 ═══
  console.log('\n═══ 二、司机端页面 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, locale: 'zh-CN', isMobile: true })
  const page = await ctx.newPage()
  const errors = []
  page.on('pageerror', e => errors.push(String(e)))

  await page.route(url => url.pathname.startsWith('/api/')
    && (url.hostname === 'localhost' || url.hostname === '127.0.0.1'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  async function openPage() {
    await page.goto(`${DRIVER_URL}/login`, { waitUntil: 'domcontentloaded' })
    await page.evaluate(tk => localStorage.setItem('token', tk), TOKEN)
    await page.goto(`${DRIVER_URL}/my-route`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(5000)
  }

  await openPage()
  let bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('司机端「我的配送路线」页面可打开', bodyText.includes('我的配送路线'), page.url())
  check('页面无 JS 运行时错误', errors.length === 0, errors.slice(0, 2).join(' | '))
  check('底部导航含「路线」Tab', bodyText.includes('路线'))
  check('加载到本人进行中路线', bodyText.includes(String(created?.routeCode || '')), (created?.routeCode || ''))
  check('展示点位卡片（客户/地址）', bodyText.includes('司机端客户甲') && bodyText.includes('北京市东城区司机路1号'), bodyText.slice(0, 200))
  check('点位含操作按钮（到达/送达/失败/导航）', ['到达', '送达', '失败', '导航'].every(t => bodyText.includes(t)))
  check('路线级操作含重新规划与刷新ETA', bodyText.includes('重新规划') && bodyText.includes('刷新ETA'))
  await page.screenshot({ path: `${ROOT}/tool-results/dms-route-list/driver-my-route.png`, fullPage: true })

  // ═══ 三、司机逐点回写 ═══
  console.log('\n═══ 三、司机逐点回写 ═══')
  // 送达：录入签收人
  await page.locator('button:has-text("送达")').first().click()
  await page.waitForTimeout(1500)
  const popupText = (await page.locator('.van-popup:visible').last().innerText()).replace(/\s+/g, '')
  check('送达弹层要求填写签收人', popupText.includes('签收人'), popupText.slice(0, 120))
  await page.locator('.van-popup:visible input').first().fill('李签收')
  await page.locator('.van-popup:visible button:has-text("确认送达")').first().click()
  await page.waitForTimeout(3500)
  const afterSign = data(await api('GET', `/delivery/route/${driverRouteId}`))
  const p1 = (afterSign?.points || [])[0]
  check('司机送达回写成功（签收人落库）', p1?.status === 'DELIVERED' && p1?.signee === '李签收', `${p1?.status}/${p1?.signee}`)
  bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面刷新后显示已送达', bodyText.includes('已送达'), bodyText.slice(0, 200))

  // 失败：录入原因
  await page.locator('button:has-text("失败")').first().click()
  await page.waitForTimeout(1500)
  await page.locator('.van-popup:visible textarea').first().fill('客户不在家')
  await page.locator('.van-popup:visible button:has-text("确认失败")').first().click()
  await page.waitForTimeout(3500)
  const afterFail = data(await api('GET', `/delivery/route/${driverRouteId}`))
  const p2 = (afterFail?.points || []).find(p => p.status === 'FAILED')
  check('司机失败回写成功（原因落库）', p2?.failReason === '客户不在家', `${p2?.status}/${p2?.failReason}`)

  check('全程无 JS 运行时错误', errors.length === 0, errors.slice(0, 2).join(' | '))
  await page.screenshot({ path: `${ROOT}/tool-results/dms-route-list/driver-my-route-signed.png`, fullPage: true })
  await browser.close()

  // 收尾：解绑司机档案（还原 user_id），取消测试路线
  db(`UPDATE dms_rider SET user_id = NULL WHERE id = ${rider.id}`)
  if (driverRouteId) {
    await api('POST', `/delivery/route/${driverRouteId}/cancel`, { reason: '司机端验收收尾' })
  }
  finish(errors.length > 0 ? 1 : 0)
})().catch(e => {
  console.error('验收异常：', e)
  try {
    if (boundRiderId) db(`UPDATE dms_rider SET user_id = NULL WHERE id = ${boundRiderId}`)
  } catch (ignore) { /* ignore */ }
  process.exit(2)
})

function finish(code) {
  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 司机端汇总：${pass}/${results.length} 通过，${fail} 失败 ═══`)
  if (fail > 0) {
    results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  }
  process.exit(fail > 0 ? 1 : 0)
}
