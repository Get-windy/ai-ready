/*
 * 司机端「配送 Tab → 配送详情 → 签收 / 收款」端到端验收
 *
 * 覆盖（《签收管理开发文档》§2 司机端 + §3.6 业务规范）：
 *   · API：当前登录人 → 配送员档案绑定；我的任务分页（riderId + statusList）；
 *          正常签收（不回传客户坐标 → 服务端回落任务快照算偏差）；幂等拒重复；部分签收必填数量 + 应签快照；
 *          拒收必填照片 + 原因；审核驳回 → 任务退回配送中 → 重新签收；任务状态机 1→2→3→4 与非法迁移拒绝；
 *          线下收款确认（代收货款）。
 *   · UI：配送任务列表（真实任务号/客户/状态/按钮）；任务详情（商品明细/代收货款/签收记录）；
 *          签收页（部分签收 + 数量 + 画签名 + 提交）；收款页（默认金额 + 提交）。
 *
 * 前置（每次跑之前重新执行一次，会重置 8 个种子任务）：
 *   python -c "import psycopg2;c=psycopg2.connect('host=localhost port=5432 dbname=devdb user=devuser password=devuser123');c.autocommit=True;c.cursor().execute(open('tools/e2e-driver-sign-user.sql',encoding='utf-8').read());c.close()"
 *   验收后端：java -jar <fat jar> --server.port=5695
 *   司机端：cd frontend/apps/driver-delivery && npx vite --port 3004
 *
 * 用法：ERP_PORT=5695 DRIVER_URL=http://localhost:3004 node tools/e2e-driver-sign.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const PORT = Number(process.env.ERP_PORT || 5695)
const DRIVER = process.env.DRIVER_URL || 'http://localhost:3004'
const SHOTS = 'I:/AI-Ready/tool-results/driver-sign'

const E2E_USER = process.env.E2E_USER || 'e2e_dsign'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

const RIDER_ID = '2099000000000007010'
const T = {
  NORMAL: '2099000000000007101',
  PARTIAL: '2099000000000007102',
  REJECT: '2099000000000007103',
  RETRY: '2099000000000007104',
  UI_ADVANCE: '2099000000000007105',
  PAY: '2099000000000007106',
  UI_SIGN: '2099000000000007107',
  SM: '2099000000000007108',
}

const CUST = { lat: 39.9087, lng: 116.3975 }
const NEAR = { lat: 39.9089, lng: 116.3978 } // ≈ 35 米

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

// ══════════════ HTTP 基础 ══════════════
function rawReq(method, reqPath, body, token, extraHeaders) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null || body instanceof Buffer ? body : JSON.stringify(body)
    const headers = {
      ...(body instanceof Buffer ? { 'Content-Type': 'application/octet-stream' } : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {}),
    }
    if (data && !(body instanceof Buffer)) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        resolve({ status: res.statusCode, buf, headers: res.headers, json: tryJson(buf) })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function tryJson(buf) {
  try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null }
}

let TOKEN = null

async function api(method, reqPath, body, extraHeaders) {
  let r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  const code = r.json && Number(r.json.code)
  if (r.status === 401 || code === 401) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  }
  return r
}

const data = (res) => (res.json ? res.json.data : null)
const ok = (res) => Number(res.json?.code) === 200

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户',
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  TOKEN = res.json?.data?.token || res.json?.data?.accessToken || null
  if (!TOKEN) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
}

// ══════════════ 结果收集 ══════════════
const results = []
function check(name, pass, detail) {
  results.push({ name, ok: !!pass, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${pass ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

/**
 * 现场照片 / 签名图：**真实上传**后使用后端返回的 url。
 * ⚠️ 历史坑：曾写死 `/api/file/view/e2e-driver-sign.png` 这类不存在的路径 ——
 * `/api/file/view/**` 有安全白名单（`yyyy/MM/dd/{32位uuid}.{ext}`），不合规**直接 400**，
 * 司机端签收页图片全裂并刷 Sentry 资源错误。造数据必须走真实上传接口。
 */
const PNG_1PX = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==', 'base64')
const JPEG_1PX = Buffer.from(
  '/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/wAALCAABAAEBAREA/8QAFAABAAAAAAAAAAAAAAAAAAAACf/EABQQAQAAAAAAAAAAAAAAAAAAAAD/2gAIAQEAAD8AKp//2Q==', 'base64')

let SIG = ''
let PHOTO = '[]'

/** multipart/form-data 上传（真实调用 POST /api/file/upload） */
function uploadFile(fileName, mimeType, bytes) {
  const boundary = '----e2edrv' + Math.random().toString(16).slice(2)
  const head = Buffer.from(
    `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\n`
    + `Content-Type: ${mimeType}\r\n\r\n`, 'utf8')
  const tail = Buffer.from(`\r\n--${boundary}--\r\n`, 'utf8')
  return api('POST', '/file/upload', Buffer.concat([head, bytes, tail]), {
    'Content-Type': `multipart/form-data; boundary=${boundary}`,
  })
}

/** 上传真实小图（照片×1 + 签名×1），得到可直接引用的 url */
async function ensureUploadedAssets() {
  const ph = await uploadFile('e2e-driver-photo.jpg', 'image/jpeg', JPEG_1PX)
  const sg = await uploadFile('e2e-driver-sign.png', 'image/png', PNG_1PX)
  const pUrl = data(ph)?.url
  const sUrl = data(sg)?.url
  if (!pUrl || !sUrl) throw new Error('司机端签收素材上传失败: ' + JSON.stringify([ph.json, sg.json]).slice(0, 300))
  PHOTO = JSON.stringify([pUrl])
  SIG = sUrl
  check('司机端照片/签名素材真实上传（/api/file/upload 返回规范 url）',
    /^\/api\/file\/view\/\d{4}\/\d{2}\/\d{2}\/[a-f0-9]{32}\.(jpg|png)$/.test(pUrl) && !!sUrl, `${pUrl} | ${sUrl}`)
}

const submit = (taskId, extra) => ({
  taskId,
  signType: 1,
  signatureUrl: SIG,
  signLat: NEAR.lat,
  signLng: NEAR.lng,
  ...extra,
})

// ══════════════ 一、API 验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、API 验收 ═══')
  await ensureUploadedAssets()

  // 1. 登录人 → 配送员档案
  const meRes = await api('GET', '/dms/verification/me/rider')
  const me = data(meRes)
  check('当前登录人可反查到配送员档案', ok(meRes) && me && String(me.id) === RIDER_ID,
    me ? `id=${me.id} name=${me.realName}` : meRes.json?.message)
  check('配送员档案已绑定 user_id', me && me.userId != null, me ? `userId=${me.userId}` : '')

  // 2. 我的任务分页
  const pageRes = await api('GET', `/dms/task/page?riderId=${RIDER_ID}&statusList=1,2,3,4&current=1&size=50`)
  const records = data(pageRes)?.records || []
  check('按配送员 + 状态列表查回任务', ok(pageRes) && records.length === 8, `records=${records.length}`)
  check('任务带客户与坐标快照',
    records.every(r => r.customerName && r.customerLat != null),
    records[0] ? `${records[0].taskNo} ${records[0].customerName}` : '')

  // 3. 正常签收：不回传客户坐标 → 服务端回落任务快照算偏差
  const normalRes = await api('POST', '/dms/sign/submit', submit(T.NORMAL))
  const normal = data(normalRes)
  check('正常签收提交成功', ok(normalRes), normalRes.json?.message)
  check('未回传客户坐标时偏差由任务快照回落算出（>0）',
    normal && Number(normal.locationDeviation) > 0, normal ? `deviation=${normal.locationDeviation}` : '')
  check('签收落为待审核', normal && normal.auditStatus === 0, normal ? `auditStatus=${normal.auditStatus}` : '')
  check('签收后任务置「已签收(5)」',
    await taskStatus(T.NORMAL) === 5, `status=${await taskStatus(T.NORMAL)}`)

  // 4. 幂等
  const again = await api('POST', '/dms/sign/submit', submit(T.NORMAL))
  check('同任务重复提交被拒（幂等）', !ok(again) && /待审核/.test(again.json?.message || ''), again.json?.message)

  // 5. 部分签收
  const partialNoQty = await api('POST', '/dms/sign/submit', submit(T.PARTIAL, { signType: 2 }))
  check('部分签收缺数量被拒', !ok(partialNoQty) && /数量/.test(partialNoQty.json?.message || ''), partialNoQty.json?.message)
  const partialRes = await api('POST', '/dms/sign/submit', submit(T.PARTIAL, { signType: 2, actualQuantity: 8 }))
  const partial = data(partialRes)
  check('部分签收带数量提交成功', ok(partialRes) && partial?.signType === 2, partialRes.json?.message)
  check('应签数量快照 = 任务发货数量(20)',
    partial && Number(partial.plannedQuantity) === 20, partial ? `planned=${partial.plannedQuantity} actual=${partial.actualQuantity}` : '')

  // 6. 拒收
  const rejectNoPhoto = await api('POST', '/dms/sign/submit', submit(T.REJECT, { signType: 3, remark: '客户拒收' }))
  check('拒收缺照片被拒', !ok(rejectNoPhoto) && /照片/.test(rejectNoPhoto.json?.message || ''), rejectNoPhoto.json?.message)
  const rejectNoRemark = await api('POST', '/dms/sign/submit', submit(T.REJECT, { signType: 3, photoUrls: PHOTO }))
  check('拒收缺原因被拒', !ok(rejectNoRemark) && /原因/.test(rejectNoRemark.json?.message || ''), rejectNoRemark.json?.message)
  const rejectRes = await api('POST', '/dms/sign/submit', submit(T.REJECT, { signType: 3, photoUrls: PHOTO, remark: '客户拒收' }))
  const reject = data(rejectRes)
  check('拒收（照片 + 原因）提交成功', ok(rejectRes) && reject?.signType === 3, rejectRes.json?.message)
  check('拒收照片数被解析', reject && reject.photoCount === 1, reject ? `photoCount=${reject.photoCount}` : '')

  // 7. 驳回 → 退回配送中 → 重新签收
  const seedRetry = await api('POST', '/dms/sign/submit', submit(T.RETRY))
  const seedRetryId = data(seedRetry)?.id
  const rejectAudit = await api('POST', `/dms/sign/${seedRetryId}/audit`, { auditStatus: 2, auditRemark: '照片不清晰，请重签' })
  check('审核驳回成功', ok(rejectAudit), rejectAudit.json?.message)
  check('驳回后任务退回「配送中(4)」', await taskStatus(T.RETRY) === 4, `status=${await taskStatus(T.RETRY)}`)
  const resign = await api('POST', '/dms/sign/submit', submit(T.RETRY, { remark: '重新签收' }))
  check('驳回后可重新签收', ok(resign), resign.json?.message)

  // 8. 审核通过 → 任务已完成
  const pendingList = await api('GET', `/dms/sign/page?taskNo=E2EDRV-01&pageNum=1&pageSize=5`)
  const normalId = (data(pendingList)?.records || [])[0]?.id
  const approve = await api('POST', `/dms/sign/${normalId}/audit`, { auditStatus: 1 })
  check('审核通过成功', ok(approve), approve.json?.message)
  check('通过后任务置「已完成(6)」', await taskStatus(T.NORMAL) === 6, `status=${await taskStatus(T.NORMAL)}`)

  // 9. 任务状态机（司机端「接单/取货/配送」按钮链路）
  const bad = await api('PUT', `/dms/task/${T.SM}/status`, { fromStatus: 2, toStatus: 3 })
  check('fromStatus 不匹配被拒', !ok(bad), bad.json?.message)
  const s12 = await api('PUT', `/dms/task/${T.SM}/status`, { fromStatus: 1, toStatus: 2 })
  check('状态机 已分配(1)→已接单(2)', ok(s12), s12.json?.message)
  const s23 = await api('PUT', `/dms/task/${T.SM}/status`, { fromStatus: 2, toStatus: 3 })
  check('状态机 已接单(2)→取货中(3)', ok(s23), s23.json?.message)
  const s34 = await api('PUT', `/dms/task/${T.SM}/status`, { fromStatus: 3, toStatus: 4 })
  check('状态机 取货中(3)→配送中(4)', ok(s34), s34.json?.message)
  const illegal = await api('PUT', `/dms/task/${T.SM}/status`, { fromStatus: 4, toStatus: 2 })
  check('非法迁移（配送中→已接单）被拒', !ok(illegal), illegal.json?.message)

  // 10. 线下收款（代收货款）
  const payRes = await api('POST', `/dms/payment/confirm?taskId=${T.PAY}&amount=500&payChannel=3&paymentType=1`)
  const pay = data(payRes)
  check('司机端线下收款确认成功', ok(payRes) && pay?.status === 1, payRes.json?.message)
  check('收款类型落为「代收货款」', pay && pay.paymentType === 1, pay ? `paymentType=${pay.paymentType} amount=${pay.amount}` : '')
  const payDict = await api('GET', '/dms/payment/dict')
  check('收款字典含支付方式与收款类型',
    ok(payDict) && data(payDict)?.payChannels && data(payDict)?.paymentTypes, JSON.stringify(data(payDict)?.payChannels))
}

async function taskStatus(taskId) {
  const r = await api('GET', `/dms/task/${taskId}`)
  return data(r)?.status
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({
    viewport: { width: 430, height: 932 },
    locale: 'zh-CN',
    acceptDownloads: true,
    // 授权并模拟 GPS，使签收页带上 signLat/signLng；客户坐标不回传 → 服务端回落任务快照算偏差
    permissions: ['geolocation'],
    geolocation: { latitude: 39.9089, longitude: 116.3978 },
  })
  const page = await ctx.newPage()
  const errors = []
  page.on('pageerror', e => errors.push(String(e)))

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  async function openPage(url, waitMs = 4000) {
    for (let attempt = 0; attempt < 3; attempt++) {
      await login()
      await page.goto(`${DRIVER}/`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(tk => localStorage.setItem('token', tk), TOKEN)
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(waitMs)
      return
    }
  }

  const bodyText = () => page.locator('body').innerText().then(t => t.replace(/\s+/g, ''))

  /** 等 Vant toast 出现（自动消失很快，固定 sleep 抓不到） */
  async function waitToast(ms = 5000) {
    const deadline = Date.now() + ms
    let last = ''
    while (Date.now() < deadline) {
      const t = await page.locator('.van-toast').first().innerText().catch(() => '')
      if (t && t.trim()) {
        last = t.replace(/\s+/g, '')
        if (/成功|失败|必须|不能|请/.test(last)) return last
      }
      await page.waitForTimeout(150)
    }
    return last
  }

  // —— 配送任务列表 ——
  await openPage(`${DRIVER}/delivery`, 5000)
  const listTxt = await bodyText()
  check('配送任务列表可打开', listTxt.length > 20 && !listTxt.includes('页面不存在'), page.url())
  check('列表渲染真实任务号 E2EDRV-07', listTxt.includes('E2EDRV-07'), listTxt.slice(0, 120))
  check('列表渲染客户名称', listTxt.includes('E2E司机客户庚'), '')
  check('列表含「签收」按钮', listTxt.includes('签收'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // —— 状态推进：已分配 → 已接单 ——
  const advanceCard = page.locator('.delivery-card', { hasText: 'E2EDRV-05' }).first()
  const advanceBtn = advanceCard.locator('button', { hasText: '接单' }).first()
  let advanced = false
  if (await advanceBtn.count()) {
    await advanceBtn.click()
    await page.waitForTimeout(2500)
    advanced = (await advanceCard.innerText()).includes('已接单')
  }
  check('列表「接单」按钮推进状态（1→2）', advanced, advanced ? '' : '未看到已接单')
  check('状态推进后端已落库', await taskStatus(T.UI_ADVANCE) === 2, `status=${await taskStatus(T.UI_ADVANCE)}`)

  // —— 任务详情 ——
  await openPage(`${DRIVER}/delivery/${T.UI_SIGN}`, 4000)
  const detailTxt = await bodyText()
  check('配送详情可打开', detailTxt.includes('E2EDRV-07'), detailTxt.slice(0, 120))
  check('详情含客户与地址', detailTxt.includes('E2E司机客户庚') && detailTxt.includes('东长安街7号'))
  check('详情含商品明细与发货数量', detailTxt.includes('配送商品') && detailTxt.includes('30'))
  check('详情含代收货款/配送费', detailTxt.includes('代收货款') && detailTxt.includes('配送费'))
  check('详情含「签收」入口', detailTxt.includes('签收'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-detail.png'), fullPage: true })

  // —— 签收页：部分签收 + 数量 + 手写签名 ——
  await openPage(`${DRIVER}/delivery/${T.UI_SIGN}/sign`, 4000)
  const signTxt = await bodyText()
  check('签收页可打开', signTxt.includes('签收类型') && signTxt.includes('正常签收'), signTxt.slice(0, 120))
  check('签收页展示应签收数量(30)', signTxt.includes('应签收') && signTxt.includes('30'))
  check('签收页含三类签收类型', signTxt.includes('正常签收') && signTxt.includes('部分签收') && signTxt.includes('拒收'))

  // 选「部分签收」
  await page.locator('.van-radio', { hasText: '部分签收' }).first().click()
  await page.waitForTimeout(600)
  const partialTxt = await bodyText()
  check('选部分签收后出现数量输入', partialTxt.includes('实际签收数量') || partialTxt.includes('实收数量'), partialTxt.slice(0, 160))

  // 填数量
  const qtyInput = page.locator('input[placeholder*="应签收"], input[placeholder*="实际签收数量"]').first()
  await qtyInput.fill('3')
  await page.waitForTimeout(300)

  // 画签名（SignaturePad canvas）
  const canvas = page.locator('canvas').first()
  const box = await canvas.boundingBox()
  if (box) {
    await page.mouse.move(box.x + 30, box.y + 60)
    await page.mouse.down()
    for (let i = 0; i < 12; i++) {
      await page.mouse.move(box.x + 30 + i * 12, box.y + 60 + Math.sin(i) * 18)
    }
    await page.mouse.up()
  }
  await page.waitForTimeout(500)
  check('手写签名已采集', (await bodyText()).includes('已采集手写签名'), '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-sign-form.png'), fullPage: true })

  // 提交
  await page.locator('button', { hasText: '提交签收' }).first().click()
  await page.waitForTimeout(900)
  const dlgTxt = await page.locator('body').innerText()
  check('提交前弹出确认框', dlgTxt.includes('签收确认'), dlgTxt.slice(0, 100))
  await page.locator('.van-dialog__confirm').first().click()
  const signToast = await waitToast()
  check('提交后提示签收成功', /签收成功/.test(signToast), signToast || '(无提示)')
  await page.waitForTimeout(2000)

  // 后端落库校验
  const uiSignRes = await api('GET', `/dms/sign/${T.UI_SIGN}`)
  const uiSign = data(uiSignRes)
  check('UI 签收已落库（部分签收 signType=2）', uiSign && uiSign.signType === 2, uiSign ? `signType=${uiSign.signType}` : 'null')
  check('UI 签收数量=3、应签快照=30',
    uiSign && Number(uiSign.actualQuantity) === 3 && Number(uiSign.plannedQuantity) === 30,
    uiSign ? `actual=${uiSign.actualQuantity} planned=${uiSign.plannedQuantity}` : '')
  check('UI 签收签名已上传为文件 URL',
    uiSign && /^\/api\/file\/view\//.test(uiSign.signatureUrl || ''), uiSign?.signatureUrl)
  check('UI 签收定位偏差由任务快照回落算出（>0）',
    uiSign && Number(uiSign.locationDeviation) > 0, uiSign ? `deviation=${uiSign.locationDeviation}` : '')
  await page.screenshot({ path: path.join(SHOTS, 'ui-signed.png'), fullPage: true })

  // —— 收款页 ——
  await openPage(`${DRIVER}/delivery/${T.PAY}/collect`, 4000)
  const collectTxt = await bodyText()
  check('收款页可打开', collectTxt.includes('收款'), collectTxt.slice(0, 120))
  check('收款页默认带出代收货款 500', collectTxt.includes('500'), collectTxt.slice(0, 160))
  check('收款页含支付方式字典', collectTxt.includes('现金') && collectTxt.includes('微信'))
  await page.locator('button', { hasText: '确认收款' }).first().click()
  await page.waitForTimeout(900)
  check('收款前弹出确认框', (await page.locator('body').innerText()).includes('收款确认'))
  await page.locator('.van-dialog__confirm').first().click()
  const payToast = await waitToast()
  check('收款提交后提示成功', /收款成功/.test(payToast), payToast || '(无提示)')
  await page.waitForTimeout(2000)
  const payCheck = await api('GET', `/dms/payment/${T.PAY}`)
  check('UI 收款已落库（status=1）', data(payCheck)?.status === 1, JSON.stringify(data(payCheck)).slice(0, 160))
  await page.screenshot({ path: path.join(SHOTS, 'ui-collect.png'), fullPage: true })

  check('页面无 JS 运行时错误', errors.length === 0, errors.slice(0, 2).join(' | '))

  await browser.close()
}

// ══════════════ 汇总 ══════════════
;(async () => {
  await login()
  await apiSuite()
  await uiSuite()

  const pass = results.filter(r => r.ok).length
  console.log(`\n═══ 验收汇总：${pass}/${results.length} ═══`)
  fs.writeFileSync(path.join(SHOTS, 'result.json'), JSON.stringify({
    total: results.length, pass, failed: results.filter(r => !r.ok),
  }, null, 2), 'utf8')
  process.exit(pass === results.length ? 0 : 1)
})().catch(e => {
  console.error('验收脚本异常:', e)
  process.exit(1)
})
