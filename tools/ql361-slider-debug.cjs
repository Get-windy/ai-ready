/* 单次滑块调试：输出拖动前后的关键状态，便于校正 dx */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')
const OUT = path.resolve(__dirname, '../tool-results/ql361/product')
const { loginQl361, solveSlider } = require('./ql361-lib.cjs')

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 } })
  const page = await ctx.newPage()

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(3000)
  await page.evaluate(({ acc, pwd }) => {
    const set = (el, v) => {
      const s = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set
      s.call(el, v); el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true }))
    }
    const vis = [...document.querySelectorAll('input')].filter(e => e.offsetParent !== null)
    const a = vis.find(e => (e.placeholder || '').includes('手机号')) || vis[0]
    const p = document.querySelector('#pwd')
    if (a) set(a, acc); if (p) set(p, pwd)
  }, { acc: '15095664266', pwd: 'Ysh790921' })
  await page.click('button.logon_button')
  await page.waitForTimeout(3000)

  const st = () => page.evaluate(() => {
    const vis = e => { const r = e.getBoundingClientRect(); return r.width > 50 && r.height > 50 && getComputedStyle(e).display !== 'none' }
    const box = [...document.querySelectorAll('.yidun_bgimg')].filter(vis).pop()
    if (!box) return { none: true }
    const jig = box.querySelector('.yidun_jigsaw')
    const sl = document.querySelector('.yidun_slider')
    const tips = [...document.querySelectorAll('.yidun_tips__text,.yidun_tips')].map(e => (e.innerText || '').trim()).filter(Boolean)
    return {
      bg: box.getBoundingClientRect().toJSON(),
      jigLeft: jig ? jig.style.left : null,
      jigRect: jig ? jig.getBoundingClientRect().toJSON() : null,
      sliderLeft: sl ? sl.style.left : null,
      sliderRect: sl ? sl.getBoundingClientRect().toJSON() : null,
      tips,
      inputVal: (document.querySelector('.yidun_input') || {}).value,
    }
  })

  console.log('BEFORE', JSON.stringify(await st()))

  // 求解并拖动（solveSlider 内部会拖）
  const res = await solveSlider(page, ctx, console.log)
  console.log('SOLVE', JSON.stringify(res.best))
  await page.waitForTimeout(2500)
  console.log('AFTER', JSON.stringify(await st()))
  await page.screenshot({ path: path.join(OUT, 'slider-after.png') })
  console.log('url', page.url())
  await browser.close()
})()
