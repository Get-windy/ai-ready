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

  // 点击登录触发验证码
  await page.locator('button:has-text("登录"), input[type="submit"]').first().click().catch(e => console.log('click err', e.message))
  await page.waitForTimeout(5000)
  await page.screenshot({ path: path.join(OUT, 'probe2-after-submit.png') })

  const info = await page.evaluate(() => {
    const out = { found: [], allWithDrag: [], bodyLen: document.body.innerHTML.length }
    ;['.yidun_control', '.yidun_slider', '.yidun_bgimg', '.yidun_jigsaw', '.yidun_popup',
      '.yidun_panel', '.yidun_intelli', '[class*="yidun"]', '[class*="nc_"]'].forEach(s => {
      const els = document.querySelectorAll(s)
      if (els.length) out.found.push({ sel: s, count: els.length, first: els[0].outerHTML.slice(0, 1500) })
    })
    // 找出所有含"拖动"或"滑块"文字的元素
    document.querySelectorAll('*').forEach(e => {
      if (e.children.length === 0) {
        const t = e.textContent || ''
        if (/拖动|滑块|拼图/.test(t)) out.allWithDrag.push({ tag: e.tagName, cls: e.className, t: t.slice(0, 60), parent: (e.parentElement?.outerHTML || '').slice(0, 800) })
      }
    })
    return out
  })
  fs.writeFileSync(path.join(OUT, 'probe2.json'), JSON.stringify(info, null, 2), 'utf8')
  console.log('bodyLen', info.bodyLen)
  console.log('found:', info.found.map(f => f.sel + '#' + f.count).join(', '))
  console.log('drag:', info.allWithDrag.length)
  for (const f of info.found) console.log('---', f.sel, '\n', f.first.replace(/></g, '>\n<').slice(0, 1200))
  for (const d of info.allWithDrag.slice(0, 3)) console.log('### drag', d.tag, d.cls, JSON.stringify(d.t), '\n', d.parent.replace(/></g, '>\n<').slice(0, 1200))
  await browser.close()
})()
