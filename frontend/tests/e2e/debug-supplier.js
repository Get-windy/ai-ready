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
  await page.waitForTimeout(4000)

  // 检查所有 supplier 相关路由
  // 检查完整路由结构
  const routes = await page.evaluate(() => {
    const app = document.getElementById('app')
    if (!app || !app.__vue_app__) return { error: 'no vue app' }
    const router = app.__vue_app__.config.globalProperties.$router
    if (!router) return { error: 'no router' }

    // 获取 Layout 路由的完整信息
    const layout = router.getRoutes().find(r => r.name === 'Layout')
    return {
      layoutPath: layout ? layout.path : 'NOT FOUND',
      layoutChildrenCount: layout ? (layout.children ? layout.children.length : 'no children prop') : 'N/A',
      layoutRecordInfo: layout ? {
        path: layout.path,
        name: layout.name,
        hasChildren: !!layout.children,
        childrenKeys: layout.children ? layout.children.map(c => c.path) : 'no children'
      } : null
    }
  })

  // 也获取完整的路由树结构
  const allRoutes = await page.evaluate(() => {
    const app = document.getElementById('app')
    const router = app.__vue_app__.config.globalProperties.$router
    const all = router.getRoutes()

    // 找到所有 parent 为 Layout 的路由
    function findLayoutChildren(routes, parentName) {
      const result = []
      for (const r of routes) {
        // Vue Router 4 内部，子路由可以通过 record 追踪
        // 这里简单列出所有路径含 supplier 的路由及其 name
      }
      return result
    }

    return all
      .filter(r => r.path.includes('supplier') || r.name === 'Layout')
      .map(r => ({
        name: r.name,
        path: r.path,
        aliasOf: r.aliasOf ? 'yes' : 'no',
      }))
  })
  console.log('=== Registered supplier routes ===')
  console.log(JSON.stringify(routes, null, 2))

  // 逐一导航
  for (const testPath of ['/supplier', '/supplier/inquiry', '/supplier/performance', '/supplier/index']) {
    const result = await page.evaluate(async (p) => {
      const app = document.getElementById('app')
      const router = app.__vue_app__.config.globalProperties.$router
      try { await router.push(p) } catch (e) { return { error: e.message } }
      await new Promise(r => setTimeout(r, 1000))
      const cur = router.currentRoute.value
      return {
        requested: p,
        actual: cur.fullPath,
        matched: cur.matched.map(m => ({ name: m.name, path: m.path }))
      }
    }, testPath)
    console.log(`\n${testPath} →`, JSON.stringify(result))
  }

  await browser.close()
})()
