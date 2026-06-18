const { chromium } = require('playwright')
;(async () => {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage()

  const resp = await page.request.post('http://localhost:5655/api/auth/login', {
    data: { username: 'admin', password: 'admin123', tenantName: '系统租户' }
  })
  const token = (await resp.json()).data.token
  await page.addInitScript((t) => {
    localStorage.setItem('token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
  }, token)

  await page.goto('http://localhost:3000/dashboard', { waitUntil: 'domcontentloaded', timeout: 20000 })
  await page.waitForTimeout(5000)

  // 直接通过 router.resolve 测试路径解析
  const info = await page.evaluate(() => {
    const router = document.getElementById('app').__vue_app__.config.globalProperties.$router

    // 测试 router.resolve
    const testPaths = ['/supplier', '/supplier/inquiry', '/supplier/performance', '/supplier/index', '/stock']
    const results = {}
    for (const p of testPaths) {
      const resolved = router.resolve(p)
      results[p] = {
        name: resolved.name,
        path: resolved.path,
        matchedNames: resolved.matched.map(m => m.name),
        matchedPaths: resolved.matched.map(m => m.path),
        matchedCount: resolved.matched.length,
      }
    }
    return results
  })
  console.log(JSON.stringify(info, null, 2))

  await browser.close()
})()
