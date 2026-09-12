/* 往来单位「所在地区」行政区划级联 专项验证（客户表单 + 组件级共用）
 * 用法：NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/e2e-customer-area.cjs
 */
const { chromium } = require('playwright')
const fs = require('fs')

const APP = process.env.APP_URL || 'http://localhost:5656'
const API = process.env.API_URL || 'http://localhost:5671'
const OUT = 'I:/AI-Ready/screenshots-customer'

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true })
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: API + u.pathname + u.search })
  })

  await page.goto(APP + '/', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(4000)
  if (page.url().includes('login')) {
    const svg = await page.evaluate(() => {
      const imgs = [...document.querySelectorAll('img')].filter(i => (i.src || '').startsWith('data:image'))
      if (!imgs.length) return ''
      const src = imgs[imgs.length - 1].src
      try {
        if (src.includes('base64,')) return decodeURIComponent(escape(atob(src.split('base64,')[1])))
        return decodeURIComponent(src.split(',').slice(1).join(','))
      } catch { return '' }
    })
    const code = [...String(svg).matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
    const tenantInput = page.locator('input[placeholder*="租户"]').first()
    if (await tenantInput.count() > 0) await tenantInput.fill('系统租户')
    await page.locator('input[placeholder*="用户名"]').first().fill('admin')
    await page.locator('input[placeholder*="密码"]').first().fill('admin123')
    const captchaInput = page.locator('input[placeholder*="验证码"]').first()
    if (await captchaInput.count() > 0) await captchaInput.fill(code)
    await page.locator('button:has-text("登 录"), button:has-text("登录")').first().click()
    await page.waitForTimeout(8000)
  }
  check('A1 登录进入系统', !page.url().includes('login'), page.url())

  // ── 客户新增表单 ──
  await page.goto(APP + '/md/customer/form', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(6000)

  const basicText = await page.locator('.section-card').filter({ hasText: '基本信息' }).first().innerText().catch(() => '')
  check('A2 基本信息区已移除「所在省/市/区」（消除冗余）',
    !basicText.includes('所在省') && !basicText.includes('所在市') && !basicText.includes('所在区'))
  check('A3 基本信息区保留所属区域（对标）', basicText.includes('所属区域'))

  const contactSection = page.locator('.contact-section').first()
  check('A4 联系人信息区渲染', await contactSection.count() > 0)

  const cascader = contactSection.locator('.ant-cascader').first()
  check('A5 联系人「所在地区」为行政区划级联（非纯文本输入）', await cascader.count() > 0)

  // 打开级联，验证省 → 市 → 区县三级
  let level1 = 0, level2 = 0, level3 = 0
  if (await cascader.count() > 0) {
    await cascader.click()
    await page.waitForTimeout(2500)
    level1 = await page.locator('.ant-cascader-menu').nth(0).locator('.ant-cascader-menu-item').count()
    // 选「甘肃省」
    const gansu = page.locator('.ant-cascader-menu').nth(0).locator('.ant-cascader-menu-item', { hasText: '甘肃省' }).first()
    if (await gansu.count() > 0) {
      await gansu.click()
      await page.waitForTimeout(1500)
      level2 = await page.locator('.ant-cascader-menu').nth(1).locator('.ant-cascader-menu-item').count()
      const jyg = page.locator('.ant-cascader-menu').nth(1).locator('.ant-cascader-menu-item', { hasText: '嘉峪关市' }).first()
      if (await jyg.count() > 0) {
        await jyg.click()
        await page.waitForTimeout(1500)
        level3 = await page.locator('.ant-cascader-menu').nth(2).locator('.ant-cascader-menu-item').count()
        await page.locator('.ant-cascader-menu').nth(2).locator('.ant-cascader-menu-item', { hasText: '雄关街道' }).first().click()
        await page.waitForTimeout(1200)
      }
    }
  }
  check('A6 一级=省（31 个）', level1 >= 30, `省数量=${level1}`)
  check('A7 二级=市（甘肃 14 个）', level2 >= 10, `市数量=${level2}`)
  check('A8 三级=区县', level3 >= 1, `区县数量=${level3}`)

  const picked = await cascader.innerText().catch(() => '')
  check('A9 选中后回显「甘肃省 / 嘉峪关市 / 雄关街道」', picked.includes('甘肃') && picked.includes('嘉峪关'),
    picked.replace(/\s+/g, ' '))
  await page.screenshot({ path: `${OUT}/07-form-area.png`, fullPage: false })

  // ── 基本信息布局：一行 4 列（对标紧凑布局） ──
  const basicRowCols = await page.evaluate(() => {
    const card = [...document.querySelectorAll('.section-card')].find(c => (c.innerText || '').includes('基本信息'))
    if (!card) return 0
    const rows = [...card.querySelectorAll('.ant-row')]
    let max = 0
    for (const r of rows) {
      const cols = [...r.children].filter(c => c.classList.contains('ant-col'))
      if (cols.length > max) max = cols.length
    }
    return max
  })
  check('A10 基本信息区布局收紧（每行 ≥4 列）', basicRowCols >= 4, `最大列数=${basicRowCols}`)

  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 往来单位所在地区（行政区划级联）UI 验证：${pass}/${results.length} 通过 =====`)
  await browser.close()
  if (pass < results.length) {
    results.filter(r => !r.ok).forEach(r => console.log('  -', r.name, r.detail || ''))
    process.exit(1)
  }
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
