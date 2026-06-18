/**
 * 追踪 router.push('/supplier/inquiry') 在导航守卫中的每一步
 * 找出 router.resolve 正确但 router.push 被重定向的原因
 */
const { chromium } = require('playwright')
;(async () => {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage()

  // 登录
  const resp = await page.request.post('http://localhost:5655/api/auth/login', {
    data: { username: 'admin', password: 'admin123', tenantName: '系统租户' }
  })
  const token = (await resp.json()).data.token
  console.log('Token:', token.substring(0, 12) + '...')

  await page.addInitScript((t) => {
    localStorage.setItem('token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
  }, token)

  await page.goto('http://localhost:3000/dashboard', { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.waitForTimeout(5000)

  // 确认 SPA 已加载
  const loaded = await page.evaluate(() => {
    const app = document.getElementById('app')?.__vue_app__
    return !!app
  })
  console.log('SPA loaded:', loaded)
  if (!loaded) { await browser.close(); process.exit(1) }

  // Step 1: 检查路由注册情况
  console.log('\n=== Step 1: 路由注册情况 ===')
  const step1 = await page.evaluate(() => {
    const router = document.getElementById('app').__vue_app__.config.globalProperties.$router
    const all = router.getRoutes()
    const supplierRoutes = all.filter(r => {
      const p = r.path || ''
      return p.includes('supplier')
    })
    return supplierRoutes.map(r => ({
      name: r.name,
      path: r.path,
      hasComponent: !!r.components?.default,
      parentName: r.parent?.name || null,
    }))
  })
  console.log(JSON.stringify(step1, null, 2))

  // Step 2: 使用 router.resolve 静态分析
  console.log('\n=== Step 2: router.resolve 静态分析 ===')
  const step2 = await page.evaluate(() => {
    const router = document.getElementById('app').__vue_app__.config.globalProperties.$router
    const paths = ['/supplier', '/supplier/inquiry', '/supplier/performance', '/supplier/index']
    const results = {}
    for (const p of paths) {
      const resolved = router.resolve(p)
      results[p] = {
        name: resolved.name,
        path: resolved.path,
        matchedNames: resolved.matched.map(m => ({ name: m.name, path: m.path })),
        matchedCount: resolved.matched.length,
        href: resolved.href,
      }
    }
    return results
  })
  console.log(JSON.stringify(step2, null, 2))

  // Step 3: 检查导航守卫关键数据
  console.log('\n=== Step 3: 用户/权限状态 ===')
  const step3 = await page.evaluate(() => {
  try {
    // 通过 Vue devtools 全局 API 访问 store
    const app = document.getElementById('app').__vue_app__
    const pinia = app.config.globalProperties.$pinia
    if (!pinia) return { error: 'no pinia' }

    // 获取 user store 的 state
    const userState = pinia.state.value.user
    return {
      userType: userState?.userInfo?.userType,
      isSystemUser: (userState?.userInfo?.userType ?? 2) === 0,
      validModuleCodes: userState?.validModuleCodes,
      validModuleCodesLength: userState?.validModuleCodes?.length,
      permissionsCount: userState?.permissions?.length,
      permissionsSample: userState?.permissions?.slice(0, 10),
      hasStarPermission: userState?.permissions?.includes('*'),
    }
  } catch(e) { return { error: e.message } }
  })
  console.log(JSON.stringify(step3, null, 2))

  // Step 4: 直接调用 checkRouteAccess 和 getRouteModule
  console.log('\n=== Step 4: checkRouteAccess / hasValidModule ===')
  const step4 = await page.evaluate(() => {
    // 尝试从全局获取这些函数（如果暴露了的话）
    // 不直接 import，用 eval 或查找全局挂载
    const app = document.getElementById('app').__vue_app__
    const router = app.config.globalProperties.$router

    // 模拟 getRouteModule 逻辑
    function getRouteModule(routePath) {
      const normalized = routePath.replace(/^\/+|\/+$/g, '').replace(/\/:\w+\??/g, '')
      const firstSegment = normalized.split('/')[0]
      return firstSegment || undefined
    }

    // 模拟 hasValidModule 逻辑
    function hasValidModule(routePath) {
      const pinia = app.config.globalProperties.$pinia
      const userState = pinia?.state?.value?.user
      if (!userState) return { result: 'unknown', reason: 'no user state' }

      const isSystemUser = (userState.userInfo?.userType ?? 2) === 0
      if (isSystemUser) return { result: true, reason: 'system user' }

      const validModuleCodes = userState.validModuleCodes || []
      if (validModuleCodes.length === 0) return { result: true, reason: 'empty validModuleCodes' }

      const parts = routePath.replace(/^\/+/, '').split('/').filter(Boolean)
      if (parts.length === 0) return { result: true, reason: 'empty path' }

      const moduleKey = parts[0]
      if (validModuleCodes.includes(moduleKey)) return { result: true, reason: `exact match: ${moduleKey}` }

      let prefix = moduleKey
      while (prefix.includes(':')) {
        prefix = prefix.substring(0, prefix.lastIndexOf(':'))
        if (validModuleCodes.includes(prefix)) return { result: true, reason: `prefix match: ${prefix}` }
      }
      return { result: false, reason: `no match for ${moduleKey} in [${validModuleCodes.join(',')}]` }
    }

    const testPaths = ['supplier', 'supplier/inquiry', 'stock', 'sale', 'dashboard']
    const results = {}
    for (const p of testPaths) {
      const mod = getRouteModule(p)
      const valid = hasValidModule(p)
      results[p] = { module: mod, ...valid }
    }
    return results
  })
  console.log(JSON.stringify(step4, null, 2))

  // Step 5: 实际执行 router.push 并追踪
  console.log('\n=== Step 5: router.push 实际导航追踪 ===')

  // 方法1: 拦截 console.warn 查看守卫日志
  await page.evaluate(() => {
    window._guardLogs = []
    const origWarn = console.warn
    const origError = console.error
    console.warn = function(...args) {
      window._guardLogs.push({ level: 'warn', msg: args.map(String).join(' ') })
      origWarn.apply(console, args)
    }
    console.error = function(...args) {
      window._guardLogs.push({ level: 'error', msg: args.map(String).join(' ') })
      origError.apply(console, args)
    }
  })

  // 导航到 /supplier/inquiry
  const navResult = await page.evaluate(async () => {
    const router = document.getElementById('app').__vue_app__.config.globalProperties.$router
    try {
      await router.push('/supplier/inquiry')
    } catch(e) {
      return { error: 'push rejected: ' + e.message }
    }
    await new Promise(r => setTimeout(r, 1500))
    const cur = router.currentRoute.value
    return {
      requested: '/supplier/inquiry',
      actual: cur.fullPath,
      matched: cur.matched.map(m => ({ name: m.name, path: m.path })),
    }
  })
  console.log('Navigation result:', JSON.stringify(navResult, null, 2))

  // 收集守卫日志
  const logs = await page.evaluate(() => window._guardLogs || [])
  const relevantLogs = logs.filter(l =>
    l.msg.includes('守卫') || l.msg.includes('路由') || l.msg.includes('404') ||
    l.msg.includes('supplier') || l.msg.includes('权限') || l.msg.includes('动态路由')
  )
  console.log('Guard logs:', JSON.stringify(relevantLogs, null, 2))

  // Step 6: 检查当前 URL
  const currentUrl = page.url()
  console.log('\nCurrent page URL:', currentUrl)
  const bodyText = await page.textContent('body')
  console.log('Body excerpt:', bodyText.substring(0, 200))

  await browser.close()
})()
