/*
 * 菜单可见性回归 —— 真机（Playwright）验证「后端下发的菜单」= 「前端渲染的菜单」
 *
 * 背景（2026-09-23 实修）：
 *   `MegaMenuPanel` 里曾有一份 `userStore.hasPermission(item.menuCode)` 的**全等匹配**副本，
 *   与后端 `MenuPermissionDeriver` 的**前缀派生**口径不同源。全库 306 条叶子菜单里只有 4 条的
 *   menu_code 恰好等于某条权限码 ⇒ 非超管用户 190/190 个叶子菜单「后端已下发、前端又隐藏」，
 *   悬停一级菜单只能看到列标题、下面一个条目都没有。已删除该副本，可见性由后端单点决定。
 *
 * 本脚本断言：
 *   ① 非超管账号悬停「人力资源」→ 面板里出现后端下发的全部叶子条目（> 0，修复前为 0）
 *   ② 超管账号悬停「人力资源」→ 面板里出现 10 个 HR 叶子
 *   ③ 两者面板中出现的菜单名左右对照打印，便于人工复核
 *
 * 用法：node tools/verify-menu-visibility.cjs
 *      FE_URL=http://localhost:5656 MENU_PORT=5655 node tools/verify-menu-visibility.cjs
 * 前置：后端 dev（5655）+ 前端 dev server（5656）运行中；账号 e2e_hr / e2e_hr_ta（见 tools/e2e-hr-user.sql）
 */
const http = require('http')

const PORT = Number(process.env.MENU_PORT || 5655)
const FE = process.env.FE_URL || 'http://localhost:5656'
const TENANT = process.env.E2E_TENANT || '系统租户'

let pass = 0
let fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log('PASS\t' + name + (detail ? '\t' + detail : '')) }
  else { fail++; console.log('FAIL\t' + name + (detail ? '\t' + detail : '')) }
}

function req(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers.Authorization = `Bearer ${token}`
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: '/api' + reqPath, method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* 非 JSON */ }
        resolve({ status: res.statusCode, json })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login(username) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    username, password: process.env.E2E_PWD || 'admin123', tenantName: TENANT,
    captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`登录失败(${username}): ` + JSON.stringify(res.json).slice(0, 200))
  return token
}

/** 悬停侧边栏「人力资源」并读出面板里渲染出来的菜单条目 */
async function readPanel(page) {
  const item = page.locator('.mega-sidebar-item', { hasText: '人力资源' }).first()
  if (await item.count() === 0) return { headers: [], rows: [] }
  await item.hover()
  await page.waitForTimeout(1200)
  const panel = page.locator('.mega-menu-panel')
  if (await panel.count() === 0) return { headers: [], rows: [] }
  const headers = await panel.locator('.mega-menu-column-header').allInnerTexts()
  const rows = await panel.locator('.mega-menu-item-row').allInnerTexts()
  await page.mouse.move(5, 5)
  await page.waitForTimeout(400)
  return { headers, rows }
}

async function run() {
  // 依赖不装在仓库根，走前端工作区里的 playwright（口径同 tools/e2e-*.cjs）
  const { chromium } = require(process.env.PW || 'I:/AI-Ready/frontend/node_modules/playwright')
  const browser = await chromium.launch()

  const cases = [
    // 非超管（SYSTEM_ADMIN）在 tools/grant-hr-permissions.py 之后持有 37 条 hr:*，
    // 但**刻意不含 hr:salary:***（薪资保密：权限种子的注释即「薪资与其余 HR 权限分开授予」）
    // ⇒ 10 个 HR 叶子里「薪资管理」被后端按前缀派生挡掉，应恰好渲染 9 条。
    { user: 'e2e_hr_ta', label: '非超管（SYSTEM_ADMIN）', expectMin: 1, expectHrLeaves: 9,
      expectAbsent: ['薪资管理'] },
    { user: 'e2e_hr', label: '超管（SUPER_ADMIN）', expectMin: 1, expectHrLeaves: 10 },
  ]

  for (const c of cases) {
    const token = await login(c.user)
    const ctx = await browser.newContext()
    const page = await ctx.newPage()
    await page.addInitScript(([tok, tname]) => {
      localStorage.setItem('token', tok)
      localStorage.setItem('tenantId', '1')
      localStorage.setItem('tenantName', tname)
    }, [token, TENANT])
    // ⚠️ 不要先访问 `/`（会先于登录态解析重定向到 /login），直连业务页
    await page.goto(`${FE}/md/staff-dept`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(3500)

    const { headers, rows } = await readPanel(page)
    const names = rows
      .map(t => t.split('\n').map(s => s.trim()).filter(Boolean)[0])
      .filter(Boolean)

    console.log(`\n[${c.label}] 面板列数=${headers.length} 条目数=${names.length}`)
    if (headers.length) console.log('  列标题: ' + headers.join(' | '))
    if (names.length) console.log('  条目: ' + names.join(' | '))

    check(`【${c.label}】悬停「人力资源」面板非空（修复前为 0）`, names.length >= c.expectMin,
      `条目数=${names.length}`)
    check(`【${c.label}】渲染条数符合预期`, names.length === c.expectHrLeaves,
      `期望 ${c.expectHrLeaves} 实得 ${names.length}`)
    for (const absent of c.expectAbsent || []) {
      check(`【${c.label}】「${absent}」按设计不可见（无对应权限码）`, !names.includes(absent),
        names.includes(absent) ? '意外出现' : '未出现')
    }
    await ctx.close()
  }

  await browser.close()
  console.log(`\n合计 PASS=${pass} FAIL=${fail}`)
  process.exit(fail === 0 ? 0 : 1)
}

run().catch(e => { console.error(e); process.exit(1) })
