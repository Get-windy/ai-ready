/* 获取 ql361 登录验证码素材（背景图 + 拼图块），保存本地供分析 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const OUT = path.resolve(__dirname, '../tool-results/ql361/product')

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(4000)
  await page.locator('input[type="text"]:visible').first().fill('15095664266')
  await page.locator('input[type="password"]:visible').first().fill('Ysh790921')
  await page.locator('button:has-text("登录"), input[type="submit"]').first().click().catch(() => {})
  await page.waitForTimeout(5000)

  const info = await page.evaluate(() => {
    const bg = document.querySelector('.yidun_bgimg')
    const js = document.querySelector('.yidun_jigsaw')
    const sl = document.querySelector('.yidun_slider')
    const ctl = document.querySelector('.yidun_control')
    const r = e => e ? e.getBoundingClientRect().toJSON() : null
    return {
      bgStyle: bg ? bg.getAttribute('style') : null,
      bgComputed: bg ? getComputedStyle(bg).backgroundImage : null,
      bgRect: r(bg),
      jsSrc: js ? js.src : null,
      jsRect: r(js),
      jsNatural: js ? { w: js.naturalWidth, h: js.naturalHeight } : null,
      sliderRect: r(sl),
      ctlRect: r(ctl),
      bgChildren: bg ? Array.from(bg.children).map(c => ({ cls: c.className, style: c.getAttribute('style') })) : [],
    }
  })
  fs.writeFileSync(path.join(OUT, 'captcha-info.json'), JSON.stringify(info, null, 2), 'utf8')
  console.log(JSON.stringify(info, null, 2))

  // 保存素材
  const dl = async (url, name) => {
    const resp = await ctx.request.get(url)
    fs.writeFileSync(path.join(OUT, name), await resp.body())
  }
  const bgUrl = (info.bgComputed || '').replace(/^url\(["']?/, '').replace(/["']?\)$/, '')
  if (bgUrl) await dl(bgUrl, 'captcha-bg.png')
  if (info.jsSrc) await dl(info.jsSrc, 'captcha-piece.png')
  await page.screenshot({ path: path.join(OUT, 'captcha-widget.png') })
  await browser.close()
})()
