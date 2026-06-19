const { chromium } = require('playwright')

// 已知 404 的模块（独立微服务未启动）
const KNOWN_404_SEGMENTS = new Set([
  'warehouse', 'location', 'receipt', 'putaway', 'pick', 'wave',
  'ship', 'inventory', 'move', 'check', 'event',
  'rider', 'vehicle', 'route', 'dispatch', 'order-pool', 'tracking', 'verification',
  'channel',
  'asset', 'category', 'depreciation', 'transfer', 'disposal',
  'index', 'template', 'annual', 'adjustment',
  'instance-monitor', 'task-management', 'process-analysis',
])

const TITLE_ALIASES = {
  // 库存相关
  '库存查询': ['库存管理', '库存查询'],
  '库存成本调整': ['成本调价', '库存成本调整'],
  '库存溢余处理': ['报溢管理', '库存溢余处理'],
  '库存报损处理': ['报损管理', '库存报损处理'],
  // 配送
  '配送仪表盘': ['工作台', '配送仪表盘', 'Dashboard'],
  // 销售/采购
  '退货处理': ['退货管理', '退货处理'],
  'BOM管理': ['BOM', 'BOM物料清单'],
  '销售分析': ['销售分析', '销售分析报表'],
  '采购换货': ['采购换货', '采购换货管理'],
  '发货管理': ['发货管理'],
  // 财务
  '资金流水台账': ['资金流水', '资金流水台账'],
  '应收明细': ['应收账款', '应收明细', '应收'],
  '应付明细': ['应付账款', '应付明细', '应付'],
  '定金押金管理': ['定金', '押金', '双向定金'],
  // 费用
  '费用审批': ['审批中心', '费用审批', '审批'],
  '费用付款': ['付款确认', '费用付款', '付款'],
  '费用统计': ['费用统计', '费用统计台账'],
  // 资产
  '资产采购': ['采购管理', '资产采购'],
  // 预算
  '预算概览': ['工作台', '预算概览', '预算管理'],
  // 客户
  '客户档案': ['客户管理', '客户档案'],
  '组织架构': ['组织架构', '部门管理'],
  // 产品/定价
  '定价管理': ['客户等级', '产品价格', '定价管理'],
  '定价审批': ['价格审批', '定价审批'],
  '价格层级': ['价格层级', '价格层级配置'],
  // 往来
  '往来单位': ['往来单位', '往来单位管理'],
  // 订单/图表
  '订单中心': ['工作台', '订单中心'],
  '图表': ['工作台', '图表'],
}

function checkTitle(menuName, pageTitle) {
  if (!pageTitle || pageTitle.length < 2) return 'EMPTY'
  const aliases = TITLE_ALIASES[menuName]
  if (aliases) return aliases.some(a => pageTitle.includes(a)) ? 'OK' : 'TITLE?'
  const m = menuName.replace(/\s+/g, '').replace(/管理/g, '').replace(/处理/g, '').replace(/中心/g, '').replace(/配置/g, '').replace(/查询/g, '').replace(/维护/g, '')
  const p = pageTitle.replace(/\s+/g, '').replace(/管理/g, '').replace(/处理/g, '').replace(/中心/g, '').replace(/配置/g, '').replace(/查询/g, '').replace(/维护/g, '')
  return p.includes(m.substring(0, Math.min(m.length, 4))) ? 'OK' : 'TITLE?'
}

function isKnown404(path) {
  const seg = path.replace(/^\//, '').split('/')[0]
  return KNOWN_404_SEGMENTS.has(seg)
}

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage()
  const results = []

  // 1. 登录获取 token
  console.log('[1/4] 登录...')
  const resp = await page.request.post('http://localhost:5655/api/auth/login', {
    data: { username: 'admin', password: 'admin123', tenantName: '系统租户' }
  })
  const json = await resp.json()
  const token = json?.data?.token
  if (!token) { console.error('登录失败:', JSON.stringify(json)); process.exit(1) }
  console.log('   Token 获取成功')

  // 2. 获取菜单树
  console.log('[2/4] 获取菜单树...')
  const menuResp = await page.request.get('http://localhost:5655/api/menu/user/client/tenant-admin', {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  const menus = (await menuResp.json())?.data || []

  /**
   * 收集所有叶子菜单项，计算其完整浏览器 URL。
   * 前端 transformMenuToRoute 将菜单树转为嵌套 Vue Router 路由：
   *   - displayGroup=1: 纯展示分组，不生成父路由，子菜单直接注册到上级
   *   - displayGroup=0: 生成父路由，子路由嵌套在父路由下
   *   - path="index" 表示默认子路由
   */
  function collectLeaves(items, parentUrlPath = '') {
    const leaves = []
    for (const m of items) {
      const rawPath = (m.path || '').replace(/^\//, '')

      if (m.menuType === 1 && rawPath && m.visible !== 0) {
        // 计算完整浏览器 URL
        let fullPath
        if (m.displayGroup === 1 || !parentUrlPath) {
          // 无父路由或 displayGroup=1: 直接使用 path
          fullPath = '/' + rawPath
        } else if (rawPath === 'index') {
          // path="index" 是父路由的默认子路由，完整 URL 为 parentPath/index
          fullPath = parentUrlPath + '/index'
        } else if (rawPath.startsWith(parentUrlPath.replace(/^\//, '') + '/')) {
          // 子 path 包含父前缀: strip 后拼接
          const rel = rawPath.substring(parentUrlPath.replace(/^\//, '').length + 1)
          fullPath = parentUrlPath + '/' + rel
        } else {
          // 子 path 不包含父前缀: 直接拼接
          fullPath = parentUrlPath + '/' + rawPath
        }
        leaves.push({ name: m.menuName, path: fullPath })
      }

      if (m.children && m.children.length > 0) {
        if (m.displayGroup === 1) {
          // displayGroup=1: 不创建父路由，子菜单在上级 URL 空间
          leaves.push(...collectLeaves(m.children, parentUrlPath))
        } else {
          // displayGroup=0: 创建父路由，子菜单在其下
          const myUrlPath = '/' + rawPath
          leaves.push(...collectLeaves(m.children, myUrlPath))
        }
      }
    }
    return leaves
  }

  const leaves = collectLeaves(menus)
  console.log(`   共 ${leaves.length} 个可见菜单项`)

  // 3. ★ 使用 addInitScript 注入认证信息，只加载 SPA 一次
  console.log('[3/4] 加载 SPA...')
  await page.addInitScript((t) => {
    localStorage.setItem('token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
  }, token)

  await page.goto('http://localhost:3000/dashboard', { waitUntil: 'domcontentloaded', timeout: 30000 })
  await page.waitForTimeout(5000)

  if (page.url().includes('/login')) {
    console.error('SPA 加载失败: 被重定向到登录页')
    await browser.close()
    process.exit(1)
  }

  // 验证 Vue Router 可用
  const hasRouter = await page.evaluate(() => {
    // 尝试通过 Vue app 获取 router
    const app = document.getElementById('app')?.__vue_app__
    return !!app
  })
  if (!hasRouter) {
    console.error('Vue app 未挂载')
    await browser.close()
    process.exit(1)
  }
  console.log('   SPA 加载成功，开始 SPA 内导航验证\n')

  // 4. 通过 Vue Router 进行 SPA 内导航
  console.log('[4/4] 逐项验证 (SPA Router 导航)...\n')

  for (let i = 0; i < leaves.length; i++) {
    const { name, path } = leaves[i]
    try {
      // 使用 Vue Router 的 push 进行 SPA 内导航（不重新加载页面）
      const navResult = await page.evaluate(async (targetPath) => {
        try {
          const app = document.getElementById('app')?.__vue_app__
          if (!app) return { error: 'no vue app' }

          const router = app.config.globalProperties.$router
          if (!router) return { error: 'no router' }

          // 导航并捕获可能的错误
          try {
            await router.push(targetPath)
          } catch (navErr) {
            return { error: 'push failed: ' + (navErr.message || navErr) }
          }
          await new Promise(r => setTimeout(r, 500))

          const currentPath = router.currentRoute?.value?.fullPath || window.location.pathname + window.location.search
          return { success: true, requested: targetPath, path: currentPath }
        } catch (e) {
          return { error: e.message || String(e) }
        }
      }, path)

      if (navResult?.error) {
        throw new Error(navResult.error)
      }

      // 调试：如果请求路径与实际路径不同，打印差异
      if (navResult?.requested !== navResult?.path) {
        console.log(`   [REDIR] ${path} → ${navResult.path}`)
      }

      // 等待页面渲染
      await page.waitForTimeout(1200)

      const url = page.url()
      const bodyText = (await page.textContent('body')) || ''

      // 如果重定向到了登录页
      if (url.includes('/login')) {
        console.log(`[AUTH    ] ${String(i+1).padStart(3)} ${name.padEnd(16)} → 需要登录 (router guard)`)
        results.push({ name, path, title: '需要登录', url, status: 'AUTH' })
        continue
      }

      // 获取页面标题
      let pageTitle = ''
      for (const sel of ['h1', 'h2', '.page-header__title', '[class*="page-header"] h2', '.ant-page-header-heading-title']) {
        const el = page.locator(sel).first()
        if (await el.count() > 0) {
          const t = (await el.textContent()) || ''
          if (t.trim().length >= 2) { pageTitle = t.trim(); break }
        }
      }

      // 判断状态
      let status
      if (bodyText.includes('500') && (bodyText.includes('服务器') || bodyText.includes('Server'))) {
        status = '500'; pageTitle = '服务器错误'
      } else if (bodyText.includes('404') || bodyText.includes('页面不存在') || bodyText.includes('Not Found')) {
        status = isKnown404(path) ? '404*' : '404'; pageTitle = '页面不存在'
      } else if (!pageTitle || pageTitle.length < 2) {
        const mainEl = page.locator('.ant-layout-content, main, [class*="page-container"], .ant-table, .ant-pro-table').first()
        const hasContent = (await mainEl.count() > 0) && ((await mainEl.textContent()) || '').trim().length > 20
        status = hasContent ? 'EMPTY' : (isKnown404(path) ? '404*' : 'EMPTY')
        pageTitle = pageTitle || (hasContent ? '(有内容无标题)' : '无标题')
      } else {
        status = checkTitle(name, pageTitle)
      }

      const line = `[${status.padEnd(8)}] ${String(i+1).padStart(3)} ${name.padEnd(16)} → ${pageTitle.padEnd(30)} ${url}`
      console.log(line)
      results.push({ name, path, title: pageTitle, url, status })

    } catch (err) {
      const emsg = err.message ? err.message.substring(0, 100) : String(err)
      console.log(`[ERROR   ] ${String(i+1).padStart(3)} ${name.padEnd(16)} → ${emsg}`)
      results.push({ name, path, title: '', url: '', status: 'ERROR' })
    }
  }

  // 5. 报告
  console.log('\n' + '='.repeat(80))
  const counts = {}
  for (const r of results) counts[r.status] = (counts[r.status] || 0) + 1
  console.log('状态统计:', JSON.stringify(counts))

  const titleIssues = results.filter(r => r.status === 'TITLE?')
  if (titleIssues.length > 0) {
    console.log('\n⚠ 页面标题可能不匹配的菜单:')
    titleIssues.forEach(r => console.log(`  ${r.name} → "${r.title}" | ${r.url}`))
  }

  const err500 = results.filter(r => r.status === '500')
  if (err500.length > 0) {
    console.log('\n❌ 500错误:')
    err500.forEach(r => console.log(`  ${r.name} | ${r.url}`))
  }

  const err404 = results.filter(r => r.status === '404')
  if (err404.length > 0) {
    console.log('\n⚠ 新404页面:')
    err404.forEach(r => console.log(`  ${r.name} | ${r.url}`))
  }

  const known404 = results.filter(r => r.status === '404*')
  if (known404.length > 0) {
    console.log(`\n📋 已知404: ${known404.length} 项`)
  }

  const empty = results.filter(r => r.status === 'EMPTY')
  if (empty.length > 0) {
    console.log('\n📄 空白/无标题:')
    empty.forEach(r => console.log(`  ${r.name} → "${r.title}" | ${r.url}`))
  }

  const ok = results.filter(r => r.status === 'OK').length
  const total = results.length
  console.log(`\n✅ 通过: ${ok} / ${total} (${(ok/total*100).toFixed(1)}%)`)

  await browser.close()
})()
