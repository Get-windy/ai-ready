/* 往来单位「证件信息」通用组件验证（客户 / 供应商 / 物流公司 / 其他往来单位）
 * 用法：NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/e2e-cert-upload.cjs
 */
const { chromium } = require('playwright')

const APP = process.env.APP_URL || 'http://localhost:5656'
const API = process.env.API_URL || 'http://localhost:5671'

const results = []
function check(name, ok, detail) {
  results.push({ name, ok, detail })
  console.log(`${ok ? '✅' : '❌'} ${name}${detail ? ' — ' + detail : ''}`)
}

async function login(page) {
  await page.goto(APP + '/', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(4000)
  if (!page.url().includes('login') && !(await page.locator('input[type=password]').count())) return
  const svg = await page.evaluate(() => {
    const imgs = [...document.querySelectorAll('img')].filter(i => (i.src || '').startsWith('data:image'))
    if (!imgs.length) return ''
    const src = imgs[imgs.length - 1].src
    try { return src.includes('base64,') ? decodeURIComponent(escape(atob(src.split('base64,')[1]))) : decodeURIComponent(src.split(',').slice(1).join(',')) } catch { return '' }
  })
  const code = [...String(svg).matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const tn = page.locator('input[placeholder*="租户"]').first()
  if (await tn.count() > 0) await tn.fill('系统租户')
  await page.locator('input[placeholder*="用户名"]').first().fill('admin')
  await page.locator('input[placeholder*="密码"]').first().fill('admin123')
  const ci = page.locator('input[placeholder*="验证码"]').first()
  if (await ci.count() > 0) await ci.fill(code)
  await page.locator('button:has-text("登 录"), button:has-text("登录")').first().click()
  await page.waitForTimeout(8000)
}

async function main() {
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: API + u.pathname + u.search })
  })
  await login(page)
  check('C1 登录进入系统', !page.url().includes('login'), page.url())

  // ── 四个往来单位表单：证件区存在 ──
  const pages = [
    ['客户', '/md/customer/form'],
    ['供应商', '/md/supplier/form'],
    ['物流公司', '/md/logistics/form'],
    ['其他往来单位', '/md/partner/form'],
  ]
  for (const [name, path] of pages) {
    await page.goto(APP + path, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(5500)
    const hasSection = (await page.locator('body').innerText().catch(() => '')).includes('证件信息')
    const firstLabel = await page.locator('.cert-label').first().innerText().catch(() => '')
    const selectCount = await page.locator('.cert-type-select.ant-select').count()
    const inputCount = await page.locator('.cert-type-select.ant-input').count()
    check(`${name} 证件信息区渲染 + 首个固定「营业执照」`,
      hasSection && firstLabel.trim() === '营业执照',
      `区=${hasSection} 首项=${firstLabel.trim()}`)
    check(`${name} 第二个证照为类型下拉（非固定名称）`, selectCount >= 1, `下拉=${selectCount} 输入框=${inputCount}`)
    check(`${name} 始终保留一个待输入位（≥2 个上传位）`,
      (await page.locator('.cert-placeholder').count()) >= 2,
      `上传位=${await page.locator('.cert-placeholder').count()}`)
  }

  // ── 客户页：类型下拉的选项与"自定义" ──
  await page.goto(APP + '/md/customer/form', { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(5500)
  const selects = page.locator('.cert-type-select.ant-select')
  if (await selects.count() > 0) {
    await selects.first().click()
    await page.waitForTimeout(1200)
    const opts = await page.locator('.ant-select-dropdown:visible .ant-select-item-option').allInnerTexts().catch(() => [])
    const joined = opts.join('/')
    check('C2 证照类型下拉含常见类型（生产/经营许可证）',
      joined.includes('生产许可证') && joined.includes('经营许可证'), joined.slice(0, 80))
    check('C3 证照类型下拉含「自定义」', joined.includes('自定义'), joined.slice(0, 80))

    // 选「自定义」→ 出现名称输入框
    const custom = page.locator('.ant-select-dropdown:visible .ant-select-item-option', { hasText: '自定义' }).first()
    if (await custom.count() > 0) {
      await custom.click()
      await page.waitForTimeout(1200)
      const nameInput = page.locator('.cert-type-select.ant-input')
      check('C4 选「自定义」后出现名称输入框', await nameInput.count() > 0)
      if (await nameInput.count() > 0) {
        await nameInput.first().fill('特种设备使用登记证')
        await nameInput.first().blur()
        await page.waitForTimeout(1200)
      }
    } else {
      check('C4 选「自定义」后出现名称输入框', false, '选项未找到')
    }
  } else {
    check('C2 证照类型下拉含常见类型（生产/经营许可证）', false, '无下拉')
    check('C3 证照类型下拉含「自定义」', false, '无下拉')
    check('C4 选「自定义」后出现名称输入框', false, '无下拉')
  }

  // ── 选择类型后自动追加新的待输入位 ──
  const before = await page.locator('.cert-placeholder').count()
  const selects2 = page.locator('.cert-type-select.ant-select')
  if (await selects2.count() > 1) {
    await selects2.nth(1).click()
    await page.waitForTimeout(1000)
    const opt = page.locator('.ant-select-dropdown:visible .ant-select-item-option', { hasText: '经营许可证' }).first()
    if (await opt.count() > 0) {
      await opt.click()
      await page.waitForTimeout(1500)
    }
  }
  const after = await page.locator('.cert-placeholder').count()
  check('C5 填写一个证照类型后自动追加新的待输入位', after > before, `填写前=${before} 填写后=${after}`)

  const pass = results.filter(r => r.ok).length
  console.log(`\n===== 往来单位证件信息（通用组件）验证：${pass}/${results.length} 通过 =====`)
  await browser.close()
  if (pass < results.length) {
    results.filter(r => !r.ok).forEach(r => console.log('  -', r.name, r.detail || ''))
    process.exit(1)
  }
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
