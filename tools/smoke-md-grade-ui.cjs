/* 资料模块 UI 冒烟（2026-09-26 客户级别收敛 + D-17 验证）
 *
 * 覆盖两个本轮改动的用户可见面：
 *   ① 客户页「客户级别」子标签 —— 数据是否从新端点来（无 404）、能否渲染
 *   ② 物流公司新增表单 —— 编号是否仍为 WuLiu + 3 位补零（实现收敛但格式不变）
 *
 * ⚠️ 不走 e2e-ui-login.cjs 的 route 转发：vite 的 /api 代理本就指向 5655，
 *    再叠一层 route.fetch 会在二次导航时撞上 "Request context disposed"。
 *
 * 运行（playwright 装在 frontend/，必须给 NODE_PATH）：
 *   cd i:/AI-Ready/frontend && NODE_PATH=i:/AI-Ready/frontend/node_modules node ../tools/smoke-md-grade-ui.cjs
 */
const { chromium } = require('playwright')

const VITE = Number(process.env.VITE_PORT || 5173)
const USER = process.env.E2E_USER || 'e2e_product'
const PASS = process.env.E2E_PASS || 'admin123'

const results = []
function check(name, ok, detail = '') {
  results.push({ name, ok })
  console.log(`  [${ok ? 'PASS' : 'FAIL'}] ${name}${detail ? '  ' + detail : ''}`)
}

async function login(page, target) {
  await page.goto(`http://localhost:${VITE}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(2000)
  const res = await page.evaluate(async ({ user, pass }) => {
    const cap = await fetch('/api/auth/captcha').then((r) => r.json())
    const svg = atob(cap.data.img.split(',')[1])
    const code = [...svg.matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map((m) => m[1]).join('')
    const r = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: user, password: pass, tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid }),
    }).then((x) => x.json())
    if (r?.data?.token) {
      localStorage.setItem('token', r.data.token)
      localStorage.setItem('tenantId', String(r.data.tenantId || 1))
      localStorage.setItem('tenantName', r.data.tenantName || '系统租户')
    }
    return { ok: !!r?.data?.token, msg: r?.message }
  }, { user: USER, pass: PASS })
  if (!res.ok) return res
  await page.goto(`http://localhost:${VITE}${target}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(9000)
  return res
}

;(async () => {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage()
  const bad404 = []
  const consoleErrors = []
  page.on('response', (r) => {
    if (r.status() === 404) bad404.push(`${r.request().method()} ${new URL(r.url()).pathname}`)
  })
  page.on('console', (m) => { if (m.type() === 'error') consoleErrors.push(m.text().slice(0, 160)) })

  try {
    // ── ① 客户页 · 客户级别子标签 ──
    const loginRes = await login(page, '/md/customer/index')
    check('登录并进入客户页', !!loginRes.ok, loginRes.msg || '')

    const gradeTab = page.locator('text=客户级别').first()
    if (await gradeTab.count()) {
      await gradeTab.click()
      await page.waitForTimeout(5000)
      const rows = await page.locator('.ant-table-tbody tr.ant-table-row').count()
      check('客户级别子标签可切换', true, `行数=${rows}`)
    } else {
      check('客户级别子标签可切换', false, '未找到子标签')
    }

    const oldEp = bad404.filter((u) => u.includes('/customer/level'))
    check('不再请求已下线的 /erp/customer/level', oldEp.length === 0, oldEp.join(',') || '无')
    const gradeEp = bad404.filter((u) => u.includes('/partner/grades'))
    check('级别相关端点无 404', gradeEp.length === 0, gradeEp.join(',') || '无')

    // ── ② 物流公司新增表单 · 编号 ──
    bad404.length = 0
    await page.goto(`http://localhost:${VITE}/md/logistics/form`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForTimeout(7000)
    const codeVal = await page.evaluate(() => {
      const el = [...document.querySelectorAll('input')].find((i) => /^WuLiu/i.test(i.value || ''))
      return el ? el.value : ''
    })
    check('物流编号仍为 WuLiu + 3 位补零', /^WuLiu\d{3,}$/.test(codeVal), `实际=${codeVal || '(空)'}`)

    check('全流程无 JS 运行时错误', consoleErrors.length === 0, consoleErrors[0] || '')
  } catch (e) {
    check('脚本执行', false, String(e).slice(0, 220))
  } finally {
    await browser.close()
  }

  const passed = results.filter((r) => r.ok).length
  console.log(`\n== ${passed}/${results.length} 通过 ==`)
  process.exit(passed === results.length ? 0 : 1)
})()
