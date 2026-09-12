/* UI 验收的鉴权引导（共享环境专用）
 *
 * 背景：本机 5655 端口被多个并行会话共享（vite 代理固定指向它），若把页面 token 注入
 * localStorage，部分请求仍会落到别人的实例上 → 401 → 前端清 token 跳登录页，
 * UI 验收随机失败。
 *
 * 做法：① 用 route 把页面的 /api/** 统一转发到本次要验证的实例；
 *      ② 在**页面上下文内**直连该实例完成一次真实登录（含验证码），让前端 store 拿到
 *         完整会话（token / refresh 等），而不是只塞一个 token 进 localStorage。
 */
const { chromium } = require('playwright')

async function bootstrapAuthedPage(page, API, VITE, opts = {}) {
  const user = process.env.E2E_USER || 'e2e_product'
  const pass = process.env.E2E_PASS || 'admin123'
  const target = opts.target || '/erp/product/index'

  await page.route(u => u.pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    if (u.pathname.includes('/sse/')) return route.abort().catch(() => {})
    const resp = await route.fetch({ url: `http://127.0.0.1:${API}${u.pathname}${u.search}` })
    await route.fulfill({ response: resp })
  })

  await page.goto(`http://localhost:${VITE}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(2500)

  const loginRes = await page.evaluate(async ({ base, user, pass }) => {
    try {
      const cap = await fetch(base + '/api/auth/captcha').then(r => r.json())
      const svg = atob(cap.data.img.split(',')[1])
      const code = [...svg.matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
      const res = await fetch(base + '/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user, password: pass, tenantName: '系统租户', captcha: code, captchaKey: cap.data.uuid }),
      }).then(r => r.json())
      if (res?.data?.token) {
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('tenantId', String(res.data.tenantId || 1))
        localStorage.setItem('tenantName', res.data.tenantName || '系统租户')
      }
      return { code: res?.code, hasToken: !!res?.data?.token, token: res?.data?.token }
    } catch (e) { return { err: String(e) } }
  }, { base: `http://127.0.0.1:${API}`, user, pass })

  await page.goto(`http://localhost:${VITE}${target}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(opts.wait ?? 9000)
  return loginRes
}

/** 是否停在登录页 */
async function isLoginPage(page) {
  return (await page.locator('input[placeholder="请输入用户名"]').count()) > 0
}

module.exports = { bootstrapAuthedPage, isLoginPage, chromium }
