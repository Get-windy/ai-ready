/* ql361 登录库：含网易易盾滑块自动求解 */
const { chromium } = require('playwright')

/** 返回当前可见验证码组件的几何信息，无则 null */
async function visibleCaptcha(page) {
  return await page.evaluate(() => {
    const vis = e => {
      if (!e) return false
      const r = e.getBoundingClientRect()
      if (r.width < 50 || r.height < 50) return false
      let n = e
      while (n) {
        const s = getComputedStyle(n)
        if (s.display === 'none' || s.visibility === 'hidden' || s.opacity === '0') return false
        n = n.parentElement
      }
      return true
    }
    const boxes = [...document.querySelectorAll('.yidun_bgimg')].filter(vis)
    if (!boxes.length) return null
    const box = boxes[boxes.length - 1]
    const r = box.getBoundingClientRect()
    const jig = box.querySelector('.yidun_jigsaw')
    const jr = jig ? jig.getBoundingClientRect() : null
    return {
      box: { x: r.x, y: r.y, width: r.width, height: r.height },
      jigsaw: jr ? { x: jr.x, y: jr.y, width: jr.width, height: jr.height, src: jig.src } : null,
    }
  }).catch(() => null)
}

/** 等待验证码组件加载完成（提示语变为"向右拖动滑块填充拼图"且拼图块图片已加载） */
async function waitCaptchaReady(page, timeoutMs = 15000) {
  const t0 = Date.now()
  while (Date.now() - t0 < timeoutMs) {
    const ok = await page.evaluate(() => {
      const vis = e => {
        if (!e) return false
        const r = e.getBoundingClientRect()
        if (r.width < 50 || r.height < 50) return false
        let n = e
        while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
        return true
      }
      const slim = e => {
        if (!e) return false
        let n = e
        while (n) { const s = getComputedStyle(n); if (s.display === 'none' || s.visibility === 'hidden') return false; n = n.parentElement }
        return true
      }
      const tips = [...document.querySelectorAll('.yidun_tips__text')].filter(slim).map(e => (e.innerText || '').trim())
      const jig = [...document.querySelectorAll('.yidun_jigsaw')].filter(vis).pop()
      const readyTip = tips.some(t => t.includes('拖动') || t.includes('填充'))
      const imgOk = !!jig && jig.complete && jig.naturalWidth > 0
      return readyTip && imgOk
    }).catch(() => false)
    if (ok) return true
    await page.waitForTimeout(400)
  }
  return false
}

async function solveSlider(page, ctx, log) {
  const info = await visibleCaptcha(page)
  if (!info) throw new Error('no captcha widget')
  if (!(await waitCaptchaReady(page))) throw new Error('captcha not ready')
  await page.waitForTimeout(1500)
  const box = info.box
  const shot = await page.screenshot({ clip: box })
  if (process.env.QL361_DEBUG) {
    try {
      const fsx = require('fs'), pth = require('path')
      fsx.writeFileSync(pth.resolve(__dirname, '../tool-results/ql361/product/solve-bg.png'), shot)
    } catch {}
  }
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
    // 拼图块当前渲染在 x=0 处，会污染模板匹配 → 从块宽之后开始搜索
    const startDx = Math.ceil(PW) + 2
    for (let dx = startDx; dx <= W - PW; dx++) {
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
    const top = scores.map((s, i) => ({ dx: i + startDx, s })).sort((a, b) => b.s - a.s).slice(0, 5)
    return { best, top, W, H, PW, PH, piecePts: pts.length, bgB64Len: bgB64.length }
  }, {
    bgB64: 'data:image/png;base64,' + shot.toString('base64'),
    pieceB64: 'data:image/png;base64,' + pieceBuf.toString('base64'),
    pw: pieceSrc.w, ph: pieceSrc.h, boxW: box.width, boxH: box.height,
  })
  log('solve:', JSON.stringify(res.best), 'top:', JSON.stringify(res.top))

  const jigLeftOf = () => page.evaluate(() => {
    const j = [...document.querySelectorAll('.yidun_jigsaw')].pop()
    return j ? parseFloat(j.style.left || '0') : null
  })

  const dist = res.best.dx
  const slider = page.locator('.yidun_slider:visible').first()
  const sb = await slider.boundingBox()
  if (process.env.QL361_DEBUG) {
    try { await page.screenshot({ path: 'tool-results/ql361/product/solve-pre.png', clip: box }) } catch {}
  }
  const startX = sb.x + sb.width / 2, y = sb.y + sb.height / 2
  await page.mouse.move(startX, y)
  await page.mouse.down()
  // 分段移动模拟人类轨迹
  const glide = async (total, from) => {
    const steps = 24
    for (let i = 1; i <= steps; i++) {
      const t = i / steps
      const e = t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t // easeInOut
      const jitter = i === steps ? 0 : (Math.random() - 0.5) * 2
      await page.mouse.move(from + (total - from) * e, y + jitter, { steps: 1 })
      await page.waitForTimeout(12 + Math.random() * 18)
    }
    await page.mouse.move(total, y, { steps: 2 })
    await page.waitForTimeout(250)
  }
  let curX = startX
  await glide(startX + dist, curX)
  curX = startX + dist
  let moved = await jigLeftOf()
  // 伺服校正：组件内部对位移做了缩放，迭代逼近目标（鼠标保持按下）
  for (let k = 0; k < 5 && moved !== null; k++) {
    const err = dist - moved
    if (Math.abs(err) <= 1.2) break
    const ratio = moved > 1 ? (curX - startX) / moved : 1
    const step = Math.max(-260, Math.min(260, err * (isFinite(ratio) && ratio > 0.5 ? ratio : 1.1)))
    await glide(curX + step, curX)
    curX = curX + step
    moved = await jigLeftOf()
    log('servo', k, 'moved=', moved, 'target=', dist, 'curX=', Math.round(curX - startX))
  }
  if (process.env.QL361_DEBUG) {
    try { await page.screenshot({ path: 'tool-results/ql361/product/solve-post.png', clip: box }) } catch {}
  }
  await page.mouse.up()
  await page.waitForTimeout(700)
  if (process.env.QL361_DEBUG) {
    try { await page.screenshot({ path: 'tool-results/ql361/product/solve-released.png', clip: box }) } catch {}
  }
  log('drag moved piece to', moved, '(target', dist, ')')
  return { ...res, moved }
}


/** 登录 ql361 并停留在 desktop，返回 { ok, url } */
async function loginQl361(page, ctx, log = () => {}) {
  const ACCOUNT = process.env.QL361_USER || '15095664266'
  const PASSWORD = process.env.QL361_PWD || 'Ysh790921'
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(3000)
  for (let attempt = 1; attempt <= 6; attempt++) {
    if (!page.url().includes('/Account/Logon') && !page.url().includes('/account/logon')) return { ok: true, url: page.url() }
    log('login attempt', attempt)
    await page.evaluate(({ acc, pwd }) => {
      const set = (el, v) => {
        const s = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set
        s.call(el, v)
        el.dispatchEvent(new Event('input', { bubbles: true }))
        el.dispatchEvent(new Event('change', { bubbles: true }))
      }
      const vis = [...document.querySelectorAll('input')].filter(e => e.offsetParent !== null)
      const a = vis.find(e => (e.placeholder || '').includes('手机号')) || vis[0]
      const p = document.querySelector('#pwd') || vis.find(e => e.type === 'password')
      if (a) set(a, acc)
      if (p) set(p, pwd)
    }, { acc: ACCOUNT, pwd: PASSWORD }).catch(() => {})
    await page.waitForTimeout(500)
    if (!(await visibleCaptcha(page))) {
      await page.click('button.logon_button').catch(() => {})
      for (let i = 0; i < 20; i++) { await page.waitForTimeout(400); if (await visibleCaptcha(page)) break }
    }
    await page.waitForTimeout(1000)
    if (await visibleCaptcha(page)) {
      log('captcha detected, solving')
      try { await solveSlider(page, ctx, log) } catch (e) { log('solve error', e.message) }
    } else {
      log('no captcha this round')
    }
    await page.waitForTimeout(3500)
    if (!page.url().includes('/Account/Logon') && !page.url().includes('/account/logon')) return { ok: true, url: page.url() }
    await page.waitForTimeout(1500)
  }
  return { ok: false, url: page.url() }
}

module.exports = { loginQl361, solveSlider, visibleCaptcha }
