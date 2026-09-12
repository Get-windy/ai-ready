// 端到端验证：登录 → 访问账款交账 → 截图按职员/按单据视图
const { chromium } = require('playwright')

const BASE = 'http://localhost:5657'
const PROFILE = 'C:/Users/Administrator/AppData/Local/Temp/ad-pw-profile'
const OUT = 'C:/Users/Administrator/AppData/Local/Temp/ad-'

function extractCaptchaFromImg(src) {
  const m = (src || '').match(/base64,([A-Za-z0-9+/=]+)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  const texts = [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1])
  return texts.join('')
}

;(async () => {
  const browser = await chromium.launch({ headless: true, userDataDir: PROFILE })
  const page = await browser.newPage({ viewport: { width: 1600, height: 900 } })
  try {
    await page.goto(BASE + '/login', { waitUntil: 'networkidle', timeout: 30000 })
    await page.waitForSelector('.captcha-image img', { timeout: 15000 })
    const src = await page.$eval('.captcha-image img', el => el.getAttribute('src'))
    const captcha = extractCaptchaFromImg(src)
    console.log('验证码:', captcha)

    await page.fill('[aria-label="租户名称"]', '系统租户')
    await page.fill('[aria-label="用户名"]', 'admin')
    await page.fill('[aria-label="密码"]', 'admin123')
    await page.fill('[aria-label="验证码"]', captcha)
    await page.click('button[type="submit"]')
    await page.waitForTimeout(4000)

    await page.goto(BASE + '/finance/account-delivery', { waitUntil: 'networkidle', timeout: 30000 })
    await page.waitForTimeout(3500)
    await page.screenshot({ path: OUT + 'staff.png', fullPage: false })
    console.log('截图:staff')

    const docTab = await page.$('.tab-item:has-text("按单据")')
    if (docTab) { await docTab.click(); await page.waitForTimeout(3000) }
    await page.screenshot({ path: OUT + 'doc.png', fullPage: false })
    console.log('截图:doc')

    const body = await page.evaluate(() => document.body.innerText)
    console.log('===页面文本(完整)===\n' + body.slice(0, 2000))
  } catch (e) {
    console.error('E2E 出错:', e.message)
    await page.screenshot({ path: OUT + 'error.png', fullPage: false }).catch(() => {})
  } finally {
    await browser.close()
  }
})()
