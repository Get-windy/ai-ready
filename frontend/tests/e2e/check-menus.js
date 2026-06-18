const { chromium } = require('playwright')

const KNOWN_404 = new Set([
  'WMS仓储执行', '仓库管理', '库位管理', '收货管理', '上架管理', '拣货管理', '波次管理',
  '发货管理(WMS)', '库存查询(WMS)', '库内移库', '盘点管理(WMS)', '库内事件',
  '配送管理', '配送仪表盘', '渠道管理', '骑手管理', '车辆管理', '配送路线',
  '智能调度', '订单池', '配送跟踪', '实名认证', '配送配置',
  '资产管理', '资产台账', '资产分类', '资产采购', '折旧管理', '资产调拨',
  '资产处置', '资产盘点', '资产报表',
  '预算管理', '预算概览', '预算模板', '年度预算', '预算调整', '预算报表',
  '工作流', '流程监控', '任务管理', '流程分析',
])

function normalize(s) {
  return s.replace(/\s+/g, '').replace(/管理/g, '').replace(/处理/g, '').replace(/中心/g, '').replace(/配置/g, '').replace(/查询/g, '').replace(/维护/g, '')
}

function checkMatch(menuName, pageTitle) {
  if (!pageTitle || pageTitle.length < 2) return 'EMPTY'
  const m = normalize(menuName)
  const p = normalize(pageTitle)
  if (p.includes(m.substring(0, Math.min(m.length, 4)))) return 'OK'
  return 'MISMATCH?'
}

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage()
  const results = []

  try {
    // 1. Login via API
    console.log('[1/4] 登录...')
    const resp = await page.request.post('http://localhost:5655/api/auth/login', {
      data: { username: 'admin', password: 'admin123', tenantName: '系统租户' }
    })
    const json = await resp.json()
    const token = json?.data?.token
    if (!token) { console.error('登录失败:', JSON.stringify(json)); process.exit(1) }
    console.log('   Token获取成功')

    // 2. Set auth and navigate
    await page.goto('http://localhost:3000', { waitUntil: 'domcontentloaded' })
    await page.evaluate(({ token }) => {
      localStorage.setItem('token', token)
      localStorage.setItem('tenantId', '1')
      localStorage.setItem('tenantName', '系统租户')
    }, { token })

    await page.goto('http://localhost:3000/dashboard', { waitUntil: 'domcontentloaded', timeout: 20000 })
    await page.waitForTimeout(3000)

    if (page.url().includes('login')) {
      console.error('登录失败: 被重定向到登录页')
      process.exit(1)
    }
    console.log('   仪表盘加载成功')

    // 3. Expand all submenus
    console.log('[2/4] 展开所有菜单组...')
    const expanders = await page.locator('.ant-menu-submenu-title').all()
    for (const exp of expanders) {
      try { await exp.click(); await page.waitForTimeout(100) } catch {}
    }
    await page.waitForTimeout(800)

    // 4. Click each menu item and check
    const menuItems = await page.locator('.ant-menu-item').all()
    console.log(`[3/4] 共 ${menuItems.length} 个菜单项，逐一点击验证...`)

    let idx = 0
    for (const item of menuItems) {
      try {
        const text = (await item.textContent()) || ''
        const menuName = text.replace(/[^\u4e00-\u9fa5a-zA-Z0-9]/g, '').trim()
        if (!menuName) continue
        idx++

        // Click
        await item.click()
        await page.waitForTimeout(1200)

        // Get page title from h1, h2, or PageContainer
        const url = page.url()
        const bodyText = (await page.textContent('body')) || ''

        let pageTitle = ''
        // Try multiple selectors
        for (const sel of ['h1', 'h2', '.page-header__title', '[class*="page-header"] h2']) {
          const el = page.locator(sel).first()
          if (await el.count() > 0) {
            const t = (await el.textContent()) || ''
            if (t.trim().length >= 2) { pageTitle = t.trim(); break }
          }
        }

        // Determine status
        let status = 'OK'
        let note = pageTitle

        if (bodyText.includes('500') && bodyText.includes('服务器')) {
          status = '500'; note = '服务器错误'
        } else if (bodyText.includes('404') || bodyText.includes('页面不存在')) {
          status = '404'; note = '页面不存在'
        } else if (!pageTitle || pageTitle.length < 2) {
          status = 'EMPTY'; note = '无标题'
        } else {
          const matchResult = checkMatch(menuName, pageTitle)
          if (matchResult !== 'OK') status = matchResult
        }

        // Mark known-404 as expected
        if (status === '404' && KNOWN_404.has(menuName)) {
          status = '404*'
        }
        if (status === 'EMPTY' && KNOWN_404.has(menuName)) {
          status = 'EMPTY*'
        }

        const line = `[${status.padEnd(10)}] #${String(idx).padStart(3)} ${menuName.padEnd(16)} → ${(pageTitle || '(无)').padEnd(28)} ${url}`
        console.log(line)
        results.push({ idx, menu: menuName, title: pageTitle, url, status })

      } catch (err) {
        console.log(`[ERROR] #${idx}: ${err.message}`)
        results.push({ idx, menu: `item-${idx}`, title: '', url: '', status: 'ERROR' })
      }
    }

    // 5. Summary
    console.log('\n' + '='.repeat(80))
    const ok = results.filter(r => r.status === 'OK').length
    const mismatch = results.filter(r => r.status === 'MISMATCH?').length
    const err500 = results.filter(r => r.status === '500').length
    const err404 = results.filter(r => r.status === '404').length
    const known404 = results.filter(r => r.status === '404*' || r.status === 'EMPTY*').length
    const empty = results.filter(r => r.status === 'EMPTY').length
    const errors = results.filter(r => r.status === 'ERROR').length

    console.log(`总计: ${results.length} | 通过: ${ok} | 不匹配: ${mismatch} | 500: ${err500} | 404: ${err404} | 已知404: ${known404} | 空白: ${empty} | 异常: ${errors}`)

    if (mismatch > 0) {
      console.log('\n⚠ 可能不匹配的菜单:')
      results.filter(r => r.status === 'MISMATCH?').forEach(r => {
        console.log(`  ${r.menu} → 页面标题: "${r.title}" | URL: ${r.url}`)
      })
    }

    if (err500 > 0) {
      console.log('\n❌ 500错误:')
      results.filter(r => r.status === '500').forEach(r => {
        console.log(`  ${r.menu} | URL: ${r.url}`)
      })
    }

    if (err404 > 0) {
      console.log('\n⚠ 404错误（非已知）:')
      results.filter(r => r.status === '404').forEach(r => {
        console.log(`  ${r.menu} | URL: ${r.url}`)
      })
    }

  } catch (err) {
    console.error('Fatal:', err)
  } finally {
    await browser.close()
  }
})()
