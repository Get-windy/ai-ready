/* ql361 登录（含网易易盾滑块自动求解），导出可复用 storageState
 * 用法： node tools/ql361-login.cjs [stateOut]
 * 输出： tool-results/ql361/product/ql361-state.json（Playwright storageState）
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')

const OUT = path.resolve(__dirname, '../tool-results/ql361/product')
const ACCOUNT = process.env.QL361_USER || '15095664266'
const PASSWORD = process.env.QL361_PWD || 'Ysh790921'

async function solveSlider(page, ctx, log) {
  const box = await page.locator('.yidun_popup .yidun_bgimg').first().boundingBox()
  if (!box) throw new Error('no captcha widget')
  const shot = await page.screenshot({ clip: box })
  const pieceSrc = await page.evaluate(() => {
    const el = document.querySelector('.yidun_jigsaw')
    const r = el.getBoundingClientRect()
    const w = document.querySelector('.yidun_bgimg').getBoundingClientRect().width
    return { src: el.src, w: r.width, h: r.height, boxW: w, boxH: document.querySelector('.yidun_bgimg').getBoundingClientRect().height }
  })
  const resp = await ctx.request.get(pieceSrc.src)
  const pieceBuf = await resp.body()

  const res = await page.evaluate(async ({ bgB64, pieceB64, pw, ph, boxW, boxH }) => {
    const load = src => new Promise((res, rej) => { const i = new Image(); i.onload = () => res(i); i.onerror = rej; i.src = src })
    const bgImg = await load(bgB64)
    const pcImg = await load(pieceB64)
    const W = Math.round(boxW), H = Math.round(boxH)
    const PW = Math.max(1, Math.round(pw)), PH = H

    const cv = document.createElement('canvas'); cv.width = W; cv.height = H
    const cx = cv.getContext('2d')
    cx.drawImage(bgImg, 0, 0, W, H)
    const bg = cx.getImageData(0, 0, W, H).data

    const cp = document.createElement('canvas'); cp.width = PW; cp.height = PH
    const px = cp.getContext('2d')
    px.drawImage(pcImg, 0, 0, PW, PH)
    const pd = px.getImageData(0, 0, PW, PH).data

    // 提取拼图块有效像素（alpha>128）的灰度、坐标
    const pts = []
    for (let y = 0; y < PH; y++) {
      for (let x = 0; x < PW; x++) {
        const i = (y * PW + x) * 4
        if (pd[i + 3] > 128) {
          pts.push({ x, y, g: 0.299 * pd[i] + 0.587 * pd[i + 1] + 0.114 * pd[i + 2] })
        }
      }
    }
    if (pts.length < 50) return { error: 'piece mask empty', pts: pts.length }

    const lum = (x, y) => {
      if (x < 0 || y < 0 || x >= W || y >= H) return null
      const i = (y * W + x) * 4
      return 0.299 * bg[i] + 0.587 * bg[i + 1] + 0.114 * bg[i + 2]
    }

    let best = { dx: 0, score: -Infinity }
    const scores = []
    for (let dx = 0; dx <= W - PW; dx++) {
      // ZNCC（零均值归一化），对亮度线性变换不敏感
      let sa = 0, sb = 0, sxx = 0, syy = 0, sxy = 0, n = 0
      for (let k = 0; k < pts.length; k++) {
        const p = pts[k]
        const b = lum(p.x + dx, p.y)
        if (b === null) continue
        sa += p.g; sb += b; n++
      }
      if (n < 50) { scores.push(0); continue }
      const ma = sa / n, mb = sb / n
      for (let k = 0; k < pts.length; k++) {
        const p = pts[k]
        const b = lum(p.x + dx, p.y)
        if (b === null) continue
        const da = p.g - ma, db = b - mb
        sxx += da * da; syy += db * db; sxy += da * db
      }
      const den = Math.sqrt(sxx * syy)
      const score = den > 0 ? sxy / den : 0
      scores.push(score)
      if (score > best.score) best = { dx, score }
    }
    // 返回 top5 便于诊断
    const top = scores.map((s, i) => ({ dx: i, s })).sort((a, b) => b.s - a.s).slice(0, 5)
    return { best, top, W, H, PW, PH, piecePts: pts.length, bgB64Len: bgB64.length }
  }, {
    bgB64: 'data:image/png;base64,' + shot.toString('base64'),
    pieceB64: 'data:image/png;base64,' + pieceBuf.toString('base64'),
    pw: pieceSrc.w, ph: pieceSrc.h, boxW: box.width, boxH: box.height,
  })
  log('solve:', JSON.stringify(res.best), 'top:', JSON.stringify(res.top))

  const dist = res.best.dx
  const slider = page.locator('.yidun_slider').first()
  const sb = await slider.boundingBox()
  const startX = sb.x + sb.width / 2, y = sb.y + sb.height / 2
  await page.mouse.move(startX, y)
  await page.mouse.down()
  // 分段移动模拟人类轨迹
  const steps = 30
  for (let i = 1; i <= steps; i++) {
    const t = i / steps
    const e = t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t // easeInOut
    const jitter = i === steps ? 0 : (Math.random() - 0.5) * 2
    await page.mouse.move(startX + dist * e, y + jitter, { steps: 1 })
    await page.waitForTimeout(15 + Math.random() * 25)
  }
  await page.mouse.move(startX + dist, y, { steps: 3 })
  await page.waitForTimeout(300)
  await page.mouse.up()
  return res
}

;(async () => {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  const log = (...a) => console.log('[login]', ...a)

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(3000)

  for (let attempt = 1; attempt <= 6; attempt++) {
    log('attempt', attempt, 'url', page.url())
    if (!page.url().includes('/Account/Logon')) break
    await page.fill('input[placeholder="请输入手机号/用户名"]', ACCOUNT).catch(() => {})
    await page.fill('#pwd', PASSWORD).catch(() => {})
    await page.waitForTimeout(500)
    // 已出现验证码则直接解，否则点登录触发
    const hasBox = await page.locator('.yidun_popup .yidun_bgimg').count()
    if (!hasBox) {
      await page.click('button.logon_button').catch(() => {})
      for (let i = 0; i < 15; i++) { await page.waitForTimeout(400); if (await page.locator('.yidun_popup .yidun_bgimg').count()) break }
    }
    await page.waitForTimeout(1200)
    try {
      await solveSlider(page, ctx, log)
    } catch (e) { log('solve error', e.message) }
    await page.waitForTimeout(3500)
    const txt = await page.evaluate(() => (document.body.innerText || '').slice(0, 200))
    if (txt.includes('验证失败') || txt.includes('请重新')) log('captcha rejected')
    if (!page.url().includes('/Account/Logon')) { log('login OK ->', page.url()); break }
    await page.waitForTimeout(2000)
  }

  await page.screenshot({ path: path.join(OUT, 'login-result.png') })
  const stateOut = process.argv[2] || path.join(OUT, 'ql361-state.json')
  fs.writeFileSync(stateOut, JSON.stringify(await ctx.storageState(), null, 2), 'utf8')
  log('state ->', stateOut)
  log('final url', page.url())
  await browser.close()
})()
